import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/reminder/models/reminder_schedule_model.dart';
import 'package:untitled/features/reminder/services/reminder_schedule_service.dart';

void main() {
  test('creates one logical schedule with multiple local times', () async {
    String? path;
    Map<String, dynamic>? body;
    final service = ReminderScheduleService(
      getRequest: (_) async => const {},
      postRequest: (requestPath, requestBody) async {
        path = requestPath;
        body = requestBody;
        return {
          'data': {
            'scheduleId': 'schedule-1',
            'title': 'Cho con bú',
            'times': ['07:00', '12:00'],
            'timeZone': 'Asia/Ho_Chi_Minh',
            'recurrence': 'DAILY',
            'startDate': '2026-08-04',
            'active': true,
            'revision': 1,
          },
        };
      },
    );

    final schedule = await service.create(
      title: 'Cho con bú',
      times: const ['07:00', '12:00'],
      timeZone: 'Asia/Ho_Chi_Minh',
      recurrence: ReminderScheduleRecurrence.daily,
      startDate: DateTime(2026, 8, 4),
    );

    expect(path, '/api/v1/reminder-schedules');
    expect(body?['times'], ['07:00', '12:00']);
    expect(schedule.id, 'schedule-1');
    expect(schedule.times, ['07:00', '12:00']);
    expect(schedule.recurrence, ReminderScheduleRecurrence.daily);
  });

  test(
    'disable uses the schedule resource and does not create a task action',
    () async {
      String? path;
      Map<String, dynamic>? body;
      final service = ReminderScheduleService(
        getRequest: (_) async => const {},
        patchRequest: (requestPath, requestBody) async {
          path = requestPath;
          body = requestBody;
          return {
            'id': 'schedule-1',
            'title': 'Vitamin',
            'times': ['08:00'],
            'timeZone': 'Asia/Ho_Chi_Minh',
            'recurrence': 'NONE',
            'startDate': '2026-08-04',
            'active': false,
            'revision': 2,
          };
        },
      );

      final schedule = await service.disable('schedule-1');

      expect(path, '/api/v1/reminder-schedules/schedule-1');
      expect(body, {'active': false});
      expect(schedule.active, isFalse);
      expect(schedule.revision, 2);
    },
  );
}
