"""MF07 emergency locator.

The properties worth pinning are the ones a person in an emergency depends on: the hotlines are
always there and always the verified four, an unverified phone number never reaches the caller,
and the 115 instruction survives even when the facility list is empty.
"""

from __future__ import annotations

from decimal import Decimal

import pytest

from app.constants.emergency import HOTLINE_NUMBERS_WHITELIST, HOTLINE_REGISTRY
from app.repositories.care_facility_repository import load_verified_facilities
from app.services.emergency_support_service import build_response
from app.schemas import EmergencySupportRequest
from app.utils.geo import distance_km

# Hoàn Kiếm to Bệnh viện Nhi Trung ương, about 1.4 km apart.
_HANOI_LAT, _HANOI_LNG = Decimal("21.028"), Decimal("105.854")
_CHILDRENS_LAT, _CHILDRENS_LNG = Decimal("21.018"), Decimal("105.846")


def _request(**overrides) -> EmergencySupportRequest:
    payload = {
        "userId": "user-mf07-test",
        "consentGiven": False,
        "latitude": None,
        "longitude": None,
        "accuracyMeters": None,
        "contextType": "MANUAL_BUTTON",
        "contextId": None,
    }
    payload.update(overrides)
    return EmergencySupportRequest(**payload)


@pytest.mark.parametrize(
    "lat1, lng1, lat2, lng2",
    [
        (None, _HANOI_LNG, _CHILDRENS_LAT, _CHILDRENS_LNG),
        (_HANOI_LAT, None, _CHILDRENS_LAT, _CHILDRENS_LNG),
        (_HANOI_LAT, _HANOI_LNG, None, _CHILDRENS_LNG),
        (_HANOI_LAT, _HANOI_LNG, _CHILDRENS_LAT, None),
    ],
)
def test_haversine_returns_none_when_any_input_null(lat1, lng1, lat2, lng2):
    assert distance_km(lat1, lng1, lat2, lng2) is None


def test_haversine_hanoi_childrens_hospital_approx():
    measured = distance_km(_HANOI_LAT, _HANOI_LNG, _CHILDRENS_LAT, _CHILDRENS_LNG)

    assert measured == pytest.approx(1.4, abs=0.3)


def test_response_hotlines_always_exactly_four_and_whitelisted():
    response = build_response(_request())

    assert len(response.hotlines) == 4
    assert {hotline.number for hotline in response.hotlines} == HOTLINE_NUMBERS_WHITELIST
    # 115 leads the banner: the one number that answers around the clock, everywhere.
    assert response.hotlines[0].number == "115"


def test_consent_true_with_latlng_returns_snapshot_id():
    response = build_response(
        _request(
            consentGiven=True,
            latitude=_HANOI_LAT,
            longitude=_HANOI_LNG,
            contextType="TRIAGE_RED",
            contextId="assessment-1",
        )
    )

    assert response.snapshotId is not None
    assert len(response.snapshotId) == 32
    assert all(character in "0123456789abcdef" for character in response.snapshotId)


def test_consent_false_returns_none_snapshot_id():
    response = build_response(_request(consentGiven=False))

    assert response.snapshotId is None
    assert "không chia sẻ vị trí" in response.disclaimer


def test_unverified_facility_phone_is_null():
    """Every seeded facility is PENDING, so no number may be rendered."""

    response = build_response(
        _request(consentGiven=True, latitude=_HANOI_LAT, longitude=_HANOI_LNG)
    )

    assert response.facilities, "Hà Nội should be within range of the seeded hospitals"
    assert all(facility.phone is None for facility in response.facilities)
    assert all(
        facility.phoneVerificationStatus != "VERIFIED" for facility in response.facilities
    )


def test_empty_verified_facility_returns_115_in_disclaimer():
    assert load_verified_facilities() == []

    response = build_response(_request(consentGiven=False))

    assert response.facilities == []
    assert "115" in response.disclaimer
    assert len(response.hotlines) == 4


def test_backward_compatibility_existing_triage_endpoint():
    """The new route must not disturb the one the Java boundary already calls."""

    from fastapi.routing import APIRoute

    from app.main import app

    routes = {
        route.path: sorted(route.methods)
        for route in app.routes
        if isinstance(route, APIRoute)
    }

    assert "/internal/triage/turn" in routes
    assert "POST" in routes["/internal/triage/turn"]
    assert "/health" in routes
    assert "/internal/emergency/support" in routes
    assert "POST" in routes["/internal/emergency/support"]


def test_registry_and_whitelist_cannot_drift():
    assert len(HOTLINE_REGISTRY) == 4
    assert {entry["number"] for entry in HOTLINE_REGISTRY} == HOTLINE_NUMBERS_WHITELIST
    assert all(entry["sourceUrl"].startswith("https://") for entry in HOTLINE_REGISTRY)
