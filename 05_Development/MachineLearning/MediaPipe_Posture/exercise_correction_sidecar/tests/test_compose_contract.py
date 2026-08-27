from pathlib import Path

import yaml


REPO_ROOT = Path(__file__).resolve().parents[5]
OVERLAY = REPO_ROOT / "05_Development" / "Deployment" / "docker-compose.exercise-ml.yml"


def test_sidecar_is_private_and_health_gates_backend() -> None:
    compose = yaml.safe_load(OVERLAY.read_text(encoding="utf-8"))
    service = compose["services"]["exercise-correction"]
    backend = compose["services"]["backend"]

    assert "ports" not in service
    assert service["expose"] == ["8002"]
    assert service["networks"] == ["exercise-inference"]
    assert service["read_only"] is True
    assert service["cap_drop"] == ["ALL"]
    assert compose["networks"]["exercise-inference"]["internal"] is True
    assert backend["environment"]["CAREBRIDGE_EXERCISE_CORRECTION_ENABLED"] == "true"
    assert backend["environment"]["EXERCISE_CORRECTION_SERVICE_URL"] == "http://exercise-correction:8002"
    assert backend["depends_on"]["exercise-correction"]["condition"] == "service_healthy"
