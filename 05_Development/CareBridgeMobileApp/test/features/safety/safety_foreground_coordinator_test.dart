import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/privacy/models/privacy_model.dart';
import 'package:untitled/features/safety/models/safety_config_model.dart';
import 'package:untitled/features/safety/models/imu_diagnostics_model.dart';
import 'package:untitled/features/safety/services/safety_demo_mode.dart';
import 'package:untitled/features/safety/services/safety_foreground_service.dart';

class _FakeForegroundGateway implements SafetyForegroundGateway {
  bool running = false;
  int starts = 0;
  int stops = 0;
  bool? locationSharingAllowed;

  @override
  Future<bool> isRunning() async => running;

  @override
  Future<void> start({required bool locationSharingAllowed}) async {
    running = true;
    starts++;
    this.locationSharingAllowed = locationSharingAllowed;
  }

  @override
  Future<void> stop() async {
    running = false;
    stops++;
  }
}

void main() {
  const enabledConfig = SafetyConfig(
    fallDetectionEnabled: true,
    sensitivityLevel: 'MEDIUM',
    emergencyAutoAlert: true,
    sensorPermissionGranted: true,
  );

  ConsentGrant consent(String dataType, String purpose) => ConsentGrant(
    id: dataType.hashCode,
    dataType: dataType,
    purpose: purpose,
    recipient: 'CAREBRIDGE_SAFETY',
  );

  test('starts only for an authenticated user with sensor consent', () async {
    final gateway = _FakeForegroundGateway();
    final coordinator = SafetyForegroundServiceCoordinator.forTesting(
      gateway: gateway,
      isAuthenticated: () => true,
      loadConfig: () async => enabledConfig,
      loadConsents: () async => [consent('SENSOR_DATA', 'CREATE')],
    );

    await coordinator.reconcile();

    expect(gateway.starts, 1);
    expect(gateway.locationSharingAllowed, isFalse);
  });

  test('stops when sensor consent is absent', () async {
    final gateway = _FakeForegroundGateway()..running = true;
    final coordinator = SafetyForegroundServiceCoordinator.forTesting(
      gateway: gateway,
      isAuthenticated: () => true,
      loadConfig: () async => enabledConfig,
      loadConsents: () async => const [],
    );

    await coordinator.reconcile();

    expect(gateway.stops, 1);
  });

  test(
    'starts location-enabled monitoring only with config opt-in and consent',
    () async {
      final gateway = _FakeForegroundGateway();
      const locationEnabledConfig = SafetyConfig(
        fallDetectionEnabled: true,
        sensitivityLevel: 'MEDIUM',
        emergencyAutoAlert: true,
        locationSharingEnabled: true,
        sensorPermissionGranted: true,
      );
      final coordinator = SafetyForegroundServiceCoordinator.forTesting(
        gateway: gateway,
        isAuthenticated: () => true,
        loadConfig: () async => locationEnabledConfig,
        loadConsents: () async => [
          consent('SENSOR_DATA', 'CREATE'),
          consent('LOCATION', 'SHARE'),
        ],
      );

      await coordinator.reconcile();

      expect(gateway.locationSharingAllowed, isTrue);
    },
  );

  test(
    'does not start location monitoring without explicit config opt-in',
    () async {
      final gateway = _FakeForegroundGateway();
      final coordinator = SafetyForegroundServiceCoordinator.forTesting(
        gateway: gateway,
        isAuthenticated: () => true,
        loadConfig: () async => enabledConfig,
        loadConsents: () async => [
          consent('SENSOR_DATA', 'CREATE'),
          consent('LOCATION', 'SHARE'),
        ],
      );

      await coordinator.reconcile();

      expect(gateway.locationSharingAllowed, isFalse);
    },
  );

  test('does not start when logout happens during reconciliation', () async {
    final gateway = _FakeForegroundGateway();
    var authenticated = true;
    final configCompleter = Completer<SafetyConfig>();
    final coordinator = SafetyForegroundServiceCoordinator.forTesting(
      gateway: gateway,
      isAuthenticated: () => authenticated,
      loadConfig: () => configCompleter.future,
      loadConsents: () async => [consent('SENSOR_DATA', 'CREATE')],
    );

    final reconciliation = coordinator.reconcile();
    authenticated = false;
    configCompleter.complete(enabledConfig);
    await reconciliation;

    expect(gateway.starts, isZero);
    expect(coordinator.isRunning, isFalse);
  });

  test('fails closed when config or consent lookup fails', () async {
    final gateway = _FakeForegroundGateway()..running = true;
    final coordinator = SafetyForegroundServiceCoordinator.forTesting(
      gateway: gateway,
      isAuthenticated: () => true,
      loadConfig: () async => throw StateError('offline'),
      loadConsents: () async => const [],
    );

    await coordinator.reconcile();

    expect(gateway.stops, 1);
  });

  test('does not start on an unsupported platform', () async {
    final gateway = _FakeForegroundGateway();
    final coordinator = SafetyForegroundServiceCoordinator.forTesting(
      gateway: gateway,
      isAuthenticated: () => true,
      loadConfig: () async => enabledConfig,
      loadConsents: () async => [consent('SENSOR_DATA', 'CREATE')],
      platformSupported: false,
    );

    expect(coordinator.isSupported, isFalse);
    expect(await coordinator.requestRequiredPermissions(), isFalse);
    await coordinator.reconcile();

    expect(gateway.starts, isZero);
    expect(coordinator.isRunning, isFalse);
  });

  test('supported non-Android platforms skip Android permissions', () async {
    var androidPermissionRequests = 0;
    final coordinator = SafetyForegroundServiceCoordinator.forTesting(
      gateway: _FakeForegroundGateway(),
      isAuthenticated: () => true,
      loadConfig: () async => enabledConfig,
      loadConsents: () async => [consent('SENSOR_DATA', 'CREATE')],
      platformAndroid: false,
      requestAndroidPermissions: () async {
        androidPermissionRequests++;
        return false;
      },
    );

    expect(await coordinator.requestRequiredPermissions(), isTrue);
    expect(androidPermissionRequests, isZero);
  });

  test(
    'Android retains notification and battery permission handling',
    () async {
      var androidPermissionRequests = 0;
      final coordinator = SafetyForegroundServiceCoordinator.forTesting(
        gateway: _FakeForegroundGateway(),
        isAuthenticated: () => true,
        loadConfig: () async => enabledConfig,
        loadConsents: () async => [consent('SENSOR_DATA', 'CREATE')],
        requestAndroidPermissions: () async {
          androidPermissionRequests++;
          return true;
        },
      );

      expect(await coordinator.requestRequiredPermissions(), isTrue);
      expect(androidPermissionRequests, 1);
    },
  );

  test('forwards valid diagnostics and ignores malformed payloads', () async {
    final coordinator = SafetyForegroundServiceCoordinator.forTesting(
      gateway: _FakeForegroundGateway(),
      isAuthenticated: () => true,
      loadConfig: () async => enabledConfig,
      loadConsents: () async => [consent('SENSOR_DATA', 'CREATE')],
    );
    final snapshots = <ImuDiagnosticsSnapshot>[];
    final subscription = coordinator.diagnostics.listen(snapshots.add);

    coordinator.handleTaskDataForTesting({
      'type': 'imu_diagnostics',
      'snapshot': ImuDiagnosticsSnapshot.awaiting(
        generation: 2,
        capturedAt: DateTime.utc(2026, 8, 4),
      ).toJson(),
    });
    coordinator.handleTaskDataForTesting({
      'type': 'imu_diagnostics',
      'snapshot': {'generation': 'bad'},
    });
    await Future<void>.delayed(Duration.zero);

    expect(snapshots, hasLength(1));
    expect(snapshots.single.state, ImuSamplingState.awaitingSamples);
    await subscription.cancel();
  });

  test('forwards product sensor self-test results independently', () async {
    final coordinator = SafetyForegroundServiceCoordinator.forTesting(
      gateway: _FakeForegroundGateway(),
      isAuthenticated: () => true,
      loadConfig: () async => enabledConfig,
      loadConsents: () async => [consent('SENSOR_DATA', 'CREATE')],
    );
    final results = <SensorSelfTestResult>[];
    final subscription = coordinator.sensorSelfTestResults.listen(results.add);
    final result = SensorSelfTestResult(
      sequence: 1,
      detectedAt: DateTime.utc(2026, 8, 4),
      accelerationMagnitude: 16.5,
      gyroscopeMagnitude: 2.8,
    );

    coordinator.handleTaskDataForTesting({
      'type': 'sensor_self_test_result',
      'result': result.toJson(),
    });
    coordinator.handleTaskDataForTesting({
      'type': 'sensor_self_test_result',
      'result': {'sequence': -1},
    });
    await Future<void>.delayed(Duration.zero);

    expect(results, hasLength(1));
    expect(results.single.sequence, 1);
    expect(results.single.accelerationMagnitude, 16.5);
    await subscription.cancel();
  });

  test('ignores diagnostics from an older task generation', () async {
    final coordinator = SafetyForegroundServiceCoordinator.forTesting(
      gateway: _FakeForegroundGateway(),
      isAuthenticated: () => true,
      loadConfig: () async => enabledConfig,
      loadConsents: () async => [consent('SENSOR_DATA', 'CREATE')],
    );
    final snapshots = <ImuDiagnosticsSnapshot>[];
    final subscription = coordinator.diagnostics.listen(snapshots.add);
    final capturedAt = DateTime.utc(2026, 8, 4);

    coordinator.handleTaskDataForTesting({
      'type': 'imu_diagnostics',
      'snapshot': ImuDiagnosticsSnapshot.awaiting(
        generation: 3,
        capturedAt: capturedAt,
      ).toJson(),
    });
    coordinator.handleTaskDataForTesting({
      'type': 'imu_diagnostics',
      'snapshot': ImuDiagnosticsSnapshot.stopped(
        generation: 2,
        capturedAt: capturedAt,
      ).toJson(),
    });
    await Future<void>.delayed(Duration.zero);

    expect(snapshots, hasLength(1));
    expect(snapshots.single.generation, 3);
    await subscription.cancel();
  });

  test('ignores reordered diagnostics within the same generation', () async {
    final coordinator = SafetyForegroundServiceCoordinator.forTesting(
      gateway: _FakeForegroundGateway(),
      isAuthenticated: () => true,
      loadConfig: () async => enabledConfig,
      loadConsents: () async => [consent('SENSOR_DATA', 'CREATE')],
    );
    final snapshots = <ImuDiagnosticsSnapshot>[];
    final subscription = coordinator.diagnostics.listen(snapshots.add);
    final newer = DateTime.utc(2026, 8, 4, 10);

    coordinator.handleTaskDataForTesting({
      'type': 'imu_diagnostics',
      'snapshot': ImuDiagnosticsSnapshot(
        generation: 4,
        state: ImuSamplingState.sampling,
        capturedAt: newer,
      ).toJson(),
    });
    coordinator.handleTaskDataForTesting({
      'type': 'imu_diagnostics',
      'snapshot': ImuDiagnosticsSnapshot(
        generation: 4,
        state: ImuSamplingState.error,
        capturedAt: newer.subtract(const Duration(seconds: 1)),
      ).toJson(),
    });
    await Future<void>.delayed(Duration.zero);

    expect(snapshots, hasLength(1));
    expect(snapshots.single.state, ImuSamplingState.sampling);
    await subscription.cancel();
  });

  test(
    'reconcile never overwrites fresh sampling with coordinator state',
    () async {
      final gateway = _FakeForegroundGateway()..running = true;
      final coordinator = SafetyForegroundServiceCoordinator.forTesting(
        gateway: gateway,
        isAuthenticated: () => true,
        loadConfig: () async => enabledConfig,
        loadConsents: () async => [consent('SENSOR_DATA', 'CREATE')],
      );
      final snapshots = <ImuDiagnosticsSnapshot>[];
      final subscription = coordinator.diagnostics.listen(snapshots.add);
      coordinator.handleTaskDataForTesting({
        'type': 'imu_diagnostics',
        'snapshot': ImuDiagnosticsSnapshot(
          generation: 5,
          state: ImuSamplingState.sampling,
          capturedAt: DateTime.now().toUtc(),
        ).toJson(),
      });

      await coordinator.reconcile();
      await Future<void>.delayed(Duration.zero);

      expect(snapshots, hasLength(1));
      expect(snapshots.single.state, ImuSamplingState.sampling);
      await subscription.cancel();
    },
  );

  test('stopped task diagnostics clear cached running state', () async {
    final gateway = _FakeForegroundGateway()..running = true;
    final coordinator = SafetyForegroundServiceCoordinator.forTesting(
      gateway: gateway,
      isAuthenticated: () => true,
      loadConfig: () async => enabledConfig,
      loadConsents: () async => [consent('SENSOR_DATA', 'CREATE')],
    );
    await coordinator.reconcile();
    expect(coordinator.isRunning, isTrue);

    coordinator.handleTaskDataForTesting({
      'type': 'imu_diagnostics',
      'snapshot': ImuDiagnosticsSnapshot.stopped(
        generation: 8,
        capturedAt: DateTime.now().toUtc(),
      ).toJson(),
    });

    expect(coordinator.isRunning, isFalse);
  });
}
