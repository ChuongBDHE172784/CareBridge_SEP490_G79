from __future__ import annotations

import unicodedata
from uuid import uuid4

from app.gemini_client import GeminiClient
from app.risk_rules import apply_red_flag_rules
from app.schemas import ChildTriageRequest, IntakeQuestion
from app.symptom_normalizer import normalize_symptoms

MAX_QUESTIONS_PER_ROUND = 3
MAX_FOLLOWUP_ROUNDS = 3


QUESTION_BANK: dict[str, IntakeQuestion] = {
    "parentFreeText": IntakeQuestion(
        questionKey="parentFreeText",
        text="Bé đang có triệu chứng gì? Hãy mô tả ngắn gọn dấu hiệu bạn thấy.",
        answerType="TEXT",
    ),
    "childAgeMonths": IntakeQuestion(
        questionKey="childAgeMonths",
        text="Bé hiện bao nhiêu tháng tuổi?",
        answerType="NUMBER",
    ),
    "breathingStatus": IntakeQuestion(
        questionKey="breathingStatus",
        text="Bé có khó thở, thở rút lõm ngực hoặc tím tái không?",
        answerType="SINGLE_CHOICE",
        options=["Không", "Khó thở", "Thở rút lõm ngực", "Tím tái", "Không chắc"],
    ),
    "consciousnessStatus": IntakeQuestion(
        questionKey="consciousnessStatus",
        text="Bé có tỉnh táo không, hay lơ mơ/li bì/khó đánh thức?",
        answerType="SINGLE_CHOICE",
        options=["Tỉnh táo", "Lơ mơ", "Li bì", "Khó đánh thức", "Không chắc"],
    ),
    "seizure": IntakeQuestion(
        questionKey="seizure",
        text="Bé có co giật không?",
        answerType="BOOLEAN",
        options=["Không", "Có", "Không chắc"],
    ),
    "feedingStatus": IntakeQuestion(
        questionKey="feedingStatus",
        text="Bé có bú/uống được không?",
        answerType="SINGLE_CHOICE",
        options=["Bú/uống tốt", "Bú/uống kém", "Bỏ bú", "Không uống được", "Không chắc"],
    ),
    "temperatureC": IntakeQuestion(
        questionKey="temperatureC",
        text="Nhiệt độ cao nhất đo được là bao nhiêu độ C?",
        answerType="NUMBER",
    ),
    "dehydrationSigns": IntakeQuestion(
        questionKey="dehydrationSigns",
        text="Bé có dấu hiệu mất nước nào không?",
        answerType="MULTI_CHOICE",
        options=["Không", "Môi khô", "Tiểu ít", "Mắt trũng", "Khóc không có nước mắt", "Không chắc"],
    ),
    "vomiting": IntakeQuestion(
        questionKey="vomiting",
        text="Bé có nôn không? Nếu có, nôn ít hay nôn liên tục?",
        answerType="SINGLE_CHOICE",
        options=["Không", "Nôn ít", "Nôn liên tục", "Không chắc"],
    ),
    "diarrhea": IntakeQuestion(
        questionKey="diarrhea",
        text="Bé có tiêu chảy không?",
        answerType="SINGLE_CHOICE",
        options=["Không", "Nhẹ", "Nhiều lần", "Kèm mất nước", "Không chắc"],
    ),
    "duration": IntakeQuestion(
        questionKey="duration",
        text="Triệu chứng xuất hiện bao lâu?",
        answerType="SINGLE_CHOICE",
        options=["Dưới 1 ngày", "1-3 ngày", "3-7 ngày", "Hơn 1 tuần", "Không chắc"],
    ),
    "rash": IntakeQuestion(
        questionKey="rash",
        text="Bé có phát ban không?",
        answerType="SINGLE_CHOICE",
        options=["Không", "Phát ban nhẹ", "Phát ban kèm sốt cao", "Xấu nhanh", "Không chắc"],
    ),
}

QUESTION_PRIORITY = [
    "parentFreeText",
    "childAgeMonths",
    "breathingStatus",
    "consciousnessStatus",
    "seizure",
    "feedingStatus",
    "temperatureC",
    "dehydrationSigns",
    "vomiting",
    "diarrhea",
    "duration",
    "rash",
]


def new_session_id() -> str:
    return str(uuid4())


def merge_answers(intake: ChildTriageRequest, new_answers: dict[str, object]) -> ChildTriageRequest:
    data = intake.model_dump()
    for key, value in new_answers.items():
        if key not in data:
            continue
        data[key] = _coerce_answer(key, value)
    return ChildTriageRequest(**data)


def determine_missing_information(intake: ChildTriageRequest) -> list[str]:
    missing: list[str] = []
    if intake.childAgeMonths is None:
        missing.append("childAgeMonths")
    if _empty(intake.breathingStatus):
        missing.append("breathingStatus")
    if _empty(intake.consciousnessStatus):
        missing.append("consciousnessStatus")
    if intake.seizure is None:
        missing.append("seizure")
    if _empty(intake.feedingStatus):
        missing.append("feedingStatus")
    if intake.temperatureC is None and _has_fever_context(intake):
        missing.append("temperatureC")
    if _has_dehydration_or_diarrhea_context(intake) and not intake.dehydrationSigns:
        missing.append("dehydrationSigns")
    if _has_vomiting_context(intake) and _empty(intake.vomiting):
        missing.append("vomiting")
    if _has_diarrhea_context(intake) and _empty(intake.diarrhea):
        missing.append("diarrhea")
    if _empty(intake.duration):
        missing.append("duration")
    if _has_rash_context(intake) and _empty(intake.rash):
        missing.append("rash")
    if not intake.symptomList and _empty(intake.parentFreeText):
        missing.insert(0, "parentFreeText")
    return [key for key in QUESTION_PRIORITY if key in missing][:MAX_QUESTIONS_PER_ROUND]


def ask_followup_questions(intake: ChildTriageRequest) -> list[IntakeQuestion]:
    return [QUESTION_BANK[key] for key in determine_missing_information(intake) if key in QUESTION_BANK]


def naturalize_followup_questions(
    questions: list[IntakeQuestion],
    *,
    intake: ChildTriageRequest,
    normalized_symptoms: list[str],
    gemini_client: GeminiClient | None,
    deadline: float | None = None,
) -> tuple[list[IntakeQuestion], str, bool]:
    selected = questions[:MAX_QUESTIONS_PER_ROUND]
    fallback_message = "CareBridge cần thêm một vài thông tin để phân loại rủi ro an toàn hơn."
    if gemini_client is None or not selected:
        return selected, fallback_message, False
    draft = gemini_client.compose_followup_questions(
        questions=selected,
        child_age_months=intake.childAgeMonths,
        normalized_symptoms=normalized_symptoms,
        deadline=deadline,
    )
    if draft is None:
        return selected, fallback_message, False
    by_key = {item.questionKey: item for item in draft.questions}
    naturalized = [
        original.model_copy(update={"text": by_key[original.questionKey].text})
        for original in selected
    ]
    return naturalized, draft.assistantMessage, True


def has_red_flag(intake: ChildTriageRequest) -> bool:
    symptoms = normalize_symptoms(intake)
    red_flags, _ = apply_red_flag_rules(intake, symptoms)
    return bool(red_flags)


def should_ask_more(intake: ChildTriageRequest, round_number: int) -> bool:
    return not has_red_flag(intake) and round_number <= MAX_FOLLOWUP_ROUNDS and bool(ask_followup_questions(intake))


def reached_question_limit(round_number: int) -> bool:
    return round_number >= MAX_FOLLOWUP_ROUNDS


def _coerce_answer(key: str, value: object) -> object:
    if value in ("", None):
        return None
    if key == "childAgeMonths":
        return int(float(str(value)))
    if key == "temperatureC":
        return float(str(value).replace(",", "."))
    if key == "seizure":
        if isinstance(value, bool):
            return value
        text = _answer_token(value)
        if text in {"co", "yes", "true", "1"}:
            return True
        if text in {"khong", "no", "false", "0"}:
            return False
        return None
    if key == "dehydrationSigns":
        if isinstance(value, list):
            return [str(item) for item in value if _answer_token(item) != "khong"]
        return [] if _answer_token(value) == "khong" else [str(value)]
    return str(value)


def _empty(value: str | None) -> bool:
    return value is None or not value.strip()


def _answer_token(value: object) -> str:
    normalized = unicodedata.normalize("NFD", str(value).strip().lower())
    return "".join(char for char in normalized if unicodedata.category(char) != "Mn").replace("đ", "d")


def _context_text(intake: ChildTriageRequest) -> str:
    return " ".join(intake.symptomList + [intake.parentFreeText or ""]).lower()


def _has_fever_context(intake: ChildTriageRequest) -> bool:
    return "sot" in _context_text(intake) or "fever" in _context_text(intake) or intake.temperatureC is not None


def _has_diarrhea_context(intake: ChildTriageRequest) -> bool:
    text = _context_text(intake)
    return "tieu chay" in text or "diarrhea" in text or not _empty(intake.diarrhea)


def _has_dehydration_or_diarrhea_context(intake: ChildTriageRequest) -> bool:
    text = _context_text(intake)
    return _has_diarrhea_context(intake) or "mat nuoc" in text or bool(intake.dehydrationSigns)


def _has_vomiting_context(intake: ChildTriageRequest) -> bool:
    text = _context_text(intake)
    return "non" in text or "vomit" in text or not _empty(intake.vomiting)


def _has_rash_context(intake: ChildTriageRequest) -> bool:
    text = _context_text(intake)
    return "phat ban" in text or "rash" in text or not _empty(intake.rash)
