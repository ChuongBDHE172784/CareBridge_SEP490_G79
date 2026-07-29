import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/community/models/community_model.dart';

void main() {
  test('normalizes legacy baby-care stages in community feed payloads', () {
    final item = CommunityFeedItem.fromJson({
      'id': 'question-1',
      'title': 'Câu hỏi chăm bé',
      'stage': 'BABY_CARE',
    });

    expect(item.stage, 'POSTPARTUM');
  });

  test('normalizes legacy baby-care stages in question details', () {
    final detail = QuestionDetail.fromJson({
      'id': 'question-1',
      'title': 'Câu hỏi chăm bé',
      'stage': 'BABY_CARE',
    });

    expect(detail.stage, 'POSTPARTUM');
  });
}
