import json
from urllib.parse import urlparse

import pytest

from app import source_retriever
from app.config import OFFICIAL_SOURCES_FILE


def _is_content_deep_link(url: str) -> bool:
    parsed = urlparse(url)
    path = parsed.path.rstrip("/").lower()
    return parsed.scheme == "https" and bool(parsed.hostname) and path not in {"", "/vi", "/en"}


def test_official_source_registry_contains_only_content_deep_links():
    entries = json.loads(OFFICIAL_SOURCES_FILE.read_text(encoding="utf-8"))

    assert len(entries) == 6
    assert all(_is_content_deep_link(entry["url"]) for entry in entries)
    assert len({entry["url"] for entry in entries}) == len(entries)


def test_runtime_catalog_contains_only_approved_age_matched_deep_links():
    sources = source_retriever.load_sources("INFANT", child_age_months=8)

    assert sources
    assert all(source.sourceStatus == "APPROVED" for source in sources)
    assert all(_is_content_deep_link(source.url) for source in sources)
    assert all(source.id != "WHO_NEWBORN_DANGER_SIGNS_001" for source in sources)


def test_newborn_source_is_not_attached_after_newborn_age_band():
    newborn_sources = source_retriever.load_sources("INFANT", child_age_months=0)
    one_month_sources = source_retriever.load_sources("INFANT", child_age_months=1)

    assert any(source.id == "WHO_NEWBORN_DANGER_SIGNS_001" for source in newborn_sources)
    assert all(source.id != "WHO_NEWBORN_DANGER_SIGNS_001" for source in one_month_sources)


def test_breathing_and_cyanosis_use_child_specific_content_page():
    sources = source_retriever.retrieve_sources(
        ["difficulty_breathing", "cyanosis"],
        ["RED_BREATHING_DISTRESS"],
        stage="INFANT",
        child_age_months=8,
    )

    assert any(source.id == "NCH_CHILD_DANGER_SIGNS_001" for source in sources)
    assert all("virus-hop-bao-ho-hap" not in source.url for source in sources)
    assert all(_is_content_deep_link(source.url) for source in sources)


@pytest.mark.parametrize(
    ("symptoms", "rules", "age_months", "expected_source_id"),
    [
        (["difficulty_breathing", "cyanosis"], ["RED_BREATHING_DISTRESS"], 8, "NCH_CHILD_DANGER_SIGNS_001"),
        (["seizure"], ["RED_SEIZURE"], 8, "NCH_CHILD_DANGER_SIGNS_001"),
        (["fever"], ["RED_INFANT_FEVER_UNDER_3_MONTHS"], 0, "WHO_NEWBORN_DANGER_SIGNS_001"),
        (["fever"], ["RED_HIGH_FEVER"], 8, "NCH_CHILD_FEVER_001"),
        (["diarrhea", "severe_dehydration"], ["RED_DIARRHEA_DEHYDRATION"], 8, "NCH_DIARRHEA_DEHYDRATION_001"),
        (["persistent_vomiting"], ["RED_PERSISTENT_VOMITING"], 8, "ND1_CHILD_VOMITING_001"),
    ],
)
def test_each_supported_clinical_topic_maps_to_specific_source(
    symptoms, rules, age_months, expected_source_id
):
    sources = source_retriever.retrieve_sources(
        symptoms,
        rules,
        stage="INFANT",
        child_age_months=age_months,
    )

    assert any(source.id == expected_source_id for source in sources)
    assert all(_is_content_deep_link(source.url) for source in sources)
