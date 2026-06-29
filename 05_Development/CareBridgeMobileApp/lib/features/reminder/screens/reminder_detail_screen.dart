import 'package:flutter/material.dart';
import '../models/reminder_model.dart';
import '../services/reminder_service.dart';
import '../../../core/network/api_client.dart';

/// CB-167 — Reminder Detail (UC-212, UC-213, UC-214, UC-215)
/// Shows full reminder detail: hero card, bento info grid, notes, action buttons.
/// Calls GET /api/v1/reminders/{reminderId}.
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
  static const _secondary = Color(0xFF6E5A52);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFF84736F);
  static const _surfaceContainerLowest = Colors.white;
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _errorContainer = Color(0xFFFFDAD6);
  static const _error = Color(0xFFBA1A1A);

  final _service = ReminderService.instance;
  Reminder? _reminder;
  bool _loading = true;
  String? _error2;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() { _loading = true; _error2 = null; });
    try {
      final r = await _service.getReminderDetail(widget.reminderId);
      if (mounted) {
        setState(() { _reminder = r; _loading = false; });
      }
    } on ApiException {
      // fallback to mock for development
      if (mounted) {
        setState(() {
          _reminder = Reminder(
            id: widget.reminderId,
            reminderType: ReminderType.vaccination,
            title: 'Tiêm ngừa mũi 5 trong 1',
            scheduledAt: DateTime(2023, 10, 25, 8, 0),
            recurrenceType: RecurrenceType.monthly,
            status: ReminderStatus.pending,
            note: 'Nhớ mang theo sổ tiêm chủng của bé. Cho bé ăn nhẹ trước khi đi tiêm. Theo dõi nhiệt độ sau tiêm.',
            assignee: ReminderAssignee.baby,
          );
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() { _error2 = 'Lỗi kết nối.'; _loading = false; });
      }
    }
  }

  Future<void> _markDone() async {
    await _service.markDone(widget.reminderId);
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Đã hoàn thành!')));
    Navigator.pop(context);
  }

  Future<void> _snooze() async {
    await _service.snooze(widget.reminderId);
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Đã hoãn lại.')));
    Navigator.pop(context);
  }

  Future<void> _skip() async {
    await _service.skip(widget.reminderId);
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Đã bỏ qua.')));
    Navigator.pop(context);
  }

  Future<void> _delete() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text('Xóa nhắc nhở?', style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600)),
        content: const Text('Hành động này không thể khôi phục.', style: TextStyle(fontFamily: 'Lexend')),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false),
              child: const Text('Hủy', style: TextStyle(fontFamily: 'Lexend'))),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: _error),
            child: const Text('Xóa', style: TextStyle(fontFamily: 'Lexend')),
          ),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    try {
      await _service.deleteReminder(widget.reminderId);
      if (!mounted) return;
      Navigator.pop(context, true);
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không thể xóa. Vui lòng thử lại.')));
    }
  }

  String _formatDateTime(DateTime dt) {
    const wd = ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ Nhật'];
    final h = dt.hour.toString().padLeft(2, '0');
    final m = dt.minute.toString().padLeft(2, '0');
    return '$h:$m - ${wd[dt.weekday - 1]}';
  }

  String _formatDate(DateTime dt) =>
      '${dt.day} Tháng ${dt.month}, ${dt.year}';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: _loading
            ? const Center(child: CircularProgressIndicator(color: _primaryContainer))
            : _error2 != null
                ? _buildError()
                : _buildContent(_reminder!),
      ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, size: 48, color: _error),
          const SizedBox(height: 12),
          Text(_error2!, style: const TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant)),
          TextButton(onPressed: _load,
              child: const Text('Thử lại', style: TextStyle(fontFamily: 'Lexend', color: _primary))),
        ],
      ),
    );
  }

  Widget _buildContent(Reminder r) {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
      child: Column(
        children: [
          _buildAppBar(r),
          _buildHeroCard(r),
          const SizedBox(height: 16),
          _buildBentoGrid(r),
          if (r.note != null && r.note!.isNotEmpty) ...[
            const SizedBox(height: 16),
            _buildNoteCard(r.note!),
          ],
          const SizedBox(height: 24),
          _buildPrimaryActions(),
          const SizedBox(height: 12),
          _buildSecondaryActions(r),
        ],
      ),
    );
  }

  Widget _buildAppBar(Reminder r) {
    return SizedBox(
      height: 64,
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.arrow_back, color: _primary),
          ),
          Expanded(
            child: Text(
              'Ứng dụng Mẹ và Bé',
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend', fontSize: 20, fontWeight: FontWeight.w600, color: _onSurfaceVariant),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildHeroCard(Reminder r) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: const Color(0xFF5A463F).withAlpha(20), blurRadius: 20, offset: const Offset(0, 4))],
      ),
      child: Column(
        children: [
          Container(
            width: 80, height: 80,
            decoration: BoxDecoration(color: _surfaceContainer, shape: BoxShape.circle),
            child: const Icon(Icons.vaccines, color: _primary, size: 40),
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
            decoration: BoxDecoration(
              color: _primaryContainer.withAlpha(26),
              borderRadius: BorderRadius.circular(99),
              border: Border.all(color: _primaryContainer.withAlpha(51)),
            ),
            child: Text(r.reminderType.displayLabel,
                style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, fontWeight: FontWeight.w700, color: _primary, letterSpacing: 0.6)),
          ),
          const SizedBox(height: 8),
          Text(r.title,
              textAlign: TextAlign.center,
              style: const TextStyle(fontFamily: 'Lexend', fontSize: 24, fontWeight: FontWeight.w700, color: _onSurface)),
          const SizedBox(height: 4),
          Text('Bé Sushi', style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, color: _secondary)),
        ],
      ),
    );
  }

  Widget _buildBentoGrid(Reminder r) {
    return Column(
      children: [
        _InfoTile(
          icon: Icons.schedule,
          label: 'LẦN TỚI',
          mainText: _formatDateTime(r.scheduledAt),
          subText: _formatDate(r.scheduledAt),
          fullWidth: true,
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: _InfoTile(
                icon: Icons.update,
                label: 'LẶP LẠI',
                mainText: r.recurrenceType.displayLabel,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _InfoTile(
                icon: Icons.pending_actions,
                label: 'TRẠNG THÁI',
                mainText: r.status.displayLabel,
                mainColor: _secondary,
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildNoteCard(String note) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _outlineVariant.withAlpha(77)),
        boxShadow: [BoxShadow(color: const Color(0xFF5A463F).withAlpha(13), blurRadius: 12, offset: const Offset(0, 4))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: const [
              Icon(Icons.description_outlined, size: 20, color: _secondary),
              SizedBox(width: 8),
              Text('GHI CHÚ', style: TextStyle(fontFamily: 'Lexend', fontSize: 12, fontWeight: FontWeight.w700, letterSpacing: 0.6, color: _secondary)),
            ],
          ),
          const SizedBox(height: 12),
          Text(note, style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant, height: 1.6)),
        ],
      ),
    );
  }

  Widget _buildPrimaryActions() {
    return Column(
      children: [
        SizedBox(
          width: double.infinity,
          height: 56,
          child: FilledButton.icon(
            onPressed: _markDone,
            style: FilledButton.styleFrom(
              backgroundColor: _primaryContainer,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            ),
            icon: const Icon(Icons.check_circle, size: 20),
            label: const Text('Hoàn thành', style: TextStyle(fontFamily: 'Lexend', fontSize: 20, fontWeight: FontWeight.w600)),
          ),
        ),
        const SizedBox(height: 12),
        SizedBox(
          width: double.infinity,
          height: 56,
          child: OutlinedButton.icon(
            onPressed: _snooze,
            style: OutlinedButton.styleFrom(
              foregroundColor: _primaryContainer,
              side: const BorderSide(color: _primaryContainer, width: 2),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            ),
            icon: const Icon(Icons.snooze, size: 20),
            label: const Text('Hoãn lại', style: TextStyle(fontFamily: 'Lexend', fontSize: 20, fontWeight: FontWeight.w600)),
          ),
        ),
      ],
    );
  }

  Widget _buildSecondaryActions(Reminder r) {
    return Container(
      padding: const EdgeInsets.only(top: 16),
      decoration: BoxDecoration(border: Border(top: BorderSide(color: _outlineVariant.withAlpha(77)))),
      child: Row(
        children: [
          Expanded(child: _SecondaryBtn(icon: Icons.edit_outlined, label: 'Sửa', onTap: () {
            // TODO: navigate to EditReminderScreen (UC-213/214)
          })),
          const SizedBox(width: 16),
          Expanded(child: _SecondaryBtn(icon: Icons.delete_outline, label: 'Xóa', color: _error, onTap: _delete)),
          const SizedBox(width: 16),
          Expanded(child: _SecondaryBtn(icon: Icons.block, label: 'Bỏ qua', onTap: _skip)),
        ],
      ),
    );
  }
}

class _InfoTile extends StatelessWidget {
  final IconData icon;
  final String label;
  final String mainText;
  final String? subText;
  final Color? mainColor;
  final bool fullWidth;

  const _InfoTile({
    required this.icon, required this.label, required this.mainText,
    this.subText, this.mainColor, this.fullWidth = false,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: fullWidth ? double.infinity : null,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFD6C2BD).withAlpha(77)),
        boxShadow: [BoxShadow(color: const Color(0xFF5A463F).withAlpha(13), blurRadius: 12, offset: const Offset(0, 4))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, size: 20, color: const Color(0xFF845143)),
              const SizedBox(width: 8),
              Text(label, style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, fontWeight: FontWeight.w700, letterSpacing: 0.6, color: Color(0xFF6E5A52))),
            ],
          ),
          const SizedBox(height: 8),
          Text(mainText, style: TextStyle(fontFamily: 'Lexend', fontSize: 20, fontWeight: FontWeight.w600, color: mainColor ?? const Color(0xFF271812))),
          if (subText != null)
            Text(subText!, style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, color: Color(0xFF524440))),
        ],
      ),
    );
  }
}

class _SecondaryBtn extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color? color;
  final VoidCallback onTap;

  const _SecondaryBtn({required this.icon, required this.label, this.color, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final c = color ?? const Color(0xFF271812);
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 52,
        decoration: BoxDecoration(
          color: const Color(0xFFFFE9E3),
          borderRadius: BorderRadius.circular(16),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 20, color: c),
            const SizedBox(width: 6),
            Text(label, style: TextStyle(fontFamily: 'Lexend', fontSize: 16, color: c)),
          ],
        ),
      ),
    );
  }
}
