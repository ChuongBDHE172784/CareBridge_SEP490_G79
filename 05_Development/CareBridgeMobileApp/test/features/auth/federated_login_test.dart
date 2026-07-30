import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:untitled/features/auth/models/federated_auth_failure.dart';
import 'package:untitled/features/auth/screens/login_screen.dart';

void main() {
  testWidgets(
    'FED-LOGIN-TC-007-MOB exposes Google and phone sign-in controls',
    (tester) async {
      await tester.pumpWidget(const MaterialApp(home: LoginScreen()));
      expect(find.byKey(const Key('federated-google-login')), findsOneWidget);
      expect(find.byKey(const Key('federated-phone-login')), findsOneWidget);
      expect(find.byTooltip('Tiếp tục với Google'), findsOneWidget);
      expect(find.byTooltip('Tiếp tục với số điện thoại'), findsOneWidget);
      expect(find.text('G'), findsOneWidget);
      expect(find.byIcon(Icons.phone_rounded), findsOneWidget);
    },
  );

  testWidgets('cancellation leaves the login screen without an error', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: LoginScreen(
          onGoogleSignIn: () async => throw const FederatedSignInException(
            FederatedAuthFailure.canceled,
          ),
        ),
      ),
    );

    final googleButton = find.byKey(const Key('federated-google-login'));
    await tester.ensureVisible(googleButton);
    await tester.pumpAndSettle();
    await tester.tap(googleButton);
    await tester.pump();

    expect(find.byIcon(Icons.error_outline), findsNothing);
    final button = tester.widget<IconButton>(
      find.byKey(const Key('federated-google-login')),
    );
    expect(button.onPressed, isNotNull);
  });

  testWidgets('raw Google cancellation also leaves no empty error banner', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: LoginScreen(
          onGoogleSignIn: () async => throw const GoogleSignInException(
            code: GoogleSignInExceptionCode.canceled,
          ),
        ),
      ),
    );

    final googleButton = find.byKey(const Key('federated-google-login'));
    await tester.ensureVisible(googleButton);
    await tester.pumpAndSettle();
    await tester.tap(googleButton);
    await tester.pump();

    expect(find.byIcon(Icons.error_outline), findsNothing);
  });

  testWidgets('typed failure shows safe guidance and restores the button', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: LoginScreen(
          onGoogleSignIn: () async => throw const FederatedSignInException(
            FederatedAuthFailure.invalidCredential,
          ),
        ),
      ),
    );

    final googleButton = find.byKey(const Key('federated-google-login'));
    await tester.ensureVisible(googleButton);
    await tester.pumpAndSettle();
    await tester.tap(googleButton);
    await tester.pump();

    expect(
      find.text(FederatedAuthFailure.invalidCredential.userMessage),
      findsOneWidget,
    );
    final button = tester.widget<IconButton>(
      find.byKey(const Key('federated-google-login')),
    );
    expect(button.onPressed, isNotNull);
  });

  testWidgets('Google sign-in disables the button while awaiting completion', (
    tester,
  ) async {
    final completer = Completer<void>();
    await tester.pumpWidget(
      MaterialApp(home: LoginScreen(onGoogleSignIn: () => completer.future)),
    );

    final googleButton = find.byKey(const Key('federated-google-login'));
    await tester.ensureVisible(googleButton);
    await tester.pumpAndSettle();
    await tester.tap(googleButton);
    await tester.pump();

    var button = tester.widget<IconButton>(
      find.byKey(const Key('federated-google-login')),
    );
    expect(button.onPressed, isNull);

    completer.completeError(
      const FederatedSignInException(FederatedAuthFailure.connectivity),
    );
    await tester.pump();

    button = tester.widget<IconButton>(
      find.byKey(const Key('federated-google-login')),
    );
    expect(button.onPressed, isNotNull);
  });

  testWidgets('successful Google sign-in navigates to auth landing', (
    tester,
  ) async {
    final router = GoRouter(
      initialLocation: '/login',
      routes: [
        GoRoute(
          path: '/login',
          builder: (_, _) => LoginScreen(onGoogleSignIn: () async {}),
        ),
        GoRoute(
          path: '/auth-landing',
          builder: (_, _) => const Scaffold(body: Text('Auth landing')),
        ),
      ],
    );
    addTearDown(router.dispose);
    await tester.pumpWidget(MaterialApp.router(routerConfig: router));

    final googleButton = find.byKey(const Key('federated-google-login'));
    await tester.ensureVisible(googleButton);
    await tester.pumpAndSettle();
    await tester.tap(googleButton);
    await tester.pumpAndSettle();

    expect(find.text('Auth landing'), findsOneWidget);
  });
}
