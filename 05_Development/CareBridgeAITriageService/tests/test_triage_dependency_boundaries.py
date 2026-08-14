from __future__ import annotations

import ast
from pathlib import Path


TRIAGE_ROOT = Path(__file__).parents[1] / "app" / "triage"


def test_canonical_engine_never_imports_removed_graph_or_open_web_modules():
    forbidden = {
        "app.graph", "app.source_retriever", "app.official_source_searcher",
        "app.risk_rules",
    }
    violations: list[str] = []
    for path in sorted(TRIAGE_ROOT.glob("*.py")):
        tree = ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
        for node in ast.walk(tree):
            if isinstance(node, ast.ImportFrom) and node.module in forbidden:
                violations.append(f"{path.name}:{node.lineno}:{node.module}")
            if isinstance(node, ast.Import):
                violations.extend(
                    f"{path.name}:{node.lineno}:{alias.name}"
                    for alias in node.names if alias.name in forbidden
                )
    assert violations == []


def test_canonical_engine_has_no_runtime_web_or_autonomous_agent_primitive():
    forbidden_names = {"urlopen", "requests", "Agent", "create_react_agent"}
    violations: list[str] = []
    for path in sorted(TRIAGE_ROOT.glob("*.py")):
        tree = ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
        for node in ast.walk(tree):
            if isinstance(node, ast.Name) and node.id in forbidden_names:
                violations.append(f"{path.name}:{node.lineno}:{node.id}")
    assert violations == []
