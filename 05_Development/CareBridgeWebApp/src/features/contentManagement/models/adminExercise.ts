export type TrimesterScope = "FIRST" | "SECOND" | "THIRD" | "ALL";
export type DifficultyLevel = "EASY" | "MEDIUM" | "HARD";
export type ExerciseStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";

export interface AdminExercise {
  exerciseId: string;
  title: string;
  description: string | null;
  trimesterScope: TrimesterScope;
  difficultyLevel: DifficultyLevel;
  durationMinutes: number;
  instructionContent: string | null;
  mediaUrl: string | null;
  safetyWarning: string | null;
  supportsPostureAnalysis: boolean;
  status: ExerciseStatus;
  versionNo: number;
  createdBy: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface AdminExerciseForm {
  title: string;
  description: string;
  trimesterScope: TrimesterScope;
  difficultyLevel: DifficultyLevel;
  durationMinutes: number;
  instructionContent: string;
  mediaUrl: string;
  safetyWarning: string;
  supportsPostureAnalysis: boolean;
}

export type AdminExerciseFormField = keyof AdminExerciseForm;

export type AdminExerciseFieldErrors = Partial<Record<AdminExerciseFormField, string>>;

export interface AdminExerciseRequestError {
  code?: string;
  message: string;
  fieldErrors: AdminExerciseFieldErrors;
}

export const adminExerciseLimits = {
  titleMaxLength: 255,
  safetyWarningMaxLength: 2000,
  durationMinutesMin: 1,
  durationMinutesMax: 180,
} as const;

export interface PaginatedResponse<T> {
  success: boolean;
  data: T[];
  message: string | null;
  timestamp: string;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const trimesterLabels: Record<TrimesterScope, string> = {
  FIRST: "Tam cá nguyệt 1",
  SECOND: "Tam cá nguyệt 2",
  THIRD: "Tam cá nguyệt 3",
  ALL: "Tất cả giai đoạn",
};

export const difficultyLabels: Record<DifficultyLevel, string> = {
  EASY: "Dễ",
  MEDIUM: "Trung bình",
  HARD: "Nâng cao",
};

export const statusLabels: Record<ExerciseStatus, string> = {
  DRAFT: "Bản nháp",
  PUBLISHED: "Đã xuất bản",
  ARCHIVED: "Đã lưu trữ",
};
