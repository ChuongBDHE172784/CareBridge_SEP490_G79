"""Final GREEN denial, fixed response rendering, and audit boundary."""

from __future__ import annotations

from typing import Mapping

from app.rendering.response_renderer import DISCLAIMER, render
from app.rules.evaluator import CompletionReason


def green_release_gate(state: Mapping[str, object]) -> dict[str, object]:
    if state.get("triageOutcome") != "GREEN":
        return {}
    return {
        "triageOutcome": "NEEDS_MORE_INFO",
        "requiredAction": "ROUTE_TO_HEALTHCARE_WORKER",
        "reasonCodes": ["GREEN_RELEASE_GATE_DISABLED"],
        "stopConversation": True,
        "completionReason": CompletionReason.RULESET_COVERAGE_LIMITATION,
        "candidateQuestionIds": [],
        "plannedQuestionIds": [],
        "finalResponse": None,
        "readingLinks": [],
    }


def deterministic_renderer(state: Mapping[str, object]) -> dict[str, object]:
    outcome = state.get("triageOutcome")
    action = state.get("requiredAction")
    questions = list(state.get("plannedQuestionIds", []))
    if outcome is None and questions:
        return {
            "finalResponse": {
                "kind": "CLARIFICATION",
                "headline": "Cần thêm thông tin",
                "message": "Vui lòng trả lời các câu hỏi đã chọn để hệ thống tiếp tục định hướng.",
                "questions": questions,
                "disclaimer": DISCLAIMER,
            },
            "readingLinks": [],
        }
    if type(outcome) is not str or type(action) is not str:
        return {"finalResponse": None, "readingLinks": []}

    rendered = render(outcome, action, tuple(state.get("reasonCodes", [])))
    return {
        "finalResponse": {
            "kind": "TRIAGE_RESULT",
            "outcome": outcome,
            "action": action,
            "stopConversation": state.get("stopConversation") is True,
            "headline": rendered.headline,
            "message": rendered.message,
            "limitations": list(rendered.limitations),
            "disclaimer": DISCLAIMER,
            "templateId": rendered.template_id,
            "templateVersion": rendered.template_version,
            "clinicalSignOff": rendered.clinical_sign_off,
            "questions": questions,
        },
        "readingLinks": [],
    }


def audit_node(state: Mapping[str, object]) -> dict[str, object]:
    if state.get("triageOutcome") == "GREEN":
        raise RuntimeError("GREEN_RELEASE_GATE_BYPASS")
    if state.get("triageOutcome") == "RED" and state.get("stopConversation") is not True:
        return {"stopConversation": True, "candidateQuestionIds": [], "plannedQuestionIds": []}
    return {"readingLinks": []}
