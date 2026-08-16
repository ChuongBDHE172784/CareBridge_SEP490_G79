"""Document ingestion and knowledge base API router."""

from __future__ import annotations

import shutil
from pathlib import Path
from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import RAW_DOCS_DIR
from app.core.database import get_db
from app.core.security import verify_internal_api_key
from app.models.schemas import (
    BatchIngestResponse,
    IngestDocumentRequest,
    IngestDocumentResponse,
    MaternalStage,
)
from app.services.ingestion_service import get_ingestion_service

router = APIRouter(prefix="/documents", tags=["Knowledge Base Ingestion"])


@router.post(
    "/upload",
    response_model=IngestDocumentResponse,
    summary="Upload file tài liệu (PDF, Word, Markdown, TXT) để tự động Chunking & Lưu pgvector",
)
async def upload_document_file(
    file: UploadFile = File(...),
    stage: MaternalStage = Form(default=MaternalStage.PREGNANCY),
    topic: str = Form(default="GENERAL"),
    source: str = Form(default="Bộ Y Tế / Tài liệu Tải lên"),
    _auth: str = Depends(verify_internal_api_key),
    db: AsyncSession = Depends(get_db),
) -> IngestDocumentResponse:
    """Upload a medical document file (PDF, DOCX, MD), chunk, vectorize, and insert into database."""
    service = get_ingestion_service()
    RAW_DOCS_DIR.mkdir(exist_ok=True, parents=True)
    temp_path = RAW_DOCS_DIR / file.filename

    try:
        with temp_path.open("wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        chunks_count = await service.ingest_file(temp_path, session=db)
        return IngestDocumentResponse(
            success=True,
            message=f"Đã xử lý và lưu thành công {chunks_count} đoạn tri thức từ file {file.filename}.",
            total_chunks=chunks_count,
            document_title=file.filename,
            stage=stage.value,
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Lỗi khi xử lý nạp file: {str(e)}",
        )


@router.post(
    "/ingest-text",
    response_model=IngestDocumentResponse,
    summary="Nạp trực tiếp một đoạn văn bản cẩm nang vào Vector DB",
)
async def ingest_raw_text(
    request: IngestDocumentRequest,
    _auth: str = Depends(verify_internal_api_key),
    db: AsyncSession = Depends(get_db),
) -> IngestDocumentResponse:
    """Directly chunk and vectorize raw text content into pgvector."""
    service = get_ingestion_service()
    return await service.ingest_single_document(request, session=db)


@router.post(
    "/sync-directory",
    response_model=BatchIngestResponse,
    summary="Quét toàn bộ thư mục data/raw_documents và nạp tất cả tài liệu vào Vector DB",
)
async def sync_raw_documents_directory(
    _auth: str = Depends(verify_internal_api_key),
    db: AsyncSession = Depends(get_db),
) -> BatchIngestResponse:
    """Scan data/raw_documents and ingest all available files into pgvector."""
    service = get_ingestion_service()
    return await service.ingest_directory(RAW_DOCS_DIR, session=db)
