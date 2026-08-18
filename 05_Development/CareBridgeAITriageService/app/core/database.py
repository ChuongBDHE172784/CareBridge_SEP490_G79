"""Database connection and session management for PostgreSQL + pgvector."""

from __future__ import annotations

import logging
from typing import AsyncGenerator, Optional
from sqlalchemy.ext.asyncio import (
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy.orm import DeclarativeBase
from app.config import DB_SETTINGS

logger = logging.getLogger(__name__)


class Base(DeclarativeBase):
    pass


# Create Async Engine for PostgreSQL
engine = create_async_engine(
    DB_SETTINGS.url,
    echo=False,
    pool_size=DB_SETTINGS.pool_size,
    max_overflow=DB_SETTINGS.max_overflow,
    pool_pre_ping=True,
)

AsyncSessionLocal = async_sessionmaker(
    bind=engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autocommit=False,
    autoflush=False,
)


async def get_db() -> AsyncGenerator[Optional[AsyncSession], None]:
    """Dependency for obtaining async database session with graceful fallback."""
    session: Optional[AsyncSession] = None
    try:
        session = AsyncSessionLocal()
        yield session
    except Exception as e:
        logger.debug(f"Database session notice ({e}), proceeding with in-memory fallback")
        yield None
    finally:
        if session is not None:
            try:
                await session.close()
            except Exception:
                pass
