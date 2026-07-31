import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../checklist/services/checklist_assignment_refresh_bus.dart';
import '../../checklist/widgets/add_user_checklist_task_button.dart';
import '../../journey/models/journey_model.dart';
import '../../journey/services/journey_service.dart';
import '../../reminder/models/reminder_model.dart';
import '../../reminder/services/reminder_service.dart';
import '../../reminder/services/today_task_service.dart';
import '../../reminder/widgets/today_tasks_panel.dart';
import '../../notification/screens/notification_center_screen.dart';
import '../../notification/services/notification_service.dart';
import '../../../core/network/api_client.dart';
import '../../community/screens/community_feed_screen.dart';
import '../../community/screens/view_content_screen.dart';
import '../../community/models/content_model.dart';
import '../../exercise/screens/mother_exercise_screen.dart';

/// CB-008 — Mother Home (UC-24, UC-49)
/// Main home screen showing journey status card, next appointment alert,
/// quick action grid, today's tasks, and personalized content suggestions.
/// Data: GET /api/v1/journeys/me/dashboard (UC-24), mock tasks + articles.
class MotherHomeScreen extends StatefulWidget {
  const MotherHomeScreen({
    super.key,
    this.recoveryNotice,
    this.todayTaskService,
    this.dashboardLoader,
    this.reminderLoader,
  });

  final String? recoveryNotice;
  final TodayTaskService? todayTaskService;
  final Future<JourneyDashboard> Function()? dashboardLoader;
  final Future<List<Reminder>> Function()? reminderLoader;

  @override
  State<MotherHomeScreen> createState() => _MotherHomeScreenState();
}

class _MotherHomeScreenState extends State<MotherHomeScreen>
    with WidgetsBindingObserver {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _surfaceContainerHighest = Color(0xFFFADCD3);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _error = Color(0xFFBA1A1A);

  final _journeyService = JourneyService();
  late final TodayTaskService _todayTaskService;
  final TodayTasksPanelController _todayTasksController =
      TodayTasksPanelController();
  StreamSubscription<void>? _checklistAssignmentRefreshSubscription;

  JourneyDashboard? _dashboard;
  List<Reminder> _reminders = [];
  bool _loading = true;
  bool _hasUnread = false;
  int _loadGeneration = 0;

  @override
  void initState() {
    super.initState();
    _todayTaskService = widget.todayTaskService ?? TodayTaskService.instance;
    WidgetsBinding.instance.addObserver(this);
    JourneyService.dashboardRevision.addListener(_onJourneyDashboardChanged);
    _checklistAssignmentRefreshSubscription = ChecklistAssignmentRefreshBus
        .events
        .listen((_) {
          if (mounted) unawaited(_todayTasksController.refresh());
        });
    _load();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    JourneyService.dashboardRevision.removeListener(_onJourneyDashboardChanged);
    _checklistAssignmentRefreshSubscription?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _load();
    }
  }

  void _onJourneyDashboardChanged() {
    if (mounted) {
      _load();
    }
  }

  Future<void> _checkUnread() async {
    try {
      final notifs = await NotificationService.instance.getNotifications(
        size: 20,
      );
      if (mounted) {
        setState(() => _hasUnread = notifs.any((n) => n.isUnread));
      }
    } catch (_) {}
  }

  Future<void> _load() async {
    final todayRefresh = _todayTasksController.refresh();
    final generation = ++_loadGeneration;
    final isInitialLoad = _dashboard == null;
    if (isInitialLoad) {
      setState(() => _loading = true);
    }
    _checkUnread();
    try {
      final dashboard =
          await (widget.dashboardLoader?.call() ??
              _journeyService.getDashboard());
      final reminders = await _loadReminders();
      if (mounted && generation == _loadGeneration) {
        setState(() {
          _dashboard = dashboard;
          _reminders = reminders;
          _loading = false;
        });
      }
    } on ApiException {
      if (mounted && generation == _loadGeneration && isInitialLoad) {
        setState(() => _loading = false);
      }
    } catch (_) {
      if (mounted && generation == _loadGeneration && isInitialLoad) {
        setState(() => _loading = false);
      }
    } finally {
      await todayRefresh;
    }
  }

  Future<List<Reminder>> _loadReminders() async {
    if (widget.reminderLoader != null) {
      return widget.reminderLoader!();
    }
    try {
      final upcoming = await ReminderService.instance.listUpcomingReminders();
      if (upcoming.isNotEmpty) return upcoming;
      return await ReminderService.instance.listTodayReminders();
    } catch (_) {
      return _reminders;
    }
  }

  Reminder? _nearestAppointment() {
    final pending =
        _reminders
            .where(
              (reminder) =>
                  reminder.reminderType == ReminderType.appointment &&
                  reminder.status == ReminderStatus.pending,
            )
            .toList()
          ..sort((a, b) => a.scheduledAt.compareTo(b.scheduledAt));
    return pending.firstOrNull;
  }

  String _formatDateTime(DateTime date) {
    final localDate = date.toLocal();
    final day = localDate.day.toString().padLeft(2, '0');
    final month = localDate.month.toString().padLeft(2, '0');
    final hour = localDate.hour.toString().padLeft(2, '0');
    final minute = localDate.minute.toString().padLeft(2, '0');
    return '$hour:$minute - $day/$month/${localDate.year}';
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
                if (widget.recoveryNotice != null) ...[
                  _buildContinuationRecoveryNotice(),
                  const SizedBox(height: 16),
                ],
                _buildGreeting(),
                const SizedBox(height: 24),
                _buildDiscoverSection(),
                const SizedBox(height: 24),
                if (_loading)
                  const Center(
                    child: CircularProgressIndicator(
                      key: Key('mother-home-dashboard-loading'),
                      color: _primaryContainer,
                    ),
                  )
                else ...[
                  _buildJourneyCard(),
                  const SizedBox(height: 16),
                  _buildAlertCard(),
                  const SizedBox(height: 24),
                ],
                _buildTasksSection(),
                const SizedBox(height: 24),
              ]),
            ),
          ),
          if (!_loading) SliverToBoxAdapter(child: _buildContentSection()),
          const SliverToBoxAdapter(child: SizedBox(height: 24)),
        ],
      ),
    );
  }

  Widget _buildContinuationRecoveryNotice() {
    final message = widget.recoveryNotice!;
    return Semantics(
      key: const Key('triage-continuation-recovery-notice'),
      container: true,
      liveRegion: true,
      label: message,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: const Color(0xFFF2EAE4),
          borderRadius: BorderRadius.circular(20),
          border: const Border(
            left: BorderSide(color: Color(0xFFC98C7B), width: 4),
          ),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Icon(Icons.info_outline_rounded, color: Color(0xFF845143)),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                message,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  height: 1.45,
                  color: Color(0xFF5A463F),
                ),
              ),
            ),
          ],
        ),
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
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: _surfaceContainerHigh,
                border: Border.all(color: _surfaceContainerHighest),
              ),
              child: const Icon(
                Icons.person,
                size: 22,
                color: _onSurfaceVariant,
              ),
            ),
            const Expanded(
              child: Text(
                'CareBridge',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 32,
                  fontWeight: FontWeight.w700,
                  color: _primary,
                  letterSpacing: -0.5,
                ),
              ),
            ),
            // Notification bell + red dot
            Stack(
              clipBehavior: Clip.none,
              children: [
                IconButton(
                  onPressed: () => Navigator.of(context)
                      .push(
                        MaterialPageRoute(
                          builder: (_) => const NotificationCenterScreen(),
                        ),
                      )
                      .then((_) => _checkUnread()),
                  icon: const Icon(
                    Icons.notifications,
                    size: 28,
                    color: _primary,
                  ),
                ),
                if (_hasUnread)
                  Positioned(
                    top: 8,
                    right: 8,
                    child: Container(
                      width: 10,
                      height: 10,
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
        const Text(
          'Chào Mẹ,',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 24,
            fontWeight: FontWeight.w600,
            color: _onSurface,
          ),
        ),
        const SizedBox(height: 4),
        const Text(
          'Hôm nay mẹ và bé cảm thấy thế nào?',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            color: _onSurfaceVariant,
          ),
        ),
      ],
    );
  }

  /// TDS MotherExpertDiscoveryInbox §13.1 — Cộng đồng/Bài tập lost their bottom-nav slots to
  /// Chuyên gia/Trò chuyện, so they need a discovery entry point here instead.
  Widget _buildDiscoverSection() {
    return Row(
      children: [
        _QuickAction(
          icon: Icons.group_outlined,
          label: 'Cộng đồng',
          onTap: () => Navigator.of(context).push(
            MaterialPageRoute(builder: (_) => const CommunityFeedScreen()),
          ),
        ),
        _QuickAction(
          icon: Icons.self_improvement,
          label: 'Bài tập',
          onTap: () => Navigator.of(context).push(
            MaterialPageRoute(builder: (_) => const MotherExerciseScreen()),
          ),
        ),
        _QuickAction(
          icon: Icons.menu_book_outlined,
          label: 'Nội dung & FAQ',
          onTap: () => Navigator.of(context).push(
            MaterialPageRoute(
              builder: (_) =>
                  const ViewContentScreen(mode: ContentBrowseMode.lifecycle),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildJourneyCard() {
    final d = _dashboard;
    if (d?.hasActiveJourney != true) {
      return _buildNoJourneyCard();
    }

    final week = d!.displayPregnancyWeek;
    final progress = d.pregnancyProgress;
    final title = week != null ? 'Tuần $week' : d.phaseLabel;
    final description = week != null
        ? 'Bé đang lớn bằng ${d.fruitName}, ${d.fruitSizeNote}.'
        : 'CareBridge đang theo dõi hành trình từ dữ liệu mẹ đã thiết lập.';

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withAlpha(15),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Week badge
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  color: _surfaceContainerLow,
                  borderRadius: BorderRadius.circular(99),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: const [
                    Icon(Icons.favorite, size: 16, color: _primary),
                    SizedBox(width: 4),
                    Text(
                      'Hành trình thai kỳ',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                        color: _primary,
                        letterSpacing: 0.5,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 12),
              // Week number
              Text(
                title,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 32,
                  fontWeight: FontWeight.w700,
                  color: _onSurface,
                  letterSpacing: -0.5,
                ),
              ),
              const SizedBox(height: 8),
              // Description (2/3 width to avoid fruit image)
              SizedBox(
                width: MediaQuery.of(context).size.width * 0.5,
                child: Text(
                  description,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _onSurfaceVariant,
                    height: 1.4,
                  ),
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
                  valueColor: const AlwaysStoppedAnimation<Color>(
                    _primaryContainer,
                  ),
                ),
              ),
            ],
          ),
          // Fruit icon (top-right decorative)
          Positioned(
            top: 0,
            right: 0,
            child: Container(
              width: 88,
              height: 88,
              decoration: BoxDecoration(
                color: _surfaceContainerHighest.withAlpha(128),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.eco, size: 48, color: _primaryContainer),
            ),
          ),
          // Decorative blur circle bottom-right
          Positioned(
            bottom: -32,
            right: -32,
            child: Container(
              width: 120,
              height: 120,
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

  Widget _buildNoJourneyCard() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withAlpha(15),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: const BoxDecoration(
              color: _surfaceContainerHigh,
              shape: BoxShape.circle,
            ),
            child: const Icon(Icons.route_rounded, color: _primary),
          ),
          const SizedBox(height: 16),
          const Text(
            'Thiết lập hành trình của mẹ',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Hoàn tất setup để Home hiển thị tuần thai và ngày dự sinh theo dữ liệu thật.',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
              height: 1.45,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAlertCard() {
    final appointment = _nearestAppointment();

    return GestureDetector(
      key: const Key('mother-home-next-appointment-card'),
      onTap: () async {
        await context.push('/reminders/calendar');
        if (mounted) await _load();
      },
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: const Color(0xFFF2EAE4),
          borderRadius: BorderRadius.circular(24),
          boxShadow: const [
            BoxShadow(
              color: Color(0x0F5A463F),
              blurRadius: 24,
              offset: Offset(0, 8),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Lịch hẹn tiếp theo',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                          color: _onSurface,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        appointment?.title ??
                            'Chưa có lịch khám sắp tới (Chạm để xem hoặc tạo mới)',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 14,
                          color: _onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                Container(
                  width: 40,
                  height: 40,
                  decoration: const BoxDecoration(
                    color: _surface,
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.calendar_today,
                    color: _primary,
                    size: 20,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            const Divider(color: Color(0xFFD6C2BD), thickness: 1),
            const SizedBox(height: 12),
            Row(
              children: [
                const Icon(Icons.schedule, size: 16, color: _onSurfaceVariant),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    appointment != null
                        ? [
                            _formatDateTime(appointment.scheduledAt),
                            if (appointment.location != null)
                              appointment.location!,
                          ].join(' • ')
                        : 'Quản lý danh sách lịch hẹn',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      color: _onSurfaceVariant,
                    ),
                  ),
                ),
                const Icon(
                  Icons.chevron_right_rounded,
                  color: _onSurfaceVariant,
                  size: 20,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTasksSection() => TodayTasksPanel(
    service: _todayTaskService,
    audience: TodayTasksAudience.mother,
    layout: TodayTasksLayout.sourceGroups,
    controller: _todayTasksController,
    headingAction: (_dashboard?.journeyId?.isNotEmpty ?? false)
        ? AddUserChecklistTaskButton(journeyId: _dashboard!.journeyId)
        : null,
  );

  Widget _buildContentSection() {
    if (_dashboard == null) return const SizedBox.shrink();
    final week = _dashboard!.displayPregnancyWeek;
    if (week == null) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Text(
        'Dành riêng cho tuần $week',
        style: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 20,
          fontWeight: FontWeight.w600,
          color: _onSurface,
        ),
      ),
    );
  }
}

// ─── Quick action button ──────────────────────────────────────────────────────
class _QuickAction extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  const _QuickAction({
    required this.icon,
    required this.label,
    required this.onTap,
  });

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
            boxShadow: [
              BoxShadow(
                color: const Color(0xFF5A463F).withAlpha(15),
                blurRadius: 20,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Column(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: const BoxDecoration(
                  color: Color(0xFFF6DACF),
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, color: const Color(0xFF735E56)),
              ),
              const SizedBox(height: 8),
              Text(
                label,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 11,
                  fontWeight: FontWeight.w500,
                  color: Color(0xFF524440),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
