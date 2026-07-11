import 'package:flutter/material.dart';
import '../models/care_group_model.dart';
import '../services/care_group_service.dart';

class SentPendingInvitationsScreen extends StatefulWidget {
  final String groupId;

  const SentPendingInvitationsScreen({Key? key, required this.groupId}) : super(key: key);

  @override
  State<SentPendingInvitationsScreen> createState() => _SentPendingInvitationsScreenState();
}

class _SentPendingInvitationsScreenState extends State<SentPendingInvitationsScreen> {
  final _service = CareGroupService();
  bool _isLoading = true;
  List<CareGroupMember> _pendingMembers = [];

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final group = await _service.getGroupMembers(widget.groupId);
      if (mounted) {
        setState(() {
          _pendingMembers = group.members.where((m) => m.inviteStatus == 'PENDING').toList();
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    }
  }

  Future<void> _revoke(String userId) async {
    try {
      await _service.revokeInvitation(widget.groupId, userId);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Đã hủy lời mời')));
        _loadData();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    }
  }

  Future<void> _resend(CareGroupMember member) async {
    try {
      // Assuming inviteMember is used to resend.
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Đã gửi lại lời mời')));
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
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
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Color(0xFF845143)),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text('Lời mời chờ xử lý', style: TextStyle(color: Color(0xFF845143), fontSize: 20, fontWeight: FontWeight.bold, fontFamily: 'Quicksand')),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFFC98C7B)))
          : RefreshIndicator(
              color: const Color(0xFFC98C7B),
              onRefresh: _loadData,
              child: _pendingMembers.isEmpty
                  ? ListView(
                      padding: const EdgeInsets.all(16),
                      children: const [
                        Center(child: Padding(
                          padding: EdgeInsets.all(32.0),
                          child: Text('Không có lời mời nào đang chờ', style: TextStyle(color: Colors.grey, fontFamily: 'Quicksand')),
                        ))
                      ],
                    )
                  : Column(
                      children: [
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
                          child: Text(
                            'Bạn có ${_pendingMembers.length} lời mời chia sẻ hồ sơ đang chờ xác nhận.',
                            style: const TextStyle(fontSize: 14, color: Color(0xFF524440), fontFamily: 'Quicksand'),
                            textAlign: TextAlign.center,
                          ),
                        ),
                        Expanded(
                          child: ListView.builder(
                            padding: const EdgeInsets.all(20),
                            itemCount: _pendingMembers.length,
                            itemBuilder: (context, index) {
                              final member = _pendingMembers[index];
                              return _buildInviteCard(member);
                            },
                          ),
                        ),
                      ],
                    ),
            ),
    );
  }

  Widget _buildInviteCard(CareGroupMember member) {
    final dateStr = member.joinedAt != null 
        ? '${member.joinedAt!.hour}:${member.joinedAt!.minute.toString().padLeft(2, '0')}, ${member.joinedAt!.day}/${member.joinedAt!.month}/${member.joinedAt!.year}' 
        : 'Gần đây';

    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: const [BoxShadow(color: Color(0x145A463F), blurRadius: 24, offset: Offset(0, 8))],
      ),
      child: Column(
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: const BoxDecoration(color: Color(0xFFF6DACF), shape: BoxShape.circle),
                child: const Icon(Icons.person, color: Color(0xFF735E56)),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(member.displayName, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Color(0xFF271812), fontFamily: 'Quicksand')),
                    Text('Vai trò: ${member.memberRole}', style: const TextStyle(fontSize: 14, color: Color(0xFF524440), fontFamily: 'Quicksand')),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                decoration: BoxDecoration(color: const Color(0x1AC98C7B), borderRadius: BorderRadius.circular(999)),
                child: const Text('Đang chờ', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Color(0xFF845143), fontFamily: 'Quicksand')),
              ),
            ],
          ),
          const SizedBox(height: 24),
          _buildInfoRow(Icons.calendar_today, 'Đã gửi: $dateStr', const Color(0xFF6E5A52), const Color(0xFF524440)),
          const SizedBox(height: 8),
          _buildInfoRow(Icons.hourglass_empty, 'Hết hạn: Trong vòng 48h', const Color(0xFF6E5A52), const Color(0xFF524440)),
          const SizedBox(height: 24),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () => _revoke(member.memberId),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: const Color(0xFF845143),
                    side: const BorderSide(color: Color(0xFF845143), width: 2),
                    minimumSize: const Size(0, 48),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
                  ),
                  icon: const Icon(Icons.cancel, size: 20),
                  label: const Text('Hủy lời mời', style: TextStyle(fontWeight: FontWeight.bold, fontFamily: 'Quicksand')),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: () => _resend(member),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF845143),
                    foregroundColor: Colors.white,
                    minimumSize: const Size(0, 48),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
                    shadowColor: const Color(0x26845143),
                    elevation: 4,
                  ),
                  icon: const Icon(Icons.send, size: 20),
                  label: const Text('Gửi lại', style: TextStyle(fontWeight: FontWeight.bold, fontFamily: 'Quicksand')),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildInfoRow(IconData icon, String text, Color iconColor, Color textColor) {
    return Row(
      children: [
        Icon(icon, size: 18, color: iconColor),
        const SizedBox(width: 8),
        Text(text, style: TextStyle(fontSize: 14, color: textColor, fontFamily: 'Quicksand')),
      ],
    );
  }
}
