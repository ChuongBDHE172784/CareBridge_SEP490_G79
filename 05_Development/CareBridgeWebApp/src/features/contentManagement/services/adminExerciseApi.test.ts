import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '../../../shared/api/apiClient';
import type { AdminExercise, AdminExerciseForm } from '../models/adminExercise';
import {
  createAdminExercise,
  toAdminExerciseRequestError,
  updateAdminExercise,
} from './adminExerciseApi';

vi.mock('../../../shared/api/apiClient', () => ({
  default: {
    post: vi.fn(),
    put: vi.fn(),
  },
}));

const payload: AdminExerciseForm = {
  title: 'Yoga nhẹ nhàng',
  description: '',
  trimesterScope: 'ALL',
  difficultyLevel: 'EASY',
  durationMinutes: 15,
  instructionContent: '',
  mediaUrl: '',
  safetyWarning: 'Dừng lại nếu thấy chóng mặt.',
  supportsPostureAnalysis: false,
};

const exercise: AdminExercise = {
  exerciseId: 'exercise-123',
  ...payload,
  description: null,
  instructionContent: null,
  mediaUrl: null,
  status: 'DRAFT',
  versionNo: 1,
  createdBy: null,
  createdAt: null,
  updatedAt: null,
};

describe('adminExerciseApi draft persistence', () => {
  beforeEach(() => {
    vi.mocked(apiClient.post).mockReset();
    vi.mocked(apiClient.put).mockReset();
  });

  it('unwraps create and update responses at the shared API boundary', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: exercise } } as never);
    vi.mocked(apiClient.put).mockResolvedValue({ data: { data: exercise } } as never);

    await expect(createAdminExercise(payload)).resolves.toEqual(exercise);
    await expect(updateAdminExercise('exercise-123', payload)).resolves.toEqual(exercise);

    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/admin/exercises', payload);
    expect(apiClient.put).toHaveBeenCalledWith('/api/v1/admin/exercises/exercise-123', payload);
  });

  it('keeps the backend code and typed field details in a request error', () => {
    const result = toAdminExerciseRequestError({
      isAxiosError: true,
      response: {
        data: {
          error: 'VALIDATION_ERROR',
          message: 'Invalid request',
          details: [
            { field: 'title', message: 'must not be blank' },
            { field: 'unknownField', message: 'ignored' },
          ],
        },
      },
    });

    expect(result).toEqual({
      code: 'VALIDATION_ERROR',
      message: '[VALIDATION_ERROR] Invalid request',
      fieldErrors: { title: 'must not be blank' },
    });
  });

  it('returns an actionable fallback for non-Axios failures', () => {
    const result = toAdminExerciseRequestError(new Error('network unavailable'));

    expect(result.code).toBeUndefined();
    expect(result.message).toContain('Không thể lưu bài tập');
    expect(result.fieldErrors).toEqual({});
  });

  it('ignores malformed validation details without hiding the backend error', () => {
    const result = toAdminExerciseRequestError({
      isAxiosError: true,
      response: {
        data: {
          error: 'VALIDATION_ERROR',
          message: 'Invalid request',
          details: [null, 42, 'invalid', { field: 'safetyWarning', message: 'must not be blank' }],
        },
      },
    });

    expect(result.message).toBe('[VALIDATION_ERROR] Invalid request');
    expect(result.fieldErrors).toEqual({ safetyWarning: 'must not be blank' });
  });
});
