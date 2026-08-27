import json

from carebridge_evaluation.knowledge import load_medqa_jsonl
from carebridge_evaluation.models import DatasetType


def test_medqa_adapter_labels_records_as_auxiliary_knowledge_not_triage(tmp_path):
    path = tmp_path / "medqa.jsonl"
    path.write_text(json.dumps({
        "id": "q1",
        "question": "Knowledge-only question",
        "options": {"A": "one", "B": "two"},
        "answer_idx": "A",
    }) + "\n", encoding="utf-8")

    items = load_medqa_jsonl(path)

    assert items[0].datasetType == DatasetType.KNOWLEDGE_BASELINE
    assert items[0].clinicalAuthority == "AUXILIARY_KNOWLEDGE_BASELINE"

