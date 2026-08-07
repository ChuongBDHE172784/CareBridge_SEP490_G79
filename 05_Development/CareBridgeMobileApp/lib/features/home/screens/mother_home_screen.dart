import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/auth/auth_state.dart';
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
import '../../community/screens/verified_content_detail_screen.dart';
import '../../community/services/content_service.dart';
import '../../exercise/screens/mother_exercise_screen.dart';
import '../../healthRecords/screens/fetal_movement_tracker_screen.dart';
import '../../healthRecords/screens/epds_screen.dart';
import '../../recommendation/models/recommendation_model.dart';
import '../../recommendation/services/recommendation_service.dart';

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
    this.recommendationService,
    this.recommendationLoader,
  });

  final String? recoveryNotice;
  final TodayTaskService? todayTaskService;
  final Future<JourneyDashboard> Function()? dashboardLoader;
  final Future<List<Reminder>> Function()? reminderLoader;
  final RecommendationService? recommendationService;
  final Future<RecommendationContentResponse> Function()? recommendationLoader;

  @override
  State<MotherHomeScreen> createState() => _MotherHomeScreenState();
}

class _MotherHomeScreenState extends State<MotherHomeScreen>
    with WidgetsBindingObserver {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF8F5F1);
  static const _surface = Color(0xFFFFFCF9);
  static const _surfaceContainerHigh = Color(0xFFF1E6E0);
  static const _surfaceContainerLow = Color(0xFFF8EEE9);
  static const _surfaceContainerHighest = Color(0xFFE5D3CA);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);
  static const _error = Color(0xFFBA1A1A);
  static const _sectionTitleStyle = TextStyle(
    fontFamily: 'Lexend',
    fontSize: 18,
    fontWeight: FontWeight.w700,
    color: _onSurface,
    letterSpacing: -0.2,
  );

  final _journeyService = JourneyService();
  late final TodayTaskService _todayTaskService;
  late final RecommendationService _recommendationService;
  final TodayTasksPanelController _todayTasksController =
      TodayTasksPanelController();
  StreamSubscription<void>? _checklistAssignmentRefreshSubscription;

  JourneyDashboard? _dashboard;
  List<Reminder> _reminders = [];
  bool _loading = true;
  bool _hasUnread = false;
  int _loadGeneration = 0;
  int _recommendationLoadGeneration = 0;
  bool _recommendationLoading = false;
  RecommendationContentResponse? _recommendations;
  String? _recommendationError;
  String? _observedAccountId;

  @override
  void initState() {
    super.initState();
    _todayTaskService = widget.todayTaskService ?? TodayTaskService.instance;
    _recommendationService =
        widget.recommendationService ?? RecommendationService();
    _observedAccountId = AuthState.instance.userId;
    AuthState.instance.addListener(_onAccountChanged);
    RecommendationService.profileChangeRevision.addListener(
      _onRecommendationProfileChanged,
    );
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
    AuthState.instance.removeListener(_onAccountChanged);
    RecommendationService.profileChangeRevision.removeListener(
      _onRecommendationProfileChanged,
    );
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

  void _onAccountChanged() {
    final accountId = AuthState.instance.userId;
    if (accountId == _observedAccountId) return;
    _observedAccountId = accountId;
    // Do not render the previous account while the new account is resolving.
    // Increment both generations so every in-flight response is discarded.
    _loadGeneration++;
    _recommendationLoadGeneration++;
    if (mounted) {
      unawaited(_todayTasksController.clear());
      setState(() {
        _dashboard = null;
        _reminders = [];
        _hasUnread = false;
        _recommendations = null;
        _recommendationLoading = false;
        _recommendationError = null;
        _loading = true;
      });
      unawaited(_load());
    }
  }

  void _onRecommendationProfileChanged() {
    if (!mounted) return;
    // Revoke/decline clears the server profile in the same transaction. Drop
    // any personalized cards immediately while the replacement response is in
    // flight so an IndexedStack-retained Home cannot display stale sensitive
    // content after returning from Privacy or Profile.
    setState(() {
      _recommendations = null;
      _recommendationError = null;
      _recommendationLoading = false;
    });
    unawaited(_loadRecommendations());
  }

  Future<void> _checkUnread({
    required int generation,
    required String? accountId,
  }) async {
    try {
      final notifs = await NotificationService.instance.getNotifications(
        size: 20,
      );
      if (mounted &&
          generation == _loadGeneration &&
          accountId == AuthState.instance.userId) {
        setState(() => _hasUnread = notifs.any((n) => n.isUnread));
      }
    } catch (_) {}
  }

  Future<void> _load() async {
    unawaited(_loadRecommendations());
    final todayRefresh = _todayTasksController.refresh();
    final generation = ++_loadGeneration;
    final isInitialLoad = _dashboard == null;
    if (isInitialLoad) {
      setState(() => _loading = true);
    }
    _checkUnread(generation: generation, accountId: AuthState.instance.userId);
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

  Future<void> _loadRecommendations() async {
    final generation = ++_recommendationLoadGeneration;
    final accountId = AuthState.instance.userId;
    final hasInjectedLoader = widget.recommendationLoader != null;
    if (!hasInjectedLoader && accountId == null) {
      if (mounted && generation == _recommendationLoadGeneration) {
        setState(() {
          _recommendationLoading = false;
          _recommendationError = null;
          _recommendations = null;
        });
      }
      return;
    }
    if (mounted && generation == _recommendationLoadGeneration) {
      setState(() {
        // Keep recommendation progress independent from the main dashboard
        // load while the eligibility context is being resolved.
        _recommendationLoading = true;
        _recommendationError = null;
      });
    }
    try {
      final dashboard =
          _dashboard ??
          await (widget.dashboardLoader?.call() ??
              _journeyService.getDashboard());
      if (!mounted || generation != _recommendationLoadGeneration) return;
      if (accountId != AuthState.instance.userId) return;
      // Recommendation content is meaningful only for an active maternal
      // lifecycle.  In particular, never call the recommendation endpoint
      // for a missing journey, BABY_CARE, or an unknown/retired stage.
      if (!_isRecommendationEligibleDashboard(dashboard)) {
        _clearRecommendationState(generation);
        return;
      }
    } catch (_) {
      // Fail closed: without a trustworthy maternal dashboard we must not
      // fabricate a stage or call the recommendation endpoint.
      _clearRecommendationState(generation);
      return;
    }
    try {
      final response =
          await (widget.recommendationLoader?.call() ??
              _recommendationService.getContent(limit: 3));
      if (!mounted || generation != _recommendationLoadGeneration) return;
      if (accountId != null && accountId != AuthState.instance.userId) return;
      setState(() {
        _recommendations = response;
        _recommendationLoading = false;
        _recommendationError = null;
      });
    } catch (_) {
      if (!mounted || generation != _recommendationLoadGeneration) return;
      if (accountId != null && accountId != AuthState.instance.userId) return;
      setState(() {
        _recommendationLoading = false;
        _recommendationError =
            'Chưa tải được nội dung phù hợp. Vui lòng thử lại.';
      });
    }
  }

  bool _isRecommendationEligibleDashboard(JourneyDashboard dashboard) {
    const activeMaternalStatuses = {
      'ACTIVE_PREGNANCY',
      'ACTIVE_POSTPARTUM',
      'PRE_PREGNANCY',
    };
    return dashboard.hasActiveJourney &&
        dashboard.isMaternalLifecycle &&
        activeMaternalStatuses.contains(dashboard.status);
  }

  void _clearRecommendationState(int generation) {
    if (!mounted || generation != _recommendationLoadGeneration) return;
    setState(() {
      _recommendationLoading = false;
      _recommendationError = null;
      _recommendations = null;
    });
  }

  Future<List<Reminder>> _loadReminders() async {
    if (widget.reminderLoader != null) {
      return widget.reminderLoader!();
    }
    try {
      return await ReminderService.instance.listAppointmentsOrThrow();
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
    return Scaffold(
      backgroundColor: _canvas,
      body: RefreshIndicator(
        color: _primaryContainer,
        onRefresh: _load,
        child: CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            SliverToBoxAdapter(child: _buildTopBar()),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
              sliver: SliverList(
                delegate: SliverChildListDelegate([
                  if (widget.recoveryNotice != null) ...[
                    _buildContinuationRecoveryNotice(),
                    const SizedBox(height: 20),
                  ],
                  _buildGreeting(),
                  const SizedBox(height: 20),
                  if (_loading)
                    _buildDashboardLoadingState()
                  else ...[
                    _buildJourneyCard(),
                    const SizedBox(height: 28),
                    _buildDiscoverSection(),
                    const SizedBox(height: 28),
                    _buildAlertCard(),
                    const SizedBox(height: 28),
                    _buildQuickActions(),
                    const SizedBox(height: 28),
                  ],
                  _buildTasksSection(),
                  const SizedBox(height: 28),
                  _buildRecommendationSection(),
                  const SizedBox(height: 8),
                ]),
              ),
            ),
            const SliverToBoxAdapter(child: SizedBox(height: 104)),
          ],
        ),
      ),
      floatingActionButton: _buildEmergencyMapFab(),
      floatingActionButtonLocation: FloatingActionButtonLocation.endFloat,
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
    return SafeArea(
      bottom: false,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 10, 16, 0),
        child: Row(
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: _surfaceContainerHigh,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: _surfaceContainerHighest),
              ),
              child: const Icon(
                Icons.person_outline_rounded,
                size: 24,
                color: _primary,
              ),
            ),
            const SizedBox(width: 12),
            const Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'CareBridge',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                      color: _primary,
                      letterSpacing: 0.8,
                    ),
                  ),
                  SizedBox(height: 2),
                  Text(
                    'Không gian của mẹ',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: _onSurface,
                    ),
                  ),
                ],
              ),
            ),
            Stack(
              clipBehavior: Clip.none,
              children: [
                DecoratedBox(
                  decoration: BoxDecoration(
                    color: _surface,
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: _surfaceContainerHighest),
                  ),
                  child: IconButton(
                    tooltip: 'Thông báo',
                    onPressed: () => Navigator.of(context)
                        .push(
                          MaterialPageRoute(
                            builder: (_) => const NotificationCenterScreen(),
                          ),
                        )
                        .then(
                          (_) => _checkUnread(
                            generation: _loadGeneration,
                            accountId: AuthState.instance.userId,
                          ),
                        ),
                    icon: const Icon(
                      Icons.notifications_none_rounded,
                      size: 23,
                      color: _primary,
                    ),
                  ),
                ),
                if (_hasUnread)
                  Positioned(
                    top: 8,
                    right: 8,
                    child: Container(
                      width: 9,
                      height: 9,
                      decoration: BoxDecoration(
                        color: _error,
                        shape: BoxShape.circle,
                        border: Border.all(color: _surface, width: 1.5),
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
        const Text('Chào Mẹ,', style: _sectionTitleStyle),
        const SizedBox(height: 6),
        const Text(
          'Hôm nay mẹ và bé cảm thấy thế nào?',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 15,
            color: _onSurfaceVariant,
            height: 1.4,
          ),
        ),
      ],
    );
  }

  Widget _buildDashboardLoadingState() {
    return Container(
      key: const Key('mother-home-dashboard-loading'),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(26),
        border: Border.all(color: _surfaceContainerHighest),
      ),
      child: const Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _SkeletonLine(width: 118, height: 14),
          SizedBox(height: 16),
          _SkeletonLine(width: 152, height: 30),
          SizedBox(height: 12),
          _SkeletonLine(width: double.infinity, height: 14),
          SizedBox(height: 8),
          _SkeletonLine(width: 210, height: 14),
          SizedBox(height: 22),
          _SkeletonLine(width: double.infinity, height: 8),
        ],
      ),
    );
  }

  Widget _buildEmergencyMapFab() {
    return Semantics(
      button: true,
      label: 'Tìm bệnh viện gần đây bằng TrackAsia Map',
      child: SizedBox.square(
        key: const Key('mother-emergency-map-fab'),
        dimension: 64,
        child: Material(
          color: _error,
          elevation: 6,
          shadowColor: const Color(0x55000000),
          shape: const CircleBorder(),
          clipBehavior: Clip.antiAlias,
          child: InkWell(
            key: const Key('mother-emergency-map-action'),
            customBorder: const CircleBorder(),
            onTap: () =>
                context.push('/emergency/map?mode=manual&stage=PREGNANCY'),
            child: const Center(
              child: Icon(
                Icons.local_hospital_outlined,
                color: Colors.white,
                size: 30,
              ),
            ),
          ),
        ),
      ),
    );
  }

  /// TDS MotherExpertDiscoveryInbox §13.1 — Cộng đồng/Bài tập lost their bottom-nav slots to
  /// Chuyên gia/Trò chuyện, so they need a discovery entry point here instead.
  Widget _buildDiscoverSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('Khám phá thêm', style: _sectionTitleStyle),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: _DiscoveryAction(
                icon: Icons.group_outlined,
                label: 'Cộng đồng',
                onTap: () => Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => const CommunityFeedScreen(),
                  ),
                ),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: _DiscoveryAction(
                icon: Icons.self_improvement,
                label: 'Bài tập',
                onTap: () => Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => const MotherExerciseScreen(),
                  ),
                ),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: _DiscoveryAction(
                icon: Icons.menu_book_outlined,
                label: 'Nội dung & FAQ',
                onTap: () => Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => const ViewContentScreen(
                      mode: ContentBrowseMode.lifecycle,
                    ),
                  ),
                ),
              ),
            ),
          ],
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
    final progressValue = progress.clamp(0.0, 1.0).toDouble();

    return Container(
      padding: const EdgeInsets.fromLTRB(20, 20, 20, 18),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(26),
        border: Border.all(color: _surfaceContainerHighest),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [_surface, _surfaceContainerLow],
        ),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF845143).withAlpha(18),
            blurRadius: 24,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Hành trình thai kỳ',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    fontWeight: FontWeight.w700,
                    color: _primary,
                  ),
                ),
                const SizedBox(height: 12),
                Text(
                  title,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 30,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                    letterSpacing: -0.5,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  description,
                  maxLines: 3,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _onSurfaceVariant,
                    height: 1.45,
                  ),
                ),
                const SizedBox(height: 18),
                Row(
                  children: [
                    const Icon(Icons.spa_outlined, size: 17, color: _primary),
                    const SizedBox(width: 6),
                    Text(
                      week != null
                          ? 'Tiếp tục theo dõi mỗi ngày'
                          : 'Đang đồng hành cùng mẹ',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: _primary,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(width: 16),
          Semantics(
            label:
                'Tiến độ hành trình ${(progressValue * 100).round()} phần trăm',
            child: SizedBox(
              width: 96,
              height: 96,
              child: Stack(
                alignment: Alignment.center,
                children: [
                  SizedBox.square(
                    dimension: 92,
                    child: CircularProgressIndicator(
                      value: progressValue,
                      strokeWidth: 9,
                      backgroundColor: _surfaceContainerHighest,
                      valueColor: const AlwaysStoppedAnimation<Color>(
                        _primaryContainer,
                      ),
                    ),
                  ),
                  Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        '${(progressValue * 100).round()}%',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 19,
                          fontWeight: FontWeight.w700,
                          color: _onSurface,
                        ),
                      ),
                      const Text(
                        'tiến độ',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 11,
                          color: _onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNoJourneyCard() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(26),
        border: Border.all(color: _surfaceContainerHighest),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 52,
            height: 52,
            decoration: BoxDecoration(
              color: _surfaceContainerHigh,
              borderRadius: BorderRadius.circular(16),
            ),
            child: const Icon(Icons.route_outlined, color: _primary),
          ),
          const SizedBox(height: 16),
          const Text(
            'Thiết lập hành trình của mẹ',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 19,
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

    return Semantics(
      button: true,
      label: 'Lịch hẹn tiếp theo',
      child: Material(
        color: _surface,
        borderRadius: BorderRadius.circular(22),
        child: InkWell(
          key: const Key('mother-home-next-appointment-card'),
          borderRadius: BorderRadius.circular(22),
          onTap: () async {
            await context.push('/appointments/calendar');
            if (mounted) await _load();
          },
          child: Ink(
            width: double.infinity,
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: _surface,
              borderRadius: BorderRadius.circular(22),
              border: Border.all(color: _surfaceContainerHighest),
            ),
            child: Column(
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      width: 48,
                      height: 48,
                      decoration: BoxDecoration(
                        color: _surfaceContainerLow,
                        borderRadius: BorderRadius.circular(15),
                      ),
                      child: const Icon(
                        Icons.calendar_month_outlined,
                        color: _primary,
                        size: 23,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Lịch hẹn tiếp theo',
                            style: _sectionTitleStyle,
                          ),
                          const SizedBox(height: 4),
                          Text(
                            appointment?.title ??
                                'Chưa có lịch khám sắp tới (Chạm để xem hoặc tạo mới)',
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 13,
                              color: _onSurfaceVariant,
                              height: 1.35,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 6),
                    const Icon(
                      Icons.arrow_outward_rounded,
                      color: _onSurfaceVariant,
                      size: 19,
                    ),
                  ],
                ),
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 14),
                  child: Divider(
                    height: 1,
                    thickness: 1,
                    color: _surfaceContainerHighest,
                  ),
                ),
                Row(
                  children: [
                    const Icon(
                      Icons.schedule_outlined,
                      size: 17,
                      color: _primary,
                    ),
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
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 13,
                          color: _onSurfaceVariant,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildQuickActions() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('Ghi chú nhanh', style: _sectionTitleStyle),
        const SizedBox(height: 5),
        const Text(
          'Ghi lại những điều quan trọng trong ngày',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 13,
            color: _onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 14),
        LayoutBuilder(
          builder: (context, constraints) {
            final tileWidth = (constraints.maxWidth - 12) / 2;
            return Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                SizedBox(
                  width: tileWidth,
                  child: _QuickAction(
                    icon: Icons.calculate_outlined,
                    label: 'Chỉ số BMI',
                    onTap: () => _openQuickMetric('BMI'),
                  ),
                ),
                SizedBox(
                  width: tileWidth,
                  child: _QuickAction(
                    icon: Icons.water_drop_outlined,
                    label: 'Nước uống',
                    onTap: () => _openQuickMetric('HYDRATION'),
                  ),
                ),
                SizedBox(
                  width: tileWidth,
                  child: _QuickAction(
                    icon: Icons.sentiment_satisfied_alt_outlined,
                    label: 'Tâm trạng',
                    onTap: () => _openQuickMetric('MOOD'),
                  ),
                ),
                SizedBox(
                  width: tileWidth,
                  child: _QuickAction(
                    icon: Icons.favorite_border_rounded,
                    label: 'Cử động',
                    onTap: () => _openQuickMetric('FETAL_MOVEMENT_COUNT'),
                  ),
                ),
              ],
            );
          },
        ),
      ],
    );
  }

  Future<void> _openQuickMetric(String metricType) async {
    final dashboard = _dashboard;
    if (dashboard?.journeyId == null || dashboard?.hasActiveJourney != true) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Hãy thiết lập hành trình của mẹ trước khi ghi chú.'),
        ),
      );
      return;
    }
    if (metricType == 'FETAL_MOVEMENT_COUNT' &&
        dashboard?.isPregnancy != true) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Cử động thai chỉ áp dụng cho hành trình thai kỳ đang hoạt động.',
          ),
        ),
      );
      return;
    }

    if (metricType == 'FETAL_MOVEMENT_COUNT') {
      await Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) =>
              FetalMovementTrackerScreen(journeyId: dashboard!.journeyId!),
        ),
      );
      return;
    }

    if (metricType == 'MOOD') {
      if (dashboard?.isPregnancy != true && dashboard?.isPostpartum != true) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'EPDS áp dụng cho hành trình mang thai hoặc sau sinh.',
            ),
          ),
        );
        return;
      }
      await Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => EpdsScreen(journeyId: dashboard!.journeyId!),
        ),
      );
      return;
    }

    final saved = await context.push<bool>(
      '/journeys/${Uri.encodeComponent(dashboard!.journeyId!)}'
      '/metrics/add?metricType=${Uri.encodeQueryComponent(metricType)}',
    );
    if (saved == true && mounted) await _load();
  }

  Widget _buildTasksSection() => TodayTasksPanel(
    service: _todayTaskService,
    audience: TodayTasksAudience.mother,
    layout: TodayTasksLayout.sourceGroups,
    controller: _todayTasksController,
    headingAction: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        IconButton(
          key: const Key('mother-home-checklist-history-button'),
          tooltip: 'Lịch sử checklist',
          onPressed: () => context.push('/checklists/history'),
          icon: const Icon(Icons.history_rounded),
          color: _primary,
        ),
        IconButton(
          key: const Key('mother-home-reminder-schedules-button'),
          tooltip: 'Lịch nhắc',
          onPressed: () => context.push('/reminder-schedules'),
          icon: const Icon(Icons.alarm_rounded),
          color: _primary,
        ),
        if (_dashboard?.journeyId?.isNotEmpty ?? false)
          AddUserChecklistTaskButton(journeyId: _dashboard!.journeyId),
      ],
    ),
  );

  // ignore: unused_element, legacy lifecycle-week heading retained for compatibility.
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
    return Semantics(
      button: true,
      label: 'Ghi chú $label',
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(20),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(20),
          child: Ink(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: _MotherHomeScreenState._surface,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: _MotherHomeScreenState._surfaceContainerHighest,
              ),
            ),
            child: Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: _MotherHomeScreenState._surfaceContainerLow,
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: Icon(
                    icon,
                    size: 22,
                    color: _MotherHomeScreenState._primary,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    label,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: _MotherHomeScreenState._onSurface,
                      height: 1.25,
                    ),
                  ),
                ),
                const SizedBox(width: 4),
                const Icon(
                  Icons.add_rounded,
                  size: 19,
                  color: _MotherHomeScreenState._primary,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _DiscoveryAction extends StatelessWidget {
  const _DiscoveryAction({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: label,
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(18),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(18),
          child: Ink(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 12),
            decoration: BoxDecoration(
              color: _MotherHomeScreenState._surfaceContainerLow,
              borderRadius: BorderRadius.circular(18),
            ),
            child: Column(
              children: [
                Icon(icon, color: _MotherHomeScreenState._primary, size: 23),
                const SizedBox(height: 8),
                Text(
                  label,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                    color: _MotherHomeScreenState._onSurfaceVariant,
                    height: 1.25,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _SkeletonLine extends StatelessWidget {
  const _SkeletonLine({required this.width, required this.height});

  final double width;
  final double height;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        color: _MotherHomeScreenState._surfaceContainerLow,
        borderRadius: BorderRadius.circular(8),
      ),
    );
  }
}

extension _MotherHomeRecommendationView on _MotherHomeScreenState {
  Widget _buildRecommendationSection() {
    final response = _recommendations;
    if (response == null && _recommendationLoading) {
      return const _RecommendationLoadingState();
    }
    if (response == null && _recommendationError != null) {
      return _RecommendationErrorState(
        message: _recommendationError!,
        onRetry: _loadRecommendations,
      );
    }
    if (response == null) return const SizedBox.shrink();
    final items = response.items;
    final title = switch (response.stage) {
      'PRE_PREGNANCY' => 'Gợi ý cho chuẩn bị mang thai',
      'PREGNANCY' when response.pregnancyWeek != null =>
        'Gợi ý dành riêng cho tuần ${response.pregnancyWeek}',
      'PREGNANCY' => 'Gợi ý cho thai kỳ',
      'POSTPARTUM' => 'Gợi ý cho sau sinh',
      'BABY_CARE' => 'Gợi ý chăm sóc bé',
      _ => 'Gợi ý dành cho bạn',
    };
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: _MotherHomeScreenState._sectionTitleStyle),
        const SizedBox(height: 5),
        const Text(
          'Nội dung được chọn theo giai đoạn hiện tại của mẹ',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 13,
            color: _MotherHomeScreenState._onSurfaceVariant,
          ),
        ),
        if (response.profileStatus ==
                RecommendationProfileStatus.reviewRequired ||
            response.profileStatus ==
                RecommendationProfileStatus.reconsentRequired) ...[
          const SizedBox(height: 4),
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton(
              key: const Key('mother-home-recommendation-review-profile'),
              onPressed: () => context.push(
                '/recommendation-profile',
                extra: response.stage,
              ),
              child: const Text('Xem lại hồ sơ cá nhân hóa'),
            ),
          ),
        ],
        if (_recommendationLoading) ...[
          const SizedBox(height: 8),
          const LinearProgressIndicator(
            key: Key('mother-home-recommendation-refreshing'),
            minHeight: 4,
            color: _MotherHomeScreenState._primaryContainer,
            backgroundColor: _MotherHomeScreenState._surfaceContainerLow,
          ),
        ],
        if (_recommendationError != null) ...[
          const SizedBox(height: 8),
          _RecommendationErrorState(
            message: _recommendationError!,
            onRetry: _loadRecommendations,
          ),
        ],
        if (response.selectionMode == 'FALLBACK_ONLY')
          const _RecommendationCoverageNotice(
            key: Key('mother-home-recommendation-fallback-only'),
            message: 'Đây là nội dung nền an toàn cho giai đoạn hiện tại.',
          ),
        if (response.coverageStatus == 'PARTIAL')
          _RecommendationCoverageNotice(
            key: const Key('mother-home-recommendation-partial'),
            message:
                'Hiện có một số nội dung phù hợp. Bạn có thể xem thêm trong thư viện.',
            onBrowse: () => context.push('/content'),
          ),
        if (response.coverageStatus == 'EMPTY')
          _RecommendationCoverageNotice(
            key: const Key('mother-home-recommendation-empty-coverage'),
            message:
                'Chưa có bài viết phù hợp; hãy xem toàn bộ thư viện nội dung.',
            onBrowse: () => context.push('/content'),
          ),
        const SizedBox(height: 14),
        if (items.isNotEmpty) ...items.map(_buildRecommendationCard),
      ],
    );
  }

  Widget _buildRecommendationCard(RecommendationContentItem item) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Semantics(
        button: true,
        label: item.title,
        child: Card(
          key: Key('mother-home-recommendation-card-${item.id}'),
          margin: EdgeInsets.zero,
          color: _MotherHomeScreenState._surface,
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: const BorderSide(
              color: _MotherHomeScreenState._surfaceContainerHighest,
            ),
          ),
          child: InkWell(
            borderRadius: BorderRadius.circular(16),
            onTap: () => _openRecommendation(item.id),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(
                    Icons.menu_book_outlined,
                    color: _MotherHomeScreenState._primary,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          item.title,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 15,
                            fontWeight: FontWeight.w600,
                            color: _MotherHomeScreenState._onSurface,
                          ),
                        ),
                        if (item.summary?.trim().isNotEmpty ?? false) ...[
                          const SizedBox(height: 4),
                          Text(
                            item.summary!,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 13,
                              color: _MotherHomeScreenState._onSurfaceVariant,
                              height: 1.35,
                            ),
                          ),
                        ],
                        const SizedBox(height: 8),
                        Text(
                          item.reasonLabel,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            color: _MotherHomeScreenState._primary,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 8),
                  const Icon(
                    Icons.chevron_right_rounded,
                    color: _MotherHomeScreenState._onSurfaceVariant,
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  void _openRecommendation(String contentId) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => VerifiedContentDetailScreen(
          contentId: contentId,
          mode: ContentBrowseMode.lifecycle,
          contentService: ContentService.instance,
        ),
      ),
    );
  }
}

class _RecommendationLoadingState extends StatelessWidget {
  const _RecommendationLoadingState();

  @override
  Widget build(BuildContext context) {
    return Container(
      key: const Key('mother-home-recommendation-loading'),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: _MotherHomeScreenState._surface,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(
          color: _MotherHomeScreenState._surfaceContainerHighest,
        ),
      ),
      child: const Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _SkeletonLine(width: 156, height: 16),
          SizedBox(height: 12),
          _SkeletonLine(width: double.infinity, height: 13),
          SizedBox(height: 8),
          _SkeletonLine(width: 210, height: 13),
        ],
      ),
    );
  }
}

class _RecommendationErrorState extends StatelessWidget {
  const _RecommendationErrorState({
    required this.message,
    required this.onRetry,
  });

  final String message;
  final Future<void> Function() onRetry;

  @override
  Widget build(BuildContext context) {
    return Container(
      key: const Key('mother-home-recommendation-error'),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: _MotherHomeScreenState._surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
          color: _MotherHomeScreenState._surfaceContainerHighest,
        ),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.info_outline_rounded,
            color: _MotherHomeScreenState._primary,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              message,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                color: _MotherHomeScreenState._onSurfaceVariant,
              ),
            ),
          ),
          IconButton(
            key: const Key('mother-home-recommendation-retry'),
            tooltip: 'Thử lại',
            onPressed: () => unawaited(onRetry()),
            icon: const Icon(Icons.refresh_rounded),
            color: _MotherHomeScreenState._primary,
          ),
        ],
      ),
    );
  }
}

class _RecommendationCoverageNotice extends StatelessWidget {
  const _RecommendationCoverageNotice({
    super.key,
    required this.message,
    this.onBrowse,
  });

  final String message;
  final VoidCallback? onBrowse;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(top: 10),
      padding: const EdgeInsets.fromLTRB(14, 12, 8, 12),
      decoration: BoxDecoration(
        color: _MotherHomeScreenState._surfaceContainerLow,
        borderRadius: BorderRadius.circular(18),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.menu_book_outlined,
            color: _MotherHomeScreenState._primary,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              message,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                color: _MotherHomeScreenState._onSurfaceVariant,
                height: 1.35,
              ),
            ),
          ),
          if (onBrowse != null)
            TextButton(onPressed: onBrowse, child: const Text('Xem thêm')),
        ],
      ),
    );
  }
}
