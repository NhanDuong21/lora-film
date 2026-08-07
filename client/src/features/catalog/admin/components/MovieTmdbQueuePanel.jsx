import { AlertTriangle, CheckCircle2, CircleHelp, Clock3, Film, Loader2, ShieldCheck, XCircle } from 'lucide-react';

export default function MovieTmdbQueuePanel({
  breakdown,
  isBreakdownLoading = false,
  breakdownError = '',
  limit = 100,
  approval = {},
  onApprove,
}) {
  const eligibleUpcoming = breakdown?.eligibleUpcoming ?? 0;
  const releaseDateExpired = breakdown?.releaseDateExpired ?? 0;
  const undated = breakdown?.undated ?? 0;
  const eligible = eligibleUpcoming;
  const approvalBatch = Math.min(eligible, limit);

  return (
    <section className="rounded-2xl border border-sky-500/20 bg-sky-500/[0.04] p-4" aria-labelledby="tmdb-queue-title">
      <div className="flex items-start gap-3">
        <div className="rounded-xl border border-sky-500/20 bg-sky-500/10 p-2 text-sky-300">
          <Film className="h-5 w-5" />
        </div>
        <div>
          <h2 id="tmdb-queue-title" className="text-sm font-black uppercase tracking-wide text-sky-200">
            Xử lý phim nhập tự động
          </h2>
          <p className="mt-1 max-w-4xl text-xs leading-5 text-zinc-400">
            Chỉ phim có ngày bắt đầu khai thác sau hôm nay mới được duyệt sang Sắp chiếu. Phim đã tới ngày cần lập
            một đợt khai thác mới trước khi duyệt; không có trường hợp duyệt thẳng từ Chờ hoàn thiện sang Đang chiếu.
          </p>
        </div>
      </div>

      {breakdownError && (
        <div className="mt-4 flex items-start gap-2 rounded-xl border border-red-500/20 bg-red-500/10 p-3 text-xs text-red-200" role="alert">
          <XCircle className="mt-0.5 h-4 w-4 shrink-0" />
          <span>{breakdownError}</span>
        </div>
      )}

      <div className="mt-4 grid gap-3 md:grid-cols-3">
        <QueueBucket
          icon={<Clock3 className="h-4 w-4" />}
          label="Đủ điều kiện về ngày"
          description="Ngày khai thác sau hôm nay; có thể duyệt sang Sắp chiếu khi hồ sơ đầy đủ"
          count={eligibleUpcoming}
          tone="emerald"
        />
        <QueueBucket
          icon={<AlertTriangle className="h-4 w-4" />}
          label="Ngày khai thác không còn hợp lệ"
          description="Ngày là hôm nay hoặc đã qua; cần đổi sang một ngày sau hôm nay để lập đợt mới"
          count={releaseDateExpired}
          tone="amber"
        />
        <QueueBucket
          icon={<CircleHelp className="h-4 w-4" />}
          label="Chưa có ngày bắt đầu khai thác"
          description="Cần mở hồ sơ để bổ sung thời gian khai thác tại rạp"
          count={undated}
          tone="zinc"
        />
      </div>

      <button
        type="button"
        disabled={isBreakdownLoading || eligible === 0 || approval.isPending}
        onClick={onApprove}
        className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-lg bg-emerald-400 px-3 py-3 text-xs font-black uppercase tracking-wide text-zinc-950 transition-colors hover:bg-emerald-300 disabled:cursor-not-allowed disabled:bg-zinc-800 disabled:text-zinc-500"
      >
        {approval.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <ShieldCheck className="h-4 w-4" />}
        {approval.isPending
          ? 'Đang kiểm tra và duyệt'
          : eligible
            ? `Duyệt tối đa ${approvalBatch} phim đủ điều kiện`
            : 'Chưa có phim đủ điều kiện duyệt'}
      </button>

      {approval.error && <InlineError message={approval.error} />}
      {approval.result && (
        <ActionResult
          title="Kết quả duyệt phim sang Sắp chiếu"
          result={approval.result}
          successKey="approved"
          successText="Đã chuyển sang Sắp chiếu"
        />
      )}
    </section>
  );
}

function QueueBucket({ icon, label, description, count, tone }) {
  const tones = {
    emerald: {
      border: 'border-emerald-500/20',
      icon: 'border-emerald-500/20 bg-emerald-500/10 text-emerald-300',
      count: 'text-emerald-300',
    },
    sky: {
      border: 'border-sky-500/20',
      icon: 'border-sky-500/20 bg-sky-500/10 text-sky-300',
      count: 'text-sky-300',
    },
    amber: {
      border: 'border-amber-500/20',
      icon: 'border-amber-500/20 bg-amber-500/10 text-amber-300',
      count: 'text-amber-300',
    },
    zinc: {
      border: 'border-zinc-800',
      icon: 'border-zinc-700 bg-zinc-800/70 text-zinc-400',
      count: 'text-zinc-300',
    },
  };
  const palette = tones[tone];

  return (
    <div className={`rounded-xl border ${palette.border} bg-zinc-950/60 p-4`}>
      <div className="flex items-center justify-between gap-3">
        <div className="flex min-w-0 items-center gap-2">
          <div className={`rounded-lg border p-1.5 ${palette.icon}`}>{icon}</div>
          <span className="truncate text-xs font-black uppercase tracking-wide text-zinc-200">{label}</span>
        </div>
        <span className={`text-2xl font-black ${palette.count}`}>{count}</span>
      </div>
      <p className="mt-2 min-h-10 text-xs leading-5 text-zinc-500">{description}</p>
    </div>
  );
}

function InlineError({ message }) {
  return (
    <div className="mt-3 flex items-start gap-2 rounded-xl border border-red-500/20 bg-red-500/10 p-3 text-xs text-red-200" role="alert">
      <XCircle className="mt-0.5 h-4 w-4 shrink-0" />
      <span>{message}</span>
    </div>
  );
}

function ActionResult({ title, result, successKey, successText }) {
  const skippedItems = result.results?.filter(item => item.outcome === 'SKIPPED') || [];
  const errorItems = result.results?.filter(item => item.outcome === 'ERROR') || [];
  const successCount = result[successKey] ?? 0;

  return (
    <div className="mt-4 rounded-xl border border-zinc-800 bg-zinc-950/60 p-3" aria-live="polite">
      <p className="text-xs font-black uppercase tracking-wide text-zinc-300">{title}</p>
      <div className="mt-3 grid gap-2 sm:grid-cols-4">
        <ResultStat label="Đã kiểm tra" value={result.requested} />
        <ResultStat label="Thành công" value={successCount} tone="text-emerald-300" />
        <ResultStat label="Bỏ qua" value={result.skipped} tone="text-amber-300" />
        <ResultStat label="Lỗi" value={result.errors} tone="text-red-300" />
      </div>
      {successCount > 0 && (
        <div className="mt-3 flex items-center gap-2 text-xs text-emerald-200">
          <CheckCircle2 className="h-4 w-4 shrink-0" />
          {successText}: {successCount} phim.
        </div>
      )}
      {(skippedItems.length > 0 || errorItems.length > 0) && (
        <details className="mt-3 rounded-lg border border-zinc-800 bg-zinc-900/60 p-3">
          <summary className="cursor-pointer text-xs font-bold text-zinc-300">
            Xem {skippedItems.length + errorItems.length} kết quả chưa thành công
          </summary>
          <ul className="mt-3 max-h-48 space-y-2 overflow-auto pr-1">
            {[...skippedItems, ...errorItems].map(item => (
              <li key={item.moviePublicId} className="flex items-start gap-2 rounded-lg bg-zinc-950 px-3 py-2 text-xs">
                <AlertTriangle className={`mt-0.5 h-3.5 w-3.5 shrink-0 ${item.outcome === 'ERROR' ? 'text-red-400' : 'text-amber-400'}`} />
                <span className="min-w-0">
                  <strong className="block truncate text-zinc-200">{item.title || item.moviePublicId}</strong>
                  <span className="text-zinc-500">{item.reason || item.reasonCode || 'Không xác định được nguyên nhân.'}</span>
                </span>
              </li>
            ))}
          </ul>
        </details>
      )}
    </div>
  );
}

function ResultStat({ label, value, tone = 'text-zinc-100' }) {
  return (
    <div className="rounded-lg border border-zinc-800 bg-zinc-900/70 px-3 py-2">
      <p className="text-[10px] font-bold uppercase tracking-wide text-zinc-500">{label}</p>
      <p className={`mt-1 text-lg font-black ${tone}`}>{value ?? 0}</p>
    </div>
  );
}
