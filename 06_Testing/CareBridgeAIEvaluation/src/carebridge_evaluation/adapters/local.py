"""Direct deterministic adapter for fast CI regression evaluation."""

from __future__ import annotations

import importlib
import sys
import time
from pathlib import Path
from typing import Any

from ..assertions import evaluate_response
from ..models import (
    BenchmarkCase,
    CaseResult,
    ExecutionMode,
    ExecutionStatus,
    ExpectedExecutionStatus,
)
from ..sanitization import minimize_response


class LocalAdapter:
    def __init__(self, ai_service_path: Path) -> None:
        self.ai_service_path = ai_service_path

    def execute(self, case: BenchmarkCase, run_id: str) -> CaseResult:
        if case.expectedExecutionStatus != ExpectedExecutionStatus.EXECUTE:
            return _scope_result(case, run_id, ExecutionMode.LOCAL_DETERMINISTIC)
        if ExecutionMode.LOCAL_DETERMINISTIC not in case.supportedModes:
            return _not_run(case, run_id, ExecutionMode.LOCAL_DETERMINISTIC, "Case is not supported by the direct local adapter")

        started = time.perf_counter()
        try:
            run_triage, request_type, ask_followup_questions = self._runtime()
            request = request_type.model_validate(case.input)
            response = minimize_response(run_triage(request, deterministic_only=True).model_dump(mode="json"))
            result = _base_result(case, run_id, ExecutionMode.LOCAL_DETERMINISTIC, response, started)
            result.conversationStatus = "ASK_MORE" if response.get("riskLevel") == "NEED_MORE_INFO" else "TRIAGE_COMPLETE"
            if result.conversationStatus == "ASK_MORE":
                result.questionsByRound = [[
                    question.questionKey for question in ask_followup_questions(request)
                ]]
            return evaluate_response(case, result)
        except Exception as exc:  # noqa: BLE001 - evaluator records runtime failures as case failures
            return CaseResult(
                **_identity(case, run_id, ExecutionMode.LOCAL_DETERMINISTIC),
                executionStatus=ExecutionStatus.FAILED,
                passed=False,
                failureReasons=[f"Local runtime error: {type(exc).__name__}: {exc}"],
                latencyMs=(time.perf_counter() - started) * 1000,
            )

    def _runtime(self) -> tuple[Any, Any, Any]:
        if not self.ai_service_path.exists():
            raise FileNotFoundError(f"AI service path does not exist: {self.ai_service_path}")
        path = str(self.ai_service_path)
        if path not in sys.path:
            sys.path.insert(0, path)
        graph = importlib.import_module("app.graph")
        question_engine = importlib.import_module("app.intake_question_engine")
        schemas = importlib.import_module("app.schemas")
        return graph.run_triage, schemas.ChildTriageRequest, question_engine.ask_followup_questions


def _identity(case: BenchmarkCase, run_id: str, mode: ExecutionMode) -> dict[str, Any]:
    return {
        "runId": run_id,
        "caseId": case.id,
        "category": case.category,
        "stage": case.stage,
        "journeyPhase": case.journeyPhase,
        "executionMode": mode,
        "reviewStatus": case.reviewStatus,
        "expectedRisk": case.expected.riskLevel,
    }


def _base_result(
    case: BenchmarkCase,
    run_id: str,
    mode: ExecutionMode,
    response: dict[str, Any],
    started: float,
) -> CaseResult:
    return CaseResult(
        **_identity(case, run_id, mode),
        executionStatus=ExecutionStatus.PASSED,
        actualRisk=response.get("riskLevel"),
        latencyMs=(time.perf_counter() - started) * 1000,
        fallbackUsed=response.get("fallbackUsed"),
        assistantProvider=response.get("assistantProvider"),
        response=response,
    )


def _scope_result(case: BenchmarkCase, run_id: str, mode: ExecutionMode) -> CaseResult:
    status = (
        ExecutionStatus.KNOWN_SCOPE_GAP
        if case.expectedExecutionStatus == ExpectedExecutionStatus.KNOWN_SCOPE_GAP
        else ExecutionStatus.UNSUPPORTED_SCOPE
    )
    return CaseResult(
        **_identity(case, run_id, mode),
        executionStatus=status,
        passed=None,
        failureReasons=[f"Expected scope status: {case.expectedExecutionStatus.value}"],
    )


def _not_run(case: BenchmarkCase, run_id: str, mode: ExecutionMode, reason: str) -> CaseResult:
    return CaseResult(
        **_identity(case, run_id, mode),
        executionStatus=ExecutionStatus.NOT_RUN,
        passed=None,
        failureReasons=[reason],
    )
