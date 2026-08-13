import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:sensors_plus/sensors_plus.dart';
import 'package:untitled/features/safety/models/imu_diagnostics_model.dart';
import 'package:untitled/features/safety/models/safety_config_model.dart';
import 'package:untitled/features/safety/services/fall_detection_sensor_service.dart';
import 'package:untitled/features/safety/services/safety_demo_mode.dart';
import 'package:untitled/features/safety/services/safety_service.dart';

class _FakeSafetyService extends SafetyService {
  _FakeSafetyService(this.onSend);

  final Future<SafetyEvent?> Function() onSend;
  int sendCount = 0;

  @override
  Future<SafetyEvent?> sendImuData({
    required double accelerometerX,
    required double accelerometerY,
    required double accelerometerZ,
    required double gyroscopeX,
    required double gyroscopeY,
    required double gyroscopeZ,
    required DateTime timestamp,
    bool onDeviceFallConfirmed = false,
    String? signalId,
    double? latitude,
    double? longitude,
  }) {
    sendCount++;
    return onSend();
  }
}

class _QueuedSafetyService extends SafetyService {
  final List<Completer<SafetyEvent?>> sends = [];

  @override
  Future<SafetyEvent?> sendImuData({
    required double accelerometerX,
    required double accelerometerY,
    required double accelerometerZ,
    required double gyroscopeX,
    required double gyroscopeY,
    required double gyroscopeZ,
    required DateTime timestamp,
    bool onDeviceFallConfirmed = false,
    String? signalId,
    double? latitude,
    double? longitude,
  }) {
    final completer = Completer<SafetyEvent?>();
    sends.add(completer);
    return completer.future;
  }
}

Future<void> _emitFall({
  required StreamController<AccelerometerEvent> accelerometer,
  required StreamController<GyroscopeEvent> gyroscope,
  required DateTime startedAt,
}) async {
  void addSample(Duration offset, double acceleration) {
    final timestamp = startedAt.add(offset);
    gyroscope.add(GyroscopeEvent(0.1, 0, 0, timestamp));
    accelerometer.add(AccelerometerEvent(acceleration, 0, 0, timestamp));
  }

  addSample(Duration.zero, 2.5);
  await Future<void>.delayed(Duration.zero);
  addSample(const Duration(milliseconds: 80), 2.5);
  await Future<void>.delayed(Duration.zero);
  addSample(const Duration(milliseconds: 100), 30);
  await Future<void>.delayed(Duration.zero);
  for (var index = 1; index <= 6; index++) {
    addSample(Duration(milliseconds: 100 + index * 200), 9.81);
    await Future<void>.delayed(Duration.zero);
  }
}

void main() {
  test('submits and emits a fall event without waiting for GPS', () async {
    final accelerometer = StreamController<AccelerometerEvent>.broadcast();
    final gyroscope = StreamController<GyroscopeEvent>.broadcast();
    final locationRefresh = Completer<void>();
    var locationRefreshCount = 0;
    const event = SafetyEvent(
      id: 'fall-1',
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'OPEN',
    );
    final safetyService = _FakeSafetyService(() async => event);
    final service = FallDetectionSensorService.forTesting(
      accelerometerEvents: () => accelerometer.stream,
      gyroscopeEvents: () => gyroscope.stream,
      now: () => DateTime.utc(2026, 8, 13, 10),
      safetyService: safetyService,
      refreshLocation: () {
        locationRefreshCount++;
        return locationRefresh.future;
      },
    );
    final emitted = <SafetyEvent>[];
    final subscription = service.detectedEvents.listen(emitted.add);

    await service.start(locationSharingAllowed: true);
    await _emitFall(
      accelerometer: accelerometer,
      gyroscope: gyroscope,
      startedAt: DateTime.utc(2026, 8, 13, 10),
    );
    await Future<void>.delayed(Duration.zero);

    expect(locationRefresh.isCompleted, isFalse);
    expect(locationRefreshCount, 1);
    expect(safetyService.sendCount, 1);
    expect(emitted, [same(event)]);

    await service.stop();
    await subscription.cancel();
    await accelerometer.close();
    await gyroscope.close();
  });

  test(
    'submits another physical fall while the previous request is in flight',
    () async {
      final accelerometer = StreamController<AccelerometerEvent>.broadcast();
      final gyroscope = StreamController<GyroscopeEvent>.broadcast();
      final safetyService = _QueuedSafetyService();
      final service = FallDetectionSensorService.forTesting(
        accelerometerEvents: () => accelerometer.stream,
        gyroscopeEvents: () => gyroscope.stream,
        now: () => DateTime.utc(2026, 8, 13, 10),
        safetyService: safetyService,
      );

      await service.start();
      final startedAt = DateTime.utc(2026, 8, 13, 10);
      await _emitFall(
        accelerometer: accelerometer,
        gyroscope: gyroscope,
        startedAt: startedAt,
      );
      expect(safetyService.sends, hasLength(1));

      await _emitFall(
        accelerometer: accelerometer,
        gyroscope: gyroscope,
        startedAt: startedAt.add(const Duration(seconds: 5)),
      );
      expect(safetyService.sends, hasLength(2));

      for (final send in safetyService.sends) {
        send.complete(null);
      }
      await Future<void>.delayed(Duration.zero);
      await service.stop();
      await accelerometer.close();
      await gyroscope.close();
    },
  );

  test('closes a stale API response after the user responded safe', () async {
    final accelerometer = StreamController<AccelerometerEvent>.broadcast();
    final gyroscope = StreamController<GyroscopeEvent>.broadcast();
    final safetyService = _QueuedSafetyService();
    final closed = <SafetyEvent>[];
    final service = FallDetectionSensorService.forTesting(
      accelerometerEvents: () => accelerometer.stream,
      gyroscopeEvents: () => gyroscope.stream,
      now: () => DateTime.utc(2026, 8, 13, 10),
      safetyService: safetyService,
      closeStaleEvent: (event) async => closed.add(event),
    );
    final emitted = <SafetyEvent>[];
    final subscription = service.detectedEvents.listen(emitted.add);

    await service.start();
    final startedAt = DateTime.utc(2026, 8, 13, 10);
    await _emitFall(
      accelerometer: accelerometer,
      gyroscope: gyroscope,
      startedAt: startedAt,
    );
    expect(safetyService.sends, hasLength(1));

    service.beginAlertResponse();
    const stale = SafetyEvent(
      id: 'stale-fall',
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'OPEN',
    );
    safetyService.sends.single.complete(stale);
    await Future<void>.delayed(Duration.zero);
    await Future<void>.delayed(Duration.zero);

    expect(emitted, isEmpty);
    expect(closed, [same(stale)]);
    await service.stop();
    await subscription.cancel();
    await accelerometer.close();
    await gyroscope.close();
  });

  test(
    'discards a fall detected while the alert was being answered',
    () async {
      final accelerometer = StreamController<AccelerometerEvent>.broadcast();
      final gyroscope = StreamController<GyroscopeEvent>.broadcast();
      final safetyService = _QueuedSafetyService();
      var now = DateTime.utc(2026, 8, 13, 10);
      final service = FallDetectionSensorService.forTesting(
        accelerometerEvents: () => accelerometer.stream,
        gyroscopeEvents: () => gyroscope.stream,
        now: () => now,
        safetyService: safetyService,
      );

      await service.start();
      service.beginAlertResponse();
      await _emitFall(
        accelerometer: accelerometer,
        gyroscope: gyroscope,
        startedAt: DateTime.utc(2026, 8, 13, 10),
      );

      expect(safetyService.sends, isEmpty);

      now = now.add(const Duration(seconds: 30));
      // The impact happened before the user answered, so it belongs to the
      // alert that was just closed and must not raise a second one.
      service.rearmAfterAlertResponse(DateTime.utc(2026, 8, 13, 10, 0, 2));
      expect(safetyService.sends, isEmpty);
      await service.stop();
      await accelerometer.close();
      await gyroscope.close();
    },
  );

  test('forwards a fall that happened after the alert was answered', () async {
    final accelerometer = StreamController<AccelerometerEvent>.broadcast();
    final gyroscope = StreamController<GyroscopeEvent>.broadcast();
    final safetyService = _QueuedSafetyService();
    var now = DateTime.utc(2026, 8, 13, 10);
    final service = FallDetectionSensorService.forTesting(
      accelerometerEvents: () => accelerometer.stream,
      gyroscopeEvents: () => gyroscope.stream,
      now: () => now,
      safetyService: safetyService,
    );

    await service.start();
    service.beginAlertResponse();
    await _emitFall(
      accelerometer: accelerometer,
      gyroscope: gyroscope,
      startedAt: DateTime.utc(2026, 8, 13, 10, 0, 5),
    );

    expect(safetyService.sends, isEmpty);

    now = now.add(const Duration(seconds: 30));
    service.rearmAfterAlertResponse(DateTime.utc(2026, 8, 13, 10, 0, 2));
    expect(safetyService.sends, hasLength(1));
    safetyService.sends.single.complete(null);
    await Future<void>.delayed(Duration.zero);
    await service.stop();
    await accelerometer.close();
    await gyroscope.close();
  });

  test('resumes detection if an alert response is never rearmed', () async {
    final accelerometer = StreamController<AccelerometerEvent>.broadcast();
    final gyroscope = StreamController<GyroscopeEvent>.broadcast();
    final safetyService = _QueuedSafetyService();
    var now = DateTime.utc(2026, 8, 13, 10);
    final service = FallDetectionSensorService.forTesting(
      accelerometerEvents: () => accelerometer.stream,
      gyroscopeEvents: () => gyroscope.stream,
      now: () => now,
      safetyService: safetyService,
    );

    await service.start();
    service.beginAlertResponse();

    now = now.add(const Duration(minutes: 10));
    await _emitFall(
      accelerometer: accelerometer,
      gyroscope: gyroscope,
      startedAt: now,
    );

    expect(safetyService.sends, hasLength(1));
    safetyService.sends.single.complete(null);
    await Future<void>.delayed(Duration.zero);
    await service.stop();
    await accelerometer.close();
    await gyroscope.close();
  });

  test('publishes the same backend fall event only once', () async {
    final accelerometer = StreamController<AccelerometerEvent>.broadcast();
    final gyroscope = StreamController<GyroscopeEvent>.broadcast();
    final safetyService = _QueuedSafetyService();
    final service = FallDetectionSensorService.forTesting(
      accelerometerEvents: () => accelerometer.stream,
      gyroscopeEvents: () => gyroscope.stream,
      now: () => DateTime.utc(2026, 8, 13, 10),
      safetyService: safetyService,
    );
    final emitted = <SafetyEvent>[];
    final subscription = service.detectedEvents.listen(emitted.add);

    await service.start();
    final startedAt = DateTime.utc(2026, 8, 13, 10);
    await _emitFall(
      accelerometer: accelerometer,
      gyroscope: gyroscope,
      startedAt: startedAt,
    );
    await _emitFall(
      accelerometer: accelerometer,
      gyroscope: gyroscope,
      startedAt: startedAt.add(const Duration(seconds: 5)),
    );
    expect(safetyService.sends, hasLength(2));

    const sameBackendEvent = SafetyEvent(
      id: 'same-backend-event',
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'OPEN',
    );
    for (final send in safetyService.sends) {
      send.complete(sameBackendEvent);
    }
    await Future<void>.delayed(Duration.zero);
    await Future<void>.delayed(Duration.zero);

    expect(emitted, [same(sameBackendEvent)]);
    await service.stop();
    await subscription.cancel();
    await accelerometer.close();
    await gyroscope.close();
  });

  test('ignores a fall sample batch delivered long after impact', () async {
    final accelerometer = StreamController<AccelerometerEvent>.broadcast();
    final gyroscope = StreamController<GyroscopeEvent>.broadcast();
    final safetyService = _QueuedSafetyService();
    final service = FallDetectionSensorService.forTesting(
      accelerometerEvents: () => accelerometer.stream,
      gyroscopeEvents: () => gyroscope.stream,
      now: () => DateTime.utc(2026, 8, 13, 10, 0, 10),
      safetyService: safetyService,
    );

    await service.start();
    await _emitFall(
      accelerometer: accelerometer,
      gyroscope: gyroscope,
      startedAt: DateTime.utc(2026, 8, 13, 10),
    );

    expect(safetyService.sends, isEmpty);
    await service.stop();
    await accelerometer.close();
    await gyroscope.close();
  });

  test('detects repeated falls one second after each safe response', () async {
    final accelerometer = StreamController<AccelerometerEvent>.broadcast();
    final gyroscope = StreamController<GyroscopeEvent>.broadcast();
    var eventSequence = 0;
    final safetyService = _FakeSafetyService(
      () async => SafetyEvent(
        id: 'fall-${++eventSequence}',
        eventType: 'SUSPECTED_FALL',
        magnitude: 30,
        status: 'OPEN',
      ),
    );
    final service = FallDetectionSensorService.forTesting(
      accelerometerEvents: () => accelerometer.stream,
      gyroscopeEvents: () => gyroscope.stream,
      now: () => DateTime.utc(2026, 8, 13, 10),
      safetyService: safetyService,
    );
    final emitted = <SafetyEvent>[];
    final subscription = service.detectedEvents.listen(emitted.add);

    await service.start();
    var fallStartedAt = DateTime.utc(2026, 8, 13, 10);
    for (var cycle = 0; cycle < 6; cycle++) {
      await _emitFall(
        accelerometer: accelerometer,
        gyroscope: gyroscope,
        startedAt: fallStartedAt,
      );
      await Future<void>.delayed(Duration.zero);
      expect(emitted, hasLength(cycle + 1));

      final respondedAt = fallStartedAt.add(const Duration(milliseconds: 1300));
      service.rearmAfterAlertResponse(respondedAt);
      // Past the post-response debounce, so each cycle is a new physical fall.
      fallStartedAt = respondedAt.add(const Duration(seconds: 4));
    }

    expect(safetyService.sendCount, 6);
    await service.stop();
    await subscription.cancel();
    await accelerometer.close();
    await gyroscope.close();
  });

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
    'product sensor self-test emits locally without creating a fall event',
    () async {
      final accelerometer = StreamController<AccelerometerEvent>.broadcast();
      final gyroscope = StreamController<GyroscopeEvent>.broadcast();
      var now = DateTime.utc(2026, 8, 4, 10);
      final service = FallDetectionSensorService.forTesting(
        accelerometerEvents: () => accelerometer.stream,
        gyroscopeEvents: () => gyroscope.stream,
        now: () => now,
      );
      final results = <SensorSelfTestResult>[];
      final safetyEvents = <Object>[];
      final resultSubscription = service.sensorSelfTestResults.listen(
        results.add,
      );
      final safetySubscription = service.detectedEvents.listen(
        safetyEvents.add,
      );
      await service.start();

      final startedAt = now;
      for (var index = 0; index <= 20; index++) {
        now = startedAt.add(Duration(milliseconds: index * 20));
        gyroscope.add(GyroscopeEvent(0.05, 0, 0, now));
        accelerometer.add(AccelerometerEvent(9.81, 0, 0, now));
        await Future<void>.delayed(Duration.zero);
      }
      expect(results, isEmpty);

      for (var index = 0; index < 26; index++) {
        now = startedAt.add(Duration(milliseconds: 420 + index * 20));
        gyroscope.add(GyroscopeEvent(2.8, 0, 0, now));
        accelerometer.add(AccelerometerEvent(16.5, 0, 0, now));
        await Future<void>.delayed(Duration.zero);
        if (results.isNotEmpty) break;
      }

      expect(results, hasLength(1));
      expect(results.single.sequence, 1);
      expect(results.single.accelerationMagnitude, closeTo(16.5, 0.001));
      expect(results.single.gyroscopeMagnitude, closeTo(2.8, 0.001));
      expect(safetyEvents, isEmpty);

      await service.stop();
      await resultSubscription.cancel();
      await safetySubscription.cancel();
      await accelerometer.close();
      await gyroscope.close();
    },
  );
}
