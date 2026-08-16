"""AI Nurse Assistant RAG Chat API router."""

from __future__ import annotations

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import verify_internal_api_key
from app.models.schemas import RagChatRequest, RagChatResponse
from app.services.rag_chat_service import get_rag_chat_service

router = APIRouter(prefix="/chat", tags=["AI Nurse Assistant RAG Chat"])


@router.post(
    "/message",
    response_model=RagChatResponse,
    summary="Hỏi đáp cẩm nang y tế & Làm rõ triệu chứng với AI Nurse Assistant (Bước 10)",
)
async def chat_with_ai_nurse(
    request: RagChatRequest,
    _auth: str = Depends(verify_internal_api_key),
    db: AsyncSession = Depends(get_db),
) -> RagChatResponse:
    """Answer maternal health questions grounded in official medical documents via Gemini 3.7 Flash."""
    service = get_rag_chat_service()
    return await service.chat(request, session=db)
