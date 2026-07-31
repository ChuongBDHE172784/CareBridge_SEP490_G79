class ChecklistAssignmentContext {
  const ChecklistAssignmentContext({
    required this.canAssign,
    required this.journeyId,
  });

  final bool canAssign;
  final String? journeyId;

  factory ChecklistAssignmentContext.resolve({
    required String templateStage,
    required String? journeyId,
    required bool lifecycleMode,
  }) {
    final normalizedJourneyId = journeyId == null || journeyId.isEmpty
        ? null
        : journeyId;
    if (templateStage == 'BABY_CARE') {
      return const ChecklistAssignmentContext(canAssign: true, journeyId: null);
    }
    return ChecklistAssignmentContext(
      canAssign: lifecycleMode || normalizedJourneyId != null,
      journeyId: lifecycleMode ? null : normalizedJourneyId,
    );
  }
}
