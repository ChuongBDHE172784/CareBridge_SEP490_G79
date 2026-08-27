import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../../auth/models/user';
import type {
  AdminUserSummary,
  AdminUserSearchParams,
  UpdateUserStatusRequest,
  CreateStaffAccountRequest,
  StaffAccountResult,
  UpdateUserRoleRequest,
  UserRoleResult,
  AdminUserSession,
  AdminUserActivity,
  PaginatedResult,
  AccountLockAppeal,
  AccountLockAppealStatus,
} from '../models/adminUser';

type PageEnvelope<T> = ApiResponse<T[]> & {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

// UC114 — GET /api/v1/admin/users
export async function searchUsers(params: AdminUserSearchParams): Promise<PaginatedResult<AdminUserSummary>> {
  const q = new URLSearchParams();
  if (params.email) q.set('email', params.email);
  if (params.phone) q.set('phone', params.phone);
  if (params.name) q.set('name', params.name);
  if (params.role) q.set('role', params.role);
  if (params.enabled !== undefined) q.set('enabled', String(params.enabled));
  if (params.locked !== undefined) q.set('locked', String(params.locked));
  q.set('page', String(params.page ?? 0));
  q.set('size', String(params.size ?? 10));

  const res = await apiClient.get<PageEnvelope<AdminUserSummary>>(`/api/v1/admin/users?${q}`);
  const body = res.data;
  return {
    content: body.data ?? [],
    totalElements: body.totalElements ?? 0,
    totalPages: body.totalPages ?? 0,
    page: body.page ?? (params.page ?? 0),
    size: body.size ?? (params.size ?? 10),
  };
}

// UC114 — GET /api/v1/admin/users/{userId}
export async function getUser(userId: string): Promise<AdminUserSummary> {
  const res = await apiClient.get<ApiResponse<AdminUserSummary>>(
    `/api/v1/admin/users/${userId}`,
  );
  return res.data.data;
}

// UC114 — GET /api/v1/admin/users/{userId}/sessions
export async function getUserSessions(
  userId: string,
  page = 0,
  size = 20,
): Promise<PaginatedResult<AdminUserSession>> {
  const res = await apiClient.get<PageEnvelope<AdminUserSession>>(
    `/api/v1/admin/users/${userId}/sessions?page=${page}&size=${size}`,
  );
  const body = res.data;
  return {
    content: body.data ?? [],
    totalElements: body.totalElements ?? 0,
    totalPages: body.totalPages ?? 0,
    page: body.page ?? page,
    size: body.size ?? size,
  };
}

// UC114 — GET /api/v1/admin/users/{userId}/activity
export async function getUserActivity(
  userId: string,
  page = 0,
  size = 20,
): Promise<PaginatedResult<AdminUserActivity>> {
  const res = await apiClient.get<PageEnvelope<AdminUserActivity>>(
    `/api/v1/admin/users/${userId}/activity?page=${page}&size=${size}`,
  );
  const body = res.data;
  return {
    content: body.data ?? [],
    totalElements: body.totalElements ?? 0,
    totalPages: body.totalPages ?? 0,
    page: body.page ?? page,
    size: body.size ?? size,
  };
}

// UC114 — PATCH /api/v1/admin/users/{userId}/status
export async function updateUserStatus(
  userId: string,
  request: UpdateUserStatusRequest,
): Promise<AdminUserSummary> {
  const res = await apiClient.patch<ApiResponse<AdminUserSummary>>(
    `/api/v1/admin/users/${userId}/status`,
    request,
  );
  return res.data.data;
}

// UC116 — PATCH /api/v1/admin/users/{userId}/role
export async function updateUserRole(
  userId: string,
  request: UpdateUserRoleRequest,
): Promise<UserRoleResult> {
  const res = await apiClient.patch<ApiResponse<UserRoleResult>>(
    `/api/v1/admin/users/${userId}/role`,
    request,
  );
  return res.data.data;
}

export async function getAccountLockAppeals(
  status: AccountLockAppealStatus = 'PENDING', page = 0, size = 20,
): Promise<PaginatedResult<AccountLockAppeal>> {
  const res = await apiClient.get<PageEnvelope<AccountLockAppeal>>(
    `/api/v1/admin/account-lock-appeals?status=${status}&page=${page}&size=${size}`,
  );
  return {
    content: res.data.data ?? [],
    totalElements: res.data.totalElements ?? 0,
    totalPages: res.data.totalPages ?? 0,
    page: res.data.page ?? page,
    size: res.data.size ?? size,
  };
}

export async function getAccountLockAppeal(id: string): Promise<AccountLockAppeal> {
  const res = await apiClient.get<ApiResponse<AccountLockAppeal>>(
    `/api/v1/admin/account-lock-appeals/${id}`,
  );
  return res.data.data;
}

export async function reviewAccountLockAppeal(
  id: string,
  decision: 'APPROVE' | 'REJECT',
  reviewNote?: string,
): Promise<AccountLockAppeal> {
  const res = await apiClient.patch<ApiResponse<AccountLockAppeal>>(
    `/api/v1/admin/account-lock-appeals/${id}/review`,
    { decision, reviewNote },
  );
  return res.data.data;
}

// UC115 — POST /api/v1/admin/staff-accounts
export async function createStaffAccount(
  request: CreateStaffAccountRequest,
): Promise<StaffAccountResult> {
  const res = await apiClient.post<ApiResponse<StaffAccountResult>>(
    '/api/v1/admin/staff-accounts',
    request,
  );
  return res.data.data;
}
