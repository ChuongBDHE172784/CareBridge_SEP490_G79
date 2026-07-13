import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../services/care_group_service.dart';
import 'family_alert_detail_screen.dart';

class FamilyAlertsScreen extends StatefulWidget {
  final String groupId;

  const FamilyAlertsScreen({super.key, required this.groupId});

  @override
  State<FamilyAlertsScreen> createState() => _FamilyAlertsScreenState();
}

class _FamilyAlertsScreenState extends State<FamilyAlertsScreen> {
  final _groupService = CareGroupService();
  bool _isLoading = true;
  List<Map<String, dynamic>> _alerts = [];

  String _selectedFilter = 'Tất cả';

  @override
  void initState() {
    super.initState();
    _loadAlerts();
  }

  Future<void> _loadAlerts() async {
    setState(() => _isLoading = true);
    try {
      final alerts = await _groupService.getFamilyAlerts(
        widget.groupId,
        page: 0,
        size: 50,
      );
      if (mounted) {
        setState(() {
          _alerts = alerts;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi tải cảnh báo: $e')));
      }
    }
  }

  List<Map<String, dynamic>> get _filteredAlerts {
    if (_selectedFilter == 'Tất cả') return _alerts;
    // Basic mock filtering based on filter string matching title/body heuristics
    // In a real app with dedicated type fields, we would filter by type.
    if (_selectedFilter == 'Khẩn cấp') {
      return _alerts
          .where(
            (a) =>
                (a['title'] as String).toLowerCase().contains('khẩn cấp') ||
                (a['body'] as String).toLowerCase().contains('bất thường'),
          )
          .toList();
    }
    if (_selectedFilter == 'Nhắc nhở') {
      return _alerts
          .where(
            (a) =>
                (a['title'] as String).toLowerCase().contains('nhắc nhở') ||
                (a['title'] as String).toLowerCase().contains('thuốc'),
          )
          .toList();
    }
    return _alerts;
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
          'Thông báo gia đình',
          style: TextStyle(
            color: Color(0xFF845143),
            fontSize: 20,
            fontWeight: FontWeight.bold,
            fontFamily: 'Lexend',
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.done_all, color: Color(0xFF845143)),
            tooltip: 'Đánh dấu tất cả đã đọc',
            onPressed: () {
              // Not yet implemented in backend, so just update UI locally
              setState(() {
                for (var a in _alerts) {
                  a['isRead'] = true;
                }
              });
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Đã đánh dấu tất cả đã đọc')),
              );
            },
          ),
        ],
      ),
      body: Column(
        children: [
          // Filters
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(
              horizontal: 24.0,
              vertical: 8.0,
            ),
            child: Row(
              children: [
                _buildFilterChip('Tất cả'),
                const SizedBox(width: 8),
                _buildFilterChip('Khẩn cấp'),
                const SizedBox(width: 8),
                _buildFilterChip('Nhắc nhở'),
              ],
            ),
          ),

          // Alerts List
          Expanded(
            child: _isLoading
                ? const Center(
                    child: CircularProgressIndicator(color: Color(0xFFC98C7B)),
                  )
                : RefreshIndicator(
                    color: const Color(0xFFC98C7B),
                    onRefresh: _loadAlerts,
                    child: _filteredAlerts.isEmpty
                        ? ListView(
                            padding: const EdgeInsets.all(24),
                            children: const [
                              Center(
                                child: Padding(
                                  padding: EdgeInsets.all(32.0),
                                  child: Text(
                                    'Không có cảnh báo nào',
                                    style: TextStyle(
                                      color: Color(0xFF524440),
                                      fontFamily: 'Lexend',
                                    ),
                                  ),
                                ),
                              ),
                            ],
                          )
                        : ListView.builder(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 24,
                              vertical: 8,
                            ),
                            itemCount: _filteredAlerts.length,
                            itemBuilder: (context, index) {
                              final alert = _filteredAlerts[index];
                              return _buildAlertCard(alert);
                            },
                          ),
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildFilterChip(String label) {
    final isSelected = _selectedFilter == label;
    return GestureDetector(
      onTap: () {
        setState(() => _selectedFilter = label);
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        decoration: BoxDecoration(
          color: isSelected ? const Color(0xFFC98C7B) : const Color(0xFFFFE9E3),
          borderRadius: BorderRadius.circular(999),
          border: Border.all(
            color: isSelected ? Colors.transparent : const Color(0xFFFFE2D9),
          ),
          boxShadow: isSelected
              ? const [
                  BoxShadow(
                    color: Color(0x0F5A463F),
                    blurRadius: 20,
                    offset: Offset(0, 4),
                  ),
                ]
              : null,
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 14,
            fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
            color: isSelected ? Colors.white : const Color(0xFF524440),
            fontFamily: 'Lexend',
          ),
        ),
      ),
    );
  }

  Widget _buildAlertCard(Map<String, dynamic> alert) {
    final bool isRead = alert['isRead'] as bool? ?? false;
    final title = alert['title'] as String? ?? '';
    final body = alert['body'] as String? ?? '';
    final createdAt = alert['createdAt'] as String?;

    // Determine type/urgency heuristically for UI styling
    bool isUrgent =
        title.toLowerCase().contains('khẩn cấp') ||
        body.toLowerCase().contains('bất thường');
    bool isReminder =
        title.toLowerCase().contains('nhắc nhở') ||
        title.toLowerCase().contains('thuốc');

    IconData icon = Icons.info;
    Color iconColor = const Color(0xFF6E5A52);
    Color bgColor = isRead ? const Color(0xFFFFFFFF) : const Color(0xFFFFF1EC);
    Color borderColor = Colors.transparent;

    if (isUrgent) {
      icon = Icons.warning;
      iconColor = const Color(0xFFBA1A1A);
      if (!isRead) {
        bgColor = const Color(0xFFFFFFFF);
        borderColor = const Color(0xFFFFDAD6);
      }
    } else if (isReminder) {
      icon = Icons.medical_services;
      iconColor = const Color(0xFF845143);
      if (!isRead) {
        bgColor = const Color(0xFFFFFFFF);
      }
    }

    return GestureDetector(
      onTap: () async {
        final result = await Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => FamilyAlertDetailScreen(alert: alert),
          ),
        );

        // If marked as read in detail screen, update local state
        if (result == true) {
          setState(() {
            alert['isRead'] = true;
          });
        }
      },
      child: Container(
        margin: const EdgeInsets.only(bottom: 16),
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: bgColor,
          borderRadius: BorderRadius.circular(24),
          border: Border.all(
            color: borderColor,
            width: isUrgent && !isRead ? 1 : 0,
          ),
          boxShadow: const [
            BoxShadow(
              color: Color(0x0F5A463F),
              blurRadius: 20,
              offset: Offset(0, 4),
            ),
          ],
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: isUrgent
                    ? const Color(0xFFFFDAD6)
                    : (isReminder
                          ? const Color(0xFFFFE9E3)
                          : const Color(0xFFE9E1DB)),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, color: iconColor),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        isUrgent
                            ? 'Khẩn cấp'
                            : (isReminder ? 'Nhắc nhở' : 'Thông tin'),
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: iconColor,
                          fontFamily: 'Lexend',
                        ),
                      ),
                      Row(
                        children: [
                          if (!isRead)
                            Container(
                              width: 8,
                              height: 8,
                              margin: const EdgeInsets.only(right: 8),
                              decoration: BoxDecoration(
                                color: iconColor,
                                shape: BoxShape.circle,
                              ),
                            ),
                          Text(
                            _formatTime(createdAt),
                            style: const TextStyle(
                              fontSize: 12,
                              color: Color(0xFF84736F),
                              fontFamily: 'Lexend',
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    title,
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: !isRead ? FontWeight.bold : FontWeight.normal,
                      color: const Color(0xFF271812),
                      fontFamily: 'Lexend',
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    body,
                    style: const TextStyle(
                      fontSize: 14,
                      color: Color(0xFF524440),
                      fontFamily: 'Lexend',
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _formatTime(String? isoDate) {
    if (isoDate == null) return '';
    try {
      final date = DateTime.parse(isoDate).toLocal();
      final now = DateTime.now();
      if (date.year == now.year &&
          date.month == now.month &&
          date.day == now.day) {
        return DateFormat('hh:mm a').format(date);
      } else {
        return '${date.day}/${date.month}';
      }
    } catch (e) {
      return '';
    }
  }
}
