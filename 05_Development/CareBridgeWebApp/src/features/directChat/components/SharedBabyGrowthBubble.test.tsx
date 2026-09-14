import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { SharedBabyGrowthBubble } from './SharedBabyGrowthBubble';
import {
  fetchBabyGrowthChart,
  type BabyGrowthPoint,
  type BabyGrowthShareData,
} from '../../expert/services/expertSharedRecordsService';

vi.mock('../../expert/services/expertSharedRecordsService', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../expert/services/expertSharedRecordsService')>();
  return { ...actual, fetchBabyGrowthChart: vi.fn() };
});

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

const makeGrowthPoints = (includeRecent = false): BabyGrowthPoint[] => [
  { measuredDate: '2025-01-10', weightKg: 3.2, heightCm: 50.0, headCircumferenceCm: 34.0 },
  { measuredDate: '2025-02-10', weightKg: 4.5, heightCm: 54.5, headCircumferenceCm: null },
  { measuredDate: '2025-03-10', weightKg: 5.6, heightCm: 58.0, headCircumferenceCm: 39.0 },
  ...(includeRecent
    ? [{ measuredDate: '2026-09-01', weightKg: 9.8, heightCm: 75.0, headCircumferenceCm: 45.0 }]
    : []),
];

describe('SharedBabyGrowthBubble', () => {
  beforeEach(() => {
    vi.mocked(fetchBabyGrowthChart).mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it('SBG-TC-015 renders three charts over all data and toggles history below them', async () => {
    vi.mocked(fetchBabyGrowthChart).mockResolvedValue(makeGrowthPoints(true));

    const { container } = render(<SharedBabyGrowthBubble data={makeShareData()} isOwn={false} />);

    expect(await screen.findByText('Xu hướng cân nặng')).toBeTruthy();
    expect(screen.getByText('Xu hướng chiều cao')).toBeTruthy();
    expect(screen.getByText('Xu hướng vòng đầu')).toBeTruthy();
    const charts = container.querySelectorAll('[data-testid="baby-growth-chart"]');
    expect(charts).toHaveLength(3);
    expect(screen.getByText('3.2 kg – 9.8 kg')).toBeTruthy();
    expect(screen.getByText('50.0 cm – 75.0 cm')).toBeTruthy();
    expect(screen.getByText('34.0 cm – 45.0 cm')).toBeTruthy();
    expect(fetchBabyGrowthChart).toHaveBeenCalledTimes(1);
    expect(fetchBabyGrowthChart).toHaveBeenCalledWith('11111111-1111-1111-1111-111111111111');
    expect(screen.queryAllByTestId('baby-growth-history-row')).toHaveLength(0);

    const toggle = screen.getByRole('button', { name: 'Xem lịch sử đo (4)' });
    expect(charts[2].compareDocumentPosition(toggle) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    fireEvent.click(toggle);

    const rows = screen.getAllByTestId('baby-growth-history-row');
    expect(rows).toHaveLength(4);
    expect(rows[0].textContent).toContain('01/09/2026 · 1 tuổi 8 tháng');
    expect(rows[1].textContent).toContain('10/03/2025 · 2 tháng tuổi');
    expect(rows[3].textContent).toContain('10/01/2025 · 0 ngày tuổi');
    expect(screen.getByText('4.5 kg · 54.5 cm · —')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Thu gọn lịch sử' }));
    expect(screen.queryAllByTestId('baby-growth-history-row')).toHaveLength(0);

    expect(screen.queryByText('Bình thường')).toBeNull();
    expect(screen.queryByText('Cần lưu ý')).toBeNull();
    expect(screen.queryByText('Nguy hiểm')).toBeNull();
  });

  it('SBG-TC-016 falls back to the snapshot when loading fails', async () => {
    vi.mocked(fetchBabyGrowthChart).mockRejectedValue(new Error('403'));

    render(<SharedBabyGrowthBubble data={makeShareData()} />);

    expect(await screen.findByText('Không thể tải dữ liệu tăng trưởng mới nhất')).toBeTruthy();
    expect(screen.getAllByText(/5\.6 kg/).length).toBeGreaterThan(0);
    expect(screen.queryByText(/403/)).toBeNull();
  });

  it('SBG-TC-016 shows empty states when the baby has no measurements', async () => {
    vi.mocked(fetchBabyGrowthChart).mockResolvedValue([]);

    render(<SharedBabyGrowthBubble data={makeShareData({ measurementCount: 0, latest: null })} />);

    expect(await screen.findByText('Chưa có dữ liệu cân nặng.')).toBeTruthy();
    expect(screen.getByText('Chưa có dữ liệu chiều cao.')).toBeTruthy();
    expect(screen.getByText('Chưa có dữ liệu vòng đầu.')).toBeTruthy();
    expect(screen.queryByRole('button', { name: /Xem lịch sử đo/ })).toBeNull();
  });
});
