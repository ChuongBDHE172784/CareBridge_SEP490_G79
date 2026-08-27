import { test, expect } from '@playwright/test';

const enabled = process.env.MF03_E2E_ENABLED === 'true';

test.describe('MF-03 baby-care hub', () => {
  test.skip(!enabled, 'MF03_E2E_ENABLED is not true; API-backed MF-03 E2E is opt-in.');

  test.beforeEach(async ({ page }) => {
    await page.route('**/api/v1/babies', async (route) => {
      if (route.request().method() !== 'GET') return route.fallback();
      await route.fulfill({ json: { data: [
        { id: 'baby-a', nickname: 'Baby A', isActive: true },
        { id: 'baby-b', nickname: 'Baby B', isActive: false },
      ] } });
    });
    await page.route('**/api/v1/babies/*/active', async (route) => {
      await route.fulfill({ json: { data: { id: 'baby-b', nickname: 'Baby B', isActive: true } } });
    });
    await page.route('**/api/v1/babies/*/daily-logs', async (route) => {
      await route.fulfill({ json: { data: [{ id: 'log-1' }] } });
    });
    await page.route('**/api/v1/babies/*/growth-measurements*', async (route) => {
      await route.fulfill({ json: { data: { content: [{ id: 'growth-1' }] } } });
    });
    await page.route('**/api/v1/babies/*/milestones', async (route) => {
      await route.fulfill({ json: { data: [{ id: 'milestone-1' }] } });
    });
    await page.route('**/api/v1/vaccination/babies/*/records', async (route) => {
      await route.fulfill({ json: { data: [{ id: 'vaccination-1' }] } });
    });
  });

  test('mother can switch baby and see API-backed baby-scoped hub data', async ({ page }) => {
    await page.goto('/mother/baby-care');
    await expect(page.getByRole('heading', { name: /baby care hub/i })).toBeVisible();
    await expect(page.getByTestId('active-baby-name')).toHaveText(/Baby A/);
    await page.getByRole('combobox', { name: /switch baby/i }).selectOption({ label: 'Baby B' });
    await expect(page.getByTestId('active-baby-name')).toHaveText(/Baby B/);
    await expect(page.getByTestId('baby-care-journal')).toContainText('1 records');
    await expect(page.getByTestId('baby-care-growth')).toContainText('1 records');
    await expect(page.getByTestId('baby-care-milestones')).toContainText('1 records');
    await expect(page.getByTestId('baby-care-vaccinations')).toContainText('1 records');
  });

  test('deep link cannot expose another baby resource', async ({ page }) => {
    await page.goto('/mother/babies/baby-a/daily-logs/log-b');
    await expect(page.getByRole('alert')).toContainText(/not found|access denied/i);
  });

  test('hub exposes accessible names for navigation and data cards', async ({ page }) => {
    await page.goto('/mother/baby-care');
    await expect(page.getByRole('combobox', { name: /switch baby/i })).toBeVisible();
    for (const id of ['baby-care-journal', 'baby-care-growth', 'baby-care-milestones', 'baby-care-vaccinations']) {
      await expect(page.getByTestId(id)).toHaveRole('article');
    }
  });
});
