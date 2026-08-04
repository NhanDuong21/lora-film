import { useMemo, useState } from 'react';
import { useLocation, useNavigate, useOutletContext } from 'react-router-dom';
import {
  AlertCircle,
  ArrowLeft,
  CalendarDays,
  Check,
  ChevronLeft,
  ChevronRight,
  Film,
  Loader2,
  MapPin,
  Save,
  Settings2,
  Users,
} from 'lucide-react';
import useAutoScheduleForm from '@/features/scheduling/admin/hooks/useAutoScheduleForm';
import useExistingShowtimeSummary from '@/features/scheduling/admin/hooks/useExistingShowtimeSummary';
import { formatPreviewDateRange } from '@/features/scheduling/admin/utils/autoSchedulePreviewDateTime';

const inputClassName = 'min-h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none transition-colors placeholder:text-zinc-600 focus:border-brand-orange focus:ring-2 focus:ring-brand-orange/20';

const addDays = (dateValue, days) => {
  if (!dateValue) return '';
  const [year, month, day] = dateValue.split('-').map(Number);
  const next = new Date(Date.UTC(year, month - 1, day + days));
  return next.toISOString().slice(0, 10);
};

const getEligibilityReasonLabel = reason => {
  const labels = {
    MOVIE_STATUS_NOT_ELIGIBLE: 'Phim chưa ở trạng thái có thể chiếu.',
    MOVIE_DURATION_INVALID: 'Thời lượng phim chưa hợp lệ.',
    NO_ACTIVE_MOVIE_VERSION: 'Phim chưa có định dạng đang hoạt động.',
    OUTSIDE_RELEASE_WINDOW: 'Khoảng ngày tạo lịch nằm ngoài thời gian phát hành của phim.',
  };
  return labels[reason?.code] || reason?.message || 'Phim chưa đủ điều kiện tạo lịch.';
};

const MoviePoster = ({ src, title }) => {
  const [failed, setFailed] = useState(false);
  if (!src || failed) {
    return (
      <span className="flex aspect-[2/3] w-20 shrink-0 flex-col items-center justify-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 text-center text-zinc-600">
        <Film className="h-6 w-6" aria-hidden="true" />
        <span className="px-1 text-[9px] font-bold">Chưa có poster</span>
      </span>
    );
  }
  return <img src={src} alt={`Poster ${title}`} onError={() => setFailed(true)} className="aspect-[2/3] w-20 shrink-0 rounded-xl border border-zinc-700 object-cover" />;
};

const Step = ({ number, title, active, complete, disabled, onClick }) => (
  <button
    type="button"
    disabled={disabled}
    onClick={onClick}
    className={`flex min-w-0 flex-1 items-center gap-3 rounded-xl border px-3 py-3 text-left transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${active ? 'border-brand-orange bg-brand-orange/10' : 'border-zinc-800 bg-zinc-900/50 hover:border-zinc-700'}`}
  >
    <span className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-sm font-black ${complete ? 'bg-emerald-500 text-zinc-950' : active ? 'bg-brand-orange text-zinc-950' : 'bg-zinc-800 text-zinc-400'}`}>
      {complete ? <Check className="h-4 w-4" aria-hidden="true" /> : number}
    </span>
    <span className="min-w-0">
      <span className={`block truncate text-sm font-black ${active ? 'text-brand-orange' : 'text-zinc-200'}`}>{title}</span>
      <span className="mt-0.5 block truncate text-xs text-zinc-500">{complete ? 'Đã hoàn tất' : active ? 'Đang thực hiện' : 'Chưa hoàn tất'}</span>
    </span>
  </button>
);

export default function AdminAutoScheduleCreatePage() {
  const { triggerToast } = useOutletContext() || {};
  const navigate = useNavigate();
  const location = useLocation();
  const [activeStep, setActiveStep] = useState(1);
  const [movieSearch, setMovieSearch] = useState('');
  const [movieFilter, setMovieFilter] = useState('eligible');
  const [selectedOnly, setSelectedOnly] = useState(false);
  const [expandedMovies, setExpandedMovies] = useState({});
  const [advancedOpen, setAdvancedOpen] = useState(false);

  const handleSuccess = previewPublicId => navigate(`/admin/showtime-schedules/${previewPublicId}`);
  const recreateContext = location.state?.autoScheduleRecreate || null;
  const form = useAutoScheduleForm({
    triggerToast,
    onSuccess: handleSuccess,
    initialDraft: recreateContext?.draft,
  });
  const {
    cinemas,
    movies,
    auditoriums,
    versionsByMovie,
    selectedCinemaId,
    setSelectedCinemaId,
    selectedCinema,
    scheduleFrom,
    setScheduleFrom,
    scheduleTo,
    setScheduleTo,
    slotGranularityMinutes,
    setSlotGranularityMinutes,
    previewTtlMinutes,
    setPreviewTtlMinutes,
    selectedAuditoriumIds,
    toggleAuditorium,
    selectAllActiveAuditoriums,
    clearAuditoriums,
    selectedMovieVersionIds,
    selectedVersions,
    toggleVersion,
    selectEligibleMovieVersions,
    clearMovieVersions,
    isLoadingCinemas,
    isLoadingAuditoriums,
    isLoadingMovies,
    isSubmitting,
    errors,
    readinessIssues,
    isReady,
    selectionNotice,
    movieLoadError,
    retryMovies,
    dateRangeInfo,
    handleSubmit,
  } = form;
  const existingSchedule = useExistingShowtimeSummary({
    cinemaSlug: selectedCinema?.slug,
    scheduleFrom,
    scheduleTo,
  });

  const visibleMovies = useMemo(() => {
    const query = movieSearch.trim().toLocaleLowerCase('vi');
    return movies.filter(movie => {
      const matchesFilter = movieFilter === 'eligible' ? movie.eligible : !movie.eligible;
      const matchesSearch = !query || movie.title?.toLocaleLowerCase('vi').includes(query);
      const versions = versionsByMovie[movie.publicId] || [];
      const matchesSelected = !selectedOnly
        || versions.some(version => selectedMovieVersionIds.includes(version.publicId));
      return matchesFilter && matchesSearch && matchesSelected;
    });
  }, [
    movieFilter,
    movieSearch,
    movies,
    selectedMovieVersionIds,
    selectedOnly,
    versionsByMovie,
  ]);

  const scopeComplete = Boolean(selectedCinemaId && scheduleFrom && scheduleTo && !dateRangeInfo.isTooLong && !Object.keys(dateRangeInfo.errors || {}).length);
  const roomsComplete = selectedAuditoriumIds.length > 0;
  const moviesComplete = selectedMovieVersionIds.length > 0;
  const steps = [
    { id: 1, title: 'Rạp & thời gian', complete: scopeComplete, disabled: false },
    { id: 2, title: 'Phòng chiếu', complete: roomsComplete, disabled: !scopeComplete },
    { id: 3, title: 'Phim', complete: moviesComplete, disabled: !scopeComplete || !roomsComplete },
    { id: 4, title: 'Kiểm tra', complete: isReady, disabled: !scopeComplete || !roomsComplete || !moviesComplete },
  ];

  const hasAdvancedErrors = Boolean(errors.previewTtlMinutes || errors.slotGranularityMinutes);

  const chooseStep = step => {
    if (step === 2 && !scopeComplete) return;
    if (step === 3 && !roomsComplete) return;
    if (step === 4 && !moviesComplete) return;
    setActiveStep(step);
  };

  const next = () => {
    if (activeStep === 1 && scopeComplete) setActiveStep(2);
    if (activeStep === 2 && roomsComplete) setActiveStep(3);
    if (activeStep === 3 && moviesComplete) setActiveStep(4);
  };

  const applyDatePreset = days => {
    const start = scheduleFrom || dateRangeInfo.cinemaToday;
    if (!start) return;
    setScheduleFrom(start);
    setScheduleTo(addDays(start, days - 1));
  };

  const activeRoomCount = auditoriums.filter(room => room.status === 'ACTIVE').length;
  const selectedMovieNames = [...new Set(selectedVersions.map(version => version.movieTitle))];

  return (
    <div className="min-h-full space-y-6 bg-zinc-950 text-white animate-fade-in">
      <header className="flex items-start gap-4 border-b border-zinc-800 pb-6">
        <button type="button" onClick={() => navigate(-1)} aria-label="Quay lại" className="mt-1 rounded-xl p-2 text-zinc-400 hover:bg-zinc-800 hover:text-white">
          <ArrowLeft className="h-5 w-5" aria-hidden="true" />
        </button>
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-brand-orange">Tạo lịch chiếu</p>
          <h1 className="mt-2 text-3xl font-black tracking-tight">Tạo lịch tuần</h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-400">Chọn vài thông tin cơ bản, hệ thống sẽ tự xếp giờ và cho bạn kiểm tra trước khi mở bán.</p>
        </div>
      </header>

      {recreateContext && (
        <section className="rounded-xl border border-blue-500/30 bg-blue-500/10 p-4 text-sm text-blue-100" role="status">
          <p className="font-black">Đang tạo lại từ lịch {recreateContext.sourceShortCode}</p>
          <p className="mt-1 text-blue-200/80">
            Rạp, khoảng ngày, phòng và phim cũ đã được điền lại. Hãy kiểm tra các lựa chọn trước khi tạo bản đề xuất mới.
          </p>
        </section>
      )}

      <nav className="flex flex-col gap-2 lg:flex-row" aria-label="Các bước tạo lịch">
        {steps.map(step => (
          <Step
            key={step.id}
            number={step.id}
            title={step.title}
            active={activeStep === step.id}
            complete={step.complete}
            disabled={step.disabled}
            onClick={() => chooseStep(step.id)}
          />
        ))}
      </nav>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <main className="space-y-5">
          {activeStep === 1 && (
            <section className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-5 md:p-6" aria-labelledby="scope-heading">
              <div className="mb-6 flex items-start gap-3">
                <span className="rounded-xl bg-brand-orange/10 p-2.5 text-brand-orange"><MapPin className="h-5 w-5" aria-hidden="true" /></span>
                <div>
                  <h2 id="scope-heading" className="text-xl font-black">Chọn rạp và khoảng ngày</h2>
                  <p className="mt-1 text-sm text-zinc-500">Một lần tạo lịch tối đa 7 ngày.</p>
                </div>
              </div>
              <div className="space-y-5">
                <label className="block space-y-1.5 text-sm font-bold text-zinc-300">
                  Rạp <span className="text-brand-orange">*</span>
                  <select value={selectedCinemaId} onChange={event => setSelectedCinemaId(event.target.value)} disabled={isLoadingCinemas} className={inputClassName}>
                    <option value="">Chọn rạp bạn muốn lập lịch</option>
                    {cinemas.map(cinema => <option key={cinema.publicId} value={cinema.publicId}>{cinema.name}</option>)}
                  </select>
                  {selectedCinema?.address && <span className="mt-1 block text-xs font-normal text-zinc-500">{selectedCinema.address}</span>}
                </label>
                <div className="grid gap-4 md:grid-cols-2">
                  <label className="space-y-1.5 text-sm font-bold text-zinc-300">
                    Từ ngày <span className="text-brand-orange">*</span>
                    <input type="date" value={scheduleFrom} onChange={event => setScheduleFrom(event.target.value)} className={inputClassName} />
                  </label>
                  <label className="space-y-1.5 text-sm font-bold text-zinc-300">
                    Đến ngày <span className="text-brand-orange">*</span>
                    <input type="date" value={scheduleTo} onChange={event => setScheduleTo(event.target.value)} className={inputClassName} />
                  </label>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <span className="mr-1 text-xs font-bold text-zinc-500">Chọn nhanh:</span>
                  {[1, 3, 7].map(days => <button key={days} type="button" onClick={() => applyDatePreset(days)} className="min-h-9 rounded-lg border border-zinc-700 px-3 text-xs font-bold text-zinc-300 hover:border-brand-orange hover:text-brand-orange">{days} ngày</button>)}
                </div>
                {dateRangeInfo.isTooLong && (
                  <div role="alert" className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-200">
                    <p className="font-black">Khoảng ngày đang dài hơn mức cho phép</p>
                    <p className="mt-1">Khoảng đã chọn gồm {dateRangeInfo.dayCount} ngày. Mỗi bản lịch tối đa 7 ngày. Bạn có thể tạo nhiều bản liên tiếp để chuẩn bị lịch cho cả tháng.</p>
                    <p className="mt-2 text-xs">Gợi ý: {formatPreviewDateRange(dateRangeInfo.suggestedScheduleFrom, dateRangeInfo.suggestedScheduleTo)}</p>
                  </div>
                )}
                {Object.values(dateRangeInfo.errors || {}).map(message => <p key={message} className="text-sm font-bold text-rose-300">{message}</p>)}
                <div className="flex justify-end border-t border-zinc-800 pt-5">
                  <button type="button" onClick={next} disabled={!scopeComplete} className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-brand-orange px-5 text-sm font-black text-zinc-950 disabled:cursor-not-allowed disabled:opacity-40">
                    Tiếp tục chọn phòng <ChevronRight className="h-4 w-4" aria-hidden="true" />
                  </button>
                </div>
              </div>
            </section>
          )}

          {activeStep === 2 && (
            <section className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-5 md:p-6" aria-labelledby="rooms-heading">
              <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
                <div className="flex items-start gap-3">
                  <span className="rounded-xl bg-brand-orange/10 p-2.5 text-brand-orange"><Users className="h-5 w-5" aria-hidden="true" /></span>
                  <div>
                    <h2 id="rooms-heading" className="text-xl font-black">Chọn phòng chiếu</h2>
                    <p className="mt-1 text-sm text-zinc-500">Chọn các phòng muốn dùng trong khoảng ngày này.</p>
                  </div>
                </div>
                <div className="flex gap-2">
                  <button type="button" onClick={selectAllActiveAuditoriums} className="min-h-9 rounded-lg border border-brand-orange/40 px-3 text-xs font-bold text-brand-orange hover:bg-brand-orange/10">Chọn tất cả đang hoạt động</button>
                  <button type="button" onClick={clearAuditoriums} className="min-h-9 rounded-lg border border-zinc-700 px-3 text-xs font-bold text-zinc-300 hover:bg-zinc-800">Xóa chọn</button>
                </div>
              </div>
              <p className="mb-4 text-sm font-bold text-zinc-300">Đã chọn {selectedAuditoriumIds.length}/{activeRoomCount || auditoriums.length} phòng đang hoạt động</p>
              {isLoadingAuditoriums ? <p className="py-10 text-center text-sm text-zinc-500">Đang tải phòng chiếu…</p> : auditoriums.length === 0 ? <p className="py-10 text-center text-sm text-zinc-500">Chưa có phòng đang hoạt động tại rạp này.</p> : (
                <div className="grid gap-3 md:grid-cols-2">
                  {auditoriums.map(room => {
                    const selected = selectedAuditoriumIds.includes(room.publicId);
                    return (
                      <label key={room.publicId} className={`flex cursor-pointer items-center gap-3 rounded-xl border p-4 transition-colors ${selected ? 'border-brand-orange bg-brand-orange/10' : 'border-zinc-800 bg-zinc-950 hover:border-zinc-700'}`}>
                        <input type="checkbox" checked={selected} onChange={() => toggleAuditorium(room.publicId)} className="h-4 w-4 accent-orange-500" />
                        <span className="min-w-0">
                          <span className="block font-black text-zinc-100">{room.name}</span>
                          <span className="mt-1 block text-xs text-zinc-500">{room.screenType || 'STANDARD'} · {room.soundType || 'Âm thanh tiêu chuẩn'} · {room.capacity || 0} ghế</span>
                        </span>
                      </label>
                    );
                  })}
                </div>
              )}
              {selectionNotice && <p className="mt-4 text-sm font-bold text-amber-300">{selectionNotice}</p>}
              <div className="mt-6 flex justify-between border-t border-zinc-800 pt-5">
                <button type="button" onClick={() => setActiveStep(1)} className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-zinc-700 px-4 text-sm font-bold text-zinc-300 hover:bg-zinc-800"><ChevronLeft className="h-4 w-4" aria-hidden="true" /> Quay lại</button>
                <button type="button" onClick={next} disabled={!roomsComplete} className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-brand-orange px-5 text-sm font-black text-zinc-950 disabled:opacity-40">Tiếp tục chọn phim <ChevronRight className="h-4 w-4" aria-hidden="true" /></button>
              </div>
            </section>
          )}

          {activeStep === 3 && (
            <section className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-5 md:p-6" aria-labelledby="movies-heading">
              <div className="mb-6 flex items-start gap-3">
                <span className="rounded-xl bg-brand-orange/10 p-2.5 text-brand-orange"><Film className="h-5 w-5" aria-hidden="true" /></span>
                <div>
                  <h2 id="movies-heading" className="text-xl font-black">Chọn phim muốn chiếu</h2>
                  <p className="mt-1 text-sm text-zinc-500">Chọn ít nhất một định dạng cho mỗi phim. Hệ thống sẽ tự xếp giờ chiếu.</p>
                </div>
              </div>
              <div className="flex flex-col gap-3 md:flex-row">
                <label className="relative flex-1">
                  <span className="sr-only">Tìm phim</span>
                  <input type="search" aria-label="Tìm phim" value={movieSearch} onChange={event => setMovieSearch(event.target.value)} placeholder="Tìm theo tên phim…" className={inputClassName} />
                </label>
                <div className="flex rounded-xl border border-zinc-700 bg-zinc-950 p-1">
                  <button type="button" onClick={() => setMovieFilter('eligible')} className={`rounded-lg px-3 text-xs font-bold ${movieFilter === 'eligible' ? 'bg-emerald-500/15 text-emerald-300' : 'text-zinc-500'}`}>Đủ điều kiện ({movies.filter(movie => movie.eligible).length})</button>
                  <button type="button" onClick={() => setMovieFilter('ineligible')} className={`rounded-lg px-3 text-xs font-bold ${movieFilter === 'ineligible' ? 'bg-rose-500/15 text-rose-300' : 'text-zinc-500'}`}>Bị loại ({movies.filter(movie => !movie.eligible).length})</button>
                </div>
              </div>
              <label className="mt-3 inline-flex min-h-9 cursor-pointer items-center gap-2 text-xs font-bold text-zinc-400">
                <input
                  type="checkbox"
                  checked={selectedOnly}
                  onChange={event => setSelectedOnly(event.target.checked)}
                  className="h-4 w-4 accent-orange-500"
                />
                Chỉ xem đã chọn
              </label>
              <div className="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-zinc-800 bg-zinc-950 p-3">
                <p className="text-sm text-zinc-400">Đã chọn <strong className="text-white">{selectedVersions.length}</strong> định dạng cho <strong className="text-white">{selectedMovieNames.length}</strong> phim</p>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => selectEligibleMovieVersions(
                      movies.filter(movie => movie.eligible).map(movie => movie.publicId),
                    )}
                    className="rounded-lg border border-brand-orange/40 px-3 py-2 text-xs font-bold text-brand-orange hover:bg-brand-orange/10"
                  >
                    Chọn phim đủ điều kiện
                  </button>
                  <button type="button" onClick={clearMovieVersions} className="rounded-lg border border-zinc-700 px-3 py-2 text-xs font-bold text-zinc-300 hover:bg-zinc-800">Bỏ chọn tất cả</button>
                </div>
              </div>
              {selectedMovieNames.length === 1 && (
                <div className="mt-3 flex gap-3 rounded-xl border border-amber-500/30 bg-amber-500/10 p-3 text-sm text-amber-100" role="status">
                  <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-amber-300" aria-hidden="true" />
                  <p><strong>Bạn đang chọn một phim.</strong> Lịch tạo ra có thể dồn nhiều suất vào phim này vì không có phim khác để cân bằng.</p>
                </div>
              )}
              {movieLoadError ? (
                <div className="mt-4 rounded-xl border border-rose-500/30 bg-rose-500/10 p-4 text-sm text-rose-200">
                  <p>Không thể tải danh sách phim.</p>
                  <button type="button" onClick={retryMovies} className="mt-3 rounded-lg border border-rose-400/40 px-3 py-2 text-xs font-bold">Thử tải lại</button>
                </div>
              ) : isLoadingMovies ? <p className="py-12 text-center text-sm text-zinc-500">Đang kiểm tra phim có thể chiếu…</p> : visibleMovies.length === 0 ? (
                <p className="py-12 text-center text-sm text-zinc-500">
                  {movieSearch.trim()
                    ? `Không tìm thấy phim khớp từ khóa “${movieSearch.trim()}”.`
                    : selectedOnly
                      ? 'Chưa có định dạng nào được chọn để hiển thị.'
                      : movieFilter === 'eligible'
                        ? 'Chưa có phim đủ điều kiện trong khoảng ngày đã chọn.'
                        : 'Không có phim bị loại trong khoảng ngày này.'}
                </p>
              ) : (
                <div className="mt-4 grid gap-3 md:grid-cols-2">
                  {visibleMovies.map(movie => {
                    const versions = versionsByMovie[movie.publicId] || [];
                    const expanded = Boolean(expandedMovies[movie.publicId]);
                    const selectedCount = versions.filter(version => selectedMovieVersionIds.includes(version.publicId)).length;
                    return (
                      <article key={movie.publicId} className={`overflow-hidden rounded-2xl border ${selectedCount ? 'border-brand-orange/50 bg-brand-orange/5' : 'border-zinc-800 bg-zinc-950'}`}>
                        <button type="button" onClick={() => setExpandedMovies(current => ({ ...current, [movie.publicId]: !expanded }))} className="flex w-full items-center gap-3 p-3 text-left hover:bg-zinc-900">
                          <MoviePoster src={movie.primaryPoster} title={movie.title} />
                          <span className="min-w-0 flex-1">
                            <span className={`inline-flex rounded-md border px-2 py-1 text-[10px] font-bold ${movie.eligible ? 'border-emerald-500/30 text-emerald-300' : 'border-rose-500/30 text-rose-300'}`}>{movie.eligible ? 'Có thể chiếu' : 'Chưa thể chiếu'}</span>
                            {movie.status === 'DRAFT' && (
                              <span className="ml-2 inline-flex rounded-md border border-amber-500/30 px-2 py-1 text-[10px] font-bold text-amber-300">
                                Phim nháp · chỉ lập lịch chuẩn bị
                              </span>
                            )}
                            <span className="mt-2 block truncate text-base font-black text-white">{movie.title}</span>
                            <span className="mt-1 block text-xs text-zinc-500">{movie.durationMinutes || '—'} phút · {versions.length} định dạng</span>
                            {selectedCount > 0 && <span className="mt-2 block text-xs font-bold text-brand-orange">{selectedCount} định dạng đã chọn</span>}
                          </span>
                          <ChevronRight className={`h-5 w-5 shrink-0 text-zinc-500 transition-transform ${expanded ? 'rotate-90' : ''}`} aria-hidden="true" />
                        </button>
                        {!movie.eligible && (
                          <div className="border-t border-rose-500/20 px-3 py-3 text-xs leading-5 text-rose-300">
                            {movie.reasons?.length > 0 && <p>{movie.reasons.map(getEligibilityReasonLabel).join(' ')}</p>}
                            <p className="mt-1 text-rose-200/75">Chỉ dùng để kiểm tra lý do, không thể chọn định dạng.</p>
                          </div>
                        )}
                        {expanded && (
                          <div className="space-y-2 border-t border-zinc-800 p-3">
                            <p className="text-xs font-bold text-zinc-500">Chọn định dạng trình chiếu</p>
                            {versions.length === 0 ? <p className="text-xs text-zinc-500">Phim chưa có định dạng.</p> : versions.map(version => {
                              const selectable = movie.eligible && version.status === 'ACTIVE';
                              const checked = selectedMovieVersionIds.includes(version.publicId);
                              return (
                                <label key={version.publicId} className={`flex items-center gap-3 rounded-xl border p-3 ${checked ? 'border-brand-orange/50 bg-brand-orange/10' : 'border-zinc-800'} ${selectable ? 'cursor-pointer' : 'cursor-not-allowed opacity-50'}`}>
                                  <input type="checkbox" checked={checked} disabled={!selectable} onChange={() => toggleVersion(version.publicId)} className="h-4 w-4 accent-orange-500" aria-label={version.versionName || version.format} />
                                  <span className="min-w-0"><span className="block text-sm font-bold text-zinc-100">{version.versionName || version.format}</span><span className="mt-1 block text-xs text-zinc-500">{version.audioLanguage ? `Audio: ${version.audioLanguage}` : ''}{version.subtitleLanguage ? ` · Phụ đề: ${version.subtitleLanguage}` : ''}</span></span>
                                </label>
                              );
                            })}
                          </div>
                        )}
                      </article>
                    );
                  })}
                </div>
              )}
              <div className="mt-6 flex justify-between border-t border-zinc-800 pt-5">
                <button type="button" onClick={() => setActiveStep(2)} className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-zinc-700 px-4 text-sm font-bold text-zinc-300 hover:bg-zinc-800"><ChevronLeft className="h-4 w-4" aria-hidden="true" /> Quay lại</button>
                <button type="button" onClick={next} disabled={!moviesComplete} className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-brand-orange px-5 text-sm font-black text-zinc-950 disabled:opacity-40">Kiểm tra lịch <ChevronRight className="h-4 w-4" aria-hidden="true" /></button>
              </div>
            </section>
          )}

          {activeStep === 4 && (
            <section className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-5 md:p-6" aria-labelledby="review-heading">
              <div className="mb-6 flex items-start gap-3">
                <span className="rounded-xl bg-emerald-500/10 p-2.5 text-emerald-300"><Check className="h-5 w-5" aria-hidden="true" /></span>
                <div>
                  <h2 id="review-heading" className="text-xl font-black">Kiểm tra trước khi tạo lịch</h2>
                  <p className="mt-1 text-sm text-zinc-500">Hệ thống sẽ tạo một lịch đang soạn để bạn kiểm tra. Chưa mở bán ngay.</p>
                </div>
              </div>
              <div className="grid gap-3 md:grid-cols-2">
                {[
                  ['Rạp', selectedCinema?.name || 'Chưa chọn'],
                  ['Khoảng ngày', formatPreviewDateRange(scheduleFrom, scheduleTo)],
                  ['Phòng chiếu', `${selectedAuditoriumIds.length} phòng`],
                  ['Phim', selectedMovieNames.join(', ') || 'Chưa chọn'],
                ].map(([label, value]) => <div key={label} className="rounded-xl border border-zinc-800 bg-zinc-950 p-4"><p className="text-xs font-bold text-zinc-500">{label}</p><p className="mt-2 text-sm font-black text-white">{value}</p></div>)}
              </div>
              <div className="mt-5 flex flex-col gap-3 rounded-xl border border-blue-500/30 bg-blue-500/10 p-4 text-sm text-blue-100 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="font-black">Cách lịch tự động xử lý lịch cũ</p>
                  <p className="mt-1 leading-6 text-blue-100/80">Hệ thống chỉ bổ sung vào khung còn trống. Suất đã có không bị di chuyển hoặc hủy; vì vậy ngày đã kín có thể không tạo thêm suất nào.</p>
                  {existingSchedule.isLoading && <p className="mt-1 text-xs text-blue-200/70" role="status">Đang kiểm tra lịch hiện có…</p>}
                  {!existingSchedule.isLoading && !existingSchedule.error && (
                    <p className="mt-1 text-xs font-bold text-blue-200">Đã tìm thấy {existingSchedule.totalExisting} suất hiện có trong khoảng ngày này.</p>
                  )}
                  {existingSchedule.error && <p className="mt-1 text-xs text-amber-200">Chưa tải được lịch hiện có; bạn vẫn có thể kiểm tra lại ở bản đề xuất.</p>}
                </div>
                <button
                  type="button"
                  onClick={() => navigate(`/admin/showtimes?${new URLSearchParams({
                    ...(selectedCinema?.slug ? { cinemaSlug: selectedCinema.slug } : {}),
                    date: scheduleFrom,
                  }).toString()}`)}
                  className="min-h-10 shrink-0 rounded-lg border border-blue-400/30 px-3 text-xs font-black text-blue-200 hover:bg-blue-500/10"
                >
                  Kiểm tra lịch hiện có
                </button>
              </div>
              {selectedMovieNames.length === 1 && (
                <div className="mt-4 flex gap-3 rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-100" role="status">
                  <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-amber-300" aria-hidden="true" />
                  <p><strong>Cảnh báo phân bổ:</strong> Bạn chỉ chọn {selectedMovieNames[0]}. Hệ thống không thể cân bằng suất giữa nhiều phim trong bản lịch này.</p>
                </div>
              )}
              {readinessIssues.length > 0 ? (
                <div role="alert" className="mt-5 rounded-xl border border-amber-500/30 bg-amber-500/10 p-4">
                  <p className="font-black text-amber-200">Cần hoàn tất trước khi tạo lịch</p>
                  <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-amber-100">{readinessIssues.map(issue => <li key={issue}>{issue}</li>)}</ul>
                </div>
              ) : (
                <div className="mt-5 flex items-start gap-3 rounded-xl border border-emerald-500/30 bg-emerald-500/10 p-4 text-sm text-emerald-200">
                  <Check className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
                  <p>Thông tin đã hợp lệ. Bạn có thể tạo lịch để kiểm tra trước khi mở bán.</p>
                </div>
              )}
              <div className="mt-5 flex items-center justify-between border-t border-zinc-800 pt-5">
                <button type="button" onClick={() => setActiveStep(3)} className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-zinc-700 px-4 text-sm font-bold text-zinc-300 hover:bg-zinc-800"><ChevronLeft className="h-4 w-4" aria-hidden="true" /> Chỉnh lại</button>
                <button type="button" onClick={handleSubmit} disabled={!isReady || isSubmitting} className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-brand-orange px-5 text-sm font-black text-zinc-950 disabled:opacity-40">
                  {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" /> : <Save className="h-4 w-4" aria-hidden="true" />}
                  {isSubmitting ? 'Đang tạo lịch…' : 'Tạo lịch để kiểm tra'}
                </button>
              </div>
            </section>
          )}

          <details open={advancedOpen || hasAdvancedErrors} onToggle={event => setAdvancedOpen(event.currentTarget.open)} className="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-5">
            <summary className="flex cursor-pointer list-none items-center gap-2 text-sm font-black text-zinc-200">
              <Settings2 className="h-4 w-4 text-zinc-500" aria-hidden="true" />
              Tùy chọn nâng cao
              <span className="text-xs font-normal text-zinc-500">Không cần thay đổi nếu bạn chưa biết rõ</span>
            </summary>
            <div className="mt-4 grid gap-4 border-t border-zinc-800 pt-4 md:grid-cols-2">
              <label className="space-y-1.5 text-xs font-bold text-zinc-400">
                Khoảng cách giữa các mốc giờ (phút)
                <input type="number" min="5" max="60" step="5" value={slotGranularityMinutes} onChange={event => setSlotGranularityMinutes(event.target.value)} className={inputClassName} />
                {errors.slotGranularityMinutes && <span className="block text-rose-300">{errors.slotGranularityMinutes}</span>}
              </label>
              <label className="space-y-1.5 text-xs font-bold text-zinc-400">
                Giữ lịch đang soạn trong (phút)
                <input type="number" min="5" max="120" step="5" value={previewTtlMinutes} onChange={event => setPreviewTtlMinutes(event.target.value)} className={inputClassName} />
                {errors.previewTtlMinutes && <span className="block text-rose-300">{errors.previewTtlMinutes}</span>}
              </label>
            </div>
          </details>
        </main>

        <aside className="h-fit rounded-2xl border border-zinc-800 bg-zinc-900/70 p-5 xl:sticky xl:top-6">
          <div className="flex items-center gap-2 border-b border-zinc-800 pb-4">
            <CalendarDays className="h-5 w-5 text-brand-orange" aria-hidden="true" />
            <h2 className="font-black">Tóm tắt lịch</h2>
          </div>
          <dl className="mt-4 space-y-4 text-sm">
            <div><dt className="text-xs font-bold text-zinc-500">Rạp</dt><dd className="mt-1 font-bold text-zinc-200">{selectedCinema?.name || 'Chưa chọn'}</dd></div>
            <div><dt className="text-xs font-bold text-zinc-500">Khoảng ngày</dt><dd className="mt-1 font-bold text-zinc-200">{formatPreviewDateRange(scheduleFrom, scheduleTo)}</dd></div>
            <div className="grid grid-cols-3 gap-2">
              <div className="rounded-xl bg-zinc-950 p-3 text-center"><dt className="text-[11px] text-zinc-500">Ngày</dt><dd className="mt-1 text-lg font-black text-white">{dateRangeInfo.dayCount || 0}</dd></div>
              <div className="rounded-xl bg-zinc-950 p-3 text-center"><dt className="text-[11px] text-zinc-500">Phòng</dt><dd className="mt-1 text-lg font-black text-white">{selectedAuditoriumIds.length}</dd></div>
              <div className="rounded-xl bg-zinc-950 p-3 text-center"><dt className="text-[11px] text-zinc-500">Phim</dt><dd className="mt-1 text-lg font-black text-white">{selectedMovieNames.length}</dd></div>
            </div>
          </dl>
          <div className={`mt-5 rounded-xl border p-4 text-sm ${isReady ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-200' : 'border-amber-500/30 bg-amber-500/10 text-amber-200'}`}>
            <div className="flex items-start gap-2">
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
              <p>{isReady ? 'Đã đủ thông tin để tạo lịch kiểm tra.' : `Còn ${readinessIssues.length || 1} việc cần hoàn tất.`}</p>
            </div>
          </div>
          {selectedVersions.length > 0 && (
            <div className="mt-4 space-y-2">
              <p className="text-xs font-bold text-zinc-500">Định dạng đã chọn</p>
              {selectedVersions.slice(0, 4).map(version => (
                <button
                  type="button"
                  key={version.publicId}
                  onClick={() => toggleVersion(version.publicId)}
                  aria-label={`Bỏ chọn ${version.movieTitle} ${version.versionName || version.format}`}
                  className="flex w-full items-center justify-between rounded-lg border border-zinc-800 bg-zinc-950 px-3 py-2 text-left text-xs text-zinc-300 hover:border-brand-orange"
                >
                  <span className="truncate">{version.movieTitle} · {version.versionName || version.format}</span>
                  <span className="ml-2 text-zinc-500">Bỏ chọn</span>
                </button>
              ))}
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}
