class ContentStageOption {
  final String value;
  final String label;

  const ContentStageOption({required this.value, required this.label});
}

const prePregnancyContentStage = 'PRE_PREGNANCY';
const pregnancyContentStage = 'PREGNANCY';
const postpartumContentStage = 'POSTPARTUM';
const babyCareContentStage = 'BABY_CARE';

const contentStageOptions = <ContentStageOption>[
  ContentStageOption(
    value: prePregnancyContentStage,
    label: 'Chuẩn bị mang thai',
  ),
  ContentStageOption(value: pregnancyContentStage, label: 'Thai kỳ'),
  ContentStageOption(value: postpartumContentStage, label: 'Hậu sản'),
  ContentStageOption(value: babyCareContentStage, label: 'Chăm bé'),
];

const canonicalContentStages = <String>{
  prePregnancyContentStage,
  pregnancyContentStage,
  postpartumContentStage,
  babyCareContentStage,
};

String? tryNormalizeContentStage(String? value) {
  final normalized = value?.trim().toUpperCase();
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
      return 'Hậu sản';
    case babyCareContentStage:
      return 'Chăm bé';
    default:
      return value ?? '';
  }
}
