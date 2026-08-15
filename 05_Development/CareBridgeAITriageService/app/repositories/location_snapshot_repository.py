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
) -> str | None:
    """Record a consented location snapshot and return its identifier, or ``None``.

    Returns ``None`` while there is nothing to record into. An earlier version minted a
    ``uuid4().hex`` and returned it, which told the caller a snapshot existed and could be
    referred to later; nothing was stored, so that reference pointed at nothing. A caller
    persisting it — the Java boundary keeps request state — would have been holding a dangling
    id for health-adjacent data.

    The signature already returns what the real implementation will: once a
    ``location_snapshots`` table exists and its retention policy is agreed, this returns the row
    id and no other file changes.
    """

    return None
