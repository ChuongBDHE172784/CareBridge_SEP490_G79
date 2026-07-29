# CareBridge Codex Instructions

## Git dual-remote rules (always active)

This repository uses two remotes in parallel:

- `github`: `git@github.com:ChuongBDHE172784/CareBridge_SEP490_G79.git`
- `gitlab`: `git@gitlab.com:manhnc2/su26_sep490_g79.git`
- Personal working branch: `LamVH1`
- Shared integration branch: `dev`

For every Git task involving branches, syncing, commits, pulls, pushes, merges,
or conflicts, read and follow these project instructions before suggesting or
executing commands:

- `.claude/rules/git-dual-remote.md`
- `.claude/skills/git-dual-remote-handler.md`
- `.claude/workflows/start-day.md`
- `.claude/workflows/end-day.md`

Treat `LamVH1` as canonical. Some examples in
`.claude/skills/git-dual-remote-handler.md` mistakenly say `LamVH`; replace that
with `LamVH1` when applying them.

### Non-negotiable safety rules

- Never push until `dev` has first been pulled from both `github` and `gitlab`.
- Never create feature or fix commits directly on `dev`. Work and commit on
  `LamVH1`, then merge `LamVH1` into `dev` through the documented workflow.
- Never use `git push origin`; always name `github` or `gitlab` explicitly.
- Before every merge, run `git status` and inspect unpushed commits against both
  remotes with `git log github/dev..HEAD --oneline` and
  `git log gitlab/dev..HEAD --oneline`.
- Never push with unresolved merge conflicts.
- Never force-switch branches with uncommitted work; stash unfinished changes
  first and restore them after syncing.
- Before committing, run `git config user.email` and use a focused semantic
  commit message (`feat:`, `fix:`, `style:`, or `refactor:` as appropriate).
- **Mandatory Granular Commits**: ALWAYS split commits into small, logical groups of changes by module/feature area (e.g. separate commits for Backend API, Mobile App, Web App, or specific bug fixes). NEVER combine multiple unrelated features or large multi-component updates into a single monolithic commit.

### Workflow triggers

- At the start of a coding session, follow `.claude/workflows/start-day.md`:
  inspect status, sync `dev` from both remotes, merge it into `LamVH1`, and
  verify the active email.
- When finishing work or preparing to push, follow
  `.claude/workflows/end-day.md`: ensure work is committed on `LamVH1`, re-sync
  `dev` from both remotes, merge `LamVH1` into `dev`, resolve every conflict,
  then push `dev` and the `LamVH1` backup to each explicitly named remote.


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

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **CareBridge_SEP490_G79** (70782 symbols, 114095 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

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
