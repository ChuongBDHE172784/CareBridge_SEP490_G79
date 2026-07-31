import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/appointment_notification_timing.dart';
import '../models/reminder_model.dart';
import '../services/reminder_service.dart';

class ReminderDetailScreen extends StatefulWidget {
  final String reminderId;

  const ReminderDetailScreen({super.key, required this.reminderId});

  @override
  State<ReminderDetailScreen> createState() => _ReminderDetailScreenState();
}

class _ReminderDetailScreenState extends State<ReminderDetailScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF271812);
  static const _error = Color(0xFFBA1A1A);

  final _service = ReminderService.instance;
  Reminder? _reminder;
  bool _loading = true;
  bool _processing = false;
  String? _errorText;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _errorText = null;
    });
    try {
      final reminder = await _service.getReminderDetail(widget.reminderId);
      if (!mounted) return;
      setState(() {
        _reminder = reminder;
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

  Future<bool> _confirm({
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
        content: Text(message, style: const TextStyle(fontFamily: 'Lexend')),
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

  Future<void> _runAction(Future<void> Function() action) async {
    setState(() => _processing = true);
    try {
      await action();
      if (!mounted) return;
      Navigator.pop(context, true);
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

  Future<void> _complete() async {
    final ok = await _confirm(
      title: 'Hoàn thành nhắc lịch?',
      message: 'Chỉ đánh dấu nhắc lịch này là đã hoàn thành.',
      confirmLabel: 'Hoàn thành',
    );
    if (!ok) return;
    await _runAction(() async {
      await _service.completeReminder(widget.reminderId);
    });
  }

  Future<void> _skip() async {
    final ok = await _confirm(
      title: 'Bỏ qua nhắc lịch?',
      message: 'Chỉ bỏ qua lần nhắc lịch này.',
      confirmLabel: 'Bỏ qua',
    );
    if (!ok) return;
    await _runAction(() async {
      await _service.skipReminder(widget.reminderId);
    });
  }

  Future<void> _delete() async {
    final ok = await _confirm(
      title: 'Tắt nhắc lịch?',
      message: 'Chỉ hủy nhắc lịch này để không còn xuất hiện khi đến hạn.',
      confirmLabel: 'Tắt',
      confirmColor: _error,
    );
    if (!ok) return;
    await _runAction(() async {
      await _service.deleteReminder(widget.reminderId);
    });
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
          'Chi tiết nhắc lịch',
          style: TextStyle(
            fontFamily: 'Lexend',
            color: _onSurface,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: _primary))
          : _errorText != null
          ? _ErrorState(message: _errorText!, onRetry: _load)
          : _ReminderContent(
              reminder: _reminder!,
              processing: _processing,
              onComplete: _complete,
              onEdit: () async {
                final changed = await context.push(
                  '/reminders/${_reminder!.id}/manage',
                  extra: _reminder,
                );
                if (!context.mounted) return;
                if (changed == 'deleted') {
                  Navigator.pop(context, true);
                  return;
                }
                if (changed == true) _load();
              },
              onDelete: _delete,
              onSkip: _skip,
            ),
    );
  }
}

class _ReminderContent extends StatelessWidget {
  final Reminder reminder;
  final bool processing;
  final VoidCallback onComplete;
  final VoidCallback onEdit;
  final VoidCallback onDelete;
  final VoidCallback onSkip;

  const _ReminderContent({
    required this.reminder,
    required this.processing,
    required this.onComplete,
    required this.onEdit,
    required this.onDelete,
    required this.onSkip,
  });

  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surface = Colors.white;
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _error = Color(0xFFBA1A1A);

  @override
  Widget build(BuildContext context) {
    final isTerminal = reminder.status.isTerminal;
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
      children: [
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
              Row(
                children: [
                  CircleAvatar(
                    backgroundColor: _primaryContainer.withAlpha(35),
                    child: Icon(
                      _iconFor(reminder.reminderType),
                      color: _primary,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          reminder.title,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 20,
                            fontWeight: FontWeight.w800,
                            color: _onSurface,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          _typeLabel(reminder.reminderType),
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 13,
                            color: _onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              _InfoRow(
                icon: Icons.calendar_month_rounded,
                label: 'Ngày bắt đầu',
                value: _formatDateOnly(reminder.scheduledAt),
              ),
              _InfoRow(
                icon: Icons.schedule_rounded,
                label: 'Giờ nhắc',
                value: _formatTimeOnly(reminder.scheduledAt),
              ),
              _InfoRow(
                icon: Icons.event_available_rounded,
                label: 'Ngày kết thúc',
                value: _formatEndDate(reminder),
              ),
              _InfoRow(
                icon: Icons.repeat_rounded,
                label: 'Lặp lại',
                value: _recurrenceLabel(reminder.recurrenceType),
              ),
              _InfoRow(
                icon: Icons.flag_rounded,
                label: 'Trạng thái',
                value: _statusLabel(reminder.status),
              ),
              if (reminder.reminderType == ReminderType.appointment)
                _InfoRow(
                  icon: Icons.notifications_active_rounded,
                  label: 'Thông báo',
                  value: reminder.notificationOffsetsMinutes.isEmpty
                      ? 'Đã tắt'
                      : reminder.notificationOffsetsMinutes
                            .map(AppointmentNotificationTiming.label)
                            .join(', '),
                ),
              if (reminder.location != null &&
                  reminder.location!.trim().isNotEmpty)
                _InfoRow(
                  icon: Icons.place_rounded,
                  label: 'Địa điểm',
                  value: reminder.location!,
                ),
              if (reminder.note != null &&
                  reminder.note!.trim().isNotEmpty) ...[
                const SizedBox(height: 12),
                const Text(
                  'Hướng dẫn',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  reminder.note!,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    color: _onSurfaceVariant,
                    height: 1.5,
                  ),
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: 18),
        if (isTerminal)
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: _surface,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: _primaryContainer.withAlpha(70)),
            ),
            child: Text(
              'Nhắc lịch này đang ở trạng thái ${_statusLabel(reminder.status).toLowerCase()} và không thể chỉnh sửa.',
              style: const TextStyle(
                fontFamily: 'Lexend',
                color: _onSurfaceVariant,
                fontWeight: FontWeight.w700,
              ),
            ),
          )
        else ...[
          FilledButton.icon(
            onPressed: processing ? null : onComplete,
            style: FilledButton.styleFrom(
              backgroundColor: _primaryContainer,
              minimumSize: const Size.fromHeight(54),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
            icon: const Icon(Icons.check_circle_outline_rounded),
            label: const Text(
              'Hoàn thành',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          const SizedBox(height: 18),
          Row(
            children: [
              Expanded(
                child: _SmallAction(
                  icon: Icons.block_rounded,
                  label: 'Bỏ qua',
                  onTap: processing ? null : onSkip,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: _SmallAction(
                  icon: Icons.delete_outline_rounded,
                  label: 'Tắt',
                  color: _error,
                  onTap: processing ? null : onDelete,
                ),
              ),
            ],
          ),
        ],
      ],
    );
  }

  static IconData _iconFor(ReminderType type) {
    switch (type) {
      case ReminderType.medication:
        return Icons.medication_rounded;
      case ReminderType.vaccination:
        return Icons.vaccines_rounded;
      case ReminderType.appointment:
        return Icons.event_rounded;
      case ReminderType.task:
        return Icons.task_alt_rounded;
      case ReminderType.other:
        return Icons.notifications_rounded;
    }
  }

  static String _typeLabel(ReminderType type) {
    switch (type) {
      case ReminderType.medication:
        return 'Nhắc thuốc hoặc vitamin';
      case ReminderType.vaccination:
        return 'Nhắc lịch tiêm chủng';
      case ReminderType.appointment:
        return 'Nhắc lịch hẹn hoặc tái khám';
      case ReminderType.task:
        return 'Nhắc việc';
      case ReminderType.other:
        return 'Nhắc lịch';
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

  static String _formatEndDate(Reminder reminder) {
    if (reminder.recurrenceType == RecurrenceType.none) {
      return 'Không áp dụng';
    }
    final endDate = reminder.recurrenceEndDate;
    if (endDate == null) {
      return 'Không giới hạn';
    }
    return _formatDateOnly(endDate);
  }
}

class _InfoRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;

  const _InfoRow({
    required this.icon,
    required this.label,
    required this.value,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 12),
      child: Row(
        children: [
          Icon(icon, size: 20, color: const Color(0xFF845143)),
          const SizedBox(width: 10),
          SizedBox(
            width: 82,
            child: Text(
              label,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                color: Color(0xFF524440),
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontWeight: FontWeight.w700,
                color: Color(0xFF271812),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _SmallAction extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback? onTap;
  final Color color;

  const _SmallAction({
    required this.icon,
    required this.label,
    required this.onTap,
    this.color = const Color(0xFF845143),
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        height: 76,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: color.withAlpha(55)),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: color),
            const SizedBox(height: 6),
            Text(
              label,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontWeight: FontWeight.w700,
                color: color,
              ),
            ),
          ],
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
