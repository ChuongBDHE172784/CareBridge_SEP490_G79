export type AnalysisMode =
  "REAL_TIME" | "VIDEO_BATCH" | "HYBRID" | "MODEL_BASED" | "RULE_BASED";
export type PostureFeedbackLevel =
  "BASIC" | "DETAILED" | "STRICT" | "STANDARD" | "LENIENT";
export type PostureConfigStatus =
  "ACTIVE" | "SUPERSEDED" | "DRAFT" | "DISABLED";

export interface AdminPostureConfig {
  postureConfigId: string;
  exerciseId: string;
  configuredBy: string | null;
  analysisMode: AnalysisMode;
  ruleOrModelVersion: string | null;
  confidenceThreshold: number;
  feedbackLevel: PostureFeedbackLevel | null;
  configJson: string | null;
  effectiveFrom: string | null;
  effectiveTo: string | null;
  status: PostureConfigStatus;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface PostureConfigCreateForm {
  exerciseId: string;
  analysisMode: AnalysisMode;
  ruleOrModelVersion: string;
  confidenceThreshold: number;
  feedbackLevel: PostureFeedbackLevel;
  configJson?: string;
}

export interface PostureConfigVersionForm {
  analysisMode: AnalysisMode;
  ruleOrModelVersion: string;
  confidenceThreshold: number;
  feedbackLevel: PostureFeedbackLevel;
  configJson?: string;
}

export const analysisModeLabels: Record<AnalysisMode, string> = {
  REAL_TIME: "Thời gian thực",
  VIDEO_BATCH: "Phân tích video tải lên",
  HYBRID: "Hỗn hợp",
  MODEL_BASED: "Pose Landmark",
  RULE_BASED: "Quy tắc an toàn",
};

export const feedbackLevelLabels: Record<PostureFeedbackLevel, string> = {
  BASIC: "Cơ bản",
  DETAILED: "Chi tiết",
  STRICT: "Nghiêm ngặt",
  STANDARD: "Tiêu chuẩn",
  LENIENT: "Linh hoạt",
};
