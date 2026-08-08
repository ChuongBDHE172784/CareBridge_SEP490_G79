import json
from urllib.error import URLError

import pytest

from app import evidence_registry_client as registry


approved_sources_for_stage = registry.approved_sources_for_stage


class FakeResponse:
    def __init__(self, status: int, payload: object):
        self.status = status
        self._body = json.dumps(payload).encode("utf-8")

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        return False

    def read(self) -> bytes:
        return self._body


@pytest.fixture(autouse=True)
def clear_cache():
    registry.clear_registry_cache()
    yield
    registry.clear_registry_cache()


def test_registry_missing_configuration_fails_closed_without_network(monkeypatch):
    monkeypatch.setattr(registry, "EVIDENCE_REGISTRY_URL", "")
    monkeypatch.setattr(registry, "EVIDENCE_REGISTRY_INTERNAL_KEY", "")

    def unexpected_request(*args, **kwargs):
        raise AssertionError("network must not be called without registry configuration")

    monkeypatch.setattr(registry, "urlopen", unexpected_request)

    assert approved_sources_for_stage("infant") == ()


@pytest.mark.parametrize(
    "response",
    [
        FakeResponse(503, {"success": True, "data": []}),
        FakeResponse(200, {"success": False, "data": []}),
        FakeResponse(200, {"success": True, "data": "not-a-list"}),
    ],
)
def test_registry_non_success_or_malformed_response_fails_closed(monkeypatch, response):
    monkeypatch.setattr(registry, "EVIDENCE_REGISTRY_URL", "http://localhost:8080")
    monkeypatch.setattr(registry, "EVIDENCE_REGISTRY_INTERNAL_KEY", "test-only-key")
    monkeypatch.setattr(registry, "urlopen", lambda request, timeout: response)

    assert approved_sources_for_stage("INFANT") == ()


def test_registry_network_error_fails_closed(monkeypatch):
    monkeypatch.setattr(registry, "EVIDENCE_REGISTRY_URL", "http://localhost:8080")
    monkeypatch.setattr(registry, "EVIDENCE_REGISTRY_INTERNAL_KEY", "test-only-key")

    def unavailable(request, timeout):
        raise URLError("offline")

    monkeypatch.setattr(registry, "urlopen", unavailable)

    assert approved_sources_for_stage("INFANT") == ()


def test_unreachable_registry_is_attempted_once_per_failure_ttl(monkeypatch):
    """One outage must not cost one connect timeout per candidate source.

    Callers ask per source and twice per source, so an uncached failure multiplied a single
    unreachable registry into 13 lookups and 52s on one RED turn.
    """
    monkeypatch.setattr(registry, "EVIDENCE_REGISTRY_URL", "http://localhost:8080")
    monkeypatch.setattr(registry, "EVIDENCE_REGISTRY_INTERNAL_KEY", "test-only-key")
    attempts = []

    def unavailable(request, timeout):
        attempts.append(timeout)
        raise URLError("offline")

    monkeypatch.setattr(registry, "urlopen", unavailable)

    for _ in range(13):
        assert approved_sources_for_stage("INFANT") == ()

    assert len(attempts) == 1


def test_failure_is_retried_once_the_short_ttl_expires(monkeypatch):
    """A remembered failure must expire quickly — a recovered registry cannot stay unseen."""
    monkeypatch.setattr(registry, "EVIDENCE_REGISTRY_URL", "http://localhost:8080")
    monkeypatch.setattr(registry, "EVIDENCE_REGISTRY_INTERNAL_KEY", "test-only-key")
    monkeypatch.setattr(registry, "EVIDENCE_REGISTRY_FAILURE_CACHE_SECONDS", 30)
    clock = [1_000.0]
    monkeypatch.setattr(registry.time, "monotonic", lambda: clock[0])
    attempts = []

    def unavailable(request, timeout):
        attempts.append(timeout)
        raise URLError("offline")

    monkeypatch.setattr(registry, "urlopen", unavailable)

    assert approved_sources_for_stage("INFANT") == ()
    clock[0] += 29
    assert approved_sources_for_stage("INFANT") == ()
    assert len(attempts) == 1

    clock[0] += 2
    assert approved_sources_for_stage("INFANT") == ()
    assert len(attempts) == 2


def test_a_successful_answer_is_not_dropped_after_the_failure_ttl(monkeypatch):
    """The short TTL belongs to failures only; a real answer keeps the long one."""
    monkeypatch.setattr(registry, "EVIDENCE_REGISTRY_URL", "http://localhost:8080")
    monkeypatch.setattr(registry, "EVIDENCE_REGISTRY_INTERNAL_KEY", "test-only-key")
    monkeypatch.setattr(registry, "EVIDENCE_REGISTRY_FAILURE_CACHE_SECONDS", 30)
    monkeypatch.setattr(registry, "EVIDENCE_REGISTRY_CACHE_SECONDS", 300)
    clock = [1_000.0]
    monkeypatch.setattr(registry.time, "monotonic", lambda: clock[0])
    calls = []

    def responder(request, timeout):
        calls.append(timeout)
        return FakeResponse(200, {"success": True, "data": [{
            "id": "who-1", "domain": "who.int", "baseUrl": "https://who.int/x",
            "organization": "WHO", "applicableStages": ["INFANT"],
        }]})

    monkeypatch.setattr(registry, "urlopen", responder)

    assert len(approved_sources_for_stage("INFANT")) == 1
    clock[0] += 60  # past the failure TTL, well inside the success TTL
    assert len(approved_sources_for_stage("INFANT")) == 1
    assert len(calls) == 1


def test_registry_returns_only_valid_https_sources_for_requested_stage(monkeypatch):
    monkeypatch.setattr(registry, "EVIDENCE_REGISTRY_URL", "http://localhost:8080")
    monkeypatch.setattr(registry, "EVIDENCE_REGISTRY_INTERNAL_KEY", "test-only-key")
    payload = {
        "success": True,
        "data": [
            {
                "id": "WHO_1",
                "domain": "www.who.int",
                "baseUrl": "https://www.who.int/health-topics/child-health",
                "organization": "WHO",
                "applicableStages": ["INFANT"],
            },
            {
                "id": "HTTP_ONLY",
                "domain": "example.org",
                "baseUrl": "http://example.org/source",
                "organization": "Example",
                "applicableStages": ["INFANT"],
            },
            {
                "id": "WRONG_STAGE",
                "domain": "moh.gov.vn",
                "baseUrl": "https://moh.gov.vn/health",
                "organization": "MOH",
                "applicableStages": ["PREGNANCY"],
            },
        ],
    }
    captured = {}

    def successful(request, timeout):
        captured["url"] = request.full_url
        captured["key"] = request.get_header("X-carebridge-internal-key")
        captured["timeout"] = timeout
        return FakeResponse(200, payload)

    monkeypatch.setattr(registry, "urlopen", successful)

    sources = approved_sources_for_stage("infant")

    assert [source.id for source in sources] == ["WHO_1"]
    assert sources[0].domain == "who.int"
    assert captured == {
        "url": "http://localhost:8080/internal/api/v1/triage/evidence-sources/approved?stage=INFANT",
        "key": "test-only-key",
        "timeout": 2.0,
    }
