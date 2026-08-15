import 'dart:math';

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

  test('rejects a 1 cm tap whose low-g phase is less than 80ms', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 5.0));

    detector.addSample(
      sample(at: const Duration(milliseconds: 40), acceleration: 12),
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
      detector.addSample(sample(at: Duration.zero, acceleration: 4.0));

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
        at: const Duration(milliseconds: 200),
        acceleration: 8.6,
        gyro: 0.2,
      );

      expect(detector.addSample(impact), isNull);
      expect(detector.phase, FallDetectionPhase.impact);
      expect(
        completeImmobility(
          detector,
          impactAt: const Duration(milliseconds: 200),
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
          gyro: 1.5,
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

  test(
    'accepts a filtered noisy 50 cm pillow fall before the phone is picked up',
    () {
      final detector = ImuFallDetector();
      detector.addSample(sample(at: Duration.zero, acceleration: 4.5));
      detector.addSample(
        sample(at: const Duration(milliseconds: 100), acceleration: 2.2),
      );
      detector.addSample(
        sample(at: const Duration(milliseconds: 250), acceleration: 2.0),
      );
      detector.addSample(
        sample(
          at: const Duration(milliseconds: 280),
          acceleration: 8.8,
          gyro: 0.4,
        ),
      );
      expect(detector.phase, FallDetectionPhase.impact);

      final postImpact = <ImuSample>[
        sample(
          at: const Duration(milliseconds: 500),
          acceleration: 11.0,
          gyro: 0.4,
        ),
        sample(
          at: const Duration(milliseconds: 700),
          acceleration: 10.2,
          gyro: 0.3,
        ),
        sample(
          at: const Duration(milliseconds: 900),
          acceleration: 9.9,
          gyro: 0.2,
        ),
        sample(
          at: const Duration(milliseconds: 1100),
          acceleration: 9.81,
          gyro: 0.1,
        ),
        sample(
          at: const Duration(milliseconds: 1300),
          acceleration: 9.81,
          gyro: 0.05,
        ),
      ];
      FallCandidate? candidate;
      for (final current in postImpact) {
        candidate ??= detector.addSample(current);
      }

      expect(candidate, isNotNull);
      expect(
        candidate!.stationarySampleRatio,
        greaterThanOrEqualTo(ImuFallDetector.minimumSoftLandingStationaryRatio),
      );
    },
  );

  test('cancels a soft-fall candidate on sustained post-impact rotation', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));
    detector.addSample(
      sample(at: const Duration(milliseconds: 100), acceleration: 30),
    );

    FallCandidate? candidate;
    var cancelled = false;
    for (var offset = 400; offset <= 3200 && !cancelled; offset += 100) {
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

  test('accepts a 50 cm drop that rebounds before coming to rest', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));
    final impact = sample(
      at: const Duration(milliseconds: 250),
      acceleration: 30,
    );
    detector.addSample(impact);

    // The phone bounces off the surface: it is briefly airborne again and then
    // strikes a second time, both outside the 250 ms settling grace.
    expect(
      detector.addSample(
        sample(
          at: const Duration(milliseconds: 550),
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
          at: const Duration(milliseconds: 650),
          acceleration: 24,
          gyro: 1.0,
        ),
      ),
      isNull,
    );

    final candidate = completeImmobility(
      detector,
      impactAt: const Duration(milliseconds: 650),
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
    detector.addSample(sample(at: Duration.zero, acceleration: 6.0));
    expect(detector.phase, FallDetectionPhase.idle);

    detector.addSample(
      sample(at: const Duration(milliseconds: 100), acceleration: 5.9),
    );
    detector.addSample(
      sample(at: const Duration(milliseconds: 200), acceleration: 5.8),
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

  test('requires at least 70 percent stationary samples for a soft fall', () {
    final detector = ImuFallDetector();
    detector.addSample(sample(at: Duration.zero, acceleration: 2));
    detector.addSample(
      sample(at: const Duration(milliseconds: 200), acceleration: 30),
    );

    FallCandidate? result;
    for (var index = 1; index <= 4; index++) {
      result ??= detector.addSample(
        sample(
          at: Duration(milliseconds: 200 + index * 200),
          acceleration: index <= 2 ? 9.81 : 13,
          gyro: index <= 2 ? 0.1 : 1.0,
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

  test(
    'alert response clears partial phases, debounces, then rearms the next fall',
    () {
      final detector = ImuFallDetector();
      detector.addSample(sample(at: Duration.zero, acceleration: 2));
      detector.addSample(
        sample(at: const Duration(milliseconds: 100), acceleration: 30),
      );
      expect(completeImmobility(detector), isNotNull);

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

      detector.addSample(
        sample(at: const Duration(milliseconds: 5300), acceleration: 2),
      );
      expect(detector.phase, FallDetectionPhase.idle);
      expect(detector.latestDecision.reason, ImuDetectorDecisionReason.cooldown);

      detector.addSample(
        sample(at: const Duration(milliseconds: 7400), acceleration: 2),
      );
      detector.addSample(
        sample(at: const Duration(milliseconds: 7500), acceleration: 30),
      );
      final nextCandidate = completeImmobility(
        detector,
        impactAt: const Duration(milliseconds: 7500),
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
      if (candidate != null) break;
    }

    expect(candidate, isNotNull);
    expect(detector.latestDecision.reason, ImuDetectorDecisionReason.accepted);
    expect(candidate!.stationarySampleRatio, 1);
  });

  test('accepts a controlled 50 cm fall onto a soft surface', () {
    final detector = ImuFallDetector();
    FallCandidate? candidate;

    final samples = <ImuSample>[
      sample(at: Duration.zero, acceleration: 4.5),
      sample(at: const Duration(milliseconds: 250), acceleration: 2.0),
      sample(
        at: const Duration(milliseconds: 280),
        acceleration: 9.6,
        gyro: 0.2,
      ),
      for (var index = 1; index <= 6; index++)
        sample(
          at: Duration(milliseconds: 280 + index * 200),
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

  // =========================================================================
  // NEW TESTS: Hand Shaking Rejection & 50cm Pillow Soft Landing Verification
  // =========================================================================

  test(
    'rejects vigorous hand shaking with continuous oscillation and high rotation',
    () {
      final detector = ImuFallDetector();
      FallCandidate? candidate;

      // 3.5 Hz sinusoidal shaking for 2 seconds (50 Hz sampling)
      for (var step = 0; step <= 100; step++) {
        final tSeconds = step * 0.02;
        final acc = 9.81 + 14.0 * sin(2 * pi * 3.5 * tSeconds);
        final gyro = 3.8 + 1.2 * sin(2 * pi * 3.5 * tSeconds).abs();

        final result = detector.addSample(
          sample(
            at: Duration(milliseconds: step * 20),
            acceleration: acc.abs(),
            gyro: gyro,
          ),
        );
        candidate ??= result;
      }

      expect(candidate, isNull);
      expect(detector.phase, FallDetectionPhase.idle);
    },
  );

  test(
    'rejects rapid hand shake troughs shorter than minimumFreeFallDuration',
    () {
      final detector = ImuFallDetector();

      // Brief 50ms trough during violent direction change (< 80ms minimum duration)
      detector.addSample(
        sample(at: Duration.zero, acceleration: 2.0, gyro: 1.0),
      );
      detector.addSample(
        sample(
          at: const Duration(milliseconds: 50),
          acceleration: 26.0,
          gyro: 1.5,
        ),
      );

      expect(detector.phase, FallDetectionPhase.idle);
      expect(
        detector.latestDecision.reason,
        ImuDetectorDecisionReason.freeFallTooShort,
      );
    },
  );

  test('rejects hand shake with excessive rotation during low-g phase', () {
    final detector = ImuFallDetector();

    // 100ms dip but with high angular velocity from hand twisting (gyro 4.2 rad/s > 3.2 limit)
    detector.addSample(sample(at: Duration.zero, acceleration: 2.0, gyro: 4.2));

    expect(detector.phase, FallDetectionPhase.idle);
    expect(
      detector.latestDecision.reason,
      ImuDetectorDecisionReason.excessiveMovement,
    );
  });

  test(
    'rejects hand shake followed by holding phone in hand with minor tremor',
    () {
      final detector = ImuFallDetector();

      // Rapid shake spike without qualifying soft-fall duration
      detector.addSample(
        sample(at: Duration.zero, acceleration: 2.0, gyro: 1.0),
      );
      detector.addSample(
        sample(
          at: const Duration(milliseconds: 100),
          acceleration: 25.0,
          gyro: 1.2,
        ),
      );
      expect(detector.phase, FallDetectionPhase.impact);

      // Held in hand: physiological tremor (gyro ~0.6 rad/s > 0.4 stationary threshold)
      FallCandidate? candidate;
      var failedStationary = false;
      for (var index = 1; index <= 8; index++) {
        candidate ??= detector.addSample(
          sample(
            at: Duration(milliseconds: 100 + index * 200),
            acceleration: 9.81,
            gyro: 0.6,
          ),
        );
        if (detector.latestDecision.reason ==
            ImuDetectorDecisionReason.insufficientStationarySamples) {
          failedStationary = true;
        }
      }

      expect(candidate, isNull);
      expect(detector.phase, FallDetectionPhase.idle);
      expect(failedStationary, isTrue);
    },
  );

  test('accepts authentic 50 cm drop onto a soft pillow', () {
    final detector = ImuFallDetector();
    FallCandidate? candidate;

    // 1. Free fall: ~280ms duration, low-g (2.0 m/s²), low rotation (0.2 rad/s)
    detector.addSample(sample(at: Duration.zero, acceleration: 2.2, gyro: 0.2));
    detector.addSample(
      sample(
        at: const Duration(milliseconds: 120),
        acceleration: 1.8,
        gyro: 0.2,
      ),
    );
    detector.addSample(
      sample(
        at: const Duration(milliseconds: 260),
        acceleration: 2.0,
        gyro: 0.25,
      ),
    );

    // 2. Soft impact on pillow: 8.8 m/s² deceleration peak
    final impactSample = sample(
      at: const Duration(milliseconds: 280),
      acceleration: 8.8,
      gyro: 0.35,
    );
    expect(detector.addSample(impactSample), isNull);
    expect(detector.phase, FallDetectionPhase.impact);

    // 3. Resting on pillow: 800ms of stationary rest (a ≈ 9.81, gyro ≈ 0.05 rad/s)
    for (var index = 1; index <= 5; index++) {
      candidate ??= detector.addSample(
        sample(
          at: Duration(milliseconds: 280 + index * 200),
          acceleration: 9.81,
          gyro: 0.05,
        ),
      );
    }

    expect(candidate, isNotNull);
    expect(candidate!.impactSample, same(impactSample));
    expect(candidate.impactSample.accelerationMagnitude, 8.8);
    expect(detector.latestDecision.reason, ImuDetectorDecisionReason.accepted);
    expect(candidate.stationarySampleRatio, 1.0);
  });
}
