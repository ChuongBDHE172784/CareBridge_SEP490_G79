import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/aiTriage/models/triage_continuation.dart';
import 'package:untitled/features/aiTriage/models/triage_entry_context.dart';
import 'package:untitled/features/aiTriage/services/triage_continuation_store.dart';
import 'package:untitled/features/aiTriage/services/triage_service.dart';
import 'package:untitled/features/baby/models/baby_model.dart';
import 'package:untitled/features/baby/screens/baby_profile_detail_screen.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/journey/screens/mother_journey_screen.dart';

class _RequestTestContinuationStore implements TriageContinuationStore {
  @override
  int generationFor(String userId) => 0;

  @override
  Future<void> invalidateUser(String userId) async {}

  @override
  Future<PendingTriageContinuation?> read(String userId) async => null;

  @override
  Future<void> save({
    required String userId,
    required PendingTriageContinuation continuation,
    required int generation,
  }) async {}
}

Widget _bindingProbe(GoRouterState state) {
  final dynamic context = state.extra as TriageEntryContext;
  final binding = Map<String, dynamic>.from(
    context.toLifecycleBindingJson() as Map,
  );
  return Scaffold(
    body: Text(
      '${context.stage.apiValue}|${binding['journeyId']}|'
      '${binding['originReferenceId']}|${binding['originDashboard']}',
      key: const Key('story-6-7-binding-probe'),
    ),
  );
}

GoRouter _surfaceRouter(Widget origin) => GoRouter(
  initialLocation: '/origin',
  routes: [
    GoRoute(
      path: '/origin',
      builder: (_, _) => Scaffold(body: origin),
    ),
    GoRoute(
      path: '/triage/intake',
      builder: (_, state) => _bindingProbe(state),
    ),
  ],
);

Future<void> _openSurfaceAndAssertBinding(
  WidgetTester tester, {
  required Widget origin,
  required String stage,
  required String? journeyId,
  required String originReferenceId,
  required String originDashboard,
}) async {
  final router = _surfaceRouter(origin);
  addTearDown(router.dispose);
  await tester.pumpWidget(MaterialApp.router(routerConfig: router));
  await tester.pumpAndSettle();

  final action = find.byKey(Key('triage-safety-entry-${stage.toLowerCase()}'));
  await tester.scrollUntilVisible(
    action,
    300,
    scrollable: find.byType(Scrollable).first,
  );
  await tester.tap(action);
  await tester.pumpAndSettle();

  expect(find.byKey(const Key('story-6-7-binding-probe')), findsOneWidget);
  expect(
    find.text('$stage|$journeyId|$originReferenceId|$originDashboard'),
    findsOneWidget,
  );
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
  });

  group('Story 6.7 lifecycle-bound origin contract', () {
    final fixtures =
        <
          ({
            TriageStageIntent stage,
            TriageOriginIntent origin,
            String? journeyId,
            String originReferenceId,
            String originDashboard,
          })
        >[
          (
            stage: TriageStageIntent.preconception,
            origin: TriageOriginIntent.motherJourney,
            journeyId: '10000000-0000-0000-0000-000000000001',
            originReferenceId: '10000000-0000-0000-0000-000000000001',
            originDashboard: 'MOTHER_JOURNEY',
          ),
          (
            stage: TriageStageIntent.pregnancy,
            origin: TriageOriginIntent.motherJourney,
            journeyId: '10000000-0000-0000-0000-000000000002',
            originReferenceId: '10000000-0000-0000-0000-000000000002',
            originDashboard: 'MOTHER_JOURNEY',
          ),
          (
            stage: TriageStageIntent.postpartum,
            origin: TriageOriginIntent.motherJourney,
            journeyId: '10000000-0000-0000-0000-000000000003',
            originReferenceId: '10000000-0000-0000-0000-000000000003',
            originDashboard: 'MOTHER_JOURNEY',
          ),
          (
            stage: TriageStageIntent.infant,
            origin: TriageOriginIntent.babyProfile,
            journeyId: null,
            originReferenceId: '20000000-0000-0000-0000-000000000004',
            originDashboard: 'BABY_PROFILE',
          ),
          (
            stage: TriageStageIntent.toddler,
            origin: TriageOriginIntent.babyProfile,
            journeyId: null,
            originReferenceId: '20000000-0000-0000-0000-000000000005',
            originDashboard: 'BABY_PROFILE',
          ),
        ];

    for (final fixture in fixtures) {
      test(
        '${fixture.stage.apiValue} carries exact lifecycle and origin IDs',
        () {
          final dynamic context =
              Function.apply(TriageEntryContext.locked, const [], {
                #stage: fixture.stage,
                #origin: fixture.origin,
                #journeyId: fixture.journeyId,
                #originReferenceId: fixture.originReferenceId,
              });

          expect(context.stage, fixture.stage);
          expect(context.lockStage, isTrue);
          expect(context.journeyId, fixture.journeyId);
          expect(context.originReferenceId, fixture.originReferenceId);
          expect(context.toLifecycleBindingJson(), {
            'journeyId': fixture.journeyId,
            'originDashboard': fixture.originDashboard,
            'originReferenceId': fixture.originReferenceId,
            if (fixture.origin == TriageOriginIntent.babyProfile)
              'babyProfileId': fixture.originReferenceId,
          });
        },
      );
    }

    test('TODDLER binding does not add or reinterpret an age cutoff', () {
      final dynamic context =
          Function.apply(TriageEntryContext.locked, const [], {
            #stage: TriageStageIntent.toddler,
            #origin: TriageOriginIntent.babyProfile,
            #journeyId: null,
            #originReferenceId: '20000000-0000-0000-0000-000000000005',
          });

      final binding = Map<String, dynamic>.from(
        context.toLifecycleBindingJson() as Map,
      );
      expect(binding.keys, isNot(contains('childAgeMonths')));
      expect(binding.keys, isNot(contains('toddlerAgeCutoffMonths')));
      expect(binding.keys, isNot(contains('birthDate')));
    });

    test('legacy direct entry sends no lifecycle binding', () {
      final dynamic context = const TriageEntryContext();

      expect(context.origin, TriageOriginIntent.direct);
      expect(context.toLifecycleBindingJson(), isEmpty);
    });

    for (final stage in const [
      TriageStageIntent.infant,
      TriageStageIntent.toddler,
    ]) {
      test(
        '${stage.apiValue} service request lifts babyProfileId to the top level',
        () async {
          await AuthState.instance.clear();
          await AuthState.instance.setTokens(
            accessToken: 'access-a',
            refreshToken: 'refresh-a',
            userId: 'account-a',
            role: 'MOTHER',
          );
          addTearDown(AuthState.instance.clear);
          final requestBodies = <Map<String, dynamic>>[];
          final context = TriageEntryContext.locked(
            stage: stage,
            origin: TriageOriginIntent.babyProfile,
            journeyId: null,
            originReferenceId: '20000000-0000-0000-0000-000000000020',
          );
          final service = TriageService(
            continuationStore: _RequestTestContinuationStore(),
            postRequest: (path, body) async {
              requestBodies.add(Map<String, dynamic>.from(body));
              return {
                'data': {
                  'status': 'ASK_MORE',
                  'intakeSessionId': '40000000-0000-0000-0000-000000000020',
                  'stage': stage.apiValue,
                  'mergedIntake': {'stage': stage.apiValue},
                  'questions': const [],
                  'round': 1,
                },
              };
            },
          );

          await service.startConversation(
            initialText: 'synthetic',
            currentIntake: {
              'stage': stage.apiValue,
              ...context.toLifecycleBindingJson(),
            },
          );

          expect(requestBodies, hasLength(1));
          final body = requestBodies.single;
          expect(body['journeyId'], isNull);
          expect(body['originDashboard'], 'BABY_PROFILE');
          expect(body['originReferenceId'], context.originReferenceId);
          expect(body['babyProfileId'], context.originReferenceId);
          expect(body['currentIntake'], {'stage': stage.apiValue});
        },
      );
    }
  });

  group('Story 6.7 production entry surfaces', () {
    final maternalFixtures =
        <({String stage, String journeyId, JourneyDashboard dashboard})>[
          (
            stage: 'PRECONCEPTION',
            journeyId: '10000000-0000-0000-0000-000000000011',
            dashboard: const JourneyDashboard(
              journeyId: '10000000-0000-0000-0000-000000000011',
              journeyType: 'PRE_PREGNANCY',
              status: 'PRE_PREGNANCY',
            ),
          ),
          (
            stage: 'PREGNANCY',
            journeyId: '10000000-0000-0000-0000-000000000012',
            dashboard: const JourneyDashboard(
              journeyId: '10000000-0000-0000-0000-000000000012',
              journeyType: 'PREGNANCY',
              status: 'ACTIVE_PREGNANCY',
              pregnancyWeek: 24,
            ),
          ),
          (
            stage: 'POSTPARTUM',
            journeyId: '10000000-0000-0000-0000-000000000013',
            dashboard: const JourneyDashboard(
              journeyId: '10000000-0000-0000-0000-000000000013',
              journeyType: 'POSTPARTUM',
              status: 'ACTIVE_POSTPARTUM',
            ),
          ),
        ];

    for (final fixture in maternalFixtures) {
      testWidgets('${fixture.stage} surface forwards its exact journey ID', (
        tester,
      ) async {
        await _openSurfaceAndAssertBinding(
          tester,
          origin: MotherJourneyScreen(
            loadData: false,
            initialDashboard: fixture.dashboard,
          ),
          stage: fixture.stage,
          journeyId: fixture.journeyId,
          originReferenceId: fixture.journeyId,
          originDashboard: 'MOTHER_JOURNEY',
        );
      });
    }

    final babyFixtures =
        <({String stage, String? journeyId, String babyId, Duration age})>[
          (
            stage: 'INFANT',
            journeyId: null,
            babyId: '20000000-0000-0000-0000-000000000014',
            age: const Duration(days: 180),
          ),
          (
            stage: 'TODDLER',
            journeyId: null,
            babyId: '20000000-0000-0000-0000-000000000015',
            age: const Duration(days: 500),
          ),
        ];

    for (final fixture in babyFixtures) {
      testWidgets(
        '${fixture.stage} surface forwards baby origin with no journey ID',
        (tester) async {
          final profile = BabyProfile(
            id: fixture.babyId,
            nickname: 'Synthetic safety profile',
            birthDate: DateTime.now().subtract(fixture.age),
            gender: BabyGender.unknown,
            isActive: true,
          );
          await _openSurfaceAndAssertBinding(
            tester,
            origin: BabyProfileDetailScreen(
              babyId: fixture.babyId,
              loadData: false,
              loadCareCollectionsData: false,
              initialProfile: profile,
            ),
            stage: fixture.stage,
            journeyId: fixture.journeyId,
            originReferenceId: fixture.babyId,
            originDashboard: 'BABY_PROFILE',
          );
        },
      );
    }
  });
}
