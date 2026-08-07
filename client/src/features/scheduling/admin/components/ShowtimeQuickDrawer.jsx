import { useEffect, useRef, useState } from 'react';
import { ArrowUpRight, X } from 'lucide-react';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';
import {
  formatCinemaDateTime,
  formatCinemaTime,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewDateTime';
import { getShowtimeStatusPresentation } from '@/features/scheduling/admin/utils/schedulingPresentation';

const PRICING_STATUS_LABELS = {
  COMPLETE: 'Đã đủ giá',
  READY: 'Đã đủ giá',
  INCOMPLETE: 'Chưa đủ giá',
  MISSING: 'Chưa có giá',
  AMBIGUOUS: 'Có mức giá bị trùng',
};

const getPricingStatusPresentation = (pricing, fallbackStatus) => {
  const status = pricing?.status || fallbackStatus;
  if (status && PRICING_STATUS_LABELS[status]) {
    return {
      label: PRICING_STATUS_LABELS[status],
      tone: status === 'COMPLETE' || status === 'READY' ? 'text-emerald-300' : 'text-amber-300',
    };
  }
  if (pricing?.complete === true) return { label: 'Đã đủ giá', tone: 'text-emerald-300' };
  if ((pricing?.ambiguousSeatTypes || []).length > 0) {
    return { label: 'Có mức giá bị trùng', tone: 'text-amber-300' };
  }
  if ((pricing?.missingSeatTypes || []).length > 0) {
    return { label: 'Chưa có giá đầy đủ', tone: 'text-amber-300' };
  }
  return { label: 'Chưa có thông tin', tone: 'text-zinc-300' };
};

const addMinutes = (instant, minutes) => {
  const value = new Date(instant);
  if (!Number.isFinite(value.getTime())) return instant;
  return new Date(value.getTime() + (Number(minutes || 0) * 60_000)).toISOString();
};

export default function ShowtimeQuickDrawer({ showtime, onClose, onViewDetail }) {
  const closeRef = useRef(null);
  const [pricingState, setPricingState] = useState({ data: null, isLoading: false, hasError: false });

  useEffect(() => {
    if (!showtime) return undefined;
    const frame = requestAnimationFrame(() => closeRef.current?.focus());
    const onKeyDown = event => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    return () => {
      cancelAnimationFrame(frame);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [onClose, showtime]);

  useEffect(() => {
    if (!showtime) return undefined;
    let active = true;
    if (showtime.pricingStatus) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- an enriched list item does not need another request.
      setPricingState({ data: { status: showtime.pricingStatus }, isLoading: false, hasError: false });
      return undefined;
    }

    setPricingState({ data: null, isLoading: true, hasError: false });
    adminShowtimeService.getPricing(showtime.showtimePublicId)
      .then(response => {
        if (!active) return;
        setPricingState({
          data: response?.success ? response.data : null,
          isLoading: false,
          hasError: !response?.success,
        });
      })
      .catch(() => {
        if (active) setPricingState({ data: null, isLoading: false, hasError: true });
      });

    return () => {
      active = false;
    };
  }, [showtime]);

  if (!showtime) return null;
  const timezone = showtime.cinema?.timezone;
  const cleaningMinutes = Number(showtime.auditorium?.cleaningBufferMinutes || 0);
  const occupancyEndTime = addMinutes(showtime.endTime, cleaningMinutes);
  const statusLabel = getShowtimeStatusPresentation(showtime.status).label;
  const pricingPresentation = getPricingStatusPresentation(pricingState.data, showtime.pricingStatus);
  const pricingLabel = pricingState.isLoading
    ? 'Đang kiểm tra…'
    : pricingState.hasError
      ? 'Không thể kiểm tra'
      : pricingPresentation.label;

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/65 backdrop-blur-sm" onMouseDown={event => event.target === event.currentTarget && onClose()}>
      <aside role="dialog" aria-modal="true" aria-labelledby="showtime-quick-title" className="flex h-full w-full max-w-md flex-col border-l border-zinc-800 bg-zinc-950 shadow-2xl">
        <header className="flex items-start justify-between gap-4 border-b border-zinc-800 p-5">
          <div>
            <p className="text-[10px] font-black uppercase tracking-[0.2em] text-brand-orange">Suất chiếu vận hành</p>
            <h2 id="showtime-quick-title" className="mt-1 text-xl font-black text-white">{showtime.movie?.title || 'Phim chưa xác định'}</h2>
            <p className="mt-1 text-sm text-zinc-400">{showtime.movieVersion?.versionName || showtime.movieVersion?.format || 'Chưa có phiên bản'}</p>
          </div>
          <button ref={closeRef} type="button" onClick={onClose} aria-label="Đóng xem nhanh suất chiếu" className="rounded-xl p-2 text-zinc-400 hover:bg-zinc-800 hover:text-white">
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </header>
        <div className="flex-1 space-y-5 overflow-y-auto p-5">
          <div className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-4">
            <p className="text-3xl font-black text-white">
              {formatCinemaTime(showtime.startTime, timezone)}
              <span className="mx-2 text-zinc-600">–</span>
              {formatCinemaTime(showtime.endTime, timezone)}
            </p>
            <p className="mt-2 text-sm text-zinc-400">{formatCinemaDateTime(showtime.startTime, timezone)}</p>
          </div>
          <dl className="divide-y divide-zinc-800 rounded-2xl border border-zinc-800 px-4">
            {[
              ['Rạp', showtime.cinema?.name],
              ['Phòng', showtime.auditorium?.name],
              ['Trạng thái', statusLabel],
              ['Dọn phòng', `${cleaningMinutes} phút · sẵn sàng lúc ${formatCinemaTime(occupancyEndTime, timezone)}`],
            ].map(([label, value]) => (
              <div key={label} className="grid grid-cols-[110px_1fr] gap-3 py-3 text-sm">
                <dt className="font-bold text-zinc-500">{label}</dt>
                <dd className="text-zinc-200">{value || '—'}</dd>
              </div>
            ))}
            <div className="grid grid-cols-[110px_1fr] gap-3 py-3 text-sm">
              <dt className="font-bold text-zinc-500">Tình trạng giá</dt>
              <dd className={`font-bold ${pricingState.isLoading || pricingState.hasError ? 'text-zinc-300' : pricingPresentation.tone}`}>
                {pricingLabel}
              </dd>
            </div>
          </dl>
        </div>
        <footer className="border-t border-zinc-800 p-5">
          <button type="button" onClick={() => onViewDetail(showtime.showtimePublicId)} className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-brand-orange px-4 text-sm font-black text-zinc-950">
            Mở trang chỉnh sửa đầy đủ <ArrowUpRight className="h-4 w-4" aria-hidden="true" />
          </button>
        </footer>
      </aside>
    </div>
  );
}
