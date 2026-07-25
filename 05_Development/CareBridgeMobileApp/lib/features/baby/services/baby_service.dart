import '../../../core/network/api_client.dart';
import '../models/baby_model.dart';

typedef BabyGet =
    Future<dynamic> Function(String path, {Map<String, dynamic>? queryParams});
typedef BabyPut =
    Future<dynamic> Function(String path, Map<String, dynamic> body);

class BabyService {
  BabyService({BabyGet? get, BabyPut? put})
    : _get = get ?? apiGet,
      _put = put ?? apiPut;

  final BabyGet _get;
  final BabyPut _put;
  // UC31: Create a new baby profile
  Future<Map<String, dynamic>> createBabyProfile(
    CreateBabyRequest request,
  ) async {
    final data = await apiPost('/api/v1/babies', request.toJson());
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

  Future<void> linkBabyToJourney({
    required String babyId,
    required String relatedJourneyId,
    required String submissionId,
  }) async {
    await _put('/api/v1/babies/$babyId/journey-link', {
      'relatedJourneyId': relatedJourneyId,
      'submissionId': submissionId,
    });
  }

  Future<BabyProfilePage> listJourneyBabies(
    String journeyId, {
    int page = 0,
    int size = 20,
  }) async {
    final data = await _get(
      '/api/v1/journeys/$journeyId/babies',
      queryParams: {'page': page, 'size': size},
    );
    // The journey-scoped endpoint returns its pagination body directly,
    // unlike the profile endpoints which use the standard `data` envelope.
    // Accept both forms so the client remains compatible with the API
    // contract and avoids treating a successful 200 response as a load error.
    if (data is! Map<String, dynamic>) {
      throw const FormatException('Invalid journey babies response');
    }
    final response = data;
    final payload = response['data'];
    final Map<String, dynamic> pageBody;
    if (payload is Map<String, dynamic>) {
      pageBody = payload;
    } else if (payload is List<dynamic>) {
      // PaginatedResponse extends ApiResponse<List<T>>, so paging metadata
      // lives beside the list-valued `data` field.
      pageBody = {...response, 'items': payload};
    } else if (response['content'] is List<dynamic> ||
        response['items'] is List<dynamic>) {
      pageBody = response;
    } else {
      throw const FormatException(
        'Journey babies response has no recognized page items',
      );
    }
    if (pageBody['content'] is! List<dynamic> &&
        pageBody['items'] is! List<dynamic>) {
      throw const FormatException(
        'Journey babies response has no recognized page items',
      );
    }
    return BabyProfilePage.fromJson(pageBody);
  }
}
