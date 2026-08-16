"""Configuration settings for CareBridge AI Maternal RAG & Health Metrics Screening Service."""

from __future__ import annotations

import os
from pathlib import Path
from pydantic import BaseModel
from dotenv import load_dotenv

load_dotenv()

BASE_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = BASE_DIR / "data"
RAW_DOCS_DIR = DATA_DIR / "raw_documents"

# Ensure directories exist
DATA_DIR.mkdir(exist_ok=True)
RAW_DOCS_DIR.mkdir(exist_ok=True)


class DatabaseSettings(BaseModel):
    # Support PostgreSQL with pgvector
    url: str = os.getenv(
        "DATABASE_URL",
        "postgresql+asyncpg://carebridge:carebridge@localhost:5433/carebridge",
    )
    sync_url: str = os.getenv(
        "SYNC_DATABASE_URL",
        "postgresql://carebridge:carebridge@localhost:5433/carebridge",
    )
    pool_size: int = int(os.getenv("DB_POOL_SIZE", "10"))
    max_overflow: int = int(os.getenv("DB_MAX_OVERFLOW", "20"))


class GeminiSettings(BaseModel):
    api_key: str = os.getenv("GEMINI_API_KEY", "")
    model: str = os.getenv("GEMINI_MODEL", "gemini-flash-lite-latest")
    embedding_model: str = os.getenv("GEMINI_EMBEDDING_MODEL", "gemini-embedding-001")
    embedding_dimension: int = 768
    temperature: float = float(os.getenv("GEMINI_TEMPERATURE", "0.3"))
    timeout_seconds: float = float(os.getenv("GEMINI_TIMEOUT_SECONDS", "15.0"))
    enabled: bool = os.getenv("GEMINI_ENABLED", "true").lower() in ("true", "1", "yes")


class SecuritySettings(BaseModel):
    internal_api_key: str = os.getenv("AI_TRIAGE_INTERNAL_API_KEY", "carebridge")


class ServerSettings(BaseModel):
    host: str = os.getenv("HOST", "0.0.0.0")
    port: int = int(os.getenv("PORT", "8001"))
    debug: bool = os.getenv("DEBUG", "false").lower() in ("true", "1", "yes")


DB_SETTINGS = DatabaseSettings()
GEMINI_SETTINGS = GeminiSettings()
SECURITY_SETTINGS = SecuritySettings()
SERVER_SETTINGS = ServerSettings()

MEDICAL_DISCLAIMER = (
    "Lưu ý: Thông tin do AI cung cấp chỉ mang tính chất tham khảo và hướng dẫn cẩm nang y tế, "
    "không thay thế cho chẩn đoán, xét nghiệm và điều trị trực tiếp từ Bác sĩ chuyên khoa."
)
