export type ContentType = 'ARTICLE' | 'FAQ' | 'CHECKLIST';
export type ContentStage = 'PRE_PREGNANCY' | 'PREGNANCY' | 'POSTPARTUM' | 'BABY_CARE';
export type ContentStatus = 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'ARCHIVED';
export type ChecklistTemplateStatus = 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'ARCHIVED';
export type ChecklistTemplateType = 'MANDATORY' | 'OPTIONAL';
export type ContentDecision = 'APPROVE' | 'REJECT';
export type ChecklistRecipientRole = 'MOTHER' | 'FAMILY';
export type ChecklistTargetSubject = 'MOTHER' | 'BABY';
export type ChecklistAnchorType = 'NONE' | 'LMP' | 'EDD' | 'DELIVERY_DATE' | 'BIRTH_DATE';
export type ChecklistRangeUnit = 'DAY' | 'WEEK' | 'MONTH';

export interface ChecklistSubstage {
  code: string;
  anchor: ChecklistAnchorType;
  startInclusive: number;
  endInclusive: number;
  unit: ChecklistRangeUnit;
}

export interface ReviewFeedback {
  reason: string;
  requestedAt: string | null;
  requestedBy: string | null;
  versionNo: number | null;
}

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
  summary?: string | null;
  stage: ContentStage;
  topicId: string;
  tagIds?: string[];
  version: number;
  publishedAt: string | null;
  status: ContentStatus;
  createdAt: string;
  updatedAt?: string | null;
  sourceLabel?: string | null;
  sources?: ContentSource[];
  latestReviewFeedback?: ReviewFeedback | null;
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
  stage: ContentStage | null;
  status: ChecklistTemplateStatus;
  description: string;
  templateType: ChecklistTemplateType;
  items: ChecklistItem[];
  latestReviewFeedback?: ReviewFeedback | null;
}

export interface ContentVersionSnapshot {
  versionNo: number;
  title: string;
  stage: string | null;
  status: string;
  sourceSummary: string | null;
  changedBy: string | null;
  createdAt: string;
}

export interface ChecklistTemplateVersionSnapshot {
  versionNo: number;
  name: string;
  stage: string | null;
  status: string;
  itemCount: number;
  changedBy: string | null;
  createdAt: string;
}

export interface AdminChecklistTemplateDetail extends ChecklistTemplate {
  versionNo: number;
  lineageId: string;
  versionId: string;
  recipientRoles: ChecklistRecipientRole[];
  substage: ChecklistSubstage | null;
  migrationReviewRequired: boolean;
  distributionEnabled: boolean;
  approvedAt: string | null;
  approvedBy: string | null;
  migrationReviewedAt?: string | null;
  migrationReviewedBy?: string | null;
}

export interface ChecklistItem {
  id: string;
  itemText: string;
  order: number;
  isRequired: boolean;
  targetSubject: ChecklistTargetSubject;
}

export interface ChecklistItemInput {
  id?: string;
  itemText: string;
  order: number;
  isRequired: boolean;
  targetSubject: ChecklistTargetSubject;
}

export interface CreateChecklistTemplatePayload {
  name: string;
  description?: string;
  templateType: ChecklistTemplateType;
  recipientRoles: ChecklistRecipientRole[];
  stage: ContentStage | null;
  substage: ChecklistSubstage | null;
  items: ChecklistItemInput[];
}

export interface UpdateChecklistTemplatePayload {
  name: string;
  description?: string;
  templateType: ChecklistTemplateType;
  recipientRoles: ChecklistRecipientRole[];
  stage: ContentStage | null;
  substage: ChecklistSubstage | null;
  status: ChecklistTemplateStatus;
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

export interface AdminChecklistTemplate {
  id: string;
  name: string;
  lineageId?: string;
  versionId?: string;
  recipientRoles?: ChecklistRecipientRole[];
  stage: ContentStage | null;
  substage?: ChecklistSubstage | null;
  status: ChecklistTemplateStatus;
  description: string;
  templateType?: ChecklistTemplateType;
  versionNo: number;
  updatedAt: string | null;
  itemCount: number;
  migrationReviewRequired?: boolean;
  distributionEnabled?: boolean;
  latestReviewFeedback?: ReviewFeedback | null;
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
  PRE_PREGNANCY: 'Muốn mang thai',
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

export const CHECKLIST_STATUS_LABELS: Record<ChecklistTemplateStatus, string> = {
  DRAFT: 'Bản nháp',
  PENDING_REVIEW: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Đã từ chối',
  ARCHIVED: 'Đã lưu trữ',
};
