"""Document ingestion and knowledge base API router."""

from __future__ import annotations

import shutil
from pathlib import Path
from typing import List, Optional
from fastapi import APIRouter, Depends, File, Form, HTTPException, Query, UploadFile, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import RAW_DOCS_DIR
from app.core.database import get_db
from app.core.security import verify_internal_api_key
from app.models.schemas import (
    BatchIngestResponse,
    IngestDocumentRequest,
    IngestDocumentResponse,
    KnowledgeListResponse,
    KnowledgeStatsResponse,
    MaternalStage,
    SourceCitation,
    VectorSearchTestRequest,
    VectorSearchTestResponse,
)
from app.rag.vector_store import get_vector_store
from app.services.ingestion_service import get_ingestion_service

router = APIRouter(prefix="/documents", tags=["Knowledge Base Ingestion & Management"])


@router.get(
    "/stats",
    response_model=KnowledgeStatsResponse,
    summary="[Admin] Xem thống kê tổng quan cơ sở tri thức (Tổng chunks, phân bố theo giai đoạn/chủ đề)",
)
async def get_knowledge_statistics(
    _auth: str = Depends(verify_internal_api_key),
    db: AsyncSession = Depends(get_db),
) -> KnowledgeStatsResponse:
    """Return high-level summary of the entire RAG knowledge base."""
    store = get_vector_store()
    return await store.get_stats(session=db)


@router.get(
    "/list",
    response_model=KnowledgeListResponse,
    summary="[Admin] Liệt kê danh sách các đoạn tri thức (chunks) đang lưu trong Vector DB",
)
async def list_knowledge_chunks(
    stage: Optional[MaternalStage] = Query(default=None, description="Lọc theo giai đoạn thai kỳ"),
    topic: Optional[str] = Query(default=None, description="Lọc theo chủ đề (DANGER_SIGNS, NUTRITION...)"),
    keyword: Optional[str] = Query(default=None, description="Tìm kiếm từ khóa trong nội dung/tiêu đề"),
    page: int = Query(default=1, ge=1, description="Trang số"),
    page_size: int = Query(default=20, ge=1, le=100, description="Số lượng mỗi trang"),
    _auth: str = Depends(verify_internal_api_key),
    db: AsyncSession = Depends(get_db),
) -> KnowledgeListResponse:
    """Paginate and search through all active chunks in the vector database."""
    store = get_vector_store()
    return await store.list_chunks(
        stage=stage.value if stage else None,
        topic=topic,
        keyword=keyword,
        page=page,
        page_size=page_size,
        session=db,
    )


@router.post(
    "/search-vector",
    response_model=VectorSearchTestResponse,
    summary="[Vector Simulator] Thử nghiệm tìm kiếm Vector theo khoảng cách Cosine (<=>) và xem điểm tương đồng",
)
async def simulate_vector_search(
    request: VectorSearchTestRequest,
    _auth: str = Depends(verify_internal_api_key),
    db: AsyncSession = Depends(get_db),
) -> VectorSearchTestResponse:
    """Debug and inspect raw semantic vector retrieval before feeding context into LLM."""
    store = get_vector_store()
    raw_results = await store.similarity_search(
        query=request.query,
        stage=request.stage.value if request.stage else None,
        top_k=request.top_k,
        session=db,
    )

    citations = [
        SourceCitation(
            title=r["title"],
            source=r["source"],
            section=r.get("section"),
            snippet=r["content"],
            similarity_score=round(r.get("similarity", 0.0), 4),
        )
        for r in raw_results
    ]

    return VectorSearchTestResponse(
        query=request.query,
        total_retrieved=len(citations),
        results=citations,
    )


@router.get(
    "/files",
    summary="[Admin] Xem danh sách tất cả các file tài liệu gốc trên đĩa cứng",
)
async def list_raw_files(
    _auth: str = Depends(verify_internal_api_key),
) -> dict:
    """List physical source files in data/raw_documents with size and status."""
    RAW_DOCS_DIR.mkdir(exist_ok=True, parents=True)
    files_info = []
    for f in sorted(RAW_DOCS_DIR.iterdir()):
        if f.is_file() and not f.name.startswith("."):
            stat = f.stat()
            files_info.append({
                "filename": f.name,
                "size_kb": round(stat.st_size / 1024, 2),
                "extension": f.suffix.lower(),
            })
    return {"total_files": len(files_info), "files": files_info}


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


@router.delete(
    "/by-title",
    summary="Xóa bỏ toàn bộ tri thức của một tài liệu theo Tên/Tiêu đề khỏi CSDL Vector",
)
async def delete_document_by_title(
    title: str = Query(..., description="Tên file hoặc tiêu đề cuốn sách/tài liệu cần xóa bỏ"),
    _auth: str = Depends(verify_internal_api_key),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """Delete all knowledge chunks associated with a document title from both pgvector and memory."""
    store = get_vector_store()
    deleted_count = await store.delete_by_title(title, session=db)

    # Also delete physical raw file if exists
    for f in RAW_DOCS_DIR.iterdir():
        if title.lower() in f.name.lower():
            try:
                f.unlink()
            except Exception:
                pass

    return {
        "success": True,
        "message": f"Đã xóa thành công {deleted_count} đoạn tri thức của tài liệu '{title}' khỏi hệ thống AI.",
        "deleted_chunks": deleted_count,
    }


@router.delete(
    "/clear-all",
    summary="Xóa sạch toàn bộ tri thức trong Vector Database để nạp mới từ đầu",
)
async def clear_all_knowledge(
    _auth: str = Depends(verify_internal_api_key),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """Wipe all knowledge chunks from pgvector and memory cache."""
    store = get_vector_store()
    cleared_count = await store.clear_all(session=db)
    return {
        "success": True,
        "message": f"Đã dọn dẹp sạch toàn bộ {cleared_count} đoạn tri thức trong Vector Database.",
        "cleared_chunks": cleared_count,
    }
