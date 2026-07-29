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

/// CB-027 — Shared Care Group Detail (UC-83, UC-84, UC-71, UC-73, UC-216)
/// Shows group info, member circles (64x64 with star for owner),
/// bento grid sections (calendar, tasks, shared data, alerts).
/// Calls GET /api/v1/care-groups/{id}/members.
class CareGroupDetailScreen extends StatefulWidget {
  final String groupId;
  final String groupName;

  const CareGroupDetailScreen({
    super.key,
    required this.groupId,
    required this.groupName,
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

  final _service = CareGroupService();
  final _journeyService = JourneyService();
  final _healthMetricService = HealthMetricService();
  CareGroup? _group;
  FamilyPermission? _myPermissions;
  JourneyDashboard? _dashboard;
  MetricTrend? _weightTrend;
  MetricTrend? _heartRateTrend;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final g = await _service.getGroupMembers(widget.groupId);
      FamilyPermission? perm;
      final isMotherUser =
          AuthState.instance.role == 'MOTHER' ||
          g.myRole == 'MOTHER' ||
          g.myRole == 'OWNER';
      if (!isMotherUser) {
        final currentUserId = AuthState.instance.userId;
        final myMember = g.members.firstWhere(
          (m) => m.userId == currentUserId || m.memberId == currentUserId,
          orElse: () => g.members.isNotEmpty
              ? g.members.first
              : const CareGroupMember(
                  memberId: '',
                  displayName: '',
                  memberRole: 'MEMBER',
                  inviteStatus: 'ACCEPTED',
                ),
        );
        if (myMember.memberId.isNotEmpty) {
          try {
            perm = await _service.getFamilyPermission(
              widget.groupId,
              myMember.memberId,
            );
          } catch (_) {}
        }
      }

      JourneyDashboard? dashboard;
      MetricTrend? weightTrend;
      MetricTrend? heartRateTrend;
      try {
        dashboard = await _journeyService.getDashboard();
        if (dashboard.hasActiveJourney && dashboard.journeyId != null) {
          try {
            weightTrend = await _healthMetricService.getMetricTrend(
              journeyId: dashboard.journeyId!,
              metricType: 'WEIGHT',
              from: DateTime.now().subtract(const Duration(days: 28)),
              to: DateTime.now(),
            );
          } catch (_) {}
          try {
            heartRateTrend = await _healthMetricService.getMetricTrend(
              journeyId: dashboard.journeyId!,
              metricType: 'HEART_RATE',
              from: DateTime.now().subtract(const Duration(days: 28)),
              to: DateTime.now(),
            );
          } catch (_) {}
        }
      } catch (_) {}

      if (mounted) {
        setState(() {
          _group = g;
          _myPermissions = perm;
          _dashboard = dashboard;
          _weightTrend = weightTrend;
          _heartRateTrend = heartRateTrend;
          _loading = false;
        });
      }
    } catch (_) {
      // Fallback mock
      if (mounted) {
        setState(() {
          _group = CareGroup(
            id: widget.groupId,
            groupName: widget.groupName,
            isActive: true,
            memberCount: 3,
            members: [
              CareGroupMember(
                memberId: 'm-1',
                displayName: 'Mẹ Linh',
                memberRole: 'ADMIN',
                inviteStatus: 'ACCEPTED',
                joinedAt: DateTime(2024, 1, 5),
              ),
              CareGroupMember(
                memberId: 'm-2',
                displayName: 'Bố Tuấn',
                memberRole: 'MEMBER',
                inviteStatus: 'ACCEPTED',
                joinedAt: DateTime(2024, 1, 10),
              ),
              CareGroupMember(
                memberId: 'm-3',
                displayName: 'Bà Ngoại',
                memberRole: 'MEMBER',
                inviteStatus: 'ACCEPTED',
                joinedAt: DateTime(2024, 2, 3),
              ),
            ],
          );
          _loading = false;
        });
      }
    }
  }

  bool get _isMother =>
      AuthState.instance.role == 'MOTHER' ||
      _group?.myRole == 'MOTHER' ||
      _group?.myRole == 'OWNER';

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
                    sliver: SliverToBoxAdapter(child: _buildMotherJourneySection()),
                  ),
                ],
              ),
      ),
    );
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
          child: _buildBentoMetricCard(
            icon: Icons.monitor_weight_outlined,
            label: 'Cân nặng',
            value: weightValue,
            unit: 'kg',
            trend: weightTrendPct,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildBentoMetricCard(
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
                  const SnackBar(content: Text('Đã sao chép mã nhóm vào bộ nhớ tạm!')),
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
                  builder: (_) => SharedDataScreen(
                    groupId: widget.groupId,
                  ),
                ),
              );
            },
          ),
        ),
      ],
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
                  Icon(
                    icon,
                    color: const Color(0xFF845143),
                    size: 24,
                  ),
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
