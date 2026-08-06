from __future__ import annotations

from fastapi.testclient import TestClient

import app.triage_v2.api as api
from app.main import app
from app.rules.registry import get_registry
from app.triage_v2.extraction import TriageV2Extraction


client = TestClient(app)


def _request(**overrides):
    registry = get_registry()
    payload = {
        "sessionId": "8bfe7690-6db2-4c50-8172-43e3e0823001",
        "stateVersion": 0,
        "expectedStateVersion": 0,
        "requestId": "request_1234567890",
        "messageId": "message_1234567890",
        "latestUserMessage": "Tôi khó thở dữ dội",
        "selectedTarget": "UNKNOWN",
        "journeyContext": {},
        "signals": {"SEVERE_BREATHING_DIFFICULTY": "PRESENT"},
        "measurements": {},
        "expectedRulesetHash": registry.ruleset_sha256,
    }
    payload.update(overrides)
    return payload


def test_internal_endpoint_is_disabled_without_shared_secret(monkeypatch):
    monkeypatch.setattr(api, "TRIAGE_V2_INTERNAL_API_KEY", "")
    response = client.post("/internal/triage/v2/turn", json=_request())
    assert response.status_code == 503


def test_internal_endpoint_rejects_wrong_secret(monkeypatch):
    monkeypatch.setattr(api, "TRIAGE_V2_INTERNAL_API_KEY", "expected-secret")
    response = client.post(
        "/internal/triage/v2/turn",
        headers={"X-CareBridge-Internal-Key": "wrong-secret"},
        json=_request(),
    )
    assert response.status_code == 403


def test_turn_returns_deterministic_red_and_no_links(monkeypatch):
    monkeypatch.setattr(api, "TRIAGE_V2_INTERNAL_API_KEY", "expected-secret")
    response = client.post(
        "/internal/triage/v2/turn",
        headers={"X-CareBridge-Internal-Key": "expected-secret"},
        json=_request(),
    )
    assert response.status_code == 200
    state = response.json()["state"]
    assert state["triageOutcome"] == "RED"
    assert state["stopConversation"] is True
    assert state["readingLinks"] == []


def test_hash_mismatch_fails_before_partial_execution(monkeypatch):
    monkeypatch.setattr(api, "TRIAGE_V2_INTERNAL_API_KEY", "expected-secret")
    response = client.post(
        "/internal/triage/v2/turn",
        headers={"X-CareBridge-Internal-Key": "expected-secret"},
        json=_request(expectedRulesetHash="0" * 64),
    )
    assert response.status_code == 409


def test_persisted_state_identity_cannot_be_swapped(monkeypatch):
    monkeypatch.setattr(api, "TRIAGE_V2_INTERNAL_API_KEY", "expected-secret")
    previous = api.execute_turn(api.TriageV2TurnRequest.model_validate(_request())).state
    response = client.post(
        "/internal/triage/v2/turn",
        headers={"X-CareBridge-Internal-Key": "expected-secret"},
        json=_request(
            sessionId="8bfe7690-6db2-4c50-8172-43e3e0823002",
            previousState=previous,
        ),
    )
    assert response.status_code == 409


def test_free_text_cannot_hide_in_structured_signal_or_measurement(monkeypatch):
    monkeypatch.setattr(api, "TRIAGE_V2_INTERNAL_API_KEY", "expected-secret")
    for field, value in (
        ("signals", {"SEIZURE": {"presence": "PRESENT", "note": "PII raw text"}}),
        ("measurements", {"TEMPERATURE": {"value": 38.5, "note": "PII raw text"}}),
    ):
        response = client.post(
            "/internal/triage/v2/turn",
            headers={"X-CareBridge-Internal-Key": "expected-secret"},
            json=_request(**{field: value}),
        )
        assert response.status_code == 422


def test_sensitive_text_encoded_as_identifier_is_not_a_canonical_code(monkeypatch):
    monkeypatch.setattr(api, "TRIAGE_V2_INTERNAL_API_KEY", "expected-secret")
    for field, value in (
        ("signals", {"PATIENT_JANE_DOE_HAS_HIV": "PRESENT"}),
        ("measurements", {"JANE_DOE_PRIVATE_VALUE": 38.5}),
    ):
        response = client.post(
            "/internal/triage/v2/turn",
            headers={"X-CareBridge-Internal-Key": "expected-secret"},
            json=_request(**{field: value}),
        )
        assert response.status_code == 422


def test_normalized_historical_presence_shape_matches_java_boundary(monkeypatch):
    monkeypatch.setattr(api, "TRIAGE_V2_INTERNAL_API_KEY", "expected-secret")
    response = client.post(
        "/internal/triage/v2/turn",
        headers={"X-CareBridge-Internal-Key": "expected-secret"},
        json=_request(signals={
            "SEIZURE": {"presence": "ABSENT", "temporalStatus": "CURRENT",
                        "historicalPresence": "PRESENT"}
        }),
    )
    assert response.status_code == 200


def test_selected_target_and_journey_context_reach_deterministic_resolvers(monkeypatch):
    monkeypatch.setattr(api, "TRIAGE_V2_INTERNAL_API_KEY", "expected-secret")
    monkeypatch.setattr(api, "get_gemini_client", lambda: None)
    response = client.post(
        "/internal/triage/v2/turn",
        headers={"X-CareBridge-Internal-Key": "expected-secret"},
        json=_request(
            latestUserMessage="Chua ro",
            signals={},
            selectedTarget="BABY",
            journeyContext={"stage": "INFANT_0_12M", "babyAgeMonths": 6},
        ),
    )

    assert response.status_code == 200
    state = response.json()["state"]
    assert state["targetEntity"] == "BABY"
    assert state["targetEntitySource"] == "EXPLICIT_SELECTED_PROFILE"
    assert state["stage"] == "INFANT_0_12M"
    assert state["babyAgeMonths"] == 6
    assert state["triageOutcome"] != "GREEN"


def test_journey_context_is_closed_and_cannot_hide_health_text(monkeypatch):
    monkeypatch.setattr(api, "TRIAGE_V2_INTERNAL_API_KEY", "expected-secret")
    response = client.post(
        "/internal/triage/v2/turn",
        headers={"X-CareBridge-Internal-Key": "expected-secret"},
        json=_request(journeyContext={"note": "private health text"}),
    )

    assert response.status_code == 422


def test_grounded_extraction_reenters_deterministic_global_safety(monkeypatch):
    text = "Tôi bị co giật"
    start = text.index("co giật")
    extraction = TriageV2Extraction.model_validate({
        "targetEntityCandidate": "MOTHER",
        "targetEvidenceSpans": [{"text": "Tôi", "start": 0, "end": 3}],
        "intentCandidate": "SYMPTOM_TRIAGE",
        "intentEvidenceSpans": [{"text": text, "start": 0, "end": len(text)}],
        "stageClues": [],
        "symptomCandidates": [{"code": "SEIZURE", "evidenceSpanIndexes": [0],
                               "confidence": 0.99}],
        "symptomEvidenceSpans": [{"text": "co giật", "start": start,
                                  "end": start + len("co giật")}],
        "measurements": [], "temporalExpressions": [], "explicitNegations": [],
        "currentVsHistorical": [{"symptomCandidateIndex": 0, "status": "CURRENT"}],
        "conflictCandidates": [], "confidence": 0.95, "unknownFields": [],
        "language": "vi", "parserWarnings": [],
    })

    class FakeExtractor:
        def extract_triage_v2(self, *, text: str):
            return extraction

    monkeypatch.setattr(api, "TRIAGE_V2_INTERNAL_API_KEY", "expected-secret")
    monkeypatch.setattr(api, "get_gemini_client", lambda: FakeExtractor())
    response = client.post(
        "/internal/triage/v2/turn",
        headers={"X-CareBridge-Internal-Key": "expected-secret"},
        json=_request(
            latestUserMessage=text,
            signals={"SEIZURE": {"presence": "ABSENT"}},
        ),
    )
    assert response.status_code == 200
    assert response.json()["state"]["triageOutcome"] == "RED"


def test_evidence_failure_cannot_erase_completed_yellow(monkeypatch):
    class FakeGraph:
        def invoke(self, state, config):
            return {**state, "triageOutcome": "YELLOW", "stage": "PREGNANCY",
                    "citations": [], "stopConversation": True}

    monkeypatch.setattr(api, "TRIAGE_V2_INTERNAL_API_KEY", "expected-secret")
    monkeypatch.setattr(api, "get_gemini_client", lambda: None)
    monkeypatch.setattr(api, "build_triage_v2_graph", lambda: FakeGraph())
    monkeypatch.setattr(api, "approved_domains", lambda _stage: (_ for _ in ()).throw(
        RuntimeError("registry unavailable")
    ))

    response = client.post(
        "/internal/triage/v2/turn",
        headers={"X-CareBridge-Internal-Key": "expected-secret"},
        json=_request(signals={}, latestUserMessage="Chưa rõ"),
    )

    assert response.status_code == 200
    assert response.json()["state"]["triageOutcome"] == "YELLOW"
    assert response.json()["state"]["citations"] == []


def test_existing_global_red_never_calls_gemini(monkeypatch):
    monkeypatch.setattr(api, "TRIAGE_V2_INTERNAL_API_KEY", "expected-secret")

    def forbidden():
        raise AssertionError("Gemini must not run before explicit RED")

    monkeypatch.setattr(api, "get_gemini_client", forbidden)
    monkeypatch.setattr(api, "approved_domains", lambda _stage: (_ for _ in ()).throw(
        AssertionError("RED must not wait for evidence registry or RAG")
    ))
    response = client.post(
        "/internal/triage/v2/turn",
        headers={"X-CareBridge-Internal-Key": "expected-secret"},
        json=_request(),
    )
    assert response.status_code == 200
    assert response.json()["state"]["triageOutcome"] == "RED"


def test_answered_question_ids_must_exist_in_the_canonical_catalogue(monkeypatch):
    """Only the Java mapper may name an answered question, and only a real one."""

    monkeypatch.setattr(api, "TRIAGE_V2_INTERNAL_API_KEY", "expected-secret")
    response = client.post(
        "/internal/triage/v2/turn",
        headers={"X-CareBridge-Internal-Key": "expected-secret"},
        json=_request(answeredQuestionIds=["Q_NOT_A_REAL_QUESTION"]),
    )
    assert response.status_code == 422


def test_answered_questions_accumulate_without_duplicates():
    """The planner stops re-asking only if answers are actually recorded and never doubled."""

    state = api._turn_state(api.TriageV2TurnRequest(**_request(
        answeredQuestionIds=["Q_DIZZINESS"], signals={},
    )))
    assert state["answeredQuestionIds"] == ["Q_DIZZINESS"]

    resumed = api._turn_state(api.TriageV2TurnRequest(**_request(
        stateVersion=1, expectedStateVersion=1, signals={},
        answeredQuestionIds=["Q_DIZZINESS", "Q_CLOTS"],
        previousState={**state, "stateVersion": 1},
    )))
    assert resumed["answeredQuestionIds"] == ["Q_DIZZINESS", "Q_CLOTS"]
