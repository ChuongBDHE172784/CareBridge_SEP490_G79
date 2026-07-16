import '../models/notification_model.dart';

String? resolveNotificationRoute(NotificationRecord notification) {
  final referenceId = notification.referenceId;
  if (notification.referenceType == 'CONSULTATION_REQUEST' &&
      referenceId != null &&
      referenceId.isNotEmpty) {
    return '/consultation-requests/${Uri.encodeComponent(referenceId)}';
  }
  return null;
}
