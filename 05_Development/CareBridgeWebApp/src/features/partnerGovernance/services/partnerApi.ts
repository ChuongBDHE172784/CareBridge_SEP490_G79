import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../../auth/models/user';
import type { CreatePartnerProfileRequest, CreatePartnerProfileResponse, PageResult, PartnerVerificationQueueItem, UpdatePartnerProfileRequest } from '../models/partner';

export async function registerPartnerAccount(data: {
  phone: string;
  password: string;
  name: string;
  email?: string;
}) {
  const res = await apiClient.post('/api/v1/auth/register', {
    phone: data.phone,
    password: data.password,
    name: data.name,
    email: data.email,
    role: 'PARTNER',
  });
  return res.data;
}

export async function createPartnerProfile(data: CreatePartnerProfileRequest): Promise<CreatePartnerProfileResponse> {
  const res = await apiClient.post<ApiResponse<CreatePartnerProfileResponse>>('/api/v1/partner/profile', data);
  return res.data.data;
}

export async function updatePartnerProfile(data: UpdatePartnerProfileRequest): Promise<CreatePartnerProfileResponse> {
  const res = await apiClient.put<ApiResponse<CreatePartnerProfileResponse>>('/api/v1/partner/profile', data);
  return res.data.data;
}

export async function fetchPartnerProfile(): Promise<UpdatePartnerProfileRequest & { status: string }> { const res = await apiClient.get<ApiResponse<UpdatePartnerProfileRequest & { status: string }>>('/api/v1/partner/profile'); return res.data.data; }
export async function fetchPartnerVerificationQueue(search = ''): Promise<PageResult<PartnerVerificationQueueItem>> { const query = new URLSearchParams({ page: '0', size: '20' }); if (search) query.set('search', search); const res = await apiClient.get<PageResult<PartnerVerificationQueueItem>>(`/api/v1/admin/partners?${query}`); return res.data; }

export async function decidePartnerProfile(partnerId: string, decision: 'APPROVE' | 'REJECT' | 'SUSPEND' | 'REINSTATE', reason?: string) {
  const res = await apiClient.post<ApiResponse<{ previousStatus: string; newStatus: string }>>(`/api/v1/admin/partners/${partnerId}/decision`, { decision, reason });
  return res.data.data;
}
