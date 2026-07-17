import '../../../core/network/api_client.dart';
import '../models/notification_model.dart';

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

  Future<List<NotificationPreference>> getPreferences() async {
    final res = await apiGet('/api/v1/users/me/notification-preferences');
    final data = res['data'] as Map<String, dynamic>;
    final list = data['preferences'] as List<dynamic>;
    return list
        .map((e) => NotificationPreference.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<List<NotificationPreference>> updatePreferences(
    List<NotificationPreference> prefs,
  ) async {
    final res = await apiPut('/api/v1/users/me/notification-preferences', {
      'preferences': prefs.map((p) => p.toJson()).toList(),
    });
    final data = res['data'] as Map<String, dynamic>;
    final list = data['preferences'] as List<dynamic>;
    return list
        .map((e) => NotificationPreference.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
