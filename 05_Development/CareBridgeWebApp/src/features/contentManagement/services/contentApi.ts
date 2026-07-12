import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../../auth/models/user';
import type {
  ContentListItem,
  ContentDetail,
  ContentSearchItem,
  ChecklistTemplate,
  CommunityTopic,
  PaginatedResponse,
  ContentType,
  ContentStage,
  ContentStatus,
  ContentDecision,
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
  stage: ContentStage;
  topicId?: string;
}): Promise<CreateContentResult> {
  const res = await apiClient.post<ApiResponse<CreateContentResult>>('/api/v1/admin/content', data);
  return res.data.data;
}

export async function updateContent(
  id: string,
  data: {
    title: string;
    body: string;
    stage: ContentStage;
    topicId?: string;
    status: ContentStatus;
    sourceLabel?: string;
  },
): Promise<ContentDetail & { status: ContentStatus; versionNo: number }> {
  const res = await apiClient.put<ApiResponse<ContentDetail & { status: ContentStatus; versionNo: number }>>(
    `/api/v1/admin/content/${id}`,
    data,
  );
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
    `/api/v1/community/topics?includeHidden=${includeHidden}`,
  );
  return res.data.data;
}

export async function createTopic(data: {
  name: string;
  description?: string;
  icon?: string;
  sortOrder?: number;
}): Promise<CommunityTopic> {
  const res = await apiClient.post<ApiResponse<CommunityTopic>>('/api/v1/community/topics', data);
  return res.data.data;
}

export async function updateTopic(
  id: string,
  data: { name?: string; description?: string; icon?: string; isHidden?: boolean; sortOrder?: number },
): Promise<CommunityTopic> {
  const res = await apiClient.patch<ApiResponse<CommunityTopic>>(
    `/api/v1/community/topics/${id}`,
    data,
  );
  return res.data.data;
}

// UC-226 uses the Content Admin boundary. Community-topic routes are reserved
// for the Moderator workflow (UC-109), even though both workflows share data.
export async function fetchContentCategories(includeHidden = false): Promise<CommunityTopic[]> {
  const res = await apiClient.get<ApiResponse<CommunityTopic[]>>(
    `/api/v1/admin/content/categories?includeHidden=${includeHidden}`,
  );
  return res.data.data;
}

export async function createContentCategory(data: {
  name: string;
  description?: string;
  icon?: string;
  sortOrder?: number;
}): Promise<CommunityTopic> {
  const res = await apiClient.post<ApiResponse<CommunityTopic>>('/api/v1/admin/content/categories', data);
  return res.data.data;
}

export async function updateContentCategory(
  id: string,
  data: { name?: string; description?: string; icon?: string; isHidden?: boolean; sortOrder?: number },
): Promise<CommunityTopic> {
  const res = await apiClient.patch<ApiResponse<CommunityTopic>>(
    `/api/v1/admin/content/categories/${id}`,
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
