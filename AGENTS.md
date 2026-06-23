# Repository Guidelines

## Project Structure & Module Organization

This repository follows an SDLC folder layout:
- `01_Planning/` for project planning, meeting minutes, schedules, and risk tracking.
- `02_Requirements/` for software requirements (SRS, Use Cases, DFD, Traceability Matrix, Business Rules).
- `03_Design/` for system design (APIs, Architecture, Class/Activity/Sequence Diagrams, UI/UX).
- `04_Implement/` for deployment/implementation plans and artifacts.
- `05_Development/` for source code, database scripts, deployment, and tools:
  - `CareBridgeAPI/` (Backend — Spring Boot)
  - `CareBridgeMobileApp/` (Mobile — Flutter)
  - `CareBridgeWebApp/` (Web Portal — React + Vite)
  - `Database/` for ERD and SQL scripts.
  - `Deployment/` for Docker and CI/CD configurations.
  - `DevTools/` for development utilities.
  - `MachineLearning/` for AI/ML preprocessing, training, models, and safety rules.
- `06_Testing/` for all test plans, test cases, automation scripts, and UAT.
- `07_Reports/` for university project submission reports.
- `08_References/` for templates and reference materials.

Application code is under `05_Development/`:

- `CareBridgeAPI/`: Spring Boot Java 21 service. Main code is in `src/main/java/com/carebridge/backend`, resources and Flyway migrations are in `src/main/resources`, and tests are in `src/test/java`.
- `CareBridgeWebApp/`: Vite React TypeScript app. Use `src/app` for routing/providers/layouts, `src/features/<feature>` for feature modules, `src/shared` for reusable UI, API, hooks, and utilities.
- `CareBridgeMobileApp/`: Flutter app. Use `lib/app`, `lib/core`, `lib/features/<feature>`, `lib/shared`, and `test/`.

## Build, Test, and Development Commands

- Backend: from `05_Development/CareBridgeAPI`, run `.\mvnw.cmd spring-boot:run` to start locally, `.\mvnw.cmd test` for tests, and `.\mvnw.cmd clean package` for a full build.
- Frontend: from `05_Development/CareBridgeWebApp`, run `npm install`, `npm run dev`, `npm run build`, `npm run lint`, and `npm run preview`.
- Mobile: from `05_Development/CareBridgeMobileApp`, run `flutter pub get`, `flutter run`, `flutter test`, and `flutter analyze`.

## Coding Style & Naming Conventions

Follow each stack's conventions. Java uses package names under `com.carebridge.backend`, PascalCase classes, camelCase methods/fields, and uppercase constants. React components use PascalCase `.tsx` files; hooks start with `use`; feature folders stay organized by `components`, `models`, `pages`, and `services`. Dart files use `snake_case.dart`, classes use PascalCase, and private members begin with `_`. Keep indentation consistent with existing files; format Dart with `dart format`.

## Testing Guidelines

Backend tests use Spring Boot test dependencies and should mirror production packages under `src/test/java`; name tests `*Test.java`. Frontend has no test runner configured yet, so run `npm run lint` and `npm run build` before PRs. Mobile tests use `flutter_test`; place tests in `test/` and name files `*_test.dart`.

## Commit & Pull Request Guidelines

Git history uses Conventional Commits with scopes, for example `feat(security): add STORY-002 OTP auth scaffold` and `docs(codex): add STORY-002 plans and handoff`. Use `type(scope): summary`, keep summaries imperative, and include story IDs when relevant.

PRs should include a short description, linked issue/story, affected modules, test evidence, and screenshots for UI changes. Call out database migrations, configuration changes, or new secrets explicitly.

## Security & Configuration Tips

Do not commit credentials, `.env` files, or local IDE secrets. Backend configuration belongs in `application.yaml` with environment-specific values supplied externally. Review Flyway migration names before merging database changes.

## Codex Role and Responsibilities

Codex acts as the **Business Analyst, Software Architect, and Code Reviewer** for this repository. Codex must not implement application code or business logic directly.

### Workflow for Every New Task

1. Analyze the requirements, identify assumptions and constraints, and define clear acceptance criteria.
2. Design the solution, including the architecture, component responsibilities, data flow, and API contracts where applicable.
3. Produce a detailed implementation specification that another coding agent can follow without ambiguity.
4. Prepare a coding task for Claude. The user will manually provide the task to Claude for implementation.
5. Review the implementation returned by the user or Claude against the specification, acceptance criteria, repository conventions, security requirements, and test expectations.
6. Either approve the implementation or request specific, actionable changes.

### Implementation Restrictions

- Do not create or edit application source files with the `.java`, `.tsx`, or `.ts` extensions. Configuration files are the only exception.
- Do not implement business logic directly.
- Limit repository changes to requirements, architecture, specifications, review documentation, and configuration files unless the user explicitly changes these instructions.

## BMAD Integration

This project uses the BMad framework for AI-assisted development.

### Skill Selection

For every new task, invoke the `bmad-help` skill first. Use its recommendation to select the most appropriate BMad workflow. Common mappings include:

- Planning and architecture: `bmad-product-brief`, `bmad-prd`, `bmad-architecture`, `bmad-create-epics-and-stories`, and `bmad-sprint-planning`.
- Documentation: `bmad-prd`, `bmad-create-story`, `bmad-spec`, and `bmad-document-project`.
- Development handoff: `bmad-quick-dev`, `bmad-module-builder`, and `bmad-dev-story`.
- Review: `bmad-code-review`, `bmad-review-edge-case-hunter`, and `bmad-review-adversarial-general`.
- Testing: `bmad-qa-generate-e2e-tests` and the applicable `bmad-testarch-*` skill.
- UI/UX: `bmad-ux` and `ui-skill-system` when visual design or interface styling is involved.

### BMAD Resources

- Configuration: `_bmad/config.toml`.
- Installed Codex skills: `.agents/skills/`.
- BMAD modules and automation: `_bmad/automator/`, `_bmad/bmb/`, `_bmad/bmm/`, and `_bmad/tea/`.
- Generated artifacts: `_bmad-output/`.

### Required Usage Pattern

1. Invoke `bmad-help` before starting any new task.
2. Clearly tell the user which BMad skill or skills will be used and why before invoking them.
3. Follow the selected skill workflow and its required sequence.
4. Respect the implementation restrictions in this file. If a recommended skill would write prohibited application source code or implement business logic, use it only to produce the specification and coding handoff; the user will provide that handoff to Claude for implementation.
5. For UI/UX work, use both the applicable BMad UX workflow and `ui-skill-system` when its trigger conditions apply.

### Transparency Requirement

Always state the selected skill explicitly, for example: "Using `bmad-architecture` to design the solution." This allows the user to understand the approach and track the BMAD workflow.
