# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: federated-login.spec.ts >> federated login keeps keyboard focus visible and exposes a neutral live region
- Location: e2e\federated-login.spec.ts:9:1

# Error details

```
Error: expect(locator).toBeFocused() failed

Locator: getByRole('button', { name: /continue with google/i })
Expected: focused
Timeout: 5000ms
Error: element(s) not found

Call log:
  - Expect "toBeFocused" with timeout 5000ms
  - waiting for getByRole('button', { name: /continue with google/i })

```

```yaml
- main:
  - img "CareBridge Logo"
  - heading "CareBridge" [level=1]
  - paragraph: Hệ thống quản lý y tế
  - status
  - text: Email hoặc Số điện thoại
  - textbox "Email hoặc Số điện thoại":
    - /placeholder: Nhập email hoặc SĐT
  - text: Mật khẩu
  - textbox "Mật khẩu":
    - /placeholder: Nhập mật khẩu
  - button "Hiện mật khẩu"
  - checkbox "Ghi nhớ tôi"
  - text: Ghi nhớ tôi
  - link "Quên mật khẩu?":
    - /url: /forgot-password
  - button "Đăng nhập"
  - paragraph:
    - text: Bạn là bác sĩ/chuyên gia?
    - link "Đăng ký chuyên gia":
      - /url: /expert/register
  - paragraph:
    - text: Cần trợ giúp?
    - link "Hỗ trợ kỹ thuật":
      - /url: "#"
```

# Test source

```ts
  1  | import { expect, test } from '@playwright/test';
  2  | 
  3  | test('FED-LOGIN-TC-007-WEB exposes accessible Google and phone login controls', async ({ page }) => {
  4  |   await page.goto('/login');
  5  |   await expect(page.getByRole('button', { name: /continue with google/i })).toBeVisible();
  6  |   await expect(page.getByRole('button', { name: /continue with phone/i })).toBeVisible();
  7  | });
  8  | 
  9  | test('federated login keeps keyboard focus visible and exposes a neutral live region', async ({ page }) => {
  10 |   await page.goto('/login');
  11 |   await page.keyboard.press('Tab');
> 12 |   await expect(page.getByRole('button', { name: /continue with google/i })).toBeFocused();
     |                                                                             ^ Error: expect(locator).toBeFocused() failed
  13 |   await expect(page.getByRole('status')).toBeAttached();
  14 | });
  15 | 
```