export type RedFlagSeverity = 'GREEN' | 'YELLOW' | 'RED';
export type RedFlagAction = 'BLOCK' | 'WARN' | 'ESCALATE';

export interface RedFlagRule {
  id: string;
  keyword: string;
  severity: RedFlagSeverity;
  action: RedFlagAction;
  isActive: boolean;
  isSystemDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface RedFlagRulePage {
  content: RedFlagRule[];
  totalElements: number;
  page: number;
  size: number;
}

export interface CreateRedFlagRuleRequest {
  keyword: string;
  severity: RedFlagSeverity;
  action: RedFlagAction;
}

export interface UpdateRedFlagRuleRequest {
  keyword?: string;
  severity?: RedFlagSeverity;
  action?: RedFlagAction;
  isActive?: boolean;
}

export const SEVERITY_LABELS: Record<RedFlagSeverity, string> = {
  GREEN: 'Bình thường',
  YELLOW: 'Cảnh báo',
  RED: 'Nghiêm trọng',
};

export const SEVERITY_STYLES: Record<RedFlagSeverity, string> = {
  GREEN: 'bg-[#e6f4ea] text-[#1e7e34]',
  YELLOW: 'bg-[#ffe2d9] text-[#845143]',
  RED: 'bg-[#ffdad6] text-[#ba1a1a]',
};

export const ACTION_LABELS: Record<RedFlagAction, string> = {
  BLOCK: 'Chặn',
  WARN: 'Cảnh báo',
  ESCALATE: 'Leo thang',
};
