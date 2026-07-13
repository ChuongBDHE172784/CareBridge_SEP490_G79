from app.schemas import SourceDocument
from app.source_validator import validate_source, validate_source_domain


def test_source_outside_whitelist_rejected():
    source = source_doc(url="https://example.com/child-fever", domain="example.com")
    assert validate_source_domain(source) is False


def test_source_missing_title_or_organization_rejected():
    source = source_doc(title="", organization="WHO")
    assert validate_source(source, ["fever"]) is False


def test_irrelevant_source_rejected():
    source = source_doc(
        title="WHO adult dental care",
        body="Official source about adult dental care.",
        symptoms=["dental_pain"],
        topic="dental",
    )
    assert validate_source(source, ["fever"]) is False


def test_whitelisted_relevant_source_allowed():
    source = source_doc()
    assert validate_source(source, ["fever"]) is True


def source_doc(
    *,
    url: str = "https://www.who.int/publications/i/item/978-92-4-154837-3",
    domain: str = "who.int",
    title: str = "WHO child fever",
    organization: str = "WHO",
    symptoms: list[str] | None = None,
    topic: str = "fever",
    body: str = "Official child fever source.",
) -> SourceDocument:
    return SourceDocument(
        id="TEST_SOURCE",
        title=title,
        organization=organization,
        url=url,
        domain=domain,
        lastReviewed="2026-07-10",
        topic=topic,
        ageRange="child",
        riskLevels=["YELLOW"],
        symptoms=symptoms or ["fever"],
        sourceType="official_guideline",
        sourceStatus="REVIEWED",
        body=body,
    )
