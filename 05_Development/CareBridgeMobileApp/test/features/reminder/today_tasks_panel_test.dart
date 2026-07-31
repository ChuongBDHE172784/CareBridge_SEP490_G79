import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/network/api_client.dart';
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
  'allowedActions': completed ? <String>[] : ['COMPLETE', 'SKIP'],
  'dueAt': due ? '2026-08-03T08:00:00Z' : null,
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
              'allowedActions': ['COMPLETE', 'SKIP'],
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
      expect(find.byIcon(Icons.radio_button_unchecked_rounded), findsNWidgets(2));
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
        tester.getTopLeft(
          find.descendant(
            of: systemSection,
            matching: find.text('Hệ thống mới hơn'),
          ),
        ).dy,
        lessThan(
          tester.getTopLeft(
            find.descendant(
              of: systemSection,
              matching: find.text('Hệ thống cũ hơn'),
            ),
          ).dy,
        ),
      );

      // Switch to user tasks tab
      await tester.tap(find.byKey(const Key('tab-user-tasks')));
      await tester.pumpAndSettle();

      final userSection = find.byKey(const Key('today-user-tasks'));
      expect(
        tester.getTopLeft(
          find.descendant(
            of: userSection,
            matching: find.text('Tôi thêm có giờ'),
          ),
        ).dy,
        lessThan(
          tester.getTopLeft(
            find.descendant(
              of: userSection,
              matching: find.text('Tôi thêm chưa có giờ'),
            ),
          ).dy,
        ),
      );
    },
  );

  testWidgets('source layout is empty when the response only has appointments', (
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
  });

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

  testWidgets('invokes unified COMPLETE and SKIP actions', (tester) async {
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
          'data': {...body, 'status': currentCompleted ? 'COMPLETED' : 'SKIPPED'},
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
    expect(calls.last['action'], 'SKIP');
    expect(calls.last['reason'], 'USER_CHOICE');
  });
}
