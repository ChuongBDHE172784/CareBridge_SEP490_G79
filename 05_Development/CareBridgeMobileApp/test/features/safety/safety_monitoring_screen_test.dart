import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/safety/models/imu_diagnostics_model.dart';
import 'package:untitled/features/safety/models/safety_config_model.dart';
import 'package:untitled/features/safety/screens/safety_monitoring_screen.dart';
import 'package:untitled/features/safety/widgets/safety_countdown_sheet.dart';

void main() {
  test('every simulated outcome performs zero external writes', () async {
    for (final result in <SafetyCountdownResult>[
      const SafetyCountdownResult.safe(),
      const SafetyCountdownResult.falsePositive(
        reasonCode: 'EXERCISE',
        reason: 'Exercise',
      ),
      const SafetyCountdownResult.help(),
      const SafetyCountdownResult.timeout(),
    ]) {
      var writes = 0;
      await dispatchSafetyCountdownResult(
        result: result,
        simulated: true,
        onSafe: () async => writes++,
        onFalsePositive: (_, _) async => writes++,
        onEmergency: () async => writes++,
      );
      expect(writes, isZero, reason: '${result.action} must remain local');
    }
  });

  test('real outcomes retain the production callbacks', () async {
    var safeWrites = 0;
    await dispatchSafetyCountdownResult(
      result: const SafetyCountdownResult.safe(),
      simulated: false,
      onSafe: () async => safeWrites++,
      onFalsePositive: (_, _) async {},
      onEmergency: () async {},
    );
    expect(safeWrites, 1);
  });

  test('selects a queued real OPEN event after local simulation closes', () {
    const simulationId = 'local-simulation-1';
    const resolved = SafetyEvent(
      id: 'resolved',
      eventType: 'SUSPECTED_FALL',
      magnitude: 20,
      status: 'RESOLVED',
    );
    const simulation = SafetyEvent(
      id: simulationId,
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'OPEN',
    );
    const realEvent = SafetyEvent(
      id: 'server-event-1',
      eventType: 'SUSPECTED_FALL',
      magnitude: 31,
      status: 'OPEN',
    );

    final next = selectNextOpenSafetyEvent(const [
      resolved,
      simulation,
      realEvent,
    ], excludingId: simulationId);

    expect(next, same(realEvent));
  });

  test(
    'dedicated queue preserves a real event across API list replacement',
    () {
      const realEvent = SafetyEvent(
        id: 'server-event-2',
        eventType: 'SUSPECTED_FALL',
        magnitude: 32,
        status: 'OPEN',
      );
      final queue = SafetyRealEventQueue()..enqueue(realEvent);

      const apiReloadedEvents = <SafetyEvent>[];
      expect(apiReloadedEvents, isEmpty);
      expect(
        queue.takeNext(excludingId: 'local-simulation-2'),
        same(realEvent),
      );
    },
  );

  test('simulation requires a fresh sampling diagnostic', () {
    const config = SafetyConfig(
      fallDetectionEnabled: true,
      sensitivityLevel: 'MEDIUM',
      emergencyAutoAlert: true,
      sensorPermissionGranted: true,
    );
    final now = DateTime.utc(2026, 8, 4, 10);
    final fresh = ImuDiagnosticsSnapshot(
      generation: 1,
      state: ImuSamplingState.sampling,
      capturedAt: now.subtract(const Duration(milliseconds: 500)),
    );
    final stale = ImuDiagnosticsSnapshot(
      generation: 1,
      state: ImuSamplingState.sampling,
      capturedAt: now.subtract(const Duration(seconds: 3)),
    );

    expect(
      isSafeFallSimulationEligible(
        config: config,
        coordinatorRunning: true,
        diagnostics: fresh,
        now: now,
      ),
      isTrue,
    );
    expect(
      isSafeFallSimulationEligible(
        config: config,
        coordinatorRunning: true,
        diagnostics: stale,
        now: now,
      ),
      isFalse,
    );
  });

  test('product sensor self-test requires enabled and running IMU', () {
    const enabled = SafetyConfig(
      fallDetectionEnabled: true,
      sensitivityLevel: 'MEDIUM',
      emergencyAutoAlert: true,
      sensorPermissionGranted: true,
    );
    const missingPermission = SafetyConfig(
      fallDetectionEnabled: true,
      sensitivityLevel: 'MEDIUM',
      emergencyAutoAlert: true,
      sensorPermissionGranted: false,
    );

    expect(
      isSensorSelfTestEligible(config: enabled, coordinatorRunning: true),
      isTrue,
    );
    expect(
      isSensorSelfTestEligible(
        config: missingPermission,
        coordinatorRunning: true,
      ),
      isFalse,
    );
    expect(
      isSensorSelfTestEligible(config: enabled, coordinatorRunning: false),
      isFalse,
    );
  });

  test('sensor self-test accepts only results captured after arming', () {
    final armedAt = DateTime.utc(2026, 8, 4, 10);

    expect(
      shouldAcceptSensorSelfTestResult(
        armed: true,
        armedAt: armedAt,
        detectedAt: armedAt.add(const Duration(milliseconds: 20)),
      ),
      isTrue,
    );
    expect(
      shouldAcceptSensorSelfTestResult(
        armed: true,
        armedAt: armedAt,
        detectedAt: armedAt.subtract(const Duration(milliseconds: 1)),
      ),
      isFalse,
    );
    expect(
      shouldAcceptSensorSelfTestResult(
        armed: false,
        armedAt: armedAt,
        detectedAt: armedAt.add(const Duration(milliseconds: 20)),
      ),
      isFalse,
    );
  });
}
