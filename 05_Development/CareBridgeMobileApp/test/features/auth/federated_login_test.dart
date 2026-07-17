import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/auth/screens/login_screen.dart';

void main() {
  testWidgets('FED-LOGIN-TC-007-MOB exposes Google and phone sign-in controls', (tester) async {
    await tester.pumpWidget(const MaterialApp(home: LoginScreen()));
    expect(find.byKey(const Key('federated-google-login')), findsOneWidget);
    expect(find.byKey(const Key('federated-phone-login')), findsOneWidget);
  });
}
