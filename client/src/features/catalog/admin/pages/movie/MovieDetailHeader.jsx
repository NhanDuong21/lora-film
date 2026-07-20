import { ArrowLeft, Image as ImageIcon } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getStatusConfig } from '@/features/catalog/admin/config/movieStatusConfig';
import { LazyImage } from '@/components/common/ui/uiKit';
import { formatDate, AGE_RATING_LABELS } from '@/utils/movieHelpers';

export default function MovieDetailHeader({ movie }) {
  const navigate = useNavigate();
  
  if (!movie) {
    return (
      <div className="flex items-center gap-4 mb-6">
        <button
          onClick={() => navigate('/admin/movies')}
          className="p-2 hover:bg-zinc-800 rounded-lg text-zinc-400 hover:text-zinc-100 transition-colors"
          aria-label="Quay lại danh sách"
        >
          <ArrowLeft size={20} />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-zinc-100">Đang tải...</h1>
        </div>
      </div>
    );
  }

  const statusCfg = getStatusConfig(movie.status);

  return (
    <div className="flex flex-col md:flex-row md:items-start justify-between gap-4 mb-6">
      <div className="flex items-start gap-4">
        <button
          onClick={() => navigate('/admin/movies')}
          className="p-2 hover:bg-zinc-800 rounded-lg text-zinc-400 hover:text-zinc-100 transition-colors shrink-0 mt-1"
          aria-label="Quay lại danh sách"
        >
          <ArrowLeft size={20} />
        </button>
        
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="w-20 h-28 bg-zinc-800 rounded-lg overflow-hidden shrink-0 border border-zinc-700">
            {movie.primaryPoster ? (
              <LazyImage
                src={movie.primaryPoster}
                alt={movie.title || 'Poster'}
                containerClassName="w-full h-full border-none rounded-none bg-transparent"
                className="object-cover"
              />
            ) : (
              <div className="w-full h-full flex flex-col items-center justify-center text-zinc-600 p-2 text-center">
                <ImageIcon className="w-6 h-6 mb-1" />
                <span className="text-[9px] leading-tight">Chưa có poster</span>
              </div>
            )}
          </div>
          
          <div className="flex flex-col justify-center">
            <h1 className="text-2xl font-bold text-zinc-100 flex items-center gap-3">
              {movie.title || <span className="italic text-zinc-500 text-lg">Chưa có tên</span>}
              <span className={`text-xs font-black px-2.5 py-1 rounded-md border uppercase tracking-wider ${statusCfg.colorClass}`}>
                {statusCfg.label}
              </span>
            </h1>
            
            {movie.originalTitle && (
              <p className="text-sm text-zinc-400 mt-0.5">{movie.originalTitle}</p>
            )}
            
            <div className="flex flex-wrap items-center gap-3 mt-3 text-xs text-zinc-300">
              <span className="bg-zinc-800/60 px-2 py-1 rounded border border-zinc-700/50">
                <span className="text-zinc-500 mr-1">Khởi chiếu:</span>
                <span className="font-medium text-zinc-200">
                  {movie.releaseDate ? formatDate(movie.releaseDate) : <span className="italic text-zinc-500">Chưa có ngày</span>}
                </span>
              </span>
              
              <span className="bg-zinc-800/60 px-2 py-1 rounded border border-zinc-700/50">
                <span className="text-zinc-500 mr-1">Thời lượng:</span>
                <span className="font-medium text-zinc-200">
                  {movie.durationMinutes ? `${movie.durationMinutes} phút` : <span className="italic text-zinc-500">Chưa có</span>}
                </span>
              </span>
              
              <span className="bg-zinc-800/60 px-2 py-1 rounded border border-zinc-700/50">
                <span className="text-zinc-500 mr-1">Phân loại:</span>
                <span className="font-medium text-zinc-200">
                  {movie.ageRating ? (AGE_RATING_LABELS[movie.ageRating] || movie.ageRating) : <span className="italic text-zinc-500">Chưa phân loại</span>}
                </span>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
