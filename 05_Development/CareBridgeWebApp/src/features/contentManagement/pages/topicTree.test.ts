import { describe, expect, it } from 'vitest';
import type { CommunityTopic } from '../models/content';
import { buildTopicTree } from './topicTree';

const topic = (overrides: Partial<CommunityTopic>): CommunityTopic => ({
  id: 'id',
  name: 'name',
  description: '',
  icon: '',
  type: 'TOPIC',
  slug: 'slug',
  parentId: null,
  questionCount: 0,
  isHidden: false,
  sortOrder: 0,
  createdAt: '2026-07-21T00:00:00Z',
  updatedAt: '2026-07-21T00:00:00Z',
  ...overrides,
});

// WEB-TC-001: a CATEGORY nests only under its real parentId, not under every expanded topic.
describe('buildTopicTree', () => {
  it('nests a category only under its real parent topic, not under every expanded topic', () => {
    const topicA = topic({ id: 'topic-a', name: 'Topic A', type: 'TOPIC' });
    const topicB = topic({ id: 'topic-b', name: 'Topic B', type: 'TOPIC' });
    const categoryX = topic({ id: 'category-x', name: 'Category X', type: 'CATEGORY', parentId: 'topic-a' });

    const rows = buildTopicTree([topicA, topicB, categoryX], new Set(['topic-a', 'topic-b']));

    const categoryRows = rows.filter((r) => r.item.id === 'category-x');
    expect(categoryRows).toHaveLength(1);

    const topicAIndex = rows.findIndex((r) => r.item.id === 'topic-a');
    const categoryIndex = rows.findIndex((r) => r.item.id === 'category-x');
    const topicBIndex = rows.findIndex((r) => r.item.id === 'topic-b');
    expect(categoryIndex).toBeGreaterThan(topicAIndex);
    expect(categoryIndex).toBeLessThan(topicBIndex);
    expect(rows[categoryIndex].isChild).toBe(true);
  });

  it('does not render a category under a topic when that topic is not expanded', () => {
    const topicA = topic({ id: 'topic-a', type: 'TOPIC' });
    const categoryX = topic({ id: 'category-x', type: 'CATEGORY', parentId: 'topic-a' });

    const rows = buildTopicTree([topicA, categoryX], new Set());

    expect(rows.some((r) => r.item.id === 'category-x')).toBe(false);
  });

  it('renders a parentless category (e.g. from ContentCategoryController) as a standalone top-level row', () => {
    const standaloneCategory = topic({ id: 'standalone', type: 'CATEGORY', parentId: null });

    const rows = buildTopicTree([standaloneCategory], new Set());

    expect(rows).toHaveLength(1);
    expect(rows[0].isChild).toBe(false);
  });
});
