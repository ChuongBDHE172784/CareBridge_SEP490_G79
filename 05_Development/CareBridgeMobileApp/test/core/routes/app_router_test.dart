import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/routes/app_router.dart';
import 'package:untitled/features/baby/screens/add_baby_screen.dart';

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

  group('expert onboarding redirect', () {
    test('authenticated experts enter the onboarding gate after auth', () {
      final redirect = resolveAppRedirect(
        isAuthenticated: true,
        isRestoring: false,
        blockedReason: null,
        role: 'EXPERT',
        location: '/login',
      );

      expect(redirect, '/expert-onboarding');
    });
  });

  group('Add Baby entry authorization', () {
    test('typed live-birth extra enables the transition entry', () {
      expect(
        resolveAddBabyEntryPoint(
          extra: const AddBabyRouteArgs(
            entryPoint: AddBabyEntryPoint.liveBirthTransition,
          ),
          legacyEntry: null,
        ),
        AddBabyEntryPoint.liveBirthTransition,
      );
    });

    test('deep links cannot enable transition defer', () {
      expect(
        resolveAddBabyEntryPoint(
          extra: null,
          legacyEntry: 'liveBirthTransition',
        ),
        AddBabyEntryPoint.profileList,
      );
      expect(
        resolveAddBabyEntryPoint(extra: null, legacyEntry: 'onboarding'),
        AddBabyEntryPoint.onboarding,
      );
    });
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
        containsAll(const {
          '/checklists/history',
          '/journey-onboarding',
          '/mother-stage-selection',
        }),
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

    test('experts cannot deep-link into checklist history', () {
      final redirect = resolveAppRedirect(
        isAuthenticated: true,
        isRestoring: false,
        blockedReason: null,
        role: 'EXPERT',
        location: '/checklists/history',
      );

      expect(redirect, '/');
    });

    test('family can deep-link into selected-group checklist history', () {
      final redirect = resolveAppRedirect(
        isAuthenticated: true,
        isRestoring: false,
        blockedReason: null,
        role: 'FAMILY',
        location: '/checklists/history',
      );

      expect(redirect, isNull);
    });
  });

  group('family triage continuation gate', () {
    test('cold start dispatches Family through auth landing once', () {
      expect(
        resolveAppRedirect(
          isAuthenticated: true,
          isRestoring: false,
          blockedReason: null,
          role: ' family ',
          location: '/',
        ),
        '/auth-landing',
      );
      expect(
        resolveAppRedirect(
          isAuthenticated: true,
          isRestoring: false,
          blockedReason: null,
          role: 'FAMILY',
          location: '/',
          familyLandingComplete: true,
        ),
        isNull,
      );
    });

    test('Family may remain on auth landing while restoration runs', () {
      final redirect = resolveAppRedirect(
        isAuthenticated: true,
        isRestoring: false,
        blockedReason: null,
        role: 'FAMILY',
        location: '/auth-landing',
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
