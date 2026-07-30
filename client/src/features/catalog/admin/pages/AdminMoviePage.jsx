import { useState } from 'react';
import { Cloud, Filter, Plus, Search } from 'lucide-react';
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

  const handleOpenAdd = () => {
    setSelectedMovie(null);
    setIsFormOpen(true);
  };

  const handleOpenEdit = movie => {
    setSelectedMovie(movie);
    setIsFormOpen(true);
  };

  const handleOpenDetail = movie => {
    navigate(`/admin/movies/${movie.publicId}${location.search}`);
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

  const isTmdbApprovalQueue = query.status === 'DRAFT' && query.source === 'TMDB';

  const handleBulkApprove = async () => {
    const count = Math.min(adminMovies.totalElements, 100);
    if (count <= 0) return;

    const confirmed = await triggerConfirm?.({
      title: `Duyệt tối đa ${count} phim mới?`,
      message: 'Hệ thống sẽ kiểm tra từng phim và chỉ đưa vào khai thác những phim đã đủ nội dung cần thiết.',
      confirmLabel: 'Kiểm tra và duyệt',
    });
    if (!confirmed) return;

    await adminMovies.bulkApproveTmdbMovies(100);
  };

  const handleBulkArchive = async () => {
    const confirmed = await triggerConfirm?.({
      title: 'Lưu trữ các phim không còn phù hợp?',
      message: 'Các phim mới nhập nhưng đã qua ngày phát hành và chưa được duyệt sẽ được chuyển sang khu vực tạm ngừng khai thác.',
      confirmLabel: 'Lưu trữ phim',
    });
    if (!confirmed) return;

    await adminMovies.bulkArchiveOldTmdbMovies(100);
  };

  return (
    <div className="flex h-full flex-col overflow-auto bg-zinc-950 text-white" data-testid="admin-movie-page">
      <div className="space-y-6 p-6 md:p-8">
        <div className="border-b border-zinc-800 pb-4">
          <h1 className="text-xl font-black uppercase tracking-wider md:text-2xl">Trung tâm nội dung phim</h1>
          <p className="mt-2 max-w-3xl text-sm text-zinc-400">
            Duyệt phim mới, bổ sung nội dung còn thiếu và quản lý các phim đang phục vụ khách hàng.
          </p>
        </div>

        <MovieSummaryCards
          summary={summaryState.summary}
          query={query}
          isLoading={summaryState.isLoading}
          isRefreshing={summaryState.isRefreshing}
          error={summaryState.error}
          onRetry={summaryState.fetchSummary}
          onSelect={changes => adminMovies.commitQuery(changes)}
        />

        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => adminMovies.commitQuery({ status: 'DRAFT', source: 'TMDB' })}
            aria-pressed={query.status === 'DRAFT' && query.source === 'TMDB'}
            className={`inline-flex items-center gap-2 rounded-xl border px-4 py-2 text-xs font-bold transition-all ${
              query.status === 'DRAFT' && query.source === 'TMDB'
                ? 'border-brand-orange bg-brand-orange text-white'
                : 'border-transparent bg-zinc-900 text-zinc-400 hover:bg-zinc-800 hover:text-zinc-200'
            }`}
          >
            <Cloud className="h-4 w-4" /> Phim mới cần duyệt
          </button>
          {ADMIN_MOVIE_STATUS_TABS.map(tab => (
            <button
              key={tab.value}
              type="button"
              onClick={() => adminMovies.commitQuery({ status: tab.value })}
              aria-pressed={query.status === tab.value}
              className={`rounded-xl border px-4 py-2 text-xs font-bold transition-all ${
                query.status === tab.value
                  ? 'border-zinc-600 bg-zinc-700 text-white'
                  : 'border-transparent bg-zinc-900 text-zinc-400 hover:bg-zinc-800 hover:text-zinc-200'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="enterprise-card flex flex-col items-stretch justify-between gap-3 lg:flex-row lg:items-center py-3">
          <div className="relative w-full lg:w-80">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500" />
            <input
              type="search"
              value={adminMovies.searchInput}
              onChange={event => adminMovies.setSearchInput(event.target.value)}
              placeholder="Tìm theo tên phim…"
              className="w-full rounded-xl border border-zinc-800 bg-zinc-900 py-2.5 pl-9 pr-4 text-xs text-zinc-100 outline-none transition-colors placeholder:text-zinc-500 focus-ring"
            />
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <button
              type="button"
              onClick={() => setAdvancedOpen(open => !open)}
              aria-expanded={advancedOpen}
              className={`inline-flex items-center justify-center gap-2 rounded-xl border px-4 py-2.5 text-xs font-bold transition-colors ${
                advancedOpen || advancedFilterCount > 0
                  ? 'border-brand-orange bg-brand-orange/10 text-brand-orange'
                  : 'border-zinc-700 bg-zinc-800 text-zinc-300 hover:bg-zinc-700'
              }`}
            >
              <Filter className="h-4 w-4" /> Lọc danh sách ({advancedFilterCount})
            </button>
            <select
              aria-label="Số phim mỗi trang"
              value={query.size}
              onChange={event => adminMovies.commitQuery({ size: Number(event.target.value) })}
              className="rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-2.5 text-xs text-zinc-100 outline-none focus-ring"
            >
              {[5, 10, 20, 50].map(size => <option key={size} value={size}>{size}/trang</option>)}
            </select>
            <button type="button" onClick={handleOpenAdd} className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 bg-zinc-800 px-5 py-2.5 text-xs font-bold uppercase tracking-wider text-zinc-200 hover:bg-zinc-700 transition-colors">
              <Plus className="h-4 w-4" /> Thêm phim
            </button>
          </div>
        </div>

        <MovieAdvancedFilters
          query={query}
          genres={adminMovies.genresList}
          isOpen={advancedOpen}
          onApply={applyAdvancedFilters}
          onReset={adminMovies.clearAdvancedFilters}
          onRemove={removeAdvancedFilter}
        />

        {isTmdbApprovalQueue && (
          <MovieTmdbQueuePanel
            breakdown={adminMovies.queueBreakdown.data}
            isBreakdownLoading={adminMovies.queueBreakdown.isLoading}
            breakdownError={adminMovies.queueBreakdown.error}
            approval={adminMovies.bulkApproval}
            archive={adminMovies.bulkArchive}
            onApprove={handleBulkApprove}
            onArchive={handleBulkArchive}
          />
        )}

        <details className="enterprise-card group">
          <summary className="flex cursor-pointer list-none items-center justify-between gap-4 text-sm font-bold text-zinc-200">
            <span>Tình trạng nhập dữ liệu tự động</span>
            <span className="text-xs font-medium text-zinc-500 group-open:hidden">Mở thông tin kỹ thuật</span>
            <span className="hidden text-xs font-medium text-zinc-500 group-open:inline">Thu gọn</span>
          </summary>
          <div className="mt-4 border-t border-zinc-800 pt-4">
            <TmdbSyncStatusPanel />
          </div>
        </details>

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
