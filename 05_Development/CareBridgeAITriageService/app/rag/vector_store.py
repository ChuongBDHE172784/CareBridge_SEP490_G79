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
        """Find the top-K most similar knowledge chunks using Hybrid Search (Dense Vector + Sparse Keyword Re-ranking)."""
        import re
        from sqlalchemy import or_

        query_vector = await self.embedder.embed_query(query)
        stopwords = {
            "là", "và", "của", "cho", "các", "những", "được", "có", "trong",
            "để", "khi", "ở", "gì", "thế", "nào", "ạ", "nhé", "với", "từ",
            "ra", "vào", "thì", "cần", "nên", "hãy", "bị", "do", "về",
            "cách", "theo", "dõi", "tại", "nhà", "cho", "làm", "sao",
        }
        clean_words = [
            w for w in re.sub(r"[^\w\s]", " ", query.lower()).split()
            if w not in stopwords and (len(w) > 1 or w.isdigit())
        ]

        # Extract 2-gram and 3-gram meaningful clinical phrases
        raw_words = re.sub(r"[^\w\s]", " ", query.lower()).split()
        phrases = []
        for i in range(len(raw_words) - 1):
            phrases.append(f"{raw_words[i]} {raw_words[i+1]}")
        for i in range(len(raw_words) - 2):
            phrases.append(f"{raw_words[i]} {raw_words[i+1]} {raw_words[i+2]}")

        meaningful_phrases = [
            p for p in phrases
            if any(k in p for k in [
                "cử động", "thai máy", "đếm thai", "huyết áp", "đường huyết", "sinh hiệu",
                "tiền sản giật", "ra máu", "sốt", "đau đầu", "vi chất", "axit folic",
                "canxi", "siêu âm", "khám thai", "dinh dưỡng", "nghén", "vỡ ối"
            ])
        ]

        async def _search_db(s: AsyncSession) -> List[Dict[str, Any]]:
            # 1. Query Top Dense Vector candidates
            stmt_vec = select(
                MaternalKnowledgeChunk,
                MaternalKnowledgeChunk.embedding.cosine_distance(query_vector).label("distance"),
            )
            if stage and stage != "ALL":
                stmt_vec = stmt_vec.where(MaternalKnowledgeChunk.stage.in_([stage, "ALL"]))
            if topic:
                stmt_vec = stmt_vec.where(MaternalKnowledgeChunk.topic == topic)

            stmt_vec = stmt_vec.order_by("distance").limit(40)
            res_vec = await s.execute(stmt_vec)
            
            candidates: Dict[int, tuple[MaternalKnowledgeChunk, float]] = {}
            for chunk, dist in res_vec.all():
                vec_sim = 1.0 - float(dist) if dist is not None else 0.0
                candidates[chunk.id] = (chunk, vec_sim)

            # 2. Query Sparse Keyword & Phrase candidates
            search_terms = meaningful_phrases if meaningful_phrases else [w for w in clean_words if len(w) >= 3]
            if search_terms:
                kw_filters = [
                    MaternalKnowledgeChunk.content.ilike(f"%{t}%") | MaternalKnowledgeChunk.title.ilike(f"%{t}%")
                    for t in search_terms
                ]
                stmt_kw = select(MaternalKnowledgeChunk).where(or_(*kw_filters))
                if stage and stage != "ALL":
                    stmt_kw = stmt_kw.where(MaternalKnowledgeChunk.stage.in_([stage, "ALL"]))
                if topic:
                    stmt_kw = stmt_kw.where(MaternalKnowledgeChunk.topic == topic)
                stmt_kw = stmt_kw.limit(40)
                res_kw = await s.execute(stmt_kw)
                for chunk in res_kw.scalars():
                    if chunk.id not in candidates:
                        candidates[chunk.id] = (chunk, 0.0)

            # 3. Compute Hybrid Re-ranking Score
            scored = []
            for chunk_id, (chunk, vec_sim) in candidates.items():
                content_lower = chunk.content.lower()
                title_lower = chunk.title.lower()
                
                kw_hits = sum(1 for w in clean_words if w in content_lower or w in title_lower)
                kw_ratio = kw_hits / max(len(clean_words), 1)

                phrase_boost = 0.0
                for p in meaningful_phrases:
                    if p in content_lower or p in title_lower:
                        phrase_boost += 0.45

                hybrid_score = (vec_sim * 0.35) + (kw_ratio * 0.30) + phrase_boost
                scored.append((chunk, hybrid_score))

            scored.sort(key=lambda x: x[1], reverse=True)

            results = []
            seen_sections = set()
            for chunk, h_score in scored:
                sec_key = f"{chunk.title}_{chunk.section}"
                if sec_key in seen_sections:
                    continue
                seen_sections.add(sec_key)

                results.append({
                    "id": chunk.id,
                    "title": chunk.title,
                    "stage": chunk.stage,
                    "topic": chunk.topic,
                    "source": chunk.source,
                    "section": chunk.section,
                    "content": chunk.content,
                    "similarity": h_score,
                })
                if len(results) >= top_k:
                    break

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
