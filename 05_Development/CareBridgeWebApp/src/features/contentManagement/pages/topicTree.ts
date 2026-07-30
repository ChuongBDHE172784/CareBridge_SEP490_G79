import type { CommunityTopic } from '../models/content';

export interface TopicTreeRow {
  item: CommunityTopic;
  isChild: boolean;
}

// Amendment 2 (ADR-COM-020): CATEGORY is the root, TOPIC is its child, and TAG stays flat.
export type TopicSorter = (topics: CommunityTopic[]) => CommunityTopic[];

const bySortOrder: TopicSorter = (topics) => [...topics].sort((a, b) => a.sortOrder - b.sortOrder);

// Callers update `sortOrder` via PATCH and merge the response back into the `topics` array in
// place (same array index), so the array's own order can lag behind `sortOrder` until a refetch.
// Sorting here — rather than relying on caller order — keeps the ▲/▼ move buttons reflected
// immediately without requiring a full list refetch after every swap.
export function buildTopicTree(
  topics: CommunityTopic[],
  expandedIds: Set<string>,
  sortTopics: TopicSorter = bySortOrder,
): TopicTreeRow[] {
  const categories = sortTopics(topics.filter((topic) => topic.type === 'CATEGORY'));
  const topicItems = sortTopics(topics.filter((topic) => topic.type === 'TOPIC'));
  const tags = sortTopics(topics.filter((topic) => topic.type === 'TAG'));

  const rows: TopicTreeRow[] = [];

  categories.forEach((category) => {
    rows.push({ item: category, isChild: false });
    if (expandedIds.has(category.id)) {
      topicItems
        .filter((topic) => topic.parentId === category.id)
        .forEach((topic) => rows.push({ item: topic, isChild: true }));
    }
  });

  tags.forEach((tag) => rows.push({ item: tag, isChild: false }));

  return rows;
}
