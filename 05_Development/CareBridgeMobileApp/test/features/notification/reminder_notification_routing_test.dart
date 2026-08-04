import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/notification/models/notification_model.dart';
import 'package:untitled/features/notification/routing/consultation_notification_routing.dart';

void main() {
  test('appointment notification reference opens appointment detail', () {
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
      '/appointments/detail/dddddddd-dddd-4ddd-8ddd-dddddddddddd',
    );
  });

  test(
    'shared appointment notification opens the selected care-group detail',
    () {
      final notification = NotificationRecord(
        id: 'notification-shared',
        userId: 'user-1',
        type: 'REMINDER',
        title: 'Lịch hẹn đã thay đổi',
        body: 'Lịch hẹn đã cập nhật',
        referenceId: 'dddddddd-dddd-4ddd-8ddd-dddddddddddd',
        referenceType: 'APPOINTMENT',
        status: 'SENT',
        createdAt: DateTime.utc(2026, 7, 30),
        metadata: {'careGroupId': 'eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee'},
      );

      expect(
        resolveNotificationRoute(notification),
        '/care-groups/eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee/appointments/'
        'dddddddd-dddd-4ddd-8ddd-dddddddddddd',
      );
    },
  );

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

  test('reminder schedule reference opens the alarm schedule detail', () {
    final notification = NotificationRecord(
      id: 'notification-3',
      userId: 'user-1',
      type: 'REMINDER',
      title: 'Lich nhac',
      body: 'Den gio',
      referenceId: 'ffffffff-ffff-4fff-8fff-ffffffffffff',
      referenceType: 'REMINDER_SCHEDULE',
      status: 'SENT',
      createdAt: DateTime.utc(2026, 7, 30),
    );

    expect(
      resolveNotificationRoute(notification),
      '/reminder-schedules/ffffffff-ffff-4fff-8fff-ffffffffffff',
    );
  });
}
