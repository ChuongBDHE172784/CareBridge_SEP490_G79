from __future__ import annotations

from typing import Any

import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError

from app.contracts import (
    LANDMARK_SCHEMA_VERSION,
    MAX_SEQUENCE_NUMBER,
    MODEL_VERSION,
    InferenceRequest,
    Landmark,
)
from app.main import create_app
from app.model_registry import MODEL_SPECS, Prediction


class FakeRegistry:
    def __init__(self, *, ready: bool = True):
        self.ready = ready
        self.error_code = None if ready else "HASH_MISMATCH"

    def load(self) -> None:
        return None

    def health_snapshot(self) -> dict[str, Any]:
        if self.ready:
            return {
                "status": "ready",
                "ready": True,
                "modelVersion": MODEL_VERSION,
                "loadedModels": sorted(MODEL_SPECS),
                "artifactCount": 8,
            }
        return {
            "status": "unavailable",
            "ready": False,
            "code": self.error_code,
            "modelVersion": MODEL_VERSION,
        }

    @staticmethod
    def supports(exercise_key: str) -> bool:
        return exercise_key in MODEL_SPECS

    @staticmethod
    def required_landmarks(exercise_key: str) -> tuple[str, ...]:
        return MODEL_SPECS[exercise_key].landmarks

    @staticmethod
    def predict(exercise_key: str, landmarks: dict[str, Landmark]) -> Prediction:
        assert landmarks
        predicted = "up" if exercise_key == "squat" else "C"
        return Prediction(predicted, 0.99, True, 99.0, ())


def _payload(exercise_key: str) -> dict[str, Any]:
    return {
        "schemaVersion": LANDMARK_SCHEMA_VERSION,
        "modelVersion": MODEL_VERSION,
        "exerciseKey": exercise_key,
        "sequenceNumber": 7,
        "inferenceStreamId": "demo-stream-1",
        "landmarks": {
            name: {"x": 0.5, "y": 0.5, "z": 0.0, "visibility": 0.99}
            for name in MODEL_SPECS[exercise_key].landmarks
        },
    }


@pytest.mark.parametrize("exercise_key", sorted(MODEL_SPECS))
def test_supported_exercises_follow_versioned_contract(exercise_key: str) -> None:
    with TestClient(create_app(FakeRegistry())) as client:
        response = client.post("/v1/inference/landmarks", json=_payload(exercise_key))

    assert response.status_code == 200
    body = response.json()
    assert body["modelVersion"] == MODEL_VERSION
    assert body["exerciseKey"] == exercise_key
    assert body["sequenceNumber"] == 7
    assert body["inferenceStreamId"] == "demo-stream-1"
    assert body["correct"] is True


def test_unknown_exercise_is_rejected_without_fallback() -> None:
    payload = _payload("plank")
    payload["exerciseKey"] = "uploaded_model"
    with TestClient(create_app(FakeRegistry())) as client:
        response = client.post("/v1/inference/landmarks", json=payload)

    assert response.status_code == 400
    assert response.json()["code"] == "UNSUPPORTED_EXERCISE"


def test_missing_required_landmark_is_rejected() -> None:
    payload = _payload("plank")
    payload["landmarks"].pop("nose")
    with TestClient(create_app(FakeRegistry())) as client:
        response = client.post("/v1/inference/landmarks", json=payload)

    assert response.status_code == 422
    assert response.json()["code"] == "MISSING_LANDMARKS"


def test_non_finite_landmark_is_rejected_by_contract() -> None:
    payload = _payload("plank")
    payload["landmarks"]["nose"]["x"] = float("nan")

    with pytest.raises(ValidationError):
        InferenceRequest.model_validate(payload)


def test_sequence_number_is_bounded_to_signed_64_bit_wire_range() -> None:
    payload = _payload("plank")
    payload["sequenceNumber"] = MAX_SEQUENCE_NUMBER + 1

    with pytest.raises(ValidationError):
        InferenceRequest.model_validate(payload)


def test_unready_registry_has_unhealthy_health_and_no_inference() -> None:
    registry = FakeRegistry(ready=False)
    with TestClient(create_app(registry)) as client:
        health = client.get("/health")
        inference = client.post("/v1/inference/landmarks", json=_payload("plank"))

    assert health.status_code == 503
    assert health.json()["code"] == "HASH_MISMATCH"
    assert inference.status_code == 503
    assert inference.json()["code"] == "MODEL_UNAVAILABLE"
