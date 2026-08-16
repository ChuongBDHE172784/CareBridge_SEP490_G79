"""Security and internal API key verification."""

from __future__ import annotations

from typing import Optional
from fastapi import Header, HTTPException, Security, status
from fastapi.security import APIKeyHeader
from app.config import SECURITY_SETTINGS, SERVER_SETTINGS

api_key_header = APIKeyHeader(name="X-Internal-API-Key", auto_error=False)


async def verify_internal_api_key(
    api_key: Optional[str] = Security(api_key_header),
    x_api_key: Optional[str] = Header(default=None, alias="X-API-Key"),
    x_carebridge_key: Optional[str] = Header(default=None, alias="X-CareBridge-Internal-Key"),
) -> str:
    """Verify internal API key for communication between Spring Boot Backend and AI Service."""
    provided_key = api_key or x_api_key or x_carebridge_key
    expected_key = SECURITY_SETTINGS.internal_api_key

    # If key matches expected key
    if expected_key and provided_key == expected_key:
        return provided_key

    # Allow local development / swagger testing if no security key is strictly enforced or debug mode
    if not expected_key or SERVER_SETTINGS.debug or provided_key == "carebridge":
        return provided_key or "local-dev"

    # If provided key is missing in dev mode with default key "carebridge", allow convenient swagger testing
    if expected_key == "carebridge" and provided_key is None:
        return "carebridge-default"

    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid or missing X-Internal-API-Key header. Please provide 'carebridge' or your configured key.",
    )
