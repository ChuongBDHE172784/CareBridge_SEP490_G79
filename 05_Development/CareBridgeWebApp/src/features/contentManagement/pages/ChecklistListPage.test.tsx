// @vitest-environment jsdom

import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminChecklistTemplate, PaginatedResponse } from '../models/content';

const harness = vi.hoisted(() => ({
  fetchAdminChecklists: vi.fn(),
  navigate: vi.fn(),
}));

vi.mock('../services/contentApi', () => ({
  fetchAdminChecklists: harness.fetchAdminChecklists,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>(
    'react-router-dom',
  );
  return { ...actual, useNavigate: () => harness.navigate };
});

import ChecklistListPage from './ChecklistListPage';

function checklist(
  overrides: Partial<AdminChecklistTemplate> = {},
): AdminChecklistTemplate {
  return {
    id: 'synthetic-checklist-69',
    name: 'Synthetic approved checklist',
    stage: 'PREGNANCY',
    status: 'APPROVED',
    description: 'Synthetic metadata only',
    versionNo: 1,
    updatedAt: '2026-07-23T00:00:00Z',
    itemCount: 4,
    ...overrides,
  };
}

function page(
  content: AdminChecklistTemplate[],
  number = 0,
  totalPages = content.length === 0 ? 0 : 1,
  totalElements = content.length,
): PaginatedResponse<AdminChecklistTemplate> {
  return {
    content,
    number,
    size: 10,
    totalElements,
    totalPages,
  };
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

describe('UC82-69-WEB-001 admin checklist boundary', () => {
  beforeEach(() => {
    harness.fetchAdminChecklists.mockReset();
    harness.navigate.mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders true status and itemCount from a successful admin response', async () => {
    harness.fetchAdminChecklists.mockResolvedValue(page([
      checklist({ id: 'draft', name: 'Draft with items', status: 'DRAFT', itemCount: 4 }),
      checklist({ id: 'rejected', name: 'Rejected empty', status: 'REJECTED', itemCount: 0 }),
      checklist({ id: 'approved', name: 'Approved empty', status: 'APPROVED', itemCount: 0 }),
    ]));

    render(<ChecklistListPage />);

    expect(await screen.findByText('Draft with items')).toBeTruthy();
    expect(screen.getByText('DRAFT · Bản nháp')).toBeTruthy();
    expect(screen.getByText('REJECTED · Đã từ chối')).toBeTruthy();
    expect(screen.getByText('APPROVED · Đã duyệt')).toBeTruthy();
    expect(screen.getByText('4')).toBeTruthy();
    expect(harness.fetchAdminChecklists).toHaveBeenCalledWith({
      stage: undefined,
      status: undefined,
      page: 0,
      size: 10,
    });
  });

  it('does not expose an edit action when no edit route exists', async () => {
    harness.fetchAdminChecklists.mockResolvedValue(page([
      checklist({ name: 'Checklist without edit route' }),
    ]));

    render(<ChecklistListPage />);

    expect(await screen.findByText('Checklist without edit route')).toBeTruthy();
    expect(
      screen.queryByRole('button', {
        name: 'Chỉnh sửa checklist Checklist without edit route',
      }),
    ).toBeNull();
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Xem checklist Checklist without edit route',
      }),
    );
    expect(harness.navigate).toHaveBeenCalledWith(
      '/content/checklists/synthetic-checklist-69',
    );

    fireEvent.click(screen.getByRole('button', { name: 'Tạo checklist mới' }));
    expect(harness.navigate).toHaveBeenCalledWith('/content/checklists/create');
  });

  it('runs real filter hooks and resets the requested page to zero', async () => {
    harness.fetchAdminChecklists.mockResolvedValue(page([]));
    render(<ChecklistListPage />);
    await waitFor(() => expect(harness.fetchAdminChecklists).toHaveBeenCalledTimes(1));

    fireEvent.change(screen.getByLabelText('Lọc checklist theo giai đoạn'), {
      target: { value: 'PREGNANCY' },
    });
    await waitFor(() => expect(harness.fetchAdminChecklists).toHaveBeenLastCalledWith({
      stage: 'PREGNANCY',
      status: undefined,
      page: 0,
      size: 10,
    }));

    fireEvent.change(screen.getByLabelText('Lọc checklist theo trạng thái duyệt'), {
      target: { value: 'REJECTED' },
    });
    await waitFor(() => expect(harness.fetchAdminChecklists).toHaveBeenLastCalledWith({
      stage: 'PREGNANCY',
      status: 'REJECTED',
      page: 0,
      size: 10,
    }));
  });

  it('runs real pagination hooks and renders the requested page', async () => {
    harness.fetchAdminChecklists
      .mockResolvedValueOnce(page([checklist({ id: 'page-zero', name: 'Page zero' })], 0, 2, 11))
      .mockResolvedValueOnce(page([checklist({ id: 'page-one', name: 'Page one' })], 1, 2, 11));
    render(<ChecklistListPage />);

    expect(await screen.findByText('Page zero')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Trang checklist sau' }));

    expect(await screen.findByText('Page one')).toBeTruthy();
    expect(screen.queryByText('Page zero')).toBeNull();
    expect(harness.fetchAdminChecklists).toHaveBeenLastCalledWith({
      stage: undefined,
      status: undefined,
      page: 1,
      size: 10,
    });
  });

  it('clamps and reloads when the server shrinks totalPages below the requested page', async () => {
    harness.fetchAdminChecklists
      .mockResolvedValueOnce(page([checklist({ id: 'page-zero', name: 'Original page zero' })], 0, 2, 11))
      .mockResolvedValueOnce(page([], 1, 1, 1))
      .mockResolvedValueOnce(page([checklist({ id: 'survivor', name: 'Remaining checklist' })], 0, 1, 1));
    render(<ChecklistListPage />);

    expect(await screen.findByText('Original page zero')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Trang checklist sau' }));

    await waitFor(() => expect(harness.fetchAdminChecklists).toHaveBeenCalledTimes(3));
    expect(harness.fetchAdminChecklists).toHaveBeenLastCalledWith({
      stage: undefined,
      status: undefined,
      page: 0,
      size: 10,
    });
    expect(await screen.findByText('Remaining checklist')).toBeTruthy();
    expect(screen.getByText('Trang 1/1')).toBeTruthy();
    expect(screen.getByText('Hiển thị 1-1 trên 1 kết quả')).toBeTruthy();
    expect(screen.queryByText(/Trang 2\/1/)).toBeNull();
  });

  it('clears stale rows on the latest error and retries successfully', async () => {
    harness.fetchAdminChecklists
      .mockResolvedValueOnce(page([checklist({ name: 'Previous row' })]))
      .mockRejectedValueOnce(new Error('synthetic failure'))
      .mockResolvedValueOnce(page([checklist({ name: 'Recovered row' })]));
    render(<ChecklistListPage />);

    expect(await screen.findByText('Previous row')).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Lọc checklist theo trạng thái duyệt'), {
      target: { value: 'DRAFT' },
    });

    expect((await screen.findByRole('alert')).textContent).toBe(
      'Không thể tải danh sách checklist. Vui lòng thử lại.',
    );
    expect(screen.queryByText('Previous row')).toBeNull();

    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }));
    expect(await screen.findByText('Recovered row')).toBeTruthy();
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it('keeps loading and rows owned by the latest request when an older success resolves first', async () => {
    const older = deferred<PaginatedResponse<AdminChecklistTemplate>>();
    const latest = deferred<PaginatedResponse<AdminChecklistTemplate>>();
    harness.fetchAdminChecklists
      .mockReturnValueOnce(older.promise)
      .mockReturnValueOnce(latest.promise);
    render(<ChecklistListPage />);
    await waitFor(() => expect(harness.fetchAdminChecklists).toHaveBeenCalledTimes(1));

    fireEvent.change(screen.getByLabelText('Lọc checklist theo giai đoạn'), {
      target: { value: 'POSTPARTUM' },
    });
    await waitFor(() => expect(harness.fetchAdminChecklists).toHaveBeenCalledTimes(2));

    await act(async () => {
      older.resolve(page([checklist({ name: 'Stale row' })]));
      await older.promise;
    });
    expect(screen.getByText('Đang tải...')).toBeTruthy();
    expect(screen.queryByText('Stale row')).toBeNull();

    await act(async () => {
      latest.resolve(page([checklist({ name: 'Latest row', stage: 'POSTPARTUM' })]));
      await latest.promise;
    });
    expect(await screen.findByText('Latest row')).toBeTruthy();
    expect(screen.queryByText('Đang tải...')).toBeNull();
  });

  it('ignores an older rejection after the latest request succeeds', async () => {
    const older = deferred<PaginatedResponse<AdminChecklistTemplate>>();
    const latest = deferred<PaginatedResponse<AdminChecklistTemplate>>();
    harness.fetchAdminChecklists
      .mockReturnValueOnce(older.promise)
      .mockReturnValueOnce(latest.promise);
    render(<ChecklistListPage />);
    await waitFor(() => expect(harness.fetchAdminChecklists).toHaveBeenCalledTimes(1));

    fireEvent.change(screen.getByLabelText('Lọc checklist theo trạng thái duyệt'), {
      target: { value: 'APPROVED' },
    });
    await waitFor(() => expect(harness.fetchAdminChecklists).toHaveBeenCalledTimes(2));

    await act(async () => {
      latest.resolve(page([checklist({ name: 'Latest retained row' })]));
      await latest.promise;
    });
    expect(await screen.findByText('Latest retained row')).toBeTruthy();

    await act(async () => {
      older.reject(new Error('stale synthetic failure'));
      try {
        await older.promise;
      } catch {
        // The component must consume the stale rejection without mutating UI state.
      }
    });
    expect(screen.getByText('Latest retained row')).toBeTruthy();
    expect(screen.queryByRole('alert')).toBeNull();
    expect(screen.queryByText('Đang tải...')).toBeNull();
  });

  it('renders exact Vietnamese fallbacks for nullable stage and updatedAt', async () => {
    const nullableRow = {
      ...checklist({ id: 'nullable', name: 'Nullable metadata row' }),
      stage: null,
      updatedAt: null,
    } satisfies AdminChecklistTemplate;
    harness.fetchAdminChecklists.mockResolvedValue(page([nullableRow]));

    render(<ChecklistListPage />);

    expect(await screen.findByText('Nullable metadata row')).toBeTruthy();
    expect(screen.getByText('Không xác định')).toBeTruthy();
    expect(screen.getByText('Chưa cập nhật')).toBeTruthy();
  });
});
