import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/notification/models/notification_model.dart';
import 'package:untitled/features/notification/routing/consultation_notification_routing.dart';

void main() {
  test('appointment notification reference opens reminder detail', () {
    final notification = NotificationRecord(
      id: 'notification-1',
      userId: 'user-1',
      type: 'REMINDER',
      title: 'Lịch hẹn',
      body: 'Sắp đến giờ hẹn',
      referenceId: 'dddddddd-dddd-4ddd-8ddd-dddddddddddd',
      referenceType: 'APPOINTMENT',
      status: 'SENT',
      createdAt: DateTime.utc(2026, 7, 30),
    );

    expect(
      resolveNotificationRoute(notification),
      '/reminders/detail/dddddddd-dddd-4ddd-8ddd-dddddddddddd',
    );
  });

  test('metadata reminder id is a safe fallback for legacy records', () {
    final notification = NotificationRecord(
      id: 'notification-2',
      userId: 'user-1',
      type: 'REMINDER',
      title: 'Lịch hẹn',
      body: 'Sắp đến giờ hẹn',
      status: 'SENT',
      createdAt: DateTime.utc(2026, 7, 30),
      metadata: const {'reminderId': 'eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee'},
    );

    expect(
      resolveNotificationRoute(notification),
      '/reminders/detail/eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee',
    );
  });
}
