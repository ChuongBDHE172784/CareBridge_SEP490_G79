"""Isolated deterministic LangGraph for AI Triage."""

from __future__ import annotations

from langgraph.graph import END, StateGraph
from langgraph.checkpoint.memory import InMemorySaver

from app.triage.clinical_rule_engine import clinical_rule_engine
from app.triage.coverage_resolver import coverage_resolver
from app.triage.dataset_scope_nodes import dataset_calculator, scope_calculator
from app.triage.entity_stage_validator import entity_stage_validator
from app.triage.global_safety_gate import global_safety_gate
from app.triage.input_validator import input_validator
from app.triage.intent_resolver import intent_resolver
from app.triage.question_resume import follow_up_interrupt, idempotency_guard, question_planner
from app.triage.renderer_audit import audit_node, deterministic_renderer, green_release_gate
from app.triage.signal_normalizer import signal_normalizer
from app.triage.stage_context_resolver import stage_context_resolver
from app.triage.stage_safety_gate import stage_safety_gate
from app.triage.state import TriageState
from app.triage.target_entity_resolver import target_entity_resolver


def signal_merge_conflict_detector(state):
    return signal_normalizer(state)


def question_catalog_filter(state):
    return entity_stage_validator(state)


def _terminal(state: TriageState) -> str:
    return "terminal" if state.get("stopConversation") is True else "continue"


def _idempotency_route(state: TriageState) -> str:
    return (
        "terminal"
        if state.get("requiredAction") in {"STATE_VERSION_CONFLICT", "DUPLICATE_REQUEST"}
        else "continue"
    )


def _follow_up_route(state: TriageState) -> str:
    return (
        "interrupt"
        if state.get("plannedQuestionIds") and state.get("stopConversation") is not True
        else "complete"
    )


def build_triage_graph():
    graph = StateGraph(TriageState)
    nodes = {
        "input_validator": input_validator,
        "global_safety_gate": global_safety_gate,
        "idempotency_guard": idempotency_guard,
        "follow_up_interrupt": follow_up_interrupt,
        "target_entity_resolver": target_entity_resolver,
        "intent_resolver": intent_resolver,
        "stage_context_resolver": stage_context_resolver,
        "signal_normalizer": signal_normalizer,
        "signal_merge_conflict_detector": signal_merge_conflict_detector,
        "entity_stage_validator": entity_stage_validator,
        "stage_safety_gate": stage_safety_gate,
        "dataset_calculator": dataset_calculator,
        "scope_calculator": scope_calculator,
        "coverage_resolver": coverage_resolver,
        "question_catalog_filter": question_catalog_filter,
        "clinical_rule_engine": clinical_rule_engine,
        "question_planner": question_planner,
        "green_release_gate": green_release_gate,
        "deterministic_renderer": deterministic_renderer,
        "audit_node": audit_node,
    }
    for name, node in nodes.items():
        graph.add_node(name, node)
    graph.set_entry_point("input_validator")
    graph.add_edge("input_validator", "global_safety_gate")
    graph.add_conditional_edges(
        "global_safety_gate", _terminal,
        {"terminal": "green_release_gate", "continue": "idempotency_guard"},
    )
    graph.add_conditional_edges(
        "idempotency_guard", _idempotency_route,
        {"terminal": "green_release_gate", "continue": "target_entity_resolver"},
    )
    graph.add_edge("target_entity_resolver", "intent_resolver")
    graph.add_edge("intent_resolver", "stage_context_resolver")
    graph.add_edge("stage_context_resolver", "signal_normalizer")
    graph.add_edge("signal_normalizer", "signal_merge_conflict_detector")
    graph.add_edge("signal_merge_conflict_detector", "entity_stage_validator")
    graph.add_edge("entity_stage_validator", "stage_safety_gate")
    graph.add_conditional_edges(
        "stage_safety_gate", _terminal,
        {"terminal": "green_release_gate", "continue": "dataset_calculator"},
    )
    graph.add_edge("dataset_calculator", "scope_calculator")
    # Coverage sits between scope and the rule engine: scope says whether the problem is ours,
    # coverage says whether our rules can actually assess it, and only the second one may refuse
    # the clinical question catalogue.
    graph.add_conditional_edges(
        "scope_calculator", _terminal,
        {"terminal": "green_release_gate", "continue": "coverage_resolver"},
    )
    graph.add_edge("coverage_resolver", "clinical_rule_engine")
    graph.add_edge("clinical_rule_engine", "question_catalog_filter")
    graph.add_edge("question_catalog_filter", "question_planner")
    graph.add_edge("question_planner", "green_release_gate")
    graph.add_edge("green_release_gate", "deterministic_renderer")
    graph.add_conditional_edges(
        "deterministic_renderer", _follow_up_route,
        {"interrupt": "follow_up_interrupt", "complete": "audit_node"},
    )
    graph.add_conditional_edges(
        "follow_up_interrupt", _terminal,
        {"terminal": "green_release_gate", "continue": "idempotency_guard"},
    )
    graph.add_edge("audit_node", END)
    return graph.compile(checkpointer=InMemorySaver())


def graph_config(session_id: str) -> dict[str, dict[str, str]]:
    """Use the session identity as LangGraph's isolated checkpoint thread."""

    return {"configurable": {"thread_id": session_id}}


triage_graph = build_triage_graph()
