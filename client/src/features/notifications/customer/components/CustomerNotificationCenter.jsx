import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Bell,
  CalendarClock,
  CheckCheck,
  ChevronLeft,
  ChevronRight,
  CircleAlert,
  Film,
  Inbox,
  MapPin,
  RefreshCw,
  Ticket,
  Utensils
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import {
  announceNotificationChange,
  notificationCustomerService
} from '../services/notificationCustomerService';

const PAGE_SIZE = 10;

const formatDateTime = value => {
  if (!value) return 'Đang cập nhật';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(date);
};

const formatCurrency = (value, currency = 'VND') => {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return 'Đang cập nhật';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: currency || 'VND',
    maximumFractionDigits: 0
  }).format(amount);
};

const asList = value => Array.isArray(value)
  ? value.filter(Boolean)
  : (value ? [value] : []);

const isTicketNotification = item => (
  item?.notificationType === 'TICKET_PURCHASED'
  || item?.notificationType === 'TICKET_ISSUED'
);

function TicketDetails({ data }) {
  const seats = asList(data?.seatNames);
  const ticketCodes = asList(data?.ticketCodes);
  const foodItems = asList(data?.foodItems);

  return (
    <div className="mt-4 overflow-hidden rounded-2xl border border-amber-500/15 bg-zinc-950/70">
      <div className="flex gap-4 p-4">
        {data?.moviePosterUrl ? (
          <img
            src={data.moviePosterUrl}
            alt=""
            className="hidden h-28 w-20 shrink-0 rounded-xl object-cover sm:block"
          />
        ) : (
          <div className="hidden h-28 w-20 shrink-0 items-center justify-center rounded-xl bg-zinc-900 text-zinc-700 sm:flex">
            <Film className="h-7 w-7" />
          </div>
        )}

        <div className="min-w-0 flex-1 space-y-3">
          <div className="flex flex-wrap items-start justify-between gap-2">
            <div>
              <p className="text-[10px] font-black uppercase tracking-[0.18em] text-amber-500">
                Vé xem phim
              </p>
              <h4 className="mt-1 text-sm font-black text-white">
                {data?.movieTitle || 'LoraFilm'}
              </h4>
            </div>
            {data?.bookingCode && (
              <span className="rounded-lg border border-zinc-700 bg-zinc-900 px-2.5 py-1 font-mono text-[10px] font-bold text-zinc-300">
                {data.bookingCode}
              </span>
            )}
          </div>

          <div className="grid gap-2 text-[11px] text-zinc-400 md:grid-cols-2">
            <span className="flex items-start gap-2">
              <CalendarClock className="mt-0.5 h-3.5 w-3.5 shrink-0 text-amber-500" />
              {formatDateTime(data?.showtime)}
            </span>
            <span className="flex items-start gap-2">
              <MapPin className="mt-0.5 h-3.5 w-3.5 shrink-0 text-amber-500" />
              {[data?.cinemaName, data?.auditoriumName].filter(Boolean).join(' · ') || 'Đang cập nhật rạp'}
            </span>
            <span className="flex items-start gap-2">
              <Ticket className="mt-0.5 h-3.5 w-3.5 shrink-0 text-amber-500" />
              Ghế {seats.length ? seats.join(', ') : 'đang cập nhật'}
            </span>
            <span className="font-black text-emerald-400">
              {formatCurrency(data?.totalPaid ?? data?.totalAmount, data?.currency)}
            </span>
          </div>

          {foodItems.length > 0 && (
            <div className="flex items-start gap-2 border-t border-zinc-800 pt-2 text-[10px] text-zinc-500">
              <Utensils className="mt-0.5 h-3 w-3 shrink-0" />
              <span>
                {foodItems
                  .map(item => `${item?.name || 'Bắp nước'} ×${item?.quantity || 1}`)
                  .join(', ')}
              </span>
            </div>
          )}

          {ticketCodes.length > 0 && (
            <p className="border-t border-zinc-800 pt-2 font-mono text-[10px] text-zinc-600">
              Mã vé: {ticketCodes.join(' · ')}
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

function NotificationCard({ item, onOpen }) {
  const unread = !item.readAt;
  const ticket = isTicketNotification(item);

  return (
    <button
      type="button"
      onClick={() => onOpen(item)}
      className={`w-full rounded-2xl border p-4 text-left transition-all sm:p-5 ${
        unread
          ? 'border-amber-500/25 bg-amber-500/[0.04] hover:border-amber-500/40'
          : 'border-zinc-800 bg-zinc-950/30 hover:border-zinc-700'
      }`}
    >
      <div className="flex items-start gap-3">
        <span className={`mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl ${
          ticket ? 'bg-amber-500/10 text-amber-500' : 'bg-zinc-800 text-zinc-400'
        }`}>
          {ticket ? <Ticket className="h-4 w-4" /> : <Bell className="h-4 w-4" />}
        </span>
        <div className="min-w-0 flex-1">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h4 className={`text-sm ${unread ? 'font-black text-white' : 'font-bold text-zinc-300'}`}>
                {item.title}
              </h4>
              <p className="mt-1 text-xs leading-5 text-zinc-500">{item.body}</p>
            </div>
            {unread && (
              <span className="mt-1 h-2.5 w-2.5 shrink-0 rounded-full bg-brand-orange" />
            )}
          </div>
          {ticket && <TicketDetails data={item.data || {}} />}
          <div className="mt-3 flex items-center justify-between gap-3 text-[10px] font-bold text-zinc-600">
            <span>{formatDateTime(item.createdAt)}</span>
            {item.actionUrl && (
              <span className="inline-flex items-center gap-1 text-amber-500">
                Xem chi tiết <ChevronRight className="h-3 w-3" />
              </span>
            )}
          </div>
        </div>
      </div>
    </button>
  );
}

export default function CustomerNotificationCenter() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setPageData(await notificationCustomerService.list({ page, size: PAGE_SIZE }));
    } catch (requestError) {
      setError(requestError?.message || 'Không thể tải thông báo lúc này.');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  const items = useMemo(() => pageData?.content || [], [pageData]);
  const unreadOnPage = items.some(item => !item.readAt);

  const markAllRead = async () => {
    try {
      await notificationCustomerService.markAllRead();
      const now = new Date().toISOString();
      setPageData(current => ({
        ...current,
        content: (current?.content || []).map(item => (
          item.readAt ? item : { ...item, readAt: now }
        ))
      }));
      announceNotificationChange();
    } catch (requestError) {
      setError(requestError?.message || 'Không thể đánh dấu đã đọc.');
    }
  };

  const openNotification = async item => {
    if (!item.readAt) {
      try {
        const updated = await notificationCustomerService.markRead(item.publicId);
        setPageData(current => ({
          ...current,
          content: (current?.content || []).map(candidate => (
            candidate.publicId === item.publicId ? { ...candidate, ...updated } : candidate
          ))
        }));
        announceNotificationChange();
      } catch {
        // Opening the related booking is still useful if marking read fails.
      }
    }
    if (item.actionUrl?.startsWith('/')) {
      navigate(item.actionUrl);
    }
  };

  if (loading && !pageData) {
    return (
      <div className="flex min-h-72 items-center justify-center text-zinc-500">
        <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
        <span className="text-xs font-bold">Đang tải thông báo...</span>
      </div>
    );
  }

  if (error && !pageData) {
    return (
      <div className="flex min-h-72 flex-col items-center justify-center text-center">
        <CircleAlert className="h-8 w-8 text-red-400" />
        <p className="mt-3 text-sm font-bold text-zinc-300">{error}</p>
        <button
          type="button"
          onClick={load}
          className="mt-4 rounded-xl bg-zinc-800 px-4 py-2 text-xs font-black text-white hover:bg-zinc-700"
        >
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <section className="space-y-5" aria-label="Hộp thư thông báo">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h3 className="text-sm font-black uppercase tracking-wider text-white">
            Hộp thư thông báo
          </h3>
          <p className="mt-1 text-[10px] text-zinc-500">
            Vé đã mua, cập nhật đặt chỗ, ưu đãi và thông tin tài khoản của bạn.
          </p>
        </div>
        {unreadOnPage && (
          <button
            type="button"
            onClick={markAllRead}
            className="inline-flex items-center gap-2 rounded-xl border border-zinc-800 px-3 py-2 text-[10px] font-black text-zinc-400 hover:border-zinc-700 hover:text-white"
          >
            <CheckCheck className="h-3.5 w-3.5" />
            Đánh dấu tất cả đã đọc
          </button>
        )}
      </div>

      {error && (
        <div className="rounded-xl border border-red-500/20 bg-red-500/5 px-4 py-3 text-xs text-red-300">
          {error}
        </div>
      )}

      {items.length === 0 ? (
        <div className="flex min-h-72 flex-col items-center justify-center rounded-2xl border border-dashed border-zinc-800 text-center">
          <Inbox className="h-9 w-9 text-zinc-700" />
          <p className="mt-3 text-xs font-black text-zinc-400">Chưa có thông báo</p>
          <p className="mt-1 max-w-xs text-[10px] leading-5 text-zinc-600">
            Sau khi thanh toán thành công, thông tin vé sẽ xuất hiện tại đây.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {items.map(item => (
            <NotificationCard key={item.publicId} item={item} onOpen={openNotification} />
          ))}
        </div>
      )}

      {(pageData?.totalPages || 0) > 1 && (
        <div className="flex items-center justify-end gap-2">
          <button
            type="button"
            aria-label="Trang thông báo trước"
            disabled={page <= 0}
            onClick={() => setPage(current => Math.max(0, current - 1))}
            className="rounded-lg border border-zinc-800 p-2 text-zinc-400 disabled:opacity-30"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>
          <span className="px-2 text-[10px] font-bold text-zinc-500">
            {page + 1}/{pageData.totalPages}
          </span>
          <button
            type="button"
            aria-label="Trang thông báo sau"
            disabled={page + 1 >= pageData.totalPages}
            onClick={() => setPage(current => current + 1)}
            className="rounded-lg border border-zinc-800 p-2 text-zinc-400 disabled:opacity-30"
          >
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      )}
    </section>
  );
}
