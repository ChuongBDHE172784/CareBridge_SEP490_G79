import type { UserRole } from '../../../shared/auth/authStore';

// UC114 Manage User Accounts — mirrors backend AdminUserSummaryResponse
export interface AdminUserSummary {
  id: string;
  email: string;
  phone: string | null;
  name: string;
  role: UserRole;
  enabled: boolean;
  locked: boolean;
  lockedAt: string | null;
  createdAt: string;
}

export interface AdminUserSearchParams {
  email?: string;
  phone?: string;
  name?: string;
  role?: UserRole;
  enabled?: boolean;
  locked?: boolean;
  page?: number;
  size?: number;
}

// UC114 — status mutation (enable/disable, lock/unlock)
export interface UpdateUserStatusRequest {
  enabled?: boolean;
  locked?: boolean;
  reason?: string;
}

// UC115 Create Staff Account
export type StaffRole = 'MODERATOR' | 'CONTENT_ADMIN' | 'SYSTEM_ADMIN';

export interface CreateStaffAccountRequest {
  email: string;
  phone?: string;
  name: string;
  role: StaffRole;
}

export interface StaffAccountResult {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  mustChangePassword: boolean;
  createdAt: string;
}

// UC116 Update Role and Permission
export interface UpdateUserRoleRequest {
  newRole: UserRole;
  lockAccessRights?: boolean;
  reason?: string;
}

export interface UserRoleResult {
  id: string;
  previousRole: UserRole;
  newRole: UserRole;
  locked: boolean;
  updatedAt: string;
}

export interface PaginatedResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
