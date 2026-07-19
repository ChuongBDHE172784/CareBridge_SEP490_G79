import json
from pathlib import Path

from carebridge_evaluation.models import JourneyPhase

MAPPING = Path(__file__).parents[1] / "datasets" / "care_journey_mapping.json"


def test_care_journey_mapping_is_complete_and_machine_readable():
    payload = json.loads(MAPPING.read_text(encoding="utf-8"))
    journeys = payload["journeys"]

    assert {item["journeyPhase"] for item in journeys} == {phase.value for phase in JourneyPhase}
    for item in journeys:
        assert "runtimeStage" in item
        assert isinstance(item["supported"], bool)
        assert item["evidenceStatus"]
        assert item["clinicalReviewStatus"]
        assert isinstance(item["knownGaps"], list)
        assert item["targetUsers"]
        assert set(item["topics"]) == {"nutrition", "exercise", "weight", "growth", "vaccination", "emergency"}

