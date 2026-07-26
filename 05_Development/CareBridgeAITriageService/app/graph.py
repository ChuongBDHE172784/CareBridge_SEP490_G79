from __future__ import annotations

import logging
import time
from typing import TypedDict

from langgraph.graph import END, StateGraph

from app.config import (
    DISCLAIMER,
    GRAPH_VERSION,
    NO_OFFICIAL_REALTIME_SOURCE_WARNING,
    MIN_REALTIME_SEARCH_REMAINING_SECONDS,
    NO_SOURCE_WARNING,
    ONTOLOGY_VERSION,
    PYTHON_SERVICE_TIMEOUT_SECONDS,
    RED_LOCAL_EVIDENCE_GAP_WARNING,
    REALTIME_SEARCH_SKIPPED_DEADLINE_WARNING,
    RESPONSE_SCHEMA_VERSION,
    RISK_COLORS,
    RULE_SET_VERSION,
)
from app.evidence_cache import cache_pending_sources
from app.gemini_client import GeminiClient, get_gemini_client
from app.intake_question_engine import (
    ask_followup_questions,
    determine_missing_information,
    naturalize_followup_questions,
)
from app.risk_rules import (
    MATERNAL_STAGES,
    apply_red_flag_rules,
    missing_info_questions,
    score_risk,
)
from app.schemas import (
    ChildTriageRequest,
    ChildTriageResponse,
    Claim,
    Citation,
    Evidence,
    ExplainabilityMetrics,
    IntakeQuestion,
    NormalizedSymptom,
    SourceDocument,
)
from app.source_retriever import (
    attach_citations,
    build_evidence,
    retrieve_realtime_sources,
    retrieve_sources,
    approved_domains,
)
from app.source_validator import validate_source
from app.symptom_normalizer import (
    CANONICAL_SYMPTOM_CODES,
    normalize_symptom_details_deterministic,
    normalize_symptom_details_with_metadata,
)


log = logging.getLogger(__name__)


class TriageState(TypedDict, total=False):
    intake: ChildTriageRequest
    geminiClient: GeminiClient | None
    normalizedSymptoms: list[str]
    normalizedSymptomDetails: list[NormalizedSymptom]
    normalizationPrepared: bool
    normalizationGeminiUsed: bool
    requestDeadline: float
    missingInformation: list[str]
    retrievedSources: list[SourceDocument]
    riskLevel: str
    riskColor: str
    summary: str
    possibleConcern: str
    recommendedAction: str
    emergencyActionRequired: bool
    immediateRed: bool
    redFlags: list[str]
    matchedRules: list[str]
    citations: list[Citation]
    claims: list[Claim]
    evidence: Evidence
    questions: list[str]
    followupQuestions: list[IntakeQuestion]
    assistantMessage: str
    assistantProvider: str
    assistantFallbackUsed: bool
    warning: str | None
    disclaimer: str
    forceCautiousYellow: bool
    forcedWarning: str | None
    realtimeSearchSkippedForDeadline: bool


def collect_intake(state: TriageState) -> TriageState:
    state["questions"] = missing_info_questions(state["intake"])
    state["assistantProvider"] = "DETERMINISTIC_FALLBACK"
    state["assistantFallbackUsed"] = True
    return state


def deterministic_emergency_scan(state: TriageState) -> TriageState:
    details = normalize_symptom_details_deterministic(state["intake"])
    symptoms = [item.normalizedCode for item in details]
    red_flags, matched_rules = apply_red_flag_rules(state["intake"], symptoms)
    if red_flags or not state.get("normalizationPrepared"):
        state["normalizedSymptomDetails"] = details
        state["normalizedSymptoms"] = symptoms
    state["redFlags"] = red_flags
    state["matchedRules"] = matched_rules
    state["immediateRed"] = bool(red_flags)
    if red_flags:
        state["questions"] = []
        state["followupQuestions"] = []
        _set_risk(state, "RED")
    return state


def route_after_emergency_scan(state: TriageState) -> str:
    return "immediate_red" if state.get("immediateRed") else "normal"


def normalize_symptoms_with_gemini(state: TriageState) -> TriageState:
    if state.get("normalizationPrepared"):
        return state
    details, used = normalize_symptom_details_with_metadata(
        state["intake"], state.get("geminiClient"), state.get("requestDeadline")
    )
    state["normalizedSymptomDetails"] = details
    state["normalizedSymptoms"] = [item.normalizedCode for item in details]
    state["normalizationGeminiUsed"] = used
    return state


def validate_normalized_symptoms(state: TriageState) -> TriageState:
    valid = [
        item for item in state.get("normalizedSymptomDetails", [])
        if item.normalizedCode in CANONICAL_SYMPTOM_CODES
    ]
    state["normalizedSymptomDetails"] = valid
    state["normalizedSymptoms"] = [item.normalizedCode for item in valid]
    return state


def detect_normalized_red_flags(state: TriageState) -> TriageState:
    red_flags, matched_rules = apply_red_flag_rules(
        state["intake"], state.get("normalizedSymptoms", [])
    )
    state["redFlags"] = red_flags
    state["matchedRules"] = matched_rules
    if red_flags:
        state["questions"] = []
        state["followupQuestions"] = []
        _set_risk(state, "RED")
    return state


def route_after_normalized_red_scan(state: TriageState) -> str:
    return "red" if state.get("redFlags") else "continue"


def determine_missing_information_node(state: TriageState) -> TriageState:
    state["missingInformation"] = determine_missing_information(state["intake"])
    return state


def select_followup_question_keys(state: TriageState) -> TriageState:
    questions = ask_followup_questions(state["intake"])
    if not state.get("normalizedSymptoms") and not state.get("redFlags"):
        generic = IntakeQuestion(
            questionKey="parentFreeText",
            text=(
                f"Vui lòng mô tả cụ thể dấu hiệu bạn đang gặp {_maternal_context(state['intake'].stage)}."
                if state["intake"].stage in MATERNAL_STAGES
                else "Vui lòng mô tả dấu hiệu cụ thể mà bạn quan sát được ở trẻ."
            ),
            answerType="TEXT",
        )
        questions = [generic] + [item for item in questions if item.questionKey != "parentFreeText"]
    state["followupQuestions"] = questions[:3]
    state["questions"] = [item.text for item in state["followupQuestions"]]
    return state


def naturalize_followup_questions_with_gemini(state: TriageState) -> TriageState:
    questions, message, used = naturalize_followup_questions(
        state.get("followupQuestions", []),
        intake=state["intake"],
        normalized_symptoms=state.get("normalizedSymptoms", []),
        gemini_client=(
            None if state["intake"].stage == "POSTPARTUM" else state.get("geminiClient")
        ),
        deadline=state.get("requestDeadline"),
    )
    state["followupQuestions"] = questions
    state["questions"] = [item.text for item in questions]
    state["assistantMessage"] = message
    if used:
        state["assistantProvider"] = "GEMINI"
        state["assistantFallbackUsed"] = False
    return state


def apply_deterministic_rules(state: TriageState) -> TriageState:
    red_flags, matched_rules = apply_red_flag_rules(
        state["intake"], state.get("normalizedSymptoms", [])
    )
    state["redFlags"] = red_flags
    state["matchedRules"] = matched_rules
    if red_flags:
        risk = "RED"
    elif state.get("forceCautiousYellow"):
        risk = "YELLOW"
        if "YELLOW_INCOMPLETE_INFORMATION" not in state["matchedRules"]:
            state["matchedRules"].append("YELLOW_INCOMPLETE_INFORMATION")
    elif state.get("questions"):
        risk = "NEED_MORE_INFO"
    else:
        risk, rules = score_risk(
            state["intake"],
            state.get("normalizedSymptoms", []),
            red_flags,
            matched_rules,
        )
        state["matchedRules"] = rules
    _set_risk(state, risk)
    return state


def retrieve_sources_node(state: TriageState) -> TriageState:
    state["retrievedSources"] = retrieve_sources(
        state.get("normalizedSymptoms", []),
        state.get("matchedRules", []),
        state["intake"].stage,
        state["intake"].childAgeMonths,
    )
    return state


def realtime_official_search_if_needed(state: TriageState) -> TriageState:
    local = attach_citations(
        state.get("retrievedSources", []),
        state.get("normalizedSymptoms", []),
        state.get("matchedRules", []),
        state["intake"].stage,
    )
    if local or state.get("riskLevel") in {"RED", "NEED_MORE_INFO"}:
        return state
    request_deadline = state.get("requestDeadline")
    if request_deadline is not None and request_deadline - time.monotonic() < MIN_REALTIME_SEARCH_REMAINING_SECONDS:
        state["realtimeSearchSkippedForDeadline"] = True
        return state
    realtime = retrieve_realtime_sources(
        state.get("normalizedSymptoms", []), state.get("matchedRules", []), state["intake"].stage,
        request_deadline=request_deadline,
    )
    cache_pending_sources(realtime)
    state["retrievedSources"] = realtime
    return state


def validate_sources(state: TriageState) -> TriageState:
    symptoms = state.get("normalizedSymptoms", [])
    state["retrievedSources"] = [
        source for source in state.get("retrievedSources", [])
        if validate_source(source, symptoms, approved_domains(state["intake"].stage))
    ]
    return state


def attach_evidence(state: TriageState) -> TriageState:
    citations = attach_citations(
        state.get("retrievedSources", []),
        state.get("normalizedSymptoms", []),
        state.get("matchedRules", []),
        state["intake"].stage,
    )
    state["citations"] = citations
    state["claims"] = build_claims(state.get("riskLevel", "NEED_MORE_INFO"), state.get("redFlags", []), citations)
    state["evidence"] = build_evidence(citations, state.get("normalizedSymptoms", []))
    if citations:
        state["warning"] = None
    elif state.get("riskLevel") == "RED":
        state["warning"] = RED_LOCAL_EVIDENCE_GAP_WARNING
    elif state.get("riskLevel") in {"YELLOW", "GREEN"}:
        state["warning"] = (
            REALTIME_SEARCH_SKIPPED_DEADLINE_WARNING
            if state.get("realtimeSearchSkippedForDeadline")
            else NO_OFFICIAL_REALTIME_SOURCE_WARNING
        )
    else:
        state["warning"] = NO_SOURCE_WARNING
    if state.get("forcedWarning"):
        state["warning"] = state["forcedWarning"]
    state["disclaimer"] = _disclaimer(state["intake"].stage)
    return state


def compose_deterministic_response(state: TriageState) -> TriageState:
    risk = state["riskLevel"]
    if state["intake"].stage in MATERNAL_STAGES:
        return _compose_maternal_response(state, risk)
    symptoms = ", ".join(state.get("normalizedSymptoms", [])) or "các dấu hiệu đã nhập"
    if risk == "NEED_MORE_INFO":
        state["summary"] = "CareBridge cần thêm thông tin trước khi phân loại rủi ro."
        state["possibleConcern"] = "Thông tin hiện tại chưa đủ để phân loại an toàn."
        state["recommendedAction"] = (
            "Vui lòng trả lời các câu hỏi bổ sung. Nếu trẻ có dấu hiệu nặng, "
            "hãy liên hệ cơ sở y tế hoặc cấp cứu."
        )
    elif risk == "RED":
        flags = ", ".join(state.get("redFlags", [])) or "dấu hiệu nguy hiểm"
        state["summary"] = f"Hệ thống ghi nhận {flags}; đây là dấu hiệu cần được đánh giá khẩn cấp."
        state["possibleConcern"] = "Có dấu hiệu nguy hiểm theo quy tắc phân loại rủi ro của CareBridge."
        state["recommendedAction"] = (
            "Hãy liên hệ cấp cứu hoặc đưa trẻ đến cơ sở y tế gần nhất ngay. "
            "Không chờ tư vấn AI nếu tình trạng đang xấu đi."
        )
    elif risk == "YELLOW":
        state["summary"] = f"Các dấu hiệu {symptoms} cần được theo dõi sát."
        state["possibleConcern"] = "Chưa ghi nhận red flag rõ ràng nhưng diễn tiến cần được theo dõi."
        state["recommendedAction"] = (
            "Theo dõi nhiệt độ, nhịp thở, bú/uống và độ tỉnh táo; liên hệ nhân viên y tế "
            "nếu triệu chứng kéo dài, nặng hơn hoặc xuất hiện dấu hiệu cảnh báo."
        )
    else:
        state["summary"] = "Chưa ghi nhận dấu hiệu nguy hiểm từ thông tin hiện có."
        state["possibleConcern"] = "Các dấu hiệu hiện tại phù hợp với mức theo dõi tại nhà."
        state["recommendedAction"] = (
            "Tiếp tục theo dõi, cho trẻ bú/uống đủ và đánh giá lại ngay nếu tình trạng thay đổi."
        )
    state["disclaimer"] = DISCLAIMER
    return state


def build_claims(risk: str, red_flags: list[str], citations: list[Citation]) -> list[Claim]:
    """Claims are deterministic, and may only cite already validated evidence."""
    evidence_ids = [citation.sourceId or citation.id for citation in citations if citation.sourceId or citation.id]
    if not evidence_ids:
        return []
    if risk == "RED":
        text = "Các dấu hiệu nguy hiểm đã ghi nhận cần được đánh giá khẩn cấp."
    elif risk == "NEED_MORE_INFO":
        text = "Thông tin hiện tại chưa đủ để phân loại rủi ro an toàn."
    elif risk == "YELLOW":
        text = "Các dấu hiệu hiện tại cần được theo dõi sát và đánh giá thêm khi diễn tiến thay đổi."
    else:
        text = "Chưa ghi nhận dấu hiệu nguy hiểm từ thông tin hiện có; vẫn cần theo dõi diễn tiến."
    return [Claim(claimId="CLAIM_TRIAGE_001", text=text, evidenceIds=evidence_ids)]


def compose_safe_explanation_with_gemini(state: TriageState) -> TriageState:
    compose_deterministic_response(state)
    if state.get("riskLevel") == "RED" or state["intake"].stage == "POSTPARTUM":
        return state
    client = state.get("geminiClient")
    if client is None:
        return state
    explanation = client.explain_triage_result(
        risk_level=state["riskLevel"],
        matched_rules=state.get("matchedRules", []),
        red_flags=state.get("redFlags", []),
        normalized_symptoms=state.get("normalizedSymptoms", []),
        recommendation_code=_recommendation_code(state["riskLevel"]),
        citations=state.get("citations", []),
        deadline=state.get("requestDeadline"),
        # CB-TRIAGE-THMC-IMP-001: advisory prior-context summaries — narrative only.
        # Deterministic risk scoring above never reads healthContext (BR-THMC-004).
        health_context_notes=[item.summaryText for item in state["intake"].healthContext],
    )
    if explanation is None:
        state["assistantProvider"] = "DETERMINISTIC_FALLBACK"
        state["assistantFallbackUsed"] = True
        return state
    state["summary"] = explanation.summary
    state["possibleConcern"] = explanation.possibleConcern
    state["recommendedAction"] = explanation.recommendedAction
    state["assistantProvider"] = "GEMINI"
    state["assistantFallbackUsed"] = False
    state["disclaimer"] = _disclaimer(state["intake"].stage)
    return state


def validate_final_response(state: TriageState) -> TriageState:
    if state.get("redFlags") and state.get("riskLevel") != "RED":
        _set_risk(state, "RED")
        compose_deterministic_response(state)
        state["assistantProvider"] = "DETERMINISTIC_FALLBACK"
        state["assistantFallbackUsed"] = True
    state["disclaimer"] = _disclaimer(state["intake"].stage)
    return state


def audit_handoff(state: TriageState) -> TriageState:
    log.info(
        "ai_triage_audit status=COMPLETED assistantProvider=%s fallback=%s",
        state.get("assistantProvider"),
        state.get("assistantFallbackUsed"),
    )
    return state


def _set_risk(state: TriageState, risk: str) -> None:
    state["riskLevel"] = risk
    state["riskColor"] = RISK_COLORS[risk]
    state["emergencyActionRequired"] = risk == "RED"


def _recommendation_code(risk: str) -> str:
    return {
        "RED": "SEEK_EMERGENCY_CARE",
        "YELLOW": "CONTACT_HEALTHCARE_PROVIDER",
        "GREEN": "MONITOR_AT_HOME",
        "NEED_MORE_INFO": "PROVIDE_MORE_INFORMATION",
    }[risk]


def _compose_maternal_response(state: TriageState, risk: str) -> TriageState:
    stage = state["intake"].stage
    context = _maternal_context(stage)
    if risk == "RED":
        flags = ", ".join(state.get("redFlags", [])) or "dấu hiệu cảnh báo"
        state["summary"] = (
            f"CareBridge ghi nhận {flags} {context}; bạn cần được hỗ trợ khẩn cấp."
        )
        state["possibleConcern"] = (
            f"Có dấu hiệu cảnh báo {context} theo quy tắc an toàn của CareBridge."
        )
        state["recommendedAction"] = (
            "Gọi cấp cứu 115 hoặc đến cơ sở y tế gần nhất ngay. "
            "Không chờ thêm kết quả trực tuyến nếu tình trạng đang xấu đi."
        )
    elif risk == "YELLOW":
        state["summary"] = (
            f"Thông tin {context} hiện chưa đầy đủ; kết quả được phân loại thận trọng."
        )
        state["possibleConcern"] = "Dấu hiệu cần được nhân viên y tế đánh giá thêm."
        state["recommendedAction"] = (
            "Liên hệ nhân viên y tế để được đánh giá và theo dõi diễn tiến. "
            "Gọi cấp cứu ngay nếu xuất hiện dấu hiệu nặng hoặc bạn cảm thấy không an toàn."
        )
    elif risk == "NEED_MORE_INFO":
        state["summary"] = (
            f"CareBridge cần thêm thông tin trước khi phân loại rủi ro {context}."
        )
        state["possibleConcern"] = (
            f"Thông tin {context} hiện chưa đủ để phân loại an toàn."
        )
        state["recommendedAction"] = (
            "Vui lòng trả lời các câu hỏi bổ sung. Nếu có dấu hiệu nặng hoặc cảm thấy không an toàn, "
            "hãy liên hệ cơ sở y tế hoặc cấp cứu ngay."
        )
    else:
        state["summary"] = "Chưa ghi nhận dấu hiệu nguy hiểm từ thông tin hiện có."
        state["possibleConcern"] = "Thông tin hiện có chưa cho thấy dấu hiệu cảnh báo rõ ràng."
        state["recommendedAction"] = (
            "Tiếp tục theo dõi và liên hệ nhân viên y tế nếu dấu hiệu kéo dài hoặc nặng hơn."
        )
    state["disclaimer"] = _disclaimer(stage)
    return state


def _disclaimer(stage: str) -> str:
    if stage in MATERNAL_STAGES:
        return (
            "CareBridge không chẩn đoán bệnh, không kê thuốc và không thay thế nhân viên y tế hoặc cấp cứu. "
            "Nếu bạn có dấu hiệu nặng hoặc cảm thấy không an toàn, hãy liên hệ cơ sở y tế hoặc cấp cứu ngay."
        )
    return DISCLAIMER


def _maternal_context(stage: str) -> str:
    return {
        "PRECONCEPTION": "trước khi mang thai",
        "PREGNANCY": "trong thai kỳ",
        "POSTPARTUM": "sau sinh",
    }[stage]


def build_graph():
    graph = StateGraph(TriageState)
    graph.add_node("collect_intake", collect_intake)
    graph.add_node("deterministic_emergency_scan", deterministic_emergency_scan)
    graph.add_node("normalize_symptoms_with_gemini", normalize_symptoms_with_gemini)
    graph.add_node("validate_normalized_symptoms", validate_normalized_symptoms)
    graph.add_node("detect_normalized_red_flags", detect_normalized_red_flags)
    graph.add_node("determine_missing_information", determine_missing_information_node)
    graph.add_node("select_followup_question_keys", select_followup_question_keys)
    graph.add_node("naturalize_followup_questions_with_gemini", naturalize_followup_questions_with_gemini)
    graph.add_node("apply_deterministic_rules", apply_deterministic_rules)
    graph.add_node("retrieve_sources", retrieve_sources_node)
    graph.add_node("realtime_official_search_if_needed", realtime_official_search_if_needed)
    graph.add_node("validate_sources", validate_sources)
    graph.add_node("attach_evidence", attach_evidence)
    graph.add_node("compose_safe_explanation_with_gemini", compose_safe_explanation_with_gemini)
    graph.add_node("validate_final_response", validate_final_response)
    graph.add_node("audit_handoff", audit_handoff)

    graph.set_entry_point("collect_intake")
    graph.add_edge("collect_intake", "deterministic_emergency_scan")
    graph.add_conditional_edges(
        "deterministic_emergency_scan",
        route_after_emergency_scan,
        {"immediate_red": "retrieve_sources", "normal": "normalize_symptoms_with_gemini"},
    )
    graph.add_edge("normalize_symptoms_with_gemini", "validate_normalized_symptoms")
    graph.add_edge("validate_normalized_symptoms", "detect_normalized_red_flags")
    graph.add_conditional_edges(
        "detect_normalized_red_flags",
        route_after_normalized_red_scan,
        {"red": "retrieve_sources", "continue": "determine_missing_information"},
    )
    graph.add_edge("determine_missing_information", "select_followup_question_keys")
    graph.add_edge("select_followup_question_keys", "naturalize_followup_questions_with_gemini")
    graph.add_edge("naturalize_followup_questions_with_gemini", "apply_deterministic_rules")
    graph.add_edge("apply_deterministic_rules", "retrieve_sources")
    graph.add_edge("retrieve_sources", "realtime_official_search_if_needed")
    graph.add_edge("realtime_official_search_if_needed", "validate_sources")
    graph.add_edge("validate_sources", "attach_evidence")
    graph.add_edge("attach_evidence", "compose_safe_explanation_with_gemini")
    graph.add_edge("compose_safe_explanation_with_gemini", "validate_final_response")
    graph.add_edge("validate_final_response", "audit_handoff")
    graph.add_edge("audit_handoff", END)
    return graph.compile()


TRIAGE_GRAPH = build_graph()


def run_triage(
    intake: ChildTriageRequest,
    *,
    force_cautious_yellow: bool = False,
    forced_warning: str | None = None,
    gemini_client: GeminiClient | None = None,
    normalized_details: list[NormalizedSymptom] | None = None,
    normalization_gemini_used: bool = False,
    request_deadline: float | None = None,
    deterministic_only: bool = False,
) -> ChildTriageResponse:
    client = None if deterministic_only else (gemini_client if gemini_client is not None else get_gemini_client())
    initial: TriageState = {
        "intake": intake,
        "geminiClient": client,
        "forceCautiousYellow": force_cautious_yellow,
        "forcedWarning": forced_warning,
        "requestDeadline": request_deadline or (time.monotonic() + PYTHON_SERVICE_TIMEOUT_SECONDS),
    }
    if normalized_details is not None:
        initial.update({
            "normalizedSymptomDetails": normalized_details,
            "normalizedSymptoms": [item.normalizedCode for item in normalized_details],
            "normalizationPrepared": True,
            "normalizationGeminiUsed": normalization_gemini_used,
        })
    state = TRIAGE_GRAPH.invoke(initial)
    citations = state.get("citations", [])
    details = state.get("normalizedSymptomDetails", [])
    retrieval_modes = {citation.retrievalMode for citation in citations}
    retrieval_mode = "REALTIME" if "REALTIME" in retrieval_modes else ("LOCAL" if citations else "NONE")
    matched_symptoms = set(state.get("normalizedSymptoms", []))
    matched_rules = set(state.get("matchedRules", []))
    covered_symptoms = {value for item in citations for value in item.matchedSymptoms}
    covered_rules = {value for item in citations for value in item.matchedRules}
    coverage = (
        "NONE" if not citations
        else "FULL" if matched_symptoms <= covered_symptoms and matched_rules <= covered_rules
        else "PARTIAL"
    )
    methods = {item.normalizationMethod for item in details}
    quality = (
        "NONE" if not matched_rules
        else "EXACT" if methods and methods <= {"EXACT", "STRUCTURED"}
        else "PARTIAL"
    )
    risk = state["riskLevel"]
    return ChildTriageResponse(
        stage=intake.stage,
        riskLevel=risk,
        riskColor=state["riskColor"],
        summary=state["summary"],
        possibleConcern=state["possibleConcern"],
        recommendedAction=state["recommendedAction"],
        emergencyActionRequired=state["emergencyActionRequired"],
        redFlags=state.get("redFlags", []),
        matchedRules=state.get("matchedRules", []),
        citations=citations,
        claims=state.get("claims", []),
        evidence=state["evidence"],
        questions=state.get("questions", []),
        warning=state.get("warning"),
        disclaimer=_disclaimer(intake.stage),
        normalizedSymptoms=state.get("normalizedSymptoms", []),
        normalizedSymptomDetails=details,
        evidenceIds=[item.sourceId or item.id for item in citations if item.sourceId or item.id],
        recommendationCode=_recommendation_code(risk),
        explainabilityMetrics=ExplainabilityMetrics(
            ruleMatchQuality=quality,
            evidenceCoverage=coverage,
            normalizationConfidence=min(
                (item.normalizationConfidence for item in details), default=0.0
            ),
            matchedSourceCount=len(citations),
            retrievalMode=retrieval_mode,
        ),
        graphVersion=GRAPH_VERSION,
        ruleSetVersion=RULE_SET_VERSION,
        ontologyVersion=ONTOLOGY_VERSION,
        responseSchemaVersion=RESPONSE_SCHEMA_VERSION,
        fallbackUsed=False,
        assistantProvider=state.get("assistantProvider", "DETERMINISTIC_FALLBACK"),
        assistantFallbackUsed=state.get("assistantFallbackUsed", True),
    )
