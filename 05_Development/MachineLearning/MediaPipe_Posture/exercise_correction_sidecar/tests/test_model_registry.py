from __future__ import annotations

import hashlib
from pathlib import Path
from typing import Any

import pytest

from app.contracts import Landmark
from app.model_registry import EXPECTED_ARTIFACTS, MODEL_SPECS, ModelRegistry, ModelRegistryError


class FakeScaler:
    def transform(self, frame: Any) -> Any:
        return frame


class FakeModel:
    def __init__(self, predicted_class: str, confidence: float = 0.99):
        self.predicted_class = predicted_class
        self.confidence = confidence

    def predict(self, _: Any) -> list[str]:
        return [self.predicted_class]

    def predict_proba(self, _: Any) -> list[list[float]]:
        return [[1.0 - self.confidence, self.confidence]]


def _write_artifacts(model_dir: Path) -> Path:
    model_dir.mkdir()
    lines = []
    for name in sorted(EXPECTED_ARTIFACTS):
        content = f"trusted-test-artifact:{name}".encode()
        (model_dir / name).write_bytes(content)
        lines.append(f"{hashlib.sha256(content).hexdigest()}  {name}")
    manifest = model_dir / "SHA256SUMS"
    manifest.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return manifest


def _loader(path: Path) -> Any:
    if "scaler" in path.name:
        return FakeScaler()
    classes = {
        "bicep_curl_model.pkl": "C",
        "plank_model.pkl": "C",
        "squat_model.pkl": "up",
        "lunge_stage_model.pkl": "D",
        "lunge_err_model.pkl": "C",
    }
    return FakeModel(classes[path.name])


def _frame_factory(data: Any, _: list[str] | None) -> Any:
    return data


def _landmarks(exercise_key: str) -> dict[str, Landmark]:
    return {
        name: Landmark(x=0.5, y=0.5, z=0.0, visibility=0.99)
        for name in MODEL_SPECS[exercise_key].landmarks
    }


def test_registry_verifies_all_artifacts_then_loads_once(tmp_path: Path) -> None:
    model_dir = tmp_path / "models"
    manifest = _write_artifacts(model_dir)
    loaded: list[str] = []

    def recording_loader(path: Path) -> Any:
        loaded.append(path.name)
        return _loader(path)

    registry = ModelRegistry(
        model_dir,
        manifest,
        pickle_loader=recording_loader,
        frame_factory=_frame_factory,
    )
    registry.load()

    assert registry.ready is True
    assert sorted(loaded) == sorted(EXPECTED_ARTIFACTS)
    assert registry.health_snapshot()["artifactCount"] == 8
    for exercise_key in MODEL_SPECS:
        prediction = registry.predict(exercise_key, _landmarks(exercise_key))
        assert prediction.correct is True
        assert prediction.confidence == 0.99


def test_hash_mismatch_prevents_any_pickle_load(tmp_path: Path) -> None:
    model_dir = tmp_path / "models"
    manifest = _write_artifacts(model_dir)
    (model_dir / "plank_model.pkl").write_bytes(b"tampered")
    calls = 0

    def forbidden_loader(_: Path) -> Any:
        nonlocal calls
        calls += 1
        raise AssertionError("pickle loader must not run before every hash is verified")

    registry = ModelRegistry(model_dir, manifest, pickle_loader=forbidden_loader)
    with pytest.raises(ModelRegistryError) as error:
        registry.load()

    assert error.value.code == "HASH_MISMATCH"
    assert registry.ready is False
    assert calls == 0


def test_manifest_must_describe_exact_pinned_set(tmp_path: Path) -> None:
    model_dir = tmp_path / "models"
    manifest = _write_artifacts(model_dir)
    lines = manifest.read_text(encoding="utf-8").splitlines()
    manifest.write_text("\n".join(lines[:-1]) + "\n", encoding="utf-8")
    registry = ModelRegistry(model_dir, manifest, pickle_loader=_loader)

    with pytest.raises(ModelRegistryError) as error:
        registry.load()

    assert error.value.code == "INVALID_MANIFEST"
    assert registry.ready is False

