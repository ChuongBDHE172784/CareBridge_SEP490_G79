import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/home/screens/mother_home_screen.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/reminder/services/today_task_service.dart';
import 'package:untitled/features/safety/models/safety_config_model.dart';

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
      'correlationId': 'mother-home-safety-test',
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

void main() {
  const disabledConfig = SafetyConfig(
    fallDetectionEnabled: false,
    sensitivityLevel: 'MEDIUM',
    emergencyAutoAlert: true,
  );

  const enabledConfig = SafetyConfig(
    fallDetectionEnabled: true,
    sensitivityLevel: 'MEDIUM',
    emergencyAutoAlert: true,
    sensorPermissionGranted: true,
  );

  testWidgets(
    'shows safety monitoring reminder card below Today Tasks when fall detection is not enabled',
    (tester) async {
      tester.view.physicalSize = const Size(800, 1600);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);

      await tester.pumpWidget(
        MaterialApp(
          home: MotherHomeScreen(
            todayTaskService: _todayTaskService(),
            dashboardLoader: () async => _dashboard(12),
            reminderLoader: () async => const [],
            safetyConfigLoader: () async => disabledConfig,
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Card is displayed
      expect(
        find.byKey(const Key('mother-home-safety-reminder-card')),
        findsOneWidget,
      );
      expect(find.text('Giám sát an toàn'), findsOneWidget);
      expect(find.text('Chưa bật'), findsOneWidget);
      expect(
        find.textContaining('Kích hoạt cảm biến IMU và phát hiện ngã'),
        findsOneWidget,
      );
      expect(
        find.byKey(const Key('mother-home-enable-safety-button')),
        findsOneWidget,
      );
      expect(find.text('Bật giám sát an toàn'), findsOneWidget);

      // Verify position: below Today Tasks ('Việc cần làm')
      final todayTasksTop = tester.getTopLeft(find.text('Việc cần làm')).dy;
      final reminderTop =
          tester.getTopLeft(
            find.byKey(const Key('mother-home-safety-reminder-card')),
          ).dy;
      expect(todayTasksTop, lessThan(reminderTop));
    },
  );

  testWidgets(
    'clicking the enable button navigates to /safety screen',
    (tester) async {
      String? openedRoute;
      final router = GoRouter(
        initialLocation: '/',
        routes: [
          GoRoute(
            path: '/',
            builder: (_, _) => MotherHomeScreen(
              todayTaskService: _todayTaskService(),
              dashboardLoader: () async => _dashboard(12),
              reminderLoader: () async => const [],
              safetyConfigLoader: () async => disabledConfig,
            ),
          ),
          GoRoute(
            path: '/safety',
            builder: (_, state) {
              openedRoute = state.uri.path;
              return const Scaffold(body: Text('Màn hình Giám sát an toàn'));
            },
          ),
        ],
      );
      addTearDown(router.dispose);

      tester.view.physicalSize = const Size(800, 1600);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);

      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      final button = find.byKey(const Key('mother-home-enable-safety-button'));
      expect(button, findsOneWidget);
      await tester.tap(button);
      await tester.pumpAndSettle();

      expect(openedRoute, '/safety');
      expect(find.text('Màn hình Giám sát an toàn'), findsOneWidget);
    },
  );

  testWidgets(
    'clicking the reminder card itself navigates to /safety screen',
    (tester) async {
      String? openedRoute;
      final router = GoRouter(
        initialLocation: '/',
        routes: [
          GoRoute(
            path: '/',
            builder: (_, _) => MotherHomeScreen(
              todayTaskService: _todayTaskService(),
              dashboardLoader: () async => _dashboard(12),
              reminderLoader: () async => const [],
              safetyConfigLoader: () async => disabledConfig,
            ),
          ),
          GoRoute(
            path: '/safety',
            builder: (_, state) {
              openedRoute = state.uri.path;
              return const Scaffold(body: Text('Màn hình Giám sát an toàn'));
            },
          ),
        ],
      );
      addTearDown(router.dispose);

      tester.view.physicalSize = const Size(800, 1600);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);

      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      final card = find.byKey(const Key('mother-home-safety-reminder-card'));
      expect(card, findsOneWidget);
      await tester.tap(card);
      await tester.pumpAndSettle();

      expect(openedRoute, '/safety');
      expect(find.text('Màn hình Giám sát an toàn'), findsOneWidget);
    },
  );

  testWidgets(
    'hides safety monitoring reminder card when fall detection is already enabled',
    (tester) async {
      tester.view.physicalSize = const Size(800, 1600);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);

      await tester.pumpWidget(
        MaterialApp(
          home: MotherHomeScreen(
            todayTaskService: _todayTaskService(),
            dashboardLoader: () async => _dashboard(12),
            reminderLoader: () async => const [],
            safetyConfigLoader: () async => enabledConfig,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(
        find.byKey(const Key('mother-home-safety-reminder-card')),
        findsNothing,
      );
      expect(find.text('Bật giám sát an toàn'), findsNothing);
    },
  );
}
