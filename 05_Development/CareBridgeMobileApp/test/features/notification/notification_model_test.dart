import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/notification/models/notification_model.dart';

void main() {
  test('uses canonical isRead independently of delivery status', () {
    final notification = NotificationRecord.fromJson({
      'id': 'notification-1',
      'userId': 'user-1',
      'type': 'REMINDER',
      'title': 'Reminder',
      'body': 'Body',
      'status': 'DELIVERED',
      'isRead': true,
      'createdAt': '2026-07-22T00:00:00Z',
    });

    expect(notification.isRead, isTrue);
    expect(notification.isUnread, isFalse);
  });

  test('defaults missing isRead to unread for backward compatibility', () {
    final notification = NotificationRecord.fromJson({
      'id': 'notification-2',
      'userId': 'user-1',
      'type': 'REMINDER',
      'title': 'Reminder',
      'body': 'Body',
      'status': 'SENT',
      'createdAt': '2026-07-22T00:00:00Z',
    });

    expect(notification.isUnread, isTrue);
  });
}
