import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/journey/screens/journey_setup_screen.dart';
import 'package:untitled/features/journey/screens/mother_journey_screen.dart';
import 'package:untitled/features/journey/services/journey_service.dart';

class _HistoryFailureJourneyService extends JourneyService {
  _HistoryFailureJourneyService(this.dashboard);

  final JourneyDashboard dashboard;

  @override
  Future<JourneyDashboard> getDashboard() async => dashboard;

  @override
  Future<List<JourneyTransition>> getHistory(String journeyId) async {
    throw StateError('synthetic history failure');
  }
}

JourneyTransition _transition({String toStage = 'PRE_PREGNANCY'}) {
  return JourneyTransition.fromJson({
    'transitionId': 'transition-1',
    'eventType': 'CREATED',
    'fromStage': null,
    'toStage': toStage,
    'changedFields': ['journeyType'],
    'source': 'SELF_REPORTED',
    'confidence': 'ESTIMATED',
    'reason': 'INITIAL_SETUP',
    'effectiveAt': '2026-07-18T03:00:00Z',
    'recordedAt': '2026-07-18T03:00:01Z',
    'journeyVersion': 1,
  });
}

void main() {
  group('Story 6.1 canonical request contract', () {
    test('pregnancy create includes date provenance and audit context', () {
      final json = const CreateJourneyRequest(
        journeyType: JourneyType.pregnancy,
        startDate: '2026-07-18',
        lastMenstrualDate: '2026-05-01',
        datingBasis: 'LMP',
      ).toJson();

      expect(json['datingBasis'], 'LMP');
      expect(json.containsKey('estimatedDueDate'), isFalse);
      expect(json['dateSource'], 'SELF_REPORTED');
      expect(json['dateConfidence'], 'ESTIMATED');
      expect(json['changeReason'], 'INITIAL_SETUP');
      expect(DateTime.tryParse(json['effectiveAt'] as String), isNotNull);
    });

    test('stage-only create retains explicit audit context', () {
      final json = const CreateJourneyRequest(
        journeyType: JourneyType.prePregnancy,
        startDate: '2026-07-18',
        dateSource: 'SELF_REPORTED',
        dateConfidence: 'ESTIMATED',
        changeReason: 'INITIAL_SETUP',
        effectiveAt: '2026-07-18T03:00:00Z',
      ).toJson();

      expect(json['dateSource'], 'SELF_REPORTED');
      expect(json['dateConfidence'], 'ESTIMATED');
      expect(json['changeReason'], 'INITIAL_SETUP');
      expect(json['effectiveAt'], '2026-07-18T03:00:00Z');
    });

    test('pregnancy update includes date provenance and audit context', () {
      final json = const UpdateJourneyRequest(
        journeyType: JourneyType.pregnancy,
        estimatedDueDate: '2027-02-12',
        datingBasis: 'EDD',
      ).toJson();

      expect(json['datingBasis'], 'EDD');
      expect(json.containsKey('lastMenstrualDate'), isFalse);
      expect(json['dateSource'], 'SELF_REPORTED');
      expect(json['dateConfidence'], 'ESTIMATED');
      expect(json['changeReason'], 'DATE_CORRECTION');
      expect(DateTime.tryParse(json['effectiveAt'] as String), isNotNull);
    });

    test('transition history parses the canonical backend response', () {
      final transition = JourneyTransition.fromJson({
        'transitionId': 'transition-1',
        'eventType': 'STAGE_CHANGED',
        'fromStage': 'PRE_PREGNANCY',
        'toStage': 'PREGNANCY',
        'changedFields': ['journeyType', 'estimatedDueDate'],
        'source': 'CLINICIAN_CONFIRMED',
        'confidence': 'CONFIRMED',
        'reason': 'PREGNANCY_CONFIRMED',
        'effectiveAt': '2026-07-18T03:00:00Z',
        'recordedAt': '2026-07-18T03:00:01Z',
        'journeyVersion': 2,
      });

      expect(transition.eventType, 'STAGE_CHANGED');
      expect(transition.fromStage, 'PRE_PREGNANCY');
      expect(transition.toStage, 'PREGNANCY');
      expect(transition.changedFields, contains('estimatedDueDate'));
      expect(transition.journeyVersion, 2);
    });
  });

  group('Story 6.1 mobile behavior regressions', () {
    test('POSTPARTUM remains a canonical maternal lifecycle', () {
      const dashboard = JourneyDashboard(
        journeyId: 'postpartum-1',
        journeyType: 'POSTPARTUM',
        status: 'ACTIVE_POSTPARTUM',
      );

      expect(dashboard.isPostpartum, isTrue);
      expect(dashboard.isMaternalLifecycle, isTrue);
      expect(JourneyType.postpartum.isMaternalLifecycle, isTrue);
    });

    test('create response retains version and provenance metadata', () {
      final response = CreateJourneyResponse.fromJson({
        'id': 'journey-1',
        'journeyType': 'PREGNANCY',
        'status': 'ACTIVE',
        'startDate': '2026-07-18',
        'createdAt': '2026-07-18T03:00:00Z',
        'version': 3,
        'dateSource': 'CLINICIAN_CONFIRMED',
        'dateConfidence': 'CONFIRMED',
      });

      expect(response.version, 3);
      expect(response.dateSource, 'CLINICIAN_CONFIRMED');
      expect(response.dateConfidence, 'CONFIRMED');
    });

    test('nullable LMP must match symmetrically during reconciliation', () {
      const server = JourneyDashboard(
        journeyId: 'journey-1',
        journeyType: 'PREGNANCY',
        status: 'ACTIVE_PREGNANCY',
      );
      final stale = JourneyDashboard(
        journeyId: 'journey-1',
        journeyType: 'PREGNANCY',
        status: 'ACTIVE_PREGNANCY',
        lastMenstrualDate: DateTime(2026, 5, 1),
      );

      expect(JourneyDashboardReconciler.matches(server, stale), isFalse);
    });

    test('authoritative NO_JOURNEY only keeps a pending mutation', () {
      const server = JourneyDashboard(status: 'NO_JOURNEY');
      const cached = JourneyDashboard(
        journeyId: 'journey-1',
        journeyType: 'PREGNANCY',
        status: 'ACTIVE_PREGNANCY',
      );

      expect(
        JourneyDashboardReconciler.shouldUse(
          server,
          cached,
          pendingSync: false,
        ),
        isFalse,
      );
      expect(
        JourneyDashboardReconciler.shouldUse(server, cached, pendingSync: true),
        isTrue,
      );
    });

    test('authoritative POSTPARTUM is not shadowed by pregnancy cache', () {
      final server = JourneyDashboard(
        journeyId: 'journey-1',
        journeyType: 'POSTPARTUM',
        status: 'ACTIVE_POSTPARTUM',
      );
      final cached = JourneyDashboard(
        journeyId: 'journey-1',
        journeyType: 'PREGNANCY',
        status: 'ACTIVE_PREGNANCY',
        lastMenstrualDate: DateTime(2026, 1, 1),
      );

      expect(
        JourneyDashboardReconciler.shouldUse(
          server,
          cached,
          pendingSync: false,
        ),
        isFalse,
      );
    });

    test('unresolved or quarantined pregnancy never restores cached Plan', () {
      final cached = JourneyDashboard(
        journeyId: 'journey-1',
        journeyType: 'PREGNANCY',
        status: 'ACTIVE_PREGNANCY',
        lastMenstrualDate: DateTime(2026, 1, 1),
        estimatedDueDate: DateTime(2026, 10, 8),
        sourceWeekNumber: 21,
        plan: 2,
        datingBasis: 'LMP',
      );
      final unresolved = JourneyDashboard(
        journeyId: 'journey-1',
        journeyType: 'PREGNANCY',
        status: 'ACTIVE_PREGNANCY',
        lastMenstrualDate: DateTime(2026, 1, 1),
        estimatedDueDate: DateTime(2026, 10, 8),
      );
      final quarantined = JourneyDashboard(
        journeyId: 'journey-1',
        journeyType: 'PREGNANCY',
        status: 'ACTIVE_PREGNANCY',
        lastMenstrualDate: DateTime(2026, 1, 1),
        estimatedDueDate: DateTime(2026, 10, 8),
        datingQuarantineReason: 'DATING_DISCREPANCY',
      );

      expect(
        JourneyDashboardReconciler.shouldUse(
          unresolved,
          cached,
          pendingSync: true,
        ),
        isFalse,
      );
      expect(
        JourneyDashboardReconciler.shouldUse(
          quarantined,
          cached,
          pendingSync: true,
        ),
        isFalse,
      );
      expect(unresolved.displayPregnancyWeek, isNull);
      expect(quarantined.displayPregnancyWeek, isNull);
    });

    test('EDD-only update clears a cached LMP', () {
      expect(
        JourneyDashboardReconciler.updatedLastMenstrualDate(
          responseContainsField: true,
          responseValue: null,
          requestValue: null,
          requestEstimatedDueDate: DateTime(2026, 10, 1),
          fallbackValue: DateTime(2026, 1, 1),
        ),
        isNull,
      );
      expect(
        JourneyDashboardReconciler.updatedLastMenstrualDate(
          responseContainsField: false,
          responseValue: null,
          requestValue: null,
          requestEstimatedDueDate: DateTime(2026, 10, 1),
          fallbackValue: DateTime(2026, 1, 1),
        ),
        isNull,
      );
    });

    test('response from a previous account cannot be applied', () {
      expect(
        JourneyDashboardReconciler.canApplyResponse('account-a', 'account-b'),
        isFalse,
      );
      expect(
        JourneyDashboardReconciler.canApplyResponse('account-a', 'account-a'),
        isTrue,
      );
    });
  });

  testWidgets('POSTPARTUM state and history remain visible', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: MotherJourneyScreen(
            loadData: false,
            initialDashboard: JourneyDashboard(
              journeyId: 'postpartum-1',
              journeyType: 'POSTPARTUM',
              status: 'ACTIVE_POSTPARTUM',
              startDate: DateTime(2026, 7, 1),
            ),
            initialJourneyHistory: [_transition(toStage: 'POSTPARTUM')],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Hành trình hậu sản'), findsOneWidget);
    await tester.scrollUntilVisible(
      find.text('Lịch sử hành trình'),
      500,
      scrollable: find.byType(Scrollable).first,
    );
    expect(find.text('Lịch sử hành trình'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('history failure preserves prior history and exposes retry', (
    tester,
  ) async {
    const dashboard = JourneyDashboard(
      journeyId: 'journey-1',
      journeyType: 'PREGNANCY',
      status: 'ACTIVE_PREGNANCY',
    );
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: MotherJourneyScreen(
            initialDashboard: dashboard,
            initialJourneyHistory: [_transition(toStage: 'PREGNANCY')],
            journeyService: _HistoryFailureJourneyService(dashboard),
            loadSupportingData: false,
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.byKey(const Key('journey-history-retry')),
      500,
      scrollable: find.byType(Scrollable).first,
    );

    expect(find.byKey(const Key('journey-history-retry')), findsOneWidget);
    expect(find.text('Lịch sử hành trình'), findsOneWidget);
  });

  testWidgets(
    'PRE_PREGNANCY is visible and opens the canonical update transition',
    (tester) async {
      final router = GoRouter(
        initialLocation: '/journey',
        routes: [
          GoRoute(
            path: '/journey',
            builder: (_, _) => Scaffold(
              body: MotherJourneyScreen(
                loadData: false,
                initialDashboard: JourneyDashboard(
                  journeyId: 'journey-pre-1',
                  journeyType: 'PRE_PREGNANCY',
                  status: 'PRE_PREGNANCY',
                  startDate: DateTime(2026, 7, 18),
                ),
                initialJourneyHistory: [_transition()],
              ),
            ),
          ),
          GoRoute(
            path: '/journey-setup',
            builder: (_, state) => Text(
              [
                state.uri.queryParameters['mode'],
                state.uri.queryParameters['journeyId'],
                state.uri.queryParameters['transition'],
              ].join(':'),
              key: const Key('transition-route-probe'),
            ),
          ),
        ],
      );
      addTearDown(router.dispose);

      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      expect(
        find.byKey(const Key('pre-pregnancy-transition-action')),
        findsOneWidget,
      );
      await tester.tap(
        find.byKey(const Key('pre-pregnancy-transition-action')),
      );
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('transition-route-probe')), findsOneWidget);
      expect(find.text('edit:journey-pre-1:pre-pregnancy'), findsOneWidget);
    },
  );

  testWidgets(
    'POSTPARTUM opens the canonical update transition for a new pregnancy',
    (tester) async {
      final router = GoRouter(
        initialLocation: '/journey',
        routes: [
          GoRoute(
            path: '/journey',
            builder: (_, _) => Scaffold(
              body: MotherJourneyScreen(
                loadData: false,
                initialDashboard: JourneyDashboard(
                  journeyId: 'journey-postpartum-1',
                  journeyType: 'POSTPARTUM',
                  status: 'ACTIVE_POSTPARTUM',
                  startDate: DateTime(2026, 7, 18),
                ),
                initialJourneyHistory: [_transition(toStage: 'POSTPARTUM')],
              ),
            ),
          ),
          GoRoute(
            path: '/journey-setup',
            builder: (_, state) => Text(
              [
                state.uri.queryParameters['mode'],
                state.uri.queryParameters['journeyId'],
                state.uri.queryParameters['transition'],
              ].join(':'),
              key: const Key('transition-route-probe'),
            ),
          ),
        ],
      );
      addTearDown(router.dispose);

      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('postpartum-transition-action')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('transition-route-probe')), findsOneWidget);
      expect(find.text('edit:journey-postpartum-1:postpartum'), findsOneWidget);
    },
  );

  testWidgets('setup remains readable at 150% and labels the back button', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(1080, 1920);
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(
        builder: (context, child) => MediaQuery(
          data: MediaQuery.of(
            context,
          ).copyWith(textScaler: const TextScaler.linear(1.5)),
          child: child!,
        ),
        home: const JourneySetupScreen(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byTooltip('Quay lại'), findsOneWidget);
    final method = find.byKey(const Key('dating-method-due-date'));
    await tester.ensureVisible(method);
    await tester.tap(method);
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Tiếp theo'));
    await tester.pumpAndSettle();
    final picker = find.byKey(const Key('journey-date-picker'));
    expect(picker, findsOneWidget);
    final today = find.descendant(
      of: picker,
      matching: find.text(DateTime.now().day.toString()),
    );
    expect(today.hitTestable(), findsOneWidget);
    await tester.tap(today.hitTestable());
    await tester.pump();
    await tester.tap(find.widgetWithText(FilledButton, 'Tiếp theo'));
    await tester.pumpAndSettle();

    final resultTitle = find.text('Kết quả');
    expect(resultTitle, findsOneWidget);
    expect(tester.getTopLeft(resultTitle).dy, greaterThan(70));
    expect(tester.takeException(), isNull);
  });
}
