import { useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  Film,
  PlayCircle,
  Image as ImageIcon,
  Tags,
  Users,
  Building2,
  ChevronDown,
  CircleHelp,
} from 'lucide-react';
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
    { id: 'overview', label: 'Nội dung chính', icon: Film },
    { id: 'versions', label: 'Định dạng chiếu', icon: PlayCircle },
    { id: 'media', label: 'Ảnh & video', icon: ImageIcon },
    { id: 'genres', label: 'Phân loại', icon: Tags },
    { id: 'credits', label: 'Diễn viên & ê-kíp', icon: Users },
    { id: 'companies', label: 'Đơn vị sản xuất', icon: Building2 },
  ];

  return (
    <div className="p-6 space-y-6 max-w-7xl mx-auto">
      <MovieDetailHeader movie={movie} />

      <AsyncState isLoading={isLoading} error={error} onRetry={reload}>
        <MovieLifecycleReviewPanel
          movie={movie}
          tmdbReview={tmdbReview.review}
          onUpdate={() => Promise.all([reload(), tmdbReview.reload()])}
        />
        <MovieDetailWarnings movie={movie} />

        <div className="bg-[#0a0a0a] border border-zinc-800 rounded-2xl overflow-hidden mt-6">
          <div className="flex flex-col gap-1 px-6 pt-6 pb-4 border-b border-zinc-800">
            <h2 className="text-lg font-bold text-white">Hồ sơ nội dung phim</h2>
            <p className="text-sm text-zinc-400">
              Hoàn thiện từng nhóm thông tin bên dưới trước khi đưa phim vào phục vụ.
            </p>
          </div>
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

        <details className="group bg-[#0a0a0a] border border-zinc-800 rounded-2xl overflow-hidden">
          <summary className="flex cursor-pointer list-none items-center justify-between gap-4 px-6 py-5 hover:bg-zinc-900/60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange">
            <span className="flex min-w-0 items-start gap-3">
              <span className="mt-0.5 rounded-lg bg-zinc-800 p-2 text-zinc-300">
                <CircleHelp size={18} />
              </span>
              <span>
                <span className="block font-semibold text-white">Đối chiếu nguồn nhập (nâng cao)</span>
                <span className="mt-1 block text-sm text-zinc-400">
                  Chỉ mở khi cần kiểm tra dữ liệu đồng bộ; nội dung tại đây không tự thay đổi phim.
                </span>
              </span>
            </span>
            <ChevronDown
              size={20}
              className="shrink-0 text-zinc-400 transition-transform group-open:rotate-180"
              aria-hidden="true"
            />
          </summary>
          <div className="border-t border-zinc-800 p-5">
            <TmdbMovieReviewPanel movie={movie} {...tmdbReview} onRetry={tmdbReview.reload} />
          </div>
        </details>
      </AsyncState>
    </div>
  );
}
