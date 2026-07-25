import { useState, useEffect, useMemo, useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Clock, AlertTriangle, ChevronRight, Search, ShieldCheck, Check } from 'lucide-react';
import { getBookingDetails, initiatePayment, cancelBooking } from '../services/bookingService';
import { getConcessions, addFoodItem, updateFoodQuantity, removeFoodItem } from '../services/foodService';

export default function BookingCheckoutPage() {
  const location = useLocation();
  const navigate = useNavigate();

  // Extract bookingId from query params
  const bookingId = useMemo(() => {
    const params = new URLSearchParams(location.search);
    return params.get('bookingId');
  }, [location.search]);

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
  const [paymentMethod, setPaymentMethod] = useState('VNPAY');

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
      setBooking(bookingData);
      
      const concessionsData = await getConcessions();
      setConcessions(concessionsData || []);
    } catch (err) {
      setError(err.message || err.detail || "Không thể tải thông tin đặt vé.");
    } finally {
      setLoading(false);
    }
  }, [bookingId]);

  useEffect(() => {
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
      // Expiration redirect
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
        // Manually fetch fresh booking details since remove response is empty
        const freshBooking = await getBookingDetails(bookingId);
        setBooking(freshBooking);
        setCartUpdatingId(null);
        return;
      }

      // Update state with updated food order and adjust booking totals
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

  // Handle Payment initiation
  const handlePayment = async () => {
    if (paymentLoading) return;
    setPaymentLoading(true);
    try {
      const response = await initiatePayment(bookingId, {
        paymentMethod,
        channel: "Web Portal"
      });
      if (response && response.paymentUrl) {
        // Redirect to mock payment gateway
        window.location.href = response.paymentUrl;
      } else {
        alert("Không thể tạo liên kết thanh toán. Vui lòng thử lại.");
      }
    } catch (err) {
      alert("Đã xảy ra lỗi khi thanh toán: " + (err.message || err.detail || "Lỗi hệ thống"));
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
          <p className="text-sm font-semibold tracking-wider text-zinc-400">Đang chuẩn bị thanh toán...</p>
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
        <h2 className="text-xl font-bold text-zinc-100 mb-2">Thanh toán thất bại</h2>
        <p className="text-sm text-zinc-400 max-w-md mb-8">{error || "Đơn hàng này không tồn tại."}</p>
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

  return (
    <div className="bg-zinc-950 text-zinc-100 min-h-screen pt-32 pb-16 px-4 md:px-12 selection:bg-brand-orange selection:text-zinc-950 font-sans font-medium">
      <div className="max-w-7xl mx-auto w-full">
        {/* Progress Path */}
        <div className="flex items-center gap-3 text-xs font-semibold text-zinc-500 uppercase tracking-widest mb-10 overflow-x-auto py-2">
          <span>1. Chọn Ghế</span>
          <ChevronRight className="w-4 h-4" />
          <span className="text-brand-orange font-black">2. Bắp Nước & Thanh Toán</span>
          <ChevronRight className="w-4 h-4" />
          <span>3. Nhận Vé</span>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
          {/* Left panel: F&B selection & payment selection */}
          <div className="lg:col-span-2 space-y-8">
            
            {/* Countdown notice on mobile */}
            <div className="lg:hidden bg-zinc-900 border border-zinc-800 rounded-2xl p-4 flex items-center justify-between gap-4">
              <div className="flex items-center gap-2">
                <Clock className="w-5 h-5 text-amber-500 shrink-0" />
                <span className="text-xs text-zinc-400 font-bold">Thời gian hoàn tất giao dịch</span>
              </div>
              <span className={`text-sm font-black tracking-widest ${timeLeft < 60 ? 'text-red-500 animate-pulse' : 'text-amber-500'}`}>
                {formattedTimeLeft}
              </span>
            </div>

            {/* Food Selection Grid Section */}
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
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-xl pl-11 pr-4 py-2.5 text-xs text-zinc-200 focus:outline-none focus:border-brand-orange placeholder:text-zinc-650 transition-colors"
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
                        <div className="w-20 aspect-square rounded-xl bg-zinc-950/60 overflow-hidden border border-zinc-850 shrink-0">
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
                            <p className="text-[9px] text-zinc-500 leading-normal line-clamp-2">{item.description}</p>
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
                <div className="text-center py-10 bg-zinc-900/20 border border-zinc-850 rounded-2xl border-dashed">
                  <span className="text-xs text-zinc-500 italic block">Không tìm thấy bắp nước phù hợp...</span>
                </div>
              )}
            </div>

            {/* Payment Method Section */}
            <div className="bg-zinc-900/40 border border-zinc-800/80 rounded-3xl p-6 md:p-8 space-y-6">
              <div>
                <h2 className="text-lg font-black text-white uppercase tracking-wider">Phương Thức Thanh Toán</h2>
                <p className="text-[10px] text-zinc-500 font-bold uppercase mt-1">Vui lòng chọn cổng thanh toán tiện lợi nhất</p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {/* VNPay Option */}
                <label className={`border-2 rounded-2xl p-5 flex items-center gap-4 cursor-pointer select-none transition-all ${
                  paymentMethod === 'VNPAY'
                    ? 'border-brand-orange bg-brand-orange/5'
                    : 'border-zinc-800 hover:border-zinc-750 bg-zinc-900/30'
                }`}>
                  <input
                    type="radio"
                    name="paymentMethod"
                    value="VNPAY"
                    checked={paymentMethod === 'VNPAY'}
                    onChange={() => setPaymentMethod('VNPAY')}
                    className="sr-only"
                  />
                  <div className="w-12 aspect-[4/3] bg-white rounded-lg p-1.5 shrink-0 flex items-center justify-center border border-zinc-200">
                    <img src="/images/vnpay-logo.png" alt="VNPay Logo" className="max-h-full max-w-full object-contain" onError={(e)=>{e.target.src="https://sandbox.vnpayment.vn/paymentv2/Images/brands/logo.svg"}}/>
                  </div>
                  <div className="flex-grow">
                    <h4 className="text-xs font-black text-white">Thanh toán qua cổng VNPay</h4>
                    <p className="text-[9px] text-zinc-500 mt-0.5">Hỗ trợ QR Code, thẻ ATM nội địa, thẻ Visa/Master</p>
                  </div>
                  {paymentMethod === 'VNPAY' && (
                    <div className="w-5 h-5 rounded-full bg-brand-orange flex items-center justify-center shrink-0">
                      <Check className="w-3 h-3 text-white stroke-[4]" />
                    </div>
                  )}
                </label>

                {/* Momo Option */}
                <label className={`border-2 rounded-2xl p-5 flex items-center gap-4 cursor-pointer select-none transition-all ${
                  paymentMethod === 'MOMO'
                    ? 'border-brand-orange bg-brand-orange/5'
                    : 'border-zinc-800 hover:border-zinc-750 bg-zinc-900/30'
                }`}>
                  <input
                    type="radio"
                    name="paymentMethod"
                    value="MOMO"
                    checked={paymentMethod === 'MOMO'}
                    onChange={() => setPaymentMethod('MOMO')}
                    className="sr-only"
                  />
                  <div className="w-12 aspect-[4/3] bg-pink-100 rounded-lg p-2 shrink-0 flex items-center justify-center border border-zinc-200">
                    <img src="/images/momo-logo.png" alt="Momo Logo" className="max-h-full max-w-full object-contain" onError={(e)=>{e.target.src="https://upload.wikimedia.org/wikipedia/vi/f/fe/MoMo_Logo.png"}}/>
                  </div>
                  <div className="flex-grow">
                    <h4 className="text-xs font-black text-white">Thành toán qua ví MoMo</h4>
                    <p className="text-[9px] text-zinc-500 mt-0.5">Thanh toán nhanh chóng bằng quét mã MoMo</p>
                  </div>
                  {paymentMethod === 'MOMO' && (
                    <div className="w-5 h-5 rounded-full bg-brand-orange flex items-center justify-center shrink-0">
                      <Check className="w-3 h-3 text-white stroke-[4]" />
                    </div>
                  )}
                </label>
              </div>
            </div>
          </div>

          {/* Right panel: Sticky order info sidebar */}
          <div className="lg:col-span-1 sticky top-24 bg-zinc-900 border border-zinc-800 rounded-3xl p-6 space-y-6 shadow-2xl">
            
            {/* Desktop Countdown Timer */}
            <div className="hidden lg:flex items-center justify-between gap-4 py-3 px-4 bg-zinc-950/60 rounded-2xl border border-zinc-850 shadow-inner">
              <div className="flex items-center gap-2">
                <Clock className="w-4 h-4 text-amber-500" />
                <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Thời gian thanh toán</span>
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
                  <span>{snapshot?.duration} phút</span>
                  <span>•</span>
                  <span className="text-brand-yellow font-black border border-brand-yellow/30 px-1 py-0.2 rounded text-[8px]">{snapshot?.ageRating}</span>
                </div>
              </div>
            </div>

            {/* Booking Details */}
            <div className="space-y-3 py-2 text-xs border-b border-zinc-800">
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Cụm rạp</span>
                <span className="text-white font-bold text-right">{snapshot?.cinemaName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Phòng chiếu</span>
                <span className="text-zinc-200 font-bold text-right">{snapshot?.auditoriumName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Suất chiếu</span>
                <span className="text-white font-bold text-right">
                  {snapshot?.showtimeStart ? new Date(snapshot.showtimeStart).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false }) : ''} | {snapshot?.showtimeStart ? new Date(snapshot.showtimeStart).toLocaleDateString('vi-VN') : ''}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Số lượng ghế</span>
                <span className="text-brand-orange font-black text-right">{snapshot?.seatCount} ghế</span>
              </div>
            </div>

            {/* Selected Seats */}
            <div className="py-2 border-b border-zinc-800 space-y-2">
              <span className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Các vị trí ghế</span>
              <div className="flex flex-wrap gap-1.5">
                {(booking.tickets || []).map(t => (
                  <span key={t.id} className="text-[10px] bg-zinc-800 text-zinc-200 px-2 py-0.5 rounded font-black">
                    {t.seatLabel} ({t.seatType})
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
                <div className="text-[11px] text-zinc-600 italic">Chưa chọn bắp nước đi kèm</div>
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
                <span>Thuế giá trị gia tăng (VAT):</span>
                <span>Bao gồm 10%</span>
              </div>
              <div className="flex justify-between items-center py-4 px-4 bg-zinc-950/60 rounded-2xl border border-zinc-850 shadow-inner">
                <div>
                  <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Tổng tiền</span>
                </div>
                <span className="text-lg md:text-xl font-black text-brand-orange">
                  {formatCurrency(booking.finalAmount)}
                </span>
              </div>
            </div>

            {/* Action buttons */}
            <div className="space-y-3 pt-2">
              <button
                disabled={isExpired || paymentLoading}
                onClick={handlePayment}
                className={`w-full py-4 rounded-2xl font-black uppercase text-xs tracking-wider shadow-lg transition-all duration-300 transform ${
                  !isExpired && !paymentLoading
                    ? 'bg-brand-orange hover:bg-opacity-95 hover:scale-[1.02] text-white shadow-brand-orange/25'
                    : 'bg-zinc-850 text-zinc-550 border border-zinc-800 cursor-not-allowed'
                }`}
              >
                {paymentLoading ? 'Đang khởi tạo thanh toán...' : 'Thanh Toán Ngay'}
              </button>

              <button
                disabled={paymentLoading}
                onClick={async () => {
                  if (confirm("Bạn có chắc chắn muốn hủy đơn hàng này không?")) {
                    try {
                      await cancelBooking(bookingId, "Khách hàng chủ động hủy từ trang thanh toán");
                      navigate('/movies');
                    } catch (e) {
                      alert("Không thể hủy đặt vé: " + e.message);
                    }
                  }
                }}
                className="w-full py-3.5 bg-transparent border border-zinc-800 text-zinc-400 hover:text-white hover:bg-zinc-800/40 rounded-2xl font-bold uppercase text-[10px] tracking-widest transition-colors cursor-pointer text-center block"
              >
                Hủy đặt vé
              </button>
            </div>
            
            {/* Safety policy */}
            <div className="flex items-center gap-2 text-[9px] text-zinc-500 font-bold uppercase justify-center mt-4">
              <ShieldCheck className="w-4 h-4 text-zinc-500" />
              <span>Giao dịch an toàn & bảo mật</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
