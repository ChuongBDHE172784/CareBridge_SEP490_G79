import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../models/baby_model.dart';
import '../models/baby_daily_log_model.dart';
import '../models/milestone_model.dart';
import '../services/baby_profile_selection_storage.dart';
import '../services/baby_log_service.dart';
import '../services/baby_service.dart';
import '../../../core/network/api_client.dart';
import '../../aiTriage/models/triage_entry_context.dart';
import '../../aiTriage/models/triage_continuation.dart';
import '../../aiTriage/services/triage_continuation_restore_coordinator.dart';
import '../../aiTriage/widgets/triage_safety_entry_action.dart';
import '../../healthRecords/models/vaccination_model.dart';
import '../../healthRecords/models/growth_measurement_model.dart';
import '../../healthRecords/services/growth_measurement_service.dart';
import '../../healthRecords/services/vaccination_service.dart';

/// CB-011 — Baby Profile Detail (UC-34, UC-35, UC-36, UC-37, UC-38, UC-192, UC-194–197)
/// Shows full baby profile: avatar, age, weight/height, 24h summary, tabs for
/// growth/milestones/vaccination, and trend chart. Calls GET /api/v1/babies/{babyId}.
class BabyProfileDetailScreen extends StatefulWidget {
  final String babyId;
  final bool embedded;
  final VoidCallback? onSwitchBaby;
  final VoidCallback? onAddBaby;
  final VoidCallback? onProfileChanged;
  final bool loadData;
  final BabyProfile? initialProfile;
  final List<Milestone> initialMilestones;
  final List<VaccinationRecord> initialVaccinations;
  final BabyLogSummaryResponse? initialSummary;
  final List<GrowthMeasurement> initialGrowthMeasurements;
  final Future<BabyProfile> Function(String babyId)? profileLoader;
  final Future<BabyLogSummaryResponse> Function(String babyId)? summaryLoader;
  final Future<List<GrowthMeasurement>> Function(String babyId)? growthLoader;
  final bool loadCareCollectionsData;
  final TriageContinuationArrival? continuationArrival;

  const BabyProfileDetailScreen({
    super.key,
    required this.babyId,
    this.embedded = false,
    this.onSwitchBaby,
    this.onAddBaby,
    this.onProfileChanged,
    this.loadData = true,
    this.initialProfile,
    this.initialMilestones = const [],
    this.initialVaccinations = const [],
    this.initialSummary,
    this.initialGrowthMeasurements = const [],
    this.profileLoader,
    this.summaryLoader,
    this.growthLoader,
    this.loadCareCollectionsData = true,
    this.continuationArrival,
  });

  @override
  State<BabyProfileDetailScreen> createState() =>
      _BabyProfileDetailScreenState();
}

enum _Tab { growth, milestones, vaccination }

class _BabyProfileDetailScreenState extends State<BabyProfileDetailScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _secondary = Color(0xFF6E5A52);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);

  final _service = BabyService();
  final _babyLogService = BabyLogService();
  final _vaccinationService = VaccinationService();
  final _growthMeasurementService = GrowthMeasurementService();
  final _selectionStorage = BabyProfileSelectionStorage();
  BabyProfile? _profile;
  List<Milestone> _milestones = const [];
  List<VaccinationRecord> _vaccinations = const [];
  BabyLogSummaryResponse? _summary;
  List<GrowthMeasurement> _growthMeasurements = const [];
  bool _summaryLoading = false;
  bool _growthLoading = false;
  String? _summaryError;
  String? _growthError;
  bool _careCollectionsLoading = false;
  String? _careCollectionsError;
  int _loadGeneration = 0;
  bool _loading = true;
  String? _error;
  bool _showContinuationConfirmation = false;
  bool _continuationAcknowledgementInProgress = false;
  bool _continuationAcknowledged = false;
  bool _continuationAcknowledgementFailed = false;
  _Tab _activeTab = _Tab.growth;

  double get _horizontalPadding => widget.embedded ? 0 : 24;

  TriageStageIntent? get _triageStage {
    final birthDate = _profile?.birthDate;
    if (birthDate == null) return null;
    final now = DateTime.now();
    if (birthDate.isAfter(now)) return null;
    var ageMonths =
        (now.year - birthDate.year) * 12 + now.month - birthDate.month;
    if (now.day < birthDate.day) ageMonths--;
    if (ageMonths < 12) return TriageStageIntent.infant;
    if (ageMonths <= 24) return TriageStageIntent.toddler;
    return null;
  }

  @override
  void initState() {
    super.initState();
    _profile = widget.initialProfile;
    _milestones = widget.initialMilestones;
    _vaccinations = widget.initialVaccinations;
    _summary = widget.initialSummary;
    _growthMeasurements = _sortGrowthMeasurements(
      widget.initialGrowthMeasurements,
    );
    _loading = widget.loadData && widget.initialProfile == null;
    if (!widget.loadData && widget.initialProfile == null) {
      _error = 'Không có dữ liệu hồ sơ.';
    }
    if (widget.loadData) {
      _selectionStorage.saveLastOpenedBabyProfileId(widget.babyId);
      _loadProfile();
    } else if (_profile != null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _completeContinuationArrivalIfReady(_profile!);
      });
    }
  }

  @override
  void didUpdateWidget(covariant BabyProfileDetailScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.babyId != widget.babyId) {
      _activeTab = _Tab.growth;
      _profile = widget.initialProfile;
      _milestones = widget.initialMilestones;
      _vaccinations = widget.initialVaccinations;
      _summary = widget.initialSummary;
      _growthMeasurements = _sortGrowthMeasurements(
        widget.initialGrowthMeasurements,
      );
      _summaryError = null;
      _growthError = null;
      _careCollectionsLoading = false;
      _careCollectionsError = null;
      _summaryLoading = false;
      _growthLoading = false;
      _error = !widget.loadData && widget.initialProfile == null
          ? 'Không có dữ liệu hồ sơ.'
          : null;
      _loading = widget.loadData && widget.initialProfile == null;
      if (widget.loadData) {
        _selectionStorage.saveLastOpenedBabyProfileId(widget.babyId);
        _loadProfile();
      }
    } else {
      if (oldWidget.initialProfile != widget.initialProfile) {
        _profile = widget.initialProfile;
        if (!widget.loadData) {
          _loading = false;
          _error = widget.initialProfile == null
              ? 'Không có dữ liệu hồ sơ.'
              : null;
        }
      }
      if (oldWidget.initialSummary != widget.initialSummary) {
        _summary = widget.initialSummary;
      }
      if (oldWidget.initialGrowthMeasurements !=
          widget.initialGrowthMeasurements) {
        _growthMeasurements = _sortGrowthMeasurements(
          widget.initialGrowthMeasurements,
        );
      }
      if (oldWidget.initialMilestones != widget.initialMilestones) {
        _milestones = widget.initialMilestones;
      }
      if (oldWidget.initialVaccinations != widget.initialVaccinations) {
        _vaccinations = widget.initialVaccinations;
      }
      if (oldWidget.loadData && !widget.loadData) {
        _loadGeneration++;
        _loading = false;
        _summaryLoading = false;
        _growthLoading = false;
        _careCollectionsLoading = false;
        _error = widget.initialProfile == null
            ? 'Không có dữ liệu hồ sơ.'
            : null;
      } else if (!oldWidget.loadData && widget.loadData) {
        _selectionStorage.saveLastOpenedBabyProfileId(widget.babyId);
        _loadProfile();
      }
    }
  }

  Future<void> _loadProfile() async {
    final requestedBabyId = widget.babyId;
    final generation = ++_loadGeneration;
    setState(() {
      _loading = true;
      _error = null;
      _summary = null;
      _growthMeasurements = const [];
      _summaryLoading = true;
      _growthLoading = true;
      _summaryError = null;
      _growthError = null;
    });
    try {
      final p =
          await (widget.profileLoader?.call(requestedBabyId) ??
              _service.getBabyProfile(requestedBabyId));
      if (mounted &&
          requestedBabyId == widget.babyId &&
          generation == _loadGeneration) {
        setState(() {
          _profile = p;
          _loading = false;
        });
        _completeContinuationArrivalIfReady(p);
        await Future.wait([
          _loadSummary(requestedBabyId, generation),
          _loadGrowthHistory(requestedBabyId, generation),
          _loadCareCollections(requestedBabyId, generation),
        ]);
      }
    } on ApiException catch (e) {
      if (mounted &&
          requestedBabyId == widget.babyId &&
          generation == _loadGeneration) {
        setState(() {
          _error = e.statusCode == 403
              ? 'Bạn không có quyền xem hồ sơ này.'
              : 'Không thể tải hồ sơ. Vui lòng thử lại.';
          _loading = false;
          _summaryLoading = false;
          _growthLoading = false;
        });
      }
    } catch (_) {
      if (mounted &&
          requestedBabyId == widget.babyId &&
          generation == _loadGeneration) {
        setState(() {
          _error = 'Lỗi kết nối.';
          _loading = false;
          _summaryLoading = false;
          _growthLoading = false;
        });
      }
    }
  }

  void _completeContinuationArrivalIfReady(BabyProfile profile) {
    final arrival = widget.continuationArrival;
    if (!mounted ||
        arrival == null ||
        _continuationAcknowledgementInProgress ||
        _continuationAcknowledged ||
        _continuationAcknowledgementFailed) {
      return;
    }
    final decision = arrival.decision;
    final exactOrigin =
        decision.destination == TriageContinuationDestination.babyProfile &&
        decision.originReferenceId == widget.babyId &&
        profile.id == widget.babyId;
    if (!exactOrigin || !decision.showRecordedConfirmation) return;

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

  Future<void> _loadSummary(String babyId, int generation) async {
    try {
      final summary =
          await (widget.summaryLoader?.call(babyId) ??
              _babyLogService.getLogSummary(babyId, period: '24h'));
      if (summary.babyId != babyId) {
        throw StateError('Summary does not belong to the selected baby');
      }
      if (!mounted ||
          babyId != widget.babyId ||
          generation != _loadGeneration) {
        return;
      }
      setState(() {
        _summary = summary;
        _summaryLoading = false;
      });
    } catch (_) {
      if (!mounted ||
          babyId != widget.babyId ||
          generation != _loadGeneration) {
        return;
      }
      setState(() {
        _summary = null;
        _summaryLoading = false;
        _summaryError = 'Không thể tải tổng kết 24 giờ.';
      });
    }
  }

  Future<void> _loadGrowthHistory(String babyId, int generation) async {
    try {
      final measurements =
          await (widget.growthLoader?.call(babyId) ??
              _growthMeasurementService.getGrowthHistoryForTrend(babyId));
      if (!mounted ||
          babyId != widget.babyId ||
          generation != _loadGeneration) {
        return;
      }
      setState(() {
        _growthMeasurements = _sortGrowthMeasurements(measurements);
        _growthLoading = false;
      });
    } catch (_) {
      if (!mounted ||
          babyId != widget.babyId ||
          generation != _loadGeneration) {
        return;
      }
      setState(() {
        _growthMeasurements = const [];
        _growthLoading = false;
        _growthError = 'Không thể tải lịch sử tăng trưởng.';
      });
    }
  }

  List<GrowthMeasurement> _sortGrowthMeasurements(
    Iterable<GrowthMeasurement> measurements,
  ) {
    return [...measurements]
      ..sort((a, b) => a.measuredAt.compareTo(b.measuredAt));
  }

  Future<void> _retrySummary() async {
    final generation = _loadGeneration;
    setState(() {
      _summaryLoading = true;
      _summaryError = null;
    });
    await _loadSummary(widget.babyId, generation);
  }

  Future<void> _retryGrowth() async {
    final generation = _loadGeneration;
    setState(() {
      _growthLoading = true;
      _growthError = null;
    });
    await _loadGrowthHistory(widget.babyId, generation);
  }

  Future<void> _loadCareCollections(String babyId, int generation) async {
    if (!widget.loadData || !widget.loadCareCollectionsData) return;
    setState(() {
      _careCollectionsLoading = true;
      _careCollectionsError = null;
    });

    List<Milestone>? milestones;
    List<VaccinationRecord>? vaccinations;
    var failed = false;
    try {
      milestones = await _babyLogService.getMilestones(babyId);
    } catch (_) {
      failed = true;
    }
    try {
      vaccinations = await _vaccinationService.listVaccinationRecords(babyId);
    } catch (_) {
      failed = true;
    }

    if (!mounted || babyId != widget.babyId || generation != _loadGeneration) {
      return;
    }
    setState(() {
      if (milestones != null) _milestones = milestones;
      if (vaccinations != null) _vaccinations = vaccinations;
      _careCollectionsLoading = false;
      _careCollectionsError = failed
          ? 'Một phần dữ liệu chăm sóc chưa thể tải.'
          : null;
    });
  }

  Future<void> _refreshAfterReturn() async {
    if (widget.loadData) await _loadProfile();
    widget.onProfileChanged?.call();
  }

  Future<void> _openEditProfile() async {
    await context.push('/babies/${widget.babyId}/edit');
    if (mounted) {
      await _refreshAfterReturn();
    }
  }

  Future<void> _openLogSummary() async {
    await context.push('/babies/${widget.babyId}/log-summary');
    if (mounted) await _refreshAfterReturn();
  }

  Future<void> _openAddMilestone() async {
    await context.push('/babies/${widget.babyId}/milestones/add');
    if (mounted) {
      await _refreshAfterReturn();
    }
  }

  Future<void> _openGrowth() async {
    await context.push('/babies/${widget.babyId}/growth');
    if (mounted) {
      await _refreshAfterReturn();
    }
  }

  Future<void> _openVaccination() async {
    await context.push('/babies/${widget.babyId}/vaccinations/add');
    if (mounted) {
      await _refreshAfterReturn();
    }
  }

  Future<void> _openMilestoneDetail(Milestone milestone) async {
    await context.push(
      '/babies/${widget.babyId}/milestones/${milestone.id}',
      extra: milestone,
    );
    if (mounted) await _refreshAfterReturn();
  }

  Future<void> _openVaccinationDetail(VaccinationRecord record) async {
    await context.push(
      '/babies/${widget.babyId}/vaccinations/${record.vaccinationId}',
      extra: record,
    );
    if (mounted) await _refreshAfterReturn();
  }

  @override
  Widget build(BuildContext context) {
    if (widget.embedded) {
      if (_loading) {
        return const Padding(
          padding: EdgeInsets.symmetric(vertical: 56),
          child: Center(
            child: CircularProgressIndicator(color: _primaryContainer),
          ),
        );
      }
      if (_error != null) return _buildErrorState();
      return _buildEmbeddedContent();
    }

    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: _loading
            ? const Center(
                child: CircularProgressIndicator(color: _primaryContainer),
              )
            : _error != null
            ? _buildErrorState()
            : _buildContent(),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _openLogSummary,
        backgroundColor: _primary,
        foregroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        child: const Icon(Icons.add, size: 28),
      ),
    );
  }

  Widget _buildErrorState() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, size: 48, color: Color(0xFFBA1A1A)),
          const SizedBox(height: 12),
          Text(
            _error!,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 16),
          TextButton(
            onPressed: _loadProfile,
            child: const Text(
              'Thử lại',
              style: TextStyle(fontFamily: 'Lexend', color: _primary),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildContent() {
    final p = _profile!;
    return CustomScrollView(
      slivers: [
        SliverToBoxAdapter(child: _buildAppBar(p)),
        if (_showContinuationConfirmation)
          SliverToBoxAdapter(child: _buildContinuationConfirmation()),
        SliverToBoxAdapter(child: _buildIdentityHeader(p)),
        SliverToBoxAdapter(child: _buildTriageSafetyEntry()),
        SliverToBoxAdapter(child: _buildSummary24h()),
        SliverToBoxAdapter(child: _buildQuickActions()),
        SliverToBoxAdapter(child: _buildTabBar()),
        SliverToBoxAdapter(child: _buildTabContent()),
        const SliverToBoxAdapter(child: SizedBox(height: 100)),
      ],
    );
  }

  Widget _buildEmbeddedContent() {
    final p = _profile!;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _buildEmbeddedToolbar(p),
        if (_showContinuationConfirmation) ...[
          const SizedBox(height: 14),
          _buildContinuationConfirmation(),
        ],
        const SizedBox(height: 14),
        _buildIdentityHeader(p),
        _buildTriageSafetyEntry(),
        _buildSummary24h(),
        _buildQuickActions(),
        _buildTabBar(),
        _buildTabContent(),
        const SizedBox(height: 12),
      ],
    );
  }

  Widget _buildContinuationConfirmation() {
    const confirmation =
        'Kết quả kiểm tra an toàn đã được ghi vào dòng thời gian.';
    return Semantics(
      key: const Key('baby-triage-recorded-confirmation'),
      container: true,
      liveRegion: true,
      label: _continuationAcknowledgementFailed
          ? '$confirmation Chưa thể xác nhận đã nhận. Bạn có thể thử lại ngay tại đây.'
          : confirmation,
      child: Container(
        margin: EdgeInsets.fromLTRB(
          _horizontalPadding,
          12,
          _horizontalPadding,
          4,
        ),
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
                  key: const Key('baby-triage-acknowledgement-retry'),
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

  Widget _buildEmbeddedToolbar(BabyProfile p) {
    return Row(
      children: [
        Expanded(
          child: Semantics(
            key: ValueKey('active-baby-id-${p.id}'),
            container: true,
            child: Text(
              p.nickname,
              key: const Key('active-baby-name'),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w700,
                color: _primary,
              ),
            ),
          ),
        ),
        if (widget.onAddBaby != null)
          Tooltip(
            message: 'Thêm hồ sơ bé',
            child: IconButton(
              onPressed: widget.onAddBaby,
              icon: const Icon(Icons.add_rounded),
              color: _onSurfaceVariant,
            ),
          ),
        if (widget.onSwitchBaby != null)
          Tooltip(
            message: 'Đổi hồ sơ bé',
            child: IconButton.filled(
              key: const Key('baby-switcher'),
              onPressed: widget.onSwitchBaby,
              icon: const Icon(Icons.swap_horiz_rounded),
              style: IconButton.styleFrom(
                backgroundColor: _primary,
                foregroundColor: Colors.white,
              ),
            ),
          ),
      ],
    );
  }

  Widget _buildTriageSafetyEntry() {
    final stage = _triageStage;
    final profile = _profile;
    if (stage == null || profile == null) {
      return const SizedBox.shrink();
    }
    return Padding(
      padding: EdgeInsets.only(
        left: _horizontalPadding,
        right: _horizontalPadding,
        bottom: 24,
      ),
      child: TriageSafetyEntryAction(
        entryContext: TriageEntryContext.locked(
          stage: stage,
          origin: TriageOriginIntent.babyProfile,
          journeyId: null,
          originReferenceId: profile.id,
        ),
      ),
    );
  }

  Widget _buildAppBar(BabyProfile p) {
    return SizedBox(
      height: 72,
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.arrow_back),
            color: _onSurface,
          ),
          Expanded(
            child: Text(
              p.nickname,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: _primary,
              ),
            ),
          ),
          IconButton(
            onPressed: () {
              _openEditProfile();
            },
            icon: const Icon(Icons.edit_outlined),
            color: _onSurfaceVariant,
          ),
        ],
      ),
    );
  }

  Widget _buildIdentityHeader(BabyProfile p) {
    return Padding(
      padding: EdgeInsets.symmetric(horizontal: _horizontalPadding),
      child: Column(
        children: [
          Container(
            width: 128,
            height: 128,
            decoration: BoxDecoration(
              color: _surfaceContainer,
              shape: BoxShape.circle,
              border: Border.all(color: _canvas, width: 4),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF5A463F).withAlpha(15),
                  blurRadius: 20,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: const Icon(Icons.child_care, size: 56, color: _primary),
          ),
          const SizedBox(height: 16),
          Text(
            p.ageLabel,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _secondary,
            ),
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (p.birthWeightKg != null) ...[
                _StatChip(
                  icon: Icons.monitor_weight,
                  label: '${p.birthWeightKg} kg',
                ),
                const SizedBox(width: 8),
              ],
              if (p.birthLengthCm != null)
                _StatChip(icon: Icons.height, label: '${p.birthLengthCm} cm'),
            ],
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  Widget _buildSummary24h() {
    return Padding(
      padding: EdgeInsets.symmetric(horizontal: _horizontalPadding),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.schedule, size: 20, color: _primary),
              SizedBox(width: 8),
              Text(
                'Tổng kết 24h qua',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          if (_summaryLoading)
            const _SummaryLoadingRow()
          else if (_summaryError != null)
            _InlineDataError(
              key: const Key('baby-summary-error'),
              message: _summaryError!,
              onRetry: _retrySummary,
            )
          else if (_summary == null)
            const _EmptySummary()
          else
            Row(
              key: const Key('baby-summary-real-data'),
              children: [
                Expanded(
                  child: _SummaryCard(
                    key: const Key('baby-summary-feeding'),
                    icon: Icons.water_drop_outlined,
                    value: '${_summary?.feeding?.count ?? 0}',
                    label: 'Cữ bú',
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _SummaryCard(
                    key: const Key('baby-summary-sleep'),
                    icon: Icons.bed_outlined,
                    value: formatSleepDuration(_summary?.sleep),
                    label: 'Giấc ngủ',
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _SummaryCard(
                    key: const Key('baby-summary-diaper'),
                    icon: Icons.cleaning_services_outlined,
                    value: '${_summary?.diaper?.count ?? 0}',
                    label: 'Thay tã',
                  ),
                ),
              ],
            ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  Widget _buildQuickActions() {
    return Padding(
      padding: EdgeInsets.fromLTRB(
        _horizontalPadding,
        0,
        _horizontalPadding,
        20,
      ),
      child: Row(
        children: [
          Expanded(
            child: OutlinedButton.icon(
              key: const Key('baby-care-journal'),
              onPressed: _openLogSummary,
              icon: const Icon(Icons.list_alt_outlined, size: 18),
              label: const Text('Log'),
              style: OutlinedButton.styleFrom(
                foregroundColor: _primary,
                side: const BorderSide(color: _outlineVariant),
              ),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: OutlinedButton.icon(
              key: const Key('baby-care-milestone-add'),
              onPressed: () {
                _openAddMilestone();
              },
              icon: const Icon(Icons.flag_outlined, size: 18),
              label: const Text('Milestone'),
              style: OutlinedButton.styleFrom(
                foregroundColor: _primary,
                side: const BorderSide(color: _outlineVariant),
              ),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: OutlinedButton.icon(
              onPressed: () {
                _openEditProfile();
              },
              icon: const Icon(Icons.edit_outlined, size: 18),
              label: const Text('Edit'),
              style: OutlinedButton.styleFrom(
                foregroundColor: _primary,
                side: const BorderSide(color: _outlineVariant),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTabBar() {
    return Container(
      color: _canvas.withAlpha(230),
      padding: EdgeInsets.symmetric(
        horizontal: _horizontalPadding,
        vertical: 4,
      ),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          children: [
            _TabChip(
              label: 'Phát triển',
              selected: _activeTab == _Tab.growth,
              onTap: () => setState(() => _activeTab = _Tab.growth),
            ),
            const SizedBox(width: 8),
            _TabChip(
              label: 'Cột mốc',
              selected: _activeTab == _Tab.milestones,
              onTap: () => setState(() => _activeTab = _Tab.milestones),
            ),
            const SizedBox(width: 8),
            _TabChip(
              label: 'Tiêm chủng',
              selected: _activeTab == _Tab.vaccination,
              onTap: () => setState(() => _activeTab = _Tab.vaccination),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTabContent() {
    return Padding(
      padding: EdgeInsets.fromLTRB(
        _horizontalPadding,
        16,
        _horizontalPadding,
        0,
      ),
      child: switch (_activeTab) {
        _Tab.growth => _buildGrowthTab(),
        _Tab.milestones => _buildMilestoneTab(),
        _Tab.vaccination => _buildVaccinationTab(),
      },
    );
  }

  Widget _buildVaccinationTab() {
    return Container(
      key: const Key('baby-care-vaccinations'),
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
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
          const Text(
            'Lịch tiêm chủng',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Xem hồ sơ tiêm chủng của bé và thêm mũi tiêm đã thực hiện.',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 16),
          _buildCollectionStatus(
            empty: _vaccinations.isEmpty,
            emptyLabel: 'Chưa có bản ghi tiêm chủng.',
          ),
          for (final record in _vaccinations) ...[
            Material(
              color: Colors.transparent,
              child: ListTile(
                key: ValueKey('vaccination-${record.vaccinationId}'),
                contentPadding: EdgeInsets.zero,
                leading: const CircleAvatar(
                  backgroundColor: _surfaceContainer,
                  child: Icon(Icons.vaccines_outlined, color: _primary),
                ),
                title: Text(
                  record.vaccineName,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
                subtitle: Text(
                  '${record.status.displayLabel} · ${record.plannedDateLabel}',
                  style: const TextStyle(color: _onSurfaceVariant),
                ),
                trailing: const Icon(Icons.chevron_right_rounded),
                onTap: () => _openVaccinationDetail(record),
              ),
            ),
            const Divider(height: 1),
          ],
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              key: const Key('baby-care-vaccination-add'),
              onPressed: _openVaccination,
              icon: const Icon(Icons.add),
              label: const Text('Thêm mũi tiêm'),
              style: FilledButton.styleFrom(
                backgroundColor: _primaryContainer,
                foregroundColor: Colors.white,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMilestoneTab() {
    return Container(
      key: const Key('baby-care-milestones'),
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
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
          const Text(
            'Cột mốc phát triển',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 12),
          const Text(
            'Ghi nhận cột mốc mới hoặc mở lại một cột mốc đã lưu.',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
              height: 1.4,
            ),
          ),
          const SizedBox(height: 16),
          _buildCollectionStatus(
            empty: _milestones.isEmpty,
            emptyLabel: 'Chưa có cột mốc phát triển.',
          ),
          for (final milestone in _milestones) ...[
            Material(
              color: Colors.transparent,
              child: ListTile(
                key: ValueKey('milestone-${milestone.id}'),
                contentPadding: EdgeInsets.zero,
                leading: const CircleAvatar(
                  backgroundColor: _surfaceContainer,
                  child: Icon(Icons.flag_outlined, color: _primary),
                ),
                title: Text(
                  milestone.milestoneType.displayLabel,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
                subtitle: Text(
                  '${milestone.achievedDate.day.toString().padLeft(2, '0')}/'
                  '${milestone.achievedDate.month.toString().padLeft(2, '0')}/'
                  '${milestone.achievedDate.year}',
                  style: const TextStyle(color: _onSurfaceVariant),
                ),
                trailing: const Icon(Icons.chevron_right_rounded),
                onTap: () => _openMilestoneDetail(milestone),
              ),
            ),
            const Divider(height: 1),
          ],
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              key: const Key('baby-care-milestone-add'),
              onPressed: () {
                _openAddMilestone();
              },
              icon: const Icon(Icons.add),
              label: const Text('Ghi nhận cột mốc'),
              style: FilledButton.styleFrom(
                backgroundColor: _primaryContainer,
                foregroundColor: Colors.white,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCollectionStatus({
    required bool empty,
    required String emptyLabel,
  }) {
    if (_careCollectionsLoading) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 12),
        child: Center(
          child: CircularProgressIndicator(color: _primaryContainer),
        ),
      );
    }
    if (_careCollectionsError != null) {
      return Padding(
        padding: const EdgeInsets.only(bottom: 12),
        child: Text(
          _careCollectionsError!,
          style: const TextStyle(color: _onSurfaceVariant),
        ),
      );
    }
    if (!empty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Text(
        emptyLabel,
        style: const TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant),
      ),
    );
  }

  Widget _buildGrowthTab() {
    return Container(
      key: const Key('baby-care-growth'),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
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
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Xu hướng cân nặng',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: _secondaryContainer.withAlpha(77),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Text(
                  '1 tháng qua',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                    color: _secondary,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              key: const Key('baby-care-growth-history'),
              onPressed: _openGrowth,
              icon: const Icon(Icons.history),
              label: const Text('Mở lịch sử đo lường'),
            ),
          ),
          const SizedBox(height: 12),
          _buildTrendChart(),
          const SizedBox(height: 12),
          const Text(
            'Dữ liệu đo lường được hiển thị theo nguồn đã ghi nhận.',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _secondary,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTrendChart() {
    if (_growthLoading) {
      return const _GrowthChartLoading();
    }
    if (_growthError != null) {
      return _InlineDataError(
        key: const Key('baby-growth-error'),
        message: _growthError!,
        onRetry: _retryGrowth,
      );
    }

    final measurements = _growthMeasurements
        .where((measurement) => measurement.weightKg != null)
        .toList(growable: false);
    if (measurements.isEmpty) {
      return const _EmptyGrowthChart();
    }
    final weights = measurements
        .map((measurement) => measurement.weightKg!)
        .toList(growable: false);
    return SizedBox(
      key: ValueKey('growth-chart-points-${weights.length}'),
      height: 160,
      child: Column(
        children: [
          Expanded(
            child: CustomPaint(
              painter: _TrendChartPainter(measurements),
              size: Size.infinite,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            '${weights.first.toStringAsFixed(1)} kg – '
            '${weights.last.toStringAsFixed(1)} kg',
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              color: _onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}

// ─── Sub-widgets ──────────────────────────────────────────────────────────────

class _StatChip extends StatelessWidget {
  final IconData icon;
  final String label;

  const _StatChip({required this.icon, required this.label});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: const Color(0xFFFFE9E3),
        borderRadius: BorderRadius.circular(99),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 16, color: const Color(0xFF524440)),
          const SizedBox(width: 4),
          Text(
            label,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w500,
              color: Color(0xFF524440),
            ),
          ),
        ],
      ),
    );
  }
}

class _SummaryCard extends StatelessWidget {
  final IconData icon;
  final String value;
  final String label;

  const _SummaryCard({
    super.key,
    required this.icon,
    required this.value,
    required this.label,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 8),
      decoration: BoxDecoration(
        color: Colors.white,
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
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: const Color(0xFFC98C7B).withAlpha(30),
              shape: BoxShape.circle,
            ),
            child: Icon(icon, size: 20, color: const Color(0xFF845143)),
          ),
          const SizedBox(height: 8),
          Text(
            value,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w600,
              color: Color(0xFF845143),
            ),
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w500,
              color: Color(0xFF6E5A52),
            ),
          ),
        ],
      ),
    );
  }
}

class _SummaryLoadingRow extends StatelessWidget {
  const _SummaryLoadingRow();

  @override
  Widget build(BuildContext context) {
    return Row(
      key: const Key('baby-summary-loading'),
      children: List.generate(
        3,
        (index) => Expanded(
          child: Padding(
            padding: EdgeInsets.only(right: index == 2 ? 0 : 12),
            child: Container(
              height: 116,
              decoration: BoxDecoration(
                color: const Color(0xFFE8DDD6),
                borderRadius: BorderRadius.circular(16),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _EmptySummary extends StatelessWidget {
  const _EmptySummary();

  @override
  Widget build(BuildContext context) {
    return const SizedBox(
      key: Key('baby-summary-empty'),
      height: 96,
      child: Center(
        child: Text(
          'Chưa có dữ liệu tổng kết.',
          style: TextStyle(fontFamily: 'Lexend', color: Color(0xFF6E5A52)),
        ),
      ),
    );
  }
}

class _InlineDataError extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const _InlineDataError({
    super.key,
    required this.message,
    required this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFF2EAE4),
        borderRadius: BorderRadius.circular(16),
        border: const Border(
          left: BorderSide(color: Color(0xFFC98C7B), width: 4),
        ),
      ),
      child: Row(
        children: [
          const Icon(Icons.info_outline_rounded, color: Color(0xFF845143)),
          const SizedBox(width: 12),
          Expanded(child: Text(message)),
          TextButton(onPressed: onRetry, child: const Text('Thử lại')),
        ],
      ),
    );
  }
}

class _GrowthChartLoading extends StatelessWidget {
  const _GrowthChartLoading();

  @override
  Widget build(BuildContext context) {
    return Container(
      key: const Key('baby-growth-loading'),
      height: 160,
      decoration: BoxDecoration(
        color: const Color(0xFFE8DDD6),
        borderRadius: BorderRadius.circular(16),
      ),
    );
  }
}

class _EmptyGrowthChart extends StatelessWidget {
  const _EmptyGrowthChart();

  @override
  Widget build(BuildContext context) {
    return const SizedBox(
      key: Key('baby-growth-empty'),
      height: 120,
      child: Center(
        child: Text(
          'Chưa có dữ liệu cân nặng.',
          style: TextStyle(fontFamily: 'Lexend', color: Color(0xFF6E5A52)),
        ),
      ),
    );
  }
}

class _TabChip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _TabChip({
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
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
        decoration: BoxDecoration(
          color: selected ? const Color(0xFF845143) : Colors.white,
          borderRadius: BorderRadius.circular(99),
          border: selected ? null : Border.all(color: const Color(0xFFD6C2BD)),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: selected ? Colors.white : const Color(0xFF524440),
          ),
        ),
      ),
    );
  }
}

// Simple line chart painter for weight trend
class _TrendChartPainter extends CustomPainter {
  final List<GrowthMeasurement> measurements;

  const _TrendChartPainter(this.measurements);

  @override
  void paint(Canvas canvas, Size size) {
    final linePaint = Paint()
      ..color = const Color(0xFFC98C7B)
      ..strokeWidth = 3
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;

    final fillPaint = Paint()
      ..shader = LinearGradient(
        begin: Alignment.topCenter,
        end: Alignment.bottomCenter,
        colors: [
          const Color(0xFFC98C7B).withAlpha(51),
          const Color(0xFFC98C7B).withAlpha(0),
        ],
      ).createShader(Rect.fromLTWH(0, 0, size.width, size.height))
      ..style = PaintingStyle.fill;

    final weights = measurements
        .map((measurement) => measurement.weightKg!)
        .toList(growable: false);
    final minWeight = weights.reduce((a, b) => a < b ? a : b);
    final maxWeight = weights.reduce((a, b) => a > b ? a : b);
    final centerWeight = (minWeight + maxWeight) / 2;
    final displayRange = (maxWeight - minWeight)
        .clamp(1.0, double.infinity)
        .toDouble();
    final displayMin = centerWeight - displayRange / 2;
    final firstTime = measurements.first.measuredAt.millisecondsSinceEpoch;
    final lastTime = measurements.last.measuredAt.millisecondsSinceEpoch;
    final timeRange = lastTime - firstTime;
    final points = List.generate(measurements.length, (index) {
      final x = measurements.length == 1 || timeRange == 0
          ? size.width / 2
          : size.width *
                (measurements[index].measuredAt.millisecondsSinceEpoch -
                    firstTime) /
                timeRange;
      final normalized = (weights[index] - displayMin) / displayRange;
      return Offset(x, size.height * (0.9 - normalized * 0.7));
    });

    // Draw smooth curve
    final path = Path()..moveTo(points.first.dx, points.first.dy);
    for (int i = 0; i < points.length - 1; i++) {
      final cp1 = Offset((points[i].dx + points[i + 1].dx) / 2, points[i].dy);
      final cp2 = Offset(
        (points[i].dx + points[i + 1].dx) / 2,
        points[i + 1].dy,
      );
      path.cubicTo(
        cp1.dx,
        cp1.dy,
        cp2.dx,
        cp2.dy,
        points[i + 1].dx,
        points[i + 1].dy,
      );
    }

    // Fill under curve
    final fillPath = Path.from(path)
      ..lineTo(size.width, size.height)
      ..lineTo(0, size.height)
      ..close();
    canvas.drawPath(fillPath, fillPaint);

    // Draw line
    canvas.drawPath(path, linePaint);

    // Draw dots
    final dotPaint = Paint()
      ..color = const Color(0xFF845143)
      ..style = PaintingStyle.fill;
    for (final p in points) {
      canvas.drawCircle(p, p == points.last ? 5 : 3, dotPaint);
    }
  }

  @override
  bool shouldRepaint(covariant _TrendChartPainter oldDelegate) {
    if (measurements.length != oldDelegate.measurements.length) return true;
    for (var i = 0; i < measurements.length; i++) {
      if (measurements[i].weightKg != oldDelegate.measurements[i].weightKg ||
          measurements[i].measuredAt !=
              oldDelegate.measurements[i].measuredAt) {
        return true;
      }
    }
    return false;
  }
}
