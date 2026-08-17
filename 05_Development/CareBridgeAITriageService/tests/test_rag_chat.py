"""Tests for AI Nurse Assistant RAG Chat Service."""

import pytest
from app.models.schemas import (
    ChatMessage,
    MaternalStage,
    RagChatRequest,
)
from app.services.rag_chat_service import RagChatService


@pytest.mark.asyncio
async def test_rag_chat_general_question():
    service = RagChatService()
    request = RagChatRequest(
        message="Mang thai 3 tháng đầu nên bổ sung sắt và axit folic như thế nào cho đúng cách?",
        stage=MaternalStage.PREGNANCY,
        gestational_age_weeks=10,
    )
    result = await service.chat(request)
    assert result.answer is not None
    assert len(result.answer) > 0
    assert result.has_critical_warning is False
    assert len(result.suggested_followups) > 0
    assert result.disclaimer is not None


@pytest.mark.asyncio
async def test_rag_chat_detects_emergency_intent():
    service = RagChatService()
    request = RagChatRequest(
        message="Em bị ra máu âm đạo kèm đau bụng quặn dữ dội thì phải làm sao?",
        stage=MaternalStage.PREGNANCY,
        gestational_age_weeks=28,
    )
    result = await service.chat(request)
    assert result.has_critical_warning is True
    assert any("115" in fu or "Bệnh viện" in fu for fu in result.suggested_followups)


@pytest.mark.asyncio
async def test_rag_chat_multi_turn_conversation():
    service = RagChatService()
    # Turn 2: User asks follow-up with implicit reference ("Nó có nguy hiểm không?")
    request = RagChatRequest(
        message="Nó có nguy hiểm đến em bé không ạ?",
        stage=MaternalStage.PREGNANCY,
        gestational_age_weeks=32,
        conversation_history=[
            ChatMessage(role="user", content="Em đang mang thai 32 tuần, hôm nay thấy bị đau đầu và phù hai chân"),
            ChatMessage(role="assistant", content="Chào mẹ, đau đầu và phù chân ở tuần 32 là dấu hiệu cần được theo dõi kỹ vì có thể liên quan đến tăng huyết áp thai kỳ."),
        ],
    )
    result = await service.chat(request)
    assert result.answer is not None
    assert len(result.answer) > 0
    # Should retrieve preeclampsia or blood pressure related sources thanks to multi-turn query expansion
    assert len(result.sources) > 0
