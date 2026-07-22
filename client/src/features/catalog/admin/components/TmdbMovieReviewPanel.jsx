import { AlertTriangle, CheckCircle2, Cloud, Loader2, RefreshCw } from 'lucide-react';

function formatTimestamp(value) {
  if (!value) return 'Chưa có';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? value : parsed.toLocaleString('vi-VN');
}

function Value({ children }) {
  return <span className="break-words text-xs text-zinc-300">{children || '—'}</span>;
}

export default function TmdbMovieReviewPanel({ movie, review, isLoading, isRefreshing, error, onRetry }) {
  if (movie?.source !== 'TMDB') return null;

  return (
    <section className="mt-6 overflow-hidden rounded-2xl border border-sky-500/20 bg-[#0a0a0a]" aria-labelledby="tmdb-review-title">
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-zinc-800 p-6">
        <div className="flex gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-sky-500/10 text-sky-400"><Cloud size={20} /></div>
          <div>
            <h3 id="tmdb-review-title" className="font-semibold text-white">TMDB Import Review</h3>
            <p className="mt-1 text-sm text-zinc-400">So sánh trực tiếp dữ liệu đang lưu với provider; thay đổi này chỉ mang tính tham khảo.</p>
          </div>
        </div>
        <button type="button" onClick={onRetry} disabled={isLoading || isRefreshing} className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 px-3 py-2 text-xs text-zinc-300 hover:bg-zinc-800 disabled:opacity-50">
          {isRefreshing ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />} Làm mới
        </button>
      </div>

      {isLoading && !review && <div className="flex items-center gap-3 p-6 text-sm text-zinc-400"><Loader2 className="h-4 w-4 animate-spin" /> Đang tải dữ liệu TMDB mới nhất…</div>}

      {error && (
        <div className="m-6 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-red-500/20 bg-red-500/10 p-4 text-sm text-red-300" role="alert">
          <span>Không thể tải so sánh TMDB: {error}</span>
          <button type="button" onClick={onRetry} className="rounded-lg border border-red-400/30 px-3 py-1.5 font-medium">Thử lại</button>
        </div>
      )}

      {review && (
        <div className="space-y-6 p-6">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-xl border border-zinc-800 bg-zinc-950 p-3"><p className="text-[11px] uppercase text-zinc-500">Review status</p><p className="mt-1 text-sm font-semibold text-white">{review.reviewStatus}</p></div>
            <div className="rounded-xl border border-zinc-800 bg-zinc-950 p-3"><p className="text-[11px] uppercase text-zinc-500">Có thể duyệt</p><p className={`mt-1 text-sm font-semibold ${review.canApprove ? 'text-green-400' : 'text-red-400'}`}>{review.canApprove ? 'Có' : 'Chưa'}</p></div>
            <div className="rounded-xl border border-zinc-800 bg-zinc-950 p-3"><p className="text-[11px] uppercase text-zinc-500">Snapshot đã áp dụng</p><p className="mt-1 text-xs text-zinc-300">{formatTimestamp(review.appliedTmdbLastUpdated)}</p></div>
            <div className="rounded-xl border border-zinc-800 bg-zinc-950 p-3"><p className="text-[11px] uppercase text-zinc-500">Provider hiện tại</p><p className="mt-1 text-xs text-zinc-300">{formatTimestamp(review.providerLastUpdated)}</p></div>
          </div>

          {review.approvalBlockers?.length > 0 && (
            <div className="rounded-xl border border-red-500/20 bg-red-500/10 p-4">
              <div className="flex gap-2 text-sm font-semibold text-red-300"><AlertTriangle size={17} /> Chưa thể duyệt</div>
              <ul className="mt-2 list-disc space-y-1 pl-5 text-xs text-red-200/80">{review.approvalBlockers.map(item => <li key={item}>{item}</li>)}</ul>
            </div>
          )}

          <div>
            <div className="mb-3 flex items-center gap-2">
              {review.hasProviderChanges ? <AlertTriangle className="h-4 w-4 text-amber-400" /> : <CheckCircle2 className="h-4 w-4 text-green-400" />}
              <h4 className="text-sm font-semibold text-white">Metadata {review.hasProviderChanges ? 'có thay đổi' : 'không thay đổi'}</h4>
            </div>
            <div className="overflow-x-auto rounded-xl border border-zinc-800">
              <table className="min-w-full divide-y divide-zinc-800 text-left">
                <thead className="bg-zinc-900 text-[11px] uppercase text-zinc-500"><tr><th className="px-4 py-3">Trường</th><th className="px-4 py-3">Hiện tại</th><th className="px-4 py-3">TMDB</th></tr></thead>
                <tbody className="divide-y divide-zinc-800/70">
                  {review.scalarDiffs?.map(diff => (
                    <tr key={diff.field} className={diff.changed ? 'bg-amber-500/[0.04]' : ''}>
                      <th className="w-40 px-4 py-3 text-xs font-medium text-zinc-400">{diff.label}{diff.changed && <span className="ml-2 text-amber-400">•</span>}</th>
                      <td className="max-w-sm px-4 py-3"><Value>{diff.currentValue}</Value></td>
                      <td className="max-w-sm px-4 py-3"><Value>{diff.providerValue}</Value></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="grid gap-3 lg:grid-cols-2">
            {review.collectionDiffs?.map(diff => (
              <details key={diff.field} className="rounded-xl border border-zinc-800 bg-zinc-950 p-4" open={diff.changed}>
                <summary className="cursor-pointer text-sm font-medium text-zinc-200">{diff.label} <span className={diff.changed ? 'text-amber-400' : 'text-green-400'}>{diff.changed ? `(+${diff.added.length}/-${diff.removed.length})` : '(không đổi)'}</span></summary>
                <div className="mt-3 grid gap-3 text-xs sm:grid-cols-2">
                  <div><p className="mb-2 font-semibold text-green-400">Provider thêm</p><ul className="space-y-1 text-zinc-400">{diff.added.length ? diff.added.map(item => <li key={item} className="break-all">+ {item}</li>) : <li>—</li>}</ul></div>
                  <div><p className="mb-2 font-semibold text-red-400">Không còn trên provider</p><ul className="space-y-1 text-zinc-400">{diff.removed.length ? diff.removed.map(item => <li key={item} className="break-all">− {item}</li>) : <li>—</li>}</ul></div>
                </div>
              </details>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}
