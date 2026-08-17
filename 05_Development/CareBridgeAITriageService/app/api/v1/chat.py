"""AI Nurse Assistant RAG Chat API router."""

from __future__ import annotations

import time
from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import GEMINI_SETTINGS
from app.core.database import get_db
from app.core.gemini import FALLBACK_GENERATION_MODELS, get_gemini_client
from app.core.security import verify_internal_api_key
from app.models.schemas import (
    CustomPromptTestRequest,
    CustomPromptTestResponse,
    RagChatRequest,
    RagChatResponse,
)
from app.services.rag_chat_service import get_rag_chat_service

router = APIRouter(prefix="/chat", tags=["AI Nurse Assistant RAG Chat & Prompt Tuning"])


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
    """Answer maternal health questions grounded in official medical documents via Gemini Flash."""
    service = get_rag_chat_service()
    return await service.chat(request, session=db)


@router.post(
    "/test-prompt",
    response_model=CustomPromptTestResponse,
    summary="[Prompt Playground] Thử nghiệm ngay Prompt / Model / Temperature tùy chỉnh",
)
async def test_custom_prompt(
    request: CustomPromptTestRequest,
    _auth: str = Depends(verify_internal_api_key),
) -> CustomPromptTestResponse:
    """Directly test prompt instructions against Gemini without RAG context injection."""
    gemini = get_gemini_client()
    system_inst = request.system_instruction or (
        "Bạn là CareBridge AI Nurse Assistant. Hãy trả lời ngắn gọn, ân cần, giải thích nguyên nhân sinh lý thai kỳ."
    )
    model_to_use = request.model or GEMINI_SETTINGS.model

    answer = await gemini.generate_response(
        prompt=request.user_message,
        system_instruction=system_inst,
        temperature=request.temperature or 0.3,
    )

    return CustomPromptTestResponse(
        model_used=model_to_use,
        temperature=request.temperature or 0.3,
        answer=answer,
    )


@router.get(
    "/models",
    summary="[Admin] Xem danh sách các AI Models đang được cấu hình",
)
async def list_available_models(
    _auth: str = Depends(verify_internal_api_key),
) -> dict:
    """Return currently active primary model, embedding model, and fallback chain."""
    return {
        "active_primary_model": GEMINI_SETTINGS.model,
        "active_embedding_model": GEMINI_SETTINGS.embedding_model,
        "embedding_dimension": GEMINI_SETTINGS.embedding_dimension,
        "auto_fallback_models": FALLBACK_GENERATION_MODELS,
        "status": "READY",
    }
