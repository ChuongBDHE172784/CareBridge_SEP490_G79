import 'recommendation_model.dart';

/// The small, two-state adapter used by the progressive questionnaire.
///
/// The recommendation API still accepts its historical four-state vocabulary
/// for backwards compatibility.  The questionnaire deliberately exposes only
/// an answered value (KNOWN) or an explicit skip (UNKNOWN).  This boundary is
/// also where old drafts/profiles are repaired before they are rendered or
/// submitted again.
enum RecommendationQuestionKind {
  age,
  bmi,
  codeSet,
  lifestyle,
  vaccination,
  sti,
}

class RecommendationQuestion {
  const RecommendationQuestion({
    required this.id,
    required this.domain,
    required this.title,
    required this.kind,
    this.code,
  });

  final String id;
  final String domain;
  final String title;
  final RecommendationQuestionKind kind;
  final String? code;
}

class RecommendationQuestionnaire {
  const RecommendationQuestionnaire._();

  static const vaccineCodes = <String>[
    'INFLUENZA',
    'COVID_19',
    'TDAP',
    'HEPATITIS_B',
    'RUBELLA_IMMUNITY',
  ];

  static const questions = <RecommendationQuestion>[
    RecommendationQuestion(
      id: 'age',
      domain: 'age',
      title: 'Ngày sinh của bạn là ngày nào?',
      kind: RecommendationQuestionKind.age,
    ),
    RecommendationQuestion(
      id: 'bmi',
      domain: 'bmi',
      title: 'Vui lòng nhập cân nặng và chiều cao hiện tại của bạn.',
      kind: RecommendationQuestionKind.bmi,
    ),
    RecommendationQuestion(
      id: 'reproductiveHistory',
      domain: 'reproductiveHistory',
      title: 'Bạn có tiền sử sinh sản nào dưới đây?',
      kind: RecommendationQuestionKind.codeSet,
    ),
    RecommendationQuestion(
      id: 'underlyingConditions',
      domain: 'underlyingConditions',
      title:
          'Bạn hiện đang mắc hoặc từng được chẩn đoán mắc bệnh nào dưới đây?',
      kind: RecommendationQuestionKind.codeSet,
    ),
    RecommendationQuestion(
      id: 'lifestyle',
      domain: 'lifestyle',
      title: 'Bạn hiện có những thói quen hoặc tình trạng nào dưới đây?',
      kind: RecommendationQuestionKind.lifestyle,
    ),
    RecommendationQuestion(
      id: 'nutrition',
      domain: 'nutrition',
      title: 'Bạn hiện có tình trạng nào dưới đây về dinh dưỡng và vi chất?',
      kind: RecommendationQuestionKind.codeSet,
    ),
    RecommendationQuestion(
      id: 'vaccination',
      domain: 'vaccination',
      title: 'Tình trạng tiêm chủng trước khi mang thai của bạn như thế nào?',
      kind: RecommendationQuestionKind.vaccination,
    ),
    RecommendationQuestion(
      id: 'currentMedications',
      domain: 'currentMedications',
      title: 'Bạn hiện có tình trạng nào liên quan đến việc sử dụng thuốc?',
      kind: RecommendationQuestionKind.codeSet,
    ),
    RecommendationQuestion(
      id: 'sexualHealth',
      domain: 'sexualHealth',
      title:
          'Bạn hiện có tình trạng nào dưới đây liên quan đến sức khỏe tình dục?',
      kind: RecommendationQuestionKind.codeSet,
    ),
  ];

  static int get questionCount => questions.length;

  /// Every controlled wire value rendered by the questionnaire has a
  /// Vietnamese label.  The map intentionally includes stage/context values
  /// used by the BMI follow-up as well as the STI infection choices.
  static const labels = <String, String>{
    // Reproductive history
    'NO_PRIOR_PREGNANCY': 'Chưa từng mang thai',
    'PRIOR_LIVE_BIRTH': 'Từng sinh con sống',
    'PRIOR_PREGNANCY_LOSS': 'Từng sảy thai',
    'PRIOR_RECURRENT_PREGNANCY_LOSS': 'Từng sảy thai nhiều lần',
    'PRIOR_STILLBIRTH': 'Từng thai lưu',
    'PRIOR_PRETERM_BIRTH': 'Từng sinh non',
    'PRIOR_MULTIPLE_PREGNANCY': 'Từng mang đa thai',
    'PRIOR_ECTOPIC_PREGNANCY': 'Từng mang thai ngoài tử cung',
    'PRIOR_PREECLAMPSIA': 'Từng bị tiền sản giật',
    'PRIOR_GESTATIONAL_DIABETES': 'Từng mắc đái tháo đường thai kỳ',
    'NO_LISTED_REPRODUCTIVE_HISTORY': 'Không thuộc các trường hợp trên',
    'OTHER_HISTORY': 'Thông tin khác',
    // Conditions
    'NONE_KNOWN': 'Không thuộc các trường hợp trên',
    'HYPERTENSION': 'Tăng huyết áp',
    'DIABETES': 'Đái tháo đường',
    'THYROID_DISORDER': 'Bệnh tuyến giáp',
    'CARDIOVASCULAR_DISEASE': 'Bệnh tim',
    'ASTHMA': 'Hen',
    'EPILEPSY': 'Động kinh',
    'KIDNEY_DISEASE': 'Bệnh thận',
    'AUTOIMMUNE_DISEASE': 'Bệnh tự miễn',
    'LUPUS': 'Lupus',
    'ANEMIA': 'Thiếu máu',
    'PCOS': 'PCOS',
    'ENDOMETRIOSIS': 'Lạc nội mạc tử cung',
    'INFERTILITY': 'Hiếm muộn',
    'MENTAL_HEALTH_CONDITION': 'Tình trạng sức khỏe tâm thần',
    'OTHER_CLINICIAN_CONFIRMED': 'Bệnh khác đã được xác nhận',
    // Lifestyle
    'NEVER': 'Chưa bao giờ',
    'FORMER': 'Đã từng, hiện đã bỏ',
    'CURRENT': 'Hiện đang hút thuốc',
    'NONE': 'Không thuộc các trường hợp trên',
    'LESS_THAN_WEEKLY': 'Ít hơn mỗi tuần',
    'WEEKLY_OR_MORE': 'Mỗi tuần hoặc thường xuyên hơn',
    'ANY_USE': 'Có sử dụng',
    'LOW': 'Thấp',
    'MODERATE': 'Vừa phải',
    'HIGH': 'Cao',
    'NO_CONCERN': 'Không có lo lắng',
    'CONCERN': 'Có lo lắng',
    // Grouped lifestyle flags
    'SMOKING': 'Hút thuốc',
    'ALCOHOL_USE': 'Uống rượu bia',
    'SUBSTANCE_USE': 'Sử dụng ma túy hoặc chất kích thích',
    'SLEEP_CONCERN': 'Thiếu ngủ',
    'STRESS': 'Stress',
    'LOW_ACTIVITY': 'Ít vận động',
    'UNHEALTHY_DIET': 'Chế độ ăn không lành mạnh',
    'NONE_KNOWN_LIFESTYLE': 'Không thuộc các trường hợp trên',
    // Nutrition
    'NO_CURRENT_CONCERN': 'Không thuộc các trường hợp trên',
    'VEGETARIAN': 'Ăn chay',
    'VEGAN': 'Thuần chay',
    'FOOD_INSECURITY': 'Khó bảo đảm đủ thực phẩm',
    'LOW_APPETITE': 'Chán ăn',
    'NAUSEA_OR_VOMITING': 'Buồn nôn hoặc nôn',
    'IRON_OR_FOLATE_CONCERN': 'Lo lắng về sắt hoặc folate',
    'FOLIC_ACID_NOT_STARTED': 'Chưa sử dụng acid folic',
    'IODINE_UNASSESSED_OR_INSUFFICIENT':
        'Chưa được đánh giá nguồn iod hoặc có thể không đủ iod',
    'VITAMIN_D_INSUFFICIENT_OR_SUPPLEMENT':
        'Có thể không đủ vitamin D hoặc có chỉ định bổ sung',
    'IRON_INSUFFICIENT_OR_SUPPLEMENT':
        'Có thể thiếu sắt hoặc có chỉ định bổ sung',
    'CALCIUM_INSUFFICIENT_OR_SUPPLEMENT':
        'Có thể không đủ canxi hoặc có chỉ định bổ sung',
    'OTHER_NUTRITION_CONCERN': 'Mối quan tâm dinh dưỡng khác',
    // Vaccination
    'INFLUENZA': 'Cúm',
    'COVID_19': 'COVID-19',
    'TDAP': 'Tdap (uốn ván, bạch hầu, ho gà)',
    'HEPATITIS_B': 'Viêm gan B',
    'RUBELLA_IMMUNITY': 'Miễn dịch rubella',
    'NOT_ASSESSED': 'Chưa được đánh giá tình trạng tiêm chủng',
    'RUBELLA_NONIMMUNE': 'Chưa có miễn dịch Rubella/MMR',
    'HEPATITIS_B_INCOMPLETE': 'Chưa được bảo vệ đầy đủ với viêm gan B',
    'INFLUENZA_DUE': 'Cần tiêm cúm mùa',
    'COVID_19_UPDATE': 'Cần cập nhật vắc xin COVID-19',
    'NONE_KNOWN_VACCINATION': 'Không thuộc các trường hợp trên',
    'UP_TO_DATE': 'Đã tiêm đủ / đang cập nhật',
    'DUE': 'Đến hạn tiêm',
    'NOT_RECEIVED': 'Chưa tiêm',
    // Current medications
    'PRENATAL_VITAMIN': 'Vitamin dành cho thai kỳ',
    'FOLIC_ACID': 'A-xít folic',
    'IRON': 'Sắt',
    'THYROID_MEDICATION': 'Thuốc tuyến giáp',
    'DIABETES_MEDICATION': 'Thuốc tiểu đường',
    'ANTIHYPERTENSIVE': 'Thuốc hạ huyết áp',
    'ANTICOAGULANT': 'Thuốc chống đông',
    'ANTIEPILEPTIC': 'Thuốc chống động kinh',
    'MENTAL_HEALTH_MEDICATION': 'Thuốc sức khỏe tâm thần',
    'OTHER_PRESCRIBED': 'Thuốc kê đơn khác',
    'HIGH_RISK_OR_CONTRAINDICATED':
        'Đang dùng thuốc chống chỉ định hoặc có nguy cơ cao khi mang thai',
    'NEEDS_ADJUSTMENT': 'Đang dùng thuốc cần điều chỉnh',
    'NONE_KNOWN_MEDICATION': 'Không thuộc các trường hợp trên',
    // Sexual health
    'NO_CURRENT_INFORMATION_NEED': 'Hiện không cần thêm thông tin',
    'GENERAL_INFORMATION': 'Thông tin chung',
    'CONTRACEPTION_OR_FERTILITY': 'Tránh thai hoặc khả năng sinh sản',
    'INTIMACY_DURING_LIFECYCLE': 'Thân mật trong các giai đoạn',
    'OTHER_NON_URGENT_INFORMATION': 'Thông tin không khẩn cấp khác',
    'SAFE_SEX_COUNSELING_NEEDED': 'Chưa được tư vấn về tình dục an toàn',
    'STI_RISK': 'Có nguy cơ mắc bệnh lây truyền qua đường tình dục (STIs)',
    'REPRODUCTIVE_TRACT_INFECTION':
        'Nghi ngờ hoặc mắc nhiễm khuẩn đường sinh sản',
    'STI_SUSPECTED_OR_KNOWN':
        'Nghi ngờ hoặc mắc bệnh lây truyền qua đường tình dục',
    'NO_PREGNANCY_PLAN': 'Chưa có kế hoạch mang thai',
    // STI
    'NO_KNOWN_HISTORY': 'Không có tiền sử đã biết',
    'SCREENING_INFORMATION': 'Thông tin xét nghiệm sàng lọc',
    'PAST_HISTORY': 'Tiền sử trước đây',
    'CURRENT_OR_UNDER_TREATMENT': 'Hiện tại hoặc đang điều trị',
    'AT_RISK': 'Có nguy cơ',
    'SUSPECTED_OR_KNOWN': 'Nghi ngờ hoặc mắc',
    'HIV': 'HIV',
    'SYPHILIS': 'Giang mai',
    'HEPATITIS_C': 'Viêm gan C',
    'CHLAMYDIA': 'Chlamydia',
    'GONORRHEA': 'Lậu',
    'HERPES': 'Herpes',
    'HPV': 'HPV',
    'OTHER': 'Khác',
    // BMI contexts
    'PRE_PREGNANCY': 'Trước khi mang thai',
    'CURRENT_NON_PREGNANT': 'Hiện tại, không mang thai',
    'CURRENT_PREGNANCY': 'Trong thai kỳ hiện tại',
    'CURRENT_POSTPARTUM': 'Trong giai đoạn hậu sản hiện tại',
  };

  /// Choices rendered by the nine-group questionnaire.  Legacy values remain
  /// accepted below so loading an older profile never silently drops data.
  static const codeOptions = <String, List<String>>{
    'reproductiveHistory': [
      'NO_PRIOR_PREGNANCY',
      'PRIOR_PREGNANCY_LOSS',
      'PRIOR_RECURRENT_PREGNANCY_LOSS',
      'PRIOR_STILLBIRTH',
      'PRIOR_PRETERM_BIRTH',
      'PRIOR_ECTOPIC_PREGNANCY',
      'PRIOR_PREECLAMPSIA',
      'PRIOR_GESTATIONAL_DIABETES',
      'NO_LISTED_REPRODUCTIVE_HISTORY',
    ],
    'underlyingConditions': [
      'DIABETES',
      'HYPERTENSION',
      'CARDIOVASCULAR_DISEASE',
      'KIDNEY_DISEASE',
      'THYROID_DISORDER',
      'ASTHMA',
      'EPILEPSY',
      'LUPUS',
      'ANEMIA',
      'PCOS',
      'ENDOMETRIOSIS',
      'INFERTILITY',
      'NONE_KNOWN',
    ],
    'nutrition': [
      'FOLIC_ACID_NOT_STARTED',
      'IODINE_UNASSESSED_OR_INSUFFICIENT',
      'VITAMIN_D_INSUFFICIENT_OR_SUPPLEMENT',
      'IRON_INSUFFICIENT_OR_SUPPLEMENT',
      'CALCIUM_INSUFFICIENT_OR_SUPPLEMENT',
      'NO_CURRENT_CONCERN',
    ],
    'currentMedications': [
      'HIGH_RISK_OR_CONTRAINDICATED',
      'NEEDS_ADJUSTMENT',
      'NONE',
    ],
    'sexualHealth': [
      'SAFE_SEX_COUNSELING_NEEDED',
      'STI_RISK',
      'REPRODUCTIVE_TRACT_INFECTION',
      'STI_SUSPECTED_OR_KNOWN',
      'NO_PREGNANCY_PLAN',
    ],
  };

  /// Historical values accepted while repairing an already-saved profile.
  /// These are deliberately not rendered by the new questionnaire.
  static const acceptedCodeOptions = <String, List<String>>{
    'reproductiveHistory': [
      'NO_PRIOR_PREGNANCY',
      'PRIOR_LIVE_BIRTH',
      'PRIOR_PREGNANCY_LOSS',
      'PRIOR_RECURRENT_PREGNANCY_LOSS',
      'PRIOR_STILLBIRTH',
      'PRIOR_PRETERM_BIRTH',
      'PRIOR_MULTIPLE_PREGNANCY',
      'PRIOR_ECTOPIC_PREGNANCY',
      'PRIOR_PREECLAMPSIA',
      'PRIOR_GESTATIONAL_DIABETES',
      'NO_LISTED_REPRODUCTIVE_HISTORY',
      'OTHER_HISTORY',
    ],
    'underlyingConditions': [
      'NONE_KNOWN',
      'HYPERTENSION',
      'DIABETES',
      'THYROID_DISORDER',
      'CARDIOVASCULAR_DISEASE',
      'ASTHMA',
      'EPILEPSY',
      'KIDNEY_DISEASE',
      'AUTOIMMUNE_DISEASE',
      'LUPUS',
      'ANEMIA',
      'PCOS',
      'ENDOMETRIOSIS',
      'INFERTILITY',
      'MENTAL_HEALTH_CONDITION',
      'OTHER_CLINICIAN_CONFIRMED',
    ],
    'nutrition': [
      'NO_CURRENT_CONCERN',
      'VEGETARIAN',
      'VEGAN',
      'FOOD_INSECURITY',
      'LOW_APPETITE',
      'NAUSEA_OR_VOMITING',
      'IRON_OR_FOLATE_CONCERN',
      'FOLIC_ACID_NOT_STARTED',
      'IODINE_UNASSESSED_OR_INSUFFICIENT',
      'VITAMIN_D_INSUFFICIENT_OR_SUPPLEMENT',
      'IRON_INSUFFICIENT_OR_SUPPLEMENT',
      'CALCIUM_INSUFFICIENT_OR_SUPPLEMENT',
      'OTHER_NUTRITION_CONCERN',
    ],
    'currentMedications': [
      'NONE',
      'PRENATAL_VITAMIN',
      'FOLIC_ACID',
      'IRON',
      'THYROID_MEDICATION',
      'DIABETES_MEDICATION',
      'ANTIHYPERTENSIVE',
      'ANTICOAGULANT',
      'ANTIEPILEPTIC',
      'MENTAL_HEALTH_MEDICATION',
      'HIGH_RISK_OR_CONTRAINDICATED',
      'NEEDS_ADJUSTMENT',
      'OTHER_PRESCRIBED',
    ],
    'sexualHealth': [
      'NO_CURRENT_INFORMATION_NEED',
      'GENERAL_INFORMATION',
      'CONTRACEPTION_OR_FERTILITY',
      'INTIMACY_DURING_LIFECYCLE',
      'SAFE_SEX_COUNSELING_NEEDED',
      'STI_RISK',
      'REPRODUCTIVE_TRACT_INFECTION',
      'STI_SUSPECTED_OR_KNOWN',
      'NO_PREGNANCY_PLAN',
      'OTHER_NON_URGENT_INFORMATION',
    ],
  };

  static const lifestyleOptions = <String, List<String>>{
    'smoking': ['NEVER', 'FORMER', 'CURRENT'],
    'alcohol': ['NONE', 'LESS_THAN_WEEKLY', 'WEEKLY_OR_MORE', 'ANY_USE'],
    'physicalActivity': ['LOW', 'MODERATE', 'HIGH'],
    'sleep': ['NO_CONCERN', 'CONCERN'],
  };

  static const lifestyleGroupOptions = <String>[
    'SMOKING',
    'ALCOHOL_USE',
    'SUBSTANCE_USE',
    'SLEEP_CONCERN',
    'STRESS',
    'LOW_ACTIVITY',
    'UNHEALTHY_DIET',
    'NONE_KNOWN_LIFESTYLE',
  ];

  static const vaccinationGroupOptions = <String>[
    'NOT_ASSESSED',
    'RUBELLA_NONIMMUNE',
    'HEPATITIS_B_INCOMPLETE',
    'INFLUENZA_DUE',
    'COVID_19_UPDATE',
    'NONE_KNOWN_VACCINATION',
  ];

  static const lifestyleFlagValues = {
    'SMOKING',
    'ALCOHOL_USE',
    'SUBSTANCE_USE',
    'SLEEP_CONCERN',
    'STRESS',
    'LOW_ACTIVITY',
    'UNHEALTHY_DIET',
  };
  static const vaccinationFlags = {'NOT_ASSESSED'};

  static const vaccinationValues = {'UP_TO_DATE', 'DUE', 'NOT_RECEIVED'};
  static const stiStatuses = {
    'NO_KNOWN_HISTORY',
    'SCREENING_INFORMATION',
    'PAST_HISTORY',
    'CURRENT_OR_UNDER_TREATMENT',
    'AT_RISK',
    'SUSPECTED_OR_KNOWN',
  };
  static const stiInfections = {
    'HIV',
    'SYPHILIS',
    'HEPATITIS_B',
    'HEPATITIS_C',
    'CHLAMYDIA',
    'GONORRHEA',
    'HERPES',
    'HPV',
    'OTHER',
  };

  static String labelFor(String value) => labels[value] ?? 'Lựa chọn';

  static bool isLegacyState(dynamic value) =>
      value == 'PREFER_NOT_TO_SAY' || value == 'NOT_APPLICABLE';

  static String normalizeState(dynamic value) =>
      value == 'KNOWN' ? 'KNOWN' : 'UNKNOWN';

  /// Normalizes old four-state responses at the questionnaire boundary and
  /// clears fields that cannot accompany an UNKNOWN answer.  A fresh repaired
  /// profile always has all ten canonical domain nodes and five vaccine rows.
  static Map<String, dynamic> normalizeProfile(Map<String, dynamic>? source) {
    final result = RecommendationProfileDraft.copyProfile(source);
    const domains = {
      'age',
      'bmi',
      'reproductiveHistory',
      'underlyingConditions',
      'lifestyle',
      'nutrition',
      'vaccination',
      'currentMedications',
      'sexualHealth',
      'sti',
    };
    result.removeWhere((key, _) => !domains.contains(key));
    _normalizeStateOnly(result, 'age');
    _normalizeBmi(result);
    _normalizeCodeDomain(result, 'reproductiveHistory');
    _normalizeCodeDomain(result, 'underlyingConditions');
    _normalizeLifestyle(result);
    _normalizeCodeDomain(result, 'nutrition');
    _normalizeVaccination(result);
    _normalizeCodeDomain(result, 'currentMedications');
    _normalizeCodeDomain(result, 'sexualHealth');
    _normalizeSti(result);
    return result;
  }

  static void _normalizeStateOnly(Map<String, dynamic> result, String key) {
    final value = _map(result[key]);
    value['state'] = normalizeState(value['state']);
    value.removeWhere((field, _) => field != 'state');
    result[key] = value;
  }

  static void _normalizeBmi(Map<String, dynamic> result) {
    final value = _map(result['bmi']);
    value['state'] = normalizeState(value['state']);
    if (value['state'] != 'KNOWN') {
      value.removeWhere((field, _) => field != 'state');
    } else {
      value.removeWhere(
        (field, _) =>
            field != 'state' &&
            field != 'heightCm' &&
            field != 'weightKg' &&
            field != 'weightContext' &&
            field != 'measuredOn',
      );
    }
    result['bmi'] = value;
  }

  static void _normalizeCodeDomain(Map<String, dynamic> result, String key) {
    final value = _map(result[key]);
    value['state'] = normalizeState(value['state']);
    if (value['state'] != 'KNOWN') {
      value.removeWhere((field, _) => field != 'state');
    } else {
      value.removeWhere((field, _) => field != 'state' && field != 'codes');
      final codes = value['codes'];
      if (codes is List) {
        final allowed = acceptedCodeOptions[key] ?? const <String>[];
        value['codes'] =
            codes.whereType<String>().where(allowed.contains).toSet().toList()
              ..sort();
      }
    }
    result[key] = value;
  }

  static void _normalizeLifestyle(Map<String, dynamic> result) {
    final lifestyle = _map(result['lifestyle']);
    const answerKeys = {
      'smoking',
      'alcohol',
      'physicalActivity',
      'sleep',
      'flags',
    };
    lifestyle.removeWhere((key, _) => !answerKeys.contains(key));
    for (final key in const [
      'smoking',
      'alcohol',
      'physicalActivity',
      'sleep',
    ]) {
      final value = _map(lifestyle[key]);
      value['state'] = normalizeState(value['state']);
      value.removeWhere((field, _) => field != 'state' && field != 'value');
      if (value['state'] != 'KNOWN') {
        value.remove('value');
      } else if (!lifestyleOptions[key]!.contains(value['value'])) {
        value.remove('value');
      }
      lifestyle[key] = value;
    }
    final rawFlags = lifestyle['flags'];
    if (rawFlags is List) {
      lifestyle['flags'] =
          rawFlags
              .whereType<String>()
              .where(lifestyleFlagValues.contains)
              .toSet()
              .toList()
            ..sort();
    } else {
      lifestyle.remove('flags');
    }
    result['lifestyle'] = lifestyle;
  }

  static void _normalizeVaccination(Map<String, dynamic> result) {
    final vaccination = _map(result['vaccination']);
    vaccination.removeWhere((key, _) => key != 'answers' && key != 'flags');
    final rawFlags = vaccination['flags'];
    if (rawFlags is List) {
      vaccination['flags'] =
          rawFlags
              .whereType<String>()
              .where(vaccinationFlags.contains)
              .toSet()
              .toList()
            ..sort();
    } else {
      vaccination.remove('flags');
    }
    final incoming = vaccination['answers'];
    final byCode = <String, Map<String, dynamic>>{};
    if (incoming is List) {
      for (final raw in incoming.whereType<Map>()) {
        final code = raw['code'];
        if (code is! String || !vaccineCodes.contains(code)) continue;
        final answer = <String, dynamic>{
          'code': code,
          'state': normalizeState(raw['state']),
        };
        if (answer['state'] == 'KNOWN' &&
            vaccinationValues.contains(raw['value'])) {
          answer['value'] = raw['value'];
        }
        byCode[code] = answer;
      }
    }
    vaccination['answers'] = [
      for (final code in vaccineCodes)
        byCode[code] ?? <String, dynamic>{'code': code, 'state': 'UNKNOWN'},
    ];
    final flags = vaccination['flags'];
    if (flags is List && flags.contains('NOT_ASSESSED')) {
      vaccination['answers'] = [
        for (final code in vaccineCodes)
          <String, dynamic>{'code': code, 'state': 'UNKNOWN'},
      ];
    }
    result['vaccination'] = vaccination;
  }

  static void _normalizeSti(Map<String, dynamic> result) {
    final value = _map(result['sti']);
    value['state'] = normalizeState(value['state']);
    if (value['state'] != 'KNOWN') {
      value.removeWhere((field, _) => field != 'state');
    } else {
      value.removeWhere(
        (field, _) =>
            field != 'state' && field != 'status' && field != 'infectionCodes',
      );
    }
    if (value['state'] == 'KNOWN' && !stiStatuses.contains(value['status'])) {
      value.remove('status');
      value.remove('infectionCodes');
    } else if (value['status'] != 'PAST_HISTORY' &&
        value['status'] != 'CURRENT_OR_UNDER_TREATMENT') {
      value.remove('infectionCodes');
    } else {
      final codes = value['infectionCodes'];
      if (codes is List) {
        value['infectionCodes'] =
            codes
                .whereType<String>()
                .where(stiInfections.contains)
                .toSet()
                .toList()
              ..sort();
      }
    }
    result['sti'] = value;
  }

  static Map<String, dynamic> _map(dynamic value) {
    if (value is Map) {
      return value.map((key, item) => MapEntry(key.toString(), item));
    }
    return <String, dynamic>{};
  }

  /// Returns a copy with one question explicitly skipped.  Dependent values
  /// are cleared so that a later full replacement cannot submit stale data.
  static Map<String, dynamic> skipQuestion(
    Map<String, dynamic> profile,
    RecommendationQuestion question,
  ) {
    final result = normalizeProfile(profile);
    if (question.kind == RecommendationQuestionKind.vaccination) {
      final vaccination = _map(result['vaccination']);
      final answers = vaccination['answers'];
      if (question.code == null && answers is List) {
        vaccination.remove('flags');
        vaccination['answers'] = answers.map((raw) {
          final answer = _map(raw);
          return <String, dynamic>{'code': answer['code'], 'state': 'UNKNOWN'};
        }).toList();
      } else if (answers is List) {
        vaccination['answers'] = answers.map((raw) {
          final answer = _map(raw);
          if (answer['code'] == question.code) {
            return <String, dynamic>{'code': question.code, 'state': 'UNKNOWN'};
          }
          return answer;
        }).toList();
      }
      result['vaccination'] = vaccination;
    } else if (question.kind == RecommendationQuestionKind.lifestyle) {
      final lifestyle = _map(result['lifestyle']);
      if (question.code == null) {
        for (final key in const [
          'smoking',
          'alcohol',
          'physicalActivity',
          'sleep',
        ]) {
          lifestyle[key] = <String, dynamic>{'state': 'UNKNOWN'};
        }
        lifestyle.remove('flags');
      } else {
        lifestyle[question.code!] = <String, dynamic>{'state': 'UNKNOWN'};
      }
      result['lifestyle'] = lifestyle;
    } else {
      final value = _map(result[question.domain]);
      value
        ..clear()
        ..['state'] = 'UNKNOWN';
      result[question.domain] = value;
      if (question.domain == 'sexualHealth') {
        result['sti'] = <String, dynamic>{'state': 'UNKNOWN'};
      }
    }
    return normalizeProfile(result);
  }

  static bool isComplete(
    Map<String, dynamic> profile,
    RecommendationQuestion question, {
    DateTime? today,
  }) {
    final value = question.kind == RecommendationQuestionKind.lifestyle
        ? question.code == null
              ? _map(profile['lifestyle'])
              : _map(_map(profile['lifestyle'])[question.code])
        : question.kind == RecommendationQuestionKind.vaccination
        ? question.code == null
              ? _map(profile['vaccination'])
              : _vaccinationAnswer(profile, question.code)
        : _map(profile[question.domain]);
    if (question.kind == RecommendationQuestionKind.lifestyle &&
        question.code == null) {
      return _isCompleteLifestyleGroup(value);
    }
    if (question.kind == RecommendationQuestionKind.vaccination &&
        question.code == null) {
      return _isCompleteVaccinationGroup(value);
    }
    if (normalizeState(value['state']) == 'UNKNOWN') return true;
    if (value['state'] != 'KNOWN') return false;
    switch (question.kind) {
      case RecommendationQuestionKind.age:
        return true;
      case RecommendationQuestionKind.bmi:
        return _isCompleteBmi(value, today: today);
      case RecommendationQuestionKind.codeSet:
        final codes = value['codes'];
        final allowed = acceptedCodeOptions[question.domain] ?? const <String>[];
        if (codes is! List || codes.isEmpty) return false;
        final strings = codes.whereType<String>().toList();
        if (strings.length != codes.length ||
            strings.toSet().length != strings.length ||
            !strings.every(allowed.contains)) {
          return false;
        }
        if ((question.domain == 'reproductiveHistory' &&
                (strings.contains('NO_PRIOR_PREGNANCY') ||
                    strings.contains('NO_LISTED_REPRODUCTIVE_HISTORY'))) ||
            (question.domain == 'underlyingConditions' &&
                strings.contains('NONE_KNOWN')) ||
            (question.domain == 'nutrition' &&
                strings.contains('NO_CURRENT_CONCERN')) ||
            (question.domain == 'currentMedications' &&
                strings.contains('NONE')) ||
            (question.domain == 'sexualHealth' &&
                strings.contains('NO_CURRENT_INFORMATION_NEED'))) {
          return strings.length == 1;
        }
        return !(question.domain == 'nutrition' &&
            strings.contains('VEGETARIAN') &&
            strings.contains('VEGAN'));
      case RecommendationQuestionKind.lifestyle:
        return value['value'] is String &&
            lifestyleOptions[question.code!]!.contains(value['value']);
      case RecommendationQuestionKind.vaccination:
        return vaccinationValues.contains(value['value']);
      case RecommendationQuestionKind.sti:
        final status = value['status'];
        if (status is! String || !stiStatuses.contains(status)) return false;
        if (status == 'PAST_HISTORY' ||
            status == 'CURRENT_OR_UNDER_TREATMENT') {
          final codes = value['infectionCodes'];
          if (codes is! List || codes.isEmpty || codes.length > 9) {
            return false;
          }
          final strings = codes.whereType<String>().toList();
          return strings.length == codes.length &&
              strings.toSet().length == strings.length &&
              strings.every(stiInfections.contains);
        }
        return !value.containsKey('infectionCodes');
    }
  }

  static bool _isCompleteLifestyleGroup(Map<String, dynamic> value) {
    final keys = const ['smoking', 'alcohol', 'physicalActivity', 'sleep'];
    final answers = <String, Map<String, dynamic>>{
      for (final key in keys) key: _map(value[key]),
    };
    if (answers.values.every(
      (answer) => normalizeState(answer['state']) == 'UNKNOWN',
    )) {
      return true;
    }
    if (!answers.values.every((answer) => answer['state'] == 'KNOWN')) {
      return false;
    }
    if (!lifestyleOptions['smoking']!.contains(answers['smoking']!['value']) ||
        !lifestyleOptions['alcohol']!.contains(answers['alcohol']!['value']) ||
        !lifestyleOptions['physicalActivity']!.contains(
          answers['physicalActivity']!['value'],
        ) ||
        !lifestyleOptions['sleep']!.contains(answers['sleep']!['value'])) {
      return false;
    }
    final flags = value['flags'];
    return flags == null ||
        (flags is List &&
            flags.whereType<String>().length == flags.length &&
            flags.toSet().length == flags.length &&
            flags.every(lifestyleFlagValues.contains));
  }

  static bool _isCompleteVaccinationGroup(Map<String, dynamic> value) {
    final flags = value['flags'];
    if (flags is List && flags.whereType<String>().contains('NOT_ASSESSED')) {
      return true;
    }
    final answers = value['answers'];
    if (answers is! List || answers.length != vaccineCodes.length) return false;
    final normalized = answers.whereType<Map>().map(_map).toList();
    if (normalized.every(
      (answer) => normalizeState(answer['state']) == 'UNKNOWN',
    )) {
      return true;
    }
    if (normalized.any((answer) => answer['state'] != 'KNOWN')) return false;
    final codes = normalized.map((answer) => answer['code']).toSet();
    return codes.length == vaccineCodes.length &&
        codes.containsAll(vaccineCodes) &&
        normalized.every(
          (answer) => vaccinationValues.contains(answer['value']),
        );
  }

  static bool _isCompleteBmi(Map<String, dynamic> value, {DateTime? today}) {
    final height = value['heightCm'];
    final weight = value['weightKg'];
    final context = value['weightContext'];
    final measuredOn = value['measuredOn'];
    return height is num &&
        weight is num &&
        height.isFinite &&
        weight.isFinite &&
        height >= 100 &&
        height <= 250 &&
        weight >= 20 &&
        weight <= 300 &&
        _oneDecimal(height) &&
        _oneDecimal(weight) &&
        context is String &&
        const {
          'PRE_PREGNANCY',
          'CURRENT_NON_PREGNANT',
          'CURRENT_PREGNANCY',
          'CURRENT_POSTPARTUM',
        }.contains(context) &&
        measuredOn is String &&
        _isCalendarDate(measuredOn, today: today);
  }

  static bool _oneDecimal(num value) {
    if (!value.isFinite) return false;
    final text = value.toString();
    if (text.contains('e') || text.contains('E')) return false;
    final point = text.indexOf('.');
    return point < 0 || text.length - point - 1 <= 1;
  }

  static bool _isCalendarDate(String value, {DateTime? today}) {
    if (!RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(value)) return false;
    final parts = value.split('-').map(int.parse).toList(growable: false);
    final parsed = DateTime.tryParse(value);
    if (parsed == null ||
        parsed.year != parts[0] ||
        parsed.month != parts[1] ||
        parsed.day != parts[2] ||
        parsed.year < 1900) {
      return false;
    }
    final currentDay = today ?? DateTime.now();
    return !parsed.isAfter(
      DateTime(currentDay.year, currentDay.month, currentDay.day),
    );
  }

  static Map<String, dynamic> _vaccinationAnswer(
    Map<String, dynamic> profile,
    String? code,
  ) {
    final answers = _map(profile['vaccination'])['answers'];
    if (answers is List) {
      for (final raw in answers.whereType<Map>()) {
        if (raw['code'] == code) return _map(raw);
      }
    }
    return <String, dynamic>{'state': 'UNKNOWN'};
  }
}

/// Compatibility alias for callers that prefer a top-level catalog constant.
const recommendationQuestionCatalog = RecommendationQuestionnaire.questions;

/// Compatibility alias for tests and copy review tooling.
const recommendationVietnameseLabels = RecommendationQuestionnaire.labels;
