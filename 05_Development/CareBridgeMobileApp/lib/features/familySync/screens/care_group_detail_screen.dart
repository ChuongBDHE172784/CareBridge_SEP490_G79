import 'dart:math';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../core/auth/auth_state.dart';
import '../../journey/models/journey_model.dart';
import '../../journey/services/journey_service.dart';
import '../../healthRecords/models/health_metric_model.dart';
import '../../healthRecords/services/health_metric_service.dart';
import '../models/care_group_model.dart';
import '../services/care_group_service.dart';
import 'care_group_members_screen.dart';
import 'invite_family_member_screen.dart';
import 'sent_pending_invitations_screen.dart';
import 'assigned_tasks_screen.dart';
import 'leave_care_group_confirmation_screen.dart';
import 'shared_data_screen.dart';
import '../models/family_permission_model.dart';
import '../services/family_home_service.dart';
import 'family_quick_note_history_screen.dart';

/// CB-027 — Shared Care Group Detail (UC-83, UC-84, UC-71, UC-73, UC-216)
/// Shows group info, member circles (64x64 with star for owner),
/// Shows tasks, shared data, alerts, and accepted-member context.
/// Calls GET /api/v1/care-groups/{id}/members.
class CareGroupDetailScreen extends StatefulWidget {
  final String groupId;
  final String groupName;
  final CareGroupService? service;
  final Future<FamilyHomeSnapshot> Function({String? selectedCareGroupId})?
  dashboardLoader;

  const CareGroupDetailScreen({
    super.key,
    required this.groupId,
    required this.groupName,
    this.service,
    this.dashboardLoader,
  });

  @override
  State<CareGroupDetailScreen> createState() => _CareGroupDetailScreenState();
}

class _CareGroupDetailScreenState extends State<CareGroupDetailScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  late final CareGroupService _service = widget.service ?? CareGroupService();
  final _journeyService = JourneyService();
  final _healthMetricService = HealthMetricService();
  CareGroup? _group;
  FamilyPermission? _myPermissions;
  JourneyDashboard? _dashboard;
  MetricTrend? _bmiTrend;
  FamilyHomeGroupDetail? _familyDetail;
  Object? _familyDetailError;
  bool _loading = true;
  Object? _loadError;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _loadError = null;
    });
    try {
      final g = await _service.getGroupMembers(widget.groupId);
      FamilyPermission? perm;
      final isMotherUser = _isOwnerInGroup(g);
      if (!isMotherUser) {
        final currentUserId = AuthState.instance.userId;
        CareGroupMember? myMember;
        for (final member in g.members) {
          if (member.userId == currentUserId ||
              member.memberId == currentUserId) {
            myMember = member;
            break;
          }
        }
        if (myMember != null) {
          try {
            perm = await _service.getFamilyPermission(
              widget.groupId,
              myMember.memberId,
            );
          } catch (_) {}
        }
      }

      JourneyDashboard? dashboard;
      MetricTrend? bmiTrend;
      FamilyHomeGroupDetail? familyDetail;
      Object? familyDetailError;
      if (!isMotherUser) {
        try {
          final snapshot =
              await (widget.dashboardLoader ??
                  FamilyHomeService.instance.loadSnapshot)(
                selectedCareGroupId: widget.groupId,
              );
          if (snapshot.selectedCareGroupId == widget.groupId) {
            familyDetail = snapshot.selectedGroupDetail;
          }
        } catch (error) {
          familyDetailError = error;
        }
      } else {
        try {
          dashboard = await _journeyService.getDashboard();
          if (dashboard.hasActiveJourney && dashboard.journeyId != null) {
            try {
              bmiTrend = await _healthMetricService.getMetricTrend(
                journeyId: dashboard.journeyId!,
                metricType: 'BMI',
                from: DateTime.now().subtract(const Duration(days: 28)),
                to: DateTime.now(),
              );
            } catch (_) {}
          }
        } catch (_) {}
      }

      if (mounted) {
        setState(() {
          _group = g;
          _myPermissions = perm;
          _dashboard = dashboard;
          _bmiTrend = bmiTrend;
          _familyDetail = familyDetail;
          _familyDetailError = familyDetailError;
          _loading = false;
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() {
          _loadError = error;
          _loading = false;
        });
      }
    }
  }

  bool _isOwnerInGroup(CareGroup group) {
    if (group.myRole == 'OWNER' || group.myRole == 'MOTHER') return true;
    final currentUserId = AuthState.instance.userId;
    return currentUserId != null &&
        group.members.any(
          (member) =>
              member.userId == currentUserId && member.memberRole == 'OWNER',
        );
  }

  bool get _isMother => _group != null && _isOwnerInGroup(_group!);

  bool get _canAccessAlertsAndTasks =>
      _isMother || (_myPermissions != null ? _myPermissions!.alerts : false);
  bool get _canAccessData =>
      _isMother ||
      (_myPermissions != null
          ? (_myPermissions!.logs || _myPermissions!.records)
          : false);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: _loading
            ? const Center(
                child: CircularProgressIndicator(color: _primaryContainer),
              )
            : _loadError != null
            ? _buildLoadError()
            : CustomScrollView(
                slivers: [
                  SliverToBoxAdapter(child: _buildAppBar()),
                  SliverToBoxAdapter(child: _buildMemberSection()),
                  SliverPadding(
                    padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
                    sliver: SliverToBoxAdapter(child: _buildBentoGrid()),
                  ),
                  SliverPadding(
                    padding: const EdgeInsets.fromLTRB(20, 0, 20, 32),
                    sliver: SliverToBoxAdapter(
                      child: _isMother
                          ? _buildMotherJourneySection()
                          : _buildFamilyHealthSection(),
                    ),
                  ),
                ],
              ),
      ),
    );
  }

  Widget _buildLoadError() {
    return Center(
      key: const Key('care-group-detail-error'),
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.cloud_off_outlined, size: 46, color: _primary),
            const SizedBox(height: 12),
            const Text(
              'Không thể tải thông tin nhóm lúc này.',
              textAlign: TextAlign.center,
              style: TextStyle(fontFamily: 'Lexend', color: _onSurface),
            ),
            const SizedBox(height: 8),
            TextButton.icon(
              key: const Key('care-group-detail-retry'),
              onPressed: _load,
              icon: const Icon(Icons.refresh_rounded),
              label: const Text('Thử lại'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFamilyHealthSection() {
    final detail = _familyDetail;
    if (_familyDetailError != null) {
      return _FamilyHealthStateCard(
        key: const Key('family-group-health-error'),
        icon: Icons.cloud_off_outlined,
        message: 'Chưa thể tải chỉ số sức khỏe. Thông tin nhóm vẫn khả dụng.',
        action: TextButton.icon(
          onPressed: _load,
          icon: const Icon(Icons.refresh_rounded),
          label: const Text('Thử lại'),
        ),
      );
    }
    if (detail == null) {
      return const _FamilyHealthStateCard(
        key: Key('family-group-health-unavailable'),
        icon: Icons.lock_outline_rounded,
        message: 'Bạn chưa được chia sẻ thông tin sức khỏe trong nhóm này.',
      );
    }
    final permission = detail.permissionScope;
    final definitions =
        <({String code, String label, IconData icon, bool shared})>[
          (
            code: 'BMI',
            label: 'Chỉ số BMI',
            icon: Icons.calculate_outlined,
            shared: _isHealthMetricShared('BMI', permission),
          ),
          (
            code: 'FETAL_MOVEMENT_COUNT',
            label: 'Cử động thai',
            icon: Icons.child_friendly_outlined,
            shared: _isHealthMetricShared('FETAL_MOVEMENT_COUNT', permission),
          ),
          (
            code: 'BLOOD_PRESSURE',
            label: 'Huyết áp',
            icon: Icons.monitor_heart_outlined,
            shared: _isHealthMetricShared('BLOOD_PRESSURE', permission),
          ),
          (
            code: 'HYDRATION',
            label: 'Nước',
            icon: Icons.water_drop_outlined,
            shared: _isHealthMetricShared('HYDRATION', permission),
          ),
          (
            code: 'EPDS_SCORE',
            label: 'Sàng lọc EPDS',
            icon: Icons.psychology_alt_outlined,
            shared: _isHealthMetricShared('EPDS_SCORE', permission),
          ),
          (
            code: 'BLOOD_GLUCOSE',
            label: 'Đường huyết',
            icon: Icons.bloodtype_outlined,
            shared: _isHealthMetricShared('BLOOD_GLUCOSE', permission),
          ),
        ].where((item) => item.shared).toList(growable: false);

    return Column(
      key: const Key('family-group-health-section'),
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Tình trạng sức khỏe của ${detail.motherDisplayName}',
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 20,
            fontWeight: FontWeight.w700,
            color: _onSurface,
          ),
        ),
        const SizedBox(height: 6),
        const Text(
          'Thông tin do mẹ chủ động chia sẻ • Chỉ xem',
          style: TextStyle(color: _onSurfaceVariant),
        ),
        const SizedBox(height: 14),
        if (definitions.isEmpty)
          const _FamilyHealthStateCard(
            key: Key('family-group-health-locked'),
            icon: Icons.lock_outline_rounded,
            message: 'Mẹ chưa chia sẻ chỉ số sức khỏe với bạn.',
          )
        else
          ...definitions.map((definition) {
            FamilyHomeHealthMetricSummary? summary;
            for (final item in detail.healthMetricSummaries) {
              if (item.metricType == definition.code) {
                summary = item;
                break;
              }
            }
            final value = summary?.valueDisplay;
            final unit = _localizedFamilyMetricUnit(summary?.unit);
            final measuredAt = summary?.measuredAt;
            return Card(
              margin: const EdgeInsets.only(bottom: 10),
              elevation: 0,
              color: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(20),
                side: const BorderSide(color: Color(0xFFF0E4DF)),
              ),
              child: ListTile(
                key: Key(
                  'family-group-health-${definition.code.toLowerCase()}',
                ),
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 7,
                ),
                leading: CircleAvatar(
                  backgroundColor: const Color(0xFFFFE9E3),
                  foregroundColor: _primary,
                  child: Icon(definition.icon),
                ),
                title: Text(
                  definition.label,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontWeight: FontWeight.w700,
                  ),
                ),
                subtitle: Text(
                  measuredAt == null
                      ? 'Đã chia sẻ • Chưa có dữ liệu'
                      : 'Cập nhật ${_familyMetricTime(measuredAt)}${_familyGlucoseContext(summary?.measurementContext)}',
                ),
                trailing: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 112),
                  child: Text(
                    value == null
                        ? '—'
                        : '$value${unit?.isNotEmpty == true ? ' $unit' : ''}',
                    textAlign: TextAlign.end,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontWeight: FontWeight.w700,
                      color: _primary,
                    ),
                  ),
                ),
                onTap: () => Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => FamilyQuickNoteHistoryScreen(
                      careGroupId: widget.groupId,
                      metricType: definition.code,
                    ),
                  ),
                ),
              ),
            );
          }),
        if (definitions.any((item) => item.code == 'EPDS_SCORE'))
          const Padding(
            padding: EdgeInsets.only(top: 2),
            child: Text(
              'Điểm EPDS là công cụ sàng lọc, không thay thế chẩn đoán chuyên môn.',
              style: TextStyle(fontSize: 12, color: _onSurfaceVariant),
            ),
          ),
      ],
    );
  }

  bool _isHealthMetricShared(
    String metricCode,
    FamilyHomePermission dashboardPermission,
  ) {
    final directPermission = _myPermissions;
    if (directPermission == null) {
      return switch (metricCode) {
        'BMI' =>
          dashboardPermission.quickNotes && dashboardPermission.quickNoteWeight,
        'FETAL_MOVEMENT_COUNT' =>
          dashboardPermission.quickNotes &&
              dashboardPermission.quickNoteFetalMovement,
        'BLOOD_PRESSURE' =>
          dashboardPermission.quickNotes &&
              dashboardPermission.quickNoteBloodPressure,
        'HYDRATION' =>
          dashboardPermission.quickNotes &&
              dashboardPermission.quickNoteHydration,
        'EPDS_SCORE' =>
          dashboardPermission.quickNotes && dashboardPermission.quickNoteEpds,
        'BLOOD_GLUCOSE' =>
          dashboardPermission.quickNotes &&
              dashboardPermission.quickNoteBloodGlucose,
        _ => false,
      };
    }
    if (!directPermission.quickNotes) return false;
    return switch (metricCode) {
      'BMI' => directPermission.quickNoteWeight,
      'FETAL_MOVEMENT_COUNT' => directPermission.quickNoteFetalMovement,
      'BLOOD_PRESSURE' => directPermission.quickNoteBloodPressure,
      'HYDRATION' => directPermission.quickNoteHydration,
      'EPDS_SCORE' => directPermission.quickNoteEpds,
      'BLOOD_GLUCOSE' => directPermission.quickNoteBloodGlucose,
      _ => false,
    };
  }

  String _familyMetricTime(DateTime value) {
    final local = value.toLocal();
    return '${local.hour.toString().padLeft(2, '0')}:'
        '${local.minute.toString().padLeft(2, '0')} '
        '${local.day.toString().padLeft(2, '0')}/'
        '${local.month.toString().padLeft(2, '0')}/${local.year}';
  }

  String? _localizedFamilyMetricUnit(String? unit) {
    return switch (unit) {
      'count' => 'lần',
      _ => unit,
    };
  }

  String _familyGlucoseContext(String? value) {
    return switch (value) {
      'FASTING' => ' • Lúc đói',
      'PRE_MEAL' => ' • Trước ăn',
      'POST_MEAL_1H' => ' • Sau ăn 1 giờ',
      'POST_MEAL_2H' => ' • Sau ăn 2 giờ',
      'RANDOM' => ' • Ngẫu nhiên',
      'OTHER_APPROVED' => ' • Thời điểm khác',
      _ => '',
    };
  }

  Widget _buildMotherJourneySection() {
    final dashboard = _dashboard;
    if (dashboard == null || !dashboard.hasActiveJourney) {
      return const SizedBox.shrink();
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Padding(
          padding: EdgeInsets.only(bottom: 14),
          child: Text(
            'Hành trình của mẹ',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
        ),
        _buildJourneyHeroCard(dashboard),
        const SizedBox(height: 12),
        _buildJourneyDueDateCard(dashboard),
        const SizedBox(height: 12),
        _buildJourneyMetricsBentoSummary(),
      ],
    );
  }

  Widget _buildJourneyHeroCard(JourneyDashboard dashboard) {
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
                    fontSize: 32,
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
                    fontSize: 13,
                    color: _onSurfaceVariant,
                    height: 1.4,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 16),
          _CircularProgressWidget(
            progress: dashboard.pregnancyProgress,
            week: week,
          ),
        ],
      ),
    );
  }

  Widget _buildJourneyDueDateCard(JourneyDashboard dashboard) {
    final dueDate = dashboard.estimatedDueDate;
    final monthLabel = dueDate != null
        ? 'THG ${dueDate.month.toString().padLeft(2, '0')}'
        : '--';
    final dayLabel = dueDate != null ? '${dueDate.day}' : '--';

    return Container(
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
      child: Row(
        children: [
          Container(
            width: 64,
            height: 64,
            decoration: BoxDecoration(
              color: const Color(0xFFFFE2D9),
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
        ],
      ),
    );
  }

  String _daysUntilDueLabel(JourneyDashboard dashboard) {
    final days = dashboard.calculatedDaysUntilDue;
    if (days == null) return 'Chưa có ngày dự sinh';
    if (days == 0) return 'Dự sinh hôm nay';
    if (days > 0) return 'Còn lại $days ngày';
    return 'Quá ngày dự sinh ${days.abs()} ngày';
  }

  Widget _buildJourneyMetricsBentoSummary() {
    final bmiPoint = _bmiTrend?.dataPoints.isNotEmpty == true
        ? _bmiTrend!.dataPoints.last
        : null;
    final height = (bmiPoint?.context['heightCm'] as num?)?.toDouble();
    final bmiValue = bmiPoint?.valueDisplay ?? '—';
    final bmiTrendPct = _bmiTrend?.trend;
    final heightValue = height == null
        ? '—'
        : height % 1 == 0
        ? height.toStringAsFixed(0)
        : height.toStringAsFixed(1);

    return Row(
      children: [
        Expanded(
          child: _buildBentoMetricCard(
            icon: Icons.calculate_outlined,
            label: 'Chỉ số BMI',
            value: bmiValue,
            unit: 'kg/m²',
            trend: bmiTrendPct,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildBentoMetricCard(
            icon: Icons.height_rounded,
            label: 'Chiều cao',
            value: heightValue,
            unit: 'cm',
          ),
        ),
      ],
    );
  }

  Widget _buildBentoMetricCard({
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

  Widget _buildAppBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(4, 16, 4, 0),
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.arrow_back, color: _onSurfaceVariant),
          ),
          Expanded(
            child: Text(
              widget.groupName,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
              overflow: TextOverflow.ellipsis,
            ),
          ),
          PopupMenuButton<String>(
            icon: const Icon(Icons.more_vert, color: _onSurfaceVariant),
            onSelected: (val) async {
              if (val == 'copy_code') {
                Clipboard.setData(ClipboardData(text: widget.groupId));
                if (!mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Đã sao chép mã nhóm vào bộ nhớ tạm!'),
                  ),
                );
              } else if (val == 'pending_invites') {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (_) =>
                        SentPendingInvitationsScreen(groupId: widget.groupId),
                  ),
                );
              } else if (val == 'leave_group') {
                final left = await Navigator.push<bool>(
                  context,
                  MaterialPageRoute(
                    builder: (_) => LeaveCareGroupConfirmationScreen(
                      groupId: widget.groupId,
                      groupName: widget.groupName,
                    ),
                  ),
                );
                if (left == true && mounted) {
                  Navigator.pop(context);
                }
              }
            },
            itemBuilder: (_) => [
              const PopupMenuItem(
                value: 'copy_code',
                child: Row(
                  children: [
                    Icon(Icons.copy, color: _primary, size: 18),
                    SizedBox(width: 8),
                    Text(
                      'Sao chép mã nhóm',
                      style: TextStyle(fontFamily: 'Lexend', color: _primary),
                    ),
                  ],
                ),
              ),
              if (_isMother)
                const PopupMenuItem(
                  value: 'pending_invites',
                  child: Row(
                    children: [
                      Icon(Icons.hourglass_empty, color: _primary, size: 18),
                      SizedBox(width: 8),
                      Text(
                        'Lời mời chờ xử lý',
                        style: TextStyle(fontFamily: 'Lexend', color: _primary),
                      ),
                    ],
                  ),
                ),
              if (!_isMother)
                const PopupMenuItem(
                  value: 'leave_group',
                  child: Row(
                    children: [
                      Icon(Icons.logout, color: Color(0xFFBA1A1A), size: 18),
                      SizedBox(width: 8),
                      Text(
                        'Rời nhóm',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          color: Color(0xFFBA1A1A),
                        ),
                      ),
                    ],
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildMemberSection() {
    final members = _group?.members ?? [];
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Thành viên',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
              // Chỉ MOTHER (owner) mới được vào màn quản lý thành viên đầy đủ
              if (_isMother)
                TextButton.icon(
                  onPressed: () async {
                    await Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => CareGroupMembersScreen(
                          groupId: widget.groupId,
                          groupName: widget.groupName,
                          members: members,
                        ),
                      ),
                    );
                    _load();
                  },
                  icon: const Icon(
                    Icons.arrow_forward,
                    size: 16,
                    color: _primaryContainer,
                  ),
                  label: const Text(
                    'Xem tất cả',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      color: _primaryContainer,
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 12),
          SizedBox(
            height: 88,
            child: ListView(
              scrollDirection: Axis.horizontal,
              children: [
                // Chỉ hiển thị thành viên đã được CHẤP NHẬN (ACCEPTED)
                // Thành viên PENDING chưa vào nhóm thì không hiện ở đây
                ...members
                    .where((m) => m.inviteStatus == 'ACCEPTED')
                    .map(
                      (m) => Padding(
                        padding: const EdgeInsets.only(right: 16),
                        child: _MemberCircle(member: m),
                      ),
                    ),
                if (_isMother)
                  GestureDetector(
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (_) =>
                              InviteFamilyMemberScreen(groupId: widget.groupId),
                        ),
                      );
                    },
                    child: Column(
                      children: [
                        Container(
                          width: 64,
                          height: 64,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            border: Border.all(
                              color: _primaryContainer.withAlpha(102),
                              width: 2,
                            ),
                          ),
                          child: const Icon(
                            Icons.person_add,
                            color: _primaryContainer,
                            size: 28,
                          ),
                        ),
                        const SizedBox(height: 6),
                        const Text(
                          'Mời',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            color: Color(0xFF524440),
                          ),
                        ),
                      ],
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBentoGrid() {
    return Row(
      children: [
        Expanded(
          child: _BentoCard(
            icon: Icons.task_alt,
            label: 'Việc cần làm',
            value: 'Việc hôm nay',
            isEnabled: _canAccessAlertsAndTasks,
            onTap: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => AssignedTasksScreen(
                    groupId: widget.groupId,
                    groupName: widget.groupName,
                  ),
                ),
              );
            },
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _BentoCard(
            icon: Icons.folder_shared,
            label: 'Dữ liệu chia sẻ',
            value: 'Xem hồ sơ',
            isEnabled: _canAccessData,
            onTap: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => SharedDataScreen(groupId: widget.groupId),
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}

class _FamilyHealthStateCard extends StatelessWidget {
  const _FamilyHealthStateCard({
    super.key,
    required this.icon,
    required this.message,
    this.action,
  });

  final IconData icon;
  final String message;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: const Color(0xFFF0E4DF)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: const Color(0xFF845143)),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  message,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    color: Color(0xFF524440),
                  ),
                ),
              ),
            ],
          ),
          if (action != null) ...[const SizedBox(height: 8), action!],
        ],
      ),
    );
  }
}

class _MemberCircle extends StatelessWidget {
  final CareGroupMember member;

  const _MemberCircle({required this.member});

  @override
  Widget build(BuildContext context) {
    final isAdmin = member.isAdmin;
    return Column(
      children: [
        Stack(
          clipBehavior: Clip.none,
          children: [
            Container(
              width: 64,
              height: 64,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: const Color(0xFFFFE9E3),
                border: Border.all(
                  color: isAdmin
                      ? const Color(0xFFC98C7B)
                      : const Color(0xFFFFE2D9),
                  width: isAdmin ? 3 : 2,
                ),
              ),
              child: Center(
                child: Text(
                  member.displayName.isNotEmpty ? member.displayName[0] : '?',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 24,
                    fontWeight: FontWeight.w700,
                    color: Color(0xFF845143),
                  ),
                ),
              ),
            ),
            if (isAdmin)
              Positioned(
                top: -4,
                right: -4,
                child: Container(
                  width: 20,
                  height: 20,
                  decoration: const BoxDecoration(
                    color: Color(0xFFC98C7B),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.star, size: 12, color: Colors.white),
                ),
              ),
          ],
        ),
        const SizedBox(height: 6),
        SizedBox(
          width: 64,
          child: Text(
            member.displayName,
            textAlign: TextAlign.center,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 11,
              color: Color(0xFF524440),
            ),
          ),
        ),
      ],
    );
  }
}

class _BentoCard extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;
  final VoidCallback onTap;
  final bool isEnabled;

  const _BentoCard({
    required this.icon,
    required this.label,
    required this.value,
    required this.onTap,
    this.isEnabled = true,
  });

  @override
  Widget build(BuildContext context) {
    return Opacity(
      opacity: isEnabled ? 1.0 : 0.45,
      child: GestureDetector(
        onTap: isEnabled ? onTap : null,
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(16),
            boxShadow: [
              BoxShadow(
                color: const Color(0xFF5A463F).withAlpha(13),
                blurRadius: 12,
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
                  Icon(icon, color: const Color(0xFF845143), size: 24),
                  if (!isEnabled)
                    const Icon(
                      Icons.lock_outline,
                      size: 16,
                      color: Color(0xFF84736F),
                    ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                label,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 12,
                  color: Color(0xFF524440),
                ),
              ),
              const SizedBox(height: 4),
              Text(
                isEnabled ? value : 'Chưa cấp quyền',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: isEnabled ? 18 : 12,
                  fontWeight: isEnabled ? FontWeight.w600 : FontWeight.normal,
                  color: isEnabled
                      ? const Color(0xFF271812)
                      : const Color(0xFF84736F),
                ),
              ),
            ],
          ),
        ),
      ),
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
      width: 96,
      height: 96,
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
                  fontSize: 18,
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
    final radius = (size.width / 2) - 6;
    const strokeWidth = 7.0;

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
