# Story: Frontend web portal foundation

**Story ID**: STORY-004  
**Status**: DRAFT  
**Epic**: EPIC-001-foundation  
**Actor**: Frontend developer team  
**Goal**: Set up the React + Vite + TypeScript project with routing, layout, shared components, and authentication pages to create a functional admin/partner portal foundation  
**Context**: The backend will provide authentication endpoints (STORY-002). The frontend web portal is primarily for administrators, moderators, content managers, experts, and partners (not for mothers—that's the mobile app). This story establishes the Vite project structure, routing, state management, API client, and core UI components needed for login, dashboard layout, and placeholder pages.

---

## Requirements

### 4.1 Project Structure

Ensure `04_SourceCode/CareBridgeWebApp/` follows this structure:

```
src/
├── main.tsx                              (React entry, providers setup)
├── App.tsx                               (Router setup with routes)
├── index.css                             (global styles, Tailwind imports)
├── vite-env.d.ts
├── app/
│   ├── router/
│   │   ├── routes.ts                    (route definitions and guards)
│   │   └── ProtectedRoute.tsx           (HOC or wrapper for role-based access)
│   ├── providers/
│   │   ├── AuthProvider.tsx             (React Context for auth state)
│   │   ├── ThemeProvider.tsx            (Tailwind theme)
│   │   └── I18nProvider.tsx             (intl context)
│   ├── layouts/
│   │   ├── AdminLayout.tsx              (sidebar, header, main content area)
│   │   ├── AuthLayout.tsx               (centered card for login)
│   │   └── PartnerLayout.tsx            (similar but different branding)
│   └── guards/
│       ├── RoleBasedRoute.tsx           (checks user role before rendering)
│       └── AuthGuard.tsx                (redirects to login if not authenticated)
├── shared/
│   ├── api/
│   │   ├── api_client.ts                (axios instance with interceptors)
│   │   ├── api_response.ts              (wrapper with data, error, message)
│   │   ├── endpoints.ts                 (API endpoint constants)
│   │   └── error_handler.ts             (centralized error display)
│   ├── auth/
│   │   ├── auth_context.tsx             (AuthContext: user, login, logout)
│   │   ├── use_auth.ts                  (custom hook)
│   │   ├── auth_service.ts              (login, logout, refresh token)
│   │   └── models/
│   │       ├── user.ts
│   │       ├── login_request.ts
│   │       └── auth_response.ts
│   ├── components/
│   │   ├── ui/
│   │   │   ├── button/
│   │   │   │   └── Button.tsx
│   │   │   ├── input/
│   │   │   │   └── Input.tsx
│   │   │   ├── modal/
│   │   │   │   └── Modal.tsx
│   │   │   ├── spinner/
│   │   │   │   └── Spinner.tsx
│   │   │   └── banner/
│   │   │       └── Banner.tsx
│   │   ├── layout/
│   │   │   ├── header/
│   │   │   │   └── Header.tsx
│   │   │   ├── sidebar/
│   │   │   │   └── Sidebar.tsx
│   │   │   └── footer/
│   │   │       └── Footer.tsx
│   │   └── common/
│   │       ├── avatar/
│   │       │   └── Avatar.tsx
│   │       ├── empty_state/
│   │       │   └── EmptyState.tsx
│   │       └── confirm_dialog/
│   │           └── ConfirmDialog.tsx
│   ├── forms/
│   │   ├── login_form.tsx
│   │   └── common_field.tsx (reusable form field with label, error)
│   ├── tables/
│   │   ├── data_table.tsx
│   │   └── table_pagination.tsx
│   ├── hooks/
│   │   ├── use_query.ts                (React Query wrapper)
│   │   ├── use_mutation.ts
│   │   └── use_permissions.ts
│   ├── utils/
│   │   ├── format.ts                   (date, phone, currency)
│   │   ├── validation.ts               (regex, required)
│   │   └── constants.ts                (app constants, role names)
│   └── constants/
│       ├── routes.ts                   (route path constants)
│       └── roles.ts                    (role constant strings)
└── features/
    ├── auth/
    │   ├── pages/
    │   │   ├── LoginPage.tsx
    │   │   └── VerifyOtpPage.tsx
    │   ├── components/
    │   │   └── OtpInput.tsx
    │   └── services/
    │       └── auth_api_service.ts
    ├── dashboard/
    │   ├── pages/
    │   │   └── DashboardPage.tsx
    │   ├── components/
    │   │   ├── stats_card.tsx
    │   │   └── recent_activity.tsx
    │   └── services/
    │       └── dashboard_service.ts
    ├── user-management/
    │   ├── pages/
    │   │   └── UserListPage.tsx
    │   ├── components/
    │   │   └── UserTable.tsx
    │   └── services/
    │       └── user_service.ts
    ├── expert-verification/
    │   ├── pages/
    │   │   └── ExpertVerificationPage.tsx
    │   └── components/
    │       └── verification_card.tsx
    ├── moderation/
    │   └── pages/
    │       └── ModerationQueuePage.tsx
    ├── content-management/
    │   └── pages/
    │       └── ContentListPage.tsx
    ├── consultation-management/
    │   └── pages/
    │       └── ConsultationListPage.tsx
    ├── payment-refunds/
    │   └── pages/
    │       └── PaymentListPage.tsx
    ├── audit-security/
    │   └── pages/
    │       └── AuditLogPage.tsx
    ├── ai-rule-management/
    │   └── pages/
    │       └── TriageRulesPage.tsx
    └── posture-configuration/
        └── pages/
            └── PostureConfigPage.tsx
```

### 4.2 Dependencies

`package.json` should include:

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "@tanstack/react-query": "^5.0.0",
    "axios": "^1.6.0",
    "zustand": "^4.4.0",
    "react-hook-form": "^7.48.0",
    "@hookform/resolvers": "^3.3.0",
    "zod": "^3.22.0",
    "dayjs": "^1.11.0",
    "lucide-react": "^0.294.0",
    "tailwindcss": "^3.3.0",
    "autoprefixer": "^10.4.0",
    "postcss": "^8.4.0"
  },
  "devDependencies": {
    "@types/react": "^18.2.0",
    "@types/react-dom": "^18.2.0",
    "@vitejs/plugin-react": "^4.2.0",
    "typescript": "^5.3.0",
    "vite": "^5.0.0",
    "eslint": "^8.55.0",
    "@typescript-eslint/eslint-plugin": "^6.13.0",
    "@typescript-eslint/parser": "^6.13.0"
  }
}
```

### 4.3 API Client (Axios)

Create `shared/api/api_client.ts`:

```typescript
import axios from 'axios';
import { storage } from './secure_storage'; // wrapper for localStorage/sessionStorage

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
  timeout: 10000,
});

apiClient.interceptors.request.use(async (config) => {
  const token = await storage.get('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      // logout, redirect to login
      window.location.href = '/login';
    }
    const message = error.response?.data?.message || error.message;
    return Promise.reject({ status: error.response?.status, message });
  }
);

export default apiClient;
```

### 4.4 Authentication Flow

1. **Login Page** (`/login`): Phone number input → POST to `/auth/login` → store OTP (temporary) → navigate to OTP verification.
2. **OTP Verification** (`/verify-otp`): Enter 6-digit code → POST `/auth/verify-otp` → on success, store tokens in localStorage (or better, use `sessionStorage`) → redirect to dashboard.
3. **Dashboard** (`/dashboard`): Protected route; requires auth. Shows welcome message, role-specific quick links (admin → user management, moderation; expert → question queue; partner → partner profile).
4. **Logout**: POST `/auth/logout`, clear storage, redirect to `/login`.

### 4.5 Routing and Guards

Use React Router v6 with route guards:

```tsx
// app/router/routes.tsx
const routes = [
  {
    path: '/login',
    element: <AuthLayout><LoginPage /></AuthLayout>,
    public: true,
  },
  {
    path: '/verify-otp',
    element: <AuthLayout><VerifyOtpPage /></AuthLayout>,
    public: true,
  },
  {
    path: '/dashboard',
    element: <ProtectedRoute allowedRoles={['ADMIN','MODERATOR','CONTENT_ADMIN','EXPERT','PARTNER']}>
      <AdminLayout><DashboardPage /></AdminLayout>
    </ProtectedRoute>,
  },
  // other protected routes...
];
```

`ProtectedRoute` checks auth context; if not authenticated, redirects to `/login`. If authenticated but role not allowed, shows "Forbidden" or redirects to appropriate dashboard.

### 4.6 State Management

- **React Query**: Server state (API data fetching). Configure `QueryClientProvider` in `main.tsx`.
- **Zustand**: Global UI state (theme, sidebar collapsed, notifications). Optional for MVP; can use React Context instead.
- **React Hook Form + Zod**: Form state and validation.

### 4.7 UI Components

Build reusable components in `shared/components/ui/`:

- `Button`: primary, secondary, danger, loading states
- `Input`: text, password, phone with label and error display
- `Modal`: confirm dialogs
- `Spinner`: loading indicator
- `Banner`: success/error/info alerts
- `DataTable`: generic table with pagination (using TanStack Table if needed)
- `EmptyState`: when no data to show

Use Tailwind CSS for styling. Configure Tailwind in `tailwind.config.js` with CareBridge color palette.

### 4.8 Layouts

- **AuthLayout**: Centered card on neutral background, logo on top.
- **AdminLayout**: Sidebar (collapsible) with navigation links, top header with user menu (profile, logout), main content area.
- **PartnerLayout**: Similar but with partner-specific branding.

---

## Acceptance Criteria

**Scenario 1**: Login page renders and submits credentials
Given a user navigates to `/login`
When they enter a phone number and click "Send OTP"
Then a POST request is made to `/api/v1/auth/login` with `{ "phone": "0987654321" }`
And the browser navigates to `/verify-otp`

**Scenario 2**: OTP verification succeeds and redirects to dashboard
Given the user is on `/verify-otp`
When they enter `123456` and click "Verify"
Then a POST request is made to `/api/v1/auth/verify-otp` with `{ "phone": "...", "otp": "123456" }`
On success, access token and user profile are stored
And the browser navigates to `/dashboard`

**Scenario 3**: Dashboard displays after login
Given the user is authenticated
When they visit `/dashboard`
Then they see a welcome message with their name
And a sidebar with navigation links appropriate for their role
And a logout button in the header

**Scenario 4**: Protected routes require authentication
Given an unauthenticated user
When they try to access `/dashboard` directly
Then they are redirected to `/login`
After login, they are redirected back to `/dashboard`

**Scenario 5**: Role-based access control works
Given a user with role `MODERATOR`
When they are on the dashboard
Then they see a link to "Moderation Queue"
But they do NOT see a link to "User Management" (admin-only)

**Scenario 6**: API errors display user-friendly messages
Given the user enters an invalid OTP
When they click Verify
Then a red error banner appears: "Invalid OTP. Please try again."
And they remain on the OTP page

**Scenario 7**: UI follows responsive design
Given the admin portal is viewed on a desktop (≥1024px)
When the sidebar is collapsed
Then main content area expands to use full width
And on mobile (≤768px), the sidebar is hidden behind a hamburger menu

---

## Files Expected to be Touched

**New files** (non-exhaustive):

```
04_SourceCode/CareBridgeWebApp/
├── public/ (existing)
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── index.css
│   ├── app/
│   │   ├── router/
│   │   │   ├── routes.tsx
│   │   │   └── ProtectedRoute.tsx
│   │   ├── providers/
│   │   │   ├── AuthProvider.tsx
│   │   │   ├── ThemeProvider.tsx
│   │   │   └── I18nProvider.tsx
│   │   ├── layouts/
│   │   │   ├── AdminLayout.tsx
│   │   │   ├── AuthLayout.tsx
│   │   │   └── PartnerLayout.tsx
│   │   └── guards/
│   │       ├── RoleBasedRoute.tsx
│   │       └── AuthGuard.tsx
│   ├── shared/
│   │   ├── api/
│   │   │   ├── api_client.ts
│   │   │   ├── api_response.ts
│   │   │   ├── endpoints.ts
│   │   │   └── error_handler.ts
│   │   ├── auth/
│   │   │   ├── auth_context.tsx
│   │   │   ├── use_auth.ts
│   │   │   ├── auth_service.ts
│   │   │   └── models/ (user, login_request, auth_response)
│   │   ├── components/ui/ (Button, Input, Modal, Spinner, Banner, etc.)
│   │   ├── components/layout/ (Header, Sidebar, Footer)
│   │   ├── forms/
│   │   │   └── login_form.tsx
│   │   ├── hooks/
│   │   │   ├── use_query.ts
│   │   │   └── use_permissions.ts
│   │   ├── utils/
│   │   │   ├── format.ts
│   │   │   ├── validation.ts
│   │   │   └── constants.ts
│   │   └── constants/
│   │       ├── routes.ts
│   │       └── roles.ts
│   └── features/
│       ├── auth/pages/ (LoginPage, VerifyOtpPage)
│       ├── auth/components/ (OtpInput)
│       ├── auth/services/auth_api_service.ts
│       ├── dashboard/pages/DashboardPage.tsx
│       ├── user-management/pages/UserListPage.tsx
│       ├── moderation/pages/ModerationQueuePage.tsx
│       └── ... (other feature pages stubs)
├── tailwind.config.js
├── postcss.config.js
├── tsconfig.json
├── tsconfig.node.json
├── vite.config.ts
├── eslint.config.js
└── package.json (may need to add/adjust dependencies)
```

**Modified files**:
- `index.html` (update title, favicon)
- `vite.config.ts` (configure proxy if needed, path aliases)
- `tsconfig.json` (path mappings for `@/*` to `src/`)

---

## Validation Approach

- **Unit tests** (Jest + Testing Library):
  - `AuthContext.test.tsx`: login, logout state changes
  - `auth_service.test.ts`: API calls with correct endpoints
  - `components/ui/Button.test.tsx`: renders correctly, handles click
- **Component tests**:
  - `LoginPage.test.tsx`: phone input, submit button, error states
  - `DashboardPage.test.tsx`: shows user name, role-specific links
- **Integration test** (Cypress or Playwright if setup allows, else manual):
  - `auth_flow.spec.ts`: visit `/login` → enter phone → submit → OTP page → enter 123456 → verify → redirect to dashboard → logout → back to login
- **Manual smoke test**:
  - `npm run dev` → Vite server starts
  - Open browser to `http://localhost:5173`
  - Test login flow with backend (STORY-005 running)
  - Verify responsive layout at different screen sizes

---

## Dependencies

- **STORY-002**: Backend auth API must be implemented. For early development, can mock `auth_service.ts` to return dummy tokens and user profile.
- **STORY-005**: Consent settings page will be implemented later; stub a placeholder link in the sidebar for now.

---

## Notes

- **Styling**: Use Tailwind CSS for rapid UI development. Configure primary colors to match CareBridge brand (consider soft colors appropriate for maternal health).
- **State Management**: React Query is highly recommended for server state (data fetching, caching, mutations). Zustand can be used for UI state but is optional.
- **Forms**: Use React Hook Form with Zod for validation. Keep validation rules consistent with backend (e.g., Vietnamese phone format: 10 digits starting with `0`).
- **Role-Based UI**: Use the `useAuth` hook to get current user's role, then conditionally render menu items.
- **Error Handling**: Global error boundary at the top level (`App.tsx`) to catch unhandled errors. Use the `error_handler` utility for API errors.
- **i18n**: For MVP, all strings can be in English. Vietnamese localization can be added later if needed. Use a simple context or `IntlProvider`.
- **Environment**: Use `.env` file for `VITE_API_URL`. Default to `http://localhost:8080`.
- **Security**: Store tokens in `localStorage` for MVP simplicity; consider `sessionStorage` or more secure storage if security review demands. In production, consider httpOnly cookies (requires CORS config).
- **Proxy**: If facing CORS issues during development, configure Vite proxy in `vite.config.ts` to forward `/api` to backend.
- **Testing**: If testing is new to the frontend team, prioritize at least unit tests for services and components. Integration tests are valuable but can be added later.
- **Accessibility**: Use semantic HTML, proper labels, and ARIA attributes where needed. This is important for an admin portal.

---

**Story End**
