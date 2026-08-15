import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/reminder/models/today_task_model.dart';
import 'package:untitled/features/reminder/models/today_task_support_function.dart';
import 'package:untitled/features/reminder/screens/checklist_task_detail_screen.dart';
import 'package:untitled/features/reminder/services/today_task_service.dart';

const _supportRoutes = <String, String>{
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

TodayTask _task({
  String title = 'Chuẩn bị hồ sơ khám',
  String? description = 'Mang theo kết quả xét nghiệm gần nhất.',
  TodayTaskSupportFunction? supportFunction,
  TodayTaskTarget target = TodayTaskTarget.baby,
  bool completed = false,
  String careContextType = 'BABY',
  String careContextId = 'baby-1',
}) {
  final action = completed ? TodayTaskAction.reopen : TodayTaskAction.complete;
  return TodayTask.fromJson({
    'taskKind': 'CHECKLIST',
    'taskId': completed ? 'detail-completed' : 'detail-pending',
    'title': title,
    'description': ?description,
    if (supportFunction != null) 'supportFunction': supportFunction.apiValue,
    'careGroupId': 'group-1',
    'careContextType': careContextType,
    'careContextId': careContextId,
    'careContextLabel': 'Bé An',
    'targetSubject': switch (target) {
      TodayTaskTarget.mother => 'MOTHER',
      TodayTaskTarget.baby => 'BABY',
      TodayTaskTarget.unknown => 'UNKNOWN',
    },
    'origin': 'SYSTEM_TEMPLATE',
    'status': completed ? 'COMPLETED' : 'PENDING',
    'timeBucket': 'TODAY',
    'allowedActions': [action.apiValue],
  });
}

Future<void> _openDetail(
  WidgetTester tester, {
  required TodayTask task,
  required TodayTaskService service,
  required ValueChanged<bool?> onResult,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      home: Builder(
        builder: (context) => Scaffold(
          body: Center(
            child: FilledButton(
              key: const Key('open-task-detail'),
              onPressed: () async {
                final result = await Navigator.of(context).push<bool>(
                  MaterialPageRoute(
                    builder: (_) =>
                        ChecklistTaskDetailScreen(task: task, service: service),
                  ),
                );
                onResult(result);
              },
              child: const Text('Mở chi tiết'),
            ),
          ),
        ),
      ),
    ),
  );
  await tester.tap(find.byKey(const Key('open-task-detail')));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('renders title, detailed content, target and status metadata', (
    tester,
  ) async {
    final task = _task();

    await tester.pumpWidget(
      MaterialApp(home: ChecklistTaskDetailScreen(task: task)),
    );

    expect(find.byKey(const Key('task-detail-title')), findsOneWidget);
    expect(find.text('Chuẩn bị hồ sơ khám'), findsOneWidget);
    expect(find.text('Mang theo kết quả xét nghiệm gần nhất.'), findsOneWidget);
    expect(find.text('Bé'), findsOneWidget);
    expect(find.text('Đang chờ'), findsOneWidget);
    expect(find.text('Bé An'), findsOneWidget);
  });

  testWidgets('renders targetless V2 checklist as a neutral recommendation', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: ChecklistTaskDetailScreen(
          task: _task(target: TodayTaskTarget.unknown),
        ),
      ),
    );

    expect(find.text('Khuyến nghị'), findsOneWidget);
    expect(find.text('Cá nhân'), findsNothing);
  });

  testWidgets(
    'shows the description fallback and omits an absent support CTA',
    (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: ChecklistTaskDetailScreen(task: _task(description: '   ')),
        ),
      );

      expect(
        find.text('Chưa có nội dung chi tiết cho việc này.'),
        findsOneWidget,
      );
      expect(
        find.byKey(const Key('task-support-function-button')),
        findsNothing,
      );
      expect(find.text('Mở chức năng hỗ trợ'), findsNothing);
    },
  );

  for (final entry in _supportRoutes.entries) {
    testWidgets('${entry.key} support CTA opens ${entry.value}', (
      tester,
    ) async {
      final supportFunction = TodayTaskSupportFunction.fromApi(entry.key)!;
      final destinationUri = Uri.parse(entry.value);
      String? openedUri;
      final router = GoRouter(
        initialLocation: '/detail',
        routes: [
          GoRoute(
            path: '/detail',
            builder: (_, _) => ChecklistTaskDetailScreen(
              task: _task(supportFunction: supportFunction),
            ),
          ),
          GoRoute(
            path: destinationUri.path,
            builder: (_, state) {
              openedUri = state.uri.toString();
              return const Scaffold(body: Text('Chức năng đích'));
            },
          ),
        ],
      );
      addTearDown(router.dispose);

      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      expect(
        find.byKey(const Key('task-support-function-label')),
        findsOneWidget,
      );
      expect(find.text(supportFunction.label), findsOneWidget);
      final supportButton = find.byKey(
        const Key('task-support-function-button'),
      );
      await tester.ensureVisible(supportButton);
      await tester.pumpAndSettle();
      await tester.tap(supportButton);
      await tester.pumpAndSettle();

      expect(openedUri, entry.value);
      expect(find.text('Chức năng đích'), findsOneWidget);
    });
  }

  testWidgets('maternal health metrics support opens the journey trend', (
    tester,
  ) async {
    final supportFunction = TodayTaskSupportFunction.fromApi(
      'MATERNAL_HEALTH_METRICS',
    )!;
    String? openedUri;
    final router = GoRouter(
      initialLocation: '/detail',
      routes: [
        GoRoute(
          path: '/detail',
          builder: (_, _) => ChecklistTaskDetailScreen(
            task: _task(
              supportFunction: supportFunction,
              careContextType: 'JOURNEY',
              careContextId: 'journey-1',
            ),
          ),
        ),
        GoRoute(
          path: '/journeys/:journeyId/metrics/trend',
          builder: (_, state) {
            openedUri = state.uri.toString();
            return const Scaffold(body: Text('Chỉ số sức khỏe'));
          },
        ),
      ],
    );
    addTearDown(router.dispose);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();
    final supportButton = find.byKey(const Key('task-support-function-button'));
    await tester.ensureVisible(supportButton);
    await tester.pumpAndSettle();
    await tester.tap(supportButton);
    await tester.pumpAndSettle();

    expect(openedUri, '/journeys/journey-1/metrics/trend?metricType=BMI');
    expect(find.text('Chỉ số sức khỏe'), findsOneWidget);
  });

  testWidgets(
    'maternal health metrics without a journey stays on task detail',
    (tester) async {
      final supportFunction = TodayTaskSupportFunction.fromApi(
        'MATERNAL_HEALTH_METRICS',
      )!;
      final router = GoRouter(
        initialLocation: '/detail',
        routes: [
          GoRoute(
            path: '/detail',
            builder: (_, _) => ChecklistTaskDetailScreen(
              task: _task(
                supportFunction: supportFunction,
                careContextType: 'BABY',
                careContextId: 'baby-1',
              ),
            ),
          ),
        ],
      );
      addTearDown(router.dispose);

      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();
      final supportButton = find.byKey(
        const Key('task-support-function-button'),
      );
      await tester.ensureVisible(supportButton);
      await tester.pumpAndSettle();
      await tester.tap(supportButton);
      await tester.pump();

      expect(
        find.text('Chưa có hành trình để mở chỉ số sức khỏe.'),
        findsOneWidget,
      );
      expect(
        find.byKey(const Key('task-support-function-button')),
        findsOneWidget,
      );
    },
  );

  for (final scenario
      in <({bool completed, TodayTaskAction action, String label})>[
        (
          completed: false,
          action: TodayTaskAction.complete,
          label: 'Đánh dấu hoàn tất',
        ),
        (completed: true, action: TodayTaskAction.reopen, label: 'Mở lại việc'),
      ]) {
    testWidgets(
      '${scenario.action.apiValue} calls TodayTaskService and pops true',
      (tester) async {
        String? postPath;
        Map<String, dynamic>? postBody;
        bool? detailResult;
        final service = TodayTaskService(
          getRequest: (_, {queryParams}) async => const {},
          postRequest: (path, body) async {
            postPath = path;
            postBody = Map<String, dynamic>.from(body);
            return {
              'data': {
                ...body,
                'status': scenario.completed ? 'PENDING' : 'COMPLETED',
              },
            };
          },
          clientRequestIdFactory: () => 'detail-client-request',
        );

        await _openDetail(
          tester,
          task: _task(completed: scenario.completed),
          service: service,
          onResult: (result) => detailResult = result,
        );

        expect(find.text(scenario.label), findsOneWidget);
        await tester.tap(find.byKey(const Key('task-detail-status-action')));
        await tester.pumpAndSettle();

        expect(
          postPath,
          '/api/v1/care-groups/group-1/checklists/tasks/'
          '${scenario.completed ? 'detail-completed' : 'detail-pending'}/actions',
        );
        expect(postBody, {
          'action': scenario.action.apiValue,
          'clientRequestId': 'detail-client-request',
        });
        expect(detailResult, isTrue);
      },
    );
  }

  testWidgets(
    'hides support function section when showSupportFunction is false',
    (tester) async {
      final supportFunction = TodayTaskSupportFunction.fromApi(
        'MATERNAL_EXERCISES',
      )!;
      await tester.pumpWidget(
        MaterialApp(
          home: ChecklistTaskDetailScreen(
            task: _task(supportFunction: supportFunction),
            showSupportFunction: false,
          ),
        ),
      );

      expect(
        find.byKey(const Key('task-support-function-label')),
        findsNothing,
      );
      expect(
        find.byKey(const Key('task-support-function-button')),
        findsNothing,
      );
      expect(find.text('Chức năng hỗ trợ'), findsNothing);
      expect(find.text('Mở chức năng hỗ trợ'), findsNothing);
    },
  );
}
