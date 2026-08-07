import { ArrowLeft, CalendarDays, Clock3, Image as ImageIcon, Star } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import { getStatusConfig } from '@/features/catalog/admin/config/movieStatusConfig';
import { LazyImage } from '@/components/common/ui/uiKit';
import { formatDate, AGE_RATING_LABELS } from '@/utils/movieHelpers';

function MetaItem({ icon: Icon, label, value, emptyText }) {
  return (
    <div className="flex min-w-0 items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-950/70 px-3 py-2">
      <Icon className="h-4 w-4 shrink-0 text-zinc-500" />
      <span className="text-xs text-zinc-500">{label}</span>
      <strong className={`truncate text-xs ${value ? 'text-zinc-200' : 'font-medium text-zinc-600'}`}>
        {value || emptyText}
      </strong>
    </div>
  );
}

export default function MovieDetailHeader({ movie }) {
  const navigate = useNavigate();
  const location = useLocation();
  const returnToList = () => navigate(`/admin/movies${location.search}`);

  if (!movie) {
    return (
      <div className="rounded-2xl border border-zinc-800 bg-zinc-900/30 p-5">
        <button
          type="button"
          onClick={returnToList}
          className="inline-flex items-center gap-2 text-sm font-semibold text-zinc-400 hover:text-white"
        >
          <ArrowLeft className="h-4 w-4" />
          Quay lại danh sách phim
        </button>
        <div className="mt-5 h-24 animate-pulse rounded-xl bg-zinc-900" />
      </div>
    );
  }

  const status = getStatusConfig(movie.status);
  const sourceLabel = movie.source === 'TMDB' ? 'Nhập tự động' : 'Tạo thủ công';

  return (
    <header className="rounded-2xl border border-zinc-800 bg-gradient-to-br from-zinc-900/80 to-zinc-950 p-5 md:p-6">
      <button
        type="button"
        onClick={returnToList}
        className="inline-flex items-center gap-2 rounded-lg text-sm font-semibold text-zinc-400 transition hover:text-white"
      >
        <ArrowLeft className="h-4 w-4" />
        Quay lại danh sách phim
      </button>

      <div className="mt-5 flex flex-col gap-5 sm:flex-row">
        <div className="h-32 w-24 shrink-0 overflow-hidden rounded-xl border border-zinc-700 bg-zinc-900">
          {movie.primaryPoster ? (
            <LazyImage
              src={movie.primaryPoster}
              alt={`Poster ${movie.title || 'phim'}`}
              containerClassName="h-full w-full"
              className="h-full w-full object-cover"
            />
          ) : (
            <div className="flex h-full w-full flex-col items-center justify-center gap-2 px-2 text-center text-zinc-600">
              <ImageIcon className="h-6 w-6" />
              <span className="text-[10px]">Chưa có poster</span>
            </div>
          )}
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="rounded-md border border-zinc-700 bg-zinc-800 px-2 py-1 text-[10px] font-bold uppercase tracking-wide text-zinc-400">
              {sourceLabel}
            </span>
            <span className={`rounded-md border px-2 py-1 text-[10px] font-black uppercase tracking-wide ${status.colorClass}`}>
              {status.label}
            </span>
          </div>

          <h1 className="mt-3 break-words text-2xl font-black text-white md:text-3xl">
            {movie.title || 'Phim chưa đặt tên'}
          </h1>
          {movie.originalTitle && movie.originalTitle !== movie.title && (
            <p className="mt-1 text-sm text-zinc-500">{movie.originalTitle}</p>
          )}

          <div className="mt-4 grid gap-2 sm:grid-cols-3">
            <MetaItem
              icon={CalendarDays}
              label="Bắt đầu khai thác"
              value={movie.releaseDate ? formatDate(movie.releaseDate) : ''}
              emptyText="Chưa có ngày"
            />
            <MetaItem
              icon={Clock3}
              label="Thời lượng"
              value={movie.durationMinutes ? `${movie.durationMinutes} phút` : ''}
              emptyText="Chưa có"
            />
            <MetaItem
              icon={Star}
              label="Độ tuổi"
              value={movie.ageRating ? (AGE_RATING_LABELS[movie.ageRating] || movie.ageRating) : ''}
              emptyText="Chưa phân loại"
            />
          </div>
        </div>
      </div>
    </header>
  );
}
