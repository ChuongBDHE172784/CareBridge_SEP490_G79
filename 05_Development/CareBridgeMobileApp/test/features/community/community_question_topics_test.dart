import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/community/models/community_model.dart';
import 'package:untitled/features/community/services/community_service.dart';

CommunityTopic _topic(String id, String type) => CommunityTopic(
  id: id,
  name: id,
  description: '',
  icon: 'topic',
  type: type,
  isHidden: false,
  sortOrder: 0,
);

void main() {
  test(
    'question topic loader requests TOPIC and rejects other row types',
    () async {
      String? requestedType;

      final result = await loadQuestionTopics(({String? type}) async {
        requestedType = type;
        return [
          _topic('category', 'CATEGORY'),
          _topic('topic', 'TOPIC'),
          _topic('tag', 'TAG'),
        ];
      });

      expect(requestedType, 'TOPIC');
      expect(result.map((topic) => topic.id), ['topic']);
    },
  );
}
