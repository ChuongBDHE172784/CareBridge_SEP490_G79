import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/expert/screens/expert_calendar_screen.dart';

class _FakeCalendarApi implements ExpertCalendarApi {
  final List<Map<String, dynamic>> slots;
  Object? getError;
  Object? postError;
  final List<Map<String, dynamic>> postBodies = [];
  final List<Map<String, dynamic>> putBodies = [];
  final List<String> deletePaths = [];
  int getCount = 0;

  _FakeCalendarApi({
    List<Map<String, dynamic>>? slots,
    this.getError,
    this.postError,
  }) : slots = slots ?? [];

  @override
  Future<dynamic> get(String path) async {
    getCount++;
    final error = getError;
    if (error != null) throw error;
    return {'data': slots};
  }

  @override
  Future<dynamic> post(String path, Map<String, dynamic> body) async {
    final error = postError;
    if (error != null) throw error;
    postBodies.add(body);
    final row = <String, dynamic>{
      'availabilityId': 'slot-${postBodies.length}',
      'startAt': body['startAt'],
      'endAt': body['endAt'],
      'status': 'AVAILABLE',
    };
    slots.add(row);
    return {'data': row};
  }

  @override
  Future<dynamic> put(String path, Map<String, dynamic> body) async {
    final error = postError;
    if (error != null) throw error;
    putBodies.add(body);
    slots.clear();
    for (final date in (body['targetDates'] as List)) {
      for (final item in (body['slots'] as List)) {
        final local = DateTime.parse('$date ${item['startTime']}:00');
        slots.add({
          'availabilityId': 'slot-${slots.length + 1}',
          'startAt': local.toUtc().toIso8601String(),
          'endAt': local
              .add(const Duration(hours: 1))
              .toUtc()
              .toIso8601String(),
          'status': 'AVAILABLE',
        });
      }
    }
    return {'data': slots};
  }

  @override
  Future<dynamic> delete(String path) async {
    deletePaths.add(path);
    final id = path.split('/').last;
    slots.removeWhere((row) => '${row['availabilityId'] ?? row['id']}' == id);
    return null;
  }
}

String _routeDate(DateTime date) =>
    '${date.year}-${date.month.toString().padLeft(2, '0')}-'
    '${date.day.toString().padLeft(2, '0')}';

Future<void> _openDate(WidgetTester tester, DateTime date) async {
  final today = DateTime.now();
  if (today.year != date.year || today.month != date.month) {
    await tester.tap(find.byKey(const Key('calendar-next-month')));
    await tester.pumpAndSettle();
  }
  final finder = find.byKey(Key('calendar-day-${_routeDate(date)}'));
  await tester.ensureVisible(finder);
  await tester.tap(finder);
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('renders a month calendar and retry state without the add FAB', (
    tester,
  ) async {
    final api = _FakeCalendarApi();
    await tester.pumpWidget(MaterialApp(home: ExpertCalendarScreen(api: api)));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('calendar-month-label')), findsOneWidget);
    expect(find.byKey(const Key('calendar-add-slot')), findsNothing);
    expect(find.byKey(const Key('calendar-refresh')), findsOneWidget);
    expect(api.getCount, 1);

    await tester.tap(find.byKey(const Key('calendar-refresh')));
    await tester.pumpAndSettle();
    expect(api.getCount, 2);

    final errorApi = _FakeCalendarApi(
      getError: ApiException(503, '{"message":"Service unavailable"}'),
    );
    await tester.pumpWidget(
      MaterialApp(
        home: ExpertCalendarScreen(
          key: const ValueKey('error-calendar'),
          api: errorApi,
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Thử lại'), findsOneWidget);
    expect(find.byType(CircularProgressIndicator), findsNothing);
  });

  testWidgets('opens a day editor with fourteen one-hour slots', (
    tester,
  ) async {
    final api = _FakeCalendarApi();
    final date = DateTime.now().add(const Duration(days: 1));
    await tester.pumpWidget(MaterialApp(home: ExpertCalendarScreen(api: api)));
    await tester.pumpAndSettle();

    await _openDate(tester, date);

    for (var hour = 7; hour < 21; hour++) {
      expect(find.byKey(Key('availability-hour-$hour')), findsOneWidget);
    }
    expect(find.text('07:00–08:00'), findsOneWidget);
    expect(find.text('20:00–21:00'), findsOneWidget);
  });

  testWidgets('saves selected hours through one atomic batch request', (
    tester,
  ) async {
    final api = _FakeCalendarApi();
    final date = DateTime.now().add(const Duration(days: 1));
    await tester.pumpWidget(MaterialApp(home: ExpertCalendarScreen(api: api)));
    await tester.pumpAndSettle();
    await _openDate(tester, date);

    await tester.tap(find.byKey(const Key('availability-hour-7')));
    await tester.tap(find.byKey(const Key('availability-hour-8')));
    await tester.tap(find.byKey(const Key('availability-save')));
    await tester.pumpAndSettle();

    expect(api.putBodies, hasLength(1));
    expect(api.putBodies.single['slots'], [
      {'startTime': '07:00'},
      {'startTime': '08:00'},
    ]);
    expect(find.text('2 ca'), findsOneWidget);
    expect(find.text('Đã cập nhật lịch rảnh'), findsOneWidget);
  });

  testWidgets('clearing an available day sends an empty batch slot list', (
    tester,
  ) async {
    final date = DateTime.now().add(const Duration(days: 2));
    final start = DateTime(date.year, date.month, date.day, 7);
    final api = _FakeCalendarApi(
      slots: [
        {
          'availabilityId': 'slot-delete',
          'startAt': start.toUtc().toIso8601String(),
          'endAt': start
              .add(const Duration(hours: 2))
              .toUtc()
              .toIso8601String(),
          'status': 'AVAILABLE',
        },
      ],
    );
    await tester.pumpWidget(MaterialApp(home: ExpertCalendarScreen(api: api)));
    await tester.pumpAndSettle();
    await _openDate(tester, date);

    await tester.tap(find.byKey(const Key('availability-clear-all')));
    await tester.tap(find.byKey(const Key('availability-save')));
    await tester.pumpAndSettle();

    expect(api.putBodies.single['slots'], isEmpty);
  });

  testWidgets('reloads authoritative data after an overlap failure', (
    tester,
  ) async {
    final api = _FakeCalendarApi(
      postError: ApiException(
        409,
        '{"message":"Availability overlaps an existing slot"}',
      ),
    );
    final date = DateTime.now().add(const Duration(days: 1));
    await tester.pumpWidget(MaterialApp(home: ExpertCalendarScreen(api: api)));
    await tester.pumpAndSettle();
    await _openDate(tester, date);

    await tester.tap(find.byKey(const Key('availability-hour-9')));
    await tester.tap(find.byKey(const Key('availability-save')));
    await tester.pumpAndSettle();

    expect(api.getCount, 2);
    expect(
      find.text(
        'Khung giờ này trùng với lịch hiện có. Hãy chọn thời gian khác.',
      ),
      findsOneWidget,
    );
  });

  test('resolves week, month, weekday and month-day application scopes', () {
    final anchor = DateTime(2026, 8, 12);
    final today = DateTime(2026, 8, 1);

    expect(
      resolveAvailabilityDates(
        anchor: anchor,
        scope: AvailabilityApplyScope.week,
        today: today,
      ).map((date) => date.day),
      [10, 11, 12, 13, 14, 15, 16],
    );
    expect(
      resolveAvailabilityDates(
        anchor: anchor,
        scope: AvailabilityApplyScope.month,
        today: today,
      ),
      hasLength(31),
    );
    expect(
      resolveAvailabilityDates(
        anchor: anchor,
        scope: AvailabilityApplyScope.selectedWeekdays,
        weekdays: {DateTime.monday, DateTime.wednesday},
        today: today,
      ).every(
        (date) =>
            date.weekday == DateTime.monday ||
            date.weekday == DateTime.wednesday,
      ),
      isTrue,
    );
    expect(
      resolveAvailabilityDates(
        anchor: anchor,
        scope: AvailabilityApplyScope.selectedMonthDays,
        monthDays: {1, 15, 31},
        today: today,
      ).map((date) => date.day),
      [1, 15, 31],
    );
  });

  test('filters past targets and merges only valid 07:00-21:00 hours', () {
    final dates = resolveAvailabilityDates(
      anchor: DateTime(2026, 8, 12),
      scope: AvailabilityApplyScope.month,
      today: DateTime(2026, 8, 20),
    );
    expect(dates.first, DateTime(2026, 8, 20));

    final ranges = mergeAvailabilityHours(DateTime(2026, 8, 20), {
      6,
      7,
      8,
      10,
      20,
      21,
    });
    expect(ranges, hasLength(3));
    expect((ranges[0].start.hour, ranges[0].end.hour), (7, 9));
    expect((ranges[1].start.hour, ranges[1].end.hour), (10, 11));
    expect((ranges[2].start.hour, ranges[2].end.hour), (20, 21));
  });

  test('maps a long available interval back to one-hour visual slots', () {
    final date = DateTime(2026, 8, 20);
    final slots = [
      {
        'startAt': DateTime(2026, 8, 20, 7).toUtc().toIso8601String(),
        'endAt': DateTime(2026, 8, 20, 10).toUtc().toIso8601String(),
        'status': 'AVAILABLE',
      },
    ];
    expect(availableHoursForDate(slots, date), [7, 8, 9]);
  });

  test('strict range validation rejects equal or reversed times', () {
    final now = DateTime(2026, 7, 30, 8);
    final start = DateTime(2026, 7, 31, 10);

    expect(
      validateAvailabilityRange(start, start, now: now),
      'Thời gian kết thúc phải sau thời gian bắt đầu.',
    );
    expect(
      validateAvailabilityRange(
        start,
        start.subtract(const Duration(minutes: 1)),
        now: now,
      ),
      'Thời gian kết thúc phải sau thời gian bắt đầu.',
    );
  });

  test('combines a custom local date and time before UTC encoding', () {
    final local = combineLocalDateAndTime(
      DateTime(2026, 8, 3),
      const TimeOfDay(hour: 19, minute: 45),
    );

    expect((local.year, local.month, local.day), (2026, 8, 3));
    expect((local.hour, local.minute), (19, 45));
    expect(DateTime.parse(local.toUtc().toIso8601String()).isUtc, isTrue);
  });
}
