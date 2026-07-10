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
        className="absolute inset-0 bg-[rgba(39,24,18,0.4)] backdrop-blur-[4px]"
        onClick={submitting ? undefined : onCancel}
      />
      <div className="relative w-full max-w-md mx-4 bg-surface rounded-3xl shadow-[0_8px_40px_rgba(90,70,63,0.25)] p-6">
        <div className="flex items-start gap-3 mb-2">
          <span
            className={`material-symbols-outlined text-2xl rounded-full p-2 ${
              tone === 'danger' ? 'bg-error-container text-error' : 'bg-primary-container text-on-primary-container'
            }`}
          >
            {icon}
          </span>
          <div className="flex-1 pt-1">
            <h2 className="text-base font-bold text-on-surface m-0">{title}</h2>
            {description && <p className="text-sm text-on-surface-variant mt-1 mb-0">{description}</p>}
          </div>
        </div>

        {reasonLabel !== undefined && (
          <div className="mt-4">
            <label className="block text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-2">
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
              className="w-full text-sm border border-outline-variant rounded-2xl p-3 resize-none outline-none font-sans focus:border-primary"
            />
            {reasonError && <p className="text-error text-xs mt-1.5">{reasonError}</p>}
          </div>
        )}

        {errorText && <p className="text-error text-xs mt-3">{errorText}</p>}

        <div className="flex justify-end gap-2 mt-6">
          <button
            type="button"
            onClick={onCancel}
            disabled={submitting}
            className="px-4 py-2.5 rounded-2xl bg-transparent border border-outline-variant text-on-surface text-sm font-semibold disabled:opacity-40"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={submitting}
            className={`px-4 py-2.5 rounded-2xl border-0 text-sm font-semibold disabled:opacity-50 ${
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
