enum TriageStageIntent {
  preconception('PRECONCEPTION'),
  pregnancy('PREGNANCY'),
  postpartum('POSTPARTUM'),
  infant('INFANT'),
  toddler('TODDLER');

  const TriageStageIntent(this.apiValue);

  final String apiValue;
}

class TriageEntryContext {
  const TriageEntryContext({
    this.stage = TriageStageIntent.infant,
    this.lockStage = false,
  });

  const TriageEntryContext.postpartum()
    : stage = TriageStageIntent.postpartum,
      lockStage = true;

  final TriageStageIntent stage;
  final bool lockStage;

  bool get isPostpartum => stage == TriageStageIntent.postpartum;
}
