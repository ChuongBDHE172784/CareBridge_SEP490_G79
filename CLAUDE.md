# CareBridge Agent Rules

## Token Budget Rules

Use graph tools before filesystem reads. The goal is to answer with enough context, not maximum context.

1. Start with `code-review-graph` for changed files, review context, impact radius, affected flows, architecture, and test gaps.
2. Use `GitNexus` for execution flows, symbol context, call chains, dependency paths, impact analysis, and safe renames/refactors.
3. Use `rtk` for shell commands to keep output compact.
4. Read files only after graph tools identify the relevant files, symbols, or flows.

Do not begin with broad `grep`, `find`, `ls -R`, full directory trees, or whole-file reads. If MCP is unavailable, run the connection checks below, then fall back to narrow `rtk rg`, `rtk git`, `rtk read`, or native commands.

## Connection Checks

Use only when starting a session, after tool upgrades, or when tool output looks stale:

```bash
rtk --version
rtk node .gitnexus/run.cjs status
rtk code-review-graph status --repo /Users/huy/Documents/Đồ\ án/CareBridge_SEP490_G79
```

Refresh stale indexes:

```bash
rtk node .gitnexus/run.cjs analyze
rtk code-review-graph update --brief --repo /Users/huy/Documents/Đồ\ án/CareBridge_SEP490_G79
```

If `.gitnexus/run.cjs` is missing:

```bash
rtk npx gitnexus@latest analyze
```

## Required Workflow

Before editing application code:

1. Run graph scoping: `code-review-graph detect_changes` or `get_review_context`.
2. Locate the feature path with `GitNexus query` or symbol context.
3. Run impact analysis for each function, class, method, shared module, API contract, or migration area you intend to modify.
4. Warn the user before editing if impact is HIGH or CRITICAL.
5. Read only graph-identified files/snippets.

After editing:

1. Run the narrowest useful tests/builds.
2. Run `code-review-graph update --brief` or MCP change detection.
3. Before commit, run GitNexus `detect_changes`.
4. Report changed files, verification, and remaining risks.

## Project Map

* Backend: `05_Development/CareBridgeAPI`
* Web: `05_Development/CareBridgeWebApp`
* Mobile: `05_Development/CareBridgeMobileApp`
* Architecture: `03_Design/Architecture`
* Database: `05_Development/CareBridgeAPI/src/main/resources/db`

Current code, contracts, and migrations override historical design notes.

## Commands

Backend:

```bash
cd 05_Development/CareBridgeAPI
set -a && source .env && set +a && ./mvnw spring-boot:run
rtk ./mvnw test
rtk ./mvnw clean package
```

Web:

```bash
cd 05_Development/CareBridgeWebApp
rtk npm run dev
rtk npm run build
```

Mobile:

```bash
cd 05_Development/CareBridgeMobileApp
rtk flutter run -d chrome
rtk flutter run
rtk flutter run -d <device-id> --dart-define=API_BASE_URL=http://<LAN_IP>:8080
rtk flutter test
rtk flutter build apk
```

Mobile API defaults:

* Web target: `localhost:8080`
* Android emulator: `10.0.2.2:8080`
* Physical device: pass `--dart-define=API_BASE_URL=http://<LAN_IP>:8080`

## Stack

* Backend: Java 21, Spring Boot 3.5.x, Maven, PostgreSQL, Flyway.
* Web: React, TypeScript, Vite.
* Mobile: Flutter, Dart.
* Infrastructure: Docker Compose, GitLab CI/CD.
* Integrations: Firebase, TrackAsia, ZegoCloud, VNPay, Gmail SMTP, Gemini.

Use Firebase skills/tools for Firebase tasks before manual implementation.

## Architecture

Use a modular monolith. Do not introduce microservices, MongoDB, new infrastructure, or new dependencies without approval.

Backend package style: domain first, then layer:

```text
controller
service
repository
entity
dto
mapper
policy
```

Layer rules:

* Controller: validation and request/response mapping only.
* Service: workflows, transactions, authorization, auditing.
* Repository: persistence/query only.
* Mapper: entity/DTO conversion.
* Policy: reusable domain rules.

Never expose JPA entities in API responses. Use DTOs and mappers.

## Safety Rules

For health, location, payment, expert, moderation, consent, AI, auth, and emergency workflows:

* Preserve RBAC.
* Preserve consent scope and expiry.
* Preserve audit requirements.
* Do not weaken validation.
* AI guidance must never diagnose, prescribe, or delay emergency routing.
* Do not hide urgent-care escalation.

Use Flyway for schema changes. Never modify an applied migration.

## Environment And Secrets

Use `.env.example` as source of truth before changing env handling.

Backend commonly uses `CAREBRIDGE_DB_*`, `JWT_ACTIVE_KEY_ID`, `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEYS`, and integration secrets. Some older notes may mention Supabase-style variables; verify current code first.

`JWT_PRIVATE_KEY` is base64 DER PKCS#8. `JWT_PUBLIC_KEYS` is a semicolon-separated `kid:base64-DER-SPKI` ring.

Quote `.env` values containing `&` or spaces before `source`. Never commit real secrets or key material.

## Dev Seeder

`DevDataSeeder` runs only when Spring `dev` is active, `prod` is absent, and `CAREBRIDGE_DEV_SEED_ENABLED=true`.

Never commit `CAREBRIDGE_DEV_SEED_PASSWORD`, logs, screenshots, or shell snippets containing it. Never enable dev seeding on staging or production.

Seeded accounts may include:

| Role          | Email                      | Password    |
| ------------- | -------------------------- | ----------- |
| SYSTEM_ADMIN  | `admin@carebridge.dev`     | `Test@1234` |
| MODERATOR     | `moderator@carebridge.dev` | `Test@1234` |
| CONTENT_ADMIN | `content@carebridge.dev`   | `Test@1234` |
| EXPERT        | `expert@carebridge.dev`    | `Test@1234` |
| PARTNER       | `partner@carebridge.dev`   | `Test@1234` |
| MOTHER        | `mother@carebridge.dev`    | `Test@1234` |
| FAMILY        | `family@carebridge.dev`    | `Test@1234` |
| MOTHER        | `mother3@carebridge.dev`   | `Test@1234` |
| MOTHER        | `mother4@carebridge.dev`   | `Test@1234` |
| FAMILY        | `family2@carebridge.dev`   | `Test@1234` |
| FAMILY        | `family3@carebridge.dev`   | `Test@1234` |
| EXPERT        | `expert2@carebridge.dev`   | `Test@1234` |
| EXPERT        | `expert3@carebridge.dev`   | `Test@1234` |

## Graph Tool Notes

GitNexus resources:

* `gitnexus://repo/CareBridge_SEP490_G79/context`
* `gitnexus://repo/CareBridge_SEP490_G79/clusters`
* `gitnexus://repo/CareBridge_SEP490_G79/processes`
* `gitnexus://repo/CareBridge_SEP490_G79/process/{name}`

Use graph-backed rename/refactor tools. Never rename with text replacement alone.

Use GitNexus security `explain` only when the index was built with `analyze --pdg`; otherwise state that PDG/taint data is unavailable.

## Output

Final responses should be concise:

* What changed.
* What checks ran.
* What risks remain.

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **CareBridge_SEP490_G79** (70437 symbols, 112489 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> Index stale? Run `node .gitnexus/run.cjs analyze` from the project root — it auto-selects an available runner. No `.gitnexus/run.cjs` yet? `npx gitnexus analyze` (npm 11 crash → `npm i -g gitnexus`; #1939).

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows. For regression review, compare against the default branch: `detect_changes({scope: "compare", base_ref: "main"})`.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `query({search_query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `context({name: "symbolName"})`.
- For security review, `explain({target: "fileOrSymbol"})` lists taint findings (source→sink flows; needs `analyze --pdg`).

## Never Do

- NEVER edit a function, class, or method without first running `impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `rename` which understands the call graph.
- NEVER commit changes without running `detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/CareBridge_SEP490_G79/context` | Codebase overview, check index freshness |
| `gitnexus://repo/CareBridge_SEP490_G79/clusters` | All functional areas |
| `gitnexus://repo/CareBridge_SEP490_G79/processes` | All execution flows |
| `gitnexus://repo/CareBridge_SEP490_G79/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
