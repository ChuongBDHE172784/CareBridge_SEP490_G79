import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/directChat/calls/direct_call_api.dart';
import 'package:untitled/features/directChat/calls/direct_call_coordinator.dart';
import 'package:untitled/features/directChat/calls/direct_call_host.dart';
import 'package:untitled/features/directChat/models/conversation_call.dart';
import 'package:untitled/features/directChat/models/direct_conversation.dart';
import 'package:untitled/features/directChat/models/timeline_item.dart';
import 'package:untitled/features/directChat/models/timeline_page.dart';
import 'package:untitled/features/directChat/models/zego_join_credentials.dart';
import 'package:untitled/features/directChat/screens/conversation_list_screen.dart';
import 'package:untitled/features/directChat/screens/direct_chat_screen.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';
import 'package:untitled/integrations/firebaseRealtime/conversation_event_signal.dart';
import 'package:untitled/features/directChat/widgets/checklist_message_card.dart';
import 'package:untitled/features/directChat/widgets/health_metrics_message_card.dart';
import 'package:untitled/features/directChat/widgets/baby_growth_message_card.dart';

import 'baby_growth_test_factory.dart';

const _conversationId = 'conv-1';
const _messageId = 'message-latest';

class _ScriptedDirectChatService extends DirectChatService {
  _ScriptedDirectChatService({this.timelineItems});

  final List<TimelineItem>? timelineItems;
  int markReadCallCount = 0;
  String? lastMarkReadConversationId;
  String? lastMarkReadMessageId;

  @override
  Future<DirectConversation> getConversation(String conversationId) async =>
      DirectConversation(
        conversationId: conversationId,
        motherUserId: 'mother-1',
        expertUserId: 'expert-1',
        status: 'ACTIVE',
        createdAt: DateTime.utc(2026, 1, 1),
        expertAvailable: true,
      );

  @override
  Future<TimelinePage> getTimeline(
    String conversationId, {
    String? after,
    String? before,
    int limit = 30,
  }) async => TimelinePage(
    items:
        timelineItems ??
        [
          TimelineItem.fromJson({
            'kind': 'MESSAGE',
            'messageId': _messageId,
            'clientMessageId': 'client-1',
            'senderUserId': 'expert-1',
            'messageType': 'TEXT',
            'messageBody': 'Xin chào mẹ',
            'createdAt': '2026-01-01T00:00:00Z',
          }),
        ],
    hasMoreNewer: false,
    hasMoreOlder: false,
  );

  @override
  Future<void> markRead(String conversationId, String lastSeenMessageId) async {
    markReadCallCount++;
    lastMarkReadConversationId = conversationId;
    lastMarkReadMessageId = lastSeenMessageId;
  }

  @override
  Future<List<DirectConversationSummary>> listMyConversations() async => [
    DirectConversationSummary(
      conversationId: _conversationId,
      counterpartUserId: 'expert-1',
      counterpartRole: 'EXPERT',
      expertAvailable: true,
      counterpartDisplayName: 'BS. A',
      // Reflects the server-side effect of the markRead call already made — mirrors what a
      // real reload would return once the cursor has advanced.
      unreadCount: markReadCallCount > 0 ? 0 : 2,
    ),
  ];
}

class _OutgoingCallApi implements DirectCallApiPort {
  int initiateCalls = 0;

  @override
  Future<ConversationCall> initiate(
    String conversationId,
    String callType,
  ) async {
    initiateCalls++;
    return ConversationCall(
      callId: 'call-1',
      conversationId: conversationId,
      initiatedByUserId: 'mother-1',
      callType: callType,
      callStatus: 'INITIATED',
      initiatedAt: DateTime.utc(2026, 8, 19),
    );
  }

  @override
  Future<List<ConversationCall>> listActiveCalls() async => const [];

  @override
  Future<ConversationCall> getCall(String conversationId, String callId) =>
      throw UnimplementedError();

  @override
  Future<ConversationCall> markRinging(String conversationId, String callId) =>
      throw UnimplementedError();

  @override
  Future<ConversationCall> answer(String conversationId, String callId) =>
      throw UnimplementedError();

  @override
  Future<ConversationCall> decline(String conversationId, String callId) =>
      throw UnimplementedError();

  @override
  Future<ConversationCall> end(String conversationId, String callId) =>
      throw UnimplementedError();

  @override
  Future<ZegoJoinCredentials> issueJoinCredentials(
    String conversationId,
    String callId,
  ) => throw UnimplementedError();

  @override
  Future<ConversationCall> uploadRecording({
    required String conversationId,
    required String callId,
    required String filePath,
    int? recordedDurationSeconds,
    bool consentAttested = true,
  }) => throw UnimplementedError();
}

void main() {
  late DirectChatService original;

  setUp(() => original = DirectChatService.instance);
  tearDown(() => DirectChatService.instance = original);

  testWidgets('outgoing call is not initiated before recording consent', (
    tester,
  ) async {
    DirectChatService.instance = _ScriptedDirectChatService();
    final api = _OutgoingCallApi();
    const signals = Stream<ConversationEventSignal>.empty();
    final coordinator = DirectCallCoordinator(
      api: api,
      signals: signals,
      currentUserId: () => 'mother-1',
    );

    await tester.pumpWidget(
      MaterialApp(
        home: DirectCallScope(
          coordinator: coordinator,
          child: const DirectChatScreen(conversationId: _conversationId),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.phone_rounded));
    await tester.pumpAndSettle();
    expect(find.textContaining('Tuân thủ PDPA'), findsOneWidget);
    expect(api.initiateCalls, 0);

    await tester.tap(find.text('Không đồng ý'));
    await tester.pumpAndSettle();
    expect(api.initiateCalls, 0);

    await tester.tap(find.byIcon(Icons.phone_rounded));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Đồng ý'));
    await tester.pumpAndSettle();
    expect(api.initiateCalls, 1);

    unawaited(coordinator.dispose());
  });

  // MEDI-FL-10
  testWidgets(
    'opening the chat calls markRead once with the right conversationId; returning to the list reflects unreadCount=0',
    (tester) async {
      final service = _ScriptedDirectChatService();
      DirectChatService.instance = service;

      final router = GoRouter(
        initialLocation: '/conversations',
        routes: [
          GoRoute(
            path: '/conversations',
            builder: (_, _) => const ConversationListScreen(),
          ),
          GoRoute(
            path: '/direct-chat/:id',
            builder: (_, state) =>
                DirectChatScreen(conversationId: state.pathParameters['id']!),
          ),
        ],
      );
      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      expect(
        find.text('2'),
        findsOneWidget,
      ); // initial unread badge on the list row

      // Tap the row itself (ConversationListScreen._openConversation), not router.push
      // directly — its `await push(...); _load();` is what drives the reload-on-return.
      await tester.tap(find.text('BS. A'));
      await tester.pumpAndSettle();

      expect(service.markReadCallCount, 1);
      expect(service.lastMarkReadConversationId, _conversationId);
      expect(service.lastMarkReadMessageId, _messageId);

      router.pop();
      await tester.pumpAndSettle();

      expect(find.text('2'), findsNothing); // badge cleared after reload
    },
  );

  testWidgets('renders a shared location as a tappable navigation card', (
    tester,
  ) async {
    DirectChatService.instance = _ScriptedDirectChatService(
      timelineItems: [
        TimelineItem.fromJson({
          'kind': 'MESSAGE',
          'messageId': 'location-message',
          'clientMessageId': 'location-client',
          'senderUserId': 'expert-1',
          'messageType': 'LOCATION',
          'locationLatitude': 10.7769,
          'locationLongitude': 106.7009,
          'locationLabel': 'Cổng bệnh viện',
          'createdAt': '2026-08-12T08:00:00Z',
        }),
      ],
    );

    await tester.pumpWidget(
      const MaterialApp(
        home: DirectChatScreen(conversationId: _conversationId),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Cổng bệnh viện'), findsOneWidget);
    expect(find.text('Chạm để dẫn đường'), findsOneWidget);
    expect(find.byIcon(Icons.location_on_rounded), findsOneWidget);
  });

  testWidgets('renders shared health metrics as an interactive health card', (
    tester,
  ) async {
    final healthShare = HealthMetricsShareData(
      title: 'Chỉ số sức khỏe mẹ bầu',
      gestationalWeek: 28,
      measuredDate: '18/08/2026',
      note: 'Huyết áp sáng nay của em',
      metrics: const [
        HealthMetricItemData(
          code: 'BLOOD_PRESSURE',
          name: 'Huyết áp',
          value: '120/80',
          unit: 'mmHg',
          status: 'NORMAL',
        ),
        HealthMetricItemData(
          code: 'BLOOD_GLUCOSE',
          name: 'Đường huyết đói',
          value: '90',
          unit: 'mg/dL',
          status: 'NORMAL',
        ),
      ],
    );

    DirectChatService.instance = _ScriptedDirectChatService(
      timelineItems: [
        TimelineItem.fromJson({
          'kind': 'MESSAGE',
          'messageId': 'health-share-msg',
          'clientMessageId': 'health-share-client',
          'senderUserId': 'mother-1',
          'messageType': 'TEXT',
          'messageBody': healthShare.serialize(),
          'createdAt': '2026-08-18T08:00:00Z',
        }),
      ],
    );

    await tester.pumpWidget(
      const MaterialApp(
        home: DirectChatScreen(conversationId: _conversationId),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Chỉ số sức khỏe mẹ bầu'), findsOneWidget);
    expect(find.text('Huyết áp'), findsOneWidget);
    expect(find.text('Đường huyết đói'), findsOneWidget);
    expect(find.text('Huyết áp sáng nay của em'), findsOneWidget);
    expect(find.byIcon(Icons.monitor_heart_outlined), findsWidgets);
  });

  testWidgets(
    'SBG-TC-012 offers baby growth sharing and renders growth cards for both sides',
    (tester) async {
      final body = makeShareData().serialize();
      DirectChatService.instance = _ScriptedDirectChatService(
        timelineItems: [
          TimelineItem.fromJson({
            'kind': 'MESSAGE',
            'messageId': 'growth-counterpart',
            'clientMessageId': 'growth-counterpart-client',
            'senderUserId': 'expert-1',
            'messageType': 'TEXT',
            'messageBody': body,
            'createdAt': '2026-09-14T08:00:00Z',
          }),
          TimelineItem.fromJson({
            'kind': 'MESSAGE',
            'messageId': 'growth-own',
            'clientMessageId': 'growth-own-client',
            'senderUserId': 'mother-1',
            'messageType': 'TEXT',
            'messageBody': body,
            'createdAt': '2026-09-14T08:01:00Z',
          }),
          TimelineItem.fromJson({
            'kind': 'MESSAGE',
            'messageId': 'growth-recalled',
            'clientMessageId': 'growth-recalled-client',
            'senderUserId': 'mother-1',
            'messageType': 'TEXT',
            'messageBody': body,
            'recalledAt': '2026-09-14T08:03:00Z',
            'createdAt': '2026-09-14T08:02:00Z',
          }),
        ],
      );

      await tester.pumpWidget(
        const MaterialApp(
          home: DirectChatScreen(conversationId: _conversationId),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byType(BabyGrowthMessageCard), findsNWidgets(2));
      expect(
        find.textContaining('[CAREBRIDGE_BABY_GROWTH_SHARE]'),
        findsNothing,
      );

      await tester.tap(find.byTooltip('Đính kèm & Chia sẻ'));
      await tester.pumpAndSettle();

      expect(find.text('Chia sẻ phát triển của bé'), findsOneWidget);
    },
  );

  testWidgets('renders shared checklist as an interactive progress card', (
    tester,
  ) async {
    final checklistShare = ChecklistShareData(
      title: 'Danh sách việc cần làm (Checklist)',
      gestationalWeek: 28,
      completedCount: 3,
      totalCount: 4,
      progressPercent: 75,
      note: 'Các việc em đã hoàn thành',
      items: const [
        ChecklistItemShareData(
          text: 'Khám thai định kỳ tuần 28',
          completed: true,
          category: 'Khám thai',
        ),
        ChecklistItemShareData(
          text: 'Tiêm uốn ván mũi 1',
          completed: false,
          category: 'Tiêm chủng',
        ),
      ],
    );

    DirectChatService.instance = _ScriptedDirectChatService(
      timelineItems: [
        TimelineItem.fromJson({
          'kind': 'MESSAGE',
          'messageId': 'checklist-share-msg',
          'clientMessageId': 'checklist-share-client',
          'senderUserId': 'mother-1',
          'messageType': 'TEXT',
          'messageBody': checklistShare.serialize(),
          'createdAt': '2026-08-18T08:00:00Z',
        }),
      ],
    );

    await tester.pumpWidget(
      const MaterialApp(
        home: DirectChatScreen(conversationId: _conversationId),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Danh sách việc cần làm (Checklist)'), findsOneWidget);
    expect(find.text('3/4 (75%)'), findsOneWidget);
    expect(find.text('Khám thai định kỳ tuần 28'), findsOneWidget);
    expect(find.text('Tiêm uốn ván mũi 1'), findsOneWidget);
    expect(find.text('Các việc em đã hoàn thành'), findsOneWidget);
    expect(find.byIcon(Icons.checklist_rtl_rounded), findsWidgets);
  });

  testWidgets('automatically scrolls down to the newest message upon accessing the chat', (
    tester,
  ) async {
    final manyMessages = List.generate(
      25,
      (i) => TimelineItem.fromJson({
        'kind': 'MESSAGE',
        'messageId': 'msg-$i',
        'clientMessageId': 'client-$i',
        'senderUserId': i.isEven ? 'expert-1' : 'mother-1',
        'messageType': 'TEXT',
        'messageBody': 'Tin nhắn số $i trong cuộc trò chuyện',
        'createdAt': DateTime.utc(2026, 1, 1, 0, i).toIso8601String(),
      }),
    );

    DirectChatService.instance = _ScriptedDirectChatService(
      timelineItems: manyMessages,
    );

    await tester.pumpWidget(
      const MaterialApp(
        home: DirectChatScreen(conversationId: _conversationId),
      ),
    );
    await tester.pumpAndSettle();

    // The newest message (last index) must be visible on screen
    expect(find.text('Tin nhắn số 24 trong cuộc trò chuyện'), findsOneWidget);

    // Oldest message at index 0 should be scrolled off-screen above
    expect(find.text('Tin nhắn số 0 trong cuộc trò chuyện'), findsNothing);
  });
}
