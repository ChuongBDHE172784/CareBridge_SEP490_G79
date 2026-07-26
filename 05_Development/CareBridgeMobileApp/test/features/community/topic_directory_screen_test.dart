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
        sortOrder:
            1, // chosen so a lingering sortOrder*100 bug would render "100 câu hỏi"
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

  group('buildCategoryChips', () {
    test('uses real CATEGORY rows and ignores TOPIC rows', () {
      final rows = [
        _taxonomyRow(id: 'c1', name: 'Mang thai', type: 'CATEGORY'),
        _taxonomyRow(id: 't1', name: 'Chăm bé', type: 'TOPIC', parentId: 'c1'),
        _taxonomyRow(id: 'c2', name: 'Sau sinh', type: 'CATEGORY'),
      ];

      final chips = buildCategoryChips(rows);

      expect(chips.map((chip) => chip.label), [
        'Tất cả',
        'Mang thai',
        'Sau sinh',
      ]);
      expect(chips.map((chip) => chip.categoryId), [null, 'c1', 'c2']);
    });
  });

  group('filterDirectoryTopics', () {
    final topics = [
      _taxonomyRow(
        id: 't1',
        name: 'Dinh dưỡng thai kỳ',
        description: 'Ăn uống an toàn',
        type: 'TOPIC',
        parentId: 'c1',
      ),
      _taxonomyRow(
        id: 't2',
        name: 'Giấc ngủ sau sinh',
        description: 'Phục hồi cho mẹ',
        type: 'TOPIC',
        parentId: 'c2',
      ),
      _taxonomyRow(
        id: 't3',
        name: 'Vận động nhẹ',
        description: 'Thai kỳ khoẻ mạnh',
        type: 'TOPIC',
        parentId: 'c1',
      ),
    ];

    test('combines selected category and inline keyword', () {
      final filtered = filterDirectoryTopics(
        topics,
        selectedCategoryId: 'c1',
        keyword: 'dinh dưỡng',
      );

      expect(filtered.map((topic) => topic.id), ['t1']);
    });

    test('matches descriptions case-insensitively', () {
      final filtered = filterDirectoryTopics(
        topics,
        selectedCategoryId: 'c1',
        keyword: 'KHOẺ MẠNH',
      );

      expect(filtered.map((topic) => topic.id), ['t3']);
    });

    test('empty query and all categories return every topic', () {
      final filtered = filterDirectoryTopics(
        topics,
        selectedCategoryId: null,
        keyword: '',
      );

      expect(filtered.map((topic) => topic.id), ['t1', 't2', 't3']);
    });
  });
}

CommunityTopic _taxonomyRow({
  required String id,
  required String name,
  String description = '',
  required String type,
  String? parentId,
}) => CommunityTopic(
  id: id,
  name: name,
  description: description,
  icon: 'topic',
  isHidden: false,
  sortOrder: 0,
  type: type,
  parentId: parentId,
);
