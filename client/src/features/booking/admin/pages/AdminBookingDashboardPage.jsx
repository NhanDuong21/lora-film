import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import { 
  Search, SlidersHorizontal, Eye, RefreshCw, ShoppingCart, 
  CheckCircle, XCircle, AlertCircle, Clock, Copy, FileDown, 
  ArrowUpDown, User, Film, Building, CreditCard, Gift, History
} from 'lucide-react';
import { getBookings, getBookingMonitoringSummary, updateBookingStatus } from '../services/adminBookingService';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import { useAuth } from '@/contexts/AuthContext';
import { parseApiError } from '@/utils/apiErrorHandler';
import SkeletonTable from '@/components/common/SkeletonTable';

export default function AdminBookingDashboardPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { triggerToast, triggerConfirm } = useOutletContext() || {};

  // Check if current user is Admin
  const isAdmin = user?.role === 'ADMIN';

  // API Filter States (Sent to Backend)
  const [bookingCode, setBookingCode] = useState('');
  const [userId, setUserId] = useState('');
  const [status, setStatus] = useState('ALL');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  // Sorting
  const [sortField, setSortField] = useState('createdAt');
  const [sortDirection, setSortDirection] = useState('desc');

  // Client-Side Advanced Filter States (Applied to Page Data)
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const [selectedMovieId, setSelectedMovieId] = useState('ALL');
  const [selectedCinemaId, setSelectedCinemaId] = useState('ALL');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [paymentStatusFilter, setPaymentStatusFilter] = useState('ALL');
  const [hasFoodFilter, setHasFoodFilter] = useState('ALL');
  const [hasPromotionFilter, setHasPromotionFilter] = useState('ALL');

  // Data States
  const [bookingPage, setBookingPage] = useState(null);
  const [monitoringSummary, setMonitoringSummary] = useState(null);
  const [moviesList, setMoviesList] = useState([]);
  const [cinemasList, setCinemasList] = useState([]);
  
  // Lookups
  const [moviesLookup, setMoviesLookup] = useState({});
  const [cinemasLookup, setCinemasLookup] = useState({});

  // Loading & Error States
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  // Table Column Widths (Resize Mock state)
  const [colWidths, setColWidths] = useState({
    code: 120,
    customer: 100,
    movie: 150,
    cinema: 120,
    showtime: 140,
    amounts: 180,
    status: 120,
    created: 140,
    actions: 130
  });

  const resizingCol = useRef(null);
  const startX = useRef(0);
  const startWidth = useRef(0);

  // Fetch Lookups (Movies and Cinemas)
  const fetchLookups = useCallback(async () => {
    try {
      const [moviesRes, cinemasRes] = await Promise.all([
        adminMovieService.getMovies({ size: 100 }),
        adminCinemaService.getCinemas({ size: 100 })
      ]);
      
      const movies = moviesRes?.data?.data || [];
      const cinemas = cinemasRes?.data?.data || [];
      
      setMoviesList(movies);
      setCinemasList(cinemas);

      const mLookup = {};
      movies.forEach(m => { mLookup[m.id] = m.movieTitle; });
      setMoviesLookup(mLookup);

      const cLookup = {};
      cinemas.forEach(c => { cLookup[c.id] = c.cinemaName; });
      setCinemasLookup(cLookup);
    } catch (err) {
      console.error("Error fetching lookups:", err);
    }
  }, []);

  // Fetch Summary stats
  const fetchSummaryStats = useCallback(async () => {
    try {
      const summary = await getBookingMonitoringSummary();
      setMonitoringSummary(summary);
    } catch (err) {
      console.warn("Could not fetch monitoring stats:", err);
    }
  }, []);

  // Fetch Bookings List
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
      setError(parseApiError(err) || "Không thể tải danh sách đơn hàng.");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [page, size, bookingCode, userId, status, fromDate, toDate]);

  useEffect(() => {
    fetchLookups();
  }, [fetchLookups]);

  useEffect(() => {
    fetchBookingsList();
    fetchSummaryStats();
  }, [fetchBookingsList, fetchSummaryStats]);

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
    setSelectedMovieId('ALL');
    setSelectedCinemaId('ALL');
    setMinPrice('');
    setMaxPrice('');
    setPaymentStatusFilter('ALL');
    setHasFoodFilter('ALL');
    setHasPromotionFilter('ALL');
    setPage(0);
  };

  // Client-side filtering & sorting logic
  const processedBookings = useMemo(() => {
    let list = bookingPage?.content || [];

    // Filter by Movie ID
    if (selectedMovieId !== 'ALL') {
      list = list.filter(b => b.movieId === Number(selectedMovieId));
    }
    // Filter by Cinema ID
    if (selectedCinemaId !== 'ALL') {
      list = list.filter(b => b.cinemaId === Number(selectedCinemaId));
    }
    // Filter by Price Range
    if (minPrice) {
      list = list.filter(b => b.finalAmount >= Number(minPrice));
    }
    if (maxPrice) {
      list = list.filter(b => b.finalAmount <= Number(maxPrice));
    }
    // Filter by Payment Status
    if (paymentStatusFilter !== 'ALL') {
      list = list.filter(b => b.paymentStatus === paymentStatusFilter);
    }
    // Filter by Food Ordered
    if (hasFoodFilter !== 'ALL') {
      list = list.filter(b => {
        const hasFood = b.foodAmount > 0;
        return hasFoodFilter === 'YES' ? hasFood : !hasFood;
      });
    }
    // Filter by Promotion Applied
    if (hasPromotionFilter !== 'ALL') {
      list = list.filter(b => {
        const hasPromo = b.promotionDiscount > 0 || b.voucherDiscount > 0;
        return hasPromotionFilter === 'YES' ? hasPromo : !hasPromo;
      });
    }

    // Sort list
    return [...list].sort((a, b) => {
      let valA = a[sortField];
      let valB = b[sortField];

      // Handle nulls
      if (valA === undefined || valA === null) valA = '';
      if (valB === undefined || valB === null) valB = '';

      if (typeof valA === 'string') {
        return sortDirection === 'asc' 
          ? valA.localeCompare(valB) 
          : valB.localeCompare(valA);
      } else {
        return sortDirection === 'asc' 
          ? valA - valB 
          : valB - valA;
      }
    });
  }, [bookingPage, selectedMovieId, selectedCinemaId, minPrice, maxPrice, paymentStatusFilter, hasFoodFilter, hasPromotionFilter, sortField, sortDirection]);

  // Aggregate stats from processed bookings list for cards
  const aggregatedStats = useMemo(() => {
    const list = bookingPage?.content || [];
    let totalRev = 0;
    let ticketRev = 0;
    let foodRev = 0;
    
    let pending = 0;
    let confirmed = 0;
    let completed = 0;
    let cancelled = 0;
    let expired = 0;
    let refunded = 0;

    list.forEach(b => {
      totalRev += (b.finalAmount || 0);
      ticketRev += (b.ticketAmount || 0);
      foodRev += (b.foodAmount || 0);

      switch (b.bookingStatus) {
        case 'PENDING_PAYMENT': pending++; break;
        case 'CONFIRMED': confirmed++; break;
        case 'COMPLETED': completed++; break;
        case 'CANCELLED': cancelled++; break;
        case 'EXPIRED': expired++; break;
        case 'REFUNDED': refunded++; break;
        default: break;
      }
    });

    return {
      totalRevenue: totalRev,
      ticketRevenue: ticketRev,
      foodRevenue: foodRev,
      pending,
      confirmed,
      completed,
      cancelled,
      expired,
      refunded,
      totalCount: list.length
    };
  }, [bookingPage]);

  // Generate Booking Trend Data for custom SVG curve (7 days back)
  const trendPoints = useMemo(() => {
    const list = bookingPage?.content || [];
    const dateMap = {};
    
    // Seed last 7 days
    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      const dateStr = d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
      dateMap[dateStr] = 0;
    }

    list.forEach(b => {
      const dateStr = new Date(b.createdAt).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
      if (dateMap[dateStr] !== undefined) {
        dateMap[dateStr]++;
      }
    });

    return Object.keys(dateMap).map((date, index) => ({
      x: (index * 80) + 40,
      y: 150 - (dateMap[date] * 12), // scale booking count
      label: date,
      value: dateMap[date]
    }));
  }, [bookingPage]);

  const handleCopyCode = (code) => {
    navigator.clipboard.writeText(code);
    if (triggerToast) triggerToast(`Đã sao chép mã đặt vé: ${code}`, 'info');
    else alert(`Đã sao chép: ${code}`);
  };

  const handleCancelBooking = async (publicId, bookingCode) => {
    const confirmMessage = `Bạn có chắc chắn muốn hủy đơn hàng ${bookingCode} không? Tất cả ghế và vé đi kèm sẽ bị giải phóng.`;
    const shouldCancel = triggerConfirm 
      ? await triggerConfirm(confirmMessage) 
      : window.confirm(confirmMessage);

    if (!shouldCancel) return;

    try {
      await updateBookingStatus(publicId, 'CANCELLED', 'Admin dashboard manual cancellation');
      if (triggerToast) triggerToast(`Hủy đơn đặt vé ${bookingCode} thành công.`, 'success');
      else alert('Đã hủy đơn thành công');
      fetchBookingsList();
      fetchSummaryStats();
    } catch (err) {
      const msg = parseApiError(err) || 'Không thể hủy đơn hàng';
      if (triggerToast) triggerToast(msg, 'error');
      else alert(msg);
    }
  };

  // Export filtered list to CSV
  const handleExportCSV = () => {
    if (processedBookings.length === 0) {
      if (triggerToast) triggerToast('Không có dữ liệu để xuất file', 'warning');
      return;
    }

    const headers = [
      'Mã đặt vé', 'Mã User', 'Phim', 'Rạp', 'Tiền Vé', 'Tiền Bắp Nước', 
      'Khuyến Mãi', 'Tổng Tiền', 'Trạng Thái Đơn', 'Thanh Toán', 'Ngày Tạo'
    ];

    const rows = processedBookings.map(b => [
      b.bookingCode,
      b.userId,
      moviesLookup[b.movieId] || `Phim #${b.movieId}`,
      cinemasLookup[b.cinemaId] || `Rạp #${b.cinemaId}`,
      b.ticketAmount,
      b.foodAmount,
      b.promotionDiscount + b.voucherDiscount,
      b.finalAmount,
      b.bookingStatus,
      b.paymentStatus,
      new Date(b.createdAt).toLocaleString('vi-VN')
    ]);

    const csvContent = "\ufeff" + [
      headers.join(','),
      ...rows.map(row => row.map(val => `"${String(val).replace(/"/g, '""')}"`).join(','))
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', `danh_sach_dat_ve_${new Date().toISOString().slice(0,10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Sort toggle handler
  const handleSort = (field) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('desc');
    }
  };

  // Column Resizing mouse actions
  const startResize = (colKey, e) => {
    resizingCol.current = colKey;
    startX.current = e.clientX;
    startWidth.current = colWidths[colKey];
    document.addEventListener('mousemove', doResize);
    document.addEventListener('mouseup', stopResize);
  };

  const doResize = (e) => {
    if (!resizingCol.current) return;
    const diff = e.clientX - startX.current;
    const newWidth = Math.max(70, startWidth.current + diff);
    setColWidths(prev => ({
      ...prev,
      [resizingCol.current]: newWidth
    }));
  };

  const stopResize = () => {
    resizingCol.current = null;
    document.removeEventListener('mousemove', doResize);
    document.removeEventListener('mouseup', stopResize);
  };

  const formatCurrency = (val) => {
    return (val || 0).toLocaleString('vi-VN') + 'đ';
  };

  const translateStatus = (bStatus) => {
    switch (bStatus) {
      case 'CONFIRMED': return 'Đã xác nhận';
      case 'PENDING_PAYMENT': return 'Chờ thanh toán';
      case 'COMPLETED': return 'Đã hoàn thành';
      case 'CANCELLED': return 'Đã hủy';
      case 'EXPIRED': return 'Hết hạn';
      case 'REFUNDED': return 'Hoàn tiền';
      default: return bStatus;
    }
  };

  const getStatusBadgeStyle = (bStatus) => {
    switch (bStatus) {
      case 'CONFIRMED':
        return 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20';
      case 'COMPLETED':
        return 'bg-blue-500/10 text-blue-400 border border-blue-500/20';
      case 'PENDING_PAYMENT':
        return 'bg-amber-500/10 text-amber-400 border border-amber-500/20';
      case 'CANCELLED':
        return 'bg-red-500/10 text-red-400 border border-red-500/20';
      case 'EXPIRED':
        return 'bg-zinc-800 text-zinc-500 border border-zinc-700';
      case 'REFUNDED':
        return 'bg-purple-500/10 text-purple-400 border border-purple-500/20';
      default:
        return 'bg-zinc-800 text-zinc-400';
    }
  };

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-screen bg-zinc-950 text-zinc-100 space-y-6 selection:bg-[#ff7a1a] selection:text-zinc-950">

      {/* Main Title Banner */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-zinc-850 pb-4">
        <div>
          <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">
            QUẢN LÝ ĐƠN HÀNG ĐẶT VÉ
          </h1>
          <p className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mt-1">
            Hệ thống quản lý, giám sát giao dịch, bắp nước, kiểm toán và cập nhật trạng thái hóa đơn đặt chỗ
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={handleExportCSV}
            className="bg-zinc-900 border border-zinc-850 hover:border-zinc-700 px-4 py-2 rounded-xl text-zinc-300 hover:text-white transition-all text-xs flex items-center gap-2 cursor-pointer font-bold"
          >
            <FileDown className="w-4 h-4 text-emerald-400" />
            <span>Xuất báo cáo (CSV)</span>
          </button>
          <button
            onClick={() => { fetchBookingsList(true); fetchSummaryStats(); }}
            disabled={refreshing}
            className="bg-zinc-900 border border-zinc-800 hover:border-zinc-700 p-2.5 rounded-xl text-zinc-400 hover:text-white transition-all text-xs flex items-center gap-2 cursor-pointer"
          >
            <RefreshCw className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} />
            <span>{refreshing ? 'Đang làm mới...' : 'Làm mới'}</span>
          </button>
        </div>
      </div>

      {/* Analytics Dashboard Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Left 2 Columns: KPI Summaries & charts */}
        <div className="lg:col-span-2 space-y-6">
          {/* KPI Metrics */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div className="bg-zinc-900/60 border border-zinc-850 rounded-2xl p-4 shadow-lg flex flex-col justify-between">
              <span className="text-[10px] text-zinc-500 font-bold uppercase block">Tổng đơn (Trang)</span>
              <span className="text-xl font-black text-white mt-1">{aggregatedStats.totalCount}</span>
              <span className="text-[9px] text-zinc-500 mt-1 block">Xác nhận: <span className="text-emerald-400 font-bold">{aggregatedStats.confirmed + aggregatedStats.completed}</span></span>
            </div>
            <div className="bg-zinc-900/60 border border-zinc-850 rounded-2xl p-4 shadow-lg flex flex-col justify-between">
              <span className="text-[10px] text-zinc-400 font-bold uppercase block text-amber-500">Chờ thanh toán</span>
              <span className="text-xl font-black text-amber-400 mt-1">{aggregatedStats.pending}</span>
              <span className="text-[9px] text-zinc-500 mt-1 block">Hết hạn: <span className="text-zinc-400 font-bold">{aggregatedStats.expired}</span></span>
            </div>
            <div className="bg-zinc-900/60 border border-zinc-850 rounded-2xl p-4 shadow-lg flex flex-col justify-between">
              <span className="text-[10px] text-zinc-400 font-bold uppercase block text-[#ff7a1a]">Doanh Thu (Trang)</span>
              <span className="text-xl font-black text-[#ff7a1a] mt-1 truncate">{formatCurrency(aggregatedStats.totalRevenue)}</span>
              <span className="text-[9px] text-zinc-500 mt-1 block">Bắp nước: <span className="text-amber-300 font-bold">{formatCurrency(aggregatedStats.foodRevenue)}</span></span>
            </div>
            <div className="bg-zinc-900/60 border border-zinc-850 rounded-2xl p-4 shadow-lg flex flex-col justify-between">
              <span className="text-[10px] text-zinc-400 font-bold uppercase block text-red-500">Hủy / Hoàn Tiền</span>
              <span className="text-xl font-black text-red-400 mt-1">{aggregatedStats.cancelled + aggregatedStats.refunded}</span>
              <span className="text-[9px] text-zinc-500 mt-1 block">Hoàn: <span className="text-purple-400 font-bold">{aggregatedStats.refunded}</span></span>
            </div>
          </div>

          {/* Booking Trend Custom SVG Curve Chart */}
          <div className="bg-zinc-900 border border-zinc-850 rounded-3xl p-5 md:p-6 shadow-xl space-y-4">
            <div className="flex justify-between items-center">
              <div>
                <h3 className="text-xs font-black uppercase tracking-wider text-white">Xu hướng Đặt Vé (Tuần này)</h3>
                <p className="text-[9px] text-zinc-550 font-bold mt-0.5">Thống kê số lượng đơn hàng mới phát sinh theo ngày</p>
              </div>
              <span className="text-[9px] bg-[#ff7a1a]/10 border border-[#ff7a1a]/30 text-[#ff7a1a] font-bold px-2 py-0.5 rounded uppercase">Biểu đồ động</span>
            </div>
            <div className="w-full overflow-x-auto select-none pt-2">
              <svg className="w-[600px] h-[180px] mx-auto overflow-visible">
                {/* Grid Lines */}
                <line x1="40" y1="30" x2="520" y2="30" stroke="#27272a" strokeDasharray="3" />
                <line x1="40" y1="70" x2="520" y2="70" stroke="#27272a" strokeDasharray="3" />
                <line x1="40" y1="110" x2="520" y2="110" stroke="#27272a" strokeDasharray="3" />
                <line x1="40" y1="150" x2="520" y2="150" stroke="#3f3f46" strokeWidth="1.5" />

                {/* Draw curve path */}
                {trendPoints.length > 1 && (
                  <path
                    d={`M ${trendPoints.map(p => `${p.x} ${p.y}`).join(' L ')}`}
                    fill="none"
                    stroke="#ff7a1a"
                    strokeWidth="3.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                )}

                {/* Connection points with popover values */}
                {trendPoints.map((p, i) => (
                  <g key={i} className="group cursor-pointer">
                    <circle
                      cx={p.x}
                      cy={p.y}
                      r="5.5"
                      fill="#09090b"
                      stroke="#ff7a1a"
                      strokeWidth="3.5"
                      className="transition-transform group-hover:scale-150"
                    />
                    {/* Tooltip showing amount */}
                    <g className="opacity-0 group-hover:opacity-100 transition-opacity duration-150 pointer-events-none">
                      <rect x={p.x - 20} y={p.y - 28} width="40" height="18" rx="4" fill="#ff7a1a" />
                      <text x={p.x} y={p.y - 16} fill="#000" fontSize="9" fontWeight="bold" textAnchor="middle">
                        {p.value} đơn
                      </text>
                    </g>
                    {/* Date label */}
                    <text x={p.x} y="170" fill="#71717a" fontSize="9" fontWeight="bold" textAnchor="middle">
                      {p.label}
                    </text>
                  </g>
                ))}
              </svg>
            </div>
          </div>
        </div>

        {/* Right 1 Column: System Monitoring Summary Panel */}
        <div className="bg-zinc-900 border border-zinc-850 rounded-3xl p-5 md:p-6 shadow-xl flex flex-col justify-between space-y-4">
          <div>
            <div className="border-b border-zinc-800 pb-3 flex items-center justify-between">
              <span className="text-xs font-black uppercase tracking-wider text-white">Giám Sát Hệ Thống</span>
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-ping"></span>
            </div>
            <p className="text-[9px] text-zinc-550 font-semibold mt-2">
              Dữ liệu tải từ API Monitoring với cơ chế Cache 10s TTL chống quá tải
            </p>
          </div>

          <div className="space-y-3.5 flex-1 justify-center flex flex-col">
            <div className="flex justify-between items-center border-b border-zinc-850 pb-2.5">
              <span className="text-xs text-zinc-400 flex items-center gap-2">
                <History className="w-4 h-4 text-emerald-400 shrink-0" />
                <span>Đặt vé hôm nay (Hồ Chí Minh)</span>
              </span>
              <span className="text-sm font-black text-white">{monitoringSummary?.bookingToday ?? '0'}</span>
            </div>
            <div className="flex justify-between items-center border-b border-zinc-850 pb-2.5">
              <span className="text-xs text-zinc-400 flex items-center gap-2">
                <XCircle className="w-4 h-4 text-red-500 shrink-0" />
                <span>Giao dịch lỗi (Thanh toán hỏng)</span>
              </span>
              <span className="text-sm font-black text-red-400">{monitoringSummary?.paymentFailed ?? '0'}</span>
            </div>
            <div className="flex justify-between items-center border-b border-zinc-850 pb-2.5">
              <span className="text-xs text-zinc-400 flex items-center gap-2">
                <Clock className="w-4 h-4 text-amber-500 shrink-0" />
                <span>Số lượng đơn hết hạn chờ</span>
              </span>
              <span className="text-sm font-black text-amber-400">{monitoringSummary?.expiredBooking ?? '0'}</span>
            </div>
            <div className="flex justify-between items-center pb-1">
              <span className="text-xs text-zinc-400 flex items-center gap-2">
                <RefreshCw className="w-4 h-4 text-sky-400 shrink-0" />
                <span>Tiến trình đồng bộ bù lỗi (Kafka)</span>
              </span>
              <span className="text-sm font-black text-sky-400">{monitoringSummary?.pendingRetry ?? '0'} tác vụ</span>
            </div>
          </div>

          <div className="pt-2 text-[9px] text-zinc-500 border-t border-zinc-850 italic text-center">
            Hạ tầng Microservices kết nối Kafka Outbox & Redis Lock
          </div>
        </div>
      </div>

      {/* Advanced Search & Filtering form */}
      <form onSubmit={handleSearchSubmit} className="bg-zinc-900 border border-zinc-850 rounded-2xl p-5 space-y-4 shadow-xl">
        <div className="flex justify-between items-center border-b border-zinc-800 pb-3">
          <div className="flex items-center gap-2">
            <SlidersHorizontal className="w-4 h-4 text-[#ff7a1a]" />
            <span className="text-xs font-black uppercase tracking-wider text-white">Bộ lọc tra cứu chuyên sâu</span>
          </div>
          <button
            type="button"
            onClick={() => setAdvancedOpen(!advancedOpen)}
            className="text-zinc-500 hover:text-white text-[10px] uppercase font-bold tracking-widest transition-colors flex items-center gap-1"
          >
            <span>{advancedOpen ? 'Thu gọn' : 'Mở rộng'}</span>
            <span>{advancedOpen ? '▲' : '▼'}</span>
          </button>
        </div>

        {/* Primary Filter Row */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
          <div className="space-y-1">
            <label className="text-[9px] text-zinc-500 font-bold uppercase">Mã đặt vé (Booking Code)</label>
            <input
              type="text"
              placeholder="Nhập mã đặt vé..."
              value={bookingCode}
              onChange={(e) => setBookingCode(e.target.value)}
              className="w-full bg-[#050506] border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-[#ff7a1a]/40 text-zinc-200"
            />
          </div>

          <div className="space-y-1">
            <label className="text-[9px] text-zinc-500 font-bold uppercase">Mã khách hàng (User ID)</label>
            <input
              type="number"
              placeholder="VD: 1, 2"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              className="w-full bg-[#050506] border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-[#ff7a1a]/40 text-zinc-200"
            />
          </div>

          <div className="space-y-1">
            <label className="text-[9px] text-zinc-500 font-bold uppercase">Trạng thái đặt vé</label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              className="w-full bg-[#050506] border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-[#ff7a1a]/40 text-zinc-200 outline-none"
            >
              <option value="ALL">Tất cả</option>
              <option value="PENDING_PAYMENT">Chờ thanh toán</option>
              <option value="CONFIRMED">Đã xác nhận (Paid)</option>
              <option value="COMPLETED">Đã hoàn thành</option>
              <option value="CANCELLED">Đã hủy</option>
              <option value="EXPIRED">Đã hết hạn</option>
              <option value="REFUNDED">Hoàn tiền</option>
            </select>
          </div>

          <div className="space-y-1">
            <label className="text-[9px] text-zinc-500 font-bold uppercase">Sắp xếp theo</label>
            <div className="flex gap-2">
              <select
                value={sortField}
                onChange={(e) => setSortField(e.target.value)}
                className="w-full bg-[#050506] border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-[#ff7a1a]/40 text-zinc-200 outline-none"
              >
                <option value="createdAt">Ngày tạo</option>
                <option value="bookingCode">Mã đặt vé</option>
                <option value="finalAmount">Tổng tiền</option>
                <option value="bookingStatus">Trạng thái</option>
              </select>
              <button
                type="button"
                onClick={() => setSortDirection(prev => prev === 'asc' ? 'desc' : 'asc')}
                className="bg-[#050506] border border-zinc-800 p-2 rounded-xl text-zinc-400 hover:text-white transition-colors"
                title="Đổi chiều sắp xếp"
              >
                <ArrowUpDown className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>

        {/* Collapsible Advanced Filters Section */}
        {advancedOpen && (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 pt-3 border-t border-zinc-800/40 animate-fade-in">
            {/* Filter by Movie */}
            <div className="space-y-1">
              <label className="text-[9px] text-zinc-500 font-bold uppercase">Phim chiếu</label>
              <select
                value={selectedMovieId}
                onChange={(e) => setSelectedMovieId(e.target.value)}
                className="w-full bg-[#050506] border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-[#ff7a1a]/40 text-zinc-200 outline-none"
              >
                <option value="ALL">Tất cả phim</option>
                {moviesList.map(m => (
                  <option key={m.id} value={m.id}>{m.movieTitle}</option>
                ))}
              </select>
            </div>

            {/* Filter by Cinema */}
            <div className="space-y-1">
              <label className="text-[9px] text-zinc-500 font-bold uppercase">Cụm rạp</label>
              <select
                value={selectedCinemaId}
                onChange={(e) => setSelectedCinemaId(e.target.value)}
                className="w-full bg-[#050506] border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-[#ff7a1a]/40 text-zinc-200 outline-none"
              >
                <option value="ALL">Tất cả cụm rạp</option>
                {cinemasList.map(c => (
                  <option key={c.id} value={c.id}>{c.cinemaName}</option>
                ))}
              </select>
            </div>

            {/* Filter by Price Range */}
            <div className="space-y-1 col-span-1">
              <label className="text-[9px] text-zinc-500 font-bold uppercase">Khoảng giá (VNĐ)</label>
              <div className="flex items-center gap-1.5">
                <input
                  type="number"
                  placeholder="Từ"
                  value={minPrice}
                  onChange={(e) => setMinPrice(e.target.value)}
                  className="w-full bg-[#050506] border border-zinc-800 rounded-xl px-2.5 py-2 text-xs focus:outline-none focus:border-[#ff7a1a]/40 text-zinc-200"
                />
                <span className="text-zinc-600 text-xs">-</span>
                <input
                  type="number"
                  placeholder="Đến"
                  value={maxPrice}
                  onChange={(e) => setMaxPrice(e.target.value)}
                  className="w-full bg-[#050506] border border-zinc-800 rounded-xl px-2.5 py-2 text-xs focus:outline-none focus:border-[#ff7a1a]/40 text-zinc-200"
                />
              </div>
            </div>

            {/* Date range filters */}
            <div className="space-y-1 col-span-1">
              <label className="text-[9px] text-zinc-500 font-bold uppercase">Lập trong ngày</label>
              <div className="flex items-center gap-1.5">
                <input
                  type="date"
                  value={fromDate}
                  onChange={(e) => setFromDate(e.target.value)}
                  className="w-full bg-[#050506] border border-zinc-800 rounded-xl px-2 py-2 text-xs focus:outline-none focus:border-[#ff7a1a]/40 text-zinc-200"
                />
                <span className="text-zinc-600 text-xs">-</span>
                <input
                  type="date"
                  value={toDate}
                  onChange={(e) => setToDate(e.target.value)}
                  className="w-full bg-[#050506] border border-zinc-800 rounded-xl px-2 py-2 text-xs focus:outline-none focus:border-[#ff7a1a]/40 text-zinc-200"
                />
              </div>
            </div>

            {/* Filter by Payment Status */}
            <div className="space-y-1">
              <label className="text-[9px] text-zinc-500 font-bold uppercase">Giao dịch thanh toán</label>
              <select
                value={paymentStatusFilter}
                onChange={(e) => setPaymentStatusFilter(e.target.value)}
                className="w-full bg-[#050506] border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-[#ff7a1a]/40 text-zinc-200 outline-none"
              >
                <option value="ALL">Tất cả cổng</option>
                <option value="PENDING">Chờ xử lý (Pending)</option>
                <option value="SUCCESS">Thành công (Success)</option>
                <option value="FAILED">Thất bại (Failed)</option>
                <option value="REFUNDED">Hoàn tiền (Refunded)</option>
              </select>
            </div>

            {/* Filter by Food Ordered */}
            <div className="space-y-1">
              <label className="text-[9px] text-zinc-500 font-bold uppercase">Kèm bắp nước</label>
              <select
                value={hasFoodFilter}
                onChange={(e) => setHasFoodFilter(e.target.value)}
                className="w-full bg-[#050506] border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-[#ff7a1a]/40 text-zinc-200 outline-none"
              >
                <option value="ALL">Tất cả</option>
                <option value="YES">Đơn có mua bắp nước</option>
                <option value="NO">Đơn chỉ mua vé xem phim</option>
              </select>
            </div>

            {/* Filter by Promotion Code */}
            <div className="space-y-1">
              <label className="text-[9px] text-zinc-500 font-bold uppercase">Sử dụng khuyến mãi</label>
              <select
                value={hasPromotionFilter}
                onChange={(e) => setHasPromotionFilter(e.target.value)}
                className="w-full bg-[#050506] border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-[#ff7a1a]/40 text-zinc-200 outline-none"
              >
                <option value="ALL">Tất cả</option>
                <option value="YES">Có áp dụng mã giảm</option>
                <option value="NO">Giữ nguyên giá gốc</option>
              </select>
            </div>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={handleResetFilters}
            className="px-4 py-2 border border-zinc-800 text-zinc-400 hover:text-white rounded-xl text-xs font-bold transition-all cursor-pointer bg-transparent"
          >
            Nhập lại bộ lọc
          </button>

          <button
            type="submit"
            className="px-6 py-2 bg-[#ff7a1a] hover:bg-opacity-90 text-zinc-950 rounded-xl text-xs font-black tracking-wider uppercase transition-all shadow-md shadow-[#ff7a1a]/15 flex items-center gap-1.5 cursor-pointer"
          >
            <Search className="w-3.5 h-3.5 stroke-[3]" />
            <span>Áp dụng tìm kiếm</span>
          </button>
        </div>
      </form>

      {/* Main Data Table Panel */}
      <div className="bg-zinc-900 border border-zinc-850 rounded-2xl overflow-hidden shadow-2xl">
        <div className="overflow-x-auto w-full relative">
          <table className="w-full text-left text-xs border-collapse table-fixed">
            <thead>
              <tr className="bg-zinc-950/80 text-zinc-500 border-b border-zinc-800 uppercase tracking-widest text-[9px] font-black sticky top-0 z-10 backdrop-blur-md">
                
                {/* Code Column */}
                <th className="p-4 pl-6 relative" style={{ width: colWidths.code }}>
                  <div className="flex items-center gap-1.5 cursor-pointer select-none" onClick={() => handleSort('bookingCode')}>
                    <span>Mã đặt vé</span>
                    {sortField === 'bookingCode' && (sortDirection === 'asc' ? ' ▲' : ' ▼')}
                  </div>
                  <div onMouseDown={(e) => startResize('code', e)} className="absolute right-0 top-0 bottom-0 w-1.5 cursor-col-resize hover:bg-[#ff7a1a] opacity-0 hover:opacity-100 transition-opacity" />
                </th>

                {/* Customer Column */}
                <th className="p-4 relative" style={{ width: colWidths.customer }}>
                  <div className="flex items-center gap-1.5 cursor-pointer select-none" onClick={() => handleSort('userId')}>
                    <span>Khách</span>
                    {sortField === 'userId' && (sortDirection === 'asc' ? ' ▲' : ' ▼')}
                  </div>
                  <div onMouseDown={(e) => startResize('customer', e)} className="absolute right-0 top-0 bottom-0 w-1.5 cursor-col-resize hover:bg-[#ff7a1a] opacity-0 hover:opacity-100 transition-opacity" />
                </th>

                {/* Movie Column */}
                <th className="p-4 relative" style={{ width: colWidths.movie }}>
                  <div className="flex items-center gap-1.5 cursor-pointer select-none" onClick={() => handleSort('movieId')}>
                    <span>Phim</span>
                    {sortField === 'movieId' && (sortDirection === 'asc' ? ' ▲' : ' ▼')}
                  </div>
                  <div onMouseDown={(e) => startResize('movie', e)} className="absolute right-0 top-0 bottom-0 w-1.5 cursor-col-resize hover:bg-[#ff7a1a] opacity-0 hover:opacity-100 transition-opacity" />
                </th>

                {/* Cinema Column */}
                <th className="p-4 relative" style={{ width: colWidths.cinema }}>
                  <div className="flex items-center gap-1.5 cursor-pointer select-none" onClick={() => handleSort('cinemaId')}>
                    <span>Cụm rạp / Phòng</span>
                    {sortField === 'cinemaId' && (sortDirection === 'asc' ? ' ▲' : ' ▼')}
                  </div>
                  <div onMouseDown={(e) => startResize('cinema', e)} className="absolute right-0 top-0 bottom-0 w-1.5 cursor-col-resize hover:bg-[#ff7a1a] opacity-0 hover:opacity-100 transition-opacity" />
                </th>

                {/* Showtime Column */}
                <th className="p-4 relative" style={{ width: colWidths.showtime }}>
                  <span>Suất chiếu</span>
                  <div onMouseDown={(e) => startResize('showtime', e)} className="absolute right-0 top-0 bottom-0 w-1.5 cursor-col-resize hover:bg-[#ff7a1a] opacity-0 hover:opacity-100 transition-opacity" />
                </th>

                {/* Amounts Column */}
                <th className="p-4 relative" style={{ width: colWidths.amounts }}>
                  <div className="flex items-center gap-1.5 cursor-pointer select-none" onClick={() => handleSort('finalAmount')}>
                    <span>Chi tiết tiền vé</span>
                    {sortField === 'finalAmount' && (sortDirection === 'asc' ? ' ▲' : ' ▼')}
                  </div>
                  <div onMouseDown={(e) => startResize('amounts', e)} className="absolute right-0 top-0 bottom-0 w-1.5 cursor-col-resize hover:bg-[#ff7a1a] opacity-0 hover:opacity-100 transition-opacity" />
                </th>

                {/* Status Column */}
                <th className="p-4 relative" style={{ width: colWidths.status }}>
                  <div className="flex items-center gap-1.5 cursor-pointer select-none" onClick={() => handleSort('bookingStatus')}>
                    <span>Hóa đơn</span>
                    {sortField === 'bookingStatus' && (sortDirection === 'asc' ? ' ▲' : ' ▼')}
                  </div>
                  <div onMouseDown={(e) => startResize('status', e)} className="absolute right-0 top-0 bottom-0 w-1.5 cursor-col-resize hover:bg-[#ff7a1a] opacity-0 hover:opacity-100 transition-opacity" />
                </th>

                {/* Created Column */}
                <th className="p-4 relative" style={{ width: colWidths.created }}>
                  <div className="flex items-center gap-1.5 cursor-pointer select-none" onClick={() => handleSort('createdAt')}>
                    <span>Thời điểm lập</span>
                    {sortField === 'createdAt' && (sortDirection === 'asc' ? ' ▲' : ' ▼')}
                  </div>
                  <div onMouseDown={(e) => startResize('created', e)} className="absolute right-0 top-0 bottom-0 w-1.5 cursor-col-resize hover:bg-[#ff7a1a] opacity-0 hover:opacity-100 transition-opacity" />
                </th>

                {/* Actions Column */}
                <th className="p-4 text-center pr-6" style={{ width: colWidths.actions }}>
                  <span>Hành động</span>
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-850">
              {loading ? (
                <tr>
                  <td colSpan="9" className="p-0">
                    <SkeletonTable rows={size} columns={9} />
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan="9" className="p-12 text-center text-red-500">
                    <div className="flex flex-col items-center justify-center gap-2">
                      <AlertCircle className="w-10 h-10 opacity-70" />
                      <p className="font-bold text-sm">{error}</p>
                      <button
                        type="button"
                        onClick={() => fetchBookingsList()}
                        className="mt-3 px-4 py-1.5 bg-zinc-800 text-xs font-bold text-zinc-300 hover:text-white rounded-lg border border-zinc-700"
                      >
                        Thử tải lại
                      </button>
                    </div>
                  </td>
                </tr>
              ) : processedBookings.length > 0 ? (
                processedBookings.map(b => {
                  const movieName = moviesLookup[b.movieId] || `Mã phim: ${b.movieId}`;
                  const cinemaName = cinemasLookup[b.cinemaId] || `Cụm rạp: ${b.cinemaId}`;
                  const hasFood = b.foodAmount > 0;
                  const discount = (b.promotionDiscount || 0) + (b.voucherDiscount || 0);

                  return (
                    <tr key={b.id} className="border-b border-zinc-850 hover:bg-zinc-950/40 text-zinc-300 transition-all">
                      
                      {/* Code */}
                      <td className="p-4 pl-6 font-mono font-bold text-zinc-100 truncate">
                        <div className="flex items-center gap-1.5">
                          <button
                            type="button"
                            onClick={() => handleCopyCode(b.bookingCode)}
                            className="text-zinc-550 hover:text-[#ff7a1a] transition-colors"
                            title="Sao chép mã"
                          >
                            <Copy className="w-3 h-3" />
                          </button>
                          <span className="truncate">{b.bookingCode}</span>
                        </div>
                      </td>

                      {/* Customer */}
                      <td className="p-4 font-semibold text-zinc-400">
                        <div className="flex items-center gap-1">
                          <User className="w-3.5 h-3.5 text-zinc-650" />
                          <span className="truncate">KH #{b.userId}</span>
                        </div>
                      </td>

                      {/* Movie */}
                      <td className="p-4 font-bold text-zinc-200 truncate">
                        <div className="flex items-center gap-1">
                          <Film className="w-3.5 h-3.5 text-zinc-650 shrink-0" />
                          <span className="truncate" title={movieName}>{movieName}</span>
                        </div>
                      </td>

                      {/* Cinema / Room */}
                      <td className="p-4 font-semibold text-zinc-400 truncate">
                        <div className="flex items-center gap-1">
                          <Building className="w-3.5 h-3.5 text-zinc-650 shrink-0" />
                          <span className="truncate" title={`${cinemaName} | Phòng #${b.auditoriumId}`}>
                            {cinemaName} | P.{b.auditoriumId}
                          </span>
                        </div>
                      </td>

                      {/* Showtime */}
                      <td className="p-4 text-[10px] text-zinc-400">
                        <div className="flex flex-col">
                          <span className="font-bold text-zinc-300">Suất #{b.showtimeId}</span>
                          <span className="text-[9px] text-zinc-500 font-bold uppercase mt-0.5">Giữ vé: {new Date(b.expiresAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</span>
                        </div>
                      </td>

                      {/* Amounts */}
                      <td className="p-4 font-bold text-zinc-200">
                        <div className="flex flex-col text-[10px] space-y-0.5">
                          <div className="flex justify-between">
                            <span className="text-zinc-550 font-medium">Vé:</span>
                            <span>{formatCurrency(b.ticketAmount)}</span>
                          </div>
                          {hasFood && (
                            <div className="flex justify-between text-amber-300">
                              <span className="font-medium">Bắp nước:</span>
                              <span>+{formatCurrency(b.foodAmount)}</span>
                            </div>
                          )}
                          {discount > 0 && (
                            <div className="flex justify-between text-emerald-500">
                              <span className="font-medium">Giảm giá:</span>
                              <span>-{formatCurrency(discount)}</span>
                            </div>
                          )}
                          <div className="flex justify-between border-t border-zinc-800 pt-0.5 text-white font-extrabold">
                            <span>Phải trả:</span>
                            <span className="text-[#ff7a1a]">{formatCurrency(b.finalAmount)}</span>
                          </div>
                        </div>
                      </td>

                      {/* Booking Status / Payment Status */}
                      <td className="p-4">
                        <div className="flex flex-col gap-1 items-start">
                          <span className={`text-[8.5px] font-black px-2 py-0.5 rounded uppercase tracking-wider ${getStatusBadgeStyle(b.bookingStatus)}`}>
                            {translateStatus(b.bookingStatus)}
                          </span>
                          <span className={`text-[8px] font-bold px-1.5 py-0.5 rounded flex items-center gap-1 ${
                            b.paymentStatus === 'SUCCESS' ? 'text-emerald-500 bg-emerald-500/5' : 
                            b.paymentStatus === 'PENDING' ? 'text-amber-500 bg-amber-500/5' : 'text-red-500 bg-red-500/5'
                          }`}>
                            <CreditCard className="w-2.5 h-2.5" />
                            <span>{b.paymentStatus}</span>
                          </span>
                        </div>
                      </td>

                      {/* Created Time */}
                      <td className="p-4 text-zinc-500 text-[10px] font-semibold">
                        <div className="flex flex-col">
                          <span>{new Date(b.createdAt).toLocaleDateString('vi-VN')}</span>
                          <span className="text-[9px] text-zinc-650">{new Date(b.createdAt).toLocaleTimeString('vi-VN')}</span>
                        </div>
                      </td>

                      {/* Actions */}
                      <td className="p-4 text-center pr-6">
                        <div className="flex items-center justify-center gap-2">
                          <button
                            onClick={() => navigate(`/admin/bookings/${b.publicId}`)}
                            className="bg-zinc-950 border border-zinc-800 hover:border-brand-orange hover:text-white p-2 rounded-xl text-zinc-400 transition-all cursor-pointer flex items-center justify-center gap-1.5 text-[9px] uppercase font-bold"
                            title="Chi tiết đơn"
                          >
                            <Eye className="w-3.5 h-3.5" />
                            <span>Xem</span>
                          </button>

                          {(b.bookingStatus === 'PENDING_PAYMENT' || b.bookingStatus === 'CONFIRMED') && (
                            <button
                              onClick={() => handleCancelBooking(b.publicId, b.bookingCode)}
                              className="border border-red-950 bg-red-950/20 hover:border-red-500 hover:text-white p-2 rounded-xl text-red-400 transition-all cursor-pointer text-[9px] uppercase font-bold flex items-center gap-1"
                              title="Hủy vé"
                            >
                              <XCircle className="w-3.5 h-3.5" />
                              <span>Hủy</span>
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan="9" className="p-12 text-center text-zinc-500 italic">
                    Không tìm thấy đơn hàng nào khớp với các bộ lọc tìm kiếm
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Data Table Pagination Footer */}
        {!loading && bookingPage?.totalPages > 1 && (
          <div className="bg-zinc-950/50 p-4 border-t border-zinc-800 flex justify-between items-center text-xs">
            <span className="text-zinc-500 font-bold">
              Tổng số bản ghi: <span className="text-zinc-300">{bookingPage.totalElements}</span>
            </span>

            <div className="flex gap-2">
              <button
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
                className={`px-3 py-1.5 border rounded-xl font-bold transition-all ${
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
                className={`px-3 py-1.5 border rounded-xl font-bold transition-all ${
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
