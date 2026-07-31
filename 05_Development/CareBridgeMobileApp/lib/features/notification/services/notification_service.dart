import '../../../core/network/api_client.dart';
import '../../reminder/models/appointment_notification_timing.dart';
import '../models/notification_model.dart';
import '../models/notification_preferences_model.dart';

class NotificationService {
  static NotificationService instance = NotificationService();
  NotificationService();

  Future<List<NotificationRecord>> getNotifications({
    String? type,
    int page = 0,
    int size = 50,
  }) async {
    var path = '/api/v1/notifications/me?page=$page&size=$size';
    if (type != null && type.isNotEmpty) path += '&type=$type';
    final res = await apiGet(path);
    final data = res['data'] as Map<String, dynamic>;
    final content = data['content'] as List<dynamic>;
    return content
        .map((e) => NotificationRecord.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> markAsRead(String notificationId) async {
    await apiPut('/api/v1/notifications/$notificationId/read', {});
  }

  Future<int> markAllAsRead() async {
    final res = await apiPut('/api/v1/notifications/read-all', {});
    final data = res['data'] as Map<String, dynamic>?;
    return (data?['markedCount'] as int?) ?? 0;
  }

  Future<NotificationPreferences> getPreferences() async {
    final res = await apiGet('/api/v1/users/me/notification-preferences');
    return _parsePreferences(res['data'] as Map<String, dynamic>);
  }

  Future<NotificationPreferences> updatePreferences(
    List<NotificationPreference> prefs, {
    List<int>? appointmentReminderDefaults,
  }) async {
    final res = await apiPut('/api/v1/users/me/notification-preferences', {
      'preferences': prefs.map((p) => p.toJson()).toList(),
      'appointmentReminderDefaults': ?appointmentReminderDefaults,
    });
    return _parsePreferences(res['data'] as Map<String, dynamic>);
  }

  NotificationPreferences _parsePreferences(Map<String, dynamic> data) {
    final list = data['preferences'] as List<dynamic>? ?? const [];
    final rawDefaults =
        data['appointmentReminderDefaults'] as List<dynamic>? ??
        AppointmentNotificationTiming.systemDefaults;
    return NotificationPreferences(
      preferences: list
          .map(
            (e) => NotificationPreference.fromJson(e as Map<String, dynamic>),
          )
          .toList(),
      appointmentReminderDefaults: AppointmentNotificationTiming.normalize(
        rawDefaults.whereType<num>().map((value) => value.toInt()).toList(),
      ),
    );
  }
}
