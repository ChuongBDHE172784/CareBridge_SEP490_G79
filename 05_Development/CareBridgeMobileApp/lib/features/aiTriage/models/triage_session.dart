/// One answered question, as identifiers only.
///
/// The app states which question it was asked and which option was chosen. It never states what
/// that means clinically — the server's canonical mapper decides that.
class TriageAnswer {
  TriageAnswer({required this.questionId, this.optionCode, this.numericValue}) {
    if ((optionCode == null) == (numericValue == null)) {
      throw ArgumentError(
        'TriageAnswer requires exactly one optionCode or numericValue',
      );
    }
  }

  final String questionId;
  final String? optionCode;
  final num? numericValue;

  Map<String, dynamic> toJson() => {
    'questionId': questionId,
    if (optionCode != null) 'optionCode': optionCode,
    if (numericValue != null) 'numericValue': numericValue,
  };

  @override
  bool operator ==(Object other) =>
      other is TriageAnswer &&
      other.questionId == questionId &&
      other.optionCode == optionCode &&
      other.numericValue == numericValue;

  @override
  int get hashCode => Object.hash(questionId, optionCode, numericValue);
}

class VerifiedTriageCitation {
  const VerifiedTriageCitation({
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

  static VerifiedTriageCitation? verifiedFromJson(Map<String, dynamic> json) {
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
    final rawRuleIds = json['ruleIds'];
    if (rawRuleIds is! List) return null;
    return VerifiedTriageCitation(
      sourceId: sourceId,
      title: title,
      organization: organization,
      url: url,
      domain: domain,
      section: section,
      contentHash: hash,
      ruleIds: rawRuleIds
          .map((value) => value.toString())
          .toList(growable: false),
    );
  }
}

class TriageSession {
  const TriageSession({
    required this.sessionId,
    required this.stateVersion,
    required this.target,
    required this.intent,
    required this.stage,
    required this.outcome,
    required this.action,
    required this.stop,
    required this.questionIds,
    required this.questionDetails,
    required this.scope,
    required this.pendingRisks,
    required this.citations,
    this.rationale = '',
    this.evidenceStatus = 'UNAVAILABLE',
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
  static const evidenceStatuses = {
    'AVAILABLE',
    'PENDING',
    'UNAVAILABLE',
    'REJECTED',
  };
  static const emergencyActions = {
    'IMMEDIATE_EMERGENCY_ASSESSMENT',
    'IMMEDIATE_SAFETY_SUPPORT',
  };

  final String sessionId;
  final int stateVersion;
  final String target;
  final String intent;
  final String stage;
  final String outcome;
  final String action;
  final bool stop;
  final List<String> questionIds;
  final List<TriageQuestion> questionDetails;
  final String scope;
  final List<String> pendingRisks;
  final String? completionReason;
  final String? rulesetVersion;
  final String? rulesetHash;
  final List<VerifiedTriageCitation> citations;
  final String rationale;
  final String evidenceStatus;
  final String disclaimer;
  final Map<String, dynamic> readiness;

  bool get isUnavailable => readiness['technicalStatus'] != 'READY';

  factory TriageSession.fromJson(Map<String, dynamic> json) {
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
        disclaimer.isEmpty ||
        json['stop'] is! bool ||
        (outcome == 'RED' &&
            (json['stop'] != true || !emergencyActions.contains(action)))) {
      throw const FormatException('Invalid triage response');
    }
    // Public GREEN is deliberately unsupported. A server/fallback defect must not
    // become a reassuring mobile state.
    if (json['outcome'] == 'GREEN') {
      throw const FormatException('Public GREEN is disabled');
    }
    final citations = (json['citations'] as List<dynamic>? ?? const [])
        .whereType<Map>()
        .map(
          (item) => VerifiedTriageCitation.verifiedFromJson(
            Map<String, dynamic>.from(item),
          ),
        )
        .whereType<VerifiedTriageCitation>()
        .toList(growable: false);
    final suppliedEvidenceStatus = json['evidenceStatus']?.toString();
    final evidenceStatus = evidenceStatuses.contains(suppliedEvidenceStatus)
        ? suppliedEvidenceStatus!
        : citations.isNotEmpty
        ? 'AVAILABLE'
        : 'UNAVAILABLE';
    if (evidenceStatus == 'AVAILABLE' && citations.isEmpty) {
      throw const FormatException('AVAILABLE evidence requires a citation');
    }
    if (evidenceStatus != 'AVAILABLE' && citations.isNotEmpty) {
      throw const FormatException(
        'Citation evidence requires AVAILABLE status',
      );
    }
    final questionIds = (json['questions'] as List<dynamic>? ?? const [])
        .map((value) => value.toString())
        .toList(growable: false);
    final questionDetails =
        (json['questionDetails'] as List<dynamic>? ?? const [])
            .whereType<Map>()
            .map(
              (value) =>
                  TriageQuestion.fromJson(Map<String, dynamic>.from(value)),
            )
            .toList(growable: false);
    final detailIds = questionDetails.map((question) => question.id).toList();
    if (questionIds.toSet().length != questionIds.length ||
        questionDetails.length != questionIds.length ||
        detailIds.toSet().length != detailIds.length ||
        questionIds.toSet().difference(detailIds.toSet()).isNotEmpty ||
        detailIds.toSet().difference(questionIds.toSet()).isNotEmpty) {
      throw const FormatException('Missing canonical question details');
    }
    return TriageSession(
      sessionId: sessionId,
      stateVersion: stateVersion,
      target: target,
      intent: json['intent']?.toString() ?? 'UNKNOWN',
      stage: stage,
      outcome: outcome,
      action: action,
      stop: json['stop'] == true,
      questionIds: questionIds,
      questionDetails: questionDetails,
      scope: json['scope']?.toString() ?? 'UNKNOWN',
      pendingRisks: (json['pendingRisks'] as List<dynamic>? ?? const [])
          .map((value) => value.toString())
          .toList(growable: false),
      completionReason: json['completionReason']?.toString(),
      rulesetVersion: json['rulesetVersion']?.toString(),
      rulesetHash: json['rulesetHash']?.toString(),
      citations: citations,
      rationale: json['rationale']?.toString() ?? '',
      evidenceStatus: evidenceStatus,
      disclaimer: disclaimer,
      readiness: json['readiness'] is Map
          ? Map<String, dynamic>.from(json['readiness'] as Map)
          : const {'technicalStatus': 'FALLBACK_ONLY'},
    );
  }
}

class TriageQuestion {
  const TriageQuestion({
    required this.id,
    required this.text,
    required this.answerType,
    required this.options,
  });

  final String id;
  final String text;
  final String answerType;
  final List<TriageQuestionOption> options;

  List<String> get optionCodes =>
      options.map((option) => option.optionCode).toList(growable: false);

  factory TriageQuestion.fromJson(Map<String, dynamic> json) {
    final id = json['questionId']?.toString() ?? '';
    final text = json['text']?.toString() ?? '';
    final answerType = json['answerType']?.toString() ?? '';
    final options = (json['options'] as List<dynamic>? ?? const [])
        .whereType<Map>()
        .map(
          (value) =>
              TriageQuestionOption.fromJson(Map<String, dynamic>.from(value)),
        )
        .toList(growable: false);
    if (id.isEmpty ||
        text.isEmpty ||
        answerType.isEmpty ||
        options.any(
          (option) => option.optionCode.isEmpty || option.displayText.isEmpty,
        )) {
      throw const FormatException('Invalid canonical question detail');
    }
    return TriageQuestion(
      id: id,
      text: text,
      answerType: answerType,
      options: options,
    );
  }
}

class TriageQuestionOption {
  const TriageQuestionOption({
    required this.optionCode,
    required this.displayText,
  });

  final String optionCode;
  final String displayText;

  factory TriageQuestionOption.fromJson(Map<String, dynamic> json) =>
      TriageQuestionOption(
        optionCode: json['optionCode']?.toString() ?? '',
        displayText: json['displayText']?.toString() ?? '',
      );
}
