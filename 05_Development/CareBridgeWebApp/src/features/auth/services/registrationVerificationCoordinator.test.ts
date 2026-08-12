import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  clearRegistrationDraft,
  getRegistrationDraft,
  setRegistrationDraft,
} from './registrationVerificationCoordinator';

describe('registration verification coordinator', () => {
  afterEach(() => {
    clearRegistrationDraft();
    vi.restoreAllMocks();
  });

  it('keeps the registration draft in memory without browser persistence', () => {
    const localStorageWrite = vi.spyOn(localStorage, 'setItem');
    const sessionStorageWrite = vi.spyOn(sessionStorage, 'setItem');
    setRegistrationDraft({
      name: 'Nguyễn An',
      email: 'an@example.test',
      phone: '+84901234567',
      password: 'Password1!',
      role: 'MOTHER',
    });

    expect(getRegistrationDraft()).toEqual(expect.objectContaining({
      email: 'an@example.test',
      phone: '+84901234567',
    }));
    expect(localStorageWrite).not.toHaveBeenCalled();
    expect(sessionStorageWrite).not.toHaveBeenCalled();
  });

  it('returns a copy and clears the sensitive draft explicitly', () => {
    setRegistrationDraft({
      name: 'Nguyễn An',
      email: 'an@example.test',
      phone: '+84901234567',
      password: 'Password1!',
      role: 'MOTHER',
    });
    const first = getRegistrationDraft()!;
    first.email = 'changed@example.test';
    expect(getRegistrationDraft()?.email).toBe('an@example.test');

    clearRegistrationDraft();
    expect(getRegistrationDraft()).toBeUndefined();
  });
});
