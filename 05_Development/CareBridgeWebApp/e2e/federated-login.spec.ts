import { expect, test } from '@playwright/test';

test('FED-LOGIN-TC-007-WEB exposes accessible email, Google and phone login controls', async ({ page }) => {
  await page.goto('/login');
  await expect(page.getByRole('button', { name: /continue with google/i })).toBeVisible();
  await expect(page.getByRole('button', { name: /continue with phone/i })).toBeVisible();
  await expect(page.getByLabel('Email')).toBeVisible();

  await page.getByRole('button', { name: /continue with phone/i }).click();
  await expect(page.getByLabel('Số điện thoại')).toBeVisible();
  await expect(page.getByText(/nếu số điện thoại chưa có tài khoản/i)).toBeVisible();
  await expect(page.getByRole('button', { name: /tiếp tục bằng số điện thoại/i })).toBeVisible();
});

test('federated login keeps keyboard focus visible and exposes a neutral live region', async ({ page }) => {
  await page.goto('/login');
  await page.keyboard.press('Tab');
  await expect(page.getByRole('button', { name: 'Email' })).toBeFocused();
  await expect(page.getByRole('status')).toBeAttached();
});
