import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/constants/content_stages.dart';
import 'package:untitled/features/community/models/content_model.dart';

void main() {
  test('maps real journey lifecycle types to content stage tabs', () {
    expect(contentStageIndexForJourneyType('PRE_PREGNANCY'), 0);
    expect(contentStageIndexForJourneyType('PREGNANCY'), 1);
    expect(contentStageIndexForJourneyType('POSTPARTUM'), 2);
    expect(contentStageIndexForJourneyType('BABY_CARE'), 2);
    expect(contentStageIndexForJourneyType(null), -1);
  });

  test('normalizes the legacy baby-care content stage', () {
    expect(normalizeContentStage('BABY_CARE'), postpartumContentStage);
    expect(contentStageLabel('BABY_CARE'), 'Hậu sản & Chăm bé');
    expect(contentStageOptions.map((stage) => stage.value), [
      prePregnancyContentStage,
      pregnancyContentStage,
      postpartumContentStage,
    ]);
  });

  test('normalizes legacy content response payloads', () {
    final content = ContentListItem.fromJson({
      'id': 'content-legacy',
      'title': 'Chăm sóc bé',
      'stage': 'BABY_CARE',
    });

    expect(content.stage, postpartumContentStage);
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
