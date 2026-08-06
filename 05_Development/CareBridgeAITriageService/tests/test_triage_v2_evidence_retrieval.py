from __future__ import annotations

from hashlib import sha256
from pathlib import Path
import inspect

from app.triage_v2.evidence_retrieval import (
    bm25_rank, corpus_inventory, load_verified_corpus, retrieve_verified_evidence,
)


def write_source(directory: Path, *, source_id="SRC_1", status="SOURCE_VERIFIED",
                 body="Co giật cần được đánh giá khẩn cấp.", declared_hash=None):
    content_hash = declared_hash or sha256(body.encode("utf-8")).hexdigest()
    text = f"""---
id: {source_id}
title: Hướng dẫn dấu hiệu cảnh báo
organization: Public Health Publisher
publisher: Public Health Publisher
url: https://health.example/guidance/seizure
domain: health.example
sectionOrPage: Section 2
contentHash: {content_hash}
sourceStatus: {status}
language: vi
targetEntities: [MOTHER]
applicableStages: [PREGNANCY]
ruleIds: [GLOBAL_RED_001]
---
{body}
"""
    (directory / f"{source_id}.md").write_text(text, encoding="utf-8")


def write_source_with_metadata(
    directory: Path, *, source_id: str, organization: str, domain: str
):
    write_source(directory, source_id=source_id)
    path = directory / f"{source_id}.md"
    text = path.read_text(encoding="utf-8")
    text = text.replace("organization: Public Health Publisher", f"organization: {organization}")
    text = text.replace("publisher: Public Health Publisher", f"publisher: {organization}")
    text = text.replace("https://health.example/", f"https://{domain}/")
    text = text.replace("domain: health.example", f"domain: {domain}")
    path.write_text(text, encoding="utf-8")


def state():
    return {
        "triageOutcome": "RED", "targetEntity": "MOTHER", "stage": "PREGNANCY",
        "decisiveRuleIds": ["GLOBAL_RED_001"], "reasonCodes": ["GLOBAL_DANGER"],
        "signals": {"SEIZURE": {"presence": "PRESENT"}},
    }


def test_only_allowlisted_hash_verified_specific_sources_load(tmp_path):
    write_source(tmp_path)
    write_source(tmp_path, source_id="PENDING", status="PENDING")
    write_source(tmp_path, source_id="BROKEN", declared_hash="0" * 64)
    documents = load_verified_corpus(
        corpus_dir=tmp_path, allowed_domains={"health.example"}, target="MOTHER",
        stage="PREGNANCY", rule_ids=("GLOBAL_RED_001",),
    )
    assert [document.source_id for document in documents] == ["SRC_1"]
    assert load_verified_corpus(
        corpus_dir=tmp_path, allowed_domains={"other.example"}, target="MOTHER",
        stage="PREGNANCY",
    ) == []


def test_pending_changed_broken_or_legacy_approved_never_becomes_citation(tmp_path):
    for index, status in enumerate(("PENDING", "SOURCE_CHANGED", "BROKEN", "APPROVED")):
        write_source(tmp_path, source_id=f"SRC_{index}", status=status)
    assert retrieve_verified_evidence(
        state(), allowed_domains={"health.example"}, corpus_dir=tmp_path
    ) == []


def test_post_outcome_bm25_returns_only_verified_metadata(tmp_path):
    write_source(tmp_path)
    citations = retrieve_verified_evidence(
        state(), allowed_domains={"health.example"}, corpus_dir=tmp_path
    )
    assert len(citations) == 1
    assert citations[0]["sourceStatus"] == "SOURCE_VERIFIED"
    assert citations[0]["retrievalMode"] == "LOCAL_BM25"
    assert citations[0]["contentHash"] == sha256(
        "Co giật cần được đánh giá khẩn cấp.".encode("utf-8")
    ).hexdigest()


def test_decisive_rule_requires_explicit_document_rule_mapping(tmp_path):
    write_source(tmp_path)
    path = tmp_path / "SRC_1.md"
    path.write_text(
        path.read_text(encoding="utf-8").replace(
            "ruleIds: [GLOBAL_RED_001]", "ruleIds: []"
        ),
        encoding="utf-8",
    )

    assert retrieve_verified_evidence(
        state(), allowed_domains={"health.example"}, corpus_dir=tmp_path
    ) == []


def test_retrieval_never_changes_outcome_and_failure_is_empty(tmp_path):
    original = state()
    before = dict(original)
    assert retrieve_verified_evidence(original, allowed_domains=set(), corpus_dir=tmp_path) == []
    assert original == before
    original["triageOutcome"] = "NEEDS_MORE_INFO"
    assert retrieve_verified_evidence(
        original, allowed_domains={"health.example"}, corpus_dir=tmp_path
    ) == []


def test_lexical_benchmark_ranks_relevant_document_first(tmp_path):
    write_source(tmp_path, source_id="SEIZURE")
    write_source(tmp_path, source_id="OTHER", body="Thông tin chăm sóc thông thường.")
    documents = load_verified_corpus(
        corpus_dir=tmp_path, allowed_domains={"health.example"}, target="MOTHER",
        stage="PREGNANCY",
    )
    ranked = bm25_rank(documents, "SEIZURE co giật GLOBAL_RED_001")
    assert ranked[0][0].source_id == "SEIZURE"
    assert ranked[0][1] > ranked[1][1]


def test_verified_vietnam_source_is_prioritized_and_who_is_capped_at_one(tmp_path):
    write_source_with_metadata(
        tmp_path, source_id="WHO_1", organization="World Health Organization", domain="who.int"
    )
    write_source_with_metadata(
        tmp_path, source_id="WHO_2", organization="World Health Organization", domain="who.int"
    )
    write_source_with_metadata(
        tmp_path, source_id="VN_1", organization="Bo Y te", domain="moh.gov.vn"
    )

    citations = retrieve_verified_evidence(
        state(), allowed_domains={"who.int", "moh.gov.vn"}, corpus_dir=tmp_path
    )

    assert [citation["sourceId"] for citation in citations] == ["VN_1", "WHO_1"]


def test_current_corpus_inventory_proves_pgvector_is_not_justified():
    inventory = corpus_inventory()
    assert inventory["documents"] == 13
    assert inventory["declaredSourceVerified"] == 0


def test_malformed_frontmatter_is_skipped_without_losing_valid_documents(tmp_path):
    """A single unparsable corpus file must not abort retrieval for every other source."""

    write_source(tmp_path, source_id="SRC_1")
    (tmp_path / "BROKEN.md").write_text(
        "---\nid: BROKEN\ntitle: [unclosed\n---\nbody\n", encoding="utf-8"
    )
    rejections: list[str] = []

    documents = load_verified_corpus(
        corpus_dir=tmp_path, allowed_domains={"health.example"}, target="MOTHER",
        stage="PREGNANCY", rule_ids=("GLOBAL_RED_001",), on_reject=rejections.append,
    )

    assert [document.source_id for document in documents] == ["SRC_1"]
    assert rejections == ["CITATION_REJECTED"]


def test_v2_retriever_has_no_runtime_web_search_or_agent_loop():
    import app.triage_v2.evidence_retrieval as module
    source = inspect.getsource(module)
    assert "official_source_searcher" not in source
    assert "requests." not in source
    assert "urlopen(" not in source
    assert "Agent" not in source
