import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import {
  AlertTriangle,
  Ban,
  CheckCircle2,
  Clock3,
  Eye,
  RefreshCw,
  Search,
  Ticket,
  TimerReset,
} from 'lucide-react';
import { EmptyWorkspace, HrHero } from '../../admin/components/HrWorkspace';
import {
  ActionModal,
  ConsolePagination,
  ConsolePanel,
  MetricStrip,
} from '../../admin/components/OperationsConsole';
import managerOperationsService from '../services/managerOperationsService';

const BOOKING_STATUS = {
  PENDING_PAYMENT: 'Đang giữ ghế',
  CONFIRMED: 'Đã xác nhận',
  COMPLETED: 'Đã hoàn tất',
  CANCELLED: 'Đã hủy',
  EXPIRED: 'Hết hạn giữ ghế',
  REFUNDED: 'Đã hoàn tiền',
};

const PAYMENT_STATUS = {
  PENDING: 'Đang thanh toán',
  PROCESSING: 'Đang xử lý thanh toán',
  SUCCESS: 'Đã thanh toán',
  FAILED: 'Thanh toán lỗi',
  REFUNDED: 'Đã hoàn tiền',
  PARTIALLY_REFUNDED: 'Hoàn tiền một phần',
};

const ATTENTION = {
  EXPIRING_SOON: 'Sắp hết hạn giữ ghế',
  OVERDUE: 'Đã quá hạn',
  PAYMENT_FAILED: 'Thanh toán lỗi',
};

const formatMoney = (value, currency = 'VND') => new Intl.NumberFormat('vi-VN', {
  style: 'currency', currency: currency || 'VND', maximumFractionDigits: 0,
}).format(Number(value || 0));

const formatDateTime = value => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
  : 'Chưa có';

const localizeAuditoriumName = value => {
  if (!value) return 'Chưa rõ phòng';
  return String(value)
    .replace(/\bScreen\b/gi, 'Phòng')
    .replace(/\bStandard\b/gi, 'Tiêu chuẩn')
    .replace(/\bDeluxe\b/gi, 'Cao cấp');
};

const statusTone = status => {
  if (['CONFIRMED', 'COMPLETED', 'SUCCESS'].includes(status)) return 'border-emerald-500/20 bg-emerald-500/10 text-emerald-300';
  if (['CANCELLED', 'EXPIRED', 'FAILED'].includes(status)) return 'border-red-500/20 bg-red-500/10 text-red-300';
  if (status === 'PENDING_PAYMENT' || status === 'PENDING' || status === 'UNPAID') return 'border-amber-500/20 bg-amber-500/10 text-amber-300';
  return 'border-white/10 bg-white/5 text-zinc-300';
};

const StatusPill = ({ status, kind = 'booking' }) => (
  <span className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-black ${statusTone(status)}`}>
    {(kind === 'payment' ? PAYMENT_STATUS : BOOKING_STATUS)[status] || status || 'Chưa xác định'}
  </span>
);

const emptyPage = { data: [], pageNo: 0, totalPages: 0, totalElements: 0 };

export default function ManagerBookingsPage() {
  const navigate = useNavigate();
  const { selectedCinema, selectedCinemaId, cinemaState } = useOutletContext();
  const [filters, setFilters] = useState({ bookingCode: '', status: '', attention: '', page: 0, size: 20 });
  const [draftQuery, setDraftQuery] = useState('');
  const [pageData, setPageData] = useState(emptyPage);
  const [summary, setSummary] = useState({});
  const [state, setState] = useState({ loading: false, error: '' });
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelReason, setCancelReason] = useState('');
  const [actionState, setActionState] = useState({ loading: false, error: '', success: '' });

  const load = useCallback(async () => {
    if (!selectedCinemaId) return;
    setState({ loading: true, error: '' });
    try {
      const params = Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== ''));
      const [bookings, counts] = await Promise.all([
        managerOperationsService.getBookings(selectedCinemaId, params),
        managerOperationsService.getBookingSummary(selectedCinemaId),
      ]);
      setPageData(bookings || emptyPage);
      setSummary(counts || {});
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.response?.data?.message || 'Không thể tải đơn đặt vé của rạp lúc này.' });
    }
  }, [filters, selectedCinemaId]);

  useEffect(() => {
    // Fetch whenever the selected cinema or operator filters change.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const rows = useMemo(() => pageData?.data || pageData?.content || [], [pageData]);

  const submitSearch = event => {
    event.preventDefault();
    setFilters(value => ({ ...value, bookingCode: draftQuery.trim(), page: 0 }));
  };

  const cancelHold = async event => {
    event.preventDefault();
    if (cancelReason.trim().length < 5) {
      setActionState({ loading: false, success: '', error: 'Vui lòng nhập lý do ít nhất 5 ký tự.' });
      return;
    }
    setActionState({ loading: true, error: '', success: '' });
    try {
      await managerOperationsService.cancelBookingHold(selectedCinemaId, cancelTarget.publicId, cancelReason.trim());
      setCancelTarget(null);
      setCancelReason('');
      setActionState({ loading: false, error: '', success: 'Đã hủy lượt giữ ghế chưa thanh toán và trả ghế về kho.' });
      await load();
    } catch (error) {
      setActionState({ loading: false, success: '', error: error?.response?.data?.message || 'Không thể hủy lượt giữ ghế.' });
    }
  };

  if (cinemaState.loading) return <p className="py-24 text-center text-sm font-bold text-zinc-500">Đang tải phạm vi rạp được phân công…</p>;
  if (!selectedCinema) return <EmptyWorkspace title="Chưa được phân công rạp" description="Quản trị viên cần phân công rạp trước khi bạn có thể xử lý đơn đặt vé." />;

  return (
    <main className="space-y-5 pb-8 text-white">
      <HrHero
        context={`Vận hành tại rạp · ${selectedCinema.name}`}
        title="Đơn đặt vé & giữ ghế"
        description="Theo dõi đơn và lượt giữ ghế của đúng rạp đang phụ trách. Quản lý rạp chỉ hủy lượt giữ ghế chưa thanh toán; các đơn đã thu tiền được xử lý qua quy trình hoàn tiền riêng."
        actions={<button type="button" onClick={load} disabled={state.loading} className="inline-flex items-center gap-2 rounded-xl bg-white px-4 py-2.5 text-sm font-black text-black disabled:opacity-40"><RefreshCw size={17} className={state.loading ? 'animate-spin' : ''} /> Làm mới dữ liệu</button>}
      />

      <MetricStrip items={[
        { icon: Ticket, label: 'Tổng đơn tại rạp', value: summary.totalBookings || 0, hint: 'Theo phạm vi rạp được phân công', tone: 'blue' },
        { icon: Clock3, label: 'Đang giữ ghế', value: summary.pendingPayment || 0, hint: `${summary.expiringSoon || 0} lượt sắp hết hạn`, tone: 'amber' },
        { icon: CheckCircle2, label: 'Đã xác nhận', value: summary.confirmed || 0, hint: `${summary.completed || 0} đơn đã hoàn tất`, tone: 'green' },
        { icon: AlertTriangle, label: 'Cần chú ý', value: summary.needsAttention || 0, hint: `${summary.paymentFailed || 0} đơn thanh toán lỗi`, tone: 'red' },
      ]} />

      {actionState.success ? <div role="status" className="rounded-xl border border-emerald-500/20 bg-emerald-500/5 p-4 text-sm font-bold text-emerald-200">{actionState.success}</div> : null}
      {actionState.error ? <div role="alert" className="rounded-xl border border-red-500/20 bg-red-500/5 p-4 text-sm font-bold text-red-200">{actionState.error}</div> : null}

      <ConsolePanel className="overflow-hidden">
        <form onSubmit={submitSearch} className="grid gap-3 border-b border-white/10 p-4 lg:grid-cols-[minmax(260px,1fr)_220px_220px_auto]">
          <label className="relative">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-600" size={18} />
            <input value={draftQuery} onChange={event => setDraftQuery(event.target.value)} placeholder="Nhập mã đơn đặt vé" aria-label="Tìm theo mã đơn đặt vé" className="h-11 w-full rounded-xl border border-white/10 bg-black/30 pl-11 pr-4 text-sm outline-none focus:border-orange-500" />
          </label>
          <select value={filters.status} onChange={event => setFilters(value => ({ ...value, status: event.target.value, page: 0 }))} aria-label="Lọc trạng thái đơn" className="h-11 rounded-xl border border-white/10 bg-black/30 px-4 text-sm outline-none focus:border-orange-500">
            <option value="">Tất cả trạng thái</option>
            {Object.entries(BOOKING_STATUS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
          <select value={filters.attention} onChange={event => setFilters(value => ({ ...value, attention: event.target.value, page: 0 }))} aria-label="Lọc việc cần chú ý" className="h-11 rounded-xl border border-white/10 bg-black/30 px-4 text-sm outline-none focus:border-orange-500">
            <option value="">Tất cả mức độ</option>
            {Object.entries(ATTENTION).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
          <button className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-brand-orange px-5 text-sm font-black text-black"><Search size={17} /> Tra cứu</button>
        </form>

        {state.loading ? <p className="py-20 text-center text-sm font-bold text-zinc-500">Đang tải đơn đặt vé…</p> : state.error ? (
          <EmptyWorkspace title="Không thể tải dữ liệu" description={state.error} action={<button type="button" onClick={load} className="rounded-xl bg-white px-4 py-2 text-sm font-black text-black">Thử lại</button>} />
        ) : rows.length ? (
          <div className="overflow-x-auto">
            <table className="min-w-[980px] w-full text-left text-sm">
              <thead className="bg-white/[0.025] text-[10px] font-black uppercase tracking-wider text-zinc-600"><tr><th className="p-4">Đơn & phim</th><th className="p-4">Suất chiếu</th><th className="p-4">Ghế</th><th className="p-4">Thanh toán</th><th className="p-4">Trạng thái</th><th className="p-4 text-right">Thao tác</th></tr></thead>
              <tbody className="divide-y divide-white/5">
                {rows.map(booking => <tr key={booking.publicId} className="hover:bg-white/[0.02]">
                  <td className="p-4"><p className="font-black text-zinc-100">{booking.bookingCode}</p><p className="mt-1 max-w-56 truncate text-xs text-zinc-500">{booking.movieTitle || 'Chưa có tên phim'}</p>{booking.attentionCode ? <p className="mt-2 text-[10px] font-black text-amber-300">{ATTENTION[booking.attentionCode] || 'Cần kiểm tra'}</p> : null}</td>
                  <td className="p-4"><p className="font-bold text-zinc-300">{localizeAuditoriumName(booking.auditoriumName)}</p><p className="mt-1 text-xs text-zinc-600">{formatDateTime(booking.showtimeStart)}</p></td>
                  <td className="p-4"><p className="font-black text-zinc-200">{booking.seatCount || 0} ghế</p>{booking.bookingStatus === 'PENDING_PAYMENT' ? <p className="mt-1 text-xs text-zinc-600">Hết hạn {formatDateTime(booking.expiresAt)}</p> : null}</td>
                  <td className="p-4"><p className="font-black text-zinc-100">{formatMoney(booking.finalAmount, booking.currency)}</p><div className="mt-2"><StatusPill status={booking.paymentStatus} kind="payment" /></div></td>
                  <td className="p-4"><StatusPill status={booking.bookingStatus} /></td>
                  <td className="p-4"><div className="flex justify-end gap-2"><button type="button" onClick={() => navigate(`/manager/bookings/${booking.publicId}`)} className="inline-flex items-center gap-1.5 rounded-lg border border-white/10 px-3 py-2 text-xs font-black text-zinc-300 hover:bg-white/5"><Eye size={15} /> Xem hồ sơ</button>{booking.bookingStatus === 'PENDING_PAYMENT' && booking.paymentStatus !== 'SUCCESS' ? <button type="button" onClick={() => { setCancelTarget(booking); setCancelReason(''); setActionState(value => ({ ...value, error: '' })); }} className="inline-flex items-center gap-1.5 rounded-lg border border-red-500/20 bg-red-500/5 px-3 py-2 text-xs font-black text-red-300"><Ban size={15} /> Hủy giữ ghế</button> : null}</div></td>
                </tr>)}
              </tbody>
            </table>
          </div>
        ) : <EmptyWorkspace title="Không có đơn phù hợp" description="Thử đổi mã đơn, trạng thái hoặc bộ lọc cần chú ý." />}
        <ConsolePagination page={pageData.pageNo ?? pageData.number ?? 0} totalPages={pageData.totalPages || 0} totalElements={pageData.totalElements || 0} onPage={page => setFilters(value => ({ ...value, page }))} />
      </ConsolePanel>

      <p className="flex items-center gap-2 text-xs text-zinc-600"><TimerReset size={15} /> Lượt giữ ghế quá hạn được hệ thống tự giải phóng. Quản lý rạp chỉ hủy thủ công khi cần xử lý vận hành tại rạp.</p>

      <ActionModal open={Boolean(cancelTarget)} onClose={() => { setCancelTarget(null); setActionState(value => ({ ...value, error: '' })); }} title="Hủy lượt giữ ghế" description={`${cancelTarget?.bookingCode || ''} · Chỉ áp dụng cho đơn chưa thanh toán.`} onSubmit={cancelHold} submitLabel="Xác nhận hủy giữ ghế" submitting={actionState.loading} tone="danger">
        {actionState.error ? <p role="alert" className="rounded-xl border border-red-500/20 bg-red-500/5 p-3 text-xs font-bold text-red-200">{actionState.error}</p> : null}
        <div className="rounded-xl border border-amber-500/20 bg-amber-500/5 p-4 text-xs leading-5 text-amber-100/75">Ghế sẽ được trả lại để khách khác đặt. Không dùng thao tác này cho đơn đã thu tiền; hãy chuyển sang quy trình yêu cầu hoàn tiền.</div>
        <label className="block text-xs font-black uppercase tracking-wider text-zinc-500">Lý do hủy *<textarea required minLength={5} maxLength={500} value={cancelReason} onChange={event => setCancelReason(event.target.value)} placeholder="Ví dụ: Khách xác nhận không tiếp tục thanh toán tại quầy…" className="mt-2 min-h-24 w-full rounded-xl border border-white/10 bg-black/40 p-3 text-sm normal-case text-white outline-none focus:border-red-500" /></label>
      </ActionModal>
    </main>
  );
}
