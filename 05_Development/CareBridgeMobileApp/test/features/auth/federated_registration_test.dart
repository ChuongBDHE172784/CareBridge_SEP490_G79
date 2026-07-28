import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/routes/app_router.dart';
import 'package:untitled/features/auth/models/federated_auth_failure.dart';
import 'package:untitled/features/auth/screens/register_screen.dart';

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
    'FED-REG-TC-007-MOB exposes Google and phone registration controls',
    (tester) async {
      await tester.pumpWidget(const MaterialApp(home: RegisterScreen()));
      expect(
        find.byKey(const Key('federated-google-register')),
        findsOneWidget,
      );
      expect(find.byKey(const Key('federated-phone-register')), findsOneWidget);
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
    expect(tester.widget<OutlinedButton>(buttonFinder).onPressed, isNotNull);
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
    expect(tester.widget<OutlinedButton>(buttonFinder).onPressed, isNotNull);
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

    expect(tester.widget<OutlinedButton>(buttonFinder).onPressed, isNull);

    completer.completeError(
      const FederatedSignInException(FederatedAuthFailure.connectivity),
    );
    await tester.pump();
    expect(tester.widget<OutlinedButton>(buttonFinder).onPressed, isNotNull);
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
