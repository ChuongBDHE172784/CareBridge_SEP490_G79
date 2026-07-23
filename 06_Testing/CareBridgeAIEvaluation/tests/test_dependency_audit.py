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


def test_dependency_audit_never_hides_vulnerability_findings(monkeypatch):
    module = _module()

    class Completed:
        returncode = 1
        stdout = "Found 1 known vulnerability in 1 package"
        stderr = "Connection error: read timed out"

    monkeypatch.setattr(module.subprocess, "run", lambda *args, **kwargs: Completed())
    assert module.main() == 1


def test_dependency_audit_times_out_as_infrastructure_skip(monkeypatch):
    module = _module()

    def timeout(*args, **kwargs):
        raise module.subprocess.TimeoutExpired(cmd="pip-audit", timeout=300)

    monkeypatch.setattr(module.subprocess, "run", timeout)
    assert module.main() == 0

