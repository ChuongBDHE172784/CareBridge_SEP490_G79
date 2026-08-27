import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/reminder/models/today_task_model.dart';
import 'package:untitled/features/reminder/widgets/today_tasks_panel.dart';

import 'support/today_task_red_fixture.dart';

Widget _panel(StatefulTodayBackend backend) => MaterialApp(
  home: Scaffold(
    body: SingleChildScrollView(
      child: TodayTasksPanel(service: backend.service),
    ),
  ),
);

void main() {
  test(
    'sequential GET pending -> POST once -> GET completed is stable',
    () async {
      final backend = StatefulTodayBackend();

      final pending = await backend.service.loadToday(
        date: DateTime(2026, 8, 3),
      );
      expect(pending.sections.today.single.taskStatus, TodayTaskStatus.pending);
      expect(
        pending.sections.today.single.allowedActions,
        contains(TodayTaskAction.complete),
      );

      final result = await backend.service.performAction(
        taskKind: TodayTaskKind.checklist,
        taskId: 'today-red-task',
        action: TodayTaskAction.complete,
      );
      expect(result['status'], 'COMPLETED');
      expect(backend.postCount, 1);

      final completed = await backend.service.loadToday(
        date: DateTime(2026, 8, 3),
      );
      final refreshed = await backend.service.loadToday(
        date: DateTime(2026, 8, 3),
      );
      for (final snapshot in [completed, refreshed]) {
        expect(
          snapshot.sections.today.single.taskStatus,
          TodayTaskStatus.completed,
        );
        expect(
          snapshot.sections.today.single.allowedActions,
          contains(TodayTaskAction.reopen),
        );
      }
      expect(backend.getCount, 3);
    },
  );

  testWidgets('panel action reload removes actions and preserves completion', (
    tester,
  ) async {
    final backend = StatefulTodayBackend();
    await tester.pumpWidget(_panel(backend));
    await tester.pumpAndSettle();

    expect(backend.getCount, 1);
    expect(find.byIcon(Icons.radio_button_unchecked_rounded), findsOneWidget);
    await tester.tap(find.byIcon(Icons.radio_button_unchecked_rounded));
    await tester.pumpAndSettle();

    expect(backend.postCount, 1);
    expect(backend.getCount, 2);
    expect(find.byIcon(Icons.check_circle_rounded), findsOneWidget);
  });

  testWidgets(
    'double tap sends one POST and announces action progress/result',
    (tester) async {
      final backend = StatefulTodayBackend();
      backend.actionGate = Completer<void>();
      await tester.pumpWidget(_panel(backend));
      await tester.pumpAndSettle();

      final complete = find.byIcon(Icons.radio_button_unchecked_rounded);
      await tester.tap(complete);
      await tester.tap(complete);
      await tester.pump();
      expect(backend.postCount, 1);

      backend.actionGate!.complete();
      await tester.pumpAndSettle();
      expect(find.byIcon(Icons.check_circle_rounded), findsOneWidget);
      expect(
        find.byWidgetPredicate(
          (widget) =>
              widget is Semantics &&
              widget.properties.liveRegion == true &&
              (widget.properties.label ?? '').contains('Đã hoàn tất'),
        ),
        findsOneWidget,
      );
    },
  );

  testWidgets('failed action keeps pending task and exposes retry feedback', (
    tester,
  ) async {
    final backend = StatefulTodayBackend(failActions: true);
    await tester.pumpWidget(_panel(backend));
    await tester.pumpAndSettle();
    await tester.tap(find.byIcon(Icons.radio_button_unchecked_rounded));
    await tester.pumpAndSettle();

    expect(backend.postCount, 1);
    expect(backend.getCount, 1);
    expect(find.byIcon(Icons.radio_button_unchecked_rounded), findsOneWidget);
    expect(find.textContaining('Không thể cập nhật công việc'), findsOneWidget);
  });

  test('API labels remain canonical and case-stable', () {
    expect(TodayTaskKind.checklist.apiValue, 'CHECKLIST');
    expect(TodayTaskKind.reminder.apiValue, 'REMINDER');
    expect(TodayTaskKind.careTask.apiValue, 'CARE_TASK');
    expect(TodayTaskAction.complete.apiValue, 'COMPLETE');
    expect(TodayTaskAction.skip.apiValue, 'SKIP');
    expect(TodayTaskAction.reopen.apiValue, 'REOPEN');
    expect(TodayTaskSkipReason.notApplicable.apiValue, 'NOT_APPLICABLE');
    expect(TodayTaskSkipReason.userChoice.apiValue, 'USER_CHOICE');
    expect(TodayTaskSkipReason.lifecycleChanged.apiValue, 'LIFECYCLE_CHANGED');
  });

  test('Today GET includes the server timezone contract', () {
    final source = File(
      'lib/features/reminder/services/today_task_service.dart',
    ).readAsStringSync();
    expect(
      source,
      contains('X-User-Timezone'),
      reason: 'Today must send a valid IANA timezone to preserve day buckets',
    );
  });
}
