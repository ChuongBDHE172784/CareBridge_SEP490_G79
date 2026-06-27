import '../../../core/network/api_client.dart';
import '../models/notification_model.dart';

class NotificationService {
  static final NotificationService instance = NotificationService._();
  NotificationService._();

  Future<List<NotificationRecord>> getNotifications({
    String? type,
    int page = 0,
    int size = 20,
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

  // TODO: wire to backend when mark-as-read endpoint is implemented
  Future<void> markAsRead(String notificationId) async {
    // await apiPost('/api/v1/notifications/$notificationId/read', {});
  }

  // TODO: wire to backend when mark-all-as-read endpoint is implemented
  Future<void> markAllAsRead() async {
    // await apiPost('/api/v1/notifications/read-all', {});
  }
}
