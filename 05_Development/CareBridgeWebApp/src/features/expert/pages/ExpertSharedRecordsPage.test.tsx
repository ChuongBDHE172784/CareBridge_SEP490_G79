import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import ExpertSharedRecordsPage from './ExpertSharedRecordsPage';
import {
  fetchBabyGrowthChart,
  fetchExpertSharedRecords,
  type SharedRecordEntry,
} from '../services/expertSharedRecordsService';

vi.mock('../services/expertSharedRecordsService', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../services/expertSharedRecordsService')>();
  return { ...actual, fetchExpertSharedRecords: vi.fn(), fetchBabyGrowthChart: vi.fn() };
});

const makeRecord = (overrides: Partial<SharedRecordEntry>): SharedRecordEntry => ({
  id: 'rec-1',
  conversationId: 'conv-1',
  motherUserId: 'mother-1',
  motherName: 'Mẹ M1',
  createdAt: '2026-09-14T08:00:00Z',
  type: 'BABY_GROWTH',
  alertLevel: 'NORMAL',
  status: 'PENDING_REVIEW',
  ...overrides,
});

describe('ExpertSharedRecordsPage (SBG-TC-018)', () => {
  afterEach(() => {
    cleanup();
  });

  it('filters mother cards by the baby growth tab', async () => {
    vi.mocked(fetchExpertSharedRecords).mockResolvedValue([
      makeRecord({
        babyGrowthData: {
          title: 'Phát triển của bé',
          babyId: '11111111-1111-1111-1111-111111111111',
          babyNickname: 'Bé Test',
          birthDate: '2025-01-10',
          measurementCount: 1,
          latest: null,
          isLiveSync: true,
        },
      }),
      makeRecord({
        id: 'rec-2',
        conversationId: 'conv-2',
        motherUserId: 'mother-2',
        motherName: 'Mẹ M2',
        type: 'CHECKLIST',
        checklistData: {
          title: 'Checklist',
          completedCount: 0,
          totalCount: 1,
          progressPercent: 0,
          currentItems: [{ text: 'Khám thai', completed: false }],
        },
      }),
    ]);
    vi.mocked(fetchBabyGrowthChart).mockResolvedValue([
      { measuredDate: '2025-01-10', weightKg: 3.2, heightCm: 50.0, headCircumferenceCm: 34.0 },
    ]);

    render(
      <MemoryRouter>
        <ExpertSharedRecordsPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText('Mẹ M2')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: /^Tăng trưởng bé \(/ }));

    expect(screen.getByText('Mẹ M1')).toBeTruthy();
    expect(screen.queryByText('Mẹ M2')).toBeNull();
    expect(await screen.findByText('Xu hướng cân nặng')).toBeTruthy();
  });
});
