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
    Duration impactAt = const Duration(milliseconds: 400),
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
      at: const Duration(milliseconds: 400),
      acceleration: 38,
      gyro: 0.2,
    );
    expect(detector.addSample(impact), isNull);

    final candidate = completeImmobility(detector, impactAt: const Duration(milliseconds: 400));

    expect(candidate, isNotNull);
    expect(candidate!.impactSample, same(impact));
    expect(candidate.impactSample.accelerationMagnitude, 38);
    expect(detector.phase, FallDetectionPhase.idle);
  });

  test('retains the peak across a multi-sample impact pulse', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2.5));
    detector.addSample(
      sample(at: const Duration(milliseconds: 400), acceleration: 38),
    );
    final peak = sample(
      at: const Duration(milliseconds: 480),
      acceleration: 45,
      gyro: 0.3,
    );

    expect(detector.addSample(peak), isNull);
    final candidate = completeImmobility(detector, impactAt: const Duration(milliseconds: 480));

    expect(candidate, isNotNull);
    expect(candidate!.impactSample, same(peak));
    expect(candidate.impactSample.accelerationMagnitude, 45);
  });

  test('rejects an impact that has no free-fall phase', () {
    final detector = ImuFallDetector();

    expect(
      detector.addSample(sample(at: Duration.zero, acceleration: 38)),
      isNull,
    );
    expect(detector.phase, FallDetectionPhase.idle);
  });

  test('rejects a short motion whose low-g phase is less than 350ms', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 3.5));

    detector.addSample(
      sample(at: const Duration(milliseconds: 150), acceleration: 38.0),
    );

    expect(detector.phase, FallDetectionPhase.idle);
    expect(
      detector.latestDecision.reason,
      ImuDetectorDecisionReason.freeFallTooShort,
    );
  });

  test('rejects lifting or lowering phone by 10cm (acceleration stays > 4.5 m/s2)', () {
    final detector = ImuFallDetector();
    // Lifting/lowering phone dips acceleration to ~6.5 - 7.5 m/s2
    expect(detector.addSample(sample(at: Duration.zero, acceleration: 7.0)), isNull);
    expect(detector.phase, FallDetectionPhase.idle);
    expect(detector.latestDecision.reason, ImuDetectorDecisionReason.awaitingFreeFall);

    expect(detector.addSample(sample(at: const Duration(milliseconds: 100), acceleration: 6.2)), isNull);
    expect(detector.phase, FallDetectionPhase.idle);
    expect(detector.latestDecision.reason, ImuDetectorDecisionReason.awaitingFreeFall);

    expect(detector.addSample(sample(at: const Duration(milliseconds: 200), acceleration: 9.81)), isNull);
    expect(detector.phase, FallDetectionPhase.idle);
  });

  test('rejects impact after the 1400ms free-fall window', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));

    detector.addSample(
      sample(at: const Duration(milliseconds: 1401), acceleration: 38),
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
      detector.addSample(sample(at: Duration.zero, acceleration: 3.5));

      detector.addSample(
        sample(at: const Duration(milliseconds: 600), acceleration: 23.0),
      );

      expect(detector.phase, FallDetectionPhase.freeFall);
      expect(
        detector.latestDecision.reason,
        ImuDetectorDecisionReason.jerkTooLow,
      );
    },
  );

  test('allows a long soft landing with jerk between 40 and 80 m/s3', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));
    final impact = sample(
      at: const Duration(milliseconds: 400),
      acceleration: 23.0,
      gyro: 0.2,
    );

    expect(detector.addSample(impact), isNull);
    expect(detector.phase, FallDetectionPhase.impact);
    FallCandidate? candidate;
    for (var index = 1; index <= 6; index++) {
      candidate ??= detector.addSample(
        sample(
          at: Duration(milliseconds: 600 + index * 200),
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
        at: const Duration(milliseconds: 400),
        acceleration: 23.0,
        gyro: 0.2,
      );

      expect(detector.addSample(impact), isNull);
      expect(detector.phase, FallDetectionPhase.impact);
      expect(
        completeImmobility(
          detector,
          impactAt: const Duration(milliseconds: 400),
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
        at: const Duration(milliseconds: 400),
        acceleration: 23.0,
        gyro: 0.2,
      ),
    );

    expect(
      detector.addSample(
        sample(
          at: const Duration(milliseconds: 650),
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
          at: Duration(milliseconds: 650 + index * 200),
          acceleration: 9.81,
          gyro: 0.1,
        ),
      );
    }
    expect(candidate, isNotNull);
  });

  test(
    'accepts a filtered noisy 150 cm pillow fall before the phone is picked up',
    () {
      final detector = ImuFallDetector();
      detector.addSample(sample(at: Duration.zero, acceleration: 3.8));
      detector.addSample(
        sample(at: const Duration(milliseconds: 200), acceleration: 3.2),
      );
      detector.addSample(
        sample(
          at: const Duration(milliseconds: 400),
          acceleration: 23.5,
          gyro: 0.4,
        ),
      );
      expect(detector.phase, FallDetectionPhase.impact);

      final postImpact = <ImuSample>[
        sample(
          at: const Duration(milliseconds: 600),
          acceleration: 12.0,
          gyro: 1.8,
        ),
        sample(
          at: const Duration(milliseconds: 680),
          acceleration: 12.2,
          gyro: 1.6,
        ),
        sample(
          at: const Duration(milliseconds: 780),
          acceleration: 11.8,
          gyro: 0.8,
        ),
        sample(
          at: const Duration(milliseconds: 880),
          acceleration: 10.8,
          gyro: 0.7,
        ),
        sample(
          at: const Duration(milliseconds: 1000),
          acceleration: 9.9,
          gyro: 0.6,
        ),
        sample(
          at: const Duration(milliseconds: 1150),
          acceleration: 9.85,
          gyro: 0.3,
        ),
        sample(
          at: const Duration(milliseconds: 1300),
          acceleration: 9.81,
          gyro: 0.1,
        ),
      ];
      FallCandidate? candidate;
      for (final current in postImpact) {
        candidate ??= detector.addSample(current);
      }

      expect(candidate, isNotNull);
      expect(candidate!.stationarySampleRatio, greaterThanOrEqualTo(0.6));
    },
  );

  test('cancels a soft-fall candidate on sustained post-impact rotation', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));
    detector.addSample(
      sample(at: const Duration(milliseconds: 400), acceleration: 38),
    );

    FallCandidate? candidate;
    var cancelled = false;
    for (var offset = 700; offset <= 3200 && !cancelled; offset += 100) {
      candidate ??= detector.addSample(
        sample(
          at: Duration(milliseconds: offset),
          acceleration: 9.81,
          gyro: 2.6,
        ),
      );
      cancelled =
          detector.latestDecision.reason ==
          ImuDetectorDecisionReason.excessiveMovement;
    }

    expect(candidate, isNull);
    expect(cancelled, isTrue);
    expect(detector.phase, FallDetectionPhase.idle);
  });

  test('accepts a 150 cm drop that rebounds before coming to rest', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));
    final impact = sample(
      at: const Duration(milliseconds: 400),
      acceleration: 38,
    );
    detector.addSample(impact);

    // The phone bounces off the surface: it is briefly airborne again and then
    // strikes a second time, both outside the 250 ms settling grace.
    expect(
      detector.addSample(
        sample(
          at: const Duration(milliseconds: 700),
          acceleration: 0.5,
          gyro: 1.2,
        ),
      ),
      isNull,
    );
    expect(detector.phase, FallDetectionPhase.impact);
    expect(
      detector.addSample(
        sample(
          at: const Duration(milliseconds: 800),
          acceleration: 24,
          gyro: 1.0,
        ),
      ),
      isNull,
    );

    final candidate = completeImmobility(
      detector,
      impactAt: const Duration(milliseconds: 800),
    );

    expect(candidate, isNotNull);
    expect(candidate!.impactSample, same(impact));
    expect(
      candidate.stationarySampleRatio,
      greaterThanOrEqualTo(ImuFallDetector.minimumSoftLandingStationaryRatio),
    );
  });

  test('fails safe when gyroscope data is stale', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));
    detector.addSample(
      sample(at: const Duration(milliseconds: 400), acceleration: 38),
    );

    detector.addSample(
      sample(
        at: const Duration(milliseconds: 700),
        gyroAt: const Duration(milliseconds: 400),
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
      sample(at: const Duration(milliseconds: 400), acceleration: 38),
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
        at: const Duration(milliseconds: 400),
        acceleration: 38,
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
    detector.addSample(sample(at: Duration.zero, acceleration: 4.5));
    expect(detector.phase, FallDetectionPhase.idle);

    detector.addSample(
      sample(at: const Duration(milliseconds: 100), acceleration: 4.4),
    );
    detector.addSample(
      sample(at: const Duration(milliseconds: 500), acceleration: 22.0),
    );
    expect(detector.phase, FallDetectionPhase.freeFall);

    detector.addSample(
      sample(at: const Duration(milliseconds: 350), acceleration: 38),
    );
    expect(detector.phase, FallDetectionPhase.idle);
    expect(
      detector.latestDecision.reason,
      ImuDetectorDecisionReason.outOfOrder,
    );
  });

  test('requires at least 60 percent stationary samples for a soft fall', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));
    detector.addSample(
      sample(at: const Duration(milliseconds: 400), acceleration: 38),
    );

    FallCandidate? result;
    for (var index = 1; index <= 6; index++) {
      result ??= detector.addSample(
        sample(
          at: Duration(milliseconds: 400 + index * 200),
          acceleration: index <= 2 ? 9.81 : 13,
          gyro: index <= 2 ? 0.1 : 1.0,
        ),
      );
      if (detector.phase == FallDetectionPhase.idle) break;
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
      sample(at: const Duration(milliseconds: 400), acceleration: 38),
    );
    expect(completeImmobility(detector, impactAt: const Duration(milliseconds: 400)), isNotNull);

    detector.addSample(
      sample(at: const Duration(milliseconds: 3500), acceleration: 2),
    );
    detector.addSample(
      sample(at: const Duration(milliseconds: 3950), acceleration: 38),
    );

    expect(detector.phase, FallDetectionPhase.idle);
    expect(detector.latestDecision.reason, ImuDetectorDecisionReason.cooldown);
  });

  test(
    'alert response clears partial phases, debounces, then rearms the next fall',
    () {
      final detector = ImuFallDetector();
      detector.addSample(sample(at: Duration.zero, acceleration: 2));
      detector.addSample(
        sample(at: const Duration(milliseconds: 400), acceleration: 38),
      );
      expect(completeImmobility(detector, impactAt: const Duration(milliseconds: 400)), isNotNull);

      detector.addSample(
        sample(at: const Duration(milliseconds: 4200), acceleration: 2),
      );
      expect(detector.phase, FallDetectionPhase.freeFall);

      final respondedAt = sample(
        at: const Duration(milliseconds: 4300),
        acceleration: 9.81,
      ).timestamp;
      detector.rearmAfterAlertResponse(respondedAt);
      expect(detector.phase, FallDetectionPhase.idle);

      // Handling the phone to answer the alert must not raise a second alert.
      detector.addSample(
        sample(at: const Duration(milliseconds: 5300), acceleration: 2),
      );
      expect(detector.phase, FallDetectionPhase.idle);
      expect(detector.latestDecision.reason, ImuDetectorDecisionReason.cooldown);

      // A genuine later fall is detected again once the debounce has elapsed.
      detector.addSample(
        sample(at: const Duration(milliseconds: 7400), acceleration: 2),
      );
      detector.addSample(
        sample(at: const Duration(milliseconds: 7850), acceleration: 38),
      );
      final nextCandidate = completeImmobility(
        detector,
        impactAt: const Duration(milliseconds: 7850),
      );

      expect(nextCandidate, isNotNull);
      expect(
        detector.latestDecision.reason,
        ImuDetectorDecisionReason.accepted,
      );
    },
  );

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
        at: const Duration(milliseconds: 400),
        acceleration: 38,
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
      if (candidate != null) break;
    }

    expect(candidate, isNotNull);
    expect(detector.latestDecision.reason, ImuDetectorDecisionReason.accepted);
    expect(candidate!.stationarySampleRatio, 1);
  });

  test('accepts a controlled 150 cm fall onto a soft surface (pillow/cushion)', () {
    final detector = ImuFallDetector();
    FallCandidate? candidate;

    // A 150 cm drop has ~550ms free fall (< 4.5 m/s2) and soft landing peak ~23.5 m/s2
    final samples = <ImuSample>[
      sample(at: Duration.zero, acceleration: 3.5),
      sample(at: const Duration(milliseconds: 200), acceleration: 2.0),
      sample(at: const Duration(milliseconds: 400), acceleration: 2.2),
      sample(
        at: const Duration(milliseconds: 520),
        acceleration: 24.5,
        gyro: 0.3,
      ),
      for (var index = 1; index <= 6; index++)
        sample(
          at: Duration(milliseconds: 520 + index * 200),
          acceleration: 9.81,
          gyro: 0.1,
        ),
    ];

    for (final value in samples) {
      candidate = detector.addSample(value) ?? candidate;
    }

    expect(candidate, isNotNull);
    expect(candidate!.impactSample.accelerationMagnitude, 24.5);
    expect(detector.phase, FallDetectionPhase.idle);
  });
}
