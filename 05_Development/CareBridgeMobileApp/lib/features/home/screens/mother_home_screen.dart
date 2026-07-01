import 'package:flutter/material.dart';
import '../../journey/models/journey_model.dart';
import '../../journey/services/journey_service.dart';
import '../../reminder/models/reminder_model.dart';
import '../../reminder/services/reminder_service.dart';
import '../../reminder/screens/today_tasks_screen.dart';
import '../../notification/screens/notification_center_screen.dart';
import '../../notification/services/notification_service.dart';
import '../../../core/network/api_client.dart';

/// CB-008 — Mother Home (UC-24, UC-49)
/// Main home screen showing journey status card, next appointment alert,
/// quick action grid, today's tasks, and personalized content suggestions.
/// Data: GET /api/v1/journeys/me/dashboard (UC-24), mock tasks + articles.
class MotherHomeScreen extends StatefulWidget {
  const MotherHomeScreen({super.key});

  @override
  State<MotherHomeScreen> createState() => _MotherHomeScreenState();
}

class _MotherHomeScreenState extends State<MotherHomeScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _surfaceContainerHighest = Color(0xFFFADCD3);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _onSecondaryContainer = Color(0xFF735E56);
  static const _error = Color(0xFFBA1A1A);

  final _journeyService = JourneyService();
  final _reminderService = ReminderService.instance;

  JourneyDashboard? _dashboard;
  List<Reminder> _tasks = [];
  bool _loading = true;
  bool _hasUnread = false;


  @override
  void initState() {
    super.initState();
    _reminderService.addListener(_onServiceChanged);
    _load();
  }

  @override
  void dispose() {
    _reminderService.removeListener(_onServiceChanged);
    super.dispose();
  }

  void _onServiceChanged() {
    if (mounted) setState(() {});
  }

  Future<void> _checkUnread() async {
    try {
      final notifs = await NotificationService.instance.getNotifications(size: 20);
      if (mounted) {
        setState(() => _hasUnread = notifs.any((n) => n.isUnread));
      }
    } catch (_) {}
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    _checkUnread();
    try {
      final results = await Future.wait([
        _journeyService.getDashboard(),
        _reminderService.listTodayReminders(),
      ]);
      if (mounted) {
        setState(() {
          _dashboard = results[0] as JourneyDashboard;
          _tasks = (results[1] as List).cast<Reminder>().take(3).toList();
          _loading = false;
        });
      }
    } on ApiException {
      if (mounted) {
        setState(() => _loading = false);
      }
    } catch (_) {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      color: _primaryContainer,
      onRefresh: _load,
      child: CustomScrollView(
        slivers: [
          SliverToBoxAdapter(child: _buildTopBar()),
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 24),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                _buildGreeting(),
                const SizedBox(height: 24),
                if (_loading) const Center(child: CircularProgressIndicator(color: _primaryContainer))
                else ...[
                  _buildJourneyCard(),
                  const SizedBox(height: 16),
                  _buildAlertCard(),
                  const SizedBox(height: 24),
                  _buildQuickActions(),
                  const SizedBox(height: 24),
                  _buildTasksSection(),
                  const SizedBox(height: 24),
                ],
              ]),
            ),
          ),
          if (!_loading) SliverToBoxAdapter(child: _buildContentSection()),
          const SliverToBoxAdapter(child: SizedBox(height: 24)),
        ],
      ),
    );
  }

  Widget _buildTopBar() {
    return SizedBox(
      height: 56,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: Row(
          children: [
            // Avatar circle
            Container(
              width: 40, height: 40,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: _surfaceContainerHigh,
                border: Border.all(color: _surfaceContainerHighest),
              ),
              child: const Icon(Icons.person, size: 22, color: _onSurfaceVariant),
            ),
            const Expanded(
              child: Text('CareBridge',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontFamily: 'Lexend', fontSize: 32, fontWeight: FontWeight.w700, color: _primary, letterSpacing: -0.5)),
            ),
            // Notification bell + red dot
            Stack(
              clipBehavior: Clip.none,
              children: [
                IconButton(
                  onPressed: () => Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => const NotificationCenterScreen()),
                  ).then((_) => _checkUnread()),
                  icon: const Icon(Icons.notifications, size: 28, color: _primary),
                ),
                if (_hasUnread)
                  Positioned(
                    top: 8, right: 8,
                    child: Container(
                      width: 10, height: 10,
                      decoration: BoxDecoration(
                        color: _error,
                        shape: BoxShape.circle,
                        border: Border.all(color: _canvas, width: 1.5),
                      ),
                    ),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildGreeting() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('Chào Mẹ,',
            style: TextStyle(fontFamily: 'Lexend', fontSize: 24, fontWeight: FontWeight.w600, color: _onSurface)),
        const SizedBox(height: 4),
        const Text('Hôm nay mẹ và bé cảm thấy thế nào?',
            style: TextStyle(fontFamily: 'Lexend', fontSize: 16, color: _onSurfaceVariant)),
      ],
    );
  }

  Widget _buildJourneyCard() {
    final d = _dashboard;
    final week = d?.pregnancyWeek ?? 24;
    final progress = d?.pregnancyProgress ?? 0.6;

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: const Color(0xFF5A463F).withAlpha(15), blurRadius: 20, offset: const Offset(0, 4))],
      ),
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Week badge
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                decoration: BoxDecoration(
                  color: _surfaceContainerLow,
                  borderRadius: BorderRadius.circular(99),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: const [
                    Icon(Icons.favorite, size: 16, color: _primary),
                    SizedBox(width: 4),
                    Text('Hành trình thai kỳ',
                        style: TextStyle(fontFamily: 'Lexend', fontSize: 12, fontWeight: FontWeight.w500, color: _primary, letterSpacing: 0.5)),
                  ],
                ),
              ),
              const SizedBox(height: 12),
              // Week number
              Text('Tuần $week',
                  style: const TextStyle(fontFamily: 'Lexend', fontSize: 32, fontWeight: FontWeight.w700, color: _onSurface, letterSpacing: -0.5)),
              const SizedBox(height: 8),
              // Description (2/3 width to avoid fruit image)
              SizedBox(
                width: MediaQuery.of(context).size.width * 0.5,
                child: Text(
                  'Bé đang lớn bằng ${d?.fruitName ?? 'quả dưa lưới'}, ${d?.fruitSizeNote ?? 'dài khoảng 30cm và nặng 600g'}.',
                  style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant, height: 1.4),
                ),
              ),
              const SizedBox(height: 20),
              // Progress bar
              ClipRRect(
                borderRadius: BorderRadius.circular(99),
                child: LinearProgressIndicator(
                  value: progress,
                  minHeight: 8,
                  backgroundColor: _surfaceContainerLow,
                  valueColor: const AlwaysStoppedAnimation<Color>(_primaryContainer),
                ),
              ),
            ],
          ),
          // Fruit icon (top-right decorative)
          Positioned(
            top: 0, right: 0,
            child: Container(
              width: 88, height: 88,
              decoration: BoxDecoration(
                color: _surfaceContainerHighest.withAlpha(128),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.eco, size: 48, color: _primaryContainer),
            ),
          ),
          // Decorative blur circle bottom-right
          Positioned(
            bottom: -32, right: -32,
            child: Container(
              width: 120, height: 120,
              decoration: BoxDecoration(
                color: _secondaryContainer.withAlpha(102),
                shape: BoxShape.circle,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAlertCard() {
    final next = _tasks.where((t) => t.status == ReminderStatus.pending).firstOrNull;
    if (next == null) return const SizedBox.shrink();
    final timeStr = '${next.scheduledAt.hour.toString().padLeft(2, '0')}:${next.scheduledAt.minute.toString().padLeft(2, '0')}';
    final detail = [timeStr, if (next.location != null) next.location!].join(' · ');
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _surfaceVariant,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 40, height: 40,
            decoration: BoxDecoration(color: _surface, shape: BoxShape.circle),
            child: const Icon(Icons.event, color: _primary, size: 22),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Lịch hẹn sắp tới',
                    style: TextStyle(fontFamily: 'Lexend', fontSize: 20, fontWeight: FontWeight.w600, color: _onSurface)),
                const SizedBox(height: 4),
                Text('${next.title} — $detail',
                    style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant, height: 1.4)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildQuickActions() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('Ghi chú nhanh',
            style: TextStyle(fontFamily: 'Lexend', fontSize: 20, fontWeight: FontWeight.w600, color: _onSurface)),
        const SizedBox(height: 16),
        Row(
          children: [
            _QuickAction(icon: Icons.monitor_weight, label: 'Cân nặng', onTap: () {}),
            const SizedBox(width: 12),
            _QuickAction(icon: Icons.water_drop, label: 'Nước', onTap: () {}),
            const SizedBox(width: 12),
            _QuickAction(icon: Icons.mood, label: 'Tâm trạng', onTap: () {}),
            const SizedBox(width: 12),
            _QuickAction(icon: Icons.child_care, label: 'Cử động', onTap: () {}),
          ],
        ),
      ],
    );
  }

  Widget _buildTasksSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text('Việc hôm nay',
                style: TextStyle(fontFamily: 'Lexend', fontSize: 20, fontWeight: FontWeight.w600, color: _onSurface)),
            GestureDetector(
              onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const TodayTasksScreen())),
              child: const Text('Xem tất cả',
                  style: TextStyle(fontFamily: 'Lexend', fontSize: 12, fontWeight: FontWeight.w500, color: _primary)),
            ),
          ],
        ),
        const SizedBox(height: 12),
        if (_tasks.isEmpty)
          const Text('Không có việc nào hôm nay.',
              style: TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant))
        else
          ...List.generate(_tasks.length, (i) {
            final t = _tasks[i];
            return Padding(
              padding: EdgeInsets.only(bottom: i < _tasks.length - 1 ? 10 : 0),
              child: _TaskRow(
                reminder: t,
                isDone: _reminderService.isDone(t),
                onToggle: () => _reminderService.toggleDone(t),
              ),
            );
          }),
      ],
    );
  }

  Widget _buildContentSection() {
    if (_dashboard == null) return const SizedBox.shrink();
    final week = _dashboard!.pregnancyWeek;
    if (week == null) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Text('Dành riêng cho tuần $week',
          style: const TextStyle(fontFamily: 'Lexend', fontSize: 20, fontWeight: FontWeight.w600, color: _onSurface)),
    );
  }
}

// ─── Quick action button ──────────────────────────────────────────────────────
class _QuickAction extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  const _QuickAction({required this.icon, required this.label, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: GestureDetector(
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 12),
          decoration: BoxDecoration(
            color: const Color(0xFFFFF8F6),
            borderRadius: BorderRadius.circular(16),
            boxShadow: [BoxShadow(color: const Color(0xFF5A463F).withAlpha(15), blurRadius: 20, offset: const Offset(0, 4))],
          ),
          child: Column(
            children: [
              Container(
                width: 48, height: 48,
                decoration: const BoxDecoration(color: Color(0xFFF6DACF), shape: BoxShape.circle),
                child: Icon(icon, color: const Color(0xFF735E56)),
              ),
              const SizedBox(height: 8),
              Text(label,
                  textAlign: TextAlign.center,
                  style: const TextStyle(fontFamily: 'Lexend', fontSize: 11, fontWeight: FontWeight.w500, color: Color(0xFF524440))),
            ],
          ),
        ),
      ),
    );
  }
}

// ─── Task row ─────────────────────────────────────────────────────────────────
class _TaskRow extends StatelessWidget {
  final Reminder reminder;
  final bool isDone;
  final VoidCallback onToggle;

  const _TaskRow({required this.reminder, required this.isDone, required this.onToggle});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF8F6),
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: const Color(0xFF5A463F).withAlpha(15), blurRadius: 20, offset: const Offset(0, 4))],
      ),
      child: Row(
        children: [
          GestureDetector(
            onTap: onToggle,
            child: Container(
              width: 24, height: 24,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: isDone ? const Color(0xFF845143) : Colors.transparent,
                border: Border.all(
                  color: isDone ? const Color(0xFF845143) : const Color(0xFFD6C2BD),
                  width: 2,
                ),
              ),
              child: isDone
                  ? const Icon(Icons.check, size: 14, color: Colors.white)
                  : null,
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Text(
              reminder.title,
              style: TextStyle(
                fontFamily: 'Lexend', fontSize: 16,
                color: isDone ? const Color(0xFF84736F) : const Color(0xFF271812),
                decoration: isDone ? TextDecoration.lineThrough : null,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

