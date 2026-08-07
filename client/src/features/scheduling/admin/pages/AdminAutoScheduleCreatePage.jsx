import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useOutletContext } from 'react-router-dom';
import {
  AlertCircle,
  ArrowLeft,
  Ban,
  CalendarDays,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  Film,
  Loader2,
  MapPin,
  Search,
  Settings2,
  Sparkles,
  Users,
} from 'lucide-react';
import useAutoScheduleForm from '@/features/scheduling/admin/hooks/useAutoScheduleForm';
import { getAutoScheduleBlockerMessage } from '@/features/scheduling/admin/utils/autoScheduleBlockerMessages';
import {
  formatPreviewDateRange,
  formatServiceDateKey,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewDateTime';

const inputClassName = 'min-h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none transition-colors focus:border-brand-orange focus:ring-2 focus:ring-brand-orange/20';
const MOVIE_VERSION_PAGE_SIZE = 10;

const generationPhases = [
  { afterSeconds: 0, message: 'Đang gửi phạm vi và chuẩn bị dữ liệu tối ưu.' },
  { afterSeconds: 8, message: 'Hệ thống đang tạo và đánh giá các phương án lịch.' },
  { afterSeconds: 45, message: 'Bộ tối ưu vẫn đang tính toán lịch phù hợp nhất.' },
  { afterSeconds: 180, message: 'Phạm vi lớn cần thêm thời gian; hệ thống vẫn đang xử lý.' },
];

const formatElapsedTime = totalSeconds => {
  const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, '0');
  const seconds = (totalSeconds % 60).toString().padStart(2, '0');
  return `${minutes}:${seconds}`;
};

const GenerationProgress = ({ planningDays }) => {
  const [elapsedSeconds, setElapsedSeconds] = useState(0);

  useEffect(() => {
    const startedAt = Date.now();
    const timer = setInterval(() => {
      setElapsedSeconds(Math.floor((Date.now() - startedAt) / 1000));
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const currentPhase = generationPhases
    .findLast(phase => elapsedSeconds >= phase.afterSeconds) || generationPhases[0];

  return (
    <section className="mt-4 rounded-xl border border-brand-orange/30 bg-brand-orange/10 p-4" aria-labelledby="generation-progress-title">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 id="generation-progress-title" className="flex items-center gap-2 text-sm font-black text-orange-100">
            <span className="h-2 w-2 animate-pulse rounded-full bg-brand-orange" aria-hidden="true" />
            Hệ thống vẫn đang xử lý
          </h3>
          <p className="mt-1 text-xs leading-5 text-orange-100/70" role="status" aria-live="polite">
            {currentPhase.message}
          </p>
        </div>
        <span className="shrink-0 rounded-lg bg-zinc-950/70 px-2.5 py-1 font-mono text-xs font-bold text-orange-100" aria-label={`Đã chờ ${formatElapsedTime(elapsedSeconds)}`}>
          {formatElapsedTime(elapsedSeconds)}
        </span>
      </div>
      <div
        className="mt-3 h-2 overflow-hidden rounded-full bg-zinc-950/80"
        role="progressbar"
        aria-label="Tiến trình tạo lịch tối ưu"
        aria-valuetext={currentPhase.message}
      >
        <span className="auto-schedule-progress-bar block h-full w-2/5 rounded-full bg-gradient-to-r from-orange-600 via-orange-300 to-orange-600" />
      </div>
      <p className="mt-2 text-[11px] leading-4 text-zinc-400">
        Đây là tiến trình chờ thực tế. Phạm vi {planningDays} ngày có thể mất vài phút và trang sẽ tự chuyển sang bản xem trước khi hoàn tất.
      </p>
    </section>
  );
};

const formatVersionName = version => (
  version.versionName || version.format || 'Định dạng mặc định'
);

const normalizeSearchText = value => String(value || '')
  .normalize('NFD')
  .replace(/\p{Diacritic}/gu, '')
  .toLocaleLowerCase('vi');

const blockerDetailLabels = Object.freeze({
  EXISTING_SHOWTIME_CONFLICT: 'Lịch chiếu hiện có',
  MAINTENANCE_CONFLICT: 'Đóng phòng hoặc bảo trì',
  CINEMA_CLOSURE_CONFLICT: 'Đóng cửa toàn rạp',
  NO_COMPATIBLE_VERSION_FOR_ROOM: 'Định dạng không tương thích',
  NO_MOVIE_IN_RELEASE_WINDOW: 'Ngoài thời gian phát hành',
  OPERATING_WINDOW_TOO_SHORT: 'Giờ hoạt động không đủ dài',
});

const groupBlockerDetailsByDate = details => {
  const groups = new Map();
  (details || []).forEach(detail => {
    const dateKey = detail.serviceDate || 'unknown';
    if (!groups.has(dateKey)) groups.set(dateKey, []);
    groups.get(dateKey).push(detail);
  });
  return Array.from(groups.entries())
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([dateKey, items]) => ({
      dateKey,
      items: [...items].sort((left, right) => (
        (left.auditoriumName || '').localeCompare(right.auditoriumName || '', 'vi')
        || (left.code || '').localeCompare(right.code || '')
      )),
    }));
};

const AutoScheduleBlockerDetails = ({ details }) => {
  const groups = groupBlockerDetailsByDate(details);
  if (groups.length === 0) return null;

  return (
    <div className="mt-3 border-t border-amber-500/20 pt-3" aria-label="Chi tiết điều kiện đang chặn">
      <p className="text-xs font-black uppercase tracking-wider text-amber-200">
        Chi tiết theo ngày và phòng
      </p>
      <div className="mt-3 space-y-3">
        {groups.map(group => (
          <section key={group.dateKey} className="rounded-lg border border-amber-500/20 bg-zinc-950/40 p-3">
            <h4 className="text-sm font-black text-amber-100">
              {formatServiceDateKey(group.dateKey, { weekday: true })}
            </h4>
            <ul className="mt-2 space-y-2">
              {group.items.map((detail, index) => (
                <li
                  key={`${detail.code || 'UNKNOWN'}-${detail.auditoriumPublicId || 'cinema'}-${index}`}
                  className="rounded-lg border border-zinc-800 bg-zinc-950/70 p-3"
                >
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                    <div className="min-w-0">
                      <span className="inline-flex rounded-md bg-amber-500/10 px-2 py-1 text-[10px] font-black uppercase tracking-wider text-amber-300">
                        {blockerDetailLabels[detail.code] || 'Điều kiện vận hành'}
                      </span>
                      <p className="mt-2 text-xs leading-5 text-zinc-200">{detail.message}</p>
                    </div>
                    {detail.actionPath && (
                      <Link
                        to={detail.actionPath}
                        className="inline-flex shrink-0 items-center gap-1 text-xs font-black text-brand-orange"
                      >
                        Xử lý nguyên nhân này <ChevronRight className="h-3.5 w-3.5" />
                      </Link>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>
      <p className="mt-2 text-[11px] leading-4 text-amber-100/60">
        Số phương án có thể chồng nhiều nguyên nhân, ví dụ vừa trùng lịch chiếu vừa trùng thời gian bảo trì.
      </p>
    </div>
  );
};

const ScopeChoice = ({ id, label, included, excluded, onChange }) => (
  <div className="flex flex-col gap-3 rounded-xl border border-zinc-800 bg-zinc-950 p-3 sm:flex-row sm:items-center sm:justify-between">
    <span className="min-w-0 truncate text-sm font-bold text-zinc-200">{label}</span>
    <div className="flex shrink-0 gap-2">
      <button
        type="button"
        aria-pressed={included}
        onClick={() => onChange('include', id, !included)}
        className={`cursor-pointer rounded-lg border px-3 py-2 text-xs font-bold ${included ? 'border-blue-500 bg-blue-500/10 text-blue-200' : 'border-zinc-700 text-zinc-400'}`}
      >
        Chỉ dùng
      </button>
      <button
        type="button"
        aria-pressed={excluded}
        onClick={() => onChange('exclude', id, !excluded)}
        className={`cursor-pointer rounded-lg border px-3 py-2 text-xs font-bold ${excluded ? 'border-rose-500 bg-rose-500/10 text-rose-200' : 'border-zinc-700 text-zinc-400'}`}
      >
        Loại khỏi lịch
      </button>
    </div>
  </div>
);

const MovieVersionChoice = ({ movie, version, included, excluded, onChange }) => {
  const [posterFailed, setPosterFailed] = useState(false);
  const posterUrl = movie.primaryPoster || movie.posterUrl || movie.image || '';
  const versionName = formatVersionName(version);
  const selectionClassName = included
    ? 'border-blue-500/70 bg-blue-500/5 ring-1 ring-blue-500/30'
    : excluded
      ? 'border-rose-500/70 bg-rose-500/5 ring-1 ring-rose-500/30'
      : 'border-zinc-800 bg-zinc-950 hover:border-zinc-700';

  return (
    <article className={`group overflow-hidden rounded-2xl border transition-colors ${selectionClassName}`}>
      <div className="relative aspect-[2/3] overflow-hidden bg-gradient-to-br from-zinc-800 via-zinc-900 to-black">
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 px-4 text-center text-zinc-600">
          <Film className="h-8 w-8" aria-hidden="true" />
          <span className="text-[11px] font-bold uppercase tracking-wider">Chưa có poster</span>
        </div>
        {posterUrl && !posterFailed && (
          <img
            src={posterUrl}
            alt={`Poster ${movie.title}`}
            loading="lazy"
            decoding="async"
            onError={() => setPosterFailed(true)}
            className="absolute inset-0 h-full w-full object-cover transition duration-300 group-hover:scale-[1.03]"
          />
        )}
        <div className="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-black/90 to-transparent" aria-hidden="true" />
        <span className="absolute left-2.5 top-2.5 rounded-full border border-white/10 bg-black/75 px-2.5 py-1 text-[10px] font-black uppercase tracking-wide text-white backdrop-blur-sm">
          {versionName}
        </span>
        {included && <span className="absolute bottom-2.5 left-2.5 rounded-full bg-blue-500 px-2.5 py-1 text-[10px] font-black text-white shadow-lg">Chỉ dùng</span>}
        {excluded && <span className="absolute bottom-2.5 left-2.5 rounded-full bg-rose-500 px-2.5 py-1 text-[10px] font-black text-white shadow-lg">Đã loại</span>}
      </div>

      <div className="flex min-h-36 flex-col p-3">
        <h4 className="line-clamp-2 min-h-10 text-sm font-black leading-5 text-zinc-100" title={movie.title}>{movie.title}</h4>
        <div className="mt-1 flex flex-wrap items-center gap-1.5 text-[11px] text-zinc-500">
          <span>{versionName}</span>
          {movie.durationMinutes && <><span aria-hidden="true">·</span><span>{movie.durationMinutes} phút</span></>}
        </div>
        <div className="mt-auto grid grid-cols-2 gap-2 pt-3">
          <button
            type="button"
            aria-label={`Chỉ dùng ${movie.title} - ${versionName}`}
            aria-pressed={included}
            onClick={() => onChange('include', `version:${version.publicId}`, !included)}
            className={`inline-flex min-h-9 items-center justify-center gap-1 whitespace-nowrap rounded-lg border px-1 text-[10px] font-black transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-400 ${included ? 'border-blue-500 bg-blue-500 text-white' : 'border-zinc-700 text-zinc-300 hover:border-blue-500/70 hover:text-blue-200'}`}
          >
            <CheckCircle2 className="h-3.5 w-3.5" aria-hidden="true" /> Chỉ dùng
          </button>
          <button
            type="button"
            aria-label={`Loại khỏi lịch ${movie.title} - ${versionName}`}
            aria-pressed={excluded}
            onClick={() => onChange('exclude', `version:${version.publicId}`, !excluded)}
            className={`inline-flex min-h-9 items-center justify-center gap-1 whitespace-nowrap rounded-lg border px-1 text-[10px] font-black transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-400 ${excluded ? 'border-rose-500 bg-rose-500 text-white' : 'border-zinc-700 text-zinc-300 hover:border-rose-500/70 hover:text-rose-200'}`}
          >
            <Ban className="h-3.5 w-3.5" aria-hidden="true" /> Loại
          </button>
        </div>
      </div>
    </article>
  );
};

export default function AdminAutoScheduleCreatePage() {
  const { triggerToast } = useOutletContext() || {};
  const navigate = useNavigate();
  const location = useLocation();
  const recreateContext = location.state?.autoScheduleRecreate || null;
  const form = useAutoScheduleForm({
    triggerToast,
    initialDraft: recreateContext?.draft,
    onSuccess: previewPublicId => navigate(`/admin/showtime-schedules/${previewPublicId}`),
  });
  const {
    cinemas,
    selectedCinemaId,
    setSelectedCinemaId,
    selectedCinema,
    planningDays,
    setPlanningPreset,
    slotGranularityMinutes,
    setSlotGranularityMinutes,
    previewTtlMinutes,
    setPreviewTtlMinutes,
    auditoriums,
    movies,
    includeAuditoriumIds,
    excludeAuditoriumIds,
    includeMovieVersionIds,
    excludeMovieVersionIds,
    setScopeChoice,
    preflight,
    preflightError,
    isLoadingCinemas,
    isLoadingScope,
    isCheckingPreflight,
    isSubmitting,
    errors,
    isReady,
    runPreflight,
    handleSubmit,
  } = form;

  const versions = useMemo(() => movies.flatMap(movie => (movie.versions || [])
    .filter(version => version.status === 'ACTIVE')
    .map(version => ({ movie, version }))), [movies]);
  const [movieVersionQuery, setMovieVersionQuery] = useState('');
  const [visibleVersionCount, setVisibleVersionCount] = useState(MOVIE_VERSION_PAGE_SIZE);
  const filteredVersions = useMemo(() => {
    const query = normalizeSearchText(movieVersionQuery.trim());
    if (!query) return versions;
    return versions.filter(({ movie, version }) => normalizeSearchText([
      movie.title,
      movie.originalTitle,
      version.versionName,
      version.format,
      version.audioLanguage,
      version.subtitleLanguage,
      version.dubLanguage,
    ].filter(Boolean).join(' ')).includes(query));
  }, [movieVersionQuery, versions]);
  const visibleVersions = filteredVersions.slice(0, visibleVersionCount);
  const remainingVersionCount = Math.max(0, filteredVersions.length - visibleVersions.length);
  const visibleVersionLabel = movieVersionQuery.trim()
    ? `${filteredVersions.length} kết quả`
    : visibleVersions.length < versions.length
      ? `${visibleVersions.length}/${versions.length} phiên bản`
      : `${versions.length} phiên bản`;
  const advancedChoiceCount = includeAuditoriumIds.length + excludeAuditoriumIds.length
    + includeMovieVersionIds.length + excludeMovieVersionIds.length;
  const hasAdvancedErrors = Boolean(errors.slotGranularityMinutes || errors.previewTtlMinutes);
  const [isAdvancedOpen, setIsAdvancedOpen] = useState(() => Boolean(
    recreateContext?.draft?.auditoriumPublicIds?.length
    || recreateContext?.draft?.movieVersionPublicIds?.length,
  ));
  const handleAdvancedToggle = event => {
    if (!event.currentTarget.open && hasAdvancedErrors) {
      event.currentTarget.open = true;
      return;
    }
    setIsAdvancedOpen(event.currentTarget.open);
  };
  const resetMovieVersionBrowser = () => {
    setMovieVersionQuery('');
    setVisibleVersionCount(MOVIE_VERSION_PAGE_SIZE);
  };
  const handleCinemaChange = event => {
    resetMovieVersionBrowser();
    setSelectedCinemaId(event.target.value);
  };
  const handlePlanningPresetChange = days => {
    resetMovieVersionBrowser();
    setPlanningPreset(days);
  };
  const handleMovieVersionQueryChange = event => {
    setMovieVersionQuery(event.target.value);
    setVisibleVersionCount(MOVIE_VERSION_PAGE_SIZE);
  };

  return (
    <div className="min-h-full space-y-6 bg-zinc-950 text-white animate-fade-in">
      <header className="flex items-start gap-4 border-b border-zinc-800 pb-6">
        <button type="button" onClick={() => navigate(-1)} aria-label="Quay lại" className="mt-1 rounded-xl p-2 text-zinc-400 hover:bg-zinc-800 hover:text-white">
          <ArrowLeft className="h-5 w-5" aria-hidden="true" />
        </button>
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-brand-orange">Xếp lịch tự động · Tối ưu theo nhu cầu</p>
          <h1 className="mt-2 text-3xl font-black tracking-tight">Tạo lịch tối ưu</h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">
            Chọn rạp và số ngày. Hệ thống tự xác định phim, định dạng, phòng, nhu cầu và khung giờ phù hợp trước khi cho bạn xem bản đề xuất.
          </p>
        </div>
      </header>

      {recreateContext && (
        <section className="rounded-xl border border-blue-500/30 bg-blue-500/10 p-4 text-sm text-blue-100" role="status">
          <p className="font-black">Đang tạo lại từ lịch {recreateContext.sourceShortCode}</p>
          <p className="mt-1 text-blue-200/80">Chế độ nhanh sẽ lập lại từ ngày mai; các giới hạn phòng và phim cũ đã được chuyển vào phần Nâng cao để bạn kiểm tra.</p>
        </section>
      )}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
        <main className="space-y-5">
          <section className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-5 md:p-6" aria-labelledby="quick-mode-heading">
            <div className="mb-6 flex items-start gap-3">
              <span className="rounded-xl bg-brand-orange/10 p-2.5 text-brand-orange"><Sparkles className="h-5 w-5" aria-hidden="true" /></span>
              <div>
                <h2 id="quick-mode-heading" className="text-xl font-black">Chế độ nhanh</h2>
                <p className="mt-1 text-sm text-zinc-500">Không cần tự đánh giá mức độ quan tâm hoặc chọn thủ công từng phim và phòng.</p>
              </div>
            </div>

            <label className="block space-y-1.5 text-sm font-bold text-zinc-300">
              Rạp <span className="text-brand-orange">*</span>
              <select
                value={selectedCinemaId}
                onChange={handleCinemaChange}
                disabled={isLoadingCinemas}
                className={inputClassName}
              >
                <option value="">Chọn rạp cần lập lịch</option>
                {cinemas.map(cinema => <option key={cinema.publicId} value={cinema.publicId}>{cinema.name}</option>)}
              </select>
              {selectedCinema?.address && <span className="block text-xs font-normal text-zinc-500">{selectedCinema.address}</span>}
              {errors.cinemaId && <span className="block text-xs text-rose-300">{errors.cinemaId}</span>}
            </label>

            <fieldset className="mt-6">
              <legend className="text-sm font-bold text-zinc-300">Khoảng lập lịch</legend>
              <div className="mt-2 grid gap-3 sm:grid-cols-3">
                {[
                  { days: 1, label: 'Ngày mai', detail: '1 ngày' },
                  { days: 3, label: '3 ngày', detail: 'Ngắn hạn' },
                  { days: 7, label: '7 ngày', detail: 'Cả tuần' },
                ].map(preset => (
                  <button
                    key={preset.days}
                    type="button"
                    aria-pressed={planningDays === preset.days}
                    onClick={() => handlePlanningPresetChange(preset.days)}
                    className={`rounded-xl border p-4 text-left transition-colors ${planningDays === preset.days ? 'border-brand-orange bg-brand-orange/10' : 'border-zinc-700 bg-zinc-950 hover:border-zinc-500'}`}
                  >
                    <span className={`block font-black ${planningDays === preset.days ? 'text-brand-orange' : 'text-zinc-200'}`}>{preset.label}</span>
                    <span className="mt-1 block text-xs text-zinc-500">{preset.detail}</span>
                  </button>
                ))}
              </div>
            </fieldset>
          </section>

          <section className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-5 md:p-6" aria-labelledby="preflight-heading" aria-busy={isCheckingPreflight}>
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="flex items-start gap-3">
                <span className="rounded-xl bg-blue-500/10 p-2.5 text-blue-300"><CheckCircle2 className="h-5 w-5" aria-hidden="true" /></span>
                <div>
                  <h2 id="preflight-heading" className="text-xl font-black">Kiểm tra trước khi tối ưu</h2>
                  <p className="mt-1 text-sm text-zinc-500">Điều kiện tham gia, tương thích định dạng, giờ hoạt động, bảo trì và bảng giá.</p>
                </div>
              </div>
              <button type="button" onClick={() => runPreflight()} disabled={!selectedCinemaId || isCheckingPreflight} className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-zinc-700 px-4 text-sm font-bold text-zinc-200 disabled:opacity-40">
                {isCheckingPreflight && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
                Kiểm tra lại
              </button>
            </div>

            {!selectedCinemaId && <p className="mt-5 rounded-xl border border-zinc-800 bg-zinc-950 p-4 text-sm text-zinc-500">Chọn rạp để hệ thống tự kiểm tra dữ liệu.</p>}
            {selectedCinemaId && isCheckingPreflight && !preflight && <p className="mt-5 flex items-center gap-2 text-sm text-blue-200"><Loader2 className="h-4 w-4 animate-spin" /> Đang kiểm tra dữ liệu vận hành…</p>}
            {selectedCinemaId && isCheckingPreflight && preflight && <p className="mt-5 flex items-center gap-2 text-sm text-blue-200"><Loader2 className="h-4 w-4 animate-spin" /> Đang cập nhật kết quả kiểm tra…</p>}
            {preflightError && <p role="alert" className="mt-5 rounded-xl border border-rose-500/30 bg-rose-500/10 p-4 text-sm text-rose-200">{preflightError}</p>}

            {preflight && (
              <div className="mt-5 space-y-4">
                <div className={`rounded-xl border p-4 ${preflight.canGenerate ? 'border-emerald-500/30 bg-emerald-500/10' : 'border-amber-500/30 bg-amber-500/10'}`}>
                  <p className={`font-black ${preflight.canGenerate ? 'text-emerald-200' : 'text-amber-200'}`}>
                    {preflight.canGenerate ? 'Sẵn sàng tạo lịch' : 'Cần xử lý trước khi tạo lịch'}
                  </p>
                  <p className="mt-1 text-sm text-zinc-300">{formatPreviewDateRange(preflight.planningFrom, preflight.planningTo)} · Giờ địa phương của rạp</p>
                </div>
                <dl className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                  {[
                    ['Phim', preflight.eligibleMovieCount],
                    ['Định dạng', preflight.eligibleVersionCount],
                    ['Phòng', preflight.eligibleAuditoriumCount],
                    ['Cặp tương thích', preflight.compatiblePairCount],
                  ].map(([label, value]) => (
                    <div key={label} className="rounded-xl border border-zinc-800 bg-zinc-950 p-3 text-center">
                      <dt className="text-xs text-zinc-500">{label}</dt>
                      <dd className="mt-1 text-xl font-black text-white">{value ?? 0}</dd>
                    </div>
                  ))}
                </dl>
                {(preflight.blockers || []).length > 0 && (
                  <ul className="space-y-2">
                    {preflight.blockers.map(blocker => (
                      <li key={blocker.code} className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4">
                        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                          <span className="flex items-start gap-2 text-sm text-amber-100"><AlertCircle className="mt-0.5 h-4 w-4 shrink-0" /> {getAutoScheduleBlockerMessage(blocker)}</span>
                          {blocker.actionPath && <Link to={blocker.actionPath} className="inline-flex shrink-0 items-center gap-1 text-xs font-black text-brand-orange">Mở nơi xử lý <ChevronRight className="h-3.5 w-3.5" /></Link>}
                        </div>
                        <AutoScheduleBlockerDetails details={blocker.details} />
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </section>

          <details
            className="group rounded-2xl border border-zinc-800 bg-zinc-900/60"
            open={isAdvancedOpen || hasAdvancedErrors}
            onToggle={handleAdvancedToggle}
          >
            <summary className="flex cursor-pointer list-none items-center justify-between gap-3 p-5 font-black text-zinc-200 md:p-6">
              <span className="flex items-center gap-3"><Settings2 className="h-5 w-5 text-brand-orange" /> Nâng cao <span className="text-xs font-normal text-zinc-500">(tùy chọn)</span></span>
              {advancedChoiceCount > 0 && <span className="rounded-full bg-blue-500/10 px-2.5 py-1 text-xs text-blue-200">{advancedChoiceCount} giới hạn</span>}
            </summary>
            <div className="space-y-6 border-t border-zinc-800 p-5 md:p-6">
              <p className="text-sm leading-6 text-zinc-400">Mặc định optimizer tự dùng toàn bộ dữ liệu đủ điều kiện. “Chỉ dùng” giới hạn phạm vi vào các mục được chọn trong cùng nhóm; “Loại khỏi lịch” bỏ mục đó khỏi lần chạy này. Mục được chọn vẫn chỉ được xếp khi có phương án khả thi.</p>
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="space-y-1.5 text-sm font-bold text-zinc-300">Bước thời gian (phút)
                  <input type="number" min="5" max="60" value={slotGranularityMinutes} onChange={event => setSlotGranularityMinutes(event.target.value)} className={inputClassName} />
                  {errors.slotGranularityMinutes && <span className="block text-xs text-rose-300">{errors.slotGranularityMinutes}</span>}
                </label>
                <label className="space-y-1.5 text-sm font-bold text-zinc-300">Thời gian giữ bản xem trước (phút)
                  <input type="number" min="5" max="120" value={previewTtlMinutes} onChange={event => setPreviewTtlMinutes(event.target.value)} className={inputClassName} />
                  {errors.previewTtlMinutes && <span className="block text-xs text-rose-300">{errors.previewTtlMinutes}</span>}
                </label>
              </div>

              <div>
                <h3 className="flex items-center gap-2 font-black"><Users className="h-4 w-4 text-brand-orange" /> Phòng chiếu</h3>
                <div className="mt-3 space-y-2">
                  {isLoadingScope && auditoriums.length === 0 && <p className="text-sm text-zinc-500">Đang tải…</p>}
                  {auditoriums.map(room => (
                    <ScopeChoice
                      key={room.publicId}
                      id={`auditorium:${room.publicId}`}
                      label={`${room.name} · ${room.screenType || 'STANDARD'}`}
                      included={includeAuditoriumIds.includes(room.publicId)}
                      excluded={excludeAuditoriumIds.includes(room.publicId)}
                      onChange={setScopeChoice}
                    />
                  ))}
                </div>
              </div>

              <div>
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <h3 className="flex items-center gap-2 font-black"><Film className="h-4 w-4 text-brand-orange" /> Phim và phiên bản đủ điều kiện</h3>
                  {versions.length > 0 && <span className="rounded-full border border-zinc-800 bg-zinc-950 px-2.5 py-1 text-[11px] font-bold text-zinc-400">{visibleVersionLabel}</span>}
                </div>
                {versions.length > 0 && (
                  <label className="relative mt-3 block">
                    <span className="sr-only">Tìm phim hoặc phiên bản</span>
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" aria-hidden="true" />
                    <input
                      type="search"
                      value={movieVersionQuery}
                      onChange={handleMovieVersionQueryChange}
                      placeholder="Tìm theo tên phim hoặc phiên bản…"
                      className="min-h-10 w-full rounded-xl border border-zinc-800 bg-zinc-950 py-2 pl-10 pr-3 text-sm text-zinc-200 outline-none transition-colors placeholder:text-zinc-600 focus:border-brand-orange/70 focus:ring-2 focus:ring-brand-orange/10"
                    />
                  </label>
                )}
                <div className="mt-3 max-h-[46rem] overflow-y-auto pr-2">
                  <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 2xl:grid-cols-5">
                    {visibleVersions.map(({ movie, version }) => (
                      <MovieVersionChoice
                        key={`${version.publicId}:${movie.primaryPoster || ''}`}
                        movie={movie}
                        version={version}
                        included={includeMovieVersionIds.includes(version.publicId)}
                        excluded={excludeMovieVersionIds.includes(version.publicId)}
                        onChange={setScopeChoice}
                      />
                    ))}
                  </div>
                  {remainingVersionCount > 0 && (
                    <button
                      type="button"
                      onClick={() => setVisibleVersionCount(current => current + MOVIE_VERSION_PAGE_SIZE)}
                      className="mx-auto mt-4 flex min-h-10 items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 text-xs font-black text-zinc-200 transition-colors hover:border-brand-orange/70 hover:text-brand-orange"
                    >
                      Xem thêm {Math.min(MOVIE_VERSION_PAGE_SIZE, remainingVersionCount)} phiên bản
                      <ChevronDown className="h-4 w-4" aria-hidden="true" />
                    </button>
                  )}
                  {!isLoadingScope && versions.length > 0 && filteredVersions.length === 0 && <p className="rounded-xl border border-zinc-800 bg-zinc-950 p-4 text-center text-sm text-zinc-500">Không tìm thấy phim hoặc phiên bản phù hợp.</p>}
                  {!isLoadingScope && preflight && versions.length === 0 && <p className="text-sm text-zinc-500">Không có phiên bản phim khả dụng trong phạm vi hiện tại.</p>}
                </div>
              </div>
            </div>
          </details>
        </main>

        <aside className="h-fit rounded-2xl border border-zinc-800 bg-zinc-900/70 p-5 xl:sticky xl:top-6">
          <div className="flex items-center gap-2"><CalendarDays className="h-5 w-5 text-brand-orange" /><h2 className="font-black">Tóm tắt lần chạy</h2></div>
          <dl className="mt-5 space-y-4 text-sm">
            <div><dt className="text-xs font-bold text-zinc-500">Rạp</dt><dd className="mt-1 font-bold text-zinc-200">{selectedCinema?.name || 'Chưa chọn'}</dd></div>
            <div><dt className="text-xs font-bold text-zinc-500">Phạm vi</dt><dd className="mt-1 font-bold text-zinc-200">Từ ngày mai · {planningDays} ngày</dd></div>
            <div className="grid grid-cols-3 gap-2">
              <div className="rounded-xl bg-zinc-950 p-3 text-center"><dt className="text-[11px] text-zinc-500">Phim</dt><dd className="mt-1 text-lg font-black">{preflight?.eligibleMovieCount ?? 0}</dd></div>
              <div className="rounded-xl bg-zinc-950 p-3 text-center"><dt className="text-[11px] text-zinc-500">Phòng</dt><dd className="mt-1 text-lg font-black">{preflight?.eligibleAuditoriumCount ?? 0}</dd></div>
              <div className="rounded-xl bg-zinc-950 p-3 text-center"><dt className="text-[11px] text-zinc-500">Giới hạn</dt><dd className="mt-1 text-lg font-black">{advancedChoiceCount}</dd></div>
            </div>
          </dl>
          <div className={`mt-5 rounded-xl border p-4 text-sm ${isReady ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-200' : 'border-zinc-700 bg-zinc-950 text-zinc-400'}`}>
            {isCheckingPreflight ? 'Đang kiểm tra dữ liệu…' : isReady ? 'Kiểm tra trước đã thông qua. Sẵn sàng tối ưu lịch.' : 'Chọn rạp và xử lý các điều kiện đang chặn để tiếp tục.'}
          </div>
          {isSubmitting && <GenerationProgress planningDays={planningDays} />}
          <button
            type="button"
            onClick={handleSubmit}
            disabled={!isReady || isSubmitting}
            className="mt-4 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-brand-orange px-5 text-sm font-black text-zinc-950 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
            {isSubmitting ? 'Đang tối ưu lịch…' : 'Tạo lịch tối ưu'}
          </button>
          <p className="mt-3 flex items-start gap-2 text-xs leading-5 text-zinc-500"><MapPin className="mt-0.5 h-3.5 w-3.5 shrink-0" /> Bản xem trước không tự mở bán. Bạn vẫn phải kiểm tra và xác nhận tạo suất chiếu.</p>
        </aside>
      </div>
    </div>
  );
}
