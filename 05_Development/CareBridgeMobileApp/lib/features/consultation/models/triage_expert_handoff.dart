const _yellowExpertContextPolicy = 'YELLOW_EXPERT_CONTEXT_V1';
const _expectedSharedFields = <String>[
  'YELLOW risk',
  'Lifecycle stage',
  'Risk summary',
  'Approved source metadata',
];
const _expectedExcludedFields = <String>[
  'Raw answers or symptoms',
  'Normalized symptoms',
  'Red flags',
  'Claims',
  'Health notes',
  'AI payload',
  'Identifiers or tokens',
  'Route or origin data',
  'Pending or unreviewed sources',
  'Surplus health data',
];

class TriageExpertHandoffCitation {
  final String evidenceSourceId;
  final String organization;
  final String baseUrl;
  final DateTime reviewedAt;

  const TriageExpertHandoffCitation({
    required this.evidenceSourceId,
    required this.organization,
    required this.baseUrl,
    required this.reviewedAt,
  });

  factory TriageExpertHandoffCitation.fromJson(Map<String, dynamic> json) {
    final reviewedAt = DateTime.tryParse(json['reviewedAt']?.toString() ?? '');
    final evidenceSourceId = json['evidenceSourceId']?.toString() ?? '';
    final organization = json['organization']?.toString() ?? '';
    final baseUrl = json['baseUrl']?.toString() ?? '';
    if (evidenceSourceId.isEmpty ||
        organization.isEmpty ||
        !baseUrl.startsWith('https://') ||
        reviewedAt == null) {
      throw const FormatException('Invalid approved citation metadata');
    }
    return TriageExpertHandoffCitation(
      evidenceSourceId: evidenceSourceId,
      organization: organization,
      baseUrl: baseUrl,
      reviewedAt: reviewedAt,
    );
  }
}

class TriageExpertHandoffContext {
  final String riskLevel;
  final String stage;
  final String riskSummary;
  final List<TriageExpertHandoffCitation> citations;

  const TriageExpertHandoffContext({
    required this.riskLevel,
    required this.stage,
    required this.riskSummary,
    this.citations = const [],
  });

  factory TriageExpertHandoffContext.fromJson(Map<String, dynamic> json) {
    final riskLevel = json['riskLevel']?.toString() ?? '';
    final stage = json['stage']?.toString() ?? '';
    final riskSummary = json['riskSummary']?.toString() ?? '';
    if (riskLevel != 'YELLOW' || stage.isEmpty || riskSummary.trim().isEmpty) {
      throw const FormatException('Invalid YELLOW handoff context');
    }
    return TriageExpertHandoffContext(
      riskLevel: riskLevel,
      stage: stage,
      riskSummary: riskSummary,
      citations: (json['citations'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .map(TriageExpertHandoffCitation.fromJson)
          .toList(growable: false),
    );
  }
}

class TriageExpertHandoffPreview {
  final String intakeSessionId;
  final String consentPolicyVersion;
  final TriageExpertHandoffContext context;
  final List<String> sharedFields;
  final List<String> excludedFields;

  const TriageExpertHandoffPreview({
    required this.intakeSessionId,
    required this.consentPolicyVersion,
    required this.context,
    required this.sharedFields,
    required this.excludedFields,
  });

  factory TriageExpertHandoffPreview.fromJson(Map<String, dynamic> json) {
    final intakeSessionId = json['intakeSessionId']?.toString() ?? '';
    final policy = json['consentPolicyVersion']?.toString() ?? '';
    final sharedFields = _stringList(json['sharedFields']);
    final excludedFields = _stringList(json['excludedFields']);
    if (intakeSessionId.isEmpty ||
        policy != _yellowExpertContextPolicy ||
        !_sameStrings(sharedFields, _expectedSharedFields) ||
        !_sameStrings(excludedFields, _expectedExcludedFields)) {
      throw const FormatException('Invalid handoff preview identity');
    }
    return TriageExpertHandoffPreview(
      intakeSessionId: intakeSessionId,
      consentPolicyVersion: policy,
      context: TriageExpertHandoffContext.fromJson(json),
      sharedFields: sharedFields,
      excludedFields: excludedFields,
    );
  }
}

class TriageExpertHandoffCreateResult {
  final String consultationRequestId;
  final String requestStatus;
  final bool replayed;
  final DateTime sharedAt;
  final TriageExpertHandoffContext context;

  const TriageExpertHandoffCreateResult({
    required this.consultationRequestId,
    required this.requestStatus,
    required this.replayed,
    required this.sharedAt,
    required this.context,
  });

  factory TriageExpertHandoffCreateResult.fromJson(Map<String, dynamic> json) {
    final consultationRequestId =
        json['consultationRequestId']?.toString() ?? '';
    final requestStatus = json['requestStatus']?.toString() ?? '';
    final sharedAt = DateTime.tryParse(json['sharedAt']?.toString() ?? '');
    final context = json['context'];
    if (consultationRequestId.isEmpty ||
        requestStatus.isEmpty ||
        sharedAt == null ||
        context is! Map<String, dynamic>) {
      throw const FormatException('Invalid handoff create response');
    }
    return TriageExpertHandoffCreateResult(
      consultationRequestId: consultationRequestId,
      requestStatus: requestStatus,
      replayed: json['replayed'] == true,
      sharedAt: sharedAt,
      context: TriageExpertHandoffContext.fromJson(context),
    );
  }
}

class TriageExpertHandoffParticipantContext {
  final String consultationRequestId;
  final String requestStatus;
  final DateTime sharedAt;
  final TriageExpertHandoffContext context;

  const TriageExpertHandoffParticipantContext({
    required this.consultationRequestId,
    required this.requestStatus,
    required this.sharedAt,
    required this.context,
  });

  factory TriageExpertHandoffParticipantContext.fromJson(
    Map<String, dynamic> json,
  ) {
    final create = TriageExpertHandoffCreateResult.fromJson({
      ...json,
      'replayed': false,
    });
    return TriageExpertHandoffParticipantContext(
      consultationRequestId: create.consultationRequestId,
      requestStatus: create.requestStatus,
      sharedAt: create.sharedAt,
      context: create.context,
    );
  }
}

List<String> _stringList(dynamic value) => (value as List<dynamic>? ?? const [])
    .map((item) => item.toString().trim())
    .where((item) => item.isNotEmpty)
    .toList(growable: false);

bool _sameStrings(List<String> actual, List<String> expected) {
  if (actual.length != expected.length) return false;
  for (var index = 0; index < actual.length; index++) {
    if (actual[index] != expected[index]) return false;
  }
  return true;
}
