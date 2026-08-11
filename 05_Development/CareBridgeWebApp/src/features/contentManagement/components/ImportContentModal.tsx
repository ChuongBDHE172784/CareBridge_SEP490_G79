import { useState, useEffect, useRef } from 'react';
import type { CommunityTopic, ContentType } from '../models/content';
import { TYPE_LABELS } from '../models/content';
import { createContent, fetchTopics } from '../services/contentApi';
import { generateContentTemplate, parseContentCsv, type ParsedImportRow } from '../utils/csvImportUtils';

interface ImportContentModalProps {
  type: ContentType;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export default function ImportContentModal({
  type,
  isOpen,
  onClose,
  onSuccess,
}: ImportContentModalProps) {
  const [topics, setTopics] = useState<CommunityTopic[]>([]);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [parsedRows, setParsedRows] = useState<ParsedImportRow[]>([]);
  const [isParsing, setIsParsing] = useState(false);
  const [importing, setImporting] = useState(false);
  const [progressCount, setProgressCount] = useState(0);
  const [importResults, setImportResults] = useState<{ successCount: number; failCount: number; errors: string[] } | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen) {
      fetchTopics()
        .then(setTopics)
        .catch(() => setTopics([]));
      resetState();
    }
  }, [isOpen]);

  const resetState = () => {
    setSelectedFile(null);
    setParsedRows([]);
    setIsParsing(false);
    setImporting(false);
    setProgressCount(0);
    setImportResults(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  if (!isOpen) return null;

  const typeLabel = TYPE_LABELS[type];

  const handleDownloadTemplate = () => {
    const csvContent = generateContentTemplate(type);
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `Form_Mau_Import_${type === 'ARTICLE' ? 'Bai_Viet' : 'FAQ'}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const handleFileChange = (file: File | null) => {
    if (!file) return;
    setSelectedFile(file);
    setIsParsing(true);
    setImportResults(null);

    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const text = e.target?.result as string;
        const rows = parseContentCsv(text, topics);
        setParsedRows(rows);
      } catch {
        setParsedRows([]);
      } finally {
        setIsParsing(false);
      }
    };
    reader.onerror = () => {
      setParsedRows([]);
      setIsParsing(false);
    };
    reader.readAsText(file, 'UTF-8');
  };

  const validRows = parsedRows.filter((r) => r.isValid);
  const invalidRows = parsedRows.filter((r) => !r.isValid);

  const handleRunImport = async () => {
    if (validRows.length === 0 || importing) return;

    setImporting(true);
    setProgressCount(0);
    let successCount = 0;
    let failCount = 0;
    const errors: string[] = [];

    for (let i = 0; i < validRows.length; i++) {
      const row = validRows[i];
      try {
        await createContent({
          type,
          title: row.title,
          body: row.body,
          summary: row.summary,
          stage: row.stage as any,
          topicId: row.topicId,
          sources: row.sourceLabel
            ? [{ title: row.sourceLabel, url: row.sourceUrl, publisher: row.sourcePublisher }]
            : undefined,
        });
        successCount++;
      } catch (err: any) {
        failCount++;
        errors.push(`Hàng ${row.rowIndex} ("${row.title}"): ${err?.response?.data?.message || 'Lỗi hệ thống'}`);
      }
      setProgressCount(i + 1);
    }

    setImporting(false);
    setImportResults({ successCount, failCount, errors });
    if (successCount > 0) {
      onSuccess();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 font-sans backdrop-blur-sm animate-fade-in">
      <div className="bg-surface rounded-2xl p-6 shadow-2xl w-full max-w-[850px] max-h-[90vh] flex flex-col overflow-hidden border border-outline-variant">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-surface-container-highest">
          <div>
            <h2 className="text-xl font-bold text-on-surface m-0">Import danh sách {typeLabel}</h2>
            <p className="text-xs text-outline mt-1">Tải file CSV/Excel lên để thêm nhiều {typeLabel.toLowerCase()} cùng lúc.</p>
          </div>
          <button
            onClick={onClose}
            disabled={importing}
            className="w-8 h-8 rounded-full border-0 bg-surface-container hover:bg-surface-container-high cursor-pointer flex items-center justify-center text-outline"
          >
            <span className="material-symbols-outlined text-lg">close</span>
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto py-4 flex flex-col gap-5">
          {/* Step 1: Download Template */}
          <div className="rounded-2xl border border-primary/20 bg-primary-container/10 p-4">
            <div className="flex items-start justify-between gap-4">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="material-symbols-outlined text-primary text-xl">description</span>
                  <h3 className="text-sm font-bold text-on-surface m-0">1. Tải mẫu template quy chuẩn (.CSV/Excel)</h3>
                </div>
                <p className="text-xs text-on-surface-variant m-0">
                  File mẫu bao gồm các cột chuẩn: <strong className="text-primary">tiêu_đề (*)</strong>, <strong className="text-primary">giai_đoạn (*)</strong> (PRE_PREGNANCY / PREGNANCY / POSTPARTUM), <strong className="text-primary">nội_dung (*)</strong>, tóm_tắt, danh_mục, tên_nguồn, link_nguồn, nhà_xuất_bản. Các cột đánh dấu (*) là bắt buộc, các cột còn lại có thể để trống.
                </p>
              </div>
              <button
                type="button"
                onClick={handleDownloadTemplate}
                className="inline-flex items-center gap-1.5 py-2.5 px-4 rounded-full bg-primary/10 hover:bg-primary/20 text-primary text-xs font-semibold border border-primary/30 cursor-pointer whitespace-nowrap"
              >
                <span className="material-symbols-outlined text-base">download</span>
                Tải file mẫu
              </button>
            </div>
          </div>

          {/* Step 2: Upload File Dropzone */}
          <div>
            <h3 className="text-sm font-bold text-on-surface mb-2">2. Chọn hoặc kéo thả file dữ liệu</h3>
            <div
              onClick={() => fileInputRef.current?.click()}
              className="border-2 border-dashed border-outline-variant hover:border-primary rounded-2xl p-6 text-center bg-surface-container-lowest hover:bg-surface-container-low cursor-pointer transition-colors"
            >
              <input
                ref={fileInputRef}
                type="file"
                accept=".csv, .txt, .xlsx, .xls"
                onChange={(e) => handleFileChange(e.target.files?.[0] || null)}
                className="hidden"
              />
              <span className="material-symbols-outlined text-primary text-4xl mb-2">upload_file</span>
              <p className="text-sm font-semibold text-on-surface m-0">
                {selectedFile ? selectedFile.name : 'Nhấn để chọn file hoặc kéo thả file vào đây'}
              </p>
              <p className="text-xs text-outline mt-1 m-0">Hỗ trợ các định dạng file: .csv, .txt, .xlsx, .xls (Hỗ trợ font UTF-8 tiếng Việt)</p>
            </div>
          </div>

          {/* Step 3: Parsing & Preview Table */}
          {isParsing && (
            <div className="py-8 text-center text-outline text-sm flex items-center justify-center gap-2">
              <span className="material-symbols-outlined animate-spin text-primary">sync</span>
              Đang đọc và kiểm tra dữ liệu file...
            </div>
          )}

          {!isParsing && parsedRows.length > 0 && (
            <div>
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-bold text-on-surface m-0">3. Xem trước và kiểm tra tính hợp lệ ({parsedRows.length} hàng)</h3>
                <div className="flex gap-2 text-xs">
                  <span className="py-1 px-3 rounded-full bg-[#E6F4EA] text-[#137333] font-semibold">
                    {validRows.length} hợp lệ
                  </span>
                  {invalidRows.length > 0 && (
                    <span className="py-1 px-3 rounded-full bg-error-container text-error font-semibold">
                      {invalidRows.length} lỗi
                    </span>
                  )}
                </div>
              </div>

              <div className="max-h-[220px] overflow-y-auto rounded-xl border border-outline-variant bg-surface">
                <table className="w-full border-collapse text-xs">
                  <thead className="bg-surface-container-low sticky top-0 border-b border-outline-variant text-left">
                    <tr>
                      <th className="p-2.5 w-12 text-center text-outline">STT</th>
                      <th className="p-2.5 text-outline">TIÊU ĐỀ (*)</th>
                      <th className="p-2.5 text-outline">GIAI ĐOẠN (*)</th>
                      <th className="p-2.5 text-outline">DANH MỤC</th>
                      <th className="p-2.5 text-outline">TRẠNG THÁI</th>
                    </tr>
                  </thead>
                  <tbody>
                    {parsedRows.map((row) => (
                      <tr key={row.rowIndex} className="border-b border-surface-container-highest hover:bg-surface-bright">
                        <td className="p-2.5 text-center text-outline font-medium">{row.rowIndex}</td>
                        <td className="p-2.5 max-w-[260px] truncate font-semibold text-on-surface" title={row.title}>
                          {row.title || <span className="text-error italic">(Trống)</span>}
                        </td>
                        <td className="p-2.5 font-mono">{row.stage || <span className="text-error italic">(Trống)</span>}</td>
                        <td className="p-2.5 text-on-surface-variant">{row.topicName || row.topicId || '—'}</td>
                        <td className="p-2.5">
                          {row.isValid ? (
                            <span className="text-[#137333] font-medium flex items-center gap-1">
                              <span className="material-symbols-outlined text-sm">check_circle</span> Hợp lệ
                            </span>
                          ) : (
                            <div className="text-error flex flex-col gap-0.5">
                              {row.errors.map((err, idx) => (
                                <span key={idx} className="flex items-center gap-1">
                                  <span className="material-symbols-outlined text-xs">error</span> {err}
                                </span>
                              ))}
                            </div>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Import Execution Progress & Results */}
          {importing && (
            <div className="rounded-2xl bg-surface-container-low p-4 text-center">
              <p className="text-sm font-semibold text-primary mb-2">
                Đang tiến hành import dữ liệu ({progressCount} / {validRows.length})...
              </p>
              <div className="w-full bg-surface-container-high rounded-full h-2 overflow-hidden">
                <div
                  className="bg-primary h-2 transition-all duration-200"
                  style={{ width: `${(progressCount / validRows.length) * 100}%` }}
                />
              </div>
            </div>
          )}

          {importResults && (
            <div className="rounded-2xl border p-4 bg-surface-container-low">
              <h4 className="text-sm font-bold text-on-surface m-0 mb-2">Kết quả Import:</h4>
              <p className="text-xs text-on-surface-variant m-0 mb-1">
                - Đã thêm thành công: <strong className="text-[#137333]">{importResults.successCount}</strong> bài viết/FAQ dạng Bản nháp (Draft).
              </p>
              {importResults.failCount > 0 && (
                <p className="text-xs text-error m-0 mb-2">
                  - Thất bại: <strong>{importResults.failCount}</strong> hàng.
                </p>
              )}
              {importResults.errors.length > 0 && (
                <div className="mt-2 p-2 rounded-xl bg-error-container text-error text-xs max-h-32 overflow-y-auto font-mono">
                  {importResults.errors.map((err, idx) => (
                    <div key={idx}>{err}</div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="pt-4 border-t border-surface-container-highest flex justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={importing}
            className="py-2.5 px-6 rounded-full border border-outline-variant bg-transparent text-on-surface text-sm font-semibold cursor-pointer"
          >
            {importResults ? 'Đóng' : 'Hủy bỏ'}
          </button>
          {!importResults && (
            <button
              type="button"
              onClick={handleRunImport}
              disabled={validRows.length === 0 || importing}
              className="py-2.5 px-6 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-2"
            >
              {importing && <span className="material-symbols-outlined animate-spin text-lg">sync</span>}
              Import {validRows.length} hàng hợp lệ
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
