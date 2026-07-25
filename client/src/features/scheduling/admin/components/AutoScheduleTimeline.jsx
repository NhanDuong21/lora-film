import { useEffect, useMemo, useRef, useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  Clock3,
  Minus,
  Search,
  Star,
} from 'lucide-react';
import {
  buildDynamicTimelineWindow,
  formatServiceDateKey,
  formatTimelineMinute,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewDateTime';
import { TIMELINE_ZOOM_MODES } from '@/features/scheduling/admin/utils/autoSchedulePreviewViewModel';

const LABEL_WIDTH = 144;
const FALLBACK_VIEWPORT_WIDTH = 960;

const ZOOM_OPTIONS = [
  { value: TIMELINE_ZOOM_MODES.FIT, label: 'Vừa khung' },
  { value: TIMELINE_ZOOM_MODES.COMPACT, label: '30 px/giờ' },
  { value: TIMELINE_ZOOM_MODES.COMFORTABLE, label: '60 px/giờ' },
  { value: TIMELINE_ZOOM_MODES.DETAILED, label: '120 px/giờ' },
];

const getStatePresentation = candidate => {
  if (candidate.diagnostic) {
    return { marker: 'Chẩn đoán', icon: Search, className: 'border-dashed border-white/80' };
  }
  if (candidate.applyStatus === 'CREATED') {
    return { marker: 'Đã tạo', icon: CheckCircle2, className: 'border-solid' };
  }
  if (candidate.applyStatus === 'CONFLICT') {
    return { marker: 'Xung đột', icon: AlertTriangle, className: 'border-dashed border-red-200' };
  }
  if (candidate.applyStatus === 'FAILED') {
    return { marker: 'Thất bại', icon: AlertTriangle, className: 'border-dashed border-red-200' };
  }
  if (candidate.applyStatus === 'SKIPPED') {
    return { marker: 'Bỏ qua', icon: Minus, className: 'border-solid opacity-75' };
  }
  if (candidate.selected) {
    return { marker: 'Đã chọn', icon: Star, className: 'border-solid' };
  }
  return { marker: 'Đang chờ', icon: Clock3, className: 'border-solid' };
};

const AutoScheduleTimeline = ({
  serviceDate,
  candidates,
  auditoriums,
  zoomMode,
  onZoomChange,
  onOpenDetails,
}) => {
  const viewportRef = useRef(null);
  const [viewportWidth, setViewportWidth] = useState(FALLBACK_VIEWPORT_WIDTH);
  const timelineWindow = useMemo(
    () => buildDynamicTimelineWindow(candidates),
    [candidates],
  );

  useEffect(() => {
    const node = viewportRef.current;
    if (!node) return undefined;

    const measure = () => {
      const nextWidth = node.clientWidth || FALLBACK_VIEWPORT_WIDTH;
      setViewportWidth(previous => Math.abs(previous - nextWidth) > 1 ? nextWidth : previous);
    };
    const frame = requestAnimationFrame(measure);
    const observer = typeof ResizeObserver === 'function' ? new ResizeObserver(measure) : null;
    observer?.observe(node);
    window.addEventListener('resize', measure);
    return () => {
      cancelAnimationFrame(frame);
      observer?.disconnect();
      window.removeEventListener('resize', measure);
    };
  }, []);

  const durationHours = timelineWindow ? timelineWindow.totalMinutes / 60 : 1;
  const fitWidth = Math.max(viewportWidth - LABEL_WIDTH - 32, 320);
  const pixelsPerHour = zoomMode === TIMELINE_ZOOM_MODES.FIT
    ? fitWidth / durationHours
    : Number(zoomMode);
  const contentWidth = zoomMode === TIMELINE_ZOOM_MODES.FIT
    ? fitWidth
    : Math.max(durationHours * pixelsPerHour, 1);
  const timelineItemsByAuditorium = useMemo(() => {
    const groups = new Map();
    (candidates || []).forEach(candidate => {
      if (!candidate.timelineEligible) return;
      if (!groups.has(candidate.auditoriumKey)) groups.set(candidate.auditoriumKey, []);
      groups.get(candidate.auditoriumKey).push(candidate);
    });
    groups.forEach(items => items.sort((left, right) => (
      left.startMinuteOffset - right.startMinuteOffset || left.id.localeCompare(right.id)
    )));
    return groups;
  }, [candidates]);
  const invalidCount = (candidates || []).filter(candidate => !candidate.timelineEligible).length;

  return (
    <section className="space-y-4" aria-label={`Timeline ${formatServiceDateKey(serviceDate)}`}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-black text-white">{formatServiceDateKey(serviceDate, { weekday: true })}</h2>
          <p className="mt-1 text-xs text-zinc-500">Thời gian rạp · phạm vi tự động theo thời gian chiếm phòng</p>
        </div>
        <div className="flex flex-wrap gap-1 rounded-xl border border-zinc-800 bg-zinc-900 p-1" role="group" aria-label="Mức thu phóng timeline">
          {ZOOM_OPTIONS.map(option => (
            <button
              key={option.value}
              type="button"
              aria-pressed={zoomMode === option.value}
              onClick={() => onZoomChange(option.value)}
              className={`rounded-lg px-3 py-2 text-xs font-bold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange ${zoomMode === option.value ? 'bg-zinc-700 text-white' : 'text-zinc-400 hover:text-white'}`}
            >
              {option.label}
            </button>
          ))}
        </div>
      </div>

      <ul
        className="flex flex-wrap gap-x-4 gap-y-2 rounded-xl border border-zinc-800 bg-zinc-950/55 p-3 text-xs text-zinc-300"
        aria-label="Chú giải timeline"
      >
        <li className="flex items-center gap-2">
          <span className="h-4 w-8 rounded border border-orange-300 bg-orange-500" aria-hidden="true" />
          Thời lượng phim
        </li>
        <li className="flex items-center gap-2">
          <span
            className="h-4 w-8 rounded border border-orange-300 bg-orange-500/40"
            style={{ backgroundImage: 'repeating-linear-gradient(135deg, rgba(255,255,255,0.7) 0 2px, transparent 2px 6px)' }}
            aria-hidden="true"
          />
          Thời gian dọn phòng
        </li>
        <li className="flex items-center gap-2">
          <span className="inline-flex h-5 w-5 items-center justify-center rounded border border-emerald-300 text-emerald-300" aria-hidden="true"><Star className="h-3 w-3" /></span>
          Đề xuất đã chọn
        </li>
        <li className="flex items-center gap-2">
          <span className="inline-flex h-5 w-5 items-center justify-center rounded border border-dashed border-white text-white" aria-hidden="true"><Search className="h-3 w-3" /></span>
          Phủ chẩn đoán
        </li>
        <li className="flex items-center gap-2">
          <span className="inline-flex h-5 w-5 items-center justify-center rounded border border-dashed border-red-300 text-red-300" aria-hidden="true"><AlertTriangle className="h-3 w-3" /></span>
          Không hợp lệ / xung đột
        </li>
      </ul>

      {invalidCount > 0 && (
        <p className="flex items-center gap-2 text-sm text-amber-300" role="status">
          <AlertTriangle className="h-4 w-4" aria-hidden="true" />
          {invalidCount} ứng viên có thời gian không hợp lệ đã được bỏ qua khỏi timeline.
        </p>
      )}

      <div ref={viewportRef} className="max-w-full overflow-x-auto rounded-2xl border border-zinc-800 bg-zinc-900/40" data-zoom-mode={zoomMode} data-pixels-per-hour={pixelsPerHour.toFixed(2)}>
        {!timelineWindow ? (
          <div className="p-8 text-center text-sm text-zinc-500">
            Chưa có đề xuất hợp lệ để dựng trục thời gian cho ngày này.
          </div>
        ) : (
          <div style={{ width: `${LABEL_WIDTH + contentWidth}px` }}>
            <div className="sticky top-0 z-10 grid border-b border-zinc-800 bg-zinc-950/95" style={{ gridTemplateColumns: `${LABEL_WIDTH}px ${contentWidth}px` }}>
              <div className="sticky left-0 z-20 border-r border-zinc-800 bg-zinc-950 px-3 py-3 text-[10px] font-black uppercase tracking-wider text-zinc-500">
                Phòng chiếu
              </div>
              <div className="relative h-11" data-testid="timeline-ruler" style={{ width: `${contentWidth}px` }}>
                {timelineWindow.ticks.map(tick => {
                  const left = ((tick - timelineWindow.startMinute) / timelineWindow.totalMinutes) * 100;
                  return (
                    <div key={tick} className="absolute inset-y-0 border-l border-zinc-700/70" style={{ left: `${left}%` }}>
                      <span className="absolute left-1 top-2 whitespace-nowrap text-[10px] font-bold text-zinc-500">{formatTimelineMinute(tick)}</span>
                    </div>
                  );
                })}
              </div>
            </div>

            <div>
              {(auditoriums || []).map(auditorium => {
                const rowCandidates = timelineItemsByAuditorium.get(auditorium.key) || [];
                return (
                  <div key={auditorium.key} className="grid min-h-[76px] border-b border-zinc-800/70 last:border-b-0" style={{ gridTemplateColumns: `${LABEL_WIDTH}px ${contentWidth}px` }}>
                    <div className="sticky left-0 z-10 flex items-center border-r border-zinc-800 bg-zinc-900 px-3 py-2">
                      <span className="truncate text-xs font-bold text-zinc-200" title={auditorium.name}>{auditorium.name}</span>
                    </div>
                    <div className="relative my-2 h-[60px] bg-zinc-950/45" data-testid={`timeline-track-${auditorium.key}`} style={{ width: `${contentWidth}px` }}>
                      {timelineWindow.ticks.map(tick => (
                        <div key={tick} className="pointer-events-none absolute inset-y-0 border-l border-zinc-800/60" style={{ left: `${((tick - timelineWindow.startMinute) / timelineWindow.totalMinutes) * 100}%` }} />
                      ))}
                      {rowCandidates.length === 0 && (
                        <span className="absolute inset-0 flex items-center px-4 text-xs text-zinc-600">Chưa có đề xuất đã chọn</span>
                      )}
                      {rowCandidates.map(candidate => {
                        const state = getStatePresentation(candidate);
                        const StateIcon = state.icon;
                        const totalDuration = candidate.occupancyEndMinuteOffset - candidate.startMinuteOffset;
                        const runtimeDuration = candidate.endMinuteOffset - candidate.startMinuteOffset;
                        const left = ((candidate.startMinuteOffset - timelineWindow.startMinute) / timelineWindow.totalMinutes) * 100;
                        const width = (totalDuration / timelineWindow.totalMinutes) * 100;
                        const runtimeWidth = totalDuration > 0 ? (runtimeDuration / totalDuration) * 100 : 100;
                        const cleaningWidth = Math.max(100 - runtimeWidth, 0);

                        return (
                          <button
                            key={candidate.id}
                            type="button"
                            onClick={event => onOpenDetails(candidate, event.currentTarget)}
                            aria-label={`${candidate.diagnostic ? 'Ứng viên chẩn đoán. ' : ''}${candidate.movieTitle}, ${candidate.startTimeDisplay}, ${state.marker}. Mở chi tiết`}
                            data-testid={`timeline-candidate-${candidate.id}`}
                            data-palette-index={candidate.palette.index}
                            data-state-marker={state.marker}
                            data-diagnostic={candidate.diagnostic ? 'true' : 'false'}
                            className={`absolute top-1 h-[52px] min-w-11 overflow-hidden rounded-lg border-2 text-left shadow-lg transition-transform hover:z-20 hover:scale-[1.02] focus-visible:z-30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white ${state.className}`}
                            style={{ left: `${left}%`, width: `${width}%`, borderColor: candidate.diagnostic ? '#ffffff' : candidate.palette.border }}
                          >
                            <span className="absolute inset-y-0 left-0" data-testid={`runtime-segment-${candidate.id}`} style={{ width: `${runtimeWidth}%`, backgroundColor: candidate.palette.solid }} />
                            {cleaningWidth > 0 && (
                              <span
                                className="absolute inset-y-0 right-0 border-l border-black/25"
                                data-testid={`cleaning-segment-${candidate.id}`}
                                aria-label={`Dọn phòng đến ${candidate.occupancyEndTimeDisplay}`}
                                style={{
                                  width: `${cleaningWidth}%`,
                                  backgroundColor: candidate.palette.cleaning,
                                  backgroundImage: 'repeating-linear-gradient(135deg, rgba(255,255,255,0.55) 0 2px, transparent 2px 6px)',
                                }}
                              />
                            )}
                            <span className="relative z-10 flex h-full min-w-0 flex-col justify-center px-2" style={{ color: candidate.palette.text }}>
                              <span className="flex items-center gap-1 truncate text-[10px] font-black">
                                <StateIcon className="h-3 w-3 shrink-0" aria-hidden="true" />
                                <span className="truncate">{candidate.movieTitle}</span>
                              </span>
                              <span className="truncate text-[9px] font-bold opacity-80">{candidate.startTimeDisplay} · {state.marker}</span>
                            </span>
                          </button>
                        );
                      })}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </section>
  );
};

export default AutoScheduleTimeline;
