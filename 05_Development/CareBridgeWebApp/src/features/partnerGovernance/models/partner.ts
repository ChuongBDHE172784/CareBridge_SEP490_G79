export type OrganizationType = 'CLINIC' | 'HOSPITAL' | 'NGO' | 'COMPANY';
export type OrganizationStatus = 'PENDING_APPROVAL' | 'APPROVED' | 'SUSPENDED' | 'REJECTED';

export interface CreatePartnerProfileRequest {
  name: string;
  type: OrganizationType;
  address: string;
  city: string;
  phone: string;
  email: string;
  website: string;
  description: string;
}

export interface CreatePartnerProfileResponse {
  id: string;
  name: string;
  type: OrganizationType;
  status: OrganizationStatus;
  createdAt: string;
}

export interface UpdatePartnerProfileRequest extends CreatePartnerProfileRequest {
  logoUrl?: string;
}

export interface PageResult<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number; }
export interface PartnerVerificationQueueItem { id: string; name: string; type: OrganizationType; status: OrganizationStatus; city: string; createdAt: string; }

export const ORG_TYPE_LABELS: Record<OrganizationType, string> = {
  CLINIC: 'Phòng khám',
  HOSPITAL: 'Bệnh viện',
  NGO: 'Tổ chức phi lợi nhuận',
  COMPANY: 'Công ty',
};
