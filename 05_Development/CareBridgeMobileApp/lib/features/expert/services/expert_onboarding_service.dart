import '../../../core/network/api_client.dart';
import '../models/expert_onboarding_model.dart';

abstract class ExpertOnboardingApi {
  Future<dynamic> get(String path);
  Future<dynamic> post(String path, Map<String, dynamic> body);
  Future<dynamic> multipart(
    String path,
    Map<String, String> fields,
    List<MultipartUploadFile> files,
  );
}

class _DefaultExpertOnboardingApi implements ExpertOnboardingApi {
  @override
  Future<dynamic> get(String path) => apiGet(path);

  @override
  Future<dynamic> post(String path, Map<String, dynamic> body) =>
      apiPost(path, body);

  @override
  Future<dynamic> multipart(
    String path,
    Map<String, String> fields,
    List<MultipartUploadFile> files,
  ) => apiMultipart(path, fields, files: files);
}

class ExpertOnboardingService {
  static ExpertOnboardingService instance = ExpertOnboardingService();

  final ExpertOnboardingApi api;

  ExpertOnboardingService({ExpertOnboardingApi? api})
    : api = api ?? _DefaultExpertOnboardingApi();

  Future<ExpertOnboardingState> loadState() async {
    final response = await api.get('/api/v1/expert/onboarding');
    return ExpertOnboardingState.fromJson(
      response as Map<String, dynamic>? ?? const {},
    );
  }

  Future<void> createProfile({
    required String specialtyId,
    required String professionalTitle,
    int? experienceYears,
    required String hospitalId,
    String? trackAsiaName,
    String? trackAsiaAddress,
    double? trackAsiaLat,
    double? trackAsiaLng,
    String? consultationScope,
  }) async {
    final body = <String, dynamic>{
      'specialtyId': specialtyId,
      'professionalTitle': professionalTitle,
      'hospitalId': hospitalId,
    };
    if (experienceYears != null) body['experienceYears'] = experienceYears;
    if (consultationScope != null && consultationScope.trim().isNotEmpty) {
      body['consultationScope'] = consultationScope.trim();
    }
    if (hospitalId == 'OTHER' &&
        trackAsiaName != null &&
        trackAsiaName.trim().isNotEmpty) {
      body['trackAsiaName'] = trackAsiaName.trim();
      body['trackAsiaAddress'] = trackAsiaAddress?.trim();
      body['trackAsiaLat'] = trackAsiaLat;
      body['trackAsiaLng'] = trackAsiaLng;
    }
    await api.post('/api/v1/expert/profiles', body);
  }

  Future<List<ExpertMasterOption>> loadSpecialties() =>
      _loadMasterOptions('/api/v1/master-data/specialties', 'specialtyId');

  Future<List<ExpertMasterOption>> loadProvinces() =>
      _loadMasterOptions('/api/v1/master-data/provinces', 'provinceId');

  Future<List<ExpertMasterOption>> loadDistricts(
    String provinceId,
  ) => _loadMasterOptions(
    '/api/v1/master-data/districts?provinceId=${Uri.encodeQueryComponent(provinceId)}',
    'districtId',
  );

  Future<List<ExpertMasterOption>> loadWards({
    String? provinceId,
    String? districtId,
  }) {
    final query = <String, String>{};
    if (provinceId != null && provinceId.isNotEmpty) {
      query['provinceId'] = provinceId;
    }
    if (districtId != null && districtId.isNotEmpty) {
      query['districtId'] = districtId;
    }
    return _loadMasterOptions(
      '/api/v1/master-data/wards${_queryString(query)}',
      'wardId',
    );
  }

  Future<List<ExpertMasterOption>> loadHospitals({
    String? provinceId,
  }) => _loadMasterOptions(
    '/api/v1/master-data/hospitals${_queryString({if (provinceId != null && provinceId.isNotEmpty) 'provinceId': provinceId})}',
    'hospitalId',
  );

  Future<List<Map<String, dynamic>>> searchTrackAsiaHospitals(String q) async {
    final response = await api.get(
      '/api/v1/master-data/hospitals/search/trackasia${_queryString({'q': q.trim()})}',
    );
    return _unwrapList(response);
  }

  Future<List<ExpertMasterOption>> _loadMasterOptions(
    String path,
    String idKey,
  ) async {
    final response = await api.get(path);
    return _unwrapList(response)
        .map(
          (item) => ExpertMasterOption(
            id: item[idKey]?.toString() ?? '',
            name: item['name']?.toString() ?? '',
          ),
        )
        .where((option) => option.id.isNotEmpty && option.name.isNotEmpty)
        .toList();
  }

  String _queryString(Map<String, String> values) {
    if (values.isEmpty) return '';
    return '?${values.entries.map((entry) => '${Uri.encodeQueryComponent(entry.key)}=${Uri.encodeQueryComponent(entry.value)}').join('&')}';
  }

  List<Map<String, dynamic>> _unwrapList(dynamic response) {
    final envelope = response as Map<String, dynamic>? ?? const {};
    final raw = envelope['data'] ?? const [];
    return (raw as List)
        .whereType<Map>()
        .map((item) => Map<String, dynamic>.from(item))
        .toList();
  }

  Future<void> submitIdentity({
    required ExpertEvidenceImage selfie,
    required ExpertEvidenceImage identityFront,
    required ExpertEvidenceImage identityBack,
  }) async {
    _validateIdentityImage(selfie, 'Ảnh chân dung');
    _validateIdentityImage(identityFront, 'CCCD mặt trước');
    _validateIdentityImage(identityBack, 'CCCD mặt sau');
    await api.multipart('/api/v1/expert/identity', const {}, [
      _toUpload('selfie', selfie),
      _toUpload('identityFront', identityFront),
      _toUpload('identityBack', identityBack),
    ]);
  }

  Future<ExpertFacePreview> previewFace({
    required ExpertEvidenceImage selfie,
    required ExpertEvidenceImage identityFront,
  }) async {
    _validateIdentityImage(selfie, 'Ảnh chân dung');
    _validateIdentityImage(identityFront, 'CCCD mặt trước');
    final response = await api.multipart(
      '/api/v1/expert/verify-face',
      const {},
      [_toUpload('selfie', selfie), _toUpload('idCard', identityFront)],
    );
    return ExpertFacePreview.fromJson(
      response as Map<String, dynamic>? ?? const {},
    );
  }

  Future<void> submitCredential({
    required String credentialType,
    required String credentialNumber,
    required String issuer,
    required String issuedDate,
    String? expiryDate,
    required ExpertEvidenceImage file,
  }) async {
    _validateCredential(file);
    final fields = <String, String>{
      'credentialType': credentialType,
      'credentialNumber': credentialNumber,
      'issuer': issuer,
      'issuedDate': issuedDate,
    };
    if (expiryDate != null && expiryDate.isNotEmpty) {
      fields['expiryDate'] = expiryDate;
    }
    await api.multipart('/api/v1/expert/credentials', fields, [
      _toUpload('file', file),
    ]);
  }

  MultipartUploadFile _toUpload(String field, ExpertEvidenceImage image) =>
      MultipartUploadFile(
        fieldName: field,
        bytes: image.bytes,
        fileName: image.fileName,
        mimeType: image.mimeType,
      );

  void _validateIdentityImage(ExpertEvidenceImage image, String label) {
    if (!const {'image/jpeg', 'image/png'}.contains(image.mimeType)) {
      throw ArgumentError('$label phải là ảnh JPEG hoặc PNG.');
    }
    if (image.bytes.isEmpty || image.bytes.length > 5 * 1024 * 1024) {
      throw ArgumentError('$label phải có dung lượng tối đa 5 MB.');
    }
  }

  void _validateCredential(ExpertEvidenceImage image) {
    if (!const {
      'image/jpeg',
      'image/png',
      'application/pdf',
      'application/msword',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    }.contains(image.mimeType)) {
      throw ArgumentError('Giấy tờ phải là JPEG, PNG, PDF, DOC hoặc DOCX.');
    }
    if (image.bytes.isEmpty || image.bytes.length > 10 * 1024 * 1024) {
      throw ArgumentError('Giấy tờ phải có dung lượng tối đa 10 MB.');
    }
  }
}

class ExpertMasterOption {
  const ExpertMasterOption({required this.id, required this.name});

  final String id;
  final String name;
}

class ExpertFacePreview {
  const ExpertFacePreview({
    required this.status,
    this.similarity,
    this.threshold,
    this.providerErrorCode,
  });

  final String status;
  final double? similarity;
  final double? threshold;
  final String? providerErrorCode;

  bool get matched => status == 'MATCHED';

  factory ExpertFacePreview.fromJson(Map<String, dynamic> json) {
    final data = json['data'] is Map
        ? Map<String, dynamic>.from(json['data'] as Map)
        : json;
    return ExpertFacePreview(
      status: (data['status'] ?? 'RETRYABLE_ERROR').toString().toUpperCase(),
      similarity: _asDouble(data['similarity']),
      threshold: _asDouble(data['threshold']),
      providerErrorCode: data['providerErrorCode']?.toString(),
    );
  }

  static double? _asDouble(dynamic value) => value is num
      ? value.toDouble()
      : double.tryParse(value?.toString() ?? '');
}
