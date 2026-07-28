import { useCallback, useEffect, useState } from 'react';
import {
  AlertTriangle,
  Clock3,
  RefreshCw,
  RotateCcw,
  TicketCheck,
  Users,
  Briefcase,
  Banknote,
  TrendingUp,
  Activity
} from 'lucide-react';
import { getBookingMonitoringSummary } from '@/features/booking/admin/services/adminBookingService';
import { getDashboard } from '@/features/internal-staff/admin/services/userAdminService';
import { ErrorState, LoadingState } from '@/components/common/ui/uiKit';
import AdminStatCard from '../components/AdminStatCard';

const MONITORING_CARDS = [
  {
    key: 'bookingToday',
    title: 'Đơn tạo hôm nay',
    description: 'Theo múi giờ Hồ Chí Minh',
    icon: TicketCheck,
    iconClass: 'text-emerald-400',
    iconBackground: 'border-emerald-500/20 bg-emerald-500/10'
  },
  {
    key: 'paymentFailed',
    title: 'Thanh toán thất bại',
    description: 'Tổng Booking có payment status FAILED',
    icon: AlertTriangle,
    iconClass: 'text-red-400',
    iconBackground: 'border-red-500/20 bg-red-500/10'
  },
  {
    key: 'expiredBooking',
    title: 'Đơn đã hết hạn',
    description: 'Tổng Booking ở trạng thái EXPIRED',
    icon: Clock3,
    iconClass: 'text-amber-400',
    iconBackground: 'border-amber-500/20 bg-amber-500/10'
  },
  {
    key: 'pendingRetry',
    title: 'Tác vụ chờ đồng bộ lại',
    description: 'Retry task đang ở trạng thái PENDING',
    icon: RotateCcw,
    iconClass: 'text-sky-400',
    iconBackground: 'border-sky-500/20 bg-sky-500/10'
  }
];

export default function AdminDashboardView() {
  const [summary, setSummary] = useState(null);
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
    try {
      const [bookingResult, userResult] = await Promise.allSettled([
        getBookingMonitoringSummary(),
        getDashboard()
      ]);

      setSummary(bookingResult.status === 'fulfilled' ? bookingResult.value : null);
      if (userResult.status === 'fulfilled') {
        const userData = userResult.value;
        setUserStats({
          customers: userData?.totalCustomers || 0,
          employees: userData?.totalEmployees || 0,
          pendingPayrolls: userData?.pendingPayrolls || 0
        });
      }

      if (bookingResult.status === 'rejected' && userResult.status === 'rejected') {
        setError('Không thể tải dữ liệu giám sát hệ thống.');
      } else if (bookingResult.status === 'rejected') {
        setWarning('Dữ liệu đặt vé tạm thời chưa khả dụng. Số liệu nhân sự vẫn được cập nhật.');
      } else if (userResult.status === 'rejected') {
        setWarning('Dữ liệu nhân sự tạm thời chưa khả dụng. Số liệu đặt vé vẫn được cập nhật.');
      }
    } catch {
      setSummary(null);
      setError('Không thể tải dữ liệu giám sát hệ thống.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData();
  }, [loadData]);

  return (
    <div className="flex min-h-[400px] flex-1 flex-col space-y-8 overflow-auto bg-[#050506] p-6 text-white md:p-8">
      <header className="flex flex-col justify-between gap-4 border-b border-zinc-800 pb-4 lg:flex-row lg:items-end">
        <div>
          <h1 className="text-2xl font-black uppercase tracking-wider text-white">
            Trung tâm <span className="text-brand-orange">Điều hành</span>
          </h1>
          <p className="mt-1 text-sm text-zinc-500">
            Giám sát vận hành hệ thống, nhân sự và khách hàng.
          </p>
        </div>
        <button
          type="button"
          disabled={loading || refreshing}
          onClick={() => loadData(true)}
          className="flex items-center justify-center gap-2 rounded-xl bg-zinc-900 px-5 py-2.5 text-sm font-bold text-zinc-300 transition-colors border border-zinc-700 hover:bg-zinc-800 disabled:cursor-wait disabled:opacity-60"
        >
          <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin text-brand-orange' : ''}`} />
          {refreshing ? 'Đang đồng bộ...' : 'Đồng bộ dữ liệu'}
        </button>
      </header>

      {loading ? (
        <LoadingState message="Đang tải dữ liệu tổng quan..." />
      ) : error ? (
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-8">
          <ErrorState message={error} onRetry={() => loadData()} />
        </div>
      ) : (
        <div className="space-y-8">
          {warning && (
            <div
              role="status"
              className="flex items-start gap-3 rounded-2xl border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-200"
            >
              <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0" />
              <p>{warning}</p>
            </div>
          )}
          {/* Nhân sự & Khách hàng */}
          <section>
            <div className="flex items-center gap-2 mb-4">
              <Activity className="w-5 h-5 text-brand-orange" />
              <h2 className="text-sm font-bold text-zinc-300 uppercase tracking-widest">Nhân sự & Khách hàng</h2>
            </div>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
              <AdminStatCard
                title="Khách hàng đăng ký" 
                value={userStats.customers.toLocaleString('vi-VN')} 
                description="Tổng số tài khoản khách hàng trên hệ thống" 
                icon={Users} 
                colorClass="border-blue-500/20 bg-blue-500/10 text-blue-400" 
                showTrend
              />
              <AdminStatCard
                title="Nhân viên nội bộ" 
                value={userStats.employees.toLocaleString('vi-VN')} 
                description="Tổng số nhân sự đang hoạt động" 
                icon={Briefcase} 
                colorClass="border-purple-500/20 bg-purple-500/10 text-purple-400" 
                showTrend
              />
              <AdminStatCard
                title="Bảng lương chờ duyệt" 
                value={userStats.pendingPayrolls.toLocaleString('vi-VN')} 
                description="Số lượng bảng lương cần xử lý" 
                icon={Banknote} 
                colorClass="border-amber-500/20 bg-amber-500/10 text-amber-400" 
                showTrend
              />
            </div>
          </section>

          {/* Booking Monitoring */}
          <section>
            <div className="flex items-center gap-2 mb-4">
              <Activity className="w-5 h-5 text-emerald-400" />
              <h2 className="text-sm font-bold text-zinc-300 uppercase tracking-widest">Giám sát Đặt vé (Booking)</h2>
            </div>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
              {MONITORING_CARDS.map(card => {
                const Icon = card.icon;
                return (
                  <div key={card.key} className="bg-zinc-900/30 border border-zinc-800/80 rounded-2xl p-5 flex items-center justify-between hover:bg-zinc-900/80 transition-colors">
                    <div>
                      <p className="text-[11px] font-bold uppercase tracking-wider text-zinc-500 mb-1">
                        {card.title}
                      </p>
                      <h3 className="text-2xl font-black text-white">
                        {summary?.[card.key] ?? '—'}
                      </h3>
                    </div>
                    <div className={`w-10 h-10 rounded-full flex items-center justify-center border ${card.iconBackground}`}>
                      <Icon className={`h-5 w-5 ${card.iconClass}`} />
                    </div>
                  </div>
                );
              })}
            </div>
          </section>

          <div className="rounded-2xl border border-dashed border-zinc-800 bg-zinc-900/30 px-6 py-8 text-center flex flex-col items-center justify-center gap-3">
            <div className="w-12 h-12 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center">
              <TrendingUp className="w-5 h-5 text-zinc-600" />
            </div>
            <div>
              <p className="text-sm font-bold text-zinc-300">Biểu đồ Doanh thu chưa khả dụng</p>
              <p className="mx-auto mt-2 max-w-xl text-xs leading-5 text-zinc-500">
                Doanh thu chi tiết, vé bán, và thống kê tăng trưởng sẽ được tích hợp đầy đủ khi module Analytics hoàn thiện.
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
