import 'package:flutter/material.dart';

import '../models/reminder_model.dart';
import '../services/reminder_service.dart';

class UpdateSnoozeReminderScreen extends StatefulWidget {
  final String reminderId;
  final Reminder? initialReminder;

  const UpdateSnoozeReminderScreen({
    super.key,
    required this.reminderId,
    this.initialReminder,
  });

  @override
  State<UpdateSnoozeReminderScreen> createState() => _UpdateSnoozeReminderScreenState();
}

class _UpdateSnoozeReminderScreenState extends State<UpdateSnoozeReminderScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Colors.white;
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _error = Color(0xFFBA1A1A);

  final _service = ReminderService.instance;
  Reminder? _reminder;
  bool _loading = true;
  bool _processing = false;
  String? _errorText;

  DateTime? _startDate;
  DateTime? _endDate;
  RecurrenceType _recurrence = RecurrenceType.none;
  final TextEditingController _titleController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _reminder = widget.initialReminder;
    if (_reminder != null) {
      _titleController.text = _reminder!.title;
    }
    _load();
  }

  @override
  void dispose() {
    _titleController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    if (_reminder == null) setState(() => _loading = true);
    try {
      final reminder = await _service.getReminderDetail(widget.reminderId);
      if (!mounted) return;
      setState(() {
        _reminder = reminder;
        _titleController.text = reminder.title;
        _startDate = reminder.scheduledAt.toLocal();
        _endDate = reminder.recurrenceEndDate?.toLocal();
        _recurrence = reminder.recurrenceType;
        _errorText = null;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorText = 'Không thể tải nhắc lịch.';
        _loading = false;
      });
    }
  }

  Future<void> _run(Future<Reminder> Function() action) async {
    setState(() => _processing = true);
    try {
      final updated = await action();
      if (!mounted) return;
      setState(() => _reminder = updated);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã cập nhật nhắc lịch')),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Thao tác thất bại: $e'), backgroundColor: _error),
      );
    } finally {
      if (mounted) setState(() => _processing = false);
    }
  }

  Future<void> _pickStartDate() async {
    if (_startDate == null) return;
    final date = await showDatePicker(
      context: context,
      initialDate: _startDate!,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365 * 2)),
    );
    if (date == null || !mounted) return;
    final time = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(_startDate!),
    );
    if (time == null) return;
    setState(() {
      _startDate = DateTime(date.year, date.month, date.day, time.hour, time.minute);
    });
  }

  Future<void> _pickEndDate() async {
    final current = _endDate ?? (_startDate ?? DateTime.now());
    final date = await showDatePicker(
      context: context,
      initialDate: current,
      firstDate: _startDate ?? DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365 * 5)),
    );
    if (date == null || !mounted) return;
    setState(() {
      _endDate = DateTime(date.year, date.month, date.day, 23, 59, 59);
    });
  }

  Future<void> _save() async {
    if (_startDate == null || _reminder == null) return;
    if (_titleController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Tên nhắc nhở không được để trống'), backgroundColor: _error),
      );
      return;
    }

    final titleChanged = _reminder!.title != _titleController.text.trim();
    final dateChanged = !_reminder!.scheduledAt.isAtSameMomentAs(_startDate!);
    final recurrenceChanged = _reminder!.recurrenceType != _recurrence;
    
    bool endDateChanged = false;
    if (_reminder!.recurrenceEndDate == null && _endDate != null) endDateChanged = true;
    if (_reminder!.recurrenceEndDate != null && _endDate == null) endDateChanged = true;
    if (_reminder!.recurrenceEndDate != null && _endDate != null) {
      endDateChanged = !_reminder!.recurrenceEndDate!.isAtSameMomentAs(_endDate!);
    }

    await _run(() => _service.updateReminder(
      widget.reminderId,
      title: titleChanged ? _titleController.text.trim() : null,
      scheduledAt: dateChanged ? _startDate!.toUtc() : null,
      recurrenceType: recurrenceChanged ? _recurrence : null,
      recurrenceEndDate: endDateChanged ? _endDate?.toUtc() : null,
    ));
    if (mounted) Navigator.pop(context, true);
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
          onPressed: () => Navigator.pop(context, true),
        ),
        title: const Text(
          'Chỉnh sửa nhắc nhở',
          style: TextStyle(fontFamily: 'Lexend', color: _onSurface, fontWeight: FontWeight.w700),
        ),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: _primary))
          : _errorText != null && _reminder == null
              ? _ErrorState(message: _errorText!, onRetry: _load)
              : _buildContent(_reminder!),
    );
  }

  Widget _buildContent(Reminder reminder) {
    final isTerminal = reminder.status.isTerminal;
    final canEdit = !_processing && !isTerminal;

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
      children: [
        if (isTerminal)
          Container(
            padding: const EdgeInsets.all(16),
            margin: const EdgeInsets.only(bottom: 16),
            decoration: BoxDecoration(
              color: const Color(0xFFFFDAD6).withAlpha(77),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: _error.withAlpha(50)),
            ),
            child: Row(
              children: [
                const Icon(Icons.info_outline_rounded, color: _error),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    'Nhắc nhở này đã ở trạng thái "${_statusLabel(reminder.status).toLowerCase()}" và không thể chỉnh sửa.',
                    style: const TextStyle(fontFamily: 'Lexend', color: _error),
                  ),
                ),
              ],
            ),
          ),
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: _surface,
            borderRadius: BorderRadius.circular(20),
            boxShadow: [
              BoxShadow(color: _primary.withAlpha(18), blurRadius: 18, offset: const Offset(0, 8)),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Nội dung / Tên', style: TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant, fontSize: 13)),
              const SizedBox(height: 4),
              TextFormField(
                controller: _titleController,
                enabled: canEdit,
                style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, color: _onSurface, fontWeight: FontWeight.w600),
                decoration: InputDecoration(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide(color: _primaryContainer.withAlpha(80)),
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide(color: _primaryContainer.withAlpha(80)),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: _primary),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              
              const Text('Ngày bắt đầu', style: TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant, fontSize: 13)),
              const SizedBox(height: 4),
              InkWell(
                onTap: canEdit ? _pickStartDate : null,
                borderRadius: BorderRadius.circular(12),
                child: Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    border: Border.all(color: _primaryContainer.withAlpha(80)),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.calendar_month_rounded, color: _primary),
                      const SizedBox(width: 12),
                      Text(
                        _startDate != null ? _formatDateTime(_startDate!) : '',
                        style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, color: _onSurface),
                      ),
                    ],
                  ),
                ),
              ),
              
              const SizedBox(height: 16),
              const Text('Ngày kết thúc', style: TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant, fontSize: 13)),
              const SizedBox(height: 4),
              Row(
                children: [
                  Expanded(
                    child: InkWell(
                      onTap: canEdit ? _pickEndDate : null,
                      borderRadius: BorderRadius.circular(12),
                      child: Container(
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          border: Border.all(color: _primaryContainer.withAlpha(80)),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Row(
                          children: [
                            const Icon(Icons.event_available_rounded, color: _primary),
                            const SizedBox(width: 12),
                            Text(
                              _endDate != null ? _formatDateOnly(_endDate!) : 'Không giới hạn',
                              style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, color: _onSurface),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                  if (_endDate != null && canEdit)
                    IconButton(
                      icon: const Icon(Icons.clear, color: _onSurfaceVariant),
                      onPressed: () => setState(() => _endDate = null),
                    ),
                ],
              ),
              
              const SizedBox(height: 16),
              const Text('Lặp lại', style: TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant, fontSize: 13)),
              const SizedBox(height: 4),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                decoration: BoxDecoration(
                  border: Border.all(color: _primaryContainer.withAlpha(80)),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: DropdownButtonHideUnderline(
                  child: DropdownButton<RecurrenceType>(
                    isExpanded: true,
                    value: _recurrence,
                    icon: const Icon(Icons.expand_more_rounded, color: _primary),
                    onChanged: canEdit ? (v) => setState(() => _recurrence = v ?? RecurrenceType.none) : null,
                    items: RecurrenceType.values.map((r) => DropdownMenuItem(
                      value: r,
                      child: Text(_recurrenceLabel(r), style: const TextStyle(fontFamily: 'Lexend', fontSize: 16)),
                    )).toList(),
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 24),
        FilledButton.icon(
          onPressed: canEdit ? _save : null,
          style: FilledButton.styleFrom(
            backgroundColor: _primaryContainer,
            minimumSize: const Size.fromHeight(54),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          ),
          icon: _processing ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)) : const Icon(Icons.save_rounded),
          label: const Text('Lưu thay đổi', style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w700, fontSize: 16)),
        ),
      ],
    );
  }

  static String _formatDateOnly(DateTime value) {
    final d = value.day.toString().padLeft(2, '0');
    final m = value.month.toString().padLeft(2, '0');
    return '$d/$m/${value.year}';
  }

  static String _formatDateTime(DateTime value) {
    final local = value.toLocal();
    final d = local.day.toString().padLeft(2, '0');
    final m = local.month.toString().padLeft(2, '0');
    final h = local.hour.toString().padLeft(2, '0');
    final min = local.minute.toString().padLeft(2, '0');
    return '$d/$m/${local.year} $h:$min';
  }

  static String _statusLabel(ReminderStatus status) {
    switch (status) {
      case ReminderStatus.done:
        return 'Đã hoàn thành';
      case ReminderStatus.snoozed:
        return 'Đã hoãn';
      case ReminderStatus.skipped:
        return 'Đã bỏ qua';
      case ReminderStatus.cancelled:
        return 'Đã tắt';
      case ReminderStatus.pending:
        return 'Đang chờ';
    }
  }

  static String _recurrenceLabel(RecurrenceType recurrence) {
    switch (recurrence) {
      case RecurrenceType.daily:
        return 'Hằng ngày';
      case RecurrenceType.weekly:
        return 'Hằng tuần';
      case RecurrenceType.monthly:
        return 'Hằng tháng';
      case RecurrenceType.none:
        return 'Không lặp lại';
    }
  }
}

class _InfoLine extends StatelessWidget {
  final String label;
  final String value;

  const _InfoLine({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: Row(
        children: [
          SizedBox(
            width: 92,
            child: Text(
              label,
              style: const TextStyle(fontFamily: 'Lexend', color: Color(0xFF524440)),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(
                fontFamily: 'Lexend',
                color: Color(0xFF271812),
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ActionButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool disabled;
  final VoidCallback onTap;

  const _ActionButton({
    required this.icon,
    required this.label,
    required this.disabled,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: OutlinedButton.icon(
        onPressed: disabled ? null : onTap,
        icon: Icon(icon),
        label: Text(label, style: const TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w700)),
        style: OutlinedButton.styleFrom(
          foregroundColor: const Color(0xFF845143),
          side: const BorderSide(color: Color(0xFFC98C7B)),
          minimumSize: const Size.fromHeight(52),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          alignment: Alignment.centerLeft,
        ),
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const _ErrorState({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline_rounded, color: Color(0xFFBA1A1A), size: 44),
          const SizedBox(height: 10),
          Text(message, style: const TextStyle(fontFamily: 'Lexend')),
          const SizedBox(height: 10),
          TextButton(onPressed: onRetry, child: const Text('Thử lại')),
        ],
      ),
    );
  }
}
