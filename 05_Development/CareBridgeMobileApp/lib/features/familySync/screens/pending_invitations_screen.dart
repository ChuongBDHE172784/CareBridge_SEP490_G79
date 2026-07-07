import 'package:flutter/material.dart';
import '../models/care_group_model.dart';
import '../services/care_group_service.dart';

/// CB-024 — Pending Care Group Invitations (UC-83)
/// Lists invitations the current user has not yet accepted/declined.
/// GET /api/v1/care-groups/invitations/me, accept/decline per group.
class PendingInvitationsScreen extends StatefulWidget {
  const PendingInvitationsScreen({super.key});

  @override
  State<PendingInvitationsScreen> createState() =>
      _PendingInvitationsScreenState();
}

class _PendingInvitationsScreenState extends State<PendingInvitationsScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _error = Color(0xFFBA1A1A);

  final _service = CareGroupService();
  List<PendingInvitation> _invites = [];
  bool _loading = true;
  final Set<String> _busyGroupIds = {};

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final list = await _service.listMyInvitations();
      if (mounted)
        setState(() {
          _invites = list;
          _loading = false;
        });
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _respond(PendingInvitation invite, bool accept) async {
    setState(() => _busyGroupIds.add(invite.groupId));
    try {
      if (accept) {
        await _service.acceptInvite(invite.groupId);
      } else {
        await _service.declineInvite(invite.groupId);
      }
      if (mounted) {
        setState(() {
          _invites.removeWhere((i) => i.groupId == invite.groupId);
          _busyGroupIds.remove(invite.groupId);
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              accept
                  ? 'Đã tham gia "${invite.groupName}"'
                  : 'Đã từ chối lời mời',
            ),
          ),
        );
      }
    } catch (_) {
      if (mounted) {
        setState(() => _busyGroupIds.remove(invite.groupId));
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể xử lý lời mời. Vui lòng thử lại.'),
          ),
        );
      }
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
            Expanded(
              child: _loading
                  ? const Center(
                      child: CircularProgressIndicator(
                        color: _primaryContainer,
                      ),
                    )
                  : _invites.isEmpty
                  ? _buildEmpty()
                  : RefreshIndicator(
                      color: _primaryContainer,
                      onRefresh: _load,
                      child: ListView.separated(
                        padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
                        itemCount: _invites.length,
                        separatorBuilder: (_, _) => const SizedBox(height: 12),
                        itemBuilder: (_, i) => _InviteCard(
                          invite: _invites[i],
                          busy: _busyGroupIds.contains(_invites[i].groupId),
                          onAccept: () => _respond(_invites[i], true),
                          onDecline: () => _respond(_invites[i], false),
                        ),
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
          const Expanded(
            child: Text(
              'Lời mời đang chờ',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.mail_outline, size: 64, color: Color(0xFFC98C7B)),
          const SizedBox(height: 16),
          const Text(
            'Không có lời mời nào',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Lời mời tham gia nhóm chăm sóc sẽ hiện ở đây.',
            textAlign: TextAlign.center,
            style: TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

class _InviteCard extends StatelessWidget {
  final PendingInvitation invite;
  final bool busy;
  final VoidCallback onAccept;
  final VoidCallback onDecline;

  const _InviteCard({
    required this.invite,
    required this.busy,
    required this.onAccept,
    required this.onDecline,
  });

  static const _primary = Color(0xFF845143);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _error = Color(0xFFBA1A1A);

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
                      invite.groupName,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: _onSurface,
                      ),
                    ),
                    Text(
                      'Vai trò: ${invite.roleLabel}',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: busy ? null : onDecline,
                  style: OutlinedButton.styleFrom(
                    foregroundColor: _error,
                    side: const BorderSide(color: _error),
                    shape: const StadiumBorder(),
                  ),
                  child: const Text(
                    'Từ chối',
                    style: TextStyle(fontFamily: 'Lexend'),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: FilledButton(
                  onPressed: busy ? null : onAccept,
                  style: FilledButton.styleFrom(
                    backgroundColor: const Color(0xFFC98C7B),
                    shape: const StadiumBorder(),
                  ),
                  child: busy
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(
                            color: Colors.white,
                            strokeWidth: 2,
                          ),
                        )
                      : const Text(
                          'Chấp nhận',
                          style: TextStyle(fontFamily: 'Lexend'),
                        ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
