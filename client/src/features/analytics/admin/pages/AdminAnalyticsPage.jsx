import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Activity,
  AlertTriangle,
  BarChart3,
  Building2,
  CalendarDays,
  Check,
  CheckCircle2,
  CircleDollarSign,
  Clock3,
  Film,
  Gauge,
  Lightbulb,
  RefreshCw,
  Search,
  Target,
  Ticket,
  TrendingUp,
  X
} from 'lucide-react';
import { ErrorState, LoadingState } from '@/components/common/ui/uiKit';
import {
  acknowledgeAnalyticsAlert,
  getAnalyticsDashboard,
  getCinemaDirectory,
  getCinemaKpis,
  updateAnalyticsRecommendation
} from '../services/analyticsAdminService';

const formatIsoDate = date => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const dateRangeFor = days => {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - days + 1);
  return { startDate: formatIsoDate(start), endDate: formatIsoDate(end) };
};

const DATE_RANGE_OPTIONS = [
  { days: 1, label: 'Hôm nay' },
  { days: 7, label: '7 ngày' },
  { days: 30, label: '30 ngày' },
  { days: 90, label: '90 ngày' }
];

const DECISION_VIEWS = [
  {
    value: 'happened',
    eyebrow: '01 · Kết quả',
    label: 'Đã xảy ra gì?',
    description: 'Doanh thu và hiệu quả'
  },
  {
    value: 'why',
    eyebrow: '02 · Nguyên nhân',
    label: 'Vì sao?',
    description: 'Điểm bất thường cần chú ý'
  },
  {
    value: 'next',
    eyebrow: '03 · Dự báo',
    label: 'Sắp xảy ra gì?',
    description: 'Doanh thu trong thời gian tới'
  },
  {
    value: 'action',
    eyebrow: '04 · Hành động',
    label: 'Nên làm gì?',
    description: 'Việc cần xử lý tiếp theo'
  }
];

const money = value => new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0
}).format(Number(value) || 0);

const number = value => new Intl.NumberFormat('vi-VN').format(Number(value) || 0);
const percent = value => `${((Number(value) || 0) * 100).toFixed(1)}%`;
const score = value => Math.round(Number(value) || 0);

const HEALTH_LABELS = {
  HEALTHY: 'Khỏe mạnh',
  STABLE: 'Ổn định',
  AT_RISK: 'Có rủi ro',
  CRITICAL: 'Nghiêm trọng'
};

const METRIC_LABELS = {
  NET_REVENUE: 'Doanh thu thuần',
  REVENUE: 'Doanh thu',
  TICKET_COUNT: 'Nhu cầu vé',
  TICKET: 'Nhu cầu vé',
  REFUND_RATE: 'Tỷ lệ hoàn tiền',
  OCCUPANCY_RATE: 'Công suất ghế',
  OCCUPANCY: 'Công suất ghế'
};

const PRIORITY_LABELS = {
  URGENT: 'Khẩn cấp',
  HIGH: 'Ưu tiên cao',
  MEDIUM: 'Ưu tiên vừa',
  LOW: 'Ưu tiên thấp'
};

const STATUS_LABELS = {
  SUCCESS: 'Thành công',
  FAILED: 'Gặp lỗi',
  RUNNING: 'Đang xử lý',
  NEVER_RUN: 'Chưa từng cập nhật',
  FRESH: 'Dữ liệu mới',
  DEGRADED: 'Cập nhật chậm',
  STALE: 'Dữ liệu đã cũ',
  NO_DATA: 'Chưa có dữ liệu',
  CRITICAL: 'Nghiêm trọng',
  HIGH: 'Mức cao',
  WARNING: 'Cần chú ý',
  MEDIUM: 'Mức vừa',
  LOW: 'Mức thấp',
  INFO: 'Thông tin',
  PENDING: 'Chờ xử lý',
  ACCEPTED: 'Đang xử lý',
  COMPLETED: 'Đã hoàn tất',
  DISMISSED: 'Đã bỏ qua',
  ACKNOWLEDGED: 'Đã ghi nhận',
  HEALTHY: 'Khỏe mạnh',
  STABLE: 'Ổn định',
  AT_RISK: 'Có rủi ro'
};

const PIPELINE_LABELS = {
  SUCCESS: 'Lần tổng hợp gần nhất thành công',
  FAILED: 'Lần tổng hợp gần nhất gặp lỗi',
  RUNNING: 'Hệ thống đang tổng hợp dữ liệu',
  NEVER_RUN: 'Hệ thống chưa từng tổng hợp dữ liệu'
};

const DETECTION_LABELS = {
  Z_SCORE: 'So với mức thông thường',
  ROBUST_Z_SCORE: 'So với mức thông thường',
  WEEKDAY_ROBUST_MAD_56D_V2: 'So với cùng thứ của các tuần trước',
  SEASONAL_BASELINE: 'So với cùng thời điểm trước đó',
  IQR: 'So với khoảng dao động thường thấy'
};

const SERVICE_LABELS = {
  'analytics-service': 'Bộ phận dữ liệu',
  'booking-service': 'Bộ phận đơn hàng',
  'business-operations': 'Bộ phận kinh doanh',
  'movie-service': 'Bộ phận lịch chiếu',
  'payment-service': 'Bộ phận thanh toán',
  'promotion-service': 'Bộ phận ưu đãi'
};

const CAUSE_LABELS = {
  CINEMA_REVENUE_DECLINE: 'Doanh thu tại rạp giảm',
  CINEMA_REFUND_PRESSURE: 'Hoàn tiền tập trung tại rạp',
  CINEMA_LOW_OCCUPANCY: 'Tỷ lệ lấp đầy ghế tại rạp thấp',
  MISSING_EVENT_SNAPSHOT: 'Dữ liệu đầu vào còn thiếu',
  SYSTEM_METRIC_DEVIATION: 'Kết quả chung thay đổi ngoài mức thông thường'
};

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const UUID_IN_TEXT_PATTERN = /\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\b/gi;

const formatDisplayDate = value => {
  if (!value) return 'chưa xác định';
  const [year, month, day] = String(value).slice(0, 10).split('-');
  return year && month && day ? `${day}/${month}/${year}` : value;
};

const cinemaKeyOf = cinema => cinema?.cinemaKey
  || cinema?.publicId
  || cinema?.cinemaPublicId
  || cinema?.id;

const cinemaNameOf = cinema => cinema?.name || cinema?.cinemaName;

const isReadableCinemaName = (name, cinemaKey) => {
  const normalizedName = String(name || '').trim();
  return Boolean(normalizedName)
    && normalizedName !== String(cinemaKey || '').trim()
    && !UUID_PATTERN.test(normalizedName);
};

const resolveCinemaName = (cinemaKey, fallbackName, cinemaNameByKey) => {
  const directoryName = cinemaNameByKey.get(String(cinemaKey || ''));
  if (isReadableCinemaName(directoryName, cinemaKey)) return directoryName;
  if (isReadableCinemaName(fallbackName, cinemaKey)) return fallbackName;
  return 'Rạp chưa cập nhật tên';
};

const localizeAnalyticsText = (value, cinemaNameByKey) => {
  if (!value) return 'Chưa có mô tả.';
  let result = String(value);
  [...cinemaNameByKey.entries()]
    .sort(([left], [right]) => right.length - left.length)
    .forEach(([cinemaKey, cinemaName]) => {
      result = result.split(cinemaKey).join(cinemaName);
    });

  const replacements = {
    'Refund Rate': 'Tỷ lệ hoàn tiền',
    Occupancy: 'Tỷ lệ lấp đầy ghế',
    'anomaly score': 'mức độ bất thường',
    'root-cause factor': 'nguyên nhân',
    'root-cause': 'nguyên nhân',
    snapshot: 'thông tin chi tiết',
    Snapshot: 'Thông tin chi tiết',
    dimension: 'nhóm dữ liệu',
    Dimension: 'Nhóm dữ liệu',
    event: 'dữ liệu đầu vào',
    Event: 'Dữ liệu đầu vào',
    insight: 'kết quả phân tích',
    Insight: 'Kết quả phân tích',
    forecast: 'dự báo',
    Forecast: 'Dự báo',
    provider: 'đơn vị thanh toán',
    producer: 'hệ thống cung cấp dữ liệu',
    booking: 'đơn hàng',
    Booking: 'Đơn hàng',
    KPI: 'số liệu',
    'đường cơ sở': 'mức trung bình trước đó'
  };
  Object.entries(replacements).forEach(([source, replacement]) => {
    result = result.split(source).join(replacement);
  });
  return result.replace(UUID_IN_TEXT_PATTERN, 'rạp chưa cập nhật tên');
};

const statusTone = status => {
  if (['CRITICAL', 'FAILED', 'AT_RISK'].includes(status)) {
    return 'border-red-500/25 bg-red-500/10 text-red-300';
  }
  if (['HIGH', 'WARNING', 'DEGRADED'].includes(status)) {
    return 'border-amber-500/25 bg-amber-500/10 text-amber-300';
  }
  if (['SUCCESS', 'HEALTHY', 'FRESH', 'STABLE'].includes(status)) {
    return 'border-emerald-500/25 bg-emerald-500/10 text-emerald-300';
  }
  return 'border-zinc-700 bg-zinc-800/70 text-zinc-300';
};

function Badge({ value, label }) {
  return (
    <span className={`inline-flex rounded-full border px-2.5 py-1 text-[11px] font-semibold ${statusTone(value)}`}>
      {label || STATUS_LABELS[value] || 'Chưa xác định'}
    </span>
  );
}

function SectionTitle({ icon: Icon, eyebrow, title, description, aside }) {
  return (
    <header className="mb-5 flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
      <div>
        <p className="mb-2 flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-orange-400">
          <Icon className="h-4 w-4" /> {eyebrow}
        </p>
        <h2 className="text-xl font-semibold tracking-tight text-zinc-100">{title}</h2>
        {description && <p className="mt-1 text-sm text-zinc-500">{description}</p>}
      </div>
      {aside}
    </header>
  );
}

function MetricCard({ icon: Icon, label, value, detail }) {
  return (
    <article className="rounded-2xl border border-zinc-800 bg-zinc-900/55 p-5">
      <div className="mb-6 flex items-center justify-between">
        <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-zinc-800 text-zinc-300">
          <Icon className="h-4 w-4" />
        </span>
        <span className="h-1.5 w-1.5 rounded-full bg-orange-400" />
      </div>
      <p className="text-xs font-medium text-zinc-500">{label}</p>
      <strong className="mt-2 block text-2xl font-semibold tracking-tight text-zinc-100">{value}</strong>
      <p className="mt-2 text-xs leading-5 text-zinc-600">{detail}</p>
    </article>
  );
}

function RevenueChart({ values = [] }) {
  const points = useMemo(() => {
    if (!values.length) return '';
    const revenues = values.map(item => Number(item.netRevenue) || 0);
    const min = Math.min(...revenues);
    const max = Math.max(...revenues);
    const spread = max - min || 1;
    return revenues.map((value, index) => {
      const x = values.length === 1 ? 50 : (index / (values.length - 1)) * 100;
      const y = 88 - ((value - min) / spread) * 70;
      return `${x},${y}`;
    }).join(' ');
  }, [values]);

  if (!values.length) {
    return (
      <div className="flex min-h-64 items-center justify-center rounded-xl border border-dashed border-zinc-800">
        <p className="max-w-sm text-center text-sm leading-6 text-zinc-600">
          Chưa có số liệu trong khoảng đã chọn. Dữ liệu hôm nay được cập nhật mỗi phút khi có giao dịch mới.
        </p>
      </div>
    );
  }

  return (
    <div>
      <svg
        viewBox="0 0 100 100"
        preserveAspectRatio="none"
        className="h-64 w-full overflow-visible"
        role="img"
        aria-label="Xu hướng doanh thu thuần"
      >
        {[20, 40, 60, 80].map(y => (
          <line key={y} x1="0" x2="100" y1={y} y2={y} stroke="#27272a" strokeWidth="0.4" />
        ))}
        <polyline
          points={points}
          fill="none"
          stroke="#fb923c"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
          vectorEffect="non-scaling-stroke"
        />
      </svg>
      <div className="mt-2 flex justify-between text-[11px] text-zinc-600">
        <span>{values[0]?.statDate}</span>
        <span>{values.at(-1)?.statDate}</span>
      </div>
    </div>
  );
}

function HealthScore({ health, quality }) {
  const value = score(health?.overallScore);
  const components = [
    ['Doanh thu', health?.revenueScore],
    ['Nhu cầu', health?.demandScore],
    ['Công suất', health?.occupancyScore],
    ['Khách hàng', health?.customerScore],
    ['Vận hành', health?.operationalScore],
    ['Độ đầy đủ dữ liệu', health?.dataQualityScore]
  ];

  return (
    <section className="grid gap-6 rounded-2xl border border-zinc-800 bg-zinc-900/55 p-5 lg:grid-cols-[220px_1fr] lg:p-6">
      <div className="flex items-center gap-5 border-zinc-800 lg:flex-col lg:justify-center lg:border-r lg:pr-6">
        <div className="flex h-28 w-28 shrink-0 items-center justify-center rounded-full border-[10px] border-zinc-800">
          <div className="text-center">
            <strong className="block text-3xl font-semibold text-zinc-100">{value}</strong>
            <span className="text-[10px] uppercase tracking-wider text-zinc-600">/ 100</span>
          </div>
        </div>
        <div className="lg:text-center">
          <Badge
            value={health?.healthStatus}
            label={HEALTH_LABELS[health?.healthStatus] || 'Chưa tính'}
          />
          <p className="mt-2 text-xs text-zinc-600">
            Độ tin cậy {percent(health?.confidenceScore)}
          </p>
        </div>
      </div>
      <div>
        <div className="mb-5 flex items-start justify-between gap-4">
          <div>
            <h3 className="font-semibold text-zinc-100">Sức khỏe hoạt động toàn chuỗi</h3>
            <p className="mt-1 text-xs leading-5 text-zinc-500">
              Điểm tổng hợp từ doanh thu, nhu cầu, công suất, khách hàng và rủi ro vận hành.
            </p>
          </div>
          <Gauge className="h-5 w-5 shrink-0 text-orange-400" />
        </div>
        <div className="grid gap-x-6 gap-y-4 sm:grid-cols-2">
          {components.map(([label, component]) => (
            <div key={label}>
              <div className="mb-1.5 flex justify-between text-xs">
                <span className="text-zinc-500">{label}</span>
                <span className="font-medium text-zinc-300">{score(component)}</span>
              </div>
              <div className="h-1.5 overflow-hidden rounded-full bg-zinc-800">
                <div
                  className="h-full rounded-full bg-orange-400"
                  style={{ width: `${Math.min(100, score(component))}%` }}
                />
              </div>
            </div>
          ))}
        </div>
        <p className="mt-5 border-t border-zinc-800 pt-4 text-xs leading-5 text-zinc-600">
          Mức độ đầy đủ của dữ liệu: {percent(quality?.latestCompleteness)}.
          Điểm càng cao cho thấy số liệu toàn hệ thống càng đáng tin cậy.
        </p>
      </div>
    </section>
  );
}

function RankingTable({ rows = [], kind, cinemaNameByKey }) {
  const isMovie = kind === 'movie';
  return (
    <section className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/55">
      <header className="flex items-center gap-3 border-b border-zinc-800 px-5 py-4">
        {isMovie
          ? <Film className="h-4 w-4 text-orange-400" />
          : <Building2 className="h-4 w-4 text-orange-400" />}
        <h3 className="text-sm font-semibold text-zinc-200">
          {isMovie ? 'Phim dẫn đầu' : 'Hiệu suất rạp'}
        </h3>
      </header>
      <div className="divide-y divide-zinc-800/80">
        {rows.slice(0, 6).map((row, index) => (
          <article
            key={row.movieKey || row.cinemaKey}
            className="grid grid-cols-[28px_1fr_auto] items-center gap-3 px-5 py-4"
          >
            <span className="text-xs font-semibold text-zinc-600">{String(index + 1).padStart(2, '0')}</span>
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-zinc-200">
                {isMovie
                  ? row.movieTitle
                  : resolveCinemaName(row.cinemaKey, row.cinemaName, cinemaNameByKey)}
              </p>
              <p className="mt-1 text-[11px] text-zinc-600">
                {number(row.ticketCount)} vé · Lấp đầy {percent(row.occupancyRate)}
              </p>
            </div>
            <strong className="text-sm font-medium text-zinc-300">{money(row.netRevenue)}</strong>
          </article>
        ))}
        {!rows.length && (
          <p className="px-5 py-10 text-center text-sm text-zinc-600">Chưa có dữ liệu xếp hạng.</p>
        )}
      </div>
    </section>
  );
}

function EmptyState({ children }) {
  return (
    <div className="rounded-2xl border border-dashed border-zinc-800 px-6 py-12 text-center text-sm text-zinc-600">
      {children}
    </div>
  );
}

export default function AdminAnalyticsPage() {
  const [days, setDays] = useState(30);
  const [view, setView] = useState('happened');
  const [selectedCinemaKey, setSelectedCinemaKey] = useState('');
  const [cinemaKpis, setCinemaKpis] = useState([]);
  const [cinemaDirectory, setCinemaDirectory] = useState([]);
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [acting, setActing] = useState('');
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(async (refresh = false) => {
    if (refresh) setRefreshing(true);
    else setLoading(true);
    setError('');
    try {
      const period = dateRangeFor(days);
      const [dashboardData, cinemas] = await Promise.all([
        getAnalyticsDashboard({
          ...period,
          ...(selectedCinemaKey ? { cinemaKey: selectedCinemaKey } : {})
        }),
        getCinemaKpis({ ...period, limit: 100 })
      ]);
      const cinemaKeys = [
        ...cinemas.map(cinema => cinema.cinemaKey),
        dashboardData?.scope?.cinemaKey,
        selectedCinemaKey
      ];
      const directory = await getCinemaDirectory(cinemaKeys);
      setDashboard(dashboardData);
      setCinemaKpis(cinemas);
      setCinemaDirectory(directory);
    } catch (requestError) {
      setDashboard(null);
      setError(requestError?.message || 'Không thể tải dữ liệu Analytics.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [days, selectedCinemaKey]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const cinemaNameByKey = useMemo(() => {
    const names = new Map();
    cinemaDirectory.forEach(cinema => {
      const cinemaKey = cinemaKeyOf(cinema);
      const cinemaName = cinemaNameOf(cinema);
      if (cinemaKey && isReadableCinemaName(cinemaName, cinemaKey)) {
        names.set(String(cinemaKey), cinemaName);
      }
    });
    cinemaKpis.forEach(cinema => {
      const cinemaKey = cinemaKeyOf(cinema);
      const cinemaName = cinemaNameOf(cinema);
      if (cinemaKey && !names.has(String(cinemaKey))
          && isReadableCinemaName(cinemaName, cinemaKey)) {
        names.set(String(cinemaKey), cinemaName);
      }
    });
    return names;
  }, [cinemaDirectory, cinemaKpis]);

  const cinemaOptions = useMemo(() => {
    const optionsByKey = new Map();
    [...cinemaDirectory, ...cinemaKpis].forEach(cinema => {
      const cinemaKey = cinemaKeyOf(cinema);
      if (!cinemaKey) return;
      optionsByKey.set(String(cinemaKey), {
        cinemaKey,
        cinemaName: resolveCinemaName(cinemaKey, cinemaNameOf(cinema), cinemaNameByKey)
      });
    });
    return [...optionsByKey.values()]
      .sort((left, right) => left.cinemaName.localeCompare(right.cinemaName, 'vi'));
  }, [
    cinemaDirectory,
    cinemaKpis,
    cinemaNameByKey
  ]);

  const handleAlert = async id => {
    setActing(`alert-${id}`);
    setNotice('');
    try {
      await acknowledgeAnalyticsAlert(id);
      setNotice('Đã ghi nhận cảnh báo.');
      await load(true);
    } catch (requestError) {
      setNotice(requestError?.message || 'Không thể cập nhật cảnh báo.');
    } finally {
      setActing('');
    }
  };

  const handleRecommendation = async (id, status) => {
    setActing(`recommendation-${id}`);
    setNotice('');
    try {
      await updateAnalyticsRecommendation(id, status);
      setNotice(status === 'ACCEPTED'
        ? 'Đã nhận xử lý khuyến nghị.'
        : status === 'COMPLETED'
          ? 'Đã đánh dấu hoàn tất.'
          : 'Đã bỏ qua khuyến nghị.');
      await load(true);
    } catch (requestError) {
      setNotice(requestError?.message || 'Không thể cập nhật khuyến nghị.');
    } finally {
      setActing('');
    }
  };

  if (loading) {
    return <div className="p-8"><LoadingState message="Đang xây dựng bức tranh kinh doanh..." /></div>;
  }
  if (error) {
    return <div className="p-8"><ErrorState message={error} onRetry={() => load()} /></div>;
  }

  const summary = dashboard?.summary || {};
  const quality = dashboard?.dataQuality || {};
  const isCinemaScope = dashboard?.scope?.type === 'CINEMA';
  const scopeCinemaKey = dashboard?.scope?.cinemaKey || selectedCinemaKey;
  const scopeName = isCinemaScope
    ? resolveCinemaName(
        scopeCinemaKey,
        dashboard?.scope?.cinemaName,
        cinemaNameByKey
      )
    : 'Toàn hệ thống';
  const hasCinemaData = !isCinemaScope
    || Boolean(dashboard?.daily?.length)
    || Number(summary.bookingCount) > 0;
  const revenueForecasts = dashboard?.forecasts?.filter(item => item.forecastType === 'REVENUE') || [];

  return (
    <main className="min-h-full space-y-6 bg-[#070708] p-5 text-white md:p-8">
      <header className="flex flex-col justify-between gap-5 border-b border-zinc-800 pb-6 xl:flex-row xl:items-end">
        <div>
          <p className="mb-2 flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-orange-400">
            <Activity className="h-4 w-4" /> Báo cáo và phân tích kinh doanh
          </p>
          <h1 className="text-3xl font-semibold tracking-tight text-zinc-100">Trung tâm điều hành kinh doanh</h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-500">
            Theo dõi kết quả kinh doanh, tìm nguyên nhân thay đổi và xem những việc cần xử lý tiếp theo.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <label className="relative">
            <span className="sr-only">Chọn rạp phân tích</span>
            <Building2 className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500" />
            <select
              aria-label="Chọn rạp phân tích"
              value={selectedCinemaKey}
              onChange={event => setSelectedCinemaKey(event.target.value)}
              className="min-w-52 appearance-none rounded-xl border border-zinc-800 bg-zinc-900 py-2.5 pl-10 pr-8 text-xs font-medium text-zinc-200 outline-none transition focus:border-orange-500/50"
            >
              <option value="">Toàn hệ thống</option>
              {cinemaOptions.map(cinema => (
                <option key={cinema.cinemaKey} value={cinema.cinemaKey}>
                  {cinema.cinemaName}
                </option>
              ))}
            </select>
          </label>
          <div className="flex rounded-xl border border-zinc-800 bg-zinc-900 p-1">
            {DATE_RANGE_OPTIONS.map(option => (
              <button
                key={option.days}
                type="button"
                onClick={() => setDays(option.days)}
                aria-pressed={option.days === days}
                className={`rounded-lg px-3.5 py-2 text-xs font-medium transition ${
                  option.days === days
                    ? 'bg-zinc-100 text-zinc-950'
                    : 'text-zinc-500 hover:text-zinc-200'
                }`}
              >
                {option.label}
              </button>
            ))}
          </div>
          <button
            type="button"
            onClick={() => load(true)}
            disabled={refreshing}
            className="flex items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-2.5 text-xs font-medium text-zinc-300 transition hover:border-zinc-700 disabled:opacity-50"
          >
            <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
            Làm mới
          </button>
        </div>
      </header>

      <section className="flex items-center gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/40 px-4 py-3">
        <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-orange-500/10 text-orange-400">
          <Building2 className="h-4 w-4" />
        </span>
        <div>
          <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-zinc-600">
            Phạm vi đang xem
          </p>
          <p className="mt-0.5 text-sm font-medium text-zinc-200">{scopeName}</p>
        </div>
      </section>

      <section className="flex flex-col justify-between gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/40 px-4 py-3 sm:flex-row sm:items-center">
        <div className="flex items-center gap-3">
          <span className={`h-2 w-2 rounded-full ${
            quality.lastPipelineStatus === 'SUCCESS' ? 'bg-emerald-400' : 'bg-amber-400'
          }`} />
          <p className="text-xs text-zinc-400">
            Số liệu cập nhật đến <strong className="font-medium text-zinc-200">
              {formatDisplayDate(quality.lastCalculatedDate)}
            </strong>
            {' '}· {PIPELINE_LABELS[quality.lastPipelineStatus] || 'Chưa rõ trạng thái cập nhật'}
          </p>
        </div>
        <div className="flex items-center gap-2 text-xs text-zinc-500">
          <Clock3 className="h-3.5 w-3.5" />
          Hôm nay tự cập nhật mỗi phút
          <Badge
            value={quality.freshnessStatus}
            label={STATUS_LABELS[quality.freshnessStatus] || STATUS_LABELS.NO_DATA}
          />
        </div>
      </section>

      <nav className="grid gap-2 md:grid-cols-2 xl:grid-cols-4" aria-label="Bốn nhóm báo cáo">
        {DECISION_VIEWS.map(item => (
          <button
            key={item.value}
            type="button"
            onClick={() => setView(item.value)}
            aria-pressed={view === item.value}
            className={`rounded-2xl border p-4 text-left transition ${
              view === item.value
                ? 'border-orange-500/40 bg-orange-500/10'
                : 'border-zinc-800 bg-zinc-900/40 hover:border-zinc-700'
            }`}
          >
            <span className={`text-[10px] font-semibold uppercase tracking-[0.16em] ${
              view === item.value ? 'text-orange-400' : 'text-zinc-600'
            }`}>
              {item.eyebrow}
            </span>
            <strong className="mt-2 block text-sm font-semibold text-zinc-200">{item.label}</strong>
            <span className="mt-1 block text-xs text-zinc-600">{item.description}</span>
          </button>
        ))}
      </nav>

      {notice && (
        <div className="flex items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-3 text-xs text-zinc-300">
          <CheckCircle2 className="h-4 w-4 text-emerald-400" /> {notice}
        </div>
      )}

      {view === 'happened' && (
        <div className="space-y-6">
          <SectionTitle
            icon={BarChart3}
            eyebrow="Đã xảy ra gì?"
            title={isCinemaScope ? `Hiệu suất riêng · ${scopeName}` : 'Bức tranh kinh doanh'}
            description={`${formatDisplayDate(dashboard?.period?.startDate)} – ${formatDisplayDate(dashboard?.period?.endDate)}`}
          />
          <section className="rounded-2xl border border-zinc-800 bg-zinc-900/55 p-5">
            <SectionTitle
              icon={TrendingUp}
              eyebrow="Xu hướng doanh thu"
              title="Doanh thu thuần theo ngày"
              description="Số tiền thực nhận sau khi trừ giảm giá và hoàn tiền"
              aside={(
                <span className="flex items-center gap-2 text-xs text-zinc-600">
                  <CalendarDays className="h-4 w-4" /> {days === 1 ? 'Trong ngày' : `${days} ngày`}
                </span>
              )}
            />
            <RevenueChart values={dashboard?.daily} />
          </section>
          {!isCinemaScope && <HealthScore health={dashboard?.healthScore} quality={quality} />}
          {isCinemaScope && (
            <section className="rounded-2xl border border-orange-500/20 bg-orange-500/5 p-5 text-sm leading-6 text-zinc-400">
              {hasCinemaData ? (
                <>
                  Các số liệu bên dưới chỉ lấy giao dịch của <strong className="font-semibold text-zinc-200">{scopeName}</strong>.
                  Dự báo và điểm sức khỏe toàn chuỗi không được trộn vào số liệu của rạp này.
                </>
              ) : (
                <>
                  <strong className="font-semibold text-zinc-200">{scopeName}</strong> chưa phát sinh
                  giao dịch trong khoảng thời gian này. Các chỉ số bằng 0 là trạng thái chưa có dữ liệu,
                  không phải lỗi hệ thống.
                </>
              )}
            </section>
          )}
          <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <MetricCard
              icon={CircleDollarSign}
              label="Doanh thu thuần"
              value={money(summary.netRevenue)}
              detail={`Doanh thu gộp ${money(summary.grossRevenue)}`}
            />
            <MetricCard
              icon={Ticket}
              label="Đơn hàng thành công"
              value={number(summary.bookingCount)}
              detail={`Trung bình mỗi đơn ${money(summary.averageBookingValue)}`}
            />
            <MetricCard
              icon={Target}
              label="Công suất ghế"
              value={percent(summary.occupancyRate)}
              detail={`${number(summary.ticketCount)} vé đã bán`}
            />
            <MetricCard
              icon={AlertTriangle}
              label="Tỷ lệ hoàn tiền"
              value={percent(summary.refundRate)}
              detail={`${number(summary.refundBookingCount)} đơn hàng đã hoàn tiền`}
            />
          </section>
          <section className={`grid gap-6 ${isCinemaScope ? '' : 'xl:grid-cols-2'}`}>
            <RankingTable
              rows={dashboard?.topMovies}
              kind="movie"
              cinemaNameByKey={cinemaNameByKey}
            />
            {!isCinemaScope && (
              <RankingTable
                rows={dashboard?.topCinemas}
                kind="cinema"
                cinemaNameByKey={cinemaNameByKey}
              />
            )}
          </section>
        </div>
      )}

      {view === 'why' && (
        <div className="space-y-6">
          <SectionTitle
            icon={Search}
            eyebrow="Vì sao xảy ra?"
            title="Nguyên nhân và điểm bất thường"
            description="So sánh với kết quả những ngày trước để chỉ ra thay đổi đáng chú ý và nơi ảnh hưởng nhiều nhất."
          />
          <section className="grid gap-4 lg:grid-cols-2">
            {dashboard?.anomalies?.map(item => (
              <article key={item.id} className="rounded-2xl border border-zinc-800 bg-zinc-900/55 p-5">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-[11px] font-semibold uppercase tracking-wider text-zinc-600">
                      {formatDisplayDate(item.statDate)} ·
                      {' '}{DETECTION_LABELS[item.detectionMethod] || 'Hệ thống tự động phát hiện'}
                    </p>
                    <h3 className="mt-2 font-semibold text-zinc-100">
                      {METRIC_LABELS[item.metricName] || 'Chỉ số kinh doanh'}
                    </h3>
                  </div>
                  <Badge value={item.severity} label={STATUS_LABELS[item.severity]} />
                </div>
                <div className="mt-5 grid grid-cols-3 gap-3">
                  <div><p className="text-[11px] text-zinc-600">Thực tế</p><strong className="mt-1 block text-sm text-zinc-200">{number(item.actualValue)}</strong></div>
                  <div><p className="text-[11px] text-zinc-600">Kỳ vọng</p><strong className="mt-1 block text-sm text-zinc-200">{number(item.expectedValue)}</strong></div>
                  <div><p className="text-[11px] text-zinc-600">Sai lệch</p><strong className="mt-1 block text-sm text-orange-300">{percent(item.deviationRate)}</strong></div>
                </div>
              </article>
            ))}
            {!dashboard?.anomalies?.length && (
              <div className="lg:col-span-2">
                <EmptyState>Chưa phát hiện bất thường có ý nghĩa trong khoảng đã chọn.</EmptyState>
              </div>
            )}
          </section>

          <section className="space-y-4">
            {dashboard?.insights?.map(item => (
              <article key={item.id} className="rounded-2xl border border-zinc-800 bg-zinc-900/55 p-5">
                <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                  <div>
                    <div className="mb-3 flex items-center gap-2">
                      <Badge value={item.severity} label={STATUS_LABELS[item.severity]} />
                      <span className="text-[11px] text-zinc-600">
                        Mức tin cậy {percent(item.confidenceScore)}
                      </span>
                    </div>
                    <h3 className="font-semibold text-zinc-100">
                      {localizeAnalyticsText(item.title, cinemaNameByKey)}
                    </h3>
                    <p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-500">
                      {localizeAnalyticsText(item.summary, cinemaNameByKey)}
                    </p>
                  </div>
                  <time className="text-xs text-zinc-600">{formatDisplayDate(item.statDate)}</time>
                </div>
                <div className="mt-5 rounded-xl border border-zinc-800 bg-zinc-950/40 p-4">
                  <p className="flex items-center gap-2 text-xs font-semibold text-zinc-300">
                    <Lightbulb className="h-4 w-4 text-orange-400" /> Kết luận nguyên nhân
                  </p>
                  <p className="mt-2 text-sm leading-6 text-zinc-500">
                    {localizeAnalyticsText(item.rootCause, cinemaNameByKey)}
                  </p>
                  {!!item.rootCauses?.length && (
                    <div className="mt-4 grid gap-2 md:grid-cols-3">
                      {item.rootCauses.map(cause => (
                        <div key={`${item.id}-${cause.rank}`} className="rounded-lg bg-zinc-900 px-3 py-3">
                          <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-600">
                            Nguyên nhân #{cause.rank}
                          </p>
                          <p className="mt-1 text-xs font-medium text-zinc-300">
                            {cause.dimensionType === 'CINEMA'
                              ? `${CAUSE_LABELS[cause.causeType] || 'Ảnh hưởng tại rạp'}: ${
                                  resolveCinemaName(
                                    cause.dimensionKey,
                                    null,
                                    cinemaNameByKey
                                  )
                                }`
                              : CAUSE_LABELS[cause.causeType] || 'Biến động chung của hệ thống'}
                          </p>
                          <p className="mt-1 text-[11px] text-zinc-600">
                            Đóng góp {percent(cause.contributionScore)}
                          </p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </article>
            ))}
            {!dashboard?.insights?.length && (
              <EmptyState>Không có phân tích nào cần chú ý trong khoảng đã chọn.</EmptyState>
            )}
          </section>
        </div>
      )}

      {view === 'next' && (
        <div className="space-y-6">
          <SectionTitle
            icon={TrendingUp}
            eyebrow="Sắp xảy ra gì?"
            title="Dự báo 7 ngày tiếp theo"
            description="Ước tính dựa trên kết quả các ngày trước, kèm khoảng doanh thu thấp nhất và cao nhất có thể xảy ra."
          />
          <section className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/55">
            <div className="grid grid-cols-[1fr_auto_auto] gap-4 border-b border-zinc-800 px-5 py-3 text-[10px] font-semibold uppercase tracking-wider text-zinc-600">
              <span>Ngày</span><span>Khoảng dự báo</span><span>Dự báo</span>
            </div>
            <div className="divide-y divide-zinc-800/80">
              {revenueForecasts.map(item => (
                <article key={`${item.forecastDate}-${item.forecastType}`} className="grid grid-cols-[1fr_auto_auto] items-center gap-4 px-5 py-4">
                  <div>
                    <p className="text-sm font-medium text-zinc-200">
                      {formatDisplayDate(item.forecastDate)}
                    </p>
                    <p className="mt-1 text-[11px] text-zinc-600">
                      Mức tin cậy {percent(item.confidenceScore)}
                    </p>
                  </div>
                  <p className="text-right text-xs text-zinc-600">
                    {money(item.predictionLowerBound)} – {money(item.predictionUpperBound)}
                  </p>
                  <strong className="min-w-32 text-right text-sm font-semibold text-emerald-300">
                    {money(item.predictedValue)}
                  </strong>
                </article>
              ))}
              {!revenueForecasts.length && (
                <p className="px-5 py-12 text-center text-sm text-zinc-600">
                  Cần thêm số liệu kinh doanh trước khi có thể dự báo.
                </p>
              )}
            </div>
          </section>

          <section>
            <h3 className="mb-3 text-sm font-semibold text-zinc-300">Độ chính xác của dự báo gần nhất</h3>
            <div className="grid gap-4 md:grid-cols-3">
              {dashboard?.forecastQuality?.map(item => (
                <article key={item.forecastType} className="rounded-2xl border border-zinc-800 bg-zinc-900/55 p-5">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-zinc-300">
                      {METRIC_LABELS[item.forecastType] || 'Chỉ số kinh doanh'}
                    </span>
                    <Badge
                      value={Number(item.mape) <= 0.2 ? 'SUCCESS' : 'WARNING'}
                      label={Number(item.mape) <= 0.2 ? 'Đáng tin cậy' : 'Cần theo dõi'}
                    />
                  </div>
                  <strong className="mt-5 block text-2xl font-semibold text-zinc-100">
                    {item.mape == null ? '—' : percent(item.mape)}
                  </strong>
                  <p className="mt-1 text-xs text-zinc-600">
                    Mức chênh lệch trung bình giữa dự báo và thực tế
                  </p>
                  <p className="mt-4 text-[11px] text-zinc-600">
                    Được đánh giá trên {number(item.sampleSize)} ngày có dữ liệu
                  </p>
                </article>
              ))}
              {!dashboard?.forecastQuality?.length && (
                <div className="md:col-span-3">
                  <EmptyState>Cần thêm dữ liệu thực tế để đánh giá độ chính xác của dự báo.</EmptyState>
                </div>
              )}
            </div>
          </section>
        </div>
      )}

      {view === 'action' && (
        <div className="space-y-6">
          <SectionTitle
            icon={Target}
            eyebrow="Nên làm gì tiếp theo?"
            title="Danh sách việc cần xử lý"
            description="Mỗi đề xuất đều có mức ưu tiên và trạng thái để người quản lý dễ theo dõi."
          />
          <section className="grid gap-6 xl:grid-cols-[1.35fr_1fr]">
            <div className="space-y-4">
              <h3 className="flex items-center gap-2 text-sm font-semibold text-zinc-300">
                <Lightbulb className="h-4 w-4 text-orange-400" /> Khuyến nghị
              </h3>
              {dashboard?.recommendations?.map(item => (
                <article key={item.id} className="rounded-2xl border border-zinc-800 bg-zinc-900/55 p-5">
                  <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                    <div>
                      <p className="text-[11px] font-semibold uppercase tracking-wider text-orange-400">
                        {PRIORITY_LABELS[item.priority] || 'Ưu tiên thường'} ·
                        {' '}{SERVICE_LABELS[item.targetService] || 'Bộ phận liên quan'}
                      </p>
                      <h4 className="mt-2 font-semibold text-zinc-100">
                        {localizeAnalyticsText(item.title, cinemaNameByKey)}
                      </h4>
                      {item.confidenceScore != null && (
                        <p className="mt-1 text-[11px] text-zinc-600">
                          Mức tin cậy {percent(item.confidenceScore)}
                        </p>
                      )}
                    </div>
                    <Badge value={item.status} label={STATUS_LABELS[item.status]} />
                  </div>
                  <p className="mt-3 text-sm leading-6 text-zinc-500">
                    {localizeAnalyticsText(item.description, cinemaNameByKey)}
                  </p>
                  <div className="mt-4 rounded-xl bg-zinc-950/45 p-3 text-xs leading-5 text-zinc-500">
                    <span className="font-medium text-zinc-300">Tác động kỳ vọng:</span>{' '}
                    {localizeAnalyticsText(item.expectedImpact, cinemaNameByKey)}
                  </div>
                  {item.status === 'PENDING' && (
                    <div className="mt-4 flex flex-wrap gap-2">
                      <button
                        type="button"
                        onClick={() => handleRecommendation(item.id, 'ACCEPTED')}
                        disabled={acting === `recommendation-${item.id}`}
                        className="flex items-center gap-2 rounded-lg bg-zinc-100 px-3 py-2 text-xs font-semibold text-zinc-950 disabled:opacity-50"
                      >
                        <Check className="h-3.5 w-3.5" /> Nhận xử lý
                      </button>
                      <button
                        type="button"
                        onClick={() => handleRecommendation(item.id, 'DISMISSED')}
                        disabled={acting === `recommendation-${item.id}`}
                        className="flex items-center gap-2 rounded-lg border border-zinc-800 px-3 py-2 text-xs font-medium text-zinc-400 disabled:opacity-50"
                      >
                        <X className="h-3.5 w-3.5" /> Bỏ qua
                      </button>
                    </div>
                  )}
                  {item.status === 'ACCEPTED' && (
                    <button
                      type="button"
                      onClick={() => handleRecommendation(item.id, 'COMPLETED')}
                      disabled={acting === `recommendation-${item.id}`}
                      className="mt-4 flex items-center gap-2 rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-xs font-semibold text-emerald-300 disabled:opacity-50"
                    >
                      <CheckCircle2 className="h-3.5 w-3.5" /> Đánh dấu hoàn tất
                    </button>
                  )}
                </article>
              ))}
              {!dashboard?.recommendations?.length && (
                <EmptyState>Chưa có khuyến nghị cần quyết định.</EmptyState>
              )}
            </div>

            <div className="space-y-4">
              <h3 className="flex items-center gap-2 text-sm font-semibold text-zinc-300">
                <AlertTriangle className="h-4 w-4 text-red-400" /> Cảnh báo cần chú ý
              </h3>
              {dashboard?.alerts?.map(item => (
                <article key={item.id} className="rounded-2xl border border-red-500/15 bg-red-500/5 p-5">
                  <div className="flex items-center justify-between gap-3">
                    <Badge value={item.severity} label={STATUS_LABELS[item.severity]} />
                    <time className="text-[11px] text-zinc-600">
                      {formatDisplayDate(item.createdAt)}
                    </time>
                  </div>
                  <h4 className="mt-3 text-sm font-semibold text-zinc-100">
                    {localizeAnalyticsText(item.title, cinemaNameByKey)}
                  </h4>
                  <p className="mt-2 text-xs leading-5 text-zinc-500">
                    {localizeAnalyticsText(item.message, cinemaNameByKey)}
                  </p>
                  {item.acknowledged ? (
                    <p className="mt-4 flex items-center gap-2 text-xs text-emerald-400">
                      <CheckCircle2 className="h-4 w-4" /> Đã ghi nhận
                      {item.acknowledgedBy ? ` bởi ${item.acknowledgedBy}` : ''}
                    </p>
                  ) : (
                    <button
                      type="button"
                      onClick={() => handleAlert(item.id)}
                      disabled={acting === `alert-${item.id}`}
                      className="mt-4 rounded-lg border border-red-500/25 px-3 py-2 text-xs font-semibold text-red-300 disabled:opacity-50"
                    >
                      Ghi nhận cảnh báo
                    </button>
                  )}
                </article>
              ))}
              {!dashboard?.alerts?.length && (
                <EmptyState>Không có cảnh báo cần xử lý.</EmptyState>
              )}
            </div>
          </section>
        </div>
      )}
    </main>
  );
}
