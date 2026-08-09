import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ArrowLeft, Banknote, CalendarClock, CheckCircle2, Film, LoaderCircle,
  MapPin, Popcorn, Printer, RotateCcw, Ticket, User,
} from 'lucide-react';
import TicketQrCode from '@/features/booking/customer/components/TicketQrCode';
import { useNavigate, useParams } from 'react-router-dom';
import { getBookingDetails, getBookingTickets } from '@/features/booking/customer/services/bookingService';
import { getPaymentsForBooking } from '@/features/payment/services/paymentService';
import {
  auditoriumLabel, bookingStatus, clock, dateTime, money, productName,
} from '../employeePresentation';

const paymentLabel = value => ({
  CASH: 'Tiền mặt tại quầy',
  VNPAY: 'VNPay',
  MOMO: 'MoMo',
  FULL_DISCOUNT: 'Ưu đãi thanh toán toàn bộ',
}[value] || 'Chưa ghi nhận');

export default function EmployeeOrderDetailPage() {
  const { bookingPublicId } = useParams();
  const navigate = useNavigate();
  const [detail, setDetail] = useState(null);
  const [tickets, setTickets] = useState([]);
  const [payment, setPayment] = useState(null);
  const [state, setState] = useState({ loading: true, error: '' });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const [bookingResult, ticketResult, paymentResult] = await Promise.allSettled([
        getBookingDetails(bookingPublicId),
        getBookingTickets(bookingPublicId),
        getPaymentsForBooking(bookingPublicId),
      ]);
      if (bookingResult.status !== 'fulfilled') throw bookingResult.reason;
      setDetail(bookingResult.value);
      setTickets(ticketResult.status === 'fulfilled' ? ticketResult.value || [] : []);
      setPayment(paymentResult.status === 'fulfilled'
        ? paymentResult.value?.content?.find(item => item.status === 'SUCCESS')
          || paymentResult.value?.content?.[0]
          || null
        : null);
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải chi tiết đơn tại quầy.' });
    }
  }, [bookingPublicId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const foodItems = detail?.foodOrder?.items || [];
  const statusView = bookingStatus(detail?.status);
  const discount = useMemo(() => Number(detail?.promotionDiscount || 0)
    + Number(detail?.voucherDiscount || 0)
    + Number(detail?.scoreDiscount || 0), [detail]);

  const printReceipt = () => {
    const className = 'printing-employee-order';
    const cleanup = () => document.body.classList.remove(className);
    document.body.classList.add(className);
    window.addEventListener('afterprint', cleanup, { once: true });
    window.print();
    window.setTimeout(cleanup, 1000);
  };

  if (state.loading) return <div className="flex min-h-[60vh] items-center justify-center gap-3 text-zinc-500"><LoaderCircle className="animate-spin" /> Đang tải chi tiết đơn…</div>;
  if (state.error || !detail) return <div className="mx-auto max-w-xl rounded-3xl border border-red-500/25 bg-red-500/10 p-8 text-center text-red-200"><p className="font-black">{state.error || 'Không tìm thấy đơn'}</p><button type="button" onClick={() => navigate('/employee/orders')} className="mt-4 rounded-xl border border-red-300/30 px-4 py-2 text-sm font-black">Quay lại danh sách</button></div>;

  return (
    <section className="mx-auto w-full max-w-7xl space-y-6 text-white">
      <button type="button" onClick={() => navigate('/employee/orders')} className="flex items-center gap-2 text-sm font-black text-zinc-400 hover:text-white"><ArrowLeft size={17} /> Quay lại danh sách đơn</button>

      <header className="rounded-3xl border border-zinc-800 bg-zinc-900/70 p-6 md:p-8">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
          <div><p className="text-xs font-black uppercase tracking-[0.2em] text-zinc-500">Mã đơn tại quầy</p><h1 className="mt-2 text-3xl font-black">{detail.bookingCode}</h1><div className="mt-3 flex flex-wrap items-center gap-2"><span className={`rounded-full border px-3 py-1 text-xs font-black ${statusView.tone}`}>{statusView.label}</span><span className="text-xs text-zinc-500">Tạo lúc {dateTime(detail.createdAt)}</span></div></div>
          <div className="text-left lg:text-right"><p className="text-xs font-black uppercase text-zinc-500">Khách đã thanh toán</p><p className="mt-2 text-3xl font-black text-amber-400">{money(detail.totalAmount, detail.currency)}</p></div>
        </div>
      </header>

      <div className="flex flex-wrap gap-3">
        {detail.status === 'PENDING_PAYMENT' ? <button type="button" onClick={() => navigate(`/employee/payments/cash?reference=${encodeURIComponent(detail.bookingCode)}`)} className="rounded-xl bg-emerald-500 px-5 py-3 text-sm font-black text-black">Tiếp tục thu tiền</button> : null}
        {tickets.length ? <button type="button" onClick={printReceipt} className="flex items-center gap-2 rounded-xl bg-amber-500 px-5 py-3 text-sm font-black text-black"><Printer size={17} /> In lại vé và biên nhận</button> : null}
        {['CONFIRMED', 'COMPLETED'].includes(detail.status) && payment ? <button type="button" onClick={() => navigate(`/employee/payments/refunds?reference=${encodeURIComponent(payment.paymentTransactionCode || payment.paymentPublicId)}`)} className="flex items-center gap-2 rounded-xl border border-red-500/40 px-5 py-3 text-sm font-black text-red-300"><RotateCcw size={17} /> Tạo yêu cầu hoàn tiền</button> : null}
      </div>

      <div id="employee-order-receipt" className="space-y-6">
        <div className="hidden border-b border-black pb-4 text-center print:block"><p className="text-xl font-black tracking-widest">LORAFILM</p><p className="mt-1 text-sm font-black uppercase">Vé và biên nhận tại quầy</p><p className="mt-1 text-xs">{detail.bookingCode}</p></div>

        <div className="grid gap-6 xl:grid-cols-[1.15fr_.85fr]">
          <article className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-6 print:border-black print:bg-white">
            <div className="flex items-center gap-2 border-b border-zinc-800 pb-4 print:border-black"><Film className="text-amber-500 print:text-black" size={20} /><h2 className="font-black">Phim và suất chiếu</h2></div>
            <div className="mt-5 grid gap-5 sm:grid-cols-[110px_1fr]">
              {detail.posterUrl ? <img src={detail.posterUrl} alt="" className="h-40 w-28 rounded-xl object-cover print:hidden" /> : null}
              <dl className="space-y-3 text-sm">
                <div><dt className="text-xs text-zinc-500 print:text-black">Phim</dt><dd className="mt-1 font-black">{detail.movieTitle || 'Chưa có tên phim'}</dd></div>
                <div className="flex gap-2"><MapPin size={16} className="mt-0.5 text-zinc-500 print:text-black" /><div><dt className="text-xs text-zinc-500 print:text-black">Rạp / phòng</dt><dd className="mt-1 font-bold">{detail.cinemaName || 'Rạp đang làm việc'} · {auditoriumLabel(detail.auditoriumName)}</dd></div></div>
                <div className="flex gap-2"><CalendarClock size={16} className="mt-0.5 text-zinc-500 print:text-black" /><div><dt className="text-xs text-zinc-500 print:text-black">Suất chiếu</dt><dd className="mt-1 font-bold">{dateTime(detail.showtimeStart)} – {clock(detail.showtimeEnd)}</dd></div></div>
                <div><dt className="text-xs text-zinc-500 print:text-black">Ghế</dt><dd className="mt-1 font-black text-amber-300 print:text-black">{detail.seatNames || 'Chưa ghi nhận'}</dd></div>
              </dl>
            </div>
          </article>

          <article className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-6 print:border-black print:bg-white">
            <div className="flex items-center gap-2 border-b border-zinc-800 pb-4 print:border-black"><Banknote className="text-emerald-400 print:text-black" size={20} /><h2 className="font-black">Thanh toán</h2></div>
            <dl className="mt-5 space-y-3 text-sm">
              <div className="flex justify-between gap-4"><dt className="text-zinc-500 print:text-black">Tiền vé</dt><dd className="font-bold">{money(detail.ticketAmount, detail.currency)}</dd></div>
              <div className="flex justify-between gap-4"><dt className="text-zinc-500 print:text-black">Bắp nước</dt><dd className="font-bold">{money(detail.foodAmount, detail.currency)}</dd></div>
              <div className="flex justify-between gap-4"><dt className="text-zinc-500 print:text-black">Phí và thuế</dt><dd className="font-bold">{money(Number(detail.serviceFee || 0) + Number(detail.taxAmount || 0), detail.currency)}</dd></div>
              <div className="flex justify-between gap-4 text-emerald-300 print:text-black"><dt>Ưu đãi</dt><dd className="font-bold">-{money(discount, detail.currency)}</dd></div>
              <div className="flex justify-between gap-4 border-t border-zinc-700 pt-3 text-lg print:border-black"><dt className="font-black">Tổng thanh toán</dt><dd className="font-black text-amber-400 print:text-black">{money(detail.totalAmount, detail.currency)}</dd></div>
              {payment?.receivedAmount != null ? <><div className="flex justify-between gap-4"><dt className="text-zinc-500 print:text-black">Tiền khách đưa</dt><dd className="font-bold">{money(payment.receivedAmount, payment.currency)}</dd></div><div className="flex justify-between gap-4"><dt className="text-zinc-500 print:text-black">Tiền thối đã trả khách</dt><dd className="font-bold">{money(payment.changeAmount, payment.currency)}</dd></div></> : null}
              <div className="flex justify-between gap-4"><dt className="text-zinc-500 print:text-black">Phương thức</dt><dd className="font-bold">{paymentLabel(payment?.provider || detail.paymentMethodSnapshot)}</dd></div>
            </dl>
          </article>

          <article className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-6 print:border-black print:bg-white">
            <div className="flex items-center gap-2 border-b border-zinc-800 pb-4 print:border-black"><User className="text-sky-400 print:text-black" size={20} /><h2 className="font-black">Khách được phục vụ</h2></div>
            <dl className="mt-5 space-y-3 text-sm">
              <div><dt className="text-xs text-zinc-500 print:text-black">Tên khách</dt><dd className="mt-1 font-black">{detail.counterCustomerName || 'Khách lẻ'}</dd></div>
              {detail.counterCustomerPhone ? <div><dt className="text-xs text-zinc-500 print:text-black">Số điện thoại</dt><dd className="mt-1 font-bold">{detail.counterCustomerPhone}</dd></div> : null}
              {detail.counterCustomerEmail ? <div><dt className="text-xs text-zinc-500 print:text-black">Email liên hệ</dt><dd className="mt-1 break-all font-bold">{detail.counterCustomerEmail}</dd></div> : null}
              {detail.counterCustomerAccountId ? <div><dt className="text-xs text-zinc-500 print:text-black">Loại khách</dt><dd className="mt-1 font-bold text-emerald-300 print:text-black">Thành viên LoraFilm</dd></div> : <p className="text-xs leading-5 text-zinc-500 print:text-black">Không gắn tài khoản thành viên cho đơn này.</p>}
            </dl>
          </article>
        </div>

        {foodItems.length ? <article className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-6 print:border-black print:bg-white"><div className="flex items-center gap-2 border-b border-zinc-800 pb-4 print:border-black"><Popcorn className="text-amber-500 print:text-black" size={20} /><h2 className="font-black">Bắp nước đã mua</h2></div><div className="mt-4 divide-y divide-zinc-800 print:divide-black">{foodItems.map(item => <div key={item.id || item.productId} className="flex justify-between py-3 text-sm"><span>{productName(item.name || item.productName)} × {item.quantity}</span><strong>{money(item.finalAmount || Number(item.unitPrice || 0) * Number(item.quantity || 0), detail.currency)}</strong></div>)}</div></article> : null}

        <article className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-6 print:border-black print:bg-white">
          <div className="flex items-center gap-2 border-b border-zinc-800 pb-4 print:border-black"><Ticket className="text-sky-400 print:text-black" size={20} /><h2 className="font-black">Vé đã phát hành ({tickets.length})</h2></div>
          {tickets.length ? <div className="mt-4 grid gap-3 sm:grid-cols-2">{tickets.map(ticketItem => <div key={ticketItem.publicId || ticketItem.ticketCode} className="flex items-center gap-4 rounded-xl border border-zinc-800 bg-zinc-950 p-4 print:border-black print:bg-white"><TicketQrCode ticketCode={ticketItem.qrCode || ticketItem.ticketCode} size={140} className="h-24 w-24 shrink-0 rounded-lg bg-white p-1" /><div className="min-w-0"><div className="flex items-center gap-2"><p className="font-black">Ghế {ticketItem.seatLabel}</p><CheckCircle2 size={17} className="text-emerald-400 print:text-black" /></div><p className="mt-2 break-all font-mono text-xs text-zinc-500 print:text-black">{ticketItem.ticketCode}</p><p className="mt-2 text-[10px] text-zinc-600 print:text-black">Quét QR này tại cửa phòng chiếu.</p></div></div>)}</div> : <p className="py-8 text-center text-sm text-zinc-500 print:text-black">Đơn chưa phát hành vé.</p>}
        </article>

        <div className="hidden border-t border-black pt-4 text-xs print:block"><p><strong>Thời điểm in:</strong> {dateTime(new Date())}</p><p className="mt-1">Vui lòng giữ vé để xuất trình khi vào phòng chiếu.</p></div>
      </div>

      <article className="rounded-2xl border border-sky-500/20 bg-sky-500/[0.06] p-4 text-sm text-sky-200"><div className="flex gap-3"><User size={19} className="shrink-0" /><p>Nhân viên chỉ xem các đơn do chính tài khoản quầy này tạo. Quản lý rạp xem bức tranh toàn rạp tại màn hình quản lý đơn.</p></div></article>
    </section>
  );
}
