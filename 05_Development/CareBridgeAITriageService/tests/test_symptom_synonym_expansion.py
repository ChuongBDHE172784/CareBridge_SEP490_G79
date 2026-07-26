# TriageSymptomSynonymExpansion — CB-TRIAGE-IMP-005 / CB-TRIAGE-IMP-005-TEST.
# Oracle: TDS §5.3 Synonym Additions Table, rows S2, S4–S13 only
# (S1 bulging_fontanelle DEFERRED per ADR-TSSE-001; S3 documented no-op — TSSE-TC-09).
# SYNTHETIC test data only. CASE 2.0 Props Isolation: fresh request per test via factory.
import json
from pathlib import Path

import pytest

from app.schemas import ChildTriageRequest
from app.symptom_normalizer import (
    ONTOLOGY,
    normalize_symptom_details_deterministic,
    normalize_symptoms,
)

# FX-SYN-01 — oracle rows copied verbatim from TDS CB-TRIAGE-IMP-005 §5.3 (S2, S4–S13)
SYNONYM_ROWS = [
    ("trớ sữa", "tro sua", "vomiting"),
    ("hâm hấp", "ham hap", "fever"),
    ("lừ đừ", "lu du", "lethargy"),
    ("sụt sịt", "sut sit", "runny_nose"),
    ("khò khè", "kho khe", "difficulty_breathing"),
    ("thở rít", "tho rit", "difficulty_breathing"),
    ("biếng ăn", "bieng an", "poor_feeding"),
    ("ọc sữa", "oc sua", "vomiting"),
    ("đi ngoài", "di ngoai", "diarrhea"),
    ("ỉa chảy", "ia chay", "diarrhea"),
    ("rôm sảy", "rom say", "rash"),
]

# FX-SYN-03 — golden list, one representative existing keyword per current canonical code,
# copied verbatim from the baseline ONTOLOGY (19 codes incl. the 2 Python-only codes — L1).
GOLDEN_EXISTING_ROWS = [
    ("sot", "fever"),
    ("sot cao", "high_fever"),
    ("ho", "cough"),
    ("so mui", "runny_nose"),
    ("kho tho", "difficulty_breathing"),
    ("rut lom", "chest_indrawing"),
    ("tim tai", "cyanosis"),
    ("co giat", "seizure"),
    ("li bi", "lethargy"),
    ("kho danh thuc", "difficult_to_wake"),
    ("khong uong", "unable_to_drink"),
    ("bo bu", "poor_feeding"),
    ("non", "vomiting"),
    ("non lien tuc", "persistent_vomiting"),
    ("tieu chay", "diarrhea"),
    ("moi kho", "mild_dehydration"),
    ("mat nuoc nang", "severe_dehydration"),
    ("phat ban", "rash"),
    ("nang hon", "worsening_condition"),
]


def make_free_text_request(parent_free_text: str) -> ChildTriageRequest:
    # FX-SYN-04 — minimal valid intake, ONLY free text set
    return ChildTriageRequest(stage="INFANT", childAgeMonths=8, parentFreeText=parent_free_text)


# ---------------------------------------------------------------------------
# TSSE-TC-06 — each new synonym maps to its canonical code (3 case forms)
# ---------------------------------------------------------------------------

@pytest.mark.parametrize("diacritic,stripped,canonical", SYNONYM_ROWS)
def test_tc06_accent_stripped_form_maps_to_canonical(diacritic, stripped, canonical):
    codes = normalize_symptoms(make_free_text_request(stripped))
    assert canonical in codes, f"TSSE-TC-06 stripped '{stripped}' -> {canonical}"


@pytest.mark.parametrize("diacritic,stripped,canonical", SYNONYM_ROWS)
def test_tc06_diacritic_form_maps_to_canonical(diacritic, stripped, canonical):
    codes = normalize_symptoms(make_free_text_request(diacritic))
    assert canonical in codes, f"TSSE-TC-06 diacritic '{diacritic}' -> {canonical}"


@pytest.mark.parametrize("diacritic,stripped,canonical", SYNONYM_ROWS)
def test_tc06_mixed_case_form_maps_to_canonical(diacritic, stripped, canonical):
    mixed = diacritic.upper()
    codes = normalize_symptoms(make_free_text_request(mixed))
    assert canonical in codes, f"TSSE-TC-06 mixed-case '{mixed}' -> {canonical}"


def test_tc06_representative_row_uses_deterministic_keyword_method():
    # Representative row S2 embedded in a sentence -> KEYWORD (not EXACT, not Gemini).
    details = normalize_symptom_details_deterministic(
        make_free_text_request("bé bị trớ sữa nhiều")
    )
    vomiting = [item for item in details if item.normalizedCode == "vomiting"]
    assert vomiting, "TSSE-TC-06 'trớ sữa' in sentence must normalize to vomiting"
    assert vomiting[0].normalizationMethod == "KEYWORD"


# ---------------------------------------------------------------------------
# TSSE-TC-07 — regression on existing ONTOLOGY codes + negation guard on new terms
# ---------------------------------------------------------------------------

@pytest.mark.parametrize("keyword,canonical", GOLDEN_EXISTING_ROWS)
def test_tc07_existing_ontology_baseline_intact(keyword, canonical):
    # Regression guard sub-case — expected PASS at red phase (Test-Spec §5.1).
    codes = normalize_symptoms(make_free_text_request(keyword))
    assert canonical in codes, f"TSSE-TC-07 golden '{keyword}' -> {canonical}"


def test_tc07_negation_guard_applies_to_new_term():
    # Red-Gate-eligible sub-case: term itself is new.
    negated = normalize_symptoms(make_free_text_request("khong tro sua"))
    assert "vomiting" not in negated, "TSSE-TC-07 negated 'khong tro sua' must NOT map"
    affirmed = normalize_symptoms(make_free_text_request("be tro sua"))
    assert "vomiting" in affirmed, "TSSE-TC-07 'be tro sua' must map to vomiting"


# ---------------------------------------------------------------------------
# TSSE-TC-08 (Python side) — parity vectors shared with the Java backend
# (pattern copied from tests/test_pediatric_red_parity.py)
# ---------------------------------------------------------------------------

def test_tc08_all_synonym_parity_vectors_match_shared_contract():
    vectors = json.loads(
        (Path(__file__).parent / "data" / "symptom_synonym_parity_vectors.json")
        .read_text(encoding="utf-8")
    )
    assert len(vectors) == 11, "one vector per implemented TDS §5.3 row (S2, S4–S13)"
    for vector in vectors:
        codes = normalize_symptoms(make_free_text_request(vector["parentFreeText"]))
        missing = [code for code in vector["expectedCodes"] if code not in codes]
        assert not missing, f"TSSE-TC-08 vector '{vector['parentFreeText']}' missing {missing}"


# ---------------------------------------------------------------------------
# TSSE-TC-09 — S3 "sốt sình sịch" already normalizes to fever (documented no-op /
# regression guard; expected PASS from birth, excluded from Red Gate — Logic Issue L3)
# ---------------------------------------------------------------------------

@pytest.mark.parametrize("text", ["sốt sình sịch", "sot sinh sich"])
def test_tc09_sot_sinh_sich_already_normalizes_to_fever(text):
    codes = normalize_symptoms(make_free_text_request(text))
    assert "fever" in codes, "TSSE-TC-09 pre-existing 'sot' keyword regression"


# ---------------------------------------------------------------------------
# BR-SAFETY guard — S1 stays deferred: no new canonical code may appear (ADR-TSSE-001 / C3)
# ---------------------------------------------------------------------------

def test_no_new_canonical_code_introduced():
    assert "bulging_fontanelle" not in ONTOLOGY, "S1 is DEFERRED — clinical review required"
