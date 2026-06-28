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

export async function createContent(data: {
  type: ContentType;
  title: string;
  body: string;
  stage: ContentStage;
  topicId: string;
}) {
  const res = await apiClient.post('/api/v1/admin/content', data);
  return res.data.data;
}

export async function fetchTopics(includeHidden = false): Promise<CommunityTopic[]> {
  const res = await apiClient.get<ApiResponse<CommunityTopic[]>>(
    `/api/v1/community/topics?includeHidden=${includeHidden}`,
  );
  return res.data.data;
}
