import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/recommendation/models/recommendation_model.dart';
import 'package:untitled/features/recommendation/models/recommendation_questionnaire.dart';

void main() {
  test('catalog has one ordered question for every answer node', () {
    expect(RecommendationQuestionnaire.questionCount, 9);
    expect(
      RecommendationQuestionnaire.questions.map((question) => question.id),
      [
        'age',
        'bmi',
        'reproductiveHistory',
        'underlyingConditions',
        'lifestyle',
        'nutrition',
        'vaccination',
        'currentMedications',
        'sexualHealth',
      ],
    );
    expect(
      recommendationVietnameseLabels.values.every(
        (label) => label.trim().isNotEmpty,
      ),
      isTrue,
    );
    expect(RecommendationQuestionnaire.labelFor('DUE'), 'Đến hạn tiêm');
    expect(
      RecommendationQuestionnaire.labelFor('PRIOR_PREECLAMPSIA'),
      'Từng bị tiền sản giật',
    );
    expect(RecommendationQuestionnaire.labelFor('UNKNOWN'), 'Lựa chọn');
    expect(
      RecommendationQuestionnaire.lifestyleGroupOptions,
      containsAll(<String>[
        'SMOKING',
        'ALCOHOL_USE',
        'SUBSTANCE_USE',
        'SLEEP_CONCERN',
        'STRESS',
        'LOW_ACTIVITY',
        'UNHEALTHY_DIET',
        'NONE_KNOWN_LIFESTYLE',
      ]),
    );
    expect(
      RecommendationQuestionnaire.vaccinationGroupOptions,
      containsAll(<String>[
        'NOT_ASSESSED',
        'RUBELLA_NONIMMUNE',
        'HEPATITIS_B_INCOMPLETE',
        'INFLUENZA_DUE',
        'COVID_19_UPDATE',
        'NONE_KNOWN_VACCINATION',
      ]),
    );
    expect(
      RecommendationQuestionnaire.codeOptions['sexualHealth'],
      <String>[
        'SAFE_SEX_COUNSELING_NEEDED',
        'STI_RISK',
        'REPRODUCTIVE_TRACT_INFECTION',
        'STI_SUSPECTED_OR_KNOWN',
        'NO_PREGNANCY_PLAN',
      ],
    );
    expect(
      RecommendationQuestionnaire.labelFor('PRIOR_PREGNANCY_LOSS'),
      'Từng sảy thai',
    );
    expect(
      RecommendationQuestionnaire.labelFor('NONE_KNOWN'),
      'Không thuộc các trường hợp trên',
    );
  });

  test('legacy disclosure states become an explicit unknown skip', () {
    final source = <String, dynamic>{
      'age': {'state': 'PREFER_NOT_TO_SAY'},
      'bmi': {'state': 'NOT_APPLICABLE', 'heightCm': 160.0, 'weightKg': 55.0},
      'lifestyle': {
        'smoking': {'state': 'PREFER_NOT_TO_SAY', 'value': 'CURRENT'},
      },
      'vaccination': {
        'answers': [
          {'code': 'TDAP', 'state': 'NOT_APPLICABLE'},
        ],
      },
      'sti': {
        'state': 'PREFER_NOT_TO_SAY',
        'status': 'PAST_HISTORY',
        'infectionCodes': ['HIV'],
      },
    };

    final normalized = RecommendationQuestionnaire.normalizeProfile(source);

    expect((normalized['age'] as Map)['state'], 'UNKNOWN');
    expect(normalized['bmi'], {'state': 'UNKNOWN'});
    expect((normalized['lifestyle'] as Map)['smoking'], {'state': 'UNKNOWN'});
    final tdap = ((normalized['vaccination'] as Map)['answers'] as List)
        .whereType<Map>()
        .singleWhere((answer) => answer['code'] == 'TDAP');
    expect(tdap, {'code': 'TDAP', 'state': 'UNKNOWN'});
    expect(normalized['sti'], {'state': 'UNKNOWN'});
    expect(
      normalized.values.whereType<Map>().every((value) {
        final state = value['state'];
        return state == null || state == 'KNOWN' || state == 'UNKNOWN';
      }),
      isTrue,
    );
  });

  test('container domains never receive an answer state', () {
    final normalized = RecommendationQuestionnaire.normalizeProfile({
      'unexpectedRoot': true,
      'lifestyle': {'state': 'UNKNOWN', 'note': 'stale'},
      'vaccination': {'state': 'UNKNOWN', 'note': 'stale'},
    });

    expect(normalized.containsKey('unexpectedRoot'), isFalse);
    expect(normalized['lifestyle'], isNot(contains('state')));
    expect(normalized['lifestyle'], isNot(contains('note')));
    expect(normalized['vaccination'], isNot(contains('state')));
    expect(normalized['vaccination'], isNot(contains('note')));
    expect(
      ((normalized['lifestyle'] as Map)['smoking'] as Map)['state'],
      'UNKNOWN',
    );
    expect(((normalized['vaccination'] as Map)['answers'] as List).length, 5);
  });

  test('skipping a question clears its dependent values', () {
    final source = RecommendationProfileDraft.mergeProfiles(null, null);
    source['nutrition'] = {
      'state': 'KNOWN',
      'codes': ['VEGAN'],
    };

    final skipped = RecommendationQuestionnaire.skipQuestion(
      source,
      RecommendationQuestionnaire.questions.singleWhere(
        (question) => question.id == 'nutrition',
      ),
    );

    expect(skipped['nutrition'], {'state': 'UNKNOWN'});
    expect(
      RecommendationQuestionnaire.isComplete(
        skipped,
        RecommendationQuestionnaire.questions.singleWhere(
          (question) => question.id == 'nutrition',
        ),
      ),
      isTrue,
    );
  });

  test('grouped sexual-health answers derive the STI status domain', () {
    var profile = RecommendationProfileDraft.empty();
    profile['sexualHealth'] = {
      'state': 'KNOWN',
      'codes': ['STI_RISK'],
    };
    profile['sti'] = {'state': 'KNOWN', 'status': 'AT_RISK'};

    final question = RecommendationQuestionnaire.questions.singleWhere(
      (item) => item.id == 'sexualHealth',
    );
    expect(RecommendationQuestionnaire.isComplete(profile, question), isTrue);
    final skipped = RecommendationQuestionnaire.skipQuestion(profile, question);
    expect(skipped['sexualHealth'], {'state': 'UNKNOWN'});
    expect(skipped['sti'], {'state': 'UNKNOWN'});
  });

  test(
    'BMI completeness rejects non-finite values and uses the supplied day',
    () {
      final question = RecommendationQuestionnaire.questions.singleWhere(
        (item) => item.id == 'bmi',
      );
      final profile = RecommendationProfileDraft.empty();
      profile['bmi'] = {
        'state': 'KNOWN',
        'heightCm': double.nan,
        'weightKg': 55.0,
        'weightContext': 'PRE_PREGNANCY',
        'measuredOn': '2026-08-03',
      };

      expect(
        RecommendationQuestionnaire.isComplete(
          profile,
          question,
          today: DateTime(2026, 8, 3),
        ),
        isFalse,
      );

      profile['bmi'] = {
        'state': 'KNOWN',
        'heightCm': 160.0,
        'weightKg': 55.0,
        'weightContext': 'PRE_PREGNANCY',
        'measuredOn': '2026-08-04',
      };
      expect(
        RecommendationQuestionnaire.isComplete(
          profile,
          question,
          today: DateTime(2026, 8, 3),
        ),
        isFalse,
      );
    },
  );

  test('skipping every question produces a complete canonical replacement', () {
    var profile = RecommendationProfileDraft.empty();
    for (final question in RecommendationQuestionnaire.questions) {
      profile = RecommendationQuestionnaire.skipQuestion(profile, question);
    }

    expect(
      RecommendationQuestionnaire.questions.every(
        (question) => RecommendationQuestionnaire.isComplete(profile, question),
      ),
      isTrue,
    );
    expect(
      profile.values.whereType<Map>().every(
        (value) =>
            value['state'] == null ||
            value['state'] == 'KNOWN' ||
            value['state'] == 'UNKNOWN',
      ),
      isTrue,
    );
    expect(
      ((profile['vaccination'] as Map)['answers'] as List).length,
      RecommendationQuestionnaire.vaccineCodes.length,
    );
  });
}
