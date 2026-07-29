import 'package:untitled/features/community/screens/post_answer_screen.dart';
import 'package:untitled/features/community/screens/question_detail_screen.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('only approved questions can open the answer composer', () {
    expect(canAnswerCommunityQuestion('APPROVED'), isTrue);
    expect(canAnswerCommunityQuestion('PENDING'), isFalse);
    expect(canAnswerCommunityQuestion('LOCKED'), isFalse);
    expect(canAnswerCommunityQuestion(null), isFalse);
  });

  test('maps COM-007 to a friendly approval-state message', () {
    final message = postAnswerErrorMessage(
      ApiException(
        422,
        '{"error":"COM-007","message":"Question must be APPROVED"}',
      ),
    );

    expect(message, contains('chờ duyệt'));
    expect(message, isNot(contains('ApiException')));
  });

  testWidgets('states that a new answer is screened before publication', (
    tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(home: PostAnswerScreen(questionId: 'question-1')),
    );

    expect(
      find.text('Câu trả lời sẽ được kiểm duyệt trước khi hiển thị.'),
      findsOneWidget,
    );
    expect(find.textContaining('hiển thị ngay'), findsNothing);
    expect(find.text('Chụp ảnh'), findsOneWidget);
    expect(find.text('Thư viện'), findsOneWidget);
  });
}
