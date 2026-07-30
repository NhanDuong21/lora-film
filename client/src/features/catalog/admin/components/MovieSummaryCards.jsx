import { AlertTriangle, Ban, Clapperboard, Clock3, RefreshCw } from 'lucide-react';

const CARDS = [
  {
    key: 'draft',
    label: 'Chờ duyệt',
    description: 'Phim mới nhập đang chờ quyết định',
    icon: Clock3,
    active: query => query.status === 'DRAFT',
    changes: { status: 'DRAFT' },
    accent: 'text-yellow-400 border-yellow-500/30 bg-yellow-500/10',
  },
  {
    key: 'warning',
    label: 'Cần bổ sung',
    description: 'Thiếu dữ liệu cần hoàn thiện',
    icon: AlertTriangle,
    active: query => query.healthStatus === 'WARNING',
    changes: { healthStatus: 'WARNING' },
    accent: 'text-amber-400 border-amber-500/30 bg-amber-500/10',
  },
  {
    key: 'blocked',
    label: 'Chưa thể phát hành',
    description: 'Đang bị chặn khỏi lịch chiếu',
    icon: Ban,
    active: query => query.healthStatus === 'BLOCKED',
    changes: { healthStatus: 'BLOCKED' },
    accent: 'text-red-400 border-red-500/30 bg-red-500/10',
  },
  {
    key: 'nowShowing',
    label: 'Đang phục vụ',
    description: 'Có suất chiếu đang mở bán',
    icon: Clapperboard,
    active: query => query.status === 'NOW_SHOWING',
    changes: { status: 'NOW_SHOWING' },
    accent: 'text-emerald-400 border-emerald-500/30 bg-emerald-500/10',
  },
];

export default function MovieSummaryCards({ summary, query, isLoading, isRefreshing, error, onRetry, onSelect }) {
  if (isLoading) {
    return (
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4" aria-label="Đang tải thống kê phim">
        {CARDS.map(card => (
          <div key={card.key} className="h-32 rounded-2xl border border-zinc-800 bg-zinc-900/50 animate-pulse" />
        ))}
      </div>
    );
  }

  if (error && !summary) {
    return (
      <div className="flex items-center justify-between gap-4 rounded-2xl border border-red-900/50 bg-red-950/20 p-4 text-sm text-red-300">
        <span>Không thể tải thống kê phim: {error}</span>
        <button type="button" onClick={onRetry} className="inline-flex items-center gap-2 rounded-lg border border-red-800 px-3 py-2 text-xs font-bold hover:bg-red-900/30">
          <RefreshCw className="h-3.5 w-3.5" /> Thử lại
        </button>
      </div>
    );
  }

  return (
    <section className="space-y-3" aria-labelledby="movie-work-queue-title">
      <div>
        <h2 id="movie-work-queue-title" className="text-sm font-black uppercase tracking-wider text-white">
          Việc cần làm
        </h2>
        <p className="mt-1 text-xs text-zinc-500">
          Chọn một nhóm để xem đúng các phim cần xử lý.
        </p>
      </div>
      {error && (
        <div className="flex items-center justify-between rounded-xl border border-red-900/40 bg-red-950/20 px-3 py-2 text-xs text-red-300">
          <span>Không thể làm mới thống kê: {error}</span>
          <button type="button" onClick={onRetry} className="font-bold underline">Thử lại</button>
        </div>
      )}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {CARDS.map(card => {
          const Icon = card.icon;
          const active = card.active(query);
          return (
            <button
              key={card.key}
              type="button"
              onClick={() => onSelect(card.changes)}
              aria-pressed={active}
              className={`enterprise-card min-h-32 p-5 text-left transition-all relative overflow-hidden group ${
                active
                  ? `${card.accent} ring-1 ring-current`
                  : 'hover:border-zinc-700 hover:bg-zinc-800'
              }`}
            >
              <div className="flex items-start justify-between gap-3 relative z-10">
                <div>
                  <p className="text-[11px] font-black uppercase tracking-wider opacity-80">{card.label}</p>
                  <p className="mt-2 text-2xl font-black">{summary?.[card.key] ?? 0}</p>
                  <p className="mt-2 text-xs leading-5 text-zinc-400">{card.description}</p>
                </div>
                <Icon className="h-5 w-5 opacity-80" />
              </div>
              {isRefreshing && <span className="absolute inset-x-0 bottom-0 h-0.5 animate-pulse bg-current opacity-50" />}
            </button>
          );
        })}
      </div>
    </section>
  );
}
