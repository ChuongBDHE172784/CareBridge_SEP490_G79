import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/community/models/community_model.dart';

void main() {
  test('question and answer parse Cloudinary image URLs', () {
    const questionImage =
        'https://res.cloudinary.com/demo/image/upload/question.jpg';
    const answerImage =
        'https://res.cloudinary.com/demo/image/upload/answer.jpg';

    final detail = QuestionDetail.fromJson({
      'id': 'question-1',
      'title': 'Question',
      'body': 'Question body',
      'imageUrls': [questionImage],
      'answers': [
        {
          'id': 'answer-1',
          'questionId': 'question-1',
          'body': 'Answer body',
          'imageUrls': [answerImage],
        },
      ],
    });

    expect(detail.imageUrls, [questionImage]);
    expect(detail.answers.single.imageUrls, [answerImage]);
  });

  test('missing imageUrls remains backward compatible', () {
    final answer = CommunityAnswer.fromJson({
      'id': 'answer-1',
      'questionId': 'question-1',
    });

    expect(answer.imageUrls, isEmpty);
  });

  test('my question parses management status and counters', () {
    final question = MyCommunityQuestion.fromJson({
      'id': 'question-1',
      'title': 'Question',
      'body': 'Question body',
      'imageUrls': [
        'https://res.cloudinary.com/demo/image/upload/question.jpg',
      ],
      'status': 'HIDDEN',
      'answerCount': 3,
      'likeCount': 7,
      'createdAt': '2026-07-29T00:00:00Z',
      'updatedAt': '2026-07-29T01:00:00Z',
    });

    expect(question.status, 'HIDDEN');
    expect(question.answerCount, 3);
    expect(question.likeCount, 7);
    expect(question.imageUrls, hasLength(1));
    expect(question.copyWith(status: 'DELETED').status, 'DELETED');
  });
}
