"""CareBridge AI Maternal RAG & Health Metrics Screening Service."""

from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.health import router as health_router
from app.api.v1.chat import router as chat_router
from app.api.v1.documents import router as documents_router
from app.api.v1.metrics import router as metrics_router
from app.config import SERVER_SETTINGS
from app.services.ingestion_service import get_ingestion_service

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup and shutdown lifecycle events."""
    logger.info("Initializing CareBridge AI Maternal RAG Service...")
    # Preload local knowledge documents into in-memory vector cache on startup
    try:
        service = get_ingestion_service()
        result = await service.ingest_directory()
        logger.info(
            f"Startup knowledge preload completed: {result.total_chunks_created} chunks from {result.total_files_processed} files."
        )
    except Exception as e:
        logger.warning(f"Startup knowledge preload notice: {e}")
    yield
    logger.info("Shutting down CareBridge AI Maternal RAG Service.")


app = FastAPI(
    title="CareBridge AI Maternal Health RAG & Triage Service",
    description="Hệ thống AI RAG & Sàng lọc Chỉ số Sức khỏe Mẹ bầu sử dụng Gemini 3.7 Flash và pgvector",
    version="2.0.0",
    lifespan=lifespan,
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register API Routers
app.include_router(health_router)
app.include_router(metrics_router, prefix="/api/v1")
app.include_router(chat_router, prefix="/api/v1")
app.include_router(documents_router, prefix="/api/v1")
