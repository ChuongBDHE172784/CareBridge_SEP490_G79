from __future__ import annotations

import os
import re
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv


BASE_DIR = Path(__file__).resolve().parents[1]
load_dotenv(BASE_DIR / ".env", override=False)


def _float_env(name: str, default: float, minimum: float, maximum: float | None = None) -> float:
    try:
        value = max(minimum, float(os.getenv(name, str(default))))
        return min(value, maximum) if maximum is not None else value
    except (TypeError, ValueError):
        return default


def _int_env(name: str, default: int, minimum: int, maximum: int | None = None) -> int:
    try:
        value = max(minimum, int(os.getenv(name, str(default))))
        return min(value, maximum) if maximum is not None else value
    except (TypeError, ValueError):
        return default


def _bool_env(name: str, default: bool) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    normalized = value.strip().lower()
    if normalized in {"1", "true", "yes", "on"}:
        return True
    if normalized in {"0", "false", "no", "off"}:
        return False
    return default


@dataclass(frozen=True)
class GeminiSettings:
    enabled: bool
    api_key: str | None
    model: str
    timeout_seconds: float
    max_retries: int
    temperature: float


def load_gemini_settings() -> GeminiSettings:
    enabled = _bool_env("GEMINI_ENABLED", False)
    api_key = (os.getenv("GEMINI_API_KEY") or "").strip() or None
    model = (os.getenv("GEMINI_MODEL") or "gemini-2.5-flash").strip()
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._/-]{1,127}", model):
        raise RuntimeError("GEMINI_MODEL is invalid")
    if enabled and not api_key:
        raise RuntimeError(
            "GEMINI_ENABLED=true requires GEMINI_API_KEY. "
            "Set GEMINI_ENABLED=false for deterministic-only mode."
        )
    return GeminiSettings(
        enabled=enabled,
        api_key=api_key,
        model=model,
        timeout_seconds=_float_env("GEMINI_TIMEOUT_SECONDS", 6.0, 0.5, 6.0),
        max_retries=_int_env("GEMINI_MAX_RETRIES", 1, 0, 3),
        temperature=_float_env("GEMINI_TEMPERATURE", 0.2, 0.0, 0.3),
    )


GEMINI_SETTINGS = load_gemini_settings()

MEDICAL_SOURCES_DIR = BASE_DIR / "data" / "medical_sources"
MEDICAL_SOURCES_PENDING_DIR = BASE_DIR / "data" / "medical_sources_pending"
OFFICIAL_SOURCES_FILE = BASE_DIR / "data" / "official_sources.json"
RULE_SOURCE_MAPPING_FILE = BASE_DIR / "data" / "triage_rule_source_mapping.json"

DISCLAIMER = (
    "CareBridge không chẩn đoán bệnh, không kê thuốc và không thay thế bác sĩ hoặc cấp cứu. "
    "Nếu trẻ có dấu hiệu nặng, hãy liên hệ cơ sở y tế hoặc cấp cứu ngay."
)

RISK_COLORS = {
    "GREEN": "#22C55E",
    "YELLOW": "#FACC15",
    "RED": "#EF4444",
    "NEED_MORE_INFO": "#9CA3AF",
}

# Approved evidence domains are fetched from Spring's DB-managed registry.  Do not
# restore a static allowlist here: deployment-time code must not become the source
# of truth for medical evidence approval.
EVIDENCE_REGISTRY_URL = (os.getenv("AI_TRIAGE_EVIDENCE_REGISTRY_URL") or "").rstrip("/")
EVIDENCE_REGISTRY_INTERNAL_KEY = (os.getenv("AI_TRIAGE_INTERNAL_API_KEY") or "").strip()
TRIAGE_V2_INTERNAL_API_KEY = (os.getenv("AI_TRIAGE_INTERNAL_API_KEY") or "").strip()
EVIDENCE_REGISTRY_CACHE_SECONDS = _int_env("AI_TRIAGE_EVIDENCE_REGISTRY_CACHE_SECONDS", 300, 30, 3600)
# A failed lookup is remembered too, on a much shorter TTL. Without this, one unreachable
# registry cost a full connect timeout per candidate source per turn — measured at 13 lookups
# and 52s on a single RED turn, far past the 7s request budget. The TTL stays short because
# the penalty for remembering a failure is only a temporarily missing citation: evidence is
# post-outcome and never changes a risk level.
EVIDENCE_REGISTRY_FAILURE_CACHE_SECONDS = _int_env(
    "AI_TRIAGE_EVIDENCE_REGISTRY_FAILURE_CACHE_SECONDS", 30, 1, 300
)

# Realtime official-source search backend (Google Programmable Search Engine —
# https://programmablesearchengine.google.com/ — free tier: 100 queries/day).
# Plain HTML scraping of a general search engine is not used here: it is
# unreliable (bot-detection/CAPTCHA challenges) and would violate the "no
# CAPTCHA bypass" policy this service must follow. Leave both blank to disable
# realtime search entirely (local Knowledge Base citations still work).
GOOGLE_SEARCH_API_KEY = (os.getenv("GOOGLE_SEARCH_API_KEY") or "").strip()
GOOGLE_SEARCH_ENGINE_ID = (os.getenv("GOOGLE_SEARCH_ENGINE_ID") or "").strip()

GRAPH_VERSION = "tv5-gemini-assisted-triage-1.1"
RULE_SET_VERSION = "pediatric-risk-rules-1.0"
ONTOLOGY_VERSION = "child-symptoms-1.0"
RESPONSE_SCHEMA_VERSION = "2.1"
PYTHON_SERVICE_TIMEOUT_SECONDS = _float_env("AI_TRIAGE_SERVICE_TIMEOUT_SECONDS", 7.0, 0.5, 7.0)
REALTIME_SEARCH_TIMEOUT_SECONDS = min(
    _float_env("AI_TRIAGE_REALTIME_TIMEOUT_SECONDS", 3.5, 0.1, 4.0),
    max(0.1, PYTHON_SERVICE_TIMEOUT_SECONDS - 0.5),
)
MIN_REALTIME_SEARCH_REMAINING_SECONDS = 1.0
EVIDENCE_CACHE_TTL_DAYS = _int_env("AI_TRIAGE_EVIDENCE_CACHE_TTL_DAYS", 30, 1)

OFFICIAL_SOURCE_WARNING = (
    "Kết quả do rule engine nội bộ xác định nhưng chưa tìm thấy nguồn chính thống phù hợp "
    "trong Knowledge Base. Vui lòng kiểm tra lại thư viện nguồn."
)
NO_SOURCE_WARNING = "Không tìm thấy nguồn phù hợp trong knowledge base."
NO_OFFICIAL_REALTIME_SOURCE_WARNING = (
    "Không tìm thấy nguồn chính thống phù hợp trong Knowledge Base và realtime official search."
)
REALTIME_SEARCH_SKIPPED_DEADLINE_WARNING = (
    "KhÃ´ng Ä‘á»§ thá»i gian cÃ²n láº¡i Ä‘á»ƒ truy xuáº¥t nguá»“n chÃ­nh thá»©c; "
    "phÃ¢n loáº¡i rá»§i ro váº«n do bá»™ quy táº¯c quyáº¿t Ä‘á»‹nh."
)
RED_LOCAL_EVIDENCE_GAP_WARNING = (
    "Không tìm thấy nguồn local đã duyệt phù hợp. Kết quả RED được trả ngay và không chờ realtime search."
)
LEGAL_SAFETY_NOTE = (
    "Kết quả này là phân loại rủi ro ban đầu dựa trên triệu chứng và nguồn tham khảo "
    "chính thống, không phải chẩn đoán y khoa."
)
