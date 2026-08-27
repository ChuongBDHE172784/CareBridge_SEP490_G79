import { useEffect, useMemo, useRef, useState } from 'react';
import { STAGE_LABELS } from '../models/content';
import { importChecklistTemplatesBatch } from '../services/contentApi';
import {
  parseChecklistImportFile,
  type ParsedChecklistImportGroup,
} from '../utils/checklistImportParser';

interface ChecklistImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

interface ImportSummary {
  successCount: number;
  failCount: number;
  errors: string[];
}

const MAX_IMPORT_FILE_BYTES = 5 * 1024 * 1024;
const MAX_CHECKLISTS_PER_IMPORT = 100;
const FOCUSABLE_SELECTOR = [
  'button:not([disabled])',
  'input:not([disabled])',
  '[href]',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

export default function ChecklistImportModal({ isOpen, onClose, onSuccess }: ChecklistImportModalProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [groups, setGroups] = useState<ParsedChecklistImportGroup[]>([]);
  const [isParsing, setIsParsing] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [importing, setImporting] = useState(false);
  const [fileError, setFileError] = useState('');
  const [summary, setSummary] = useState<ImportSummary | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const dialogRef = useRef<HTMLElement>(null);
  const previousActiveElementRef = useRef<HTMLElement | null>(null);
  const parseGenerationRef = useRef(0);
  const importingRef = useRef(false);

  useEffect(() => {
    if (!isOpen) return;
    previousActiveElementRef.current = document.activeElement instanceof HTMLElement
      ? document.activeElement
      : null;
    setSelectedFile(null);
    setGroups([]);
    setIsParsing(false);
    setIsDragging(false);
    setImporting(false);
    setFileError('');
    setSummary(null);
    parseGenerationRef.current += 1;
    importingRef.current = false;
    const focusTimer = window.setTimeout(() => closeButtonRef.current?.focus(), 0);
    return () => {
      window.clearTimeout(focusTimer);
      parseGenerationRef.current += 1;
      previousActiveElementRef.current?.focus();
    };
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !importing) onClose();
      if (event.key !== 'Tab') return;
      const focusable = Array.from(
        dialogRef.current?.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR) ?? [],
      ).filter((element) => !element.hasAttribute('disabled'));
      if (focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };
    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [importing, isOpen, onClose]);

  const validGroups = useMemo(() => groups.filter((group) => group.isValid && group.template), [groups]);
  const invalidGroups = useMemo(() => groups.filter((group) => !group.isValid), [groups]);
  const exceedsBatchLimit = validGroups.length > MAX_CHECKLISTS_PER_IMPORT;

  if (!isOpen) return null;

  const handleFile = async (file: File | null) => {
    if (!file || importingRef.current) return;
    const parseGeneration = ++parseGenerationRef.current;
    const lowerName = file.name.toLowerCase();
    setSelectedFile(file);
    setGroups([]);
    setSummary(null);
    setFileError('');
    setIsParsing(false);
    if (!lowerName.endsWith('.xlsx') && !lowerName.endsWith('.xls')) {
      setFileError('Chỉ hỗ trợ file Excel định dạng .xlsx hoặc .xls.');
      return;
    }
    if (file.size > MAX_IMPORT_FILE_BYTES) {
      setFileError('File Excel không được vượt quá 5 MB.');
      return;
    }

    setIsParsing(true);
    try {
      const parsed = await parseChecklistImportFile(file);
      if (parseGeneration !== parseGenerationRef.current) return;
      setGroups(parsed);
      if (parsed.length === 0) setFileError('File Excel không có checklist nào để import.');
      else if (parsed.filter((group) => group.isValid && group.template).length > MAX_CHECKLISTS_PER_IMPORT) {
        setFileError(`Mỗi lần chỉ được import tối đa ${MAX_CHECKLISTS_PER_IMPORT} checklist hợp lệ.`);
      }
    } catch (error) {
      if (parseGeneration !== parseGenerationRef.current) return;
      setFileError(error instanceof Error ? error.message : 'Không thể đọc file Excel. Vui lòng kiểm tra lại định dạng.');
    } finally {
      if (parseGeneration === parseGenerationRef.current) setIsParsing(false);
    }
  };

  const handleDownloadTemplate = () => {
    if (importingRef.current) return;
    const link = document.createElement('a');
    link.href = '/Form_Mau_Import_Checklist.xlsx';
    link.download = 'Form_Mau_Import_Checklist.xlsx';
    document.body.appendChild(link);
    link.click();
    link.remove();
  };

  const handleImport = async () => {
    if (validGroups.length === 0 || exceedsBatchLimit || importingRef.current) return;
    importingRef.current = true;
    setImporting(true);
    setSummary(null);
    try {
      const result = await importChecklistTemplatesBatch({
        templates: validGroups.map((group) => ({
          rowIndex: group.rowIndex,
          checklistCode: group.checklistCode,
          template: group.template!,
        })),
      });
      setSummary({ successCount: result.successCount, failCount: result.failedCount, errors: result.errors ?? [] });
      if (result.successCount > 0) onSuccess();
    } catch (error: unknown) {
      const responseMessage = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setSummary({
        successCount: 0,
        failCount: validGroups.length,
        errors: [responseMessage || 'Không thể import checklist. Vui lòng thử lại.'],
      });
    } finally {
      importingRef.current = false;
      setImporting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 font-sans backdrop-blur-sm animate-fade-in">
      <section
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="checklist-import-title"
        aria-describedby="checklist-import-description"
        className="flex max-h-[92vh] w-full max-w-[960px] flex-col overflow-hidden rounded-2xl border border-outline-variant bg-surface p-6 shadow-2xl"
      >
        <header className="flex items-start justify-between gap-4 border-b border-surface-container-highest pb-4">
          <div>
            <h2 id="checklist-import-title" className="m-0 text-xl font-bold text-on-surface">Import danh sách Checklist</h2>
            <p id="checklist-import-description" className="m-0 mt-1 text-xs text-outline">
              Tải file Excel hai sheet, kiểm tra dữ liệu và tạo các checklist hợp lệ ở trạng thái Bản nháp.
            </p>
          </div>
          <button
            ref={closeButtonRef}
            type="button"
            aria-label="Đóng import checklist"
            onClick={onClose}
            disabled={importing}
            className="flex min-h-11 min-w-11 items-center justify-center rounded-full border-0 bg-surface-container text-outline transition-colors hover:bg-surface-container-high focus:outline-none focus:ring-2 focus:ring-primary/40 disabled:opacity-40"
          >
            <span aria-hidden="true" className="material-symbols-outlined text-lg">close</span>
          </button>
        </header>

        <div className="flex flex-1 flex-col gap-5 overflow-y-auto py-4">
          <section className="rounded-2xl border border-primary/20 bg-primary-container/10 p-4">
            <div className="flex flex-col items-start justify-between gap-4 sm:flex-row">
              <div>
                <div className="mb-1 flex items-center gap-2">
                  <span aria-hidden="true" className="material-symbols-outlined text-xl text-primary">description</span>
                  <h3 className="m-0 text-sm font-bold text-on-surface">1. Tải mẫu template quy chuẩn</h3>
                </div>
                <p className="m-0 max-w-[68ch] text-xs leading-5 text-on-surface-variant">
                  File mẫu gồm sheet <strong>Checklists</strong>, <strong>Checklist_Items</strong> và bảng hướng dẫn mã giai đoạn, nhịp lặp, chức năng hỗ trợ.
                </p>
              </div>
              <button
                type="button"
                onClick={handleDownloadTemplate}
                disabled={importing}
                className="inline-flex min-h-11 shrink-0 items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 px-4 py-2.5 text-xs font-semibold text-primary transition-colors hover:bg-primary/20 focus:outline-none focus:ring-2 focus:ring-primary/30"
              >
                <span aria-hidden="true" className="material-symbols-outlined text-base">download</span>
                Tải file mẫu Excel
              </button>
            </div>
          </section>

          <section>
            <h3 className="mb-2 text-sm font-bold text-on-surface">2. Chọn hoặc kéo thả file dữ liệu</h3>
            <div
              role="button"
              tabIndex={importing ? -1 : 0}
              aria-disabled={importing}
              aria-label="Chọn hoặc kéo thả file Excel checklist"
              onClick={() => { if (!importing) fileInputRef.current?.click(); }}
              onKeyDown={(event) => {
                if (!importing && (event.key === 'Enter' || event.key === ' ')) {
                  event.preventDefault();
                  fileInputRef.current?.click();
                }
              }}
              onDragEnter={(event) => { event.preventDefault(); setIsDragging(true); }}
              onDragOver={(event) => { event.preventDefault(); setIsDragging(true); }}
              onDragLeave={(event) => { event.preventDefault(); setIsDragging(false); }}
              onDrop={(event) => {
                event.preventDefault();
                setIsDragging(false);
                if (importing) return;
                void handleFile(event.dataTransfer.files?.[0] ?? null);
              }}
              className={`rounded-2xl border-2 border-dashed p-6 text-center transition-colors focus:outline-none focus:ring-2 focus:ring-primary/30 ${importing ? 'cursor-not-allowed opacity-50' : 'cursor-pointer'} ${isDragging ? 'border-primary bg-primary-container/20' : 'border-outline-variant bg-surface-container-lowest hover:border-primary hover:bg-surface-container-low'}`}
            >
              <input
                ref={fileInputRef}
                type="file"
                accept=".xlsx,.xls"
                aria-label="Chọn file Excel checklist"
                disabled={importing}
                onChange={(event) => void handleFile(event.target.files?.[0] ?? null)}
                className="sr-only"
              />
              <span aria-hidden="true" className="material-symbols-outlined mb-2 text-4xl text-primary">upload_file</span>
              <p className="m-0 text-sm font-semibold text-on-surface">
                {selectedFile ? selectedFile.name : 'Nhấn để chọn file hoặc kéo thả file vào đây'}
              </p>
              <p className="m-0 mt-1 text-xs text-outline">Hỗ trợ .xlsx và .xls; dữ liệu phải có đúng hai sheet bắt buộc.</p>
            </div>
            {fileError && <p role="alert" className="m-0 mt-2 rounded-xl bg-error-container px-3 py-2 text-xs font-semibold text-error">{fileError}</p>}
          </section>

          {isParsing && (
            <div role="status" className="flex items-center justify-center gap-2 py-8 text-sm text-outline">
              <span aria-hidden="true" className="material-symbols-outlined animate-spin text-primary">sync</span>
              Đang đọc và kiểm tra dữ liệu checklist...
            </div>
          )}

          {!isParsing && groups.length > 0 && (
            <section aria-labelledby="checklist-preview-title">
              <div className="mb-3 flex flex-col justify-between gap-2 sm:flex-row sm:items-center">
                <h3 id="checklist-preview-title" className="m-0 text-sm font-bold text-on-surface">3. Xem trước và kiểm tra ({groups.length} checklist)</h3>
                <div className="flex gap-2 text-xs" aria-live="polite">
                  <span className="rounded-full bg-emerald-100 px-3 py-1 font-semibold text-emerald-800">{validGroups.length} hợp lệ</span>
                  {invalidGroups.length > 0 && <span className="rounded-full bg-error-container px-3 py-1 font-semibold text-error">{invalidGroups.length} lỗi</span>}
                </div>
              </div>
              <div className="max-h-[280px] overflow-auto rounded-xl border border-outline-variant bg-surface">
                <table className="w-full border-collapse text-xs">
                  <caption className="sr-only">Kết quả kiểm tra các checklist trong file Excel</caption>
                  <thead className="sticky top-0 border-b border-outline-variant bg-surface-container-low text-left">
                    <tr>
                      <th scope="col" className="p-2.5 text-outline">DÒNG</th>
                      <th scope="col" className="p-2.5 text-outline">MÃ / TÊN CHECKLIST</th>
                      <th scope="col" className="p-2.5 text-outline">GIAI ĐOẠN</th>
                      <th scope="col" className="p-2.5 text-outline">SỐ MỤC</th>
                      <th scope="col" className="p-2.5 text-outline">TRẠNG THÁI</th>
                    </tr>
                  </thead>
                  <tbody>
                    {groups.map((group) => (
                      <tr key={`${group.checklistCode}-${group.rowIndex}`} className="border-b border-surface-container-highest align-top hover:bg-surface-bright">
                        <td className="p-2.5 text-center font-medium text-outline">{group.rowIndex}</td>
                        <td className="p-2.5">
                          <div className="font-mono text-[11px] font-semibold text-primary">{group.checklistCode}</div>
                          <div className="mt-0.5 font-semibold text-on-surface">{group.name || '(Chưa có tên)'}</div>
                        </td>
                        <td className="p-2.5 font-medium text-on-surface-variant">{group.stage ? STAGE_LABELS[group.stage] : '—'}</td>
                        <td className="p-2.5 text-center font-semibold text-on-surface">{group.itemCount}</td>
                        <td className="p-2.5">
                          {group.isValid ? (
                            <span className="inline-flex items-center gap-1 font-semibold text-emerald-700"><span aria-hidden="true" className="material-symbols-outlined text-sm">check_circle</span>Hợp lệ</span>
                          ) : (
                            <ul className="m-0 list-none space-y-1 p-0 text-error">
                              {group.errors.map((error, index) => <li key={`${error}-${index}`} className="flex gap-1"><span aria-hidden="true" className="material-symbols-outlined text-sm">error</span><span>{error}</span></li>)}
                            </ul>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          )}

          {importing && <div role="status" className="rounded-2xl bg-surface-container-low p-4 text-center text-sm font-semibold text-primary">Đang import {validGroups.length} checklist...</div>}

          {summary && (
            <section aria-live="polite" className="rounded-2xl border border-outline-variant bg-surface-container-low p-4">
              <h3 className="m-0 mb-2 text-sm font-bold text-on-surface">Kết quả Import</h3>
              <p className="m-0 text-xs text-on-surface-variant">Thêm thành công: <strong className="text-emerald-700">{summary.successCount} checklist</strong> ở trạng thái Bản nháp.</p>
              {summary.failCount > 0 && <p className="m-0 mt-1 text-xs text-error">Thất bại: <strong>{summary.failCount} checklist</strong>.</p>}
              {summary.errors.length > 0 && <ul className="mt-2 max-h-36 overflow-y-auto rounded-xl bg-error-container p-3 pl-7 text-xs text-error">{summary.errors.map((error, index) => <li key={`${error}-${index}`}>{error}</li>)}</ul>}
            </section>
          )}
        </div>

        <footer className="flex justify-end gap-3 border-t border-surface-container-highest pt-4">
          <button type="button" onClick={onClose} disabled={importing} className="min-h-11 rounded-full border border-outline-variant bg-transparent px-6 py-2.5 text-sm font-semibold text-on-surface hover:bg-surface-container-low focus:outline-none focus:ring-2 focus:ring-primary/30 disabled:opacity-40">
            {summary ? 'Đóng' : 'Hủy bỏ'}
          </button>
          {!summary && (
            <button
              type="button"
              aria-label={`Import ${validGroups.length} checklist hợp lệ`}
              onClick={() => void handleImport()}
              disabled={validGroups.length === 0 || exceedsBatchLimit || importing}
              className="inline-flex min-h-11 items-center gap-2 rounded-full border-0 bg-primary px-6 py-2.5 text-sm font-semibold text-on-primary hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-primary/30 disabled:cursor-not-allowed disabled:opacity-40"
            >
              {importing && <span aria-hidden="true" className="material-symbols-outlined animate-spin text-lg">sync</span>}
              Import {validGroups.length} checklist hợp lệ
            </button>
          )}
        </footer>
      </section>
    </div>
  );
}
