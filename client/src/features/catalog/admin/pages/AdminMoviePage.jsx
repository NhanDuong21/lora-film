import { useState } from 'react';
import { Cloud, Filter, Plus, Search } from 'lucide-react';
import { useLocation, useNavigate, useOutletContext } from 'react-router-dom';
import useAdminMovies from '@/features/catalog/admin/hooks/useAdminMovies';
import useMovieSummary from '@/features/catalog/admin/hooks/useMovieSummary';
import MovieAdvancedFilters from '@/features/catalog/admin/components/MovieAdvancedFilters';
import MovieFormModal from '@/features/catalog/admin/components/MovieFormModal';
import MovieSummaryCards from '@/features/catalog/admin/components/MovieSummaryCards';
import MovieTable from '@/features/catalog/admin/components/MovieTable';
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

  return (
    <div className="flex h-full flex-col overflow-auto bg-zinc-950 text-white" data-testid="admin-movie-page">
      <div className="space-y-6 p-6 md:p-8">
        <TmdbSyncStatusPanel />

        <div className="border-b border-zinc-800 pb-4">
          <h1 className="text-xl font-black uppercase tracking-wider md:text-2xl">Quản lý phim</h1>
          <p className="mt-2 text-sm text-zinc-400">Theo dõi vòng đời, chất lượng dữ liệu và khả năng vận hành của toàn bộ kho phim.</p>
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
                ? 'border-sky-500/30 bg-sky-500/20 text-sky-300'
                : 'border-sky-500/20 bg-sky-500/5 text-sky-400 hover:bg-sky-500/10'
            }`}
          >
            <Cloud className="h-4 w-4" /> Hàng chờ TMDB
          </button>
          {ADMIN_MOVIE_STATUS_TABS.map(tab => (
            <button
              key={tab.value}
              type="button"
              onClick={() => adminMovies.commitQuery({ status: tab.value })}
              aria-pressed={query.status === tab.value}
              className={`rounded-xl border px-4 py-2 text-xs font-bold transition-all ${
                query.status === tab.value
                  ? 'border-amber-500/30 bg-amber-500/20 text-amber-400'
                  : 'border-transparent bg-zinc-900/60 text-zinc-400 hover:bg-zinc-800'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="flex flex-col items-stretch justify-between gap-3 rounded-2xl border border-zinc-800/60 bg-zinc-900/60 p-4 lg:flex-row lg:items-center">
          <div className="relative w-full lg:w-80">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500" />
            <input
              type="search"
              value={adminMovies.searchInput}
              onChange={event => adminMovies.setSearchInput(event.target.value)}
              placeholder="Tìm kiếm tên phim..."
              className="w-full rounded-xl border border-zinc-800 bg-[#050506] py-2.5 pl-9 pr-4 text-xs text-zinc-100 outline-none transition-colors placeholder:text-zinc-500 focus:border-amber-500/50"
            />
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <button
              type="button"
              onClick={() => setAdvancedOpen(open => !open)}
              aria-expanded={advancedOpen}
              className={`inline-flex items-center justify-center gap-2 rounded-xl border px-4 py-2.5 text-xs font-bold ${
                advancedOpen || advancedFilterCount > 0
                  ? 'border-amber-500/30 bg-amber-500/10 text-amber-300'
                  : 'border-zinc-700 bg-zinc-800 text-zinc-300 hover:bg-zinc-700'
              }`}
            >
              <Filter className="h-4 w-4" /> Bộ lọc ({advancedFilterCount})
            </button>
            <select
              aria-label="Số phim mỗi trang"
              value={query.size}
              onChange={event => adminMovies.commitQuery({ size: Number(event.target.value) })}
              className="rounded-xl border border-zinc-800 bg-[#050506] px-4 py-2.5 text-xs text-zinc-100 outline-none focus:border-amber-500/50"
            >
              {[5, 10, 20, 50].map(size => <option key={size} value={size}>{size}/trang</option>)}
            </select>
            <button type="button" onClick={handleOpenAdd} className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 bg-zinc-800 px-5 py-2.5 text-xs font-bold uppercase tracking-wider text-zinc-200 hover:bg-zinc-700">
              <Plus className="h-4 w-4" /> Tạo phim thủ công
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
