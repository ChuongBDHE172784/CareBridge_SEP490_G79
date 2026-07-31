import '../../../core/network/api_client.dart';
import '../models/baby_model.dart';

typedef BabyGet =
    Future<dynamic> Function(String path, {Map<String, dynamic>? queryParams});
typedef BabyPut =
    Future<dynamic> Function(String path, Map<String, dynamic> body);
typedef BabyPost =
    Future<dynamic> Function(
      String path,
      Map<String, dynamic> body, {
      String? token,
      String? expectedAccountId,
    });

Future<dynamic> _defaultBabyPost(
  String path,
  Map<String, dynamic> body, {
  String? token,
  String? expectedAccountId,
}) => apiPost(path, body, token: token, expectedAccountId: expectedAccountId);

class BabyService {
  BabyService({BabyGet? get, BabyPut? put, BabyPost? post})
    : _get = get ?? apiGet,
      _put = put ?? apiPut,
      _post = post ?? _defaultBabyPost;

  final BabyGet _get;
  final BabyPut _put;
  final BabyPost _post;
  // UC31: Create a new baby profile
  Future<Map<String, dynamic>> createBabyProfile(
    CreateBabyRequest request, {
    String? token,
    String? expectedAccountId,
  }) async {
    final data = await _post(
      '/api/v1/babies',
      request.toJson(),
      token: token,
      expectedAccountId: expectedAccountId,
    );
    return data['data'] as Map<String, dynamic>;
  }

  // UC192: Get a single baby profile by ID
  Future<BabyProfile> getBabyProfile(String babyId) async {
    final data = await _get('/api/v1/babies/$babyId');
    final body = data['data'] as Map<String, dynamic>;
    return BabyProfile.fromJson(body);
  }

  // UC32: List baby profiles for the current user
  Future<List<BabyProfile>> listBabyProfiles() async {
    final data = await _get('/api/v1/babies');
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
    final data = await _put('/api/v1/babies/$babyId', request.toJson());
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

  // UC-193: Switch active baby profile (CB-160)
  Future<BabyProfile> switchActiveBabyProfile(String babyId) async {
    final data = await apiPatch('/api/v1/babies/$babyId/active', {});
    final body = data['data'] as Map<String, dynamic>;
    return BabyProfile.fromJson(body);
  }

  // UC-33: Soft-archive baby profile, linked data is preserved.
  Future<void> archiveBabyProfile(String babyId) async {
    await apiPost('/api/v1/babies/$babyId/archive', {});
  }
}
