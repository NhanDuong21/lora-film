import { useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  CircleHelp,
  ShieldCheck,
  XCircle,
} from 'lucide-react';
import { useOutletContext } from 'react-router-dom';
import { MOVIE_TRANSITIONS } from '../config/movieLifecycleConfig';
import { getMovieReadinessView, getPublishChecklist } from '../utils/movieReadiness';
import useMovieStatusTransition from '../hooks/useMovieStatusTransition';
import MovieStatusTransitionDialog from './MovieStatusTransitionDialog';

const CHECKLIST = [
  { key: 'hasGenre', label: 'Chọn ít nhất một thể loại', tab: 'genres' },
  { key: 'hasActiveVersion', label: 'Có bản chiếu đang hoạt động', tab: 'versions' },
  { key: 'hasPrimaryPoster', label: 'Có poster chính đang hoạt động', tab: 'media' },
];

export default function MovieLifecycleReviewPanel({ movie, tmdbReview, onUpdate, onNavigateToTab }) {
  const [selectedTransition, setSelectedTransition] = useState(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [warningAcknowledged, setWarningAcknowledged] = useState(false);
  const [transitionReason, setTransitionReason] = useState('');
  const { triggerToast } = useOutletContext() || {};

  const handleSuccess = async () => {
    triggerToast?.('Đã cập nhật trạng thái phim.', 'success');
    setIsDialogOpen(false);
    await onUpdate?.();
  };

  const {
    isPending,
    error,
    transitionStatus,
    resetError,
  } = useMovieStatusTransition(movie?.publicId, handleSuccess);

  if (!movie) return null;

  const currentStatus = movie.status || 'UNKNOWN';
  const releaseDate = movie.releaseDate ? new Date(`${movie.releaseDate}T00:00:00`) : null;
  const inferredApprovalTarget = tmdbReview?.approvalTarget
    || (releaseDate && releaseDate <= new Date() ? 'NOW_SHOWING' : 'UPCOMING');
  const configuredTransitions = MOVIE_TRANSITIONS[currentStatus] || [];
  const allowedTransitions = currentStatus === 'DRAFT'
    ? configuredTransitions.filter(transition => (
        !transition.requiresPublishChecklist || transition.target === inferredApprovalTarget
      ))
    : configuredTransitions;
  const readiness = getMovieReadinessView(movie);
  const checklist = getPublishChecklist(readiness);
  const isDraft = currentStatus === 'DRAFT';
  const passedCount = CHECKLIST.filter(item => checklist[item.key] === 'PASS').length;
  const hasMissing = CHECKLIST.some(item => checklist[item.key] === 'MISSING');

  if (!isDraft && allowedTransitions.length === 0) return null;

  const handleActionClick = transition => {
    resetError();
    setSelectedTransition(transition);
    setWarningAcknowledged(false);
    setTransitionReason('');
    setIsDialogOpen(true);
  };

  const handleConfirmTransition = async () => {
    if (selectedTransition) await transitionStatus(selectedTransition.target, transitionReason);
  };

  const renderChecklistStatus = status => {
    if (status === 'PASS') {
      return {
        icon: CheckCircle2,
        label: 'Đã đủ',
        className: 'border-emerald-500/20 bg-emerald-500/5 text-emerald-300',
      };
    }
    if (status === 'MISSING') {
      return {
        icon: XCircle,
        label: 'Cần bổ sung',
        className: 'border-red-500/20 bg-red-500/5 text-red-300',
      };
    }
    return {
      icon: CircleHelp,
      label: 'Đang kiểm tra',
      className: 'border-zinc-800 bg-zinc-900/60 text-zinc-400',
    };
  };

  return (
    <>
      <section className="rounded-2xl border border-zinc-800 bg-zinc-900/35" aria-labelledby="movie-review-title">
        <div className="flex flex-col gap-4 border-b border-zinc-800 p-5 md:flex-row md:items-start md:justify-between md:p-6">
          <div className="flex items-start gap-3">
            <span className="rounded-xl bg-orange-500/10 p-2.5 text-orange-400">
              <ShieldCheck className="h-5 w-5" />
            </span>
            <div>
              <h2 id="movie-review-title" className="text-lg font-bold text-white">
                {isDraft ? 'Kiểm tra trước khi duyệt phim' : 'Trạng thái vận hành'}
              </h2>
              <p className="mt-1 max-w-2xl text-sm leading-6 text-zinc-500">
                {isDraft
                  ? inferredApprovalTarget === 'NOW_SHOWING'
                    ? 'Phim đã tới ngày bắt đầu khai thác. Hãy hoàn thiện dữ liệu và lập ít nhất một suất chiếu hợp lệ trước khi duyệt sang Đang chiếu.'
                    : 'Phim chưa tới ngày bắt đầu khai thác. Khi đủ dữ liệu bắt buộc, phim sẽ được duyệt sang Sắp chiếu.'
                  : 'Các thao tác bên dưới thay đổi việc phim có được hiển thị và nhận lịch chiếu hay không.'}
              </p>
            </div>
          </div>
          {isDraft && (
            <div className="shrink-0 rounded-xl border border-zinc-800 bg-zinc-950/70 px-4 py-3 text-left md:text-right">
              <p className="text-[11px] font-bold uppercase tracking-wide text-zinc-500">Tiến độ bắt buộc</p>
              <p className={`mt-1 text-xl font-black ${passedCount === CHECKLIST.length ? 'text-emerald-300' : 'text-orange-300'}`}>
                {passedCount}/{CHECKLIST.length}
              </p>
            </div>
          )}
        </div>

        {isDraft && (
          <div className="p-5 md:p-6">
            <div className="grid gap-2 md:grid-cols-3">
              {CHECKLIST.map(item => {
                const status = renderChecklistStatus(checklist[item.key]);
                const Icon = status.icon;
                const isMissing = checklist[item.key] === 'MISSING';
                return (
                  <div key={item.key} className={`rounded-xl border p-3 ${status.className}`}>
                    <div className="flex items-start gap-2">
                      <Icon className="mt-0.5 h-4 w-4 shrink-0" />
                      <div className="min-w-0">
                        <p className="text-sm font-semibold">{item.label}</p>
                        <p className="mt-1 text-xs opacity-80">{status.label}</p>
                        {isMissing && onNavigateToTab && (
                          <button
                            type="button"
                            onClick={() => onNavigateToTab(item.tab)}
                            className="mt-2 text-xs font-bold underline underline-offset-4"
                          >
                            Bổ sung ngay
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="mt-4 rounded-xl border border-zinc-800 bg-zinc-950/50 p-3">
              <div className="flex items-start gap-2 text-sm">
                {readiness.healthStatus === 'UNKNOWN' ? (
                  <>
                    <CircleHelp className="mt-0.5 h-4 w-4 shrink-0 text-zinc-500" />
                    <span className="text-zinc-400">Máy chủ chưa trả về kết quả kiểm tra đầy đủ.</span>
                  </>
                ) : readiness.warnings.length > 0 ? (
                  <>
                    <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-400" />
                    <span className="text-amber-200">{readiness.warnings[0].message || readiness.warnings[0].code}</span>
                  </>
                ) : (
                  <>
                    <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-400" />
                    <span className="text-emerald-200">Không có cảnh báo bổ sung.</span>
                  </>
                )}
              </div>
              {readiness.warnings.length > 1 && (
                <p className="mt-2 pl-6 text-xs text-zinc-500">
                  Còn {readiness.warnings.length - 1} cảnh báo khác trong hồ sơ.
                </p>
              )}
            </div>
          </div>
        )}

        {allowedTransitions.length > 0 && (
          <div className="flex flex-col gap-3 border-t border-zinc-800 p-5 sm:flex-row sm:items-center sm:justify-between md:px-6">
            <p className="text-xs text-zinc-500">
              {hasMissing
                ? 'Phim chưa thể duyệt. Hãy bổ sung đủ các mục bắt buộc.'
                : 'Bạn đang chuẩn bị thay đổi trạng thái phục vụ của phim.'}
            </p>
            <div className="flex flex-wrap gap-2">
              {allowedTransitions.map((transition, index) => {
                let isDisabled = false;
                let disableReason = '';
                if (transition.requiresPublishChecklist) {
                  if (currentStatus === 'DRAFT' && !movie.releaseDate) {
                    isDisabled = true;
                    disableReason = 'Cần bổ sung ngày bắt đầu khai thác tại rạp trước khi duyệt.';
                  } else if (
                    currentStatus === 'ENDED'
                    && transition.target === 'UPCOMING'
                    && (!releaseDate || releaseDate <= new Date())
                  ) {
                    isDisabled = true;
                    disableReason = 'Hãy lập một đợt khai thác mới có ngày bắt đầu sau hôm nay.';
                  } else if (readiness.healthStatus === 'BLOCKED') {
                    isDisabled = true;
                    disableReason = 'Còn mục bắt buộc chưa hoàn thiện.';
                  } else if (readiness.healthStatus === 'UNKNOWN') {
                    isDisabled = true;
                    disableReason = 'Chưa kiểm tra được điều kiện phát hành.';
                  }
                  if (movie.source === 'TMDB' && currentStatus === 'DRAFT' && tmdbReview?.canApprove === false) {
                    isDisabled = true;
                    disableReason = tmdbReview.approvalBlockers?.[0] || 'Phim nhập tự động chưa đủ điều kiện duyệt.';
                  }
                  if (currentStatus === 'DRAFT'
                    && transition.target === 'NOW_SHOWING'
                    && !tmdbReview
                    && !(movie.showtimeCount > 0)) {
                    isDisabled = true;
                    disableReason = 'Cần lập ít nhất một suất chiếu hiện tại hoặc tương lai trước khi duyệt.';
                  }
                }

                return (
                  <div key={transition.target} className="flex flex-col items-end gap-1">
                    <button
                      type="button"
                      onClick={() => handleActionClick(transition)}
                      disabled={isDisabled || isPending}
                      className={`rounded-xl px-4 py-2.5 text-sm font-bold transition disabled:cursor-not-allowed disabled:opacity-40 ${
                        transition.variant === 'danger'
                          ? 'bg-red-500/10 text-red-300 hover:bg-red-500/20'
                          : index === 0
                            ? 'bg-orange-500 text-zinc-950 hover:bg-orange-400'
                            : 'border border-zinc-700 text-zinc-300 hover:bg-zinc-800'
                      }`}
                    >
                      {transition.label}
                    </button>
                    {isDisabled && <span className="max-w-56 text-right text-[11px] text-red-300">{disableReason}</span>}
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </section>

      <MovieStatusTransitionDialog
        isOpen={isDialogOpen}
        onClose={() => !isPending && setIsDialogOpen(false)}
        config={selectedTransition}
        checklist={checklist}
        readiness={readiness}
        isPending={isPending}
        error={error}
        warningAcknowledged={warningAcknowledged}
        onWarningAcknowledged={setWarningAcknowledged}
        reason={transitionReason}
        onReasonChange={setTransitionReason}
        onConfirm={handleConfirmTransition}
      />
    </>
  );
}
