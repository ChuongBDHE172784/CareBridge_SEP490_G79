export type AiViolationCategory =
  | 'SPAM_ADVERTISING'
  | 'HARASSMENT_BULLYING'
  | 'HATE_SPEECH'
  | 'CHILD_SAFETY'
  | 'SELF_HARM_ENCOURAGEMENT'
  | 'DANGEROUS_MEDICAL_ADVICE'
  | 'EXPERT_IMPERSONATION'
  | 'HARMFUL_MISINFORMATION'
  | 'PII_DOXXING'
  | 'SCAM_FRAUD'
  | 'PROMPT_INJECTION'
  | 'OTHER';

export type AiPolicySeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type ReportCategory =
  | 'INACCURATE_INFORMATION'
  | 'DISGUISED_ADVERTISING'
  | 'HARASSMENT'
  | 'UNSAFE_ADVICE'
  | 'SPAM'
  | 'OTHER';

export type PolicyTargetType = 'QUESTION' | 'ANSWER' | 'CONTENT';

export interface PolicyReferenceLink {
  title: string;
  url: string;
}

export interface PolicyReferenceFile {
  fileId: string;
  fileName: string;
  fileUrl: string;
  fileSizeBytes: number;
}

export interface AiPolicy {
  id: string;
  policyCode: string;
  name: string;
  detectionGuidance: string;
  violationCategory: AiViolationCategory;
  reportCategory: ReportCategory;
  severity: AiPolicySeverity;
  applicableTargetTypes: PolicyTargetType[];
  confidenceThreshold: number;
  active: boolean;
  systemDefault: boolean;
  version: number;
  referenceLinks?: PolicyReferenceLink[];
  referenceFiles?: PolicyReferenceFile[];
  createdAt: string;
  updatedAt: string;
}

export interface AiPolicyPage {
  content: AiPolicy[];
  totalElements: number;
  page: number;
  size: number;
}

export interface AiModerationStatus {
  enabled: boolean;
  configured: boolean;
  model: string;
  /** What Google reported running, once a call has succeeded. `model` is normally an
   *  alias - gemini-flash-latest - so this is the only place the release shows. */
  resolvedModel?: string | null;
  state: 'DISABLED' | 'NOT_CONFIGURED' | 'READY';
  businessToggleEnabled: boolean;
  queuedJobs: number;
  processingJobs: number;
  failedJobs: number;
  lastCompletedAt: string | null;
  policySetHash: string;
  activePolicies: number;
}

export interface AiPolicyTestMatch {
  policyCode: string;
  category: string;
  severity: AiPolicySeverity;
  confidence: number;
  evidence: string[];
  explanation: string | null;
}

export interface AiPolicyTestResult {
  classification: 'SAFE' | 'VIOLATION' | 'UNCERTAIN';
  overallSeverity: AiPolicySeverity | null;
  confidence: number | null;
  recommendedAction: string | null;
  explanation: string | null;
  matches: AiPolicyTestMatch[];
  wouldCreateCase: boolean;
  wouldCreatePriority: 'NORMAL' | 'HIGH' | 'URGENT' | null;
  model: string;
  latencyMs: number;
}

export interface CreateAiPolicyRequest {
  policyCode: string;
  name: string;
  detectionGuidance: string;
  violationCategory: AiViolationCategory;
  reportCategory: ReportCategory;
  severity: AiPolicySeverity;
  applicableTargetTypes: PolicyTargetType[];
  confidenceThreshold: number;
  active: boolean;
  referenceLinks?: PolicyReferenceLink[];
  referenceFiles?: PolicyReferenceFile[];
}

export interface UpdateAiPolicyRequest {
  name?: string;
  detectionGuidance?: string;
  violationCategory?: AiViolationCategory;
  reportCategory?: ReportCategory;
  severity?: AiPolicySeverity;
  applicableTargetTypes?: PolicyTargetType[];
  confidenceThreshold?: number;
  active?: boolean;
  referenceLinks?: PolicyReferenceLink[];
  referenceFiles?: PolicyReferenceFile[];
}

export interface AiPolicyTestRequest {
  targetType: PolicyTargetType;
  sampleText: string;
}

export const AI_VIOLATION_CATEGORY_LABELS: Record<AiViolationCategory, string> = {
  SPAM_ADVERTISING: 'Spam / quảng cáo trá hình',
  HARASSMENT_BULLYING: 'Quấy rối, bắt nạt',
  HATE_SPEECH: 'Ngôn từ thù ghét',
  CHILD_SAFETY: 'An toàn trẻ em',
  SELF_HARM_ENCOURAGEMENT: 'Cổ suý tự hại',
  DANGEROUS_MEDICAL_ADVICE: 'Lời khuyên y khoa nguy hiểm',
  EXPERT_IMPERSONATION: 'Giả mạo chuyên gia',
  HARMFUL_MISINFORMATION: 'Thông tin sai lệch gây hại',
  PII_DOXXING: 'Lộ thông tin cá nhân',
  SCAM_FRAUD: 'Lừa đảo',
  PROMPT_INJECTION: 'Thao túng bộ phân loại',
  OTHER: 'Khác',
};

export const AI_POLICY_SEVERITY_LABELS: Record<AiPolicySeverity, string> = {
  LOW: 'Thấp',
  MEDIUM: 'Trung bình',
  HIGH: 'Cao',
  CRITICAL: 'Nghiêm trọng',
};

export const AI_POLICY_SEVERITY_STYLES: Record<AiPolicySeverity, string> = {
  LOW: 'bg-surface-container-high text-on-surface-variant',
  MEDIUM: 'bg-surface-container-high text-on-surface-variant',
  HIGH: 'bg-error-container text-error',
  CRITICAL: 'bg-error text-on-primary',
};

export const REPORT_CATEGORY_LABELS: Record<ReportCategory, string> = {
  INACCURATE_INFORMATION: 'Thông tin không chính xác',
  DISGUISED_ADVERTISING: 'Quảng cáo trá hình',
  HARASSMENT: 'Quấy rối hoặc bắt nạt',
  UNSAFE_ADVICE: 'Lời khuyên không an toàn',
  SPAM: 'Nội dung rác',
  OTHER: 'Lý do khác',
};

export const POLICY_TARGET_TYPE_LABELS: Record<PolicyTargetType, string> = {
  QUESTION: 'Câu hỏi',
  ANSWER: 'Câu trả lời',
  CONTENT: 'Nội dung thư viện',
};

export function formatPolicyName(policyCode: string, category?: string): string {
  if (policyCode && AI_VIOLATION_CATEGORY_LABELS[policyCode as AiViolationCategory]) {
    return AI_VIOLATION_CATEGORY_LABELS[policyCode as AiViolationCategory];
  }
  if (category && AI_VIOLATION_CATEGORY_LABELS[category as AiViolationCategory]) {
    return AI_VIOLATION_CATEGORY_LABELS[category as AiViolationCategory];
  }
  return policyCode;
}

