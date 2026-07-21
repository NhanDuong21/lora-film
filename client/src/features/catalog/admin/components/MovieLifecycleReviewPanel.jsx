import { useState } from 'react';
import { ShieldCheck, CheckCircle2, XCircle, AlertTriangle, HelpCircle } from 'lucide-react';
import { useOutletContext } from 'react-router-dom';
import { MOVIE_TRANSITIONS } from '../config/movieLifecycleConfig';
import { evaluatePublishChecklist } from '../utils/movieLifecycleChecklist';
import useMovieStatusTransition from '../hooks/useMovieStatusTransition';
import MovieStatusTransitionDialog from './MovieStatusTransitionDialog';

export default function MovieLifecycleReviewPanel({ movie, onUpdate }) {
  const [selectedTransition, setSelectedTransition] = useState(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const { triggerToast } = useOutletContext() || {};

  const handleSuccess = async () => {
    if (triggerToast) triggerToast('Cập nhật trạng thái phim thành công!', 'success');
    setIsDialogOpen(false);
    if (onUpdate) {
      await onUpdate();
    }
  };

  const { isPending, error, transitionStatus, resetError } = useMovieStatusTransition(movie?.publicId, handleSuccess);

  if (!movie) return null;

  const currentStatus = movie.status || 'UNKNOWN';
  const allowedTransitions = MOVIE_TRANSITIONS[currentStatus] || [];
  const checklist = evaluatePublishChecklist(movie);

  const handleActionClick = (transition) => {
    resetError();
    setSelectedTransition(transition);
    setIsDialogOpen(true);
  };

  const handleConfirmTransition = async () => {
    if (!selectedTransition) return;
    await transitionStatus(selectedTransition.target);
  };

  const handleCloseDialog = () => {
    if (!isPending) {
      setIsDialogOpen(false);
      setTimeout(() => setSelectedTransition(null), 200);
    }
  };

  const renderChecklistItem = (label, status) => {
    let Icon = HelpCircle;
    let colorClass = 'text-zinc-500';
    let statusText = 'Đang kiểm tra...';

    if (status === 'PASS') {
      Icon = CheckCircle2;
      colorClass = 'text-green-500';
      statusText = 'Đạt';
    } else if (status === 'MISSING') {
      Icon = XCircle;
      colorClass = 'text-red-500';
      statusText = 'Thiếu';
    }

    return (
      <div className="flex flex-col gap-1 p-3 bg-zinc-900/50 rounded-xl border border-zinc-800">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Icon size={18} className={colorClass} />
            <span className="text-sm text-zinc-300">{label}</span>
          </div>
          <span className={`text-xs font-medium px-2 py-1 rounded-md ${
            status === 'PASS' ? 'bg-green-500/10 text-green-500' :
            status === 'MISSING' ? 'bg-red-500/10 text-red-500' :
            'bg-zinc-800 text-zinc-400'
          }`}>
            {statusText}
          </span>
        </div>
      </div>
    );
  };

  const isDurationShort = movie.durationMinutes && movie.durationMinutes > 0 && movie.durationMinutes < 30;
  const isDraft = currentStatus === 'DRAFT';

  // Always show review panel for DRAFT, or if there are lifecycle actions
  if (!isDraft && allowedTransitions.length === 0) return null;

  return (
    <>
      <div className="bg-[#0a0a0a] border border-zinc-800 rounded-2xl overflow-hidden mt-6">
        <div className="p-6">
          <div className="flex items-center gap-3 mb-6">
            <div className="w-10 h-10 rounded-xl bg-brand-orange/10 flex items-center justify-center text-brand-orange">
              <ShieldCheck size={20} />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-white">Kiểm tra trước khi chuyển trạng thái</h3>
              <p className="text-sm text-zinc-400">Điều kiện đưa vào khai thác và quản lý vòng đời phim</p>
            </div>
          </div>

          {isDraft && (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
              <div className="space-y-4">
                <h4 className="text-sm font-medium text-zinc-300">Điều kiện bắt buộc (Backend xác thực)</h4>
                <div className="space-y-2">
                  {renderChecklistItem('Có thể loại', checklist.hasGenre)}
                  {renderChecklistItem('Có phiên bản đang hoạt động', checklist.hasActiveVersion)}
                  {renderChecklistItem('Có media poster chính đang hoạt động', checklist.hasPrimaryPoster)}
                </div>
              </div>

              <div className="space-y-4">
                <h4 className="text-sm font-medium text-zinc-300">Thông tin nên kiểm tra (Cảnh báo)</h4>
                <div className="space-y-2">
                  {isDurationShort ? (
                    <div className="flex items-center gap-3 p-3 bg-amber-500/10 border border-amber-500/20 rounded-xl text-amber-500">
                      <AlertTriangle size={18} className="shrink-0" />
                      <span className="text-sm text-amber-500/90">Thời lượng phim chỉ {movie.durationMinutes} phút</span>
                    </div>
                  ) : (
                    <div className="flex items-center gap-3 p-3 bg-green-500/10 border border-green-500/20 rounded-xl text-green-500">
                      <CheckCircle2 size={18} className="shrink-0" />
                      <span className="text-sm text-green-500/90">Thời lượng hợp lý</span>
                    </div>
                  )}
                  {/* Add more non-blocking warnings here if needed */}
                </div>
              </div>
            </div>
          )}

          {allowedTransitions.length > 0 && (
            <div className="flex flex-wrap items-center gap-3 pt-6 border-t border-zinc-800">
              {allowedTransitions.map((transition, index) => {
                const isPrimary = index === 0 && transition.variant === 'primary';
                const isWarning = transition.variant === 'warning';
                const isSecondary = transition.variant === 'secondary';
                
                let isDisabled = false;
                let disableReason = '';

                if (transition.target === 'UPCOMING' && isDraft) {
                  const hasMissing = checklist.hasGenre === 'MISSING' || checklist.hasActiveVersion === 'MISSING' || checklist.hasPrimaryPoster === 'MISSING';
                  const hasUnknown = checklist.hasGenre === 'UNKNOWN' || checklist.hasActiveVersion === 'UNKNOWN' || checklist.hasPrimaryPoster === 'UNKNOWN';
                  
                  if (hasMissing) {
                    isDisabled = true;
                    disableReason = 'Chưa thể duyệt phim. Hãy bổ sung các điều kiện còn thiếu.';
                  } else if (hasUnknown) {
                    isDisabled = true;
                    disableReason = 'Chưa thể xác minh điều kiện phát hành.';
                  }
                }
                
                let buttonClass = 'px-4 py-2 rounded-xl text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed ';
                if (transition.variant === 'danger') {
                  buttonClass += 'bg-red-500/10 text-red-500 hover:bg-red-500/20';
                } else if (isWarning) {
                  buttonClass += 'bg-amber-500/10 text-amber-500 hover:bg-amber-500/20';
                } else if (isSecondary) {
                  buttonClass += 'border border-zinc-700 text-zinc-300 hover:bg-zinc-800';
                } else if (isPrimary) {
                  buttonClass += 'bg-brand-orange text-black hover:bg-brand-orange/90';
                } else {
                  buttonClass += 'bg-zinc-800 text-white hover:bg-zinc-700';
                }
                
                return (
                  <div key={transition.target} className="flex flex-col gap-1 items-start">
                    <button
                      className={buttonClass}
                      onClick={() => handleActionClick(transition)}
                      disabled={isDisabled}
                    >
                      {transition.label}
                    </button>
                    {isDisabled && disableReason && (
                      <span className="text-xs text-red-400 mt-1">{disableReason}</span>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      <MovieStatusTransitionDialog
        isOpen={isDialogOpen}
        onClose={handleCloseDialog}
        config={selectedTransition}
        checklist={checklist}
        movieDetail={movie}
        isPending={isPending}
        error={error}
        onConfirm={handleConfirmTransition}
      />
    </>
  );
}
