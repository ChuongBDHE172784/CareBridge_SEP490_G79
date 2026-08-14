"""Deterministic public explanation and honest post-outcome evidence state."""

from __future__ import annotations

from typing import Mapping

from app.rules.registry import get_registry

EVIDENCE_STATUSES = frozenset({"AVAILABLE", "PENDING", "UNAVAILABLE", "REJECTED"})


def rationale_for_state(state: Mapping[str, object]) -> str:
    """Explain a disposition in Vietnamese without exposing internal identifiers."""

    outcome = state.get("triageOutcome")
    titles = _decisive_titles(state.get("decisiveRuleIds"))
    if titles:
        joined = "; ".join(titles[:3])
        if outcome == "RED":
            return f"Kết quả Đỏ được xác định vì hệ thống ghi nhận: {joined}."
        if outcome == "YELLOW":
            return f"Kết quả Vàng được xác định vì hệ thống ghi nhận: {joined}."
    if outcome == "RED":
        return (
            "Kết quả Đỏ được xác định từ dấu hiệu nguy hiểm đã ghi nhận; "
            "bạn cần được nhân viên y tế đánh giá ngay."
        )
    if outcome == "YELLOW":
        return (
            "Kết quả Vàng được xác định từ các dữ kiện đã cung cấp; "
            "bạn nên được nhân viên y tế đánh giá sớm."
        )
    if outcome == "OUT_OF_SCOPE":
        return "Nội dung hiện tại nằm ngoài phạm vi định hướng của công cụ."
    return "Hiện chưa đủ dữ kiện để đưa ra định hướng an toàn."


def finalize_evidence(
    state: Mapping[str, object], *, retrieval_attempted: bool
) -> dict[str, object]:
    """Return final evidence fields without ever altering outcome or action."""

    result = dict(state)
    citations = list(state.get("citations", [])) if type(state.get("citations")) is list else []
    rejections = (
        list(dict.fromkeys(state.get("evidenceRejections", [])))
        if type(state.get("evidenceRejections")) is list
        else []
    )
    if citations:
        status = "AVAILABLE"
    elif rejections:
        status = "REJECTED"
    elif retrieval_attempted:
        status = "UNAVAILABLE"
    else:
        status = "PENDING"
    result.update(
        citations=citations,
        evidenceRejections=rejections,
        evidenceStatus=status,
        rationale=rationale_for_state(state),
    )
    return result


def _decisive_titles(value: object) -> list[str]:
    if type(value) is not list:
        return []
    wanted = {item for item in value if type(item) is str}
    if not wanted:
        return []
    try:
        registry = get_registry()
    except Exception:
        return []
    titled = {
        rule.rule_id: rule.title for rule in registry.rules if rule.rule_id in wanted
    }
    # SafetyPolicy uses policy_id rather than rule_id.
    titled.update({
        policy.policy_id: policy.title
        for policy in registry.safety_policies
        if policy.policy_id in wanted
    })
    return [titled[item] for item in value if item in titled]
