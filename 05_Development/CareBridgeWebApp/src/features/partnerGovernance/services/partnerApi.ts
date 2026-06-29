import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../../auth/models/user';
import type { CreatePartnerProfileRequest, CreatePartnerProfileResponse } from '../models/partner';

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
