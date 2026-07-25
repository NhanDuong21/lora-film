import { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, SlidersHorizontal, Eye, RefreshCw, ShoppingCart, CheckCircle, XCircle, AlertCircle, Clock } from 'lucide-react';
import { getBookings } from '../services/adminBookingService';

export default function AdminBookingDashboardPage() {
  const navigate = useNavigate();

  // Filter States
  const [bookingCode, setBookingCode] = useState('');
  const [userId, setUserId] = useState('');
  const [status, setStatus] = useState('ALL');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [page, setPage] = useState(0);
  const [size] = useState(10);

  const [bookingPage, setBookingPage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  const fetchBookingsList = useCallback(async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true);
    else setLoading(true);
    setError(null);

    try {
      const filters = {
        page,
        size
      };
      if (bookingCode) filters.bookingCode = bookingCode.trim();
      if (userId) filters.userId = Number(userId);
      if (status !== 'ALL') filters.status = status;
      if (fromDate) filters.fromDate = new Date(fromDate).toISOString();
      if (toDate) filters.toDate = new Date(toDate).toISOString();

      const response = await getBookings(filters);
      setBookingPage(response);
    } catch (err) {
      setError(err.message || err.detail || "Không thể tải danh sách đơn hàng.");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [page, size, bookingCode, userId, status, fromDate, toDate]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchBookingsList();
  }, [fetchBookingsList]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    fetchBookingsList();
  };

  const handleResetFilters = () => {
    setBookingCode('');
    setUserId('');
    setStatus('ALL');
    setFromDate('');
    setToDate('');
    setPage(0);
  };

  const formatCurrency = (val) => {
    return (val || 0).toLocaleString('vi-VN') + 'đ';
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

  // Dynamically compute stats from page items for UI look
  const stats = useMemo(() => {
    const list = bookingPage?.content || [];
    const counts = { total: bookingPage?.totalElements || 0, pending: 0, confirmed: 0, cancelled: 0 };
    list.forEach(b => {
      if (b.bookingStatus === 'PENDING_PAYMENT') counts.pending++;
      else if (b.bookingStatus === 'CONFIRMED') counts.confirmed++;
      else if (b.bookingStatus === 'CANCELLED' || b.bookingStatus === 'EXPIRED') counts.cancelled++;
    });
    return counts;
  }, [bookingPage]);

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-screen bg-zinc-950 text-zinc-100 space-y-6 selection:bg-brand-orange selection:text-zinc-950">

      {/* Title */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-zinc-850 pb-4">
        <div>
          <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">Quản Lý Đơn Hàng</h1>
          <p className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mt-1">
            Tra cứu, kiểm tra lịch sử và cập nhật trạng thái đặt vé của khách hàng
          </p>
        </div>
        <button
          onClick={() => fetchBookingsList(true)}
          disabled={refreshing}
          className="bg-zinc-900 border border-zinc-800 hover:border-zinc-700 p-2.5 rounded-xl text-zinc-400 hover:text-white transition-all text-xs flex items-center gap-2 cursor-pointer"
        >
          <RefreshCw className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} />
          <span>{refreshing ? 'Đang làm mới...' : 'Làm mới'}</span>
        </button>
      </div>

      {/* Metrics Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Metric 1 */}
        <div className="bg-zinc-900 border border-zinc-850 rounded-2xl p-5 flex items-center gap-4 shadow-lg">
          <div className="w-12 h-12 rounded-xl bg-brand-orange/10 flex items-center justify-center text-brand-orange shrink-0 border border-brand-orange/20">
            <ShoppingCart className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider block">Tổng số đơn hàng</span>
            <span className="text-2xl font-black text-white">{stats.total}</span>
          </div>
        </div>

        {/* Metric 2 */}
        <div className="bg-zinc-900 border border-zinc-850 rounded-2xl p-5 flex items-center gap-4 shadow-lg">
          <div className="w-12 h-12 rounded-xl bg-amber-500/10 flex items-center justify-center text-amber-400 shrink-0 border border-amber-500/20">
            <Clock className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider block">Đang chờ thanh toán</span>
            <span className="text-2xl font-black text-white">{stats.pending}</span>
          </div>
        </div>

        {/* Metric 3 */}
        <div className="bg-zinc-900 border border-zinc-850 rounded-2xl p-5 flex items-center gap-4 shadow-lg">
          <div className="w-12 h-12 rounded-xl bg-emerald-500/10 flex items-center justify-center text-emerald-400 shrink-0 border border-emerald-500/20">
            <CheckCircle className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider block">Đã thanh toán (Paid)</span>
            <span className="text-2xl font-black text-white">{stats.confirmed}</span>
          </div>
        </div>

        {/* Metric 4 */}
        <div className="bg-zinc-900 border border-zinc-850 rounded-2xl p-5 flex items-center gap-4 shadow-lg">
          <div className="w-12 h-12 rounded-xl bg-red-500/10 flex items-center justify-center text-red-400 shrink-0 border border-red-500/20">
            <XCircle className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider block">Hủy / Hết hạn</span>
            <span className="text-2xl font-black text-white">{stats.cancelled}</span>
          </div>
        </div>
      </div>

      {/* Search & Filter Panel */}
      <form onSubmit={handleSearchSubmit} className="bg-zinc-900 border border-zinc-850 rounded-2xl p-5 md:p-6 space-y-4 shadow-xl">
        <div className="flex items-center gap-2 border-b border-zinc-800 pb-3">
          <SlidersHorizontal className="w-4 h-4 text-brand-orange" />
          <span className="text-xs font-black uppercase tracking-wider text-white">Bộ lọc tìm kiếm</span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
          {/* Booking Code */}
          <div className="space-y-1">
            <label className="text-[10px] text-zinc-500 font-bold uppercase">Mã đặt vé</label>
            <input
              type="text"
              placeholder="VD: BOOKING23..."
              value={bookingCode}
              onChange={(e) => setBookingCode(e.target.value)}
              className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-brand-orange text-zinc-200"
            />
          </div>

          {/* User ID */}
          <div className="space-y-1">
            <label className="text-[10px] text-zinc-500 font-bold uppercase">Mã khách hàng (User ID)</label>
            <input
              type="number"
              placeholder="VD: 1, 2"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-brand-orange text-zinc-200"
            />
          </div>

          {/* Status Dropdown */}
          <div className="space-y-1">
            <label className="text-[10px] text-zinc-500 font-bold uppercase">Trạng thái</label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-brand-orange text-zinc-200"
            >
              <option value="ALL">Tất cả</option>
              <option value="PENDING_PAYMENT">Chờ thanh toán</option>
              <option value="CONFIRMED">Đã thanh toán</option>
              <option value="CANCELLED">Đã hủy</option>
              <option value="EXPIRED">Hết hạn</option>
            </select>
          </div>

          {/* From Date */}
          <div className="space-y-1">
            <label className="text-[10px] text-zinc-500 font-bold uppercase">Từ ngày</label>
            <input
              type="date"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-brand-orange text-zinc-200"
            />
          </div>
        </div>

        {/* Action triggers */}
        <div className="flex justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={handleResetFilters}
            className="px-4 py-2 border border-zinc-800 text-zinc-400 hover:text-white rounded-xl text-xs font-bold transition-all cursor-pointer"
          >
            Xóa bộ lọc
          </button>

          <button
            type="submit"
            className="px-6 py-2 bg-brand-orange hover:bg-opacity-90 text-white rounded-xl text-xs font-bold tracking-wider uppercase transition-all shadow-md shadow-brand-orange/15 flex items-center gap-1.5 cursor-pointer"
          >
            <Search className="w-3.5 h-3.5" />
            <span>Tìm kiếm</span>
          </button>
        </div>
      </form>

      {/* Bookings Data Table */}
      <div className="bg-zinc-900 border border-zinc-850 rounded-2xl overflow-hidden shadow-2xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-zinc-950/60 text-zinc-500 border-b border-zinc-800 uppercase tracking-widest text-[10px] font-black">
                <th className="p-4 pl-6">Mã đặt vé</th>
                <th className="p-4">User ID</th>
                <th className="p-4">Tổng tiền</th>
                <th className="p-4">Cổng</th>
                <th className="p-4">Trạng thái</th>
                <th className="p-4">Ngày tạo</th>
                <th className="p-4 text-center pr-6">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                [...Array(5)].map((_, idx) => (
                  <tr key={idx} className="border-b border-zinc-800/40 animate-pulse">
                    <td className="p-4 pl-6"><div className="h-4 bg-zinc-800 rounded w-16"></div></td>
                    <td className="p-4"><div className="h-4 bg-zinc-800 rounded w-10"></div></td>
                    <td className="p-4"><div className="h-4 bg-zinc-800 rounded w-20"></div></td>
                    <td className="p-4"><div className="h-4 bg-zinc-800 rounded w-12"></div></td>
                    <td className="p-4"><div className="h-6 bg-zinc-800 rounded w-24"></div></td>
                    <td className="p-4"><div className="h-4 bg-zinc-800 rounded w-24"></div></td>
                    <td className="p-4 text-center"><div className="h-8 bg-zinc-800 rounded w-12 mx-auto"></div></td>
                  </tr>
                ))
              ) : error ? (
                <tr>
                  <td colSpan="7" className="p-8 text-center text-red-500">
                    <div className="flex flex-col items-center gap-2">
                      <AlertCircle className="w-8 h-8" />
                      <span>{error}</span>
                    </div>
                  </td>
                </tr>
              ) : bookingPage?.content?.length > 0 ? (
                bookingPage.content.map(b => (
                  <tr key={b.id} className="border-b border-zinc-850 hover:bg-zinc-950/20 text-zinc-350 transition-colors">
                    <td className="p-4 pl-6 font-bold text-zinc-100">{b.bookingCode}</td>
                    <td className="p-4 font-semibold">{b.userId}</td>
                    <td className="p-4 font-black text-white">{formatCurrency(b.finalAmount)}</td>
                    <td className="p-4 font-bold">{b.paymentMethodSnapshot || 'MOCK'}</td>
                    <td className="p-4">
                      <span className={`text-[9px] font-black px-2 py-0.5 rounded uppercase tracking-wider ${getStatusBadgeStyle(b.bookingStatus)}`}>
                        {translateStatus(b.bookingStatus)}
                      </span>
                    </td>
                    <td className="p-4 text-zinc-500 font-semibold">
                      {new Date(b.createdAt).toLocaleString('vi-VN')}
                    </td>
                    <td className="p-4 text-center pr-6">
                      <button
                        onClick={() => navigate(`/admin/bookings/${b.publicId}`)}
                        className="bg-zinc-950 border border-zinc-800 hover:border-brand-orange hover:text-white p-2 rounded-lg text-zinc-400 transition-all cursor-pointer flex items-center justify-center gap-1.5 text-[10px] uppercase font-bold mx-auto"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        <span>Xem</span>
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="7" className="p-8 text-center text-zinc-500 italic">
                    Không tìm thấy đơn hàng nào khớp với bộ lọc
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination controls */}
        {!loading && bookingPage?.totalPages > 1 && (
          <div className="bg-zinc-950/40 p-4 border-t border-zinc-800 flex justify-between items-center text-xs">
            <span className="text-zinc-500 font-bold">
              Tổng số bản ghi: <span className="text-zinc-300">{bookingPage.totalElements}</span>
            </span>

            <div className="flex gap-2">
              <button
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
                className={`px-3 py-1.5 border rounded-lg font-bold transition-all ${
                  page === 0
                    ? 'border-zinc-850 text-zinc-650 cursor-not-allowed'
                    : 'border-zinc-800 text-zinc-350 hover:bg-zinc-850 hover:text-white'
                }`}
              >
                Trước
              </button>
              <div className="flex items-center text-zinc-500 font-bold px-3">
                Trang {page + 1} / {bookingPage.totalPages}
              </div>
              <button
                disabled={page >= bookingPage.totalPages - 1}
                onClick={() => setPage(page + 1)}
                className={`px-3 py-1.5 border rounded-lg font-bold transition-all ${
                  page >= bookingPage.totalPages - 1
                    ? 'border-zinc-850 text-zinc-650 cursor-not-allowed'
                    : 'border-zinc-800 text-zinc-350 hover:bg-zinc-850 hover:text-white'
                }`}
              >
                Sau
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
