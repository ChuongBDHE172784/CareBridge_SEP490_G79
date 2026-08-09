import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/safety/services/safety_demo_mode.dart';
import 'package:untitled/features/safety/services/imu_fall_detector.dart';

void main() {
  test('compile-time demo flag controls release diagnostics', () {
    const expectedDemoMode = bool.fromEnvironment(
      'EXPECT_SAFETY_DEMO',
      defaultValue: false,
    );

    expect(safetyDemoMode, expectedDemoMode);
    expect(safetyDiagnosticsEnabled, kDebugMode || expectedDemoMode);
    expect(
      safetyDiagnosticsModeLabel,
      expectedDemoMode ? 'CHẾ ĐỘ TRÌNH DIỄN' : 'DEBUG',
    );
  });

  test('sensor self-test result uses a strict JSON-safe contract', () {
    final result = SensorSelfTestResult(
      sequence: 2,
      detectedAt: DateTime.utc(2026, 8, 4, 10),
      accelerationMagnitude: 16.5,
      gyroscopeMagnitude: 2.8,
    );

    final decoded = SensorSelfTestResult.fromJson(result.toJson());
    expect(decoded.sequence, 2);
    expect(decoded.detectedAt, result.detectedAt);
    expect(decoded.accelerationMagnitude, 16.5);
    expect(decoded.gyroscopeMagnitude, 2.8);
    expect(SensorSelfTestResult.tryParse({'sequence': -1}), isNull);
  });

  test('stationary 50 Hz samples and a gentle lift do not trigger', () {
    final detector = SafetySensorSelfTestDetector();
    final startedAt = DateTime.utc(2026, 8, 4);

    ImuSample sample(Duration offset, double acceleration, {double gyro = 0}) {
      final timestamp = startedAt.add(offset);
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

    for (var index = 0; index < 250; index++) {
      expect(
        detector.addSample(
          sample(Duration(milliseconds: index * 20), 9.81, gyro: 0.05),
        ),
        isFalse,
      );
    }
    for (var index = 0; index < 40; index++) {
      expect(
        detector.addSample(
          sample(Duration(milliseconds: 5000 + index * 20), 10.8, gyro: 0.7),
        ),
        isFalse,
      );
    }
    expect(detector.sequence, isZero);
  });

  test('short hard jerk is rejected but a fast long swing triggers once', () {
    final detector = SafetySensorSelfTestDetector();
    final startedAt = DateTime.utc(2026, 8, 4);

    ImuSample sample(Duration offset, double acceleration, {double gyro = 0}) {
      final timestamp = startedAt.add(offset);
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

    for (var index = 0; index <= 20; index++) {
      detector.addSample(
        sample(Duration(milliseconds: index * 20), 9.81, gyro: 0.05),
      );
    }
    for (var index = 0; index < 5; index++) {
      expect(
        detector.addSample(
          sample(Duration(milliseconds: 420 + index * 20), 20, gyro: 3),
        ),
        isFalse,
      );
    }
    expect(detector.sequence, isZero);

    detector.reset();
    for (var index = 0; index <= 20; index++) {
      detector.addSample(
        sample(Duration(milliseconds: index * 20), 9.81, gyro: 0.05),
      );
    }

    var detections = 0;
    for (var index = 0; index < 26; index++) {
      if (detector.addSample(
        sample(Duration(milliseconds: 420 + index * 20), 16.5, gyro: 2.8),
      )) {
        detections++;
      }
    }
    expect(detections, 1);
    expect(detector.sequence, 1);
  });

  test('controlled 50 cm soft-surface drop triggers the sensor self-test', () {
    final detector = SafetySensorSelfTestDetector();
    final startedAt = DateTime.utc(2026, 8, 4);

    ImuSample sample(Duration offset, double acceleration, {double gyro = 0}) {
      final timestamp = startedAt.add(offset);
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

    for (var index = 0; index <= 20; index++) {
      expect(
        detector.addSample(
          sample(Duration(milliseconds: index * 20), 9.81, gyro: 0.05),
        ),
        isFalse,
      );
    }

    for (var index = 0; index < 3; index++) {
      expect(
        detector.addSample(
          sample(Duration(milliseconds: 420 + index * 20), 5.5),
        ),
        isFalse,
      );
    }

    expect(
      detector.addSample(
        sample(const Duration(milliseconds: 480), 9.6, gyro: 0.4),
      ),
      isTrue,
    );
    expect(detector.sequence, 1);
  });
}
