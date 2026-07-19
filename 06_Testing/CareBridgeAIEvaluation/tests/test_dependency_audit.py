import importlib.util
from pathlib import Path

SCRIPT = Path(__file__).parents[1] / "scripts" / "run_dependency_audit.py"


def _module():
    spec = importlib.util.spec_from_file_location("run_dependency_audit", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_dependency_audit_distinguishes_index_outage_from_vulnerability(monkeypatch):
    module = _module()

    class Completed:
        returncode = 1
        stdout = ""
        stderr = "Connection error: read timed out"

    monkeypatch.setattr(module.subprocess, "run", lambda *args, **kwargs: Completed())
    assert module.main() == 0

