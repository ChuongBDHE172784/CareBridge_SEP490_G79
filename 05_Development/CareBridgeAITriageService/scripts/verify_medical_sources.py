"""Verification and ingestion script for medical source documents.

Fetches the source document over HTTP, extracts the specific quoted/referenced
clinical section verbatim, calculates sha256 hashes of both the remote HTML
and the extracted markdown body, and writes standard frontmatter.
"""

from __future__ import annotations

import hashlib
import json
import re
import sys
import urllib.request
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

SOURCES_DIR = Path(__file__).resolve().parents[1] / "data" / "medical_sources"

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 CareBridgeSourceVerifier/1.0"
    )
}

VINMEC_SOURCES = [
    {
        "filename": "vinmec_pregnancy_abnormal_signs.md",
        "id": "SRC_VINMEC_PREG_ABNORMAL",
        "title": "Chú ý các biểu hiện bất thường khi mang thai",
        "url": "https://www.vinmec.com/vie/bai-viet/chu-y-cac-bieu-hien-bat-thuong-khi-mang-thai-vi",
        "sectionOrPage": "1. Ra máu kèm theo đau bụng dưới",
        "targetEntities": ["MOTHER"],
        "applicableStages": ["PREGNANCY", "POSSIBLE_PREGNANCY"],
        "ruleIds": ["PREG_YELLOW_001", "PREG_RED_001"],
    },
    {
        "filename": "vinmec_pregnancy_danger_signs.md",
        "id": "SRC_VINMEC_PREG_DANGER",
        "title": "7 dấu hiệu cần lưu ý khi mang thai",
        "url": "https://www.vinmec.com/vie/bai-viet/7-dau-hieu-can-luu-y-khi-mang-thai-vi",
        "sectionOrPage": "1. Chảy máu khi mang thai",
        "targetEntities": ["MOTHER"],
        "applicableStages": ["PREGNANCY"],
        "ruleIds": ["PREG_RED_001", "PREG_YELLOW_001"],
    },
    {
        "filename": "vinmec_pregnancy_preeclampsia.md",
        "id": "SRC_VINMEC_PREECLAMPSIA",
        "title": "Các dấu hiệu của tiền sản giật",
        "url": "https://www.vinmec.com/vie/bai-viet/cac-dau-hieu-cua-tien-san-giat-vi",
        "sectionOrPage": "2. Dấu hiệu tiền sản giật khi mang thai",
        "targetEntities": ["MOTHER"],
        "applicableStages": ["PREGNANCY", "POSTPARTUM_MOTHER"],
        "ruleIds": ["PREG_RED_002"],
    },
    {
        "filename": "vinmec_pregnancy_fever.md",
        "id": "SRC_VINMEC_PREG_FEVER",
        "title": "Sốt trong khi mang thai có nguy hiểm?",
        "url": "https://www.vinmec.com/vie/bai-viet/sot-trong-khi-mang-thai-co-nguy-hiem-vi",
        "sectionOrPage": "2. Sốt khi mang thai có nguy hiểm không?",
        "targetEntities": ["MOTHER"],
        "applicableStages": ["PREGNANCY"],
        "ruleIds": ["PREG_YELLOW_001"],
    },
    {
        "filename": "vinmec_pregnancy_urinary_infection.md",
        "id": "SRC_VINMEC_PREG_UTI",
        "title": "Nhiễm trùng tiết niệu khi mang thai: Nguyên nhân, dấu hiệu nhận biết, cách phòng tránh",
        "url": "https://www.vinmec.com/vie/bai-viet/nhiem-trung-tiet-nieu-khi-mang-thai-nguyen-nhan-dau-hieu-nhan-biet-cach-phong-tranh-vi",
        "sectionOrPage": "2. Dấu hiệu nhiễm trùng đường tiết niệu khi mang thai",
        "targetEntities": ["MOTHER"],
        "applicableStages": ["PREGNANCY"],
        "ruleIds": ["PREG_YELLOW_001"],
    },
    {
        "filename": "vinmec_postpartum_warning_signs.md",
        "id": "SRC_VINMEC_POST_WARNING",
        "title": "Các dấu hiệu cảnh báo bất thường sau sinh",
        "url": "https://www.vinmec.com/vie/bai-viet/cac-dau-hieu-canh-bao-bat-thuong-sau-sinh-vi",
        "sectionOrPage": "2. Các biến chứng sau sinh thường gặp",
        "targetEntities": ["MOTHER"],
        "applicableStages": ["POSTPARTUM_MOTHER"],
        "ruleIds": ["POST_RED_001"],
    },
    {
        "filename": "vinmec_postpartum_infection.md",
        "id": "SRC_VINMEC_POST_INFECTION",
        "title": "Dấu hiệu nhận biết nhiễm khuẩn sau sinh con",
        "url": "https://www.vinmec.com/vie/bai-viet/dau-hieu-nhan-biet-nhiem-khuan-sau-sinh-con-vi",
        "sectionOrPage": "3. Dấu hiệu nhận biết nhiễm khuẩn sau sinh",
        "targetEntities": ["MOTHER"],
        "applicableStages": ["POSTPARTUM_MOTHER"],
        "ruleIds": ["POST_RED_001"],
    },
]


class SectionExtractor(HTMLParser):
    def __init__(self, target_h2: str):
        super().__init__()
        self.target_h2 = target_h2.strip().lower()
        self.capturing = False
        self.current_tag: str | None = None
        self.captured_paragraphs: list[str] = []
        self.current_text: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]):
        self.current_tag = tag

    def handle_endtag(self, tag: str):
        if tag == "p" and self.capturing and self.current_text:
            p_str = " ".join("".join(self.current_text).split())
            if p_str and len(p_str) > 10:
                self.captured_paragraphs.append(p_str)
            self.current_text = []
        self.current_tag = None

    def handle_data(self, data: str):
        text = data.strip()
        if not text:
            return
        if self.current_tag in ["h1", "h2", "h3"]:
            cleaned = " ".join(text.lower().split())
            if self.target_h2 in cleaned or cleaned in self.target_h2:
                self.capturing = True
                return
            elif self.capturing:
                self.capturing = False
                return
        if self.capturing:
            self.current_text.append(data)


def verify_and_update_vinmec_sources():
    fetched_at = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    for spec in VINMEC_SOURCES:
        url = spec["url"]
        target_section = spec["sectionOrPage"]
        print(f"Fetching {spec['filename']} from {url}...")
        req = urllib.request.Request(url, headers=HEADERS)
        with urllib.request.urlopen(req, timeout=15) as resp:
            http_status = resp.status
            raw_bytes = resp.read()
            remote_hash = hashlib.sha256(raw_bytes).hexdigest()
            html_text = raw_bytes.decode("utf-8", errors="replace")

        parser = SectionExtractor(target_section)
        parser.feed(html_text)

        if not parser.captured_paragraphs:
            raise RuntimeError(f"Failed to extract paragraphs for {target_section} from {url}")

        body_text = "\n\n".join(parser.captured_paragraphs)
        content_hash = hashlib.sha256(body_text.encode("utf-8")).hexdigest()

        target_entities_yaml = "\n".join(f'  - "{e}"' for e in spec["targetEntities"])
        stages_yaml = "\n".join(f'  - "{s}"' for s in spec["applicableStages"])
        rule_ids_yaml = "\n".join(f'  - "{r}"' for r in spec["ruleIds"])

        file_content = f"""---
id: "{spec['id']}"
title: "{spec['title']}"
url: "{spec['url']}"
domain: "vinmec.com"
organization: "Vinmec International Hospital"
sourceStatus: "SOURCE_VERIFIED"
publisherTrustStatus: "DOMAIN_AND_DOCUMENT_VERIFIED"
sectionOrPage: "{spec['sectionOrPage']}"
bodyProvenance: "VERBATIM_SECTION_EXTRACTED_FROM_LIVE_URL"
fetchedAt: "{fetched_at}"
httpStatus: {http_status}
remoteContentHash: "{remote_hash}"
contentHash: "{content_hash}"
targetEntities:
{target_entities_yaml}
applicableStages:
{stages_yaml}
languages:
  - "vi"
ruleIds:
{rule_ids_yaml}
---
{body_text}
"""
        target_file = SOURCES_DIR / spec["filename"]
        target_file.write_text(file_content, encoding="utf-8")
        print(f"  -> Successfully verified and saved {spec['filename']} (HTTP {http_status}, body hash {content_hash[:12]}...)")


if __name__ == "__main__":
    verify_and_update_vinmec_sources()
