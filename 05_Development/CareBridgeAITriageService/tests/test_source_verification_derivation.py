"""PHASE0-TEST-005 — rule source verification must be derived, never hand-set.

The registry once claimed ``SOURCE_VERIFIED`` on every rule while the source manifest said
PENDING. Two artifacts disagreeing about whether the evidence was ever checked is worse than
either answer alone, so the value is now computed by the sync tool and the registry copy is
not authoritative. These tests pin the derivation table and the honesty of the current state.
"""

from __future__ import annotations

import importlib.util
import json
from pathlib import Path

import pytest

CONTRACTS = Path(__file__).resolve().parents[2] / "Contracts" / "triage"
SYNC_TOOL = Path(__file__).resolve().parents[2] / "DevTools" / "sync_triage_rule_registry.py"

VALID_STATUSES = {"PENDING", "SOURCE_VERIFIED", "SOURCE_CHANGED", "BROKEN",
                  "UNRESOLVED_SOURCE", "NOT_APPLICABLE"}


def _load_sync_module():
    spec = importlib.util.spec_from_file_location("sync_tool", SYNC_TOOL)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


@pytest.fixture(scope="module")
def registry_document() -> dict:
    return json.loads((CONTRACTS / "triage_rules_v2.json").read_text(encoding="utf-8"))


@pytest.fixture(scope="module")
def source_manifest() -> dict:
    return json.loads((CONTRACTS / "source_verification_manifest.json").read_text(encoding="utf-8"))


def test_derivation_matches_the_manifest(registry_document, source_manifest):
    """Recompute every rule's status from the manifest and compare with what shipped."""

    by_source = {s["sourceId"]: s["verificationStatus"] for s in source_manifest["sources"]}

    for rule in registry_document["rules"]:
        statuses = [by_source.get(sid, "PENDING") for sid in rule.get("sourceIds", [])]
        if not statuses:
            expected = "NOT_APPLICABLE"
        elif "BROKEN" in statuses:
            expected = "BROKEN"
        elif "SOURCE_CHANGED" in statuses:
            expected = "SOURCE_CHANGED"
        elif all(status == "SOURCE_VERIFIED" for status in statuses):
            expected = "SOURCE_VERIFIED"
        else:
            expected = "PENDING"

        assert rule["sourceVerificationStatus"] == expected, rule["ruleId"]


def test_no_rule_claims_verification_the_manifest_does_not_support(registry_document,
                                                                   source_manifest):
    verified = {s["sourceId"] for s in source_manifest["sources"]
                if s["verificationStatus"] == "SOURCE_VERIFIED"}

    for rule in registry_document["rules"]:
        if rule["sourceVerificationStatus"] == "SOURCE_VERIFIED":
            assert set(rule.get("sourceIds", [])) <= verified, (
                f"{rule['ruleId']} claims SOURCE_VERIFIED but cites an unverified source"
            )


def test_current_state_is_honest_about_unverified_sources(registry_document, source_manifest):
    """Today nothing has been fetched and hashed, so nothing may claim verification."""

    assert source_manifest["overallStatus"] == "PENDING"
    assert all(rule["sourceVerificationStatus"] == "PENDING"
               for rule in registry_document["rules"])


def test_unresolvable_source_keeps_a_null_url_rather_than_a_guess(source_manifest):
    byt = next(s for s in source_manifest["sources"] if s["sourceId"] == "BYT_1139_2026")
    assert byt["verificationStatus"] == "UNRESOLVED_SOURCE"
    assert byt["url"] is None, "an unlocatable citation must not be given an invented URL"


def test_every_source_status_is_from_the_closed_set(source_manifest):
    for source in source_manifest["sources"]:
        assert source["verificationStatus"] in VALID_STATUSES, source["sourceId"]


def test_who_sources_record_their_intended_audience_limitation(source_manifest):
    """WHO material is written for trained health workers, not for public self-assessment."""

    iitt = next(s for s in source_manifest["sources"] if s["sourceId"] == "WHO_IITT_ADULT")
    joined = " ".join(iitt["limitations"]).lower()
    assert "facility-based" in joined
    assert "not an official who iitt implementation" in joined

    pcpnc = next(s for s in source_manifest["sources"] if s["sourceId"] == "WHO_PCPNC_2015")
    assert any("skilled attendants" in limitation.lower() for limitation in pcpnc["limitations"])


def test_manifest_disclaims_publisher_endorsement(source_manifest):
    assert "has reviewed, approved or endorsed" in source_manifest["endorsementDisclaimer"]


def test_sync_check_reports_drift_when_a_status_is_hand_edited(tmp_path, monkeypatch):
    """A hand-edited registry value must be reported, not silently accepted."""

    module = _load_sync_module()
    problems: list[str] = []

    document = json.loads((CONTRACTS / "triage_rules_v2.json").read_text(encoding="utf-8"))
    document["rules"][0]["sourceVerificationStatus"] = "SOURCE_VERIFIED"

    staged = tmp_path / "triage"
    staged.mkdir()
    (staged / "triage_rules_v2.json").write_text(json.dumps(document), encoding="utf-8")
    (staged / "source_verification_manifest.json").write_text(
        (CONTRACTS / "source_verification_manifest.json").read_text(encoding="utf-8"),
        encoding="utf-8",
    )
    monkeypatch.setattr(module, "CANONICAL_DIR", staged)

    module.derive_source_verification(True, problems)

    assert any("sourceVerificationStatus" in problem for problem in problems), (
        "hand-editing a derived value must surface as drift"
    )
