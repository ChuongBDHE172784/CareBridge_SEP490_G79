/// Response from POST/GET /api/v1/emergency/sessions* (UC-62).
class EmergencySession {
  final String sessionId;
  final String userId;
  final String status; // ACTIVE | RESOLVED | CANCELLED
  final String triggerSource;
  final DateTime? createdAt;
  final DateTime? resolvedAt;

  const EmergencySession({
    required this.sessionId,
    required this.userId,
    required this.status,
    required this.triggerSource,
    this.createdAt,
    this.resolvedAt,
  });

  factory EmergencySession.fromJson(Map<String, dynamic> json) {
    return EmergencySession(
      sessionId: json['sessionId'] as String,
      userId: json['userId'] as String,
      status: json['status'] as String,
      triggerSource: json['triggerSource'] as String,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'] as String)
          : null,
      resolvedAt: json['resolvedAt'] != null
          ? DateTime.tryParse(json['resolvedAt'] as String)
          : null,
    );
  }
}
