"""Document loader and text chunking pipeline for maternal health materials."""

from __future__ import annotations

import logging
import re
from pathlib import Path
from typing import List
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
        section: str | None = None,
    ) -> List[DocumentChunkDTO]:
        """Chunk raw text string into DocumentChunkDTOs."""
        cleaned = self._clean_text(text)
        splits = self.splitter.split_text(cleaned)
        chunks: List[DocumentChunkDTO] = []
        for idx, chunk_text in enumerate(splits):
            if not chunk_text.strip():
                continue
            chunks.append(
                DocumentChunkDTO(
                    title=title,
                    stage=stage,
                    topic=topic,
                    source=source,
                    section=section,
                    content=chunk_text.strip(),
                    chunk_index=idx,
                )
            )
        return chunks

    def _chunk_markdown(self, path: Path) -> List[DocumentChunkDTO]:
        post = frontmatter.load(path)
        metadata = post.metadata or {}
        body = post.content

        title = metadata.get("title") or path.stem.replace("_", " ").title()
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
        chunks: List[DocumentChunkDTO] = []
        full_text = []

        for page_idx, page in enumerate(reader.pages):
            page_text = page.extract_text() or ""
            if page_text.strip():
                full_text.append(f"[Trang {page_idx + 1}]\n{page_text}")

        combined = "\n\n".join(full_text)
        return self.chunk_raw_text(
            text=combined,
            title=title,
            stage="PREGNANCY",
            topic="GENERAL",
            source=f"Tài liệu PDF: {path.name}",
        )

    def _chunk_docx(self, path: Path) -> List[DocumentChunkDTO]:
        doc = docx.Document(str(path))
        title = path.stem.replace("_", " ").title()
        text = "\n\n".join([p.text for p in doc.paragraphs if p.text.strip()])
        return self.chunk_raw_text(
            text=text,
            title=title,
            stage="PREGNANCY",
            topic="GENERAL",
            source=f"Tài liệu Word: {path.name}",
        )

    def _chunk_text(self, path: Path) -> List[DocumentChunkDTO]:
        text = path.read_text(encoding="utf-8", errors="ignore")
        title = path.stem.replace("_", " ").title()
        return self.chunk_raw_text(
            text=text,
            title=title,
            stage="ALL",
            topic="GENERAL",
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
