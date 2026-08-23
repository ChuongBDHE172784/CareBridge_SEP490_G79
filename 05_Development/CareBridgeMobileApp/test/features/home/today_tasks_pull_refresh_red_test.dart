import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/familySync/services/family_home_service.dart';
import 'package:untitled/features/home/screens/family_member_home_screen.dart';
import 'package:untitled/features/home/screens/mother_home_screen.dart';
import 'package:untitled/features/journey/models/journey_model.dart';

import '../reminder/support/today_task_red_fixture.dart';

JourneyDashboard _dashboard() => const JourneyDashboard(
  journeyId: 'red-journey',
  journeyType: 'PREGNANCY',
  status: 'ACTIVE_PREGNANCY',
  pregnancyWeek: 20,
);

/// A single-care-group family dashboard: the Today panel only mounts once a group is
/// unambiguously selected, so every Family Home widget test needs this snapshot.
FamilyHomeSnapshot familyDashboardWithOneGroup() => FamilyHomeSnapshot(
  groups: [
    FamilyHomeGroup(
      id: 'group-1',
      name: 'Nhà mình',
      joinedAt: DateTime(2026, 1, 1),
      lastActivityAt: DateTime(2026, 8, 1),
      relationshipRole: 'HUSBAND',
      customRelationshipRole: null,
      permissionScope: const FamilyHomePermission(
        calendar: true,
        logs: true,
        alerts: true,
        checklistView: true,
        records: true,
      ),
      aggregate: const FamilyHomeAggregate(
        overdue: 0,
        dueSoon: 0,
        inProgress: 0,
        alerts: 0,
      ),
    ),
  ],
  globalAggregate: const FamilyHomeAggregate(
    overdue: 0,
    dueSoon: 0,
    inProgress: 0,
    alerts: 0,
  ),
  selectedCareGroupId: 'group-1',
  selectedGroupDetail: null,
);

void main() {
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
        home: FamilyMemberHomeScreen(
          todayTaskService: backend.service,
          dashboardLoader: ({selectedCareGroupId}) async =>
              familyDashboardWithOneGroup(),
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
}
