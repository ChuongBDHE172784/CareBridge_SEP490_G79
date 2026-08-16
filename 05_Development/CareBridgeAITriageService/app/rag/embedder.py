"""Embedding generator wrapping Google Gemini text-embedding-004."""

from __future__ import annotations

import logging
from typing import List
from app.core.gemini import get_gemini_client

logger = logging.getLogger(__name__)


class TextEmbedder:
    def __init__(self) -> None:
        self.client = get_gemini_client()

    async def embed_query(self, query: str) -> List[float]:
        """Embed a user query into a 768-dim float vector."""
        return await self.client.embed_text(query)

    async def embed_documents(self, texts: List[str]) -> List[List[float]]:
        """Embed a batch of document texts into 768-dim float vectors."""
        return await self.client.embed_texts(texts)


embedder = TextEmbedder()


def get_embedder() -> TextEmbedder:
    return embedder
