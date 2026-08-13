from fastapi.testclient import TestClient

from app.main import app
from app.triage_v2 import api as triage_v2_api


client = TestClient(app)


def test_all_triage_endpoints_require_the_shared_internal_key(monkeypatch):
    monkeypatch.setattr(triage_v2_api, "TRIAGE_V2_INTERNAL_API_KEY", "expected-secret")

    for path, payload in (
        ("/triage/child", {"stage": "INFANT"}),
        ("/triage/intake/start", {"stage": "INFANT", "currentIntake": {"stage": "INFANT"}}),
        ("/triage/intake/continue", {}),
    ):
        assert client.post(path, json=payload).status_code == 403


def test_health_remains_available_without_an_internal_key():
    assert client.get("/health").status_code in {200, 503}
