import { useState } from 'react';
import {
  ChevronDown,
  Cloud,
  Filter,
  ListFilter,
  Plus,
  Search,
  Settings2,
} from 'lucide-react';
import { useLocation, useNavigate, useOutletContext } from 'react-router-dom';
import useAdminMovies from '@/features/catalog/admin/hooks/useAdminMovies';
import useMovieSummary from '@/features/catalog/admin/hooks/useMovieSummary';
import MovieAdvancedFilters from '@/features/catalog/admin/components/MovieAdvancedFilters';
import MovieFormModal from '@/features/catalog/admin/components/MovieFormModal';
import MovieSummaryCards from '@/features/catalog/admin/components/MovieSummaryCards';
import MovieTable from '@/features/catalog/admin/components/MovieTable';
import MovieTmdbQueuePanel from '@/features/catalog/admin/components/MovieTmdbQueuePanel';
import TmdbSyncStatusPanel from '@/features/catalog/admin/components/TmdbSyncStatusPanel';
import { ADMIN_MOVIE_STATUS_TABS } from '@/features/catalog/admin/config/movieStatusConfig';
import { ADVANCED_FILTER_KEYS, countAdvancedFilters } from '@/features/catalog/admin/utils/adminMovieQuery';

export default function AdminMoviePage() {
  const { triggerToast, triggerConfirm } = useOutletContext() || {};
  const navigate = useNavigate();
  const location = useLocation();
  const summaryState = useMovieSummary();
  const adminMovies = useAdminMovies({
    triggerConfirm,
    triggerToast,
    onMutation: summaryState.fetchSummary,
  });
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [selectedMovie, setSelectedMovie] = useState(null);
  const [advancedOpen, setAdvancedOpen] = useState(false);

  const { query } = adminMovies;
  const advancedFilterCount = countAdvancedFilters(query);
  const isTmdbApprovalQueue = query.status === 'DRAFT' && query.source === 'TMDB';

  const handleOpenAdd = () => {
    setSelectedMovie(null);
    setIsFormOpen(true);
  };

  const handleOpenEdit = movie => {
    if (!movie?.publicId) {
      triggerToast?.('Không xác định được mã phim để chỉnh sửa.', 'error');
      return;
    }
    setSelectedMovie(movie);
    setIsFormOpen(true);
  };

  const handleOpenDetail = movie => {
    if (!movie?.publicId) {
      triggerToast?.('Không xác định được mã phim để mở hồ sơ.', 'error');
      return;
    }
    navigate(`/admin/movies/${encodeURIComponent(movie.publicId)}${location.search}`);
  };

  const selectStatus = status => {
    adminMovies.commitQuery({
      status,
      source: '',
      healthStatus: '',
    });
  };

  const applyAdvancedFilters = draft => {
    const changes = { sort: draft.sort };
    ADVANCED_FILTER_KEYS.forEach(key => { changes[key] = draft[key]; });
    adminMovies.commitQuery(changes);
  };

  const removeAdvancedFilter = key => {
    if (key === 'releaseDateRange') {
      adminMovies.commitQuery({ releaseDateFrom: '', releaseDateTo: '' });
    } else if (key === 'tmdbUpdatedRange') {
      adminMovies.commitQuery({ tmdbUpdatedFrom: '', tmdbUpdatedTo: '' });
    } else {
      adminMovies.commitQuery({ [key]: '' });
    }
  };

  const handleBulkApprove = async () => {
    const count = Math.min(adminMovies.totalElements, 100);
    if (count <= 0) return;

    const confirmed = await triggerConfirm?.({
      title: `Duyệt tối đa ${count} phim mới?`,
      message: 'Hệ thống sẽ kiểm tra từng phim và chỉ đưa vào phục vụ những phim đã đủ nội dung bắt buộc.',
      confirmLabel: 'Kiểm tra và duyệt',
    });
    if (!confirmed) return;

    await adminMovies.bulkApproveTmdbMovies(100);
  };

  const handleBulkArchive = async () => {
    const confirmed = await triggerConfirm?.({
      title: 'Lưu trữ các phim đã cũ?',
      message: 'Phim nhập tự động đã qua ngày phát hành và chưa được duyệt sẽ chuyển sang trạng thái tạm ngừng khai thác.',
      confirmLabel: 'Lưu trữ phim',
    });
    if (!confirmed) return;

    await adminMovies.bulkArchiveOldTmdbMovies(100);
  };

  return (
    <div
      className="flex h-full flex-col overflow-auto bg-zinc-950 text-white"
      data-testid="admin-movie-page"
    >
      <div className="mx-auto w-full max-w-[1600px] space-y-7 p-5 md:p-8">
        <header className="flex flex-col justify-between gap-5 border-b border-zinc-800 pb-6 lg:flex-row lg:items-end">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.18em] text-orange-400">
              Nội dung & phát hành
            </p>
            <h1 className="mt-2 text-2xl font-black text-white md:text-3xl">Quản lý phim</h1>
            <p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">
              Chọn nhóm phim cần xử lý, hoàn thiện hồ sơ rồi đưa phim vào phục vụ khách hàng.
            </p>
            <p className="mt-3 inline-flex rounded-lg border border-zinc-800 bg-zinc-900/60 px-3 py-2 text-xs text-zinc-500">
              Quy trình: Chọn phim → Hoàn thiện nội dung → Kiểm tra điều kiện → Duyệt phát hành
            </p>
          </div>
          <button
            type="button"
            onClick={handleOpenAdd}
            className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-orange-500 px-5 py-3 text-sm font-black text-zinc-950 transition hover:bg-orange-400"
          >
            <Plus className="h-4 w-4" />
            Thêm phim thủ công
          </button>
        </header>

        <MovieSummaryCards
          summary={summaryState.summary}
          query={query}
          isLoading={summaryState.isLoading}
          isRefreshing={summaryState.isRefreshing}
          error={summaryState.error}
          onRetry={summaryState.fetchSummary}
          onSelect={changes => adminMovies.commitQuery(changes)}
        />

        <section className="space-y-4" aria-labelledby="movie-list-title">
          <div className="rounded-2xl border border-zinc-800 bg-zinc-900/35">
            <div className="flex flex-col gap-2 border-b border-zinc-800 px-4 py-4 sm:flex-row sm:items-center sm:justify-between md:px-5">
              <div>
                <h2 id="movie-list-title" className="flex items-center gap-2 text-base font-bold text-white">
                  <ListFilter className="h-4 w-4 text-orange-400" />
                  Danh sách phim
                </h2>
                <p className="mt-1 text-xs text-zinc-500">
                  {adminMovies.totalElements.toLocaleString('vi-VN')} phim phù hợp với lựa chọn hiện tại
                </p>
              </div>
              {(advancedFilterCount > 0 || query.keyword) && (
                <button
                  type="button"
                  onClick={() => {
                    adminMovies.clearAdvancedFilters();
                    adminMovies.setSearchInput('');
                  }}
                  className="self-start text-xs font-semibold text-zinc-400 underline decoration-zinc-700 underline-offset-4 hover:text-white sm:self-auto"
                >
                  Xóa bộ lọc
                </button>
              )}
            </div>

            <div className="border-b border-zinc-800 px-4 py-3 md:px-5">
              <p className="mb-2 text-[11px] font-bold uppercase tracking-wide text-zinc-600">Trạng thái phim</p>
              <div className="flex flex-wrap gap-2">
                {ADMIN_MOVIE_STATUS_TABS.map(tab => {
                  const active = query.status === tab.value && !query.source && !query.healthStatus;
                  return (
                    <button
                      key={tab.value}
                      type="button"
                      onClick={() => selectStatus(tab.value)}
                      aria-pressed={active}
                      className={`rounded-lg border px-3 py-2 text-xs font-semibold transition ${
                        active
                          ? 'border-orange-500 bg-orange-500/10 text-orange-300'
                          : 'border-zinc-800 bg-zinc-950/50 text-zinc-400 hover:border-zinc-700 hover:text-zinc-200'
                      }`}
                    >
                      {tab.label}
                    </button>
                  );
                })}
                <span className="mx-1 hidden w-px bg-zinc-800 sm:block" aria-hidden="true" />
                <button
                  type="button"
                  onClick={() => adminMovies.commitQuery({
                    status: 'DRAFT',
                    source: 'TMDB',
                    healthStatus: '',
                  })}
                  aria-pressed={isTmdbApprovalQueue}
                  className={`inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-xs font-semibold transition ${
                    isTmdbApprovalQueue
                      ? 'border-sky-500 bg-sky-500/10 text-sky-300'
                      : 'border-zinc-800 bg-zinc-950/50 text-zinc-400 hover:border-zinc-700 hover:text-zinc-200'
                  }`}
                >
                  <Cloud className="h-3.5 w-3.5" />
                  Phim nhập tự động
                </button>
              </div>
            </div>

            <div className="flex flex-col gap-3 p-4 lg:flex-row lg:items-center lg:justify-between md:p-5">
              <div className="relative w-full lg:max-w-md">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500" />
                <input
                  type="search"
                  value={adminMovies.searchInput}
                  onChange={event => adminMovies.setSearchInput(event.target.value)}
                  placeholder="Tìm theo tên phim…"
                  aria-label="Tìm theo tên phim"
                  className="h-11 w-full rounded-xl border border-zinc-800 bg-zinc-950 pl-10 pr-4 text-sm text-zinc-100 outline-none transition placeholder:text-zinc-600 focus:border-orange-500/60"
                />
              </div>
              <div className="flex flex-col gap-2 sm:flex-row">
                <button
                  type="button"
                  onClick={() => setAdvancedOpen(open => !open)}
                  aria-expanded={advancedOpen}
                  className={`inline-flex h-11 items-center justify-center gap-2 rounded-xl border px-4 text-xs font-bold transition ${
                    advancedOpen || advancedFilterCount > 0
                      ? 'border-orange-500 bg-orange-500/10 text-orange-300'
                      : 'border-zinc-700 bg-zinc-800 text-zinc-300 hover:bg-zinc-700'
                  }`}
                >
                  <Filter className="h-4 w-4" />
                  Lọc chi tiết
                  {advancedFilterCount > 0 && (
                    <span className="rounded-full bg-orange-500 px-1.5 py-0.5 text-[10px] text-black">
                      {advancedFilterCount}
                    </span>
                  )}
                </button>
                <label className="flex h-11 items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-950 px-3 text-xs text-zinc-500">
                  Hiển thị
                  <select
                    aria-label="Số phim mỗi trang"
                    value={query.size}
                    onChange={event => adminMovies.commitQuery({ size: Number(event.target.value) })}
                    className="bg-transparent font-bold text-zinc-200 outline-none"
                  >
                    {[5, 10, 20, 50].map(size => (
                      <option key={size} value={size}>{size} phim</option>
                    ))}
                  </select>
                </label>
              </div>
            </div>

            <div className="px-4 pb-4 md:px-5 md:pb-5">
              <MovieAdvancedFilters
                query={query}
                genres={adminMovies.genresList}
                isOpen={advancedOpen}
                onApply={applyAdvancedFilters}
                onReset={adminMovies.clearAdvancedFilters}
                onRemove={removeAdvancedFilter}
              />
            </div>
          </div>

          {isTmdbApprovalQueue && (
            <details className="group rounded-2xl border border-sky-500/20 bg-sky-500/[0.03]">
              <summary className="flex cursor-pointer list-none items-center justify-between gap-4 px-5 py-4 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500">
                <span className="flex items-start gap-3">
                  <span className="rounded-lg bg-sky-500/10 p-2 text-sky-300">
                    <Settings2 className="h-4 w-4" />
                  </span>
                  <span>
                    <span className="block text-sm font-bold text-zinc-100">Xử lý nhiều phim nhập tự động</span>
                    <span className="mt-1 block text-xs text-zinc-500">
                      Công cụ dành cho người quản trị có kinh nghiệm. Không cần mở khi xử lý từng phim.
                    </span>
                  </span>
                </span>
                <ChevronDown className="h-5 w-5 shrink-0 text-zinc-500 transition group-open:rotate-180" />
              </summary>
              <div className="border-t border-sky-500/15 p-4">
                <MovieTmdbQueuePanel
                  breakdown={adminMovies.queueBreakdown.data}
                  isBreakdownLoading={adminMovies.queueBreakdown.isLoading}
                  breakdownError={adminMovies.queueBreakdown.error}
                  approval={adminMovies.bulkApproval}
                  archive={adminMovies.bulkArchive}
                  onApprove={handleBulkApprove}
                  onArchive={handleBulkArchive}
                />
              </div>
            </details>
          )}

          <MovieTable
            movies={adminMovies.movies}
            isInitialLoading={adminMovies.isInitialLoading}
            isRefreshing={adminMovies.isRefreshing}
            error={adminMovies.error}
            emptyDatabase={summaryState.summary?.total === 0}
            currentPage={query.page}
            pageSize={query.size}
            totalElements={adminMovies.totalElements}
            totalPages={adminMovies.totalPages}
            onRetry={adminMovies.fetchMovies}
            onPageChange={page => adminMovies.commitQuery({ page }, { resetPage: false })}
            onOpenDetail={handleOpenDetail}
            onOpenEdit={handleOpenEdit}
            onDelete={adminMovies.handleDelete}
          />
        </section>

        <details className="group rounded-2xl border border-zinc-800 bg-zinc-900/25">
          <summary className="flex cursor-pointer list-none items-center justify-between gap-4 px-5 py-4 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-orange-500">
            <span>
              <span className="block text-sm font-semibold text-zinc-300">Thông tin hệ thống nhập phim</span>
              <span className="mt-1 block text-xs text-zinc-600">
                Chỉ cần mở khi cần kiểm tra tình trạng đồng bộ dữ liệu.
              </span>
            </span>
            <ChevronDown className="h-5 w-5 text-zinc-600 transition group-open:rotate-180" />
          </summary>
          <div className="border-t border-zinc-800 p-4">
            <TmdbSyncStatusPanel />
          </div>
        </details>
      </div>

      {isFormOpen && (
        <MovieFormModal
          selectedMovie={selectedMovie}
          genresList={adminMovies.genresList}
          setGenresList={adminMovies.setGenresList}
          triggerToast={triggerToast}
          onClose={() => setIsFormOpen(false)}
          onRefreshList={adminMovies.refreshAll}
          detailQuery={location.search}
        />
      )}
    </div>
  );
}
