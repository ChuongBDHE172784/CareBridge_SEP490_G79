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
}
