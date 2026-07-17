import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/auth/screens/register_screen.dart';

void main() {
  testWidgets('FED-REG-TC-007-MOB exposes Google and phone registration controls', (tester) async {
    await tester.pumpWidget(const MaterialApp(home: RegisterScreen()));
    expect(find.byKey(const Key('federated-google-register')), findsOneWidget);
    expect(find.byKey(const Key('federated-phone-register')), findsOneWidget);
  });
}
