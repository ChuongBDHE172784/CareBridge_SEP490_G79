import axios from 'axios';
import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../../auth/models/user';
import type {
  AdminExercise,
  AdminExerciseFieldErrors,
  AdminExerciseForm,
  AdminExerciseFormField,
  AdminExerciseRequestError,
  DifficultyLevel,
  ExerciseStatus,
  PaginatedResponse,
  TrimesterScope,
} from '../models/adminExercise';

interface BackendErrorDetail {
  field?: unknown;
  message?: unknown;
}

interface BackendErrorResponse {
  error?: unknown;
  message?: unknown;
  details?: unknown;
}

function isBackendErrorDetail(value: unknown): value is BackendErrorDetail {
  return typeof value === 'object' && value !== null;
}

const formFields = new Set<AdminExerciseFormField>([
  'title',
  'description',
  'trimesterScope',
  'difficultyLevel',
  'durationMinutes',
  'instructionContent',
  'mediaUrl',
  'safetyWarning',
  'supportsPostureAnalysis',
]);

function isFormField(value: unknown): value is AdminExerciseFormField {
  return typeof value === 'string' && formFields.has(value as AdminExerciseFormField);
}

export function toAdminExerciseRequestError(error: unknown): AdminExerciseRequestError {
  const fallbackMessage = 'Không thể lưu bài tập. Vui lòng kiểm tra thông tin và thử lại.';
  if (!axios.isAxiosError<BackendErrorResponse>(error)) {
    return { message: fallbackMessage, fieldErrors: {} };
  }

  const payload = error.response?.data;
  const code = typeof payload?.error === 'string' && payload.error.trim()
    ? payload.error.trim()
    : undefined;
  const backendMessage = typeof payload?.message === 'string' && payload.message.trim()
    ? payload.message.trim()
    : fallbackMessage;
  const fieldErrors: AdminExerciseFieldErrors = {};

  if (Array.isArray(payload?.details)) {
    for (const detail of payload.details) {
      if (!isBackendErrorDetail(detail)) continue;
      if (isFormField(detail.field) && typeof detail.message === 'string' && detail.message.trim()) {
        fieldErrors[detail.field] = detail.message.trim();
      }
    }
  }

  return {
    code,
    message: code ? `[${code}] ${backendMessage}` : backendMessage,
    fieldErrors,
  };
}

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

