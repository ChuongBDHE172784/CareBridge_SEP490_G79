import os
from collections import Counter
from pathlib import Path

from carebridge_evaluation.catalog import EXPECTED_PARITY_SHA256, load_official_catalog, module_root, sha256
from carebridge_evaluation.models import BenchmarkCategory, ExpectedExecutionStatus, ReviewStatus


def repository_root() -> Path:
    return module_root().parents[1]


def test_official_catalog_contains_the_nine_approved_groups_and_thirty_cases():
    catalog = load_official_catalog()
    counts = Counter(case.category for case in catalog.cases)

    assert set(counts) == set(BenchmarkCategory)
    assert counts[BenchmarkCategory.PEDIATRIC_RED] == 10
    assert counts == {
        BenchmarkCategory.PEDIATRIC_RED: 10,
        BenchmarkCategory.NEED_MORE_INFO: 3,
        BenchmarkCategory.PRECONCEPTION: 2,
        BenchmarkCategory.PREGNANCY: 3,
        BenchmarkCategory.POSTPARTUM_MOTHER: 2,
        BenchmarkCategory.AGE_BAND_CHILD_CARE: 4,
        BenchmarkCategory.LEGAL_PRIVACY_SAFETY: 2,
        BenchmarkCategory.PROMPT_INJECTION: 2,
        BenchmarkCategory.RAG_CITATION: 2,
    }
    assert len(catalog.cases) == 30


def test_maternal_clinical_cases_are_pending_and_postpartum_is_known_gap():
    catalog = load_official_catalog()
    maternal = [
        case for case in catalog.cases
        if case.category in {BenchmarkCategory.PRECONCEPTION, BenchmarkCategory.PREGNANCY}
    ]
    postpartum = [case for case in catalog.cases if case.category == BenchmarkCategory.POSTPARTUM_MOTHER]

    assert maternal and all(case.reviewStatus == ReviewStatus.PENDING_MEDICAL_REVIEW for case in maternal)
    assert postpartum and all(
        case.expectedExecutionStatus == ExpectedExecutionStatus.KNOWN_SCOPE_GAP for case in postpartum
    )


def test_parity_review_status_is_not_inferred_from_passing_tests():
    parity = [case for case in load_official_catalog().cases if case.category == BenchmarkCategory.PEDIATRIC_RED]
    assert parity and all(case.reviewStatus == ReviewStatus.PENDING_MEDICAL_REVIEW for case in parity)
    assert all("reviewStatus metadata" in case.notes for case in parity)


def test_every_case_has_provenance_and_safety_metadata():
    for case in load_official_catalog().cases:
        assert case.forbiddenBehaviors
        assert case.requiredDisclaimer is True
        assert case.sourceReferences
        assert case.notes


def test_all_three_parity_vector_copies_have_the_canonical_digest():
    root = repository_root()
    ai_service = Path(os.getenv("CAREBRIDGE_AI_SERVICE_PATH", root / "05_Development" / "CareBridgeAITriageService"))
    files = [
        module_root() / "datasets" / "pediatric_red_parity_vectors.json",
        ai_service / "tests" / "data" / "pediatric_red_parity_vectors.json",
        root / "05_Development" / "CareBridgeAPI" / "src" / "test" / "resources" / "triage" / "pediatric_red_parity_vectors.json",
    ]

    available = [path for path in files if path.exists()]
    assert len(available) >= 2
    assert {sha256(path) for path in available} == {EXPECTED_PARITY_SHA256}
