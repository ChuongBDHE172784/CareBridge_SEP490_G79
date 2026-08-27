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
      expect(state.credentialStatus, 'NOT_SUBMITTED');
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

    test(
      'treats canonical MISSING statuses as incomplete without nextStep',
      () {
        final state = ExpertOnboardingState.fromJson({
          'profileExists': true,
          'identityStatus': 'MISSING',
          'credentialStatus': 'MISSING',
          'verificationStatus': 'PENDING',
        });

        expect(state.identityComplete, isFalse);
        expect(state.credentialComplete, isFalse);
        expect(state.nextStep, ExpertOnboardingStep.identity);
      },
    );

    test('falls back to latest identity rejection reason', () {
      final state = ExpertOnboardingState.fromJson({
        'profileExists': true,
        'identityStatus': 'REJECTED',
        'credentialStatus': 'PENDING',
        'verificationStatus': 'REJECTED',
        'latestIdentityAttempt': {'reviewReason': 'Ảnh CCCD bị lóa'},
      });

      expect(state.rejectionReason, 'Ảnh CCCD bị lóa');
      expect(state.identityComplete, isFalse);
      expect(state.nextStep, ExpertOnboardingStep.identity);
    });

    test(
      'honors identityComplete when legacy payload omits identityStatus',
      () {
        final state = ExpertOnboardingState.fromJson({
          'profileExists': true,
          'identityComplete': true,
          'credentialStatus': 'NOT_SUBMITTED',
          'verificationStatus': 'PENDING',
        });

        expect(state.identityComplete, isTrue);
        expect(state.nextStep, ExpertOnboardingStep.credential);
      },
    );
  });

  group('ExpertOnboardingService', () {
    test(
      'sends all three identity images atomically with backend field names',
      () async {
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
        expect(api.files.map((file) => file.fieldName), [
          'selfie',
          'identityFront',
          'identityBack',
        ]);
      },
    );

    test(
      'rejects an oversized identity image before network submission',
      () async {
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
      },
    );

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

    test('accepts DOCX credentials used by the web portal', () async {
      final api = _FakeApi();
      final service = ExpertOnboardingService(api: api);
      const file = ExpertEvidenceImage(
        bytes: [1, 2, 3],
        fileName: 'degree.docx',
        mimeType:
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      );

      await service.submitCredential(
        credentialType: 'DEGREE',
        credentialNumber: 'DEG-1',
        issuer: 'Đại học Y Hà Nội',
        issuedDate: '2020-01-01',
        file: file,
      );

      expect(api.files.single.mimeType, file.mimeType);
    });

    test(
      'omits optional profile fields and preserves OTHER workplace metadata',
      () async {
        final api = _FakeApi();
        final service = ExpertOnboardingService(api: api);

        await service.createProfile(
          specialtyId: 'specialty-1',
          professionalTitle: 'Bác sĩ',
          hospitalId: 'OTHER',
          trackAsiaName: 'Phòng khám An Tâm',
          trackAsiaAddress: 'Hà Nội',
          trackAsiaLat: 21.02,
          trackAsiaLng: 105.84,
        );

        expect(api.postPath, '/api/v1/expert/profiles');
        expect(api.postBody['hospitalId'], 'OTHER');
        expect(api.postBody['trackAsiaName'], 'Phòng khám An Tâm');
        expect(api.postBody.containsKey('experienceYears'), isFalse);
        expect(api.postBody.containsKey('consultationScope'), isFalse);
      },
    );

    test('uses authenticated multipart abstraction for face preview', () async {
      final api = _FakeApi()
        ..multipartResponse = {
          'data': {
            'status': 'MATCHED',
            'similarity': 0.91,
            'threshold': '0.80',
          },
        };
      final service = ExpertOnboardingService(api: api);
      const image = ExpertEvidenceImage(
        bytes: [0xff, 0xd8, 1],
        fileName: 'face.jpg',
        mimeType: 'image/jpeg',
      );

      final preview = await service.previewFace(
        selfie: image,
        identityFront: image,
      );

      expect(api.multipartPath, '/api/v1/expert/verify-face');
      expect(api.files.map((file) => file.fieldName), ['selfie', 'idCard']);
      expect(preview.matched, isTrue);
      expect(preview.similarity, .91);
      expect(preview.threshold, .8);
    });
  });
}

class _FakeApi implements ExpertOnboardingApi {
  String? multipartPath;
  String? patchPath;
  Map<String, dynamic>? patchBody;
  String? postPath;
  Map<String, dynamic> postBody = {};
  Map<String, String> fields = {};
  List<MultipartUploadFile> files = [];
  dynamic multipartResponse = const {'data': <String, dynamic>{}};

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
    return multipartResponse;
  }

  @override
  Future<dynamic> post(String path, Map<String, dynamic> body) async {
    postPath = path;
    postBody = body;
    return {'data': {}};
  }

  @override
  Future<dynamic> patch(String path, Map<String, dynamic> body) async {
    patchPath = path;
    patchBody = body;
    return {'data': {}};
  }
}
