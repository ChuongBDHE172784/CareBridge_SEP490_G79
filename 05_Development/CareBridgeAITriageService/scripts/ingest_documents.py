"""CLI script to chunk and ingest documents into pgvector database."""

from __future__ import annotations

import argparse
import asyncio
import logging
from pathlib import Path
import sys

# Add project root to sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.config import RAW_DOCS_DIR
from app.services.ingestion_service import get_ingestion_service

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)


async def main():
    parser = argparse.ArgumentParser(description="Ingest maternal medical documents into pgvector database.")
    parser.add_argument(
        "--dir",
        type=str,
        default=str(RAW_DOCS_DIR),
        help="Directory containing PDF, DOCX, Markdown, or TXT documents to ingest",
    )
    args = parser.parse_args()

    target_dir = Path(args.dir)
    logger.info(f"Starting ingestion from directory: {target_dir}")

    service = get_ingestion_service()
    result = await service.ingest_directory(target_dir)

    print("\n" + "=" * 60)
    print("           KẾT QUẢ NẠP TÀI LIỆU VÀO VECTOR DB")
    print("=" * 60)
    print(f"Trạng thái: {'Thành công' if result.success else 'Có lỗi'}")
    print(f"Tổng số file đã xử lý: {result.total_files_processed}")
    print(f"Tổng số chunks đã tạo & lưu Vector: {result.total_chunks_created}")
    print(f"Danh sách file: {', '.join(result.processed_files)}")
    if result.errors:
        print(f"Lỗi: {result.errors}")
    print("=" * 60 + "\n")


if __name__ == "__main__":
    asyncio.run(main())
