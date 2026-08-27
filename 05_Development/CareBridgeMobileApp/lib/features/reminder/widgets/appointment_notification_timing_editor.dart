import 'package:flutter/material.dart';

import '../models/appointment_notification_timing.dart';

class AppointmentNotificationTimingEditor extends StatelessWidget {
  const AppointmentNotificationTimingEditor({
    super.key,
    required this.values,
    required this.onChanged,
    this.enabled = true,
    this.title = 'Thời gian thông báo',
  });

  final List<int> values;
  final ValueChanged<List<int>> onChanged;
  final bool enabled;
  final String title;

  static const _primary = Color(0xFF845143);
  static const _surface = Color(0xFFFFF8F6);

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                title,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontWeight: FontWeight.w800,
                  color: Color(0xFF271812),
                ),
              ),
            ),
            IconButton(
              key: const Key('appointment-notification-add'),
              tooltip: 'Thêm thời gian thông báo',
              onPressed: enabled ? () => _showAddDialog(context) : null,
              icon: const Icon(Icons.add_alarm_rounded, color: _primary),
            ),
          ],
        ),
        const SizedBox(height: 6),
        if (values.isEmpty)
          const Text(
            'Không gửi thông báo cho lịch hẹn này.',
            style: TextStyle(fontFamily: 'Lexend', color: Color(0xFF524440)),
          )
        else
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: values
                .map(
                  (offset) => InputChip(
                    key: ValueKey('appointment-notification-$offset'),
                    label: Text(AppointmentNotificationTiming.label(offset)),
                    onDeleted: enabled
                        ? () {
                            final next = [...values]..remove(offset);
                            onChanged(AppointmentNotificationTiming.normalize(next));
                          }
                        : null,
                  ),
                )
                .toList(),
          ),
      ],
    );
  }

  Future<void> _showAddDialog(BuildContext context) async {
    final selected = await showModalBottomSheet<int>(
      context: context,
      isScrollControlled: true,
      backgroundColor: _surface,
      builder: (context) => _TimingPicker(existing: values),
    );
    if (selected == null || values.contains(selected)) return;
    try {
      onChanged(AppointmentNotificationTiming.normalize([...values, selected]));
    } on FormatException catch (error) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error.message), backgroundColor: Colors.red),
      );
    }
  }
}

class _TimingPicker extends StatefulWidget {
  const _TimingPicker({required this.existing});

  final List<int> existing;

  @override
  State<_TimingPicker> createState() => _TimingPickerState();
}

class _TimingPickerState extends State<_TimingPicker> {
  final _days = TextEditingController(text: '0');
  final _hours = TextEditingController(text: '0');
  final _minutes = TextEditingController(text: '15');
  bool _after = false;
  String? _error;

  @override
  void dispose() {
    _days.dispose();
    _hours.dispose();
    _minutes.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.fromLTRB(
          20,
          20,
          20,
          20 + MediaQuery.viewInsetsOf(context).bottom,
        ),
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Thêm thời gian thông báo',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(height: 16),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: AppointmentNotificationTiming.presets
                    .map(
                      (value) => ActionChip(
                        label: Text(AppointmentNotificationTiming.label(value)),
                        onPressed: widget.existing.contains(value)
                            ? null
                            : () => Navigator.pop(context, value),
                      ),
                    )
                    .toList(),
              ),
              const SizedBox(height: 20),
              const Text(
                'Hoặc nhập thời gian tùy chỉnh',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 10),
              SegmentedButton<bool>(
                segments: const [
                  ButtonSegment(value: false, label: Text('Trước lịch hẹn')),
                  ButtonSegment(value: true, label: Text('Sau lịch hẹn')),
                ],
                selected: {_after},
                onSelectionChanged: (selection) =>
                    setState(() => _after = selection.first),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(child: _numberField(_days, 'Ngày')),
                  const SizedBox(width: 8),
                  Expanded(child: _numberField(_hours, 'Giờ')),
                  const SizedBox(width: 8),
                  Expanded(child: _numberField(_minutes, 'Phút')),
                ],
              ),
              if (_error != null) ...[
                const SizedBox(height: 8),
                Text(_error!, style: const TextStyle(color: Colors.red)),
              ],
              const SizedBox(height: 16),
              FilledButton(
                key: const Key('appointment-notification-save-custom'),
                onPressed: _save,
                style: FilledButton.styleFrom(
                  backgroundColor: const Color(0xFF845143),
                  minimumSize: const Size.fromHeight(48),
                ),
                child: const Text('Thêm mốc thông báo'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _numberField(TextEditingController controller, String label) {
    return TextField(
      controller: controller,
      keyboardType: TextInputType.number,
      decoration: InputDecoration(
        labelText: label,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
      ),
    );
  }

  void _save() {
    final days = int.tryParse(_days.text) ?? -1;
    final hours = int.tryParse(_hours.text) ?? -1;
    final minutes = int.tryParse(_minutes.text) ?? -1;
    if (days < 0 || hours < 0 || minutes < 0 || hours > 23 || minutes > 59) {
      setState(() => _error = 'Vui lòng nhập ngày, giờ và phút hợp lệ.');
      return;
    }
    final absolute = days * 1440 + hours * 60 + minutes;
    final value = _after ? absolute : -absolute;
    try {
      AppointmentNotificationTiming.normalize([value]);
      if (widget.existing.contains(value)) {
        setState(() => _error = 'Mốc thông báo này đã tồn tại.');
        return;
      }
      Navigator.pop(context, value);
    } on FormatException catch (error) {
      setState(() => _error = error.message);
    }
  }
}
