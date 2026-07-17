import '../../../core/network/api_client.dart';

class BabyCareCompositeService {
  Future<Map<String, dynamic>> getOverview(String babyId) async =>
      (await apiGet('/api/v1/babies/$babyId/care-overview'))['data'] as Map<String, dynamic>;

  Future<Map<String, dynamic>> getTimeline(String babyId) async =>
      (await apiGet('/api/v1/babies/$babyId/care-timeline?size=50'))['data'] as Map<String, dynamic>;

  Future<Map<String, dynamic>> getPreparation(String babyId) async =>
      (await apiGet('/api/v1/babies/$babyId/appointment-preparation-summary'))['data'] as Map<String, dynamic>;
}
