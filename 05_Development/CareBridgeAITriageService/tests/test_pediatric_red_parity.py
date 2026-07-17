import json
from pathlib import Path

from app.risk_rules import apply_red_flag_rules
from app.schemas import ChildTriageRequest


def test_all_pediatric_red_vectors_match_shared_contract():
    vectors = json.loads((Path(__file__).parent / "data" / "pediatric_red_parity_vectors.json").read_text(encoding="utf-8"))
    for vector in vectors:
        intake = ChildTriageRequest(
            stage="INFANT",
            childAgeMonths=vector.get("childAgeMonths", 12),
            temperatureC=vector.get("temperatureC"),
        )
        _, matched_rules = apply_red_flag_rules(intake, vector["symptoms"])
        assert vector["expectedRule"] in matched_rules, vector["name"]
