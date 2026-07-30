import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../../auth/models/user';
import type {
  AiPolicy,
  AiPolicyPage,
  AiModerationStatus,
  AiPolicyTestRequest,
  AiPolicyTestResult,
  CreateAiPolicyRequest,
  UpdateAiPolicyRequest,
} from '../models/aiModerationPolicy';

const BASE = '/api/v1/admin/ai-moderation';

// AiModerationController wraps every response in ApiResponse — read res.data.data.
// There is NO delete endpoint: system-default policies must never be hard-deleted;
// deactivate via updateAiPolicyStatus instead.
export async function fetchAiPolicies(params: {
  active?: boolean;
  page?: number;
  size?: number;
}): Promise<AiPolicyPage> {
  const res = await apiClient.get<ApiResponse<AiPolicyPage>>(`${BASE}/policies`, {
    params: {
      active: params.active,
      page: params.page ?? 0,
      size: params.size ?? 50,
    },
  });
  return res.data.data;
}

export async function createAiPolicy(request: CreateAiPolicyRequest): Promise<AiPolicy> {
  const res = await apiClient.post<ApiResponse<AiPolicy>>(`${BASE}/policies`, request);
  return res.data.data;
}

export async function updateAiPolicy(id: string, request: UpdateAiPolicyRequest): Promise<AiPolicy> {
  const res = await apiClient.put<ApiResponse<AiPolicy>>(`${BASE}/policies/${id}`, request);
  return res.data.data;
}

export async function updateAiPolicyStatus(id: string, active: boolean): Promise<AiPolicy> {
  const res = await apiClient.patch<ApiResponse<AiPolicy>>(`${BASE}/policies/${id}/status`, { active });
  return res.data.data;
}

export async function fetchAiModerationStatus(): Promise<AiModerationStatus> {
  const res = await apiClient.get<ApiResponse<AiModerationStatus>>(`${BASE}/status`);
  return res.data.data;
}

export async function testAiPolicy(request: AiPolicyTestRequest): Promise<AiPolicyTestResult> {
  const res = await apiClient.post<ApiResponse<AiPolicyTestResult>>(`${BASE}/test`, request);
  return res.data.data;
}

export async function uploadPolicyDocument(file: File): Promise<{
  fileId: string;
  fileName: string;
  fileUrl: string;
  fileSizeBytes: number;
}> {
  const isImage = file.type.startsWith('image/');
  const kind = isImage ? 'IMAGE' : 'DOCUMENT';
  const form = new FormData();
  form.append('file', file);
  form.append('kind', kind);
  form.append('purpose', 'AI_MODERATION_POLICY_DOCUMENT');
  form.append('accessMode', 'PRIVATE');

  const res = await apiClient.post<ApiResponse<{
    fileId: string;
    originalName: string;
    mimeType: string;
    fileSizeBytes: number;
    presignedUrl: string;
  }>>('/api/v1/files/upload/with-purpose', form, {
    headers: { 'Content-Type': undefined },
  });

  const d = res.data.data;
  return {
    fileId: d.fileId,
    fileName: d.originalName,
    fileUrl: d.presignedUrl,
    fileSizeBytes: d.fileSizeBytes,
  };
}
