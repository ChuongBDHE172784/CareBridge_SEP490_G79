import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/community/models/community_model.dart';
import 'package:untitled/features/community/screens/topic_directory_screen.dart';

// MOB-TC-001 (CommunityTopicManagement_Test-Spec.md): the "X câu hỏi" badge must reflect the real
// questionCount field from the backend (ADR-COM-015), not the old `sortOrder * 100` placeholder.
void main() {
  group('questionCountLabel', () {
    test('uses the real questionCount, not sortOrder * 100', () {
      final topic = CommunityTopic(
        id: 't1',
        name: 'X',
        description: '',
        icon: 'topic',
        isHidden: false,
        sortOrder: 1, // chosen so a lingering sortOrder*100 bug would render "100 câu hỏi"
        questionCount: 42,
      );

      final label = questionCountLabel(topic);

      expect(label, '42 câu hỏi');
      expect(label, isNot('100 câu hỏi'));
    });

    test('renders 0 when a topic has no approved questions yet', () {
      final topic = CommunityTopic(
        id: 't2',
        name: 'Y',
        description: '',
        icon: 'topic',
        isHidden: false,
        sortOrder: 5,
        questionCount: 0,
      );

      expect(questionCountLabel(topic), '0 câu hỏi');
    });
  });
}
