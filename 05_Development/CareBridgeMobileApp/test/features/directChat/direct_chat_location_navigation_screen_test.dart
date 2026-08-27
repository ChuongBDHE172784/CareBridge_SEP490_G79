import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/screens/direct_chat_location_navigation_screen.dart';

void main() {
  testWidgets('DirectChatLocationNavigationScreen renders and toggles navigation mode', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      const MaterialApp(
        home: DirectChatLocationNavigationScreen(
          latitude: 10.762622,
          longitude: 106.660172,
          label: 'Vị trí thử nghiệm',
        ),
      ),
    );
    await tester.pump();

    // Verify title and header
    expect(find.text('Dẫn đường'), findsOneWidget);
    expect(find.text('Vị trí thử nghiệm'), findsOneWidget);

    // Verify header action button for Google Maps exists
    expect(find.byTooltip('Mở Google Maps'), findsOneWidget);

    // Verify the primary button is "Bắt đầu dẫn đường"
    expect(find.byKey(const Key('start-navigation-btn')), findsOneWidget);
    expect(find.text('Bắt đầu dẫn đường'), findsOneWidget);

    // Tap "Bắt đầu dẫn đường"
    await tester.tap(find.byKey(const Key('start-navigation-btn')));
    await tester.pump();

    // Verify active navigation mode
    expect(find.text('Đang dẫn đường'), findsOneWidget);
    expect(find.byKey(const Key('stop-navigation-btn')), findsOneWidget);
    expect(find.text('Dừng dẫn đường'), findsOneWidget);

    // Tap "Dừng dẫn đường"
    await tester.tap(find.byKey(const Key('stop-navigation-btn')));
    await tester.pump();

    // Verify returned to overview mode
    expect(find.text('Dẫn đường'), findsOneWidget);
    expect(find.byKey(const Key('start-navigation-btn')), findsOneWidget);
  });
}
