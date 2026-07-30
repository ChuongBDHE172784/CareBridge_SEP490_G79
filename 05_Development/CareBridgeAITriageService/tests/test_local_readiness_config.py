from pathlib import Path


AI_SERVICE_ROOT = Path(__file__).resolve().parents[1]
DEVELOPMENT_ROOT = AI_SERVICE_ROOT.parent
AI_ENV_EXAMPLE = AI_SERVICE_ROOT / ".env.example"
API_ENV_EXAMPLE = DEVELOPMENT_ROOT / "CareBridgeAPI" / ".env.example"
AI_README = AI_SERVICE_ROOT / "README.md"


def _env_values(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key] = value
    return values


def test_local_env_templates_wire_ai_service_to_backend_registry_without_secret():
    ai_env = _env_values(AI_ENV_EXAMPLE)
    api_env = _env_values(API_ENV_EXAMPLE)

    assert ai_env["AI_TRIAGE_EVIDENCE_REGISTRY_URL"] == "http://localhost:8080"
    assert ai_env["AI_TRIAGE_EVIDENCE_REGISTRY_CACHE_SECONDS"] == "300"
    assert api_env["AI_TRIAGE_SERVICE_URL"] == "http://localhost:8001"
    assert ai_env["AI_TRIAGE_INTERNAL_API_KEY"] == ""
    assert api_env["AI_TRIAGE_INTERNAL_API_KEY"] == ""


def test_local_readme_documents_safe_start_order_and_shared_key_contract():
    content = AI_README.read_text(encoding="utf-8")

    assert "start CareBridgeAPI first" in content
    assert "set the same\nnon-empty `AI_TRIAGE_INTERNAL_API_KEY`" in content
    assert "never commit either" in content
    assert "registry deliberately\nfails closed" in content
