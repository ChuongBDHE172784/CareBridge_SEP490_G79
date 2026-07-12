import 'package:flutter/material.dart';

import '../models/reminder_model.dart';
import '../services/reminder_service.dart';

class CreateAppointmentReminderScreen extends StatefulWidget {
  const CreateAppointmentReminderScreen({super.key});

  @override
  State<CreateAppointmentReminderScreen> createState() =>
      _CreateAppointmentReminderScreenState();
}

class _CreateAppointmentReminderScreenState
    extends State<CreateAppointmentReminderScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Colors.white;
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _error = Color(0xFFBA1A1A);

  final _service = ReminderService.instance;
  final _titleController = TextEditingController();
  final _locationController = TextEditingController();
  DateTime _date = DateTime.now().add(const Duration(minutes: 10));
  TimeOfDay _time = TimeOfDay.fromDateTime(DateTime.now().add(const Duration(minutes: 10)));
  RecurrenceType _recurrence = RecurrenceType.none;
  bool _saving = false;

  @override
  void dispose() {
    _titleController.dispose();
    _locationController.dispose();
    super.dispose();
  }

  Future<void> _pickDateTime() async {
    final pickedDate = await showDatePicker(
      context: context,
      initialDate: _date,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365 * 2)),
    );
    if (pickedDate == null || !mounted) return;
    final pickedTime = await showTimePicker(
      context: context,
      initialTime: _time,
    );
    if (pickedTime == null) return;
    setState(() {
      _date = pickedDate;
      _time = pickedTime;
    });
  }

  Future<void> _save() async {
    final title = _titleController.text.trim();
    if (title.isEmpty) {
      _showError('Vui lòng nhập nội dung lịch hẹn, khám định kỳ hoặc tái khám.');
      return;
    }
    final scheduledAt = DateTime(
      _date.year,
      _date.month,
      _date.day,
      _time.hour,
      _time.minute,
    );
    if (scheduledAt.isBefore(DateTime.now().add(const Duration(minutes: 5)))) {
      _showError('Giờ nhắc phải sau hiện tại ít nhất 5 phút.');
      return;
    }

    final location = _locationController.text.trim();
    final reminderTitle = location.isEmpty ? title : '$title - $location';
    setState(() => _saving = true);
    try {
      await _service.createAppointmentReminder(
        title: reminderTitle,
        scheduledAt: scheduledAt,
        recurrenceType: _recurrence,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã tạo nhắc lịch hẹn.')),
      );
      Navigator.pop(context, true);
    } catch (e) {
      if (!mounted) return;
      _showError('Không thể tạo nhắc lịch: $e');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: _error),
    );
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
          style: TextStyle(fontFamily: 'Lexend', color: _onSurface, fontWeight: FontWeight.w800),
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
                  decoration: _inputDecoration('Lịch hẹn, khám định kỳ hoặc tái khám *'),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: _locationController,
                  maxLength: 120,
                  decoration: _inputDecoration('Địa điểm hoặc phòng khám'),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          _Section(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _DateButton(
                  label: 'Giờ nhắc',
                  value: _formatDateTime(_scheduledAt),
                  onTap: _pickDateTime,
                ),
                const SizedBox(height: 10),
                DropdownButtonFormField<RecurrenceType>(
                  value: _recurrence,
                  decoration: _inputDecoration('Lặp lại'),
                  items: RecurrenceType.values
                      .map((value) => DropdownMenuItem(
                            value: value,
                            child: Text(value.displayLabel),
                          ))
                      .toList(),
                  onChanged: _saving
                      ? null
                      : (value) => setState(() => _recurrence = value ?? RecurrenceType.none),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          FilledButton.icon(
            onPressed: _saving ? null : _save,
            icon: _saving
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2),
                  )
                : const Icon(Icons.save_rounded),
            label: const Text(
              'Lưu nhắc lịch',
              style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w800),
            ),
            style: FilledButton.styleFrom(
              backgroundColor: _primary,
              minimumSize: const Size.fromHeight(54),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            ),
          ),
        ],
      ),
    );
  }

  DateTime get _scheduledAt => DateTime(
        _date.year,
        _date.month,
        _date.day,
        _time.hour,
        _time.minute,
      );

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

  static String _formatDateTime(DateTime value) {
    final d = value.day.toString().padLeft(2, '0');
    final m = value.month.toString().padLeft(2, '0');
    final h = value.hour.toString().padLeft(2, '0');
    final min = value.minute.toString().padLeft(2, '0');
    return '$d/$m/${value.year} $h:$min';
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
        borderRadius: BorderRadius.circular(18),
        boxShadow: const [
          BoxShadow(color: Color(0x12000000), blurRadius: 14, offset: Offset(0, 6)),
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
          Text(value, style: const TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w700)),
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
