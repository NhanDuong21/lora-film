import { useState, useEffect, useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight, Search, Info, AlertTriangle } from 'lucide-react';
import { getBookingHistory } from '../services/bookingService';

export default function BookingHistoryPage() {
  // Filters state
  const [status, setStatus] = useState('ALL'); // 'ALL', 'PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'EXPIRED'
  const [searchQuery, setSearchQuery] = useState('');
  const [page, setPage] = useState(0);
  const [size] = useState(6);

  const [bookingPage, setBookingPage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchHistory = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const activeStatus = status === 'ALL' ? undefined : status;
      const historyData = await getBookingHistory({
        page,
        size,
        status: activeStatus
      });
      setBookingPage(historyData);
    } catch (err) {
      setError(err.message || err.detail || "Không thể tải lịch sử đặt vé.");
    } finally {
      setLoading(false);
    }
  }, [page, size, status]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchHistory();
  }, [fetchHistory]);

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
      (b.movieTitle || '').toLowerCase().includes(searchQuery.toLowerCase())
    );
  }, [bookingPage, searchQuery]);

  return (
    <div className="bg-zinc-950 text-zinc-100 min-h-screen pt-32 pb-16 px-4 md:px-12 selection:bg-brand-orange selection:text-zinc-950 font-sans font-medium">
      <div className="max-w-6xl mx-auto w-full space-y-8">

        {/* Page Header */}
        <div>
          <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">Lịch Sử Đặt Vé</h1>
          <p className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mt-1">
            Xem lại danh sách vé xem phim và bắp nước bạn đã đặt
          </p>
        </div>

        {/* Filters and Tabs bar */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-zinc-800 pb-4">
          <div className="flex gap-2 overflow-x-auto py-1 w-full sm:w-auto scrollbar-none">
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

          {/* Quick search input */}
          <div className="relative w-full sm:w-64 shrink-0">
            <Search className="w-3.5 h-3.5 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Mã đặt vé / Phim..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-zinc-900 border border-zinc-850 rounded-xl pl-9 pr-4 py-2 text-xs text-zinc-200 focus:outline-none focus:border-brand-orange placeholder:text-zinc-650"
            />
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
                const amount = b.finalAmount ?? b.totalAmount ?? 0;

                return (
                <div
                  key={b.publicId || b.id}
                  className="bg-zinc-900/60 hover:bg-zinc-900 border border-zinc-850 hover:border-zinc-750 transition-all rounded-3xl p-6 flex flex-col justify-between space-y-4 shadow-xl"
                >
                  <div className="space-y-3">
                    <div className="flex justify-between items-start gap-4">
                      {/* Booking Code */}
                      <span className="text-xs text-brand-orange font-black tracking-wider uppercase">
                        {b.bookingCode}
                      </span>
                      {/* Status Badges */}
                      <span className={`text-[9px] font-black px-2 py-0.5 rounded uppercase tracking-wider ${getStatusBadgeStyle(bookingStatus)}`}>
                        {translateStatus(bookingStatus)}
                      </span>
                    </div>

                    {/* Movie Info */}
                    <div className="space-y-1">
                      <h3 className="text-sm font-black text-white line-clamp-1 leading-snug">{b.movieTitle || 'Đơn đặt vé'}</h3>
                      <div className="flex items-center gap-1 text-[10px] text-zinc-500 font-semibold">
                        <span>{b.cinemaName}</span>
                        <span>•</span>
                        <span>{b.auditoriumName}</span>
                      </div>
                    </div>

                    {/* Cost and Time info */}
                    <div className="grid grid-cols-2 gap-2 text-[10px] text-zinc-400 py-1 bg-zinc-950/40 rounded-xl p-3 border border-zinc-850">
                      <div>
                        <span className="text-zinc-600 font-bold block uppercase">Suất chiếu</span>
                        <span className="text-zinc-200 font-bold">
                          {b.showtimeStart ? new Date(b.showtimeStart).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : ''} | {b.showtimeStart ? new Date(b.showtimeStart).toLocaleDateString('vi-VN') : ''}
                        </span>
                      </div>
                      <div>
                        <span className="text-zinc-600 font-bold block uppercase">Tổng tiền</span>
                        <span className="text-white font-black text-xs">{formatCurrency(amount)}</span>
                      </div>
                    </div>
                  </div>

                  {/* Button trigger detail view */}
                  <div className="border-t border-zinc-850/80 pt-4 flex justify-between items-center text-xs">
                    <span className="text-[10px] text-zinc-500">
                      Đặt lúc: {new Date(b.createdAt).toLocaleDateString('vi-VN')}
                    </span>

                    <Link
                      to={`/bookings/${b.publicId}`}
                      className="flex items-center gap-1 text-brand-orange font-black hover:underline tracking-widest uppercase text-[10px]"
                    >
                      <span>Xem chi tiết</span>
                      <ChevronRight className="w-4 h-4" />
                    </Link>
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
    </div>
  );
}
