import { useState, useEffect, useMemo, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Printer, ArrowLeft, Trash2, Clock, AlertTriangle, CheckCircle2 } from 'lucide-react';
import { getBookingDetails, getBookingTickets, cancelBooking, finalizeCheckout } from '../services/bookingService';
import { getBookingFoodOrder } from '../services/foodService';

export default function BookingDetailPage() {
  const { bookingId } = useParams();
  const navigate = useNavigate();

  // States
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [cancelling, setCancelling] = useState(false);
  const [paymentLoading, setPaymentLoading] = useState(false);
  const [timeLeft, setTimeLeft] = useState(null);

  const fetchDetail = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getBookingDetails(bookingId);
      const [tickets, foodOrder] = await Promise.all([
        getBookingTickets(bookingId).catch(() => data.tickets || []),
        getBookingFoodOrder(bookingId).catch(() => data.foodOrder || null)
      ]);
      setBooking({
        ...data,
        tickets,
        foodOrder,
        finalAmount: data.finalAmount ?? data.totalAmount ?? 0,
        ticketAmount: data.ticketAmount ?? data.totalAmount ?? 0,
        expiresAt: data.expiresAt ?? data.expiredAt ?? data.paymentDeadline,
        bookingStatus: data.bookingStatus ?? data.status
      });
    } catch (err) {
      setError(err.message || err.detail || "Không thể tải thông tin chi tiết đặt vé.");
    } finally {
      setLoading(false);
    }
  }, [bookingId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchDetail();
  }, [fetchDetail]);

  // Expiration countdown logic
  useEffect(() => {
    if (!booking || booking.bookingStatus !== 'PENDING_PAYMENT' || !booking.expiresAt) return;

    const calculateTimeLeft = () => {
      const diff = new Date(booking.expiresAt) - new Date();
      if (diff <= 0) {
        setTimeLeft(0);
        return;
      }
      setTimeLeft(Math.floor(diff / 1000));
    };

    calculateTimeLeft();
    const interval = setInterval(calculateTimeLeft, 1000);

    return () => clearInterval(interval);
  }, [booking]);

  const handleCancel = async () => {
    const reason = prompt("Vui lòng nhập lý do hủy đặt vé:");
    if (reason === null) return; // Cancel prompt

    setCancelling(true);
    try {
      await cancelBooking(bookingId, reason || "Khách hàng chủ động hủy");
      // Refresh details
      const freshData = await getBookingDetails(bookingId);
      setBooking(prev => ({
        ...prev,
        ...freshData,
        finalAmount: freshData.finalAmount ?? freshData.totalAmount ?? 0,
        ticketAmount: freshData.ticketAmount ?? freshData.totalAmount ?? 0,
        expiresAt: freshData.expiresAt ?? freshData.expiredAt ?? freshData.paymentDeadline,
        bookingStatus: freshData.bookingStatus ?? freshData.status
      }));
    } catch (err) {
      alert("Không thể hủy đặt vé: " + (err.message || "Lỗi kết nối"));
    } finally {
      setCancelling(false);
    }
  };

  const handlePayNow = async () => {
    setPaymentLoading(true);
    try {
      await finalizeCheckout(bookingId);
      alert(`Booking is ready for Payment Service: ${bookingId}`);
    } catch (err) {
      alert("Lỗi thanh toán: " + (err.message || "Lỗi kết nối"));
    } finally {
      setPaymentLoading(false);
    }
  };

  // Format countdown string
  const formattedTimeLeft = useMemo(() => {
    if (timeLeft === null) return '';
    const mins = String(Math.floor(timeLeft / 60)).padStart(2, '0');
    const secs = String(timeLeft % 60).padStart(2, '0');
    return `${mins}:${secs}`;
  }, [timeLeft]);

  const formatCurrency = (val) => {
    return (val || 0).toLocaleString('vi-VN') + 'đ';
  };

  const getQrCodeUrl = (code) => {
    return `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(code)}`;
  };

  const translateStatus = (bStatus) => {
    switch (bStatus) {
      case 'CONFIRMED': return 'Đã thanh toán';
      case 'PENDING_PAYMENT': return 'Chờ thanh toán';
      case 'CANCELLED': return 'Đã hủy';
      case 'EXPIRED': return 'Hết hạn';
      default: return bStatus;
    }
  };

  const getStatusColor = (bStatus) => {
    switch (bStatus) {
      case 'CONFIRMED': return 'text-emerald-400 border-emerald-400 bg-emerald-500/10';
      case 'PENDING_PAYMENT': return 'text-amber-400 border-amber-400 bg-amber-500/10';
      case 'CANCELLED': return 'text-red-400 border-red-400 bg-red-500/10';
      case 'EXPIRED': return 'text-zinc-500 border-zinc-700 bg-zinc-800/40';
      default: return 'text-zinc-400 border-zinc-800 bg-zinc-900';
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#050506] text-white">
        <div className="flex flex-col items-center gap-4 animate-pulse">
          <div className="w-12 h-12 border-4 border-brand-orange border-t-transparent rounded-full animate-spin"></div>
          <p className="text-sm font-semibold tracking-wider text-zinc-400">Đang tải chi tiết đặt vé...</p>
        </div>
      </div>
    );
  }

  if (error || !booking) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-zinc-950 px-4 text-center">
        <div className="w-16 h-16 rounded-full bg-red-500/10 flex items-center justify-center text-red-500 mb-6">
          <AlertTriangle className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-bold text-zinc-100 mb-2">Không tìm thấy vé xem phim</h2>
        <p className="text-sm text-zinc-400 max-w-md mb-8">{error || "Hóa đơn này không tồn tại hoặc đã bị xóa."}</p>
        <button
          onClick={() => navigate('/bookings')}
          className="bg-brand-orange hover:bg-opacity-90 text-white font-bold px-6 py-3 rounded-full transition-all text-xs uppercase tracking-wider"
        >
          Quay lại lịch sử đặt vé
        </button>
      </div>
    );
  }

  const { snapshot, tickets = [], foodOrder } = booking;
  const showtimeDate = snapshot?.showtimeStart ? new Date(snapshot.showtimeStart) : null;
  const currentStatus = booking.bookingStatus || booking.status;
  const isPending = currentStatus === 'PENDING_PAYMENT';
  const showTimer = isPending && timeLeft !== null && timeLeft > 0;

  return (
    <div className="bg-zinc-950 text-zinc-100 min-h-screen pt-32 pb-16 px-4 md:px-12 selection:bg-brand-orange selection:text-zinc-950 font-sans font-medium print:bg-white print:text-black print:pt-4">

      <div className="max-w-5xl mx-auto w-full space-y-8">

        {/* Back Link */}
        <div className="flex justify-between items-center print:hidden">
          <Link
            to="/bookings"
            className="flex items-center gap-2 text-zinc-400 hover:text-brand-orange transition-colors text-xs font-bold"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Quay lại danh sách đặt vé</span>
          </Link>

          <button
            onClick={() => window.print()}
            className="bg-zinc-900 hover:bg-zinc-850 text-zinc-300 font-bold px-4 py-2 border border-zinc-800 rounded-xl text-xs flex items-center gap-1.5 cursor-pointer"
          >
            <Printer className="w-4 h-4" />
            <span>In vé xem phim</span>
          </button>
        </div>

        {/* Top Banner: Booking Code & Status Summary */}
        <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 md:p-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-6 shadow-2xl print:border-black print:bg-white print:text-black">
          <div className="space-y-2">
            <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Mã đặt vé (Booking Code)</span>
            <div className="flex items-center gap-3">
              <h1 className="text-xl md:text-2xl font-black tracking-widest text-white uppercase print:text-black">
                {booking.bookingCode}
              </h1>
              <span className={`text-[10px] border px-2.5 py-0.5 rounded-full font-black uppercase tracking-wider ${getStatusColor(currentStatus)}`}>
                {translateStatus(currentStatus)}
              </span>
            </div>
            <p className="text-[10px] text-zinc-500 font-semibold">
              Khởi tạo lúc: {new Date(booking.createdAt).toLocaleString('vi-VN')}
            </p>
          </div>

          {/* Actions / Hold Countdown */}
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 w-full md:w-auto print:hidden">
            {showTimer && (
              <div className="flex items-center justify-between gap-4 py-3 px-4 bg-zinc-950/60 rounded-2xl border border-zinc-850 shadow-inner shrink-0">
                <div className="flex items-center gap-2">
                  <Clock className="w-4 h-4 text-amber-500 animate-spin" />
                  <span className="text-[9px] text-zinc-500 font-black uppercase tracking-wider">Hết hạn sau</span>
                </div>
                <span className="text-sm font-black text-amber-500 tracking-widest">
                  {formattedTimeLeft}
                </span>
              </div>
            )}

            {isPending && timeLeft !== 0 && (
              <button
                disabled={paymentLoading || cancelling}
                onClick={handlePayNow}
                className="bg-brand-orange hover:bg-opacity-95 text-white font-black px-6 py-3.5 rounded-2xl text-xs uppercase tracking-wider transition-all shadow-lg shadow-brand-orange/20"
              >
                Thanh toán ngay
              </button>
            )}

            {isPending && (
              <button
                disabled={cancelling || paymentLoading}
                onClick={handleCancel}
                className="bg-transparent hover:bg-red-500/10 border border-red-500/20 hover:border-red-500/40 text-red-400 font-bold px-6 py-3.5 rounded-2xl text-xs uppercase tracking-wider flex items-center justify-center gap-2 transition-all"
              >
                <Trash2 className="w-4 h-4" />
                <span>{cancelling ? 'Đang hủy...' : 'Hủy đặt vé'}</span>
              </button>
            )}
          </div>
        </div>

        {/* Timeline Status Trace */}
        <div className="bg-zinc-900/40 border border-zinc-800/80 rounded-3xl p-6 md:p-8 space-y-6 print:hidden">
          <h2 className="text-xs font-black uppercase tracking-widest text-zinc-400">TIẾN TRÌNH TRẠNG THÁI (TIMELINE)</h2>

          <div className="flex flex-col md:flex-row md:items-center gap-4 md:gap-0">
            {/* Stage 1: PENDING_PAYMENT */}
            <div className="flex-1 flex items-center gap-3 relative">
              <div className="w-8 h-8 rounded-full bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400 shrink-0 z-10">
                <Clock className="w-4 h-4" />
              </div>
              <div className="space-y-0.5">
                <span className="text-xs font-bold text-white block">Đặt vé thành công</span>
                <span className="text-[10px] text-zinc-500 block">Đang chờ thanh toán</span>
              </div>
              <div className="hidden md:block absolute left-8 right-0 top-1/2 -translate-y-1/2 h-0.5 bg-zinc-800 pointer-events-none"></div>
            </div>

            {/* Stage 2: CONFIRMED */}
            <div className="flex-1 flex items-center gap-3 relative mt-4 md:mt-0">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 z-10 ${
                currentStatus === 'CONFIRMED'
                  ? 'bg-emerald-500/10 border border-emerald-500/30 text-emerald-400'
                  : 'bg-zinc-900 border border-zinc-800 text-zinc-650'
              }`}>
                <CheckCircle2 className="w-4 h-4" />
              </div>
              <div className="space-y-0.5">
                <span className={`text-xs font-bold block ${currentStatus === 'CONFIRMED' ? 'text-white' : 'text-zinc-500'}`}>
                  Hoàn tất thanh toán
                </span>
                <span className="text-[10px] text-zinc-500 block">Nhận vé xem phim</span>
              </div>
              {currentStatus === 'CONFIRMED' && (
                <div className="hidden md:block absolute left-8 right-0 top-1/2 -translate-y-1/2 h-0.5 bg-brand-orange pointer-events-none"></div>
              )}
            </div>

            {/* Stage 3: CANCELLED / EXPIRED */}
            {(currentStatus === 'CANCELLED' || currentStatus === 'EXPIRED') && (
              <div className="flex-1 flex items-center gap-3 mt-4 md:mt-0">
                <div className="w-8 h-8 rounded-full bg-red-500/10 border border-red-500/30 flex items-center justify-center text-red-400 shrink-0">
                  <AlertTriangle className="w-4 h-4" />
                </div>
                <div className="space-y-0.5">
                  <span className="text-xs font-bold text-white block">
                     {currentStatus === 'CANCELLED' ? 'Đã hủy đơn' : 'Giao dịch hết hạn'}
                   </span>
                   <span className="text-[10px] text-zinc-500 block">Seat hold released</span>
                 </div>
                 </div>
             )}
          </div>
        </div>

        {/* Tickets and QR Section */}
        {tickets.length > 0 && (
          <div className="space-y-6">
            <h2 className="text-xs font-black uppercase tracking-widest text-zinc-400 print:hidden">THÔNG TIN VÉ XEM PHIM (TICKETS)</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {tickets.map(ticket => (
                <div
                  key={ticket.id}
                  className="bg-zinc-900 border border-zinc-800 rounded-3xl overflow-hidden flex flex-col sm:flex-row shadow-2xl relative print:bg-white print:text-black print:border-black print:shadow-none"
                >
                  <div className="hidden sm:block absolute -left-3 top-1/2 -translate-y-1/2 w-6 h-6 bg-zinc-950 rounded-full border-r border-zinc-800 print:hidden"></div>
                  <div className="hidden sm:block absolute -right-3 top-1/2 -translate-y-1/2 w-6 h-6 bg-zinc-950 rounded-full border-l border-zinc-800 print:hidden"></div>

                  <div className="flex-grow p-6 space-y-4 sm:border-r sm:border-dashed sm:border-zinc-800 print:border-black">
                    <div className="space-y-1">
                      <span className="text-[9px] bg-brand-orange/15 text-brand-orange border border-brand-orange/20 px-2 py-0.5 rounded font-black uppercase tracking-wider print:border-black">
                        {ticket.movieFormat || '2D Digital'}
                      </span>
                      <h3 className="text-sm font-black text-white leading-snug pt-1 print:text-black">{ticket.movieTitle}</h3>
                    </div>

                    <div className="grid grid-cols-2 gap-y-3 gap-x-2 text-[10px] text-zinc-400 print:text-black">
                      <div>
                        <span className="text-zinc-500 font-bold block">RẠP</span>
                        <span className="text-white font-extrabold print:text-black">{ticket.cinemaName}</span>
                      </div>
                      <div>
                        <span className="text-zinc-500 font-bold block">PHÒNG</span>
                        <span className="text-white font-extrabold print:text-black">{ticket.auditoriumName}</span>
                      </div>
                      <div>
                        <span className="text-zinc-500 font-bold block">SUẤT CHIẾU</span>
                        <span className="text-brand-orange font-black print:text-black">
                          {showtimeDate ? showtimeDate.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : ''}
                        </span>
                      </div>
                      <div>
                        <span className="text-zinc-500 font-bold block">NGÀY CHIẾU</span>
                        <span className="text-white font-extrabold print:text-black">
                          {showtimeDate ? showtimeDate.toLocaleDateString('vi-VN') : ''}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Tickets and QR Section */}
        {tickets.length > 0 && (
          <div className="space-y-6">
            <h2 className="text-xs font-black uppercase tracking-widest text-zinc-400 print:hidden">THÔNG TIN VÉ XEM PHIM (TICKETS)</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {tickets.map(ticket => (
                <div
                  key={ticket.id}
                  className="bg-zinc-900 border border-zinc-800 rounded-3xl overflow-hidden flex flex-col sm:flex-row shadow-2xl relative print:bg-white print:text-black print:border-black print:shadow-none"
                >
                  <div className="hidden sm:block absolute -left-3 top-1/2 -translate-y-1/2 w-6 h-6 bg-zinc-950 rounded-full border-r border-zinc-800 print:hidden"></div>
                  <div className="hidden sm:block absolute -right-3 top-1/2 -translate-y-1/2 w-6 h-6 bg-zinc-950 rounded-full border-l border-zinc-800 print:hidden"></div>

                  <div className="flex-grow p-6 space-y-4 sm:border-r sm:border-dashed sm:border-zinc-800 print:border-black">
                    <div className="space-y-1">
                      <span className="text-[9px] bg-brand-orange/15 text-brand-orange border border-brand-orange/20 px-2 py-0.5 rounded font-black uppercase tracking-wider print:border-black">
                        {ticket.movieFormat || '2D Digital'}
                      </span>
                      <h3 className="text-sm font-black text-white leading-snug pt-1 print:text-black">{ticket.movieTitle}</h3>
                    </div>

                    <div className="grid grid-cols-2 gap-y-3 gap-x-2 text-[10px] text-zinc-400 print:text-black">
                      <div>
                        <span className="text-zinc-500 font-bold block">RẠP</span>
                        <span className="text-white font-extrabold print:text-black">{ticket.cinemaName}</span>
                      </div>
                      <div>
                        <span className="text-zinc-500 font-bold block">PHÒNG</span>
                        <span className="text-white font-extrabold print:text-black">{ticket.auditoriumName}</span>
                      </div>
                      <div>
                        <span className="text-zinc-500 font-bold block">SUẤT CHIẾU</span>
                        <span className="text-brand-orange font-black print:text-black">
                          {showtimeDate ? showtimeDate.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : ''}
                        </span>
                      </div>
                      <div>
                        <span className="text-zinc-500 font-bold block">NGÀY CHIẾU</span>
                        <span className="text-white font-extrabold print:text-black">
                          {showtimeDate ? showtimeDate.toLocaleDateString('vi-VN') : ''}
                        </span>
                      </div>
                      <div>
                        <span className="text-zinc-500 font-bold block">GHẾ</span>
                        <span className="text-emerald-400 font-black text-xs print:text-black">{ticket.seatLabel} ({ticket.seatType})</span>
                      </div>
                      <div>
                        <span className="text-zinc-500 font-bold block">MÃ VÉ</span>
                        <span className="text-white font-bold tracking-wider print:text-black">{ticket.ticketCode}</span>
                      </div>
                    </div>
                  </div>

                  <div className="w-full sm:w-40 shrink-0 bg-zinc-950/60 p-6 flex flex-col items-center justify-center text-center gap-3 print:bg-white print:border-t print:border-black">
                    <div className="w-24 aspect-square bg-white rounded-xl p-2 shadow-inner shrink-0 flex items-center justify-center">
                      <img
                        src={getQrCodeUrl(ticket.ticketCode)}
                        alt={ticket.ticketCode}
                        className="max-w-full max-h-full"
                      />
                    </div>
                    <span className="text-[8px] text-zinc-500 font-black uppercase tracking-wider">QUÉT LÚC SOÁT VÉ</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Separate Food Order Section */}
        {foodOrder && foodOrder.items && foodOrder.items.length > 0 && (
          <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 md:p-8 space-y-6 print:border-black print:bg-white print:text-black">
            <h2 className="text-xs font-black uppercase tracking-widest text-zinc-400 print:text-black">BẮP NƯỚC ĐI KÈM (F&B)</h2>
            <div className="space-y-4 border-t border-zinc-800 pt-4 print:border-black">
              {foodOrder.items.map(item => (
                <div key={item.id} className="flex justify-between items-center bg-zinc-950/40 p-4 rounded-2xl border border-zinc-850 print:bg-white print:border-black">
                  <div className="space-y-1">
                    <span className="font-bold text-white text-sm print:text-black">{item.productName}</span>
                    <p className="text-[10px] text-zinc-500">Số lượng: x{item.quantity} | Đơn giá: {formatCurrency(item.unitPrice)}</p>
                  </div>
                  <span className="text-white font-bold print:text-black">{formatCurrency(item.finalAmount)}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Price Breakdown / Invoice Summary */}
        <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 md:p-8 space-y-6 print:border-black print:bg-white print:text-black">
          <div className="border-b border-zinc-800 pb-4 flex justify-between items-center print:border-black">
            <h2 className="text-xs font-black uppercase tracking-widest text-zinc-400 print:text-black">CHI TIẾT THANH TOÁN (PRICE BREAKDOWN)</h2>
          </div>

          <div className="space-y-4">
            <div className="flex justify-between items-start text-xs border-b border-zinc-800 pb-4 border-dashed print:border-black">
              <span className="font-bold text-zinc-300 print:text-black">Tiền vé xem phim</span>
              <span className="text-white font-bold print:text-black">{formatCurrency(booking.ticketAmount)}</span>
            </div>

            {foodOrder && foodOrder.items && foodOrder.items.length > 0 && (
              <div className="flex justify-between items-start text-xs border-b border-zinc-800 pb-4 border-dashed print:border-black">
                <span className="font-bold text-zinc-300 print:text-black">Tiền bắp nước</span>
                <span className="text-white font-bold print:text-black">{formatCurrency(foodOrder.finalAmount)}</span>
              </div>
            )}

            {/* Promo discount */}
            {booking.promotionDiscount > 0 && (
              <div className="flex justify-between items-start text-xs border-b border-zinc-800 pb-4 border-dashed print:border-black">
                <span className="font-bold text-emerald-500">Khuyến mãi & Giảm giá</span>
                <span className="text-emerald-500 font-bold">-{formatCurrency(booking.promotionDiscount)}</span>
              </div>
            )}

            {/* Transaction metadata */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs text-zinc-400 py-2 print:text-black">
              <div>
                <span className="text-zinc-500 font-bold block text-[9px] uppercase">Cổng giao dịch</span>
                <span className="text-zinc-200 font-bold print:text-black">{booking.paymentMethodSnapshot || 'Chưa ghi nhận'}</span>
              </div>
              <div>
                <span className="text-zinc-500 font-bold block text-[9px] uppercase">Mã tham chiếu ngân hàng</span>
                <span className="text-zinc-200 font-bold print:text-black">{booking.paymentReference || 'Chưa ghi nhận'}</span>
              </div>
            </div>

            {/* Total Cost */}
            <div className="flex justify-between items-center py-5 px-6 bg-zinc-950/80 rounded-2xl border border-brand-orange/30 shadow-[0_0_15px_rgba(255,122,0,0.1)] print:bg-white print:border-black print:shadow-none">
              <div>
                <span className="text-[10px] text-zinc-400 font-black uppercase tracking-wider block mb-0.5 print:text-zinc-600">Tổng số tiền thanh toán</span>
                <span className="text-[9px] text-brand-orange/80 font-bold uppercase print:text-black">Đã bao gồm VAT</span>
              </div>
              <span className="text-2xl md:text-3xl font-black text-brand-orange print:text-black">
                {formatCurrency(booking.finalAmount)}
              </span>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}
