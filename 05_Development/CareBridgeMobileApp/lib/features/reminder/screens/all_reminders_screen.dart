import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../models/reminder_model.dart';
import '../services/reminder_service.dart';
import 'create_appointment_reminder_screen.dart';
import 'create_medication_reminder_screen.dart';
import 'create_vaccination_reminder_screen.dart';
import 'reminder_detail_screen.dart';

class AllRemindersScreen extends StatefulWidget {
  const AllRemindersScreen({super.key});

  @override
  State<AllRemindersScreen> createState() => _AllRemindersScreenState();
}

enum _TaskFilter { active, cancelled }

class _AllRemindersScreenState extends State<AllRemindersScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  final _service = ReminderService.instance;
  List<Reminder> _allReminders = [];
  _TaskFilter _filter = _TaskFilter.active;
  bool _loading = true;
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
      final list = await _service.listAllReminders();
      if (!mounted) return;
      setState(() {
        _allReminders = list;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _errorText = 'Không thể tải danh sách lời nhắc.';
        _loading = false;
      });
    }
  }

  List<Reminder> get _filteredAndSortedReminders {
    final list = _allReminders.where((task) {
      if (_filter == _TaskFilter.active) {
        return task.status != ReminderStatus.cancelled;
      } else {
        return task.status == ReminderStatus.cancelled;
      }
    }).toList();
    list.sort((a, b) => a.scheduledAt.compareTo(b.scheduledAt));
    return list;
  }

  Future<void> _openTask(Reminder task) async {
    final result = await context.push('/reminders/${task.id}/manage', extra: task);
    if (result == true) {
      _load();
    }
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
                  ? const Center(
                      child: CircularProgressIndicator(
                        color: _primaryContainer,
                      ),
                    )
                  : RefreshIndicator(
                      color: _primaryContainer,
                      onRefresh: _load,
                      child: _errorText != null
                          ? _buildError()
                          : _filteredAndSortedReminders.isEmpty
                          ? _buildEmpty()
                          : ListView.separated(
                              padding: const EdgeInsets.fromLTRB(24, 0, 24, 100),
                              itemCount: _filteredAndSortedReminders.length,
                              separatorBuilder: (_, _) => const SizedBox(height: 12),
                              itemBuilder: (_, i) => _TaskCard(
                                task: _filteredAndSortedReminders[i],
                                onTap: () => _openTask(_filteredAndSortedReminders[i]),
                              ),
                            ),
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 24, 24, 4),
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.of(context).pop(),
            icon: const Icon(Icons.arrow_back, color: _primary),
          ),
          const Expanded(
            child: Text(
              'Tất cả lời nhắc',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 24,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
          ),
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
            _FilterPill(
              label: 'Đang hoạt động',
              selected: _filter == _TaskFilter.active,
              onTap: () => setState(() => _filter = _TaskFilter.active),
            ),
            const SizedBox(width: 8),
            _FilterPill(
              label: 'Đã tắt',
              selected: _filter == _TaskFilter.cancelled,
              onTap: () => setState(() => _filter = _TaskFilter.cancelled),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmpty() {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      children: const [
        SizedBox(height: 180),
        Center(
          child: Text(
            'Không có lời nhắc nào.',
            style: TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant),
          ),
        ),
      ],
    );
  }

  Widget _buildError() {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      children: [
        const SizedBox(height: 160),
        const Icon(Icons.error_outline_rounded, color: _primary, size: 40),
        const SizedBox(height: 10),
        Center(
          child: Text(
            _errorText!,
            style: const TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant),
          ),
        ),
        Center(
          child: TextButton(
            onPressed: _load,
            child: const Text('Thử lại'),
          ),
        ),
      ],
    );
  }
}

class _FilterPill extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _FilterPill({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
        decoration: BoxDecoration(
          color: selected ? const Color(0xFFC98C7B) : const Color(0xFFFFF8F6),
          borderRadius: BorderRadius.circular(99),
          border: selected ? null : Border.all(color: const Color(0xFFFADCD3)),
          boxShadow: [
            BoxShadow(
              color: const Color(0xFF5A463F).withAlpha(15),
              blurRadius: 20,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Text(
          label,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: selected ? Colors.white : const Color(0xFF524440),
          ),
        ),
      ),
    );
  }
}

class _TaskCard extends StatelessWidget {
  final Reminder task;
  final VoidCallback onTap;

  const _TaskCard({
    required this.task,
    required this.onTap,
  });

  IconData get _typeIcon {
    switch (task.reminderType) {
      case ReminderType.appointment:
        return Icons.calendar_month;
      case ReminderType.medication:
        return Icons.medication;
      case ReminderType.vaccination:
        return Icons.vaccines;
      default:
        return Icons.task_alt;
    }
  }

  Color get _iconBg {
    switch (task.reminderType) {
      case ReminderType.appointment:
        return const Color(0xFFFFE2D9);
      case ReminderType.medication:
        return const Color(0xFFF6DACF);
      case ReminderType.vaccination:
        return const Color(0xFFE9E1DB);
      default:
        return const Color(0xFFFFE9E3);
    }
  }

  Color get _iconColor {
    switch (task.reminderType) {
      case ReminderType.appointment:
        return const Color(0xFF845143);
      case ReminderType.medication:
        return const Color(0xFF6E5A52);
      case ReminderType.vaccination:
        return const Color(0xFF625D59);
      default:
        return const Color(0xFF845143);
    }
  }

  String get _sourceLabel {
    return task.reminderType?.displayLabel ?? 'Khác';
  }

  String _formatTime(DateTime dt) {
    final date = '${dt.toLocal().day.toString().padLeft(2, '0')}/${dt.toLocal().month.toString().padLeft(2, '0')}';
    final h = dt.toLocal().hour.toString().padLeft(2, '0');
    final m = dt.toLocal().minute.toString().padLeft(2, '0');
    return '$date - $h:$m';
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: const Color(0xFFFFF8F6),
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: const Color(0xFF5A463F).withAlpha(15),
              blurRadius: 20,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 48,
              height: 48,
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
                        child: Text(
                          task.title,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 18,
                            fontWeight: FontWeight.w600,
                            color: Color(0xFF271812),
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    _formatTime(task.scheduledAt),
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      color: Color(0xFF524440),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 4,
                    ),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF6DACF).withAlpha(128),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(
                          Icons.alarm,
                          size: 16,
                          color: Color(0xFF735E56),
                        ),
                        const SizedBox(width: 4),
                        Text(
                          _sourceLabel,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            color: Color(0xFF735E56),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Container(
                  padding: const EdgeInsets.all(6),
                  decoration: BoxDecoration(
                    color: const Color(0xFFC98C7B).withAlpha(30),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    task.status.isTerminal ? Icons.visibility_rounded : Icons.edit_rounded,
                    color: const Color(0xFF845143),
                    size: 16,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  task.status.isTerminal ? 'Xem' : 'Chỉnh sửa',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 10,
                    fontWeight: FontWeight.w600,
                    color: Color(0xFF845143),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
