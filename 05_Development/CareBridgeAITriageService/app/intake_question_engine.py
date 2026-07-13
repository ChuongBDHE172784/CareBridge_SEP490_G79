from __future__ import annotations

from uuid import uuid4

from app.risk_rules import apply_red_flag_rules
from app.schemas import ChildTriageRequest, IntakeQuestion
from app.symptom_normalizer import normalize_symptoms

MAX_QUESTIONS_PER_ROUND = 3
MAX_FOLLOWUP_ROUNDS = 3


QUESTION_BANK: dict[str, IntakeQuestion] = {
    "parentFreeText": IntakeQuestion(
        questionKey="parentFreeText",
        text="Be dang co trieu chung gi? Hay mo ta ngan gon dau hieu ban thay.",
        answerType="TEXT",
    ),
    "childAgeMonths": IntakeQuestion(
        questionKey="childAgeMonths",
        text="Be hien bao nhieu thang tuoi?",
        answerType="NUMBER",
    ),
    "breathingStatus": IntakeQuestion(
        questionKey="breathingStatus",
        text="Be co kho tho, tho rut lom nguc hoac tim tai khong?",
        answerType="SINGLE_CHOICE",
        options=["Khong", "Kho tho", "Tho rut lom nguc", "Tim tai", "Khong chac"],
    ),
    "consciousnessStatus": IntakeQuestion(
        questionKey="consciousnessStatus",
        text="Be co tinh tao khong, hay lo mo/li bi/kho danh thuc?",
        answerType="SINGLE_CHOICE",
        options=["Tinh tao", "Lo mo", "Li bi", "Kho danh thuc", "Khong chac"],
    ),
    "seizure": IntakeQuestion(
        questionKey="seizure",
        text="Be co co giat khong?",
        answerType="BOOLEAN",
        options=["Khong", "Co", "Khong chac"],
    ),
    "feedingStatus": IntakeQuestion(
        questionKey="feedingStatus",
        text="Be co bu/uong duoc khong?",
        answerType="SINGLE_CHOICE",
        options=["Bu/uong tot", "Bu/uong kem", "Bo bu", "Khong uong duoc", "Khong chac"],
    ),
    "temperatureC": IntakeQuestion(
        questionKey="temperatureC",
        text="Nhiet do cao nhat do duoc la bao nhieu do C?",
        answerType="NUMBER",
    ),
    "dehydrationSigns": IntakeQuestion(
        questionKey="dehydrationSigns",
        text="Be co dau hieu mat nuoc nao khong?",
        answerType="MULTI_CHOICE",
        options=["Khong", "Moi kho", "Tieu it", "Mat trung", "Khoc khong co nuoc mat", "Khong chac"],
    ),
    "vomiting": IntakeQuestion(
        questionKey="vomiting",
        text="Be co non khong? Neu co, non it hay non lien tuc?",
        answerType="SINGLE_CHOICE",
        options=["Khong", "Non it", "Non lien tuc", "Khong chac"],
    ),
    "diarrhea": IntakeQuestion(
        questionKey="diarrhea",
        text="Be co tieu chay khong?",
        answerType="SINGLE_CHOICE",
        options=["Khong", "Nhe", "Nhieu lan", "Kem mat nuoc", "Khong chac"],
    ),
    "duration": IntakeQuestion(
        questionKey="duration",
        text="Trieu chung xuat hien bao lau?",
        answerType="SINGLE_CHOICE",
        options=["Duoi 1 ngay", "1-3 ngay", "3-7 ngay", "Hon 1 tuan", "Khong chac"],
    ),
    "rash": IntakeQuestion(
        questionKey="rash",
        text="Be co phat ban khong?",
        answerType="SINGLE_CHOICE",
        options=["Khong", "Phat ban nhe", "Phat ban kem sot cao", "Xau nhanh", "Khong chac"],
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
        text = str(value).strip().lower()
        if text in {"co", "yes", "true", "1"}:
            return True
        if text in {"khong", "no", "false", "0"}:
            return False
        return None
    if key == "dehydrationSigns":
        if isinstance(value, list):
            return [str(item) for item in value if str(item).lower() != "khong"]
        return [] if str(value).lower() == "khong" else [str(value)]
    return str(value)


def _empty(value: str | None) -> bool:
    return value is None or not value.strip()


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
