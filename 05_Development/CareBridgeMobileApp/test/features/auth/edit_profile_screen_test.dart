import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/auth/screens/edit_profile_screen.dart';
import 'package:untitled/features/recommendation/models/recommendation_model.dart';
import 'package:untitled/features/recommendation/services/recommendation_service.dart';

class _FakeRecommendationService extends RecommendationService {
  _FakeRecommendationService({this.response});

  final RecommendationProfileResponse? response;

  @override
  Future<RecommendationProfileResponse> getProfile() async {
    return response ??
        const RecommendationProfileResponse(
          status: RecommendationProfileStatus.notStarted,
          requiresAction: true,
          profileComplete: false,
          schemaVersion: 1,
          profileRevision: 0,
          completedAt: null,
          profile: null,
          derived: null,
        );
  }
}

Future<void> _signInForTest() async {
  FlutterSecureStorage.setMockInitialValues({});
  await AuthState.instance.clear();
  await AuthState.instance.setTokens(
    accessToken: 'test-access',
    refreshToken: 'test-refresh',
    userId: 'mother-1',
    role: 'MOTHER',
  );
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(_signInForTest);
  tearDown(() => AuthState.instance.clear());

  testWidgets(
    'EditProfileScreen renders DOB label and empty personalized profile banner',
    (tester) async {
      tester.view.physicalSize = const Size(1080, 2400);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });

      final fakeService = _FakeRecommendationService();

      await tester.pumpWidget(
        MaterialApp(
          home: EditProfileScreen(
            recommendationService: fakeService,
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Check DOB label
      expect(find.text('Ngày sinh'), findsOneWidget);
      expect(find.text('Hồ sơ sức khoẻ cá nhân hoá'), findsOneWidget);
      expect(find.text('Chưa khảo sát'), findsOneWidget);
      expect(find.text('Làm khảo sát cá nhân hoá ngay'), findsOneWidget);
    },
  );

  testWidgets(
    'EditProfileScreen renders active personalized profile with BMI and conditions',
    (tester) async {
      tester.view.physicalSize = const Size(1080, 2400);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });

      final activeResponse = RecommendationProfileResponse(
        status: RecommendationProfileStatus.active,
        requiresAction: false,
        profileComplete: true,
        schemaVersion: 1,
        profileRevision: 1,
        completedAt: DateTime(2026, 1, 1),
        profile: const {
          'bmi': {
            'state': 'KNOWN',
            'heightCm': 160.0,
            'weightKg': 52.0,
            'weightContext': 'PRE_PREGNANCY',
          },
          'reproductiveHistory': {
            'state': 'KNOWN',
            'codes': ['PRIOR_LIVE_BIRTH'],
          },
          'underlyingConditions': {
            'state': 'KNOWN',
            'codes': ['HYPERTENSION'],
          },
        },
        derived: const {
          'bmi': 20.31,
          'bmiCategory': 'HEALTHY_RANGE',
        },
      );

      final fakeService = _FakeRecommendationService(response: activeResponse);

      await tester.pumpWidget(
        MaterialApp(
          home: EditProfileScreen(
            recommendationService: fakeService,
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('Hoạt động'), findsOneWidget);
      expect(find.text('Thể trạng & Chỉ số BMI'), findsOneWidget);
      expect(find.text('Chiều cao: 160.0 cm'), findsOneWidget);
      expect(find.text('Cân nặng: 52.0 kg'), findsOneWidget);
      expect(find.text('BMI: 20.31 (Bình thường)'), findsOneWidget);
      expect(find.text('Tiền sử sinh sản'), findsOneWidget);
      expect(find.text('Từng sinh con sống'), findsOneWidget);
      expect(find.text('Bệnh nền & Tình trạng sức khỏe'), findsOneWidget);
      expect(find.text('Tăng huyết áp'), findsOneWidget);
      expect(find.text('Cập nhật khảo sát cá nhân hoá'), findsOneWidget);
    },
  );
}
