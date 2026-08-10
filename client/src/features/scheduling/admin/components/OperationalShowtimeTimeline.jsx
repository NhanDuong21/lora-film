import { useMemo, useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  Clock3,
  ListFilter,
} from 'lucide-react';
import AutoScheduleTimeline from './AutoScheduleTimeline';
import ShowtimeQuickDrawer from './ShowtimeQuickDrawer';
import {
  compareServiceDateKeys,
  formatCinemaTime,
  formatServiceDateKey,
  getCandidateTimelineOffsets,
  getCinemaDateKey,
  getServiceDateKey,
  UNKNOWN_SERVICE_DATE_KEY,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewDateTime';
import { TIMELINE_ZOOM_MODES } from '@/features/scheduling/admin/utils/autoSchedulePreviewViewModel';
import { getOperationalShowtimeStatus } from '@/features/scheduling/admin/utils/schedulingPresentation';

const FILTERS = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'OPEN_FOR_BOOKING', label: 'Đang mở bán' },
  { value: 'DRAFT', label: 'Đang soạn' },
  { value: 'EXPIRED_DRAFT', label: 'Đã quá giờ' },
  { value: 'CLOSED', label: 'Đã đóng bán' },
];

const OPERATIONAL_PALETTES = Object.freeze({
  OPEN_FOR_BOOKING: { solid: '#153b38', border: '#34d399', text: '#ecfdf5', cleaning: '#1f524d', index: 'open' },
  DRAFT: { solid: '#172554', border: '#60a5fa', text: '#eff6ff', cleaning: '#1e3a6d', index: 'draft' },
  EXPIRED_DRAFT: { solid: '#3f1d24', border: '#fb7185', text: '#fff1f2', cleaning: '#592832', index: 'expired' },
  CLOSED: { solid: '#292524', border: '#a8a29e', text: '#fafaf9', cleaning: '#44403c', index: 'closed' },
  CANCELLED: { solid: '#3f1d24', border: '#fb7185', text: '#fff1f2', cleaning: '#592832', index: 'cancelled' },
  FINISHED: { solid: '#27272a', border: '#71717a', text: '#e4e4e7', cleaning: '#3f3f46', index: 'finished' },
});

const getOperationalPalette = status => OPERATIONAL_PALETTES[status] || OPERATIONAL_PALETTES.CLOSED;

const getDateChoicePresentation = dateKey => {
  const [weekday = '', fullDate = dateKey] = formatServiceDateKey(dateKey, { weekday: true }).split(', ');
  return { weekday, date: fullDate.slice(0, 5) };
};

const addMinutes = (instant, minutes) => {
  const date = new Date(instant);
  if (!Number.isFinite(date.getTime())) return instant;
  return new Date(date.getTime() + (Number(minutes || 0) * 60_000)).toISOString();
};

const buildViewModel = showtime => {
  const id = showtime.showtimePublicId;
  const timezone = showtime.cinema?.timezone;
  const serviceDate = getServiceDateKey(showtime.serviceDate || getCinemaDateKey(showtime.startTime, timezone));
  const cleaningMinutes = Number(showtime.auditorium?.cleaningBufferMinutes || 0);
  const occupancyEndTime = addMinutes(showtime.endTime, cleaningMinutes);
  const offsets = getCandidateTimelineOffsets({
    startTime: showtime.startTime,
    endTime: showtime.endTime,
    occupancyEndTime,
  }, serviceDate, timezone);
  const cinemaKey = showtime.cinema?.publicId || showtime.cinema?.slug || showtime.cinema?.name || 'unknown-cinema';
  const roomKey = showtime.auditorium?.publicId || showtime.auditorium?.name || 'unknown-room';

  return {
    id,
    raw: showtime,
    serviceDate,
    timezone,
    startMinuteOffset: offsets.startMinute,
    endMinuteOffset: offsets.endMinute,
    occupancyEndMinuteOffset: offsets.occupancyEndMinute,
    startTimeDisplay: formatCinemaTime(showtime.startTime, timezone),
    endTimeDisplay: formatCinemaTime(showtime.endTime, timezone),
    occupancyEndTimeDisplay: formatCinemaTime(occupancyEndTime, timezone),
    movieTitle: showtime.movie?.title || 'Phim chưa xác định',
    versionName: showtime.movieVersion?.versionName || showtime.movieVersion?.format || 'Chưa có phiên bản',
    auditoriumKey: `${cinemaKey}:${roomKey}`,
    auditoriumName: `${showtime.auditorium?.name || 'Chưa có phòng'}${showtime.cinema?.name ? ` · ${showtime.cinema.name}` : ''}`,
    auditoriumPublicId: showtime.auditorium?.publicId || null,
    operationalStatus: getOperationalShowtimeStatus(showtime),
    selected: true,
    timelineEligible: serviceDate !== UNKNOWN_SERVICE_DATE_KEY && offsets.valid,
    palette: getOperationalPalette(getOperationalShowtimeStatus(showtime)),
    cleaningMinutes,
    occupancyEndTime,
  };
};

export default function OperationalShowtimeTimeline({
  showtimes = [],
  requestedDate,
  dateOptions = [],
  onRequestedDateChange,
  onViewDetail,
  readOnly = false,
  quickDrawerProps = {},
}) {
  const [dateChoice, setDateChoice] = useState('');
  const [filter, setFilter] = useState('ALL');
  const [zoomMode, setZoomMode] = useState(TIMELINE_ZOOM_MODES.FIT);
  const [drawerCandidate, setDrawerCandidate] = useState(null);
  const models = useMemo(() => showtimes.map(buildViewModel), [showtimes]);
  const serviceDates = useMemo(() => Array.from(new Set(models
    .map(item => item.serviceDate)
    .filter(value => value !== UNKNOWN_SERVICE_DATE_KEY)))
    .sort(compareServiceDateKeys), [models]);
  const selectableDates = dateOptions.length > 0 ? dateOptions : serviceDates;
  const activeDate = serviceDates.includes(dateChoice)
    ? dateChoice
    : serviceDates.includes(requestedDate) ? requestedDate : serviceDates[0];
  const dateModels = models.filter(item => item.serviceDate === activeDate);
  const filteredModels = dateModels.filter(item => {
    if (filter === 'ALL') return true;
    return item.operationalStatus === filter;
  });
  const auditoriums = useMemo(() => {
    const values = new Map();
    dateModels.forEach(item => values.set(item.auditoriumKey, {
      key: item.auditoriumKey,
      publicId: item.auditoriumPublicId,
      name: item.auditoriumName,
    }));
    return Array.from(values.values()).sort((left, right) => left.name.localeCompare(right.name, 'vi'));
  }, [dateModels]);
  const draftCount = dateModels.filter(item => item.operationalStatus === 'DRAFT').length;
  const expiredDraftCount = dateModels.filter(item => item.operationalStatus === 'EXPIRED_DRAFT').length;
  const openCount = dateModels.filter(item => item.operationalStatus === 'OPEN_FOR_BOOKING').length;

  if (models.length === 0) return null;

  return (
    <section className="space-y-4 rounded-2xl border border-zinc-800 bg-zinc-900/30 p-4 md:p-5" aria-labelledby="operations-timeline-title">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-[10px] font-black uppercase tracking-[0.2em] text-brand-orange">Lịch vận hành</p>
          <h2 id="operations-timeline-title" className="mt-1 text-xl font-black text-white">Phòng chiếu × thời gian</h2>
          <p className="mt-1 text-sm text-zinc-500">Màu thể hiện trạng thái; mỗi khối ghi đủ giờ chiếu và phần sọc là thời gian dọn phòng.</p>
        </div>
        <div className="flex flex-wrap gap-2 text-xs font-bold">
          <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1.5 text-emerald-300"><CheckCircle2 className="h-3.5 w-3.5" />{openCount} mở bán</span>
          <span className="inline-flex items-center gap-1.5 rounded-full border border-blue-500/30 bg-blue-500/10 px-3 py-1.5 text-blue-300"><Clock3 className="h-3.5 w-3.5" />{draftCount} đang soạn</span>
          {expiredDraftCount > 0 && <span className="inline-flex items-center gap-1.5 rounded-full border border-red-500/30 bg-red-500/10 px-3 py-1.5 text-red-300"><AlertTriangle className="h-3.5 w-3.5" />{expiredDraftCount} đã quá giờ</span>}
        </div>
      </div>

      <div className="flex flex-col gap-3 border-y border-zinc-800 py-4 xl:flex-row xl:items-center xl:justify-between">
        <div className="flex flex-wrap gap-2" role="group" aria-label="Chọn ngày trên lịch vận hành">
          {selectableDates.map(value => {
            const presentation = getDateChoicePresentation(value);
            return (
              <button
                key={value}
                type="button"
                aria-label={`Xem lịch ngày ${formatServiceDateKey(value)}`}
                aria-pressed={activeDate === value}
                onClick={() => {
                  setDateChoice(value);
                  if (value !== requestedDate) onRequestedDateChange?.(value);
                }}
                className={`min-w-[72px] rounded-xl border px-3 py-2 text-left transition-colors ${activeDate === value ? 'border-brand-orange bg-brand-orange/10 text-brand-orange' : 'border-zinc-800 bg-zinc-950/30 text-zinc-400 hover:border-zinc-600 hover:text-zinc-200'}`}
              >
                <span className="block text-[10px] font-bold">{presentation.weekday}</span>
                <span className="mt-0.5 block text-xs font-black">{presentation.date}</span>
              </button>
            );
          })}
        </div>
        <div className="flex flex-wrap gap-1 rounded-xl border border-zinc-800 bg-zinc-950/60 p-1" role="group" aria-label="Lọc trạng thái trên lịch">
          <ListFilter className="mx-2 h-4 w-4 self-center text-zinc-500" aria-hidden="true" />
          {FILTERS.map(option => (
            <button key={option.value} type="button" aria-pressed={filter === option.value} onClick={() => setFilter(option.value)} className={`rounded-lg px-3 py-2 text-xs font-bold ${filter === option.value ? 'bg-zinc-700 text-white' : 'text-zinc-400 hover:text-white'}`}>
              {option.label}
            </button>
          ))}
        </div>
      </div>

      {filter !== 'ALL' && filteredModels.length === 0 && (
        <p className="flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900/60 p-3 text-sm text-zinc-300"><CheckCircle2 className="h-4 w-4" />Không có suất nào ở trạng thái này trong ngày đã chọn.</p>
      )}
      <AutoScheduleTimeline
        serviceDate={activeDate}
        candidates={filteredModels}
        auditoriums={auditoriums}
        zoomMode={zoomMode}
        onZoomChange={setZoomMode}
        onOpenDetails={readOnly ? undefined : candidate => setDrawerCandidate(candidate)}
        variant="operations"
      />
      <p className="flex items-center gap-2 text-xs text-zinc-500"><Clock3 className="h-3.5 w-3.5" />Đang hiển thị {filteredModels.length}/{dateModels.length} suất thuộc trang dữ liệu hiện tại.</p>
      {!readOnly && <ShowtimeQuickDrawer showtime={drawerCandidate?.raw || null} onClose={() => setDrawerCandidate(null)} onViewDetail={onViewDetail} {...quickDrawerProps} />}
    </section>
  );
}
