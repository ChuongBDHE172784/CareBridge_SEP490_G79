from app.schemas import ChildTriageRequest
from app.symptom_normalizer import strip_accents


def missing_info_questions(intake: ChildTriageRequest) -> list[str]:
    questions: list[str] = []
    no_symptom_context = not intake.symptomList and not (intake.parentFreeText or "").strip()
    if intake.childAgeMonths is None:
        questions.append("Trẻ hiện bao nhiêu tháng tuổi?")
    if no_symptom_context:
        questions.append("Trẻ đang có triệu chứng gì?")
    return questions[:3]


def apply_red_flag_rules(
    intake: ChildTriageRequest,
    normalized_symptoms: list[str],
) -> tuple[list[str], list[str]]:
    symptoms = set(normalized_symptoms)
    red_flags: list[str] = []
    matched_rules: list[str] = []

    def add(rule: str, flag: str) -> None:
        matched_rules.append(rule)
        red_flags.append(flag)

    if {"difficulty_breathing", "chest_indrawing", "cyanosis"} & symptoms:
        add("RED_BREATHING_DISTRESS", "Khó thở, thở rút lõm hoặc tím tái")
    if "seizure" in symptoms:
        add("RED_SEIZURE", "Co giật")
    if {"lethargy", "difficult_to_wake"} & symptoms:
        add("RED_LETHARGY", "Lơ mơ, li bì hoặc khó đánh thức")
    if {"unable_to_drink", "poor_feeding"} & symptoms:
        add("RED_POOR_FEEDING", "Bỏ bú hoặc không uống được")
    if {"mild_dehydration", "severe_dehydration"} & symptoms and ("diarrhea" in symptoms or intake.diarrhea):
        add("RED_DIARRHEA_DEHYDRATION", "Tiêu chảy kèm dấu hiệu mất nước")
    elif "severe_dehydration" in symptoms:
        add("RED_SEVERE_DEHYDRATION", "Dấu hiệu mất nước nặng")
    if "persistent_vomiting" in symptoms:
        add("RED_PERSISTENT_VOMITING", "Nôn liên tục")
    if intake.childAgeMonths is not None and intake.childAgeMonths < 3 and _has_fever(intake):
        add("RED_INFANT_FEVER_UNDER_3_MONTHS", "Trẻ dưới 3 tháng có sốt")
    elif "high_fever" in symptoms or _has_high_fever(intake):
        add("RED_HIGH_FEVER", "Sốt cao")

    return red_flags, matched_rules


def score_risk(
    intake: ChildTriageRequest,
    normalized_symptoms: list[str],
    red_flags: list[str],
    matched_rules: list[str],
) -> tuple[str, list[str]]:
    if red_flags:
        return "RED", matched_rules

    symptoms = set(normalized_symptoms)
    if "fever" in symptoms and _has_fever(intake):
        matched_rules.append("YELLOW_FEVER_MONITOR")
    if "difficulty_breathing" not in symptoms and "cough" in symptoms:
        matched_rules.append("YELLOW_RESPIRATORY_NO_DISTRESS")
    if "diarrhea" in symptoms:
        matched_rules.append("YELLOW_MILD_MODERATE_DIARRHEA")
    if "vomiting" in symptoms and "persistent_vomiting" not in symptoms:
        matched_rules.append("YELLOW_LIMITED_VOMITING")
    if "rash" in symptoms:
        matched_rules.append("YELLOW_MILD_RASH")

    if matched_rules:
        return "YELLOW", matched_rules

    matched_rules.append("GREEN_MILD_NO_RED_FLAGS")
    return "GREEN", matched_rules


def _has_fever(intake: ChildTriageRequest) -> bool:
    return intake.temperatureC is not None and intake.temperatureC >= 38.0


def _has_high_fever(intake: ChildTriageRequest) -> bool:
    return intake.temperatureC is not None and intake.temperatureC >= 39.0


def _contains_any(value: str | None, keywords: list[str]) -> bool:
    if not value:
        return False
    text = strip_accents(value)
    return any(keyword in text for keyword in keywords)
