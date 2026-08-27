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
  children?: React.ReactNode;
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
  children,
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
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
        className="relative mx-4 w-full max-w-md rounded-lg border border-outline-variant/80 bg-surface p-5 shadow-[0_18px_48px_rgba(15,23,42,0.18)] sm:p-6"
      >
        <div className="mb-3 flex items-start gap-3.5">
          <span
            className={`material-symbols-outlined rounded-md p-2 text-2xl ${
              tone === 'danger' ? 'bg-error-container text-error' : 'bg-primary-container text-primary'
            }`}
          >
            {icon}
          </span>
          <div className="flex-1 pt-0.5">
            <h2 id="confirm-dialog-title" className="m-0 text-base font-semibold leading-6 text-on-surface">
              {title}
            </h2>
            {description && <p className="mb-0 mt-1.5 text-sm leading-6 text-on-surface-variant">{description}</p>}
          </div>
        </div>

        {reasonLabel !== undefined && (
          <div className="mt-4">
            <label className="mb-2 block text-xs font-semibold uppercase tracking-[0.02em] text-outline">
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
              rows={4}
              disabled={submitting}
              className="w-full resize-none rounded-md border border-outline-variant p-3 font-sans text-sm leading-6 text-on-surface outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 disabled:bg-surface-container-low disabled:opacity-70"
            />
            {reasonError && <p className="mt-2 text-sm leading-5 text-error">{reasonError}</p>}
          </div>
        )}

        {children}

        {errorText && <p className="mt-3 text-sm leading-5 text-error">{errorText}</p>}


        <div className="mt-6 flex flex-wrap justify-end gap-2.5">
          <button
            type="button"
            onClick={onCancel}
            disabled={submitting}
            className="min-h-10 rounded-md border border-outline-variant bg-surface px-4 py-2 text-sm font-semibold text-on-surface-variant transition hover:bg-surface-container-low focus:outline-none focus:ring-2 focus:ring-primary/20 disabled:opacity-40"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={submitting}
            className={`min-h-10 rounded-md border-0 px-4 py-2 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-primary/25 disabled:opacity-50 ${
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
