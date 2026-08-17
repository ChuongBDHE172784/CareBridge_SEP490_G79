"""Health metrics screening and danger-sign detection service (Steps 7 -> 8 -> 9 in Workflow)."""

from __future__ import annotations

import logging
from typing import List, Tuple
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import MEDICAL_DISCLAIMER
from app.models.schemas import (
    HealthMetricsEvaluationResponse,
    HealthMetricsLogRequest,
    MaternalStage,
    SourceCitation,
    TriageRiskStatus,
)
from app.rag.vector_store import get_vector_store

logger = logging.getLogger(__name__)


class MetricsScreeningService:
    def __init__(self) -> None:
        self.vector_store = get_vector_store()

    async def evaluate_metrics(
        self,
        request: HealthMetricsLogRequest,
        session: AsyncSession | None = None,
    ) -> HealthMetricsEvaluationResponse:
        """Evaluate maternal health metrics against clinical thresholds & knowledge base."""
        risk_factors: List[str] = []
        is_critical = False
        is_anomaly = False

        # 1. Evaluate Blood Pressure (Huyết áp)
        bp_status, bp_factors = self._check_blood_pressure(
            request.systolic_bp, request.diastolic_bp, request.symptoms
        )
        if bp_status == TriageRiskStatus.CRITICAL_EMERGENCY:
            is_critical = True
        elif bp_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(bp_factors)

        # 2. Evaluate Temperature (Thân nhiệt)
        temp_status, temp_factors = self._check_temperature(request.temperature)
        if temp_status == TriageRiskStatus.CRITICAL_EMERGENCY:
            is_critical = True
        elif temp_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(temp_factors)

        # 3. Evaluate Blood Glucose (Đường huyết thai kỳ)
        glucose_status, glucose_factors = self._check_glucose(
            request.blood_glucose, request.is_fasting_glucose
        )
        if glucose_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(glucose_factors)

        # 4. Evaluate Fetal Movements (Cử động thai - Kicks)
        fetal_status, fetal_factors = self._check_fetal_movements(
            request.fetal_movements_count,
            request.fetal_movements_duration_hours,
            request.gestational_age_weeks,
        )
        if fetal_status == TriageRiskStatus.CRITICAL_EMERGENCY:
            is_critical = True
        elif fetal_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(fetal_factors)

        # 5. Evaluate Critical Symptoms (Triệu chứng báo động đỏ)
        symptom_status, symptom_factors = self._check_symptoms(request.symptoms, request.free_text_notes)
        if symptom_status == TriageRiskStatus.CRITICAL_EMERGENCY:
            is_critical = True
        elif symptom_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(symptom_factors)

        # 6. Evaluate BMI & Weight / Height (Chỉ số BMI và thể trạng mẹ)
        bmi_status, bmi_factors = self._check_bmi(
            request.bmi, request.weight_kg, request.height_cm
        )
        if bmi_status == TriageRiskStatus.CRITICAL_EMERGENCY:
            is_critical = True
        elif bmi_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(bmi_factors)

        # 7. Evaluate Maternal Heart Rate (Nhịp tim mẹ)
        hr_status, hr_factors = self._check_heart_rate(request.heart_rate)
        if hr_status == TriageRiskStatus.CRITICAL_EMERGENCY:
            is_critical = True
        elif hr_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(hr_factors)

        # 8. RAG Retrieval for matching medical guidelines
        query = self._build_retrieval_query(request, risk_factors)
        rag_results = await self.vector_store.similarity_search(
            query=query,
            stage=request.stage.value if request.stage else "PREGNANCY",
            top_k=3,
            session=session,
        )

        citations = [
            SourceCitation(
                title=doc["title"],
                source=doc["source"],
                section=doc.get("section"),
                snippet=doc["content"][:280] + "..." if len(doc["content"]) > 280 else doc["content"],
                similarity_score=doc.get("similarity"),
            )
            for doc in rag_results
        ]

        # 7. Final Classification Outcome
        if is_critical:
            status = TriageRiskStatus.CRITICAL_EMERGENCY
            emergency_mode = True
            headline = "CẢNH BÁO: Phát hiện chỉ số / dấu hiệu nguy hiểm khẩn cấp!"
            summary = (
                "Chỉ số sinh hiệu hoặc triệu chứng của mẹ đang ở ngưỡng báo động cao. "
                "Cần kích hoạt chế độ khẩn cấp, liên hệ ngay cơ sở y tế hoặc khoa Cấp cứu Sản gần nhất."
            )
            suggested_action = (
                "KÍCH HOẠT CHẾ ĐỘ CẤP CỨU: Gọi 115 hoặc di chuyển ngay đến Bệnh viện chuyên khoa Sản gần nhất."
            )
        elif is_anomaly or len(risk_factors) > 0:
            status = TriageRiskStatus.ANOMALY_MONITOR
            emergency_mode = False
            headline = "Lưu ý: Chỉ số có dấu hiệu bất thường nhẹ cần theo dõi"
            summary = (
                f"Phát hiện {len(risk_factors)} yếu tố cần lưu ý trong các chỉ số mẹ vừa nhập. "
                "Mẹ nên trao đổi thêm với AI Nurse Assistant để được hướng dẫn chi tiết hoặc đặt lịch khám Bác sĩ."
            )
            suggested_action = (
                "Trò chuyện với AI Nurse Assistant để làm rõ triệu chứng hoặc Đặt lịch hẹn Bác sĩ tư vấn."
            )
        else:
            status = TriageRiskStatus.NORMAL
            emergency_mode = False
            headline = "Tuyệt vời: Các chỉ số sinh hiệu hoàn toàn bình thường"
            summary = (
                "Tất cả các chỉ số sinh hiệu của mẹ đều nằm trong giới hạn an toàn theo hướng dẫn y tế thai kỳ. "
                "Mẹ hãy tiếp tục duy trì chế độ dinh dưỡng và nghỉ ngơi hợp lý."
            )
            suggested_action = "Tiếp tục theo dõi sức khỏe và thực hiện kế hoạch chăm sóc hàng ngày."

        return HealthMetricsEvaluationResponse(
            status=status,
            emergency_mode=emergency_mode,
            headline=headline,
            summary=summary,
            risk_factors=risk_factors,
            suggested_action=suggested_action,
            relevant_sources=citations,
            disclaimer=MEDICAL_DISCLAIMER,
        )

    # -------------------------------------------------------------------------
    # Threshold Checkers
    # -------------------------------------------------------------------------

    def _check_blood_pressure(
        self,
        sbp: int | None,
        dbp: int | None,
        symptoms: List[str],
    ) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []
        if sbp is None and dbp is None:
            return TriageRiskStatus.NORMAL, factors

        symptom_text = " ".join(symptoms).lower()
        has_preeclampsia_symptoms = any(
            k in symptom_text
            for k in [
                "đau đầu", "hoa mắt", "nhìn mờ", "đau thượng vị", "đau hạ sườn", "phù mặt",
                "headache", "blurred vision", "epigastric",
            ]
        )

        # Severe Hypertension (Cơn tăng huyết áp nặng)
        if (sbp and sbp >= 160) or (dbp and dbp >= 110):
            factors.append(f"Huyết áp rất cao ({sbp}/{dbp} mmHg) - Nguy cơ Tiền sản giật nặng / Đột quỵ thai kỳ")
            return TriageRiskStatus.CRITICAL_EMERGENCY, factors

        # Stage 1 Hypertension with symptoms (Tiền sản giật)
        if (sbp and sbp >= 140) or (dbp and dbp >= 90):
            if has_preeclampsia_symptoms:
                factors.append(
                    f"Huyết áp cao ({sbp}/{dbp} mmHg) kèm triệu chứng báo động (đau đầu/hoa mắt/đau bụng) - Nghi ngờ Tiền sản giật"
                )
                return TriageRiskStatus.CRITICAL_EMERGENCY, factors
            else:
                factors.append(f"Huyết áp tăng cao ({sbp}/{dbp} mmHg) - Cần theo dõi sát huyết áp thai kỳ")
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        # Pre-hypertension
        if (sbp and 130 <= sbp < 140) or (dbp and 85 <= dbp < 90):
            factors.append(f"Huyết áp hơi cao ({sbp}/{dbp} mmHg) so với bình thường")
            return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    def _check_temperature(self, temp: float | None) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []
        if temp is None:
            return TriageRiskStatus.NORMAL, factors

        if temp >= 38.5:
            factors.append(f"Sốt cao ({temp}°C) - Nguy cơ nhiễm trùng ối hoặc nhiễm trùng toàn thân")
            return TriageRiskStatus.CRITICAL_EMERGENCY, factors
        elif temp >= 37.5:
            factors.append(f"Thân nhiệt sốt nhẹ ({temp}°C)")
            return TriageRiskStatus.ANOMALY_MONITOR, factors
        elif temp < 35.5:
            factors.append(f"Thân nhiệt hạ thấp ({temp}°C)")
            return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    def _check_glucose(
        self, glucose: float | None, is_fasting: bool | None
    ) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []
        if glucose is None:
            return TriageRiskStatus.NORMAL, factors

        # Fasting threshold (>= 5.1 mmol/L nghi đái tháo đường thai kỳ)
        if is_fasting:
            if glucose >= 7.0:
                factors.append(f"Đường huyết lúc đói rất cao ({glucose} mmol/L)")
                return TriageRiskStatus.ANOMALY_MONITOR, factors
            elif glucose >= 5.1:
                factors.append(f"Đường huyết lúc đói tăng nhẹ ({glucose} mmol/L, chuẩn < 5.1)")
                return TriageRiskStatus.ANOMALY_MONITOR, factors
        else:
            if glucose >= 11.1:
                factors.append(f"Đường huyết sau ăn rất cao ({glucose} mmol/L)")
                return TriageRiskStatus.ANOMALY_MONITOR, factors
            elif glucose >= 8.5:
                factors.append(f"Đường huyết sau ăn tăng ({glucose} mmol/L, chuẩn < 8.5)")
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    def _check_fetal_movements(
        self,
        count: int | None,
        duration_hours: int | None,
        weeks: int | None,
    ) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []
        if count is None:
            return TriageRiskStatus.NORMAL, factors

        # Fetal kick counting is critical from week 28 onwards
        if weeks and weeks >= 28:
            duration = duration_hours or 2
            if count == 0:
                factors.append(f"Thai không cử động suốt {duration} giờ (Tuần thai {weeks}) - Nguy cơ suy thai cấp")
                return TriageRiskStatus.CRITICAL_EMERGENCY, factors
            elif count < 4 and duration >= 2:
                factors.append(f"Thai cử động yếu (chỉ {count} lần/{duration}h, chuẩn tối thiểu >= 4 lần/2h)")
                return TriageRiskStatus.CRITICAL_EMERGENCY, factors
            elif count < 10 and duration >= 4:
                factors.append(f"Cử động thai giảm ({count} lần/{duration}h)")
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    def _check_symptoms(
        self, symptoms: List[str], notes: str | None
    ) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []
        combined = (" ".join(symptoms) + " " + (notes or "")).lower()

        # Severe Red Flags
        critical_keywords = [
            ("ra máu", "Ra máu âm đạo tươi lượng nhiều"),
            ("chảy máu", "Chảy máu âm đạo bất thường"),
            ("vỡ ối", "Rỉ ối / Vỡ ối sớm"),
            ("rỉ ối", "Rỉ nước ối"),
            ("co giật", "Co giật / Tiền sản giật giật"),
            ("đau bụng dữ dội", "Đau bụng quặn dữ dội liên tục"),
            ("khó thở dữ dội", "Khó thở nghiêm trọng"),
        ]
        for kw, desc in critical_keywords:
            if kw in combined:
                factors.append(f"Triệu chứng khẩn cấp: {desc}")
                return TriageRiskStatus.CRITICAL_EMERGENCY, factors

        # Mild / Common anomalies
        mild_keywords = [
            ("phù", "Phù nề chân/tay"),
            ("đau lưng", "Đau mỏi lưng hông"),
            ("buồn nôn", "Ốm nghén / Buồn nôn"),
            ("chóng mặt", "Chóng mặt, choáng váng"),
            ("tiểu buốt", "Tiểu buốt / Tiểu rắt (nghi ngờ nhiễm trùng tiết niệu)"),
            ("ngứa", "Ngứa ngoài da / lòng bàn tay bàn chân"),
        ]
        for kw, desc in mild_keywords:
            if kw in combined:
                factors.append(f"Triệu chứng cần lưu ý: {desc}")

        if factors:
            return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    def _check_bmi(
        self,
        bmi: float | None,
        weight_kg: float | None,
        height_cm: float | None,
    ) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []
        val_bmi = bmi
        if val_bmi is None and weight_kg is not None and height_cm is not None and height_cm > 0:
            val_bmi = weight_kg / ((height_cm / 100) ** 2)

        if val_bmi is None:
            return TriageRiskStatus.NORMAL, factors

        if val_bmi >= 40.0:
            factors.append(
                f"Béo phì độ III rất nặng (BMI: {val_bmi:.1f} kg/m²) - Nguy cơ cao Tiền sản giật, ĐTĐ thai kỳ, Thuyên tắc huyết khối"
            )
            return TriageRiskStatus.ANOMALY_MONITOR, factors
        elif val_bmi >= 30.0:
            factors.append(
                f"Béo phì thai kỳ (BMI: {val_bmi:.1f} kg/m²) - Cần khám dinh dưỡng và theo dõi sát huyết áp, đường huyết"
            )
            return TriageRiskStatus.ANOMALY_MONITOR, factors
        elif val_bmi >= 25.0:
            factors.append(
                f"Thừa cân thai kỳ (BMI: {val_bmi:.1f} kg/m²) - Cần kiểm soát mức tăng cân hợp lý theo khuyến nghị"
            )
            return TriageRiskStatus.ANOMALY_MONITOR, factors
        elif val_bmi < 18.5:
            factors.append(
                f"Thiếu cân thai kỳ (BMI: {val_bmi:.1f} kg/m²) - Nguy cơ suy dinh dưỡng bào thai hoặc sinh non"
            )
            return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    def _check_heart_rate(self, hr: int | None) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []
        if hr is None:
            return TriageRiskStatus.NORMAL, factors

        if hr >= 120:
            factors.append(f"Nhịp tim mẹ tăng rất nhanh ({hr} bpm) - Nguy cơ rối loạn nhịp tim cấp, thiếu máu nặng hoặc sốc")
            return TriageRiskStatus.CRITICAL_EMERGENCY, factors
        elif hr >= 100:
            factors.append(f"Nhịp tim mẹ hơi nhanh ({hr} bpm) so với sinh lý bình thường")
            return TriageRiskStatus.ANOMALY_MONITOR, factors
        elif hr < 50:
            factors.append(f"Nhịp tim mẹ chậm ({hr} bpm)")
            return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    def _build_retrieval_query(
        self, request: HealthMetricsLogRequest, risk_factors: List[str]
    ) -> str:
        parts = []
        if request.gestational_age_weeks:
            parts.append(f"thai tuần {request.gestational_age_weeks}")
        if request.systolic_bp:
            parts.append(f"huyết áp {request.systolic_bp}/{request.diastolic_bp}")
        if request.blood_glucose:
            parts.append(f"đường huyết {request.blood_glucose}")
        if request.bmi or (request.weight_kg and request.height_cm):
            bmi_val = request.bmi or (request.weight_kg / ((request.height_cm / 100) ** 2))
            if bmi_val >= 25.0:
                parts.append("thừa cân béo phì dinh dưỡng thai kỳ")
            elif bmi_val < 18.5:
                parts.append("thiếu cân suy dinh dưỡng thai kỳ")
        if request.fetal_movements_count is not None:
            parts.append(f"cử động thai {request.fetal_movements_count} lần")
        if request.temperature and request.temperature >= 37.5:
            parts.append(f"sốt {request.temperature} độ")
        if request.symptoms:
            parts.extend(request.symptoms)
        if request.free_text_notes:
            parts.append(request.free_text_notes)
        if risk_factors:
            parts.extend(risk_factors)
        if not parts:
            parts.append("hướng dẫn chăm sóc và theo dõi sức khỏe thai kỳ chuẩn y tế")
        return " ".join(parts)


metrics_screening_service = MetricsScreeningService()


def get_metrics_screening_service() -> MetricsScreeningService:
    return metrics_screening_service
