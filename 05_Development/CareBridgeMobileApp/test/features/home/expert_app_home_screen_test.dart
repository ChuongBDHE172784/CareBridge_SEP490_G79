import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/directChat/models/direct_conversation.dart';
import 'package:untitled/features/directChat/models/expert_directory_item.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';
import 'package:untitled/features/directChat/services/conversation_refresh_bus.dart';
import 'package:untitled/features/home/screens/expert_home_shell.dart';

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
    'EXPERT bottom nav shows unread badge on Trò chuyện and retains direct chat navigation',
    (tester) async {
      original = DirectChatService.instance;
      DirectChatService.instance = _FakeDirectChatService(
        unreadConversationCount: 3,
      );

      await tester.pumpWidget(const MaterialApp(home: ExpertHomeShell()));
      await tester.pumpAndSettle();

      expect(find.text('3'), findsOneWidget);
      expect(find.byType(NavigationBar), findsOneWidget);
      expect(find.text('Yêu cầu tư vấn'), findsNothing);
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
      // The synchronous role/user assignment happens before the first secure-storage await.
      // ignore: unawaited_futures
      AuthState.instance.setTokens(
        accessToken: 'test-access',
        refreshToken: 'test-refresh',
        userId: 'expert-1',
        role: 'EXPERT',
      );

      await tester.pumpWidget(const MaterialApp(home: ExpertHomeShell()));
      await tester.pumpAndSettle();

      for (final label in const ['Tìm Mother', 'Nhắn tin mới']) {
        expect(find.text(label), findsNothing);
      }

      for (final tabLabel in const [
        'Trò chuyện',
        'Lịch',
        'Tài khoản',
        'Tổng quan',
      ]) {
        await tester.tap(find.text(tabLabel).last);
        await tester.pumpAndSettle();
        for (final label in const ['Tìm Mother', 'Nhắn tin mới']) {
          expect(find.text(label), findsNothing);
        }
      }
    },
  );
}
