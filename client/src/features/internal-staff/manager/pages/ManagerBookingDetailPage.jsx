import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useOutletContext, useParams } from 'react-router-dom';
import {
  Activity,
  Armchair,
  ArrowLeft,
  Ban,
  CalendarClock,
  CreditCard,
  Film,
  History,
  Popcorn,
  ReceiptText,
  ShieldCheck,
  Ticket,
  UserRound,
} from 'lucide-react';
import { EmptyWorkspace } from '../../admin/components/HrWorkspace';
import { ActionModal } from '../../admin/components/OperationsConsole';
import { bookingOperationalConclusion } from '@/features/booking/admin/bookingAdminPresentation';
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

const RESERVATION_STATUS = {
  HELD: 'Đang giữ',
  BOOKED: 'Đã đặt',
  RELEASED: 'Đã trả ghế',
  EXPIRED: 'Đã hết hạn',
};

const TICKET_STATUS = {
  ACTIVE: 'Có hiệu lực',
  USED: 'Đã sử dụng',
  CANCELLED: 'Đã hủy',
  REFUNDED: 'Đã hoàn tiền',
};

const FOOD_STATUS = {
  DRAFT: 'Đang chọn món',
  PENDING: 'Chờ thanh toán',
  CONFIRMED: 'Đã xác nhận',
  PREPARING: 'Đang chuẩn bị',
  READY: 'Sẵn sàng nhận',
  COMPLETED: 'Đã giao',
  CANCELLED: 'Đã hủy',
};

const SEAT_TYPE = {
  STANDARD: 'Ghế thường',
  VIP: 'Ghế VIP',
  COUPLE: 'Ghế đôi',
  ACCESSIBLE: 'Ghế hỗ trợ',
};

const PAYMENT_METHOD = {
  ONLINE: 'Thanh toán trực tuyến',
  CASH: 'Tiền mặt tại quầy',
  VNPAY: 'VNPay',
  MOMO: 'MoMo',
};

const money = (value, currency = 'VND') => new Intl.NumberFormat('vi-VN', {
  style: 'currency', currency: currency || 'VND', maximumFractionDigits: 0,
}).format(Number(value || 0));

const dateTime = value => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
  : 'Chưa ghi nhận';

const toneClass = tone => ({
  success: 'border-emerald-500/30 bg-emerald-500/[0.06] text-emerald-200',
  danger: 'border-red-500/30 bg-red-500/[0.06] text-red-200',
  warning: 'border-amber-500/30 bg-amber-500/[0.06] text-amber-200',
  info: 'border-sky-500/30 bg-sky-500/[0.06] text-sky-200',
  neutral: 'border-zinc-800 bg-zinc-900 text-zinc-200',
}[tone] || 'border-zinc-800 bg-zinc-900 text-zinc-200');

const statusTone = status => {
  if (['CONFIRMED', 'COMPLETED', 'SUCCESS', 'ACTIVE', 'BOOKED'].includes(status)) return 'border-emerald-500/25 bg-emerald-500/10 text-emerald-300';
  if (['CANCELLED', 'FAILED', 'EXPIRED', 'RELEASED'].includes(status)) return 'border-red-500/25 bg-red-500/10 text-red-300';
  if (['PENDING_PAYMENT', 'PENDING', 'HELD'].includes(status)) return 'border-amber-500/25 bg-amber-500/10 text-amber-300';
  return 'border-zinc-700 bg-zinc-800/60 text-zinc-300';
};

function StatusPill({ status, labels = BOOKING_STATUS }) {
  return <span className={`inline-flex rounded-full border px-3 py-1 text-[10px] font-black ${statusTone(status)}`}>{labels[status] || 'Chưa xác định'}</span>;
}

function InfoCard({ icon: Icon, title, children, className = '' }) {
  return (
    <section className={`rounded-3xl border border-zinc-800 bg-zinc-900/80 p-5 md:p-6 ${className}`}>
      <h2 className="flex items-center gap-2 border-b border-zinc-800 pb-4 text-sm font-black uppercase tracking-wide text-zinc-100"><Icon size={18} className="text-brand-orange" /> {title}</h2>
      <div className="mt-5">{children}</div>
    </section>
  );
}

function DetailRows({ rows }) {
  return <dl className="space-y-3">{rows.map(([label, value]) => <div key={label} className="flex items-start justify-between gap-5 text-sm"><dt className="text-zinc-500">{label}</dt><dd className="max-w-[65%] text-right font-bold text-zinc-200 break-words">{value}</dd></div>)}</dl>;
}

const customerCode = userId => userId ? `KH${String(userId).padStart(6, '0')}` : 'Khách vãng lai';

const localizeAuditoriumName = value => {
  if (!value) return 'Chưa rõ phòng';
  return String(value)
    .replace(/\bScreen\b/gi, 'Phòng')
    .replace(/\bStandard\b/gi, 'Tiêu chuẩn')
    .replace(/\bDeluxe\b/gi, 'Cao cấp');
};

const FOOD_NAME = {
  'Popcorn Size L': 'Bắp rang cỡ lớn',
  'Popcorn Size S': 'Bắp rang cỡ nhỏ',
  'Cheese Hot Dog': 'Xúc xích phô mai',
  'Pepsi Size L': 'Pepsi cỡ lớn',
  'Pepsi Size S': 'Pepsi cỡ nhỏ',
  'Mineral Water': 'Nước khoáng',
};

const localizeFoodName = value => FOOD_NAME[value] || value || 'Sản phẩm bắp nước';

const AGE_RATING = {
  P: 'P · Mọi độ tuổi',
  K: 'K · Dưới 13 tuổi xem cùng người giám hộ',
  T13: 'T13 · Từ đủ 13 tuổi',
  T16: 'T16 · Từ đủ 16 tuổi',
  T18: 'T18 · Từ đủ 18 tuổi',
};

const resolveDuration = snapshot => {
  const storedDuration = Number(snapshot?.duration);
  if (storedDuration > 0) return storedDuration;
  if (!snapshot?.showtimeStart || !snapshot?.showtimeEnd) return null;
  const minutes = Math.round(
    (new Date(snapshot.showtimeEnd).getTime() - new Date(snapshot.showtimeStart).getTime()) / 60000,
  );
  return minutes > 0 ? minutes : null;
};

const historyReason = item => {
  const known = {
    USER_CANCEL: 'Khách hàng chủ động hủy đơn',
    ADMIN_CANCEL: 'Quản trị viên hủy theo yêu cầu vận hành',
    MANAGER_CANCELLED_HOLD: 'Quản lý rạp hủy lượt giữ ghế chưa thanh toán',
    PAYMENT_SUCCESS: 'Hệ thống xác nhận thanh toán thành công',
    PAYMENT_FAILED: 'Thanh toán không thành công',
    PAYMENT_DEADLINE_EXPIRED: 'Đơn hết thời hạn thanh toán',
    BOOKING_EXPIRED: 'Đơn hết thời hạn giữ ghế',
    'Authoritative payment result': 'Hệ thống thanh toán đã xác nhận kết quả chính thức',
    'Payment result received': 'Hệ thống đặt vé đã nhận kết quả thanh toán',
  };
  return known[item?.reason] || item?.reason || 'Hệ thống cập nhật trạng thái đơn';
};

const historyActor = value => ({
  SYSTEM: 'Hệ thống',
  CRON: 'Tác vụ tự động',
  SCHEDULER: 'Tác vụ tự động',
  ADMIN: 'Quản trị viên',
  MANAGER: 'Quản lý rạp',
  CUSTOMER: 'Khách hàng',
  PAYMENT_SERVICE: 'Hệ thống thanh toán',
  BOOKING_SERVICE: 'Hệ thống đặt vé',
  INTERNAL_SERVICE: 'Dịch vụ nội bộ',
}[value] || 'Hệ thống');

export default function ManagerBookingDetailPage() {
  const { bookingPublicId } = useParams();
  const navigate = useNavigate();
  const { selectedCinema, selectedCinemaId, cinemaState } = useOutletContext();
  const [booking, setBooking] = useState(null);
  const [foodOrder, setFoodOrder] = useState(null);
  const [state, setState] = useState({ loading: true, error: '', success: '' });
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [cancelling, setCancelling] = useState(false);

  const load = useCallback(async () => {
    if (!selectedCinemaId) return;
    setState(value => ({ ...value, loading: true, error: '' }));
    try {
      const [bookingResult, foodResult] = await Promise.all([
        managerOperationsService.getBookingDetail(selectedCinemaId, bookingPublicId),
        managerOperationsService.getBookingFoods(selectedCinemaId, bookingPublicId).catch(() => null),
      ]);
      setBooking(bookingResult);
      setFoodOrder(foodResult);
      setState(value => ({ ...value, loading: false, error: '' }));
    } catch (error) {
      setBooking(null);
      setState({ loading: false, success: '', error: error?.response?.data?.message || 'Không thể tải hồ sơ đơn đặt vé này.' });
    }
  }, [bookingPublicId, selectedCinemaId]);

  useEffect(() => {
    const timer = window.setTimeout(load, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const cancelHold = async event => {
    event.preventDefault();
    if (cancelReason.trim().length < 5) {
      setState(value => ({ ...value, error: 'Vui lòng nhập lý do ít nhất 5 ký tự.' }));
      return;
    }
    setCancelling(true);
    try {
      await managerOperationsService.cancelBookingHold(selectedCinemaId, bookingPublicId, cancelReason.trim());
      setCancelOpen(false);
      setCancelReason('');
      setState({ loading: false, error: '', success: 'Đã hủy lượt giữ ghế và trả ghế về kho.' });
      await load();
    } catch (error) {
      setState(value => ({ ...value, error: error?.response?.data?.message || 'Không thể hủy lượt giữ ghế.' }));
    } finally {
      setCancelling(false);
    }
  };

  const seatLines = useMemo(() => {
    if (!booking) return [];
    const tickets = booking.tickets || [];
    const reservations = booking.reservations || [];
    if (reservations.length) return reservations.map(reservation => ({
      ...reservation,
      ticket: tickets.find(ticket => ticket.seatLabel === reservation.seatLabel),
    }));
    return tickets.map(ticket => ({
      seatLabel: ticket.seatLabel,
      seatType: ticket.seatType,
      status: ticket.status,
      ticket,
    }));
  }, [booking]);

  if (cinemaState.loading || (state.loading && !booking)) return <div className="py-24 text-center text-sm font-bold text-zinc-500">Đang tải toàn bộ hồ sơ đơn đặt vé…</div>;
  if (!selectedCinema) return <EmptyWorkspace title="Chưa được phân công rạp" description="Quản trị viên cần phân công rạp trước khi bạn có thể xem hồ sơ đơn." />;
  if (state.error && !booking) return <EmptyWorkspace title="Không thể mở hồ sơ đơn" description={state.error} action={<button type="button" onClick={() => navigate('/manager/bookings')} className="rounded-xl bg-white px-4 py-2 text-sm font-black text-black">Quay lại danh sách</button>} />;
  if (!booking) return null;

  const snapshot = booking.snapshot || {};
  const duration = resolveDuration(snapshot);
  const ageRating = AGE_RATING[snapshot.ageRating] || snapshot.ageRating;
  const operational = booking.operationalInfo || {};
  const conclusion = bookingOperationalConclusion(booking, operational);
  const canCancel = booking.bookingStatus === 'PENDING_PAYMENT' && booking.paymentStatus !== 'SUCCESS';
  const histories = [...(booking.statusHistories || [])].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));

  return (
    <main className="mx-auto w-full max-w-[1500px] space-y-6 pb-10 text-white">
      <button type="button" onClick={() => navigate('/manager/bookings')} className="inline-flex items-center gap-2 text-sm font-bold text-zinc-400 hover:text-white"><ArrowLeft size={17} /> Quay lại danh sách đơn</button>

      <header className="flex flex-col justify-between gap-6 rounded-3xl border border-zinc-800 bg-zinc-900 p-6 md:p-8 lg:flex-row lg:items-center">
        <div>
          <p className="text-xs font-black uppercase tracking-[0.18em] text-zinc-500">Hồ sơ vận hành đơn đặt vé</p>
          <div className="mt-3 flex flex-wrap items-center gap-3"><h1 className="text-2xl font-black tracking-wide">{booking.bookingCode}</h1><StatusPill status={booking.bookingStatus} /></div>
          <p className="mt-3 text-sm text-zinc-500">Tạo lúc {dateTime(booking.createdAt)} · {selectedCinema.name}</p>
        </div>
        <div className="lg:text-right"><p className="text-xs font-black uppercase text-zinc-500">Tổng thanh toán</p><p className="mt-2 text-3xl font-black text-brand-orange">{money(booking.finalAmount, booking.currency)}</p><div className="mt-2"><StatusPill status={booking.paymentStatus} labels={PAYMENT_STATUS} /></div></div>
      </header>

      {state.success ? <div role="status" className="rounded-2xl border border-emerald-500/25 bg-emerald-500/[0.06] p-4 text-sm font-bold text-emerald-200">{state.success}</div> : null}
      {state.error ? <div role="alert" className="rounded-2xl border border-red-500/25 bg-red-500/[0.06] p-4 text-sm font-bold text-red-200">{state.error}</div> : null}

      <section className={`rounded-3xl border p-6 ${toneClass(conclusion.tone)}`}>
        <div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-center"><div><p className="flex items-center gap-2 text-xs font-black uppercase tracking-wider opacity-75"><Activity size={17} /> Kết luận vận hành</p><h2 className="mt-3 text-xl font-black text-white">{conclusion.title}</h2><p className="mt-2 max-w-4xl text-sm leading-6 text-zinc-400">{conclusion.detail}</p></div><div className="flex shrink-0 flex-wrap gap-2">{conclusion.paymentLink ? <button type="button" onClick={() => navigate(`/manager/payments?query=${booking.publicId}`)} className="rounded-xl border border-brand-orange/40 px-4 py-2.5 text-xs font-black text-brand-orange">Xem giao dịch liên quan</button> : null}{canCancel ? <button type="button" onClick={() => { setCancelOpen(true); setCancelReason(''); }} className="inline-flex items-center gap-2 rounded-xl bg-red-500 px-4 py-2.5 text-xs font-black text-white"><Ban size={16} /> Hủy lượt giữ ghế</button> : null}</div></div>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[
          [Armchair, 'Tình trạng ghế', RESERVATION_STATUS[operational.reservationState] || 'Chưa ghi nhận', `${operational.bookedSeatCount || 0} đã đặt · ${operational.heldSeatCount || 0} đang giữ`],
          [Ticket, 'Vé đã phát hành', `${booking.tickets?.length || 0} vé`, booking.tickets?.length ? 'Có dữ liệu mã vé và trạng thái' : 'Chưa phát hành vé'],
          [CreditCard, 'Thanh toán', PAYMENT_STATUS[booking.paymentStatus] || 'Chưa ghi nhận', booking.paymentReference || 'Chưa có mã tham chiếu'],
          [CalendarClock, 'Mốc trạng thái', dateTime(operational.stateChangedAt), operational.reasonDetail || 'Không có ghi chú bổ sung'],
        ].map(([Icon, label, value, hint]) => <article key={label} className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-5"><Icon size={19} className="text-brand-orange" /><p className="mt-4 text-xs font-black uppercase tracking-wider text-zinc-600">{label}</p><p className="mt-2 font-black text-zinc-100">{value}</p><p className="mt-2 text-xs leading-5 text-zinc-500 line-clamp-2">{hint}</p></article>)}
      </section>

      <div className="grid gap-6 xl:grid-cols-3">
        <InfoCard icon={Film} title="Phim và suất chiếu">
          <div className="flex gap-4">{snapshot.moviePoster ? <img src={snapshot.moviePoster} alt={`Áp phích ${snapshot.movieTitle || 'phim'}`} className="h-36 w-24 shrink-0 rounded-xl object-cover" /> : <div className="grid h-36 w-24 shrink-0 place-items-center rounded-xl bg-zinc-800"><Film className="text-zinc-600" /></div>}<DetailRows rows={[["Phim", snapshot.movieTitle || 'Chưa có tên phim'], ['Rạp / phòng', `${snapshot.cinemaName || selectedCinema.name} · ${localizeAuditoriumName(snapshot.auditoriumName)}`], ['Bắt đầu', dateTime(snapshot.showtimeStart)], ['Kết thúc', dateTime(snapshot.showtimeEnd)], ['Thời lượng', duration ? `${duration} phút` : 'Chưa ghi nhận'], ['Phân loại', ageRating || 'Chưa ghi nhận']]} /></div>
        </InfoCard>
        <InfoCard icon={UserRound} title="Khách hàng và thanh toán">
          <DetailRows rows={[["Mã khách hàng", customerCode(booking.userId)], ['Phương thức', PAYMENT_METHOD[booking.paymentMethodSnapshot] || PAYMENT_METHOD[booking.paymentProvider] || 'Chưa ghi nhận'], ['Nhà cung cấp', PAYMENT_METHOD[booking.paymentProvider] || booking.paymentProvider || 'Chưa ghi nhận'], ['Mã tham chiếu', booking.paymentReference || 'Chưa phát sinh'], ['Hạn giữ ghế', dateTime(booking.expiresAt)], ['Ghi chú đơn', booking.note || 'Không có ghi chú']]} />
        </InfoCard>
        <InfoCard icon={ReceiptText} title="Cơ cấu số tiền">
          <DetailRows rows={[["Tiền vé", money(booking.ticketAmount, booking.currency)], ['Bắp nước', money(booking.foodAmount, booking.currency)], ['Phí dịch vụ', money(booking.serviceFee, booking.currency)], ['Thuế', money(booking.taxAmount, booking.currency)], ['Giảm khuyến mãi', `-${money(Number(booking.promotionDiscount || 0) + Number(booking.voucherDiscount || 0), booking.currency)}`], ['Tổng thanh toán', money(booking.finalAmount, booking.currency)]]} />
        </InfoCard>
      </div>

      <InfoCard icon={Armchair} title={`Ghế và vé (${seatLines.length})`}>
        {seatLines.length ? <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">{seatLines.map((seat, index) => <article key={seat.publicId || seat.ticket?.publicId || `${seat.seatLabel}-${index}`} className="rounded-2xl border border-zinc-800 bg-zinc-950/50 p-4"><div className="flex items-start justify-between gap-3"><div><p className="text-lg font-black text-white">Ghế {seat.seatLabel || 'Chưa rõ'}</p><p className="mt-1 text-xs text-zinc-500">{SEAT_TYPE[seat.seatType || seat.ticket?.seatType] || 'Chưa phân loại'} · {money(seat.ticket?.ticketPrice ?? seat.price, booking.currency)}</p></div><StatusPill status={seat.ticket?.status || seat.status} labels={seat.ticket ? TICKET_STATUS : RESERVATION_STATUS} /></div>{seat.ticket?.ticketCode ? <p className="mt-4 border-t border-zinc-800 pt-3 font-mono text-xs text-zinc-400">Mã vé: {seat.ticket.ticketCode}</p> : <p className="mt-4 border-t border-zinc-800 pt-3 text-xs text-zinc-600">Chưa phát hành mã vé</p>}</article>)}</div> : <p className="py-8 text-center text-sm text-zinc-500">Đơn chưa có dữ liệu ghế hoặc vé.</p>}
      </InfoCard>

      <InfoCard icon={Popcorn} title="Bắp nước đi kèm đơn">
        {foodOrder?.items?.length ? <div className="space-y-3">{foodOrder.items.map(item => <div key={item.id || item.productId} className="flex items-center justify-between gap-4 rounded-2xl border border-zinc-800 bg-zinc-950/50 p-4"><div className="flex items-center gap-4">{item.productImage ? <img src={item.productImage} alt={localizeFoodName(item.productName)} className="h-14 w-14 rounded-xl object-cover" /> : <span className="grid h-14 w-14 place-items-center rounded-xl bg-zinc-800"><Popcorn size={20} className="text-zinc-500" /></span>}<div><p className="font-black text-zinc-100">{localizeFoodName(item.productName)}</p><p className="mt-1 text-xs text-zinc-500">{item.quantity || 0} × {money(item.unitPrice, booking.currency)}</p></div></div><p className="font-black text-zinc-100">{money(item.finalAmount, booking.currency)}</p></div>)}<div className="flex flex-wrap items-center justify-between gap-3 border-t border-zinc-800 pt-4"><div><StatusPill status={foodOrder.status} labels={FOOD_STATUS} /><span className="ml-2 text-xs text-zinc-500">{foodOrder.totalQuantity || 0} sản phẩm</span></div><p className="text-lg font-black text-brand-orange">{money(foodOrder.finalAmount, booking.currency)}</p></div></div> : <div className="py-8 text-center"><Popcorn className="mx-auto text-zinc-700" /><p className="mt-3 text-sm font-bold text-zinc-400">Đơn không có bắp nước</p><p className="mt-1 text-xs text-zinc-600">Toàn bộ giá trị hiện tại đến từ vé và các khoản phí liên quan.</p></div>}
      </InfoCard>

      <InfoCard icon={History} title="Lịch sử trạng thái đơn">
        {histories.length ? <ol className="space-y-0">{histories.map((item, index) => <li key={item.id || `${item.createdAt}-${index}`} className="relative grid grid-cols-[24px_1fr] gap-3 pb-6 last:pb-0"><div className="relative flex justify-center"><span className="mt-1.5 h-2.5 w-2.5 rounded-full bg-brand-orange" />{index < histories.length - 1 ? <span className="absolute bottom-0 top-4 w-px bg-zinc-800" /> : null}</div><div><div className="flex flex-wrap items-center gap-2"><p className="font-black text-zinc-200">{BOOKING_STATUS[item.toStatus] || 'Cập nhật trạng thái'}</p><span className="text-xs text-zinc-600">{dateTime(item.createdAt)}</span></div><p className="mt-1 text-sm text-zinc-400">{historyReason(item)}</p><p className="mt-1 text-xs text-zinc-600">Thực hiện bởi: {historyActor(item.changedBy || item.source)}</p></div></li>)}</ol> : <p className="py-8 text-center text-sm text-zinc-500">Chưa có lịch sử trạng thái.</p>}
      </InfoCard>

      <div className="rounded-2xl border border-sky-500/20 bg-sky-500/[0.05] p-5 text-sm leading-6 text-sky-100/70"><p className="flex items-center gap-2 font-black text-sky-300"><ShieldCheck size={17} /> Phạm vi xử lý của quản lý rạp</p><p className="mt-2">Bạn được xem toàn bộ thông tin vận hành của đơn tại rạp đang phụ trách. Chỉ được hủy lượt giữ ghế chưa thanh toán; đơn đã thu tiền phải đi qua quy trình yêu cầu hoàn tiền và đối soát riêng.</p></div>

      <ActionModal open={cancelOpen} onClose={() => { setCancelOpen(false); setState(value => ({ ...value, error: '' })); }} title="Hủy lượt giữ ghế" description={`${booking.bookingCode} · Chỉ áp dụng cho đơn chưa thanh toán.`} onSubmit={cancelHold} submitLabel="Xác nhận hủy giữ ghế" submitting={cancelling} tone="danger">
        <div className="rounded-xl border border-amber-500/20 bg-amber-500/5 p-4 text-xs leading-5 text-amber-100/75">Ghế sẽ được trả lại để khách khác đặt. Không dùng thao tác này cho đơn đã thu tiền.</div>
        <label className="block text-xs font-black uppercase tracking-wider text-zinc-500">Lý do hủy *<textarea required minLength={5} maxLength={500} value={cancelReason} onChange={event => setCancelReason(event.target.value)} placeholder="Ví dụ: Khách xác nhận không tiếp tục thanh toán tại quầy…" className="mt-2 min-h-24 w-full rounded-xl border border-white/10 bg-black/40 p-3 text-sm normal-case text-white outline-none focus:border-red-500" /></label>
      </ActionModal>
    </main>
  );
}
