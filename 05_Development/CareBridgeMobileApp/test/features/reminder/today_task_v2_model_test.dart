import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/reminder/models/today_task_model.dart';
import 'package:untitled/features/reminder/models/today_task_support_function.dart';

void main() {
  test('maps canonical and compatibility cadence values', () {
    expect(TodayTaskCadenceApi.fromApi('DAILY'), TodayTaskCadence.daily);
    expect(TodayTaskCadenceApi.fromApi('WEEKLY'), TodayTaskCadence.weekly);
    expect(TodayTaskCadenceApi.fromApi('ONCE'), TodayTaskCadence.once);
    expect(
      TodayTaskCadenceApi.fromApi(
        null,
        scheduleType: 'WEEKLY',
        materializationPolicy: 'EACH_WEEK',
      ),
      TodayTaskCadence.weekly,
    );
    expect(
      TodayTaskCadenceApi.fromApi(
        null,
        scheduleType: 'DAILY',
        materializationPolicy: 'EACH_DAY',
      ),
      TodayTaskCadence.daily,
    );
    expect(
      TodayTaskCadenceApi.fromApi(
        'ONCE',
        scheduleType: 'DAILY',
        materializationPolicy: 'EACH_DAY',
      ),
      TodayTaskCadence.once,
      reason: 'canonical cadence must win over compatibility fields',
    );
    expect(TodayTaskCadenceApi.fromApi(null), TodayTaskCadence.unknown);
  });

  test('parses cadence metadata onto a Today task', () {
    final task = TodayTask.fromJson({
      'taskKind': 'CHECKLIST',
      'taskId': 'cadence-1',
      'title': 'Duy trì thói quen',
      'origin': 'SYSTEM_TEMPLATE',
      'targetSubject': 'MOTHER',
      'status': 'PENDING',
      'timeBucket': 'TODAY',
      'cadence': 'WEEKLY',
      'allowedActions': ['COMPLETE'],
    });

    expect(task.cadence, TodayTaskCadence.weekly);
    expect(task.cadenceLabel, 'Hằng tuần');

    final legacyMetadataTask = TodayTask.fromJson({
      'taskKind': 'CHECKLIST',
      'taskId': 'cadence-compat-1',
      'title': 'Nhắc mỗi ngày',
      'origin': 'SYSTEM_TEMPLATE',
      'targetSubject': 'MOTHER',
      'status': 'PENDING',
      'timeBucket': 'TODAY',
      'scheduleType': 'DAILY',
      'materializationPolicy': 'EACH_DAY',
      'allowedActions': ['COMPLETE'],
    });

    expect(legacyMetadataTask.cadence, TodayTaskCadence.daily);
  });

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
              'stage': 'BABY_CARE',
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
      expect(task.stage, TodayChecklistStage.babyCare);
      expect(task.stage.label, 'Chăm bé');
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

  test('uses a neutral label for targetless V2 checklist tasks', () {
    final task = TodayTask.fromJson({
      'taskKind': 'CHECKLIST',
      'taskId': 'v2-targetless-1',
      'title': 'Duy trì thói quen hằng ngày',
      'origin': 'USER_CREATED',
      'targetSubject': null,
      'status': 'PENDING',
      'timeBucket': 'TODAY',
      'allowedActions': ['COMPLETE'],
    });

    expect(task.target, TodayTaskTarget.unknown);
    expect(task.targetLabel, 'Khuyến nghị');
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

  test('parses checklist description and controlled support function', () {
    final task = TodayTask.fromJson({
      'taskKind': 'CHECKLIST',
      'taskId': 'detail-1',
      'title': 'Chuẩn bị hồ sơ khám',
      'description': 'Kiểm tra và bổ sung các kết quả khám gần nhất.',
      'supportFunction': 'HEALTH_RECORDS',
      'origin': 'SYSTEM_TEMPLATE',
      'targetSubject': 'MOTHER',
      'status': 'PENDING',
      'timeBucket': 'TODAY',
      'allowedActions': ['COMPLETE'],
    });

    expect(task.description, 'Kiểm tra và bổ sung các kết quả khám gần nhất.');
    expect(
      task.supportFunction?.code,
      TodayTaskSupportFunctionCode.healthRecords,
    );
    expect(task.supportFunction?.route, '/health-records');
  });

  test('resolves maternal health metrics from a journey context', () {
    final task = TodayTask.fromJson({
      'taskKind': 'CHECKLIST',
      'taskId': 'maternal-metrics-1',
      'title': 'Theo dõi chỉ số',
      'supportFunction': 'MATERNAL_HEALTH_METRICS',
      'careContextType': 'JOURNEY',
      'careContextId': 'journey-1',
      'origin': 'SYSTEM_TEMPLATE',
      'targetSubject': 'MOTHER',
      'status': 'PENDING',
      'timeBucket': 'TODAY',
      'allowedActions': ['COMPLETE'],
    });

    expect(
      task.supportFunction?.code,
      TodayTaskSupportFunctionCode.maternalHealthMetrics,
    );
    expect(
      task.supportFunction?.routeFor(
        careContextType: task.careContextType,
        careContextId: task.careContextId,
      ),
      '/journeys/journey-1/metrics/trend?metricType=BMI',
    );
    expect(
      task.supportFunction?.routeFor(
        careContextType: 'BABY',
        careContextId: 'journey-1',
      ),
      isNull,
    );
  });

  test('maps every support function code to its safe native route', () {
    const expectedRoutes = <String, String>{
      'HEALTH_RECORDS': '/health-records',
      'MATERNAL_EXERCISES': '/mother-exercise',
      'APPOINTMENTS': '/appointments/calendar',
      'REMINDERS': '/reminder-schedules',
      'JOURNEY': '/mother-home?tab=1',
      'BABY_CARE': '/babies',
      'EXPERT_CONSULTATION': '/experts',
      'CONTENT_LIBRARY': '/content',
      'AI_TRIAGE': '/triage/intake',
    };

    for (final entry in expectedRoutes.entries) {
      final task = TodayTask.fromJson({
        'taskKind': 'CHECKLIST',
        'taskId': 'support-${entry.key}',
        'title': entry.key,
        'supportFunction': entry.key,
        'origin': 'SYSTEM_TEMPLATE',
        'targetSubject': 'MOTHER',
        'status': 'PENDING',
        'timeBucket': 'TODAY',
        'allowedActions': ['COMPLETE'],
      });

      expect(task.supportFunction?.apiValue, entry.key, reason: entry.key);
      expect(task.supportFunction?.route, entry.value, reason: entry.key);
    }
  });

  test('keeps absent or unknown support functions non-navigable', () {
    Map<String, dynamic> payload([String? supportFunction]) => {
      'taskKind': 'CHECKLIST',
      'taskId': 'support-optional',
      'title': 'Việc không có hỗ trợ',
      'supportFunction': ?supportFunction,
      'origin': 'USER_CREATED',
      'targetSubject': 'MOTHER',
      'status': 'PENDING',
      'timeBucket': 'UNSCHEDULED',
      'allowedActions': ['COMPLETE'],
    };

    expect(TodayTask.fromJson(payload()).supportFunction, isNull);
    expect(
      TodayTask.fromJson(
        payload('https://example.invalid/unsafe'),
      ).supportFunction,
      isNull,
    );
  });

  test('parses ready sequence projection and next-set metadata', () {
    final snapshot = TodayTasksSnapshot.fromJson({
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
      'correlationId': 'correlation-2',
      'sequence': {
        'sequenceState': 'READY_TO_ADVANCE',
        'currentInstanceId': 'instance-1',
        'currentTemplateVersionId': 'version-1',
        'currentSetName': 'Nền tảng sức khỏe',
        'currentPosition': 1,
        'totalPositions': 3,
        'qualifiedPositions': 1,
        'advanceAvailable': true,
        'nextSet': {'name': 'Tư vấn trước thai kỳ', 'position': 2},
        'sequenceComplete': false,
      },
    });

    expect(snapshot.sequence?.state, TodaySequenceState.readyToAdvance);
    expect(snapshot.sequence?.readyToAdvance, isTrue);
    expect(snapshot.sequence?.nextSet?.position, 2);
    expect(snapshot.sequence?.advanceAvailable, isTrue);
  });
}
