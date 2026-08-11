import '../models/notification_model.dart';

final RegExp _uuidPattern = RegExp(
  r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$',
);

/// Resolves an in-app notification from its typed backend reference rather
/// than guessing a destination from display text or arbitrary metadata.
String? resolveNotificationRoute(NotificationRecord notification) {
  final referenceId = notification.referenceId;
  final notificationType = notification.type.trim().toUpperCase();
  final referenceType = notification.referenceType?.trim().toUpperCase();
  if (notificationType == 'EMERGENCY' &&
      referenceType == 'EMERGENCY_SESSION' &&
      referenceId != null &&
      _uuidPattern.hasMatch(referenceId)) {
    return '/emergency/alert/${Uri.encodeComponent(referenceId)}';
  }
  if (notification.referenceType?.trim().toUpperCase() ==
          'CONSULTATION_REQUEST' &&
      referenceId != null &&
      _uuidPattern.hasMatch(referenceId)) {
    return '/consultation-requests/${Uri.encodeComponent(referenceId)}';
  }
  if (notificationType == 'REMINDER' &&
      referenceType == 'REMINDER_SCHEDULE' &&
      referenceId != null &&
      _uuidPattern.hasMatch(referenceId)) {
    return '/reminder-schedules/${Uri.encodeComponent(referenceId)}';
  }
  final metadataScheduleId = notification.metadata?['scheduleId'];
  if (notificationType == 'REMINDER' &&
      referenceType == 'REMINDER_SCHEDULE' &&
      metadataScheduleId is String &&
      _uuidPattern.hasMatch(metadataScheduleId)) {
    return '/reminder-schedules/${Uri.encodeComponent(metadataScheduleId)}';
  }
  if (referenceType == 'APPOINTMENT' &&
      referenceId != null &&
      _uuidPattern.hasMatch(referenceId)) {
    final careGroupId = notification.metadata?['careGroupId'];
    if (careGroupId is String && _uuidPattern.hasMatch(careGroupId)) {
      return '/care-groups/${Uri.encodeComponent(careGroupId)}/appointments/'
          '${Uri.encodeComponent(referenceId)}';
    }
    return '/appointments/detail/${Uri.encodeComponent(referenceId)}';
  }
  if (notificationType == 'REMINDER' &&
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
