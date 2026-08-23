import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/checklist/services/user_checklist_service.dart';
import 'package:untitled/features/familySync/services/family_home_service.dart';
import 'package:untitled/features/home/screens/family_member_home_screen.dart';
import 'package:untitled/features/home/screens/mother_home_screen.dart';
import 'package:untitled/features/reminder/services/today_task_service.dart';
import 'package:untitled/features/reminder/widgets/today_tasks_panel.dart';

TodayTaskService _service({void Function()? onGet}) => TodayTaskService(
  getRequest: (_, {queryParams}) async {
    onGet?.call();
    return {
      'data': {
        'asOf': '2026-08-03T01:00:00Z',
        'zoneId': 'Asia/Ho_Chi_Minh',
        'horizonDays': 7,
        'sections': {
          'overdue': [],
          'today': [],
          'upcoming': [],
          'unscheduled': [],
        },
        'counts': {'overdue': 0, 'today': 0, 'upcoming': 0, 'unscheduled': 0},
        'correlationId': 'home-integration',
      },
    };
  },
  postRequest: (_, body) async => {'data': body},
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
  testWidgets('Mother Home owns the injected Mother Today panel', (
    tester,
  ) async {
    final service = _service();
    tester.view.physicalSize = const Size(800, 1600);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(
      MaterialApp(home: MotherHomeScreen(todayTaskService: service)),
    );
    await tester.pump();

    final panel = tester.widget<TodayTasksPanel>(find.byType(TodayTasksPanel));
    expect(panel.service, same(service));
    expect(panel.audience, TodayTasksAudience.mother);
    expect(panel.layout, TodayTasksLayout.sourceGroups);
    expect(tester.getSemantics(find.byType(TodayTasksPanel)).label, isNotEmpty);
  });

  testWidgets('Family Home owns the injected Family Today panel', (
    tester,
  ) async {
    final service = _service();
    // The Family dashboard scrolls; a short viewport leaves the Today panel unbuilt.
    tester.view.physicalSize = const Size(800, 1600);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(
      MaterialApp(
        home: FamilyMemberHomeScreen(
          todayTaskService: service,
          dashboardLoader: ({selectedCareGroupId}) async =>
              familyDashboardWithOneGroup(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    final panel = tester.widget<TodayTasksPanel>(find.byType(TodayTasksPanel));
    expect(panel.service, same(service));
    expect(panel.audience, TodayTasksAudience.family);
    expect(panel.layout, TodayTasksLayout.sourceGroups);
    expect(panel.careGroupId, 'group-1');
    expect(tester.getSemantics(find.byType(TodayTasksPanel)).label, isNotEmpty);
  });

  // The Family Today list is embedded in the dashboard now; there is no shortcut that pushes a
  // standalone Today screen any more. Pin that: the panel and its heading actions live inline.
  testWidgets('Family Home hosts Today inline instead of pushing a Today screen', (
    tester,
  ) async {
    final service = _service();
    // The Family dashboard scrolls; a short viewport leaves the Today panel unbuilt.
    tester.view.physicalSize = const Size(800, 1600);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(
      MaterialApp(
        home: FamilyMemberHomeScreen(
          todayTaskService: service,
          dashboardLoader: ({selectedCareGroupId}) async =>
              familyDashboardWithOneGroup(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(TodayTasksPanel), findsOneWidget);
    expect(
      find.byKey(const Key('family-home-checklist-history-button')),
      findsOneWidget,
    );
  });

  testWidgets(
    'retained Mother Home refreshes after assignment from an alternate entry',
    (tester) async {
      var todayLoads = 0;
      final todayService = _service(onGet: () => todayLoads++);
      final assignmentService = UserChecklistService(
        postRequest: (_, _) async => {
          'data': {'createdTasks': 1, 'existingTasks': 0},
        },
      );
      tester.view.physicalSize = const Size(800, 1600);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      await tester.pumpWidget(
        MaterialApp(
          home: Offstage(
            offstage: true,
            child: MotherHomeScreen(todayTaskService: todayService),
          ),
        ),
      );
      await tester.pumpAndSettle();
      final beforeAssignment = todayLoads;

      await assignmentService.addTemplate(
        templateId: 'optional-from-journey',
        journeyId: 'journey-1',
      );
      await tester.pumpAndSettle();

      expect(todayLoads, beforeAssignment + 1);

      final afterRefresh = todayLoads;
      await tester.pumpWidget(const MaterialApp(home: SizedBox.shrink()));
      await tester.pump();
      await assignmentService.addTemplate(
        templateId: 'optional-after-home-dispose',
        journeyId: 'journey-1',
      );
      await tester.pump();

      expect(todayLoads, afterRefresh);
    },
  );

  testWidgets(
    'Family Home displays friendly notice when mother has not granted checklistView permission',
    (tester) async {
      final service = _service();
      tester.view.physicalSize = const Size(800, 1600);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final snapshotNoPermission = FamilyHomeSnapshot(
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
              checklistView: false,
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
        selectedGroupDetail: const FamilyHomeGroupDetail(
          careGroupId: 'group-1',
          motherDisplayName: 'Mẹ Lan',
          relationshipRole: 'HUSBAND',
          customRelationshipRole: null,
          permissionScope: FamilyHomePermission(
            calendar: true,
            logs: true,
            alerts: true,
            checklistView: false,
            records: true,
          ),
          members: [],
          todayReminders: [],
          healthMetricSummaries: [],
          alerts: [],
          memberCount: 2,
          sharedDataSummary: FamilyHomeSharedDataSummary(
            totalItems: 0,
            categories: [],
          ),
        ),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: FamilyMemberHomeScreen(
            todayTaskService: service,
            dashboardLoader: ({selectedCareGroupId}) async =>
                snapshotNoPermission,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byType(TodayTasksPanel), findsNothing);
      expect(
        find.byKey(const Key('family-dashboard-no-checklist-permission')),
        findsOneWidget,
      );
      expect(
        find.text('Mẹ chưa cấp quyền chia sẻ việc cần làm.'),
        findsOneWidget,
      );
    },
  );
}
