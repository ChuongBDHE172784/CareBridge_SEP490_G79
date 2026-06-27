import 'package:flutter/material.dart';
import '../models/reminder_model.dart';
import '../services/reminder_service.dart';
import 'reminder_detail_screen.dart';

/// CB-013 — Today Tasks (UC-45, UC-46, UC-47, UC-48, UC-49, UC-50, UC-212–215)
/// Lists today's reminders grouped with filter chips (Tất cả / Cần làm / Đã xong).
/// Task cards with checkbox, importance badge, role chip, location.
/// Data: mock list (TODO: wire to GET /api/v1/reminders?date=today).
class TodayTasksScreen extends StatefulWidget {
  const TodayTasksScreen({super.key});

  @override
  State<TodayTasksScreen> createState() => _TodayTasksScreenState();
}

enum _TaskFilter { all, pending, done }

class _TodayTasksScreenState extends State<TodayTasksScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSecondaryContainer = Color(0xFF735E56);
  static const _tertiary = Color(0xFF625D59);
  static const _tertiaryContainer = Color(0xFFA09A95);
  static const _onTertiaryContainer = Color(0xFF36322E);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFF84736F);

  final _service = ReminderService();
  List<Reminder> _allReminders = [];
  _TaskFilter _filter = _TaskFilter.all;
  bool _loading = true;

  // Local done state for optimistic updates
  final Set<String> _localDone = {};

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final list = await _service.listTodayReminders();
      if (mounted) setState(() { _allReminders = list; _loading = false; });
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  List<Reminder> get _filtered {
    return _allReminders.where((r) {
      final done = r.status == ReminderStatus.done || _localDone.contains(r.id);
      switch (_filter) {
        case _TaskFilter.all: return true;
        case _TaskFilter.pending: return !done;
        case _TaskFilter.done: return done;
      }
    }).toList();
  }

  void _toggleDone(Reminder r) {
    setState(() {
      if (_localDone.contains(r.id)) {
        _localDone.remove(r.id);
      } else {
        _localDone.add(r.id);
        _service.markDone(r.id);
      }
    });
  }

  String _todayLabel() {
    const weekdays = ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ Nhật'];
    const months = ['Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6',
      'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'];
    final now = DateTime.now();
    final wd = weekdays[now.weekday - 1];
    return '$wd, ${now.day} ${months[now.month - 1]}';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildHeader(),
            _buildFilterRow(),
            Expanded(
              child: _loading
                  ? const Center(child: CircularProgressIndicator(color: _primaryContainer))
                  : RefreshIndicator(
                      color: _primaryContainer,
                      onRefresh: _load,
                      child: _filtered.isEmpty
                          ? _buildEmpty()
                          : ListView.separated(
                              padding: const EdgeInsets.fromLTRB(24, 0, 24, 100),
                              itemCount: _filtered.length,
                              separatorBuilder: (_, __) => const SizedBox(height: 12),
                              itemBuilder: (_, i) => _TaskCard(
                                reminder: _filtered[i],
                                isDone: _filtered[i].status == ReminderStatus.done ||
                                    _localDone.contains(_filtered[i].id),
                                onToggle: () => _toggleDone(_filtered[i]),
                                onTap: () => Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (_) => ReminderDetailScreen(reminderId: _filtered[i].id),
                                  ),
                                ),
                              ),
                            ),
                    ),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        backgroundColor: _primaryContainer,
        foregroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        onPressed: () {
          // TODO: open CreateReminderSheet (UC-45)
        },
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Việc cần làm',
              style: TextStyle(fontFamily: 'Lexend', fontSize: 24, fontWeight: FontWeight.w600, color: Color(0xFF271812))),
          const SizedBox(height: 4),
          Text(_todayLabel(),
              style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, color: Color(0xFF524440))),
        ],
      ),
    );
  }

  Widget _buildFilterRow() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 8, 24, 16),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          children: [
            _FilterPill(label: 'Tất cả', selected: _filter == _TaskFilter.all, onTap: () => setState(() => _filter = _TaskFilter.all)),
            const SizedBox(width: 8),
            _FilterPill(label: 'Cần làm', selected: _filter == _TaskFilter.pending, onTap: () => setState(() => _filter = _TaskFilter.pending)),
            const SizedBox(width: 8),
            _FilterPill(label: 'Đã xong', selected: _filter == _TaskFilter.done, onTap: () => setState(() => _filter = _TaskFilter.done)),
          ],
        ),
      ),
    );
  }

  Widget _buildEmpty() {
    return const Center(
      child: Text('Không có việc nào.',
          style: TextStyle(fontFamily: 'Lexend', color: Color(0xFF524440))),
    );
  }
}

class _FilterPill extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _FilterPill({required this.label, required this.selected, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
        decoration: BoxDecoration(
          color: selected ? const Color(0xFFC98C7B) : const Color(0xFFFFF8F6),
          borderRadius: BorderRadius.circular(99),
          border: selected ? null : Border.all(color: const Color(0xFFFADCD3)),
          boxShadow: [
            BoxShadow(color: const Color(0xFF5A463F).withAlpha(15), blurRadius: 20, offset: const Offset(0, 4)),
          ],
        ),
        child: Text(label,
            style: TextStyle(
              fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w600,
              color: selected ? Colors.white : const Color(0xFF524440),
            )),
      ),
    );
  }
}

class _TaskCard extends StatelessWidget {
  final Reminder reminder;
  final bool isDone;
  final VoidCallback onToggle;
  final VoidCallback onTap;

  const _TaskCard({required this.reminder, required this.isDone, required this.onToggle, required this.onTap});

  IconData get _typeIcon {
    switch (reminder.reminderType) {
      case ReminderType.appointment: return Icons.calendar_month;
      case ReminderType.medication: return Icons.medication;
      case ReminderType.vaccination: return Icons.vaccines;
      default: return Icons.task_alt;
    }
  }

  Color get _iconBg {
    switch (reminder.reminderType) {
      case ReminderType.appointment: return const Color(0xFFFFE2D9);
      case ReminderType.medication: return const Color(0xFFF6DACF);
      case ReminderType.vaccination: return const Color(0xFFE9E1DB);
      default: return const Color(0xFFFFE9E3);
    }
  }

  Color get _iconColor {
    switch (reminder.reminderType) {
      case ReminderType.appointment: return const Color(0xFF845143);
      case ReminderType.medication: return const Color(0xFF6E5A52);
      case ReminderType.vaccination: return const Color(0xFF625D59);
      default: return const Color(0xFF845143);
    }
  }

  String _formatTime(DateTime dt) {
    final h = dt.hour.toString().padLeft(2, '0');
    final m = dt.minute.toString().padLeft(2, '0');
    return '$h:$m';
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Opacity(
        opacity: isDone ? 0.7 : 1.0,
        child: Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: const Color(0xFFFFF8F6),
            borderRadius: BorderRadius.circular(16),
            boxShadow: [
              BoxShadow(color: const Color(0xFF5A463F).withAlpha(15), blurRadius: 20, offset: const Offset(0, 4)),
            ],
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 48, height: 48,
                decoration: BoxDecoration(color: _iconBg, shape: BoxShape.circle),
                child: Icon(_typeIcon, color: _iconColor),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(reminder.title,
                              style: TextStyle(
                                fontFamily: 'Lexend', fontSize: 20, fontWeight: FontWeight.w600,
                                color: const Color(0xFF271812),
                                decoration: isDone ? TextDecoration.lineThrough : null,
                              )),
                        ),
                        if (reminder.isImportant)
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                            decoration: BoxDecoration(
                              color: const Color(0xFFFFDAD6).withAlpha(77),
                              borderRadius: BorderRadius.circular(99),
                            ),
                            child: const Text('Quan trọng',
                                style: TextStyle(fontFamily: 'Lexend', fontSize: 12, color: Color(0xFFBA1A1A))),
                          ),
                      ],
                    ),
                    if (reminder.location != null) ...[
                      const SizedBox(height: 4),
                      Text('${reminder.location}, ${_formatTime(reminder.scheduledAt)}',
                          style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, color: Color(0xFF524440))),
                    ],
                    const SizedBox(height: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: reminder.assignee == ReminderAssignee.mother
                            ? const Color(0xFFF6DACF).withAlpha(128)
                            : const Color(0xFFE9E1DB).withAlpha(128),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            reminder.assignee == ReminderAssignee.mother ? Icons.person : Icons.child_care,
                            size: 16,
                            color: const Color(0xFF735E56),
                          ),
                          const SizedBox(width: 4),
                          Text(reminder.assignee.displayLabel,
                              style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, color: Color(0xFF735E56))),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              GestureDetector(
                onTap: onToggle,
                child: Container(
                  width: 24, height: 24,
                  decoration: BoxDecoration(
                    color: isDone ? const Color(0xFF845143) : Colors.transparent,
                    borderRadius: BorderRadius.circular(4),
                    border: Border.all(
                      color: isDone ? const Color(0xFF845143) : const Color(0xFF84736F),
                      width: 2,
                    ),
                  ),
                  child: isDone
                      ? const Icon(Icons.check, size: 16, color: Colors.white)
                      : null,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
