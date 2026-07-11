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

export interface PageResult<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number; }
export interface PartnerServiceListItem { id: string; serviceName: string; description: string; priceFrom: number | null; currency: string; bookingUrl: string | null; approvalStatus: string; createdAt: string; }
export interface SponsoredCampaignListItem { id: string; title: string; description: string; startDate: string | null; endDate: string | null; sponsorLabel: string | null; approvalStatus: string; createdAt: string; }
export interface PartnerVerificationQueueItem { id: string; name: string; type: OrganizationType; status: OrganizationStatus; city: string; createdAt: string; }

export const ORG_TYPE_LABELS: Record<OrganizationType, string> = {
  CLINIC: 'Phòng khám',
  HOSPITAL: 'Bệnh viện',
  NGO: 'Tổ chức phi lợi nhuận',
  COMPANY: 'Công ty',
};
