import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, useOutletContext } from 'react-router-dom';
import {
  ArrowLeft, Sliders, History, AlertTriangle, ShoppingCart,
  User, CreditCard, Film, Armchair, Activity, FileCheck, HelpCircle
} from 'lucide-react';
import { getBookingDetail, updateBookingStatus, getBookingFoods } from '../services/adminBookingService';
import { getUserProfile } from '@/features/auth/services/userService';
import { useAuth } from '@/contexts/AuthContext';
import { LazyImage } from '@/components/common/ui/uiKit';
import { getBookingErrorMessage } from '../../customer/utils/bookingErrorMessages';

export default function AdminBookingDetailPage() {
  const { bookingId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { triggerToast, triggerConfirm, triggerAlert } = useOutletContext() || {};

  const isAdmin = user?.role === 'ADMIN';

  // Data States
  const [booking, setBooking] = useState(null);
  const [customer, setCustomer] = useState(null);
  const [foodOrder, setFoodOrder] = useState(null);

  // Loading/Error States
  const [loading, setLoading] = useState(true);
  const [customerLoading, setCustomerLoading] = useState(false);
  const [foodLoading, setFoodLoading] = useState(false);
  const [error, setError] = useState(null);
  const [updating, setUpdating] = useState(false);

  // Status transition controls
  const [targetStatus, setTargetStatus] = useState('');
  const [changeReason, setChangeReason] = useState('');

  // Fetch Booking details
  const fetchDetails = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getBookingDetail(bookingId);
      setBooking(data);
      setTargetStatus('');
      
      // Fetch Customer profile
      if (data?.userId) {
        setCustomerLoading(true);
        try {
          const profile = await getUserProfile(data.userId);
          setCustomer(profile);
        } catch (cErr) {
          console.warn("Could not load user profile:", cErr);
        } finally {
          setCustomerLoading(false);
        }
      }

      // Fetch Food Order detail
      setFoodLoading(true);
      try {
        const foods = await getBookingFoods(bookingId);
        setFoodOrder(foods);
      } catch (fErr) {
        console.warn("Could not load food details for booking:", fErr);
      } finally {
        setFoodLoading(false);
      }

    } catch (err) {
      setError(getBookingErrorMessage(err, "Không thể tải chi tiết đơn hàng."));
    } finally {
      setLoading(false);
    }
  }, [bookingId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchDetails();
  }, [fetchDetails]);

  const allowedAdminActions = (() => {
    if (booking?.bookingStatus === 'PENDING_PAYMENT') {
      return [{ status: 'CANCELLED', label: 'Hủy đơn và trả ghế' }];
    }
    if (booking?.bookingStatus === 'CONFIRMED') {
      return [{ status: 'COMPLETED', label: 'Đánh dấu đã hoàn thành' }];
    }
    return [];
  })();

  const notify = (message, type = 'info') => {
    if (triggerToast) {
      triggerToast(message, type);
    } else if (triggerAlert) {
      triggerAlert(message);
    }
  };

  // Admin executes explicit lifecycle commands; Payment owns confirmation and refund.
  const handleStatusUpdate = async (e) => {
    e.preventDefault();
    if (!targetStatus) return;

    const confirmMessage = `Xác nhận thực hiện “${translateStatus(targetStatus)}” cho đơn ${booking.bookingCode}?`;
    const confirmed = triggerConfirm
      ? await triggerConfirm(confirmMessage)
      : false;
    if (!confirmed) {
      return;
    }

    setUpdating(true);
    try {
      await updateBookingStatus(
        bookingId,
        targetStatus,
        changeReason || 'Quản trị viên thực hiện lệnh vòng đời'
      );
      setChangeReason('');
      setTargetStatus('');
      notify("Cập nhật trạng thái đơn hàng thành công.", 'success');
      
      // Reload details
      fetchDetails();
    } catch (err) {
      const msg = getBookingErrorMessage(err, "Không thể cập nhật trạng thái đơn hàng.");
      notify(msg, 'error');
    } finally {
      setUpdating(false);
    }
  };

  const translateStatus = (bStatus) => {
    switch (bStatus) {
      case 'CONFIRMED': return 'Đã xác nhận';
      case 'PENDING_PAYMENT': return 'Chờ thanh toán';
      case 'COMPLETED': return 'Đã hoàn thành';
      case 'CANCELLED': return 'Đã hủy';
      case 'EXPIRED': return 'Hết hạn';
      case 'REFUNDED': return 'Hoàn tiền';
      default: return bStatus || 'PENDING_PAYMENT';
    }
  };

  const getStatusColor = (bStatus) => {
    switch (bStatus) {
      case 'CONFIRMED': return 'text-emerald-400 border-emerald-400 bg-emerald-500/10';
      case 'COMPLETED': return 'text-blue-400 border-blue-400 bg-blue-500/10';
      case 'PENDING_PAYMENT': return 'text-amber-400 border-amber-400 bg-amber-500/10';
      case 'CANCELLED': return 'text-red-400 border-red-400 bg-red-500/10';
      case 'EXPIRED': return 'text-zinc-500 border-zinc-700 bg-zinc-800/40';
      case 'REFUNDED': return 'text-purple-400 border-purple-400 bg-purple-500/10';
      default: return 'text-zinc-400 border-zinc-800 bg-zinc-900';
    }
  };

  const translatePaymentStatus = (paymentStatus) => {
    switch (paymentStatus) {
      case 'SUCCESS': return 'Thành công';
      case 'FAILED': return 'Thất bại';
      case 'REFUNDED': return 'Đã hoàn tiền';
      case 'PENDING': return 'Đang chờ';
      default: return 'Chưa ghi nhận';
    }
  };

  const translateTicketStatus = (ticketStatus) => {
    switch (ticketStatus) {
      case 'ACTIVE': return 'Có hiệu lực';
      case 'USED': return 'Đã sử dụng';
      case 'CANCELLED': return 'Đã hủy';
      case 'REFUNDED': return 'Đã hoàn tiền';
      default: return ticketStatus || 'Chưa ghi nhận';
    }
  };

  const formatCurrency = (val) => {
    return (val || 0).toLocaleString('vi-VN') + 'đ';
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-zinc-950 text-white">
        <div className="flex flex-col items-center gap-4 animate-pulse">
          <div className="w-12 h-12 border-4 border-[#ff7a1a] border-t-transparent rounded-full animate-spin"></div>
          <p className="text-sm font-semibold tracking-wider text-zinc-400">Đang tải hồ sơ & chi tiết giao dịch...</p>
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
        <h2 className="text-xl font-bold text-zinc-100 mb-2">Đã có lỗi xảy ra</h2>
        <p className="text-sm text-zinc-400 max-w-md mb-8">{error || "Hóa đơn này không tồn tại hoặc đã bị xóa."}</p>
        <button
          onClick={() => navigate('/admin/bookings')}
          className="bg-[#ff7a1a] hover:bg-opacity-90 text-zinc-950 font-black px-6 py-3 rounded-xl transition-all text-xs uppercase tracking-wider"
        >
          Quay lại danh sách đơn
        </button>
      </div>
    );
  }

  const { snapshot, tickets = [], statusHistories = [] } = booking;

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-screen bg-zinc-950 text-zinc-100 space-y-6 selection:bg-[#ff7a1a] selection:text-zinc-950">

      {/* Back Link */}
      <div>
        <button
          onClick={() => navigate('/admin/bookings')}
          className="flex items-center gap-2 text-zinc-400 hover:text-[#ff7a1a] transition-colors text-xs font-bold bg-transparent border-none p-0"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại danh sách đơn hàng</span>
        </button>
      </div>

      {/* Header Info */}
      <div className="bg-zinc-900 border border-zinc-850 rounded-3xl p-6 md:p-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-6 shadow-2xl">
        <div className="space-y-1.5">
          <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider block">Hóa đơn đặt vé & bắp nước</span>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-xl md:text-2xl font-black tracking-widest text-white uppercase">{booking.bookingCode}</h1>
            <span className={`text-[10px] border px-2.5 py-0.5 rounded-full font-black uppercase tracking-wider ${getStatusColor(booking.bookingStatus)}`}>
              {translateStatus(booking.bookingStatus)}
            </span>
          </div>
          <p className="text-[10px] text-zinc-500 font-semibold">
            Ngày lập đơn: {new Date(booking.createdAt).toLocaleString('vi-VN')}
          </p>
        </div>
        <div className="flex flex-col text-right items-start md:items-end text-xs text-zinc-400 gap-1 bg-zinc-950/60 p-4 rounded-2xl border border-zinc-850">
          <span className="text-[10px] text-zinc-550 font-bold uppercase block">Tổng số tiền thanh toán</span>
          <span className="text-xl font-black text-[#ff7a1a]">{formatCurrency(booking.finalAmount)}</span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">

        {/* Left Side: Order info & Audit History */}
        <div className="lg:col-span-2 space-y-8">

          {/* Customer & Payment Information */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            
            {/* Customer Box */}
            <div className="bg-zinc-900 border border-zinc-850 rounded-3xl p-6 space-y-4">
              <div className="border-b border-zinc-800 pb-3 flex items-center gap-2">
                <User className="w-4 h-4 text-[#ff7a1a]" />
                <span className="text-xs font-black uppercase tracking-wider text-white">Khách Hàng</span>
              </div>
              {customerLoading ? (
                <div className="h-20 animate-pulse bg-zinc-950/50 rounded-xl" />
              ) : customer ? (
                <div className="space-y-2 text-xs">
                  <div className="flex justify-between">
                    <span className="text-zinc-500">Họ và tên:</span>
                    <span className="font-bold text-zinc-100">{customer.fullName}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-zinc-500">Email:</span>
                    <span className="font-mono text-zinc-300">{customer.email}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-zinc-500">Số điện thoại:</span>
                    <span className="font-mono text-zinc-300">{customer.phoneNumber || 'Không có số'}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-zinc-500">Mã Account ID:</span>
                    <span className="font-bold text-zinc-400">#{booking.userId}</span>
                  </div>
                </div>
              ) : (
                <div className="space-y-2 text-xs">
                  <p className="text-zinc-400">Không tìm thấy thông tin khách hàng chi tiết.</p>
                  <p className="text-zinc-550">Tài khoản Account ID: <span className="font-bold">#{booking.userId}</span></p>
                </div>
              )}
            </div>

            {/* Payment Snapshot */}
            <div className="bg-zinc-900 border border-zinc-850 rounded-3xl p-6 space-y-4">
              <div className="border-b border-zinc-800 pb-3 flex items-center gap-2">
                <CreditCard className="w-4 h-4 text-[#ff7a1a]" />
                <span className="text-xs font-black uppercase tracking-wider text-white">Phương Thức Thanh Toán</span>
              </div>
              <div className="space-y-2 text-xs">
                <div className="flex justify-between">
                  <span className="text-zinc-500">Cổng thanh toán:</span>
                  <span className="font-bold text-zinc-100">{booking.paymentMethodSnapshot || 'Chưa ghi nhận'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-500">Nhà cung cấp:</span>
                  <span className="font-mono text-zinc-300">{booking.paymentProvider || 'Chưa ghi nhận'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-500">Trạng thái GD:</span>
                  <span className={`font-bold uppercase ${
                    booking.paymentStatus === 'SUCCESS' ? 'text-emerald-400' : 'text-amber-400'
                  }`}>{translatePaymentStatus(booking.paymentStatus)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-500">Tham chiếu GD:</span>
                  <span className="font-mono text-zinc-400 text-[10px] select-all truncate max-w-[150px]">{booking.paymentReference || 'Chưa ghi nhận'}</span>
                </div>
              </div>
            </div>
          </div>

          {/* Movie snapshot details card */}
          <div className="bg-zinc-900 border border-zinc-850 rounded-3xl p-6 md:p-8 space-y-6">
            <div className="border-b border-zinc-800 pb-3 flex items-center gap-2">
              <Film className="w-4 h-4 text-[#ff7a1a]" />
              <span className="text-xs font-black uppercase tracking-wider text-white">Thông tin Suất chiếu & Ghế đặt (Snapshot)</span>
            </div>

            <div className="flex flex-col md:flex-row gap-6">
              {/* Poster image */}
              <div className="w-24 h-36 rounded-2xl overflow-hidden shrink-0 border border-zinc-800 shadow-xl bg-zinc-950">
                <LazyImage
                  src={snapshot?.moviePoster}
                  alt={snapshot?.movieTitle || 'Áp phích phim chưa có dữ liệu'}
                  className="w-full h-full object-cover"
                />
              </div>

              {/* Show details */}
              <div className="flex-1 grid grid-cols-1 sm:grid-cols-2 gap-6 text-xs text-zinc-400">
                <div>
                  <span className="text-zinc-500 font-bold block text-[10px] uppercase">Phim Chiếu</span>
                  <span className="text-white font-extrabold text-sm block mt-1">{snapshot?.movieTitle || 'Chưa có dữ liệu phim'}</span>
                  {snapshot?.originalTitle && (
                    <span className="text-[10px] text-zinc-550 mt-1 block">Tên gốc: {snapshot.originalTitle}</span>
                  )}
                  {(snapshot?.duration || snapshot?.ageRating) && (
                    <span className="text-[10px] text-zinc-550 mt-0.5 block">
                      {snapshot?.duration ? `Thời lượng: ${snapshot.duration} phút` : ''}
                      {snapshot?.duration && snapshot?.ageRating ? ' | ' : ''}
                      {snapshot?.ageRating && (
                        <>Phân loại: <span className="text-amber-400 font-extrabold">{snapshot.ageRating}</span></>
                      )}
                    </span>
                  )}
                </div>
                <div>
                  <span className="text-zinc-500 font-bold block text-[10px] uppercase">Lịch Suất Chiếu</span>
                  <span className="text-white font-extrabold text-sm block mt-1">
                    {snapshot?.showtimeStart ? new Date(snapshot.showtimeStart).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : ''} | {snapshot?.showtimeStart ? new Date(snapshot.showtimeStart).toLocaleDateString('vi-VN') : ''}
                  </span>
                  <span className="text-[10px] text-zinc-550 mt-1 block">Rạp chiếu: <span className="text-zinc-300 font-bold">{snapshot?.cinemaName || 'Chưa có dữ liệu'}</span></span>
                  <span className="text-[10px] text-zinc-550 mt-0.5 block">Phòng chiếu: <span className="text-zinc-300 font-bold">{snapshot?.auditoriumName || 'Chưa có dữ liệu'}</span></span>
                </div>
              </div>
            </div>

            {/* List of seats and generated tickets code */}
            <div className="space-y-3 pt-2">
              <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Danh sách vé & Ghế phát hành</span>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {tickets.map(t => (
                  <div key={t.id} className="bg-zinc-950 border border-zinc-850 rounded-2xl p-4 flex justify-between items-center text-xs">
                    <div>
                      <div className="flex items-center gap-1.5">
                        <Armchair className="w-3.5 h-3.5 text-[#ff7a1a]" />
                        <span className="text-zinc-100 font-extrabold block">Ghế {t.seatLabel} ({t.seatType})</span>
                      </div>
                      <span className="text-[9px] text-zinc-550 font-bold mt-1 block font-mono">MÃ VÉ: {t.ticketCode}</span>
                    </div>
                    <span className={`text-[9px] font-black px-2 py-0.5 rounded ${
                      t.status === 'ACTIVE' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-zinc-800 text-zinc-500'
                    }`}>
                      {translateTicketStatus(t.status)}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Concessions / Food lists details */}
          <div className="bg-zinc-900 border border-zinc-850 rounded-3xl p-6 md:p-8 space-y-6">
            <div className="border-b border-zinc-800 pb-3 flex justify-between items-center">
              <div className="flex items-center gap-2">
                <ShoppingCart className="w-4 h-4 text-[#ff7a1a]" />
                <span className="text-xs font-black uppercase tracking-wider text-white">Danh sách bắp nước phục vụ (Concessions)</span>
              </div>
              <span className="text-[9px] text-zinc-500">Đơn hàng: {foodOrder ? `#${foodOrder.publicId.slice(0, 8)}` : 'Không có'}</span>
            </div>

            {foodLoading ? (
              <div className="space-y-3 animate-pulse">
                <div className="h-6 bg-zinc-950/50 rounded w-full" />
                <div className="h-6 bg-zinc-950/50 rounded w-3/4" />
              </div>
            ) : foodOrder && foodOrder.items && foodOrder.items.length > 0 ? (
              <div className="space-y-4">
                <div className="divide-y divide-zinc-800">
                  {foodOrder.items.map(item => (
                    <div key={item.id} className="py-4 flex justify-between items-center text-xs first:pt-0 last:pb-0">
                      <div className="space-y-1">
                        <h4 className="font-extrabold text-white">{item.productName}</h4>
                        <p className="text-[9px] text-zinc-500 font-bold">Mã sản phẩm: <span className="font-mono">{item.productCode}</span> | Loại: {item.productType}</p>
                      </div>
                      <div className="flex items-center gap-6">
                        <span className="text-zinc-500 font-bold">x{item.quantity}</span>
                        <div className="text-right min-w-[90px]">
                          <span className="text-white font-black block">{formatCurrency(item.finalAmount)}</span>
                          {item.discountAmount > 0 && (
                            <span className="text-[8.5px] text-emerald-500 font-bold block">Đã giảm -{formatCurrency(item.discountAmount)}</span>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
                
                <div className="bg-zinc-950/50 border border-zinc-850 p-4 rounded-2xl flex items-center justify-between text-xs mt-2">
                  <div className="flex items-center gap-2.5">
                    <Activity className="w-4 h-4 text-zinc-500" />
                    <span className="text-zinc-400 font-bold">Trạng thái đơn bắp nước do Booking Service ghi nhận</span>
                  </div>
                  <span className="text-[9px] bg-zinc-800 text-zinc-300 font-black px-2 py-0.5 rounded border border-zinc-700 uppercase tracking-wider">
                    {foodOrder.status || 'Chưa ghi nhận'}
                  </span>
                </div>
              </div>
            ) : (
              <div className="text-center py-6 text-xs text-zinc-500 italic">
                Khách hàng không mua kèm bắp nước cho hóa đơn này
              </div>
            )}
          </div>

          {/* Transition Audit log */}
          <div className="bg-zinc-900 border border-zinc-850 rounded-3xl p-6 md:p-8 space-y-6">
            <div className="border-b border-zinc-800 pb-3 flex items-center gap-2">
              <History className="w-4 h-4 text-[#ff7a1a]" />
              <span className="text-xs font-black uppercase tracking-wider text-white">Lịch sử Chuyển đổi trạng thái (Audit Log)</span>
            </div>

            {statusHistories.length > 0 ? (
              <div className="relative border-l border-zinc-800 ml-4 pl-6 space-y-6 py-2">
                {statusHistories.map((log, index) => (
                  <div key={log.id || index} className="relative space-y-1.5 text-xs">
                    {/* Circle marker */}
                    <div className="absolute -left-[31px] top-0.5 w-2.5 h-2.5 rounded-full bg-[#ff7a1a] border-2 border-zinc-900 shadow"></div>

                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-zinc-500 text-[10px] font-bold">
                        {new Date(log.createdAt).toLocaleString('vi-VN')}
                      </span>
                      <span className="text-zinc-650">•</span>
                      <span className="text-zinc-400 font-bold">
                        {log.fromStatus ? translateStatus(log.fromStatus) : 'Mới tạo'} → <span className="text-white font-extrabold">{translateStatus(log.toStatus)}</span>
                      </span>
                    </div>

                    <div className="text-[10px] text-zinc-500 font-semibold space-y-0.5">
                      <div>Lý do ghi nhận: <span className="text-zinc-400 font-bold italic">"{log.reason || 'Không ghi nhận lý do'}"</span></div>
                      <div>Tác nhân: <span className="text-zinc-400">{log.changedBy || 'Hệ thống'}</span> (Kênh: <span className="text-zinc-550">{log.source || 'CRON'}</span>)</div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-6 text-xs text-zinc-500 italic">
                Không ghi nhận lịch sử thay đổi trạng thái nào cho hóa đơn này
              </div>
            )}
          </div>
        </div>

        {/* Right Side Sticky Sidebar: Pricing & Status Update panel */}
        <div className="lg:col-span-1 space-y-8">
          <div className="sticky top-24 space-y-8">

            {/* Price Summary card */}
            <div className="bg-zinc-900 border border-zinc-850 rounded-3xl p-6 space-y-4 shadow-2xl">
              <div className="flex items-center justify-between border-b border-zinc-800 pb-3">
                <h3 className="text-xs font-black uppercase tracking-wider text-white">Chi tiết Doanh Thu</h3>
                <FileCheck className="w-4 h-4 text-emerald-400" />
              </div>

              <div className="space-y-3 text-xs">
                <div className="flex justify-between text-zinc-400">
                  <span>Tiền vé xem phim:</span>
                  <span className="font-semibold text-zinc-200">{formatCurrency(booking.ticketAmount)}</span>
                </div>
                <div className="flex justify-between text-zinc-400">
                  <span>Tiền bắp nước (Foods):</span>
                  <span className="font-semibold text-zinc-200">{formatCurrency(booking.foodAmount || 0)}</span>
                </div>
                <div className="flex justify-between text-zinc-450 border-t border-zinc-850/50 pt-2 text-[10px]">
                  <span>Thuế suất (VAT) & Phí dịch vụ:</span>
                  <span>+{formatCurrency(
                    (booking.taxAmount || 0) + (booking.serviceFee || 0)
                  )}</span>
                </div>
                
                {(booking.promotionDiscount > 0 || booking.voucherDiscount > 0) && (
                  <div className="flex justify-between text-emerald-500 font-bold">
                    <span>Khuyến mãi / Giảm giá:</span>
                    <span>-{formatCurrency(
                      (booking.promotionDiscount || 0) + (booking.voucherDiscount || 0)
                    )}</span>
                  </div>
                )}

                <div className="flex justify-between items-center py-4 px-4 bg-zinc-950/60 rounded-2xl border border-zinc-850 shadow-inner mt-2">
                  <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider">Tổng cộng thu</span>
                  <span className="text-lg font-black text-[#ff7a1a]">{formatCurrency(booking.finalAmount)}</span>
                </div>
              </div>
            </div>

            {/* Manual Transition Control Panel (Admin only) */}
            <div className="bg-zinc-900 border border-zinc-850 rounded-3xl p-6 space-y-4 shadow-2xl">
              <div className="flex items-center gap-2 border-b border-zinc-800 pb-3">
                <Sliders className="w-4 h-4 text-[#ff7a1a]" />
                <span className="text-xs font-black uppercase tracking-wider text-white">Chuyển đổi Trạng Thái</span>
              </div>

              {isAdmin ? (
                <form onSubmit={handleStatusUpdate} className="space-y-4 text-xs">
                  {allowedAdminActions.length > 0 ? (
                    <div className="space-y-1">
                      <label className="text-[10px] text-zinc-500 font-bold uppercase">Thao tác được phép</label>
                      <select
                        value={targetStatus}
                        onChange={(e) => setTargetStatus(e.target.value)}
                        className="w-full bg-[#050506] border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-[#ff7a1a]/40 text-zinc-200"
                      >
                        <option value="">Chọn thao tác</option>
                        {allowedAdminActions.map(action => (
                          <option key={action.status} value={action.status}>{action.label}</option>
                        ))}
                      </select>
                    </div>
                  ) : (
                    <div className="rounded-xl border border-zinc-800 bg-zinc-950/60 p-3 text-[10px] leading-relaxed text-zinc-500">
                      Trạng thái hiện tại không có thao tác thủ công. Xác nhận và hoàn tiền chỉ được áp dụng từ kết quả hợp lệ của Payment Service; hết hạn do Booking Service quản lý.
                    </div>
                  )}

                  {allowedAdminActions.length > 0 && (
                  <div className="space-y-1">
                    <label className="text-[10px] text-zinc-500 font-bold uppercase">Lý do điều chỉnh (Audit)</label>
                    <textarea
                      placeholder="Nhập lý do thay đổi trạng thái..."
                      value={changeReason}
                      onChange={(e) => setChangeReason(e.target.value)}
                      rows="3"
                      className="w-full bg-[#050506] border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-[#ff7a1a]/40 text-zinc-200 resize-none"
                      required
                    />
                  </div>
                  )}

                  <button
                    type="submit"
                    disabled={updating || !targetStatus}
                    className={`w-full py-3.5 rounded-xl font-black uppercase text-[10px] tracking-widest transition-colors ${
                      targetStatus && !updating
                        ? 'bg-[#ff7a1a] hover:bg-opacity-90 text-zinc-950 cursor-pointer'
                        : 'bg-zinc-850 text-zinc-600 border border-zinc-800 cursor-not-allowed'
                    }`}
                  >
                    {updating ? 'Đang thực hiện đổi...' : 'Áp dụng thay đổi trạng thái'}
                  </button>
                </form>
              ) : (
                <div className="flex flex-col items-center justify-center p-4 border border-zinc-850 rounded-2xl bg-zinc-950/40 text-center gap-2">
                  <HelpCircle className="w-8 h-8 text-zinc-650" />
                  <span className="text-[10px] text-zinc-550 font-bold uppercase tracking-wide">Yêu cầu quyền Administrator</span>
                  <p className="text-[10px] text-zinc-500">
                    Tài khoản nhân sự của bạn không có đủ quyền thay đổi trạng thái đơn đặt vé.
                  </p>
                </div>
              )}
            </div>

          </div>
        </div>

      </div>
    </div>
  );
}
