import { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertTriangle, CheckCircle2, Clock3, ExternalLink, RefreshCw } from 'lucide-react';
import { Link } from 'react-router-dom';
import adminMovieService from '../services/adminMovieService';

const actionLabel = {
  CREATE_SHOWTIME: 'Tạo suất chiếu',
  FIX_PRICING: 'Sửa giá',
  OPEN_SHOWTIME: 'Mở suất',
  REVIEW_SHOWTIME: 'Kiểm tra suất',
  PUBLISH_MOVIE: 'Duyệt phim',
  COMPLETE_MOVIE: 'Hoàn thiện hồ sơ',
  REVIEW_MOVIE: 'Kiểm tra hồ sơ',
};

const issueMessages = {
  RELEASE_DATE_REACHED_WITHOUT_SHOWTIME:
    'Đã tới ngày khai thác nhưng phim chưa có suất chiếu đã công bố. Phim đang được ẩn khỏi trang khách; hãy dời ngày khai thác hoặc chuẩn bị lịch cho đợt mới.',
  NO_FUTURE_SHOWTIME:
    'Phim chưa có suất chiếu nháp hoặc suất đã mở bán trong tương lai. Hãy tạo suất chiếu hoặc mở một suất nháp.',
  NO_OPEN_SHOWTIME:
    'Phim đang ở trạng thái đang chiếu nhưng chưa có suất nào mở bán. Hãy mở một suất chiếu.',
};

const getIssueMessage = issue => issueMessages[issue?.code] || issue?.message;

const issueLink = issue => {
  if (issue?.action === 'CREATE_SHOWTIME') return '/admin/showtimes/create';
  if (issue?.action === 'OPEN_SHOWTIME' && !issue?.showtimePublicId) {
    return '/admin/showtimes?status=DRAFT';
  }
  if (!issue?.showtimePublicId) return null;
  const id = encodeURIComponent(issue.showtimePublicId);
  return issue.action === 'FIX_PRICING'
    ? `/admin/showtimes/${id}/pricing`
    : `/admin/showtimes/${id}`;
};

const formatTime = value => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
  : '—';

export default function MovieLaunchReadinessPanel({ movie }) {
  const moviePublicId = movie?.publicId;
  const [readiness, setReadiness] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const reload = useCallback(async () => {
    if (!moviePublicId) return;
    setLoading(true);
    setError('');
    const [readinessResult, historyResult] = await Promise.allSettled([
      adminMovieService.getMovieLaunchReadiness(moviePublicId),
      adminMovieService.getMovieStatusHistory(moviePublicId),
    ]);
    if (readinessResult.status === 'fulfilled') {
      setReadiness(readinessResult.value?.data || null);
    } else {
      setError('Không thể kiểm tra trạng thái sẵn sàng phát hành.');
    }
    if (historyResult.status === 'fulfilled') {
      setHistory(historyResult.value?.data || []);
    }
    setLoading(false);
  }, [moviePublicId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    reload();
  }, [reload, movie?.status, movie?.activeVersionCount, movie?.mediaCount, movie?.showtimeCount]);

  const issues = useMemo(() => {
    if (!readiness) return [];
    const showtimeIssues = (readiness.showtimes || []).flatMap(item => item.blockers || []);
    return [...(readiness.blockers || []), ...(readiness.warnings || []), ...showtimeIssues]
      .filter((item, index, all) => all.findIndex(candidate => (
        candidate.code === item.code && candidate.showtimePublicId === item.showtimePublicId
      )) === index);
  }, [readiness]);

  if (!movie) return null;

  return (
    <section className="rounded-2xl border border-zinc-800 bg-zinc-900/35" aria-labelledby="launch-readiness-title">
      <div className="flex items-start justify-between gap-4 border-b border-zinc-800 p-5 md:p-6">
        <div>
          <h2 id="launch-readiness-title" className="text-lg font-bold text-white">Sẵn sàng phát hành & mở bán</h2>
          <p className="mt-1 text-sm text-zinc-500">Kiểm tra xuyên suốt hồ sơ phim, suất chiếu và giá trước khi khách hàng nhìn thấy nút đặt vé.</p>
        </div>
        <button type="button" onClick={reload} disabled={loading} aria-label="Kiểm tra lại" className="rounded-lg border border-zinc-700 p-2 text-zinc-400 hover:text-white disabled:opacity-50">
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {error ? (
        <p className="p-5 text-sm text-red-300">{error}</p>
      ) : !readiness ? (
        <p className="p-5 text-sm text-zinc-500">Đang kiểm tra…</p>
      ) : (
        <div className="space-y-5 p-5 md:p-6">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {[
              ['Hồ sơ', readiness.contentReady ? 'Đã đủ' : 'Còn thiếu'],
              ['Có thể duyệt', readiness.publishable ? 'Có' : 'Chưa'],
              ['Suất nháp có thể mở', `${readiness.openableDraftShowtimeCount}/${readiness.draftShowtimeCount}`],
              ['Khách hàng đặt được', readiness.bookable ? 'Đang mở bán' : 'Chưa mở bán'],
            ].map(([label, value]) => (
              <div key={label} className="rounded-xl border border-zinc-800 bg-zinc-950/50 p-3">
                <p className="text-[11px] font-bold uppercase tracking-wide text-zinc-600">{label}</p>
                <p className="mt-1 text-sm font-bold text-zinc-200">{value}</p>
              </div>
            ))}
          </div>

          {issues.length === 0 ? (
            <div className="flex items-center gap-2 rounded-xl border border-emerald-500/20 bg-emerald-500/5 p-3 text-sm text-emerald-300">
              <CheckCircle2 className="h-4 w-4" /> Không còn điều kiện chặn trong phạm vi hiện tại.
            </div>
          ) : (
            <div className="space-y-2">
              <p className="text-xs font-bold uppercase tracking-wide text-zinc-500">Việc cần xử lý ({issues.length})</p>
              {issues.map((issue, index) => {
                const link = issueLink(issue);
                return (
                  <div key={`${issue.code}-${issue.showtimePublicId || index}`} className="flex flex-col gap-2 rounded-xl border border-amber-500/15 bg-amber-500/5 p-3 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex items-start gap-2 text-sm text-amber-100">
                      <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-400" />
                      <span>{getIssueMessage(issue)}</span>
                    </div>
                    {link ? (
                      <Link to={link} className="flex shrink-0 items-center gap-1 text-xs font-bold text-orange-300 hover:text-orange-200">
                        {actionLabel[issue.action] || 'Xử lý'} <ExternalLink className="h-3.5 w-3.5" />
                      </Link>
                    ) : (
                      <span className="shrink-0 text-xs font-bold text-zinc-500">{actionLabel[issue.action] || 'Kiểm tra'}</span>
                    )}
                  </div>
                );
              })}
            </div>
          )}

          {history.length > 0 && (
            <details className="rounded-xl border border-zinc-800 bg-zinc-950/40">
              <summary className="cursor-pointer px-4 py-3 text-sm font-semibold text-zinc-300">Lịch sử trạng thái ({history.length})</summary>
              <div className="space-y-2 border-t border-zinc-800 p-4">
                {history.slice(0, 5).map((item, index) => (
                  <div key={`${item.changedAt}-${index}`} className="flex items-start gap-2 text-xs text-zinc-400">
                    <Clock3 className="mt-0.5 h-3.5 w-3.5 shrink-0" />
                    <span><strong className="text-zinc-200">{item.previousStatus || 'Khởi tạo'} → {item.newStatus}</strong> · {formatTime(item.changedAt)}{item.reason ? ` · ${item.reason}` : ''}</span>
                  </div>
                ))}
              </div>
            </details>
          )}
        </div>
      )}
    </section>
  );
}
