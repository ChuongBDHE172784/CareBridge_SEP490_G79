import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/journey/models/journey_onboarding_model.dart';
import 'package:untitled/features/journey/screens/mother_stage_selection_screen.dart';
import 'package:untitled/features/journey/services/journey_onboarding_draft_storage.dart';
import 'package:untitled/features/journey/services/journey_onboarding_service.dart';
import 'package:untitled/features/journey/services/journey_service.dart';

class _FakeOnboardingService extends JourneyOnboardingService {
  _FakeOnboardingService({
    this.complete = false,
    this.failures = 0,
    this.statusFuture,
  });

  final bool complete;
  int failures;
  final Future<JourneyOnboardingStatus>? statusFuture;
  final List<JourneyOnboardingRequest> submissions = [];

  @override
  Future<JourneyOnboardingStatus> getStatus() async =>
      statusFuture ??
      JourneyOnboardingStatus(
        baselineComplete: complete,
        consentValid: complete,
        baselineRevision: complete ? 1 : 0,
      );

  @override
  Future<JourneyOnboardingStatus> submit(
    JourneyOnboardingRequest request,
  ) async {
    submissions.add(request);
    if (failures > 0) {
      failures--;
      throw ApiException(503, 'temporary');
    }
    return const JourneyOnboardingStatus(
      baselineComplete: true,
      consentValid: true,
      baselineRevision: 1,
    );
  }
}

class _FakeDraftStorage extends JourneyOnboardingDraftStorage {
  _FakeDraftStorage({this.failClear = false});

  final bool failClear;
  Map<String, dynamic>? draft;
  final List<Map<String, dynamic>> writes = [];
  int clearCalls = 0;

  @override
  Future<Map<String, dynamic>?> read() async => draft;

  @override
  Future<void> write(Map<String, dynamic> value) async {
    draft = Map<String, dynamic>.from(value);
    writes.add(Map<String, dynamic>.from(value));
  }

  @override
  Future<void> clear() async {
    clearCalls++;
    if (failClear) throw StateError('secure storage unavailable');
    draft = null;
  }
}

class _FakeJourneyService extends JourneyService {
  final List<CreateJourneyRequest> submissions = [];

  @override
  Future<CreateJourneyResponse> createJourney(
    CreateJourneyRequest request,
  ) async {
    submissions.add(request);
    return CreateJourneyResponse(
      id: 'journey-1',
      journeyType: request.journeyType.toApiValue(),
      status: 'ACTIVE',
      startDate: request.startDate,
      createdAt: '2026-07-28T00:00:00Z',
    );
  }
}

Future<GoRouter> _pumpScreen(
  WidgetTester tester, {
  required _FakeOnboardingService onboarding,
  required _FakeDraftStorage drafts,
  JourneyService? journeyService,
  Future<void> Function()? refreshSession,
  bool settle = true,
}) async {
  final router = GoRouter(
    initialLocation: '/mother-stage-selection',
    routes: [
      GoRoute(
        path: '/mother-stage-selection',
        builder: (_, _) => MotherStageSelectionScreen(
          onboardingService: onboarding,
          draftStorage: drafts,
          journeyService: journeyService,
          refreshSession: refreshSession,
        ),
      ),
      GoRoute(
        path: '/journey-setup',
        builder: (_, _) => const Scaffold(
          body: Text('pregnant-route', key: Key('pregnant-route')),
        ),
      ),
      GoRoute(
        path: '/postpartum-recovery-setup',
        builder: (_, _) => const Scaffold(
          body: Text('postpartum-route', key: Key('postpartum-route')),
        ),
      ),
      GoRoute(
        path: '/babies/add',
        builder: (_, _) =>
            const Scaffold(body: Text('baby-route', key: Key('baby-route'))),
      ),
      GoRoute(
        path: '/',
        builder: (_, _) =>
            const Scaffold(body: Text('home-route', key: Key('home-route'))),
      ),
      // Creating a journey now hands the user to the personalisation questionnaire, carrying
      // the stage in `extra`, instead of dropping them back on Home.
      GoRoute(
        path: '/recommendation-profile',
        builder: (_, state) => Scaffold(
          body: Text(
            'recommendation-profile:${state.extra}',
            key: const Key('recommendation-profile-route'),
          ),
        ),
      ),
    ],
  );
  await tester.pumpWidget(MaterialApp.router(routerConfig: router));
  if (settle) {
    await tester.pumpAndSettle();
  } else {
    await tester.pump();
  }
  return router;
}

Future<void> _selectPregnantWithConsent(WidgetTester tester) async {
  await _selectStageWithConsent(tester, 'mother-stage-pregnant');
}

Future<void> _selectStageWithConsent(
  WidgetTester tester,
  String stageKey,
) async {
  await _tapStage(tester, stageKey);
  final preference = find.byKey(const Key('preference-NUTRITION'));
  await tester.ensureVisible(preference);
  await tester.tap(preference);
  final consent = find.byKey(const Key('lifecycle-consent'));
  await tester.ensureVisible(consent);
  await tester.tap(consent);
  await tester.pump();
}

Future<void> _tapStage(WidgetTester tester, String key) async {
  final finder = find.byKey(Key(key));
  await tester.ensureVisible(finder);
  await tester.pumpAndSettle();
  await tester.tap(finder);
  await tester.pump();
}

void main() {
  testWidgets('restore blocks interaction until authoritative status arrives', (
    tester,
  ) async {
    final status = Completer<JourneyOnboardingStatus>();
    final service = _FakeOnboardingService(statusFuture: status.future);
    final router = await _pumpScreen(
      tester,
      onboarding: service,
      drafts: _FakeDraftStorage(),
      settle: false,
    );
    addTearDown(router.dispose);

    await tester.ensureVisible(find.byKey(const Key('mother-stage-pregnant')));
    await tester.tap(find.byKey(const Key('mother-stage-pregnant')));
    await tester.pump();
    expect(find.byKey(const Key('lifecycle-consent')), findsNothing);
    expect(service.submissions, isEmpty);

    status.complete(
      const JourneyOnboardingStatus(
        baselineComplete: false,
        consentValid: false,
        baselineRevision: 0,
      ),
    );
    await tester.pumpAndSettle();
    await _tapStage(tester, 'mother-stage-pregnant');
    expect(find.byKey(const Key('lifecycle-consent')), findsOneWidget);
  });

  testWidgets('consolidated screen keeps stage and consent unselected', (
    tester,
  ) async {
    final router = await _pumpScreen(
      tester,
      onboarding: _FakeOnboardingService(),
      drafts: _FakeDraftStorage(),
    );
    addTearDown(router.dispose);

    expect(find.byKey(const Key('lifecycle-consent')), findsNothing);
    await _tapStage(tester, 'mother-stage-pregnant');

    final consent = tester.widget<CheckboxListTile>(
      find.byKey(const Key('lifecycle-consent')),
    );
    expect(consent.value, isFalse);
    expect(find.byKey(const Key('preference-NUTRITION')), findsOneWidget);
  });

  testWidgets('missing preference and consent produce no API mutation', (
    tester,
  ) async {
    final service = _FakeOnboardingService();
    final router = await _pumpScreen(
      tester,
      onboarding: service,
      drafts: _FakeDraftStorage(),
    );
    addTearDown(router.dispose);

    await _tapStage(tester, 'mother-stage-pregnant');
    await tester.tap(find.byKey(const Key('mother-stage-continue')));
    await tester.pump();
    expect(
      find.text('Vui lòng chọn ít nhất một nội dung hỗ trợ.'),
      findsOneWidget,
    );
    expect(service.submissions, isEmpty);

    final preference = find.byKey(const Key('preference-NUTRITION'));
    await tester.ensureVisible(preference);
    await tester.tap(preference);
    await tester.tap(find.byKey(const Key('mother-stage-continue')));
    await tester.pump();
    expect(
      find.text('Vui lòng đọc và đồng ý trước khi tiếp tục.'),
      findsOneWidget,
    );
    expect(service.submissions, isEmpty);
  });

  testWidgets('valid selection submits onboarding before pregnant route', (
    tester,
  ) async {
    final service = _FakeOnboardingService();
    final drafts = _FakeDraftStorage();
    final router = await _pumpScreen(
      tester,
      onboarding: service,
      drafts: drafts,
    );
    addTearDown(router.dispose);

    await _selectPregnantWithConsent(tester);
    await tester.tap(find.byKey(const Key('mother-stage-continue')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('pregnant-route')), findsOneWidget);
    expect(service.submissions, hasLength(1));
    expect(
      service.submissions.single.lifecycleGoal,
      LifecycleGoal.currentlyPregnant,
    );
    expect(service.submissions.single.preferences, [
      SupportPreference.nutrition,
    ]);
    expect(service.submissions.single.consentAccepted, isTrue);
    expect(drafts.clearCalls, 1);
  });

  testWidgets('retry reuses submission id and does not duplicate success', (
    tester,
  ) async {
    final service = _FakeOnboardingService(failures: 1);
    final drafts = _FakeDraftStorage();
    final router = await _pumpScreen(
      tester,
      onboarding: service,
      drafts: drafts,
    );
    addTearDown(router.dispose);

    await _selectPregnantWithConsent(tester);
    await tester.tap(find.byKey(const Key('mother-stage-continue')));
    await tester.pumpAndSettle();
    expect(service.submissions, hasLength(1));
    expect(find.byKey(const Key('pregnant-route')), findsNothing);

    await tester.tap(find.byKey(const Key('mother-stage-continue')));
    await tester.pumpAndSettle();
    expect(service.submissions, hasLength(2));
    expect(
      service.submissions[0].submissionId,
      service.submissions[1].submissionId,
    );
    expect(find.byKey(const Key('pregnant-route')), findsOneWidget);
  });

  testWidgets('restored draft keeps stage, preferences, and submission id', (
    tester,
  ) async {
    const submissionId = '00000000-0000-4000-8000-000000006200';
    final service = _FakeOnboardingService();
    final drafts = _FakeDraftStorage()
      ..draft = {
        'submissionId': submissionId,
        'stage': 'pregnant',
        'lifecycleGoal': 'CURRENTLY_PREGNANT',
        'preferences': ['NUTRITION'],
        'consentAccepted': true,
      };
    final router = await _pumpScreen(
      tester,
      onboarding: service,
      drafts: drafts,
    );
    addTearDown(router.dispose);

    expect(
      tester
          .widget<FilterChip>(find.byKey(const Key('preference-NUTRITION')))
          .selected,
      isTrue,
    );
    expect(
      tester
          .widget<CheckboxListTile>(find.byKey(const Key('lifecycle-consent')))
          .value,
      isFalse,
    );
    await tester.ensureVisible(find.byKey(const Key('lifecycle-consent')));
    await tester.tap(find.byKey(const Key('lifecycle-consent')));
    await tester.tap(find.byKey(const Key('mother-stage-continue')));
    await tester.pumpAndSettle();

    expect(service.submissions.single.submissionId, submissionId);
  });

  testWidgets('remote success routes even when local draft cleanup fails', (
    tester,
  ) async {
    final service = _FakeOnboardingService();
    final router = await _pumpScreen(
      tester,
      onboarding: service,
      drafts: _FakeDraftStorage(failClear: true),
    );
    addTearDown(router.dispose);

    await _selectPregnantWithConsent(tester);
    await tester.tap(find.byKey(const Key('mother-stage-continue')));
    await tester.pumpAndSettle();

    expect(service.submissions, hasLength(1));
    expect(find.byKey(const Key('pregnant-route')), findsOneWidget);
  });

  testWidgets('existing valid onboarding routes without consent prompt', (
    tester,
  ) async {
    final service = _FakeOnboardingService(complete: true);
    final router = await _pumpScreen(
      tester,
      onboarding: service,
      drafts: _FakeDraftStorage(),
    );
    addTearDown(router.dispose);

    await _tapStage(tester, 'mother-stage-postpartum');
    expect(find.byKey(const Key('lifecycle-consent')), findsNothing);
    await tester.tap(find.byKey(const Key('mother-stage-continue')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('postpartum-route')), findsOneWidget);
    expect(service.submissions, isEmpty);
  });

  testWidgets('planning preserves existing create-journey branch', (
    tester,
  ) async {
    final journey = _FakeJourneyService();
    var refreshCalls = 0;
    final router = await _pumpScreen(
      tester,
      onboarding: _FakeOnboardingService(complete: true),
      drafts: _FakeDraftStorage(),
      journeyService: journey,
      refreshSession: () async => refreshCalls++,
    );
    addTearDown(router.dispose);

    await _tapStage(tester, 'mother-stage-planning');
    await tester.tap(find.byKey(const Key('mother-stage-continue')));
    await tester.pumpAndSettle();

    expect(journey.submissions, hasLength(1));
    expect(journey.submissions.single.journeyType, JourneyType.prePregnancy);
    expect(refreshCalls, 1);
    expect(find.byKey(const Key('recommendation-profile-route')), findsOneWidget);
    expect(find.text('recommendation-profile:PRE_PREGNANCY'), findsOneWidget);
  });

  testWidgets('fresh planning submits mapped onboarding before journey', (
    tester,
  ) async {
    final onboarding = _FakeOnboardingService();
    final journey = _FakeJourneyService();
    final router = await _pumpScreen(
      tester,
      onboarding: onboarding,
      drafts: _FakeDraftStorage(),
      journeyService: journey,
      refreshSession: () async {},
    );
    addTearDown(router.dispose);

    await _selectStageWithConsent(tester, 'mother-stage-planning');
    await tester.tap(find.byKey(const Key('mother-stage-continue')));
    await tester.pumpAndSettle();

    expect(
      onboarding.submissions.single.lifecycleGoal,
      LifecycleGoal.preparingForPregnancy,
    );
    expect(journey.submissions, hasLength(1));
  });

  testWidgets('fresh postpartum submits mapped onboarding before route', (
    tester,
  ) async {
    final onboarding = _FakeOnboardingService();
    final router = await _pumpScreen(
      tester,
      onboarding: onboarding,
      drafts: _FakeDraftStorage(),
    );
    addTearDown(router.dispose);

    await _selectStageWithConsent(tester, 'mother-stage-postpartum');
    await tester.tap(find.byKey(const Key('mother-stage-continue')));
    await tester.pumpAndSettle();

    expect(
      onboarding.submissions.single.lifecycleGoal,
      LifecycleGoal.postpartumRecovery,
    );
    expect(find.byKey(const Key('postpartum-route')), findsOneWidget);
  });

  testWidgets('baby-care branch remains independent of mother consent', (
    tester,
  ) async {
    final service = _FakeOnboardingService();
    final router = await _pumpScreen(
      tester,
      onboarding: service,
      drafts: _FakeDraftStorage(),
    );
    addTearDown(router.dispose);

    await _tapStage(tester, 'mother-stage-baby-care');
    expect(find.byKey(const Key('lifecycle-consent')), findsNothing);
    await tester.tap(find.byKey(const Key('mother-stage-continue')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('baby-route')), findsOneWidget);
    expect(service.submissions, isEmpty);
  });
}
