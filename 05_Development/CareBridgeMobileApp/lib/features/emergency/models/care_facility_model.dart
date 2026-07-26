class CareFacility {
  final String? facilityId;
  final String? partnerId;
  final String name;
  final String? facilityType;
  final String? facilityLevel;
  final String? ownershipType;
  final String? address;
  final double? latitude;
  final double? longitude;
  final String? phone;
  final String? openingHoursJson;
  final String sourceType;
  final String? externalSourceId;
  final String verificationStatus;
  final int? distanceMeters;

  const CareFacility({
    this.facilityId,
    this.partnerId,
    required this.name,
    this.facilityType,
    this.facilityLevel,
    this.ownershipType,
    this.address,
    this.latitude,
    this.longitude,
    this.phone,
    this.openingHoursJson,
    this.sourceType = 'MANUAL',
    this.externalSourceId,
    this.verificationStatus = 'UNVERIFIED',
    this.distanceMeters,
  });

  bool get hasCoordinates => latitude != null && longitude != null;
  bool get isVerified =>
      verificationStatus == 'VERIFIED' || verificationStatus == 'APPROVED';
  String get sourceLabel => switch (sourceType) {
    'TRACKASIA' => 'Dữ liệu từ TrackAsia',
    'MANUAL' || 'CAREBRIDGE' => 'Dữ liệu CareBridge',
    _ => 'Nguồn dữ liệu chưa xác định',
  };
  String get verificationLabel => isVerified ? 'Đã xác minh' : 'Chưa xác minh';

  factory CareFacility.fromJson(Map<String, dynamic> json) {
    final name = (json['name'] as String?)?.trim();
    if (name == null || name.isEmpty) {
      throw const FormatException('Facility name is required');
    }
    return CareFacility(
      // hospitalId is a temporary read alias for older responses. New clients
      // and servers always use facilityId.
      facilityId: (json['facilityId'] ?? json['hospitalId']) as String?,
      partnerId: json['partnerId'] as String?,
      name: name,
      facilityType: json['facilityType'] as String?,
      facilityLevel: json['facilityLevel'] as String?,
      ownershipType: json['ownershipType'] as String?,
      address: json['address'] as String?,
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      phone: json['phone'] as String?,
      openingHoursJson: json['openingHoursJson'] as String?,
      sourceType: json['sourceType'] as String? ?? 'UNKNOWN',
      externalSourceId: json['externalSourceId'] as String?,
      verificationStatus: json['verificationStatus'] as String? ?? 'UNVERIFIED',
      distanceMeters: (json['distanceMeters'] as num?)?.round(),
    );
  }
}

class CareRoute {
  final double distanceMeters;
  final int etaMinutes;
  final String transportMode;

  const CareRoute({
    required this.distanceMeters,
    required this.etaMinutes,
    required this.transportMode,
  });

  factory CareRoute.fromJson(Map<String, dynamic> json) => CareRoute(
    distanceMeters: (json['distanceMeters'] as num?)?.toDouble() ?? 0,
    etaMinutes: (json['etaMinutes'] as num?)?.round() ?? 0,
    transportMode: json['transportMode'] as String? ?? 'DRIVING',
  );
}
