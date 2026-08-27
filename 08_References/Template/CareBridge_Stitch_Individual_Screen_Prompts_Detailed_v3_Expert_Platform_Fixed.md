# CareBridge — Stitch Individual Screen Prompts (Detailed v3 — Expert App / Web Portal Separated)

## What this file is

This file contains **222 standalone prompts**, one for every row in **Table 261 — Screen Descriptions** of `4_Functional_Requirements.md`. Each prompt below is self-contained: paste **one prompt only** into Stitch to generate **one complete screen/frame**. **Detailed v3 corrects a platform-generation defect in the old v2 pack: every Expert App prompt is now a hard-locked 390 × 844 mobile phone UI; every Expert Web Portal prompt remains a hard-locked 1440 × 1024 desktop B2B portal.**

The prompt structure intentionally follows the detailed SRS style you used before: platform, primary role, reference screen name, purpose, exact canvas/layout requirements, detailed required components/fields, interaction states, safety logic, and one-screen output rule. The default visual output is **high-fidelity product UI** because the CareBridge requirement is to follow the attached Warm Claymorphism UI skill for app screens and a modern white-primary B2B style for web portals.

### How to use

1. Create one Stitch project: **CareBridge UI System**.
2. Copy the text inside the code block for the screen you want. Paste it by itself into Stitch.
3. Generate exactly one frame. Keep all frames in the same project; do not ask Stitch to generate the whole file in one request.
4. The prompt gives field/action/state contracts. If Stitch omits an item, paste the screen prompt again and say: `Revise only frame [ID]. Add every missing required component and state from this prompt; do not create another screen.`
5. The **reference screen name is for the prompt only**. Mobile prompts explicitly forbid displaying technical screen names inside the app UI.

## Expert Platform Lock — Read Before Generating

CareBridge has **two separate expert clients**. Do not mix them.

| Client | Prompt IDs | Required output | Must never appear |
| --- | --- | --- | --- |
| **Expert App** | `CB-033–048`, `CB-134–136`, `CB-146–149`, `CB-182–185`, `CB-208–211` | One **390 × 844 portrait mobile phone** frame. Main signed-in destinations may use a floating bottom navigation. Detail, form, call, map, and confirmation screens use a compact app bar. | Desktop browser chrome, a left sidebar, wide tables, 2–3 column dashboards, or a 1440 × 1024 layout. |
| **Expert Web Portal** | `CB-054–065`, `CB-137–138`, `CB-150`, `CB-186–188`, `CB-212–215` | One **1440 × 1024 desktop B2B portal** frame with an Expert Portal sidebar and operational workspace. | Phone frame, bottom navigation, a mobile-only stacked layout, or consumer-app treatment. |

**Platform precedence rule:** when the prompt says `Platform: Expert App`, the words `app`, `mobile`, `390 × 844`, and `MOBILE ONLY` override any generic interpretation of the word “expert”. When the prompt says `Platform: Expert Web Portal`, the words `portal`, `desktop`, `1440 × 1024`, and `WEB ONLY` override any generic interpretation.

### Optional SRS grayscale switch

The individual prompts default to high-fidelity product UI. To make a low-fidelity SRS wireframe instead, prepend this sentence before **any one** prompt:

```text
SRS WIREFRAME MODE: Keep every required component, field, action, safety rule, and state in the prompt, but render a single low-fidelity grayscale academic wireframe: white background #FFFFFF, panels #F2F2F2/#E6E6E6, 1 px #777777 borders, #111111 text, Arial/Inter, flat rectangles, maximum 4 px radius, no shadows, gradients, photos, colorful icons, illustrations, decorative patterns, 3D, or brand accents.
```

---


### Prompt 001 — `CB-001` — Mobile Welcome

**Platform:** Shared Mobile Apps  
**Primary role:** Guest  
**Functional coverage:** UC-01, UC-03

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-001`
Platform: Shared Mobile Apps
Primary role: Guest
Feature: Authentication & setup
Reference screen name: “Mobile Welcome”
Purpose: Shared entry screen for Mother, Family Member, and Verified Expert mobile apps; role-specific routing is handled after registration or login.
Functional coverage: UC-01, UC-03

Canvas and navigation: MOBILE. No standard app bar and no bottom navigation. The welcome screen is a clean entry canvas; do not add a fake back button.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. CareBridge wordmark at the top; a one-line value statement only, without a hero illustration or long onboarding copy.
2. Primary pill button “Tạo tài khoản” and secondary button “Đăng nhập”, both large enough for touch.
3. Compact text links for “Điều khoản”, “Quyền riêng tư”, and the short medical-safety notice.
4. No role selector here; role routing occurs only after login or registration.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 002 — `CB-002` — Register Account

**Platform:** Shared Mobile Apps  
**Primary role:** Guest  
**Functional coverage:** UC-01

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-002`
Platform: Shared Mobile Apps
Primary role: Guest
Feature: Authentication & setup
Reference screen name: “Register Account”
Purpose: Collects common account credentials and registration details for mobile users, with role-specific fields shown only when required.
Functional coverage: UC-01

Canvas and navigation: MOBILE. Use a simple top app bar with a clear back/close action. Do not render bottom navigation on this auth, setup, invite, or confirmation screen.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Back button; text fields for full/display name and one unique contact field that accepts email or phone.
2. Initial role selector only when the registration flow requires it; show Mother, Family Member, and Verified Expert as mutually exclusive cards/chips.
3. Password and confirm-password fields with show/hide controls and a concise password-policy checklist.
4. Mandatory Terms, Privacy, and safety-notice checkbox; primary “Tạo tài khoản” button and existing-account login link.
5. Inline validation examples: invalid contact, password mismatch, unchecked consent; the CTA has a loading state.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 003 — `CB-003` — Verify OTP

**Platform:** Shared Mobile Apps  
**Primary role:** Guest  
**Functional coverage:** UC-02

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-003`
Platform: Shared Mobile Apps
Primary role: Guest
Feature: Authentication & setup
Reference screen name: “Verify OTP”
Purpose: Validates a one-time password for account activation, recovery, or sensitive account actions across mobile roles.
Functional coverage: UC-02

Canvas and navigation: MOBILE. Use a simple top app bar with a clear back/close action. Do not render bottom navigation on this auth, setup, invite, or confirmation screen.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Back button; masked email/phone that received the code; six separate numeric OTP cells with focus state.
2. Countdown expiry indicator, resend action with disabled state until allowed, and optional channel switch only where supported.
3. Primary “Xác nhận” button; inline incorrect/expired-code message directly below the input.
4. No unrelated personal data or role selection.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 004 — `CB-004` — Login

**Platform:** Shared Mobile Apps  
**Primary role:** Existing CareBridge user  
**Functional coverage:** UC-03

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-004`
Platform: Shared Mobile Apps
Primary role: Existing CareBridge user
Feature: Authentication & setup
Reference screen name: “Login”
Purpose: Authenticates mobile users and routes each role to the correct home screen.
Functional coverage: UC-03

Canvas and navigation: MOBILE. Use a simple top app bar with a clear back/close action. Do not render bottom navigation on this auth, setup, invite, or confirmation screen.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Contact field (email or phone), password field, show/hide password control, and field-level validation.
2. Primary “Đăng nhập” button, “Quên mật khẩu?” link, and “Tạo tài khoản” link.
3. One compact role-routing hint such as “Bạn sẽ được đưa tới không gian phù hợp sau khi đăng nhập”; do not ask the user to choose a role again.
4. Failed-login message must be generic and not reveal whether a specific account exists.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 005 — `CB-005` — Forgot Password

**Platform:** Shared Mobile Apps  
**Primary role:** Guest  
**Functional coverage:** UC-05

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-005`
Platform: Shared Mobile Apps
Primary role: Guest
Feature: Authentication & setup
Reference screen name: “Forgot Password”
Purpose: Starts password recovery for mobile users by collecting the registered email or phone number.
Functional coverage: UC-05

Canvas and navigation: MOBILE. Use a simple top app bar with a clear back/close action. Do not render bottom navigation on this auth, setup, invite, or confirmation screen.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Back button and a single registered email/phone field.
2. Optional delivery-channel selector only if both email and phone are configured.
3. Primary “Gửi mã” CTA and a neutral success state that does not reveal whether the contact exists.
4. Inline invalid-format and network-retry states.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 006 — `CB-006` — Reset Password

**Platform:** Shared Mobile Apps  
**Primary role:** Guest  
**Functional coverage:** UC-06

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-006`
Platform: Shared Mobile Apps
Primary role: Guest
Feature: Authentication & setup
Reference screen name: “Reset Password”
Purpose: Allows mobile users to set a new password after a valid recovery code or link is confirmed.
Functional coverage: UC-06

Canvas and navigation: MOBILE. Use a simple top app bar with a clear back/close action. Do not render bottom navigation on this auth, setup, invite, or confirmation screen.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Back button; new-password and confirm-password fields with show/hide controls.
2. Compact password-policy checklist, mismatch validation, and “Cập nhật mật khẩu” CTA.
3. Success state that returns the user to login; no long educational text.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 007 — `CB-007` — Mother Journey Setup

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-22

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-007`
Platform: Mother Mobile App
Primary role: Mother
Feature: Authentication & setup
Reference screen name: “Mother Journey Setup”
Purpose: Collects the mother’s current stage and key dates to initialize a personalized care journey.
Functional coverage: UC-22

Canvas and navigation: MOBILE. Use a simple top app bar with a clear back/close action. Do not render bottom navigation on this auth, setup, invite, or confirmation screen.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Progress indicator for a short setup flow; stage selector with “Chuẩn bị mang thai”, “Đang mang thai”, and “Sau sinh”.
2. Conditional date fields: last menstrual period and due date for pregnancy; delivery date for postpartum; preparation stage hides medical-only fields.
3. Optional nickname or preference field only where it personalizes the journey; privacy note that data stays private by default.
4. Primary “Bắt đầu hành trình” CTA and clear back/cancel action; validate future and inconsistent dates.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 008 — `CB-008` — Mother Home

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-24, UC-49

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-008`
Platform: Mother Mobile App
Primary role: Mother
Feature: Core experience
Reference screen name: “Mother Home”
Purpose: Shows the mother’s personalized overview, priorities, reminders, alerts, and shortcuts.
Functional coverage: UC-24, UC-49

Canvas and navigation: MOBILE. Use signed-in Mother bottom navigation: Trang chủ · Hành trình · Cộng đồng · Việc cần làm · Hồ sơ. Highlight only the relevant destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Natural contextual header such as “Chào Linh”; notification icon with unread badge and a profile/avatar shortcut.
2. One journey-status card showing current stage/week and a single next action; not a long explanation.
3. “Việc hôm nay” preview with 2–3 priority tasks, one alert card only when action is needed, and quick-add buttons for nhật ký bé, triệu chứng, hoặc nhắc lịch.
4. Compact cards for the active baby, upcoming appointment, and suggested verified content; bottom navigation for the signed-in Mother app.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.

Additional screen-specific interaction notes:
- Tapping a quick action visibly opens the corresponding task, log, or intake path.
- Alert cards show label plus icon, never color alone.
```

### Prompt 009 — `CB-009` — Mother Journey

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-23, UC-25–UC-28, UC-51–UC-53

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-009`
Platform: Mother Mobile App
Primary role: Mother
Feature: Core experience
Reference screen name: “Mother Journey”
Purpose: Displays and manages pre-pregnancy, pregnancy, and postpartum journey information.
Functional coverage: UC-23, UC-25–UC-28, UC-51–UC-53

Canvas and navigation: MOBILE. Use signed-in Mother bottom navigation: Trang chủ · Hành trình · Cộng đồng · Việc cần làm · Hồ sơ. Highlight only the relevant destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Stage timeline or segmented control for preparation, pregnancy, and postpartum; current stage is clearly selected.
2. Key-date card with editable date values; metric-summary cards for only applicable user-entered indicators.
3. Quick actions to add a maternal metric, add postpartum log, manage reminder, and open expense summary.
4. Lightweight trend chart with date range; every measurement card shows source “Bạn nhập”.
5. Overflow action for archive/delete must be a confirmation sheet, not an immediate destructive action.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 010 — `CB-010` — Baby Profiles

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-31–UC-33, UC-192–UC-193

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-010`
Platform: Mother Mobile App
Primary role: Mother
Feature: Core experience
Reference screen name: “Baby Profiles”
Purpose: Lists the baby profiles managed by the mother and provides access to create or open a profile.
Functional coverage: UC-31–UC-33, UC-192–UC-193

Canvas and navigation: MOBILE. Use signed-in Mother bottom navigation: Trang chủ · Hành trình · Cộng đồng · Việc cần làm · Hồ sơ. Highlight only the relevant destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. List/grid of baby profile cards with nickname, age, photo placeholder, active indicator, and a short latest-log summary.
2. “Thêm hồ sơ bé” CTA plus an overflow action for edit/archive; archive must explain data remains retained when linked.
3. Active-baby selector state when more than one profile exists; empty state contains only a direct create CTA.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 011 — `CB-011` — Baby Profile

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-34–UC-38, UC-192, UC-194–UC-197

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-011`
Platform: Mother Mobile App
Primary role: Mother
Feature: Core experience
Reference screen name: “Baby Profile”
Purpose: Shows one baby’s overview, daily logs, growth, milestones, vaccination, and health records.
Functional coverage: UC-34–UC-38, UC-192, UC-194–UC-197

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Header with active baby identity, age, and switch-baby control; no real child photo or identifying details.
2. Short 24-hour log summary for feeding, sleep, diaper, and symptoms, plus one-tap “Ghi nhanh”.
3. Cards/tabs for growth, milestones, vaccination, and health records; show source labels for user-entered vs device-sourced data.
4. Visible actions for add daily log, record milestone, add vaccination record, and view health timeline.
5. Growth visual is trend-only and never labels a medical diagnosis.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 012 — `CB-012` — Health Record Timeline

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-39–UC-42, UC-211

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-012`
Platform: Mother Mobile App
Primary role: Mother
Feature: Core experience
Reference screen name: “Health Record Timeline”
Purpose: Displays maternal and baby health records chronologically with source and category filters.
Functional coverage: UC-39–UC-42, UC-211

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Chronological timeline grouped by date with item icon, category, owner (Mother/Baby), source label, and attachment indicator.
2. Filter chips for owner, record type, source, and date range; search field for record name/tag.
3. Each item has a protected “Xem” action; a compact permission badge appears when the record is currently shared.
4. Add/upload CTA; empty state says no records yet and offers upload, without unnecessary guidance.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 013 — `CB-013` — Today Tasks

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-45–UC-50, UC-212–UC-215

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-013`
Platform: Mother Mobile App
Primary role: Mother
Feature: Core experience
Reference screen name: “Today Tasks”
Purpose: Shows reminders, appointments, checklist items, and family tasks due today.
Functional coverage: UC-45–UC-50, UC-212–UC-215

Canvas and navigation: MOBILE. Use signed-in Mother bottom navigation: Trang chủ · Hành trình · Cộng đồng · Việc cần làm · Hồ sơ. Highlight only the relevant destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Date header and segmented filter “Tất cả / Cần làm / Đã xong”.
2. Grouped task cards for appointment, medication/vitamin, vaccination, checklist, and assigned family task; each contains due time, owner, priority, and completion checkbox.
3. Quick actions to complete, snooze, skip, edit, and open detail; skip/delete require a confirmation sheet.
4. Add button opens a task-type chooser; task conflict or overdue labels are explicit.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 014 — `CB-014` — Community Feed

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-54–UC-59, UC-198–UC-201

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-014`
Platform: Mother Mobile App
Primary role: Mother
Feature: Core experience
Reference screen name: “Community Feed”
Purpose: Displays moderated community questions, answers, topics, and saved content.
Functional coverage: UC-54–UC-59, UC-198–UC-201

Canvas and navigation: MOBILE. Use signed-in Mother bottom navigation: Trang chủ · Hành trình · Cộng đồng · Việc cần làm · Hồ sơ. Highlight only the relevant destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Search entry, topic chips, and feed filters for newest, answered, saved, and verified-expert answers.
2. Post cards show anonymous/public author state, stage/topic, short question excerpt, moderation/answer labels, reactions/bookmark, and report action.
3. Primary “Đặt câu hỏi” CTA; composer entry supports topic, stage/child-age context, anonymous toggle, and urgency label.
4. Expert answers must show verified badge; ordinary answers show “Chia sẻ kinh nghiệm”, never medical authority.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 015 — `CB-015` — AI Symptom Intake

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-60, UC-131–UC-132

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-015`
Platform: Mother Mobile App
Primary role: Mother
Feature: Core experience
Reference screen name: “AI Symptom Intake”
Purpose: Collects symptoms and relevant context through a guided, structured intake flow.
Functional coverage: UC-60, UC-131–UC-132

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Guided multi-step intake with visible step count, one question at a time, and concise selectable answers plus an “Khác” text field.
2. Fields cover who is affected (Mother/Baby), symptom, onset, severity/context, existing care, and red-flag questions when relevant.
3. Clear data-use/AI disclaimer and a “Dừng lại” action; preserve draft only if the user confirms.
4. No diagnostic copy, medication advice, or overwhelming explanatory content; primary CTA advances to the next safe question.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 016 — `CB-016` — Risk Triage Result

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-61

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-016`
Platform: Mother Mobile App
Primary role: Mother
Feature: Core experience
Reference screen name: “Risk Triage Result”
Purpose: Shows the non-diagnostic risk level and the recommended next safe action.
Functional coverage: UC-61

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Prominent risk label with icon and explicit text such as “Cần theo dõi”, “Nên liên hệ chuyên gia”, or “Cần hỗ trợ khẩn”; never rely on color alone.
2. 2–3 short next safe actions, including emergency routing first for red risk.
3. Buttons for “Tìm nơi hỗ trợ gần đây”, “Gọi ngay”, “Chia sẻ với người thân” when the selected permission allows it.
4. Compact disclaimer: this is initial guidance, not diagnosis or emergency replacement; option to review answers or start over.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 017 — `CB-017` — Emergency Map

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-62–UC-65

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-017`
Platform: Mother Mobile App
Primary role: Mother
Feature: Core experience
Reference screen name: “Emergency Map”
Purpose: Shows nearby care facilities, available experts, routes, ETA, quick call, and emergency actions.
Functional coverage: UC-62–UC-65

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Full-width map region with current-location indicator, nearby verified facility markers, and a bottom sheet with selected facility.
2. Facility card includes name, care type, distance, ETA, opening/availability data when known, quick-call and navigate buttons.
3. Emergency CTA is visually distinct but not a diagnosis; show location-permission denied state and allow facility list fallback.
4. Family-alert CTA shows only recipients already authorized and a preview of minimal data/location shared.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 018 — `CB-018` — Expert Directory

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-80–UC-81, UC-164–UC-165

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-018`
Platform: Mother Mobile App
Primary role: Mother
Feature: Core experience
Reference screen name: “Expert Directory”
Purpose: Lists and filters verified experts by specialty, availability, location, and rating.
Functional coverage: UC-80–UC-81, UC-164–UC-165

Canvas and navigation: MOBILE. Use signed-in Mother bottom navigation: Trang chủ · Hành trình · Cộng đồng · Việc cần làm · Hồ sơ. Highlight only the relevant destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Search bar plus filters for specialty, method (chat/voice/video), availability, fee range, rating, and distance only with location consent.
2. Expert result cards include verified badge, specialty, short approved scope, available method, rating/review count, starting locked package price, and availability label.
3. Open-profile and select-expert actions; no direct booking before details, available slot, consent, and price snapshot are reviewed.
4. Empty state explains no matching expert and offers reset filters.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 019 — `CB-019` — Consultation Detail

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-75–UC-79, UC-202–UC-205, UC-208

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-019`
Platform: Mother Mobile App
Primary role: Mother
Feature: Core experience
Reference screen name: “Consultation Detail”
Purpose: Shows consultation booking information, status, shared data scope, payment, and session actions.
Functional coverage: UC-75–UC-79, UC-202–UC-205, UC-208

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Consultation header with status, date/time, channel, expert identity, and immutable booking price snapshot.
2. Consent card lists shared-data recipient, scope, purpose, expiry, and revoke/manage link; hide data outside the approved scope.
3. Action area changes by lifecycle: pay, join, reschedule, cancel, dispute/refund, or review. Every destructive/refund action opens a confirmation sheet.
4. Payment status and receipt reference, participant notes/summary shortcut after completion, and short platform-safety disclaimer.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 020 — `CB-020` — Realtime Consultation Session

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-77, UC-154

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-020`
Platform: Mother Mobile App
Primary role: Mother
Feature: Core experience
Reference screen name: “Realtime Consultation Session”
Purpose: Provides authenticated chat, voice call, or video call for an active consultation.
Functional coverage: UC-77, UC-154

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Authenticated session header with expert name, channel status, elapsed timer, connection indicator, and safe “Kết thúc” action.
2. Chat composer with attachment policy, call/video controls matching the booked channel, and reconnect state.
3. Visible shared-data scope indicator and “Xem tóm tắt được cấp quyền” action; no unrestricted health-record browsing.
4. End-session confirmation and a compact technical issue/retry state.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 021 — `CB-021` — Care Groups

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-70–UC-74, UC-216–UC-223

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-021`
Platform: Mother Mobile App
Primary role: Mother
Feature: Core experience
Reference screen name: “Care Groups”
Purpose: Lists and manages family care groups, members, permissions, invitations, and assigned tasks.
Functional coverage: UC-70–UC-74, UC-216–UC-223

Canvas and navigation: MOBILE. Use signed-in Mother bottom navigation: Trang chủ · Hành trình · Cộng đồng · Việc cần làm · Hồ sơ. Highlight only the relevant destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Care-group list with group name, members count, active permissions, pending invite count, and next shared task.
2. Create-group CTA; group card opens member, permission, invitation, shared calendar, and task management.
3. Member action includes invite, change/revoke permission, remove member, and assign task; every permission control exposes scope, purpose, and expiry.
4. No full health record appears on the list screen; empty state offers create group.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 022 — `CB-022` — Connected Devices

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-66–UC-69, UC-130

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-022`
Platform: Mother Mobile App
Primary role: Mother
Feature: Core experience
Reference screen name: “Connected Devices”
Purpose: Manages connected health platforms or devices and displays imported data status.
Functional coverage: UC-66–UC-69, UC-130

Canvas and navigation: MOBILE. Use signed-in Mother bottom navigation: Trang chủ · Hành trình · Cộng đồng · Việc cần làm · Hồ sơ. Highlight only the relevant destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Connected-platform/device cards with provider/device name, connection status, last sync, and source label.
2. Actions: connect, manual import, view trend, disconnect; disconnect requires confirmation and says historical imported records remain according to policy.
3. Manual import sheet includes metric type, observed time, value/unit, optional note, and explicit “Bạn nhập” source.
4. Device trend opens only data with source/precision notice; no medical conclusion.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 023 — `CB-023` — Safety Monitoring

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-133–UC-141, UC-176

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-023`
Platform: Mother Mobile App
Primary role: Mother
Feature: Core experience
Reference screen name: “Safety Monitoring”
Purpose: Controls IMU-based activity monitoring and shows safety status, detected events, and alerts.
Functional coverage: UC-133–UC-141, UC-176

Canvas and navigation: MOBILE. Use signed-in Mother bottom navigation: Trang chủ · Hành trình · Cộng đồng · Việc cần làm · Hồ sơ. Highlight only the relevant destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Safety monitoring status card with sensor permission, fall-detection toggle, emergency contact readiness, countdown duration, and location-sharing consent state.
2. History preview of suspected events with result, delivery status, and “Báo phát hiện nhầm” action.
3. Controls for configure contacts, enable/disable detection, and open emergency support; enable flow requires consent and shows device-sensor limitations.
4. If monitoring is inactive due to missing permission/contact, show exact blocking reason and direct repair action.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 024 — `CB-024` — Care Group Invitation

**Platform:** Family Member Mobile App  
**Primary role:** Family Member  
**Functional coverage:** UC-83

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-024`
Platform: Family Member Mobile App
Primary role: Family Member
Feature: Authentication & invitation
Reference screen name: “Care Group Invitation”
Purpose: Displays an invitation and allows the family member to accept or decline joining a care group.
Functional coverage: UC-83

Canvas and navigation: MOBILE. Use a simple top app bar with a clear back/close action. Do not render bottom navigation on this auth, setup, invite, or confirmation screen.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Invitation card showing inviter/group name, relationship context, permissions offered (not raw data), purpose, and expiry.
2. Primary “Chấp nhận” and secondary “Từ chối” buttons; acceptance only proceeds after consent summary is readable.
3. Expired/revoked invitation state with no join action; no health detail displayed before acceptance.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 025 — `CB-025` — Family Member Home

**Platform:** Family Member Mobile App  
**Primary role:** Family Member  
**Functional coverage:** UC-74, UC-85–UC-86

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-025`
Platform: Family Member Mobile App
Primary role: Family Member
Feature: Authentication & invitation
Reference screen name: “Family Member Home”
Purpose: Shows shared care groups, assigned tasks, calendar items, and family alerts.
Functional coverage: UC-74, UC-85–UC-86

Canvas and navigation: MOBILE. Use signed-in Family bottom navigation: Tổng quan · Nhóm chăm sóc · Lịch · Thông báo · Hồ sơ. Highlight only the relevant destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Natural greeting and summary cards for upcoming shared tasks, calendar items, and most recent permitted alert.
2. Care-group switcher and shortcut row for tasks, calendar, alerts, and shared data.
3. Each health-related preview uses minimum necessary information and an access-scope label; signed-in Family bottom navigation.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 026 — `CB-026` — My Care Groups

**Platform:** Family Member Mobile App  
**Primary role:** Family Member  
**Functional coverage:** UC-83–UC-84, UC-216, UC-220

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-026`
Platform: Family Member Mobile App
Primary role: Family Member
Feature: Shared care
Reference screen name: “My Care Groups”
Purpose: Lists the care groups that the family member has joined.
Functional coverage: UC-83–UC-84, UC-216, UC-220

Canvas and navigation: MOBILE. Use signed-in Family bottom navigation: Tổng quan · Nhóm chăm sóc · Lịch · Thông báo · Hồ sơ. Highlight only the relevant destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Care-group cards with group name, role, member count, permissions summary, and next action.
2. Actions to open details, leave group, and view pending invitations; leaving triggers a confirmation with loss-of-access implication.
3. Empty state contains an “Nhập lời mời” or “Chờ lời mời” status, not an unrelated create-group CTA.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 027 — `CB-027` — Shared Care Group Detail

**Platform:** Family Member Mobile App  
**Primary role:** Family Member  
**Functional coverage:** UC-74, UC-84–UC-85, UC-216, UC-219

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-027`
Platform: Family Member Mobile App
Primary role: Family Member
Feature: Shared care
Reference screen name: “Shared Care Group Detail”
Purpose: Shows the selected group, members, granted permissions, shared data, and tasks.
Functional coverage: UC-74, UC-84–UC-85, UC-216, UC-219

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this detail, invite, content, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Group header, member avatars/placeholders, role labels, and visible permission boundary for the current Family Member.
2. Tabs/cards for shared calendar, assigned tasks, permitted data, and alerts.
3. Member contact or task action only when permission permits; show scope/expiry and “Quyền đã hết hạn” state.
4. Leave-group action in overflow with confirmation.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 028 — `CB-028` — Shared Care Calendar

**Platform:** Family Member Mobile App  
**Primary role:** Family Member  
**Functional coverage:** UC-74

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-028`
Platform: Family Member Mobile App
Primary role: Family Member
Feature: Shared care
Reference screen name: “Shared Care Calendar”
Purpose: Displays appointments, reminders, and care tasks shared with the family member.
Functional coverage: UC-74

Canvas and navigation: MOBILE. Use signed-in Family bottom navigation: Tổng quan · Nhóm chăm sóc · Lịch · Thông báo · Hồ sơ. Highlight only the relevant destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Month/week calendar toggle and date navigator; items for appointments, reminders, and assigned tasks.
2. Filter by group and item type; each item opens detail with only permitted context.
3. Today button and empty-day state; no edit control for items the family member cannot manage.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 029 — `CB-029` — Assigned Tasks

**Platform:** Family Member Mobile App  
**Primary role:** Family Member  
**Functional coverage:** UC-85, UC-221–UC-223

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-029`
Platform: Family Member Mobile App
Primary role: Family Member
Feature: Shared care
Reference screen name: “Assigned Tasks”
Purpose: Lists assigned care tasks and allows the family member to update task status.
Functional coverage: UC-85, UC-221–UC-223

Canvas and navigation: MOBILE. Use signed-in Family bottom navigation: Tổng quan · Nhóm chăm sóc · Lịch · Thông báo · Hồ sơ. Highlight only the relevant destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Task list with status, due date, assignee, group, and priority; filter tabs for open/completed/overdue.
2. Each task supports open detail and allowed status update; update sheet includes complete, in progress, or blocked with optional short note.
3. No reassignment/deletion controls unless the current role has explicit permission.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 030 — `CB-030` — Shared Data

**Platform:** Family Member Mobile App  
**Primary role:** Family Member  
**Functional coverage:** UC-84

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-030`
Platform: Family Member Mobile App
Primary role: Family Member
Feature: Shared care
Reference screen name: “Shared Data”
Purpose: Displays only the maternal or baby data included in the family member’s active permission scope.
Functional coverage: UC-84

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this detail, invite, content, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Permission banner naming the data owner, allowed categories, purpose, and expiry.
2. Category cards/list for only authorized maternal/baby data with source label and date; unavailable categories are hidden rather than teased.
3. Filter by owner/category/date and explicit access-expired/denied state; no export or share-forward action.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 031 — `CB-031` — Family Alerts

**Platform:** Family Member Mobile App  
**Primary role:** Family Member  
**Functional coverage:** UC-86

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-031`
Platform: Family Member Mobile App
Primary role: Family Member
Feature: Shared care
Reference screen name: “Family Alerts”
Purpose: Lists safety, emergency, and important care alerts shared with the family member.
Functional coverage: UC-86

Canvas and navigation: MOBILE. Use signed-in Family bottom navigation: Tổng quan · Nhóm chăm sóc · Lịch · Thông báo · Hồ sơ. Highlight only the relevant destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Alert list with urgency text + icon, time, group/source, delivery/read status, and minimum permitted context.
2. Filter for safety, emergency, and care alerts; unread state is visible without relying on color alone.
3. Each row opens detail; no raw sensor or full health history on the list.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 032 — `CB-032` — Family Alert Detail

**Platform:** Family Member Mobile App  
**Primary role:** Family Member  
**Functional coverage:** UC-86, UC-161

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-032`
Platform: Family Member Mobile App
Primary role: Family Member
Feature: Shared care
Reference screen name: “Family Alert Detail”
Purpose: Shows the selected alert, permitted location or context, time, and response actions.
Functional coverage: UC-86, UC-161

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this detail, invite, content, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Alert headline, urgency label, received time, and permitted location/context (or explicit “Không chia sẻ vị trí”).
2. Action buttons for call/contact, open approved map route, acknowledge, or open emergency support when applicable.
3. Delivery/status timeline and short privacy note explaining only minimum necessary data is shown.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 033 — `CB-033` — Expert Profile Setup

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-87

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-033`
Platform: Expert App
Primary role: Verified Expert
Feature: Setup & mobile operations
Reference screen name: “Expert Profile Setup”
Purpose: Collects the expert’s specialty, experience, service scope, and public profile information.
Functional coverage: UC-87

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Editable professional profile form: public name, avatar placeholder, specialties, professional title, years of experience, organization, approved service scope, supported channels, and bio.
2. Location/service-area fields only where the expert opts into nearby support; privacy/visibility control is explicit.
3. Save draft and submit-for-verification CTAs; field validation for required specialty/scope/contact.
4. No claims that the profile has been verified before approval.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 034 — `CB-034` — Upload Verification Documents

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-89

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-034`
Platform: Expert App
Primary role: Verified Expert
Feature: Setup & mobile operations
Reference screen name: “Upload Verification Documents”
Purpose: Allows the expert to upload credentials and supporting documents for verification.
Functional coverage: UC-89

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Document-upload checklist with credential type, issuing organization, document number (masked/limited display), issue date, expiry date, and file picker.
2. Per-document status chips: missing, uploaded, needs update, under review, accepted, rejected; show accepted file only via protected viewer.
3. Upload validation for supported type/size and expiry; submit-for-review CTA.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 035 — `CB-035` — Verification Status

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-103, UC-173

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-035`
Platform: Expert App
Primary role: Verified Expert
Feature: Setup & mobile operations
Reference screen name: “Verification Status”
Purpose: Shows the expert verification result, missing information, rejection reason, or approval state.
Functional coverage: UC-103, UC-173

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Verification status headline with icon/label: under review, approved, needs correction, rejected, or expired.
2. Checklist of documents and profile sections with actionable missing/rejection reason.
3. Actions to edit profile, replace document, submit renewal, or contact support; do not expose internal reviewer identity.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 036 — `CB-036` — Expert App Home

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-112, UC-142–UC-143, UC-150

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-036`
Platform: Expert App
Primary role: Verified Expert
Feature: Setup & mobile operations
Reference screen name: “Expert App Home”
Purpose: Shows mobile consultation activity, question queue, availability, notifications, and nearby support requests.
Functional coverage: UC-112, UC-142–UC-143, UC-150

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use the floating Expert App bottom navigation: “Trang chủ”, “Yêu cầu”, “Cộng đồng”, “Tài khoản”; mark “Trang chủ” active.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Header with current availability status and quick status toggle; notification badge and profile shortcut.
2. Cards for today’s consultations, new requests, community question queue, expiring documents, and nearby support requests.
3. Each card has a single action-first CTA; no dense charts; bottom navigation for Expert App.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 037 — `CB-037` — Availability Status

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-142, UC-148

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-037`
Platform: Expert App
Primary role: Verified Expert
Feature: Setup & mobile operations
Reference screen name: “Availability Status”
Purpose: Allows the expert to set online status, support methods, location-sharing status, and availability duration.
Functional coverage: UC-142, UC-148

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Current availability switch with label Online/Offline, consultation methods, active duration, and auto-expiry time.
2. Location-sharing state with explicit consent/precision/expiry; never default location sharing to on.
3. Quick edit for start/end availability and an unavailable/conflict state.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 038 — `CB-038` — Consultation Requests

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-143

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-038`
Platform: Expert App
Primary role: Verified Expert
Feature: Setup & mobile operations
Reference screen name: “Consultation Requests”
Purpose: Lists new mobile consultation requests and provides quick accept or decline actions.
Functional coverage: UC-143

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use the floating Expert App bottom navigation: “Trang chủ”, “Yêu cầu”, “Cộng đồng”, “Tài khoản”; mark “Yêu cầu” active.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Filter tabs for new, expiring, accepted, declined; request cards show requester anonymized/limited identity, channel, requested slot, price snapshot, reason summary, and consent status.
2. Accept/decline action buttons with a decline-reason sheet; accepted action links to detail, not directly to unrestricted records.
3. Expired request state and count badge.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 039 — `CB-039` — Consultation Detail

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-94–UC-96, UC-143–UC-146

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-039`
Platform: Expert App
Primary role: Verified Expert
Feature: Setup & mobile operations
Reference screen name: “Consultation Detail”
Purpose: Shows the request, user consent scope, schedule, status, and actions to accept, reject, or join the consultation.
Functional coverage: UC-94–UC-96, UC-143–UC-146

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Consultation request header with lifecycle status, requested slot, channel, locked price, participant, and consent-status badge.
2. Consent summary names scope/purpose/expiry and health-summary link; no sensitive records unless consent is active.
3. Accept, decline, join, reschedule, no-show, complete, or contact actions shown only when status permits.
4. Brief policy note that consultation supports guidance and does not replace emergency care.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 040 — `CB-040` — Shared Health Summary

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-94

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-040`
Platform: Expert App
Primary role: Verified Expert
Feature: Setup & mobile operations
Reference screen name: “Shared Health Summary”
Purpose: Displays the consented health summary needed for the selected mobile consultation.
Functional coverage: UC-94

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Consent banner with owner, approved data scope, purpose, expiry, and last updated time.
2. Structured summary sections: current concern, key timeline, selected maternal/baby metrics, relevant records, and user-entered notes, each source-labeled.
3. Access-expired/denied state hides data and offers no workaround; no diagnosis or medication action.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 041 — `CB-041` — Realtime Consultation Session

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-144–UC-146, UC-154

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-041`
Platform: Expert App
Primary role: Verified Expert
Feature: Setup & mobile operations
Reference screen name: “Realtime Consultation Session”
Purpose: Provides authenticated chat, voice call, or video call for an active consultation.
Functional coverage: UC-144–UC-146, UC-154

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Session controls for approved chat, voice, or video channel; expert/participant names, timer, connection state, and end-session action.
2. Private chat and allowed attachment action; current consent scope indicator and shortcut to limited health summary.
3. Reconnect/failure state and confirmation before ending; no automatic recording controls unless project scope explicitly adds it.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 042 — `CB-042` — Expert Question Queue

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-91–UC-93

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-042`
Platform: Expert App
Primary role: Verified Expert
Feature: Setup & mobile operations
Reference screen name: “Expert Question Queue”
Purpose: Lists community questions matched to the expert’s verified specialties.
Functional coverage: UC-91–UC-93

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use the floating Expert App bottom navigation: “Trang chủ”, “Yêu cầu”, “Cộng đồng”, “Tài khoản”; mark “Cộng đồng” active.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Question list matched to verified specialty with filters for topic, urgency, unanswered, and status.
2. Question card shows anonymous/public display, moderation label, brief content, tags, and request-for-private-consultation indicator.
3. Answer composer supports concise response, safe wording reminder, and optional “Gợi ý tư vấn riêng” action; no diagnosis/prescription copy.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 043 — `CB-043` — Expert Location Sharing

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-147–UC-148

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-043`
Platform: Expert App
Primary role: Verified Expert
Feature: Setup & mobile operations
Reference screen name: “Expert Location Sharing”
Purpose: Allows the expert to start, pause, update, or stop controlled location sharing.
Functional coverage: UC-147–UC-148

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Location-sharing toggle with supported purpose, precision, duration/expiry, and current visible status.
2. Start, pause, update, and stop controls; map preview is approximate and not a live public tracker.
3. Explicit location-permission denied and expiry states; stop sharing confirmation.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 044 — `CB-044` — Nearby Support Requests

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-149–UC-150

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-044`
Platform: Expert App
Primary role: Verified Expert
Feature: Setup & mobile operations
Reference screen name: “Nearby Support Requests”
Purpose: Lists active nearby support requests that match the expert’s specialty and availability.
Functional coverage: UC-149–UC-150

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use the floating Expert App bottom navigation: “Trang chủ”, “Yêu cầu”, “Cộng đồng”, “Tài khoản”; mark “Yêu cầu” active; nearby support is a scoped tab inside Requests.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. List of eligible nearby support requests with urgency label, specialty match, distance/ETA, method, and consent-state badge.
2. Filters for distance, urgency, status, and specialty; accept action opens detail.
3. No exact user location or direct contact before acceptance and consent validation.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 045 — `CB-045` — Nearby Requests Map

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-149–UC-150, UC-155

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-045`
Platform: Expert App
Primary role: Verified Expert
Feature: Setup & mobile operations
Reference screen name: “Nearby Requests Map”
Purpose: Displays eligible nearby support requests on the shared MF-19 map.
Functional coverage: UC-149–UC-150, UC-155

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Map with eligible request markers, current/expert location indicator only if sharing is on, and a bottom sheet summary.
2. Filter chips for urgency/specialty and list-map switcher.
3. Marker detail contains only minimal request information, distance, ETA, and “Xem yêu cầu”; location/permission-denied fallback list.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 046 — `CB-046` — Nearby Support Request Detail

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-150–UC-151

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-046`
Platform: Expert App
Primary role: Verified Expert
Feature: Setup & mobile operations
Reference screen name: “Nearby Support Request Detail”
Purpose: Shows the request summary, distance, consent scope, urgency label, and response options.
Functional coverage: UC-150–UC-151

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Request header with urgency, distance/ETA, specialty fit, short user-provided summary, and time received.
2. Consent scope card states what can be accessed and until when; hide unrelated records.
3. Accept/decline actions, permitted contact action after acceptance, and route action after location consent.
4. No claim that the expert is an emergency responder.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 047 — `CB-047` — Contact Nearby User

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-151

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-047`
Platform: Expert App
Primary role: Verified Expert
Feature: Setup & mobile operations
Reference screen name: “Contact Nearby User”
Purpose: Provides approved chat or call actions after the expert accepts a nearby support request.
Functional coverage: UC-151

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Approved contact card with selected channel (chat/call), recipient display name/privacy-safe identity, connection status, and safe end action.
2. Short context/consent banner; no medical record dump.
3. Unavailable/offline and permission-revoked states.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 048 — `CB-048` — Route to Nearby User

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-152, UC-129

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-048`
Platform: Expert App
Primary role: Verified Expert
Feature: Setup & mobile operations
Reference screen name: “Route to Nearby User”
Purpose: Displays the route and ETA to the consented user location using the map service.
Functional coverage: UC-152, UC-129

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Route map with start/end markers, route line, distance, ETA, and route alternatives if available.
2. Open-navigation CTA, refresh-route action, and a permission-denied fallback with no location leak.
3. Keep the user’s exact location only if consent remains active; visible expiry label.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 049 — `CB-049` — Notification Center

**Platform:** Shared Mobile Apps  
**Primary role:** Current signed-in mobile user  
**Functional coverage:** UC-11–UC-12, UC-158–UC-161

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-049`
Platform: Shared Mobile Apps
Primary role: Current signed-in mobile user
Feature: Account & privacy
Reference screen name: “Notification Center”
Purpose: Lists alerts and account notifications across mobile roles, filtered by the user's permissions and care-group context.
Functional coverage: UC-11–UC-12, UC-158–UC-161

Canvas and navigation: MOBILE. Use the current role’s signed-in mobile navigation only when it is already part of the enclosing app. Do not invent a separate navigation scheme.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Notification list with icon/type, short message, time, unread marker, and deep-link target label.
2. Filters for all/unread and category; mark-all-read action and per-item read state.
3. List includes only authorized care-group context; no sensitive data in push-preview text.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 050 — `CB-050` — Privacy Settings

**Platform:** Shared Mobile Apps  
**Primary role:** Current signed-in mobile user  
**Functional coverage:** UC-17–UC-19, UC-157

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-050`
Platform: Shared Mobile Apps
Primary role: Current signed-in mobile user
Feature: Account & privacy
Reference screen name: “Privacy Settings”
Purpose: Lets mobile users manage consent, data sharing, notification privacy, and account visibility settings.
Functional coverage: UC-17–UC-19, UC-157

Canvas and navigation: MOBILE. Use the current role’s signed-in mobile navigation only when it is already part of the enclosing app. Do not invent a separate navigation scheme.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Privacy sections for data sharing/consent, community visibility/anonymous display, notification privacy, and account visibility.
2. Consent list shows recipient, scope, purpose, expiry, status, and revoke action; new-grant action uses an in-frame sheet.
3. Toggle changes include clear impact text; no defaults that expose data.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 051 — `CB-051` — Delete Account Confirmation

**Platform:** Shared Mobile Apps  
**Primary role:** Current signed-in mobile user  
**Functional coverage:** UC-156

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-051`
Platform: Shared Mobile Apps
Primary role: Current signed-in mobile user
Feature: Account & privacy
Reference screen name: “Delete Account Confirmation”
Purpose: Confirms permanent account deletion and explains role-specific data retention or care-group impact.
Functional coverage: UC-156

Canvas and navigation: MOBILE. Use a simple top app bar with a clear back/close action. Do not render bottom navigation on this auth, setup, invite, or confirmation screen.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Destructive confirmation sheet with current role, deletion impact, retained-data note, and optional reason.
2. Explicit acknowledgement checkbox/text entry, Cancel and “Xóa tài khoản” CTA; CTA disabled until acknowledgement.
3. No fake instant success; show processing state and safe sign-out outcome.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 052 — `CB-052` — Deactivate Account Confirmation

**Platform:** Shared Mobile Apps  
**Primary role:** Current signed-in mobile user  
**Functional coverage:** UC-15

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-052`
Platform: Shared Mobile Apps
Primary role: Current signed-in mobile user
Feature: Account & privacy
Reference screen name: “Deactivate Account Confirmation”
Purpose: Confirms temporary account deactivation where supported and explains access limitations while inactive.
Functional coverage: UC-15

Canvas and navigation: MOBILE. Use a simple top app bar with a clear back/close action. Do not render bottom navigation on this auth, setup, invite, or confirmation screen.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Confirmation sheet with temporary access limitations, linked care-group impact, reactivation rule/timing, optional reason.
2. Cancel and “Tạm ngưng tài khoản” CTA; primary action styled destructive but still accessible.
3. Processing and success/sign-out state.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 053 — `CB-053` — Web Login

**Platform:** Shared Web Portals  
**Primary role:** Guest  
**Functional coverage:** UC-03

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-053`
Platform: Shared Web Portals
Primary role: Guest
Feature: Authentication & account access
Reference screen name: “Web Login”
Purpose: Authenticates portal users and routes them to the correct role dashboard.
Functional coverage: UC-03

Canvas and navigation: WEB. Public/auth web layout: no signed-in sidebar until authentication/verification is completed.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Centered white login panel inside an uncluttered web canvas; CareBridge logo/wordmark, email/phone field, password field, show/hide control.
2. Login CTA, forgot-password link, contact/support link, and generic failed-login validation.
3. No public role selector; portal routes based on authenticated role.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 054 — `CB-054` — Expert Portal Dashboard

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-112

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-054`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Professional operations
Reference screen name: “Expert Portal Dashboard”
Purpose: Summarizes consultations, availability, questions, documents, revenue, and contribution activity.
Functional coverage: UC-112

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Persistent Expert sidebar and compact top bar with notification/profile menu.
2. Action-first dashboard cards: today’s sessions, pending requests, availability state, question queue, document expiry, revenue snapshot, contribution points.
3. Today calendar preview and only a small trend visual; every card has a direct detail CTA.
4. Use white-primary B2B layout with generous whitespace, not a cute consumer dashboard.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 055 — `CB-055` — Expert Professional Profile

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-87–UC-88

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-055`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Professional operations
Reference screen name: “Expert Professional Profile”
Purpose: Displays and edits the expert’s professional profile and approved service scope.
Functional coverage: UC-87–UC-88

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Professional profile editor with identity/public details, specialties, organization, experience, approved scope, consultation channels, bio, and visibility.
2. Profile completeness/verification badge and read-only fields where approval is required.
3. Save changes and submit-for-review actions; inline validation and unsaved-changes warning.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 056 — `CB-056` — Verification Documents

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-89, UC-173

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-056`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Professional operations
Reference screen name: “Verification Documents”
Purpose: Lists submitted credentials, verification status, expiry dates, and document update actions.
Functional coverage: UC-89, UC-173

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Document table: credential type, issuing organization, expiry, verification status, upload/update time, and protected view action.
2. Upload/update drawer fields include document type, expiry, file selection, and optional note.
3. Filters for status/expiry; renewal CTA for expiring or expired items; no raw document public links.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 057 — `CB-057` — Availability Calendar

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-90

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-057`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Professional operations
Reference screen name: “Availability Calendar”
Purpose: Manages recurring availability, consultation slots, exceptions, and support methods.
Functional coverage: UC-90

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Week calendar with recurring availability slots, booked slots, blocked dates, and timezone.
2. Controls to add/edit recurring slot, add exception, choose consultation methods, and publish changes.
3. Conflict/overlap validation and a visible effective date; no booking price edits here.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 058 — `CB-058` — Consultation Requests

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-143

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-058`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Professional operations
Reference screen name: “Consultation Requests”
Purpose: Lists new consultation requests that the expert can review, accept, or decline.
Functional coverage: UC-143

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Filterable table/list for new consultation requests: received time, requested slot/channel, user privacy-safe identity, consent status, price snapshot, expiry.
2. Row actions review, accept, or decline; decline opens reason dialog.
3. Bulk actions only for harmless filtering/read state, never bulk accept.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 059 — `CB-059` — Consultation List

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-202

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-059`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Professional operations
Reference screen name: “Consultation List”
Purpose: Lists scheduled, active, completed, cancelled, and disputed consultations.
Functional coverage: UC-202

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Consultation table with status tabs scheduled/active/completed/cancelled/disputed, date range, channel, participant, amount, and session state.
2. Search and filters; each row opens detail. No inline destructive lifecycle action without confirmation.
3. Empty state for filters and clear status labels.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 060 — `CB-060` — Consultation Detail

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-94–UC-97, UC-203–UC-208

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-060`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Professional operations
Reference screen name: “Consultation Detail”
Purpose: Shows the consultation schedule, participant, consented records, payment state, session access, and summary actions.
Functional coverage: UC-94–UC-97, UC-203–UC-208

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Detail header with schedule, participant privacy-safe identity, status, channel, locked price, payment state, and session shortcut.
2. Consent panel with scope/purpose/expiry and link to shared summary; summary/action area supports join, reschedule, no-show, complete, dispute/review as lifecycle permits.
3. Consultation notes/summary section, payment/commission snapshot, and audit-friendly timestamps; no medical diagnosis text.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 061 — `CB-061` — Shared Health Summary

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-94

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-061`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Professional operations
Reference screen name: “Shared Health Summary”
Purpose: Displays consented maternal or baby health summaries and records for the selected consultation.
Functional coverage: UC-94

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Consent header (owner/scope/purpose/expiry), selected health summary blocks, source labels, and protected attachments list.
2. No edit or export action for the expert; access-expired screen with clear reason and return link.
3. Information is concise and consultation-relevant only.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 062 — `CB-062` — Web Consultation Session

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-144–UC-146, UC-154

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-062`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Professional operations
Reference screen name: “Web Consultation Session”
Purpose: Provides the web interface for authenticated chat, voice, or video consultation.
Functional coverage: UC-144–UC-146, UC-154

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Wide main session workspace with participant tiles or call state, chat panel, connection indicator, elapsed timer, and end-session control.
2. Side panel for approved health summary and session details; consent badge remains visible.
3. Reconnect, mic/camera permission, and ended-session states; no generic video marketplace controls.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 063 — `CB-063` — Expert Question Queue

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-91–UC-93

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-063`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Professional operations
Reference screen name: “Expert Question Queue”
Purpose: Lists matched community questions and supports detailed expert response drafting.
Functional coverage: UC-91–UC-93

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Question queue table/list with keyword, topic, expert-label, answer status, moderation status, and date filters.
2. Detail pane shows question context and response composer; include safe-writing reminder and optional consult suggestion.
3. Answer save draft/publish states; no ability to bypass moderation lock.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 064 — `CB-064` — Revenue and Commission

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-97, UC-126–UC-127

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-064`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Professional operations
Reference screen name: “Revenue and Commission”
Purpose: Shows completed paid consultations, gross revenue, platform commission, and settlement status.
Functional coverage: UC-97, UC-126–UC-127

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Revenue summary cards: gross completed consultations, commission, estimated payable, settlement status, and period selector.
2. Detailed table with consultation ID, locked amount, commission rate, fees, net amount, settlement batch/status, and dates.
3. Filters/export action where allowed; show that figures are operational, not editable payout instructions.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 065 — `CB-065` — Contribution Points

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-98

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-065`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Professional operations
Reference screen name: “Contribution Points”
Purpose: Shows contribution points, badges, and qualifying community activities.
Functional coverage: UC-98

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Contribution summary with current points, badges, qualifying activities, and next milestone.
2. Activity history table/list with action type, earned point, date, and policy label.
3. No gamified pressure copy; helpful explanation stays brief.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 066 — `CB-066` — Moderator Dashboard

**Platform:** Admin Web Portal  
**Primary role:** Moderator  
**Functional coverage:** UC-111

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-066`
Platform: Admin Web Portal
Primary role: Moderator
Feature: Moderation
Reference screen name: “Moderator Dashboard”
Purpose: Summarizes pending reports, moderation queues, violations, and escalated safety cases.
Functional coverage: UC-111

Canvas and navigation: WEB. Use Moderator sidebar: Tổng quan · Hàng đợi · Báo cáo · Vi phạm · Ca an toàn.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Moderator dashboard with KPIs for pending reports, aging queue, confirmed violations, escalated safety cases, and recent actions.
2. Priority queue preview with urgency/SLA label and direct review CTA.
3. Short chart/table only where it improves scanning; avoid exposing full health data.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 067 — `CB-067` — Moderation Queue

**Platform:** Admin Web Portal  
**Primary role:** Moderator  
**Functional coverage:** UC-99

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-067`
Platform: Admin Web Portal
Primary role: Moderator
Feature: Moderation
Reference screen name: “Moderation Queue”
Purpose: Lists reported or automatically flagged content awaiting moderator review.
Functional coverage: UC-99

Canvas and navigation: WEB. Use Moderator sidebar: Tổng quan · Hàng đợi · Báo cáo · Vi phạm · Ca an toàn.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Queue table with report/flag ID, content/account target, reason/source, risk label, report count, received time, assignee, and status.
2. Filters for queue status, type, risk, source, date; search; row opens moderation detail.
3. Bulk assignment allowed, but enforcement actions only in item detail.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 068 — `CB-068` — Moderation Item Detail

**Platform:** Admin Web Portal  
**Primary role:** Moderator  
**Functional coverage:** UC-100

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-068`
Platform: Admin Web Portal
Primary role: Moderator
Feature: Moderation
Reference screen name: “Moderation Item Detail”
Purpose: Shows the flagged content, context, evidence, history, and available moderation actions.
Functional coverage: UC-100

Canvas and navigation: WEB. Use Moderator sidebar: Tổng quan · Hàng đợi · Báo cáo · Vi phạm · Ca an toàn.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Two-column review workspace: flagged content/context/evidence on left; history, policy match, and action panel on right.
2. Actions include keep, hide/remove, warn, restrict, escalate, request more information, each requiring reason and confirmation when impactful.
3. Case timeline with actor/time; avoid showing irrelevant private health data.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 069 — `CB-069` — Content Report Detail

**Platform:** Admin Web Portal  
**Primary role:** Moderator  
**Functional coverage:** UC-101

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-069`
Platform: Admin Web Portal
Primary role: Moderator
Feature: Moderation
Reference screen name: “Content Report Detail”
Purpose: Shows a report against a post or answer and supports resolution actions.
Functional coverage: UC-101

Canvas and navigation: WEB. Use Moderator sidebar: Tổng quan · Hàng đợi · Báo cáo · Vi phạm · Ca an toàn.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Content-report detail with reported post/answer preview, report reason, reporter anonymity, evidence, prior reports, and moderation history.
2. Resolution controls with rationale: dismiss, hide, remove, escalate; confirmation for irreversible enforcement.
3. Link to related content/account but no unrelated profile disclosure.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 070 — `CB-070` — Account Report Detail

**Platform:** Admin Web Portal  
**Primary role:** Moderator  
**Functional coverage:** UC-101–UC-102

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-070`
Platform: Admin Web Portal
Primary role: Moderator
Feature: Moderation
Reference screen name: “Account Report Detail”
Purpose: Shows a report against an account, related evidence, and enforcement options.
Functional coverage: UC-101–UC-102

Canvas and navigation: WEB. Use Moderator sidebar: Tổng quan · Hàng đợi · Báo cáo · Vi phạm · Ca an toàn.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Account-report detail with privacy-safe account summary, report reason, evidence, prior violation count, and current restrictions.
2. Enforcement actions: dismiss, warning, temporary restriction, suspend/escalate; require reason, duration when relevant, and confirmation.
3. History/audit timeline and appeal/review state if in scope.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 071 — `CB-071` — Violation History

**Platform:** Admin Web Portal  
**Primary role:** Moderator  
**Functional coverage:** UC-102

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-071`
Platform: Admin Web Portal
Primary role: Moderator
Feature: Moderation
Reference screen name: “Violation History”
Purpose: Displays prior warnings, restrictions, suspensions, and resolved violations for an account.
Functional coverage: UC-102

Canvas and navigation: WEB. Use Moderator sidebar: Tổng quan · Hàng đợi · Báo cáo · Vi phạm · Ca an toàn.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Chronological violation history: event type, rule, action, duration, moderator, status, and related case.
2. Filters by date/action/status; open case/detail action; no direct enforcement from history without confirmation.
3. Empty state says no recorded violations.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 072 — `CB-072` — Escalated Safety Case

**Platform:** Admin Web Portal  
**Primary role:** Moderator  
**Functional coverage:** UC-99–UC-101

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-072`
Platform: Admin Web Portal
Primary role: Moderator
Feature: Moderation
Reference screen name: “Escalated Safety Case”
Purpose: Shows a high-risk content or safety case requiring urgent review and escalation.
Functional coverage: UC-99–UC-101

Canvas and navigation: WEB. Use Moderator sidebar: Tổng quan · Hàng đợi · Báo cáo · Vi phạm · Ca an toàn.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. High-risk safety case header with urgency label, received source, summary, and escalation timer/SLA.
2. Protected evidence/context panel, linked reports, case timeline, and escalation destination/status.
3. Actions must prioritize safe routing/escalation; do not place payment or generic chat CTA before emergency action.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 073 — `CB-073` — Content Admin Dashboard

**Platform:** Admin Web Portal  
**Primary role:** Content Admin  
**Functional coverage:** UC-105–UC-108

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-073`
Platform: Admin Web Portal
Primary role: Content Admin
Feature: Content management
Reference screen name: “Content Admin Dashboard”
Purpose: Summarizes drafts, pending updates, published content, FAQs, checklists, and categories.
Functional coverage: UC-105–UC-108

Canvas and navigation: WEB. Use Content Admin sidebar: Tổng quan · Thư viện · FAQ & Checklist · Bài tập · Danh mục · Phê duyệt.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Content-admin dashboard cards for drafts, submitted-for-approval, published, update requests, FAQ count, checklist count, and category issues.
2. Recent content table and direct actions to create content, review revisions, and manage topics.
3. Operational content view only; no dense consumer-style hero.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 074 — `CB-074` — Content List

**Platform:** Admin Web Portal  
**Primary role:** Content Admin  
**Functional coverage:** UC-105–UC-108

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-074`
Platform: Admin Web Portal
Primary role: Content Admin
Feature: Content management
Reference screen name: “Content List”
Purpose: Lists articles, FAQs, and checklists with filters for type, status, topic, and version.
Functional coverage: UC-105–UC-108

Canvas and navigation: WEB. Use Content Admin sidebar: Tổng quan · Thư viện · FAQ & Checklist · Bài tập · Danh mục · Phê duyệt.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Content table with type, title, topic, stage/age target, status, version, last editor, last update, and source-label columns.
2. Filters for type/status/topic/version plus search; create CTA; row actions view/edit/preview/version history based on authorization.
3. Empty state and no public medical claims in list excerpts.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 075 — `CB-075` — Content Detail

**Platform:** Admin Web Portal  
**Primary role:** Content Admin  
**Functional coverage:** UC-105–UC-108

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-075`
Platform: Admin Web Portal
Primary role: Content Admin
Feature: Content management
Reference screen name: “Content Detail”
Purpose: Shows the full content, metadata, source labels, publication status, and version information.
Functional coverage: UC-105–UC-108

Canvas and navigation: WEB. Use Content Admin sidebar: Tổng quan · Thư viện · FAQ & Checklist · Bài tập · Danh mục · Phê duyệt.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Content detail layout with title, type, audience/stage tags, publication status, source labels, version metadata, and last editor.
2. Readable content preview with concise sections; right action rail for edit, preview, submit/review, or archive based on status.
3. History link and moderation/approval state; no uncontrolled publish action.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 076 — `CB-076` — Create Content

**Platform:** Admin Web Portal  
**Primary role:** Content Admin  
**Functional coverage:** UC-105

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-076`
Platform: Admin Web Portal
Primary role: Content Admin
Feature: Content management
Reference screen name: “Create Content”
Purpose: Provides the form for creating a new article, FAQ, or checklist.
Functional coverage: UC-105

Canvas and navigation: WEB. Use Content Admin sidebar: Tổng quan · Thư viện · FAQ & Checklist · Bài tập · Danh mục · Phê duyệt.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Creation form fields: content type, title, short summary, body/sections, audience stage/child age, topic/category, tags, source references, and status.
2. Actions save draft, preview, submit for approval; no direct publish bypass unless role permits.
3. Validation for title/type/source/status and unsaved changes prompt.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 077 — `CB-077` — Edit Content

**Platform:** Admin Web Portal  
**Primary role:** Content Admin  
**Functional coverage:** UC-106

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-077`
Platform: Admin Web Portal
Primary role: Content Admin
Feature: Content management
Reference screen name: “Edit Content”
Purpose: Allows authorized fields, metadata, sources, tags, and status of existing content to be updated.
Functional coverage: UC-106

Canvas and navigation: WEB. Use Content Admin sidebar: Tổng quan · Thư viện · FAQ & Checklist · Bài tập · Danh mục · Phê duyệt.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Prefilled authorized content fields: title, summary, body, audience, tags, sources, status, and change-summary text area.
2. Version-aware save/submit controls; show current version and locked fields clearly.
3. Inline validation plus preview action; preserve audit/change history.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 078 — `CB-078` — Content Preview

**Platform:** Admin Web Portal  
**Primary role:** Content Admin  
**Functional coverage:** UC-105–UC-106

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-078`
Platform: Admin Web Portal
Primary role: Content Admin
Feature: Content management
Reference screen name: “Content Preview”
Purpose: Shows how content will appear to end users before submission or publication.
Functional coverage: UC-105–UC-106

Canvas and navigation: WEB. Use Content Admin sidebar: Tổng quan · Thư viện · FAQ & Checklist · Bài tập · Danh mục · Phê duyệt.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. End-user-style preview inside the portal with responsive mobile/desktop toggle.
2. Visible content metadata, source label, safety/next-action block where content is health-related, and back-to-edit action.
3. No editing widgets or long explanatory admin panels in preview.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 079 — `CB-079` — Content Version History

**Platform:** Admin Web Portal  
**Primary role:** Content Admin  
**Functional coverage:** UC-106, UC-108

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-079`
Platform: Admin Web Portal
Primary role: Content Admin
Feature: Content management
Reference screen name: “Content Version History”
Purpose: Lists previous content versions, editors, timestamps, and change summaries.
Functional coverage: UC-106, UC-108

Canvas and navigation: WEB. Use Content Admin sidebar: Tổng quan · Thư viện · FAQ & Checklist · Bài tập · Danh mục · Phê duyệt.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Version history table with version number, status, editor, timestamp, change summary, and compare/view actions.
2. Current version marker and read-only older-version preview; no silent overwrite.
3. Filters by status/editor/date if needed.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 080 — `CB-080` — FAQ List

**Platform:** Admin Web Portal  
**Primary role:** Content Admin  
**Functional coverage:** UC-105–UC-107

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-080`
Platform: Admin Web Portal
Primary role: Content Admin
Feature: Content management
Reference screen name: “FAQ List”
Purpose: Lists and manages verified frequently asked questions and answers.
Functional coverage: UC-105–UC-107

Canvas and navigation: WEB. Use Content Admin sidebar: Tổng quan · Thư viện · FAQ & Checklist · Bài tập · Danh mục · Phê duyệt.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. FAQ table/list with question, answer preview, topic, source, status, version, and last updated.
2. Search/filter and create/edit actions; verified-source label.
3. No long full answer in table; open detail/edit for content.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 081 — `CB-081` — Checklist List

**Platform:** Admin Web Portal  
**Primary role:** Content Admin  
**Functional coverage:** UC-105–UC-107

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-081`
Platform: Admin Web Portal
Primary role: Content Admin
Feature: Content management
Reference screen name: “Checklist List”
Purpose: Lists and manages verified preparation and care checklists.
Functional coverage: UC-105–UC-107

Canvas and navigation: WEB. Use Content Admin sidebar: Tổng quan · Thư viện · FAQ & Checklist · Bài tập · Danh mục · Phê duyệt.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Checklist table/list with title, target stage, item count, status, version, and last updated.
2. Filters/search, create/edit/preview actions; reorder is available only in edit form.
3. Keep checklist content action-oriented, not a medical prescription.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 082 — `CB-082` — Topic and Category Management

**Platform:** Admin Web Portal  
**Primary role:** Content Admin  
**Functional coverage:** UC-109, UC-226

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-082`
Platform: Admin Web Portal
Primary role: Content Admin
Feature: Content management
Reference screen name: “Topic and Category Management”
Purpose: Creates and maintains content topics, categories, tags, and display order.
Functional coverage: UC-109, UC-226

Canvas and navigation: WEB. Use Content Admin sidebar: Tổng quan · Thư viện · FAQ & Checklist · Bài tập · Danh mục · Phê duyệt.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Management table for topics/categories/tags with name, parent, type, display order, active status, content count, and last update.
2. Create/edit drawer: name, slug/identifier, parent, description, display order, active toggle; no destructive action without confirmation.
3. Drag/reorder control or explicit order input, with validation for duplicate name/order.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 083 — `CB-083` — Admin Dashboard

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-103, UC-114, UC-123, UC-126–UC-127

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-083`
Platform: Admin Web Portal
Primary role: System Admin
Feature: System administration
Reference screen name: “Admin Dashboard”
Purpose: Summarizes users, expert and partner verification, operations, disputes, safety, and payments.
Functional coverage: UC-103, UC-114, UC-123, UC-126–UC-127

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Executive admin dashboard KPI cards for users, expert/partner verification, moderation risk, consultations, disputes, payment/commission, and safety events.
2. Quick links to highest-risk queues plus high-level impact/operations snapshot; avoid individual health data.
3. Date range and region/role filters only where supported.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 084 — `CB-084` — User List

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-114

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-084`
Platform: Admin Web Portal
Primary role: System Admin
Feature: System administration
Reference screen name: “User List”
Purpose: Lists system accounts with filters for role, status, verification, and risk indicators.
Functional coverage: UC-114

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. User table with account name/contact masked appropriately, roles, account status, verification, risk indicator, join date, and last active.
2. Filters role/status/verification/risk/date and search; row opens user detail.
3. Bulk actions restricted to low-risk operational tasks; no bulk suspend.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 085 — `CB-085` — User Detail

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-114–UC-117

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-085`
Platform: Admin Web Portal
Primary role: System Admin
Feature: System administration
Reference screen name: “User Detail”
Purpose: Shows account information, roles, status, sessions, reports, and permitted administration actions.
Functional coverage: UC-114–UC-117

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. User detail header with role/status/verification and privacy-safe identity/contact.
2. Tabs/sections for roles/permissions, active sessions, reports/violations, account history, and permitted admin actions.
3. Actions for role/permission change, deactivate/reactivate, security response, each with confirmation and audit reason.
4. Do not show health records unless a separate authorized workflow allows it.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 086 — `CB-086` — Expert Verification Queue

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-103

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-086`
Platform: Admin Web Portal
Primary role: System Admin
Feature: System administration
Reference screen name: “Expert Verification Queue”
Purpose: Lists expert applications and credential updates awaiting administrative review.
Functional coverage: UC-103

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Expert application queue table with applicant, specialties, document completeness, verification status, submitted/updated date, expiry risk, and reviewer.
2. Filters/search; row opens application review; decisions approve/reject/request correction require reason.
3. No direct public profile publication before approval.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 087 — `CB-087` — Content Approval Queue

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-108

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-087`
Platform: Admin Web Portal
Primary role: System Admin
Feature: System administration
Reference screen name: “Content Approval Queue”
Purpose: Lists content versions awaiting approval, rejection, or a request for revision.
Functional coverage: UC-108

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Content approval queue with title/type/version, author, source completeness, status, submitted date, and reviewer.
2. Row detail preview and actions approve, reject, request revision with mandatory rationale.
3. Filters by type/status/topic; audit timestamps visible.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 088 — `CB-088` — Escalated Moderation Cases

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-99–UC-101

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-088`
Platform: Admin Web Portal
Primary role: System Admin
Feature: System administration
Reference screen name: “Escalated Moderation Cases”
Purpose: Lists moderation cases escalated to the system administrator for final action.
Functional coverage: UC-99–UC-101

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Escalated moderation case table with severity, type, owner, deadline, source queue, and current status.
2. Filters/search and row detail action; final actions require reason, confirmation, and audit note.
3. No sensitive raw health content in list view.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 089 — `CB-089` — Partner Verification Queue

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-123

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-089`
Platform: Admin Web Portal
Primary role: System Admin
Feature: System administration
Reference screen name: “Partner Verification Queue”
Purpose: Lists partner applications and submitted evidence awaiting verification.
Functional coverage: UC-123

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Partner verification queue table with organization, representative, facility/service type, evidence completeness, submitted date, status, and reviewer.
2. Open review, approve, reject, request correction actions with reason.
3. Partner content/services remain unpublished until approved.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 090 — `CB-090` — Operations and Impact Dashboard

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-113

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-090`
Platform: Admin Web Portal
Primary role: System Admin
Feature: System administration
Reference screen name: “Operations and Impact Dashboard”
Purpose: Displays operational KPIs and aggregated social-impact metrics without exposing personal health data.
Functional coverage: UC-113

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Operations and impact dashboard with aggregated anonymized KPIs: engagement, reminders completed, verified content usage, expert contribution, safely routed alerts, and consultation operations.
2. Clear data-aggregation/privacy label; period filter; compact trends.
3. No user-level health data or data-selling visualization.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 091 — `CB-091` — Safety Rule Management

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-110

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-091`
Platform: Admin Web Portal
Primary role: System Admin
Feature: System administration
Reference screen name: “Safety Rule Management”
Purpose: Manages controlled safety, triage, escalation, and abuse-prevention rules.
Functional coverage: UC-110

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Safety-rule management table with rule name/type, trigger category, severity/action, status, version, effective dates, and last editor.
2. Create/edit panel includes rule condition, safe next action, escalation route, active status, version/effective period, and test preview.
3. No free-form AI medical diagnosis rule; changes require reason and versioning.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 092 — `CB-092` — System Configuration

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-110

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-092`
Platform: Admin Web Portal
Primary role: System Admin
Feature: System administration
Reference screen name: “System Configuration”
Purpose: Manages system-wide settings, limits, reference data, and integration configuration.
Functional coverage: UC-110

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. System configuration sections for reference data, limits, feature toggles, integration status, and notification defaults.
2. Each setting displays value, scope, last editor, and updated time; edit uses controlled form and confirmation for high-impact changes.
3. No secrets/API keys rendered in plaintext; no diagnostic configuration as user-facing content.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 093 — `CB-093` — Consultation Disputes

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-209

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-093`
Platform: Admin Web Portal
Primary role: System Admin
Feature: System administration
Reference screen name: “Consultation Disputes”
Purpose: Lists consultation complaints and supports investigation, resolution, and refund decisions.
Functional coverage: UC-209

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Dispute table with consultation ID, participant privacy-safe names, category, status, amount, opened date, assignee, and refund state.
2. Filters/search; row opens investigation/resolution; lifecycle/status labels are explicit.
3. No direct refund approval from table without detailed review.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 094 — `CB-094` — Payment and Commission Management

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-126–UC-127

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-094`
Platform: Admin Web Portal
Primary role: System Admin
Feature: System administration
Reference screen name: “Payment and Commission Management”
Purpose: Manages payment records, platform commission, refunds, and settlement status.
Functional coverage: UC-126–UC-127

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Payment/commission table with transaction/booking reference, locked amount, status, payment method, commission rate, fees, refund/settlement state, and timestamps.
2. Filters date/status/partner/expert; row opens reconciliation or dispute context.
3. Export is operational only; no editing of immutable price snapshots.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 095 — `CB-095` — Audit Log

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-117

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-095`
Platform: Admin Web Portal
Primary role: System Admin
Feature: System administration
Reference screen name: “Audit Log”
Purpose: Displays immutable records of sensitive access, permission, moderation, and administration events.
Functional coverage: UC-117

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Immutable audit-log table with timestamp, actor, role, action, target, result, source/IP/device summary, and correlation/case reference.
2. Filters actor/action/target/date/result; protected detail drawer includes before/after summary where authorized.
3. No edit/delete action; privacy-safe masking for identifiers.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 096 — `CB-096` — Partner Portal Landing

**Platform:** Partner Web Portal  
**Primary role:** Guest or unverified Partner Representative  
**Functional coverage:** UC-118

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-096`
Platform: Partner Web Portal
Primary role: Guest or unverified Partner Representative
Feature: Partner operations
Reference screen name: “Partner Portal Landing”
Purpose: Introduces the partner portal and provides registration or login entry points.
Functional coverage: UC-118

Canvas and navigation: WEB. Public/auth web layout: no signed-in sidebar until authentication/verification is completed.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Clean public portal landing without signed-in sidebar: CareBridge Partner wordmark, one-line purpose, and clear “Đăng ký đối tác” / “Đăng nhập” CTAs.
2. Short eligibility/status link and governance note; no marketing carousel or long pitch.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 097 — `CB-097` — Register Partner Account

**Platform:** Partner Web Portal  
**Primary role:** Guest or unverified Partner Representative  
**Functional coverage:** UC-118

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-097`
Platform: Partner Web Portal
Primary role: Guest or unverified Partner Representative
Feature: Partner operations
Reference screen name: “Register Partner Account”
Purpose: Collects organization and representative information to create a partner application.
Functional coverage: UC-118

Canvas and navigation: WEB. Public/auth web layout: no signed-in sidebar until authentication/verification is completed.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Partner-account registration form: representative full name, work email/phone, password/confirm password, organization name, organization type, and terms/privacy acceptance.
2. Primary create-account CTA, validation, and OTP next step; no partner service details before account/OTP stage.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 098 — `CB-098` — Verify OTP

**Platform:** Partner Web Portal  
**Primary role:** Guest or unverified Partner Representative  
**Functional coverage:** UC-02

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-098`
Platform: Partner Web Portal
Primary role: Guest or unverified Partner Representative
Feature: Partner operations
Reference screen name: “Verify OTP”
Purpose: Validates the OTP used to verify the partner representative’s contact information.
Functional coverage: UC-02

Canvas and navigation: WEB. Public/auth web layout: no signed-in sidebar until authentication/verification is completed.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Masked work contact, six OTP cells, expiry/countdown, resend control, verify CTA, and inline expired/invalid state.
2. Back to registration action; no unrelated fields.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 099 — `CB-099` — Partner Profile Setup

**Platform:** Partner Web Portal  
**Primary role:** Partner Representative  
**Functional coverage:** UC-118

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-099`
Platform: Partner Web Portal
Primary role: Partner Representative
Feature: Partner operations
Reference screen name: “Partner Profile Setup”
Purpose: Collects organization identity, contacts, facilities, services, and verification evidence.
Functional coverage: UC-118

Canvas and navigation: WEB. Public/auth web layout: no signed-in sidebar until authentication/verification is completed.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Partner profile setup sections: organization identity, representative contacts, organization/facility address, services, linked experts (if any), public description, evidence/documents.
2. Consent/governance acknowledgement and submit-for-verification CTA; draft save allowed.
3. Validate required identity/contact/evidence fields; no auto-public listing.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 100 — `CB-100` — Partner Verification Status

**Platform:** Partner Web Portal  
**Primary role:** Partner Representative  
**Functional coverage:** UC-123

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-100`
Platform: Partner Web Portal
Primary role: Partner Representative
Feature: Partner operations
Reference screen name: “Partner Verification Status”
Purpose: Shows the partner application status and any requested corrections or documents.
Functional coverage: UC-123

Canvas and navigation: WEB. Public/auth web layout: no signed-in sidebar until authentication/verification is completed.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Partner verification headline/status, submitted profile/documents checklist, requested corrections/rejection reason, and updated date.
2. Actions to edit, upload replacement evidence, resubmit; public listing is clearly unavailable until approved.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 101 — `CB-101` — Partner Dashboard

**Platform:** Partner Web Portal  
**Primary role:** Partner Representative  
**Functional coverage:** UC-122

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-101`
Platform: Partner Web Portal
Primary role: Partner Representative
Feature: Partner operations
Reference screen name: “Partner Dashboard”
Purpose: Summarizes profile status, associated experts, services, referrals, campaigns, and performance.
Functional coverage: UC-122

Canvas and navigation: WEB. Use Partner Portal sidebar: Tổng quan · Hồ sơ · Chuyên gia liên kết · Dịch vụ · Giới thiệu lịch · Chiến dịch · Hiệu quả.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Partner dashboard cards for profile/verification status, associated experts, services, appointment referrals, campaigns, and aggregate performance.
2. Action-first shortcuts for update profile, add service, add campaign; no detailed personal health data.
3. Compact status alerts for requested corrections/expiring evidence.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 102 — `CB-102` — Partner Profile

**Platform:** Partner Web Portal  
**Primary role:** Partner Representative  
**Functional coverage:** UC-119

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-102`
Platform: Partner Web Portal
Primary role: Partner Representative
Feature: Partner operations
Reference screen name: “Partner Profile”
Purpose: Displays and updates the approved organization profile and public partner information.
Functional coverage: UC-119

Canvas and navigation: WEB. Use Partner Portal sidebar: Tổng quan · Hồ sơ · Chuyên gia liên kết · Dịch vụ · Giới thiệu lịch · Chiến dịch · Hiệu quả.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Partner profile editor with public identity, contacts, approved locations/services, display text, approved badge/state, and last review date.
2. Edit/save and submit-update action; status informs when changes need review.
3. No unapproved sponsor claims or medical advertising language.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 103 — `CB-103` — Associated Experts

**Platform:** Partner Web Portal  
**Primary role:** Partner Representative  
**Functional coverage:** UC-119

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-103`
Platform: Partner Web Portal
Primary role: Partner Representative
Feature: Partner operations
Reference screen name: “Associated Experts”
Purpose: Lists experts linked to the partner and supports submission or removal requests.
Functional coverage: UC-119

Canvas and navigation: WEB. Use Partner Portal sidebar: Tổng quan · Hồ sơ · Chuyên gia liên kết · Dịch vụ · Giới thiệu lịch · Chiến dịch · Hiệu quả.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Associated-expert table with expert name, specialty, linkage status, evidence/status, submitted date, and actions request add/remove.
2. Add/remove request drawer contains expert selection/evidence/reason; change requires review rather than instant public linkage.
3. Filters/search and empty state.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 104 — `CB-104` — Service Listings

**Platform:** Partner Web Portal  
**Primary role:** Partner Representative  
**Functional coverage:** UC-120

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-104`
Platform: Partner Web Portal
Primary role: Partner Representative
Feature: Partner operations
Reference screen name: “Service Listings”
Purpose: Lists and manages partner service or appointment-referral listings submitted for review.
Functional coverage: UC-120

Canvas and navigation: WEB. Use Partner Portal sidebar: Tổng quan · Hồ sơ · Chuyên gia liên kết · Dịch vụ · Giới thiệu lịch · Chiến dịch · Hiệu quả.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Service-listing table with service name/type, facility/location, channel, status, submitted/approved date, and action.
2. Create/edit listing drawer fields: name, category, location, short description, eligibility notes, contact/booking routing, evidence, status submission.
3. No medical prescription, price manipulation, or unapproved publication.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 105 — `CB-105` — Appointment Referrals

**Platform:** Partner Web Portal  
**Primary role:** Partner Representative  
**Functional coverage:** UC-120

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-105`
Platform: Partner Web Portal
Primary role: Partner Representative
Feature: Partner operations
Reference screen name: “Appointment Referrals”
Purpose: Shows appointment referrals, their status, and permitted operational details.
Functional coverage: UC-120

Canvas and navigation: WEB. Use Partner Portal sidebar: Tổng quan · Hồ sơ · Chuyên gia liên kết · Dịch vụ · Giới thiệu lịch · Chiến dịch · Hiệu quả.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Appointment-referral list with referral ID, service, date/time, status, operational contact, and permitted notes only.
2. Filters by status/date/service; open referral detail uses minimum necessary data and indicates consent boundary.
3. No access to unrelated personal health records.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 106 — `CB-106` — Sponsored Campaigns

**Platform:** Partner Web Portal  
**Primary role:** Partner Representative  
**Functional coverage:** UC-121

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-106`
Platform: Partner Web Portal
Primary role: Partner Representative
Feature: Partner operations
Reference screen name: “Sponsored Campaigns”
Purpose: Lists and manages sponsored education or support campaigns submitted for approval.
Functional coverage: UC-121

Canvas and navigation: WEB. Use Partner Portal sidebar: Tổng quan · Hồ sơ · Chuyên gia liên kết · Dịch vụ · Giới thiệu lịch · Chiến dịch · Hiệu quả.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Sponsored-campaign table with title, campaign type, target topic/audience, sponsor label, status, period, and review state.
2. Create/edit fields: title, purpose, education/support content, target scope, period, sponsor disclosure, supporting evidence; submit for approval.
3. Explicit rule: no disguised medicine/formula/supplement advertising and no targeting based on private health data.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 107 — `CB-107` — Partner Performance

**Platform:** Partner Web Portal  
**Primary role:** Partner Representative  
**Functional coverage:** UC-122

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-107`
Platform: Partner Web Portal
Primary role: Partner Representative
Feature: Partner operations
Reference screen name: “Partner Performance”
Purpose: Displays aggregated referral, service, and campaign performance metrics.
Functional coverage: UC-122

Canvas and navigation: WEB. Use Partner Portal sidebar: Tổng quan · Hồ sơ · Chuyên gia liên kết · Dịch vụ · Giới thiệu lịch · Chiến dịch · Hiệu quả.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Aggregate performance dashboard for service/referral/campaign metrics with period filters.
2. Anonymized totals and operational conversion/status, no individual health data.
3. Export/view details only where authorized.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 108 — `CB-108` — Notifications

**Platform:** Each actor platform  
**Primary role:** Current signed-in CareBridge actor  
**Functional coverage:** UC-11–UC-12

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-108`
Platform: Each actor platform
Primary role: Current signed-in CareBridge actor
Feature: Shared account
Reference screen name: “Notifications”
Purpose: Lists role-specific notifications with read and unread status.
Functional coverage: UC-11–UC-12

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation unless this is explicitly a primary signed-in destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Role-aware notification center for current platform with filters all/unread/type and mark-as-read actions.
2. Rows show short operational message, time, unread state, and deep link; no sensitive health content in preview.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 109 — `CB-109` — Notification Detail

**Platform:** Each actor platform  
**Primary role:** Current signed-in CareBridge actor  
**Functional coverage:** UC-11

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-109`
Platform: Each actor platform
Primary role: Current signed-in CareBridge actor
Feature: Shared account
Reference screen name: “Notification Detail”
Purpose: Shows the full notification and opens the related authorized screen or action.
Functional coverage: UC-11

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation unless this is explicitly a primary signed-in destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Notification detail with type/status/time, full permitted message, related action CTA, and mark-as-read state.
2. Back-to-notifications; handle linked resource unavailable/permission-revoked state gracefully.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 110 — `CB-110` — Account Profile

**Platform:** Each actor platform  
**Primary role:** Current signed-in CareBridge actor  
**Functional coverage:** UC-08, UC-20

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-110`
Platform: Each actor platform
Primary role: Current signed-in CareBridge actor
Feature: Shared account
Reference screen name: “Account Profile”
Purpose: Displays the signed-in user’s account information and account-setting shortcuts.
Functional coverage: UC-08, UC-20

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation unless this is explicitly a primary signed-in destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Account profile header with avatar placeholder, account name, role(s), verification/status, contact preview, and settings shortcuts.
2. Sections for community profile/public visibility when applicable; no hidden sensitive admin-only details.
3. Actions edit profile, change password, notifications, sessions, privacy, logout.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 111 — `CB-111` — Edit Account Profile

**Platform:** Each actor platform  
**Primary role:** Current signed-in CareBridge actor  
**Functional coverage:** UC-09, UC-21

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-111`
Platform: Each actor platform
Primary role: Current signed-in CareBridge actor
Feature: Shared account
Reference screen name: “Edit Account Profile”
Purpose: Updates the signed-in user’s own non-sensitive account information.
Functional coverage: UC-09, UC-21

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation unless this is explicitly a primary signed-in destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Edit form for non-sensitive fields: display/full name, avatar, phone, region/area, optional community profile fields.
2. Fields that require verification are locked or show verify path; save/cancel and inline validation.
3. No role/permission change by self-service.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 112 — `CB-112` — Change Password

**Platform:** Each actor platform  
**Primary role:** Current signed-in CareBridge actor  
**Functional coverage:** UC-07

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-112`
Platform: Each actor platform
Primary role: Current signed-in CareBridge actor
Feature: Shared account
Reference screen name: “Change Password”
Purpose: Changes the current account password after validating the existing password.
Functional coverage: UC-07

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation unless this is explicitly a primary signed-in destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Current-password, new-password, and confirm-password inputs with show/hide controls and policy checklist.
2. Validation and save action; success message asks user to sign in again only if policy requires it.
3. Never expose current password value.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 113 — `CB-113` — Notification Preferences

**Platform:** Each actor platform  
**Primary role:** Current signed-in CareBridge actor  
**Functional coverage:** UC-10

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-113`
Platform: Each actor platform
Primary role: Current signed-in CareBridge actor
Feature: Shared account
Reference screen name: “Notification Preferences”
Purpose: Manages the notification channels and categories available to the current role.
Functional coverage: UC-10

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation unless this is explicitly a primary signed-in destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Notification channel toggles and category preferences available to the current role.
2. Each choice has label, compact effect explanation, and save/reset controls; emergency/safety required notifications clearly marked if not optional.
3. No confusing all-or-nothing privacy defaults.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 114 — `CB-114` — Login Sessions

**Platform:** Each actor platform  
**Primary role:** Current signed-in CareBridge actor  
**Functional coverage:** UC-16

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-114`
Platform: Each actor platform
Primary role: Current signed-in CareBridge actor
Feature: Shared account
Reference screen name: “Login Sessions”
Purpose: Lists active and recent login sessions for the current account.
Functional coverage: UC-16

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation unless this is explicitly a primary signed-in destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Session list with device/browser, approximate location, last active, current-session badge, and status.
2. Row action “Đăng xuất thiết bị này” opens confirmation; one global sign-out-other-devices action if supported.
3. No exact IP address exposed to the user.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 115 — `CB-115` — Revoke Session Confirmation

**Platform:** Each actor platform  
**Primary role:** Current signed-in CareBridge actor  
**Functional coverage:** UC-16

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-115`
Platform: Each actor platform
Primary role: Current signed-in CareBridge actor
Feature: Shared account
Reference screen name: “Revoke Session Confirmation”
Purpose: Confirms signing out a selected device by revoking its stored session or refresh token.
Functional coverage: UC-16

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation unless this is explicitly a primary signed-in destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Confirmation dialog/sheet within the account screen with target device, last active time, consequence, Cancel and “Đăng xuất thiết bị” CTA.
2. Processing/success state; current session cannot be revoked accidentally without explicit scope warning.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 116 — `CB-116` — Logout Confirmation

**Platform:** Each actor platform  
**Primary role:** Current signed-in CareBridge actor  
**Functional coverage:** UC-04

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-116`
Platform: Each actor platform
Primary role: Current signed-in CareBridge actor
Feature: Shared account
Reference screen name: “Logout Confirmation”
Purpose: Confirms ending the current login session and returning to the login screen.
Functional coverage: UC-04

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation unless this is explicitly a primary signed-in destination.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Logout confirmation with current role/account summary, optional consequence note, Cancel and “Đăng xuất” CTA.
2. No long copy; show loading and route to login after confirmation.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 117 — `CB-117` — Community Search

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-162

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-117`
Platform: Mother Mobile App
Primary role: Mother
Feature: Community Q&A
Reference screen name: “Community Search”
Purpose: Searches community questions by keyword, stage, topic, answer status and expert label.
Functional coverage: UC-162

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Community search field with keyword input, recent search chips, and filters for stage, topic, answer status, expert answer, and date.
2. Result cards show short question, topic/stage, answer count, verified-expert label, and saved state.
3. Empty/no-result and reset-filter state; do not reveal private/removed content.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 118 — `CB-118` — Topic Directory

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-163

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-118`
Platform: Mother Mobile App
Primary role: Mother
Feature: Community Q&A
Reference screen name: “Topic Directory”
Purpose: Lists and searches community topics for pregnancy, postpartum, child care, nutrition, psychology and safety.
Functional coverage: UC-163

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Topic directory with keyword search, topic cards, followed state, and categories such as pregnancy, postpartum, child care, nutrition, psychology, safety.
2. Each topic shows short description and question count without long editorial copy.
3. Open-topic action; no medical recommendation in card copy.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 119 — `CB-119` — Topic Detail

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-171

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-119`
Platform: Mother Mobile App
Primary role: Mother
Feature: Community Q&A
Reference screen name: “Topic Detail”
Purpose: Shows a topic feed and allows the user to follow or unfollow that topic.
Functional coverage: UC-171

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Topic header with follow/unfollow control, short topic description, and moderation/source label.
2. Filtered topic feed with sort and create-question shortcut; show answer/expert labels.
3. Follow state and empty-topic state; reporting remains available per post.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 120 — `CB-120` — Edit Community Post

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-55

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-120`
Platform: Mother Mobile App
Primary role: Mother
Feature: Community Q&A
Reference screen name: “Edit Community Post”
Purpose: Edits the user’s own community post while it remains editable and is not locked by moderation.
Functional coverage: UC-55

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Prefilled post editor with text, topic, stage/age context, anonymous-display setting, attachment preview if already allowed, and save changes.
2. Editability/locked-by-moderation status visible; show validation and cancel-with-unsaved-changes state.
3. No edit action if post is locked; do not add new medical form fields.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 121 — `CB-121` — Delete Community Post Confirmation

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-170

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-121`
Platform: Mother Mobile App
Primary role: Mother
Feature: Community Q&A
Reference screen name: “Delete Community Post Confirmation”
Purpose: Confirms deletion or archival of the user’s own post when no moderation or investigation lock applies.
Functional coverage: UC-170

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Destructive confirmation sheet with post preview, reason/impact notice, Cancel and “Xóa bài đăng” CTA.
2. Explain action is blocked if under moderation/investigation; use loading/success state.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 122 — `CB-122` — File Manager

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-167–UC-169

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-122`
Platform: Mother Mobile App
Primary role: Mother
Feature: File management
Reference screen name: “File Manager”
Purpose: Lists uploaded ultrasound images, medical records, vaccination files and child photos with ownership and access status.
Functional coverage: UC-167–UC-169

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. File manager list/grid with file name, category, owner, record date, source, access/sharing state, upload date, and protected-view action.
2. Filters for category/owner/date/access plus search; upload CTA; delete action is an explicit confirmation.
3. No public download link; show access denied/expired share state.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 123 — `CB-123` — File Viewer

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-168

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-123`
Platform: Mother Mobile App
Primary role: Mother
Feature: File management
Reference screen name: “File Viewer”
Purpose: Previews or downloads an authorized file through a protected access link.
Functional coverage: UC-168

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Protected file preview with file name/category/date/owner/source and short metadata.
2. View/download action only when authorized; sharing status/consent expiry visible if file is shared.
3. Unsupported-preview, access-denied, and file-unavailable states.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 124 — `CB-124` — Upload File

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-167

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-124`
Platform: Mother Mobile App
Primary role: Mother
Feature: File management
Reference screen name: “Upload File”
Purpose: Collects file, category, owner, date and metadata before secure upload validation.
Functional coverage: UC-167

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Upload form: file picker, category, owner (Mother/Baby), record date, title/name, tags, optional note, and sharing default private.
2. File type/size validation and upload progress; primary secure-upload CTA.
3. No sharing scope selected by default.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 125 — `CB-125` — Delete File Confirmation

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-169

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-125`
Platform: Mother Mobile App
Primary role: Mother
Feature: File management
Reference screen name: “Delete File Confirmation”
Purpose: Confirms soft deletion of a user-owned file when retention or record-link rules do not block deletion.
Functional coverage: UC-169

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Delete confirmation with file preview/name, linked-record/retention impact, Cancel and delete CTA.
2. If deletion is blocked by record/retention rule, show exact reason and safe return action.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 126 — `CB-126` — Expert Search and Filters

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-164–UC-165

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-126`
Platform: Mother Mobile App
Primary role: Mother
Feature: Expert discovery
Reference screen name: “Expert Search and Filters”
Purpose: Searches and filters verified experts by name, specialty, channel, availability, fee, rating, online state and consented distance.
Functional coverage: UC-164–UC-165

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Advanced expert-search filter sheet with keyword/name, specialty, channel, availability, fee range, rating, online state, and consented-distance.
2. Applied-filter chips, sort, result count, reset filters, and list/map switch only with location consent.
3. Results link to profile, not direct payment; no private location without consent.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 127 — `CB-127` — Emergency Contacts

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-176

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-127`
Platform: Mother Mobile App
Primary role: Mother
Feature: Safety monitoring
Reference screen name: “Emergency Contacts”
Purpose: Lists verified emergency contacts and their priority for MF-21 and MF-19 alert delivery.
Functional coverage: UC-176

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Emergency-contact list with priority order, display name, relationship, masked contact, verification state, and delivery eligibility.
2. Add/edit/reorder/remove actions; empty state explains a contact is needed before monitoring can be enabled.
3. No automatic contact addition.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 128 — `CB-128` — Edit Emergency Contact

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-176

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-128`
Platform: Mother Mobile App
Primary role: Mother
Feature: Safety monitoring
Reference screen name: “Edit Emergency Contact”
Purpose: Adds, verifies, reprioritizes or removes an emergency contact.
Functional coverage: UC-176

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Contact form: name, relationship, phone/email, priority, verification method/status, and remove option for existing contact.
2. Validation for duplicate/invalid contact; save and send/verify action; removal uses confirmation.
3. Keep sensitive contact details protected.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 129 — `CB-129` — Enable Fall Detection Confirmation

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-134

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-129`
Platform: Mother Mobile App
Primary role: Mother
Feature: Safety monitoring
Reference screen name: “Enable Fall Detection Confirmation”
Purpose: Confirms consent, sensor permission and monitoring conditions before fall detection starts.
Functional coverage: UC-134

Canvas and navigation: MOBILE. Use a simple top app bar with a clear back/close action. Do not render bottom navigation on this auth, setup, invite, or confirmation screen.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Enable-fall-detection confirmation with sensor permission, conditions of use, countdown setting, emergency-contact readiness, and location-sharing choice.
2. Explicit acknowledge checkbox, Cancel and “Bật phát hiện ngã” CTA; CTA disabled if prerequisites missing.
3. Clarify it detects suspected events, not medical diagnosis.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 130 — `CB-130` — Disable Fall Detection Confirmation

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-135

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-130`
Platform: Mother Mobile App
Primary role: Mother
Feature: Safety monitoring
Reference screen name: “Disable Fall Detection Confirmation”
Purpose: Confirms stopping sensor monitoring and optionally retaining the configuration for later use.
Functional coverage: UC-135

Canvas and navigation: MOBILE. Use a simple top app bar with a clear back/close action. Do not render bottom navigation on this auth, setup, invite, or confirmation screen.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Disable confirmation with current monitoring state, impact (alerts stop), optional reason, Cancel and “Tắt phát hiện ngã” CTA.
2. No data deletion implied; show successful disabled state.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 131 — `CB-131` — Community Search

**Platform:** Family Member Mobile App  
**Primary role:** Family Member  
**Functional coverage:** UC-162

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-131`
Platform: Family Member Mobile App
Primary role: Family Member
Feature: Community Q&A
Reference screen name: “Community Search”
Purpose: Searches community questions and topics available to the family member.
Functional coverage: UC-162

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this detail, invite, content, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Family-specific community search field and filters that expose only public/moderated content.
2. Results show question/topic/answer status and safe labels; same no-result/reset state.
3. No mother-private group data in search results.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 132 — `CB-132` — Topic Detail

**Platform:** Family Member Mobile App  
**Primary role:** Family Member  
**Functional coverage:** UC-171

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-132`
Platform: Family Member Mobile App
Primary role: Family Member
Feature: Community Q&A
Reference screen name: “Topic Detail”
Purpose: Shows posts within a topic and allows follow or unfollow actions.
Functional coverage: UC-171

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this detail, invite, content, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Topic header, follow/unfollow, public moderated feed, and topic filters.
2. Family user can answer only if the generic community role permits; answer is labeled personal experience.
3. No private health data/context automatically inserted.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 133 — `CB-133` — Emergency Alert Detail

**Platform:** Family Member Mobile App  
**Primary role:** Family Member  
**Functional coverage:** UC-161

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-133`
Platform: Family Member Mobile App
Primary role: Family Member
Feature: Emergency alerts
Reference screen name: “Emergency Alert Detail”
Purpose: Shows the minimum consented alert context, location and response actions for an emergency notification.
Functional coverage: UC-161

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this detail, invite, content, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Full-screen emergency-alert detail with urgency, time, sender/group, permitted location/context, and delivery status.
2. Immediate contact/call, acknowledge, map route, and emergency-support actions as permission allows.
3. Fallback when location unavailable; never show full patient history.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 134 — `CB-134` — Verification Renewal

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-173

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-134`
Platform: Expert App
Primary role: Verified Expert
Feature: Expert governance
Reference screen name: “Verification Renewal”
Purpose: Starts renewal of expert verification before credential expiry.
Functional coverage: UC-173

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Renewal form with expiring credential list, replacement document upload, expiry date, updated specialty/organization details where needed, and submit CTA.
2. Verification-state banner and validation; do not claim approval on submission.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 135 — `CB-135` — Renewal Status

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-173

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-135`
Platform: Expert App
Primary role: Verified Expert
Feature: Expert governance
Reference screen name: “Renewal Status”
Purpose: Shows submitted renewal documents, review status, expiry date and required follow-up.
Functional coverage: UC-173

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Renewal status header plus document checklist, review state, requested correction/rejection reason, and time submitted.
2. Action to correct/resubmit or return to profile; upcoming expiry is explicit.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 136 — `CB-136` — Expert Suspension Status

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-172

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-136`
Platform: Expert App
Primary role: Verified Expert
Feature: Expert governance
Reference screen name: “Expert Suspension Status”
Purpose: Explains a suspension, restricted capabilities, effective period and permitted appeal or support action.
Functional coverage: UC-172

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Suspension status screen with reason category, effective date/duration, affected features, appeal/contact path if provided, and no hidden enforcement controls.
2. No ability to accept new consults or answer questions while suspended; show a neutral support link.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 137 — `CB-137` — Verification Renewal

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-173

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-137`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Expert governance
Reference screen name: “Verification Renewal”
Purpose: Submits updated credentials and tracks renewal before the current verification expires.
Functional coverage: UC-173

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Web renewal workspace with document table, update/upload drawer, expiry filters, profile changes, and submit-for-review CTA.
2. Unsaved changes and validation state; retain audit/version context.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 138 — `CB-138` — Expert Suspension Status

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-172

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-138`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Expert governance
Reference screen name: “Expert Suspension Status”
Purpose: Displays suspension reason, restricted capabilities, evidence and available appeal or support actions.
Functional coverage: UC-172

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Web suspension status with reason, effective scope, duration/review date, affected portal features, and support/appeal route if available.
2. Use calm operational B2B presentation; no action that bypasses suspension.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 139 — `CB-139` — Expert Verification Renewal Queue

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-173

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-139`
Platform: Admin Web Portal
Primary role: System Admin
Feature: Expert governance
Reference screen name: “Expert Verification Renewal Queue”
Purpose: Lists expert renewal submissions ordered by expiry risk, completeness and review status.
Functional coverage: UC-173

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Admin renewal queue table: expert, credential(s), expiry risk, submitted time, completeness, status, reviewer, and actions.
2. Filters/search; review row opens renewal detail; no one-click approval without review.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 140 — `CB-140` — Expert Verification Renewal Detail

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-173

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-140`
Platform: Admin Web Portal
Primary role: System Admin
Feature: Expert governance
Reference screen name: “Expert Verification Renewal Detail”
Purpose: Shows renewal credentials, previous verification, expiry history and approve, supplement or reject actions.
Functional coverage: UC-173

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Renewal review detail with current approved profile vs submitted changes, credential file metadata, expiry, evidence, prior verification, and audit timeline.
2. Approve, reject, request correction actions with reason required and confirmation; do not expose raw files outside protected viewer.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 141 — `CB-141` — Suspend Expert Confirmation

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-172

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-141`
Platform: Admin Web Portal
Primary role: System Admin
Feature: Expert governance
Reference screen name: “Suspend Expert Confirmation”
Purpose: Confirms suspension scope, reason, effective period and impact on public listing and consultation access.
Functional coverage: UC-172

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Suspend-expert confirmation dialog with selected expert, reason category, duration/effective time, impacts, moderator/admin note, Cancel and suspend CTA.
2. CTA is disabled until reason/duration confirmation is complete; status is auditable.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 146 — `CB-146` — Shared File Viewer

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-168

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-146`
Platform: Expert App
Primary role: Verified Expert
Feature: Shared files
Reference screen name: “Shared File Viewer”
Purpose: Previews a health-record or consultation file only while the expert’s consent scope and access period remain valid.
Functional coverage: UC-168

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Protected shared-file viewer with consultation/care-group context, owner, category, source, consent scope/purpose/expiry, and preview.
2. No download/share-forward when scope forbids it; access expired/denied state.
3. Back to relevant consultation or shared-data view.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 147 — `CB-147` — Search Community Questions

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-162

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-147`
Platform: Expert App
Primary role: Verified Expert
Feature: Community Q&A
Reference screen name: “Search Community Questions”
Purpose: Searches community questions by keyword, topic, status and specialty relevance.
Functional coverage: UC-162

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use the floating Expert App bottom navigation: “Trang chủ”, “Yêu cầu”, “Cộng đồng”, “Tài khoản”; mark “Cộng đồng” active.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Expert community-question search with keyword, topic, urgency, answer status, and specialty-match filters.
2. Result cards show moderation status, author anonymity, answer count, and open question action.
3. No direct access to private user health records.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 148 — `CB-148` — Search Community Topics

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-163

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-148`
Platform: Expert App
Primary role: Verified Expert
Feature: Community Q&A
Reference screen name: “Search Community Topics”
Purpose: Searches and browses community topics available to the expert.
Functional coverage: UC-163

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use the floating Expert App bottom navigation: “Trang chủ”, “Yêu cầu”, “Cộng đồng”, “Tài khoản”; mark “Cộng đồng” active.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Expert topic search/directory with keyword, specialty relevance, followed state, and content/question counts.
2. Open topic and follow/unfollow; use public moderated content only.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 149 — `CB-149` — Topic Detail

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-171

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-149`
Platform: Expert App
Primary role: Verified Expert
Feature: Community Q&A
Reference screen name: “Topic Detail”
Purpose: Shows posts in a selected topic and supports following or unfollowing it.
Functional coverage: UC-171

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Expert topic page with header, follow state, filtered question feed, specialty relevance, and answer actions.
2. Answer workflow preserves moderation/safety labels; no diagnosis/prescription language.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 150 — `CB-150` — Shared File Viewer

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-168

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-150`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Shared files
Reference screen name: “Shared File Viewer”
Purpose: Displays an authorized shared file within the expert’s valid consent scope.
Functional coverage: UC-168

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Web protected-file viewer with side metadata panel: owner, linked consultation, file category, source, consent scope/purpose/expiry.
2. Preview/download only when permitted; audit-friendly access state; unavailable/expired view.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 152 — `CB-152` — Pre-exercise Safety Check

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-177–UC-179

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-152`
Platform: Mother Mobile App
Primary role: Mother
Feature: Pregnancy Exercise & Posture Support
Reference screen name: “Pre-exercise Safety Check”
Purpose: Collects mandatory safety answers and blocks starting the exercise when a configured warning condition is present.
Functional coverage: UC-177–UC-179

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Pre-exercise check form with selected exercise, current stage/trimester, checklist of safety questions, current symptom/concern input, and stop/continue decision.
2. Contraindication/unsafe answers route to safe next action; do not call them diagnoses.
3. Primary “Bắt đầu buổi tập” only enabled after safe completion; “Quay lại danh sách bài tập” action.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 153 — `CB-153` — Exercise Session

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-30, UC-179–UC-182

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-153`
Platform: Mother Mobile App
Primary role: Mother
Feature: Pregnancy Exercise & Posture Support
Reference screen name: “Exercise Session”
Purpose: Runs the selected exercise with instructions, timer, pause/resume controls and optional live posture feedback.
Functional coverage: UC-30, UC-179–UC-182

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Active exercise screen with exercise name, step/progress, elapsed time, pause/resume, end session, and optional posture-camera state.
2. Large visual posture guidance/feedback with confidence/severity labels and concise safe correction cue; no diagnostic result.
3. Controls for camera permission, mute feedback, and emergency/stop; paused/network/camera denied state.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 154 — `CB-154` — Enable Posture Camera Confirmation

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-180

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-154`
Platform: Mother Mobile App
Primary role: Mother
Feature: Pregnancy Exercise & Posture Support
Reference screen name: “Enable Posture Camera Confirmation”
Purpose: Requests explicit consent before enabling camera-based posture analysis for the current exercise session.
Functional coverage: UC-180

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Camera-enable confirmation sheet with purpose, on-device/privacy statement, camera permission, no-storage note, Cancel and enable CTA.
2. Explain posture feedback is support only and can be turned off; permission-denied repair action.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 155 — `CB-155` — Exercise Session Result

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-183

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-155`
Platform: Mother Mobile App
Primary role: Mother
Feature: Pregnancy Exercise & Posture Support
Reference screen name: “Exercise Session Result”
Purpose: Shows duration, completion status, aggregate posture score, common posture issues and safety warnings from the completed session.
Functional coverage: UC-183

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Exercise result summary with duration, completion percent, posture feedback count/summary, pause count, and safe next step.
2. Buttons view history, repeat later, or return to exercise list; no medical fitness verdict.
3. If session stopped early, explain neutrally and retain only permitted session summary.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 156 — `CB-156` — Exercise History

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-184

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-156`
Platform: Mother Mobile App
Primary role: Mother
Feature: Pregnancy Exercise & Posture Support
Reference screen name: “Exercise History”
Purpose: Lists stored exercise sessions and opens the result of a selected session.
Functional coverage: UC-184

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Exercise-history list with date, exercise, trimester tag, duration, completion, and result label.
2. Filters by date/exercise/status; open session result; empty state with browse exercises CTA.
3. No health diagnosis or body-image judgment.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 157 — `CB-157` — Maternal Health Metric Detail

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-187–UC-188

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-157`
Platform: Mother Mobile App
Primary role: Mother
Feature: Mother Care Journey
Reference screen name: “Maternal Health Metric Detail”
Purpose: Shows one maternal metric with value, time, source and note, and provides authorized edit or delete actions.
Functional coverage: UC-187–UC-188

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Maternal metric detail with metric type, date/time, value/unit, source “Bạn nhập” or device label, optional note, and historical trend mini-chart.
2. Edit and delete actions with confirmation; show invalid/out-of-range formatting as “cần kiểm tra lại” not a diagnosis.
3. No automatic medication advice.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 158 — `CB-158` — Postpartum Log List

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-189

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-158`
Platform: Mother Mobile App
Primary role: Mother
Feature: Mother Care Journey
Reference screen name: “Postpartum Log List”
Purpose: Lists postpartum recovery logs chronologically with filters by symptom or log type.
Functional coverage: UC-189

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Postpartum log list with date, selected log tags (sleep, recovery, feeding, mood, symptoms), short note preview, and filters/date range.
2. Add-log CTA and row detail action; empty state is concise.
3. No health conclusion in cards.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 159 — `CB-159` — Postpartum Log Detail

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-190–UC-191

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-159`
Platform: Mother Mobile App
Primary role: Mother
Feature: Mother Care Journey
Reference screen name: “Postpartum Log Detail”
Purpose: Shows one postpartum log and provides edit or soft-delete actions for the owner.
Functional coverage: UC-190–UC-191

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Postpartum log detail with prefilled categories, date/time, scale/selection values, private note, source label, and edit/delete actions.
2. Delete confirmation; “Cần hỗ trợ?” shortcut only routes safely to relevant support, not diagnosis.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 160 — `CB-160` — Switch Active Baby Selector

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-193

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-160`
Platform: Mother Mobile App
Primary role: Mother
Feature: Baby Care Journey
Reference screen name: “Switch Active Baby Selector”
Purpose: Allows a mother who manages multiple babies to select the profile used by dashboards, logs and reminders.
Functional coverage: UC-193

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Bottom-sheet/full selector of baby profiles with nickname, age, active label, and last updated summary.
2. One-tap select, manage profiles action, and accessible close/back; no new record fields here.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 161 — `CB-161` — Baby Daily Log Detail

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-194–UC-195

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-161`
Platform: Mother Mobile App
Primary role: Mother
Feature: Baby Care Journey
Reference screen name: “Baby Daily Log Detail”
Purpose: Shows one feeding, sleep, diaper, symptom or care entry and provides authorized update or delete actions.
Functional coverage: UC-194–UC-195

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Baby daily-log detail with selected log type, date/time, quantity/duration/unit when applicable, symptoms/notes, and source label.
2. Edit/delete actions with confirmation; related baby/profile context visible.
3. No medication dosing advice.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 162 — `CB-162` — Development Milestone Detail

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-196–UC-197

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-162`
Platform: Mother Mobile App
Primary role: Mother
Feature: Baby Care Journey
Reference screen name: “Development Milestone Detail”
Purpose: Shows a recorded milestone, date, note and status and provides update or soft-delete actions.
Functional coverage: UC-196–UC-197

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Milestone detail with milestone type, achieved date, optional note/media indicator, and baby age at record time.
2. Edit/delete actions with confirmation; nonjudgmental text and no diagnosis from timing.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 163 — `CB-163` — My Answer Detail

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-199–UC-201

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-163`
Platform: Mother Mobile App
Primary role: Mother
Feature: Community Q&A
Reference screen name: “My Answer Detail”
Purpose: Shows the mother’s own answer and provides edit or soft-delete actions while moderation rules allow changes.
Functional coverage: UC-199–UC-201

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Own-answer detail with linked question preview, answer content, expert/personal-experience label, likes, edit/delete controls, and moderation state.
2. If answer is locked/removed, show reason/status and disable edits; delete confirmation.
3. No use of medical authority for normal user answers.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 164 — `CB-164` — Reschedule Consultation

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-204

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-164`
Platform: Mother Mobile App
Primary role: Mother
Feature: Direct Consultation
Reference screen name: “Reschedule Consultation”
Purpose: Collects a proposed replacement slot and confirms the schedule change under the consultation policy.
Functional coverage: UC-204

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Reschedule form with original booking, permitted alternative slots, selected channel, current locked price snapshot, reason optional, and submit CTA.
2. Show policy/limit and conflict validation; no price recalculation unless documented booking rules permit it.
3. Cancelled/too-late state uses a safe return action.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 165 — `CB-165` — Cancel Consultation Confirmation

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-205

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-165`
Platform: Mother Mobile App
Primary role: Mother
Feature: Direct Consultation
Reference screen name: “Cancel Consultation Confirmation”
Purpose: Confirms cancellation, reason and any fee or timing consequence before changing consultation status.
Functional coverage: UC-205

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Cancel-consultation confirmation with booking summary, cancellation policy/impact, optional reason, Cancel and confirm-cancel CTA.
2. Payment/refund status is informational, not promised; processing/success state.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 166 — `CB-166` — Consultation Summary

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-208

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-166`
Platform: Mother Mobile App
Primary role: Mother
Feature: Direct Consultation
Reference screen name: “Consultation Summary”
Purpose: Shows the expert’s post-session summary and safe follow-up steps for the completed consultation.
Functional coverage: UC-208

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Consultation summary with date/channel/expert, shared-data scope that existed, concise expert/session summary, and next safe actions.
2. Rating/review and dispute/refund shortcuts only when lifecycle permits; no diagnosis/prescription content.
3. Protected status and no raw full records.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 167 — `CB-167` — Reminder Detail

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-212–UC-215

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-167`
Platform: Mother Mobile App
Primary role: Mother
Feature: Health Records
Reference screen name: “Reminder Detail”
Purpose: Shows reminder type, recurrence, next time, status and notes with complete, skip, edit and delete actions.
Functional coverage: UC-212–UC-215

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Reminder detail with type, subject, date/time, repeat rule, owner, status, notes, linked record if any, and notification state.
2. Actions complete, snooze, skip, edit, delete; skip/delete confirmation.
3. No automatic clinical instruction beyond user-entered/verified content label.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 168 — `CB-168` — Care Group Members

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-216, UC-219

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-168`
Platform: Mother Mobile App
Primary role: Mother
Feature: Family Sync
Reference screen name: “Care Group Members”
Purpose: Lists members, roles, invitation status and permission summary for a selected care group.
Functional coverage: UC-216, UC-219

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Care-group members list with role, invitation status, active permission scope/expiry, and task responsibility summary.
2. Actions invite, view/change/revoke permission, remove member; all scope changes have confirmation and reason/impact.
3. No full health data list.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 169 — `CB-169` — Pending Invitations

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-217

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-169`
Platform: Mother Mobile App
Primary role: Mother
Feature: Family Sync
Reference screen name: “Pending Invitations”
Purpose: Lists unaccepted family invitations and allows the mother to revoke an invitation.
Functional coverage: UC-217

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Pending invitations list with recipient privacy-safe contact, invite time, expiry, offered scope, and status.
2. Actions resend/cancel/revoke; revoke confirmation; expired state visible.
3. No invite recipient health access before acceptance.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 170 — `CB-170` — Family Task Detail

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-221–UC-223

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-170`
Platform: Mother Mobile App
Primary role: Mother
Feature: Family Sync
Reference screen name: “Family Task Detail”
Purpose: Shows assignee, due date, status and notes of a family task and supports update or cancellation.
Functional coverage: UC-221–UC-223

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Family-task detail with title, description, due date/time, assignee, group, status, priority, linked reminder, and activity timeline.
2. Actions update status, edit if authorized, cancel task with confirmation; no unauthorized reassignment.
3. Keep notes concise and care-focused.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 171 — `CB-171` — Verified Content Search

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-224

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-171`
Platform: Mother Mobile App
Primary role: Mother
Feature: Verified Content
Reference screen name: “Verified Content Search”
Purpose: Searches approved articles, FAQs and checklists by keyword, care stage and topic.
Functional coverage: UC-224

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Verified-content search with keyword, stage/age, topic, type (article/FAQ/checklist), source/verified filter, and sort.
2. Results display short title, tags, source label, update date, and saved state.
3. No unverified community post mixed into results.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 172 — `CB-172` — Verified Content Detail

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-225

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-172`
Platform: Mother Mobile App
Primary role: Mother
Feature: Verified Content
Reference screen name: “Verified Content Detail”
Purpose: Shows approved content, source, version, update date and applicable safety notes.
Functional coverage: UC-225

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Content detail with title, stage/topic tags, source labels, version/update date, short sectioned body, and actionable checklist where applicable.
2. Save/share/report action as allowed; short safety/when-to-seek-support cue if content topic needs it.
3. Never write a diagnosis, treatment, or dosing instruction.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 173 — `CB-173` — Vaccination Detail

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-228, UC-230–UC-233

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-173`
Platform: Mother Mobile App
Primary role: Mother
Feature: Vaccination & Growth
Reference screen name: “Vaccination Detail”
Purpose: Shows one scheduled or completed vaccination item with date, status, facility, notes and supporting file.
Functional coverage: UC-228, UC-230–UC-233

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Vaccination detail with vaccine name, recommended/reference date, actual date/status, child profile, source, and appointment/reminder linkage.
2. Actions update record, mark completed, postpone, delete where permitted; all changes confirm and preserve history.
3. Use “Lịch tham khảo” wording, not clinical prescription.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 174 — `CB-174` — Add Vaccination Record

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-229

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-174`
Platform: Mother Mobile App
Primary role: Mother
Feature: Vaccination & Growth
Reference screen name: “Add Vaccination Record”
Purpose: Collects vaccination date, vaccine, facility, notes and optional proof file.
Functional coverage: UC-229

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Add vaccination record form: vaccine name, dose/series, actual date, provider/location optional, source/attachment optional, note, and reminder option.
2. Validation for date/series; save and cancel; record source is clearly user-entered unless imported.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 175 — `CB-175` — Growth Measurement History

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-237

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-175`
Platform: Mother Mobile App
Primary role: Mother
Feature: Vaccination & Growth
Reference screen name: “Growth Measurement History”
Purpose: Lists weight, height and head-circumference measurements used by the growth chart.
Functional coverage: UC-237

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Growth measurement history with trend chart/table for weight, height/length, and head circumference when applicable; dates/source labels.
2. Filters metric/date; add measurement action; no diagnosis label.
3. Open a point detail from table/chart.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 176 — `CB-176` — Growth Measurement Detail

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-234–UC-236

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-176`
Platform: Mother Mobile App
Primary role: Mother
Feature: Vaccination & Growth
Reference screen name: “Growth Measurement Detail”
Purpose: Shows one growth measurement and provides authorized edit or soft-delete actions.
Functional coverage: UC-234–UC-236

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Growth measurement detail with date, metric values/units, source, note, and mini comparison to prior user data.
2. Edit/delete actions with confirmation; outlier shows “Kiểm tra lại số liệu” not medical conclusion.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 177 — `CB-177` — Reject Invitation Confirmation

**Platform:** Family Member Mobile App  
**Primary role:** Family Member  
**Functional coverage:** UC-218

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-177`
Platform: Family Member Mobile App
Primary role: Family Member
Feature: Family Sync
Reference screen name: “Reject Invitation Confirmation”
Purpose: Confirms declining a care-group invitation without creating membership or data access.
Functional coverage: UC-218

Canvas and navigation: MOBILE. Use a simple top app bar with a clear back/close action. Do not render bottom navigation on this auth, setup, invite, or confirmation screen.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Reject-invitation confirmation with group/inviter, offered scope, expiry, optional reason, Cancel and reject CTA.
2. No data is disclosed; success returns to safe group list.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 178 — `CB-178` — Leave Care Group Confirmation

**Platform:** Family Member Mobile App  
**Primary role:** Family Member  
**Functional coverage:** UC-220

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-178`
Platform: Family Member Mobile App
Primary role: Family Member
Feature: Family Sync
Reference screen name: “Leave Care Group Confirmation”
Purpose: Confirms leaving a care group and ending future access to its shared data.
Functional coverage: UC-220

Canvas and navigation: MOBILE. Use a simple top app bar with a clear back/close action. Do not render bottom navigation on this auth, setup, invite, or confirmation screen.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Leave-group confirmation with selected group, loss of shared-data/task/calendar access, Cancel and leave CTA.
2. Show active task handoff warning if relevant; no automatic deletion of owner records.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 179 — `CB-179` — My Answer Detail

**Platform:** Family Member Mobile App  
**Primary role:** Family Member  
**Functional coverage:** UC-199–UC-201

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-179`
Platform: Family Member Mobile App
Primary role: Family Member
Feature: Community Q&A
Reference screen name: “My Answer Detail”
Purpose: Shows the family member’s own answer and allows editing or soft deletion when permitted.
Functional coverage: UC-199–UC-201

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this detail, invite, content, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Family member own-answer detail with linked public question, answer, personal-experience label, likes, moderation state, edit/delete actions.
2. Edits disabled when locked; delete confirmation; never expose shared care-group data in answer context.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 180 — `CB-180` — Verified Content Search

**Platform:** Family Member Mobile App  
**Primary role:** Family Member  
**Functional coverage:** UC-224

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-180`
Platform: Family Member Mobile App
Primary role: Family Member
Feature: Verified Content
Reference screen name: “Verified Content Search”
Purpose: Searches approved articles, FAQs and checklists available to the family member.
Functional coverage: UC-224

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this detail, invite, content, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Family verified-content search identical in structure to Mother version but no mother-private recommendations.
2. Filters/search/result cards with trusted source labels.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 181 — `CB-181` — Verified Content Detail

**Platform:** Family Member Mobile App  
**Primary role:** Family Member  
**Functional coverage:** UC-225

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-181`
Platform: Family Member Mobile App
Primary role: Family Member
Feature: Verified Content
Reference screen name: “Verified Content Detail”
Purpose: Shows approved content with source, version and safety information.
Functional coverage: UC-225

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this detail, invite, content, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Family verified-content detail with source/version, stage/topic tags, concise body, save/share/report as permitted.
2. No diagnosis or private data auto-personalization.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 182 — `CB-182` — My Expert Answer Detail

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-199–UC-201

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-182`
Platform: Expert App
Primary role: Verified Expert
Feature: Community Q&A
Reference screen name: “My Expert Answer Detail”
Purpose: Shows the expert’s own community answer and supports authorized edit or soft-delete actions.
Functional coverage: UC-199–UC-201

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Expert’s own answer detail with linked question, response, verified-expert label, views/likes, suggestion-to-consult flag, moderation state, edit/delete actions.
2. Safe wording hint and locked state; changes preserve moderation/audit context.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 183 — `CB-183` — Reschedule Consultation

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-204

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-183`
Platform: Expert App
Primary role: Verified Expert
Feature: Direct Consultation
Reference screen name: “Reschedule Consultation”
Purpose: Allows the expert to propose or confirm a new consultation time before the policy deadline.
Functional coverage: UC-204

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Expert reschedule form showing original session, alternate availability slots, channel, locked price snapshot, optional reason, and submit CTA.
2. Conflict/notice validation; no price changes to existing booking.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 184 — `CB-184` — Mark No-show Confirmation

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-207

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-184`
Platform: Expert App
Primary role: Verified Expert
Feature: Direct Consultation
Reference screen name: “Mark No-show Confirmation”
Purpose: Confirms recording a participant no-show after the required waiting period and session evidence.
Functional coverage: UC-207

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. No-show confirmation with booking/session data, participant, scheduled time, evidence/note field, policy impact, Cancel and mark-no-show CTA.
2. Requires confirmation/reason; no automatic punishment copy.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 185 — `CB-185` — Complete Consultation Confirmation

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-206

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-185`
Platform: Expert App
Primary role: Verified Expert
Feature: Direct Consultation
Reference screen name: “Complete Consultation Confirmation”
Purpose: Confirms ending the active consultation before writing the post-session summary.
Functional coverage: UC-206

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Complete-consultation confirmation with session/booking summary, completion time, optional concise summary/note status, commission/payment pending state, Cancel and complete CTA.
2. Confirmation prevents accidental closure; no diagnostic documentation form.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 186 — `CB-186` — My Expert Answer Detail

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-199–UC-201

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-186`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Community Q&A
Reference screen name: “My Expert Answer Detail”
Purpose: Shows an expert-authored answer and provides edit or soft-delete actions when permitted.
Functional coverage: UC-199–UC-201

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Web expert answer detail with question pane, own answer, moderation/status/history, edit/delete toolbar, and safe-writing prompt.
2. No edit when locked; audit-friendly timestamps.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 187 — `CB-187` — Reschedule Consultation

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-204

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-187`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Direct Consultation
Reference screen name: “Reschedule Consultation”
Purpose: Allows the expert to propose or confirm a replacement consultation slot.
Functional coverage: UC-204

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Web reschedule drawer/page with original booking, calendar slot picker, channel, immutable price, reason, policy/conflict summary, submit CTA.
2. No recalculation of existing price snapshot.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 188 — `CB-188` — Mark No-show Confirmation

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-207

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-188`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Direct Consultation
Reference screen name: “Mark No-show Confirmation”
Purpose: Confirms a no-show decision after checking timing and technical session evidence.
Functional coverage: UC-207

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Web no-show confirmation modal with selected session, scheduled time, reason/evidence, policy note, Cancel and confirm CTA.
2. Processing/success and audit state.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 189 — `CB-189` — Pregnancy Exercise List

**Platform:** Content Admin Web Portal  
**Primary role:** Current CareBridge actor  
**Functional coverage:** UC-185

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-189`
Platform: Content Admin Web Portal
Primary role: Current CareBridge actor
Feature: Pregnancy Exercise Management
Reference screen name: “Pregnancy Exercise List”
Purpose: Lists pregnancy exercises with trimester, difficulty, status and posture-analysis filters.
Functional coverage: UC-185

Canvas and navigation: WEB. Use the signed-in portal account area and preserve the current role sidebar; account screens should not invent a new navigation system.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Content-admin exercise list with thumbnail placeholder, title, trimester, difficulty, duration, posture-analysis mode, publication status, version, and last updated.
2. Filters/search and create CTA; table/card actions view/edit/preview; no published medical claim in preview text.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 190 — `CB-190` — Pregnancy Exercise Detail

**Platform:** Content Admin Web Portal  
**Primary role:** Current CareBridge actor  
**Functional coverage:** UC-185

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-190`
Platform: Content Admin Web Portal
Primary role: Current CareBridge actor
Feature: Pregnancy Exercise Management
Reference screen name: “Pregnancy Exercise Detail”
Purpose: Shows exercise instructions, duration, safety notes, eligibility metadata and publication state.
Functional coverage: UC-185

Canvas and navigation: WEB. Use the signed-in portal account area and preserve the current role sidebar; account screens should not invent a new navigation system.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Exercise detail with approved instructions, steps, duration, trimester/difficulty, safety/eligibility notes, posture-config link, source/version/status.
2. Edit/preview action and audit metadata; no auto-prescription logic.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 191 — `CB-191` — Create Pregnancy Exercise

**Platform:** Content Admin Web Portal  
**Primary role:** Current CareBridge actor  
**Functional coverage:** UC-185

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-191`
Platform: Content Admin Web Portal
Primary role: Current CareBridge actor
Feature: Pregnancy Exercise Management
Reference screen name: “Create Pregnancy Exercise”
Purpose: Creates a pregnancy exercise with instructions, trimester, difficulty, duration and safety guidance.
Functional coverage: UC-185

Canvas and navigation: WEB. Use the signed-in portal account area and preserve the current role sidebar; account screens should not invent a new navigation system.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Create-exercise form: title, short description, step instructions, trimester eligibility, difficulty, duration, safety/contraindication guidance, posture mode/config link, sources, status.
2. Save draft/preview/submit controls; validation for required fields; no uncontrolled direct publish.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 192 — `CB-192` — Edit Pregnancy Exercise

**Platform:** Content Admin Web Portal  
**Primary role:** Current CareBridge actor  
**Functional coverage:** UC-185

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-192`
Platform: Content Admin Web Portal
Primary role: Current CareBridge actor
Feature: Pregnancy Exercise Management
Reference screen name: “Edit Pregnancy Exercise”
Purpose: Updates an existing exercise without adding medical auto-prescription or video-storage behavior.
Functional coverage: UC-185

Canvas and navigation: WEB. Use the signed-in portal account area and preserve the current role sidebar; account screens should not invent a new navigation system.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Prefilled edit-exercise form with same controlled fields, change summary, current version/status, and submit update.
2. Unsaved changes warning and preview; preserve prior versions.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 193 — `CB-193` — Exercise Preview

**Platform:** Content Admin Web Portal  
**Primary role:** Current CareBridge actor  
**Functional coverage:** UC-185

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-193`
Platform: Content Admin Web Portal
Primary role: Current CareBridge actor
Feature: Pregnancy Exercise Management
Reference screen name: “Exercise Preview”
Purpose: Shows the mobile presentation of an exercise before activation.
Functional coverage: UC-185

Canvas and navigation: WEB. Use the signed-in portal account area and preserve the current role sidebar; account screens should not invent a new navigation system.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. End-user mobile-style exercise preview with title, duration, steps, safety pre-check prompt, and selected feedback mode labels.
2. No admin fields/editor controls; back-to-edit CTA only.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 194 — `CB-194` — Content Category List

**Platform:** Content Admin Web Portal  
**Primary role:** Current CareBridge actor  
**Functional coverage:** UC-226

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-194`
Platform: Content Admin Web Portal
Primary role: Current CareBridge actor
Feature: Verified Content
Reference screen name: “Content Category List”
Purpose: Lists, orders and manages active or hidden verified-content categories.
Functional coverage: UC-226

Canvas and navigation: WEB. Use the signed-in portal account area and preserve the current role sidebar; account screens should not invent a new navigation system.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Content-category table with name, parent, type, display order, active status, content count, last update.
2. Create/edit/reorder controls; delete disabled when linked content exists or requires clear migration/confirmation.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 195 — `CB-195` — Unpublish Content Confirmation

**Platform:** Content Admin Web Portal  
**Primary role:** Current CareBridge actor  
**Functional coverage:** UC-227

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-195`
Platform: Content Admin Web Portal
Primary role: Current CareBridge actor
Feature: Verified Content
Reference screen name: “Unpublish Content Confirmation”
Purpose: Confirms removal of an approved content version from user visibility while retaining version history.
Functional coverage: UC-227

Canvas and navigation: WEB. Use the signed-in portal account area and preserve the current role sidebar; account screens should not invent a new navigation system.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Unpublish confirmation with selected content title/type/version, current public impact, reason, optional replacement link, Cancel and unpublish CTA.
2. Content remains in version history/audit; no destructive hard delete.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 196 — `CB-196` — Posture Analysis Configuration List

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-186

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-196`
Platform: Admin Web Portal
Primary role: System Admin
Feature: Pregnancy Exercise Management
Reference screen name: “Posture Analysis Configuration List”
Purpose: Lists rule/model configurations, versions, status and linked exercises.
Functional coverage: UC-186

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Posture-analysis configuration list with linked exercise, analysis mode, rule/model version, confidence threshold, feedback level, effective dates, status, and last update.
2. Filters/search; create/edit/open detail; no individual user recording data.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 197 — `CB-197` — Posture Analysis Configuration Detail

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-186

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-197`
Platform: Admin Web Portal
Primary role: System Admin
Feature: Pregnancy Exercise Management
Reference screen name: “Posture Analysis Configuration Detail”
Purpose: Shows analysis mode, version, thresholds, feedback severity and linked exercise scope.
Functional coverage: UC-186

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Configuration detail with linked exercise, analysis mode, version, threshold, feedback rules, safety guardrails, effective period, status, and audit metadata.
2. Edit action and test-preview/validation; never render internal sensitive model secrets.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 198 — `CB-198` — Edit Posture Analysis Configuration

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-186

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-198`
Platform: Admin Web Portal
Primary role: System Admin
Feature: Pregnancy Exercise Management
Reference screen name: “Edit Posture Analysis Configuration”
Purpose: Updates posture-analysis rules, model version and confidence thresholds under controlled versioning.
Functional coverage: UC-186

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Edit configuration form with controlled fields: exercise, mode, rule/model version, confidence threshold, feedback severity/text mappings, effective dates, active status, change reason.
2. Validation for overlapping effective periods/invalid threshold; save creates auditable version.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy and scope: display only role-authorized information. Use realistic but fictional Vietnamese placeholder data. Do not add capabilities outside the stated scope. Any sensitive-data access must remain private by default and must visibly respect consent/expiry where applicable.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 199 — `CB-199` — Consultation No-show Review

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-207

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-199`
Platform: Admin Web Portal
Primary role: System Admin
Feature: Direct Consultation
Reference screen name: “Consultation No-show Review”
Purpose: Shows consultation timing and technical evidence used to review or confirm a no-show.
Functional coverage: UC-207

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Admin no-show review with session/booking facts, participant statements/notes, system timestamps, policy state, and decision panel.
2. Confirm/override/close actions require reason and audit note; no auto-refund without the refund workflow.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 200 — `CB-200` — Resolve Consultation Dispute

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-209

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-200`
Platform: Admin Web Portal
Primary role: System Admin
Feature: Direct Consultation
Reference screen name: “Resolve Consultation Dispute”
Purpose: Records the administrative dispute outcome and routes the approved financial decision.
Functional coverage: UC-209

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Dispute-resolution workspace with dispute summary, booking/payment snapshot, messages/evidence, policy references, timeline, assigned owner, and resolution options.
2. Options resolve, request information, reject, recommend refund; each requires a rationale and status transition.
3. No changes to original immutable booking price.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 201 — `CB-201` — Approve Refund Confirmation

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-210

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-201`
Platform: Admin Web Portal
Primary role: System Admin
Feature: Direct Consultation
Reference screen name: “Approve Refund Confirmation”
Purpose: Confirms the approved refund amount and transaction action.
Functional coverage: UC-210

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Approve-refund confirmation with dispute/transaction ID, refundable amount based on locked payment, reason, impact on commission/settlement, Cancel and approve CTA.
2. Explicit irreversible/processing state and audit reason.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 202 — `CB-202` — Reject Refund Confirmation

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-210

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-202`
Platform: Admin Web Portal
Primary role: System Admin
Feature: Direct Consultation
Reference screen name: “Reject Refund Confirmation”
Purpose: Confirms rejection of the refund request with a recorded reason.
Functional coverage: UC-210

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Reject-refund confirmation with dispute/transaction ID, reason field, policy summary, Cancel and reject CTA.
2. Rejection is status/audit action; does not erase the dispute.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately. Include the relevant permission, connection, expired-consent, or safe-fallback state without adding a separate frame.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 203 — `CB-203` — Expert Profile

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-81

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-203`
Platform: Mother Mobile App
Primary role: Mother
Feature: Expert Consultation & Pricing
Reference screen name: “Expert Profile”
Purpose: Shows the expert's verified identity, specialties, service channels, availability, reviews and entry point to the effective consultation pricing.
Functional coverage: UC-81

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Expert public profile with verified badge, specialty/title, approved scope, experience, organization, supported channels, availability, rating/review summary, and short bio.
2. Actions view pricing, choose consultation, and report profile; no personal contact/location outside approved visibility.
3. No diagnosis/sales claims beyond approved scope.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 204 — `CB-204` — Expert Consultation Pricing

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-241

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-204`
Platform: Mother Mobile App
Primary role: Mother
Feature: Expert Consultation & Pricing
Reference screen name: “Expert Consultation Pricing”
Purpose: Lists the expert's active chat, voice and video packages with duration, effective price, total payable estimate and cancellation/refund policy before booking.
Functional coverage: UC-241

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Pricing list for selected expert with packages by channel and duration; each row shows price, eligibility/availability, and “Chọn gói”.
2. Price is current public price only; show note that final booking locks a snapshot after slot/consent review.
3. No edit price on Mother screen.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 205 — `CB-205` — Booking Review

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-75, UC-241

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-205`
Platform: Mother Mobile App
Primary role: Mother
Feature: Expert Consultation & Pricing
Reference screen name: “Booking Review”
Purpose: Shows the selected expert, slot, shared-data consent and immutable booking price snapshot before final confirmation.
Functional coverage: UC-75, UC-241

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Booking-review screen with selected expert, date/slot, channel/duration, consent selector/summary (recipient/scope/purpose/expiry), and immutable booking price snapshot.
2. Optional short concern note, policy/cancellation link, confirm booking CTA; no payment method yet.
3. Validation for expired slot/consent and any missing mandatory consent.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 206 — `CB-206` — Payment

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-76, UC-126

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-206`
Platform: Mother Mobile App
Primary role: Mother
Feature: Expert Consultation & Pricing
Reference screen name: “Payment”
Purpose: Collects the payment method and charges the amount stored in the booking price snapshot rather than the expert's current public price.
Functional coverage: UC-76, UC-126

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Payment screen with booking ID, expert/channel, locked amount, chosen payment method, terms/receipt acknowledgement, and “Thanh toán” CTA.
2. No editable amount or re-priced display; handle payment pending/failure/retry without duplicate-charge wording.
3. Privacy-safe external payment handoff note.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 207 — `CB-207` — Payment Result

**Platform:** Mother Mobile App  
**Primary role:** Mother  
**Functional coverage:** UC-126

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-207`
Platform: Mother Mobile App
Primary role: Mother
Feature: Expert Consultation & Pricing
Reference screen name: “Payment Result”
Purpose: Shows transaction status, booking identifier, locked amount and safe retry or return actions.
Functional coverage: UC-126

Canvas and navigation: MOBILE. Use a contextual top app bar with clear back/close action. Do not render bottom navigation on this deep-detail, editor, session, map, payment, or confirmation flow.

Visual style: Create a high-fidelity mobile product UI, not a grayscale wireframe. Use a 390 × 844 portrait frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and rounded friendly sans typography. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Payment-result screen with clear success/pending/failed status, booking reference, locked amount, transaction reference masked appropriately, and next action.
2. Actions view booking, retry only when safe, or return; no success claim on pending/failed state.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete mobile screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 208 — `CB-208` — Consultation Pricing

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-238–UC-239

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-208`
Platform: Expert App
Primary role: Verified Expert
Feature: Consultation Pricing
Reference screen name: “Consultation Pricing”
Purpose: Lists the expert's active and inactive consultation packages and read-only price history by channel and duration.
Functional coverage: UC-238–UC-239

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Expert-app package list with channel, duration, active/inactive status, current public price, effective date, and read-only version/history indicator.
2. Filter/status tabs; add price CTA; row opens update/deactivate actions.
3. No existing booking snapshot is editable.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 209 — `CB-209` — Set Consultation Price

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-238

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-209`
Platform: Expert App
Primary role: Verified Expert
Feature: Consultation Pricing
Reference screen name: “Set Consultation Price”
Purpose: Creates the initial price for a supported channel and duration after validating it against the active CareBridge price band.
Functional coverage: UC-238

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Set-price form: channel, duration, value, applicable CareBridge min/max band, effective date, active status, and short public display summary.
2. Inline validation rejects outside-band values and missing effective date; save/submit CTA creates new price only.
3. No retrospective edit of confirmed/paid bookings.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 210 — `CB-210` — Update Consultation Price

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-239

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-210`
Platform: Expert App
Primary role: Verified Expert
Feature: Consultation Pricing
Reference screen name: “Update Consultation Price”
Purpose: Creates a new future-effective price version while preserving price history and existing booking snapshots.
Functional coverage: UC-239

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Update-price form showing current version read-only, new price, active price band range, future effective date, change reason, and version history preview.
2. Validate future effective date and range; update creates a new version rather than overwriting current history.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 211 — `CB-211` — Deactivate Consultation Price Confirmation

**Platform:** Expert App  
**Primary role:** Verified Expert  
**Functional coverage:** UC-239

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-211`
Platform: Expert App
Primary role: Verified Expert
Feature: Consultation Pricing
Reference screen name: “Deactivate Consultation Price Confirmation”
Purpose: Confirms stopping new bookings for a selected package without changing existing confirmed or paid bookings.
Functional coverage: UC-239

Canvas and navigation: MOBILE ONLY. Render exactly one 390 × 844 portrait mobile phone screen with a 24 px safe margin. HARD PLATFORM LOCK: This is the Verified Expert **mobile app**, not a web portal. Never create a desktop browser canvas, browser chrome, left/right sidebar, wide data table, 2–3 column workspace, or 1440 × 1024 layout. Use a compact top app bar with a clear back/close action. Do not show bottom navigation on this detail, form, setup, verification, map, realtime session, protected-file, or confirmation screen.

Visual style: Create a high-fidelity native mobile product UI, not a grayscale wireframe and not a web dashboard. Use the 390 × 844 portrait phone frame with a 24 px safe margin. The app canvas is #F6F1EC; use floating white/#F2EAE4 surfaces, 24–32 px card radii, warm soft shadows, #C98C7B primary accents, #5A463F primary text, #9C857C secondary text, and a rounded friendly sans font. Buttons, chips, badges, and toggles are pill-shaped with ≥48 px touch targets. Prefer stacked cards, compact sheets, and list rows that fit naturally on a phone. Keep copy short and action-first. Do not put the technical screen name anywhere in the visible app UI; use a natural contextual header instead. Use tasteful Lucide-style icons, never emoji in headings.

Required components and fields:
1. Deactivate-package confirmation with selected channel/duration, current price, impact on new bookings only, effective moment, Cancel and deactivate CTA.
2. Explicitly state existing confirmed/paid bookings keep their locked obligations.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete **mobile phone app** screen/frame in a 390 × 844 portrait frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Do not render a browser, desktop portal, sidebar, wide table, or 1440 × 1024 canvas. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 212 — `CB-212` — Consultation Pricing

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-238–UC-239

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-212`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Consultation Pricing
Reference screen name: “Consultation Pricing”
Purpose: Provides detailed package management, status filters and price-version history for the signed-in expert.
Functional coverage: UC-238–UC-239

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Web package-management table with channel, duration, current price, band status, active/inactive state, future version, updated date, and history.
2. Filters/search; create/update/deactivate actions; read-only booking impact note.
3. Use a structured B2B form drawer for editing.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 213 — `CB-213` — Set Consultation Price

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-238

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-213`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Consultation Pricing
Reference screen name: “Set Consultation Price”
Purpose: Creates a package price within the active minimum and maximum for the selected channel and duration.
Functional coverage: UC-238

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Web set-price drawer/page with channel, duration, band min/max, entered price, effective date, status, and public preview.
2. Validation blocks out-of-band price; save creates auditable version.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 214 — `CB-214` — Update Consultation Price

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-239

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-214`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Consultation Pricing
Reference screen name: “Update Consultation Price”
Purpose: Schedules a new effective price for future bookings and retains the previous version for audit and reconciliation.
Functional coverage: UC-239

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Web update-price drawer/page with existing version history, new value, band range, future effective date, reason, and submit CTA.
2. No overwrite/reprice of past or already-booked consultations.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 215 — `CB-215` — Deactivate Consultation Price Confirmation

**Platform:** Expert Web Portal  
**Primary role:** Verified Expert  
**Functional coverage:** UC-239

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-215`
Platform: Expert Web Portal
Primary role: Verified Expert
Feature: Consultation Pricing
Reference screen name: “Deactivate Consultation Price Confirmation”
Purpose: Confirms deactivation of a package for future bookings while preserving existing booking obligations.
Functional coverage: UC-239

Canvas and navigation: WEB. Use Expert Portal sidebar: Tổng quan · Lịch tư vấn · Lịch làm việc · Cộng đồng · Doanh thu · Hồ sơ. Keep the active item obvious.
HARD PLATFORM LOCK: This is the Verified Expert **desktop web portal**. Render one 1440 × 1024 browser/workspace screen with the Expert Portal sidebar. Never create a phone frame, mobile bottom navigation, or a mobile-only stacked consumer layout.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Web confirmation dialog for package deactivation with package details, effective moment, new-booking impact only, cancel and confirm actions.
2. Existing bookings explicitly preserved.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 216 — `CB-216` — Consultation Price Bands

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-240

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-216`
Platform: Admin Web Portal
Primary role: System Admin
Feature: Consultation Pricing & Commission
Reference screen name: “Consultation Price Bands”
Purpose: Lists active, draft, inactive and historical price-band versions by channel, duration, limits, commission rate and effective period.
Functional coverage: UC-240

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Admin price-band table with channel, duration, minimum, maximum, commission rate, status, effective period, version, and last editor.
2. Filters for status/channel/duration/history; create/configure action; no change to locked booking prices.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 217 — `CB-217` — Configure Consultation Price Band

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-240

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-217`
Platform: Admin Web Portal
Primary role: System Admin
Feature: Consultation Pricing & Commission
Reference screen name: “Configure Consultation Price Band”
Purpose: Creates or updates a versioned minimum, maximum, commission rate and effective period for a consultation channel and duration.
Functional coverage: UC-240

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Price-band configuration form: channel, duration, min price, max price, commission rate, effective-from/to, status, version/change reason.
2. Validation for min≤max, valid commission, no conflicting effective periods; save creates versioned policy.
3. No retroactive recalculate button.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 218 — `CB-218` — Deactivate Price Band Confirmation

**Platform:** Admin Web Portal  
**Primary role:** System Admin  
**Functional coverage:** UC-240

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-218`
Platform: Admin Web Portal
Primary role: System Admin
Feature: Consultation Pricing & Commission
Reference screen name: “Deactivate Price Band Confirmation”
Purpose: Confirms deactivation of a price band for new price changes without retroactively changing locked bookings.
Functional coverage: UC-240

Canvas and navigation: WEB. Use System Admin sidebar: Tổng quan · Người dùng · Xác thực · Kiểm duyệt · Đối tác · An toàn · Thanh toán · Audit.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Deactivate-price-band confirmation with band details, effective point, impact on new price changes only, Cancel and deactivate CTA.
2. Explicitly says existing price versions/bookings remain unchanged.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 219 — `CB-219` — Notification Center

**Platform:** Shared Web Portals  
**Primary role:** Current signed-in portal user  
**Functional coverage:** UC-11–UC-12

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-219`
Platform: Shared Web Portals
Primary role: Current signed-in portal user
Feature: Account & privacy
Reference screen name: “Notification Center”
Purpose: Lists operational, moderation, account, and partner notifications according to the signed-in portal role.
Functional coverage: UC-11–UC-12

Canvas and navigation: WEB. Use the signed-in portal account area and preserve the current role sidebar; account screens should not invent a new navigation system.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Portal notification-center table/list with role-aware categories, unread state, time, related resource, and open/mark-read actions.
2. Filters all/unread/type; no sensitive health content in message preview.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 220 — `CB-220` — Privacy Settings

**Platform:** Shared Web Portals  
**Primary role:** Current signed-in portal user  
**Functional coverage:** UC-17–UC-19, UC-157

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-220`
Platform: Shared Web Portals
Primary role: Current signed-in portal user
Feature: Account & privacy
Reference screen name: “Privacy Settings”
Purpose: Lets portal users manage account privacy and security preferences available to their role.
Functional coverage: UC-17–UC-19, UC-157

Canvas and navigation: WEB. Use the signed-in portal account area and preserve the current role sidebar; account screens should not invent a new navigation system.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Portal privacy settings sections for account/public profile, notification privacy, session/security, and data-sharing controls where role supports them.
2. Consent list has recipient, scope, purpose, expiry, status, and revoke action; setting changes show impact text.
3. No configuration may expose private health data by default.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 221 — `CB-221` — Delete Account Confirmation

**Platform:** Shared Web Portals  
**Primary role:** Current signed-in portal user  
**Functional coverage:** UC-156

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-221`
Platform: Shared Web Portals
Primary role: Current signed-in portal user
Feature: Account & privacy
Reference screen name: “Delete Account Confirmation”
Purpose: Confirms permanent account deletion for portal roles where deletion is allowed.
Functional coverage: UC-156

Canvas and navigation: WEB. Use the signed-in portal account area and preserve the current role sidebar; account screens should not invent a new navigation system.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Portal delete-account confirmation modal with role/account context, impact/data-retention note, acknowledgement, optional reason, Cancel/delete CTA.
2. Deletion availability respects role/policy; show processing/sign-out state.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

### Prompt 222 — `CB-222` — Deactivate Account Confirmation

**Platform:** Shared Web Portals  
**Primary role:** Current signed-in portal user  
**Functional coverage:** UC-15

```text
Create exactly one complete CareBridge screen mockup.

Screen ID: `CB-222`
Platform: Shared Web Portals
Primary role: Current signed-in portal user
Feature: Account & privacy
Reference screen name: “Deactivate Account Confirmation”
Purpose: Confirms temporary account deactivation for portal roles where deactivation is allowed.
Functional coverage: UC-15

Canvas and navigation: WEB. Use the signed-in portal account area and preserve the current role sidebar; account screens should not invent a new navigation system.

Visual style: Create a high-fidelity white-primary B2B web portal, not a grayscale wireframe. Use a 1440 × 1024 desktop frame with 24 px outer whitespace. The dominant background/surfaces are white; use #F6F1EC and #F2EAE4 only as very light warm accents. Use floating rounded sidebars/panels (24–32 px radii), restrained warm shadows, #C98C7B for primary CTA/status emphasis, #5A463F primary text, #9C857C secondary text, and a crisp readable sans font. Keep tables easy to scan, toolbars sticky where useful, and controls ≥44–48 px. The portal must feel modern, calm, and operational—not childish, dense, or overly decorative.

Required components and fields:
1. Portal deactivate-account confirmation modal with access limitation, role/organization impact, optional reason, reactivation information, Cancel/deactivate CTA.
2. No accidental deletion; processing/sign-out state.

Required interaction and state design: render the populated state as the primary state. Include visible primary action, back/close navigation, and only relevant filter/search/tab/menu interactions. Show field-level validation for forms; include a compact loading state for submit/data fetch and a clear empty/no-result state for lists where relevant. Use text + icon for statuses, never color alone. This is a destructive or high-impact action: render its confirmation modal/bottom sheet within the same frame, with a clear consequence, Cancel, and explicit confirm CTA; do not execute immediately.

Privacy, safety, and scope: show only data authorized for this role. When this screen displays health data, files, location, a care-group context, or a consultation, visibly show the recipient/owner context plus consent scope, purpose, expiry, and an access-expired/denied state where relevant. Label user-entered, device-sourced, expert-provided, and system-generated data when relevant. Never present AI output, trends, or posture feedback as a diagnosis, prescription, treatment, or emergency replacement. Use realistic but fictional Vietnamese placeholder data only. Do not add capabilities outside the stated scope.

Output rule: Produce exactly one complete web screen/frame, not a user-flow diagram and not multiple alternative screens. Do not create or redesign other frames. Keep labels in natural Vietnamese; use short, readable UI copy. Do not include unnecessary onboarding text, long instructions, or a visible technical screen title.
```

## Final coverage audit prompt

```text
Audit the current CareBridge UI System project against the 222 screen IDs CB-001 through CB-222. Do not redesign frames that already satisfy their prompt. Produce one audit-only frame listing: missing screen IDs, missing required fields/actions/states, incorrect role/RBAC visibility, missing consent scope/purpose/expiry, missing safety fallback, lifecycle errors for consultation/payment/pricing, and navigation gaps. Do not invent any feature or screen outside the existing prompt pack.
```

## Traceability note

Each prompt lists the primary CareBridge use cases it represents. A single screen can represent multiple use cases through in-frame actions, sheets, validation, and lifecycle states; the prompt always tells Stitch to keep those states in the same frame rather than generating uncontrolled extra screens.


---

## v3 Expert Platform Audit

- **Expert App prompt count:** 31. Every Expert App prompt must contain `MOBILE ONLY`, `390 × 844`, and `mobile phone app`; none may contain a `Canvas and navigation: WEB` line or a 1440 × 1024 requirement.
- **Expert Web Portal prompt count:** 22. Every Expert Web prompt must contain `WEB` and `1440 × 1024`; none may request a phone frame or Expert App bottom navigation.
- **Pricing correction:** Expert package-management prompts cover `UC-238` and `UC-239`. `UC-241` belongs to the end-user/mobile price-view screen before booking, so it is no longer assigned to Expert App/Web package management.
- **Existing Stitch repair:** for any Expert App frame generated from v2, use this exact instruction before redesigning it:

```text
Revise only frame [FRAME_ID]. This is the CareBridge Verified Expert MOBILE APP, not the web portal. Replace the current layout with exactly one 390 × 844 portrait phone screen. Remove browser chrome, sidebar, wide table, desktop multi-column layout, and all 1440 × 1024 styling. Keep every required field and action from the source prompt, but render it as a Warm Claymorphism mobile app with a compact app bar or Expert App bottom navigation only when this is a main signed-in destination.
```
