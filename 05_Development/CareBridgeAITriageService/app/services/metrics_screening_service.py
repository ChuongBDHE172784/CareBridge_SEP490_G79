"""Health metrics screening and danger-sign detection service (Steps 7 -> 8 -> 9 in Workflow).

Clinical Guidelines & Scientific Sources:
- Huyết áp & Tiền sản giật: Quyết định 1154/QĐ-BYT (04/05/2024 - Bộ Y Tế) & ACOG Practice Bulletin No. 222
  Link QĐ 1154/BYT: https://thuvienphapluat.vn/van-ban/The-thao-Y-te/Quyet-dinh-1154-QD-BYT-2024-tai-lieu-Huong-dan-xu-tri-tang-huyet-ap-o-phu-nu-mang-thai-643196.aspx
  Link ACOG PB 222 (PubMed): https://pubmed.ncbi.nlm.nih.gov/32443079/
- Thân nhiệt & Sốt Sản khoa: WHO Peripartum Infection Guidelines & Quyết định 1359/QĐ-BYT
  Link WHO IRIS: https://apps.who.int/iris/handle/10665/186171
  Link PubMed: https://pubmed.ncbi.nlm.nih.gov/26512398/
- Đường huyết Thai kỳ (6 ngữ cảnh): Quyết định 1470/QĐ-BYT (29/05/2024 - Bộ Y Tế) & ADA Standards of Care (2024)
  Link QĐ 1470/BYT: https://bvdkbaclieu.gov.vn/van-ban-phap-quy/quyet-dinh-1470-qd-byt-cua-bo-y-te-ve-viec-ban-hanh-tai-lieu.html
  Link ADA: https://diabetesjournals.org/care/article/47/Supplement_1/S282/153957/15-Management-of-Diabetes-in-Pregnancy-Standards
- Cử động Thai máy (Tuần 28+): RCOG Green-top Guideline No. 57 & ACOG PB 229
  Link RCOG: https://www.rcog.org.uk/guidance/browse-all-guidance/green-top-guidelines/reduced-fetal-movements-green-top-guideline-no-57/
  Link ACOG (PubMed): https://pubmed.ncbi.nlm.nih.gov/34011892/
- BMI & Thể trạng theo giai đoạn: WHO Asian BMI (Lancet 2004) & IOM Pregnancy Weight Gain Guidelines (2009)
  Link WHO Asian BMI (PubMed): https://pubmed.ncbi.nlm.nih.gov/14726461/
  Link IOM Guidelines: https://www.ncbi.nlm.nih.gov/books/NBK32813/
- Sàng lọc Trầm cảm: Edinburgh Postnatal Depression Scale (EPDS) & COPE / NSW Health Guidelines
  Link: https://www.cope.org.au/health-professionals/health-professional-guidelines/
"""

from __future__ import annotations

import logging
from typing import List, Tuple
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import MEDICAL_DISCLAIMER
from app.constants.vital_thresholds import (
    # Blood pressure constants
    BP_CRITICAL_SYSTOLIC,
    BP_CRITICAL_DIASTOLIC,
    BP_STAGE1_SYSTOLIC,
    BP_STAGE1_DIASTOLIC,
    BP_ELEVATED_SYSTOLIC_MIN,
    BP_ELEVATED_DIASTOLIC_MIN,
    # Temperature constants
    TEMP_HYPOTHERMIA_THRESHOLD,
    TEMP_MILD_FEVER_THRESHOLD,
    TEMP_MODERATE_FEVER_THRESHOLD,
    TEMP_CRITICAL_FEVER_PREGNANCY,
    TEMP_CRITICAL_FEVER_POSTPARTUM,
    TEMP_CRITICAL_FEVER_GENERAL,
    # Glucose constants & contexts
    GLUCOSE_UNIT_CONVERSION_FACTOR,
    GLUCOSE_UNIT_AUTODETECT_MG_DL_THRESHOLD,
    GLUCOSE_HYPOGLYCEMIA_THRESHOLD,
    GLUCOSE_FASTING_WARNING_THRESHOLD,
    GLUCOSE_FASTING_CRITICAL_THRESHOLD,
    GLUCOSE_PRE_MEAL_WARNING_THRESHOLD,
    GLUCOSE_POST_MEAL_1H_WARNING_THRESHOLD,
    GLUCOSE_POST_MEAL_1H_CRITICAL_THRESHOLD,
    GLUCOSE_POST_MEAL_2H_WARNING_THRESHOLD,
    GLUCOSE_POST_MEAL_2H_CRITICAL_THRESHOLD,
    GLUCOSE_RANDOM_CRITICAL_THRESHOLD,
    GLUCOSE_POSTPRANDIAL_WARNING_THRESHOLD,
    GLUCOSE_POSTPRANDIAL_CRITICAL_THRESHOLD,
    GlucoseMeasurementContext,
    # Fetal movement constants
    FETAL_MONITORING_MIN_GESTATIONAL_WEEK,
    FETAL_MONITORING_EARLIEST_VALID_WEEK,
    FETAL_DEFAULT_DURATION_HOURS,
    FETAL_CRITICAL_ZERO_KICKS,
    FETAL_MIN_KICKS_2H_THRESHOLD,
    FETAL_MIN_KICKS_4H_THRESHOLD,
    # BMI constants
    BMI_ASIAN_UNDERWEIGHT_MAX,
    BMI_ASIAN_NORMAL_MAX,
    BMI_ASIAN_OVERWEIGHT_MAX,
    BMI_UNDERWEIGHT_THRESHOLD,
    BMI_OVERWEIGHT_THRESHOLD,
    BMI_OBESE_THRESHOLD,
    BMI_SEVERELY_OBESE_THRESHOLD,
    # Heart rate constants
    HR_CRITICAL_HIGH_THRESHOLD,
    HR_ELEVATED_HIGH_THRESHOLD,
    HR_BRADYCARDIA_LOW_THRESHOLD,
    # Water intake constants
    WATER_PRECONCEPTION_MIN_ML,
    WATER_PRECONCEPTION_REC_ML,
    WATER_PREGNANCY_CRITICAL_LOW_ML,
    WATER_PREGNANCY_MIN_ML,
    WATER_PREGNANCY_REC_ML,
    WATER_POSTPARTUM_MIN_ML,
    WATER_POSTPARTUM_REC_ML,
    WATER_EXCESSIVE_ML,
    # EPDS constants
    EPDS_QUESTION_10_RED_FLAG_THRESHOLD,
    EPDS_HIGH_RISK_DEPRESSION_THRESHOLD,
    EPDS_MILD_RISK_DEPRESSION_THRESHOLD,
    # SpO2 constants
    SPO2_CRITICAL_LOW_THRESHOLD,
    SPO2_WARNING_LOW_THRESHOLD,
    # Sleep constants
    SLEEP_MIN_HOURS_THRESHOLD,
    # Sanity validation
    SANITY_RANGES,
    # Symptoms references
    PREECLAMPSIA_ALARM_KEYWORDS,
    TEMPERATURE_ALARM_KEYWORDS,
    CRITICAL_SYMPTOMS_CATALOG,
    MILD_SYMPTOMS_CATALOG,
)
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
        """Evaluate maternal health metrics against clinical thresholds & knowledge base.
        
        Applies Layer 1 Deterministic Clinical Safety Guardrails (Rule-based)
        followed by Layer 2 AI RAG Knowledge Retrieval & Evidence Citations.
        """
        risk_factors: List[str] = []
        is_critical = False
        is_anomaly = False
        current_stage = request.stage or MaternalStage.PREGNANCY

        # 1. Evaluate Blood Pressure (Huyết áp - ACOG PB 222 & QĐ 4163/BYT)
        bp_status, bp_factors = self._check_blood_pressure(
            request.systolic_bp,
            request.diastolic_bp,
            request.symptoms or [],
            free_text_notes=request.free_text_notes,
        )
        if bp_status == TriageRiskStatus.CRITICAL_EMERGENCY:
            is_critical = True
        elif bp_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(bp_factors)

        # 2. Evaluate Temperature (Thân nhiệt - WHO Sepsis & QĐ 1359/BYT)
        temp_status, temp_factors = self._check_temperature(
            request.temperature,
            stage=current_stage,
            symptoms=request.symptoms or [],
        )
        if temp_status == TriageRiskStatus.CRITICAL_EMERGENCY:
            is_critical = True
        elif temp_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(temp_factors)

        # 3. Evaluate Blood Glucose (Đường huyết thai kỳ 6 ngữ cảnh - ADA 2024 & QĐ 3494/BYT)
        glucose_status, glucose_factors = self._check_glucose(
            request.blood_glucose,
            glucose_context=request.glucose_context,
            is_fasting=request.is_fasting_glucose,
        )
        if glucose_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(glucose_factors)

        # 4. Evaluate Fetal Movements (Cử động thai - ACOG Opinion 828 & RCOG No. 57)
        fetal_status, fetal_factors = self._check_fetal_movements(
            request.fetal_movements_count,
            request.fetal_movements_duration_hours,
            request.gestational_age_weeks,
            stage=current_stage,
        )
        if fetal_status == TriageRiskStatus.CRITICAL_EMERGENCY:
            is_critical = True
        elif fetal_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(fetal_factors)

        # 5. Evaluate Critical Symptoms (Triệu chứng báo động đỏ sản khoa)
        symptom_status, symptom_factors = self._check_symptoms(request.symptoms, request.free_text_notes)
        if symptom_status == TriageRiskStatus.CRITICAL_EMERGENCY:
            is_critical = True
        elif symptom_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(symptom_factors)

        # 6. Evaluate BMI & Thể trạng theo giai đoạn (IOM 2009 & WHO Asian IDI/WPRO)
        bmi_status, bmi_factors = self._check_bmi(
            request.bmi,
            request.weight_kg,
            request.height_cm,
            stage=current_stage,
            weeks=request.gestational_age_weeks,
        )
        if bmi_status == TriageRiskStatus.CRITICAL_EMERGENCY:
            is_critical = True
        elif bmi_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(bmi_factors)

        # 7. Evaluate Maternal Heart Rate (Nhịp tim mẹ - ESC Guidelines)
        hr_status, hr_factors = self._check_heart_rate(request.heart_rate)
        if hr_status == TriageRiskStatus.CRITICAL_EMERGENCY:
            is_critical = True
        elif hr_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(hr_factors)

        # 8. Evaluate Hydration theo Giai đoạn (Viện Dinh Dưỡng & WHO)
        water_status, water_factors = self._check_water_intake(
            request.water_intake_ml,
            stage=current_stage,
        )
        if water_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(water_factors)

        # 9. Evaluate EPDS Mood Score (Thang trầm cảm EPDS & Cờ đỏ Câu 10 - Cox et al. / COPE)
        epds_status, epds_factors = self._check_epds_score(
            score=request.epds_score,
            question_10_score=request.epds_question_10_score,
        )
        if epds_status == TriageRiskStatus.CRITICAL_EMERGENCY:
            is_critical = True
        elif epds_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(epds_factors)

        # 10. Evaluate SpO2 & Sleep (Oxy máu và giấc ngủ)
        spo2_status, spo2_factors = self._check_spo2(request.spo2)
        if spo2_status == TriageRiskStatus.CRITICAL_EMERGENCY:
            is_critical = True
        elif spo2_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(spo2_factors)

        sleep_status, sleep_factors = self._check_sleep(request.sleep_hours)
        if sleep_status == TriageRiskStatus.ANOMALY_MONITOR:
            is_anomaly = True
        risk_factors.extend(sleep_factors)

        # 11. RAG Retrieval for matching medical guidelines (Lớp 2 AI RAG)
        query = self._build_retrieval_query(request, risk_factors)
        rag_results = await self.vector_store.similarity_search(
            query=query,
            stage=current_stage.value if current_stage else "PREGNANCY",
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

        # 12. Final Classification Outcome
        if is_critical:
            status = TriageRiskStatus.CRITICAL_EMERGENCY
            emergency_mode = True
            headline = "CẢNH BÁO: Phát hiện chỉ số / dấu hiệu nguy hiểm khẩn cấp!"
            summary = (
                "Chỉ số sinh hiệu hoặc triệu chứng của mẹ đang ở ngưỡng báo động cao theo chuẩn y tế. "
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
    # 1. Huyết áp (Blood Pressure Check)
    # Nguồn y khoa: ACOG Practice Bulletin No. 222 & Quyết định 4163/QĐ-BYT
    # -------------------------------------------------------------------------
    def _check_blood_pressure(
        self,
        sbp: int | None,
        dbp: int | None,
        symptoms: List[str],
        free_text_notes: str | None = None,
    ) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []
        if sbp is None and dbp is None:
            return TriageRiskStatus.NORMAL, factors

        # Plausibility check: SBP must be > DBP
        if sbp is not None and dbp is not None and sbp <= dbp:
            factors.append(f"Chỉ số huyết áp không hợp lệ ({sbp}/{dbp} mmHg: Huyết áp tâm thu phải lớn hơn tâm trương)")
            return TriageRiskStatus.ANOMALY_MONITOR, factors

        symptom_text = (" ".join(symptoms) + " " + (free_text_notes or "")).lower()
        has_preeclampsia_symptoms = any(k in symptom_text for k in PREECLAMPSIA_ALARM_KEYWORDS)

        # Severe Hypertension (Cơn tăng huyết áp nặng - Cấp cứu khẩn cấp)
        if (sbp and sbp >= BP_CRITICAL_SYSTOLIC) or (dbp and dbp >= BP_CRITICAL_DIASTOLIC):
            factors.append(
                f"Huyết áp rất cao ({sbp}/{dbp} mmHg) - Nguy cơ Tiền sản giật nặng / Đột quỵ thai kỳ (Chuẩn ACOG/BYT SBP>=160 hoặc DBP>=110)"
            )
            return TriageRiskStatus.CRITICAL_EMERGENCY, factors

        # Stage 1 Hypertension with symptoms (Nghi ngờ Tiền sản giật)
        if (sbp and sbp >= BP_STAGE1_SYSTOLIC) or (dbp and dbp >= BP_STAGE1_DIASTOLIC):
            if has_preeclampsia_symptoms:
                factors.append(
                    f"Huyết áp cao ({sbp}/{dbp} mmHg) kèm triệu chứng báo động (đau đầu/hoa mắt/chóng mặt/đau bụng) - Nghi ngờ Tiền sản giật"
                )
                return TriageRiskStatus.CRITICAL_EMERGENCY, factors
            else:
                factors.append(f"Huyết áp tăng cao ({sbp}/{dbp} mmHg, chuẩn < 140/90) - Cần theo dõi sát huyết áp thai kỳ")
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        # Pre-hypertension (Tiền tăng huyết áp)
        if (sbp and BP_ELEVATED_SYSTOLIC_MIN <= sbp < BP_STAGE1_SYSTOLIC) or (
            dbp and BP_ELEVATED_DIASTOLIC_MIN <= dbp < BP_STAGE1_DIASTOLIC
        ):
            factors.append(f"Huyết áp hơi cao ({sbp}/{dbp} mmHg) so với mức tối ưu thai kỳ (< 130/85 mmHg)")
            return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    # -------------------------------------------------------------------------
    # 2. Thân nhiệt (Temperature Check)
    # Nguồn y khoa: WHO Guidelines on Maternal Sepsis (2017) & Quyết định 1359/QĐ-BYT
    # -------------------------------------------------------------------------
    def _check_temperature(
        self,
        temp: float | None,
        stage: MaternalStage = MaternalStage.PREGNANCY,
        symptoms: List[str] = [],
    ) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []
        if temp is None:
            return TriageRiskStatus.NORMAL, factors

        symptom_text = " ".join(symptoms).lower()
        has_alarm_symptoms = any(k in symptom_text for k in TEMPERATURE_ALARM_KEYWORDS)

        # 1. Giai đoạn Hậu sản / Sau sinh (POSTPARTUM)
        if stage == MaternalStage.POSTPARTUM:
            if temp >= TEMP_CRITICAL_FEVER_POSTPARTUM or (
                temp >= TEMP_MODERATE_FEVER_THRESHOLD and has_alarm_symptoms
            ):
                factors.append(
                    f"Sốt hậu sản ({temp}°C) - Nghi ngờ Nhiễm trùng hậu sản / Viêm nội mạc tử cung / Viêm tắc tuyến vú theo chuẩn WHO"
                )
                return TriageRiskStatus.CRITICAL_EMERGENCY, factors
            elif temp >= TEMP_MODERATE_FEVER_THRESHOLD:
                factors.append(
                    f"Sốt sau sinh ({temp}°C) - Cần theo dõi sát nguy cơ Nhiễm trùng hậu sản"
                )
                return TriageRiskStatus.CRITICAL_EMERGENCY, factors
            elif temp >= TEMP_MILD_FEVER_THRESHOLD:
                factors.append(f"Thân nhiệt tăng nhẹ sau sinh ({temp}°C)")
                return TriageRiskStatus.ANOMALY_MONITOR, factors
            elif temp < TEMP_HYPOTHERMIA_THRESHOLD:
                factors.append(f"Thân nhiệt hạ thấp ({temp}°C, chuẩn >= 35.5°C)")
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        # 2. Giai đoạn Đang mang thai (PREGNANCY)
        elif stage == MaternalStage.PREGNANCY:
            if temp >= TEMP_CRITICAL_FEVER_PREGNANCY or (
                temp >= TEMP_MODERATE_FEVER_THRESHOLD and has_alarm_symptoms
            ):
                factors.append(
                    f"Sốt cao thai kỳ ({temp}°C) - Nguy cơ Nhiễm trùng ối (Chorioamnionitis) hoặc Nhiễm khuẩn toàn thân"
                )
                return TriageRiskStatus.CRITICAL_EMERGENCY, factors
            elif temp >= TEMP_MILD_FEVER_THRESHOLD:
                factors.append(
                    f"Sốt nhẹ thai kỳ ({temp}°C) - Cần bù nước, hạ sốt an toàn và theo dõi sát"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors
            elif temp < TEMP_HYPOTHERMIA_THRESHOLD:
                factors.append(f"Thân nhiệt hạ thấp ({temp}°C)")
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        # 3. Giai đoạn Tiền mang thai / Chung (PRECONCEPTION / ALL)
        else:
            if temp >= TEMP_CRITICAL_FEVER_GENERAL or (
                temp >= TEMP_CRITICAL_FEVER_PREGNANCY and has_alarm_symptoms
            ):
                factors.append(f"Sốt cao ({temp}°C) - Cần cấp cứu y tế")
                return TriageRiskStatus.CRITICAL_EMERGENCY, factors
            elif temp >= TEMP_MILD_FEVER_THRESHOLD:
                factors.append(f"Sốt nhẹ ({temp}°C)")
                return TriageRiskStatus.ANOMALY_MONITOR, factors
            elif temp < TEMP_HYPOTHERMIA_THRESHOLD:
                factors.append(f"Thân nhiệt hạ thấp ({temp}°C)")
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    # -------------------------------------------------------------------------
    # 3. Đường huyết (Blood Glucose Check - 6 Ngữ cảnh)
    # Nguồn y khoa: ADA Standards of Care (2024), IADPSG & Quyết định 3494/QĐ-BYT
    # -------------------------------------------------------------------------
    def _check_glucose(
        self,
        glucose: float | None,
        glucose_context: GlucoseMeasurementContext | str | None = None,
        is_fasting: bool | None = None,
    ) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []
        if glucose is None:
            return TriageRiskStatus.NORMAL, factors

        # Tự động nhận diện đơn vị đo & chuyển đổi chuẩn xác
        # Nếu glucose >= 25.0, giá trị được nhập theo đơn vị mg/dL
        is_mg_dl = glucose >= GLUCOSE_UNIT_AUTODETECT_MG_DL_THRESHOLD
        mmol_val = round(glucose / GLUCOSE_UNIT_CONVERSION_FACTOR, 2) if is_mg_dl else round(glucose, 2)
        mg_dl_val = round(glucose, 1) if is_mg_dl else round(glucose * GLUCOSE_UNIT_CONVERSION_FACTOR, 1)

        # Plausibility / Sanity check: Ngăn chặn lỗi nhập phi thực tế (ví dụ: gõ nhầm 300 mmol/L)
        if not is_mg_dl and mmol_val > SANITY_RANGES["glucose_mmol_l"][1]:
            factors.append(
                f"Giá trị đường huyết ({mmol_val} mmol/L) vượt ngưỡng sinh lý tối đa của máy đo. "
                "Có thể mẹ đã nhập theo đơn vị mg/dL nhưng chọn nhầm mmol/L."
            )
            return TriageRiskStatus.ANOMALY_MONITOR, factors

        display_str = (
            f"{mg_dl_val} mg/dL (~{mmol_val} mmol/L)"
            if is_mg_dl
            else f"{mmol_val} mmol/L (~{mg_dl_val} mg/dL)"
        )

        # 1. Hạ đường huyết (Hypoglycemia < 3.5 mmol/L)
        if mmol_val < GLUCOSE_HYPOGLYCEMIA_THRESHOLD:
            factors.append(
                f"Hạ đường huyết ({display_str}, chuẩn an toàn >= 3.5 mmol/L) - Cần bổ sung ngay nước đường/nước trái cây và theo dõi"
            )
            return TriageRiskStatus.ANOMALY_MONITOR, factors

        # Chuẩn hóa ngữ cảnh đo
        context_str = ""
        if glucose_context is not None:
            context_str = str(glucose_context.value if hasattr(glucose_context, "value") else glucose_context).upper()
        elif is_fasting is not None:
            context_str = "FASTING" if is_fasting else "POST_MEAL_2H"
        else:
            context_str = "FASTING"

        # 2. Đánh giá theo từng ngữ cảnh đo cụ thể (6 loại Dropdown)
        # A. FASTING (Lúc đói - Chuẩn < 5.1 mmol/L)
        if context_str == "FASTING":
            if mmol_val >= GLUCOSE_FASTING_CRITICAL_THRESHOLD:
                factors.append(
                    f"Đường huyết lúc đói rất cao ({display_str}, chuẩn thai kỳ < 5.1 mmol/L) - Nguy cơ đái tháo đường thai kỳ mức độ nặng"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors
            elif mmol_val >= GLUCOSE_FASTING_WARNING_THRESHOLD:
                factors.append(
                    f"Đường huyết lúc đói tăng ({display_str}, chuẩn an toàn thai kỳ < 5.1 mmol/L / < 92 mg/dL theo QĐ 3494/BYT)"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        # B. PRE_MEAL (Trước ăn - Chuẩn < 5.3 mmol/L)
        elif context_str == "PRE_MEAL":
            if mmol_val >= GLUCOSE_PRE_MEAL_WARNING_THRESHOLD:
                factors.append(
                    f"Đường huyết trước ăn tăng ({display_str}, chuẩn thai kỳ ADA < 5.3 mmol/L / < 95 mg/dL)"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        # C. POST_MEAL_1H (Sau ăn 1 giờ - Chuẩn < 7.8 mmol/L)
        elif context_str == "POST_MEAL_1H":
            if mmol_val >= GLUCOSE_POST_MEAL_1H_CRITICAL_THRESHOLD:
                factors.append(
                    f"Đường huyết sau ăn 1 giờ rất cao ({display_str}, chuẩn thai kỳ < 7.8 mmol/L / < 140 mg/dL)"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors
            elif mmol_val >= GLUCOSE_POST_MEAL_1H_WARNING_THRESHOLD:
                factors.append(
                    f"Đường huyết sau ăn 1 giờ tăng ({display_str}, chuẩn an toàn < 7.8 mmol/L / < 140 mg/dL theo ADA/ACOG)"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        # D. POST_MEAL_2H (Sau ăn 2 giờ - Chuẩn < 6.7 mmol/L / OGTT < 8.5 mmol/L)
        elif context_str == "POST_MEAL_2H":
            if mmol_val >= GLUCOSE_POSTPRANDIAL_CRITICAL_THRESHOLD:
                factors.append(
                    f"Đường huyết sau ăn 2 giờ rất cao ({display_str}, chuẩn thai kỳ < 8.5 mmol/L)"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors
            elif mmol_val >= GLUCOSE_POSTPRANDIAL_WARNING_THRESHOLD:
                factors.append(
                    f"Đường huyết sau ăn 2 giờ tăng ({display_str}, chuẩn an toàn < 8.5 mmol/L / < 153 mg/dL theo QĐ 3494/BYT)"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors
            elif mmol_val >= GLUCOSE_POST_MEAL_2H_WARNING_THRESHOLD:
                factors.append(
                    f"Đường huyết sau ăn 2 giờ hơi cao so với mức kiểm soát tối ưu ({display_str}, mục tiêu < 6.7 mmol/L / < 120 mg/dL)"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        # E. RANDOM / OTHER_APPROVED (Ngẫu nhiên - Chuẩn < 11.1 mmol/L)
        else:
            if mmol_val >= GLUCOSE_RANDOM_CRITICAL_THRESHOLD:
                factors.append(
                    f"Đường huyết ngẫu nhiên rất cao ({display_str}, chuẩn an toàn < 11.1 mmol/L / < 200 mg/dL) - Cần tầm soát ĐTĐ thai kỳ"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors
            elif mmol_val >= GLUCOSE_POSTPRANDIAL_WARNING_THRESHOLD:
                factors.append(
                    f"Đường huyết ngẫu nhiên tăng nhẹ ({display_str}, chuẩn < 8.5 mmol/L)"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    # -------------------------------------------------------------------------
    # 4. Thai máy (Fetal Movements Check)
    # Nguồn y khoa: ACOG Committee Opinion No. 828 (2021) & RCOG Green-top No. 57
    # -------------------------------------------------------------------------
    def _check_fetal_movements(
        self,
        count: int | None,
        duration_hours: int | None,
        weeks: int | None,
        stage: MaternalStage = MaternalStage.PREGNANCY,
    ) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []
        if count is None:
            return TriageRiskStatus.NORMAL, factors

        # Validate giai đoạn & tuần thai:
        # Nếu mẹ ở giai đoạn Chưa mang thai (PRECONCEPTION) hoặc Sau sinh (POSTPARTUM)
        if stage in (MaternalStage.PRECONCEPTION, MaternalStage.POSTPARTUM):
            return TriageRiskStatus.NORMAL, factors

        # Nếu tuần thai còn nhỏ (< 24 tuần), thai nhi chưa có chu kỳ máy đều đặn
        if weeks and weeks < FETAL_MONITORING_EARLIEST_VALID_WEEK:
            return TriageRiskStatus.NORMAL, factors

        # Đánh giá cử động thai từ tuần 28 trở đi (hoặc từ tuần 24 nếu có số liệu)
        if weeks and weeks >= FETAL_MONITORING_MIN_GESTATIONAL_WEEK:
            duration = duration_hours or FETAL_DEFAULT_DURATION_HOURS
            if count == FETAL_CRITICAL_ZERO_KICKS:
                factors.append(
                    f"Thai không cử động suốt {duration} giờ (Tuần thai {weeks}) - Nguy cơ suy thai cấp (Cần đến ngay bệnh viện Sản)"
                )
                return TriageRiskStatus.CRITICAL_EMERGENCY, factors
            elif count < FETAL_MIN_KICKS_2H_THRESHOLD and duration >= FETAL_DEFAULT_DURATION_HOURS:
                factors.append(
                    f"Thai cử động yếu (chỉ {count} lần/{duration}h, chuẩn tối thiểu >= {FETAL_MIN_KICKS_2H_THRESHOLD} lần/2h theo ACOG/RCOG)"
                )
                return TriageRiskStatus.CRITICAL_EMERGENCY, factors
            elif count < FETAL_MIN_KICKS_4H_THRESHOLD and duration >= 4:
                factors.append(f"Cử động thai giảm ({count} lần/{duration}h, chuẩn >= 10 lần/4h)")
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    # -------------------------------------------------------------------------
    # 5. Triệu chứng báo động đỏ (Critical & Alarm Symptoms)
    # Nguồn y khoa: Hướng dẫn Chăm sóc Thiết yếu Sản phụ khoa Bộ Y Tế
    # -------------------------------------------------------------------------
    def _check_symptoms(
        self, symptoms: List[str] | None, notes: str | None
    ) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []
        combined = (" ".join(symptoms or []) + " " + (notes or "")).lower()

        # Severe Red Flags
        for kw, desc in CRITICAL_SYMPTOMS_CATALOG:
            if kw in combined:
                factors.append(f"Triệu chứng khẩn cấp: {desc}")
                return TriageRiskStatus.CRITICAL_EMERGENCY, factors

        # Mild / Common anomalies
        for kw, desc in MILD_SYMPTOMS_CATALOG:
            if kw in combined:
                factors.append(f"Triệu chứng cần lưu ý: {desc}")

        if factors:
            return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    # -------------------------------------------------------------------------
    # 6. BMI theo Giai đoạn Hành trình
    # Nguồn y khoa: IOM Pregnancy Weight Gain Guidelines (2009) & WHO Asian IDI/WPRO
    # -------------------------------------------------------------------------
    def _check_bmi(
        self,
        bmi: float | None,
        weight_kg: float | None,
        height_cm: float | None,
        stage: MaternalStage = MaternalStage.PREGNANCY,
        weeks: int | None = None,
    ) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []
        val_bmi = bmi
        if val_bmi is None and weight_kg is not None and height_cm is not None and height_cm > 0:
            val_bmi = weight_kg / ((height_cm / 100) ** 2)

        if val_bmi is None:
            return TriageRiskStatus.NORMAL, factors

        # Giai đoạn 1: PRECONCEPTION (Chuẩn bị mang thai - Đo theo chuẩn WHO Châu Á)
        if stage == MaternalStage.PRECONCEPTION:
            if val_bmi >= BMI_OBESE_THRESHOLD or val_bmi >= 25.0:
                factors.append(
                    f"Thể trạng béo phì trước mang thai (BMI: {val_bmi:.1f} kg/m²) - Khuyến nghị tư vấn dinh dưỡng và giảm cân an toàn trước khi thụ thai để phòng ngừa tiểu đường thai kỳ và tiền sản giật"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors
            elif val_bmi > BMI_ASIAN_NORMAL_MAX:
                factors.append(
                    f"Thể trạng thừa cân trước mang thai (BMI: {val_bmi:.1f} kg/m², chuẩn tối ưu 18.5 - 22.9 kg/m² theo WHO Châu Á)"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors
            elif val_bmi < BMI_ASIAN_UNDERWEIGHT_MAX:
                factors.append(
                    f"Thể trạng thiếu cân trước mang thai (BMI: {val_bmi:.1f} kg/m²) - Khuyên bổ sung dinh dưỡng để đạt thể trạng tốt nhất trước khi có thai"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        # Giai đoạn 2: PREGNANCY (Đang mang thai - Đánh giá mức độ tăng cân theo IOM 2009)
        elif stage == MaternalStage.PREGNANCY:
            if val_bmi >= BMI_SEVERELY_OBESE_THRESHOLD:
                factors.append(
                    f"Béo phì độ III rất nặng (BMI: {val_bmi:.1f} kg/m²) - Nguy cơ cao Tiền sản giật, ĐTĐ thai kỳ và Thuyên tắc huyết khối"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors
            elif val_bmi >= BMI_OBESE_THRESHOLD:
                factors.append(
                    f"Béo phì thai kỳ (BMI: {val_bmi:.1f} kg/m²) - Cần kiểm soát mức tăng cân theo chuẩn IOM (5-9 kg suốt thai kỳ)"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors
            elif val_bmi >= BMI_OVERWEIGHT_THRESHOLD:
                factors.append(
                    f"Thừa cân thai kỳ (BMI: {val_bmi:.1f} kg/m²) - Khuyến nghị tăng 7-11.5 kg suốt thai kỳ theo chuẩn IOM"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors
            elif val_bmi < BMI_UNDERWEIGHT_THRESHOLD:
                factors.append(
                    f"Thiếu cân thai kỳ (BMI: {val_bmi:.1f} kg/m²) - Cần bồi bổ đạt mức tăng 12.5-18 kg để phòng ngừa suy dinh dưỡng bào thai"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        # Giai đoạn 3: POSTPARTUM (Sau sinh / Nuôi con bú)
        elif stage == MaternalStage.POSTPARTUM:
            if val_bmi >= BMI_OBESE_THRESHOLD:
                factors.append(
                    f"Thể trạng sau sinh thừa cân/béo phì (BMI: {val_bmi:.1f} kg/m²) - Lưu ý giảm cân an toàn, không ăn kiêng cực đoan gây thiếu hụt nguồn sữa mẹ"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors
            elif val_bmi < BMI_UNDERWEIGHT_THRESHOLD:
                factors.append(
                    f"Thể trạng sau sinh thiếu cân (BMI: {val_bmi:.1f} kg/m²) - Cần tăng cường dinh dưỡng để đảm bảo năng lượng và chất lượng sữa cho bé"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    # -------------------------------------------------------------------------
    # 7. Nhịp tim mẹ (Maternal Heart Rate Check)
    # Nguồn y khoa: ESC Guidelines on Cardiovascular Diseases during Pregnancy
    # -------------------------------------------------------------------------
    def _check_heart_rate(self, hr: int | None) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []
        if hr is None:
            return TriageRiskStatus.NORMAL, factors

        if hr >= HR_CRITICAL_HIGH_THRESHOLD:
            factors.append(f"Nhịp tim mẹ tăng rất nhanh ({hr} bpm) - Nguy cơ rối loạn nhịp tim cấp, thiếu máu nặng hoặc sốc")
            return TriageRiskStatus.CRITICAL_EMERGENCY, factors
        elif hr >= HR_ELEVATED_HIGH_THRESHOLD:
            factors.append(f"Nhịp tim mẹ hơi nhanh ({hr} bpm) so với sinh lý bình thường (60-100 bpm)")
            return TriageRiskStatus.ANOMALY_MONITOR, factors
        elif hr < HR_BRADYCARDIA_LOW_THRESHOLD:
            factors.append(f"Nhịp tim mẹ chậm ({hr} bpm, chuẩn >= 50 bpm)")
            return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    # -------------------------------------------------------------------------
    # 8. Lượng nước uống theo Giai đoạn (Water Intake Check)
    # Nguồn y khoa: Khuyến nghị Viện Dinh Dưỡng Quốc Gia & WHO
    # -------------------------------------------------------------------------
    def _check_water_intake(
        self,
        water_ml: int | None,
        stage: MaternalStage = MaternalStage.PREGNANCY,
    ) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []
        if water_ml is None:
            return TriageRiskStatus.NORMAL, factors

        # Kiểm tra theo từng giai đoạn sinh lý
        if stage == MaternalStage.PRECONCEPTION:
            if water_ml < WATER_PRECONCEPTION_MIN_ML:
                factors.append(
                    f"Lượng nước uống chưa đủ ({water_ml} ml/ngày, chuẩn khuyến nghị 1500-2000 ml/ngày)"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        elif stage == MaternalStage.PREGNANCY:
            if water_ml < WATER_PREGNANCY_CRITICAL_LOW_ML:
                factors.append(
                    f"Lượng nước uống rất ít ({water_ml} ml/ngày, chuẩn thai kỳ 2000-2500 ml) - Nguy cơ mất nước, thiểu ối và táo bón thai kỳ"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors
            elif water_ml < WATER_PREGNANCY_MIN_ML:
                factors.append(
                    f"Lượng nước uống chưa đủ ({water_ml} ml/ngày, cần bổ sung đạt 2000-2500 ml/ngày để hỗ trợ tái tạo nước ối)"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        elif stage == MaternalStage.POSTPARTUM:
            if water_ml < WATER_POSTPARTUM_MIN_ML:
                factors.append(
                    f"Lượng nước uống sau sinh chưa đủ ({water_ml} ml/ngày, chuẩn mẹ cho con bú 2500-3000 ml/ngày vì sữa mẹ chứa 88% nước)"
                )
                return TriageRiskStatus.ANOMALY_MONITOR, factors

        if water_ml >= WATER_EXCESSIVE_ML:
            factors.append(
                f"Lượng nước uống quá nhiều ({water_ml} ml/ngày) - Cần tầm soát đái tháo đường thai kỳ hoặc hội chứng đa niệu"
            )
            return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    # -------------------------------------------------------------------------
    # 9. Sàng lọc Trầm cảm EPDS
    # Nguồn y khoa: Edinburgh Postnatal Depression Scale (Cox et al.) & COPE / NSW Health
    # -------------------------------------------------------------------------
    def _check_epds_score(
        self,
        score: int | None,
        question_10_score: int | None = None,
    ) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []

        # CỜ ĐỎ KHẨN CẤP: Câu hỏi số 10 EPDS (Ý nghĩ tự gây hại / tự sát theo chuẩn COPE / NSW Health / ACOG)
        if question_10_score is not None and question_10_score >= EPDS_QUESTION_10_RED_FLAG_THRESHOLD:
            factors.append(
                f"Cảnh báo an toàn tâm lý khẩn cấp: Câu hỏi số 10 EPDS đạt {question_10_score}/3 điểm (Xuất hiện ý nghĩ tự gây hại/tự sát) - Cần liên hệ cấp cứu 115 và tìm hỗ trợ y tế khẩn cấp ngay lập tức"
            )
            return TriageRiskStatus.CRITICAL_EMERGENCY, factors

        if score is None:
            return TriageRiskStatus.NORMAL, factors

        if score >= EPDS_HIGH_RISK_DEPRESSION_THRESHOLD:
            factors.append(
                f"Điểm sàng lọc trầm cảm EPDS rất cao ({score}/30 điểm) - Nguy cơ trầm cảm thai kỳ / sau sinh mức độ nặng (Cần được bác sĩ hoặc chuyên gia tâm lý đánh giá chuyên sâu)"
            )
            return TriageRiskStatus.ANOMALY_MONITOR, factors
        elif score >= EPDS_MILD_RISK_DEPRESSION_THRESHOLD:
            factors.append(
                f"Điểm sàng lọc EPDS ở mức rủi ro ({score}/30 điểm) - Dấu hiệu lo âu / trầm cảm nhẹ đến trung bình (Cần theo dõi sát và sàng lọc lại sau 2-4 tuần)"
            )
            return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    # -------------------------------------------------------------------------
    # 10. SpO2 Oxy máu & Giấc ngủ
    # -------------------------------------------------------------------------
    def _check_spo2(self, spo2: int | None) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []
        if spo2 is None:
            return TriageRiskStatus.NORMAL, factors

        if spo2 < SPO2_CRITICAL_LOW_THRESHOLD:
            factors.append(
                f"Độ bão hòa oxy SpO2 rất thấp ({spo2}%) - Suy hô hấp cấp, nguy cơ thiếu oxy bào thai nghiêm trọng"
            )
            return TriageRiskStatus.CRITICAL_EMERGENCY, factors
        elif spo2 < SPO2_WARNING_LOW_THRESHOLD:
            factors.append(f"Độ bão hòa oxy SpO2 giảm ({spo2}%, chuẩn >= 95%)")
            return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    def _check_sleep(self, sleep_hours: float | None) -> Tuple[TriageRiskStatus, List[str]]:
        factors = []
        if sleep_hours is None:
            return TriageRiskStatus.NORMAL, factors

        if sleep_hours < SLEEP_MIN_HOURS_THRESHOLD:
            factors.append(
                f"Thời gian ngủ quá ít ({sleep_hours:.1f} giờ/ngày) - Mất ngủ kéo dài gây kiệt sức và căng thẳng thai kỳ"
            )
            return TriageRiskStatus.ANOMALY_MONITOR, factors

        return TriageRiskStatus.NORMAL, factors

    # -------------------------------------------------------------------------
    # 11. Xây dựng truy vấn tìm kiếm Vector RAG
    # -------------------------------------------------------------------------
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
            if bmi_val >= BMI_OVERWEIGHT_THRESHOLD:
                parts.append("thừa cân béo phì dinh dưỡng thai kỳ")
            elif bmi_val < BMI_UNDERWEIGHT_THRESHOLD:
                parts.append("thiếu cân suy dinh dưỡng thai kỳ")
        if request.fetal_movements_count is not None:
            parts.append(f"cử động thai {request.fetal_movements_count} lần")
        if request.water_intake_ml is not None and request.water_intake_ml < WATER_PREGNANCY_MIN_ML:
            parts.append("uống ít nước bổ sung nước thai kỳ thiểu ối")
        if request.epds_score is not None and request.epds_score >= EPDS_MILD_RISK_DEPRESSION_THRESHOLD:
            parts.append("trầm cảm tâm lý lo âu thai kỳ sau sinh")
        if request.temperature:
            if request.temperature >= TEMP_CRITICAL_FEVER_PREGNANCY:
                parts.append(f"sốt cao {request.temperature} độ hạ sốt khẩn cấp nhiễm trùng ối hậu sản")
            elif request.temperature >= TEMP_MILD_FEVER_THRESHOLD:
                parts.append(f"sốt nhẹ {request.temperature} độ cách hạ sốt an toàn paracetamol thai kỳ")
            elif request.temperature < TEMP_HYPOTHERMIA_THRESHOLD:
                parts.append(f"hạ thân nhiệt {request.temperature} độ")
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
