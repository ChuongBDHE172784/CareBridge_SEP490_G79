import '../../../core/network/api_client.dart';
import '../models/growth_measurement_model.dart';

class GrowthMeasurementService {
  // UC-237: Fetch growth history
  Future<List<GrowthMeasurement>> getGrowthHistory(
    String babyId, {
    int page = 0,
    int size = 20,
  }) async {
    final response = await apiGet(
      '/api/v1/babies/$babyId/growth-measurements?page=$page&size=$size',
    );
    final content = response['data']?['content'] as List? ?? [];
    return content
        .map((e) => GrowthMeasurement.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  // UC-234: Add growth measurement
  Future<void> addGrowthMeasurement(
    String babyId,
    Map<String, dynamic> payload,
  ) async {
    await apiPost('/api/v1/babies/$babyId/growth-measurements', payload);
  }

  // UC-235: Update growth measurement
  Future<void> updateGrowthMeasurement(
    String babyId,
    String measurementId,
    Map<String, dynamic> payload,
  ) async {
    await apiPatch(
      '/api/v1/babies/$babyId/growth-measurements/$measurementId',
      payload,
    );
  }

  // UC-236: Delete growth measurement
  Future<void> deleteGrowthMeasurement(
    String babyId,
    String measurementId,
  ) async {
    await apiDelete(
      '/api/v1/babies/$babyId/growth-measurements/$measurementId',
    );
  }
}
