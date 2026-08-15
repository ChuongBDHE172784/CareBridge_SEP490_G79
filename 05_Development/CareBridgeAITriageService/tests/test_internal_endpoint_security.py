from fastapi.testclient import TestClient

from app.main import app
from app.triage import api as triage_api


client = TestClient(app)


def test_canonical_transport_requires_the_shared_internal_key(monkeypatch):
    monkeypatch.setattr(triage_api, "TRIAGE_INTERNAL_API_KEY", "expected-secret")

    assert client.post("/internal/triage/turn", json={}).status_code == 403


def test_legacy_clinical_graph_routes_are_not_reachable():
    for path in ("/triage/child", "/triage/intake/start", "/triage/intake/continue"):
        assert client.post(path, json={}).status_code == 404


def test_health_remains_available_without_an_internal_key():
    assert client.get("/health").status_code in {200, 503}
