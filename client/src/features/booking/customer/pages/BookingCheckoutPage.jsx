import { useState, useEffect, useMemo, useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Clock, AlertTriangle, Search, ShieldCheck, CreditCard } from 'lucide-react';
import { getBookingDetails, cancelBooking, finalizeCheckout } from '../services/bookingService';
import { getConcessions, getBookingFoodOrder, addFoodItem, updateFoodQuantity, removeFoodItem } from '../services/foodService';
import BookingStepper from '../components/BookingStepper';
import BookingCancellationModal from '../components/BookingCancellationModal';
import BookingNoticeModal from '../components/BookingNoticeModal';
import { getBookingErrorMessage } from '../utils/bookingErrorMessages';
import scoreCustomerService from '@/features/score/customer/services/scoreCustomerService';
import {
  createPaymentHandoff,
  getOrCreatePaymentAttemptKey
} from '../services/paymentHandoffService';

const FALLBACK_POSTER = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='500' height='750' viewBox='0 0 500 750'><rect width='500' height='750' fill='%2309090b'/><text x='50%25' y='48%25' dominant-baseline='middle' text-anchor='middle' font-family='sans-serif' font-weight='bold' font-size='32' fill='%2352525b'>LORA FILM</text><text x='50%25' y='54%25' dominant-baseline='middle' text-anchor='middle' font-family='sans-serif' font-size='17' fill='%233f3f46'>Chưa có áp phích</text></svg>";

export default function BookingCheckoutPage() {
  const location = useLocation();
  const navigate = useNavigate();

  // Extract bookingId from query params
  const bookingId = useMemo(() => {
    const params = new URLSearchParams(location.search);
    return params.get('bookingId');
  }, [location.search]);
  const bookingDraft = location.state || {};

  // Step state within checkout: 3 (Food Selection) or 4 (Payment/Summary)
  const [step, setStep] = useState(3);

  // States
  const [booking, setBooking] = useState(null);
  const [concessions, setConcessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Cart operations loading states
  const [cartUpdatingId, setCartUpdatingId] = useState(null);
  const [paymentLoading, setPaymentLoading] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [cancelError, setCancelError] = useState('');
  const [notice, setNotice] = useState(null);

  // Filter & Search states
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  
  // Terms agreement state for payment step
  const [termsAgreed, setTermsAgreed] = useState(false);
  const [selectedPaymentMethod, setSelectedPaymentMethod] = useState('VNPAY');
  const [userScore, setUserScore] = useState(null);

  // Countdown timer state
  const [timeLeft, setTimeLeft] = useState(null);

  // Load initial data
  const fetchData = useCallback(async () => {
    if (!bookingId) {
      setError("Mã đơn hàng không hợp lệ.");
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const bookingData = await getBookingDetails(bookingId);
      let foodOrder = null;
      try {
        foodOrder = await getBookingFoodOrder(bookingId);
      } catch {
        foodOrder = null;
      }
      setBooking({
        ...bookingData,
        foodOrder: foodOrder ?? bookingData.foodOrder ?? bookingData.food ?? null,
        finalAmount: bookingData.totalAmount ?? bookingData.finalAmount ?? 0,
        ticketAmount: bookingData.ticketAmount ?? 0,
        expiresAt: bookingData.expiresAt ?? bookingData.expiredAt ?? bookingData.paymentDeadline,
        snapshot: bookingData.snapshot ?? bookingData.presentation ?? bookingDraft.showtime ?? null
      });

      const concessionsData = await getConcessions();
      setConcessions(concessionsData || []);

      try {
        const scoreResponse = await scoreCustomerService.getScoreBalance();
        setUserScore(scoreResponse?.data ?? scoreResponse ?? null);
      } catch {
        // Score Service is optional for checkout. Its outage must not prevent
        // the customer from completing the current Booking.
        setUserScore(null);
      }
    } catch (err) {
      setError(getBookingErrorMessage(
        err,
        'Không thể tải thông tin đặt vé. Vui lòng thử lại.'
      ));
    } finally {
      setLoading(false);
    }
  }, [bookingId, bookingDraft.showtime]);

  useEffect(() => {
    // Data loading is intentionally triggered when the public Booking ID changes.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchData();
  }, [fetchData]);

  // Expiration countdown logic
  useEffect(() => {
    if (!booking || !booking.expiresAt) return;

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

  // Handle countdown expiration
  useEffect(() => {
    if (timeLeft === 0) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setNotice({
        title: 'Thời gian giữ ghế đã kết thúc',
        message: 'Đơn không còn khả dụng để thanh toán và ghế sẽ được trả lại cho khách hàng khác.',
        variant: 'warning',
        redirectTo: '/movies?error=expired'
      });
    }
  }, [timeLeft]);

  // Concession categories
  const categories = useMemo(() => {
    const cats = new Set();
    concessions.forEach(c => {
      if (c.type) cats.add(c.type.toUpperCase());
    });
    return ['ALL', ...Array.from(cats)];
  }, [concessions]);

  // Filtered concessions
  const filteredConcessions = useMemo(() => {
    return concessions.filter(c => {
      const matchSearch = (c.name || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
                          (c.description || '').toLowerCase().includes(searchQuery.toLowerCase());
      const matchCat = selectedCategory === 'ALL' || (c.type && c.type.toUpperCase() === selectedCategory);
      return matchSearch && matchCat;
    });
  }, [concessions, searchQuery, selectedCategory]);

  // Find quantity and item ID in current booking cart
  const getItemCartInfo = useCallback((concessionId) => {
    if (!booking || !booking.foodOrder || !booking.foodOrder.items) {
      return { quantity: 0, itemId: null };
    }
    const item = booking.foodOrder.items.find(i => i.productId === concessionId);
    return item ? { quantity: item.quantity, itemId: item.id } : { quantity: 0, itemId: null };
  }, [booking]);

  // Handle add/modify food in cart
  const handleQuantityChange = async (concession, increment) => {
    if (cartUpdatingId) return; // Prevent concurrent requests
    setCartUpdatingId(concession.id);

    const { quantity, itemId } = getItemCartInfo(concession.id);
    const newQty = quantity + (increment ? 1 : -1);

    try {
      let updatedFoodOrder;
      if (quantity === 0 && increment) {
        // Add new
        updatedFoodOrder = await addFoodItem(bookingId, { productId: concession.id, quantity: 1 });
      } else if (newQty > 0) {
        // Update quantity
        updatedFoodOrder = await updateFoodQuantity(bookingId, itemId, newQty);
      } else {
        // Remove
        await removeFoodItem(bookingId, itemId);
        // Fetch fresh details since remove returns empty
        const freshBooking = await getBookingDetails(bookingId);
        setBooking(prev => ({
          ...prev,
          ...freshBooking,
          snapshot: freshBooking.snapshot || prev.snapshot,
          foodOrder: freshBooking.foodOrder,
          finalAmount: freshBooking.totalAmount ?? freshBooking.finalAmount ?? prev.finalAmount
        }));
        setCartUpdatingId(null);
        return;
      }

      const freshBooking = await getBookingDetails(bookingId);
      setBooking(prev => ({
        ...prev,
        ...freshBooking,
        snapshot: freshBooking.snapshot || prev.snapshot,
        foodOrder: updatedFoodOrder || freshBooking.foodOrder,
        finalAmount: freshBooking.totalAmount ?? freshBooking.finalAmount ?? prev.finalAmount
      }));
    } catch (err) {
      setNotice({
        title: 'Không thể cập nhật bắp nước',
        message: getBookingErrorMessage(
          err,
          'Kết nối không ổn định. Vui lòng thử lại.'
        ),
        variant: 'error'
      });
    } finally {
      setCartUpdatingId(null);
    }
  };

  // Lock Booking-owned amount, then let Payment Service create the attempt.
  const handleStartPayment = async () => {
    if (!termsAgreed) {
      setNotice({
        title: 'Chưa đồng ý điều khoản',
        message: 'Bạn cần đồng ý với Điều khoản và Quy định của LoraFilm trước khi tiếp tục thanh toán.',
        variant: 'warning'
      });
      return;
    }
    setPaymentLoading(true);
    try {
      const finalized = await finalizeCheckout(bookingId);
      setBooking(prev => ({ ...prev, ...finalized }));
      const idempotencyKey = getOrCreatePaymentAttemptKey(
        bookingId,
        selectedPaymentMethod
      );
      const payment = await createPaymentHandoff({
        bookingPublicId: bookingId,
        paymentMethod: selectedPaymentMethod,
        idempotencyKey
      });

      if (payment?.paymentUrl) {
        window.location.assign(payment.paymentUrl);
        return;
      }

      setNotice({
        title: 'Đã tạo yêu cầu thanh toán',
        message: 'Payment Service đã tiếp nhận yêu cầu. Bạn có thể tiếp tục theo dõi trạng thái của giao dịch.',
        variant: 'success',
        redirectTo: `/bookings/${bookingId}`
      });
    } catch (err) {
      setNotice({
        title: 'Không thể chuẩn bị thanh toán',
        message: getBookingErrorMessage(
          err,
          'Không thể chuẩn bị thanh toán. Vui lòng thử lại.'
        ),
        variant: 'error'
      });
    } finally {
      setPaymentLoading(false);
    }
  };

  const handleCancelBooking = async () => {
    setCancelling(true);
    setCancelError('');
    try {
      await cancelBooking(
        bookingId,
        'Khách hàng chủ động hủy đặt chỗ tại checkout'
      );
      setCancelModalOpen(false);
      navigate('/movies');
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

  // Format countdown string
  const formattedTimeLeft = useMemo(() => {
    if (timeLeft === null) return '00:00';
    const mins = String(Math.floor(timeLeft / 60)).padStart(2, '0');
    const secs = String(timeLeft % 60).padStart(2, '0');
    return `${mins}:${secs}`;
  }, [timeLeft]);

  const formatCurrency = (val) => {
    return (val || 0).toLocaleString('vi-VN') + 'đ';
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#050506] text-white">
        <div className="flex flex-col items-center gap-4 animate-pulse">
          <div className="w-12 h-12 border-4 border-brand-orange border-t-transparent rounded-full animate-spin"></div>
          <p className="text-sm font-semibold tracking-wider text-zinc-400">Đang chuẩn bị thông tin thanh toán...</p>
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
        <h2 className="text-xl font-bold text-zinc-100 mb-2">Đơn hàng không khả dụng</h2>
        <p className="text-sm text-zinc-400 max-w-md mb-8">{error || "Đơn hàng này không tồn tại hoặc đã hết hạn."}</p>
        <button
          onClick={() => navigate('/movies')}
          className="bg-brand-orange hover:bg-opacity-90 text-white font-bold px-6 py-3 rounded-full transition-all text-xs uppercase tracking-wider"
        >
          Quay lại chọn phim
        </button>
      </div>
    );
  }

  const { snapshot } = booking;
  const bookingStatus = booking.bookingStatus || booking.status;
  const isPending = bookingStatus === 'PENDING_PAYMENT';
  const isExpired = timeLeft === 0 || !isPending;
  const draftSeats = bookingDraft.selectedSeats || [];
  const snapshotSeats = Array.isArray(snapshot?.seats) ? snapshot.seats : [];
  const visibleSeats = snapshotSeats.length
    ? snapshotSeats
    : booking.tickets?.length
      ? booking.tickets
      : draftSeats;
  const selectedFoodItems = Array.isArray(booking.foodOrder?.items)
    ? booking.foodOrder.items
    : [];
  const showtimeStart = snapshot?.showtimeStart || snapshot?.startTime;
  const movie = snapshot?.movie || {};
  const cinema = snapshot?.cinema || {};
  const auditorium = snapshot?.auditorium || {};
  const movieTitle = snapshot?.movieTitle || movie?.title || booking.movieTitle || 'Thông tin phim đang cập nhật';
  const moviePosterUrl = snapshot?.moviePosterUrl
    || snapshot?.moviePoster
    || movie?.posterUrl
    || booking.posterUrl
    || null;
  const seatLabel = seat => seat.label || seat.seatLabel || seat.seatCode || 'Chưa rõ';
  const seatType = seat => seat.type || seat.seatType;
  const foodItemName = item => item.productName || item.name || 'Bắp nước';
  const foodItemAmount = item => item.finalAmount
    ?? item.totalAmount
    ?? ((item.unitPrice || 0) * (item.quantity || 0));
  const foodAmount = booking.foodOrder?.finalAmount
    ?? booking.foodOrder?.totalAmount
    ?? booking.foodAmount
    ?? 0;
  const availableScorePoints = Math.max(
    0,
    Number(userScore?.currentPoints || 0) - Number(userScore?.heldPoints || 0)
  );

  return (
    <div className="bg-zinc-950 text-zinc-100 min-h-screen pt-28 pb-16 px-4 md:px-12 selection:bg-brand-orange selection:text-zinc-950 font-sans font-medium">
      {notice && (
        <BookingNoticeModal
          title={notice.title}
          message={notice.message}
          variant={notice.variant}
          onClose={() => {
            const redirectTo = notice.redirectTo;
            setNotice(null);
            if (redirectTo) navigate(redirectTo);
          }}
        />
      )}

      {cancelModalOpen && (
        <BookingCancellationModal
          bookingCode={booking.bookingCode}
          error={cancelError}
          pending={cancelling}
          onClose={() => {
            setCancelError('');
            setCancelModalOpen(false);
          }}
          onConfirm={handleCancelBooking}
        />
      )}

      <div className="max-w-7xl mx-auto w-full">
        {/* Booking Stepper */}
        <BookingStepper currentStep={step} />

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
          {/* Left panel: F&B selection OR payment selection */}
          <div className="lg:col-span-2 space-y-8">
            {/* Countdown notice on mobile */}
            <div className="lg:hidden bg-zinc-900 border border-zinc-800 rounded-2xl p-4 flex items-center justify-between gap-4">
              <div className="flex items-center gap-2">
                <Clock className="w-5 h-5 text-amber-500 shrink-0" />
                <span className="text-xs text-zinc-400 font-bold">Thời gian giao dịch còn lại</span>
              </div>
              <span className={`text-sm font-black tracking-widest ${timeLeft < 60 ? 'text-red-500 animate-pulse' : 'text-amber-500'}`}>
                {formattedTimeLeft}
              </span>
            </div>

            {step === 3 ? (
              /* Step 3: Choose Food & Beverage */
              <div className="bg-zinc-900/40 border border-zinc-800/80 rounded-3xl p-6 md:p-8 space-y-6">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-zinc-800 pb-6">
                  <div>
                    <h2 className="text-lg font-black text-white uppercase tracking-wider">Chọn Bắp Nước đi kèm</h2>
                    <p className="text-[10px] text-zinc-500 font-bold uppercase mt-1">Nâng cấp trải nghiệm điện ảnh của bạn</p>
                  </div>

                  {/* Categories filtering tab bar */}
                  <div className="flex gap-2 overflow-x-auto py-1 scrollbar-none">
                    {categories.map(cat => (
                      <button
                        key={cat}
                        onClick={() => setSelectedCategory(cat)}
                        className={`px-4 py-2 rounded-xl text-xs font-bold transition-all shrink-0 cursor-pointer ${
                          selectedCategory === cat
                            ? 'bg-brand-orange text-white'
                            : 'bg-zinc-800 text-zinc-400 hover:text-zinc-200'
                        }`}
                      >
                        {cat === 'ALL' ? 'TẤT CẢ' : cat}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Search bar */}
                <div className="relative max-w-sm">
                  <Search className="w-4 h-4 text-zinc-500 absolute left-4 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    placeholder="Tìm bắp nước nhanh..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl pl-11 pr-4 py-2.5 text-xs text-zinc-200 focus:outline-none focus:border-brand-orange placeholder:text-zinc-600 transition-colors"
                  />
                </div>

                {/* Grid of concession items */}
                {filteredConcessions.length > 0 ? (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                    {filteredConcessions.map(item => {
                      const { quantity } = getItemCartInfo(item.id);
                      const isUpdating = cartUpdatingId === item.id;

                      return (
                        <div key={item.id} className="bg-zinc-900 border border-zinc-800/80 hover:border-zinc-700/80 rounded-2xl p-4 flex gap-4 transition-all relative overflow-hidden">
                          {/* Concession Image */}
                          <div className="relative w-20 aspect-square rounded-xl bg-zinc-950/60 overflow-hidden border border-zinc-800 shrink-0 flex items-center justify-center">
                            <span className="px-2 text-center text-[8px] font-bold text-zinc-600">Chưa có ảnh</span>
                            {item.imageUrl && (
                              <img
                                src={item.imageUrl}
                                alt={item.name}
                                className="absolute inset-0 w-full h-full object-cover"
                                onError={(event) => {
                                  event.currentTarget.style.display = 'none';
                                }}
                              />
                            )}
                          </div>
                          {/* Meta and selector */}
                          <div className="flex-grow flex flex-col justify-between py-0.5 space-y-3">
                            <div className="space-y-1">
                              <span className="text-[8px] bg-zinc-800 text-zinc-400 font-black px-1.5 py-0.5 rounded uppercase tracking-wider">
                                {item.type || 'Combo'}
                              </span>
                              <h4 className="text-xs font-black text-white leading-snug line-clamp-1">{item.name}</h4>
                              <p className="text-[9px] text-zinc-550 leading-normal line-clamp-2">{item.description}</p>
                            </div>

                            <div className="flex justify-between items-center">
                              <span className="text-xs font-black text-brand-orange">{formatCurrency(item.price)}</span>

                              {/* Quantity controller */}
                              <div className="flex items-center bg-zinc-950 border border-zinc-800 rounded-lg p-1 gap-3">
                                <button
                                  disabled={quantity === 0 || isUpdating || isExpired}
                                  onClick={() => handleQuantityChange(item, false)}
                                  className={`w-6 h-6 rounded flex items-center justify-center font-black text-xs transition-colors ${
                                    quantity > 0 && !isUpdating && !isExpired
                                      ? 'bg-zinc-800 hover:bg-zinc-700 text-zinc-300'
                                      : 'text-zinc-700 cursor-not-allowed'
                                  }`}
                                >
                                  -
                                </button>
                                <span className="w-5 text-center text-xs font-bold text-zinc-200">
                                  {isUpdating ? '...' : quantity}
                                </span>
                                <button
                                  disabled={isUpdating || isExpired}
                                  onClick={() => handleQuantityChange(item, true)}
                                  className={`w-6 h-6 rounded flex items-center justify-center font-black text-xs transition-colors ${
                                    !isUpdating && !isExpired
                                      ? 'bg-brand-orange hover:bg-opacity-90 text-white'
                                      : 'text-zinc-700 cursor-not-allowed'
                                  }`}
                                >
                                  +
                                </button>
                              </div>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div className="text-center py-10 bg-zinc-900/20 border border-zinc-800 rounded-2xl border-dashed">
                    <span className="text-xs text-zinc-500 italic block">Không tìm thấy bắp nước phù hợp...</span>
                  </div>
                )}
              </div>
            ) : (
              /* Step 4: Payment Service handoff */
              <div className="space-y-8">
                {availableScorePoints > 0 && (
                  <div className="rounded-3xl border border-amber-500/30 bg-gradient-to-r from-amber-950/30 to-zinc-900/60 p-6 md:p-8">
                    <div className="flex flex-wrap items-start justify-between gap-4">
                      <div>
                        <p className="text-[10px] font-black uppercase tracking-wider text-amber-400">
                          Điểm thành viên
                        </p>
                        <h2 className="mt-1 text-lg font-black text-white">
                          Bạn đang có {availableScorePoints.toLocaleString('vi-VN')} điểm khả dụng
                        </h2>
                      </div>
                      <span className="rounded-full border border-amber-500/30 bg-amber-500/10 px-3 py-1 text-[10px] font-black uppercase text-amber-300">
                        Đã đồng bộ Score Service
                      </span>
                    </div>
                    <p className="mt-4 text-xs font-medium leading-relaxed text-zinc-400">
                      Việc giữ và trừ điểm phải được Payment Service xác nhận cùng giao dịch.
                      Tính năng dùng điểm tại checkout sẽ được mở khi tích hợp thanh toán hoàn tất;
                      số tiền của đơn hiện tại vẫn do Booking Service quản lý.
                    </p>
                  </div>
                )}

                <div className="bg-zinc-900/40 border border-zinc-800/80 rounded-3xl p-6 md:p-8 space-y-6">
                  <div>
                    <h2 className="text-lg font-black text-white uppercase tracking-wider">Chọn Phương Thức Thanh Toán</h2>
                    <p className="text-[10px] text-zinc-500 font-bold uppercase mt-1">Payment Service sẽ xác thực lại số tiền và thời hạn của đơn</p>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <button
                      type="button"
                      onClick={() => setSelectedPaymentMethod('VNPAY')}
                      className={`border rounded-2xl p-5 flex items-center gap-4 text-left transition-colors ${
                        selectedPaymentMethod === 'VNPAY'
                          ? 'border-brand-orange bg-brand-orange/10'
                          : 'border-zinc-800 bg-zinc-900/30 hover:border-zinc-700'
                      }`}
                    >
                      <div className="w-12 aspect-[4/3] bg-white rounded-lg p-1.5 flex items-center justify-center">
                        <img src="https://sandbox.vnpayment.vn/paymentv2/Images/brands/logo.svg" alt="VNPay Logo" className="max-h-full max-w-full object-contain" />
                      </div>
                      <div>
                        <h4 className="text-xs font-black text-zinc-200">VNPay</h4>
                        <p className="text-[9px] text-zinc-650 mt-0.5">Hỗ trợ ngân hàng nội địa & quốc tế</p>
                      </div>
                    </button>

                    <button
                      type="button"
                      onClick={() => setSelectedPaymentMethod('MOMO')}
                      className={`border rounded-2xl p-5 flex items-center gap-4 text-left transition-colors ${
                        selectedPaymentMethod === 'MOMO'
                          ? 'border-brand-orange bg-brand-orange/10'
                          : 'border-zinc-800 bg-zinc-900/30 hover:border-zinc-700'
                      }`}
                    >
                      <div className="w-12 aspect-[4/3] bg-pink-100 rounded-lg p-2 flex items-center justify-center">
                        <img src="https://upload.wikimedia.org/wikipedia/vi/f/fe/MoMo_Logo.png" alt="Momo Logo" className="max-h-full max-w-full object-contain" />
                      </div>
                      <div>
                        <h4 className="text-xs font-black text-zinc-200">MoMo</h4>
                        <p className="text-[9px] text-zinc-650 mt-0.5">Thanh toán nhanh qua ví điện tử</p>
                      </div>
                    </button>
                  </div>
                </div>

                <div className="bg-zinc-900/40 border border-zinc-800/80 rounded-3xl p-6 md:p-8 space-y-6">
                  <div>
                    <h2 className="text-lg font-black text-white uppercase tracking-wider">Chuyển sang thanh toán</h2>
                    <p className="text-[10px] text-zinc-500 font-bold uppercase mt-1">
                      LoraFilm không gửi số tiền từ trình duyệt; Payment Service lấy số tiền đã khóa trực tiếp từ Booking Service
                    </p>
                  </div>

                  <button
                    onClick={handleStartPayment}
                    disabled={paymentLoading || isExpired}
                    className="w-full py-4 bg-brand-orange hover:bg-opacity-95 disabled:bg-zinc-800 disabled:text-zinc-600 text-white font-black uppercase text-xs tracking-wider rounded-2xl shadow-lg transition-all cursor-pointer flex items-center justify-center gap-2"
                  >
                    <CreditCard className="w-4 h-4" />
                    <span>{paymentLoading ? 'Đang tạo giao dịch...' : `Thanh toán qua ${selectedPaymentMethod}`}</span>
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Right panel: Sticky order info sidebar / Food summary */}
          <div className="lg:col-span-1 sticky top-24 bg-zinc-900 border border-zinc-800 rounded-3xl p-6 space-y-6 shadow-2xl">
            {/* Desktop Countdown Timer */}
            <div className="hidden lg:flex items-center justify-between gap-4 py-3 px-4 bg-zinc-950/60 rounded-2xl border border-zinc-850 shadow-inner">
              <div className="flex items-center gap-2">
                <Clock className="w-4 h-4 text-amber-500" />
                <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Thời gian còn lại</span>
              </div>
              <span className={`text-base font-black tracking-widest ${timeLeft < 60 ? 'text-red-500 animate-pulse' : 'text-amber-500'}`}>
                {formattedTimeLeft}
              </span>
            </div>

            {/* Movie Poster & Meta details */}
            <div className="flex gap-4 items-start pb-6 border-b border-zinc-800">
              <div className="w-20 aspect-[2/3] rounded-xl overflow-hidden bg-zinc-950 border border-zinc-800 shrink-0 flex items-center justify-center">
                <img
                  src={moviePosterUrl || FALLBACK_POSTER}
                  alt={`Áp phích phim ${movieTitle}`}
                  className="w-full h-full object-cover"
                  onError={(event) => {
                    event.currentTarget.onerror = null;
                    event.currentTarget.src = FALLBACK_POSTER;
                  }}
                />
              </div>
              <div className="space-y-2 flex-grow min-w-0">
                <span className="text-[9px] font-black uppercase tracking-widest text-brand-orange">Thông tin suất chiếu</span>
                <h3 className="text-base font-black text-white leading-snug">{movieTitle}</h3>
                {snapshot?.originalTitle && (
                  <p className="text-[10px] text-zinc-500 line-clamp-1">{snapshot.originalTitle}</p>
                )}
                <div className="flex items-center gap-1.5 text-[10px] text-zinc-500 font-semibold">
                  {(snapshot?.duration || movie?.durationMinutes) && (
                    <span>{snapshot?.duration || movie?.durationMinutes} phút</span>
                  )}
                  {(snapshot?.duration || movie?.durationMinutes) && (snapshot?.ageRating || movie?.ageRating) && <span>•</span>}
                  {(snapshot?.ageRating || movie?.ageRating) && (
                    <span className="text-brand-yellow font-black border border-brand-yellow/30 px-1.5 py-0.5 rounded text-[8px]">
                      {snapshot?.ageRating || movie?.ageRating}
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Booking Details */}
            <div className="space-y-3 py-2 text-xs border-b border-zinc-800">
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Cụm rạp</span>
                <span className="text-white font-bold text-right">{snapshot?.cinemaName || cinema?.name || 'Chưa có thông tin rạp'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Phòng chiếu</span>
                <span className="text-zinc-200 font-bold text-right">{snapshot?.auditoriumName || auditorium?.name || 'Chưa có thông tin phòng'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Suất chiếu</span>
                <span className="text-white font-bold text-right">
                  {showtimeStart
                    ? `${new Date(showtimeStart).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false })} · ${new Date(showtimeStart).toLocaleDateString('vi-VN')}`
                    : 'Chưa có thông tin'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Số lượng ghế</span>
                <span className="text-brand-orange font-black text-right">{visibleSeats.length} ghế</span>
              </div>
            </div>

            {/* Selected Seats */}
            <div className="py-2 border-b border-zinc-800 space-y-2">
              <span className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Các vị trí ghế</span>
              {visibleSeats.length > 0 ? (
                <div className="flex flex-wrap gap-2">
                  {visibleSeats.map((seat, index) => (
                    <span
                      key={seat.seatPublicId || seat.id || seat.publicId || index}
                      className="rounded-lg border border-brand-orange/20 bg-brand-orange/10 px-2.5 py-1 text-[10px] font-black text-brand-orange"
                    >
                      {seatLabel(seat)}
                      {seatType(seat) ? ` · ${seatType(seat)}` : ''}
                    </span>
                  ))}
                </div>
              ) : (
                <p className="text-[11px] text-red-400">Không tìm thấy dữ liệu ghế của đơn. Vui lòng tải lại trang.</p>
              )}
            </div>

            {/* Food items breakdown */}
            <div className="py-2 border-b border-zinc-800 space-y-3">
              <span className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Bắp nước đã chọn</span>
              {selectedFoodItems.length > 0 ? (
                <div className="space-y-2 max-h-36 overflow-y-auto pr-1">
                  {selectedFoodItems.map((item, index) => (
                    <div
                      key={item.id || item.productId || `${foodItemName(item)}-${index}`}
                      className="grid grid-cols-[minmax(0,1fr)_36px_78px] items-center gap-2 text-[11px]"
                    >
                      <span className="truncate text-zinc-300" title={foodItemName(item)}>{foodItemName(item)}</span>
                      <span className="text-center font-bold text-zinc-500">x{item.quantity}</span>
                      <span className="text-right font-bold text-zinc-100">{formatCurrency(foodItemAmount(item))}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-[11px] text-zinc-650 italic">Chưa chọn bắp nước đi kèm</div>
              )}
            </div>

            {/* Pricing breakdown */}
            <div className="space-y-4 text-xs py-4 border-b border-zinc-800">
              <div className="flex justify-between items-center text-zinc-300">
                <span className="font-bold">Tiền vé ({visibleSeats.length} ghế):</span>
                <span className="font-black text-sm">{formatCurrency(booking.ticketAmount)}</span>
              </div>
              <div className="flex justify-between items-center text-zinc-300">
                <span className="font-bold">Tiền bắp nước:</span>
                <span className="font-black text-sm">{formatCurrency(foodAmount)}</span>
              </div>
              {booking.promotionDiscount > 0 && (
                <div className="flex justify-between items-center text-emerald-400 font-bold bg-emerald-500/10 p-2 rounded-lg border border-emerald-500/20">
                  <span>Khuyến mãi / Giảm giá:</span>
                  <span>-{formatCurrency(booking.promotionDiscount)}</span>
                </div>
              )}
              <div className="flex justify-between text-zinc-500 text-[10px] pt-1">
                <span>Thuế GTGT (VAT) đã bao gồm:</span>
                <span>10%</span>
              </div>
            </div>

            {/* Grand Total box */}
            <div className="flex justify-between items-center py-5 px-5 bg-zinc-950/80 rounded-2xl border border-brand-orange/30 shadow-[0_0_15px_rgba(255,122,0,0.1)]">
              <div>
                <span className="text-[10px] text-zinc-400 font-black uppercase tracking-wider block mb-0.5">Tổng số tiền</span>
                <span className="text-[9px] text-brand-orange/80 font-bold uppercase">Đã bao gồm VAT</span>
              </div>
              <span className="text-2xl md:text-3xl font-black text-brand-orange tracking-tight">
                {formatCurrency(booking.finalAmount)}
              </span>
            </div>

            {/* Terms and Conditions for step 4 */}
            {step === 4 && (
              <label className="flex items-start gap-3 select-none cursor-pointer py-1">
                <input
                  type="checkbox"
                  checked={termsAgreed}
                  onChange={(e) => setTermsAgreed(e.target.checked)}
                  className="mt-1 accent-brand-orange w-4 h-4 shrink-0 rounded border-zinc-800 bg-zinc-950 focus:ring-0"
                />
                <span className="text-[10px] text-zinc-500 font-bold leading-normal uppercase">
                  Tôi đồng ý với Điều khoản và Quy định của LoraFilm về giao dịch mua vé trực tuyến.
                </span>
              </label>
            )}

            {/* Action buttons */}
            <div className="space-y-3 pt-2">
              {step === 3 ? (
                <button
                  disabled={isExpired}
                  onClick={() => setStep(4)}
                  className={`w-full py-4 rounded-2xl font-black uppercase text-xs tracking-wider shadow-lg transition-all duration-300 transform ${
                    !isExpired
                      ? 'bg-brand-orange hover:bg-opacity-95 hover:scale-[1.02] text-white shadow-brand-orange/25 cursor-pointer'
                      : 'bg-zinc-850 text-zinc-500 border border-zinc-800 cursor-not-allowed'
                  }`}
                >
                  Xác Nhận & Tiếp Tục
                </button>
              ) : (
                <button
                  onClick={() => setStep(3)}
                  className="w-full py-3.5 bg-transparent border border-zinc-800 text-zinc-400 hover:text-white hover:bg-zinc-800/40 rounded-2xl font-bold uppercase text-[10px] tracking-widest transition-colors cursor-pointer text-center block"
                >
                  Quay lại chọn bắp nước
                </button>
              )}

              <button
                disabled={paymentLoading || isExpired || cancelling}
                onClick={() => {
                  setCancelError('');
                  setCancelModalOpen(true);
                }}
                className="w-full py-2.5 text-center text-zinc-600 hover:text-red-400 font-semibold text-[10px] uppercase tracking-wider transition-colors cursor-pointer disabled:cursor-not-allowed disabled:opacity-50 block"
              >
                Hủy giao dịch
              </button>
            </div>

            {/* Safety policy */}
            <div className="flex items-center gap-2 text-[9px] text-zinc-650 font-bold uppercase justify-center mt-4 border-t border-zinc-800/60 pt-4">
              <ShieldCheck className="w-4 h-4 text-zinc-600" />
              <span>Thanh toán an toàn bảo mật</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
