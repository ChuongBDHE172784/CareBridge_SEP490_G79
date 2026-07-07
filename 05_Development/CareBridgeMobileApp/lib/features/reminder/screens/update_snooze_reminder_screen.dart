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
  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  final _service = ReminderService.instance;
  Reminder? _reminder;
  bool _isLoading = true;
  bool _isProcessing = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    if (widget.initialReminder != null) {
      _reminder = widget.initialReminder;
      _isLoading = false;
    } else {
      _loadReminder();
    }
  }

  Future<void> _loadReminder() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      final r = await _service.getReminderDetail(widget.reminderId);
      setState(() => _reminder = r);
    } catch (_) {
      setState(() => _error = 'Không thể tải nhắc nhở. Vui lòng thử lại.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showActionSheet() {
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.white,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
      ),
      builder: (_) => _ReminderActionSheet(
        reminder: _reminder!,
        onComplete: _handleComplete,
        onSkip: _handleSkip,
        onSnooze: _handleSnooze,
        onReschedule: _handleReschedule,
      ),
    );
  }

  Future<void> _handleComplete(bool entireSeries) async {
    Navigator.of(context).pop();
    await _doAction(() => _service.completeReminder(widget.reminderId));
  }

  Future<void> _handleSkip(bool entireSeries) async {
    Navigator.of(context).pop();
    await _doAction(() => _service.skipReminder(widget.reminderId));
  }

  Future<void> _handleSnooze(Duration delay) async {
    Navigator.of(context).pop();
    final until = DateTime.now().add(delay);
    await _doAction(() => _service.snoozeReminder(widget.reminderId, until));
  }

  Future<void> _handleReschedule(DateTime newTime) async {
    Navigator.of(context).pop();
    await _doAction(() => _service.updateReminder(widget.reminderId, scheduledAt: newTime));
  }

  Future<void> _doAction(Future<Reminder> Function() action) async {
    setState(() => _isProcessing = true);
    try {
      final updated = await action();
      setState(() => _reminder = updated);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(_statusLabel(updated.status), style: const TextStyle(fontFamily: 'Lexend')),
            backgroundColor: _primary,
            behavior: SnackBarBehavior.floating,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          ),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Không thể thực hiện. Vui lòng thử lại.'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isProcessing = false);
    }
  }

  String _statusLabel(ReminderStatus s) {
    switch (s) {
      case ReminderStatus.done: return '✓ Đã hoàn thành';
      case ReminderStatus.skipped: return 'Đã bỏ qua';
      case ReminderStatus.snoozed: return 'Đã hoãn';
      default: return 'Đã cập nhật';
    }
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
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          'Chi tiết nhắc nhở',
          style: TextStyle(fontFamily: 'Lexend', fontSize: 18, fontWeight: FontWeight.w700, color: _onSurface),
        ),
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator(color: _primaryContainer));
    }
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline_rounded, color: _primaryContainer, size: 48),
            const SizedBox(height: 12),
            Text(_error!, style: const TextStyle(fontFamily: 'Lexend', fontSize: 13, color: _onSurfaceVariant)),
            const SizedBox(height: 12),
            TextButton(onPressed: _loadReminder, child: const Text('Thử lại', style: TextStyle(color: _primary))),
          ],
        ),
      );
    }
    final r = _reminder!;
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 40),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _buildStatusChip(r),
          const SizedBox(height: 12),
          _buildHeroCard(r),
          const SizedBox(height: 16),
          _buildBentoGrid(r),
          const SizedBox(height: 24),
          _buildActionButton(),
        ],
      ),
    );
  }

  Widget _buildStatusChip(Reminder r) {
    final isPending = r.status == ReminderStatus.pending;
    return Row(
      children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
          decoration: BoxDecoration(
            color: isPending ? const Color(0xFFE8F5E9) : _surface,
            borderRadius: BorderRadius.circular(50),
          ),
          child: Text(
            isPending ? 'Đang diễn ra' : r.status.displayLabel,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: isPending ? const Color(0xFF2E7D32) : _onSurfaceVariant,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildHeroCard(Reminder r) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [_primary, _primaryContainer],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(28),
        boxShadow: [BoxShadow(color: _primary.withAlpha(60), blurRadius: 20, offset: const Offset(0, 8))],
      ),
      child: Row(
        children: [
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(color: Colors.white.withAlpha(40), borderRadius: BorderRadius.circular(18)),
            child: Icon(_reminderIcon(r.reminderType), color: Colors.white, size: 28),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  r.title,
                  style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w700, color: Colors.white),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 6),
                Row(
                  children: [
                    const Icon(Icons.schedule_rounded, color: Colors.white70, size: 14),
                    const SizedBox(width: 4),
                    Text(
                      _formatDateTime(r.scheduledAt),
                      style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, color: Colors.white70),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBentoGrid(Reminder r) {
    return Row(
      children: [
        Expanded(
          child: _buildBentoItem(
            icon: Icons.repeat_rounded,
            label: 'Tần suất',
            value: r.recurrenceType.displayLabel,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildBentoItem(
            icon: Icons.info_outline_rounded,
            label: 'Trạng thái',
            value: r.status.displayLabel,
          ),
        ),
      ],
    );
  }

  Widget _buildBentoItem({required IconData icon, required String label, required String value}) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [BoxShadow(color: _primary.withAlpha(12), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: _primaryContainer, size: 20),
          const SizedBox(height: 8),
          Text(value, style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, fontWeight: FontWeight.w700, color: _onSurface)),
          const SizedBox(height: 2),
          Text(label, style: const TextStyle(fontFamily: 'Lexend', fontSize: 11, color: _onSurfaceVariant)),
        ],
      ),
    );
  }

  Widget _buildActionButton() {
    final isDone = _reminder?.status == ReminderStatus.done || _reminder?.status == ReminderStatus.skipped;
    return ElevatedButton.icon(
      onPressed: (_isProcessing || isDone) ? null : _showActionSheet,
      icon: _isProcessing
          ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2.5, color: Colors.white))
          : const Icon(Icons.bolt_rounded, size: 20),
      label: Text(
        isDone ? 'Đã xử lý' : 'Xử lý nhắc lịch này',
        style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w700),
      ),
      style: ElevatedButton.styleFrom(
        backgroundColor: _primary,
        foregroundColor: Colors.white,
        disabledBackgroundColor: _surface,
        padding: const EdgeInsets.symmetric(vertical: 16),
        shape: const StadiumBorder(),
        elevation: 0,
      ),
    );
  }

  IconData _reminderIcon(ReminderType t) {
    switch (t) {
      case ReminderType.medication: return Icons.medication_rounded;
      case ReminderType.vaccination: return Icons.vaccines_rounded;
      case ReminderType.appointment: return Icons.event_rounded;
      default: return Icons.notifications_rounded;
    }
  }

  String _formatDateTime(DateTime dt) {
    return '${dt.day.toString().padLeft(2, '0')}/${dt.month.toString().padLeft(2, '0')}/${dt.year} ${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
  }
}

class _ReminderActionSheet extends StatefulWidget {
  final Reminder reminder;
  final void Function(bool entireSeries) onComplete;
  final void Function(bool entireSeries) onSkip;
  final void Function(Duration delay) onSnooze;
  final void Function(DateTime newTime) onReschedule;

  const _ReminderActionSheet({
    required this.reminder,
    required this.onComplete,
    required this.onSkip,
    required this.onSnooze,
    required this.onReschedule,
  });

  @override
  State<_ReminderActionSheet> createState() => _ReminderActionSheetState();
}

class _ReminderActionSheetState extends State<_ReminderActionSheet> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  bool _entireSeries = false;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 12, 24, 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(
              child: Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(color: const Color(0xFFE0D8D5), borderRadius: BorderRadius.circular(2)),
              ),
            ),
            const SizedBox(height: 20),
            _buildScopeToggle(),
            const SizedBox(height: 20),
            const Text('Hoãn lại', style: TextStyle(fontFamily: 'Lexend', fontSize: 13, fontWeight: FontWeight.w600, color: _onSurface)),
            const SizedBox(height: 10),
            _buildSnoozeGrid(),
            const SizedBox(height: 16),
            const Text('Hành động', style: TextStyle(fontFamily: 'Lexend', fontSize: 13, fontWeight: FontWeight.w600, color: _onSurface)),
            const SizedBox(height: 10),
            Row(
              children: [
                Expanded(child: _buildActionBtn(Icons.check_circle_outline_rounded, 'Hoàn thành', const Color(0xFF4CAF50), () => widget.onComplete(_entireSeries))),
                const SizedBox(width: 10),
                Expanded(child: _buildActionBtn(Icons.skip_next_rounded, 'Bỏ qua', Colors.orange, () => widget.onSkip(_entireSeries))),
                const SizedBox(width: 10),
                Expanded(child: _buildActionBtn(Icons.access_time_rounded, 'Đổi giờ', _primary, () => _pickReschedule())),
              ],
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }

  Widget _buildScopeToggle() {
    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(color: _surface, borderRadius: BorderRadius.circular(50)),
      child: Row(
        children: [
          Expanded(child: _scopeBtn('CHỈ LẦN NÀY', false)),
          Expanded(child: _scopeBtn('CẢ CHUỖI', true)),
        ],
      ),
    );
  }

  Widget _scopeBtn(String label, bool entireSeries) {
    final selected = _entireSeries == entireSeries;
    return GestureDetector(
      onTap: () => setState(() => _entireSeries = entireSeries),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: const EdgeInsets.symmetric(vertical: 10),
        decoration: BoxDecoration(
          color: selected ? _primary : Colors.transparent,
          borderRadius: BorderRadius.circular(50),
        ),
        alignment: Alignment.center,
        child: Text(
          label,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 12,
            fontWeight: FontWeight.w700,
            color: selected ? Colors.white : _onSurfaceVariant,
          ),
        ),
      ),
    );
  }

  Widget _buildSnoozeGrid() {
    final options = [
      ('15 phút', const Duration(minutes: 15)),
      ('1 giờ', const Duration(hours: 1)),
      ('Tuỳ chỉnh', null),
    ];
    return Row(
      children: options.map((opt) {
        return Expanded(
          child: Padding(
            padding: EdgeInsets.only(right: opt == options.last ? 0 : 8),
            child: GestureDetector(
              onTap: () async {
                if (opt.$2 != null) {
                  widget.onSnooze(opt.$2!);
                } else {
                  final picked = await showTimePicker(
                    context: context,
                    initialTime: TimeOfDay.now(),
                    builder: (ctx, child) => Theme(
                      data: Theme.of(ctx).copyWith(
                        colorScheme: const ColorScheme.light(primary: _primary, onPrimary: Colors.white),
                      ),
                      child: child!,
                    ),
                  );
                  if (picked != null) {
                    final now = DateTime.now();
                    final target = DateTime(now.year, now.month, now.day, picked.hour, picked.minute);
                    final diff = target.isAfter(now) ? target.difference(now) : target.add(const Duration(days: 1)).difference(now);
                    widget.onSnooze(diff);
                  }
                }
              },
              child: Container(
                padding: const EdgeInsets.symmetric(vertical: 14),
                decoration: BoxDecoration(
                  color: _surface,
                  borderRadius: BorderRadius.circular(16),
                ),
                alignment: Alignment.center,
                child: Text(
                  opt.$1,
                  style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, fontWeight: FontWeight.w600, color: _onSurface),
                ),
              ),
            ),
          ),
        );
      }).toList(),
    );
  }

  Widget _buildActionBtn(IconData icon, String label, Color color, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 14),
        decoration: BoxDecoration(
          color: color.withAlpha(25),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: color.withAlpha(60), width: 1.5),
        ),
        child: Column(
          children: [
            Icon(icon, color: color, size: 22),
            const SizedBox(height: 4),
            Text(label, style: TextStyle(fontFamily: 'Lexend', fontSize: 11, fontWeight: FontWeight.w600, color: color)),
          ],
        ),
      ),
    );
  }

  Future<void> _pickReschedule() async {
    final date = await showDatePicker(
      context: context,
      initialDate: widget.reminder.scheduledAt,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365)),
      builder: (ctx, child) => Theme(
        data: Theme.of(ctx).copyWith(
          colorScheme: const ColorScheme.light(primary: _primary, onPrimary: Colors.white),
        ),
        child: child!,
      ),
    );
    if (date == null || !mounted) return;
    final time = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(widget.reminder.scheduledAt),
    );
    if (time == null) return;
    widget.onReschedule(DateTime(date.year, date.month, date.day, time.hour, time.minute));
  }
}
