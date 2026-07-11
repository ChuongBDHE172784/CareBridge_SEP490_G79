import 'package:flutter/material.dart';
import '../services/care_group_service.dart';

class LeaveCareGroupConfirmationScreen extends StatefulWidget {
  final String groupId;
  final String groupName;

  const LeaveCareGroupConfirmationScreen({
    Key? key,
    required this.groupId,
    required this.groupName,
  }) : super(key: key);

  @override
  State<LeaveCareGroupConfirmationScreen> createState() => _LeaveCareGroupConfirmationScreenState();
}

class _LeaveCareGroupConfirmationScreenState extends State<LeaveCareGroupConfirmationScreen> {
  final _service = CareGroupService();
  bool _isLoading = false;

  Future<void> _leave() async {
    setState(() => _isLoading = true);
    try {
      await _service.leaveGroup(widget.groupId);
      if (mounted) {
        Navigator.pop(context, true); // Return success
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
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
      backgroundColor: const Color(0xFFF6F1EC),
      appBar: AppBar(
        backgroundColor: const Color(0xFFF6F1EC),
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Color(0xFF524440)),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text('CareBridge', style: TextStyle(color: Color(0xFF845143), fontSize: 24, fontWeight: FontWeight.bold, fontFamily: 'Quicksand')),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 48.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                  boxShadow: const [BoxShadow(color: Color(0x265A463F), blurRadius: 20, offset: Offset(0, 4))],
                ),
                child: Column(
                  children: [
                    Container(
                      width: 64,
                      height: 64,
                      decoration: const BoxDecoration(color: Color(0xFFFFDAD6), shape: BoxShape.circle),
                      child: const Icon(Icons.warning, color: Color(0xFF93000A), size: 36),
                    ),
                    const SizedBox(height: 24),
                    Text(
                      'Rời nhóm "${widget.groupName}"?',
                      style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Color(0xFF271812), fontFamily: 'Quicksand'),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Bạn đang chuẩn bị rời khỏi nhóm chăm sóc. Hành động này không thể hoàn tác.',
                      style: TextStyle(fontSize: 14, color: Color(0xFF524440), fontFamily: 'Quicksand'),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 24),
                    _buildWarningItem(
                      icon: Icons.no_accounts,
                      title: 'Mất quyền truy cập',
                      desc: 'Bạn sẽ không thể xem hồ sơ sức khỏe, lịch biểu và các công việc chung của gia đình nữa.',
                    ),
                    const SizedBox(height: 16),
                    _buildWarningItem(
                      icon: Icons.assignment_late,
                      title: 'Công việc chưa hoàn thành',
                      desc: 'Các công việc đang được giao cho bạn sẽ được chuyển lại cho trưởng nhóm.',
                    ),
                    const SizedBox(height: 32),
                    ElevatedButton(
                      onPressed: _isLoading ? null : _leave,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFFC98C7B),
                        foregroundColor: Colors.white,
                        minimumSize: const Size(double.infinity, 48),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
                        shadowColor: const Color(0x4DC98C7B),
                        elevation: 4,
                      ),
                      child: _isLoading
                          ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                          : const Text('Rời nhóm', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, fontFamily: 'Quicksand')),
                    ),
                    const SizedBox(height: 12),
                    OutlinedButton(
                      onPressed: _isLoading ? null : () => Navigator.pop(context),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: const Color(0xFFC98C7B),
                        side: const BorderSide(color: Color(0xFFC98C7B), width: 2),
                        minimumSize: const Size(double.infinity, 48),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
                      ),
                      child: const Text('Quay lại', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, fontFamily: 'Quicksand')),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildWarningItem({required IconData icon, required String title, required String desc}) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, color: const Color(0xFFBA1A1A), size: 24),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF271812), fontSize: 16, fontFamily: 'Quicksand')),
              Text(desc, style: const TextStyle(fontSize: 14, color: Color(0xFF524440), fontFamily: 'Quicksand')),
            ],
          ),
        ),
      ],
    );
  }
}
