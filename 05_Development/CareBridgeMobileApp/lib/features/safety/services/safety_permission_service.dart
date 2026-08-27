import 'dart:async';

import 'package:geolocator/geolocator.dart';
import 'package:sensors_plus/sensors_plus.dart';

typedef SensorAccessProbe = Future<void> Function();
typedef LocationReader = Future<Position?> Function();

/// Dịch vụ xác minh quyền phần cứng và đọc tọa độ GPS vị trí an toàn cho phân hệ Safety.
class SafetyPermissionService {
  SafetyPermissionService({
    SensorAccessProbe? sensorProbe,
    LocationReader? locationReader,
  }) : _sensorProbe = sensorProbe ?? _defaultSensorProbe,
       _locationReader = locationReader ?? _defaultLocationReader;

  final SensorAccessProbe _sensorProbe;
  final LocationReader _locationReader;

  /// Xác minh quyền truy cập và tính sẵn sàng của cảm biến chuyển động (Gia tốc kế & Con quay hồi chuyển).
  /// Trả về `true` nếu thiết bị phát ra được dữ liệu cảm biến trong vòng 3 giây.
  Future<bool> attestSensorAccess() async {
    try {
      await _sensorProbe();
      return true;
    } catch (_) {
      return false;
    }
  }

  /// Đọc tọa độ GPS thời gian thực của thiết bị khi người dùng đã cấp quyền định vị.
  Future<Position?> readConsentedLocation() => _locationReader();

  /// Thử nhận mẫu dữ liệu đầu tiên từ Gia tốc kế và Con quay hồi chuyển để kiểm tra phần cứng.
  static Future<void> _defaultSensorProbe() async {
    await Future.wait([
      accelerometerEventStream().first.timeout(const Duration(seconds: 3)),
      gyroscopeEventStream().first.timeout(const Duration(seconds: 3)),
    ]);
  }

  /// Kiểm tra dịch vụ GPS, quyền truy cập vị trí và lấy tọa độ chính xác cao (LocationAccuracy.high).
  static Future<Position?> _defaultLocationReader() async {
    if (!await Geolocator.isLocationServiceEnabled()) return null;
    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }
    if (permission == LocationPermission.denied ||
        permission == LocationPermission.deniedForever) {
      return null;
    }
    return Geolocator.getCurrentPosition(
      desiredAccuracy: LocationAccuracy.high,
      timeLimit: const Duration(seconds: 5),
    );
  }
}
