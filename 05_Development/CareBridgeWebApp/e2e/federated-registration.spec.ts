import { expect, test } from '@playwright/test';

test('FED-REG-TC-007-WEB separates contacts and lets the user choose the verification channel', async ({ page }) => {
  await page.goto('/register');
  await expect(page.getByLabel('Email')).toBeVisible();
  await expect(page.getByLabel('Số điện thoại')).toBeVisible();
  await expect(page.getByRole('button', { name: /sign up with google/i })).toBeVisible();
  await expect(page.getByRole('radio')).toHaveCount(0);
  await page.getByLabel('Họ và tên').fill('Nguyễn An');
  await page.getByLabel('Email').fill('an@example.test');
  await page.getByLabel('Số điện thoại').fill('+84901234567');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('Password1!');
  await page.getByLabel('Nhập lại mật khẩu').fill('Password1!');
  await page.getByRole('button', { name: /tiếp tục xác thực/i }).click();
  await expect(page).toHaveURL(/\/register\/verify$/);
  await expect(page.getByRole('button', { name: /nhận mã qua email/i })).toBeVisible();
  await expect(page.getByRole('button', { name: /nhận mã qua sms/i })).toBeVisible();
});
