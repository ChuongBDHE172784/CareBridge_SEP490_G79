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
	avatarUrl?: string | null;
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

export type ExpertOnboardingStep = 'PROFILE' | 'IDENTITY' | 'CREDENTIAL' | 'UNDER_REVIEW' | 'COMPLETE';

export interface IdentityAttemptResponse {
	identityVerificationId?: string;
	attemptId?: string;
	expertProfileId?: string;
	selfieFileId?: string;
	identityFrontFileId?: string;
	identityBackFileId?: string;
	status?: string;
	reviewStatus?: string;
	faceStatus?: string;
	providerStatus?: string | null;
	faceSimilarity?: number | null;
	faceThreshold?: number | null;
	similarity?: number | null;
	providerErrorCode?: string | null;
	reviewReason?: string | null;
	createdAt?: string;
}

export interface ExpertOnboardingResponse {
	profileExists: boolean;
	identityStatus: string | null;
	credentialStatus: string | null;
	verificationStatus: string | null;
	nextStep: ExpertOnboardingStep;
	latestIdentityAttempt: IdentityAttemptResponse | null;
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
	createdAt: string;
	reviewStatus: string;
	reviewNote: string | null;
	reviewedBy: string;
	reviewedAt: string | null;
	expertName?: string;
	specialty?: string;
	professionalTitle?: string;
	experienceYears?: number | null;
	workplace?: string;
	phone?: string;
	email?: string;
	ratingAvg?: number | null;
	avatarUrl?: string | null;
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
	file?: File;
}

export interface ReviewCredentialRequest {
	decision?: string;
	reviewStatus: string;
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

export async function createMyProfile(body: {
	specialty: string;
	professionalTitle: string;
	experienceYears?: number;
	workplace: string;
	consultationScope: string;
}): Promise<ExpertProfileResponse> {
	const { data } = await apiClient.post('/api/v1/expert/profiles', body);
	return data.data;
}

export async function updateMyProfile(body: {
	specialty?: string;
	professionalTitle?: string;
	experienceYears?: number;
	workplace?: string;
	consultationScope?: string;
}): Promise<ExpertProfileResponse> {
	const { data } = await apiClient.patch('/api/v1/expert/profiles/me', body);
	return data.data;
}

export async function getExpertOnboarding(): Promise<ExpertOnboardingResponse> {
	const { data } = await apiClient.get('/api/v1/expert/onboarding');
	return data.data;
}

export async function submitIdentityEvidence(files: {
	selfie: File;
	identityFront: File;
	identityBack: File;
}): Promise<IdentityAttemptResponse> {
	const form = new FormData();
	form.append('selfie', files.selfie);
	form.append('identityFront', files.identityFront);
	form.append('identityBack', files.identityBack);
	const { data } = await apiClient.post('/api/v1/expert/identity', form, {
		headers: { 'Content-Type': undefined },
	});
	return data.data;
}

export async function getIdentityFileUrl(fileId: string): Promise<string> {
	const { data } = await apiClient.get(`/api/v1/expert/identity/files/${fileId}/url`);
	return typeof data.data === 'string' ? data.data : data.data.presignedUrl;
}

export async function getPendingIdentityReviews(): Promise<IdentityAttemptResponse[]> {
	const { data } = await apiClient.get('/api/v1/expert/identity/pending');
	return data.data;
}

export async function reviewIdentity(attemptId: string, reviewStatus: 'APPROVED' | 'REJECTED', reason?: string): Promise<IdentityAttemptResponse> {
	const { data } = await apiClient.put(`/api/v1/expert/identity/${attemptId}/review`, { reviewStatus, reason: reason || undefined });
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

export async function submitCredential(params: {
	body: SubmitCredentialRequest;
	file: File;
}): Promise<CredentialResponse> {
	const form = new FormData();
	form.append('credentialType', params.body.credentialType);
	if (params.body.credentialNumber) form.append('credentialNumber', params.body.credentialNumber);
	if (params.body.issuer) form.append('issuer', params.body.issuer);
	form.append('issuedDate', params.body.issuedDate);
	if (params.body.expiryDate) form.append('expiryDate', params.body.expiryDate);
	if (params.file) form.append('file', params.file);
	const { data } = await apiClient.post('/api/v1/expert/credentials', form, {
		headers: { 'Content-Type': undefined },
	});
	return data.data;
}

export async function getMyCredentials(): Promise<CredentialResponse[]> {
	const { data } = await apiClient.get('/api/v1/expert/credentials/me');
	return data.data;
}

export async function deleteCredential(credentialId: string): Promise<void> {
	await apiClient.delete(`/api/v1/expert/credentials/${credentialId}`);
}

// Admin: pending reviews
export async function getPendingReviews(credentialType?: string): Promise<DocumentReviewResponse[]> {
	const { data } = await apiClient.get('/api/v1/expert/credentials/pending', {
		params: credentialType ? { credentialType } : {},
	});
	return data.data;
}

export async function reviewCredential(
	credentialId: string,
	body: ReviewCredentialRequest
): Promise<DocumentReviewResponse> {
	const { data } = await apiClient.put(`/api/v1/expert/credentials/${credentialId}/review`, body);
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

// ── Location Sharing ──────────────────────────────────────────────────────

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
