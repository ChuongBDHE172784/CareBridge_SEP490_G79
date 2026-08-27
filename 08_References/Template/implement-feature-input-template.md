# Implement-Feature Input Template

Use this template when requesting implementation handoff or implementation review for one approved CareBridge Function.

## Pasteable Template

```md
Implement function theo workflow `implement-feature`:

- Function ID: [e.g. 3.1.1.3]
- Function name: [e.g. Login]
- UC number: [e.g. UC03]
- Workspace path: [optional; e.g. 04_Implement/<feature-folder>/]
- Requested mode: [Gemini handoff / review implementation returned by Gemini / readiness check only]
- Approved artifacts:
  - TDS: [path + Approved by/date]
  - Test-Spec: [path + Approved by/date]
- Target platforms: [Backend / Web / Mobile / End-to-end]
- In-scope implementation round: [what should be implemented now]
- Explicit out of scope: [what must not be changed in this round]
- Codebase area expected to change: [optional; modules, packages, screens, services]
- Red Gate expectation: [optional; which tests should fail first]
- Verification scope: [optional; targeted tests, build, lint, analyze, integration]
- Implementation source: [Gemini will implement / implementation already returned / partial implementation exists]
- Returned implementation evidence: [optional; changed files, patch, commit hash, screenshots, test logs]
- Known blockers or environment limits: [optional]
- Open review focus: [optional; security, API contract, session flow, privacy, migration risk]
```

## Minimal Template

```md
Implement function theo workflow `implement-feature`:

- Function ID:
- Function name:
- UC number:
- Requested mode:
- TDS status:
- Test-Spec status:
- In-scope implementation round:
```

## Example 1: Gemini Handoff

```md
Implement function theo workflow `implement-feature`:

- Function ID: 3.2.1
- Function name: Manage Own Login Sessions
- UC number: UC-19
- Workspace path: 04_Implement/<feature-folder>/
- Requested mode: Gemini handoff
- Approved artifacts:
  - TDS: Approved by user on 2026-06-25
  - Test-Spec: Approved by user on 2026-06-25
- Target platforms: Backend, Web
- In-scope implementation round: API list/revoke sessions, web UI session list, revoke current/other session behavior, audit logging.
- Explicit out of scope: Mobile UI, device fingerprint enrichment, suspicious session detection.
- Codebase area expected to change: CareBridgeAPI identity/session module, CareBridgeWebApp session settings pages.
- Red Gate expectation: Session controller/service tests and web component tests should fail first for missing revoke/list behavior.
- Verification scope: targeted backend tests, web tests, lint, build.
- Implementation source: Gemini will implement
- Known blockers or environment limits: No Docker-based integration test if local environment is unavailable.
- Open review focus: authorization ownership checks, revoke semantics, audit trail completeness.
```

## Example 2: Review Returned Implementation

```md
Implement function theo workflow `implement-feature`:

- Function ID: 3.1.1.3
- Function name: Login
- UC number: UC03
- Requested mode: review implementation returned by Gemini
- Approved artifacts:
  - TDS: Approved by user on 2026-06-25
  - Test-Spec: Approved by user on 2026-06-25
- Target platforms: Backend, Web, Mobile
- In-scope implementation round: Complete end-to-end login contract and session creation behavior.
- Explicit out of scope: SSO and biometric login.
- Implementation source: implementation already returned
- Returned implementation evidence: Gemini patch + changed files + test output
- Open review focus: contract drift vs TDS, token/session security, negative-path coverage, privacy-safe logging.
```

## Notes

- If `UC number` is unknown, write `Open` and I will derive it from the Function ID/name.
- If you want only a readiness/freshness audit before giving work to Gemini, set `Requested mode: readiness check only`.
- If you want me to produce the Gemini handoff task file, say: `Hay tao luon [FolderName]_Gemini-Task.md tu approved specs.`
