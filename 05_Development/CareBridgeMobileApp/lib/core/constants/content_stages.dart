class ContentStageOption {
  final String value;
  final String label;

  const ContentStageOption({required this.value, required this.label});
}

const prePregnancyContentStage = 'PRE_PREGNANCY';
const pregnancyContentStage = 'PREGNANCY';
const postpartumContentStage = 'POSTPARTUM';

const contentStageOptions = <ContentStageOption>[
  ContentStageOption(
    value: prePregnancyContentStage,
    label: 'Chuẩn bị mang thai',
  ),
  ContentStageOption(value: pregnancyContentStage, label: 'Thai kỳ'),
  ContentStageOption(value: postpartumContentStage, label: 'Hậu sản & Chăm bé'),
];

const canonicalContentStages = <String>{
  prePregnancyContentStage,
  pregnancyContentStage,
  postpartumContentStage,
};

String? tryNormalizeContentStage(String? value) {
  final normalized = value?.trim().toUpperCase();
  if (normalized == 'BABY_CARE') return postpartumContentStage;
  return canonicalContentStages.contains(normalized) ? normalized : null;
}

String normalizeContentStage(
  String? value, {
  String fallback = pregnancyContentStage,
}) => tryNormalizeContentStage(value) ?? fallback;

String contentStageLabel(String? value) {
  switch (tryNormalizeContentStage(value)) {
    case prePregnancyContentStage:
      return 'Chuẩn bị mang thai';
    case pregnancyContentStage:
      return 'Thai kỳ';
    case postpartumContentStage:
      return 'Hậu sản & Chăm bé';
    default:
      return value ?? '';
  }
}
