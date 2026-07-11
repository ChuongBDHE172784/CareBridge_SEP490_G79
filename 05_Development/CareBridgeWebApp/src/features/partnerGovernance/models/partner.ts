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

export interface PartnerServiceDraft {
  name: string;
  category: string;
  location: string;
  description: string;
  eligibility?: string;
  routing: 'CUSTOMER_SELECTS' | 'AUTO_ASSIGN';
}

export interface SponsoredCampaignDraft {
  name: string;
  objective: string;
  audience: string;
  region: string;
  requiresSponsorLabel: boolean;
  requiresContentApproval: boolean;
}

export interface PartnerPerformance {
  serviceListings: Record<string, number>;
  sponsoredCampaigns: Record<string, number>;
  activeExpertLinks: number;
}

export const ORG_TYPE_LABELS: Record<OrganizationType, string> = {
  CLINIC: 'Phòng khám',
  HOSPITAL: 'Bệnh viện',
  NGO: 'Tổ chức phi lợi nhuận',
  COMPANY: 'Công ty',
};
