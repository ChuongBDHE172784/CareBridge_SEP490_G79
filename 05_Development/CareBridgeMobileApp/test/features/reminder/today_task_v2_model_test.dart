import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/reminder/models/today_task_model.dart';

void main() {
  test(
    'parses unified Today envelope without losing care context metadata',
    () {
      final snapshot = TodayTasksSnapshot.fromJson({
        'asOf': '2026-08-03T01:00:00Z',
        'zoneId': 'Asia/Ho_Chi_Minh',
        'horizonDays': 7,
        'sections': {
          'overdue': [
            {
              'taskKind': 'CHECKLIST',
              'taskId': 'task-1',
              'instanceId': 'instance-1',
              'templateVersionId': 'version-1',
              'careGroupId': 'group-1',
              'careGroupName': 'Gia đình An',
              'careContextType': 'BABY',
              'careContextId': 'baby-1',
              'careContextLabel': 'Bé An',
              'title': 'Chuẩn bị bình sữa',
              'targetSubject': 'BABY',
              'origin': 'SYSTEM_TEMPLATE',
              'status': 'PENDING',
              'timeBucket': 'OVERDUE',
              'allowedActions': ['COMPLETE'],
              'dueAt': '2026-08-02T08:00:00Z',
            },
          ],
          'today': [],
          'upcoming': [],
          'unscheduled': [],
        },
        'counts': {'overdue': 1, 'today': 0, 'upcoming': 0, 'unscheduled': 0},
        'correlationId': 'correlation-1',
      });

      final task = snapshot.sections.overdue.single;
      expect(task.kind, TodayTaskKind.checklist);
      expect(task.origin, TodayTaskOrigin.systemTemplate);
      expect(task.target, TodayTaskTarget.baby);
      expect(task.bucket, TodayTimeBucket.overdue);
      expect(task.allowedActions, {TodayTaskAction.complete});
      expect(task.careGroupLabel, 'Gia đình An');
      expect(task.careContextLabel, 'Bé An');
      expect(snapshot.totalCount, 1);
    },
  );

  test('preserves active unscheduled task with nullable dueAt', () {
    final task = TodayTask.fromJson({
      'taskKind': 'CHECKLIST',
      'taskId': 'legacy-1',
      'title': 'Việc cũ cần xem lại',
      'origin': 'USER_CREATED',
      'targetSubject': 'MOTHER',
      'status': 'IN_PROGRESS',
      'timeBucket': 'UNSCHEDULED',
      'allowedActions': ['COMPLETE'],
      'dueAt': null,
    });

    expect(task.dueAt, isNull);
    expect(task.bucket, TodayTimeBucket.unscheduled);
    expect(task.originLabel, 'My care');
    expect(task.targetLabel, 'My care');
  });

  test('parses completed checklist reopen action', () {
    final task = TodayTask.fromJson({
      'taskKind': 'CHECKLIST',
      'taskId': 'completed-1',
      'title': 'Review task',
      'origin': 'SYSTEM_TEMPLATE',
      'targetSubject': 'MOTHER',
      'status': 'COMPLETED',
      'timeBucket': 'TODAY',
      'allowedActions': ['REOPEN'],
    });

    expect(task.isCompleted, isTrue);
    expect(task.allowedActions, {TodayTaskAction.reopen});
    expect(TodayTaskAction.reopen.apiValue, 'REOPEN');
  });
}
