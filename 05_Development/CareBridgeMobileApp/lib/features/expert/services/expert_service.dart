import '../../../../core/network/api_client.dart';
import '../../../../core/auth/auth_state.dart';

class ExpertService {
  static final ExpertService instance = ExpertService._();
  ExpertService._();

  Future<Map<String, dynamic>> createProfile({
    required String displayName,
    String? bio,
    required List<String> specialties,
    required int yearsOfExperience,
    required int consultationFeeVnd,
    required List<String> consultationModalities,
  }) async {
    final body = <String, dynamic>{
      'displayName': displayName,
      'specialties': specialties,
      'yearsOfExperience': yearsOfExperience,
      'consultationFeeVnd': consultationFeeVnd,
      'consultationModalities': consultationModalities,
    };
    if (bio != null && bio.isNotEmpty) body['bio'] = bio;
    final res = await apiPost('/api/v1/expert-profiles', body);
    return res['data'] as Map<String, dynamic>;
  }
}
