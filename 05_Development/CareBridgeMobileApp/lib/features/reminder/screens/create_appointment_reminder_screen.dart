import 'package:flutter/material.dart';

import '../../notification/services/notification_service.dart';
import '../models/appointment_notification_timing.dart';
import '../models/reminder_model.dart';
import '../services/reminder_service.dart';
import '../widgets/appointment_notification_timing_editor.dart';

class CreateAppointmentReminderScreen extends StatefulWidget {
  const CreateAppointmentReminderScreen({super.key, this.initialDate});

  final DateTime? initialDate;

  @override
  State<CreateAppointmentReminderScreen> createState() =>
      _CreateAppointmentReminderScreenState();
}

class _CreateAppointmentReminderScreenState
    extends State<CreateAppointmentReminderScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _error = Color(0xFFBA1A1A);

  final _service = ReminderService.instance;
  final _titleController = TextEditingController();
  final _locationController = TextEditingController();
  final List<TimeOfDay> _times = [];
  late DateTime _startDate;
  DateTime? _endDate;
  RecurrenceType _recurrence = RecurrenceType.none;
  List<int> _notificationOffsets = AppointmentNotificationTiming.systemDefaults;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final requestedDate = widget.initialDate?.toLocal();
    final normalizedRequested = requestedDate == null
        ? null
        : DateTime(requestedDate.year, requestedDate.month, requestedDate.day);
    _startDate =
        normalizedRequested == null || normalizedRequested.isBefore(today)
        ? today
        : normalizedRequested;
    final defaultTime = now.add(const Duration(minutes: 10));
    _times.add(TimeOfDay.fromDateTime(defaultTime));
    _loadNotificationDefaults();
  }

  Future<void> _loadNotificationDefaults() async {
    try {
      final preferences = await NotificationService.instance.getPreferences();
      if (!mounted) return;
      setState(() {
        _notificationOffsets = preferences.appointmentReminderDefaults;
      });
    } catch (_) {
      // Keep the documented system defaults when preferences cannot be loaded.
    }
  }

  @override
  void dispose() {
    _titleController.dispose();
    _locationController.dispose();
    super.dispose();
  }

  Future<void> _pickStartDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _startDate,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365 * 2)),
    );
    if (picked != null) setState(() => _startDate = picked);
  }

  Future<void> _pickEndDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _endDate ?? _startDate.add(const Duration(days: 7)),
      firstDate: _startDate,
      lastDate: DateTime.now().add(const Duration(days: 365 * 2)),
    );
    if (picked != null) setState(() => _endDate = picked);
  }

  Future<void> _addTime() async {
    final picked = await showTimePicker(
      context: context,
      initialTime: const TimeOfDay(hour: 8, minute: 0),
    );
    if (picked == null) return;
    setState(() {
      if (!_times.any(
        (t) => t.hour == picked.hour && t.minute == picked.minute,
      )) {
        _times.add(picked);
        _times.sort(
          (a, b) => (a.hour * 60 + a.minute).compareTo(b.hour * 60 + b.minute),
        );
      }
    });
  }

  Future<void> _save() async {
    final title = _titleController.text.trim();
    if (title.isEmpty) {
      _showError(
        'Vui lòng nhập nội dung lịch hẹn, khám định kỳ hoặc tái khám.',
      );
      return;
    }
    if (_times.isEmpty) {
      _showError('Vui lòng thêm ít nhất một giờ nhắc.');
      return;
    }

    final scheduledTimes =
        _times
            .map(
              (time) => DateTime(
                _startDate.year,
                _startDate.month,
                _startDate.day,
                time.hour,
                time.minute,
              ),
            )
            .toList()
          ..sort();

    final minimum = DateTime.now().add(const Duration(minutes: 5));
    if (scheduledTimes.any((time) => time.isBefore(minimum))) {
      _showError('Giờ nhắc phải sau hiện tại ít nhất 5 phút.');
      return;
    }

    setState(() => _saving = true);
    try {
      final location = _locationController.text.trim();
      final reminderTitle = location.isEmpty ? title : '$title - $location';
      var createdCount = 0;
      for (final scheduledAt in scheduledTimes) {
        await _service.createAppointmentReminder(
          title: reminderTitle,
          scheduledAt: scheduledAt,
          recurrenceType: _recurrence,
          recurrenceEndDate: _endDate,
          notificationOffsetsMinutes: _notificationOffsets,
        );
        createdCount++;
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Đã tạo $createdCount nhắc lịch hẹn.')),
      );
      Navigator.pop(context, true);
    } catch (e) {
      if (!mounted) return;
      _showError(
        'Không thể tạo nhắc lịch. Hãy kiểm tra Việc hôm nay rồi thử lại.',
      );
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message), backgroundColor: _error));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded, color: _onSurface),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Nhắc lịch hẹn',
          style: TextStyle(
            fontFamily: 'Lexend',
            color: _onSurface,
            fontWeight: FontWeight.w800,
          ),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
        children: [
          _Section(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                TextField(
                  controller: _titleController,
                  maxLength: 255,
                  decoration: _inputDecoration(
                    'Lịch hẹn, khám định kỳ hoặc tái khám *',
                  ),
                ),
                const SizedBox(height: 10),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFF8F6),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: const Row(
                    children: [
                      Icon(
                        Icons.info_outline_rounded,
                        size: 18,
                        color: _primary,
                      ),
                      SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          'Nhắc nhở này sẽ giúp bạn không quên lịch hẹn quan trọng với bác sĩ.',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            color: _onSurfaceVariant,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          _Section(
            child: TextField(
              controller: _locationController,
              minLines: 2,
              maxLines: 4,
              decoration: _inputDecoration('Địa điểm hoặc phòng khám'),
            ),
          ),
          const SizedBox(height: 14),
          _Section(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                DropdownButtonFormField<RecurrenceType>(
                  initialValue: _recurrence,
                  decoration: _inputDecoration('Lặp lại'),
                  items: RecurrenceType.values
                      .map(
                        (value) => DropdownMenuItem(
                          value: value,
                          child: Text(value.displayLabel),
                        ),
                      )
                      .toList(),
                  onChanged: _saving
                      ? null
                      : (value) => setState(
                          () => _recurrence = value ?? RecurrenceType.none,
                        ),
                ),
                const SizedBox(height: 10),
                _DateButton(
                  label: 'Ngày bắt đầu',
                  value: _formatDate(_startDate),
                  onTap: _pickStartDate,
                ),
                const SizedBox(height: 10),
                _DateButton(
                  label: 'Ngày kết thúc',
                  value: _endDate == null
                      ? 'Không có ngày kết thúc'
                      : _formatDate(_endDate!),
                  onTap: _pickEndDate,
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          _Section(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  children: [
                    const Expanded(
                      child: Text(
                        'Giờ nhắc',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontWeight: FontWeight.w800,
                          color: _onSurface,
                        ),
                      ),
                    ),
                    IconButton(
                      onPressed: _saving ? null : _addTime,
                      icon: const Icon(
                        Icons.add_alarm_rounded,
                        color: _primary,
                      ),
                    ),
                  ],
                ),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: _times
                      .map(
                        (time) => InputChip(
                          label: Text(time.format(context)),
                          onDeleted: _times.length == 1 || _saving
                              ? null
                              : () => setState(() => _times.remove(time)),
                        ),
                      )
                      .toList(),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          _Section(
            child: AppointmentNotificationTimingEditor(
              values: _notificationOffsets,
              enabled: !_saving,
              onChanged: (values) =>
                  setState(() => _notificationOffsets = values),
            ),
          ),
          const SizedBox(height: 20),
          FilledButton.icon(
            onPressed: _saving ? null : _save,
            icon: _saving
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                      color: Colors.white,
                      strokeWidth: 2,
                    ),
                  )
                : const Icon(Icons.save_rounded),
            label: const Text(
              'Lưu nhắc lịch',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontWeight: FontWeight.w800,
              ),
            ),
            style: FilledButton.styleFrom(
              backgroundColor: _primary,
              minimumSize: const Size.fromHeight(54),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
          ),
        ],
      ),
    );
  }

  InputDecoration _inputDecoration(String label) => InputDecoration(
    labelText: label,
    labelStyle: const TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant),
    filled: true,
    fillColor: Colors.white,
    border: OutlineInputBorder(borderRadius: BorderRadius.circular(14)),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: _primaryContainer.withAlpha(70)),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: const BorderSide(color: _primary, width: 1.5),
    ),
  );

  String _formatDate(DateTime value) {
    return '${value.day.toString().padLeft(2, '0')}/${value.month.toString().padLeft(2, '0')}/${value.year}';
  }
}

class _Section extends StatelessWidget {
  final Widget child;

  const _Section({required this.child});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: const [
          BoxShadow(
            color: Color(0x12000000),
            blurRadius: 14,
            offset: Offset(0, 6),
          ),
        ],
      ),
      child: child,
    );
  }
}

class _DateButton extends StatelessWidget {
  final String label;
  final String value;
  final VoidCallback onTap;

  const _DateButton({
    required this.label,
    required this.value,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return OutlinedButton.icon(
      onPressed: onTap,
      icon: const Icon(Icons.calendar_month_rounded),
      label: Row(
        children: [
          Text(label, style: const TextStyle(fontFamily: 'Lexend')),
          const Spacer(),
          Text(
            value,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
      style: OutlinedButton.styleFrom(
        foregroundColor: const Color(0xFF845143),
        side: const BorderSide(color: Color(0xFFC98C7B)),
        minimumSize: const Size.fromHeight(50),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      ),
    );
  }
}
