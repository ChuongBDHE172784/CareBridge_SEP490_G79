import apiClient from '../../../shared/api/apiClient';

export interface ExpertProfileResponse {
  expertProfileId: string;
  userId: string;
  specialty: string;
  professionalTitle: string;
  experienceYears: number | null;
  workplace: string;
  consultationScope: string;
  verificationStatus: string;
  verifiedAt: string | null;
  ratingAvg: number | null;
  createdAt: string;
}

export interface ExpertDirectoryItem {
  expertProfileId: string;
  userId: string;
  specialty: string;
  professionalTitle: string;
  experienceYears: number | null;
  workplace: string;
  verificationStatus: string;
  ratingAvg: number | null;
}

export interface ExpertDirectoryResponse {
  content: ExpertDirectoryItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CredentialResponse {
  credentialId: string;
  expertProfileId: string;
  credentialType: string;
  credentialNumber: string;
  issuer: string;
  issuedDate: string;
  expiryDate: string | null;
  fileUrl: string | null;
  reviewStatus: string;
  reviewNote: string | null;
  reviewedAt: string | null;
  createdAt: string;
}

export interface DocumentReviewResponse {
  credentialId: string;
  expertProfileId: string;
  credentialType: string;
  credentialNumber: string;
  issuer: string;
  issuedDate: string;
  expiryDate: string | null;
  fileUrl: string | null;
  reviewStatus: string;
  reviewNote: string | null;
  reviewedBy: string;
  reviewedAt: string | null;
}

export interface AvailabilityResponse {
  availabilityId: string;
  startAt: string;
  endAt: string;
  channelType: string;
  status: string;
  createdAt: string;
}

export interface LocationShareResponse {
  locationShareId: string;
  latitude: number;
  longitude: number;
  accuracyMeters: number | null;
  availabilityStatus: string | null;
  sharedAt: string;
  expiresAt: string | null;
}

export interface SubmitCredentialRequest {
  credentialType: string;
  credentialNumber?: string;
  issuer?: string;
  issuedDate: string;
  expiryDate?: string;
  fileUrl?: string;
}

export interface ReviewCredentialRequest {
  decision: 'APPROVED' | 'REJECTED';
  reviewNote?: string;
}

export interface CreateAvailabilityRequest {
  startAt: string;
  endAt: string;
  channelType: string;
  status: string;
}

export interface ShareLocationRequest {
  latitude: number;
  longitude: number;
  accuracyMeters?: number;
  availabilityStatus?: string;
  expiresAt?: string;
}

// ── PKG-01 Expert Profile ──────────────────────────────────────────────────

export async function getMyProfile(): Promise<ExpertProfileResponse> {
  const { data } = await apiClient.get('/api/v1/expert/profiles/me');
  return data.data;
}

export async function updateMyProfile(body: {
  specialty?: string;
  professionalTitle?: string;
  experienceYears?: number;
  workplace?: string;
  consultationScope?: string;
}): Promise<ExpertProfileResponse> {
  const { data } = await apiClient.put('/api/v1/expert/profiles/me', body);
  return data.data;
}

export async function getPublicDirectory(params: {
  specialty?: string;
  page?: number;
  size?: number;
}): Promise<ExpertDirectoryResponse> {
  const { data } = await apiClient.get('/api/v1/expert/directory', { params });
  return data.data;
}

export async function approveExpert(profileId: string): Promise<void> {
  await apiClient.post(`/api/v1/expert/profiles/${profileId}/approve`);
}

export async function rejectExpert(profileId: string, reason?: string): Promise<void> {
  await apiClient.post(`/api/v1/expert/profiles/${profileId}/reject`, { reason });
}

// ── PKG-02 Expert Verification ────────────────────────────────────────────

export async function submitCredential(body: SubmitCredentialRequest): Promise<CredentialResponse> {
  const { data } = await apiClient.post('/api/v1/expert/credentials', body);
  return data.data;
}

export async function getMyCredentials(): Promise<CredentialResponse[]> {
  const { data } = await apiClient.get('/api/v1/expert/credentials');
  return data.data;
}

export async function deleteCredential(credentialId: string): Promise<void> {
  await apiClient.delete(`/api/v1/expert/credentials/${credentialId}`);
}

// Admin: pending reviews
export async function getPendingReviews(credentialType?: string): Promise<DocumentReviewResponse[]> {
  const { data } = await apiClient.get('/api/v1/admin/expert/credentials/reviews/pending', {
    params: credentialType ? { credentialType } : {},
  });
  return data.data;
}

export async function reviewCredential(
  credentialId: string,
  body: ReviewCredentialRequest
): Promise<DocumentReviewResponse> {
  const { data } = await apiClient.put(`/api/v1/admin/expert/credentials/${credentialId}/review`, body);
  return data.data;
}

// ── PKG-03 Expert Availability ────────────────────────────────────────────

export async function createAvailability(body: CreateAvailabilityRequest): Promise<AvailabilityResponse> {
  const { data } = await apiClient.post('/api/v1/expert/availability', body);
  return data.data;
}

export async function getMyAvailability(): Promise<AvailabilityResponse[]> {
  const { data } = await apiClient.get('/api/v1/expert/availability/me');
  return data.data;
}

export async function deleteAvailability(availabilityId: string): Promise<void> {
  await apiClient.delete(`/api/v1/expert/availability/${availabilityId}`);
}

export async function shareLocation(body: ShareLocationRequest): Promise<LocationShareResponse> {
  const { data } = await apiClient.post('/api/v1/expert/location/share', body);
  return data.data;
}

export async function stopSharingLocation(): Promise<void> {
  await apiClient.delete('/api/v1/expert/location/share');
}

// ── Community answer (UC-68) ──────────────────────────────────────────────

export async function postCommunityAnswer(
  questionId: string,
  content: string
): Promise<{ answerId: string }> {
  const { data } = await apiClient.post(
    `/api/v1/community/questions/${questionId}/answers`,
    { content }
  );
  return data.data;
}
