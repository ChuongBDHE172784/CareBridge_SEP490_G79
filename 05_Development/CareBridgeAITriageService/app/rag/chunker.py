"""Document loader and text chunking pipeline for maternal health materials."""

from __future__ import annotations

import logging
import re
from pathlib import Path
from typing import List, Optional, Tuple
import frontmatter
from langchain_text_splitters import RecursiveCharacterTextSplitter
from pypdf import PdfReader
import docx

from app.models.schemas import DocumentChunkDTO

logger = logging.getLogger(__name__)

# Default optimal chunking parameters
DEFAULT_CHUNK_SIZE = 900
DEFAULT_CHUNK_OVERLAP = 180


class DocumentChunker:
    def __init__(
        self,
        chunk_size: int = DEFAULT_CHUNK_SIZE,
        chunk_overlap: int = DEFAULT_CHUNK_OVERLAP,
    ) -> None:
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            separators=["\n## ", "\n### ", "\n#### ", "\n\n", "\n", ". ", " "],
            keep_separator=True,
        )

    def chunk_file(self, file_path: Path | str) -> List[DocumentChunkDTO]:
        """Read and chunk a document file (Markdown, PDF, DOCX, TXT)."""
        path = Path(file_path)
        if not path.exists():
            raise FileNotFoundError(f"File not found: {path}")

        suffix = path.suffix.lower()
        if suffix in (".md", ".markdown"):
            return self._chunk_markdown(path)
        elif suffix == ".pdf":
            return self._chunk_pdf(path)
        elif suffix in (".docx", ".doc"):
            return self._chunk_docx(path)
        elif suffix in (".txt", ".text"):
            return self._chunk_text(path)
        else:
            logger.warning(f"Unsupported file extension {suffix}, parsing as plain text.")
            return self._chunk_text(path)

    def chunk_raw_text(
        self,
        text: str,
        title: str,
        stage: str = "ALL",
        topic: str = "GENERAL",
        source: str = "Bộ Y Tế / Cẩm nang Y khoa",
        section: Optional[str] = None,
    ) -> List[DocumentChunkDTO]:
        """Chunk raw text string into DocumentChunkDTOs with smart section extraction."""
        cleaned = self._clean_text(text)
        splits = self.splitter.split_text(cleaned)
        chunks: List[DocumentChunkDTO] = []
        current_section = section

        for idx, chunk_text in enumerate(splits):
            if not chunk_text.strip():
                continue

            # Smart Section Extractor: Detect markdown/text heading in the chunk
            detected_heading = self._extract_section_heading(chunk_text)
            if detected_heading:
                current_section = detected_heading
            
            chunk_section = current_section or section or f"{title} (Mục {idx + 1})"

            chunks.append(
                DocumentChunkDTO(
                    title=title,
                    stage=stage,
                    topic=topic,
                    source=source,
                    section=chunk_section,
                    content=chunk_text.strip(),
                    chunk_index=idx,
                )
            )
        return chunks

    def _extract_section_heading(self, chunk_text: str) -> Optional[str]:
        """Auto-detect Markdown headers (##, ###, #) or chapter/page markers in text."""
        lines = [line.strip() for line in chunk_text.splitlines() if line.strip()]
        for line in lines[:3]:  # Check first 3 lines
            # Markdown header (#, ##, ###, ####)
            m_header = re.match(r"^#{1,4}\s+(.+)$", line)
            if m_header:
                clean_h = m_header.group(1).strip("*_# ")
                if len(clean_h) >= 3:
                    return clean_h[:120]
            
            # Numbered headings (1. Tiêu đề, I. Tiêu đề, Chương 1: ...)
            m_sec = re.match(r"^(Chương\s+[IVXLCDM0-9]+|Phần\s+[IVXLCDM0-9]+|Mục\s+[0-9]+|Điều\s+[0-9]+|[0-9]+\.[0-9]*)\s*[:\.\-]?\s+(.+)$", line, re.IGNORECASE)
            if m_sec:
                clean_s = f"{m_sec.group(1)}: {m_sec.group(2)}".strip("*_ ")
                return clean_s[:120]

            # Page marker ([Trang X])
            m_page = re.match(r"^\[Trang\s+([0-9]+)\]", line, re.IGNORECASE)
            if m_page:
                return f"Trang {m_page.group(1)}"

        return None

    @staticmethod
    def infer_stage_and_topic(title: str, text_sample: str) -> Tuple[str, str]:
        """Infer stage and topic from document title and text content when metadata is missing."""
        combined = f"{title} {text_sample[:2000]}".lower()

        # Infer stage
        if any(w in combined for w in ["kế hoạch hóa gia đình", "phá thai", "tránh thai", "biện pháp tránh thai", "triệt sản"]):
            stage = "ALL"
            topic = "FAMILY_PLANNING"
        elif any(w in combined for w in ["chuẩn bị mang thai", "tiền hôn nhân", "trước khi mang thai", "thụ thai"]):
            stage = "PRECONCEPTION"
            topic = "PRECONCEPTION_CARE"
        elif any(w in combined for w in ["sau sinh", "ở cữ", "sản dịch", "hậu sản", "trầm cảm sau sinh", "sơ sinh", "sữa mẹ", "kích sữa", "nuôi con bằng sữa mẹ"]):
            stage = "POSTPARTUM"
            topic = "POSTPARTUM_CARE"
        elif any(w in combined for w in ["mang thai", "thai kỳ", "bà bầu", "tuần thai", "tam cá nguyệt", "thai nhi", "mẹ bầu", "khám thai"]):
            stage = "PREGNANCY"
            if any(w in combined for w in ["nguy hiểm", "cấp cứu", "tiền sản giật", "sản giật", "ra máu", "vỡ ối", "sốt cao"]):
                topic = "DANGER_SIGNS"
            elif any(w in combined for w in ["dinh dưỡng", "vi chất", "sắt", "canxi", "axit folic", "uống gì", "ăn gì"]):
                topic = "NUTRITION"
            elif any(w in combined for w in ["huyết áp", "đường huyết", "cử động thai", "thai máy", "theo dõi"]):
                topic = "HEALTH_MONITORING"
            else:
                topic = "GENERAL"
        else:
            stage = "ALL"
            topic = "GENERAL"

        return stage, topic

    def _chunk_markdown(self, path: Path) -> List[DocumentChunkDTO]:
        post = frontmatter.load(path)
        metadata = post.metadata or {}
        body = post.content

        title = metadata.get("title") or path.stem.replace("_", " ").title()
        
        # If metadata not provided, infer from content
        if not metadata.get("stage") and not metadata.get("topic"):
            inferred_stage, inferred_topic = self.infer_stage_and_topic(str(title), body)
            stage = inferred_stage
            topic = inferred_topic
        else:
            stage = metadata.get("stage") or metadata.get("applicableStages", "ALL")
            if isinstance(stage, list):
                stage = stage[0] if stage else "ALL"
            topic = metadata.get("topic") or "GENERAL"

        source = metadata.get("source") or metadata.get("organization") or metadata.get("publisher") or "Bộ Y Tế / Tài liệu Y tế"
        section = metadata.get("section") or metadata.get("sectionOrPage") or None

        return self.chunk_raw_text(
            text=body,
            title=str(title),
            stage=str(stage),
            topic=str(topic),
            source=str(source),
            section=str(section) if section else None,
        )

    def _chunk_pdf(self, path: Path) -> List[DocumentChunkDTO]:
        reader = PdfReader(str(path))
        title = path.stem.replace("_", " ").title()
        full_text = []

        for page_idx, page in enumerate(reader.pages):
            page_text = page.extract_text() or ""
            if page_text.strip():
                full_text.append(f"[Trang {page_idx + 1}]\n{page_text}")

        combined = "\n\n".join(full_text)
        stage, topic = self.infer_stage_and_topic(title, combined)

        return self.chunk_raw_text(
            text=combined,
            title=title,
            stage=stage,
            topic=topic,
            source=f"Tài liệu PDF: {path.name}",
        )

    def _chunk_docx(self, path: Path) -> List[DocumentChunkDTO]:
        doc = docx.Document(str(path))
        title = path.stem.replace("_", " ").title()
        text = "\n\n".join([p.text for p in doc.paragraphs if p.text.strip()])
        stage, topic = self.infer_stage_and_topic(title, text)

        return self.chunk_raw_text(
            text=text,
            title=title,
            stage=stage,
            topic=topic,
            source=f"Tài liệu Word: {path.name}",
        )

    def _chunk_text(self, path: Path) -> List[DocumentChunkDTO]:
        text = path.read_text(encoding="utf-8", errors="ignore")
        title = path.stem.replace("_", " ").title()
        stage, topic = self.infer_stage_and_topic(title, text)

        return self.chunk_raw_text(
            text=text,
            title=title,
            stage=stage,
            topic=topic,
            source=f"Tài liệu: {path.name}",
        )

    @staticmethod
    def _clean_text(text: str) -> str:
        """Remove excessive newlines, duplicate spaces, and weird encoding artifacts."""
        cleaned = re.sub(r"\r\n|\r", "\n", text)
        cleaned = re.sub(r"\n{3,}", "\n\n", cleaned)
        cleaned = re.sub(r"[ \t]{2,}", " ", cleaned)
        return cleaned.strip()


chunker = DocumentChunker()


def get_chunker() -> DocumentChunker:
    return chunker
