from __future__ import annotations

import logging
from typing import TypedDict

from langgraph.graph import END, StateGraph

from app.config import DISCLAIMER, NO_OFFICIAL_REALTIME_SOURCE_WARNING, NO_SOURCE_WARNING, RISK_COLORS
from app.evidence_cache import cache_pending_sources
from app.intake_question_engine import ask_followup_questions, determine_missing_information
from app.risk_rules import apply_red_flag_rules, missing_info_questions, score_risk
from app.schemas import ChildTriageRequest, ChildTriageResponse, Citation, Evidence, IntakeQuestion, SourceDocument
from app.source_retriever import attach_citations, build_evidence, retrieve_realtime_sources, retrieve_sources
from app.source_validator import validate_source
from app.symptom_normalizer import normalize_symptoms

log = logging.getLogger(__name__)

class TriageState(TypedDict, total=False):
    intake: ChildTriageRequest
    normalizedSymptoms: list[str]
    missingInformation: list[str]
    retrievedSources: list[SourceDocument]
    riskLevel: str
    riskColor: str
    summary: str
    possibleConcern: str
    recommendedAction: str
    emergencyActionRequired: bool
    redFlags: list[str]
    matchedRules: list[str]
    citations: list[Citation]
    evidence: Evidence
    questions: list[str]
    followupQuestions: list[IntakeQuestion]
    warning: str | None
    disclaimer: str
    forceCautiousYellow: bool
    forcedWarning: str | None


def collect_intake(state: TriageState) -> TriageState:
    state["questions"] = missing_info_questions(state["intake"])
    return state


def normalize_symptoms_node(state: TriageState) -> TriageState:
    state["normalizedSymptoms"] = normalize_symptoms(state["intake"])
    return state


def determine_missing_information_node(state: TriageState) -> TriageState:
    state["missingInformation"] = determine_missing_information(state["intake"])
    state["questions"] = missing_info_questions(state["intake"])
    return state


def ask_followup_questions_node(state: TriageState) -> TriageState:
    state["followupQuestions"] = ask_followup_questions(state["intake"])
    return state


def apply_red_flag_rules_node(state: TriageState) -> TriageState:
    red_flags, matched_rules = apply_red_flag_rules(
        state["intake"],
        state.get("normalizedSymptoms", []),
    )
    state["redFlags"] = red_flags
    state["matchedRules"] = matched_rules
    return state


def score_risk_node(state: TriageState) -> TriageState:
    if state.get("forceCautiousYellow") and not state.get("redFlags"):
        risk = "YELLOW"
        matched_rules = state.get("matchedRules", [])
        if "YELLOW_INCOMPLETE_INFORMATION" not in matched_rules:
            matched_rules.append("YELLOW_INCOMPLETE_INFORMATION")
        state["matchedRules"] = matched_rules
    elif state.get("questions"):
        risk = "NEED_MORE_INFO"
    else:
        risk, matched_rules = score_risk(
            state["intake"],
            state.get("normalizedSymptoms", []),
            state.get("redFlags", []),
            state.get("matchedRules", []),
        )
        state["matchedRules"] = matched_rules
    state["riskLevel"] = risk
    state["riskColor"] = RISK_COLORS[risk]
    state["emergencyActionRequired"] = risk == "RED"
    return state


def retrieve_sources_node(state: TriageState) -> TriageState:
    state["retrievedSources"] = retrieve_sources(
        state.get("normalizedSymptoms", []),
        state.get("matchedRules", []),
    )
    return state


def realtime_official_search_if_needed_node(state: TriageState) -> TriageState:
    local_citations = attach_citations(
        state.get("retrievedSources", []),
        state.get("normalizedSymptoms", []),
        state.get("matchedRules", []),
    )
    if local_citations:
        return state
    if state.get("riskLevel") not in {"RED", "YELLOW", "GREEN"}:
        return state
    realtime_sources = retrieve_realtime_sources(
        state.get("normalizedSymptoms", []),
        state.get("matchedRules", []),
    )
    cache_pending_sources(realtime_sources)
    state["retrievedSources"] = realtime_sources
    return state


def validate_sources_node(state: TriageState) -> TriageState:
    symptoms = state.get("normalizedSymptoms", [])
    state["retrievedSources"] = [
        source for source in state.get("retrievedSources", [])
        if validate_source(source, symptoms)
    ]
    return state


def generate_safe_response_node(state: TriageState) -> TriageState:
    risk = state["riskLevel"]
    symptoms = ", ".join(state.get("normalizedSymptoms", [])) or "trieu chung da nhap"
    if risk == "NEED_MORE_INFO":
        state["summary"] = "CareBridge can them thong tin truoc khi phan loai rui ro."
        state["possibleConcern"] = "Thieu tuoi cua tre hoac mo ta trieu chung."
        state["recommendedAction"] = (
            "Vui long tra loi cac cau hoi bo sung. Neu tre co dau hieu nang, "
            "hay lien he co so y te hoac cap cuu."
        )
    elif risk == "RED":
        red_flags = ", ".join(state.get("redFlags", [])) or "dau hieu nguy hiem"
        state["summary"] = (
            f"Trieu chung {red_flags} la dau hieu nguy hiem theo rule engine va "
            "nguon y te chinh thong, can dua tre di kham/cap cuu ngay."
        )
        state["possibleConcern"] = "; ".join(state.get("redFlags", []))
        state["recommendedAction"] = (
            "Lien he cap cuu hoac dua tre den co so y te gan nhat ngay. "
            "Khong cho tu van AI neu tinh trang dang xau di."
        )
    elif risk == "YELLOW":
        state["summary"] = (
            f"Cac trieu chung {symptoms} can duoc theo doi va co the can nhan vien y te "
            "danh gia neu keo dai, nang hon hoac kem dau hieu canh bao."
        )
        state["possibleConcern"] = "Trieu chung chua co red flag ro, nhung can theo doi dien tien."
        state["recommendedAction"] = (
            "Theo doi nhiet do, nhip tho, bu/uong va tinh trang tinh tao. "
            "Lien he bac si neu keo dai, nang hon hoac xuat hien red flag."
        )
    else:
        state["summary"] = "Chua ghi nhan red flag tu thong tin hien co."
        state["possibleConcern"] = (
            "Trieu chung nhe, tre van tinh tao, bu/uong tot va tho binh thuong."
        )
        state["recommendedAction"] = (
            "Theo doi tai nha, cho tre bu/uong du, ghi nhan trieu chung va "
            "quay lai kiem tra neu co thay doi."
        )
    return state


def attach_citations_node(state: TriageState) -> TriageState:
    citations = attach_citations(
        state.get("retrievedSources", []),
        state.get("normalizedSymptoms", []),
        state.get("matchedRules", []),
    )
    state["citations"] = citations
    state["evidence"] = build_evidence(citations, state.get("normalizedSymptoms", []))
    if citations:
        state["warning"] = None
    elif state.get("riskLevel") in {"RED", "YELLOW", "GREEN"}:
        state["warning"] = NO_OFFICIAL_REALTIME_SOURCE_WARNING
    else:
        state["warning"] = NO_SOURCE_WARNING
    if state.get("forcedWarning"):
        state["warning"] = state["forcedWarning"]
    state["disclaimer"] = DISCLAIMER
    return state


def audit_log(state: TriageState) -> TriageState:
    log.info(
        "ai_triage_audit normalizedSymptoms=%s riskLevel=%s matchedRules=%s citationIds=%s sourceStatus=%s emergencyActionRequired=%s warning=%s",
        state.get("normalizedSymptoms", []),
        state.get("riskLevel"),
        state.get("matchedRules", []),
        [citation.id for citation in state.get("citations", [])],
        [citation.sourceStatus for citation in state.get("citations", [])],
        state.get("emergencyActionRequired"),
        state.get("warning"),
    )
    return state


def build_graph():
    graph = StateGraph(TriageState)
    graph.add_node("collect_intake", collect_intake)
    graph.add_node("normalize_symptoms", normalize_symptoms_node)
    graph.add_node("determine_missing_information", determine_missing_information_node)
    graph.add_node("ask_followup_questions", ask_followup_questions_node)
    graph.add_node("retrieve_local_sources", retrieve_sources_node)
    graph.add_node("realtime_official_search_if_needed", realtime_official_search_if_needed_node)
    graph.add_node("validate_sources", validate_sources_node)
    graph.add_node("apply_red_flag_rules", apply_red_flag_rules_node)
    graph.add_node("score_risk", score_risk_node)
    graph.add_node("generate_safe_response", generate_safe_response_node)
    graph.add_node("attach_citations", attach_citations_node)
    graph.add_node("audit_log", audit_log)

    graph.set_entry_point("collect_intake")
    graph.add_edge("collect_intake", "normalize_symptoms")
    graph.add_edge("normalize_symptoms", "determine_missing_information")
    graph.add_edge("determine_missing_information", "ask_followup_questions")
    graph.add_edge("ask_followup_questions", "apply_red_flag_rules")
    graph.add_edge("apply_red_flag_rules", "score_risk")
    graph.add_edge("score_risk", "retrieve_local_sources")
    graph.add_edge("retrieve_local_sources", "realtime_official_search_if_needed")
    graph.add_edge("realtime_official_search_if_needed", "validate_sources")
    graph.add_edge("validate_sources", "generate_safe_response")
    graph.add_edge("generate_safe_response", "attach_citations")
    graph.add_edge("attach_citations", "audit_log")
    graph.add_edge("audit_log", END)
    return graph.compile()


TRIAGE_GRAPH = build_graph()


def run_triage(
    intake: ChildTriageRequest,
    *,
    force_cautious_yellow: bool = False,
    forced_warning: str | None = None,
) -> ChildTriageResponse:
    state = TRIAGE_GRAPH.invoke({
        "intake": intake,
        "forceCautiousYellow": force_cautious_yellow,
        "forcedWarning": forced_warning,
    })
    return ChildTriageResponse(
        riskLevel=state["riskLevel"],
        riskColor=state["riskColor"],
        summary=state["summary"],
        possibleConcern=state["possibleConcern"],
        recommendedAction=state["recommendedAction"],
        emergencyActionRequired=state["emergencyActionRequired"],
        redFlags=state.get("redFlags", []),
        matchedRules=state.get("matchedRules", []),
        citations=state.get("citations", []),
        evidence=state["evidence"],
        questions=state.get("questions", []),
        warning=state.get("warning"),
        disclaimer=state.get("disclaimer", DISCLAIMER),
    )
