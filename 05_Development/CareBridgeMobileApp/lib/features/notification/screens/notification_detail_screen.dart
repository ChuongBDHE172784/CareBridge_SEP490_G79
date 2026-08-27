import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../models/notification_model.dart';
import '../services/notification_service.dart';
import '../routing/consultation_notification_routing.dart';
import 'location_share_notification_detail_screen.dart';

class NotificationDetailScreen extends StatelessWidget {
  final NotificationRecord notification;

  const NotificationDetailScreen({super.key, required this.notification});

  static const _bgColor = Color(0xFFFFF1EC);
  static const _primaryColor = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFF84736F);
  static const _surfaceContainerLowest = Color(0xFFFFFFFF);
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _surfaceContainerHighest = Color(0xFFFADCD3);
  static const _secondaryFixed = Color(0xFFF8DDD2);
  static const _onSecondaryFixedVariant = Color(0xFF55433B);

  @override
  Widget build(BuildContext context) {
    if (notification.type.trim().toUpperCase() == 'LOCATION_SHARE' ||
        notification.referenceType?.trim().toUpperCase() == 'LOCATION_SHARE') {
      return LocationShareNotificationDetailScreen(notification: notification);
    }
    return Scaffold(
      backgroundColor: _bgColor,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(context),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(24, 16, 24, 24),
                child: Column(
                  children: [
                    _buildDetailCard(),
                    const Spacer(),
                    _buildActions(context),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar(BuildContext context) {
    return Container(
      color: _bgColor,
      height: 72,
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Row(
        children: [
          SizedBox(
            width: 48,
            height: 48,
            child: IconButton(
              onPressed: () => Navigator.of(context).pop(),
              icon: const Icon(Icons.arrow_back, color: _onSurface, size: 28),
              padding: EdgeInsets.zero,
            ),
          ),
          Expanded(
            child: Text(
              'Chi tiết thông báo',
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: _primaryColor,
              ),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildDetailCard() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _surfaceContainerHigh.withValues(alpha: 0.5)),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.06),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildMetadataHeader(),
          Container(
            height: 1,
            margin: const EdgeInsets.only(top: 16),
            color: _surfaceContainerHigh,
          ),
          const SizedBox(height: 16),
          _buildContentBody(),
        ],
      ),
    );
  }

  Widget _buildMetadataHeader() {
    return Row(
      children: [
        Container(
          width: 40,
          height: 40,
          decoration: const BoxDecoration(
            color: _secondaryFixed,
            shape: BoxShape.circle,
          ),
          child: const Icon(
            Icons.calendar_month_rounded,
            color: _onSecondaryFixedVariant,
            size: 20,
          ),
        ),
        const SizedBox(width: 12),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              _getTypeLabel(notification.type).toUpperCase(),
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                fontWeight: FontWeight.w500,
                letterSpacing: 0.6,
                color: _onSecondaryFixedVariant,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              _formatTime(notification.createdAt),
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                fontWeight: FontWeight.w400,
                color: _outline,
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildContentBody() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          notification.title,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: _onSurface,
          ),
        ),
        const SizedBox(height: 12),
        Text(
          notification.body,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            fontWeight: FontWeight.w400,
            color: _onSurfaceVariant,
            height: 1.6,
          ),
        ),
      ],
    );
  }

  Widget _buildActions(BuildContext context) {
    // EPDS results have no detail destination for a Family account: the screening
    // itself is not family-visible, and resolveNotificationRoute returns null, so the
    // button would only close the screen. The notification body already carries the
    // whole message, so the primary action is omitted for this type.
    final hasPrimaryAction = notification.type.toUpperCase() != 'EPDS_RESULT';

    return Column(
      children: [
        if (hasPrimaryAction) ...[
          SizedBox(
            width: double.infinity,
            height: 52,
            child: FilledButton(
              onPressed: () {
                final route = resolveNotificationRoute(notification);
                if (route != null) {
                  context.push(route);
                } else {
                  Navigator.of(context).pop();
                }
              },
              style: FilledButton.styleFrom(
                backgroundColor: _primaryContainer,
                foregroundColor: Colors.white,
                shape: const StadiumBorder(),
                elevation: 0,
              ),
              child: Text(
                _getPrimaryActionLabel(),
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ),
          const SizedBox(height: 8),
        ],
        SizedBox(
          width: double.infinity,
          height: 52,
          child: FilledButton(
            onPressed: () async {
              await NotificationService.instance.markAsRead(notification.id);
              if (context.mounted) Navigator.of(context).pop();
            },
            style: FilledButton.styleFrom(
              backgroundColor: _surfaceContainerHighest,
              foregroundColor: _onSurfaceVariant,
              shape: const StadiumBorder(),
              elevation: 0,
            ),
            child: const Text(
              'Đánh dấu đã đọc',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ),
      ],
    );
  }

  String _getPrimaryActionLabel() {
    switch (notification.type.toUpperCase()) {
      case 'APPOINTMENT':
      case 'REMINDER':
        return 'Xem lịch hẹn';
      case 'MESSAGE':
      case 'CHAT':
        return 'Xem tin nhắn';
      case 'HEALTH':
      case 'HEALTH_ALERT':
        return 'Xem chi tiết';
      case 'CONSULTATION':
        return 'Xem yêu cầu tư vấn';
      default:
        return 'Xem chi tiết';
    }
  }

  String _getTypeLabel(String type) {
    switch (type.toUpperCase()) {
      case 'HEALTH':
      case 'HEALTH_ALERT':
        return 'Cảnh báo sức khỏe';
      case 'APPOINTMENT':
      case 'REMINDER':
        return 'Lịch hẹn';
      case 'MESSAGE':
      case 'CHAT':
        return 'Tin nhắn';
      case 'CONSULTATION':
        return 'Yêu cầu tư vấn';
      case 'LOCATION_SHARE':
        return 'Vị trí của Mother';
      case 'EPDS_RESULT':
        return 'Kết quả sàng lọc EPDS';
      default:
        return 'Thông báo';
    }
  }

  String _formatTime(DateTime dateTime) {
    final now = DateTime.now();
    final diff = now.difference(dateTime);

    if (diff.inMinutes < 1) return 'Vừa xong';
    if (diff.inMinutes < 60) return '${diff.inMinutes} phút trước';
    if (diff.inHours < 24) return '${diff.inHours} giờ trước';
    if (diff.inDays == 1) return 'Hôm qua';
    if (diff.inDays < 7) return '${diff.inDays} ngày trước';

    return '${dateTime.day.toString().padLeft(2, '0')}/'
        '${dateTime.month.toString().padLeft(2, '0')}/'
        '${dateTime.year}';
  }
}
