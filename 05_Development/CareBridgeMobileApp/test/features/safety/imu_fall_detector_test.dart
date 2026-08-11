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
      result ??= detector.addSample(
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

  test('rejects a short tap that looks like free fall for less than 80ms', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));

    detector.addSample(
      sample(at: const Duration(milliseconds: 60), acceleration: 12),
    );

    expect(detector.phase, FallDetectionPhase.idle);
    expect(
      detector.latestDecision.reason,
      ImuDetectorDecisionReason.freeFallTooShort,
    );
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

  test(
    'rejects an impact whose jerk is below the applicable soft-fall floor',
    () {
      final detector = ImuFallDetector();
      detector.addSample(sample(at: Duration.zero, acceleration: 6.4));

      detector.addSample(
        sample(at: const Duration(milliseconds: 500), acceleration: 9.6),
      );

      expect(detector.phase, FallDetectionPhase.freeFall);
      expect(
        detector.latestDecision.reason,
        ImuDetectorDecisionReason.jerkTooLow,
      );
    },
  );

  test('allows a long soft landing with jerk between 20 and 40 m/s3', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));
    final impact = sample(
      at: const Duration(milliseconds: 250),
      acceleration: 9.6,
      gyro: 0.2,
    );

    expect(detector.addSample(impact), isNull);
    expect(detector.phase, FallDetectionPhase.impact);
    FallCandidate? candidate;
    for (var index = 1; index <= 6; index++) {
      candidate ??= detector.addSample(
        sample(
          at: Duration(milliseconds: 500 + index * 200),
          acceleration: 9.81,
          gyro: 0.1,
        ),
      );
    }
    expect(candidate, isNotNull);
  });

  test(
    'accepts a long soft landing whose peak stays below hard-impact threshold',
    () {
      final detector = ImuFallDetector();
      detector.addSample(sample(at: Duration.zero, acceleration: 2));
      final impact = sample(
        at: const Duration(milliseconds: 250),
        acceleration: 8.6,
        gyro: 0.2,
      );

      expect(detector.addSample(impact), isNull);
      expect(detector.phase, FallDetectionPhase.impact);
      expect(
        completeImmobility(
          detector,
          impactAt: const Duration(milliseconds: 250),
        ),
        isNotNull,
      );
    },
  );

  test('allows brief pillow rebound after a long fall', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));
    detector.addSample(
      sample(
        at: const Duration(milliseconds: 250),
        acceleration: 9.6,
        gyro: 0.2,
      ),
    );

    expect(
      detector.addSample(
        sample(
          at: const Duration(milliseconds: 500),
          acceleration: 9.81,
          gyro: 2.0,
        ),
      ),
      isNull,
    );
    FallCandidate? candidate;
    for (var index = 1; index <= 6; index++) {
      candidate ??= detector.addSample(
        sample(
          at: Duration(milliseconds: 500 + index * 200),
          acceleration: 9.81,
          gyro: 0.1,
        ),
      );
    }
    expect(candidate, isNotNull);
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
    detector.addSample(sample(at: Duration.zero, acceleration: 6.5));
    expect(detector.phase, FallDetectionPhase.idle);

    detector.addSample(
      sample(at: const Duration(milliseconds: 100), acceleration: 6.4),
    );
    detector.addSample(
      sample(at: const Duration(milliseconds: 200), acceleration: 9.5),
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
    for (var index = 1; index <= 5; index++) {
      result ??= detector.addSample(
        sample(
          at: Duration(milliseconds: 100 + index * 200),
          acceleration: index <= 2 ? 9.81 : 11.9,
          gyro: index <= 2 ? 0.1 : 0.6,
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

  test('suppresses duplicate candidates during the three-second cooldown', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));
    detector.addSample(
      sample(at: const Duration(milliseconds: 100), acceleration: 30),
    );
    expect(completeImmobility(detector), isNotNull);

    detector.addSample(
      sample(at: const Duration(milliseconds: 3500), acceleration: 2),
    );
    detector.addSample(
      sample(at: const Duration(milliseconds: 3600), acceleration: 30),
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

  test('accepts a controlled 50 cm fall onto a soft surface', () {
    final detector = ImuFallDetector();
    FallCandidate? candidate;

    final samples = <ImuSample>[
      sample(at: Duration.zero, acceleration: 5.5),
      sample(at: const Duration(milliseconds: 300), acceleration: 2.0),
      sample(
        at: const Duration(milliseconds: 320),
        acceleration: 9.6,
        gyro: 0.2,
      ),
      for (var index = 1; index <= 6; index++)
        sample(
          at: Duration(milliseconds: 320 + index * 200),
          acceleration: 9.81,
          gyro: 0.1,
        ),
    ];

    for (final value in samples) {
      candidate = detector.addSample(value) ?? candidate;
    }

    expect(candidate, isNotNull);
    expect(candidate!.impactSample.accelerationMagnitude, 9.6);
    expect(detector.phase, FallDetectionPhase.idle);
  });
}
