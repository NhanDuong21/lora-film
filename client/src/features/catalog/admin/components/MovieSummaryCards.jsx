import {
  AlertTriangle,
  Ban,
  CheckCircle2,
  ChevronRight,
  Clapperboard,
  Clock3,
  RefreshCw,
} from 'lucide-react';

const WORK_QUEUES = [
  {
    key: 'draft',
    label: 'Phim mới cần duyệt',
    description: 'Kiểm tra nội dung trước khi đưa vào phục vụ',
    icon: Clock3,
    active: query => query.status === 'DRAFT' && !query.healthStatus && !query.source,
    changes: { status: 'DRAFT', healthStatus: '', source: '' },
    activeClass: 'border-amber-400 bg-amber-400/10 text-amber-300',
    countClass: 'text-amber-300',
  },
  {
    key: 'warning',
    label: 'Cần bổ sung thông tin',
    description: 'Có dữ liệu nên kiểm tra hoặc hoàn thiện',
    icon: AlertTriangle,
    active: query => query.healthStatus === 'WARNING',
    changes: { status: 'ALL', healthStatus: 'WARNING', source: '' },
    activeClass: 'border-orange-400 bg-orange-400/10 text-orange-300',
    countClass: 'text-orange-300',
  },
  {
    key: 'blocked',
    label: 'Chưa đủ điều kiện',
    description: 'Đang thiếu nội dung bắt buộc để phát hành',
    icon: Ban,
    active: query => query.healthStatus === 'BLOCKED',
    changes: { status: 'ALL', healthStatus: 'BLOCKED', source: '' },
    activeClass: 'border-red-400 bg-red-400/10 text-red-300',
    countClass: 'text-red-300',
  },
  {
    key: 'nowShowing',
    label: 'Đang phục vụ',
    description: 'Phim đang có suất chiếu mở bán',
    icon: Clapperboard,
    active: query => query.status === 'NOW_SHOWING' && !query.healthStatus,
    changes: { status: 'NOW_SHOWING', healthStatus: '', source: '' },
    activeClass: 'border-emerald-400 bg-emerald-400/10 text-emerald-300',
    countClass: 'text-emerald-300',
  },
];

export default function MovieSummaryCards({
  summary,
  query,
  isLoading,
  isRefreshing,
  error,
  onRetry,
  onSelect,
}) {
  if (isLoading) {
    return (
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4" aria-label="Đang tải nhóm công việc">
        {WORK_QUEUES.map(queue => (
          <div key={queue.key} className="h-28 animate-pulse rounded-2xl border border-zinc-800 bg-zinc-900/50" />
        ))}
      </div>
    );
  }

  if (error && !summary) {
    return (
      <div className="flex flex-col items-start justify-between gap-3 rounded-2xl border border-red-900/50 bg-red-950/20 p-4 text-sm text-red-300 sm:flex-row sm:items-center">
        <span>Chưa tải được số lượng phim cần xử lý.</span>
        <button
          type="button"
          onClick={onRetry}
          className="inline-flex items-center gap-2 rounded-lg border border-red-800 px-3 py-2 text-xs font-bold hover:bg-red-900/30"
        >
          <RefreshCw className="h-3.5 w-3.5" />
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <section className="space-y-3" aria-labelledby="movie-work-queue-title">
      <div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-end">
        <div>
          <h2 id="movie-work-queue-title" className="text-base font-bold text-white">
            Bạn muốn xử lý nhóm phim nào?
          </h2>
          <p className="mt-1 text-sm text-zinc-500">
            Chọn một nhóm để danh sách bên dưới chỉ hiển thị những phim liên quan.
          </p>
        </div>
        {isRefreshing && (
          <span className="inline-flex items-center gap-2 text-xs text-zinc-500">
            <RefreshCw className="h-3.5 w-3.5 animate-spin" />
            Đang cập nhật
          </span>
        )}
      </div>

      {error && (
        <div className="flex items-center justify-between rounded-xl border border-amber-900/40 bg-amber-950/20 px-3 py-2 text-xs text-amber-200">
          <span>Số liệu có thể chưa phải mới nhất.</span>
          <button type="button" onClick={onRetry} className="font-bold underline">
            Tải lại
          </button>
        </div>
      )}

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {WORK_QUEUES.map(queue => {
          const Icon = queue.icon;
          const active = queue.active(query);
          const count = summary?.[queue.key] ?? 0;

          return (
            <button
              key={queue.key}
              type="button"
              onClick={() => onSelect(queue.changes)}
              aria-pressed={active}
              aria-label={`${queue.label}: ${count} phim`}
              className={`group relative min-h-28 rounded-2xl border p-4 text-left transition ${
                active
                  ? queue.activeClass
                  : 'border-zinc-800 bg-zinc-900/45 text-zinc-300 hover:border-zinc-700 hover:bg-zinc-900'
              }`}
            >
              <div className="flex items-start justify-between gap-3">
                <span className={`rounded-xl border p-2 ${
                  active ? 'border-current/20 bg-current/10' : 'border-zinc-800 bg-zinc-950 text-zinc-500'
                }`}>
                  <Icon className="h-4 w-4" />
                </span>
                <span className={`text-2xl font-black ${active ? '' : queue.countClass}`}>
                  {count}
                </span>
              </div>
              <div className="mt-3 flex items-center gap-1.5 font-bold text-zinc-100">
                {active && <CheckCircle2 className="h-4 w-4 shrink-0" />}
                <span>{queue.label}</span>
                <ChevronRight className="ml-auto h-4 w-4 text-zinc-600 transition group-hover:translate-x-0.5 group-hover:text-zinc-300" />
              </div>
              <p className="mt-1 text-xs leading-5 text-zinc-500">{queue.description}</p>
            </button>
          );
        })}
      </div>
    </section>
  );
}
