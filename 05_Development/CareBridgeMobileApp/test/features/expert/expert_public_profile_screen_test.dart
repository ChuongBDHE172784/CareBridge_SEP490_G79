import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/directChat/models/direct_conversation.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';
import 'package:untitled/features/expert/screens/expert_public_profile_screen.dart';

class _ScriptedDirectChatService extends DirectChatService {
  int findOrCreateCallCount = 0;
  String? lastExpertProfileId;

  @override
  Future<Map<String, dynamic>> getExpertProfile(String expertProfileId) async =>
      {
        'expertProfileId': expertProfileId,
        'displayName': 'BS. Trần Thị B',
        'professionalTitle': 'Bác sĩ Nhi khoa',
        'specialty': 'Nhi khoa',
        'verificationStatus': 'APPROVED',
        'consultationEligible': true,
      };

  @override
  Future<DirectConversation> findOrCreateConversation(
    String expertProfileId,
  ) async {
    findOrCreateCallCount++;
    lastExpertProfileId = expertProfileId;
    return DirectConversation(
      conversationId: 'conv-X',
      motherUserId: 'mother-1',
      expertUserId: 'expert-1',
      status: 'ACTIVE',
      createdAt: DateTime.utc(2026, 1, 1),
      expertAvailable: true,
    );
  }
}

void main() {
  late DirectChatService original;

  setUp(() => original = DirectChatService.instance);
  tearDown(() => DirectChatService.instance = original);

  // A profile without an existing conversation keeps the consultation CTA.
  testWidgets(
    'shows consultation CTA when the expert is not yet in the conversation list',
    (tester) async {
      final service = _ScriptedDirectChatService();
      DirectChatService.instance = service;

      final router = GoRouter(
        initialLocation: '/expert/public/expert-profile-9',
        routes: [
          GoRoute(
            path: '/expert/public/:expertProfileId',
            builder: (_, state) => ExpertPublicProfileScreen(
              expertProfileId: state.pathParameters['expertProfileId']!,
            ),
          ),
          GoRoute(
            path: '/direct-chat/:id',
            builder: (_, state) =>
                Scaffold(body: Text('chat:${state.pathParameters['id']}')),
          ),
        ],
      );
      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      expect(find.text('Yêu cầu tư vấn'), findsOneWidget);
      expect(find.text('Trò chuyện'), findsNothing);
      expect(service.findOrCreateCallCount, 0);
    },
  );

  testWidgets(
    'shows chat CTA and hides consultation request CTA when expert is in conversation list',
    (tester) async {
      final service = _ScriptedDirectChatServiceWithConversation();
      DirectChatService.instance = service;

      final router = GoRouter(
        initialLocation: '/expert/public/expert-profile-9',
        routes: [
          GoRoute(
            path: '/expert/public/:expertProfileId',
            builder: (_, state) => ExpertPublicProfileScreen(
              expertProfileId: state.pathParameters['expertProfileId']!,
            ),
          ),
          GoRoute(
            path: '/direct-chat/:id',
            builder: (_, state) =>
                Scaffold(body: Text('chat:${state.pathParameters['id']}')),
          ),
        ],
      );
      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      expect(find.text('Trò chuyện'), findsOneWidget);
      expect(find.text('Yêu cầu tư vấn'), findsNothing);

      await tester.tap(find.text('Trò chuyện'));
      await tester.pumpAndSettle();
      expect(find.text('chat:conv-123'), findsOneWidget);
    },
  );
}

class _ScriptedDirectChatServiceWithConversation
    extends _ScriptedDirectChatService {
  @override
  Future<Map<String, dynamic>> getExpertProfile(String expertProfileId) async =>
      {
        'expertProfileId': expertProfileId,
        'userId': 'expert-user-1',
        'displayName': 'BS. Trần Thị B',
        'professionalTitle': 'Bác sĩ Nhi khoa',
        'specialty': 'Nhi khoa',
        'verificationStatus': 'APPROVED',
        'consultationEligible': true,
      };

  @override
  Future<List<DirectConversationSummary>> listMyConversations() async => [
        DirectConversationSummary(
          conversationId: 'conv-123',
          counterpartUserId: 'expert-user-1',
          counterpartRole: 'EXPERT',
          expertAvailable: true,
          counterpartDisplayName: 'BS. Trần Thị B',
          unreadCount: 0,
        ),
      ];
}
