import '../../../core/network/api_client.dart';
import '../models/baby_model.dart';

class BabyService {
  // UC31: Create a new baby profile
  Future<Map<String, dynamic>> createBabyProfile(
    CreateBabyRequest request,
  ) async {
    final data = await apiPost('/api/v1/babies', request.toJson());
    return data['data'] as Map<String, dynamic>;
  }

  // UC192: Get a single baby profile by ID
  Future<BabyProfile> getBabyProfile(String babyId) async {
    final data = await apiGet('/api/v1/babies/$babyId');
    final body = data['data'] as Map<String, dynamic>;
    return BabyProfile.fromJson(body);
  }

  // UC32: List baby profiles for the current user
  Future<List<BabyProfile>> listBabyProfiles() async {
    final data = await apiGet('/api/v1/babies');
    final items = data['data'] as List<dynamic>;
    return items
        .map((e) => BabyProfile.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  // UC-32: Update baby profile (CB-233)
  Future<BabyProfile> updateBabyProfile(
    String babyId,
    UpdateBabyProfileRequest request,
  ) async {
    final data = await apiPut('/api/v1/babies/$babyId', request.toJson());
    final body = data['data'] as Map<String, dynamic>;
    return BabyProfile.fromJson({
      'id': body['id'] ?? body['babyId'] ?? babyId,
      'nickname': body['nickname'],
      'birthDate': body['birthDate'],
      'gender': body['gender'],
      'birthWeightKg': body['birthWeightKg'],
      'birthLengthCm': body['birthLengthCm'],
      'isActive': body['isActive'] ?? true,
    });
  }
}
