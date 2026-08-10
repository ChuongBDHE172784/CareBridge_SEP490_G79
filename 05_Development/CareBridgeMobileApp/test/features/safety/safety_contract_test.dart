import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/privacy/models/privacy_model.dart';
import 'package:untitled/features/safety/models/safety_config_model.dart';
import 'package:untitled/features/safety/services/safety_permission_service.dart';
import 'package:untitled/features/safety/services/safety_service.dart';
import 'package:untitled/features/safety/services/safety_demo_mode.dart';
import 'package:untitled/features/safety/widgets/safety_countdown_sheet.dart';

void main() {
  test('safety config parses additive permission and countdown fields', () {
    final config = SafetyConfig.fromJson({
      'fallDetectionEnabled': true,
      'sensitivityLevel': 'HIGH',
      'emergencyAutoAlert': true,
      'locationSharingEnabled': true,
      'countdownSeconds': 15,
      'sensorPermissionGranted': true,
      'sensorPermissionRecordedAt': '2026-07-22T00:00:00Z',
    });

    expect(config.countdownSeconds, 15);
    expect(config.locationSharingEnabled, isTrue);
    expect(config.sensorPermissionGranted, isTrue);
    expect(config.sensorPermissionRecordedAt, isNotNull);
  });

  test('old safety config remains compatible with safe defaults', () {
    final config = SafetyConfig.fromJson({
      'fallDetectionEnabled': false,
      'sensitivityLevel': 'MEDIUM',
      'emergencyAutoAlert': true,
    });

    expect(config.countdownSeconds, 30);
    expect(config.locationSharingEnabled, isFalse);
    expect(config.sensorPermissionGranted, isFalse);
  });

  test(
    'disable config request persists false without dropping additive state',
    () {
      final request = SafetyService.buildConfigRequest(
        fallDetectionEnabled: false,
        sensitivityLevel: 'MEDIUM',
        emergencyAutoAlert: true,
        locationSharingEnabled: true,
        countdownSeconds: 60,
        sensorPermissionGranted: true,
      );

      expect(request['fallDetectionEnabled'], isFalse);
      expect(request['countdownSeconds'], 60);
      expect(request['locationSharingEnabled'], isTrue);
      expect(request['sensorPermissionGranted'], isTrue);
    },
  );

  test('safety event retains countdown response and emergency identity', () {
    final event = SafetyEvent.fromJson({
      'id': 'event-1',
      'eventType': 'SUSPECTED_FALL',
      'magnitude': 18.4,
      'status': 'ESCALATION_REQUESTED',
      'countdownDeadlineAt': '2026-07-22T00:00:30Z',
      'responseType': 'TIMEOUT',
      'respondedAt': '2026-07-22T00:00:30Z',
      'emergencySessionId': 'session-1',
    });

    expect(event.responseType, 'TIMEOUT');
    expect(event.emergencySessionId, 'session-1');
    expect(event.countdownDeadlineAt, isNotNull);
  });

  test('sensor self-test request serializes stable id and IMU metrics', () {
    final result = SensorSelfTestResult(
      sequence: 7,
      detectedAt: DateTime.utc(2026, 8, 4, 10),
      accelerationMagnitude: 17.2,
      gyroscopeMagnitude: 3.4,
    );

    final request = SafetyService.buildSensorSelfTestRequest(result);

    expect(request['testId'], contains('-7'));
    expect(request['detectedAt'], '2026-08-04T10:00:00.000Z');
    expect(request['accelerationMagnitude'], 17.2);
    expect(request['gyroscopeMagnitude'], 3.4);
  });

  test('expired consent is not treated as active', () {
    final consent = ConsentGrant(
      id: 1,
      dataType: 'LOCATION',
      purpose: 'SHARE',
      recipient: 'CAREBRIDGE_SAFETY',
      expiryAt: DateTime.now().toUtc().subtract(const Duration(minutes: 1)),
    );

    expect(consent.isActive, isFalse);
  });

  test(
    'sensor permission attestation reflects the real probe result',
    () async {
      final granted = SafetyPermissionService(sensorProbe: () async {});
      final denied = SafetyPermissionService(
        sensorProbe: () async => throw StateError('denied'),
      );

      expect(await granted.attestSensorAccess(), isTrue);
      expect(await denied.attestSensorAccess(), isFalse);
    },
  );

  testWidgets('countdown exposes safe and need-help actions', (tester) async {
    SafetyCountdownResult? result;
    final event = SafetyEvent(
      id: 'event-1',
      eventType: 'SUSPECTED_FALL',
      magnitude: 18,
      status: 'OPEN',
      countdownDeadlineAt: DateTime.now().toUtc().add(
        const Duration(seconds: 30),
      ),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Builder(
          builder: (context) => ElevatedButton(
            onPressed: () async {
              result = await showModalBottomSheet<SafetyCountdownResult>(
                context: context,
                isScrollControlled: true,
                builder: (_) => SafetyCountdownSheet(event: event),
              );
            },
            child: const Text('open'),
          ),
        ),
      ),
    );
    await tester.tap(find.text('open'));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('safety-countdown-safe')), findsOneWidget);
    expect(find.byKey(const Key('safety-countdown-help')), findsOneWidget);
    await tester.tap(find.byKey(const Key('safety-countdown-safe')));
    await tester.pumpAndSettle();
    expect(result?.action, SafetyCountdownAction.safe);
  });
}
