import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/models/expert_directory_item.dart';
import 'package:untitled/features/directChat/screens/expert_directory_screen.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';

class _RecordingDirectChatService extends DirectChatService {
  final List<String?> queries = [];
  final List<String?> specialties = [];

  @override
  Future<ExpertDirectoryPage> getExpertDirectory({
    String? q,
    String? specialty,
    int page = 0,
    int size = 20,
  }) async {
    queries.add(q);
    specialties.add(specialty);
    return const ExpertDirectoryPage(
      experts: [],
      currentPage: 0,
      pageSize: 20,
      totalElements: 0,
      totalPages: 0,
      specialties: ['Nhi khoa', 'Sản khoa'],
    );
  }
}

class _RacingDirectoryService extends DirectChatService {
  final Map<String, Completer<ExpertDirectoryPage>> pending = {};

  @override
  Future<ExpertDirectoryPage> getExpertDirectory({
    String? q,
    String? specialty,
    int page = 0,
    int size = 20,
  }) {
    if (q == null || q.isEmpty) {
      return Future.value(
        const ExpertDirectoryPage(
          experts: [],
          currentPage: 0,
          pageSize: 20,
          totalElements: 0,
          totalPages: 1,
        ),
      );
    }
    return (pending[q] ??= Completer<ExpertDirectoryPage>()).future;
  }
}

void main() {
  late DirectChatService original;

  setUp(() => original = DirectChatService.instance);
  tearDown(() => DirectChatService.instance = original);

  // MEDI-FL-04
  testWidgets(
    'typing 3 characters within the debounce window only issues 1 request, with q="ngu"',
    (tester) async {
      final service = _RecordingDirectChatService();
      DirectChatService.instance = service;

      await tester.pumpWidget(const MaterialApp(home: ExpertDirectoryScreen()));
      await tester.pump(); // initial load — the 1 call fired by initState

      final initialCallCount = service.queries.length;

      await tester.enterText(find.byType(TextField), 'n');
      await tester.pump(const Duration(milliseconds: 100));
      await tester.enterText(find.byType(TextField), 'ng');
      await tester.pump(const Duration(milliseconds: 100));
      await tester.enterText(find.byType(TextField), 'ngu');
      // Still inside the 400ms debounce window relative to the last keystroke — no call yet.
      await tester.pump(const Duration(milliseconds: 100));
      expect(service.queries.length, initialCallCount);

      // Now let the debounce timer fire.
      await tester.pump(const Duration(milliseconds: 400));

      expect(service.queries.length, initialCallCount + 1);
      expect(service.queries.last, 'ngu');
    },
  );

  testWidgets(
    'specialty chips use backend values and send the selected specialty',
    (tester) async {
      final service = _RecordingDirectChatService();
      DirectChatService.instance = service;

      await tester.pumpWidget(const MaterialApp(home: ExpertDirectoryScreen()));
      await tester.pumpAndSettle();

      expect(find.text('Nhi khoa'), findsOneWidget);
      await tester.tap(find.text('Nhi khoa'));
      await tester.pumpAndSettle();

      expect(service.specialties.last, 'Nhi khoa');
    },
  );

  testWidgets('a slower old search cannot overwrite a newer query', (
    tester,
  ) async {
    final service = _RacingDirectoryService();
    DirectChatService.instance = service;
    await tester.pumpWidget(const MaterialApp(home: ExpertDirectoryScreen()));
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextField), 'old');
    await tester.pump(const Duration(milliseconds: 401));
    await tester.enterText(find.byType(TextField), 'new');
    await tester.pump(const Duration(milliseconds: 401));

    service.pending['new']!.complete(
      const ExpertDirectoryPage(
        experts: [
          ExpertDirectoryItem(
            expertProfileId: 'new-id',
            displayName: 'New result',
          ),
        ],
        currentPage: 0,
        pageSize: 20,
        totalElements: 1,
        totalPages: 1,
      ),
    );
    await tester.pump();
    service.pending['old']!.complete(
      const ExpertDirectoryPage(
        experts: [
          ExpertDirectoryItem(
            expertProfileId: 'old-id',
            displayName: 'Old result',
          ),
        ],
        currentPage: 0,
        pageSize: 20,
        totalElements: 1,
        totalPages: 1,
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('New result'), findsOneWidget);
    expect(find.text('Old result'), findsNothing);
  });
}
