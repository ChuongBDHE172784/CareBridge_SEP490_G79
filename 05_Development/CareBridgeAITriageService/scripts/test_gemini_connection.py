from __future__ import annotations

import sys
import os
from pathlib import Path

from dotenv import load_dotenv

SERVICE_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SERVICE_ROOT))
load_dotenv(SERVICE_ROOT / ".env", override=False)

if (os.getenv("GEMINI_ENABLED") or "false").strip().lower() in {"1", "true", "yes", "on"} \
        and not (os.getenv("GEMINI_API_KEY") or "").strip():
    print("Gemini smoke failed: GEMINI_API_KEY is not configured.")
    raise SystemExit(2)

from app.config import GEMINI_SETTINGS  # noqa: E402
from app.gemini_client import GeminiClient  # noqa: E402


def main() -> int:
    if not GEMINI_SETTINGS.enabled:
        print("Gemini smoke failed: GEMINI_ENABLED is not true.")
        return 2
    if not GEMINI_SETTINGS.api_key:
        print("Gemini smoke failed: GEMINI_API_KEY is not configured.")
        return 2

    client = GeminiClient()
    result = client.normalize_symptom_text(
        text="Bé ho nhẹ và chảy mũi.",
        child_age_months=12,
        allowed_codes={"cough", "runny_nose"},
    )
    if result is None:
        print(f"Gemini smoke failed for model {GEMINI_SETTINGS.model}.")
        return 1
    codes = [item.code for item in result.normalizedSymptoms]
    if not set(codes) <= {"cough", "runny_nose"}:
        print("Gemini smoke failed: structured output contained an invalid code.")
        return 1
    print(f"Gemini structured-output smoke passed: model={GEMINI_SETTINGS.model}, codes={codes}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
