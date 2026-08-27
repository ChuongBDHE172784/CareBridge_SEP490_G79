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
  parentId: 'category-id',
  questionCount: 0,
  isHidden: false,
  sortOrder: 0,
  createdAt: '2026-07-21T00:00:00Z',
  updatedAt: '2026-07-21T00:00:00Z',
  ...overrides,
});

// WEB-TC-001/002: CATEGORY is the root, TOPIC is its child, and TAG stays flat.
describe('buildTopicTree', () => {
  it('nests a topic only under its real parent category', () => {
    const categoryA = topic({ id: 'category-a', name: 'Category A', type: 'CATEGORY', parentId: null });
    const categoryB = topic({ id: 'category-b', name: 'Category B', type: 'CATEGORY', parentId: null });
    const topicX = topic({ id: 'topic-x', name: 'Topic X', parentId: 'category-a' });

    const rows = buildTopicTree([categoryA, categoryB, topicX], new Set(['category-a', 'category-b']));

    expect(rows.map((row) => [row.item.id, row.isChild])).toEqual([
      ['category-a', false],
      ['topic-x', true],
      ['category-b', false],
    ]);
  });

  it('does not render a topic when its parent category is collapsed', () => {
    const category = topic({ id: 'category', type: 'CATEGORY', parentId: null });
    const childTopic = topic({ id: 'child-topic', parentId: 'category' });

    const rows = buildTopicTree([category, childTopic], new Set());

    expect(rows.map((row) => row.item.id)).toEqual(['category']);
  });

  it('renders tags as a separate flat list', () => {
    const category = topic({ id: 'category', type: 'CATEGORY', parentId: null });
    const childTopic = topic({ id: 'child-topic', parentId: 'category' });
    const tag = topic({ id: 'tag', type: 'TAG', parentId: null });

    const rows = buildTopicTree([category, childTopic, tag], new Set(['category']));

    expect(rows.map((row) => [row.item.id, row.isChild])).toEqual([
      ['category', false],
      ['child-topic', true],
      ['tag', false],
    ]);
  });

  it('orders categories, child topics, and tags by sortOrder rather than array position', () => {
    const categoryA = topic({ id: 'category-a', type: 'CATEGORY', parentId: null, sortOrder: 2 });
    const categoryB = topic({ id: 'category-b', type: 'CATEGORY', parentId: null, sortOrder: 1 });
    const topicA = topic({ id: 'topic-a', parentId: 'category-a', sortOrder: 2 });
    const topicB = topic({ id: 'topic-b', parentId: 'category-a', sortOrder: 1 });
    const tagA = topic({ id: 'tag-a', type: 'TAG', parentId: null, sortOrder: 2 });
    const tagB = topic({ id: 'tag-b', type: 'TAG', parentId: null, sortOrder: 1 });

    const rows = buildTopicTree(
      [categoryA, topicA, tagA, categoryB, topicB, tagB],
      new Set(['category-a', 'category-b']),
    );

    expect(rows.map((row) => row.item.id)).toEqual([
      'category-b',
      'category-a',
      'topic-b',
      'topic-a',
      'tag-b',
      'tag-a',
    ]);
  });
});
