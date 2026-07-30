import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/privacy/models/privacy_model.dart';
import 'package:untitled/features/safety/models/safety_config_model.dart';
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
    'never starts background GPS even when LOCATION/SHARE is active',
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
}
