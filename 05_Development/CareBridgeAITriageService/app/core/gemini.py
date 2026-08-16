"""Google GenAI Client for Gemini 3.7 Flash & gemini-embedding-001."""

from __future__ import annotations

import logging
from typing import Optional
from google import genai
from google.genai import types
from app.config import GEMINI_SETTINGS

logger = logging.getLogger(__name__)


class GeminiClient:
    def __init__(self) -> None:
        self._client: Optional[genai.Client] = None
        if GEMINI_SETTINGS.enabled and GEMINI_SETTINGS.api_key:
            try:
                self._client = genai.Client(api_key=GEMINI_SETTINGS.api_key)
                logger.info(
                    f"Gemini client initialized with model={GEMINI_SETTINGS.model} "
                    f"embedding_model={GEMINI_SETTINGS.embedding_model}"
                )
            except Exception as e:
                logger.warning(f"Failed to initialize live Gemini Client: {e}")
                self._client = None
        else:
            logger.info("Gemini client running in offline/mock mode (no API key configured)")

    @property
    def is_available(self) -> bool:
        return self._client is not None

    async def embed_text(self, text: str) -> list[float]:
        """Generate a 768-dimensional vector embedding for a single text chunk."""
        if not text.strip():
            return [0.0] * GEMINI_SETTINGS.embedding_dimension

        if self._client:
            try:
                response = self._client.models.embed_content(
                    model=GEMINI_SETTINGS.embedding_model,
                    contents=text,
                    config=types.EmbedContentConfig(
                        output_dimensionality=GEMINI_SETTINGS.embedding_dimension
                    ),
                )
                if hasattr(response, "embeddings") and response.embeddings:
                    return response.embeddings[0].values
                if hasattr(response, "embedding") and response.embedding:
                    return response.embedding.values
            except Exception as e:
                logger.warning(f"Gemini embedding API notice ({e}), using deterministic embedding fallback")

        # Fallback deterministic pseudo-embedding for testing without live API key
        return self._mock_embedding(text)

    async def embed_texts(self, texts: list[str]) -> list[list[float]]:
        """Generate vector embeddings for a list of text chunks."""
        if not texts:
            return []

        if self._client:
            try:
                embeddings = []
                # Process in batches
                for i in range(0, len(texts), 16):
                    batch = texts[i : i + 16]
                    response = self._client.models.embed_content(
                        model=GEMINI_SETTINGS.embedding_model,
                        contents=batch,
                        config=types.EmbedContentConfig(
                            output_dimensionality=GEMINI_SETTINGS.embedding_dimension
                        ),
                    )
                    if hasattr(response, "embeddings") and response.embeddings:
                        embeddings.extend([e.values for e in response.embeddings])
                    elif hasattr(response, "embedding") and response.embedding:
                        embeddings.append(response.embedding.values)
                if len(embeddings) == len(texts):
                    return embeddings
            except Exception as e:
                logger.warning(f"Gemini batch embedding notice ({e}), using deterministic embedding fallback")

        return [self._mock_embedding(t) for t in texts]

    async def generate_response(
        self,
        prompt: str,
        system_instruction: str,
        temperature: float | None = None,
    ) -> str:
        """Generate a structured response using Gemini 3.7 Flash."""
        temp = temperature if temperature is not None else GEMINI_SETTINGS.temperature

        if self._client:
            try:
                response = self._client.models.generate_content(
                    model=GEMINI_SETTINGS.model,
                    contents=prompt,
                    config=types.GenerateContentConfig(
                        system_instruction=system_instruction,
                        temperature=temp,
                    ),
                )
                if response and response.text:
                    return response.text.strip()
            except Exception as e:
                logger.warning(f"Gemini generation notice ({e}), using fallback response generator")

        # Fallback for testing / offline
        return (
            "Chào mẹ, CareBridge AI Nurse Assistant xin được giải đáp: Dựa trên cẩm nang y tế thai kỳ chính thống, "
            "mẹ cần chú ý theo dõi kỹ các thay đổi sinh lý, bổ sung đầy đủ vi chất (sắt, canxi, axit folic), "
            "nghỉ ngơi hợp lý và tái khám định kỳ theo chỉ định của Bác sĩ chuyên khoa sản."
        )

    def _mock_embedding(self, text: str) -> list[float]:
        """Generate a deterministic normalized 768-dim vector for testing without external API."""
        import hashlib
        import math

        h = hashlib.sha256(text.encode("utf-8")).digest()
        vector = []
        for i in range(GEMINI_SETTINGS.embedding_dimension):
            byte_val = h[i % len(h)]
            val = (byte_val - 128) / 128.0 + math.sin(i * 0.1)
            vector.append(val)

        # Normalize vector
        norm = math.sqrt(sum(x * x for x in vector)) or 1.0
        return [x / norm for x in vector]


# Global singleton client
gemini_client = GeminiClient()


def get_gemini_client() -> GeminiClient:
    return gemini_client
