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
      startedAt: json['startedAt'] != null
          ? DateTime.tryParse(json['startedAt'] as String)
          : null,
    );
  }
}

class SafetyEvent {
  final String id;
  final String eventType;
  final double magnitude;
  final String status;
  final DateTime? detectedAt;
  final DateTime? resolvedAt;
  final String? notes;

  const SafetyEvent({
    required this.id,
    required this.eventType,
    required this.magnitude,
    required this.status,
    this.detectedAt,
    this.resolvedAt,
    this.notes,
  });

  factory SafetyEvent.fromJson(Map<String, dynamic> json) {
    return SafetyEvent(
      id: json['id'] as String,
      eventType: json['eventType'] as String? ?? 'SUSPECTED_FALL',
      magnitude: (json['magnitude'] as num?)?.toDouble() ?? 0,
      status: json['status'] as String? ?? 'OPEN',
      detectedAt: json['detectedAt'] != null
          ? DateTime.tryParse(json['detectedAt'] as String)
          : null,
      resolvedAt: json['resolvedAt'] != null
          ? DateTime.tryParse(json['resolvedAt'] as String)
          : null,
      notes: json['notes'] as String?,
    );
  }
}
