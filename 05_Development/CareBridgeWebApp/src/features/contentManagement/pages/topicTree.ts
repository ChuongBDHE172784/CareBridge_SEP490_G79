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
export function buildTopicTree(topics: CommunityTopic[], expandedIds: Set<string>): TopicTreeRow[] {
  const topicItems = topics.filter((t) => t.type === 'TOPIC');
  const childItems = topics.filter((t) => t.type === 'CATEGORY' || t.type === 'TAG');

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
