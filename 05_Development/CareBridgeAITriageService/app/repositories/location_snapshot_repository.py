"""Location snapshots taken with the user's consent.

MVP stand-in. There is no `location_snapshots` table in this service yet — persistence lives on
the Java side — so this mints and returns an identifier without storing the coordinates
anywhere. That is the conservative direction while the schema is being settled: the response can
carry a snapshot reference, and no location data is written to a store nobody has reviewed for
retention.

The signature is the one the real repository will keep, so wiring it to a table later changes
this file and nothing else.
"""

from __future__ import annotations

from decimal import Decimal
from uuid import uuid4


def save_snapshot(
    *,
    user_id: str,
    latitude: Decimal | None,
    longitude: Decimal | None,
    accuracyMeters: Decimal | None = None,
    contextType: str | None = None,
    contextId: str | None = None,
) -> str:
    """Record a consented location snapshot and return its identifier.

    Returns a fresh 32-character hex identifier. Coordinates are accepted and deliberately not
    retained until the retention policy for this data is agreed — see the open items in the
    MF07 report.
    """

    return uuid4().hex
