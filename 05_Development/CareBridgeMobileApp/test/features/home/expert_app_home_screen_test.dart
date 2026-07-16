import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/directChat/models/direct_conversation.dart';
import 'package:untitled/features/directChat/models/expert_directory_item.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';
import 'package:untitled/features/directChat/services/conversation_refresh_bus.dart';
import 'package:untitled/features/home/screens/expert_home_shell.dart';
import 'package:untitled/features/community/screens/expert_question_queue_screen.dart';

class _FakeDirectChatService extends DirectChatService {
  int unreadConversationCount;
  _FakeDirectChatService({this.unreadConversationCount = 0});

  @override
  Future<List<DirectConversationSummary>> listMyConversations() async =>
      const [];

  @override
  Future<UnreadSummary> getUnreadSummary() async => UnreadSummary(
    unreadConversationCount: unreadConversationCount,
    totalUnreadMessageCount: unreadConversationCount,
  );

  @override
  Future<ExpertDirectoryPage> getExpertDirectory({
    String? q,
    String? specialty,
    int page = 0,
    int size = 20,
  }) async => const ExpertDirectoryPage(
    experts: [],
    currentPage: 0,
    pageSize: 20,
    totalElements: 0,
    totalPages: 0,
  );
}

void main() {
  late DirectChatService original;

  tearDown(() {
    DirectChatService.instance = original;
  });

  // MEDI-FL-02
  testWidgets(
    'EXPERT bottom nav shows unread badge on Trò chuyện; Yêu cầu tab actually navigates (no longer a no-op)',
    (tester) async {
      original = DirectChatService.instance;
      DirectChatService.instance = _FakeDirectChatService(
        unreadConversationCount: 3,
      );

      await tester.pumpWidget(const MaterialApp(home: ExpertHomeShell()));
      await tester.pumpAndSettle();

      expect(
        find.text('3'),
        findsOneWidget,
      ); // Badge label on the Trò chuyện destination

      await tester.tap(find.text('Yêu cầu'));
      await tester.pumpAndSettle();

      // ExpertQuestionQueueScreen actually rendered in place — not a no-op empty callback.
      expect(find.byType(ExpertQuestionQueueScreen), findsOneWidget);
      expect(find.text('Yêu cầu tư vấn'), findsOneWidget);
      expect(find.text('Cộng đồng'), findsNothing);
      expect(find.byType(NavigationBar), findsOneWidget);
    },
  );

  testWidgets(
    'foreground conversation events refresh the Expert unread badge',
    (tester) async {
      original = DirectChatService.instance;
      final service = _FakeDirectChatService();
      DirectChatService.instance = service;
      await tester.pumpWidget(const MaterialApp(home: ExpertHomeShell()));
      await tester.pumpAndSettle();

      service.unreadConversationCount = 4;
      ConversationRefreshBus.notify();
      await tester.pumpAndSettle();
      expect(find.text('4'), findsOneWidget);
    },
  );

  // MEDI-FL-11
  testWidgets(
    'EXPERT shell has no "find/message Mother" CTA anywhere across its tabs',
    (tester) async {
      original = DirectChatService.instance;
      DirectChatService.instance = _FakeDirectChatService();
      // Not awaited: setTokens's internal secure-storage write hangs on the unmocked platform
      // channel in this test environment. The synchronous role/userId assignment inside
      // setTokens runs to completion before its first `await`, so this is already visible by
      // the very next line — no pump needed.
      // ignore: unawaited_futures
      AuthState.instance.setTokens(
        accessToken: 'test-access',
        refreshToken: 'test-refresh',
        userId: 'expert-1',
        role: 'EXPERT',
      );

      await tester.pumpWidget(const MaterialApp(home: ExpertHomeShell()));
      await tester.pumpAndSettle();

      for (final label in const [
        'Tìm chuyên gia',
        'Tìm Mother',
        'Nhắn tin mới',
      ]) {
        expect(find.text(label), findsNothing);
      }

      // Visit every tab and re-check — IndexedStack keeps them all built, but assert per tab too.
      for (final tabLabel in const [
        'Trò chuyện',
        'Yêu cầu',
        'Lịch',
        'Tài khoản',
        'Tổng quan',
      ]) {
        await tester.tap(find.text(tabLabel).first);
        await tester.pumpAndSettle();
        for (final label in const [
          'Tìm chuyên gia',
          'Tìm Mother',
          'Nhắn tin mới',
        ]) {
          expect(find.text(label), findsNothing);
        }
      }
    },
  );
}
