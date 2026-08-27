import importlib.util
from pathlib import Path
from types import SimpleNamespace

SCRIPT = Path(__file__).parents[1] / "scripts" / "run_promptfoo.py"


def _module():
    spec = importlib.util.spec_from_file_location("run_promptfoo", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_local_promptfoo_does_not_require_api_or_external_key(monkeypatch):
    module = _module()
    for name in (*module.API_FIXTURES, "GEMINI_API_KEY"):
        monkeypatch.delenv(name, raising=False)
    monkeypatch.setattr(module.shutil, "which", lambda _: "promptfoo")

    status, _ = module.promptfoo_status()

    assert status == "READY"


def test_missing_promptfoo_fails_the_runner(monkeypatch, tmp_path):
    module = _module()
    monkeypatch.setattr(module.shutil, "which", lambda _: None)
    monkeypatch.setattr(module.argparse.ArgumentParser, "parse_args", lambda _self: SimpleNamespace(output_dir=tmp_path))

    assert module.main() == 1


def test_rag_promptfoo_without_all_api_fixtures_is_infrastructure_skipped(monkeypatch):
    module = _module()
    for name in module.API_FIXTURES:
        monkeypatch.delenv(name, raising=False)

    status, reason = module.rag_status()

    assert status == "INFRASTRUCTURE_SKIPPED"
    assert "CAREBRIDGE_TEST_MOTHER_PROFILE_ID" in reason


def test_rag_promptfoo_skips_when_api_preflight_is_not_ready(monkeypatch):
    module = _module()
    for name in module.API_FIXTURES:
        monkeypatch.setenv(name, "configured-test-value")
    monkeypatch.setattr(
        module.ApiPreflight,
        "run",
        lambda _self: SimpleNamespace(ready=False, reason="required schema is unavailable"),
    )

    status, reason = module.rag_status()

    assert status == "INFRASTRUCTURE_SKIPPED"
    assert "required schema is unavailable" in reason


def test_exactly_four_promptfoo_configs_exist():
    promptfoo = SCRIPT.parents[1] / "promptfoo"
    configs = {path.name for path in promptfoo.glob("*.yaml")}
    assert configs == {
        "pediatric-safety.yaml",
        "legal-safety.yaml",
        "prompt-injection.yaml",
        "rag-citation.yaml",
    }
