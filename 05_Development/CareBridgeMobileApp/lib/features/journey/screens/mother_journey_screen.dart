import 'dart:math';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../../baby/models/baby_model.dart';
import '../../baby/screens/baby_profile_detail_screen.dart';
import '../../baby/services/baby_profile_selection_storage.dart';
import '../../baby/services/baby_service.dart';
import '../../healthRecords/models/health_metric_model.dart';
import '../../healthRecords/services/health_metric_service.dart';
import '../../reminder/models/reminder_model.dart';
import '../../reminder/services/reminder_service.dart';
import '../models/journey_model.dart';
import '../services/journey_service.dart';

/// CB-009 - Mother Journey (UC-23, UC-24, UC-25, UC-26, UC-27, UC-28)
/// Shows the active mother journey from GET /api/v1/journeys/me/dashboard.
class MotherJourneyScreen extends StatefulWidget {
  const MotherJourneyScreen({
    super.key,
    this.loadData = true,
    this.initialBabyProfiles = const [],
  });

  final bool loadData;
  final List<BabyProfile> initialBabyProfiles;

  @override
  State<MotherJourneyScreen> createState() => _MotherJourneyScreenState();
}

enum _JourneySection { pregnancy, babyCare }

class _MotherJourneyScreenState extends State<MotherJourneyScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _surfaceContainerLowest = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);

  final _journeyService = JourneyService();
  final _babyService = BabyService();
  final _babySelectionStorage = BabyProfileSelectionStorage();
  final _reminderService = ReminderService.instance;
  final _healthMetricService = HealthMetricService();
  JourneyDashboard? _dashboard;
  List<BabyProfile> _babyProfiles = [];
  String? _selectedBabyProfileId;
  List<Reminder> _reminders = [];
  MetricTrend? _weightTrend;
  MetricTrend? _heartRateTrend;
  _JourneySection _selectedSection = _JourneySection.pregnancy;
  bool _didChooseInitialSection = false;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    if (widget.loadData) {
      _load();
    } else {
      _babyProfiles = widget.initialBabyProfiles;
      _selectedBabyProfileId = _resolveSelectedBabyProfileId(_babyProfiles);
      _selectedSection = _JourneySection.babyCare;
      _didChooseInitialSection = true;
      _loading = false;
    }
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final dashboard = await _journeyService.getDashboard();
      final babyProfiles = await _loadBabyProfiles();
      final lastOpenedBabyProfileId = await _babySelectionStorage
          .readLastOpenedBabyProfileId();
      final reminders = await _loadReminders();
      final hasPregnancyJourney =
          dashboard.hasActiveJourney && dashboard.isPregnancy;
      final shouldShowBabyCare =
          !hasPregnancyJourney &&
          (dashboard.journeyType == 'BABY_CARE' || babyProfiles.isNotEmpty);
      final weightTrend = hasPregnancyJourney && dashboard.journeyId != null
          ? await _loadWeightTrend(dashboard.journeyId!)
          : null;
      final heartRateTrend = hasPregnancyJourney && dashboard.journeyId != null
          ? await _loadHeartRateTrend(dashboard.journeyId!)
          : null;
      if (!mounted) return;
      setState(() {
        _dashboard = dashboard;
        _babyProfiles = babyProfiles;
        _selectedBabyProfileId = _resolveSelectedBabyProfileId(
          babyProfiles,
          preferredBabyProfileId: lastOpenedBabyProfileId,
        );
        _reminders = reminders;
        _weightTrend = weightTrend;
        _heartRateTrend = heartRateTrend;
        if (!_didChooseInitialSection && shouldShowBabyCare) {
          _selectedSection = _JourneySection.babyCare;
        }
        _didChooseInitialSection = true;
        _loading = false;
      });
    } on ApiException catch (_) {
      if (!mounted) return;
      setState(() {
        _dashboard = null;
        _babyProfiles = [];
        _error = 'Không thể tải dữ liệu hành trình.';
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _dashboard = null;
        _babyProfiles = [];
        _error = 'Lỗi kết nối. Vui lòng kéo để thử lại.';
        _loading = false;
      });
    }
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

  Future<List<Reminder>> _loadReminders() async {
    try {
      return await _reminderService.listTodayReminders();
    } catch (_) {
      return [];
    }
  }

  Future<MetricTrend?> _loadWeightTrend(String journeyId) async {
    try {
      return await _healthMetricService.getMetricTrend(
        journeyId: journeyId,
        metricType: 'WEIGHT',
        from: DateTime.now().subtract(const Duration(days: 28)),
        to: DateTime.now(),
      );
    } catch (_) {
      return null;
    }
  }

  Future<MetricTrend?> _loadHeartRateTrend(String journeyId) async {
    try {
      return await _healthMetricService.getMetricTrend(
        journeyId: journeyId,
        metricType: 'HEART_RATE',
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
    await context.push('/journey-setup');
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

  Reminder? _nearestReminder(ReminderType type) {
    final pending =
        _reminders
            .where(
              (r) =>
                  r.reminderType == type && r.status == ReminderStatus.pending,
            )
            .toList()
          ..sort((a, b) => a.scheduledAt.compareTo(b.scheduledAt));
    return pending.firstOrNull;
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
          SliverToBoxAdapter(child: _buildHeader()),
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 24),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                if (_loading)
                  const Padding(
                    padding: EdgeInsets.only(top: 80),
                    child: Center(
                      child: CircularProgressIndicator(
                        color: _primaryContainer,
                      ),
                    ),
                  )
                else ...[
                  _buildSectionTabs(),
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

  JourneyDashboard? get _pregnancyDashboard {
    final dashboard = _dashboard;
    if (dashboard == null) return null;
    if (!dashboard.hasActiveJourney || !dashboard.isPregnancy) return null;
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

    return [
      _buildHeroCard(dashboard),
      const SizedBox(height: 16),
      _buildDueDateCard(dashboard),
      const SizedBox(height: 16),
      _buildNextAppointmentCard(),
      const SizedBox(height: 16),
      _buildVaccinationCard(),
      const SizedBox(height: 16),
      _buildSetupSourceCard(dashboard),
      const SizedBox(height: 24),
      _buildMetricButtons(),
      const SizedBox(height: 24),
      _buildBentoSummary(),
    ];
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

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 16, 8, 16),
      child: Row(
        children: [
          const Expanded(
            child: Text(
              'Hành trình của Mẹ',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 24,
                fontWeight: FontWeight.w700,
                color: _onSurface,
              ),
            ),
          ),
        ],
      ),
    );
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
              label: 'Mang thai',
              icon: Icons.pregnant_woman_rounded,
              section: _JourneySection.pregnancy,
            ),
          ),
          Expanded(
            child: _buildSectionTab(
              label: 'Nuôi con',
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

  Widget _buildNextAppointmentCard() {
    final appointment = _nearestReminder(ReminderType.appointment);
    final hasData = appointment != null;

    return GestureDetector(
      onTap: hasData
          ? () => context.push('/reminders/detail/${appointment.id}')
          : null,
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: const Color(0xFFF2EAE4),
          borderRadius: BorderRadius.circular(24),
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
                        appointment?.title ?? 'Chưa có lịch khám sắp tới',
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
                    color: _surfaceContainerLowest,
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
                        : 'Chưa có dữ liệu',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      color: _onSurfaceVariant,
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

  Widget _buildVaccinationCard() {
    final vaccination = _nearestReminder(ReminderType.vaccination);
    final hasData = vaccination != null;

    return GestureDetector(
      onTap: hasData
          ? () => context.push('/reminders/detail/${vaccination.id}')
          : null,
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: const Color(0xFFF2EAE4),
          borderRadius: BorderRadius.circular(24),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: const BoxDecoration(
                color: _surfaceContainerLowest,
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.vaccines, color: _primary, size: 20),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Thông tin tiêm phòng',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      color: _onSurface,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    vaccination != null
                        ? '${vaccination.title} (${_formatDateTime(vaccination.scheduledAt)})'
                        : 'Chưa có lịch tiêm phòng',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      color: _onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            Icon(
              hasData ? Icons.chevron_right : Icons.info_outline_rounded,
              color: _onSurfaceVariant,
            ),
          ],
        ),
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
      (Icons.monitor_heart_outlined, 'Chỉ số sức khỏe', 'WEIGHT'),
      (Icons.history_edu, 'Hồ sơ sức khỏe', '/health-records'),
      (Icons.psychology_alt_outlined, 'Kiểm tra triệu chứng', '/triage/intake'),
      (Icons.health_and_safety_outlined, 'Giám sát an toàn', '/safety'),
    ];

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: metrics.asMap().entries.map((entry) {
          final i = entry.key;
          final metric = entry.value;
          return Padding(
            padding: EdgeInsets.only(right: i < metrics.length - 1 ? 12 : 0),
            child: GestureDetector(
              onTap: () async {
                if (metric.$3 == 'WEIGHT') {
                  await _openMetricRoute(metric.$3);
                  return;
                }
                context.push(metric.$3);
              },
              child: Container(
                width: 104,
                padding: const EdgeInsets.symmetric(
                  vertical: 16,
                  horizontal: 8,
                ),
                decoration: BoxDecoration(
                  color: _surfaceContainerLowest,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: _outlineVariant),
                ),
                child: Column(
                  children: [
                    Container(
                      width: 40,
                      height: 40,
                      decoration: const BoxDecoration(
                        color: _surfaceContainerHigh,
                        shape: BoxShape.circle,
                      ),
                      child: Icon(metric.$1, color: _primary, size: 20),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      metric.$2,
                      textAlign: TextAlign.center,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 11,
                        fontWeight: FontWeight.w600,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildBentoSummary() {
    final weightPoint = _weightTrend?.dataPoints.isNotEmpty == true
        ? _weightTrend!.dataPoints.last
        : null;
    final hrPoint = _heartRateTrend?.dataPoints.isNotEmpty == true
        ? _heartRateTrend!.dataPoints.last
        : null;

    final weightValue = weightPoint?.valueDisplay ?? '—';
    final weightTrendPct = _weightTrend?.trend;

    final hrValue = hrPoint?.valueDisplay ?? '—';
    final hrTrendPct = _heartRateTrend?.trend;

    return Row(
      children: [
        Expanded(
          child: _buildBentoCard(
            icon: Icons.monitor_weight_outlined,
            label: 'Cân nặng',
            value: weightValue,
            unit: 'kg',
            trend: weightTrendPct,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildBentoCard(
            icon: Icons.show_chart_rounded,
            label: 'Nhịp tim',
            value: hrValue,
            unit: 'bpm',
            trend: hrTrendPct,
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
