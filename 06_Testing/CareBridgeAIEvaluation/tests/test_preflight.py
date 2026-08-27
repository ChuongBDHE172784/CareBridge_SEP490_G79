from pathlib import Path

from carebridge_evaluation.config import EvaluationSettings
from carebridge_evaluation.preflight import ApiPreflight, is_explicit_non_production_url


def settings(**overrides):
    values = {
        "api_base_url": None,
        "test_jwt": None,
        "ai_service_path": Path("."),
    }
    values.update(overrides)
    return EvaluationSettings(**values)


def test_non_production_url_guard_is_fail_closed():
    assert is_explicit_non_production_url("http://localhost:8080")
    assert is_explicit_non_production_url("https://api.carebridge.test")
    assert is_explicit_non_production_url("http://192.168.1.20:8080")
    assert not is_explicit_non_production_url("https://api.carebridge.vn")
    assert not is_explicit_non_production_url("https://example.com")


def test_missing_jwt_is_infrastructure_preflight_failure():
    result = ApiPreflight(settings(api_base_url="http://localhost:8080")).run()
    assert not result.ready
    assert "jwtConfigured" in result.reason

