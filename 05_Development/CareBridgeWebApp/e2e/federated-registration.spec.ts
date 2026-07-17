import { expect, test } from '@playwright/test';

test('FED-REG-TC-007-WEB offers federated registration with role completion', async ({ page }) => {
  await page.goto('/register');
  await expect(page.getByRole('button', { name: /sign up with google/i })).toBeVisible();
  await expect(page.getByRole('button', { name: /sign up with phone/i })).toBeVisible();
  await expect(page.getByText(/choose your carebridge role/i)).toBeVisible();
});
