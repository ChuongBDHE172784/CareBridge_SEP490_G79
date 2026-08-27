import { describe, expect, it } from 'vitest';
import { normalizeVietnamesePhone } from './vietnamesePhone';

describe('normalizeVietnamesePhone', () => {
  it('accepts the local 0-prefixed form people actually type', () => {
    expect(normalizeVietnamesePhone('0342149031')).toBe('+84342149031');
    expect(normalizeVietnamesePhone('0901234567')).toBe('+84901234567');
  });

  it('accepts the country-code and canonical forms', () => {
    expect(normalizeVietnamesePhone('84901234567')).toBe('+84901234567');
    expect(normalizeVietnamesePhone('+84901234567')).toBe('+84901234567');
  });

  it('ignores the separators a phone number is often written with', () => {
    expect(normalizeVietnamesePhone('034 214 9031')).toBe('+84342149031');
    expect(normalizeVietnamesePhone('(034) 214-9031')).toBe('+84342149031');
    expect(normalizeVietnamesePhone('  0342149031  ')).toBe('+84342149031');
  });

  it('rejects anything that is not a Vietnamese mobile number', () => {
    expect(normalizeVietnamesePhone('')).toBeNull();
    expect(normalizeVietnamesePhone('0212345678')).toBeNull(); // đầu số cố định
    expect(normalizeVietnamesePhone('034214903')).toBeNull(); // thiếu một chữ số
    expect(normalizeVietnamesePhone('03421490311')).toBeNull(); // thừa một chữ số
    expect(normalizeVietnamesePhone('+1202555019')).toBeNull(); // không phải Việt Nam
    expect(normalizeVietnamesePhone('không phải số')).toBeNull();
  });
});
