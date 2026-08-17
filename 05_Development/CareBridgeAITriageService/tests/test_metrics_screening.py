"""Tests for maternal health metrics screening and danger detection."""

import pytest
from app.models.schemas import (
    HealthMetricsLogRequest,
    MaternalStage,
    TriageRiskStatus,
)
from app.services.metrics_screening_service import MetricsScreeningService


@pytest.mark.asyncio
async def test_normal_maternal_metrics():
    service = MetricsScreeningService()
    request = HealthMetricsLogRequest(
        stage=MaternalStage.PREGNANCY,
        gestational_age_weeks=20,
        systolic_bp=115,
        diastolic_bp=75,
        blood_glucose=4.8,
        is_fasting_glucose=True,
        temperature=36.7,
        heart_rate=78,
    )
    result = await service.evaluate_metrics(request)
    assert result.status == TriageRiskStatus.NORMAL
    assert result.emergency_mode is False
    assert len(result.risk_factors) == 0


@pytest.mark.asyncio
async def test_critical_severe_preeclampsia():
    service = MetricsScreeningService()
    request = HealthMetricsLogRequest(
        stage=MaternalStage.PREGNANCY,
        gestational_age_weeks=32,
        systolic_bp=165,
        diastolic_bp=110,
        symptoms=["Đau đầu dữ dội", "Hoa mắt nhìn mờ", "Phù mặt"],
    )
    result = await service.evaluate_metrics(request)
    assert result.status == TriageRiskStatus.CRITICAL_EMERGENCY
    assert result.emergency_mode is True
    assert any("Huyết áp rất cao" in factor for factor in result.risk_factors)


@pytest.mark.asyncio
async def test_critical_high_fever():
    service = MetricsScreeningService()
    request = HealthMetricsLogRequest(
        stage=MaternalStage.PREGNANCY,
        gestational_age_weeks=24,
        temperature=39.2,
        systolic_bp=120,
        diastolic_bp=80,
    )
    result = await service.evaluate_metrics(request)
    assert result.status == TriageRiskStatus.CRITICAL_EMERGENCY
    assert result.emergency_mode is True
    assert any("Sốt cao" in factor for factor in result.risk_factors)


@pytest.mark.asyncio
async def test_critical_fetal_kick_loss():
    service = MetricsScreeningService()
    request = HealthMetricsLogRequest(
        stage=MaternalStage.PREGNANCY,
        gestational_age_weeks=34,
        fetal_movements_count=0,
        fetal_movements_duration_hours=2,
    )
    result = await service.evaluate_metrics(request)
    assert result.status == TriageRiskStatus.CRITICAL_EMERGENCY
    assert result.emergency_mode is True
    assert any("Thai không cử động" in factor for factor in result.risk_factors)


@pytest.mark.asyncio
async def test_mild_anomaly_monitoring():
    service = MetricsScreeningService()
    request = HealthMetricsLogRequest(
        stage=MaternalStage.PREGNANCY,
        gestational_age_weeks=14,
        systolic_bp=135,
        diastolic_bp=88,
        symptoms=["Buồn nôn", "Đau lưng"],
    )
    result = await service.evaluate_metrics(request)
    assert result.status == TriageRiskStatus.ANOMALY_MONITOR
    assert result.emergency_mode is False
    assert len(result.risk_factors) > 0
