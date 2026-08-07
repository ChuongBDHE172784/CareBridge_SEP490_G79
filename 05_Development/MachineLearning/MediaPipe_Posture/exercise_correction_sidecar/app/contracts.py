from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator


LANDMARK_SCHEMA_VERSION = "mediapipe-pose-landmarks-v1"
RESPONSE_SCHEMA_VERSION = "exercise-correction-inference-v1"
MODEL_VERSION = "exercise-correction@202a0a802d8d2e3ea42f00e6a8c47da9cafc09d7"
MAX_SEQUENCE_NUMBER = 9_223_372_036_854_775_807

MEDIAPIPE_POSE_LANDMARKS = frozenset(
    {
        "nose",
        "left_eye_inner",
        "left_eye",
        "left_eye_outer",
        "right_eye_inner",
        "right_eye",
        "right_eye_outer",
        "left_ear",
        "right_ear",
        "mouth_left",
        "mouth_right",
        "left_shoulder",
        "right_shoulder",
        "left_elbow",
        "right_elbow",
        "left_wrist",
        "right_wrist",
        "left_pinky",
        "right_pinky",
        "left_index",
        "right_index",
        "left_thumb",
        "right_thumb",
        "left_hip",
        "right_hip",
        "left_knee",
        "right_knee",
        "left_ankle",
        "right_ankle",
        "left_heel",
        "right_heel",
        "left_foot_index",
        "right_foot_index",
    }
)


class Landmark(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False, frozen=True)

    x: float
    y: float
    z: float
    visibility: float = Field(ge=0.0, le=1.0)


class InferenceRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)

    schemaVersion: Literal[LANDMARK_SCHEMA_VERSION]
    modelVersion: Literal[MODEL_VERSION]
    exerciseKey: str = Field(min_length=1, max_length=64, pattern=r"^[a-z0-9_]+$")
    sequenceNumber: int = Field(ge=0, le=MAX_SEQUENCE_NUMBER)
    inferenceStreamId: str | None = Field(
        default=None,
        min_length=1,
        max_length=128,
        pattern=r"^[A-Za-z0-9._:-]+$",
    )
    landmarks: dict[str, Landmark] = Field(min_length=1, max_length=33)

    @field_validator("landmarks")
    @classmethod
    def reject_unknown_landmarks(cls, landmarks: dict[str, Landmark]) -> dict[str, Landmark]:
        unknown = sorted(set(landmarks) - MEDIAPIPE_POSE_LANDMARKS)
        if unknown:
            raise ValueError("unknown MediaPipe landmark names")
        return landmarks


class Feedback(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    code: str
    severity: Literal["INFO", "WARNING"]
    message: str


class InferenceResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    schemaVersion: Literal[RESPONSE_SCHEMA_VERSION] = RESPONSE_SCHEMA_VERSION
    modelVersion: Literal[MODEL_VERSION] = MODEL_VERSION
    exerciseKey: str
    sequenceNumber: int
    inferenceStreamId: str | None = None
    predictedClass: str
    # The movement phase, reported separately because predictedClass cannot carry it:
    # for a lunge in the down phase the error model's verdict replaces the class, so
    # the phase would otherwise be unrecoverable by the caller. Optional and additive,
    # so a caller built against the earlier v1 response simply ignores it.
    stage: str | None = None
    confidence: float = Field(ge=0.0, le=1.0)
    correct: bool
    score: float = Field(ge=0.0, le=100.0)
    feedback: list[Feedback]


class ErrorResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    code: str
    message: str
