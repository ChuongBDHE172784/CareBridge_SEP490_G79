export type CallType = 'VIDEO' | 'AUDIO';

export type CallStatus =
  | 'INITIATED'
  | 'RINGING'
  | 'ANSWERED'
  | 'ENDED'
  | 'MISSED'
  | 'DECLINED'
  | 'CANCELLED'
  | 'FAILED';

export interface ConsultationCallAdminSummary {
  callId: string;
  conversationId: string;
  callType: CallType;
  callStatus: CallStatus;
  initiatedAt: string;
  answeredAt: string | null;
  endedAt: string | null;
  durationSeconds: number | null;
  recordingFileId: string | null;
  recordingStatus: 'NONE' | 'RECORDING' | 'UPLOADED' | 'FAILED';
  recordedDurationSeconds: number | null;
  consentAttested: boolean;

  initiatedByUserId: string;
  initiatedByRole: 'MOTHER' | 'EXPERT' | 'UNKNOWN';

  motherUserId: string | null;
  motherName: string;
  motherPhone: string | null;
  motherEmail: string | null;

  expertUserId: string | null;
  expertName: string;
  expertSpecialization: string | null;
  expertHospital: string | null;
}

export interface ConsultationCallSearchQuery {
  keyword?: string;
  callType?: CallType;
  callStatus?: CallStatus;
  hasRecording?: boolean;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export interface PaginatedConsultationCalls {
  content: ConsultationCallAdminSummary[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
