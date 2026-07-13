/// Response from GET /api/v1/triage/intake/{sessionId} (UC-61)
class TriageResult {
  final String sessionId;
  final String? triageStatus; // COMPLETED | NEED_MORE_INFO
  final String? riskLevel; // GREEN | YELLOW | RED
  final String? riskColor;
  final String? summary;
  final String? possibleConcern;
  final String? recommendedAction;
  final bool emergencyActionRequired;
  final List<String> redFlags;
  final List<String> matchedRules;
  final List<TriageCitation> citations;
  final TriageEvidence? evidence;
  final String? disclaimer;
  final List<String> questions;
  final String? warning;
  final String status; // PROCESSING | NEED_MORE_INFO | COMPLETED | FAILED
  final DateTime? createdAt;
  final DateTime? completedAt;

  const TriageResult({
    required this.sessionId,
    required this.status,
    this.triageStatus,
    this.riskLevel,
    this.riskColor,
    this.summary,
    this.possibleConcern,
    this.recommendedAction,
    this.emergencyActionRequired = false,
    this.redFlags = const [],
    this.matchedRules = const [],
    this.citations = const [],
    this.evidence,
    this.disclaimer,
    this.questions = const [],
    this.warning,
    this.createdAt,
    this.completedAt,
  });

  factory TriageResult.fromJson(Map<String, dynamic> json) {
    return TriageResult(
      sessionId: json['sessionId'] as String,
      status: json['status'] as String,
      triageStatus: json['triageStatus'] as String?,
      riskLevel: json['riskLevel'] as String?,
      riskColor: json['riskColor'] as String?,
      summary: json['summary'] as String?,
      possibleConcern: json['possibleConcern'] as String?,
      recommendedAction: json['recommendedAction'] as String?,
      emergencyActionRequired: json['emergencyActionRequired'] as bool? ?? false,
      redFlags: (json['redFlags'] as List<dynamic>? ?? const [])
          .map((item) => item.toString())
          .toList(),
      matchedRules: (json['matchedRules'] as List<dynamic>? ?? const [])
          .map((item) => item.toString())
          .toList(),
      citations: (json['citations'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .map(TriageCitation.fromJson)
          .toList(),
      evidence: json['evidence'] is Map<String, dynamic>
          ? TriageEvidence.fromJson(json['evidence'] as Map<String, dynamic>)
          : null,
      disclaimer: json['disclaimer'] as String?,
      questions: (json['questions'] as List<dynamic>? ?? const [])
          .map((item) => item.toString())
          .toList(),
      warning: json['warning'] as String?,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'] as String)
          : null,
      completedAt: json['completedAt'] != null
          ? DateTime.tryParse(json['completedAt'] as String)
          : null,
    );
  }
}

class TriageCitation {
  final String? id;
  final String title;
  final String source;
  final String? organization;
  final String url;
  final String? domain;
  final String excerpt;
  final String retrievedAt;
  final List<String> matchedSymptoms;
  final String sourceStatus;

  const TriageCitation({
    this.id,
    required this.title,
    required this.source,
    this.organization,
    required this.url,
    this.domain,
    required this.excerpt,
    required this.retrievedAt,
    this.matchedSymptoms = const [],
    this.sourceStatus = 'REVIEWED',
  });

  factory TriageCitation.fromJson(Map<String, dynamic> json) {
    return TriageCitation(
      id: json['id']?.toString(),
      title: json['title']?.toString() ?? '',
      source: json['source']?.toString() ?? '',
      organization: json['organization']?.toString(),
      url: json['url']?.toString() ?? '',
      domain: json['domain']?.toString(),
      excerpt: json['excerpt']?.toString() ?? '',
      retrievedAt: json['retrievedAt']?.toString() ?? '',
      matchedSymptoms: (json['matchedSymptoms'] as List<dynamic>? ?? const [])
          .map((item) => item.toString())
          .toList(),
      sourceStatus: json['sourceStatus']?.toString() ?? 'REVIEWED',
    );
  }
}

class TriageEvidence {
  final String basis;
  final String legalSafetyNote;
  final List<String> matchedSymptoms;
  final List<String> matchedOfficialSources;
  final List<String> unmatchedSymptoms;

  const TriageEvidence({
    required this.basis,
    required this.legalSafetyNote,
    this.matchedSymptoms = const [],
    this.matchedOfficialSources = const [],
    this.unmatchedSymptoms = const [],
  });

  factory TriageEvidence.fromJson(Map<String, dynamic> json) {
    return TriageEvidence(
      basis: json['basis']?.toString() ?? '',
      legalSafetyNote: json['legalSafetyNote']?.toString() ?? '',
      matchedSymptoms: (json['matchedSymptoms'] as List<dynamic>? ?? const [])
          .map((item) => item.toString())
          .toList(),
      matchedOfficialSources:
          (json['matchedOfficialSources'] as List<dynamic>? ?? const [])
              .map((item) => item.toString())
              .toList(),
      unmatchedSymptoms:
          (json['unmatchedSymptoms'] as List<dynamic>? ?? const [])
              .map((item) => item.toString())
              .toList(),
    );
  }
}
