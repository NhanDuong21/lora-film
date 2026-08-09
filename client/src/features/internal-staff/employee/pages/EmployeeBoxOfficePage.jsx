import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Armchair,
  Banknote,
  CalendarDays,
  Check,
  CheckCircle2,
  Clock3,
  Film,
  LoaderCircle,
  MailX,
  MapPin,
  Minus,
  Plus,
  Popcorn,
  Printer,
  RefreshCw,
  RotateCcw,
  Search,
  ShieldCheck,
  Sparkles,
  Ticket,
  UserRound,
  XCircle,
} from 'lucide-react';
import { getCinemas, getSeatLayout, getShowtimes } from '@/features/catalog/customer/services/movieService';
import {
  cancelBooking,
  createBooking,
  finalizeCheckout,
  getBookingDetails,
  getBookingTickets,
  previewBookingPromotions,
} from '@/features/booking/customer/services/bookingService';
import {
  addFoodItem,
  getConcessions,
  removeFoodItem,
  updateFoodQuantity,
} from '@/features/booking/customer/services/foodService';
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
import { searchCounterCustomers } from '../services/employeeOperationsService';

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
const dateTime = value => value
  ? new Date(value).toLocaleString('vi-VN', {
    hour: '2-digit', minute: '2-digit', second: '2-digit',
    day: '2-digit', month: '2-digit', year: 'numeric', hour12: false,
  })
  : 'Vừa ghi nhận';
const duration = showtime => Math.max(0, Math.round(
  (new Date(showtime.endTime) - new Date(showtime.startTime)) / 60000,
));
const auditoriumLabel = value => (value || 'Phòng chiếu')
  .replace(/\bScreen\b/gi, 'Phòng')
  .replace(/\bStandard\b/gi, 'Tiêu chuẩn');

const CONCESSION_NAMES = {
  POP_S: 'Bắp rang cỡ nhỏ',
  POP_L: 'Bắp rang cỡ lớn',
  POP_CARAMEL: 'Bắp rang vị caramel',
  POP_CHEESE: 'Bắp rang vị phô mai',
  WATER: 'Nước suối',
  COKE_L: 'Coca-Cola cỡ lớn',
  PEPSI_L: 'Pepsi cỡ lớn',
  LEMON_TEA: 'Trà chanh',
  COMBO_COUPLE: 'Combo đôi',
  COMBO_SINGLE: 'Combo cá nhân',
  COMBO_CLASSIC: 'Combo xem phim cổ điển',
  COMBO_DATE: 'Combo cặp đôi',
  COMBO_FAMILY: 'Combo gia đình',
  COMBO_KIDS: 'Combo trẻ em',
  HOT_DOG: 'Xúc xích phô mai',
};

const concessionName = item => CONCESSION_NAMES[item?.code] || item?.name || 'Sản phẩm tại quầy';

const emptyCounterCustomer = () => ({
  accountId: null,
  customerCode: '',
  fullName: '',
  phoneNumber: '',
  email: '',
});

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
  const [concessions, setConcessions] = useState([]);
  const [foodOrder, setFoodOrder] = useState(null);
  const [promotionPreview, setPromotionPreview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [scheduleLoading, setScheduleLoading] = useState(false);
  const [seatLoading, setSeatLoading] = useState(false);
  const [concessionsLoading, setConcessionsLoading] = useState(true);
  const [promotionLoading, setPromotionLoading] = useState(false);
  const [cartBusyId, setCartBusyId] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [booking, setBooking] = useState(null);
  const [payment, setPayment] = useState(null);
  const [receivedAmount, setReceivedAmount] = useState('');
  const [tickets, setTickets] = useState([]);
  const [notice, setNotice] = useState(null);
  const [confirmCollect, setConfirmCollect] = useState(false);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [customerMode, setCustomerMode] = useState('GUEST');
  const [counterCustomer, setCounterCustomer] = useState(emptyCounterCustomer);
  const [customerQuery, setCustomerQuery] = useState('');
  const [customerResults, setCustomerResults] = useState([]);
  const [customerSearching, setCustomerSearching] = useState(false);
  const [customerSearchMessage, setCustomerSearchMessage] = useState('');

  const availableDates = useMemo(
    () => dates.filter(item => (schedule[item.key] || []).length > 0),
    [dates, schedule],
  );

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
        if (!active) return null;
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

  useEffect(() => {
    let active = true;
    getConcessions()
      .then(items => {
        if (active) setConcessions((items || []).filter(item => item.sellable !== false));
      })
      .catch(() => {
        if (active) setConcessions([]);
      })
      .finally(() => { if (active) setConcessionsLoading(false); });
    return () => { active = false; };
  }, []);

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
  const selectedSeatTotal = useMemo(
    () => selectedSeats.reduce((sum, seat) => sum + Number(seat.price || 0), 0),
    [selectedSeats],
  );
  const foodItems = useMemo(() => foodOrder?.items || [], [foodOrder?.items]);
  const foodByProductId = useMemo(
    () => new Map(foodItems.map(item => [Number(item.productId), item])),
    [foodItems],
  );
  const foodAmount = Number(
    foodOrder?.finalAmount
    ?? foodOrder?.totalAmount
    ?? foodItems.reduce((sum, item) => sum + Number(item.finalAmount ?? item.totalAmount ?? 0), 0),
  );
  const ticketAmount = Number(booking?.ticketAmount ?? selectedSeatTotal);
  const subtotal = ticketAmount + foodAmount;
  const promotionDiscount = Number(promotionPreview?.discountAmount || 0);
  const previewTotal = Number(promotionPreview?.finalAmount ?? subtotal);
  const amountDue = Number(
    payment || booking?.amountLockedAt
      ? (booking?.finalAmount ?? previewTotal)
      : (booking ? previewTotal : selectedSeatTotal),
  );
  const change = Math.max(0, Number(receivedAmount || 0) - amountDue);
  const paid = payment?.status === 'SUCCESS';
  const checkoutLocked = Boolean(booking?.amountLockedAt || payment);
  const collectedAmount = Number(payment?.amount ?? amountDue);
  const collectedReceivedAmount = Number(payment?.receivedAmount ?? receivedAmount ?? collectedAmount);
  const collectedChangeAmount = Number(
    payment?.changeAmount
    ?? Math.max(0, collectedReceivedAmount - collectedAmount),
  );

  const refreshPromotion = useCallback(async bookingId => {
    if (!bookingId) return null;
    setPromotionLoading(true);
    try {
      const preview = await previewBookingPromotions(bookingId, { paymentMethod: 'CASH' });
      setPromotionPreview(preview);
      return preview;
    } catch {
      setPromotionPreview(null);
      return null;
    } finally {
      setPromotionLoading(false);
    }
  }, []);

  const pollTickets = useCallback(async bookingId => {
    let issued = [];
    for (let attempt = 0; attempt < 5 && !issued.length; attempt += 1) {
      try {
        issued = await getBookingTickets(bookingId);
      } catch {
        issued = [];
      }
      if (!issued.length && attempt < 4) await new Promise(resolve => setTimeout(resolve, 700));
    }
    setTickets(issued);
    return issued;
  }, []);

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

  const findCustomer = async event => {
    event.preventDefault();
    const keyword = customerQuery.trim();
    if (keyword.length < 3) {
      setCustomerSearchMessage('Nhập ít nhất 3 ký tự của tên, số điện thoại, email hoặc mã thành viên.');
      return;
    }
    setCustomerSearching(true);
    setCustomerSearchMessage('');
    try {
      const result = await searchCounterCustomers(keyword);
      const items = result?.content || [];
      setCustomerResults(items);
      if (!items.length) setCustomerSearchMessage('Không tìm thấy thành viên phù hợp. Có thể tiếp tục dưới dạng khách lẻ.');
    } catch (error) {
      setCustomerResults([]);
      setCustomerSearchMessage(error?.response?.data?.message || 'Chưa thể tra cứu thành viên. Vui lòng thử lại.');
    } finally {
      setCustomerSearching(false);
    }
  };

  const changeCustomerMode = mode => {
    if (booking) return;
    setCustomerMode(mode);
    setCounterCustomer(emptyCounterCustomer());
    setCustomerResults([]);
    setCustomerQuery('');
    setCustomerSearchMessage('');
  };

  const createCounterBooking = async () => {
    if (!selectedSeats.length || !layout || submitting) return;
    if (customerMode === 'MEMBER' && !counterCustomer.accountId) {
      setNotice({
        tone: 'info',
        title: 'Chưa chọn thành viên',
        message: 'Hãy tìm và chọn đúng thành viên, hoặc chuyển sang “Khách lẻ” để tiếp tục.',
      });
      return;
    }
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
        counterCustomerAccountId: counterCustomer.accountId,
        counterCustomerName: counterCustomer.fullName,
        counterCustomerPhone: counterCustomer.phoneNumber,
        counterCustomerEmail: counterCustomer.email,
        idempotencyKey: operationKey('counter-booking'),
      });
      setBooking(created);
      setFoodOrder(null);
      await refreshPromotion(created.publicId);
      setNotice({
        tone: 'success',
        title: 'Đã giữ ghế cho khách',
        message: 'Tiếp tục chọn bắp nước hoặc bỏ qua, sau đó kiểm tra ưu đãi và chốt số tiền.',
      });
    } catch (error) {
      setNotice({
        tone: 'danger',
        title: 'Chưa thể giữ ghế tại quầy',
        message: error?.response?.data?.message || paymentErrorMessage(error),
      });
    } finally {
      setSubmitting(false);
    }
  };

  const changeFoodQuantity = async (concession, delta) => {
    if (!booking?.publicId || cartBusyId || checkoutLocked) return;
    const current = foodByProductId.get(Number(concession.id));
    const currentQuantity = Number(current?.quantity || 0);
    const nextQuantity = currentQuantity + delta;
    if (nextQuantity < 0) return;
    setCartBusyId(concession.id);
    try {
      let updated = null;
      if (!current && nextQuantity > 0) {
        updated = await addFoodItem(booking.publicId, { productId: concession.id, quantity: 1 });
      } else if (current && nextQuantity > 0) {
        updated = await updateFoodQuantity(booking.publicId, current.id, nextQuantity);
      } else if (current) {
        await removeFoodItem(booking.publicId, current.id);
      }
      const fresh = await getBookingDetails(booking.publicId);
      setBooking(previous => ({ ...previous, ...fresh }));
      setFoodOrder(updated || fresh.foodOrder || null);
      await refreshPromotion(booking.publicId);
    } catch (error) {
      setNotice({
        tone: 'danger',
        title: 'Chưa cập nhật được bắp nước',
        message: error?.response?.data?.message || 'Vui lòng thử lại hoặc bỏ qua sản phẩm này.',
      });
    } finally {
      setCartBusyId(null);
    }
  };

  const lockAndOpenCashTransaction = async () => {
    if (!booking?.publicId || payment || submitting || promotionLoading) return;
    setSubmitting(true);
    try {
      const finalized = await finalizeCheckout(booking.publicId, { paymentMethod: 'CASH' });
      setBooking(previous => ({ ...previous, ...finalized }));
      const finalAmount = Number(finalized.finalAmount ?? finalized.amount ?? previewTotal);
      if (finalAmount <= 0) {
        setPayment({ status: 'SUCCESS', paymentMethod: 'FULL_DISCOUNT' });
        await pollTickets(booking.publicId);
        setNotice({
          tone: 'success',
          title: 'Ưu đãi đã thanh toán toàn bộ đơn',
          message: 'Vé đã được phát hành để in tại quầy; không cần thu thêm tiền từ khách.',
        });
        return;
      }
      const cashPayment = await createCashPayment(
        { bookingPublicId: booking.publicId },
        operationKey('counter-cash'),
      );
      setPayment(cashPayment);
      setReceivedAmount(String(finalAmount));
    } catch (error) {
      setNotice({
        tone: 'danger',
        title: 'Chưa thể chốt số tiền tại quầy',
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
      await pollTickets(booking.publicId);
      setNotice({
        tone: 'success',
        title: 'Đã thu tiền và phát hành vé',
        message: `Trả khách ${money(Number(receivedAmount) - amountDue)} tiền thừa. Vé chỉ được in tại quầy, không gửi vào email nhân viên.`,
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
    setFoodOrder(null);
    setPromotionPreview(null);
    setReceivedAmount('');
    setSelectedSeats([]);
    setCustomerMode('GUEST');
    setCounterCustomer(emptyCounterCustomer());
    setCustomerQuery('');
    setCustomerResults([]);
    setCustomerSearchMessage('');
    if (selectedShowtime) loadSeats(selectedShowtime);
  };

  const printCounterReceipt = () => {
    const printingClass = 'printing-counter-receipt';
    const cleanup = () => document.body.classList.remove(printingClass);
    document.body.classList.add(printingClass);
    window.addEventListener('afterprint', cleanup, { once: true });
    window.print();
    window.setTimeout(cleanup, 1000);
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
      setFoodOrder(null);
      setPromotionPreview(null);
      setReceivedAmount('');
      setSelectedSeats([]);
      setCustomerMode('GUEST');
      setCounterCustomer(emptyCounterCustomer());
      setCustomerQuery('');
      setCustomerResults([]);
      setCustomerSearchMessage('');
      if (selectedShowtime) await loadSeats(selectedShowtime);
      setNotice({
        tone: 'success',
        title: 'Đã hủy đơn chưa thu',
        message: 'Giao dịch đã được đóng, bắp nước đã bỏ và các ghế vừa giữ đã được trả lại sơ đồ.',
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

  if (loading) {
    return <div className="grid min-h-[60vh] place-items-center text-zinc-400"><LoaderCircle className="h-9 w-9 animate-spin text-amber-500" /></div>;
  }

  const currentShowtimes = schedule[selectedDate] || [];
  const appliedPromotions = promotionPreview?.appliedPromotions || [];

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-6 pb-10 text-white">
      <header className="flex flex-col gap-5 rounded-3xl border border-zinc-800 bg-zinc-900/70 p-6 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p className="text-xs font-black uppercase tracking-[0.22em] text-amber-500">Quầy vé trực tiếp</p>
          <h1 className="mt-2 text-3xl font-black">Bán vé tại quầy</h1>
          <p className="mt-2 text-sm text-zinc-400">Chọn suất chiếu, ghế, bắp nước, áp dụng ưu đãi và thu tiền trong một luồng.</p>
        </div>
        <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/[0.06] px-5 py-4">
          <p className="flex items-center gap-2 text-xs font-black uppercase text-emerald-400"><ShieldCheck size={16} /> Rạp được phân công</p>
          <p className="mt-2 font-black">{cinema?.name || 'Chưa phân công rạp'}</p>
          <p className="mt-1 text-xs text-zinc-500">Chỉ mở bán suất chiếu của rạp này</p>
        </div>
      </header>

      <section className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="flex items-center gap-2 font-black"><CalendarDays size={18} className="text-amber-500" /> 1. Chọn ngày và suất chiếu</h2>
            <p className="mt-1 text-xs text-zinc-500">Chỉ hiển thị những ngày có suất đang mở bán để tư vấn khách nhanh hơn.</p>
          </div>
          <button type="button" onClick={() => loadSchedule(cinema)} disabled={scheduleLoading || Boolean(booking)} className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 px-4 py-2 text-xs font-black text-zinc-300 disabled:opacity-50">
            <RefreshCw size={15} className={scheduleLoading ? 'animate-spin' : ''} /> Làm mới lịch
          </button>
        </div>

        {scheduleLoading ? (
          <p className="py-10 text-center text-sm text-zinc-500">Đang kiểm tra các ngày có suất chiếu…</p>
        ) : availableDates.length ? (
          <>
            <div className="mt-5 flex gap-2 overflow-x-auto pb-2">
              {availableDates.map(item => (
                <button key={item.key} type="button" onClick={() => {
                  setSelectedDate(item.key);
                  setSelectedShowtime(null);
                  setLayout(null);
                  setSelectedSeats([]);
                }} disabled={Boolean(booking)} className={`min-w-24 rounded-xl border px-4 py-3 text-center disabled:opacity-50 ${selectedDate === item.key ? 'border-amber-500 bg-amber-500 text-black' : 'border-zinc-700 bg-zinc-950 text-zinc-300'}`}>
                  <span className="block text-xs font-bold capitalize">{item.weekday}</span>
                  <span className="mt-1 block text-sm font-black">{item.label}</span>
                  <span className="mt-1 block text-[10px]">{schedule[item.key].length} suất</span>
                </button>
              ))}
            </div>
            <div className="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
              {currentShowtimes.map(showtime => (
                <button key={showtime.showtimePublicId} type="button" onClick={() => loadSeats(showtime)} disabled={Boolean(booking)} className={`rounded-2xl border p-4 text-left disabled:opacity-50 ${selectedShowtime?.showtimePublicId === showtime.showtimePublicId ? 'border-amber-500 bg-amber-500/10' : 'border-zinc-700 bg-zinc-950/60 hover:border-zinc-500'}`}>
                  <div className="flex items-start justify-between gap-3">
                    <div><p className="font-black text-zinc-100">{showtime.movie?.title || 'Phim chưa có tên'}</p><p className="mt-1 text-xs text-zinc-500">{showtime.movieVersion?.versionName || showtime.movieVersion?.format || 'Định dạng tiêu chuẩn'}</p></div>
                    <span className="rounded-lg bg-amber-500 px-2.5 py-1 text-sm font-black text-black">{clock(showtime.startTime)}</span>
                  </div>
                  <div className="mt-4 flex flex-wrap gap-x-4 gap-y-2 text-xs text-zinc-400"><span className="flex items-center gap-1.5"><Clock3 size={14} /> {duration(showtime)} phút</span><span className="flex items-center gap-1.5"><Film size={14} /> {auditoriumLabel(showtime.auditorium?.name)}</span></div>
                </button>
              ))}
            </div>
          </>
        ) : (
          <div className="mt-5 rounded-2xl border border-dashed border-zinc-700 py-10 text-center text-sm text-zinc-500">Bảy ngày tới chưa có suất chiếu đang mở bán tại rạp này.</div>
        )}
      </section>

      {selectedShowtime ? (
        <div className="grid min-w-0 items-start gap-6 xl:grid-cols-[minmax(0,1fr)_380px]">
          <section className="min-w-0 space-y-8 rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5 md:p-7">
            <div>
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div><h2 className="flex items-center gap-2 font-black"><UserRound size={19} className="text-amber-500" /> 2. Khách đang phục vụ</h2><p className="mt-1 text-xs text-zinc-500">Ghi nhận thông tin để tra cứu đơn và hỗ trợ khách sau bán.</p></div>
                <div className="flex rounded-xl bg-zinc-950 p-1 text-xs font-black">
                  <button type="button" disabled={Boolean(booking)} onClick={() => changeCustomerMode('GUEST')} className={`rounded-lg px-4 py-2 ${customerMode === 'GUEST' ? 'bg-amber-500 text-black' : 'text-zinc-400'}`}>Khách lẻ</button>
                  <button type="button" disabled={Boolean(booking)} onClick={() => changeCustomerMode('MEMBER')} className={`rounded-lg px-4 py-2 ${customerMode === 'MEMBER' ? 'bg-amber-500 text-black' : 'text-zinc-400'}`}>Thành viên</button>
                </div>
              </div>

              {customerMode === 'GUEST' ? <div className="mt-5 grid gap-3 md:grid-cols-3">
                <label className="text-xs font-black text-zinc-400">Tên khách (không bắt buộc)<input disabled={Boolean(booking)} value={counterCustomer.fullName} onChange={event => setCounterCustomer(value => ({ ...value, fullName: event.target.value }))} placeholder="Ví dụ: Anh Minh" maxLength={150} className="mt-2 h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 text-sm font-normal text-white outline-none focus:border-amber-500" /></label>
                <label className="text-xs font-black text-zinc-400">Số điện thoại (không bắt buộc)<input disabled={Boolean(booking)} value={counterCustomer.phoneNumber} onChange={event => setCounterCustomer(value => ({ ...value, phoneNumber: event.target.value }))} placeholder="Ví dụ: 0901 234 567" maxLength={30} className="mt-2 h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 text-sm font-normal text-white outline-none focus:border-amber-500" /></label>
                <label className="text-xs font-black text-zinc-400">Email nhận hỗ trợ (không bắt buộc)<input disabled={Boolean(booking)} type="email" value={counterCustomer.email} onChange={event => setCounterCustomer(value => ({ ...value, email: event.target.value }))} placeholder="khachhang@email.com" maxLength={254} className="mt-2 h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 text-sm font-normal text-white outline-none focus:border-amber-500" /></label>
                <p className="text-xs leading-5 text-zinc-600 md:col-span-3">Vé vẫn được in tại quầy. Hệ thống không tự gửi vé vào email nhân viên.</p>
              </div> : <div className="mt-5 space-y-3">
                {counterCustomer.accountId ? <div className="flex flex-col gap-3 rounded-2xl border border-emerald-500/30 bg-emerald-500/[0.07] p-4 sm:flex-row sm:items-center sm:justify-between"><div><p className="font-black text-emerald-200">{counterCustomer.fullName}</p><p className="mt-1 text-xs text-emerald-100/60">{counterCustomer.customerCode} · {counterCustomer.phoneNumber || counterCustomer.email}</p></div><button type="button" disabled={Boolean(booking)} onClick={() => setCounterCustomer(emptyCounterCustomer())} className="rounded-xl border border-emerald-500/30 px-3 py-2 text-xs font-black text-emerald-200">Chọn thành viên khác</button></div> : <>
                  <form onSubmit={findCustomer} className="flex flex-col gap-2 sm:flex-row"><label className="relative flex-1"><Search className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" size={17} /><input value={customerQuery} onChange={event => setCustomerQuery(event.target.value)} placeholder="Tên, số điện thoại, email hoặc mã thành viên" className="h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 pl-11 pr-4 text-sm outline-none focus:border-amber-500" /></label><button disabled={customerSearching || Boolean(booking)} className="rounded-xl border border-amber-500/40 px-5 py-2 text-xs font-black text-amber-300">{customerSearching ? 'Đang tìm…' : 'Tìm thành viên'}</button></form>
                  {customerSearchMessage ? <p className="text-xs text-amber-300">{customerSearchMessage}</p> : null}
                  {customerResults.length ? <div className="grid gap-2 md:grid-cols-2">{customerResults.map(item => <button key={item.accountId} type="button" onClick={() => setCounterCustomer(item)} className="rounded-xl border border-zinc-700 bg-zinc-950 p-3 text-left hover:border-amber-500"><p className="text-sm font-black text-zinc-100">{item.fullName}</p><p className="mt-1 text-xs text-zinc-500">{item.customerCode} · {item.phoneNumber || item.email}</p></button>)}</div> : null}
                </>}
              </div>}
            </div>

            <div>
              <div className="flex items-center justify-between"><div><h2 className="flex items-center gap-2 font-black"><Armchair size={19} className="text-amber-500" /> 3. Chọn ghế</h2><p className="mt-1 text-xs text-zinc-500">Ghế mờ là ghế đã giữ, đã bán hoặc đang khóa vận hành.</p></div><span className="rounded-full bg-zinc-800 px-3 py-1 text-xs font-black text-zinc-300">Tối đa {layout?.maxSeatsPerBooking || 8} ghế</span></div>
              {seatLoading ? <div className="grid min-h-72 place-items-center"><LoaderCircle className="animate-spin text-amber-500" /></div> : layout ? <><div className="mx-auto mt-8 min-w-0 max-w-4xl"><div className="mx-auto mb-9 h-2 w-3/4 rounded-full bg-gradient-to-r from-transparent via-zinc-300 to-transparent shadow-[0_12px_30px_rgba(255,255,255,0.18)]" /><p className="-mt-5 mb-8 text-center text-[10px] font-black uppercase tracking-[0.28em] text-zinc-600">Màn hình</p><div className="max-w-full space-y-2 overflow-x-auto pb-3">{rows.map(([label, units]) => <div key={label} className="flex min-w-max items-center justify-center gap-2"><span className="w-7 text-center text-xs font-black text-zinc-500">{label}</span>{units.map(unit => { const selected = unit.seats.every(seat => selectedIds.has(seat.publicId)); return <button key={unit.key} type="button" disabled={!unit.sellable || !unit.pairValid || Boolean(booking)} onClick={() => toggleSeat(unit)} title={`${unit.seatCode} · ${money(unit.price)}`} className={`h-9 rounded-lg border px-2 text-[10px] font-black transition ${unit.isCouple ? 'min-w-20' : 'w-10'} ${selected ? 'border-amber-300 bg-amber-500 text-black' : seatTone(unit)}`}>{unit.seatCode}</button>; })}</div>)}</div></div><div className="mt-7 flex flex-wrap justify-center gap-4 text-xs text-zinc-400"><span className="flex items-center gap-2"><i className="h-3 w-3 rounded bg-zinc-800" /> Ghế thường</span><span className="flex items-center gap-2"><i className="h-3 w-3 rounded bg-amber-500/50" /> VIP</span><span className="flex items-center gap-2"><i className="h-3 w-3 rounded bg-pink-500/50" /> Ghế đôi</span><span className="flex items-center gap-2"><i className="h-3 w-3 rounded bg-amber-500" /> Đang chọn</span><span className="flex items-center gap-2"><i className="h-3 w-3 rounded bg-zinc-900" /> Không khả dụng</span></div></> : null}
            </div>

            {booking && !payment ? (
              <div className="border-t border-zinc-800 pt-7">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div><h2 className="flex items-center gap-2 font-black"><Popcorn size={19} className="text-amber-500" /> 4. Chọn bắp nước</h2><p className="mt-1 text-xs text-zinc-500">Có thể bỏ qua nếu khách chỉ mua vé. Giá và ưu đãi cập nhật ngay sau mỗi thay đổi.</p></div>
                  <span className="rounded-full bg-zinc-800 px-3 py-1 text-xs font-black text-zinc-300">{foodItems.reduce((sum, item) => sum + Number(item.quantity || 0), 0)} sản phẩm</span>
                </div>
                {concessionsLoading ? <div className="grid min-h-48 place-items-center"><LoaderCircle className="animate-spin text-amber-500" /></div> : concessions.length ? (
                  <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {concessions.map(item => {
                      const cartItem = foodByProductId.get(Number(item.id));
                      const quantity = Number(cartItem?.quantity || 0);
                      const busy = Number(cartBusyId) === Number(item.id);
                      return (
                        <article key={item.id} className={`overflow-hidden rounded-2xl border bg-zinc-950/70 ${quantity ? 'border-amber-500/60' : 'border-zinc-800'}`}>
                          <div className="aspect-[16/9] bg-zinc-900"><img src={item.imageUrl} alt={concessionName(item)} className="h-full w-full object-cover" /></div>
                          <div className="p-4"><p className="min-h-10 text-sm font-black text-zinc-100">{concessionName(item)}</p><p className="mt-2 text-base font-black text-amber-400">{money(item.price)}</p><div className="mt-4 flex items-center justify-between"><button type="button" aria-label={`Giảm ${concessionName(item)}`} onClick={() => changeFoodQuantity(item, -1)} disabled={!quantity || busy || checkoutLocked} className="grid h-9 w-9 place-items-center rounded-lg border border-zinc-700 disabled:opacity-30"><Minus size={16} /></button><span className="min-w-8 text-center font-black">{busy ? <LoaderCircle className="mx-auto animate-spin" size={17} /> : quantity}</span><button type="button" aria-label={`Thêm ${concessionName(item)}`} onClick={() => changeFoodQuantity(item, 1)} disabled={busy || checkoutLocked} className="grid h-9 w-9 place-items-center rounded-lg bg-amber-500 text-black disabled:opacity-40"><Plus size={16} /></button></div></div>
                        </article>
                      );
                    })}
                  </div>
                ) : <p className="mt-5 rounded-xl border border-dashed border-zinc-700 p-6 text-center text-sm text-zinc-500">Danh mục bắp nước đang tạm ngưng bán.</p>}
              </div>
            ) : null}
          </section>

          <aside className="space-y-4 rounded-3xl border border-amber-500/25 bg-zinc-900 p-6 xl:sticky xl:top-6">
            <div className="border-b border-zinc-800 pb-5"><p className="text-xs font-black uppercase tracking-widest text-zinc-500">Đơn tại quầy</p><h2 className="mt-2 text-lg font-black">{selectedShowtime.movie?.title}</h2><p className="mt-2 flex items-center gap-2 text-xs text-zinc-400"><MapPin size={14} /> {cinema?.name}</p><p className="mt-2 flex items-center gap-2 text-xs text-zinc-400"><Clock3 size={14} /> {clock(selectedShowtime.startTime)} · {auditoriumLabel(selectedShowtime.auditorium?.name)}</p></div>
            <div className="rounded-xl bg-zinc-950/70 p-3"><p className="text-[10px] font-black uppercase text-zinc-600">Khách đang phục vụ</p><p className="mt-1 text-sm font-black text-zinc-200">{counterCustomer.fullName || (customerMode === 'MEMBER' ? 'Chưa chọn thành viên' : 'Khách lẻ')}</p>{counterCustomer.customerCode || counterCustomer.phoneNumber ? <p className="mt-1 text-xs text-zinc-500">{counterCustomer.customerCode || counterCustomer.phoneNumber}</p> : null}</div>
            <div><p className="text-xs font-bold text-zinc-500">Ghế đã chọn ({selectedSeats.length})</p><div className="mt-2 flex min-h-10 flex-wrap gap-2">{selectedSeats.length ? selectedSeats.map(seat => <span key={seat.publicId} className="rounded-lg bg-zinc-800 px-2.5 py-1.5 text-xs font-black">{seat.seatCode}</span>) : <span className="text-sm text-zinc-600">Chưa chọn ghế</span>}</div></div>

            {booking ? (
              <div className="space-y-2 border-t border-zinc-800 pt-4 text-sm">
                <div className="flex justify-between text-zinc-400"><span>Tiền vé</span><strong className="text-zinc-200">{money(ticketAmount)}</strong></div>
                <div className="flex justify-between text-zinc-400"><span>Bắp nước</span><strong className="text-zinc-200">{money(foodAmount)}</strong></div>
                {promotionDiscount > 0 ? <div className="flex justify-between text-emerald-400"><span>Ưu đãi hệ thống</span><strong>-{money(promotionDiscount)}</strong></div> : null}
              </div>
            ) : null}

            {booking && !payment ? (
              <div className={`rounded-xl border p-3 text-xs ${promotionDiscount > 0 ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-200' : 'border-zinc-700 bg-zinc-950 text-zinc-400'}`}>
                <p className="flex items-center gap-2 font-black"><Sparkles size={15} /> {promotionLoading ? 'Đang kiểm tra ưu đãi hệ thống…' : promotionDiscount > 0 ? 'Đã tự động áp dụng ưu đãi tốt nhất' : 'Không có ưu đãi hệ thống phù hợp'}</p>
                {appliedPromotions.length ? <div className="mt-2 space-y-1">{appliedPromotions.map(item => <p key={item.promotionPublicId || item.name} className="flex items-center gap-1.5"><Check size={13} /> {item.name || item.promotionName || 'Ưu đãi tự động'}</p>)}</div> : null}
              </div>
            ) : null}

            <div className="flex items-end justify-between border-t border-zinc-800 pt-5"><span className="text-sm text-zinc-400">{booking ? 'Khách cần trả' : 'Tạm tính'}</span><strong className="text-2xl text-amber-400">{money(booking ? amountDue : selectedSeatTotal)}</strong></div>

            {!booking ? (
              <button type="button" onClick={createCounterBooking} disabled={!selectedSeats.length || submitting} className="flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-amber-500 font-black text-black disabled:opacity-40">{submitting ? <LoaderCircle className="animate-spin" size={19} /> : <Ticket size={19} />} Giữ ghế & chọn bắp nước</button>
            ) : !payment ? (
              <div className="space-y-3">
                <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/[0.05] p-3 text-xs text-emerald-300"><p className="font-black">Đã giữ ghế · {booking.bookingCode}</p><p className="mt-1 text-emerald-200/60">Kiểm tra ghế, bắp nước và ưu đãi trước khi chốt tiền.</p></div>
                <div className="rounded-xl border border-sky-500/25 bg-sky-500/[0.06] p-3 text-xs text-sky-200"><p className="flex items-center gap-2 font-black"><MailX size={15} /> Vé in trực tiếp tại quầy</p><p className="mt-1 text-sky-200/60">Không gửi vé vào email của tài khoản nhân viên.</p></div>
                <button type="button" onClick={lockAndOpenCashTransaction} disabled={submitting || promotionLoading || Boolean(cartBusyId)} className="flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-emerald-500 font-black text-black disabled:opacity-40">{submitting ? <LoaderCircle className="animate-spin" size={19} /> : <Banknote size={19} />} Chốt đơn & chuyển sang thu tiền</button>
                <button type="button" onClick={() => setConfirmCancel(true)} disabled={submitting} className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-rose-500/40 font-black text-rose-300 disabled:opacity-40"><XCircle size={18} /> Hủy đơn và trả ghế</button>
              </div>
            ) : !paid ? (
              <div className="space-y-4">
                <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/[0.05] p-3 text-xs text-emerald-300"><p className="font-black">Đã chốt số tiền · {booking.bookingCode}</p><p className="mt-1 text-emerald-200/60">Chỉ giao vé sau khi hệ thống ghi nhận đủ tiền.</p></div>
                <label className="block text-xs font-black text-zinc-300">5. Tiền khách đưa<input aria-label="Tiền khách đưa tại quầy" type="number" min={amountDue} step="1000" value={receivedAmount} onChange={event => setReceivedAmount(event.target.value)} className="mt-2 h-12 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 text-lg font-black outline-none focus:border-amber-500" /></label>
                <div className="flex items-center justify-between rounded-xl bg-zinc-950 p-4"><span className="text-sm text-zinc-400">Tiền thừa</span><strong className="text-lg text-emerald-400">{money(change)}</strong></div>
                <button type="button" onClick={() => setConfirmCollect(true)} disabled={submitting || Number(receivedAmount) < amountDue} className="flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-emerald-500 font-black text-black disabled:opacity-40"><Banknote size={19} /> Xác nhận đã nhận đủ tiền</button>
                <button type="button" onClick={() => setConfirmCancel(true)} disabled={submitting} className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-rose-500/40 font-black text-rose-300 disabled:opacity-40"><XCircle size={18} /> Hủy đơn chưa thu</button>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="rounded-2xl border border-emerald-500/30 bg-emerald-500/10 p-5 text-center"><CheckCircle2 className="mx-auto text-emerald-400" /><p className="mt-3 font-black text-emerald-200">Đã thanh toán và phát hành vé</p><p className="mt-1 text-xs text-emerald-200/60">Mã đơn {booking.bookingCode}</p></div>
                <section id="counter-cash-receipt" aria-label="Đối chiếu tiền mặt" className="space-y-4 rounded-2xl border border-zinc-700 bg-zinc-950/70 p-4 text-sm print:bg-white print:text-black">
                  <div className="hidden text-center print:block">
                    <p className="text-lg font-black tracking-widest">LORAFILM</p>
                    <p className="mt-1 text-xs font-black uppercase">Biên nhận bán vé tại quầy</p>
                    <p className="mt-1 text-[10px]">Mã đơn: {booking.bookingCode}</p>
                  </div>
                  <div className="hidden space-y-1 border-y border-black py-3 text-xs print:block">
                    <p><strong>Rạp:</strong> {cinema?.name}</p>
                    <p><strong>Phim:</strong> {selectedShowtime.movie?.title}</p>
                    <p><strong>Suất chiếu:</strong> {clock(selectedShowtime.startTime)} · {auditoriumLabel(selectedShowtime.auditorium?.name)}</p>
                    <p><strong>Ghế:</strong> {selectedSeats.map(seat => seat.seatCode).join(', ')}</p>
                    {foodItems.length ? <p><strong>Bắp nước:</strong> {foodItems.map(item => `${item.productName || item.name || 'Sản phẩm'} ×${item.quantity}`).join(', ')}</p> : null}
                  </div>
                  <div className="flex items-center justify-between">
                    <div><p className="font-black text-zinc-100 print:text-black">Đối chiếu tiền mặt</p><p className="mt-1 text-xs text-zinc-500 print:text-black">Kiểm tra trước khi giao biên nhận cho khách.</p></div>
                    <Banknote size={20} className="text-emerald-400 print:text-black" />
                  </div>
                  <div className="space-y-2 border-y border-zinc-800 py-3 print:border-black">
                    <div className="flex justify-between text-zinc-400 print:text-black"><span>Tổng tiền đơn</span><strong className="text-zinc-100 print:text-black">{money(collectedAmount)}</strong></div>
                    <div className="flex justify-between text-zinc-400 print:text-black"><span>Tiền khách đưa</span><strong className="text-zinc-100 print:text-black">{money(collectedReceivedAmount)}</strong></div>
                    <div className="flex justify-between text-emerald-400 print:text-black"><span>Tiền thối đã trả khách</span><strong>{money(collectedChangeAmount)}</strong></div>
                  </div>
                  <div className="space-y-1 text-xs text-zinc-500 print:text-black">
                    <p>Thu lúc: <strong className="text-zinc-300 print:text-black">{dateTime(payment?.collectedAt)}</strong></p>
                    <p>Phương thức: <strong className="text-zinc-300 print:text-black">{payment?.paymentMethod === 'FULL_DISCOUNT' ? 'Ưu đãi thanh toán toàn bộ' : 'Tiền mặt tại quầy'}</strong></p>
                    <p className="pt-1">Doanh thu ghi nhận là tổng tiền đơn; tiền thối không tính vào doanh thu.</p>
                  </div>
                  {tickets.length ? <div className="hidden space-y-1 border-t border-black pt-3 text-xs print:block">{tickets.map(item => <p key={`receipt-${item.publicId || item.ticketCode}`}><strong>Vé ghế {item.seatLabel}:</strong> {item.ticketCode}</p>)}</div> : null}
                </section>
                <div className="space-y-2">{tickets.length ? tickets.map(item => <div key={item.publicId || item.ticketCode} className="flex items-center justify-between rounded-xl bg-zinc-950 p-3"><span className="font-black">Ghế {item.seatLabel}</span><span className="font-mono text-xs text-zinc-400">{item.ticketCode}</span></div>) : <p className="rounded-xl bg-zinc-950 p-3 text-center text-xs text-zinc-500">Vé đang đồng bộ; có thể tra cứu lại bằng mã đơn.</p>}</div>
                <div className="rounded-xl border border-sky-500/25 bg-sky-500/[0.06] p-3 text-xs text-sky-200"><p className="flex items-center gap-2 font-black"><MailX size={15} /> Không gửi email nhân viên</p><p className="mt-1 text-sky-200/60">Giao vé in trực tiếp cho khách tại quầy.</p></div>
                <button type="button" onClick={printCounterReceipt} className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-zinc-600 font-black"><Printer size={18} /> In vé / biên nhận</button>
                <button type="button" onClick={resetSale} className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-white font-black text-black"><RotateCcw size={18} /> Bán đơn tiếp theo</button>
              </div>
            )}
          </aside>
        </div>
      ) : (
        <section className="grid min-h-64 place-items-center rounded-3xl border border-dashed border-zinc-800 text-center text-zinc-500"><div><Ticket className="mx-auto mb-4 h-12 w-12" /><p className="font-black text-zinc-300">Chọn một suất chiếu để bắt đầu bán vé</p><p className="mt-2 text-sm">Sơ đồ ghế và giá bán thực tế sẽ hiển thị ở đây.</p></div></section>
      )}

      <PaymentNoticeModal open={Boolean(notice)} {...notice} onClose={() => setNotice(null)} />
      <PaymentNoticeModal open={confirmCollect} tone="success" title="Xác nhận đã nhận đủ tiền?" message={`Nhận ${money(receivedAmount)}; trả khách ${money(change)} tiền thừa. Vé sẽ được phát hành để in tại quầy và không gửi vào email nhân viên.`} confirmLabel="Xác nhận đã thu" cancelLabel="Kiểm tra lại" busy={submitting} onConfirm={collect} onClose={() => setConfirmCollect(false)} />
      <PaymentNoticeModal open={confirmCancel} tone="danger" title="Hủy đơn chưa thu?" message="Giao dịch tiền mặt sẽ bị đóng, bắp nước bị bỏ và toàn bộ ghế đang giữ sẽ được trả lại để tiếp tục bán." confirmLabel="Hủy đơn và trả ghế" cancelLabel="Giữ lại đơn" busy={submitting} onConfirm={cancelCurrentSale} onClose={() => setConfirmCancel(false)} />
    </div>
  );
}
