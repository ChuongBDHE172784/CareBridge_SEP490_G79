"""Danger phrases written the way people write them, and the matcher that reads them.

Extracted from ``risk_rules`` so it has exactly one implementation. V1 scores maternal RED
flags with it; V2 uses it as a deterministic floor under Gemini extraction. Keeping two copies
of Vietnamese phrase matching in one codebase is how the two engines would drift apart on the
question that matters most, and V2 may not import ``app.risk_rules`` (see
``tests/test_triage_dependency_boundaries.py``) — so the shared piece lives here, depending
on neither.

Nothing in this module decides an outcome. It reports which danger phrases a message contains,
unnegated; the callers' rule engines decide what that means.
"""

from __future__ import annotations

import re
import unicodedata
from functools import lru_cache
from typing import Iterator, NamedTuple

from app.symptom_normalizer import strip_accents_aligned as _accent_free

#: Words that negate a danger sign standing in front of it in the same clause.
_NEGATION_TOKENS = (
    "khong", "chua", "chang", "cha", "not", "no", "never", "without", "denies", "denied",
)

#: Degree adverbs a speaker drops *inside* a fixed phrase. "ra máu nhiều" matched but
#: "ra máu rất nhiều" did not, so describing the bleeding as worse made the RED rule stop
#: firing — for postpartum haemorrhage that is the difference between showing the 115 route
#: and not. Only these tokens are allowed to sit in the gap: permitting arbitrary words would
#: let "tự nhiên tử tế" satisfy "tự tử", and a self-harm false positive is its own harm.
_INTENSIFIER_TOKENS = (
    "rất", "quá", "khá", "hơi", "cực", "kỳ", "vô", "cùng", "thật", "siêu", "hết", "sức",
)
#: Two covers the compound intensifiers ("cực kỳ", "vô cùng", "hết sức").
_MAX_INTENSIFIER_GAP = 2

#: Fields whose answers feed the complaint-independent RED rules (RED_LETHARGY,
#: RED_BREATHING_DISTRESS, RED_SEIZURE). Defined here rather than in the question engine so
#: both the planner that asks them and the scorer that requires them share one list, and so
#: risk_rules stays free of an import cycle back into the engine.
DANGER_SIGN_KEYS = ("consciousnessStatus", "breathingStatus", "seizure")

# Phrases are written WITH diacritics on purpose. They used to be stored accent-stripped and
# matched against accent-stripped text, which merged unrelated Vietnamese words onto the same
# ASCII form and fired RED on ordinary sentences: "có giặt" ("does the washing") read as
# "co giật" (seizure), "ngắt sữa" (weaning) as "ngất" (fainting), "mới tìm" ("just looked up")
# as "môi tím" (blue lips), and "từ từ" ("slowly") as "tự tử" (suicide). A mother writing
# "bé bú từ từ" was routed to self-harm support. See _phrase_spans for how accent-free input
# is still accepted.
MATERNAL_BREATHING_DISTRESS_PHRASES = (
    "khó thở",
    "không thở được",
    "thiếu hơi",
    "nghẹt thở",
    "difficulty breathing",
    "breathing difficulty",
    "shortness of breath",
    "cannot breathe",
    "unable to breathe",
)
MATERNAL_CYANOSIS_PHRASES = (
    "tím tái",
    "tím môi",
    "môi tím",
    "blue lips",
    "cyanosis",
    "cyanotic",
)
MATERNAL_SEIZURE_PHRASES = ("co giật", "seizure", "convulsion")
MATERNAL_ALTERED_CONSCIOUSNESS_PHRASES = (
    "lơ mơ",
    "li bì",
    "khó đánh thức",
    "khó giữ tỉnh táo",
    "bất tỉnh",
    "ngất",
    "ngất xỉu",
    "faint",
    "fainted",
    "fainting",
    "unconscious",
    "altered consciousness",
    "difficult to wake",
    "loss of consciousness",
)
MATERNAL_HEAVY_BLEEDING_PHRASES = (
    "băng huyết",
    "chảy máu nhiều",
    "ra máu nhiều",
    # Word order varies and "ồ ạt" carries the volume without the word "nhiều"; both were
    # observed to miss while describing the same haemorrhage.
    "máu ra nhiều",
    "máu ồ ạt",
    "mất máu nhiều",
    "thấm ướt băng",
    "ướt đẫm băng",
    "heavy bleeding",
    "bleeding heavily",
    "hemorrhage",
    "haemorrhage",
    "soaking pad",
)
#: The two halves of PREG_RED_002, ported from the registry under D-030. Neither escalates
#: alone — the registry states the rule as a conjunction, so a lone headache or a lone
#: "hoa mắt" stays silent, which is what keeps this from becoming a broad new trigger.
MATERNAL_SEVERE_HEADACHE_PHRASES = (
    "đau đầu dữ dội",
    "đau đầu nhiều",
    "nhức đầu dữ dội",
    "nhức đầu nhiều",
    # Bệnh viện Từ Dũ words the pre-eclampsia warning as "Đau đầu kéo dài, mức độ tăng dần,
    # không thuyên giảm khi nghỉ ngơi..." (tudu.com.vn, pre-eclampsia warning-signs page).
    # Added under D-031 as another spelling of a sign PREG_RED_002 already acts on; the mirror
    # "nhức đầu" form follows the pattern of the four above. Neither matches "đau đầu nhẹ" or
    # "đau đầu chút rồi hết", and neither reaches RED alone — the rule is a conjunction with
    # VISUAL_DISTURBANCE.
    "đau đầu kéo dài",
    "nhức đầu kéo dài",
    "severe headache",
)
MATERNAL_VISUAL_DISTURBANCE_PHRASES = (
    "mờ mắt",
    "mắt mờ",
    "nhìn mờ",
    "hoa mắt",
    "nhìn không rõ",
    "blurred vision",
    "visual disturbance",
)
MATERNAL_SELF_HARM_PHRASES = (
    "tự làm hại",
    "làm hại bản thân",
    "tự hại",
    "tự sát",
    "tự tử",
    "kết liễu",
    "muốn chết",
    # Indirect phrasings are the common ones; "muốn chết" alone missed both of these.
    "không muốn sống",
    "không thiết sống",
    # Bệnh viện Bạch Mai lists this euphemism among the postpartum-depression warning signs
    # (bachmai.gov.vn): "muốn 'giải thoát' cho cả mẹ và con". Kept as the whole phrase — bare
    # "giải thoát" is an ordinary word ("giải thoát khỏi cơn đau"). Added under D-029.
    "giải thoát cho cả mẹ và con",
    "self harm",
    "self-harm",
    "kill myself",
    "suicide",
    "suicidal",
    "want to die",
)



class Text(NamedTuple):
    """One input held in both spellings, character-aligned.

    ``stripped`` is produced one character at a time so it has the same length as
    ``accented``. That is what lets a match found in either spelling be checked against the
    other at the same offsets — the negation scan runs on ``stripped`` (its token list is
    ASCII) while the diacritic check reads ``accented``.
    """

    accented: str
    stripped: str


def normalized_text(value: str | None) -> Text:
    accented = _expand_english_negative_contractions(
        re.sub(r"\s+", " ", unicodedata.normalize("NFC", (value or "").lower())).strip()
    )
    return Text(accented, _accent_free(accented))


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


def canonical_sign_is_unnegated(
    canonical_signs: set[str],
    sign: str,
    reported_signs: Text,
    *phrases: str,
) -> bool:
    if sign not in canonical_signs:
        return False
    mentioned_phrases = [
        phrase for phrase in phrases if any(phrase_spans(reported_signs, phrase))
    ]
    return not mentioned_phrases or any(
        contains_unnegated_phrase(reported_signs, phrase)
        for phrase in mentioned_phrases
    )


def text_contains_any(value: Text, *tokens: str) -> bool:
    return any(contains_unnegated_phrase(value, token) for token in tokens)


def contains_unnegated_phrase(value: Text, phrase: str) -> bool:
    return any(
        not _negated_before(value.stripped, start)
        for start in phrase_spans(value, phrase)
    )


def phrase_spans(value: Text, phrase: str) -> Iterator[int]:
    """Yield the start offset of every occurrence of ``phrase``, negation aside.

    Diacritics are matched when the writer supplied them. A match on the stripped spelling
    counts only where the source span carries no diacritics at all — that is a writer typing
    without accents, where "co giat" genuinely cannot be told apart from "co giật" and the
    safe reading is the clinical one. Where the writer did type accents, "có giặt" is their
    own word and must not be read as a seizure.
    """

    accented_pattern, stripped_pattern = _phrase_patterns(phrase)
    seen: set[int] = set()
    for match in accented_pattern.finditer(value.accented):
        seen.add(match.start())
        yield match.start()
    for match in stripped_pattern.finditer(value.stripped):
        start, end = match.start(), match.end()
        if start in seen:
            continue
        if value.accented[start:end] == value.stripped[start:end]:
            yield start


def _negated_before(stripped: str, start: int) -> bool:
    """True when the clause leading up to ``start`` negates what follows."""

    clause_prefix = re.split(
        r"[,.!?;:]|\b(?:nhung|ma|song|tuy nhien|however|but|va|and)\b",
        stripped[:start],
    )[-1]
    preceding_tokens = re.findall(r"[a-z]+", clause_prefix)[-6:]
    return bool(set(_NEGATION_TOKENS) & set(preceding_tokens))


@lru_cache(maxsize=None)
def _phrase_patterns(phrase: str) -> tuple[re.Pattern[str], re.Pattern[str]]:
    """The phrase compiled in both spellings; cached because this runs per request."""

    return (
        _compile_phrase(phrase, _INTENSIFIER_TOKENS),
        _compile_phrase(
            _accent_free(phrase), tuple(_accent_free(t) for t in _INTENSIFIER_TOKENS)
        ),
    )


def _compile_phrase(phrase: str, intensifiers: tuple[str, ...]) -> re.Pattern[str]:
    gap = (
        rf"(?:\s+(?:{'|'.join(re.escape(token) for token in intensifiers)}))"
        rf"{{0,{_MAX_INTENSIFIER_GAP}}}\s+"
    )
    body = gap.join(re.escape(word) for word in phrase.split())
    return re.compile(rf"(?<!\w){body}(?!\w)")
