from __future__ import annotations

import logging
import os
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Any

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.contracts import ErrorResponse, InferenceRequest, InferenceResponse
from app.model_registry import ModelRegistry, ModelRegistryError


LOGGER = logging.getLogger("exercise_correction_sidecar")


def _default_registry() -> ModelRegistry:
    model_dir = Path(os.environ.get("EXERCISE_MODEL_DIR", "/app/models"))
    manifest_path = Path(os.environ.get("EXERCISE_MODEL_MANIFEST", str(model_dir / "SHA256SUMS")))
    return ModelRegistry(model_dir=model_dir, manifest_path=manifest_path)


def _error(status_code: int, code: str, message: str) -> JSONResponse:
    return JSONResponse(status_code=status_code, content=ErrorResponse(code=code, message=message).model_dump())


def create_app(registry: ModelRegistry | None = None) -> FastAPI:
    model_registry = registry or _default_registry()

    @asynccontextmanager
    async def lifespan(application: FastAPI):
        try:
            model_registry.load()
        except ModelRegistryError as exc:
            # Never log model payloads or request landmarks. The stable code is sufficient.
            LOGGER.error("Exercise-Correction registry startup failed: %s", exc.code)
        application.state.model_registry = model_registry
        yield

    application = FastAPI(
        title="CareBridge Exercise-Correction Sidecar",
        version="1.0.0",
        docs_url=None,
        redoc_url=None,
        openapi_url=None,
        lifespan=lifespan,
    )

    @application.exception_handler(RequestValidationError)
    async def validation_error_handler(_: Request, __: RequestValidationError) -> JSONResponse:
        return _error(422, "INVALID_INPUT", "The inference request is invalid")

    @application.get("/health")
    def health() -> JSONResponse:
        snapshot = model_registry.health_snapshot()
        return JSONResponse(status_code=200 if snapshot["ready"] else 503, content=snapshot)

    @application.post(
        "/v1/inference/landmarks",
        response_model=InferenceResponse,
        responses={
            400: {"model": ErrorResponse},
            422: {"model": ErrorResponse},
            503: {"model": ErrorResponse},
        },
    )
    def infer(payload: InferenceRequest) -> InferenceResponse | JSONResponse:
        if not model_registry.ready:
            return _error(503, "MODEL_UNAVAILABLE", "Exercise-Correction models are unavailable")
        if not model_registry.supports(payload.exerciseKey):
            return _error(400, "UNSUPPORTED_EXERCISE", "Exercise key is not supported")
        required = set(model_registry.required_landmarks(payload.exerciseKey))
        if not required.issubset(payload.landmarks):
            return _error(422, "MISSING_LANDMARKS", "Required landmarks are missing")
        try:
            prediction = model_registry.predict(payload.exerciseKey, payload.landmarks)
        except ModelRegistryError as exc:
            status = 400 if exc.code == "UNSUPPORTED_EXERCISE" else 503
            public_code = exc.code if status == 400 else "MODEL_UNAVAILABLE"
            LOGGER.warning("Exercise-Correction inference failed: %s", exc.code)
            return _error(status, public_code, "Exercise-Correction inference is unavailable")
        except Exception:
            LOGGER.error("Exercise-Correction inference failed: INTERNAL_ERROR")
            return _error(503, "MODEL_UNAVAILABLE", "Exercise-Correction inference is unavailable")
        return InferenceResponse(
            exerciseKey=payload.exerciseKey,
            sequenceNumber=payload.sequenceNumber,
            inferenceStreamId=payload.inferenceStreamId,
            predictedClass=prediction.predicted_class,
            confidence=prediction.confidence,
            correct=prediction.correct,
            score=prediction.score,
            feedback=list(prediction.feedback),
        )

    return application


app = create_app()
