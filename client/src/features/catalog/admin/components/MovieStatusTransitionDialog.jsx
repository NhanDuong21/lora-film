import { AlertTriangle, CheckCircle2, XCircle, HelpCircle, Loader2 } from 'lucide-react';

export default function MovieStatusTransitionDialog({
  isOpen,
  onClose,
  config,
  checklist,
  movieDetail,
  isPending,
  error,
  onConfirm
}) {
  if (!isOpen || !config) return null;

  const renderChecklistItem = (label, status) => {
    let Icon = HelpCircle;
    let colorClass = 'text-zinc-500';

    if (status === 'PASS') {
      Icon = CheckCircle2;
      colorClass = 'text-green-500';
    } else if (status === 'MISSING') {
      Icon = XCircle;
      colorClass = 'text-red-500';
    }

    return (
      <div className="flex items-center gap-2 text-sm">
        <Icon size={16} className={colorClass} />
        <span className="text-zinc-300">{label}</span>
      </div>
    );
  };

  const isDurationShort = movieDetail?.durationMinutes && movieDetail.durationMinutes > 0 && movieDetail.durationMinutes < 30;

  return (
    <div className="relative z-50">
      <div className="fixed inset-0 bg-black/80 backdrop-blur-sm" aria-hidden="true" onClick={() => { if (!isPending) onClose(); }} />
      
      <div className="fixed inset-0 flex items-center justify-center p-4 pointer-events-none">
        <div className="mx-auto max-w-md rounded-2xl bg-zinc-900 border border-zinc-800 p-6 shadow-xl w-full max-h-[90vh] overflow-y-auto pointer-events-auto">
          <h2 className="text-lg font-semibold text-white mb-2">
            {config.confirmTitle}
          </h2>
          
          <p className="text-sm text-zinc-400 mb-6">
            {config.confirmDescription}
          </p>

          {config.requiresPublishChecklist && checklist && (
            <div className="mb-6 space-y-4">
              <div className="bg-zinc-950 rounded-xl p-4 border border-zinc-800">
                <h4 className="text-sm font-medium text-white mb-3">Điều kiện phát hành:</h4>
                <div className="space-y-2">
                  {renderChecklistItem('Có thể loại', checklist.hasGenre)}
                  {renderChecklistItem('Có phiên bản đang hoạt động', checklist.hasActiveVersion)}
                  {renderChecklistItem('Có poster chính', checklist.hasPrimaryPoster)}
                </div>
              </div>

              {isDurationShort && (
                <div className="bg-amber-500/10 border border-amber-500/20 rounded-xl p-4 flex gap-3 text-amber-500">
                  <AlertTriangle size={18} className="shrink-0" />
                  <div className="text-sm">
                    <strong>Thông tin cần kiểm tra:</strong> Thời lượng phim chỉ {movieDetail.durationMinutes} phút.
                  </div>
                </div>
              )}
            </div>
          )}

          {error && (
            <div className="mb-6 bg-red-500/10 border border-red-500/20 rounded-xl p-4 flex gap-3 text-red-500">
              <XCircle size={18} className="shrink-0" />
              <div className="text-sm">
                <strong>Không thể chuyển trạng thái:</strong> {error}
              </div>
            </div>
          )}

          <div className="flex justify-end gap-3 mt-8">
            <button
              className="px-4 py-2 rounded-xl text-sm font-medium transition-colors bg-zinc-800 text-white hover:bg-zinc-700 disabled:opacity-50 disabled:cursor-not-allowed"
              onClick={onClose}
              disabled={isPending}
            >
              Hủy
            </button>
            <button
              className={`px-4 py-2 rounded-xl text-sm font-medium transition-colors flex items-center justify-center min-w-[100px] disabled:opacity-50 disabled:cursor-not-allowed ${
                config.variant === 'danger' 
                  ? 'bg-red-500/10 text-red-500 hover:bg-red-500/20' 
                  : 'bg-brand-orange text-black hover:bg-brand-orange/90'
              }`}
              onClick={onConfirm}
              disabled={isPending}
            >
              {isPending ? (
                <div className="flex items-center gap-2">
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Đang xử lý...
                </div>
              ) : 'Xác nhận'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
