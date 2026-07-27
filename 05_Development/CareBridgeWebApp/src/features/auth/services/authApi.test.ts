import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '../../../shared/api/apiClient';
import { loginDirect, registerExpert } from './authApi';

vi.mock('../../../shared/api/apiClient', () => ({ default: { post: vi.fn() } }));
vi.mock('../../../shared/auth/authStore', () => ({ useAuthStore: { getState: vi.fn() } }));

describe('expert registration API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('forces the EXPERT role instead of trusting a UI role selection', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: { message: 'OTP sent', expiresIn: 60, userId: 'u1', otpExpiresAt: null, auth: null } } });
    await registerExpert({ name: 'Dr Test', email: 'expert@example.com', password: 'Password1' });
    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/auth/register', expect.objectContaining({ role: 'EXPERT' }));
  });

  it('calls the local/test direct login endpoint', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({
      data: {
        data: {
          accessToken: 'access',
          refreshToken: 'refresh',
          user: {
            id: 'u1',
            name: 'Admin Test',
            email: 'admin@carebridge.dev',
            phone: null,
            avatarUrl: null,
            role: 'SYSTEM_ADMIN',
            accountStatus: 'ACTIVE',
            emailVerified: true,
            phoneVerified: false,
            createdAt: '2026-07-27T00:00:00Z',
          },
        },
      },
    });

    await loginDirect({ email: 'admin@carebridge.dev', password: 'Test@1234' });

    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/auth/login-direct', {
      email: 'admin@carebridge.dev',
      password: 'Test@1234',
    });
  });
});
