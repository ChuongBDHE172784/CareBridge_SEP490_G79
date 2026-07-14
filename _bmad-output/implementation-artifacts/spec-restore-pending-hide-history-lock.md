---
title: 'Restore pending hide and safe history lock'
type: 'bugfix'
created: '2026-07-14'
status: 'done'
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The pending moderation queue lost the `Ẩn` action for new questions and answers after an over-correction of lock placement. A lock action must not be available for pending content, but must remain possible for an already approved question after it is processed.

**Approach:** Restore `Ẩn` with a required reason in both pending tabs. Offer `Khóa thảo luận` only from an APPROVE history record for a question, verify its live state in the UI, and conditionally transition `APPROVED` to `LOCKED` in one database operation.

## Boundaries & Constraints

**Always:** Preserve existing moderation RBAC, action audit logging, required reasons, and the rule that answers do not support locking.

**Ask First:** Any new moderation action, workflow state, schema change, or broader redesign of the queue.

**Never:** Show lock on a PENDING item, lock an answer, use stale UI data as the sole authorization check, or create an audit action when the lock transition did not occur.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|----------------------------|----------------|
| Pending hide | PENDING question or answer, reason supplied | Content is hidden and removed from its queue tab | Keep dialog open with error on request failure |
| History lock | APPROVE history record for a currently APPROVED question | Question atomically becomes LOCKED and a LOCK action is recorded | Reload history after success |
| Stale lock request | Question is no longer APPROVED | No lock action is recorded | UI shows status error; backend returns `MOD-031` conflict |

</frozen-after-approval>

## Code Map

- `../../05_Development/CareBridgeWebApp/src/features/moderation/pages/PendingContentQueuePage.tsx#L15` -- pending actions, history lock entry point, and confirmation dialogs.
- `../../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/repository/CommunityQuestionRepository.java#L81` -- atomic approved-to-locked update.
- `../../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/service/ModerationServiceImpl.java#L334` -- lock workflow and audit sequencing.
- `../../05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/moderation/ModerationServiceImplTest.java#L436` -- success and rejected-lock coverage.
- `../../04_Implement/UC99_PendingContentQueue/UC99_PendingContentQueue_TDS.md#L340` -- current moderation UI contract.

## Tasks & Acceptance

**Execution:**
- [x] `05_Development/CareBridgeWebApp/src/features/moderation/pages/PendingContentQueuePage.tsx` -- restore pending Hide and constrain Lock to eligible processed question history -- align actions with content lifecycle.
- [x] `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/repository/CommunityQuestionRepository.java` and `.../ModerationServiceImpl.java` -- make locking conditional on current APPROVED state -- prevent competing moderators from recording duplicate locks.
- [x] `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/moderation/ModerationServiceImplTest.java` -- cover accepted and rejected conditional locks -- protect audit consistency.
- [x] `04_Implement/UC99_PendingContentQueue/` -- update TDS and test-spec clarification -- preserve implementation contract.

**Acceptance Criteria:**
- Given a pending question or answer, when a moderator opens its actions, then Duyệt, Ẩn, and Yêu cầu sửa are available and Ẩn requires a reason.
- Given an approved-question history record, when the question is still APPROVED, then the moderator can lock its discussion from Đã xử lý.
- Given a lock request whose question is not APPROVED, when it reaches the service, then it returns `MOD-031` and persists no LOCK action.

## Spec Change Log

- 2026-07-14: Review identified stale-state and concurrent-lock risks; added UI detail refresh plus conditional database transition, including timestamp update.

## Verification

**Commands:**
- `cd 05_Development/CareBridgeAPI && ./mvnw test -Dtest=ModerationServiceImplTest` -- expected: all focused moderation service tests pass.
- `cd 05_Development/CareBridgeWebApp && npm run build` -- expected: TypeScript production build passes when unrelated Expert-page diagnostics are resolved.
