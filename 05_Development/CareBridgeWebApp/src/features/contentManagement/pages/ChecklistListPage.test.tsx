// @vitest-environment jsdom

import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminChecklistTemplate, ChecklistItem, PaginatedResponse } from '../models/content';

const harness = vi.hoisted(() => {
  const fetchAdminChecklistTemplates = vi.fn();
  return {
    fetchAdminChecklistTemplates,
    // Alias retained so the existing boundary tests continue to describe the
    // same request while the page migrates to the canonical endpoint.
    fetchAdminChecklists: fetchAdminChecklistTemplates,
    updateChecklistTemplate: vi.fn(),
    archiveChecklistTemplate: vi.fn(),
    navigate: vi.fn(),
  };
});

vi.mock('../services/contentApi', () => ({
  fetchAdminChecklistTemplates: harness.fetchAdminChecklistTemplates,
  updateChecklistTemplate: harness.updateChecklistTemplate,
  archiveChecklistTemplate: harness.archiveChecklistTemplate,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>(
    'react-router-dom',
  );
  return { ...actual, useNavigate: () => harness.navigate };
});

import ChecklistListPage from './ChecklistListPage';

type ChecklistListFixture = AdminChecklistTemplate & { items?: ChecklistItem[] };

function checklist(
  overrides: Partial<ChecklistListFixture> = {},
): ChecklistListFixture {
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
    harness.updateChecklistTemplate.mockReset();
    harness.archiveChecklistTemplate.mockReset();
    harness.navigate.mockReset();
    harness.fetchAdminChecklists.mockResolvedValue(page([]));
    harness.updateChecklistTemplate.mockResolvedValue(checklist({ status: 'PENDING_REVIEW' }));
    vi.spyOn(window, 'prompt').mockReturnValue(null);
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('renders Vietnamese-only statuses and itemCount from a successful admin response', async () => {
    harness.fetchAdminChecklists.mockResolvedValue(page([
      checklist({ id: 'draft', name: 'Draft with items', status: 'DRAFT', itemCount: 4 }),
      checklist({ id: 'pending', name: 'Pending review', status: 'PENDING_REVIEW', itemCount: 1 }),
      checklist({ id: 'rejected', name: 'Rejected empty', status: 'REJECTED', itemCount: 0 }),
      checklist({ id: 'approved', name: 'Approved empty', status: 'APPROVED', itemCount: 0 }),
      checklist({ id: 'archived', name: 'Archived empty', status: 'ARCHIVED', itemCount: 0 }),
    ]));

    render(<ChecklistListPage />);

    expect(await screen.findByText('Draft with items')).toBeTruthy();
    expect(screen.getAllByText('Bản nháp').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Chờ duyệt').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Đã từ chối').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Đã duyệt').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Đã lưu trữ').length).toBeGreaterThan(0);
    expect(screen.queryByText(/DRAFT ·|PENDING_REVIEW ·|REJECTED ·|APPROVED ·|ARCHIVED ·/)).toBeNull();
    expect(screen.getByText('4')).toBeTruthy();
    expect(harness.fetchAdminChecklists).toHaveBeenCalledWith({
      stage: undefined,
      status: undefined,
      page: 0,
      size: 10,
    });
  });

  it('renders canonical recipient, substage, and per-item target metadata', async () => {
    harness.fetchAdminChecklists.mockResolvedValue(page([checklist({
      name: 'Canonical metadata checklist',
      recipientRoles: ['MOTHER', 'FAMILY'],
      substage: {
        code: 'BABY_CARE_MONTH_0_3',
        anchor: 'BIRTH_DATE',
        startInclusive: 0,
        endInclusive: 3,
        unit: 'MONTH',
      },
      itemCount: 2,
      items: [
        { id: 'one', itemText: 'Theo dõi mẹ', order: 1, isRequired: true, targetSubject: 'MOTHER' },
        { id: 'two', itemText: 'Theo dõi bé', order: 2, isRequired: false, targetSubject: 'BABY' },
      ],
    })]));

    render(<ChecklistListPage />);

    expect(await screen.findByText('Canonical metadata checklist')).toBeTruthy();
    expect(screen.getByLabelText('Người nhận: Mẹ')).toBeTruthy();
    expect(screen.getByLabelText('Người nhận: Gia đình')).toBeTruthy();
    expect(screen.getByText('BABY_CARE_MONTH_0_3')).toBeTruthy();
    expect(screen.getByLabelText('Mục 1: Mẹ')).toBeTruthy();
    expect(screen.getByLabelText('Mục 2: Em bé')).toBeTruthy();
  });

  it('labels returned drafts and exposes the review reason', async () => {
    harness.fetchAdminChecklists.mockResolvedValue(page([
      checklist({
        name: 'Checklist cần sửa',
        status: 'DRAFT',
        latestReviewFeedback: {
          reason: 'Bổ sung mục khám thai bắt buộc',
          requestedAt: '2026-07-27T10:00:00Z',
          requestedBy: 'admin-id',
          versionNo: 2,
        },
      }),
    ]));

    render(<ChecklistListPage />);

    expect(await screen.findByText('Checklist cần sửa')).toBeTruthy();
    expect(screen.getByText('Cần chỉnh sửa')).toBeTruthy();
    expect(screen.getByText('Bổ sung mục khám thai bắt buộc')).toBeTruthy();
  });

  it('enables actions according to the checklist lifecycle', async () => {
    harness.fetchAdminChecklists.mockResolvedValue(page([
      checklist({ id: 'draft', name: 'Draft checklist', status: 'DRAFT' }),
      checklist({ id: 'pending', name: 'Pending checklist', status: 'PENDING_REVIEW' }),
      checklist({ id: 'approved', name: 'Approved checklist', status: 'APPROVED' }),
      checklist({ id: 'rejected', name: 'Rejected checklist', status: 'REJECTED' }),
      checklist({ id: 'archived', name: 'Archived checklist', status: 'ARCHIVED' }),
    ]));

    render(<ChecklistListPage />);

    expect(await screen.findByText('Draft checklist')).toBeTruthy();
    expect(screen.getByTestId('checklist-list-page').className).toContain('bg-background');
    expect(screen.getByTestId('checklist-list-page').className).toContain('font-sans');
    expect(screen.getByRole('heading', { name: 'Quản lý Checklist' }).className).toContain('text-on-surface');
    const draftEdit = screen.getByRole('button', { name: 'Chỉnh sửa checklist Draft checklist' });
    const pendingEdit = screen.getByRole('button', { name: 'Chỉnh sửa checklist Pending checklist' });
    const approvedEdit = screen.getByRole('button', { name: 'Chỉnh sửa checklist Approved checklist' });
    const rejectedEdit = screen.getByRole('button', { name: 'Chỉnh sửa checklist Rejected checklist' });
    const archivedEdit = screen.getByRole('button', { name: 'Chỉnh sửa checklist Archived checklist' });
    const archivedDelete = screen.getByRole('button', { name: 'Xóa checklist Archived checklist' });

    expect((draftEdit as HTMLButtonElement).disabled).toBe(false);
    expect((pendingEdit as HTMLButtonElement).disabled).toBe(false);
    expect((approvedEdit as HTMLButtonElement).disabled).toBe(true);
    expect((rejectedEdit as HTMLButtonElement).disabled).toBe(true);
    expect((archivedEdit as HTMLButtonElement).disabled).toBe(true);
    expect((archivedDelete as HTMLButtonElement).disabled).toBe(true);

    const draftView = screen.getByRole('button', { name: 'Xem checklist Draft checklist' });
    const draftDelete = screen.getByRole('button', { name: 'Xóa checklist Draft checklist' });
    for (const action of [draftView, draftEdit, draftDelete]) {
      expect(action.className).toContain('min-h-12');
      expect(action.className).toContain('min-w-12');
      expect(action.className).toContain('rounded-full');
      expect(action.querySelector('[aria-hidden="true"]')).toBeTruthy();
    }

    fireEvent.click(draftEdit);
    expect(harness.navigate).toHaveBeenCalledWith('/content/checklists/draft/edit');
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Xem checklist Archived checklist',
      }),
    );
    expect(harness.navigate).toHaveBeenCalledWith('/content/checklists/archived');

    fireEvent.click(archivedDelete);
    expect(window.prompt).not.toHaveBeenCalled();
    expect(harness.archiveChecklistTemplate).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'Tạo checklist mới' }));
    expect(harness.navigate).toHaveBeenCalledWith('/content/checklists/create');
  });

  it('does not delete when the reason prompt is cancelled', async () => {
    harness.fetchAdminChecklists.mockResolvedValue(page([
      checklist({ name: 'Cancelled delete', status: 'DRAFT' }),
    ]));

    render(<ChecklistListPage />);
    fireEvent.click(await screen.findByRole('button', { name: 'Xóa checklist Cancelled delete' }));

    expect(window.prompt).toHaveBeenCalledTimes(1);
    expect(harness.archiveChecklistTemplate).not.toHaveBeenCalled();
    expect(harness.fetchAdminChecklists).toHaveBeenCalledTimes(1);
  });

  it('rejects a blank delete reason without calling the archive API', async () => {
    vi.mocked(window.prompt).mockReturnValue('   ');
    harness.fetchAdminChecklists.mockResolvedValue(page([
      checklist({ name: 'Blank reason', status: 'APPROVED' }),
    ]));

    render(<ChecklistListPage />);
    fireEvent.click(await screen.findByRole('button', { name: 'Xóa checklist Blank reason' }));

    expect(screen.getByRole('alert').textContent).toBe('Vui lòng nhập lý do trước khi xóa.');
    expect(harness.archiveChecklistTemplate).not.toHaveBeenCalled();
  });

  it('archives with a trimmed reason, blocks duplicate actions, and reloads the active query', async () => {
    const archive = deferred<{ previousStatus: 'DRAFT'; newStatus: 'ARCHIVED' }>();
    vi.mocked(window.prompt).mockReturnValue('  Nội dung không còn phù hợp  ');
    harness.archiveChecklistTemplate.mockReturnValue(archive.promise);
    harness.fetchAdminChecklists
      .mockResolvedValueOnce(page([checklist({ id: 'to-delete', name: 'Delete me', status: 'DRAFT' })]))
      .mockResolvedValueOnce(page([]));

    render(<ChecklistListPage />);
    const deleteButton = await screen.findByRole('button', { name: 'Xóa checklist Delete me' });
    fireEvent.click(deleteButton);

    expect(harness.archiveChecklistTemplate).toHaveBeenCalledWith(
      'to-delete',
      'Nội dung không còn phù hợp',
    );
    expect((deleteButton as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(deleteButton);
    expect(harness.archiveChecklistTemplate).toHaveBeenCalledTimes(1);

    await act(async () => {
      archive.resolve({ previousStatus: 'DRAFT', newStatus: 'ARCHIVED' });
      await archive.promise;
    });

    await waitFor(() => expect(harness.fetchAdminChecklists).toHaveBeenCalledTimes(2));
    expect(harness.fetchAdminChecklists).toHaveBeenLastCalledWith({
      stage: undefined,
      status: undefined,
      page: 0,
      size: 10,
    });
    expect(screen.queryByText('Delete me')).toBeNull();
  });

  it('reloads the latest filters when they change during an archive request', async () => {
    const archive = deferred<{ previousStatus: 'DRAFT'; newStatus: 'ARCHIVED' }>();
    vi.mocked(window.prompt).mockReturnValue('Lý do hợp lệ');
    harness.archiveChecklistTemplate.mockReturnValue(archive.promise);
    harness.fetchAdminChecklists.mockResolvedValue(page([
      checklist({ id: 'changing-filter', name: 'Changing filter', status: 'DRAFT' }),
    ]));

    render(<ChecklistListPage />);
    fireEvent.click(await screen.findByRole('button', { name: 'Xóa checklist Changing filter' }));
    fireEvent.change(screen.getByLabelText('Lọc checklist theo giai đoạn'), {
      target: { value: 'POSTPARTUM' },
    });
    await waitFor(() => expect(harness.fetchAdminChecklists).toHaveBeenLastCalledWith({
      stage: 'POSTPARTUM',
      status: undefined,
      page: 0,
      size: 10,
    }));

    await act(async () => {
      archive.resolve({ previousStatus: 'DRAFT', newStatus: 'ARCHIVED' });
      await archive.promise;
    });

    await waitFor(() => expect(harness.fetchAdminChecklists).toHaveBeenCalledTimes(3));
    expect(harness.fetchAdminChecklists).toHaveBeenLastCalledWith({
      stage: 'POSTPARTUM',
      status: undefined,
      page: 0,
      size: 10,
    });
  });

  it('keeps the row, reports an archive failure, and allows retry', async () => {
    vi.mocked(window.prompt).mockReturnValue('Lý do hợp lệ');
    harness.archiveChecklistTemplate.mockRejectedValue(new Error('synthetic archive failure'));
    harness.fetchAdminChecklists.mockResolvedValue(page([
      checklist({ id: 'retry-delete', name: 'Retry delete', status: 'REJECTED' }),
    ]));

    render(<ChecklistListPage />);
    const deleteButton = await screen.findByRole('button', { name: 'Xóa checklist Retry delete' });
    fireEvent.click(deleteButton);

    expect((await screen.findByRole('alert')).textContent).toBe(
      'Không thể xóa checklist. Vui lòng thử lại.',
    );
    expect(screen.getByText('Retry delete')).toBeTruthy();
    expect((deleteButton as HTMLButtonElement).disabled).toBe(false);

    fireEvent.click(deleteButton);
    await waitFor(() => expect(harness.archiveChecklistTemplate).toHaveBeenCalledTimes(2));
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

  it('renders exact Vietnamese fallbacks for nullable lifecycle metadata', async () => {
    const nullableRow = {
      ...checklist({ id: 'nullable', name: 'Nullable metadata row' }),
      stage: null,
      updatedAt: null,
    } satisfies AdminChecklistTemplate;
    harness.fetchAdminChecklists.mockResolvedValue(page([nullableRow]));

    render(<ChecklistListPage />);

    expect(await screen.findByText('Nullable metadata row')).toBeTruthy();
    expect(screen.getByText('Không áp dụng')).toBeTruthy();
    expect(screen.getByText('Không có cửa sổ')).toBeTruthy();
  });

  it('renders "Gửi phê duyệt tất cả" button and handles bulk submission upon confirmation', async () => {
    harness.fetchAdminChecklists.mockResolvedValueOnce(page([
      checklist({ id: 'chk-1', name: 'Checklist Nháp 1', status: 'DRAFT' }),
    ]));

    render(<ChecklistListPage />);

    expect(await screen.findByText('Checklist Nháp 1')).toBeTruthy();

    const submitAllBtn = screen.getByRole('button', { name: /Gửi phê duyệt tất cả/i });
    expect(submitAllBtn).toBeTruthy();

    // Mock response when fetching all DRAFT items for modal
    harness.fetchAdminChecklists.mockResolvedValueOnce({
      content: [
        checklist({ id: 'chk-1', name: 'Checklist Nháp 1', status: 'DRAFT' }),
        checklist({ id: 'chk-2', name: 'Checklist Nháp 2', status: 'DRAFT' }),
      ],
      totalElements: 2,
      totalPages: 1,
      number: 0,
      size: 50,
    });

    harness.updateChecklistTemplate.mockResolvedValue(checklist({ id: 'chk-1', status: 'PENDING_REVIEW' }));

    fireEvent.click(submitAllBtn);

    expect(await screen.findByText('Gửi phê duyệt tất cả checklist?')).toBeTruthy();
    expect(await screen.findByText(/Bạn có chắc chắn muốn gửi phê duyệt 2\/2 checklist/)).toBeTruthy();

    const confirmBtn = await screen.findByRole('button', { name: /Gửi phê duyệt \(2 mục\)/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(harness.updateChecklistTemplate).toHaveBeenCalledTimes(2);
      expect(harness.updateChecklistTemplate).toHaveBeenNthCalledWith(
        1,
        'chk-1',
        expect.objectContaining({ status: 'PENDING_REVIEW' }),
      );
      expect(harness.updateChecklistTemplate).toHaveBeenNthCalledWith(
        2,
        'chk-2',
        expect.objectContaining({ status: 'PENDING_REVIEW' }),
      );
    });
  });

  it('allows unchecking items in collapsible dropdown list before submitting', async () => {
    harness.fetchAdminChecklists.mockResolvedValueOnce(page([
      checklist({ id: 'chk-1', name: 'Checklist 1', status: 'DRAFT' }),
    ]));

    render(<ChecklistListPage />);

    expect(await screen.findByText('Checklist 1')).toBeTruthy();

    const submitAllBtn = screen.getByRole('button', { name: /Gửi phê duyệt tất cả/i });

    harness.fetchAdminChecklists.mockResolvedValueOnce({
      content: [
        checklist({ id: 'chk-1', name: 'Checklist 1', status: 'DRAFT' }),
        checklist({ id: 'chk-2', name: 'Checklist 2', status: 'DRAFT' }),
      ],
      totalElements: 2,
      totalPages: 1,
      number: 0,
      size: 50,
    });

    harness.updateChecklistTemplate.mockResolvedValue(checklist({ id: 'chk-1', status: 'PENDING_REVIEW' }));

    fireEvent.click(submitAllBtn);

    expect(await screen.findByText('Gửi phê duyệt tất cả checklist?')).toBeTruthy();

    const expandListBtn = await screen.findByRole('button', { name: /Danh sách checklist bản nháp/i });
    fireEvent.click(expandListBtn);

    expect(await screen.findByText('Đã chọn 2 / 2')).toBeTruthy();

    const checkboxes = screen.getAllByRole('checkbox');
    expect(checkboxes.length).toBe(2);

    // Uncheck item 2
    fireEvent.click(checkboxes[1]);

    const confirmBtn = await screen.findByRole('button', { name: /Gửi phê duyệt \(1 mục\)/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(harness.updateChecklistTemplate).toHaveBeenCalledTimes(1);
      expect(harness.updateChecklistTemplate).toHaveBeenCalledWith(
        'chk-1',
        expect.objectContaining({ status: 'PENDING_REVIEW' }),
      );
    });
  });

  it('handles empty draft list when clicking "Gửi phê duyệt tất cả"', async () => {
    harness.fetchAdminChecklists.mockResolvedValueOnce(page([]));

    render(<ChecklistListPage />);

    await screen.findByText('Không có checklist phù hợp.');

    const submitAllBtn = screen.getByRole('button', { name: /Gửi phê duyệt tất cả/i });

    harness.fetchAdminChecklists.mockResolvedValueOnce({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 50,
    });

    fireEvent.click(submitAllBtn);

    expect(await screen.findByText('Gửi phê duyệt tất cả checklist?')).toBeTruthy();
    expect(await screen.findByText('Không có checklist bản nháp nào cần gửi phê duyệt.')).toBeTruthy();

    const closeBtn = await screen.findByRole('button', { name: 'Đóng' });
    fireEvent.click(closeBtn);

    await waitFor(() => {
      expect(screen.queryByText('Gửi phê duyệt tất cả checklist?')).toBeNull();
    });
  });

  it('normalizes substage to null when submitting PRE_PREGNANCY checklists', async () => {
    harness.fetchAdminChecklists.mockResolvedValueOnce(page([
      checklist({
        id: 'pre-1',
        name: 'Chuẩn bị mang thai 1',
        stage: 'PRE_PREGNANCY',
        status: 'DRAFT',
      }),
    ]));

    render(<ChecklistListPage />);

    expect(await screen.findByText('Chuẩn bị mang thai 1')).toBeTruthy();

    const submitAllBtn = screen.getByRole('button', { name: /Gửi phê duyệt tất cả/i });

    harness.fetchAdminChecklists.mockResolvedValueOnce({
      content: [
        {
          ...checklist({
            id: 'pre-1',
            name: 'Chuẩn bị mang thai 1',
            stage: 'PRE_PREGNANCY',
            status: 'DRAFT',
          }),
          substage: {
            code: 'PRE_PREGNANCY_NONE_DAY_0_0',
            anchor: 'NONE',
            startInclusive: 0,
            endInclusive: 0,
            unit: 'DAY',
          },
        },
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 50,
    });

    harness.updateChecklistTemplate.mockResolvedValue(checklist({ id: 'pre-1', status: 'PENDING_REVIEW' }));

    fireEvent.click(submitAllBtn);

    expect(await screen.findByText('Gửi phê duyệt tất cả checklist?')).toBeTruthy();

    const confirmBtn = await screen.findByRole('button', { name: /Gửi phê duyệt \(1 mục\)/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(harness.updateChecklistTemplate).toHaveBeenCalledWith(
        'pre-1',
        expect.objectContaining({
          stage: 'PRE_PREGNANCY',
          substage: null,
          status: 'PENDING_REVIEW',
        }),
      );
    });
  });
});
