export type ContentType = 'ARTICLE' | 'FAQ' | 'CHECKLIST';
export type ContentStage = 'PRE_PREGNANCY' | 'PREGNANCY' | 'POSTPARTUM' | 'BABY_CARE';
export type ContentStatus = 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'ARCHIVED';
export type ContentDecision = 'APPROVE' | 'REJECT';

export interface ContentListItem {
  id: string;
  type: ContentType;
  title: string;
  stage: ContentStage;
  topicId: string;
  publishedAt: string | null;
}

export interface ContentDetail {
  id: string;
  type: ContentType;
  title: string;
  body: string;
  stage: ContentStage;
  topicId: string;
  version: number;
  publishedAt: string | null;
  status: ContentStatus;
  createdAt: string;
  sourceLabel?: string | null;
  sources?: ContentSource[];
}

export interface ContentSource { title: string; url?: string; publisher?: string; }

export interface ContentSearchItem {
  id: string;
  type: ContentType;
  title: string;
  stage: ContentStage;
  topicName: string;
  publishedAt: string | null;
}

export interface ChecklistTemplate {
  id: string;
  name: string;
  stage: ContentStage;
  status: ContentStatus;
  description: string;
  items: ChecklistItem[];
}

export interface ChecklistItem {
  id: string;
  itemText: string;
  order: number;
  isRequired: boolean;
}

export interface ChecklistItemInput {
  itemText: string;
  order: number;
  isRequired: boolean;
}

export interface CreateChecklistTemplatePayload {
  name: string;
  description?: string;
  stage: ContentStage;
  items: ChecklistItemInput[];
}

export interface UpdateChecklistTemplatePayload {
  name: string;
  description?: string;
  stage: ContentStage;
  status: ContentStatus;
  // null/undefined = keep existing items unchanged; [] = clear all; non-empty = full replace
  items?: ChecklistItemInput[] | null;
}

export type CommunityTopicType = 'TOPIC' | 'CATEGORY' | 'TAG';

interface CommunityTopicMutationFields {
  name: string;
  description?: string;
  icon?: string;
  sortOrder?: number;
}

export type CreateCommunityTopicPayload = CommunityTopicMutationFields & (
  | { type: 'TOPIC'; parentId: string }
  | { type: 'CATEGORY' | 'TAG'; parentId: null }
);

// Type is intentionally absent: ADR-COM-025 makes it immutable after creation.
export interface UpdateCommunityTopicPayload extends Partial<CommunityTopicMutationFields> {
  parentId?: string | null;
  isHidden?: boolean;
}

export interface CommunityTopic {
  id: string;
  name: string;
  description: string;
  icon: string;
  type: CommunityTopicType;
  slug: string;
  parentId: string | null;
  questionCount: number;
  isHidden: boolean;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const STAGE_LABELS: Record<ContentStage, string> = {
  PRE_PREGNANCY: 'Chuẩn bị',
  PREGNANCY: 'Thai kỳ',
  POSTPARTUM: 'Sau sinh',
  BABY_CARE: 'Chăm bé',
};

export const TYPE_LABELS: Record<ContentType, string> = {
  ARTICLE: 'Bài viết',
  FAQ: 'FAQ',
  CHECKLIST: 'Checklist',
};

export const STATUS_LABELS: Record<ContentStatus, string> = {
  DRAFT: 'Bản nháp',
  PENDING_REVIEW: 'Chờ phê duyệt',
  APPROVED: 'Đã xuất bản',
  ARCHIVED: 'Đã lưu trữ',
};
