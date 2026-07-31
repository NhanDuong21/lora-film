import {
  memo,
  useCallback,
  useDeferredValue,
  useEffect,
  useMemo,
  useRef,
  useState
} from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Clock, AlertTriangle, Search, ShieldCheck, CreditCard } from 'lucide-react';
import {
  BOOKING_CHANGED_EVENT,
  getBookingDetails,
  cancelBooking,
  finalizeCheckout,
  getOrCreateScoreRedemptionKey
} from '../services/bookingService';
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
import {
  paymentErrorCode,
  paymentErrorMessage
} from '@/features/payment/services/paymentService';
import { getOptimizedImageUrl } from '@/utils/imageOptimization';

const FALLBACK_POSTER = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='500' height='750' viewBox='0 0 500 750'><rect width='500' height='750' fill='%2309090b'/><text x='50%25' y='48%25' dominant-baseline='middle' text-anchor='middle' font-family='sans-serif' font-weight='bold' font-size='32' fill='%2352525b'>LORA FILM</text><text x='50%25' y='54%25' dominant-baseline='middle' text-anchor='middle' font-family='sans-serif' font-size='17' fill='%233f3f46'>Chưa có áp phích</text></svg>";

const MOMO_LOGO_URL = 'https://upload.wikimedia.org/wikipedia/commons/a/a0/MoMo_Logo_App.svg';
const MOMO_LOGO_FALLBACK_URL = 'https://res.cloudinary.com/dqc4hufot/image/upload/f_auto,q_auto,w_96/logo_jg9h5v.png';
const CONCESSION_PAGE_SIZE = 12;
const CHECKOUT_PHASE = Object.freeze({
  ADD_ONS: 'ADD_ONS',
  PAYMENT: 'PAYMENT'
});
const CATEGORY_LABELS = {
  ALL: 'Tất cả',
  FOOD: 'Đồ ăn',
  DRINK: 'Nước uống',
  COMBO: 'Combo'
};

const formatCurrency = value => `${Number(value || 0).toLocaleString('vi-VN')}đ`;

const secondsUntil = expiresAt => {
  const deadline = new Date(expiresAt).getTime();
  if (!Number.isFinite(deadline)) return 0;
  return Math.max(0, Math.floor((deadline - Date.now()) / 1000));
};

const BookingCountdown = memo(function BookingCountdown({
  expiresAt,
  onExpire,
  className = ''
}) {
  const [seconds, setSeconds] = useState(() => secondsUntil(expiresAt));
  const notifiedRef = useRef(false);

  useEffect(() => {
    notifiedRef.current = false;
    const update = () => {
      const remaining = secondsUntil(expiresAt);
      setSeconds(current => current === remaining ? current : remaining);
      if (remaining === 0 && !notifiedRef.current) {
        notifiedRef.current = true;
        onExpire();
      }
    };

    update();
    const timer = window.setInterval(update, 1000);
    return () => window.clearInterval(timer);
  }, [expiresAt, onExpire]);

  const minutes = String(Math.floor(seconds / 60)).padStart(2, '0');
  const remainingSeconds = String(seconds % 60).padStart(2, '0');

  return (
    <span className={`${className} ${seconds < 60 ? 'text-red-500 animate-pulse' : 'text-amber-500'}`}>
      {minutes}:{remainingSeconds}
    </span>
  );
});

const ConcessionProductCard = memo(function ConcessionProductCard({
  item,
  quantity,
  isUpdating,
  disabled,
  onQuantityChange
}) {
  const imageUrl = getOptimizedImageUrl(item.imageUrl, { width: 192, height: 192 });

  return (
    <article
      className="relative flex min-h-40 gap-4 overflow-hidden rounded-2xl border border-zinc-800/80 bg-zinc-900 p-4 transition-colors hover:border-zinc-700/80"
      style={{ contentVisibility: 'auto', containIntrinsicSize: '160px' }}
    >
      <div className="relative flex aspect-square w-20 shrink-0 items-center justify-center overflow-hidden rounded-xl border border-zinc-800 bg-zinc-950/60">
        <span className="px-2 text-center text-[8px] font-bold text-zinc-600">Chưa có ảnh</span>
        {imageUrl && (
          <img
            src={imageUrl}
            alt={item.name}
            width="80"
            height="80"
            loading="lazy"
            decoding="async"
            fetchPriority="low"
            className="absolute inset-0 h-full w-full object-cover"
            onError={event => {
              event.currentTarget.style.display = 'none';
            }}
          />
        )}
      </div>

      <div className="flex flex-grow flex-col justify-between space-y-3 py-0.5">
        <div className="space-y-1">
          <span className="rounded bg-zinc-800 px-1.5 py-0.5 text-[8px] font-black uppercase tracking-wider text-zinc-400">
            {CATEGORY_LABELS[String(item.type || '').toUpperCase()] || item.type || 'Combo'}
          </span>
          <h4 className="line-clamp-1 text-xs font-black leading-snug text-white">{item.name}</h4>
          <p className="line-clamp-2 text-[9px] leading-normal text-zinc-500">{item.description}</p>
        </div>

        <div className="flex items-center justify-between">
          <span className="text-xs font-black text-brand-orange">{formatCurrency(item.price)}</span>
          <div className="flex items-center gap-3 rounded-lg border border-zinc-800 bg-zinc-950 p-1">
            <button
              type="button"
              aria-label={`Giảm số lượng ${item.name}`}
              disabled={quantity === 0 || isUpdating || disabled}
              onClick={() => onQuantityChange(item, false, quantity)}
              className={`flex h-6 w-6 items-center justify-center rounded text-xs font-black transition-colors ${
                quantity > 0 && !isUpdating && !disabled
                  ? 'bg-zinc-800 text-zinc-300 hover:bg-zinc-700'
                  : 'cursor-not-allowed text-zinc-700'
              }`}
            >
              -
            </button>
            <span className="w-5 text-center text-xs font-bold text-zinc-200">
              {isUpdating ? '...' : quantity}
            </span>
            <button
              type="button"
              aria-label={`Tăng số lượng ${item.name}`}
              disabled={isUpdating || disabled}
              onClick={() => onQuantityChange(item, true, quantity)}
              className={`flex h-6 w-6 items-center justify-center rounded text-xs font-black transition-colors ${
                !isUpdating && !disabled
                  ? 'bg-brand-orange text-white hover:bg-opacity-90'
                  : 'cursor-not-allowed text-zinc-700'
              }`}
            >
              +
            </button>
          </div>
        </div>
      </div>
    </article>
  );
});

const getBookingStatus = booking => booking?.bookingStatus || booking?.status;

const mergeCheckoutBooking = (current, fresh) => ({
  ...current,
  ...fresh,
  snapshot: fresh?.snapshot || current?.snapshot || null,
  foodOrder: fresh?.foodOrder ?? fresh?.food ?? current?.foodOrder ?? null,
  finalAmount: fresh?.totalAmount ?? fresh?.finalAmount ?? current?.finalAmount ?? 0,
  ticketAmount: fresh?.ticketAmount ?? current?.ticketAmount ?? 0,
  expiresAt: fresh?.expiresAt
    ?? fresh?.expiredAt
    ?? fresh?.paymentDeadline
    ?? current?.expiresAt
});

const getTerminalBookingNotice = (status, bookingId) => {
  if (status === 'CANCELLED') {
    return {
      title: 'Đơn đã được hủy',
      message: 'Đơn này đã được hủy và ghế đã được trả lại. Bạn không thể tiếp tục thanh toán cho đơn này.',
      variant: 'warning',
      redirectTo: `/bookings/${bookingId}`
    };
  }
  if (status === 'EXPIRED') {
    return {
      title: 'Đã hết thời gian giữ ghế',
      message: 'Thời gian thanh toán của đơn đã kết thúc và ghế không còn được giữ. Vui lòng chọn lại suất chiếu và ghế.',
      variant: 'warning',
      redirectTo: '/movies'
    };
  }
  if (['CONFIRMED', 'COMPLETED'].includes(status)) {
    return {
      title: 'Đơn đã được thanh toán',
      message: 'Đơn này đã hoàn tất thanh toán. Bạn có thể mở chi tiết đơn để xem vé.',
      variant: 'success',
      redirectTo: `/bookings/${bookingId}`
    };
  }
  return {
    title: 'Đơn không còn thanh toán được',
    message: 'Trạng thái mới nhất của đơn không cho phép tạo thêm giao dịch thanh toán.',
    variant: 'warning',
    redirectTo: `/bookings/${bookingId}`
  };
};

export default function BookingCheckoutPage() {
  const location = useLocation();
  const navigate = useNavigate();

  // Extract bookingId from query params
  const bookingId = useMemo(() => {
    const params = new URLSearchParams(location.search);
    return params.get('bookingId');
  }, [location.search]);
  const bookingDraft = location.state || {};

  const [phase, setPhase] = useState(CHECKOUT_PHASE.ADD_ONS);

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
  const [catalogPage, setCatalogPage] = useState(1);
  const [catalogExpanded, setCatalogExpanded] = useState(false);

  // Terms agreement state for payment step
  const [termsAgreed, setTermsAgreed] = useState(false);
  const [selectedPaymentMethod, setSelectedPaymentMethod] = useState('VNPAY');
  const [userScore, setUserScore] = useState(null);
  const [scorePointsInput, setScorePointsInput] = useState('');
  const [scorePreview, setScorePreview] = useState(null);
  const [scorePreviewLoading, setScorePreviewLoading] = useState(false);
  const [scorePreviewError, setScorePreviewError] = useState('');
  const lastTerminalNoticeRef = useRef(null);
  const cartUpdatingRef = useRef(false);
  const expirationHandledRef = useRef(false);
  const [deadlineExpired, setDeadlineExpired] = useState(false);
  const deferredSearchQuery = useDeferredValue(searchQuery);

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
      setBooking(mergeCheckoutBooking(null, {
        ...bookingData,
        foodOrder: foodOrder ?? bookingData.foodOrder ?? bookingData.food ?? null,
        finalAmount: bookingData.totalAmount ?? bookingData.finalAmount ?? 0,
        ticketAmount: bookingData.ticketAmount ?? 0,
        expiresAt: bookingData.expiresAt ?? bookingData.expiredAt ?? bookingData.paymentDeadline,
        snapshot: bookingData.snapshot ?? bookingData.presentation ?? bookingDraft.showtime ?? null
      }));
      if (Number(bookingData.scorePointsUsed || 0) > 0) {
        setScorePointsInput(String(bookingData.scorePointsUsed));
        setScorePreview({
          eligible: true,
          requestedPoints: Number(bookingData.scorePointsUsed),
          discountAmount: Number(bookingData.scoreDiscount || 0),
          remainingAmount: Number(bookingData.totalAmount ?? bookingData.finalAmount ?? 0),
          locked: Boolean(bookingData.amountLockedAt)
        });
      }

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

  const refreshBookingState = useCallback(async ({ notifyTerminal = false } = {}) => {
    if (!bookingId) return null;

    try {
      const freshBooking = await getBookingDetails(bookingId);
      const freshStatus = getBookingStatus(freshBooking);
      setBooking(current => mergeCheckoutBooking(current, freshBooking));

      if (
        notifyTerminal
        && freshStatus
        && freshStatus !== 'PENDING_PAYMENT'
        && lastTerminalNoticeRef.current !== freshStatus
      ) {
        lastTerminalNoticeRef.current = freshStatus;
        setPaymentLoading(false);
        setNotice(getTerminalBookingNotice(freshStatus, bookingId));
      }
      return freshBooking;
    } catch {
      // Background synchronization must not replace an already loaded checkout
      // with a transient connection error. Commands still validate server-side.
      return null;
    }
  }, [bookingId]);

  useEffect(() => {
    if (!bookingId) return undefined;

    const handleBookingChanged = event => {
      const changedBookingId = event?.detail?.publicId;
      if (changedBookingId && changedBookingId !== bookingId) return;

      if (event?.detail?.action === 'CANCELLED') {
        setBooking(current => current
          ? { ...current, status: 'CANCELLED', bookingStatus: 'CANCELLED' }
          : current);
        setPaymentLoading(false);
        if (lastTerminalNoticeRef.current !== 'CANCELLED') {
          lastTerminalNoticeRef.current = 'CANCELLED';
          setNotice(getTerminalBookingNotice('CANCELLED', bookingId));
        }
      }
      void refreshBookingState({ notifyTerminal: true });
    };
    const handleFocus = () => {
      void refreshBookingState({ notifyTerminal: true });
    };
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        void refreshBookingState({ notifyTerminal: true });
      }
    };

    const refreshTimer = window.setInterval(
      () => void refreshBookingState({ notifyTerminal: true }),
      15_000
    );
    window.addEventListener(BOOKING_CHANGED_EVENT, handleBookingChanged);
    window.addEventListener('focus', handleFocus);
    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      window.clearInterval(refreshTimer);
      window.removeEventListener(BOOKING_CHANGED_EVENT, handleBookingChanged);
      window.removeEventListener('focus', handleFocus);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [bookingId, refreshBookingState]);

  const handleDeadlineExpired = useCallback(() => {
    if (expirationHandledRef.current) return;
    expirationHandledRef.current = true;
    setDeadlineExpired(true);
    setNotice({
      title: 'Thời gian giữ ghế đã kết thúc',
      message: 'Đơn không còn khả dụng để thanh toán và ghế sẽ được trả lại cho khách hàng khác.',
      variant: 'warning',
      redirectTo: '/movies?error=expired'
    });
  }, []);

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
    const normalizedSearch = deferredSearchQuery.trim().toLocaleLowerCase('vi-VN');
    return concessions.filter(c => {
      const matchSearch = (c.name || '').toLocaleLowerCase('vi-VN').includes(normalizedSearch) ||
                          (c.description || '').toLocaleLowerCase('vi-VN').includes(normalizedSearch);
      const matchCat = selectedCategory === 'ALL' || (c.type && c.type.toUpperCase() === selectedCategory);
      return matchSearch && matchCat;
    });
  }, [concessions, deferredSearchQuery, selectedCategory]);

  const catalogTotalPages = Math.max(
    1,
    Math.ceil(filteredConcessions.length / CONCESSION_PAGE_SIZE)
  );
  const currentCatalogPage = Math.min(catalogPage, catalogTotalPages);
  const pagedConcessions = useMemo(() => {
    const start = (currentCatalogPage - 1) * CONCESSION_PAGE_SIZE;
    return filteredConcessions.slice(start, start + CONCESSION_PAGE_SIZE);
  }, [currentCatalogPage, filteredConcessions]);
  const visibleConcessions = catalogExpanded
    ? pagedConcessions
    : filteredConcessions.slice(0, 4);

  const foodOrderItems = booking?.foodOrder?.items;
  const cartItemsByProductId = useMemo(() => {
    const result = new Map();
    const cartItems = Array.isArray(foodOrderItems)
      ? foodOrderItems
      : [];
    cartItems.forEach(item => {
      result.set(item.productId, {
        quantity: item.quantity || 0,
        itemId: item.id
      });
    });
    return result;
  }, [foodOrderItems]);

  // Handle add/modify food in cart
  const handleQuantityChange = useCallback(async (concession, increment, quantity) => {
    if (cartUpdatingRef.current) return;
    cartUpdatingRef.current = true;
    setCartUpdatingId(concession.id);

    const itemId = cartItemsByProductId.get(concession.id)?.itemId;
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
        setScorePreview(null);
        setScorePreviewError('');
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
      setScorePreview(null);
      setScorePreviewError('');
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
      cartUpdatingRef.current = false;
      setCartUpdatingId(null);
    }
  }, [bookingId, cartItemsByProductId]);

  const handlePreviewScore = async () => {
    const points = Number(scorePointsInput);
    if (!Number.isInteger(points) || points <= 0) {
      setScorePreview(null);
      setScorePreviewError('Vui lòng nhập số điểm nguyên lớn hơn 0.');
      return;
    }
    setScorePreviewLoading(true);
    setScorePreviewError('');
    try {
      const response = await scoreCustomerService.redeemPreview({
        bookingPublicId: bookingId,
        points
      });
      const preview = response?.data ?? response;
      if (!preview?.eligible) {
        setScorePreview(null);
        setScorePreviewError(preview?.message || 'Số điểm này chưa thể áp dụng cho đơn.');
        return;
      }
      setScorePreview(preview);
    } catch (err) {
      setScorePreview(null);
      setScorePreviewError(getBookingErrorMessage(
        err,
        'Không thể kiểm tra điểm lúc này. Vui lòng thử lại.'
      ));
    } finally {
      setScorePreviewLoading(false);
    }
  };

  const clearScoreSelection = () => {
    setScorePreview(null);
    setScorePointsInput('');
    setScorePreviewError('');
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
      const freshBooking = await getBookingDetails(bookingId);
      const freshStatus = getBookingStatus(freshBooking);
      const freshDeadline = freshBooking.expiresAt
        ?? freshBooking.expiredAt
        ?? freshBooking.paymentDeadline;
      setBooking(current => mergeCheckoutBooking(current, freshBooking));

      if (
        freshStatus !== 'PENDING_PAYMENT'
        || (freshDeadline && new Date(freshDeadline).getTime() <= Date.now())
      ) {
        const effectiveStatus = freshStatus === 'PENDING_PAYMENT'
          ? 'EXPIRED'
          : freshStatus;
        lastTerminalNoticeRef.current = effectiveStatus;
        setNotice(getTerminalBookingNotice(effectiveStatus, bookingId));
        return;
      }

      const selectedScorePoints = Number(scorePreview?.requestedPoints || 0);
      const finalized = await finalizeCheckout(bookingId, {
        scorePoints: selectedScorePoints,
        scoreIdempotencyKey: selectedScorePoints > 0
          ? getOrCreateScoreRedemptionKey(bookingId, selectedScorePoints)
          : null
      });
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
        message: 'Hệ thống thanh toán đã tiếp nhận yêu cầu. Bạn có thể tiếp tục theo dõi trạng thái giao dịch.',
        variant: 'success',
        redirectTo: `/bookings/${bookingId}`
      });
    } catch (err) {
      const errorCode = paymentErrorCode(err);
      const bookingUnavailable = [
        'BOOKING_CANCELLED',
        'BOOKING_NOT_PAYABLE',
        'BOOKING_PAYMENT_DEADLINE_EXPIRED',
        'BOOKING_SEATS_NOT_HELD'
      ].includes(errorCode);
      const alreadyPaid = [
        'BOOKING_ALREADY_PAID',
        'PAYMENT_ALREADY_SUCCESS'
      ].includes(errorCode);
      let latestBooking = null;
      if (bookingUnavailable || alreadyPaid) {
        latestBooking = await refreshBookingState();
      }
      const latestStatus = getBookingStatus(latestBooking);

      if (latestStatus && latestStatus !== 'PENDING_PAYMENT') {
        lastTerminalNoticeRef.current = latestStatus;
        setNotice(getTerminalBookingNotice(latestStatus, bookingId));
      } else {
        setNotice({
          title: errorCode === 'BOOKING_PAYMENT_DEADLINE_EXPIRED'
            ? 'Đã hết thời gian giữ ghế'
            : alreadyPaid
              ? 'Đơn đã được thanh toán'
              : bookingUnavailable
                ? 'Đơn không còn thanh toán được'
                : 'Không thể chuẩn bị thanh toán',
          message: paymentErrorMessage(err),
          variant: alreadyPaid ? 'success' : 'error',
          redirectTo: bookingUnavailable || alreadyPaid
            ? `/bookings/${bookingId}`
            : undefined
        });
      }
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
  const isExpired = deadlineExpired || !isPending;
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
  const displayMoviePosterUrl = moviePosterUrl
    ? getOptimizedImageUrl(moviePosterUrl, { width: 200, height: 300 })
    : FALLBACK_POSTER;
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
  const selectedScoreDiscount = Number(scorePreview?.discountAmount || 0);
  const displayFinalAmount = scorePreview?.eligible
    ? Number(scorePreview.remainingAmount)
    : Number(booking.finalAmount || 0);
  const maxScorePoints = Math.max(
    0,
    Math.min(
      availableScorePoints,
      Math.floor(Math.max(0, Number(booking.finalAmount || 0) - 1) / 1000)
    )
  );
  const isPaymentPhase = phase === CHECKOUT_PHASE.PAYMENT;

  return (
    <div className="min-h-screen bg-zinc-950 px-4 pb-12 pt-6 font-sans font-medium text-zinc-100 selection:bg-brand-orange selection:text-zinc-950 md:px-8">
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
        <BookingStepper currentStep={3} />

        <div className="grid grid-cols-1 items-start gap-5 lg:grid-cols-3">
          {/* Left panel: F&B selection OR payment selection */}
          <div className="lg:col-span-2 space-y-8">
            {/* Countdown notice on mobile */}
            <div className="lg:hidden bg-zinc-900 border border-zinc-800 rounded-2xl p-4 flex items-center justify-between gap-4">
              <div className="flex items-center gap-2">
                <Clock className="w-5 h-5 text-amber-500 shrink-0" />
                <span className="text-xs text-zinc-400 font-bold">Thời gian giao dịch còn lại</span>
              </div>
              <BookingCountdown
                expiresAt={booking.expiresAt}
                onExpire={handleDeadlineExpired}
                className="text-sm font-black tracking-widest"
              />
            </div>

            {isExpired ? (
              <div
                role="status"
                className="rounded-3xl border border-amber-500/30 bg-amber-500/5 p-8 text-center"
              >
                <div className="mx-auto mb-5 flex h-14 w-14 items-center justify-center rounded-full bg-amber-500/10 text-amber-400">
                  <AlertTriangle className="h-7 w-7" />
                </div>
                <h2 className="text-xl font-black text-white">
                  {bookingStatus === 'CANCELLED'
                    ? 'Đơn đã được hủy'
                    : bookingStatus === 'EXPIRED' || deadlineExpired
                      ? 'Đã hết thời gian giữ ghế'
                      : ['CONFIRMED', 'COMPLETED'].includes(bookingStatus)
                        ? 'Đơn đã thanh toán thành công'
                        : 'Đơn không còn thanh toán được'}
                </h2>
                <p className="mx-auto mt-3 max-w-lg text-sm leading-6 text-zinc-400">
                  {bookingStatus === 'CANCELLED'
                    ? 'Ghế của đơn đã được trả lại. VNPay và MoMo đã được khóa để tránh tạo giao dịch cho đơn đã hủy.'
                    : bookingStatus === 'EXPIRED' || deadlineExpired
                      ? 'Thời hạn thanh toán đã kết thúc và ghế không còn được giữ.'
                      : ['CONFIRMED', 'COMPLETED'].includes(bookingStatus)
                        ? 'Bạn có thể mở chi tiết đơn để xem thông tin vé đã phát hành.'
                        : 'Vui lòng mở chi tiết đơn để kiểm tra trạng thái mới nhất.'}
                </p>
                <div className="mt-6 flex flex-wrap justify-center gap-3">
                  <button
                    type="button"
                    onClick={() => navigate(`/bookings/${bookingId}`)}
                    className="rounded-xl bg-brand-orange px-5 py-3 text-xs font-black uppercase text-white transition-colors hover:bg-orange-600"
                  >
                    Xem chi tiết đơn
                  </button>
                  <button
                    type="button"
                    onClick={() => navigate('/movies')}
                    className="rounded-xl border border-zinc-700 px-5 py-3 text-xs font-black uppercase text-zinc-200 transition-colors hover:bg-zinc-800"
                  >
                    Chọn suất chiếu khác
                  </button>
                </div>
              </div>
            ) : !isPaymentPhase ? (
              /* Step 3: Choose Food & Beverage */
              <div className="space-y-5 rounded-3xl border border-zinc-800/80 bg-zinc-900/40 p-5 md:p-6">
                <div className="flex flex-col justify-between gap-4 border-b border-zinc-800 pb-5 sm:flex-row sm:items-center">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="text-lg font-black text-white">Thêm bắp nước?</h2>
                      <span className="rounded-full border border-zinc-700 bg-zinc-800 px-2.5 py-1 text-[9px] font-black uppercase tracking-wider text-zinc-400">
                        Không bắt buộc
                      </span>
                    </div>
                    <p className="mt-1 text-xs text-zinc-500">Chọn nhanh món yêu thích hoặc bỏ qua để thanh toán vé.</p>
                  </div>
                  <div className="flex shrink-0 items-center gap-2">
                    <button
                      type="button"
                      onClick={() => setCatalogExpanded(value => !value)}
                      className="rounded-xl border border-zinc-700 px-3 py-2 text-xs font-bold text-zinc-300 transition-colors hover:border-zinc-600 hover:bg-zinc-800"
                    >
                      {catalogExpanded ? 'Thu gọn' : 'Xem tất cả'}
                    </button>
                    <button
                      type="button"
                      onClick={() => setPhase(CHECKOUT_PHASE.PAYMENT)}
                      className="rounded-xl bg-zinc-800 px-3 py-2 text-xs font-black text-white transition-colors hover:bg-zinc-700"
                    >
                      Bỏ qua
                    </button>
                  </div>
                </div>

                {catalogExpanded && (
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex gap-2 overflow-x-auto py-1 scrollbar-none">
                      {categories.map(cat => (
                        <button
                          key={cat}
                          onClick={() => {
                            setSelectedCategory(cat);
                            setCatalogPage(1);
                          }}
                          className={`shrink-0 rounded-xl px-3 py-2 text-xs font-bold transition-all ${
                            selectedCategory === cat
                              ? 'bg-brand-orange text-white'
                              : 'bg-zinc-800 text-zinc-400 hover:text-zinc-200'
                          }`}
                        >
                          {CATEGORY_LABELS[cat] || cat}
                        </button>
                      ))}
                    </div>
                    <div className="relative w-full sm:max-w-xs">
                      <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500" />
                      <input
                        type="search"
                        aria-label="Tìm bắp nước"
                        placeholder="Tìm món..."
                        value={searchQuery}
                        onChange={(e) => {
                          setSearchQuery(e.target.value);
                          setCatalogPage(1);
                        }}
                        className="w-full rounded-xl border border-zinc-800 bg-zinc-900 py-2.5 pl-11 pr-4 text-xs text-zinc-200 placeholder:text-zinc-600 focus:border-brand-orange focus:outline-none"
                      />
                    </div>
                  </div>
                )}

                {/* Grid of concession items */}
                {filteredConcessions.length > 0 ? (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                    {visibleConcessions.map(item => {
                      const { quantity = 0 } = cartItemsByProductId.get(item.id) || {};
                      const isUpdating = cartUpdatingId === item.id;

                      return (
                        <ConcessionProductCard
                          key={item.id}
                          item={item}
                          quantity={quantity}
                          isUpdating={isUpdating}
                          disabled={isExpired}
                          onQuantityChange={handleQuantityChange}
                        />
                      );
                    })}
                  </div>
                ) : (
                  <div className="text-center py-10 bg-zinc-900/20 border border-zinc-800 rounded-2xl border-dashed">
                    <span className="text-xs text-zinc-500 italic block">Không tìm thấy bắp nước phù hợp...</span>
                  </div>
                )}
                {catalogExpanded && catalogTotalPages > 1 && (
                  <nav
                    className="flex items-center justify-center gap-3 border-t border-zinc-800 pt-5"
                    aria-label="Phân trang bắp nước"
                  >
                    <button
                      type="button"
                      disabled={currentCatalogPage === 1}
                      onClick={() => setCatalogPage(value => Math.max(1, value - 1))}
                      className="rounded-xl border border-zinc-700 px-4 py-2 text-xs font-bold text-zinc-300 transition-colors hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      Trang trước
                    </button>
                    <span className="text-xs font-bold text-zinc-400">
                      {currentCatalogPage}/{catalogTotalPages}
                    </span>
                    <button
                      type="button"
                      disabled={currentCatalogPage === catalogTotalPages}
                      onClick={() => setCatalogPage(value => Math.min(catalogTotalPages, value + 1))}
                      className="rounded-xl border border-zinc-700 px-4 py-2 text-xs font-bold text-zinc-300 transition-colors hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      Trang sau
                    </button>
                  </nav>
                )}
              </div>
            ) : (
              /* Step 4: Payment Service handoff */
              <div className="space-y-5">
                <div className="space-y-5 rounded-3xl border border-zinc-800/80 bg-zinc-900/40 p-5 md:p-6">
                  <div>
                    <h2 className="text-lg font-black text-white">Chọn phương thức thanh toán</h2>
                    <p className="mt-1 text-xs text-zinc-500">Bạn sẽ được chuyển sang cổng thanh toán bảo mật.</p>
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
                        <img
                          src={MOMO_LOGO_URL}
                          alt="Logo MoMo"
                          width="48"
                          height="36"
                          decoding="async"
                          onError={event => {
                            if (event.currentTarget.dataset.fallbackApplied) return;
                            event.currentTarget.dataset.fallbackApplied = 'true';
                            event.currentTarget.src = MOMO_LOGO_FALLBACK_URL;
                          }}
                          className="max-h-full max-w-full object-contain"
                        />
                      </div>
                      <div>
                        <h4 className="text-xs font-black text-zinc-200">MoMo</h4>
                        <p className="text-[9px] text-zinc-650 mt-0.5">Thanh toán nhanh qua ví điện tử</p>
                      </div>
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Right panel: Sticky order info sidebar / Food summary */}
          <aside className="sticky top-20 space-y-3 rounded-3xl border border-zinc-800 bg-zinc-900 p-4 shadow-2xl lg:col-span-1 lg:max-h-[calc(100vh-5.5rem)] lg:overflow-y-auto">
            {/* Desktop Countdown Timer */}
            <div className="hidden items-center justify-between gap-4 rounded-2xl border border-zinc-800 bg-zinc-950/60 px-4 py-2.5 shadow-inner lg:flex">
              <div className="flex items-center gap-2">
                <Clock className="w-4 h-4 text-amber-500" />
                <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Thời gian còn lại</span>
              </div>
              <BookingCountdown
                expiresAt={booking.expiresAt}
                onExpire={handleDeadlineExpired}
                className="text-base font-black tracking-widest"
              />
            </div>

            {/* Movie Poster & Meta details */}
            <div className="flex items-center gap-3 border-b border-zinc-800 pb-3">
              <div className="flex aspect-[2/3] w-14 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-zinc-800 bg-zinc-950">
                <img
                  src={displayMoviePosterUrl}
                  alt={`Áp phích phim ${movieTitle}`}
                  width="56"
                  height="84"
                  decoding="async"
                  fetchPriority="high"
                  className="w-full h-full object-cover"
                  onError={(event) => {
                    if (event.currentTarget.src.startsWith('data:')) return;
                    event.currentTarget.onerror = null;
                    event.currentTarget.src = FALLBACK_POSTER;
                  }}
                />
              </div>
              <div className="min-w-0 flex-grow space-y-1">
                <span className="text-[9px] font-black uppercase tracking-widest text-brand-orange">Thông tin suất chiếu</span>
                <h3 className="line-clamp-2 text-sm font-black leading-snug text-white">{movieTitle}</h3>
              </div>
            </div>

            {/* Booking Details */}
            <div className="space-y-2 border-b border-zinc-800 py-2 text-[11px]">
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Cụm rạp</span>
                <span className="text-white font-bold text-right">{snapshot?.cinemaName || cinema?.name || 'Chưa có thông tin rạp'}</span>
              </div>
              <div className="flex justify-between gap-3">
                <span className="shrink-0 font-medium text-zinc-500">Phòng chiếu</span>
                <span className="text-right font-bold text-zinc-200">{snapshot?.auditoriumName || auditorium?.name || 'Chưa có thông tin phòng'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Suất chiếu</span>
                <span className="text-white font-bold text-right">
                  {showtimeStart
                    ? `${new Date(showtimeStart).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false })} · ${new Date(showtimeStart).toLocaleDateString('vi-VN')}`
                    : 'Chưa có thông tin'}
                </span>
              </div>
            </div>

            {/* Selected Seats */}
            <div className="space-y-2 border-b border-zinc-800 py-2">
              <span className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Các vị trí ghế</span>
              {visibleSeats.length > 0 ? (
                <div className="flex flex-wrap gap-1.5">
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
            <div className="space-y-2 border-b border-zinc-800 py-2">
              <span className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Bắp nước đã chọn</span>
              {selectedFoodItems.length > 0 ? (
                <div className="max-h-24 space-y-1.5 overflow-y-auto pr-1">
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
            <div className="space-y-2.5 border-b border-zinc-800 py-2 text-xs">
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
              <div className="flex justify-between pt-1 text-[10px] text-zinc-500">
                <span>Thuế GTGT (VAT) đã bao gồm:</span>
                <span>10%</span>
              </div>
            </div>

            {/* Score redemption is available before Payment handoff on both checkout steps. */}
            <div className="rounded-2xl border border-brand-orange/25 bg-brand-orange/[0.06] p-4 space-y-3">
              <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-2 min-w-0">
                  <ShieldCheck className="h-4 w-4 shrink-0 text-brand-orange" />
                  <span className="text-[10px] font-black uppercase tracking-wider text-zinc-200">
                    Dùng điểm thành viên
                  </span>
                </div>
                <span className="text-[10px] font-black text-brand-orange whitespace-nowrap">
                  {availableScorePoints.toLocaleString('vi-VN')} điểm
                </span>
              </div>

              {userScore ? (
                <>
                  <p className="text-[10px] leading-relaxed text-zinc-500">
                    1 điểm = 1.000đ. Điểm được giữ khi chốt đơn và chỉ bị trừ sau khi thanh toán thành công.
                  </p>
                  {scorePreview?.eligible ? (
                    <div className="space-y-2">
                      <div className="flex justify-between rounded-xl border border-emerald-500/20 bg-emerald-500/10 p-3 text-[11px] font-bold text-emerald-400">
                        <span>Đã chọn {Number(scorePreview.requestedPoints).toLocaleString('vi-VN')} điểm</span>
                        <span>-{formatCurrency(selectedScoreDiscount)}</span>
                      </div>
                      {scorePreview.locked ? (
                        <p className="text-center text-[9px] font-bold uppercase tracking-wider text-zinc-500">
                          Điểm đã được khóa theo đơn
                        </p>
                      ) : (
                        <button
                          type="button"
                          onClick={clearScoreSelection}
                          className="w-full rounded-xl border border-zinc-700 py-2 text-[10px] font-black uppercase tracking-wider text-zinc-400 transition-colors hover:border-zinc-500 hover:text-white"
                        >
                          Bỏ dùng điểm
                        </button>
                      )}
                    </div>
                  ) : (
                    <div className="space-y-2">
                      <div className="flex gap-2">
                        <input
                          type="number"
                          min="1"
                          max={maxScorePoints || undefined}
                          step="1"
                          value={scorePointsInput}
                          onChange={(event) => {
                            setScorePointsInput(event.target.value);
                            setScorePreviewError('');
                          }}
                          placeholder={maxScorePoints > 0 ? `Tối đa ${maxScorePoints}` : 'Không đủ điểm'}
                          disabled={maxScorePoints <= 0 || isExpired}
                          aria-label="Số điểm muốn dùng"
                          className="min-w-0 flex-1 rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2.5 text-xs font-bold text-white outline-none transition-colors placeholder:text-zinc-700 focus:border-brand-orange disabled:cursor-not-allowed disabled:opacity-50"
                        />
                        <button
                          type="button"
                          onClick={handlePreviewScore}
                          disabled={scorePreviewLoading || maxScorePoints <= 0 || isExpired}
                          className="shrink-0 rounded-xl bg-brand-orange px-3 py-2.5 text-[10px] font-black uppercase tracking-wider text-white transition-opacity disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          {scorePreviewLoading ? 'Đang kiểm tra...' : 'Dùng điểm'}
                        </button>
                      </div>
                      {maxScorePoints > 0 && (
                        <button
                          type="button"
                          onClick={() => setScorePointsInput(String(maxScorePoints))}
                          className="text-[9px] font-bold uppercase tracking-wider text-brand-orange hover:underline"
                        >
                          Chọn số điểm tối đa
                        </button>
                      )}
                      {scorePreviewError && (
                        <p role="alert" className="text-[10px] leading-relaxed text-red-400">
                          {scorePreviewError}
                        </p>
                      )}
                    </div>
                  )}
                </>
              ) : (
                <p className="text-[10px] leading-relaxed text-zinc-500">
                  Điểm thành viên đang tạm thời không khả dụng. Bạn vẫn có thể thanh toán bình thường.
                </p>
              )}
            </div>

            {/* Grand Total box */}
            <div className="flex items-center justify-between rounded-2xl border border-brand-orange/30 bg-zinc-950/80 px-4 py-3 shadow-[0_0_15px_rgba(255,122,0,0.1)]">
              <div>
                <span className="text-[10px] text-zinc-400 font-black uppercase tracking-wider block mb-0.5">Tổng số tiền</span>
                <span className="text-[9px] text-brand-orange/80 font-bold uppercase">Đã bao gồm VAT</span>
              </div>
              <span className="text-2xl font-black tracking-tight text-brand-orange">
                {formatCurrency(displayFinalAmount)}
              </span>
            </div>

            {isPaymentPhase && (
              <label className="flex cursor-pointer select-none items-start gap-3 rounded-xl border border-zinc-800 bg-zinc-950/40 p-3">
                <input
                  type="checkbox"
                  checked={termsAgreed}
                  onChange={(e) => setTermsAgreed(e.target.checked)}
                  className="mt-0.5 h-4 w-4 shrink-0 accent-brand-orange"
                />
                <span className="text-[11px] font-medium leading-4 text-zinc-400">
                  Tôi đồng ý với <span className="font-bold text-zinc-200">Điều khoản và Quy định</span> giao dịch mua vé trực tuyến.
                </span>
              </label>
            )}

            <div className="sticky bottom-0 -mx-1 space-y-2 border-t border-zinc-800 bg-zinc-900/95 px-1 pt-3 backdrop-blur">
              {!isPaymentPhase ? (
                <button
                  disabled={isExpired}
                  onClick={() => setPhase(CHECKOUT_PHASE.PAYMENT)}
                  className={`w-full rounded-2xl py-3.5 text-xs font-black uppercase tracking-wider shadow-lg transition-all ${
                    !isExpired
                      ? 'cursor-pointer bg-brand-orange text-white shadow-brand-orange/25 hover:bg-orange-600'
                      : 'cursor-not-allowed border border-zinc-800 bg-zinc-850 text-zinc-500'
                  }`}
                >
                  Tiếp tục thanh toán · {formatCurrency(displayFinalAmount)}
                </button>
              ) : (
                <>
                  <button
                    onClick={handleStartPayment}
                    disabled={!termsAgreed || paymentLoading || isExpired}
                    className="flex w-full items-center justify-center gap-2 rounded-2xl bg-brand-orange py-3.5 text-xs font-black uppercase tracking-wider text-white shadow-lg shadow-brand-orange/20 transition-colors hover:bg-orange-600 disabled:cursor-not-allowed disabled:bg-zinc-800 disabled:text-zinc-500 disabled:shadow-none"
                  >
                    <CreditCard className="h-4 w-4" />
                    <span>
                      {paymentLoading
                        ? 'Đang tạo giao dịch...'
                        : `Thanh toán qua ${selectedPaymentMethod} · ${formatCurrency(displayFinalAmount)}`}
                    </span>
                  </button>
                  {!termsAgreed && (
                    <p className="text-center text-[10px] text-zinc-500">Đồng ý điều khoản để tiếp tục thanh toán.</p>
                  )}
                  <button
                    onClick={() => setPhase(CHECKOUT_PHASE.ADD_ONS)}
                    className="w-full rounded-xl py-2 text-center text-[10px] font-bold uppercase tracking-wider text-zinc-500 transition-colors hover:text-white"
                  >
                    Quay lại bắp nước
                  </button>
                </>
              )}

              <button
                disabled={paymentLoading || isExpired || cancelling}
                onClick={() => {
                  setCancelError('');
                  setCancelModalOpen(true);
                }}
                className="block w-full cursor-pointer py-2 text-center text-[10px] font-semibold uppercase tracking-wider text-zinc-600 transition-colors hover:text-red-400 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Hủy giao dịch
              </button>
            </div>

            <div className="flex items-center justify-center gap-2 text-[9px] font-bold uppercase text-zinc-650">
              <ShieldCheck className="h-4 w-4 text-zinc-600" />
              <span>Thanh toán an toàn bảo mật</span>
            </div>
          </aside>
        </div>
      </div>
    </div>
  );
}
