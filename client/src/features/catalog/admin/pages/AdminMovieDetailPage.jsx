import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { Film, PlayCircle, Image as ImageIcon, Tags, Users, Building2 } from 'lucide-react';
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

export default function AdminMovieDetailPage() {
  const { moviePublicId } = useParams();
  const { movie, isLoading, error, reload } = useAdminMovieDetail(moviePublicId);
  const tmdbReview = useTmdbMovieReview(movie);
  const [activeTab, setActiveTab] = useState('overview');

  const tabs = [
    { id: 'overview', label: 'Thông tin chung', icon: Film },
    { id: 'versions', label: 'Phiên bản', icon: PlayCircle },
    { id: 'media', label: 'Hình ảnh/Video', icon: ImageIcon },
    { id: 'genres', label: 'Thể loại', icon: Tags },
    { id: 'credits', label: 'Đội ngũ', icon: Users },
    { id: 'companies', label: 'Hãng sản xuất', icon: Building2 },
  ];

  return (
    <div className="p-6 space-y-6 max-w-7xl mx-auto">
      <MovieDetailHeader movie={movie} />

      <AsyncState isLoading={isLoading} error={error} onRetry={reload}>
        <TmdbMovieReviewPanel movie={movie} {...tmdbReview} onRetry={tmdbReview.reload} />
        <MovieDetailWarnings movie={movie} />
        <MovieLifecycleReviewPanel
          movie={movie}
          tmdbReview={tmdbReview.review}
          onUpdate={() => Promise.all([reload(), tmdbReview.reload()])}
        />
        
        <div className="bg-[#0a0a0a] border border-zinc-800 rounded-2xl overflow-hidden mt-6">
          <div className="flex overflow-x-auto border-b border-zinc-800 scrollbar-hide">
            {tabs.map(tab => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center gap-2 px-6 py-4 text-sm font-medium whitespace-nowrap transition-colors border-b-2 outline-none focus-visible:bg-zinc-800/50 ${
                  activeTab === tab.id
                    ? 'border-brand-orange text-brand-orange bg-brand-orange/5'
                    : 'border-transparent text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800/50'
                }`}
                role="tab"
                aria-selected={activeTab === tab.id}
              >
                <tab.icon size={16} />
                {tab.label}
              </button>
            ))}
          </div>

          <div className="p-6 overflow-x-hidden">
            {activeTab === 'overview' && <MovieOverviewTab movie={movie} onUpdate={reload} />}
            {activeTab === 'versions' && <MovieVersionTab movie={movie} onUpdate={reload} />}
            {activeTab === 'media' && <MovieMediaTab movie={movie} onUpdate={reload} />}
            {activeTab === 'genres' && <MovieGenreTab movie={movie} onUpdate={reload} />}
            {activeTab === 'credits' && <MovieCreditTab movie={movie} onUpdate={reload} />}
            {activeTab === 'companies' && <MovieCompanyTab movie={movie} onUpdate={reload} />}
          </div>
        </div>
      </AsyncState>
    </div>
  );
}
