import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/directChat/models/direct_conversation.dart';
import 'package:untitled/features/directChat/screens/conversation_list_screen.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';
import 'package:untitled/features/directChat/services/conversation_refresh_bus.dart';

class _ScriptedDirectChatService extends DirectChatService {
  final List<DirectConversationSummary> conversations;
  _ScriptedDirectChatService(this.conversations);

  @override
  Future<List<DirectConversationSummary>> listMyConversations() async =>
      conversations;
}

class _RacingConversationService extends DirectChatService {
  final List<Completer<List<DirectConversationSummary>>> requests = [];

  @override
  Future<List<DirectConversationSummary>> listMyConversations() {
    final completer = Completer<List<DirectConversationSummary>>();
    requests.add(completer);
    return completer.future;
  }
}

Future<GoRouter> _pumpWithRouter(WidgetTester tester) async {
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
            Scaffold(body: Text('chat:${state.pathParameters['id']}')),
      ),
      GoRoute(
        path: '/experts',
        builder: (_, _) => const Scaffold(body: Text('directory')),
      ),
    ],
  );
  await tester.pumpWidget(MaterialApp.router(routerConfig: router));
  return router;
}

void main() {
  late DirectChatService original;

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
    AuthState.instance.clearState();
    original = DirectChatService.instance;
  });
  tearDown(() {
    AuthState.instance.clearState();
    DirectChatService.instance = original;
  });

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
          lastMessageAt: DateTime.now().toUtc().subtract(
            const Duration(minutes: 5),
          ),
          unreadCount: 2,
        ),
      ]);

      await _pumpWithRouter(tester);
      await tester.pumpAndSettle();

      expect(
        find.text('BS. A'),
        findsOneWidget,
      ); // real name, not hardcoded "Chuyên gia"
      expect(find.text('Chào mẹ, em bé khỏe không?'), findsOneWidget);
      expect(find.text('2'), findsOneWidget); // unread badge
    },
  );

  // MEDI-FL-08 — MOTHER empty state
  testWidgets('MOTHER empty state shows the CTA to find an expert', (
    tester,
  ) async {
    await AuthState.instance.setTokens(
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
    await AuthState.instance.setTokens(
      accessToken: 'a',
      refreshToken: 'r',
      userId: 'expert-1',
      role: 'EXPERT',
    );
    DirectChatService.instance = _ScriptedDirectChatService(const []);

    await _pumpWithRouter(tester);
    await tester.pumpAndSettle();

    expect(find.textContaining('Chưa có mẹ nào nhắn cho bạn'), findsOneWidget);
    expect(find.text('Tìm chuyên gia'), findsNothing);
    expect(find.byType(FilledButton), findsNothing);
  });

  testWidgets(
    'EXPERT row never labels its mother counterpart as an unavailable expert',
    (tester) async {
      await AuthState.instance.setTokens(
        accessToken: 'a',
        refreshToken: 'r',
        userId: 'expert-1',
        role: 'EXPERT',
      );
      DirectChatService.instance = _ScriptedDirectChatService([
        DirectConversationSummary(
          conversationId: 'conv-1',
          counterpartUserId: 'mother-1',
          counterpartRole: 'MOTHER',
          expertAvailable: false,
          counterpartDisplayName: 'Mẹ An',
          lastMessagePreview: 'Em cần tư vấn',
        ),
      ]);
      await _pumpWithRouter(tester);
      await tester.pumpAndSettle();

      expect(find.text('Em cần tư vấn'), findsOneWidget);
      expect(find.text('Chuyên gia hiện không khả dụng'), findsNothing);
    },
  );

  testWidgets('an older inbox response cannot overwrite a newer refresh', (
    tester,
  ) async {
    final service = _RacingConversationService();
    DirectChatService.instance = service;
    await _pumpWithRouter(tester);
    await tester.pump();
    expect(service.requests, hasLength(1));

    ConversationRefreshBus.notify();
    await tester.pump();
    expect(service.requests, hasLength(2));

    service.requests[1].complete([
      DirectConversationSummary(
        conversationId: 'new',
        counterpartUserId: 'expert-2',
        counterpartRole: 'EXPERT',
        expertAvailable: true,
        counterpartDisplayName: 'New inbox',
      ),
    ]);
    await tester.pump();
    service.requests[0].complete([
      DirectConversationSummary(
        conversationId: 'old',
        counterpartUserId: 'expert-1',
        counterpartRole: 'EXPERT',
        expertAvailable: true,
        counterpartDisplayName: 'Old inbox',
      ),
    ]);
    await tester.pumpAndSettle();

    expect(find.text('New inbox'), findsOneWidget);
    expect(find.text('Old inbox'), findsNothing);
  });
}
