import {
  AlertTriangle,
  CheckCircle2,
  Cloud,
  Database,
  Loader2,
  RefreshCw,
  ShieldAlert,
} from 'lucide-react';

const REVIEW_STATUS_CONFIG = {
  PENDING: { label: 'Chờ duyệt', className: 'border-amber-500/30 bg-amber-500/10 text-amber-300' },
  ACTIVATED: { label: 'Đã đưa vào khai thác', className: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300' },
  INACTIVE: { label: 'Không hoạt động', className: 'border-zinc-600 bg-zinc-800 text-zinc-300' },
  NOT_APPLICABLE: { label: 'Không áp dụng', className: 'border-zinc-700 bg-zinc-900 text-zinc-400' },
};

const HEALTH_STATUS_CONFIG = {
  READY: { label: 'Sẵn sàng', className: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300' },
  WARNING: { label: 'Cần kiểm tra', className: 'border-amber-500/30 bg-amber-500/10 text-amber-300' },
  BLOCKED: { label: 'Bị chặn', className: 'border-red-500/30 bg-red-500/10 text-red-300' },
};

const PANEL_STATUS_CONFIG = {
  LOADING: { label: 'Đang tải', className: 'border-sky-500/30 bg-sky-500/10 text-sky-300' },
  REFRESHING: { label: 'Đang cập nhật', className: 'border-sky-500/30 bg-sky-500/10 text-sky-300' },
  UNAVAILABLE: { label: 'Không khả dụng', className: 'border-red-500/30 bg-red-500/10 text-red-300' },
  STALE: { label: 'Chưa cập nhật', className: 'border-amber-500/30 bg-amber-500/10 text-amber-300' },
  EMPTY: { label: 'Không có dữ liệu', className: 'border-zinc-700 bg-zinc-900 text-zinc-400' },
};

const asArray = value => (Array.isArray(value) ? value : []);

function formatTimestamp(value) {
  if (!value) return 'Chưa có';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? value : parsed.toLocaleString('vi-VN');
}

function Value({ children }) {
  const displayValue = children === null || children === undefined || children === '' ? '—' : children;
  return <span className="whitespace-pre-wrap break-words text-xs leading-5 text-zinc-300">{displayValue}</span>;
}

function StatusBadge({ config, fallback }) {
  const resolved = config || { label: fallback, className: 'border-zinc-700 bg-zinc-900 text-zinc-300' };
  return <span className={`inline-flex rounded-full border px-2.5 py-1 text-[11px] font-semibold ${resolved.className}`}>{resolved.label}</span>;
}

function IssueList({ title, issues, tone }) {
  if (!issues.length) return null;
  const styles = tone === 'danger'
    ? 'border-red-500/20 bg-red-500/5 text-red-200'
    : 'border-amber-500/20 bg-amber-500/5 text-amber-100';

  return (
    <div className={`rounded-xl border p-3 ${styles}`}>
      <p className="text-xs font-semibold">{title}</p>
      <ul className="mt-2 space-y-1.5 text-xs leading-5 opacity-90">
        {issues.map((issue, index) => (
          <li key={`${issue.code || title}-${index}`} className="flex gap-2">
            <span aria-hidden="true">•</span>
            <span>{issue.message || issue.code}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

function CollectionValues({ title, items, emptyLabel }) {
  return (
    <div className="min-w-0">
      <p className="mb-2 text-[11px] font-semibold uppercase tracking-wide text-zinc-500">{title}</p>
      {items.length ? (
        <ul className="max-h-52 space-y-1.5 overflow-y-auto pr-2 text-xs leading-5 text-zinc-300">
          {items.map((item, index) => <li key={`${item}-${index}`} className="break-words rounded-lg bg-zinc-900/70 px-2.5 py-1.5">{item}</li>)}
        </ul>
      ) : (
        <p className="text-xs text-zinc-600">{emptyLabel}</p>
      )}
    </div>
  );
}

export default function TmdbMovieReviewPanel({ movie, review, isLoading, isRefreshing, hasRequested, error, onRetry }) {
  if (movie?.source !== 'TMDB') return null;

  const readiness = review?.readiness;
  const blockers = asArray(readiness?.blockers);
  const warnings = asArray(readiness?.warnings);
  const approvalBlockers = asArray(review?.approvalBlockers);
  const scalarDiffs = asArray(review?.scalarDiffs);
  const collectionDiffs = asArray(review?.collectionDiffs);
  const reviewStatusConfig = REVIEW_STATUS_CONFIG[review?.reviewStatus];
  const healthStatusConfig = HEALTH_STATUS_CONFIG[readiness?.healthStatus];
  const featureDisabled = error?.type === 'FEATURE_DISABLED';
  const showError = Boolean(error && !featureDisabled && !isLoading && !isRefreshing);
  const panelStatusConfig = isRefreshing
    ? PANEL_STATUS_CONFIG.REFRESHING
    : isLoading || !hasRequested
      ? PANEL_STATUS_CONFIG.LOADING
      : featureDisabled
        ? PANEL_STATUS_CONFIG.UNAVAILABLE
        : showError && !review
        ? PANEL_STATUS_CONFIG.UNAVAILABLE
        : showError && review
          ? PANEL_STATUS_CONFIG.STALE
          : review
            ? reviewStatusConfig
            : PANEL_STATUS_CONFIG.EMPTY;

  return (
    <section id="tmdb-review" className="mt-6 overflow-hidden rounded-2xl border border-sky-500/20 bg-[#0a0a0a]" aria-labelledby="tmdb-review-title">
      <div className="flex flex-wrap items-start justify-between gap-4 border-b border-zinc-800 p-5 md:p-6">
        <div className="flex min-w-0 gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-sky-500/10 text-sky-400"><Cloud size={20} /></div>
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <h2 id="tmdb-review-title" className="font-semibold text-white">Rà soát dữ liệu TMDB</h2>
              <StatusBadge config={panelStatusConfig} fallback={review?.reviewStatus || 'Không có dữ liệu'} />
            </div>
            <p className="mt-1 max-w-3xl text-sm leading-5 text-zinc-400">
              So sánh dữ liệu đang lưu với dữ liệu TMDB hiện tại. Kết quả chỉ dùng để hỗ trợ rà soát và không tự động ghi đè thông tin phim.
            </p>
            <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-zinc-500">
              <span>Nguồn: <strong className="font-medium text-sky-300">{review?.source || 'TMDB'}</strong></span>
              <span>TMDB ID: <strong className="font-medium text-zinc-300">{review?.tmdbId || movie.tmdbId || '—'}</strong></span>
            </div>
          </div>
        </div>
        <button
          type="button"
          onClick={onRetry}
          disabled={featureDisabled || isLoading || isRefreshing}
          className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 px-3 py-2 text-xs font-medium text-zinc-300 hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {isLoading || isRefreshing ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
          {featureDisabled ? 'Tích hợp đang tắt' : isRefreshing ? 'Đang cập nhật' : isLoading ? 'Đang tải' : 'Làm mới'}
        </button>
      </div>

      {featureDisabled && (
        <div className="m-4 rounded-xl border border-amber-500/20 bg-amber-500/[0.07] px-4 py-3 text-sm text-amber-200" role="status">
          <p className="font-semibold">Đối chiếu nguồn ngoài không khả dụng</p>
          <p className="mt-1 text-xs leading-5 text-zinc-400">{error.message}</p>
        </div>
      )}

      {(!hasRequested || isLoading) && !review && !error && (
        <div className="flex items-start gap-3 p-6 text-sm text-zinc-400" role="status">
          <Loader2 className="mt-0.5 h-4 w-4 shrink-0 animate-spin" />
          <div>
            <p>Đang tải dữ liệu rà soát TMDB...</p>
            <p className="mt-1 text-xs text-zinc-500">So sánh này mang tính tham khảo; thao tác lifecycle vẫn dựa trên dữ liệu phim đã lưu.</p>
          </div>
        </div>
      )}

      {showError && (
        <div className="m-4 flex flex-wrap items-start justify-between gap-3 rounded-xl border border-red-500/20 bg-red-500/[0.07] px-4 py-3 text-sm text-red-200" role="alert">
          <div className="min-w-0 flex-1">
            <p className="font-semibold">{review ? 'Không thể cập nhật dữ liệu so sánh TMDB' : 'Không thể tải dữ liệu so sánh TMDB'}</p>
            <p className="mt-0.5 text-xs text-red-200/80">{error.message}</p>
            <p className="mt-1 text-xs leading-5 text-zinc-400">
              Dữ liệu phim đã lưu vẫn khả dụng. Việc duyệt phim vẫn dựa trên dữ liệu đã lưu và được máy chủ kiểm tra lại.
            </p>
            {error.technicalDetail && (
              <details className="mt-1 text-[11px] text-zinc-500">
                <summary className="cursor-pointer select-none hover:text-zinc-400">Chi tiết kỹ thuật</summary>
                <p className="mt-1 font-mono">{error.technicalDetail}</p>
              </details>
            )}
          </div>
          <button type="button" onClick={onRetry} disabled={isLoading || isRefreshing} className="shrink-0 rounded-lg border border-red-400/30 px-3 py-1.5 text-xs font-medium hover:bg-red-500/10 disabled:opacity-50">Thử lại</button>
        </div>
      )}

      {hasRequested && !isLoading && !error && !review && (
        <div className="p-6 text-sm text-zinc-500">
          <p>Không có dữ liệu rà soát TMDB.</p>
          <p className="mt-1 text-xs">Dữ liệu phim đã lưu và các thao tác lifecycle vẫn hoạt động độc lập.</p>
        </div>
      )}

      {review && (
        <div className="space-y-6 p-5 md:p-6" aria-live="polite">
          {isRefreshing && <p className="text-xs text-sky-300" role="status">Đang cập nhật dữ liệu TMDB; nội dung hiện tại vẫn được giữ lại.</p>}

          <div className="grid overflow-hidden rounded-xl border border-zinc-800 bg-zinc-950 sm:grid-cols-3 sm:divide-x sm:divide-zinc-800">
            <div className="border-b border-zinc-800 p-4 sm:border-b-0">
              <p className="text-[11px] uppercase tracking-wide text-zinc-500">Dữ liệu từ TMDB</p>
              <p className={`mt-1 text-sm font-semibold ${review.hasProviderChanges ? 'text-amber-300' : 'text-emerald-300'}`}>
                {review.hasProviderChanges ? 'Có thay đổi' : 'Không thay đổi'}
              </p>
            </div>
            <div className="border-b border-zinc-800 p-4 sm:border-b-0">
              <p className="text-[11px] uppercase tracking-wide text-zinc-500">Đã áp dụng lúc</p>
              <p className="mt-1 text-xs leading-5 text-zinc-300">{formatTimestamp(review.appliedTmdbLastUpdated)}</p>
            </div>
            <div className="p-4">
              <p className="text-[11px] uppercase tracking-wide text-zinc-500">TMDB cập nhật</p>
              <p className="mt-1 text-xs leading-5 text-zinc-300">{formatTimestamp(review.providerLastUpdated)}</p>
            </div>
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <section className="rounded-xl border border-zinc-800 bg-zinc-950 p-4" aria-labelledby="tmdb-health-title">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <Database className="h-4 w-4 text-zinc-400" />
                  <h3 id="tmdb-health-title" className="text-sm font-semibold text-white">Tình trạng dữ liệu phim</h3>
                </div>
                <StatusBadge config={healthStatusConfig} fallback={readiness?.healthStatus || 'Không xác định'} />
              </div>
              <div className="mt-4 space-y-3">
                <IssueList title="Điều kiện đang chặn" issues={blockers} tone="danger" />
                <IssueList title="Thông tin cần kiểm tra" issues={warnings} tone="warning" />
                {!blockers.length && !warnings.length && (
                  <div className="flex gap-2 rounded-xl border border-emerald-500/20 bg-emerald-500/5 p-3 text-xs text-emerald-200">
                    <CheckCircle2 className="h-4 w-4 shrink-0" /> Máy chủ không trả về điều kiện chặn hoặc cảnh báo.
                  </div>
                )}
              </div>
            </section>

            <section className="rounded-xl border border-zinc-800 bg-zinc-950 p-4" aria-labelledby="tmdb-approval-title">
              <div className="flex items-center gap-2">
                {review.canApprove ? <CheckCircle2 className="h-4 w-4 text-emerald-400" /> : <ShieldAlert className="h-4 w-4 text-red-400" />}
                <h3 id="tmdb-approval-title" className="text-sm font-semibold text-white">Điều kiện duyệt phim</h3>
              </div>
              <p className={`mt-3 text-sm font-semibold ${review.canApprove ? 'text-emerald-300' : 'text-red-300'}`}>
                {review.canApprove ? 'Đủ điều kiện phê duyệt' : 'Chưa thể phê duyệt'}
              </p>
              {review.approvalTarget && (
                <p className="mt-1 text-xs font-semibold text-sky-300">
                  Trạng thái sau duyệt: {review.approvalTarget === 'NOW_SHOWING' ? 'Đang chiếu' : 'Sắp chiếu'}
                </p>
              )}
              <p className="mt-1 text-xs leading-5 text-zinc-500">
                Kết quả này do máy chủ xác định. Khi xác nhận, vòng đời và tình trạng dữ liệu sẽ được kiểm tra lại trước khi lưu.
              </p>
              {approvalBlockers.length > 0 && (
                <ul className="mt-3 space-y-1.5 rounded-xl border border-red-500/20 bg-red-500/5 p-3 text-xs leading-5 text-red-200">
                  {approvalBlockers.map((item, index) => <li key={`${item}-${index}`} className="flex gap-2"><span aria-hidden="true">•</span><span>{item}</span></li>)}
                </ul>
              )}
              {!review.canApprove && approvalBlockers.length === 0 && (
                <p className="mt-3 rounded-xl border border-zinc-800 bg-zinc-900/70 p-3 text-xs text-zinc-400">Trạng thái rà soát hiện tại chưa cho phép duyệt phim.</p>
              )}
            </section>
          </div>

          <section aria-labelledby="tmdb-scalar-title">
            <div className="mb-3 flex items-center gap-2">
              {review.hasProviderChanges ? <AlertTriangle className="h-4 w-4 text-amber-400" /> : <CheckCircle2 className="h-4 w-4 text-emerald-400" />}
              <h3 id="tmdb-scalar-title" className="text-sm font-semibold text-white">So sánh thông tin phim</h3>
            </div>
            {scalarDiffs.length ? (
              <div className="overflow-x-auto rounded-xl border border-zinc-800">
                <table className="min-w-[680px] divide-y divide-zinc-800 text-left">
                  <caption className="sr-only">So sánh thông tin phim đang lưu và dữ liệu từ TMDB</caption>
                  <thead className="bg-zinc-900 text-[11px] uppercase tracking-wide text-zinc-500">
                    <tr><th className="px-4 py-3">Trường</th><th className="px-4 py-3">Dữ liệu đang lưu</th><th className="px-4 py-3">Dữ liệu từ TMDB</th></tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800/70">
                    {scalarDiffs.map(diff => (
                      <tr key={diff.field} className={diff.changed ? 'bg-amber-500/[0.04]' : ''}>
                        <th className="w-44 px-4 py-3 text-xs font-medium text-zinc-400">
                          <span>{diff.label}</span>
                          {diff.changed && <span className="ml-2 rounded bg-amber-500/10 px-1.5 py-0.5 text-[10px] text-amber-300">Thay đổi</span>}
                        </th>
                        <td className="max-w-sm px-4 py-3 align-top"><Value>{diff.currentValue}</Value></td>
                        <td className="max-w-sm px-4 py-3 align-top"><Value>{diff.providerValue}</Value></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="rounded-xl border border-zinc-800 bg-zinc-950 p-4 text-xs text-zinc-500">Máy chủ không trả về thông tin phim để so sánh.</p>
            )}
          </section>

          <section aria-labelledby="tmdb-relations-title">
            <h3 id="tmdb-relations-title" className="mb-3 text-sm font-semibold text-white">So sánh quan hệ và hình ảnh/video</h3>
            {collectionDiffs.length ? (
              <div className="space-y-3">
                {collectionDiffs.map(diff => {
                  const currentValues = asArray(diff.currentValues);
                  const providerValues = asArray(diff.providerValues);
                  const added = asArray(diff.added);
                  const removed = asArray(diff.removed);
                  return (
                    <details key={diff.field} className="rounded-xl border border-zinc-800 bg-zinc-950 p-4" open={diff.changed}>
                      <summary className="cursor-pointer list-none text-sm font-medium text-zinc-200">
                        <span className="flex flex-wrap items-center justify-between gap-2">
                          <span>{diff.label}</span>
                          <StatusBadge
                            config={diff.changed
                              ? { label: 'Có thay đổi', className: 'border-amber-500/30 bg-amber-500/10 text-amber-300' }
                              : { label: 'Không thay đổi', className: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300' }}
                          />
                        </span>
                      </summary>
                      <div className="mt-4 grid gap-4 border-t border-zinc-800 pt-4 md:grid-cols-2">
                        <CollectionValues title="Dữ liệu đang lưu" items={currentValues} emptyLabel="Không có dữ liệu đang lưu" />
                        <CollectionValues title="Dữ liệu từ TMDB" items={providerValues} emptyLabel="TMDB không trả về dữ liệu" />
                      </div>
                      {diff.changed && (
                        <div className="mt-4 grid gap-3 border-t border-zinc-800 pt-4 md:grid-cols-2">
                          <CollectionValues title="Bổ sung từ TMDB" items={added} emptyLabel="Không có mục bổ sung" />
                          <CollectionValues title="Không còn trên TMDB" items={removed} emptyLabel="Không có mục bị loại bỏ" />
                        </div>
                      )}
                    </details>
                  );
                })}
              </div>
            ) : (
              <p className="rounded-xl border border-zinc-800 bg-zinc-950 p-4 text-xs text-zinc-500">Máy chủ không trả về dữ liệu quan hệ để so sánh.</p>
            )}
          </section>
        </div>
      )}
    </section>
  );
}
