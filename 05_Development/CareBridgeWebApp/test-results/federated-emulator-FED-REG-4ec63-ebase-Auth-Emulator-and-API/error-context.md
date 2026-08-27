# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: federated-emulator.spec.ts >> FED-REG-TC-009-WEB completes Google registration through Firebase Auth Emulator and API
- Location: e2e\federated-emulator.spec.ts:60:1

# Error details

```
Error: locator.click: Target page, context or browser has been closed
Call log:
  - waiting for getByRole('button', { name: /add new account/i })
    - waiting for navigation to finish...

```

# Page snapshot

```yaml
- generic [active] [ref=e1]:
  - main [ref=e3]:
    - generic [ref=e4]:
      - heading "Create your CareBridge account" [level=1] [ref=e5]
      - paragraph [ref=e6]: Choose your CareBridge role after your identity is verified.
      - status [ref=e7]: Unable to create the account. Use your existing sign-in method if this contact is already registered.
      - button "Sign up with Google" [ref=e8] [cursor=pointer]
      - button "Sign up with phone" [ref=e9] [cursor=pointer]
  - iframe [ref=e10]:
    
```

# Test source

```ts
  1  | import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
  2  | 
  3  | const emulatorBaseUrl = 'http://127.0.0.1:9099';
  4  | const emulatorProjectId = 'demo-carebridge';
  5  | 
  6  | interface VerificationCode {
  7  |   code: string;
  8  |   phoneNumber: string;
  9  | }
  10 | 
  11 | async function verificationCodeFor(request: APIRequestContext, phoneNumber: string) {
  12 |   for (let attempt = 0; attempt < 20; attempt += 1) {
  13 |     const response = await request.get(
  14 |       `${emulatorBaseUrl}/emulator/v1/projects/${emulatorProjectId}/verificationCodes`,
  15 |     );
  16 |     if (response.ok()) {
  17 |       const body = await response.json() as { verificationCodes?: VerificationCode[] };
  18 |       const match = body.verificationCodes?.find((entry) => entry.phoneNumber === phoneNumber);
  19 |       if (match?.code) return match.code;
  20 |     }
  21 |     await new Promise((resolve) => setTimeout(resolve, 100));
  22 |   }
  23 |   throw new Error(`No emulator verification code was created for ${phoneNumber}`);
  24 | }
  25 | 
  26 | async function expectAuthenticatedProfile(page: Page) {
  27 |   await expect.poll(() => page.evaluate(() => {
  28 |     const stored = localStorage.getItem('carebridge-auth');
  29 |     if (!stored) return null;
  30 |     return (JSON.parse(stored) as { state?: { accessToken?: string } }).state?.accessToken ?? null;
  31 |   }), { timeout: 30_000 }).not.toBeNull();
  32 |   await expect(page).toHaveURL(/\/account\/profile$/, { timeout: 30_000 });
  33 | }
  34 | 
  35 | test('FED-REG-TC-008-WEB completes phone registration through Firebase Auth Emulator and API', async ({
  36 |   page,
  37 |   request,
  38 | }) => {
  39 |   const suffix = String(Date.now()).slice(-9);
  40 |   const phoneNumber = `+1555${suffix}`;
  41 | 
  42 |   page.on('dialog', async (dialog) => {
  43 |     if (dialog.message().startsWith('Phone number')) {
  44 |       await dialog.accept(phoneNumber);
  45 |       return;
  46 |     }
  47 |     if (dialog.message().startsWith('Enter the SMS')) {
  48 |       await dialog.accept(await verificationCodeFor(request, phoneNumber));
  49 |       return;
  50 |     }
  51 |     await dialog.dismiss();
  52 |   });
  53 | 
  54 |   await page.goto('/register');
  55 |   await page.getByRole('button', { name: /sign up with phone/i }).click();
  56 | 
  57 |   await expectAuthenticatedProfile(page);
  58 | });
  59 | 
  60 | test('FED-REG-TC-009-WEB completes Google registration through Firebase Auth Emulator and API', async ({
  61 |   page,
  62 | }) => {
  63 |   const email = `carebridge.e2e.${Date.now()}@example.test`;
  64 | 
  65 |   await page.goto('/register');
  66 |   const popupPromise = page.waitForEvent('popup');
  67 |   await page.getByRole('button', { name: /sign up with google/i }).click();
  68 |   const popup = await popupPromise;
  69 |   await popup.waitForLoadState('domcontentloaded');
  70 | 
> 71 |   await popup.getByRole('button', { name: /add new account/i }).click();
     |                                                                 ^ Error: locator.click: Target page, context or browser has been closed
  72 |   await popup.locator('#email-input').fill(email);
  73 |   await popup.locator('#display-name-input').fill('CareBridge Emulator User');
  74 |   await Promise.all([
  75 |     popup.waitForEvent('close'),
  76 |     popup.getByRole('button', { name: /sign in with google/i }).click(),
  77 |   ]);
  78 | 
  79 |   await expectAuthenticatedProfile(page);
  80 | });
  81 | 
```