---
title: 'Expose real moderator account-violation history'
type: 'bugfix'
created: '2026-07-14'
status: 'done'
baseline_commit: '66ba823ea45bc27df39921b81994c02bba893d72'
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `/moderator/violations` is a placeholder even though WARN, SUSPEND, RESTRICT, and ESCALATE actions are already persisted in `moderation_actions`. The existing `/history` endpoint explicitly excludes `ACCOUNT` actions, so moderators cannot review real enforcement history.

**Approach:** Add a moderator-only, paginated read contract for account moderation actions and have the violations page consume it, presenting action, affected account, moderator, reason, duration, current status, and the recorded time without seeded or fixed UI data.

## Boundaries & Constraints

**Always:** Keep controller logic limited to validation/mapping; enforce `MODERATOR` RBAC; return DTOs only; preserve append-only moderation actions and existing audit behavior; page results newest-first; respect the existing maximum page size of 50; avoid health/private-profile data.

**Ask First:** Adding a schema migration, a dependency, exporting files, changing enforcement actions, or exposing fields beyond the stored moderation-action/user display identity.

**Never:** Reuse the content-only `/history` contract for account data, mutate historical actions, show fabricated entries, or add direct enforcement controls to the history screen.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Account history exists | Moderator requests page 0 with valid size | Returns newest ACCOUNT WARN/SUSPEND/RESTRICT/ESCALATE actions with account and moderator display names, reason, expiry, report reference, and time | Missing/deleted related user renders a safe fallback name rather than failing the page |
| No account actions | Moderator requests history with no matching rows | API returns an empty page; UI renders truthful no-recorded-violations state | No static sample rows |
| Unauthorized or invalid pagination | Non-moderator requests route, or `size > 50` | Non-moderator is denied; oversized request is rejected with current MOD-002 convention | UI shows a load failure and retry affordance; no stale sample data |
| Expiring enforcement | Action has no expiry, future expiry, or elapsed expiry | UI labels warnings/escalations as non-expiring and restrictions/suspensions as active or expired based on `expiresAt` | Null expiry is never treated as an active temporary restriction |

</frozen-after-approval>

## Code Map

- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ModerationController.java` -- moderator-only REST routes and pagination guards.
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/service/ModerationService.java` and `ModerationServiceImpl.java` -- account-action read workflow, RBAC context, batch name projection.
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/repository/ModerationActionRepository.java` -- paged ACCOUNT-action query.
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/dto/response/` -- dedicated account-violation page/item DTOs.
- `05_Development/CareBridgeWebApp/src/features/moderation/{services/moderationApi.ts,models/moderation.ts,pages/ViolationHistoryPage.tsx}` -- typed API client and real loading/error/empty/table states.
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/moderation/` -- controller/service and authorization coverage.

## Tasks & Acceptance

**Execution:**
- [x] Backend response/repository/service/controller -- add a distinct paginated account-violation history endpoint that queries only ACCOUNT actions, batch-resolves safe account/moderator display names, preserves descending `actionAt` order, and retains the established page-size/RBAC protections.
- [x] Backend tests -- prove scope, newest-first paging, name fallback, empty result, size guard, and MODERATOR-only access.
- [x] Web moderation API/models -- add typed request/response definitions for the new endpoint.
- [x] Violation history page -- replace the placeholder with loading, error/retry, empty, paged table, and non-actionable status presentation using only returned data.

**Acceptance Criteria:**
- Given account enforcement actions are stored, when a Moderator opens `/moderator/violations`, then the page displays the returned history and never the old backend-unavailable notice.
- Given a suspension or restriction has elapsed, when it is displayed, then it is visibly marked expired; a future expiry is marked active.
- Given a user lacks the MODERATOR role, when requesting the history endpoint, then the API returns 403.

## Spec Change Log

## Design Notes

The endpoint is intentionally separate from `GET /api/v1/admin/moderation/history`: that endpoint is a content moderation audit stream for QUESTION/ANSWER only. The new response should make `targetUserId` and `reportId` available for traceability, but the initial page remains a read-only global history list because the current route carries no account identifier.

## Verification

**Commands:**
- `cd 05_Development/CareBridgeAPI && ./mvnw test -Dtest=ModerationServiceImplTest,ModerationControllerTest,ModerationControllerSecurityTest` -- expected: account-history and existing moderation tests pass.
- `cd 05_Development/CareBridgeWebApp && npm run build` -- expected: TypeScript/Vite build succeeds.

**Manual checks (if no CLI):**
- Log in as `moderator@carebridge.dev`, open `/moderator/violations`, and verify real persisted account actions render; verify the no-recorded-violations state only when the API returns an empty page.

## Suggested Review Order

**Moderator API boundary**

- Introduces the restricted read endpoint with familiar pagination validation.
  [ModerationController.java:105](../../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ModerationController.java#L105)

- Queries only account enforcement events and projects safe identity fallbacks.
  [ModerationServiceImpl.java:207](../../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/service/ModerationServiceImpl.java#L207)

**Moderator experience**

- Replaces the unavailable-backend placeholder with guarded real-data loading and states.
  [ViolationHistoryPage.tsx:20](../../05_Development/CareBridgeWebApp/src/features/moderation/pages/ViolationHistoryPage.tsx#L20)

**Coverage**

- Pins deterministic pagination order and deleted-account fallback behavior.
  [ModerationServiceImplTest.java:398](../../05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/moderation/ModerationServiceImplTest.java#L398)
