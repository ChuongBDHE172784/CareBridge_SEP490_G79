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
  static const double gravity = 9.81;
  static const double stationaryAccelerationTolerance = 0.8;
  static const double stationaryRotationMagnitude = 0.25;
  static const double motionStartAccelerationDeviation = 3.0;
  static const double motionStartRotationMagnitude = 1.4;
  static const double strongAccelerationDeviation = 2.5;
  static const double strongRotationMagnitude = 1.2;
  static const double requiredPeakAccelerationDeviation = 5.0;
  static const double requiredPeakRotationMagnitude = 2.0;
  static const double minimumEstimatedTravelMetres = 0.45;
  static const int minimumStrongSamples = 8;
  static const Duration stationaryPreparation = Duration(milliseconds: 300);
  static const Duration preparationValidity = Duration(seconds: 3);
  static const Duration minimumMotionDuration = Duration(milliseconds: 250);
  static const Duration maximumMotionDuration = Duration(milliseconds: 900);
  static const Duration maximumSampleGap = Duration(milliseconds: 120);
  static const Duration cooldown = Duration(seconds: 2);

  DateTime? _lastDetectedAt;
  DateTime? _lastSampleAt;
  DateTime? _stationarySince;
  DateTime? _preparedAt;
  DateTime? _motionStartedAt;
  double _estimatedSpeed = 0;
  double _estimatedTravel = 0;
  double _peakAccelerationDeviation = 0;
  double _peakRotationMagnitude = 0;
  int _strongSamples = 0;
  int _sequence = 0;

  int get sequence => _sequence;

  bool addSample(ImuSample sample) {
    final lastDetectedAt = _lastDetectedAt;
    if (lastDetectedAt != null &&
        sample.timestamp.difference(lastDetectedAt) < cooldown) {
      _lastSampleAt = sample.timestamp;
      return false;
    }

    final previousAt = _lastSampleAt;
    _lastSampleAt = sample.timestamp;
    if (previousAt != null) {
      final gap = sample.timestamp.difference(previousAt);
      if (gap <= Duration.zero || gap > maximumSampleGap) {
        _resetMotionPreparation();
        return false;
      }
    }

    final accelerationDeviation = (sample.accelerationMagnitude - gravity)
        .abs();
    final gyroscopeFresh =
        sample.gyroscopeTimestamp != null &&
        sample.timestamp.difference(sample.gyroscopeTimestamp!).abs() <=
            maximumSampleGap;
    final rotationMagnitude = gyroscopeFresh ? sample.gyroscopeMagnitude : 0.0;
    final stationary =
        accelerationDeviation <= stationaryAccelerationTolerance &&
        rotationMagnitude <= stationaryRotationMagnitude;

    final motionStartedAt = _motionStartedAt;
    if (motionStartedAt == null) {
      _updatePreparation(sample.timestamp, stationary);
      final preparedAt = _preparedAt;
      final prepared =
          preparedAt != null &&
          sample.timestamp.difference(preparedAt) <= preparationValidity;
      final startsDeliberateMotion =
          accelerationDeviation >= motionStartAccelerationDeviation &&
          rotationMagnitude >= motionStartRotationMagnitude;
      if (!prepared || !startsDeliberateMotion) return false;

      _motionStartedAt = sample.timestamp;
      _estimatedSpeed = 0;
      _estimatedTravel = 0;
      _peakAccelerationDeviation = accelerationDeviation;
      _peakRotationMagnitude = rotationMagnitude;
      _strongSamples = 1;
      return false;
    }

    final elapsed = sample.timestamp.difference(motionStartedAt);
    if (elapsed > maximumMotionDuration) {
      _resetMotionPreparation();
      _updatePreparation(sample.timestamp, stationary);
      return false;
    }

    final seconds =
        sample.timestamp.difference(previousAt!).inMicroseconds /
        Duration.microsecondsPerSecond;
    // Magnitude-only IMU data cannot recover an exact world-space trajectory.
    // Integrating gravity-compensated movement gives a conservative proxy that
    // rejects a short jerk while accepting a deliberate ~50 cm hand swing.
    final effectiveAcceleration =
        (accelerationDeviation - stationaryAccelerationTolerance).clamp(
          0.0,
          double.infinity,
        );
    _estimatedTravel +=
        _estimatedSpeed * seconds +
        0.5 * effectiveAcceleration * seconds * seconds;
    _estimatedSpeed += effectiveAcceleration * seconds;
    if (accelerationDeviation > _peakAccelerationDeviation) {
      _peakAccelerationDeviation = accelerationDeviation;
    }
    if (rotationMagnitude > _peakRotationMagnitude) {
      _peakRotationMagnitude = rotationMagnitude;
    }
    if (accelerationDeviation >= strongAccelerationDeviation &&
        rotationMagnitude >= strongRotationMagnitude) {
      _strongSamples++;
    }

    final deliberateLongSwing =
        elapsed >= minimumMotionDuration &&
        _estimatedTravel >= minimumEstimatedTravelMetres &&
        _strongSamples >= minimumStrongSamples &&
        _peakAccelerationDeviation >= requiredPeakAccelerationDeviation &&
        _peakRotationMagnitude >= requiredPeakRotationMagnitude;
    if (!deliberateLongSwing) return false;

    _lastDetectedAt = sample.timestamp;
    _sequence++;
    _resetMotionPreparation();
    return true;
  }

  void _updatePreparation(DateTime timestamp, bool stationary) {
    if (stationary) {
      _stationarySince ??= timestamp;
      if (timestamp.difference(_stationarySince!) >= stationaryPreparation) {
        _preparedAt = timestamp;
      }
      return;
    }
    _stationarySince = null;
    final preparedAt = _preparedAt;
    if (preparedAt != null &&
        timestamp.difference(preparedAt) > preparationValidity) {
      _preparedAt = null;
    }
  }

  void _resetMotionPreparation() {
    _stationarySince = null;
    _preparedAt = null;
    _motionStartedAt = null;
    _estimatedSpeed = 0;
    _estimatedTravel = 0;
    _peakAccelerationDeviation = 0;
    _peakRotationMagnitude = 0;
    _strongSamples = 0;
  }

  void reset() {
    _lastDetectedAt = null;
    _lastSampleAt = null;
    _resetMotionPreparation();
    _sequence = 0;
  }
}
