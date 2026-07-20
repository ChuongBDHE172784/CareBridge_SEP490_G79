import 'package:flutter/material.dart';
import '../../../core/auth/auth_state.dart';
import '../models/care_group_model.dart';
import '../services/care_group_service.dart';
import 'manage_family_permission_screen.dart';

/// CB-168 — Care Group Members (UC-216, UC-71, UC-219)
/// Full member list with avatar, role badge, permission chips, delete member option.
/// Navigated to from CareGroupDetailScreen.
class CareGroupMembersScreen extends StatefulWidget {
  final String groupId;
  final String groupName;
  final List<CareGroupMember> members;

  const CareGroupMembersScreen({
    super.key,
    required this.groupId,
    required this.groupName,
    required this.members,
  });

  @override
  State<CareGroupMembersScreen> createState() => _CareGroupMembersScreenState();
}

class _CareGroupMembersScreenState extends State<CareGroupMembersScreen> {
  static const _canvas = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  final _service = CareGroupService();
  late List<CareGroupMember> _members;
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    _members = List.from(widget.members);
    _reloadMembers();
  }

  Future<void> _reloadMembers() async {
    try {
      final g = await _service.getGroupMembers(widget.groupId);
      if (mounted) {
        setState(() {
          _members = g.members;
        });
      }
    } catch (_) {
      // Keep initial list if offline/failed
    }
  }

  Future<void> _removeMember(CareGroupMember member) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text(
          'Xóa thành viên?',
          style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600),
        ),
        content: Text(
          'Bạn có chắc chắn muốn xóa "${member.displayName}" khỏi nhóm chăm sóc? '
          'Thành viên sẽ bị xóa vĩnh viễn khỏi nhóm!',
          style: const TextStyle(fontFamily: 'Lexend'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy', style: TextStyle(fontFamily: 'Lexend')),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: const Color(0xFFBA1A1A)),
            child: const Text('Xóa vĩnh viễn', style: TextStyle(fontFamily: 'Lexend')),
          ),
        ],
      ),
    );

    if (ok != true) return;

    final targetUserId = member.userId ?? member.memberId;
    try {
      setState(() => _loading = true);
      await _service.removeMember(widget.groupId, targetUserId);
      await _reloadMembers();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Đã xóa ${member.displayName} khỏi nhóm.')),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Không thể xóa thành viên: $e')),
      );
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(context),
            if (_loading)
              const LinearProgressIndicator(color: Color(0xFFC98C7B)),
            Expanded(
              child: _members.isEmpty
                  ? const Center(
                      child: Text(
                        'Chưa có thành viên.',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          color: _onSurfaceVariant,
                        ),
                      ),
                    )
                  : ListView.separated(
                      padding: const EdgeInsets.fromLTRB(20, 0, 20, 100),
                      itemCount: _members.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 12),
                      itemBuilder: (_, i) => _MemberCard(
                        member: _members[i],
                        groupId: widget.groupId,
                        onDelete: () => _removeMember(_members[i]),
                        onReload: _reloadMembers,
                      ),
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(4, 16, 4, 16),
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.arrow_back, color: _onSurfaceVariant),
          ),
          Expanded(
            child: Column(
              children: [
                const Text(
                  'Nhóm chăm sóc',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: _onSurfaceVariant,
                  ),
                ),
                Text(
                  widget.groupName,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 20,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.close, color: _onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

class _MemberCard extends StatelessWidget {
  final CareGroupMember member;
  final String groupId;
  final VoidCallback onDelete;
  final VoidCallback onReload;

  const _MemberCard({
    required this.member,
    required this.groupId,
    required this.onDelete,
    required this.onReload,
  });

  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  String _formatDate(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

  @override
  Widget build(BuildContext context) {
    final isAdmin = member.isAdmin;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: isAdmin
            ? Border.all(color: _primaryContainer.withAlpha(102))
            : null,
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withAlpha(13),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          Stack(
            clipBehavior: Clip.none,
            children: [
              Container(
                width: 52,
                height: 52,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: const Color(0xFFFFE9E3),
                  border: Border.all(
                    color: isAdmin
                        ? _primaryContainer
                        : const Color(0xFFFFE2D9),
                    width: isAdmin ? 3 : 2,
                  ),
                ),
                child: Center(
                  child: Text(
                    member.displayName.isNotEmpty ? member.displayName[0] : '?',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 20,
                      fontWeight: FontWeight.w700,
                      color: _primary,
                    ),
                  ),
                ),
              ),
              if (isAdmin)
                Positioned(
                  top: -4,
                  right: -4,
                  child: Container(
                    width: 18,
                    height: 18,
                    decoration: const BoxDecoration(
                      color: _primaryContainer,
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.star,
                      size: 10,
                      color: Colors.white,
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  member.displayName,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
                if (member.joinedAt != null)
                  Text(
                    'Tham gia: ${_formatDate(member.joinedAt!)}',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 11,
                      color: _onSurfaceVariant,
                    ),
                  ),
                const SizedBox(height: 6),
                Row(
                  children: [
                    _PermChip(label: member.roleLabel, isPrimary: isAdmin),
                    const SizedBox(width: 6),
                    _PermChip(
                      label: member.inviteStatus == 'ACCEPTED'
                          ? 'Đã xác nhận'
                          : 'Chờ xác nhận',
                      isPrimary: false,
                    ),
                  ],
                ),
              ],
            ),
          ),
          if (AuthState.instance.role == 'MOTHER' && !isAdmin)
            PopupMenuButton<String>(
              icon: const Icon(Icons.more_vert, color: _onSurfaceVariant),
              onSelected: (val) async {
                if (val == 'manage') {
                  final result = await Navigator.push<bool>(
                    context,
                    MaterialPageRoute(
                      builder: (_) => ManageFamilyPermissionScreen(
                        groupId: groupId,
                        member: member,
                      ),
                    ),
                  );
                  if (result == true) {
                    onReload();
                  }
                } else if (val == 'delete') {
                  onDelete();
                }
              },
              itemBuilder: (_) => const [
                PopupMenuItem(
                  value: 'manage',
                  child: Row(
                    children: [
                      Icon(Icons.security, color: _primary, size: 18),
                      SizedBox(width: 8),
                      Text(
                        'Quản lý quyền hạn',
                        style: TextStyle(fontFamily: 'Lexend', color: _primary),
                      ),
                    ],
                  ),
                ),
                PopupMenuItem(
                  value: 'delete',
                  child: Row(
                    children: [
                      Icon(Icons.delete_outline, color: Color(0xFFBA1A1A), size: 18),
                      SizedBox(width: 8),
                      Text(
                        'Xóa thành viên',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          color: Color(0xFFBA1A1A),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            )
          else
            const SizedBox.shrink(),
        ],
      ),
    );
  }
}

class _PermChip extends StatelessWidget {
  final String label;
  final bool isPrimary;

  const _PermChip({required this.label, required this.isPrimary});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: isPrimary
            ? const Color(0xFFC98C7B).withAlpha(26)
            : const Color(0xFFFFF1EC),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontFamily: 'Lexend',
          fontSize: 11,
          color: isPrimary ? const Color(0xFF845143) : const Color(0xFF524440),
        ),
      ),
    );
  }
}
