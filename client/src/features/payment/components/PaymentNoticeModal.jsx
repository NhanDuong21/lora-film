import { AlertTriangle, CheckCircle2, Info, X } from 'lucide-react';

const icons = {
  success: <CheckCircle2 className="h-6 w-6 text-emerald-400" />,
  danger: <AlertTriangle className="h-6 w-6 text-red-400" />,
  info: <Info className="h-6 w-6 text-amber-400" />,
};

export default function PaymentNoticeModal({
  open,
  title = 'Thông báo',
  message,
  tone = 'info',
  confirmLabel = 'Đã hiểu',
  cancelLabel,
  busy = false,
  onConfirm,
  onClose,
}) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
      <div className="w-full max-w-md overflow-hidden rounded-3xl border border-zinc-700 bg-zinc-900 shadow-2xl">
        <div className="flex items-start gap-4 border-b border-zinc-800 p-6">
          <div className="rounded-2xl bg-zinc-800 p-3">{icons[tone] || icons.info}</div>
          <div className="min-w-0 flex-1">
            <h2 className="text-lg font-black text-white">{title}</h2>
            <p className="mt-2 whitespace-pre-line text-sm leading-6 text-zinc-400">{message}</p>
          </div>
          <button type="button" onClick={onClose} className="text-zinc-500 hover:text-white" aria-label="Đóng">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="flex justify-end gap-3 p-5">
          {cancelLabel && (
            <button type="button" disabled={busy} onClick={onClose}
              className="rounded-xl border border-zinc-700 px-5 py-2.5 text-xs font-black uppercase text-zinc-300 hover:bg-zinc-800">
              {cancelLabel}
            </button>
          )}
          <button type="button" disabled={busy} onClick={onConfirm || onClose}
            className="rounded-xl bg-brand-orange px-5 py-2.5 text-xs font-black uppercase text-white disabled:opacity-50">
            {busy ? 'Đang xử lý...' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
