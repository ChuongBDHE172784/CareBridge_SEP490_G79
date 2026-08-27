from carebridge_evaluation.adapters.local import LocalAdapter
from carebridge_evaluation.catalog import load_official_catalog
from carebridge_evaluation.config import EvaluationSettings
from carebridge_evaluation.models import ExecutionStatus


def test_local_adapter_executes_canonical_pediatric_red_vector():
    case = next(case for case in load_official_catalog().cases if case.id == "PEDIATRIC_RED_SEIZURE")

    result = LocalAdapter(EvaluationSettings.from_env().ai_service_path).execute(case, "run-1")

    assert result.executionStatus == ExecutionStatus.PASSED
    assert result.actualRisk == "RED"
    assert result.passed is True
