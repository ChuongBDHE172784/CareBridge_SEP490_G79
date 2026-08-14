import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../models/notification_model.dart';
import '../services/notification_service.dart';

class LocationShareNotificationDetailScreen extends StatelessWidget {
  const LocationShareNotificationDetailScreen({
    super.key,
    required this.notification,
  });

  final NotificationRecord notification;

  static const _background = Color(0xFFF6F1EC);
  static const _surface = Color(0xFFFFFFFF);
  static const _surfaceMuted = Color(0xFFF2EAE4);
  static const _accent = Color(0xFFC98C7B);
  static const _accentDark = Color(0xFF845143);
  static const _text = Color(0xFF5A463F);
  static const _textMuted = Color(0xFF9C857C);
  static const _outline = Color(0xFFE8DDD6);

  double? get _latitude =>
      double.tryParse(notification.metadata?['latitude']?.toString() ?? '');
  double? get _longitude =>
      double.tryParse(notification.metadata?['longitude']?.toString() ?? '');
  String get _motherName =>
      notification.metadata?['motherName']?.toString().trim().isNotEmpty == true
      ? notification.metadata!['motherName'].toString().trim()
      : 'Mother';
  DateTime get _sharedAt =>
      DateTime.tryParse(notification.metadata?['sharedAt']?.toString() ?? '') ??
      notification.createdAt;

  Future<void> _openDirections(BuildContext context) async {
    final latitude = _latitude;
    final longitude = _longitude;
    if (latitude == null || longitude == null) return;
    final uri = Uri.parse(
      'https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude',
    );
    final opened = await launchUrl(uri, mode: LaunchMode.externalApplication);
    if (!opened && context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không thể mở ứng dụng bản đồ.')),
      );
    }
  }

  Future<void> _markRead(BuildContext context) async {
    await NotificationService.instance.markAsRead(notification.id);
    if (context.mounted) Navigator.of(context).pop();
  }

  String _formatTime(DateTime value) {
    final local = value.toLocal();
    return '${local.hour.toString().padLeft(2, '0')}:'
        '${local.minute.toString().padLeft(2, '0')} · '
        '${local.day.toString().padLeft(2, '0')}/'
        '${local.month.toString().padLeft(2, '0')}/${local.year}';
  }

  @override
  Widget build(BuildContext context) {
    final latitude = _latitude;
    final longitude = _longitude;
    final hasCoordinates = latitude != null && longitude != null;
    return Scaffold(
      backgroundColor: _background,
      appBar: AppBar(
        backgroundColor: _background,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          onPressed: () => Navigator.of(context).pop(),
          icon: const Icon(Icons.arrow_back_rounded, color: _accentDark),
        ),
        title: const Text(
          'Vị trí của Mother',
          style: TextStyle(
            color: _text,
            fontSize: 20,
            fontWeight: FontWeight.w800,
          ),
        ),
      ),
      body: SafeArea(
        top: false,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
          children: [
            Container(
              padding: const EdgeInsets.all(22),
              decoration: BoxDecoration(
                color: _surface,
                borderRadius: BorderRadius.circular(32),
                border: Border.all(color: _outline),
                boxShadow: const [
                  BoxShadow(
                    color: Color.fromRGBO(90, 70, 63, 0.07),
                    blurRadius: 32,
                    offset: Offset(0, 12),
                  ),
                ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 52,
                        height: 52,
                        decoration: BoxDecoration(
                          color: _accent.withValues(alpha: 0.16),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.location_on_rounded,
                          color: _accentDark,
                          size: 28,
                        ),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              _motherName,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                color: _text,
                                fontSize: 20,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                            const SizedBox(height: 3),
                            const Text(
                              'đã chia sẻ vị trí hiện tại',
                              style: TextStyle(
                                color: _textMuted,
                                fontSize: 14,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(18),
                    decoration: BoxDecoration(
                      color: _surfaceMuted,
                      borderRadius: BorderRadius.circular(24),
                      boxShadow: const [
                        BoxShadow(
                          color: Color.fromRGBO(90, 70, 63, 0.05),
                          blurRadius: 9,
                          offset: Offset(0, 4),
                          blurStyle: BlurStyle.inner,
                        ),
                      ],
                    ),
                    child: hasCoordinates
                        ? Row(
                            children: [
                              Expanded(
                                child: _Coordinate(
                                  label: 'VĨ ĐỘ',
                                  value: latitude.toStringAsFixed(6),
                                ),
                              ),
                              Container(width: 1, height: 42, color: _outline),
                              Expanded(
                                child: _Coordinate(
                                  label: 'KINH ĐỘ',
                                  value: longitude.toStringAsFixed(6),
                                ),
                              ),
                            ],
                          )
                        : const Text(
                            'Tọa độ không còn khả dụng.',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              color: _textMuted,
                              fontSize: 15,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                  ),
                  const SizedBox(height: 14),
                  Row(
                    children: [
                      const Icon(
                        Icons.schedule_rounded,
                        size: 18,
                        color: _textMuted,
                      ),
                      const SizedBox(width: 7),
                      Text(
                        'Đã gửi lúc ${_formatTime(_sharedAt)}',
                        style: const TextStyle(
                          color: _textMuted,
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
            SizedBox(
              height: 54,
              child: FilledButton.icon(
                key: const Key('location-share-directions-action'),
                onPressed: hasCoordinates
                    ? () => _openDirections(context)
                    : null,
                icon: const Icon(Icons.directions_rounded),
                label: const Text('Mở chỉ đường đến Mother'),
                style: FilledButton.styleFrom(
                  backgroundColor: _accent,
                  foregroundColor: Colors.white,
                  shape: const StadiumBorder(),
                  elevation: 2,
                  textStyle: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 10),
            SizedBox(
              height: 50,
              child: OutlinedButton.icon(
                onPressed: notification.isRead
                    ? () => Navigator.of(context).pop()
                    : () => _markRead(context),
                icon: Icon(
                  notification.isRead
                      ? Icons.check_circle_rounded
                      : Icons.mark_email_read_outlined,
                ),
                label: Text(notification.isRead ? 'Đã đọc' : 'Đánh dấu đã đọc'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: _accentDark,
                  side: const BorderSide(color: _outline),
                  shape: const StadiumBorder(),
                  textStyle: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'Vị trí chỉ được chia sẻ với thành viên Family trong nhóm gia đình.',
              textAlign: TextAlign.center,
              style: TextStyle(color: _textMuted, fontSize: 13, height: 1.4),
            ),
          ],
        ),
      ),
    );
  }
}

class _Coordinate extends StatelessWidget {
  const _Coordinate({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) => Column(
    children: [
      Text(
        label,
        style: const TextStyle(
          color: LocationShareNotificationDetailScreen._textMuted,
          fontSize: 11,
          fontWeight: FontWeight.w800,
          letterSpacing: 0.7,
        ),
      ),
      const SizedBox(height: 5),
      Text(
        value,
        style: const TextStyle(
          color: LocationShareNotificationDetailScreen._text,
          fontSize: 15,
          fontWeight: FontWeight.w800,
        ),
      ),
    ],
  );
}
