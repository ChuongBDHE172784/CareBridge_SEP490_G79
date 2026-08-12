import { beforeEach, describe, expect, it, vi } from 'vitest';

const firebaseMocks = vi.hoisted(() => ({
  confirm: vi.fn(),
  getIdToken: vi.fn(),
  signInWithPhoneNumber: vi.fn(),
}));

vi.mock('firebase/app', () => ({
  getApp: vi.fn(),
  getApps: vi.fn(() => []),
  initializeApp: vi.fn(() => ({})),
}));

vi.mock('firebase/auth', () => ({
  connectAuthEmulator: vi.fn(),
  getAuth: vi.fn(() => ({})),
  GoogleAuthProvider: class GoogleAuthProvider {},
  RecaptchaVerifier: class RecaptchaVerifier {
    clear = vi.fn();
  },
  signInWithPhoneNumber: firebaseMocks.signInWithPhoneNumber,
  signInWithPopup: vi.fn(),
}));

import {
  clearPhoneVerification,
  confirmPhoneVerificationCode,
  sendPhoneVerificationCode,
} from './firebaseAuth';

describe('Firebase phone verification', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubEnv('VITE_FIREBASE_API_KEY', 'test-api-key');
    clearPhoneVerification();
  });

  it('keeps the confirmation session after an invalid code and requests a fresh token on success', async () => {
    firebaseMocks.signInWithPhoneNumber.mockResolvedValue({
      confirm: firebaseMocks.confirm,
    });
    firebaseMocks.confirm
      .mockRejectedValueOnce(new Error('invalid verification code'))
      .mockResolvedValueOnce({ user: { getIdToken: firebaseMocks.getIdToken } });
    firebaseMocks.getIdToken.mockResolvedValue('fresh-id-token');

    await sendPhoneVerificationCode('+84901234567');

    await expect(confirmPhoneVerificationCode('000000')).rejects.toThrow(
      'invalid verification code',
    );
    await expect(confirmPhoneVerificationCode('123456')).resolves.toBe(
      'fresh-id-token',
    );
    await expect(confirmPhoneVerificationCode('123456')).resolves.toBe(
      'fresh-id-token',
    );
    expect(firebaseMocks.confirm).toHaveBeenCalledTimes(2);
    expect(firebaseMocks.getIdToken).toHaveBeenCalledWith(true);
  });

  it('refreshes the confirmed proof when a resend attempt fails', async () => {
    firebaseMocks.signInWithPhoneNumber
      .mockResolvedValueOnce({ confirm: firebaseMocks.confirm })
      .mockRejectedValueOnce(new Error('network unavailable'));
    firebaseMocks.confirm.mockResolvedValueOnce({
      user: { getIdToken: firebaseMocks.getIdToken },
    });
    firebaseMocks.getIdToken
      .mockResolvedValueOnce('confirmed-id-token')
      .mockResolvedValueOnce('refreshed-id-token');

    await sendPhoneVerificationCode('+84901234567');
    await expect(confirmPhoneVerificationCode('123456')).resolves.toBe(
      'confirmed-id-token',
    );
    await expect(sendPhoneVerificationCode('+84901234567')).rejects.toThrow(
      'network unavailable',
    );
    await expect(confirmPhoneVerificationCode('123456')).resolves.toBe(
      'refreshed-id-token',
    );
    expect(firebaseMocks.confirm).toHaveBeenCalledTimes(1);
  });

  it('does not publish a phone challenge that completes after cleanup', async () => {
    let completeStart!: (value: { confirm: typeof firebaseMocks.confirm }) => void;
    firebaseMocks.signInWithPhoneNumber.mockReturnValueOnce(new Promise((resolve) => {
      completeStart = resolve;
    }));

    const pending = sendPhoneVerificationCode('+84901234567');
    clearPhoneVerification();
    completeStart({ confirm: firebaseMocks.confirm });

    await expect(pending).rejects.toThrow('PHONE_VERIFICATION_SUPERSEDED');
    await expect(confirmPhoneVerificationCode('123456')).rejects.toThrow(
      'PHONE_VERIFICATION_NOT_STARTED',
    );
  });

  it('does not publish a proof when cleanup races an in-flight code confirmation', async () => {
    let resolveConfirmation!: (value: { user: { getIdToken: typeof firebaseMocks.getIdToken } }) => void;
    firebaseMocks.signInWithPhoneNumber.mockResolvedValue({
      confirm: vi.fn(() => new Promise(resolve => { resolveConfirmation = resolve; })),
    });
    firebaseMocks.getIdToken.mockResolvedValue('stale-id-token');

    await sendPhoneVerificationCode('+84901234567');
    const pending = confirmPhoneVerificationCode('123456');
    clearPhoneVerification();
    resolveConfirmation({ user: { getIdToken: firebaseMocks.getIdToken } });

    await expect(pending).rejects.toThrow('PHONE_VERIFICATION_SUPERSEDED');
    await expect(confirmPhoneVerificationCode('123456')).rejects.toThrow(
      'PHONE_VERIFICATION_NOT_STARTED',
    );
  });
});
