import 'dart:math';

enum FallDetectionPhase { idle, freeFall, impact }

enum ImuDetectorDecisionReason {
  awaitingFreeFall,
  freeFallDetected,
  freeFallTooShort,

  /// chờ xung lực va chạm
  awaitingImpact,
  impactWindowExpired,
  jerkTooLow,

  /// Thiếu dữ liệu con quay hồi chuyển
  gyroscopeMissing,

  /// Dữ liệu con quay hồi chuyển bị trễ/lỗi thời gian so với gia tốc kế.
  gyroscopeStale,

  /// Đã phát hiện xung lực va chạm vượt ngưỡng.
  impactDetected,

  /// Đang trong giai đoạn chờ ổn định ngay sau va đập (Settling Grace Period).
  impactSettling,

  /// Mất mẫu hoặc khoảng cách giữa 2 mẫu sau va đập quá lớn (>500ms).
  sampleGap,

  /// Thiết bị di chuyển quá nhiều sau va chạm
  excessiveMovement,

  /// Đang theo dõi giai đoạn nằm yên sau va đập.
  awaitingImmobility,

  /// Tỷ lệ mẫu nằm yên sau va chạm không đạt mức tối thiểu quy định.
  insufficientStationarySamples,

  /// Đã xác nhận một cú ngã hoàn chỉnh (hợp lệ qua tất cả các pha).
  accepted,

  /// Đang trong thời gian hạ nhiệt (Cooldown) để chống gửi cảnh báo trùng lặp.
  cooldown,

  /// Dấu thời gian mẫu bị đảo lộn thứ tự (mẫu sau có timestamp trước mẫu trước).
  outOfOrder,

  reset,
}

/// Kết quả quyết định hiện tại của bộ phát hiện té ngã kèm lý do chi tiết.
class ImuDetectorDecision {
  const ImuDetectorDecision({required this.phase, required this.reason});

  final FallDetectionPhase phase;
  final ImuDetectorDecisionReason reason;
}

/// Mẫu dữ liệu cảm biến IMU đồng bộ tại một thời điểm (Gia tốc kế + Con quay hồi chuyển).
class ImuSample {
  const ImuSample({
    required this.accelerometerX,
    required this.accelerometerY,
    required this.accelerometerZ,
    required this.gyroscopeX,
    required this.gyroscopeY,
    required this.gyroscopeZ,
    required this.timestamp,
    required this.gyroscopeTimestamp,
  });

  final double accelerometerX;
  final double accelerometerY;
  final double accelerometerZ;
  final double gyroscopeX;
  final double gyroscopeY;
  final double gyroscopeZ;
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

/// Pass
class FallCandidate {
  const FallCandidate({
    required this.impactSample,
    required this.stationarySampleRatio,
  });

  final ImuSample impactSample;
  final double stationarySampleRatio; //tỉ lệ bất động (0-1)
}

/// 1. Pha rơi tự do (Free Fall): Gia tốc tổng hợp giảm mạnh (< 4.5 m/s² do mất trọng lực khi rơi) trong ít nhất 350ms.
/// 2. Pha va chạm (Impact): Gia tốc tăng vọt đột ngột (> 35.0 m/s² cho sàn cứng hoặc > 22.0 m/s² cho nệm mềm) với Jerk cao.
/// 3. Pha bất động (Immobility): Sau va đập, thiết bị phải nằm yên (ổn định gần 9.81 m/s², con quay < 0.5 rad/s) trong 800ms-1000ms.
class ImuFallDetector {
  /// Ngưỡng gia tốc rơi tự do (m/s²): khi rơi, gia tốc đo được giảm từ ~9.81 xuống dưới 4.5 m/s².
  static const double freeFallThreshold = 4.5;

  /// Ngưỡng xung lực va đập tiêu chuẩn (m/s²): ~3.5G, tương ứng tiếp đất trên bề mặt cứng.
  static const double impactThreshold = 35.0;

  /// Ngưỡng xung lực va đập khi tiếp đất trên bề mặt êm/nệm (m/s²): áp dụng sau cú rơi tự do dài.
  static const double softLandingImpactThreshold = 22.0;

  /// Độ giật tối thiểu (Jerk = Δa / Δt, đơn vị m/s³): đảm bảo sự thay đổi gia tốc xảy ra đột ngột do va đập.
  static const double minimumJerk = 80.0;

  /// Độ giật tối thiểu cho cú ngã êm (m/s³).
  static const double minimumSoftFallJerk = 40.0;

  /// Gia tốc trọng trường chuẩn của Trái Đất (m/s²).
  static const double gravity = 9.81;

  /// Dung sai gia tốc khi nằm yên (|a - 9.81| <= 2.0 m/s²).
  static const double stationaryAccelerationTolerance = 2.0;

  /// Ngưỡng vận tốc góc khi nằm yên (rad/s): không được xoay quá 0.5 rad/s.
  static const double stationaryGyroscopeThreshold = 0.5;

  /// Dung sai gia tốc nằm yên cho cú tiếp đất êm (m/s²).
  static const double softLandingStationaryAccelerationTolerance = 2.5;

  /// Ngưỡng vận tốc góc nằm yên cho cú tiếp đất êm (rad/s).
  static const double softLandingStationaryGyroscopeThreshold = 0.8;

  /// Ngưỡng vận tốc góc hủy bỏ (rad/s): nếu sau va chạm điện thoại xoay mạnh > 1.8 rad/s -> người dùng đang cầm sử dụng.
  static const double cancellationGyroscopeThreshold = 1.8;

  /// Độ lệch gia tốc hủy bỏ (m/s²): nếu gia tốc lệch khỏi 9.81 > 6.0 m/s² -> đang có vận động mạnh.
  static const double strongMovementAccelerationDeviation = 6.0;

  /// Ngưỡng xoay hủy bỏ cho cú ngã êm (rad/s).
  static const double softLandingCancellationGyroscopeThreshold = 2.2;

  /// Độ lệch gia tốc hủy bỏ cho cú ngã êm (m/s²).
  static const double softLandingStrongMovementAccelerationDeviation = 7.5;

  /// Tỷ lệ mẫu bất động tối thiểu bắt buộc sau va chạm (80% số mẫu phải bất động).
  static const double minimumStationaryRatio = 0.8;

  /// Tỷ lệ mẫu bất động tối thiểu cho cú ngã tiếp đất êm (60%).
  static const double minimumSoftLandingStationaryRatio = 0.6;

  /// Thời gian rơi tự do tối thiểu (350ms, tương đương độ cao rơi tự do ~60cm).
  static const Duration minimumFreeFallDuration = Duration(milliseconds: 350);

  /// Thời gian rơi tự do để áp dụng bộ tiêu chuẩn ngã êm (350ms).
  static const Duration softFallQualificationDuration = Duration(
    milliseconds: 350,
  );

  /// Cửa sổ thời gian tối đa từ lúc bắt đầu rơi đến khi va chạm tiếp đất (1400ms).
  static const Duration impactWindow = Duration(milliseconds: 1400);

  /// Thời gian chờ ổn định ngay sau cú chạm đầu tiên để tìm đỉnh va đập cao nhất (250ms).
  static const Duration impactSettlingGrace = Duration(milliseconds: 250);

  /// Thời gian tối đa cho phép thiết bị dao động trước khi phải hoàn toàn nằm yên (2500ms).
  static const Duration maximumSettlingDuration = Duration(milliseconds: 2500);

  /// Khoảng cách thời gian tối đa giữa 2 mẫu cảm biến liên tiếp sau va chạm (500ms).
  static const Duration maximumPostImpactSampleGap = Duration(
    milliseconds: 500,
  );

  /// Thời gian liên tục bắt buộc thiết bị phải nằm yên sau va chạm (1000ms = 1 giây).
  static const Duration immobilityWindow = Duration(milliseconds: 1000);

  /// Thời gian liên tục bắt buộc nằm yên cho cú ngã êm (800ms).
  static const Duration softLandingImmobilityWindow = Duration(
    milliseconds: 800,
  );

  /// Độ trễ tối đa cho phép giữa mẫu gia tốc kế và mẫu con quay hồi chuyển (200ms).
  static const Duration maximumGyroscopeAge = Duration(milliseconds: 200);

  /// Thời gian hạ nhiệt (3 giây) sau khi phát hiện một cú ngã để chống lặp cảnh báo.
  static const Duration cooldown = Duration(seconds: 3);

  FallDetectionPhase _phase = FallDetectionPhase.idle;
  ImuSample? _previousSample;
  DateTime? _freeFallAt;
  DateTime? _impactStartedAt;
  ImuSample? _impactSample;
  var _postImpactSamples = 0;
  var _stationaryPostImpactSamples = 0;
  DateTime? _lastPostImpactSampleAt;
  DateTime? _settledSince;
  DateTime? _cooldownUntil;
  ImuDetectorDecision _latestDecision = const ImuDetectorDecision(
    phase: FallDetectionPhase.idle,
    reason: ImuDetectorDecisionReason.awaitingFreeFall,
  );

  FallDetectionPhase get phase => _phase;
  ImuDetectorDecision get latestDecision => _latestDecision;

  static List<ImuSample> canonicalSimulationSamples(DateTime startedAt) {
    ImuSample sample(
      Duration offset,
      double acceleration, {
      double gyro = 0.1,
    }) {
      final timestamp = startedAt.toUtc().add(offset);
      return ImuSample(
        accelerometerX: acceleration,
        accelerometerY: 0,
        accelerometerZ: 0,
        gyroscopeX: gyro,
        gyroscopeY: 0,
        gyroscopeZ: 0,
        timestamp: timestamp,
        gyroscopeTimestamp: timestamp,
      );
    }

    return <ImuSample>[
      sample(Duration.zero, 2),
      sample(const Duration(milliseconds: 400), 40, gyro: 0.2),
      for (var index = 1; index <= 5; index++)
        sample(Duration(milliseconds: 400 + index * 200), gravity),
    ];
  }

  /// Nạp mẫu cảm biến IMU mới vào máy trạng thái phân tích.
  /// Trả về [FallCandidate] nếu mẫu này hoàn tất việc xác nhận một cú ngã thật.
  FallCandidate? addSample(ImuSample sample) {
    // Kiểm tra thứ tự thời gian của mẫu cảm biến
    final previous = _previousSample;
    if (previous != null && !sample.timestamp.isAfter(previous.timestamp)) {
      _resetCandidate();
      _previousSample = sample;
      _setDecision(ImuDetectorDecisionReason.outOfOrder);
      return null;
    }
    _previousSample = sample;

    // Kiểm tra time cooldown để tránh trùng lặp
    final cooldownUntil = _cooldownUntil;
    if (cooldownUntil != null && sample.timestamp.isBefore(cooldownUntil)) {
      _setDecision(ImuDetectorDecisionReason.cooldown);
      return null;
    }
    if (cooldownUntil != null) _cooldownUntil = null;

    // Phân luồng xử lý theo pha hiện tại của máy trạng thái
    switch (_phase) {
      case FallDetectionPhase.idle:
        // Nếu gia tốc giảm xuống dưới ngưỡng rơi tự do (< 4.5 m/s²) -> Chuyển sang pha freeFall
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

  /// Xử lý pha rơi tự do: kiểm tra thời lượng rơi và điều kiện chuyển tiếp sang pha va chạm (Impact).
  FallCandidate? _handleFreeFall(ImuSample sample, ImuSample? previous) {
    final freeFallAt = _freeFallAt;
    // Nếu vượt quá cửa sổ thời gian va chạm (1.4s) mà chưa tiếp đất -> Hết hạn cửa sổ rơi
    if (freeFallAt == null ||
        sample.timestamp.difference(freeFallAt) > impactWindow) {
      _resetCandidate();
      if (sample.accelerationMagnitude < freeFallThreshold) {
        _phase = FallDetectionPhase.freeFall;
        _freeFallAt = sample.timestamp;
      }
      _setDecision(ImuDetectorDecisionReason.impactWindowExpired);
      return null;
    }

    final freeFallDuration = sample.timestamp.difference(freeFallAt);
    // Rơi quá nhanh (< 350ms): nếu có va đập mạnh ngay thì bỏ qua vì giống cú vung tay/đặt điện thoại mạnh
    if (freeFallDuration < minimumFreeFallDuration) {
      if (sample.accelerationMagnitude > impactThreshold) {
        _resetCandidate();
        _setDecision(ImuDetectorDecisionReason.freeFallTooShort);
      } else {
        _setDecision(ImuDetectorDecisionReason.awaitingImpact);
      }
      return null;
    }

    // Chọn ngưỡng va đập: nếu rơi đủ lâu (>= 350ms) thì hỗ trợ cả cú ngã tiếp đất êm (22.0 m/s²)
    final impactThresholdForSequence =
        freeFallDuration >= softFallQualificationDuration
        ? softLandingImpactThreshold
        : impactThreshold;
    if (sample.accelerationMagnitude <= impactThresholdForSequence ||
        previous == null) {
      _setDecision(ImuDetectorDecisionReason.awaitingImpact);
      return null;
    }

    // Tính độ biến thiên gia tốc (Jerk = Δa / Δt) để loại trừ các chuyển động mượt mà
    final elapsedMicros = sample.timestamp
        .difference(previous.timestamp)
        .inMicroseconds;
    if (elapsedMicros <= 0) {
      _resetCandidate();
      _setDecision(ImuDetectorDecisionReason.outOfOrder);
      return null;
    }
    final elapsedSeconds = elapsedMicros / Duration.microsecondsPerSecond;
    final jerk =
        (sample.accelerationMagnitude - previous.accelerationMagnitude).abs() /
        elapsedSeconds;
    final requiredJerk = freeFallDuration >= softFallQualificationDuration
        ? minimumSoftFallJerk
        : minimumJerk;
    if (jerk < requiredJerk) {
      _setDecision(ImuDetectorDecisionReason.jerkTooLow);
      return null;
    }

    // Đảm bảo dữ liệu con quay hồi chuyển đang hoạt động đồng bộ
    if (!_hasFreshGyroscope(sample)) {
      _resetCandidate();
      _setDecision(_gyroscopeFailureReason(sample));
      return null;
    }

    // Chuyển sang pha va chạm và bắt đầu theo dõi trạng thái nằm yên
    _phase = FallDetectionPhase.impact;
    _impactStartedAt = sample.timestamp;
    _impactSample = sample;
    _postImpactSamples = 0;
    _stationaryPostImpactSamples = 0;
    _lastPostImpactSampleAt = sample.timestamp.add(impactSettlingGrace);
    _settledSince = sample.timestamp;
    _setDecision(ImuDetectorDecisionReason.impactDetected);
    return null;
  }

  /// Xử lý pha sau va đập: theo dõi độ bất động (immobility) để loại bỏ báo động giả khi người dùng vẫn cử động bình thường.
  FallCandidate? _handlePostImpact(ImuSample sample) {
    final impact = _impactSample;
    final impactStartedAt = _impactStartedAt;
    if (impact == null || impactStartedAt == null) {
      _resetCandidate();
      _setDecision(ImuDetectorDecisionReason.reset);
      return null;
    }

    final sinceFirstImpact = sample.timestamp.difference(impactStartedAt);
    // Trong 250ms đầu sau va chạm: ghi nhận đỉnh va đập cao nhất
    if (sinceFirstImpact <= impactSettlingGrace) {
      if (sample.accelerationMagnitude > impact.accelerationMagnitude + 1 &&
          sample.accelerationMagnitude > impactThreshold &&
          _hasFreshGyroscope(sample)) {
        _impactSample = sample;
      }
      _setDecision(ImuDetectorDecisionReason.impactSettling);
      return null;
    }

    // Kiểm tra độ tươi của dữ liệu con quay hồi chuyển
    if (!_hasFreshGyroscope(sample)) {
      _resetCandidate();
      _setDecision(_gyroscopeFailureReason(sample));
      return null;
    }

    // Kiểm tra nếu mất mẫu cảm biến quá 500ms
    final lastPostImpactSampleAt = _lastPostImpactSampleAt;
    if (lastPostImpactSampleAt == null ||
        sample.timestamp.difference(lastPostImpactSampleAt) >
            maximumPostImpactSampleGap) {
      _resetCandidate();
      _setDecision(ImuDetectorDecisionReason.sampleGap);
      return null;
    }
    _lastPostImpactSampleAt = sample.timestamp;

    final accelerationDeviation = (sample.accelerationMagnitude - gravity)
        .abs();
    final freeFallAt = _freeFallAt;
    final freeFallDuration = freeFallAt == null
        ? Duration.zero
        : impactStartedAt.difference(freeFallAt);
    final isLongSoftFall = freeFallDuration >= softFallQualificationDuration;
    final movementGyroscopeThreshold = isLongSoftFall
        ? softLandingCancellationGyroscopeThreshold
        : cancellationGyroscopeThreshold;
    final movementAccelerationDeviation = isLongSoftFall
        ? softLandingStrongMovementAccelerationDeviation
        : strongMovementAccelerationDeviation;
    _postImpactSamples++;

    // Nếu thiết bị vận động mạnh (xoay hoặc gia tốc lệch lớn) -> Người dùng đang cầm máy cử động
    if (sample.gyroscopeMagnitude > movementGyroscopeThreshold ||
        accelerationDeviation > movementAccelerationDeviation) {
      if (sinceFirstImpact > maximumSettlingDuration) {
        _resetCandidate();
        _setDecision(ImuDetectorDecisionReason.excessiveMovement);
        return null;
      }
      _settledSince = null;
      _setDecision(ImuDetectorDecisionReason.awaitingImmobility);
      return null;
    }
    final settledSince = _settledSince ??= sample.timestamp;

    // Đếm số mẫu đạt tiêu chuẩn bất động (|a - 9.81| <= tol && gyro <= limit)
    final stationaryAccelerationLimit = isLongSoftFall
        ? softLandingStationaryAccelerationTolerance
        : stationaryAccelerationTolerance;
    final stationaryGyroscopeLimit = isLongSoftFall
        ? softLandingStationaryGyroscopeThreshold
        : stationaryGyroscopeThreshold;
    if (accelerationDeviation <= stationaryAccelerationLimit &&
        sample.gyroscopeMagnitude <= stationaryGyroscopeLimit) {
      _stationaryPostImpactSamples++;
    }

    // Yêu cầu thiết bị phải nằm yên liên tục đủ cửa sổ thời gian quy định (800ms - 1000ms)
    final requiredImmobilityWindow = isLongSoftFall
        ? softLandingImmobilityWindow
        : immobilityWindow;
    if (sample.timestamp.difference(settledSince) < requiredImmobilityWindow) {
      _setDecision(ImuDetectorDecisionReason.awaitingImmobility);
      return null;
    }

    // Kiểm tra tỷ lệ mẫu bất động tối thiểu (>= 60% cho ngã êm, >= 80% cho ngã thường)
    final ratio = _postImpactSamples == 0
        ? 0.0
        : _stationaryPostImpactSamples / _postImpactSamples;
    _resetCandidate();
    final requiredStationaryRatio = isLongSoftFall
        ? minimumSoftLandingStationaryRatio
        : minimumStationaryRatio;
    if (ratio < requiredStationaryRatio) {
      _setDecision(ImuDetectorDecisionReason.insufficientStationarySamples);
      return null;
    }

    // Đã thỏa mãn tất cả tiêu chí -> Kích hoạt thời gian hạ nhiệt cooldown và phát ra ứng viên ngã
    _cooldownUntil = sample.timestamp.add(cooldown);
    _setDecision(ImuDetectorDecisionReason.accepted);
    return FallCandidate(impactSample: impact, stationarySampleRatio: ratio);
  }

  ImuDetectorDecisionReason _gyroscopeFailureReason(ImuSample sample) =>
      sample.gyroscopeTimestamp == null
      ? ImuDetectorDecisionReason.gyroscopeMissing
      : ImuDetectorDecisionReason.gyroscopeStale;

  void _setDecision(ImuDetectorDecisionReason reason) {
    _latestDecision = ImuDetectorDecision(phase: _phase, reason: reason);
  }

  bool _hasFreshGyroscope(ImuSample sample) {
    final gyroscopeTimestamp = sample.gyroscopeTimestamp;
    if (gyroscopeTimestamp == null) return false;
    final age = sample.timestamp.difference(gyroscopeTimestamp).abs();
    return age <= maximumGyroscopeAge;
  }

  /// Đặt lại toàn bộ bộ phân tích về trạng thái ban đầu.
  void reset() {
    _resetCandidate();
    _previousSample = null;
    _cooldownUntil = null;
    _setDecision(ImuDetectorDecisionReason.reset);
  }

  /// Tái kích hoạt bộ phân tích sau khi người dùng phản hồi cảnh báo (bật cooldown để chống duplicate).
  void rearmAfterAlertResponse(DateTime respondedAt) {
    _resetCandidate();
    _previousSample = null;
    final requestedCooldownUntil = respondedAt.toUtc().add(cooldown);
    final currentCooldownUntil = _cooldownUntil;
    if (currentCooldownUntil == null ||
        requestedCooldownUntil.isAfter(currentCooldownUntil)) {
      _cooldownUntil = requestedCooldownUntil;
    }
    _setDecision(ImuDetectorDecisionReason.cooldown);
  }

  /// Đặt lại các biến trạng thái tạm thời của ứng viên cú ngã về pha idle.
  void _resetCandidate() {
    _phase = FallDetectionPhase.idle;
    _freeFallAt = null;
    _impactStartedAt = null;
    _impactSample = null;
    _postImpactSamples = 0;
    _stationaryPostImpactSamples = 0;
    _lastPostImpactSampleAt = null;
    _settledSince = null;
  }
}
