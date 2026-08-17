"""Pydantic schemas for CareBridge Maternal Health RAG & Metrics Screening."""

from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field


class TriageRiskStatus(str, Enum):
    NORMAL = "NORMAL"
    ANOMALY_MONITOR = "ANOMALY_MONITOR"
    CRITICAL_EMERGENCY = "CRITICAL_EMERGENCY"


class MaternalStage(str, Enum):
    PRECONCEPTION = "PRECONCEPTION"
    PREGNANCY = "PREGNANCY"
    POSTPARTUM = "POSTPARTUM"
    ALL = "ALL"


class SourceCitation(BaseModel):
    title: str = Field(description="Tiêu đề tài liệu nguồn")
    source: str = Field(description="Tác giả / Nhà xuất bản / Cơ quan y tế (Bộ Y Tế, WHO, Vinmec...)")
    section: Optional[str] = Field(default=None, description="Chương / Mục / Trang")
    snippet: Optional[str] = Field(default=None, description="Trích đoạn nội dung tài liệu đối chiếu")
    similarity_score: Optional[float] = Field(default=None, description="Độ tương đồng ngữ nghĩa")


# ============================================================================
# 1. Health Metrics Screening (Bước 7 ➔ 8 ➔ 9 trong Workflow)
# ============================================================================

class HealthMetricsLogRequest(BaseModel):
    user_id: Optional[str] = Field(default=None, description="ID của mẹ bầu")
    stage: MaternalStage = Field(default=MaternalStage.PREGNANCY, description="Giai đoạn: PRECONCEPTION, PREGNANCY, POSTPARTUM")
    gestational_age_weeks: Optional[int] = Field(default=None, ge=1, le=44, description="Tuần thai (1 - 42 tuần)")
    
    # Vital signs
    systolic_bp: Optional[int] = Field(default=None, ge=50, le=250, description="Huyết áp tâm thu (mmHg)")
    diastolic_bp: Optional[int] = Field(default=None, ge=30, le=160, description="Huyết áp tâm trương (mmHg)")
    blood_glucose: Optional[float] = Field(default=None, ge=1.0, le=600.0, description="Chỉ số đường huyết (mmol/L hoặc mg/dL)")
    is_fasting_glucose: Optional[bool] = Field(default=True, description="Đo lúc đói hay sau ăn")
    temperature: Optional[float] = Field(default=None, ge=34.0, le=43.0, description="Thân nhiệt (°C)")
    heart_rate: Optional[int] = Field(default=None, ge=30, le=220, description="Nhịp tim (lần/phút)")
    weight_kg: Optional[float] = Field(default=None, ge=20.0, le=300.0, description="Cân nặng hiện tại (kg)")
    height_cm: Optional[float] = Field(default=None, ge=50.0, le=250.0, description="Chiều cao (cm)")
    bmi: Optional[float] = Field(default=None, ge=10.0, le=250.0, description="Chỉ số khối cơ thể BMI (kg/m²)")
    
    # Fetal movement (Kicks) - Quan trọng từ tuần 28
    fetal_movements_count: Optional[int] = Field(default=None, ge=0, le=100, description="Số lần thai cử động")
    fetal_movements_duration_hours: Optional[int] = Field(default=2, ge=1, le=12, description="Khoảng thời gian đếm (mặc định 2 giờ)")

    # Lifestyle & Mental Health (Lối sống & Sức khỏe tâm thần)
    water_intake_ml: Optional[int] = Field(default=None, ge=0, le=10000, description="Lượng nước uống trong ngày (ml)")
    epds_score: Optional[int] = Field(default=None, ge=0, le=30, description="Điểm sàng lọc trầm cảm/tâm trạng EPDS (0-30)")
    spo2: Optional[int] = Field(default=None, ge=50, le=100, description="Độ bão hòa oxy trong máu SpO2 (%)")
    sleep_hours: Optional[float] = Field(default=None, ge=0.0, le=24.0, description="Thời lượng giấc ngủ (giờ)")
    
    # Symptoms reported
    symptoms: List[str] = Field(default_factory=list, description="Danh sách triệu chứng chọn (nhức đầu, phù chân, đau bụng...)")
    free_text_notes: Optional[str] = Field(default=None, description="Ghi chú mô tả thêm của mẹ bầu")


class HealthMetricsEvaluationResponse(BaseModel):
    status: TriageRiskStatus = Field(description="Mức độ rủi ro: NORMAL, ANOMALY_MONITOR, CRITICAL_EMERGENCY")
    emergency_mode: bool = Field(description="True nếu cần kích hoạt Emergency Mode (Gọi 115, SOS, Bệnh viện)")
    headline: str = Field(description="Tiêu đề thông báo nhanh cho mẹ bầu")
    summary: str = Field(description="Đánh giá chi tiết và phân tích các chỉ số sức khỏe")
    risk_factors: List[str] = Field(default_factory=list, description="Các yếu tố nguy cơ phát hiện được")
    suggested_action: str = Field(description="Hành động khuyến nghị (Kích hoạt cấp cứu / Chat AI Nurse / Tiếp tục theo dõi)")
    relevant_sources: List[SourceCitation] = Field(default_factory=list, description="Tài liệu cẩm nang y tế đối soát")
    evaluated_at: datetime = Field(default_factory=datetime.utcnow)
    disclaimer: str = Field(description="Cảnh báo y tế pháp lý")


# ============================================================================
# 2. AI Nurse Assistant RAG Chat (Bước 10 trong Workflow)
# ============================================================================

class ChatMessage(BaseModel):
    role: str = Field(description="user hoặc assistant")
    content: str = Field(description="Nội dung tin nhắn")


class RagChatRequest(BaseModel):
    message: str = Field(description="Câu hỏi hoặc chia sẻ của mẹ bầu")
    stage: MaternalStage = Field(default=MaternalStage.PREGNANCY, description="Giai đoạn của người dùng")
    gestational_age_weeks: Optional[int] = Field(default=None, description="Tuần thai hiện tại")
    conversation_history: List[ChatMessage] = Field(default_factory=list, description="Lịch sử đoạn hội thoại trước đó")
    recent_metrics: Optional[HealthMetricsLogRequest] = Field(default=None, description="Chỉ số sinh hiệu gần nhất để AI nắm bối cảnh")


class RagChatResponse(BaseModel):
    answer: str = Field(description="Nội dung giải đáp chi tiết, ân cần từ AI Nurse Assistant")
    has_critical_warning: bool = Field(default=False, description="True nếu câu hỏi của mẹ chứa dấu hiệu nguy hiểm cần đi viện")
    need_expert_consultation: bool = Field(default=False, description="True nếu phát hiện dấu hiệu hoặc chỉ số sức khỏe bất thường cần tham vấn bác sĩ chuyên khoa")
    suggested_followups: List[str] = Field(default_factory=list, description="Các câu hỏi gợi ý tiếp theo")
    sources: List[SourceCitation] = Field(default_factory=list, description="Các đoạn tài liệu cẩm nang y tế được trích dẫn")
    disclaimer: str = Field(description="Cảnh báo y tế không thay thế chẩn đoán bác sĩ")
    generated_at: datetime = Field(default_factory=datetime.utcnow)


# ============================================================================
# 3. Document Ingestion & Knowledge Management
# ============================================================================

class DocumentChunkDTO(BaseModel):
    title: str
    stage: str
    topic: str
    source: str
    section: Optional[str] = None
    content: str
    chunk_index: int


class IngestDocumentRequest(BaseModel):
    title: str
    stage: MaternalStage = MaternalStage.ALL
    topic: str = "GENERAL"
    source: str = "Bộ Y Tế / Cẩm nang Y khoa"
    section: Optional[str] = None
    text_content: str


class IngestDocumentResponse(BaseModel):
    success: bool
    message: str
    total_chunks: int
    document_title: str
    stage: str


class BatchIngestResponse(BaseModel):
    success: bool
    total_files_processed: int
    total_chunks_created: int
    processed_files: List[str]
    errors: List[str] = Field(default_factory=list)


class ChunkDetailItem(BaseModel):
    id: Optional[int] = None
    title: str
    stage: str
    topic: str
    source: str
    section: Optional[str] = None
    snippet: str
    content_length: int
    chunk_index: int


class KnowledgeListResponse(BaseModel):
    total: int
    page: int
    page_size: int
    items: List[ChunkDetailItem]


class KnowledgeStatsResponse(BaseModel):
    total_chunks: int
    total_documents: int
    stage_distribution: Dict[str, int]
    topic_distribution: Dict[str, int]
    files_in_disk: List[str]


class VectorSearchTestRequest(BaseModel):
    query: str = Field(description="Câu hỏi hoặc triệu chứng cần tìm kiếm trong CSDL vector")
    stage: Optional[MaternalStage] = Field(default=None, description="Lọc theo giai đoạn (Tùy chọn)")
    top_k: int = Field(default=4, ge=1, le=20, description="Số lượng đoạn tài liệu cần lấy")


class VectorSearchTestResponse(BaseModel):
    query: str
    total_retrieved: int
    results: List[SourceCitation]


# ============================================================================
# 4. Prompt Engineering & Simulation Testing
# ============================================================================

class CustomPromptTestRequest(BaseModel):
    user_message: str
    system_instruction: Optional[str] = Field(default=None, description="System prompt tùy chỉnh để thử nghiệm")
    temperature: Optional[float] = Field(default=0.3, ge=0.0, le=1.0)
    model: Optional[str] = Field(default=None, description="Model muốn thử (gemini-flash-lite-latest, gemini-2.5-flash, gemini-3.7-flash)")


class CustomPromptTestResponse(BaseModel):
    model_used: str
    temperature: float
    answer: str
    generated_at: datetime = Field(default_factory=datetime.utcnow)


class BatchSimulationCase(BaseModel):
    case_name: str
    metrics: HealthMetricsLogRequest
    expected_status: TriageRiskStatus
    actual_status: Optional[TriageRiskStatus] = None
    passed: Optional[bool] = None
    headline: Optional[str] = None


class BatchSimulationResponse(BaseModel):
    total_cases: int
    passed_cases: int
    all_passed: bool
    results: List[BatchSimulationCase]


# ============================================================================
# 5. Health & System Status
# ============================================================================

class SystemHealthResponse(BaseModel):
    status: str
    model: str
    embedding_model: str
    database_connected: bool
    total_knowledge_chunks: int
    gemini_api_configured: bool
