"""AI Nurse Assistant RAG Chat Service (Step 10 in Workflow)."""

from __future__ import annotations

import logging
from typing import List
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import MEDICAL_DISCLAIMER
from app.core.gemini import get_gemini_client
from app.models.schemas import (
    RagChatRequest,
    RagChatResponse,
    SourceCitation,
)
from app.rag.prompts import (
    NURSE_ASSISTANT_SYSTEM_PROMPT,
    build_rag_chat_prompt,
)
from app.rag.vector_store import get_vector_store

logger = logging.getLogger(__name__)


class RagChatService:
    def __init__(self) -> None:
        self.gemini = get_gemini_client()
        self.vector_store = get_vector_store()

    async def chat(
        self,
        request: RagChatRequest,
        session: AsyncSession | None = None,
    ) -> RagChatResponse:
        """Process user message, retrieve relevant medical chunks, and generate grounded answer via Gemini Flash."""
        # 1. Build Context-Aware Vector Search Query
        # If user refers to previous context (e.g., "nó có nguy hiểm không?"), merge with recent symptoms
        search_query = request.message
        if request.conversation_history:
            recent_user_turns = [
                msg.content
                for msg in request.conversation_history[-4:]
                if msg.role.lower() in ("user", "human")
            ]
            if recent_user_turns:
                # Merge recent user mentions with current message for richer vector recall
                search_query = f"{' '.join(recent_user_turns)} {request.message}"

        # 2. Semantic Search across Maternal Knowledge pgvector
        stage_filter = request.stage.value if request.stage else "PREGNANCY"
        retrieved_chunks = await self.vector_store.similarity_search(
            query=search_query,
            stage=stage_filter,
            top_k=4,
            session=session,
        )

        # 3. Check for Critical Emergency Flags in the user's message & recent turns
        combined_text = f"{search_query} {request.message}"
        has_critical_warning = self._detect_emergency_intent(combined_text)

        # 4. Format Recent Metrics Context if provided
        recent_metrics_summary = None
        if request.recent_metrics:
            m = request.recent_metrics
            parts = []
            if m.systolic_bp and m.diastolic_bp:
                parts.append(f"Huyết áp {m.systolic_bp}/{m.diastolic_bp} mmHg")
            if m.temperature:
                parts.append(f"Thân nhiệt {m.temperature}°C")
            if m.blood_glucose:
                parts.append(f"Đường huyết {m.blood_glucose} mmol/L")
            if m.fetal_movements_count is not None:
                parts.append(f"Cử động thai {m.fetal_movements_count} lần")
            if m.symptoms:
                parts.append(f"Triệu chứng: {', '.join(m.symptoms)}")
            recent_metrics_summary = ", ".join(parts)

        # 5. Build Grounded Prompt with Conversation History for Gemini Flash
        history_dicts = [
            {"role": msg.role, "content": msg.content}
            for msg in request.conversation_history
        ] if request.conversation_history else None

        prompt = build_rag_chat_prompt(
            user_message=request.message,
            context_chunks=retrieved_chunks,
            stage=stage_filter,
            gestational_age_weeks=request.gestational_age_weeks,
            recent_metrics_summary=recent_metrics_summary,
            conversation_history=history_dicts,
        )

        # 6. Call Gemini Flash Generator
        raw_answer = await self.gemini.generate_response(
            prompt=prompt,
            system_instruction=NURSE_ASSISTANT_SYSTEM_PROMPT,
        )

        # 7. Format Source Citations
        citations: List[SourceCitation] = []
        for doc in retrieved_chunks:
            citations.append(
                SourceCitation(
                    title=doc.get("title", "Cẩm nang y tế"),
                    source=doc.get("source", "Bộ Y Tế"),
                    section=doc.get("section"),
                    snippet=doc.get("content", "")[:250] + "...",
                    similarity_score=doc.get("similarity"),
                )
            )

        # 8. Generate Dynamic Follow-up Suggestions
        followups = self._generate_followups(request.message, stage_filter, has_critical_warning)

        return RagChatResponse(
            answer=raw_answer.strip(),
            has_critical_warning=has_critical_warning,
            suggested_followups=followups,
            sources=citations,
            disclaimer=MEDICAL_DISCLAIMER,
        )

    def _detect_emergency_intent(self, message: str) -> bool:
        lower = message.lower()
        danger_signals = [
            "ra máu", "chảy máu", "vỡ ối", "rỉ ối", "co giật",
            "đau bụng dữ dội", "không thấy con đạp", "thai không cử động",
            "nhìn mờ", "hoa mắt dữ dội", "sốt cao 39", "sốt cao 40",
        ]
        return any(signal in lower for signal in danger_signals)

    def _generate_followups(
        self, message: str, stage: str, is_emergency: bool
    ) -> List[str]:
        if is_emergency:
            return [
                "Bệnh viện phụ sản gần nhất ở đâu?",
                "Gọi cấp cứu 115 như thế nào?",
                "Cần chuẩn bị giấy tờ gì khi vào viện cấp cứu?",
            ]

        lower = message.lower()
        if "ăn" in lower or "dinh dưỡng" in lower or "uống" in lower:
            return [
                "Mang thai nên kiêng những thực phẩm nào?",
                "Cách bổ sung canxi và sắt đúng cách?",
                "Lượng nước cần uống mỗi ngày là bao nhiêu?",
            ]
        elif "đau" in lower or "mệt" in lower or "nghén" in lower:
            return [
                "Làm thế nào để giảm triệu chứng đau lưng thai kỳ?",
                "Cách massage an toàn cho mẹ bầu?",
                "Khi nào cần đi khám Bác sĩ?",
            ]
        elif "thai" in lower or "tuần" in lower:
            return [
                "Lịch khám thai định kỳ theo từng tuần?",
                "Cách đếm cử động thai chuẩn tại nhà?",
                "Các mốc siêu âm dị tật quan trọng?",
            ]

        return [
            "Lịch tiêm phòng uốn ván cho mẹ bầu?",
            "Dấu hiệu chuyển dạ sắp sinh cần biết?",
            "Cách chăm sóc sức khỏe 3 tháng cuối thai kỳ?",
        ]


rag_chat_service = RagChatService()


def get_rag_chat_service() -> RagChatService:
    return rag_chat_service
