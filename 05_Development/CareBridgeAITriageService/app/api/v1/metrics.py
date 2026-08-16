"""Health metrics screening API router."""

from __future__ import annotations

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import verify_internal_api_key
from app.models.schemas import (
    BatchSimulationCase,
    BatchSimulationResponse,
    HealthMetricsEvaluationResponse,
    HealthMetricsLogRequest,
    MaternalStage,
    TriageRiskStatus,
)
from app.services.metrics_screening_service import get_metrics_screening_service

router = APIRouter(prefix="/metrics", tags=["Health Metrics Screening & Triage Simulation"])


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


@router.post(
    "/simulate-batch",
    response_model=BatchSimulationResponse,
    summary="[Clinical Simulator] Chạy tự động bộ 5 ca lâm sàng mẫu để kiểm tra & demo cho Hội đồng",
)
async def simulate_clinical_cases(
    _auth: str = Depends(verify_internal_api_key),
    db: AsyncSession = Depends(get_db),
) -> BatchSimulationResponse:
    """Run a batch of verified clinical benchmark test cases (Preeclampsia, high fever, kick loss, normal)."""
    service = get_metrics_screening_service()

    cases = [
        BatchSimulationCase(
            case_name="Ca 1: Thai phụ 32 tuần, Huyết áp 165/110 + Đau đầu, Nhìn mờ (Tiền sản giật nặng)",
            metrics=HealthMetricsLogRequest(
                stage=MaternalStage.PREGNANCY,
                gestational_age_weeks=32,
                systolic_bp=165,
                diastolic_bp=110,
                symptoms=["Đau đầu dữ dội", "Hoa mắt nhìn mờ"],
            ),
            expected_status=TriageRiskStatus.CRITICAL_EMERGENCY,
        ),
        BatchSimulationCase(
            case_name="Ca 2: Mẹ bầu 24 tuần, Sốt cao 39.2°C",
            metrics=HealthMetricsLogRequest(
                stage=MaternalStage.PREGNANCY,
                gestational_age_weeks=24,
                temperature=39.2,
                systolic_bp=120,
                diastolic_bp=80,
            ),
            expected_status=TriageRiskStatus.CRITICAL_EMERGENCY,
        ),
        BatchSimulationCase(
            case_name="Ca 3: Thai phụ 34 tuần, 2 giờ không thấy thai cử động (Mất cử động thai)",
            metrics=HealthMetricsLogRequest(
                stage=MaternalStage.PREGNANCY,
                gestational_age_weeks=34,
                fetal_movements_count=0,
                fetal_movements_duration_hours=2,
            ),
            expected_status=TriageRiskStatus.CRITICAL_EMERGENCY,
        ),
        BatchSimulationCase(
            case_name="Ca 4: Thai phụ 20 tuần, Huyết áp 118/76, Thân nhiệt 36.6°C (Bình thường)",
            metrics=HealthMetricsLogRequest(
                stage=MaternalStage.PREGNANCY,
                gestational_age_weeks=20,
                systolic_bp=118,
                diastolic_bp=76,
                temperature=36.6,
                blood_glucose=4.8,
            ),
            expected_status=TriageRiskStatus.NORMAL,
        ),
        BatchSimulationCase(
            case_name="Ca 5: Mẹ bầu 12 tuần, Tiền tăng huyết áp 135/88, Buồn nôn nhẹ (Cần theo dõi)",
            metrics=HealthMetricsLogRequest(
                stage=MaternalStage.PREGNANCY,
                gestational_age_weeks=12,
                systolic_bp=135,
                diastolic_bp=88,
                symptoms=["Buồn nôn"],
            ),
            expected_status=TriageRiskStatus.ANOMALY_MONITOR,
        ),
    ]

    passed_count = 0
    results: list[BatchSimulationCase] = []

    for case in cases:
        eval_res = await service.evaluate_metrics(case.metrics, session=db)
        is_passed = eval_res.status == case.expected_status
        if is_passed:
            passed_count += 1
        case.actual_status = eval_res.status
        case.passed = is_passed
        case.headline = eval_res.headline
        results.append(case)

    return BatchSimulationResponse(
        total_cases=len(cases),
        passed_cases=passed_count,
        all_passed=(passed_count == len(cases)),
        results=results,
    )
