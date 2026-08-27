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


@pytest.mark.asyncio
async def test_rag_chat_family_role():
    service = RagChatService()
    request = RagChatRequest(
        message="Vợ tôi đang mang thai 3 tháng đầu hay bị ốm nghén, tôi nên nấu những món gì bồi bổ và chăm sóc vợ thế nào?",
        stage=MaternalStage.PREGNANCY,
        user_role="FAMILY",
    )
    result = await service.chat(request)
    assert result.answer is not None
    assert len(result.answer) > 0
    assert result.has_critical_warning is False
    assert len(result.suggested_followups) > 0


@pytest.mark.asyncio
async def test_rag_chat_mother_with_survey_profile():
    service = RagChatService()
    request = RagChatRequest(
        message="Em nên ăn uống và vận động như thế nào trong tam cá nguyệt này?",
        stage=MaternalStage.PREGNANCY,
        gestational_age_weeks=24,
        user_role="MOTHER",
        survey_profile={
            "conditions": ["Tiền sử tiền sản giật nhẹ lần mang thai trước"],
            "allergies": ["Hải sản"],
        },
    )
    result = await service.chat(request)
    assert result.answer is not None
    assert len(result.answer) > 0
    assert result.has_critical_warning is False


def test_clean_latex_and_math_artifacts():
    service = RagChatService()
    raw = r"Huyết áp $\ge 140/90$ mmHg, sốt $\ge 38.5^\circ C$, đường huyết $\le 5.1$ mmol/L, $\approx 10$ ngày, $\pm 2$ tuần, $140/90$."
    cleaned = service._clean_latex_and_math_artifacts(raw)
    assert r"$\ge" not in cleaned
    assert r"\ge" not in cleaned
    assert r"$\le" not in cleaned
    assert r"\le" not in cleaned
    assert r"^\circ" not in cleaned
    assert "≥ 140/90" in cleaned
    assert "≥ 38.5°C" in cleaned
    assert "≤ 5.1" in cleaned
    assert "≈ 10" in cleaned
    assert "± 2" in cleaned

