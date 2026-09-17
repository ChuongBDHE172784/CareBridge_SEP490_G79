import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/auth/auth_state.dart';
import '../../../shared/components/app_user_avatar.dart';
import '../../auth/services/auth_service.dart';
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
import '../../baby/models/baby_model.dart';
import '../../baby/services/baby_service.dart';
import '../../community/models/content_model.dart';
import '../../community/screens/verified_content_detail_screen.dart';
import '../../community/services/content_service.dart';
import '../../exercise/screens/mother_exercise_screen.dart';
import '../../healthRecords/screens/fetal_movement_tracker_screen.dart';
import '../../healthRecords/screens/epds_screen.dart';
import '../../recommendation/models/recommendation_model.dart';
import '../../recommendation/services/recommendation_service.dart';
import '../../safety/models/safety_config_model.dart';
import '../../safety/services/safety_service.dart';
import '../../safety/services/safety_foreground_service.dart';

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
    this.babyService,
    this.babyLoader,
    this.contentService,
    this.babyContentLoader,
    this.safetyService,
    this.safetyConfigLoader,
    this.safetyCoordinator,
  });

  final String? recoveryNotice;
  final TodayTaskService? todayTaskService;
  final Future<JourneyDashboard> Function()? dashboardLoader;
  final Future<List<Reminder>> Function()? reminderLoader;
  final RecommendationService? recommendationService;
  final Future<RecommendationContentResponse> Function()? recommendationLoader;
  final BabyService? babyService;
  final Future<List<BabyProfile>> Function()? babyLoader;
  final ContentService? contentService;
  final Future<List<ContentListItem>> Function()? babyContentLoader;
  final SafetyService? safetyService;
  final Future<SafetyConfig> Function()? safetyConfigLoader;
  final SafetyForegroundServiceCoordinator? safetyCoordinator;

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
    letterSpacing: -0.3,
  );

  final _journeyService = JourneyService();
  late final TodayTaskService _todayTaskService;
  late final RecommendationService _recommendationService;
  late final BabyService _babyService;
  late final ContentService _contentService;
  late final SafetyService _safetyService;
  late final SafetyForegroundServiceCoordinator _foregroundCoordinator;
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
  List<BabyProfile> _babies = [];
  List<RecommendationContentItem> _babyRecommendations = [];
  String? _recommendationError;
  String? _observedAccountId;
  String? _userAvatarUrl;
  SafetyConfig? _safetyConfig;
  bool _safetyLoaded = false;
  int _safetyLoadGeneration = 0;

  @override
  void initState() {
    super.initState();
    _todayTaskService = widget.todayTaskService ?? TodayTaskService.instance;
    _recommendationService =
        widget.recommendationService ?? RecommendationService();
    _babyService = widget.babyService ?? BabyService();
    _contentService = widget.contentService ?? ContentService.instance;
    _safetyService = widget.safetyService ?? SafetyService();
    _foregroundCoordinator =
        widget.safetyCoordinator ?? SafetyForegroundServiceCoordinator.instance;
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
        _babies = [];
        _babyRecommendations = [];
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

  /// [BƯỚC 1: TIẾP NHẬN DỮ LIỆU TUẦN THAI TỪ BACKEND]
  /// Tải dữ liệu tổng quan hành trình của mẹ (bao gồm tuần thai, tam cá nguyệt, ngày dự sinh)
  /// thông qua JourneyService.getDashboard() -> Gọi API `GET /api/v1/journeys/me/dashboard`
  Future<void> _load() async {
    unawaited(_loadRecommendations());
    unawaited(_loadSafetyStatus());
    final todayRefresh = _todayTasksController.refresh();
    final generation = ++_loadGeneration;
    final isInitialLoad = _dashboard == null;
    if (isInitialLoad) {
      setState(() => _loading = true);
    }
    _checkUnread(generation: generation, accountId: AuthState.instance.userId);
    try {
      final profile = await AuthService.instance.getProfile();
      if (mounted && generation == _loadGeneration) {
        setState(() => _userAvatarUrl = profile.avatarUrl);
      }
    } catch (_) {}
    try {
      // Gọi Service để gửi request lấy Dashboard dữ liệu tuổi thai từ Backend
      final dashboard =
          await (widget.dashboardLoader?.call() ??
              _journeyService.getDashboard());
      final reminders = await _loadReminders();
      if (mounted && generation == _loadGeneration) {
        setState(() {
          // Cập nhật State Dashboard (chứa pregnancyWeek, completedGestationalDays, trimester, plan...)
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

  /// [BƯỚC 1: KIỂM TRA ĐIỀU KIỆN & GỌI API GỢI Ý BÀI VIẾT TỪ FRONTEND]
  /// 1. Tải hồ sơ bé (BabyProfile) và bài viết chăm sóc bé tương ứng (nếu có).
  /// 2. Kiểm tra hành trình thai kỳ có đang hoạt động (ACTIVE_PREGNANCY, ACTIVE_POSTPARTUM, PRE_PREGNANCY)
  ///    và gọi RecommendationService.getContent(limit: 10) -> GET /api/v1/recommendations/content.
  /// 3. Cập nhật State `_recommendations` và `_babyRecommendations` để render danh sách gợi ý.
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
          _babies = [];
          _babyRecommendations = [];
        });
      }
      return;
    }
    if (mounted && generation == _recommendationLoadGeneration) {
      setState(() {
        _recommendationLoading = true;
        _recommendationError = null;
      });
    }

    // (1) Lấy thông tin Dashboard hành trình hiện tại
    JourneyDashboard? dashboard;
    try {
      dashboard =
          _dashboard ??
          await (widget.dashboardLoader?.call() ??
              _journeyService.getDashboard());
    } catch (_) {
      dashboard = null;
    }

    // (2) Lấy danh sách hồ sơ bé của tài khoản
    List<BabyProfile> babies = [];
    try {
      babies = await (widget.babyLoader?.call() ??
          _babyService.listBabyProfiles());
    } catch (_) {
      babies = [];
    }

    // (3) Tải bài viết chăm sóc bé nếu tài khoản có hồ sơ bé
    List<RecommendationContentItem> babyRecs = [];
    if (babies.isNotEmpty) {
      try {
        final activeBaby = babies.first;
        final rawContent = await (widget.babyContentLoader?.call() ??
            _loadBabyContentFromApi());
        babyRecs = _rankBabyRecommendations(rawContent, activeBaby);
      } catch (_) {
        babyRecs = [];
      }
    }

    // (4) Chỉ gọi API gợi ý cho mẹ khi mẹ đang ở giai đoạn thai kỳ/sau sinh hợp lệ
    RecommendationContentResponse? maternalResponse;
    String? maternalError;
    final isMaternalEligible =
        dashboard != null && _isRecommendationEligibleDashboard(dashboard);

    if (isMaternalEligible) {
      try {
        maternalResponse =
            await (widget.recommendationLoader?.call() ??
                _recommendationService.getContent(limit: 10));
      } catch (_) {
        if (babies.isEmpty) {
          maternalError = 'Chưa tải được nội dung phù hợp. Vui lòng thử lại.';
        }
      }
    }

    if (!mounted || generation != _recommendationLoadGeneration) return;
    if (accountId != null && accountId != AuthState.instance.userId) return;

    if (!isMaternalEligible && babies.isEmpty) {
      _clearRecommendationState(generation);
      return;
    }

    setState(() {
      _recommendations = maternalResponse;
      _babies = babies;
      _babyRecommendations = babyRecs;
      _recommendationLoading = false;
      _recommendationError = maternalError;
    });
  }

  Future<List<ContentListItem>> _loadBabyContentFromApi() async {
    final results = <ContentListItem>[];
    final seenIds = <String>{};
    try {
      final babyCareItems = await _contentService.getContent(
        stage: 'BABY_CARE',
        type: 'ARTICLE',
        size: 20,
      );
      for (final item in babyCareItems) {
        if (seenIds.add(item.id)) results.add(item);
      }
    } catch (_) {}

    try {
      final postpartumItems = await _contentService.getContent(
        stage: 'POSTPARTUM',
        type: 'ARTICLE',
        size: 30,
      );
      for (final item in postpartumItems) {
        if (seenIds.add(item.id)) results.add(item);
      }
    } catch (_) {}

    return results;
  }

  List<RecommendationContentItem> _rankBabyRecommendations(
    List<ContentListItem> items,
    BabyProfile baby,
  ) {
    final babyItems = items.where((item) => _isBabyCareContent(item)).toList();
    final days = DateTime.now().difference(baby.birthDate).inDays;
    final isNewborn = days <= 30;

    babyItems.sort((a, b) {
      if (isNewborn) {
        final aNewborn =
            a.title.toLowerCase().contains('sơ sinh') ||
            (a.summary?.toLowerCase().contains('sơ sinh') ?? false);
        final bNewborn =
            b.title.toLowerCase().contains('sơ sinh') ||
            (b.summary?.toLowerCase().contains('sơ sinh') ?? false);
        if (aNewborn && !bNewborn) return -1;
        if (!aNewborn && bNewborn) return 1;
      }
      return 0;
    });

    return babyItems.take(10).toList().asMap().entries.map((entry) {
      final index = entry.key;
      final item = entry.value;
      final isDirectNewborn =
          isNewborn &&
          (item.title.toLowerCase().contains('sơ sinh') ||
              (item.summary?.toLowerCase().contains('sơ sinh') ?? false));

      final reasonLabel = isDirectNewborn
          ? 'Phù hợp cho ${baby.nickname} (${baby.ageLabel})'
          : 'Hữu ích cho sự phát triển của ${baby.nickname}';

      return RecommendationContentItem(
        rank: index + 1,
        selectionType: RecommendationSelectionType.targeted,
        reasonCode: 'BABY_CARE_CONTEXT',
        reasonLabel: reasonLabel,
        id: item.id,
        title: item.title,
        summary: item.summary,
        stage: item.stage.isNotEmpty ? item.stage : 'BABY_CARE',
      );
    }).toList();
  }

  bool _isBabyCareContent(ContentListItem item) {
    if (item.stage == 'BABY_CARE') return true;
    final title = item.title.toLowerCase();
    final summary = (item.summary ?? '').toLowerCase();

    if (title.contains('3 tình trạng') ||
        title.contains('sau sinh thường') ||
        title.contains('hỗ trợ trong giai đoạn')) {
      return false;
    }

    const babyKeywords = [
      'trẻ',
      'sơ sinh',
      'bé',
      'bú',
      'tắm',
      'ngủ',
      'sữa mẹ',
      'phát triển của trẻ',
      'nuôi con',
    ];
    return babyKeywords.any((k) => title.contains(k) || summary.contains(k));
  }

  /// Kiểm tra Dashboard người mẹ có đủ điều kiện nhận gợi ý cá nhân hóa không
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
      _babies = [];
      _babyRecommendations = [];
    });
  }

  Future<void> _loadSafetyStatus() async {
    final generation = ++_safetyLoadGeneration;
    final accountId = AuthState.instance.userId;
    final hasInjectedLoader = widget.safetyConfigLoader != null;
    if (!hasInjectedLoader && accountId == null) {
      if (mounted && generation == _safetyLoadGeneration) {
        setState(() {
          _safetyLoaded = true;
          _safetyConfig = null;
        });
      }
      return;
    }
    try {
      final config = await (widget.safetyConfigLoader?.call() ??
          _safetyService.getConfig());
      if (_foregroundCoordinator.isSupported &&
          AuthState.instance.isAuthenticated) {
        await _foregroundCoordinator.reconcile();
      }
      if (mounted && generation == _safetyLoadGeneration) {
        setState(() {
          _safetyConfig = config;
          _safetyLoaded = true;
        });
      }
    } catch (_) {
      if (mounted && generation == _safetyLoadGeneration) {
        setState(() {
          _safetyLoaded = true;
        });
      }
    }
  }

  bool get _isSafetyMonitoringActive {
    if (!_safetyLoaded) return true;
    final config = _safetyConfig;
    if (config == null || !config.fallDetectionEnabled) {
      return false;
    }
    if (_foregroundCoordinator.isSupported && !_foregroundCoordinator.isRunning) {
      return false;
    }
    return true;
  }

  bool get _showSafetyMonitoringReminder =>
      !_loading && _safetyLoaded && !_isSafetyMonitoringActive;

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
        backgroundColor: _surface,
        onRefresh: _load,
        child: CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            SliverToBoxAdapter(child: _buildTopBar()),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
              sliver: SliverToBoxAdapter(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
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
                      const SizedBox(height: 26),
                      _buildDiscoverSection(),
                      const SizedBox(height: 26),
                      _buildAlertCard(),
                      const SizedBox(height: 26),
                      _buildQuickActions(),
                      const SizedBox(height: 26),
                    ],
                    _buildTasksSection(),
                    const SizedBox(height: 26),
                    _buildRecommendationSection(),
                    const SizedBox(height: 12),
                  ],
                ),
              ),
            ),
            const SliverToBoxAdapter(child: SizedBox(height: 108)),
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
          color: const Color(0xFFFBF4EE),
          borderRadius: BorderRadius.circular(22),
          border: const Border(
            left: BorderSide(color: _primaryContainer, width: 4),
          ),
          boxShadow: [
            BoxShadow(
              color: _primary.withValues(alpha: 0.06),
              blurRadius: 14,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: _surfaceContainerHigh,
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Icon(Icons.info_outline_rounded, color: _primary, size: 20),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                message,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
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
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 4),
        child: Row(
          children: [
            Container(
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                boxShadow: [
                  BoxShadow(
                    color: _primary.withValues(alpha: 0.12),
                    blurRadius: 10,
                    offset: const Offset(0, 3),
                  ),
                ],
              ),
              child: AppUserAvatar(
                avatarUrl: _userAvatarUrl,
                radius: 23,
                backgroundColor: _surfaceContainerHigh,
                border: Border.all(color: _surfaceContainerHighest, width: 1.5),
                onTap: () => context.push('/profile'),
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Container(
                        width: 6,
                        height: 6,
                        margin: const EdgeInsets.only(right: 6),
                        decoration: const BoxDecoration(
                          color: _primaryContainer,
                          shape: BoxShape.circle,
                        ),
                      ),
                      const Text(
                        'CareBridge',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                          color: _primary,
                          letterSpacing: 0.8,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 2),
                  const Text(
                    'Không gian của mẹ',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 15,
                      fontWeight: FontWeight.w700,
                      color: _onSurface,
                      letterSpacing: -0.2,
                    ),
                  ),
                ],
              ),
            ),
            Stack(
              clipBehavior: Clip.none,
              children: [
                Container(
                  decoration: BoxDecoration(
                    color: _surface,
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: _surfaceContainerHighest.withValues(alpha: 0.9)),
                    boxShadow: [
                      BoxShadow(
                        color: _primary.withValues(alpha: 0.07),
                        blurRadius: 12,
                        offset: const Offset(0, 4),
                      ),
                    ],
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
                    top: 6,
                    right: 6,
                    child: Container(
                      width: 10,
                      height: 10,
                      decoration: BoxDecoration(
                        color: _error,
                        shape: BoxShape.circle,
                        border: Border.all(color: _surface, width: 2),
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
        const SizedBox(height: 4),
        const Text(
          'Hôm nay mẹ và bé cảm thấy thế nào?',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 14,
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
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(26),
        border: Border.all(color: _surfaceContainerHighest),
        boxShadow: [
          BoxShadow(
            color: _primary.withValues(alpha: 0.06),
            blurRadius: 18,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: const Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _SkeletonLine(width: 120, height: 14),
          SizedBox(height: 16),
          _SkeletonLine(width: 160, height: 32),
          SizedBox(height: 12),
          _SkeletonLine(width: double.infinity, height: 14),
          SizedBox(height: 8),
          _SkeletonLine(width: 220, height: 14),
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
        child: Container(
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            boxShadow: [
              BoxShadow(
                color: _error.withValues(alpha: 0.38),
                blurRadius: 18,
                spreadRadius: 2,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: Material(
            color: _error,
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

  /// [HIỂN THỊ GIAO DIỆN: THẺ TIẾN ĐỘ THAI KỲ & TUẦN THAI HIỆN TẠI]
  /// Sử dụng `displayPregnancyWeek` (hoặc `pregnancyWeek`) và `pregnancyProgress`
  /// tính từ `completedGestationalWeek / 40` để hiển thị trực quan cho mẹ bầu.
  Widget _buildJourneyCard() {
    final d = _dashboard;
    if (d?.hasActiveJourney != true) {
      return _buildNoJourneyCard();
    }

    // Lấy tuần thai hiển thị (1-based: ví dụ Tuần 12)
    final week = d!.displayPregnancyWeek;
    // Lấy tỷ lệ tiến độ hoàn thành thai kỳ (0.0 đến 1.0)
    final progress = d.pregnancyProgress;
    final title = week != null ? 'Tuần $week' : d.phaseLabel;
    final description = week != null
        ? 'Bé đang lớn bằng ${d.fruitName}, ${d.fruitSizeNote}.'
        : 'CareBridge đang theo dõi hành trình từ dữ liệu mẹ đã thiết lập.';
    final progressValue = progress.clamp(0.0, 1.0).toDouble();

    return Container(
      padding: const EdgeInsets.fromLTRB(22, 22, 20, 20),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: _surfaceContainerHighest.withValues(alpha: 0.8)),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [_surface, _surfaceContainerLow],
        ),
        boxShadow: [
          BoxShadow(
            color: _primary.withValues(alpha: 0.10),
            blurRadius: 26,
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
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: _surfaceContainerHigh,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: const Text(
                    'Hành trình thai kỳ',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                      color: _primary,
                      letterSpacing: 0.2,
                    ),
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  title,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 30,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                    letterSpacing: -0.6,
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
                const SizedBox(height: 16),
                Row(
                  children: [
                    const Icon(Icons.spa_outlined, size: 16, color: _primary),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Text(
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
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(width: 14),
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
                      strokeWidth: 8.5,
                      strokeCap: StrokeCap.round,
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
                          fontSize: 20,
                          fontWeight: FontWeight.w700,
                          color: _onSurface,
                          letterSpacing: -0.5,
                        ),
                      ),
                      const Text(
                        'tiến độ',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 11,
                          fontWeight: FontWeight.w500,
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
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(26),
        border: Border.all(color: _surfaceContainerHighest),
        boxShadow: [
          BoxShadow(
            color: _primary.withValues(alpha: 0.06),
            blurRadius: 20,
            offset: const Offset(0, 8),
          ),
        ],
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
            child: const Icon(Icons.route_outlined, color: _primary, size: 26),
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
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(24),
          boxShadow: [
            BoxShadow(
              color: _primary.withValues(alpha: 0.07),
              blurRadius: 18,
              offset: const Offset(0, 6),
            ),
          ],
        ),
        child: Material(
          color: _surface,
          borderRadius: BorderRadius.circular(24),
          child: InkWell(
            key: const Key('mother-home-next-appointment-card'),
            borderRadius: BorderRadius.circular(24),
            onTap: () async {
              await context.push('/appointments/calendar');
              if (mounted) await _load();
            },
            child: Ink(
              width: double.infinity,
              padding: const EdgeInsets.all(18),
              decoration: BoxDecoration(
                color: _surface,
                borderRadius: BorderRadius.circular(24),
                border: Border.all(color: _surfaceContainerHighest.withValues(alpha: 0.9)),
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
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: _surfaceContainerHighest),
                        ),
                        child: const Icon(
                          Icons.calendar_month_outlined,
                          color: _primary,
                          size: 23,
                        ),
                      ),
                      const SizedBox(width: 14),
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
                      Container(
                        padding: const EdgeInsets.all(6),
                        decoration: BoxDecoration(
                          color: _surfaceContainerLow,
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.arrow_outward_rounded,
                          color: _primary,
                          size: 16,
                        ),
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
                            fontWeight: FontWeight.w500,
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
      ),
    );
  }

  Widget _buildQuickActions() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('Ghi chú nhanh', style: _sectionTitleStyle),
        const SizedBox(height: 4),
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
    belowHeading: _showSafetyMonitoringReminder
        ? _buildSafetyMonitoringReminder()
        : null,
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

  Widget _buildSafetyMonitoringReminder() {
    return Semantics(
      container: true,
      label: 'Lời nhắc kích hoạt giám sát an toàn cảm biến IMU phát hiện ngã',
      child: Container(
        key: const Key('mother-home-safety-reminder-card'),
        decoration: BoxDecoration(
          color: _surface,
          borderRadius: BorderRadius.circular(24),
          border: Border.all(
            color: _surfaceContainerHighest.withValues(alpha: 0.9),
          ),
          boxShadow: [
            BoxShadow(
              color: _primary.withValues(alpha: 0.06),
              blurRadius: 18,
              offset: const Offset(0, 6),
            ),
          ],
        ),
        child: Material(
          color: Colors.transparent,
          borderRadius: BorderRadius.circular(24),
          child: InkWell(
            borderRadius: BorderRadius.circular(24),
            onTap: () async {
              await context.push('/safety');
              if (mounted) await _loadSafetyStatus();
            },
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        width: 46,
                        height: 46,
                        decoration: BoxDecoration(
                          color: _surfaceContainerLow,
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: _surfaceContainerHighest),
                        ),
                        child: const Icon(
                          Icons.health_and_safety_outlined,
                          color: _primary,
                          size: 24,
                        ),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                const Expanded(
                                  child: Text(
                                    'Giám sát an toàn',
                                    style: TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 16,
                                      fontWeight: FontWeight.w700,
                                      color: _onSurface,
                                      letterSpacing: -0.2,
                                    ),
                                  ),
                                ),
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 10,
                                    vertical: 4,
                                  ),
                                  decoration: BoxDecoration(
                                    color: const Color(0xFFFFEAE4),
                                    borderRadius: BorderRadius.circular(999),
                                    border: Border.all(
                                      color: _primaryContainer.withValues(alpha: 0.4),
                                    ),
                                  ),
                                  child: Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Container(
                                        width: 6,
                                        height: 6,
                                        decoration: const BoxDecoration(
                                          color: Color(0xFFBA1A1A),
                                          shape: BoxShape.circle,
                                        ),
                                      ),
                                      const SizedBox(width: 5),
                                      const Text(
                                        'Chưa bật',
                                        style: TextStyle(
                                          fontFamily: 'Lexend',
                                          fontSize: 11,
                                          fontWeight: FontWeight.w600,
                                          color: _primary,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 6),
                            const Text(
                              'Kích hoạt cảm biến IMU và phát hiện ngã để CareBridge kịp thời nhận diện sự cố bất thường và gửi cảnh báo khẩn cấp bảo vệ mẹ.',
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 13,
                                color: _onSurfaceVariant,
                                height: 1.4,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Align(
                    alignment: Alignment.centerRight,
                    child: ElevatedButton.icon(
                      key: const Key('mother-home-enable-safety-button'),
                      onPressed: () async {
                        await context.push('/safety');
                        if (mounted) await _loadSafetyStatus();
                      },
                      style: ElevatedButton.styleFrom(
                        backgroundColor: _primary,
                        foregroundColor: Colors.white,
                        elevation: 0,
                        padding: const EdgeInsets.symmetric(
                          horizontal: 18,
                          vertical: 10,
                        ),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(999),
                        ),
                      ),
                      icon: const Icon(Icons.shield_outlined, size: 16),
                      label: const Text(
                        'Bật giám sát an toàn',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

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
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(20),
          boxShadow: [
            BoxShadow(
              color: _MotherHomeScreenState._primary.withValues(alpha: 0.04),
              blurRadius: 12,
              offset: const Offset(0, 4),
            ),
          ],
        ),
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
                  color: _MotherHomeScreenState._surfaceContainerHighest.withValues(alpha: 0.9),
                ),
              ),
              child: Row(
                children: [
                  Container(
                    width: 42,
                    height: 42,
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
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(20),
          boxShadow: [
            BoxShadow(
              color: _MotherHomeScreenState._primary.withValues(alpha: 0.04),
              blurRadius: 10,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Material(
          color: Colors.transparent,
          borderRadius: BorderRadius.circular(20),
          child: InkWell(
            onTap: onTap,
            borderRadius: BorderRadius.circular(20),
            child: Ink(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 14),
              decoration: BoxDecoration(
                color: _MotherHomeScreenState._surfaceContainerLow,
                borderRadius: BorderRadius.circular(20),
                border: Border.all(
                  color: _MotherHomeScreenState._surfaceContainerHighest.withValues(alpha: 0.6),
                ),
              ),
              child: Column(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: _MotherHomeScreenState._surface,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      icon,
                      color: _MotherHomeScreenState._primary,
                      size: 22,
                    ),
                  ),
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
  /// [BƯỚC 5: RENDER GIAO DIỆN RECOMMENDATION WIDGET TRÊN HOME SCREEN]
  /// Hiển thị danh sách các bài viết gợi ý tổng hợp cho mẹ và bé,
  /// được sắp xếp theo mức độ ưu tiên lâm sàng và chăm sóc thiết yếu.
  Widget _buildRecommendationSection() {
    final response = _recommendations;
    final hasBaby = _babies.isNotEmpty && _babyRecommendations.isNotEmpty;

    // (1) Trạng thái đang tải lần đầu: hiển thị Skeleton Loading
    if (response == null && !hasBaby && _recommendationLoading) {
      return const _RecommendationLoadingState();
    }
    // (2) Trạng thái lỗi tải dữ liệu: hiển thị nút thử lại
    if (response == null && !hasBaby && _recommendationError != null) {
      return _RecommendationErrorState(
        message: _recommendationError!,
        onRetry: _loadRecommendations,
      );
    }
    if (response == null && !hasBaby) return const SizedBox.shrink();

    final activeBaby = _babies.firstOrNull;
    final items = _getUnifiedPrioritizedRecommendations();
    final hasBoth = response != null && hasBaby;

    final String sectionTitle;
    final String sectionSubtitle;

    if (hasBoth) {
      sectionTitle = 'Bài viết gợi ý cho mẹ và bé';
      sectionSubtitle =
          'Nội dung chăm sóc phù hợp được sắp xếp theo mức độ ưu tiên';
    } else if (response != null) {
      sectionTitle = switch (response.stage) {
        'PRE_PREGNANCY' => 'Gợi ý cho chuẩn bị mang thai',
        'PREGNANCY' when response.pregnancyWeek != null =>
          'Gợi ý dành riêng cho tuần ${response.pregnancyWeek}',
        'PREGNANCY' => 'Gợi ý cho thai kỳ',
        'POSTPARTUM' => 'Gợi ý cho sau sinh',
        'BABY_CARE' => 'Gợi ý chăm sóc bé',
        _ => 'Gợi ý dành cho bạn',
      };
      sectionSubtitle = 'Nội dung được chọn theo giai đoạn hiện tại của mẹ';
    } else {
      sectionTitle = 'Gợi ý chăm sóc bé';
      sectionSubtitle = activeBaby != null
          ? 'Nội dung chăm sóc phù hợp cho bé ${activeBaby.ageLabel}'
          : 'Nội dung gợi ý chăm sóc bé';
    }

    final shouldOfferPersonalization = response != null &&
        switch (response.profileStatus) {
          RecommendationProfileStatus.notStarted ||
          RecommendationProfileStatus.declined ||
          RecommendationProfileStatus.revoked => true,
          _ => false,
        };

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(sectionTitle, style: _MotherHomeScreenState._sectionTitleStyle),
        const SizedBox(height: 5),
        Text(
          sectionSubtitle,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 13,
            color: _MotherHomeScreenState._onSurfaceVariant,
          ),
        ),
        if (shouldOfferPersonalization) ...[
          const SizedBox(height: 4),
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton(
              key: const Key('mother-home-recommendation-personalize'),
              onPressed: () => context.push(
                '/recommendation-profile',
                extra: response.stage,
              ),
              child: const Text('Cá nhân hóa nội dung'),
            ),
          ),
        ],
        if (response != null &&
            (response.profileStatus ==
                    RecommendationProfileStatus.reviewRequired ||
                response.profileStatus ==
                    RecommendationProfileStatus.reconsentRequired)) ...[
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
        if (response != null && response.selectionMode == 'FALLBACK_ONLY')
          const _RecommendationCoverageNotice(
            key: Key('mother-home-recommendation-fallback-only'),
            message: 'Đây là nội dung nền an toàn cho giai đoạn hiện tại.',
          ),
        if (response != null && response.coverageStatus == 'PARTIAL')
          _RecommendationCoverageNotice(
            key: const Key('mother-home-recommendation-partial'),
            message:
                'Hiện có một số nội dung phù hợp. Bạn có thể xem thêm trong thư viện.',
            onBrowse: () => context.push('/content'),
          ),
        if (response != null &&
            response.coverageStatus == 'EMPTY' &&
            _babyRecommendations.isEmpty)
          _RecommendationCoverageNotice(
            key: const Key('mother-home-recommendation-empty-coverage'),
            message:
                'Chưa có bài viết phù hợp; hãy xem toàn bộ thư viện nội dung.',
            onBrowse: () => context.push('/content'),
          ),
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
        const SizedBox(height: 10),
        if (items.isNotEmpty)
          ...items.map(_buildRecommendationCard)
        else
          _RecommendationCoverageNotice(
            key: const Key('mother-home-recommendation-empty-all'),
            message:
                'Chưa có bài viết phù hợp; hãy xem toàn bộ thư viện nội dung.',
            onBrowse: () => context.push('/content'),
          ),
      ],
    );
  }

  List<RecommendationContentItem> _getUnifiedPrioritizedRecommendations() {
    final maternalItems =
        _recommendations?.items ?? const <RecommendationContentItem>[];
    final babyItems = _babyRecommendations;
    final activeBaby = _babies.firstOrNull;

    if (babyItems.isEmpty) return maternalItems;
    if (maternalItems.isEmpty) return babyItems;

    final days = activeBaby != null
        ? DateTime.now().difference(activeBaby.birthDate).inDays
        : 999;
    final isNewborn = days <= 30;

    int calculatePriority(RecommendationContentItem item) {
      if (item.reasonCode == 'BABY_CARE_CONTEXT') {
        final title = item.title.toLowerCase();
        final summary = (item.summary ?? '').toLowerCase();
        // Priority 10: Chăm sóc trẻ sơ sinh & dinh dưỡng bú sữa cấp thiết
        if (isNewborn &&
            (title.contains('bú') ||
                title.contains('sơ sinh') ||
                summary.contains('bú'))) {
          return 10;
        }
        // Priority 20: Tắm bé, vệ sinh, giấc ngủ an toàn
        if (isNewborn &&
            (title.contains('tắm') ||
                title.contains('ngủ') ||
                summary.contains('tắm'))) {
          return 20;
        }
        // Priority 30: Mốc phát triển & vận động của bé
        return 30;
      } else {
        // Maternal items:
        // Priority 15: Hướng dẫn theo tuần thai mục tiêu
        if (item.selectionType == RecommendationSelectionType.targeted ||
            item.rank <= 2) {
          return 15;
        }
        // Priority 25: Hướng dẫn giai đoạn thai kỳ (tam cá nguyệt, dinh dưỡng)
        return 25;
      }
    }

    final combined = <RecommendationContentItem>[...babyItems, ...maternalItems];
    combined.sort((a, b) {
      final pA = calculatePriority(a);
      final pB = calculatePriority(b);
      if (pA != pB) return pA.compareTo(pB);
      return a.rank.compareTo(b.rank);
    });

    return combined;
  }

  /// Render Card bài viết gợi ý đơn lẻ kèm tiêu đề, tóm tắt và thông điệp giải thích lý do gợi ý
  Widget _buildRecommendationCard(RecommendationContentItem item) {
    final isBabyItem = item.reasonCode == 'BABY_CARE_CONTEXT';

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
                  Icon(
                    isBabyItem
                        ? Icons.child_care_rounded
                        : Icons.menu_book_outlined,
                    color: isBabyItem
                        ? const Color(0xFFD97757)
                        : _MotherHomeScreenState._primary,
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
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            color: isBabyItem
                                ? const Color(0xFFD97757)
                                : _MotherHomeScreenState._primary,
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
