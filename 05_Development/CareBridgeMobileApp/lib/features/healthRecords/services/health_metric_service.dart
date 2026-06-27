import '../../../core/network/api_client.dart';
import '../models/health_metric_model.dart';

class HealthMetricService {
  // UC-187: Get metric detail
  Future<HealthMetricDetail> getMetricDetail(String metricId) async {
    final data = await apiGet('/api/v1/health-metrics/$metricId');
    final body = data['data'] as Map<String, dynamic>;
    return HealthMetricDetail.fromJson(body);
  }

  // UC-188: Delete metric (to be called on Xóa)
  Future<void> deleteMetric(String metricId) async {
    await apiDelete('/api/v1/health-metrics/$metricId');
  }
}
