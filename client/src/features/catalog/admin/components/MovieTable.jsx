import {
  AlertTriangle,
  Film,
  Image as ImageIcon,
  Pencil,
  RefreshCw,
  Trash2
} from 'lucide-react';
import SkeletonTable from '@/components/common/SkeletonTable';
import { EmptyState, LazyImage, StatusBadge } from '@/components/common/ui/uiKit';
import { getStatusConfig } from '@/features/catalog/admin/config/movieStatusConfig';
import { getMovieReadinessView } from '@/features/catalog/admin/utils/movieReadiness';
import { formatDate } from '@/utils/movieHelpers';

const READINESS_STATUS_CONFIG = Object.freeze({
  READY: {
    label: 'Sẵn sàng phát hành',
    className: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300'
  },
  WARNING: {
    label: 'Cần bổ sung',
    className: 'border-amber-500/30 bg-amber-500/10 text-amber-300'
  },
  BLOCKED: {
    label: 'Chưa thể phát hành',
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
  String(source || '').toUpperCase() === 'TMDB' ? 'Nhập tự động' : 'Tạo thủ công'
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
      <div className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-950/70">
        <SkeletonTable columns={5} rows={6} />
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

      <div className="overflow-x-auto">
        <table className="w-full min-w-[1120px] table-fixed">
          <thead className="border-b border-zinc-800 bg-black/30 text-left text-[11px] font-bold uppercase tracking-[0.14em] text-zinc-500">
            <tr>
              <th className="w-[29%] px-5 py-4">Phim</th>
              <th className="w-[24%] px-5 py-4">Tình trạng nội dung</th>
              <th className="w-[16%] px-5 py-4">Lịch phát hành</th>
              <th className="w-[15%] px-5 py-4">Trạng thái phục vụ</th>
              <th className="w-[16%] px-5 py-4 text-right">Hành động</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-800/80">
            {movies.map((movie) => {
              const readiness = getMovieReadinessView(movie);
              const readinessConfig = READINESS_STATUS_CONFIG[readiness.healthStatus]
                || READINESS_STATUS_CONFIG.UNKNOWN;
              const issues = readiness.issues;
              const primaryIssue = issues[0];
              const statusConfig = getStatusConfig(movie.status);
              const canDelete = movie.status === 'DRAFT';

              return (
                <tr
                  key={movie.publicId}
                  className="align-top transition-colors hover:bg-white/[0.025]"
                >
                  <td className="px-5 py-5">
                    <div className="flex min-w-0 gap-4">
                      <div className="h-28 w-20 shrink-0 overflow-hidden rounded-xl border border-zinc-700 bg-zinc-900">
                        {movie.primaryPoster ? (
                          <LazyImage
                            src={movie.primaryPoster}
                            alt={`Poster ${movie.title}`}
                            className="h-full w-full object-cover"
                          />
                        ) : (
                          <div className="flex h-full w-full flex-col items-center justify-center gap-2 text-zinc-600">
                            <ImageIcon size={22} />
                            <span className="text-[9px] font-semibold uppercase">Chưa có poster</span>
                          </div>
                        )}
                      </div>
                      <div className="min-w-0 py-1">
                        <button
                          type="button"
                          onClick={() => onOpenDetail(movie)}
                          className="line-clamp-2 text-left text-base font-extrabold text-white transition hover:text-orange-400"
                        >
                          {movie.title || 'Phim chưa đặt tên'}
                        </button>
                        <p className="mt-2 text-xs font-medium text-zinc-500">
                          {getSourceLabel(movie.source)}
                        </p>
                        <div className="mt-3 flex flex-wrap gap-2 text-[11px] text-zinc-400">
                          <span className="rounded-md bg-zinc-900 px-2 py-1">
                            {movie.ageRating || 'Chưa phân loại tuổi'}
                          </span>
                          <span className="rounded-md bg-zinc-900 px-2 py-1">
                            {movie.durationMinutes ? `${movie.durationMinutes} phút` : 'Chưa có thời lượng'}
                          </span>
                        </div>
                      </div>
                    </div>
                  </td>

                  <td className="px-5 py-5">
                    <span className={`inline-flex rounded-full border px-3 py-1 text-xs font-bold ${readinessConfig.className}`}>
                      {readinessConfig.label}
                    </span>
                    {primaryIssue ? (
                      <>
                        <p className="mt-3 line-clamp-2 text-sm font-semibold leading-5 text-zinc-200">
                          {getIssueText(primaryIssue)}
                        </p>
                        {issues.length > 1 && (
                          <p className="mt-1 text-xs text-zinc-500">
                            Còn {issues.length - 1} mục cần kiểm tra
                          </p>
                        )}
                      </>
                    ) : (
                      <p className="mt-3 text-sm leading-5 text-zinc-400">
                        Đủ dữ liệu cơ bản để đưa vào khai thác.
                      </p>
                    )}
                    <p className="mt-2 text-xs text-zinc-600">
                      {movie.activeVersionCount || 0} phiên bản · {movie.mediaCount || 0} hình ảnh/video
                    </p>
                  </td>

                  <td className="px-5 py-5">
                    <p className="font-bold text-white">
                      {movie.releaseDate ? formatDate(movie.releaseDate) : 'Chưa đặt ngày khởi chiếu'}
                    </p>
                    <p className={`mt-2 text-xs font-semibold ${(movie.showtimeCount || 0) > 0 ? 'text-emerald-400' : 'text-zinc-500'}`}>
                      {(movie.showtimeCount || 0) > 0
                        ? `${movie.showtimeCount} suất chiếu`
                        : 'Chưa có suất chiếu'}
                    </p>
                  </td>

                  <td className="px-5 py-5">
                    <StatusBadge
                      status={movie.status}
                      label={statusConfig.label}
                    />
                    <p className="mt-3 text-xs leading-5 text-zinc-500">
                      {statusConfig.description}
                    </p>
                  </td>

                  <td className="px-5 py-5">
                    <div className="flex flex-col items-stretch gap-2">
                      <button
                        type="button"
                        onClick={() => onOpenDetail(movie)}
                        className="rounded-xl bg-orange-500 px-4 py-2.5 text-sm font-extrabold text-black transition hover:bg-orange-400"
                      >
                        Xem và xử lý
                      </button>
                      <button
                        type="button"
                        onClick={() => onOpenEdit(movie)}
                        className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 px-4 py-2.5 text-sm font-bold text-zinc-200 transition hover:border-zinc-500 hover:bg-zinc-900"
                      >
                        <Pencil size={14} />
                        Chỉnh sửa
                      </button>
                      {canDelete && (
                        <button
                          type="button"
                          onClick={() => onDelete(movie.publicId, movie.title)}
                          className="inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2 text-xs font-semibold text-red-400 transition hover:bg-red-500/10"
                        >
                          <Trash2 size={13} />
                          Xóa bản nháp
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
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
