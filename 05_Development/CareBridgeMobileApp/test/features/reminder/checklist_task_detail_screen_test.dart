import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/directChat/models/timeline_item.dart';
import 'package:untitled/features/directChat/models/timeline_page.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';
import 'package:untitled/features/directChat/widgets/checklist_message_card.dart';
import 'package:untitled/features/reminder/models/reminder_model.dart';
import 'package:untitled/features/reminder/models/today_task_model.dart';
import 'package:untitled/features/reminder/models/today_task_support_function.dart';
import 'package:untitled/features/reminder/screens/checklist_task_detail_screen.dart';
import 'package:untitled/features/reminder/services/reminder_schedule_service.dart';
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
  'AI_TRIAGE': '/rag/chat',
};

TodayTask _task({
  String title = 'Chuẩn bị hồ sơ khám',
  String? description = 'Mang theo kết quả xét nghiệm gần nhất.',
  TodayTaskSupportFunction? supportFunction,
  TodayTaskTarget target = TodayTaskTarget.baby,
  TodayTaskOrigin origin = TodayTaskOrigin.systemTemplate,
  bool completed = false,
  String careContextType = 'BABY',
  String careContextId = 'baby-1',
  String? sourceUrl,
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
    'origin': switch (origin) {
      TodayTaskOrigin.systemTemplate => 'SYSTEM_TEMPLATE',
      TodayTaskOrigin.userCreated => 'USER_CREATED',
      TodayTaskOrigin.unknown => 'UNKNOWN',
    },
    'status': completed ? 'COMPLETED' : 'PENDING',
    'timeBucket': 'TODAY',
    'allowedActions': [action.apiValue],
    'sourceUrl': ?sourceUrl,
  });
}

class _FakeDirectChatService extends DirectChatService {
  final List<TimelineItem> timelineItems;
  String? sentMessageBody;

  _FakeDirectChatService({this.timelineItems = const []});

  @override
  Future<TimelinePage> getTimeline(
    String conversationId, {
    String? after,
    String? before,
    int limit = 30,
  }) async {
    return TimelinePage(
      items: timelineItems,
      hasMoreNewer: false,
      hasMoreOlder: false,
    );
  }

  @override
  Future<TimelineItem> sendMessage(
    String conversationId, {
    required String clientMessageId,
    String? messageBody,
    String messageType = 'TEXT',
    String? attachmentId,
    double? locationLatitude,
    double? locationLongitude,
    String? locationLabel,
  }) async {
    sentMessageBody = messageBody;
    return TimelineItem(
      kind: 'MESSAGE',
      messageId: 'msg-1',
      senderUserId: 'user-1',
      messageType: messageType,
      messageBody: messageBody,
      createdAt: DateTime.now(),
    );
  }
}

Future<void> _openDetail(
  WidgetTester tester, {
  required TodayTask task,
  required TodayTaskService service,
  DirectChatService? directChatService,
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
                    builder: (_) => ChecklistTaskDetailScreen(
                      task: task,
                      service: service,
                      directChatService: directChatService,
                    ),
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
  testWidgets('renders title, detailed content, source, target and status metadata', (
    tester,
  ) async {
    final task = _task();

    await tester.pumpWidget(
      MaterialApp(home: ChecklistTaskDetailScreen(task: task)),
    );

    expect(find.byKey(const Key('task-detail-title')), findsOneWidget);
    expect(find.text('Chuẩn bị hồ sơ khám'), findsOneWidget);
    expect(find.text('Mang theo kết quả xét nghiệm gần nhất.'), findsOneWidget);
    expect(find.text('Gợi ý CareBridge'), findsOneWidget);
    expect(find.text('Bé'), findsOneWidget);
    expect(find.text('Đang chờ'), findsOneWidget);
    expect(find.text('Bé An'), findsOneWidget);
  });

  testWidgets('renders user created task source metadata', (tester) async {
    final task = _task(origin: TodayTaskOrigin.userCreated);

    await tester.pumpWidget(
      MaterialApp(home: ChecklistTaskDetailScreen(task: task)),
    );

    expect(find.text('Việc cá nhân'), findsOneWidget);
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
    expect(find.text('Gợi ý CareBridge'), findsOneWidget);
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
    'expert task COMPLETE updates chat message payload and pops true',
    (tester) async {
      final initialShare = ChecklistShareData(
        title: 'Checklist thai kỳ',
        completedCount: 0,
        totalCount: 1,
        progressPercent: 0,
        currentItems: [
          const ChecklistItemShareData(
            text: 'Đi khám thai lần 2',
            completed: false,
            origin: 'EXPERT',
            createdBy: 'EXPERT',
            isExpertCustom: true,
            replacesText: 'Đi khám thai lần đầu',
            supportFunction: 'APPOINTMENTS',
          ),
        ],
      );

      final fakeChatService = _FakeDirectChatService(
        timelineItems: [
          TimelineItem(
            kind: 'MESSAGE',
            messageId: 'msg-1',
            senderUserId: 'expert-1',
            messageType: 'TEXT',
            messageBody: initialShare.serialize(),
            createdAt: DateTime.now(),
          ),
        ],
      );

      final task = TodayTask(
        id: 'expert-task-conv-123-0',
        kind: TodayTaskKind.checklist,
        sourceType: TodayTaskSourceType.checklist,
        type: ReminderType.other,
        title: 'Đi khám thai lần 2',
        status: ReminderStatus.pending,
        taskStatus: TodayTaskStatus.pending,
        priority: 1,
        target: TodayTaskTarget.mother,
        origin: TodayTaskOrigin.systemTemplate,
        bucket: TodayTimeBucket.today,
        allowedActions: const {TodayTaskAction.complete, TodayTaskAction.reopen},
        supportFunction: TodayTaskSupportFunction.appointments,
      );

      bool? detailResult;
      await _openDetail(
        tester,
        task: task,
        service: TodayTaskService(
          getRequest: (_, {queryParams}) async => const {},
          postRequest: (_, _) async => const {},
        ),
        directChatService: fakeChatService,
        onResult: (result) => detailResult = result,
      );

      expect(find.text('Đánh dấu hoàn tất'), findsOneWidget);
      await tester.tap(find.byKey(const Key('task-detail-status-action')));
      await tester.pumpAndSettle();

      expect(detailResult, isTrue);
      expect(fakeChatService.sentMessageBody, isNotNull);
      final updatedShare = ChecklistShareData.parse(
        fakeChatService.sentMessageBody!,
      );
      expect(updatedShare, isNotNull);
      expect(updatedShare!.currentItems.first.completed, isTrue);
      expect(updatedShare.completedCount, 1);
      expect(updatedShare.progressPercent, 100);
    },
  );

  testWidgets(
    'expert task REOPEN updates chat message payload and pops true',
    (tester) async {
      final initialShare = ChecklistShareData(
        title: 'Checklist thai kỳ',
        completedCount: 1,
        totalCount: 1,
        progressPercent: 100,
        currentItems: [
          const ChecklistItemShareData(
            text: 'Đi khám thai lần 2',
            completed: true,
            origin: 'EXPERT',
            createdBy: 'EXPERT',
            isExpertCustom: true,
            replacesText: 'Đi khám thai lần đầu',
            supportFunction: 'APPOINTMENTS',
          ),
        ],
      );

      final fakeChatService = _FakeDirectChatService(
        timelineItems: [
          TimelineItem(
            kind: 'MESSAGE',
            messageId: 'msg-1',
            senderUserId: 'expert-1',
            messageType: 'TEXT',
            messageBody: initialShare.serialize(),
            createdAt: DateTime.now(),
          ),
        ],
      );

      final task = TodayTask(
        id: 'expert-task-conv-123-0',
        kind: TodayTaskKind.checklist,
        sourceType: TodayTaskSourceType.checklist,
        type: ReminderType.other,
        title: 'Đi khám thai lần 2',
        status: ReminderStatus.pending,
        taskStatus: TodayTaskStatus.completed,
        priority: 1,
        target: TodayTaskTarget.mother,
        origin: TodayTaskOrigin.systemTemplate,
        bucket: TodayTimeBucket.today,
        allowedActions: const {TodayTaskAction.complete, TodayTaskAction.reopen},
        supportFunction: TodayTaskSupportFunction.appointments,
      );

      bool? detailResult;
      await _openDetail(
        tester,
        task: task,
        service: TodayTaskService(
          getRequest: (_, {queryParams}) async => const {},
          postRequest: (_, _) async => const {},
        ),
        directChatService: fakeChatService,
        onResult: (result) => detailResult = result,
      );

      expect(find.text('Mở lại việc'), findsOneWidget);
      await tester.tap(find.byKey(const Key('task-detail-status-action')));
      await tester.pumpAndSettle();

      expect(detailResult, isTrue);
      expect(fakeChatService.sentMessageBody, isNotNull);
      final updatedShare = ChecklistShareData.parse(
        fakeChatService.sentMessageBody!,
      );
      expect(updatedShare, isNotNull);
      expect(updatedShare!.currentItems.first.completed, isFalse);
      expect(updatedShare.completedCount, 0);
      expect(updatedShare.progressPercent, 0);
    },
  );

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

  testWidgets(
    'displays medical reference source url section when sourceUrl is present',
    (tester) async {
      const url = 'https://www.who.int/guidelines/maternal-care';
      final task = _task(sourceUrl: url);

      await tester.pumpWidget(
        MaterialApp(home: ChecklistTaskDetailScreen(task: task)),
      );

      expect(find.text('Nguồn tham khảo y khoa'), findsOneWidget);
      expect(find.byKey(const Key('task-detail-source-url')), findsOneWidget);
      expect(find.text(url), findsOneWidget);
      expect(find.byKey(const Key('task-detail-source-url-button')), findsOneWidget);
    },
  );

  testWidgets(
    'hides medical reference source url section when sourceUrl is null or empty',
    (tester) async {
      final task = _task(sourceUrl: '   ');

      await tester.pumpWidget(
        MaterialApp(home: ChecklistTaskDetailScreen(task: task)),
      );

      expect(find.text('Nguồn tham khảo y khoa'), findsNothing);
      expect(find.byKey(const Key('task-detail-source-url')), findsNothing);
    },
  );

  testWidgets('renders quick reminder action in app bar and in list', (
    tester,
  ) async {
    final task = _task();

    await tester.pumpWidget(
      MaterialApp(home: ChecklistTaskDetailScreen(task: task)),
    );

    expect(
      find.byKey(const Key('task-detail-quick-reminder-action')),
      findsOneWidget,
    );

    final addReminderButton = find.byKey(
      const Key('task-detail-add-quick-reminder-button'),
    );
    await tester.ensureVisible(addReminderButton);
    await tester.pumpAndSettle();

    expect(find.text('Lịch nhắc nhở'), findsOneWidget);
    expect(addReminderButton, findsOneWidget);
  });

  testWidgets(
    'tapping quick reminder opens ReminderScheduleEditor pre-filled with task title',
    (tester) async {
      final task = _task(title: 'Uống vitamin D');

      await tester.pumpWidget(
        MaterialApp(home: ChecklistTaskDetailScreen(task: task)),
      );

      await tester.tap(
        find.byKey(const Key('task-detail-quick-reminder-action')),
      );
      await tester.pumpAndSettle();

      expect(find.text('Tạo lịch nhắc nhanh'), findsOneWidget);
      expect(find.text('Uống vitamin D'), findsWidgets);
      expect(
        find.byKey(const Key('reminder-schedule-title-input')),
        findsOneWidget,
      );
      expect(
        find.byKey(const Key('reminder-schedule-save-button')),
        findsOneWidget,
      );
    },
  );

  testWidgets(
    'saving quick reminder creates schedule via service and displays confirmation',
    (tester) async {
      String? createdPath;
      Map<String, dynamic>? createdBody;

      final reminderScheduleService = ReminderScheduleService(
        postRequest: (path, body) async {
          createdPath = path;
          createdBody = Map<String, dynamic>.from(body);
          return {
            'data': {
              'scheduleId': 'sched-99',
              'title': body['title'],
              'times': body['times'],
              'timeZone': body['timeZone'] ?? 'Asia/Ho_Chi_Minh',
              'recurrence': body['recurrence'] ?? 'DAILY',
              'startDate': '2026-09-08',
              'active': true,
              'revision': 1,
            },
          };
        },
      );

      final task = _task(title: 'Massage cho bé');

      await tester.pumpWidget(
        MaterialApp(
          home: ChecklistTaskDetailScreen(
            task: task,
            reminderScheduleService: reminderScheduleService,
          ),
        ),
      );

      final addReminderButton = find.byKey(
        const Key('task-detail-add-quick-reminder-button'),
      );
      await tester.ensureVisible(addReminderButton);
      await tester.pumpAndSettle();
      await tester.tap(addReminderButton);
      await tester.pumpAndSettle();

      expect(find.text('Tạo lịch nhắc nhanh'), findsOneWidget);

      await tester.tap(find.byKey(const Key('reminder-schedule-save-button')));
      await tester.pumpAndSettle();

      expect(createdPath, '/api/v1/reminder-schedules');
      expect(createdBody?['title'], 'Massage cho bé');
      expect(find.text('Đã tạo lịch nhắc cho việc này.'), findsOneWidget);
      expect(find.text('Xem lịch nhắc'), findsOneWidget);
    },
  );

  testWidgets(
    'can select tomorrow date for one-time reminder and save successfully',
    (tester) async {
      String? savedStartDate;
      final reminderScheduleService = ReminderScheduleService(
        postRequest: (path, body) async {
          savedStartDate = body['startDate'] as String?;
          return {
            'data': {
              'scheduleId': 'sched-100',
              'title': body['title'],
              'times': body['times'],
              'timeZone': 'Asia/Ho_Chi_Minh',
              'recurrence': body['recurrence'],
              'startDate': body['startDate'],
              'active': true,
              'revision': 1,
            },
          };
        },
      );

      final task = _task(title: 'Sàng lọc dị tật bẩm sinh');

      await tester.pumpWidget(
        MaterialApp(
          home: ChecklistTaskDetailScreen(
            task: task,
            reminderScheduleService: reminderScheduleService,
          ),
        ),
      );

      await tester.tap(
        find.byKey(const Key('task-detail-quick-reminder-action')),
      );
      await tester.pumpAndSettle();

      // Switch to "Một lần"
      await tester.tap(
        find.byKey(const Key('reminder-schedule-recurrence-dropdown')),
      );
      await tester.pumpAndSettle();
      await tester.tap(find.text('Một lần').last);
      await tester.pumpAndSettle();

      // Select "Ngày mai"
      await tester.tap(find.byKey(const Key('reminder-schedule-date-tomorrow')));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('reminder-schedule-save-button')));
      await tester.pumpAndSettle();

      final tomorrow = DateTime.now().add(const Duration(days: 1));
      final expectedDate =
          '${tomorrow.year.toString().padLeft(4, '0')}-${tomorrow.month.toString().padLeft(2, '0')}-${tomorrow.day.toString().padLeft(2, '0')}';
      expect(savedStartDate, expectedDate);
      expect(find.text('Đã tạo lịch nhắc cho việc này.'), findsOneWidget);
    },
  );
}


