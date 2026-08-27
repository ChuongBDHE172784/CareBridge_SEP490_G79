import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/journey/models/journey_onboarding_model.dart';
import 'package:untitled/features/journey/screens/mother_stage_selection_screen.dart';
import 'package:untitled/features/journey/screens/postpartum_recovery_setup_screen.dart';
import 'package:untitled/features/journey/services/journey_onboarding_draft_storage.dart';
import 'package:untitled/features/journey/services/journey_onboarding_service.dart';

class _CompleteOnboardingService extends JourneyOnboardingService {
  @override
  Future<JourneyOnboardingStatus> getStatus() async =>
      const JourneyOnboardingStatus(
        baselineComplete: true,
        consentValid: true,
        baselineRevision: 1,
      );
}

class _EmptyDraftStorage extends JourneyOnboardingDraftStorage {
  @override
  Future<Map<String, dynamic>?> read() async => null;
}

void main() {
  testWidgets('stage selection exposes a peer postpartum recovery route', (
    tester,
  ) async {
    final router = GoRouter(
      initialLocation: '/mother-stage-selection',
      routes: [
        GoRoute(
          path: '/mother-stage-selection',
          builder: (_, _) => MotherStageSelectionScreen(
            onboardingService: _CompleteOnboardingService(),
            draftStorage: _EmptyDraftStorage(),
          ),
        ),
        GoRoute(
          path: '/postpartum-recovery-setup',
          builder: (_, _) => const Scaffold(
            body: Text('postpartum-route', key: Key('postpartum-route-probe')),
          ),
        ),
      ],
    );
    addTearDown(router.dispose);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('mother-stage-postpartum')), findsOneWidget);
    await tester.ensureVisible(
      find.byKey(const Key('mother-stage-postpartum')),
    );
    await tester.tap(find.byKey(const Key('mother-stage-postpartum')));
    await tester.pump();
    await tester.tap(find.byKey(const Key('mother-stage-continue')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('postpartum-route-probe')), findsOneWidget);
  });

  testWidgets(
    'setup submits direct postpartum provenance and opens journey tab',
    (tester) async {
      CreateJourneyRequest? submitted;
      final router = GoRouter(
        initialLocation: '/postpartum-recovery-setup',
        routes: [
          GoRoute(
            path: '/postpartum-recovery-setup',
            builder: (_, _) => PostpartumRecoverySetupScreen(
              initialRecoveryStartDate: DateTime(2026, 7, 18),
              now: () => DateTime(2026, 7, 19),
              createJourney: (request) async {
                submitted = request;
                return const CreateJourneyResponse(
                  id: 'journey-post-1',
                  journeyType: 'POSTPARTUM',
                  status: 'ACTIVE',
                  startDate: '2026-07-18',
                  createdAt: '2026-07-19T00:00:00Z',
                );
              },
            ),
          ),
          GoRoute(
            path: '/mother-home',
            builder: (_, state) => Text(
              'tab:${state.uri.queryParameters['tab']}',
              key: const Key('mother-home-probe'),
            ),
          ),
          // A completed postpartum setup now continues into the personalisation questionnaire.
          GoRoute(
            path: '/recommendation-profile',
            builder: (_, state) => Text(
              'recommendation-profile:${state.extra}',
              key: const Key('recommendation-profile-probe'),
            ),
          ),
        ],
      );
      addTearDown(router.dispose);

      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();
      await tester.tap(
        find.byKey(const Key('postpartum-confidence-estimated')),
      );
      await tester.tap(find.byKey(const Key('postpartum-submit')));
      await tester.pumpAndSettle();

      expect(submitted, isNotNull);
      expect(submitted!.journeyType, JourneyType.postpartum);
      expect(submitted!.startDate, '2026-07-18');
      expect(submitted!.dateSource, 'SELF_REPORTED');
      expect(submitted!.dateConfidence, 'ESTIMATED');
      expect(submitted!.changeReason, 'INITIAL_SETUP');
      expect(
        find.byKey(const Key('recommendation-profile-probe')),
        findsOneWidget,
      );
      expect(find.text('recommendation-profile:POSTPARTUM'), findsOneWidget);
    },
  );

  testWidgets('setup rejects future dates without calling the backend', (
    tester,
  ) async {
    var calls = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: PostpartumRecoverySetupScreen(
          initialRecoveryStartDate: DateTime(2026, 7, 20),
          now: () => DateTime(2026, 7, 19),
          createJourney: (request) async {
            calls++;
            throw StateError('must not submit');
          },
        ),
      ),
    );

    await tester.tap(find.byKey(const Key('postpartum-submit')));
    await tester.pump();

    expect(calls, 0);
    expect(find.byKey(const Key('postpartum-date-error')), findsOneWidget);
  });

  testWidgets(
    'canonical conflict reconciles active postpartum dashboard before routing',
    (tester) async {
      var dashboardLoads = 0;
      final router = GoRouter(
        initialLocation: '/postpartum-recovery-setup',
        routes: [
          GoRoute(
            path: '/postpartum-recovery-setup',
            builder: (_, _) => PostpartumRecoverySetupScreen(
              initialRecoveryStartDate: DateTime(2026, 7, 19),
              now: () => DateTime(2026, 7, 19),
              createJourney: (_) async =>
                  throw ApiException(409, 'canonical already exists'),
              loadDashboard: () async {
                dashboardLoads++;
                return const JourneyDashboard(
                  journeyId: 'journey-post-1',
                  journeyType: 'POSTPARTUM',
                  status: 'ACTIVE_POSTPARTUM',
                );
              },
            ),
          ),
          GoRoute(
            path: '/mother-home',
            builder: (_, state) => Text(
              'tab:${state.uri.queryParameters['tab']}',
              key: const Key('mother-home-probe'),
            ),
          ),
        ],
      );
      addTearDown(router.dispose);

      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('postpartum-submit')));
      await tester.pumpAndSettle();

      expect(dashboardLoads, 1);
      // The conflict branch reconciles the already-active journey and lands on the journey tab;
      // only a fresh setup continues into the personalisation questionnaire.
      expect(find.byKey(const Key('mother-home-probe')), findsOneWidget);
      expect(find.text('tab:1'), findsOneWidget);
    },
  );
}
