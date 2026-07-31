import '../models/notification_model.dart';

final RegExp _uuidPattern = RegExp(
  r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$',
);

/// Resolves an in-app notification from its typed backend reference rather
/// than guessing a destination from display text or arbitrary metadata.
String? resolveNotificationRoute(NotificationRecord notification) {
  final referenceId = notification.referenceId;
  if (notification.referenceType?.trim().toUpperCase() ==
          'CONSULTATION_REQUEST' &&
      referenceId != null &&
      _uuidPattern.hasMatch(referenceId)) {
    return '/consultation-requests/${Uri.encodeComponent(referenceId)}';
  }
  final referenceType = notification.referenceType?.trim().toUpperCase();
  if ((referenceType == 'APPOINTMENT' ||
          notification.type.trim().toUpperCase() == 'REMINDER') &&
      referenceId != null &&
      _uuidPattern.hasMatch(referenceId)) {
    return '/reminders/detail/${Uri.encodeComponent(referenceId)}';
  }
  final metadataReminderId = notification.metadata?['reminderId'];
  if (metadataReminderId is String &&
      _uuidPattern.hasMatch(metadataReminderId)) {
    return '/reminders/detail/${Uri.encodeComponent(metadataReminderId)}';
  }
  return null;
}
