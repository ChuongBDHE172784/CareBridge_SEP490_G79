# CareBridge — Claude Code Context

## Commands

* Backend (`05_Development/CareBridgeAPI`):
  * Run: `./mvnw spring-boot:run`
  * Build/Package: `./mvnw clean package`
  * Test: `./mvnw test`
* Web (`05_Development/CareBridgeWebApp`):
  * Run: `npm run dev`
  * Build: `npm run build`
* Mobile (`05_Development/CareBridgeMobileApp`):
  * Run: `flutter run`
  * Build: `flutter build apk`
  * Test: `flutter test`

## Stack

* Backend: Java 21, Spring Boot 3.5.x, Maven, PostgreSQL, Flyway.
* Web: React + TypeScript + Vite.
* Mobile: Flutter/Dart.
* Infrastructure: Docker Compose, GitLab CI/CD.
* Integrations: Firebase, TrackAsia, ZegoCloud, VNPay, Gmail SMTP, Gemini.

## Architecture

Use a modular monolith. Do not introduce microservices, MongoDB, new infrastructure, or dependencies without approval.

Backend package style: Package by domain, then layers inside:
`controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `policy`.

Rules:
* Controller: validation, request/response mapping only; no business logic.
* Service: workflows, transactions, authorization checks.
* Repository: persistence/query only; no business decisions.
* Never expose JPA entities in API responses; use DTOs and mappers.
* Policy: reusable domain rules (healthcare safety, consent, payment eligibility, moderation).

## Read On Demand

Read only the files relevant to the task:
* Backend: `05_Development/CareBridgeAPI/`
* Web: `05_Development/CareBridgeWebApp/`
* Mobile: `05_Development/CareBridgeMobileApp/`
* Architecture: `03_Design/Architecture/`
* Database/contracts: `05_Development/Database/`, `05_Development/Contracts/`

Current code and migrations override historical design notes.

## Delivery Rules

Make the smallest scoped change. Do not refactor unrelated code, delete existing code, or upgrade dependencies unless requested.

For health, location, payment, expert, moderation, and safety workflows: enforce existing RBAC, consent scope/expiry, and audit requirements. AI provides guidance only; never diagnose, prescribe, or delay emergency routing.

Use Flyway for schema changes. Never modify an applied migration.

Inspect relevant code/tests first. Add focused tests where practical, run the narrowest relevant command, then report changed files, verification, and remaining risks.

## Code Search & Analysis Priority Matrix

**Rule: NEVER use grep/glob/read first. Always use Graph MCP tools to minimize tokens and map blast radius.**

1. **Step 1: Scoping & Blast Radius (code-review-graph)**
   * Use `detect_changes` and `get_impact_radius` to identify modified files and their affected areas.
   * Use `get_review_context` to fetch only the exact code changes and relevant context.

2. **Step 2: Architecture & Relations (gitnexus & code-review-graph)**
   * For call chains, dependency paths, or tracking tests: Use `query_graph` or `query` (cypher query).
   * For visual/conceptual routing & API maps: Use `route_map` or `get_architecture_overview`.
   * For structural multi-file renaming: Use `rename`.

3. **Step 3: Verification (Native Commands)**
   * Only read/edit the files discovered in Step 1 & 2.
   * Use native `grep` or `read` as a fallback only when seeking precise string matching inside the narrowed file set.


<!-- code-review-graph MCP tools -->
## MCP Tools: code-review-graph

**IMPORTANT: This project has a knowledge graph. ALWAYS use the
code-review-graph MCP tools BEFORE using Grep/Glob/Read to explore
the codebase.** The graph is faster, cheaper (fewer tokens), and gives
you structural context (callers, dependents, test coverage) that file
scanning cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes` or `query_graph` instead of Grep
- **Understanding impact**: `get_impact_radius` instead of manually tracing imports
- **Code review**: `detect_changes` + `get_review_context` instead of reading entire files
- **Finding relationships**: `query_graph` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview` + `list_communities`

Fall back to Grep/Glob/Read **only** when the graph doesn't cover what you need.

### Key Tools

| Tool | Use when |
| ------ | ---------- |
| `detect_changes` | Reviewing code changes — gives risk-scored analysis |
| `get_review_context` | Need source snippets for review — token-efficient |
| `get_impact_radius` | Understanding blast radius of a change |
| `get_affected_flows` | Finding which execution paths are impacted |
| `query_graph` | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes` | Finding functions/classes by name or keyword |
| `get_architecture_overview` | Understanding high-level codebase structure |
| `refactor_tool` | Planning renames, finding dead code |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes` for code review.
3. Use `get_affected_flows` to understand impact.
4. Use `query_graph` pattern="tests_for" to check coverage.
