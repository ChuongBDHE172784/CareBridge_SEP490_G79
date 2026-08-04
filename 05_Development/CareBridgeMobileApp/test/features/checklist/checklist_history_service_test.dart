import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/checklist/models/checklist_history_model.dart';
import 'package:untitled/features/checklist/services/checklist_history_service.dart';

void main() {
  test(
    'loads checklist history from the dedicated paginated endpoint',
    () async {
      String? capturedPath;
      Map<String, dynamic>? capturedQuery;
      final service = ChecklistHistoryService(
        getRequest: (path, {queryParams}) async {
          capturedPath = path;
          capturedQuery = queryParams;
          return {
            'success': true,
            'data': {
              'items': const [],
              'page': 1,
              'size': 5,
              'totalElements': 7,
              'totalPages': 2,
            },
          };
        },
      );

      final page = await service.loadHistory(
        page: 1,
        size: 5,
        targetSubject: ChecklistHistoryTargetSubject.baby,
      );

      expect(capturedPath, '/api/v1/checklists/history');
      expect(capturedQuery, {'page': 1, 'size': 5, 'targetSubject': 'BABY'});
      expect(page.page, 1);
      expect(page.hasNextPage, isFalse);
    },
  );

  test('binds FAMILY history to the selected care group', () async {
    String? capturedPath;
    final service = ChecklistHistoryService(
      getRequest: (path, {queryParams}) async {
        capturedPath = path;
        return {
          'data': {
            'items': const [],
            'page': 0,
            'size': 20,
            'totalElements': 0,
            'totalPages': 0,
          },
        };
      },
    );

    await service.loadHistory(careGroupId: 'group-b');

    expect(capturedPath, '/api/v1/care-groups/group-b/checklists/history');
  });
}
