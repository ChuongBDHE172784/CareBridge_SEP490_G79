import 'package:flutter/material.dart';
import '../models/care_group_model.dart';
import '../services/care_group_service.dart';
import 'care_group_detail_screen.dart';

/// CB-021 — Care Groups (Mother perspective, UC-69, UC-70, UC-71, UC-72, UC-73, UC-83, UC-216)
/// Mother's view of care groups she manages: active group highlighted, member avatars,
/// inactive groups, FAB to create, buttons to manage.
/// Data: mock list (TODO: wire to GET /api/v1/care-groups when endpoint available).
class CareGroupsScreen extends StatefulWidget {
  const CareGroupsScreen({super.key});

  @override
  State<CareGroupsScreen> createState() => _CareGroupsScreenState();
}

class _CareGroupsScreenState extends State<CareGroupsScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _secondary = Color(0xFF6E5A52);
  static const _outlineVariant = Color(0xFF84736F);

  final _service = CareGroupService();
  List<CareGroup> _groups = [];
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
      if (mounted) setState(() { _groups = list; _loading = false; });
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _createGroup() async {
    final nameCtrl = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text('Tạo nhóm mới', style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600)),
        content: TextField(
          controller: nameCtrl,
          decoration: const InputDecoration(
            labelText: 'Tên nhóm chăm sóc',
            border: OutlineInputBorder(),
          ),
          style: const TextStyle(fontFamily: 'Lexend'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false),
              child: const Text('Hủy', style: TextStyle(fontFamily: 'Lexend'))),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: _primaryContainer),
            child: const Text('Tạo', style: TextStyle(fontFamily: 'Lexend')),
          ),
        ],
      ),
    );
    if (ok != true) return;
    try {
      await _service.createCareGroup(nameCtrl.text);
      _load();
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Không thể tạo nhóm.')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: _loading
            ? const Center(child: CircularProgressIndicator(color: _primaryContainer))
            : RefreshIndicator(
                color: _primaryContainer,
                onRefresh: _load,
                child: CustomScrollView(
                  slivers: [
                    SliverToBoxAdapter(child: _buildAppBar()),
                    SliverPadding(
                      padding: const EdgeInsets.fromLTRB(20, 0, 20, 100),
                      sliver: SliverList(
                        delegate: SliverChildBuilderDelegate(
                          (_, i) => _buildGroupItem(_groups[i], i),
                          childCount: _groups.length,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: _primaryContainer,
        foregroundColor: Colors.white,
        shape: const StadiumBorder(),
        onPressed: _createGroup,
        icon: const Icon(Icons.group_add),
        label: const Text('Tạo nhóm', style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600)),
      ),
    );
  }

  Widget _buildAppBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 24, 20, 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              IconButton(
                onPressed: () => Navigator.pop(context),
                icon: const Icon(Icons.arrow_back, color: _onSurfaceVariant),
              ),
              const Expanded(child: SizedBox()),
              IconButton(onPressed: () {}, icon: const Icon(Icons.notifications_outlined, color: _onSurfaceVariant)),
            ],
          ),
          const Text('Nhóm chăm sóc',
              style: TextStyle(fontFamily: 'Lexend', fontSize: 24, fontWeight: FontWeight.w700, color: _onSurface)),
          const Text('Quản lý và chia sẻ chăm sóc',
              style: TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant)),
        ],
      ),
    );
  }

  Widget _buildGroupItem(CareGroup group, int idx) {
    if (idx == 0 && group.isActive) {
      return Padding(
        padding: const EdgeInsets.only(bottom: 16),
        child: _ActiveGroupCard(group: group, onTap: () => _openDetail(group)),
      );
    }
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: _SecondaryGroupCard(group: group, onTap: () => _openDetail(group)),
    );
  }

  void _openDetail(CareGroup g) {
    Navigator.push(context, MaterialPageRoute(
      builder: (_) => CareGroupDetailScreen(groupId: g.id, groupName: g.groupName),
    ));
  }
}

// ─── Active group card (glassmorphism style) ──────────────────────────────────
class _ActiveGroupCard extends StatelessWidget {
  final CareGroup group;
  final VoidCallback onTap;

  const _ActiveGroupCard({required this.group, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFFC98C7B), Color(0xFF845143)],
          ),
          borderRadius: BorderRadius.circular(20),
          boxShadow: [BoxShadow(color: const Color(0xFFC98C7B).withAlpha(77), blurRadius: 24, offset: const Offset(0, 8))],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: Colors.white.withAlpha(51),
                    borderRadius: BorderRadius.circular(99),
                  ),
                  child: const Text('Đang hoạt động',
                      style: TextStyle(fontFamily: 'Lexend', fontSize: 12, color: Colors.white)),
                ),
                const Icon(Icons.more_vert, color: Colors.white),
              ],
            ),
            const SizedBox(height: 12),
            Text(group.groupName,
                style: const TextStyle(fontFamily: 'Lexend', fontSize: 24, fontWeight: FontWeight.w700, color: Colors.white)),
            const SizedBox(height: 4),
            Text('${group.memberCount} thành viên',
                style: TextStyle(fontFamily: 'Lexend', fontSize: 14, color: Colors.white.withAlpha(204))),
            const SizedBox(height: 16),
            // Stacked member avatars
            _StackedAvatars(members: group.members),
            const SizedBox(height: 16),
            // Upcoming task preview
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.white.withAlpha(38),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Row(
                children: [
                  const Icon(Icons.event, size: 18, color: Colors.white),
                  const SizedBox(width: 8),
                  const Text('Khám thai tuần 28 - Hôm nay 9:00',
                      style: TextStyle(fontFamily: 'Lexend', fontSize: 12, color: Colors.white)),
                ],
              ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () {},
                    style: OutlinedButton.styleFrom(
                      foregroundColor: Colors.white,
                      side: const BorderSide(color: Colors.white, width: 1.5),
                      shape: const StadiumBorder(),
                    ),
                    icon: const Icon(Icons.person_add, size: 18),
                    label: const Text('Mời', style: TextStyle(fontFamily: 'Lexend')),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: FilledButton.icon(
                    onPressed: onTap,
                    style: FilledButton.styleFrom(
                      backgroundColor: Colors.white.withAlpha(51),
                      foregroundColor: Colors.white,
                      shape: const StadiumBorder(),
                    ),
                    icon: const Icon(Icons.settings, size: 18),
                    label: const Text('Quản lý', style: TextStyle(fontFamily: 'Lexend')),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _StackedAvatars extends StatelessWidget {
  final List<CareGroupMember> members;

  const _StackedAvatars({required this.members});

  @override
  Widget build(BuildContext context) {
    final show = members.take(4).toList();
    return SizedBox(
      height: 36,
      child: Stack(
        children: [
          ...show.asMap().entries.map((e) {
            return Positioned(
              left: e.key * 24.0,
              child: Container(
                width: 36, height: 36,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: const Color(0xFFC98C7B).withAlpha(128),
                  border: Border.all(color: Colors.white, width: 2),
                ),
                child: Center(
                  child: Text(e.value.displayName.isNotEmpty ? e.value.displayName[0] : '?',
                      style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, fontWeight: FontWeight.w600, color: Colors.white)),
                ),
              ),
            );
          }),
          if (members.length > 4)
            Positioned(
              left: 4 * 24.0,
              child: Container(
                width: 36, height: 36,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: Colors.white.withAlpha(51),
                  border: Border.all(color: Colors.white, width: 2),
                ),
                child: Center(
                  child: Text('+${members.length - 4}',
                      style: const TextStyle(fontFamily: 'Lexend', fontSize: 11, color: Colors.white)),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

// ─── Secondary (inactive) group card ─────────────────────────────────────────
class _SecondaryGroupCard extends StatelessWidget {
  final CareGroup group;
  final VoidCallback onTap;

  const _SecondaryGroupCard({required this.group, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [BoxShadow(color: const Color(0xFF5A463F).withAlpha(13), blurRadius: 12, offset: const Offset(0, 4))],
        ),
        child: Row(
          children: [
            Container(
              width: 48, height: 48,
              decoration: const BoxDecoration(color: Color(0xFFFFE9E3), shape: BoxShape.circle),
              child: const Icon(Icons.group, color: Color(0xFF845143)),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(group.groupName,
                      style: const TextStyle(fontFamily: 'Lexend', fontSize: 18, fontWeight: FontWeight.w600, color: Color(0xFF271812))),
                  Text('${group.memberCount} thành viên',
                      style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, color: Color(0xFF524440))),
                ],
              ),
            ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: const Color(0xFFE9E1DB).withAlpha(128),
                borderRadius: BorderRadius.circular(99),
              ),
              child: const Text('Tạm ngưng',
                  style: TextStyle(fontFamily: 'Lexend', fontSize: 11, color: Color(0xFF524440))),
            ),
          ],
        ),
      ),
    );
  }
}
