import { AlertTriangle, CheckCircle2, XCircle, HelpCircle, Loader2 } from 'lucide-react';

export default function MovieStatusTransitionDialog({
  isOpen,
  onClose,
  config,
  checklist,
  readiness,
  isPending,
  error,
  warningAcknowledged,
  onWarningAcknowledged,
  reason,
  onReasonChange,
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
                  {renderChecklistItem('Có áp phích chính', checklist.hasPrimaryPoster)}
                </div>
              </div>

              {readiness?.warnings?.map((warning, index) => (
                <div key={`${warning.code || 'warning'}-${index}`} className="bg-amber-500/10 border border-amber-500/20 rounded-xl p-4 flex gap-3 text-amber-500">
                  <AlertTriangle size={18} className="shrink-0" />
                  <div className="text-sm">
                    <strong>Thông tin cần kiểm tra:</strong> {warning.message || warning.code}
                  </div>
                </div>
              ))}

              {readiness?.warnings?.length > 0 && (
                <label className="flex cursor-pointer items-start gap-3 rounded-xl border border-amber-500/20 bg-amber-500/5 p-4 text-sm text-amber-100">
                  <input type="checkbox" checked={warningAcknowledged} onChange={event => onWarningAcknowledged(event.target.checked)} className="mt-0.5 h-4 w-4 accent-amber-500" />
                  <span>Tôi đã kiểm tra các cảnh báo và vẫn muốn tiếp tục chuyển trạng thái.</span>
                </label>
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

          <label className="mb-6 block text-sm text-zinc-300">
            <span className="mb-2 block font-medium">Ghi chú vận hành (không bắt buộc)</span>
            <textarea
              value={reason || ''}
              onChange={event => onReasonChange?.(event.target.value)}
              maxLength={500}
              rows={3}
              placeholder="Ví dụ: Đã kiểm tra nội dung và lịch chiếu tuần đầu."
              className="w-full resize-none rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2.5 text-sm text-white outline-none focus:border-brand-orange"
            />
            <span className="mt-1 block text-right text-xs text-zinc-600">{reason?.length || 0}/500</span>
          </label>

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
              disabled={isPending || (config.requiresPublishChecklist && (!checklist?.isReady || (readiness?.warnings?.length > 0 && !warningAcknowledged)))}
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
