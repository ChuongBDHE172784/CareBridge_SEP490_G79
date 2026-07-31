import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/checklist/services/user_checklist_service.dart';
import 'package:untitled/features/home/screens/family_member_home_screen.dart';
import 'package:untitled/features/home/screens/mother_home_screen.dart';
import 'package:untitled/features/reminder/screens/today_tasks_screen.dart';
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
    await tester.pumpWidget(
      MaterialApp(home: FamilyMemberHomeScreen(todayTaskService: service)),
    );
    await tester.pump();

    final panel = tester.widget<TodayTasksPanel>(find.byType(TodayTasksPanel));
    expect(panel.service, same(service));
    expect(panel.audience, TodayTasksAudience.family);
    expect(panel.layout, TodayTasksLayout.timeBuckets);
    expect(tester.getSemantics(find.byType(TodayTasksPanel)).label, isNotEmpty);
  });

  testWidgets('Family task shortcut navigates to the Family Today screen', (
    tester,
  ) async {
    final service = _service();
    await tester.pumpWidget(
      MaterialApp(home: FamilyMemberHomeScreen(todayTaskService: service)),
    );
    await tester.pump();

    await tester.tap(find.byIcon(Icons.check_circle_outline).first);
    await tester.pumpAndSettle();

    final screen = tester.widget<TodayTasksScreen>(
      find.byType(TodayTasksScreen),
    );
    expect(screen.service, same(service));
    expect(screen.audience, TodayTasksAudience.family);
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
}
