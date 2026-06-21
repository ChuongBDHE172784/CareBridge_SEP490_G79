# Safe GitHub and GitLab Push Workflow

This guide keeps the `dev` branch synchronized with both project repositories while preventing unrelated working-tree changes from entering a documentation-only commit.

## Repository endpoints

```text
GitHub: https://github.com/ChuongBDHE172784/CareBridge_SEP490_G79
GitLab: https://gitlab.com/manhnc2/su26_sep490_g79
GitHub authentication account: lamgameplayforme@gmail.com
GitLab authentication account: lamvhhe186943@fpt.edu.vn
Shared branch: dev
Personal branch: LamVH
```

Never place a personal access token in a remote URL, command, script, or committed file. Use the operating-system credential manager, an SSH key, or an interactive credential prompt.

The authentication accounts above are used when each hosting service asks for credentials. They do not select the commit author. A Git commit stores one author name and one author email, and the same immutable commit is pushed to both platforms. Configure one commit email that is verified on both accounts:

```powershell
git config --local user.name "Vu Lam"
git config --local user.email "<email-verified-on-both-platforms>"
git config --local --get user.name
git config --local --get user.email
```

Do not create different commits merely to use a different author email on each platform; that would make the GitHub and GitLab histories diverge.

The current machine-wide commit identity is `LamVH <lamgameplayforme@gmail.com>`. Keep it for this repository only if that email is verified on both platforms; otherwise verify it on GitLab or configure another email shared by both accounts before committing.

## 1. Configure and verify both remotes

Keep `origin` for GitHub and add `gitlab` for GitLab:

```powershell
git remote get-url origin
git remote add gitlab https://gitlab.com/manhnc2/su26_sep490_g79.git
git remote -v
```

If `gitlab` already exists, verify it instead of adding it again:

```powershell
git remote get-url gitlab
```

Correct an existing GitLab URL only when it is wrong:

```powershell
git remote set-url gitlab https://gitlab.com/manhnc2/su26_sep490_g79.git
```

## 2. Commit completed work on `LamVH`

All development starts and is committed on the personal branch. Use Conventional Commits in the repository format `type(scope): imperative summary`, including a story ID when relevant.

```powershell
git switch LamVH
git status --short --branch
git add <intentional-paths-only>
git diff --cached --check
git diff --cached --stat
git commit -m "docs(git): document safe dual-remote workflow"
```

Examples:

```text
feat(security): add STORY-002 OTP verification
fix(consultation): prevent duplicate booking
docs(git): clarify LamVH merge and dual push workflow
```

Never use `git add .` without reviewing its scope first.

## 3. Inspect before changing branch history

```powershell
git branch --show-current
git status --short --branch
git fetch origin --prune
git fetch gitlab --prune
```

Do not run `git pull`, `git merge`, or `git push` until unexpected changes are understood. In particular, do not use `git add .` when the intended commit is limited to `docs/`.

## 4. Switch to `dev` and pull before merging

Run this section only after the `LamVH` commit is complete and the working tree is clean. Fetch both platforms first and confirm that their `dev` branches are not already divergent.

```powershell
git switch dev
git fetch origin dev
git fetch gitlab dev
git log --oneline --left-right origin/dev...gitlab/dev
git pull --ff-only origin dev
```

The comparison must produce no output when both remote `dev` branches are synchronized. If it shows commits on either side, stop and reconcile the remote histories before continuing.

## 5. Merge `LamVH` into `dev`

```powershell
git merge --no-ff LamVH
```

Check for unresolved conflicts. The first command must produce no output:

```powershell
git diff --name-only --diff-filter=U
git status --short --branch
git log -5 --oneline --decorate
```

Run the tests required by every affected module. Do not push when conflicts remain, a required test fails, or the merge contains unexpected files. Never solve divergence with `--force` on `dev`.

## Documentation-only variation

When the work on `LamVH` is documentation-only, the `-A` flag records all three intended states under `docs/`: added files, modified files, and deleted files that no longer exist locally.

```powershell
git status --short -- docs
git add -f -A -- docs
git diff --cached --name-status
```

The local repository currently excludes untracked `docs/` content through `.git/info/exclude`, so `-f` is required to include intentional existing documentation. It does not add anything outside `docs/`.

Use this PowerShell scope guard. It must produce no output:

```powershell
git diff --cached --name-only | Where-Object { $_ -notlike 'docs/*' }
```

If it prints any path outside `docs/`, clear the index without discarding local work and stage `docs/` again:

```powershell
git restore --staged -- .
git add -f -A -- docs
```

Validate the staged documentation and commit it:

```powershell
git diff --cached --check
git diff --cached --stat
git commit -m "docs(git): synchronize documentation across remotes"
```

The repository's `docs/.gitattributes` permits Markdown's intentional two-space hard line breaks while retaining Git whitespace checks for other documentation formats.

Return to sections 4 and 5 after this commit to update `dev`, merge `LamVH`, and validate the result.

## 6. Push safely to both platforms

Push separately so a failure identifies the affected platform clearly:

```powershell
git push origin dev
git push gitlab dev
```

This publishes the same validated `dev` commit to both platforms as one release step. Git cannot make pushes to two independent hosting services atomic. If the first succeeds and the second fails, do not amend or recreate the commit. Fix authentication, permission, protection, or divergence on the failed remote and retry the same commit there.

## 7. Verify both platforms point to the same commit

```powershell
git fetch origin dev
git fetch gitlab dev
git rev-parse dev
git rev-parse origin/dev
git rev-parse gitlab/dev
```

All three hashes must match. A compact final check is:

```powershell
git log -1 --oneline --decorate dev origin/dev gitlab/dev
git status --short --branch
```

## Recovery rules

- Push rejected because the remote is ahead: fetch it, inspect the remote-only commits, then merge or rebase according to the team policy.
- Authentication failed: repair the credential manager or SSH key; never save a token in the repository.
- Wrong files were staged but not committed: run `git restore --staged -- .`, then stage `docs/` again.
- Wrong documentation commit was already pushed: create a normal corrective or revert commit. Do not rewrite shared `dev` history.
- One platform succeeded and the other failed: retry only the failed platform after resolving its error.
