import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../models/user';

export interface SubmittedAppeal {
  id: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
  submittedAt: string;
}

export async function submitAccountLockAppeal(appealToken: string, reason: string) {
  const response = await apiClient.post<ApiResponse<SubmittedAppeal>>('/api/v1/auth/lock-appeals', {
    appealToken,
    reason,
  });
  return response.data.data;
}

