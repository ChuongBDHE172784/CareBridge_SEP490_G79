import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/community/models/community_model.dart';
import 'package:untitled/features/community/screens/question_detail_screen.dart';
import 'package:untitled/features/community/services/community_service.dart';

class _FakeCommunityService extends CommunityService {
  final QuestionDetail detail;

  _FakeCommunityService(this.detail);

  @override
  Future<QuestionDetail> getQuestionDetail(String questionId) async => detail;
}

void main() {
  late CommunityService originalService;

  setUp(() {
    originalService = CommunityService.instance;
  });

  tearDown(() {
    CommunityService.instance = originalService;
  });

  testWidgets(
    'displays expert name in answer card and navigates to expert public profile on tap without header tag',
    (tester) async {
      final question = QuestionDetail(
        id: 'q-101',
        topicName: 'Dinh dưỡng cho mẹ',
        title: 'Thực đơn ăn dặm cho bé 6 tháng',
        body: 'Bé nhà em chuẩn bị ăn dặm, nhờ bác sĩ tư vấn thực đơn phù hợp.',
        stage: 'INFANT',
        urgency: 'NORMAL',
        anonymous: false,
        authorId: 'mother-user-1',
        authorDisplay: 'Mẹ Bé Bông',
        status: 'APPROVED',
        answerCount: 1,
        likeCount: 5,
        isBookmarked: false,
        isLiked: false,
        createdAt: DateTime.now().subtract(const Duration(hours: 2)).toIso8601String(),
        updatedAt: DateTime.now().subtract(const Duration(hours: 2)).toIso8601String(),
        answers: [
          CommunityAnswer(
            id: 'ans-1',
            questionId: 'q-101',
            authorId: 'expert-user-1',
            authorDisplay: 'BS. CKII Nguyễn Văn A',
            body: 'Bé 6 tháng nên bắt đầu từ bột/cháo loãng ngọt từ rau củ quả.',
            personalExperience: false,
            expertLabeled: true,
            expertProfileId: 'expert-profile-001',
            status: 'APPROVED',
            likeCount: 12,
            liked: false,
            createdAt: DateTime.now().subtract(const Duration(hours: 1)).toIso8601String(),
          ),
        ],
      );

      CommunityService.instance = _FakeCommunityService(question);

      final router = GoRouter(
        initialLocation: '/community/questions/q-101',
        routes: [
          GoRoute(
            path: '/community/questions/:id',
            builder: (_, state) => QuestionDetailScreen(
              questionId: state.pathParameters['id']!,
            ),
          ),
          GoRoute(
            path: '/expert/public/:expertProfileId',
            builder: (_, state) => Scaffold(
              body: Text('expert_profile:${state.pathParameters['expertProfileId']}'),
            ),
          ),
        ],
      );

      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      // Verify regular tags exist, but header does not have "Bác sĩ BS. CKII Nguyễn Văn A"
      expect(find.text('Dinh dưỡng cho mẹ'), findsOneWidget);
      expect(find.text('Bác sĩ BS. CKII Nguyễn Văn A'), findsNothing);

      // Verify expert name and "Chuyên gia" label in answer card
      expect(find.text('BS. CKII Nguyễn Văn A'), findsOneWidget);
      expect(find.text('Chuyên gia'), findsOneWidget);

      // Tap on expert author name in answer card
      await tester.tap(find.text('BS. CKII Nguyễn Văn A'));
      await tester.pumpAndSettle();

      // Expect navigation to public profile
      expect(find.text('expert_profile:expert-profile-001'), findsOneWidget);
    },
  );
}
