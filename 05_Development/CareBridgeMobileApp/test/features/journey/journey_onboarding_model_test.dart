import 'package:untitled/features/journey/models/journey_onboarding_model.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('serializes approved baseline and explicit consent contract', () {
    final request = JourneyOnboardingRequest(
      submissionId: '00000000-0000-0000-0000-000000006200',
      lifecycleGoal: LifecycleGoal.preparingForPregnancy,
      locale: 'vi-VN',
      timeZone: 'Asia/Ho_Chi_Minh',
      preferences: const [SupportPreference.nutrition],
      consentAccepted: true,
    );

    expect(request.toJson(), {
      'submissionId': '00000000-0000-0000-0000-000000006200',
      'lifecycleGoal': 'PREPARING_FOR_PREGNANCY',
      'locale': 'vi-VN',
      'timeZone': 'Asia/Ho_Chi_Minh',
      'preferences': ['NUTRITION'],
      'consentAccepted': true,
      'policyVersion': 'MOTHER_LIFECYCLE_V1',
    });
  });

  test('parses authoritative completion state', () {
    final status = JourneyOnboardingStatus.fromJson({
      'baselineComplete': true,
      'consentValid': false,
      'baselineRevision': 2,
    });

    expect(status.baselineComplete, isTrue);
    expect(status.consentValid, isFalse);
    expect(status.canStartJourney, isFalse);
  });
}
