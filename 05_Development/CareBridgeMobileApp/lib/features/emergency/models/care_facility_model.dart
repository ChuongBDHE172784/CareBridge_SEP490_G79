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
    'TRACKASIA' => 'Nguồn TrackAsia',
    'MANUAL' || 'CAREBRIDGE' => 'Dữ liệu CareBridge',
    _ => 'Nguồn dữ liệu chưa xác định',
  };
  String get verificationLabel => isVerified
      ? 'Đã được CareBridge xác minh'
      : 'Chưa được CareBridge xác minh';

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
  final int durationSeconds;
  final String? encodedPolyline;
  final List<CareRouteCoordinate> coordinates;
  final List<CareRouteStep> steps;
  final String transportMode;

  const CareRoute({
    required this.distanceMeters,
    required this.etaMinutes,
    this.durationSeconds = 0,
    this.encodedPolyline,
    this.coordinates = const [],
    this.steps = const [],
    required this.transportMode,
  });

  factory CareRoute.fromJson(Map<String, dynamic> json) {
    final encoded = json['encodedPolyline'] as String?;
    final rawSteps = json['steps'] as List? ?? const [];
    return CareRoute(
      distanceMeters: (json['distanceMeters'] as num?)?.toDouble() ?? 0,
      etaMinutes: (json['etaMinutes'] as num?)?.round() ?? 0,
      durationSeconds: (json['durationSeconds'] as num?)?.round() ?? 0,
      encodedPolyline: encoded,
      coordinates: encoded == null || encoded.isEmpty
          ? const []
          : decodePolyline(encoded, precision: 6),
      steps: rawSteps
          .whereType<Map>()
          .map((row) => CareRouteStep.fromJson(Map<String, dynamic>.from(row)))
          .toList(growable: false),
      transportMode: json['transportMode'] as String? ?? 'DRIVING',
    );
  }

  static List<CareRouteCoordinate> decodePolyline(
    String encoded, {
    int precision = 6,
  }) {
    final coordinates = <CareRouteCoordinate>[];
    var index = 0;
    var latitude = 0;
    var longitude = 0;
    final factor = _pow10(precision);
    while (index < encoded.length) {
      final latResult = _decodeValue(encoded, index);
      index = latResult.nextIndex;
      latitude += latResult.value;
      if (index >= encoded.length) {
        throw const FormatException('Incomplete encoded polyline coordinate');
      }
      final lngResult = _decodeValue(encoded, index);
      index = lngResult.nextIndex;
      longitude += lngResult.value;
      coordinates.add(
        CareRouteCoordinate(latitude / factor, longitude / factor),
      );
    }
    return coordinates;
  }

  static _DecodedPolylineValue _decodeValue(String encoded, int start) {
    var result = 0;
    var shift = 0;
    var index = start;
    var byte = 0;
    do {
      if (index >= encoded.length) {
        throw const FormatException('Invalid encoded polyline');
      }
      byte = encoded.codeUnitAt(index++) - 63;
      if (byte < 0) {
        throw const FormatException('Invalid encoded polyline byte');
      }
      result |= (byte & 0x1f) << shift;
      shift += 5;
    } while (byte >= 0x20);
    final value = (result & 1) != 0 ? ~(result >> 1) : result >> 1;
    return _DecodedPolylineValue(value, index);
  }

  static int _pow10(int precision) {
    var value = 1;
    for (var i = 0; i < precision; i++) {
      value *= 10;
    }
    return value;
  }
}

class CareRouteCoordinate {
  final double latitude;
  final double longitude;

  const CareRouteCoordinate(this.latitude, this.longitude);
}

class CareRouteStep {
  final double latitude;
  final double longitude;
  final String maneuver;
  final String? roadName;
  final int distanceMeters;
  final int durationSeconds;

  const CareRouteStep({
    required this.latitude,
    required this.longitude,
    required this.maneuver,
    this.roadName,
    required this.distanceMeters,
    required this.durationSeconds,
  });

  factory CareRouteStep.fromJson(Map<String, dynamic> json) => CareRouteStep(
    latitude: (json['lat'] as num?)?.toDouble() ?? 0,
    longitude: (json['lng'] as num?)?.toDouble() ?? 0,
    maneuver: json['maneuver'] as String? ?? 'continue',
    roadName: json['roadName'] as String?,
    distanceMeters: (json['distanceMeters'] as num?)?.round() ?? 0,
    durationSeconds: (json['durationSeconds'] as num?)?.round() ?? 0,
  );
}

class _DecodedPolylineValue {
  final int value;
  final int nextIndex;

  const _DecodedPolylineValue(this.value, this.nextIndex);
}
