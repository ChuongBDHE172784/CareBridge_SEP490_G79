from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[1]
MEDICAL_SOURCES_DIR = BASE_DIR / "data" / "medical_sources"
MEDICAL_SOURCES_PENDING_DIR = BASE_DIR / "data" / "medical_sources_pending"
OFFICIAL_SOURCES_FILE = BASE_DIR / "data" / "official_sources.json"
RULE_SOURCE_MAPPING_FILE = BASE_DIR / "data" / "triage_rule_source_mapping.json"

DISCLAIMER = (
    "CareBridge không chẩn đoán bệnh, không kê thuốc và không thay thế bác sĩ. "
    "Nếu trẻ có dấu hiệu nặng, hãy liên hệ cơ sở y tế hoặc cấp cứu."
)

RISK_COLORS = {
    "GREEN": "#22C55E",
    "YELLOW": "#FACC15",
    "RED": "#EF4444",
    "NEED_MORE_INFO": "#9CA3AF",
}

OFFICIAL_DOMAIN_WHITELIST = {
    "who.int",
    "moh.gov.vn",
    "mch.moh.gov.vn",
    "cdc.gov",
    "unicef.org",
    "nice.org.uk",
    "aap.org",
    "benhviennhitrunguong.gov.vn",
    "nhidong.org.vn",
    "bvndtp.org.vn",
}

OFFICIAL_SOURCE_WARNING = (
    "Káº¿t quáº£ Ä‘Æ°á»£c xÃ¡c Ä‘á»‹nh bá»Ÿi rule engine ná»™i bá»™ nhÆ°ng chÆ°a tÃ¬m tháº¥y "
    "nguá»“n chÃ­nh thá»‘ng phÃ¹ há»£p trong Knowledge Base. Vui lÃ²ng kiá»ƒm tra láº¡i thÆ° viá»‡n nguá»“n."
)

NO_SOURCE_WARNING = "KhÃ´ng tÃ¬m tháº¥y nguá»“n phÃ¹ há»£p trong knowledge base."

NO_OFFICIAL_REALTIME_SOURCE_WARNING = (
    "KhÃ´ng tÃ¬m tháº¥y nguá»“n chÃ­nh thá»‘ng phÃ¹ há»£p trong Knowledge Base "
    "vÃ  realtime official search."
)

LEGAL_SAFETY_NOTE = (
    "Káº¿t quáº£ nÃ y lÃ  phÃ¢n loáº¡i rá»§i ro ban Ä‘áº§u dá»±a trÃªn triá»‡u chá»©ng vÃ  "
    "nguá»“n tham kháº£o chÃ­nh thá»‘ng, khÃ´ng pháº£i cháº©n Ä‘oÃ¡n y khoa."
)
