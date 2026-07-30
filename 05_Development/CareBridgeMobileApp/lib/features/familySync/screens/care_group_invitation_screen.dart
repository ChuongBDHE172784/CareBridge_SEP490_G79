import 'package:flutter/material.dart';
import '../models/care_group_model.dart';
import '../services/care_group_service.dart';
import 'reject_invitation_confirmation_screen.dart';
import '../widgets/family_relationship_role_picker.dart';

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
        Navigator.pop(context, true); // Return true on success
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
      Navigator.pop(context, true);
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
          onPressed: () => Navigator.pop(context),
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
                      RichText(
                        textAlign: TextAlign.center,
                        text: TextSpan(
                          style: const TextStyle(
                            fontSize: 16,
                            color: Color(0xFF524440),
                            fontFamily: 'Lexend',
                          ),
                          children: [
                            const TextSpan(
                              text: 'Bạn đã được mời tham gia nhóm chăm sóc ',
                            ),
                            TextSpan(
                              text: widget.invitation.groupName,
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                                color: Color(0xFF271812),
                              ),
                            ),
                            const TextSpan(text: '.'),
                          ],
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
                              'Vai trò: ${widget.invitation.memberRole}',
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
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: const Color(0xFFFFF1EC),
                          borderRadius: BorderRadius.circular(16),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'QUYỀN HẠN',
                              style: TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.bold,
                                color: Color(0xFF524440),
                              ),
                            ),
                            const SizedBox(height: 8),
                            _buildRightRow('Xem lịch tiêm chủng'),
                            const SizedBox(height: 8),
                            _buildRightRow('Theo dõi giấc ngủ'),
                            const SizedBox(height: 8),
                            _buildRightRow('Nhận thông báo khẩn cấp'),
                            const SizedBox(height: 16),
                            const Text(
                              'MỤC ĐÍCH',
                              style: TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.bold,
                                color: Color(0xFF524440),
                              ),
                            ),
                            const SizedBox(height: 4),
                            const Text(
                              'Hỗ trợ chăm sóc bé',
                              style: TextStyle(
                                fontSize: 14,
                                color: Color(0xFF271812),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 24),
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
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildRightRow(String text) {
    return Row(
      children: [
        const Icon(Icons.check_circle, color: Color(0xFF845143), size: 20),
        const SizedBox(width: 8),
        Text(
          text,
          style: const TextStyle(fontSize: 14, color: Color(0xFF271812)),
        ),
      ],
    );
  }
}
