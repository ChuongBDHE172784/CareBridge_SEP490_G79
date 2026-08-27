from carebridge_evaluation.config import EvaluationSettings
from carebridge_evaluation.registry_scan import scan_hardcoded_registry


def test_production_evidence_registry_has_no_hardcoded_domain_source_of_truth():
    ai_service = EvaluationSettings.from_env().ai_service_path
    assert scan_hardcoded_registry(ai_service) == []


def test_scanner_detects_literal_domain_registry(tmp_path):
    app = tmp_path / "app"
    app.mkdir()
    (app / "bad.py").write_text('APPROVED_DOMAINS = {"who.int", "moh.gov.vn"}\n', encoding="utf-8")
    violations = scan_hardcoded_registry(tmp_path)
    assert len(violations) == 1
    assert "who.int" in violations[0]
