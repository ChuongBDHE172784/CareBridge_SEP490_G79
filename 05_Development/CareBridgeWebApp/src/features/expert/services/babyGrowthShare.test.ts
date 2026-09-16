import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  BABY_GROWTH_SHARE_TAG,
  HEALTH_SHARE_TAG,
  fetchBabyGrowthChart,
  fetchExpertSharedRecords,
  formatBabyAgeAt,
  parseBabyGrowthShare,
  parseHealthMetricsShare,
  type BabyGrowthShareData,
} from './expertSharedRecordsService';
import { getTimeline, listMyConversations } from '../../directChat/services/directChatApi';
import apiClient from '../../../shared/api/apiClient';

vi.mock('../../directChat/services/directChatApi', () => ({
  listMyConversations: vi.fn(),
  getTimeline: vi.fn(),
}));

vi.mock('../../../shared/api/apiClient', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
}));

const makeShareData = (overrides: Partial<BabyGrowthShareData> = {}): BabyGrowthShareData => ({
  title: 'Phát triển của bé',
  babyId: '11111111-1111-1111-1111-111111111111',
  babyNickname: 'Bé Test',
  birthDate: '2025-01-10',
  measurementCount: 3,
  latest: { measuredDate: '2025-03-10', weightKg: 5.6, heightCm: 58.0, headCircumferenceCm: 39.0 },
  isLiveSync: true,
  ...overrides,
});

const growthBody = (data: BabyGrowthShareData = makeShareData()) =>
  `${BABY_GROWTH_SHARE_TAG}\n${JSON.stringify(data)}`;

describe('baby growth share parsing (SBG-TC-014)', () => {
  it('rejects non-tag, malformed and babyId-less bodies', () => {
    const invalid = [
      undefined,
      'hello',
      `${HEALTH_SHARE_TAG}\n{}`,
      `${BABY_GROWTH_SHARE_TAG}\n{bad`,
      `${BABY_GROWTH_SHARE_TAG}\n{"babyNickname":"x"}`,
    ];
    for (const body of invalid) {
      expect(parseBabyGrowthShare(body)).toBeNull();
    }
  });

  it('parses a valid body and is not mistaken for a health share', () => {
    const parsed = parseBabyGrowthShare(growthBody());

    expect(parsed?.babyId).toBe('11111111-1111-1111-1111-111111111111');
    expect(parsed?.latest?.headCircumferenceCm).toBe(39);
    expect(parseHealthMetricsShare(growthBody())).toBeNull();
  });

  it('formats age at a measurement date with BabyProfile.ageLabel rules', () => {
    expect(formatBabyAgeAt('2025-01-10', '2025-03-10')).toBe('2 tháng tuổi');
    expect(formatBabyAgeAt('2025-01-10', '2025-01-10')).toBe('0 ngày tuổi');
    expect(formatBabyAgeAt('2024-01-10', '2025-03-10')).toBe('1 tuổi 2 tháng');
    expect(formatBabyAgeAt('2024-01-10', '2025-01-10')).toBe('1 tuổi');
  });
});

describe('fetchBabyGrowthChart contract (SBG-TC-015 support)', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('maps GrowthChartResponse measurements by field name and orders them oldest first', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: {
        success: true,
        data: {
          babyId: '11111111-1111-1111-1111-111111111111',
          nickname: 'Bé Test',
          birthDate: '2025-01-10',
          measurements: [
            { growthMeasurementId: 'm3', measuredDate: '2025-03-10', weightKg: 5.6, heightCm: 58.0, headCircumferenceCm: 39.0, ageInDays: 59 },
            { growthMeasurementId: 'm1', measuredDate: '2025-01-10', weightKg: 3.2, heightCm: 50.0, headCircumferenceCm: 34.0, ageInDays: 0 },
          ],
        },
      },
    } as never);

    const points = await fetchBabyGrowthChart('11111111-1111-1111-1111-111111111111');

    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/babies/11111111-1111-1111-1111-111111111111/growth-chart');
    expect(points.map((p) => p.measuredDate)).toEqual(['2025-01-10', '2025-03-10']);
    expect(points[1]).toMatchObject({ weightKg: 5.6, heightCm: 58.0, headCircumferenceCm: 39.0 });
  });
});

describe('fetchExpertSharedRecords (SBG-TC-017)', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('emits a BABY_GROWTH entry alongside health shares and skips recalled shares', async () => {
    vi.mocked(listMyConversations).mockResolvedValue([
      { conversationId: 'c1', counterpartUserId: '22222222-2222-2222-2222-222222222222' } as never,
    ]);
    vi.mocked(getTimeline).mockResolvedValue({
      items: [
        { kind: 'MESSAGE', messageId: 'g1', messageBody: growthBody(), createdAt: '2026-09-14T08:00:00Z' },
        {
          kind: 'MESSAGE',
          messageId: 'h1',
          messageBody: `${HEALTH_SHARE_TAG}\n${JSON.stringify({ title: 'Chỉ số', isLiveSync: false, metrics: [] })}`,
          createdAt: '2026-09-14T08:01:00Z',
        },
        {
          kind: 'MESSAGE',
          messageId: 'g2',
          messageBody: growthBody(),
          recalledAt: '2026-09-14T08:03:00Z',
          createdAt: '2026-09-14T08:02:00Z',
        },
      ],
    } as never);

    const records = await fetchExpertSharedRecords();

    const growth = records.filter((r) => r.type === 'BABY_GROWTH');
    expect(growth).toHaveLength(1);
    expect(growth[0].babyGrowthData?.babyId).toBe('11111111-1111-1111-1111-111111111111');
    expect(growth[0].conversationId).toBe('c1');
    expect(growth[0].alertLevel).toBe('NORMAL');
    expect(growth[0].status).toBe('PENDING_REVIEW');
    expect(records.filter((r) => r.type === 'HEALTH_METRICS')).toHaveLength(1);
  });
});
