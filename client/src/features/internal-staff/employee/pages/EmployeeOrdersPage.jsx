import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  CalendarDays, Eye, LoaderCircle, Printer, RefreshCw, Search, Ticket,
  WalletCards, XCircle,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { cancelBooking, getBookingHistory } from '@/features/booking/customer/services/bookingService';
import { bookingStatus, dateTime, foodNames, money, seatCount } from '../employeePresentation';

const localDate = (date = new Date()) => new Date(
  date.getTime() - date.getTimezoneOffset() * 60_000,
).toISOString().slice(0, 10);

const STATUS_OPTIONS = [
  ['', 'Tất cả trạng thái'],
  ['PENDING_PAYMENT', 'Chờ thanh toán'],
  ['CONFIRMED', 'Đã thanh toán'],
  ['COMPLETED', 'Đã hoàn thành'],
  ['CANCELLED', 'Đã hủy'],
  ['EXPIRED', 'Hết thời gian giữ ghế'],
  ['REFUNDED', 'Đã hoàn tiền'],
];

export default function EmployeeOrdersPage() {
  const navigate = useNavigate();
  const [fromDate, setFromDate] = useState(localDate());
  const [toDate, setToDate] = useState(localDate());
  const [status, setStatus] = useState('');
  const [keyword, setKeyword] = useState('');
  const [orders, setOrders] = useState([]);
  const [state, setState] = useState({ loading: true, error: '' });
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelling, setCancelling] = useState(false);

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const result = await getBookingHistory({
        page: 0, size: 100, fromDate, toDate, status: status || undefined,
      });
      setOrders(result?.content || []);
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({
        loading: false,
        error: error?.message || 'Chưa thể tải danh sách đơn tại quầy. Vui lòng thử lại.',
      });
    }
  }, [fromDate, status, toDate]);

  useEffect(() => {
    load();
  }, [load]);

  const visibleOrders = useMemo(() => {
    const normalized = keyword.trim().toLocaleLowerCase('vi-VN');
    if (!normalized) return orders;
    return orders.filter(order => [
      order.bookingCode,
      order.movieTitle,
      order.seatNames,
      order.cinemaName,
      order.counterCustomerName,
      order.counterCustomerPhone,
      order.counterCustomerEmail,
    ].some(value => String(value || '').toLocaleLowerCase('vi-VN').includes(normalized)));
  }, [keyword, orders]);

  const summary = useMemo(() => orders.reduce((result, order) => {
    result.orders += 1;
    result.tickets += seatCount(order);
    if (['CONFIRMED', 'COMPLETED'].includes(order.status)) {
      result.revenue += Number(order.totalAmount || 0);
    }
    if (order.status === 'PENDING_PAYMENT') result.pending += 1;
    return result;
  }, { orders: 0, tickets: 0, revenue: 0, pending: 0 }), [orders]);

  const confirmCancel = async () => {
    if (!cancelTarget) return;
    setCancelling(true);
    try {
      await cancelBooking(cancelTarget.publicId, 'Khách không tiếp tục thanh toán tại quầy');
      setCancelTarget(null);
      await load();
    } catch (error) {
      setState(current => ({
        ...current,
        error: error?.message || 'Không thể hủy đơn. Vui lòng kiểm tra lại trạng thái đơn.',
      }));
    } finally {
      setCancelling(false);
    }
  };

  return (
    <section className="mx-auto w-full max-w-7xl space-y-6 text-white">
      <header className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-6 md:p-8">
        <p className="text-xs font-black uppercase tracking-[0.22em] text-amber-500">Theo dõi sau bán</p>
        <div className="mt-2 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h1 className="text-3xl font-black">Đơn tại quầy</h1>
            <p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">
              Tra cứu các đơn bạn đã tạo, tiếp tục đơn chờ thanh toán và in lại vé cho khách.
            </p>
          </div>
          <button type="button" onClick={() => navigate('/employee/box-office')} className="rounded-xl bg-amber-500 px-5 py-3 text-sm font-black text-black">
            Bán vé mới
          </button>
        </div>
      </header>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[
          ['Đơn trong khoảng đã chọn', summary.orders, Ticket, 'text-sky-300'],
          ['Số vé', summary.tickets, Printer, 'text-violet-300'],
          ['Doanh thu đã thu', money(summary.revenue), WalletCards, 'text-emerald-300'],
          ['Đơn chờ thanh toán', summary.pending, CalendarDays, 'text-amber-300'],
        ].map(([label, value, Icon, tone]) => (
          <article key={label} className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-5">
            <div className="flex items-center justify-between"><p className="text-xs font-bold text-zinc-500">{label}</p><Icon size={18} className={tone} /></div>
            <p className={`mt-3 text-2xl font-black ${tone}`}>{value}</p>
          </article>
        ))}
      </div>

      <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/50">
        <div className="grid gap-3 border-b border-zinc-800 p-4 lg:grid-cols-[1.5fr_.7fr_.7fr_1fr_auto]">
          <label className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" size={18} />
            <input value={keyword} onChange={event => setKeyword(event.target.value)} placeholder="Tìm mã đơn, phim, ghế hoặc khách" className="h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 pl-10 pr-3 text-sm outline-none focus:border-amber-500" />
          </label>
          <input aria-label="Từ ngày" type="date" value={fromDate} onChange={event => setFromDate(event.target.value)} className="h-11 rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm" />
          <input aria-label="Đến ngày" type="date" value={toDate} onChange={event => setToDate(event.target.value)} className="h-11 rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm" />
          <select aria-label="Trạng thái đơn" value={status} onChange={event => setStatus(event.target.value)} className="h-11 rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm">
            {STATUS_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
          <button type="button" onClick={load} disabled={state.loading} className="flex h-11 items-center justify-center gap-2 rounded-xl border border-zinc-700 px-4 text-sm font-black disabled:opacity-50">
            <RefreshCw size={16} className={state.loading ? 'animate-spin' : ''} /> Làm mới
          </button>
        </div>

        {state.error ? <p className="m-4 rounded-xl border border-red-500/25 bg-red-500/10 p-4 text-sm text-red-300">{state.error}</p> : null}
        {state.loading ? (
          <div className="flex items-center justify-center gap-3 py-20 text-zinc-500"><LoaderCircle className="animate-spin" /> Đang tải đơn tại quầy…</div>
        ) : visibleOrders.length ? (
          <div className="divide-y divide-zinc-800">
            {visibleOrders.map(order => {
              const statusView = bookingStatus(order.status);
              return (
                <article key={order.publicId} className="grid gap-4 p-5 transition hover:bg-zinc-900 lg:grid-cols-[1.2fr_1fr_.75fr_auto] lg:items-center">
                  <div>
                    <div className="flex flex-wrap items-center gap-2"><p className="font-black text-zinc-100">{order.bookingCode}</p><span className={`rounded-full border px-2.5 py-1 text-[11px] font-black ${statusView.tone}`}>{statusView.label}</span></div>
                    <p className="mt-2 text-sm font-bold text-zinc-300">{order.movieTitle || 'Đơn xem phim'}</p>
                    <p className="mt-1 text-xs text-zinc-500">{order.counterCustomerName || 'Khách lẻ'} · Tạo lúc {dateTime(order.createdAt)}</p>
                  </div>
                  <div className="text-sm"><p className="font-bold">{order.seatNames || 'Chưa có thông tin ghế'}</p><p className="mt-1 text-xs text-zinc-500">{order.cinemaName || 'Rạp đang làm việc'} · {seatCount(order)} vé</p></div>
                  <div><p className="text-lg font-black text-amber-300">{money(order.totalAmount, order.currency)}</p>{order.foodNames ? <p className="mt-1 line-clamp-1 text-xs text-zinc-500">{foodNames(order.foodNames)}</p> : null}</div>
                  <div className="flex flex-wrap gap-2 lg:justify-end">
                    {order.status === 'PENDING_PAYMENT' ? <button type="button" onClick={() => navigate(`/employee/payments/cash?reference=${encodeURIComponent(order.bookingCode)}`)} className="rounded-xl bg-emerald-500 px-3 py-2 text-xs font-black text-black">Tiếp tục thu tiền</button> : null}
                    <button type="button" onClick={() => navigate(`/employee/orders/${order.publicId}`)} className="flex items-center gap-1.5 rounded-xl border border-zinc-700 px-3 py-2 text-xs font-black"><Eye size={14} /> Xem chi tiết</button>
                    {order.status === 'PENDING_PAYMENT' ? <button type="button" onClick={() => setCancelTarget(order)} className="flex items-center gap-1.5 rounded-xl border border-red-500/30 px-3 py-2 text-xs font-black text-red-300"><XCircle size={14} /> Hủy đơn</button> : null}
                  </div>
                </article>
              );
            })}
          </div>
        ) : (
          <div className="py-20 text-center"><Ticket className="mx-auto text-zinc-700" size={42} /><p className="mt-4 font-black text-zinc-400">Không có đơn phù hợp</p><p className="mt-1 text-sm text-zinc-600">Thử đổi ngày, trạng thái hoặc từ khóa tìm kiếm.</p></div>
        )}
      </section>

      {cancelTarget ? (
        <div className="fixed inset-0 z-50 grid place-items-center bg-black/80 p-4">
          <div className="w-full max-w-md rounded-3xl border border-red-500/30 bg-zinc-900 p-6 shadow-2xl">
            <h2 className="text-xl font-black">Hủy đơn đang giữ ghế?</h2>
            <p className="mt-2 text-sm leading-6 text-zinc-400">Đơn <strong className="text-zinc-100">{cancelTarget.bookingCode}</strong> chưa thanh toán. Khi hủy, ghế sẽ được trả lại để bán cho khách khác.</p>
            <div className="mt-6 flex justify-end gap-2"><button type="button" onClick={() => setCancelTarget(null)} className="rounded-xl border border-zinc-700 px-4 py-2.5 text-sm font-black">Quay lại</button><button type="button" disabled={cancelling} onClick={confirmCancel} className="rounded-xl bg-red-500 px-4 py-2.5 text-sm font-black text-white disabled:opacity-50">{cancelling ? 'Đang hủy…' : 'Xác nhận hủy đơn'}</button></div>
          </div>
        </div>
      ) : null}
    </section>
  );
}
