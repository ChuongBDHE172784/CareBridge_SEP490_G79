import frontmatter

from app import source_retriever
from app import graph as graph_module
from app import official_source_searcher
from app.evidence_cache import cache_pending_source
from app.graph import run_triage
from app.schemas import ChildTriageRequest, SourceDocument


def make_request(**overrides):
    data = {
        "childAgeMonths": 18,
        "symptomList": [],
        "duration": "1 ngay",
        "temperatureC": None,
        "feedingStatus": "bu uong tot",
        "breathingStatus": "tho binh thuong",
        "consciousnessStatus": "tinh tao",
        "vomiting": None,
        "diarrhea": None,
        "rash": None,
        "seizure": False,
        "dehydrationSigns": [],
        "parentFreeText": "",
    }
    data.update(overrides)
    return ChildTriageRequest(**data)


def test_mild_fever_feeding_well_green():
    response = run_triage(make_request(symptomList=["sot nhe"], temperatureC=37.8))
    assert response.riskLevel == "GREEN"
    assert response.emergencyActionRequired is False


def test_normal_playing_text_does_not_trigger_vomiting():
    response = run_triage(
        make_request(
            symptomList=["sot nhe"],
            temperatureC=37.8,
            parentFreeText="tre van choi binh thuong",
        )
    )
    assert response.riskLevel == "GREEN"
    assert "YELLOW_LIMITED_VOMITING" not in response.matchedRules


def test_fever_cough_normal_breathing_yellow_has_official_citation():
    response = run_triage(make_request(symptomList=["sot", "ho"], temperatureC=38.2))
    assert response.riskLevel == "YELLOW"
    assert "YELLOW_RESPIRATORY_NO_DISTRESS" in response.matchedRules
    assert response.citations
    assert any(c.source in {"WHO", "Bo Y te Viet Nam"} for c in response.citations)
    assert response.evidence.basis == "RULE_ENGINE_AND_OFFICIAL_SOURCES"
    assert "fever" in response.evidence.matchedSymptoms


def test_breathing_difficulty_red_has_who_or_moh_citation():
    response = run_triage(make_request(symptomList=["kho tho"], breathingStatus="kho tho"))
    assert response.riskLevel == "RED"
    assert response.emergencyActionRequired is True
    assert "RED_BREATHING_DISTRESS" in response.matchedRules
    assert any(c.source in {"WHO", "Bo Y te Viet Nam"} for c in response.citations)
    assert any("breathing_difficulty" in c.matchedSymptoms for c in response.citations)


def test_seizure_red_has_official_citation():
    response = run_triage(make_request(symptomList=["co giat"], seizure=True))
    assert response.riskLevel == "RED"
    assert "RED_SEIZURE" in response.matchedRules
    assert any(c.source == "WHO" for c in response.citations)


def test_lethargy_red():
    response = run_triage(
        make_request(symptomList=["li bi"], consciousnessStatus="li bi kho danh thuc")
    )
    assert response.riskLevel == "RED"
    assert "RED_LETHARGY" in response.matchedRules


def test_diarrhea_dehydration_red():
    response = run_triage(
        make_request(
            symptomList=["tieu chay"],
            diarrhea="tieu chay nhieu",
            dehydrationSigns=["moi kho", "tieu it"],
        )
    )
    assert response.riskLevel == "RED"
    assert "RED_DIARRHEA_DEHYDRATION" in response.matchedRules


def test_missing_child_age_need_more_info():
    response = run_triage(make_request(childAgeMonths=None))
    assert response.riskLevel == "NEED_MORE_INFO"
    assert response.riskColor == "#9CA3AF"
    assert response.questions


def test_no_source_does_not_fabricate_url(monkeypatch, tmp_path):
    monkeypatch.setattr(source_retriever, "MEDICAL_SOURCES_DIR", tmp_path)
    monkeypatch.setattr(graph_module, "retrieve_realtime_sources", lambda symptoms, rules: [])
    monkeypatch.setattr(graph_module, "cache_pending_sources", lambda sources: [])
    response = run_triage(make_request(symptomList=["kho tho"], breathingStatus="kho tho"))
    assert response.riskLevel == "RED"
    assert response.citations == []
    assert response.evidence.matchedOfficialSources == []
    assert response.warning is not None
    assert "realtime official search" in response.warning


def test_non_whitelisted_source_is_filtered(monkeypatch, tmp_path):
    post = frontmatter.Post(
        "Unofficial content",
        id="BAD_SOURCE_001",
        title="Bad source",
        organization="Blog",
        url="https://example.com/child-health",
        domain="example.com",
        topic="danger_signs",
        ageRange="child",
        riskLevels=["RED"],
        symptoms=["seizure"],
        sourceType="blog",
    )
    (tmp_path / "bad.md").write_text(frontmatter.dumps(post), encoding="utf-8")
    monkeypatch.setattr(source_retriever, "MEDICAL_SOURCES_DIR", tmp_path)
    monkeypatch.setattr(graph_module, "retrieve_realtime_sources", lambda symptoms, rules: [])
    monkeypatch.setattr(graph_module, "cache_pending_sources", lambda sources: [])

    assert source_retriever.load_sources() == []
    response = run_triage(make_request(symptomList=["co giat"], seizure=True))
    assert response.citations == []


def test_attach_citations_filters_non_whitelisted_source():
    source = SourceDocument(
        id="BAD_SOURCE_001",
        title="Bad source",
        organization="Blog",
        url="https://example.com/child-health",
        domain="example.com",
        lastReviewed="2026-07-10",
        topic="danger_signs",
        ageRange="child",
        riskLevels=["RED"],
        symptoms=["seizure"],
        sourceType="blog",
        body="Unofficial content",
    )
    assert source_retriever.attach_citations([source], ["seizure"]) == []


def test_response_does_not_contain_diagnostic_claims():
    response = run_triage(make_request(symptomList=["kho tho"], breathingStatus="kho tho"))
    combined = " ".join([
        response.summary,
        response.possibleConcern,
        response.recommendedAction,
    ]).lower()
    blocked_phrases = ["tre bi viem phoi", "tre bi sot xuat huyet", "diagnosed with"]
    assert all(phrase not in combined for phrase in blocked_phrases)


def test_missing_local_source_calls_realtime_search(monkeypatch, tmp_path):
    calls = {"count": 0}
    realtime_source = official_source(
        symptoms=["breathing_difficulty"],
        url="https://www.who.int/publications/i/item/978-92-4-154837-3",
    )

    def fake_search(symptoms, rules):
        calls["count"] += 1
        return [realtime_source]

    monkeypatch.setattr(source_retriever, "MEDICAL_SOURCES_DIR", tmp_path)
    monkeypatch.setattr(graph_module, "retrieve_realtime_sources", fake_search)
    monkeypatch.setattr(graph_module, "cache_pending_sources", lambda sources: [])

    response = run_triage(make_request(symptomList=["kho tho"], breathingStatus="kho tho"))

    assert calls["count"] == 1
    assert response.riskLevel == "RED"
    assert response.citations
    assert response.citations[0].sourceStatus == "PENDING_REVIEW"


def test_realtime_who_source_attaches_pending_citation(monkeypatch, tmp_path):
    realtime_source = official_source(
        symptoms=["seizure"],
        url="https://www.who.int/publications/i/item/978-92-4-154837-3",
    )
    cached = []

    monkeypatch.setattr(source_retriever, "MEDICAL_SOURCES_DIR", tmp_path)
    monkeypatch.setattr(graph_module, "retrieve_realtime_sources", lambda symptoms, rules: [realtime_source])
    monkeypatch.setattr(graph_module, "cache_pending_sources", lambda sources: cached.extend(sources))

    response = run_triage(make_request(symptomList=["co giat"], seizure=True))

    assert response.riskLevel == "RED"
    assert response.citations[0].domain == "who.int"
    assert response.citations[0].sourceStatus == "PENDING_REVIEW"
    assert cached == [realtime_source]


def test_realtime_source_outside_whitelist_is_rejected():
    bad_source = official_source(
        symptoms=["seizure"],
        url="https://example.com/medical",
        domain="example.com",
    )
    assert source_retriever.attach_citations([bad_source], ["seizure"]) == []
    assert source_retriever.retrieve_realtime_sources.__name__ == "retrieve_realtime_sources"


def test_realtime_search_does_not_change_risk(monkeypatch, tmp_path):
    realtime_source = official_source(
        symptoms=["cough"],
        url="https://www.who.int/publications/i/item/978-92-4-154837-3",
    )
    monkeypatch.setattr(source_retriever, "MEDICAL_SOURCES_DIR", tmp_path)
    monkeypatch.setattr(graph_module, "retrieve_realtime_sources", lambda symptoms, rules: [realtime_source])
    monkeypatch.setattr(graph_module, "cache_pending_sources", lambda sources: [])

    response = run_triage(make_request(symptomList=["ho"], temperatureC=None))

    assert response.riskLevel == "YELLOW"
    assert response.citations
    assert response.citations[0].sourceStatus == "PENDING_REVIEW"


def test_build_search_queries_are_site_restricted():
    queries = official_source_searcher.build_search_queries(["fever"])
    assert queries
    assert all(query.startswith("site:") for query in queries)
    assert all("child fever official medical source" in query for query in queries)


def test_pending_realtime_source_is_cached_with_review_metadata(tmp_path):
    source = official_source(
        symptoms=["seizure"],
        url="https://www.who.int/publications/i/item/978-92-4-154837-3",
    )
    path = cache_pending_source(source, cache_dir=tmp_path)
    post = frontmatter.load(path)

    assert post.metadata["sourceStatus"] == "PENDING_REVIEW"
    assert post.metadata["retrievedBy"] == "realtime_official_search"
    assert post.metadata["url"] == source.url
    assert post.metadata["domain"] == "who.int"
    assert post.metadata["matchedSymptoms"] == ["seizure"]


def official_source(
    *,
    symptoms: list[str],
    url: str,
    domain: str = "who.int",
) -> SourceDocument:
    return SourceDocument(
        id="REALTIME_WHO_TEST",
        title="WHO child danger signs",
        organization="WHO",
        url=url,
        domain=domain,
        lastReviewed="2026-07-10",
        topic="danger_signs",
        ageRange="child",
        riskLevels=["RED", "YELLOW"],
        symptoms=symptoms,
        sourceType="official_web_page",
        sourceStatus="PENDING_REVIEW",
        retrievedAt="2026-07-10T00:00:00+00:00",
        retrievedBy="realtime_official_search",
        body="Official child medical source about danger signs and symptom monitoring.",
    )
