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
  Future<Map<String, dynamic>> getExpertProfile(String expertProfileId) async => {
    'expertProfileId': expertProfileId,
    'displayName': 'BS. Trần Thị B',
    'professionalTitle': 'Bác sĩ Nhi khoa',
    'specialty': 'Nhi khoa',
    'verificationStatus': 'APPROVED',
  };

  @override
  Future<DirectConversation> findOrCreateConversation(String expertProfileId) async {
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

  // MEDI-FL-06
  testWidgets(
    'CTA calls find-or-create with the right expertProfileId then pushes the returned conversation; retapping stays on the same conversation',
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
            builder: (_, state) => Scaffold(body: Text('chat:${state.pathParameters['id']}')),
          ),
        ],
      );
      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Trò chuyện'));
      await tester.pumpAndSettle();

      expect(service.lastExpertProfileId, 'expert-profile-9');
      expect(service.findOrCreateCallCount, 1);
      expect(find.text('chat:conv-X'), findsOneWidget);

      // Repeat: pop back to the profile and tap the CTA a second time — the backend's
      // find-or-create idempotency (BR-DCC-002) means a second call still resolves to the
      // exact same conversationId, never a duplicate.
      router.pop();
      await tester.pumpAndSettle();
      await tester.tap(find.text('Trò chuyện'));
      await tester.pumpAndSettle();

      expect(service.findOrCreateCallCount, 2);
      expect(find.text('chat:conv-X'), findsOneWidget);
    },
  );
}
