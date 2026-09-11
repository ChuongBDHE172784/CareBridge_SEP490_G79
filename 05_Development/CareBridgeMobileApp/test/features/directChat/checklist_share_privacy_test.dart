import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/widgets/checklist_message_card.dart';

void main() {
  group('ChecklistShareData Privacy Tests - Removing Personal Tasks', () {
    test('ChecklistItemShareData correctly identifies personal vs carebridge tasks', () {
      final userTask = ChecklistItemShareData(
        text: 'Mua tã bỉm cho bé',
        origin: 'USER',
        createdBy: 'USER',
      );
      final systemTask = ChecklistItemShareData(
        text: 'Xét nghiệm đường huyết thai kỳ (OGTT)',
        origin: 'SYSTEM',
        createdBy: 'SYSTEM',
      );
      final expertTask = ChecklistItemShareData(
        text: 'Đo huyết áp và nghỉ ngơi',
        origin: 'EXPERT',
        createdBy: 'EXPERT',
        isExpertCustom: true,
      );

      expect(userTask.isPersonal, isTrue);
      expect(userTask.isCareBridgeSuggestion, isFalse);

      expect(systemTask.isPersonal, isFalse);
      expect(systemTask.isCareBridgeSuggestion, isTrue);

      expect(expertTask.isPersonal, isFalse);
      expect(expertTask.isCareBridgeSuggestion, isTrue);
    });

    test('ChecklistShareData.parse filters out all user-created personal tasks', () {
      final payload = {
        'title': 'Danh sách việc cần làm (Checklist)',
        'gestationalWeek': 24,
        'journeyId': 'j-123',
        'isLiveSync': true,
        'completedCount': 3,
        'totalCount': 4,
        'progressPercent': 75,
        'historyItems': [
          {
            'text': 'Khám thai lần đầu',
            'completed': true,
            'origin': 'SYSTEM',
            'createdBy': 'SYSTEM',
          },
          {
            'text': 'Đi dạo công viên cùng chồng',
            'completed': true,
            'origin': 'USER',
            'createdBy': 'USER',
          },
        ],
        'currentItems': [
          {
            'text': 'Xét nghiệm đường huyết thai kỳ (OGTT)',
            'completed': false,
            'origin': 'SYSTEM',
            'createdBy': 'SYSTEM',
          },
          {
            'text': 'Nhắc chồng mua sữa chua',
            'completed': true,
            'origin': 'USER',
            'createdBy': 'USER',
          },
        ],
        'futureItems': [
          {
            'text': 'Tiêm phòng uốn ván mũi 2',
            'completed': false,
            'origin': 'SYSTEM',
            'createdBy': 'SYSTEM',
          },
          {
            'text': 'Sắp xếp phòng cho bé',
            'completed': false,
            'origin': 'USER',
            'createdBy': 'USER',
          },
        ],
      };

      final serialized = ChecklistShareData(
        completedCount: 3,
        totalCount: 4,
        progressPercent: 75,
      ).serialize(); // test base format tag

      final fullMessage = '${ChecklistShareData.tag}\n${payload.toString().replaceAll("'", '"')}';
      // Use clean json string
      final jsonBody = '${ChecklistShareData.tag}\n'
          '{"title":"Checklist","historyItems":[{"text":"Khám thai","completed":true,"origin":"SYSTEM"},{"text":"Việc riêng mẹ","completed":true,"origin":"USER"}],'
          '"currentItems":[{"text":"Xét nghiệm máu","completed":false,"origin":"SYSTEM"},{"text":"Mua quà sinh nhật","completed":false,"origin":"USER"}],'
          '"futureItems":[{"text":"Tiêm uốn ván","completed":false,"origin":"SYSTEM"},{"text":"Đi du lịch nghỉ dưỡng","completed":false,"origin":"USER"}]}';

      final parsed = ChecklistShareData.parse(jsonBody);
      expect(parsed, isNotNull);

      // Only SYSTEM items remain, all USER items are excluded
      expect(parsed!.historyItems.length, equals(1));
      expect(parsed.historyItems.first.text, equals('Khám thai'));

      expect(parsed.currentItems.length, equals(1));
      expect(parsed.currentItems.first.text, equals('Xét nghiệm máu'));

      expect(parsed.futureItems.length, equals(1));
      expect(parsed.futureItems.first.text, equals('Tiêm uốn ván'));

      expect(parsed.totalCount, equals(3));
      expect(parsed.completedCount, equals(1));
    });
  });
}
