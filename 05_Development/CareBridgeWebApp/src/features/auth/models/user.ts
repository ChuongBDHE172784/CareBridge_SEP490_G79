export interface UserProfile {
  id: number;
  phone: string;
  email: string | null;
  name: string | null;
  avatarUrl: string | null;
  role: UserRole;
}

export type UserRole =
  | 'MOTHER'
  | 'FAMILY'
  | 'EXPERT'
  | 'MODERATOR'
  | 'CONTENT_ADMIN'
  | 'SYSTEM_ADMIN'
  | 'PARTNER';

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  timestamp: string;
}

export const ROLE_LABELS: Record<UserRole, string> = {
  MOTHER: 'Mother',
  FAMILY: 'Family Member',
  EXPERT: 'Verified Expert',
  MODERATOR: 'Moderator',
  CONTENT_ADMIN: 'Content Admin',
  SYSTEM_ADMIN: 'System Admin',
  PARTNER: 'Partner',
};
