import { useEffect, useRef } from 'react';
import { Clock3, LoaderCircle, RotateCcw, TicketCheck, X } from 'lucide-react';
import { createPortal } from 'react-dom';

export default function ActiveBookingConflictModal({
  bookingCode,
  seatNames,
  timeLeft,
  error,
  pending = false,
  onClose,
  onResume,
  onCancel
}) {
  const resumeButtonRef = useRef(null);
  const onCloseRef = useRef(onClose);

  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    resumeButtonRef.current?.focus();

    const handleKeyDown = event => {
      if (event.key === 'Escape' && !pending) onCloseRef.current();
    };
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [pending]);

  return createPortal(
    <div
      className="fixed inset-0 z-[85] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
      onMouseDown={event => {
        if (event.target === event.currentTarget && !pending) onClose();
      }}
    >
      <section
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="active-booking-conflict-title"
        aria-describedby="active-booking-conflict-description"
        className="w-full max-w-lg overflow-hidden rounded-3xl border border-brand-orange/30 bg-zinc-900 text-zinc-100 shadow-2xl shadow-black/70"
      >
        <div className="flex items-start justify-between gap-4 border-b border-zinc-800 px-6 py-5">
          <div className="flex items-start gap-3">
            <div className="rounded-2xl bg-brand-orange/10 p-3 text-brand-orange">
              <Clock3 className="h-6 w-6" />
            </div>
            <div>
              <h2 id="active-booking-conflict-title" className="text-lg font-black text-white">
                Bạn đã có đơn giữ ghế cho suất chiếu này
              </h2>
              <p className="mt-1 text-xs font-semibold text-zinc-500">
                {bookingCode ? `Đơn ${bookingCode}` : 'Đơn đang chờ thanh toán'}
              </p>
            </div>
          </div>
          <button
            type="button"
            aria-label="Đóng"
            disabled={pending}
            onClick={onClose}
            className="rounded-xl p-2 text-zinc-500 transition-colors hover:bg-zinc-800 hover:text-white disabled:cursor-not-allowed disabled:opacity-40"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="space-y-4 px-6 py-5">
          <p id="active-booking-conflict-description" className="text-sm leading-6 text-zinc-300">
            Mỗi khách chỉ có thể giữ một đơn trên cùng suất chiếu. Bạn có thể tiếp tục
            thanh toán đơn hiện tại hoặc hủy đơn đó để chọn lại ghế.
          </p>

          <dl className="grid grid-cols-2 gap-3">
            <div className="rounded-2xl border border-white/10 bg-black/20 p-4">
              <dt className="text-[10px] font-black uppercase tracking-wider text-zinc-500">
                Ghế đang giữ
              </dt>
              <dd className="mt-1 text-sm font-black text-white">{seatNames || 'Đang cập nhật'}</dd>
            </div>
            <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 p-4">
              <dt className="text-[10px] font-black uppercase tracking-wider text-amber-300/70">
                Thời gian còn lại
              </dt>
              <dd className="mt-1 text-lg font-black tracking-wider text-amber-300">
                {timeLeft || '--:--'}
              </dd>
            </div>
          </dl>

          {error && (
            <div
              role="alert"
              className="rounded-2xl border border-red-500/25 bg-red-500/10 px-4 py-3 text-sm font-semibold text-red-300"
            >
              {error}
            </div>
          )}
        </div>

        <div className="grid gap-3 border-t border-zinc-800 bg-zinc-950/40 px-6 py-4 sm:grid-cols-2">
          <button
            type="button"
            disabled={pending}
            onClick={onCancel}
            className="flex items-center justify-center gap-2 rounded-xl border border-red-500/35 px-4 py-3 text-xs font-black uppercase tracking-wider text-red-300 transition-colors hover:bg-red-500/10 disabled:cursor-wait disabled:opacity-50"
          >
            {pending
              ? <LoaderCircle className="h-4 w-4 animate-spin" />
              : <RotateCcw className="h-4 w-4" />}
            {pending ? 'Đang hủy đơn cũ...' : 'Hủy đơn cũ để chọn lại'}
          </button>
          <button
            ref={resumeButtonRef}
            type="button"
            disabled={pending}
            onClick={onResume}
            className="flex items-center justify-center gap-2 rounded-xl bg-brand-orange px-4 py-3 text-xs font-black uppercase tracking-wider text-white transition-colors hover:bg-orange-600 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <TicketCheck className="h-4 w-4" />
            Tiếp tục thanh toán
          </button>
        </div>
      </section>
    </div>,
    document.body
  );
}
