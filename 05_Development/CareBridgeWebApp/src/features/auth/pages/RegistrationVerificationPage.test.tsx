// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import RegistrationVerificationPage from './RegistrationVerificationPage';
import {
  clearRegistrationDraft,
  setRegistrationDraft,
} from '../services/registrationVerificationCoordinator';

const harness = vi.hoisted(() => ({
  registerUser: vi.fn(),
  registerWithPhone: vi.fn(),
  resendOtp: vi.fn(),
  verifyOtp: vi.fn(),
  clearPhoneVerification: vi.fn(),
  confirmPhoneVerificationCode: vi.fn(),
  sendPhoneVerificationCode: vi.fn(),
}));

vi.mock('../services/authApi', () => ({
  registerUser: harness.registerUser,
  registerWithPhone: harness.registerWithPhone,
  resendOtp: harness.resendOtp,
  verifyOtp: harness.verifyOtp,
}));

vi.mock('../services/firebaseAuth', () => ({
  clearPhoneVerification: harness.clearPhoneVerification,
  confirmPhoneVerificationCode: harness.confirmPhoneVerificationCode,
  sendPhoneVerificationCode: harness.sendPhoneVerificationCode,
}));

describe('registration verification channel selection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setRegistrationDraft({
      name: 'Nguyễn An',
      email: 'an@example.test',
      phone: '+84901234567',
      password: 'Password1!',
      role: 'MOTHER',
    });
    harness.registerUser.mockResolvedValue({
      message: 'OTP sent',
      expiresIn: 300,
      userId: 'user-1',
      otpExpiresAt: '2026-08-12T04:00:00Z',
      auth: null,
    });
    harness.sendPhoneVerificationCode.mockResolvedValue(undefined);
  });

  afterEach(() => {
    cleanup();
    clearRegistrationDraft();
  });

  it('offers Email and SMS only after the complete draft is handed off', () => {
    render(<MemoryRouter><RegistrationVerificationPage /></MemoryRouter>);

    expect(screen.getByRole('button', { name: /nhận mã qua email/i })).toBeTruthy();
    expect(screen.getByRole('button', { name: /nhận mã qua sms/i })).toBeTruthy();
    expect(screen.queryByText('Password1!')).toBeNull();
  });

  it('starts email registration with both contacts and locks the channel', async () => {
    render(<MemoryRouter><RegistrationVerificationPage /></MemoryRouter>);
    fireEvent.click(screen.getByRole('button', { name: /nhận mã qua email/i }));

    await waitFor(() => expect(harness.registerUser).toHaveBeenCalledWith({
      name: 'Nguyễn An',
      email: 'an@example.test',
      phone: '+84901234567',
      password: 'Password1!',
      role: 'MOTHER',
      verificationMethod: 'EMAIL',
    }));
    expect(await screen.findByLabelText('Mã xác thực')).toBeTruthy();
    expect(screen.queryByRole('button', { name: /nhận mã qua sms/i })).toBeNull();
  });

  it('does not offer SMS to an expert draft', () => {
    setRegistrationDraft({
      name: 'Bác sĩ An',
      email: 'doctor@example.test',
      phone: '+84901234567',
      password: 'Password1!',
      role: 'EXPERT',
    });
    render(<MemoryRouter><RegistrationVerificationPage /></MemoryRouter>);

    expect(screen.getByRole('button', { name: /nhận mã qua email/i })).toBeTruthy();
    expect(screen.queryByRole('button', { name: /nhận mã qua sms/i })).toBeNull();
  });
});
