import re
import unicodedata

from app.gemini_client import GeminiClient, sanitize_symptom_text
from app.schemas import ChildTriageRequest, NormalizedSymptom


# Only these codes may cross the deterministic rule-engine boundary.
ONTOLOGY: dict[str, tuple[str, ...]] = {
    "fever": ("sot", "fever", "temperature"),
    "high_fever": ("sot cao", "high fever"),
    "cough": ("ho", "cough"),
    "runny_nose": ("so mui", "chay mui", "runny nose"),
    "difficulty_breathing": ("kho tho", "tho gap", "wheeze", "difficulty breathing"),
    "chest_indrawing": ("rut lom", "chest indrawing"),
    "cyanosis": ("tim tai", "moi tim", "da tim", "cyanosis"),
    "seizure": ("co giat", "seizure", "convulsion"),
    "lethargy": ("li bi", "lo mo", "ngu ga", "lethargy"),
    "difficult_to_wake": ("kho danh thuc", "difficult to wake"),
    "unable_to_drink": ("khong uong", "khong bu", "unable to drink"),
    "poor_feeding": ("bo bu", "uong kem", "an kem", "poor feeding"),
    "vomiting": ("non", "oi", "vomit", "vomiting"),
    "persistent_vomiting": ("non lien tuc", "non nhieu", "vomiting everything", "persistent vomiting"),
    "diarrhea": ("tieu chay", "diarrhea"),
    "mild_dehydration": ("moi kho", "tieu it", "mild dehydration"),
    "severe_dehydration": ("mat nuoc nang", "mat trung", "khoc khong co nuoc mat", "severe dehydration"),
    "rash": ("phat ban", "noi ban", "rash"),
    "worsening_condition": ("nang hon", "xau di", "worsening"),
}

INSTRUCTION_PATTERNS = (
    r"ignore (?:all |the )?(?:previous |prior )?(?:rules|instructions)",
    r"return\s+(?:green|yellow|red)",
    r"say (?:my )?child is (?:normal|fine|healthy)",
    r"tra\s+(?:ve|loi)\s+(?:green|yellow|red)",
    r"bo qua (?:quy tac|huong dan)",
)


def strip_accents(value: str) -> str:
    normalized = unicodedata.normalize("NFD", value.lower())
    normalized = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", " ", normalized).strip()


def remove_instruction_like_text(value: str) -> str:
    safe = strip_accents(value)
    for pattern in INSTRUCTION_PATTERNS:
        safe = re.sub(pattern, " ", safe, flags=re.IGNORECASE)
    return re.sub(r"\s+", " ", safe).strip()


CANONICAL_SYMPTOM_CODES = frozenset(ONTOLOGY)


def normalize_symptom_details_deterministic(intake: ChildTriageRequest) -> list[NormalizedSymptom]:
    fragments = list(intake.symptomList)
    fragments.extend(filter(None, [
        intake.feedingStatus, intake.breathingStatus, intake.consciousnessStatus,
        intake.vomiting, intake.diarrhea, intake.rash, *intake.dehydrationSigns,
        intake.parentFreeText,
    ]))
    safe_text = f" {remove_instruction_like_text(' '.join(fragments))} "
    found: dict[str, NormalizedSymptom] = {}
    for code, keywords in ONTOLOGY.items():
        candidates = (code, code.replace("_", " "), *keywords)
        match = next((
            keyword for keyword in candidates
            if re.search(
                rf"(?<!\w){re.escape(keyword)}(?!\w)",
                _without_negated_candidate(safe_text, keyword),
            )
        ), None)
        if match:
            found[code] = NormalizedSymptom(
                originalTextMasked=_mask(match),
                normalizedCode=code,
                normalizationMethod="EXACT" if safe_text.strip() == match else "KEYWORD",
                normalizationConfidence=1.0 if safe_text.strip() == match else 0.94,
                exactMatch=safe_text.strip() == match,
            )

    def structured(code: str, present: bool) -> None:
        if present and code not in found:
            found[code] = NormalizedSymptom(
                originalTextMasked="[structured answer]",
                normalizedCode=code,
                normalizationMethod="STRUCTURED",
                normalizationConfidence=1.0,
                exactMatch=True,
            )

    structured("fever", intake.temperatureC is not None and intake.temperatureC >= 37.5)
    structured("high_fever", intake.temperatureC is not None and intake.temperatureC >= 39.0)
    structured("seizure", intake.seizure is True)
    structured("mild_dehydration", bool(intake.dehydrationSigns))
    return [found[code] for code in sorted(found)]


def normalize_symptom_details_with_metadata(
    intake: ChildTriageRequest,
    gemini_client: GeminiClient | None = None,
    deadline: float | None = None,
) -> tuple[list[NormalizedSymptom], bool]:
    deterministic = normalize_symptom_details_deterministic(intake)
    free_text = (intake.parentFreeText or "").strip()
    if gemini_client is None or not free_text or not _needs_gemini(free_text):
        return deterministic, False

    result = gemini_client.normalize_symptom_text(
        text=free_text,
        child_age_months=intake.childAgeMonths,
        allowed_codes=set(CANONICAL_SYMPTOM_CODES),
        deadline=deadline,
    )
    if result is None:
        return deterministic, False

    merged = {item.normalizedCode: item for item in deterministic}
    for item in result.normalizedSymptoms:
        if item.code not in CANONICAL_SYMPTOM_CODES or item.code in merged:
            continue
        merged[item.code] = NormalizedSymptom(
            originalTextMasked="[gemini matched phrase]",
            normalizedCode=item.code,
            normalizationMethod="GEMINI_STRUCTURED_OUTPUT",
            normalizationConfidence=item.confidence,
            exactMatch=False,
        )
    return [merged[code] for code in sorted(merged)], True


def normalize_symptom_details(
    intake: ChildTriageRequest,
    gemini_client: GeminiClient | None = None,
) -> list[NormalizedSymptom]:
    details, _ = normalize_symptom_details_with_metadata(intake, gemini_client)
    return details


def normalize_symptoms(intake: ChildTriageRequest) -> list[str]:
    return [item.normalizedCode for item in normalize_symptom_details_deterministic(intake)]


def _mask(text: str) -> str:
    # Store only the matched phrase, never the full parent narrative.
    return re.sub(r"\d", "*", sanitize_symptom_text(text))[:48]


def _without_negated_candidate(text: str, candidate: str) -> str:
    if candidate.startswith(("khong ", "chua ")):
        return text
    return re.sub(
        rf"(?<!\w)(?:khong|chua|not|no|never|without)\s+"
        rf"(?:(?:co|bi|con|he|have|has|had|feel|feeling|any|signs?|symptoms?|of|currently)\s+){{0,4}}"
        rf"{re.escape(candidate)}(?!\w)",
        " ",
        text,
    )


def _needs_gemini(text: str) -> bool:
    remainder = f" {remove_instruction_like_text(text)} "
    known_terms = sorted(
        {code.replace("_", " ") for code in ONTOLOGY} |
        {term for terms in ONTOLOGY.values() for term in terms},
        key=len,
        reverse=True,
    )
    for term in known_terms:
        remainder = re.sub(rf"(?<!\w){re.escape(term)}(?!\w)", " ", remainder)
    stopwords = {
        "be", "tre", "con", "nha", "em", "toi", "dang", "co", "va", "voi",
        "hien", "tai", "hoi", "hon", "binh", "thuong", "mot", "kieu", "thay",
    }
    tokens = [token for token in re.findall(r"[a-z]+", remainder) if token not in stopwords]
    return any(len(token) >= 2 for token in tokens)
