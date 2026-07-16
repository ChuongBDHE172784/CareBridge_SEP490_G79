import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/models/direct_conversation.dart';
import 'package:untitled/features/directChat/models/expert_directory_item.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';
import 'package:untitled/features/directChat/services/conversation_refresh_bus.dart';
import 'package:untitled/features/home/screens/home_shell.dart';

class _FakeDirectChatService extends DirectChatService {
  int unreadConversationCount = 0;
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

  setUp(() {
    original = DirectChatService.instance;
    DirectChatService.instance = _FakeDirectChatService();
  });

  tearDown(() {
    DirectChatService.instance = original;
  });

  // MEDI-FL-01
  testWidgets(
    'MOTHER bottom nav has Chuyên gia and Trò chuyện in the new slots; tapping switches tabs via IndexedStack (no route push)',
    (tester) async {
      await tester.pumpWidget(const MaterialApp(home: HomeShell()));
      await tester.pumpAndSettle();

      final navBarFinder = find.byType(NavigationBar);
      expect(
        find.descendant(of: navBarFinder, matching: find.text('Chuyên gia')),
        findsOneWidget,
      );
      expect(
        find.descendant(of: navBarFinder, matching: find.text('Trò chuyện')),
        findsOneWidget,
      );
      // Cộng đồng/Bài tập lost their nav-bar slots (moved into MotherHomeScreen's "Khám phá"
      // section instead — that section itself also renders a "Cộng đồng" label, so this
      // assertion is scoped to the NavigationBar specifically, not the whole page).
      expect(
        find.descendant(of: navBarFinder, matching: find.text('Cộng đồng')),
        findsNothing,
      );
      expect(
        find.descendant(of: navBarFinder, matching: find.text('Bài tập')),
        findsNothing,
      );

      await tester.tap(find.text('Chuyên gia'));
      await tester.pumpAndSettle();

      // Directory tab's own AppBar title proves it rendered in place (IndexedStack, not a push).
      expect(find.text('Chuyên gia đã xác thực'), findsOneWidget);
    },
  );

  testWidgets(
    'foreground conversation events refresh the Mother unread badge',
    (tester) async {
      final service = _FakeDirectChatService();
      DirectChatService.instance = service;
      await tester.pumpWidget(const MaterialApp(home: HomeShell()));
      await tester.pumpAndSettle();
      expect(find.text('2'), findsNothing);

      service.unreadConversationCount = 2;
      ConversationRefreshBus.notify();
      await tester.pumpAndSettle();
      expect(find.text('2'), findsOneWidget);
    },
  );
}
