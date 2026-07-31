import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/healthRecords/models/health_metric_model.dart';
import 'package:untitled/features/healthRecords/services/health_metric_service.dart';

/// Destructive live-API evidence harness for the manual maternal metric P0 flow.
///
/// Run only against a disposable local database and a dedicated Android device:
///
/// `flutter test integration_test/maternal_metric_p0_e2e_test.dart`
/// ` -d <device> --dart-define-from-file=<gitignored-json>`
///
/// Required defines: API_BASE_URL, MATERNAL_METRIC_E2E,
/// MATERNAL_METRIC_E2E_ENVIRONMENT, MATERNAL_METRIC_E2E_DEVICE_ACK,
/// MATERNAL_METRIC_ACCESS_TOKEN, MATERNAL_METRIC_REFRESH_TOKEN,
/// MATERNAL_METRIC_USER_ID, and MATERNAL_METRIC_JOURNEY_ID.
///
/// Dart defines are embedded in the generated test artifact. Use only a
/// short-lived disposable session and remove the artifact after verification.
const _apiBacked =
    String.fromEnvironment('MATERNAL_METRIC_E2E', defaultValue: 'false') ==
    'true';
const _environmentMarker = String.fromEnvironment(
  'MATERNAL_METRIC_E2E_ENVIRONMENT',
);
const _deviceAcknowledgement = String.fromEnvironment(
  'MATERNAL_METRIC_E2E_DEVICE_ACK',
);
const _accessToken = String.fromEnvironment('MATERNAL_METRIC_ACCESS_TOKEN');
const _refreshToken = String.fromEnvironment('MATERNAL_METRIC_REFRESH_TOKEN');
const _userId = String.fromEnvironment('MATERNAL_METRIC_USER_ID');
const _journeyId = String.fromEnvironment('MATERNAL_METRIC_JOURNEY_ID');

const _overallTimeout = Duration(minutes: 4);
const _disclaimer = 'Đây là dữ liệu theo dõi, không phải chẩn đoán y khoa.';

class _MetricCase {
  const _MetricCase({
    required this.code,
    required this.addRequest,
    required this.updateRequest,
    required this.updatedPrimary,
    this.updatedSecondary,
    this.expectedContext = const {},
  });

  final String code;
  final AddMetricRequest addRequest;
  final UpdateMetricRequest updateRequest;
  final double updatedPrimary;
  final double? updatedSecondary;
  final Map<String, dynamic> expectedContext;
}

void _validatePreflight() {
  expect(
    _environmentMarker,
    'DISPOSABLE_LOCAL_POSTGRESQL',
    reason:
        'Smoke testing must target the disposable local PostgreSQL database.',
  );
  expect(
    _deviceAcknowledgement,
    'PHYSICAL_ANDROID_DEVICE_CONFIRMED',
    reason: 'A dedicated physical Android test device must be acknowledged.',
  );
  expect(apiBaseUrl, 'http://127.0.0.1:8080');
  expect(_accessToken, isNotEmpty);
  expect(_refreshToken, isNotEmpty);
  expect(_userId, isNotEmpty);
  expect(_journeyId, isNotEmpty);
}

Future<void> _waitForBackend() async {
  Object? lastError;
  for (var attempt = 0; attempt < 10; attempt++) {
    try {
      await apiGet('/api/v1/auth/profile').timeout(const Duration(seconds: 5));
      return;
    } catch (error) {
      lastError = error;
      await Future<void>.delayed(const Duration(milliseconds: 300));
    }
  }
  throw StateError(
    'Physical device could not reach the local backend: $lastError',
  );
}

void _assertValue(
  HealthMetricDetail metric,
  double primary,
  double? secondary,
) {
  expect(metric.valueNumeric, closeTo(primary, 0.001));
  if (secondary == null) {
    expect(metric.valueSecondary, isNull);
  } else {
    expect(metric.valueSecondary, closeTo(secondary, 0.001));
  }
  expect(metric.disclaimer, _disclaimer);
  expect(metric.definitionVersion, 1);
}

Future<void> _runMetricCase(
  HealthMetricService service,
  _MetricCase metricCase,
  List<String> cleanupIds,
) async {
  final added = await service.addMetric(_journeyId, metricCase.addRequest);
  cleanupIds.add(added.id);
  expect(added.metricCode, metricCase.code);
  expect(added.journeyId, _journeyId);
  _assertValue(
    added,
    metricCase.addRequest.valueNumeric,
    metricCase.addRequest.valueSecondary,
  );

  final detail = await service.getMetricDetail(added.id);
  expect(detail.id, added.id);
  expect(detail.metricCode, metricCase.code);

  final updated = await service.updateMetric(
    _journeyId,
    added.id,
    metricCase.updateRequest,
  );
  expect(updated.metricCode, metricCase.code);
  _assertValue(updated, metricCase.updatedPrimary, metricCase.updatedSecondary);
  for (final entry in metricCase.expectedContext.entries) {
    expect(updated.context[entry.key], entry.value);
  }

  final trend = await service.getMetricTrend(
    journeyId: _journeyId,
    metricType: metricCase.code,
    from: DateTime.now().toUtc().subtract(const Duration(days: 1)),
    to: DateTime.now().toUtc().add(const Duration(minutes: 5)),
  );
  expect(trend.metricType, metricCase.code);
  expect(trend.disclaimer, _disclaimer);
  expect(trend.dataPoints.any((point) => point.metricId == added.id), isTrue);

  await service.deleteMetric(added.id);
  cleanupIds.remove(added.id);
  await expectLater(
    service.getMetricDetail(added.id),
    throwsA(isA<ApiException>()),
  );
}

Future<void> _runScenario() async {
  _validatePreflight();
  await AuthState.instance.setTokens(
    accessToken: _accessToken,
    refreshToken: _refreshToken,
    userId: _userId,
    role: 'MOTHER',
  );
  await _waitForBackend();

  final service = HealthMetricService();
  final capabilities = await service.getCapabilities(_journeyId);
  expect(capabilities.map((capability) => capability.metricCode).toSet(), {
    'WEIGHT',
    'BLOOD_PRESSURE',
    'BLOOD_GLUCOSE',
    'FETAL_MOVEMENT_SESSION',
  });

  final now = DateTime.now().toUtc().subtract(const Duration(minutes: 2));
  final sessionStart = now.subtract(const Duration(minutes: 30));
  final sessionEnd = now;
  final cleanupIds = <String>[];
  final cases = [
    _MetricCase(
      code: 'WEIGHT',
      addRequest: AddMetricRequest(
        metricType: 'WEIGHT',
        valueNumeric: 62.4,
        unit: 'kg',
        measuredAt: now,
        note: '[E2E] weight add',
      ),
      updateRequest: const UpdateMetricRequest(
        valueNumeric: 62.8,
        note: '[E2E] weight update',
      ),
      updatedPrimary: 62.8,
    ),
    _MetricCase(
      code: 'BLOOD_PRESSURE',
      addRequest: AddMetricRequest(
        metricType: 'BLOOD_PRESSURE',
        valueNumeric: 118,
        valueSecondary: 76,
        unit: 'mmHg',
        measuredAt: now.add(const Duration(seconds: 1)),
        note: '[E2E] blood pressure add',
      ),
      updateRequest: const UpdateMetricRequest(
        valueNumeric: 120,
        valueSecondary: 78,
        note: '[E2E] blood pressure update',
      ),
      updatedPrimary: 120,
      updatedSecondary: 78,
    ),
    _MetricCase(
      code: 'BLOOD_GLUCOSE',
      addRequest: AddMetricRequest(
        metricType: 'BLOOD_GLUCOSE',
        valueNumeric: 91,
        unit: 'mg/dL',
        measuredAt: now.add(const Duration(seconds: 2)),
        note: '[E2E] blood glucose add',
        context: const {'measurementContext': 'FASTING'},
      ),
      updateRequest: const UpdateMetricRequest(
        valueNumeric: 94,
        note: '[E2E] blood glucose update',
        context: {'measurementContext': 'FASTING'},
      ),
      updatedPrimary: 94,
      expectedContext: const {'measurementContext': 'FASTING'},
    ),
    _MetricCase(
      code: 'FETAL_MOVEMENT_SESSION',
      addRequest: AddMetricRequest(
        metricType: 'FETAL_MOVEMENT_SESSION',
        valueNumeric: 8,
        unit: 'count',
        measuredAt: now.add(const Duration(seconds: 3)),
        note: '[E2E] fetal movement add',
        periodStart: sessionStart,
        periodEnd: sessionEnd,
        context: const {
          'protocolCode': 'COUNT_TO_10',
          'completionStatus': 'COMPLETED',
          'gestationalAgeSnapshot': '23W0D',
        },
      ),
      updateRequest: UpdateMetricRequest(
        valueNumeric: 10,
        note: '[E2E] fetal movement update',
        periodStart: sessionStart,
        periodEnd: sessionEnd.add(const Duration(minutes: 5)),
        context: const {
          'protocolCode': 'COUNT_TO_10',
          'completionStatus': 'COMPLETED',
          'gestationalAgeSnapshot': '23W0D',
        },
      ),
      updatedPrimary: 10,
      expectedContext: const {
        'protocolCode': 'COUNT_TO_10',
        'completionStatus': 'COMPLETED',
        'gestationalAgeSnapshot': '23W0D',
      },
    ),
  ];

  try {
    for (final metricCase in cases) {
      await _runMetricCase(service, metricCase, cleanupIds);
    }
  } finally {
    for (final metricId in cleanupIds.reversed) {
      try {
        await service.deleteMetric(metricId);
      } catch (_) {
        // Best-effort cleanup; PostgreSQL verification catches leftovers.
      }
    }
    await AuthState.instance.clear();
  }
}

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets(
    'manual maternal P0 capability/add/detail/update/trend/delete on physical Android',
    (_) => _runScenario().timeout(
      _overallTimeout,
      onTimeout: () => throw TimeoutException(
        'Maternal metric P0 smoke scenario exceeded $_overallTimeout.',
        _overallTimeout,
      ),
    ),
    skip: !_apiBacked,
  );
}
