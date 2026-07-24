import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/community/services/content_service.dart';

Map<String, dynamic> _item(String id, String type) => {
  'id': id,
  'type': type,
  'title': 'Synthetic reviewed guidance',
  'stage': 'PRE_PREGNANCY',
  'topicId': 'synthetic-topic-69',
  'publishedAt': '2026-07-23T00:00:00Z',
};

Map<String, dynamic> _detail(String id) => {
  ..._item(id, 'ARTICLE'),
  'body': 'Synthetic safe body',
  'version': 1,
};

void main() {
  group('UC82-69-MOB-001 lifecycle content contract', () {
    test('generic page parser reads top-level ApiResponse data', () async {
      final paths = <String>[];
      final service = ContentService(
        getRequest: (path) async {
          paths.add(path);
          return {
            'data': [_item('generic-69', 'ARTICLE')],
            'page': 0,
            'size': 10,
            'totalElements': 1,
            'totalPages': 1,
          };
        },
      );

      final result = await service.getContent(
        type: 'ARTICLE',
        stage: 'PRE_PREGNANCY',
      );

      expect(result.single.id, 'generic-69');
      expect(result.single.stage, 'PRE_PREGNANCY');
      expect(paths.single, contains('/api/v1/content?'));
    });

    test(
      'lifecycle list and checklist parse typed server-stage envelopes',
      () async {
        final paths = <String>[];
        final service = ContentService(
          getRequest: (path) async {
            paths.add(path);
            if (path == '/api/v1/content/lifecycle/checklists') {
              return {
                'data': {
                  'stage': 'PRE_PREGNANCY',
                  'payload': [
                    {
                      'id': 'checklist-69',
                      'name': 'Synthetic checklist',
                      'stage': 'PRE_PREGNANCY',
                      'description': 'Synthetic metadata',
                      'items': const [],
                    },
                  ],
                },
              };
            }
            return {
              'data': {
                'stage': 'PRE_PREGNANCY',
                'payload': {
                  'data': [_item('lifecycle-69', 'ARTICLE')],
                  'page': 0,
                  'size': 20,
                  'totalElements': 1,
                  'totalPages': 1,
                },
              },
            };
          },
        );

        final list = await service.getLifecycleContent(type: 'ARTICLE');
        final checklists = await service.getLifecycleChecklists();

        expect(list.stage, 'PRE_PREGNANCY');
        expect(list.payload.data.single.id, 'lifecycle-69');
        expect(checklists.stage, 'PRE_PREGNANCY');
        expect(checklists.payload.single.id, 'checklist-69');
        expect(paths.first, startsWith('/api/v1/content/lifecycle?'));
        expect(paths.first, isNot(contains('stage=')));
        expect(paths.last, '/api/v1/content/lifecycle/checklists');
      },
    );

    test(
      'lifecycle detail uses its dedicated route and canonical PRE stage',
      () async {
        final paths = <String>[];
        final service = ContentService(
          getRequest: (path) async {
            paths.add(path);
            return {
              'data': {
                'stage': 'PRE_PREGNANCY',
                'payload': _detail('detail-69'),
              },
            };
          },
        );

        final detail = await service.getLifecycleContentDetail('detail-69');

        expect(detail.stage, 'PRE_PREGNANCY');
        expect(detail.payload.stage, 'PRE_PREGNANCY');
        expect(paths.single, '/api/v1/content/lifecycle/detail-69');
      },
    );
  });
}
