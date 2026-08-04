import '../../../core/network/api_client.dart';
import 'package:flutter/foundation.dart';
import '../models/safety_config_model.dart';
import 'safety_demo_mode.dart';

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
    int? countdownSeconds,
    bool? sensorPermissionGranted,
  }) async {
    final request = buildConfigRequest(
      fallDetectionEnabled: fallDetectionEnabled,
      sensitivityLevel: sensitivityLevel,
      emergencyAutoAlert: emergencyAutoAlert,
      countdownSeconds: countdownSeconds,
      sensorPermissionGranted: sensorPermissionGranted,
    );
    final data = await apiPut('/api/v1/safety/config', request);
    return SafetyConfig.fromJson(data['data'] as Map<String, dynamic>);
  }

  @visibleForTesting
  static Map<String, dynamic> buildConfigRequest({
    required bool fallDetectionEnabled,
    required String sensitivityLevel,
    required bool emergencyAutoAlert,
    int? countdownSeconds,
    bool? sensorPermissionGranted,
  }) => <String, dynamic>{
    'fallDetectionEnabled': fallDetectionEnabled,
    'sensitivityLevel': sensitivityLevel,
    'emergencyAutoAlert': emergencyAutoAlert,
    'countdownSeconds': ?countdownSeconds,
    'sensorPermissionGranted': ?sensorPermissionGranted,
  };

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
  Future<SafetyEvent?> sendImuData({
    required double accelerometerX,
    required double accelerometerY,
    required double accelerometerZ,
    required double gyroscopeX,
    required double gyroscopeY,
    required double gyroscopeZ,
    required DateTime timestamp,
    String? signalId,
    double? latitude,
    double? longitude,
  }) async {
    final data = await apiPost('/api/v1/safety/imu-data', {
      'accelerometerX': accelerometerX,
      'accelerometerY': accelerometerY,
      'accelerometerZ': accelerometerZ,
      'gyroscopeX': gyroscopeX,
      'gyroscopeY': gyroscopeY,
      'gyroscopeZ': gyroscopeZ,
      'timestamp': timestamp.toUtc().toIso8601String(),
      'signalId': ?signalId,
      'latitude': ?latitude,
      'longitude': ?longitude,
    });
    final payload = data['data'];
    return payload is Map<String, dynamic>
        ? SafetyEvent.fromJson(payload)
        : null;
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

  Future<SafetyEvent> createSensorSelfTestEvent(
    SensorSelfTestResult result,
  ) async {
    final data = await apiPost(
      '/api/v1/safety/events/sensor-self-test',
      buildSensorSelfTestRequest(result),
    );
    return SafetyEvent.fromJson(data['data'] as Map<String, dynamic>);
  }

  @visibleForTesting
  static Map<String, dynamic> buildSensorSelfTestRequest(
    SensorSelfTestResult result,
  ) => <String, dynamic>{
    'testId':
        'self-test-${result.detectedAt.microsecondsSinceEpoch}-${result.sequence}',
    'detectedAt': result.detectedAt.toUtc().toIso8601String(),
    'accelerationMagnitude': result.accelerationMagnitude,
    'gyroscopeMagnitude': result.gyroscopeMagnitude,
  };

  Future<SafetyEvent> completeSensorSelfTest(
    String eventId, {
    required String outcome,
  }) async {
    final data = await apiPost(
      '/api/v1/safety/events/$eventId/sensor-self-test/complete',
      {'outcome': outcome},
    );
    return SafetyEvent.fromJson(data['data'] as Map<String, dynamic>);
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
