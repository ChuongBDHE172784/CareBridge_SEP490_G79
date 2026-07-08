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
  PaginatedResult,
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
