import { AGE_RATING_LABELS, STATUS_LABELS } from '@/utils/movieHelpers';

export default function MovieOverviewTab({ movie }) {
  if (!movie) return null;

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label className="block text-xs font-medium text-zinc-400 mb-1">Tên phim</label>
          <div className="text-sm text-zinc-100">{movie.title || '-'}</div>
        </div>
        <div>
          <label className="block text-xs font-medium text-zinc-400 mb-1">Tên gốc</label>
          <div className="text-sm text-zinc-100">{movie.originalTitle || '-'}</div>
        </div>
        <div>
          <label className="block text-xs font-medium text-zinc-400 mb-1">Đường dẫn (Slug)</label>
          <div className="text-sm text-zinc-100">{movie.slug || movie.activeSlug || '-'}</div>
        </div>
        <div>
          <label className="block text-xs font-medium text-zinc-400 mb-1">Quốc gia</label>
          <div className="text-sm text-zinc-100">{movie.country || '-'}</div>
        </div>
        <div>
          <label className="block text-xs font-medium text-zinc-400 mb-1">Ngày khởi chiếu</label>
          <div className="text-sm text-zinc-100">{movie.releaseDate || '-'}</div>
        </div>
        <div>
          <label className="block text-xs font-medium text-zinc-400 mb-1">Ngày kết thúc</label>
          <div className="text-sm text-zinc-100">{movie.endDate || '-'}</div>
        </div>
        <div>
          <label className="block text-xs font-medium text-zinc-400 mb-1">Thời lượng</label>
          <div className="text-sm text-zinc-100">{movie.durationMinutes ? `${movie.durationMinutes} phút` : '-'}</div>
        </div>
        <div>
          <label className="block text-xs font-medium text-zinc-400 mb-1">Độ tuổi</label>
          <div className="text-sm text-zinc-100">{AGE_RATING_LABELS[movie.ageRating] || movie.ageRating || '-'}</div>
        </div>
        <div>
          <label className="block text-xs font-medium text-zinc-400 mb-1">Trạng thái</label>
          <div className="text-sm text-zinc-100">{STATUS_LABELS[movie.status] || movie.status || '-'}</div>
        </div>
      </div>
      <div>
        <label className="block text-xs font-medium text-zinc-400 mb-1">Tóm tắt</label>
        <div className="text-sm text-zinc-100 whitespace-pre-wrap leading-relaxed">{movie.synopsis || '-'}</div>
      </div>
    </div>
  );
}
