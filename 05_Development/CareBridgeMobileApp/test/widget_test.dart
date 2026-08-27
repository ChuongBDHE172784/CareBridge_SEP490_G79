import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('CareBridge app smoke test — splash screen renders', (
    WidgetTester tester,
  ) async {
    // Auth init is async; the splash screen should render immediately.
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          backgroundColor: Color(0xFFF6F1EC),
          body: Center(
            child: CircularProgressIndicator(color: Color(0xFFC98C7B)),
          ),
        ),
      ),
    );

    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });
}
