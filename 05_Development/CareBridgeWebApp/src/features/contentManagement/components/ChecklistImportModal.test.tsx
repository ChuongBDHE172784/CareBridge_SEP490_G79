// @vitest-environment jsdom

import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const harness = vi.hoisted(() => ({
  parseChecklistImportFile: vi.fn(),
  importChecklistTemplatesBatch: vi.fn(),
}));

vi.mock('../utils/checklistImportParser', () => ({
  parseChecklistImportFile: harness.parseChecklistImportFile,
}));
vi.mock('../services/contentApi', () => ({
  importChecklistTemplatesBatch: harness.importChecklistTemplatesBatch,
}));

import ChecklistImportModal from './ChecklistImportModal';

describe('ChecklistImportModal', () => {
  beforeEach(() => {
    harness.parseChecklistImportFile.mockReset();
    harness.importChecklistTemplatesBatch.mockReset();
  });

  afterEach(cleanup);

  it('downloads the canonical public checklist workbook', () => {
    render(<ChecklistImportModal isOpen onClose={vi.fn()} onSuccess={vi.fn()} />);
    const anchor = document.createElement('a');
    const clickSpy = vi.spyOn(anchor, 'click').mockImplementation(() => {});
    const createElementSpy = vi.spyOn(document, 'createElement').mockReturnValueOnce(anchor);

    fireEvent.click(screen.getByRole('button', { name: 'Tải file mẫu Excel' }));

    expect(anchor.getAttribute('href')).toBe('/Form_Mau_Import_Checklist.xlsx');
    expect(anchor.download).toBe('Form_Mau_Import_Checklist.xlsx');
    expect(clickSpy).toHaveBeenCalledTimes(1);
    createElementSpy.mockRestore();
  });

  it('previews valid and invalid groups and imports only valid templates', async () => {
    const template = {
      name: 'Checklist hợp lệ', templateType: 'MANDATORY', checklistContractVersion: 2,
      recipientRoles: ['MOTHER'], stage: 'PREGNANCY', substage: null,
      items: [{ itemText: 'Mục một', order: 1, isRequired: true, targetSubject: null }],
    };
    harness.parseChecklistImportFile.mockResolvedValue([
      { rowIndex: 2, checklistCode: 'OK-01', name: 'Checklist hợp lệ', stage: 'PREGNANCY', itemCount: 1, isValid: true, errors: [], template },
      { rowIndex: 3, checklistCode: 'BAD-01', name: 'Checklist lỗi', stage: '', itemCount: 2, isValid: false, errors: ['Dòng 3: Giai đoạn không hợp lệ.'] },
    ]);
    harness.importChecklistTemplatesBatch.mockResolvedValue({
      totalRows: 1, successCount: 1, failedCount: 0, errors: [], createdIds: ['created-1'],
    });
    const onSuccess = vi.fn();
    render(<ChecklistImportModal isOpen onClose={vi.fn()} onSuccess={onSuccess} />);

    fireEvent.change(screen.getByLabelText('Chọn file Excel checklist'), {
      target: { files: [new File(['bytes'], 'checklists.xlsx')] },
    });

    expect(await screen.findByText('Checklist hợp lệ')).toBeTruthy();
    expect(screen.getByText('Checklist lỗi')).toBeTruthy();
    expect(screen.getByText('Dòng 3: Giai đoạn không hợp lệ.')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Import 1 checklist hợp lệ' }));

    await waitFor(() => expect(harness.importChecklistTemplatesBatch).toHaveBeenCalledWith({
      templates: [{ rowIndex: 2, checklistCode: 'OK-01', template }],
    }));
    const resultHeading = await screen.findByText('Kết quả Import');
    expect(resultHeading.closest('section')?.textContent).toContain('Thêm thành công: 1 checklist');
    expect(onSuccess).toHaveBeenCalledTimes(1);
  });

  it('supports drag and drop and exposes a file-level parsing error', async () => {
    harness.parseChecklistImportFile.mockRejectedValue(new Error('Thiếu sheet bắt buộc "Checklist_Items".'));
    render(<ChecklistImportModal isOpen onClose={vi.fn()} onSuccess={vi.fn()} />);

    const dropzone = screen.getByRole('button', { name: 'Chọn hoặc kéo thả file Excel checklist' });
    fireEvent.drop(dropzone, { dataTransfer: { files: [new File(['bytes'], 'bad.xlsx')] } });

    expect((await screen.findByRole('alert')).textContent).toContain('Thiếu sheet bắt buộc "Checklist_Items".');
  });

  it('rejects oversized files before parsing', async () => {
    render(<ChecklistImportModal isOpen onClose={vi.fn()} onSuccess={vi.fn()} />);
    const oversizedFile = new File(['bytes'], 'too-large.xlsx');
    Object.defineProperty(oversizedFile, 'size', { value: 5 * 1024 * 1024 + 1 });

    fireEvent.change(screen.getByLabelText('Chọn file Excel checklist'), {
      target: { files: [oversizedFile] },
    });

    expect((await screen.findByRole('alert')).textContent).toContain('không được vượt quá 5 MB');
    expect(harness.parseChecklistImportFile).not.toHaveBeenCalled();
  });

  it('ignores a stale parse result after a newer file has been selected', async () => {
    const firstGroups = [
      { rowIndex: 2, checklistCode: 'FILE-A', name: 'Checklist file A', stage: 'PREGNANCY', itemCount: 1, isValid: false, errors: ['File A lỗi'] },
    ];
    const secondGroups = [
      { rowIndex: 2, checklistCode: 'FILE-B', name: 'Checklist file B', stage: 'PREGNANCY', itemCount: 1, isValid: false, errors: ['File B lỗi'] },
    ];
    let resolveFirst!: (groups: typeof firstGroups) => void;
    const firstParse = new Promise<typeof firstGroups>((resolve) => { resolveFirst = resolve; });
    harness.parseChecklistImportFile
      .mockReturnValueOnce(firstParse)
      .mockResolvedValueOnce(secondGroups);
    render(<ChecklistImportModal isOpen onClose={vi.fn()} onSuccess={vi.fn()} />);
    const input = screen.getByLabelText('Chọn file Excel checklist');

    fireEvent.change(input, { target: { files: [new File(['a'], 'file-a.xlsx')] } });
    fireEvent.change(input, { target: { files: [new File(['b'], 'file-b.xlsx')] } });

    expect(await screen.findByText('Checklist file B')).toBeTruthy();
    await act(async () => { resolveFirst(firstGroups); });
    await waitFor(() => expect(screen.queryByText('Checklist file A')).toBeNull());
    expect(screen.getByText('file-b.xlsx')).toBeTruthy();
  });

  it('locks file replacement while an import request is in flight', async () => {
    const template = {
      name: 'Checklist đang import', templateType: 'MANDATORY', checklistContractVersion: 2,
      recipientRoles: ['MOTHER'], stage: 'PREGNANCY', substage: null,
      items: [{ itemText: 'Mục một', order: 1, isRequired: true, targetSubject: null }],
    };
    harness.parseChecklistImportFile.mockResolvedValue([
      { rowIndex: 2, checklistCode: 'LOCK-01', name: 'Checklist đang import', stage: 'PREGNANCY', itemCount: 1, isValid: true, errors: [], template },
    ]);
    const response = { totalRows: 1, successCount: 1, failedCount: 0, errors: [], createdIds: ['created-1'] };
    let resolveImport!: (result: typeof response) => void;
    harness.importChecklistTemplatesBatch.mockReturnValue(
      new Promise<typeof response>((resolve) => { resolveImport = resolve; }),
    );
    render(<ChecklistImportModal isOpen onClose={vi.fn()} onSuccess={vi.fn()} />);
    const input = screen.getByLabelText('Chọn file Excel checklist');
    fireEvent.change(input, { target: { files: [new File(['a'], 'file-a.xlsx')] } });
    await screen.findByText('Checklist đang import');

    fireEvent.click(screen.getByRole('button', { name: 'Import 1 checklist hợp lệ' }));
    await waitFor(() => expect((input as HTMLInputElement).disabled).toBe(true));
    const dropzone = screen.getByRole('button', { name: 'Chọn hoặc kéo thả file Excel checklist', hidden: true });
    expect(dropzone.getAttribute('aria-disabled')).toBe('true');
    fireEvent.drop(dropzone, { dataTransfer: { files: [new File(['b'], 'file-b.xlsx')] } });
    expect(harness.parseChecklistImportFile).toHaveBeenCalledTimes(1);
    expect(screen.getByText('file-a.xlsx')).toBeTruthy();

    await act(async () => { resolveImport(response); });
  });

  it('blocks a file containing more than 100 valid checklists', async () => {
    const template = {
      name: 'Checklist', templateType: 'MANDATORY', checklistContractVersion: 2,
      recipientRoles: ['MOTHER'], stage: 'PREGNANCY', substage: null,
      items: [{ itemText: 'Mục một', order: 1, isRequired: true, targetSubject: null }],
    };
    harness.parseChecklistImportFile.mockResolvedValue(Array.from({ length: 101 }, (_, index) => ({
      rowIndex: index + 2,
      checklistCode: `LIMIT-${index + 1}`,
      name: `Checklist ${index + 1}`,
      stage: 'PREGNANCY',
      itemCount: 1,
      isValid: true,
      errors: [],
      template,
    })));
    render(<ChecklistImportModal isOpen onClose={vi.fn()} onSuccess={vi.fn()} />);

    fireEvent.change(screen.getByLabelText('Chọn file Excel checklist'), {
      target: { files: [new File(['bytes'], 'too-many.xlsx')] },
    });

    expect((await screen.findByRole('alert')).textContent).toContain('tối đa 100 checklist hợp lệ');
    expect((screen.getByRole('button', { name: 'Import 101 checklist hợp lệ' }) as HTMLButtonElement).disabled).toBe(true);
  });
});
