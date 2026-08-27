import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/auth/screens/otp_verification_screen.dart';
import 'package:untitled/features/auth/screens/register_screen.dart';
import 'package:untitled/features/auth/screens/registration_verification_method_screen.dart';
import 'package:untitled/features/auth/services/auth_service.dart';

void main() {
  testWidgets('duplicate account code shows the specific Vietnamese message', (
    tester,
  ) async {
    final service = _authService((_, _) async {
      throw ApiException(
        409,
        '{"error":"AUTH_ACCOUNT_EXISTS",'
        '"message":"sensitive backend details"}',
      );
    });

    await _pumpAndSubmit(tester, service);
    await tester.tap(
      find.byKey(const Key('registration-verification-continue')),
    );
    await tester.pumpAndSettle();

    expect(find.text('Tài khoản đã tồn tại'), findsOneWidget);
    expect(
      find.text('Thông tin không hợp lệ. Vui lòng kiểm tra lại.'),
      findsNothing,
    );
    expect(find.text('sensitive backend details'), findsNothing);
  });

  testWidgets('uncoded validation error keeps safe generic guidance', (
    tester,
  ) async {
    final service = _authService((_, _) async {
      throw ApiException(400, '{"message":"internal validation detail"}');
    });

    await _pumpAndSubmit(tester, service);
    await tester.tap(
      find.byKey(const Key('registration-verification-continue')),
    );
    await tester.pumpAndSettle();

    expect(
      find.text('Thông tin không hợp lệ. Vui lòng kiểm tra lại.'),
      findsOneWidget,
    );
    expect(find.text('internal validation detail'), findsNothing);
  });

  testWidgets('successful registration still opens OTP verification', (
    tester,
  ) async {
    final requests = <({String path, Map<String, dynamic> body})>[];
    final service = _authService((path, body) async {
      requests.add((path: path, body: Map.of(body)));
      return {
        'data': {'message': 'OTP sent', 'expiresIn': 300, 'userId': 'user-1'},
      };
    });

    await _pumpAndSubmit(tester, service);

    expect(find.byType(RegistrationVerificationMethodScreen), findsOneWidget);
    expect(requests, isEmpty);

    await tester.tap(
      find.byKey(const Key('registration-verification-continue')),
    );
    await tester.pumpAndSettle();

    expect(requests, hasLength(1));
    expect(requests.single.path, '/api/v1/auth/register');
    expect(requests.single.body, {
      'name': 'Mother Test',
      'password': 'Password@123',
      'verificationMethod': 'EMAIL',
      'email': 'mother@example.com',
      'phone': '+84912345678',
    });
    expect(find.byType(OtpVerificationScreen), findsOneWidget);
  });
}

AuthService _authService(AuthApiPost postRequest) {
  return AuthService.forTesting(
    postRequest: postRequest,
    tokenPersister: (_) async {},
    postLoginAction: () async {},
  );
}

Future<void> _pumpAndSubmit(
  WidgetTester tester,
  AuthService service, {
  bool settle = true,
}) async {
  await tester.pumpWidget(
    MaterialApp(home: RegisterScreen(authService: service)),
  );

  final fields = find.byType(TextField);
  await tester.enterText(fields.at(0), 'Mother Test');
  await tester.enterText(fields.at(1), 'mother@example.com');
  await tester.enterText(fields.at(2), '+84912345678');
  await tester.enterText(fields.at(3), 'Password@123');
  await tester.enterText(fields.at(4), 'Password@123');
  final checkbox = find.byType(Checkbox);
  await tester.ensureVisible(checkbox);
  await tester.pumpAndSettle();
  await tester.tap(checkbox);
  await tester.pump();

  final submit = find.widgetWithText(FilledButton, 'Tạo tài khoản');
  await tester.tap(submit);
  if (settle) {
    await tester.pumpAndSettle();
  } else {
    await tester.pump();
  }
}
