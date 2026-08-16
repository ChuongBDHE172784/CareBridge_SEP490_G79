"""Tests for FastAPI HTTP endpoints."""

import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)
AUTH_HEADERS = {"X-Internal-API-Key": "carebridge"}


def test_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert "model" in data
    assert "embedding_model" in data


def test_metrics_evaluate_endpoint_normal():
    payload = {
        "stage": "PREGNANCY",
        "gestational_age_weeks": 24,
        "systolic_bp": 118,
        "diastolic_bp": 76,
        "temperature": 36.6,
        "blood_glucose": 4.9,
    }
    response = client.post("/api/v1/metrics/evaluate", json=payload, headers=AUTH_HEADERS)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "NORMAL"
    assert data["emergency_mode"] is False


def test_metrics_evaluate_endpoint_critical():
    payload = {
        "stage": "PREGNANCY",
        "gestational_age_weeks": 32,
        "systolic_bp": 165,
        "diastolic_bp": 110,
        "symptoms": ["Nhìn mờ", "Đau đầu"],
    }
    response = client.post("/api/v1/metrics/evaluate", json=payload, headers=AUTH_HEADERS)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "CRITICAL_EMERGENCY"
    assert data["emergency_mode"] is True


def test_chat_message_endpoint():
    payload = {
        "message": "Em mang thai tháng thứ 5 thì cần lưu ý những gì về dinh dưỡng?",
        "stage": "PREGNANCY",
        "gestational_age_weeks": 20,
    }
    response = client.post("/api/v1/chat/message", json=payload, headers=AUTH_HEADERS)
    assert response.status_code == 200
    data = response.json()
    assert "answer" in data
    assert len(data["answer"]) > 0
    assert "sources" in data
    assert "disclaimer" in data


def test_sync_directory_endpoint():
    response = client.post("/api/v1/documents/sync-directory", headers=AUTH_HEADERS)
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["total_files_processed"] > 0
