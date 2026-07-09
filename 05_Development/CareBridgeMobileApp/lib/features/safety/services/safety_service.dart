import '../../../core/network/api_client.dart';
import '../models/safety_config_model.dart';

class SafetyService {
  // UC-133
  Future<SafetyConfig> getConfig() async {
    final data = await apiGet('/api/v1/safety/config');
    return SafetyConfig.fromJson(data['data'] as Map<String, dynamic>);
  }

  Future<SafetyConfig> updateConfig({
    required bool fallDetectionEnabled,
    required String sensitivityLevel,
    required bool emergencyAutoAlert,
  }) async {
    final data = await apiPut('/api/v1/safety/config', {
      'fallDetectionEnabled': fallDetectionEnabled,
      'sensitivityLevel': sensitivityLevel,
      'emergencyAutoAlert': emergencyAutoAlert,
    });
    return SafetyConfig.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-134: idempotent — returns existing ACTIVE session if already enabled.
  Future<ImuMonitoringSession> enableFallDetection() async {
    final data = await apiPost(
      '/api/v1/safety/fall-detection/enable',
      const {},
    );
    return ImuMonitoringSession.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-135
  Future<void> disableFallDetection() async {
    await apiPost('/api/v1/safety/fall-detection/disable', const {});
  }

  // UC-136/UC-137: send real phone IMU samples to backend fall analysis.
  Future<void> sendImuData({
    required double accelerometerX,
    required double accelerometerY,
    required double accelerometerZ,
    required double gyroscopeX,
    required double gyroscopeY,
    required double gyroscopeZ,
    required DateTime timestamp,
  }) async {
    await apiPost('/api/v1/safety/imu-data', {
      'accelerometerX': accelerometerX,
      'accelerometerY': accelerometerY,
      'accelerometerZ': accelerometerZ,
      'gyroscopeX': gyroscopeX,
      'gyroscopeY': gyroscopeY,
      'gyroscopeZ': gyroscopeZ,
      'timestamp': timestamp.toUtc().toIso8601String(),
    });
  }

  Future<List<SafetyEvent>> getSafetyEvents({
    int page = 0,
    int size = 20,
  }) async {
    final data = await apiGet('/api/v1/safety/events?page=$page&size=$size');
    final items = data['data'] as List<dynamic>? ?? const [];
    return items
        .map((item) => SafetyEvent.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<SafetyEvent> confirmSafetyCheck(String eventId, {String? note}) async {
    final data = await apiPost('/api/v1/safety/events/$eventId/confirm', {
      'note': note,
    });
    return SafetyEvent.fromJson(data['data'] as Map<String, dynamic>);
  }

  Future<SafetyEvent> reportFalsePositive(
    String eventId, {
    String? note,
  }) async {
    final data = await apiPost(
      '/api/v1/safety/events/$eventId/false-positive',
      {'note': note},
    );
    return SafetyEvent.fromJson(data['data'] as Map<String, dynamic>);
  }

  Future<void> sendEmergencyAlertForEvent(String eventId) async {
    await apiPost('/api/v1/safety/events/$eventId/emergency-alert', const {});
  }
}
