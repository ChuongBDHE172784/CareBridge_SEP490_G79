import 'dart:async';
import 'dart:collection';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/aiTriage/models/triage_entry_context.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/journey/screens/mother_journey_screen.dart';
import 'package:untitled/features/journey/services/journey_service.dart';

class _QueuedJourneyService extends JourneyService {
  _QueuedJourneyService(Iterable<Future<JourneyDashboard>> dashboards)
    : _dashboards = Queue<Future<JourneyDashboard>>.from(dashboards);

  final Queue<Future<JourneyDashboard>> _dashboards;

  @override
  Future<JourneyDashboard> getDashboard() {
    if (_dashboards.isEmpty) {
      return Future<JourneyDashboard>.error(
        StateError('No queued dashboard response'),
      );
    }
    return _dashboards.removeFirst();
  }

  @override
  Future<JourneyTimelinePage> getTimeline(
    String journeyId, {
    int page = 0,
    int size = 20,
  }) async => JourneyTimelinePage(
    items: const [],
    page: page,
    size: size,
    totalElements: 0,
    totalPages: 1,
  );
}

JourneyDashboard _pregnancyDashboard(int week) => JourneyDashboard(
  journeyId: '10000000-0000-0000-0000-000000000001',
  journeyType: 'PREGNANCY',
  status: 'ACTIVE_PREGNANCY',
  pregnancyWeek: week,
  version: 1,
);

Future<void> _pumpJourneyScreen(WidgetTester tester, JourneyService service) =>
    tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: MotherJourneyScreen(
            journeyService: service,
            loadSupportingData: false,
          ),
        ),
      ),
    );

void main() {
  test('Add Baby opens only when the outcome newly enters live birth', () {
    const liveBirth = PregnancyOutcomeResult(
      evidenceId: 'evidence-1',
      journeyId: 'journey-1',
      outcomeType: PregnancyOutcome.liveBirth,
      journeyType: 'POSTPARTUM',
      journeyVersion: 2,
      revisionNumber: 1,
    );
    const loss = PregnancyOutcomeResult(
      evidenceId: 'evidence-2',
      journeyId: 'journey-1',
      outcomeType: PregnancyOutcome.pregnancyLoss,
      journeyType: 'POSTPARTUM',
      journeyVersion: 2,
      revisionNumber: 1,
    );

    expect(
      shouldOpenLiveBirthAddBaby(previousOutcome: null, result: liveBirth),
      isTrue,
    );
    expect(
      shouldOpenLiveBirthAddBaby(
        previousOutcome: PregnancyOutcome.liveBirth,
        result: liveBirth,
      ),
      isFalse,
    );
    expect(
      shouldOpenLiveBirthAddBaby(previousOutcome: null, result: loss),
      isFalse,
    );
  });

  for (final fixture in const [
    (
      name: 'pre-pregnancy',
      dashboard: JourneyDashboard(
        journeyId: '10000000-0000-0000-0000-000000000001',
        journeyType: 'PRE_PREGNANCY',
        status: 'PRE_PREGNANCY',
      ),
    ),
    (
      name: 'pregnancy',
      dashboard: JourneyDashboard(
        journeyId: '10000000-0000-0000-0000-000000000002',
        journeyType: 'PREGNANCY',
        status: 'ACTIVE_PREGNANCY',
        pregnancyWeek: 24,
        version: 1,
      ),
    ),
    (
      name: 'postpartum',
      dashboard: JourneyDashboard(
        journeyId: '10000000-0000-0000-0000-000000000003',
        journeyType: 'POSTPARTUM',
        status: 'ACTIVE_POSTPARTUM',
        version: 1,
      ),
    ),
  ]) {
    testWidgets('${fixture.name} exposes the shared maternal health block', (
      tester,
    ) async {
      await tester.binding.setSurfaceSize(const Size(900, 1800));
      addTearDown(() => tester.binding.setSurfaceSize(null));
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: MotherJourneyScreen(
              loadData: false,
              loadSupportingData: false,
              initialDashboard: fixture.dashboard,
            ),
          ),
        ),
      );
      await tester.pump();

      expect(find.text('Mẹ'), findsOneWidget);
      expect(find.text('Bé'), findsOneWidget);
      expect(find.text('Hành trình của Mẹ'), findsNothing);

      expect(find.text('Lịch hẹn tiếp theo'), findsNothing);
      expect(find.text('Thông tin tiêm phòng'), findsNothing);

      for (final label in const ['Chỉ số sức khỏe', 'Cân nặng', 'Nhịp tim']) {
        await tester.scrollUntilVisible(
          find.text(label),
          250,
          scrollable: find.byType(Scrollable).first,
        );
        expect(find.text(label), findsOneWidget);
      }

      if (fixture.name == 'postpartum') {
        expect(find.byKey(const Key('postpartum-baby-linkage')), findsNothing);
        expect(find.text('Hồ sơ bé'), findsNothing);
      }
    });
  }

  testWidgets('maternal metric triage keeps the typed postpartum origin', (
    tester,
  ) async {
    Object? capturedExtra;
    const dashboard = JourneyDashboard(
      journeyId: '10000000-0000-0000-0000-000000000003',
      journeyType: 'POSTPARTUM',
      status: 'ACTIVE_POSTPARTUM',
      version: 1,
    );
    final router = GoRouter(
      initialLocation: '/mother',
      routes: [
        GoRoute(
          path: '/mother',
          builder: (_, _) => const Scaffold(
            body: MotherJourneyScreen(
              loadData: false,
              loadSupportingData: false,
              initialDashboard: dashboard,
            ),
          ),
        ),
        GoRoute(
          path: '/triage/intake',
          builder: (_, state) {
            capturedExtra = state.extra;
            return const Scaffold(body: Text('triage'));
          },
        ),
      ],
    );
    addTearDown(router.dispose);
    await tester.binding.setSurfaceSize(const Size(900, 1800));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();

    await tester.scrollUntilVisible(
      find.text('Kiểm tra triệu chứng'),
      250,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.tap(find.text('Kiểm tra triệu chứng'));
    await tester.pumpAndSettle();

    expect(find.text('triage'), findsOneWidget);
    expect(capturedExtra, isA<TriageEntryContext>());
    final entry = capturedExtra! as TriageEntryContext;
    expect(entry.stage, TriageStageIntent.postpartum);
    expect(entry.origin, TriageOriginIntent.motherJourney);
    expect(entry.journeyId, dashboard.journeyId);
    expect(entry.originReferenceId, dashboard.journeyId);
    expect(entry.lockStage, isTrue);
  });

  testWidgets('shows loading feedback only while the first journey loads', (
    tester,
  ) async {
    final initialDashboard = Completer<JourneyDashboard>();
    await _pumpJourneyScreen(
      tester,
      _QueuedJourneyService([initialDashboard.future]),
    );
    await tester.pump();

    expect(
      find.byKey(const Key('mother-journey-initial-loading')),
      findsOneWidget,
    );

    initialDashboard.complete(_pregnancyDashboard(20));
    await tester.pumpAndSettle();

    expect(find.text('Tuần 20'), findsOneWidget);
    expect(
      find.byKey(const Key('mother-journey-initial-loading')),
      findsNothing,
    );
  });

  testWidgets('keeps the journey visible when a foreground refresh fails', (
    tester,
  ) async {
    final foregroundRefresh = Completer<JourneyDashboard>();
    await _pumpJourneyScreen(
      tester,
      _QueuedJourneyService([
        Future<JourneyDashboard>.value(_pregnancyDashboard(20)),
        foregroundRefresh.future,
      ]),
    );
    await tester.pumpAndSettle();
    expect(find.text('Tuần 20'), findsOneWidget);

    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();

    expect(find.text('Tuần 20'), findsOneWidget);
    expect(
      find.byKey(const Key('mother-journey-initial-loading')),
      findsNothing,
    );

    foregroundRefresh.completeError(StateError('offline'));
    await tester.pump();

    expect(find.text('Tuần 20'), findsOneWidget);
    expect(find.text('Lỗi kết nối. Vui lòng kéo để thử lại.'), findsNothing);
  });

  testWidgets('discards stale foreground refresh results', (tester) async {
    final staleRefresh = Completer<JourneyDashboard>();
    final latestRefresh = Completer<JourneyDashboard>();
    await _pumpJourneyScreen(
      tester,
      _QueuedJourneyService([
        Future<JourneyDashboard>.value(_pregnancyDashboard(20)),
        staleRefresh.future,
        latestRefresh.future,
      ]),
    );
    await tester.pumpAndSettle();

    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();

    latestRefresh.complete(_pregnancyDashboard(30));
    await tester.pumpAndSettle();
    expect(find.text('Tuần 30'), findsOneWidget);

    staleRefresh.complete(_pregnancyDashboard(24));
    await tester.pumpAndSettle();
    expect(find.text('Tuần 30'), findsOneWidget);
    expect(find.text('Tuần 24'), findsNothing);
  });
}
