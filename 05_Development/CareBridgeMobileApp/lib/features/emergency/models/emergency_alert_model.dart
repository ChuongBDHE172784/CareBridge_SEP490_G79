class EmergencyAlert {
  final String id;
  final String alertType;
  final String personName;
  final String? personAvatarUrl;
  final String? address;
  final double? latitude;
  final double? longitude;
  final int? deviceBattery;
  final String? phoneNumber;
  final DateTime createdAt;
  final bool acknowledged;
  final DateTime? acknowledgedAt;

  EmergencyAlert({
    required this.id,
    required this.alertType,
    required this.personName,
    this.personAvatarUrl,
    this.address,
    this.latitude,
    this.longitude,
    this.deviceBattery,
    this.phoneNumber,
    required this.createdAt,
    this.acknowledged = false,
    this.acknowledgedAt,
  });

  factory EmergencyAlert.fromJson(Map<String, dynamic> json) {
    return EmergencyAlert(
      id: json['id'] as String,
      alertType: json['alertType'] as String? ?? 'FALL_DETECTED',
      personName: json['personName'] as String? ?? '',
      personAvatarUrl: json['personAvatarUrl'] as String?,
      address: json['address'] as String?,
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      deviceBattery: json['deviceBattery'] as int?,
      phoneNumber: json['phoneNumber'] as String?,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : DateTime.now(),
      acknowledged: json['acknowledged'] as bool? ?? false,
      acknowledgedAt: json['acknowledgedAt'] != null
          ? DateTime.tryParse(json['acknowledgedAt'].toString())
          : null,
    );
  }

  // UC-65/UC-141: real alert detail from GET /api/v1/emergency/sessions/{id}/alert.
  // No wearable/device data source exists yet, so deviceBattery/phoneNumber/
  // address stay null — the screen already renders those as "--"
  // / "Không xác định" when absent.
  factory EmergencyAlert.fromDetailJson(Map<String, dynamic> json) {
    final triggerSource = json['triggerSource'] as String?;
    return EmergencyAlert(
      id: json['sessionId'] as String,
      alertType: triggerSource == 'FALL_DETECTION' ? 'FALL_DETECTED' : 'SOS',
      personName: json['motherName'] as String? ?? 'Người thân',
      phoneNumber: json['motherPhone'] as String?,
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      acknowledged: json['acknowledged'] as bool? ?? false,
      acknowledgedAt: json['acknowledgedAt'] != null
          ? DateTime.tryParse(json['acknowledgedAt'].toString())
          : null,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : DateTime.now(),
    );
  }
}
