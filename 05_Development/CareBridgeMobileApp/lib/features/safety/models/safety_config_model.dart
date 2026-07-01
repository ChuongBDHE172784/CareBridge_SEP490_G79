/// Response from GET/PUT /api/v1/safety/config (UC-133)
class SafetyConfig {
  final String? id;
  final bool fallDetectionEnabled;
  final String sensitivityLevel; // LOW | MEDIUM | HIGH
  final bool emergencyAutoAlert;

  const SafetyConfig({
    this.id,
    required this.fallDetectionEnabled,
    required this.sensitivityLevel,
    required this.emergencyAutoAlert,
  });

  factory SafetyConfig.fromJson(Map<String, dynamic> json) {
    return SafetyConfig(
      id: json['id'] as String?,
      fallDetectionEnabled: json['fallDetectionEnabled'] as bool? ?? false,
      sensitivityLevel: json['sensitivityLevel'] as String? ?? 'MEDIUM',
      emergencyAutoAlert: json['emergencyAutoAlert'] as bool? ?? true,
    );
  }
}

/// Response from POST /api/v1/safety/fall-detection/enable (UC-134)
class ImuMonitoringSession {
  final String sessionId;
  final String status; // ACTIVE | STOPPED
  final String sensitivityLevel;
  final DateTime? startedAt;

  const ImuMonitoringSession({
    required this.sessionId,
    required this.status,
    required this.sensitivityLevel,
    this.startedAt,
  });

  factory ImuMonitoringSession.fromJson(Map<String, dynamic> json) {
    return ImuMonitoringSession(
      sessionId: json['sessionId'] as String,
      status: json['status'] as String,
      sensitivityLevel: json['sensitivityLevel'] as String,
      startedAt: json['startedAt'] != null ? DateTime.tryParse(json['startedAt'] as String) : null,
    );
  }
}
