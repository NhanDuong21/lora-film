import { useState } from 'react';
import { Pencil } from 'lucide-react';
import { useOutletContext } from 'react-router-dom';
import { getStatusConfig } from '@/features/catalog/admin/config/movieStatusConfig';
import { formatDate, AGE_RATING_LABELS } from '@/utils/movieHelpers';
import MovieFormModal from '../../components/MovieFormModal';

export default function MovieOverviewTab({ movie, onUpdate }) {
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const { triggerToast } = useOutletContext() || {};

  if (!movie) return null;

  const statusCfg = getStatusConfig(movie.status);

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-zinc-100">Thông tin chung</h2>
        <button
          onClick={() => setIsEditModalOpen(true)}
          className="flex items-center gap-2 px-4 py-2 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 text-sm font-semibold rounded-lg transition-colors"
        >
          <Pencil size={16} /> Chỉnh sửa metadata
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Thông tin nhận diện */}
        <div className="space-y-4 bg-zinc-900/50 p-6 rounded-2xl border border-zinc-800/50">
          <h3 className="text-sm font-black uppercase tracking-wider text-amber-500 mb-4">Thông tin nhận diện</h3>
          
          <div className="space-y-4">
            {movie.title && (
              <div>
                <label className="block text-xs font-medium text-zinc-500 mb-1">Tên phim</label>
                <div className="text-sm text-zinc-100">{movie.title}</div>
              </div>
            )}
            
            {movie.originalTitle && (
              <div>
                <label className="block text-xs font-medium text-zinc-500 mb-1">Tên gốc</label>
                <div className="text-sm text-zinc-100">{movie.originalTitle}</div>
              </div>
            )}
            
            {(movie.slug || movie.activeSlug) && (
              <div>
                <label className="block text-xs font-medium text-zinc-500 mb-1">Đường dẫn (Slug)</label>
                <div className="text-sm text-zinc-100 font-mono">{movie.slug || movie.activeSlug}</div>
              </div>
            )}
            
            {movie.country && (
              <div>
                <label className="block text-xs font-medium text-zinc-500 mb-1">Quốc gia</label>
                <div className="text-sm text-zinc-100">{movie.country}</div>
              </div>
            )}
          </div>
        </div>

        {/* Thông tin phát hành */}
        <div className="space-y-4 bg-zinc-900/50 p-6 rounded-2xl border border-zinc-800/50">
          <h3 className="text-sm font-black uppercase tracking-wider text-amber-500 mb-4">Thông tin phát hành</h3>
          
          <div className="grid grid-cols-2 gap-4">
            {movie.releaseDate && (
              <div>
                <label className="block text-xs font-medium text-zinc-500 mb-1">Ngày khởi chiếu</label>
                <div className="text-sm text-zinc-100">{formatDate(movie.releaseDate)}</div>
              </div>
            )}
            
            {movie.endDate && (
              <div>
                <label className="block text-xs font-medium text-zinc-500 mb-1">Ngày kết thúc</label>
                <div className="text-sm text-zinc-100">{formatDate(movie.endDate)}</div>
              </div>
            )}
            
            {movie.durationMinutes && (
              <div>
                <label className="block text-xs font-medium text-zinc-500 mb-1">Thời lượng</label>
                <div className="text-sm text-zinc-100">{movie.durationMinutes} phút</div>
              </div>
            )}
            
            {movie.ageRating && (
              <div>
                <label className="block text-xs font-medium text-zinc-500 mb-1">Phân loại độ tuổi</label>
                <div className="text-sm text-zinc-100 font-bold">{AGE_RATING_LABELS[movie.ageRating] || movie.ageRating}</div>
              </div>
            )}
            
            <div className="col-span-2">
              <label className="block text-xs font-medium text-zinc-500 mb-1">Trạng thái (Read-only)</label>
              <div className="text-sm">
                <span className={`text-[10px] font-black px-2 py-0.5 rounded border uppercase tracking-wider ${statusCfg.colorClass}`}>
                  {statusCfg.label}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Nội dung */}
      <div className="bg-zinc-900/50 p-6 rounded-2xl border border-zinc-800/50">
        <h3 className="text-sm font-black uppercase tracking-wider text-amber-500 mb-4">Nội dung (Synopsis)</h3>
        {movie.synopsis ? (
          <div className="text-sm text-zinc-300 whitespace-pre-wrap leading-relaxed">
            {movie.synopsis}
          </div>
        ) : (
          <p className="text-sm italic text-zinc-500">Chưa có nội dung tóm tắt.</p>
        )}
      </div>

      {isEditModalOpen && (
        <MovieFormModal
          selectedMovie={movie}
          triggerToast={triggerToast}
          onClose={() => setIsEditModalOpen(false)}
          onRefreshList={() => {
            setIsEditModalOpen(false);
            if (onUpdate) onUpdate();
          }}
        />
      )}
    </div>
  );
}
