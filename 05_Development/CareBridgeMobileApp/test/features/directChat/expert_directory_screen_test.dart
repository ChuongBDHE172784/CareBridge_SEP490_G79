import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/directChat/models/direct_conversation.dart';
import 'package:untitled/features/directChat/models/expert_directory_item.dart';
import 'package:untitled/features/directChat/screens/expert_directory_screen.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';

class _ScriptedDirectChatService extends DirectChatService {
  final Future<ExpertDirectoryPage> Function()? onGetDirectory;
  String? lastConversationExpertProfileId;

  _ScriptedDirectChatService({this.onGetDirectory});

  @override
  Future<ExpertDirectoryPage> getExpertDirectory({
    String? q,
    String? specialty,
    int page = 0,
    int size = 20,
  }) => onGetDirectory!();

  @override
  Future<DirectConversation> findOrCreateConversation(
    String expertProfileId,
  ) async {
    lastConversationExpertProfileId = expertProfileId;
    return DirectConversation(
      conversationId: 'conv-1',
      motherUserId: 'mother-1',
      expertUserId: 'expert-1',
      status: 'ACTIVE',
      createdAt: DateTime.utc(2026, 1, 1),
      expertAvailable: true,
    );
  }
}

ExpertDirectoryPage _pageWith(List<ExpertDirectoryItem> experts) =>
    ExpertDirectoryPage(
      experts: experts,
      currentPage: 0,
      pageSize: 20,
      totalElements: experts.length,
      totalPages: 1,
    );

const _expert = ExpertDirectoryItem(
  expertProfileId: 'expert-profile-1',
  displayName: 'BS. Nguyễn Văn A',
  professionalTitle: 'Bác sĩ Sản khoa',
  specialty: 'Sản khoa',
);

Future<GoRouter> _pumpWithRouter(WidgetTester tester) async {
  final router = GoRouter(
    initialLocation: '/directory',
    routes: [
      GoRoute(
        path: '/directory',
        builder: (_, _) => const ExpertDirectoryScreen(),
      ),
      GoRoute(
        path: '/expert/public/:id',
        builder: (_, state) =>
            Scaffold(body: Text('profile:${state.pathParameters['id']}')),
      ),
      GoRoute(
        path: '/direct-chat/:id',
        builder: (_, state) =>
            Scaffold(body: Text('chat:${state.pathParameters['id']}')),
      ),
    ],
  );
  await tester.pumpWidget(MaterialApp.router(routerConfig: router));
  return router;
}

void main() {
  late DirectChatService original;

  setUp(() => original = DirectChatService.instance);
  tearDown(() => DirectChatService.instance = original);

  // MEDI-FL-03
  testWidgets(
    'shows a loading indicator while the directory request is pending',
    (tester) async {
      DirectChatService.instance = _ScriptedDirectChatService(
        onGetDirectory: () => Completer<ExpertDirectoryPage>().future,
      );
      await _pumpWithRouter(tester);
      await tester.pump();

      expect(find.byType(CircularProgressIndicator), findsWidgets);
    },
  );

  testWidgets(
    'shows an error + retry button when the directory request fails',
    (tester) async {
      DirectChatService.instance = _ScriptedDirectChatService(
        onGetDirectory: () async => throw Exception('network down'),
      );
      await _pumpWithRouter(tester);
      await tester.pumpAndSettle();

      expect(find.text('Thử lại'), findsOneWidget);
    },
  );

  testWidgets('shows the empty-state text when the directory has no results', (
    tester,
  ) async {
    DirectChatService.instance = _ScriptedDirectChatService(
      onGetDirectory: () async => _pageWith(const []),
    );
    await _pumpWithRouter(tester);
    await tester.pumpAndSettle();

    expect(find.text('Không tìm thấy chuyên gia phù hợp'), findsOneWidget);
  });

  testWidgets('renders data rows for each returned expert', (tester) async {
    DirectChatService.instance = _ScriptedDirectChatService(
      onGetDirectory: () async => _pageWith(const [_expert]),
    );
    await _pumpWithRouter(tester);
    await tester.pumpAndSettle();

    expect(find.text('BS. Nguyễn Văn A'), findsOneWidget);
  });

  // Directory now directs the user to the profile; chat is exposed there only for existing conversations.
  testWidgets(
    'shows only the profile CTA on the first card',
    (tester) async {
      final service = _ScriptedDirectChatService(
        onGetDirectory: () async => _pageWith(const [_expert]),
      );
      DirectChatService.instance = service;
      await _pumpWithRouter(tester);
      await tester.pumpAndSettle();

      expect(find.text('Xem hồ sơ'), findsOneWidget);
      expect(find.text('Trò chuyện'), findsNothing);
    },
  );

  // MEDI-FL-05
  testWidgets(
    'tapping the card itself (not the CTA button) opens the right profile',
    (tester) async {
      DirectChatService.instance = _ScriptedDirectChatService(
        onGetDirectory: () async => _pageWith(const [_expert]),
      );
      await _pumpWithRouter(tester);
      await tester.pumpAndSettle();

      await tester.tap(find.text('BS. Nguyễn Văn A'));
      await tester.pumpAndSettle();

      expect(find.text('profile:expert-profile-1'), findsOneWidget);
    },
  );
}
