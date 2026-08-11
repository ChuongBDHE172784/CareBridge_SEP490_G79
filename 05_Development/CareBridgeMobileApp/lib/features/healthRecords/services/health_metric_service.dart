import '../../../core/network/api_client.dart';
import '../models/health_metric_model.dart';

class HealthMetricService {
  Future<List<MetricCapability>> getCapabilities(String journeyId) async {
    final data = await apiGet(
      '/api/v1/journeys/$journeyId/metrics/capabilities',
    );
    final body = data['data'] as List<dynamic>? ?? const [];
    return body
        .map((item) => MetricCapability.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<HealthMetricDetail> getMetricDetail(String metricId) async {
    final data = await apiGet('/api/v1/health-metrics/$metricId');
    final body = data['data'] as Map<String, dynamic>;
    return HealthMetricDetail.fromJson(body);
  }

  Future<void> deleteMetric(String metricId) async {
    await apiDelete('/api/v1/health-metrics/$metricId');
  }

  Future<HealthMetricDetail> addMetric(
    String journeyId,
    AddMetricRequest request,
  ) async {
    final data = await apiPost(
      '/api/v1/journeys/$journeyId/metrics',
      request.toJson(),
    );
    final body = data['data'] as Map<String, dynamic>;
    return HealthMetricDetail.fromJson(body);
  }

  Future<HealthMetricDetail> updateMetric(
    String journeyId,
    String metricId,
    UpdateMetricRequest request,
  ) async {
    final data = await apiPut(
      '/api/v1/journeys/$journeyId/metrics/$metricId',
      request.toJson(),
    );
    final body = data['data'] as Map<String, dynamic>;
    return HealthMetricDetail.fromJson({
      'id': body['metricId'],
      ...body,
      'createdAt': body['createdAt'] ?? body['updatedAt'],
    });
  }

  Future<MetricTrend> getMetricTrend({
    required String journeyId,
    required String metricType,
    DateTime? from,
    DateTime? to,
  }) async {
    final now = DateTime.now().toUtc();
    final resolvedFrom = (from ?? now.subtract(const Duration(days: 90)))
        .toUtc();
    final resolvedTo = (to ?? now).toUtc();
    final query = [
      'metricType=${Uri.encodeQueryComponent(_legacyQueryType(metricType))}',
      'from=${Uri.encodeQueryComponent(resolvedFrom.toIso8601String())}',
      'to=${Uri.encodeQueryComponent(resolvedTo.toIso8601String())}',
    ].join('&');
    final data = await apiGet('/api/v1/journeys/$journeyId/metrics?$query');
    final body = data['data'] as Map<String, dynamic>;
    return MetricTrend.fromJson(body);
  }

  String _legacyQueryType(String metricType) {
    switch (metricType) {
      case 'BLOOD_PRESSURE':
        return 'BLOOD_PRESSURE_SYSTOLIC';
      case 'FETAL_MOVEMENT_SESSION':
        return 'FETAL_MOVEMENT_COUNT';
      case 'HEART_RATE':
        return 'MATERNAL_HEART_RATE';
      default:
        return metricType;
    }
  }
}
