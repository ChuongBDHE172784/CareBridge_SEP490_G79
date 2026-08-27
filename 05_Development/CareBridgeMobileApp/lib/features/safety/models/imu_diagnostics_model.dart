import '../services/imu_fall_detector.dart';

enum ImuSamplingState {
  coordinatorRunning,
  awaitingSamples,
  sampling,
  stale,
  stopped,
  error,
}

extension ImuSamplingStateDisplay on ImuSamplingState {
  String get label => switch (this) {
    ImuSamplingState.coordinatorRunning => 'Dịch vụ đang khởi động',
    ImuSamplingState.awaitingSamples => 'Đang chờ dữ liệu cảm biến',
    ImuSamplingState.sampling => 'Đang nhận dữ liệu cảm biến',
    ImuSamplingState.stale => 'Dữ liệu cảm biến đã cũ',
    ImuSamplingState.stopped => 'Cảm biến đã dừng',
    ImuSamplingState.error => 'Lỗi luồng cảm biến',
  };
}

extension ImuDetectorDecisionReasonDisplay on ImuDetectorDecisionReason {
  String get label => switch (this) {
    ImuDetectorDecisionReason.awaitingFreeFall => 'Đang chờ pha rơi tự do',
    ImuDetectorDecisionReason.freeFallDetected => 'Đã nhận pha rơi tự do',
    ImuDetectorDecisionReason.freeFallTooShort =>
      'Hủy: pha rơi quá ngắn để là một cú ngã',
    ImuDetectorDecisionReason.awaitingImpact => 'Đang chờ va chạm đủ ngưỡng',
    ImuDetectorDecisionReason.impactWindowExpired =>
      'Hủy: quá thời gian chờ va chạm',
    ImuDetectorDecisionReason.jerkTooLow => 'Hủy: độ giật chưa đủ ngưỡng',
    ImuDetectorDecisionReason.gyroscopeMissing =>
      'Hủy: chưa nhận dữ liệu con quay',
    ImuDetectorDecisionReason.gyroscopeStale => 'Hủy: dữ liệu con quay đã cũ',
    ImuDetectorDecisionReason.impactDetected => 'Đã nhận pha va chạm',
    ImuDetectorDecisionReason.impactSettling => 'Đang chờ ổn định sau va chạm',
    ImuDetectorDecisionReason.sampleGap => 'Hủy: luồng mẫu bị gián đoạn',
    ImuDetectorDecisionReason.excessiveMovement =>
      'Hủy: còn chuyển động mạnh sau va chạm',
    ImuDetectorDecisionReason.awaitingImmobility =>
      'Đang xác minh trạng thái bất động',
    ImuDetectorDecisionReason.insufficientStationarySamples =>
      'Hủy: tỷ lệ mẫu bất động chưa đủ',
    ImuDetectorDecisionReason.accepted => 'Đã nhận diện dấu hiệu nghi ngờ ngã',
    ImuDetectorDecisionReason.cooldown => 'Đang trong thời gian chống lặp',
    ImuDetectorDecisionReason.outOfOrder => 'Hủy: thứ tự mẫu không hợp lệ',
    ImuDetectorDecisionReason.reset => 'Bộ phát hiện đã đặt lại',
  };
}

class ImuDiagnosticsSnapshot {
  const ImuDiagnosticsSnapshot({
    required this.generation,
    required this.state,
    required this.capturedAt,
    this.sampleRateHz,
    this.accelerationMagnitude,
    this.gyroscopeMagnitude,
    this.detectorPhase = FallDetectionPhase.idle,
    this.detectorReason = ImuDetectorDecisionReason.awaitingFreeFall,
    this.errorMessage,
    this.demoGestureSequence = 0,
  });

  factory ImuDiagnosticsSnapshot.awaiting({
    required int generation,
    required DateTime capturedAt,
  }) => ImuDiagnosticsSnapshot(
    generation: generation,
    state: ImuSamplingState.awaitingSamples,
    capturedAt: capturedAt,
  );

  factory ImuDiagnosticsSnapshot.stopped({
    required int generation,
    required DateTime capturedAt,
  }) => ImuDiagnosticsSnapshot(
    generation: generation,
    state: ImuSamplingState.stopped,
    capturedAt: capturedAt,
    detectorReason: ImuDetectorDecisionReason.reset,
  );

  factory ImuDiagnosticsSnapshot.fromJson(Map<String, dynamic> json) {
    final generation = json['generation'];
    final capturedAt = json['capturedAt'];
    final state = json['state'];
    final demoGestureSequence = json['demoGestureSequence'] ?? 0;
    if (generation is! int ||
        capturedAt is! String ||
        state is! String ||
        demoGestureSequence is! int ||
        demoGestureSequence < 0) {
      throw const FormatException('Invalid IMU diagnostics payload');
    }
    return ImuDiagnosticsSnapshot(
      generation: generation,
      state: ImuSamplingState.values.byName(state),
      capturedAt: DateTime.parse(capturedAt).toUtc(),
      sampleRateHz: _finiteDouble(json['sampleRateHz']),
      accelerationMagnitude: _finiteDouble(json['accelerationMagnitude']),
      gyroscopeMagnitude: _finiteDouble(json['gyroscopeMagnitude']),
      detectorPhase: FallDetectionPhase.values.byName(
        json['detectorPhase'] as String? ?? FallDetectionPhase.idle.name,
      ),
      detectorReason: ImuDetectorDecisionReason.values.byName(
        json['detectorReason'] as String? ??
            ImuDetectorDecisionReason.awaitingFreeFall.name,
      ),
      errorMessage: json['errorMessage'] as String?,
      demoGestureSequence: demoGestureSequence,
    );
  }

  static ImuDiagnosticsSnapshot? tryParse(Object? value) {
    if (value is! Map) return null;
    try {
      return ImuDiagnosticsSnapshot.fromJson(Map<String, dynamic>.from(value));
    } catch (_) {
      return null;
    }
  }

  static double? _finiteDouble(Object? value) {
    if (value == null) return null;
    if (value is! num) throw const FormatException('Invalid numeric value');
    final result = value.toDouble();
    if (!result.isFinite) throw const FormatException('Non-finite value');
    return result;
  }

  static double? _jsonSafeDouble(double? value) =>
      value != null && value.isFinite ? value : null;

  final int generation;
  final ImuSamplingState state;
  final DateTime capturedAt;
  final double? sampleRateHz;
  final double? accelerationMagnitude;
  final double? gyroscopeMagnitude;
  final FallDetectionPhase detectorPhase;
  final ImuDetectorDecisionReason detectorReason;
  final String? errorMessage;
  final int demoGestureSequence;

  Duration ageAt(DateTime now) {
    final age = now.toUtc().difference(capturedAt);
    return age.isNegative ? Duration.zero : age;
  }

  Map<String, dynamic> toJson() => {
    'generation': generation,
    'state': state.name,
    'capturedAt': capturedAt.toUtc().toIso8601String(),
    'sampleRateHz': _jsonSafeDouble(sampleRateHz),
    'accelerationMagnitude': _jsonSafeDouble(accelerationMagnitude),
    'gyroscopeMagnitude': _jsonSafeDouble(gyroscopeMagnitude),
    'detectorPhase': detectorPhase.name,
    'detectorReason': detectorReason.name,
    'errorMessage': errorMessage,
    'demoGestureSequence': demoGestureSequence,
  };

  @override
  bool operator ==(Object other) =>
      other is ImuDiagnosticsSnapshot &&
      generation == other.generation &&
      state == other.state &&
      capturedAt == other.capturedAt &&
      sampleRateHz == other.sampleRateHz &&
      accelerationMagnitude == other.accelerationMagnitude &&
      gyroscopeMagnitude == other.gyroscopeMagnitude &&
      detectorPhase == other.detectorPhase &&
      detectorReason == other.detectorReason &&
      errorMessage == other.errorMessage &&
      demoGestureSequence == other.demoGestureSequence;

  @override
  int get hashCode => Object.hash(
    generation,
    state,
    capturedAt,
    sampleRateHz,
    accelerationMagnitude,
    gyroscopeMagnitude,
    detectorPhase,
    detectorReason,
    errorMessage,
    demoGestureSequence,
  );
}
