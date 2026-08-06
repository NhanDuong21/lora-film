import { useMemo } from 'react';
import { Link, useLocation, useNavigate, useOutletContext } from 'react-router-dom';
import {
  AlertCircle,
  ArrowLeft,
  CalendarDays,
  CheckCircle2,
  ChevronRight,
  Film,
  Loader2,
  MapPin,
  Settings2,
  Sparkles,
  Users,
} from 'lucide-react';
import useAutoScheduleForm from '@/features/scheduling/admin/hooks/useAutoScheduleForm';
import { formatPreviewDateRange } from '@/features/scheduling/admin/utils/autoSchedulePreviewDateTime';

const inputClassName = 'min-h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none transition-colors focus:border-brand-orange focus:ring-2 focus:ring-brand-orange/20';

const formatVersionLabel = (movie, version) => (
  `${movie.title} · ${version.versionName || version.format || 'Định dạng mặc định'}`
);

const ScopeChoice = ({ id, label, included, excluded, onChange }) => (
  <div className="flex flex-col gap-3 rounded-xl border border-zinc-800 bg-zinc-950 p-3 sm:flex-row sm:items-center sm:justify-between">
    <span className="min-w-0 truncate text-sm font-bold text-zinc-200">{label}</span>
    <div className="flex shrink-0 gap-2">
      <label className={`cursor-pointer rounded-lg border px-3 py-2 text-xs font-bold ${included ? 'border-blue-500 bg-blue-500/10 text-blue-200' : 'border-zinc-700 text-zinc-400'}`}>
        <input
          className="sr-only"
          type="checkbox"
          checked={included}
          onChange={event => onChange('include', id, event.target.checked)}
        />
        Ghim dùng
      </label>
      <label className={`cursor-pointer rounded-lg border px-3 py-2 text-xs font-bold ${excluded ? 'border-rose-500 bg-rose-500/10 text-rose-200' : 'border-zinc-700 text-zinc-400'}`}>
        <input
          className="sr-only"
          type="checkbox"
          checked={excluded}
          onChange={event => onChange('exclude', id, event.target.checked)}
        />
        Loại khỏi lịch
      </label>
    </div>
  </div>
);

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
  const advancedChoiceCount = includeAuditoriumIds.length + excludeAuditoriumIds.length
    + includeMovieVersionIds.length + excludeMovieVersionIds.length;

  return (
    <div className="min-h-full space-y-6 bg-zinc-950 text-white animate-fade-in">
      <header className="flex items-start gap-4 border-b border-zinc-800 pb-6">
        <button type="button" onClick={() => navigate(-1)} aria-label="Quay lại" className="mt-1 rounded-xl p-2 text-zinc-400 hover:bg-zinc-800 hover:text-white">
          <ArrowLeft className="h-5 w-5" aria-hidden="true" />
        </button>
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-brand-orange">Auto Schedule · Demand-Aware</p>
          <h1 className="mt-2 text-3xl font-black tracking-tight">Tạo lịch tối ưu</h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">
            Chọn rạp và số ngày. Hệ thống tự xác định phim, định dạng, phòng, nhu cầu và khung giờ phù hợp trước khi cho bạn xem preview.
          </p>
        </div>
      </header>

      {recreateContext && (
        <section className="rounded-xl border border-blue-500/30 bg-blue-500/10 p-4 text-sm text-blue-100" role="status">
          <p className="font-black">Đang tạo lại từ lịch {recreateContext.sourceShortCode}</p>
          <p className="mt-1 text-blue-200/80">Quick Mode sẽ lập lại từ ngày mai; các giới hạn phòng và phim cũ đã được chuyển vào phần Nâng cao để bạn kiểm tra.</p>
        </section>
      )}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
        <main className="space-y-5">
          <section className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-5 md:p-6" aria-labelledby="quick-mode-heading">
            <div className="mb-6 flex items-start gap-3">
              <span className="rounded-xl bg-brand-orange/10 p-2.5 text-brand-orange"><Sparkles className="h-5 w-5" aria-hidden="true" /></span>
              <div>
                <h2 id="quick-mode-heading" className="text-xl font-black">Quick Mode</h2>
                <p className="mt-1 text-sm text-zinc-500">Không cần chấm độ hot hoặc chọn thủ công từng phim và phòng.</p>
              </div>
            </div>

            <label className="block space-y-1.5 text-sm font-bold text-zinc-300">
              Rạp <span className="text-brand-orange">*</span>
              <select
                value={selectedCinemaId}
                onChange={event => setSelectedCinemaId(event.target.value)}
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
                    onClick={() => setPlanningPreset(preset.days)}
                    className={`rounded-xl border p-4 text-left transition-colors ${planningDays === preset.days ? 'border-brand-orange bg-brand-orange/10' : 'border-zinc-700 bg-zinc-950 hover:border-zinc-500'}`}
                  >
                    <span className={`block font-black ${planningDays === preset.days ? 'text-brand-orange' : 'text-zinc-200'}`}>{preset.label}</span>
                    <span className="mt-1 block text-xs text-zinc-500">{preset.detail}</span>
                  </button>
                ))}
              </div>
            </fieldset>
          </section>

          <section className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-5 md:p-6" aria-labelledby="preflight-heading">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="flex items-start gap-3">
                <span className="rounded-xl bg-blue-500/10 p-2.5 text-blue-300"><CheckCircle2 className="h-5 w-5" aria-hidden="true" /></span>
                <div>
                  <h2 id="preflight-heading" className="text-xl font-black">Kiểm tra trước khi tối ưu</h2>
                  <p className="mt-1 text-sm text-zinc-500">Eligibility, tương thích định dạng, giờ hoạt động, bảo trì và bảng giá.</p>
                </div>
              </div>
              <button type="button" onClick={() => runPreflight()} disabled={!selectedCinemaId || isCheckingPreflight} className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-zinc-700 px-4 text-sm font-bold text-zinc-200 disabled:opacity-40">
                {isCheckingPreflight && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
                Kiểm tra lại
              </button>
            </div>

            {!selectedCinemaId && <p className="mt-5 rounded-xl border border-zinc-800 bg-zinc-950 p-4 text-sm text-zinc-500">Chọn rạp để hệ thống tự chạy preflight.</p>}
            {selectedCinemaId && isCheckingPreflight && !preflight && <p className="mt-5 flex items-center gap-2 text-sm text-blue-200"><Loader2 className="h-4 w-4 animate-spin" /> Đang kiểm tra dữ liệu vận hành…</p>}
            {preflightError && <p role="alert" className="mt-5 rounded-xl border border-rose-500/30 bg-rose-500/10 p-4 text-sm text-rose-200">{preflightError}</p>}

            {preflight && (
              <div className="mt-5 space-y-4">
                <div className={`rounded-xl border p-4 ${preflight.canGenerate ? 'border-emerald-500/30 bg-emerald-500/10' : 'border-amber-500/30 bg-amber-500/10'}`}>
                  <p className={`font-black ${preflight.canGenerate ? 'text-emerald-200' : 'text-amber-200'}`}>
                    {preflight.canGenerate ? 'Sẵn sàng tạo lịch' : 'Cần xử lý trước khi tạo lịch'}
                  </p>
                  <p className="mt-1 text-sm text-zinc-300">{formatPreviewDateRange(preflight.planningFrom, preflight.planningTo)} · {preflight.timezone}</p>
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
                      <li key={blocker.code} className="flex flex-col gap-3 rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 sm:flex-row sm:items-center sm:justify-between">
                        <span className="flex items-start gap-2 text-sm text-amber-100"><AlertCircle className="mt-0.5 h-4 w-4 shrink-0" /> {blocker.message}</span>
                        {blocker.actionPath && <Link to={blocker.actionPath} className="inline-flex shrink-0 items-center gap-1 text-xs font-black text-brand-orange">Mở nơi xử lý <ChevronRight className="h-3.5 w-3.5" /></Link>}
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </section>

          <details className="group rounded-2xl border border-zinc-800 bg-zinc-900/60" open={Boolean(errors.slotGranularityMinutes || errors.previewTtlMinutes)}>
            <summary className="flex cursor-pointer list-none items-center justify-between gap-3 p-5 font-black text-zinc-200 md:p-6">
              <span className="flex items-center gap-3"><Settings2 className="h-5 w-5 text-brand-orange" /> Nâng cao <span className="text-xs font-normal text-zinc-500">(tùy chọn)</span></span>
              {advancedChoiceCount > 0 && <span className="rounded-full bg-blue-500/10 px-2.5 py-1 text-xs text-blue-200">{advancedChoiceCount} giới hạn</span>}
            </summary>
            <div className="space-y-6 border-t border-zinc-800 p-5 md:p-6">
              <p className="text-sm leading-6 text-zinc-400">Mặc định optimizer tự dùng toàn bộ dữ liệu đủ điều kiện. “Ghim dùng” giới hạn phạm vi vào các mục được ghim; “Loại khỏi lịch” bỏ mục đó khỏi lần chạy này.</p>
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="space-y-1.5 text-sm font-bold text-zinc-300">Bước thời gian (phút)
                  <input type="number" min="5" max="60" value={slotGranularityMinutes} onChange={event => setSlotGranularityMinutes(event.target.value)} className={inputClassName} />
                  {errors.slotGranularityMinutes && <span className="block text-xs text-rose-300">{errors.slotGranularityMinutes}</span>}
                </label>
                <label className="space-y-1.5 text-sm font-bold text-zinc-300">Thời gian giữ preview (phút)
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
                <h3 className="flex items-center gap-2 font-black"><Film className="h-4 w-4 text-brand-orange" /> Phiên bản phim đủ điều kiện</h3>
                <div className="mt-3 max-h-96 space-y-2 overflow-y-auto pr-1">
                  {versions.map(({ movie, version }) => (
                    <ScopeChoice
                      key={version.publicId}
                      id={`version:${version.publicId}`}
                      label={formatVersionLabel(movie, version)}
                      included={includeMovieVersionIds.includes(version.publicId)}
                      excluded={excludeMovieVersionIds.includes(version.publicId)}
                      onChange={setScopeChoice}
                    />
                  ))}
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
            {isCheckingPreflight ? 'Đang kiểm tra dữ liệu…' : isReady ? 'Preflight đã thông qua. Sẵn sàng chạy Demand-Aware CP-SAT.' : 'Chọn rạp và xử lý các blocker để tiếp tục.'}
          </div>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={!isReady || isSubmitting}
            className="mt-4 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-brand-orange px-5 text-sm font-black text-zinc-950 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
            {isSubmitting ? 'Đang tối ưu lịch…' : 'Tạo lịch tối ưu'}
          </button>
          <p className="mt-3 flex items-start gap-2 text-xs leading-5 text-zinc-500"><MapPin className="mt-0.5 h-3.5 w-3.5 shrink-0" /> Preview không mở bán tự động. Bạn vẫn phải xem và bấm áp dụng.</p>
        </aside>
      </div>
    </div>
  );
}
