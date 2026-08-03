import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/reminder/models/today_task_model.dart';
import 'package:untitled/features/reminder/services/today_task_service.dart';

void main() {
  test(
    'loads the authoritative Today endpoint with requested local date',
    () async {
      String? capturedPath;
      Map<String, dynamic>? capturedQuery;
      final service = TodayTaskService(
        getRequest: (path, {queryParams}) async {
          capturedPath = path;
          capturedQuery = queryParams;
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
              'counts': {
                'overdue': 0,
                'today': 0,
                'upcoming': 0,
                'unscheduled': 0,
              },
              'correlationId': 'c-1',
            },
          };
        },
        postRequest: (_, _) async => const {},
      );

      final result = await service.loadToday(date: DateTime(2026, 8, 3));

      expect(capturedPath, '/api/v1/checklists/current/tasks');
      expect(capturedQuery, {'date': '2026-08-03'});
      expect(result.zoneId, 'Asia/Ho_Chi_Minh');
    },
  );

  test(
    'complete and skip dispatch through unified retry-safe facade',
    () async {
      final calls = <(String, Map<String, dynamic>)>[];
      final service = TodayTaskService(
        getRequest: (_, {queryParams}) async => const {},
        postRequest: (path, body) async {
          calls.add((path, body));
          return {
            'data': {...body, 'status': 'COMPLETED'},
          };
        },
        clientRequestIdFactory: () => 'client-1',
      );

      await service.performAction(
        taskKind: TodayTaskKind.checklist,
        taskId: 'task-1',
        action: TodayTaskAction.complete,
      );
      await service.performAction(
        taskKind: TodayTaskKind.careTask,
        taskId: 'task-2',
        action: TodayTaskAction.skip,
        reason: TodayTaskSkipReason.userChoice,
      );

      expect(calls[0].$1, '/api/v1/checklists/tasks/task-1/actions');
      expect(calls[0].$2, {
        'action': 'COMPLETE',
        'clientRequestId': 'client-1',
        'reason': null,
      });
      expect(calls[1].$1, '/api/v1/tasks/CARE_TASK/task-2/actions');
      expect(calls[1].$2['action'], 'SKIP');
      expect(calls[1].$2['reason'], 'USER_CHOICE');
    },
  );

  test('advanceSequence sends the current instance and stable UUID', () async {
    String? capturedPath;
    Map<String, dynamic>? capturedBody;
    final service = TodayTaskService(
      getRequest: (_, {queryParams}) async => const {},
      postRequest: (path, body) async {
        capturedPath = path;
        capturedBody = body;
        return {
          'data': {
            'predecessorInstanceId': 'instance-1',
            'successorInstanceId': 'instance-2',
          },
        };
      },
      clientRequestIdFactory: () => '00000000-0000-0000-0000-000000000001',
    );

    await service.advanceSequence(currentInstanceId: 'instance-1');

    expect(capturedPath, '/api/v1/checklists/sequences/advance');
    expect(capturedBody, {
      'currentInstanceId': 'instance-1',
      'clientRequestId': '00000000-0000-0000-0000-000000000001',
    });
  });
}
