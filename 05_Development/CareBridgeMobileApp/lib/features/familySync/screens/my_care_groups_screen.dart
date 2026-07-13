import 'package:flutter/material.dart';
import '../models/care_group_model.dart';
import '../services/care_group_service.dart';
import 'care_group_detail_screen.dart';
import 'pending_invitations_screen.dart';

/// CB-026 — My Care Groups (Family member perspective, UC-83, UC-84, UC-72, UC-216)
/// Shows groups this user (family member / co-carer) belongs to.
/// Role badge, permission type, detail + leave buttons.
/// Data: mock list (TODO: wire to GET /api/v1/care-groups/mine when endpoint available).
class MyCareGroupsScreen extends StatefulWidget {
  const MyCareGroupsScreen({super.key});

  @override
  State<MyCareGroupsScreen> createState() => _MyCareGroupsScreenState();
}

class _MyCareGroupsScreenState extends State<MyCareGroupsScreen> {
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _error = Color(0xFFBA1A1A);

  final _service = CareGroupService();
  List<CareGroup> _groups = [];
  int _pendingInviteCount = 0;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final list = await _service.listMyGroups();
      final invites = await _service.listMyInvitations();
      if (mounted) {
        setState(() {
          _groups = list;
          _pendingInviteCount = invites.length;
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _openInvitations() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const PendingInvitationsScreen()),
    ).then((_) => _load());
  }

  Future<void> _leaveGroup(CareGroup g) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text(
          'Rời nhóm?',
          style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600),
        ),
        content: Text(
          'Bạn sẽ không còn xem được thông tin nhóm "${g.groupName}".',
          style: const TextStyle(fontFamily: 'Lexend'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy', style: TextStyle(fontFamily: 'Lexend')),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: _error),
            child: const Text(
              'Rời nhóm',
              style: TextStyle(fontFamily: 'Lexend'),
            ),
          ),
        ],
      ),
    );
    if (ok != true) return;
    try {
      await _service.leaveGroup(g.id);
      _load();
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không thể rời nhóm. Vui lòng thử lại.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(),
            Expanded(
              child: _loading
                  ? const Center(
                      child: CircularProgressIndicator(
                        color: _primaryContainer,
                      ),
                    )
                  : RefreshIndicator(
                      color: _primaryContainer,
                      onRefresh: _load,
                      child: _groups.isEmpty ? _buildEmpty() : _buildList(),
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Container(
      padding: const EdgeInsets.fromLTRB(20, 20, 20, 16),
      child: Column(
        children: [
          Row(
            children: [
              IconButton(
                onPressed: () => Navigator.pop(context),
                icon: const Icon(Icons.arrow_back, color: _onSurfaceVariant),
              ),
              const Spacer(),
              IconButton(
                onPressed: _openInvitations,
                icon: Badge(
                  label: Text('$_pendingInviteCount'),
                  isLabelVisible: _pendingInviteCount > 0,
                  child: const Icon(
                    Icons.mail_outline,
                    color: _onSurfaceVariant,
                  ),
                ),
              ),
            ],
          ),
          // App logo area
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(
              color: _primaryContainer,
              borderRadius: BorderRadius.circular(16),
            ),
            child: const Icon(Icons.favorite, color: Colors.white, size: 28),
          ),
          const SizedBox(height: 8),
          const Text(
            'CareBridge',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 18,
              fontWeight: FontWeight.w700,
              color: Color(0xFF271812),
            ),
          ),
          const SizedBox(height: 4),
          const Text(
            'Nhóm chăm sóc của tôi',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: Color(0xFF524440),
            ),
          ),
          const SizedBox(height: 16),
          // Invite hint
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(12),
              border: Border.all(
                color: _primaryContainer.withAlpha(102),
                style: BorderStyle.solid,
              ),
            ),
            child: Row(
              children: [
                const Icon(Icons.mail_outline, color: _primaryContainer),
                const SizedBox(width: 8),
                const Expanded(
                  child: Text(
                    'Nhập mã nhóm để tham gia nhóm mới',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      color: Color(0xFF524440),
                    ),
                  ),
                ),
                TextButton(
                  onPressed: () {},
                  child: const Text(
                    'Tham gia',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      color: Color(0xFFC98C7B),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildList() {
    return ListView.separated(
      padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
      itemCount: _groups.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (_, i) => _GroupCard(
        group: _groups[i],
        onDetail: () => Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => CareGroupDetailScreen(
              groupId: _groups[i].id,
              groupName: _groups[i].groupName,
            ),
          ),
        ),
        onLeave: () => _leaveGroup(_groups[i]),
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.group_off, size: 64, color: Color(0xFFC98C7B)),
          const SizedBox(height: 16),
          const Text(
            'Chưa có nhóm nào',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w600,
              color: Color(0xFF271812),
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Nhập mã nhóm để tham gia',
            style: TextStyle(fontFamily: 'Lexend', color: Color(0xFF524440)),
          ),
        ],
      ),
    );
  }
}

class _GroupCard extends StatelessWidget {
  final CareGroup group;
  final VoidCallback onDetail;
  final VoidCallback onLeave;

  const _GroupCard({
    required this.group,
    required this.onDetail,
    required this.onLeave,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
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
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: const BoxDecoration(
                  color: Color(0xFFFFE9E3),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.group, color: Color(0xFF845143)),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      group.groupName,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 18,
                        fontWeight: FontWeight.w600,
                        color: Color(0xFF271812),
                      ),
                    ),
                    Row(
                      children: [
                        const Icon(
                          Icons.people,
                          size: 14,
                          color: Color(0xFF524440),
                        ),
                        const SizedBox(width: 4),
                        Text(
                          '${group.memberCount} thành viên',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            color: Color(0xFF524440),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              // Role badge
              if (group.myRole != null)
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: group.myRole == 'ADMIN' || group.myRole == 'OWNER'
                        ? const Color(0xFFC98C7B).withAlpha(38)
                        : const Color(0xFFFADCD3).withAlpha(77),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    group.myRole == 'ADMIN' || group.myRole == 'OWNER'
                        ? 'Quản trị'
                        : 'Thành viên',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                      color: group.myRole == 'ADMIN' || group.myRole == 'OWNER'
                          ? const Color(0xFF845143)
                          : const Color(0xFF524440),
                    ),
                  ),
                ),
            ],
          ),
          if (group.myPermission != null) ...[
            const SizedBox(height: 10),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
              decoration: BoxDecoration(
                color: const Color(0xFFFFF1EC),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(
                    Icons.shield_outlined,
                    size: 14,
                    color: Color(0xFF6E5A52),
                  ),
                  const SizedBox(width: 4),
                  Text(
                    group.myPermission!,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 11,
                      color: Color(0xFF6E5A52),
                    ),
                  ),
                ],
              ),
            ),
          ],
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: FilledButton(
                  onPressed: onDetail,
                  style: FilledButton.styleFrom(
                    backgroundColor: const Color(0xFFC98C7B),
                    shape: const StadiumBorder(),
                  ),
                  child: const Text(
                    'Chi tiết',
                    style: TextStyle(fontFamily: 'Lexend'),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: const Color(0xFFFFDAD6).withAlpha(77),
                  shape: BoxShape.circle,
                ),
                child: IconButton(
                  icon: const Icon(
                    Icons.logout,
                    size: 18,
                    color: Color(0xFFBA1A1A),
                  ),
                  onPressed: onLeave,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
