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
  State<UpdateSnoozeReminderScreen> createState() =>
      _UpdateSnoozeReminderScreenState();
}

class _UpdateSnoozeReminderScreenState
    extends State<UpdateSnoozeReminderScreen> {
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
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Đã cập nhật nhắc lịch')));
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Thao tác thất bại: $e'),
          backgroundColor: _error,
        ),
      );
    } finally {
      if (mounted) setState(() => _processing = false);
    }
  }

  Future<bool> _confirmAction({
    required String title,
    required String message,
    required String confirmLabel,
    Color confirmColor = _primaryContainer,
  }) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Text(
          title,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontWeight: FontWeight.w700,
          ),
        ),
        content: Text(
          message,
          style: const TextStyle(fontFamily: 'Lexend'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy', style: TextStyle(fontFamily: 'Lexend')),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: confirmColor),
            child: Text(
              confirmLabel,
              style: const TextStyle(fontFamily: 'Lexend'),
            ),
          ),
        ],
      ),
    );
    return ok == true;
  }

  Future<void> _disableReminder() async {
    if (_reminder == null) return;
    final ok = await _confirmAction(
      title: 'Tắt nhắc lịch?',
      message: 'Nhắc lịch này sẽ không còn xuất hiện khi đến hạn.',
      confirmLabel: 'Tắt',
      confirmColor: _error,
    );
    if (!ok) return;

    setState(() => _processing = true);
    try {
      await _service.deleteReminder(widget.reminderId);
      final reminder = await _service.getReminderDetail(widget.reminderId);
      if (!mounted) return;
      setState(() => _reminder = reminder);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã tắt nhắc lịch')),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Thao tác thất bại: $e'),
          backgroundColor: _error,
        ),
      );
    } finally {
      if (mounted) setState(() => _processing = false);
    }
  }

  Future<void> _enableReminder() async {
    if (_reminder == null) return;
    final ok = await _confirmAction(
      title: 'Mở nhắc lịch?',
      message: 'Nhắc lịch sẽ hoạt động trở lại theo lịch đã đặt.',
      confirmLabel: 'Mở',
    );
    if (!ok) return;

    setState(() => _processing = true);
    try {
      final reminder = await _service.enableReminder(widget.reminderId);
      if (!mounted) return;
      setState(() {
        _reminder = reminder;
        _titleController.text = reminder.title;
        _startDate = reminder.scheduledAt.toLocal();
        _endDate = reminder.recurrenceEndDate?.toLocal();
        _recurrence = reminder.recurrenceType;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã mở nhắc lịch')),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Thao tác thất bại: $e'),
          backgroundColor: _error,
        ),
      );
    } finally {
      if (mounted) setState(() => _processing = false);
    }
  }

  Future<void> _hardDeleteReminder() async {
    if (_reminder == null) return;
    final ok = await _confirmAction(
      title: 'Xóa nhắc lịch?',
      message: 'Nhắc lịch sẽ bị xóa vĩnh viễn khỏi hệ thống và không thể khôi phục.',
      confirmLabel: 'Xóa',
      confirmColor: _error,
    );
    if (!ok) return;

    setState(() => _processing = true);
    try {
      await _service.hardDeleteReminder(widget.reminderId);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã xóa nhắc lịch')),
      );
      Navigator.pop(context, 'deleted');
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Thao tác thất bại: $e'),
          backgroundColor: _error,
        ),
      );
    } finally {
      if (mounted) setState(() => _processing = false);
    }
  }

  Future<void> _pickStartDate() async {
    if (_startDate == null) return;
    final current = _startDate!;
    final today = _dateOnly(DateTime.now());
    final currentDate = _dateOnly(current);
    final endDate = _endDate == null ? null : _dateOnly(_endDate!);
    var firstDate = currentDate.isBefore(today) ? currentDate : today;
    final defaultLastDate = today.add(const Duration(days: 365 * 2));
    final lastDate = endDate ??
        (currentDate.isAfter(defaultLastDate) ? currentDate : defaultLastDate);
    if (firstDate.isAfter(lastDate)) {
      firstDate = lastDate;
    }
    final date = await showDatePicker(
      context: context,
      initialDate: _clampDate(currentDate, firstDate, lastDate),
      firstDate: firstDate,
      lastDate: lastDate,
    );
    if (date == null || !mounted) return;
    setState(() {
      _startDate = DateTime(
        date.year,
        date.month,
        date.day,
        current.hour,
        current.minute,
      );
    });
  }

  Future<void> _pickStartTime() async {
    if (_startDate == null) return;
    final current = _startDate!;
    final time = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(current),
    );
    if (time == null || !mounted) return;
    setState(() {
      _startDate = DateTime(
        current.year,
        current.month,
        current.day,
        time.hour,
        time.minute,
      );
    });
  }

  Future<void> _pickEndDate() async {
    final startDate = _dateOnly(_startDate ?? DateTime.now());
    final current = _dateOnly(_endDate ?? startDate);
    final defaultLastDate = DateTime.now().add(const Duration(days: 365 * 5));
    final lastDate = startDate.isAfter(defaultLastDate)
        ? startDate
        : defaultLastDate;
    final date = await showDatePicker(
      context: context,
      initialDate: _clampDate(current, startDate, lastDate),
      firstDate: startDate,
      lastDate: lastDate,
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
        const SnackBar(
          content: Text('Tên nhắc nhở không được để trống'),
          backgroundColor: _error,
        ),
      );
      return;
    }

    if (_endDate != null &&
        _dateOnly(_startDate!).isAfter(_dateOnly(_endDate!))) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Ngày bắt đầu không được sau ngày kết thúc'),
          backgroundColor: _error,
        ),
      );
      return;
    }

    final titleChanged = _reminder!.title != _titleController.text.trim();
    final dateChanged = !_reminder!.scheduledAt.isAtSameMomentAs(_startDate!);
    final recurrenceChanged = _reminder!.recurrenceType != _recurrence;

    bool endDateChanged = false;
    if (_reminder!.recurrenceEndDate == null && _endDate != null) {
      endDateChanged = true;
    }
    if (_reminder!.recurrenceEndDate != null && _endDate == null) {
      endDateChanged = true;
    }
    if (_reminder!.recurrenceEndDate != null && _endDate != null) {
      endDateChanged = !_reminder!.recurrenceEndDate!.isAtSameMomentAs(
        _endDate!,
      );
    }

    await _run(
      () => _service.updateReminder(
        widget.reminderId,
        title: titleChanged ? _titleController.text.trim() : null,
        scheduledAt: dateChanged ? _startDate!.toUtc() : null,
        recurrenceType: recurrenceChanged ? _recurrence : null,
        recurrenceEndDate: endDateChanged ? _endDate?.toUtc() : null,
        recurrenceEndDateSet: endDateChanged,
      ),
    );
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
          style: TextStyle(
            fontFamily: 'Lexend',
            color: _onSurface,
            fontWeight: FontWeight.w700,
          ),
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
    final isDisabled = reminder.status == ReminderStatus.cancelled;
    final toggleLabel = isDisabled ? 'Mở nhắc lịch' : 'Tắt nhắc lịch';
    final toggleIcon = isDisabled
        ? Icons.notifications_active_rounded
        : Icons.notifications_off_rounded;
    final toggleColor = isDisabled ? _primary : _error;
    final VoidCallback toggleAction = isDisabled
        ? () {
            _enableReminder();
          }
        : () {
            _disableReminder();
          };

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
              BoxShadow(
                color: _primary.withAlpha(18),
                blurRadius: 18,
                offset: const Offset(0, 8),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Nội dung / Tên',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  color: _onSurfaceVariant,
                  fontSize: 13,
                ),
              ),
              const SizedBox(height: 4),
              TextFormField(
                controller: _titleController,
                enabled: canEdit,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  color: _onSurface,
                  fontWeight: FontWeight.w600,
                ),
                decoration: InputDecoration(
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 16,
                  ),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide(
                      color: _primaryContainer.withAlpha(80),
                    ),
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide(
                      color: _primaryContainer.withAlpha(80),
                    ),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: _primary),
                  ),
                ),
              ),
              const SizedBox(height: 20),

              const Text(
                'Ngày bắt đầu',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  color: _onSurfaceVariant,
                  fontSize: 13,
                ),
              ),
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
                        _startDate != null ? _formatDateOnly(_startDate!) : '',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 16,
                          color: _onSurface,
                        ),
                      ),
                    ],
                  ),
                ),
              ),

              const SizedBox(height: 16),
              const Text(
                'Giờ nhắc',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  color: _onSurfaceVariant,
                  fontSize: 13,
                ),
              ),
              const SizedBox(height: 4),
              InkWell(
                onTap: canEdit ? _pickStartTime : null,
                borderRadius: BorderRadius.circular(12),
                child: Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    border: Border.all(color: _primaryContainer.withAlpha(80)),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.schedule_rounded, color: _primary),
                      const SizedBox(width: 12),
                      Text(
                        _startDate != null ? _formatTimeOnly(_startDate!) : '',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 16,
                          color: _onSurface,
                        ),
                      ),
                    ],
                  ),
                ),
              ),

              const SizedBox(height: 16),
              const Text(
                'Ngày kết thúc',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  color: _onSurfaceVariant,
                  fontSize: 13,
                ),
              ),
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
                          border: Border.all(
                            color: _primaryContainer.withAlpha(80),
                          ),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Row(
                          children: [
                            const Icon(
                              Icons.event_available_rounded,
                              color: _primary,
                            ),
                            const SizedBox(width: 12),
                            Text(
                              _endDate != null
                                  ? _formatDateOnly(_endDate!)
                                  : 'Không giới hạn',
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 16,
                                color: _onSurface,
                              ),
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
              const Text(
                'Lặp lại',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  color: _onSurfaceVariant,
                  fontSize: 13,
                ),
              ),
              const SizedBox(height: 4),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  border: Border.all(color: _primaryContainer.withAlpha(80)),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: DropdownButtonHideUnderline(
                  child: DropdownButton<RecurrenceType>(
                    isExpanded: true,
                    value: _recurrence,
                    icon: const Icon(
                      Icons.expand_more_rounded,
                      color: _primary,
                    ),
                    onChanged: canEdit
                        ? (v) => setState(
                            () => _recurrence = v ?? RecurrenceType.none,
                          )
                        : null,
                    items: RecurrenceType.values
                        .map(
                          (r) => DropdownMenuItem(
                            value: r,
                            child: Text(
                              _recurrenceLabel(r),
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 16,
                              ),
                            ),
                          ),
                        )
                        .toList(),
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
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
          ),
          icon: _processing
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: Colors.white,
                  ),
                )
              : const Icon(Icons.save_rounded),
          label: const Text(
            'Lưu thay đổi',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontWeight: FontWeight.w700,
              fontSize: 16,
            ),
          ),
        ),
        const SizedBox(height: 12),
        OutlinedButton.icon(
          onPressed: _processing ? null : toggleAction,
          style: OutlinedButton.styleFrom(
            foregroundColor: toggleColor,
            side: BorderSide(color: toggleColor),
            minimumSize: const Size.fromHeight(54),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
          ),
          icon: Icon(toggleIcon),
          label: Text(
            toggleLabel,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontWeight: FontWeight.w700,
              fontSize: 16,
            ),
          ),
        ),
        const SizedBox(height: 12),
        OutlinedButton.icon(
          onPressed: _processing ? null : _hardDeleteReminder,
          style: OutlinedButton.styleFrom(
            foregroundColor: _error,
            side: BorderSide(color: _error.withAlpha(180)),
            minimumSize: const Size.fromHeight(54),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
          ),
          icon: const Icon(Icons.delete_forever_rounded),
          label: const Text(
            'Xóa nhắc lịch',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontWeight: FontWeight.w700,
              fontSize: 16,
            ),
          ),
        ),
      ],
    );
  }

  static String _formatDateOnly(DateTime value) {
    final local = value.toLocal();
    final d = local.day.toString().padLeft(2, '0');
    final m = local.month.toString().padLeft(2, '0');
    return '$d/$m/${local.year}';
  }

  static String _formatTimeOnly(DateTime value) {
    final local = value.toLocal();
    final h = local.hour.toString().padLeft(2, '0');
    final min = local.minute.toString().padLeft(2, '0');
    return '$h:$min';
  }

  static DateTime _dateOnly(DateTime value) {
    final local = value.toLocal();
    return DateTime(local.year, local.month, local.day);
  }

  static DateTime _clampDate(
    DateTime value,
    DateTime firstDate,
    DateTime lastDate,
  ) {
    if (value.isBefore(firstDate)) return firstDate;
    if (value.isAfter(lastDate)) return lastDate;
    return value;
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
          const Icon(
            Icons.error_outline_rounded,
            color: Color(0xFFBA1A1A),
            size: 44,
          ),
          const SizedBox(height: 10),
          Text(message, style: const TextStyle(fontFamily: 'Lexend')),
          const SizedBox(height: 10),
          TextButton(onPressed: onRetry, child: const Text('Thử lại')),
        ],
      ),
    );
  }
}
