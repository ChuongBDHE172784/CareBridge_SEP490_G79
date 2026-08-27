import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:integration_test/integration_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/baby/models/baby_model.dart';
import 'package:untitled/features/baby/screens/baby_log_summary_screen.dart';
import 'package:untitled/features/baby/screens/record_milestone_screen.dart';
import 'package:untitled/features/healthRecords/screens/add_vaccination_record_screen.dart';
import 'package:untitled/features/healthRecords/screens/growth_measurement_history_screen.dart';
import 'package:untitled/features/journey/screens/mother_journey_screen.dart';

GoRouter _canonicalRouter(Widget journey, {bool useRealDestinations = false}) =>
    GoRouter(
      initialLocation: '/journey',
      routes: [
        GoRoute(
          path: '/journey',
          builder: (_, _) => Scaffold(body: journey),
        ),
        for (final route in const [
          ('/babies/:babyId/log-summary', 'journal'),
          ('/babies/:babyId/growth', 'growth'),
          ('/babies/:babyId/milestones/add', 'milestone'),
          ('/babies/:babyId/vaccinations/add', 'vaccination'),
        ])
          GoRoute(
            path: route.$1,
            builder: (_, state) {
              final babyId = state.pathParameters['babyId'] ?? '';
              if (useRealDestinations) {
                return switch (route.$2) {
                  'journal' => BabyLogSummaryScreen(babyId: babyId),
                  'growth' => GrowthMeasurementHistoryScreen(babyId: babyId),
                  'milestone' => RecordMilestoneScreen(babyId: babyId),
                  _ => AddVaccinationRecordScreen(babyId: babyId),
                };
              }
              return Scaffold(
                body: Text(
                  '${route.$2}:$babyId',
                  key: const Key('route-probe'),
                ),
              );
            },
          ),
      ],
    );

Future<void> _waitForKey(WidgetTester tester, Key key) async {
  for (var attempt = 0; attempt < 30; attempt++) {
    await tester.pump(const Duration(seconds: 1));
    if (find.byKey(key).evaluate().isNotEmpty) return;
  }
  fail('Timed out waiting for $key');
}

Future<void> _waitUntilGone(WidgetTester tester, Key key) async {
  for (var attempt = 0; attempt < 30; attempt++) {
    await tester.pump(const Duration(seconds: 1));
    if (find.byKey(key).evaluate().isEmpty) return;
  }
  fail('Timed out waiting for $key to disappear');
}

String _activeBabyId(WidgetTester tester) {
  final finder = find.byWidgetPredicate((widget) {
    final key = widget.key;
    return key is ValueKey<String> && key.value.startsWith('active-baby-id-');
  });
  expect(finder, findsOneWidget);
  final key = tester.widget(finder).key! as ValueKey<String>;
  return key.value.substring('active-baby-id-'.length);
}

Future<String> _openProbeRoute(
  WidgetTester tester,
  GoRouter router,
  Finder action,
  String domain,
) async {
  await tester.ensureVisible(action);
  await tester.pumpAndSettle();
  await tester.tap(action);
  await tester.pumpAndSettle();
  final probe = tester.widget<Text>(find.byKey(const Key('route-probe')));
  expect(probe.data, startsWith('$domain:'));
  final babyId = probe.data!.substring(domain.length + 1);
  expect(babyId, isNotEmpty);
  router.pop();
  await tester.pumpAndSettle();
  return babyId;
}

Future<List<String>> _exerciseProbeActions(
  WidgetTester tester,
  GoRouter router,
) async {
  await tester.drag(find.byType(CustomScrollView), const Offset(0, -350));
  await tester.pumpAndSettle();
  final ids = <String>[];
  ids.add(
    await _openProbeRoute(
      tester,
      router,
      find.byKey(const Key('baby-care-journal')),
      'journal',
    ),
  );
  ids.add(
    await _openProbeRoute(
      tester,
      router,
      find.byKey(const Key('baby-care-growth-history')),
      'growth',
    ),
  );
  await tester.tap(find.text('Cột mốc'));
  await tester.pumpAndSettle();
  ids.add(
    await _openProbeRoute(
      tester,
      router,
      find.byKey(const Key('baby-care-milestone-add')).last,
      'milestone',
    ),
  );
  await tester.tap(find.text('Tiêm chủng'));
  await tester.pumpAndSettle();
  ids.add(
    await _openProbeRoute(
      tester,
      router,
      find.byKey(const Key('baby-care-vaccination-add')),
      'vaccination',
    ),
  );
  return ids;
}

Future<void> _exerciseRealActions(
  WidgetTester tester,
  GoRouter router,
  String expectedBabyId,
) async {
  await _waitForKey(tester, const Key('baby-summary-real-data'));
  final feedingValue = find.descendant(
    of: find.byKey(const Key('baby-summary-feeding')),
    matching: find.byType(Text),
  );
  final feedingCountBefore = int.parse(
    tester.widget<Text>(feedingValue.first).data!,
  );

  await tester.drag(find.byType(CustomScrollView), const Offset(0, -350));
  await tester.pumpAndSettle();

  await tester.ensureVisible(find.byKey(const Key('baby-care-journal')));
  await tester.tap(find.byKey(const Key('baby-care-journal')));
  await _waitForKey(tester, const Key('baby-log-add'));
  expect(
    tester
        .widget<BabyLogSummaryScreen>(find.byType(BabyLogSummaryScreen))
        .babyId,
    expectedBabyId,
  );
  await tester.tap(find.byKey(const Key('baby-log-add')));
  await tester.pumpAndSettle();
  await tester.tap(find.byKey(const Key('baby-log-save')));
  await _waitUntilGone(tester, const Key('baby-log-save'));
  router.pop();
  await _waitForKey(tester, const Key('active-baby-name'));
  await _waitForKey(tester, const Key('baby-summary-real-data'));
  expect(_activeBabyId(tester), expectedBabyId);
  final feedingCountAfter = int.parse(
    tester.widget<Text>(feedingValue.first).data!,
  );
  expect(feedingCountAfter, greaterThanOrEqualTo(feedingCountBefore + 1));

  await tester.drag(find.byType(CustomScrollView), const Offset(0, -350));
  await tester.pumpAndSettle();
  await tester.ensureVisible(find.byKey(const Key('baby-care-growth-history')));
  await tester.tap(find.byKey(const Key('baby-care-growth-history')));
  await _waitForKey(tester, const Key('growth-history-screen'));
  expect(
    tester
        .widget<GrowthMeasurementHistoryScreen>(
          find.byType(GrowthMeasurementHistoryScreen),
        )
        .babyId,
    expectedBabyId,
  );
  router.pop();
  await _waitForKey(tester, const Key('active-baby-name'));

  await tester.drag(find.byType(CustomScrollView), const Offset(0, -350));
  await tester.pumpAndSettle();
  await tester.tap(find.text('Cột mốc'));
  await tester.pumpAndSettle();
  await tester.ensureVisible(
    find.byKey(const Key('baby-care-milestone-add')).last,
  );
  await tester.tap(find.byKey(const Key('baby-care-milestone-add')).last);
  await tester.pumpAndSettle();
  expect(
    tester
        .widget<RecordMilestoneScreen>(find.byType(RecordMilestoneScreen))
        .babyId,
    expectedBabyId,
  );
  router.pop();
  await _waitForKey(tester, const Key('active-baby-name'));

  await tester.drag(find.byType(CustomScrollView), const Offset(0, -350));
  await tester.pumpAndSettle();
  await tester.tap(find.text('Tiêm chủng'));
  await tester.pumpAndSettle();
  await tester.ensureVisible(
    find.byKey(const Key('baby-care-vaccination-add')),
  );
  await tester.tap(find.byKey(const Key('baby-care-vaccination-add')));
  await tester.pumpAndSettle();
  expect(
    tester
        .widget<AddVaccinationRecordScreen>(
          find.byType(AddVaccinationRecordScreen),
        )
        .babyId,
    expectedBabyId,
  );
  router.pop();
  await _waitForKey(tester, const Key('active-baby-name'));
  expect(_activeBabyId(tester), expectedBabyId);
}

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('MF-03 canonical journey routes four scoped interactions', (
    tester,
  ) async {
    final fixtureBaby = BabyProfile(
      id: 'mf03-fixture-baby',
      nickname: 'Mỡ',
      birthDate: DateTime.now().subtract(const Duration(days: 2)),
      gender: BabyGender.unknown,
      birthWeightKg: 3.5,
      birthLengthCm: 49,
      isActive: true,
    );
    final router = _canonicalRouter(
      MotherJourneyScreen(loadData: false, initialBabyProfiles: [fixtureBaby]),
    );
    addTearDown(router.dispose);
    runApp(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();

    final ids = await _exerciseProbeActions(tester, router);
    expect(ids, everyElement('mf03-fixture-baby'));
    expect(find.byKey(const Key('active-baby-name')), findsOneWidget);
  });

  final apiBacked =
      const String.fromEnvironment('MF03_API_E2E', defaultValue: 'false') ==
      'true';
  const accessToken = String.fromEnvironment('MF03_ACCESS_TOKEN');

  testWidgets(
    'MF-03 API-backed canonical journey uses one real baby across interactions',
    (tester) async {
      expect(
        accessToken,
        isNotEmpty,
        reason: 'MF03_ACCESS_TOKEN is required in API-backed mode.',
      );
      await AuthState.instance.setTokens(
        accessToken: accessToken,
        refreshToken: const String.fromEnvironment('MF03_REFRESH_TOKEN'),
        userId: const String.fromEnvironment('MF03_USER_ID'),
        role: 'MOTHER',
      );
      final router = _canonicalRouter(
        const MotherJourneyScreen(),
        useRealDestinations: true,
      );
      addTearDown(router.dispose);
      runApp(MaterialApp.router(routerConfig: router));
      await _waitForKey(tester, const Key('active-baby-name'));

      final activeBaby = tester.widget<Text>(
        find.byKey(const Key('active-baby-name')),
      );
      expect(activeBaby.data, isNotNull);
      expect(activeBaby.data, isNot(anyOf('—', 'Baby A', 'Baby B')));
      final activeBabyId = _activeBabyId(tester);
      expect(activeBabyId, isNot('mf03-fixture-baby'));

      await _exerciseRealActions(tester, router, activeBabyId);
      expect(find.byKey(const Key('active-baby-name')), findsOneWidget);
    },
    skip: !apiBacked,
  );
}
