import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/community/models/content_model.dart';

void main() {
  test('maps real journey lifecycle types to content stage tabs', () {
    expect(contentStageIndexForJourneyType('PRE_PREGNANCY'), 0);
    expect(contentStageIndexForJourneyType('PREGNANCY'), 1);
    expect(contentStageIndexForJourneyType('POSTPARTUM'), 2);
    expect(contentStageIndexForJourneyType('BABY_CARE'), 3);
    expect(contentStageIndexForJourneyType(null), -1);
  });

  test('extracts image sources from the server-sanitized content body', () {
    final content = ContentDetail.fromJson({
      'id': 'content-1',
      'title': 'Bài viết có ảnh',
      'body': '<p>Nội dung</p><img src="/api/v1/files/cover.png">',
    });

    expect(content.imageUrls, ['/api/v1/files/cover.png']);
  });

  test('does not treat a lazy data-src attribute as an image source', () {
    final content = ContentDetail.fromJson({
      'id': 'content-1',
      'title': 'Bài viết',
      'body': '<img data-src="/lazy.png" src=/api/v1/files/cover.png>',
    });

    expect(content.imageUrls, ['/api/v1/files/cover.png']);
  });
}
