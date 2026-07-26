import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '../../../shared/api/apiClient';
import { registerExpert } from './authApi';

vi.mock('../../../shared/api/apiClient', () => ({ default: { post: vi.fn() } }));
vi.mock('../../../shared/auth/authStore', () => ({ useAuthStore: { getState: vi.fn() } }));

describe('expert registration API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('forces the EXPERT role instead of trusting a UI role selection', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: { message: 'OTP sent', expiresIn: 60, userId: 'u1', otpExpiresAt: null, auth: null } } });
    await registerExpert({ name: 'Dr Test', email: 'expert@example.com', password: 'Password1' });
    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/auth/register', expect.objectContaining({ role: 'EXPERT' }));
  });
});
