"""Assemble the emergency support package.

Ordering is the whole point of this module. The hotlines are built first and unconditionally,
before any facility lookup can fail, because a person in an emergency needs a number to call
whether or not the seed file loaded, whether or not they shared their location, and whether or
not any hospital is within range. Everything after the hotlines is best-effort.

Nothing here diagnoses. It renders numbers, a fixed disclaimer, and a distance-sorted list.
"""

from __future__ import annotations

from decimal import Decimal, InvalidOperation

from app.constants.emergency import (
    DISCLAIMER_BASE,
    DISCLAIMER_EMPTY_FACILITIES_SUFFIX,
    DISCLAIMER_NO_CONSENT_SUFFIX,
    HOTLINE_NUMBERS_WHITELIST,
    HOTLINE_REGISTRY,
)
from app.repositories.care_facility_repository import (
    load_facilities,
    load_verified_facilities,
)
from app.repositories.location_snapshot_repository import save_snapshot
from app.schemas import (
    EmergencySupportRequest,
    EmergencySupportResponse,
    FacilityDto,
    HotlineDto,
)
from app.utils.geo import distance_km

_RADIUS_KM = 25.0
_MAX_FACILITIES = 5


def _to_hotlines() -> list[HotlineDto]:
    """The whitelist check is the gate, not a formality.

    A number can only be published if it appears both in the registry and in the whitelist, so
    adding one in a single place fails here instead of quietly reaching a user.
    """

    for entry in HOTLINE_REGISTRY:
        if entry["number"] not in HOTLINE_NUMBERS_WHITELIST:
            raise AssertionError(
                f"số hotline {entry['number']!r} không nằm trong whitelist đã xác minh"
            )
    return [HotlineDto(**entry) for entry in HOTLINE_REGISTRY]


def _to_facility(raw: dict, distance: float | None) -> FacilityDto:
    """Suppress the phone number unless it has been verified.

    An unverified number shown during an emergency is worse than no number: it costs the caller
    the one thing they do not have.
    """

    status = raw.get("phoneVerificationStatus", "PENDING")
    return FacilityDto(
        id=raw["id"],
        name=raw["name"],
        address=raw["address"],
        distanceKm=distance,
        phone=raw.get("phone") if status == "VERIFIED" else None,
        phoneVerificationStatus=status,
        openingHours=raw.get("openingHours"),
        sourceUrl=raw["sourceUrl"],
    )


def _coordinate(value: object) -> Decimal | None:
    try:
        return Decimal(str(value))
    except (InvalidOperation, TypeError, ValueError):
        return None


def _nearby(latitude: Decimal, longitude: Decimal) -> list[FacilityDto]:
    scored: list[tuple[float, dict]] = []
    for raw in load_facilities():
        distance = distance_km(
            latitude,
            longitude,
            _coordinate(raw.get("latitude")),
            _coordinate(raw.get("longitude")),
        )
        if distance is not None and distance <= _RADIUS_KM:
            scored.append((distance, raw))
    scored.sort(key=lambda item: item[0])
    return [_to_facility(raw, distance) for distance, raw in scored[:_MAX_FACILITIES]]


def build_response(request: EmergencySupportRequest) -> EmergencySupportResponse:
    """Hotlines always; facilities where we can, sorted by distance where we may."""

    hotlines = _to_hotlines()
    disclaimer = DISCLAIMER_BASE
    facilities: list[FacilityDto] = []
    snapshot_id: str | None = None

    has_position = request.latitude is not None and request.longitude is not None
    if request.consentGiven and has_position:
        snapshot_id = save_snapshot(
            user_id=request.userId,
            latitude=request.latitude,
            longitude=request.longitude,
            accuracyMeters=request.accuracyMeters,
            contextType=request.contextType,
            contextId=request.contextId,
        )
        facilities = _nearby(request.latitude, request.longitude)
    else:
        if not request.consentGiven:
            disclaimer += DISCLAIMER_NO_CONSENT_SUFFIX
        facilities = [
            _to_facility(raw, None) for raw in load_verified_facilities()[:_MAX_FACILITIES]
        ]

    if not facilities:
        disclaimer += DISCLAIMER_EMPTY_FACILITIES_SUFFIX

    return EmergencySupportResponse(
        hotlines=hotlines,
        disclaimer=disclaimer,
        facilities=facilities,
        snapshotId=snapshot_id,
    )
