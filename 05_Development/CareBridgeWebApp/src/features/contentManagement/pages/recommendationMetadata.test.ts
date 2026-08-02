import { describe, expect, it } from 'vitest';
import {
  recommendationClassification,
  recommendationMetadataError,
  recommendationWindowLabel,
} from './recommendationMetadata';
import type { RecommendationTag } from '../models/content';

const catalog: RecommendationTag[] = [
  { id: 'age-young', slug: 'rec-age-under-18', domain: 'AGE', label: 'Age: under 18' },
  { id: 'age-adult', slug: 'rec-age-18-24', domain: 'AGE', label: 'Age: 18-24' },
  { id: 'nutrition', slug: 'rec-nutrition-vegetarian', domain: 'NUTRITION', label: 'Nutrition: vegetarian' },
];

describe('recommendation metadata authoring rules', () => {
  it('rejects two values from one exclusive audience group', () => {
    expect(recommendationMetadataError({
      type: 'ARTICLE',
      stage: 'PREGNANCY',
      from: null,
      to: null,
      priority: 10,
      selectedTagIds: ['age-young', 'age-adult'],
      catalog,
    })).toContain('exclusive');
  });

  it('requires pregnancy bounds to be paired and keeps other stages stage-wide', () => {
    expect(recommendationMetadataError({
      type: 'ARTICLE',
      stage: 'POSTPARTUM',
      from: 1,
      to: 2,
      priority: 0,
      selectedTagIds: [],
      catalog,
    })).toContain('stage-wide');
    expect(recommendationMetadataError({
      type: 'ARTICLE',
      stage: 'PREGNANCY',
      from: 3,
      to: null,
      priority: 0,
      selectedTagIds: [],
      catalog,
    })).toContain('bounds');
  });

  it('fails closed for a stale catalog id and exposes deterministic summary labels', () => {
    expect(recommendationMetadataError({
      type: 'ARTICLE',
      stage: 'PREGNANCY',
      from: 4,
      to: 8,
      priority: 50,
      selectedTagIds: ['retired-id'],
      catalog,
    })).toContain('catalog changed');
    expect(recommendationClassification([])).toBe('FALLBACK');
    expect(recommendationClassification(['nutrition'])).toBe('TARGETED');
    expect(recommendationWindowLabel('PREGNANCY', 4, 8)).toContain('4');
  });
});
