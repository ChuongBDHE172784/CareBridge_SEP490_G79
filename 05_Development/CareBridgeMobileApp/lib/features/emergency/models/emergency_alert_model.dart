class EmergencyAlert {
  final String id;
  final String alertType;
  final String personName;
  final int? heartRate;
  final int? deviceBattery;
  final String? phoneNumber;
  final DateTime createdAt;
  final bool acknowledged;

  EmergencyAlert({
    required this.id,
    required this.alertType,
    required this.personName,
    this.heartRate,
    this.deviceBattery,
    this.phoneNumber,
    required this.createdAt,
    this.acknowledged = false,
  });

  factory EmergencyAlert.fromJson(Map<String, dynamic> json) {
    return EmergencyAlert(
      id: json['id'] as String,
      alertType: json['alertType'] as String? ?? 'FALL_DETECTED',
      personName: json['personName'] as String? ?? '',
      heartRate: json['heartRate'] as int?,
      deviceBattery: json['deviceBattery'] as int?,
      phoneNumber: json['phoneNumber'] as String?,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : DateTime.now(),
      acknowledged: json['acknowledged'] as bool? ?? false,
    );
  }

  // TV5 renders emergency alert status and contact support only.
  // Location, map, route, ETA, and nearby facility fields belong to TV4.
  factory EmergencyAlert.fromDetailJson(Map<String, dynamic> json) {
    final triggerSource = json['triggerSource'] as String?;
    return EmergencyAlert(
      id: json['sessionId'] as String,
      alertType: triggerSource == 'FALL_DETECTION' ? 'FALL_DETECTED' : 'SOS',
      personName: json['motherName'] as String? ?? 'Người thân',
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : DateTime.now(),
    );
  }
}
