import 'package:flutter/material.dart';
import '../services/care_group_service.dart';

class RejectInvitationConfirmationScreen extends StatefulWidget {
  final String groupId;
  final String groupName;

  const RejectInvitationConfirmationScreen({
    super.key,
    required this.groupId,
    required this.groupName,
  });

  @override
  State<RejectInvitationConfirmationScreen> createState() =>
      _RejectInvitationConfirmationScreenState();
}

class _RejectInvitationConfirmationScreenState
    extends State<RejectInvitationConfirmationScreen> {
  final _service = CareGroupService();
  bool _isLoading = false;

  Future<void> _decline() async {
    setState(() => _isLoading = true);
    try {
      await _service.declineInvite(widget.groupId);
      if (mounted) {
        Navigator.pop(context, true); // Return success
      }
    } catch (e) {
      if (mounted) {
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFEF8F4),
      appBar: AppBar(
        backgroundColor: const Color(0xFFFEF8F4),
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.close, color: Color(0xFF524440)),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(32.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Stack(
                alignment: Alignment.bottomCenter,
                clipBehavior: Clip.none,
                children: [
                  Container(
                    width: 112,
                    height: 112,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      border: Border.all(color: Colors.white, width: 4),
                      boxShadow: const [
                        BoxShadow(
                          color: Color(0x26C98C7B),
                          blurRadius: 20,
                          offset: Offset(0, 4),
                        ),
                      ],
                      image: const DecorationImage(
                        image: NetworkImage(
                          'https://lh3.googleusercontent.com/aida-public/AB6AXuDRh8M169EA9xdx-_TI9YHGEj_WRRsBtKv0dpxmVjZ9RAXECGNG9rxTEkhywVkfxNV1jW-08v1HZ27bMgtLppMwHi-m3RTNV_hF7gbKAgSx8HPkKbz00mdKitmaJuDc88hChxFFyjYovB9KeLVFzPhKtx9ryL7lpZ8IeOVJLaTyrANHoJl0It6XF8YfXIDGu85mg-sG3Bk0d_jbatqYU0J1G4qQ83zM0hOZTzAvfUwr1L69rRfVSMm1hReaTvbZcdDys091R2xzVNM',
                        ),
                        fit: BoxFit.cover,
                      ),
                    ),
                  ),
                  Positioned(
                    bottom: -12,
                    child: Container(
                      width: 48,
                      height: 48,
                      decoration: const BoxDecoration(
                        color: Colors.white,
                        shape: BoxShape.circle,
                      ),
                      padding: const EdgeInsets.all(4),
                      child: Container(
                        decoration: const BoxDecoration(
                          color: Color(0xFFFFDAD6),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.person_remove,
                          color: Color(0xFF93000A),
                          size: 24,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 48),
              const Text(
                'Từ chối lời mời?',
                style: TextStyle(
                  fontSize: 32,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF2D2A28),
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: const Color(0xFFE7E1DD)),
                  boxShadow: const [
                    BoxShadow(
                      color: Color(0x0DC98C7B),
                      blurRadius: 20,
                      offset: Offset(0, 4),
                    ),
                  ],
                ),
                child: Column(
                  children: [
                    RichText(
                      textAlign: TextAlign.center,
                      text: TextSpan(
                        style: const TextStyle(
                          fontSize: 14,
                          color: Color(0xFF524F4C),
                          fontFamily: 'Quicksand',
                          fontWeight: FontWeight.w500,
                        ),
                        children: [
                          const TextSpan(
                            text: 'Bạn đang từ chối tham gia nhóm chăm sóc ',
                          ),
                          TextSpan(
                            text: widget.groupName,
                            style: const TextStyle(
                              fontWeight: FontWeight.bold,
                              color: Color(0xFF2D2A28),
                            ),
                          ),
                          const TextSpan(text: '.'),
                        ],
                      ),
                    ),
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 12.0),
                      child: Divider(color: Color(0xFFE7E1DD)),
                    ),
                    const Text(
                      'Nếu từ chối, bạn sẽ không nhận được thông báo về lịch trình y tế, nhắc nhở thuốc, hay các cập nhật sức khỏe quan trọng từ nhóm này.',
                      style: TextStyle(fontSize: 14, color: Color(0xFF605E5A)),
                      textAlign: TextAlign.center,
                    ),
                  ],
                ),
              ),
              const Spacer(),
              ElevatedButton.icon(
                onPressed: _isLoading ? null : _decline,
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFBA1A1A),
                  foregroundColor: Colors.white,
                  minimumSize: const Size(double.infinity, 56),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(999),
                  ),
                ),
                icon: _isLoading
                    ? const SizedBox.shrink()
                    : const Icon(Icons.block, size: 20),
                label: _isLoading
                    ? const SizedBox(
                        width: 24,
                        height: 24,
                        child: CircularProgressIndicator(
                          color: Colors.white,
                          strokeWidth: 2,
                        ),
                      )
                    : const Text(
                        'XÁC NHẬN TỪ CHỐI',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          letterSpacing: 0.5,
                        ),
                      ),
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: _isLoading ? null : () => Navigator.pop(context),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFE7E1DD),
                  foregroundColor: const Color(0xFF524440),
                  minimumSize: const Size(double.infinity, 56),
                  elevation: 0,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(999),
                  ),
                ),
                child: const Text(
                  'QUAY LẠI',
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 0.5,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
