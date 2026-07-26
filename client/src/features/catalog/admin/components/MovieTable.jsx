import { AlertTriangle, Image as ImageIcon, Pencil, RefreshCw, Trash2, Film } from 'lucide-react';
import SkeletonTable from '@/components/common/SkeletonTable';
import { LazyImage, StatusBadge, EmptyState } from '@/components/common/ui/uiKit';
import { formatDate } from '@/utils/movieHelpers';
import { getStatusConfig } from '@/features/catalog/admin/config/movieStatusConfig';
import { getMovieReadinessView } from '@/features/catalog/admin/utils/movieReadiness';

const READINESS_STATUS_CONFIG = {
  READY: { label: 'Sẵn sàng', className: 'text-emerald-400' },
  WARNING: { label: 'Cần kiểm tra', className: 'text-amber-400' },
  BLOCKED: { label: 'Bị chặn', className: 'text-red-400' },
  UNKNOWN: { label: 'Chưa xác định', className: 'text-zinc-500' },
};

function Pagination({ currentPage, pageSize, totalElements, totalPages, onPageChange }) {
  if (totalElements <= 0) return null;

  const pageItems = [];
  if (totalPages <= 7) {
    for (let page = 0; page < totalPages; page += 1) pageItems.push(page);
  } else if (currentPage <= 3) {
    pageItems.push(0, 1, 2, 3, 4, 'right-ellipsis', totalPages - 1);
  } else if (currentPage >= totalPages - 4) {
    pageItems.push(0, 'left-ellipsis');
    for (let page = totalPages - 5; page < totalPages; page += 1) pageItems.push(page);
  } else {
    pageItems.push(0, 'left-ellipsis', currentPage - 1, currentPage, currentPage + 1, 'right-ellipsis', totalPages - 1);
  }

  return (
    <div className="flex flex-col items-center justify-between gap-4 border-t border-zinc-800 bg-zinc-950 p-4 sm:flex-row">
      <span className="text-xs text-zinc-400 font-medium">
        Hiển thị {currentPage * pageSize + 1}–{Math.min((currentPage + 1) * pageSize, totalElements)} / {totalElements} phim
      </span>
      <div className="flex items-center gap-1.5">
        <button
          type="button"
          disabled={currentPage === 0}
          onClick={() => onPageChange(currentPage - 1)}
          className="rounded-lg border border-zinc-800 bg-zinc-900 px-3 py-1.5 text-xs font-semibold text-zinc-300 transition-all hover:bg-zinc-800 hover:text-white disabled:cursor-not-allowed disabled:opacity-40"
        >
          Trước
        </button>
        {pageItems.map(item => typeof item === 'string' ? (
          <span key={item} className="flex h-7 w-7 items-center justify-center text-xs text-zinc-500 font-bold">…</span>
        ) : (
          <button
            type="button"
            key={item}
            onClick={() => onPageChange(item)}
            className={`flex h-7 w-7 items-center justify-center rounded-lg text-xs font-bold transition-all ${
              currentPage === item ? 'bg-brand-orange text-white border-brand-orange' : 'text-zinc-400 border border-transparent hover:border-zinc-700 hover:bg-zinc-800 hover:text-white'
            }`}
          >
            {item + 1}
          </button>
        ))}
        <button
          type="button"
          disabled={currentPage >= totalPages - 1 || totalPages === 0}
          onClick={() => onPageChange(currentPage + 1)}
          className="rounded-lg border border-zinc-800 bg-zinc-900 px-3 py-1.5 text-xs font-semibold text-zinc-300 transition-all hover:bg-zinc-800 hover:text-white disabled:cursor-not-allowed disabled:opacity-40"
        >
          Sau
        </button>
      </div>
    </div>
  );
}

export default function MovieTable({
  movies = [],
  isInitialLoading = false,
  isRefreshing = false,
  error = '',
  emptyDatabase = false,
  currentPage = 0,
  pageSize = 10,
  totalElements = 0,
  totalPages = 0,
  onRetry,
  onPageChange,
  onOpenDetail,
  onOpenEdit,
  onDelete,
}) {
  if (isInitialLoading) return <SkeletonTable rows={pageSize} columns={8} />;

  if (error && movies.length === 0) {
    return (
      <div className="flex min-h-64 flex-col items-center justify-center gap-3 rounded-2xl border border-red-900/40 bg-red-950/20 p-8 text-center text-red-300">
        <AlertTriangle className="h-8 w-8" />
        <p className="text-sm">Không thể tải danh sách phim: {error}</p>
        <button type="button" onClick={onRetry} className="inline-flex items-center gap-2 rounded-lg border border-red-800 px-3 py-2 text-xs font-bold hover:bg-red-900/30">
          <RefreshCw className="h-3.5 w-3.5" /> Thử lại
        </button>
      </div>
    );
  }

  return (
    <div className="enterprise-card p-0 overflow-hidden flex flex-col relative shrink-0">
      {isRefreshing && (
        <div className="absolute inset-x-0 top-0 z-30 flex items-center justify-center gap-2 bg-brand-orange/90 py-1 text-[10px] font-black uppercase tracking-wider text-white">
          <RefreshCw className="h-3 w-3 animate-spin" /> Đang cập nhật
        </div>
      )}
      {error && movies.length > 0 && (
        <div className="flex items-center justify-between border-b border-red-900/40 bg-red-950/20 px-4 py-2 text-xs text-red-400">
          <span>Không thể làm mới danh sách: {error}</span>
          <button type="button" onClick={onRetry} className="font-bold underline hover:text-red-300">Thử lại</button>
        </div>
      )}

      <div className="w-full overflow-auto custom-scrollbar">
        <table className="w-full whitespace-nowrap text-left border-collapse">
          <thead className="sticky top-0 z-20 bg-zinc-950">
            <tr className="border-b border-zinc-800 text-xs font-semibold tracking-wide text-zinc-400">
              <th className="w-12 px-5 py-3.5 text-center">STT</th>
              <th className="w-16 px-5 py-3.5 text-center">Poster</th>
              <th className="px-5 py-3.5">Tên phim</th>
              <th className="w-40 px-5 py-3.5">Dữ liệu</th>
              <th className="w-32 px-5 py-3.5 text-center">Thời lượng</th>
              <th className="w-36 px-5 py-3.5 text-center">Khởi chiếu</th>
              <th className="w-32 px-5 py-3.5 text-center">Trạng thái</th>
              <th className="w-28 px-5 py-3.5 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {movies.length === 0 ? (
              <tr>
                <td colSpan={8} className="py-8">
                  <EmptyState 
                    icon={Film}
                    message={emptyDatabase ? 'Chưa có phim nào trong hệ thống.' : 'Không có phim phù hợp với bộ lọc.'}
                    description="Vui lòng điều chỉnh bộ lọc hoặc thêm phim mới."
                  />
                </td>
              </tr>
            ) : movies.map((movie, index) => {
              const statusConfig = getStatusConfig(movie.status);
              const readiness = getMovieReadinessView(movie);
              const readinessConfig = READINESS_STATUS_CONFIG[readiness.healthStatus];
              const primaryIssue = readiness.healthStatus === 'BLOCKED'
                ? readiness.blockers[0]
                : readiness.warnings[0];
              const remainingIssues = Math.max(0, readiness.issues.length - (primaryIssue ? 1 : 0));

              return (
                <tr key={movie.publicId} className="h-[116px] border-b border-neutral-800/50 transition-colors hover:bg-neutral-900/50">
                  <td className="px-5 py-4 text-center text-xs font-black text-neutral-500">
                    {(currentPage * pageSize + index + 1).toString().padStart(2, '0')}
                  </td>
                  <td className="px-5 py-4">
                    <div className="mx-auto h-[52px] w-9 overflow-hidden rounded bg-neutral-800">
                      {movie.primaryPoster ? (
                        <LazyImage src={movie.primaryPoster} alt={movie.title} containerClassName="w-full h-full border-none rounded-none bg-transparent" className="object-cover" />
                      ) : (
                        <div className="flex h-full w-full items-center justify-center text-neutral-700"><ImageIcon className="h-4 w-4" /></div>
                      )}
                    </div>
                  </td>
                  <td className="max-w-[300px] px-5 py-4 align-top">
                    <div className="flex items-center gap-2">
                      <button type="button" onClick={() => onOpenDetail(movie)} className="block max-w-[220px] cursor-pointer truncate text-left text-sm font-bold text-zinc-200 transition-colors hover:text-amber-400">
                        {movie.title || <span className="italic text-zinc-500">Chưa có tên</span>}
                      </button>
                      <span className={`rounded border px-1.5 py-0.5 text-[9px] font-bold uppercase ${movie.source === 'TMDB' ? 'border-blue-500/30 bg-blue-500/20 text-blue-400' : 'border-zinc-600/50 bg-zinc-700/50 text-zinc-400'}`}>
                        {movie.source === 'TMDB' ? 'TMDB' : 'Thủ công'}
                      </span>
                    </div>
                    <div className="mt-1 flex items-center gap-2">
                      {movie.ageRating && <span className="text-[10px] font-bold uppercase text-neutral-500">[{movie.ageRating}]</span>}
                      <span className={`text-[10px] font-bold ${readinessConfig.className}`}>● {readinessConfig.label}</span>
                    </div>
                    {primaryIssue && (
                      <div className="group relative mt-1 inline-flex max-w-[250px] items-center gap-1" tabIndex={0} aria-label={readiness.issues.map(issue => issue.message || issue.code).join('. ')}>
                        <AlertTriangle className={`h-3 w-3 shrink-0 ${readiness.healthStatus === 'BLOCKED' ? 'text-red-500' : 'text-amber-500'}`} />
                        <span className={`truncate text-[10px] ${readiness.healthStatus === 'BLOCKED' ? 'text-red-400' : 'text-amber-400'}`}>
                          {primaryIssue.message || primaryIssue.code}{remainingIssues > 0 ? ` · +${remainingIssues} vấn đề khác` : ''}
                        </span>
                        <div role="tooltip" className="absolute bottom-full left-0 z-50 mb-2 hidden min-w-[260px] whitespace-normal rounded-lg border border-zinc-700 bg-zinc-900 p-3 text-[10px] text-zinc-300 shadow-xl group-hover:block group-focus-within:block">
                          <ul className="list-disc space-y-1 pl-4">
                            {readiness.issues.map((issue, issueIndex) => <li key={`${issue.code || 'issue'}-${issueIndex}`}>{issue.message || issue.code}</li>)}
                          </ul>
                        </div>
                      </div>
                    )}
                  </td>
                  <td className="px-5 py-4">
                    <div className="flex flex-col gap-1 text-[10px] text-zinc-400">
                      <div className="flex items-center justify-between rounded bg-zinc-900/60 px-2 py-0.5"><span>Phiên bản hoạt động</span><strong className="text-zinc-200">{movie.activeVersionCount || 0}</strong></div>
                      <div className="flex items-center justify-between rounded bg-zinc-900/60 px-2 py-0.5"><span>Hình ảnh/Video</span><strong className="text-zinc-200">{movie.mediaCount || 0}</strong></div>
                      <div className="flex items-center justify-between rounded bg-zinc-900/60 px-2 py-0.5"><span>Lịch chiếu</span><strong className="text-zinc-200">{movie.showtimeCount || 0}</strong></div>
                    </div>
                  </td>
                  <td className="px-5 py-4 text-center text-xs text-zinc-300">{movie.durationMinutes ? `${movie.durationMinutes} phút` : 'Chưa có'}</td>
                  <td className="px-5 py-4 text-center text-xs text-zinc-300">{formatDate(movie.releaseDate)}</td>
                  <td className="px-5 py-4 text-center">
                    <StatusBadge status={statusConfig.value || movie.status} label={statusConfig.label} />
                  </td>
                  <td className="px-5 py-4 text-right">
                    <div className="flex items-center justify-end gap-2">
                      <button type="button" onClick={() => onOpenDetail(movie)} className="rounded-lg border border-zinc-700 bg-zinc-800 px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider text-zinc-200 hover:bg-zinc-700">
                        {movie.status === 'DRAFT' ? 'Xem xét' : 'Chi tiết'}
                      </button>
                      <button type="button" onClick={() => onOpenEdit(movie)} className="rounded-lg p-1.5 text-neutral-400 hover:bg-amber-500/10 hover:text-amber-500" title="Chỉnh sửa thông tin phim"><Pencil className="h-4 w-4" /></button>
                      <button
                        type="button"
                        disabled={movie.status !== 'ENDED' && movie.status !== 'DRAFT'}
                        onClick={() => onDelete(movie.publicId, movie.title)}
                        className="rounded-lg p-1.5 text-neutral-400 hover:bg-red-500/10 hover:text-red-500 disabled:cursor-not-allowed disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-neutral-400"
                        title={movie.status === 'ENDED' || movie.status === 'DRAFT' ? 'Xóa' : 'Chỉ có thể xóa phim ở trạng thái Nháp hoặc Đã kết thúc'}
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <Pagination currentPage={currentPage} pageSize={pageSize} totalElements={totalElements} totalPages={totalPages} onPageChange={onPageChange} />
    </div>
  );
}
