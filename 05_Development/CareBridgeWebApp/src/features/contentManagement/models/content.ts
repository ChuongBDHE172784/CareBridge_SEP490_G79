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
  description: string;
  items: ChecklistItem[];
}

export interface ChecklistItem {
  id: string;
  itemText: string;
  order: number;
  isRequired: boolean;
}

export interface CommunityTopic {
  id: string;
  name: string;
  description: string;
  icon: string;
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
  PRE_PREGNANCY: 'Chuan bi',
  PREGNANCY: 'Thai ky',
  POSTPARTUM: 'Sau sinh',
  BABY_CARE: 'Cham be',
};

export const TYPE_LABELS: Record<ContentType, string> = {
  ARTICLE: 'Bai viet',
  FAQ: 'FAQ',
  CHECKLIST: 'Checklist',
};

export const STATUS_LABELS: Record<ContentStatus, string> = {
  DRAFT: 'Ban nhap',
  PENDING_REVIEW: 'Cho phe duyet',
  APPROVED: 'Da xuat ban',
  ARCHIVED: 'Luu tru',
};
