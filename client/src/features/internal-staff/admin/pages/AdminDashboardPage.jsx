import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Activity,
  AlertCircle,
  AlertTriangle,
  Banknote,
  BellRing,
  Briefcase,
  CheckCircle2,
  Clock3,
  ExternalLink,
  Gauge,
  RefreshCw,
  RotateCcw,
  Ticket,
  TicketCheck,
  Users
} from 'lucide-react';
import { getBookingMonitoringSummary } from '@/features/booking/admin/services/adminBookingService';
import { getBookings } from '@/features/booking/admin/services/adminBookingService';
import { getAnalyticsDashboard } from '@/features/analytics/admin/services/analyticsAdminService';
import { getDashboard } from '@/features/internal-staff/admin/services/userAdminService';
import { ErrorState, LoadingState } from '@/components/common/ui/uiKit';

const number = value => new Intl.NumberFormat('vi-VN').format(Number(value) || 0);
const money = value => new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0
}).format(Number(value) || 0);

const todayKey = () => new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Ho_Chi_Minh',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit'
}).format(new Date());

const todayRange = () => {
  const date = todayKey();
  return {
    date,
    fromDate: `${date}T00:00:00+07:00`,
    toDate: `${date}T23:59:59.999+07:00`
  };
};

const formatTime = value => {
  if (!value) return '--:--';
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
};

const formatActivityDate = value => {
  if (!value) return 'Vừa cập nhật';
  const date = new Date(value);
  const day = new Intl.DateTimeFormat('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    day: '2-digit',
    month: '2-digit'
  }).format(date);
  return `Hôm nay, ${day} lúc ${formatTime(value)}`;
};

const bookingStatusLabels = {
  PENDING_PAYMENT: 'chờ thanh toán',
  CONFIRMED: 'đã xác nhận',
  COMPLETED: 'đã hoàn tất',
  CANCELLED: 'đã hủy',
  EXPIRED: 'đã hết hạn',
  REFUNDED: 'đã hoàn tiền'
};

const TONE_STYLES = {
  orange: 'border-orange-500/20 bg-orange-500/10 text-orange-400',
  emerald: 'border-emerald-500/20 bg-emerald-500/10 text-emerald-400',
  red: 'border-red-500/20 bg-red-500/10 text-red-400',
  amber: 'border-amber-500/20 bg-amber-500/10 text-amber-400',
  sky: 'border-sky-500/20 bg-sky-500/10 text-sky-400'
};

function SectionHeading({ icon: Icon, title, tone = 'orange', action }) {
  return (
    <div className="mb-4 flex items-center justify-between gap-3">
      <div className="flex items-center gap-2">
        <Icon className={`h-5 w-5 ${TONE_STYLES[tone].split(' ').at(-1)}`} />
        <h2 className="text-sm font-bold uppercase tracking-widest text-zinc-300">{title}</h2>
      </div>
      {action}
    </div>
  );
}

function KpiCard({ title, value, detail, icon: Icon, tone = 'orange', href }) {
  const content = (
    <div className="flex h-full items-center justify-between gap-4">
      <div className="min-w-0">
        <p className="text-[11px] font-bold uppercase tracking-wider text-zinc-500">{title}</p>
        <p className="mt-2 truncate text-2xl font-black text-white">{value}</p>
        <p className="mt-1 text-xs text-zinc-600">{detail}</p>
      </div>
      <div className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border ${TONE_STYLES[tone]}`}>
        <Icon className="h-5 w-5" aria-hidden="true" />
      </div>
    </div>
  );

  if (href) {
    return (
      <Link to={href} className="enterprise-card block min-h-[132px] hover:border-zinc-700 hover:bg-zinc-900">
        {content}
      </Link>
    );
  }

  return <div className="enterprise-card min-h-[132px]">{content}</div>;
}

function AlertCard({ tone, icon: Icon, title, message, href }) {
  return (
    <article className="flex items-start gap-3 rounded-xl border border-zinc-800 bg-zinc-950/60 p-4">
      <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border ${TONE_STYLES[tone]}`}>
        <Icon className="h-4 w-4" />
      </div>
      <div className="min-w-0 flex-1">
        <h3 className="text-sm font-semibold text-zinc-100">{title}</h3>
        <p className="mt-1 text-xs leading-5 text-zinc-500">{message}</p>
        {href && (
          <Link to={href} className="mt-3 inline-flex items-center gap-1 text-xs font-bold text-orange-400 hover:text-orange-300">
            Xem và xử lý <ExternalLink className="h-3.5 w-3.5" />
          </Link>
        )}
      </div>
    </article>
  );
}

function RecentBooking({ booking }) {
  const status = bookingStatusLabels[booking.bookingStatus] || 'đang cập nhật';
  const isPaymentFailed = booking.paymentStatus === 'FAILED';
  const Icon = isPaymentFailed ? AlertCircle : booking.bookingStatus === 'CONFIRMED' ? CheckCircle2 : Ticket;
  const tone = isPaymentFailed ? 'red' : booking.bookingStatus === 'CONFIRMED' ? 'emerald' : 'orange';
  const reference = booking.bookingCode || `#${booking.id || '—'}`;

  return (
    <li className="flex items-start gap-3 border-b border-zinc-800/80 py-4 first:pt-0 last:border-b-0 last:pb-0">
      <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border ${TONE_STYLES[tone]}`}>
        <Icon className="h-4 w-4" />
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-sm text-zinc-200">
          <span className="font-bold">Đơn {reference}</span> {isPaymentFailed ? 'thanh toán thất bại' : status}
        </p>
        <p className="mt-1 truncate text-xs text-zinc-500">
          {booking.movieTitle || 'Chưa có tên phim'} · {booking.cinemaName || 'Chưa có rạp'}
        </p>
      </div>
      <div className="shrink-0 text-right">
        <p className="text-xs font-semibold text-zinc-300">{money(booking.finalAmount)}</p>
        <time className="mt-1 block text-[11px] text-zinc-600">{formatActivityDate(booking.createdAt)}</time>
      </div>
    </li>
  );
}

export default function AdminDashboardView() {
  const [summary, setSummary] = useState(null);
  const [analytics, setAnalytics] = useState(null);
  const [recentBookings, setRecentBookings] = useState([]);
  const [userStats, setUserStats] = useState({ customers: 0, employees: 0, pendingPayrolls: 0 });
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [warning, setWarning] = useState(null);

  const loadData = useCallback(async (refresh = false) => {
    if (refresh) setRefreshing(true);
    else setLoading(true);
    setError(null);
    setWarning(null);

    const range = todayRange();
    const [bookingResult, userResult, analyticsResult, recentBookingsResult] = await Promise.allSettled([
      getBookingMonitoringSummary(),
      getDashboard(),
      getAnalyticsDashboard({ startDate: range.date, endDate: range.date }),
      getBookings({ fromDate: range.fromDate, toDate: range.toDate, page: 0, size: 6 })
    ]);

    setSummary(bookingResult.status === 'fulfilled' ? bookingResult.value : null);
    setAnalytics(analyticsResult.status === 'fulfilled' ? analyticsResult.value : null);
    setRecentBookings(
      recentBookingsResult.status === 'fulfilled'
        ? (recentBookingsResult.value?.content || [])
        : []
    );

    if (userResult.status === 'fulfilled') {
      const userData = userResult.value;
      setUserStats({
        customers: userData?.totalCustomers || 0,
        employees: userData?.totalEmployees || 0,
        pendingPayrolls: userData?.pendingPayrolls || 0
      });
    }

    const failedSources = [bookingResult, userResult, analyticsResult, recentBookingsResult]
      .filter(result => result.status === 'rejected').length;
    if (bookingResult.status === 'rejected' && userResult.status === 'rejected') {
      setError('Không thể tải dữ liệu giám sát hệ thống.');
    } else if (failedSources > 0) {
      setWarning('Một số dữ liệu vận hành tạm thời chưa khả dụng. Các phần còn lại vẫn được cập nhật.');
    }

    setLoading(false);
    setRefreshing(false);
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData();
  }, [loadData]);

  const todayKpi = useMemo(() => {
    const daily = analytics?.daily || [];
    const current = daily.find(item => item.statDate === todayKey()) || daily.at(-1) || {};
    const analyticsSummary = analytics?.summary || {};
    return {
      revenue: current.netRevenue ?? analyticsSummary.netRevenue,
      tickets: current.ticketCount ?? analyticsSummary.ticketCount,
      occupancy: current.occupancyRate ?? analyticsSummary.occupancyRate,
      orders: current.bookingCount ?? summary?.bookingToday
    };
  }, [analytics, summary]);

  const alerts = useMemo(() => {
    const items = [];
    if (Number(summary?.paymentFailed) > 0) {
      items.push({
        key: 'payment-failed',
        tone: 'red',
        icon: AlertTriangle,
        title: 'Thanh toán thất bại',
        message: `${number(summary.paymentFailed)} đơn cần được kiểm tra lại.`,
        href: '/admin/bookings'
      });
    }
    if (Number(summary?.expiredBooking) > 0) {
      items.push({
        key: 'expired-booking',
        tone: 'amber',
        icon: Clock3,
        title: 'Đơn đã hết hạn',
        message: `${number(summary.expiredBooking)} đơn chưa hoàn tất trong vòng đời đặt vé.`,
        href: '/admin/bookings'
      });
    }
    if (Number(summary?.pendingRetry) > 0) {
      items.push({
        key: 'pending-retry',
        tone: 'sky',
        icon: RotateCcw,
        title: 'Tác vụ chờ đồng bộ',
        message: `${number(summary.pendingRetry)} tác vụ đang chờ hệ thống thử lại.`,
        href: '/admin/analytics'
      });
    }

    (analytics?.alerts || [])
      .filter(item => !item.acknowledged && !item.resolved)
      .slice(0, 3)
      .forEach(item => items.push({
        key: `analytics-${item.id}`,
        tone: item.severity === 'CRITICAL' || item.severity === 'HIGH' ? 'red' : 'amber',
        icon: BellRing,
        title: item.title || 'Cảnh báo phân tích',
        message: item.message || 'Có một điểm bất thường cần được xem xét.',
        href: '/admin/analytics'
      }));

    return items.slice(0, 5);
  }, [analytics, summary]);

  return (
    <div className="flex min-h-[400px] flex-1 flex-col space-y-8 overflow-auto bg-[#050506] p-6 text-white md:p-8">
      <header className="flex flex-col justify-between gap-4 border-b border-zinc-800 pb-5 lg:flex-row lg:items-end">
        <div>
          <h1 className="text-2xl font-black uppercase tracking-wider text-white">
            Trung tâm <span className="text-brand-orange">Điều hành</span>
          </h1>
          <p className="mt-1 text-sm text-zinc-500">
            Nắm bắt tình hình hôm nay và xử lý nhanh những việc cần chú ý.
          </p>
        </div>
        <button
          type="button"
          disabled={loading || refreshing}
          onClick={() => loadData(true)}
          className="flex items-center justify-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-5 py-2.5 text-sm font-bold text-zinc-300 transition-colors hover:bg-zinc-800 disabled:cursor-wait disabled:opacity-60"
        >
          <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin text-brand-orange' : ''}`} />
          {refreshing ? 'Đang đồng bộ...' : 'Đồng bộ dữ liệu'}
        </button>
      </header>

      {loading ? (
        <LoadingState message="Đang tải dữ liệu điều hành..." />
      ) : error ? (
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-8">
          <ErrorState message={error} onRetry={() => loadData()} />
        </div>
      ) : (
        <div className="space-y-8">
          {warning && (
            <div role="status" className="flex items-start gap-3 rounded-2xl border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-200">
              <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0" />
              <p>{warning}</p>
            </div>
          )}

          <section>
            <SectionHeading icon={Gauge} title="KPI hôm nay" action={<span className="text-xs text-zinc-600">Cập nhật theo dữ liệu mới nhất</span>} />
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <KpiCard title="Doanh thu thuần" value={money(todayKpi.revenue)} detail="Tổng tiền thực nhận hôm nay" icon={Banknote} href="/admin/analytics" />
              <KpiCard title="Vé đã bán" value={number(todayKpi.tickets)} detail="Số vé trong các đơn hôm nay" icon={Ticket} tone="emerald" href="/admin/bookings" />
              <KpiCard title="Công suất ghế" value={`${(Number(todayKpi.occupancy || 0) * 100).toFixed(1)}%`} detail="Tỷ lệ lấp đầy trung bình" icon={Gauge} tone="sky" href="/admin/analytics" />
              <KpiCard title="Đơn tạo hôm nay" value={number(todayKpi.orders)} detail="Tổng đơn mới trong ngày" icon={TicketCheck} tone="orange" href="/admin/bookings" />
            </div>
          </section>

          <section>
            <SectionHeading icon={AlertTriangle} title="Cảnh báo cần xử lý" tone="amber" action={<Link to="/admin/bookings" className="text-xs font-bold text-orange-400 hover:text-orange-300">Mở danh sách đơn</Link>} />
            {alerts.length ? (
              <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
                {alerts.map(({ key, ...alert }) => <AlertCard key={key} {...alert} />)}
              </div>
            ) : (
              <div className="flex items-center gap-3 rounded-2xl border border-emerald-500/20 bg-emerald-500/5 p-5 text-sm text-emerald-300">
                <CheckCircle2 className="h-5 w-5 shrink-0" />
                <span>Không có cảnh báo cần xử lý. Hệ thống đang vận hành ổn định.</span>
              </div>
            )}
          </section>

          <section>
            <SectionHeading icon={Activity} title="Hoạt động gần đây" tone="sky" action={<Link to="/admin/bookings" className="text-xs font-bold text-orange-400 hover:text-orange-300">Xem tất cả</Link>} />
            <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.5fr)_minmax(280px,0.8fr)]">
              <div className="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-5">
                {recentBookings.length ? (
                  <ul aria-label="Các booking gần đây">
                    {recentBookings.map(booking => <RecentBooking key={booking.publicId || booking.id} booking={booking} />)}
                  </ul>
                ) : (
                  <div className="flex min-h-40 items-center justify-center text-sm text-zinc-600">Chưa có hoạt động đặt vé hôm nay.</div>
                )}
              </div>
              <div className="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-5">
                <div className="mb-5 flex items-center justify-between">
                  <div>
                    <p className="text-xs font-bold uppercase tracking-wider text-zinc-500">Nhân sự & khách hàng</p>
                    <p className="mt-1 text-xs text-zinc-600">Tổng quan nguồn lực hệ thống</p>
                  </div>
                  <Users className="h-5 w-5 text-blue-400" />
                </div>
                <div className="space-y-4">
                  <div className="flex items-center justify-between border-b border-zinc-800/80 pb-3 text-sm">
                    <span className="flex items-center gap-2 text-zinc-500"><Users className="h-4 w-4" /> Khách hàng</span>
                    <strong className="text-zinc-200">{number(userStats.customers)}</strong>
                  </div>
                  <div className="flex items-center justify-between border-b border-zinc-800/80 pb-3 text-sm">
                    <span className="flex items-center gap-2 text-zinc-500"><Briefcase className="h-4 w-4" /> Nhân viên</span>
                    <strong className="text-zinc-200">{number(userStats.employees)}</strong>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <span className="flex items-center gap-2 text-zinc-500"><Banknote className="h-4 w-4" /> Bảng lương chờ duyệt</span>
                    <strong className={userStats.pendingPayrolls ? 'text-amber-300' : 'text-zinc-200'}>{number(userStats.pendingPayrolls)}</strong>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
