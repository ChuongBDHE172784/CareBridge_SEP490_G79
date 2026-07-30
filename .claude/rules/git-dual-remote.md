---
trigger: always_on
---

# Git Dual Remote Safety Rules

This project uses **two Git remotes in parallel**:
- `github` → `git@github.com:ChuongBDHE172784/CareBridge_SEP490_G79.git` (email: `lamgameplayforme@gmail.com`)
- `gitlab` → `git@gitlab.com:manhnc2/su26_sep490_g79.git` (email: `lamvhhe186943@fpt.edu.vn`)

The working branch is `LamVH1`. The shared integration branch is `dev`.

---

## Absolute Rules (Never Break)

- ❌ NEVER run `git push` without first running `git pull` from **both** remotes on the `dev` branch.
- ❌ NEVER commit or push directly to the `dev` branch — always work on `LamVH1` first.
- ❌ NEVER use `git push origin` — always specify the remote explicitly: `git push github <branch>` or `git push gitlab <branch>`.
- ❌ NEVER run `git merge` without checking `git status` and `git log <remote>/dev..HEAD --oneline` first.
- ❌ NEVER ignore a merge conflict — all conflicts must be resolved before any push.

---

## Safe Commit Rules

- ❌ NEVER add "Co-Authored-By: Claude Opus 4.8" or any Claude/Anthropic signature to commit messages — commits are authored by you alone.
- Always verify your active email before committing:
  ```bash
  git config user.email
  ```
- Use semantic commit messages:
  - `feat:` — new feature
  - `fix:` — bug fix
  - `style:` — CSS/formatting only
  - `refactor:` — code improvement, no logic change
- **Commit Small and Granular**: ALWAYS split commits into small, focused groups of changes by module/feature area (e.g. separate commits for Backend API, Mobile App, Web App, or specific bug fixes). NEVER batch multiple unrelated features or large multi-component updates into a single monolithic commit.

---

## Branch Rules

- `LamVH1` — your personal working branch. All new code goes here.
- `dev` — shared integration branch. Only receives merges from `LamVH1`, never direct commits.
- When creating a new feature branch, always start from an up-to-date `dev`:
  ```bash
  git checkout dev
  git pull github dev
  git pull gitlab dev
  git checkout -b LamVH1
  ```

---

## Conflict Prevention

- Before starting any coding session, always sync `dev` from both remotes first (see Workflow: `start-day`).
- If a conflict occurs on `dev` after pulling from both remotes, resolve it on `dev` locally before merging into `LamVH1`.
- Use `git stash` to safely park unfinished work before switching branches — never force-switch with uncommitted changes.
