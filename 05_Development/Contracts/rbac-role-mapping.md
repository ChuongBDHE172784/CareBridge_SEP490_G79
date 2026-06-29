---
title: CareBridge RBAC Role Mapping
date: 2026-06-25
status: Authoritative
source: com.carebridge.backend.security.rbac.Role enum (V1 schema)
---

# CareBridge RBAC Role Mapping

This document is the **single authoritative reference** for role names across the backend, web portal, and Flutter app. All authorization checks, route guards, and seed data must use these exact values.

> Note: Earlier documentation drafts (CLAUDE.md, some design docs) used stale names `ADMIN` and `PARTNER_REPRESENTATIVE`. Those are incorrect and must not be used.

---

## 1. Authoritative Role List

| Role value (enum / DB) | Spring authority | Domain label | Who holds it |
|---|---|---|---|
| `MOTHER` | `ROLE_MOTHER` | Pregnant/postpartum user | Self-registration (`/auth/register` with `role: MOTHER` or null) |
| `FAMILY` | `ROLE_FAMILY` | Family member / support person | Self-registration (`role: FAMILY`) |
| `EXPERT` | `ROLE_EXPERT` | Healthcare professional (pending verification) | Self-registration (`role: EXPERT`); verified separately by admin |
| `MODERATOR` | `ROLE_MODERATOR` | Community and content moderator | Seeded by system admin; cannot self-register |
| `CONTENT_ADMIN` | `ROLE_CONTENT_ADMIN` | Content and article administrator | Seeded by system admin; cannot self-register |
| `SYSTEM_ADMIN` | `ROLE_SYSTEM_ADMIN` | Platform administrator | Seeded at deployment; cannot self-register |
| `PARTNER` | `ROLE_PARTNER` | Partner organization representative | Seeded by system admin; cannot self-register |

Self-registration is restricted by `AuthenticationPolicy.resolveSelfRegistrationRole()`: only `MOTHER`, `FAMILY`, and `EXPERT` are permitted via `/auth/register`.

---

## 2. Source of Truth

**Backend enum**: `com.carebridge.backend.security.rbac.Role`
```
MOTHER, FAMILY, EXPERT, MODERATOR, CONTENT_ADMIN, SYSTEM_ADMIN, PARTNER
```

**Spring authority prefix**: All roles are stored without the `ROLE_` prefix in the database `users.role` column and in JWT claims. Spring Security adds `ROLE_` automatically when calling `hasRole('X')`.

**JWT claim**: The access token contains `"role": "MOTHER"` (no prefix). `JwtTokenProvider` maps this to `ROLE_MOTHER` for Spring Security.

**Database**: `users.role` column stores the enum value directly (e.g., `MOTHER`). The `roles` and `user_roles` tables exist in the V1 schema for future RBAC expansion but are not yet used by the authorization layer.

---

## 3. Current Backend Authorization by Endpoint

| Endpoint | Method | Required role(s) |
|----------|--------|-----------------|
| `/api/v1/admin/audit-logs` | GET | `SYSTEM_ADMIN` |
| `/api/v1/admin/moderation/queue` | GET | `MODERATOR` |
| `/api/v1/admin/content` | POST | `CONTENT_ADMIN` |
| `/api/v1/community/topics` | POST | `MODERATOR` |
| `/api/v1/community/topics/**` | PATCH | `MODERATOR` |
| `/api/v1/partner/profile` | POST | `PARTNER` |
| `/api/v1/rag/answer` | POST | `MOTHER`, `FAMILY`, `EXPERT`, `MODERATOR`, `CONTENT_ADMIN`, `SYSTEM_ADMIN` |
| `/api/v1/community/questions` (create) | POST | `MOTHER` |
| All `/api/v1/auth/**` | — | Public (no auth) |
| All other `/api/v1/**` | — | Authenticated (any role) |

---

## 4. Web Portal Route Guards (planned)

| Route prefix | Required role |
|---|---|
| `/admin` | `SYSTEM_ADMIN` |
| `/moderator` | `MODERATOR` |
| `/content` | `CONTENT_ADMIN` |
| `/expert` | `EXPERT` |
| `/partner` | `PARTNER` |

---

## 5. Flutter Route Guards (planned)

Flutter uses `SecureStorage` to persist the role value returned in the JWT response. Route guard reads the stored role:
- Maternal dashboard: `MOTHER`, `FAMILY`
- Expert queue: `EXPERT`
- Community: all authenticated roles

---

## 6. Stale Names to Remove

The following role names appear in older documentation and **must not be used**:

| Stale name | Correct name |
|---|---|
| `ADMIN` | `SYSTEM_ADMIN` |
| `PARTNER_REPRESENTATIVE` | `PARTNER` |

Update any documentation or code that still uses these stale names.
