import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/notification/models/notification_model.dart';
import 'package:untitled/features/notification/screens/location_share_notification_detail_screen.dart';
import 'package:untitled/features/notification/screens/notification_detail_screen.dart';

NotificationRecord _locationShareNotification({
  String type = 'LOCATION_SHARE',
  String? referenceType = 'LOCATION_SHARE',
  Map<String, dynamic>? metadata,
}) => NotificationRecord(
  id: 'notification-1',
  userId: 'family-1',
  type: type,
  title: 'Mother đã chia sẻ vị trí',
  body: 'Mở chi tiết để xem vị trí hiện tại.',
  referenceId: 'share-1',
  referenceType: referenceType,
  status: 'SENT',
  createdAt: DateTime.utc(2026, 8, 11),
  metadata:
      metadata ??
      {
        'motherName': 'Nguyễn Thị Lan',
        'latitude': 10.762622,
        'longitude': 106.660172,
        'sharedAt': '2026-08-11T01:02:03Z',
      },
);

void main() {
  test('notification parser preserves location-share metadata', () {
    final notification = NotificationRecord.fromJson({
      'id': 'notification-1',
      'userId': 'family-1',
      'type': 'LOCATION_SHARE',
      'title': 'Mother đã chia sẻ vị trí',
      'body': 'Mở chi tiết để xem vị trí hiện tại.',
      'referenceType': 'LOCATION_SHARE',
      'status': 'SENT',
      'createdAt': '2026-08-11T01:02:03Z',
      'metadata': {
        'motherName': 'Nguyễn Thị Lan',
        'latitude': 10.762622,
        'longitude': 106.660172,
        'sharedAt': '2026-08-11T01:02:03Z',
      },
    });

    expect(notification.type, 'LOCATION_SHARE');
    expect(notification.referenceType, 'LOCATION_SHARE');
    expect(notification.metadata?['motherName'], 'Nguyễn Thị Lan');
    expect(notification.metadata?['latitude'], 10.762622);
    expect(notification.metadata?['longitude'], 106.660172);
  });

  testWidgets('detail dispatches to the dedicated family location UI', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(800, 1400);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(
        home: NotificationDetailScreen(
          notification: _locationShareNotification(type: 'GENERAL'),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(LocationShareNotificationDetailScreen), findsOneWidget);
    expect(find.text('Vị trí của Mother'), findsOneWidget);
    expect(find.text('Nguyễn Thị Lan'), findsOneWidget);
    expect(find.text('10.762622'), findsOneWidget);
    expect(find.text('106.660172'), findsOneWidget);
    expect(find.textContaining('Đã gửi lúc'), findsOneWidget);
    expect(find.text('Mở chỉ đường đến Mother'), findsOneWidget);

    final directions = tester.widget<FilledButton>(
      find.byKey(const Key('location-share-directions-action')),
    );
    expect(directions.onPressed, isNotNull);
  });

  testWidgets('directions are disabled when coordinates are unavailable', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: NotificationDetailScreen(
          notification: _locationShareNotification(
            metadata: {
              'motherName': 'Nguyễn Thị Lan',
              'sharedAt': '2026-08-11T01:02:03Z',
            },
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Tọa độ không còn khả dụng.'), findsOneWidget);
    final directions = tester.widget<FilledButton>(
      find.byKey(const Key('location-share-directions-action')),
    );
    expect(directions.onPressed, isNull);
  });
}
