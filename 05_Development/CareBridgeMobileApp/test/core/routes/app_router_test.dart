import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/routes/app_router.dart';

void main() {
  test('Epic 6 consultation and expert-handoff routes remain registered', () {
    final paths = appRouter.configuration.routes
        .whereType<GoRoute>()
        .map((route) => route.path)
        .toSet();

    expect(
      paths,
      containsAll(const {
        '/triage/expert-handoff',
        '/consultation-requests',
        '/consultation-requests/:requestId',
      }),
    );
  });

  group('mother startup consent gate', () {
    test(
      'cold start dispatches an authenticated mother through auth landing',
      () {
        final redirect = resolveAppRedirect(
          isAuthenticated: true,
          isRestoring: false,
          blockedReason: null,
          role: 'MOTHER',
          location: '/',
        );

        expect(redirect, '/auth-landing');
      },
    );

    test('resolved mother home does not re-enter the startup dispatcher', () {
      final redirect = resolveAppRedirect(
        isAuthenticated: true,
        isRestoring: false,
        blockedReason: null,
        role: 'MOTHER',
        location: '/mother-home',
      );

      expect(redirect, isNull);
    });
  });

  group('blocked account routing', () {
    test('keeps a blocked unauthenticated user on the blocked screen', () {
      final redirect = resolveAppRedirect(
        isAuthenticated: false,
        isRestoring: false,
        blockedReason: 'ACCOUNT_ADMIN_LOCKED',
        role: null,
        location: '/blocked',
      );

      expect(redirect, isNull);
    });

    test('returns to welcome only after the blocked state is cleared', () {
      final redirect = resolveAppRedirect(
        isAuthenticated: false,
        isRestoring: false,
        blockedReason: null,
        role: null,
        location: '/blocked',
      );

      expect(redirect, '/welcome');
    });
  });
}
