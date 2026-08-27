import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/reminder/models/reminder_model.dart';
import 'package:untitled/features/reminder/screens/appointment_calendar_screen.dart';

Reminder _reminder({
  required String id,
  required ReminderType type,
  required String title,
  required DateTime scheduledAt,
  ReminderStatus status = ReminderStatus.pending,
  String? location,
  String? note,
  RecurrenceType recurrenceType = RecurrenceType.none,
}) {
  return Reminder(
    id: id,
    reminderType: type,
    title: title,
    scheduledAt: scheduledAt,
    status: status,
    location: location,
    note: note,
    recurrenceType: recurrenceType,
  );
}

Widget _calendar({
  required Future<List<Reminder>> Function() loader,
  DateTime? initialMonth,
  DateTime Function()? nowProvider,
}) {
  return MaterialApp(
    home: AppointmentCalendarScreen(
      reminderLoader: loader,
      initialMonth: initialMonth ?? DateTime(2026, 8),
      nowProvider: nowProvider ?? () => DateTime(2026, 7, 29, 12),
    ),
  );
}

void main() {
  testWidgets(
    'marks appointment days and ignores other or cancelled reminders',
    (tester) async {
      await tester.pumpWidget(
        _calendar(
          loader: () async => [
            _reminder(
              id: 'appointment-1',
              type: ReminderType.appointment,
              title: 'Khám thai',
              scheduledAt: DateTime(2026, 8, 5, 9),
            ),
            _reminder(
              id: 'vaccination-1',
              type: ReminderType.vaccination,
              title: 'Tiêm phòng',
              scheduledAt: DateTime(2026, 8, 6, 9),
            ),
            _reminder(
              id: 'cancelled-1',
              type: ReminderType.appointment,
              title: 'Lịch đã tắt',
              scheduledAt: DateTime(2026, 8, 7, 9),
              status: ReminderStatus.cancelled,
            ),
          ],
        ),
      );
      await tester.pumpAndSettle();

      expect(
        find.byKey(const Key('appointment-calendar-count-2026-08-05')),
        findsOneWidget,
      );
      expect(
        find.byKey(const Key('appointment-calendar-count-2026-08-06')),
        findsNothing,
      );
      expect(
        find.byKey(const Key('appointment-calendar-count-2026-08-07')),
        findsNothing,
      );
      expect(find.text('1 lịch hẹn trong tháng'), findsOneWidget);
    },
  );

  testWidgets('shows all appointments for a selected date ordered by time', (
    tester,
  ) async {
    await tester.pumpWidget(
      _calendar(
        loader: () async => [
          _reminder(
            id: 'later',
            type: ReminderType.appointment,
            title: 'Khám buổi chiều',
            scheduledAt: DateTime(2026, 8, 5, 15, 30),
            note: 'Mang theo hồ sơ',
          ),
          _reminder(
            id: 'earlier',
            type: ReminderType.appointment,
            title: 'Siêu âm buổi sáng',
            scheduledAt: DateTime(2026, 8, 5, 8),
            location: 'Bệnh viện CareBridge',
            recurrenceType: RecurrenceType.monthly,
          ),
        ],
      ),
    );
    await tester.pumpAndSettle();

    expect(
      find.descendant(
        of: find.byKey(const Key('appointment-calendar-count-2026-08-05')),
        matching: find.text('2'),
      ),
      findsOneWidget,
    );

    await tester.tap(
      find.byKey(const Key('appointment-calendar-day-2026-08-05')),
    );
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('appointment-day-sheet')), findsOneWidget);
    expect(find.text('2 lịch hẹn'), findsOneWidget);
    expect(find.text('Siêu âm buổi sáng'), findsOneWidget);
    expect(find.text('Khám buổi chiều'), findsOneWidget);
    expect(find.text('08:00'), findsOneWidget);
    expect(find.text('15:30'), findsOneWidget);
    expect(find.text('Bệnh viện CareBridge'), findsOneWidget);
    expect(find.text('Mang theo hồ sơ'), findsOneWidget);
    expect(find.text('Hằng tháng'), findsOneWidget);

    final earlyTop = tester
        .getTopLeft(find.byKey(const Key('appointment-day-item-earlier')))
        .dy;
    final laterTop = tester
        .getTopLeft(find.byKey(const Key('appointment-day-item-later')))
        .dy;
    expect(earlyTop, lessThan(laterTop));
  });

  testWidgets('routes an empty future date to add with the selected date', (
    tester,
  ) async {
    Uri? openedUri;
    final router = GoRouter(
      initialLocation: '/',
      routes: [
        GoRoute(
          path: '/',
          builder: (_, _) => AppointmentCalendarScreen(
            reminderLoader: () async => const [],
            initialMonth: DateTime(2026, 8),
            nowProvider: () => DateTime(2026, 7, 29, 12),
          ),
        ),
        GoRoute(
          path: '/reminders/add',
          builder: (_, state) {
            openedUri = state.uri;
            return const Scaffold(body: Text('Tạo lịch hẹn'));
          },
        ),
      ],
    );
    addTearDown(router.dispose);

    tester.view.physicalSize = const Size(800, 1000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();
    await tester.tap(
      find.byKey(const Key('appointment-calendar-day-2026-08-10')),
    );
    await tester.pumpAndSettle();

    expect(find.text('Ngày này chưa có lịch hẹn.'), findsOneWidget);
    await tester.tap(find.byKey(const Key('appointment-day-add')));
    await tester.pumpAndSettle();

    expect(openedUri?.path, '/reminders/add');
    expect(openedUri?.queryParameters['date'], '2026-08-10');
    expect(find.text('Tạo lịch hẹn'), findsOneWidget);
  });

  testWidgets('does not allow creating an appointment on a past date', (
    tester,
  ) async {
    await tester.pumpWidget(
      _calendar(
        loader: () async => const [],
        initialMonth: DateTime(2026, 7),
        nowProvider: () => DateTime(2026, 7, 29, 12),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(
      find.byKey(const Key('appointment-calendar-day-2026-07-10')),
    );
    await tester.pumpAndSettle();

    expect(
      find.byKey(const Key('appointment-day-past-notice')),
      findsOneWidget,
    );
    expect(find.byKey(const Key('appointment-day-add')), findsNothing);
  });

  testWidgets('reloads after detail returns a changed result', (tester) async {
    var loadCount = 0;
    final router = GoRouter(
      initialLocation: '/',
      routes: [
        GoRoute(
          path: '/',
          builder: (_, _) => AppointmentCalendarScreen(
            reminderLoader: () async {
              loadCount++;
              return [
                _reminder(
                  id: 'appointment-1',
                  type: ReminderType.appointment,
                  title: 'Khám thai',
                  scheduledAt: DateTime(2026, 8, 5, 9),
                ),
              ];
            },
            initialMonth: DateTime(2026, 8),
            nowProvider: () => DateTime(2026, 7, 29, 12),
          ),
        ),
        GoRoute(
          path: '/appointments/detail/:id',
          builder: (context, _) => Scaffold(
            body: Center(
              child: FilledButton(
                onPressed: () => context.pop(true),
                child: const Text('Lưu thay đổi'),
              ),
            ),
          ),
        ),
      ],
    );
    addTearDown(router.dispose);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();
    expect(loadCount, 1);

    await tester.tap(
      find.byKey(const Key('appointment-calendar-day-2026-08-05')),
    );
    await tester.pumpAndSettle();
    await tester.tap(
      find.byKey(const Key('appointment-day-open-appointment-1')),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Lưu thay đổi'));
    await tester.pumpAndSettle();

    expect(loadCount, 2);
  });

  testWidgets('shows loading and supports retry after an error', (
    tester,
  ) async {
    final first = Completer<List<Reminder>>();
    var calls = 0;

    await tester.pumpWidget(
      _calendar(
        loader: () {
          calls++;
          if (calls == 1) return first.future;
          return Future.value(const []);
        },
      ),
    );

    expect(
      find.byKey(const Key('appointment-calendar-loading')),
      findsOneWidget,
    );
    first.completeError(Exception('network'));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('appointment-calendar-error')), findsOneWidget);
    await tester.tap(find.byKey(const Key('appointment-calendar-retry')));
    await tester.pumpAndSettle();

    expect(find.text('0 lịch hẹn trong tháng'), findsOneWidget);
    expect(calls, 2);
  });
}
