import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowLeft,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Film,
  MapPin,
  Popcorn,
  Printer,
  ReceiptText,
  Sofa,
  Trash2,
  Mail
} from 'lucide-react';
import {
  cancelBooking,
  getBookingDetails,
  getBookingTickets,
  resendBookingEmail
} from '../services/bookingService';
import BookingCancellationModal from '../components/BookingCancellationModal';
import { getBookingErrorMessage } from '../utils/bookingErrorMessages';

const statusPresentation = {
  PENDING_PAYMENT: {
    label: 'Chờ thanh toán',
    className: 'border-amber-400/30 bg-amber-500/10 text-amber-300',
    title: 'Ghế đang được giữ cho bạn',
    description: 'Hoàn tất thanh toán trước khi đồng hồ kết thúc để giữ các ghế đã chọn.'
  },
  CONFIRMED: {
    label: 'Đã thanh toán',
    className: 'border-emerald-400/30 bg-emerald-500/10 text-emerald-300',
    title: 'Đặt vé thành công',
    description: 'Vé đã được xác nhận. Bạn có thể dùng mã vé bên dưới khi đến rạp.'
  },
  COMPLETED: {
    label: 'Đã hoàn thành',
    className: 'border-emerald-400/30 bg-emerald-500/10 text-emerald-300',
    title: 'Suất chiếu đã hoàn thành',
    description: 'Cảm ơn bạn đã sử dụng dịch vụ của LoraFilm.'
  },
  CANCELLED: {
    label: 'Đã hủy',
    className: 'border-red-400/30 bg-red-500/10 text-red-300',
    title: 'Đơn đã được hủy',
    description: 'Ghế của đơn này đã được trả lại và đơn không thể tiếp tục thanh toán.'
  },
  EXPIRED: {
    label: 'Hết hạn',
    className: 'border-zinc-600 bg-zinc-800/70 text-zinc-300',
    title: 'Thời gian giữ ghế đã kết thúc',
    description: 'Đơn không còn khả dụng để thanh toán. Bạn có thể chọn lại suất chiếu và ghế khác.'
  },
  REFUNDED: {
    label: 'Đã hoàn tiền',
    className: 'border-sky-400/30 bg-sky-500/10 text-sky-300',
    title: 'Đơn đã được ghi nhận hoàn tiền',
    description: 'Thông tin hoàn tiền sẽ được xử lý theo phương thức thanh toán của bạn.'
  }
};

const formatCurrency = value =>
  Number(value || 0).toLocaleString('vi-VN') + 'đ';

const formatDate = value => {
  if (!value) return 'Chưa có thông tin';
  return new Date(value).toLocaleDateString('vi-VN', {
    weekday: 'long',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  });
};

const formatTime = value => {
  if (!value) return '--:--';
  return new Date(value).toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit'
  });
};

export default function BookingDetailPage() {
  const { bookingId } = useParams();
  const navigate = useNavigate();
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [cancelling, setCancelling] = useState(false);
  const [timeLeft, setTimeLeft] = useState(null);
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [cancelError, setCancelError] = useState('');
  const [resendingEmail, setResendingEmail] = useState(false);
  const [resendMessage, setResendMessage] = useState('');
  const [resendError, setResendError] = useState('');

  const handleResendEmail = async () => {
    if (!bookingId) return;
    setResendingEmail(true);
    setResendMessage('');
    setResendError('');
    try {
      await resendBookingEmail(bookingId);
      setResendMessage('Đã gửi lại email vé thành công!');
      setTimeout(() => setResendMessage(''), 5000);
    } catch (err) {
      setResendError(err.response?.data?.message || 'Có lỗi xảy ra khi gửi lại email. Vui lòng thử lại.');
      setTimeout(() => setResendError(''), 5000);
    } finally {
      setResendingEmail(false);
    }
  };

  const fetchDetail = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getBookingDetails(bookingId);
      const tickets = await getBookingTickets(bookingId).catch(() => data.tickets || []);
      setBooking({
        ...data,
        tickets,
        finalAmount: data.finalAmount ?? data.totalAmount ?? 0,
        expiresAt: data.expiresAt ?? data.expiredAt ?? data.paymentDeadline,
        bookingStatus: data.bookingStatus ?? data.status
      });
    } catch (requestError) {
      setError(getBookingErrorMessage(
        requestError,
        'Không thể tải thông tin chi tiết đặt vé.'
      ));
    } finally {
      setLoading(false);
    }
  }, [bookingId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchDetail();
  }, [fetchDetail]);

  useEffect(() => {
    if (
      !booking
      || booking.bookingStatus !== 'PENDING_PAYMENT'
      || !booking.expiresAt
    ) {
      return undefined;
    }

    const calculateTimeLeft = () => {
      const remaining = new Date(booking.expiresAt).getTime() - Date.now();
      setTimeLeft(Math.max(0, Math.floor(remaining / 1000)));
    };
    calculateTimeLeft();
    const interval = window.setInterval(calculateTimeLeft, 1000);
    return () => window.clearInterval(interval);
  }, [booking]);

  const formattedTimeLeft = useMemo(() => {
    if (timeLeft === null) return '';
    const minutes = String(Math.floor(timeLeft / 60)).padStart(2, '0');
    const seconds = String(timeLeft % 60).padStart(2, '0');
    return `${minutes}:${seconds}`;
  }, [timeLeft]);

  const handleCancel = async () => {
    setCancelling(true);
    setCancelError('');
    try {
      await cancelBooking(bookingId, 'Khách hàng chủ động hủy từ chi tiết đơn');
      const refreshed = await getBookingDetails(bookingId);
      setBooking(current => ({
        ...current,
        ...refreshed,
        finalAmount: refreshed.finalAmount ?? refreshed.totalAmount ?? 0,
        expiresAt: refreshed.expiresAt ?? refreshed.expiredAt ?? refreshed.paymentDeadline,
        bookingStatus: refreshed.bookingStatus ?? refreshed.status
      }));
      setCancelModalOpen(false);
    } catch (requestError) {
      setCancelError(
        getBookingErrorMessage(
          requestError,
          'Không thể hủy đặt vé. Vui lòng thử lại.'
        )
      );
    } finally {
      setCancelling(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-950 text-white">
        <div className="flex flex-col items-center gap-4">
          <div className="h-11 w-11 animate-spin rounded-full border-4 border-brand-orange border-t-transparent" />
          <p className="text-sm font-semibold text-zinc-400">Đang tải thông tin đơn...</p>
        </div>
      </div>
    );
  }

  if (error || !booking) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-zinc-950 px-4 text-center">
        <div className="mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-red-500/10 text-red-400">
          <AlertTriangle className="h-8 w-8" />
        </div>
        <h1 className="text-xl font-black text-white">Không thể mở đơn đặt vé</h1>
        <p className="mt-2 max-w-md text-sm text-zinc-400">
          {error || 'Đơn không tồn tại hoặc bạn không có quyền xem đơn này.'}
        </p>
        <Link
          to="/bookings"
          className="mt-7 rounded-xl bg-brand-orange px-6 py-3 text-xs font-black uppercase text-white"
        >
          Quay lại lịch sử đặt vé
        </Link>
      </div>
    );
  }

  const presentation = booking.presentation || booking.snapshot || {};
  const seats = Array.isArray(presentation.seats) ? presentation.seats : [];
  const foodOrder = booking.food || booking.foodOrder;
  const foodItems = Array.isArray(foodOrder?.items) ? foodOrder.items : [];
  const tickets = Array.isArray(booking.tickets) ? booking.tickets : [];
  const currentStatus = booking.bookingStatus || booking.status;
  const status = statusPresentation[currentStatus] || {
    label: 'Đang cập nhật',
    className: 'border-zinc-700 bg-zinc-800 text-zinc-300',
    title: 'Trạng thái đơn đã được cập nhật',
    description: 'Xem thông tin chi tiết của đơn bên dưới.'
  };
  const canRecover = currentStatus === 'PENDING_PAYMENT' && timeLeft > 0;
  const showtimeStart = presentation.showtimeStart;
  const amountLabel = ['CONFIRMED', 'COMPLETED', 'REFUNDED'].includes(currentStatus)
    ? 'Tổng tiền đã thanh toán'
    : 'Tổng giá trị đơn';

  return (
    <main className="min-h-screen bg-zinc-950 px-4 pb-16 pt-28 text-zinc-100 md:px-8 print:bg-white print:pt-4 print:text-black">
      {cancelModalOpen && (
        <BookingCancellationModal
          bookingCode={booking.bookingCode}
          error={cancelError}
          pending={cancelling}
          onClose={() => {
            setCancelError('');
            setCancelModalOpen(false);
          }}
          onConfirm={handleCancel}
        />
      )}

      <div className="mx-auto max-w-6xl space-y-6">
        <div className="flex items-center justify-between gap-3 print:hidden">
          <Link
            to="/bookings"
            className="flex items-center gap-2 text-xs font-bold text-zinc-400 transition-colors hover:text-brand-orange"
          >
            <ArrowLeft className="h-4 w-4" />
            Lịch sử đặt vé
          </Link>
          <button
            type="button"
            onClick={() => window.print()}
            className="flex items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-2.5 text-xs font-bold text-zinc-300 hover:bg-zinc-800"
          >
            <Printer className="h-4 w-4" />
            In thông tin đơn
          </button>
        </div>

        <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900 shadow-2xl print:border-black print:bg-white">
          <div className="grid md:grid-cols-[220px_1fr]">
            <div className="relative min-h-64 overflow-hidden bg-zinc-800 md:min-h-[310px]">
              <div className="absolute inset-0 flex items-center justify-center text-zinc-600">
                <Film className="h-14 w-14" />
              </div>
              {(presentation.moviePosterUrl || presentation.moviePoster) && (
                <img
                  src={presentation.moviePosterUrl || presentation.moviePoster}
                  alt={`Poster ${presentation.movieTitle || 'phim'}`}
                  className="absolute inset-0 h-full w-full object-cover"
                  onError={event => {
                    event.currentTarget.style.display = 'none';
                  }}
                />
              )}
              <div className="absolute inset-x-0 bottom-0 h-28 bg-gradient-to-t from-zinc-950 to-transparent md:hidden" />
            </div>

            <div className="flex flex-col justify-between p-6 md:p-8">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <span className={`rounded-full border px-3 py-1 text-[10px] font-black uppercase ${status.className}`}>
                    {status.label}
                  </span>
                  <span className="text-[10px] font-bold uppercase tracking-wider text-zinc-500">
                    Mã đơn: {booking.bookingCode}
                  </span>
                </div>
                <h1 className="mt-4 text-2xl font-black leading-tight text-white md:text-3xl print:text-black">
                  {presentation.movieTitle || 'Thông tin phim đang được cập nhật'}
                </h1>

                <div className="mt-6 grid gap-4 sm:grid-cols-2">
                  <div className="flex gap-3">
                    <MapPin className="mt-0.5 h-5 w-5 shrink-0 text-brand-orange" />
                    <div>
                      <p className="text-[10px] font-black uppercase tracking-wider text-zinc-500">Rạp và phòng chiếu</p>
                      <p className="mt-1 text-sm font-bold text-zinc-100 print:text-black">
                        {presentation.cinemaName || 'Chưa có thông tin rạp'}
                      </p>
                      <p className="mt-0.5 text-xs text-zinc-400">
                        {presentation.auditoriumName || 'Chưa có thông tin phòng'}
                      </p>
                    </div>
                  </div>
                  <div className="flex gap-3">
                    <CalendarDays className="mt-0.5 h-5 w-5 shrink-0 text-brand-orange" />
                    <div>
                      <p className="text-[10px] font-black uppercase tracking-wider text-zinc-500">Suất chiếu</p>
                      <p className="mt-1 text-sm font-bold text-zinc-100 print:text-black">
                        {formatTime(showtimeStart)} · {formatDate(showtimeStart)}
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              <div className="mt-7 border-t border-zinc-800 pt-5 print:border-black">
                <div className="flex items-start gap-3">
                  <Sofa className="mt-0.5 h-5 w-5 shrink-0 text-brand-orange" />
                  <div>
                    <p className="text-[10px] font-black uppercase tracking-wider text-zinc-500">Ghế đã chọn</p>
                    <div className="mt-2 flex flex-wrap gap-2">
                      {seats.length > 0 ? seats.map((seat, index) => (
                        <span
                          key={seat.seatPublicId || `${seat.label}-${index}`}
                          className="rounded-lg border border-brand-orange/25 bg-brand-orange/10 px-3 py-1.5 text-xs font-black text-brand-orange"
                        >
                          {seat.label}
                          {seat.type ? <span className="ml-1 font-semibold text-zinc-500">· {seat.type}</span> : null}
                        </span>
                      )) : (
                        <span className="text-sm text-zinc-500">Chưa có thông tin ghế</span>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className={`rounded-2xl border p-5 ${status.className} print:border-black print:bg-white print:text-black`}>
          <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
            <div className="flex items-start gap-3">
              {currentStatus === 'CONFIRMED' || currentStatus === 'COMPLETED' ? (
                <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0" />
              ) : currentStatus === 'PENDING_PAYMENT' ? (
                <Clock3 className="mt-0.5 h-5 w-5 shrink-0" />
              ) : (
                <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0" />
              )}
              <div>
                <h2 className="text-sm font-black">{status.title}</h2>
                <p className="mt-1 text-xs leading-5 opacity-80">{status.description}</p>
              </div>
            </div>

            {canRecover && (
              <div className="flex flex-wrap items-center gap-2 print:hidden">
                <div className="rounded-xl border border-current/20 px-4 py-2 text-center">
                  <p className="text-[9px] font-black uppercase opacity-70">Còn lại</p>
                  <p className="text-base font-black tracking-wider">{formattedTimeLeft}</p>
                </div>
                <button
                  type="button"
                  onClick={() => navigate(`/bookings/checkout?bookingId=${encodeURIComponent(bookingId)}`)}
                  className="rounded-xl bg-brand-orange px-5 py-3 text-xs font-black uppercase text-white shadow-lg shadow-brand-orange/20"
                >
                  Tiếp tục thanh toán
                </button>
                <button
                  type="button"
                  disabled={cancelling}
                  onClick={() => {
                    setCancelError('');
                    setCancelModalOpen(true);
                  }}
                  className="flex items-center gap-2 rounded-xl border border-red-400/30 px-4 py-3 text-xs font-black uppercase text-red-300 hover:bg-red-500/10"
                >
                  <Trash2 className="h-4 w-4" />
                  Hủy giữ ghế
                </button>
              </div>
            )}

            {(currentStatus === 'CONFIRMED' || currentStatus === 'COMPLETED') && (
              <div className="flex flex-col sm:flex-row items-center gap-3 print:hidden">
                {resendMessage && (
                  <span className="text-xs font-semibold text-emerald-400">{resendMessage}</span>
                )}
                {resendError && (
                  <span className="text-xs font-semibold text-red-400">{resendError}</span>
                )}
                <button
                  type="button"
                  disabled={resendingEmail}
                  onClick={handleResendEmail}
                  className="flex items-center gap-2 rounded-xl bg-brand-orange px-5 py-3 text-xs font-black uppercase text-white shadow-lg shadow-brand-orange/20 disabled:opacity-50"
                >
                  <Mail className="h-4 w-4" />
                  {resendingEmail ? 'Đang gửi...' : 'Gửi lại email vé'}
                </button>
              </div>
            )}
          </div>
        </section>

        <div className="grid gap-6 lg:grid-cols-[1.35fr_0.85fr]">
          <div className="space-y-6">
            {foodItems.length > 0 && (
              <section className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6 print:border-black print:bg-white">
                <div className="flex items-center gap-3">
                  <div className="rounded-xl bg-brand-orange/10 p-2.5 text-brand-orange">
                    <Popcorn className="h-5 w-5" />
                  </div>
                  <div>
                    <h2 className="font-black text-white print:text-black">Bắp nước đi kèm</h2>
                    <p className="text-xs text-zinc-500">{foodOrder.totalQuantity || 0} sản phẩm</p>
                  </div>
                </div>
                <div className="mt-5 divide-y divide-zinc-800 border-t border-zinc-800 print:divide-black print:border-black">
                  {foodItems.map((item, index) => (
                    <div key={`${item.name || item.productName}-${index}`} className="flex items-center justify-between gap-4 py-4">
                      <div>
                        <p className="text-sm font-bold text-zinc-100 print:text-black">
                          {item.name || item.productName}
                        </p>
                        <p className="mt-1 text-xs text-zinc-500">
                          {item.quantity} × {formatCurrency(item.unitPrice)}
                        </p>
                      </div>
                      <span className="text-sm font-black text-white print:text-black">
                        {formatCurrency(item.totalAmount ?? item.finalAmount)}
                      </span>
                    </div>
                  ))}
                </div>
              </section>
            )}

            {tickets.length > 0 && (
              <section className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6 print:border-black print:bg-white">
                <h2 className="font-black text-white print:text-black">Vé xem phim</h2>
                <p className="mt-1 text-xs text-zinc-500">Xuất trình mã vé khi đến rạp</p>
                <div className="mt-5 grid gap-4 sm:grid-cols-2">
                  {tickets.map(ticket => (
                    <div key={ticket.publicId || ticket.id} className="flex items-center justify-between gap-4 rounded-2xl border border-zinc-800 bg-zinc-950/50 p-4 print:border-black print:bg-white">
                      <div>
                        <p className="text-[10px] font-black uppercase tracking-wider text-zinc-500">Ghế</p>
                        <p className="mt-1 text-lg font-black text-brand-orange">{ticket.seatLabel}</p>
                        <p className="mt-2 text-[10px] text-zinc-500">Mã vé: {ticket.ticketCode}</p>
                      </div>
                      <img
                        src={`https://api.qrserver.com/v1/create-qr-code/?size=120x120&data=${encodeURIComponent(ticket.ticketCode)}`}
                        alt={`Mã QR vé ${ticket.ticketCode}`}
                        className="h-20 w-20 rounded-lg bg-white p-1"
                      />
                    </div>
                  ))}
                </div>
              </section>
            )}
          </div>

          <section className="h-fit rounded-3xl border border-zinc-800 bg-zinc-900 p-6 print:border-black print:bg-white">
            <div className="flex items-center gap-3">
              <div className="rounded-xl bg-brand-orange/10 p-2.5 text-brand-orange">
                <ReceiptText className="h-5 w-5" />
              </div>
              <div>
                <h2 className="font-black text-white print:text-black">Chi tiết thanh toán</h2>
                <p className="text-xs text-zinc-500">
                  Đặt lúc {new Date(booking.createdAt).toLocaleString('vi-VN')}
                </p>
              </div>
            </div>

            <div className="mt-6 space-y-3 text-sm">
              <div className="flex justify-between gap-4 text-zinc-400">
                <span>Tiền vé ({seats.length} ghế)</span>
                <span className="font-bold text-zinc-200 print:text-black">{formatCurrency(booking.ticketAmount)}</span>
              </div>
              <div className="flex justify-between gap-4 text-zinc-400">
                <span>Bắp nước</span>
                <span className="font-bold text-zinc-200 print:text-black">
                  {formatCurrency(booking.foodAmount ?? foodOrder?.totalAmount)}
                </span>
              </div>
              {Number(booking.serviceFee || 0) > 0 && (
                <div className="flex justify-between gap-4 text-zinc-400">
                  <span>Phí dịch vụ</span>
                  <span className="font-bold text-zinc-200 print:text-black">{formatCurrency(booking.serviceFee)}</span>
                </div>
              )}
              {Number(booking.taxAmount || 0) > 0 && (
                <div className="flex justify-between gap-4 text-zinc-400">
                  <span>Thuế</span>
                  <span className="font-bold text-zinc-200 print:text-black">{formatCurrency(booking.taxAmount)}</span>
                </div>
              )}
              {Number(booking.promotionDiscount || 0) + Number(booking.voucherDiscount || 0) > 0 && (
                <div className="flex justify-between gap-4 text-emerald-400">
                  <span>Ưu đãi</span>
                  <span className="font-bold">
                    -{formatCurrency(
                      Number(booking.promotionDiscount || 0)
                      + Number(booking.voucherDiscount || 0)
                    )}
                  </span>
                </div>
              )}
            </div>

            <div className="mt-6 border-t border-zinc-800 pt-5 print:border-black">
              <p className="text-[10px] font-black uppercase tracking-wider text-zinc-500">{amountLabel}</p>
              <p className="mt-1 text-3xl font-black text-brand-orange">
                {formatCurrency(booking.finalAmount)}
              </p>
            </div>

            {(booking.paymentMethodSnapshot || booking.paymentReference) && (
              <div className="mt-5 rounded-2xl bg-zinc-950/50 p-4 text-xs text-zinc-400 print:bg-white">
                {booking.paymentMethodSnapshot && (
                  <p>Phương thức: <strong className="text-zinc-200 print:text-black">{booking.paymentMethodSnapshot}</strong></p>
                )}
                {booking.paymentReference && (
                  <p className="mt-2">Mã giao dịch: <strong className="text-zinc-200 print:text-black">{booking.paymentReference}</strong></p>
                )}
              </div>
            )}
          </section>
        </div>
      </div>
    </main>
  );
}
