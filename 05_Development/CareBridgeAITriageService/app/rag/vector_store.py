"""Vector store operations using PostgreSQL pgvector with in-memory fallback."""

from __future__ import annotations

import logging
from typing import Any, Dict, List, Optional
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import AsyncSessionLocal
from app.models.db_models import MaternalKnowledgeChunk
from app.models.schemas import DocumentChunkDTO, SourceCitation
from app.rag.embedder import get_embedder

logger = logging.getLogger(__name__)


class PgVectorStore:
    def __init__(self) -> None:
        self.embedder = get_embedder()
        # In-memory cache fallback for fast local testing
        self._local_cache: List[Dict[str, Any]] = []

    async def add_chunks(
        self,
        chunks: List[DocumentChunkDTO],
        session: Optional[AsyncSession] = None,
    ) -> int:
        """Embed and insert chunks into PostgreSQL pgvector."""
        if not chunks:
            return 0

        texts = [c.content for c in chunks]
        embeddings = await self.embedder.embed_documents(texts)

        # Store in local memory cache as well
        for chunk, emb in zip(chunks, embeddings):
            self._local_cache.append({
                "title": chunk.title,
                "stage": chunk.stage,
                "topic": chunk.topic,
                "source": chunk.source,
                "section": chunk.section,
                "content": chunk.content,
                "embedding": emb,
            })

        # Insert into Database if session/db is available
        async def _insert_to_db(s: AsyncSession) -> int:
            count = 0
            for chunk, emb in zip(chunks, embeddings):
                db_chunk = MaternalKnowledgeChunk(
                    title=chunk.title,
                    stage=chunk.stage,
                    topic=chunk.topic,
                    source=chunk.source,
                    section=chunk.section,
                    content=chunk.content,
                    chunk_index=chunk.chunk_index,
                    embedding=emb,
                )
                s.add(db_chunk)
                count += 1
            await s.commit()
            return count

        if session is not None:
            try:
                return await _insert_to_db(session)
            except Exception as e:
                logger.warning(f"Failed to insert chunks to PostgreSQL via provided session: {e}")
                return len(chunks)
        else:
            try:
                async with AsyncSessionLocal() as db:
                    return await _insert_to_db(db)
            except Exception as e:
                logger.warning(f"PostgreSQL not reachable for insert, saved to in-memory store: {e}")
                return len(chunks)

    async def similarity_search(
        self,
        query: str,
        stage: Optional[str] = None,
        topic: Optional[str] = None,
        top_k: int = 4,
        session: Optional[AsyncSession] = None,
    ) -> List[Dict[str, Any]]:
        """Find the top-K most similar knowledge chunks to the query vector."""
        query_vector = await self.embedder.embed_query(query)

        # Attempt PostgreSQL pgvector search
        async def _search_db(s: AsyncSession) -> List[Dict[str, Any]]:
            # pgvector cosine distance: embedding.cosine_distance(query_vector)
            stmt = select(
                MaternalKnowledgeChunk,
                MaternalKnowledgeChunk.embedding.cosine_distance(query_vector).label("distance"),
            )
            if stage and stage != "ALL":
                stmt = stmt.where(MaternalKnowledgeChunk.stage.in_([stage, "ALL"]))
            if topic:
                stmt = stmt.where(MaternalKnowledgeChunk.topic == topic)

            stmt = stmt.order_by("distance").limit(top_k)
            result = await s.execute(stmt)
            rows = result.all()

            results = []
            for chunk, distance in rows:
                similarity = 1.0 - float(distance) if distance is not None else 0.0
                results.append({
                    "id": chunk.id,
                    "title": chunk.title,
                    "stage": chunk.stage,
                    "topic": chunk.topic,
                    "source": chunk.source,
                    "section": chunk.section,
                    "content": chunk.content,
                    "similarity": similarity,
                })
            return results

        if session is not None:
            try:
                db_results = await _search_db(session)
                if db_results:
                    return db_results
            except Exception as e:
                logger.debug(f"pgvector query error on provided session: {e}")
        else:
            try:
                async with AsyncSessionLocal() as db:
                    db_results = await _search_db(db)
                    if db_results:
                        return db_results
            except Exception as e:
                logger.debug(f"PostgreSQL pgvector query unavailable ({e}), using in-memory cache")

        # Fallback in-memory Cosine Similarity
        return self._in_memory_search(query_vector, stage, top_k)

    def _in_memory_search(
        self,
        query_vector: List[float],
        stage: Optional[str] = None,
        top_k: int = 4,
    ) -> List[Dict[str, Any]]:
        if not self._local_cache:
            return []

        scored = []
        for item in self._local_cache:
            if stage and stage != "ALL" and item["stage"] not in (stage, "ALL"):
                continue

            doc_vector = item.get("embedding") or []
            # Calculate cosine similarity
            dot = sum(a * b for a, b in zip(query_vector, doc_vector))
            norm_q = sum(a * a for a in query_vector) ** 0.5 or 1.0
            norm_d = sum(b * b for b in doc_vector) ** 0.5 or 1.0
            sim = dot / (norm_q * norm_d)
            scored.append((item, sim))

        scored.sort(key=lambda x: -x[1])
        return [
            {
                "title": doc["title"],
                "stage": doc["stage"],
                "topic": doc["topic"],
                "source": doc["source"],
                "section": doc.get("section"),
                "content": doc["content"],
                "similarity": sim,
            }
            for doc, sim in scored[:top_k]
        ]


vector_store = PgVectorStore()


def get_vector_store() -> PgVectorStore:
    return vector_store
