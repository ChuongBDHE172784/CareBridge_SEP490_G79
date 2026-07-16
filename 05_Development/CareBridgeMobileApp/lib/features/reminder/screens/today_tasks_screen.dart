import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../models/reminder_model.dart';
import '../models/today_task_model.dart';
import '../services/reminder_service.dart';
import 'create_appointment_reminder_screen.dart';
import 'create_medication_reminder_screen.dart';
import 'create_vaccination_reminder_screen.dart';
import 'reminder_detail_screen.dart';

/// CB-013 - Today Tasks.
/// Server-backed aggregate of reminders and family care tasks due today.
class TodayTasksScreen extends StatefulWidget {
  const TodayTasksScreen({super.key});

  @override
  State<TodayTasksScreen> createState() => _TodayTasksScreenState();
}

enum _TaskFilter { all, pending, completed, skipped }

class _TodayTasksScreenState extends State<TodayTasksScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  final _service = ReminderService.instance;
  List<TodayTask> _allTasks = [];
  _TaskFilter _filter = _TaskFilter.all;
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
      final list = await _service.listTodayTasks();
      if (!mounted) return;
      setState(() {
        _allTasks = list;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _errorText = 'Không thể tải công việc hôm nay.';
        _loading = false;
      });
    }
  }

  List<TodayTask> get _filtered {
    final list = _allTasks.where((task) {
      switch (_filter) {
        case _TaskFilter.all:
          return true;
        case _TaskFilter.pending:
          return task.isPending;
        case _TaskFilter.completed:
          return task.isCompleted;
        case _TaskFilter.skipped:
          return task.isSkipped;
      }
    }).toList();
    list.sort((a, b) => a.dueAt.compareTo(b.dueAt));
    return list;
  }

  String _todayLabel() {
    final now = DateTime.now();
    return '${now.day.toString().padLeft(2, '0')}/${now.month.toString().padLeft(2, '0')}/${now.year}';
  }

  Future<void> _openTask(TodayTask task) async {
    if (task.isReminder) {
      final result = await Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => ReminderDetailScreen(reminderId: task.id),
        ),
      );
      if (result == true) {
        _load();
      }
      return;
    }

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text(
          'Chi tiết công việc gia đình chưa được kết nối từ Today Tasks.',
        ),
      ),
    );
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
                          : _filtered.isEmpty
                          ? _buildEmpty()
                          : ListView.separated(
                              padding: const EdgeInsets.fromLTRB(
                                24,
                                0,
                                24,
                                100,
                              ),
                              itemCount: _filtered.length,
                              separatorBuilder: (_, _) =>
                                  const SizedBox(height: 12),
                              itemBuilder: (_, i) => _TaskCard(
                                task: _filtered[i],
                                onTap: () => _openTask(_filtered[i]),
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
        onPressed: _showCreateReminderTypeSheet,
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
          Row(
            children: [
              if (Navigator.of(context).canPop())
                IconButton(
                  onPressed: () => Navigator.of(context).pop(),
                  icon: const Icon(Icons.arrow_back, color: _primary),
                ),
              const Expanded(
                child: Text(
                  'Việc hôm nay',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 24,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
              ),
              IconButton(
                tooltip: 'Tất cả lời nhắc',
                onPressed: () => context.push('/reminders/all'),
                icon: const Icon(Icons.format_list_bulleted, color: _primary),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            _todayLabel(),
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 16,
              color: _onSurfaceVariant,
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
              label: 'Tất cả',
              selected: _filter == _TaskFilter.all,
              onTap: () => setState(() => _filter = _TaskFilter.all),
            ),
            const SizedBox(width: 8),
            _FilterPill(
              label: 'Chưa làm',
              selected: _filter == _TaskFilter.pending,
              onTap: () => setState(() => _filter = _TaskFilter.pending),
            ),
            const SizedBox(width: 8),
            _FilterPill(
              label: 'Đã làm',
              selected: _filter == _TaskFilter.completed,
              onTap: () => setState(() => _filter = _TaskFilter.completed),
            ),
            const SizedBox(width: 8),
            _FilterPill(
              label: 'Bỏ qua',
              selected: _filter == _TaskFilter.skipped,
              onTap: () => setState(() => _filter = _TaskFilter.skipped),
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
            'Hôm nay không có việc đến hạn.',
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
            style: const TextStyle(
              fontFamily: 'Lexend',
              color: _onSurfaceVariant,
            ),
          ),
        ),
        Center(
          child: TextButton(onPressed: _load, child: const Text('Thử lại')),
        ),
      ],
    );
  }

  void _showCreateReminderTypeSheet() {
    showModalBottomSheet(
      context: context,
      backgroundColor: _surface,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (context) {
        return Padding(
          padding: EdgeInsets.fromLTRB(
            24,
            32,
            24,
            MediaQuery.of(context).padding.bottom + 24,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Tạo nhắc lịch',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
              const SizedBox(height: 24),
              _buildTypeOption(
                icon: Icons.medication,
                color: const Color(0xFF6E5A52),
                bgColor: const Color(0xFFF6DACF),
                title: 'Thuốc hoặc vitamin',
                subtitle: 'Chỉ tạo từ hướng dẫn bạn đã có',
                onTap: () =>
                    _openCreateScreen(const CreateMedicationReminderScreen()),
              ),
              const SizedBox(height: 16),
              _buildTypeOption(
                icon: Icons.vaccines,
                color: const Color(0xFF625D59),
                bgColor: const Color(0xFFE9E1DB),
                title: 'Tiêm chủng',
                subtitle: 'Dùng gợi ý từ lịch tiêm đã ghi nhận',
                onTap: () =>
                    _openCreateScreen(const CreateVaccinationReminderScreen()),
              ),
              const SizedBox(height: 16),
              _buildTypeOption(
                icon: Icons.calendar_month,
                color: _primary,
                bgColor: const Color(0xFFFFE2D9),
                title: 'Lịch hẹn hoặc khám định kỳ',
                subtitle: 'Tạo nhắc lịch chăm sóc chung',
                onTap: () =>
                    _openCreateScreen(const CreateAppointmentReminderScreen()),
              ),
            ],
          ),
        );
      },
    );
  }

  Future<void> _openCreateScreen(Widget screen) async {
    Navigator.pop(context);
    final result = await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => screen),
    );
    if (result == true) {
      _load();
    }
  }

  Widget _buildTypeOption({
    required IconData icon,
    required Color color,
    required Color bgColor,
    required String title,
    required String subtitle,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          border: Border.all(color: _surfaceVariant),
          borderRadius: BorderRadius.circular(16),
        ),
        child: Row(
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(color: bgColor, shape: BoxShape.circle),
              child: Icon(icon, color: color),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: _onSurface,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    subtitle,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      color: _onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            const Icon(
              Icons.arrow_forward_ios,
              size: 16,
              color: _onSurfaceVariant,
            ),
          ],
        ),
      ),
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
  final TodayTask task;
  final VoidCallback onTap;

  const _TaskCard({required this.task, required this.onTap});

  IconData get _typeIcon {
    if (task.isCareTask) return Icons.groups;
    switch (task.type) {
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
    if (task.isCareTask) return const Color(0xFFE9E1DB);
    switch (task.type) {
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
    if (task.isCareTask) return const Color(0xFF625D59);
    switch (task.type) {
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
    if (task.isCareTask) return 'Việc gia đình';
    return task.type.displayLabel;
  }

  String _formatTime(DateTime dt) {
    final h = dt.toLocal().hour.toString().padLeft(2, '0');
    final m = dt.toLocal().minute.toString().padLeft(2, '0');
    return '$h:$m';
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
                      if (task.isCompleted)
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 4,
                          ),
                          decoration: BoxDecoration(
                            color: const Color(0xFFE2F3E7),
                            borderRadius: BorderRadius.circular(99),
                          ),
                          child: const Text(
                            'Đã xong',
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 12,
                              color: Color(0xFF1E8E3E),
                            ),
                          ),
                        )
                      else if (task.isSkipped)
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 4,
                          ),
                          decoration: BoxDecoration(
                            color: const Color(0xFFFFE8C7),
                            borderRadius: BorderRadius.circular(99),
                          ),
                          child: const Text(
                            'Đã bỏ qua',
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 12,
                              color: Color(0xFF8A4B00),
                            ),
                          ),
                        ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    _formatTime(task.dueAt),
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
                      color: task.isCareTask
                          ? const Color(0xFFE9E1DB).withAlpha(128)
                          : const Color(0xFFF6DACF).withAlpha(128),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          task.isCareTask ? Icons.groups : Icons.alarm,
                          size: 16,
                          color: const Color(0xFF735E56),
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
            const Icon(Icons.chevron_right, color: Color(0xFF84736F)),
          ],
        ),
      ),
    );
  }
}
