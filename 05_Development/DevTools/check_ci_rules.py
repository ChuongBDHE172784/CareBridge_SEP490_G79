"""Check that .gitlab-ci.yml stays consistent with the branch policy it encodes.

GitLab only reports most of these mistakes by refusing to create the pipeline, which
surfaces as a push that appears to do nothing. Two of them have already happened
here, so they are checked rather than remembered:

1. ``needs:`` pointing at a job that does not run on the same branch. GitLab rejects
   the *whole* pipeline when a needs: target is missing, so widening one job's rules
   without widening the job it depends on breaks every push to that branch, not just
   the one job. scan_web_image / build_web_image drifted apart exactly this way.

2. A branch quietly falling outside every rules anchor. $CI_DEFAULT_BRANCH is "main"
   here and main rarely moves, so "dev" matched nothing for months: the branch the
   team pushes to daily ran no validate, no test and no security job, while the
   pages job published it to carebridgevn.site regardless.

Also asserted: pages publishes from exactly one branch, and dev reaches the security
stage. Both are policy, and policy that is not checked drifts back.

Usage:
    python 05_Development/DevTools/check_ci_rules.py            # check the repo file
    python 05_Development/DevTools/check_ci_rules.py <path>     # check a copy
"""

from __future__ import annotations

import sys
from pathlib import Path

import yaml

REPO_ROOT = Path(__file__).resolve().parents[2]
CI_FILE = REPO_ROOT / ".gitlab-ci.yml"

# The branch GitLab treats as default. Read from here rather than assumed, because
# the whole class of bug above comes from assuming this is "dev".
DEFAULT_BRANCH = "main"

# Branches a push can realistically land on. Personal branches are represented by
# one sample: they are all meant to gate through merge requests, not direct pushes.
BRANCHES = ["main", "dev", "page-web", "LamVH1"]

# Exactly the branch allowed to overwrite the published site.
PUBLISHING_BRANCH = "page-web"

# Jobs that must run on every push to dev, whatever the push touched.
DEV_REQUIRED_JOBS = ["semgrep_sast", "trivy_fs", "gitleaks_repo"]

RESERVED = {"stages", "workflow", "default", "variables", "include", "image", "services"}


def load(path: Path) -> dict:
    with path.open(encoding="utf-8") as handle:
        return yaml.safe_load(handle)


def resolve(config: dict, name: str) -> dict:
    """Merge a job with what it extends, so inherited rules and needs are visible."""
    body = dict(config[name])
    extends = body.get("extends")
    if extends:
        for parent in [extends] if isinstance(extends, str) else extends:
            for key, value in config.get(parent, {}).items():
                body.setdefault(key, value)
    return body


def jobs_of(config: dict) -> dict:
    return {
        name: resolve(config, name)
        for name, body in config.items()
        if isinstance(body, dict) and not name.startswith(".") and name not in RESERVED
    }


def fires(rules, branch: str) -> bool:
    """Whether a push to `branch` can create this job.

    A rule carrying `changes:` counts as firing: it depends on what the push touched,
    and the checks here must hold for the pushes where it does fire.
    """
    if rules is None:
        return True
    for rule in rules:
        if isinstance(rule, str):
            return True
        if rule.get("when") == "never":
            continue
        condition = rule.get("if", "").replace("$CI_DEFAULT_BRANCH", '"%s"' % DEFAULT_BRANCH)
        if "CI_COMMIT_BRANCH ==" in condition:
            wanted = condition.split("==")[1].strip().strip("\"' ")
            if wanted == branch:
                return True
            continue
        # Tag, merge-request and protected-ref rules say nothing about a branch push.
        if any(token in condition for token in ("CI_COMMIT_TAG", "merge_request_event", "REF_PROTECTED")):
            continue
        if condition == "":
            return True
    return False


def main(argv: list[str]) -> int:
    path = Path(argv[1]).resolve() if len(argv) > 1 else CI_FILE
    config = load(path)
    jobs = jobs_of(config)
    stage_order = {stage: index for index, stage in enumerate(config["stages"])}
    problems: list[str] = []

    for branch in BRANCHES:
        live = {name for name, body in jobs.items() if fires(body.get("rules"), branch)}
        for name in sorted(live):
            for need in jobs[name].get("needs") or []:
                target = need["job"] if isinstance(need, dict) else need
                if target not in live:
                    problems.append(
                        "[%s] '%s' needs '%s', which does not run on this branch; "
                        "GitLab will refuse to create the pipeline" % (branch, name, target, )
                    )
                    continue
                here = stage_order.get(jobs[name].get("stage"), 99)
                there = stage_order.get(jobs[target].get("stage"), 99)
                if there > here:
                    problems.append(
                        "[%s] '%s' (stage %s) needs '%s' from the later stage %s"
                        % (branch, name, jobs[name].get("stage"), target, jobs[target].get("stage"))
                    )

    # pages must publish from one branch only.
    publishers = [b for b in BRANCHES if fires(jobs.get("pages", {}).get("rules"), b)]
    if publishers != [PUBLISHING_BRANCH]:
        problems.append(
            "pages publishes from %s; expected only '%s'. Every branch listed there can "
            "overwrite the live site, and the last push wins."
            % (publishers or "no branch", PUBLISHING_BRANCH)
        )

    # dev must reach the security stage on every push.
    for name in DEV_REQUIRED_JOBS:
        if name not in jobs:
            problems.append("required job '%s' is missing from .gitlab-ci.yml" % name)
            continue
        for rule in jobs[name].get("rules") or []:
            if isinstance(rule, dict) and rule.get("if", "").strip() == '$CI_COMMIT_BRANCH == "dev"':
                if "changes" in rule:
                    problems.append(
                        "'%s' is gated on dev by changes:; it must run on every push to dev, "
                        "because a secret or an injection can arrive in any file" % name
                    )
                break
        else:
            problems.append("'%s' does not run on pushes to dev" % name)

    if problems:
        print("FAIL - .gitlab-ci.yml branch policy violated:\n")
        for problem in sorted(set(problems)):
            print("  - " + problem)
        return 1

    print("OK - %s branch policy holds." % path.name)
    print("  branches checked : " + ", ".join(BRANCHES))
    print("  publishing branch: " + PUBLISHING_BRANCH)
    print("  always on dev    : " + ", ".join(DEV_REQUIRED_JOBS))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
