#!/usr/bin/env python3
"""Render self-contained Compose files for hand-delivery to a server.

The tracked docker-compose.server*.yml files keep every secret outside the repo
behind ${...} placeholders and an env_file. That is the right shape for version
control, but it means deploying needs the compose file *and* .env.server *and* a
tunnel token file.

This renders the same two stacks with every value substituted inline, so each
output file runs on its own:

    python 05_Development/Deployment/render-standalone-compose.py

Outputs *.standalone.yml at the repo root. Those files contain the database
password, JWT private key and Firebase service-account key in plain text, so
.gitignore excludes them. Transfer them over scp, never over chat or a shared
document, and re-run this script after editing a template or .env.server.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
ENV_FILE = REPO / "05_Development/Deployment/.env.server"
JOBS = [
    ("docker-compose.server.yml", "carebridge-server.standalone.yml"),
    ("docker-compose.server.compreface.yml", "carebridge-compreface.standalone.yml"),
]

# ${VAR:?msg} | ${VAR:-default} | ${VAR}
PLACEHOLDER = re.compile(r"\$\{([A-Za-z_][A-Za-z0-9_]*)(?::([?-])([^}]*))?\}")


def read_env(path: Path) -> dict[str, str]:
    """Parse an env file the way Compose does, including quote stripping."""
    values: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8-sig").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        values[key.strip()] = value
    return values


def substitute(text: str, env: dict[str, str], missing: set[str]) -> str:
    def replace(match: re.Match[str]) -> str:
        name, kind, fallback = match.group(1), match.group(2), match.group(3)
        value = env.get(name, "")
        if value:
            return value
        if kind == "-":
            return fallback
        # ${VAR:?} with nothing to fall back on, or a bare ${VAR}
        missing.add(name)
        return f"REPLACE_ME__{name}"

    return PLACEHOLDER.sub(replace, text)


def yaml_block(pairs: dict[str, str], indent: int) -> str:
    """Emit a mapping whose values survive base64 padding, spaces and colons."""
    pad = " " * indent
    return "\n".join(f"{pad}{k}: {json.dumps(v)}" for k, v in pairs.items())


def inline_env_file(text: str, env: dict[str, str]) -> str:
    """Replace the backend's `env_file:` list with the values it would have loaded."""
    pattern = re.compile(r"^    env_file:\n(?:      -.*\n)+", re.MULTILINE)
    if not pattern.search(text):
        return text
    block = "    environment:\n" + yaml_block(env, 6) + "\n"
    # The service already has its own `environment:` further down; Compose rejects
    # a duplicate key, so merge by emitting the env-file values first and letting
    # the explicit block that follows stay authoritative.
    return pattern.sub(lambda _: block, text, count=1)


def merge_duplicate_environment(text: str) -> str:
    """Fold two consecutive `environment:` maps in one service into a single map.

    The later entries win, matching Compose precedence where `environment:`
    overrides `env_file:`.
    """
    pattern = re.compile(
        r"^    environment:\n((?:      \S.*\n)+)    environment:\n((?:      \S.*\n)+)",
        re.MULTILINE,
    )

    def fold(match: re.Match[str]) -> str:
        merged: dict[str, str] = {}
        for chunk in (match.group(1), match.group(2)):
            for line in chunk.splitlines():
                entry = line.strip()
                # The templates document themselves with comments; a generated
                # file has no use for them and they carry no colon to split on.
                if entry.startswith("#") or ":" not in entry:
                    continue
                key, value = entry.split(":", 1)
                merged[key.strip()] = value.strip()
        body = "\n".join(f"      {k}: {v}" for k, v in merged.items())
        return f"    environment:\n{body}\n"

    return pattern.sub(fold, text)


def inline_tunnel_token(text: str, env: dict[str, str], missing: set[str]) -> str:
    """Swap the Docker secret for an inline token so the file needs no companion."""
    token = env.get("CLOUDFLARE_TUNNEL_TOKEN", "")
    if not token:
        token = "REPLACE_ME__CLOUDFLARE_TUNNEL_TOKEN"
        missing.add("CLOUDFLARE_TUNNEL_TOKEN")
    text = text.replace(
        '      TUNNEL_TOKEN_FILE: /run/secrets/cloudflare_tunnel_token\n'
        '    secrets:\n'
        '      - cloudflare_tunnel_token\n',
        f"      TUNNEL_TOKEN: {json.dumps(token)}\n",
    )
    return re.sub(r"\nsecrets:\n(?:  .*\n|\n)*$", "\n", text)


def main() -> int:
    if not ENV_FILE.exists():
        print(f"Missing {ENV_FILE}. Copy server.env.example and fill it in.", file=sys.stderr)
        return 1

    env = read_env(ENV_FILE)
    backend_env = {k: v for k, v in env.items() if v}
    failed = False

    for template_name, output_name in JOBS:
        missing: set[str] = set()
        text = (REPO / template_name).read_text(encoding="utf-8")
        text = inline_env_file(text, backend_env)
        text = substitute(text, env, missing)
        text = merge_duplicate_environment(text)
        if "cloudflared:" in text:
            text = inline_tunnel_token(text, env, missing)

        banner = (
            "# GENERATED by 05_Development/Deployment/render-standalone-compose.py\n"
            f"# Source: {template_name} + 05_Development/Deployment/.env.server\n"
            "# Contains live credentials in plain text. Never commit, never paste\n"
            "# into chat or a shared document. Transfer over scp and chmod 600.\n\n"
        )
        out = REPO / output_name
        out.write_text(banner + text, encoding="utf-8", newline="\n")

        status = "OK" if not missing else "NEEDS INPUT"
        print(f"{output_name:42s} {status}")
        for name in sorted(missing):
            failed = True
            print(f"    still to fill: {name}")

    if failed:
        print("\nSearch the output for REPLACE_ME__ and fill those in before deploying.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
