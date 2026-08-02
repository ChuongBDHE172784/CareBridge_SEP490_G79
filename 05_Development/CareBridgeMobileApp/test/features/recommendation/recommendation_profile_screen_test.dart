import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/journey/services/journey_service.dart';
import 'package:untitled/features/recommendation/models/recommendation_model.dart';
import 'package:untitled/features/recommendation/screens/recommendation_profile_screen.dart';
import 'package:untitled/features/recommendation/services/recommendation_service.dart';

class _FakeJourneyService extends JourneyService {
  @override
  Future<JourneyDashboard> getDashboard() async => const JourneyDashboard(
    journeyId: 'journey-1',
    journeyType: 'PRE_PREGNANCY',
    status: 'PRE_PREGNANCY',
  );
}

class _FakeRecommendationService extends RecommendationService {
  _FakeRecommendationService({Map<String, dynamic>? profile})
    : profile = profile ?? RecommendationProfileDraft.empty(),
      super();

  String? dateOfBirth;
  final Map<String, dynamic> profile;
  Map<String, dynamic>? lastDraft;
  Map<String, dynamic>? submittedProfile;
  final List<String> events = [];

  @override
  Future<String?> getDateOfBirth() async {
    events.add('get-dob');
    return dateOfBirth;
  }

  @override
  Future<RecommendationProfileResponse> getProfile() async {
    events.add('get-profile');
    return RecommendationProfileResponse(
      status: RecommendationProfileStatus.notStarted,
      requiresAction: true,
      profileComplete: false,
      schemaVersion: 1,
      profileRevision: 0,
      completedAt: null,
      profile: profile,
      derived: null,
    );
  }

  @override
  Future<Map<String, dynamic>?> readDraft() async {
    events.add('read-draft');
    return null;
  }

  @override
  Future<void> saveDraft(Map<String, dynamic> value) async {
    events.add('save-draft');
    lastDraft = RecommendationProfileDraft.copyProfile(value);
  }

  @override
  Future<void> updateDateOfBirth(String value) async {
    events.add('patch-dob');
    dateOfBirth = value;
  }

  @override
  Future<RecommendationProfileResponse> putProfile({
    required Map<String, dynamic> profile,
    String? submissionId,
  }) async {
    events.add('put-profile');
    submittedProfile = RecommendationProfileDraft.copyProfile(profile);
    return RecommendationProfileResponse(
      status: RecommendationProfileStatus.active,
      requiresAction: false,
      profileComplete: true,
      schemaVersion: 1,
      profileRevision: 1,
      completedAt: DateTime(2026, 8, 3),
      profile: submittedProfile,
      derived: null,
    );
  }

  @override
  Future<void> clearDraftFor(String userId) async => events.add('clear-draft');
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
  setUp(_signInForTest);
  tearDown(() => AuthState.instance.clear());

  testWidgets('shows one localized question and maps skip to UNKNOWN', (
    tester,
  ) async {
    final service = _FakeRecommendationService();
    await tester.pumpWidget(
      MaterialApp(
        home: RecommendationProfileScreen(
          service: service,
          journeyService: _FakeJourneyService(),
          now: () => DateTime(2026, 8, 3),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Đồng ý và tiếp tục'));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('recommendation-dob-field')), findsOneWidget);
    expect(find.text('Bỏ qua'), findsOneWidget);
    expect(find.text('KNOWN'), findsNothing);
    expect(find.text('PREFER_NOT_TO_SAY'), findsNothing);
    expect(find.text('Mở Hồ sơ để cập nhật ngày sinh'), findsNothing);

    await tester.tap(find.byKey(const Key('recommendation-skip-button')));
    await tester.pumpAndSettle();
    expect(
      find.text('Bạn có muốn chia sẻ chỉ số và cân nặng gần đây không?'),
      findsOneWidget,
    );
    expect(
      find.byKey(const Key('recommendation-measured-on-field')),
      findsNothing,
    );
    expect(find.text('Bối cảnh của cân nặng'), findsNothing);
    expect((service.lastDraft?['age'] as Map?)?['state'], 'UNKNOWN');
  });

  testWidgets('direct DOB entry patches account before advancing', (
    tester,
  ) async {
    final service = _FakeRecommendationService();
    await tester.pumpWidget(
      MaterialApp(
        home: RecommendationProfileScreen(
          service: service,
          journeyService: _FakeJourneyService(),
          now: () => DateTime(2026, 8, 3),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Đồng ý và tiếp tục'));
    await tester.pumpAndSettle();

    await tester.enterText(
      find.byKey(const Key('recommendation-dob-field')),
      '1995-08-21',
    );
    await tester.tap(find.byKey(const Key('recommendation-continue-button')));
    await tester.pumpAndSettle();

    expect(service.events, contains('patch-dob'));
    expect(service.dateOfBirth, '1995-08-21');
    expect((service.lastDraft?['age'] as Map?)?['state'], 'KNOWN');
    expect(
      find.text('Bạn có muốn chia sẻ chỉ số và cân nặng gần đây không?'),
      findsOneWidget,
    );
  });

  testWidgets('BMI derives stage context and measurement date automatically', (
    tester,
  ) async {
    final service = _FakeRecommendationService();
    await tester.pumpWidget(
      MaterialApp(
        home: RecommendationProfileScreen(
          service: service,
          journeyService: _FakeJourneyService(),
          now: () => DateTime(2026, 8, 3),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Đồng ý và tiếp tục'));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('recommendation-skip-button')));
    await tester.pumpAndSettle();

    await tester.enterText(
      find.byKey(const Key('recommendation-height-field')),
      '160',
    );
    await tester.enterText(
      find.byKey(const Key('recommendation-weight-field')),
      '55',
    );
    await tester.tap(find.byKey(const Key('recommendation-continue-button')));
    await tester.pumpAndSettle();

    final bmi = service.lastDraft?['bmi'] as Map?;
    expect(bmi?['weightContext'], 'CURRENT_NON_PREGNANT');
    expect(bmi?['measuredOn'], '2026-08-03');
  });
}
