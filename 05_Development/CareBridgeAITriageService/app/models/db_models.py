"""SQLAlchemy database models with pgvector support."""

from __future__ import annotations

from datetime import datetime
from pgvector.sqlalchemy import Vector
from sqlalchemy import DateTime, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class MaternalKnowledgeChunk(Base):
    """Stores chunked medical knowledge and maternal care documents with 768-dim vector embeddings."""

    __tablename__ = "maternal_knowledge_chunks"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    title: Mapped[str] = mapped_column(String(255), nullable=False, index=True)
    stage: Mapped[str] = mapped_column(String(50), nullable=False, default="ALL", index=True)
    topic: Mapped[str] = mapped_column(String(100), nullable=False, default="GENERAL", index=True)
    source: Mapped[str] = mapped_column(String(255), nullable=False)
    section: Mapped[str] = mapped_column(String(255), nullable=True)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    chunk_index: Mapped[int] = mapped_column(Integer, default=0)
    embedding = mapped_column(Vector(768), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    def __repr__(self) -> str:
        return f"<MaternalKnowledgeChunk(id={self.id}, title='{self.title}', stage='{self.stage}', topic='{self.topic}')>"
