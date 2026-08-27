import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/aiTriage/models/triage_continuation.dart';
import 'package:untitled/features/aiTriage/services/triage_continuation_restore_coordinator.dart';
import 'package:untitled/features/aiTriage/services/triage_continuation_store.dart';
import 'package:untitled/features/directChat/models/direct_conversation.dart';
import 'package:untitled/features/directChat/models/expert_directory_item.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';
import 'package:untitled/features/directChat/services/conversation_refresh_bus.dart';
import 'package:untitled/features/checklist/services/checklist_assignment_refresh_bus.dart';
import 'package:untitled/features/home/screens/home_shell.dart';
import 'package:untitled/features/journey/screens/mother_journey_screen.dart';

class _NoopContinuationStore implements TriageContinuationStore {
  @override
  int generationFor(String userId) => 0;

  @override
  Future<void> invalidateUser(String userId) async {}

  @override
  Future<PendingTriageContinuation?> read(String userId) async => null;

  @override
  Future<void> save({
    required String userId,
    required PendingTriageContinuation continuation,
    required int generation,
  }) async {}
}

class _NoopContinuationGateway implements TriageContinuationGateway {
  @override
  Future<void> acknowledge(String token) async {}

  @override
  Future<TriageContinuationResolution> resolve(String token) =>
      throw StateError('not used');
}

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

      final navBar = tester.widget<NavigationBar>(navBarFinder);
      navBar.onDestinationSelected!(2);
      await tester.pumpAndSettle();

      // Directory tab's own AppBar title proves it rendered in place (IndexedStack, not a push).
      expect(find.text('Đội ngũ Chuyên gia'), findsOneWidget);
    },
  );

  testWidgets('reactivating the retained Home tab requests a Today refresh', (
    tester,
  ) async {
    var refreshEvents = 0;
    final subscription = ChecklistAssignmentRefreshBus.events.listen(
      (_) => refreshEvents++,
    );
    addTearDown(subscription.cancel);

    await tester.pumpWidget(const MaterialApp(home: HomeShell()));
    await tester.pumpAndSettle();

    final navigation = tester.widget<NavigationBar>(find.byType(NavigationBar));
    navigation.onDestinationSelected!(1);
    await tester.pump();
    expect(refreshEvents, 0);

    navigation.onDestinationSelected!(0);
    await tester.pump();
    expect(refreshEvents, 1);
  });

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

  testWidgets(
    '150 percent text scale keeps bottom navigation labels from colliding',
    (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          builder: (context, child) => MediaQuery(
            data: MediaQuery.of(
              context,
            ).copyWith(textScaler: const TextScaler.linear(1.5)),
            child: child!,
          ),
          home: const HomeShell(),
        ),
      );
      await tester.pumpAndSettle();

      final navigationBar = tester.widget<NavigationBar>(
        find.byType(NavigationBar),
      );
      expect(
        navigationBar.labelBehavior,
        NavigationDestinationLabelBehavior.onlyShowSelected,
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'same-route Mother shell update forwards a newly resolved continuation',
    (tester) async {
      final coordinator = TriageContinuationRestoreCoordinator(
        store: _NoopContinuationStore(),
        gateway: _NoopContinuationGateway(),
      );
      final arrival = TriageContinuationArrival(
        userId: 'account-a',
        decision: const TriageContinuationDecision(
          destination: TriageContinuationDestination.motherJourney,
          continuationToken: 'continuation-token',
          generation: 0,
          originReferenceId: '10000000-0000-0000-0000-000000000001',
          riskLevel: 'GREEN',
          stage: 'PREGNANCY',
          showRecordedConfirmation: true,
        ),
        coordinator: coordinator,
      );

      await tester.pumpWidget(
        const MaterialApp(home: HomeShell(initialIndex: 1)),
      );
      await tester.pump();

      await tester.pumpWidget(
        MaterialApp(
          home: HomeShell(initialIndex: 1, continuationArrival: arrival),
        ),
      );
      await tester.pump();

      final journey = tester.widget<MotherJourneyScreen>(
        find.byType(MotherJourneyScreen),
      );
      expect(identical(journey.continuationArrival, arrival), isTrue);
    },
  );
}
