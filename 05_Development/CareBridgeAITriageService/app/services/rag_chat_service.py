"""AI Nurse Assistant RAG Chat Service (Step 10 in Workflow)."""

from __future__ import annotations

import logging
from typing import List
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import MEDICAL_DISCLAIMER
from app.core.gemini import get_gemini_client
from app.models.schemas import (
    HealthMetricsLogRequest,
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
        # 1. Build Context-Aware Vector Search Query
        # If user refers to short contextual follow-up (e.g. "có sao không?", "uống gì?"), enrich with last topic
        search_query = request.message
        if request.conversation_history and len(request.message.strip().split()) <= 4:
            recent_user_turns = [
                msg.content
                for msg in request.conversation_history[-2:]
                if msg.role.lower() in ("user", "human")
            ]
            if recent_user_turns:
                search_query = f"{recent_user_turns[-1]} {request.message}"

        # 2. Semantic Search across Maternal Knowledge pgvector
        stage_filter = request.stage.value if request.stage else "PREGNANCY"
        retrieved_chunks = await self.vector_store.similarity_search(
            query=search_query,
            stage=stage_filter,
            top_k=4,
            session=session,
        )

        # 3. Check for Critical Emergency Flags in the CURRENT user message
        has_critical_warning = self._detect_emergency_intent(request.message)

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

        # 5. Filter relevant chunks (threshold >= 0.35) and Build Grounded Prompt
        valid_chunks = [
            c for c in retrieved_chunks
            if c.get("similarity") is None or c.get("similarity", 0.0) >= 0.35
        ]

        history_dicts = [
            {"role": msg.role, "content": msg.content}
            for msg in request.conversation_history
        ] if request.conversation_history else None

        prompt = build_rag_chat_prompt(
            user_message=request.message,
            context_chunks=valid_chunks,
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

        # 7. Extract Dynamic Follow-up Suggestions & LLM Clinical Decision Flag
        answer_text, need_expert_llm, dynamic_followups = self._extract_llm_flags_and_followups(raw_answer)

        if has_critical_warning:
            emergency_chips = ["Gọi cấp cứu 115 ngay?", "Bệnh viện phụ sản gần nhất?"]
            dynamic_followups = (emergency_chips + [f for f in dynamic_followups if f not in emergency_chips])[:3]
        elif not dynamic_followups:
            dynamic_followups = self._generate_fallback_followups(has_critical_warning)

        # 8. Format Source Citations (Relevance Filter + Smart Deduplication)
        citations: List[SourceCitation] = []
        seen_keys = set()
        seen_titles_with_specific_sections = set()

        # First pass: identify titles that already have specific detailed sub-sections
        for doc in valid_chunks:
            title = doc.get("title", "Cẩm nang").strip()
            section = doc.get("section")
            if section and section.strip() and section.strip().lower() != title.lower():
                seen_titles_with_specific_sections.add(title.lower())

        for doc in valid_chunks:
            title = doc.get("title", "Cẩm nang").strip()
            section = doc.get("section")
            if section:
                section = section.strip()

            # If section is identical to title, avoid repeating "(Title)"
            if section and section.lower() == title.lower():
                # If we already cite specific sections of this document, skip the generic root title chunk
                if title.lower() in seen_titles_with_specific_sections:
                    continue
                section = None

            key = f"{title.lower()}_{section.lower() if section else ''}"
            if key in seen_keys:
                continue
            seen_keys.add(key)

            citations.append(
                SourceCitation(
                    title=title,
                    source=doc.get("source", "Bộ Y Tế"),
                    section=section,
                    snippet=doc.get("content", "")[:250] + "...",
                    similarity_score=doc.get("similarity"),
                )
            )

        # Check if expert consultation is required (LLM Clinical Judgment + Safety Gate)
        need_expert_consultation = has_critical_warning or need_expert_llm or self._detect_expert_consultation_need(
            request.message, request.recent_metrics, answer_text=answer_text
        )

        return RagChatResponse(
            answer=answer_text.strip(),
            has_critical_warning=has_critical_warning,
            need_expert_consultation=need_expert_consultation,
            suggested_followups=dynamic_followups,
            sources=citations,
            disclaimer=MEDICAL_DISCLAIMER,
        )

    def _extract_llm_flags_and_followups(self, text: str) -> tuple[str, bool, List[str]]:
        """Extracts dynamic decision flags and follow-up questions generated by Gemini LLM."""
        need_expert_from_llm = False
        cleaned_text = text

        # 1. Extract [NEED_EXPERT_CONSULTATION] tag if present
        if "[NEED_EXPERT_CONSULTATION]:" in cleaned_text:
            parts = cleaned_text.split("[NEED_EXPERT_CONSULTATION]:", 1)
            before_tag = parts[0]
            after_tag = parts[1]
            flag_line = after_tag.split("\n", 1)[0].strip().upper()
            if "YES" in flag_line or "TRUE" in flag_line:
                need_expert_from_llm = True
            
            rest = after_tag.split("\n", 1)[1] if "\n" in after_tag else ""
            cleaned_text = before_tag.strip() + ("\n\n" + rest.strip() if rest.strip() else "")

        # 2. Extract followups
        tag_candidates = [
            "[GỢI Ý CÂU HỎI]:",
            "[GỢI Ý CÂU HỎI TIẾP THEO]:",
            "[SUGGESTED_QUESTIONS]:",
            "[GỢI Ý]:",
        ]

        found_tag = None
        for tag in tag_candidates:
            if tag in cleaned_text:
                found_tag = tag
                break

        if not found_tag:
            return cleaned_text.strip(), need_expert_from_llm, []

        parts = cleaned_text.split(found_tag, 1)
        main_answer = parts[0].strip()
        followup_raw = parts[1].strip()

        followups: List[str] = []
        for line in followup_raw.splitlines():
            cleaned = line.strip().lstrip("-*•123456789.) ").strip()
            if cleaned and len(cleaned) > 3:
                if not cleaned.endswith("?"):
                    cleaned += "?"
                followups.append(cleaned)

        return main_answer, need_expert_from_llm, followups[:3]

    def _detect_emergency_intent(self, message: str) -> bool:
        lower = message.lower()
        danger_signals = [
            "ra máu", "chảy máu", "vỡ ối", "rỉ ối", "co giật",
            "đau bụng dữ dội", "không thấy con đạp", "thai không cử động",
            "nhìn mờ", "hoa mắt dữ dội", "sốt cao 39", "sốt cao 40",
        ]
        return any(signal in lower for signal in danger_signals)

    def _detect_expert_consultation_need(
        self,
        message: str,
        metrics: HealthMetricsLogRequest | None = None,
        answer_text: str | None = None,
    ) -> bool:
        """Determines if the physiological context warrants professional expert consult (Safety Guardrail)."""
        lower = message.lower()
        
        # 1. Check blood pressure regex pattern with abnormal numbers (e.g. 145/95, 140/90, 135/85)
        import re
        bp_match = re.search(r'(\d{2,3})\s*[\/\\-]\s*(\d{2,3})', lower)
        if bp_match:
            try:
                sys_val = int(bp_match.group(1))
                dia_val = int(bp_match.group(2))
                if 100 <= sys_val <= 250 and 50 <= dia_val <= 150:
                    if sys_val >= 135 or dia_val >= 85:
                        return True
            except ValueError:
                pass

        # 2. Critical danger keywords (Emergency red-flags)
        critical_danger_keywords = [
            "ra máu", "chảy máu âm đạo", "ra huyết", "vỡ ối", "rỉ ối",
            "co giật", "ngất xỉu", "đau bụng dữ dội", "đau quặn bụng",
            "thai không đạp", "không thấy con đạp", "thai không cử động",
            "sốt cao 39", "sốt cao 40", "tiền sản giật nặng",
        ]
        if any(kw in lower for kw in critical_danger_keywords):
            return True

        # 3. Check empirical measurements from logged metrics
        if metrics:
            if (metrics.systolic_bp and metrics.systolic_bp >= 135) or (metrics.diastolic_bp and metrics.diastolic_bp >= 85):
                return True
            if metrics.temperature and metrics.temperature >= 37.8:
                return True
            if metrics.blood_glucose and metrics.blood_glucose >= 7.2:
                return True
            if metrics.epds_score and metrics.epds_score >= 10:
                return True
            if metrics.fetal_movements_count is not None and metrics.fetal_movements_count < 4:
                return True
            if metrics.symptoms and len(metrics.symptoms) > 0:
                # Only flag if there are recognized warning symptoms
                risk_symptoms = ["đau đầu", "nhìn mờ", "hoa mắt", "phù", "ra máu", "sốt", "đau bụng"]
                if any(any(rs in s.lower() for rs in risk_symptoms) for s in metrics.symptoms):
                    return True

        return False

    def _generate_fallback_followups(self, is_emergency: bool) -> List[str]:
        """Minimal fallback only used if Gemini fails to provide dynamic follow-up tags."""
        if is_emergency:
            return [
                "Bệnh viện phụ sản gần nhất ở đâu?",
                "Gọi cấp cứu 115 như thế nào?",
                "Cần chuẩn bị giấy tờ gì khi vào viện cấp cứu?",
            ]
        return [
            "Các mốc khám thai quan trọng cần nhớ?",
            "Chế độ dinh dưỡng khoa học theo từng giai đoạn?",
            "Dấu hiệu bất thường cần đến cơ sở y tế?",
        ]


rag_chat_service = RagChatService()


def get_rag_chat_service() -> RagChatService:
    return rag_chat_service
