"""Document Ingestion & Knowledge Base Management Service."""

from __future__ import annotations

import logging
from pathlib import Path
from typing import List, Optional
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import RAW_DOCS_DIR
from app.models.schemas import BatchIngestResponse, IngestDocumentRequest, IngestDocumentResponse
from app.rag.chunker import get_chunker
from app.rag.vector_store import get_vector_store

logger = logging.getLogger(__name__)


class IngestionService:
    def __init__(self) -> None:
        self.chunker = get_chunker()
        self.vector_store = get_vector_store()

    async def ingest_single_document(
        self,
        request: IngestDocumentRequest,
        session: Optional[AsyncSession] = None,
    ) -> IngestDocumentResponse:
        """Chunk raw text and insert into pgvector knowledge base."""
        chunks = self.chunker.chunk_raw_text(
            text=request.text_content,
            title=request.title,
            stage=request.stage.value if request.stage else "ALL",
            topic=request.topic,
            source=request.source,
            section=request.section,
        )

        inserted_count = await self.vector_store.add_chunks(chunks, session=session)

        return IngestDocumentResponse(
            success=True,
            message=f"Đã nạp và lưu thành công {inserted_count} đoạn tri thức vào Vector Database.",
            total_chunks=inserted_count,
            document_title=request.title,
            stage=request.stage.value if request.stage else "ALL",
        )

    async def ingest_file(
        self,
        file_path: Path | str,
        session: Optional[AsyncSession] = None,
    ) -> int:
        """Process a single document file from filesystem into pgvector."""
        path = Path(file_path)
        logger.info(f"Processing file for vector ingestion: {path.name}")
        chunks = self.chunker.chunk_file(path)
        if not chunks:
            logger.warning(f"No text extracted from {path.name}")
            return 0

        inserted = await self.vector_store.add_chunks(chunks, session=session)
        logger.info(f"Successfully ingested {inserted} chunks for {path.name}")
        return inserted

    async def ingest_directory(
        self,
        dir_path: Path | str = RAW_DOCS_DIR,
        session: Optional[AsyncSession] = None,
    ) -> BatchIngestResponse:
        """Scan directory and batch ingest all supported documents (PDF, DOCX, MD, TXT)."""
        folder = Path(dir_path)
        if not folder.exists():
            return BatchIngestResponse(
                success=False,
                total_files_processed=0,
                total_chunks_created=0,
                processed_files=[],
                errors=[f"Thư mục không tồn tại: {folder}"],
            )

        supported_extensions = {".md", ".markdown", ".pdf", ".docx", ".txt"}
        files = [f for f in sorted(folder.iterdir()) if f.suffix.lower() in supported_extensions]

        total_chunks = 0
        processed_files: List[str] = []
        errors: List[str] = []

        for f in files:
            try:
                count = await self.ingest_file(f, session=session)
                total_chunks += count
                processed_files.append(f.name)
            except Exception as e:
                logger.error(f"Error ingesting file {f.name}: {e}")
                errors.append(f"{f.name}: {str(e)}")

        return BatchIngestResponse(
            success=len(errors) == 0,
            total_files_processed=len(processed_files),
            total_chunks_created=total_chunks,
            processed_files=processed_files,
            errors=errors,
        )


ingestion_service = IngestionService()


def get_ingestion_service() -> IngestionService:
    return ingestion_service
