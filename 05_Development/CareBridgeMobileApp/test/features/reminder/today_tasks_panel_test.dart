import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/checklist/services/user_checklist_service.dart';
import 'package:untitled/features/reminder/models/today_task_model.dart';
import 'package:untitled/features/reminder/services/today_task_service.dart';
import 'package:untitled/features/reminder/widgets/today_tasks_panel.dart';

Map<String, dynamic> _envelope({bool empty = false, bool completed = false}) =>
    {
      'asOf': '2026-08-03T01:00:00Z',
      'zoneId': 'Asia/Ho_Chi_Minh',
      'horizonDays': 7,
      'sections': {
        'overdue': empty
            ? []
            : [_task('overdue', 'OVERDUE', 'MOTHER', completed: completed)],
        'today': empty
            ? []
            : [_task('today', 'TODAY', 'BABY', completed: completed)],
        'upcoming': empty
            ? []
            : [_task('upcoming', 'UPCOMING', 'MOTHER', completed: completed)],
        'unscheduled': empty
            ? []
            : [
                _task(
                  'unscheduled',
                  'UNSCHEDULED',
                  'BABY',
                  due: false,
                  completed: completed,
                ),
              ],
      },
      'counts': {
        'overdue': empty ? 0 : 1,
        'today': empty ? 0 : 1,
        'upcoming': empty ? 0 : 1,
        'unscheduled': empty ? 0 : 1,
      },
      'correlationId': 'c-1',
    };

Map<String, dynamic> _task(
  String id,
  String bucket,
  String target, {
  bool due = true,
  bool completed = false,
}) => {
  'taskKind': 'CHECKLIST',
  'taskId': id,
  'title': 'Việc $id',
  'careGroupId': 'group-1',
  'careGroupName': 'Gia đình An',
  'careContextType': 'BABY',
  'careContextId': 'baby-1',
  'careContextLabel': 'Bé An',
  'targetSubject': target,
  'origin': 'SYSTEM_TEMPLATE',
  'status': completed ? 'COMPLETED' : 'PENDING',
  'timeBucket': bucket,
  'allowedActions': completed ? <String>['REOPEN'] : ['COMPLETE'],
  'dueAt': due ? '2026-08-03T08:00:00Z' : null,
};

Map<String, dynamic> _singleTaskEnvelope({bool completed = false}) => {
  'asOf': '2026-08-03T01:00:00Z',
  'zoneId': 'Asia/Ho_Chi_Minh',
  'horizonDays': 7,
  'sections': {
    'overdue': <Map<String, dynamic>>[],
    'today': [
      _task('navigation-task', 'TODAY', 'MOTHER', completed: completed),
    ],
    'upcoming': <Map<String, dynamic>>[],
    'unscheduled': <Map<String, dynamic>>[],
  },
  'counts': {'overdue': 0, 'today': 1, 'upcoming': 0, 'unscheduled': 0},
  'correlationId': 'navigation-contract',
};

TodayTaskService _service(Future<dynamic> Function() response) =>
    TodayTaskService(
      getRequest: (_, {queryParams}) => response(),
      postRequest: (_, body) async => {
        'data': {...body, 'status': 'COMPLETED'},
      },
      clientRequestIdFactory: () => 'client-1',
    );

Widget _wrap(Widget child) => MaterialApp(
  home: Scaffold(body: SingleChildScrollView(child: child)),
);

void main() {
  testWidgets(
    'renders deterministic sections, icon+text badges, state and family context',
    (tester) async {
      await tester.pumpWidget(
        _wrap(
          TodayTasksPanel(
            service: _service(() async => {'data': _envelope()}),
            audience: TodayTasksAudience.family,
          ),
        ),
      );
      await tester.pumpAndSettle();

      for (final label in [
        'Quá hạn',
        'Hôm nay',
        '7 ngày tới',
        'Chưa xếp lịch',
      ]) {
        expect(find.text(label), findsOneWidget);
      }
      expect(find.text('Gia đình An'), findsNWidgets(4));
      expect(find.text('Bé An'), findsNWidgets(4));
    },
  );

  testWidgets('shows optional sequence advance CTA and dispatches it', (
    tester,
  ) async {
    var postPath = '';
    final service = TodayTaskService(
      getRequest: (_, {queryParams}) async => {
        'data': {
          ..._envelope(empty: true),
          'sequence': {
            'sequenceState': 'READY_TO_ADVANCE',
            'currentInstanceId': 'instance-1',
            'currentSetName': 'Bộ 1',
            'currentPosition': 1,
            'totalPositions': 2,
            'qualifiedPositions': 1,
            'advanceAvailable': true,
            'nextSet': {'name': 'Bộ 2', 'position': 2},
            'sequenceComplete': false,
          },
        },
      },
      postRequest: (path, body) async {
        postPath = path;
        return {'data': body};
      },
      clientRequestIdFactory: () => '00000000-0000-0000-0000-000000000001',
    );

    await tester.pumpWidget(_wrap(TodayTasksPanel(service: service)));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('sequence-advance-button')), findsOneWidget);
    await tester.tap(find.byKey(const Key('sequence-advance-button')));
    await tester.pumpAndSettle();
    expect(postPath, '/api/v1/checklists/sequences/advance');
  });

  testWidgets('family audience does not offer user-created deletion', (
    tester,
  ) async {
    final envelope = {
      'asOf': '2026-08-03T01:00:00Z',
      'zoneId': 'Asia/Ho_Chi_Minh',
      'horizonDays': 7,
      'sections': {
        'overdue': <Map<String, dynamic>>[],
        'today': [
          {
            'taskKind': 'CHECKLIST',
            'taskId': 'family-user-created',
            'title': 'Family visible personal task',
            'origin': 'USER_CREATED',
            'targetSubject': 'MOTHER',
            'status': 'PENDING',
            'timeBucket': 'TODAY',
            'allowedActions': ['COMPLETE'],
          },
        ],
        'upcoming': <Map<String, dynamic>>[],
        'unscheduled': <Map<String, dynamic>>[],
      },
      'counts': {'overdue': 0, 'today': 1, 'upcoming': 0, 'unscheduled': 0},
      'correlationId': 'family-delete-hidden',
    };

    await tester.pumpWidget(
      _wrap(
        TodayTasksPanel(
          service: _service(() async => {'data': envelope}),
          audience: TodayTasksAudience.family,
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Family visible personal task'), findsOneWidget);
    expect(
      find.byKey(const Key('delete-task-family-user-created')),
      findsNothing,
    );
  });

  testWidgets('shows loading then accessible empty state', (tester) async {
    final pending = Completer<dynamic>();
    await tester.pumpWidget(
      _wrap(TodayTasksPanel(service: _service(() => pending.future))),
    );
    expect(find.byKey(const Key('today-loading')), findsOneWidget);

    pending.complete({'data': _envelope(empty: true)});
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('today-empty')), findsOneWidget);
    expect(find.text('Không có việc nào trong 7 ngày tới.'), findsOneWidget);
  });

  testWidgets(
    'renders direct-context mandatory and user-created tasks together',
    (tester) async {
      final envelope = {
        'asOf': '2026-08-03T01:00:00Z',
        'zoneId': 'Asia/Ho_Chi_Minh',
        'horizonDays': 7,
        'sections': {
          'overdue': <Map<String, dynamic>>[],
          'today': <Map<String, dynamic>>[],
          'upcoming': <Map<String, dynamic>>[],
          'unscheduled': [
            {
              'taskKind': 'CHECKLIST',
              'taskId': 'mandatory-direct',
              'instanceId': 'mandatory-instance',
              'templateVersionId': 'mandatory-version',
              'careGroupId': null,
              'careContextType': 'JOURNEY',
              'careContextId': 'journey-1',
              'title': 'Admin mandatory task',
              'targetSubject': 'MOTHER',
              'origin': 'SYSTEM_TEMPLATE',
              'status': 'PENDING',
              'timeBucket': 'UNSCHEDULED',
              'allowedActions': ['COMPLETE'],
              'dueAt': null,
            },
            {
              'taskKind': 'CHECKLIST',
              'taskId': 'user-created',
              'instanceId': 'user-instance',
              'careGroupId': 'group-1',
              'careContextType': 'JOURNEY',
              'careContextId': 'journey-1',
              'careGroupLabel': 'Gia đình An',
              'careContextLabel': 'Mang thai',
              'title': 'User-added task',
              'targetSubject': 'MOTHER',
              'origin': 'USER_CREATED',
              'status': 'PENDING',
              'timeBucket': 'UNSCHEDULED',
              'allowedActions': ['COMPLETE'],
              'dueAt': null,
            },
          ],
        },
        'counts': {'overdue': 0, 'today': 0, 'upcoming': 0, 'unscheduled': 2},
        'correlationId': 'mixed-direct-context',
      };

      await tester.pumpWidget(
        _wrap(
          TodayTasksPanel(service: _service(() async => {'data': envelope})),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Admin mandatory task'), findsOneWidget);
      expect(find.text('User-added task'), findsOneWidget);
      expect(
        find.byIcon(Icons.radio_button_unchecked_rounded),
        findsNWidgets(2),
      );
    },
  );

  testWidgets(
    'source layout splits origins, hides appointments and sorts newest first',
    (tester) async {
      Map<String, dynamic> task({
        required String id,
        required String title,
        required String origin,
        String? dueAt,
        String taskKind = 'CHECKLIST',
        String? type,
      }) => {
        'taskKind': taskKind,
        'taskId': id,
        'title': title,
        'origin': origin,
        'targetSubject': 'MOTHER',
        'status': 'PENDING',
        'timeBucket': dueAt == null ? 'UNSCHEDULED' : 'TODAY',
        'allowedActions': ['COMPLETE'],
        'dueAt': dueAt,
        'type': ?type,
      };
      final envelope = {
        'asOf': '2026-08-03T01:00:00Z',
        'zoneId': 'Asia/Ho_Chi_Minh',
        'horizonDays': 7,
        'sections': {
          'overdue': [
            task(
              id: 'system-older',
              title: 'Hệ thống cũ hơn',
              origin: 'SYSTEM_TEMPLATE',
              dueAt: '2026-08-02T08:00:00Z',
            ),
          ],
          'today': [
            task(
              id: 'appointment',
              title: 'Lịch khám cần ẩn',
              origin: 'USER_CREATED',
              dueAt: '2026-08-03T10:00:00Z',
              taskKind: 'REMINDER',
              type: 'APPOINTMENT',
            ),
            task(
              id: 'system-newer',
              title: 'Hệ thống mới hơn',
              origin: 'SYSTEM_TEMPLATE',
              dueAt: '2026-08-03T09:00:00Z',
            ),
            task(
              id: 'user-timed',
              title: 'Tôi thêm có giờ',
              origin: 'USER_CREATED',
              dueAt: '2026-08-03T07:00:00Z',
            ),
          ],
          'upcoming': <Map<String, dynamic>>[],
          'unscheduled': [
            task(
              id: 'user-unscheduled',
              title: 'Tôi thêm chưa có giờ',
              origin: 'USER_CREATED',
            ),
          ],
        },
        'correlationId': 'source-layout',
      };

      await tester.pumpWidget(
        _wrap(
          TodayTasksPanel(
            service: _service(() async => {'data': envelope}),
            layout: TodayTasksLayout.sourceGroups,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Gợi ý CareBridge'), findsOneWidget);
      expect(find.text('Việc cá nhân'), findsOneWidget);
      expect(find.text('Lịch khám cần ẩn'), findsNothing);
      expect(find.text('Quá hạn'), findsNothing);
      expect(find.text('Hôm nay'), findsNothing);

      final systemSection = find.byKey(const Key('today-system-tasks'));
      expect(
        tester
            .getTopLeft(
              find.descendant(
                of: systemSection,
                matching: find.text('Hệ thống mới hơn'),
              ),
            )
            .dy,
        lessThan(
          tester
              .getTopLeft(
                find.descendant(
                  of: systemSection,
                  matching: find.text('Hệ thống cũ hơn'),
                ),
              )
              .dy,
        ),
      );

      // Switch to user tasks tab
      await tester.tap(find.byKey(const Key('tab-user-tasks')));
      await tester.pumpAndSettle();

      final userSection = find.byKey(const Key('today-user-tasks'));
      expect(
        tester
            .getTopLeft(
              find.descendant(
                of: userSection,
                matching: find.text('Tôi thêm có giờ'),
              ),
            )
            .dy,
        lessThan(
          tester
              .getTopLeft(
                find.descendant(
                  of: userSection,
                  matching: find.text('Tôi thêm chưa có giờ'),
                ),
              )
              .dy,
        ),
      );
    },
  );

  testWidgets(
    'source layout is empty when the response only has appointments',
    (tester) async {
      final envelope = {
        'asOf': '2026-08-03T01:00:00Z',
        'zoneId': 'Asia/Ho_Chi_Minh',
        'horizonDays': 7,
        'sections': {
          'overdue': <Map<String, dynamic>>[],
          'today': [
            {
              'taskKind': 'REMINDER',
              'taskId': 'appointment-only',
              'type': 'APPOINTMENT',
              'title': 'Lịch khám duy nhất',
              'origin': 'USER_CREATED',
              'targetSubject': 'MOTHER',
              'status': 'PENDING',
              'timeBucket': 'TODAY',
              'allowedActions': ['COMPLETE'],
              'dueAt': '2026-08-03T09:00:00Z',
            },
          ],
          'upcoming': <Map<String, dynamic>>[],
          'unscheduled': <Map<String, dynamic>>[],
        },
        'correlationId': 'appointment-only',
      };

      await tester.pumpWidget(
        _wrap(
          TodayTasksPanel(
            service: _service(() async => {'data': envelope}),
            layout: TodayTasksLayout.sourceGroups,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('today-empty')), findsOneWidget);
      expect(find.text('Lịch khám duy nhất'), findsNothing);
    },
  );

  testWidgets('an older pending load cannot overwrite a newer completed load', (
    tester,
  ) async {
    final first = Completer<dynamic>();
    final second = Completer<dynamic>();
    var request = 0;
    final controller = TodayTasksPanelController();
    await tester.pumpWidget(
      _wrap(
        TodayTasksPanel(
          controller: controller,
          service: _service(
            () => request++ == 0 ? first.future : second.future,
          ),
        ),
      ),
    );

    final refresh = controller.refresh();
    await tester.pump();
    second.complete({'data': _envelope(completed: true)});
    await refresh;
    await tester.pump();
    expect(find.byIcon(Icons.check_circle_rounded), findsNWidgets(4));

    first.complete({'data': _envelope()});
    await tester.pumpAndSettle();
    expect(find.byIcon(Icons.check_circle_rounded), findsNWidgets(4));
  });

  testWidgets('distinguishes retryable, terminal and offline-safe states', (
    tester,
  ) async {
    var attempt = 0;
    await tester.pumpWidget(
      _wrap(
        TodayTasksPanel(
          service: _service(() async {
            attempt++;
            if (attempt == 1) throw ApiException(503, 'unavailable');
            return {'data': _envelope(empty: true)};
          }),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('today-retryable-error')), findsOneWidget);
    await tester.tap(find.text('Thử lại'));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('today-empty')), findsOneWidget);

    await tester.pumpWidget(
      _wrap(
        TodayTasksPanel(
          service: _service(() async => throw ApiException(403, 'forbidden')),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('today-terminal-error')), findsOneWidget);
    expect(find.text('Thử lại'), findsNothing);

    await tester.pumpWidget(
      _wrap(
        TodayTasksPanel(
          service: _service(() async => throw const SocketException('offline')),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('today-offline')), findsOneWidget);
    expect(
      find.text('Bạn đang ngoại tuyến. Dữ liệu sẽ được tải lại khi có mạng.'),
      findsOneWidget,
    );
  });

  testWidgets('invokes unified COMPLETE and REOPEN actions', (tester) async {
    final calls = <Map<String, dynamic>>[];
    var currentCompleted = false;
    final service = TodayTaskService(
      getRequest: (_, {queryParams}) async => {
        'data': _envelope(completed: currentCompleted),
      },
      postRequest: (_, body) async {
        calls.add(body);
        currentCompleted = body['action'] == 'COMPLETE';
        return {
          'data': {
            ...body,
            'status': currentCompleted ? 'COMPLETED' : 'PENDING',
          },
        };
      },
      clientRequestIdFactory: () => 'client-1',
    );
    await tester.pumpWidget(_wrap(TodayTasksPanel(service: service)));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.radio_button_unchecked_rounded).first);
    await tester.pumpAndSettle();
    expect(calls.single['action'], 'COMPLETE');

    await tester.tap(find.byIcon(Icons.check_circle_rounded).first);
    await tester.pumpAndSettle();
    expect(calls.last['action'], 'REOPEN');
    expect(calls.last['reason'], isNull);
  });

  testWidgets(
    'tapping a task card opens detail with TodayTask in state.extra',
    (tester) async {
      var getCount = 0;
      var postCount = 0;
      TodayTask? receivedTask;
      final service = TodayTaskService(
        getRequest: (_, {queryParams}) async {
          getCount++;
          return {'data': _singleTaskEnvelope()};
        },
        postRequest: (_, body) async {
          postCount++;
          return {'data': body};
        },
      );
      final router = GoRouter(
        initialLocation: '/',
        routes: [
          GoRoute(
            path: '/',
            builder: (_, _) => Scaffold(
              body: SingleChildScrollView(
                child: TodayTasksPanel(service: service),
              ),
            ),
          ),
          GoRoute(
            path: '/checklists/task-detail',
            builder: (_, state) {
              final extra = state.extra;
              if (extra is TodayTask) receivedTask = extra;
              return const Scaffold(body: Text('Chi tiết đã mở'));
            },
          ),
        ],
      );
      addTearDown(router.dispose);

      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('task-item-navigation-task')));
      await tester.pumpAndSettle();

      expect(find.text('Chi tiết đã mở'), findsOneWidget);
      expect(receivedTask, isA<TodayTask>());
      expect(receivedTask?.id, 'navigation-task');
      expect(postCount, 0);

      router.pop(true);
      await tester.pumpAndSettle();
      expect(getCount, 2);
    },
  );

  testWidgets(
    'the separate 48dp status control acts without opening task detail',
    (tester) async {
      var getCount = 0;
      Map<String, dynamic>? postBody;
      final service = TodayTaskService(
        getRequest: (_, {queryParams}) async {
          getCount++;
          return {'data': _singleTaskEnvelope()};
        },
        postRequest: (_, body) async {
          postBody = Map<String, dynamic>.from(body);
          return {
            'data': {...body, 'status': 'COMPLETED'},
          };
        },
      );
      final router = GoRouter(
        initialLocation: '/',
        routes: [
          GoRoute(
            path: '/',
            builder: (_, _) => Scaffold(
              body: SingleChildScrollView(
                child: TodayTasksPanel(service: service),
              ),
            ),
          ),
          GoRoute(
            path: '/checklists/task-detail',
            builder: (_, _) => const Scaffold(body: Text('Chi tiết đã mở')),
          ),
        ],
      );
      addTearDown(router.dispose);

      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      final statusControl = find.byKey(
        const Key('task-status-navigation-task'),
      );
      final size = tester.getSize(statusControl);
      expect(size.width, greaterThanOrEqualTo(48));
      expect(size.height, greaterThanOrEqualTo(48));

      await tester.tap(statusControl);
      await tester.pumpAndSettle();

      expect(postBody?['action'], 'COMPLETE');
      expect(postBody?['clientRequestId'], allOf(isA<String>(), isNotEmpty));
      expect(getCount, 2);
      expect(router.routeInformationProvider.value.uri.toString(), '/');
      expect(find.text('Chi tiết đã mở'), findsNothing);
    },
  );

  testWidgets(
    'skip-only reminder does not expose a misleading completion control',
    (tester) async {
      final envelope = _singleTaskEnvelope();
      final sections = Map<String, dynamic>.from(
        envelope['sections'] as Map<String, dynamic>,
      );
      final skipOnlyTask = _task('skip-only-task', 'TODAY', 'MOTHER')
        ..['taskKind'] = 'REMINDER'
        ..['allowedActions'] = ['SKIP'];
      sections['today'] = [skipOnlyTask];
      envelope['sections'] = sections;
      var postCount = 0;
      final service = TodayTaskService(
        getRequest: (_, {queryParams}) async => {'data': envelope},
        postRequest: (_, body) async {
          postCount++;
          return {'data': body};
        },
      );

      await tester.pumpWidget(_wrap(TodayTasksPanel(service: service)));
      await tester.pumpAndSettle();

      expect(find.text('Việc skip-only-task'), findsOneWidget);
      expect(find.byKey(const Key('task-status-skip-only-task')), findsNothing);
      expect(find.byTooltip('Đánh dấu hoàn tất'), findsNothing);
      expect(find.byTooltip('Bỏ qua việc'), findsNothing);
      expect(postCount, 0);
    },
  );

  testWidgets('offers deletion only for user-created checklist tasks', (
    tester,
  ) async {
    var deleted = false;
    var getCount = 0;
    String? deletePath;
    final todayService = TodayTaskService(
      getRequest: (_, {queryParams}) async {
        getCount++;
        return {
          'data': {
            'asOf': '2026-08-03T01:00:00Z',
            'zoneId': 'Asia/Ho_Chi_Minh',
            'horizonDays': 7,
            'sections': {
              'overdue': <Map<String, dynamic>>[],
              'today': deleted
                  ? <Map<String, dynamic>>[]
                  : <Map<String, dynamic>>[
                      {
                        'taskKind': 'CHECKLIST',
                        'taskId': 'user-delete-task',
                        'title': 'User-added task',
                        'origin': 'USER_CREATED',
                        'targetSubject': 'MOTHER',
                        'status': 'PENDING',
                        'timeBucket': 'TODAY',
                        'allowedActions': ['COMPLETE'],
                      },
                    ],
              'upcoming': <Map<String, dynamic>>[],
              'unscheduled': <Map<String, dynamic>>[],
            },
            'counts': {
              'overdue': 0,
              'today': deleted ? 0 : 1,
              'upcoming': 0,
              'unscheduled': 0,
            },
            'correlationId': 'delete-test',
          },
        };
      },
      postRequest: (_, body) async => {'data': body},
    );
    final checklistService = UserChecklistService(
      deleteRequest: (path) async {
        deletePath = path;
        deleted = true;
        return const {};
      },
    );

    await tester.pumpWidget(
      _wrap(
        TodayTasksPanel(
          service: todayService,
          checklistService: checklistService,
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(
      find.byKey(const Key('delete-task-user-delete-task')),
      findsOneWidget,
    );
    await tester.tap(find.byKey(const Key('delete-task-user-delete-task')));
    await tester.pumpAndSettle();

    expect(deletePath, '/api/v1/user-checklist-items/user-delete-task');
    expect(getCount, 2);
    expect(find.text('User-added task'), findsNothing);
  });
}
