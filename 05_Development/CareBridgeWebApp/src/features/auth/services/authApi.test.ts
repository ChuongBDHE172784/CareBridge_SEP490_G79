import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '../../../shared/api/apiClient';
import { login, loginWithPhone, registerExpert, registerUser, registerWithPhone } from './authApi';

vi.mock('../../../shared/api/apiClient', () => ({ default: { post: vi.fn() } }));
vi.mock('../../../shared/auth/authStore', () => ({ useAuthStore: { getState: vi.fn() } }));

describe('expert registration API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('forces the EXPERT role instead of trusting a UI role selection', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: { message: 'OTP sent', expiresIn: 60, userId: 'u1', otpExpiresAt: null, auth: null } } });
    await registerExpert({ name: 'Dr Test', email: 'expert@example.com', phone: '+84912345678', password: 'Password1' });
    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/auth/register', expect.objectContaining({
      role: 'EXPERT',
      phone: '+84912345678',
      verificationMethod: 'EMAIL',
    }));
  });

  it('calls the canonical password login endpoint and returns the session', async () => {
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

    const response = await login({ email: 'user@example.com', password: 'Password@123' });

    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/auth/login', {
      email: 'user@example.com',
      password: 'Password@123',
    });
    expect(response.accessToken).toBe('access');
  });

  it('rejects an incomplete password-login response', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({
      data: { data: { accessToken: '', refreshToken: 'refresh', user: { id: 'u1' } } },
    });

    await expect(login({ email: 'user@example.com', password: 'Password@123' }))
      .rejects.toThrow('Login response is incomplete');
  });

  it('sends separated email and phone fields with the selected email verification method', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({
      data: { data: { message: 'OTP sent', expiresIn: 60, userId: 'u1', otpExpiresAt: null, auth: null } },
    });

    await registerUser({
      name: 'Nguyen An',
      email: 'an@example.com',
      phone: '+84901234567',
      password: 'Password1',
      role: 'MOTHER',
      verificationMethod: 'EMAIL',
    });

    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/auth/register', expect.objectContaining({
      email: 'an@example.com',
      phone: '+84901234567',
      verificationMethod: 'EMAIL',
    }));
  });

  it('exchanges a Firebase proof for a phone registration session', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: completeAuthResponse() } });

    await registerWithPhone({
      idToken: 'firebase-id-token',
      name: 'Nguyen An',
      email: 'an@example.com',
      phone: '+84901234567',
      password: 'Password1',
      role: 'MOTHER',
    });

    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/auth/phone/register', expect.objectContaining({
      idToken: 'firebase-id-token',
      phone: '+84901234567',
      deviceInfo: expect.any(String),
    }));
  });

  it('exchanges a Firebase proof for a phone login session', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: completeAuthResponse() } });

    await loginWithPhone({ idToken: 'firebase-id-token' });

    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/auth/phone/login', {
      idToken: 'firebase-id-token',
      deviceInfo: expect.any(String),
    });
  });
});

function completeAuthResponse() {
  return {
    accessToken: 'access',
    refreshToken: 'refresh',
    user: {
      id: 'u1',
      name: 'Nguyen An',
      email: 'an@example.com',
      phone: '+84901234567',
      avatarUrl: null,
      role: 'MOTHER',
      accountStatus: 'ACTIVE',
      emailVerified: false,
      phoneVerified: true,
      createdAt: '2026-08-11T00:00:00Z',
    },
  };
}
