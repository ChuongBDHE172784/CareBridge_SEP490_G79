import apiClient from '../../../shared/api/apiClient';

export interface ExpertProfileResponse {
	expertProfileId: string;
	userId: string;
	specialtyId: string;
	professionalTitle: string;
	experienceYears: number | null;
	hospitalId: string;
	specialty?: string | null;
	workplace?: string | null;
	consultationScope: string;
	verificationStatus: string;
	verifiedAt: string | null;
	ratingAvg: number | null;
	consultationFeeVnd: number | null;
	avatarUrl?: string | null;
	createdAt: string;
}

export interface ExpertDirectoryItem {
	expertProfileId: string;
	userId: string;
	specialtyId: string;
	professionalTitle: string;
	experienceYears: number | null;
	hospitalId: string;
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
	selfieCropFileId?: string;
	idCardCropFileId?: string;
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
	rejectionReason?: string | null;
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
	expiresAt: string;
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
	expiresAt: string;
}

// ── Master Data ──────────────────────────────────────────────────────

export interface ProvinceResponse {
	provinceId: string;
	name: string;
	nameEn: string;
	region: string;
}

export interface DistrictResponse {
	districtId: string;
	provinceId: string;
	name: string;
	nameEn: string;
}

export interface SpecialtyResponse {
	specialtyId: string;
	name: string;
	description: string;
	category: string;
}

export interface HospitalResponse {
	hospitalId: string;
	name: string;
	provinceId: string;
	districtId: string;
	address: string;
	level: string;
	type: string;
	phone: string;
}

export interface WardResponse {
	wardId: string;
	districtId: string;
	provinceId: string;
	name: string;
	nameEn: string | null;
}

export async function getProvinces(): Promise<ProvinceResponse[]> {
	const { data } = await apiClient.get('/api/v1/master-data/provinces');
	return data.data;
}

export async function getDistricts(provinceId: string): Promise<DistrictResponse[]> {
	const { data } = await apiClient.get('/api/v1/master-data/districts', { params: { provinceId } });
	return data.data;
}

export async function getSpecialties(): Promise<SpecialtyResponse[]> {
	const { data } = await apiClient.get('/api/v1/master-data/specialties');
	return data.data;
}

export async function getHospitals(params: { provinceId?: string; districtId?: string; q?: string }): Promise<HospitalResponse[]> {
	const { data } = await apiClient.get('/api/v1/master-data/hospitals', { params });
	return data.data;
}

export async function getWards(params: { districtId?: string; provinceId?: string }): Promise<WardResponse[]> {
	const { data } = await apiClient.get('/api/v1/master-data/wards', { params });
	return data.data;
}

export async function searchTrackAsiaHospitals(q: string): Promise<any[]> {
	const { data } = await apiClient.get('/api/v1/master-data/hospitals/search/trackasia', { params: { q } });
	return data.data;
}

// ── PKG-01 Expert Profile ──────────────────────────────────────────────────

export async function getMyProfile(): Promise<ExpertProfileResponse> {
	const { data } = await apiClient.get('/api/v1/expert/profiles/me');
	return data.data;
}

export async function createMyProfile(body: {
	specialtyId: string;
	professionalTitle: string;
	experienceYears?: number;
	hospitalId: string;
	consultationScope: string;
	consultationFeeVnd?: number;
}): Promise<ExpertProfileResponse> {
	const { data } = await apiClient.post('/api/v1/expert/profiles', body);
	return data.data;
}

export async function updateMyProfile(body: {
	specialtyId?: string;
	professionalTitle?: string;
	experienceYears?: number;
	hospitalId?: string;
	consultationScope?: string;
	consultationFeeVnd?: number;
}): Promise<ExpertProfileResponse> {
	const { data } = await apiClient.patch('/api/v1/expert/profiles/me', body);
	return data.data;
}

export async function getExpertOnboarding(): Promise<ExpertOnboardingResponse> {
	const { data } = await apiClient.get('/api/v1/expert/onboarding');
	return data.data;
}

export async function verifyFace(files: {
	selfie: File;
	idCard: File;
}): Promise<{ similar: boolean; similarity: number; status: string }> {
	const form = new FormData();
	form.append('selfie', files.selfie);
	form.append('idCard', files.idCard);
	const { data } = await apiClient.post('/api/v1/expert/verify-face', form, {
		headers: { 'Content-Type': undefined },
	});
	const result = data.data as { status: string; similarity: number | null };
	return {
		similar: result.status === 'MATCHED',
		similarity: result.similarity ?? 0,
		status: result.status,
	};
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

// ── Contribution (Medical Knowledge) ────────────────────────────────────────

export type ContributionStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';

export interface ContributionAttachmentRequest {
	fileId: string;
	kind: 'IMAGE' | 'DOCUMENT';
	purpose: 'MEDICAL_CONTRIBUTION_IMAGE' | 'MEDICAL_CONTRIBUTION_DOCUMENT';
	accessMode: 'PUBLIC' | 'AUTHENTICATED' | 'PRIVATE';
	displayOrder?: number;
}

export interface CreateContributionRequest {
	title: string;
	content: string;
	specialtyId?: string;
	hospitalId?: string;
	attachments?: ContributionAttachmentRequest[];
}

export interface UpdateContributionRequest {
	title: string;
	content: string;
	specialtyId?: string;
	hospitalId?: string;
	attachments?: ContributionAttachmentRequest[];
}

export interface ContributionAttachmentResponse {
	id: string;
	fileId: string;
	contributionId: string;
	kind: 'IMAGE' | 'DOCUMENT';
	purpose: 'MEDICAL_CONTRIBUTION_IMAGE' | 'MEDICAL_CONTRIBUTION_DOCUMENT';
	accessMode: 'PUBLIC' | 'AUTHENTICATED' | 'PRIVATE';
	displayOrder: number;
	originalName: string | null;
	mimeType: string;
	fileSizeBytes: number;
	presignedUrl: string | null;
}

export interface ContributionResponse {
	id: string;
	expertUserId: string;
	title: string;
	content: string;
	specialtyId?: string;
	hospitalId?: string;
	status: 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';
	rejectionReason?: string;
	version: number;
	createdAt: string;
	updatedAt: string;
	attachments?: ContributionAttachmentResponse[];
}

export interface PaginatedContributionResponse {
	content: ContributionResponse[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
}

export interface UploadFileResponse {
	fileId: string;
	originalName: string;
	mimeType: string;
	fileSizeBytes: number;
	presignedUrl: string;
	createdAt: string;
}

export async function uploadContributionFile(
	file: File,
	kind: 'IMAGE' | 'DOCUMENT',
	purpose: 'MEDICAL_CONTRIBUTION_IMAGE' | 'MEDICAL_CONTRIBUTION_DOCUMENT',
	accessMode: 'PUBLIC' | 'AUTHENTICATED' | 'PRIVATE'
): Promise<UploadFileResponse> {
	const form = new FormData();
	form.append('file', file);
	form.append('kind', kind);
	form.append('purpose', purpose);
	form.append('accessMode', accessMode);
	const { data } = await apiClient.post('/api/v1/files/upload/with-purpose', form, {
		headers: { 'Content-Type': undefined },
	});
	return data.data;
}

export async function createContribution(body: CreateContributionRequest): Promise<ContributionResponse> {
	const { data } = await apiClient.post('/api/v1/contributions', body);
	return data.data;
}

export async function getContribution(contributionId: string): Promise<ContributionResponse> {
	const { data } = await apiClient.get(`/api/v1/contributions/${contributionId}`);
	return data.data;
}

export async function listMyContributions(params: { page?: number; size?: number }): Promise<PaginatedContributionResponse> {
	const { data } = await apiClient.get('/api/v1/contributions/me', { params });
	return data.data;
}

export async function updateContribution(contributionId: string, body: UpdateContributionRequest): Promise<ContributionResponse> {
	const { data } = await apiClient.put(`/api/v1/contributions/${contributionId}`, body);
	return data.data;
}

export async function submitContribution(contributionId: string): Promise<ContributionResponse> {
	const { data } = await apiClient.post(`/api/v1/contributions/${contributionId}/submit`);
	return data.data;
}

export async function deleteContribution(contributionId: string): Promise<void> {
	await apiClient.delete(`/api/v1/contributions/${contributionId}`);
}

export async function checkContributionEligibility(): Promise<boolean> {
	const { data } = await apiClient.get('/api/v1/contributions/eligibility');
	return data.data;
}

// Admin functions
export async function listContributionsForReview(params: { status: 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'DRAFT'; page?: number; size?: number }): Promise<PaginatedContributionResponse> {
	const { data } = await apiClient.get('/api/v1/contributions/review-queue', { params });
	return data.data;
}

export async function approveContribution(contributionId: string): Promise<ContributionResponse> {
	const { data } = await apiClient.post(`/api/v1/contributions/${contributionId}/approve`);
	return data.data;
}

export async function rejectContribution(contributionId: string, reason: string): Promise<ContributionResponse> {
	const { data } = await apiClient.post(`/api/v1/contributions/${contributionId}/reject`, null, { params: { reason } });
	return data.data;
}
