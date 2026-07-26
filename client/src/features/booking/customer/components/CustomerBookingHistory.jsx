import { useState, useEffect, useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertTriangle, ArrowUpDown, ChevronRight, Clock3, CreditCard, Film, Info,
  Search, Trash2
} from 'lucide-react';
import { cancelBooking, getBookingHistory } from '../services/bookingService';
import {
  formatHoldTimeLeft,
  getBookingRecoveryState
} from '../utils/bookingRecovery';
import BookingCancellationModal from './BookingCancellationModal';

export default function CustomerBookingHistory() {
  // Filters state
  const [status, setStatus] = useState('ALL'); // 'ALL', 'PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'EXPIRED'
  const [searchQuery, setSearchQuery] = useState('');
  const [page, setPage] = useState(0);
  const [size] = useState(6);
  
  // New features: Date range and sorting
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [sortField, setSortField] = useState('createdAt'); // 'createdAt', 'totalAmount'
  const [sortDirection, setSortDirection] = useState('desc'); // 'asc', 'desc'

  const [bookingPage, setBookingPage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [nowMs, setNowMs] = useState(() => Date.now());
  const [cancellingBookingId, setCancellingBookingId] = useState(null);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelError, setCancelError] = useState('');

  const fetchHistory = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const activeStatus = status === 'ALL' ? undefined : status;
      const historyData = await getBookingHistory({
        page,
        size,
        status: activeStatus,
        fromDate: fromDate || undefined,
        toDate: toDate || undefined,
        sort: `${sortField},${sortDirection}`
      });
      setBookingPage(historyData);
    } catch (err) {
      setError(err.message || err.detail || "Không thể tải lịch sử đặt vé.");
    } finally {
      setLoading(false);
    }
  }, [page, size, status, fromDate, toDate, sortField, sortDirection]);

  useEffect(() => {
    // The effect owns loading history whenever its memoized query changes.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchHistory();
  }, [fetchHistory]);

  useEffect(() => {
    const timer = window.setInterval(() => setNowMs(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  const handleCancelHold = async () => {
    const publicId = cancelTarget?.publicId || cancelTarget?.id;
    if (!publicId || cancellingBookingId) return;

    setCancellingBookingId(publicId);
    setCancelError('');
    try {
      await cancelBooking(
        publicId,
        'Khách hàng chủ động hủy giữ ghế từ lịch sử đặt vé'
      );
      await fetchHistory();
      setCancelTarget(null);
    } catch (requestError) {
      setCancelError(
        `Không thể hủy giữ ghế: ${requestError.message || 'Vui lòng thử lại.'}`
      );
    } finally {
      setCancellingBookingId(null);
    }
  };

  const handleStatusTabChange = (newStatus) => {
    setStatus(newStatus);
    setPage(0); // Reset to first page
  };

  const formatCurrency = (val) => {
    return (val || 0).toLocaleString('vi-VN') + 'đ';
  };

  const getStatusBadgeStyle = (bStatus) => {
    switch (bStatus) {
      case 'CONFIRMED':
        return 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20';
      case 'PENDING_PAYMENT':
        return 'bg-amber-500/10 text-amber-400 border border-amber-500/20';
      case 'CANCELLED':
        return 'bg-red-500/10 text-red-400 border border-red-500/20';
      case 'EXPIRED':
        return 'bg-zinc-800 text-zinc-500 border border-zinc-700';
      default:
        return 'bg-zinc-800 text-zinc-400';
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

  const filteredBookings = useMemo(() => {
    const list = bookingPage?.content || [];
    if (!searchQuery) return list;
    return list.filter(b =>
      (b.bookingCode || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
      (b.movieTitle || b.presentation?.movieTitle || b.snapshot?.movieTitle || '')
        .toLowerCase()
        .includes(searchQuery.toLowerCase())
    );
  }, [bookingPage, searchQuery]);

  return (
    <div className="space-y-6">
      {cancelTarget && (
        <BookingCancellationModal
          bookingCode={cancelTarget.bookingCode}
          error={cancelError}
          pending={cancellingBookingId !== null}
          onClose={() => {
            setCancelError('');
            setCancelTarget(null);
          }}
          onConfirm={handleCancelHold}
        />
      )}

      {/* Filters and Tabs bar */}
      <div className="flex flex-col xl:flex-row justify-between items-start xl:items-center gap-4 border-b border-zinc-800 pb-4">
        <div className="flex gap-2 overflow-x-auto py-1 w-full xl:w-auto scrollbar-none">
          {['ALL', 'PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'EXPIRED'].map(tab => (
            <button
              key={tab}
              onClick={() => handleStatusTabChange(tab)}
              className={`px-4 py-2 rounded-xl text-xs font-bold transition-all shrink-0 cursor-pointer ${
                status === tab
                  ? 'bg-brand-orange text-white'
                  : 'bg-zinc-900 text-zinc-400 hover:text-zinc-200 border border-zinc-850'
              }`}
            >
              {tab === 'ALL' ? 'TẤT CẢ' : translateStatus(tab)}
            </button>
          ))}
        </div>

        <div className="flex flex-wrap gap-2 w-full xl:w-auto items-center">
          {/* Quick search input */}
          <div className="relative w-full sm:w-auto sm:min-w-[200px] shrink-0">
            <Search className="w-3.5 h-3.5 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Mã đặt vé / Phim..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-zinc-900 border border-zinc-850 rounded-xl pl-9 pr-4 py-2 text-xs text-zinc-200 focus:outline-none focus:border-brand-orange placeholder:text-zinc-650"
            />
          </div>
          
          {/* Date Range filter */}
          <div className="flex items-center gap-2">
            <input 
              type="date"
              value={fromDate}
              onChange={(e) => { setFromDate(e.target.value); setPage(0); }}
              className="bg-zinc-900 border border-zinc-850 rounded-xl px-3 py-2 text-xs text-zinc-200 focus:outline-none focus:border-brand-orange"
              title="Từ ngày"
            />
            <span className="text-zinc-600 font-bold">-</span>
            <input 
              type="date"
              value={toDate}
              onChange={(e) => { setToDate(e.target.value); setPage(0); }}
              className="bg-zinc-900 border border-zinc-850 rounded-xl px-3 py-2 text-xs text-zinc-200 focus:outline-none focus:border-brand-orange"
              title="Đến ngày"
            />
          </div>

          {/* Sorting */}
          <div className="flex gap-2">
            <select
              value={sortField}
              onChange={(e) => { setSortField(e.target.value); setPage(0); }}
              className="bg-zinc-900 border border-zinc-850 rounded-xl px-3 py-2 text-xs text-zinc-200 focus:outline-none focus:border-brand-orange"
            >
              <option value="createdAt">Ngày đặt (Mới - Cũ)</option>
              <option value="totalAmount">Giá trị đơn hàng</option>
            </select>
            <button
              type="button"
              onClick={() => { setSortDirection(prev => prev === 'asc' ? 'desc' : 'asc'); setPage(0); }}
              className="bg-zinc-900 border border-zinc-850 p-2 rounded-xl text-zinc-400 hover:text-white transition-colors"
              title="Đổi chiều sắp xếp"
            >
              <ArrowUpDown className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {loading ? (
        /* Loading states skeletons */
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="bg-zinc-900/40 border border-zinc-850 rounded-3xl p-6 h-48 animate-pulse space-y-4">
              <div className="h-4 bg-zinc-800 rounded w-1/3"></div>
              <div className="h-6 bg-zinc-800 rounded w-3/4"></div>
              <div className="h-4 bg-zinc-800 rounded w-1/2"></div>
            </div>
          ))}
        </div>
      ) : error ? (
        <div className="text-center py-12 bg-zinc-900/30 border border-zinc-800 rounded-3xl">
          <AlertTriangle className="w-12 h-12 text-red-500 mx-auto mb-4" />
          <p className="text-sm text-zinc-400">{error}</p>
        </div>
      ) : filteredBookings.length > 0 ? (
        /* Bookings Grid list */
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {filteredBookings.map(b => {
              const bookingStatus = b.bookingStatus || b.status;
              const publicId = b.publicId || b.id;
              const recovery = getBookingRecoveryState(b, nowMs);
              const displayStatus = recovery.isExpiredPending ? 'EXPIRED' : bookingStatus;
              const amount = b.finalAmount ?? b.totalAmount ?? 0;
              const presentation = b.presentation || b.snapshot || {};
              const movieTitle = b.movieTitle || presentation.movieTitle;
              const posterUrl = b.posterUrl
                || presentation.moviePosterUrl
                || presentation.moviePoster;
              const cinemaName = b.cinemaName || presentation.cinemaName;
              const auditoriumName = b.auditoriumName || presentation.auditoriumName;
              const showtimeStart = b.showtimeStart || presentation.showtimeStart;
              const seatNames = b.seatLabel
                || b.seatNames
                || presentation.seats?.map(seat => seat.label).filter(Boolean).join(', ');
              const food = b.food || b.foodOrder;
              const foodNames = b.foodNames
                || food?.items?.map(item =>
                  `${item.name || item.productName} x${item.quantity}`).join(', ');

              return (
                <div
                  key={b.publicId || b.id}
                  className="bg-zinc-900/60 hover:bg-zinc-900 border border-zinc-850 hover:border-zinc-750 transition-all rounded-3xl p-5 flex flex-col justify-between space-y-4 shadow-xl"
                >
                  <div className="flex flex-col sm:flex-row gap-4">
                    {/* Poster Placeholder */}
                    <div className="relative w-full sm:w-24 h-32 sm:h-auto bg-zinc-800 rounded-xl overflow-hidden shrink-0 flex items-center justify-center border border-zinc-750">
                      <Film className="w-8 h-8 text-zinc-600" />
                      {posterUrl && (
                        <img
                          src={posterUrl}
                          alt={`Poster ${movieTitle || 'phim'}`}
                          className="absolute inset-0 w-full h-full object-cover"
                          onError={event => {
                            event.currentTarget.style.display = 'none';
                          }}
                        />
                      )}
                    </div>
                    
                    <div className="flex-1 space-y-3">
                      <div className="flex justify-between items-start gap-4">
                        <div>
                          <span className="text-xs text-brand-orange font-black tracking-wider uppercase">
                            {b.bookingCode}
                          </span>
                          <h3 className="text-sm font-black text-white line-clamp-2 leading-snug mt-1">
                            {movieTitle || 'Thông tin phim đang được cập nhật'}
                          </h3>
                        </div>
                        <span className={`text-[9px] font-black px-2 py-0.5 rounded uppercase tracking-wider whitespace-nowrap ${getStatusBadgeStyle(displayStatus)}`}>
                          {translateStatus(displayStatus)}
                        </span>
                      </div>

                      <div className="flex flex-col gap-1 text-[10px] text-zinc-400 font-semibold">
                        <div className="flex items-center gap-1.5">
                          <span className="text-zinc-500 w-16">Rạp:</span>
                          <span className="text-zinc-300">
                            {[cinemaName, auditoriumName].filter(Boolean).join(' · ') || 'Chưa có thông tin'}
                          </span>
                        </div>
                        <div className="flex items-center gap-1.5">
                          <span className="text-zinc-500 w-16">Suất chiếu:</span>
                          <span className="text-zinc-300">
                            {showtimeStart
                              ? `${new Date(showtimeStart).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })} · ${new Date(showtimeStart).toLocaleDateString('vi-VN')}`
                              : 'Chưa có thông tin'}
                          </span>
                        </div>
                        <div className="flex items-center gap-1.5">
                          <span className="text-zinc-500 w-16">Ghế:</span>
                          <span className="text-zinc-300">{seatNames || 'Chưa có thông tin'}</span>
                        </div>
                        {(foodNames || b.foodAmount > 0) && (
                          <div className="flex items-center gap-1.5">
                            <span className="text-zinc-500 w-16">Bắp nước:</span>
                            <span className="text-zinc-300 line-clamp-1">{foodNames || 'Đã đặt bắp nước'}</span>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Financial Breakdown & Actions */}
                  <div className="border-t border-zinc-800/80 pt-4 space-y-3">
                    {recovery.canRecover && (
                      <div className="flex items-center justify-between gap-3 rounded-xl border border-amber-500/20 bg-amber-500/5 px-3 py-2 text-[10px] font-bold text-amber-300">
                        <span className="flex items-center gap-1.5">
                          <Clock3 className="h-3.5 w-3.5" />
                          Ghế đang được giữ
                        </span>
                        <span className="font-black tracking-wider">
                          {formatHoldTimeLeft(recovery.remainingSeconds)}
                        </span>
                      </div>
                    )}

                    {recovery.isExpiredPending && (
                      <p className="rounded-xl border border-zinc-700 bg-zinc-800/60 px-3 py-2 text-[10px] font-bold text-zinc-400">
                        Thời gian giữ ghế đã kết thúc. Bạn không thể tiếp tục thanh toán đơn này.
                      </p>
                    )}

                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 text-[10px] text-zinc-400">
                      <div>
                        <span className="text-zinc-600 font-bold block uppercase">Tiền vé</span>
                        <span className="text-zinc-300">{formatCurrency(b.ticketAmount || 0)}</span>
                      </div>
                      <div>
                        <span className="text-zinc-600 font-bold block uppercase">Bắp nước</span>
                        <span className="text-zinc-300">{formatCurrency(b.foodAmount || 0)}</span>
                      </div>
                      <div className="col-span-2 sm:col-span-1 text-right sm:text-left">
                        <span className="text-brand-orange/80 font-bold block uppercase">Tổng thanh toán</span>
                        <span className="text-brand-orange font-black text-xs">{formatCurrency(amount)}</span>
                      </div>
                    </div>

                    <div className="flex flex-wrap justify-between items-center gap-2 text-xs pt-2">
                      <span className="text-[9px] text-zinc-500 uppercase tracking-widest font-bold">
                        Lập ngày: {new Date(b.createdAt).toLocaleDateString('vi-VN')}
                      </span>

                      <div className="flex flex-wrap justify-end gap-2">
                        {recovery.canRecover && (
                          <>
                            <Link
                              to={`/bookings/checkout?bookingId=${publicId}`}
                              className="flex items-center gap-1.5 rounded-lg bg-brand-orange px-3 py-2 text-[9px] font-black uppercase tracking-wider text-white transition-colors hover:bg-orange-600"
                            >
                              <CreditCard className="h-3.5 w-3.5" />
                              Tiếp tục thanh toán
                            </Link>
                            <button
                              type="button"
                              disabled={cancellingBookingId === publicId}
                              onClick={() => {
                                setCancelError('');
                                setCancelTarget(b);
                              }}
                              className="flex items-center gap-1.5 rounded-lg border border-red-500/20 px-3 py-2 text-[9px] font-black uppercase tracking-wider text-red-400 transition-colors hover:bg-red-500/10 disabled:cursor-wait disabled:opacity-60"
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                              {cancellingBookingId === publicId ? 'Đang hủy...' : 'Hủy giữ ghế'}
                            </button>
                          </>
                        )}
                        <Link
                          to={`/bookings/${publicId}`}
                          className="flex items-center gap-1 text-zinc-300 hover:text-white bg-zinc-800 hover:bg-zinc-700 px-3 py-2 rounded-lg font-black tracking-widest uppercase text-[9px] transition-colors"
                        >
                          <span>Chi tiết</span>
                          <ChevronRight className="w-3.5 h-3.5" />
                        </Link>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Pagination Controls */}
          {bookingPage?.totalPages > 1 && (
            <div className="flex justify-center gap-2 pt-6">
              <button
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
                className={`px-4 py-2 border rounded-xl text-xs font-bold transition-all ${
                  page === 0
                    ? 'border-zinc-850 text-zinc-650 cursor-not-allowed'
                    : 'border-zinc-800 text-zinc-350 hover:bg-zinc-850 hover:text-white'
                }`}
              >
                Trước
              </button>
              <div className="flex items-center text-xs text-zinc-500 font-semibold px-4">
                Trang {page + 1} / {bookingPage.totalPages}
              </div>
              <button
                disabled={page >= bookingPage.totalPages - 1}
                onClick={() => setPage(page + 1)}
                className={`px-4 py-2 border rounded-xl text-xs font-bold transition-all ${
                  page >= bookingPage.totalPages - 1
                    ? 'border-zinc-850 text-zinc-650 cursor-not-allowed'
                    : 'border-zinc-800 text-zinc-350 hover:bg-zinc-850 hover:text-white'
                }`}
              >
                Sau
              </button>
            </div>
          )}
        </div>
      ) : (
        /* Empty State */
        <div className="text-center py-16 bg-zinc-900/20 border border-zinc-850 rounded-3xl border-dashed">
          <Info className="w-12 h-12 text-zinc-600 mx-auto mb-4" />
          <h3 className="text-sm font-bold text-zinc-300">Không có đơn đặt vé nào</h3>
          <p className="text-xs text-zinc-500 max-w-xs mx-auto mt-2 leading-relaxed">
            Bạn chưa mua vé xem phim nào hoặc bộ lọc trạng thái hiện tại không khớp kết quả nào.
          </p>
          <Link
            to="/booking"
            className="inline-block mt-6 bg-brand-orange hover:bg-opacity-95 text-white font-bold px-6 py-3 rounded-full text-xs uppercase tracking-wider shadow-lg shadow-brand-orange/15"
          >
            Đặt vé ngay
          </Link>
        </div>
      )}
    </div>
  );
}
