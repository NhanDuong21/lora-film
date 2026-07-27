import { useCallback, useEffect, useState } from 'react';
import {
  AlertTriangle,
  Clock3,
  RefreshCw,
  RotateCcw,
  TicketCheck
} from 'lucide-react';
import { getBookingMonitoringSummary } from '@/features/booking/admin/services/adminBookingService';
import { ErrorState, LoadingState } from '@/components/common/ui/uiKit';

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
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const loadSummary = useCallback(async (refresh = false) => {
    if (refresh) setRefreshing(true);
    else setLoading(true);
    setError(null);
    try {
      setSummary(await getBookingMonitoringSummary());
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
    loadSummary();
  }, [loadSummary]);

  return (
    <div className="flex min-h-[400px] flex-1 flex-col space-y-8 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <div className="flex flex-col justify-between gap-4 border-b border-zinc-800 pb-4 lg:flex-row lg:items-center">
        <div>
          <h1 className="text-xl font-black uppercase tracking-wider text-white md:text-2xl">
            Tổng quan vận hành
          </h1>
          <p className="mt-1 text-xs text-zinc-400">
            Chỉ hiển thị số liệu lấy trực tiếp từ Booking Monitoring API.
          </p>
        </div>
        <button
          type="button"
          disabled={loading || refreshing}
          onClick={() => loadSummary(true)}
          className="flex items-center justify-center gap-2 rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-2.5 text-xs font-bold text-zinc-300 transition-colors hover:border-zinc-700 hover:text-white disabled:cursor-wait disabled:opacity-60"
        >
          <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
          {refreshing ? 'Đang làm mới...' : 'Làm mới dữ liệu'}
        </button>
      </div>

      {loading ? (
        <LoadingState message="Đang tải dữ liệu vận hành..." />
      ) : error ? (
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/70">
          <ErrorState message={error} onRetry={() => loadSummary()} />
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
          {MONITORING_CARDS.map(card => {
            const Icon = card.icon;
            return (
              <section key={card.key} className="enterprise-card flex min-h-44 flex-col justify-between p-5">
                <div className={`w-fit rounded-2xl border p-3 ${card.iconBackground}`}>
                  <Icon className={`h-5 w-5 ${card.iconClass}`} />
                </div>
                <div className="mt-6">
                  <p className="text-xs font-bold uppercase tracking-wider text-zinc-500">
                    {card.title}
                  </p>
                  <p className="mt-1 text-3xl font-black text-zinc-100">
                    {summary?.[card.key] ?? '—'}
                  </p>
                  <p className="mt-2 text-[11px] leading-5 text-zinc-500">{card.description}</p>
                </div>
              </section>
            );
          })}
        </div>
      )}

      <div className="rounded-2xl border border-dashed border-zinc-800 bg-zinc-900/30 px-6 py-8 text-center">
        <p className="text-sm font-bold text-zinc-300">Chưa có nguồn dữ liệu dashboard tổng hợp</p>
        <p className="mx-auto mt-2 max-w-2xl text-xs leading-5 text-zinc-500">
          Doanh thu, vé bán, khách hàng mới và hoạt động gần đây sẽ chỉ xuất hiện khi có
          contract tổng hợp từ Analytics, Payment và User Service.
        </p>
      </div>
    </div>
  );
}
