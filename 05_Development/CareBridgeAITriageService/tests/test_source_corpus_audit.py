from __future__ import annotations

import importlib.util
from pathlib import Path


TOOL = Path(__file__).resolve().parents[2] / "DevTools" / "audit_triage_source_corpus.py"


def _module():
    spec = importlib.util.spec_from_file_location("source_audit", TOOL)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_current_corpus_reports_all_documents_verified():
    report = _module().audit_corpus()

    assert report["summary"] == {"documents": 13, "passed": 13, "failed": 0}
    unverified = [doc for doc in report["documents"] if not doc["passed"]]
    assert len(unverified) == 0


def test_publisher_name_alone_never_passes_verification(tmp_path):
    (tmp_path / "trusted-name.md").write_text(
        """---
id: TRUSTED_NAME
title: Example
organization: World Health Organization
publisherTrustStatus: IDENTIFIED_NOT_VALIDATED
url: https://www.who.int/example
domain: who.int
applicableStages: [PREGNANCY]
sourceStatus: APPROVED
---
Body
""",
        encoding="utf-8",
    )

    document = _module().audit_corpus(tmp_path)["documents"][0]

    assert document["passed"] is False
    assert "PUBLISHER_IDENTITY_IS_NOT_DOCUMENT_VERIFICATION" in document["reasons"]


def test_forged_target_stage_and_rule_mappings_are_rejected(tmp_path):
    (tmp_path / "forged.md").write_text(
        """---
id: FORGED
title: Example
organization: Example
publisherTrustStatus: DOMAIN_AND_DOCUMENT_VERIFIED
url: https://example.org/guidance
domain: example.org
sectionOrPage: 1
contentHash: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
targetEntities: [BABY]
applicableStages: [NOT_A_STAGE]
ruleIds: [NOT_A_RULE]
sourceStatus: SOURCE_VERIFIED
---
Body
""",
        encoding="utf-8",
    )

    reasons = _module().audit_corpus(tmp_path)["documents"][0]["reasons"]

    assert "INVALID_STAGE_MAPPING" in reasons
    assert "INVALID_RULE_MAPPING" in reasons
