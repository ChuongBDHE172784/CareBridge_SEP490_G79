import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/recommendation/models/recommendation_model.dart';

Map<String, dynamic> _vaccinationAnswer(
  String code,
  String state, {
  String? value,
}) {
  final answer = <String, dynamic>{'code': code, 'state': state};
  if (value != null) answer['value'] = value;
  return answer;
}

List<Map<String, dynamic>> _answers(Map<String, dynamic> profile) {
  expect(profile['vaccination'], isA<Map<String, dynamic>>());
  final vaccination = (profile['vaccination'] as Map).cast<String, dynamic>();
  expect(vaccination['answers'], isA<List<Map<String, dynamic>>>());
  return (vaccination['answers'] as List)
      .map((answer) => (answer as Map).cast<String, dynamic>())
      .toList(growable: false);
}

void main() {
  const vaccineCodes = [
    'INFLUENZA',
    'COVID_19',
    'TDAP',
    'HEPATITIS_B',
    'RUBELLA_IMMUNITY',
  ];

  group('RecommendationProfileDraft vaccination repair', () {
    test('merges a fresh profile without a runtime type error', () {
      final merged = RecommendationProfileDraft.mergeProfiles(null, null);

      expect(recommendationProfileHasAllDomains(merged), isTrue);
      expect(
        _answers(merged).map((answer) => answer['code']).toList(),
        vaccineCodes,
      );
      expect(
        _answers(merged).every((answer) => answer['state'] == 'UNKNOWN'),
        isTrue,
      );

      expect(
        _answers(RecommendationProfileDraft.copyProfile(null)),
        _answers(merged),
      );
    });

    test(
      'repairs missing server vaccine answers and preserves provided data',
      () {
        final server = <String, dynamic>{
          'vaccination': <String, dynamic>{
            'answers': <Map<String, dynamic>>[
              _vaccinationAnswer('TDAP', 'KNOWN', value: 'DUE'),
            ],
          },
        };

        final merged = RecommendationProfileDraft.mergeProfiles(server, null);
        final answersByCode = {
          for (final answer in _answers(merged)) answer['code']: answer,
        };

        expect(answersByCode, hasLength(vaccineCodes.length));
        expect(answersByCode['TDAP']!['state'], 'KNOWN');
        expect(answersByCode['INFLUENZA']!['state'], 'UNKNOWN');
        expect(_answers(server), [
          _vaccinationAnswer('TDAP', 'KNOWN', value: 'DUE'),
        ]);

        answersByCode['TDAP']!['state'] = 'PREFER_NOT_TO_SAY';
        expect(_answers(server), [
          _vaccinationAnswer('TDAP', 'KNOWN', value: 'DUE'),
        ]);
      },
    );

    test('applies draft precedence by code and keeps canonical order', () {
      final server = <String, dynamic>{
        'vaccination': <String, dynamic>{
          'answers': <Map<String, dynamic>>[
            _vaccinationAnswer('TDAP', 'KNOWN', value: 'UP_TO_DATE'),
          ],
        },
      };
      final draft = <String, dynamic>{
        'vaccination': <String, dynamic>{
          'answers': <Map<String, dynamic>>[
            _vaccinationAnswer('COVID_19', 'KNOWN', value: 'DUE'),
            _vaccinationAnswer('TDAP', 'PREFER_NOT_TO_SAY'),
          ],
        },
      };

      final merged = RecommendationProfileDraft.mergeProfiles(server, draft);
      final answers = _answers(merged);

      expect(answers.map((answer) => answer['code']).toList(), vaccineCodes);
      expect(
        answers.singleWhere((answer) => answer['code'] == 'COVID_19')['state'],
        'KNOWN',
      );
      expect(
        answers.singleWhere((answer) => answer['code'] == 'TDAP')['state'],
        'PREFER_NOT_TO_SAY',
      );
      expect(_answers(server), [
        _vaccinationAnswer('TDAP', 'KNOWN', value: 'UP_TO_DATE'),
      ]);
      expect(_answers(draft), [
        _vaccinationAnswer('COVID_19', 'KNOWN', value: 'DUE'),
        _vaccinationAnswer('TDAP', 'PREFER_NOT_TO_SAY'),
      ]);
    });

    test('repairs decoded and malformed vaccination payloads safely', () {
      final server =
          jsonDecode(
                '{"vaccination":{"answers":[null,{"code":12},'
                '{"code":"INFLUENZA","unexpected":true},'
                '{"code":"TDAP","state":"KNOWN","value":"DUE"}],'
                '"note":"keep"}}',
              )
              as Map<String, dynamic>;

      final merged = RecommendationProfileDraft.mergeProfiles(server, null);
      final vaccination = (merged['vaccination'] as Map)
          .cast<String, dynamic>();

      expect(_answers(merged).map((answer) => answer['code']).toList(), [
        'INFLUENZA',
        'COVID_19',
        'TDAP',
        'HEPATITIS_B',
        'RUBELLA_IMMUNITY',
      ]);
      expect(
        _answers(
          merged,
        ).singleWhere((answer) => answer['code'] == 'TDAP')['state'],
        'KNOWN',
      );
      expect(vaccination['note'], 'keep');
    });
  });

  group('RecommendationContentItem.fromJson reasonLabel localization', () {
    test('localizes LIFECYCLE_FALLBACK to Vietnamese label', () {
      final item = RecommendationContentItem.fromJson({
        'rank': 1,
        'selectionType': 'FALLBACK',
        'reasonCode': 'LIFECYCLE_FALLBACK',
        'reasonLabel': 'Useful for your current stage',
        'content': {'id': 'art-1', 'title': 'Bài viết 1'},
      });

      expect(item.reasonLabel, 'Hữu ích cho giai đoạn hiện tại của bạn');
    });

    test('localizes PERSONALIZED_CONTEXT to Vietnamese label', () {
      final item = RecommendationContentItem.fromJson({
        'rank': 1,
        'selectionType': 'TARGETED',
        'reasonCode': 'PERSONALIZED_CONTEXT',
        'reasonLabel': 'Selected for your current care context',
        'content': {'id': 'art-2', 'title': 'Bài viết 2'},
      });

      expect(item.reasonLabel, 'Phù hợp với ngữ cảnh chăm sóc của bạn');
    });
  });
}
