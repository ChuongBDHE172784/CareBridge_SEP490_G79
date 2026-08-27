// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { UserRole } from '../../shared/auth/authStore';
import AdminLayout from './AdminLayout';
import ExpertLayout from './ExpertLayout';
import ContentPortalSidebar from '../../features/contentManagement/components/ContentPortalSidebar';

const harness = vi.hoisted(() => ({ role: 'SYSTEM_ADMIN' as UserRole }));

vi.mock('../../shared/auth/useAuth', () => ({
  useAuth: () => ({
    user: { id: 'user-1', name: 'Nguyễn An', phone: '+84901234567', role: harness.role },
    logout: vi.fn(),
    hasRole: (role: UserRole) => harness.role === role,
    hasAnyRole: (...roles: UserRole[]) => roles.includes(harness.role),
  }),
}));

afterEach(cleanup);

describe('portal password settings navigation', () => {
  it.each([
    ['SYSTEM_ADMIN', '/admin/settings/password'],
    ['MODERATOR', '/moderator/settings/password'],
  ] as const)('links %s to its guarded settings route', (role, expectedPath) => {
    harness.role = role;
    render(<MemoryRouter><AdminLayout /></MemoryRouter>);

    expect(screen.getByRole('link', { name: /cài đặt/i }).getAttribute('href')).toBe(expectedPath);
  });

  it('links the expert portal footer to expert settings', () => {
    harness.role = 'EXPERT';
    render(<MemoryRouter><ExpertLayout /></MemoryRouter>);

    expect(screen.getByRole('link', { name: /cài đặt/i }).getAttribute('href')).toBe('/expert/settings/password');
  });

  it('links the content portal footer to content settings', () => {
    harness.role = 'CONTENT_ADMIN';
    render(<MemoryRouter><ContentPortalSidebar /></MemoryRouter>);

    expect(screen.getByRole('link', { name: /cài đặt/i }).getAttribute('href')).toBe('/content/settings/password');
  });

  it('returns a moderator visiting content topics to moderator settings', () => {
    harness.role = 'MODERATOR';
    render(<MemoryRouter><ContentPortalSidebar /></MemoryRouter>);

    expect(screen.getByRole('link', { name: /cài đặt/i }).getAttribute('href')).toBe('/moderator/settings/password');
  });
});
