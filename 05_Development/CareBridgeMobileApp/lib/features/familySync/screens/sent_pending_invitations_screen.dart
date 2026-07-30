import 'package:flutter/material.dart';
import '../models/care_group_model.dart';
import '../services/care_group_service.dart';

class SentPendingInvitationsScreen extends StatefulWidget {
  final String groupId;

  const SentPendingInvitationsScreen({super.key, required this.groupId});

  @override
  State<SentPendingInvitationsScreen> createState() =>
      _SentPendingInvitationsScreenState();
}

class _SentPendingInvitationsScreenState
    extends State<SentPendingInvitationsScreen>
    with SingleTickerProviderStateMixin {
  final _service = CareGroupService();
  bool _isLoading = true;
  List<CareGroupMember> _pendingMembers = [];
  List<JoinRequest> _joinRequests = [];
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _loadData();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final group = await _service.getGroupMembers(widget.groupId);
      final requests = await _service.listJoinRequests(widget.groupId);
      if (mounted) {
        setState(() {
          _pendingMembers = group.members
              .where((m) => m.inviteStatus == 'PENDING' && !m.isJoinRequest)
              .toList();
          _joinRequests = requests;
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
    }
  }

  Future<void> _revoke(String userId) async {
    try {
      await _service.revokeInvitation(widget.groupId, userId);
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Đã hủy lời mời')));
        _loadData();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    }
  }

  Future<void> _resend(CareGroupMember member) async {
    try {
      // Assuming inviteMember is used to resend.
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Đã gửi lại lời mời')));
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    }
  }

  Future<void> _handleJoinResponse(String memberId, bool approve) async {
    try {
      await _service.respondJoinRequest(widget.groupId, memberId, approve);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(approve
                ? 'Đã chấp nhận yêu cầu tham gia'
                : 'Đã từ chối yêu cầu tham gia'),
          ),
        );
        _loadData();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Lỗi: $e')),
        );
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
        title: const Text(
          'Lời mời chờ xử lý',
          style: TextStyle(
            color: Color(0xFF845143),
            fontSize: 20,
            fontWeight: FontWeight.bold,
            fontFamily: 'Quicksand',
          ),
        ),
        bottom: TabBar(
          controller: _tabController,
          labelColor: const Color(0xFF845143),
          unselectedLabelColor: const Color(0xFF845143).withAlpha(128),
          indicatorColor: const Color(0xFF845143),
          labelStyle: const TextStyle(fontWeight: FontWeight.bold, fontFamily: 'Quicksand'),
          unselectedLabelStyle: const TextStyle(fontFamily: 'Quicksand'),
          tabs: const [
            Tab(text: 'Lời mời đã gửi'),
            Tab(text: 'Yêu cầu tham gia'),
          ],
        ),
      ),
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(color: Color(0xFFC98C7B)),
            )
          : TabBarView(
              controller: _tabController,
              children: [
                _buildSentInvitesTab(),
                _buildJoinRequestsTab(),
              ],
            ),
    );
  }

  Widget _buildSentInvitesTab() {
    return RefreshIndicator(
      color: const Color(0xFFC98C7B),
      onRefresh: _loadData,
      child: _pendingMembers.isEmpty
          ? ListView(
              padding: const EdgeInsets.all(16),
              children: const [
                Center(
                  child: Padding(
                    padding: EdgeInsets.all(32.0),
                    child: Text(
                      'Không có lời mời nào đang chờ',
                      style: TextStyle(
                        color: Colors.grey,
                        fontFamily: 'Quicksand',
                      ),
                    ),
                  ),
                ),
              ],
            )
          : Column(
              children: [
                Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 20,
                    vertical: 12,
                  ),
                  child: Text(
                    'Bạn có ${_pendingMembers.length} lời mời chia sẻ hồ sơ đang chờ xác nhận.',
                    style: const TextStyle(
                      fontSize: 14,
                      color: Color(0xFF524440),
                      fontFamily: 'Quicksand',
                    ),
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
    );
  }

  Widget _buildJoinRequestsTab() {
    return RefreshIndicator(
      color: const Color(0xFFC98C7B),
      onRefresh: _loadData,
      child: _joinRequests.isEmpty
          ? ListView(
              padding: const EdgeInsets.all(16),
              children: const [
                Center(
                  child: Padding(
                    padding: EdgeInsets.all(32.0),
                    child: Text(
                      'Không có yêu cầu tham gia nào',
                      style: TextStyle(
                        color: Colors.grey,
                        fontFamily: 'Quicksand',
                      ),
                    ),
                  ),
                ),
              ],
            )
          : Column(
              children: [
                Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 20,
                    vertical: 12,
                  ),
                  child: Text(
                    'Có ${_joinRequests.length} yêu cầu tham gia nhóm của bạn.',
                    style: const TextStyle(
                      fontSize: 14,
                      color: Color(0xFF524440),
                      fontFamily: 'Quicksand',
                    ),
                    textAlign: TextAlign.center,
                  ),
                ),
                Expanded(
                  child: ListView.builder(
                    padding: const EdgeInsets.all(20),
                    itemCount: _joinRequests.length,
                    itemBuilder: (context, index) {
                      final req = _joinRequests[index];
                      return _buildJoinRequestCard(req);
                    },
                  ),
                ),
              ],
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
        boxShadow: const [
          BoxShadow(
            color: Color(0x145A463F),
            blurRadius: 24,
            offset: Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: const BoxDecoration(
                  color: Color(0xFFF6DACF),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.person, color: Color(0xFF735E56)),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      member.displayName,
                      style: const TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF271812),
                        fontFamily: 'Quicksand',
                      ),
                    ),
                    Text(
                      'Vai trò: ${member.memberRole}',
                      style: const TextStyle(
                        fontSize: 14,
                        color: Color(0xFF524440),
                        fontFamily: 'Quicksand',
                      ),
                    ),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  color: const Color(0x1AC98C7B),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: const Text(
                  'Đang chờ',
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF845143),
                    fontFamily: 'Quicksand',
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 24),
          _buildInfoRow(
            Icons.calendar_today,
            'Đã gửi: $dateStr',
            const Color(0xFF6E5A52),
            const Color(0xFF524440),
          ),
          const SizedBox(height: 8),
          _buildInfoRow(
            Icons.hourglass_empty,
            'Hết hạn: Trong vòng 48h',
            const Color(0xFF6E5A52),
            const Color(0xFF524440),
          ),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: () => _revoke(member.userId ?? member.memberId),
              style: OutlinedButton.styleFrom(
                foregroundColor: const Color(0xFF845143),
                side: const BorderSide(color: Color(0xFF845143), width: 2),
                minimumSize: const Size(0, 48),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(999),
                ),
              ),
              icon: const Icon(Icons.cancel, size: 20),
              label: const Text(
                'Hủy lời mời',
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontFamily: 'Quicksand',
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildJoinRequestCard(JoinRequest req) {
    final dateStr = req.requestedAt != null
        ? '${req.requestedAt!.hour}:${req.requestedAt!.minute.toString().padLeft(2, '0')}, ${req.requestedAt!.day}/${req.requestedAt!.month}/${req.requestedAt!.year}'
        : 'Gần đây';

    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: const [
          BoxShadow(
            color: Color(0x145A463F),
            blurRadius: 24,
            offset: Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: const BoxDecoration(
                  color: Color(0xFFF6DACF),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.person, color: Color(0xFF735E56)),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      req.displayName,
                      style: const TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF271812),
                        fontFamily: 'Quicksand',
                      ),
                    ),
                    if (req.email != null)
                      Text(
                        req.email!,
                        style: const TextStyle(
                          fontSize: 14,
                          color: Color(0xFF524440),
                          fontFamily: 'Quicksand',
                        ),
                      ),
                    if (req.phone != null)
                      Text(
                        req.phone!,
                        style: const TextStyle(
                          fontSize: 14,
                          color: Color(0xFF524440),
                          fontFamily: 'Quicksand',
                        ),
                      ),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  color: const Color(0x1AC98C7B),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: const Text(
                  'Yêu cầu',
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF845143),
                    fontFamily: 'Quicksand',
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          _buildInfoRow(
            Icons.calendar_today,
            'Yêu cầu lúc: $dateStr',
            const Color(0xFF6E5A52),
            const Color(0xFF524440),
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () => _handleJoinResponse(req.memberId, false),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: const Color(0xFFBA1A1A),
                    side: const BorderSide(color: Color(0xFFBA1A1A), width: 2),
                    minimumSize: const Size(0, 48),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(999),
                    ),
                  ),
                  icon: const Icon(Icons.close, size: 20),
                  label: const Text(
                    'Từ chối',
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontFamily: 'Quicksand',
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: () => _handleJoinResponse(req.memberId, true),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF845143),
                    foregroundColor: Colors.white,
                    minimumSize: const Size(0, 48),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(999),
                    ),
                    shadowColor: const Color(0x26845143),
                    elevation: 4,
                  ),
                  icon: const Icon(Icons.check, size: 20),
                  label: const Text(
                    'Chấp nhận',
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontFamily: 'Quicksand',
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildInfoRow(
    IconData icon,
    String text,
    Color iconColor,
    Color textColor,
  ) {
    return Row(
      children: [
        Icon(icon, size: 18, color: iconColor),
        const SizedBox(width: 8),
        Text(
          text,
          style: TextStyle(
            fontSize: 14,
            color: textColor,
            fontFamily: 'Quicksand',
          ),
        ),
      ],
    );
  }
}
