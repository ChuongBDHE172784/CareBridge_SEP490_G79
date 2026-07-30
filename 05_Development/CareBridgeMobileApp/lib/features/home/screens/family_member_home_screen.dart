import 'package:flutter/material.dart';

import '../../../core/network/api_client.dart';
import '../../auth/screens/account_profile_screen.dart';
import '../../community/screens/community_feed_screen.dart';
import '../../community/models/content_model.dart';
import '../../community/screens/view_content_screen.dart';
import '../../directChat/screens/conversation_list_screen.dart';
import '../../directChat/screens/expert_directory_screen.dart';
import '../../familySync/screens/my_care_groups_screen.dart';
import '../../familySync/services/family_home_service.dart';

class FamilyMemberHomeScreen extends StatefulWidget {
  const FamilyMemberHomeScreen({super.key, this.dashboardLoader});

  final Future<FamilyHomeSnapshot> Function({String? selectedCareGroupId})?
  dashboardLoader;

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
  bool _loading = true;
  Object? _loadError;
  bool _permissionDenied = false;
  String? _selectedCareGroupId;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
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
      if (!mounted) return;
      setState(() {
        _snapshot = snapshot;
        _selectedCareGroupId = snapshot.selectedCareGroupId;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _permissionDenied = error is ApiException && error.statusCode == 403;
        _loadError = error;
      });
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
                padding: const EdgeInsets.fromLTRB(20, 16, 20, 124),
                children: [
                  _buildHeader(),
                  const SizedBox(height: 20),
                  _buildShortcuts(),
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
                    _buildGlobalAggregate(_snapshot!.globalAggregate),
                    if (_loading)
                      const Padding(
                        key: Key('family-dashboard-loading'),
                        padding: EdgeInsets.only(top: 16),
                        child: LinearProgressIndicator(color: _primary),
                      ),
                    if (_snapshot!.groups.length > 1) ...[
                      const SizedBox(height: 24),
                      _buildGroupSelector(_snapshot!),
                    ],
                    const SizedBox(height: 24),
                    if (_snapshot!.selectedGroupDetail != null)
                      _buildSelectedGroupDetail(
                        _snapshot!.selectedGroupDetail!,
                      ),
                  ],
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
                'Family Dashboard',
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
        IconButton(
          tooltip: 'Nhóm chăm sóc',
          onPressed: _openGroups,
          icon: const Icon(Icons.group_outlined, color: _primary),
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
        const _SectionTitle('Tổng quan tất cả nhóm'),
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
    return DropdownButtonFormField<String>(
      key: const Key('family-dashboard-group-selector'),
      value: snapshot.selectedCareGroupId,
      decoration: const InputDecoration(
        labelText: 'Nhóm đang xem',
        border: OutlineInputBorder(),
      ),
      items: snapshot.groups
          .map(
            (group) =>
                DropdownMenuItem(value: group.id, child: Text(group.name)),
          )
          .toList(growable: false),
      onChanged: _loading
          ? null
          : (value) {
              if (value == null || value == _selectedCareGroupId) return;
              setState(() => _selectedCareGroupId = value);
              _load();
            },
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
        Text(
          selectedGroup.name,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 24,
            fontWeight: FontWeight.w700,
            color: _onSurface,
          ),
        ),
        const SizedBox(height: 6),
        Text(
          _relationshipLabel(
            detail.relationshipRole,
            detail.customRelationshipRole,
          ),
          style: const TextStyle(color: _onSurfaceVariant),
        ),
        const SizedBox(height: 24),
        _SectionTitle('Lịch nhắc của ${detail.motherDisplayName}'),
        const SizedBox(height: 10),
        if (detail.todayReminders.isEmpty)
          _EmptyCard(
            key: Key('family-dashboard-empty-reminders'),
            message: 'Hôm nay ${detail.motherDisplayName} chưa có lịch nhắc.',
          )
        else
          ...detail.todayReminders.map(_buildReminderCard),
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

  Widget _buildReminderCard(FamilyHomeTodayReminder reminder) {
    return _DashboardCard(
      key: Key('family-reminder-${reminder.id}'),
      child: ListTile(
        contentPadding: EdgeInsets.zero,
        leading: Icon(_reminderIcon(reminder.type), color: _primary),
        title: Text(reminder.title),
        subtitle: Text(
          '${_reminderTypeLabel(reminder.type)} • '
          '${_statusLabel(reminder.status)} • ${_dateLabel(reminder.dueAt)}',
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
