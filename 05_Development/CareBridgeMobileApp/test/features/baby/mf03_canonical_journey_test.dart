import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/baby/models/baby_model.dart';
import 'package:untitled/features/baby/models/baby_daily_log_model.dart';
import 'package:untitled/features/baby/models/milestone_model.dart';
import 'package:untitled/features/baby/screens/baby_profile_detail_screen.dart';
import 'package:untitled/features/baby/screens/development_milestone_detail_screen.dart';
import 'package:untitled/features/healthRecords/models/vaccination_model.dart';
import 'package:untitled/features/healthRecords/models/growth_measurement_model.dart';
import 'package:untitled/features/healthRecords/screens/vaccination_detail_screen.dart';
import 'package:untitled/features/journey/screens/mother_journey_screen.dart';

GoRouter _fixtureRouter(List<BabyProfile> babies) => GoRouter(
  initialLocation: '/journey',
  routes: [
    GoRoute(
      path: '/journey',
      builder: (_, _) => Scaffold(
        body: MotherJourneyScreen(loadData: false, initialBabyProfiles: babies),
      ),
    ),
    GoRoute(
      path: '/baby-care-hub',
      builder: (_, state) => Scaffold(
        body: Text(
          'hub:${state.uri.queryParameters['babyId']}',
          key: const Key('route-probe'),
        ),
      ),
    ),
    for (final route in const [
      ('/babies/:babyId/log-summary', 'journal'),
      ('/babies/:babyId/growth', 'growth'),
      ('/babies/:babyId/milestones/add', 'milestone'),
      ('/babies/:babyId/vaccinations/add', 'vaccination'),
    ])
      GoRoute(
        path: route.$1,
        builder: (_, state) => Scaffold(
          body: Text(
            '${route.$2}:${state.pathParameters['babyId']}',
            key: const Key('route-probe'),
          ),
        ),
      ),
  ],
);

Future<void> _expectRoute(
  WidgetTester tester,
  GoRouter router,
  Finder action,
  String expected,
) async {
  await tester.ensureVisible(action);
  await tester.pumpAndSettle();
  await tester.tap(action);
  await tester.pumpAndSettle();
  expect(find.byKey(const Key('route-probe')), findsOneWidget);
  expect(find.text(expected), findsOneWidget);
  router.pop();
  await tester.pumpAndSettle();
}

void main() {
  BabyProfile baby(String id) => BabyProfile(
    id: id,
    nickname: 'Bé $id',
    birthDate: DateTime(2026, 7, 1),
    gender: BabyGender.unknown,
    isActive: true,
  );

  BabyLogSummaryResponse summary(
    String babyId, {
    required int feeding,
    required int diaper,
    double? sleepQuantity,
    String? sleepUnit,
    int sleepCount = 1,
  }) => BabyLogSummaryResponse(
    babyId: babyId,
    period: '24h',
    summaries: {
      'FEEDING': LogTypeSummary(count: feeding),
      'SLEEP': LogTypeSummary(
        count: sleepCount,
        totalQuantity: sleepQuantity,
        unit: sleepUnit,
      ),
      'DIAPER': LogTypeSummary(count: diaper),
    },
  );

  testWidgets('canonical summary and growth render supplied database values', (
    tester,
  ) async {
    final profile = baby('real-data');
    final measurements = [
      GrowthMeasurement(
        id: 'm2',
        measuredAt: DateTime(2026, 7, 15),
        weightKg: 4.1,
        recordedBy: 'mother',
      ),
      GrowthMeasurement(
        id: 'm1',
        measuredAt: DateTime(2026, 7, 1),
        weightKg: 3.6,
        recordedBy: 'mother',
      ),
    ];

    await tester.pumpWidget(
      MaterialApp(
        home: SingleChildScrollView(
          child: BabyProfileDetailScreen(
            babyId: profile.id,
            embedded: true,
            loadData: false,
            initialProfile: profile,
            initialSummary: BabyLogSummaryResponse.fromJson({
              'babyId': profile.id,
              'period': '24h',
              'summaries': {
                'FEEDING': {'count': 8},
                'SLEEP': {'count': 2, 'totalQuantity': 90, 'unit': 'minutes'},
                'DIAPER': {'count': 5},
              },
            }),
            initialGrowthMeasurements: measurements,
          ),
        ),
      ),
    );
    await tester.pump();

    expect(find.byKey(const Key('baby-summary-real-data')), findsOneWidget);
    expect(find.text('8'), findsOneWidget);
    expect(find.text('1.5h'), findsOneWidget);
    expect(find.text('5'), findsOneWidget);
    expect(find.byKey(const ValueKey('growth-chart-points-2')), findsOneWidget);
    expect(find.text('3.6 kg – 4.1 kg'), findsOneWidget);
    expect(find.text('13h'), findsNothing);
  });

  testWidgets('empty fixture never substitutes production-looking values', (
    tester,
  ) async {
    final profile = baby('empty');
    await tester.pumpWidget(
      MaterialApp(
        home: SingleChildScrollView(
          child: BabyProfileDetailScreen(
            babyId: profile.id,
            embedded: true,
            loadData: false,
            initialProfile: profile,
            initialSummary: summary(
              profile.id,
              feeding: 0,
              diaper: 0,
              sleepCount: 0,
            ),
          ),
        ),
      ),
    );
    await tester.pump();

    expect(find.text('0h'), findsOneWidget);
    expect(find.byKey(const Key('baby-growth-empty')), findsOneWidget);
    expect(find.text('13h'), findsNothing);
  });

  testWidgets('API failures render retryable states without fake zeros', (
    tester,
  ) async {
    final profile = baby('error');
    await tester.pumpWidget(
      MaterialApp(
        home: SingleChildScrollView(
          child: BabyProfileDetailScreen(
            babyId: profile.id,
            embedded: true,
            initialProfile: profile,
            profileLoader: (_) async => profile,
            summaryLoader: (_) async => throw Exception('summary failed'),
            growthLoader: (_) async => throw Exception('growth failed'),
            loadCareCollectionsData: false,
          ),
        ),
      ),
    );
    await tester.pump();
    await tester.pump();

    expect(find.byKey(const Key('baby-summary-error')), findsOneWidget);
    expect(find.byKey(const Key('baby-growth-error')), findsOneWidget);
    expect(find.text('13h'), findsNothing);
  });

  testWidgets('late response from previous baby is ignored', (tester) async {
    final first = baby('first');
    final second = baby('second');
    final firstCompleter = Completer<BabyLogSummaryResponse>();
    late StateSetter updateHarness;
    var activeBaby = first;

    Widget buildHarness() => MaterialApp(
      home: StatefulBuilder(
        builder: (context, setState) {
          updateHarness = setState;
          return SingleChildScrollView(
            child: BabyProfileDetailScreen(
              babyId: activeBaby.id,
              embedded: true,
              initialProfile: activeBaby,
              profileLoader: (id) async => id == first.id ? first : second,
              summaryLoader: (id) => id == first.id
                  ? firstCompleter.future
                  : Future.value(
                      summary(id, feeding: 9, diaper: 7, sleepCount: 0),
                    ),
              growthLoader: (_) async => const [],
              loadCareCollectionsData: false,
            ),
          );
        },
      ),
    );

    await tester.pumpWidget(buildHarness());
    await tester.pump();
    updateHarness(() => activeBaby = second);
    await tester.pump();
    await tester.pump();
    expect(find.text('9'), findsOneWidget);

    firstCompleter.complete(
      summary(first.id, feeding: 2, diaper: 1, sleepCount: 0),
    );
    await tester.pump();
    expect(find.text('9'), findsOneWidget);
    expect(find.text('2'), findsNothing);
  });

  testWidgets('canonical Baby Journey switches baby and scopes all routes', (
    tester,
  ) async {
    final firstBaby = BabyProfile(
      id: 'mf03-first-baby',
      nickname: 'Mỡ',
      birthDate: DateTime.now().subtract(const Duration(days: 2)),
      gender: BabyGender.unknown,
      birthWeightKg: 3.5,
      birthLengthCm: 49,
      isActive: true,
    );
    final secondBaby = BabyProfile(
      id: 'mf03-second-baby',
      nickname: 'Bông',
      birthDate: DateTime.now().subtract(const Duration(days: 40)),
      gender: BabyGender.unknown,
      isActive: false,
    );
    final router = _fixtureRouter([firstBaby, secondBaby]);
    addTearDown(router.dispose);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();
    expect(find.text('Mỡ'), findsWidgets);

    await tester.tap(find.byKey(const Key('baby-switcher')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Bông').last);
    await tester.pumpAndSettle();
    expect(
      find.byKey(const ValueKey('active-baby-id-mf03-second-baby')),
      findsOneWidget,
    );

    await tester.drag(find.byType(CustomScrollView), const Offset(0, -350));
    await tester.pumpAndSettle();
    await _expectRoute(
      tester,
      router,
      find.byKey(const Key('baby-care-journal')),
      'journal:mf03-second-baby',
    );
    await _expectRoute(
      tester,
      router,
      find.byKey(const Key('baby-care-growth-history')),
      'growth:mf03-second-baby',
    );
    await _expectRoute(
      tester,
      router,
      find.byKey(const Key('baby-care-hub')),
      'hub:mf03-second-baby',
    );

    await tester.tap(find.text('Cột mốc'));
    await tester.pumpAndSettle();
    await _expectRoute(
      tester,
      router,
      find.byKey(const Key('baby-care-milestone-add')).last,
      'milestone:mf03-second-baby',
    );

    await tester.tap(find.text('Tiêm chủng'));
    await tester.pumpAndSettle();
    await _expectRoute(
      tester,
      router,
      find.byKey(const Key('baby-care-vaccination-add')),
      'vaccination:mf03-second-baby',
    );
    expect(
      find.byKey(const ValueKey('active-baby-id-mf03-second-baby')),
      findsOneWidget,
    );
  });

  testWidgets(
    'canonical profile opens scoped milestone and vaccination detail',
    (tester) async {
      final baby = BabyProfile(
        id: 'detail-baby',
        nickname: 'Bông',
        birthDate: DateTime(2026, 6, 1),
        gender: BabyGender.unknown,
        isActive: true,
      );
      final milestone = Milestone(
        id: 'milestone-1',
        babyId: baby.id,
        milestoneType: MilestoneType.roll,
        achievedDate: DateTime(2026, 7, 1),
      );
      final vaccination = VaccinationRecord(
        vaccinationId: 'vaccination-1',
        vaccineName: 'Vaccine A',
        status: VaccinationStatus.completed,
        plannedDate: DateTime(2026, 7, 2),
        childId: baby.id,
      );
      final router = GoRouter(
        initialLocation: '/profile',
        routes: [
          GoRoute(
            path: '/profile',
            builder: (_, _) => Scaffold(
              body: SingleChildScrollView(
                child: BabyProfileDetailScreen(
                  babyId: baby.id,
                  embedded: true,
                  loadData: false,
                  initialProfile: baby,
                  initialMilestones: [milestone],
                  initialVaccinations: [vaccination],
                ),
              ),
            ),
          ),
          GoRoute(
            path: '/babies/:babyId/milestones/:milestoneId',
            builder: (_, state) => Scaffold(
              body: Text(
                'milestone:${state.pathParameters['babyId']}:'
                '${state.pathParameters['milestoneId']}',
                key: const Key('detail-probe'),
              ),
            ),
          ),
          GoRoute(
            path: '/babies/:babyId/vaccinations/:id',
            builder: (_, state) => Scaffold(
              body: Text(
                'vaccination:${state.pathParameters['babyId']}:'
                '${state.pathParameters['id']}',
                key: const Key('detail-probe'),
              ),
            ),
          ),
        ],
      );
      addTearDown(router.dispose);
      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.text('Cột mốc'));
      await tester.tap(find.text('Cột mốc'));
      await tester.pumpAndSettle();
      await tester.ensureVisible(
        find.byKey(const ValueKey('milestone-milestone-1')),
      );
      await tester.tap(find.byKey(const ValueKey('milestone-milestone-1')));
      await tester.pumpAndSettle();
      expect(find.text('milestone:detail-baby:milestone-1'), findsOneWidget);
      router.pop();
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.text('Tiêm chủng'));
      await tester.tap(find.text('Tiêm chủng'));
      await tester.pumpAndSettle();
      await tester.ensureVisible(
        find.byKey(const ValueKey('vaccination-vaccination-1')),
      );
      await tester.tap(find.byKey(const ValueKey('vaccination-vaccination-1')));
      await tester.pumpAndSettle();
      expect(
        find.text('vaccination:detail-baby:vaccination-1'),
        findsOneWidget,
      );
    },
  );

  testWidgets('detail screens never substitute another baby mock', (
    tester,
  ) async {
    final milestone = Milestone(
      id: 'milestone-real',
      babyId: 'baby-real',
      milestoneType: MilestoneType.roll,
      achievedDate: DateTime(2026, 7, 1),
      note: 'Ghi chú thật',
    );
    await tester.pumpWidget(
      MaterialApp(
        home: DevelopmentMilestoneDetailScreen(
          babyId: milestone.babyId,
          milestoneId: milestone.id,
          initialMilestone: milestone,
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text(milestone.milestoneType.displayLabel), findsOneWidget);
    expect(find.text('Ghi chú thật'), findsWidgets);
    expect(find.text('Lần đầu tiên ngồi vững'), findsNothing);

    final vaccination = VaccinationRecord(
      vaccinationId: 'vaccination-real',
      vaccineName: 'Vaccine thật',
      status: VaccinationStatus.completed,
      plannedDate: DateTime(2026, 7, 2),
      childId: 'baby-real',
      childName: 'Bông',
    );
    await tester.pumpWidget(
      MaterialApp(
        home: VaccinationDetailScreen(
          babyId: 'baby-real',
          vaccinationId: vaccination.vaccinationId,
          initialRecord: vaccination,
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Vaccine thật'), findsWidgets);
    expect(find.text('Bông'), findsWidgets);
    expect(find.text('Nguyễn Văn A'), findsNothing);
  });
}
