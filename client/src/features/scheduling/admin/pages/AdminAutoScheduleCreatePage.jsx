import { useMemo, useState } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import {
  AlertCircle,
  ArrowLeft,
  CalendarDays,
  CheckCircle2,
  ChevronRight,
  Loader2,
  MapPin,
  Save,
  Search,
  Settings2,
  X,
} from 'lucide-react';
import SearchableSelect from '@/components/common/SearchableSelect';
import useAutoScheduleForm from '@/features/scheduling/admin/hooks/useAutoScheduleForm';

const sectionClassName = 'rounded-2xl border border-zinc-800 bg-zinc-900/60 p-5 md:p-6';
const inputClassName = 'min-h-11 w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-200 outline-none transition-colors focus:border-brand-orange/60 focus:ring-2 focus:ring-brand-orange/30';

const AdminAutoScheduleCreatePage = () => {
  const { triggerToast } = useOutletContext() || {};
  const navigate = useNavigate();
  const [expandedMovies, setExpandedMovies] = useState({});
  const [movieSearch, setMovieSearch] = useState('');
  const [showSelectedOnly, setShowSelectedOnly] = useState(false);

  const handleSuccess = previewPublicId => navigate(`/admin/showtime-schedules/${previewPublicId}`);

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
    isLoadingCinemas,
    isLoadingAuditoriums,
    isLoadingMovies,
    isSubmitting,
    errors,
    readinessIssues,
    isReady,
    selectionNotice,
    movieLoadError,
    dateRangeInfo,
    toggleMovieExpansion,
    handleSubmit,
  } = useAutoScheduleForm({ triggerToast, onSuccess: handleSuccess });

  const cinemaOptions = useMemo(() => cinemas.map(cinema => ({
    value: cinema.publicId,
    label: cinema.name,
    subtitle: cinema.address,
  })), [cinemas]);

  const filteredMovies = useMemo(() => {
    const query = movieSearch.trim().toLocaleLowerCase('vi');
    return movies.filter(movie => {
      const versions = versionsByMovie[movie.publicId] || [];
      const matchesSearch = !query || movie.title?.toLocaleLowerCase('vi').includes(query);
      const matchesSelected = !showSelectedOnly
        || versions.some(version => selectedMovieVersionIds.includes(version.publicId));
      return matchesSearch && matchesSelected;
    });
  }, [movieSearch, movies, selectedMovieVersionIds, showSelectedOnly, versionsByMovie]);

  const handleToggleMovie = movieId => {
    const nextExpanded = !expandedMovies[movieId];
    setExpandedMovies(previous => ({ ...previous, [movieId]: nextExpanded }));
    toggleMovieExpansion(movieId, nextExpanded);
  };

  const activeAuditoriumCount = auditoriums.filter(auditorium => auditorium.status === 'ACTIVE').length;
  const broadScope = dateRangeInfo.dayCount > 1
    && (selectedAuditoriumIds.length > 1 || selectedMovieVersionIds.length > 1);

  return (
    <div className="flex min-h-[400px] flex-1 flex-col space-y-6 overflow-auto bg-zinc-950 p-6 text-white animate-fade-in md:p-8">
      <header className="flex items-center gap-4 border-b border-zinc-800 pb-4">
        <button
          type="button"
          aria-label="Quay lại"
          onClick={() => navigate(-1)}
          className="min-h-11 min-w-11 rounded-xl p-2 text-zinc-400 transition-colors hover:bg-zinc-800 hover:text-white focus:outline-none focus:ring-2 focus:ring-brand-orange/60"
        >
          <ArrowLeft className="mx-auto h-5 w-5" aria-hidden="true" />
        </button>
        <div>
          <h1 className="flex items-center gap-2 text-xl font-black uppercase tracking-wider md:text-2xl">
            <Settings2 className="h-6 w-6 text-brand-orange" aria-hidden="true" />
            Cấu hình bản xem trước
          </h1>
          <p className="mt-1 text-sm text-zinc-500">Chọn phạm vi vận hành trước khi hệ thống tạo lịch đề xuất.</p>
        </div>
      </header>

      <p className="sr-only" aria-live="polite">{selectionNotice}</p>

      <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <main className="space-y-6">
          <section className={sectionClassName} aria-labelledby="scope-heading">
            <div className="mb-5 border-b border-zinc-800 pb-3">
              <h2 id="scope-heading" className="text-sm font-black uppercase tracking-wider text-zinc-200">1. Phạm vi</h2>
              <p className="mt-1 text-xs text-zinc-500">Khoảng lập kế hoạch có thể dài hơn, nhưng mỗi bản xem trước chỉ xử lý một lô từ 1–7 ngày.</p>
            </div>

            <div className="space-y-5">
              <div>
                <label id="cinema-label" htmlFor="auto-schedule-cinema" className="mb-1.5 block text-xs font-bold text-zinc-400">Cụm rạp *</label>
                <SearchableSelect
                  id="auto-schedule-cinema"
                  ariaLabelledBy="cinema-label"
                  ariaDescribedBy={errors.cinemaId ? 'cinema-error' : 'cinema-help'}
                  ariaInvalid={Boolean(errors.cinemaId)}
                  options={cinemaOptions}
                  value={selectedCinemaId}
                  onChange={setSelectedCinemaId}
                  placeholder="Chọn cụm rạp..."
                  disabled={isLoadingCinemas}
                  error={errors.cinemaId}
                />
                <p id="cinema-help" className="mt-1.5 flex items-center gap-1 text-[11px] text-zinc-500">
                  <MapPin className="h-3 w-3" aria-hidden="true" />
                  Múi giờ: {selectedCinema?.timezone || 'Sẽ hiển thị sau khi chọn rạp'}
                </p>
                {errors.cinemaId && <p id="cinema-error" className="mt-1 text-xs text-red-400">{errors.cinemaId}</p>}
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label htmlFor="schedule-from" className="mb-1.5 block text-xs font-bold text-zinc-400">Từ ngày *</label>
                  <input
                    id="schedule-from"
                    type="date"
                    value={scheduleFrom}
                    min={dateRangeInfo.cinemaToday || undefined}
                    aria-invalid={Boolean(errors.scheduleFrom)}
                    aria-describedby={errors.scheduleFrom ? 'schedule-from-error date-range-help' : 'date-range-help'}
                    onChange={event => setScheduleFrom(event.target.value)}
                    className={`${inputClassName} [color-scheme:dark] ${errors.scheduleFrom ? 'border-red-500' : ''}`}
                  />
                  {errors.scheduleFrom && <p id="schedule-from-error" className="mt-1 text-xs text-red-400">{errors.scheduleFrom}</p>}
                </div>
                <div>
                  <label htmlFor="schedule-to" className="mb-1.5 block text-xs font-bold text-zinc-400">Đến ngày *</label>
                  <input
                    id="schedule-to"
                    type="date"
                    value={scheduleTo}
                    min={scheduleFrom || dateRangeInfo.cinemaToday || undefined}
                    aria-invalid={Boolean(errors.scheduleTo)}
                    aria-describedby={errors.scheduleTo ? 'schedule-to-error date-range-help' : 'date-range-help'}
                    onChange={event => setScheduleTo(event.target.value)}
                    className={`${inputClassName} [color-scheme:dark] ${errors.scheduleTo ? 'border-red-500' : ''}`}
                  />
                  {errors.scheduleTo && <p id="schedule-to-error" className="mt-1 text-xs text-red-400">{errors.scheduleTo}</p>}
                </div>
              </div>

              <p id="date-range-help" className="flex items-start gap-1.5 text-xs leading-relaxed text-zinc-500">
                <AlertCircle className="mt-0.5 h-3.5 w-3.5 shrink-0" aria-hidden="true" />
                Mỗi bản xem trước tối đa 7 ngày. Bạn có thể tạo nhiều bản liên tiếp để lập lịch trước cho cả tháng.
              </p>
              {dateRangeInfo.isTooLong && (
                <div role="alert" className="rounded-xl border border-red-500/30 bg-red-500/10 p-3 text-xs leading-relaxed text-red-300">
                  Khoảng đã chọn gồm {dateRangeInfo.dayCount} ngày, nhưng mỗi bản xem trước chỉ được tối đa 7 ngày. Gợi ý khoảng hợp lệ đầu tiên:{' '}
                  <strong>{dateRangeInfo.suggestedScheduleFrom} đến {dateRangeInfo.suggestedScheduleTo}</strong>. Ngày bạn đã nhập được giữ nguyên; hãy điều chỉnh trước khi gửi.
                </div>
              )}
            </div>
          </section>

          <section className={sectionClassName} aria-labelledby="rooms-heading">
            <div className="mb-4 flex flex-col gap-3 border-b border-zinc-800 pb-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 id="rooms-heading" className="text-sm font-black uppercase tracking-wider text-zinc-200">2. Phòng chiếu</h2>
                <p className="mt-1 text-xs text-zinc-500">Bắt đầu với lựa chọn trống để tránh tạo phạm vi ngoài ý muốn.</p>
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={selectAllActiveAuditoriums}
                  disabled={!selectedCinemaId || isLoadingAuditoriums || activeAuditoriumCount === 0}
                  className="min-h-11 rounded-xl border border-brand-orange/30 px-3 text-xs font-bold text-brand-orange hover:bg-brand-orange/10 focus:outline-none focus:ring-2 focus:ring-brand-orange/60 disabled:opacity-40"
                >
                  Chọn tất cả đang hoạt động
                </button>
                <button
                  type="button"
                  onClick={clearAuditoriums}
                  disabled={selectedAuditoriumIds.length === 0}
                  className="min-h-11 rounded-xl border border-zinc-700 px-3 text-xs font-bold text-zinc-300 hover:bg-zinc-800 focus:outline-none focus:ring-2 focus:ring-brand-orange/60 disabled:opacity-40"
                >
                  Xóa chọn
                </button>
              </div>
            </div>

            <p className="mb-3 text-xs font-bold text-zinc-400">Đã chọn {selectedAuditoriumIds.length}/{activeAuditoriumCount} phòng đang hoạt động</p>
            {isLoadingAuditoriums ? (
              <div className="flex justify-center py-8"><Loader2 className="h-5 w-5 animate-spin text-zinc-500" aria-label="Đang tải phòng chiếu" /></div>
            ) : !selectedCinemaId ? (
              <p className="py-6 text-center text-xs text-zinc-500">Chọn cụm rạp để tải danh sách phòng.</p>
            ) : auditoriums.length === 0 ? (
              <p className="py-6 text-center text-xs text-zinc-500">Không có phòng chiếu khả dụng.</p>
            ) : (
              <div className="grid gap-3 md:grid-cols-2">
                {auditoriums.map(auditorium => {
                  const isActive = auditorium.status === 'ACTIVE';
                  const isChecked = selectedAuditoriumIds.includes(auditorium.publicId);
                  return (
                    <label key={auditorium.publicId} className={`flex min-h-16 items-start gap-3 rounded-xl border p-3 transition-colors ${isChecked ? 'border-brand-orange/50 bg-brand-orange/5' : 'border-zinc-800 bg-zinc-950/60'} ${isActive ? 'cursor-pointer hover:bg-zinc-800/40' : 'cursor-not-allowed opacity-50'}`}>
                      <input
                        type="checkbox"
                        checked={isChecked}
                        disabled={!isActive}
                        onChange={() => toggleAuditorium(auditorium.publicId)}
                        className="mt-1 h-4 w-4 rounded border-zinc-700 bg-zinc-950 text-brand-orange focus:ring-brand-orange"
                      />
                      <span className="min-w-0 flex-1">
                        <span className="flex items-center justify-between gap-2 text-sm font-bold text-white">
                          <span className="truncate">{auditorium.name}</span>
                          {!isActive && <span className="text-[9px] uppercase text-red-400">Không hoạt động</span>}
                        </span>
                        <span className="mt-1 flex flex-wrap gap-x-2 text-[10px] text-zinc-500">
                          <span>{auditorium.screenType || 'Màn hình tiêu chuẩn'}</span>
                          <span>{auditorium.soundType || 'Âm thanh tiêu chuẩn'}</span>
                          <span>{auditorium.capacity ?? '—'} ghế</span>
                        </span>
                      </span>
                    </label>
                  );
                })}
              </div>
            )}
            {errors.auditoriums && <p id="auditoriums-error" className="mt-3 text-xs text-red-400">{errors.auditoriums}</p>}
          </section>

          <section className={sectionClassName} aria-labelledby="movies-heading">
            <div className="mb-4 border-b border-zinc-800 pb-3">
              <h2 id="movies-heading" className="text-sm font-black uppercase tracking-wider text-zinc-200">3. Phim và định dạng</h2>
              <p className="mt-1 text-xs text-zinc-500">Phim không đủ điều kiện vẫn được hiển thị cùng lý do để bạn kiểm tra.</p>
            </div>

            <div className="mb-4 flex flex-col gap-3 sm:flex-row">
              <div className="relative flex-1">
                <label htmlFor="movie-search" className="sr-only">Tìm phim</label>
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500" aria-hidden="true" />
                <input
                  id="movie-search"
                  type="search"
                  value={movieSearch}
                  onChange={event => setMovieSearch(event.target.value)}
                  placeholder="Tìm phim..."
                  className={`${inputClassName} pl-9`}
                />
              </div>
              <label className="flex min-h-11 cursor-pointer items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-950 px-3 text-xs font-bold text-zinc-300">
                <input type="checkbox" checked={showSelectedOnly} onChange={event => setShowSelectedOnly(event.target.checked)} className="h-4 w-4 rounded border-zinc-700 bg-zinc-950 text-brand-orange focus:ring-brand-orange" />
                Chỉ xem đã chọn
              </label>
            </div>

            <div className="mb-4 rounded-xl border border-zinc-800 bg-zinc-950/60 p-3">
              <div className="mb-2 flex items-center justify-between gap-3">
                <h3 className="text-xs font-black uppercase tracking-wider text-zinc-400">Tóm tắt lựa chọn</h3>
                <span className="text-xs font-bold text-brand-orange">{selectedMovieVersionIds.length} định dạng</span>
              </div>
              {selectedVersions.length === 0 ? (
                <p className="text-xs text-zinc-500">Chưa chọn định dạng phim.</p>
              ) : (
                <div className="flex flex-wrap gap-2">
                  {selectedVersions.map(version => (
                    <button
                      key={version.publicId}
                      type="button"
                      aria-label={`Bỏ chọn ${version.movieTitle} ${version.versionName}`}
                      onClick={() => toggleVersion(version.publicId)}
                      className="inline-flex min-h-9 items-center gap-1.5 rounded-full border border-brand-orange/30 bg-brand-orange/10 px-3 text-xs font-bold text-brand-orange focus:outline-none focus:ring-2 focus:ring-brand-orange/60"
                    >
                      <span className="max-w-56 truncate">{version.movieTitle} · {version.versionName}</span>
                      <X className="h-3.5 w-3.5" aria-hidden="true" />
                    </button>
                  ))}
                </div>
              )}
            </div>

            {(errors.versions || errors.eligibility || movieLoadError) && (
              <p id="versions-error" role="alert" className="mb-3 rounded-xl border border-red-500/20 bg-red-500/10 p-3 text-xs text-red-300">
                {errors.versions || errors.eligibility || movieLoadError}
              </p>
            )}

            <div className="max-h-[560px] space-y-3 overflow-y-auto pr-1 custom-scrollbar">
              {isLoadingMovies ? (
                <div className="flex justify-center py-10"><Loader2 className="h-6 w-6 animate-spin text-zinc-500" aria-label="Đang tải phim" /></div>
              ) : filteredMovies.length === 0 ? (
                <p className="py-10 text-center text-sm text-zinc-500">Không có phim phù hợp với bộ lọc.</p>
              ) : filteredMovies.map(movie => {
                const isExpanded = Boolean(expandedMovies[movie.publicId]);
                const versions = versionsByMovie[movie.publicId] || [];
                const selectedCount = versions.filter(version => selectedMovieVersionIds.includes(version.publicId)).length;
                const contentId = `movie-${movie.publicId}-versions`;
                return (
                  <article key={movie.publicId} className="overflow-hidden rounded-xl border border-zinc-800 bg-zinc-950/60">
                    <button
                      type="button"
                      aria-expanded={isExpanded}
                      aria-controls={contentId}
                      onClick={() => handleToggleMovie(movie.publicId)}
                      className="flex min-h-16 w-full items-center justify-between gap-3 p-3 text-left transition-colors hover:bg-zinc-800/40 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-brand-orange/60 md:p-4"
                    >
                      <span className="flex min-w-0 items-center gap-3">
                        <span className={`rounded-lg bg-zinc-800 p-2 transition-transform ${isExpanded ? 'rotate-90' : ''}`}>
                          <ChevronRight className="h-4 w-4 text-zinc-400" aria-hidden="true" />
                        </span>
                        <span className="min-w-0">
                          <span className="flex flex-wrap items-center gap-2 text-sm font-bold text-white">
                            <span className="truncate">{movie.title}</span>
                            <span className={`rounded border px-2 py-0.5 text-[9px] font-black uppercase ${movie.eligible ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-400' : 'border-red-500/30 bg-red-500/10 text-red-400'}`}>
                              {movie.eligible ? 'Đủ điều kiện' : 'Không đủ điều kiện'}
                            </span>
                          </span>
                          <span className="mt-1 flex flex-wrap gap-2 text-[10px] text-zinc-500">
                            <span className="flex items-center gap-1"><CalendarDays className="h-3 w-3" aria-hidden="true" /> {movie.releaseDate || 'N/A'}</span>
                            <span>{movie.durationMinutes ?? '—'} phút</span>
                            {selectedCount > 0 && <span className="font-bold text-brand-orange">{selectedCount} đã chọn</span>}
                          </span>
                          {!movie.eligible && movie.reasons?.length > 0 && (
                            <span className="mt-2 block text-xs leading-relaxed text-red-300">
                              {movie.reasons.map(reason => reason.message || reason.code).join(' · ')}
                            </span>
                          )}
                        </span>
                      </span>
                    </button>

                    {isExpanded && (
                      <div id={contentId} className="grid gap-3 border-t border-zinc-800 bg-zinc-900/30 p-3 md:grid-cols-2 md:p-4">
                        {versions.length === 0 ? (
                          <p className="text-xs text-zinc-500">Không có định dạng phim.</p>
                        ) : versions.map(version => {
                          const isSelectable = movie.eligible && version.status === 'ACTIVE';
                          const isChecked = selectedMovieVersionIds.includes(version.publicId);
                          return (
                            <label key={version.publicId} className={`flex min-h-16 items-start gap-3 rounded-xl border p-3 ${isChecked ? 'border-brand-orange/50 bg-brand-orange/5' : 'border-zinc-800 bg-zinc-950/80'} ${isSelectable ? 'cursor-pointer hover:bg-zinc-800/40' : 'cursor-not-allowed opacity-55'}`}>
                              <input
                                type="checkbox"
                                checked={isChecked}
                                disabled={!isSelectable}
                                onChange={() => toggleVersion(version.publicId)}
                                className="mt-1 h-4 w-4 rounded border-zinc-700 bg-zinc-950 text-brand-orange focus:ring-brand-orange"
                              />
                              <span className="min-w-0 flex-1">
                                <span className="block truncate text-sm font-bold text-zinc-200">{version.versionName}</span>
                                <span className="mt-1 flex flex-wrap gap-1.5 text-[10px] text-zinc-500">
                                  <span>{version.format}</span>
                                  <span>Audio: {version.audioLanguage || '—'}</span>
                                  {version.subtitleLanguage && <span>Sub: {version.subtitleLanguage}</span>}
                                  {version.status !== 'ACTIVE' && <span className="font-bold text-red-400">Ngừng hoạt động</span>}
                                </span>
                              </span>
                            </label>
                          );
                        })}
                      </div>
                    )}
                  </article>
                );
              })}
            </div>
          </section>

          <section className={sectionClassName} aria-labelledby="settings-heading">
            <div className="mb-4 border-b border-zinc-800 pb-3">
              <h2 id="settings-heading" className="text-sm font-black uppercase tracking-wider text-zinc-200">4. Thiết lập tạo bản xem trước</h2>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="slot-granularity" className="mb-1.5 block text-xs font-bold text-zinc-400">Khoảng cách thử lịch (phút)</label>
                <input
                  id="slot-granularity"
                  type="number"
                  min="5"
                  max="60"
                  step="5"
                  value={slotGranularityMinutes}
                  aria-invalid={Boolean(errors.slotGranularityMinutes)}
                  aria-describedby={errors.slotGranularityMinutes ? 'slot-error' : 'slot-help'}
                  onChange={event => setSlotGranularityMinutes(event.target.value)}
                  className={`${inputClassName} ${errors.slotGranularityMinutes ? 'border-red-500' : ''}`}
                />
                <p id="slot-help" className="mt-1 text-[11px] text-zinc-500">Các mốc bắt đầu được thử theo bước này.</p>
                {errors.slotGranularityMinutes && <p id="slot-error" className="mt-1 text-xs text-red-400">{errors.slotGranularityMinutes}</p>}
              </div>
              <div>
                <label htmlFor="preview-ttl" className="mb-1.5 block text-xs font-bold text-zinc-400">Bản xem trước hết hạn sau (phút)</label>
                <input
                  id="preview-ttl"
                  type="number"
                  min="5"
                  max="120"
                  step="5"
                  value={previewTtlMinutes}
                  aria-invalid={Boolean(errors.previewTtlMinutes)}
                  aria-describedby={errors.previewTtlMinutes ? 'ttl-error' : 'ttl-help'}
                  onChange={event => setPreviewTtlMinutes(event.target.value)}
                  className={`${inputClassName} ${errors.previewTtlMinutes ? 'border-red-500' : ''}`}
                />
                <p id="ttl-help" className="mt-1 text-[11px] text-zinc-500">Hết hạn trước khi áp dụng sẽ yêu cầu tạo lại.</p>
                {errors.previewTtlMinutes && <p id="ttl-error" className="mt-1 text-xs text-red-400">{errors.previewTtlMinutes}</p>}
              </div>
            </div>
            {broadScope && (
              <div className="mt-4 flex items-start gap-2 rounded-xl border border-amber-500/25 bg-amber-500/10 p-3 text-xs leading-relaxed text-amber-300">
                <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
                Phạm vi gồm {dateRangeInfo.dayCount} ngày, {selectedAuditoriumIds.length} phòng và {selectedMovieVersionIds.length} định dạng. Phạm vi rộng có thể mất nhiều thời gian hoặc chạm giới hạn ứng viên; hãy thu hẹp nếu hệ thống yêu cầu.
              </div>
            )}
          </section>
        </main>

        <aside className="xl:sticky xl:top-24" aria-labelledby="readiness-heading">
          <div className="rounded-2xl border border-zinc-800 bg-zinc-900 p-5 shadow-xl">
            <div className="mb-4 flex items-center gap-2 border-b border-zinc-800 pb-3">
              {isReady ? <CheckCircle2 className="h-5 w-5 text-emerald-400" aria-hidden="true" /> : <AlertCircle className="h-5 w-5 text-amber-400" aria-hidden="true" />}
              <h2 id="readiness-heading" className="text-sm font-black uppercase tracking-wider text-white">Sẵn sàng tạo</h2>
            </div>

            <dl className="space-y-3 text-xs">
              <div><dt className="text-zinc-500">Cụm rạp</dt><dd className="mt-0.5 font-bold text-zinc-200">{selectedCinema?.name || 'Chưa chọn'}</dd></div>
              <div><dt className="text-zinc-500">Múi giờ</dt><dd className="mt-0.5 font-bold text-zinc-200">{selectedCinema?.timezone || '—'}</dd></div>
              <div><dt className="text-zinc-500">Lô xem trước</dt><dd className="mt-0.5 font-bold text-zinc-200">{scheduleFrom || '—'} → {scheduleTo || '—'}</dd></div>
              <div className="grid grid-cols-3 gap-2">
                <div className="rounded-lg bg-zinc-950 p-2 text-center"><dt className="text-zinc-500">Ngày</dt><dd className="mt-1 text-base font-black text-white">{dateRangeInfo.dayCount || 0}</dd></div>
                <div className="rounded-lg bg-zinc-950 p-2 text-center"><dt className="text-zinc-500">Phòng</dt><dd className="mt-1 text-base font-black text-white">{selectedAuditoriumIds.length}</dd></div>
                <div className="rounded-lg bg-zinc-950 p-2 text-center"><dt className="text-zinc-500">Định dạng</dt><dd className="mt-1 text-base font-black text-white">{selectedMovieVersionIds.length}</dd></div>
              </div>
            </dl>

            <div className="mt-4" aria-live="polite">
              {isReady ? (
                <p className="rounded-xl border border-emerald-500/20 bg-emerald-500/10 p-3 text-xs font-bold text-emerald-300">Cấu hình hợp lệ và sẵn sàng tạo bản xem trước.</p>
              ) : (
                <div className="rounded-xl border border-amber-500/20 bg-amber-500/10 p-3">
                  <p className="text-xs font-bold text-amber-300">Cần hoàn tất:</p>
                  <ul className="mt-2 list-disc space-y-1 pl-4 text-xs text-amber-200/80">
                    {readinessIssues.map(issue => <li key={issue}>{issue}</li>)}
                  </ul>
                </div>
              )}
            </div>

            <button
              type="button"
              onClick={handleSubmit}
              disabled={!isReady || isSubmitting}
              className="mt-5 flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-brand-orange px-5 text-xs font-black uppercase tracking-wider text-zinc-950 shadow-lg shadow-brand-orange/10 transition-colors hover:bg-amber-400 focus:outline-none focus:ring-2 focus:ring-brand-orange focus:ring-offset-2 focus:ring-offset-zinc-900 disabled:cursor-not-allowed disabled:opacity-45"
            >
              {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" /> : <Save className="h-4 w-4" aria-hidden="true" />}
              Tạo bản xem trước
            </button>
          </div>
        </aside>
      </div>
    </div>
  );
};

export default AdminAutoScheduleCreatePage;
