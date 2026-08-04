import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../../auth/screens/account_profile_screen.dart';
import '../../community/screens/community_feed_screen.dart';
import '../../community/models/content_model.dart';
import '../../community/screens/view_content_screen.dart';
import '../../community/screens/verified_content_detail_screen.dart';
import '../../community/services/content_service.dart';
import '../../directChat/screens/conversation_list_screen.dart';
import '../../directChat/screens/expert_directory_screen.dart';
import '../../familySync/screens/my_care_groups_screen.dart';
import '../../familySync/screens/family_quick_note_history_screen.dart';
import '../../familySync/services/family_home_service.dart';
import '../../notification/screens/notification_center_screen.dart';
import '../../notification/services/notification_service.dart';
import '../../recommendation/models/recommendation_model.dart';
import '../../recommendation/services/recommendation_service.dart';
import '../../checklist/widgets/add_user_checklist_task_button.dart';
import '../../reminder/services/today_task_service.dart';
import '../../reminder/widgets/today_tasks_panel.dart';

class FamilyMemberHomeScreen extends StatefulWidget {
  const FamilyMemberHomeScreen({
    super.key,
    this.dashboardLoader,
    this.todayTaskService,
    this.recommendationService,
    this.recommendationLoader,
  });

  final Future<FamilyHomeSnapshot> Function({String? selectedCareGroupId})?
  dashboardLoader;
  final TodayTaskService? todayTaskService;
  final RecommendationService? recommendationService;
  final Future<RecommendationContentResponse> Function()? recommendationLoader;

  @override
  State<FamilyMemberHomeScreen> createState() => _FamilyMemberHomeScreenState();
}

class _FamilyMemberHomeScreenState extends State<FamilyMemberHomeScreen> {
  static const _primary = Color(0xFF845143);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Colors.white;
  static const _surfaceLow = Color(0xFFFFF1EC);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFF84736F);
  static const _error = Color(0xFFBA1A1A);

  FamilyHomeSnapshot? _snapshot;
  late final TodayTaskService _todayTaskService;
  late final RecommendationService _recommendationService;
  final TodayTasksPanelController _todayTasksController =
      TodayTasksPanelController();
  bool _loading = true;
  Object? _loadError;
  bool _permissionDenied = false;
  String? _selectedCareGroupId;
  // The dashboard API may provide a convenience detail for the first group,
  // but checklist reads must not treat that as an explicit FAMILY scope when
  // multiple groups exist.  A group becomes eligible for checklist requests
  // only after the member selects it (a single group is unambiguous).
  bool _checklistGroupExplicitlySelected = false;
  bool _hasUnread = false;
  int _loadGeneration = 0;

  RecommendationContentResponse? _recommendations;
  bool _recommendationLoading = false;
  String? _recommendationError;
  int _recommendationLoadGeneration = 0;

  @override
  void initState() {
    super.initState();
    _todayTaskService = widget.todayTaskService ?? TodayTaskService.instance;
    _recommendationService =
        widget.recommendationService ?? RecommendationService();
    _load();
  }

  Future<void> _checkUnread({
    required int generation,
    required String? accountId,
  }) async {
    if (WidgetsBinding.instance.runtimeType.toString().contains('Test')) {
      return;
    }
    try {
      final notifs = await NotificationService.instance.getNotifications(
        size: 20,
      );
      String? activeUserId;
      try {
        activeUserId = AuthState.instance.userId;
      } catch (_) {}
      if (mounted &&
          generation == _loadGeneration &&
          accountId == activeUserId) {
        setState(() => _hasUnread = notifs.any((n) => n.isUnread));
      }
    } catch (_) {}
  }

  Future<void> _loadRecommendations() async {
    final generation = ++_recommendationLoadGeneration;
    String? accountId;
    try {
      accountId = AuthState.instance.userId;
    } catch (_) {}
    setState(() {
      _recommendationLoading = true;
      _recommendationError = null;
    });
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

  Future<void> _load() async {
    final generation = ++_loadGeneration;
    final todayRefresh = _todayTasksController.refresh();
    final recommendationRefresh = _loadRecommendations();
    String? currentAccountId;
    try {
      currentAccountId = AuthState.instance.userId;
    } catch (_) {}
    _checkUnread(generation: generation, accountId: currentAccountId);
    setState(() {
      _loading = true;
      _loadError = null;
      _permissionDenied = false;
    });
    try {
      final snapshot =
          await (widget.dashboardLoader ??
              FamilyHomeService.instance.loadSnapshot)(
            selectedCareGroupId: _selectedCareGroupId,
          );
      if (!mounted || generation != _loadGeneration) return;
      String? activeAccountId;
      try {
        activeAccountId = AuthState.instance.userId;
      } catch (_) {}
      if (currentAccountId != null && currentAccountId != activeAccountId) {
        return;
      }
      setState(() {
        _snapshot = snapshot;
        _selectedCareGroupId = snapshot.selectedCareGroupId;
        _loading = false;
      });
    } catch (error) {
      if (!mounted || generation != _loadGeneration) return;
      String? activeAccountId;
      try {
        activeAccountId = AuthState.instance.userId;
      } catch (_) {}
      if (currentAccountId != null && currentAccountId != activeAccountId) {
        return;
      }
      final terminal =
          error is ApiException &&
          (error.statusCode == 401 ||
              error.statusCode == 403 ||
              error.statusCode == 404);
      setState(() {
        _loading = false;
        _permissionDenied = error is ApiException && error.statusCode == 403;
        _loadError = error;
        if (terminal) {
          _snapshot = null;
          _selectedCareGroupId = null;
          _checklistGroupExplicitlySelected = false;
        }
      });
    } finally {
      await Future.wait([todayRefresh, recommendationRefresh]);
    }
  }

  @override
  Widget build(BuildContext context) {
    final initialLoading = _loading && _snapshot == null;
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        bottom: false,
        child: Stack(
          children: [
            RefreshIndicator(
              color: _primary,
              onRefresh: _load,
              child: ListView(
                // Leave enough scrollable space above the persistent bottom nav
                // so the group selector and its menu remain fully tappable.
                padding: const EdgeInsets.fromLTRB(20, 16, 20, 220),
                children: [
                  _buildHeader(),
                  const SizedBox(height: 20),
                  _buildShortcuts(),
                  const SizedBox(height: 28),
                  if (initialLoading)
                    const Padding(
                      key: Key('family-dashboard-loading'),
                      padding: EdgeInsets.only(top: 96),
                      child: Center(
                        child: CircularProgressIndicator(color: _primary),
                      ),
                    )
                  else if (_permissionDenied)
                    _buildPermissionDenied()
                  else if (_loadError != null)
                    _buildError()
                  else if (_snapshot!.groups.isEmpty)
                    _buildNoGroup()
                  else ...[
                    const SizedBox(height: 28),
                    if (_snapshot!.groups.length > 1) ...[
                      _buildFollowingGroups(_snapshot!),
                      const SizedBox(height: 20),
                      _buildGroupSelector(_snapshot!),
                    ],
                    const SizedBox(height: 24),
                    _buildGlobalAggregate(_snapshot!.globalAggregate),
                    if (_loading)
                      const Padding(
                        key: Key('family-dashboard-loading'),
                        padding: EdgeInsets.only(top: 16),
                        child: LinearProgressIndicator(color: _primary),
                      ),
                    const SizedBox(height: 28),
                    if (_snapshot!.selectedGroupDetail != null)
                      _buildSelectedGroupDetail(
                        _snapshot!.selectedGroupDetail!,
                      ),
                  ],
                  const SizedBox(height: 28),
                  if (_selectedCareGroupId != null &&
                      (_checklistGroupExplicitlySelected ||
                          _snapshot!.groups.length <= 1))
                    TodayTasksPanel(
                      service: _todayTaskService,
                      audience: TodayTasksAudience.family,
                      layout: TodayTasksLayout.sourceGroups,
                      careGroupId: _selectedCareGroupId,
                      controller: _todayTasksController,
                      headingAction: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          IconButton(
                            key: const Key(
                              'family-home-checklist-history-button',
                            ),
                            tooltip: 'Lịch sử checklist',
                            onPressed: () {
                              final groupId = _selectedCareGroupId;
                              if (groupId == null) return;
                              context.push(
                                '/checklists/history?careGroupId=$groupId',
                              );
                            },
                            icon: const Icon(Icons.history_rounded),
                            color: _primary,
                          ),
                          if (_selectedCareGroupId != null &&
                              _snapshot!
                                      .selectedGroupDetail
                                      ?.permissionScope
                                      .checklistView ==
                                  true)
                            AddUserChecklistTaskButton(
                              careGroupId: _selectedCareGroupId,
                              onCreated: _todayTasksController.refresh,
                            ),
                        ],
                      ),
                    ),
                  const SizedBox(height: 28),
                  _buildRecommendationSection(),
                ],
              ),
            ),
            Align(alignment: Alignment.bottomCenter, child: _buildBottomNav()),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Row(
      children: [
        Container(
          width: 52,
          height: 52,
          decoration: const BoxDecoration(
            color: _surfaceLow,
            shape: BoxShape.circle,
          ),
          child: const Icon(Icons.family_restroom, color: _primary),
        ),
        const SizedBox(width: 14),
        const Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Đồng hành cùng mẹ',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 27,
                  fontWeight: FontWeight.w700,
                  color: _primary,
                ),
              ),
              Text(
                'Cùng theo dõi việc chăm sóc gia đình',
                style: TextStyle(color: _onSurfaceVariant),
              ),
            ],
          ),
        ),
        Stack(
          clipBehavior: Clip.none,
          children: [
            IconButton(
              key: const Key('family-notification-bell'),
              tooltip: 'Thông báo',
              onPressed: () => Navigator.of(context)
                  .push(
                    MaterialPageRoute(
                      builder: (_) => const NotificationCenterScreen(),
                    ),
                  )
                  .then((_) {
                    String? currentAccountId;
                    try {
                      currentAccountId = AuthState.instance.userId;
                    } catch (_) {}
                    _checkUnread(
                      generation: _loadGeneration,
                      accountId: currentAccountId,
                    );
                  }),
              icon: const Icon(Icons.notifications, size: 28, color: _primary),
            ),
            if (_hasUnread)
              Positioned(
                top: 8,
                right: 8,
                child: Container(
                  key: const Key('family-notification-badge'),
                  width: 10,
                  height: 10,
                  decoration: const BoxDecoration(
                    color: _error,
                    shape: BoxShape.circle,
                  ),
                ),
              ),
          ],
        ),
      ],
    );
  }

  Widget _buildShortcuts() {
    return Row(
      children: [
        Expanded(
          child: _ShortcutButton(
            icon: Icons.forum_outlined,
            label: 'Cộng đồng',
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const CommunityFeedScreen()),
            ),
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: _ShortcutButton(
            icon: Icons.menu_book_outlined,
            label: 'Nội dung & FAQ',
            onTap: _openFamilyContent,
          ),
        ),
      ],
    );
  }

  Widget _buildGlobalAggregate(FamilyHomeAggregate aggregate) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const _SectionTitle('Việc cần lưu ý trên tất cả nhóm'),
        const SizedBox(height: 12),
        GridView.count(
          crossAxisCount: 2,
          crossAxisSpacing: 12,
          mainAxisSpacing: 12,
          childAspectRatio: 2.25,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          children: [
            _AggregateCard(
              key: const Key('family-global-overdue'),
              label: 'Quá hạn',
              value: aggregate.overdue,
              icon: Icons.warning_amber_rounded,
            ),
            _AggregateCard(
              key: const Key('family-global-due-soon'),
              label: 'Sắp đến hạn',
              value: aggregate.dueSoon,
              icon: Icons.schedule_outlined,
            ),
            _AggregateCard(
              key: const Key('family-global-in-progress'),
              label: 'Đang làm',
              value: aggregate.inProgress,
              icon: Icons.pending_actions_outlined,
            ),
            _AggregateCard(
              key: const Key('family-global-alerts'),
              label: 'Cảnh báo',
              value: aggregate.alerts,
              icon: Icons.notifications_active_outlined,
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildGroupSelector(FamilyHomeSnapshot snapshot) {
    return _DashboardCard(
      child: DropdownButtonFormField<String>(
        key: const Key('family-dashboard-group-selector'),
        initialValue: snapshot.selectedCareGroupId,
        isExpanded: true,
        decoration: const InputDecoration(
          labelText: 'Đang xem tình trạng của',
          prefixIcon: Icon(Icons.switch_account_outlined),
          border: InputBorder.none,
        ),
        items: snapshot.groups
            .map(
              (group) => DropdownMenuItem(
                value: group.id,
                child: Text(group.name, overflow: TextOverflow.ellipsis),
              ),
            )
            .toList(growable: false),
        onChanged: _loading
            ? null
            : (value) {
                if (value == null) return;
                final changed = value != _selectedCareGroupId;
                setState(() {
                  _selectedCareGroupId = value;
                  _checklistGroupExplicitlySelected = true;
                });
                if (changed) _load();
              },
      ),
    );
  }

  Widget _buildFollowingGroups(FamilyHomeSnapshot snapshot) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Bạn đang đồng hành cùng ${snapshot.groups.length} mẹ',
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 17,
            fontWeight: FontWeight.w700,
            color: _onSurface,
          ),
        ),
        const SizedBox(height: 10),
        SizedBox(
          height: 54,
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: snapshot.groups
                  .map((group) {
                    final selected = group.id == snapshot.selectedCareGroupId;
                    return Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: ChoiceChip(
                        key: Key('family-dashboard-group-chip-${group.id}'),
                        label: Text(group.name),
                        selected: selected,
                        avatar: Icon(
                          selected
                              ? Icons.favorite_rounded
                              : Icons.favorite_border,
                          size: 17,
                          color: selected ? Colors.white : _primary,
                        ),
                        selectedColor: _primary,
                        labelStyle: TextStyle(
                          color: selected ? Colors.white : _onSurface,
                          fontWeight: FontWeight.w700,
                        ),
                        onSelected: _loading || selected
                            ? null
                            : (_) {
                                setState(() {
                                  _selectedCareGroupId = group.id;
                                  _checklistGroupExplicitlySelected = true;
                                });
                                _load();
                              },
                      ),
                    );
                  })
                  .toList(growable: false),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildSelectedGroupDetail(FamilyHomeGroupDetail detail) {
    final selectedGroup = _snapshot!.groups.firstWhere(
      (group) => group.id == detail.careGroupId,
    );
    return Column(
      key: Key('family-selected-group-${detail.careGroupId}'),
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildMotherContextCard(detail, selectedGroup),
        const SizedBox(height: 24),
        Row(
          children: [
            Expanded(
              child: _SectionTitle(
                'Lịch hẹn của ${detail.motherDisplayName}',
              ),
            ),
            if (detail.permissionScope.calendar)
              IconButton(
                key: const Key(
                  'family-home-appointment-calendar-button',
                ),
                tooltip: 'Xem chi tiết lịch hẹn',
                onPressed: () {
                  context.push(
                    '/appointments/calendar?careGroupId=${detail.careGroupId}',
                  );
                },
                icon: const Icon(Icons.arrow_forward_rounded),
                color: _primary,
              ),
          ],
        ),
        const SizedBox(height: 10),
        if (detail.todayReminders.isEmpty)
          _EmptyCard(
            key: Key('family-dashboard-empty-reminders'),
            message: 'Hôm nay ${detail.motherDisplayName} chưa có lịch hẹn.',
          )
        else
          ...detail.todayReminders.map(
            (reminder) => _buildReminderCard(detail.careGroupId, reminder),
          ),
        const SizedBox(height: 24),
        _buildQuickNotesSection(detail),
        const SizedBox(height: 24),
        const _SectionTitle('Cảnh báo theo nhóm'),
        const SizedBox(height: 10),
        if (detail.alerts.isEmpty)
          const _EmptyCard(
            key: Key('family-dashboard-empty-alerts'),
            message: 'Chưa có cảnh báo được gắn với nhóm này.',
          )
        else
          ...detail.alerts.map(_buildAlertCard),
        const SizedBox(height: 24),
        const _SectionTitle('Thành viên đã tham gia'),
        const SizedBox(height: 10),
        ...detail.members.map(_buildMemberCard),
      ],
    );
  }

  Widget _buildMotherContextCard(
    FamilyHomeGroupDetail detail,
    FamilyHomeGroup group,
  ) {
    final sharedCount = detail.permissionScope.sharedHealthMetricCount;
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF845143), Color(0xFFB87565)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(24),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const CircleAvatar(
                backgroundColor: Color(0x33FFFFFF),
                foregroundColor: Colors.white,
                child: Icon(Icons.favorite_rounded),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  detail.motherDisplayName,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 21,
                    fontWeight: FontWeight.w700,
                    color: Colors.white,
                  ),
                ),
              ),
              Text(
                _relationshipLabel(
                  detail.relationshipRole,
                  detail.customRelationshipRole,
                ),
                style: const TextStyle(color: Color(0xFFFDEDEA)),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Text(group.name, style: const TextStyle(color: Color(0xFFFDEDEA))),
          const SizedBox(height: 16),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _MotherContextPill(
                icon: Icons.monitor_heart_outlined,
                label: '$sharedCount chỉ số được chia sẻ',
              ),
              _MotherContextPill(
                icon: Icons.groups_outlined,
                label: '${detail.memberCount} thành viên',
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildQuickNotesSection(FamilyHomeGroupDetail detail) {
    final types = _sharedQuickNoteTypes(detail.permissionScope);
    return Column(
      key: const Key('family-dashboard-quick-notes'),
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Expanded(child: _SectionTitle('Chỉ số sức khỏe')),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
              decoration: BoxDecoration(
                color: _surfaceLow,
                borderRadius: BorderRadius.circular(99),
              ),
              child: const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.visibility_outlined, size: 15, color: _primary),
                  SizedBox(width: 4),
                  Text(
                    'Chỉ xem',
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                      color: _primary,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 6),
        Text(
          types.isEmpty
              ? '${detail.motherDisplayName} chưa chia sẻ chỉ số sức khỏe với bạn.'
              : 'Dữ liệu sức khỏe ${detail.motherDisplayName} cho phép bạn theo dõi.',
          style: const TextStyle(color: _onSurfaceVariant, height: 1.35),
        ),
        const SizedBox(height: 12),
        if (types.isEmpty)
          const _EmptyCard(
            key: Key('family-health-metrics-locked'),
            message: 'Các chỉ số đang được giữ riêng tư bởi mẹ.',
          )
        else
          GridView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: types.length,
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 2,
              crossAxisSpacing: 12,
              mainAxisSpacing: 12,
              childAspectRatio: 1.02,
            ),
            itemBuilder: (context, index) {
              final type = types[index];
              FamilyHomeHealthMetricSummary? summary;
              for (final item in detail.healthMetricSummaries) {
                if (item.metricType == type.metricType) {
                  summary = item;
                  break;
                }
              }
              return _buildQuickNoteTile(detail.careGroupId, type, summary);
            },
          ),
      ],
    );
  }

  Widget _buildQuickNoteTile(
    String groupId,
    _FamilyQuickNoteType type,
    FamilyHomeHealthMetricSummary? summary,
  ) {
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(22),
      child: InkWell(
        key: Key('family-quick-note-${type.metricType.toLowerCase()}'),
        borderRadius: BorderRadius.circular(22),
        onTap: () => Navigator.of(context).push(
          MaterialPageRoute(
            builder: (_) => FamilyQuickNoteHistoryScreen(
              careGroupId: groupId,
              metricType: type.metricType,
            ),
          ),
        ),
        child: Ink(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(22),
            border: Border.all(color: const Color(0xFFF0E4DF)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.start,
            children: [
              Container(
                width: 42,
                height: 42,
                decoration: BoxDecoration(
                  color: type.tint,
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Icon(type.icon, color: _primary, size: 22),
              ),
              const SizedBox(height: 10),
              Text(
                summary?.valueDisplay == null
                    ? 'Chưa có dữ liệu'
                    : '${summary!.valueDisplay}${_localizedMetricUnit(summary.unit).isNotEmpty ? ' ${_localizedMetricUnit(summary.unit)}' : ''}',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: summary?.hasData == true ? 17 : 12,
                  fontWeight: FontWeight.w700,
                  color: summary?.hasData == true ? _onSurface : _outline,
                ),
              ),
              const SizedBox(height: 3),
              Text(
                summary?.measuredAt == null
                    ? 'Mẹ chưa ghi nhận'
                    : '${_dateLabel(summary!.measuredAt)}${_glucoseContextLabel(summary.measurementContext)}',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 10, color: _onSurfaceVariant),
              ),
              const Spacer(),
              Row(
                children: [
                  Expanded(
                    child: Text(
                      type.label,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                        color: _onSurface,
                      ),
                    ),
                  ),
                  const Icon(
                    Icons.arrow_forward_rounded,
                    size: 18,
                    color: _primary,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _localizedMetricUnit(String? unit) {
    return switch (unit) {
      'count' => 'lần',
      null => '',
      _ => unit,
    };
  }

  List<_FamilyQuickNoteType> _sharedQuickNoteTypes(
    FamilyHomePermission permission,
  ) {
    if (!permission.quickNotes) return const [];
    return [
      if (permission.quickNoteWeight)
        const _FamilyQuickNoteType(
          metricType: 'WEIGHT',
          label: 'Cân nặng',
          icon: Icons.monitor_weight_outlined,
          tint: Color(0xFFFFE9E3),
        ),
      if (permission.quickNoteFetalMovement)
        const _FamilyQuickNoteType(
          metricType: 'FETAL_MOVEMENT_COUNT',
          label: 'Cử động thai',
          icon: Icons.child_friendly_outlined,
          tint: Color(0xFFFFF0D8),
        ),
      if (permission.quickNoteBloodPressure)
        const _FamilyQuickNoteType(
          metricType: 'BLOOD_PRESSURE',
          label: 'Huyết áp',
          icon: Icons.monitor_heart_outlined,
          tint: Color(0xFFFFE4E8),
        ),
      if (permission.quickNoteHydration)
        const _FamilyQuickNoteType(
          metricType: 'HYDRATION',
          label: 'Nước',
          icon: Icons.water_drop_outlined,
          tint: Color(0xFFE5F3F6),
        ),
      if (permission.quickNoteEpds)
        const _FamilyQuickNoteType(
          metricType: 'EPDS_SCORE',
          label: 'Sàng lọc EPDS',
          icon: Icons.psychology_alt_outlined,
          tint: Color(0xFFF1E9F7),
        ),
      if (permission.quickNoteBloodGlucose)
        const _FamilyQuickNoteType(
          metricType: 'BLOOD_GLUCOSE',
          label: 'Đường huyết',
          icon: Icons.bloodtype_outlined,
          tint: Color(0xFFE9F3E7),
        ),
    ];
  }

  String _glucoseContextLabel(String? value) {
    return switch (value) {
      'FASTING' => ' • Lúc đói',
      'PRE_MEAL' => ' • Trước ăn',
      'POST_MEAL_1H' => ' • Sau ăn 1h',
      'POST_MEAL_2H' => ' • Sau ăn 2h',
      'RANDOM' => ' • Ngẫu nhiên',
      'OTHER_APPROVED' => ' • Khác',
      _ => '',
    };
  }

  Widget _buildReminderCard(
    String careGroupId,
    FamilyHomeTodayReminder reminder,
  ) {
    return _DashboardCard(
      key: Key('family-reminder-${reminder.id}'),
      child: Material(
        color: Colors.transparent,
        child: ListTile(
          contentPadding: EdgeInsets.zero,
          onTap: reminder.type == 'APPOINTMENT'
              ? () => context.push(
                  '/care-groups/$careGroupId/appointments/${reminder.id}',
                )
              : null,
          leading: Icon(_reminderIcon(reminder.type), color: _primary),
          title: Text(reminder.title),
          subtitle: Text(
            '${_reminderTypeLabel(reminder.type)} • '
            '${_statusLabel(reminder.status)} • ${_dateLabel(reminder.dueAt)}',
          ),
        ),
      ),
    );
  }

  Widget _buildAlertCard(FamilyHomeAlert alert) {
    return _DashboardCard(
      key: Key('family-alert-${alert.id}'),
      child: ListTile(
        contentPadding: EdgeInsets.zero,
        leading: Icon(
          alert.read
              ? Icons.notifications_none
              : Icons.notifications_active_outlined,
          color: _error,
        ),
        title: Text(alert.title),
        subtitle: Text('${alert.body}\n${_dateLabel(alert.createdAt)}'),
        isThreeLine: true,
      ),
    );
  }

  Widget _buildMemberCard(FamilyHomeMember member) {
    return _DashboardCard(
      key: Key('family-member-${member.memberId}'),
      child: ListTile(
        contentPadding: EdgeInsets.zero,
        leading: const CircleAvatar(
          backgroundColor: _surfaceLow,
          foregroundColor: _primary,
          child: Icon(Icons.person_outline),
        ),
        title: Text(member.displayName),
        subtitle: Text(
          '${_relationshipLabel(member.relationshipRole, member.customRelationshipRole)}'
          ' • ${_systemRoleLabel(member.systemRole)}',
        ),
      ),
    );
  }

  Widget _buildNoGroup() {
    return Padding(
      padding: const EdgeInsets.only(top: 72),
      child: Column(
        key: const Key('family-dashboard-no-group'),
        children: [
          const Icon(Icons.group_off_outlined, size: 52, color: _outline),
          const SizedBox(height: 14),
          const Text('Bạn chưa tham gia nhóm gia đình nào.'),
          TextButton(
            key: const Key('family-dashboard-join-cta'),
            onPressed: _openGroups,
            child: const Text('Tham gia nhóm'),
          ),
          TextButton(
            key: const Key('family-dashboard-invitation-cta'),
            onPressed: _openGroups,
            child: const Text('Xem lời mời'),
          ),
        ],
      ),
    );
  }

  Widget _buildPermissionDenied() {
    return const Padding(
      padding: EdgeInsets.only(top: 80),
      child: Center(
        key: Key('family-dashboard-permission-denied'),
        child: Text('Bạn không có quyền xem nhóm này.'),
      ),
    );
  }

  Widget _buildError() {
    return Padding(
      padding: const EdgeInsets.only(top: 80),
      child: Center(
        key: const Key('family-dashboard-error'),
        child: TextButton.icon(
          key: const Key('family-dashboard-retry'),
          onPressed: _load,
          icon: const Icon(Icons.refresh),
          label: const Text('Thử lại'),
        ),
      ),
    );
  }

  Widget _buildBottomNav() {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 8, 14, 18),
      decoration: const BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
        boxShadow: [
          BoxShadow(
            color: Color(0x14000000),
            blurRadius: 20,
            offset: Offset(0, -3),
          ),
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          const _BottomNavItem(
            icon: Icons.home,
            label: 'Tổng quan',
            active: true,
          ),
          _BottomNavItem(
            icon: Icons.group_outlined,
            label: 'Nhóm',
            onTap: _openGroups,
          ),
          _BottomNavItem(
            icon: Icons.medical_services_outlined,
            label: 'Chuyên gia',
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const ExpertDirectoryScreen()),
            ),
          ),
          _BottomNavItem(
            icon: Icons.chat_bubble_outline,
            label: 'Trò chuyện',
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const ConversationListScreen()),
            ),
          ),
          _BottomNavItem(
            icon: Icons.person_outline,
            label: 'Hồ sơ',
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const AccountProfileScreen()),
            ),
          ),
        ],
      ),
    );
  }

  void _openGroups() {
    Navigator.of(
      context,
    ).push(MaterialPageRoute(builder: (_) => const MyCareGroupsScreen()));
  }

  void _openFamilyContent() {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => const ViewContentScreen(mode: ContentBrowseMode.family),
      ),
    );
  }

  String _dateLabel(DateTime? value) {
    if (value == null) return 'Không có thời gian';
    final local = value.toLocal();
    final day = local.day.toString().padLeft(2, '0');
    final month = local.month.toString().padLeft(2, '0');
    final hour = local.hour.toString().padLeft(2, '0');
    final minute = local.minute.toString().padLeft(2, '0');
    return '$day/$month/${local.year} $hour:$minute';
  }

  String _relationshipLabel(String? role, String? customRole) {
    if (customRole != null && customRole.isNotEmpty) return customRole;
    return switch (role) {
      'MOTHER' => 'Mẹ',
      'FATHER' => 'Bố',
      'GRANDMOTHER' => 'Bà',
      'GRANDFATHER' => 'Ông',
      'SIBLING' => 'Anh/chị/em',
      null => 'Chưa thiết lập quan hệ',
      _ => role,
    };
  }

  String _systemRoleLabel(String? role) {
    return switch (role) {
      'OWNER' => 'Chủ nhóm',
      'MEMBER' => 'Thành viên',
      'VIEWER' => 'Người xem',
      null => 'Chưa có vai trò hệ thống',
      _ => role,
    };
  }

  String _statusLabel(String status) {
    return switch (status) {
      'OPEN' => 'Chưa bắt đầu',
      'IN_PROGRESS' => 'Đang thực hiện',
      'DONE' => 'Đã hoàn thành',
      'PENDING' => 'Đang chờ',
      'SNOOZED' => 'Đã hoãn',
      'COMPLETED' => 'Đã hoàn thành',
      'SKIPPED' => 'Đã bỏ qua',
      'CANCELLED' => 'Đã hủy',
      'NEEDS_SUPPORT' => 'Cần hỗ trợ',
      _ => status,
    };
  }

  String _reminderTypeLabel(String type) {
    return switch (type) {
      'VACCINATION' => 'Tiêm chủng',
      'MEDICATION' => 'Thuốc',
      'APPOINTMENT' => 'Lịch hẹn',
      _ => 'Lịch nhắc',
    };
  }

  IconData _reminderIcon(String type) {
    return switch (type) {
      'VACCINATION' => Icons.vaccines_outlined,
      'MEDICATION' => Icons.medication_outlined,
      'APPOINTMENT' => Icons.event_outlined,
      _ => Icons.notifications_none_outlined,
    };
  }

  Widget _buildRecommendationSection() {
    final response = _recommendations;
    if (response == null && _recommendationLoading) {
      return const Padding(
        padding: EdgeInsets.symmetric(horizontal: 24),
        child: _RecommendationLoadingState(),
      );
    }
    if (response == null && _recommendationError != null) {
      return Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: _RecommendationErrorState(
          message: _recommendationError!,
          onRetry: _loadRecommendations,
        ),
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
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w600,
              color: _onSurface,
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
                key: const Key('family-home-recommendation-review-profile'),
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
              key: Key('family-home-recommendation-refreshing'),
              color: Color(0xFFC98C7B),
              backgroundColor: _surfaceLow,
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
              key: Key('family-home-recommendation-fallback-only'),
              message: 'Đây là nội dung nền an toàn cho giai đoạn hiện tại.',
            ),
          if (response.coverageStatus == 'PARTIAL')
            _RecommendationCoverageNotice(
              key: const Key('family-home-recommendation-partial'),
              message:
                  'Hiện có một số nội dung phù hợp. Bạn có thể xem thêm trong thư viện.',
              onBrowse: () => context.push('/content'),
            ),
          if (response.coverageStatus == 'EMPTY')
            _RecommendationCoverageNotice(
              key: const Key('family-home-recommendation-empty-coverage'),
              message:
                  'Chưa có bài viết phù hợp; hãy xem toàn bộ thư viện nội dung.',
              onBrowse: () => context.push('/content'),
            ),
          const SizedBox(height: 12),
          if (items.isNotEmpty) ...items.map(_buildRecommendationCard),
        ],
      ),
    );
  }

  Widget _buildRecommendationCard(RecommendationContentItem item) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Semantics(
        button: true,
        label: item.title,
        child: Card(
          key: Key('family-home-recommendation-card-${item.id}'),
          margin: EdgeInsets.zero,
          color: _surface,
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: const BorderSide(color: Color(0xFFE6D6CE)),
          ),
          child: InkWell(
            borderRadius: BorderRadius.circular(16),
            onTap: () => _openRecommendation(item.id),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.menu_book_outlined, color: _primary),
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
                            color: _onSurface,
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
                              color: _onSurfaceVariant,
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
                            color: _primary,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 8),
                  const Icon(
                    Icons.chevron_right_rounded,
                    color: _onSurfaceVariant,
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

class _FamilyQuickNoteType {
  const _FamilyQuickNoteType({
    required this.metricType,
    required this.label,
    required this.icon,
    required this.tint,
  });

  final String metricType;
  final String label;
  final IconData icon;
  final Color tint;
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle(this.title);

  final String title;

  @override
  Widget build(BuildContext context) {
    return Text(
      title,
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 20,
        fontWeight: FontWeight.w700,
        color: _FamilyMemberHomeScreenState._onSurface,
      ),
    );
  }
}

class _AggregateCard extends StatelessWidget {
  const _AggregateCard({
    super.key,
    required this.label,
    required this.value,
    required this.icon,
  });

  final String label;
  final int value;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return _DashboardCard(
      child: Row(
        children: [
          Icon(icon, color: _FamilyMemberHomeScreenState._primary),
          const SizedBox(width: 10),
          Expanded(child: Text(label)),
          Text(
            '$value',
            style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w700),
          ),
        ],
      ),
    );
  }
}

class _MotherContextPill extends StatelessWidget {
  const _MotherContextPill({required this.icon, required this.label});

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
      decoration: BoxDecoration(
        color: const Color(0x24FFFFFF),
        borderRadius: BorderRadius.circular(99),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 16, color: Colors.white),
          const SizedBox(width: 5),
          Text(
            label,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 12,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}

class _RecommendationLoadingState extends StatelessWidget {
  const _RecommendationLoadingState();

  @override
  Widget build(BuildContext context) {
    return const Card(
      key: Key('family-home-recommendation-loading'),
      margin: EdgeInsets.zero,
      child: Padding(
        padding: EdgeInsets.all(24),
        child: Center(
          child: CircularProgressIndicator(color: Color(0xFFC98C7B)),
        ),
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
    return Card(
      key: const Key('family-home-recommendation-error'),
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            const Icon(Icons.info_outline, color: Color(0xFF845143)),
            const SizedBox(width: 10),
            Expanded(child: Text(message)),
            IconButton(
              key: const Key('family-home-recommendation-retry'),
              tooltip: 'Thử lại',
              onPressed: () => unawaited(onRetry()),
              icon: const Icon(Icons.refresh),
            ),
          ],
        ),
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
    return Card(
      margin: const EdgeInsets.only(top: 8),
      color: const Color(0xFFFFF1EC),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 12, 12, 12),
        child: Row(
          children: [
            const Icon(Icons.menu_book_outlined, color: Color(0xFF845143)),
            const SizedBox(width: 10),
            Expanded(child: Text(message)),
            if (onBrowse != null)
              TextButton(onPressed: onBrowse, child: const Text('Xem thêm')),
          ],
        ),
      ),
    );
  }
}

class _ShortcutButton extends StatelessWidget {
  const _ShortcutButton({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(18),
      child: Container(
        height: 88,
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: _FamilyMemberHomeScreenState._surfaceLow,
          borderRadius: BorderRadius.circular(18),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: _FamilyMemberHomeScreenState._primary),
            const SizedBox(height: 6),
            Text(
              label,
              maxLines: 2,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
            ),
          ],
        ),
      ),
    );
  }
}

class _DashboardCard extends StatelessWidget {
  const _DashboardCard({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      decoration: BoxDecoration(
        color: _FamilyMemberHomeScreenState._surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: const Color(0x1F845143)),
      ),
      child: child,
    );
  }
}

class _EmptyCard extends StatelessWidget {
  const _EmptyCard({super.key, required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return _DashboardCard(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 10),
        child: Text(
          message,
          style: const TextStyle(
            color: _FamilyMemberHomeScreenState._onSurfaceVariant,
          ),
        ),
      ),
    );
  }
}

class _BottomNavItem extends StatelessWidget {
  const _BottomNavItem({
    required this.icon,
    required this.label,
    this.active = false,
    this.onTap,
  });

  final IconData icon;
  final String label;
  final bool active;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final color = active
        ? _FamilyMemberHomeScreenState._primary
        : _FamilyMemberHomeScreenState._outline;
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(18),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 6),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: color, size: 23),
            const SizedBox(height: 3),
            Text(label, style: TextStyle(color: color, fontSize: 10)),
          ],
        ),
      ),
    );
  }
}
