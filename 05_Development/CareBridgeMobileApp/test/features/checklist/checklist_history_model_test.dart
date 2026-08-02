import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/checklist/models/checklist_history_model.dart';

void main() {
  test('parses baby-care history without normalizing the display stage', () {
    final page = ChecklistHistoryPage.fromJson({
      'items': [
        {
          'checklistInstanceId': 'instance-1',
          'templateVersionId': 'template-1',
          'templateName': 'Baby month 1',
          'stage': 'BABY_CARE',
          'targetSubject': 'BABY',
          'careContextType': 'BABY',
          'careContextId': 'baby-1',
          'careContextLabel': 'Bean',
          'windowStart': '2026-08-01',
          'windowEnd': '2026-08-31',
          'historicalAt': '2026-09-01T00:00:00Z',
          'historyReasonCode': 'LIFECYCLE_STAGE_OBSOLETE',
          'tasks': [
            {
              'taskId': 'task-1',
              'title': 'Measure weight',
              'status': 'COMPLETED',
              'completedAt': '2026-08-02T00:00:00Z',
              'displayOrder': 1,
              'required': true,
            },
            {
              'taskId': 'task-2',
              'title': 'Book vaccine',
              'status': 'PENDING',
              'displayOrder': 2,
              'required': false,
            },
          ],
        },
      ],
      'page': 0,
      'size': 20,
      'totalElements': 1,
      'totalPages': 1,
    });

    final item = page.items.single;
    expect(item.stage, 'BABY_CARE');
    expect(item.stageLabel, 'Chăm sóc bé');
    expect(item.targetSubject, ChecklistHistoryTargetSubject.baby);
    expect(item.completedCount, 1);
    expect(item.pendingCount, 1);
    expect(item.tasks.first.statusLabel, 'Đã hoàn thành');
    expect(item.tasks.last.statusLabel, 'Chưa hoàn thành');
  });
}
