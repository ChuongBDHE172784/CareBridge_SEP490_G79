"""HTTP smoke checks for the one canonical CareBridge AI triage route."""

from __future__ import annotations

import hashlib
import json
import os
import sys
import uuid
from pathlib import Path
from urllib.request import Request, urlopen

BASE_URL = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8001"
INTERNAL_KEY = os.getenv("AI_TRIAGE_INTERNAL_API_KEY", "")
RULES = Path(__file__).resolve().parent / "data" / "triage_rules_v2.json"
RULESET_HASH = hashlib.sha256(RULES.read_bytes()).hexdigest()


def call(path: str, payload: dict | None = None) -> dict:
    body = None if payload is None else json.dumps(payload).encode()
    headers = {"Content-Type": "application/json"} if body else {}
    if path == "/internal/triage/turn":
        headers["X-CareBridge-Internal-Key"] = INTERNAL_KEY
    request = Request(
        f"{BASE_URL}{path}", body, headers=headers, method="POST" if body else "GET"
    )
    with urlopen(request, timeout=8) as response:
        assert response.status == 200
        return json.load(response)


def turn(message: str, *, session_id: str | None = None, version: int = 0,
         previous_state: dict | None = None, **updates: object) -> dict:
    payload = {
        "sessionId": session_id or str(uuid.uuid4()),
        "stateVersion": version,
        "expectedStateVersion": version,
        "requestId": f"request_{uuid.uuid4().hex}",
        "messageId": f"message_{uuid.uuid4().hex}",
        "latestUserMessage": message,
        "selectedTarget": "UNKNOWN",
        "journeyContext": {},
        "previousState": previous_state,
        "signals": {},
        "measurements": {},
        "answeredQuestionIds": [],
        "submittedOptionQuestionIds": [],
        "submittedOptionCodes": [],
        "expectedRulesetHash": RULESET_HASH,
    }
    payload.update(updates)
    return call("/internal/triage/turn", payload)["state"]


assert INTERNAL_KEY, "AI_TRIAGE_INTERNAL_API_KEY is required for canonical smoke"
assert call("/health")["status"] == "UP"

red = turn(
    "Tôi khó thở dữ dội",
    selectedTarget="MOTHER",
    journeyContext={"stage": "PREGNANCY"},
    signals={"SEVERE_BREATHING_DIFFICULTY": "PRESENT"},
)
assert red["triageOutcome"] == "RED" and red["stopConversation"] is True
assert red["rationale"] and red["evidenceStatus"] in {
    "AVAILABLE", "PENDING", "UNAVAILABLE", "REJECTED"
}

session_id = str(uuid.uuid4())
vague = turn(
    "Bé sốt nhẹ",
    session_id=session_id,
    selectedTarget="BABY",
    journeyContext={"stage": "INFANT_0_12M", "babyAgeMonths": 6},
    signals={"FEVER": "PRESENT"},
)
assert vague["triageOutcome"] == "NEEDS_MORE_INFO"
assert vague["plannedQuestionIds"], "vague fever must ask a canonical follow-up"

numeric = turn(
    "Nhiệt độ của bé là 38 độ C",
    session_id=session_id,
    version=vague["stateVersion"],
    previous_state=vague,
    selectedTarget="BABY",
    measurements={"temperatureC": 38.0},
    answeredQuestionIds=["Q_BABY_TEMPERATURE"],
)
assert numeric["triageOutcome"] != "GREEN"
assert numeric["requiredAction"] != "SYSTEM_UNAVAILABLE"

print("Canonical AI triage HTTP smoke passed")
