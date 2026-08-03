import 'package:flutter/foundation.dart';

import 'imu_fall_detector.dart';

/// Opt-in presentation tooling for signed committee/demo builds.
///
/// Production release builds remain unchanged unless the compile-time flag is
/// explicitly provided with `--dart-define=ENABLE_SAFETY_DEMO=true`.
const bool safetyDemoMode = bool.fromEnvironment(
  'ENABLE_SAFETY_DEMO',
  defaultValue: false,
);

const bool safetyDiagnosticsEnabled = kDebugMode || safetyDemoMode;

const String safetyDiagnosticsModeLabel = safetyDemoMode
    ? 'CHẾ ĐỘ TRÌNH DIỄN'
    : 'DEBUG';

/// Recognizes an intentionally exaggerated hand swing while demo mode is
/// armed. These are presentation-gesture thresholds, not fall thresholds.
class SafetyDemoGestureDetector {
  static const double lowAccelerationMagnitude = 7.5;
  static const double highAccelerationMagnitude = 12.1;
  static const double rotationMagnitude = 0.8;
  static const Duration cooldown = Duration(milliseconds: 300);

  DateTime? _lastDetectedAt;
  int _sequence = 0;

  int get sequence => _sequence;

  bool addSample(ImuSample sample) {
    final lastDetectedAt = _lastDetectedAt;
    if (lastDetectedAt != null &&
        sample.timestamp.difference(lastDetectedAt) < cooldown) {
      return false;
    }
    final deliberateMotion =
        sample.accelerationMagnitude <= lowAccelerationMagnitude ||
        sample.accelerationMagnitude >= highAccelerationMagnitude ||
        (sample.gyroscopeTimestamp != null &&
            sample.gyroscopeMagnitude >= rotationMagnitude);
    if (!deliberateMotion) return false;

    _lastDetectedAt = sample.timestamp;
    _sequence++;
    return true;
  }

  void reset() {
    _lastDetectedAt = null;
    _sequence = 0;
  }
}
