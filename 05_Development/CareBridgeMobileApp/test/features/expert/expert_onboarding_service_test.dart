import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/expert/models/expert_onboarding_model.dart';
import 'package:untitled/features/expert/services/expert_onboarding_service.dart';

void main() {
  group('ExpertOnboardingState', () {
    test('routes an incomplete expert to identity after profile creation', () {
      final state = ExpertOnboardingState.fromJson({
        'data': {
          'profileExists': true,
          'identityStatus': 'NOT_SUBMITTED',
          'credentialStatus': 'NOT_SUBMITTED',
          'verificationStatus': 'PENDING',
          'nextStep': 'IDENTITY',
        },
      });

      expect(state.profileComplete, isTrue);
      expect(state.identityComplete, isFalse);
      expect(state.nextStep, ExpertOnboardingStep.identity);
      expect(state.approved, isFalse);
    });

    test('recognizes an approved aggregate as complete', () {
      final state = ExpertOnboardingState.fromJson({
        'profileExists': true,
        'identityStatus': 'APPROVED',
        'credentialStatus': 'APPROVED',
        'verificationStatus': 'APPROVED',
        'nextStep': 'COMPLETE',
      });

      expect(state.approved, isTrue);
      expect(state.nextStep, ExpertOnboardingStep.complete);
    });
  });

  group('ExpertOnboardingService', () {
    test('sends all three identity images atomically with backend field names', () async {
      final api = _FakeApi();
      final service = ExpertOnboardingService(api: api);
      final image = ExpertEvidenceImage(
        bytes: List<int>.filled(20, 1),
        fileName: 'evidence.jpg',
        mimeType: 'image/jpeg',
      );

      await service.submitIdentity(
        selfie: image,
        identityFront: image,
        identityBack: image,
      );

      expect(api.multipartPath, '/api/v1/expert/identity');
      expect(
        api.files.map((file) => file.fieldName),
        ['selfie', 'identityFront', 'identityBack'],
      );
    });

    test('rejects an oversized identity image before network submission', () async {
      final api = _FakeApi();
      final service = ExpertOnboardingService(api: api);
      final valid = ExpertEvidenceImage(
        bytes: List<int>.filled(20, 1),
        fileName: 'valid.png',
        mimeType: 'image/png',
      );
      final oversized = ExpertEvidenceImage(
        bytes: List<int>.filled(5 * 1024 * 1024 + 1, 1),
        fileName: 'large.jpg',
        mimeType: 'image/jpeg',
      );

      await expectLater(
        service.submitIdentity(
          selfie: oversized,
          identityFront: valid,
          identityBack: valid,
        ),
        throwsArgumentError,
      );
      expect(api.multipartPath, isNull);
    });

    test('uses the existing credential multipart contract', () async {
      final api = _FakeApi();
      final service = ExpertOnboardingService(api: api);
      const file = ExpertEvidenceImage(
        bytes: [1, 2, 3],
        fileName: 'license.jpg',
        mimeType: 'image/jpeg',
      );

      await service.submitCredential(
        credentialType: 'MEDICAL_LICENSE',
        credentialNumber: 'GP-123',
        issuer: 'Sở Y tế',
        issuedDate: '2024-01-01',
        file: file,
      );

      expect(api.multipartPath, '/api/v1/expert/credentials');
      expect(api.fields['issuer'], 'Sở Y tế');
      expect(api.files.single.fieldName, 'file');
    });
  });
}

class _FakeApi implements ExpertOnboardingApi {
  String? multipartPath;
  Map<String, String> fields = {};
  List<MultipartUploadFile> files = [];

  @override
  Future<dynamic> get(String path) async => {
        'data': {'nextStep': 'PROFILE'},
      };

  @override
  Future<dynamic> multipart(
    String path,
    Map<String, String> fields,
    List<MultipartUploadFile> files,
  ) async {
    multipartPath = path;
    this.fields = fields;
    this.files = files;
    return {'data': {}};
  }

  @override
  Future<dynamic> post(String path, Map<String, dynamic> body) async => {
        'data': {},
      };
}
