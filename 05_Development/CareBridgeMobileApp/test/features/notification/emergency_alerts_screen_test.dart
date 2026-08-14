import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/notification/screens/emergency_alerts_screen.dart';

void main() {
  group('EmergencyAlertsScreen widget tests', () {
    testWidgets('renders screen loading or header state', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: EmergencyAlertsScreen(),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byType(EmergencyAlertsScreen), findsOneWidget);
    });
  });
}
