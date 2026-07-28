// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminUserSummary } from '../models/adminUser';

const harness = vi.hoisted(() => ({
  getUser: vi.fn(),
  getUserSessions: vi.fn(),
  getUserActivity: vi.fn(),
  updateUserStatus: vi.fn(),
  navigate: vi.fn(),
  currentUserId: 'admin-current',
}));

vi.mock('react-router-dom', () => ({
  useParams: () => ({ userId: 'user-target' }),
  useNavigate: () => harness.navigate,
}));

vi.mock('../../../shared/auth/useAuth', () => ({
  useAuth: () => ({ user: { id: harness.currentUserId, role: 'SYSTEM_ADMIN' } }),
}));

vi.mock('../services/adminUserApi', () => ({
  getUser: harness.getUser,
  getUserSessions: harness.getUserSessions,
  getUserActivity: harness.getUserActivity,
  updateUserStatus: harness.updateUserStatus,
}));

import UserDetailPage from './UserDetailPage';

function targetUser(overrides: Partial<AdminUserSummary> = {}): AdminUserSummary {
  return {
    id: 'user-target',
    email: 'target@carebridge.dev',
    phone: null,
    name: 'Target User',
    role: 'MODERATOR',
    enabled: true,
    locked: false,
    lockedAt: null,
    createdAt: '2026-07-20T02:00:00Z',
    ...overrides,
  };
}

describe('admin user detail page', () => {
  beforeEach(() => {
    harness.currentUserId = 'admin-current';
    harness.getUser.mockReset().mockResolvedValue(targetUser());
    harness.getUserSessions.mockReset().mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, page: 0, size: 50 });
    harness.getUserActivity.mockReset().mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, page: 0, size: 50 });
    harness.updateUserStatus.mockReset();
    harness.navigate.mockReset();
  });

  afterEach(cleanup);

  it('loads user details from the URL parameter without router state', async () => {
    render(<UserDetailPage />);

    expect(await screen.findByText('Target User')).toBeTruthy();
    expect(harness.getUser).toHaveBeenCalledWith('user-target');
    expect(screen.getByText('target@carebridge.dev')).toBeTruthy();
  });

  it('loads privacy-minimized sessions when the session tab opens', async () => {
    harness.getUserSessions.mockResolvedValue({
      content: [{
        id: 'session-1',
        deviceName: 'Chrome on macOS',
        status: 'ACTIVE',
        issuedAt: '2026-07-28T01:00:00Z',
        lastActivityAt: '2026-07-28T02:00:00Z',
        expiresAt: '2026-08-01T01:00:00Z',
        revokedAt: null,
      }],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 50,
    });
    render(<UserDetailPage />);
    await screen.findByText('Target User');

    fireEvent.click(screen.getByRole('button', { name: 'Phiên đăng nhập' }));

    expect(await screen.findByText('Chrome on macOS')).toBeTruthy();
    expect(harness.getUserSessions).toHaveBeenCalledWith('user-target', 0, 50);
    expect(screen.queryByText(/refresh token/i)).toBeNull();
  });

  it('hides role management for accounts outside the staff governance scope', async () => {
    harness.getUser.mockResolvedValue(targetUser({ role: 'MOTHER' }));
    render(<UserDetailPage />);

    expect(await screen.findByText('Target User')).toBeTruthy();
    expect(screen.queryByText('Cập nhật vai trò')).toBeNull();
    expect(screen.getByText('Khóa tài khoản')).toBeTruthy();
    expect(screen.getByText('Vô hiệu hóa')).toBeTruthy();
  });

  it('disables destructive controls for the current admin account', async () => {
    harness.currentUserId = 'user-target';
    render(<UserDetailPage />);

    expect(await screen.findByText(/Đây là tài khoản đang đăng nhập/)).toBeTruthy();
    await waitFor(() => {
      expect((screen.getByText('Cập nhật vai trò').closest('button') as HTMLButtonElement).disabled).toBe(true);
      expect((screen.getByText('Khóa tài khoản').closest('button') as HTMLButtonElement).disabled).toBe(true);
      expect((screen.getByText('Vô hiệu hóa').closest('button') as HTMLButtonElement).disabled).toBe(true);
    });
  });
});
