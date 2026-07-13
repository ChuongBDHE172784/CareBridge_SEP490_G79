import 'package:flutter/material.dart';
import '../models/care_group_model.dart';
import '../services/care_group_service.dart';

class ManageFamilyPermissionScreen extends StatefulWidget {
  final String groupId;
  final CareGroupMember member;

  const ManageFamilyPermissionScreen({
    super.key,
    required this.groupId,
    required this.member,
  });

  @override
  State<ManageFamilyPermissionScreen> createState() =>
      _ManageFamilyPermissionScreenState();
}

class _ManageFamilyPermissionScreenState
    extends State<ManageFamilyPermissionScreen> {
  final _service = CareGroupService();
  bool _isLoading = true;

  // Local state for switches before saving
  bool _calendar = false;
  bool _logs = false;
  bool _alerts = false;
  bool _records = false;

  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    _loadPermission();
  }

  Future<void> _loadPermission() async {
    setState(() => _isLoading = true);
    try {
      final perm = await _service.getFamilyPermission(
        widget.groupId,
        widget.member.memberId,
      );
      if (mounted) {
        setState(() {
          _calendar = perm.calendar;
          _logs = perm.logs;
          _alerts = perm.alerts;
          _records = perm.records;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi tải quyền: $e')));
      }
    }
  }

  Future<void> _saveChanges() async {
    setState(() => _isSaving = true);
    try {
      await _service.updateFamilyPermission(
        widget.groupId,
        widget.member.memberId,
        calendar: _calendar,
        logs: _logs,
        alerts: _alerts,
        records: _records,
      );
      if (mounted) {
        setState(() {
          _isSaving = false;
        });
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Đã cập nhật quyền hạn')));
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isSaving = false);
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi cập nhật: $e')));
      }
    }
  }

  Future<void> _removeMember() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Thu hồi quyền'),
        content: Text(
          'Bạn có chắc chắn muốn xóa ${widget.member.displayName} khỏi nhóm?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(
              foregroundColor: const Color(0xFFBA1A1A),
            ),
            child: const Text('Thu hồi'),
          ),
        ],
      ),
    );

    if (confirm == true && mounted) {
      try {
        await _service.removeMember(widget.groupId, widget.member.memberId);
        if (mounted) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(const SnackBar(content: Text('Đã xóa thành viên')));
          Navigator.pop(context, true); // Return true to refresh list
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
        }
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
          'Quản lý quyền hạn',
          style: TextStyle(
            color: Color(0xFF845143),
            fontSize: 22,
            fontWeight: FontWeight.bold,
            fontFamily: 'Quicksand',
          ),
        ),
      ),
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(color: Color(0xFFC98C7B)),
            )
          : SingleChildScrollView(
              padding: const EdgeInsets.symmetric(
                horizontal: 16.0,
                vertical: 8.0,
              ),
              child: Column(
                children: [
                  // Profile Header
                  Container(
                    padding: const EdgeInsets.all(24),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(16),
                      boxShadow: const [
                        BoxShadow(
                          color: Color(0x14C98C7B),
                          blurRadius: 20,
                          offset: Offset(0, 4),
                        ),
                      ],
                    ),
                    child: Column(
                      children: [
                        Container(
                          width: 80,
                          height: 80,
                          decoration: BoxDecoration(
                            color: const Color(0xFFFFE9E3),
                            shape: BoxShape.circle,
                            border: Border.all(
                              color: const Color(0xFFF2EAE4),
                              width: 4,
                            ),
                          ),
                          child: Center(
                            child: Text(
                              widget.member.displayName.isNotEmpty
                                  ? widget.member.displayName[0]
                                  : '?',
                              style: const TextStyle(
                                fontSize: 32,
                                fontWeight: FontWeight.bold,
                                color: Color(0xFF845143),
                                fontFamily: 'Quicksand',
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(height: 16),
                        Text(
                          widget.member.displayName,
                          style: const TextStyle(
                            fontSize: 24,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF845143),
                            fontFamily: 'Quicksand',
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          'Vai trò: ${widget.member.roleLabel}',
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w500,
                            color: Color(0xFF625D59),
                            fontFamily: 'Quicksand',
                          ),
                        ),
                        const SizedBox(height: 16),
                        OutlinedButton(
                          onPressed: _removeMember,
                          style: OutlinedButton.styleFrom(
                            foregroundColor: const Color(0xFFBA1A1A),
                            side: const BorderSide(
                              color: Color(0xFFBA1A1A),
                              width: 2,
                            ),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(999),
                            ),
                            padding: const EdgeInsets.symmetric(
                              horizontal: 24,
                              vertical: 12,
                            ),
                          ),
                          child: const Text(
                            'Thu hồi quyền',
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              fontFamily: 'Quicksand',
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 24),

                  // Permissions Grid
                  Container(
                    padding: const EdgeInsets.all(24),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(16),
                      boxShadow: const [
                        BoxShadow(
                          color: Color(0x14C98C7B),
                          blurRadius: 20,
                          offset: Offset(0, 4),
                        ),
                      ],
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Row(
                          children: [
                            Icon(Icons.security, color: Color(0xFF845143)),
                            SizedBox(width: 8),
                            Text(
                              'Quyền hạn chi tiết',
                              style: TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.bold,
                                color: Color(0xFF845143),
                                fontFamily: 'Quicksand',
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 24),

                        _buildPermissionSwitch(
                          title: 'Lịch trình',
                          subtitle:
                              'Xem và chỉnh sửa lịch tiêm chủng, khám định kỳ.',
                          icon: Icons.calendar_month,
                          value: _calendar,
                          onChanged: (val) => setState(() => _calendar = val),
                        ),
                        const SizedBox(height: 16),
                        _buildPermissionSwitch(
                          title: 'Việc cần làm & Thông báo',
                          subtitle:
                              'Quản lý danh sách nhiệm vụ và nhận cảnh báo chung.',
                          icon: Icons.checklist,
                          value: _alerts,
                          onChanged: (val) => setState(() => _alerts = val),
                        ),
                        const SizedBox(height: 16),
                        _buildPermissionSwitch(
                          title: 'Nhật ký bé',
                          subtitle:
                              'Xem lại các khoảnh khắc, hình ảnh và ghi chú hàng ngày.',
                          icon: Icons.auto_stories,
                          value: _logs,
                          onChanged: (val) => setState(() => _logs = val),
                        ),
                        const SizedBox(height: 16),
                        _buildPermissionSwitch(
                          title: 'Sức khỏe',
                          subtitle:
                              'Thông số chiều cao, cân nặng và tiền sử bệnh lý.',
                          icon: Icons.monitor_heart,
                          value: _records,
                          onChanged: (val) => setState(() => _records = val),
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 24),

                  // Summary
                  Container(
                    padding: const EdgeInsets.all(24),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF2EAE4),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Minh bạch & Đồng thuận',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF845143),
                            fontFamily: 'Quicksand',
                          ),
                        ),
                        const SizedBox(height: 16),
                        _buildSummaryRow(
                          Icons.check_circle,
                          'Quyền truy cập của ${widget.member.displayName} được giới hạn trong phạm vi gia đình.',
                        ),
                        const SizedBox(height: 8),
                        _buildSummaryRow(
                          Icons.history,
                          'Mọi thay đổi trong nhật ký sẽ được lưu vết với tên người thực hiện.',
                        ),
                        const SizedBox(height: 8),
                        _buildSummaryRow(
                          Icons.lock,
                          'Dữ liệu sức khỏe chỉ có thể được xem, không có quyền xuất tập tin.',
                        ),
                        const SizedBox(height: 24),
                        const Divider(color: Color(0x33845143)),
                        const SizedBox(height: 16),
                        const Text(
                          'Bằng cách nhấn "Lưu thay đổi", bạn đồng ý chia sẻ các dữ liệu đã chọn với thành viên này. Bạn có thể thu hồi quyền bất cứ lúc nào.',
                          style: TextStyle(
                            fontSize: 12,
                            fontStyle: FontStyle.italic,
                            color: Color(0xFF84736F),
                            fontFamily: 'Quicksand',
                          ),
                        ),
                        const SizedBox(height: 24),
                        ElevatedButton(
                          onPressed: _isSaving ? null : _saveChanges,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFF845143),
                            foregroundColor: Colors.white,
                            minimumSize: const Size(double.infinity, 56),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(999),
                            ),
                            elevation: 4,
                          ),
                          child: _isSaving
                              ? const CircularProgressIndicator(
                                  color: Colors.white,
                                )
                              : const Text(
                                  'Lưu thay đổi',
                                  style: TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.bold,
                                    fontFamily: 'Quicksand',
                                  ),
                                ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 48),
                ],
              ),
            ),
    );
  }

  Widget _buildPermissionSwitch({
    required String title,
    required String subtitle,
    required IconData icon,
    required bool value,
    required ValueChanged<bool> onChanged,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFFEF8F4),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: value ? const Color(0xFFC98C7B) : const Color(0xFFF2EAE4),
          width: 2,
        ),
      ),
      child: Row(
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: const Color(0x1A845143),
              borderRadius: BorderRadius.circular(24),
            ),
            child: Icon(icon, color: const Color(0xFF845143)),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF845143),
                    fontFamily: 'Quicksand',
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  subtitle,
                  style: const TextStyle(
                    fontSize: 12,
                    color: Color(0xFF625D59),
                    fontFamily: 'Quicksand',
                  ),
                ),
              ],
            ),
          ),
          Switch(
            value: value,
            onChanged: onChanged,
            activeThumbColor: Colors.white,
            activeTrackColor: const Color(0xFFC98C7B),
            inactiveThumbColor: Colors.white,
            inactiveTrackColor: const Color(0xFFDFD9D5),
          ),
        ],
      ),
    );
  }

  Widget _buildSummaryRow(IconData icon, String text) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 20, color: const Color(0xFF845143)),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            text,
            style: const TextStyle(
              fontSize: 14,
              color: Color(0xFF625D59),
              fontFamily: 'Quicksand',
            ),
          ),
        ),
      ],
    );
  }
}
