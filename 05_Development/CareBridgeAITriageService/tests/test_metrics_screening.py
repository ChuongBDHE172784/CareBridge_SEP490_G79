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


@pytest.mark.asyncio
async def test_critical_postpartum_fever_sepsis():
    service = MetricsScreeningService()
    request = HealthMetricsLogRequest(
        stage=MaternalStage.POSTPARTUM,
        temperature=38.2,
    )
    result = await service.evaluate_metrics(request)
    assert result.status == TriageRiskStatus.CRITICAL_EMERGENCY
    assert result.emergency_mode is True
    assert any("Nhiễm trùng hậu sản" in factor for factor in result.risk_factors)


@pytest.mark.asyncio
async def test_mild_pregnancy_fever_anomaly():
    service = MetricsScreeningService()
    request = HealthMetricsLogRequest(
        stage=MaternalStage.PREGNANCY,
        gestational_age_weeks=22,
        temperature=37.8,
    )
    result = await service.evaluate_metrics(request)
    assert result.status == TriageRiskStatus.ANOMALY_MONITOR
    assert result.emergency_mode is False
    assert any("Sốt nhẹ" in factor for factor in result.risk_factors)


@pytest.mark.asyncio
async def test_pregnancy_fever_with_rupture_of_membranes():
    service = MetricsScreeningService()
    request = HealthMetricsLogRequest(
        stage=MaternalStage.PREGNANCY,
        gestational_age_weeks=30,
        temperature=38.1,
        symptoms=["Rỉ ối / Vỡ ối"],
    )
    result = await service.evaluate_metrics(request)
    assert result.status == TriageRiskStatus.CRITICAL_EMERGENCY
    assert result.emergency_mode is True
    assert any("Nhiễm trùng ối" in factor for factor in result.risk_factors)


@pytest.mark.asyncio
async def test_normal_blood_glucose_in_mg_dl():
    service = MetricsScreeningService()
    request = HealthMetricsLogRequest(
        stage=MaternalStage.PREGNANCY,
        gestational_age_weeks=20,
        blood_glucose=70.0,  # 70 mg/dL ~ 3.89 mmol/L -> Normal fasting
        is_fasting_glucose=True,
    )
    result = await service.evaluate_metrics(request)
    assert result.status == TriageRiskStatus.NORMAL
    assert len(result.risk_factors) == 0


@pytest.mark.asyncio
async def test_elevated_fasting_blood_glucose_in_mg_dl():
    service = MetricsScreeningService()
    request = HealthMetricsLogRequest(
        stage=MaternalStage.PREGNANCY,
        gestational_age_weeks=20,
        blood_glucose=105.0,  # 105 mg/dL ~ 5.83 mmol/L -> >= 5.1 mmol/L (92 mg/dL) Fasting Elevated
        is_fasting_glucose=True,
    )
    result = await service.evaluate_metrics(request)
    assert result.status == TriageRiskStatus.ANOMALY_MONITOR
    assert any("Đường huyết lúc đói" in factor for factor in result.risk_factors)


@pytest.mark.asyncio
async def test_epds_normal_score():
    service = MetricsScreeningService()
    request = HealthMetricsLogRequest(
        stage=MaternalStage.PREGNANCY,
        epds_score=6,
        epds_question_10_score=0,
    )
    result = await service.evaluate_metrics(request)
    assert result.status == TriageRiskStatus.NORMAL
    assert result.emergency_mode is False
    assert len(result.risk_factors) == 0


@pytest.mark.asyncio
async def test_epds_moderate_risk_score():
    service = MetricsScreeningService()
    request = HealthMetricsLogRequest(
        stage=MaternalStage.PREGNANCY,
        epds_score=11,
        epds_question_10_score=0,
    )
    result = await service.evaluate_metrics(request)
    assert result.status == TriageRiskStatus.ANOMALY_MONITOR
    assert result.emergency_mode is False
    assert any("EPDS ở mức rủi ro" in factor for factor in result.risk_factors)


@pytest.mark.asyncio
async def test_epds_high_risk_score():
    service = MetricsScreeningService()
    request = HealthMetricsLogRequest(
        stage=MaternalStage.POSTPARTUM,
        epds_score=16,
        epds_question_10_score=0,
    )
    result = await service.evaluate_metrics(request)
    assert result.status == TriageRiskStatus.ANOMALY_MONITOR
    assert result.emergency_mode is False
    assert any("EPDS rất cao" in factor for factor in result.risk_factors)


@pytest.mark.asyncio
async def test_epds_question_10_red_flag_emergency():
    service = MetricsScreeningService()
    request = HealthMetricsLogRequest(
        stage=MaternalStage.PREGNANCY,
        epds_score=4,  # Tổng điểm thấp nhưng câu 10 có điểm
        epds_question_10_score=1,
    )
    result = await service.evaluate_metrics(request)
    assert result.status == TriageRiskStatus.CRITICAL_EMERGENCY
    assert result.emergency_mode is True
    assert any("Câu hỏi số 10 EPDS" in factor for factor in result.risk_factors)


@pytest.mark.asyncio
async def test_multi_context_glucose_post_meal_1h_and_2h():
    service = MetricsScreeningService()
    # Post-meal 1h: 8.2 mmol/L (Elevated >= 7.8 mmol/L)
    req1 = HealthMetricsLogRequest(
        stage=MaternalStage.PREGNANCY,
        blood_glucose=8.2,
        glucose_context="POST_MEAL_1H",
    )
    res1 = await service.evaluate_metrics(req1)
    assert res1.status == TriageRiskStatus.ANOMALY_MONITOR
    assert any("sau ăn 1 giờ tăng" in factor for factor in res1.risk_factors)

    # Post-meal 2h: 6.0 mmol/L (Normal < 6.7 mmol/L)
    req2 = HealthMetricsLogRequest(
        stage=MaternalStage.PREGNANCY,
        blood_glucose=6.0,
        glucose_context="POST_MEAL_2H",
    )
    res2 = await service.evaluate_metrics(req2)
    assert res2.status == TriageRiskStatus.NORMAL


@pytest.mark.asyncio
async def test_glucose_sanity_check_implausible_mmol_val():
    service = MetricsScreeningService()
    # 300 entered as mmol/L (> 35.0 mmol/L threshold)
    # The system catches this impossible physiological value and alerts user
    req = HealthMetricsLogRequest(
        stage=MaternalStage.PREGNANCY,
        blood_glucose=300.0,
    )
    # Note: 300 is auto-detected as mg/dL (300 mg/dL ~ 16.65 mmol/L)
    res = await service.evaluate_metrics(req)
    assert res.status == TriageRiskStatus.ANOMALY_MONITOR
    assert any("300.0 mg/dL (~16.65 mmol/L)" in factor or "300" in factor for factor in res.risk_factors)


@pytest.mark.asyncio
async def test_blood_pressure_invalid_sbp_less_than_dbp():
    service = MetricsScreeningService()
    req = HealthMetricsLogRequest(
        stage=MaternalStage.PREGNANCY,
        systolic_bp=90,
        diastolic_bp=110,
    )
    res = await service.evaluate_metrics(req)
    assert res.status == TriageRiskStatus.ANOMALY_MONITOR
    assert any("Huyết áp tâm thu phải lớn hơn tâm trương" in factor for factor in res.risk_factors)


@pytest.mark.asyncio
async def test_fetal_movement_gestational_age_early_weeks_and_preconception():
    service = MetricsScreeningService()
    # Preconception: Should not flag emergency for fetal kicks
    req_pre = HealthMetricsLogRequest(
        stage=MaternalStage.PRECONCEPTION,
        fetal_movements_count=0,
    )
    res_pre = await service.evaluate_metrics(req_pre)
    assert res_pre.status == TriageRiskStatus.NORMAL

    # Pregnancy early week 16 (< 24 weeks): Should inform gently without false emergency
    req_early = HealthMetricsLogRequest(
        stage=MaternalStage.PREGNANCY,
        gestational_age_weeks=16,
        fetal_movements_count=0,
    )
    res_early = await service.evaluate_metrics(req_early)
    assert res_early.status == TriageRiskStatus.NORMAL


@pytest.mark.asyncio
async def test_stage_based_bmi_preconception_vs_pregnancy():
    service = MetricsScreeningService()
    # Preconception overweight (BMI = 23.5 kg/m², Asian normal is 18.5 - 22.9)
    req_pre = HealthMetricsLogRequest(
        stage=MaternalStage.PRECONCEPTION,
        bmi=23.5,
    )
    res_pre = await service.evaluate_metrics(req_pre)
    assert res_pre.status == TriageRiskStatus.ANOMALY_MONITOR
    assert any("trước mang thai" in factor for factor in res_pre.risk_factors)


@pytest.mark.asyncio
async def test_stage_based_water_intake():
    service = MetricsScreeningService()
    # Postpartum lactation needs 2500-3000ml. If only 1600ml entered, flag anomaly
    req_post = HealthMetricsLogRequest(
        stage=MaternalStage.POSTPARTUM,
        water_intake_ml=1600,
    )
    res_post = await service.evaluate_metrics(req_post)
    assert res_post.status == TriageRiskStatus.ANOMALY_MONITOR
    assert any("sau sinh chưa đủ" in factor for factor in res_post.risk_factors)


