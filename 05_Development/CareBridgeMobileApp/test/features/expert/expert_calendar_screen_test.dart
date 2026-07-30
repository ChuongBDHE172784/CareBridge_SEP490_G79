import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/intl.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/expert/screens/expert_calendar_screen.dart';

class _FakeCalendarApi implements ExpertCalendarApi {
  final List<Map<String, dynamic>> slots;
  Object? getError;
  Object? postError;
  Map<String, dynamic>? lastPostBody;
  String? lastDeletePath;
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
    lastPostBody = body;
    slots.add({
      'availabilityId': 'slot-1',
      'startAt': body['startAt'],
      'endAt': body['endAt'],
      'status': 'AVAILABLE',
    });
    return {'data': slots.last};
  }

  @override
  Future<dynamic> delete(String path) async {
    lastDeletePath = path;
    slots.clear();
    return null;
  }
}

void main() {
  testWidgets('shows empty and retry states without an endless spinner', (
    tester,
  ) async {
    final emptyApi = _FakeCalendarApi();
    await tester.pumpWidget(
      MaterialApp(home: ExpertCalendarScreen(api: emptyApi)),
    );
    await tester.pumpAndSettle();
    expect(find.text('Chưa có khung giờ rảnh nào'), findsOneWidget);
    expect(emptyApi.getCount, 1);
    await tester.tap(find.byKey(const Key('calendar-refresh')));
    await tester.pumpAndSettle();
    expect(emptyApi.getCount, 2);

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

  testWidgets('creates a preset slot in UTC and displays it in local time', (
    tester,
  ) async {
    final api = _FakeCalendarApi();
    await tester.pumpWidget(MaterialApp(home: ExpertCalendarScreen(api: api)));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('calendar-add-slot')));
    await tester.pumpAndSettle();
    expect(find.text('Hôm nay'), findsOneWidget);
    expect(find.text('Ngày mai'), findsOneWidget);
    expect(find.text('Ngày kia'), findsOneWidget);
    expect(find.byKey(const Key('availability-date-custom')), findsOneWidget);
    expect(find.textContaining('Ca sáng'), findsOneWidget);
    expect(find.textContaining('Ca chiều'), findsOneWidget);
    expect(find.textContaining('Ca tối'), findsOneWidget);

    await tester.tap(find.byKey(const Key('availability-preset-afternoon')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('availability-save')));
    await tester.pumpAndSettle();

    expect(api.lastPostBody, isNotNull);
    expect(api.lastPostBody!['startAt'], endsWith('Z'));
    expect(api.lastPostBody!['endAt'], endsWith('Z'));
    final postedStart = DateTime.parse(
      api.lastPostBody!['startAt'] as String,
    ).toLocal();
    final postedEnd = DateTime.parse(
      api.lastPostBody!['endAt'] as String,
    ).toLocal();
    expect((postedStart.hour, postedStart.minute), (13, 30));
    expect((postedEnd.hour, postedEnd.minute), (17, 0));
    final displayedStart = DateFormat('HH:mm').format(postedStart);
    final displayedEnd = DateFormat('HH:mm').format(postedEnd);
    expect(find.text('$displayedStart – $displayedEnd'), findsOneWidget);
    expect(find.text('Đã thêm khung giờ rảnh'), findsOneWidget);
  });

  testWidgets('shows actionable overlap errors inside the create sheet', (
    tester,
  ) async {
    final api = _FakeCalendarApi(
      postError: ApiException(
        409,
        '{"message":"Availability overlaps an existing slot"}',
      ),
    );
    await tester.pumpWidget(MaterialApp(home: ExpertCalendarScreen(api: api)));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('calendar-add-slot')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('availability-save')));
    await tester.pumpAndSettle();

    expect(
      find.text(
        'Khung giờ này trùng với lịch hiện có. Hãy chọn thời gian khác.',
      ),
      findsOneWidget,
    );
  });

  testWidgets('deletes a slot after confirmation and refreshes the list', (
    tester,
  ) async {
    final api = _FakeCalendarApi(
      slots: [
        {
          'availabilityId': 'slot-delete',
          'startAt': DateTime.now()
              .add(const Duration(days: 2))
              .toUtc()
              .toIso8601String(),
          'endAt': DateTime.now()
              .add(const Duration(days: 2, hours: 1))
              .toUtc()
              .toIso8601String(),
          'status': 'AVAILABLE',
        },
      ],
    );
    await tester.pumpWidget(MaterialApp(home: ExpertCalendarScreen(api: api)));
    await tester.pumpAndSettle();

    await tester.tap(find.byTooltip('Xóa khung giờ'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Xóa'));
    await tester.pumpAndSettle();

    expect(api.lastDeletePath, '/api/v1/expert/availability/slot-delete');
    expect(find.text('Chưa có khung giờ rảnh nào'), findsOneWidget);
    expect(find.text('Đã xóa khung giờ rảnh'), findsOneWidget);
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
