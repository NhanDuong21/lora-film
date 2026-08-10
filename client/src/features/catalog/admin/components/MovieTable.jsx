import {
  AlertTriangle,
  CalendarDays,
  Film,
  Image as ImageIcon,
  Pencil,
  RefreshCw,
  Trash2
} from 'lucide-react';
import { EmptyState, LazyImage, StatusBadge } from '@/components/common/ui/uiKit';
import { getStatusConfig } from '@/features/catalog/admin/config/movieStatusConfig';
import { getMovieReadinessView } from '@/features/catalog/admin/utils/movieReadiness';
import { formatDate } from '@/utils/movieHelpers';

const READINESS_STATUS_CONFIG = Object.freeze({
  READY: {
    label: 'Đã đủ thông tin',
    className: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300'
  },
  WARNING: {
    label: 'Nên kiểm tra',
    className: 'border-amber-500/30 bg-amber-500/10 text-amber-300'
  },
  BLOCKED: {
    label: 'Thiếu thông tin bắt buộc',
    className: 'border-red-500/30 bg-red-500/10 text-red-300'
  },
  UNKNOWN: {
    label: 'Chưa kiểm tra',
    className: 'border-zinc-700 bg-zinc-900 text-zinc-400'
  }
});

const getIssueText = (issue) => (
  issue?.message || issue?.code || 'Chưa có thông tin kiểm tra'
);

const getSourceLabel = (source) => (
  String(source || '').toUpperCase() === 'TMDB' ? 'Hệ thống nhập tự động' : 'Nhân viên tạo thủ công'
);

function Pagination({
  currentPage,
  pageSize,
  totalElements,
  totalPages,
  onPageChange
}) {
  if (!totalElements || totalPages <= 1) {
    return null;
  }

  const firstItem = currentPage * pageSize + 1;
  const lastItem = Math.min((currentPage + 1) * pageSize, totalElements);

  return (
    <div className="flex flex-col gap-3 border-t border-zinc-800 px-5 py-4 text-sm text-zinc-400 sm:flex-row sm:items-center sm:justify-between">
      <span>
        Đang hiển thị <strong className="text-white">{firstItem}–{lastItem}</strong>
        {' '}trong tổng số <strong className="text-white">{totalElements}</strong> phim
      </span>
      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage <= 0}
          className="rounded-xl border border-zinc-700 px-4 py-2 font-semibold text-zinc-200 transition hover:border-zinc-500 disabled:cursor-not-allowed disabled:opacity-40"
        >
          Trang trước
        </button>
        <span className="min-w-24 text-center text-xs font-semibold uppercase tracking-wide text-zinc-500">
          Trang {currentPage + 1}/{totalPages}
        </span>
        <button
          type="button"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage >= totalPages - 1}
          className="rounded-xl border border-zinc-700 px-4 py-2 font-semibold text-zinc-200 transition hover:border-zinc-500 disabled:cursor-not-allowed disabled:opacity-40"
        >
          Trang sau
        </button>
      </div>
    </div>
  );
}

function MoviePosterCard({ movie, onOpenDetail, onOpenEdit, onDelete }) {
  const readiness = getMovieReadinessView(movie);
  const readinessConfig = READINESS_STATUS_CONFIG[readiness.healthStatus]
    || READINESS_STATUS_CONFIG.UNKNOWN;
  const issues = readiness.issues;
  const primaryIssue = issues[0];
  const statusConfig = getStatusConfig(movie.status);
  const canDelete = movie.status === 'DRAFT';
  const title = movie.title || 'Phim chưa đặt tên';

  return (
    <article
      data-testid="movie-poster-card"
      className="group flex h-full min-w-0 flex-col overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/55 shadow-lg shadow-black/10 transition duration-200 hover:-translate-y-1 hover:border-zinc-600 hover:shadow-2xl hover:shadow-black/30"
    >
      <button
        type="button"
        onClick={() => onOpenDetail(movie)}
        aria-label={`Mở hồ sơ ${title}`}
        className="relative block aspect-[2/3] w-full overflow-hidden bg-zinc-900 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-orange-500"
      >
        {movie.primaryPoster ? (
          <LazyImage
            src={movie.primaryPoster}
            alt={`Áp phích ${title}`}
            className="h-full w-full object-cover transition duration-300 group-hover:scale-[1.03]"
          />
        ) : (
          <span className="flex h-full w-full flex-col items-center justify-center gap-3 bg-gradient-to-b from-zinc-900 to-zinc-950 text-zinc-600">
            <ImageIcon size={34} />
            <span className="text-[10px] font-bold uppercase tracking-wider">Chưa có áp phích</span>
          </span>
        )}

        <span className="pointer-events-none absolute inset-x-0 bottom-0 h-28 bg-gradient-to-t from-black via-black/70 to-transparent" />
        <span className="absolute left-3 top-3 rounded-full border border-white/10 bg-black/75 px-2.5 py-1 text-[10px] font-bold text-zinc-200 backdrop-blur-sm">
          {getSourceLabel(movie.source)}
        </span>
        <span className="absolute bottom-3 left-3 right-3 flex items-end justify-between gap-2">
          <span className="line-clamp-2 text-base font-black leading-5 text-white drop-shadow-lg">
            {title}
          </span>
          <span className="shrink-0 rounded-lg border border-white/10 bg-black/75 px-2 py-1 text-[10px] font-bold text-zinc-200 backdrop-blur-sm">
            {movie.ageRating || 'Chưa phân loại'}
          </span>
        </span>
      </button>

      <div className="flex flex-1 flex-col p-4">
        <div className="flex items-start justify-between gap-2">
          <StatusBadge status={movie.status} label={statusConfig.label} />
          <span className="rounded-lg bg-zinc-950 px-2 py-1 text-[10px] font-semibold text-zinc-400">
            {movie.durationMinutes ? `${movie.durationMinutes} phút` : 'Chưa có thời lượng'}
          </span>
        </div>

        <div className="mt-4 min-h-[94px] rounded-xl border border-zinc-800 bg-black/20 p-3">
          <span className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-bold ${readinessConfig.className}`}>
            {readinessConfig.label}
          </span>
          <p className="mt-2 line-clamp-2 text-xs font-semibold leading-5 text-zinc-300">
            {primaryIssue ? getIssueText(primaryIssue) : 'Đủ dữ liệu cơ bản để đưa vào khai thác.'}
          </p>
          <p className="mt-1 text-[10px] text-zinc-600">
            {primaryIssue && issues.length > 1
              ? `Còn ${issues.length - 1} mục cần kiểm tra · `
              : ''}
            {movie.activeVersionCount || 0} phiên bản · {movie.mediaCount || 0} hình ảnh/video
          </p>
        </div>

        <div className="mt-3 grid grid-cols-2 divide-x divide-zinc-800 rounded-xl border border-zinc-800 bg-zinc-950/60 py-3">
          <div className="min-w-0 px-3">
            <p className="flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wide text-zinc-600">
              <CalendarDays className="h-3 w-3" />
              Khai thác
            </p>
            <p className="mt-1 truncate text-xs font-bold text-zinc-200">
              {movie.releaseDate ? formatDate(movie.releaseDate) : 'Chưa đặt ngày'}
            </p>
          </div>
          <div className="min-w-0 px-3">
            <p className="text-[10px] font-bold uppercase tracking-wide text-zinc-600">Suất chiếu</p>
            <p className={`mt-1 truncate text-xs font-bold ${(movie.showtimeCount || 0) > 0 ? 'text-emerald-300' : 'text-zinc-500'}`}>
              {(movie.showtimeCount || 0) > 0 ? `${movie.showtimeCount} suất` : 'Chưa có'}
            </p>
          </div>
        </div>

        <div className="mt-auto grid grid-cols-[1fr_auto] gap-2 pt-4">
          <button
            type="button"
            onClick={() => onOpenDetail(movie)}
            className="rounded-xl bg-orange-500 px-3 py-2.5 text-xs font-black text-black transition hover:bg-orange-400"
          >
            Mở hồ sơ
          </button>
          <button
            type="button"
            onClick={() => onOpenEdit(movie)}
            className="inline-flex items-center justify-center gap-1.5 rounded-xl border border-zinc-700 px-3 py-2.5 text-xs font-bold text-zinc-200 transition hover:border-zinc-500 hover:bg-zinc-800"
          >
            <Pencil size={13} />
            Sửa nhanh
          </button>
          {canDelete && (
            <button
              type="button"
              onClick={() => onDelete(movie.publicId, movie.title)}
              className="col-span-2 inline-flex items-center justify-center gap-1.5 rounded-xl py-2 text-[11px] font-semibold text-red-400 transition hover:bg-red-500/10"
            >
              <Trash2 size={12} />
              Xóa bản nháp
            </button>
          )}
        </div>
      </div>
    </article>
  );
}

function MovieTable({
  movies,
  isInitialLoading,
  isRefreshing,
  error,
  emptyDatabase,
  currentPage,
  pageSize,
  totalElements,
  totalPages,
  onRetry,
  onPageChange,
  onOpenDetail,
  onOpenEdit,
  onDelete
}) {
  if (isInitialLoading) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5" aria-label="Đang tải danh sách phim">
        {Array.from({ length: 10 }, (_, index) => (
          <div key={index} className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/55">
            <div className="aspect-[2/3] animate-pulse bg-zinc-800" />
            <div className="space-y-3 p-4">
              <div className="h-5 w-2/3 animate-pulse rounded bg-zinc-800" />
              <div className="h-20 animate-pulse rounded-xl bg-zinc-800/70" />
              <div className="h-11 animate-pulse rounded-xl bg-zinc-800/70" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (error && movies.length === 0) {
    return (
      <div className="rounded-2xl border border-red-500/20 bg-red-500/5 p-8">
        <EmptyState
          icon={AlertTriangle}
          title="Chưa tải được danh sách phim"
          description="Kết nối đến kho phim đang gặp sự cố. Dữ liệu hiện có không bị thay đổi."
          actionLabel="Thử tải lại"
          onAction={onRetry}
        />
      </div>
    );
  }

  if (movies.length === 0) {
    return (
      <div className="rounded-2xl border border-zinc-800 bg-zinc-950/70 p-8">
        <EmptyState
          icon={Film}
          title={emptyDatabase ? 'Kho phim chưa có nội dung' : 'Không tìm thấy phim phù hợp'}
          description={
            emptyDatabase
              ? 'Hãy thêm phim mới để bắt đầu chuẩn bị nội dung phát hành.'
              : 'Hãy thử đổi từ khóa, trạng thái hoặc xóa bớt bộ lọc.'
          }
        />
      </div>
    );
  }

  return (
    <section className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-950/70">
      {isRefreshing && (
        <div className="flex items-center gap-2 border-b border-sky-500/20 bg-sky-500/10 px-5 py-3 text-sm text-sky-200">
          <RefreshCw size={15} className="animate-spin" />
          Đang cập nhật danh sách phim mới nhất…
        </div>
      )}

      {error && (
        <div className="flex flex-col gap-3 border-b border-amber-500/20 bg-amber-500/10 px-5 py-3 text-sm text-amber-100 sm:flex-row sm:items-center sm:justify-between">
          <span>Chưa thể làm mới dữ liệu. Danh sách gần nhất vẫn đang được hiển thị.</span>
          <button
            type="button"
            onClick={onRetry}
            className="inline-flex items-center gap-2 self-start rounded-lg border border-amber-500/30 px-3 py-2 font-semibold hover:bg-amber-500/10 sm:self-auto"
          >
            <RefreshCw size={14} />
            Thử lại
          </button>
        </div>
      )}

      <div className="grid gap-4 p-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 md:p-5">
        {movies.map(movie => (
          <MoviePosterCard
            key={movie.publicId}
            movie={movie}
            onOpenDetail={onOpenDetail}
            onOpenEdit={onOpenEdit}
            onDelete={onDelete}
          />
        ))}
      </div>

      <Pagination
        currentPage={currentPage}
        pageSize={pageSize}
        totalElements={totalElements}
        totalPages={totalPages}
        onPageChange={onPageChange}
      />
    </section>
  );
}

export default MovieTable;
