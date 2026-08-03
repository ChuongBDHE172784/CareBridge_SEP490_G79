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

  test('deliberate downward swing is easy to trigger but cooldown-limited', () {
    final detector = SafetyDemoGestureDetector();
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

    expect(detector.addSample(sample(Duration.zero, 9.81)), isFalse);
    expect(
      detector.addSample(sample(const Duration(milliseconds: 100), 6)),
      isTrue,
    );
    expect(detector.sequence, 1);
    expect(
      detector.addSample(
        sample(const Duration(milliseconds: 250), 15, gyro: 2),
      ),
      isFalse,
    );
    expect(
      detector.addSample(
        sample(const Duration(milliseconds: 450), 9.81, gyro: 1),
      ),
      isTrue,
    );
    expect(detector.sequence, 2);

    detector.reset();
    expect(detector.sequence, isZero);
  });
}
