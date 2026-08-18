"""CareBridge AI Maternal RAG & Health Metrics Screening Service."""

from __future__ import annotations

import logging
from typing import Any, Dict
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware

from app.api.health import router as health_router
from app.api.v1.chat import router as chat_router
from app.api.v1.documents import router as documents_router
from app.api.v1.metrics import router as metrics_router
from app.config import SERVER_SETTINGS
from app.models.schemas import HealthMetricsLogRequest, MaternalStage
from app.services.ingestion_service import get_ingestion_service
from app.services.metrics_screening_service import get_metrics_screening_service

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup and shutdown lifecycle events."""
    logger.info("CareBridge AI Maternal RAG Service ready on port 8001.")
    yield
    logger.info("Shutting down CareBridge AI Maternal RAG Service.")


app = FastAPI(
    title="CareBridge AI Maternal Health RAG & Triage Service",
    description="Hệ thống AI RAG & Sàng lọc Chỉ số Sức khỏe Mẹ bầu sử dụng Gemini Flash và pgvector",
    version="2.0.0",
    lifespan=lifespan,
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register API Routers
app.include_router(health_router)
app.include_router(metrics_router, prefix="/api/v1")
app.include_router(chat_router, prefix="/api/v1")
app.include_router(documents_router, prefix="/api/v1")


# Backward Compatibility Endpoint for Spring Boot CareBridgeAPI
@app.post("/internal/triage/turn", tags=["Backward Compatibility"])
async def handle_internal_triage_turn(request: Request) -> Dict[str, Any]:
    """Compatibility bridge for Spring Boot CareBridgeAPI HttpTriageWorkflowClient."""
    try:
        body = await request.json()
    except Exception:
        body = {}

    state = body.get("state", {}) if isinstance(body, dict) else {}
    stage_str = str(state.get("stage", "PREGNANCY")).upper()
    stage = MaternalStage.PREGNANCY
    if "POSTPARTUM" in stage_str:
        stage = MaternalStage.POSTPARTUM
    elif "PRECONCEPTION" in stage_str:
        stage = MaternalStage.PRECONCEPTION

    # Run modern metrics screening service
    service = get_metrics_screening_service()
    eval_req = HealthMetricsLogRequest(
        stage=stage,
        gestational_age_weeks=int(state.get("gestationalAgeWeeks", 20)),
        systolic_bp=int(state.get("systolicBp", 120)) if state.get("systolicBp") else None,
        diastolic_bp=int(state.get("diastolicBp", 80)) if state.get("diastolicBp") else None,
        temperature=float(state.get("temperature", 36.8)) if state.get("temperature") else None,
        symptoms=[str(s) for s in state.get("symptoms", [])] if state.get("symptoms") else [],
    )
    result = await service.evaluate_metrics(eval_req)

    return {
        "state": {
            **state,
            "status": result.status.value,
            "emergency_mode": result.emergency_mode,
            "headline": result.headline,
            "summary": result.summary,
            "risk_factors": result.risk_factors,
            "suggested_action": result.suggested_action,
        },
        "readiness": "READY",
        "rulesetVersion": "2.0.0-rag",
        "rulesetHash": "carebridge-rag-pgvector",
    }
