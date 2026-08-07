/// One answered question, as identifiers only.
///
/// The app states which question it was asked and which option was chosen. It never states what
/// that means clinically — the server's canonical mapper decides that.
class TriageV2Answer {
  const TriageV2Answer({required this.questionId, required this.optionCode});

  final String questionId;
  final String optionCode;

  Map<String, dynamic> toJson() => {
    'questionId': questionId,
    'optionCode': optionCode,
  };

  @override
  bool operator ==(Object other) =>
      other is TriageV2Answer &&
      other.questionId == questionId &&
      other.optionCode == optionCode;

  @override
  int get hashCode => Object.hash(questionId, optionCode);
}

class TriageV2Citation {
  const TriageV2Citation({
    required this.sourceId,
    required this.title,
    required this.organization,
    required this.url,
    required this.domain,
    required this.section,
    required this.contentHash,
    required this.ruleIds,
  });

  final String sourceId;
  final String title;
  final String organization;
  final String url;
  final String domain;
  final String section;
  final String contentHash;
  final List<String> ruleIds;

  static TriageV2Citation? verifiedFromJson(Map<String, dynamic> json) {
    if (json['sourceStatus'] != 'SOURCE_VERIFIED' ||
        json['retrievalMode'] != 'LOCAL_BM25') {
      return null;
    }
    final sourceId = json['sourceId']?.toString() ?? '';
    final title = json['title']?.toString() ?? '';
    final organization = json['organization']?.toString() ?? '';
    final url = json['url']?.toString() ?? '';
    final domain = json['domain']?.toString().toLowerCase() ?? '';
    final section = json['section']?.toString() ?? '';
    final hash = json['contentHash']?.toString().toLowerCase() ?? '';
    final uri = Uri.tryParse(url);
    final host = (uri?.host.toLowerCase() ?? '').replaceFirst('www.', '');
    final validHost = host == domain || host.endsWith('.$domain');
    if (sourceId.isEmpty ||
        title.isEmpty ||
        organization.isEmpty ||
        section.isEmpty ||
        domain.isEmpty ||
        uri?.scheme != 'https' ||
        uri!.pathSegments.isEmpty ||
        !validHost ||
        !RegExp(r'^[a-f0-9]{64}$').hasMatch(hash)) {
      return null;
    }
    return TriageV2Citation(
      sourceId: sourceId,
      title: title,
      organization: organization,
      url: url,
      domain: domain,
      section: section,
      contentHash: hash,
      ruleIds: (json['ruleIds'] as List<dynamic>? ?? const [])
          .map((value) => value.toString())
          .toList(growable: false),
    );
  }
}

class TriageV2Session {
  const TriageV2Session({
    required this.sessionId,
    required this.stateVersion,
    required this.target,
    required this.intent,
    required this.stage,
    required this.outcome,
    required this.action,
    required this.stop,
    required this.questionIds,
    required this.scope,
    required this.pendingRisks,
    required this.citations,
    required this.disclaimer,
    required this.readiness,
    this.completionReason,
    this.rulesetVersion,
    this.rulesetHash,
  });

  static const targets = {'MOTHER', 'BABY', 'UNKNOWN', 'CONFLICTED'};
  static const stages = {
    'PRECONCEPTION',
    'POSSIBLE_PREGNANCY',
    'PREGNANCY',
    'POSTPARTUM_MOTHER',
    'INFANT_0_12M',
    'TODDLER_12_24M',
    'UNKNOWN',
    'CONFLICTED',
  };
  static const outcomes = {'RED', 'YELLOW', 'NEEDS_MORE_INFO', 'OUT_OF_SCOPE'};

  final String sessionId;
  final int stateVersion;
  final String target;
  final String intent;
  final String stage;
  final String outcome;
  final String action;
  final bool stop;
  final List<String> questionIds;
  final String scope;
  final List<String> pendingRisks;
  final String? completionReason;
  final String? rulesetVersion;
  final String? rulesetHash;
  final List<TriageV2Citation> citations;
  final String disclaimer;
  final Map<String, dynamic> readiness;

  bool get isUnavailable => readiness['technicalStatus'] != 'READY';

  factory TriageV2Session.fromJson(Map<String, dynamic> json) {
    final sessionId = json['sessionId']?.toString() ?? '';
    final stateVersion = json['stateVersion'];
    final target = json['target']?.toString() ?? '';
    final stage = json['stage']?.toString() ?? '';
    final outcome = json['outcome']?.toString() ?? '';
    final action = json['action']?.toString() ?? '';
    final disclaimer = json['disclaimer']?.toString() ?? '';
    if (sessionId.isEmpty ||
        stateVersion is! int ||
        stateVersion < 0 ||
        !targets.contains(target) ||
        !stages.contains(stage) ||
        !outcomes.contains(outcome) ||
        action.isEmpty ||
        disclaimer.isEmpty) {
      throw const FormatException('Invalid Triage V2 response');
    }
    // Public GREEN is deliberately unsupported. A server/fallback defect must not
    // become a reassuring mobile state.
    if (json['outcome'] == 'GREEN') {
      throw const FormatException('Public GREEN is disabled');
    }
    final citations = (json['citations'] as List<dynamic>? ?? const [])
        .whereType<Map>()
        .map(
          (item) => TriageV2Citation.verifiedFromJson(
            Map<String, dynamic>.from(item),
          ),
        )
        .whereType<TriageV2Citation>()
        .toList(growable: false);
    return TriageV2Session(
      sessionId: sessionId,
      stateVersion: stateVersion,
      target: target,
      intent: json['intent']?.toString() ?? 'UNKNOWN',
      stage: stage,
      outcome: outcome,
      action: action,
      stop: json['stop'] == true,
      questionIds: (json['questions'] as List<dynamic>? ?? const [])
          .map((value) => value.toString())
          .toList(growable: false),
      scope: json['scope']?.toString() ?? 'UNKNOWN',
      pendingRisks: (json['pendingRisks'] as List<dynamic>? ?? const [])
          .map((value) => value.toString())
          .toList(growable: false),
      completionReason: json['completionReason']?.toString(),
      rulesetVersion: json['rulesetVersion']?.toString(),
      rulesetHash: json['rulesetHash']?.toString(),
      citations: citations,
      disclaimer: disclaimer,
      readiness: json['readiness'] is Map
          ? Map<String, dynamic>.from(json['readiness'] as Map)
          : const {'technicalStatus': 'FALLBACK_ONLY'},
    );
  }
}

class TriageV2Question {
  const TriageV2Question(this.id, this.text, this.optionCodes);

  final String id;
  final String text;
  final List<String> optionCodes;

  static const _catalog = <String, TriageV2Question>{
    'Q_BLEEDING_AMOUNT': TriageV2Question(
      'Q_BLEEDING_AMOUNT',
      'Lượng máu ra hiện tại như thế nào?',
      ['BLEEDING_NONE', 'BLEEDING_SPOTTING', 'BLEEDING_MODERATE', 'BLEEDING_HEAVY', 'UNSURE'],
    ),
    'Q_CLOTS': TriageV2Question(
      'Q_CLOTS',
      'Bạn có thấy cục máu đông lớn không?',
      ['CLOTS_LARGE', 'CLOTS_NONE', 'UNSURE'],
    ),
    'Q_DIZZINESS': TriageV2Question(
      'Q_DIZZINESS',
      'Bạn có thấy choáng váng hoặc hoa mắt không?',
      ['DIZZINESS_YES', 'DIZZINESS_NO', 'UNSURE'],
    ),
    'Q_VISUAL_CHANGE': TriageV2Question(
      'Q_VISUAL_CHANGE',
      'Bạn có bị nhìn mờ, hoa mắt hoặc thay đổi thị lực không?',
      ['VISUAL_CHANGE_YES', 'VISUAL_CHANGE_NO', 'UNSURE'],
    ),
    'Q_BP_IF_KNOWN': TriageV2Question(
      'Q_BP_IF_KNOWN',
      'Nếu bạn có đo huyết áp gần đây, chỉ số là bao nhiêu?',
      ['BP_GTE_140_90', 'BP_LT_140_90', 'NO_DEVICE_OR_UNAWARE'],
    ),
    'Q_EPIGASTRIC_PAIN': TriageV2Question(
      'Q_EPIGASTRIC_PAIN',
      'Bạn có đau vùng trên rốn (vùng thượng vị) hoặc đau dưới bờ sườn phải không?',
      ['EPIGASTRIC_YES', 'EPIGASTRIC_NO', 'UNSURE'],
    ),
    'Q_SWELLING': TriageV2Question(
      'Q_SWELLING',
      'Bạn có bị sưng phù mặt, tay hoặc chân tăng lên gần đây không?',
      ['SWELLING_YES', 'SWELLING_NO', 'UNSURE'],
    ),
    'Q_PAIN_SEVERITY': TriageV2Question(
      'Q_PAIN_SEVERITY',
      'Mức độ đau của bạn hiện tại thế nào?',
      ['PAIN_NONE', 'PAIN_MILD', 'PAIN_MODERATE', 'PAIN_SEVERE', 'UNSURE'],
    ),
    'Q_PREGNANCY_TEST': TriageV2Question(
      'Q_PREGNANCY_TEST',
      'Bạn đã thử thai hoặc có kỳ kinh cuối vào khoảng thời gian nào?',
      ['TEST_POSITIVE', 'TEST_NEGATIVE', 'TEST_NOT_DONE', 'UNSURE'],
    ),
    'Q_GESTATIONAL_WEEK': TriageV2Question(
      'Q_GESTATIONAL_WEEK',
      'Bạn đang mang thai khoảng bao nhiêu tuần?',
      ['UNSURE'],
    ),
    'Q_POSTPARTUM_DAY': TriageV2Question(
      'Q_POSTPARTUM_DAY',
      'Bạn sinh em bé được bao nhiêu ngày rồi?',
      ['UNSURE'],
    ),
    'Q_CLARIFY_TARGET_ENTITY': TriageV2Question(
      'Q_CLARIFY_TARGET_ENTITY',
      'Bạn muốn định hướng nguy cơ cho ai?',
      ['CLARIFY_TARGET_MOTHER', 'CLARIFY_TARGET_BABY', 'CLARIFY_TARGET_BOTH'],
    ),
    'Q_CLARIFY_TARGET_FIRST': TriageV2Question(
      'Q_CLARIFY_TARGET_FIRST',
      'Mẹ và bé đều có dấu hiệu. Bạn muốn đánh giá ai trước?',
      ['CLARIFY_TARGET_MOTHER', 'CLARIFY_TARGET_BABY'],
    ),
    'Q_CLARIFY_INTENT': TriageV2Question(
      'Q_CLARIFY_INTENT',
      'Bạn đang muốn CareBridge hỗ trợ điều gì?',
      ['INTENT_SYMPTOM_TRIAGE', 'INTENT_GENERAL_INFO', 'INTENT_SOURCE_LOOKUP'],
    ),
    'Q_BABY_AGE_MONTHS': TriageV2Question(
      'Q_BABY_AGE_MONTHS',
      'Bé hiện được bao nhiêu tháng tuổi?',
      ['UNSURE'],
    ),
    // Deterministic danger and safety screens. Without these the emergency signals could only be
    // reached through free-text extraction, so an LLM outage left the engine unable to see them.
    'Q_GLOBAL_DANGER': TriageV2Question(
      'Q_GLOBAL_DANGER',
      'Hiện tại có dấu hiệu nào sau đây không?',
      [
        'DANGER_NONE',
        'DANGER_SEIZURE',
        'DANGER_UNCONSCIOUS',
        'DANGER_BREATHING',
        'DANGER_CYANOSIS',
        'UNSURE',
      ],
    ),
    'Q_SAFETY_SELF_HARM': TriageV2Question(
      'Q_SAFETY_SELF_HARM',
      'Gần đây bạn có ý nghĩ làm hại bản thân hoặc em bé, hoặc thấy không thể tự giữ an toàn không?',
      [
        'SELF_HARM_NONE',
        'SELF_HARM_THOUGHTS',
        'SELF_HARM_PLAN',
        'HARM_TO_BABY_THOUGHTS',
        'CANNOT_ENSURE_SAFETY',
        'UNSURE',
      ],
    ),
    'Q_HEADACHE_SEVERITY': TriageV2Question(
      'Q_HEADACHE_SEVERITY',
      'Bạn có đau đầu không, và mức độ thế nào?',
      ['HEADACHE_NONE', 'HEADACHE_MILD', 'HEADACHE_SEVERE', 'UNSURE'],
    ),
    'Q_BABY_FEEDING': TriageV2Question(
      'Q_BABY_FEEDING',
      'Bé bú hoặc uống như thế nào so với bình thường?',
      ['FEEDING_NORMAL', 'FEEDING_REDUCED', 'FEEDING_REFUSED', 'UNSURE'],
    ),
    'Q_BABY_HYDRATION': TriageV2Question(
      'Q_BABY_HYDRATION',
      'Bé có dấu hiệu mất nước không?',
      ['HYDRATION_NORMAL', 'HYDRATION_REDUCED', 'HYDRATION_SEVERE', 'UNSURE'],
    ),
    'Q_BABY_TEMPERATURE': TriageV2Question(
      'Q_BABY_TEMPERATURE',
      'Nếu bạn đã đo nhiệt độ cho bé, kết quả trong khoảng nào?',
      ['TEMP_LT_38', 'TEMP_38_TO_39', 'TEMP_GTE_39', 'NO_DEVICE_OR_UNAWARE'],
    ),
  };

  static TriageV2Question fromId(String id) =>
      _catalog[id] ??
      TriageV2Question(
        id,
        'Vui lòng bổ sung thông tin cho câu hỏi $id.',
        const [],
      );
}
