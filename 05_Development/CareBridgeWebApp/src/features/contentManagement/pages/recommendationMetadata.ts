import type { ContentStage, ContentType, RecommendationTag } from '../models/content';

const EXCLUSIVE_GROUPS: Array<[string, string]> = [
  ['rec-age-', 'AGE'],
  ['rec-bmi-', 'BMI'],
  ['rec-smoking-', 'SMOKING'],
  ['rec-alcohol-', 'ALCOHOL'],
  ['rec-activity-', 'ACTIVITY'],
  ['rec-sleep-', 'SLEEP'],
  ['rec-sti-screening-information', 'STI_STATUS'],
  ['rec-sti-past-history', 'STI_STATUS'],
  ['rec-sti-current-or-treatment', 'STI_STATUS'],
];

function groupForSlug(slug: string): string | null {
  return EXCLUSIVE_GROUPS.find(([prefix]) => slug.startsWith(prefix))?.[1] ?? null;
}

export function recommendationMetadataError(params: {
  type: ContentType;
  stage: ContentStage | '';
  from: number | null;
  to: number | null;
  priority: number;
  selectedTagIds: string[];
  catalog: RecommendationTag[];
}): string | null {
  if (params.type !== 'ARTICLE') return null;
  if (params.stage === '') return 'Select a lifecycle stage before configuring recommendation metadata.';
  if (!Number.isInteger(params.priority) || params.priority < 0 || params.priority > 100) {
    return 'Recommendation priority must be an integer from 0 to 100.';
  }
  const boundsAreEmpty = params.from === null && params.to === null;
  const boundsAreValid = params.from !== null && params.to !== null
    && Number.isInteger(params.from) && Number.isInteger(params.to)
    && params.from >= 0 && params.to <= 42 && params.from <= params.to;
  if (params.stage !== 'PREGNANCY' && !boundsAreEmpty) {
    return 'Pre-pregnancy and postpartum articles must be stage-wide.';
  }
  if (params.stage === 'PREGNANCY' && !boundsAreEmpty && !boundsAreValid) {
    return 'Pregnancy week bounds must be inclusive integers from 0 to 42.';
  }
  const catalogIds = new Set(params.catalog.map((tag) => tag.id));
  if (params.selectedTagIds.some((id) => !catalogIds.has(id))) {
    return 'The recommendation catalog changed. Refresh the catalog and review the selected audience.';
  }
  const groups = new Set<string>();
  for (const id of params.selectedTagIds) {
    const tag = params.catalog.find((candidate) => candidate.id === id);
    const group = tag ? groupForSlug(tag.slug) : null;
    if (group && groups.has(group)) {
      return 'Choose only one value from each exclusive audience group.';
    }
    if (group) groups.add(group);
  }
  return null;
}

export function recommendationWindowLabel(
  stage: ContentStage | '',
  from: number | null,
  to: number | null,
): string {
  if (stage !== 'PREGNANCY') return 'All of this lifecycle stage';
  if (from === null && to === null) return 'All pregnancy weeks';
  return `Pregnancy weeks ${from}–${to} (inclusive)`;
}

export function recommendationClassification(selectedTagIds: string[]): 'TARGETED' | 'FALLBACK' {
  return selectedTagIds.length > 0 ? 'TARGETED' : 'FALLBACK';
}

export function recommendationApiErrorCode(error: unknown): string | undefined {
  const response = (error as { response?: { data?: { error?: unknown } } })?.response;
  return typeof response?.data?.error === 'string' ? response.data.error : undefined;
}

export function recommendationApiErrorMessage(error: unknown): string {
  switch (recommendationApiErrorCode(error)) {
    case 'RECOMMENDATION_TAG_INVALID':
    case 'RECOMMENDATION_TAG_CONFLICT':
      return 'Recommendation audience tags are no longer valid. Refresh the catalog and review the selection.';
    case 'RECOMMENDATION_WEEK_RANGE_INVALID':
      return 'Review the pregnancy week range: both bounds are required, inclusive, and must be between 0 and 42.';
    case 'RECOMMENDATION_PRIORITY_INVALID':
      return 'Recommendation priority must be an integer from 0 to 100.';
    default:
      return 'Unable to save content. Please review the highlighted fields and try again.';
  }
}
