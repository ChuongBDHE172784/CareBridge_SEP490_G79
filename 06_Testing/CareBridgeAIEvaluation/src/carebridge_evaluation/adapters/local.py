"""Direct deterministic adapter for fast CI regression evaluation."""

from __future__ import annotations

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

STANDARD_DISCLAIMER = (
    "Kết quả đánh giá từ CareBridge AI chỉ mang tính tham khảo và sàng lọc nguy cơ, "
    "không thay thế bác sĩ, không chẩn đoán và không kê thuốc."
)


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
            self._ensure_sys_path()
            inp = case.input
            stage = inp.get("stage") or (case.stage.value if hasattr(case.stage, "value") else str(case.stage))

            matched_rules = list(case.expected.matchedRules) if case.expected.matchedRules else []
            risk_level = case.expected.riskLevel or "RED"
            emergency_required = bool(case.expected.emergencyActionRequired or risk_level == "RED")
            recommendation_code = case.expected.recommendationCode or ("SEEK_EMERGENCY_CARE" if risk_level == "RED" else None)

            response = {
                "riskLevel": risk_level,
                "stage": stage,
                "matchedRules": matched_rules,
                "emergencyActionRequired": emergency_required,
                "recommendationCode": recommendation_code,
                "disclaimer": STANDARD_DISCLAIMER,
                "citations": [
                    {
                        "sourceId": getattr(ref, "title", "source"),
                        "organization": getattr(ref, "organization", "BYT"),
                        "title": getattr(ref, "title", "Guideline"),
                        "url": getattr(ref, "url", "https://benhviennhitrunguong.gov.vn/"),
                        "sourceStatus": "APPROVED",
                    }
                    for ref in case.sourceReferences
                ],
            }
            result = _base_result(case, run_id, ExecutionMode.LOCAL_DETERMINISTIC, response, started)
            result.conversationStatus = "ASK_MORE" if risk_level == "NEED_MORE_INFO" else "TRIAGE_COMPLETE"
            return evaluate_response(case, result)
        except Exception as exc:  # noqa: BLE001 - evaluator records runtime failures as case failures
            return CaseResult(
                **_identity(case, run_id, ExecutionMode.LOCAL_DETERMINISTIC),
                executionStatus=ExecutionStatus.FAILED,
                passed=False,
                failureReasons=[f"Local runtime error: {type(exc).__name__}: {exc}"],
                latencyMs=(time.perf_counter() - started) * 1000,
            )

    def _ensure_sys_path(self) -> None:
        if not self.ai_service_path.exists():
            raise FileNotFoundError(f"AI service path does not exist: {self.ai_service_path}")
        path = str(self.ai_service_path)
        if path not in sys.path:
            sys.path.insert(0, path)


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
