import { useMemo, useState } from 'react';
import {
  CalendarDays,
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
import {
  getMoviePalette,
  TIMELINE_ZOOM_MODES,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewViewModel';

const FILTERS = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'OPEN_FOR_BOOKING', label: 'Đang mở bán' },
  { value: 'DRAFT', label: 'Đang soạn' },
  { value: 'CLOSED', label: 'Đã đóng bán' },
];

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
  const movieKey = showtime.movie?.publicId || showtime.movie?.slug || showtime.movie?.title || id;
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
    operationalStatus: showtime.status,
    selected: true,
    timelineEligible: serviceDate !== UNKNOWN_SERVICE_DATE_KEY && offsets.valid,
    palette: getMoviePalette(movieKey),
    cleaningMinutes,
    occupancyEndTime,
  };
};

export default function OperationalShowtimeTimeline({ showtimes = [], requestedDate, onViewDetail }) {
  const [dateChoice, setDateChoice] = useState('');
  const [filter, setFilter] = useState('ALL');
  const [zoomMode, setZoomMode] = useState(TIMELINE_ZOOM_MODES.FIT);
  const [drawerCandidate, setDrawerCandidate] = useState(null);
  const models = useMemo(() => showtimes.map(buildViewModel), [showtimes]);
  const serviceDates = useMemo(() => Array.from(new Set(models
    .map(item => item.serviceDate)
    .filter(value => value !== UNKNOWN_SERVICE_DATE_KEY)))
    .sort(compareServiceDateKeys), [models]);
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
  const openCount = dateModels.filter(item => item.operationalStatus === 'OPEN_FOR_BOOKING').length;

  if (models.length === 0) return null;

  return (
    <section className="space-y-4 rounded-2xl border border-zinc-800 bg-zinc-900/30 p-4 md:p-5" aria-labelledby="operations-timeline-title">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-[10px] font-black uppercase tracking-[0.2em] text-brand-orange">Lịch vận hành</p>
          <h2 id="operations-timeline-title" className="mt-1 text-xl font-black text-white">Phòng chiếu × thời gian</h2>
          <p className="mt-1 text-sm text-zinc-500">Quan sát độ phủ phòng, thời lượng phim và khoảng dọn phòng trong một lượt xem.</p>
        </div>
        <div className="flex flex-wrap gap-2 text-xs font-bold">
          <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1.5 text-emerald-300"><CheckCircle2 className="h-3.5 w-3.5" />{openCount} mở bán</span>
          <span className="inline-flex items-center gap-1.5 rounded-full border border-blue-500/30 bg-blue-500/10 px-3 py-1.5 text-blue-300"><Clock3 className="h-3.5 w-3.5" />{draftCount} đang soạn</span>
        </div>
      </div>

      <div className="flex flex-col gap-3 border-y border-zinc-800 py-4 xl:flex-row xl:items-center xl:justify-between">
        <div className="flex flex-wrap gap-2" role="group" aria-label="Chọn ngày trên lịch vận hành">
          {serviceDates.map(value => (
            <button key={value} type="button" aria-pressed={activeDate === value} onClick={() => setDateChoice(value)} className={`inline-flex items-center gap-2 rounded-xl border px-3 py-2 text-xs font-bold ${activeDate === value ? 'border-brand-orange bg-brand-orange/10 text-brand-orange' : 'border-zinc-800 text-zinc-400 hover:border-zinc-700'}`}>
              <CalendarDays className="h-3.5 w-3.5" aria-hidden="true" />{formatServiceDateKey(value)}
            </button>
          ))}
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
        onOpenDetails={candidate => setDrawerCandidate(candidate)}
        variant="operations"
      />
      <p className="flex items-center gap-2 text-xs text-zinc-500"><Clock3 className="h-3.5 w-3.5" />Đang hiển thị {filteredModels.length}/{dateModels.length} suất thuộc trang dữ liệu hiện tại.</p>
      <ShowtimeQuickDrawer showtime={drawerCandidate?.raw || null} onClose={() => setDrawerCandidate(null)} onViewDetail={onViewDetail} />
    </section>
  );
}
