import re
import unicodedata

from app.gemini_client import GeminiClient, sanitize_symptom_text
from app.schemas import ChildTriageRequest, NormalizedSymptom


# Only these codes may cross the deterministic rule-engine boundary.
# Folk-term synonyms per TDS CB-TRIAGE-IMP-005 §5.3 (S2, S4–S13). Values are the engine's
# accent-stripped forms; 'đ' (U+0111) has no NFD decomposition, so đ-variants ("lu đu",
# "đi ngoai") are stored alongside the plain-d forms to match diacritic input.
ONTOLOGY: dict[str, tuple[str, ...]] = {
    "fever": ("sot", "fever", "temperature", "ham hap"),
    "high_fever": ("sot cao", "high fever"),
    "cough": ("ho", "cough"),
    "runny_nose": ("so mui", "chay mui", "runny nose", "sut sit"),
    # These five carry the danger signs, and each had an ASCII twin among ordinary words:
    # "có giặt"/"co giật", "mới tìm"/"môi tím", "ngủ gà"/"ngu ga". They are written with
    # diacritics so a writer who uses them is taken at their word; accent-free input is still
    # matched (see _ontology_match).
    "difficulty_breathing": ("khó thở", "thở gấp", "wheeze", "difficulty breathing", "khò khè", "thở rít"),
    "chest_indrawing": ("rút lõm", "chest indrawing"),
    "cyanosis": ("tím tái", "môi tím", "da tím", "cyanosis"),
    "seizure": ("co giật", "seizure", "seizures", "convulsion", "convulsions"),
    "lethargy": ("li bì", "lơ mơ", "ngủ gà", "lethargy", "lừ đừ"),
    "difficult_to_wake": ("kho danh thuc", "difficult to wake"),
    "unable_to_drink": ("khong uong", "khong bu", "unable to drink"),
    "poor_feeding": ("bo bu", "uong kem", "an kem", "poor feeding", "bieng an"),
    "vomiting": ("non", "oi", "vomit", "vomiting", "tro sua", "oc sua"),
    "persistent_vomiting": ("non lien tuc", "non nhieu", "vomiting everything", "persistent vomiting"),
    "diarrhea": ("tieu chay", "diarrhea", "di ngoai", "đi ngoai", "ia chay"),
    # Common presentations are intentionally descriptive rather than diagnostic.
    # They only select a bounded, stage-scoped follow-up policy.
    "abdominal_pain": ("dau bung", "dau da day", "dau quanh ron", "abdominal pain", "stomach ache", "tummy ache", "belly pain"),
    "nausea": ("buon non", "non nao", "nausea", "queasy"),
    "constipation": ("tao bon", "kho di ngoai", "khong di ngoai duoc", "constipation", "hard stool"),
    "mild_dehydration": ("moi kho", "tieu it", "mild dehydration"),
    "severe_dehydration": ("mat nuoc nang", "mat trung", "khoc khong co nuoc mat", "severe dehydration"),
    "rash": ("phat ban", "noi ban", "rash", "rom say"),
    "itching": ("ngua", "ngua da", "itch", "itchy"),
    "urinary_discomfort": ("tieu buot", "tieu rat", "dau khi tieu", "dysuria", "painful urination"),
    "ear_pain": ("dau tai", "nhuc tai", "earache", "ear pain"),
    "headache": ("dau dau", "nhuc dau", "headache", "head pain"),
    "cold_symptoms": ("cam lanh", "cam cum", "cold symptoms", "common cold"),
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
    return re.sub(r"\s+", " ", normalized.replace("đ", "d")).strip()


def collapse_case_and_space(value: str) -> str:
    """Lowercase and collapse whitespace, keeping diacritics."""

    return re.sub(r"\s+", " ", unicodedata.normalize("NFC", value.lower())).strip()


def strip_accents_aligned(value: str) -> str:
    """Strip diacritics one character at a time, so the result keeps the input's length.

    ``strip_accents`` also collapses whitespace, which moves every offset after the change.
    Callers that need to look up what the writer originally typed at a matched offset need
    the two spellings to line up exactly, and that is what this gives them.
    """

    characters = []
    for character in unicodedata.normalize("NFC", value):
        if character == "đ":
            characters.append("d")
            continue
        base = "".join(
            part
            for part in unicodedata.normalize("NFD", character)
            if unicodedata.category(part) != "Mn"
        )
        # Anything that does not reduce to exactly one character is left alone rather than
        # silently shifting every offset after it.
        characters.append(base if len(base) == 1 else character)
    return "".join(characters)


def remove_instruction_like_text(value: str) -> str:
    safe = strip_accents(value)
    for pattern in INSTRUCTION_PATTERNS:
        safe = re.sub(pattern, " ", safe, flags=re.IGNORECASE)
    return re.sub(r"\s+", " ", safe).strip()


CANONICAL_SYMPTOM_CODES = frozenset(ONTOLOGY)


def _ontology_match(safe_text: str, accented_text: str | None, keyword: str) -> str | None:
    """The spelling of ``keyword`` that occurs unnegated, or None.

    A keyword written with diacritics matches the accented text directly. It also matches the
    stripped text, but only across a span the writer left accent-free — otherwise "có giặt"
    (does the washing) is read as "co giật" (a seizure), which is how ordinary sentences ended
    up escalating. The spelling that matched is returned rather than the catalogue's, so the
    recorded provenance stays the writer's own text.
    """

    stripped_keyword = strip_accents_aligned(keyword)
    # Negation is detected on the stripped spelling (its token list is ASCII, so "không" only
    # registers there) and blanked out of both spellings at the same offsets.
    stripped, accented = _blank_negated(safe_text, accented_text, stripped_keyword)
    if accented is not None and keyword != stripped_keyword:
        if re.search(rf"(?<!\w){re.escape(keyword)}(?!\w)", accented):
            return keyword
    pattern = rf"(?<!\w){re.escape(stripped_keyword)}(?!\w)"
    for match in re.finditer(pattern, stripped):
        if accented is None or keyword == stripped_keyword:
            return stripped_keyword
        if accented[match.start():match.end()] == stripped[match.start():match.end()]:
            return stripped_keyword
    return None


def _blank_negated(
    stripped: str, accented: str | None, candidate: str
) -> tuple[str, str | None]:
    """Blank each negated occurrence of ``candidate``, keeping both spellings aligned.

    The span is overwritten with spaces rather than removed: collapsing it would shift every
    offset after it, and the two spellings are only comparable while their offsets agree.
    """

    if candidate.startswith(("khong ", "chua ")):
        return stripped, accented
    pattern = (
        rf"(?<!\w)(?:khong|chua|not|no|never|without)\s+"
        rf"(?:(?:co|bi|con|he|have|has|had|feel|feeling|any|signs?|symptoms?|of|currently)\s+){{0,4}}"
        rf"{re.escape(candidate)}(?!\w)"
    )
    spans = [match.span() for match in re.finditer(pattern, stripped)]
    if not spans:
        return stripped, accented

    def blanked(text: str) -> str:
        characters = list(text)
        for start, end in spans:
            characters[start:end] = " " * (end - start)
        return "".join(characters)

    return blanked(stripped), None if accented is None else blanked(accented)


def normalize_symptom_details_deterministic(intake: ChildTriageRequest) -> list[NormalizedSymptom]:
    fragments = list(intake.symptomList)
    fragments.extend(filter(None, [
        intake.feedingStatus, intake.breathingStatus, intake.consciousnessStatus,
        intake.vomiting, intake.diarrhea, intake.rash, intake.painSeverity,
        intake.urinarySymptoms, *intake.dehydrationSigns,
        intake.parentFreeText,
    ]))
    raw_text = " ".join(fragments)
    safe_text = f" {remove_instruction_like_text(raw_text)} "
    # The accented spelling is only usable while it still lines up with the stripped one. The
    # injection scrubber rewrites spans, so on the rare input it fires against the two drift
    # apart and matching falls back to the stripped text alone — an injection attempt is not
    # the place to start trusting diacritics.
    accented_text = f" {collapse_case_and_space(raw_text)} "
    if strip_accents_aligned(accented_text) != safe_text:
        accented_text = None
    found: dict[str, NormalizedSymptom] = {}
    for code, keywords in ONTOLOGY.items():
        candidates = (code, code.replace("_", " "), *keywords)
        match = next(
            filter(
                None,
                (
                    _ontology_match(safe_text, accented_text, keyword)
                    for keyword in candidates
                ),
            ),
            None,
        )
        if match:
            # Compared stripped on both sides: whether the writer used diacritics does not
            # change whether their message was only this one symptom.
            exact = safe_text.strip() == strip_accents_aligned(match)
            found[code] = NormalizedSymptom(
                originalTextMasked=_mask(match),
                normalizedCode=code,
                normalizationMethod="EXACT" if exact else "KEYWORD",
                normalizationConfidence=1.0 if exact else 0.94,
                exactMatch=exact,
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
    # `remainder` is accent-stripped, so the ontology's terms have to be compared in that
    # spelling too — otherwise every accented term would survive here as "unrecognised" and
    # send otherwise-understood text to Gemini.
    known_terms = sorted(
        {code.replace("_", " ") for code in ONTOLOGY} |
        {strip_accents_aligned(term) for terms in ONTOLOGY.values() for term in terms},
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
