import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Film, PlayCircle, Image as ImageIcon, Tags, Users, Building2 } from 'lucide-react';
import useAdminMovieDetail from '@/features/catalog/admin/hooks/useAdminMovieDetail';
import { AsyncState } from '@/components/common/ui/uiKit';

import MovieOverviewTab from './movie/MovieOverviewTab';
import MovieVersionTab from './movie/MovieVersionTab';
import MovieMediaTab from './movie/MovieMediaTab';
import MovieGenreTab from './movie/MovieGenreTab';
import MovieCreditTab from './movie/MovieCreditTab';
import MovieCompanyTab from './movie/MovieCompanyTab';

export default function AdminMovieDetailPage() {
  const { moviePublicId } = useParams();
  const navigate = useNavigate();
  const { movie, isLoading, error, reload } = useAdminMovieDetail(moviePublicId);
  const [activeTab, setActiveTab] = useState('overview');

  const tabs = [
    { id: 'overview', label: 'Thông tin chung', icon: Film },
    { id: 'versions', label: 'Phiên bản', icon: PlayCircle },
    { id: 'media', label: 'Hình ảnh/Video', icon: ImageIcon },
    { id: 'genres', label: 'Thể loại', icon: Tags },
    { id: 'credits', label: 'Đội ngũ', icon: Users },
    { id: 'companies', label: 'Hãng sản xuất', icon: Building2 },
  ];

  const handleBack = () => navigate('/admin/movies');

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button
            onClick={handleBack}
            className="p-2 hover:bg-zinc-800 rounded-lg text-zinc-400 hover:text-zinc-100 transition-colors"
            aria-label="Quay lại"
          >
            <ArrowLeft size={20} />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-zinc-100 flex items-center gap-3">
              {movie?.title || 'Đang tải...'}
            </h1>
            <p className="text-sm text-zinc-500 mt-1">Quản lý chi tiết tài nguyên phim</p>
          </div>
        </div>
      </div>

      <AsyncState isLoading={isLoading} error={error} onRetry={reload}>
        <div className="bg-[#0a0a0a] border border-zinc-800 rounded-2xl overflow-hidden">
          <div className="flex overflow-x-auto border-b border-zinc-800 scrollbar-hide">
            {tabs.map(tab => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center gap-2 px-6 py-4 text-sm font-medium whitespace-nowrap transition-colors border-b-2 ${
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

          <div className="p-6">
            {activeTab === 'overview' && <MovieOverviewTab movie={movie} />}
            {activeTab === 'versions' && <MovieVersionTab movie={movie} />}
            {activeTab === 'media' && <MovieMediaTab movie={movie} />}
            {activeTab === 'genres' && <MovieGenreTab movie={movie} onUpdate={reload} />}
            {activeTab === 'credits' && <MovieCreditTab movie={movie} onUpdate={reload} />}
            {activeTab === 'companies' && <MovieCompanyTab movie={movie} onUpdate={reload} />}
          </div>
        </div>
      </AsyncState>
    </div>
  );
}
