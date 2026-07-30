import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  ArrowRight,
  BadgeDollarSign,
  Building2,
  CheckCircle2,
  CircleDot,
  Film,
  ListChecks,
  RefreshCw,
  Sparkles,
} from 'lucide-react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import adminMovieOperationsService from '../services/adminMovieOperationsService';

const number = value => new Intl.NumberFormat('vi-VN').format(Number(value) || 0);

const quickActions = [
  {
    title: 'Kiểm tra nội dung phim',
    description: 'Hoàn thiện poster, phiên bản và thông tin trước khi đưa phim vào lịch.',
    icon: Film,
    path: '/admin/movies',
  },
  {
    title: 'Kiểm tra cơ sở rạp',
    description: 'Xem phòng chiếu, sơ đồ ghế, lịch đóng cửa và bảo trì.',
    icon: Building2,
    path: '/admin/cinemas',
  },
  {
    title: 'Lập lịch tuần',
    description: 'Tạo bản lịch nháp, rà soát rồi mới áp dụng vào vận hành.',
    icon: Sparkles,
    path: '/admin/showtime-schedules/create',
  },
  {
    title: 'Chuẩn bị giá vé',
    description: 'Đảm bảo rạp có mẫu giá đang áp dụng trước khi mở bán.',
    icon: BadgeDollarSign,
    path: '/admin/pricing',
  },
];

export default function AdminMovieOperationsPage() {
  const navigate = useNavigate();
  const { triggerToast } = useOutletContext() || {};
  const [overview, setOverview] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await adminMovieOperationsService.getOverview();
      setOverview(data);
      if (data.unavailableSections.length > 0) {
        triggerToast?.('Một số số liệu chưa tải được. Bạn vẫn có thể tiếp tục vận hành từng khu vực.', 'error');
      }
    } catch {
      triggerToast?.('Không thể tải tổng quan vận hành phim. Vui lòng thử lại.', 'error');
    } finally {
      setLoading(false);
    }
  }, [triggerToast]);

  useEffect(() => {
    let cancelled = false;

    adminMovieOperationsService.getOverview()
      .then(data => {
        if (cancelled) return;
        setOverview(data);
        if (data.unavailableSections.length > 0) {
          triggerToast?.('Một số số liệu chưa tải được. Bạn vẫn có thể tiếp tục vận hành từng khu vực.', 'error');
        }
      })
      .catch(() => {
        if (!cancelled) {
          triggerToast?.('Không thể tải tổng quan vận hành phim. Vui lòng thử lại.', 'error');
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [triggerToast]);

  const tasks = useMemo(() => {
    if (!overview) return [];
    const movie = overview.movies || {};
    return [
      Number(movie.blocked) > 0 && {
        key: 'blocked-movies',
        level: 'danger',
        title: `${number(movie.blocked)} phim chưa thể đưa vào lịch`,
        description: 'Thiếu dữ liệu bắt buộc hoặc chưa có phiên bản phim hợp lệ.',
        action: 'Kiểm tra phim',
        path: '/admin/movies?healthStatus=BLOCKED',
      },
      Number(movie.warning) > 0 && {
        key: 'warning-movies',
        level: 'warning',
        title: `${number(movie.warning)} phim cần kiểm tra thêm`,
        description: 'Phim vẫn lưu được nhưng chưa đạt chất lượng tốt để vận hành.',
        action: 'Mở danh sách',
        path: '/admin/movies?healthStatus=WARNING',
      },
      Number(movie.draft) > 0 && {
        key: 'draft-movies',
        level: 'neutral',
        title: `${number(movie.draft)} phim đang chờ duyệt`,
        description: 'Xác nhận nội dung trước khi chuyển sang sắp chiếu hoặc đang chiếu.',
        action: 'Duyệt phim',
        path: '/admin/movies?status=DRAFT',
      },
      overview.draftShowtimes > 0 && {
        key: 'draft-showtimes',
        level: 'warning',
        title: `${number(overview.draftShowtimes)} suất chiếu chưa mở bán`,
        description: 'Kiểm tra phòng, thời gian và giá vé trước khi khách hàng đặt ghế.',
        action: 'Kiểm tra lịch',
        path: '/admin/showtimes?status=DRAFT',
      },
      overview.activeCinemas === 0 && {
        key: 'cinemas',
        level: 'danger',
        title: 'Chưa có cụm rạp đang hoạt động',
        description: 'Cần hoàn thiện thông tin và giờ hoạt động của ít nhất một cụm rạp.',
        action: 'Quản lý rạp',
        path: '/admin/cinemas',
      },
      overview.activePricePolicies === 0 && {
        key: 'pricing',
        level: 'danger',
        title: 'Chưa có mẫu giá vé đang áp dụng',
        description: 'Suất chiếu sẽ không thể mở bán nếu chưa xác định đủ giá cho các loại ghế.',
        action: 'Tạo mẫu giá',
        path: '/admin/pricing/create',
      },
    ].filter(Boolean);
  }, [overview]);

  const readinessSteps = [
    {
      label: 'Nội dung phim',
      value: overview?.movies?.ready,
      total: overview?.movies?.total,
      ready: Number(overview?.movies?.ready) > 0,
      path: '/admin/movies',
    },
    {
      label: 'Cơ sở đang hoạt động',
      value: overview?.activeCinemas,
      ready: Number(overview?.activeCinemas) > 0,
      path: '/admin/cinemas',
    },
    {
      label: 'Mẫu giá đang áp dụng',
      value: overview?.activePricePolicies,
      ready: Number(overview?.activePricePolicies) > 0,
      path: '/admin/pricing',
    },
    {
      label: 'Suất đang mở bán',
      value: overview?.openShowtimes,
      ready: Number(overview?.openShowtimes) > 0,
      path: '/admin/showtimes?status=OPEN_FOR_BOOKING',
    },
  ];

  return (
    <div className="min-h-full space-y-6 bg-zinc-950 pb-10 text-white" data-testid="movie-operations-page">
      <header className="flex flex-col gap-4 border-b border-zinc-800 pb-6 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.22em] text-brand-orange">Vận hành nội dung & lịch chiếu</p>
          <h1 className="mt-2 text-3xl font-black tracking-tight md:text-4xl">Trung tâm vận hành phim</h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">
            Bắt đầu từ những việc cần xử lý. Hệ thống sẽ dẫn bạn từ nội dung phim, cơ sở rạp, giá vé đến mở bán suất chiếu.
          </p>
        </div>
        <button
          type="button"
          onClick={load}
          disabled={loading}
          className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-sm font-bold text-zinc-200 hover:bg-zinc-800 disabled:opacity-50"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Cập nhật số liệu
        </button>
      </header>

      <section aria-labelledby="today-tasks-heading" className="rounded-3xl border border-zinc-800 bg-zinc-900/45 p-5 md:p-6">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 id="today-tasks-heading" className="flex items-center gap-2 text-xl font-black">
              <ListChecks className="h-5 w-5 text-brand-orange" />
              Việc cần xử lý
            </h2>
            <p className="mt-1 text-sm text-zinc-500">Ưu tiên các mục màu đỏ và vàng trước khi mở bán.</p>
          </div>
          {!loading && (
            <span className={`rounded-full px-3 py-1.5 text-xs font-bold ${
              tasks.length === 0
                ? 'bg-emerald-500/10 text-emerald-300'
                : 'bg-amber-500/10 text-amber-300'
            }`}>
              {tasks.length === 0 ? 'Không có cảnh báo' : `${tasks.length} nhóm việc`}
            </span>
          )}
        </div>

        {loading ? (
          <div className="mt-5 grid gap-3 lg:grid-cols-2">
            {[0, 1, 2, 3].map(item => <div key={item} className="h-28 animate-pulse rounded-2xl bg-zinc-800/70" />)}
          </div>
        ) : tasks.length === 0 ? (
          <div className="mt-5 flex items-start gap-4 rounded-2xl border border-emerald-500/20 bg-emerald-500/10 p-5">
            <CheckCircle2 className="mt-0.5 h-6 w-6 shrink-0 text-emerald-400" />
            <div>
              <p className="font-black text-emerald-200">Các điều kiện chính đang ổn định</p>
              <p className="mt-1 text-sm text-emerald-100/70">Bạn có thể tiếp tục lập lịch hoặc kiểm tra các suất sắp mở bán.</p>
            </div>
          </div>
        ) : (
          <div className="mt-5 grid gap-3 lg:grid-cols-2">
            {tasks.map(task => (
              <button
                key={task.key}
                type="button"
                onClick={() => navigate(task.path)}
                className={`group flex items-start gap-4 rounded-2xl border p-4 text-left transition-colors ${
                  task.level === 'danger'
                    ? 'border-red-500/20 bg-red-500/5 hover:bg-red-500/10'
                    : task.level === 'warning'
                      ? 'border-amber-500/20 bg-amber-500/5 hover:bg-amber-500/10'
                      : 'border-zinc-700 bg-zinc-900 hover:bg-zinc-800'
                }`}
              >
                <AlertTriangle className={`mt-0.5 h-5 w-5 shrink-0 ${
                  task.level === 'danger' ? 'text-red-400' : task.level === 'warning' ? 'text-amber-400' : 'text-zinc-400'
                }`} />
                <span className="min-w-0 flex-1">
                  <span className="block font-black text-zinc-100">{task.title}</span>
                  <span className="mt-1 block text-sm leading-5 text-zinc-400">{task.description}</span>
                  <span className="mt-3 inline-flex items-center gap-1 text-xs font-bold text-brand-orange">
                    {task.action} <ArrowRight className="h-3.5 w-3.5 transition-transform group-hover:translate-x-1" />
                  </span>
                </span>
              </button>
            ))}
          </div>
        )}
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.2fr_1fr]">
        <div className="rounded-3xl border border-zinc-800 bg-zinc-900/35 p-5 md:p-6">
          <div>
            <h2 className="text-xl font-black">Chuỗi sẵn sàng mở bán</h2>
            <p className="mt-1 text-sm text-zinc-500">Mỗi bước phải sẵn sàng trước khi khách hàng có thể đặt ghế.</p>
          </div>
          <div className="mt-6 space-y-3">
            {readinessSteps.map((step, index) => (
              <button
                key={step.label}
                type="button"
                onClick={() => navigate(step.path)}
                className="flex w-full items-center gap-4 rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4 text-left hover:border-zinc-700 hover:bg-zinc-900"
              >
                <span className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full border text-sm font-black ${
                  step.ready
                    ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300'
                    : 'border-zinc-700 bg-zinc-900 text-zinc-500'
                }`}>
                  {step.ready ? <CheckCircle2 className="h-5 w-5" /> : index + 1}
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block font-bold text-zinc-200">{step.label}</span>
                  <span className="mt-1 block text-xs text-zinc-500">
                    {step.total !== undefined
                      ? `${number(step.value)} / ${number(step.total)} phim đạt yêu cầu`
                      : `${number(step.value)} mục đang sẵn sàng`}
                  </span>
                </span>
                <ArrowRight className="h-4 w-4 text-zinc-600" />
              </button>
            ))}
          </div>
        </div>

        <div className="rounded-3xl border border-zinc-800 bg-zinc-900/35 p-5 md:p-6">
          <h2 className="text-xl font-black">Bắt đầu nhanh</h2>
          <p className="mt-1 text-sm text-zinc-500">Chọn đúng công việc bạn muốn thực hiện.</p>
          <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-1">
            {quickActions.map(action => {
              const Icon = action.icon;
              return (
                <button
                  key={action.title}
                  type="button"
                  onClick={() => navigate(action.path)}
                  className="group flex items-start gap-3 rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4 text-left hover:border-brand-orange/30 hover:bg-brand-orange/5"
                >
                  <span className="rounded-xl bg-brand-orange/10 p-2.5 text-brand-orange">
                    <Icon className="h-5 w-5" />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block font-black text-zinc-100">{action.title}</span>
                    <span className="mt-1 block text-xs leading-5 text-zinc-500">{action.description}</span>
                  </span>
                  <ArrowRight className="mt-3 h-4 w-4 text-zinc-600 transition-transform group-hover:translate-x-1 group-hover:text-brand-orange" />
                </button>
              );
            })}
          </div>
        </div>
      </section>

      <div className="flex items-start gap-3 rounded-2xl border border-blue-500/20 bg-blue-500/5 p-4 text-sm text-blue-100/80">
        <CircleDot className="mt-0.5 h-4 w-4 shrink-0 text-blue-300" />
        <p>
          Số liệu trên đây dùng để định hướng công việc. Khi mở bán, hệ thống vẫn kiểm tra lại phim, phòng, lịch và giá bằng dữ liệu mới nhất.
        </p>
      </div>
    </div>
  );
}
