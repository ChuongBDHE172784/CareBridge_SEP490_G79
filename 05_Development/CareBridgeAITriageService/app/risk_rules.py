import re

from app.schemas import ChildTriageRequest
from app.symptom_normalizer import strip_accents


MATERNAL_STAGES = frozenset({"PRECONCEPTION", "PREGNANCY", "POSTPARTUM"})

_MATERNAL_BREATHING_DISTRESS_PHRASES = (
    "kho tho",
    "khong tho duoc",
    "thieu hoi",
    "nghet tho",
    "difficulty breathing",
    "breathing difficulty",
    "shortness of breath",
    "cannot breathe",
    "unable to breathe",
)
_MATERNAL_CYANOSIS_PHRASES = (
    "tim tai",
    "tim moi",
    "moi tim",
    "blue lips",
    "cyanosis",
    "cyanotic",
)
_MATERNAL_SEIZURE_PHRASES = ("co giat", "seizure", "convulsion")
_MATERNAL_ALTERED_CONSCIOUSNESS_PHRASES = (
    "lo mo",
    "li bi",
    "kho danh thuc",
    "kho giu tinh tao",
    "bat tinh",
    "ngat",
    "ngat xiu",
    "faint",
    "fainted",
    "fainting",
    "unconscious",
    "altered consciousness",
    "difficult to wake",
    "loss of consciousness",
)
_MATERNAL_HEAVY_BLEEDING_PHRASES = (
    "bang huyet",
    "chay mau nhieu",
    "ra mau nhieu",
    "mat mau nhieu",
    "tham uot bang",
    "heavy bleeding",
    "bleeding heavily",
    "hemorrhage",
    "haemorrhage",
    "soaking pad",
)
_MATERNAL_SELF_HARM_PHRASES = (
    "tu lam hai",
    "lam hai ban than",
    "tu hai",
    "tu sat",
    "tu tu",
    "muon chet",
    "self harm",
    "self-harm",
    "kill myself",
    "suicide",
    "suicidal",
    "want to die",
)


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
    if {"mild_dehydration", "severe_dehydration"} & symptoms and ("diarrhea" in symptoms or intake.diarrhea):
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


def _apply_maternal_universal_red_flag_rules(
    intake: ChildTriageRequest,
    normalized_symptoms: list[str],
) -> tuple[list[str], list[str]]:
    breathing = _normalized_text(intake.breathingStatus)
    consciousness = _normalized_text(intake.consciousnessStatus)
    canonical_signs = set(normalized_symptoms)
    reported_signs = _normalized_text(" ".join([
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

    if _canonical_sign_is_unnegated(
        canonical_signs,
        "difficulty_breathing",
        reported_signs,
        *_MATERNAL_BREATHING_DISTRESS_PHRASES,
    ) or _text_contains_any(
        breathing, *_MATERNAL_BREATHING_DISTRESS_PHRASES
    ) or _text_contains_any(
        reported_signs, *_MATERNAL_BREATHING_DISTRESS_PHRASES
    ):
        add(f"{rule_prefix}_BREATHING_DISTRESS", "Khó thở")
    if _canonical_sign_is_unnegated(
        canonical_signs,
        "cyanosis",
        reported_signs,
        *_MATERNAL_CYANOSIS_PHRASES,
    ) or _text_contains_any(
        breathing, *_MATERNAL_CYANOSIS_PHRASES
    ) or _text_contains_any(
        reported_signs, *_MATERNAL_CYANOSIS_PHRASES
    ):
        add(f"{rule_prefix}_CYANOSIS", "Tím tái")
    if (
        intake.seizure is True
        or _canonical_sign_is_unnegated(
            canonical_signs,
            "seizure",
            reported_signs,
            *_MATERNAL_SEIZURE_PHRASES,
        )
        or _text_contains_any(reported_signs, *_MATERNAL_SEIZURE_PHRASES)
    ):
        add(f"{rule_prefix}_SEIZURE", "Co giật")
    if any(
        _canonical_sign_is_unnegated(
            canonical_signs,
            sign,
            reported_signs,
            *_MATERNAL_ALTERED_CONSCIOUSNESS_PHRASES,
        )
        for sign in ("lethargy", "difficult_to_wake")
    ) or _text_contains_any(
        consciousness, *_MATERNAL_ALTERED_CONSCIOUSNESS_PHRASES
    ) or _text_contains_any(
        reported_signs, *_MATERNAL_ALTERED_CONSCIOUSNESS_PHRASES
    ):
        add(f"{rule_prefix}_ALTERED_CONSCIOUSNESS", "Thay đổi ý thức")
    if _text_contains_any(
        reported_signs, *_MATERNAL_HEAVY_BLEEDING_PHRASES
    ):
        add(f"{rule_prefix}_HEAVY_BLEEDING", "Chảy máu nhiều")
    if _text_contains_any(
        reported_signs, *_MATERNAL_SELF_HARM_PHRASES
    ):
        add(f"{rule_prefix}_SELF_HARM", "Có ý nghĩ tự làm hại bản thân")
    if not red_flags:
        matched_rules.append(_maternal_review_rule(intake.stage))
    return red_flags, matched_rules


def _maternal_review_rule(stage: str) -> str:
    if stage == "POSTPARTUM":
        return "POSTPARTUM_RULES_REQUIRE_CLINICAL_REVIEW"
    return f"{stage}_RULES_NEED_CLINICAL_REVIEW"


def _normalized_text(value: str | None) -> str:
    normalized = strip_accents(value or "").replace("đ", "d")
    return _expand_english_negative_contractions(normalized)


def _expand_english_negative_contractions(value: str) -> str:
    normalized = value.replace("’", "'").replace("`", "'")
    special_cases = {
        r"\bcan't\b": "cannot",
        r"\bwon't\b": "will not",
        r"\bshan't\b": "shall not",
        r"\bain't\b": "is not",
    }
    for pattern, replacement in special_cases.items():
        normalized = re.sub(pattern, replacement, normalized)
    normalized = re.sub(r"\b([a-z]+)n't\b", r"\1 not", normalized)
    return re.sub(r"\s+", " ", normalized).strip()


def _canonical_sign_is_unnegated(
    canonical_signs: set[str],
    sign: str,
    reported_signs: str,
    *phrases: str,
) -> bool:
    if sign not in canonical_signs:
        return False
    mentioned_phrases = [
        phrase
        for phrase in phrases
        if re.search(rf"(?<!\w){re.escape(phrase)}(?!\w)", reported_signs)
    ]
    return not mentioned_phrases or any(
        _contains_unnegated_phrase(reported_signs, phrase)
        for phrase in mentioned_phrases
    )


def _text_contains_any(value: str, *tokens: str) -> bool:
    return any(_contains_unnegated_phrase(value, token) for token in tokens)


def _contains_unnegated_phrase(value: str, phrase: str) -> bool:
    pattern = re.compile(rf"(?<!\w){re.escape(phrase)}(?!\w)")
    for match in pattern.finditer(value):
        clause_prefix = re.split(
            r"[,.!?;:]|\b(?:nhung|ma|song|tuy nhien|however|but|va|and)\b",
            value[:match.start()],
        )[-1]
        preceding_tokens = re.findall(r"[a-z]+", clause_prefix)[-6:]
        if not {
            "khong",
            "chua",
            "chang",
            "cha",
            "not",
            "no",
            "never",
            "without",
            "denies",
            "denied",
        } & set(preceding_tokens):
            return True
    return False
