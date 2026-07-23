import '../../../core/network/api_client.dart';
import '../models/care_facility_model.dart';

class CareFacilityService {
  Future<List<CareFacility>> searchNearby({
    required double latitude,
    required double longitude,
    int radiusMeters = 5000,
  }) async {
    final response = await apiGet(
      '/api/v1/map/nearby-facilities',
      queryParams: {
        'lat': latitude,
        'lng': longitude,
        'radiusMeters': radiusMeters,
      },
    );
    final data = response['data'] as Map<String, dynamic>? ?? const {};
    final rows = data['facilities'] as List? ?? const [];
    final facilities = <CareFacility>[];
    for (final row in rows.whereType<Map>()) {
      try {
        facilities.add(CareFacility.fromJson(Map<String, dynamic>.from(row)));
      } on FormatException {
        // One malformed provider row must not discard other usable facilities.
      } on TypeError {
        // Provider payloads are untrusted at this client boundary.
      }
    }
    return facilities;
  }

  Future<CareFacility> getFacility(String facilityId) async {
    final response = await apiGet('/api/v1/map/facilities/$facilityId');
    return CareFacility.fromJson(response['data'] as Map<String, dynamic>);
  }

  Future<CareRoute> getRoute({
    required double fromLatitude,
    required double fromLongitude,
    required double toLatitude,
    required double toLongitude,
  }) async {
    final response = await apiPost('/api/v1/map/route', {
      'fromLat': fromLatitude,
      'fromLng': fromLongitude,
      'toLat': toLatitude,
      'toLng': toLongitude,
      'transportMode': 'DRIVING',
    });
    return CareRoute.fromJson(response['data'] as Map<String, dynamic>);
  }
}
