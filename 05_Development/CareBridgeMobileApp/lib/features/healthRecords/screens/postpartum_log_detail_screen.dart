import 'package:flutter/material.dart';

class PostpartumLogDetailScreen extends StatefulWidget {
  final String logId;

  const PostpartumLogDetailScreen({
    Key? key,
    required this.logId,
  }) : super(key: key);

  @override
  State<PostpartumLogDetailScreen> createState() => _PostpartumLogDetailScreenState();
}

class _PostpartumLogDetailScreenState extends State<PostpartumLogDetailScreen> {
  bool _isLoading = true;
  bool _isDeleting = false;
  Map<String, dynamic>? _detail;

  @override
  void initState() {
    super.initState();
    _fetchDetail();
  }

  Future<void> _fetchDetail() async {
    setState(() => _isLoading = true);
    try {
      // API call: GET /api/v1/postpartum-logs/{logId}
      await Future.delayed(const Duration(milliseconds: 700));
      _detail = {
        'title': 'Cảm giác hôm nay',
        'timeLabel': '08:30 Sáng • 12/10/2023',
        'mood': 'Khá tốt, có chút mệt mỏi vào buổi sáng.',
        'food': 'Ăn ngon miệng, đã uống đủ 2 lít nước.',
        'pain': ['Đau lưng nhẹ', 'Căng tức ngực'],
        'note': 'Bé ngủ ngoan hơn đêm qua nên mẹ được nghỉ ngơi nhiều hơn. Vết mổ đã khô và bớt đau rát.',
      };
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Lỗi tải thông tin chi tiết')),
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _deleteLog() async {
    setState(() => _isDeleting = true);
    try {
      // API call: DELETE /api/v1/postpartum-logs/{logId}
      await Future.delayed(const Duration(milliseconds: 600));
      if (mounted) {
        Navigator.pop(context, true); // true = deleted
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Không thể xóa bản ghi này')),
        );
      }
    } finally {
      if (mounted) setState(() => _isDeleting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    const primaryColor = Color(0xFF845143);
    const bgColor = Color(0xFFFFF8F6);
    const textColor = Color(0xFF271812);
    const errorColor = Color(0xFFBA1A1A);
    const secondaryColor = Color(0xFF6E5A52);

    return Scaffold(
      backgroundColor: bgColor,
      appBar: AppBar(
        backgroundColor: bgColor,
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: primaryColor),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Chi tiết phục hồi',
          style: TextStyle(
            color: primaryColor,
            fontWeight: FontWeight.bold,
            fontFamily: 'Quicksand',
            fontSize: 20,
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.edit, color: primaryColor),
            onPressed: () {
              // Edit log PATCH /api/v1/postpartum-logs/{logId}
            },
          ),
          IconButton(
            icon: _isDeleting 
              ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: errorColor))
              : const Icon(Icons.delete, color: errorColor),
            onPressed: _isDeleting ? null : _deleteLog,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: primaryColor))
          : _detail == null
              ? const Center(child: Text('Không có dữ liệu'))
              : SingleChildScrollView(
                  padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
                  child: Column(
                    children: [
                      // Header Info
                      Column(
                        children: [
                          Container(
                            width: 64,
                            height: 64,
                            decoration: const BoxDecoration(
                              color: Color(0xFFC98C7B), // primary-container
                              shape: BoxShape.circle,
                            ),
                            child: const Icon(Icons.favorite, color: Color(0xFF51271B), size: 32),
                          ),
                          const SizedBox(height: 8),
                          Text(
                            _detail!['title'],
                            style: const TextStyle(
                              fontSize: 24,
                              fontWeight: FontWeight.bold,
                              color: textColor,
                              fontFamily: 'Quicksand',
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            _detail!['timeLabel'],
                            style: const TextStyle(
                              fontSize: 16,
                              color: secondaryColor,
                              fontFamily: 'Quicksand',
                            ),
                          ),
                        ],
                      ),
                      
                      const SizedBox(height: 24),

                      // Details Card
                      Container(
                        padding: const EdgeInsets.all(20),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(16),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withOpacity(0.04),
                              blurRadius: 20,
                              offset: const Offset(0, 4),
                            ),
                          ],
                        ),
                        child: Column(
                          children: [
                            _buildDetailRow(
                              icon: Icons.mood,
                              iconBgColor: const Color(0xFFF6DACF),
                              iconColor: const Color(0xFF735E56),
                              title: 'Tâm trạng',
                              content: Text(_detail!['mood'], style: const TextStyle(fontSize: 16, color: textColor)),
                              showBorder: true,
                            ),
                            _buildDetailRow(
                              icon: Icons.restaurant,
                              iconBgColor: const Color(0xFFA09A95), // tertiary-container mock
                              iconColor: const Color(0xFF36322E),
                              title: 'Ăn uống',
                              content: Text(_detail!['food'], style: const TextStyle(fontSize: 16, color: textColor)),
                              showBorder: true,
                            ),
                            _buildDetailRow(
                              icon: Icons.local_hospital,
                              iconBgColor: const Color(0xFFFFDAD6),
                              iconColor: const Color(0xFF93000A),
                              title: 'Cơn đau/Khó chịu',
                              content: Wrap(
                                spacing: 8,
                                runSpacing: 8,
                                children: (_detail!['pain'] as List<String>).map((pain) {
                                  return Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                                    decoration: BoxDecoration(
                                      color: primaryColor.withOpacity(0.1),
                                      borderRadius: BorderRadius.circular(20),
                                    ),
                                    child: Text(
                                      pain,
                                      style: const TextStyle(
                                        color: primaryColor,
                                        fontSize: 14,
                                      ),
                                    ),
                                  );
                                }).toList(),
                              ),
                              showBorder: true,
                            ),
                            _buildDetailRow(
                              icon: Icons.notes,
                              iconBgColor: const Color(0xFFFADCD3), // surface-variant
                              iconColor: const Color(0xFF524440),
                              title: 'Ghi chú thêm',
                              content: Text(_detail!['note'], style: const TextStyle(fontSize: 16, color: textColor)),
                              showBorder: false,
                            ),
                          ],
                        ),
                      ),
                      
                      const SizedBox(height: 32),

                      // Action Area
                      OutlinedButton.icon(
                        onPressed: () {
                          // Medical support action
                        },
                        icon: const Icon(Icons.support_agent, color: primaryColor),
                        label: const Text(
                          'Cần hỗ trợ y tế?',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                            color: primaryColor,
                          ),
                        ),
                        style: OutlinedButton.styleFrom(
                          backgroundColor: Colors.white,
                          side: const BorderSide(color: primaryColor, width: 2),
                          minimumSize: const Size(double.infinity, 56),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(28),
                          ),
                          elevation: 2,
                          shadowColor: Colors.black.withOpacity(0.05),
                        ),
                      ),
                      const SizedBox(height: 12),
                      const Text(
                        'Nếu bạn cảm thấy đau bất thường, hãy liên hệ bác sĩ ngay.',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 14,
                          color: secondaryColor,
                        ),
                      ),
                    ],
                  ),
                ),
    );
  }

  Widget _buildDetailRow({
    required IconData icon,
    required Color iconBgColor,
    required Color iconColor,
    required String title,
    required Widget content,
    required bool showBorder,
  }) {
    return Container(
      padding: const EdgeInsets.only(bottom: 16),
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        border: showBorder
            ? const Border(bottom: BorderSide(color: Color(0xFFD6C2BD)))
            : null,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: iconBgColor,
              shape: BoxShape.circle,
            ),
            child: Icon(icon, color: iconColor, size: 20),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontSize: 14,
                    color: Color(0xFF6E5A52),
                    fontFamily: 'Quicksand',
                  ),
                ),
                const SizedBox(height: 4),
                content,
              ],
            ),
          ),
        ],
      ),
    );
  }
}
