import 'package:flutter/material.dart';
import '../services/baby_log_service.dart';

class DevelopmentMilestoneDetailScreen extends StatefulWidget {
  final String babyId;
  final String milestoneId;

  const DevelopmentMilestoneDetailScreen({
    super.key,
    required this.babyId,
    required this.milestoneId,
  });

  @override
  State<DevelopmentMilestoneDetailScreen> createState() =>
      _DevelopmentMilestoneDetailScreenState();
}

class _DevelopmentMilestoneDetailScreenState
    extends State<DevelopmentMilestoneDetailScreen> {
  final _service = BabyLogService();
  bool _isLoading = true;
  bool _isDeleting = false;
  Map<String, dynamic>? _milestoneDetail;

  @override
  void initState() {
    super.initState();
    _fetchMilestoneDetail();
  }

  Future<void> _fetchMilestoneDetail() async {
    setState(() => _isLoading = true);
    try {
      // API call: GET /api/v1/babies/{babyId}/milestones/{milestoneId}
      // final response = await apiGet('/api/v1/babies/${widget.babyId}/milestones/${widget.milestoneId}');

      // MOCK data for UI representation
      await Future.delayed(const Duration(milliseconds: 700));
      _milestoneDetail = {
        'title': 'Lần đầu tiên ngồi vững',
        'category': 'Vận động',
        'description':
            'Bé đã tự ngồi được không cần đỡ, một cột mốc tuyệt vời!',
        'dateAchieved': '15 Tháng 10, 2023',
        'ageAtMilestone': '6 Tháng 2 Tuần',
        'motherNote':
            'Hôm nay lúc đang chơi trên thảm, bé tự dưng buông tay khỏi đồ chơi và ngồi vững được một lúc lâu. Nhìn con lớn lên từng ngày thật sự rất hạnh phúc.',
        'imageUrl':
            'https://images.unsplash.com/photo-1519689680058-324335c77eba?auto=format&fit=crop&q=80&w=800',
        'mediaCount': 3,
      };
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Lỗi tải thông tin cột mốc')),
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _deleteMilestone() async {
    setState(() => _isDeleting = true);
    try {
      await _service.deleteMilestone(widget.babyId, widget.milestoneId);
      if (mounted) {
        Navigator.pop(context, true); // true indicates deleted
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Không thể xóa cột mốc này')),
        );
      }
    } finally {
      if (mounted) setState(() => _isDeleting = false);
    }
  }

  Future<void> _editMilestoneNote() async {
    final controller = TextEditingController(
      text: _milestoneDetail?['motherNote']?.toString() ?? '',
    );

    final note = await showDialog<String>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Chỉnh sửa ghi chú'),
        content: TextField(
          controller: controller,
          maxLines: 4,
          decoration: const InputDecoration(
            labelText: 'Ghi chú',
            border: OutlineInputBorder(),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, controller.text.trim()),
            child: const Text('Lưu'),
          ),
        ],
      ),
    );
    controller.dispose();

    if (note == null) return;

    try {
      final updated = await _service.updateMilestone(
        widget.babyId,
        widget.milestoneId,
        note: note,
      );
      if (!mounted) return;
      setState(() {
        _milestoneDetail = {
          ...?_milestoneDetail,
          'motherNote': updated.note ?? note,
        };
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã cập nhật cột mốc')),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Không thể cập nhật cột mốc. $e')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    const primaryColor = Color(0xFFC98C7B);
    const bgColor = Color(0xFFF6F1EC);
    const textColor = Color(0xFF5A463F);
    const errorColor = Color(0xFFBA1A1A);

    return Scaffold(
      backgroundColor: bgColor,
      appBar: AppBar(
        backgroundColor: bgColor,
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: textColor),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Chi tiết Cột mốc',
          style: TextStyle(
            color: textColor,
            fontWeight: FontWeight.bold,
            fontFamily: 'Quicksand',
            fontSize: 20,
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.close, color: textColor),
            onPressed: () => Navigator.pop(context),
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: primaryColor))
          : _milestoneDetail == null
              ? const Center(child: Text('Không tìm thấy dữ liệu'))
              : SingleChildScrollView(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
                  child: Column(
                    children: [
                      // Hero Card
                      Container(
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(24),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withAlpha(10),
                              blurRadius: 20,
                              offset: const Offset(0, 4),
                            ),
                          ],
                        ),
                        clipBehavior: Clip.antiAlias,
                        margin: const EdgeInsets.only(bottom: 24),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            // Image Section
                            Stack(
                              children: [
                                Image.network(
                                  _milestoneDetail!['imageUrl'],
                                  height: 240,
                                  width: double.infinity,
                                  fit: BoxFit.cover,
                                ),
                                Positioned(
                                  bottom: 16,
                                  right: 16,
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 12,
                                      vertical: 8,
                                    ),
                                    decoration: BoxDecoration(
                                      color: Colors.white.withAlpha(216),
                                      borderRadius: BorderRadius.circular(20),
                                    ),
                                    child: Row(
                                      mainAxisSize: MainAxisSize.min,
                                      children: [
                                        const Icon(
                                          Icons.photo_library,
                                          color: primaryColor,
                                          size: 18,
                                        ),
                                        const SizedBox(width: 4),
                                        Text(
                                          '${_milestoneDetail!['mediaCount']} Ảnh',
                                          style: const TextStyle(
                                            fontWeight: FontWeight.bold,
                                            fontSize: 12,
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            // Content Section
                            Padding(
                              padding: const EdgeInsets.all(20),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    mainAxisAlignment:
                                        MainAxisAlignment.spaceBetween,
                                    children: [
                                      Container(
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 12,
                                          vertical: 6,
                                        ),
                                        decoration: BoxDecoration(
                                          color: primaryColor,
                                          borderRadius:
                                              BorderRadius.circular(16),
                                        ),
                                        child: Text(
                                          (_milestoneDetail!['category']
                                                  as String)
                                              .toUpperCase(),
                                          style: const TextStyle(
                                            color: Colors.white,
                                            fontSize: 10,
                                            fontWeight: FontWeight.bold,
                                            letterSpacing: 0.5,
                                          ),
                                        ),
                                      ),
                                      InkWell(
                                        onTap: _editMilestoneNote,
                                        child: Container(
                                          padding: const EdgeInsets.all(8),
                                          decoration: BoxDecoration(
                                            color: const Color(0xFFFFE9E3),
                                            borderRadius:
                                                BorderRadius.circular(20),
                                          ),
                                          child: const Icon(
                                            Icons.edit,
                                            color: primaryColor,
                                            size: 20,
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 12),
                                  Text(
                                    _milestoneDetail!['title'],
                                    style: const TextStyle(
                                      fontSize: 24,
                                      fontWeight: FontWeight.bold,
                                      color: textColor,
                                      fontFamily: 'Quicksand',
                                    ),
                                  ),
                                  const SizedBox(height: 8),
                                  Text(
                                    _milestoneDetail!['description'],
                                    style: const TextStyle(
                                      fontSize: 16,
                                      color: Color(0xFF9C857C),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),

                      // Grid Stats
                      Row(
                        children: [
                          Expanded(
                            child: _buildGridStatCard(
                              icon: Icons.calendar_today,
                              label: 'Ngày đạt được',
                              value: _milestoneDetail!['dateAchieved'],
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: _buildGridStatCard(
                              icon: Icons.child_care,
                              label: 'Tuổi của bé',
                              value: _milestoneDetail!['ageAtMilestone'],
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 24),

                      // Notes Section
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(20),
                        decoration: BoxDecoration(
                          color: const Color(0xFFFFF1EC),
                          borderRadius: BorderRadius.circular(24),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withAlpha(5),
                              blurRadius: 10,
                              offset: const Offset(0, 2),
                            ),
                          ],
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: const [
                                Icon(Icons.sticky_note_2, color: primaryColor),
                                SizedBox(width: 8),
                                Text(
                                  'Ghi chú của mẹ',
                                  style: TextStyle(
                                    fontSize: 18,
                                    fontWeight: FontWeight.bold,
                                    color: textColor,
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 12),
                            Text(
                              _milestoneDetail!['motherNote'] ??
                                  'Chưa có ghi chú.',
                              style: const TextStyle(
                                fontSize: 16,
                                color: Color(0xFF524440),
                                height: 1.5,
                              ),
                            ),
                          ],
                        ),
                      ),

                      const SizedBox(height: 32),

                      // Danger Zone Delete
                      TextButton.icon(
                        onPressed: _isDeleting ? null : _deleteMilestone,
                        icon: _isDeleting
                            ? const SizedBox(
                                width: 20,
                                height: 20,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: errorColor,
                                ),
                              )
                            : const Icon(Icons.delete, color: errorColor),
                        label: Text(
                          _isDeleting ? 'Đang xóa...' : 'Xóa cột mốc này',
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            color: errorColor,
                          ),
                        ),
                        style: TextButton.styleFrom(
                          padding: const EdgeInsets.symmetric(
                            vertical: 12,
                            horizontal: 24,
                          ),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(24),
                            side: BorderSide(
                              color: errorColor.withAlpha(76),
                            ),
                          ),
                          backgroundColor: Colors.transparent,
                        ),
                      ),
                      const SizedBox(height: 32),
                    ],
                  ),
                ),
    );
  }

  Widget _buildGridStatCard({
    required IconData icon,
    required String label,
    required String value,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 20, horizontal: 12),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF1EC),
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withAlpha(5),
            blurRadius: 10,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: const BoxDecoration(
              color: Color(0xFFFFE9E3),
              shape: BoxShape.circle,
            ),
            child: Icon(icon, color: const Color(0xFFC98C7B), size: 24),
          ),
          const SizedBox(height: 12),
          Text(
            label.toUpperCase(),
            style: const TextStyle(
              fontSize: 10,
              fontWeight: FontWeight.bold,
              color: Color(0xFF9C857C),
              letterSpacing: 0.5,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 4),
          Text(
            value,
            style: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.bold,
              color: Color(0xFF5A463F),
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}
