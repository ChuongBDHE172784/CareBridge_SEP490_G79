import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/recommendation/models/recommendation_model.dart';

Map<String, dynamic> _vaccinationAnswer(String code, String state) => {
  'code': code,
  'state': state,
};

List<Map<String, dynamic>> _answers(Map<String, dynamic> profile) {
  final vaccination = (profile['vaccination'] as Map).cast<String, dynamic>();
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
    });

    test(
      'repairs missing server vaccine answers and preserves provided data',
      () {
        final server = <String, dynamic>{
          'vaccination': <String, dynamic>{
            'answers': <Map<String, dynamic>>[
              _vaccinationAnswer('TDAP', 'KNOWN'),
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
        expect(_answers(server), [_vaccinationAnswer('TDAP', 'KNOWN')]);
      },
    );

    test('applies draft precedence by code and keeps canonical order', () {
      final server = <String, dynamic>{
        'vaccination': <String, dynamic>{
          'answers': <Map<String, dynamic>>[
            _vaccinationAnswer('TDAP', 'KNOWN'),
          ],
        },
      };
      final draft = <String, dynamic>{
        'vaccination': <String, dynamic>{
          'answers': <Map<String, dynamic>>[
            _vaccinationAnswer('COVID_19', 'KNOWN'),
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
      expect(_answers(server), [_vaccinationAnswer('TDAP', 'KNOWN')]);
      expect(_answers(draft), [
        _vaccinationAnswer('COVID_19', 'KNOWN'),
        _vaccinationAnswer('TDAP', 'PREFER_NOT_TO_SAY'),
      ]);
    });
  });
}
