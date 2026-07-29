import 'package:flutter/material.dart';
import '../services/care_group_service.dart';

class InviteFamilyMemberScreen extends StatefulWidget {
  final String groupId;

  const InviteFamilyMemberScreen({super.key, required this.groupId});

  @override
  State<InviteFamilyMemberScreen> createState() =>
      _InviteFamilyMemberScreenState();
}

class _InviteFamilyMemberScreenState extends State<InviteFamilyMemberScreen> {
  final CareGroupService _service = CareGroupService();
  final TextEditingController _contactController = TextEditingController();
  final TextEditingController _messageController = TextEditingController();

  bool _isLoading = false;
  bool _isValid = false;

  void _onContactChanged(String value) {
    final trimmed = value.trim();
    setState(() {
      _isValid =
          trimmed.length >= 9 || (trimmed.contains('@') && trimmed.contains('.'));
    });
  }

  Future<void> _sendInvitation() async {
    if (!_isValid) return;

    setState(() => _isLoading = true);
    try {
      String? phone = _contactController.text.trim();

      await _service.inviteMember(
        widget.groupId,
        channel: 'PHONE',
        phone: phone,
      );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đã gửi lời mời thành công!')),
        );
        Navigator.pop(context, true);
      }
    } catch (e) {
      if (mounted) {
        String message = 'Lỗi khi gửi lời mời: $e';
        final errStr = e.toString();
        if (errStr.contains('FAM-011') ||
            errStr.contains('already an accepted member') ||
            errStr.contains('already a member') ||
            errStr.contains('đã tồn tại') ||
            errStr.contains('đã ở trong nhóm')) {
          message = 'Thành viên này đã tồn tại trong nhóm';
        } else if (errStr.contains('FAM-010') ||
            errStr.contains('Pending invitation already exists') ||
            errStr.contains('chờ xử lý')) {
          message = 'Lời mời cho người này đang chờ xử lý';
        } else if (errStr.contains('FAM-004') ||
            errStr.contains('FAM-014') ||
            errStr.contains('User not found') ||
            errStr.contains('phone number or email')) {
          message = 'Không tìm thấy tài khoản với SĐT hoặc Gmail này';
        }
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(message)));
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  void dispose() {
    _contactController.dispose();
    _messageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFEF8F4),
      appBar: AppBar(
        backgroundColor: const Color(0xFFFEF8F4),
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Color(0xFF845143)),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Mời thành viên',
          style: TextStyle(
            color: Color(0xFF845143),
            fontSize: 22,
            fontWeight: FontWeight.bold,
            fontFamily: 'Quicksand',
          ),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Introduction
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 16.0),
              child: Column(
                children: [
                  Text(
                    'Xây dựng vòng kết nối',
                    style: TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF2D2A28),
                      fontFamily: 'Quicksand',
                    ),
                  ),
                  SizedBox(height: 8),
                  Text(
                    'Mời người thân tham gia để cùng theo dõi và chăm sóc sức khỏe cho gia đình một cách trọn vẹn nhất.',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 14,
                      color: Color(0xFF524F4C),
                      fontFamily: 'Quicksand',
                    ),
                  ),
                ],
              ),
            ),

            _buildPhoneTab(),

            const SizedBox(height: 32),

            // Note
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: const Color(0xFFF6F1EC),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: const Color(0xFFF2EAE4)),
              ),
              child: const Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.info, color: Color(0xFF845143), size: 20),
                  SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      'Ghi chú bảo mật: Quyền xem hồ sơ sức khỏe và lịch sử tiêm chủng sẽ được bạn cấu hình cụ thể cho từng thành viên sau khi họ chấp nhận lời mời.',
                      style: TextStyle(
                        fontSize: 14,
                        color: Color(0xFF524440),
                        fontFamily: 'Quicksand',
                      ),
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 32),

            // Submit Button
            ElevatedButton(
              onPressed: (_isValid && !_isLoading) ? _sendInvitation : null,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF845143),
                disabledBackgroundColor: const Color(
                  0xFF845143,
                ).withValues(alpha: 0.5),
                padding: const EdgeInsets.symmetric(vertical: 16),
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
                  : const Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          'Gửi lời mời',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                            fontFamily: 'Quicksand',
                          ),
                        ),
                        SizedBox(width: 8),
                        Icon(Icons.send, color: Colors.white, size: 20),
                      ],
                    ),
            ),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }

  Widget _buildPhoneTab() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'SỐ ĐIỆN THOẠI HOẶC GMAIL NGƯỜI NHẬN',
          style: TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.bold,
            color: Color(0xFF524440),
            fontFamily: 'Quicksand',
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: _contactController,
          onChanged: _onContactChanged,
          keyboardType: TextInputType.emailAddress,
          decoration: InputDecoration(
            hintText: 'Nhập SĐT hoặc Gmail (VD: 0987654321 hoặc user@gmail.com)',
            filled: true,
            fillColor: Colors.white,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: const BorderSide(color: Color(0xFFF2EAE4), width: 2),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: const BorderSide(color: Color(0xFFF2EAE4), width: 2),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: const BorderSide(color: Color(0xFFC98C7B), width: 2),
            ),
            suffixIcon: _isValid
                ? const Icon(Icons.check_circle, color: Colors.green)
                : null,
          ),
        ),
        const Padding(
          padding: EdgeInsets.only(top: 4.0, left: 4.0),
          child: Text(
            'Hệ thống sẽ gửi lời mời tới thông tin này.',
            style: TextStyle(
              fontSize: 12,
              fontStyle: FontStyle.italic,
              color: Color(0xFF524440),
            ),
          ),
        ),

        const SizedBox(height: 24),
        const Text(
          'TIN NHẮN MỜI (TÙY CHỌN)',
          style: TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.bold,
            color: Color(0xFF524440),
            fontFamily: 'Quicksand',
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: _messageController,
          maxLines: 3,
          decoration: InputDecoration(
            hintText: 'Chào bạn, mình mời bạn cùng tham gia nhóm...',
            filled: true,
            fillColor: Colors.white,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: const BorderSide(color: Color(0xFFF2EAE4), width: 2),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: const BorderSide(color: Color(0xFFF2EAE4), width: 2),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: const BorderSide(color: Color(0xFFC98C7B), width: 2),
            ),
          ),
        ),

        const SizedBox(height: 24),
        Row(
          children: [
            Expanded(
              child: Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: const Color(0xFFF9F2EE),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(Icons.timer, color: Color(0xFF845143)),
                    SizedBox(height: 8),
                    Text(
                      'THỜI HẠN LỜI MỜI',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF524440),
                        fontFamily: 'Quicksand',
                      ),
                    ),
                    SizedBox(height: 4),
                    Text(
                      '7 ngày',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF2D2A28),
                        fontFamily: 'Quicksand',
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: const Color(0xFFF9F2EE),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(Icons.security, color: Color(0xFF845143)),
                    SizedBox(height: 8),
                    Text(
                      'QUYỀN DỮ LIỆU',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF524440),
                        fontFamily: 'Quicksand',
                      ),
                    ),
                    SizedBox(height: 4),
                    Text(
                      'Cài đặt sau',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF2D2A28),
                        fontFamily: 'Quicksand',
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }
}
