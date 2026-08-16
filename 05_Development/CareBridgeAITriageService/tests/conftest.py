"""Pytest configuration and shared fixtures for CareBridge AI RAG."""

import sys
from pathlib import Path
import pytest

# Ensure app package is in sys.path
PROJECT_ROOT = Path(__file__).resolve().parent.parent
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


@pytest.fixture(autouse=True)
def setup_test_environment(monkeypatch):
    """Set test environment variables."""
    monkeypatch.setenv("GEMINI_ENABLED", "true")
    monkeypatch.setenv("AI_TRIAGE_INTERNAL_API_KEY", "carebridge")
