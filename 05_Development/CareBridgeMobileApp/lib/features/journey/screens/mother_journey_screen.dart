import 'dart:math';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../../aiTriage/models/triage_continuation.dart';
import '../../aiTriage/services/triage_continuation_restore_coordinator.dart';
import '../../baby/models/baby_model.dart';
import '../../baby/screens/add_baby_screen.dart';
import '../../baby/screens/baby_profile_detail_screen.dart';
import '../../baby/services/baby_profile_selection_storage.dart';
import '../../baby/services/baby_service.dart';
import '../../community/models/content_model.dart';
import '../../community/screens/view_content_screen.dart';
import '../../healthRecords/models/health_metric_model.dart';
import '../../healthRecords/services/health_metric_service.dart';
import '../models/journey_model.dart';
import '../services/journey_service.dart';
import 'pregnancy_outcome_screen.dart';

bool shouldOpenLiveBirthAddBaby({
  required PregnancyOutcome? previousOutcome,
  required PregnancyOutcomeResult result,
}) =>
    result.outcomeType == PregnancyOutcome.liveBirth &&
    previousOutcome != PregnancyOutcome.liveBirth;

/// CB-009 - Mother Journey (UC-23, UC-24, UC-25, UC-26, UC-27, UC-28)
/// Shows the active mother journey from GET /api/v1/journeys/me/dashboard.
class MotherJourneyScreen extends StatefulWidget {
  const MotherJourneyScreen({
    super.key,
    this.loadData = true,
    this.initialBabyProfiles = const [],
    this.initialDashboard,
    this.initialJourneyHistory = const [],
    this.initialTimeline = const [],
    this.journeyService,
    this.babyService,
    this.loadSupportingData = true,
    this.continuationArrival,
  });

  final bool loadData;
  final List<BabyProfile> initialBabyProfiles;
  final JourneyDashboard? initialDashboard;
  final List<JourneyTransition> initialJourneyHistory;
  final List<JourneyTimelineItem> initialTimeline;
  final JourneyService? journeyService;
  final BabyService? babyService;
  final bool loadSupportingData;
  final TriageContinuationArrival? continuationArrival;

  @override
  State<MotherJourneyScreen> createState() => _MotherJourneyScreenState();
}

enum _JourneySection { pregnancy, babyCare }

class _MotherJourneyScreenState extends State<MotherJourneyScreen>
    with WidgetsBindingObserver {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _surfaceContainerLowest = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);

  late final JourneyService _journeyService;
  late final BabyService _babyService;
  final _babySelectionStorage = BabyProfileSelectionStorage();
  final _healthMetricService = HealthMetricService();
  JourneyDashboard? _dashboard;
  List<JourneyTransition> _journeyHistory = [];
  List<JourneyTimelineItem> _journeyTimeline = [];
  List<BabyProfile> _babyProfiles = [];
  String? _selectedBabyProfileId;
  MetricTrend? _bmiTrend;
  _JourneySection _selectedSection = _JourneySection.pregnancy;
  bool _didChooseInitialSection = false;
  bool _loading = true;
  String? _error;
  String? _historyError;
  bool _showContinuationConfirmation = false;
  bool _continuationAcknowledgementInProgress = false;
  bool _continuationAcknowledged = false;
  bool _continuationAcknowledgementFailed = false;
  int _loadGeneration = 0;

  @override
  void initState() {
    super.initState();
    _journeyService = widget.journeyService ?? JourneyService();
    _babyService = widget.babyService ?? BabyService();
    WidgetsBinding.instance.addObserver(this);
    _dashboard = widget.initialDashboard;
    _journeyHistory = widget.initialJourneyHistory;
    _journeyTimeline = widget.initialTimeline;
    _babyProfiles = widget.initialBabyProfiles;
    if (widget.loadData) {
      JourneyService.dashboardRevision.addListener(_onJourneyDashboardChanged);
      _load();
    } else {
      _selectedBabyProfileId = _resolveSelectedBabyProfileId(_babyProfiles);
      _selectedSection = widget.initialDashboard?.isMaternalLifecycle == true
          ? _JourneySection.pregnancy
          : _JourneySection.babyCare;
      _didChooseInitialSection = true;
      _loading = false;
      if (widget.initialDashboard != null &&
          widget.initialTimeline.isNotEmpty) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          _completeContinuationArrivalIfReady(
            dashboard: widget.initialDashboard!,
            timelineLoaded: true,
          );
        });
      }
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    JourneyService.dashboardRevision.removeListener(_onJourneyDashboardChanged);
    super.dispose();
  }

  void _onJourneyDashboardChanged() {
    if (mounted && widget.loadData) {
      _load();
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && widget.loadData) {
      _load();
    }
  }

  Future<void> _load() async {
    final generation = ++_loadGeneration;
    final isInitialLoad = _dashboard == null;
    if (isInitialLoad) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }

    try {
      final dashboard = await _journeyService.getDashboard();
      final babyProfiles = widget.loadSupportingData
          ? await _loadBabyProfiles()
          : _babyProfiles;
      final lastOpenedBabyProfileId = widget.loadSupportingData
          ? await _babySelectionStorage.readLastOpenedBabyProfileId()
          : null;
      final hasMaternalJourney =
          dashboard.hasActiveJourney && dashboard.isMaternalLifecycle;
      final shouldShowBabyCare =
          !hasMaternalJourney &&
          (dashboard.journeyType == 'BABY_CARE' || babyProfiles.isNotEmpty);
      var journeyHistory = _journeyHistory;
      var journeyTimeline = _journeyTimeline;
      var timelineLoaded = false;
      String? historyError;
      if (hasMaternalJourney && dashboard.journeyId != null) {
        try {
          journeyTimeline = await _loadCompleteTimeline(dashboard.journeyId!);
          timelineLoaded = true;
        } catch (_) {
          try {
            journeyHistory = await _journeyService.getHistory(
              dashboard.journeyId!,
            );
            historyError =
                'Dòng thời gian an toàn tạm thời chưa tải được. Lịch sử chuyển giai đoạn bên dưới có thể chưa gồm kết quả kiểm tra an toàn.';
          } catch (_) {
            historyError =
                'Không thể tải dòng thời gian. Dữ liệu đã tải trước đó vẫn được giữ lại.';
          }
        }
      } else {
        journeyHistory = const <JourneyTransition>[];
        journeyTimeline = const <JourneyTimelineItem>[];
      }
      final bmiTrend =
          widget.loadSupportingData &&
              hasMaternalJourney &&
              dashboard.journeyId != null
          ? await _loadBmiTrend(dashboard.journeyId!)
          : _bmiTrend;
      if (!mounted || generation != _loadGeneration) return;
      setState(() {
        _dashboard = dashboard;
        _journeyHistory = journeyHistory;
        _journeyTimeline = journeyTimeline;
        _historyError = historyError;
        _babyProfiles = babyProfiles;
        _selectedBabyProfileId = _resolveSelectedBabyProfileId(
          babyProfiles,
          preferredBabyProfileId: lastOpenedBabyProfileId,
        );
        _bmiTrend = bmiTrend;
        if (!_didChooseInitialSection && shouldShowBabyCare) {
          _selectedSection = _JourneySection.babyCare;
        }
        _didChooseInitialSection = true;
        _loading = false;
      });
      _completeContinuationArrivalIfReady(
        dashboard: dashboard,
        timelineLoaded: timelineLoaded,
      );
    } on ApiException catch (_) {
      if (!mounted || generation != _loadGeneration) return;
      if (!isInitialLoad) return;
      setState(() {
        _dashboard = null;
        _babyProfiles = [];
        _error = 'Không thể tải dữ liệu hành trình.';
        _loading = false;
      });
    } catch (_) {
      if (!mounted || generation != _loadGeneration) return;
      if (!isInitialLoad) return;
      setState(() {
        _dashboard = null;
        _babyProfiles = [];
        _error = 'Lỗi kết nối. Vui lòng kéo để thử lại.';
        _loading = false;
      });
    }
  }

  Future<List<JourneyTimelineItem>> _loadCompleteTimeline(
    String journeyId,
  ) async {
    final items = <JourneyTimelineItem>[];
    var page = 0;
    var totalPages = 1;
    do {
      final result = await _journeyService.getTimeline(
        journeyId,
        page: page,
        size: 100,
      );
      items.addAll(result.items);
      totalPages = result.totalPages;
      page++;
    } while (page < totalPages);
    return List.unmodifiable(items);
  }

  void _completeContinuationArrivalIfReady({
    required JourneyDashboard dashboard,
    required bool timelineLoaded,
  }) {
    final arrival = widget.continuationArrival;
    if (arrival == null ||
        _continuationAcknowledgementInProgress ||
        _continuationAcknowledged ||
        _continuationAcknowledgementFailed) {
      return;
    }
    final decision = arrival.decision;
    final exactOrigin =
        decision.destination == TriageContinuationDestination.motherJourney &&
        dashboard.journeyId != null &&
        dashboard.journeyId == decision.originReferenceId;
    if (!exactOrigin) {
      setState(() {
        _historyError =
            'Không thể xác nhận đúng hành trình đã bắt đầu kiểm tra an toàn. Dữ liệu tiếp tục vẫn được giữ để thử lại.';
      });
      return;
    }
    if (!timelineLoaded || !decision.showRecordedConfirmation) return;

    setState(() => _showContinuationConfirmation = true);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _acknowledgeContinuation();
    });
  }

  Future<void> _acknowledgeContinuation() async {
    final arrival = widget.continuationArrival;
    if (!mounted ||
        arrival == null ||
        _continuationAcknowledgementInProgress ||
        _continuationAcknowledged) {
      return;
    }
    setState(() {
      _continuationAcknowledgementInProgress = true;
      _continuationAcknowledgementFailed = false;
    });
    var acknowledged = false;
    try {
      acknowledged = await arrival.acknowledge();
    } catch (_) {
      acknowledged = false;
    }
    if (!mounted) return;
    setState(() {
      _continuationAcknowledgementInProgress = false;
      _continuationAcknowledged = acknowledged;
      _continuationAcknowledgementFailed = !acknowledged;
    });
  }

  Future<List<BabyProfile>> _loadBabyProfiles() async {
    try {
      return await _babyService.listBabyProfiles();
    } catch (_) {
      return [];
    }
  }

  String? _resolveSelectedBabyProfileId(
    List<BabyProfile> profiles, {
    String? preferredBabyProfileId,
  }) {
    if (profiles.isEmpty) return null;
    if (preferredBabyProfileId != null &&
        profiles.any((profile) => profile.id == preferredBabyProfileId)) {
      return preferredBabyProfileId;
    }
    final currentId = _selectedBabyProfileId;
    if (currentId != null &&
        profiles.any((profile) => profile.id == currentId)) {
      return currentId;
    }
    for (final profile in profiles) {
      if (profile.isActive) return profile.id;
    }
    return profiles.first.id;
  }

  BabyProfile? get _selectedBabyProfile {
    final selectedId = _selectedBabyProfileId;
    if (selectedId != null) {
      for (final profile in _babyProfiles) {
        if (profile.id == selectedId) return profile;
      }
    }
    for (final profile in _babyProfiles) {
      if (profile.isActive) return profile;
    }
    return _babyProfiles.isEmpty ? null : _babyProfiles.first;
  }

  Future<MetricTrend?> _loadBmiTrend(String journeyId) async {
    try {
      return await _healthMetricService.getMetricTrend(
        journeyId: journeyId,
        metricType: 'BMI',
        from: DateTime.now().subtract(const Duration(days: 28)),
        to: DateTime.now(),
      );
    } catch (_) {
      return null;
    }
  }

  Future<void> _openMetricRoute(String metricType) async {
    final journeyId = _dashboard?.journeyId;
    if (journeyId == null || journeyId.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Chưa có hành trình để xem biểu đồ.')),
      );
      return;
    }

    final route = Uri(
      path: '/journeys/$journeyId/metrics/trend',
      queryParameters: {'metricType': metricType},
    ).toString();
    await context.push(route);
  }

  Future<void> _openPregnancySetup() async {
    final dashboard = _dashboard;
    final journeyId = dashboard?.journeyId;
    final isExistingJourneyTransition =
        dashboard?.hasActiveJourney == true &&
        (dashboard?.isPrePregnancy == true ||
            dashboard?.isPostpartum == true) &&
        journeyId != null;
    final route = isExistingJourneyTransition
        ? Uri(
            path: '/journey-setup',
            queryParameters: {
              'mode': 'edit',
              'journeyId': journeyId,
              'transition': dashboard?.isPostpartum == true
                  ? 'postpartum'
                  : 'pre-pregnancy',
            },
          ).toString()
        : '/journey-setup';
    await context.push(route);
    if (mounted) await _load();
  }

  Future<void> _openAddBabyProfile() async {
    final result = await context.push('/babies/add?entry=list');
    if (!mounted) return;
    if (result == true) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã tạo hồ sơ bé thành công.')),
      );
    }
    await _load();
  }

  Future<void> _openLifecycleContent() async {
    await Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) =>
            const ViewContentScreen(mode: ContentBrowseMode.lifecycle),
      ),
    );
  }

  Future<void> _openBabyProfilePicker() async {
    final result = await showModalBottomSheet<_BabyPickerResult>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _BabyProfilePickerSheet(
        profiles: _babyProfiles,
        selectedBabyId: _selectedBabyProfile?.id,
      ),
    );

    if (!mounted || result == null) return;
    if (result.action == _BabyPickerAction.add) {
      await _openAddBabyProfile();
      return;
    }

    final selected = result.profile;
    if (selected == null) return;
    final previousId = _selectedBabyProfileId;
    setState(() => _selectedBabyProfileId = selected.id);
    if (!widget.loadData) return;
    await _babySelectionStorage.saveLastOpenedBabyProfileId(selected.id);
    if (!selected.isActive) {
      try {
        await _babyService.switchActiveBabyProfile(selected.id);
      } catch (_) {
        if (!mounted) return;
        setState(() => _selectedBabyProfileId = previousId);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể đổi hồ sơ bé đang theo dõi.'),
          ),
        );
      }
    }
    if (mounted) await _load();
  }

  String _formatDate(DateTime? date) {
    if (date == null) return 'Chưa có';
    return '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}';
  }

  String _formatDateTime(DateTime date) {
    final day = date.day.toString().padLeft(2, '0');
    final month = date.month.toString().padLeft(2, '0');
    final hour = date.hour.toString().padLeft(2, '0');
    final minute = date.minute.toString().padLeft(2, '0');
    return '$hour:$minute - $day/$month/${date.year}';
  }

  String _daysUntilDueLabel(JourneyDashboard dashboard) {
    final days = dashboard.calculatedDaysUntilDue;
    if (days == null) return 'Chưa có ngày dự sinh';
    if (days == 0) return 'Dự sinh hôm nay';
    if (days > 0) return 'Còn lại $days ngày';
    return 'Quá ngày dự sinh ${days.abs()} ngày';
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      color: _primaryContainer,
      onRefresh: widget.loadData ? _load : () async {},
      child: CustomScrollView(
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(24, 20, 24, 0),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                if (_loading)
                  const Padding(
                    padding: EdgeInsets.only(top: 80),
                    child: Center(
                      child: CircularProgressIndicator(
                        key: Key('mother-journey-initial-loading'),
                        color: _primaryContainer,
                      ),
                    ),
                  )
                else ...[
                  const Padding(
                    padding: EdgeInsets.only(bottom: 16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Hành trình',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 26,
                            fontWeight: FontWeight.w700,
                            color: _onSurface,
                          ),
                        ),
                        SizedBox(height: 4),
                        Text(
                          'Theo dõi sức khỏe của mẹ và bé theo từng giai đoạn.',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 13,
                            color: _onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                  _buildSectionTabs(),
                  if (_showContinuationConfirmation) ...[
                    const SizedBox(height: 16),
                    _buildContinuationConfirmation(),
                  ],
                  const SizedBox(height: 20),
                  if (_selectedSection == _JourneySection.pregnancy)
                    ..._buildPregnancySection()
                  else
                    ..._buildBabyCareSection(),
                  const SizedBox(height: 24),
                ],
              ]),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildContinuationConfirmation() {
    const confirmation =
        'Kết quả kiểm tra an toàn đã được ghi vào dòng thời gian.';
    return Semantics(
      key: const Key('mother-triage-recorded-confirmation'),
      container: true,
      liveRegion: true,
      label: _continuationAcknowledgementFailed
          ? '$confirmation Chưa thể xác nhận đã nhận. Bạn có thể thử lại ngay tại đây.'
          : confirmation,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: const Color(0xFFF2EAE4),
          borderRadius: BorderRadius.circular(20),
          border: const Border(
            left: BorderSide(color: Color(0xFFC98C7B), width: 4),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(
              children: [
                Icon(Icons.verified_outlined, color: Color(0xFFC98C7B)),
                SizedBox(width: 12),
                Expanded(
                  child: Text(
                    confirmation,
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: Color(0xFF5A463F),
                    ),
                  ),
                ),
              ],
            ),
            if (_continuationAcknowledgementFailed) ...[
              const SizedBox(height: 12),
              const Text(
                'Chưa thể xác nhận đã nhận. Kết quả vẫn được giữ an toàn để thử lại.',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  height: 1.4,
                  color: Color(0xFF5A463F),
                ),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  key: const Key('mother-triage-acknowledgement-retry'),
                  onPressed: _continuationAcknowledgementInProgress
                      ? null
                      : _acknowledgeContinuation,
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('Thử lại xác nhận'),
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size.fromHeight(48),
                    foregroundColor: const Color(0xFF845143),
                    shape: const StadiumBorder(),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  JourneyDashboard? get _pregnancyDashboard {
    final dashboard = _dashboard;
    if (dashboard == null) return null;
    if (!dashboard.hasActiveJourney || !dashboard.isMaternalLifecycle) {
      return null;
    }
    return dashboard;
  }

  List<Widget> _buildPregnancySection() {
    final dashboard = _pregnancyDashboard;
    if (dashboard == null) {
      return [
        _buildEmptyStateCard(
          icon: _error == null ? Icons.pregnant_woman_rounded : Icons.wifi_off,
          title: _error ?? 'Chưa có hành trình mang thai',
          description:
              'Thiết lập ngày dự sinh hoặc ngày chu kỳ để CareBridge hiển thị dữ liệu thai kỳ của mẹ.',
          actionLabel: 'Thêm hành trình',
          onPressed: _openPregnancySetup,
        ),
      ];
    }

    if (dashboard.isPrePregnancy) {
      return [
        _buildPrePregnancyCard(dashboard),
        const SizedBox(height: 16),
        ..._buildMaternalHealthBlock(),
        if (_historyError != null) ...[
          const SizedBox(height: 16),
          _buildHistoryErrorCard(),
        ],
        if (_journeyTimeline.isNotEmpty || _journeyHistory.isNotEmpty) ...[
          const SizedBox(height: 16),
          _buildJourneyTimelineCard(),
        ],
        const SizedBox(height: 16),
        _buildLifecycleContentEntry(),
      ];
    }

    if (dashboard.isPostpartum) {
      return [
        _buildPostpartumCard(dashboard),
        const SizedBox(height: 16),
        ..._buildMaternalHealthBlock(),
        if (_historyError != null) ...[
          const SizedBox(height: 16),
          _buildHistoryErrorCard(),
        ],
        if (_journeyTimeline.isNotEmpty || _journeyHistory.isNotEmpty) ...[
          const SizedBox(height: 16),
          _buildJourneyTimelineCard(),
        ],
        const SizedBox(height: 16),
        _buildPostpartumRecoveryActions(dashboard),
        const SizedBox(height: 16),
        _buildLifecycleContentEntry(),
      ];
    }

    return [
      _buildHeroCard(dashboard),
      const SizedBox(height: 16),
      _buildDueDateCard(dashboard),
      const SizedBox(height: 16),
      _buildPregnancyOutcomeEntry(dashboard),
      const SizedBox(height: 16),
      _buildSetupSourceCard(dashboard),
      ..._buildMaternalHealthBlock(),
      if (_historyError != null) ...[
        const SizedBox(height: 24),
        _buildHistoryErrorCard(),
      ],
      if (_journeyTimeline.isNotEmpty || _journeyHistory.isNotEmpty) ...[
        const SizedBox(height: 24),
        _buildJourneyTimelineCard(),
      ],
      const SizedBox(height: 16),
      _buildLifecycleContentEntry(),
    ];
  }

  List<Widget> _buildMaternalHealthBlock() {
    return [
      const SizedBox(height: 16),
      _buildMetricButtons(),
      const SizedBox(height: 24),
      _buildBentoSummary(),
    ];
  }

  Widget _buildLifecycleContentEntry() {
    return Semantics(
      key: const Key('mother-lifecycle-content-entry'),
      button: true,
      label: 'Mở nội dung đã kiểm duyệt theo giai đoạn hiện tại',
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(32),
        child: InkWell(
          onTap: _openLifecycleContent,
          borderRadius: BorderRadius.circular(32),
          child: Container(
            constraints: const BoxConstraints(minHeight: 72),
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(32),
              border: Border.all(color: _outlineVariant),
              boxShadow: const [
                BoxShadow(
                  color: Color(0x0F5A463F),
                  blurRadius: 24,
                  offset: Offset(0, 8),
                ),
              ],
            ),
            child: const Row(
              children: [
                SizedBox(
                  width: 48,
                  height: 48,
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      color: Color(0x26C98C7B),
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      Icons.verified_rounded,
                      color: _primaryContainer,
                    ),
                  ),
                ),
                SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Nội dung đã kiểm duyệt',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 18,
                          fontWeight: FontWeight.w800,
                          color: _onSurface,
                        ),
                      ),
                      SizedBox(height: 4),
                      Text(
                        'Xem hướng dẫn phù hợp với giai đoạn hiện tại.',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 16,
                          color: _onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                SizedBox(width: 8),
                Icon(Icons.arrow_forward_rounded, color: _primary),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildPregnancyOutcomeEntry(JourneyDashboard dashboard) {
    return Semantics(
      key: const Key('pregnancy-outcome-entry'),
      button: true,
      label: 'Cập nhật tình trạng thai kỳ',
      child: InkWell(
        borderRadius: BorderRadius.circular(24),
        onTap: dashboard.journeyId == null || dashboard.version == null
            ? null
            : () async {
                final result = await Navigator.of(context).push(
                  MaterialPageRoute<PregnancyOutcomeResult>(
                    builder: (_) => PregnancyOutcomeScreen(
                      journeyId: dashboard.journeyId!,
                      journeyVersion: dashboard.version!,
                      currentOutcome: dashboard.pregnancyOutcome,
                    ),
                  ),
                );
                if (result != null && mounted) {
                  if (widget.loadData) await _load();
                  if (shouldOpenLiveBirthAddBaby(
                        previousOutcome: dashboard.pregnancyOutcome,
                        result: result,
                      ) &&
                      mounted) {
                    await context.push(
                      '/babies/add',
                      extra: const AddBabyRouteArgs(
                        entryPoint: AddBabyEntryPoint.liveBirthTransition,
                      ),
                    );
                  }
                }
              },
        child: Container(
          constraints: const BoxConstraints(minHeight: 64),
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            color: const Color(0xFFF2EAE4),
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: const Color(0xFFE8DDD6)),
          ),
          child: const Row(
            children: [
              SizedBox(
                width: 48,
                height: 48,
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: Colors.white,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(Icons.sync_alt_rounded, color: Color(0xFFC98C7B)),
                ),
              ),
              SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Cập nhật tình trạng thai kỳ',
                      style: TextStyle(
                        color: Color(0xFF5A463F),
                        fontSize: 16,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    SizedBox(height: 4),
                    Text(
                      'Điều chỉnh hỗ trợ theo tình trạng hiện tại của bạn',
                      style: TextStyle(color: Color(0xFF9C857C), fontSize: 14),
                    ),
                  ],
                ),
              ),
              Icon(Icons.chevron_right_rounded, color: Color(0xFF9C857C)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildPostpartumCard(JourneyDashboard dashboard) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: _cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 52,
            height: 52,
            decoration: const BoxDecoration(
              color: _surfaceContainerHigh,
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.self_improvement_rounded,
              color: _primary,
              size: 28,
            ),
          ),
          const SizedBox(height: 18),
          const Text(
            'Hành trình hậu sản',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 24,
              fontWeight: FontWeight.w800,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            dashboard.startDate == null
                ? 'CareBridge đang đồng hành cùng mẹ trong giai đoạn phục hồi hậu sản.'
                : 'Giai đoạn hậu sản bắt đầu từ ${_formatDate(dashboard.startDate)}.',
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              height: 1.5,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 20),
          FilledButton.icon(
            key: const Key('postpartum-transition-action'),
            onPressed: _openPregnancySetup,
            icon: const Icon(Icons.arrow_forward_rounded),
            label: const Text('Bắt đầu thai kỳ mới'),
            style: FilledButton.styleFrom(
              minimumSize: const Size.fromHeight(52),
              backgroundColor: _primaryContainer,
              foregroundColor: Colors.white,
              shape: const StadiumBorder(),
              textStyle: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPostpartumRecoveryActions(JourneyDashboard dashboard) {
    return Column(
      children: [
        _postpartumAction(
          key: const Key('postpartum-open-logs'),
          icon: Icons.menu_book_outlined,
          title: 'Nhật ký hồi phục',
          description: 'Ghi nhận sức khỏe và xem lại các ngày trước.',
          onTap: dashboard.journeyId == null
              ? null
              : () => context.push(
                  '/postpartum-logs?journeyId=${Uri.encodeComponent(dashboard.journeyId!)}',
                ),
        ),
        const SizedBox(height: 12),
        _postpartumAction(
          key: const Key('postpartum-safety-help'),
          icon: Icons.health_and_safety_outlined,
          title: 'Dấu hiệu cần hỗ trợ khẩn cấp',
          description: 'Xem lựa chọn hỗ trợ dành riêng cho giai đoạn hậu sản.',
          onTap: () => context.push('/postpartum-safety-help'),
          urgent: true,
        ),
      ],
    );
  }

  Widget _postpartumAction({
    required Key key,
    required IconData icon,
    required String title,
    required String description,
    required VoidCallback? onTap,
    bool urgent = false,
  }) {
    return Semantics(
      button: true,
      label: '$title. $description',
      child: Card(
        key: key,
        margin: EdgeInsets.zero,
        elevation: 0,
        color: urgent ? const Color(0xFFFFF0ED) : Colors.white,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(22),
          side: BorderSide(
            color: urgent ? const Color(0xFFE9A69A) : const Color(0xFFE8DDD6),
          ),
        ),
        child: ListTile(
          minTileHeight: 72,
          onTap: onTap,
          leading: Icon(
            icon,
            color: urgent ? const Color(0xFF93000A) : _primary,
          ),
          title: Text(
            title,
            style: const TextStyle(fontWeight: FontWeight.w800),
          ),
          subtitle: Text(description),
          trailing: const Icon(Icons.chevron_right_rounded),
        ),
      ),
    );
  }

  Widget _buildHistoryErrorCard() {
    return Semantics(
      key: const Key('journey-timeline-warning'),
      liveRegion: true,
      label: _historyError,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(18),
        decoration: BoxDecoration(
          color: const Color(0xFFF2EAE4),
          borderRadius: BorderRadius.circular(20),
          border: const Border(
            left: BorderSide(color: _primaryContainer, width: 4),
          ),
        ),
        child: Row(
          children: [
            const Icon(Icons.history_toggle_off_rounded, color: _primary),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                _historyError!,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  height: 1.45,
                  color: _onSurfaceVariant,
                ),
              ),
            ),
            const SizedBox(width: 12),
            TextButton(
              key: const Key('journey-history-retry'),
              onPressed: _load,
              style: TextButton.styleFrom(
                foregroundColor: Colors.white,
                backgroundColor: _primaryContainer,
                shape: const StadiumBorder(),
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 12,
                ),
              ),
              child: const Text(
                'Thử lại',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPrePregnancyCard(JourneyDashboard dashboard) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: _cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 52,
            height: 52,
            decoration: const BoxDecoration(
              color: _surfaceContainerHigh,
              shape: BoxShape.circle,
            ),
            child: const Icon(Icons.spa_rounded, color: _primary, size: 28),
          ),
          const SizedBox(height: 18),
          const Text(
            'Chuẩn bị mang thai',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 24,
              fontWeight: FontWeight.w800,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            dashboard.startDate == null
                ? 'CareBridge đang đồng hành cùng kế hoạch mang thai của mẹ.'
                : 'Bắt đầu từ ${_formatDate(dashboard.startDate)}. Khi đã xác nhận mang thai, hãy cập nhật hành trình hiện tại.',
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              height: 1.5,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 20),
          FilledButton.icon(
            key: const Key('pre-pregnancy-transition-action'),
            onPressed: _openPregnancySetup,
            icon: const Icon(Icons.arrow_forward_rounded),
            label: const Text('Bắt đầu hành trình thai kỳ'),
            style: FilledButton.styleFrom(
              minimumSize: const Size.fromHeight(52),
              backgroundColor: _primaryContainer,
              foregroundColor: Colors.white,
              shape: const StadiumBorder(),
              textStyle: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildJourneyTimelineCard() {
    if (_journeyTimeline.isEmpty) return _buildJourneyHistoryCard();
    final items = [..._journeyTimeline]
      ..sort((a, b) {
        final occurred = b.occurredAt.compareTo(a.occurredAt);
        if (occurred != 0) return occurred;
        final recorded = b.recordedAt.compareTo(a.recordedAt);
        if (recorded != 0) return recorded;
        return b.itemId.compareTo(a.itemId);
      });
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: _cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.history_rounded, color: _primary),
              SizedBox(width: 10),
              Expanded(
                child: Text(
                  'Dòng thời gian hành trình',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          for (var index = 0; index < items.length; index++) ...[
            _buildTimelineItem(items[index]),
            if (index < items.length - 1)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 12),
                child: Divider(color: _outlineVariant, height: 1),
              ),
          ],
        ],
      ),
    );
  }

  Widget _buildTimelineItem(JourneyTimelineItem item) {
    if (!item.isSafetyOutcome) {
      return Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SizedBox(
            width: 36,
            height: 36,
            child: DecoratedBox(
              decoration: BoxDecoration(
                color: _surfaceContainerHigh,
                shape: BoxShape.circle,
              ),
              child: Icon(Icons.flag_rounded, size: 19, color: _primary),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  _eventLabel(item.eventType ?? 'UPDATED'),
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  _formatDateTime(item.occurredAt.toLocal()),
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ],
      );
    }
    final risk = item.riskLevel ?? 'UNKNOWN';
    final stage = item.stage == null ? '' : _stageLabel(item.stage!);
    final semanticLabel =
        'Kết quả kiểm tra an toàn đã được ghi vào dòng thời gian. '
        'Mức $risk${stage.isEmpty ? '' : ', giai đoạn $stage'}.';
    return Semantics(
      key: Key('journey-timeline-safety-${item.itemId}'),
      liveRegion: true,
      label: semanticLabel,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: const Color(0xFFF2EAE4),
          borderRadius: BorderRadius.circular(24),
          border: const Border(
            left: BorderSide(color: _primaryContainer, width: 4),
          ),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(
              width: 40,
              height: 40,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: Color(0xFFFFF8F6),
                  shape: BoxShape.circle,
                ),
                child: Icon(Icons.health_and_safety_outlined, color: _primary),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Đã ghi kết quả kiểm tra an toàn',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      color: _onSurface,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Mức $risk${stage.isEmpty ? '' : ' • $stage'}',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: _onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    _formatDateTime(item.occurredAt.toLocal()),
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      color: _onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildJourneyHistoryCard() {
    final transitions = [..._journeyHistory]
      ..sort((a, b) => b.recordedAt.compareTo(a.recordedAt));
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: _cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.history_rounded, color: _primary),
              SizedBox(width: 10),
              Expanded(
                child: Text(
                  'Lịch sử hành trình',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          for (var index = 0; index < transitions.length; index++) ...[
            _buildJourneyHistoryItem(transitions[index]),
            if (index < transitions.length - 1)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 12),
                child: Divider(color: _outlineVariant, height: 1),
              ),
          ],
        ],
      ),
    );
  }

  Widget _buildJourneyHistoryItem(JourneyTransition transition) {
    final stageChange =
        transition.fromStage != null && transition.toStage != null
        ? '${_stageLabel(transition.fromStage!)} → ${_stageLabel(transition.toStage!)}'
        : null;
    final details = <String>[
      ?stageChange,
      if (transition.reason != null && transition.reason!.trim().isNotEmpty)
        _reasonLabel(transition.reason!.trim()),
      [
        if (transition.source != null) _sourceLabel(transition.source!),
        if (transition.confidence != null)
          _confidenceLabel(transition.confidence!),
      ].where((value) => value.isNotEmpty).join(' • '),
    ].where((value) => value.isNotEmpty).toList(growable: false);

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 36,
          height: 36,
          decoration: const BoxDecoration(
            color: _surfaceContainerHigh,
            shape: BoxShape.circle,
          ),
          child: Icon(
            _historyIcon(transition.eventType),
            size: 19,
            color: _primary,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                _eventLabel(transition.eventType),
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w700,
                  color: _onSurface,
                ),
              ),
              if (details.isNotEmpty) ...[
                const SizedBox(height: 4),
                Text(
                  details.join('\n'),
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    height: 1.45,
                    color: _onSurfaceVariant,
                  ),
                ),
              ],
              const SizedBox(height: 4),
              Text(
                _formatDateTime(transition.effectiveAt.toLocal()),
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 11,
                  color: _onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  String _eventLabel(String eventType) {
    switch (eventType) {
      case 'CREATED':
        return 'Khởi tạo hành trình';
      case 'STAGE_CHANGED':
        return 'Chuyển giai đoạn';
      case 'DATES_CHANGED':
        return 'Cập nhật mốc thời gian';
      case 'STATUS_CHANGED':
        return 'Cập nhật trạng thái';
      case 'MIGRATED':
        return 'Đồng bộ dữ liệu cũ';
      default:
        return 'Cập nhật hành trình';
    }
  }

  IconData _historyIcon(String eventType) {
    switch (eventType) {
      case 'STAGE_CHANGED':
        return Icons.swap_horiz_rounded;
      case 'DATES_CHANGED':
        return Icons.event_available_rounded;
      case 'STATUS_CHANGED':
        return Icons.toggle_on_rounded;
      default:
        return Icons.flag_rounded;
    }
  }

  String _stageLabel(String stage) {
    switch (stage) {
      case 'PRE_PREGNANCY':
        return 'Chuẩn bị mang thai';
      case 'PREGNANCY':
        return 'Mang thai';
      case 'POSTPARTUM':
        return 'Hậu sản';
      case 'BABY_CARE':
        return 'Nuôi con';
      default:
        return 'Hành trình';
    }
  }

  String _sourceLabel(String source) {
    switch (source) {
      case 'CLINICIAN_CONFIRMED':
        return 'Bác sĩ xác nhận';
      case 'SELF_REPORTED':
        return 'Mẹ cung cấp';
      case 'SYSTEM_DERIVED':
        return 'Hệ thống tính';
      default:
        return '';
    }
  }

  String _confidenceLabel(String confidence) {
    switch (confidence) {
      case 'CONFIRMED':
        return 'Đã xác nhận';
      case 'ESTIMATED':
        return 'Ước tính';
      default:
        return '';
    }
  }

  String _reasonLabel(String reason) {
    switch (reason) {
      case 'INITIAL_SETUP':
      case 'MF01_FIXTURE_CREATED':
        return 'Thiết lập ban đầu';
      case 'PREGNANCY_CONFIRMED':
        return 'Đã xác nhận mang thai';
      case 'DATE_CORRECTION':
        return 'Điều chỉnh mốc thời gian';
      case 'MF01_FIXTURE_CLINICIAN_CONFIRMATION':
        return 'Bác sĩ xác nhận ngày dự sinh';
      default:
        return reason;
    }
  }

  List<Widget> _buildBabyCareSection() {
    if (_babyProfiles.isEmpty) {
      return [
        _buildEmptyStateCard(
          icon: Icons.child_care_rounded,
          title: 'Chưa có hồ sơ bé',
          description:
              'Tạo hồ sơ bé để CareBridge hiển thị thông tin nuôi con, lịch tiêm và nhật ký chăm sóc phù hợp.',
          actionLabel: 'Thêm hồ sơ bé',
          onPressed: _openAddBabyProfile,
        ),
      ];
    }

    final selectedProfile = _selectedBabyProfile ?? _babyProfiles.first;

    return [
      BabyProfileDetailScreen(
        key: ValueKey(selectedProfile.id),
        babyId: selectedProfile.id,
        embedded: true,
        loadData: widget.loadData,
        initialProfile: selectedProfile,
        onSwitchBaby: _openBabyProfilePicker,
        onAddBaby: _openAddBabyProfile,
        onProfileChanged: widget.loadData ? _load : null,
      ),
    ];
  }

  Widget _buildSectionTabs() {
    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: const Color(0xFFF2EAE4),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: _outlineVariant),
      ),
      child: Row(
        children: [
          Expanded(
            child: _buildSectionTab(
              label: 'Mẹ',
              icon: Icons.pregnant_woman_rounded,
              section: _JourneySection.pregnancy,
            ),
          ),
          Expanded(
            child: _buildSectionTab(
              label: 'Bé',
              icon: Icons.child_care_rounded,
              section: _JourneySection.babyCare,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSectionTab({
    required String label,
    required IconData icon,
    required _JourneySection section,
  }) {
    final selected = _selectedSection == section;
    return Tooltip(
      message: label,
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: () => setState(() => _selectedSection = section),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
          decoration: BoxDecoration(
            color: selected ? _surfaceContainerLowest : Colors.transparent,
            borderRadius: BorderRadius.circular(14),
            boxShadow: selected
                ? [
                    BoxShadow(
                      color: _primary.withAlpha(16),
                      blurRadius: 12,
                      offset: const Offset(0, 4),
                    ),
                  ]
                : null,
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                icon,
                size: 18,
                color: selected ? _primary : _onSurfaceVariant,
              ),
              const SizedBox(width: 8),
              Flexible(
                child: Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: selected ? FontWeight.w700 : FontWeight.w600,
                    color: selected ? _primary : _onSurfaceVariant,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildEmptyStateCard({
    required IconData icon,
    required String title,
    required String description,
    required String actionLabel,
    required VoidCallback onPressed,
  }) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: _cardDecoration(),
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
            child: Icon(icon, color: _primary),
          ),
          const SizedBox(height: 16),
          Text(
            title,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            description,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
              height: 1.45,
            ),
          ),
          const SizedBox(height: 20),
          FilledButton.icon(
            onPressed: onPressed,
            icon: const Icon(Icons.add_rounded),
            label: Text(actionLabel),
            style: FilledButton.styleFrom(
              backgroundColor: _primary,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
              textStyle: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHeroCard(JourneyDashboard dashboard) {
    final week = dashboard.displayPregnancyWeek;
    final weekTitle = week != null ? 'Tuần $week' : dashboard.phaseLabel;
    final statusLabel = dashboard.displayTrimester != null
        ? 'Tam cá nguyệt ${dashboard.displayTrimester}'
        : dashboard.phaseLabel;

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFFFFF1EC), Color(0xFFFFE2D9)],
        ),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: Colors.white.withAlpha(128)),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF845143).withAlpha(20),
            blurRadius: 24,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: Colors.white.withAlpha(178),
                    borderRadius: BorderRadius.circular(99),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(Icons.favorite, size: 14, color: _primary),
                      const SizedBox(width: 4),
                      Text(
                        statusLabel,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 11,
                          color: _primary,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                Text(
                  weekTitle,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 36,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                    letterSpacing: -0.5,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  week != null
                      ? 'Bé đang lớn bằng ${dashboard.fruitName}, ${dashboard.fruitSizeNote}.'
                      : 'CareBridge đang theo dõi hành trình từ dữ liệu mẹ đã thiết lập.',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _onSurfaceVariant,
                    height: 1.5,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 24),
          _CircularProgressWidget(
            progress: dashboard.pregnancyProgress,
            week: week,
          ),
        ],
      ),
    );
  }

  Widget _buildDueDateCard(JourneyDashboard dashboard) {
    final dueDate = dashboard.estimatedDueDate;
    final monthLabel = dueDate != null
        ? 'THG ${dueDate.month.toString().padLeft(2, '0')}'
        : '--';
    final dayLabel = dueDate != null ? '${dueDate.day}' : '--';

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: _cardDecoration(),
      child: Row(
        children: [
          Container(
            width: 64,
            height: 64,
            decoration: BoxDecoration(
              color: _surfaceContainerHigh,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  monthLabel,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                    color: _primary,
                  ),
                ),
                Text(
                  dayLabel,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 22,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Ngày dự sinh',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  _daysUntilDueLabel(dashboard),
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            onPressed: dashboard.journeyId == null
                ? null
                : () => context
                      .push(
                        '/journey-setup?mode=edit&journeyId=${Uri.encodeComponent(dashboard.journeyId!)}',
                      )
                      .then((_) => _load()),
            icon: const Icon(Icons.edit_outlined, color: _onSurfaceVariant),
            tooltip: 'Cập nhật ngày dự sinh',
          ),
        ],
      ),
    );
  }

  Widget _buildSetupSourceCard(JourneyDashboard dashboard) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFFF2EAE4),
        borderRadius: BorderRadius.circular(24),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Dữ liệu đã thiết lập',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 14),
          _InfoRow(
            icon: Icons.today_rounded,
            label: 'Ngày bắt đầu',
            value: _formatDate(dashboard.startDate),
          ),
          const SizedBox(height: 10),
          _InfoRow(
            icon: Icons.calendar_month_rounded,
            label: 'Ngày đầu chu kỳ',
            value: _formatDate(dashboard.lastMenstrualDate),
          ),
          const SizedBox(height: 10),
          _InfoRow(
            icon: Icons.event_available_rounded,
            label: 'Ngày dự sinh',
            value: _formatDate(dashboard.estimatedDueDate),
          ),
        ],
      ),
    );
  }

  Widget _buildMetricButtons() {
    final metrics = [
      (Icons.monitor_heart_outlined, 'Chỉ số sức khỏe', 'BMI'),
      (Icons.history_edu, 'Hồ sơ sức khỏe', '/health-records'),
      (Icons.health_and_safety_outlined, 'Giám sát an toàn', '/safety'),
    ];

    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: metrics.asMap().entries.map((entry) {
          final i = entry.key;
          final metric = entry.value;
          return Expanded(
            child: Padding(
              padding: EdgeInsets.only(right: i < metrics.length - 1 ? 12 : 0),
              child: Semantics(
                button: true,
                label: metric.$2,
                excludeSemantics: true,
                child: GestureDetector(
                  onTap: () async {
                    if (metric.$3 == 'BMI') {
                      await _openMetricRoute(metric.$3);
                      return;
                    }
                    context.push(metric.$3);
                  },
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      vertical: 16,
                      horizontal: 6,
                    ),
                    decoration: BoxDecoration(
                      color: _surfaceContainerLowest,
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(color: _outlineVariant),
                      boxShadow: const [
                        BoxShadow(
                          color: Color(0x0F5A463F),
                          blurRadius: 12,
                          offset: Offset(0, 2),
                        ),
                      ],
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Container(
                          width: 44,
                          height: 44,
                          decoration: const BoxDecoration(
                            color: _surfaceContainerHigh,
                            shape: BoxShape.circle,
                          ),
                          child: Icon(metric.$1, color: _primary, size: 22),
                        ),
                        const SizedBox(height: 10),
                        SizedBox(
                          height: 36,
                          child: Center(
                            child: Text(
                              metric.$2,
                              textAlign: TextAlign.center,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                                color: _onSurfaceVariant,
                                height: 1.25,
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
        }).toList(),
      ),
    );
  }

  Widget _buildBentoSummary() {
    final bmiPoint = _bmiTrend?.dataPoints.isNotEmpty == true
        ? _bmiTrend!.dataPoints.last
        : null;
    final weight = switch (bmiPoint?.context['weightKg']) {
      final num value => value.toDouble(),
      final String value => double.tryParse(value),
      _ => null,
    };
    final height = switch (bmiPoint?.context['heightCm']) {
      final num value => value.toDouble(),
      final String value => double.tryParse(value),
      _ => null,
    };
    final weightValue = weight == null
        ? '—'
        : weight % 1 == 0
        ? weight.toStringAsFixed(0)
        : weight.toStringAsFixed(1);
    final heightValue = height == null
        ? '—'
        : height % 1 == 0
        ? height.toStringAsFixed(0)
        : height.toStringAsFixed(1);

    return Row(
      children: [
        Expanded(
          child: _buildBentoCard(
            icon: Icons.monitor_weight_outlined,
            label: 'Cân nặng',
            value: weightValue,
            unit: 'kg',
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildBentoCard(
            icon: Icons.height_rounded,
            label: 'Chiều cao',
            value: heightValue,
            unit: 'cm',
          ),
        ),
      ],
    );
  }

  Widget _buildBentoCard({
    required IconData icon,
    required String label,
    required String value,
    required String unit,
    double? trend,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFF6F1EC),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.white, width: 2),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(12),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Icon(icon, color: _primaryContainer, size: 22),
              if (trend != null)
                Row(
                  children: [
                    Icon(
                      trend >= 0
                          ? Icons.trending_up_rounded
                          : Icons.trending_down_rounded,
                      color: _primary,
                      size: 16,
                    ),
                    const SizedBox(width: 4),
                    Text(
                      '${trend.abs().toStringAsFixed(1)}%',
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
          const SizedBox(height: 8),
          Text(
            label,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            '$value $unit',
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
        ],
      ),
    );
  }

  BoxDecoration _cardDecoration() {
    return BoxDecoration(
      color: _surfaceContainerLowest,
      borderRadius: BorderRadius.circular(24),
      boxShadow: [
        BoxShadow(
          color: const Color(0xFF5A463F).withAlpha(15),
          blurRadius: 20,
          offset: const Offset(0, 4),
        ),
      ],
    );
  }
}

enum _BabyPickerAction { select, add }

class _BabyPickerResult {
  const _BabyPickerResult.select(this.profile)
    : action = _BabyPickerAction.select;
  const _BabyPickerResult.add()
    : action = _BabyPickerAction.add,
      profile = null;

  final _BabyPickerAction action;
  final BabyProfile? profile;
}

class _BabyProfilePickerSheet extends StatelessWidget {
  const _BabyProfilePickerSheet({
    required this.profiles,
    required this.selectedBabyId,
  });

  final List<BabyProfile> profiles;
  final String? selectedBabyId;

  static const _primary = Color(0xFF845143);
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _onSurface = Color(0xFF271812);
  static const _outlineVariant = Color(0xFFD6C2BD);

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Container(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.of(context).size.height * 0.82,
        ),
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 48,
              height: 5,
              margin: const EdgeInsets.only(bottom: 18),
              decoration: BoxDecoration(
                color: _outlineVariant,
                borderRadius: BorderRadius.circular(99),
              ),
            ),
            Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: const BoxDecoration(
                    color: _surfaceContainerHigh,
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.child_care_rounded, color: _primary),
                ),
                const SizedBox(width: 12),
                const Expanded(
                  child: Text(
                    'Chọn hồ sơ bé',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 20,
                      fontWeight: FontWeight.w700,
                      color: _onSurface,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 18),
            Flexible(
              child: ListView.separated(
                shrinkWrap: true,
                itemCount: profiles.length,
                separatorBuilder: (_, _) => const SizedBox(height: 10),
                itemBuilder: (context, index) {
                  final profile = profiles[index];
                  final selected = profile.id == selectedBabyId;
                  return _BabyPickerTile(
                    profile: profile,
                    selected: selected,
                    onTap: () => Navigator.of(
                      context,
                    ).pop(_BabyPickerResult.select(profile)),
                  );
                },
              ),
            ),
            const SizedBox(height: 14),
            OutlinedButton.icon(
              onPressed: () =>
                  Navigator.of(context).pop(const _BabyPickerResult.add()),
              icon: const Icon(Icons.add_rounded),
              label: const Text('Thêm hồ sơ bé'),
              style: OutlinedButton.styleFrom(
                minimumSize: const Size.fromHeight(50),
                foregroundColor: _primary,
                side: const BorderSide(color: _outlineVariant),
                textStyle: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _BabyPickerTile extends StatelessWidget {
  const _BabyPickerTile({
    required this.profile,
    required this.selected,
    required this.onTap,
  });

  final BabyProfile profile;
  final bool selected;
  final VoidCallback onTap;

  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _surfaceContainerLowest = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);

  @override
  Widget build(BuildContext context) {
    final genderLabel = profile.gender.displayLabel;
    return InkWell(
      borderRadius: BorderRadius.circular(18),
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: selected ? _surfaceContainerHigh : _surfaceContainerLowest,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(
            color: selected ? _primaryContainer : _outlineVariant,
            width: selected ? 2 : 1,
          ),
        ),
        child: Row(
          children: [
            Container(
              width: 46,
              height: 46,
              decoration: BoxDecoration(
                color: selected ? Colors.white : _surfaceContainerHigh,
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.child_care, color: _primary),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    profile.nickname,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      color: _onSurface,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    [
                      profile.ageLabel,
                      if (genderLabel.isNotEmpty) genderLabel,
                      if (profile.isActive) 'Đang theo dõi',
                    ].join(' • '),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      color: _onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            Icon(
              selected
                  ? Icons.check_circle_rounded
                  : Icons.radio_button_unchecked_rounded,
              color: selected ? _primaryContainer : _onSurfaceVariant,
            ),
          ],
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width: 36,
          height: 36,
          decoration: const BoxDecoration(
            color: Color(0xFFFFF8F6),
            shape: BoxShape.circle,
          ),
          child: Icon(icon, size: 18, color: const Color(0xFF845143)),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            label,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 13,
              color: Color(0xFF524440),
            ),
          ),
        ),
        Text(
          value,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 13,
            fontWeight: FontWeight.w700,
            color: Color(0xFF271812),
          ),
        ),
      ],
    );
  }
}

class _CircularProgressWidget extends StatelessWidget {
  const _CircularProgressWidget({required this.progress, required this.week});

  final double progress;
  final int? week;

  @override
  Widget build(BuildContext context) {
    final pct = (progress * 100).round();
    return SizedBox(
      width: 110,
      height: 110,
      child: CustomPaint(
        painter: _CircleProgressPainter(progress: progress),
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                '$pct%',
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w700,
                  color: Color(0xFF271812),
                ),
              ),
              Text(
                week != null ? 'Thai kỳ' : 'Thiết lập',
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 11,
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

class _CircleProgressPainter extends CustomPainter {
  const _CircleProgressPainter({required this.progress});

  final double progress;

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = (size.width / 2) - 8;
    const strokeWidth = 8.0;

    final bgPaint = Paint()
      ..color = const Color(0xFFF2EAE4)
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth;
    canvas.drawCircle(center, radius, bgPaint);

    final progressPaint = Paint()
      ..color = const Color(0xFFC98C7B)
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round;

    final rect = Rect.fromCircle(center: center, radius: radius);
    canvas.drawArc(
      rect,
      -pi / 2,
      2 * pi * progress.clamp(0.0, 1.0),
      false,
      progressPaint,
    );
  }

  @override
  bool shouldRepaint(_CircleProgressPainter old) => old.progress != progress;
}
