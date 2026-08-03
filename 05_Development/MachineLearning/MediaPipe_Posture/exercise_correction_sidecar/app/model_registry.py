from __future__ import annotations

import hashlib
import hmac
import pickle
import re
import threading
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable, Mapping

from app.contracts import Feedback, Landmark, MODEL_VERSION


class ModelRegistryError(RuntimeError):
    """A sanitized model-registry failure suitable for health reporting."""

    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code


class RegistryUnavailable(ModelRegistryError):
    pass


@dataclass(frozen=True)
class ModelSpec:
    model: str
    landmarks: tuple[str, ...]
    labels: Mapping[str, tuple[bool, str, str]]
    threshold: float
    scaler: str | None = None
    stage_model: str | None = None


@dataclass(frozen=True)
class ModelBundle:
    model: Any
    scaler: Any | None
    stage_model: Any | None


@dataclass(frozen=True)
class Prediction:
    predicted_class: str
    confidence: float
    correct: bool
    score: float
    feedback: tuple[Feedback, ...]


MODEL_SPECS: dict[str, ModelSpec] = {
    "plank": ModelSpec(
        model="plank_model.pkl",
        scaler="plank_input_scaler.pkl",
        threshold=0.60,
        landmarks=(
            "nose",
            "left_shoulder",
            "right_shoulder",
            "left_elbow",
            "right_elbow",
            "left_wrist",
            "right_wrist",
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
        ),
        labels={
            "C": (True, "correct", "Posture is within the demo model's plank class."),
            "L": (False, "low_back", "Lower back is classified as sagging; lift the hips slightly."),
            "H": (False, "high_back", "Hips are classified as too high; lower them slightly."),
        },
    ),
    "bicep_curl": ModelSpec(
        model="bicep_curl_model.pkl",
        scaler="bicep_curl_input_scaler.pkl",
        threshold=0.95,
        landmarks=(
            "nose",
            "left_shoulder",
            "right_shoulder",
            "right_elbow",
            "left_elbow",
            "right_wrist",
            "left_wrist",
            "left_hip",
            "right_hip",
        ),
        labels={
            "C": (True, "correct", "Standing posture is within the demo model's correct class."),
            "L": (False, "lean_back", "Torso is classified as leaning back; stand more upright."),
        },
    ),
    "squat": ModelSpec(
        model="squat_model.pkl",
        threshold=0.70,
        landmarks=(
            "nose",
            "left_shoulder",
            "right_shoulder",
            "left_hip",
            "right_hip",
            "left_knee",
            "right_knee",
            "left_ankle",
            "right_ankle",
        ),
        labels={
            "up": (True, "squat_up", "The demo model classified the standing phase."),
            "down": (True, "squat_down", "The demo model classified the lowering phase."),
        },
    ),
    "lunge": ModelSpec(
        model="lunge_err_model.pkl",
        stage_model="lunge_stage_model.pkl",
        scaler="lunge_input_scaler.pkl",
        threshold=0.80,
        landmarks=(
            "nose",
            "left_shoulder",
            "right_shoulder",
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
        ),
        labels={
            "C": (True, "correct", "Front-knee position is within the demo model's correct class."),
            "L": (False, "knee_over_toe", "Front knee is classified as moving too far over the toes."),
        },
    ),
}

EXPECTED_ARTIFACTS = frozenset(
    artifact
    for spec in MODEL_SPECS.values()
    for artifact in (spec.model, spec.scaler, spec.stage_model)
    if artifact is not None
)

_MANIFEST_LINE = re.compile(r"^([0-9a-fA-F]{64})[ \t]+(?:\*?)([^/\\]+)$")


def _default_pickle_loader(path: Path) -> Any:
    # Pickle is deliberately limited to hash-verified, build-vendored artifacts.
    with path.open("rb") as handle:
        return pickle.load(handle)


def _default_frame_factory(data: Any, columns: list[str] | None = None) -> Any:
    import pandas as pd

    return pd.DataFrame(data, columns=columns)


class ModelRegistry:
    def __init__(
        self,
        model_dir: Path,
        manifest_path: Path,
        *,
        pickle_loader: Callable[[Path], Any] = _default_pickle_loader,
        frame_factory: Callable[[Any, list[str] | None], Any] = _default_frame_factory,
    ) -> None:
        self._model_dir = model_dir
        self._manifest_path = manifest_path
        self._pickle_loader = pickle_loader
        self._frame_factory = frame_factory
        self._bundles: dict[str, ModelBundle] = {}
        self._prediction_lock = threading.Lock()
        self.ready = False
        self.error_code: str | None = "NOT_LOADED"

    def load(self) -> None:
        if self.ready:
            return
        self.ready = False
        self.error_code = "LOAD_FAILED"
        self._bundles = {}
        try:
            verified_paths = self._verify_artifacts()
            loaded = {name: self._pickle_loader(path) for name, path in verified_paths.items()}
            self._bundles = {
                key: ModelBundle(
                    model=loaded[spec.model],
                    scaler=loaded[spec.scaler] if spec.scaler else None,
                    stage_model=loaded[spec.stage_model] if spec.stage_model else None,
                )
                for key, spec in MODEL_SPECS.items()
            }
        except ModelRegistryError as exc:
            self.error_code = exc.code
            raise
        except Exception as exc:
            self.error_code = "MODEL_LOAD_FAILED"
            raise ModelRegistryError("MODEL_LOAD_FAILED", "A pinned model artifact could not be loaded") from exc
        self.error_code = None
        self.ready = True

    def health_snapshot(self) -> dict[str, Any]:
        if not self.ready:
            return {
                "status": "unavailable",
                "ready": False,
                "code": self.error_code or "MODEL_UNAVAILABLE",
                "modelVersion": MODEL_VERSION,
            }
        return {
            "status": "ready",
            "ready": True,
            "modelVersion": MODEL_VERSION,
            "loadedModels": sorted(self._bundles),
            "artifactCount": len(EXPECTED_ARTIFACTS),
        }

    @staticmethod
    def supports(exercise_key: str) -> bool:
        return exercise_key in MODEL_SPECS

    @staticmethod
    def required_landmarks(exercise_key: str) -> tuple[str, ...]:
        spec = MODEL_SPECS.get(exercise_key)
        if spec is None:
            raise ModelRegistryError("UNSUPPORTED_EXERCISE", "Exercise key is not supported")
        return spec.landmarks

    def predict(self, exercise_key: str, landmarks: Mapping[str, Landmark]) -> Prediction:
        if not self.ready:
            raise RegistryUnavailable("MODEL_UNAVAILABLE", "The model registry is not ready")
        spec = MODEL_SPECS.get(exercise_key)
        bundle = self._bundles.get(exercise_key)
        if spec is None or bundle is None:
            raise ModelRegistryError("UNSUPPORTED_EXERCISE", "Exercise key is not supported")

        missing = sorted(set(spec.landmarks) - set(landmarks))
        if missing:
            raise ModelRegistryError("MISSING_LANDMARKS", "Required landmarks are missing")

        row = [
            value
            for name in spec.landmarks
            for value in (
                landmarks[name].x,
                landmarks[name].y,
                landmarks[name].z,
                landmarks[name].visibility,
            )
        ]
        columns = [f"{name}_{suffix}" for name in spec.landmarks for suffix in ("x", "y", "z", "v")]
        raw_frame = self._frame_factory([row], columns)
        model_frame = raw_frame
        if bundle.scaler is not None:
            model_frame = self._frame_factory(bundle.scaler.transform(raw_frame), None)

        with self._prediction_lock:
            if exercise_key == "lunge":
                return self._predict_lunge(spec, bundle, model_frame)
            predicted_class, confidence = self._predict_class(bundle.model, model_frame)
            return self._normalize(spec, predicted_class, confidence)

    def _verify_artifacts(self) -> dict[str, Path]:
        try:
            manifest_text = self._manifest_path.read_text(encoding="utf-8")
        except OSError as exc:
            raise ModelRegistryError("MANIFEST_UNAVAILABLE", "The model hash manifest is unavailable") from exc

        manifest: dict[str, str] = {}
        for raw_line in manifest_text.splitlines():
            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue
            match = _MANIFEST_LINE.fullmatch(line)
            if match is None:
                raise ModelRegistryError("INVALID_MANIFEST", "The model hash manifest is invalid")
            digest, filename = match.groups()
            if filename in manifest:
                raise ModelRegistryError("INVALID_MANIFEST", "The model hash manifest contains duplicate entries")
            manifest[filename] = digest.lower()

        if set(manifest) != EXPECTED_ARTIFACTS:
            raise ModelRegistryError("INVALID_MANIFEST", "The model hash manifest does not match the pinned artifact set")

        resolved_dir = self._model_dir.resolve()
        verified: dict[str, Path] = {}
        for filename in sorted(EXPECTED_ARTIFACTS):
            path = (self._model_dir / filename).resolve()
            if path.parent != resolved_dir or not path.is_file():
                raise ModelRegistryError("ARTIFACT_UNAVAILABLE", "A pinned model artifact is unavailable")
            digest = hashlib.sha256()
            try:
                with path.open("rb") as handle:
                    for chunk in iter(lambda: handle.read(1024 * 1024), b""):
                        digest.update(chunk)
            except OSError as exc:
                raise ModelRegistryError("ARTIFACT_UNAVAILABLE", "A pinned model artifact is unavailable") from exc
            if not hmac.compare_digest(digest.hexdigest(), manifest[filename]):
                raise ModelRegistryError("HASH_MISMATCH", "A pinned model artifact failed integrity verification")
            verified[filename] = path
        return verified

    def _predict_lunge(self, spec: ModelSpec, bundle: ModelBundle, frame: Any) -> Prediction:
        if bundle.stage_model is None:
            raise ModelRegistryError("MODEL_UNAVAILABLE", "The lunge stage model is unavailable")
        stage_class, stage_confidence = self._predict_class(bundle.stage_model, frame)
        if stage_confidence < spec.threshold:
            return self._unknown(stage_class, stage_confidence)
        if stage_class != "D":
            return Prediction(
                predicted_class=stage_class,
                confidence=stage_confidence,
                correct=False,
                score=0.0,
                feedback=(
                    Feedback(
                        code="lunge_phase_not_evaluated",
                        severity="INFO",
                        message="Form is evaluated only during the down phase of the demo lunge model.",
                    ),
                ),
            )
        predicted_class, confidence = self._predict_class(bundle.model, frame)
        return self._normalize(spec, predicted_class, confidence)

    @staticmethod
    def _predict_class(model: Any, frame: Any) -> tuple[str, float]:
        predicted_class = str(model.predict(frame)[0])
        confidence = 1.0
        if hasattr(model, "predict_proba"):
            probabilities = model.predict_proba(frame)[0]
            confidence = float(max(probabilities))
        if not 0.0 <= confidence <= 1.0:
            raise ModelRegistryError("INVALID_MODEL_OUTPUT", "The model returned invalid confidence")
        return predicted_class, round(confidence, 6)

    def _normalize(self, spec: ModelSpec, predicted_class: str, confidence: float) -> Prediction:
        if confidence < spec.threshold or predicted_class not in spec.labels:
            return self._unknown(predicted_class, confidence)
        correct, code, message = spec.labels[predicted_class]
        feedback: tuple[Feedback, ...] = ()
        if not correct:
            feedback = (Feedback(code=code, severity="WARNING", message=message),)
        score_basis = confidence if correct else 1.0 - confidence
        return Prediction(
            predicted_class=predicted_class,
            confidence=confidence,
            correct=correct,
            score=round(max(0.0, min(100.0, score_basis * 100.0)), 2),
            feedback=feedback,
        )

    @staticmethod
    def _unknown(predicted_class: str, confidence: float) -> Prediction:
        return Prediction(
            predicted_class=predicted_class,
            confidence=confidence,
            correct=False,
            score=0.0,
            feedback=(
                Feedback(
                    code="low_confidence",
                    severity="INFO",
                    message="The demo model is not confident enough to evaluate this sample.",
                ),
            ),
        )
