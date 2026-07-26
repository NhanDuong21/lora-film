import { useEffect, useRef } from 'react';
import { AlertCircle, CheckCircle2, Info, TriangleAlert, X } from 'lucide-react';
import { createPortal } from 'react-dom';

const presentationByVariant = {
  success: {
    Icon: CheckCircle2,
    iconClass: 'bg-emerald-500/10 text-emerald-400',
    buttonClass: 'bg-emerald-500 hover:bg-emerald-600'
  },
  error: {
    Icon: AlertCircle,
    iconClass: 'bg-red-500/10 text-red-400',
    buttonClass: 'bg-red-500 hover:bg-red-600'
  },
  warning: {
    Icon: TriangleAlert,
    iconClass: 'bg-amber-500/10 text-amber-400',
    buttonClass: 'bg-brand-orange hover:bg-orange-600'
  },
  info: {
    Icon: Info,
    iconClass: 'bg-sky-500/10 text-sky-400',
    buttonClass: 'bg-brand-orange hover:bg-orange-600'
  }
};

export default function BookingNoticeModal({
  actionLabel = 'Đã hiểu',
  message,
  onClose,
  title,
  variant = 'info'
}) {
  const actionRef = useRef(null);
  const presentation = presentationByVariant[variant] || presentationByVariant.info;
  const { Icon } = presentation;

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    actionRef.current?.focus();

    const handleKeyDown = event => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [onClose]);

  return createPortal(
    <div
      className="fixed inset-0 z-[80] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
      onMouseDown={event => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="booking-notice-title"
        aria-describedby="booking-notice-message"
        className="w-full max-w-sm rounded-3xl border border-zinc-700 bg-zinc-900 p-6 text-zinc-100 shadow-2xl shadow-black/70"
      >
        <div className="flex items-start justify-between gap-4">
          <div className={`rounded-2xl p-3 ${presentation.iconClass}`}>
            <Icon className="h-6 w-6" />
          </div>
          <button
            type="button"
            aria-label="Đóng"
            onClick={onClose}
            className="rounded-xl p-2 text-zinc-500 transition-colors hover:bg-zinc-800 hover:text-white"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        <h2 id="booking-notice-title" className="mt-4 text-lg font-black text-white">
          {title}
        </h2>
        <p id="booking-notice-message" className="mt-2 text-sm leading-6 text-zinc-400">
          {message}
        </p>
        <button
          ref={actionRef}
          type="button"
          onClick={onClose}
          className={`mt-6 w-full rounded-xl px-4 py-3 text-xs font-black uppercase tracking-wider text-white transition-colors ${presentation.buttonClass}`}
        >
          {actionLabel}
        </button>
      </section>
    </div>,
    document.body
  );
}
