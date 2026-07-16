import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/directChat/models/direct_conversation.dart';
import 'package:untitled/features/directChat/screens/conversation_list_screen.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';

class _ScriptedDirectChatService extends DirectChatService {
  final List<DirectConversationSummary> conversations;
  _ScriptedDirectChatService(this.conversations);

  @override
  Future<List<DirectConversationSummary>> listMyConversations() async => conversations;
}

Future<GoRouter> _pumpWithRouter(WidgetTester tester) async {
  final router = GoRouter(
    initialLocation: '/conversations',
    routes: [
      GoRoute(path: '/conversations', builder: (_, __) => const ConversationListScreen()),
      GoRoute(
        path: '/direct-chat/:id',
        builder: (_, state) => Scaffold(body: Text('chat:${state.pathParameters['id']}')),
      ),
      GoRoute(path: '/experts', builder: (_, __) => const Scaffold(body: Text('directory'))),
    ],
  );
  await tester.pumpWidget(MaterialApp.router(routerConfig: router));
  return router;
}

void main() {
  late DirectChatService original;

  setUp(() => original = DirectChatService.instance);
  tearDown(() => DirectChatService.instance = original);

  // MEDI-FL-07
  testWidgets(
    'row shows the real counterpart name, preview, relative time and unread badge',
    (tester) async {
      DirectChatService.instance = _ScriptedDirectChatService([
        DirectConversationSummary(
          conversationId: 'conv-1',
          counterpartUserId: 'expert-1',
          counterpartRole: 'EXPERT',
          expertAvailable: true,
          counterpartDisplayName: 'BS. A',
          lastMessagePreview: 'Chào mẹ, em bé khỏe không?',
          lastMessageAt: DateTime.now().toUtc().subtract(const Duration(minutes: 5)),
          unreadCount: 2,
        ),
      ]);

      await _pumpWithRouter(tester);
      await tester.pumpAndSettle();

      expect(find.text('BS. A'), findsOneWidget); // real name, not hardcoded "Chuyên gia"
      expect(find.text('Chào mẹ, em bé khỏe không?'), findsOneWidget);
      expect(find.text('2'), findsOneWidget); // unread badge
    },
  );

  // MEDI-FL-08 — MOTHER empty state
  testWidgets('MOTHER empty state shows the CTA to find an expert', (tester) async {
    // ignore: unawaited_futures
    AuthState.instance.setTokens(
      accessToken: 'a',
      refreshToken: 'r',
      userId: 'mother-1',
      role: 'MOTHER',
    );
    DirectChatService.instance = _ScriptedDirectChatService(const []);

    await _pumpWithRouter(tester);
    await tester.pumpAndSettle();

    expect(find.text('Bạn chưa có cuộc trò chuyện nào'), findsOneWidget);
    final cta = find.text('Tìm chuyên gia');
    expect(cta, findsOneWidget);

    await tester.tap(cta);
    await tester.pumpAndSettle();
    expect(find.text('directory'), findsOneWidget);
  });

  // MEDI-FL-08 — EXPERT empty state
  testWidgets('EXPERT empty state has no CTA at all', (tester) async {
    // ignore: unawaited_futures
    AuthState.instance.setTokens(
      accessToken: 'a',
      refreshToken: 'r',
      userId: 'expert-1',
      role: 'EXPERT',
    );
    DirectChatService.instance = _ScriptedDirectChatService(const []);

    await _pumpWithRouter(tester);
    await tester.pumpAndSettle();

    expect(
      find.textContaining('Chưa có mẹ nào nhắn cho bạn'),
      findsOneWidget,
    );
    expect(find.text('Tìm chuyên gia'), findsNothing);
    expect(find.byType(FilledButton), findsNothing);
  });
}
