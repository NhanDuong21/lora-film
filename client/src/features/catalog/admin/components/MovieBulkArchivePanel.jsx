import { AlertTriangle, Archive, CheckCircle2, Loader2, XCircle } from 'lucide-react';

const reasonLabel = item => item.reason || item.reasonCode || 'Không xác định được nguyên nhân.';

export default function MovieBulkArchivePanel({
  limit = 100,
  isPending = false,
  result = null,
  error = '',
  onArchive,
}) {
  const skippedItems = result?.results?.filter(item => item.outcome === 'SKIPPED') || [];
  const errorItems = result?.results?.filter(item => item.outcome === 'ERROR') || [];
  const processedCount = result?.requested ?? limit;

  return (
    <section className="rounded-2xl border border-amber-500/20 bg-amber-500/[0.06] p-4" aria-labelledby="bulk-archive-title">
      <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-center">
        <div className="flex min-w-0 items-start gap-3">
          <div className="rounded-xl border border-amber-500/20 bg-amber-500/10 p-2 text-amber-300">
            <Archive className="h-5 w-5" />
          </div>
          <div>
            <h2 id="bulk-archive-title" className="text-sm font-black uppercase tracking-wide text-amber-200">
              Xử lý phim TMDB đã cũ
            </h2>
            <p className="mt-1 max-w-3xl text-xs leading-5 text-zinc-400">
              Chỉ chọn phim đang ở Chờ duyệt, có ngày phát hành không quá hôm nay và chuyển sang Không hoạt động.
              Phim chưa có ngày phát hành sẽ không bị tự động thay đổi.
            </p>
          </div>
        </div>

        <button
          type="button"
          disabled={isPending}
          onClick={onArchive}
          className="inline-flex shrink-0 items-center justify-center gap-2 rounded-xl bg-amber-400 px-5 py-3 text-xs font-black uppercase tracking-wide text-zinc-950 transition-colors hover:bg-amber-300 disabled:cursor-not-allowed disabled:bg-zinc-800 disabled:text-zinc-500"
        >
          {isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Archive className="h-4 w-4" />}
          {isPending ? 'Đang kiểm tra và lưu trữ' : `Đưa tối đa ${limit} phim cũ vào không hoạt động`}
        </button>
      </div>

      {error && (
        <div className="mt-4 flex items-start gap-2 rounded-xl border border-red-500/20 bg-red-500/10 p-3 text-xs text-red-200" role="alert">
          <XCircle className="mt-0.5 h-4 w-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {result && (
        <div className="mt-4 space-y-3" aria-live="polite">
          <div className="grid gap-2 sm:grid-cols-4">
            <ResultStat label="Đã kiểm tra" value={processedCount} tone="text-zinc-200" />
            <ResultStat label="Đã lưu trữ" value={result.archived} tone="text-amber-300" />
            <ResultStat label="Bỏ qua" value={result.skipped} tone="text-zinc-300" />
            <ResultStat label="Lỗi" value={result.errors} tone="text-red-300" />
          </div>

          {result.archived > 0 && (
            <div className="flex items-center gap-2 rounded-xl border border-amber-500/20 bg-amber-500/10 p-3 text-xs text-amber-100">
              <CheckCircle2 className="h-4 w-4 shrink-0" />
              Đã chuyển {result.archived} phim cũ sang trạng thái Không hoạt động.
            </div>
          )}

          {(skippedItems.length > 0 || errorItems.length > 0) && (
            <details className="rounded-xl border border-zinc-800 bg-zinc-950/70 p-3">
              <summary className="cursor-pointer text-xs font-bold text-zinc-300">
                Xem {skippedItems.length + errorItems.length} phim chưa được lưu trữ
              </summary>
              <ul className="mt-3 max-h-56 space-y-2 overflow-auto pr-1">
                {[...skippedItems, ...errorItems].map(item => (
                  <li key={item.moviePublicId} className="flex items-start gap-2 rounded-lg bg-zinc-900 px-3 py-2 text-xs">
                    <AlertTriangle className={`mt-0.5 h-3.5 w-3.5 shrink-0 ${item.outcome === 'ERROR' ? 'text-red-400' : 'text-amber-400'}`} />
                    <span className="min-w-0">
                      <strong className="block truncate text-zinc-200">{item.title || item.moviePublicId}</strong>
                      <span className="text-zinc-500">{reasonLabel(item)}</span>
                    </span>
                  </li>
                ))}
              </ul>
            </details>
          )}
        </div>
      )}
    </section>
  );
}

function ResultStat({ label, value, tone }) {
  return (
    <div className="rounded-xl border border-zinc-800 bg-zinc-950/70 px-3 py-2">
      <p className="text-[10px] font-bold uppercase tracking-wide text-zinc-500">{label}</p>
      <p className={`mt-1 text-lg font-black ${tone}`}>{value ?? 0}</p>
    </div>
  );
}
