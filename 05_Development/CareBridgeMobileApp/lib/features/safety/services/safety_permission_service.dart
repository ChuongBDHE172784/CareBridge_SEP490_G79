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

  /// Toạ độ đọc lại trong vòng ngần này được coi là còn dùng được ngay.
  static const _freshEnough = Duration(minutes: 2);

  /// Kiểm tra dịch vụ GPS, quyền truy cập vị trí và lấy toạ độ.
  ///
  /// Trước đây hàm này đòi `LocationAccuracy.high` trong đúng 5 giây. GPS khởi
  /// động lạnh, nhất là trong nhà, gần như không bao giờ kịp — máy thật ném
  /// `TimeoutException after 0:00:05`, và màn bản đồ khẩn cấp đứng ở "Đang chờ vị
  /// trí", tìm cơ sở y tế ra 0 kết quả, đúng lúc người dùng cần nó nhất.
  ///
  /// Giờ đi theo hai bước. Toạ độ đã lưu trong máy còn mới thì dùng luôn, không
  /// chờ gì cả. Không có thì mới đợi bản đo mới, với hạn rộng hơn và độ chính xác
  /// vừa phải — sai số hàng chục mét không đổi câu trả lời cho "bệnh viện trong
  /// bán kính 5 km", mà lại cho phép định vị bằng sóng di động và Wi-Fi thay vì
  /// chờ bắt đủ vệ tinh. Hết giờ thì vẫn trả toạ độ cũ còn hơn trả về rỗng.
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

    Position? lastKnown;
    try {
      lastKnown = await Geolocator.getLastKnownPosition();
    } catch (_) {
      lastKnown = null;
    }

    final timestamp = lastKnown?.timestamp;
    if (lastKnown != null &&
        DateTime.now().difference(timestamp ?? DateTime.now()) < _freshEnough) {
      return lastKnown;
    }

    try {
      return await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.medium,
        timeLimit: const Duration(seconds: 20),
      );
    } on TimeoutException {
      return lastKnown;
    } catch (_) {
      return lastKnown;
    }
  }
}
