import '../../../core/network/api_client.dart';
import '../models/emergency_alert_model.dart';
import '../models/emergency_session_model.dart';

class EmergencyService {
  // UC-62: idempotent open. Server-side events trigger UC-65 family alert.
  Future<EmergencySession> openFlow({required String triggerSource}) async {
    final data = await apiPost('/api/v1/emergency/sessions', {
      'triggerSource': triggerSource,
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

  Future<EmergencyAlert> getAlertDetail(String sessionId) async {
    final data = await apiGet('/api/v1/emergency/sessions/$sessionId/alert');
    return EmergencyAlert.fromDetailJson(data['data'] as Map<String, dynamic>);
  }
}
