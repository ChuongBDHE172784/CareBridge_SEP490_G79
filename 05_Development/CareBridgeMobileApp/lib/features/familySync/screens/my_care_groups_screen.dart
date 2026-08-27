import 'dart:convert';
import 'package:flutter/material.dart';
import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../models/care_group_model.dart';
import '../services/care_group_service.dart';
import 'care_group_detail_screen.dart';

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
      if (mounted) {
        setState(() {
          _groups = list;
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _joinGroupWithCode() async {
    final result = await showDialog<_JoinGroupResult>(
      context: context,
      builder: (_) => const _JoinGroupDialog(),
    );

    if (result == null || result.code.isEmpty) return;

    try {
      setState(() => _loading = true);
      await _service.joinGroupByCode(
        result.code,
        familyRelationshipRole: result.relationshipRole,
        customFamilyRelationshipRole: result.customRole,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Yêu cầu đã được gửi!\nVui lòng chờ Mother của nhóm duyệt.',
          ),
          duration: Duration(seconds: 4),
          behavior: SnackBarBehavior.floating,
        ),
      );
    } catch (e) {
      if (!mounted) return;
      String errorMsg = 'Không thể tham gia nhóm';
      if (e is ApiException) {
        try {
          final map = jsonDecode(e.message) as Map<String, dynamic>;
          final errCode = map['error']?.toString();
          if (errCode == 'FAM-005' || e.statusCode == 404) {
            errorMsg = 'Nhóm không tồn tại';
          } else if (map['message'] != null) {
            errorMsg = map['message'].toString();
          }
        } catch (_) {
          if (e.statusCode == 404 || e.message.contains('FAM-005')) {
            errorMsg = 'Nhóm không tồn tại';
          }
        }
      } else {
        final raw = e.toString();
        if (raw.contains('FAM-005') || raw.contains('404')) {
          errorMsg = 'Nhóm không tồn tại';
        }
      }
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(errorMsg)));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
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
            ],
          ),
          // App logo area
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(16),
              boxShadow: const [
                BoxShadow(
                  color: Color(0x1A845143),
                  blurRadius: 10,
                  offset: Offset(0, 4),
                ),
              ],
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(16),
              child: Image.asset(
                'assets/logo.png',
                width: 56,
                height: 56,
                fit: BoxFit.cover,
              ),
            ),
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
                  onPressed: _joinGroupWithCode,
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
              if (AuthState.instance.role != 'MOTHER' &&
                  group.myRole != 'OWNER' &&
                  group.myRole != 'MOTHER') ...[
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
            ],
          ),
        ],
      ),
    );
  }
}

class _JoinGroupResult {
  final String code;
  final String relationshipRole;
  final String customRole;

  const _JoinGroupResult({
    required this.code,
    required this.relationshipRole,
    required this.customRole,
  });
}

class _JoinGroupDialog extends StatefulWidget {
  const _JoinGroupDialog();

  @override
  State<_JoinGroupDialog> createState() => _JoinGroupDialogState();
}

class _JoinGroupDialogState extends State<_JoinGroupDialog> {
  late final TextEditingController _codeCtrl;
  late final TextEditingController _customRoleCtrl;
  String _relationshipRole = 'CHONG';

  @override
  void initState() {
    super.initState();
    _codeCtrl = TextEditingController();
    _customRoleCtrl = TextEditingController();
  }

  @override
  void dispose() {
    _codeCtrl.dispose();
    _customRoleCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
      ),
      title: const Text(
        'Tham gia nhóm',
        style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600),
      ),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Nhập mã nhóm (UUID hoặc mã mời) được chia sẻ từ Mẹ bầu:',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 13,
              color: Color(0xFF524440),
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _codeCtrl,
            decoration: InputDecoration(
              labelText: 'Mã nhóm chăm sóc',
              hintText: 'VD: cb51dfb1-c615-41af-ae16-47df06cbca80',
              prefixIcon: const Icon(
                Icons.key,
                color: _MyCareGroupsScreenState._primaryContainer,
              ),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
            style: const TextStyle(fontFamily: 'Lexend'),
          ),
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            initialValue: _relationshipRole,
            decoration: InputDecoration(
              labelText: 'Vai trò trong gia đình',
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
            items: familyRelationshipLabels.entries
                .map(
                  (entry) => DropdownMenuItem(
                    value: entry.key,
                    child: Text(entry.value),
                  ),
                )
                .toList(),
            onChanged: (value) {
              if (value != null) {
                setState(() => _relationshipRole = value);
              }
            },
          ),
          if (_relationshipRole == 'KHAC') ...[
            const SizedBox(height: 12),
            TextField(
              controller: _customRoleCtrl,
              decoration: InputDecoration(
                labelText: 'Vai trò tùy chỉnh',
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
          ],
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context, null),
          child: const Text('Hủy', style: TextStyle(fontFamily: 'Lexend')),
        ),
        FilledButton(
          onPressed: () {
            if (_relationshipRole == 'KHAC' &&
                _customRoleCtrl.text.trim().isEmpty) {
              return;
            }
            Navigator.pop(
              context,
              _JoinGroupResult(
                code: _codeCtrl.text.trim(),
                relationshipRole: _relationshipRole,
                customRole: _customRoleCtrl.text.trim(),
              ),
            );
          },
          style: FilledButton.styleFrom(
            backgroundColor: _MyCareGroupsScreenState._primaryContainer,
          ),
          child: const Text(
            'Tham gia',
            style: TextStyle(fontFamily: 'Lexend'),
          ),
        ),
      ],
    );
  }
}

