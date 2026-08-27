# CareBridge SYSTEM_ADMIN User Governance — Refinement Overview

## Completed

- Simplified `/admin/users` row actions to visible lock/unlock and enable/disable controls only.
- Removed the separate detail icon, post-mutation success banner, and abbreviated user ID display; row-click detail navigation remains available.
- Corrected role labels so `MOTHER` displays as `Mẹ` and `FAMILY` as `Gia đình` in filters and rows.
- Restricted UC116 role/permission management to staff-governance accounts and destinations only: `MODERATOR`, `CONTENT_ADMIN`, and `SYSTEM_ADMIN`.
- Added authoritative backend validation before any mutation or audit. Non-staff targets return `IAM-116-006`; non-staff destinations return `IAM-116-007`.
- Mirrored the rule in the frontend: non-staff detail pages do not expose role management, direct role-page access is disabled, and the selector contains only the three staff roles.
- Updated UC116 TDS and Test-Spec with the corrected business scope and rejection cases.
- No database table, column, dependency, or Flyway migration was added.

## Verification

- Backend focused role tests: 11 passed.
- Frontend focused tests: 10 passed across user list, detail, and role-update pages.
- Frontend production build: passed.
- Frontend lint: zero errors; one unrelated existing hook-dependency warning remains in `CreateContentPage.tsx`.
- Docker backend and web images rebuilt successfully; both containers are healthy.
- Backend database readiness is `UP`; `/admin/users` returns HTTP 200.
- Final GitNexus working-tree analysis: low risk, 0 affected execution processes.

## Remaining notes

- Browser automation could not run because its executable is unavailable in this environment; authenticated visual interaction remains the only unexecuted check.
- Existing Zego direct-eval, large chunk, ineffective dynamic-import, Lombok, and Mockito warnings are unrelated to this change.
- `CLAUDE.md` and other unrelated pre-existing workspace changes were preserved.
