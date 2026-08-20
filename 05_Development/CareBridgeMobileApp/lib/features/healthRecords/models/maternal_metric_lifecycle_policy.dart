const fetalMovementPregnancyOnlyMessage =
    'Cử động thai chỉ áp dụng cho hành trình thai kỳ đang hoạt động.';

const epdsPregnancyOrPostpartumOnlyMessage =
    'EPDS áp dụng cho hành trình mang thai hoặc sau sinh.';

const maternalJourneyStageUnavailableMessage =
    'Chưa thể xác định giai đoạn hành trình. Vui lòng thử lại.';

String? maternalMetricEntryRestrictionMessage({
  required String metricType,
  required String? journeyType,
}) {
  final metric = metricType.trim().toUpperCase();
  final stage = journeyType?.trim().toUpperCase();
  final isFetalMovement =
      metric == 'FETAL_MOVEMENT_SESSION' ||
      metric == 'FETAL_MOVEMENT_COUNT' ||
      metric == 'FETAL_MOVEMENT';

  if (isFetalMovement) {
    if (stage == null || stage.isEmpty) {
      return maternalJourneyStageUnavailableMessage;
    }
    return stage == 'PREGNANCY' ? null : fetalMovementPregnancyOnlyMessage;
  }

  if (metric == 'EPDS' || metric == 'EPDS_SCORE' || metric == 'MOOD') {
    if (stage == null || stage.isEmpty) {
      return maternalJourneyStageUnavailableMessage;
    }
    return stage == 'PREGNANCY' || stage == 'POSTPARTUM'
        ? null
        : epdsPregnancyOrPostpartumOnlyMessage;
  }

  return null;
}
