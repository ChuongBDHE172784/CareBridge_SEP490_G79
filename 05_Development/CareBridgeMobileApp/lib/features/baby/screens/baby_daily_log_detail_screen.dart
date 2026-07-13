import 'package:flutter/material.dart';

class BabyDailyLogDetailScreen extends StatefulWidget {
  final String babyId;
  final String logId;

  const BabyDailyLogDetailScreen({
    super.key,
    required this.babyId,
    required this.logId,
  });

  @override
  State<BabyDailyLogDetailScreen> createState() =>
      _BabyDailyLogDetailScreenState();
}

class _BabyDailyLogDetailScreenState extends State<BabyDailyLogDetailScreen> {
  bool _isLoading = true;
  bool _isDeleting = false;
  Map<String, dynamic>? _logDetail;

  @override
  void initState() {
    super.initState();
    _fetchLogDetail();
  }

  Future<void> _fetchLogDetail() async {
    setState(() => _isLoading = true);
    try {
      // API call: GET /api/v1/babies/{babyId}/daily-logs/{logId}
      // final response = await apiGet('/api/v1/babies/${widget.babyId}/daily-logs/${widget.logId}');

      // MOCK data for UI representation
      await Future.delayed(const Duration(milliseconds: 800));
      _logDetail = {
        'title': 'Bú sữa mẹ',
        'time': 'Hôm nay, 10:30 SA',
        'amount': '150 ml',
        'durationTotal': '20 phút',
        'durationLeft': '10m',
        'durationRight': '10m',
        'status': ['Vui vẻ', 'Không ọc sữa'],
        'note': 'Bé bú ngoan, ngủ thiếp đi ở cuối cữ bú bên phải.',
        'imageUrl':
            'https://images.unsplash.com/photo-1519689680058-324335c77eba?auto=format&fit=crop&q=80&w=400',
      };
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Lỗi tải chi tiết nhật ký')));
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _deleteLog() async {
    setState(() => _isDeleting = true);
    try {
      // API call: DELETE /api/v1/babies/{babyId}/daily-logs/{logId}
      // await apiDelete('/api/v1/babies/${widget.babyId}/daily-logs/${widget.logId}');

      await Future.delayed(const Duration(milliseconds: 600));

      if (mounted) {
        Navigator.pop(context, true); // true indicates deleted
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Không thể xóa nhật ký này')),
        );
      }
    } finally {
      if (mounted) setState(() => _isDeleting = false);
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
          'Ứng dụng Mẹ và Bé',
          style: TextStyle(
            color: primaryColor,
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
          : _logDetail == null
          ? const Center(child: Text('Không tìm thấy dữ liệu'))
          : SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
              child: Column(
                children: [
                  // Header
                  Column(
                    children: [
                      Container(
                        width: 80,
                        height: 80,
                        decoration: BoxDecoration(
                          color: const Color(0xFFF6DACF),
                          shape: BoxShape.circle,
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withValues(alpha: 0.05),
                              blurRadius: 12,
                              offset: const Offset(0, 4),
                            ),
                          ],
                        ),
                        child: const Icon(
                          Icons.restaurant,
                          color: primaryColor,
                          size: 40,
                        ),
                      ),
                      const SizedBox(height: 16),
                      Text(
                        _logDetail!['title'],
                        style: const TextStyle(
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                          color: textColor,
                          fontFamily: 'Quicksand',
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        _logDetail!['time'],
                        style: const TextStyle(
                          fontSize: 14,
                          color: Color(0xFF9C857C),
                          fontFamily: 'Quicksand',
                        ),
                      ),
                    ],
                  ),

                  const SizedBox(height: 32),

                  // Quantity Card
                  _buildInfoCard(
                    icon: Icons.water_drop,
                    title: 'Lượng sữa',
                    child: Text(
                      _logDetail!['amount'],
                      style: const TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: textColor,
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Duration Card
                  _buildInfoCard(
                    icon: Icons.schedule,
                    title: 'Thời gian',
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text(
                          _logDetail!['durationTotal'],
                          style: const TextStyle(
                            fontSize: 24,
                            fontWeight: FontWeight.bold,
                            color: textColor,
                          ),
                        ),
                        Row(
                          children: [
                            _buildDurationSide(
                              'TRÁI',
                              _logDetail!['durationLeft'],
                            ),
                            const SizedBox(width: 24),
                            _buildDurationSide(
                              'PHẢI',
                              _logDetail!['durationRight'],
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Context/Symptoms Card
                  _buildInfoCard(
                    icon: Icons.mood,
                    title: 'Trạng thái bé',
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Wrap(
                          spacing: 8,
                          runSpacing: 8,
                          children: (_logDetail!['status'] as List<String>).map(
                            (status) {
                              return Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 16,
                                  vertical: 6,
                                ),
                                decoration: BoxDecoration(
                                  color: const Color(0xFFFFE9E3),
                                  borderRadius: BorderRadius.circular(20),
                                ),
                                child: Text(
                                  status,
                                  style: const TextStyle(
                                    color: textColor,
                                    fontWeight: FontWeight.w500,
                                  ),
                                ),
                              );
                            },
                          ).toList(),
                        ),
                        const SizedBox(height: 16),
                        const Divider(color: Color(0xFFE9E1DB)),
                        const SizedBox(height: 12),
                        const Text(
                          'GHI CHÚ THÊM',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF9C857C),
                            letterSpacing: 0.5,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          _logDetail!['note'],
                          style: const TextStyle(
                            fontSize: 16,
                            color: textColor,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Photo Card
                  if (_logDetail!['imageUrl'] != null)
                    _buildInfoCard(
                      icon: Icons.photo_camera,
                      title: 'Hình ảnh',
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(12),
                        child: Image.network(
                          _logDetail!['imageUrl'],
                          width: double.infinity,
                          height: 140,
                          fit: BoxFit.cover,
                        ),
                      ),
                    ),

                  const SizedBox(height: 32),

                  // Actions
                  ElevatedButton.icon(
                    onPressed: () {
                      // Navigate to edit screen
                    },
                    icon: const Icon(Icons.edit),
                    label: const Text(
                      'Chỉnh sửa',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: primaryColor,
                      foregroundColor: Colors.white,
                      minimumSize: const Size(double.infinity, 56),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(28),
                      ),
                      elevation: 4,
                      shadowColor: primaryColor.withValues(alpha: 0.4),
                    ),
                  ),
                  const SizedBox(height: 12),
                  OutlinedButton.icon(
                    onPressed: _isDeleting ? null : _deleteLog,
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
                      _isDeleting ? 'Đang xóa...' : 'Xóa nhật ký',
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: errorColor,
                      ),
                    ),
                    style: OutlinedButton.styleFrom(
                      side: const BorderSide(
                        color: Color(0xFFFFDAD6),
                        width: 2,
                      ),
                      minimumSize: const Size(double.infinity, 56),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(28),
                      ),
                    ),
                  ),

                  const SizedBox(height: 32),
                ],
              ),
            ),
    );
  }

  Widget _buildInfoCard({
    required IconData icon,
    required String title,
    required Widget child,
  }) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.03),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: const Color(0xFFC98C7B), size: 22),
              const SizedBox(width: 12),
              Text(
                title,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: Color(0xFF9C857C),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          child,
        ],
      ),
    );
  }

  Widget _buildDurationSide(String label, String value) {
    return Column(
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.bold,
            color: Color(0xFF9C857C),
            letterSpacing: 0.5,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w600,
            color: Color(0xFF5A463F),
          ),
        ),
      ],
    );
  }
}
