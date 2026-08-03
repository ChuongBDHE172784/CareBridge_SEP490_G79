from pathlib import Path

import pytest

pytest.importorskip("sklearn")
pytest.importorskip("pandas")

from app.contracts import Landmark
from app.model_registry import MODEL_SPECS, ModelRegistry


MODEL_DIR = Path(__file__).resolve().parents[1] / "models"


@pytest.fixture(scope="module")
def registry() -> ModelRegistry:
    loaded = ModelRegistry(MODEL_DIR, MODEL_DIR / "SHA256SUMS")
    loaded.load()
    return loaded


@pytest.mark.parametrize("exercise_key", sorted(MODEL_SPECS))
def test_vendored_model_accepts_its_pinned_feature_schema(
    registry: ModelRegistry,
    exercise_key: str,
) -> None:
    landmarks = {
        name: Landmark(x=0.5, y=0.5, z=0.0, visibility=0.99)
        for name in MODEL_SPECS[exercise_key].landmarks
    }

    result = registry.predict(exercise_key, landmarks)

    assert result.predicted_class
    assert 0.0 <= result.confidence <= 1.0
    assert 0.0 <= result.score <= 100.0
