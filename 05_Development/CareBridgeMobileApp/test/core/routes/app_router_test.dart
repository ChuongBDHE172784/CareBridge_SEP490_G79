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
    test('newly assigned mother enters consolidated stage selection', () {
      final redirect = resolveAppRedirect(
        isAuthenticated: true,
        isRestoring: false,
        blockedReason: null,
        role: 'MOTHER',
        location: '/role-selection',
      );

      expect(redirect, '/mother-stage-selection');
    });

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

    test('legacy and consolidated mother routes remain registered', () {
      final paths = appRouter.configuration.routes
          .whereType<GoRoute>()
          .map((route) => route.path)
          .toSet();

      expect(
        paths,
        containsAll(const {'/journey-onboarding', '/mother-stage-selection'}),
      );
    });

    test(
      'incomplete onboarding cannot deep-link into pregnancy setup',
      () async {
        final redirect = await resolveMotherOnboardingRedirect(
          role: 'MOTHER',
          location: '/journey-setup',
          canStartJourney: () async => false,
        );

        expect(redirect, '/mother-stage-selection');
      },
    );

    test('verified onboarding can open postpartum setup directly', () async {
      final redirect = await resolveMotherOnboardingRedirect(
        role: 'MOTHER',
        location: '/postpartum-recovery-setup',
        canStartJourney: () async => true,
      );

      expect(redirect, isNull);
    });

    test('status failures fail closed for guarded setup deep links', () async {
      final redirect = await resolveMotherOnboardingRedirect(
        role: 'MOTHER',
        location: '/journey-setup',
        canStartJourney: () async => throw StateError('offline'),
      );

      expect(redirect, '/mother-stage-selection');
    });

    test('non-mothers cannot open mother setup routes', () {
      final redirect = resolveAppRedirect(
        isAuthenticated: true,
        isRestoring: false,
        blockedReason: null,
        role: 'FAMILY',
        location: '/mother-stage-selection',
      );

      expect(redirect, '/');
    });
  });
}
