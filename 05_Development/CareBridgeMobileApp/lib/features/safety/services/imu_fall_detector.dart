import 'dart:math';

enum FallDetectionPhase { idle, freeFall, impact }

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

class FallCandidate {
  const FallCandidate({
    required this.impactSample,
    required this.stationarySampleRatio,
  });

  final ImuSample impactSample;
  final double stationarySampleRatio;
}

/// Deterministic, on-device three-phase suspected-fall detector.
///
/// This is a safety heuristic, not a medical diagnosis. It deliberately fails
/// closed when samples are missing, out of order, or contain stale gyroscope
/// readings.
class ImuFallDetector {
  static const double freeFallThreshold = 3.0;
  static const double impactThreshold = 25.0;
  static const double minimumJerk = 80.0;
  static const double gravity = 9.81;
  static const double stationaryAccelerationTolerance = 2.0;
  static const double stationaryGyroscopeThreshold = 0.5;
  static const double cancellationGyroscopeThreshold = 1.5;
  static const double strongMovementAccelerationDeviation = 6.0;
  static const double minimumStationaryRatio = 0.8;
  static const Duration impactWindow = Duration(milliseconds: 1500);
  static const Duration impactSettlingGrace = Duration(milliseconds: 250);
  static const Duration maximumPostImpactSampleGap = Duration(
    milliseconds: 500,
  );
  static const Duration immobilityWindow = Duration(seconds: 4);
  static const Duration maximumGyroscopeAge = Duration(milliseconds: 200);
  static const Duration cooldown = Duration(seconds: 30);

  FallDetectionPhase _phase = FallDetectionPhase.idle;
  ImuSample? _previousSample;
  DateTime? _freeFallAt;
  DateTime? _impactStartedAt;
  ImuSample? _impactSample;
  var _postImpactSamples = 0;
  var _stationaryPostImpactSamples = 0;
  DateTime? _lastPostImpactSampleAt;
  DateTime? _cooldownUntil;

  FallDetectionPhase get phase => _phase;

  FallCandidate? addSample(ImuSample sample) {
    final previous = _previousSample;
    if (previous != null && !sample.timestamp.isAfter(previous.timestamp)) {
      _resetCandidate();
      _previousSample = sample;
      return null;
    }
    _previousSample = sample;

    final cooldownUntil = _cooldownUntil;
    if (cooldownUntil != null && sample.timestamp.isBefore(cooldownUntil)) {
      return null;
    }
    if (cooldownUntil != null) _cooldownUntil = null;

    switch (_phase) {
      case FallDetectionPhase.idle:
        if (sample.accelerationMagnitude < freeFallThreshold) {
          _phase = FallDetectionPhase.freeFall;
          _freeFallAt = sample.timestamp;
        }
        return null;
      case FallDetectionPhase.freeFall:
        return _handleFreeFall(sample, previous);
      case FallDetectionPhase.impact:
        return _handlePostImpact(sample);
    }
  }

  FallCandidate? _handleFreeFall(ImuSample sample, ImuSample? previous) {
    final freeFallAt = _freeFallAt;
    if (freeFallAt == null ||
        sample.timestamp.difference(freeFallAt) > impactWindow) {
      _resetCandidate();
      if (sample.accelerationMagnitude < freeFallThreshold) {
        _phase = FallDetectionPhase.freeFall;
        _freeFallAt = sample.timestamp;
      }
      return null;
    }

    if (sample.accelerationMagnitude <= impactThreshold || previous == null) {
      return null;
    }

    final elapsedMicros = sample.timestamp
        .difference(previous.timestamp)
        .inMicroseconds;
    if (elapsedMicros <= 0) {
      _resetCandidate();
      return null;
    }
    final elapsedSeconds = elapsedMicros / Duration.microsecondsPerSecond;
    final jerk =
        (sample.accelerationMagnitude - previous.accelerationMagnitude).abs() /
        elapsedSeconds;
    if (jerk < minimumJerk) return null;
    if (!_hasFreshGyroscope(sample)) {
      _resetCandidate();
      return null;
    }

    _phase = FallDetectionPhase.impact;
    _impactStartedAt = sample.timestamp;
    _impactSample = sample;
    _postImpactSamples = 0;
    _stationaryPostImpactSamples = 0;
    _lastPostImpactSampleAt = sample.timestamp.add(impactSettlingGrace);
    return null;
  }

  FallCandidate? _handlePostImpact(ImuSample sample) {
    final impact = _impactSample;
    final impactStartedAt = _impactStartedAt;
    if (impact == null || impactStartedAt == null) {
      _resetCandidate();
      return null;
    }

    final sinceFirstImpact = sample.timestamp.difference(impactStartedAt);
    if (sinceFirstImpact <= impactSettlingGrace) {
      if (sample.accelerationMagnitude > impact.accelerationMagnitude &&
          sample.accelerationMagnitude > impactThreshold &&
          _hasFreshGyroscope(sample)) {
        _impactSample = sample;
      }
      return null;
    }

    if (!_hasFreshGyroscope(sample)) {
      _resetCandidate();
      return null;
    }
    final lastPostImpactSampleAt = _lastPostImpactSampleAt;
    if (lastPostImpactSampleAt == null ||
        sample.timestamp.difference(lastPostImpactSampleAt) >
            maximumPostImpactSampleGap) {
      _resetCandidate();
      return null;
    }
    _lastPostImpactSampleAt = sample.timestamp;

    final accelerationDeviation = (sample.accelerationMagnitude - gravity)
        .abs();
    if (sample.gyroscopeMagnitude > cancellationGyroscopeThreshold ||
        accelerationDeviation > strongMovementAccelerationDeviation) {
      _resetCandidate();
      return null;
    }

    _postImpactSamples++;
    if (accelerationDeviation <= stationaryAccelerationTolerance &&
        sample.gyroscopeMagnitude <= stationaryGyroscopeThreshold) {
      _stationaryPostImpactSamples++;
    }

    if (sinceFirstImpact < immobilityWindow) {
      return null;
    }

    final ratio = _postImpactSamples == 0
        ? 0.0
        : _stationaryPostImpactSamples / _postImpactSamples;
    _resetCandidate();
    if (ratio < minimumStationaryRatio) return null;

    _cooldownUntil = sample.timestamp.add(cooldown);
    return FallCandidate(impactSample: impact, stationarySampleRatio: ratio);
  }

  bool _hasFreshGyroscope(ImuSample sample) {
    final gyroscopeTimestamp = sample.gyroscopeTimestamp;
    if (gyroscopeTimestamp == null) return false;
    final age = sample.timestamp.difference(gyroscopeTimestamp).abs();
    return age <= maximumGyroscopeAge;
  }

  void reset() {
    _resetCandidate();
    _previousSample = null;
    _cooldownUntil = null;
  }

  void _resetCandidate() {
    _phase = FallDetectionPhase.idle;
    _freeFallAt = null;
    _impactStartedAt = null;
    _impactSample = null;
    _postImpactSamples = 0;
    _stationaryPostImpactSamples = 0;
    _lastPostImpactSampleAt = null;
  }
}
