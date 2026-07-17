from carebridge_evaluation.external import GalileoAdapter, GalileoStatus


def test_galileo_is_disabled_by_default_and_does_not_require_sdk():
    result = GalileoAdapter(enabled=False).run([])
    assert result.status == GalileoStatus.DISABLED
    assert result.auxiliary_metrics == {}


def test_galileo_missing_key_is_an_explicit_non_failure_skip(monkeypatch):
    monkeypatch.delenv("GALILEO_API_KEY", raising=False)
    result = GalileoAdapter(enabled=True, api_key=None).run([])
    assert result.status == GalileoStatus.SKIPPED_NO_EXTERNAL_KEY

