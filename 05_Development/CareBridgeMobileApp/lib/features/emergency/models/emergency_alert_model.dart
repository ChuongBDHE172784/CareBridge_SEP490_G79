class EmergencyAlert {
  final String id;
  final String alertType;
  final String personName;
  final String? personAvatarUrl;
  final String? address;
  final double? latitude;
  final double? longitude;
  final int? heartRate;
  final int? deviceBattery;
  final String? phoneNumber;
  final DateTime createdAt;
  final bool acknowledged;

  EmergencyAlert({
    required this.id,
    required this.alertType,
    required this.personName,
    this.personAvatarUrl,
    this.address,
    this.latitude,
    this.longitude,
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
      personAvatarUrl: json['personAvatarUrl'] as String?,
      address: json['address'] as String?,
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      heartRate: json['heartRate'] as int?,
      deviceBattery: json['deviceBattery'] as int?,
      phoneNumber: json['phoneNumber'] as String?,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : DateTime.now(),
      acknowledged: json['acknowledged'] as bool? ?? false,
    );
  }

  // MOCK — replace when backend endpoint is available
  static EmergencyAlert mock() {
    return EmergencyAlert(
      id: 'mock-001',
      alertType: 'FALL_DETECTED',
      personName: 'Mẹ (Nguyễn Thị Mai)',
      address: '123 Đường ABC, Phường Bến Nghé, Quận 1, TP. HCM',
      latitude: 10.7769,
      longitude: 106.7009,
      heartRate: 92,
      deviceBattery: 84,
      phoneNumber: '0901234567',
      createdAt: DateTime.now().subtract(const Duration(minutes: 2)),
    );
  }
}
