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
    if action == "ASK_CLARIFYING_QUESTIONS" and questions:
        rendered = {
            "finalResponse": {
                "kind": "CLARIFICATION",
                "headline": "Cần thêm thông tin",
                "message": "Vui lòng trả lời các câu hỏi đã chọn để hệ thống tiếp tục định hướng.",
                "questions": questions,
                "disclaimer": DISCLAIMER,
            },
            "readingLinks": [],
        }
        if type(outcome) is not str:
            rendered["triageOutcome"] = "NEEDS_MORE_INFO"
        return rendered
    synthesized_outcome = type(action) is str and type(outcome) is not str
    if type(action) is str and type(outcome) is not str:
        # Defensive transport rendering. Normal graph paths assign NEEDS_MORE_INFO before this
        # point, but a partial/degraded state with an action must never leave the client blank.
        outcome = "NEEDS_MORE_INFO"
    if type(outcome) is not str or type(action) is not str:
        return {"finalResponse": None, "readingLinks": []}

    try:
        rendered = render(outcome, action, tuple(state.get("reasonCodes", [])))
    except KeyError:
        # Technical concurrency/idempotency actions are not clinical dispositions. Keep their
        # response copy and template identity technical rather than pretending to route care.
        headline, message = _technical_copy(action)
        result = {
            "finalResponse": {
                "kind": "TRIAGE_RESULT",
                "outcome": outcome,
                "action": action,
                "stopConversation": state.get("stopConversation") is True,
                "headline": headline,
                "message": message,
                "limitations": [],
                "disclaimer": DISCLAIMER,
                "templateId": f"TECHNICAL.{action}",
                "templateVersion": "1.0.0",
                "clinicalSignOff": "NOT_APPLICABLE_TECHNICAL",
                "questions": questions,
            },
            "readingLinks": [],
        }
        if synthesized_outcome:
            result["triageOutcome"] = outcome
        return result
    result = {
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
    if synthesized_outcome:
        result["triageOutcome"] = outcome
    return result


def _technical_copy(action: str) -> tuple[str, str]:
    if action == "DUPLICATE_REQUEST":
        return (
            "Yêu cầu đã được ghi nhận",
            "Yêu cầu này đã được xử lý trước đó. Vui lòng tải lại kết quả hoặc thử lại.",
        )
    if action == "STATE_VERSION_CONFLICT":
        return (
            "Dữ liệu phiên vừa thay đổi",
            "Không thể áp dụng yêu cầu vào phiên hiện tại. Vui lòng tải lại thông tin mới nhất và thử lại.",
        )
    return (
        "Tạm thời chưa thể hoàn tất yêu cầu",
        "Hệ thống gặp trạng thái kỹ thuật chưa được hỗ trợ. Vui lòng thử lại.",
    )


def audit_node(state: Mapping[str, object]) -> dict[str, object]:
    if state.get("triageOutcome") == "GREEN":
        raise RuntimeError("GREEN_RELEASE_GATE_BYPASS")
    if state.get("triageOutcome") == "RED" and state.get("stopConversation") is not True:
        return {"stopConversation": True, "candidateQuestionIds": [], "plannedQuestionIds": []}
    return {"readingLinks": []}
