# GIT-PUSH-PLAN-STORY-002-backend-shared-domain-scaffold

Status:
Approval Required: YES
Allowed To Execute: YEs

## Agent Role

MAIN_DEVELOPER

## BMAD Skills Used

- bmad-agent-dev

## Goal

Push STORY-002 code to GitHub in clear, reviewable commits grouped by module and task.

Target branch:

```text
lam-branch-code
```

Remote:

```text
origin https://github.com/ChuongBDHE172784/CareBridge_SEP490_G79
```

## Input Files

- `AGENTS.md`
- `docs/qa/codex-handoff-STORY-002-backend-shared-domain-scaffold.md`
- current `git status`
- current branch and remote

## Output Files

No source output files are created by this plan.

Git output after approval:

- Multiple local commits grouped by module/task
- Push to `origin/lam-branch-code`

## Files Allowed To Edit

No file content edits are planned.

Allowed git operations after approval:

- inspect staged/unstaged state
- stage only STORY-002 files listed below
- create commits with clear conventional messages
- push `lam-branch-code` to GitHub

Allowed STORY-002 paths to stage and commit:

- `04_SourceCode/CamBridgeAPI/pom.xml`
- `04_SourceCode/CamBridgeAPI/src/main/resources/application.yaml`
- `04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/BackendApplication.java`
- `04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/common/config/JacksonConfig.java`
- `04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/common/config/WebMvcConfig.java`
- `04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/common/config/SpringDocConfig.java`
- `04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/common/exception/`
- `04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/common/response/`
- `04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/common/constants/`
- `04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/common/validation/`
- `04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/common/util/`
- `04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/security/`
- `04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/consent/`
- `04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/audit/`
- `04_SourceCode/CamBridgeAPI/src/main/resources/db/migration/V1__create_user_table.sql`
- `04_SourceCode/CamBridgeAPI/src/main/resources/db/migration/V2__create_otp_verification_table.sql`
- `04_SourceCode/CamBridgeAPI/src/main/resources/db/migration/V3__create_consent_grants_table.sql`
- `04_SourceCode/CamBridgeAPI/src/main/resources/db/migration/V4__create_audit_logs_table.sql`
- `04_SourceCode/CamBridgeAPI/src/main/resources/db/migration/V5__create_security_events_table.sql`
- `docs/plans/codex/IMPLEMENTATION-PLAN-STORY-002-backend-shared-domain-scaffold.md`
- `docs/qa/codex-handoff-STORY-002-backend-shared-domain-scaffold.md`
- `docs/plans/codex/GIT-PUSH-PLAN-STORY-002-backend-shared-domain-scaffold.md`

## Files Forbidden To Edit

- `01_Requirements/SRS/`
- `docs/source/`
- `docs/bmad/`
- `docs/stories/`
- frontend code
- mobile code
- tests
- `.env`
- credentials
- tokens
- secrets
- any unrelated changed file

Forbidden current worktree changes to exclude:

- deleted DOCX files under `01_Requirements/SRS/`
- modified SRS markdown files under `01_Requirements/SRS/`

## Step-by-Step Tasks

1. Stop after this plan is created and wait for user approval.
2. After approval, update this plan header to:

```text
Status: APPROVED_BY_USER
Approval Required: Yes
Allowed To Execute: YES
```

3. Inspect:
   - `git status --short --untracked-files=all`
   - `git diff --cached --name-only`
   - `git diff --name-only`
   - `git branch --show-current`
   - `git remote -v`
4. If unrelated files are staged, stop unless they can be safely excluded without changing their content.
5. Run compile check:

```powershell
cd 04_SourceCode/CamBridgeAPI
.\mvnw.cmd -q -DskipTests compile
```

6. Run existing tests only:

```powershell
cd 04_SourceCode/CamBridgeAPI
.\mvnw.cmd test
```

7. If tests still fail only because PostgreSQL is unavailable at `localhost:5432`, record this in the commit notes and continue because it is already documented in the Codex handoff.
8. Create commits grouped by module/task:

```text
chore(backend): configure persistence and auth foundation
feat(common): add shared API response and exception scaffold
feat(security): add OTP authentication and JWT scaffold
feat(consent): add consent grant scaffold
feat(audit): add audit logging scaffold
feat(db): add initial Flyway migrations for STORY-002
docs(codex): add STORY-002 implementation plan and handoff
```

9. Each commit must stage only the files for that module/task.
10. Verify final pending changes still exclude unrelated SRS changes.
11. Push to GitHub:

```powershell
git push origin lam-branch-code
```

12. Update this plan header to:

```text
Status: DONE
Approval Required: Yes
Allowed To Execute: Yes
```

13. Stop and report push result.

## Requirement Traceability

- STORY-002 implementation code: pushed in backend module/task commits.
- AGENTS.md human gate: this plan requires approval before commit/push.
- GitHub best practice: conventional commit messages, scoped commits, no unrelated files.
- User request: commit/push clearly by module and task.

## Validation Method

- `.\mvnw.cmd -q -DskipTests compile`
- `.\mvnw.cmd test`
- `git status --short --untracked-files=all`
- `git log --oneline -n 10`
- `git push origin lam-branch-code`

Known validation caveat:

- Existing tests currently fail if local PostgreSQL is not running at `localhost:5432`.

## Risks

- Network access may require user approval for `git push`.
- Git credentials may be missing or expired.
- Current worktree includes unrelated SRS changes; these must not be committed.
- Some STORY-002 files may already be staged; staging must be handled carefully to avoid committing unrelated work.

## Stop Condition

Stop after creating this plan. Wait for user approval.

Valid approval command:

```text
APPROVED_BY_USER: docs/plans/codex/GIT-PUSH-PLAN-STORY-002-backend-shared-domain-scaffold.md
```
