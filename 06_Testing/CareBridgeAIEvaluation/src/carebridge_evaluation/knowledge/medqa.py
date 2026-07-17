"""Read external MedQA JSONL as a knowledge baseline, never as triage truth."""

from __future__ import annotations

import json
from pathlib import Path

from pydantic import BaseModel, ConfigDict, Field

from ..models import DatasetType


class MedQaItem(BaseModel):
    model_config = ConfigDict(extra="forbid")

    id: str
    question: str = Field(min_length=1)
    options: dict[str, str] = Field(min_length=2)
    answer: str = Field(min_length=1)
    datasetType: DatasetType = DatasetType.KNOWLEDGE_BASELINE
    clinicalAuthority: str = "AUXILIARY_KNOWLEDGE_BASELINE"


def load_medqa_jsonl(path: Path, *, limit: int | None = None) -> list[MedQaItem]:
    """Load an explicitly supplied MedQA-format file without downloading or relicensing it."""

    items: list[MedQaItem] = []
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        if not line.strip():
            continue
        payload = json.loads(line)
        options = payload.get("options") or {}
        answer = payload.get("answer") or payload.get("answer_idx") or ""
        item = MedQaItem.model_validate({
            "id": str(payload.get("id") or f"MEDQA_{line_number:06d}"),
            "question": payload.get("question"),
            "options": options,
            "answer": answer,
        })
        items.append(item)
        if limit is not None and len(items) >= limit:
            break
    return items

