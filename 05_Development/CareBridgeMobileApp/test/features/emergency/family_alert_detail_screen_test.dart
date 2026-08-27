import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/screens/direct_chat_location_navigation_screen.dart';
import 'package:untitled/features/emergency/models/emergency_alert_model.dart';
import 'package:untitled/features/emergency/screens/family_alert_detail_screen.dart';

void main() {
  EmergencyAlert alert({bool withLocation = true}) => EmergencyAlert(
    id: '5d80bd66-5d66-4c5b-bd23-6b3eb96a52b9',
    alertType: 'FALL_DETECTED',
    personName: 'Mother Test',
    phoneNumber: '0901234567',
    latitude: withLocation ? 10.762622 : null,
    longitude: withLocation ? 106.660172 : null,
    createdAt: DateTime.now().subtract(const Duration(minutes: 2)),
  );

  testWidgets('shows only real emergency contact and location data', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(
        home: FamilyAlertDetailScreen(
          sessionId: '5d80bd66-5d66-4c5b-bd23-6b3eb96a52b9',
          initialAlert: alert(),
        ),
      ),
    );

    expect(find.text('Mother Test'), findsOneWidget);
    expect(find.text('0901234567'), findsOneWidget);
    expect(find.text('10.762622'), findsOneWidget);
    expect(find.text('106.660172'), findsOneWidget);
    expect(find.text('Mở chỉ đường'), findsOneWidget);
    expect(find.textContaining('TV4'), findsNothing);
    expect(find.textContaining('TV5'), findsNothing);
    expect(find.textContaining('Thiết bị'), findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets('explains when an old alert has no coordinates', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: FamilyAlertDetailScreen(
          sessionId: '5d80bd66-5d66-4c5b-bd23-6b3eb96a52b9',
          initialAlert: alert(withLocation: false),
        ),
      ),
    );

    expect(find.text('Không có vị trí'), findsOneWidget);
    expect(find.text('Mở chỉ đường'), findsNothing);
  });

  testWidgets('tapping Mở chỉ đường opens DirectChatLocationNavigationScreen', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(
        home: FamilyAlertDetailScreen(
          sessionId: '5d80bd66-5d66-4c5b-bd23-6b3eb96a52b9',
          initialAlert: alert(),
        ),
      ),
    );

    await tester.tap(find.text('Mở chỉ đường'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 300));

    expect(find.byType(DirectChatLocationNavigationScreen), findsOneWidget);
    expect(find.text('Vị trí của Mother Test'), findsOneWidget);
  });
}
