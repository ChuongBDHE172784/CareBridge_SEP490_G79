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
}
