"""Constants and Enums for Vital Signs & Health Metrics Screening.

Comprehensive clinical thresholds and classifications based on:
1. Bộ Y Tế Việt Nam - Quyết định 1154/QĐ-BYT (ngày 04/05/2024 ban hành "Hướng dẫn sàng lọc, chẩn đoán và xử trí tăng huyết áp ở phụ nữ mang thai, tiền sản giật và sản giật")
   https://thuvienphapluat.vn/van-ban/The-thao-Y-te/Quyet-dinh-1154-QD-BYT-2024-tai-lieu-Huong-dan-xu-tri-tang-huyet-ap-o-phu-nu-mang-thai-643196.aspx
2. Bộ Y Tế Việt Nam - Quyết định 1470/QĐ-BYT (ngày 29/05/2024 ban hành "Hướng dẫn quốc gia về sàng lọc và quản lý đái tháo đường thai kỳ")
   https://bvdkbaclieu.gov.vn/van-ban-phap-quy/quyet-dinh-1470-qd-byt-cua-bo-y-te-ve-viec-ban-hanh-tai-lieu.html
3. Bộ Y Tế Việt Nam - Quyết định 1359/QĐ-BYT (Hướng dẫn Quốc gia về Chăm sóc Sức khỏe Sinh sản & Nhiễm trùng Hậu sản)
4. ACOG Practice Bulletin No. 222: Gestational Hypertension and Preeclampsia (Obstet Gynecol 2020)
   https://pubmed.ncbi.nlm.nih.gov/32443079/
5. RCOG Green-top Guideline No. 57: Reduced Fetal Movements & ACOG Practice Bulletin No. 229
   https://www.rcog.org.uk/guidance/browse-all-guidance/green-top-guidelines/reduced-fetal-movements-green-top-guideline-no-57/
   https://pubmed.ncbi.nlm.nih.gov/34011892/
6. ADA Standards of Care in Diabetes: Management of Diabetes in Pregnancy (2024) / IADPSG Criteria
   https://diabetesjournals.org/care/article/47/Supplement_1/S282/153957/15-Management-of-Diabetes-in-Pregnancy-Standards
7. WHO Guidelines for the Prevention and Treatment of Maternal Peripartum Infections & Sepsis
   https://apps.who.int/iris/handle/10665/186171
   https://pubmed.ncbi.nlm.nih.gov/26512398/
8. Institute of Medicine (IOM) & NRC Guidelines: Weight Gain During Pregnancy (2009)
   https://www.ncbi.nlm.nih.gov/books/NBK32813/
9. WHO Expert Consultation: Appropriate body-mass index for Asian populations (Lancet 2004; 363: 157-163)
   https://pubmed.ncbi.nlm.nih.gov/14726461/
10. Edinburgh Postnatal Depression Scale (EPDS) - Cox et al. (1987) & COPE / NSW Health Guidelines
    https://www.cope.org.au/health-professionals/health-professional-guidelines/
"""

from __future__ import annotations

from enum import Enum


class VitalSignType(str, Enum):
    """Categorization of maternal vital sign indicators."""
    BLOOD_PRESSURE = "BLOOD_PRESSURE"
    BODY_TEMPERATURE = "BODY_TEMPERATURE"
    BLOOD_GLUCOSE = "BLOOD_GLUCOSE"
    FETAL_MOVEMENT = "FETAL_MOVEMENT"
    HEART_RATE = "HEART_RATE"
    BMI = "BMI"
    SPO2 = "SPO2"
    WATER_INTAKE = "WATER_INTAKE"
    SLEEP = "SLEEP"
    EPDS_MOOD = "EPDS_MOOD"


class GlucoseMeasurementContext(str, Enum):
    """Contexts for blood glucose measurement matching the Mobile/Web Dropdown.
    Căn cứ: ADA Standards of Care in Diabetes (2024) & Quyết định 3494/QĐ-BYT.
    """
    FASTING = "FASTING"                 # Lúc đói (sau nhịn ăn đêm ít nhất 8 tiếng)
    PRE_MEAL = "PRE_MEAL"               # Trước các bữa ăn chính
    POST_MEAL_1H = "POST_MEAL_1H"       # Sau ăn 1 giờ (tính từ miếng ăn đầu tiên)
    POST_MEAL_2H = "POST_MEAL_2H"       # Sau ăn 2 giờ
    RANDOM = "RANDOM"                   # Ngẫu nhiên bất kỳ thời điểm nào trong ngày
    OTHER_APPROVED = "OTHER_APPROVED"   # Khác (theo chỉ định riêng của Bác sĩ điều trị)


class BloodPressureCategory(str, Enum):
    """ACOG clinical blood pressure categories in pregnancy (ACOG PB 222)."""
    NORMAL = "NORMAL"
    ELEVATED = "ELEVATED"
    STAGE_1_HYPERTENSION = "STAGE_1_HYPERTENSION"
    STAGE_2_SEVERE_HYPERTENSION = "STAGE_2_SEVERE_HYPERTENSION"


class TemperatureCategory(str, Enum):
    """Maternal temperature state classification (WHO Sepsis & QĐ 1359/BYT)."""
    HYPOTHERMIA = "HYPOTHERMIA"
    NORMAL = "NORMAL"
    MILD_FEVER = "MILD_FEVER"
    MODERATE_FEVER = "MODERATE_FEVER"
    HIGH_FEVER_CRITICAL = "HIGH_FEVER_CRITICAL"


class BMICategory(str, Enum):
    """WHO Asian BMI category classification."""
    UNDERWEIGHT = "UNDERWEIGHT"
    NORMAL = "NORMAL"
    OVERWEIGHT = "OVERWEIGHT"
    OBESE = "OBESE"
    SEVERELY_OBESE = "SEVERELY_OBESE"


# ============================================================================
# 1. Huyết áp (Blood Pressure) - Căn cứ: ACOG PB 222 & QĐ 1154/QĐ-BYT 
# SBP (Systolic Blood Pressure) – Huyết áp Tâm thu
# DBP (Diastolic Blood Pressure) – Huyết áp Tâm trương
# ============================================================================
BP_CRITICAL_SYSTOLIC = 160            # SBP >= 160 mmHg: Cơn tăng huyết áp nặng / Đột quỵ / Tiền sản giật nặng
BP_CRITICAL_DIASTOLIC = 110           # DBP >= 110 mmHg: Nguy kịch khẩn cấp
BP_STAGE1_SYSTOLIC = 140              # SBP >= 140 mmHg: Tăng HA thai kỳ / Tiền sản giật (khi có triệu chứng)
BP_STAGE1_DIASTOLIC = 90              # DBP >= 90 mmHg
BP_ELEVATED_SYSTOLIC_MIN = 130        # SBP 130-139 mmHg: Tiền tăng huyết áp cần theo dõi
BP_ELEVATED_DIASTOLIC_MIN = 85        # DBP 85-89 mmHg

# ============================================================================
# 2. Thân nhiệt (Temperature) - Căn cứ: WHO Sepsis & QĐ 1359/QĐ-BYT
# ============================================================================
TEMP_HYPOTHERMIA_THRESHOLD = 35.5     # T < 35.5°C: Hạ thân nhiệt
TEMP_MILD_FEVER_THRESHOLD = 37.5      # T >= 37.5°C: Sốt nhẹ cần theo dõi
TEMP_MODERATE_FEVER_THRESHOLD = 38.0  # T >= 38.0°C: Nguy cơ nhiễm trùng hậu sản (Postpartum) hoặc kèm cờ đỏ
TEMP_CRITICAL_FEVER_PREGNANCY = 38.5  # T >= 38.5°C: Sốt cao thai kỳ (Nhiễm trùng ối Chorioamnionitis)
TEMP_CRITICAL_FEVER_POSTPARTUM = 38.0 # T >= 38.0°C: Sốt hậu sản (Nhiễm trùng hậu sản Puerperal Sepsis)
TEMP_CRITICAL_FEVER_GENERAL = 39.0    # T >= 39.0°C: Sốt cao nguy hiểm chung

# ============================================================================
# 3. Đường huyết (Blood Glucose) - Căn cứ: ADA (2024)
# ============================================================================
GLUCOSE_UNIT_CONVERSION_FACTOR = 18.0182  # 1 mmol/L = 18.0182 mg/dL
GLUCOSE_UNIT_AUTODETECT_MG_DL_THRESHOLD = 25.0

# Hạ đường huyết (Hypoglycemia) chung cho mọi thời điểm
GLUCOSE_HYPOGLYCEMIA_THRESHOLD = 3.5  # < 3.5 mmol/L (< 63 mg/dL): Hạ đường huyết cần xử trí bổ sung carbohydrate

# Ngưỡng lúc đói (Fasting) / Trước ăn (Pre-meal): Chuẩn thai kỳ < 5.1 mmol/L (< 92 mg/dL)
GLUCOSE_FASTING_NORMAL_MAX = 5.0      # Bình thường: < 5.1 mmol/L
GLUCOSE_FASTING_WARNING_THRESHOLD = 5.1  # >= 5.1 mmol/L: ĐTĐ thai kỳ (GDM)
GLUCOSE_FASTING_CRITICAL_THRESHOLD = 7.0 # >= 7.0 mmol/L: ĐTĐ lâm sàng nặng (Overt Diabetes)

# Ngưỡng trước ăn (Pre-meal): Chuẩn thai kỳ < 5.3 mmol/L (< 95 mg/dL)
GLUCOSE_PRE_MEAL_WARNING_THRESHOLD = 5.3

# Ngưỡng sau ăn 1 giờ (Post-meal 1h): Chuẩn thai kỳ < 7.8 mmol/L (< 140 mg/dL)
GLUCOSE_POST_MEAL_1H_WARNING_THRESHOLD = 7.8
GLUCOSE_POST_MEAL_1H_CRITICAL_THRESHOLD = 10.0

# Ngưỡng sau ăn 2 giờ (Post-meal 2h): Chuẩn thai kỳ < 6.7 mmol/L (< 120 mg/dL) hoặc OGTT < 8.5 mmol/L
GLUCOSE_POST_MEAL_2H_WARNING_THRESHOLD = 6.7
GLUCOSE_POST_MEAL_2H_OGTT_THRESHOLD = 8.5
GLUCOSE_POST_MEAL_2H_CRITICAL_THRESHOLD = 11.1

# Ngưỡng ngẫu nhiên (Random): Chuẩn < 11.1 mmol/L (< 200 mg/dL)
GLUCOSE_RANDOM_CRITICAL_THRESHOLD = 11.1

# Ngưỡng tương thích ngược
GLUCOSE_POSTPRANDIAL_WARNING_THRESHOLD = 8.5
GLUCOSE_POSTPRANDIAL_CRITICAL_THRESHOLD = 11.1

# ============================================================================
# 4. Thai máy (Fetal Movements / Kicks) - Căn cứ: ACOG Opinion 828 & RCOG No. 57
# ============================================================================
FETAL_MONITORING_MIN_GESTATIONAL_WEEK = 28  # Khuyến nghị đếm thai máy định kỳ từ tuần 28
FETAL_MONITORING_EARLIEST_VALID_WEEK = 24  # Tuần thai sớm nhất có thể cảm nhận cử động đều đặn
FETAL_DEFAULT_DURATION_HOURS = 2
FETAL_CRITICAL_ZERO_KICKS = 0              # 0 cử động trong 2h: Nguy cơ suy thai cấp khẩn cấp
FETAL_MIN_KICKS_2H_THRESHOLD = 4           # Tối thiểu 4 lần cử động / 2 giờ liên tục
FETAL_MIN_KICKS_4H_THRESHOLD = 10          # Tối thiểu 10 lần cử động / 4 giờ

# ============================================================================
# 5. BMI theo Giai đoạn Hành trình - Căn cứ: IOM (2009) & WHO Asian IDI/WPRO
# ============================================================================
# Chuẩn WHO Châu Á (Áp dụng cho Preconception & Người lớn):
BMI_ASIAN_UNDERWEIGHT_MAX = 18.5
BMI_ASIAN_NORMAL_MAX = 22.9
BMI_ASIAN_OVERWEIGHT_MAX = 24.9
BMI_ASIAN_OBESE_MIN = 25.0

# Chuẩn quốc tế chung:
BMI_UNDERWEIGHT_THRESHOLD = 18.5
BMI_OVERWEIGHT_THRESHOLD = 25.0
BMI_OBESE_THRESHOLD = 30.0
BMI_SEVERELY_OBESE_THRESHOLD = 40.0        # Béo phì độ III: Nguy cơ huyết khối & tiền sản giật nặng

# ============================================================================
# 6. Nhịp tim mẹ (Heart Rate) - Căn cứ: ESC Cardiovascular in Pregnancy (2018)
# ============================================================================
HR_CRITICAL_HIGH_THRESHOLD = 120           # >= 120 bpm: Nhịp tim nhanh kịch phát, thiếu máu nặng hoặc sốc
HR_ELEVATED_HIGH_THRESHOLD = 100           # >= 100 bpm: Nhịp nhanh sinh lý / cần theo dõi
HR_BRADYCARDIA_LOW_THRESHOLD = 50          # < 50 bpm: Nhịp chậm, nguy cơ block tim / choáng ngất

# ============================================================================
# 7. Lượng nước uống theo Giai đoạn (ml/ngày) - Căn cứ: Viện Dinh Dưỡng & WHO
# ============================================================================
# Giai đoạn PRECONCEPTION (Chuẩn bị mang thai): 1500 - 2000 ml
WATER_PRECONCEPTION_MIN_ML = 1500
WATER_PRECONCEPTION_REC_ML = 2000

# Giai đoạn PREGNANCY (Mang thai - Tạo ối & Tăng tuần hoàn): 2000 - 2500 ml
WATER_PREGNANCY_CRITICAL_LOW_ML = 1200     # < 1200 ml: Mất nước, thiểu ối, táo bón
WATER_PREGNANCY_MIN_ML = 1800
WATER_PREGNANCY_REC_ML = 2500

# Giai đoạn POSTPARTUM (Nuôi con bú - Tiết sữa mẹ 88% nước): 2500 - 3000 ml
WATER_POSTPARTUM_MIN_ML = 2200
WATER_POSTPARTUM_REC_ML = 3000

WATER_CRITICAL_LOW_ML = 1200
WATER_RECOMMENDED_MIN_ML = 1800
WATER_EXCESSIVE_ML = 4500                  # >= 4500 ml: Nghi ngờ đa niệu hoặc đái tháo đường

# ============================================================================
# 8. Sàng lọc Trầm cảm EPDS - Căn cứ: Cox et al. (1987) & COPE / NSW Health
# ============================================================================
EPDS_QUESTION_10_RED_FLAG_THRESHOLD = 1    # Câu 10 >= 1: Có ý nghĩ tự gây hại / tự sát -> Cấp cứu an toàn tâm lý
EPDS_HIGH_RISK_DEPRESSION_THRESHOLD = 13   # >= 13 điểm: Trầm cảm mức độ nặng
EPDS_MILD_RISK_DEPRESSION_THRESHOLD = 10   # >= 10 điểm: Nguy cơ trầm cảm nhẹ/trung bình

# ============================================================================
# 9. SpO2 Oxy máu (%) - Căn cứ: Chuẩn Hồi sức Cấp cứu Sản khoa
# ============================================================================
SPO2_CRITICAL_LOW_THRESHOLD = 92           # < 92%: Suy hô hấp cấp, nguy cơ thiếu oxy bào thai nặng
SPO2_WARNING_LOW_THRESHOLD = 95            # < 95%: Giảm oxy mô

# ============================================================================
# 10. Giấc ngủ (Giờ/ngày)
# ============================================================================
SLEEP_MIN_HOURS_THRESHOLD = 5.0

# ============================================================================
# 11. Giới hạn Sinh lý Y tế Hợp lý (Plausibility / Sanity Validation Ranges)
# Ngăn chặn lỗi nhập liệu phi lý y khoa (như Glucose 300 mmol/L, HA 500 mmHg...)
# ============================================================================
SANITY_RANGES = {
    "systolic_bp": (50, 260),              # SBP từ 50 đến 260 mmHg
    "diastolic_bp": (30, 160),             # DBP từ 30 đến 160 mmHg
    "glucose_mmol_l": (1.0, 35.0),         # mmol/L: Tối đa trên máy đo lâm sàng ~33.3-35.0 mmol/L
    "glucose_mg_dl": (20.0, 600.0),        # mg/dL: Tối đa trên máy đo ~600 mg/dL
    "temperature": (34.0, 43.0),           # °C: Cặp nhiệt độ lâm sàng sống 34.0 - 43.0°C
    "heart_rate": (30, 220),               # bpm
    "fetal_kicks_session": (0, 60),        # Lần/phiên (tối đa ~60 lần trong 2h)
    "spo2": (50, 100),                     # %
    "weight_kg": (25.0, 250.0),            # kg
    "height_cm": (100.0, 250.0),           # cm
    "water_ml": (0, 10000),                # ml/ngày
    "sleep_hours": (0.0, 24.0),            # giờ/ngày
}

# ============================================================================
# Danh mục Từ khóa Báo động Lâm sàng (Alarm Keywords Catalog)
# ============================================================================
PREECLAMPSIA_ALARM_KEYWORDS = (
    "đau đầu", "hoa mắt", "nhìn mờ", "chóng mặt", "đau thượng vị",
    "đau hạ sườn", "phù mặt", "headache", "blurred vision", "dizziness", "epigastric",
)

TEMPERATURE_ALARM_KEYWORDS = (
    "đau bụng", "rỉ ối", "vỡ ối", "ra máu", "chảy máu", "sản dịch hôi",
    "cương tức vú", "đau ngực", "mệt lả", "rét run", "chills",
)

CRITICAL_SYMPTOMS_CATALOG = [
    ("ra máu", "Ra máu âm đạo tươi lượng nhiều"),
    ("chảy máu", "Chảy máu âm đạo bất thường"),
    ("vỡ ối", "Rỉ ối / Vỡ ối sớm"),
    ("rỉ ối", "Rỉ nước ối"),
    ("co giật", "Co giật / Tiền sản giật giật"),
    ("đau bụng dữ dội", "Đau bụng quặn dữ dội liên tục"),
    ("khó thở dữ dội", "Khó thở nghiêm trọng"),
]

MILD_SYMPTOMS_CATALOG = [
    ("phù", "Phù nề chân/tay"),
    ("đau đầu", "Đau đầu / Nhức đầu"),
    ("đau lưng", "Đau mỏi lưng hông"),
    ("buồn nôn", "Ốm nghén / Buồn nôn"),
    ("chóng mặt", "Chóng mặt, choáng váng"),
    ("tiểu buốt", "Tiểu buốt / Tiểu rắt (nghi ngờ nhiễm trùng tiết niệu)"),
    ("ngứa", "Ngứa ngoài da / lòng bàn tay bàn chân"),
]
