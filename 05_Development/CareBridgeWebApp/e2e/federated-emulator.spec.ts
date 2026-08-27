import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

const emulatorBaseUrl = 'http://127.0.0.1:9099';
const emulatorProjectId = 'demo-carebridge';

interface VerificationCode {
  code: string;
  phoneNumber: string;
}

async function verificationCodeFor(request: APIRequestContext, phoneNumber: string) {
  for (let attempt = 0; attempt < 20; attempt += 1) {
    const response = await request.get(
      `${emulatorBaseUrl}/emulator/v1/projects/${emulatorProjectId}/verificationCodes`,
    );
    if (response.ok()) {
      const body = await response.json() as { verificationCodes?: VerificationCode[] };
      const match = body.verificationCodes?.find((entry) => entry.phoneNumber === phoneNumber);
      if (match?.code) return match.code;
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error(`No emulator verification code was created for ${phoneNumber}`);
}

async function expectAuthenticatedProfile(page: Page, expectedRoute?: RegExp) {
  await expect.poll(() => page.evaluate(() => {
    const stored = localStorage.getItem('carebridge-auth');
    if (!stored) return null;
    return (JSON.parse(stored) as { state?: { accessToken?: string } }).state?.accessToken ?? null;
  }), { timeout: 30_000 }).not.toBeNull();
  await expect(page).toHaveURL(expectedRoute ?? /\/(?!login|register)[^/]+/, { timeout: 30_000 });
}

test('FED-REG-TC-008-WEB completes phone registration through Firebase Auth Emulator and API', async ({
  page,
  request,
}) => {
  const suffix = String(Date.now()).slice(-8);
  const phoneNumber = `+849${suffix}`;

  await page.goto('/register');
  await page.getByLabel('Họ và tên').fill('CareBridge Emulator User');
  await page.getByLabel('Email').fill(`phone.${Date.now()}@example.test`);
  await page.getByLabel('Số điện thoại').fill(phoneNumber);
  await page.getByLabel('Mật khẩu', { exact: true }).fill('Password1!');
  await page.getByLabel('Nhập lại mật khẩu').fill('Password1!');
  await page.getByRole('button', { name: /tiếp tục xác thực/i }).click();
  await page.getByRole('button', { name: /nhận mã qua sms/i }).click();
  await page.getByLabel(/^mã xác thực$/i).fill(await verificationCodeFor(request, phoneNumber));
  await page.getByRole('button', { name: /xác nhận và tạo tài khoản/i }).click();

  await expectAuthenticatedProfile(page, /\/direct-chats$/);
});

test('FED-REG-TC-009-WEB completes Google registration through Firebase Auth Emulator and API', async ({
  page,
}) => {
  const email = `carebridge.e2e.${Date.now()}@example.test`;

  await page.goto('/register');
  const popupPromise = page.waitForEvent('popup');
  await page.getByRole('button', { name: /sign up with google/i }).click();
  const popup = await popupPromise;
  await popup.waitForLoadState('domcontentloaded');

  await popup.getByRole('button', { name: /add new account/i }).click();
  await popup.locator('#email-input').fill(email);
  await popup.locator('#display-name-input').fill('CareBridge Emulator User');
  await Promise.all([
    popup.waitForEvent('close'),
    popup.getByRole('button', { name: /sign in with google/i }).click(),
  ]);

  await expectAuthenticatedProfile(page);
});
