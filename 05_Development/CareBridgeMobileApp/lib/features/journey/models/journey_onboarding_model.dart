enum LifecycleGoal {
  preparingForPregnancy('PREPARING_FOR_PREGNANCY'),
  currentlyPregnant('CURRENTLY_PREGNANT'),
  postpartumRecovery('POSTPARTUM_RECOVERY');

  const LifecycleGoal(this.apiValue);
  final String apiValue;
}

enum SupportPreference {
  nutrition('NUTRITION'),
  mentalWellbeing('MENTAL_WELLBEING'),
  physicalActivity('PHYSICAL_ACTIVITY'),
  appointmentReminders('APPOINTMENT_REMINDERS');

  const SupportPreference(this.apiValue);
  final String apiValue;
}

class JourneyOnboardingRequest {
  const JourneyOnboardingRequest({
    required this.submissionId,
    required this.lifecycleGoal,
    required this.locale,
    required this.timeZone,
    required this.preferences,
    required this.consentAccepted,
  });

  final String submissionId;
  final LifecycleGoal lifecycleGoal;
  final String locale;
  final String timeZone;
  final List<SupportPreference> preferences;
  final bool consentAccepted;

  Map<String, dynamic> toJson() => {
    'submissionId': submissionId,
    'lifecycleGoal': lifecycleGoal.apiValue,
    'locale': locale,
    'timeZone': timeZone,
    'preferences': preferences.map((value) => value.apiValue).toList(),
    'consentAccepted': consentAccepted,
    'policyVersion': 'MOTHER_LIFECYCLE_V1',
  };
}

class JourneyOnboardingStatus {
  const JourneyOnboardingStatus({
    required this.baselineComplete,
    required this.consentValid,
    required this.baselineRevision,
  });

  factory JourneyOnboardingStatus.fromJson(Map<String, dynamic> json) =>
      JourneyOnboardingStatus(
        baselineComplete: json['baselineComplete'] == true,
        consentValid: json['consentValid'] == true,
        baselineRevision: (json['baselineRevision'] as num?)?.toInt() ?? 0,
      );

  final bool baselineComplete;
  final bool consentValid;
  final int baselineRevision;

  bool get canStartJourney => baselineComplete && consentValid;
}
