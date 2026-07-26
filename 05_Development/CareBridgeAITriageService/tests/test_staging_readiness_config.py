from pathlib import Path

import yaml


STAGING_COMPOSE = (
    Path(__file__).resolve().parents[2] / "Deployment" / "docker-compose.staging.yml"
)

AI_ALLOWED_ENV_KEYS = {
    "AI_TRIAGE_EVIDENCE_REGISTRY_URL",
    "AI_TRIAGE_INTERNAL_API_KEY",
    "AI_TRIAGE_EVIDENCE_REGISTRY_CACHE_SECONDS",
    "AI_TRIAGE_SERVICE_TIMEOUT_SECONDS",
    "AI_TRIAGE_REALTIME_TIMEOUT_SECONDS",
    "AI_TRIAGE_EVIDENCE_CACHE_TTL_DAYS",
    "GEMINI_ENABLED",
    "GEMINI_API_KEY",
    "GEMINI_MODEL",
    "GEMINI_TIMEOUT_SECONDS",
    "GEMINI_MAX_RETRIES",
    "GEMINI_TEMPERATURE",
}
AI_REQUIRED_ENV_KEYS = {
    "AI_TRIAGE_EVIDENCE_REGISTRY_URL",
    "AI_TRIAGE_INTERNAL_API_KEY",
}


def test_staging_compose_binds_services_to_real_readiness_checks():
    content = STAGING_COMPOSE.read_text(encoding="utf-8")

    assert "${AI_IMAGE:?" in content
    assert "http://127.0.0.1:8001/health" in content
    assert "AI_TRIAGE_SERVICE_URL: http://ai-service:8001" in content
    assert "http://127.0.0.1:8080/actuator/health/readiness" in content
    assert content.count("condition: service_healthy") >= 2


def test_staging_compose_does_not_fall_back_to_process_only_health():
    content = STAGING_COMPOSE.read_text(encoding="utf-8")

    assert "kill -0 1" not in content
    assert "condition: service_started" not in content


def test_ai_service_receives_only_explicitly_allowlisted_ai_configuration():
    compose = yaml.safe_load(STAGING_COMPOSE.read_text(encoding="utf-8"))
    ai_service = compose["services"]["ai-service"]

    assert "env_file" not in ai_service
    assert set(ai_service["environment"]) == AI_ALLOWED_ENV_KEYS

    rendered_ai_config = yaml.safe_dump(ai_service, sort_keys=True)
    assert "JWT_" not in rendered_ai_config
    assert "SUPABASE_DB_" not in rendered_ai_config


def test_ai_service_required_evidence_registry_contract_fails_closed():
    compose = yaml.safe_load(STAGING_COMPOSE.read_text(encoding="utf-8"))
    environment = compose["services"]["ai-service"]["environment"]

    for key in AI_REQUIRED_ENV_KEYS:
        assert environment[key].startswith(f"${{{key}:?")
