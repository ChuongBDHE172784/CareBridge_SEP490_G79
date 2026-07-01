/// Response from GET /api/v1/triage/intake/{sessionId} (UC-61)
class TriageResult {
  final String sessionId;
  final String? riskLevel; // GREEN | YELLOW | RED
  final String? disclaimer;
  final String status; // PROCESSING | COMPLETED | FAILED
  final DateTime? createdAt;
  final DateTime? completedAt;

  const TriageResult({
    required this.sessionId,
    required this.status,
    this.riskLevel,
    this.disclaimer,
    this.createdAt,
    this.completedAt,
  });

  factory TriageResult.fromJson(Map<String, dynamic> json) {
    return TriageResult(
      sessionId: json['sessionId'] as String,
      status: json['status'] as String,
      riskLevel: json['riskLevel'] as String?,
      disclaimer: json['disclaimer'] as String?,
      createdAt: json['createdAt'] != null ? DateTime.tryParse(json['createdAt'] as String) : null,
      completedAt: json['completedAt'] != null ? DateTime.tryParse(json['completedAt'] as String) : null,
    );
  }
}
