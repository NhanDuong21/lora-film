import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import { 
  Search, SlidersHorizontal, Eye, RefreshCw, ShoppingCart, 
  XCircle, AlertCircle, Copy, FileDown, ArrowUpDown, User,
  Film, Building, CreditCard
} from 'lucide-react';
import {
  getBookings,
  getBookingOperationsSummary,
  updateBookingStatus
} from '../services/adminBookingService';
import {
  getUserProfiles,
  searchUserProfiles
} from '@/features/auth/services/userService';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import { getBookingErrorMessage } from '../../customer/utils/bookingErrorMessages';
import SkeletonTable from '@/components/common/SkeletonTable';
import { EmptyState, StatusBadge } from '@/components/common/ui/uiKit';

export default function AdminBookingDashboardPage() {
  const navigate = useNavigate();
  const { triggerToast, triggerConfirm, triggerAlert } = useOutletContext() || {};

  // API Filter States (Sent to Backend)
  const [bookingCode, setBookingCode] = useState('');
  const [customerQuery, setCustomerQuery] = useState('');
  const [status, setStatus] = useState('ALL');
  const [attention, setAttention] = useState('ALL');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [page, setPage] = useState(0);
  const size = 10;

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
  const [operationsSummary, setOperationsSummary] = useState(null);
  const [summaryError, setSummaryError] = useState(null);
  const [moviesList, setMoviesList] = useState([]);
  const [cinemasList, setCinemasList] = useState([]);
  
  // Lookups
  const [moviesLookup, setMoviesLookup] = useState({});
  const [cinemasLookup, setCinemasLookup] = useState({});
  const [customersLookup, setCustomersLookup] = useState({});

  // Loading & Error States
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  // Resizable table column widths
  const [colWidths, setColWidths] = useState({
    code: 120,
    customer: 180,
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
      if (customerQuery.trim()) {
        const customerMatches = await searchUserProfiles(customerQuery, 50);
        const userIds = customerMatches.map(customer => customer.accountId);
        if (userIds.length === 0) {
          setBookingPage({
            content: [],
            page,
            size,
            totalElements: 0,
            totalPages: 0,
            last: true
          });
          setCustomersLookup({});
          return;
        }
        filters.userIds = userIds;
      }
      if (status !== 'ALL') filters.status = status;
      if (attention !== 'ALL') filters.attention = attention;
      if (fromDate) filters.fromDate = new Date(fromDate).toISOString();
      if (toDate) filters.toDate = new Date(toDate).toISOString();

      const response = await getBookings(filters);
      setBookingPage(response);
      const visibleUserIds = [...new Set(
        (response?.content || []).map(booking => booking.userId).filter(Boolean)
      )];
      try {
        const profiles = await getUserProfiles(visibleUserIds);
        setCustomersLookup(Object.fromEntries(
          profiles.map(profile => [profile.accountId, profile])
        ));
      } catch {
        setCustomersLookup({});
      }
    } catch (err) {
      setError(getBookingErrorMessage(err, "Không thể tải danh sách đơn hàng."));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [page, size, bookingCode, customerQuery, status, attention, fromDate, toDate]);

  const fetchOperationsSummary = useCallback(async () => {
    try {
      setSummaryError(null);
      setOperationsSummary(await getBookingOperationsSummary());
    } catch (err) {
      setSummaryError(getBookingErrorMessage(
        err, 'Không thể tải số liệu vận hành đơn hàng.'));
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchLookups();
    fetchOperationsSummary();
  }, [fetchLookups, fetchOperationsSummary]);

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
    setCustomerQuery('');
    setStatus('ALL');
    setAttention('ALL');
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

  const handleRefresh = async () => {
    await Promise.all([
      fetchBookingsList(true),
      fetchOperationsSummary()
    ]);
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
      list = list.filter(b => {
        if (paymentStatusFilter === 'NO_ATTEMPT') return !b.paymentAttempted;
        if (paymentStatusFilter === 'PROCESSING') {
          return b.paymentAttempted && b.paymentStatus === 'PENDING';
        }
        return b.paymentStatus === paymentStatusFilter;
      });
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

  const handleCopyCode = (code) => {
    navigator.clipboard.writeText(code);
    if (triggerToast) triggerToast(`Đã sao chép mã đặt vé: ${code}`, 'info');
    else if (triggerAlert) triggerAlert(`Đã sao chép mã đặt vé: ${code}`);
  };

  const handleCancelBooking = async (publicId, bookingCode) => {
    const confirmMessage = `Bạn có chắc chắn muốn hủy đơn hàng ${bookingCode} không? Tất cả ghế và vé đi kèm sẽ bị giải phóng.`;
    const shouldCancel = triggerConfirm 
      ? await triggerConfirm(confirmMessage) 
      : false;

    if (!shouldCancel) return;

    try {
      await updateBookingStatus(publicId, 'CANCELLED', 'Quản trị viên hủy đơn theo yêu cầu vận hành');
      if (triggerToast) triggerToast(`Hủy đơn đặt vé ${bookingCode} thành công.`, 'success');
      else if (triggerAlert) triggerAlert(`Hủy đơn đặt vé ${bookingCode} thành công.`);
      fetchBookingsList();
      fetchOperationsSummary();
    } catch (err) {
      const msg = getBookingErrorMessage(err, 'Không thể hủy đơn hàng.');
      if (triggerToast) triggerToast(msg, 'error');
      else if (triggerAlert) triggerAlert(msg);
    }
  };

  // Export filtered list to CSV
  const handleExportCSV = () => {
    if (processedBookings.length === 0) {
      if (triggerToast) triggerToast('Không có dữ liệu để xuất file', 'warning');
      return;
    }

    const headers = [
      'Mã đặt vé', 'Mã khách hàng', 'Tên khách hàng', 'Email', 'Phim', 'Rạp',
      'Tiền Vé', 'Tiền Bắp Nước',
      'Khuyến Mãi', 'Tổng Tiền', 'Trạng Thái Đơn', 'Thanh Toán', 'Ngày Tạo'
    ];

    const rows = processedBookings.map(b => [
      b.bookingCode,
      customersLookup[b.userId]?.customerCode || formatCustomerCode(b.userId),
      customersLookup[b.userId]?.fullName || 'Chưa tải được hồ sơ',
      customersLookup[b.userId]?.email || '',
      b.movieTitle || moviesLookup[b.movieId] || 'Chưa có tên phim',
      b.cinemaName || cinemasLookup[b.cinemaId] || 'Chưa có tên rạp',
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

  const formatCustomerCode = (userId) => (
    userId ? `KH${String(userId).padStart(6, '0')}` : 'Chưa có mã'
  );

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

  const translatePaymentStatus = (paymentStatus, paymentAttempted = false) => {
    if (!paymentAttempted) return 'Chưa phát sinh thanh toán';
    switch (paymentStatus) {
      case 'SUCCESS': return 'Đã thanh toán';
      case 'FAILED': return 'Thanh toán thất bại';
      case 'REFUNDED': return 'Đã hoàn tiền';
      case 'PENDING': return 'Đang xử lý thanh toán';
      default: return 'Chưa ghi nhận';
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
            Tra cứu đơn, kiểm tra ghế và thực hiện các thao tác vận hành được phép
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={handleExportCSV}
            className="bg-zinc-900 border border-zinc-850 hover:border-zinc-700 px-4 py-2 rounded-xl text-zinc-300 hover:text-white transition-all text-xs flex items-center gap-2 cursor-pointer font-bold"
          >
            <FileDown className="w-4 h-4 text-emerald-400" />
            <span>Xuất dữ liệu trang (CSV)</span>
          </button>
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            className="bg-zinc-900 border border-zinc-800 hover:border-zinc-700 p-2.5 rounded-xl text-zinc-400 hover:text-white transition-all text-xs flex items-center gap-2 cursor-pointer"
          >
            <RefreshCw className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} />
            <span>{refreshing ? 'Đang làm mới...' : 'Làm mới'}</span>
          </button>
        </div>
      </div>

      {/* Global counters are calculated by Booking Service, independent of pagination. */}
      {summaryError && (
        <div className="rounded-xl border border-amber-500/30 bg-amber-500/5 px-4 py-3 text-xs text-amber-300">
          {summaryError} Danh sách đơn bên dưới vẫn có thể tra cứu bình thường.
        </div>
      )}
      <div className="grid grid-cols-2 gap-4 xl:grid-cols-5">
        <div className="flex flex-col justify-between rounded-2xl border border-zinc-850 bg-zinc-900/60 p-4 shadow-lg">
          <span className="block text-[10px] font-bold uppercase text-zinc-500">Tổng đơn toàn hệ thống</span>
          <span className="mt-1 text-xl font-black text-white">{operationsSummary?.totalBookings ?? '—'}</span>
          <span className="mt-1 block text-[9px] text-zinc-500">
            Bộ lọc hiện tại: <strong className="text-zinc-300">{bookingPage?.totalElements ?? 0}</strong> đơn
          </span>
        </div>
        <div className="flex flex-col justify-between rounded-2xl border border-zinc-850 bg-zinc-900/60 p-4 shadow-lg">
          <span className="block text-[10px] font-bold uppercase text-amber-500">Đang chờ thanh toán</span>
          <span className="mt-1 text-xl font-black text-amber-400">{operationsSummary?.pendingPayment ?? '—'}</span>
          <span className="mt-1 block text-[9px] text-zinc-500">Tất cả đơn còn ở bước thanh toán</span>
        </div>
        <div className="flex flex-col justify-between rounded-2xl border border-zinc-850 bg-zinc-900/60 p-4 shadow-lg">
          <span className="block text-[10px] font-bold uppercase text-emerald-500">Đã xác nhận / hoàn thành</span>
          <span className="mt-1 text-xl font-black text-emerald-400">
            {(operationsSummary?.confirmed ?? 0) + (operationsSummary?.completed ?? 0)}
          </span>
          <span className="mt-1 block text-[9px] text-zinc-500">Ghế đã được ghi nhận là đã đặt</span>
        </div>
        <div className="flex flex-col justify-between rounded-2xl border border-zinc-850 bg-zinc-900/60 p-4 shadow-lg">
          <span className="block text-[10px] font-bold uppercase text-red-500">Không còn hoạt động</span>
          <span className="mt-1 text-xl font-black text-red-400">
            {(operationsSummary?.cancelled ?? 0)
              + (operationsSummary?.expired ?? 0)
              + (operationsSummary?.refunded ?? 0)}
          </span>
          <span className="mt-1 block text-[9px] text-zinc-500">
            Đã hủy, hết hạn hoặc hoàn tiền
          </span>
        </div>
        <button
          type="button"
          onClick={() => {
            setAttention('NEEDS_ATTENTION');
            setPage(0);
          }}
          className="flex flex-col justify-between rounded-2xl border border-orange-500/30 bg-orange-500/5 p-4 text-left shadow-lg transition-colors hover:border-orange-400"
        >
          <span className="block text-[10px] font-bold uppercase text-orange-400">Cần xử lý</span>
          <span className="mt-1 text-xl font-black text-orange-300">{operationsSummary?.needsAttention ?? '—'}</span>
          <span className="mt-1 block text-[9px] text-zinc-500">
            Sắp hết hạn, quá hạn hoặc thanh toán lỗi
          </span>
        </button>
      </div>

      {/* Advanced Search & Filtering form */}
      <form onSubmit={handleSearchSubmit} className="bg-zinc-900 border border-zinc-850 rounded-2xl p-5 space-y-4 shadow-xl">
        <div className="flex justify-between items-center border-b border-zinc-800 pb-3">
          <div className="flex items-center gap-2">
            <SlidersHorizontal className="w-4 h-4 text-[#ff7a1a]" />
            <span className="text-xs font-black uppercase tracking-wider text-white">Bộ lọc dữ liệu đơn hàng</span>
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
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-5 gap-4">
          <div className="space-y-1">
            <label className="text-[9px] text-zinc-500 font-bold uppercase">Mã đặt vé</label>
              <input
              type="text"
              placeholder="Nhập mã đặt vé..."
              value={bookingCode}
              onChange={(e) => setBookingCode(e.target.value)}
              className="w-full bg-zinc-900 border border-zinc-800 rounded-xl h-10 px-4 text-xs focus-ring text-zinc-100 placeholder:text-zinc-500"
            />
          </div>

          <div className="space-y-1">
            <label className="text-[9px] text-zinc-500 font-bold uppercase">Khách hàng</label>
              <input
              type="text"
              placeholder="Tên, email, SĐT hoặc KH000005"
              value={customerQuery}
              onChange={(e) => setCustomerQuery(e.target.value)}
              className="w-full bg-zinc-900 border border-zinc-800 rounded-xl h-10 px-4 text-xs focus-ring text-zinc-100 placeholder:text-zinc-500"
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
              <option value="CONFIRMED">Đã xác nhận thanh toán</option>
              <option value="COMPLETED">Đã hoàn thành</option>
              <option value="CANCELLED">Đã hủy</option>
              <option value="EXPIRED">Đã hết hạn</option>
              <option value="REFUNDED">Hoàn tiền</option>
            </select>
          </div>

          <div className="space-y-1">
            <label className="text-[9px] text-zinc-500 font-bold uppercase">Cần xử lý</label>
            <select
              value={attention}
              onChange={(e) => setAttention(e.target.value)}
              className="w-full bg-[#050506] border border-zinc-800 rounded-xl px-3 py-2 text-xs focus:outline-none focus:border-[#ff7a1a]/40 text-zinc-200 outline-none"
            >
              <option value="ALL">Tất cả</option>
              <option value="NEEDS_ATTENTION">Tất cả đơn cần xử lý</option>
              <option value="EXPIRING_SOON">Sắp hết thời gian giữ ghế</option>
              <option value="OVERDUE">Đã quá hạn nhưng chưa đóng</option>
              <option value="PAYMENT_FAILED">Có lần thanh toán thất bại</option>
            </select>
          </div>

          <div className="space-y-1">
            <label className="text-[9px] text-zinc-500 font-bold uppercase">Sắp xếp theo</label>
            <div className="flex gap-2">
              <select
                value={sortField}
                onChange={(e) => setSortField(e.target.value)}
                className="w-full bg-zinc-900 border border-zinc-800 rounded-xl h-10 px-4 text-xs focus-ring text-zinc-100 outline-none"
              >
                <option value="createdAt">Ngày tạo</option>
                <option value="bookingCode">Mã đặt vé</option>
                <option value="finalAmount">Tổng tiền</option>
                <option value="bookingStatus">Trạng thái</option>
              </select>
              <button
                type="button"
                onClick={() => setSortDirection(prev => prev === 'asc' ? 'desc' : 'asc')}
                className="bg-zinc-900 border border-zinc-800 px-3 rounded-xl text-zinc-400 hover:text-white hover:bg-zinc-800 transition-colors"
                title="Đổi chiều sắp xếp"
              >
                <ArrowUpDown className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>

        {/* Collapsible Advanced Filters Section */}
        {advancedOpen && (
          <>
            <p className="border-t border-zinc-800/40 pt-3 text-[10px] font-semibold text-amber-300/80">
              Các bộ lọc mở rộng bên dưới chỉ áp dụng cho dữ liệu của trang hiện tại.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 animate-fade-in">
            {/* Filter by Movie */}
            <div className="space-y-1">
              <label className="text-[9px] text-zinc-500 font-bold uppercase">Phim chiếu</label>
              <select
                value={selectedMovieId}
                onChange={(e) => setSelectedMovieId(e.target.value)}
                className="w-full bg-zinc-900 border border-zinc-800 rounded-xl h-10 px-4 text-xs focus-ring text-zinc-100 outline-none"
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
                <option value="ALL">Tất cả trạng thái</option>
                <option value="NO_ATTEMPT">Chưa phát sinh thanh toán</option>
                <option value="PROCESSING">Đang xử lý thanh toán</option>
                <option value="SUCCESS">Đã thanh toán</option>
                <option value="FAILED">Thanh toán thất bại</option>
                <option value="REFUNDED">Đã hoàn tiền</option>
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
          </>
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
                    <span>Giá trị đơn</span>
                    {sortField === 'finalAmount' && (sortDirection === 'asc' ? ' ▲' : ' ▼')}
                  </div>
                  <div onMouseDown={(e) => startResize('amounts', e)} className="absolute right-0 top-0 bottom-0 w-1.5 cursor-col-resize hover:bg-[#ff7a1a] opacity-0 hover:opacity-100 transition-opacity" />
                </th>

                {/* Status Column */}
                <th className="p-4 relative" style={{ width: colWidths.status }}>
                  <div className="flex items-center gap-1.5 cursor-pointer select-none" onClick={() => handleSort('bookingStatus')}>
                    <span>Trạng thái đơn</span>
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
                  const movieName = b.movieTitle || moviesLookup[b.movieId] || 'Chưa có tên phim';
                  const cinemaName = b.cinemaName || cinemasLookup[b.cinemaId] || 'Chưa có tên rạp';
                  const auditoriumName = b.auditoriumName || 'Chưa có tên phòng';
                  const showtimeStart = b.showtimeStart ? new Date(b.showtimeStart) : null;
                  const hasFood = b.foodAmount > 0;
                  const discount = (b.promotionDiscount || 0) + (b.voucherDiscount || 0);
                  const customer = customersLookup[b.userId];
                  const isOverdue = b.attentionCode === 'OVERDUE';
                  const isExpiringSoon = b.attentionCode === 'EXPIRING_SOON';

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
                        <div className="flex items-start gap-1.5">
                          <User className="mt-0.5 w-3.5 h-3.5 shrink-0 text-zinc-650" />
                          <div className="min-w-0">
                            <span className="block truncate text-zinc-200" title={customer?.fullName}>
                              {customer?.fullName || 'Chưa tải được hồ sơ'}
                            </span>
                            <span className="block truncate text-[9px] font-mono text-orange-400">
                              {customer?.customerCode || formatCustomerCode(b.userId)}
                            </span>
                            {customer?.email && (
                              <span className="block truncate text-[9px] font-normal text-zinc-500" title={customer.email}>
                                {customer.email}
                              </span>
                            )}
                          </div>
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
                          <span className="truncate" title={`${cinemaName} · ${auditoriumName}`}>
                            {cinemaName} · {auditoriumName}
                          </span>
                        </div>
                      </td>

                      {/* Showtime */}
                      <td className="p-4 text-[10px] text-zinc-400">
                        <div className="flex flex-col">
                          <span className="font-bold text-zinc-300">
                            {showtimeStart
                              ? `${showtimeStart.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })} · ${showtimeStart.toLocaleDateString('vi-VN')}`
                              : 'Chưa có giờ chiếu'}
                          </span>
                          <span className="mt-0.5 text-[9px] font-bold uppercase text-zinc-500">
                            {b.seatCount ? `${b.seatCount} ghế` : 'Chưa có số ghế'}
                            {b.expiresAt
                              ? ` · Giữ đến ${new Date(b.expiresAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}`
                              : ''}
                          </span>
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
                          <StatusBadge status={b.bookingStatus} label={translateStatus(b.bookingStatus)} />
                          <span className={`text-[8px] font-bold px-1.5 py-0.5 rounded flex items-center gap-1 mt-1 ${
                            b.paymentStatus === 'SUCCESS' ? 'text-emerald-500 bg-emerald-500/10' : 
                            !b.paymentAttempted ? 'text-zinc-400 bg-zinc-800' :
                            b.paymentStatus === 'PENDING' ? 'text-amber-500 bg-amber-500/10' : 'text-red-500 bg-red-500/10'
                          }`}>
                            <CreditCard className="w-2.5 h-2.5" />
                            <span>{translatePaymentStatus(b.paymentStatus, b.paymentAttempted)}</span>
                          </span>
                          {isOverdue && (
                            <span className="text-[8px] font-bold text-red-400">Quá hạn, cần kiểm tra</span>
                          )}
                          {isExpiringSoon && (
                            <span className="text-[8px] font-bold text-orange-400">Sắp hết thời gian giữ ghế</span>
                          )}
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

                          {b.bookingStatus === 'PENDING_PAYMENT' && (
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
                  <td colSpan="9" className="p-8">
                    <EmptyState 
                      icon={ShoppingCart} 
                      message="Không tìm thấy đơn hàng" 
                      description="Không tìm thấy đơn hàng nào khớp với các bộ lọc tìm kiếm." 
                    />
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
