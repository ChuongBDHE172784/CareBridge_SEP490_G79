import { useState, useEffect, useRef } from 'react';
import type { CommunityTopic, ContentType } from '../models/content';
import { STAGE_LABELS, TYPE_LABELS } from '../models/content';
import { fetchTopics, importContentBatch } from '../services/contentApi';
import {
  generateContentTemplate,
  parseImportFile,
  type ParsedImportRow,
} from '../utils/contentImportParser';
import '../richContentBody.css';

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
  const [importResults, setImportResults] = useState<{
    successCount: number;
    failCount: number;
    errors: string[];
  } | null>(null);

  // Row selected for Rich Content HTML preview modal
  const [previewRow, setPreviewRow] = useState<ParsedImportRow | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const resetState = () => {
    setSelectedFile(null);
    setParsedRows([]);
    setIsParsing(false);
    setImporting(false);
    setImportResults(null);
    setPreviewRow(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  useEffect(() => {
    if (isOpen) {
      fetchTopics()
        .then(setTopics)
        .catch(() => setTopics([]));
      resetState();
    }
  }, [isOpen]);


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

  const handleFileChange = async (file: File | null) => {
    if (!file) return;
    setSelectedFile(file);
    setIsParsing(true);
    setImportResults(null);

    try {
      const rows = await parseImportFile(file, topics);
      setParsedRows(rows);
    } catch {
      setParsedRows([]);
    } finally {
      setIsParsing(false);
    }
  };

  const validRows = parsedRows.filter((r) => r.isValid);
  const invalidRows = parsedRows.filter((r) => !r.isValid);

  const handleRunImport = async () => {
    if (validRows.length === 0 || importing) return;

    setImporting(true);
    setImportResults(null);

    try {
      const payloadItems = validRows.map((r) => ({
        rowIndex: r.rowIndex,
        title: r.title,
        body: r.body,
        summary: r.summary,
        stage: r.stage,
        categoryName: r.topicName,
        topicId: r.topicId,
        sourceLabel: r.sourceLabel,
        sourceUrl: r.sourceUrl,
        sourcePublisher: r.sourcePublisher,
      }));

      const res = await importContentBatch({
        type,
        items: payloadItems,
      });

      setImportResults({
        successCount: res.successCount,
        failCount: res.failedCount,
        errors: res.errors || [],
      });

      if (res.successCount > 0) {
        onSuccess();
      }
    } catch (err: any) {
      setImportResults({
        successCount: 0,
        failCount: validRows.length,
        errors: [err?.response?.data?.message || 'Lỗi hệ thống khi import dữ liệu.'],
      });
    } finally {
      setImporting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 font-sans backdrop-blur-sm animate-fade-in">
      <div className="bg-surface rounded-2xl p-6 shadow-2xl w-full max-w-[900px] max-h-[92vh] flex flex-col overflow-hidden border border-outline-variant">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-surface-container-highest">
          <div>
            <h2 className="text-xl font-bold text-on-surface m-0">Import danh sách {typeLabel}</h2>
            <p className="text-xs text-outline mt-1 m-0">
              Tải file dữ liệu (.CSV, .TXT, .XLSX, .XLS) hỗ trợ Rich HTML Content tiếng Việt.
            </p>
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
                  <h3 className="text-sm font-bold text-on-surface m-0">
                    1. Tải mẫu template quy chuẩn (.CSV / Excel)
                  </h3>
                </div>
                <p className="text-xs text-on-surface-variant m-0">
                  File mẫu bao gồm các cột: <strong className="text-primary">tiêu_đề (*)</strong>,{' '}
                  <strong className="text-primary">giai_đoạn (*)</strong> (Chuẩn bị mang thai: PRE_PREGNANCY / Thai kỳ: PREGNANCY / Hậu sản: POSTPARTUM / Chăm bé: BABY_CARE),{' '}
                  <strong className="text-primary">nội_dung (*)</strong> (Hỗ trợ Rich HTML: h1-h4, bold, italic, danh sách, bảng, trích dẫn, link), tóm_tắt, danh_mục, tên_nguồn, link_nguồn, nhà_xuất_bản.
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
              <p className="text-xs text-outline mt-1 m-0">
                Hỗ trợ định dạng file: <strong>.csv, .txt, .xlsx, .xls</strong> (Chuẩn mã hóa UTF-8 tiếng Việt)
              </p>
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
                <h3 className="text-sm font-bold text-on-surface m-0">
                  3. Xem trước và kiểm tra tính hợp lệ ({parsedRows.length} hàng)
                </h3>
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

              <div className="max-h-[250px] overflow-y-auto rounded-xl border border-outline-variant bg-surface">
                <table className="w-full border-collapse text-xs">
                  <thead className="bg-surface-container-low sticky top-0 border-b border-outline-variant text-left">
                    <tr>
                      <th className="p-2.5 w-10 text-center text-outline">STT</th>
                      <th className="p-2.5 text-outline">TIÊU ĐỀ (*)</th>
                      <th className="p-2.5 text-outline w-28">GIAI ĐOẠN (*)</th>
                      <th className="p-2.5 text-outline w-32">DANH MỤC</th>
                      <th className="p-2.5 text-outline w-32">TRẠNG THÁI</th>
                      <th className="p-2.5 text-center text-outline w-28">XEM TRƯỚC</th>
                    </tr>
                  </thead>
                  <tbody>
                    {parsedRows.map((row) => (
                      <tr
                        key={row.rowIndex}
                        className="border-b border-surface-container-highest hover:bg-surface-bright"
                      >
                        <td className="p-2.5 text-center text-outline font-medium">{row.rowIndex}</td>
                        <td className="p-2.5 max-w-[240px] truncate font-semibold text-on-surface" title={row.title}>
                          {row.title || <span className="text-error italic">(Trống)</span>}
                        </td>
                        <td className="p-2.5 font-medium text-[12px]">{row.stage ? (STAGE_LABELS[row.stage] || row.stage) : <span className="text-error italic">(Trống)</span>}</td>
                        <td className="p-2.5 text-on-surface-variant">{row.topicName || '—'}</td>
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
                        <td className="p-2.5 text-center">
                          <button
                            type="button"
                            onClick={() => setPreviewRow(row)}
                            className="py-1 px-2.5 rounded-full bg-primary/10 hover:bg-primary/20 text-primary font-semibold border border-primary/30 cursor-pointer text-[11px] inline-flex items-center gap-1"
                          >
                            <span className="material-symbols-outlined text-xs">visibility</span>
                            Xem nội dung
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Import Progress Indicator */}
          {importing && (
            <div className="rounded-2xl bg-surface-container-low p-4 text-center">
              <p className="text-sm font-semibold text-primary mb-2 flex items-center justify-center gap-2 m-0">
                <span className="material-symbols-outlined animate-spin">sync</span>
                Đang tiến hành import {validRows.length} bài viết vào hệ thống...
              </p>
            </div>
          )}

          {/* Import Results Box */}
          {importResults && (
            <div className="rounded-2xl border p-4 bg-surface-container-low">
              <h4 className="text-sm font-bold text-on-surface m-0 mb-2">Kết quả Import:</h4>
              <p className="text-xs text-on-surface-variant m-0 mb-1">
                - Thêm thành công: <strong className="text-[#137333]">{importResults.successCount}</strong> bài viết/FAQ dạng Bản nháp (Draft).
              </p>
              {importResults.failCount > 0 && (
                <p className="text-xs text-error m-0 mb-2">
                  - Thất bại: <strong>{importResults.failCount}</strong> hàng.
                </p>
              )}
              {importResults.errors.length > 0 && (
                <div className="mt-2 p-2.5 rounded-xl bg-error-container text-error text-xs max-h-36 overflow-y-auto font-mono">
                  {importResults.errors.map((err, idx) => (
                    <div key={idx} className="mb-0.5">{err}</div>
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

      {/* Nested Modal: Rich Content HTML Preview */}
      {previewRow && (
        <div className="fixed inset-0 z-60 flex items-center justify-center bg-black/60 p-4 font-sans backdrop-blur-sm animate-fade-in">
          <div className="bg-surface rounded-2xl p-6 shadow-2xl w-full max-w-[750px] max-h-[85vh] flex flex-col overflow-hidden border border-outline-variant">
            <div className="flex items-center justify-between pb-3 border-b border-surface-container-highest">
              <div>
                <span className="text-[11px] font-bold text-outline uppercase tracking-wider">
                  Dòng {previewRow.rowIndex} • Xem trước nội dung bài viết
                </span>
                <h3 className="text-lg font-bold text-on-surface m-0 mt-0.5">{previewRow.title || '(Chưa có tiêu đề)'}</h3>
              </div>
              <button
                type="button"
                onClick={() => setPreviewRow(null)}
                className="w-8 h-8 rounded-full border-0 bg-surface-container hover:bg-surface-container-high cursor-pointer flex items-center justify-center text-outline"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            {/* Preview Metadata */}
            <div className="flex gap-4 py-2.5 px-3 my-3 bg-surface-container-low rounded-xl text-xs flex-wrap">
              <div>
                <span className="text-outline">Giai đoạn: </span>
                <strong className="text-on-surface">{previewRow.stage ? (STAGE_LABELS[previewRow.stage] || previewRow.stage) : '—'}</strong>
              </div>
              <div>
                <span className="text-outline">Danh mục: </span>
                <strong className="text-on-surface">{previewRow.topicName || '—'}</strong>
              </div>
              {previewRow.sourceLabel && (
                <div>
                  <span className="text-outline">Tên nguồn / Tác giả: </span>
                  <strong className="text-on-surface">{previewRow.sourceLabel}</strong>
                </div>
              )}
              {previewRow.sourceUrl && (
                <div>
                  <span className="text-outline">Liên kết nguồn: </span>
                  <a
                    href={previewRow.sourceUrl.startsWith('http') ? previewRow.sourceUrl : `https://${previewRow.sourceUrl}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-primary underline font-medium"
                  >
                    {previewRow.sourceUrl}
                  </a>
                </div>
              )}
              {previewRow.sourcePublisher && (
                <div>
                  <span className="text-outline">Đơn vị xuất bản: </span>
                  <strong className="text-on-surface">{previewRow.sourcePublisher}</strong>
                </div>
              )}
            </div>

            {/* Rich Content Body Render */}
            <div className="flex-1 overflow-y-auto p-4 rounded-xl border border-outline-variant bg-surface">
              {previewRow.body ? (
                <div
                  className="rich-content-body"
                  dangerouslySetInnerHTML={{ __html: previewRow.body }}
                />
              ) : (
                <div className="text-error italic text-sm py-4 text-center">Nội dung bài viết trống.</div>
              )}
            </div>

            <div className="pt-3 border-t border-surface-container-highest flex justify-end">
              <button
                type="button"
                onClick={() => setPreviewRow(null)}
                className="py-2 px-5 rounded-full bg-primary text-on-primary text-xs font-semibold cursor-pointer border-0"
              >
                Đóng xem trước
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
