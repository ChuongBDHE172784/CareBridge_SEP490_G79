import '../../../core/network/api_client.dart';
import '../models/baby_care_composite_model.dart';

class BabyCareCompositeService {
  Future<BabyCareOverview> getOverview(String babyId) async {
    final data = await apiGet('/api/v1/babies/$babyId/care-overview');
    return BabyCareOverview.fromJson(_dataMap(data));
  }

  Future<BabyCareTimeline> getTimeline(String babyId) async {
    final data = await apiGet('/api/v1/babies/$babyId/care-timeline?size=50');
    return BabyCareTimeline.fromJson(_dataMap(data));
  }

  Future<AppointmentPreparationSummary> getPreparation(String babyId) async {
    final data = await apiGet(
      '/api/v1/babies/$babyId/appointment-preparation-summary',
    );
    return AppointmentPreparationSummary.fromJson(_dataMap(data));
  }

  Map<String, dynamic> _dataMap(Map<String, dynamic> response) {
    final value = response['data'];
    if (value is! Map) {
      throw const FormatException('Invalid baby care composite response');
    }
    return Map<String, dynamic>.from(value);
  }
}
