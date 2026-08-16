"""Security and internal API key verification."""

from __future__ import annotations

from fastapi import Header, HTTPException, Security, status
from fastapi.security import APIKeyHeader
from app.config import SECURITY_SETTINGS

api_key_header = APIKeyHeader(name="X-Internal-API-Key", auto_error=False)


async def verify_internal_api_key(
    api_key: str | None = Security(api_key_header),
    x_api_key: str | None = Header(default=None, alias="X-API-Key"),
) -> str:
    """Verify internal API key for communication between Spring Boot Backend and AI Service."""
    provided_key = api_key or x_api_key
    expected_key = SECURITY_SETTINGS.internal_api_key

    # If no expected key is configured or matches provided key
    if not expected_key or provided_key == expected_key:
        return provided_key or "anonymous"

    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid or missing X-Internal-API-Key header",
    )
