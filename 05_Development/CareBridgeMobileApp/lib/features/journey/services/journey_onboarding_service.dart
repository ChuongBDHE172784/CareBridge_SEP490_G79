import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../models/journey_onboarding_model.dart';

class JourneyOnboardingService {
  Future<JourneyOnboardingStatus> getStatus() async {
    final expectedUserId = AuthState.instance.userId;
    final response = await apiGet('/api/v1/journey-onboarding/status');
    _ensureSameAccount(expectedUserId);
    return JourneyOnboardingStatus.fromJson(
      response['data'] as Map<String, dynamic>,
    );
  }

  Future<JourneyOnboardingStatus> submit(
    JourneyOnboardingRequest request,
  ) async {
    final expectedUserId = AuthState.instance.userId;
    final response = await apiPost(
      '/api/v1/journey-onboarding',
      request.toJson(),
    );
    _ensureSameAccount(expectedUserId);
    return JourneyOnboardingStatus.fromJson(
      response['data'] as Map<String, dynamic>,
    );
  }

  void _ensureSameAccount(String? expectedUserId) {
    if (expectedUserId == null || expectedUserId != AuthState.instance.userId) {
      throw StateError('Authenticated account changed during onboarding');
    }
  }
}
