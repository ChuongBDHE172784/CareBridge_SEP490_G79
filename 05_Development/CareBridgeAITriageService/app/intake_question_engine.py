from __future__ import annotations

import unicodedata
from uuid import uuid4

from app.gemini_client import GeminiClient
from app.risk_rules import DANGER_SIGN_KEYS, apply_red_flag_rules
from app.schemas import ChildTriageRequest, IntakeQuestion, TriageIntent
from app.symptom_normalizer import normalize_symptoms, remove_instruction_like_text, strip_accents


MAX_QUESTIONS_PER_ROUND = 3
MAX_FOLLOWUP_ROUNDS = 3
#: `DANGER_SIGN_KEYS` (from risk_rules) is screened for whatever the user reported, because the
#: rules that read it apply to every complaint — a febrile infant who is also lethargic was
#: previously classified YELLOW unless the parent happened to type "li bì" unprompted, since no
#: fever pathway ever asked. This is the V1 counterpart of V2's stage-independent safety gate,
#: and `score_risk` now refuses GREEN until the same fields carry an answer.
#:
#: They are folded in after the first round of complaint-relevant questions, never before it:
#: leading with seizures instead of the problem the user actually reported is the same failure
#: `test_infant_diarrhea_asks_diarrhea_facts_before_unrelated_screening` pins against. Index
#: MAX_QUESTIONS_PER_ROUND puts them in round two, which is the last round the budget reaches
#: (main.py stops asking once round_number >= MAX_FOLLOWUP_ROUNDS).
MATERNAL_STAGES = frozenset({"PRECONCEPTION", "PREGNANCY", "POSTPARTUM"})
PEDIATRIC_KEYS = frozenset({
    "childAgeMonths", "feedingStatus", "vomiting", "diarrhea", "rash", "dehydrationSigns",
    "painSeverity", "urinarySymptoms",
})

def _question(key: str, text: str, answer_type: str, options: list[str] | None = None) -> IntakeQuestion:
    return IntakeQuestion(questionKey=key, text=text, answerType=answer_type, options=options or [])


# These questions collect facts only. They never alter deterministic risk rules or thresholds.
QUESTION_BANK: dict[str, IntakeQuestion] = {
    "parentFreeText": _question("parentFreeText", "Bạn đang gặp dấu hiệu gì? Hãy mô tả ngắn gọn điều bạn quan sát được.", "TEXT"),
    "childAgeMonths": _question("childAgeMonths", "Bé hiện bao nhiêu tháng tuổi?", "NUMBER"),
    "breathingStatus": _question("breathingStatus", "Bé có khó thở, thở rút lõm ngực hoặc tím tái không?", "SINGLE_CHOICE", ["Không", "Khó thở", "Thở rút lõm ngực", "Tím tái", "Không chắc"]),
    "consciousnessStatus": _question("consciousnessStatus", "Bé có tỉnh táo không, hay lơ mơ/li bì/khó đánh thức?", "SINGLE_CHOICE", ["Tỉnh táo", "Lơ mơ", "Li bì", "Khó đánh thức", "Không chắc"]),
    "seizure": _question("seizure", "Bé có co giật không?", "BOOLEAN", ["Không", "Có", "Không chắc"]),
    "feedingStatus": _question("feedingStatus", "Bé có bú/uống được không?", "SINGLE_CHOICE", ["Bú/uống tốt", "Bú/uống kém", "Bỏ bú", "Không uống được", "Không chắc"]),
    "temperatureC": _question("temperatureC", "Nhiệt độ cao nhất đo được là bao nhiêu độ C?", "NUMBER"),
    "dehydrationSigns": _question("dehydrationSigns", "Bé có dấu hiệu mất nước nào không?", "MULTI_CHOICE", ["Không", "Môi khô", "Tiểu ít", "Mắt trũng", "Khóc không có nước mắt", "Không chắc"]),
    "vomiting": _question("vomiting", "Bé có nôn không? Nếu có, nôn ít hay nôn liên tục?", "SINGLE_CHOICE", ["Không", "Nôn ít", "Nôn liên tục", "Không chắc"]),
    "duration": _question("duration", "Triệu chứng xuất hiện bao lâu rồi?", "SINGLE_CHOICE", ["Dưới 1 ngày", "1-3 ngày", "3-7 ngày", "Hơn 1 tuần", "Không chắc"]),
    "rash": _question("rash", "Bé có phát ban không?", "SINGLE_CHOICE", ["Không", "Phát ban nhẹ", "Phát ban kèm sốt cao", "Xấu nhanh", "Không chắc"]),
    "painSeverity": _question("painSeverity", "Mức độ đau của bé hiện thế nào?", "SINGLE_CHOICE", ["Nhẹ", "Vừa", "Nhiều", "Không chắc"]),
    "urinarySymptoms": _question("urinarySymptoms", "Bé có tiểu buốt, tiểu rắt hoặc thay đổi nước tiểu không?", "SINGLE_CHOICE", ["Không", "Có", "Không chắc"]),
}

MATERNAL_QUESTION_BANK: dict[str, IntakeQuestion] = {
    "parentFreeText": _question("parentFreeText", "Bạn đang gặp triệu chứng hoặc cần hỗ trợ nội dung gì?", "TEXT"),
    "duration": _question("duration", "Triệu chứng hoặc vấn đề này xuất hiện bao lâu rồi?", "SINGLE_CHOICE", ["Dưới 1 ngày", "1-3 ngày", "3-7 ngày", "Hơn 1 tuần", "Không chắc"]),
    "hydrationStatus": _question("hydrationStatus", "Bạn có khát nhiều, khô miệng, tiểu ít hoặc không giữ được nước không?", "SINGLE_CHOICE", ["Không", "Có một dấu hiệu", "Có nhiều dấu hiệu", "Không chắc"]),
    "vomiting": _question("vomiting", "Bạn có buồn nôn hoặc nôn không?", "SINGLE_CHOICE", ["Không", "Buồn nôn", "Nôn ít", "Nôn liên tục", "Không chắc"]),
    "temperatureC": _question("temperatureC", "Nhiệt độ cao nhất bạn đo được là bao nhiêu độ C?", "NUMBER"),
    "breathingStatus": _question("breathingStatus", "Bạn có khó thở hoặc tím tái không?", "SINGLE_CHOICE", ["Không", "Khó thở", "Tím tái", "Không chắc"]),
    "consciousnessStatus": _question("consciousnessStatus", "Bạn có lơ mơ, ngất hoặc khó giữ tỉnh táo không?", "SINGLE_CHOICE", ["Tỉnh táo", "Lơ mơ", "Ngất", "Khó giữ tỉnh táo", "Không chắc"]),
    "seizure": _question("seizure", "Bạn có bị co giật không?", "BOOLEAN", ["Không", "Có", "Không chắc"]),
    "painSeverity": _question("painSeverity", "Mức độ đau hiện thế nào?", "SINGLE_CHOICE", ["Nhẹ", "Vừa", "Nhiều", "Không chắc"]),
    "urinarySymptoms": _question("urinarySymptoms", "Bạn có tiểu buốt, tiểu rắt hoặc thay đổi nước tiểu không?", "SINGLE_CHOICE", ["Không", "Có", "Không chắc"]),
    # Descriptive-only (CB-TRIAGE-MATQ-IMP-001): pattern of pain, never a risk-rule input.
    # Only offered for PREGNANCY (see _relevant_missing_keys) — routine vs. labor-pattern
    # abdominal pain is not a meaningful distinction outside pregnancy.
    "abdominalPainPattern": _question("abdominalPainPattern", "Cơn đau bụng của bạn diễn ra liên tục hay từng cơn đều đặn (co thắt)?", "SINGLE_CHOICE", ["Liên tục", "Từng cơn đều đặn", "Từng cơn không đều", "Không chắc"]),
}


def new_session_id() -> str:
    return str(uuid4())


def extract_triage_intent(intake: ChildTriageRequest) -> TriageIntent:
    """Return bounded, non-authoritative intent from deterministic extraction only."""
    symptoms = normalize_symptoms(intake)
    families: list[str] = []
    mapping = {
        "diarrhea": "DIARRHEA", "mild_dehydration": "DIARRHEA", "severe_dehydration": "DIARRHEA",
        "vomiting": "VOMITING", "persistent_vomiting": "VOMITING",
        "fever": "FEVER", "high_fever": "FEVER", "rash": "RASH", "itching": "RASH",
        "cough": "RESPIRATORY", "runny_nose": "RESPIRATORY", "cold_symptoms": "RESPIRATORY",
        "difficulty_breathing": "RESPIRATORY",
        "chest_indrawing": "RESPIRATORY", "cyanosis": "RESPIRATORY",
        "abdominal_pain": "ABDOMINAL", "nausea": "ABDOMINAL", "constipation": "ABDOMINAL",
        "urinary_discomfort": "URINARY", "ear_pain": "EAR", "headache": "HEADACHE",
    }
    for symptom in symptoms:
        family = mapping.get(symptom)
        if family and family not in families:
            families.append(family)
    raw = remove_instruction_like_text(" ".join(intake.symptomList + [intake.parentFreeText or ""]))
    if not families and raw:
        # The text is intentionally not retained: generic intent merely requests clarification.
        care_goal = "CLARIFY_SYMPTOM"
    else:
        care_goal = "ASSESS_REPORTED_SYMPTOM" if families else "CLARIFY_SYMPTOM"
    known = [key for key, value in intake.model_dump().items() if _has_value(value)]
    subject = "MOTHER" if intake.stage in MATERNAL_STAGES else "CHILD"
    return TriageIntent(
        subject=subject,
        careGoal=care_goal,
        symptomFamilies=families,
        knownFacts=known,
        confidence=0.94 if families else 0.4,
    )


def merge_answers(intake: ChildTriageRequest, new_answers: dict[str, object]) -> ChildTriageRequest:
    data = intake.model_dump()
    allowed = set(MATERNAL_QUESTION_BANK if intake.stage in MATERNAL_STAGES else QUESTION_BANK)
    for key, value in new_answers.items():
        if key in allowed and key in data:
            data[key] = _coerce_answer(key, value)
    return ChildTriageRequest(**data)


def determine_missing_information(
    intake: ChildTriageRequest, *, asked_keys: set[str] | frozenset[str] = frozenset()
) -> list[str]:
    intent = extract_triage_intent(intake)
    bank = MATERNAL_QUESTION_BANK if intake.stage in MATERNAL_STAGES else QUESTION_BANK
    candidates = _relevant_missing_keys(intake, intent)
    return [
        key for key in candidates
        if key in bank
        and (
            _is_missing(intake, key)
            or (intent.careGoal == "CLARIFY_SYMPTOM" and key == "parentFreeText" and key not in asked_keys)
        )
    ][:MAX_QUESTIONS_PER_ROUND]


def ask_followup_questions(
    intake: ChildTriageRequest, *, asked_keys: set[str] | frozenset[str] = frozenset()
) -> list[IntakeQuestion]:
    bank = MATERNAL_QUESTION_BANK if intake.stage in MATERNAL_STAGES else QUESTION_BANK
    return [bank[key] for key in determine_missing_information(intake, asked_keys=asked_keys)]


def _with_danger_sign_screen(keys: list[str]) -> list[str]:
    """Fold the universal danger-sign screen in behind the first round of relevant questions."""

    return list(dict.fromkeys([
        *keys[:MAX_QUESTIONS_PER_ROUND],
        *DANGER_SIGN_KEYS,
        *keys[MAX_QUESTIONS_PER_ROUND:],
    ]))


def _relevant_missing_keys(intake: ChildTriageRequest, intent: TriageIntent) -> list[str]:
    if intent.careGoal == "CLARIFY_SYMPTOM":
        # An unrecognised description needs a clarification, not a guessed
        # symptom pathway.  Postpartum is the narrow exception: screen for
        # altered consciousness before routine clarification.
        #
        # No danger-sign screen here on purpose: there is no complaint to screen around yet,
        # and one clarification is the most that can safely be asked for unknown text.
        return (
            ["consciousnessStatus", "parentFreeText"]
            if intake.stage == "POSTPARTUM"
            else ["parentFreeText"]
        )
    return _with_danger_sign_screen(_family_missing_keys(intake, intent))


def _family_missing_keys(intake: ChildTriageRequest, intent: TriageIntent) -> list[str]:
    """Facts the reported complaint itself calls for, ahead of any universal screen."""

    families = set(intent.symptomFamilies)
    if len(families) > 1:
        policies = (
            {
                "ABDOMINAL": ["painSeverity", "duration", "vomiting", "hydrationStatus", "temperatureC"],
                "DIARRHEA": ["duration", "hydrationStatus", "temperatureC"],
                "VOMITING": ["vomiting", "hydrationStatus", "duration"],
                "URINARY": ["urinarySymptoms", "duration", "temperatureC"],
                "EAR": ["painSeverity", "duration", "temperatureC"],
                "HEADACHE": ["painSeverity", "duration", "temperatureC"],
                "FEVER": ["duration", "temperatureC"],
                "RESPIRATORY": ["breathingStatus", "consciousnessStatus", "duration"],
                "RASH": ["rash", "temperatureC", "duration"],
            }
            if intake.stage in MATERNAL_STAGES else {
                "ABDOMINAL": ["painSeverity", "duration", "vomiting", "feedingStatus", "childAgeMonths"],
                "DIARRHEA": ["duration", "dehydrationSigns", "vomiting", "feedingStatus", "childAgeMonths"],
                "VOMITING": ["vomiting", "dehydrationSigns", "feedingStatus", "childAgeMonths"],
                "URINARY": ["urinarySymptoms", "duration", "temperatureC", "childAgeMonths"],
                "EAR": ["painSeverity", "duration", "temperatureC", "childAgeMonths"],
                "HEADACHE": ["painSeverity", "duration", "temperatureC", "childAgeMonths"],
                "FEVER": ["duration", "temperatureC", "childAgeMonths", "feedingStatus"],
                "RESPIRATORY": ["childAgeMonths", "duration", "breathingStatus", "consciousnessStatus"],
                "RASH": ["duration", "rash", "temperatureC", "childAgeMonths"],
            }
        )
        # Note: abdominalPainPattern is intentionally NOT added here (multi-family case). With
        # >1 family in play, prioritising it would consume the whole per-conversation question
        # budget and crowd out the other family's own follow-up entirely (MAX_QUESTIONS_PER_ROUND
        # x MAX_FOLLOWUP_ROUNDS is tight). It is only prioritised below when ABDOMINAL is the
        # sole reported family, matching the reported scenario exactly (CB-TRIAGE-MATQ-IMP-001).
        priority = ("ABDOMINAL", "DIARRHEA", "VOMITING", "URINARY", "EAR", "HEADACHE", "FEVER", "RESPIRATORY", "RASH")
        return list(dict.fromkeys(
            key for family in priority if family in families for key in policies[family]
        ))
    if intake.stage in MATERNAL_STAGES:
        if "ABDOMINAL" in families:
            base = ["painSeverity", "duration", "vomiting", "hydrationStatus", "temperatureC"]
            return ["abdominalPainPattern", *base] if intake.stage == "PREGNANCY" else base
        if "URINARY" in families:
            return ["urinarySymptoms", "duration", "temperatureC"]
        if "EAR" in families:
            return ["painSeverity", "duration", "temperatureC"]
        if "HEADACHE" in families:
            return ["painSeverity", "duration", "temperatureC"]
        if "DIARRHEA" in families:
            return ["duration", "hydrationStatus", "temperatureC"]
        if "FEVER" in families:
            return ["duration", "temperatureC"]
        if "RESPIRATORY" in families:
            return ["duration", "breathingStatus", "consciousnessStatus"]
        return ["parentFreeText"] if _empty(intake.parentFreeText) and not intake.symptomList else ["duration"]
    if "DIARRHEA" in families:
        return ["duration", "dehydrationSigns", "vomiting", "feedingStatus", "childAgeMonths"]
    if "ABDOMINAL" in families:
        return ["painSeverity", "duration", "vomiting", "feedingStatus", "childAgeMonths"]
    if "URINARY" in families:
        return ["urinarySymptoms", "duration", "temperatureC", "childAgeMonths"]
    if "EAR" in families:
        return ["painSeverity", "duration", "temperatureC", "childAgeMonths"]
    if "HEADACHE" in families:
        return ["painSeverity", "duration", "temperatureC", "childAgeMonths"]
    if "VOMITING" in families:
        return ["duration", "dehydrationSigns", "feedingStatus", "childAgeMonths"]
    if "FEVER" in families:
        return ["duration", "temperatureC", "childAgeMonths", "feedingStatus"]
    if "RESPIRATORY" in families:
        return ["childAgeMonths", "duration", "breathingStatus", "consciousnessStatus"]
    if "RASH" in families:
        return ["duration", "rash", "temperatureC", "childAgeMonths"]
    return ["parentFreeText"] if _empty(intake.parentFreeText) and not intake.symptomList else ["duration"]


def naturalize_followup_questions(questions: list[IntakeQuestion], *, intake: ChildTriageRequest, normalized_symptoms: list[str], gemini_client: GeminiClient | None, deadline: float | None = None) -> tuple[list[IntakeQuestion], str, bool]:
    selected = questions[:MAX_QUESTIONS_PER_ROUND]
    fallback_message = "CareBridge cần thêm một vài thông tin liên quan để phân loại rủi ro an toàn hơn."
    if gemini_client is None or not selected:
        return selected, fallback_message, False
    draft = gemini_client.compose_followup_questions(questions=selected, child_age_months=intake.childAgeMonths, normalized_symptoms=normalized_symptoms, deadline=deadline)
    if draft is None:
        return selected, fallback_message, False
    by_key = {item.questionKey: item for item in draft.questions}
    return [original.model_copy(update={"text": by_key[original.questionKey].text}) for original in selected], draft.assistantMessage, True


def has_red_flag(intake: ChildTriageRequest) -> bool:
    red_flags, _ = apply_red_flag_rules(intake, normalize_symptoms(intake))
    return bool(red_flags)


def should_ask_more(intake: ChildTriageRequest, round_number: int) -> bool:
    return not has_red_flag(intake) and round_number <= MAX_FOLLOWUP_ROUNDS and bool(ask_followup_questions(intake))


def reached_question_limit(round_number: int) -> bool:
    return round_number >= MAX_FOLLOWUP_ROUNDS


def _is_missing(intake: ChildTriageRequest, key: str) -> bool:
    value = getattr(intake, key, None)
    return not _has_value(value)


def _has_value(value: object) -> bool:
    return value is not None and value != "" and value != []


def _coerce_answer(key: str, value: object) -> object:
    if value in ("", None): return None
    if key == "childAgeMonths": return int(float(str(value)))
    if key == "temperatureC": return float(str(value).replace(",", "."))
    if key == "seizure":
        if isinstance(value, bool): return value
        return {"co": True, "yes": True, "true": True, "1": True, "khong": False, "no": False, "false": False, "0": False}.get(_answer_token(value))
    if key == "dehydrationSigns":
        return [str(item) for item in value if _answer_token(item) != "khong"] if isinstance(value, list) else ([] if _answer_token(value) == "khong" else [str(value)])
    return str(value)


def _empty(value: str | None) -> bool:
    return value is None or not value.strip()


def _answer_token(value: object) -> str:
    return strip_accents(str(value)).replace("đ", "d")
