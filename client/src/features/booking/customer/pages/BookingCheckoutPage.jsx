import { useState, useEffect, useMemo, useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Clock, AlertTriangle, ChevronRight, Search, ShieldCheck, Check, Info } from 'lucide-react';
import { getBookingDetails, cancelBooking } from '../services/bookingService';
import { getConcessions, getBookingFoodOrder, addFoodItem, updateFoodQuantity, removeFoodItem } from '../services/foodService';
import BookingStepper from '../components/BookingStepper';
import axios from 'axios';

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

  // Filter & Search states
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  
  // Terms agreement state for payment step
  const [termsAgreed, setTermsAgreed] = useState(false);

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
        foodOrder,
        finalAmount: bookingData.finalAmount ?? bookingData.totalAmount ?? 0,
        ticketAmount: bookingData.ticketAmount ?? bookingData.totalAmount ?? 0,
        expiresAt: bookingData.expiresAt ?? bookingData.expiredAt ?? bookingData.paymentDeadline,
        snapshot: bookingData.snapshot ?? bookingDraft.showtime ?? null
      });

      const concessionsData = await getConcessions();
      setConcessions(concessionsData || []);
    } catch (err) {
      setError(err.message || err.detail || "Không thể tải thông tin đặt vé.");
    } finally {
      setLoading(false);
    }
  }, [bookingId, bookingDraft.showtime]);

  useEffect(() => {
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
      alert("Thời gian giữ ghế đã hết hạn. Đơn hàng của bạn đã bị hủy.");
      navigate('/movies?error=expired');
    }
  }, [timeLeft, navigate]);

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
          foodOrder: freshBooking.foodOrder,
          finalAmount: freshBooking.finalAmount
        }));
        setCartUpdatingId(null);
        return;
      }

      setBooking(prev => {
        const foodAmount = updatedFoodOrder ? updatedFoodOrder.finalAmount : 0;
        const total = (prev.ticketAmount || 0) + foodAmount - (prev.promotionDiscount || 0);
        return {
          ...prev,
          foodOrder: updatedFoodOrder,
          finalAmount: total
        };
      });
    } catch (err) {
      alert("Không thể cập nhật giỏ hàng bắp nước: " + (err.message || "Lỗi kết nối"));
    } finally {
      setCartUpdatingId(null);
    }
  };

  // Simulate payment status
  const handleSimulatePayment = async (success = true) => {
    if (!termsAgreed && success) {
      alert("Bạn phải đồng ý với Điều khoản & Điều kiện trước khi thanh toán.");
      return;
    }
    setPaymentLoading(true);
    try {
      // Initiate payment simulation on mock controller
      const endpoint = success ? '/internal/mock/payment/success' : '/internal/mock/payment/fail';
      await axios.post(endpoint, {
        bookingCode: booking.bookingCode
      });

      // Redirect accordingly
      if (success) {
        navigate(`/bookings/success?bookingId=${bookingId}`);
      } else {
        navigate(`/bookings/failed?bookingId=${bookingId}`);
      }
    } catch (err) {
      alert("Lỗi khi thực hiện giả lập thanh toán: " + (err.response?.data?.message || err.message));
    } finally {
      setPaymentLoading(false);
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
  const isExpired = timeLeft === 0;
  const draftSeats = bookingDraft.selectedSeats || [];
  const visibleSeats = booking.tickets?.length ? booking.tickets : draftSeats;
  const showtimeStart = snapshot?.showtimeStart || snapshot?.startTime;
  const movie = snapshot?.movie || {};
  const cinema = snapshot?.cinema || {};
  const auditorium = snapshot?.auditorium || {};

  return (
    <div className="bg-zinc-950 text-zinc-100 min-h-screen pt-28 pb-16 px-4 md:px-12 selection:bg-brand-orange selection:text-zinc-950 font-sans font-medium">
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
                          <div className="w-20 aspect-square rounded-xl bg-zinc-950/60 overflow-hidden border border-zinc-800 shrink-0">
                            <img
                              src={item.imageUrl}
                              alt={item.name}
                              className="w-full h-full object-cover"
                              onError={(e) => {
                                e.target.onerror = null;
                                e.target.src = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' fill='%231f2937'><rect width='100%' height='100%'/></svg>";
                              }}
                            />
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
              /* Step 4: Payment Placeholder */
              <div className="space-y-8">
                {/* Simulated Payment Methods */}
                <div className="bg-zinc-900/40 border border-zinc-800/80 rounded-3xl p-6 md:p-8 space-y-6">
                  <div>
                    <h2 className="text-lg font-black text-white uppercase tracking-wider">Chọn Phương Thức Thanh Toán</h2>
                    <p className="text-[10px] text-zinc-500 font-bold uppercase mt-1">Hệ thống thanh toán giả lập dành cho thử nghiệm</p>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 opacity-50">
                    <div className="border border-zinc-800 rounded-2xl p-5 flex items-center gap-4 bg-zinc-900/30 cursor-not-allowed">
                      <div className="w-12 aspect-[4/3] bg-white rounded-lg p-1.5 flex items-center justify-center">
                        <img src="https://sandbox.vnpayment.vn/paymentv2/Images/brands/logo.svg" alt="VNPay Logo" className="max-h-full max-w-full object-contain" />
                      </div>
                      <div>
                        <h4 className="text-xs font-black text-zinc-400">VNPay (Coming Soon)</h4>
                        <p className="text-[9px] text-zinc-650 mt-0.5">Hỗ trợ ngân hàng nội địa & quốc tế</p>
                      </div>
                    </div>

                    <div className="border border-zinc-800 rounded-2xl p-5 flex items-center gap-4 bg-zinc-900/30 cursor-not-allowed">
                      <div className="w-12 aspect-[4/3] bg-pink-100 rounded-lg p-2 flex items-center justify-center">
                        <img src="https://upload.wikimedia.org/wikipedia/vi/f/fe/MoMo_Logo.png" alt="Momo Logo" className="max-h-full max-w-full object-contain" />
                      </div>
                      <div>
                        <h4 className="text-xs font-black text-zinc-400">MoMo (Coming Soon)</h4>
                        <p className="text-[9px] text-zinc-650 mt-0.5">Thanh toán nhanh qua ví điện tử</p>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Direct Simulation Actions */}
                <div className="bg-zinc-900/40 border border-zinc-800/80 rounded-3xl p-6 md:p-8 space-y-6">
                  <div>
                    <h2 className="text-lg font-black text-white uppercase tracking-wider">Giả Lập Quy Trình Thanh Toán</h2>
                    <p className="text-[10px] text-zinc-500 font-bold uppercase mt-1">Lựa chọn kết quả thanh toán mong muốn dưới đây</p>
                  </div>

                  <div className="flex flex-col sm:flex-row gap-4">
                    <button
                      onClick={() => handleSimulatePayment(true)}
                      disabled={paymentLoading || isExpired}
                      className="flex-1 py-4 bg-emerald-500 hover:bg-emerald-600 disabled:bg-zinc-800 disabled:text-zinc-600 text-black font-black uppercase text-xs tracking-wider rounded-2xl shadow-lg transition-all cursor-pointer flex items-center justify-center gap-2"
                    >
                      <Check className="w-4 h-4 stroke-[3]" />
                      <span>Giả lập Thanh toán Thành công</span>
                    </button>

                    <button
                      onClick={() => handleSimulatePayment(false)}
                      disabled={paymentLoading || isExpired}
                      className="flex-1 py-4 bg-red-500 hover:bg-red-650 disabled:bg-zinc-800 disabled:text-zinc-600 text-white font-black uppercase text-xs tracking-wider rounded-2xl shadow-lg transition-all cursor-pointer flex items-center justify-center gap-2"
                    >
                      <AlertTriangle className="w-4 h-4" />
                      <span>Giả lập Thanh toán Thất bại</span>
                    </button>
                  </div>
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
              <div className="w-16 aspect-[2/3] rounded-xl overflow-hidden bg-zinc-950 border border-zinc-800 shrink-0">
                <img
                  src={snapshot?.moviePoster}
                  alt={snapshot?.movieTitle}
                  className="w-full h-full object-cover"
                  onError={(e) => {
                    e.target.onerror = null;
                    e.target.src = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' fill='%2318181b'><rect width='100%' height='100%'/></svg>";
                  }}
                />
              </div>
              <div className="space-y-1.5 flex-grow">
                <h3 className="text-sm font-black text-white line-clamp-2 leading-snug">{snapshot?.movieTitle}</h3>
                <div className="flex items-center gap-1.5 text-[10px] text-zinc-500 font-semibold">
                  <span>{snapshot?.duration || movie?.durationMinutes || '--'} phút</span>
                  <span>•</span>
                  <span className="text-brand-yellow font-black border border-brand-yellow/30 px-1 py-0.2 rounded text-[8px]">{snapshot?.ageRating || movie?.ageRating || 'P'}</span>
                </div>
              </div>
            </div>

            {/* Booking Details */}
            <div className="space-y-3 py-2 text-xs border-b border-zinc-800">
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Cụm rạp</span>
                <span className="text-white font-bold text-right">{snapshot?.cinemaName || cinema?.name || `Rạp #${booking.cinemaId || '--'}`}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Phòng chiếu</span>
                <span className="text-zinc-200 font-bold text-right">{snapshot?.auditoriumName || auditorium?.name || `Phòng #${booking.auditoriumId || '--'}`}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Suất chiếu</span>
                <span className="text-white font-bold text-right">
                  {showtimeStart ? new Date(showtimeStart).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false }) : ''} | {showtimeStart ? new Date(showtimeStart).toLocaleDateString('vi-VN') : ''}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Số lượng ghế</span>
                <span className="text-brand-orange font-black text-right">{snapshot?.seatCount || visibleSeats.length || '--'} ghế</span>
              </div>
            </div>

            {/* Selected Seats */}
            <div className="py-2 border-b border-zinc-800 space-y-2">
              <span className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Các vị trí ghế</span>
              <div className="flex flex-wrap gap-1.5">
                {visibleSeats.map((t, index) => (
                  <span key={t.id || t.publicId || index} className="text-[10px] bg-zinc-800 text-zinc-200 px-2 py-0.5 rounded font-black">
                    {t.seatLabel || t.seatCode} {t.seatType ? `(${t.seatType})` : ''}
                  </span>
                ))}
              </div>
            </div>

            {/* Food items breakdown */}
            <div className="py-2 border-b border-zinc-800 space-y-3">
              <span className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Bắp nước đã chọn</span>
              {booking.foodOrder && booking.foodOrder.items && booking.foodOrder.items.length > 0 ? (
                <div className="space-y-2 max-h-36 overflow-y-auto pr-1">
                  {booking.foodOrder.items.map(item => (
                    <div key={item.id} className="flex justify-between text-[11px]">
                      <span className="text-zinc-400 line-clamp-1 max-w-[150px]">{item.productName}</span>
                      <span className="text-zinc-500 font-bold">x{item.quantity}</span>
                      <span className="text-zinc-200 font-bold">{formatCurrency(item.finalAmount)}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-[11px] text-zinc-650 italic">Chưa chọn bắp nước đi kèm</div>
              )}
            </div>

            {/* Pricing breakdown */}
            <div className="space-y-3 text-xs py-2">
              <div className="flex justify-between text-zinc-400">
                <span>Tiền vé:</span>
                <span>{formatCurrency(booking.ticketAmount)}</span>
              </div>
              <div className="flex justify-between text-zinc-400">
                <span>Tiền bắp nước:</span>
                <span>{formatCurrency(booking.foodOrder ? booking.foodOrder.finalAmount : 0)}</span>
              </div>
              {booking.promotionDiscount > 0 && (
                <div className="flex justify-between text-emerald-500 font-bold">
                  <span>Khuyến mãi:</span>
                  <span>-{formatCurrency(booking.promotionDiscount)}</span>
                </div>
              )}
              <div className="flex justify-between text-zinc-500 text-[10px] border-t border-zinc-800 pt-3">
                <span>VAT (đã bao gồm):</span>
                <span>10%</span>
              </div>

              {/* Grand Total box */}
              <div className="flex justify-between items-center py-4 px-4 bg-zinc-950/60 rounded-2xl border border-zinc-850 shadow-inner">
                <div>
                  <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Tổng số tiền</span>
                </div>
                <span className="text-lg md:text-xl font-black text-brand-orange">
                  {formatCurrency(booking.finalAmount)}
                </span>
              </div>
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
                disabled={paymentLoading}
                onClick={async () => {
                  if (confirm("Bạn có chắc chắn muốn hủy đơn hàng này không?")) {
                    try {
                      await cancelBooking(bookingId, "Khách hàng chủ động hủy đặt chỗ");
                      navigate('/movies');
                    } catch (e) {
                      alert("Không thể hủy đặt vé: " + e.message);
                    }
                  }
                }}
                className="w-full py-2.5 text-center text-zinc-600 hover:text-red-400 font-semibold text-[10px] uppercase tracking-wider transition-colors cursor-pointer block"
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
