import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/safety/services/imu_fall_detector.dart';

void main() {
  const detectorStart = Duration(seconds: 100);

  ImuSample sample({
    required Duration at,
    required double acceleration,
    double gyro = 0.1,
    Duration? gyroAt,
    bool hasGyroscope = true,
  }) {
    final timestamp = DateTime.fromMillisecondsSinceEpoch(
      (detectorStart + at).inMilliseconds,
      isUtc: true,
    );
    return ImuSample(
      accelerometerX: acceleration,
      accelerometerY: 0,
      accelerometerZ: 0,
      gyroscopeX: gyro,
      gyroscopeY: 0,
      gyroscopeZ: 0,
      timestamp: timestamp,
      gyroscopeTimestamp: !hasGyroscope
          ? null
          : gyroAt == null
          ? timestamp
          : DateTime.fromMillisecondsSinceEpoch(
              (detectorStart + gyroAt).inMilliseconds,
              isUtc: true,
            ),
    );
  }

  FallCandidate? completeImmobility(
    ImuFallDetector detector, {
    Duration impactAt = const Duration(milliseconds: 100),
  }) {
    FallCandidate? result;
    for (var index = 1; index <= 20; index++) {
      result = detector.addSample(
        sample(
          at: impactAt + Duration(milliseconds: index * 200),
          acceleration: 9.81,
        ),
      );
    }
    return result;
  }

  test('emits once after free fall, impact with jerk, and immobility', () {
    final detector = ImuFallDetector();

    expect(
      detector.addSample(sample(at: Duration.zero, acceleration: 2.5)),
      isNull,
    );
    final impact = sample(
      at: const Duration(milliseconds: 100),
      acceleration: 30,
      gyro: 0.2,
    );
    expect(detector.addSample(impact), isNull);

    final candidate = completeImmobility(detector);

    expect(candidate, isNotNull);
    expect(candidate!.impactSample, same(impact));
    expect(candidate.impactSample.accelerationMagnitude, 30);
    expect(detector.phase, FallDetectionPhase.idle);
  });

  test('retains the peak across a multi-sample impact pulse', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2.5));
    detector.addSample(
      sample(at: const Duration(milliseconds: 100), acceleration: 30),
    );
    final peak = sample(
      at: const Duration(milliseconds: 180),
      acceleration: 35,
      gyro: 0.3,
    );

    expect(detector.addSample(peak), isNull);
    final candidate = completeImmobility(detector);

    expect(candidate, isNotNull);
    expect(candidate!.impactSample, same(peak));
    expect(candidate.impactSample.accelerationMagnitude, 35);
  });

  test('rejects an impact that has no free-fall phase', () {
    final detector = ImuFallDetector();

    expect(
      detector.addSample(sample(at: Duration.zero, acceleration: 30)),
      isNull,
    );
    expect(detector.phase, FallDetectionPhase.idle);
  });

  test('rejects impact after the 1500ms free-fall window', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));

    detector.addSample(
      sample(at: const Duration(milliseconds: 1501), acceleration: 30),
    );

    expect(detector.phase, FallDetectionPhase.idle);
    expect(
      detector.latestDecision.reason,
      ImuDetectorDecisionReason.impactWindowExpired,
    );
    expect(completeImmobility(detector), isNull);
  });

  test('rejects an impact whose jerk is below 80 m/s3', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2.9));

    detector.addSample(
      sample(at: const Duration(milliseconds: 500), acceleration: 26),
    );

    expect(detector.phase, FallDetectionPhase.freeFall);
    expect(
      detector.latestDecision.reason,
      ImuDetectorDecisionReason.jerkTooLow,
    );
  });

  test('cancels a candidate on post-impact strong rotation', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));
    detector.addSample(
      sample(at: const Duration(milliseconds: 100), acceleration: 30),
    );

    detector.addSample(
      sample(
        at: const Duration(milliseconds: 400),
        acceleration: 9.81,
        gyro: 1.6,
      ),
    );

    expect(detector.phase, FallDetectionPhase.idle);
    expect(
      detector.latestDecision.reason,
      ImuDetectorDecisionReason.excessiveMovement,
    );
    expect(completeImmobility(detector), isNull);
  });

  test('fails safe when gyroscope data is stale', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));
    detector.addSample(
      sample(at: const Duration(milliseconds: 100), acceleration: 30),
    );

    detector.addSample(
      sample(
        at: const Duration(milliseconds: 400),
        gyroAt: const Duration(milliseconds: 100),
        acceleration: 9.81,
      ),
    );

    expect(detector.phase, FallDetectionPhase.idle);
    expect(
      detector.latestDecision.reason,
      ImuDetectorDecisionReason.gyroscopeStale,
    );
  });

  test('fails safe when post-impact sampling has a long gap', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));
    detector.addSample(
      sample(at: const Duration(milliseconds: 100), acceleration: 30),
    );

    final result = detector.addSample(
      sample(at: const Duration(milliseconds: 4100), acceleration: 9.81),
    );

    expect(result, isNull);
    expect(detector.phase, FallDetectionPhase.idle);
    expect(detector.latestDecision.reason, ImuDetectorDecisionReason.sampleGap);
  });

  test('fails safe when gyroscope data is missing', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));

    detector.addSample(
      sample(
        at: const Duration(milliseconds: 100),
        acceleration: 30,
        hasGyroscope: false,
      ),
    );

    expect(detector.phase, FallDetectionPhase.idle);
    expect(
      detector.latestDecision.reason,
      ImuDetectorDecisionReason.gyroscopeMissing,
    );
  });

  test('rejects out-of-order samples and exact exclusive thresholds', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 3));
    expect(detector.phase, FallDetectionPhase.idle);

    detector.addSample(
      sample(at: const Duration(milliseconds: 100), acceleration: 2),
    );
    detector.addSample(
      sample(at: const Duration(milliseconds: 200), acceleration: 25),
    );
    expect(detector.phase, FallDetectionPhase.freeFall);

    detector.addSample(
      sample(at: const Duration(milliseconds: 150), acceleration: 30),
    );
    expect(detector.phase, FallDetectionPhase.idle);
    expect(
      detector.latestDecision.reason,
      ImuDetectorDecisionReason.outOfOrder,
    );
  });

  test('requires at least 80 percent stationary post-impact samples', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));
    detector.addSample(
      sample(at: const Duration(milliseconds: 100), acceleration: 30),
    );

    FallCandidate? result;
    for (var index = 1; index <= 20; index++) {
      result = detector.addSample(
        sample(
          at: Duration(milliseconds: 100 + index * 200),
          acceleration: index <= 15 ? 9.81 : 13,
          gyro: index <= 15 ? 0.1 : 0.8,
        ),
      );
    }

    expect(result, isNull);
    expect(detector.phase, FallDetectionPhase.idle);
    expect(
      detector.latestDecision.reason,
      ImuDetectorDecisionReason.insufficientStationarySamples,
    );
  });

  test('suppresses duplicate candidates during the 30-second cooldown', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));
    detector.addSample(
      sample(at: const Duration(milliseconds: 100), acceleration: 30),
    );
    expect(completeImmobility(detector), isNotNull);

    detector.addSample(
      sample(at: const Duration(seconds: 10), acceleration: 2),
    );
    detector.addSample(
      sample(at: const Duration(milliseconds: 10100), acceleration: 30),
    );

    expect(detector.phase, FallDetectionPhase.idle);
    expect(detector.latestDecision.reason, ImuDetectorDecisionReason.cooldown);
  });

  test('reports structured phase transitions and rejection reasons', () {
    final detector = ImuFallDetector();

    detector.addSample(sample(at: Duration.zero, acceleration: 2));
    expect(detector.latestDecision.phase, FallDetectionPhase.freeFall);
    expect(
      detector.latestDecision.reason,
      ImuDetectorDecisionReason.freeFallDetected,
    );

    detector.addSample(
      sample(
        at: const Duration(milliseconds: 100),
        acceleration: 30,
        hasGyroscope: false,
      ),
    );
    expect(detector.latestDecision.phase, FallDetectionPhase.idle);
    expect(
      detector.latestDecision.reason,
      ImuDetectorDecisionReason.gyroscopeMissing,
    );
  });

  test('canonical simulation uses the real detector and emits a candidate', () {
    final detector = ImuFallDetector();
    FallCandidate? candidate;

    for (final simulated in ImuFallDetector.canonicalSimulationSamples(
      DateTime.utc(2026, 8, 4),
    )) {
      candidate = detector.addSample(simulated) ?? candidate;
    }

    expect(candidate, isNotNull);
    expect(detector.latestDecision.reason, ImuDetectorDecisionReason.accepted);
    expect(candidate!.stationarySampleRatio, 1);
  });
}
