import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/journey/models/journey_onboarding_model.dart';
import 'package:untitled/features/journey/screens/journey_onboarding_screen.dart';
import 'package:untitled/features/journey/services/journey_onboarding_draft_storage.dart';
import 'package:untitled/features/journey/services/journey_onboarding_service.dart';

class _FakeOnboardingService extends JourneyOnboardingService {
  _FakeOnboardingService({
    this.initialStatus = const JourneyOnboardingStatus(
      baselineComplete: false,
      consentValid: false,
      baselineRevision: 0,
    ),
    this.failFirstSubmit = false,
  });

  final JourneyOnboardingStatus initialStatus;
  final bool failFirstSubmit;
  int submitCalls = 0;

  @override
  Future<JourneyOnboardingStatus> getStatus() async => initialStatus;

  @override
  Future<JourneyOnboardingStatus> submit(
    JourneyOnboardingRequest request,
  ) async {
    submitCalls++;
    if (failFirstSubmit && submitCalls == 1) {
      throw ApiException(503, 'synthetic outage');
    }
    return const JourneyOnboardingStatus(
      baselineComplete: true,
      consentValid: true,
      baselineRevision: 1,
    );
  }
}

class _FakeDraftStorage extends JourneyOnboardingDraftStorage {
  _FakeDraftStorage([this.value]);

  Map<String, dynamic>? value;
  int writes = 0;
  int clears = 0;

  @override
  Future<Map<String, dynamic>?> read() async => value;

  @override
  Future<void> write(Map<String, dynamic> draft) async {
    writes++;
    value = Map<String, dynamic>.from(draft);
  }

  @override
  Future<void> clear() async {
    clears++;
    value = null;
  }
}

class _DelayedDraftStorage extends JourneyOnboardingDraftStorage {
  final writes = <Map<String, dynamic>>[];
  Map<String, dynamic>? value;

  @override
  Future<Map<String, dynamic>?> read() async => value;

  @override
  Future<void> write(Map<String, dynamic> draft) async {
    final snapshot = Map<String, dynamic>.from(draft);
    if (writes.isEmpty) {
      await Future<void>.delayed(const Duration(milliseconds: 80));
    }
    writes.add(snapshot);
    value = snapshot;
  }
}

Future<void> _pumpOnboarding(
  WidgetTester tester, {
  required JourneyOnboardingService service,
  required JourneyOnboardingDraftStorage draftStorage,
}) async {
  final router = GoRouter(
    initialLocation: '/onboarding',
    routes: [
      GoRoute(
        path: '/onboarding',
        builder: (_, _) => JourneyOnboardingScreen(
          service: service,
          draftStorage: draftStorage,
        ),
      ),
      GoRoute(
        path: '/mother-stage-selection',
        builder: (_, _) =>
            const Text('stage-selection', key: Key('stage-selection-probe')),
      ),
    ],
  );
  addTearDown(router.dispose);
  await tester.pumpWidget(MaterialApp.router(routerConfig: router));
  await tester.pumpAndSettle();
}

Future<void> _completeForm(WidgetTester tester) async {
  await tester.tap(find.byKey(const Key('baseline-goal')));
  await tester.pumpAndSettle();
  await tester.tap(find.text('Đang mang thai').last);
  await tester.pumpAndSettle();
  await tester.tap(find.byKey(const Key('preference-NUTRITION')));
  await tester.tap(find.byKey(const Key('onboarding-continue')));
  await tester.pumpAndSettle();
  await tester.scrollUntilVisible(
    find.byKey(const Key('lifecycle-consent')),
    300,
    scrollable: find.byType(Scrollable).first,
  );
  await tester.tap(find.byKey(const Key('lifecycle-consent')));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('consent is explicit, unchecked, and controls are reachable', (
    tester,
  ) async {
    await _pumpOnboarding(
      tester,
      service: _FakeOnboardingService(),
      draftStorage: _FakeDraftStorage(),
    );

    expect(find.byKey(const Key('baseline-goal')), findsOneWidget);
    expect(find.byKey(const Key('lifecycle-consent')), findsNothing);
    await tester.tap(find.byKey(const Key('baseline-goal')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Đang mang thai').last);
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.byKey(const Key('preference-NUTRITION')));
    await tester.tap(find.byKey(const Key('preference-NUTRITION')));
    await tester.tap(find.byKey(const Key('onboarding-continue')));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.byKey(const Key('lifecycle-consent')),
      300,
      scrollable: find.byType(Scrollable).first,
    );
    final checkbox = tester.widget<CheckboxListTile>(
      find.byKey(const Key('lifecycle-consent')),
    );
    expect(checkbox.value, isFalse);
  });

  testWidgets('draft restores baseline but never restores cached consent', (
    tester,
  ) async {
    await _pumpOnboarding(
      tester,
      service: _FakeOnboardingService(),
      draftStorage: _FakeDraftStorage({
        'submissionId': '00000000-0000-4000-8000-000000006200',
        'lifecycleGoal': 'CURRENTLY_PREGNANT',
        'preferences': ['NUTRITION'],
        'consentAccepted': true,
      }),
    );

    final goal = tester.widget<DropdownButtonFormField<LifecycleGoal>>(
      find.byKey(const Key('baseline-goal')),
    );
    expect(goal.initialValue, LifecycleGoal.currentlyPregnant);
    final chip = tester.widget<FilterChip>(
      find.byKey(const Key('preference-NUTRITION')),
    );
    expect(chip.selected, isTrue);
    await tester.tap(find.byKey(const Key('onboarding-continue')));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.byKey(const Key('lifecycle-consent')),
      300,
      scrollable: find.byType(Scrollable).first,
    );
    expect(
      tester
          .widget<CheckboxListTile>(find.byKey(const Key('lifecycle-consent')))
          .value,
      isFalse,
    );
  });

  testWidgets('malformed valid JSON fields are ignored without crashing', (
    tester,
  ) async {
    await _pumpOnboarding(
      tester,
      service: _FakeOnboardingService(),
      draftStorage: _FakeDraftStorage({
        'submissionId': 42,
        'lifecycleGoal': ['CURRENTLY_PREGNANT'],
        'preferences': ['NUTRITION', 7, null, 'UNKNOWN'],
      }),
    );

    expect(find.byKey(const Key('baseline-goal')), findsOneWidget);
    expect(tester.takeException(), isNull);
    expect(
      tester
          .widget<FilterChip>(find.byKey(const Key('preference-NUTRITION')))
          .selected,
      isTrue,
    );
  });

  testWidgets('rapid baseline edits persist the newest snapshot last', (
    tester,
  ) async {
    final draft = _DelayedDraftStorage();
    await _pumpOnboarding(
      tester,
      service: _FakeOnboardingService(),
      draftStorage: draft,
    );

    await tester.tap(find.byKey(const Key('baseline-goal')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Đang mang thai').last);
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.byKey(const Key('preference-NUTRITION')));
    await tester.tap(find.byKey(const Key('preference-NUTRITION')));
    await tester.tap(find.byKey(const Key('preference-MENTAL_WELLBEING')));
    await tester.pump(const Duration(milliseconds: 250));

    expect(
      draft.value?['preferences'],
      containsAll(['NUTRITION', 'MENTAL_WELLBEING']),
    );
  });

  testWidgets('transient submit failure preserves draft and retry succeeds', (
    tester,
  ) async {
    final service = _FakeOnboardingService(failFirstSubmit: true);
    final draft = _FakeDraftStorage();
    await _pumpOnboarding(tester, service: service, draftStorage: draft);
    await _completeForm(tester);

    await tester.tap(find.byKey(const Key('onboarding-continue')));
    await tester.pumpAndSettle();
    expect(find.textContaining('Dịch vụ đang tạm gián đoạn'), findsOneWidget);
    expect(draft.value, isNotNull);
    expect(draft.clears, 0);

    await tester.tap(find.byKey(const Key('onboarding-continue')));
    await tester.pumpAndSettle();
    expect(service.submitCalls, 2);
    expect(draft.clears, 1);
    expect(find.byKey(const Key('stage-selection-probe')), findsOneWidget);
  });

  testWidgets('background and resume preserve unfinished selections', (
    tester,
  ) async {
    await _pumpOnboarding(
      tester,
      service: _FakeOnboardingService(),
      draftStorage: _FakeDraftStorage(),
    );
    await _completeForm(tester);

    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    await tester.pump();
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pumpAndSettle();

    expect(
      tester
          .widget<CheckboxListTile>(find.byKey(const Key('lifecycle-consent')))
          .value,
      isTrue,
    );
    await tester.tap(find.byKey(const Key('onboarding-back')));
    await tester.pumpAndSettle();
    expect(
      tester
          .widget<FilterChip>(find.byKey(const Key('preference-NUTRITION')))
          .selected,
      isTrue,
    );
  });

  testWidgets('revoked authoritative status does not bypass onboarding', (
    tester,
  ) async {
    await _pumpOnboarding(
      tester,
      service: _FakeOnboardingService(
        initialStatus: const JourneyOnboardingStatus(
          baselineComplete: true,
          consentValid: false,
          baselineRevision: 2,
        ),
      ),
      draftStorage: _FakeDraftStorage(),
    );

    expect(find.byKey(const Key('baseline-goal')), findsOneWidget);
    expect(find.byKey(const Key('stage-selection-probe')), findsNothing);
  });
}
