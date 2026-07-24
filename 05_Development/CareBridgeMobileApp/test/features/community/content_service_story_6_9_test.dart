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

    test(
      'lifecycle aggregation loads every page under one server stage',
      () async {
        final paths = <String>[];
        final service = ContentService(
          getRequest: (path) async {
            paths.add(path);
            final page = path.contains('page=1') ? 1 : 0;
            return {
              'data': {
                'stage': 'PRE_PREGNANCY',
                'payload': {
                  'data': [_item('lifecycle-page-$page', 'ARTICLE')],
                  'page': page,
                  'size': 50,
                  'totalElements': 2,
                  'totalPages': 2,
                },
              },
            };
          },
        );

        final result = await service.getAllLifecycleContent();

        expect(result.stage, 'PRE_PREGNANCY');
        expect(result.payload.map((item) => item.id), [
          'lifecycle-page-0',
          'lifecycle-page-1',
        ]);
        expect(paths, hasLength(2));
        expect(paths.first, contains('page=0'));
        expect(paths.last, contains('page=1'));
        expect(paths, everyElement(isNot(contains('stage='))));
      },
    );

    test(
      'lifecycle aggregation follows declared pages after a duplicate-only page',
      () async {
        final paths = <String>[];
        final service = ContentService(
          getRequest: (path) async {
            paths.add(path);
            final page = path.contains('page=2')
                ? 2
                : path.contains('page=1')
                ? 1
                : 0;
            final id = page == 2 ? 'lifecycle-final' : 'lifecycle-shared';
            return {
              'data': {
                'stage': 'PRE_PREGNANCY',
                'payload': {
                  'data': [_item(id, 'ARTICLE')],
                  'page': page,
                  'size': 50,
                  'totalElements': 2,
                  'totalPages': 3,
                },
              },
            };
          },
        );

        final result = await service.getAllLifecycleContent();

        expect(paths, hasLength(3));
        expect(result.payload.map((item) => item.id), [
          'lifecycle-shared',
          'lifecycle-final',
        ]);
      },
    );

    test(
      'lifecycle aggregation rejects a stage change between pages',
      () async {
        final service = ContentService(
          getRequest: (path) async {
            final secondPage = path.contains('page=1');
            return {
              'data': {
                'stage': secondPage ? 'PREGNANCY' : 'PRE_PREGNANCY',
                'payload': {
                  'data': [
                    {
                      ..._item('lifecycle-stage-page', 'ARTICLE'),
                      'stage': secondPage ? 'PREGNANCY' : 'PRE_PREGNANCY',
                    },
                  ],
                  'page': secondPage ? 1 : 0,
                  'size': 50,
                  'totalElements': 2,
                  'totalPages': 2,
                },
              },
            };
          },
        );

        await expectLater(
          service.getAllLifecycleContent(),
          throwsA(isA<FormatException>()),
        );
      },
    );

    test(
      'lifecycle aggregation stops when its request becomes stale',
      () async {
        var calls = 0;
        var current = true;
        final service = ContentService(
          getRequest: (_) async {
            calls++;
            current = false;
            return {
              'data': {
                'stage': 'PRE_PREGNANCY',
                'payload': {
                  'data': [_item('stale-page', 'ARTICLE')],
                  'page': 0,
                  'size': 50,
                  'totalElements': 2,
                  'totalPages': 2,
                },
              },
            };
          },
        );

        await expectLater(
          service.getAllLifecycleContent(shouldContinue: () => current),
          throwsA(isA<StateError>()),
        );
        expect(calls, 1);
      },
    );

    test('lifecycle aggregation enforces the page-size boundary', () async {
      final service = ContentService(
        getRequest: (_) async => throw StateError('must not call network'),
      );

      await expectLater(
        service.getAllLifecycleContent(pageSize: 51),
        throwsA(isA<RangeError>()),
      );
    });
  });
}
