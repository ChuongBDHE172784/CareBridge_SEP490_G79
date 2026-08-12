# Hướng dẫn Công thức, Chỉ số & Giải thích Mã Nguồn Thuật toán Phát hiện Ngã (CareBridge Fall Detection Guide)

> **Mô tả**: Tài liệu giải thích chuyên sâu các công thức toán học, thông số chỉ số kỹ thuật, và **phân tích chi tiết từng đoạn code** thực thi thuật toán phát hiện ngã (Fall Detection) trên ứng dụng di động Mobile App và server Backend API.
> **File tài liệu**: `docs/FALL_DETECTION_FORMULAS_AND_METRICS_GUIDE.md`

---

## 📌 1. Tổng quan Kiến trúc Thuật toán (Architecture Overview)

Hệ thống phát hiện ngã CareBridge được xây dựng theo kiến trúc 2 lớp chính:
1. **Lớp Mobile App (On-Device Real-time Detection)**:
   - Lắng nghe luồng cảm biến IMU (*Accelerometer - Gia tốc kế* và *Gyroscope - Con quay hồi chuyển*) với tần số mẫu **50Hz** (20ms mỗi mẫu).
   - Chạy **Máy trạng thái 3 giai đoạn (3-Phase State Machine)** trực tiếp trên thiết bị (On-Device) trong file [`imu_fall_detector.dart`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/safety/services/imu_fall_detector.dart) để phát hiện tai nạn ngã ngay lập tức, không phụ thuộc vào kết nối mạng.
2. **Lớp Backend API (Server Validation & Classification)**:
   - Tiếp nhận sự cố từ di động gửi lên qua dịch vụ [`FallDetectionAlgorithmService.java`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/service/impl/FallDetectionAlgorithmService.java).
   - Phân tích lọc gia tốc thuần, đối chiếu mức độ nhạy cài đặt người dùng (`LOW`, `MEDIUM`, `HIGH`) tại [`SensitivityLevel.java`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/SensitivityLevel.java) để phân loại sự cố (`SUSPECTED_FALL` hay `SUSPECTED_IMPACT`) và điều phối cuộc gọi/tin nhắn khẩn cấp.

---

## 📁 2. Danh mục File Mã Nguồn Thuật toán & Kiểm thử

| Thành phần | Đường dẫn File mã nguồn | Vai trò & Nhiệm vụ |
| :--- | :--- | :--- |
| **Mobile App (Core Algorithm)** | [`05_Development/CareBridgeMobileApp/lib/features/safety/services/imu_fall_detector.dart`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/safety/services/imu_fall_detector.dart) | Thuật toán cốt lõi 3 giai đoạn (Rơi tự do $\rightarrow$ Va chạm $\rightarrow$ Bất động), tính toán gia tốc tổng, tốc độ quay, Jerk và tỷ lệ tĩnh. |
| **Mobile App (Self-Test Gesture)** | [`05_Development/CareBridgeMobileApp/lib/features/safety/services/safety_demo_mode.dart`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/safety/services/safety_demo_mode.dart) | Bộ nhận diện cử chỉ vung tay thử nghiệm hoặc thả ngã bề mặt mềm có chủ đích (`SafetyDemoGestureDetector`). |
| **Mobile App (Sensor Coordinator)** | [`05_Development/CareBridgeMobileApp/lib/features/safety/services/fall_detection_sensor_service.dart`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/safety/services/fall_detection_sensor_service.dart) | Quản lý luồng cảm biến `sensors_plus`, điều phối mẫu IMU vào detector, phát sự kiện ngã & chẩn đoán IMU. |
| **Backend API (Algorithm Service)** | [`05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/service/impl/FallDetectionAlgorithmService.java`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/service/impl/FallDetectionAlgorithmService.java) | Tính toán gia tốc thuần loại bỏ trọng lực ($9.81 \text{ m/s}^2$), so sánh ngưỡng độ nhạy để phân loại `SUSPECTED_FALL` hay `SUSPECTED_IMPACT`. |
| **Backend API (Sensitivity Level)** | [`05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/SensitivityLevel.java`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/SensitivityLevel.java) | Định nghĩa các mức ngưỡng độ nhạy `LOW` ($15.0 \text{ m/s}^2$), `MEDIUM` ($12.0 \text{ m/s}^2$), `HIGH` ($9.0 \text{ m/s}^2$). |
| **Mobile Unit Test** | [`05_Development/CareBridgeMobileApp/test/features/safety/imu_fall_detector_test.dart`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/test/features/safety/imu_fall_detector_test.dart) | Kiểm thử đơn vị cho toàn bộ các kịch bản ngã thật, ngã mềm, từ chối dữ liệu rác, gyroscope cũ. |

---

## 🧮 3. Các Công thức Toán học Sử dụng trong Thuật toán

### 3.1 Gia tốc Tổng hợp (Acceleration Magnitude)
Tính lực tổng hợp tác động lên cảm biến gia tốc từ 3 trục $(x, y, z)$:
$$\text{accelerationMagnitude} = \sqrt{a_x^2 + a_y^2 + a_z^2}$$

### 3.2 Tốc độ Quay Tổng hợp (Gyroscope Magnitude)
Tính tốc độ xoay tổng hợp của thiết bị trong không gian từ 3 trục con quay $(\omega_x, \omega_y, \omega_z)$:
$$\text{gyroscopeMagnitude} = \sqrt{\omega_x^2 + \omega_y^2 + \omega_z^2}$$

### 3.3 Độ Giật Gia tốc (Jerk - Tốc độ biến thiên gia tốc tức thời)
Đo độ chấn động/sốc đột ngột khi xảy ra va chạm giữa 2 mẫu liên tiếp:
$$\text{Jerk} = \frac{|\text{accelerationMagnitude}_{\text{current}} - \text{accelerationMagnitude}_{\text{previous}}|}{\Delta t}$$
*(Trong đó $\Delta t = \text{elapsedMicros} / 1.000.000$ tính bằng giây).*

### 3.4 Độ Lệch Gia tốc so với Trọng lực ($g = 9.81 \text{ m/s}^2$)
$$\text{accelerationDeviation} = |\text{accelerationMagnitude} - 9.81|$$

### 3.5 Tỷ lệ Mẫu Đứng yên Sau Va chạm (Stationary Sample Ratio)
$$\text{Stationary Ratio} = \frac{\text{\_stationaryPostImpactSamples}}{\text{\_postImpactSamples}}$$

---

## 📱 4. GIẢI THÍCH CHI TIẾT CODE THUẬT TOÁN MOBILE APP (`imu_fall_detector.dart`)

Mã nguồn [`imu_fall_detector.dart`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/safety/services/imu_fall_detector.dart) là trái tim phát hiện ngã trên thiết bị di động. Dưới đây là giải thích chi tiết cấu trúc dữ liệu, các biến hằng số, và từng hàm logic trong mã nguồn:

### 4.1 Đống Cấu trúc Dữ liệu & Mẫu Cảm biến (Data Classes & Models)

```dart
enum FallDetectionPhase { idle, freeFall, impact }
```
- **`FallDetectionPhase`**: Định nghĩa 3 trạng thái chính của máy trạng thái:
  - `idle`: Trạng thái bình thường, đang chờ phát hiện pha rơi tự do.
  - `freeFall`: Đã phát hiện rơi tự do (gia tốc $< 7.2\text{ m/s}^2$), đang chờ va chạm.
  - `impact`: Đã phát hiện va chạm (gia tốc $> 9.5\text{ m/s}^2$), đang theo dõi giai đoạn bất động.

```dart
class ImuSample {
  final double accelerometerX, accelerometerY, accelerometerZ;
  final double gyroscopeX, gyroscopeY, gyroscopeZ;
  final DateTime timestamp;
  final DateTime? gyroscopeTimestamp;

  double get accelerationMagnitude => sqrt(
    accelerometerX * accelerometerX +
        accelerometerY * accelerometerY +
        accelerometerZ * accelerometerZ,
  );

  double get gyroscopeMagnitude => sqrt(
    gyroscopeX * gyroscopeX + gyroscopeY * gyroscopeY + gyroscopeZ * gyroscopeZ,
  );
}
```
- **`ImuSample`**: Đối tượng dữ liệu đóng gói các thông số cảm biến tại một thời điểm $t$.
  - Tự động tính toán getter `accelerationMagnitude` và `gyroscopeMagnitude` theo công thức căn bậc hai tổng bình phương 3 trục.

```dart
enum ImuDetectorDecisionReason {
  awaitingFreeFall, freeFallDetected, freeFallTooShort, awaitingImpact,
  impactWindowExpired, jerkTooLow, gyroscopeMissing, gyroscopeStale,
  impactDetected, impactSettling, sampleGap, excessiveMovement,
  awaitingImmobility, insufficientStationarySamples, accepted, cooldown, outOfOrder, reset
}
```
- **`ImuDetectorDecisionReason`**: 18 mã lý do được ghi nhận minh bạch cho mỗi mẫu dữ liệu đầu vào. Giúp UI/Debugger biết chính xác lý do thuật toán chấp nhận ngã hoặc từ chối sự cố (như `gyroscopeStale`, `jerkTooLow`, `excessiveMovement`, ...).

---

### 4.2 Các Hằng số & Ngưỡng Quyết định trong Class `ImuFallDetector`

```dart
class ImuFallDetector {
  static const double freeFallThreshold = 7.2;       // Ngưỡng gia tốc rơi tự do (< 7.2 m/s²)
  static const double impactThreshold = 9.5;         // Ngưỡng va chạm tiêu chuẩn (> 9.5 m/s²)
  static const double softLandingImpactThreshold = 8.5; // Ngưỡng va chạm hạ cánh mềm (> 8.5 m/s²)
  static const double minimumJerk = 40.0;            // Ngưỡng Jerk tối thiểu tiêu chuẩn (40 m/s³)
  static const double minimumSoftFallJerk = 20.0;    // Ngưỡng Jerk hạ cánh mềm (20 m/s³)
  static const double gravity = 9.81;                // Trọng lực chuẩn (9.81 m/s²)

  static const double stationaryAccelerationTolerance = 2.0; // Dung sai gia tốc khi bất động (±2.0 m/s²)
  static const double stationaryGyroscopeThreshold = 0.5;    // Ngưỡng con quay tĩnh (0.5 rad/s)
  static const double cancellationGyroscopeThreshold = 1.5;   // Ngưỡng con quay hủy ngã (> 1.5 rad/s)
  static const double strongMovementAccelerationDeviation = 6.0; // Gia tốc lệch hủy ngã (> 6.0 m/s²)

  static const double minimumStationaryRatio = 0.8;  // Tỷ lệ đứng yên bắt buộc ≥ 80% (0.8)
  static const double minimumSoftLandingStationaryRatio = 0.6; // Tỷ lệ tĩnh ngã mềm ≥ 60% (0.6)

  static const Duration minimumFreeFallDuration = Duration(milliseconds: 60); // Thời gian rơi tối thiểu 60ms
  static const Duration impactWindow = Duration(milliseconds: 1500);          // Cửa sổ chờ va chạm 1500ms
  static const Duration immobilityWindow = Duration(seconds: 1);              // Cửa sổ kiểm tra bất động 1s
  static const Duration maximumGyroscopeAge = Duration(milliseconds: 200);   // Tuổi thọ gyro tối đa 200ms
  static const Duration cooldown = Duration(seconds: 3);                       // Thời gian hồi 3s
}
```

---

### 4.3 Hàm Điểm Vào Xử Lý Mẫu Cảm Biến: `addSample(ImuSample sample)`

Hàm `addSample` được gọi mỗi khi luồng cảm biến $50\text{Hz}$ đẩy một mẫu `ImuSample` mới vào:

```dart
FallCandidate? addSample(ImuSample sample) {
  final previous = _previousSample;
  
  // 1. Kiểm tra mẫu lỗi thứ tự thời gian (Out-of-order timestamp check)
  if (previous != null && !sample.timestamp.isAfter(previous.timestamp)) {
    _resetCandidate();
    _previousSample = sample;
    _setDecision(ImuDetectorDecisionReason.outOfOrder);
    return null;
  }
  _previousSample = sample;

  // 2. Kiểm tra thời gian hồi (Cooldown check)
  final cooldownUntil = _cooldownUntil;
  if (cooldownUntil != null && sample.timestamp.isBefore(cooldownUntil)) {
    _setDecision(ImuDetectorDecisionReason.cooldown);
    return null;
  }
  if (cooldownUntil != null) _cooldownUntil = null;

  // 3. Phân luồng điều phối theo Pha (Phase Dispatching)
  switch (_phase) {
    case FallDetectionPhase.idle:
      if (sample.accelerationMagnitude < freeFallThreshold) {
        _phase = FallDetectionPhase.freeFall;
        _freeFallAt = sample.timestamp;
        _setDecision(ImuDetectorDecisionReason.freeFallDetected);
      } else {
        _setDecision(ImuDetectorDecisionReason.awaitingFreeFall);
      }
      return null;

    case FallDetectionPhase.freeFall:
      return _handleFreeFall(sample, previous);

    case FallDetectionPhase.impact:
      return _handlePostImpact(sample);
  }
}
```

**Giải thích Logic `addSample`**:
1. **Chống nhiễu thời gian**: Nếu thời gian mẫu mới nhỏ hơn hoặc bằng mẫu trước đó $\rightarrow$ Đặt lý do `outOfOrder` và reset candidate.
2. **Thời gian hồi (Cooldown)**: Nếu chưa hết $3\text{s}$ hồi sau sự cố trước đó $\rightarrow$ Trả về `null` với lý do `cooldown`.
3. **Chuyển pha `idle` $\rightarrow$ `freeFall`**: Khi ở trạng thái `idle`, nếu gia tốc tổng $< 7.2\text{ m/s}^2$ $\rightarrow$ Máy chuyển sang pha `freeFall` và lưu lại mốc thời gian `_freeFallAt`.

---

### 4.4 Logic Pha Rơi Tự Do & Phát Hiện Va Chạm: `_handleFreeFall(...)`

```dart
FallCandidate? _handleFreeFall(ImuSample sample, ImuSample? previous) {
  final freeFallAt = _freeFallAt;

  // 1. Hết hạn cửa sổ chờ va chạm (Over 1500ms)
  if (freeFallAt == null || sample.timestamp.difference(freeFallAt) > impactWindow) {
    _resetCandidate();
    if (sample.accelerationMagnitude < freeFallThreshold) {
      _phase = FallDetectionPhase.freeFall;
      _freeFallAt = sample.timestamp;
    }
    _setDecision(ImuDetectorDecisionReason.impactWindowExpired);
    return null;
  }

  // 2. Thời gian rơi tự do quá ngắn (< 60ms)
  final freeFallDuration = sample.timestamp.difference(freeFallAt);
  if (freeFallDuration < minimumFreeFallDuration) {
    if (sample.accelerationMagnitude > impactThreshold) {
      _resetCandidate();
      _setDecision(ImuDetectorDecisionReason.freeFallTooShort);
    } else {
      _setDecision(ImuDetectorDecisionReason.awaitingImpact);
    }
    return null;
  }

  // 3. Xác định ngưỡng va chạm theo loại ngã (Tiêu chuẩn 9.5 m/s² hay Hạ cánh mềm 8.5 m/s²)
  final impactThresholdForSequence = freeFallDuration >= softFallQualificationDuration
      ? softLandingImpactThreshold
      : impactThreshold;
  if (sample.accelerationMagnitude <= impactThresholdForSequence || previous == null) {
    _setDecision(ImuDetectorDecisionReason.awaitingImpact);
    return null;
  }

  // 4. Tính toán độ giật Jerk (Δa / Δt)
  final elapsedMicros = sample.timestamp.difference(previous.timestamp).inMicroseconds;
  if (elapsedMicros <= 0) {
    _resetCandidate();
    _setDecision(ImuDetectorDecisionReason.outOfOrder);
    return null;
  }
  final elapsedSeconds = elapsedMicros / Duration.microsecondsPerSecond;
  final jerk = (sample.accelerationMagnitude - previous.accelerationMagnitude).abs() / elapsedSeconds;
  
  final requiredJerk = freeFallDuration >= softFallQualificationDuration
      ? minimumSoftFallJerk
      : minimumJerk;

  if (jerk < requiredJerk) {
    _setDecision(ImuDetectorDecisionReason.jerkTooLow);
    return null;
  }

  // 5. Kiểm tra độ tươi Gyroscope (Tuổi gyro ≤ 200ms)
  if (!_hasFreshGyroscope(sample)) {
    _resetCandidate();
    _setDecision(_gyroscopeFailureReason(sample));
    return null;
  }

  // 6. CHUYỂN SANG PHA IMPACT (Va chạm được xác nhận!)
  _phase = FallDetectionPhase.impact;
  _impactStartedAt = sample.timestamp;
  _impactSample = sample;
  _postImpactSamples = 0;
  _stationaryPostImpactSamples = 0;
  _lastPostImpactSampleAt = sample.timestamp.add(impactSettlingGrace);
  _setDecision(ImuDetectorDecisionReason.impactDetected);
  return null;
}
```

**Giải thích Logic `_handleFreeFall`**:
- **Cửa sổ 1500ms**: Nếu quá 1.5s kể từ lúc bắt đầu rơi tự do mà chưa va chạm $\rightarrow$ Đặt lý do `impactWindowExpired`.
- **Lọc vi nhiễu 60ms**: Nếu thời gian rơi $< 60\text{ms}$ mà có va chạm $\rightarrow$ Đặt lý do `freeFallTooShort` (chống nhiễu do gõ nhẹ điện thoại lên mặt bàn).
- **Tính toán Jerk**: Lấy gia tốc biến thiên chia cho khoảng thời gian microseconds $\rightarrow$ Nếu $\text{Jerk} < 40.0\text{ m/s}^3$ ($20.0\text{ m/s}^3$ với hạ cánh mềm) $\rightarrow$ Từ chối với lý do `jerkTooLow`.
- **Cơ chế Fail-Safe Gyroscope**: Hàm `_hasFreshGyroscope(sample)` đảm bảo tuổi thọ con quay hồi chuyển $\le 200\text{ms}$. Nếu cảm biến gyro dừng phát dữ liệu $\rightarrow$ Đặt lý do `gyroscopeStale` hoặc `gyroscopeMissing`.
- **Chuyển pha `freeFall` $\rightarrow$ `impact`**: Đặt mốc thời gian va chạm `_impactStartedAt` và chuẩn bị theo dõi giai đoạn bất động.

---

### 4.5 Logic Theo Dõi Bất Động & Xác Nhận Ngã Thật: `_handlePostImpact(...)`

```dart
FallCandidate? _handlePostImpact(ImuSample sample) {
  final impact = _impactSample;
  final impactStartedAt = _impactStartedAt;
  if (impact == null || impactStartedAt == null) {
    _resetCandidate();
    _setDecision(ImuDetectorDecisionReason.reset);
    return null;
  }

  final sinceFirstImpact = sample.timestamp.difference(impactStartedAt);

  // 1. Khoảng thời gian lắng ổn định va chạm (Settling Grace 250ms)
  if (sinceFirstImpact <= impactSettlingGrace) {
    if (sample.accelerationMagnitude > impact.accelerationMagnitude + 1 &&
        sample.accelerationMagnitude > impactThreshold &&
        _hasFreshGyroscope(sample)) {
      _impactSample = sample; // Lưu đỉnh gia tốc cao hơn trong xung va chạm đa đợt
    }
    _setDecision(ImuDetectorDecisionReason.impactSettling);
    return null;
  }

  // 2. Kiểm tra độ tươi Gyroscope & Khoảng trống rớt mẫu dữ liệu (> 500ms gap)
  if (!_hasFreshGyroscope(sample)) {
    _resetCandidate();
    _setDecision(_gyroscopeFailureReason(sample));
    return null;
  }
  final lastPostImpactSampleAt = _lastPostImpactSampleAt;
  if (lastPostImpactSampleAt == null ||
      sample.timestamp.difference(lastPostImpactSampleAt) > maximumPostImpactSampleGap) {
    _resetCandidate();
    _setDecision(ImuDetectorDecisionReason.sampleGap);
    return null;
  }
  _lastPostImpactSampleAt = sample.timestamp;

  // 3. TỰ ĐỘNG HỦY CẢNH BÁO NẾU CÓ CHUYỂN ĐỘNG ĐỘT NGỘT (Excessive Movement Check)
  final accelerationDeviation = (sample.accelerationMagnitude - gravity).abs();
  final freeFallAt = _freeFallAt;
  final freeFallDuration = freeFallAt == null ? Duration.zero : impactStartedAt.difference(freeFallAt);
  final isLongSoftFall = freeFallDuration >= softFallQualificationDuration;

  final movementGyroscopeThreshold = isLongSoftFall
      ? softLandingCancellationGyroscopeThreshold  // 2.5 rad/s
      : cancellationGyroscopeThreshold;           // 1.5 rad/s
  final movementAccelerationDeviation = isLongSoftFall
      ? softLandingStrongMovementAccelerationDeviation // 8.0 m/s²
      : strongMovementAccelerationDeviation;           // 6.0 m/s²

  if (sample.gyroscopeMagnitude > movementGyroscopeThreshold ||
      accelerationDeviation > movementAccelerationDeviation) {
    _resetCandidate(); // TỰ ĐỘNG HỦY! Người dùng đã ngồi dậy/nhặt máy cử động mạnh!
    _setDecision(ImuDetectorDecisionReason.excessiveMovement);
    return null;
  }

  // 4. Đếm số mẫu tĩnh (Stationary sample counting)
  _postImpactSamples++;
  final stationaryAccelerationLimit = isLongSoftFall
      ? softLandingStationaryAccelerationTolerance   // 2.8 m/s²
      : stationaryAccelerationTolerance;            // 2.0 m/s²
  final stationaryGyroscopeLimit = isLongSoftFall
      ? softLandingStationaryGyroscopeThreshold      // 0.9 rad/s
      : stationaryGyroscopeThreshold;               // 0.5 rad/s

  if (accelerationDeviation <= stationaryAccelerationLimit &&
      sample.gyroscopeMagnitude <= stationaryGyroscopeLimit) {
    _stationaryPostImpactSamples++;
  }

  // 5. Kiểm tra thời gian chờ đủ cửa sổ bất động (1000ms hoặc 600ms)
  final requiredImmobilityWindow = isLongSoftFall
      ? softLandingImmobilityWindow  // 600ms
      : immobilityWindow;            // 1000ms

  if (sinceFirstImpact < requiredImmobilityWindow) {
    _setDecision(ImuDetectorDecisionReason.awaitingImmobility);
    return null;
  }

  // 6. TÍNH TỶ LỆ MẪU TĨNH & XÁC NHẬN SỰ CỐ NGÃ THẬT!
  final ratio = _postImpactSamples == 0 ? 0.0 : _stationaryPostImpactSamples / _postImpactSamples;
  _resetCandidate();

  final requiredStationaryRatio = isLongSoftFall
      ? minimumSoftLandingStationaryRatio  // 60%
      : minimumStationaryRatio;           // 80%

  if (ratio < requiredStationaryRatio) {
    _setDecision(ImuDetectorDecisionReason.insufficientStationarySamples);
    return null;
  }

  // ĐÃ ĐẠT ĐẦY ĐỦ 3 GIAI ĐOẠN -> XÁC NHẬN NGÃ THẬT!
  _cooldownUntil = sample.timestamp.add(cooldown);
  _setDecision(ImuDetectorDecisionReason.accepted);
  return FallCandidate(impactSample: impact, stationarySampleRatio: ratio);
}
```

**Giải thích Logic `_handlePostImpact`**:
1. **Lắng va chạm 250ms (`impactSettlingGrace`)**: Trong $250\text{ms}$ đầu sau va chạm, nếu xuất hiện đỉnh gia tốc va chạm mạnh hơn $\rightarrow$ Cập nhật lại `_impactSample` để giữ gia tốc va chạm đỉnh chính xác nhất.
2. **Loại bỏ hiện tượng nghẽn luồng mẫu (`maximumPostImpactSampleGap = 500ms`)**: Nếu ứng dụng bị giật đơ làm hổng mất dữ liệu cảm biến $> 500\text{ms}$ $\rightarrow$ Hủy sự cố với lý do `sampleGap` để tránh báo động nhầm.
3. **Cơ chế Tự Động Hủy Cảnh Báo (`excessiveMovement`)**: Nếu người dùng nhặt điện thoại lên hoặc xoay cổ tay mạnh ($> 1.5\text{ rad/s}$ hoặc gia tốc lệch $> 6.0\text{ m/s}^2$) $\rightarrow$ Thuật toán xác định nạn nhân vẫn tỉnh táo cử động bình thường $\rightarrow$ Tự động hủy cảnh báo!
4. **Tính Tỷ lệ Đứng yên (`Stationary Ratio`)**: Trong suốt 1 giây theo dõi, số mẫu có gia tốc lệch $\le 2.0\text{ m/s}^2$ và tốc độ xoay $\le 0.5\text{ rad/s}$ được đếm lại. Nếu tỷ lệ mẫu tĩnh $\ge 80\%$ (hoặc $\ge 60\%$ ngã mềm) $\rightarrow$ Trả về đối tượng [`FallCandidate`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/safety/services/imu_fall_detector.dart#L65-L73) và phát tín hiệu báo ngã lên giao diện!

---

## ✋ 5. Chi tiết Thuật toán Thử Nghiệm Vung Tay (`safety_demo_mode.dart`)

Lớp [`SafetyDemoGestureDetector`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/safety/services/safety_demo_mode.dart#L23-L233) hỗ trợ người dùng/giám khảo kiểm thử tính năng đếm ngược an toàn bằng cử chỉ vung tay có chủ đích mà không làm báo ngã nhầm:

```dart
class SafetyDemoGestureDetector {
  static const double stationaryAccelerationTolerance = 0.8; // Cầm đứng yên ít lắc (±0.8 m/s²)
  static const double stationaryRotationMagnitude = 0.25;    // Cầm đứng yên không xoay (≤ 0.25 rad/s)
  static const double motionStartAccelerationDeviation = 3.0; // Gia tốc bắt đầu vung (≥ 3.0 m/s²)
  static const double motionStartRotationMagnitude = 1.4;    // Vận tốc góc bắt đầu vung (≥ 1.4 rad/s)
  static const double requiredPeakAccelerationDeviation = 5.0; // Đỉnh gia tốc vung tay (≥ 5.0 m/s²)
  static const double requiredPeakRotationMagnitude = 2.0;    // Đỉnh con quay xoay (≥ 2.0 rad/s)
  static const double minimumEstimatedTravelMetres = 0.45;   // Sải tay vung tích phân ≥ 45 cm
  static const Duration minimumMotionDuration = Duration(milliseconds: 250); // Vung dứt khoát ≥ 250ms
  static const Duration maximumMotionDuration = Duration(milliseconds: 900); // Vung không quá 900ms
}
```

- **Tích phân Quãng đường Vung (Travel Integration)**:
  $$\text{Travel} = \int v \, dt + \frac{1}{2} a_{\text{effective}} \, t^2 \ge 0.45 \text{ m}$$
  Nếu người dùng chỉ lắc cổ tay vài cm tại chỗ $\rightarrow$ Quãng đường tích phân không đủ $45\text{cm} \rightarrow$ Hệ thống bỏ qua, ngăn ngừa kích hoạt nhầm.

---

## 🖥️ 6. Thuật toán Lọc & Phân Loại sự cố trên Backend (`FallDetectionAlgorithmService.java`)

Khi sự cố ngã được ứng dụng Mobile phát hiện và gửi dữ liệu IMU lên Server, dịch vụ [`FallDetectionAlgorithmService`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/service/impl/FallDetectionAlgorithmService.java) phân tích lại theo ngưỡng cài đặt của người dùng:

```java
@Service
public class FallDetectionAlgorithmService implements IFallDetectionAlgorithmService {

    private static final double GRAVITY = 9.81;

    @Override
    public FallAnalysisResult analyze(ImuDataPayload payload, String sensitivityLevel) {
        // 1. Tính gia tốc thuần loại bỏ trọng lực 9.81 m/s²
        double magnitude = Math.sqrt(
                payload.accelerometerX() * payload.accelerometerX() +
                payload.accelerometerY() * payload.accelerometerY() +
                payload.accelerometerZ() * payload.accelerometerZ()
        ) - GRAVITY;

        // 2. Lấy ngưỡng gia tốc theo mức độ nhạy (LOW: 15.0, MEDIUM: 12.0, HIGH: 9.0)
        double threshold = SensitivityLevel.valueOf(sensitivityLevel.toUpperCase()).getThreshold();

        // 3. Phân loại loại sự cố
        if (magnitude >= threshold) {
            SafetyEventType eventType = magnitude >= threshold * 1.5
                    ? SafetyEventType.SUSPECTED_FALL      // Nghi ngờ ngã nghiêm trọng (Gia tốc cực lớn)
                    : SafetyEventType.SUSPECTED_IMPACT;    // Nghi ngờ va chạm
            return new FallAnalysisResult(true, eventType, magnitude);
        }
        return new FallAnalysisResult(false, SafetyEventType.FALSE_ALARM, magnitude);
    }
}
```

---

## 📊 7. Bảng So sánh Tổng hợp Ngưỡng & Thông số Toàn Hệ thống

| Tiêu chí / Thông số | Ngã Thật Tiêu Chuẩn | Ngã Hạ Cánh Mềm (Soft-landing) | Thử Nghiệm Vung Tay (Self-Test) | Server Backend Threshold |
| :--- | :--- | :--- | :--- | :--- |
| **Gia tốc Rơi tự do** | $< 7.2 \text{ m/s}^2$ | $< 7.2 \text{ m/s}^2$ | $< 6.5 \text{ m/s}^2$ | N/A |
| **Thời gian Rơi tối thiểu** | $\ge 60 \text{ ms}$ | $\ge 60 \text{ ms}$ | $\ge 40 \text{ ms}$ | N/A |
| **Gia tốc Va chạm** | $> 9.5 \text{ m/s}^2$ | $> 8.5 \text{ m/s}^2$ | Gia tốc vung đỉnh $\ge 14.81 \text{ m/s}^2$ | `HIGH`: $9.0$, `MED`: $12.0$, `LOW`: $15.0$ |
| **Ngưỡng Jerk tối thiểu** | $\ge 40.0 \text{ m/s}^3$ | $\ge 20.0 \text{ m/s}^3$ | N/A (Sải tay vung $\ge 45\text{cm}$) | N/A |
| **Thời gian Bất động** | $1000 \text{ ms}$ | $600 \text{ ms}$ | N/A (Vung dứt khoát 250ms–900ms) | N/A |
| **Tỷ lệ Mẫu đứng yên** | $\ge 80\%$ | $\ge 60\%$ | N/A | N/A |
| **Tự động Hủy cảnh báo** | Gyro $> 1.5 \text{ rad/s}$ | Gyro $> 2.5 \text{ rad/s}$ | Vung quá chậm $> 900\text{ms}$ | Magnitude $<$ Threshold $\rightarrow$ `FALSE_ALARM` |
| **Thời gian hồi (Cooldown)** | $3 \text{ giây}$ | $3 \text{ giây}$ | $2 \text{ giây}$ | N/A |
