export interface UserMetrics {
  total: number;
  byRole: Record<string, number>;
  active: number;
}

export interface ContentCountMetrics {
  total: number;
  byStatus: Record<string, number>;
  newInPeriod: number;
}

export interface ReportMetrics {
  byStatus: Record<string, number>;
  avgHandlingTimeSeconds: number | null;
}

export interface TrendingTopic {
  topicId: string;
  topicName: string;
  questionCount: number;
}

export interface CommunityDashboardResponse {
  userMetrics: UserMetrics;
  questionMetrics: ContentCountMetrics;
  answerMetrics: ContentCountMetrics;
  reportMetrics: ReportMetrics;
  trendingTopics: TrendingTopic[];
  generatedAt: string;
}

export interface DashboardDateRange {
  from?: string;
  to?: string;
}
