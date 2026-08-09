import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/notification/models/notification_model.dart';
import 'package:untitled/features/notification/routing/consultation_notification_routing.dart';

void main() {
  test('emergency notification opens the acknowledgement screen directly', () {
    final notification = NotificationRecord(
      id: 'notification-1',
      userId: 'family-1',
      type: 'EMERGENCY',
      title: 'Cảnh báo khẩn cấp từ CareBridge',
      body: 'Vui lòng kiểm tra tình trạng người thân ngay.',
      referenceId: '88350e6d-7dab-4d8a-b1c2-8a3c3678e1f5',
      referenceType: 'EMERGENCY_SESSION',
      status: 'SENT',
      createdAt: DateTime.utc(2026, 8, 10),
    );

    expect(
      resolveNotificationRoute(notification),
      '/emergency/alert/88350e6d-7dab-4d8a-b1c2-8a3c3678e1f5',
    );
  });

  test(
    'malformed emergency reference stays on generic notification detail',
    () {
      final notification = NotificationRecord(
        id: 'notification-2',
        userId: 'family-1',
        type: 'EMERGENCY',
        title: 'Cảnh báo khẩn cấp',
        body: 'Kiểm tra người thân.',
        referenceId: 'not-a-session-id',
        referenceType: 'EMERGENCY_SESSION',
        status: 'SENT',
        createdAt: DateTime.utc(2026, 8, 10),
      );

      expect(resolveNotificationRoute(notification), isNull);
    },
  );
}
