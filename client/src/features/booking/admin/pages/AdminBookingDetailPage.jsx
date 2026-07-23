import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Sliders, ShieldCheck, HelpCircle, History, Clock, FileText, CheckCircle, AlertTriangle } from 'lucide-react';
import { getBookingDetail, updateBookingStatus } from '../services/adminBookingService';

export default function AdminBookingDetailPage() {
  const { bookingId } = useParams();
  const navigate = useNavigate();

  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [updating, setUpdating] = useState(false);

  // Status select value
  const [targetStatus, setTargetStatus] = useState('');
  const [changeReason, setChangeReason] = useState('');

  const fetchDetails = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getBookingDetail(bookingId);
      setBooking(data);
      setTargetStatus(data.status);
    } catch (err) {
      setError(err.message || err.detail || "Không thể tải chi tiết đơn hàng.");
    } finally {
      setLoading(false);
    }
  }, [bookingId]);

  useEffect(() => {
    fetchDetails();
  }, [fetchDetails]);

  const handleStatusUpdate = async (e) => {
    e.preventDefault();
    if (!targetStatus) return;
    if (targetStatus === booking.status) {
      alert("Vui lòng chọn trạng thái khác trạng thái hiện tại.");
      return;
    }

    if (!confirm(`Bạn có chắc chắn muốn thay đổi trạng thái đơn hàng sang ${translateStatus(targetStatus)} không?`)) {
      return;
    }

    setUpdating(true);
    try {
      await updateBookingStatus(bookingId, targetStatus, changeReason || "Admin manual update");
      setChangeReason('');
      // Reload details
      const freshData = await getBookingDetail(bookingId);
      setBooking(freshData);
      setTargetStatus(freshData.status);
      alert("Cập nhật trạng thái đơn hàng thành công.");
    } catch (err) {
      alert("Lỗi cập nhật trạng thái đơn hàng: " + (err.message || "Lỗi kết nối"));
    } finally {
      setUpdating(false);
    }
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

  const formatCurrency = (val) => {
    return (val || 0).toLocaleString('vi-VN') + 'đ';
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#050506] text-white">
        <div className="flex flex-col items-center gap-4 animate-pulse">
          <div className="w-12 h-12 border-4 border-brand-orange border-t-transparent rounded-full animate-spin"></div>
          <p className="text-sm font-semibold tracking-wider text-zinc-400">Đang tải chi tiết đơn hàng...</p>
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
        <h2 className="text-xl font-bold text-zinc-100 mb-2">Không tìm thấy đơn hàng</h2>
        <p className="text-sm text-zinc-400 max-w-md mb-8">{error || "Hóa đơn này không tồn tại hoặc đã bị xóa."}</p>
        <button
          onClick={() => navigate('/admin/bookings')}
          className="bg-brand-orange hover:bg-opacity-90 text-white font-bold px-6 py-3 rounded-full transition-all text-xs uppercase tracking-wider"
        >
          Quay lại danh sách
        </button>
      </div>
    );
  }

  const { snapshot, tickets = [], foodOrder, statusHistories = [] } = booking;

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-screen bg-zinc-950 text-zinc-100 space-y-6 selection:bg-brand-orange selection:text-zinc-950">
      
      {/* Back Link */}
      <div>
        <button
          onClick={() => navigate('/admin/bookings')}
          className="flex items-center gap-2 text-zinc-400 hover:text-brand-orange transition-colors text-xs font-bold"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại danh sách đơn hàng</span>
        </button>
      </div>

      {/* Header Info */}
      <div className="bg-zinc-900 border border-zinc-850 rounded-3xl p-6 md:p-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-6 shadow-2xl">
        <div className="space-y-1.5">
          <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider block">Chi tiết đơn đặt vé</span>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-xl md:text-2xl font-black tracking-widest text-white uppercase">{booking.bookingCode}</h1>
            <span className={`text-[10px] border px-2.5 py-0.5 rounded-full font-black uppercase tracking-wider ${getStatusColor(booking.status)}`}>
              {translateStatus(booking.status)}
            </span>
          </div>
          <p className="text-[10px] text-zinc-500 font-semibold">
            Ngày lập đơn: {new Date(booking.createdAt).toLocaleString('vi-VN')}
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Left Side: Order info & Audit History */}
        <div className="lg:col-span-2 space-y-8">
          
          {/* Movie snapshot details card */}
          <div className="bg-zinc-900 border border-zinc-850 rounded-3xl p-6 md:p-8 space-y-6">
            <div className="border-b border-zinc-800 pb-3 flex items-center gap-2">
              <FileText className="w-4 h-4 text-brand-orange" />
              <span className="text-xs font-black uppercase tracking-wider text-white">Thông tin Suất chiếu & Ghế</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-xs text-zinc-400">
              <div>
                <span className="text-zinc-500 font-bold block text-[10px] uppercase">Phim</span>
                <span className="text-white font-extrabold text-sm block mt-1">{snapshot?.movieTitle}</span>
                <span className="text-[10px] text-zinc-500 mt-1 block">Thời lượng: {snapshot?.duration} phút | Nhãn: {snapshot?.ageRating}</span>
              </div>
              <div>
                <span className="text-zinc-500 font-bold block text-[10px] uppercase">Lịch chiếu</span>
                <span className="text-white font-extrabold text-sm block mt-1">
                  {snapshot?.showtimeStart ? new Date(snapshot.showtimeStart).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : ''} | {snapshot?.showtimeStart ? new Date(snapshot.showtimeStart).toLocaleDateString('vi-VN') : ''}
                </span>
                <span className="text-[10px] text-zinc-500 mt-1 block">Rạp: {snapshot?.cinemaName} | Phòng: {snapshot?.auditoriumName}</span>
              </div>
            </div>

            {/* List of seats and generated tickets code */}
            <div className="space-y-3">
              <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Danh sách vé phát hành</span>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {tickets.map(t => (
                  <div key={t.id} className="bg-zinc-950 border border-zinc-850 rounded-2xl p-4 flex justify-between items-center text-xs">
                    <div>
                      <span className="text-zinc-100 font-extrabold block">Ghế {t.seatLabel} ({t.seatType})</span>
                      <span className="text-[9px] text-zinc-550 font-semibold mt-0.5 block">Mã vé: {t.ticketCode}</span>
                    </div>
                    <span className="text-[9px] bg-zinc-800 text-zinc-400 font-black px-2 py-0.5 rounded">
                      {t.status}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Concessions lists details */}
          <div className="bg-zinc-900 border border-zinc-850 rounded-3xl p-6 md:p-8 space-y-6">
            <div className="border-b border-zinc-800 pb-3 flex items-center gap-2">
              <ShoppingCart className="w-4 h-4 text-brand-orange" />
              <span className="text-xs font-black uppercase tracking-wider text-white">Danh sách bắp nước</span>
            </div>

            {foodOrder && foodOrder.items && foodOrder.items.length > 0 ? (
              <div className="divide-y divide-zinc-800">
                {foodOrder.items.map(item => (
                  <div key={item.id} className="py-4 flex justify-between items-center text-xs first:pt-0 last:pb-0">
                    <div className="space-y-1">
                      <h4 className="font-extrabold text-white">{item.productName}</h4>
                      <p className="text-[9px] text-zinc-500">Mã sản phẩm: {item.productCode} | Loại: {item.productType}</p>
                    </div>
                    <div className="flex items-center gap-6">
                      <span className="text-zinc-500 font-bold">x{item.quantity}</span>
                      <span className="text-white font-black text-right min-w-[70px]">{formatCurrency(item.finalAmount)}</span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-6 text-xs text-zinc-500 italic">
                Khách hàng không mua kèm bắp nước
              </div>
            )}
          </div>

          {/* Transition Audit log */}
          <div className="bg-zinc-900 border border-zinc-850 rounded-3xl p-6 md:p-8 space-y-6">
            <div className="border-b border-zinc-800 pb-3 flex items-center gap-2">
              <History className="w-4 h-4 text-brand-orange" />
              <span className="text-xs font-black uppercase tracking-wider text-white">Lịch sử thay đổi trạng thái (Audit Log)</span>
            </div>

            {statusHistories.length > 0 ? (
              <div className="relative border-l border-zinc-800 ml-4 pl-6 space-y-6 py-2">
                {statusHistories.map((log, index) => (
                  <div key={log.id || index} className="relative space-y-1.5 text-xs">
                    {/* Circle marker */}
                    <div className="absolute -left-[31px] top-0.5 w-2.5 h-2.5 rounded-full bg-brand-orange border-2 border-zinc-900 shadow"></div>
                    
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-zinc-500 text-[10px] font-bold">
                        {new Date(log.createdAt).toLocaleString('vi-VN')}
                      </span>
                      <span className="text-zinc-650">•</span>
                      <span className="text-zinc-400 font-bold">
                        {log.fromStatus ? translateStatus(log.fromStatus) : 'Mới'} → <span className="text-white font-extrabold">{translateStatus(log.toStatus)}</span>
                      </span>
                    </div>

                    <div className="text-[10px] text-zinc-500 font-semibold space-y-0.5">
                      <div>Lý do: <span className="text-zinc-400 font-bold italic">"{log.reason || 'Không có lý do'}"</span></div>
                      <div>Tác nhân: <span className="text-zinc-400">{log.changedBy || 'Hệ thống'}</span> (Kênh: <span className="text-zinc-550">{log.source || 'CRON'}</span>)</div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-6 text-xs text-zinc-500 italic">
                Không ghi nhận lịch sử thay đổi trạng thái
              </div>
            )}
          </div>
        </div>

        {/* Right Side Sticky Sidebar: Pricing & Status Update panel */}
        <div className="lg:col-span-1 space-y-8 sticky top-24">
          
          {/* Price Summary card */}
          <div className="bg-zinc-900 border border-zinc-850 rounded-3xl p-6 space-y-4 shadow-2xl">
            <h3 className="text-xs font-black uppercase tracking-wider text-zinc-500 border-b border-zinc-800 pb-3">Chi tiết doanh thu</h3>
            
            <div className="space-y-3 text-xs">
              <div className="flex justify-between text-zinc-400">
                <span>Doanh thu vé:</span>
                <span className="font-semibold text-zinc-200">{formatCurrency(booking.ticketAmount)}</span>
              </div>
              <div className="flex justify-between text-zinc-400">
                <span>Doanh thu bắp nước:</span>
                <span className="font-semibold text-zinc-200">{formatCurrency(foodOrder ? foodOrder.finalAmount : 0)}</span>
              </div>
              {booking.promotionDiscount > 0 && (
                <div className="flex justify-between text-emerald-500 font-bold">
                  <span>Khuyến mãi giảm giá:</span>
                  <span>-{formatCurrency(booking.promotionDiscount)}</span>
                </div>
              )}
              <div className="flex justify-between items-center py-4 px-4 bg-zinc-950/60 rounded-2xl border border-zinc-850 shadow-inner mt-2">
                <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider">Tổng cộng</span>
                <span className="text-lg font-black text-brand-orange">{formatCurrency(booking.finalAmount)}</span>
              </div>
            </div>
          </div>

          {/* Manual Transition Control Panel */}
          <div className="bg-zinc-900 border border-zinc-850 rounded-3xl p-6 space-y-4 shadow-2xl">
            <div className="flex items-center gap-2 border-b border-zinc-800 pb-3">
              <Sliders className="w-4 h-4 text-brand-orange" />
              <span className="text-xs font-black uppercase tracking-wider text-white">Điều khiển trạng thái</span>
            </div>

            <form onSubmit={handleStatusUpdate} className="space-y-4 text-xs">
              <div className="space-y-1">
                <label className="text-[10px] text-zinc-500 font-bold uppercase">Trạng thái đích</label>
                <select
                  value={targetStatus}
                  onChange={(e) => setTargetStatus(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-brand-orange text-zinc-200"
                >
                  <option value="PENDING_PAYMENT">Chờ thanh toán</option>
                  <option value="CONFIRMED">Đã thanh toán (Confirm)</option>
                  <option value="CANCELLED">Hủy bỏ (Cancel)</option>
                  <option value="EXPIRED">Hết hạn (Expire)</option>
                </select>
              </div>

              <div className="space-y-1">
                <label className="text-[10px] text-zinc-500 font-bold uppercase">Lý do thay đổi</label>
                <textarea
                  placeholder="Nhập lý do thay đổi để ghi nhật ký audit..."
                  value={changeReason}
                  onChange={(e) => setChangeReason(e.target.value)}
                  rows="3"
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-brand-orange text-zinc-200 resize-none"
                  required
                />
              </div>

              <button
                type="submit"
                disabled={updating || targetStatus === booking.status}
                className={`w-full py-3.5 rounded-xl font-bold uppercase text-[10px] tracking-widest transition-colors ${
                  targetStatus !== booking.status && !updating
                    ? 'bg-brand-orange hover:bg-opacity-90 text-white cursor-pointer'
                    : 'bg-zinc-850 text-zinc-600 border border-zinc-800 cursor-not-allowed'
                }`}
              >
                {updating ? 'Đang thực hiện chuyển đổi...' : 'Áp dụng trạng thái mới'}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
