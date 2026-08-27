"""Spring canonical-conversation API adapter."""

from __future__ import annotations

import time
from typing import Any
from uuid import uuid4

import requests

from ..assertions import evaluate_response
from ..config import EvaluationSettings
from ..models import BenchmarkCase, CaseResult, ExecutionMode, ExecutionStatus, ExpectedExecutionStatus
from ..preflight import ApiPreflight, PreflightResult
from ..sanitization import minimize_response
from .local import _identity, _not_run, _scope_result


class ApiAdapter:
    def __init__(self, settings: EvaluationSettings, session: requests.Session | None = None) -> None:
        self.settings = settings
        self.session = session or requests.Session()
        self.preflight_result: PreflightResult | None = None

    def preflight(self) -> PreflightResult:
        self.preflight_result = ApiPreflight(self.settings, self.session).run()
        return self.preflight_result

    def execute(self, case: BenchmarkCase, run_id: str) -> CaseResult:
        if case.expectedExecutionStatus != ExpectedExecutionStatus.EXECUTE:
            return _scope_result(case, run_id, ExecutionMode.API_END_TO_END)
        if ExecutionMode.API_END_TO_END not in case.supportedModes:
            return _not_run(case, run_id, ExecutionMode.API_END_TO_END, "Case is not supported by the conversation API adapter")
        if self.preflight_result is None or not self.preflight_result.ready:
            reason = self.preflight_result.reason if self.preflight_result else "API preflight was not run"
            return CaseResult(
                **_identity(case, run_id, ExecutionMode.API_END_TO_END),
                executionStatus=ExecutionStatus.INFRASTRUCTURE_SKIPPED,
                passed=None,
                failureReasons=[reason],
            )

        started = time.perf_counter()
        deadline = started + self.settings.case_budget_seconds
        try:
            payload = self._start_payload(case)
            start = self._post("/api/v1/triage/intake/conversation/start", payload, retry_safe=True)
            envelope = self._unwrap(start)
            session_id = envelope.get("intakeSessionId")
            if not session_id:
                raise ValueError("Conversation start response did not include intakeSessionId")
            questions_by_round = [_question_keys(envelope)]
            for turn in case.turns:
                if time.perf_counter() >= deadline:
                    raise TimeoutError("Evaluation case exceeded its total 90-second budget")
                if envelope.get("status") == "TRIAGE_COMPLETE":
                    break
                # Do not retry continue: duplicate side effects must be prevented by Spring, not guessed by this runner.
                continued = self._post(
                    "/api/v1/triage/intake/conversation/continue",
                    {"intakeSessionId": session_id, "newAnswers": turn.answers},
                    retry_safe=False,
                )
                envelope = self._unwrap(continued)
                questions_by_round.append(_question_keys(envelope))

            persisted = self._get(f"/api/v1/triage/intake/{session_id}")
            persisted_data = self._unwrap(persisted)
            triage = envelope.get("triageResult") or persisted_data
            response = dict(envelope)
            if isinstance(triage, dict):
                response["triageResult"] = triage
            response = minimize_response(response)
            result = CaseResult(
                **_identity(case, run_id, ExecutionMode.API_END_TO_END),
                executionStatus=ExecutionStatus.PASSED,
                actualRisk=(triage or {}).get("riskLevel") if isinstance(triage, dict) else None,
                latencyMs=(time.perf_counter() - started) * 1000,
                fallbackUsed=(triage or {}).get("fallbackUsed") if isinstance(triage, dict) else None,
                assistantProvider=(triage or {}).get("assistantProvider") if isinstance(triage, dict) else None,
                conversationStatus=envelope.get("status"),
                questionsByRound=questions_by_round,
                response=response,
            )
            return evaluate_response(case, result)
        except requests.Timeout:
            return self._failure(case, run_id, started, "API request timed out after evaluator timeout")
        except requests.RequestException as exc:
            return self._failure(case, run_id, started, f"API network error: {type(exc).__name__}")
        except (TimeoutError, ValueError) as exc:
            return self._failure(case, run_id, started, str(exc))

    def _start_payload(self, case: BenchmarkCase) -> dict[str, Any]:
        intake = dict(case.input)
        stage = case.stage.value if case.stage else None
        if not stage:
            raise ValueError("Main benchmark API cases must specify stage explicitly")
        profile_key = "babyProfileId" if stage in {"INFANT", "TODDLER"} else "motherProfileId"
        profile_id = self.settings.baby_profile_id if profile_key == "babyProfileId" else self.settings.mother_profile_id
        if case.requiresProfile and not profile_id:
            raise ValueError(f"Case requires {profile_key}, but the fixture is not configured")
        if profile_id:
            intake[profile_key] = profile_id
        return {
            "clientRequestId": f"eval_{uuid4().hex}",
            "initialText": intake.get("parentFreeText") or ", ".join(intake.get("symptomList") or []),
            "stage": stage,
            profile_key: profile_id,
            "currentIntake": intake,
        }

    def _request(self, method: str, path: str, *, payload: dict[str, Any] | None, retry_safe: bool) -> requests.Response:
        attempts = 1 + (self.settings.max_retries if retry_safe else 0)
        last_error: requests.RequestException | None = None
        for attempt in range(attempts):
            try:
                response = self.session.request(
                    method,
                    f"{self.settings.api_base_url.rstrip('/')}{path}",
                    headers={"Authorization": f"Bearer {self.settings.test_jwt}", "Content-Type": "application/json"},
                    json=payload,
                    timeout=self.settings.request_timeout_seconds,
                )
                if response.status_code == 409 and "TRIAGE-014" in response.text:
                    raise ValueError("TRIAGE-014: active session is missing canonical stage")
                response.raise_for_status()
                return response
            except requests.RequestException as exc:
                last_error = exc
                if attempt + 1 >= attempts:
                    raise
        raise last_error or RuntimeError("Unreachable request state")

    def _post(self, path: str, payload: dict[str, Any], *, retry_safe: bool) -> requests.Response:
        return self._request("POST", path, payload=payload, retry_safe=retry_safe)

    def _get(self, path: str) -> requests.Response:
        return self._request("GET", path, payload=None, retry_safe=True)

    @staticmethod
    def _unwrap(response: requests.Response) -> dict[str, Any]:
        payload = response.json()
        if not isinstance(payload, dict):
            raise ValueError("API response must be a JSON object")
        data = payload.get("data", payload)
        if not isinstance(data, dict):
            raise ValueError("API response data must be a JSON object")
        return data

    @staticmethod
    def _failure(case: BenchmarkCase, run_id: str, started: float, reason: str) -> CaseResult:
        return CaseResult(
            **_identity(case, run_id, ExecutionMode.API_END_TO_END),
            executionStatus=ExecutionStatus.FAILED,
            passed=False,
            failureReasons=[reason],
            latencyMs=(time.perf_counter() - started) * 1000,
        )


def _question_keys(envelope: dict[str, Any]) -> list[str]:
    return [
        str(question.get("questionKey"))
        for question in envelope.get("questions") or []
        if isinstance(question, dict) and question.get("questionKey")
    ]
