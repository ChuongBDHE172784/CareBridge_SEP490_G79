# Handoff Plan — continue ChuongBD branch (written by Claude, for Codex)

Context: branch `ChuongBD` in this monorepo (backend `CareBridgeAPI`, mobile `CareBridgeMobileApp`).
Read `CLAUDE.md` at repo root first — package-by-domain, DTO/mapper rules, RBAC/consent/audit
enforcement for health/location/safety flows, Flyway migration rules, and dual-remote git rules in
`.claude/rules/git-dual-remote.md` all still apply.

## What's already done (do NOT redo)
- AI Symptom Triage flow (intake -> Gemini triage -> risk result screen), fully wired in
  `app_router.dart` (`/triage/intake`, `/triage/result/:sessionId`).
- Emergency SOS + fall-detection: `emergency_map_screen.dart`, `emergency_alert_detail_screen.dart`,
  `safety/` feature (accelerometer/gyroscope streaming to `/api/v1/safety/imu-data`), all routed and
  wired.
- FCM push end-to-end: `lib/core/notifications/fcm_service.dart`, backend
  `FirebaseConfig`/`FirebaseFcmServiceImpl` gated behind `carebridge.fcm.enabled`.
- Backend for Care Group invitations: `InviteCareGroupMemberRequest`, `PendingInvitationDto`,
  `CareGroupController`/`ICareGroupService`/`CareGroupServiceImpl` - compiles clean, verified against
  repositories.
- `SuspectedFallDetectedHandler` - auto-registered via `@EventListener`/component scan, no manual
  wiring needed.

## Remaining work, in priority order

### 1. Fix encoding bug (quick, do first)
`lib/features/safety/screens/safety_monitoring_screen.dart` line 92 has mojibake (double-encoded
UTF-8) Vietnamese text: `'Vui lÃ²ng báº­t phÃ¡t hiá»‡n ngÃ£ trÆ°á»›c'`. It must read
`'Vui lòng bật phát hiện ngã trước'` ("Please enable fall detection first"). Note line 285 in the same
file already has correctly-encoded Vietnamese ("Hệ thống ghi nhận gia tốc..."), so this is an isolated
copy-paste/encoding slip, not a project-wide encoding problem. Search the file for other
similarly corrupted strings (likely saved with wrong encoding at some point) and fix all
occurrences. Save as UTF-8.

### 2. Build mobile UI for Care Group Invitation (CB-024, UC-83)
Backend is ready and currently unused by the mobile app:
- POST invite endpoint backed by `InviteCareGroupMemberRequest`
- Pending invitations exposed via `PendingInvitationDto`
- Accept/decline logic in `CareGroupServiceImpl`

Need: a Flutter screen (or section in an existing care-group screen) to:
- List pending invitations for the current user.
- Invite a new member by email/phone (whichever the DTO expects - check
  `InviteCareGroupMemberRequest` fields before building the form).
- Accept / decline an invitation.
- Update `03_Design/UI_UX/screen_usecase_tracking.md` CB-024 row from "Not Started" to reflect
  progress once implemented.

Follow existing service pattern (see `emergency_service.dart` or `triage_service.dart` for the
http-client/DTO-mapping convention already used in this codebase) - add a
`care_group_service.dart` under `lib/features/family/` (check actual folder name in
`lib/features/` first, don't assume).

### 3. CB-018 - Expert Directory link
On the Risk Triage Result screen, the "see an expert" action is currently a TODO/snackbar
placeholder. Needs a real navigation target - check whether an Expert Directory screen already
exists elsewhere in `lib/features/` (search before building a new one); if it doesn't exist yet,
this may be out of scope for now - confirm with the user before building a whole new expert
directory feature, since that could be a bigger separate epic.

### 4. Mock data still pending real endpoints
- UC-63 nearest-facility search on `emergency_map_screen.dart` is mocked - needs a real backend
  endpoint + TrackAsia integration (per `CLAUDE.md`, TrackAsia is currently stubbed project-wide,
  so check with the user whether this is in scope now or blocked on TrackAsia integration work).
- UC-139 safety event history list is mock data - needs a real backend query once/if prioritized.

### 5. Config/secrets check before any commit
- `android/app/google-services.json` is untracked - confirm with the user whether it's a real
  Firebase config (containing real project keys) or a placeholder. If real, verify `.gitignore`
  actually excludes it (don't commit real secrets) or confirm it's intentionally meant to be
  committed for this project's setup.
- Confirm `.env.example` documents whatever FCM/Firebase env vars are needed, and that a real
  `.env` locally has them set for testing.

## Verification before considering any of the above "done"
- `flutter test` for mobile changes.
- `./mvnw test` for backend changes (none expected unless care-group mobile work requires backend
  tweaks).
- Manually click through the new/changed screen(s) - this repo's convention (see CLAUDE.md) is to
  actually run the app and test the golden path, not just rely on compiler/tests.
- Do not commit/push without following `.claude/rules/git-dual-remote.md` (pull both remotes on
  `dev` first, never push directly to `dev`, work stays on `ChuongBD`).

## Not found to be broken
Investigation found no compile-risk issues in the already-modified/new files (DTOs, repositories,
adapters all cross-check cleanly against callers). The only concrete defect found is the encoding
bug in item 1.
