from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_health_reports_deterministic_engine_readiness_without_requiring_gemini():
    response = client.get("/health")

    assert response.status_code == 200
    payload = response.json()
    assert payload["status"] == "UP"
    assert payload["readiness"] == "READY"
    assert payload["deterministicEngine"] == {
        "ready": True,
        "probe": "POSTPARTUM_RED_PRECEDENCE",
    }
    assert "geminiEnabled" in payload
    assert "geminiConfigured" in payload
    assert "geminiReachable" in payload


def test_health_fails_closed_when_the_deterministic_engine_probe_fails(monkeypatch):
    monkeypatch.setattr(
        "app.main._run_deterministic_readiness_probe",
        lambda: (_ for _ in ()).throw(RuntimeError("synthetic probe failure")),
    )

    response = client.get("/health")

    assert response.status_code == 503
    payload = response.json()
    assert payload["status"] == "DOWN"
    assert payload["readiness"] == "NOT_READY"
    assert payload["deterministicEngine"]["ready"] is False
    assert "synthetic probe failure" not in response.text


def test_health_does_not_require_the_optional_gemini_client(monkeypatch):
    monkeypatch.setattr(
        "app.main.get_gemini_client",
        lambda: (_ for _ in ()).throw(RuntimeError("optional provider unavailable")),
    )

    response = client.get("/health")

    assert response.status_code == 200
    payload = response.json()
    assert payload["status"] == "UP"
    assert payload["deterministicEngine"]["ready"] is True
    assert payload["geminiReachable"] is False
    assert "optional provider unavailable" not in response.text
