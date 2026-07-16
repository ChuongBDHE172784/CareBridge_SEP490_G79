import pytest

from app import evidence_registry_client, official_source_searcher, source_retriever
from app.evidence_registry_client import ApprovedEvidenceSource


@pytest.fixture(autouse=True)
def approved_evidence_registry(monkeypatch):
    """Unit tests emulate Spring's DB registry; production has no static allowlist."""
    sources = (
        ApprovedEvidenceSource("who", "who.int", "https://www.who.int", "WHO", ("PRECONCEPTION", "PREGNANCY", "INFANT", "TODDLER")),
        ApprovedEvidenceSource("moh", "moh.gov.vn", "https://moh.gov.vn", "Bo Y te Viet Nam", ("PRECONCEPTION", "PREGNANCY", "INFANT", "TODDLER")),
        ApprovedEvidenceSource("mch", "mch.moh.gov.vn", "https://mch.moh.gov.vn", "Bo Y te Viet Nam", ("PRECONCEPTION", "PREGNANCY", "INFANT", "TODDLER")),
        ApprovedEvidenceSource("cdc", "cdc.gov", "https://www.cdc.gov", "CDC", ("PRECONCEPTION", "PREGNANCY", "INFANT", "TODDLER")),
        ApprovedEvidenceSource("unicef", "unicef.org", "https://www.unicef.org", "UNICEF", ("PRECONCEPTION", "PREGNANCY", "INFANT", "TODDLER")),
        ApprovedEvidenceSource("nhitw", "benhviennhitrunguong.gov.vn", "https://benhviennhitrunguong.gov.vn", "Benh vien Nhi Trung uong", ("INFANT", "TODDLER")),
        ApprovedEvidenceSource("nd1", "nhidong.org.vn", "https://nhidong.org.vn", "Benh vien Nhi Dong", ("INFANT", "TODDLER")),
        ApprovedEvidenceSource("ndtp", "bvndtp.org.vn", "https://bvndtp.org.vn", "Benh vien Nhi Dong Thanh pho", ("INFANT", "TODDLER")),
    )

    def approved(stage):
        return tuple(source for source in sources if stage.upper() in source.applicable_stages)

    monkeypatch.setattr(evidence_registry_client, "approved_sources_for_stage", approved)
    monkeypatch.setattr(source_retriever, "approved_sources_for_stage", approved)
    monkeypatch.setattr(official_source_searcher, "approved_sources_for_stage", approved)
