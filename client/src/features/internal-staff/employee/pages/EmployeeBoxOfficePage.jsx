import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Armchair,
  Banknote,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Film,
  LoaderCircle,
  MapPin,
  Printer,
  RefreshCw,
  RotateCcw,
  ShieldCheck,
  Ticket,
  XCircle,
} from 'lucide-react';
import { getCinemas, getSeatLayout, getShowtimes } from '@/features/catalog/customer/services/movieService';
import {
  cancelBooking,
  createBooking,
  finalizeCheckout,
  getBookingTickets,
} from '@/features/booking/customer/services/bookingService';
import { getSeatAvailability } from '@/features/booking/customer/services/seatReservationService';
import { buildSeatUnits } from '@/features/booking/customer/utils/seatUnits';
import { hasSingleSeatGap } from '@/features/booking/customer/utils/seatGapPolicy';
import {
  cancelCashPayment,
  collectCashPayment,
  createCashPayment,
  paymentErrorMessage,
} from '@/features/payment/services/paymentService';
import PaymentNoticeModal from '@/features/payment/components/PaymentNoticeModal';
import { getMyEmployeeWorkContext } from '../services/employeeBoxOfficeService';

const money = (value, currency = 'VND') => new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: currency || 'VND',
  maximumFractionDigits: 0,
}).format(Number(value || 0));

const dateKey = date => [
  date.getFullYear(),
  String(date.getMonth() + 1).padStart(2, '0'),
  String(date.getDate()).padStart(2, '0'),
].join('-');

const pageItems = value => value?.content || value?.data || [];
const operationKey = prefix => `${prefix}-${crypto.randomUUID()}`;
const clock = value => value
  ? new Date(value).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false })
  : '--:--';
const duration = showtime => Math.max(0, Math.round(
  (new Date(showtime.endTime) - new Date(showtime.startTime)) / 60000,
));
const auditoriumLabel = value => (value || 'Phòng chiếu')
  .replace(/\bScreen\b/gi, 'Phòng')
  .replace(/\bStandard\b/gi, 'Tiêu chuẩn');

const seatTone = unit => {
  if (!unit.sellable || !unit.pairValid) return 'cursor-not-allowed border-zinc-800 bg-zinc-900 text-zinc-700';
  if (unit.seatType === 'VIP') return 'border-amber-500/45 bg-amber-500/10 text-amber-200 hover:bg-amber-500/20';
  if (unit.seatType === 'COUPLE') return 'border-pink-500/45 bg-pink-500/10 text-pink-200 hover:bg-pink-500/20';
  return 'border-zinc-600 bg-zinc-800 text-zinc-200 hover:border-zinc-400';
};

export default function EmployeeBoxOfficePage() {
  const dates = useMemo(() => Array.from({ length: 7 }, (_, index) => {
    const value = new Date();
    value.setDate(value.getDate() + index);
    return {
      key: dateKey(value),
      weekday: value.toLocaleDateString('vi-VN', { weekday: 'short' }),
      label: value.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }),
    };
  }), []);
  const [cinema, setCinema] = useState(null);
  const [schedule, setSchedule] = useState({});
  const [selectedDate, setSelectedDate] = useState(dates[0].key);
  const [selectedShowtime, setSelectedShowtime] = useState(null);
  const [layout, setLayout] = useState(null);
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [scheduleLoading, setScheduleLoading] = useState(false);
  const [seatLoading, setSeatLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [booking, setBooking] = useState(null);
  const [payment, setPayment] = useState(null);
  const [receivedAmount, setReceivedAmount] = useState('');
  const [tickets, setTickets] = useState([]);
  const [notice, setNotice] = useState(null);
  const [confirmCollect, setConfirmCollect] = useState(false);
  const [confirmCancel, setConfirmCancel] = useState(false);

  const loadSchedule = useCallback(async targetCinema => {
    if (!targetCinema?.slug) return;
    setScheduleLoading(true);
    try {
      const responses = await Promise.all(dates.map(item => getShowtimes({
        cinemaSlug: targetCinema.slug,
        date: item.key,
        page: 0,
        size: 100,
      })));
      const next = Object.fromEntries(dates.map((item, index) => [
        item.key,
        pageItems(responses[index]).filter(showtime => (
          showtime.status === 'OPEN_FOR_BOOKING'
          && new Date(showtime.startTime) > new Date()
        )),
      ]));
      setSchedule(next);
      const firstOpenDate = dates.find(item => next[item.key]?.length);
      setSelectedDate(current => next[current]?.length ? current : (firstOpenDate?.key || current));
    } catch {
      setNotice({
        tone: 'danger',
        title: 'Không thể tải lịch bán vé',
        message: 'Vui lòng kiểm tra kết nối hoặc liên hệ quản lý rạp.',
      });
    } finally {
      setScheduleLoading(false);
    }
  }, [dates]);

  useEffect(() => {
    let active = true;
    Promise.all([getMyEmployeeWorkContext(), getCinemas({ page: 0, size: 100 })])
      .then(([workContext, cinemaPage]) => {
        if (!active) return;
        const assigned = pageItems(cinemaPage).find(item => item.publicId === workContext?.cinemaPublicId);
        if (!assigned) throw new Error('EMPLOYEE_CINEMA_NOT_ASSIGNED');
        setCinema(assigned);
        return loadSchedule(assigned);
      })
      .catch(error => {
        if (!active) return;
        setNotice({
          tone: 'danger',
          title: 'Chưa xác định được quầy bán vé',
          message: error?.message === 'EMPLOYEE_CINEMA_NOT_ASSIGNED'
            ? 'Tài khoản chưa được phân công rạp hoạt động. Vui lòng liên hệ quản trị viên.'
            : 'Không thể tải thông tin rạp làm việc lúc này.',
        });
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [loadSchedule]);

  const loadSeats = useCallback(async showtime => {
    setSelectedShowtime(showtime);
    setSeatLoading(true);
    setLayout(null);
    setSelectedSeats([]);
    try {
      const [seatLayout, availability] = await Promise.all([
        getSeatLayout(showtime.showtimePublicId),
        getSeatAvailability(showtime.showtimePublicId),
      ]);
      const occupied = new Map((availability?.occupiedSeats || []).map(item => [item.seatPublicId, item]));
      setLayout({
        ...seatLayout,
        maxSeatsPerBooking: Number(availability?.maxSeatsPerBooking || 8),
        seats: (seatLayout?.seats || []).map(seat => {
          const current = occupied.get(seat.publicId);
          return current ? { ...seat, sellable: false, reservationStatus: current.status } : seat;
        }),
      });
    } catch {
      setNotice({
        tone: 'danger',
        title: 'Không thể mở sơ đồ ghế',
        message: 'Suất chiếu có thể vừa đóng bán. Hãy làm mới lịch và chọn lại.',
      });
    } finally {
      setSeatLoading(false);
    }
  }, []);

  const rows = useMemo(() => {
    const grouped = new Map();
    for (const seat of layout?.seats || []) {
      const key = seat.rowLabel || `Hàng ${seat.positionRow || '?'}`;
      if (!grouped.has(key)) grouped.set(key, []);
      grouped.get(key).push(seat);
    }
    return [...grouped.entries()]
      .sort(([, left], [, right]) => (left[0]?.positionRow || 999) - (right[0]?.positionRow || 999))
      .map(([label, seats]) => [label, buildSeatUnits(seats)]);
  }, [layout]);

  const selectedIds = useMemo(
    () => new Set(selectedSeats.map(seat => seat.publicId)),
    [selectedSeats],
  );
  const total = useMemo(
    () => selectedSeats.reduce((sum, seat) => sum + Number(seat.price || 0), 0),
    [selectedSeats],
  );
  const amountDue = Number(booking?.finalAmount ?? booking?.amount ?? total);
  const change = Math.max(0, Number(receivedAmount || 0) - amountDue);

  const toggleSeat = unit => {
    if (!unit.sellable || !unit.pairValid || booking) return;
    const ids = new Set(unit.seats.map(seat => seat.publicId));
    const selected = unit.seats.every(seat => selectedIds.has(seat.publicId));
    if (selected) {
      setSelectedSeats(current => current.filter(seat => !ids.has(seat.publicId)));
      return;
    }
    if (selectedSeats.length + unit.seats.length > Number(layout?.maxSeatsPerBooking || 8)) {
      setNotice({ tone: 'info', title: 'Đã đạt giới hạn', message: `Mỗi đơn được chọn tối đa ${layout?.maxSeatsPerBooking || 8} ghế.` });
      return;
    }
    setSelectedSeats(current => [...current, ...unit.seats]);
  };

  const openCashTransaction = async () => {
    if (!selectedSeats.length || !layout || submitting) return;
    if (hasSingleSeatGap(layout.seats, selectedIds)) {
      setNotice({
        tone: 'info',
        title: 'Lựa chọn đang để trống một ghế',
        message: 'Vui lòng chọn lại để không tạo một ghế trống đơn lẻ trong hàng.',
      });
      return;
    }
    setSubmitting(true);
    try {
      const created = await createBooking({
        showtimePublicId: layout.showtimePublicId,
        seatPublicIds: selectedSeats.map(seat => seat.publicId),
        idempotencyKey: operationKey('counter-booking'),
      });
      const finalized = await finalizeCheckout(created.publicId, { paymentMethod: 'CASH' });
      const cashPayment = await createCashPayment(
        { bookingPublicId: created.publicId },
        operationKey('counter-cash'),
      );
      setBooking({ ...created, ...finalized });
      setPayment(cashPayment);
      setReceivedAmount(String(finalized.finalAmount ?? finalized.amount ?? total));
    } catch (error) {
      setNotice({
        tone: 'danger',
        title: 'Chưa thể tạo đơn tại quầy',
        message: error?.response?.data?.message || paymentErrorMessage(error),
      });
    } finally {
      setSubmitting(false);
    }
  };

  const collect = async () => {
    setConfirmCollect(false);
    if (!payment?.paymentPublicId || Number(receivedAmount) < amountDue) return;
    setSubmitting(true);
    try {
      const result = await collectCashPayment(
        payment.paymentPublicId,
        Number(receivedAmount),
        operationKey('counter-collect'),
      );
      setPayment(current => ({ ...current, ...result, status: 'SUCCESS' }));
      let issued = [];
      for (let attempt = 0; attempt < 5 && !issued.length; attempt += 1) {
        try {
          issued = await getBookingTickets(booking.publicId);
        } catch {
          issued = [];
        }
        if (!issued.length && attempt < 4) await new Promise(resolve => setTimeout(resolve, 700));
      }
      setTickets(issued);
      setNotice({
        tone: 'success',
        title: 'Đã thu tiền và phát hành vé',
        message: `Trả khách ${money(Number(receivedAmount) - amountDue)} tiền thừa. Có thể in vé ngay tại quầy.`,
      });
    } catch (error) {
      setNotice({
        tone: 'danger',
        title: 'Chưa ghi nhận được tiền mặt',
        message: error?.response?.data?.message || paymentErrorMessage(error),
      });
    } finally {
      setSubmitting(false);
    }
  };

  const resetSale = () => {
    setBooking(null);
    setPayment(null);
    setTickets([]);
    setReceivedAmount('');
    setSelectedSeats([]);
    if (selectedShowtime) loadSeats(selectedShowtime);
  };

  const cancelCurrentSale = async () => {
    setConfirmCancel(false);
    if (!booking?.publicId || paid || submitting) return;
    setSubmitting(true);
    try {
      if (payment?.paymentPublicId && payment?.status !== 'CANCELLED') {
        await cancelCashPayment(
          payment.paymentPublicId,
          operationKey('counter-cancel-payment'),
        );
      }
      await cancelBooking(booking.publicId, 'Khách đổi ý trước khi thu tiền tại quầy');
      setBooking(null);
      setPayment(null);
      setTickets([]);
      setReceivedAmount('');
      setSelectedSeats([]);
      if (selectedShowtime) await loadSeats(selectedShowtime);
      setNotice({
        tone: 'success',
        title: 'Đã hủy đơn chưa thu',
        message: 'Giao dịch tiền mặt đã được đóng và các ghế vừa giữ đã được trả lại sơ đồ.',
      });
    } catch (error) {
      setNotice({
        tone: 'danger',
        title: 'Chưa hủy được đơn',
        message: error?.response?.data?.message || paymentErrorMessage(error),
      });
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="grid min-h-[60vh] place-items-center text-zinc-400"><LoaderCircle className="h-9 w-9 animate-spin text-amber-500" /></div>;

  const currentShowtimes = schedule[selectedDate] || [];
  const paid = payment?.status === 'SUCCESS';

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-6 pb-10 text-white">
      <header className="flex flex-col gap-5 rounded-3xl border border-zinc-800 bg-zinc-900/70 p-6 lg:flex-row lg:items-center lg:justify-between">
        <div><p className="text-xs font-black uppercase tracking-[0.22em] text-amber-500">Quầy vé trực tiếp</p><h1 className="mt-2 text-3xl font-black">Bán vé tại quầy</h1><p className="mt-2 text-sm text-zinc-400">Chọn suất chiếu, giữ ghế, thu tiền mặt và in vé trong một luồng.</p></div>
        <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/[0.06] px-5 py-4"><p className="flex items-center gap-2 text-xs font-black uppercase text-emerald-400"><ShieldCheck size={16} /> Rạp được phân công</p><p className="mt-2 font-black">{cinema?.name || 'Chưa phân công rạp'}</p><p className="mt-1 text-xs text-zinc-500">Chỉ mở bán suất chiếu của rạp này</p></div>
      </header>

      <section className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5">
        <div className="flex items-center justify-between gap-3"><div><h2 className="flex items-center gap-2 font-black"><CalendarDays size={18} className="text-amber-500" /> 1. Chọn ngày và suất chiếu</h2><p className="mt-1 text-xs text-zinc-500">Hệ thống tự chuyển đến ngày gần nhất có lịch mở bán.</p></div><button type="button" onClick={() => loadSchedule(cinema)} disabled={scheduleLoading} className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 px-4 py-2 text-xs font-black text-zinc-300 disabled:opacity-50"><RefreshCw size={15} className={scheduleLoading ? 'animate-spin' : ''} /> Làm mới lịch</button></div>
        <div className="mt-5 flex gap-2 overflow-x-auto pb-2">{dates.map(item => <button key={item.key} type="button" onClick={() => { setSelectedDate(item.key); setSelectedShowtime(null); setLayout(null); setSelectedSeats([]); }} disabled={Boolean(booking)} className={`min-w-24 rounded-xl border px-4 py-3 text-center disabled:opacity-50 ${selectedDate === item.key ? 'border-amber-500 bg-amber-500 text-black' : 'border-zinc-700 bg-zinc-950 text-zinc-300'}`}><span className="block text-xs font-bold capitalize">{item.weekday}</span><span className="mt-1 block text-sm font-black">{item.label}</span><span className="mt-1 block text-[10px]">{schedule[item.key]?.length || 0} suất</span></button>)}</div>
        <div className="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-3">{scheduleLoading ? <p className="col-span-full py-8 text-center text-sm text-zinc-500">Đang tải lịch chiếu…</p> : currentShowtimes.length ? currentShowtimes.map(showtime => <button key={showtime.showtimePublicId} type="button" onClick={() => loadSeats(showtime)} disabled={Boolean(booking)} className={`rounded-2xl border p-4 text-left disabled:opacity-50 ${selectedShowtime?.showtimePublicId === showtime.showtimePublicId ? 'border-amber-500 bg-amber-500/10' : 'border-zinc-700 bg-zinc-950/60 hover:border-zinc-500'}`}><div className="flex items-start justify-between gap-3"><div><p className="font-black text-zinc-100">{showtime.movie?.title || 'Phim chưa có tên'}</p><p className="mt-1 text-xs text-zinc-500">{showtime.movieVersion?.versionName || showtime.movieVersion?.format || 'Định dạng tiêu chuẩn'}</p></div><span className="rounded-lg bg-amber-500 px-2.5 py-1 text-sm font-black text-black">{clock(showtime.startTime)}</span></div><div className="mt-4 flex flex-wrap gap-x-4 gap-y-2 text-xs text-zinc-400"><span className="flex items-center gap-1.5"><Clock3 size={14} /> {duration(showtime)} phút</span><span className="flex items-center gap-1.5"><Film size={14} /> {auditoriumLabel(showtime.auditorium?.name)}</span></div></button>) : <div className="col-span-full rounded-2xl border border-dashed border-zinc-700 py-10 text-center text-sm text-zinc-500">Ngày này chưa có suất chiếu đang mở bán.</div>}</div>
      </section>

      {selectedShowtime ? <div className="grid min-w-0 items-start gap-6 xl:grid-cols-[minmax(0,1fr)_380px]">
        <section className="min-w-0 rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5 md:p-7"><div className="flex items-center justify-between"><div><h2 className="flex items-center gap-2 font-black"><Armchair size={19} className="text-amber-500" /> 2. Chọn ghế</h2><p className="mt-1 text-xs text-zinc-500">Ghế mờ là ghế đã giữ, đã bán hoặc đang khóa vận hành.</p></div><span className="rounded-full bg-zinc-800 px-3 py-1 text-xs font-black text-zinc-300">Tối đa {layout?.maxSeatsPerBooking || 8} ghế</span></div>{seatLoading ? <div className="grid min-h-72 place-items-center"><LoaderCircle className="animate-spin text-amber-500" /></div> : layout ? <><div className="mx-auto mt-8 min-w-0 max-w-4xl"><div className="mx-auto mb-9 h-2 w-3/4 rounded-full bg-gradient-to-r from-transparent via-zinc-300 to-transparent shadow-[0_12px_30px_rgba(255,255,255,0.18)]" /><p className="-mt-5 mb-8 text-center text-[10px] font-black uppercase tracking-[0.28em] text-zinc-600">Màn hình</p><div className="max-w-full space-y-2 overflow-x-auto pb-3">{rows.map(([label, units]) => <div key={label} className="flex min-w-max items-center justify-center gap-2"><span className="w-7 text-center text-xs font-black text-zinc-500">{label}</span>{units.map(unit => { const selected = unit.seats.every(seat => selectedIds.has(seat.publicId)); return <button key={unit.key} type="button" disabled={!unit.sellable || !unit.pairValid || Boolean(booking)} onClick={() => toggleSeat(unit)} title={`${unit.seatCode} · ${money(unit.price)}`} className={`h-9 rounded-lg border px-2 text-[10px] font-black transition ${unit.isCouple ? 'min-w-20' : 'w-10'} ${selected ? 'border-amber-300 bg-amber-500 text-black' : seatTone(unit)}`}>{unit.seatCode}</button>; })}</div>)}</div></div><div className="mt-7 flex flex-wrap justify-center gap-4 text-xs text-zinc-400"><span className="flex items-center gap-2"><i className="h-3 w-3 rounded bg-zinc-800" /> Ghế thường</span><span className="flex items-center gap-2"><i className="h-3 w-3 rounded bg-amber-500/50" /> VIP</span><span className="flex items-center gap-2"><i className="h-3 w-3 rounded bg-pink-500/50" /> Ghế đôi</span><span className="flex items-center gap-2"><i className="h-3 w-3 rounded bg-amber-500" /> Đang chọn</span><span className="flex items-center gap-2"><i className="h-3 w-3 rounded bg-zinc-900" /> Không khả dụng</span></div></> : null}</section>

        <aside className="space-y-4 rounded-3xl border border-amber-500/25 bg-zinc-900 p-6 xl:sticky xl:top-6"><div className="border-b border-zinc-800 pb-5"><p className="text-xs font-black uppercase tracking-widest text-zinc-500">Đơn tại quầy</p><h2 className="mt-2 text-lg font-black">{selectedShowtime.movie?.title}</h2><p className="mt-2 flex items-center gap-2 text-xs text-zinc-400"><MapPin size={14} /> {cinema?.name}</p><p className="mt-2 flex items-center gap-2 text-xs text-zinc-400"><Clock3 size={14} /> {clock(selectedShowtime.startTime)} · {auditoriumLabel(selectedShowtime.auditorium?.name)}</p></div><div><p className="text-xs font-bold text-zinc-500">Ghế đã chọn ({selectedSeats.length})</p><div className="mt-2 flex min-h-10 flex-wrap gap-2">{selectedSeats.length ? selectedSeats.map(seat => <span key={seat.publicId} className="rounded-lg bg-zinc-800 px-2.5 py-1.5 text-xs font-black">{seat.seatCode}</span>) : <span className="text-sm text-zinc-600">Chưa chọn ghế</span>}</div></div><div className="flex items-end justify-between border-t border-zinc-800 pt-5"><span className="text-sm text-zinc-400">Tạm tính</span><strong className="text-2xl text-amber-400">{money(total)}</strong></div>{!booking ? <button type="button" onClick={openCashTransaction} disabled={!selectedSeats.length || submitting} className="flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-amber-500 font-black text-black disabled:opacity-40">{submitting ? <LoaderCircle className="animate-spin" size={19} /> : <Ticket size={19} />} Tạo đơn & chuyển sang thu tiền</button> : !paid ? <div className="space-y-4"><div className="rounded-xl border border-emerald-500/20 bg-emerald-500/[0.05] p-3 text-xs text-emerald-300"><p className="font-black">Đã giữ ghế · {booking.bookingCode}</p><p className="mt-1 text-emerald-200/60">Chỉ giao vé sau khi hệ thống ghi nhận đủ tiền.</p></div><label className="block text-xs font-black text-zinc-300">3. Tiền khách đưa<input aria-label="Tiền khách đưa tại quầy" type="number" min={amountDue} step="1000" value={receivedAmount} onChange={event => setReceivedAmount(event.target.value)} className="mt-2 h-12 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 text-lg font-black outline-none focus:border-amber-500" /></label><div className="flex items-center justify-between rounded-xl bg-zinc-950 p-4"><span className="text-sm text-zinc-400">Tiền thừa</span><strong className="text-lg text-emerald-400">{money(change)}</strong></div><button type="button" onClick={() => setConfirmCollect(true)} disabled={submitting || Number(receivedAmount) < amountDue} className="flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-emerald-500 font-black text-black disabled:opacity-40"><Banknote size={19} /> Xác nhận đã nhận đủ tiền</button><button type="button" onClick={() => setConfirmCancel(true)} disabled={submitting} className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-rose-500/40 font-black text-rose-300 disabled:opacity-40"><XCircle size={18} /> Hủy đơn chưa thu</button></div> : <div className="space-y-4"><div className="rounded-2xl border border-emerald-500/30 bg-emerald-500/10 p-5 text-center"><CheckCircle2 className="mx-auto text-emerald-400" /><p className="mt-3 font-black text-emerald-200">Đã thanh toán và phát hành vé</p><p className="mt-1 text-xs text-emerald-200/60">Mã đơn {booking.bookingCode}</p></div><div className="space-y-2">{tickets.length ? tickets.map(item => <div key={item.publicId || item.ticketCode} className="flex items-center justify-between rounded-xl bg-zinc-950 p-3"><span className="font-black">Ghế {item.seatLabel}</span><span className="font-mono text-xs text-zinc-400">{item.ticketCode}</span></div>) : <p className="rounded-xl bg-zinc-950 p-3 text-center text-xs text-zinc-500">Vé đang đồng bộ; có thể tra cứu lại bằng mã đơn.</p>}</div><button type="button" onClick={() => window.print()} className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-zinc-600 font-black"><Printer size={18} /> In vé / biên nhận</button><button type="button" onClick={resetSale} className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-white font-black text-black"><RotateCcw size={18} /> Bán đơn tiếp theo</button></div>}</aside>
      </div> : <section className="grid min-h-64 place-items-center rounded-3xl border border-dashed border-zinc-800 text-center text-zinc-500"><div><Ticket className="mx-auto mb-4 h-12 w-12" /><p className="font-black text-zinc-300">Chọn một suất chiếu để bắt đầu bán vé</p><p className="mt-2 text-sm">Sơ đồ ghế và giá bán thực tế sẽ hiển thị ở đây.</p></div></section>}

      <PaymentNoticeModal open={Boolean(notice)} {...notice} onClose={() => setNotice(null)} />
      <PaymentNoticeModal open={confirmCollect} tone="success" title="Xác nhận đã nhận đủ tiền?" message={`Nhận ${money(receivedAmount)}; trả khách ${money(change)} tiền thừa. Thao tác này sẽ phát hành vé và không thể hủy như giao dịch chưa thu.`} confirmLabel="Xác nhận đã thu" cancelLabel="Kiểm tra lại" busy={submitting} onConfirm={collect} onClose={() => setConfirmCollect(false)} />
      <PaymentNoticeModal open={confirmCancel} tone="danger" title="Hủy đơn chưa thu?" message="Giao dịch tiền mặt sẽ bị đóng và toàn bộ ghế đang giữ của đơn này sẽ được trả lại để tiếp tục bán." confirmLabel="Hủy đơn và trả ghế" cancelLabel="Giữ lại đơn" busy={submitting} onConfirm={cancelCurrentSale} onClose={() => setConfirmCancel(false)} />
    </div>
  );
}
