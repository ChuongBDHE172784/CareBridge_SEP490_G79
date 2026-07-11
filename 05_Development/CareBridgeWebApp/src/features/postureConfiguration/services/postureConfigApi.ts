import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../../auth/models/user';
import type {
  AdminPostureConfig,
  PostureConfigCreateForm,
  PostureConfigVersionForm,
} from '../models/postureConfig';

export async function createPostureConfig(
  payload: PostureConfigCreateForm,
): Promise<AdminPostureConfig> {
  const res = await apiClient.post<ApiResponse<AdminPostureConfig>>(
    '/api/v1/admin/posture-configs',
    payload,
  );
  return res.data.data;
}

export async function createPostureConfigVersion(
  exerciseId: string,
  payload: PostureConfigVersionForm,
): Promise<AdminPostureConfig> {
  const res = await apiClient.post<ApiResponse<AdminPostureConfig>>(
    `/api/v1/admin/posture-configs/${exerciseId}/versions`,
    payload,
  );
  return res.data.data;
}

export async function activatePostureConfig(postureConfigId: string): Promise<AdminPostureConfig> {
  const res = await apiClient.patch<ApiResponse<AdminPostureConfig>>(
    `/api/v1/admin/posture-configs/${postureConfigId}/activate`,
  );
  return res.data.data;
}

export async function fetchPostureConfigVersions(exerciseId: string): Promise<AdminPostureConfig[]> {
  const res = await apiClient.get<ApiResponse<AdminPostureConfig[]>>(
    `/api/v1/admin/posture-configs/${exerciseId}`,
  );
  return res.data.data;
}

