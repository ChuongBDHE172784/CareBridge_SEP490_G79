"""Health check and system readiness router."""

from __future__ import annotations

from fastapi import APIRouter
from sqlalchemy import select, func

from app.config import GEMINI_SETTINGS
from app.core.database import AsyncSessionLocal
from app.core.gemini import get_gemini_client
from app.models.db_models import MaternalKnowledgeChunk
from app.models.schemas import SystemHealthResponse
from app.rag.vector_store import get_vector_store

router = APIRouter(tags=["Health & Monitoring"])


@router.get(
    "/health",
    response_model=SystemHealthResponse,
    summary="Kiểm tra trạng thái hoạt động của hệ thống AI RAG và CSDL pgvector",
)
async def check_health() -> SystemHealthResponse:
    """Return health status, model information, database connection, and knowledge chunk counts."""
    gemini = get_gemini_client()
    db_connected = False
    total_chunks = 0

    try:
        async with AsyncSessionLocal() as session:
            stmt = select(func.count(MaternalKnowledgeChunk.id))
            result = await session.execute(stmt)
            count = result.scalar()
            total_chunks = count or 0
            db_connected = True
    except Exception:
        # If DB is not connected, report local in-memory chunks
        store = get_vector_store()
        total_chunks = len(store._local_cache)
        db_connected = False

    return SystemHealthResponse(
        status="UP",
        model=GEMINI_SETTINGS.model,
        embedding_model=GEMINI_SETTINGS.embedding_model,
        database_connected=db_connected,
        total_knowledge_chunks=total_chunks,
        gemini_api_configured=bool(GEMINI_SETTINGS.api_key),
    )
