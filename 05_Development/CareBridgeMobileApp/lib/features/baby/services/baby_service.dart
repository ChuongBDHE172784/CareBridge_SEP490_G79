import '../../../core/network/api_client.dart';
import '../models/baby_model.dart';

class BabyService {
  // UC31: Create a new baby profile
  Future<Map<String, dynamic>> createBabyProfile(CreateBabyRequest request) async {
    final data = await apiPost('/api/v1/babies', request.toJson());
    return data['data'] as Map<String, dynamic>;
  }

  // UC192: Get a single baby profile by ID
  Future<BabyProfile> getBabyProfile(String babyId) async {
    final data = await apiGet('/api/v1/babies/$babyId');
    final body = data['data'] as Map<String, dynamic>;
    return BabyProfile.fromJson(body);
  }

  // TODO: Replace with GET /api/v1/babies when list endpoint is available (UC-31/32/33)
  Future<List<BabyProfile>> listBabyProfiles() async {
    return [
      BabyProfile(
        id: 'mock-1',
        nickname: 'Bé Đậu',
        birthDate: DateTime(2022, 3, 1),
        gender: BabyGender.male,
        isActive: true,
      ),
      BabyProfile(
        id: 'mock-2',
        nickname: 'Bé Dâu',
        birthDate: DateTime(2024, 9, 1),
        gender: BabyGender.female,
        isActive: false,
      ),
    ];
  }
}
