"""Database initialization script: creates pgvector extension and knowledge tables."""

from __future__ import annotations

import asyncio
import logging
import sys
from pathlib import Path

# Add project root to sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from sqlalchemy import text
from app.config import DB_SETTINGS
from app.core.database import engine, Base
from app.models.db_models import MaternalKnowledgeChunk

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)


async def init_db():
    logger.info(f"Connecting to PostgreSQL ({DB_SETTINGS.url}) to initialize pgvector...")
    try:
        async with engine.begin() as conn:
            # 1. Enable pgvector extension
            logger.info("Enabling pgvector extension...")
            await conn.execute(text("CREATE EXTENSION IF NOT EXISTS vector;"))

            # 2. Create tables
            logger.info("Creating maternal_knowledge_chunks table...")
            await conn.run_sync(Base.metadata.create_all)

            # 3. Create HNSW index for fast Cosine similarity search (<=>)
            logger.info("Creating HNSW vector index on embedding column...")
            await conn.execute(
                text(
                    """
                    CREATE INDEX IF NOT EXISTS idx_maternal_chunks_embedding_hnsw 
                    ON maternal_knowledge_chunks 
                    USING hnsw (embedding vector_cosine_ops);
                    """
                )
            )

        logger.info("✅ Database and pgvector initialized successfully!")
    except Exception as e:
        logger.error(f"❌ Could not connect to PostgreSQL: {e}")
        print("\n" + "=" * 70)
        print("LƯU Ý: Không thể kết nối tới PostgreSQL (Port 5433/5432).")
        print("Nếu bạn muốn lưu vector vào Database vật lý:")
        print("1. Hãy bật Docker PostgreSQL: cd 05_Development/CareBridgeAPI && docker compose up -d")
        print("2. Hoặc cấu hình DATABASE_URL trong .env trỏ đúng CSDL PostgreSQL của bạn.")
        print("(Nếu chưa bật PostgreSQL, Service AI vẫn tự động chạy bằng In-Memory Cache).")
        print("=" * 70 + "\n")


if __name__ == "__main__":
    asyncio.run(init_db())
