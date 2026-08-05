import { useMemo, useState } from 'react';
import { Check, ChevronDown, CircleHelp, Film, Image as ImageIcon, PlayCircle, Tags, Users, Building2 } from 'lucide-react';
import { useParams } from 'react-router-dom';
import useAdminMovieDetail from '@/features/catalog/admin/hooks/useAdminMovieDetail';
import { AsyncState } from '@/components/common/ui/uiKit';
import MovieOverviewTab from './movie/MovieOverviewTab';
import MovieVersionTab from './movie/MovieVersionTab';
import MovieMediaTab from './movie/MovieMediaTab';
import MovieGenreTab from './movie/MovieGenreTab';
import MovieCreditTab from './movie/MovieCreditTab';
import MovieCompanyTab from './movie/MovieCompanyTab';
import MovieDetailHeader from './movie/MovieDetailHeader';
import MovieDetailWarnings from './movie/MovieDetailWarnings';
import MovieLifecycleReviewPanel from '../components/MovieLifecycleReviewPanel';
import TmdbMovieReviewPanel from '../components/TmdbMovieReviewPanel';
import useTmdbMovieReview from '../hooks/useTmdbMovieReview';
import MovieLaunchReadinessPanel from '../components/MovieLaunchReadinessPanel';

const TABS = [
  { id: 'overview', number: '1', label: 'Thông tin phim', shortLabel: 'Thông tin', icon: Film },
  { id: 'versions', number: '2', label: 'Bản chiếu', shortLabel: 'Bản chiếu', icon: PlayCircle },
  { id: 'media', number: '3', label: 'Poster & trailer', shortLabel: 'Hình ảnh', icon: ImageIcon },
  { id: 'genres', number: '4', label: 'Thể loại', shortLabel: 'Thể loại', icon: Tags },
  { id: 'credits', number: '5', label: 'Diễn viên & ê-kíp', shortLabel: 'Ê-kíp', icon: Users },
  { id: 'companies', number: '6', label: 'Nhà sản xuất', shortLabel: 'Đơn vị', icon: Building2 },
];

const hasCredits = movie => (
  (movie?.directors?.length || 0)
  + (movie?.actors?.length || 0)
  + (movie?.writers?.length || 0)
  + (movie?.producers?.length || 0)
) > 0;

const hasCompanies = movie => (
  (movie?.productionCompanies?.length || 0)
  + (movie?.distributors?.length || 0)
  + (movie?.studios?.length || 0)
) > 0;

function getStepState(movie, id) {
  if (!movie) return 'pending';
  if (id === 'overview') {
    return movie.title && movie.durationMinutes && movie.releaseDate && movie.ageRating ? 'complete' : 'attention';
  }
  if (id === 'versions') return movie.activeVersionCount > 0 ? 'complete' : 'attention';
  if (id === 'media') return movie.primaryPoster ? 'complete' : 'attention';
  if (id === 'genres') return movie.genres?.length > 0 ? 'complete' : 'attention';
  if (id === 'credits') return hasCredits(movie) ? 'complete' : 'optional';
  if (id === 'companies') return hasCompanies(movie) ? 'complete' : 'optional';
  return 'pending';
}

function renderTabContent(activeTab, movie, reload) {
  if (activeTab === 'overview') return <MovieOverviewTab movie={movie} onUpdate={reload} />;
  if (activeTab === 'versions') return <MovieVersionTab movie={movie} onUpdate={reload} />;
  if (activeTab === 'media') return <MovieMediaTab movie={movie} onUpdate={reload} />;
  if (activeTab === 'genres') return <MovieGenreTab movie={movie} onUpdate={reload} />;
  if (activeTab === 'credits') return <MovieCreditTab movie={movie} onUpdate={reload} />;
  return <MovieCompanyTab movie={movie} onUpdate={reload} />;
}

export default function AdminMovieDetailPage() {
  const { moviePublicId } = useParams();
  const { movie, isLoading, error, reload } = useAdminMovieDetail(moviePublicId);
  const tmdbReview = useTmdbMovieReview(movie);
  const [activeTab, setActiveTab] = useState('overview');

  const stepStates = useMemo(
    () => Object.fromEntries(TABS.map(tab => [tab.id, getStepState(movie, tab.id)])),
    [movie],
  );

  return (
    <div className="min-h-full bg-zinc-950 p-4 text-white md:p-7">
      <div className="mx-auto max-w-[1440px] space-y-5">
        <MovieDetailHeader movie={movie} />

        <AsyncState isLoading={isLoading} error={error} onRetry={reload}>
          <MovieLifecycleReviewPanel
            movie={movie}
            tmdbReview={tmdbReview.review}
            onUpdate={() => Promise.all([reload(), tmdbReview.reload()])}
            onNavigateToTab={setActiveTab}
          />
          <MovieLaunchReadinessPanel movie={movie} />
          <MovieDetailWarnings movie={movie} />

          <section className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/25" aria-labelledby="movie-workspace-title">
            <div className="border-b border-zinc-800 px-5 py-5 md:px-6">
              <h2 id="movie-workspace-title" className="text-lg font-bold text-white">Hoàn thiện hồ sơ phim</h2>
              <p className="mt-1 text-sm text-zinc-500">
                Làm theo từng bước. Mục có dấu cam là phần còn cần bổ sung; hai bước cuối là thông tin khuyến nghị.
              </p>
            </div>

            <div
              className="grid grid-cols-2 border-b border-zinc-800 md:grid-cols-3 xl:grid-cols-6"
              role="tablist"
              aria-label="Các bước hoàn thiện hồ sơ phim"
            >
              {TABS.map(tab => {
                const Icon = tab.icon;
                const state = stepStates[tab.id];
                const active = activeTab === tab.id;
                return (
                  <button
                    key={tab.id}
                    type="button"
                    role="tab"
                    aria-selected={active}
                    onClick={() => setActiveTab(tab.id)}
                    className={`group relative flex min-h-20 items-center gap-3 border-b-2 px-4 py-3 text-left transition md:min-h-24 md:flex-col md:items-start md:justify-center ${
                      active
                        ? 'border-orange-500 bg-orange-500/5 text-orange-300'
                        : 'border-transparent text-zinc-400 hover:bg-zinc-900 hover:text-zinc-200'
                    }`}
                  >
                    <span className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full border text-xs font-black ${
                      state === 'complete'
                        ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300'
                        : state === 'attention'
                          ? 'border-orange-500/30 bg-orange-500/10 text-orange-300'
                          : 'border-zinc-700 bg-zinc-900 text-zinc-500'
                    }`}>
                      {state === 'complete' ? <Check className="h-4 w-4" /> : tab.number}
                    </span>
                    <span className="min-w-0">
                      <span className="flex items-center gap-1.5 text-xs font-bold md:text-sm">
                        <Icon className="hidden h-4 w-4 md:block" />
                        <span className="truncate">{tab.label}</span>
                      </span>
                      <span className={`mt-1 block text-[11px] ${
                        state === 'attention' ? 'text-orange-300/80' : state === 'complete' ? 'text-emerald-300/70' : 'text-zinc-600'
                      }`}>
                        {state === 'complete' ? 'Đã hoàn tất' : state === 'attention' ? 'Cần bổ sung' : 'Khuyến nghị'}
                      </span>
                    </span>
                  </button>
                );
              })}
            </div>

            <div className="p-5 md:p-7">
              {renderTabContent(activeTab, movie, reload)}
            </div>
          </section>

          <details className="group rounded-2xl border border-zinc-800 bg-zinc-900/20">
            <summary className="flex cursor-pointer list-none items-center justify-between gap-4 px-5 py-4 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-orange-500">
              <span className="flex min-w-0 items-start gap-3">
                <span className="rounded-lg bg-zinc-800 p-2 text-zinc-300">
                  <CircleHelp className="h-4 w-4" />
                </span>
                <span>
                  <span className="block text-sm font-semibold text-zinc-300">Đối chiếu dữ liệu nhập tự động</span>
                  <span className="mt-1 block text-xs text-zinc-600">
                    Chỉ mở khi cần kiểm tra nguồn dữ liệu; phần này không tự thay đổi hồ sơ phim.
                  </span>
                </span>
              </span>
              <ChevronDown className="h-5 w-5 shrink-0 text-zinc-600 transition group-open:rotate-180" />
            </summary>
            <div className="border-t border-zinc-800 p-5">
              <TmdbMovieReviewPanel movie={movie} {...tmdbReview} onRetry={tmdbReview.reload} />
            </div>
          </details>
        </AsyncState>
      </div>
    </div>
  );
}
