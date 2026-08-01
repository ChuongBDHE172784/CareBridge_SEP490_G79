import 'dart:ui' show SemanticsAction;

import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart' show DebugSemanticsDumpOrder;
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/aiTriage/models/triage_entry_context.dart';
import 'package:untitled/features/aiTriage/models/triage_intake_flow_model.dart';
import 'package:untitled/features/aiTriage/models/triage_result_model.dart';
import 'package:untitled/features/aiTriage/screens/symptom_intake_screen.dart';
import 'package:untitled/features/aiTriage/services/triage_service.dart';
import 'package:untitled/features/baby/models/baby_model.dart';
import 'package:untitled/features/baby/screens/baby_profile_detail_screen.dart';
import 'package:untitled/features/baby/services/baby_service.dart';
import 'package:untitled/features/emergency/models/emergency_session_model.dart';
import 'package:untitled/features/emergency/services/emergency_service.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/journey/screens/mother_journey_screen.dart';
import 'package:untitled/features/journey/services/journey_service.dart';

const _redSessionId = '11111111-1111-1111-1111-111111111111';

class _LockedRedTriageService extends TriageService {
  int starts = 0;
  Map<String, dynamic>? receivedIntake;

  @override
  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) async {
    starts++;
    receivedIntake = Map<String, dynamic>.from(currentIntake);
    final stage = currentIntake['stage'] as String;
    return IntakeFlowResponse(
      status: 'TRIAGE_COMPLETE',
      intakeSessionId: _redSessionId,
      stage: stage,
      mergedIntake: {'stage': stage, 'parentFreeText': initialText},
      round: 2,
      triageResult: TriageResult(
        sessionId: _redSessionId,
        stage: stage,
        status: 'COMPLETED',
        riskLevel: 'RED',
        emergencyActionRequired: true,
        summary: 'Safety fixture RED',
        recommendedAction: 'Open emergency support',
        disclaimer: 'Not a diagnosis',
      ),
    );
  }
}

class _ActiveEmergencyService extends EmergencyService {
  int activeCalls = 0;

  @override
  Future<EmergencySession?> getActive() async {
    activeCalls++;
    return const EmergencySession(
      sessionId: 'emergency-current-session',
      userId: 'mother-fixture',
      status: 'ACTIVE',
      triggerSource: 'AI_TRIAGE',
    );
  }
}

class _PostpartumJourneyService extends JourneyService {
  @override
  Future<JourneyDashboard> getDashboard() async => const JourneyDashboard(
    journeyId: 'journey-postpartum-runtime',
    journeyType: 'POSTPARTUM',
    status: 'ACTIVE_POSTPARTUM',
  );

  @override
  Future<List<JourneyTransition>> getHistory(String journeyId) async => [];
}

class _BabyListService extends BabyService {
  int listCalls = 0;

  @override
  Future<List<BabyProfile>> listBabyProfiles() async {
    listCalls++;
    return [
      BabyProfile(
        id: 'baby-postpartum-runtime',
        nickname: 'Runtime infant',
        birthDate: DateTime.now().subtract(const Duration(days: 180)),
        gender: BabyGender.unknown,
        isActive: true,
      ),
    ];
  }
}

Widget _entryProbe(GoRouterState state) {
  final entry = state.extra as TriageEntryContext;
  final lifecycleBinding = entry.toLifecycleBindingJson();
  return Scaffold(
    appBar: AppBar(leading: const BackButton()),
    body: Text(
      '${entry.stage.apiValue}:${entry.lockStage}:${entry.origin.name}:'
      '${lifecycleBinding['journeyId']}:'
      '${lifecycleBinding['originDashboard']}:'
      '${lifecycleBinding['originReferenceId']}:'
      '${lifecycleBinding['babyProfileId']}',
      key: const Key('triage-entry-probe'),
    ),
  );
}

GoRouter _maternalRouter(JourneyDashboard dashboard) => GoRouter(
  initialLocation: '/origin',
  routes: [
    GoRoute(
      path: '/origin',
      builder: (_, _) => Scaffold(
        body: MotherJourneyScreen(loadData: false, initialDashboard: dashboard),
      ),
    ),
    GoRoute(path: '/triage/intake', builder: (_, state) => _entryProbe(state)),
  ],
);

GoRouter _babyRouter(BabyProfile profile) => GoRouter(
  initialLocation: '/origin',
  routes: [
    GoRoute(
      path: '/origin',
      builder: (_, _) => BabyProfileDetailScreen(
        babyId: profile.id,
        loadData: false,
        loadCareCollectionsData: false,
        initialProfile: profile,
      ),
    ),
    GoRoute(path: '/triage/intake', builder: (_, state) => _entryProbe(state)),
  ],
);

GoRouter _redRoundTripRouter(
  _LockedRedTriageService triage,
  _ActiveEmergencyService emergency,
  Widget origin,
) => GoRouter(
  initialLocation: '/origin',
  routes: [
    GoRoute(path: '/origin', builder: (_, _) => origin),
    GoRoute(
      path: '/triage/intake',
      builder: (_, state) => SymptomIntakeScreen(
        entryContext: state.extra as TriageEntryContext,
        triageService: triage,
        emergencyService: emergency,
      ),
    ),
    GoRoute(
      path: '/emergency/map',
      builder: (_, state) {
        final session = state.extra as EmergencySession;
        return Scaffold(
          appBar: AppBar(leading: const BackButton()),
          body: Text(
            '${session.sessionId}:${state.uri.queryParameters['stage']}',
            key: const Key('emergency-session-probe'),
          ),
        );
      },
    ),
  ],
);

Future<void> _openAndReturn(
  WidgetTester tester, {
  required GoRouter router,
  required Key actionKey,
  required String expectedTitle,
  required String expectedDescription,
  required String expectedProbe,
}) async {
  await tester.pumpWidget(MaterialApp.router(routerConfig: router));
  await tester.pumpAndSettle();

  final action = find.byKey(actionKey);
  await tester.scrollUntilVisible(
    action,
    300,
    scrollable: find.byType(Scrollable).first,
  );
  expect(action, findsOneWidget);
  expect(find.text(expectedTitle), findsOneWidget);
  expect(tester.getSize(action).height, greaterThanOrEqualTo(48));
  final semanticsNode = tester.getSemantics(action);
  final semanticsData = semanticsNode.getSemanticsData();
  expect(semanticsData.label, '$expectedTitle. $expectedDescription');
  expect(semanticsData.hasAction(SemanticsAction.tap), isTrue);
  expect(
    semanticsNode.debugListChildrenInOrder(
      DebugSemanticsDumpOrder.traversalOrder,
    ),
    isEmpty,
  );

  // WidgetTester does not expose a public semantics-action helper on Flutter
  // 3.38, so the test must invoke the active test pipeline's owner directly.
  // ignore: deprecated_member_use
  tester.binding.pipelineOwner.semanticsOwner!.performAction(
    semanticsNode.id,
    SemanticsAction.tap,
  );
  await tester.pumpAndSettle();
  expect(find.byKey(const Key('triage-entry-probe')), findsOneWidget);
  expect(find.text(expectedProbe), findsOneWidget);

  await tester.tap(find.byType(BackButton));
  await tester.pumpAndSettle();
  expect(find.byKey(actionKey), findsOneWidget);
}

void main() {
  testWidgets('POSTPARTUM runtime loads linked babies for baby origins', (
    tester,
  ) async {
    final babyService = _BabyListService();

    await tester.pumpWidget(
      MaterialApp(
        home: MotherJourneyScreen(
          journeyService: _PostpartumJourneyService(),
          babyService: babyService,
        ),
      ),
    );
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 100));

    expect(babyService.listCalls, 1);
  });

  final maternalCases =
      <({String apiStage, String journeyType, String status})>[
        (
          apiStage: 'PRECONCEPTION',
          journeyType: 'PRE_PREGNANCY',
          status: 'PRE_PREGNANCY',
        ),
        (
          apiStage: 'PREGNANCY',
          journeyType: 'PREGNANCY',
          status: 'ACTIVE_PREGNANCY',
        ),
        (
          apiStage: 'POSTPARTUM',
          journeyType: 'POSTPARTUM',
          status: 'ACTIVE_POSTPARTUM',
        ),
      ];

  for (final fixture in maternalCases) {
    testWidgets(
      '${fixture.apiStage} origin opens a locked typed intake and returns safely',
      (tester) async {
        final journeyId = 'journey-${fixture.apiStage.toLowerCase()}';
        final router = _maternalRouter(
          JourneyDashboard(
            journeyId: journeyId,
            journeyType: fixture.journeyType,
            status: fixture.status,
            pregnancyWeek: fixture.apiStage == 'PREGNANCY' ? 24 : null,
          ),
        );
        addTearDown(router.dispose);

        await _openAndReturn(
          tester,
          router: router,
          actionKey: Key(
            'triage-safety-entry-${fixture.apiStage.toLowerCase()}',
          ),
          expectedTitle: 'AI Triage - Kiểm tra triệu chứng',
          expectedDescription:
              'Đánh giá nhanh theo đúng giai đoạn sức khỏe hiện tại.',
          expectedProbe:
              '${fixture.apiStage}:true:'
              '${TriageOriginIntent.motherJourney.name}:'
              '$journeyId:MOTHER_JOURNEY:$journeyId:null',
        );
      },
    );
  }

  for (final stage in const [
    TriageStageIntent.preconception,
    TriageStageIntent.pregnancy,
  ]) {
    testWidgets(
      '${stage.apiValue} intake uses maternal copy and excludes pediatric fields',
      (tester) async {
        final triage = _LockedRedTriageService();
        await tester.pumpWidget(
          MaterialApp(
            home: SymptomIntakeScreen(
              triageService: triage,
              entryContext: TriageEntryContext.locked(
                stage: stage,
                origin: TriageOriginIntent.motherJourney,
              ),
            ),
          ),
        );

        expect(find.text('Kiểm tra dấu hiệu an toàn'), findsOneWidget);
        expect(find.textContaining('dấu hiệu bạn đang gặp'), findsOneWidget);
        expect(find.textContaining('triệu chứng của bé'), findsNothing);
        expect(find.text('Ví dụ: Bé bị sốt và ho...'), findsNothing);
        expect(find.text('Mô tả dấu hiệu của mẹ...'), findsOneWidget);
        await tester.enterText(find.byType(TextField).first, 'khó thở');
        await tester.tap(find.byKey(const Key('triage-chat-send')));
        await tester.pump();

        expect(triage.receivedIntake?['stage'], stage.apiValue);
        for (final pediatricField in const [
          'childAgeMonths',
          'feedingStatus',
          'vomiting',
          'diarrhea',
          'rash',
          'dehydrationSigns',
        ]) {
          expect(triage.receivedIntake?.containsKey(pediatricField), isFalse);
        }
      },
    );
  }

  final babyCases = <({String apiStage, Duration age})>[
    (apiStage: 'INFANT', age: const Duration(days: 180)),
    (apiStage: 'TODDLER', age: const Duration(days: 500)),
  ];

  for (final fixture in babyCases) {
    testWidgets(
      '${fixture.apiStage} origin opens a locked typed intake and returns safely',
      (tester) async {
        final profileId = 'baby-${fixture.apiStage.toLowerCase()}';
        final profile = BabyProfile(
          id: profileId,
          nickname: 'Safety fixture',
          birthDate: DateTime.now().subtract(fixture.age),
          gender: BabyGender.unknown,
          isActive: true,
        );
        final router = _babyRouter(profile);
        addTearDown(router.dispose);

        await _openAndReturn(
          tester,
          router: router,
          actionKey: Key(
            'triage-safety-entry-${fixture.apiStage.toLowerCase()}',
          ),
          expectedTitle: 'AI Triage - Kiểm tra triệu chứng của bé',
          expectedDescription:
              'Đánh giá nhanh theo đúng độ tuổi hiện tại của bé.',
          expectedProbe:
              '${fixture.apiStage}:true:'
              '${TriageOriginIntent.babyProfile.name}:'
              'null:BABY_PROFILE:$profileId:$profileId',
        );
      },
    );
  }

  final redRoundTripCases = <({String stage, Widget origin})>[
    (
      stage: 'PRECONCEPTION',
      origin: const Scaffold(
        body: MotherJourneyScreen(
          loadData: false,
          initialDashboard: JourneyDashboard(
            journeyId: 'journey-preconception-round-trip',
            journeyType: 'PRE_PREGNANCY',
            status: 'PRE_PREGNANCY',
          ),
        ),
      ),
    ),
    (
      stage: 'PREGNANCY',
      origin: const Scaffold(
        body: MotherJourneyScreen(
          loadData: false,
          initialDashboard: JourneyDashboard(
            journeyId: 'journey-pregnancy-round-trip',
            journeyType: 'PREGNANCY',
            status: 'ACTIVE_PREGNANCY',
            pregnancyWeek: 24,
          ),
        ),
      ),
    ),
    (
      stage: 'POSTPARTUM',
      origin: const Scaffold(
        body: MotherJourneyScreen(
          loadData: false,
          initialDashboard: JourneyDashboard(
            journeyId: 'journey-postpartum-round-trip',
            journeyType: 'POSTPARTUM',
            status: 'ACTIVE_POSTPARTUM',
          ),
        ),
      ),
    ),
    (
      stage: 'INFANT',
      origin: BabyProfileDetailScreen(
        babyId: 'baby-infant-round-trip',
        loadData: false,
        loadCareCollectionsData: false,
        initialProfile: BabyProfile(
          id: 'baby-infant-round-trip',
          nickname: 'Infant safety fixture',
          birthDate: DateTime.now().subtract(const Duration(days: 180)),
          gender: BabyGender.unknown,
          isActive: true,
        ),
      ),
    ),
    (
      stage: 'TODDLER',
      origin: BabyProfileDetailScreen(
        babyId: 'baby-toddler-round-trip',
        loadData: false,
        loadCareCollectionsData: false,
        initialProfile: BabyProfile(
          id: 'baby-toddler-round-trip',
          nickname: 'Toddler safety fixture',
          birthDate: DateTime.now().subtract(const Duration(days: 500)),
          gender: BabyGender.unknown,
          isActive: true,
        ),
      ),
    ),
  ];

  for (final fixture in redRoundTripCases) {
    testWidgets(
      '${fixture.stage} RED emergency round trip retains session and returns to origin',
      (tester) async {
        final triage = _LockedRedTriageService();
        final emergency = _ActiveEmergencyService();
        final router = _redRoundTripRouter(triage, emergency, fixture.origin);
        addTearDown(router.dispose);

        await tester.pumpWidget(MaterialApp.router(routerConfig: router));
        await tester.pumpAndSettle();
        final actionKey = Key(
          'triage-safety-entry-${fixture.stage.toLowerCase()}',
        );
        final originAction = find.byKey(actionKey);
        await tester.scrollUntilVisible(
          originAction,
          300,
          scrollable: find.byType(Scrollable).first,
        );
        await tester.tap(originAction);
        await tester.pumpAndSettle();

        await tester.enterText(find.byType(TextField).first, 'khó thở');
        await tester.tap(find.byKey(const Key('triage-chat-send')));
        await tester.pump();
        expect(find.text('Mức rủi ro: RED'), findsOneWidget);
        expect(triage.starts, 1);

        final emergencyAction = find.byKey(const Key('triage-emergency-cta'));
        await tester.scrollUntilVisible(
          emergencyAction,
          300,
          scrollable: find.byType(Scrollable).first,
        );
        await tester.tap(emergencyAction);
        await tester.pumpAndSettle();
        expect(
          find.byKey(const Key('emergency-session-probe')),
          findsOneWidget,
        );
        expect(
          find.text('emergency-current-session:${fixture.stage}'),
          findsOneWidget,
        );
        expect(emergency.activeCalls, 1);

        await tester.tap(find.byType(BackButton));
        await tester.pumpAndSettle();
        expect(find.text('Mức rủi ro: RED'), findsOneWidget);
        expect(triage.starts, 1);

        router.pop();
        await tester.pumpAndSettle();
        expect(find.byKey(actionKey), findsOneWidget);
      },
    );
  }
}
