# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-144D RTC Extension — Direct Consult 1:1 Voice and Video

**Document ID:** `CB-CHAT-RTC-TDD-144D-EXT`  
**Version:** `0.3`  
**Date:** `2026-07-16`  
**Status:** `Implemented — Automated GREEN; Manual RTC Pending`  
**Standard:** `ISO/IEC/IEEE 29119-3:2021`  
**Author:** `Codex — Test Architect`  
**Reviewed by:** `[x] User`  
**DPO Sign-off:** `[ ] Pending`  
**Approved by:** `[x] User — 2026-07-16`  
**Classification:** `Internal — Confidential`

**References:**

- `04_Implement/UC144D_DirectConsultRTC/UC144D_DirectConsultRTC_TDS.md`
- `04_Implement/UC144_DirectConsultChat/UC144_DirectConsultChat_Test-Spec.md`
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260715120000__create_direct_conversation_schema.sql`
- `05_Development/firebase/firestore.rules`
- `_bmad-output/planning-artifacts/research/technical-zegocloud-direct-consult-rtc-flutter-web-research-2026-07-16.md`

> This document specifies tests before production implementation. Required order after approval: RED → GREEN → REFACTOR. No case is marked passing until its command has actually run successfully.

---

## CHANGELOG

| Date | Author | Change |
|---|---|---|
| 2026-07-16 | Codex — Test Architect | Initial Draft covering backend, Flutter, Web, Firebase Rules, and manual real-device/browser RTC matrices |
| 2026-07-16 | User + Codex | Approved for implementation. Room-ID privilege authentication moved from blocking gate to optional hardening; backend-generated JWT-user-bound Token04 remains mandatory. |
| 2026-07-16 | Codex — Implementation | Recorded RED→GREEN evidence and actual automated/emulator results. Flutter uses the approved Express fallback after Prebuilt UIKit failed the AGP 9/ZIM native-build gate. Manual real-media cases remain pending. |

---

## TABLE OF CONTENTS

1. Module Information
2. Logic Issues Resolved
3. Test Design Specification
4. Test Case Specification
5. Red-Green-Refactor Tracker
6. Entry / Exit Criteria
7. Rollback Plan
8. CASE 2.0 Anti-Pattern Detection

---

## 1. Module Information

| Field | Value |
|---|---|
| **Feature / Gap ID** | `GAP-DIRECT-CONSULT-RTC` |
| **Module** | `directchat + Flutter/Web RTC adapters` |
| **Source Spec** | `CB-CHAT-RTC-IMP-144D-EXT` |
| **Priority** | `P0` |
| **Sprint** | `Unscheduled — implementation begins only after approval` |
| **Milestone** | `Real Direct Consult RTC` |
| **Data Classification** | `Sensitive-PII metadata; transient media` |
| **Compliance Scope** | `PDPA, RBAC, secret management, audit integrity` |
| **Upstream Dependencies** | UC-144D lifecycle/signaling, ZEGOCLOUD project, Firebase Auth/Firestore |
| **Downstream Consumers** | Flutter Android/iOS, React Web Mother/Expert, PostgreSQL timeline |

### 1.1 AI Generation Context

| Field | Value |
|---|---|
| **AI Assisted?** | Yes |
| **Constraint Source** | TDS §2, §3, §9, §16, §17 |
| **Constraints Injected** | PostgreSQL authority, five-field Firestore, JWT-bound room credentials, no ZIM, join-after-answer, revoke cleanup exception, no false manual completion |
| **Model** | Codex GPT-5 |
| **Trust Level** | `T2 → T3 pending Red Gate` |

---

## 2. Logic Issues Resolved

| # | Existing behavior / old assumption | Required behavior encoded by tests |
|---|---|---|
| L1 | UC-144D was signaling-only | Keep it historically correct; add a separate RTC extension |
| L2 | Caller token generated before `INITIATED` persistence | Initiate persists lifecycle without invoking ZEGOCLOUD |
| L3 | Callee token generated inside `answer` | Answer persists first; each participant separately requests own credentials |
| L4 | Generic call response contains room/token fields | Durable call response contains no temporary credentials |
| L5 | Token04 payload is empty | Production token is JWT-user-bound; room privilege is added only if supported by the current generator without a broad compatibility change |
| L6 | Decline only accepts `RINGING` | Callee may decline `INITIATED` or `RINGING` |
| L7 | Revoked Expert participant check can block answered cleanup | Identity participant may end `ANSWERED`; no new/renewed credentials |
| L8 | Firestore listeners are chat-screen-scoped | One authenticated-session signal hub feeds chat and call coordinator |
| L9 | Web Mother has no portal route | Minimal Mother Expert-directory/profile and Direct Consult route group only |
| L10 | Current clients show fake call success | Call UI reflects REST state and real official ZEGOCLOUD media adapters |
| L11 | Web project has no unit test runner | Add Vitest for reducer/coordinator/adapter tests |
| L12 | App process loss may leave `ANSWERED` | Reconcile and offer resume/end; never fabricate duration |

---

## 3. Test Design Specification

### TDS-01 — Scope

```text
Backend
├── Call service/state policy
├── Repository conditional updates
├── Call detail and active-call queries
├── Join-credentials security
├── Token04 user identity binding and optional room privilege payload
├── AFTER_COMMIT Firestore behavior
└── PostgreSQL integration

Flutter
├── Pure call reducer/coordinator
├── REST and signal synchronization
├── RTC adapter lifecycle
├── Permission/error mapping
└── Widget overlay/screen ownership

Web
├── Pure reducer/coordinator
├── REST and signal synchronization
├── UIKit adapter cleanup/renewal
├── browser permission/autoplay/device errors
├── stale route/conversation guards
└── Mother Direct Consult routing

Firebase
└── Existing owner-only Rules regression
```

### TDS-02 — Test Basis

| Source | Derived Items |
|---|---|
| User-approved workflow request dated 2026-07-16 | Required lifecycle, security, client UI, test matrix, DoD |
| TDS `BR-RTC-*` | Expected architecture and behavior |
| Existing Flyway migration | Database fields and constraints |
| Existing UC-144D `BR-DCC-*` | Firestore payload, after-commit, timeout, timeline, revoke baseline |
| Official ZEGOCLOUD docs | production token, UIKit configuration, renewal, permissions, autoplay/cleanup |

### TDS-03 — Conditions and Coverage

| Condition | Coverage | Test Cases |
|---|---|---|
| Credential authorization and identity binding | Backend service/controller/token | `RTC-BE-001..010` |
| Lifecycle/race/history integrity | Backend repository/service/integration | `RTC-BE-011..024` |
| Flutter synchronization and lifecycle | reducer/coordinator/adapter/widgets | `RTC-FL-001..014` |
| Web synchronization and browser lifecycle | reducer/provider/adapter/router | `RTC-WEB-001..015` |
| Firestore security unchanged | Emulator Rules | `RTC-FS-001..004` |
| Real media interoperability | devices/browsers | `RTC-MAN-*` |

### TDS-04 — Techniques

| Technique | Application |
|---|---|
| State transition testing | Every valid/invalid call transition |
| Decision table | caller/callee, Mother/Expert, approved/revoked, state |
| Concurrency testing | answer versus timeout and duplicate terminal actions |
| Equivalence partitioning | participant/non-participant, correct/wrong call pair |
| Boundary testing | token expiry, dispose during async work, no device/permission |
| Fault injection | Firebase failure, ZEGOCLOUD token failure, listener/auth failure |
| Model-based reducer testing | duplicate/replayed/delayed/lost signals |
| Security testing | target-user substitution, room reuse, secret/log leakage |

### TDS-05 — Test Data

All data is `SYNTHETIC`.

| Fixture | Value / Logic | Purpose |
|---|---|---|
| `FX-RTC-M1` | approved Mother participant | Caller/callee paths |
| `FX-RTC-E1` | Expert `APPROVED` and eligible | writable/join paths |
| `FX-RTC-E-REVOKED` | same Expert changed to non-approved | revoke matrix |
| `FX-RTC-X1` | unrelated Mother/Expert | IDOR/non-participant |
| `FX-RTC-C1` | direct conversation M1 ↔ E1 | primary conversation |
| `FX-RTC-C2` | unrelated conversation | call/conversation mismatch |
| `FX-RTC-CALL-*` | one call per lifecycle state | transition partitions |
| `FX-ZEGO-SECRET` | 32-character test-only sentinel | deterministic Token04 tests |
| `FX-ZEGO-TOKEN` | `04_TEST_TOKEN_DO_NOT_LOG` | log redaction |
| `FX-CLOCK` | fixed instants | duration/race assertions |

### Props Isolation Boilerplate

Every test creates fresh entities/coordinator state through factories. No mutable coordinator, repository mock, stream controller, timer, or UIKit fake is shared across tests.

```java
static ConversationCall makeCall(CallStatus status) {
    return ConversationCall.builder()
            .id(UUID.randomUUID())
            .conversationId(UUID.randomUUID())
            .initiatedByUserId(UUID.randomUUID())
            .callType(CallType.VIDEO)
            .callStatus(status)
            .zegoRoomId("cb_" + UUID.randomUUID().toString().replace("-", ""))
            .initiatedAt(Instant.parse("2026-07-16T09:00:00Z"))
            .createdAt(Instant.parse("2026-07-16T09:00:00Z"))
            .build();
}
```

---

## 4. Test Case Specification

The case-level `🔴 Not written` labels below preserve the v0.2 pre-implementation
baseline and RED intent. Current execution status is authoritative in §5 and the
Executed Verification Record in §6; manual cases remain authoritative in §4.6.

### 4.1 Backend — Credentials and Security

#### RTC-BE-001 — Caller receives credentials only for self

**Severity:** CRITICAL  
**Feature:** `issueJoinCredentials`  
**Test File:** `ConversationCallServiceImplCredentialsTest.java`  
**Oracle:** `BR-RTC-006/007`, `ADR-RTC-003`

1. Seed `ANSWERED` call initiated by Mother.
2. Request credentials as Mother.
3. Assert token generator receives `u_<Mother UUID without hyphens>`.
4. Assert response has stored room, safe display name, expiry, and no ServerSecret.

**PASS:** Credentials are for the JWT Mother only.  
**FAIL:** Caller can choose another identity or receives secret material.  
**Status:** 🔴 Not written

#### RTC-BE-002 — Callee receives separate credentials only for self

Same arrangement as RTC-BE-001, request as Expert, and assert the Expert-derived ZEGOCLOUD user ID is used. The token must differ from the Mother's token while room ID is the same.

**Status:** 🔴 Not written

#### RTC-BE-003 — Credential endpoint has no target-user or room input

**Severity:** CRITICAL  
**Feature:** Controller contract/static test  
**Test File:** `ConversationCallControllerContractTest.java`

Assert the endpoint accepts only path IDs and authenticated `Principal`; request body/query parameters cannot specify `userId`, `targetUserId`, or `roomId`.

**Status:** 🔴 Not written

#### RTC-BE-004 — Non-participant credential request denied

Request credentials for `FX-RTC-C1` as `FX-RTC-X1`.

**PASS:** `403 DCC-003`; token generator not called; no audit issuance.  
**Status:** 🔴 Not written

#### RTC-BE-005 — Wrong conversation/call pair is hidden

Use a real call from `C1` under the `C2` path.

**PASS:** `404 DCC-006`; no token; no existence leak.  
**Status:** 🔴 Not written

#### RTC-BE-006 — Credentials require `ANSWERED`

Parameterized states: `INITIATED`, `RINGING`, `DECLINED`, `MISSED`, `CANCELLED`, `ENDED`.

**PASS:** each returns `409 DCC-007`; generator not called.  
**Status:** 🔴 Not written

#### RTC-BE-007 — Revoked Expert blocks new and renewed credentials

Matrix:

| Requester | Call state | Expected |
|---|---|---|
| Mother | `ANSWERED` | denied after Expert revoke |
| Revoked Expert | `ANSWERED` | denied |
| Either | pre-answer | denied |

`end` remains separately tested as allowed for answered cleanup.

**Status:** 🔴 Not written

#### RTC-BE-008 — Token is bound to ZEGOCLOUD-safe current user ID

Decode/test the Token04 plaintext before encryption through a generator seam or capture the JSON input.

**PASS:** `user_id = u_<JWT UUID without hyphens>`; length/character contract satisfied.  
**Status:** 🔴 Not written

#### RTC-BE-009 — Optional Token payload is bound to stored room

**Severity:** MEDIUM — optional hardening  
**Feature:** `ZegoToken04Generator`

Assert payload contains:

```json
{
  "room_id": "cb_<callIdWithoutHyphens>",
  "privilege": { "1": 1, "2": 1 },
  "stream_id_list": null
}
```

If the current generator does not support this payload, verify instead that the join-credentials endpoint accepts no caller-selected room and returns only the stored room. Keep the cross-room negative staged smoke test as follow-up after project-side room privilege validation is enabled.

**Status:** 🔴 Not written / optional hardening and manual staging portion pending

#### RTC-BE-010 — Token and secrets are absent from logs and durable responses

Use sentinel token/secret and capture logs for initiate, answer, detail, active list, credential error, and publisher failure.

**PASS:** token appears only in the successful credential response body; ServerSecret never appears; no token appears in logs/audit/Firestore.  
**Status:** 🔴 Not written

### 4.2 Backend — Lifecycle, Consistency, and History

#### RTC-BE-011 — Initiate persists without token generation

Force ZEGOCLOUD service to throw if called.

**PASS:** call is persisted `INITIATED`, room ID is generated, activity/audit/event occur; ZEGOCLOUD service is never called.  
**Status:** 🔴 Not written

#### RTC-BE-012 — Answer persists without token generation

**PASS:** conditional `RINGING -> ANSWERED` commits and publishes; no ZEGOCLOUD call occurs inside the transaction.  
**Status:** 🔴 Not written

#### RTC-BE-013 — Call detail is participant-only and durable-only

Assert participant gets all lifecycle timestamps and no credential/room fields. Non-participant gets `403`; mismatch gets `404`.

**Status:** 🔴 Not written

#### RTC-BE-014 — Active-call reconciliation returns authoritative non-terminal calls

Seed mixed states and conversations.

**PASS:** current participant gets only `INITIATED`, `RINGING`, `ANSWERED`, ordered newest first; no token/room fields; unrelated calls absent.  
**Status:** 🔴 Not written

#### RTC-BE-015 — `INITIATED -> RINGING` actor/state rules

Only callee succeeds; caller/non-participant denied; duplicate/wrong state returns `409`; success touches `last_activity_at`.

**Status:** 🔴 Not written

#### RTC-BE-016 — Decline accepts `INITIATED` and `RINGING`

Parameterized valid states for callee.

**PASS:** both become `DECLINED` with server `endedAt`; duplicate/answered decline returns `409`.  
**Status:** 🔴 Not written

#### RTC-BE-017 — Answer versus timeout has exactly one winner

Run concurrent conditional `answer` and `markMissed`.

**PASS:** final state is exactly one of `ANSWERED` or `MISSED`; one rows-affected result is `1`, the other `0`; only winner audits/publishes.  
**Status:** 🔴 Not written

#### RTC-BE-018 — Timeout covers `INITIATED` and `RINGING`

Both expired pre-answer states become `MISSED`; fresh calls remain unchanged; no client endpoint can set `MISSED`.

**Status:** 🔴 Not written

#### RTC-BE-019 — Caller cancel before answer

Caller can transition `INITIATED/RINGING -> CANCELLED`; callee cannot; revoke blocks pre-answer cancel according to new-activity policy; timeout remains fallback.

**Status:** 🔴 Not written

#### RTC-BE-020 — Server-computed duration

Given `answeredAt = 09:00:00` and backend clock `09:01:17`, end the call.

**PASS:** `durationSeconds = 77`; no client duration field exists; terminal retry returns `409`.  
**Status:** 🔴 Not written

#### RTC-BE-021 — Revoked Expert can end answered call for cleanup

Revoke Expert after `ANSWERED`, invoke `end` as that Expert.

**PASS:** `ENDED`, duration server-computed, activity/audit/event updated.  
**FAIL:** generic approved-participant policy blocks cleanup.  
**Status:** 🔴 Not written

#### RTC-BE-022 — Every successful transition touches activity and publishes after commit

Parameterized over ringing, answer, decline, cancel, end, missed.

**PASS:** `last_activity_at` updated; listener is `AFTER_COMMIT`; event recipient is counterpart; no sender echo.  
**Status:** 🔴 Not written

#### RTC-BE-023 — Firestore failure never rolls back call state

Make gateway throw after commit.

**PASS:** API/database transition remains committed; publisher logs metadata only; retry/reconciliation can recover.  
**Status:** 🔴 Not written

#### RTC-BE-024 — PostgreSQL integration preserves full call history

**Test File:** `DirectConsultRtcIntegrationTest.java`  
**Environment:** Testcontainers PostgreSQL if Docker available.

Execute voice/video calls ending as declined, missed, cancelled, and ended. Reopen timeline.

**PASS:** initiator, final state, timestamps, and ended duration are present independently of Firestore. No migration is added or applied beyond existing schema.  
**Status:** 🔴 Not written

### 4.3 Flutter

Planned test files:

- `test/features/directChat/calls/direct_call_reducer_test.dart`
- `test/features/directChat/calls/direct_call_coordinator_test.dart`
- `test/features/directChat/calls/direct_call_host_test.dart`
- `test/features/directChat/calls/rtc_permissions_test.dart`

#### RTC-FL-001 — State mapping

Map REST statuses into incoming/outgoing/ready/in-call/terminal projections for both initiator roles and both call types.

**Status:** 🔴 Not written

#### RTC-FL-002 — Signal arrives before initial active-call/timeline load

Queue the signal, complete initial REST load, and reconcile once.

**PASS:** call appears exactly once with newest REST state.  
**Status:** 🔴 Not written

#### RTC-FL-003 — Signal arrives while REST sync is running

Set pending-sync flag and verify one follow-up sync, no parallel request storm.

**Status:** 🔴 Not written

#### RTC-FL-004 — Duplicate/replayed/delayed signals

Emit repeated event IDs and distinct events for the same call in nonchronological order.

**PASS:** one overlay by `callId`; terminal REST state cannot regress to ringing.  
**Status:** 🔴 Not written

#### RTC-FL-005 — Lost signal recovered by active-call REST reconciliation

Start/resume without any Firestore event and return an incoming ringing call from REST.

**PASS:** incoming UI appears.  
**Status:** 🔴 Not written

#### RTC-FL-006 — Accept sequence marks ringing before answer when required

For an `INITIATED` incoming call, coordinator completes/reconciles ringing, then answers. A `409` from a competing transition causes detail refetch rather than duplicate UI.

**Status:** 🔴 Not written

#### RTC-FL-007 — Voice starts camera off

RTC adapter config asserts microphone on per UX, camera off, speaker/default route as specified, and no camera permission requested solely for voice.

**Status:** 🔴 Not written

#### RTC-FL-008 — Video controls and initial camera

Assert video config includes microphone, front camera, camera toggle, switch camera, speaker/audio route when supported, and hang-up.

**Status:** 🔴 Not written

#### RTC-FL-009 — Permission denied and no device

Simulate microphone denial, camera denial, and unavailable camera.

**PASS:** no join occurs; actionable UI shown; lifecycle is not falsely ended unless user chooses cancel/end.  
**Status:** 🔴 Not written

#### RTC-FL-010 — Dispose during credential request/join

Dispose host/screen before async credential or join completes.

**PASS:** no `setState after dispose`, no overlay resurrection, late credentials discarded, adapter disposed.  
**Status:** 🔴 Not written

#### RTC-FL-011 — No duplicate call overlay/screen

Repeated coordinator notifications and widget rebuilds produce one keyed route/overlay for the active call.

**Status:** 🔴 Not written

#### RTC-FL-012 — Background/resume reconciliation

Background retains answered state without fabricating end. Resume reconciles active state; terminal counterpart state closes media/UI.

**Status:** 🔴 Not written

#### RTC-FL-013 — Token expiry renews own credentials

Trigger documented token-expired callback.

**PASS:** fetch current-user credentials, call Express `renewToken`, ignore late result after disposal, and never log token.  
**Status:** 🔴 Not written

#### RTC-FL-014 — Logout disposes signal and RTC session

**PASS:** Firebase subscription, timers, coordinator streams, Express engine, and media resources are released.  
**Status:** 🔴 Not written

### 4.4 React Web

Planned test files:

- `src/features/directChat/calls/directCallState.test.ts`
- `src/features/directChat/calls/directCallCoordinator.test.ts`
- `src/features/directChat/calls/zegoRoomSession.test.ts`
- `src/features/directChat/calls/rtcMediaPermissions.test.ts`
- router authorization tests

Vitest version at Draft time: `4.1.10`; pin a compatible reviewed version during implementation.

#### RTC-WEB-001 — Reducer/state machine

Cover incoming, outgoing, answered, joining, in-call, reconnecting, and terminal states with no illegal regression.

**Status:** 🔴 Not written

#### RTC-WEB-002 — Conversation/tab switch rejects stale results

Start detail/credential requests for conversation A, switch to B, resolve A last.

**PASS:** A cannot mutate B, open an overlay, or join a room.  
**Status:** 🔴 Not written

#### RTC-WEB-003 — Duplicate signals do not duplicate overlay

Same call from Firestore and active REST list results in one provider state and one UIKit container.

**Status:** 🔴 Not written

#### RTC-WEB-004 — Initial auth/token/listener failure reconnects

Fail Firebase custom token, Firebase sign-in, and snapshot listener in separate partitions.

**PASS:** retry is bounded/deduplicated; REST active-call reconciliation still runs; provider does not crash.  
**Status:** 🔴 Not written

#### RTC-WEB-005 — Permission is requested only on accept/join

Render incoming/outgoing ringing UI without invoking media APIs. Invoke media only after explicit user action on an answered call.

**Status:** 🔴 Not written

#### RTC-WEB-006 — Permission denied/no camera/no microphone

Map browser/SDK failures to explicit UI. Voice may proceed without camera; no microphone blocks both voice/video media join.

**Status:** 🔴 Not written

#### RTC-WEB-007 — Autoplay restriction

Simulate autoplay failure.

**PASS:** user sees a “resume audio/video” action; coordinator remains in call; no duplicate room join.  
**Status:** 🔴 Not written

#### RTC-WEB-008 — Cleanup on unmount and route change

**PASS:** leave/destroy called once, listeners/timers removed, container cleared, no late callback mutates unmounted provider.  
**Status:** 🔴 Not written

#### RTC-WEB-009 — Tab refresh resumes through REST authority

Session hint contains only `callId`; reload fetches active call/detail/credentials. Stored room/token values are ignored/not present.

**Status:** 🔴 Not written

#### RTC-WEB-010 — Reconnect and short network loss

Connection callback projects `RECONNECTING` then `IN_CALL`; no database end is sent merely for disconnect.

**Status:** 🔴 Not written

#### RTC-WEB-011 — Token renewal or controlled rejoin

Before expiry, fetch own credentials. If adapter renewal succeeds, remain in room. If renewal fails/unavailable, destroy and recreate with fresh Kit Token and same authoritative answered call.

**Status:** 🔴 Not written

#### RTC-WEB-012 — Device change support

If pinned UIKit exposes stable input/output selection, verify device list refresh and selection. If unsupported by UIKit/browser, verify UI does not falsely advertise the control.

**Status:** 🔴 Not written

#### RTC-WEB-013 — Mother has the minimal Web Expert-to-Direct-Consult journey

Mother default route is `/mother/experts`. Approved Expert directory/profile pages work; “Chat” reuses the existing find-or-create endpoint and navigates to the conversation room; conversation list/room and call host work. Non-approved Experts cannot create new activity. Mother remains forbidden from Expert/Admin/Content/Partner routes.

**Status:** 🔴 Not written

#### RTC-WEB-014 — Client bundle contains no ServerSecret/AppSign

Build and scan output/env imports.

**PASS:** only AppID obtained from backend credential response is used; no ZEGOCLOUD secret variable exists in Vite env.  
**Status:** 🔴 Not written

#### RTC-WEB-015 — Chat signaling regression

Message signals still refresh PostgreSQL timeline through the shared signal hub; call signals do not render Firestore payload directly; no duplicate Firebase listener per authenticated session.

**Status:** 🔴 Not written

### 4.5 Firebase Emulator Rules

Reuse and retain existing Rules; add no RTC write permission.

| TC ID | Scenario | Expected | Status |
|---|---|---|---|
| `RTC-FS-001` | Owner reads own event | allowed | 🟢 EMULATOR GREEN |
| `RTC-FS-002` | Authenticated cross-user read | denied | 🟢 EMULATOR GREEN |
| `RTC-FS-003` | Unauthenticated read | denied | 🟢 EMULATOR GREEN |
| `RTC-FS-004` | Any client write, including token/room fields | denied | 🟢 EMULATOR GREEN |

The test payload remains exactly `eventId`, `eventType`, `conversationId`, `resourceId`, `occurredAt`.

### 4.6 Manual Real-Media E2E Matrix

All cases below are `MANUAL PENDING` until executed on real devices/browsers with real ZEGOCLOUD media. Emulator/widget/unit success does not satisfy them.

#### Client Pair and Media Matrix

| TC ID | Caller | Callee | Media | Expected | Status |
|---|---|---|---|---|---|
| `RTC-MAN-001` | Flutter Mother | Flutter Expert | Voice | two-way audio, camera off | ⏳ Pending |
| `RTC-MAN-002` | Flutter Mother | Flutter Expert | Video | two-way audio/video and controls | ⏳ Pending |
| `RTC-MAN-003` | Web Mother | Web Expert | Voice | two-way audio | ⏳ Pending |
| `RTC-MAN-004` | Web Mother | Web Expert | Video | two-way audio/video | ⏳ Pending |
| `RTC-MAN-005` | Flutter Mother | Web Expert | Voice | interoperable audio | ⏳ Pending |
| `RTC-MAN-006` | Flutter Mother | Web Expert | Video | interoperable audio/video | ⏳ Pending |
| `RTC-MAN-007` | Web Mother | Flutter Expert | Voice | interoperable audio | ⏳ Pending |
| `RTC-MAN-008` | Web Mother | Flutter Expert | Video | interoperable audio/video | ⏳ Pending |

Reverse initiation (Expert caller) must be sampled for every platform pair even when the table names Mother as caller.

#### Lifecycle and Failure Matrix

| TC ID | Scenario | Expected | Status |
|---|---|---|---|
| `RTC-MAN-009` | Decline from incoming UI | durable `DECLINED`, counterpart closes | ⏳ Pending |
| `RTC-MAN-010` | Missed timeout with no response | server `MISSED`, both reconcile | ⏳ Pending |
| `RTC-MAN-011` | Caller cancels before answer | durable `CANCELLED` | ⏳ Pending |
| `RTC-MAN-012` | Either side hangs up after answer | durable `ENDED`, server duration | ⏳ Pending |
| `RTC-MAN-013` | Expert revoked before new call | initiate denied both directions | ⏳ Pending |
| `RTC-MAN-014` | Expert revoked during answered call | no renewed token; explicit end cleanup works | ⏳ Pending |
| `RTC-MAN-015` | Browser mic permission denied | clear failure, no false join | ⏳ Pending |
| `RTC-MAN-016` | Browser camera denied for video | clear failure or audio-only fallback only if UX approves | ⏳ Pending |
| `RTC-MAN-017` | Mobile camera/mic denied | clear failure; app stable | ⏳ Pending |
| `RTC-MAN-018` | Short network loss | reconnecting UI, media resumes, history retained | ⏳ Pending |
| `RTC-MAN-019` | Web refresh during answered call | resume/end reconciliation | ⏳ Pending |
| `RTC-MAN-020` | Route change/unmount | camera/mic indicators turn off; no leaked tracks | ⏳ Pending |
| `RTC-MAN-021` | Bluetooth/headset/speaker routing | stable where supported | ⏳ Pending |
| `RTC-MAN-022` | Call exceeds original token TTL | renew/rejoin without secret exposure | ⏳ Pending |

#### Explicit Limitation Check

| TC ID | Scenario | Expected | Status |
|---|---|---|---|
| `RTC-MAN-023` | Flutter app killed before incoming call | No completion claim; incoming is not guaranteed without push/CallKit | ⏳ Documented limitation |

---

## 5. Red-Green-Refactor Tracker

| Group | Planned Test Files | RED | GREEN | REFACTOR |
|---|---|---|---|---|
| Backend credentials/security | call service and controller contract tests | `[x]` | `[x]` | `[x]` |
| Backend lifecycle/integration | call service and PostgreSQL integration tests | `[x]` | `[x]` | `[x]` |
| Flutter | reducer/coordinator/host/permission tests | `[x]` | `[x]` | `[x]` |
| Web | Vitest reducer/coordinator/adapter/permission tests | `[x]` | `[x]` | `[x]` |
| Firebase | existing emulator Rules test | N/A regression | `[x]` | N/A |
| Manual media | device/browser evidence | N/A | `[ ]` | N/A |

### 5.1 Red Gate Protocol

After approval:

1. Add tests against missing interfaces/stubs.
2. Run the narrow suites.
3. Capture evidence that every new behavior fails for the expected reason.
4. If a test passes against current signaling-only code, reject it as non-sensitive or tautological.
5. Implement only after the RED evidence is recorded.

Examples of required RED:

- current initiate calls ZEGOCLOUD, so `RTC-BE-011` must fail;
- current answer calls ZEGOCLOUD, so `RTC-BE-012` must fail;
- current decline rejects `INITIATED`, so `RTC-BE-016` must fail;
- current clients show fake dialog/alert, so real coordinator/adapter tests must fail or not compile;
- current Web Mother route test must fail.

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [x] TDS and this Test-Spec approved by explicit user reply `Approved`.
- [x] Worktree rechecked; unrelated user changes preserved.
- [x] ZEGOCLOUD AppID/ServerSecret configured only on backend.
- [x] Room privilege accepted as optional hardening; strict backend room authorization is the required gate.
- [x] Impact analysis run before production symbol edits.
- [x] No production implementation was added before RED evidence.

### Automated Exit Criteria

- [x] Backend focused tests GREEN.
- [x] Backend full relevant suite GREEN.
- [x] PostgreSQL integration GREEN with Docker/Testcontainers.
- [x] Flutter `flutter test` GREEN.
- [x] Flutter Android debug build GREEN.
- [!] Flutter analyzer BLOCKED by analysis-server LSP crash before diagnostics.
- [x] Web Vitest GREEN.
- [x] Web scoped RTC/direct-chat lint and Vite production bundle GREEN.
- [!] Full Web TypeScript build/lint has pre-existing unrelated Expert-page errors.
- [x] Firebase Emulator Rules GREEN.
- [x] No secret/token leakage in tracked client source, bundles, logs, Firestore, or durable responses.
- [x] Chat Firestore/timeline regression tests GREEN.
- [x] `detect_changes` and post-implementation impact analysis match expected scope; overall MEDIUM with no HIGH/CRITICAL symbol impact.

### Manual Exit Criteria

- [ ] Real-media matrices executed and evidence recorded.
- [ ] Android physical/emulator status explicitly separated.
- [ ] iOS physical/simulator status explicitly separated.
- [ ] Browser/device permission and autoplay cases executed.
- [ ] No case is labeled complete if it was not actually run.

### Reporting Vocabulary

Final implementation report must use only:

- `AUTOMATED GREEN`
- `EMULATOR GREEN`
- `MANUAL GREEN`
- `MANUAL PENDING`
- `BLOCKED`

It must not collapse these into a single “complete” statement.

### Executed Verification Record

| Command / Check | Result |
|---|---|
| Backend focused, contract, PostgreSQL integration and concurrency tests | **AUTOMATED GREEN — 33/33** |
| Flutter full suite | **AUTOMATED GREEN — 102/102** |
| Flutter permission policy | **AUTOMATED GREEN — 3/3** |
| Flutter Android debug build | **AUTOMATED GREEN** |
| Flutter analyzer | **BLOCKED — Flutter 3.44.1 analysis server exit 255 / malformed LSP stream** |
| Web Vitest | **AUTOMATED GREEN — 11/11** |
| Web scoped ESLint | **AUTOMATED GREEN** |
| Web Vite production bundle | **AUTOMATED GREEN** |
| Firebase Rules Emulator | **EMULATOR GREEN — 4/4 security assertions** |
| Client secret/ZIM/test-token scan | **AUTOMATED GREEN** |
| GitNexus detect/impact gate | **AUTOMATED GREEN — overall MEDIUM, no HIGH/CRITICAL** |
| iOS simulator native build | **AUTOMATED GREEN — `Runner.app` produced** |
| Real media cross-platform matrix | **MANUAL PENDING** |

### Suspension Criteria

- Pinned UIKit cannot pass mandatory token/cleanup/renewal behavior and Express fallback needs a new decision.
- iOS/Android package constraints require an unrelated dependency/platform upgrade.
- Existing dirty worktree overlaps the planned files and cannot be safely preserved.

---

## 7. Rollback Plan

No database migration exists for this feature.

Implementation rollback:

1. Revert RTC client adapters/coordinators and restore signaling-only call buttons.
2. Revert credential/detail/active API additions while preserving existing lifecycle endpoints.
3. Keep PostgreSQL call rows and timeline intact.
4. Keep Firestore Rules unchanged.
5. Rotate ZEGOCLOUD ServerSecret if exposure is suspected.
6. Run UC-144D chat regression suites after rollback.

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Risk | Detection in this spec | Gate |
|---|---|---|---|
| AP-AI-001 | Unconstrained generation | Every case references TDS/BR or current schema | G-0 |
| AP-AI-002 | Green from birth | RED examples listed in §5.1 | G-2 |
| AP-AI-003 | Implicit architecture decision | Official SDK adapter choice, token, coordinator, Web Mother access are explicit ADRs | G-1 |
| AP-AI-004 | Layer violation | Controller cases assert delegation; lifecycle/token policy stays in service | G-4 |
| AP-AI-005 | Hallucinated contract | Planned new contracts are listed in TDS §8 before implementation | G-3 |
| AP-AI-006 | False completion | Manual and automated statuses are separated | Exit Gate |

### Review Result

- [x] Draft contains traceable expected values.
- [x] Existing schema and current code were used as baseline.
- [x] Manual device cases are not pre-marked complete.
- [x] Reviewer approval received.

---

## APPENDIX — Draft Consistency Check

| Item | Result |
|---|---|
| API paths match TDS | Pass |
| State machine matches TDS | Pass |
| Revoke matrix matches TDS | Pass |
| Firestore payload/rules unchanged | Pass |
| Flutter/Web package decisions match research | Pass |
| Web Mother access explicitly tested | Pass |
| No production code written | Pass |
