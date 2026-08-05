import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/baby/models/milestone_model.dart';

void main() {
  test('serializes the milestone types accepted by the API', () {
    expect(MilestoneType.roll.toApiValue(), 'ROLLING');
    expect(MilestoneType.crawl.toApiValue(), 'CRAWLING');
    expect(MilestoneType.walk.toApiValue(), 'WALKING');
    expect(MilestoneType.talk.toApiValue(), 'SPEAKING');
    expect(MilestoneType.tooth.toApiValue(), 'TEETHING');
    expect(MilestoneType.solids.toApiValue(), 'WEANING');
    expect(MilestoneType.other.toApiValue(), 'OTHER');
  });

  test('builds a create request with the API contract', () {
    final request = AddMilestoneRequest(
      milestoneType: MilestoneType.walk,
      achievedDate: DateTime(2026, 8, 5, 14, 30),
      note: 'Bé tự bước vài bước.',
    );

    expect(request.toJson(), {
      'milestoneType': 'WALKING',
      'achievedDate': '2026-08-05',
      'note': 'Bé tự bước vài bước.',
      'sourceType': 'MOTHER_INPUT',
    });
  });

  test('parses canonical and legacy milestone values', () {
    expect(MilestoneTypeExtension.fromApi('ROLLING'), MilestoneType.roll);
    expect(MilestoneTypeExtension.fromApi('CRAWLING'), MilestoneType.crawl);
    expect(MilestoneTypeExtension.fromApi('WALKING'), MilestoneType.walk);
    expect(MilestoneTypeExtension.fromApi('SPEAKING'), MilestoneType.talk);
    expect(MilestoneTypeExtension.fromApi('TEETHING'), MilestoneType.tooth);
    expect(MilestoneTypeExtension.fromApi('WEANING'), MilestoneType.solids);
    expect(MilestoneTypeExtension.fromApi('  walking '), MilestoneType.walk);

    // Older records used the short mobile values; they must remain readable.
    expect(MilestoneTypeExtension.fromApi('ROLL'), MilestoneType.roll);
    expect(MilestoneTypeExtension.fromApi('SOLIDS'), MilestoneType.solids);
  });
}
