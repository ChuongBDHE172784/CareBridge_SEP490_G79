import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/directChat/models/direct_conversation.dart';
import 'package:untitled/features/directChat/models/timeline_item.dart';
import 'package:untitled/features/directChat/models/timeline_page.dart';
import 'package:untitled/features/directChat/screens/conversation_list_screen.dart';
import 'package:untitled/features/directChat/screens/direct_chat_screen.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';

const _conversationId = 'conv-1';
const _messageId = 'message-latest';

class _ScriptedDirectChatService extends DirectChatService {
  int markReadCallCount = 0;
  String? lastMarkReadConversationId;
  String? lastMarkReadMessageId;

  @override
  Future<DirectConversation> getConversation(String conversationId) async => DirectConversation(
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
    items: [
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

void main() {
  late DirectChatService original;

  setUp(() => original = DirectChatService.instance);
  tearDown(() => DirectChatService.instance = original);

  // MEDI-FL-10
  testWidgets(
    'opening the chat calls markRead once with the right conversationId; returning to the list reflects unreadCount=0',
    (tester) async {
      final service = _ScriptedDirectChatService();
      DirectChatService.instance = service;

      final router = GoRouter(
        initialLocation: '/conversations',
        routes: [
          GoRoute(path: '/conversations', builder: (_, __) => const ConversationListScreen()),
          GoRoute(
            path: '/direct-chat/:id',
            builder: (_, state) =>
                DirectChatScreen(conversationId: state.pathParameters['id']!),
          ),
        ],
      );
      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      expect(find.text('2'), findsOneWidget); // initial unread badge on the list row

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
}
