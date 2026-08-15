"""Fail-closed deterministic extraction of user-reported numeric measurements.

Only supported units, plausible values, current assertions, and measurements attributable to the
selected patient are emitted. Ambiguous values are exposed as conflicts so stale text-derived
state can be removed; trusted current ``MEASURED`` observations always retain precedence.
"""

from __future__ import annotations

import re
import unicodedata
from dataclasses import dataclass
from typing import Mapping


_NUMBER_WORD = r"(?:mot|hai|ba|bon|tu|nam|sau|bay|tam|chin|muoi(?:\s+(?:mot|hai))?)"
_MONTH_NUMBER = rf"(?:\d{{1,2}}|{_NUMBER_WORD})"
_AGE_MODIFIERS = r"(?:(?:duoc|moi|da|hien|nay|khoang)\s+){0,3}"
_BABY_MONTHS_WITH_SUBJECT = re.compile(
    rf"\b(?:be|con)(?:\s+(?:nha\s+em|cua\s+(?:toi|em)))?\s+"
    rf"{_AGE_MODIFIERS}(?P<number>{_MONTH_NUMBER})\s+thang(?:\s+tuoi)?\b"
)
_BABY_MONTHS_AS_AGE = re.compile(rf"\b(?P<number>{_MONTH_NUMBER})\s+thang\s+tuoi\b")
_TEMPERATURE = re.compile(
    r"(?<![\d/])(?P<number>\d{1,3}(?:[.,]\d{1,2})?)\s*"
    r"(?P<unit>°\s*[cfk]?|[cfk]\b|do(?:\s*[cfk])?\b)",
    re.IGNORECASE,
)
_TEMPERATURE_ANCHOR = re.compile(
    r"\b(?:sot|nhiet\s+do|than\s+nhiet|nhiet\s+ke|do(?:\s+duoc)?)\b",
    re.IGNORECASE,
)
_HISTORICAL_OR_RESOLVED = re.compile(
    r"\b(?:hom\s+qua|truoc\s+day|lan\s+truoc|thang\s+truoc|tuan\s+truoc|"
    r"da\s+tung|tung|da\s+het\s+sot|gio\s+da\s+het|hien\s+nay\s+da\s+het|"
    r"nay\s+khong\s+con)\b"
)
_HYPOTHETICAL = re.compile(
    r"\b(?:neu|gia\s+su|thi\s+sao|co\s+phai|bao\s+nhieu)\b"
)
_ENVIRONMENT = re.compile(
    r"\b(?:nhiet\s+do\s+phong|phong|nuoc|thoi\s+tiet|ngoai\s+troi|may\s+lanh|dieu\s+hoa)\b"
)
_BABY_SUBJECT = re.compile(r"(?<!\w)(?:bé|be|con)(?!\w)", re.IGNORECASE)
_MOTHER_SUBJECT = re.compile(r"(?<!\w)(?:mẹ|me|em|tôi|toi|mình|minh)(?!\w)", re.IGNORECASE)
_CLAUSE_BOUNDARY = re.compile(r"(?<!\d),(?!\d)|[;.!?]")
_WORD_VALUES = {
    "mot": 1,
    "hai": 2,
    "ba": 3,
    "bon": 4,
    "tu": 4,
    "nam": 5,
    "sau": 6,
    "bay": 7,
    "tam": 8,
    "chin": 9,
    "muoi": 10,
    "muoi mot": 11,
    "muoi hai": 12,
}
_PROVENANCE = "USER_REPORTED_TEXT"


@dataclass(frozen=True)
class ReportedMeasurements:
    """Audit observations, direct baby age, and codes ambiguous in the latest message."""

    measurements: Mapping[str, Mapping[str, object]]
    baby_age_months: int | None = None
    conflicted_codes: frozenset[str] = frozenset()
    invalid_codes: frozenset[str] = frozenset()


def extract_reported_measurements(
    message: object, *, target_entity: object = None
) -> ReportedMeasurements:
    """Extract current baby months and Celsius readings, otherwise remain silent."""

    if type(message) is not str or not message.strip():
        return ReportedMeasurements({})

    folded = _fold(message)
    target = getattr(target_entity, "value", target_entity)
    hypothetical = _HYPOTHETICAL.search(folded) is not None
    ages, invalid_ages = (
        (set(), set()) if hypothetical else _baby_month_values(message, folded, target)
    )
    temperatures, invalid_temperatures = _temperature_values(message, folded, target)
    measurements: dict[str, Mapping[str, object]] = {}
    conflicts: set[str] = set()

    baby_age = next(iter(ages)) if len(ages) == 1 else None
    if len(ages) > 1:
        conflicts.add("babyAgeMonths")
    elif baby_age is not None:
        measurements["babyAgeMonths"] = _observation(baby_age, "MONTHS")

    if len(temperatures) > 1:
        conflicts.add("temperatureC")
    elif len(temperatures) == 1:
        measurements["temperatureC"] = _observation(next(iter(temperatures)), "C")
    invalid_codes: set[str] = set()
    if invalid_ages:
        invalid_codes.add("babyAgeMonths")
    if invalid_temperatures:
        invalid_codes.add("temperatureC")
    return ReportedMeasurements(
        measurements, baby_age, frozenset(conflicts), frozenset(invalid_codes)
    )


def _baby_month_values(message: str, folded: str, target: object) -> tuple[set[int], set[int]]:
    values: set[int] = set()
    invalid: set[int] = set()
    for match in _BABY_MONTHS_WITH_SUBJECT.finditer(folded):
        # Accent folding turns Vietnamese "còn" into "con". Verify the authored subject against
        # the original string before treating it as a baby reference.
        original_prefix = message[match.start():match.start() + 3].lower()
        if original_prefix.startswith("cò"):
            continue
        value = _parse_month_number(match.group("number"))
        if value is not None and 0 <= value < 24:
            values.add(value)
        elif value is not None:
            invalid.add(value)

    # "Hai tháng tuổi" is explicitly an age even before target resolution. For a resolved baby
    # target this also covers follow-up answers that omit the subject.
    if target in {"BABY", "UNKNOWN", None}:
        for match in _BABY_MONTHS_AS_AGE.finditer(folded):
            value = _parse_month_number(match.group("number"))
            if value is not None and 0 <= value < 24:
                values.add(value)
            elif value is not None:
                invalid.add(value)
    return values, invalid


def _temperature_values(message: str, folded: str, target: object) -> tuple[set[float], set[float]]:
    values: set[float] = set()
    invalid: set[float] = set()
    for match in _TEMPERATURE.finditer(folded):
        unit = re.sub(r"\s+", "", match.group("unit").lower())
        if unit.endswith(("f", "k")):
            continue

        clause_start, clause_end = _clause_bounds(folded, match.start(), match.end())
        folded_clause = folded[clause_start:clause_end]
        original_clause = message[clause_start:clause_end]
        relative_start = match.start() - clause_start

        if (
            _TEMPERATURE_ANCHOR.search(folded_clause) is None
            or _HISTORICAL_OR_RESOLVED.search(folded_clause) is not None
            or _HYPOTHETICAL.search(folded_clause) is not None
            or _ENVIRONMENT.search(folded_clause) is not None
            or not _attributable_to_target(original_clause, relative_start, target)
        ):
            continue

        value = float(match.group("number").replace(",", "."))
        if 30.0 <= value <= 45.0:
            values.add(value)
        else:
            invalid.add(value)
    return values, invalid


def _clause_bounds(value: str, start: int, end: int) -> tuple[int, int]:
    before = [match.end() for match in _CLAUSE_BOUNDARY.finditer(value, 0, start)]
    after = [match.start() for match in _CLAUSE_BOUNDARY.finditer(value, end)]
    return (before[-1] if before else 0, after[0] if after else len(value))


def _attributable_to_target(clause: str, measurement_start: int, target: object) -> bool:
    subjects: list[tuple[int, str]] = []
    subjects.extend((match.start(), "BABY") for match in _BABY_SUBJECT.finditer(clause))
    subjects.extend((match.start(), "MOTHER") for match in _MOTHER_SUBJECT.finditer(clause))
    if not subjects:
        return target in {"BABY", "MOTHER"}

    before = [item for item in subjects if item[0] <= measurement_start]
    nearest = max(before, default=min(subjects, key=lambda item: item[0]), key=lambda item: item[0])
    if target in {"BABY", "MOTHER"}:
        return nearest[1] == target
    # Before target resolution, an explicit patient subject is enough to establish that this is
    # a current health measurement. Entity resolution still decides which clinical graph owns it.
    return target in {"UNKNOWN", None}


def _parse_month_number(value: str) -> int | None:
    if value.isdigit():
        number = int(value)
        return number
    return _WORD_VALUES.get(value)


def _observation(value: int | float, unit: str) -> dict[str, object]:
    return {
        "value": value,
        "unit": unit,
        "temporalStatus": "CURRENT",
        "provenance": _PROVENANCE,
    }


def _fold(value: str) -> str:
    lowered = unicodedata.normalize("NFD", value.lower().replace("đ", "d"))
    accent_free = "".join(char for char in lowered if unicodedata.category(char) != "Mn")
    return re.sub(r"\s+", " ", accent_free).strip()
