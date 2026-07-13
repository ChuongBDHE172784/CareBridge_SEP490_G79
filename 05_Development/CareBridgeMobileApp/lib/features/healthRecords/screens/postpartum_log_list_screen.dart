import 'package:flutter/material.dart';

class PostpartumLogListScreen extends StatefulWidget {
  final String journeyId;

  const PostpartumLogListScreen({super.key, required this.journeyId});

  @override
  State<PostpartumLogListScreen> createState() =>
      _PostpartumLogListScreenState();
}

class _PostpartumLogListScreenState extends State<PostpartumLogListScreen> {
  bool _isLoading = true;
  String _selectedFilter = 'Tất cả';
  final List<String> _filters = [
    'Tất cả',
    'Giấc ngủ',
    'Tâm trạng',
    'Triệu chứng',
    'Cho bú',
  ];

  List<Map<String, dynamic>> _logs = [];

  @override
  void initState() {
    super.initState();
    _fetchLogs();
  }

  Future<void> _fetchLogs() async {
    setState(() => _isLoading = true);
    try {
      // API call: GET /api/v1/postpartum-logs?journeyId={journeyId}
      await Future.delayed(const Duration(milliseconds: 800));
      _logs = [
        {
          'id': 'log_1',
          'dateLabel': 'Hôm nay, 14 Tháng 10',
          'time': '08:30',
          'category': 'Giấc ngủ',
          'icon': Icons.bedtime,
          'color': const Color(0xFFC98C7B), // primary-container
          'note':
              'Ngủ được khoảng 4 tiếng liên tục. Cảm thấy đỡ mệt hơn hôm qua một chút.',
          'tags': ['Ngủ ngon', '4h'],
        },
        {
          'id': 'log_2',
          'dateLabel':
              '', // Same date group implicitly in UI or handled via logic
          'time': '14:15',
          'category': 'Tâm trạng',
          'icon': Icons.mood,
          'color': const Color(0xFF6E5A52), // secondary
          'note':
              'Hơi lo lắng về việc bé bú không đủ. Đã gọi cho bác sĩ tư vấn, thấy yên tâm hơn.',
          'tags': ['Lo âu nhẹ'],
        },
        {
          'id': 'log_3',
          'dateLabel': 'Hôm qua, 13 Tháng 10',
          'time': '19:00',
          'category': 'Triệu chứng',
          'icon': Icons.water_drop,
          'color': const Color(0xFFC98C7B),
          'note':
              'Vết mổ hơi nhói khi di chuyển nhiều. Đã uống thuốc giảm đau theo đơn.',
          'tags': ['Đau nhẹ'],
          'isErrorTag': true,
        },
      ];
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Lỗi tải danh sách nhật ký')),
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    const primaryColor = Color(0xFF845143);
    const bgColor = Color(0xFFFFF8F6);

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
          'Nhật ký phục hồi',
          style: TextStyle(
            color: primaryColor,
            fontWeight: FontWeight.bold,
            fontFamily: 'Quicksand',
            fontSize: 20,
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.close, color: primaryColor),
            onPressed: () => Navigator.pop(context),
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: primaryColor))
          : Column(
              children: [
                // Filters
                SizedBox(
                  height: 50,
                  child: ListView.separated(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 20,
                      vertical: 8,
                    ),
                    scrollDirection: Axis.horizontal,
                    itemCount: _filters.length,
                    separatorBuilder: (_, _) => const SizedBox(width: 8),
                    itemBuilder: (context, index) {
                      final filter = _filters[index];
                      final isSelected = filter == _selectedFilter;
                      return GestureDetector(
                        onTap: () {
                          setState(() => _selectedFilter = filter);
                        },
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 16,
                            vertical: 6,
                          ),
                          decoration: BoxDecoration(
                            color: isSelected
                                ? const Color(0xFFC98C7B)
                                : Colors.white,
                            borderRadius: BorderRadius.circular(20),
                            border: Border.all(
                              color: isSelected
                                  ? Colors.transparent
                                  : const Color(0xFFD6C2BD),
                            ),
                          ),
                          child: Center(
                            child: Text(
                              filter,
                              style: TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.bold,
                                color: isSelected
                                    ? Colors.white
                                    : const Color(0xFF524440),
                                fontFamily: 'Quicksand',
                              ),
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),
                // Timeline
                Expanded(
                  child: ListView.builder(
                    padding: const EdgeInsets.only(
                      left: 36,
                      right: 20,
                      top: 16,
                      bottom: 100,
                    ),
                    itemCount: _logs.length,
                    itemBuilder: (context, index) {
                      final log = _logs[index];
                      return _buildTimelineItem(
                        log,
                        isLast: index == _logs.length - 1,
                      );
                    },
                  ),
                ),
              ],
            ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () {
          // Add new log
        },
        backgroundColor: const Color(0xFFC98C7B),
        icon: const Icon(Icons.add, color: Color(0xFF51271B)),
        label: const Text(
          'Thêm nhật ký',
          style: TextStyle(
            color: Color(0xFF51271B),
            fontWeight: FontWeight.bold,
            fontFamily: 'Quicksand',
            fontSize: 16,
          ),
        ),
      ),
    );
  }

  Widget _buildTimelineItem(Map<String, dynamic> log, {required bool isLast}) {
    final bool hasDateLabel =
        log['dateLabel'] != null && log['dateLabel'].isNotEmpty;

    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Timeline indicator
          SizedBox(
            width: 24,
            child: Stack(
              clipBehavior: Clip.none,
              children: [
                if (!isLast)
                  Positioned(
                    top: 24,
                    bottom: -24, // overlap to next item
                    left: 11,
                    child: Container(
                      width: 2,
                      color: const Color(0xFFFADCD3), // surface-variant
                    ),
                  ),
                Positioned(
                  top: hasDateLabel ? 32 : 4,
                  left: 0,
                  child: Container(
                    width: 24,
                    height: 24,
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFE2D9),
                      shape: BoxShape.circle,
                      border: Border.all(
                        color: const Color(0xFFFFF8F6),
                        width: 2,
                      ),
                    ),
                    child: Center(
                      child: Container(
                        width: 8,
                        height: 8,
                        decoration: BoxDecoration(
                          color: log['color'],
                          shape: BoxShape.circle,
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 16),
          // Content
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (hasDateLabel)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: Text(
                      log['dateLabel'],
                      style: const TextStyle(
                        fontSize: 14,
                        color: Color(0xFF524440),
                        fontFamily: 'Quicksand',
                      ),
                    ),
                  ),
                GestureDetector(
                  onTap: () {
                    // Navigator.push Named to CB-159 Detail
                    // For now:
                  },
                  child: Container(
                    margin: const EdgeInsets.only(bottom: 16),
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(
                        color: const Color(0xFFFADCD3),
                      ), // surface-variant
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withValues(alpha: 0.03),
                          blurRadius: 12,
                          offset: const Offset(0, 4),
                        ),
                      ],
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Row(
                              children: [
                                Icon(
                                  log['icon'],
                                  color: log['color'],
                                  size: 24,
                                ),
                                const SizedBox(width: 8),
                                Text(
                                  log['category'],
                                  style: const TextStyle(
                                    fontSize: 18,
                                    fontWeight: FontWeight.bold,
                                    color: Color(0xFF271812),
                                    fontFamily: 'Quicksand',
                                  ),
                                ),
                              ],
                            ),
                            Text(
                              log['time'],
                              style: const TextStyle(
                                fontSize: 14,
                                color: Color(0xFF524440),
                                fontFamily: 'Quicksand',
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        Text(
                          log['note'],
                          style: const TextStyle(
                            fontSize: 14,
                            color: Color(0xFF271812),
                            fontFamily: 'Quicksand',
                          ),
                        ),
                        const SizedBox(height: 12),
                        Wrap(
                          spacing: 8,
                          children: (log['tags'] as List<String>).map((tag) {
                            final isError = log['isErrorTag'] == true;
                            return Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 8,
                                vertical: 4,
                              ),
                              decoration: BoxDecoration(
                                color: isError
                                    ? const Color(0xFFFFDAD6)
                                    : const Color(0xFFFFF1EC),
                                borderRadius: BorderRadius.circular(4),
                              ),
                              child: Text(
                                tag,
                                style: TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.bold,
                                  color: isError
                                      ? const Color(0xFF93000A)
                                      : const Color(0xFF524440),
                                  fontFamily: 'Quicksand',
                                ),
                              ),
                            );
                          }).toList(),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
