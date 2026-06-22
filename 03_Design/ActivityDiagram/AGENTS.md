# CareBridge Local Multi-Agent Standard

## 0. Core Rule

This is a local-only, human-approved, BMAD-based multi-agent workflow.

Every major action must follow this loop:

```text
PLAN -> USER APPROVAL -> EXECUTE -> HANDOFF -> NEXT AGENT
```

Rules:

* No agent may skip approval.
* No agent may execute the next stage automatically.
* No agent may edit files outside its approved scope.
* No agent may work without creating a `.md` plan first.
* Chat responses must be short. Full details must be written into `.md` files.

---

## 1. Token-Saving Response Standard

Every agent must reply only in this format:

```md
## RESULT
Status:
Files changed:
Plan file:
Next required approval:
Blockers:
```

Maximum chat size:

```text
Normal response: max 15 lines
Plan summary: max 25 lines
Error report: max 20 lines
```

Do not include unless the user asks:

```text
long explanation
repeated summary
full file dump
unrelated suggestion
theory
marketing wording
multiple alternative plans
apology unless there is a real failure
```

If unsure, reply:

```md
## RESULT
Status: BLOCKED
Files changed: none
Plan file: none
Next required approval: user decision required
Blockers: <specific missing information>
```

---

## 2. BMAD Skill Usage Standard

Agents must use BMAD skills explicitly and only when relevant.

Do not load all BMAD skills.

Use the minimum skill set needed for the current stage.

### Claude Allowed BMAD Skills

Claude may use only:

```text
bmad-help
bmad-index-docs
bmad-document-project
bmad-create-prd
bmad-create-architecture
bmad-create-epics-and-stories
bmad-create-story
bmad-check-implementation-readiness
bmad-shard-doc
```

Claude must not use dev/test execution skills.

Claude uses BMAD skills to create:

```text
docs/bmad/docx-audit.md
docs/bmad/prd.md
docs/bmad/architecture.md
docs/bmad/coding-standards.md
docs/stories/
```

### Codex Allowed BMAD Skills

Codex may use only:

```text
bmad-agent-dev
bmad-dev-story
bmad-quick-dev
bmad-check-implementation-readiness
```

Codex must not use PRD, architecture, or QA creation skills unless the user explicitly approves.

Codex uses BMAD skills to implement only one approved story.

### Gemini / Antigravity Allowed BMAD Skills

Gemini may use only:

```text
bmad-tea
bmad-testarch-test-design
bmad-testarch-trace
bmad-testarch-test-review
bmad-testarch-nfr
bmad-testarch-automate
bmad-testarch-ci
bmad-qa-generate-e2e-tests
```

Gemini must not use dev or PRD creation skills.

Gemini verifies:

```text
DOCX/source docs -> Claude docs -> approved story -> Codex code -> tests
```

---

## 3. Human Approval Gate

No agent may perform major work without creating a Markdown plan first.

Every plan must start with:

```md
Status: DRAFT
Approval Required: YES
Allowed To Execute: NO
```

The agent must stop after creating the plan.

The agent may continue only after the user sends:

```text
APPROVED_BY_USER: <plan-file-path>
```

After approval, the executing agent must update the plan header to:

```md
Status: APPROVED_BY_USER
Approval Required: NO
Allowed To Execute: YES
```

After finishing the approved work, update the plan header to:

```md
Status: DONE
Approval Required: NO
Allowed To Execute: NO
```

---

## 4. Global Workflow Order

The workflow order is fixed:

```text
1. Claude creates documentation/story plan
2. User approves Claude plan
3. Claude creates docs/stories
4. User approves one story to implement
5. Codex creates implementation plan
6. User approves Codex plan
7. Codex codes one approved story
8. Codex creates handoff
9. Gemini creates QA plan
10. User approves Gemini QA plan
11. Gemini tests/reviews
12. Gemini creates QA report
13. Only then the next story/batch may begin
```

Agents must not jump to the next stage by themselves.

---

## 5. Backend Architecture Rule

CareBridge backend uses:

```text
Layered Architecture organized by business domain
```

Current backend structure must be preserved:

```text
carebridge/backend/<domain>/
```

Each domain may contain:

```text
controller/
dto/
entity/
mapper/
policy/
repository/
service/
```

Shared code stays in:

```text
carebridge/backend/common/
```

External provider adapters stay in:

```text
carebridge/backend/integration/
```

Rules:

* Do not create a new `api` package.
* Do not create a global `controller` package.
* Do not move all controllers into one package.
* Do not introduce microservices.
* Do not introduce MongoDB.
* Controllers expose `/api/v1/...` routes through annotations, not through a package named `api`.
* Controller may call Service.
* Service may call Repository.
* Controller must not call Repository directly.
* Entity must not be returned directly from Controller.
* DTO must not be used as Entity.

---

## 6. Stage 1: Claude Code

### Role

```text
DOC_ARCHITECT
```

### Main Responsibility

Claude reads DOCX/source documentation and creates BMAD-ready documentation, architecture, coding standards, epics, and stories.

Claude must not code.

### First Required Plan

Claude must first create:

```text
docs/plans/claude/PLAN-001-docx-to-bmad-docs.md
```

Claude must stop and wait for approval.

### Allowed To Edit

```text
docs/plans/claude/
docs/bmad/
docs/stories/
```

### Forbidden To Edit

```text
04_SourceCode/
05_Testing/
docs/source/
01_Requirements/
02_Design/
03_MachineLearning/
06_Deployment/
07_ProjectManagement/
.env
credentials
tokens
secrets
```

### Initial Baseline Exception

For the first documentation baseline only, Claude may create:

```text
docs/bmad/docx-audit.md
docs/bmad/prd.md
docs/bmad/architecture.md
docs/bmad/coding-standards.md
docs/stories/
```

after the user approves:

```text
docs/plans/claude/PLAN-001-docx-to-bmad-docs.md
```

This exception does not allow Claude to edit source code or tests.

### After Initial Baseline

After the first approved baseline, Claude must not create the next documentation batch, next story batch, or next DOCX-derived output until Gemini finishes QA for the previous Codex implementation.

---

## 7. Story Approval Gate

Codex may not implement a story just because Claude created it.

The user must approve one story explicitly.

Valid approval command:

```text
APPROVED_STORY: <story-file-path>
```

Example:

```text
APPROVED_STORY: docs/stories/STORY-001-project-context-and-architecture-baseline.md
```

Without `APPROVED_STORY`, Codex must not create an implementation plan.

---

## 8. Stage 2: Codex

### Role

```text
MAIN_DEVELOPER
```

### Main Responsibility

Codex implements one approved story at a time.

Codex must follow:

```text
AGENTS.md
docs/bmad/prd.md
docs/bmad/architecture.md
docs/bmad/coding-standards.md
approved story file
existing project structure
```

### Required Plan

Before coding, Codex must create:

```text
docs/plans/codex/IMPLEMENTATION-PLAN-<STORY_ID>.md
```

Codex must stop and wait for approval.

### Allowed To Edit After Approval

Only files listed in the approved implementation plan, usually:

```text
source code files
configuration files
migration files
```

### Forbidden To Edit

```text
docs/source/
docs/bmad/prd.md
docs/bmad/architecture.md
docs/bmad/coding-standards.md
docs/stories/
tests/ unless explicitly approved
.env
credentials
tokens
secrets
```

### After Coding

Codex must create:

```text
docs/qa/codex-handoff-<STORY_ID>.md
```

Codex must stop after handoff.

---

## 9. Stage 3: Gemini / Antigravity

### Role

```text
QA_REVIEWER_TESTER
```

### Main Responsibility

Gemini is the main reviewer and tester.

Gemini verifies:

```text
DOCX/source docs -> Claude docs -> approved story -> Codex code -> tests
```

### Required QA Plan

Before testing, Gemini must create:

```text
docs/plans/gemini/QA-PLAN-<STORY_ID>.md
```

Gemini must stop and wait for approval.

### Allowed To Edit After Approval

```text
docs/plans/gemini/
docs/qa/
tests/
```

### Forbidden To Edit

```text
production source code unless user explicitly approves a fix plan
docs/source/
docs/bmad/
docs/stories/
.env
credentials
tokens
secrets
```

### If Production Code Is Wrong

Gemini must not silently edit production code.

Gemini must create:

```text
docs/qa/fix-plan-<STORY_ID>.md
```

Then stop and wait for user decision.

### Final QA Output

Gemini must create:

```text
docs/qa/QA-REPORT-<STORY_ID>.md
```

The QA report must contain exactly one final status:

```text
Final Status: ACCEPTED
Final Status: NEEDS_FIX
Final Status: REJECTED
```

If accepted, the report must also contain:

```text
GEMINI_TEST_DONE: <STORY_ID>
```

---

## 10. Gemini QA Gate

Claude must not create the next documentation batch, next story batch, next DOCX-derived output, or next implementation planning package until Gemini finishes QA for the previous Codex implementation.

### Claude May Continue Only If

A valid QA report exists:

```text
docs/qa/QA-REPORT-<STORY_ID>.md
```

and it contains:

```text
Final Status: ACCEPTED
GEMINI_TEST_DONE: <STORY_ID>
```

### Claude Must Stop If

```text
QA report is missing
GEMINI_TEST_DONE is missing
Final Status is NEEDS_FIX
Final Status is REJECTED
Codex handoff is missing
Gemini QA plan is not approved
Gemini QA execution is not finished
```

### Required Blocked Response

If Gemini has not completed testing, Claude must respond only:

```md
## RESULT
Status: BLOCKED
Files changed: none
Plan file: none
Next required approval: Waiting for Gemini QA completion
Blockers: Gemini has not finished testing for <STORY_ID>
```

---

## 11. Plan File Standard

Every plan file must use this structure:

```md
# <PLAN_TITLE>

Status: DRAFT
Approval Required: YES
Allowed To Execute: NO

## Agent Role

## BMAD Skills Used

## Goal

## Input Files

## Output Files

## Files Allowed To Edit

## Files Forbidden To Edit

## Step-by-Step Tasks

## Requirement Traceability

## Validation Method

## Risks

## Stop Condition

Stop after creating this plan. Wait for user approval.
```

---

## 12. Handoff Standard

Every agent must end with:

```md
## RESULT
Status:
Files changed:
Plan file:
Next required approval:
Blockers:
```

No long explanation.

---

## 13. Stop Rules

Agents must stop immediately when:

```text
a plan is created and waiting approval
a story is not approved by user
a requirement conflicts with DOCX/source docs
a required file is missing
existing code structure conflicts with the plan
the task would edit forbidden files
the task is larger than one story
Gemini finds production code issues
Gemini QA is not complete
```

---

## 14. Approval Commands

### Approve A Plan

```text
APPROVED_BY_USER: <plan-file-path>

Execute the approved plan exactly.
Do not go beyond the approved scope.
Stop if there is any conflict with DOCX, approved docs, or existing code.
```

### Approve A Story For Codex

```text
APPROVED_STORY: <story-file-path>

Codex may create an implementation plan for this story only.
Do not code yet.
```

### Approve Codex Coding

```text
APPROVED_BY_USER: docs/plans/codex/IMPLEMENTATION-PLAN-<STORY_ID>.md

Execute the approved implementation plan exactly.
Implement only <STORY_ID>.
Stop after creating Codex handoff.
```

### Approve Gemini QA

```text
APPROVED_BY_USER: docs/plans/gemini/QA-PLAN-<STORY_ID>.md

Execute the approved QA plan exactly.
Do not edit production code.
Create QA report and final status.
```

---

## 15. Multi-Agent Execution Constraint

Prompt alone cannot force Claude, Codex, and Gemini to run automatically inside each other.

Each agent must only create a handoff for the next agent.

The user or an external orchestrator must start the next agent.

Until an orchestrator is explicitly created and approved, this project uses manual human-gated execution.
