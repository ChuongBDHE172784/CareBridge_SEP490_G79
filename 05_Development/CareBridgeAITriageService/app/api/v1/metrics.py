"""Health metrics screening API router."""

from __future__ import annotations

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import verify_internal_api_key
from app.models.schemas import (
    HealthMetricsEvaluationResponse,
    HealthMetricsLogRequest,
)
from app.services.metrics_screening_service import get_metrics_screening_service

router = APIRouter(prefix="/metrics", tags=["Health Metrics Screening"])


@router.post(
    "/evaluate",
    response_model=HealthMetricsEvaluationResponse,
    summary="Đánh giá chỉ số sinh hiệu & Phát hiện nguy cơ bất thường/khẩn cấp (Bước 7 ➔ 8 ➔ 9)",
)
async def evaluate_maternal_metrics(
    request: HealthMetricsLogRequest,
    _auth: str = Depends(verify_internal_api_key),
    db: AsyncSession = Depends(get_db),
) -> HealthMetricsEvaluationResponse:
    """Screen maternal vital signs (BP, Glucose, Temp, Fetal kicks) against clinical danger rules and RAG knowledge."""
    service = get_metrics_screening_service()
    return await service.evaluate_metrics(request, session=db)
