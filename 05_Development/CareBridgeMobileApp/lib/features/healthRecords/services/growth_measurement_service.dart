import '../../../core/network/api_client.dart';
import '../models/growth_measurement_model.dart';

class GrowthMeasurementService {
  static const int maxHistoryPageSize = 50;

  Future<Map<String, dynamic>> getGrowthChart(String babyId) async {
    final response = await apiGet('/api/v1/babies/$babyId/growth-chart');
    return response['data'] as Map<String, dynamic>;
  }

  // UC-237: Fetch growth history
  Future<List<GrowthMeasurement>> getGrowthHistory(
    String babyId, {
    int page = 0,
    int size = 20,
  }) async {
    if (page < 0) {
      throw ArgumentError.value(page, 'page', 'must be zero or greater');
    }
    if (size < 1 || size > maxHistoryPageSize) {
      throw ArgumentError.value(
        size,
        'size',
        'must be between 1 and $maxHistoryPageSize',
      );
    }
    final response = await apiGet(
      '/api/v1/babies/$babyId/growth-measurements?page=$page&size=$size',
    );
    final content = response['data']?['content'] as List? ?? [];
    return content
        .map((e) => GrowthMeasurement.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// Loads up to 100 recent measurements without exceeding the API page limit.
  Future<List<GrowthMeasurement>> getGrowthHistoryForTrend(
    String babyId,
  ) async {
    final firstPage = await getGrowthHistory(babyId, size: maxHistoryPageSize);
    if (firstPage.length < maxHistoryPageSize) return firstPage;

    final secondPage = await getGrowthHistory(
      babyId,
      page: 1,
      size: maxHistoryPageSize,
    );
    return [...firstPage, ...secondPage];
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
