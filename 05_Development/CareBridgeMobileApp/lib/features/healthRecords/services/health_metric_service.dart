import '../../../core/network/api_client.dart';
import '../models/health_metric_model.dart';

class HealthMetricService {
  // UC-187: Get metric detail
  Future<HealthMetricDetail> getMetricDetail(String metricId) async {
    final data = await apiGet('/api/v1/health-metrics/$metricId');
    final body = data['data'] as Map<String, dynamic>;
    return HealthMetricDetail.fromJson(body);
  }

  // UC-188: Delete metric
  Future<void> deleteMetric(String metricId) async {
    await apiDelete('/api/v1/health-metrics/$metricId');
  }

  // UC-25: Add maternal health metric
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

  // UC-26: Update maternal health metric (BR-METRIC-012: 24-hour window from createdAt)
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
      'journeyId': body['journeyId'],
      'metricType': body['metricType'],
      'valueNumeric': body['valueNumeric'],
      'valueSecondary': body['valueSecondary'],
      'unit': body['unit'],
      'measuredAt': body['measuredAt'],
      'sourceType': body['sourceType'],
      'note': body['note'],
      'createdAt': body['createdAt'] ?? body['updatedAt'],
    });
  }

  // UC-27: Get metric trend for a journey
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
      'metricType=$metricType',
      'from=${resolvedFrom.toIso8601String()}',
      'to=${resolvedTo.toIso8601String()}',
    ].join('&');
    final data = await apiGet('/api/v1/journeys/$journeyId/metrics?$query');
    final body = data['data'] as Map<String, dynamic>;
    return MetricTrend.fromJson(body);
  }
}
