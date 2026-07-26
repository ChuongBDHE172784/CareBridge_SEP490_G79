import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/community/services/content_service.dart';

Map<String, dynamic> _contentJson(String id) => {
  'id': id,
  'title': 'Nội dung $id',
  'type': 'ARTICLE',
  'stage': 'POSTPARTUM',
};

void main() {
  test('parses the live API array envelope', () {
    final items = contentItemsFromApiData([
      _contentJson('a'),
      _contentJson('b'),
    ]);

    expect(items.map((item) => item.id), ['a', 'b']);
  });

  test('also parses a Spring page envelope', () {
    final items = contentItemsFromApiData({
      'content': [_contentJson('a')],
      'last': true,
      'totalPages': 1,
    });

    expect(items.single.id, 'a');
  });

  test('uses page metadata when available and list size otherwise', () {
    expect(
      contentPageIsLast(
        data: {'content': [], 'last': false, 'totalPages': 3},
        page: 1,
        requestedPageSize: 50,
        itemCount: 50,
      ),
      isFalse,
    );
    expect(
      contentPageIsLast(
        data: {'content': [], 'last': false, 'totalPages': 3},
        page: 2,
        requestedPageSize: 50,
        itemCount: 50,
      ),
      isTrue,
    );
    expect(
      contentPageIsLast(
        data: [_contentJson('a')],
        page: 0,
        requestedPageSize: 50,
        itemCount: 1,
      ),
      isTrue,
    );
  });

  test(
    'getAllContent follows top-level pagination without losing generic filters',
    () async {
      final paths = <String>[];
      final service = ContentService(
        getRequest: (path) async {
          paths.add(path);
          final secondPage = path.contains('page=1');
          return {
            'data': [_contentJson(secondPage ? 'b' : 'a')],
            'page': secondPage ? 1 : 0,
            'size': ContentService.maxPageSize,
            'totalElements': 2,
            'totalPages': 2,
          };
        },
      );

      final result = await service.getAllContent(
        type: 'ARTICLE',
        stage: 'PRE_PREGNANCY',
        topicId: 'topic-69',
      );

      expect(result.map((item) => item.id), ['a', 'b']);
      expect(paths, hasLength(2));
      expect(paths.first, contains('type=ARTICLE'));
      expect(paths.first, contains('stage=PRE_PREGNANCY'));
      expect(paths.first, contains('topicId=topic-69'));
    },
  );

  test(
    'getAllContent follows declared pages after a duplicate-only page',
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
          return {
            'data': [_contentJson(page == 2 ? 'b' : 'a')],
            'page': page,
            'size': ContentService.maxPageSize,
            'totalElements': 2,
            'totalPages': 3,
          };
        },
      );

      final result = await service.getAllContent();

      expect(paths, hasLength(3));
      expect(result.map((item) => item.id), ['a', 'b']);
    },
  );
}
