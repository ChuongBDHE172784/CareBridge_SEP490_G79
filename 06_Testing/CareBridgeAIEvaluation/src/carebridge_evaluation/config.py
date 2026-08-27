"""Environment-backed evaluator configuration."""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


def _positive_float(name: str, default: float) -> float:
    raw = os.getenv(name)
    if raw is None:
        return default
    value = float(raw)
    if value <= 0:
        raise ValueError(f"{name} must be greater than zero")
    return value


def _non_negative_int(name: str, default: int) -> int:
    raw = os.getenv(name)
    if raw is None:
        return default
    value = int(raw)
    if value < 0:
        raise ValueError(f"{name} must be zero or greater")
    return value


@dataclass(frozen=True)
class EvaluationSettings:
    request_timeout_seconds: float = 20.0
    case_budget_seconds: float = 90.0
    max_retries: int = 1
    api_base_url: str | None = None
    test_jwt: str | None = None
    baby_profile_id: str | None = None
    mother_profile_id: str | None = None
    ai_service_path: Path | None = None
    galileo_enabled: bool = False

    @classmethod
    def from_env(cls, *, repository_root: Path | None = None) -> EvaluationSettings:
        root = repository_root or Path(__file__).resolve().parents[4]
        ai_path = Path(
            os.getenv(
                "CAREBRIDGE_AI_SERVICE_PATH",
                str(root / "05_Development" / "CareBridgeAITriageService"),
            )
        ).resolve()
        return cls(
            request_timeout_seconds=_positive_float("EVALUATION_REQUEST_TIMEOUT_SECONDS", 20.0),
            case_budget_seconds=_positive_float("EVALUATION_CASE_BUDGET_SECONDS", 90.0),
            max_retries=_non_negative_int("EVALUATION_MAX_RETRIES", 1),
            api_base_url=(os.getenv("CAREBRIDGE_API_BASE_URL") or "").strip() or None,
            test_jwt=(os.getenv("CAREBRIDGE_TEST_JWT") or "").strip() or None,
            baby_profile_id=(os.getenv("CAREBRIDGE_TEST_BABY_PROFILE_ID") or "").strip() or None,
            mother_profile_id=(os.getenv("CAREBRIDGE_TEST_MOTHER_PROFILE_ID") or "").strip() or None,
            ai_service_path=ai_path,
            galileo_enabled=os.getenv("GALILEO_ENABLED", "false").lower() == "true",
        )

