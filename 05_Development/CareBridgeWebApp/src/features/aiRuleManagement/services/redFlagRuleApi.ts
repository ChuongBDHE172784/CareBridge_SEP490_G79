import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../../auth/models/user';
import type {
  RedFlagRule,
  RedFlagRulePage,
  RedFlagSeverity,
  CreateRedFlagRuleRequest,
  UpdateRedFlagRuleRequest,
} from '../models/redFlagRule';

// RedFlagRuleController wraps every response in ApiResponse — read res.data.data.
export async function fetchRedFlagRules(params: {
  severity?: RedFlagSeverity;
  isActive?: boolean;
  page?: number;
  size?: number;
}): Promise<RedFlagRulePage> {
  const res = await apiClient.get<ApiResponse<RedFlagRulePage>>('/api/v1/admin/red-flag-rules', {
    params: {
      severity: params.severity,
      isActive: params.isActive,
      page: params.page ?? 0,
      size: params.size ?? 20,
    },
  });
  return res.data.data;
}

export async function createRedFlagRule(request: CreateRedFlagRuleRequest): Promise<RedFlagRule> {
  const res = await apiClient.post<ApiResponse<RedFlagRule>>('/api/v1/admin/red-flag-rules', request);
  return res.data.data;
}

export async function updateRedFlagRule(id: string, request: UpdateRedFlagRuleRequest): Promise<RedFlagRule> {
  const res = await apiClient.patch<ApiResponse<RedFlagRule>>(`/api/v1/admin/red-flag-rules/${id}`, request);
  return res.data.data;
}

export async function deleteRedFlagRule(id: string): Promise<void> {
  await apiClient.delete(`/api/v1/admin/red-flag-rules/${id}`);
}
