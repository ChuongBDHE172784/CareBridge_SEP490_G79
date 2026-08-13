import 'dart:math';

enum FallDetectionPhase { idle, freeFall, impact }

enum ImuDetectorDecisionReason {
  awaitingFreeFall,
  freeFallDetected,
  freeFallTooShort,
  awaitingImpact,
  impactWindowExpired,
  jerkTooLow,
  gyroscopeMissing,
  gyroscopeStale,
  impactDetected,
  impactSettling,
  sampleGap,
  excessiveMovement,
  awaitingImmobility,
  insufficientStationarySamples,
  accepted,
  cooldown,
  outOfOrder,
  reset,
}

class ImuDetectorDecision {
  const ImuDetectorDecision({required this.phase, required this.reason});

  final FallDetectionPhase phase;
  final ImuDetectorDecisionReason reason;
}

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
  // The detector must recognise a controlled 50 cm fall onto a pillow or
  // mattress. Such a landing can have only a small rebound above gravity,
  // unlike a hard-floor impact, so the three-phase sequence remains required
  // while its free-fall, rebound and jerk limits are deliberately softer.
  // iOS motion filtering can keep a genuine 50 cm low-g segment slightly
  // above the ideal free-fall value. The 60 ms duration gate below still
  // rejects the short low-g pulse produced by a 1 cm desk tap.
  static const double freeFallThreshold = 7.2;
  static const double impactThreshold = 9.5;
  static const double softLandingImpactThreshold = 8.5;
  static const double minimumJerk = 40.0;
  // A soft landing can spread the acceleration change over several samples.
  // Once the measured low-g phase is long enough to reject a 1 cm tap, use
  // this softer jerk floor because iOS filtering may shorten the visible
  // free-fall segment even when the physical fall is about 50 cm.
  static const double minimumSoftFallJerk = 20.0;
  static const double gravity = 9.81;
  static const double stationaryAccelerationTolerance = 2.0;
  static const double stationaryGyroscopeThreshold = 0.5;
  static const double softLandingStationaryAccelerationTolerance = 2.8;
  static const double softLandingStationaryGyroscopeThreshold = 0.9;
  static const double cancellationGyroscopeThreshold = 1.5;
  static const double strongMovementAccelerationDeviation = 6.0;
  static const double softLandingCancellationGyroscopeThreshold = 2.5;
  static const double softLandingStrongMovementAccelerationDeviation = 8.0;
  static const double minimumStationaryRatio = 0.8;
  static const double minimumSoftLandingStationaryRatio = 0.6;
  // A 1 cm lift produces only about 45 ms of free-fall. Three 50 Hz sample
  // intervals filter that tap while tolerating iOS sensor filtering on a
  // controlled 50 cm fall.
  static const Duration minimumFreeFallDuration = Duration(milliseconds: 60);
  static const Duration softFallQualificationDuration = Duration(
    milliseconds: 60,
  );
  static const Duration impactWindow = Duration(milliseconds: 1500);
  static const Duration impactSettlingGrace = Duration(milliseconds: 250);
  // A phone dropped from about 50 cm rebounds off a floor, mattress or pillow
  // instead of stopping dead. During a rebound it is briefly airborne again,
  // so a post-impact sample can read close to free fall or show another spike
  // even though nobody is moving the device. Such a transient restarts the
  // immobility clock instead of discarding the candidate; only movement that
  // outlasts a physical bounce means the device is being carried or handled.
  static const Duration maximumSettlingDuration = Duration(milliseconds: 2500);
  static const Duration maximumPostImpactSampleGap = Duration(
    milliseconds: 500,
  );
  static const Duration immobilityWindow = Duration(seconds: 1);
  static const Duration softLandingImmobilityWindow = Duration(
    milliseconds: 600,
  );
  static const Duration maximumGyroscopeAge = Duration(milliseconds: 200);
  // The three-phase detector already rejects a continuing impact pulse. Keep
  // only a brief debounce so a user who has dismissed an alert can perform
  // another controlled sensor check after the alert has settled.
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
      sample(const Duration(milliseconds: 100), 30, gyro: 0.2),
      for (var index = 1; index <= 5; index++)
        sample(Duration(milliseconds: 100 + index * 200), gravity),
    ];
  }

  FallCandidate? addSample(ImuSample sample) {
    final previous = _previousSample;
    if (previous != null && !sample.timestamp.isAfter(previous.timestamp)) {
      _resetCandidate();
      _previousSample = sample;
      _setDecision(ImuDetectorDecisionReason.outOfOrder);
      return null;
    }
    _previousSample = sample;

    final cooldownUntil = _cooldownUntil;
    if (cooldownUntil != null && sample.timestamp.isBefore(cooldownUntil)) {
      _setDecision(ImuDetectorDecisionReason.cooldown);
      return null;
    }
    if (cooldownUntil != null) _cooldownUntil = null;

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

  FallCandidate? _handleFreeFall(ImuSample sample, ImuSample? previous) {
    final freeFallAt = _freeFallAt;
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
    if (freeFallDuration < minimumFreeFallDuration) {
      if (sample.accelerationMagnitude > impactThreshold) {
        _resetCandidate();
        _setDecision(ImuDetectorDecisionReason.freeFallTooShort);
      } else {
        _setDecision(ImuDetectorDecisionReason.awaitingImpact);
      }
      return null;
    }

    final impactThresholdForSequence =
        freeFallDuration >= softFallQualificationDuration
        ? softLandingImpactThreshold
        : impactThreshold;
    if (sample.accelerationMagnitude <= impactThresholdForSequence ||
        previous == null) {
      _setDecision(ImuDetectorDecisionReason.awaitingImpact);
      return null;
    }

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
    if (!_hasFreshGyroscope(sample)) {
      _resetCandidate();
      _setDecision(_gyroscopeFailureReason(sample));
      return null;
    }

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

  FallCandidate? _handlePostImpact(ImuSample sample) {
    final impact = _impactSample;
    final impactStartedAt = _impactStartedAt;
    if (impact == null || impactStartedAt == null) {
      _resetCandidate();
      _setDecision(ImuDetectorDecisionReason.reset);
      return null;
    }

    final sinceFirstImpact = sample.timestamp.difference(impactStartedAt);
    if (sinceFirstImpact <= impactSettlingGrace) {
      if (sample.accelerationMagnitude > impact.accelerationMagnitude + 1 &&
          sample.accelerationMagnitude > impactThreshold &&
          _hasFreshGyroscope(sample)) {
        _impactSample = sample;
      }
      _setDecision(ImuDetectorDecisionReason.impactSettling);
      return null;
    }

    if (!_hasFreshGyroscope(sample)) {
      _resetCandidate();
      _setDecision(_gyroscopeFailureReason(sample));
      return null;
    }
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
    // Rebound transients still count towards the stationary ratio below, so
    // tolerating them does not weaken the immobility gate: only a device that
    // truly comes to rest can reach the required ratio.
    _postImpactSamples++;
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

    final requiredImmobilityWindow = isLongSoftFall
        ? softLandingImmobilityWindow
        : immobilityWindow;
    if (sample.timestamp.difference(settledSince) < requiredImmobilityWindow) {
      _setDecision(ImuDetectorDecisionReason.awaitingImmobility);
      return null;
    }

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

  void reset() {
    _resetCandidate();
    _previousSample = null;
    _cooldownUntil = null;
    _setDecision(ImuDetectorDecisionReason.reset);
  }

  void rearmAfterAlertResponse(DateTime respondedAt) {
    _resetCandidate();
    _previousSample = null;
    // Answering an alert means the phone has just been picked up and tapped.
    // Hold the short debounce so that handling does not immediately raise a
    // second alert for the incident the user has already closed.
    final requestedCooldownUntil = respondedAt.toUtc().add(cooldown);
    final currentCooldownUntil = _cooldownUntil;
    if (currentCooldownUntil == null ||
        requestedCooldownUntil.isAfter(currentCooldownUntil)) {
      _cooldownUntil = requestedCooldownUntil;
    }
    _setDecision(ImuDetectorDecisionReason.cooldown);
  }

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
