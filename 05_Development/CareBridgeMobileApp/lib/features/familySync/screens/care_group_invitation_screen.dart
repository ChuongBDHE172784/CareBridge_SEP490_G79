import 'package:flutter/material.dart';
import '../models/care_group_model.dart';
import '../services/care_group_service.dart';
import 'reject_invitation_confirmation_screen.dart';
import '../widgets/family_relationship_role_picker.dart';

enum _InvitationStatus { pending, accepted, declined }

class CareGroupInvitationScreen extends StatefulWidget {
  final PendingInvitation invitation;

  const CareGroupInvitationScreen({super.key, required this.invitation});

  @override
  State<CareGroupInvitationScreen> createState() =>
      _CareGroupInvitationScreenState();
}

class _CareGroupInvitationScreenState extends State<CareGroupInvitationScreen> {
  final _service = CareGroupService();
  bool _isLoading = false;
  bool _checkingStatus = true;
  _InvitationStatus _status = _InvitationStatus.pending;

  @override
  void initState() {
    super.initState();
    _checkStatus();
  }

  Future<void> _checkStatus() async {
    try {
      final invitations = await _service.listMyInvitations();
      final isStillPending =
          invitations.any((inv) => inv.groupId == widget.invitation.groupId);

      if (!isStillPending) {
        final myGroups = await _service.listMyGroups();
        final isMember = myGroups.any((g) => g.id == widget.invitation.groupId);
        if (mounted) {
          setState(() {
            _status = isMember
                ? _InvitationStatus.accepted
                : _InvitationStatus.declined;
            _checkingStatus = false;
          });
        }
        return;
      }
    } catch (_) {}
    if (mounted) {
      setState(() => _checkingStatus = false);
    }
  }

  Future<void> _accept() async {
    final relationship = await showFamilyRelationshipRolePicker(context);
    if (relationship == null || !mounted) return;
    setState(() => _isLoading = true);
    try {
      await _service.acceptInvite(
        widget.invitation.groupId,
        familyRelationshipRole: relationship.role,
        customFamilyRelationshipRole: relationship.customRole,
      );
      if (mounted) {
        setState(() {
          _status = _InvitationStatus.accepted;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  void _reject() async {
    final result = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (_) => RejectInvitationConfirmationScreen(
          groupId: widget.invitation.groupId,
          groupName: widget.invitation.groupName,
        ),
      ),
    );
    if (result == true && mounted) {
      setState(() {
        _status = _InvitationStatus.declined;
      });
    }
  }

  String _formatInvitationText(String groupName) {
    final clean = groupName.trim();
    if (clean.toLowerCase().startsWith('nhóm')) {
      return 'Bạn đã được mời tham gia $clean.';
    }
    return 'Bạn đã được mời tham gia nhóm chăm sóc $clean.';
  }

  String _formatRole(String role) {
    final upper = role.trim().toUpperCase();
    switch (upper) {
      case 'MEMBER':
        return 'Thành viên';
      case 'ADMIN':
        return 'Quản trị viên';
      case 'MOTHER':
        return 'Mẹ';
      case 'FAMILY':
        return 'Người thân gia đình';
      default:
        return role.isNotEmpty ? role : 'Thành viên';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFEF8F4),
      appBar: AppBar(
        backgroundColor: const Color(0xFFFEF8F4),
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Color(0xFF845143)),
          onPressed: () => Navigator.pop(context, _status != _InvitationStatus.pending),
        ),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 8.0),
          child: Column(
            children: [
              Expanded(
                child: Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(24),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(24),
                    border: Border.all(color: const Color(0xFFFFE2D9)),
                    boxShadow: const [
                      BoxShadow(
                        color: Color(0x0F5A463F),
                        blurRadius: 20,
                        offset: Offset(0, 4),
                      ),
                    ],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      Container(
                        width: 80,
                        height: 80,
                        decoration: const BoxDecoration(
                          color: Color(0xFFFFDBD1),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.group_add,
                          color: Color(0xFF845143),
                          size: 36,
                        ),
                      ),
                      const SizedBox(height: 16),
                      const Text(
                        'Lời mời tham gia nhóm chăm sóc',
                        style: TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF845143),
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 16),
                      Text(
                        _formatInvitationText(widget.invitation.groupName),
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          fontSize: 16,
                          color: Color(0xFF524440),
                          fontFamily: 'Lexend',
                        ),
                      ),
                      const SizedBox(height: 24),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 16,
                          vertical: 8,
                        ),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF8DDD2),
                          borderRadius: BorderRadius.circular(999),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Icon(
                              Icons.escalator_warning,
                              size: 20,
                              color: Color(0xFF271812),
                            ),
                            const SizedBox(width: 8),
                            Text(
                              'Vai trò: ${_formatRole(widget.invitation.memberRole)}',
                              style: const TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.w600,
                                color: Color(0xFF271812),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 24),
                      if (_checkingStatus)
                        const Padding(
                          padding: EdgeInsets.symmetric(vertical: 16.0),
                          child: CircularProgressIndicator(
                            color: Color(0xFFC98C7B),
                            strokeWidth: 2,
                          ),
                        )
                      else if (_status != _InvitationStatus.pending)
                        _buildStatusCard()
                      else
                        const Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(
                              Icons.schedule,
                              size: 16,
                              color: Color(0xFF524440),
                            ),
                            SizedBox(width: 8),
                            Text(
                              'Hết hạn sau 48 giờ',
                              style: TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.bold,
                                color: Color(0xFF524440),
                              ),
                            ),
                          ],
                        ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              if (_status == _InvitationStatus.pending) ...[
                ElevatedButton(
                  onPressed: _isLoading ? null : _accept,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFC98C7B),
                    foregroundColor: Colors.white,
                    minimumSize: const Size(double.infinity, 52),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(999),
                    ),
                  ),
                  child: _isLoading
                      ? const SizedBox(
                          width: 24,
                          height: 24,
                          child: CircularProgressIndicator(
                            color: Colors.white,
                            strokeWidth: 2,
                          ),
                        )
                      : const Text(
                          'Chấp nhận',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                ),
                const SizedBox(height: 12),
                ElevatedButton(
                  onPressed: _isLoading ? null : _reject,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFFADCD3),
                    foregroundColor: const Color(0xFF524440),
                    minimumSize: const Size(double.infinity, 52),
                    elevation: 0,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(999),
                    ),
                  ),
                  child: const Text(
                    'Từ chối',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ] else ...[
                ElevatedButton(
                  onPressed: () => Navigator.pop(context, true),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFC98C7B),
                    foregroundColor: Colors.white,
                    minimumSize: const Size(double.infinity, 52),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(999),
                    ),
                  ),
                  child: const Text(
                    'Đóng',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStatusCard() {
    final isAccepted = _status == _InvitationStatus.accepted;
    final color =
        isAccepted ? const Color(0xFF2E7D32) : const Color(0xFFC62828);
    final bgColor =
        isAccepted ? const Color(0xFFE8F5E9) : const Color(0xFFFFEBEE);
    final icon = isAccepted ? Icons.check_circle : Icons.cancel;
    final message = isAccepted
        ? 'Bạn đã chấp nhận lời mời tham gia nhóm này.'
        : 'Lời mời này đã bị từ chối hoặc không còn hiệu lực.';

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Row(
        children: [
          Icon(icon, color: color, size: 24),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              message,
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: color,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
