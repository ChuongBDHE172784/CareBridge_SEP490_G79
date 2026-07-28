import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/auth/models/auth_model.dart';
import 'package:untitled/features/auth/screens/login_screen.dart';
import 'package:untitled/features/auth/services/auth_service.dart';

void main() {
  testWidgets(
    'email password login persists one session and routes without OTP',
    (tester) async {
      final requests = <({String path, Map<String, dynamic> body})>[];
      final persisted = <AuthResponse>[];
      final service = _authService((path, body) async {
        requests.add((path: path, body: Map.of(body)));
        return _authResponse;
      }, persisted: persisted);

      await _pumpAndSubmit(
        tester,
        service: service,
        identifier: '  mother@example.com  ',
        password: '  Password@123  ',
      );

      expect(requests, hasLength(1));
      expect(requests.single.path, '/api/v1/auth/login');
      expect(requests.single.body, {
        'email': 'mother@example.com',
        'password': '  Password@123  ',
      });
      expect(requests.single.body, isNot(contains('phone')));
      expect(persisted, hasLength(1));
      expect(persisted.single.accessToken, 'access-token');
      expect(find.text('authenticated'), findsOneWidget);
    },
  );

  testWidgets('phone password login uses the canonical endpoint', (
    tester,
  ) async {
    final requests = <({String path, Map<String, dynamic> body})>[];
    final service = _authService((path, body) async {
      requests.add((path: path, body: Map.of(body)));
      return _authResponse;
    });

    await _pumpAndSubmit(
      tester,
      service: service,
      identifier: '  0912345678  ',
      password: 'Password@123',
    );

    expect(requests.single.path, '/api/v1/auth/login');
    expect(requests.single.body, {
      'phone': '0912345678',
      'password': 'Password@123',
    });
    expect(requests.single.body, isNot(contains('email')));
    expect(find.text('authenticated'), findsOneWidget);
  });

  testWidgets('duplicate submits issue only one password request', (
    tester,
  ) async {
    var requestCount = 0;
    final response = Completer<dynamic>();
    final service = _authService((_, _) {
      requestCount++;
      return response.future;
    });
    await tester.pumpWidget(_testApp(service));
    await tester.enterText(find.byType(TextField).at(0), 'mother@example.com');
    await tester.enterText(find.byType(TextField).at(1), 'Password@123');
    final button = tester.widget<FilledButton>(
      find.byKey(const Key('password-login-submit')),
    );

    button.onPressed!();
    button.onPressed!();
    await tester.pump();
    expect(requestCount, 1);

    response.complete(_authResponse);
    await tester.pumpAndSettle();
    expect(find.text('authenticated'), findsOneWidget);
  });

  testWidgets('malformed success is rejected without persistence or routing', (
    tester,
  ) async {
    final persisted = <AuthResponse>[];
    final service = _authService(
      (_, _) async => {
        'data': {
          'accessToken': '',
          'refreshToken': 'refresh-token',
          'user': {'id': 'user-1', 'role': 'MOTHER'},
        },
      },
      persisted: persisted,
    );

    await _pumpAndSubmit(
      tester,
      service: service,
      identifier: 'mother@example.com',
      password: 'Password@123',
    );

    expect(persisted, isEmpty);
    expect(find.byType(LoginScreen), findsOneWidget);
    expect(find.text('authenticated'), findsNothing);
  });

  testWidgets('persistence failure never navigates', (tester) async {
    final service = AuthService.forTesting(
      postRequest: (_, _) async => _authResponse,
      tokenPersister: (_) async => throw StateError('secure storage failed'),
      postLoginAction: () async {},
    );

    await _pumpAndSubmit(
      tester,
      service: service,
      identifier: 'mother@example.com',
      password: 'Password@123',
    );

    expect(find.byType(LoginScreen), findsOneWidget);
    expect(find.text('authenticated'), findsNothing);
  });

  for (final errorCase in const [
    (statusCode: 401, message: 'Email/số điện thoại hoặc mật khẩu không đúng.'),
    (statusCode: 403, message: 'Tài khoản bị khóa. Liên hệ hỗ trợ để mở khóa.'),
    (statusCode: 429, message: 'Quá nhiều lần thử. Vui lòng đợi 15 phút.'),
    (
      statusCode: 404,
      message: 'Dịch vụ đăng nhập hiện không khả dụng. Vui lòng thử lại sau.',
    ),
    (
      statusCode: 500,
      message: 'Dịch vụ đăng nhập hiện không khả dụng. Vui lòng thử lại sau.',
    ),
    (
      statusCode: 503,
      message: 'Dịch vụ đăng nhập hiện không khả dụng. Vui lòng thử lại sau.',
    ),
  ]) {
    testWidgets('${errorCase.statusCode} preserves safe login guidance', (
      tester,
    ) async {
      final service = _authService((_, _) async {
        throw ApiException(errorCase.statusCode, 'sensitive backend details');
      });

      await _pumpAndSubmit(
        tester,
        service: service,
        identifier: 'mother@example.com',
        password: 'Password@123',
      );

      expect(find.byType(LoginScreen), findsOneWidget);
      expect(find.text(errorCase.message), findsOneWidget);
      expect(find.text('sensitive backend details'), findsNothing);
    });
  }
}

const _authResponse = <String, dynamic>{
  'data': <String, dynamic>{
    'accessToken': 'access-token',
    'refreshToken': 'refresh-token',
    'user': <String, dynamic>{
      'id': 'user-1',
      'name': 'Mother Test',
      'email': 'mother@example.com',
      'phone': null,
      'avatarUrl': null,
      'role': 'MOTHER',
      'accountStatus': 'ACTIVE',
    },
  },
};

AuthService _authService(
  AuthApiPost postRequest, {
  List<AuthResponse>? persisted,
}) {
  return AuthService.forTesting(
    postRequest: postRequest,
    tokenPersister: (auth) async {
      persisted?.add(auth);
    },
    postLoginAction: () async {},
  );
}

Widget _testApp(AuthService service) {
  final router = GoRouter(
    initialLocation: '/login',
    routes: [
      GoRoute(
        path: '/login',
        builder: (_, _) => LoginScreen(authService: service),
      ),
      GoRoute(
        path: '/auth-landing',
        builder: (_, _) => const Scaffold(body: Text('authenticated')),
      ),
    ],
  );
  return MaterialApp.router(routerConfig: router);
}

Future<void> _pumpAndSubmit(
  WidgetTester tester, {
  required AuthService service,
  required String identifier,
  required String password,
}) async {
  await tester.pumpWidget(_testApp(service));
  await tester.enterText(find.byType(TextField).at(0), identifier);
  await tester.enterText(find.byType(TextField).at(1), password);
  final submitButton = find.byKey(const Key('password-login-submit'));
  await tester.ensureVisible(submitButton);
  await tester.tap(submitButton);
  await tester.pumpAndSettle();
}
