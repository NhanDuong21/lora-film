import { useCallback, useEffect, useMemo, useState } from 'react';
import { BarChart3, Film, RefreshCw, Ticket } from 'lucide-react';
import { getTopMoviesByRevenue } from '../services/adminAnalyticsService';
import { EmptyState, ErrorState, LoadingState } from '@/components/common/ui/uiKit';

const formatDate = date => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const resolveDateRange = range => {
  const end = new Date();
  const start = new Date(end);
  if (range === 'this_week') {
    start.setDate(end.getDate() - 6);
  } else if (range === 'this_month') {
    start.setDate(1);
  }
  return {
    startDate: formatDate(start),
    endDate: formatDate(end)
  };
};

const formatCurrency = (amount, currency = 'VND') => new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: currency || 'VND',
  maximumFractionDigits: 0
}).format(Number(amount) || 0);

export default function AdminFinancePage() {
  const [timeRange, setTimeRange] = useState('this_month');
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const loadReport = useCallback(async (refresh = false) => {
    if (refresh) setRefreshing(true);
    else setLoading(true);
    setError(null);
    try {
      const dateRange = resolveDateRange(timeRange);
      setReport(await getTopMoviesByRevenue({ ...dateRange, limit: 10 }));
    } catch {
      setReport(null);
      setError('Không thể tải báo cáo doanh thu từ Analytics Service.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [timeRange]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadReport();
  }, [loadReport]);

  const movies = useMemo(
    () => Array.isArray(report?.movies) ? report.movies : [],
    [report]
  );
  const maxRevenue = useMemo(
    () => Math.max(0, ...movies.map(movie => Number(movie.totalRevenue) || 0)),
    [movies]
  );

  return (
    <div className="mx-auto max-w-7xl space-y-6 p-6 pb-20 md:p-8">
      <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-white md:text-3xl">
            Báo cáo doanh thu
          </h1>
          <p className="mt-1 text-sm text-zinc-400">
            Dữ liệu xếp hạng phim được lấy trực tiếp từ Analytics Service.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <div className="flex rounded-xl border border-zinc-800 bg-zinc-900 p-1">
            {[
              ['today', 'Hôm nay'],
              ['this_week', '7 ngày qua'],
              ['this_month', 'Tháng này']
            ].map(([value, label]) => (
              <button
                key={value}
                type="button"
                onClick={() => setTimeRange(value)}
                className={`rounded-lg px-4 py-1.5 text-sm font-medium transition-all ${
                  timeRange === value
                    ? 'bg-amber-500/20 text-amber-400'
                    : 'text-zinc-400 hover:bg-zinc-800 hover:text-white'
                }`}
              >
                {label}
              </button>
            ))}
          </div>
          <button
            type="button"
            disabled={loading || refreshing}
            onClick={() => loadReport(true)}
            className="flex items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-2 text-sm font-medium text-zinc-300 transition-colors hover:border-zinc-700 hover:text-white disabled:cursor-wait disabled:opacity-60"
          >
            <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
            {refreshing ? 'Đang tải...' : 'Làm mới'}
          </button>
        </div>
      </div>

      <div className="rounded-2xl border border-zinc-800/80 bg-zinc-900/80 p-6">
        <div className="flex flex-col justify-between gap-2 border-b border-zinc-800 pb-5 sm:flex-row sm:items-center">
          <div>
            <h2 className="flex items-center gap-2 text-lg font-bold text-white">
              <Film className="h-5 w-5 text-amber-500" />
              Doanh thu theo phim
            </h2>
            <p className="mt-1 text-xs text-zinc-500">
              Xếp hạng theo dữ liệu tổng hợp hiện có của Analytics Service.
            </p>
          </div>
          {report?.lastUpdatedAt && (
            <span className="text-xs text-zinc-500">
              Analytics cập nhật: {new Date(report.lastUpdatedAt).toLocaleString('vi-VN')}
            </span>
          )}
        </div>

        {loading ? (
          <LoadingState message="Đang tải báo cáo từ Analytics Service..." />
        ) : error ? (
          <ErrorState message={error} onRetry={() => loadReport()} />
        ) : movies.length === 0 ? (
          <EmptyState
            icon={BarChart3}
            message="Chưa có dữ liệu doanh thu"
            description="Analytics Service chưa ghi nhận doanh thu phim trong khoảng thời gian đã chọn."
          />
        ) : (
          <div className="mt-6 space-y-5">
            {movies.map(movie => {
              const revenue = Number(movie.totalRevenue) || 0;
              const percentage = maxRevenue > 0 ? (revenue / maxRevenue) * 100 : 0;
              return (
                <article key={movie.movieId} className="space-y-2">
                  <div className="flex flex-col justify-between gap-2 text-sm sm:flex-row sm:items-center">
                    <div className="flex min-w-0 items-center gap-3">
                      <span className="w-7 text-xs font-black text-zinc-500">#{movie.rank}</span>
                      <span className="truncate font-semibold text-zinc-200">{movie.movieTitle}</span>
                    </div>
                    <div className="flex items-center gap-5 pl-10 text-zinc-400 sm:pl-0">
                      <span className="flex items-center gap-1 text-xs">
                        <Ticket className="h-3.5 w-3.5" />
                        {Number(movie.totalTicketsSold || 0).toLocaleString('vi-VN')} vé
                      </span>
                      <strong className="min-w-32 text-right text-white">
                        {formatCurrency(revenue, report.currency)}
                      </strong>
                    </div>
                  </div>
                  <div className="ml-10 h-2 overflow-hidden rounded-full bg-zinc-800">
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-amber-600 to-amber-400"
                      style={{ width: `${percentage}%` }}
                    />
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </div>

      <div className="rounded-2xl border border-dashed border-zinc-800 bg-zinc-900/30 px-6 py-8 text-center">
        <p className="text-sm font-bold text-zinc-300">Chưa có contract báo cáo tài chính tổng hợp</p>
        <p className="mx-auto mt-2 max-w-2xl text-xs leading-5 text-zinc-500">
          Các KPI tổng doanh thu, doanh thu bắp nước, tỷ lệ hoàn vé và danh sách giao dịch
          sẽ chỉ được bổ sung sau khi Analytics và Payment Service cung cấp API tương ứng.
        </p>
      </div>
    </div>
  );
}
