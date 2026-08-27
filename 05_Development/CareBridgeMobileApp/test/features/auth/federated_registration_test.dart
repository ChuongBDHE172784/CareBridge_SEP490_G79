import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/routes/app_router.dart';
import 'package:untitled/features/auth/models/federated_auth_failure.dart';
import 'package:untitled/features/auth/screens/register_screen.dart';
import 'package:untitled/features/auth/screens/registration_verification_method_screen.dart';
import 'package:untitled/features/auth/services/auth_service.dart';

void main() {
  test('canonical dispatcher preserves an existing linked mother account', () {
    final redirect = resolveAppRedirect(
      isAuthenticated: true,
      isRestoring: false,
      blockedReason: null,
      role: 'MOTHER',
      location: '/auth-landing',
    );

    expect(redirect, isNull);
  });

  testWidgets(
    'registration form requires both contacts and keeps Google and SMS quick sign-up',
    (tester) async {
      await tester.pumpWidget(const MaterialApp(home: RegisterScreen()));
      expect(
        find.byKey(const Key('federated-google-register')),
        findsOneWidget,
      );
      expect(
        find.byKey(const Key('federated-phone-register')),
        findsOneWidget,
      );
      expect(find.byKey(const Key('register-email-field')), findsOneWidget);
      expect(find.byKey(const Key('register-phone-field')), findsOneWidget);
      expect(
        find.byKey(const Key('register-verification-method')),
        findsNothing,
      );
      expect(find.byTooltip('Đăng ký với Google'), findsOneWidget);
      expect(find.byTooltip('Đăng ký bằng SMS'), findsOneWidget);
      expect(find.text('G'), findsOneWidget);
    },
  );

  testWidgets(
    'expert registration is email-only and cannot escape through federated role selection',
    (tester) async {
      await tester.pumpWidget(
        const MaterialApp(home: RegisterScreen(isExpert: true)),
      );

      expect(find.byKey(const Key('federated-google-register')), findsNothing);
      expect(find.byKey(const Key('register-email-field')), findsOneWidget);
      expect(find.byKey(const Key('register-phone-field')), findsOneWidget);
      expect(find.text('bacsi@example.com'), findsOneWidget);

      final fields = find.byType(TextField);
      await tester.enterText(fields.at(0), 'Bác sĩ Test');
      await tester.enterText(fields.at(1), 'expert@example.com');
      await tester.enterText(fields.at(2), '+84912345678');
      await tester.enterText(fields.at(3), 'Password@1');
      await tester.enterText(fields.at(4), 'Password@1');
      final checkbox = find.byType(Checkbox);
      await tester.ensureVisible(checkbox);
      await tester.tap(checkbox);
      final submit = find.widgetWithText(FilledButton, 'Tạo tài khoản');
      await tester.ensureVisible(submit);
      await tester.tap(submit);
      await tester.pumpAndSettle();

      expect(find.byType(RegistrationVerificationMethodScreen), findsOneWidget);
      expect(
        find.text('Tài khoản chuyên gia được xác minh bằng email.'),
        findsOneWidget,
      );
      final selector = tester.widget<SegmentedButton<AuthVerificationMethod>>(
        find.byKey(const Key('registration-verification-method')),
      );
      expect(selector.segments.last.enabled, isFalse);
    },
  );

  testWidgets('Google cancellation is silent and restores registration', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: RegisterScreen(
          onGoogleSignIn: () async => throw const FederatedSignInException(
            FederatedAuthFailure.canceled,
          ),
        ),
      ),
    );

    final buttonFinder = find.byKey(const Key('federated-google-register'));
    await tester.ensureVisible(buttonFinder);
    await tester.pumpAndSettle();
    await tester.tap(buttonFinder);
    await tester.pump();

    expect(find.byIcon(Icons.error_outline), findsNothing);
    expect(tester.widget<IconButton>(buttonFinder).onPressed, isNotNull);
  });

  testWidgets('typed Google failure shows safe registration guidance', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: RegisterScreen(
          onGoogleSignIn: () async => throw const FederatedSignInException(
            FederatedAuthFailure.configuration,
          ),
        ),
      ),
    );

    final buttonFinder = find.byKey(const Key('federated-google-register'));
    await tester.ensureVisible(buttonFinder);
    await tester.pumpAndSettle();
    await tester.tap(buttonFinder);
    await tester.pump();

    expect(
      find.text(FederatedAuthFailure.configuration.userMessage),
      findsOneWidget,
    );
    expect(tester.widget<IconButton>(buttonFinder).onPressed, isNotNull);
  });

  testWidgets('Google registration is disabled while authentication runs', (
    tester,
  ) async {
    final completer = Completer<void>();
    await tester.pumpWidget(
      MaterialApp(home: RegisterScreen(onGoogleSignIn: () => completer.future)),
    );

    final buttonFinder = find.byKey(const Key('federated-google-register'));
    await tester.ensureVisible(buttonFinder);
    await tester.pumpAndSettle();
    await tester.tap(buttonFinder);
    await tester.pump();

    expect(tester.widget<IconButton>(buttonFinder).onPressed, isNull);

    completer.completeError(
      const FederatedSignInException(FederatedAuthFailure.connectivity),
    );
    await tester.pump();
    expect(tester.widget<IconButton>(buttonFinder).onPressed, isNotNull);
  });

  testWidgets(
    'successful Google registration uses canonical existing-account routing',
    (tester) async {
      final router = GoRouter(
        initialLocation: '/register',
        routes: [
          GoRoute(
            path: '/register',
            builder: (_, _) => RegisterScreen(onGoogleSignIn: () async {}),
          ),
          GoRoute(
            path: '/auth-landing',
            builder: (_, _) => const Scaffold(body: Text('Auth landing')),
          ),
        ],
      );
      addTearDown(router.dispose);
      await tester.pumpWidget(MaterialApp.router(routerConfig: router));

      final buttonFinder = find.byKey(const Key('federated-google-register'));
      await tester.ensureVisible(buttonFinder);
      await tester.pumpAndSettle();
      await tester.tap(buttonFinder);
      await tester.pumpAndSettle();

      expect(find.text('Auth landing'), findsOneWidget);
    },
  );
}
