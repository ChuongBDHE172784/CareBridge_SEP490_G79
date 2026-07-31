import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/home/screens/family_member_home_screen.dart';
import 'package:untitled/features/home/screens/mother_home_screen.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/reminder/screens/today_tasks_screen.dart';

import '../reminder/support/today_task_red_fixture.dart';

JourneyDashboard _dashboard() => const JourneyDashboard(
  journeyId: 'red-journey',
  journeyType: 'PREGNANCY',
  status: 'ACTIVE_PREGNANCY',
  pregnancyWeek: 20,
);

void main() {
  testWidgets('Today screen pull-to-refresh reloads the shared panel', (
    tester,
  ) async {
    final backend = StatefulTodayBackend();
    tester.view.physicalSize = const Size(430, 932);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(home: TodayTasksScreen(service: backend.service)),
    );
    await tester.pumpAndSettle();
    final beforeRefresh = backend.getCount;
    expect(beforeRefresh, greaterThanOrEqualTo(1));
    expect(find.byType(RefreshIndicator), findsOneWidget);
    unawaited(
      tester.state<RefreshIndicatorState>(find.byType(RefreshIndicator)).show(),
    );
    await tester.pumpAndSettle();
    expect(backend.getCount, beforeRefresh + 1);
  });

  testWidgets('Mother Home pull-to-refresh propagates to Today panel', (
    tester,
  ) async {
    final backend = StatefulTodayBackend();
    tester.view.physicalSize = const Size(800, 1600);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(
        home: MotherHomeScreen(
          todayTaskService: backend.service,
          dashboardLoader: () async => _dashboard(),
          reminderLoader: () async => const [],
        ),
      ),
    );
    await tester.pumpAndSettle();
    final beforeRefresh = backend.getCount;
    expect(beforeRefresh, greaterThanOrEqualTo(1));
    unawaited(
      tester.state<RefreshIndicatorState>(find.byType(RefreshIndicator)).show(),
    );
    await tester.pumpAndSettle();
    expect(backend.getCount, beforeRefresh + 1);
  });

  testWidgets('Family Home pull-to-refresh propagates to Today panel', (
    tester,
  ) async {
    final backend = StatefulTodayBackend();
    tester.view.physicalSize = const Size(800, 1600);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(
        home: FamilyMemberHomeScreen(todayTaskService: backend.service),
      ),
    );
    await tester.pumpAndSettle();
    final beforeRefresh = backend.getCount;
    expect(beforeRefresh, greaterThanOrEqualTo(1));
    unawaited(
      tester.state<RefreshIndicatorState>(find.byType(RefreshIndicator)).show(),
    );
    await tester.pumpAndSettle();
    expect(backend.getCount, beforeRefresh + 1);
  });
}
