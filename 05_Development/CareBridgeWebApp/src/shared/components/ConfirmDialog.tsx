import { useState } from 'react';

export interface ConfirmDialogProps {
  open: boolean;
  title: string;
  description?: string;
  icon?: string;
  tone?: 'default' | 'danger';
  confirmLabel?: string;
  cancelLabel?: string;
  // When set, the dialog shows a required textarea and passes its value to onConfirm.
  reasonLabel?: string;
  reasonPlaceholder?: string;
  submitting?: boolean;
  errorText?: string;
  onConfirm: (reason?: string) => void;
  onCancel: () => void;
}

// Shared confirm/reason modal — replaces window.confirm()/window.prompt() so moderator
// actions get a consistent, styled confirmation step instead of the native browser dialog.
// Pass a `key` that changes per target/action from the caller so internal textarea state
// resets between confirmations instead of persisting across a fresh open() call.
export default function ConfirmDialog({
  open,
  title,
  description,
  icon = 'help',
  tone = 'default',
  confirmLabel = 'Xác nhận',
  cancelLabel = 'Hủy',
  reasonLabel,
  reasonPlaceholder = 'Nhập lý do...',
  submitting = false,
  errorText,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const [reason, setReason] = useState('');
  const [reasonError, setReasonError] = useState('');

  if (!open) return null;

  const handleConfirm = () => {
    if (reasonLabel !== undefined) {
      if (!reason.trim()) {
        setReasonError('Vui lòng nhập lý do trước khi xác nhận.');
        return;
      }
      onConfirm(reason.trim());
      return;
    }
    onConfirm();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center font-sans">
      <div
        className="absolute inset-0 bg-slate-950/35 backdrop-blur-[3px]"
        onClick={submitting ? undefined : onCancel}
      />
      <div className="relative mx-4 w-full max-w-sm rounded-lg border border-outline-variant/80 bg-surface p-4 shadow-[0_16px_40px_rgba(15,23,42,0.14)]">
        <div className="flex items-start gap-3 mb-2">
          <span
            className={`material-symbols-outlined rounded-md p-1.5 text-xl ${
              tone === 'danger' ? 'bg-error-container text-error' : 'bg-primary-container text-primary'
            }`}
          >
            {icon}
          </span>
          <div className="flex-1 pt-0.5">
            <h2 className="m-0 text-sm font-semibold text-on-surface">{title}</h2>
            {description && <p className="mb-0 mt-1 text-xs leading-relaxed text-on-surface-variant">{description}</p>}
          </div>
        </div>

        {reasonLabel !== undefined && (
          <div className="mt-4">
            <label className="mb-1.5 block text-[10px] font-semibold uppercase tracking-[0.02em] text-outline">
              {reasonLabel}
            </label>
            <textarea
              autoFocus
              value={reason}
              onChange={(e) => {
                setReason(e.target.value);
                if (reasonError) setReasonError('');
              }}
              placeholder={reasonPlaceholder}
              rows={3}
              disabled={submitting}
              className="w-full resize-none rounded-md border border-outline-variant p-2 font-sans text-xs outline-none focus:border-primary"
            />
            {reasonError && <p className="mt-1 text-[11px] text-error">{reasonError}</p>}
          </div>
        )}

        {errorText && <p className="mt-3 text-[11px] text-error">{errorText}</p>}

        <div className="mt-5 flex justify-end gap-2">
          <button
            type="button"
            onClick={onCancel}
            disabled={submitting}
            className="rounded-md border border-outline-variant bg-surface px-3 py-1.5 text-xs font-semibold text-on-surface-variant hover:bg-surface-container-low disabled:opacity-40"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={submitting}
            className={`rounded-md border-0 px-3 py-1.5 text-xs font-semibold disabled:opacity-50 ${
              tone === 'danger' ? 'bg-error text-on-error' : 'bg-primary text-on-primary'
            }`}
          >
            {submitting ? 'Đang xử lý...' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
