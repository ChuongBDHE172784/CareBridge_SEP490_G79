import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/checklist/models/user_checklist_item_model.dart';

void main() {
  test('maps persisted template linkage and completion state', () {
    final item = UserChecklistItem.fromJson({
      'itemId': 'user-item-1',
      'templateItemId': 'template-item-1',
      'itemText': 'Chuẩn bị giấy tờ',
      'category': 'PAPERWORK',
      'completed': true,
      'completedAt': '2026-07-23T08:00:00Z',
      'itemOrder': 2,
    });

    expect(item.templateItemId, 'template-item-1');
    expect(item.completed, isTrue);
    expect(item.category, ChecklistCategory.paperwork);
  });
}
