import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../../auth/models/user';
import type {
  AdminExercise,
  AdminExerciseForm,
  DifficultyLevel,
  ExerciseStatus,
  PaginatedResponse,
  TrimesterScope,
} from '../models/adminExercise';

export async function fetchAdminExercises(params: {
  status?: ExerciseStatus;
  trimester?: TrimesterScope;
  difficulty?: DifficultyLevel;
  page?: number;
  size?: number;
}): Promise<PaginatedResponse<AdminExercise>> {
  const q = new URLSearchParams();
  if (params.status) q.set('status', params.status);
  if (params.trimester) q.set('trimester', params.trimester);
  if (params.difficulty) q.set('difficulty', params.difficulty);
  q.set('page', String(params.page ?? 0));
  q.set('size', String(params.size ?? 20));
  const res = await apiClient.get<PaginatedResponse<AdminExercise>>(
    `/api/v1/admin/exercises?${q}`,
  );
  return res.data;
}

export async function fetchAdminExercise(exerciseId: string): Promise<AdminExercise> {
  const res = await apiClient.get<ApiResponse<AdminExercise>>(
    `/api/v1/admin/exercises/${exerciseId}`,
  );
  return res.data.data;
}

export async function createAdminExercise(payload: AdminExerciseForm): Promise<AdminExercise> {
  const res = await apiClient.post<ApiResponse<AdminExercise>>('/api/v1/admin/exercises', payload);
  return res.data.data;
}

export async function updateAdminExercise(
  exerciseId: string,
  payload: AdminExerciseForm,
): Promise<AdminExercise> {
  const res = await apiClient.put<ApiResponse<AdminExercise>>(
    `/api/v1/admin/exercises/${exerciseId}`,
    payload,
  );
  return res.data.data;
}

export async function activateAdminExercise(exerciseId: string): Promise<AdminExercise> {
  const res = await apiClient.patch<ApiResponse<AdminExercise>>(
    `/api/v1/admin/exercises/${exerciseId}/activate`,
  );
  return res.data.data;
}

export async function disableAdminExercise(exerciseId: string): Promise<AdminExercise> {
  const res = await apiClient.patch<ApiResponse<AdminExercise>>(
    `/api/v1/admin/exercises/${exerciseId}/disable`,
  );
  return res.data.data;
}

