"""Great-circle distance, in pure Python.

Haversine on a mean-radius sphere. Accurate to well under a kilometre at city scale, which is
the only scale this is used at — ranking a handful of hospitals by how far away they are. No
geography library, and no attempt at an ellipsoidal model: the seed coordinates are rounded to
four decimals anyway, so extra precision here would be invented.
"""

from __future__ import annotations

from decimal import Decimal
from math import asin, cos, radians, sin, sqrt

#: Mean Earth radius (IUGG), kilometres.
_EARTH_RADIUS_KM = 6371.0088


def distance_km(
    lat1: Decimal | float | None,
    lng1: Decimal | float | None,
    lat2: Decimal | float | None,
    lng2: Decimal | float | None,
) -> float | None:
    """Kilometres between two points, or ``None`` when any coordinate is missing.

    Returning ``None`` rather than raising is deliberate: a facility with no coordinates on
    record should drop out of the distance ranking, not take the whole emergency response down
    with it.
    """

    if any(value is None for value in (lat1, lng1, lat2, lng2)):
        return None

    phi1, phi2 = radians(float(lat1)), radians(float(lat2))
    delta_phi = radians(float(lat2) - float(lat1))
    delta_lambda = radians(float(lng2) - float(lng1))

    a = sin(delta_phi / 2) ** 2 + cos(phi1) * cos(phi2) * sin(delta_lambda / 2) ** 2
    return round(2 * _EARTH_RADIUS_KM * asin(sqrt(a)), 3)
