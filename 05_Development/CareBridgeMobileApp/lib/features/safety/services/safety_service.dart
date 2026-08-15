import '../../../core/network/api_client.dart';
import 'package:flutter/foundation.dart';
import '../models/safety_config_model.dart';
import 'safety_demo_mode.dart';

/// Dịch vụ giao tiếp API Backend cho phân hệ Giám sát An toàn (Safety Monitoring - UC-133 đến UC-137).
class SafetyService {
  /// UC-133: Lấy cấu hình giám sát an toàn hiện tại của người dùng (trạng thái bật ngã, độ nhạy, chia sẻ vị trí).
  Future<SafetyConfig> getConfig() async {
    final data = await apiGet('/api/v1/safety/config');
    return SafetyConfig.fromJson(data['data'] as Map<String, dynamic>);
  }

  /// UC-133: Cập nhật cấu hình giám sát an toàn lên Backend.
  Future<SafetyConfig> updateConfig({
    required bool fallDetectionEnabled,
    required String sensitivityLevel,
    required bool emergencyAutoAlert,
    bool? locationSharingEnabled,
    int? countdownSeconds,
    bool? sensorPermissionGranted,
  }) async {
    final request = buildConfigRequest(
      fallDetectionEnabled: fallDetectionEnabled,
      sensitivityLevel: sensitivityLevel,
      emergencyAutoAlert: emergencyAutoAlert,
      locationSharingEnabled: locationSharingEnabled,
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
    bool? locationSharingEnabled,
    int? countdownSeconds,
    bool? sensorPermissionGranted,
  }) => <String, dynamic>{
    'fallDetectionEnabled': fallDetectionEnabled,
    'sensitivityLevel': sensitivityLevel,
    'emergencyAutoAlert': emergencyAutoAlert,
    'locationSharingEnabled': ?locationSharingEnabled,
    'countdownSeconds': ?countdownSeconds,
    'sensorPermissionGranted': ?sensorPermissionGranted,
  };

  /// UC-134: Bật phiên giám sát ngã IMU trên Backend (trả về phiên ACTIVE).
  Future<ImuMonitoringSession> enableFallDetection() async {
    final data = await apiPost(
      '/api/v1/safety/fall-detection/enable',
      const {},
    );
    return ImuMonitoringSession.fromJson(data['data'] as Map<String, dynamic>);
  }

  /// UC-135: Tắt phiên giám sát ngã IMU trên Backend.
  Future<void> disableFallDetection() async {
    await apiPost('/api/v1/safety/fall-detection/disable', const {});
  }

  /// UC-136/UC-137: Gửi mẫu dữ liệu cảm biến IMU (Gia tốc + Con quay) kèm tọa độ GPS lên Backend.
  /// Backend phân tích và tạo bản ghi sự kiện té ngã [SafetyEvent] ở trạng thái OPEN.
  Future<SafetyEvent?> sendImuData({
    required double accelerometerX,
    required double accelerometerY,
    required double accelerometerZ,
    required double gyroscopeX,
    required double gyroscopeY,
    required double gyroscopeZ,
    required DateTime timestamp,
    bool onDeviceFallConfirmed = false,
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
      'onDeviceFallConfirmed': onDeviceFallConfirmed,
      'signalId': ?signalId,
      'latitude': ?latitude,
      'longitude': ?longitude,
    });
    final payload = data['data'];
    return payload is Map<String, dynamic>
        ? SafetyEvent.fromJson(payload)
        : null;
  }

  /// Lấy danh sách lịch sử các sự kiện an toàn / té ngã.
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

  /// Tạo sự kiện kiểm tra cử chỉ cảm biến (Sensor Self-Test / Diễn tập).
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

  /// Hoàn tất diễn tập kiểm tra cảm biến.
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

  /// Xác nhận an toàn: Người dùng nhấn nút "Tôi vẫn ổn" trên màn hình đếm ngược.
  Future<SafetyEvent> confirmSafetyCheck(String eventId, {String? note}) async {
    final data = await apiPost('/api/v1/safety/events/$eventId/confirm', {
      'note': note,
    });
    return SafetyEvent.fromJson(data['data'] as Map<String, dynamic>);
  }

  /// Báo phát hiện nhầm: Người dùng phản hồi là báo động giả.
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

  /// Kích hoạt cảnh báo khẩn cấp (Emergency Alert) gửi tới người thân trong gia đình (Family members).
  /// Được gọi khi hết thời gian đếm ngược 30s hoặc khi người dùng yêu cầu trợ giúp.
  Future<void> sendEmergencyAlertForEvent(String eventId) async {
    await apiPost('/api/v1/safety/events/$eventId/emergency-alert', const {});
  }
}
