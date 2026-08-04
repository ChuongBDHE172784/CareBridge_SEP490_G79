import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:sensors_plus/sensors_plus.dart';
import 'package:untitled/features/safety/models/imu_diagnostics_model.dart';
import 'package:untitled/features/safety/services/fall_detection_sensor_service.dart';
import 'package:untitled/features/safety/services/safety_demo_mode.dart';

void main() {
  test(
    'publishes awaiting, throttled sampling, stale, error, and stopped states',
    () async {
      final accelerometer = StreamController<AccelerometerEvent>.broadcast();
      final gyroscope = StreamController<GyroscopeEvent>.broadcast();
      var now = DateTime.utc(2026, 8, 4, 10);
      final service = FallDetectionSensorService.forTesting(
        accelerometerEvents: () => accelerometer.stream,
        gyroscopeEvents: () => gyroscope.stream,
        now: () => now,
      );
      final snapshots = <ImuDiagnosticsSnapshot>[];
      final subscription = service.diagnostics.listen(snapshots.add);

      await service.start();
      await Future<void>.delayed(Duration.zero);
      expect(snapshots.last.state, ImuSamplingState.awaitingSamples);
      final generation = snapshots.last.generation;
      expect(service.diagnosticsLifecycleActiveForTesting, isTrue);

      gyroscope.add(GyroscopeEvent(0.1, 0, 0, now));
      accelerometer.add(AccelerometerEvent(9.81, 0, 0, now));
      await Future<void>.delayed(Duration.zero);
      expect(snapshots.last.state, ImuSamplingState.sampling);
      expect(snapshots.last.generation, generation);
      final firstSamplingCount = snapshots.length;

      now = now.add(const Duration(milliseconds: 100));
      gyroscope.add(GyroscopeEvent(0.1, 0, 0, now));
      accelerometer.add(AccelerometerEvent(9.81, 0, 0, now));
      await Future<void>.delayed(Duration.zero);
      expect(snapshots, hasLength(firstSamplingCount));

      now = now.add(const Duration(milliseconds: 200));
      gyroscope.add(GyroscopeEvent(0.1, 0, 0, now));
      accelerometer.add(AccelerometerEvent(9.81, 0, 0, now));
      await Future<void>.delayed(Duration.zero);
      expect(snapshots, hasLength(firstSamplingCount + 1));
      expect(snapshots.last.sampleRateHz, isNotNull);

      now = now.add(const Duration(seconds: 3));
      service.checkDiagnosticsFreshnessForTesting();
      await Future<void>.delayed(Duration.zero);
      expect(snapshots.last.state, ImuSamplingState.stale);
      expect(
        snapshots.last.capturedAt,
        now.subtract(const Duration(seconds: 3)),
      );

      gyroscope.addError(StateError('sensor unavailable'));
      await Future<void>.delayed(Duration.zero);
      expect(snapshots.last.state, ImuSamplingState.error);
      expect(snapshots.last.errorMessage, contains('sensor unavailable'));

      now = now.add(const Duration(milliseconds: 300));
      accelerometer.add(AccelerometerEvent(9.81, 0, 0, now));
      await Future<void>.delayed(Duration.zero);
      expect(snapshots.last.state, ImuSamplingState.error);

      gyroscope.add(GyroscopeEvent(0.1, 0, 0, now));
      accelerometer.add(AccelerometerEvent(9.81, 0, 0, now));
      await Future<void>.delayed(Duration.zero);
      expect(snapshots.last.state, ImuSamplingState.sampling);

      await service.stop();
      await Future<void>.delayed(Duration.zero);
      expect(snapshots.last.state, ImuSamplingState.stopped);
      expect(snapshots.last.generation, greaterThan(generation));
      expect(service.diagnosticsLifecycleActiveForTesting, isFalse);
      final stoppedCount = snapshots.length;

      now = now.add(const Duration(seconds: 5));
      service.checkDiagnosticsFreshnessForTesting();
      await Future<void>.delayed(Duration.zero);
      expect(snapshots, hasLength(stoppedCount));

      now = now.subtract(const Duration(days: 1));
      await service.start();
      await Future<void>.delayed(Duration.zero);
      expect(
        snapshots.last.generation,
        greaterThan(snapshots[stoppedCount - 1].generation),
      );
      await service.stop();

      await subscription.cancel();
      await accelerometer.close();
      await gyroscope.close();
    },
  );

  test(
    'cleans up when a sensor stream fails synchronously during start',
    () async {
      final service = FallDetectionSensorService.forTesting(
        accelerometerEvents: () =>
            throw StateError('accelerometer unavailable'),
        gyroscopeEvents: () => const Stream<GyroscopeEvent>.empty(),
        now: () => DateTime.utc(2026, 8, 4),
      );
      final snapshots = <ImuDiagnosticsSnapshot>[];
      final subscription = service.diagnostics.listen(snapshots.add);

      await expectLater(service.start(), throwsStateError);
      await Future<void>.delayed(Duration.zero);

      expect(service.isRunning, isFalse);
      expect(service.diagnosticsLifecycleActiveForTesting, isFalse);
      expect(snapshots.last.state, ImuSamplingState.error);
      await subscription.cancel();
    },
  );

  test(
    'demo gesture bypasses diagnostics throttle with a new sequence',
    () async {
      final accelerometer = StreamController<AccelerometerEvent>.broadcast();
      final gyroscope = StreamController<GyroscopeEvent>.broadcast();
      var now = DateTime.utc(2026, 8, 4, 10);
      final service = FallDetectionSensorService.forTesting(
        accelerometerEvents: () => accelerometer.stream,
        gyroscopeEvents: () => gyroscope.stream,
        now: () => now,
      );
      final snapshots = <ImuDiagnosticsSnapshot>[];
      final subscription = service.diagnostics.listen(snapshots.add);
      await service.start();

      final startedAt = now;
      for (var index = 0; index <= 20; index++) {
        now = startedAt.add(Duration(milliseconds: index * 20));
        gyroscope.add(GyroscopeEvent(0.05, 0, 0, now));
        accelerometer.add(AccelerometerEvent(9.81, 0, 0, now));
        await Future<void>.delayed(Duration.zero);
      }
      expect(snapshots.last.demoGestureSequence, isZero);

      var detectedAndForcePublished = false;
      for (var index = 0; index < 26; index++) {
        now = startedAt.add(Duration(milliseconds: 420 + index * 20));
        final beforeSampleCount = snapshots.length;
        gyroscope.add(GyroscopeEvent(2.8, 0, 0, now));
        accelerometer.add(AccelerometerEvent(16.5, 0, 0, now));
        await Future<void>.delayed(Duration.zero);
        if (snapshots.last.demoGestureSequence == 1) {
          expect(snapshots.length, greaterThan(beforeSampleCount));
          expect(snapshots.last.capturedAt, now);
          detectedAndForcePublished = true;
          break;
        }
      }

      expect(detectedAndForcePublished, isTrue);
      expect(snapshots.last.demoGestureSequence, 1);

      await service.stop();
      await subscription.cancel();
      await accelerometer.close();
      await gyroscope.close();
    },
    skip: !safetyDemoMode,
  );
}
