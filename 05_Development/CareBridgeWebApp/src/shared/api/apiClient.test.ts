// @vitest-environment jsdom

import { AxiosError, AxiosHeaders } from 'axios';
import { describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '../auth/authStore';
import apiClient, {
  isApiRequestSessionCurrent,
  shouldRedirectToMaintenance,
  type ApiSessionBoundRequestConfig,
} from './apiClient';

describe('maintenance response routing', () => {
  it('redirects normal API calls for the maintenance error', () => {
    expect(shouldRedirectToMaintenance(
      503,
      'SYSTEM_MAINTENANCE',
      '/api/v1/community/questions',
    )).toBe(true);
  });

  it('keeps the system configuration recovery endpoint accessible', () => {
    expect(shouldRedirectToMaintenance(
      503,
      'SYSTEM_MAINTENANCE',
      '/api/v1/admin/system-configuration',
    )).toBe(false);
    expect(shouldRedirectToMaintenance(
      503,
      'SYSTEM_MAINTENANCE',
      'http://localhost:8080/api/v1/admin/system-configuration/?refresh=true',
    )).toBe(false);
  });

  it('does not exempt near matches or query-string mentions of the recovery endpoint', () => {
    expect(shouldRedirectToMaintenance(
      503,
      'SYSTEM_MAINTENANCE',
      '/api/v1/admin/system-configuration-export',
    )).toBe(true);
    expect(shouldRedirectToMaintenance(
      503,
      'SYSTEM_MAINTENANCE',
      '/api/v1/community/questions?next=/api/v1/admin/system-configuration',
    )).toBe(true);
  });

  it('does not redirect unrelated failures', () => {
    expect(shouldRedirectToMaintenance(503, 'UPSTREAM_UNAVAILABLE', '/api/v1/community/questions')).toBe(false);
    expect(shouldRedirectToMaintenance(500, 'SYSTEM_MAINTENANCE', '/api/v1/community/questions')).toBe(false);
  });
});

describe('session-bound request isolation', () => {
  it('ignores a late 401 that belongs to a replaced account session', async () => {
    const logout = vi.fn();
    useAuthStore.setState({
      accessToken: 'access-new',
      refreshToken: 'refresh-new',
      user: {
        id: 'user-new',
        phone: '0900000000',
        name: 'New user',
        avatarUrl: null,
        role: 'MOTHER',
      },
      logout,
    });
    const config: ApiSessionBoundRequestConfig = {
      carebridgeSession: { userId: 'user-old', accessToken: 'access-old' },
      adapter: async (requestConfig) => {
        throw new AxiosError(
          'Unauthorized',
          'ERR_BAD_REQUEST',
          requestConfig,
          undefined,
          {
            data: { error: 'TOKEN_EXPIRED' },
            status: 401,
            statusText: 'Unauthorized',
            headers: new AxiosHeaders(),
            config: requestConfig,
          },
        );
      },
    };

    await expect(apiClient.get('/api/v1/firebase/custom-token', config)).rejects.toBeInstanceOf(
      AxiosError,
    );

    expect(isApiRequestSessionCurrent(config.carebridgeSession)).toBe(false);
    expect(logout).not.toHaveBeenCalled();
    expect(useAuthStore.getState().user?.id).toBe('user-new');
    expect(useAuthStore.getState().accessToken).toBe('access-new');
  });
});
