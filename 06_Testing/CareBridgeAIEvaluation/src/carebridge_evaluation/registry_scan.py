"""Static guard against reintroducing a hardcoded evidence-domain registry."""

from __future__ import annotations

import ast
import re
from pathlib import Path

REGISTRY_NAME = re.compile(r"(?:whitelist|approved_domains|allowed_domains|official_domains)", re.IGNORECASE)
DOMAIN_LITERAL = re.compile(r"^(?:[a-z0-9-]+\.)+[a-z]{2,}$", re.IGNORECASE)


def scan_hardcoded_registry(ai_service_path: Path) -> list[str]:
    violations: list[str] = []
    app_dir = ai_service_path / "app"
    for path in sorted(app_dir.glob("*.py")):
        try:
            tree = ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
        except (OSError, SyntaxError) as exc:
            violations.append(f"Unable to inspect {path.name}: {type(exc).__name__}")
            continue
        for node in ast.walk(tree):
            if not isinstance(node, (ast.Assign, ast.AnnAssign)):
                continue
            names = _assignment_names(node)
            if not any(REGISTRY_NAME.search(name) for name in names):
                continue
            value = node.value
            domains = [
                item.value
                for item in ast.walk(value)
                if isinstance(item, ast.Constant)
                and isinstance(item.value, str)
                and DOMAIN_LITERAL.fullmatch(item.value)
            ]
            if domains:
                violations.append(f"{path.name}:{node.lineno} hardcodes evidence domains: {', '.join(domains)}")
    return violations


def _assignment_names(node: ast.Assign | ast.AnnAssign) -> list[str]:
    targets = node.targets if isinstance(node, ast.Assign) else [node.target]
    return [target.id for target in targets if isinstance(target, ast.Name)]

