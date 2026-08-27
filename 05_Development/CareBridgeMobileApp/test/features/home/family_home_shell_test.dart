import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/models/direct_conversation.dart';
import 'package:untitled/features/directChat/models/expert_directory_item.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';
import 'package:untitled/features/checklist/services/checklist_assignment_refresh_bus.dart';
import 'package:untitled/features/home/screens/family_home_shell.dart';

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
  }) async =>
      const ExpertDirectoryPage(
        experts: [],
        currentPage: 0,
        pageSize: 20,
        totalElements: 0,
        totalPages: 0,
      );
}

void main() {
  late DirectChatService original;
  late _FakeDirectChatService fake;

  setUp(() {
    original = DirectChatService.instance;
    fake = _FakeDirectChatService();
    DirectChatService.instance = fake;
  });

  tearDown(() {
    DirectChatService.instance = original;
  });

  testWidgets('FamilyHomeShell displays 5 standard NavigationBar destinations',
      (tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: FamilyHomeShell(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(NavigationBar), findsOneWidget);
    expect(find.text('Trang chủ'), findsWidgets);
    expect(find.text('Nhóm'), findsWidgets);
    expect(find.text('Chuyên gia'), findsWidgets);
    expect(find.text('Trò chuyện'), findsWidgets);
    expect(find.text('Hồ sơ'), findsWidgets);
  });

  testWidgets('FamilyHomeShell initializes at requested initialIndex',
      (tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: FamilyHomeShell(initialIndex: 2),
      ),
    );
    await tester.pumpAndSettle();

    final navBar = tester.widget<NavigationBar>(find.byType(NavigationBar));
    expect(navBar.selectedIndex, equals(2));
  });

  testWidgets('FamilyHomeShell updates unread badge when event emitted',
      (tester) async {
    fake.unreadConversationCount = 3;

    await tester.pumpWidget(
      const MaterialApp(
        home: FamilyHomeShell(initialIndex: 0),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('3'), findsOneWidget);
  });

  testWidgets(
    'FamilyHomeShell notifies ChecklistAssignmentRefreshBus when switching back to tab 0',
    (tester) async {
      final notificationAssertion = expectLater(
        ChecklistAssignmentRefreshBus.events.first.timeout(
          const Duration(milliseconds: 100),
        ),
        completes,
      );

      try {
        await tester.pumpWidget(
          const MaterialApp(home: FamilyHomeShell(initialIndex: 1)),
        );
        // One frame is enough to build the navigation shell. Waiting for the entire
        // IndexedStack would also wait for unrelated background loads in inactive tabs.
        await tester.pump();

        await tester.tap(find.text('Trang chủ'));
        // Switching tabs emits the in-process refresh signal synchronously. Do not wait
        // for every child in the IndexedStack to settle: the newly visible home tab may
        // legitimately start independent async refreshes and loading animations.
        await tester.pump(const Duration(milliseconds: 100));

        await notificationAssertion;
        final navBar = tester.widget<NavigationBar>(find.byType(NavigationBar));
        expect(navBar.selectedIndex, 0);
      } finally {
        // Dispose the IndexedStack even when an assertion fails so background work
        // cannot bleed into the next test file in a concurrent full-suite run.
        await tester.pumpWidget(const SizedBox.shrink());
        await tester.pump();
      }
    },
  );
}
