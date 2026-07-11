import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../../auth/models/user';
import type { CreatePartnerProfileRequest, CreatePartnerProfileResponse, PageResult, PartnerPerformance, PartnerServiceDraft, PartnerServiceListItem, PartnerVerificationQueueItem, SponsoredCampaignDraft, SponsoredCampaignListItem, UpdatePartnerProfileRequest } from '../models/partner';

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
export async function fetchPartnerServices(page = 0): Promise<PageResult<PartnerServiceListItem>> { const res = await apiClient.get<PageResult<PartnerServiceListItem>>(`/api/v1/partner/services?page=${page}&size=20`); return res.data; }
export async function fetchSponsoredCampaigns(page = 0): Promise<PageResult<SponsoredCampaignListItem>> { const res = await apiClient.get<PageResult<SponsoredCampaignListItem>>(`/api/v1/partner/campaigns?page=${page}&size=20`); return res.data; }
export async function fetchPartnerVerificationQueue(search = ''): Promise<PageResult<PartnerVerificationQueueItem>> { const query = new URLSearchParams({ page: '0', size: '20' }); if (search) query.set('search', search); const res = await apiClient.get<PageResult<PartnerVerificationQueueItem>>(`/api/v1/admin/partners?${query}`); return res.data; }

export async function submitServiceListing(data: PartnerServiceDraft): Promise<{ id: string; approvalStatus: string }> {
  const res = await apiClient.post<ApiResponse<{ id: string; approvalStatus: string }>>('/api/v1/partner/services', data);
  return res.data.data;
}

export async function submitSponsoredCampaign(data: SponsoredCampaignDraft): Promise<{ id: string; approvalStatus: string }> {
  const res = await apiClient.post<ApiResponse<{ id: string; approvalStatus: string }>>('/api/v1/partner/campaigns', data);
  return res.data.data;
}

export async function fetchPartnerPerformance(from?: string, to?: string): Promise<PartnerPerformance> {
  const params = new URLSearchParams();
  if (from) params.set('from', from);
  if (to) params.set('to', to);
  const suffix = params.size ? `?${params}` : '';
  const res = await apiClient.get<ApiResponse<PartnerPerformance>>(`/api/v1/partner/performance${suffix}`);
  return res.data.data;
}

export async function decidePartnerProfile(partnerId: string, decision: 'APPROVE' | 'REJECT' | 'SUSPEND' | 'REINSTATE', reason?: string) {
  const res = await apiClient.post<ApiResponse<{ previousStatus: string; newStatus: string }>>(`/api/v1/admin/partners/${partnerId}/decision`, { decision, reason });
  return res.data.data;
}

export async function decidePartnerContent(targetType: 'SERVICE' | 'CAMPAIGN', targetId: string, decision: 'APPROVE' | 'REJECT', reason?: string) {
  const res = await apiClient.post<ApiResponse<{ previousStatus: string; newStatus: string }>>(`/api/v1/admin/partner-content/${targetType}/${targetId}/decision`, { decision, reason });
  return res.data.data;
}

export async function removePartnerContent(targetType: 'SERVICE' | 'CAMPAIGN', targetId: string, reason: string) {
  const res = await apiClient.post<ApiResponse<{ isRemoved: boolean }>>(`/api/v1/admin/partner-content/${targetType}/${targetId}/remove`, { reason });
  return res.data.data;
}
