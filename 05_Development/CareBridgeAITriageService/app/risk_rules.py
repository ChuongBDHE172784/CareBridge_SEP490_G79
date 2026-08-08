import re

from app.schemas import ChildTriageRequest
from app.danger_phrases import (
    MATERNAL_ALTERED_CONSCIOUSNESS_PHRASES,
    MATERNAL_BREATHING_DISTRESS_PHRASES,
    MATERNAL_CYANOSIS_PHRASES,
    MATERNAL_HEAVY_BLEEDING_PHRASES,
    MATERNAL_SEIZURE_PHRASES,
    MATERNAL_SELF_HARM_PHRASES,
    canonical_sign_is_unnegated,
    normalized_text,
    text_contains_any,
)


MATERNAL_STAGES = frozenset({"PRECONCEPTION", "PREGNANCY", "POSTPARTUM"})

#: Fields whose answers feed the complaint-independent RED rules (RED_LETHARGY,
#: RED_BREATHING_DISTRESS, RED_SEIZURE). Defined here rather than in the question engine so
#: both the planner that asks them and the scorer that requires them share one list, and so
#: risk_rules stays free of an import cycle back into the engine.
DANGER_SIGN_KEYS = ("consciousnessStatus", "breathingStatus", "seizure")


def missing_info_questions(intake: ChildTriageRequest) -> list[str]:
    if intake.stage == "POSTPARTUM":
        questions: list[str] = []
        if not (intake.parentFreeText or "").strip() and not intake.symptomList:
            questions.append("Bạn đang gặp dấu hiệu nào trong quá trình hồi phục sau sinh?")
        if not intake.duration:
            questions.append("Dấu hiệu đã xuất hiện bao lâu và có tăng nhanh không?")
        if not intake.breathingStatus:
            questions.append("Bạn có khó thở hoặc tím tái không?")
        if not intake.consciousnessStatus:
            questions.append("Bạn có lơ mơ, ngất hoặc khó giữ tỉnh táo không?")
        if intake.seizure is None:
            questions.append("Bạn có bị co giật không?")
        return questions[:3]
    if intake.stage in {"PRECONCEPTION", "PREGNANCY"}:
        questions: list[str] = []
        if not (intake.parentFreeText or "").strip() and not intake.symptomList:
            questions.append("Bạn đang gặp triệu chứng hoặc muốn được hỗ trợ nội dung nào?")
        if not intake.duration:
            questions.append("Triệu chứng hoặc vấn đề này đã xuất hiện bao lâu?")
        if intake.stage == "PREGNANCY":
            questions.append("Bạn có ra máu âm đạo nhiều, đau bụng dữ dội, đau đầu dữ dội/hoa mắt hoặc giảm cử động thai không?")
        return questions[:3]
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
    if intake.stage in MATERNAL_STAGES:
        return _apply_maternal_universal_red_flag_rules(intake, normalized_symptoms)
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
    # NOTE: intake.diarrhea is a free-text field (str | None); any non-empty answer (including
    # a negative one like "Không") is truthy, so only the normalized "diarrhea" code is used here.
    if {"mild_dehydration", "severe_dehydration"} & symptoms and "diarrhea" in symptoms:
        add("RED_DIARRHEA_DEHYDRATION", "Tiêu chảy kèm dấu hiệu mất nước")
    elif "severe_dehydration" in symptoms:
        add("RED_SEVERE_DEHYDRATION", "Dấu hiệu mất nước nặng")
    if "persistent_vomiting" in symptoms:
        add("RED_PERSISTENT_VOMITING", "Nôn liên tục")
    if "rash" in symptoms and _has_high_fever(intake):
        add("RED_RASH_HIGH_FEVER", "Phát ban kèm sốt cao")
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
    if intake.stage in MATERNAL_STAGES:
        review_rule = _maternal_review_rule(intake.stage)
        if review_rule not in matched_rules:
            matched_rules.append(review_rule)
        return "NEED_MORE_INFO", matched_rules
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

    # GREEN is the only reassuring outcome this engine can produce, so it may not be reached
    # on an unscreened dataset. "No rule matched" is not "nothing is wrong" when the rules
    # that carry the danger signs were never given an answer to read — the same reason V2
    # gates GREEN behind its own eligibility dataset instead of treating silence as reassurance.
    if not _danger_screen_answered(intake):
        matched_rules.append("YELLOW_UNSCREENED_DANGER_SIGNS")
        return "YELLOW", matched_rules

    matched_rules.append("GREEN_MILD_NO_RED_FLAGS")
    return "GREEN", matched_rules


def _danger_screen_answered(intake: ChildTriageRequest) -> bool:
    """True only when every complaint-independent danger sign has a real answer."""

    for key in DANGER_SIGN_KEYS:
        value = getattr(intake, key, None)
        if value is None or (isinstance(value, str) and not value.strip()):
            return False
    return True


def _has_fever(intake: ChildTriageRequest) -> bool:
    return intake.temperatureC is not None and intake.temperatureC >= 38.0


def _has_high_fever(intake: ChildTriageRequest) -> bool:
    return intake.temperatureC is not None and intake.temperatureC >= 39.0


def _apply_maternal_universal_red_flag_rules(
    intake: ChildTriageRequest,
    normalized_symptoms: list[str],
) -> tuple[list[str], list[str]]:
    breathing = normalized_text(intake.breathingStatus)
    consciousness = normalized_text(intake.consciousnessStatus)
    canonical_signs = set(normalized_symptoms)
    reported_signs = normalized_text(" ".join([
        intake.parentFreeText or "",
        " ".join(intake.symptomList),
        " ".join(normalized_symptoms),
    ]))
    red_flags: list[str] = []
    matched_rules: list[str] = []
    rule_prefix = f"RED_{intake.stage}"

    def add(rule: str, flag: str) -> None:
        if rule not in matched_rules:
            matched_rules.append(rule)
        if flag not in red_flags:
            red_flags.append(flag)

    if canonical_sign_is_unnegated(
        canonical_signs,
        "difficulty_breathing",
        reported_signs,
        *MATERNAL_BREATHING_DISTRESS_PHRASES,
    ) or text_contains_any(
        breathing, *MATERNAL_BREATHING_DISTRESS_PHRASES
    ) or text_contains_any(
        reported_signs, *MATERNAL_BREATHING_DISTRESS_PHRASES
    ):
        add(f"{rule_prefix}_BREATHING_DISTRESS", "Khó thở")
    if canonical_sign_is_unnegated(
        canonical_signs,
        "cyanosis",
        reported_signs,
        *MATERNAL_CYANOSIS_PHRASES,
    ) or text_contains_any(
        breathing, *MATERNAL_CYANOSIS_PHRASES
    ) or text_contains_any(
        reported_signs, *MATERNAL_CYANOSIS_PHRASES
    ):
        add(f"{rule_prefix}_CYANOSIS", "Tím tái")
    if (
        intake.seizure is True
        or canonical_sign_is_unnegated(
            canonical_signs,
            "seizure",
            reported_signs,
            *MATERNAL_SEIZURE_PHRASES,
        )
        or text_contains_any(reported_signs, *MATERNAL_SEIZURE_PHRASES)
    ):
        add(f"{rule_prefix}_SEIZURE", "Co giật")
    if any(
        canonical_sign_is_unnegated(
            canonical_signs,
            sign,
            reported_signs,
            *MATERNAL_ALTERED_CONSCIOUSNESS_PHRASES,
        )
        for sign in ("lethargy", "difficult_to_wake")
    ) or text_contains_any(
        consciousness, *MATERNAL_ALTERED_CONSCIOUSNESS_PHRASES
    ) or text_contains_any(
        reported_signs, *MATERNAL_ALTERED_CONSCIOUSNESS_PHRASES
    ):
        add(f"{rule_prefix}_ALTERED_CONSCIOUSNESS", "Thay đổi ý thức")
    if text_contains_any(
        reported_signs, *MATERNAL_HEAVY_BLEEDING_PHRASES
    ):
        add(f"{rule_prefix}_HEAVY_BLEEDING", "Chảy máu nhiều")
    if text_contains_any(
        reported_signs, *MATERNAL_SELF_HARM_PHRASES
    ):
        add(f"{rule_prefix}_SELF_HARM", "Có ý nghĩ tự làm hại bản thân")
    if not red_flags:
        matched_rules.append(_maternal_review_rule(intake.stage))
    return red_flags, matched_rules


def _maternal_review_rule(stage: str) -> str:
    if stage == "POSTPARTUM":
        return "POSTPARTUM_RULES_REQUIRE_CLINICAL_REVIEW"
    return f"{stage}_RULES_NEED_CLINICAL_REVIEW"

