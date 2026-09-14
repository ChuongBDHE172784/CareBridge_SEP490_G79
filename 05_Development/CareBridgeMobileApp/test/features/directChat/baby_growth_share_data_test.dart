import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/widgets/baby_growth_message_card.dart';

import 'baby_growth_test_factory.dart';

void main() {
  test(
    'SBG-TC-006 serialized share is tagged, bounded and carries no history',
    () {
      final data = makeShareData(note: 'a' * 500, nickname: 'B' * 100);

      final body = data.serialize();

      expect(body.startsWith(BabyGrowthShareData.tag), isTrue);
      expect(body.trim().length, lessThanOrEqualTo(2000));
      final decoded =
          jsonDecode(body.substring(BabyGrowthShareData.tag.length).trim())
              as Map<String, dynamic>;
      expect(decoded.containsKey('measurements'), isFalse);
      expect(decoded.containsKey('history'), isFalse);
      expect(decoded['babyId'], kTestBabyId);
      expect(BabyGrowthShareData.fitsMessageLimit('x' * 2001), isFalse);
      expect(BabyGrowthShareData.fitsMessageLimit('x' * 2000), isTrue);
    },
  );

  test('SBG-TC-007 parse rejects non-tag, malformed and babyId-less bodies', () {
    const invalid = <String?>[
      null,
      'hello',
      '[CAREBRIDGE_HEALTH_SHARE]\n{}',
      '[CAREBRIDGE_BABY_GROWTH_SHARE]\n{bad',
      '[CAREBRIDGE_BABY_GROWTH_SHARE]\n{"babyNickname":"x"}',
    ];
    for (final body in invalid) {
      expect(BabyGrowthShareData.parse(body), isNull, reason: '$body');
    }

    final parsed = BabyGrowthShareData.parse(makeShareData().serialize());

    expect(parsed, isNotNull);
    expect(parsed!.babyNickname, 'Bé Test');
    expect(parsed.measurementCount, 3);
    expect(parsed.latest!.weightKg, 5.6);
    expect(parsed.isLiveSync, isTrue);
  });
}
