import { useState, useEffect, useMemo, useCallback } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { CheckCircle, Info, Printer, ArrowRight, ShoppingBag } from 'lucide-react';
import { getBookingDetails } from '../services/bookingService';

export default function BookingSuccessPage() {
  const location = useLocation();
  const navigate = useNavigate();

  // Extract bookingId from query params
  const bookingId = useMemo(() => {
    const params = new URLSearchParams(location.search);
    return params.get('bookingId');
  }, [location.search]);

  // States
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchBooking = useCallback(async () => {
    if (!bookingId) {
      setError("Mã đơn hàng không hợp lệ.");
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const data = await getBookingDetails(bookingId);
      setBooking(data);
    } catch (err) {
      setError(err.message || err.detail || "Không thể tải thông tin đơn hàng.");
    } finally {
      setLoading(false);
    }
  }, [bookingId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchBooking();
  }, [fetchBooking]);

  const formatCurrency = (val) => {
    return (val || 0).toLocaleString('vi-VN') + 'đ';
  };

  // Generate Google Charts API QR Code URL dynamically
  const getQrCodeUrl = (code) => {
    return `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(code)}`;
  };

  const handlePrint = () => {
    window.print();
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#050506] text-white">
        <div className="flex flex-col items-center gap-4 animate-pulse">
          <div className="w-12 h-12 border-4 border-brand-orange border-t-transparent rounded-full animate-spin"></div>
          <p className="text-sm font-semibold tracking-wider text-zinc-400">Đang chuẩn bị thông tin vé xem phim...</p>
        </div>
      </div>
    );
  }

  if (error || !booking) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-zinc-950 px-4 text-center">
        <div className="w-16 h-16 rounded-full bg-red-500/10 flex items-center justify-center text-red-500 mb-6">
          <Info className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-bold text-zinc-100 mb-2">Không tìm thấy thông tin đơn hàng</h2>
        <p className="text-sm text-zinc-400 max-w-md mb-8">{error || "Vui lòng kiểm tra lại đường dẫn đặt vé."}</p>
        <button
          onClick={() => navigate('/movies')}
          className="bg-brand-orange hover:bg-opacity-90 text-white font-bold px-6 py-3 rounded-full transition-all text-xs uppercase tracking-wider"
        >
          Quay lại Trang Chủ
        </button>
      </div>
    );
  }

  const { snapshot, tickets = [], foodOrder } = booking;
  const showtimeDate = snapshot?.showtimeStart ? new Date(snapshot.showtimeStart) : null;

  return (
    <div className="bg-zinc-950 text-zinc-100 min-h-screen pt-32 pb-16 px-4 md:px-12 selection:bg-brand-orange selection:text-zinc-950 font-sans font-medium print:bg-white print:text-black print:pt-4">

      <div className="max-w-4xl mx-auto w-full space-y-8">

        {/* Success Announcement Header */}
        <div className="text-center space-y-3 print:hidden">
          <div className="w-16 h-16 rounded-full bg-emerald-500/15 flex items-center justify-center text-emerald-400 mx-auto mb-4 border border-emerald-500/30">
            <CheckCircle className="w-8 h-8" />
          </div>
          <h1 className="text-2xl md:text-3xl font-black uppercase tracking-wider text-white">THANH TOÁN THÀNH CÔNG</h1>
          <p className="text-xs text-zinc-400 font-semibold max-w-md mx-auto leading-relaxed">
            Cảm ơn bạn đã mua vé xem phim tại LoraFilm. Mã đặt vé của bạn là <span className="text-brand-orange font-black text-sm tracking-wider">{booking.bookingCode}</span>.
          </p>
        </div>

        {/* Dynamic Tickets list with QR Code */}
        <div className="space-y-6">
          <h2 className="text-xs font-black uppercase tracking-widest text-zinc-400 print:hidden">VÉ XEM PHIM CỦA BẠN (TICKET)</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {tickets.map(ticket => (
              <div
                key={ticket.id}
                className="bg-zinc-900 border border-zinc-800 rounded-3xl overflow-hidden flex flex-col md:flex-row shadow-2xl relative print:bg-white print:text-black print:border-black print:shadow-none"
              >
                {/* Decorative half-circle cutouts (ticket style) */}
                <div className="hidden md:block absolute -left-3 top-1/2 -translate-y-1/2 w-6 h-6 bg-zinc-950 rounded-full border-r border-zinc-800 print:hidden"></div>
                <div className="hidden md:block absolute -right-3 top-1/2 -translate-y-1/2 w-6 h-6 bg-zinc-950 rounded-full border-l border-zinc-800 print:hidden"></div>

                {/* Left Section: Ticket detail */}
                <div className="flex-grow p-6 space-y-4 md:border-r md:border-dashed md:border-zinc-800 print:border-black">
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
                        {showtimeDate ? showtimeDate.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false }) : ''}
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

                {/* Right Section: Scanning QR Code */}
                <div className="w-full md:w-44 shrink-0 bg-zinc-950/60 p-6 flex flex-col items-center justify-center text-center gap-3 print:bg-white print:border-t print:border-black">
                  <div className="w-28 aspect-square bg-white rounded-xl p-2 shadow-inner shrink-0 flex items-center justify-center">
                    <img
                      src={getQrCodeUrl(ticket.ticketCode)}
                      alt={ticket.ticketCode}
                      className="max-w-full max-h-full"
                    />
                  </div>
                  <span className="text-[9px] text-zinc-500 font-black uppercase tracking-wider">MÃ QR ĐỂ VÀO PHÒNG CHIẾU</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Detailed Receipt section */}
        <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 md:p-8 space-y-6 print:border-black print:bg-white print:text-black">
          <div className="border-b border-zinc-800 pb-4 flex justify-between items-center print:border-black">
            <h2 className="text-xs font-black uppercase tracking-widest text-zinc-400">CHI TIẾT HÓA ĐƠN GIAO DỊCH</h2>
            <span className="text-[10px] text-zinc-500 font-semibold print:hidden">
              Ngày lập: {new Date(booking.createdAt).toLocaleString('vi-VN')}
            </span>
          </div>

          <div className="space-y-4">
            <div className="flex justify-between items-start text-xs border-b border-zinc-800 pb-4 border-dashed print:border-black">
              <div className="space-y-1">
                <span className="font-bold text-white print:text-black">Mục đặt vé (Tickets)</span>
                <p className="text-[10px] text-zinc-500">Đặt giữ {tickets.length} ghế xem phim</p>
              </div>
              <span className="text-white font-bold print:text-black">{formatCurrency(booking.ticketAmount)}</span>
            </div>

            {/* Food cart order list receipt */}
            {foodOrder && foodOrder.items && foodOrder.items.length > 0 && (
              <div className="flex justify-between items-start text-xs border-b border-zinc-800 pb-4 border-dashed print:border-black">
                <div className="space-y-1.5 flex-grow pr-6">
                  <span className="font-bold text-white print:text-black">Dịch vụ đi kèm (F&B)</span>
                  <div className="space-y-1 text-[10px] text-zinc-500">
                    {foodOrder.items.map(item => (
                      <div key={item.id} className="flex justify-between">
                        <span>{item.productName} (x{item.quantity})</span>
                        <span>{formatCurrency(item.finalAmount)}</span>
                      </div>
                    ))}
                  </div>
                </div>
                <span className="text-white font-bold print:text-black shrink-0">{formatCurrency(foodOrder.finalAmount)}</span>
              </div>
            )}

            {/* Discount breakdown */}
            {booking.promotionDiscount > 0 && (
              <div className="flex justify-between items-start text-xs border-b border-zinc-800 pb-4 border-dashed print:border-black">
                <span className="font-bold text-emerald-500">Khuyến mãi & Giảm giá</span>
                <span className="text-emerald-500 font-bold">-{formatCurrency(booking.promotionDiscount)}</span>
              </div>
            )}

            {/* Billing details */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs text-zinc-400 py-2 print:text-black">
              <div>
                <span className="text-zinc-500 font-bold block text-[9px] uppercase">Cổng thanh toán</span>
                <span className="text-zinc-200 font-bold print:text-black">
                  {booking.paymentMethodSnapshot || 'MOCK_PAY'}
                </span>
              </div>
              <div>
                <span className="text-zinc-500 font-bold block text-[9px] uppercase">Mã tham chiếu thanh toán</span>
                <span className="text-zinc-200 font-bold print:text-black">
                  {booking.paymentReference || 'MOCK-TXN-REFERENCE'}
                </span>
              </div>
            </div>

            {/* Cost Summary */}
            <div className="flex justify-between items-center py-4 px-6 bg-zinc-950/60 rounded-2xl border border-zinc-850 shadow-inner print:bg-white print:border-black">
              <div>
                <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Tổng số tiền thanh toán</span>
                <span className="text-[8px] text-zinc-650 block print:hidden">(Đã bao gồm 10% VAT)</span>
              </div>
              <span className="text-xl md:text-2xl font-black text-brand-orange">
                {formatCurrency(booking.finalAmount)}
              </span>
            </div>
          </div>
        </div>

        {/* Buttons & Actions */}
        <div className="flex flex-col sm:flex-row gap-4 justify-between items-center print:hidden">
          <button
            onClick={handlePrint}
            className="w-full sm:w-auto bg-zinc-900 hover:bg-zinc-850 text-zinc-300 font-bold px-6 py-3.5 rounded-2xl border border-zinc-800 flex items-center justify-center gap-2 cursor-pointer transition-colors"
          >
            <Printer className="w-4 h-4" />
            <span>In / Tải PDF Hóa Đơn</span>
          </button>

          <div className="flex flex-col sm:flex-row gap-3 w-full sm:w-auto">
            <Link
              to="/bookings"
              className="bg-zinc-900 hover:bg-zinc-850 text-zinc-300 font-bold px-6 py-3.5 rounded-2xl border border-zinc-800 text-xs uppercase tracking-widest text-center flex items-center justify-center gap-1.5 transition-colors"
            >
              <ShoppingBag className="w-4 h-4" />
              <span>Lịch sử đặt vé</span>
            </Link>

            <Link
              to="/"
              className="bg-brand-orange hover:bg-opacity-95 text-white font-black px-6 py-3.5 rounded-2xl text-xs uppercase tracking-widest text-center flex items-center justify-center gap-1.5 shadow-lg shadow-brand-orange/20"
            >
              <span>Quay về trang chủ</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
