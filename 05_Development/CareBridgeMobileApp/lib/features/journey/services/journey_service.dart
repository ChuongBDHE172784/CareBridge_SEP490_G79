import '../../../core/network/api_client.dart';
import '../models/journey_model.dart';

class JourneyService {
  Future<CreateJourneyResponse> createJourney(CreateJourneyRequest request) async {
    final data = await apiPost('/api/v1/journeys', request.toJson());
    final body = data['data'] as Map<String, dynamic>;
    return CreateJourneyResponse.fromJson(body);
  }

  // UC-24: Get dashboard data for Home and Journey screens
  Future<JourneyDashboard> getDashboard() async {
    final data = await apiGet('/api/v1/journeys/me/dashboard');
    return JourneyDashboard.fromJson(data['data'] as Map<String, dynamic>);
  }
}
