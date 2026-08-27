"""Tests for DocumentChunker and Ingestion Pipeline."""

import pytest
from pathlib import Path
from app.config import RAW_DOCS_DIR
from app.rag.chunker import DocumentChunker
from app.services.ingestion_service import IngestionService


def test_document_chunker_markdown_file():
    chunker = DocumentChunker(chunk_size=500, chunk_overlap=100)
    files = list(RAW_DOCS_DIR.glob("*.md"))
    assert len(files) > 0, "Expected at least 1 markdown file in raw_documents"

    sample_file = files[0]
    chunks = chunker.chunk_file(sample_file)
    assert len(chunks) > 0
    first_chunk = chunks[0]
    assert first_chunk.title is not None
    assert first_chunk.content is not None
    assert len(first_chunk.content) > 0


@pytest.mark.asyncio
async def test_batch_ingestion_directory():
    service = IngestionService()
    result = await service.ingest_directory(RAW_DOCS_DIR)
    assert result.success is True
    assert result.total_files_processed > 0
    assert result.total_chunks_created > 0
