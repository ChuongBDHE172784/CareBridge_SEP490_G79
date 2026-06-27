import 'package:flutter/material.dart';
import '../models/care_group_model.dart';
import '../services/care_group_service.dart';
import 'care_group_members_screen.dart';

/// CB-027 — Shared Care Group Detail (UC-83, UC-84, UC-71, UC-73, UC-216)
/// Shows group info, member circles (64x64 with star for owner),
/// bento grid sections (calendar, tasks, shared data, alerts).
/// Calls GET /api/v1/care-groups/{id}/members.
class CareGroupDetailScreen extends StatefulWidget {
  final String groupId;
  final String groupName;

  const CareGroupDetailScreen({super.key, required this.groupId, required this.groupName});

  @override
  State<CareGroupDetailScreen> createState() => _CareGroupDetailScreenState();
}

class _CareGroupDetailScreenState extends State<CareGroupDetailScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _secondary = Color(0xFF6E5A52);
  static const _error = Color(0xFFBA1A1A);

  final _service = CareGroupService();
  CareGroup? _group;
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
      if (mounted) setState(() { _group = g; _loading = false; });
    } catch (_) {
      // Fallback mock
      if (mounted) setState(() {
        _group = CareGroup(
          id: widget.groupId,
          groupName: widget.groupName,
          isActive: true,
          memberCount: 3,
          members: [
            CareGroupMember(memberId: 'm-1', displayName: 'Mẹ Linh', memberRole: 'ADMIN', inviteStatus: 'ACCEPTED', joinedAt: DateTime(2024, 1, 5)),
            CareGroupMember(memberId: 'm-2', displayName: 'Bố Tuấn', memberRole: 'MEMBER', inviteStatus: 'ACCEPTED', joinedAt: DateTime(2024, 1, 10)),
            CareGroupMember(memberId: 'm-3', displayName: 'Bà Ngoại', memberRole: 'MEMBER', inviteStatus: 'ACCEPTED', joinedAt: DateTime(2024, 2, 3)),
          ],
        );
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: _loading
            ? const Center(child: CircularProgressIndicator(color: _primaryContainer))
            : CustomScrollView(
                slivers: [
                  SliverToBoxAdapter(child: _buildAppBar()),
                  SliverToBoxAdapter(child: _buildMemberSection()),
                  SliverPadding(
                    padding: const EdgeInsets.fromLTRB(20, 0, 20, 32),
                    sliver: SliverToBoxAdapter(child: _buildBentoGrid()),
                  ),
                ],
              ),
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
              style: const TextStyle(fontFamily: 'Lexend', fontSize: 20, fontWeight: FontWeight.w600, color: _onSurface),
              overflow: TextOverflow.ellipsis,
            ),
          ),
          PopupMenuButton<String>(
            icon: const Icon(Icons.more_vert, color: _onSurfaceVariant),
            onSelected: (v) {
              if (v == 'leave') {
                // TODO: leave group (UC-72)
              }
            },
            itemBuilder: (_) => [
              const PopupMenuItem(value: 'leave', child: Row(
                children: [
                  Icon(Icons.logout, color: Color(0xFFBA1A1A), size: 18),
                  SizedBox(width: 8),
                  Text('Rời nhóm', style: TextStyle(fontFamily: 'Lexend', color: Color(0xFFBA1A1A))),
                ],
              )),
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
              const Text('Thành viên',
                  style: TextStyle(fontFamily: 'Lexend', fontSize: 20, fontWeight: FontWeight.w600, color: _onSurface)),
              TextButton.icon(
                onPressed: () => Navigator.push(context, MaterialPageRoute(
                  builder: (_) => CareGroupMembersScreen(groupId: widget.groupId, groupName: widget.groupName, members: members),
                )),
                icon: const Icon(Icons.arrow_forward, size: 16, color: _primaryContainer),
                label: const Text('Xem tất cả', style: TextStyle(fontFamily: 'Lexend', color: _primaryContainer)),
              ),
            ],
          ),
          const SizedBox(height: 12),
          SizedBox(
            height: 88,
            child: ListView(
              scrollDirection: Axis.horizontal,
              children: [
                ...members.map((m) => Padding(
                  padding: const EdgeInsets.only(right: 16),
                  child: _MemberCircle(member: m),
                )),
                // Invite button
                GestureDetector(
                  onTap: () {},
                  child: Column(
                    children: [
                      Container(
                        width: 64, height: 64,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          border: Border.all(color: _primaryContainer.withAlpha(102), width: 2),
                        ),
                        child: const Icon(Icons.person_add, color: _primaryContainer, size: 28),
                      ),
                      const SizedBox(height: 6),
                      const Text('Mời', style: TextStyle(fontFamily: 'Lexend', fontSize: 12, color: Color(0xFF524440))),
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
    return Column(
      children: [
        Row(
          children: [
            Expanded(child: _BentoCard(icon: Icons.calendar_month, label: 'Lịch chung', value: '3 lịch tới', onTap: () {})),
            const SizedBox(width: 12),
            Expanded(child: _BentoCard(icon: Icons.task_alt, label: 'Việc nhóm', value: '2 cần làm', onTap: () {})),
          ],
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(child: _BentoCard(icon: Icons.folder_shared, label: 'Dữ liệu chia sẻ', value: 'Xem hồ sơ', onTap: () {})),
            const SizedBox(width: 12),
            Expanded(child: _BentoCard(icon: Icons.notifications, label: 'Thông báo', value: '1 cảnh báo', onTap: () {}, isAlert: true)),
          ],
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
              width: 64, height: 64,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: const Color(0xFFFFE9E3),
                border: Border.all(
                  color: isAdmin ? const Color(0xFFC98C7B) : const Color(0xFFFFE2D9),
                  width: isAdmin ? 3 : 2,
                ),
              ),
              child: Center(
                child: Text(
                  member.displayName.isNotEmpty ? member.displayName[0] : '?',
                  style: const TextStyle(fontFamily: 'Lexend', fontSize: 24, fontWeight: FontWeight.w700, color: Color(0xFF845143)),
                ),
              ),
            ),
            if (isAdmin)
              Positioned(
                top: -4, right: -4,
                child: Container(
                  width: 20, height: 20,
                  decoration: const BoxDecoration(color: Color(0xFFC98C7B), shape: BoxShape.circle),
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
            style: const TextStyle(fontFamily: 'Lexend', fontSize: 11, color: Color(0xFF524440)),
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
  final bool isAlert;

  const _BentoCard({required this.icon, required this.label, required this.value, required this.onTap, this.isAlert = false});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: isAlert ? const Color(0xFFFFDAD6).withAlpha(38) : Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: isAlert ? Border.all(color: const Color(0xFFBA1A1A).withAlpha(51)) : null,
          boxShadow: [BoxShadow(color: const Color(0xFF5A463F).withAlpha(13), blurRadius: 12, offset: const Offset(0, 4))],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: isAlert ? const Color(0xFFBA1A1A) : const Color(0xFF845143), size: 24),
            const SizedBox(height: 8),
            Text(label,
                style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, color: Color(0xFF524440))),
            const SizedBox(height: 4),
            Text(value,
                style: TextStyle(
                  fontFamily: 'Lexend', fontSize: 18, fontWeight: FontWeight.w600,
                  color: isAlert ? const Color(0xFFBA1A1A) : const Color(0xFF271812),
                )),
          ],
        ),
      ),
    );
  }
}
