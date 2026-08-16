"""Vector store operations using PostgreSQL pgvector with in-memory fallback."""

from __future__ import annotations

import logging
from typing import Any, Dict, List, Optional
from sqlalchemy import delete, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import RAW_DOCS_DIR
from app.core.database import AsyncSessionLocal
from app.models.db_models import MaternalKnowledgeChunk
from app.models.schemas import (
    ChunkDetailItem,
    DocumentChunkDTO,
    KnowledgeListResponse,
    KnowledgeStatsResponse,
    SourceCitation,
)
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
        for i, (chunk, emb) in enumerate(zip(chunks, embeddings)):
            self._local_cache.append({
                "id": len(self._local_cache) + 1,
                "title": chunk.title,
                "stage": chunk.stage,
                "topic": chunk.topic,
                "source": chunk.source,
                "section": chunk.section,
                "content": chunk.content,
                "chunk_index": chunk.chunk_index,
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

    async def delete_by_title(
        self,
        title: str,
        session: Optional[AsyncSession] = None,
    ) -> int:
        """Delete all chunks belonging to a document title or filename."""
        prev_len = len(self._local_cache)
        self._local_cache = [c for c in self._local_cache if title.lower() not in c["title"].lower()]
        deleted_count = prev_len - len(self._local_cache)

        async def _delete_from_db(s: AsyncSession) -> int:
            stmt = delete(MaternalKnowledgeChunk).where(
                MaternalKnowledgeChunk.title.ilike(f"%{title}%")
            )
            result = await s.execute(stmt)
            await s.commit()
            return result.rowcount or 0

        if session is not None:
            try:
                db_deleted = await _delete_from_db(session)
                return max(deleted_count, db_deleted)
            except Exception as e:
                logger.warning(f"Database delete notice: {e}")
        else:
            try:
                async with AsyncSessionLocal() as db:
                    db_deleted = await _delete_from_db(db)
                    return max(deleted_count, db_deleted)
            except Exception as e:
                logger.warning(f"Database delete notice: {e}")

        return deleted_count

    async def clear_all(
        self,
        session: Optional[AsyncSession] = None,
    ) -> int:
        """Delete all knowledge chunks from the entire database and memory cache."""
        count = len(self._local_cache)
        self._local_cache.clear()

        async def _clear_db(s: AsyncSession) -> int:
            stmt = delete(MaternalKnowledgeChunk)
            result = await s.execute(stmt)
            await s.commit()
            return result.rowcount or 0

        if session is not None:
            try:
                db_cleared = await _clear_db(session)
                return max(count, db_cleared)
            except Exception as e:
                logger.warning(f"Database clear notice: {e}")
        else:
            try:
                async with AsyncSessionLocal() as db:
                    db_cleared = await _clear_db(db)
                    return max(count, db_cleared)
            except Exception as e:
                logger.warning(f"Database clear notice: {e}")

        return count

    async def list_chunks(
        self,
        stage: Optional[str] = None,
        topic: Optional[str] = None,
        keyword: Optional[str] = None,
        page: int = 1,
        page_size: int = 20,
        session: Optional[AsyncSession] = None,
    ) -> KnowledgeListResponse:
        """List chunks with pagination and filtering."""
        offset = (page - 1) * page_size

        async def _list_from_db(s: AsyncSession) -> Optional[KnowledgeListResponse]:
            # Count query
            count_stmt = select(func.count(MaternalKnowledgeChunk.id))
            if stage and stage != "ALL":
                count_stmt = count_stmt.where(MaternalKnowledgeChunk.stage.in_([stage, "ALL"]))
            if topic:
                count_stmt = count_stmt.where(MaternalKnowledgeChunk.topic == topic)
            if keyword:
                count_stmt = count_stmt.where(
                    MaternalKnowledgeChunk.title.ilike(f"%{keyword}%")
                    | MaternalKnowledgeChunk.content.ilike(f"%{keyword}%")
                )

            total_res = await s.execute(count_stmt)
            total = total_res.scalar() or 0

            # Items query
            stmt = select(MaternalKnowledgeChunk)
            if stage and stage != "ALL":
                stmt = stmt.where(MaternalKnowledgeChunk.stage.in_([stage, "ALL"]))
            if topic:
                stmt = stmt.where(MaternalKnowledgeChunk.topic == topic)
            if keyword:
                stmt = stmt.where(
                    MaternalKnowledgeChunk.title.ilike(f"%{keyword}%")
                    | MaternalKnowledgeChunk.content.ilike(f"%{keyword}%")
                )

            stmt = stmt.order_by(MaternalKnowledgeChunk.id.desc()).offset(offset).limit(page_size)
            result = await s.execute(stmt)
            chunks = result.scalars().all()

            items = [
                ChunkDetailItem(
                    id=c.id,
                    title=c.title,
                    stage=c.stage,
                    topic=c.topic,
                    source=c.source,
                    section=c.section,
                    snippet=c.content[:200] + "..." if len(c.content) > 200 else c.content,
                    content_length=len(c.content),
                    chunk_index=c.chunk_index,
                )
                for c in chunks
            ]
            return KnowledgeListResponse(total=total, page=page, page_size=page_size, items=items)

        if session is not None:
            try:
                db_res = await _list_from_db(session)
                if db_res:
                    return db_res
            except Exception as e:
                logger.debug(f"DB list notice: {e}")
        else:
            try:
                async with AsyncSessionLocal() as db:
                    db_res = await _list_from_db(db)
                    if db_res:
                        return db_res
            except Exception as e:
                logger.debug(f"DB list notice: {e}")

        # Fallback in-memory list
        filtered = self._local_cache
        if stage and stage != "ALL":
            filtered = [c for c in filtered if c["stage"] in (stage, "ALL")]
        if topic:
            filtered = [c for c in filtered if c["topic"] == topic]
        if keyword:
            k = keyword.lower()
            filtered = [c for c in filtered if k in c["title"].lower() or k in c["content"].lower()]

        total = len(filtered)
        paginated = filtered[offset : offset + page_size]
        items = [
            ChunkDetailItem(
                id=c.get("id", idx + 1),
                title=c["title"],
                stage=c["stage"],
                topic=c["topic"],
                source=c["source"],
                section=c.get("section"),
                snippet=c["content"][:200] + "..." if len(c["content"]) > 200 else c["content"],
                content_length=len(c["content"]),
                chunk_index=c.get("chunk_index", 0),
            )
            for idx, c in enumerate(paginated)
        ]
        return KnowledgeListResponse(total=total, page=page, page_size=page_size, items=items)

    async def get_stats(self, session: Optional[AsyncSession] = None) -> KnowledgeStatsResponse:
        """Get statistics about knowledge chunks and sources."""
        files = [f.name for f in RAW_DOCS_DIR.glob("*") if f.is_file() and not f.name.startswith(".")]

        stage_dist: Dict[str, int] = {}
        topic_dist: Dict[str, int] = {}
        total_chunks = len(self._local_cache)
        doc_titles = set(c["title"] for c in self._local_cache)

        for c in self._local_cache:
            stage_dist[c["stage"]] = stage_dist.get(c["stage"], 0) + 1
            topic_dist[c["topic"]] = topic_dist.get(c["topic"], 0) + 1

        return KnowledgeStatsResponse(
            total_chunks=total_chunks,
            total_documents=len(doc_titles) or len(files),
            stage_distribution=stage_dist,
            topic_distribution=topic_dist,
            files_in_disk=files,
        )

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

        async def _search_db(s: AsyncSession) -> List[Dict[str, Any]]:
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
