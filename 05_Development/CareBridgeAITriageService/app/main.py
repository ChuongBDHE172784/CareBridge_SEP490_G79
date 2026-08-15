"""CareBridge canonical deterministic triage transport.

Only the Java authority boundary may call the mutation endpoint.  The former child/intake
endpoints were removed after the mobile application cut over to the canonical session API; keeping
them here would leave a second clinical decision graph reachable at runtime.
"""

from fastapi import Depends, FastAPI
from fastapi.responses import JSONResponse

from app.config import GEMINI_SETTINGS
from app.gemini_client import get_gemini_client
from app.rules.registry import get_registry
from app.schemas import EmergencySupportRequest, EmergencySupportResponse
from app.services.emergency_support_service import (
    build_response as build_emergency_response,
)
from app.triage.api import (
    TriageTurnRequest,
    TriageTurnResponse,
    execute_turn,
    require_internal_key,
)


app = FastAPI(title="CareBridge AI Triage Service", version="1.0.0")


@app.post(
    "/internal/triage/turn",
    response_model=TriageTurnResponse,
    dependencies=[Depends(require_internal_key)],
    include_in_schema=False,
)
def triage_turn(request: TriageTurnRequest) -> TriageTurnResponse:
    """Execute one canonical deterministic triage turn."""

    return execute_turn(request)


@app.post(
    "/internal/emergency/support",
    response_model=EmergencySupportResponse,
    dependencies=[Depends(require_internal_key)],
    tags=["emergency"],
)
def emergency_support(request: EmergencySupportRequest) -> EmergencySupportResponse:
    """Return the emergency package: verified hotlines first, facilities where available."""

    return build_emergency_response(request)


@app.get("/health")
def health() -> JSONResponse:
    try:
        client = get_gemini_client()
    except Exception:
        # Gemini augments fact extraction but is not part of deterministic readiness.
        client = None
    try:
        _run_deterministic_readiness_probe()
        deterministic_ready = True
    except Exception:
        # Do not expose rules, clinical inputs, or exception details from this public probe.
        deterministic_ready = False
    payload = {
        "status": "UP" if deterministic_ready else "DOWN",
        "readiness": "READY" if deterministic_ready else "NOT_READY",
        "deterministicEngine": {
            "ready": deterministic_ready,
            "probe": "CANONICAL_RED_PRECEDENCE",
        },
        "geminiEnabled": GEMINI_SETTINGS.enabled,
        "geminiConfigured": bool(GEMINI_SETTINGS.api_key),
        "geminiReachable": client.reachable if client is not None else False,
        "model": GEMINI_SETTINGS.model,
    }
    return JSONResponse(status_code=200 if deterministic_ready else 503, content=payload)


def _run_deterministic_readiness_probe() -> None:
    registry = get_registry()
    result = execute_turn(
        TriageTurnRequest(
            sessionId="10000000-0000-0000-0000-000000000001",
            stateVersion=0,
            expectedStateVersion=0,
            requestId="readiness_request_1",
            messageId="readiness_message_1",
            latestUserMessage="Tôi đang co giật",
            selectedTarget="MOTHER",
            journeyContext={"stage": "PREGNANCY"},
            expectedRulesetHash=registry.ruleset_sha256,
        )
    )
    state = result.state
    if state.get("triageOutcome") != "RED" or not state.get("stopConversation"):
        raise RuntimeError("deterministic RED precedence probe failed")
