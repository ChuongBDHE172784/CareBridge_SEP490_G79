import type { CommunityTopic } from '../models/content';

export interface TopicTreeRow {
  item: CommunityTopic;
  isChild: boolean;
}

// Fixes Logic Issue L4 (CommunityTopicManagement_Test-Spec.md §2): the previous ManageTopicsPage
// render logic nested EVERY category under EVERY expanded topic, because parentId didn't exist on
// the backend yet. Now that it does, only a CATEGORY/TAG whose parentId matches the topic's id is
// nested under it. CATEGORY/TAG rows with no parentId (e.g. created via ContentCategoryController,
// which never sets a parent — ADR-COM-016 revised) render as standalone top-level rows.
const bySortOrder = (a: CommunityTopic, b: CommunityTopic) => a.sortOrder - b.sortOrder;

// Callers update `sortOrder` via PATCH and merge the response back into the `topics` array in
// place (same array index), so the array's own order can lag behind `sortOrder` until a refetch.
// Sorting here — rather than relying on caller order — keeps the ▲/▼ move buttons reflected
// immediately without requiring a full list refetch after every swap.
export function buildTopicTree(topics: CommunityTopic[], expandedIds: Set<string>): TopicTreeRow[] {
  const topicItems = topics.filter((t) => t.type === 'TOPIC').sort(bySortOrder);
  const childItems = topics.filter((t) => t.type === 'CATEGORY' || t.type === 'TAG').sort(bySortOrder);

  const rows: TopicTreeRow[] = [];

  topicItems.forEach((topic) => {
    rows.push({ item: topic, isChild: false });
    if (expandedIds.has(topic.id)) {
      childItems
        .filter((child) => child.parentId === topic.id)
        .forEach((child) => rows.push({ item: child, isChild: true }));
    }
  });

  childItems
    .filter((child) => child.parentId === null)
    .forEach((child) => rows.push({ item: child, isChild: false }));

  return rows;
}
