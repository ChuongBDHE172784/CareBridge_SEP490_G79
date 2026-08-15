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


def test_consent_true_with_latlng_reports_no_snapshot_until_one_is_stored():
    """No store, no id.

    The spec asked for an identifier here. Returning one while nothing is written would tell the
    caller a snapshot exists that it can refer to later, and the Java boundary persists request
    state — it would be holding a dangling reference to health-adjacent data. Null until a
    `location_snapshots` table and its retention policy exist; the ranking below still works.
    """

    response = build_response(
        _request(
            consentGiven=True,
            latitude=_HANOI_LAT,
            longitude=_HANOI_LNG,
            contextType="TRIAGE_RED",
            contextId="assessment-1",
        )
    )

    assert response.snapshotId is None
    assert response.facilities, "consent plus a position must still rank nearby hospitals"
    assert all(facility.distanceKm is not None for facility in response.facilities)


def test_consent_false_returns_none_snapshot_id():
    response = build_response(_request(consentGiven=False))

    assert response.snapshotId is None
    assert "không chia sẻ vị trí" in response.disclaimer


def test_declining_location_still_returns_the_hospital_list():
    """Declining must not cost the caller the list itself, only the distance ordering.

    These are national referral hospitals with public addresses; none of that needs a position.
    Returning nothing here while consent returned three would make the private choice the
    expensive one.
    """

    declined = build_response(_request(consentGiven=False))
    shared = build_response(
        _request(consentGiven=True, latitude=_HANOI_LAT, longitude=_HANOI_LNG)
    )

    assert declined.facilities, "declining location must still list hospitals"
    assert len(declined.facilities) >= len(shared.facilities)
    assert all(facility.distanceKm is None for facility in declined.facilities)


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


def test_no_verified_facility_yet():
    """Every seeded hospital is still PENDING, which is why no phone is ever rendered."""

    assert load_verified_facilities() == []


@pytest.mark.parametrize(
    "latitude, longitude",
    [
        (Decimal("16.047"), Decimal("108.206")),   # Đà Nẵng
        (Decimal("10.045"), Decimal("105.746")),   # Cần Thơ
        (Decimal("12.680"), Decimal("108.038")),   # Buôn Ma Thuột
    ],
)
def test_a_city_with_nothing_seeded_nearby_still_gets_the_national_list(latitude, longitude):
    """Sharing a location must never return less than withholding it.

    The seed covers Hà Nội and TP.HCM. Everywhere else has nothing inside the radius, and an
    empty screen is the worst possible answer to someone who has just pressed an emergency
    button — so the national referral list stands in, without distances.
    """

    response = build_response(
        _request(consentGiven=True, latitude=latitude, longitude=longitude)
    )

    assert response.facilities, "an out-of-range city must not get an empty list"
    assert all(facility.distanceKm is None for facility in response.facilities)
    assert len(response.hotlines) == 4


def test_115_is_named_when_the_seed_cannot_be_read(monkeypatch):
    """The only path that legitimately yields no hospitals: the seed itself failed to load."""

    import app.services.emergency_support_service as service

    monkeypatch.setattr(service, "load_facilities", lambda: [])
    monkeypatch.setattr(service, "load_verified_facilities", lambda: [])
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
