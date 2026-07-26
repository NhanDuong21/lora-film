import { useEffect, useRef, useState } from 'react';
import { AlertTriangle, LoaderCircle, TicketX, X } from 'lucide-react';
import { createPortal } from 'react-dom';

export default function BookingCancellationModal({
  bookingCode,
  error,
  pending = false,
  onClose,
  onConfirm
}) {
  const [reason, setReason] = useState('');
  const keepButtonRef = useRef(null);

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    keepButtonRef.current?.focus();

    const handleKeyDown = event => {
      if (event.key === 'Escape' && !pending) onClose();
    };
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [onClose, pending]);

  const handleSubmit = event => {
    event.preventDefault();
    if (!pending) onConfirm(reason.trim());
  };

  return createPortal(
    <div
      className="fixed inset-0 z-[80] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
      onMouseDown={event => {
        if (event.target === event.currentTarget && !pending) onClose();
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="booking-cancel-title"
        aria-describedby="booking-cancel-description"
        className="w-full max-w-md overflow-hidden rounded-3xl border border-red-500/25 bg-zinc-900 text-zinc-100 shadow-2xl shadow-black/70"
      >
        <form onSubmit={handleSubmit}>
          <div className="flex items-start justify-between gap-4 border-b border-zinc-800 px-6 py-5">
            <div className="flex items-start gap-3">
              <div className="rounded-2xl bg-red-500/10 p-3 text-red-400">
                <TicketX className="h-6 w-6" />
              </div>
              <div>
                <h2 id="booking-cancel-title" className="text-lg font-black text-white">
                  Xác nhận hủy giữ ghế
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

          <div className="space-y-5 px-6 py-5">
            <div
              id="booking-cancel-description"
              className="rounded-2xl border border-amber-500/20 bg-amber-500/5 p-4"
            >
              <div className="flex gap-3">
                <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-400" />
                <div>
                  <p className="text-sm font-black text-amber-200">
                    Ghế sẽ được trả lại ngay cho khách hàng khác
                  </p>
                  <ul className="mt-2 list-disc space-y-1 pl-4 text-xs leading-5 text-zinc-400">
                    <li>Bạn không thể tiếp tục thanh toán đơn này.</li>
                    <li>Thao tác hủy không thể hoàn tác.</li>
                    <li>Nếu đổi ý, hãy chọn lại ghế sau khi hủy.</li>
                  </ul>
                </div>
              </div>
            </div>

            <label className="block">
              <span className="text-xs font-black uppercase tracking-wider text-zinc-400">
                Lý do hủy <span className="font-medium normal-case text-zinc-600">(không bắt buộc)</span>
              </span>
              <textarea
                value={reason}
                disabled={pending}
                maxLength={255}
                rows={3}
                onChange={event => setReason(event.target.value)}
                placeholder="Ví dụ: Tôi muốn chọn lại suất chiếu khác"
                className="mt-2 w-full resize-none rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm text-zinc-200 outline-none transition-colors placeholder:text-zinc-600 focus:border-red-400 disabled:opacity-60"
              />
              <span className="mt-1 block text-right text-[10px] text-zinc-600">
                {reason.length}/255
              </span>
            </label>

            {error && (
              <div
                role="alert"
                className="rounded-2xl border border-red-500/25 bg-red-500/10 px-4 py-3 text-sm font-semibold text-red-300"
              >
                {error}
              </div>
            )}
          </div>

          <div className="grid grid-cols-2 gap-3 border-t border-zinc-800 bg-zinc-950/40 px-6 py-4">
            <button
              ref={keepButtonRef}
              type="button"
              disabled={pending}
              onClick={onClose}
              className="rounded-xl border border-zinc-700 px-4 py-3 text-xs font-black uppercase tracking-wider text-zinc-300 transition-colors hover:bg-zinc-800 hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
            >
              Giữ lại đơn
            </button>
            <button
              type="submit"
              disabled={pending}
              className="flex items-center justify-center gap-2 rounded-xl bg-red-500 px-4 py-3 text-xs font-black uppercase tracking-wider text-white transition-colors hover:bg-red-600 disabled:cursor-wait disabled:opacity-70"
            >
              {pending && <LoaderCircle className="h-4 w-4 animate-spin" />}
              {pending ? 'Đang hủy...' : 'Xác nhận hủy'}
            </button>
          </div>
        </form>
      </section>
    </div>,
    document.body
  );
}
