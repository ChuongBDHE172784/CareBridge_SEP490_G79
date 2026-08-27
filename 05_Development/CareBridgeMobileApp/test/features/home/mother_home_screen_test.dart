import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/home/screens/mother_home_screen.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/reminder/models/reminder_model.dart';
import 'package:untitled/features/reminder/services/today_task_service.dart';

TodayTaskService _todayTaskService() => TodayTaskService(
  getRequest: (_, {queryParams}) async => {
    'data': {
      'asOf': '2026-07-29T00:00:00Z',
      'zoneId': 'Asia/Ho_Chi_Minh',
      'horizonDays': 7,
      'sections': {
        'overdue': [],
        'today': [],
        'upcoming': [],
        'unscheduled': [],
      },
      'counts': {'overdue': 0, 'today': 0, 'upcoming': 0, 'unscheduled': 0},
      'correlationId': 'mother-home-loading-test',
    },
  },
  postRequest: (_, body) async => {'data': body},
);

JourneyDashboard _dashboard(int week) => JourneyDashboard(
  journeyId: 'journey-$week',
  journeyType: 'PREGNANCY',
  status: 'ACTIVE_PREGNANCY',
  pregnancyWeek: week,
);

Reminder _reminder({
  required String id,
  required ReminderType type,
  required String title,
  required DateTime scheduledAt,
  ReminderStatus status = ReminderStatus.pending,
  String? location,
}) => Reminder(
  id: id,
  reminderType: type,
  title: title,
  scheduledAt: scheduledAt,
  status: status,
  location: location,
);

void main() {
  testWidgets('shows dashboard loading indicator during the initial request', (
    tester,
  ) async {
    final initialRequest = Completer<JourneyDashboard>();

    await tester.pumpWidget(
      MaterialApp(
        home: MotherHomeScreen(
          todayTaskService: _todayTaskService(),
          dashboardLoader: () => initialRequest.future,
          reminderLoader: () async => const [],
        ),
      ),
    );

    expect(
      find.byKey(const Key('mother-home-dashboard-loading')),
      findsOneWidget,
    );
    expect(find.text('Tuần 12'), findsNothing);
  });

  testWidgets('keeps the displayed dashboard visible during a failed refresh', (
    tester,
  ) async {
    final requests = <Completer<JourneyDashboard>>[];

    await tester.pumpWidget(
      MaterialApp(
        home: MotherHomeScreen(
          todayTaskService: _todayTaskService(),
          dashboardLoader: () {
            final request = Completer<JourneyDashboard>();
            requests.add(request);
            return request.future;
          },
          reminderLoader: () async => const [],
        ),
      ),
    );
    requests.single.complete(_dashboard(12));
    await tester.pump();

    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();

    expect(find.text('Tuần 12'), findsOneWidget);
    expect(
      find.byKey(const Key('mother-home-dashboard-loading')),
      findsNothing,
    );

    requests.last.completeError(Exception('Refresh failed'));
    await tester.pump();

    expect(find.text('Tuần 12'), findsOneWidget);
    expect(
      find.byKey(const Key('mother-home-dashboard-loading')),
      findsNothing,
    );
  });

  testWidgets('only applies the latest overlapping dashboard response', (
    tester,
  ) async {
    final requests = <Completer<JourneyDashboard>>[];

    await tester.pumpWidget(
      MaterialApp(
        home: MotherHomeScreen(
          todayTaskService: _todayTaskService(),
          dashboardLoader: () {
            final request = Completer<JourneyDashboard>();
            requests.add(request);
            return request.future;
          },
          reminderLoader: () async => const [],
        ),
      ),
    );
    requests.single.complete(_dashboard(12));
    await tester.pump();

    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();

    requests[2].complete(_dashboard(14));
    await tester.pump();
    requests[1].complete(_dashboard(13));
    await tester.pump();

    expect(find.text('Tuần 14'), findsOneWidget);
    expect(find.text('Tuần 13'), findsNothing);
  });

  testWidgets('shows the pregnancy journey above Today Tasks', (tester) async {
    tester.view.physicalSize = const Size(800, 1600);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);

    await tester.pumpWidget(
      MaterialApp(
        home: MotherHomeScreen(
          todayTaskService: _todayTaskService(),
          dashboardLoader: () async => _dashboard(12),
          reminderLoader: () async => const [],
        ),
      ),
    );
    await tester.pumpAndSettle();

    final journeyTop = tester.getTopLeft(find.text('Hành trình thai kỳ')).dy;
    final todayTasksTop = tester.getTopLeft(find.text('Việc cần làm')).dy;

    expect(journeyTop, lessThan(todayTasksTop));
  });

  testWidgets('shows the nearest pending appointment on Mother Home', (
    tester,
  ) async {
    final reminders = [
      _reminder(
        id: 'vaccination-1',
        type: ReminderType.vaccination,
        title: 'Tiêm phòng cúm',
        scheduledAt: DateTime(2026, 8, 1, 8),
      ),
      _reminder(
        id: 'appointment-later',
        type: ReminderType.appointment,
        title: 'Khám thai lần sau',
        scheduledAt: DateTime(2026, 8, 20, 9),
      ),
      _reminder(
        id: 'appointment-nearest',
        type: ReminderType.appointment,
        title: 'Khám thai định kỳ',
        scheduledAt: DateTime(2026, 8, 5, 9, 30),
        location: 'Bệnh viện CareBridge',
      ),
    ];

    await tester.pumpWidget(
      MaterialApp(
        home: MotherHomeScreen(
          todayTaskService: _todayTaskService(),
          dashboardLoader: () async => _dashboard(12),
          reminderLoader: () async => reminders,
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Lịch hẹn tiếp theo'), findsOneWidget);
    expect(find.text('Khám thai định kỳ'), findsOneWidget);
    expect(find.textContaining('Bệnh viện CareBridge'), findsOneWidget);
    expect(find.text('Tiêm phòng cúm'), findsNothing);
    expect(find.text('Thông tin tiêm phòng'), findsNothing);
  });

  testWidgets('routes an appointment card to the appointment calendar', (
    tester,
  ) async {
    String? openedRoute;
    final router = GoRouter(
      initialLocation: '/',
      routes: [
        GoRoute(
          path: '/',
          builder: (_, _) => MotherHomeScreen(
            todayTaskService: _todayTaskService(),
            dashboardLoader: () async => _dashboard(12),
            reminderLoader: () async => [
              _reminder(
                id: 'appointment-nearest',
                type: ReminderType.appointment,
                title: 'Khám thai định kỳ',
                scheduledAt: DateTime(2026, 8, 5, 9, 30),
              ),
            ],
          ),
        ),
        GoRoute(
          path: '/appointments/calendar',
          builder: (_, state) {
            openedRoute = state.uri.path;
            return const Scaffold(body: Text('Lịch hẹn theo tháng'));
          },
        ),
      ],
    );
    addTearDown(router.dispose);

    tester.view.physicalSize = const Size(800, 1200);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();
    final finder = find.byKey(const Key('mother-home-next-appointment-card'));
    await tester.tap(finder);
    await tester.pumpAndSettle();

    expect(openedRoute, '/appointments/calendar');
    expect(find.text('Lịch hẹn theo tháng'), findsOneWidget);
  });

  testWidgets('routes an empty appointment card to the appointment calendar', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(800, 1200);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);

    String? openedRoute;
    final router = GoRouter(
      initialLocation: '/',
      routes: [
        GoRoute(
          path: '/',
          builder: (_, _) => MotherHomeScreen(
            todayTaskService: _todayTaskService(),
            dashboardLoader: () async => _dashboard(12),
            reminderLoader: () async => [
              _reminder(
                id: 'vaccination-1',
                type: ReminderType.vaccination,
                title: 'Tiêm phòng cúm',
                scheduledAt: DateTime(2026, 8, 1, 8),
              ),
            ],
          ),
        ),
        GoRoute(
          path: '/appointments/calendar',
          builder: (_, state) {
            openedRoute = state.uri.path;
            return const Scaffold(body: Text('Lịch hẹn theo tháng'));
          },
        ),
      ],
    );
    addTearDown(router.dispose);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();

    expect(
      find.text('Chưa có lịch khám sắp tới (Chạm để xem hoặc tạo mới)'),
      findsOneWidget,
    );
    final emptyCardFinder = find.byKey(
      const Key('mother-home-next-appointment-card'),
    );
    await tester.tap(emptyCardFinder);
    await tester.pumpAndSettle();

    expect(openedRoute, '/appointments/calendar');
    expect(find.text('Lịch hẹn theo tháng'), findsOneWidget);
  });

  testWidgets('Mother Home opens the TrackAsia emergency map', (tester) async {
    Uri? openedUri;
    final router = GoRouter(
      initialLocation: '/',
      routes: [
        GoRoute(
          path: '/',
          builder: (_, _) => MotherHomeScreen(
            todayTaskService: _todayTaskService(),
            dashboardLoader: () async => _dashboard(12),
            reminderLoader: () async => const [],
          ),
        ),
        GoRoute(
          path: '/emergency/map',
          builder: (_, state) {
            openedUri = state.uri;
            return const Scaffold(body: Text('TrackAsia Map'));
          },
        ),
      ],
    );
    addTearDown(router.dispose);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();
    final fabFinder = find.byKey(const Key('mother-emergency-map-fab'));
    expect(fabFinder, findsOneWidget);
    expect(tester.getSize(fabFinder), const Size.square(64));
    final fabMaterial = tester.widget<Material>(
      find.descendant(of: fabFinder, matching: find.byType(Material)),
    );
    expect(fabMaterial.shape, isA<CircleBorder>());
    final scaffold = tester.widget<Scaffold>(
      find.ancestor(of: fabFinder, matching: find.byType(Scaffold)).first,
    );
    expect(
      scaffold.floatingActionButtonLocation,
      FloatingActionButtonLocation.endFloat,
    );

    await tester.tap(fabFinder);
    await tester.pumpAndSettle();

    expect(openedUri?.path, '/emergency/map');
    expect(openedUri?.queryParameters['mode'], 'manual');
    expect(openedUri?.queryParameters['stage'], 'PREGNANCY');
    expect(find.text('TrackAsia Map'), findsOneWidget);
  });
}
