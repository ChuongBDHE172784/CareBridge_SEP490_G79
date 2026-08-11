# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: federated-emulator.spec.ts >> FED-REG-TC-008-WEB completes phone registration through Firebase Auth Emulator and API
- Location: e2e\federated-emulator.spec.ts:35:1

# Error details

```
Error: expect(received).not.toBeNull()

Received: null

Call Log:
- Test timeout of 30000ms exceeded
```

# Page snapshot

```yaml
- generic [ref=e1]:
  - main [ref=e3]:
    - generic [ref=e4]:
      - heading "Create your CareBridge account" [level=1] [ref=e5]
      - paragraph [ref=e6]: Choose your CareBridge role after your identity is verified.
      - status [ref=e7]
      - button "Sign up with Google" [disabled] [ref=e8]
      - button "Sending code..." [disabled] [ref=e9]
      - iframe [ref=e12]:
        - generic [ref=f1e5]:
          - generic [ref=f1e6]:
            - text: protected by
            - strong [ref=f1e7]: reCAPTCHA
          - generic [ref=f1e9]:
            - text: reCAPTCHA is changing its terms of service.
            - link "Take action." [ref=f1e10] [cursor=pointer]:
              - /url: https://google.com/recaptcha/admin/migrate
  - iframe [active] [ref=e15]:
    - dialog [ref=f4e3]:
      - generic [ref=f4e4]:
        - generic [ref=f4e7]:
          - text: Select all squares with
          - strong [ref=f4e8]: motorcycles
          - generic [ref=f4e9]: If there are none, click skip
        - table [ref=f4e12]:
          - rowgroup [ref=f4e13]:
            - row [ref=f4e14]:
              - button [ref=f4e15]
              - button [ref=f4e18]
              - button [ref=f4e21]
              - button [ref=f4e24]
            - row [ref=f4e27]:
              - button [ref=f4e28]
              - button [ref=f4e31]
              - button [ref=f4e34]
              - button [ref=f4e37]
            - row [ref=f4e40]:
              - button [ref=f4e41]
              - button [ref=f4e44]
              - button [ref=f4e47]
              - button [ref=f4e50]
            - row [ref=f4e53]:
              - button [ref=f4e54]
              - button [ref=f4e57]
              - button [ref=f4e60]
              - button [ref=f4e63]
      - generic [ref=f4e69]:
        - generic [ref=f4e70]:
          - button "Get a new challenge" [ref=f4e72] [cursor=pointer]
          - button "Get an audio challenge" [ref=f4e74] [cursor=pointer]
          - button "Help" [ref=f4e76] [cursor=pointer]
        - button "Skip" [ref=f4e78] [cursor=pointer]
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
> 31 |   }), { timeout: 30_000 }).not.toBeNull();
     |                                ^ Error: expect(received).not.toBeNull()
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
  71 |   await popup.getByRole('button', { name: /add new account/i }).click();
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