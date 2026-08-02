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
      title: 'Bạn có muốn chia sẻ chỉ số và cân nặng gần đây không?',
      kind: RecommendationQuestionKind.bmi,
    ),
    RecommendationQuestion(
      id: 'reproductiveHistory',
      domain: 'reproductiveHistory',
      title: 'Bạn muốn chia sẻ thông tin tiền sử sinh sản nào?',
      kind: RecommendationQuestionKind.codeSet,
    ),
    RecommendationQuestion(
      id: 'underlyingConditions',
      domain: 'underlyingConditions',
      title: 'Bạn có bệnh nền nào đã được nhân viên y tế xác nhận không?',
      kind: RecommendationQuestionKind.codeSet,
    ),
    RecommendationQuestion(
      id: 'smoking',
      domain: 'lifestyle',
      title: 'Thói quen hút thuốc của bạn hiện nay là gì?',
      kind: RecommendationQuestionKind.lifestyle,
      code: 'smoking',
    ),
    RecommendationQuestion(
      id: 'alcohol',
      domain: 'lifestyle',
      title: 'Bạn sử dụng đồ uống có cồn với tần suất nào?',
      kind: RecommendationQuestionKind.lifestyle,
      code: 'alcohol',
    ),
    RecommendationQuestion(
      id: 'physicalActivity',
      domain: 'lifestyle',
      title: 'Mức độ vận động của bạn trong tuần qua là gì?',
      kind: RecommendationQuestionKind.lifestyle,
      code: 'physicalActivity',
    ),
    RecommendationQuestion(
      id: 'sleep',
      domain: 'lifestyle',
      title: 'Bạn có điều gì lo lắng về giấc ngủ không?',
      kind: RecommendationQuestionKind.lifestyle,
      code: 'sleep',
    ),
    RecommendationQuestion(
      id: 'nutrition',
      domain: 'nutrition',
      title: 'Bạn muốn chia sẻ mối quan tâm nào về dinh dưỡng?',
      kind: RecommendationQuestionKind.codeSet,
    ),
    RecommendationQuestion(
      id: 'vaccination.INFLUENZA',
      domain: 'vaccination',
      title: 'Tình trạng tiêm vắc-xin cúm của bạn là gì?',
      kind: RecommendationQuestionKind.vaccination,
      code: 'INFLUENZA',
    ),
    RecommendationQuestion(
      id: 'vaccination.COVID_19',
      domain: 'vaccination',
      title: 'Tình trạng tiêm vắc-xin COVID-19 của bạn là gì?',
      kind: RecommendationQuestionKind.vaccination,
      code: 'COVID_19',
    ),
    RecommendationQuestion(
      id: 'vaccination.TDAP',
      domain: 'vaccination',
      title: 'Tình trạng tiêm vắc-xin Tdap của bạn là gì?',
      kind: RecommendationQuestionKind.vaccination,
      code: 'TDAP',
    ),
    RecommendationQuestion(
      id: 'vaccination.HEPATITIS_B',
      domain: 'vaccination',
      title: 'Tình trạng tiêm vắc-xin viêm gan B của bạn là gì?',
      kind: RecommendationQuestionKind.vaccination,
      code: 'HEPATITIS_B',
    ),
    RecommendationQuestion(
      id: 'vaccination.RUBELLA_IMMUNITY',
      domain: 'vaccination',
      title: 'Bạn đã có miễn dịch với rubella chưa?',
      kind: RecommendationQuestionKind.vaccination,
      code: 'RUBELLA_IMMUNITY',
    ),
    RecommendationQuestion(
      id: 'currentMedications',
      domain: 'currentMedications',
      title: 'Bạn đang sử dụng nhóm thuốc nào theo chỉ định không?',
      kind: RecommendationQuestionKind.codeSet,
    ),
    RecommendationQuestion(
      id: 'sexualHealth',
      domain: 'sexualHealth',
      title: 'Bạn đang cần thông tin nào về sức khỏe tình dục?',
      kind: RecommendationQuestionKind.codeSet,
    ),
    RecommendationQuestion(
      id: 'sti',
      domain: 'sti',
      title:
          'Bạn muốn chia sẻ thông tin nào về bệnh lây truyền qua đường tình dục?',
      kind: RecommendationQuestionKind.sti,
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
    'PRIOR_PREGNANCY_LOSS': 'Từng mất thai',
    'PRIOR_STILLBIRTH': 'Từng thai lưu',
    'PRIOR_PRETERM_BIRTH': 'Từng sinh non',
    'PRIOR_MULTIPLE_PREGNANCY': 'Từng mang đa thai',
    'OTHER_HISTORY': 'Thông tin khác',
    // Conditions
    'NONE_KNOWN': 'Không có bệnh nền đã biết',
    'HYPERTENSION': 'Tăng huyết áp',
    'DIABETES': 'Đái tháo đường',
    'THYROID_DISORDER': 'Rối loạn tuyến giáp',
    'CARDIOVASCULAR_DISEASE': 'Bệnh tim mạch',
    'ASTHMA': 'Hen suyễn',
    'EPILEPSY': 'Động kinh',
    'KIDNEY_DISEASE': 'Bệnh thận',
    'AUTOIMMUNE_DISEASE': 'Bệnh tự miễn',
    'MENTAL_HEALTH_CONDITION': 'Tình trạng sức khỏe tâm thần',
    'OTHER_CLINICIAN_CONFIRMED': 'Bệnh khác đã được xác nhận',
    // Lifestyle
    'NEVER': 'Chưa bao giờ',
    'FORMER': 'Đã từng, hiện đã bỏ',
    'CURRENT': 'Hiện đang hút thuốc',
    'NONE': 'Không sử dụng',
    'LESS_THAN_WEEKLY': 'Ít hơn mỗi tuần',
    'WEEKLY_OR_MORE': 'Mỗi tuần hoặc thường xuyên hơn',
    'LOW': 'Thấp',
    'MODERATE': 'Vừa phải',
    'HIGH': 'Cao',
    'NO_CONCERN': 'Không có lo lắng',
    'CONCERN': 'Có lo lắng',
    // Nutrition
    'NO_CURRENT_CONCERN': 'Không có mối quan tâm hiện tại',
    'VEGETARIAN': 'Ăn chay',
    'VEGAN': 'Thuần chay',
    'FOOD_INSECURITY': 'Khó bảo đảm đủ thực phẩm',
    'LOW_APPETITE': 'Chán ăn',
    'NAUSEA_OR_VOMITING': 'Buồn nôn hoặc nôn',
    'IRON_OR_FOLATE_CONCERN': 'Lo lắng về sắt hoặc folate',
    'OTHER_NUTRITION_CONCERN': 'Mối quan tâm dinh dưỡng khác',
    // Vaccination
    'INFLUENZA': 'Cúm',
    'COVID_19': 'COVID-19',
    'TDAP': 'Tdap (uốn ván, bạch hầu, ho gà)',
    'HEPATITIS_B': 'Viêm gan B',
    'RUBELLA_IMMUNITY': 'Miễn dịch rubella',
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
    // Sexual health
    'NO_CURRENT_INFORMATION_NEED': 'Hiện không cần thêm thông tin',
    'GENERAL_INFORMATION': 'Thông tin chung',
    'CONTRACEPTION_OR_FERTILITY': 'Tránh thai hoặc khả năng sinh sản',
    'INTIMACY_DURING_LIFECYCLE': 'Thân mật trong các giai đoạn',
    'OTHER_NON_URGENT_INFORMATION': 'Thông tin không khẩn cấp khác',
    // STI
    'NO_KNOWN_HISTORY': 'Không có tiền sử đã biết',
    'SCREENING_INFORMATION': 'Thông tin xét nghiệm sàng lọc',
    'PAST_HISTORY': 'Tiền sử trước đây',
    'CURRENT_OR_UNDER_TREATMENT': 'Hiện tại hoặc đang điều trị',
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

  static const codeOptions = <String, List<String>>{
    'reproductiveHistory': [
      'NO_PRIOR_PREGNANCY',
      'PRIOR_LIVE_BIRTH',
      'PRIOR_PREGNANCY_LOSS',
      'PRIOR_STILLBIRTH',
      'PRIOR_PRETERM_BIRTH',
      'PRIOR_MULTIPLE_PREGNANCY',
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
      'OTHER_PRESCRIBED',
    ],
    'sexualHealth': [
      'NO_CURRENT_INFORMATION_NEED',
      'GENERAL_INFORMATION',
      'CONTRACEPTION_OR_FERTILITY',
      'INTIMACY_DURING_LIFECYCLE',
      'OTHER_NON_URGENT_INFORMATION',
    ],
  };

  static const lifestyleOptions = <String, List<String>>{
    'smoking': ['NEVER', 'FORMER', 'CURRENT'],
    'alcohol': ['NONE', 'LESS_THAN_WEEKLY', 'WEEKLY_OR_MORE'],
    'physicalActivity': ['LOW', 'MODERATE', 'HIGH'],
    'sleep': ['NO_CONCERN', 'CONCERN'],
  };

  static const vaccinationValues = {'UP_TO_DATE', 'DUE', 'NOT_RECEIVED'};
  static const stiStatuses = {
    'NO_KNOWN_HISTORY',
    'SCREENING_INFORMATION',
    'PAST_HISTORY',
    'CURRENT_OR_UNDER_TREATMENT',
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
        final allowed = codeOptions[key] ?? const <String>[];
        value['codes'] =
            codes.whereType<String>().where(allowed.contains).toSet().toList()
              ..sort();
      }
    }
    result[key] = value;
  }

  static void _normalizeLifestyle(Map<String, dynamic> result) {
    final lifestyle = _map(result['lifestyle']);
    const answerKeys = {'smoking', 'alcohol', 'physicalActivity', 'sleep'};
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
    result['lifestyle'] = lifestyle;
  }

  static void _normalizeVaccination(Map<String, dynamic> result) {
    final vaccination = _map(result['vaccination']);
    vaccination.removeWhere((key, _) => key != 'answers');
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
      if (answers is List) {
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
      lifestyle[question.code!] = <String, dynamic>{'state': 'UNKNOWN'};
      result['lifestyle'] = lifestyle;
    } else {
      final value = _map(result[question.domain]);
      value
        ..clear()
        ..['state'] = 'UNKNOWN';
      result[question.domain] = value;
    }
    return normalizeProfile(result);
  }

  static bool isComplete(
    Map<String, dynamic> profile,
    RecommendationQuestion question, {
    DateTime? today,
  }) {
    final value = question.kind == RecommendationQuestionKind.lifestyle
        ? _map(_map(profile['lifestyle'])[question.code])
        : question.kind == RecommendationQuestionKind.vaccination
        ? _vaccinationAnswer(profile, question.code)
        : _map(profile[question.domain]);
    if (normalizeState(value['state']) == 'UNKNOWN') return true;
    if (value['state'] != 'KNOWN') return false;
    switch (question.kind) {
      case RecommendationQuestionKind.age:
        return true;
      case RecommendationQuestionKind.bmi:
        return _isCompleteBmi(value, today: today);
      case RecommendationQuestionKind.codeSet:
        final codes = value['codes'];
        final allowed = codeOptions[question.domain] ?? const <String>[];
        if (codes is! List || codes.isEmpty) return false;
        final strings = codes.whereType<String>().toList();
        if (strings.length != codes.length ||
            strings.toSet().length != strings.length ||
            !strings.every(allowed.contains)) {
          return false;
        }
        if ((question.domain == 'reproductiveHistory' &&
                strings.contains('NO_PRIOR_PREGNANCY')) ||
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
