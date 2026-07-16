import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/models/expert_directory_item.dart';
import 'package:untitled/features/directChat/screens/expert_directory_screen.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';

class _RecordingDirectChatService extends DirectChatService {
  final List<String?> queries = [];

  @override
  Future<ExpertDirectoryPage> getExpertDirectory({
    String? q,
    String? specialty,
    int page = 0,
    int size = 20,
  }) async {
    queries.add(q);
    return const ExpertDirectoryPage(
      experts: [],
      currentPage: 0,
      pageSize: 20,
      totalElements: 0,
      totalPages: 0,
    );
  }
}

void main() {
  late DirectChatService original;

  setUp(() => original = DirectChatService.instance);
  tearDown(() => DirectChatService.instance = original);

  // MEDI-FL-04
  testWidgets('typing 3 characters within the debounce window only issues 1 request, with q="ngu"',
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
  });
}
