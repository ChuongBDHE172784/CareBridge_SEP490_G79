import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/auth/screens/otp_verification_screen.dart';
import 'package:untitled/features/auth/screens/register_screen.dart';
import 'package:untitled/features/auth/screens/registration_verification_method_screen.dart';
import 'package:untitled/features/auth/services/auth_service.dart';

void main() {
  testWidgets(
    'duplicate account code shows error directly on RegisterScreen and blocks OTP navigation',
    (tester) async {
      final requests = <String>[];
      final service = _authService((path, _) async {
        requests.add(path);
        throw ApiException(
          409,
          '{"error":"AUTH_ACCOUNT_EXISTS",'
          '"message":"Email này đã được đăng ký tài khoản."}',
        );
      });

      await _pumpAndSubmit(tester, service);

      expect(requests, ['/api/v1/auth/check-registration']);
      // Still on RegisterScreen, never pushed RegistrationVerificationMethodScreen
      expect(find.byType(RegisterScreen), findsOneWidget);
      expect(find.byType(RegistrationVerificationMethodScreen), findsNothing);

      expect(
        find.text('Email này đã được đăng ký tài khoản.'),
        findsOneWidget,
      );
    },
  );

  testWidgets(
    'duplicate phone error shows phone-specific message on RegisterScreen',
    (tester) async {
      final service = _authService((path, _) async {
        throw ApiException(
          409,
          '{"error":"AUTH_ACCOUNT_EXISTS",'
          '"message":"Số điện thoại này đã được đăng ký tài khoản."}',
        );
      });

      await _pumpAndSubmit(tester, service);

      expect(find.byType(RegisterScreen), findsOneWidget);
      expect(find.byType(RegistrationVerificationMethodScreen), findsNothing);
      expect(
        find.text('Số điện thoại này đã được đăng ký tài khoản.'),
        findsOneWidget,
      );
    },
  );

  testWidgets(
    'duplicate email and phone shows composite message on RegisterScreen',
    (tester) async {
      final service = _authService((path, _) async {
        throw ApiException(
          409,
          '{"error":"AUTH_ACCOUNT_EXISTS",'
          '"message":"Email và số điện thoại này đã được đăng ký tài khoản."}',
        );
      });

      await _pumpAndSubmit(tester, service);

      expect(find.byType(RegisterScreen), findsOneWidget);
      expect(find.byType(RegistrationVerificationMethodScreen), findsNothing);
      expect(
        find.text('Email và số điện thoại này đã được đăng ký tài khoản.'),
        findsOneWidget,
      );
    },
  );

  testWidgets('uncoded validation error displays on RegisterScreen', (
    tester,
  ) async {
    final service = _authService((_, _) async {
      throw ApiException(400, '{"message":"Thông tin không hợp lệ. Vui lòng kiểm tra lại."}');
    });

    await _pumpAndSubmit(tester, service);

    expect(find.byType(RegisterScreen), findsOneWidget);
    expect(find.byType(RegistrationVerificationMethodScreen), findsNothing);
    expect(
      find.text('Thông tin không hợp lệ. Vui lòng kiểm tra lại.'),
      findsOneWidget,
    );
  });

  testWidgets('successful registration checks availability then opens OTP verification', (
    tester,
  ) async {
    final requests = <({String path, Map<String, dynamic> body})>[];
    final service = _authService((path, body) async {
      requests.add((path: path, body: Map.of(body)));
      if (path == '/api/v1/auth/check-registration') {
        return {'data': {'message': 'Available'}};
      }
      return {
        'data': {'message': 'OTP sent', 'expiresIn': 300, 'userId': 'user-1'},
      };
    });

    await _pumpAndSubmit(tester, service);

    expect(requests, hasLength(1));
    expect(requests.first.path, '/api/v1/auth/check-registration');
    expect(requests.first.body, {
      'email': 'mother@example.com',
      'phone': '+84912345678',
    });

    expect(find.byType(RegistrationVerificationMethodScreen), findsOneWidget);

    await tester.tap(
      find.byKey(const Key('registration-verification-continue')),
    );
    await tester.pumpAndSettle();

    expect(requests, hasLength(2));
    expect(requests[1].path, '/api/v1/auth/register');
    expect(requests[1].body, {
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
