import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../../auth/models/user';
import type {
  ContentListItem,
  ContentDetail,
  ContentSearchItem,
  ChecklistTemplate,
  AdminChecklistTemplateDetail,
  CreateChecklistTemplatePayload,
  UpdateChecklistTemplatePayload,
  CommunityTopic,
  PaginatedResponse,
  ContentType,
  ContentStage,
  ContentStatus,
  ContentDecision,
  ContentSource,
  CreateCommunityTopicPayload,
  UpdateCommunityTopicPayload,
  AdminChecklistTemplate,
  ChecklistTemplateStatus,
  ContentVersionSnapshot,
  ChecklistTemplateVersionSnapshot,
  RecommendationTagCatalog,
} from '../models/content';

export async function fetchContentList(params: {
  type?: ContentType;
  stage?: ContentStage;
  topicId?: string;
  page?: number;
  size?: number;
}): Promise<PaginatedResponse<ContentListItem>> {
  const q = new URLSearchParams();
  if (params.type) q.set('type', params.type);
  if (params.stage) q.set('stage', params.stage);
  if (params.topicId) q.set('topicId', params.topicId);
  q.set('page', String(params.page ?? 0));
  q.set('size', String(params.size ?? 10));
  const res = await apiClient.get<ApiResponse<ContentListItem[]> & { page: number; size: number; totalElements: number; totalPages: number }>(
    `/api/v1/content?${q}`,
  );
  const body = res.data;
  return {
    content: body.data ?? [],
    totalElements: body.totalElements ?? 0,
    totalPages: body.totalPages ?? 0,
    size: body.size ?? (params.size ?? 10),
    number: body.page ?? (params.page ?? 0),
  };
}

export async function fetchContentDetail(id: string): Promise<ContentDetail> {
  const res = await apiClient.get<ApiResponse<ContentDetail>>(`/api/v1/content/${id}`);
  return res.data.data;
}

/** Staff route intentionally includes drafts/pending items; public content routes never do. */
export async function fetchStaffContentDetail(id: string): Promise<ContentDetail> {
  const res = await apiClient.get<ApiResponse<ContentDetail>>(`/api/v1/admin/content/${id}`);
  return res.data.data;
}

export async function fetchContentVersionHistory(id: string): Promise<ContentVersionSnapshot[]> {
  const res = await apiClient.get<ApiResponse<ContentVersionSnapshot[]>>(`/api/v1/admin/content/${id}/versions`);
  return res.data.data ?? [];
}

export async function fetchStaffContentList(params: {
  status?: ContentStatus;
  type?: ContentType;
  stage?: ContentStage;
  keyword?: string;
  page?: number;
  size?: number;
} = {},
): Promise<PaginatedResponse<ContentDetail>> {
  const requestedSize = params.size ?? 50;
  const safeSize = Math.min(Math.max(1, requestedSize), 50);
  const res = await apiClient.get<ApiResponse<PaginatedResponse<ContentDetail>>>('/api/v1/admin/content', {
    params: { ...params, page: params.page ?? 0, size: safeSize },
  });
  return res.data.data;
}


export async function searchContent(params: {
  keyword: string;
  type?: ContentType;
  stage?: ContentStage;
  topicId?: string;
  page?: number;
  size?: number;
}): Promise<PaginatedResponse<ContentSearchItem>> {
  const q = new URLSearchParams({ keyword: params.keyword });
  if (params.type) q.set('type', params.type);
  if (params.stage) q.set('stage', params.stage);
  if (params.topicId) q.set('topicId', params.topicId);
  q.set('page', String(params.page ?? 0));
  q.set('size', String(params.size ?? 10));
  const res = await apiClient.get<ApiResponse<PaginatedResponse<ContentSearchItem>>>(
    `/api/v1/content/search?${q}`,
  );
  return res.data.data;
}

export async function fetchChecklists(stage?: ContentStage): Promise<ChecklistTemplate[]> {
  const q = stage ? `?stage=${stage}` : '';
  const res = await apiClient.get<ApiResponse<ChecklistTemplate[]>>(
    `/api/v1/content/checklists${q}`,
  );
  return res.data.data;
}

/** Admin workspace (UC-243): includes DRAFT/PENDING_REVIEW/ARCHIVED, unlike the public list above. */
export async function fetchAdminChecklistTemplates(params: {
  status?: ChecklistTemplateStatus;
  stage?: ContentStage;
  page?: number;
  size?: number;
} = {}): Promise<PaginatedResponse<AdminChecklistTemplateDetail>> {
  const res = await apiClient.get<ApiResponse<PaginatedResponse<AdminChecklistTemplateDetail>>>(
    '/api/v1/admin/checklist-templates',
    { params: { ...params, page: params.page ?? 0, size: params.size ?? 20 } },
  );
  return res.data.data;
}

export async function fetchChecklistTemplateDetail(id: string): Promise<AdminChecklistTemplateDetail> {
  const res = await apiClient.get<ApiResponse<AdminChecklistTemplateDetail>>(`/api/v1/admin/checklist-templates/${id}`);
  return res.data.data;
}

export async function fetchChecklistVersionHistory(id: string): Promise<ChecklistTemplateVersionSnapshot[]> {
  const res = await apiClient.get<ApiResponse<ChecklistTemplateVersionSnapshot[]>>(
    `/api/v1/admin/checklist-templates/${id}/versions`,
  );
  return res.data.data ?? [];
}

export async function createChecklistTemplate(data: CreateChecklistTemplatePayload): Promise<AdminChecklistTemplateDetail> {
  const res = await apiClient.post<ApiResponse<AdminChecklistTemplateDetail>>('/api/v1/admin/checklist-templates', data);
  return res.data.data;
}

export async function updateChecklistTemplate(
  id: string,
  data: UpdateChecklistTemplatePayload,
): Promise<AdminChecklistTemplateDetail> {
  const res = await apiClient.put<ApiResponse<AdminChecklistTemplateDetail>>(`/api/v1/admin/checklist-templates/${id}`, data);
  return res.data.data;
}

/** Soft-delete: transitions the template to ARCHIVED (no hard delete). */
export async function archiveChecklistTemplate(
  id: string,
  reason: string,
): Promise<{ previousStatus: ContentStatus; newStatus: ContentStatus }> {
  const res = await apiClient.post<ApiResponse<{ previousStatus: ContentStatus; newStatus: ContentStatus }>>(
    `/api/v1/admin/checklist-templates/${id}/archive`, { reason },
  );
  return res.data.data;
}

/** SYSTEM_ADMIN-only approval decision (UC-243 §14 addendum). */
export async function decideChecklistTemplate(
  id: string,
  decision: ContentDecision,
  reason?: string,
): Promise<{ previousStatus: ContentStatus; newStatus: ContentStatus }> {
  const res = await apiClient.post<ApiResponse<{ previousStatus: ContentStatus; newStatus: ContentStatus }>>(
    `/api/v1/admin/checklist-templates/${id}/decision`, { decision, reason },
  );
  return res.data.data;
}

export async function cloneChecklistVersion(
  lineageId: string,
  versionId: string,
): Promise<AdminChecklistTemplateDetail> {
  const res = await apiClient.post<ApiResponse<AdminChecklistTemplateDetail>>(
    `/api/v1/admin/checklist-templates/${lineageId}/versions/${versionId}/clone`,
  );
  return res.data.data;
}

export async function approveChecklistVersion(
  lineageId: string,
  versionId: string,
): Promise<{ previousStatus: ChecklistTemplateStatus; newStatus: ChecklistTemplateStatus }> {
  const res = await apiClient.post<ApiResponse<{ previousStatus: ChecklistTemplateStatus; newStatus: ChecklistTemplateStatus }>>(
    `/api/v1/admin/checklist-templates/${lineageId}/versions/${versionId}/approve`,
  );
  return res.data.data;
}

export async function reviewMigratedChecklistVersion(
  lineageId: string,
  versionId: string,
): Promise<{ previousStatus: ChecklistTemplateStatus; newStatus: ChecklistTemplateStatus }> {
  const res = await apiClient.post<ApiResponse<{ previousStatus: ChecklistTemplateStatus; newStatus: ChecklistTemplateStatus }>>(
    `/api/v1/admin/checklist-templates/${lineageId}/versions/${versionId}/review`,
  );
  return res.data.data;
}

export async function activateChecklistVersion(
  lineageId: string,
  versionId: string,
): Promise<{ previousStatus: ChecklistTemplateStatus; newStatus: ChecklistTemplateStatus }> {
  const res = await apiClient.post<ApiResponse<{ previousStatus: ChecklistTemplateStatus; newStatus: ChecklistTemplateStatus }>>(
    `/api/v1/admin/checklist-templates/${lineageId}/versions/${versionId}/activate`,
  );
  return res.data.data;
}

/**
 * Uploads an image for embedding in article/FAQ rich text content. Reuses the generic
 * file-upload endpoint (ADR-RTE-003) with purpose=PUBLIC_CONTENT_IMAGE, accessMode=PUBLIC —
 * the backend returns a permanent, non-expiring Cloudinary URL for this combination
 * (ADR-RTE-004), unlike the 15-minute presigned URLs used elsewhere for private files.
 */
export async function uploadContentImage(file: File): Promise<string> {
  const form = new FormData();
  form.append('file', file);
  form.append('kind', 'IMAGE');
  form.append('purpose', 'PUBLIC_CONTENT_IMAGE');
  form.append('accessMode', 'PUBLIC');
  const res = await apiClient.post<ApiResponse<{ presignedUrl: string }>>(
    '/api/v1/files/upload/with-purpose',
    form,
    { headers: { 'Content-Type': undefined } },
  );
  return res.data.data.presignedUrl;
}

/** Story 6.9 read-only admin projection with real template status and item counts. */
export async function fetchAdminChecklists(params: {
  stage?: ContentStage;
  status?: ChecklistTemplateStatus;
  keyword?: string;
  page?: number;
  size?: number;
} = {}): Promise<PaginatedResponse<AdminChecklistTemplate>> {
  const res = await apiClient.get<
    ApiResponse<AdminChecklistTemplate[]> & {
      page: number;
      size: number;
      totalElements: number;
      totalPages: number;
    }
  >('/api/v1/admin/content/checklists', {
    params: {
      ...(params.stage ? { stage: params.stage } : {}),
      ...(params.status ? { status: params.status } : {}),
      ...(params.keyword ? { keyword: params.keyword } : {}),
      page: params.page ?? 0,
      size: params.size ?? 10,
    },
  });
  const body = res.data;
  return {
    content: body.data ?? [],
    number: body.page ?? (params.page ?? 0),
    size: body.size ?? (params.size ?? 10),
    totalElements: body.totalElements ?? 0,
    totalPages: body.totalPages ?? 0,
  };
}

export interface CreateContentResult {
  id: string;
  type: ContentType;
  title: string;
  stage: ContentStage;
  status: string;
  version: number;
  createdAt: string;
}

export async function createContent(data: {
  type: ContentType;
  title: string;
  body: string;
  summary?: string;
  stage: ContentStage;
  topicId?: string;
  tagIds?: string[];
  eligibleFromWeek?: number | null;
  eligibleToWeek?: number | null;
  recommendationPriority?: number;
  sources?: ContentSource[];
}): Promise<CreateContentResult> {
  const res = await apiClient.post<ApiResponse<CreateContentResult>>('/api/v1/admin/content', data);
  return res.data.data;
}

export async function updateContent(
  id: string,
  data: {
    title: string;
    body: string;
    summary?: string;
    stage: ContentStage;
    topicId?: string;
    tagIds?: string[];
    eligibleFromWeek?: number | null;
    eligibleToWeek?: number | null;
    recommendationPriority?: number;
    status: ContentStatus;
    sourceLabel?: string;
    sources?: ContentSource[];
  },
): Promise<ContentDetail & { status: ContentStatus; versionNo: number }> {
  const res = await apiClient.put<ApiResponse<ContentDetail & { status: ContentStatus; versionNo: number }>>(
    `/api/v1/admin/content/${id}`,
    data,
  );
  return res.data.data;
}

export async function fetchRecommendationTags(): Promise<RecommendationTagCatalog> {
  const res = await apiClient.get<ApiResponse<RecommendationTagCatalog>>('/api/v1/admin/content/recommendation-tags');
  return res.data.data;
}

export async function decideContent(
  id: string,
  decision: ContentDecision,
  reason?: string,
): Promise<{ id: string; previousStatus: ContentStatus; newStatus: ContentStatus; decidedAt: string }> {
  const res = await apiClient.post<
    ApiResponse<{ id: string; previousStatus: ContentStatus; newStatus: ContentStatus; decidedAt: string }>
  >(`/api/v1/admin/content/${id}/decision`, { decision, reason });
  return res.data.data;
}

export async function fetchTopics(includeHidden = false): Promise<CommunityTopic[]> {
  const res = await apiClient.get<ApiResponse<CommunityTopic[]>>(
    `/api/v1/community/topics?includeHidden=${includeHidden}&type=TOPIC`,
  );
  return res.data.data;
}

export async function fetchTags(): Promise<CommunityTopic[]> {
  const res = await apiClient.get<ApiResponse<CommunityTopic[]>>(
    '/api/v1/community/topics?includeHidden=false&type=TAG',
  );
  return res.data.data;
}

export async function createTopic(
  data: Omit<Extract<CreateCommunityTopicPayload, { type: 'TOPIC' }>, 'type'>,
): Promise<CommunityTopic> {
  const res = await apiClient.post<ApiResponse<CommunityTopic>>('/api/v1/community/topics', {
    ...data,
    type: 'TOPIC',
  });
  return res.data.data;
}

export async function updateTopic(
  id: string,
  data: UpdateCommunityTopicPayload,
): Promise<CommunityTopic> {
  const res = await apiClient.patch<ApiResponse<CommunityTopic>>(
    `/api/v1/community/topics/${id}`,
    data,
  );
  return res.data.data;
}

export async function unpublishContent(id: string, reason: string): Promise<{ previousStatus: ContentStatus; newStatus: ContentStatus }> {
  const res = await apiClient.post<ApiResponse<{ previousStatus: ContentStatus; newStatus: ContentStatus }>>(
    `/api/v1/admin/content/${id}/unpublish`, { reason },
  );
  return res.data.data;
}

/** Soft-deletes content by transitioning it to ARCHIVED (no hard delete — keeps audit history). */
export async function archiveContent(id: string, reason: string): Promise<{ previousStatus: ContentStatus; newStatus: ContentStatus }> {
  const res = await apiClient.post<ApiResponse<{ previousStatus: ContentStatus; newStatus: ContentStatus }>>(
    `/api/v1/admin/content/${id}/archive`, { reason },
  );
  return res.data.data;
}

export interface BulkImportItemPayload {
  rowIndex: number;
  title: string;
  body: string;
  summary?: string;
  stage: string;
  categoryName?: string;
  topicId?: string;
  sourceLabel?: string;
  sourceUrl?: string;
  sourcePublisher?: string;
}

export interface BulkImportResult {
  totalRows: number;
  successCount: number;
  failedCount: number;
  errors: string[];
  createdIds: string[];
}

export async function importContentBatch(data: {
  type: ContentType;
  items: BulkImportItemPayload[];
}): Promise<BulkImportResult> {
  const res = await apiClient.post<ApiResponse<BulkImportResult>>('/api/v1/admin/content/import-batch', data);
  return res.data.data;
}
