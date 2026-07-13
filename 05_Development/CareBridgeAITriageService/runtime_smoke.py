"""HTTP smoke checks for a running CareBridge AI triage service."""

from __future__ import annotations

import json
import sys
from urllib.request import Request, urlopen

BASE_URL = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8001"


def call(path: str, payload: dict | None = None) -> dict:
    body = None if payload is None else json.dumps(payload).encode()
    request = Request(
        f"{BASE_URL}{path}",
        body,
        headers={"Content-Type": "application/json"} if body else {},
        method="POST" if body else "GET",
    )
    with urlopen(request, timeout=8) as response:
        assert response.status == 200
        return json.load(response)


def intake(symptoms: list[str], **updates: object) -> dict:
    payload = {
        "childAgeMonths": 18,
        "symptomList": symptoms,
        "duration": "1 ngay",
        "feedingStatus": "bu uong tot",
        "breathingStatus": "tho binh thuong",
        "consciousnessStatus": "tinh tao",
        "seizure": False,
        "dehydrationSigns": [],
    }
    payload.update(updates)
    return payload


assert call("/health")["status"] == "UP"
assert call("/triage/child", intake(["sot nhe"], temperatureC=37.8))["riskLevel"] == "GREEN"
assert call("/triage/child", intake(["sot"], temperatureC=38.2))["riskLevel"] == "YELLOW"
red = call("/triage/child", intake(["kho tho"], breathingStatus="kho tho"))
assert red["riskLevel"] == "RED" and red["emergencyActionRequired"] is True

session_id = "11111111-1111-1111-1111-111111111111"
start = call(
    "/triage/intake/start",
    {"intakeSessionId": session_id, "initialText": "be sot", "currentIntake": {}},
)
assert start["status"] == "ASK_MORE"
assert start["intakeSessionId"] == session_id
continued = call(
    "/triage/intake/continue",
    {
        "intakeSessionId": session_id,
        "currentIntake": start["mergedIntake"],
        "messages": [],
        "newAnswers": {"childAgeMonths": 18, "temperatureC": 38.2},
        "round": start["round"],
    },
)
assert continued["intakeSessionId"] == session_id
assert continued["status"] in {"ASK_MORE", "TRIAGE_COMPLETE"}

print("TV5 AI triage HTTP smoke passed")
