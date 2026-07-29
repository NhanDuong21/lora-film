import { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { Check, ExternalLink, Eye, X } from 'lucide-react';
import { formatServiceDateKey } from '@/features/scheduling/admin/utils/autoSchedulePreviewDateTime';
import { getCandidateValidationPresentation } from '@/features/scheduling/admin/utils/schedulingPresentation';

const FOCUSABLE_SELECTOR = [
  'button:not([disabled])',
  'a[href]',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  'summary',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

const DetailRow = ({ label, children }) => (
  <div className="grid grid-cols-[130px_1fr] gap-3 border-b border-zinc-800/70 py-2.5 last:border-b-0">
    <dt className="text-xs font-bold text-zinc-500">{label}</dt>
    <dd className="min-w-0 text-sm text-zinc-200">{children || '—'}</dd>
  </div>
);

const AutoScheduleCandidateDrawer = ({
  candidate,
  timezone,
  capabilities,
  selectionBlockedMessage,
  onToggleSelection,
  canInspectOnTimeline,
  onShowDiagnostic,
  onClearDiagnostic,
  onClose,
  returnFocusElement,
}) => {
  const panelRef = useRef(null);
  const closeButtonRef = useRef(null);

  useEffect(() => {
    if (!candidate) return undefined;
    const frame = requestAnimationFrame(() => closeButtonRef.current?.focus());
    const handleKeyDown = event => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onClose();
        return;
      }
      if (event.key !== 'Tab' || !panelRef.current) return;

      const focusable = Array.from(panelRef.current.querySelectorAll(FOCUSABLE_SELECTOR));
      if (focusable.length === 0) {
        event.preventDefault();
        panelRef.current.focus();
        return;
      }
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      cancelAnimationFrame(frame);
      document.removeEventListener('keydown', handleKeyDown);
      returnFocusElement?.focus?.();
    };
  }, [candidate, onClose, returnFocusElement]);

  if (!candidate) return null;

  const canOfferSelection = capabilities.isEditable
    && candidate.validationStatus === 'VALID'
    && candidate.applyStatus === 'PENDING';
  const selectionDisabled = !capabilities.canSelect || Boolean(selectionBlockedMessage);

  return (
    <div
      className="fixed inset-0 z-50 flex justify-end bg-black/65 backdrop-blur-sm"
      onMouseDown={event => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <aside
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="candidate-drawer-title"
        aria-describedby="candidate-drawer-description"
        tabIndex={-1}
        className="flex h-full w-full max-w-lg flex-col border-l border-zinc-800 bg-zinc-950 shadow-2xl"
      >
        <header className="flex items-start justify-between gap-4 border-b border-zinc-800 p-5">
          <div>
            <p className="text-[10px] font-black uppercase tracking-[0.2em] text-brand-orange">Chi tiết phương án</p>
            <h2 id="candidate-drawer-title" className="mt-1 text-xl font-black text-white">{candidate.movieTitle}</h2>
            <p id="candidate-drawer-description" className="mt-1 text-sm text-zinc-400">{candidate.versionName} · {candidate.auditoriumName}</p>
          </div>
          <button ref={closeButtonRef} type="button" onClick={onClose} aria-label="Đóng chi tiết phương án" className="rounded-xl p-2 text-zinc-400 hover:bg-zinc-800 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange">
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </header>

        <div className="flex-1 overflow-y-auto p-5">
          {candidate.diagnostic && (
            <div className="mb-4 rounded-xl border border-dashed border-blue-400/60 bg-blue-500/10 p-3 text-sm text-blue-200">
              Ứng viên này đang được phủ chẩn đoán trên timeline và không làm thay đổi lựa chọn backend.
            </div>
          )}
          <dl>
            <DetailRow label="Phim / phiên bản">{candidate.movieTitle} · {candidate.versionName}</DetailRow>
            <DetailRow label="Phòng chiếu">{candidate.auditoriumName}</DetailRow>
            <DetailRow label="Ngày vận hành">{formatServiceDateKey(candidate.serviceDate)}</DetailRow>
            <DetailRow label="Múi giờ">{timezone}</DetailRow>
            <DetailRow label="Bắt đầu">{candidate.startDateTimeDisplay}</DetailRow>
            <DetailRow label="Kết thúc phim">{candidate.endDateTimeDisplay}</DetailRow>
            <DetailRow label="Hết chiếm phòng">{candidate.occupancyEndDateTimeDisplay}</DetailRow>
            <DetailRow label="Điểm ưu tiên">{candidate.score ?? '—'} <span className="text-xs text-zinc-500">(cao hơn tốt hơn)</span></DetailRow>
            <DetailRow label="Hạng toàn cục">{candidate.rank ?? '—'} <span className="text-xs text-zinc-500">(thứ tự hiển thị, không phải thứ tự chọn)</span></DetailRow>
            <DetailRow label="Kiểm tra">{getCandidateValidationPresentation(candidate.validationStatus).label}</DetailRow>
            <DetailRow label="Áp dụng">{candidate.applyState.label}</DetailRow>
            <DetailRow label="Lý do">{candidate.conciseReason || 'Không có'}</DetailRow>
            {candidate.createdShowtimePath && (
              <DetailRow label="Suất chiếu đã tạo">
                <Link to={candidate.createdShowtimePath} className="inline-flex items-center gap-1 font-bold text-brand-orange hover:underline">
                  {candidate.createdShowtimePublicId}<ExternalLink className="h-3.5 w-3.5" aria-hidden="true" />
                </Link>
              </DetailRow>
            )}
          </dl>

          {canOfferSelection && (
            <div className="mt-5 rounded-xl border border-zinc-800 bg-zinc-900 p-4">
              <button
                type="button"
                disabled={selectionDisabled}
                onClick={() => onToggleSelection(candidate.id, candidate.selected)}
                className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-brand-orange px-4 py-2.5 text-sm font-black text-zinc-950 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <Check className="h-4 w-4" aria-hidden="true" />
                {candidate.selected ? 'Bỏ chọn phương án' : 'Chọn phương án'}
              </button>
              {selectionBlockedMessage && <p className="mt-2 text-xs text-red-300">{selectionBlockedMessage}</p>}
            </div>
          )}

          {canInspectOnTimeline && (
            <div className="mt-5 rounded-xl border border-dashed border-blue-400/50 bg-blue-500/10 p-4">
              <button
                type="button"
                onClick={candidate.diagnostic ? onClearDiagnostic : onShowDiagnostic}
                className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-blue-400/40 px-4 py-2.5 text-sm font-black text-blue-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-300"
              >
                {candidate.diagnostic
                  ? <X className="h-4 w-4" aria-hidden="true" />
                  : <Eye className="h-4 w-4" aria-hidden="true" />}
                {candidate.diagnostic ? 'Bỏ phủ chẩn đoán' : 'Xem trên timeline'}
              </button>
              <p className="mt-2 text-xs text-blue-200/80">Chỉ thêm một lớp kiểm tra trực quan; lựa chọn backend không thay đổi.</p>
            </div>
          )}

          <details className="mt-5 rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
            <summary className="cursor-pointer text-sm font-bold text-zinc-300">Chi tiết kỹ thuật</summary>
            <dl className="mt-3 break-all font-mono text-xs">
              {Object.entries(candidate.technicalDetails).map(([key, value]) => (
                <DetailRow key={key} label={key}>
                  {value && typeof value === 'object' ? JSON.stringify(value) : String(value ?? '—')}
                </DetailRow>
              ))}
            </dl>
          </details>
        </div>
      </aside>
    </div>
  );
};

export default AutoScheduleCandidateDrawer;
