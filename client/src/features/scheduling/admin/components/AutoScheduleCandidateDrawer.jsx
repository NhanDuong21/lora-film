import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRightLeft, Check, ExternalLink, Eye, Loader2, X } from 'lucide-react';
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

const formatCurrency = value => value === null || value === undefined
  ? '—'
  : new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(Number(value));

const formatPercent = value => value === null || value === undefined
  ? '—'
  : `${Math.round(Number(value) * 100)}%`;

const AutoScheduleCandidateDrawer = ({
  candidate,
  capabilities,
  selectionBlockedMessage,
  onToggleSelection,
  replacementAlternatives = [],
  onReplaceSelection,
  isUpdatingSelection = false,
  isLoadingAlternatives = false,
  canInspectOnTimeline,
  onShowDiagnostic,
  onClearDiagnostic,
  onClose,
  returnFocusElement,
}) => {
  const panelRef = useRef(null);
  const closeButtonRef = useRef(null);
  const [showAlternatives, setShowAlternatives] = useState(false);

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
  const canReplace = canOfferSelection && candidate.selected && Boolean(onReplaceSelection);

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
            <p className="text-[10px] font-black uppercase tracking-[0.2em] text-brand-orange">Chi tiết suất đề xuất</p>
            <h2 id="candidate-drawer-title" className="mt-1 text-xl font-black text-white">{candidate.movieTitle}</h2>
            <p id="candidate-drawer-description" className="mt-1 text-sm text-zinc-400">{candidate.versionName} · {candidate.auditoriumName}</p>
          </div>
          <button ref={closeButtonRef} type="button" onClick={onClose} aria-label="Đóng chi tiết suất đề xuất" className="rounded-xl p-2 text-zinc-400 hover:bg-zinc-800 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange">
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </header>

        <div className="flex-1 overflow-y-auto p-5">
          {candidate.diagnostic && (
            <div className="mb-4 rounded-xl border border-dashed border-blue-400/60 bg-blue-500/10 p-3 text-sm text-blue-200">
              Suất này đang được đánh dấu trên sơ đồ phòng chiếu để bạn kiểm tra; danh sách đã chọn không thay đổi.
            </div>
          )}
          {candidate.selected && (
            <div className="mb-4 flex items-center gap-2 rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-sm font-bold text-emerald-200">
              <Check className="h-4 w-4" aria-hidden="true" /> Suất này đang nằm trong lịch đề xuất
            </div>
          )}

          <dl>
            <DetailRow label="Phòng chiếu">{candidate.auditoriumName}</DetailRow>
            <DetailRow label="Ngày vận hành">{formatServiceDateKey(candidate.serviceDate)}</DetailRow>
            <DetailRow label="Bắt đầu">{candidate.startDateTimeDisplay}</DetailRow>
            <DetailRow label="Kết thúc phim">{candidate.endDateTimeDisplay}</DetailRow>
            <DetailRow label="Phòng sẵn sàng lúc">{candidate.occupancyEndDateTimeDisplay}</DetailRow>
            <DetailRow label="Kiểm tra">{getCandidateValidationPresentation(candidate.validationStatus).label}</DetailRow>
            <DetailRow label="Kết quả tạo suất">{candidate.applyState.label}</DetailRow>
            <DetailRow label="Lý do hệ thống chọn">{candidate.conciseReason || 'Phương án phù hợp và không trùng lịch'}</DetailRow>
            {candidate.createdShowtimePath && (
              <DetailRow label="Suất chiếu đã tạo">
                <Link
                  to={candidate.createdShowtimePath}
                  aria-label={`Mở suất chiếu đã tạo ${candidate.createdShowtimePublicId}`}
                  className="inline-flex items-center gap-1 font-bold text-brand-orange hover:underline"
                >
                  Mở suất chiếu<ExternalLink className="h-3.5 w-3.5" aria-hidden="true" />
                </Link>
              </DetailRow>
            )}
          </dl>

          <details className="mt-5 rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
            <summary className="cursor-pointer text-sm font-bold text-zinc-300">Xem dự báo hỗ trợ quyết định</summary>
            <dl className="mt-3">
              <DetailRow label="Điểm ưu tiên">{candidate.score ?? '—'} <span className="text-xs text-zinc-500">(cao hơn tốt hơn)</span></DetailRow>
              <DetailRow label="Khách dự kiến">{candidate.expectedAttendance ?? '—'}</DetailRow>
              <DetailRow label="Lấp đầy dự kiến">{formatPercent(candidate.expectedOccupancy)}</DetailRow>
              <DetailRow label="Doanh thu dự kiến">{formatCurrency(candidate.expectedRevenue)}</DetailRow>
              <DetailRow label="Đóng góp mục tiêu">{formatCurrency(candidate.expectedContribution)}</DetailRow>
              <DetailRow label="Độ tin cậy nhu cầu">{formatPercent(candidate.demandConfidence)}</DetailRow>
              <DetailRow label="Giải thích nhu cầu">{candidate.demandExplanation || 'Chưa có dữ liệu giải thích'}</DetailRow>
            </dl>
          </details>

          {canReplace && (
            <div className="mt-5 rounded-xl border border-brand-orange/30 bg-brand-orange/5 p-4">
              <button
                type="button"
                disabled={isUpdatingSelection}
                onClick={() => setShowAlternatives(value => !value)}
                aria-expanded={showAlternatives}
                className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-brand-orange px-4 py-2.5 text-sm font-black text-zinc-950 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {isUpdatingSelection ? <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" /> : <ArrowRightLeft className="h-4 w-4" aria-hidden="true" />}
                {isUpdatingSelection ? 'Đang thay suất…' : showAlternatives ? 'Ẩn phương án thay thế' : 'Tìm phương án thay thế'}
              </button>

              {showAlternatives && (
                <div className="mt-4 space-y-2" aria-label="Phương án thay thế phù hợp">
                  <p className="text-xs leading-5 text-zinc-400">Hệ thống chỉ hiển thị các phương án không trùng với phần lịch còn lại. Phương án cùng phim và gần giờ hiện tại được ưu tiên trước.</p>
                  {replacementAlternatives.length === 0 ? (
                    <p className="flex items-center gap-2 rounded-lg border border-dashed border-zinc-700 p-3 text-sm text-zinc-400">
                      {isLoadingAlternatives && <Loader2 className="h-4 w-4 shrink-0 animate-spin" aria-hidden="true" />}
                      {isLoadingAlternatives
                        ? 'Đang chuẩn bị các phương án thay thế an toàn…'
                        : 'Chưa có phương án thay thế an toàn cho suất này.'}
                    </p>
                  ) : replacementAlternatives.map(alternative => (
                    <article key={alternative.id} className="rounded-xl border border-zinc-800 bg-zinc-950/70 p-3">
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <p className="truncate text-sm font-black text-white">{alternative.movieTitle}</p>
                          <p className="mt-1 text-xs text-zinc-400">{alternative.auditoriumName} · {alternative.startTimeDisplay}–{alternative.endTimeDisplay}</p>
                          <div className="mt-2 flex flex-wrap gap-1.5">
                            {alternative.movieKey === candidate.movieKey && <span className="rounded-full bg-blue-500/10 px-2 py-1 text-[10px] font-bold text-blue-200">Cùng phim</span>}
                            {alternative.auditoriumKey === candidate.auditoriumKey && <span className="rounded-full bg-violet-500/10 px-2 py-1 text-[10px] font-bold text-violet-200">Cùng phòng</span>}
                          </div>
                        </div>
                        <button
                          type="button"
                          disabled={isUpdatingSelection}
                          onClick={() => onReplaceSelection(candidate.id, alternative.id)}
                          className="shrink-0 rounded-lg border border-brand-orange/40 px-3 py-2 text-xs font-black text-brand-orange hover:bg-brand-orange/10 disabled:opacity-50"
                        >
                          Thay bằng suất này
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </div>
          )}

          {canOfferSelection && (
            <div className="mt-5 rounded-xl border border-zinc-800 bg-zinc-900 p-4">
              <button
                type="button"
                disabled={selectionDisabled}
                onClick={() => onToggleSelection(candidate.id, candidate.selected)}
                className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-brand-orange px-4 py-2.5 text-sm font-black text-zinc-950 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <Check className="h-4 w-4" aria-hidden="true" />
                {candidate.selected ? 'Bỏ khỏi lịch' : 'Thêm vào lịch'}
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
                {candidate.diagnostic ? 'Bỏ đánh dấu trên sơ đồ' : 'Xem trên sơ đồ phòng chiếu'}
              </button>
              <p className="mt-2 text-xs text-blue-200/80">Thao tác này chỉ giúp kiểm tra trực quan; danh sách đã chọn không thay đổi.</p>
            </div>
          )}

          <details className="mt-5 rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
            <summary className="cursor-pointer text-sm font-bold text-zinc-300">Dữ liệu nhận diện</summary>
            <dl className="mt-3 break-all text-xs">
              <DetailRow label="Thứ tự rà soát">{candidate.rank ?? '—'}</DetailRow>
              <DetailRow label="Mã phương án">{candidate.id}</DetailRow>
              <DetailRow label="Mã phim">{candidate.moviePublicId}</DetailRow>
              <DetailRow label="Mã phiên bản">{candidate.movieVersionPublicId}</DetailRow>
              <DetailRow label="Mã phòng">{candidate.auditoriumPublicId}</DetailRow>
            </dl>
          </details>
        </div>
      </aside>
    </div>
  );
};

export default AutoScheduleCandidateDrawer;
