import 'package:flutter/material.dart';

import '../models/care_group_model.dart';
import '../services/care_group_service.dart';
import 'care_group_detail_screen.dart';

class CareGroupsScreen extends StatefulWidget {
  const CareGroupsScreen({super.key, this.service});

  final CareGroupService? service;

  @override
  State<CareGroupsScreen> createState() => _CareGroupsScreenState();
}

class _CareGroupsScreenState extends State<CareGroupsScreen> {
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Color(0xFFFFFBF8);
  static const _primary = Color(0xFF9B5E4E);
  static const _text = Color(0xFF2E211D);
  static const _muted = Color(0xFF74645F);

  late final CareGroupService _service = widget.service ?? CareGroupService();
  List<CareGroup> _groups = const [];
  bool _loading = true;
  bool _loadFailed = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _loadFailed = false;
    });
    try {
      final groups = await _service.listMyGroups();
      if (!mounted) return;
      setState(() {
        _groups = groups;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _loadFailed = true;
      });
    }
  }

  Future<void> _createGroup() async {
    final name = await showDialog<String>(
      context: context,
      builder: (context) => const _CreateCareGroupDialog(),
    );
    if (name == null || name.trim().isEmpty) return;

    try {
      await _service.createCareGroup(name.trim());
      await _load();
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Đã tạo nhóm chăm sóc.')));
    } catch (error) {
      if (!mounted) return;
      final duplicate = error.toString().contains('FAM-015');
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            duplicate
                ? 'Tên nhóm đã tồn tại. Hãy chọn tên khác.'
                : 'Chưa thể tạo nhóm. Vui lòng thử lại.',
          ),
        ),
      );
    }
  }

  Future<void> _deleteGroup(CareGroup group) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
        title: const Text(
          'Xóa nhóm chăm sóc?',
          style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w700),
        ),
        content: Text(
          'Nhóm “${group.groupName}” và quyền của các thành viên sẽ bị xóa. '
          'Thao tác này không thể hoàn tác.',
          style: const TextStyle(fontFamily: 'Lexend', height: 1.45),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: const Color(0xFFBA1A1A),
            ),
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Xóa nhóm'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    try {
      await _service.deleteCareGroup(group.id);
      await _load();
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Đã xóa nhóm chăm sóc.')));
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Chưa thể xóa nhóm. Vui lòng thử lại.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        surfaceTintColor: Colors.transparent,
        title: const Text(
          'Nhóm chăm sóc',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 22,
            fontWeight: FontWeight.w700,
            color: _text,
          ),
        ),
      ),
      body: _loading
          ? const _GroupsLoading()
          : _loadFailed
          ? _buildError()
          : RefreshIndicator(
              color: _primary,
              onRefresh: _load,
              child: CustomScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                slivers: [
                  SliverToBoxAdapter(child: _buildIntro()),
                  if (_groups.isEmpty)
                    SliverFillRemaining(
                      hasScrollBody: false,
                      child: _buildEmpty(),
                    )
                  else
                    SliverPadding(
                      padding: const EdgeInsets.fromLTRB(16, 4, 16, 104),
                      sliver: SliverList.separated(
                        itemCount: _groups.length,
                        itemBuilder: (context, index) => _GroupCard(
                          group: _groups[index],
                          onOpen: () => _openDetail(_groups[index]),
                          onDelete: () => _deleteGroup(_groups[index]),
                        ),
                        separatorBuilder: (_, _) => const SizedBox(height: 12),
                      ),
                    ),
                ],
              ),
            ),
      floatingActionButton: _loadFailed
          ? null
          : FloatingActionButton.extended(
              onPressed: _createGroup,
              backgroundColor: _primary,
              foregroundColor: Colors.white,
              icon: const Icon(Icons.add),
              label: const Text(
                'Tạo nhóm',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
    );
  }

  Widget _buildIntro() {
    return const Padding(
      padding: EdgeInsets.fromLTRB(16, 4, 16, 18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Cùng chăm sóc, đúng quyền riêng tư',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 19,
              fontWeight: FontWeight.w700,
              color: _text,
            ),
          ),
          SizedBox(height: 6),
          Text(
            'Mời người thân, phân công hỗ trợ và kiểm soát dữ liệu bạn chia sẻ.',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 13,
              height: 1.45,
              color: _muted,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      key: const Key('care-groups-empty'),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(32, 16, 32, 120),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 84,
              height: 84,
              decoration: const BoxDecoration(
                color: Color(0xFFFFE5DE),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.groups_outlined,
                size: 42,
                color: _primary,
              ),
            ),
            const SizedBox(height: 20),
            const Text(
              'Bạn chưa có nhóm chăm sóc',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: _text,
              ),
            ),
            const SizedBox(height: 8),
            const Text(
              'Tạo nhóm đầu tiên để mời người thân đồng hành cùng bạn.',
              textAlign: TextAlign.center,
              style: TextStyle(fontFamily: 'Lexend', color: _muted),
            ),
            const SizedBox(height: 20),
            OutlinedButton.icon(
              onPressed: _createGroup,
              icon: const Icon(Icons.group_add_outlined),
              label: const Text('Tạo nhóm đầu tiên'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildError() {
    return Center(
      key: const Key('care-groups-error'),
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.cloud_off_outlined, size: 50, color: _primary),
            const SizedBox(height: 16),
            const Text(
              'Chưa tải được nhóm chăm sóc',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: _text,
              ),
            ),
            const SizedBox(height: 8),
            const Text(
              'Kiểm tra kết nối mạng rồi thử lại.',
              style: TextStyle(fontFamily: 'Lexend', color: _muted),
            ),
            const SizedBox(height: 20),
            FilledButton.icon(
              key: const Key('care-groups-retry-button'),
              onPressed: _load,
              icon: const Icon(Icons.refresh),
              label: const Text('Thử lại'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _openDetail(CareGroup group) async {
    await Navigator.push<void>(
      context,
      MaterialPageRoute(
        builder: (_) => CareGroupDetailScreen(
          groupId: group.id,
          groupName: group.groupName,
        ),
      ),
    );
    if (mounted) await _load();
  }
}

class _GroupCard extends StatelessWidget {
  const _GroupCard({
    required this.group,
    required this.onOpen,
    required this.onDelete,
  });

  final CareGroup group;
  final VoidCallback onOpen;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final description = group.description?.trim();
    return Material(
      color: _CareGroupsScreenState._surface,
      borderRadius: BorderRadius.circular(22),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onOpen,
        child: Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(22),
            border: Border.all(color: const Color(0xFFEDE4DF)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 48,
                    height: 48,
                    decoration: BoxDecoration(
                      color: group.isActive
                          ? const Color(0xFFFFE5DE)
                          : const Color(0xFFF0EAE6),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Icon(
                      Icons.diversity_1_outlined,
                      color: group.isActive
                          ? _CareGroupsScreenState._primary
                          : _CareGroupsScreenState._muted,
                    ),
                  ),
                  const SizedBox(width: 13),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          group.groupName,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 17,
                            fontWeight: FontWeight.w700,
                            color: _CareGroupsScreenState._text,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '${group.memberCount} thành viên',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            color: _CareGroupsScreenState._muted,
                          ),
                        ),
                      ],
                    ),
                  ),
                  PopupMenuButton<String>(
                    tooltip: 'Tùy chọn nhóm',
                    onSelected: (value) {
                      if (value == 'delete') onDelete();
                    },
                    itemBuilder: (_) => const [
                      PopupMenuItem(
                        value: 'delete',
                        child: Row(
                          children: [
                            Icon(
                              Icons.delete_outline,
                              color: Color(0xFFBA1A1A),
                            ),
                            SizedBox(width: 10),
                            Text('Xóa nhóm'),
                          ],
                        ),
                      ),
                    ],
                  ),
                ],
              ),
              if (description != null && description.isNotEmpty) ...[
                const SizedBox(height: 12),
                Text(
                  description,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    height: 1.4,
                    color: _CareGroupsScreenState._muted,
                  ),
                ),
              ],
              const SizedBox(height: 14),
              Row(
                children: [
                  _MemberAvatars(members: group.members),
                  const Spacer(),
                  _ActivityPill(isActive: group.isActive),
                  const SizedBox(width: 8),
                  const Icon(
                    Icons.chevron_right,
                    color: _CareGroupsScreenState._muted,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _MemberAvatars extends StatelessWidget {
  const _MemberAvatars({required this.members});

  final List<CareGroupMember> members;

  @override
  Widget build(BuildContext context) {
    if (members.isEmpty) {
      return const Text(
        'Chưa có thông tin thành viên',
        style: TextStyle(
          fontFamily: 'Lexend',
          fontSize: 11,
          color: _CareGroupsScreenState._muted,
        ),
      );
    }
    final visible = members.take(3).toList();
    return SizedBox(
      width: visible.length * 25 + 18,
      height: 34,
      child: Stack(
        children: [
          for (final entry in visible.asMap().entries)
            Positioned(
              left: entry.key * 25,
              child: CircleAvatar(
                radius: 17,
                backgroundColor: const Color(0xFFC98C7B),
                foregroundColor: Colors.white,
                child: Text(
                  entry.value.displayName.trim().isEmpty
                      ? '?'
                      : entry.value.displayName.trim()[0].toUpperCase(),
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _ActivityPill extends StatelessWidget {
  const _ActivityPill({required this.isActive});

  final bool isActive;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: isActive ? const Color(0xFFEAF3EC) : const Color(0xFFF0EAE6),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        isActive ? 'Đang hoạt động' : 'Đã tạm dừng',
        style: TextStyle(
          fontFamily: 'Lexend',
          fontSize: 10,
          fontWeight: FontWeight.w600,
          color: isActive
              ? const Color(0xFF356344)
              : _CareGroupsScreenState._muted,
        ),
      ),
    );
  }
}

class _GroupsLoading extends StatelessWidget {
  const _GroupsLoading();

  @override
  Widget build(BuildContext context) {
    return ListView(
      key: const Key('care-groups-loading'),
      padding: const EdgeInsets.all(16),
      children: [
        Container(
          height: 54,
          decoration: BoxDecoration(
            color: const Color(0xFFECE4DF),
            borderRadius: BorderRadius.circular(16),
          ),
        ),
        const SizedBox(height: 20),
        for (var index = 0; index < 3; index++) ...[
          Container(
            height: 144,
            decoration: BoxDecoration(
              color: const Color(0xFFECE4DF),
              borderRadius: BorderRadius.circular(22),
            ),
          ),
          const SizedBox(height: 12),
        ],
      ],
    );
  }
}

class _CreateCareGroupDialog extends StatefulWidget {
  const _CreateCareGroupDialog();

  @override
  State<_CreateCareGroupDialog> createState() => _CreateCareGroupDialogState();
}

class _CreateCareGroupDialogState extends State<_CreateCareGroupDialog> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
      title: const Text(
        'Tạo nhóm chăm sóc',
        style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w700),
      ),
      content: TextField(
        controller: _controller,
        autofocus: true,
        maxLength: 80,
        textCapitalization: TextCapitalization.sentences,
        style: const TextStyle(fontFamily: 'Lexend'),
        decoration: InputDecoration(
          labelText: 'Tên nhóm',
          hintText: 'Ví dụ: Gia đình của Lan',
          filled: true,
          fillColor: _CareGroupsScreenState._canvas,
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(16),
            borderSide: BorderSide.none,
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context, null),
          child: const Text('Hủy'),
        ),
        FilledButton(
          style: FilledButton.styleFrom(
            backgroundColor: _CareGroupsScreenState._primary,
          ),
          onPressed: () => Navigator.pop(context, _controller.text),
          child: const Text('Tạo nhóm'),
        ),
      ],
    );
  }
}

