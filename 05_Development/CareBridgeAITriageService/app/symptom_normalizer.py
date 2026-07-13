import re
import unicodedata

from app.schemas import ChildTriageRequest


KEYWORDS = {
    "fever": ["sot", "nong", "temperature", "fever"],
    "cough": [" ho ", "cough"],
    "runny_nose": ["so mui", "chay mui", "runny"],
    "breathing_difficulty": ["kho tho", "tho gap", "rut lom", "tim tai", "wheeze"],
    "cyanosis": ["tim tai", "moi tim", "da tim"],
    "seizure": ["co giat", "seizure", "convulsion"],
    "lethargy": ["li bi", "lo mo", "kho danh thuc", "ngu ga"],
    "poor_feeding": ["bo bu", "khong uong", "khong bu", "uong kem", "an kem"],
    "vomiting": [" non", " non ", " oi ", "vomit"],
    "diarrhea": ["tieu chay", "diarrhea"],
    "rash": ["phat ban", "noi ban", "rash"],
    "dehydration": ["mat nuoc", "khoc khong co nuoc mat", "moi kho", "tieu it", "mat trung"],
}


def strip_accents(value: str) -> str:
    normalized = unicodedata.normalize("NFD", value.lower())
    normalized = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", " ", normalized)


def normalize_symptoms(intake: ChildTriageRequest) -> list[str]:
    parts: list[str] = []
    parts.extend(intake.symptomList)
    parts.extend([
        intake.duration or "",
        intake.feedingStatus or "",
        intake.breathingStatus or "",
        intake.consciousnessStatus or "",
        intake.vomiting or "",
        intake.diarrhea or "",
        intake.rash or "",
        " ".join(intake.dehydrationSigns),
        intake.parentFreeText or "",
    ])
    text = f" {strip_accents(' '.join(parts))} "
    normalized: set[str] = set()

    for symptom, keywords in KEYWORDS.items():
        if any(keyword in text for keyword in keywords):
            normalized.add(symptom)

    if intake.temperatureC is not None and intake.temperatureC >= 37.5:
        normalized.add("fever")
    if intake.seizure is True:
        normalized.add("seizure")
    if intake.dehydrationSigns:
        normalized.add("dehydration")

    return sorted(normalized)
