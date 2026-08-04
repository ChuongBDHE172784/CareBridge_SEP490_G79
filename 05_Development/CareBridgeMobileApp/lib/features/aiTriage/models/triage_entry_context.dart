enum TriageStageIntent {
  preconception('PRECONCEPTION'),
  pregnancy('PREGNANCY'),
  postpartum('POSTPARTUM'),
  infant('INFANT'),
  toddler('TODDLER');

  const TriageStageIntent(this.apiValue);

  final String apiValue;
}

enum TriageOriginIntent { direct, motherJourney, babyProfile }

class TriageEntryContext {
  const TriageEntryContext({
    this.stage = TriageStageIntent.infant,
    this.lockStage = false,
    this.requiresStageSelection = false,
    this.origin = TriageOriginIntent.direct,
    this.journeyId,
    this.originReferenceId,
  });

  const TriageEntryContext.postpartum({
    this.origin = TriageOriginIntent.direct,
    this.journeyId,
    this.originReferenceId,
  }) : stage = TriageStageIntent.postpartum,
       lockStage = true,
       requiresStageSelection = false;

  const TriageEntryContext.locked({
    required this.stage,
    required this.origin,
    this.journeyId,
    this.originReferenceId,
  }) : lockStage = true,
       requiresStageSelection = false;

  final TriageStageIntent stage;
  final bool lockStage;

  /// Direct/floating entry has no trusted lifecycle source.  Do not let its
  /// display default be serialized until the user explicitly selects a stage.
  final bool requiresStageSelection;
  final TriageOriginIntent origin;
  final String? journeyId;
  final String? originReferenceId;

  bool get isMaternal => const {
    TriageStageIntent.preconception,
    TriageStageIntent.pregnancy,
    TriageStageIntent.postpartum,
  }.contains(stage);

  bool get isPostpartum => stage == TriageStageIntent.postpartum;

  Map<String, dynamic> toLifecycleBindingJson() {
    if (origin == TriageOriginIntent.direct) return const {};
    final trustedJourneyId = journeyId;
    final trustedOriginReferenceId = originReferenceId;
    final requiresJourney = origin == TriageOriginIntent.motherJourney;
    if ((requiresJourney &&
            (trustedJourneyId == null || trustedJourneyId.isEmpty)) ||
        trustedOriginReferenceId == null ||
        trustedOriginReferenceId.isEmpty) {
      // Story 6.6 typed current-session entries remain compatible. They do
      // not claim durable lifecycle continuation until exact IDs are present.
      return const {};
    }
    return {
      'journeyId': trustedJourneyId,
      'originDashboard': origin == TriageOriginIntent.motherJourney
          ? 'MOTHER_JOURNEY'
          : 'BABY_PROFILE',
      'originReferenceId': trustedOriginReferenceId,
      if (origin == TriageOriginIntent.babyProfile)
        'babyProfileId': trustedOriginReferenceId,
    };
  }
}
