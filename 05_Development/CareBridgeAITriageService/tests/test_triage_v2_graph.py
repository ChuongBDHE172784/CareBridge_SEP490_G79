"""P2-T12 complete deterministic V2 graph tests."""

from __future__ import annotations

import inspect
import json

import pytest
from langgraph.types import Command

from app.context import CareStage
from app.rules.registry import load_dataset_requirements
from app.rules.registry import get_registry
from app.triage_v2.api import TriageV2TurnRequest, _turn_state
from app.triage_v2.graph import build_triage_v2_graph, graph_config, triage_v2_graph
from app.triage_v2.state import create_initial_state


def _state(message: str):
    return create_initial_state(
        session_id="session-123",
        state_version=1,
        request_id="request-123",
        message_id="message-123",
        latest_user_message=message,
    )


def _complete_global(state):
    for code in load_dataset_requirements()["globalSafety"]["requiredSignalCodes"]:
        state["signals"][code] = {"presence": "ABSENT"}


def _invoke(state):
    return triage_v2_graph.invoke(state, graph_config(state["sessionId"]))


@pytest.mark.parametrize(
    ("message", "expected_target"),
    [("Tôi bị tiêu chảy", "MOTHER"), ("Bé bị tiêu chảy", "BABY")],
)
def test_mother_and_baby_flows_never_green_or_cross_target(message, expected_target):
    result = _invoke(_state(message))

    assert result["targetEntity"] == expected_target
    assert result["triageOutcome"] != "GREEN"
    assert result["readingLinks"] == []


@pytest.mark.parametrize("message", ["Bị sốt 38.5 độ", "Tôi và bé đều bị sốt"])
def test_ambiguous_and_multi_entity_fever_never_guess_or_green(message):
    result = _invoke(_state(message))

    assert result["targetEntity"] in {"UNKNOWN", "CONFLICTED"}
    assert result["triageOutcome"] != "GREEN"


def test_global_red_target_unknown_is_action_first_without_questions():
    state = _state("Khó thở dữ dội")
    state["signals"]["SEVERE_BREATHING_DIFFICULTY"] = {"presence": "PRESENT"}

    result = _invoke(state)

    assert result["triageOutcome"] == "RED"
    assert result["stopConversation"] is True
    assert result["plannedQuestionIds"] == []
    assert result["finalResponse"]["action"] == "IMMEDIATE_EMERGENCY_ASSESSMENT"


def test_positive_wrist_oos_stops_in_one_pass():
    state = _state("Tôi bị đau cổ tay sau khi tập thể thao")
    state.update(stage=CareStage.PRECONCEPTION, possiblePregnancy="NO")
    _complete_global(state)

    result = _invoke(state)

    assert result["triageOutcome"] == "OUT_OF_SCOPE"
    assert result["stopConversation"] is True
    assert result["questionRound"] == 0


def test_red_on_second_or_third_follow_up_reenters_global_gate():
    for round_number in (2, 3):
        state = _state("follow up")
        state["questionRound"] = round_number
        state["signals"]["SEIZURE"] = {"presence": "PRESENT"}

        result = _invoke(state)

        assert result["triageOutcome"] == "RED"
        assert result["questionRound"] == round_number


def test_duplicate_and_stale_requests_do_not_advance_question_round():
    duplicate = _state("Bị sốt 38.5 độ")
    duplicate["processedMessageIds"] = [duplicate["messageId"]]
    duplicate_result = _invoke(duplicate)
    assert duplicate_result["questionRound"] == 0
    assert duplicate_result["requiredAction"] == "DUPLICATE_REQUEST"

    stale = _state("Bị sốt 38.5 độ")
    stale["expectedStateVersion"] = 0
    stale_result = _invoke(stale)
    assert stale_result["requiredAction"] == "STATE_VERSION_CONFLICT"
    assert stale_result["questionRound"] == 0


def test_unknown_symptom_and_every_other_path_are_non_green():
    result = _invoke(_state("Tôi thấy có gì đó không ổn"))
    assert result["triageOutcome"] != "GREEN"


def test_pending_red_at_round_three_stops_without_green():
    state = _state("Tôi đau đầu dữ dội")
    state.update(stage=CareStage.PREGNANCY, possiblePregnancy="YES", gestationalWeek=30)
    state["questionRound"] = 3
    state["signals"]["SEVERE_HEADACHE"] = {"presence": "PRESENT"}

    result = _invoke(state)

    assert result["triageOutcome"] == "NEEDS_MORE_INFO"
    assert result["stopConversation"] is True
    assert result["plannedQuestionIds"] == []


def test_registry_unavailable_is_rendered_as_controlled_non_green(monkeypatch):
    monkeypatch.setattr(
        "app.triage_v2.global_safety_gate.get_registry",
        lambda: (_ for _ in ()).throw(RuntimeError("registry details")),
    )

    result = _invoke(_state("Tôi thấy mệt"))

    assert result["triageOutcome"] == "NEEDS_MORE_INFO"
    assert result["requiredAction"] == "SYSTEM_UNAVAILABLE"
    assert result["finalResponse"]["outcome"] == "NEEDS_MORE_INFO"
    assert result["readingLinks"] == []


def test_real_interrupt_resume_reenters_validation_and_global_safety():
    state = _state("Bị sốt 38.5 độ")
    config = graph_config(state["sessionId"])
    first = triage_v2_graph.invoke(state, config)
    snapshot = triage_v2_graph.get_state(config)

    assert first["plannedQuestionIds"] == ["Q_CLARIFY_TARGET_ENTITY", "Q_GLOBAL_DANGER"]
    assert snapshot.interrupts

    resumed = triage_v2_graph.invoke(
        Command(
            resume={
                "requestId": "request-456",
                "messageId": "message-456",
                "latestUserMessage": "Tôi đang co giật",
                "signals": {"SEIZURE": {"presence": "PRESENT"}},
                "expectedStateVersion": 1,
            }
        ),
        config,
    )

    assert resumed["triageOutcome"] == "RED"
    assert resumed["stopConversation"] is True
    assert resumed["plannedQuestionIds"] == []


@pytest.mark.parametrize(
    ("resume_payload", "expected_action"),
    [
        (
            {
                "requestId": "request-123",
                "messageId": "message-123",
                "latestUserMessage": "Mẹ",
                "expectedStateVersion": 1,
            },
            "DUPLICATE_REQUEST",
        ),
        (
            {
                "requestId": "request-789",
                "messageId": "message-789",
                "latestUserMessage": "Mẹ",
                "expectedStateVersion": 0,
            },
            "STATE_VERSION_CONFLICT",
        ),
    ],
)
def test_real_resume_enforces_duplicate_and_stale_guards(resume_payload, expected_action):
    state = _state("Bị sốt 38.5 độ")
    state["sessionId"] = f"session-{expected_action.lower()}"
    config = graph_config(state["sessionId"])
    triage_v2_graph.invoke(state, config)

    resumed = triage_v2_graph.invoke(Command(resume=resume_payload), config)

    assert resumed["requiredAction"] == expected_action
    assert resumed["questionRound"] == 1


@pytest.mark.parametrize(
    "forbidden",
    [
        {"sessionId": "another-session"},
        {"stateVersion": 999},
        {"processedMessageIds": []},
        {"processedRequestIds": []},
        {"triageOutcome": "RED"},
    ],
)
def test_resume_schema_rejects_checkpoint_and_disposition_overrides(forbidden):
    state = _state("Bị sốt 38.5 độ")
    state["sessionId"] = f"session-forbidden-{next(iter(forbidden))}"
    config = graph_config(state["sessionId"])
    triage_v2_graph.invoke(state, config)
    payload = {
        "requestId": "request-new",
        "messageId": "message-new",
        "latestUserMessage": "Mẹ",
        "expectedStateVersion": 1,
        **forbidden,
    }

    with pytest.raises(ValueError, match="INVALID_RESUME_FIELDS"):
        triage_v2_graph.invoke(Command(resume=payload), config)


def test_resume_merges_signal_delta_without_erasing_prior_evidence_or_ledgers():
    state = _state("Bị sốt 38.5 độ")
    state["sessionId"] = "session-merge-delta"
    state["signals"]["VAGINAL_BLEEDING"] = {"presence": "ABSENT"}
    config = graph_config(state["sessionId"])
    triage_v2_graph.invoke(state, config)

    resumed = triage_v2_graph.invoke(
        Command(
            resume={
                "requestId": "request-new",
                "messageId": "message-new",
                "latestUserMessage": "Tôi vẫn mệt",
                "expectedStateVersion": 1,
                "signals": {"SEIZURE": {"presence": "ABSENT"}},
            }
        ),
        config,
    )

    assert "VAGINAL_BLEEDING" in resumed["signals"]
    assert "SEIZURE" in resumed["signals"]
    assert "request-123" in resumed["processedRequestIds"]
    assert "message-123" in resumed["processedMessageIds"]


def test_resume_rejects_checkpoint_thread_that_does_not_match_session_identity():
    state = _state("Bị sốt 38.5 độ")
    mismatched = graph_config("different-session-thread")
    triage_v2_graph.invoke(state, mismatched)

    with pytest.raises(ValueError, match="SESSION_THREAD_MISMATCH"):
        triage_v2_graph.invoke(
            Command(
                resume={
                    "requestId": "request-new",
                    "messageId": "message-new",
                    "latestUserMessage": "Mẹ",
                    "expectedStateVersion": 1,
                }
            ),
            mismatched,
        )


def test_resume_flattens_signal_observation_delta_so_new_danger_cannot_be_hidden():
    state = _state("Bị sốt 38.5 độ")
    state["sessionId"] = "session-flat-signal-delta"
    state["signals"]["SEIZURE"] = {"presence": "ABSENT"}
    config = graph_config(state["sessionId"])
    triage_v2_graph.invoke(state, config)

    resumed = triage_v2_graph.invoke(
        Command(
            resume={
                "requestId": "request-new",
                "messageId": "message-new",
                "latestUserMessage": "Tôi đang co giật",
                "expectedStateVersion": 1,
                "signals": {"SEIZURE": [{"presence": "PRESENT"}]},
            }
        ),
        config,
    )

    assert resumed["triageOutcome"] == "RED"
    assert resumed["stopConversation"] is True


def test_four_turn_baby_flow_keeps_intent_and_never_repeats_questions():
    """A vague follow-up must not erase the established paediatric conversation."""

    messages = ["Bé hai tháng bị sốt", "không biết", "không biết", "không biết"]
    prior_state = None
    questions_by_turn = []

    for turn_index, message in enumerate(messages):
        answered_question_ids = ["Q_GLOBAL_DANGER"] if turn_index == 1 else []
        submitted_option_codes = ["UNSURE"] if turn_index == 1 else []
        request = TriageV2TurnRequest(
            sessionId="00000000-0000-0000-0000-00000000002d",
            stateVersion=1,
            expectedStateVersion=1,
            requestId=f"phase2d-request-{turn_index:02d}",
            messageId=f"phase2d-message-{turn_index:02d}",
            latestUserMessage=message,
            activeProfileId="phase2d-profile",
            selectedTarget="BABY",
            journeyContext={"babyAgeMonths": 2},
            previousState=prior_state,
            answeredQuestionIds=answered_question_ids,
            submittedOptionCodes=submitted_option_codes,
            expectedRulesetHash=get_registry().ruleset_sha256,
        )
        state = _turn_state(request)
        result = dict(build_triage_v2_graph().invoke(state, graph_config(request.sessionId)))
        result.pop("__interrupt__", None)
        result.pop("submittedOptionCodes", None)

        assert result["targetEntity"] == "BABY"
        assert result["stage"] == "INFANT_0_12M"
        expected_intent = "FOLLOW_UP_ANSWER" if turn_index == 1 else "SYMPTOM_TRIAGE"
        assert result["intent"] == expected_intent
        assert result["confirmedConversationIntent"] == "SYMPTOM_TRIAGE"
        assert "Q_CLARIFY_INTENT" not in result["plannedQuestionIds"]

        questions_by_turn.append(list(result["plannedQuestionIds"]))
        prior_state = json.loads(json.dumps(result, ensure_ascii=False))

    flattened = [question for questions in questions_by_turn for question in questions]
    assert "Q_BABY_TEMPERATURE" in flattened
    assert len(flattened) == len(set(flattened))


def test_graph_is_isolated_from_legacy_gemini_rag_java_and_db_modules():
    """The V2 graph must not import the legacy engine, an LLM, RAG, Java bridges or the database.

    Checks the import statements rather than raw substrings: a bare "rag" search also matches
    "coverage" and "storage", which made the guard fire on unrelated prose. Import lines are what
    actually create the dependency this test exists to forbid.
    """

    source = inspect.getsource(inspect.getmodule(triage_v2_graph.__class__))
    graph_source = inspect.getsource(__import__("app.triage_v2.graph", fromlist=["*"]))
    imports = [
        line.strip().lower() for line in graph_source.splitlines()
        if line.strip().startswith(("import ", "from "))
    ]

    def module_words(line: str) -> set[str]:
        """Words making up the imported module path, so matching is exact rather than substring."""
        path = line.split()[1]
        return {word for segment in path.split(".") for word in segment.split("_")}

    forbidden = {"gemini", "rag", "java", "database", "supabase", "retriever", "evidence"}
    for line in imports:
        if line.startswith("from __future__"):
            continue
        overlap = module_words(line) & forbidden
        assert not overlap, f"{overlap} imported by the V2 graph: {line}"
        assert not line.split()[1].startswith("app.graph"), f"legacy V1 graph imported: {line}"

    # Every import must come from the isolated V2 package or LangGraph itself.
    assert imports and all(
        line.startswith(("from app.triage_v2", "from langgraph", "from __future__"))
        for line in imports
    ), imports
    assert source
