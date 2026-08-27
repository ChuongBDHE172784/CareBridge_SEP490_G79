import '../../../core/network/api_client.dart';
import '../models/emergency_alert_model.dart';
import '../models/emergency_session_model.dart';
import '../models/location_share_result.dart';

class EmergencyService {
  // UC-62: Idempotent open — returns the existing ACTIVE session if one exists.
  // Also triggers UC-65 family alert server-side as a side effect (event-driven).
  Future<EmergencySession> openFlow({
    required String triggerSource,
    double? latitude,
    double? longitude,
  }) async {
    final data = await apiPost('/api/v1/emergency/sessions', {
      'triggerSource': triggerSource,
      'userLatitude': ?latitude,
      'userLongitude': ?longitude,
    });
    return EmergencySession.fromJson(data['data'] as Map<String, dynamic>);
  }

  Future<EmergencySession?> getActive() async {
    try {
      final data = await apiGet('/api/v1/emergency/sessions/active');
      return EmergencySession.fromJson(data['data'] as Map<String, dynamic>);
    } on ApiException catch (e) {
      if (e.statusCode == 404) return null;
      rethrow;
    }
  }

  Future<EmergencySession> resolve(String sessionId) async {
    final data = await apiPatch(
      '/api/v1/emergency/sessions/$sessionId/resolve',
      const {},
    );
    return EmergencySession.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-65/UC-141: family member (or the mother) fetches real alert detail
  // after tapping the FCM push notification.
  Future<EmergencyAlert> getAlertDetail(String sessionId) async {
    final data = await apiGet('/api/v1/emergency/sessions/$sessionId/alert');
    return EmergencyAlert.fromDetailJson(data['data'] as Map<String, dynamic>);
  }

  Future<EmergencyAlert> acknowledgeAlert(String sessionId) async {
    final data = await apiPut(
      '/api/v1/emergency/sessions/$sessionId/alert/acknowledge',
      const {},
    );
    return EmergencyAlert.fromDetailJson(data['data'] as Map<String, dynamic>);
  }

  Future<LocationShareResult> shareCurrentLocation({
    required double latitude,
    required double longitude,
  }) async {
    final response = await apiPost('/api/v1/emergency/location-shares', {
      'latitude': latitude,
      'longitude': longitude,
    });
    return LocationShareResult.fromJson(
      response['data'] as Map<String, dynamic>,
    );
  }
}
