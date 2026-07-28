// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminUserSummary } from '../models/adminUser';

const harness = vi.hoisted(() => ({
  searchUsers: vi.fn(),
  updateUserStatus: vi.fn(),
  navigate: vi.fn(),
  currentUserId: 'admin-current',
}));

vi.mock('react-router-dom', () => ({
  useNavigate: () => harness.navigate,
}));

vi.mock('../../../shared/auth/useAuth', () => ({
  useAuth: () => ({ user: { id: harness.currentUserId, role: 'SYSTEM_ADMIN' } }),
}));

vi.mock('../services/adminUserApi', () => ({
  searchUsers: harness.searchUsers,
  updateUserStatus: harness.updateUserStatus,
}));

import UserListPage from './UserListPage';

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
    lockType: null,
    lockReason: null,
    lockedBy: null,
    lockEpisodeId: null,
    createdAt: '2026-07-20T02:00:00Z',
    ...overrides,
  };
}

describe('admin user list actions', () => {
  beforeEach(() => {
    harness.currentUserId = 'admin-current';
    harness.searchUsers.mockReset().mockResolvedValue({
      content: [targetUser()],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 10,
    });
    harness.updateUserStatus.mockReset();
    harness.navigate.mockReset();
  });

  afterEach(cleanup);

  it('shows direct lock and disable actions instead of a three-dot-only control', async () => {
    render(<UserListPage />);

    expect(await screen.findByRole('button', { name: /Khóa tài khoản/ })).toBeTruthy();
    expect(screen.getByRole('button', { name: /Vô hiệu hóa/ })).toBeTruthy();
    expect(screen.queryByText('more_vert')).toBeNull();
  });

  it('updates the row immediately after locking an account', async () => {
    harness.updateUserStatus.mockResolvedValue(targetUser({ locked: true, lockedAt: '2026-07-28T14:00:00Z' }));
    render(<UserListPage />);

    fireEvent.click(await screen.findByRole('button', { name: /Khóa tài khoản/ }));
    fireEvent.change(screen.getByPlaceholderText(/Nhập lý do cụ thể/), {
      target: { value: 'Locked from admin user list' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Khóa tài khoản' }));

    await waitFor(() => expect(harness.updateUserStatus).toHaveBeenCalledWith('user-target', {
      locked: true,
      reason: 'Locked from admin user list',
    }));
    expect(await screen.findByRole('button', { name: /Mở khóa/ })).toBeTruthy();
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('requires a second confirmation before unlocking an account', async () => {
    harness.searchUsers.mockResolvedValue({
      content: [targetUser({ locked: true, lockedAt: '2026-07-28T14:00:00Z' })],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 10,
    });
    harness.updateUserStatus.mockResolvedValue(targetUser({ locked: false }));
    render(<UserListPage />);

    fireEvent.click(await screen.findByRole('button', { name: /Mở khóa/ }));

    expect(screen.getByRole('dialog')).toBeTruthy();
    expect(harness.updateUserStatus).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận mở khóa' }));

    await waitFor(() => expect(harness.updateUserStatus).toHaveBeenCalledWith('user-target', { locked: false }));
  });

  it('uses distinct mother and family labels without showing abbreviated user IDs or a detail icon', async () => {
    harness.searchUsers.mockResolvedValue({
      content: [targetUser({ id: 'mother-target', role: 'MOTHER' }), targetUser({ id: 'family-target', role: 'FAMILY' })],
      totalElements: 2,
      totalPages: 1,
      page: 0,
      size: 10,
    });
    render(<UserListPage />);

    expect(await screen.findByRole('option', { name: 'Mẹ' })).toBeTruthy();
    expect(screen.getByRole('option', { name: 'Gia đình' })).toBeTruthy();
    expect(screen.queryByText(/ID: #/)).toBeNull();
    expect(screen.queryByText('open_in_new')).toBeNull();
  });

  it('disables destructive actions for the current administrator', async () => {
    harness.currentUserId = 'user-target';
    render(<UserListPage />);

    expect((await screen.findByText('Khóa tài khoản')).closest('button')).toHaveProperty('disabled', true);
    expect(screen.getAllByText('Vô hiệu hóa').find((element) => element.closest('button'))?.closest('button')).toHaveProperty('disabled', true);
  });
});
