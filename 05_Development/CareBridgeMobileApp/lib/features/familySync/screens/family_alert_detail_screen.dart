import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../services/care_group_service.dart';

class FamilyAlertDetailScreen extends StatefulWidget {
  final Map<String, dynamic> alert;

  const FamilyAlertDetailScreen({super.key, required this.alert});

  @override
  State<FamilyAlertDetailScreen> createState() =>
      _FamilyAlertDetailScreenState();
}

class _FamilyAlertDetailScreenState extends State<FamilyAlertDetailScreen> {
  final _groupService = CareGroupService();
  bool _isMarkingAsRead = false;
  late bool _isRead;

  @override
  void initState() {
    super.initState();
    _isRead = widget.alert['isRead'] as bool? ?? false;
  }

  Future<void> _markAsRead() async {
    if (_isRead) {
      Navigator.pop(context, false); // No change needed
      return;
    }

    setState(() => _isMarkingAsRead = true);
    try {
      final alertId = widget.alert['alertId'] as String?;
      if (alertId != null &&
          alertId.isNotEmpty &&
          !alertId.startsWith('mock')) {
        await _groupService.markAlertAsRead(alertId);
      } else {
        // Mock delay for mock alerts
        await Future.delayed(const Duration(milliseconds: 500));
      }

      setState(() {
        _isRead = true;
        _isMarkingAsRead = false;
      });

      if (mounted) {
        Navigator.pop(context, true); // Return true to indicate status changed
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isMarkingAsRead = false);
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final title = widget.alert['title'] as String? ?? '';
    final body = widget.alert['body'] as String? ?? '';
    final createdAt = widget.alert['createdAt'] as String?;

    // Determine type heuristics
    bool isUrgent =
        title.toLowerCase().contains('khẩn cấp') ||
        body.toLowerCase().contains('bất thường');
    bool isReminder =
        title.toLowerCase().contains('nhắc nhở') ||
        title.toLowerCase().contains('thuốc');

    IconData icon = Icons.info;
    Color primaryColor = const Color(0xFF6E5A52);
    Color containerColor = const Color(0xFFF6DACF);
    Color onContainerColor = const Color(0xFF271812);

    if (isUrgent) {
      icon = Icons.warning;
      primaryColor = const Color(0xFFBA1A1A);
      containerColor = const Color(0xFFFFDAD6);
      onContainerColor = const Color(0xFF93000A);
    } else if (isReminder) {
      icon = Icons.medical_services;
      primaryColor = const Color(0xFF845143);
      containerColor = const Color(0xFFFFE9E3);
      onContainerColor = const Color(0xFF51271B);
    }

    return Scaffold(
      backgroundColor: const Color(0xFFFEF8F4),
      appBar: AppBar(
        backgroundColor: const Color(0xFFFEF8F4),
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Color(0xFF524440)),
          onPressed: () => Navigator.pop(
            context,
            false,
          ), // false means no change if not tapped Mark as Read
        ),
        title: const Text(
          'Chi tiết cảnh báo',
          style: TextStyle(
            color: Color(0xFF271812),
            fontSize: 20,
            fontWeight: FontWeight.bold,
            fontFamily: 'Lexend',
          ),
        ),
      ),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          children: [
            // Alert Header Card
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: containerColor,
                borderRadius: BorderRadius.circular(24),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x0F5A463F),
                    blurRadius: 20,
                    offset: Offset(0, 4),
                  ),
                ],
              ),
              child: Column(
                children: [
                  Container(
                    width: 64,
                    height: 64,
                    decoration: BoxDecoration(
                      color: primaryColor,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(icon, color: Colors.white, size: 32),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    title,
                    style: TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: onContainerColor,
                      fontFamily: 'Lexend',
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 8),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 2,
                        ),
                        decoration: BoxDecoration(
                          color: primaryColor.withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          isUrgent
                              ? 'Khẩn cấp'
                              : (isReminder ? 'Nhắc nhở' : 'Thông tin'),
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: primaryColor,
                            fontFamily: 'Lexend',
                            letterSpacing: 1,
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Row(
                        children: [
                          const Icon(
                            Icons.schedule,
                            size: 16,
                            color: Color(0xFF524440),
                          ),
                          const SizedBox(width: 4),
                          Text(
                            _formatFullTime(createdAt),
                            style: const TextStyle(
                              fontSize: 14,
                              color: Color(0xFF524440),
                              fontFamily: 'Lexend',
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ],
              ),
            ),

            const SizedBox(height: 24),

            // Content Body
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(24),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x0F5A463F),
                    blurRadius: 20,
                    offset: Offset(0, 4),
                  ),
                ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(
                        isUrgent ? Icons.location_on : Icons.notes,
                        color: const Color(0xFF845143),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        isUrgent ? 'Vị trí & Chi tiết' : 'Chi tiết',
                        style: const TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF271812),
                          fontFamily: 'Lexend',
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  if (isUrgent) ...[
                    Container(
                      height: 160,
                      width: double.infinity,
                      decoration: BoxDecoration(
                        color: const Color(0xFFFADCD3),
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: const Center(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(Icons.map, size: 48, color: Color(0xFF845143)),
                            SizedBox(height: 8),
                            Text(
                              'Bản đồ vị trí (Mock)',
                              style: TextStyle(
                                color: Color(0xFF845143),
                                fontFamily: 'Lexend',
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                  ],
                  Text(
                    body,
                    style: const TextStyle(
                      fontSize: 16,
                      color: Color(0xFF524440),
                      height: 1.5,
                      fontFamily: 'Lexend',
                    ),
                  ),
                ],
              ),
            ),

            const Spacer(),

            // Actions
            if (isUrgent)
              ElevatedButton.icon(
                onPressed: () {},
                icon: const Icon(Icons.phone_in_talk),
                label: const Text(
                  'Gọi điện thoại',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    fontFamily: 'Lexend',
                  ),
                ),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFC98C7B),
                  foregroundColor: Colors.white,
                  minimumSize: const Size(double.infinity, 52),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(999),
                  ),
                ),
              ),

            const SizedBox(height: 12),

            ElevatedButton.icon(
              onPressed: _isMarkingAsRead ? null : _markAsRead,
              icon: _isMarkingAsRead
                  ? const SizedBox(
                      width: 24,
                      height: 24,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.check_circle),
              label: Text(
                _isRead ? 'Đã xác nhận xem' : 'Xác nhận đã xem',
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  fontFamily: 'Lexend',
                ),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: _isRead
                    ? const Color(0xFFE9E1DB)
                    : Colors.white,
                foregroundColor: _isRead
                    ? const Color(0xFF524440)
                    : const Color(0xFF524440),
                elevation: 0,
                side: BorderSide(
                  color: _isRead ? Colors.transparent : const Color(0xFFD6C2BD),
                ),
                minimumSize: const Size(double.infinity, 52),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(999),
                ),
              ),
            ),

            const SizedBox(height: 24),

            // Privacy Note
            const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.lock, size: 16, color: Color(0xFF84736F)),
                SizedBox(width: 8),
                Flexible(
                  child: Text(
                    'Cảnh báo được mã hóa an toàn trong nhóm.',
                    style: TextStyle(
                      fontSize: 12,
                      color: Color(0xFF84736F),
                      fontFamily: 'Lexend',
                    ),
                    textAlign: TextAlign.center,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _formatFullTime(String? isoDate) {
    if (isoDate == null) return '';
    try {
      final date = DateTime.parse(isoDate).toLocal();
      return DateFormat('hh:mm a, dd/MM/yyyy').format(date);
    } catch (e) {
      return '';
    }
  }
}
