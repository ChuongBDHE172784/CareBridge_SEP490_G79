// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminUserSummary } from '../models/adminUser';

const harness = vi.hoisted(() => ({
  getUser: vi.fn(),
  updateUserRole: vi.fn(),
  navigate: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
  useParams: () => ({ userId: 'user-target' }),
  useNavigate: () => harness.navigate,
}));

vi.mock('../../../shared/auth/useAuth', () => ({
  useAuth: () => ({ user: { id: 'admin-current', role: 'SYSTEM_ADMIN' } }),
}));

vi.mock('../services/adminUserApi', () => ({
  getUser: harness.getUser,
  updateUserRole: harness.updateUserRole,
}));

import UpdateUserRolePage from './UpdateUserRolePage';

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

describe('admin role update page', () => {
  beforeEach(() => {
    harness.getUser.mockReset().mockResolvedValue(targetUser());
    harness.updateUserRole.mockReset();
    harness.navigate.mockReset();
  });

  afterEach(cleanup);

  it('offers only staff governance destination roles', async () => {
    render(<UpdateUserRolePage />);

    expect(await screen.findByRole('radio', { name: /Kiểm duyệt viên/ })).toBeTruthy();
    expect(screen.getByRole('radio', { name: /Quản trị nội dung/ })).toBeTruthy();
    expect(screen.getByRole('radio', { name: /Quản trị hệ thống/ })).toBeTruthy();
    expect(screen.queryByRole('radio', { name: /Mẹ/ })).toBeNull();
    expect(screen.queryByRole('radio', { name: /Gia đình/ })).toBeNull();
    expect(screen.queryByRole('radio', { name: /Chuyên gia/ })).toBeNull();
    expect(screen.queryByRole('radio', { name: /Đối tác/ })).toBeNull();
  });

  it('blocks direct role-management access for a non-staff target', async () => {
    harness.getUser.mockResolvedValue(targetUser({ role: 'FAMILY' }));
    render(<UpdateUserRolePage />);

    expect(await screen.findByText(/chỉ áp dụng cho tài khoản nhân viên/)).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Lưu thay đổi' })).toHaveProperty('disabled', true);
    expect(screen.getAllByRole('radio').every((radio) => (radio as HTMLInputElement).disabled)).toBe(true);
  });
});
