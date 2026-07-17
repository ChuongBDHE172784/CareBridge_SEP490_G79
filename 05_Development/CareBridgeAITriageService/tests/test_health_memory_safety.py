import inspect

from app.graph import run_triage
from app.intake_question_engine import determine_missing_information
from app.risk_rules import apply_red_flag_rules, score_risk
from app.schemas import ChildTriageRequest


def test_prior_green_memory_cannot_lower_current_red_risk():
    # Health-memory summaries are deliberately kept outside the deterministic intake.
    prior_green_memory = [{"summaryText": "Previous triage was GREEN", "relatedStage": "INFANT"}]
    current_intake = ChildTriageRequest(
        stage="INFANT",
        childAgeMonths=8,
        symptomList=["kho tho", "tim tai"],
        breathingStatus="kho tho, tim tai",
        consciousnessStatus="tinh tao",
        feedingStatus="bu uong tot",
        seizure=False,
    )

    response = run_triage(current_intake)

    assert prior_green_memory  # Documents the deliberately non-rule-engine context.
    assert response.riskLevel == "RED"
    assert response.emergencyActionRequired is True


def test_prior_temperature_memory_cannot_skip_current_temperature_question():
    prior_memory = [{"summaryText": "Temperature last week: 36.8C", "relatedStage": "INFANT"}]
    current_intake = ChildTriageRequest(
        stage="INFANT",
        childAgeMonths=8,
        symptomList=["sot"],
        parentFreeText="be sot hom nay",
        temperatureC=None,
        breathingStatus="tho binh thuong",
        consciousnessStatus="tinh tao",
        feedingStatus="bu uong tot",
        seizure=False,
    )

    missing = determine_missing_information(current_intake)

    assert prior_memory
    assert "temperatureC" in missing


def test_deterministic_rule_functions_do_not_accept_health_memory():
    red_flag_parameters = set(inspect.signature(apply_red_flag_rules).parameters)
    score_parameters = set(inspect.signature(score_risk).parameters)

    assert "health_memory" not in red_flag_parameters
    assert "healthMemoryEntries" not in red_flag_parameters
    assert "health_memory" not in score_parameters
    assert "healthMemoryEntries" not in score_parameters
