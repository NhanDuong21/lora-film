import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  Check,
  CirclePause,
  CirclePlay,
  Film,
  LayoutList,
  Pencil,
  Plus,
  Search,
  Tags,
  Trash2,
  X,
} from 'lucide-react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import adminGenreService from '@/features/catalog/admin/services/adminGenreService';
import SkeletonTable from '@/components/common/SkeletonTable';
import { getErrorMessage } from '@/utils/apiErrorHandler';

const getGenreId = genre => genre?.publicId || genre?.id;
const getMovieCount = genre => Number(genre?.movieCount || 0);
const isGenreActive = genre => (genre?.status || 'ACTIVE') === 'ACTIVE';

const normalizeComparableName = value => (
  value
    ?.trim()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLocaleLowerCase('vi')
    .replace(/^phim\s+/, '')
    .replace(/\s+/g, ' ')
    || ''
);

const STATUS_FILTERS = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'ACTIVE', label: 'Đang sử dụng' },
  { value: 'INACTIVE', label: 'Ngừng sử dụng' },
  { value: 'UNUSED', label: 'Chưa gắn phim' },
];

export default function AdminGenrePage() {
  const { triggerToast, triggerConfirm } = useOutletContext() || {};
  const navigate = useNavigate();
  const [genres, setGenres] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingGenre, setEditingGenre] = useState(null);
  const [formData, setFormData] = useState({ name: '' });
  const [isSaving, setIsSaving] = useState(false);
  const [mutatingGenreId, setMutatingGenreId] = useState('');

  const fetchGenres = useCallback(async () => {
    setIsLoading(true);
    setLoadError('');
    try {
      const response = await adminGenreService.getAllGenres();
      const list = response?.data?.content
        || response?.data?.data
        || response?.data
        || response?.content
        || response;
      if (response?.success === false || !Array.isArray(list)) {
        throw new Error('Danh sách thể loại chưa đúng định dạng.');
      }
      setGenres(list.filter(genre => getGenreId(genre)));
    } catch (error) {
      const message = getErrorMessage(error, 'Không tải được danh sách thể loại.');
      setLoadError(message);
      triggerToast?.(message, 'error');
    } finally {
      setIsLoading(false);
    }
  }, [triggerToast]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchGenres();
  }, [fetchGenres]);

  const stats = useMemo(() => ({
    total: genres.length,
    active: genres.filter(isGenreActive).length,
    inactive: genres.filter(genre => !isGenreActive(genre)).length,
    unused: genres.filter(genre => getMovieCount(genre) === 0).length,
  }), [genres]);

  const filteredGenres = useMemo(() => {
    const normalizedSearch = searchTerm.trim().toLocaleLowerCase('vi');
    return genres
      .filter(genre => (
        !normalizedSearch
        || genre.name?.toLocaleLowerCase('vi').includes(normalizedSearch)
      ))
      .filter(genre => {
        if (statusFilter === 'ACTIVE') return isGenreActive(genre);
        if (statusFilter === 'INACTIVE') return !isGenreActive(genre);
        if (statusFilter === 'UNUSED') return getMovieCount(genre) === 0;
        return true;
      })
      .sort((left, right) => left.name.localeCompare(right.name, 'vi'));
  }, [genres, searchTerm, statusFilter]);

  const duplicateGenre = useMemo(() => {
    const comparableName = normalizeComparableName(formData.name);
    if (!comparableName) return null;
    return genres.find(genre => (
      getGenreId(genre) !== getGenreId(editingGenre)
      && normalizeComparableName(genre.name) === comparableName
    )) || null;
  }, [editingGenre, formData.name, genres]);

  const resetModal = () => {
    setIsModalOpen(false);
    setEditingGenre(null);
    setFormData({ name: '' });
  };

  const closeModal = () => {
    if (!isSaving) resetModal();
  };

  const handleOpenAdd = () => {
    setEditingGenre(null);
    setFormData({ name: '' });
    setIsModalOpen(true);
  };

  const handleOpenEdit = genre => {
    setEditingGenre(genre);
    setFormData({ name: genre.name || '' });
    setIsModalOpen(true);
  };

  const handleSave = async event => {
    event.preventDefault();
    const name = formData.name.trim();
    if (!name) {
      triggerToast?.('Vui lòng nhập tên thể loại.', 'error');
      return;
    }
    if (duplicateGenre) {
      triggerToast?.(`Tên này trùng với thể loại “${duplicateGenre.name}”.`, 'error');
      return;
    }

    setIsSaving(true);
    try {
      if (editingGenre) {
        await adminGenreService.updateGenre(getGenreId(editingGenre), { name });
        triggerToast?.('Đã cập nhật tên thể loại.');
      } else {
        await adminGenreService.createGenre({ name });
        triggerToast?.('Đã thêm thể loại mới.');
      }
      resetModal();
      await fetchGenres();
    } catch (error) {
      triggerToast?.(getErrorMessage(error, 'Không thể lưu thể loại.'), 'error');
    } finally {
      setIsSaving(false);
    }
  };

  const handleStatusChange = async genre => {
    const id = getGenreId(genre);
    const targetStatus = isGenreActive(genre) ? 'INACTIVE' : 'ACTIVE';
    if (targetStatus === 'INACTIVE') {
      const movieCount = getMovieCount(genre);
      const shouldDeactivate = await triggerConfirm?.({
        title: `Ngừng sử dụng “${genre.name}”?`,
        message: movieCount > 0
          ? `Thể loại vẫn được giữ trên ${movieCount.toLocaleString('vi-VN')} phim đã gắn, nhưng sẽ không còn xuất hiện trong lựa chọn cho phim mới và khách hàng.`
          : 'Thể loại sẽ không còn xuất hiện trong lựa chọn cho phim mới và khách hàng. Bạn có thể khôi phục sau.',
        confirmLabel: 'Ngừng sử dụng',
        tone: 'danger',
      });
      if (!shouldDeactivate) return;
    }

    setMutatingGenreId(id);
    try {
      await adminGenreService.updateGenre(id, {
        name: genre.name,
        status: targetStatus,
      });
      triggerToast?.(
        targetStatus === 'ACTIVE'
          ? 'Đã khôi phục thể loại.'
          : 'Đã ngừng sử dụng thể loại.',
      );
      await fetchGenres();
    } catch (error) {
      triggerToast?.(getErrorMessage(error, 'Không thể cập nhật trạng thái thể loại.'), 'error');
    } finally {
      setMutatingGenreId('');
    }
  };

  const handleDelete = async genre => {
    const movieCount = getMovieCount(genre);
    if (movieCount > 0) {
      triggerToast?.(
        `Không thể xóa vì thể loại đang được ${movieCount.toLocaleString('vi-VN')} phim sử dụng.`,
        'error',
      );
      return;
    }

    const shouldDelete = await triggerConfirm?.({
      title: `Xóa thể loại “${genre.name}”?`,
      message: 'Thể loại chưa được gắn với phim nào và sẽ bị xóa khỏi danh mục.',
      confirmLabel: 'Xóa thể loại',
      tone: 'danger',
    });
    if (!shouldDelete) return;

    const id = getGenreId(genre);
    setMutatingGenreId(id);
    try {
      await adminGenreService.deleteGenre(id);
      triggerToast?.('Đã xóa thể loại.');
      await fetchGenres();
    } catch (error) {
      triggerToast?.(getErrorMessage(error, 'Không thể xóa thể loại.'), 'error');
    } finally {
      setMutatingGenreId('');
    }
  };

  const openMoviesUsingGenre = genre => {
    navigate(`/admin/movies?status=ALL&genrePublicId=${encodeURIComponent(getGenreId(genre))}`);
  };

  const filterCounts = {
    ALL: stats.total,
    ACTIVE: stats.active,
    INACTIVE: stats.inactive,
    UNUSED: stats.unused,
  };

  return (
    <div className="min-h-full overflow-auto bg-zinc-950 p-5 text-white md:p-8" data-testid="admin-genre-page">
      <div className="mx-auto max-w-[1400px] space-y-6">
        <header className="flex flex-col justify-between gap-4 border-b border-zinc-800 pb-6 sm:flex-row sm:items-end">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.18em] text-orange-400">Nội dung & phát hành</p>
            <h1 className="mt-2 text-2xl font-black text-white md:text-3xl">Danh mục thể loại</h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-400">
              Thể loại được dùng trong hồ sơ phim và bộ lọc của khách hàng. Hãy dùng tên ngắn gọn, thống nhất và tránh tạo mục trùng nghĩa.
            </p>
          </div>
          <button
            type="button"
            onClick={handleOpenAdd}
            className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-orange-500 px-5 py-3 text-sm font-black text-zinc-950 transition hover:bg-orange-400"
            data-testid="create-genre-btn"
          >
            <Plus className="h-4 w-4" />
            Thêm thể loại
          </button>
        </header>

        <section className="grid gap-3 sm:grid-cols-3" aria-label="Tổng quan danh mục thể loại">
          <div className="rounded-xl border border-zinc-800 bg-zinc-900/35 p-4">
            <p className="text-xs font-semibold text-zinc-500">Tổng thể loại</p>
            <p className="mt-2 text-2xl font-black text-white">{stats.total.toLocaleString('vi-VN')}</p>
          </div>
          <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/[0.04] p-4">
            <p className="text-xs font-semibold text-zinc-500">Đang sử dụng</p>
            <p className="mt-2 text-2xl font-black text-emerald-300">{stats.active.toLocaleString('vi-VN')}</p>
          </div>
          <div className="rounded-xl border border-amber-500/20 bg-amber-500/[0.04] p-4">
            <p className="text-xs font-semibold text-zinc-500">Chưa gắn phim</p>
            <p className="mt-2 text-2xl font-black text-amber-300">{stats.unused.toLocaleString('vi-VN')}</p>
          </div>
        </section>

        <section className="rounded-2xl border border-zinc-800 bg-zinc-900/35">
          <div className="flex flex-col gap-4 border-b border-zinc-800 p-4 lg:flex-row lg:items-end lg:justify-between md:p-5">
            <div>
              <h2 className="text-base font-bold text-white">Các thể loại đang có</h2>
              <p className="mt-1 text-xs text-zinc-500">
                Hiển thị {filteredGenres.length.toLocaleString('vi-VN')} trên {stats.total.toLocaleString('vi-VN')} thể loại
              </p>
            </div>
            <div className="relative w-full lg:max-w-xs">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" />
              <input
                type="search"
                value={searchTerm}
                onChange={event => setSearchTerm(event.target.value)}
                placeholder="Tìm theo tên thể loại…"
                aria-label="Tìm theo tên thể loại"
                className="h-11 w-full rounded-xl border border-zinc-800 bg-zinc-950 pl-10 pr-4 text-sm text-zinc-100 outline-none placeholder:text-zinc-600 focus:border-orange-500/60"
              />
            </div>
          </div>

          <div className="flex flex-wrap gap-2 border-b border-zinc-800 px-4 py-3 md:px-5" aria-label="Lọc trạng thái thể loại">
            {STATUS_FILTERS.map(filter => {
              const active = statusFilter === filter.value;
              return (
                <button
                  key={filter.value}
                  type="button"
                  aria-pressed={active}
                  onClick={() => setStatusFilter(filter.value)}
                  className={`rounded-lg border px-3 py-2 text-xs font-bold transition ${
                    active
                      ? 'border-orange-500 bg-orange-500/10 text-orange-300'
                      : 'border-zinc-800 bg-zinc-950/40 text-zinc-500 hover:border-zinc-700 hover:text-zinc-300'
                  }`}
                  data-testid={`genre-filter-${filter.value.toLowerCase()}`}
                >
                  {filter.label}
                  <span className="ml-1.5 text-[10px] opacity-70">{filterCounts[filter.value]}</span>
                </button>
              );
            })}
          </div>

          {isLoading ? (
            <div className="p-4">
              <SkeletonTable rows={6} columns={4} />
            </div>
          ) : loadError ? (
            <div className="flex flex-col items-center gap-3 p-12 text-center">
              <AlertTriangle className="h-8 w-8 text-red-400" />
              <p className="text-sm text-zinc-400">{loadError}</p>
              <button type="button" onClick={fetchGenres} className="rounded-lg border border-zinc-700 px-4 py-2 text-xs font-bold hover:bg-zinc-800">
                Thử lại
              </button>
            </div>
          ) : filteredGenres.length === 0 ? (
            <div className="flex flex-col items-center gap-3 p-14 text-center">
              <LayoutList className="h-9 w-9 text-zinc-700" />
              <p className="text-sm font-semibold text-zinc-400">
                {genres.length === 0 ? 'Chưa có thể loại nào.' : 'Không tìm thấy thể loại phù hợp.'}
              </p>
              {genres.length === 0 && (
                <button type="button" onClick={handleOpenAdd} className="text-xs font-bold text-orange-300 underline underline-offset-4">
                  Thêm thể loại đầu tiên
                </button>
              )}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[900px] text-left" data-testid="genre-table">
                <thead className="border-b border-zinc-800 bg-zinc-950/40 text-[11px] font-bold uppercase tracking-wide text-zinc-600">
                  <tr>
                    <th className="px-5 py-4">Tên thể loại</th>
                    <th className="w-52 px-5 py-4">Đang dùng cho</th>
                    <th className="w-44 px-5 py-4">Trạng thái</th>
                    <th className="w-[22rem] px-5 py-4 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-800/80">
                  {filteredGenres.map(genre => {
                    const id = getGenreId(genre);
                    const movieCount = getMovieCount(genre);
                    const active = isGenreActive(genre);
                    const isMutating = mutatingGenreId === id;
                    return (
                      <tr key={id} className={`group transition hover:bg-zinc-900/60 ${isMutating ? 'opacity-50' : ''}`}>
                        <td className="px-5 py-4">
                          <div className="flex items-center gap-3">
                            <span className={`rounded-lg p-2 ${active ? 'bg-orange-500/10 text-orange-300' : 'bg-zinc-800 text-zinc-500'}`}>
                              <Tags className="h-4 w-4" />
                            </span>
                            <span className="text-sm font-semibold text-zinc-200 group-hover:text-orange-300" data-testid={`genre-name-${id}`}>
                              {genre.name}
                            </span>
                          </div>
                        </td>
                        <td className="px-5 py-4">
                          {movieCount > 0 ? (
                            <button
                              type="button"
                              onClick={() => openMoviesUsingGenre(genre)}
                              className="inline-flex items-center gap-2 text-xs font-bold text-sky-300 underline decoration-sky-500/30 underline-offset-4 hover:text-sky-200"
                              aria-label={`Xem ${movieCount} phim đang dùng thể loại ${genre.name}`}
                            >
                              <Film className="h-3.5 w-3.5" />
                              {movieCount.toLocaleString('vi-VN')} phim
                            </button>
                          ) : (
                            <span className="text-xs text-zinc-600">Chưa gắn phim</span>
                          )}
                        </td>
                        <td className="px-5 py-4">
                          <span className={`inline-flex items-center rounded-full border px-2.5 py-1 text-[11px] font-bold ${
                            active
                              ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300'
                              : 'border-zinc-700 bg-zinc-800/70 text-zinc-400'
                          }`}>
                            {active ? 'Đang sử dụng' : 'Ngừng sử dụng'}
                          </span>
                        </td>
                        <td className="px-5 py-4">
                          <div className="flex justify-end gap-2">
                            <button
                              type="button"
                              onClick={() => handleOpenEdit(genre)}
                              disabled={isMutating}
                              className="inline-flex items-center gap-1.5 rounded-lg border border-zinc-800 px-3 py-2 text-xs font-bold text-zinc-400 transition hover:border-zinc-600 hover:text-white disabled:cursor-not-allowed"
                              data-testid={`edit-genre-${id}`}
                            >
                              <Pencil className="h-3.5 w-3.5" />
                              Sửa
                            </button>
                            <button
                              type="button"
                              onClick={() => handleStatusChange(genre)}
                              disabled={isMutating}
                              className={`inline-flex items-center gap-1.5 rounded-lg border px-3 py-2 text-xs font-bold transition disabled:cursor-not-allowed ${
                                active
                                  ? 'border-amber-500/20 text-amber-300 hover:bg-amber-500/10'
                                  : 'border-emerald-500/20 text-emerald-300 hover:bg-emerald-500/10'
                              }`}
                              data-testid={`toggle-genre-${id}`}
                            >
                              {active ? <CirclePause className="h-3.5 w-3.5" /> : <CirclePlay className="h-3.5 w-3.5" />}
                              {active ? 'Ngừng dùng' : 'Khôi phục'}
                            </button>
                            <button
                              type="button"
                              onClick={() => handleDelete(genre)}
                              disabled={isMutating || movieCount > 0}
                              aria-label={
                                movieCount > 0
                                  ? `Không thể xóa thể loại ${genre.name} vì đang được ${movieCount} phim sử dụng`
                                  : `Xóa thể loại ${genre.name}`
                              }
                              title={movieCount > 0 ? `Đang được ${movieCount.toLocaleString('vi-VN')} phim sử dụng` : 'Xóa thể loại'}
                              className="inline-flex items-center gap-1.5 rounded-lg border border-zinc-800 px-3 py-2 text-xs font-bold text-zinc-500 transition hover:border-red-500/30 hover:bg-red-500/10 hover:text-red-300 disabled:cursor-not-allowed disabled:opacity-35 disabled:hover:border-zinc-800 disabled:hover:bg-transparent disabled:hover:text-zinc-500"
                              data-testid={`delete-genre-${id}`}
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                              Xóa
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
          <section
            role="dialog"
            aria-modal="true"
            aria-labelledby="genre-form-title"
            className="w-full max-w-lg overflow-hidden rounded-2xl border border-zinc-700 bg-zinc-950 shadow-2xl"
          >
            <header className="flex items-start justify-between gap-4 border-b border-zinc-800 px-5 py-4">
              <div>
                <h2 id="genre-form-title" className="text-lg font-black text-white">
                  {editingGenre ? 'Sửa thể loại' : 'Thêm thể loại mới'}
                </h2>
                <p className="mt-1 text-xs text-zinc-500">Tên này sẽ hiển thị cho nhân viên và khách hàng.</p>
              </div>
              <button type="button" onClick={closeModal} aria-label="Đóng biểu mẫu" className="rounded-lg p-2 text-zinc-500 hover:bg-zinc-800 hover:text-white">
                <X className="h-4 w-4" />
              </button>
            </header>

            <form onSubmit={handleSave}>
              <div className="space-y-4 p-5">
                {editingGenre && getMovieCount(editingGenre) > 0 && (
                  <div className="flex gap-3 rounded-xl border border-sky-500/20 bg-sky-500/[0.05] p-3 text-xs leading-5 text-sky-100">
                    <Film className="mt-0.5 h-4 w-4 shrink-0 text-sky-300" />
                    <span>
                      Đổi tên sẽ cập nhật cách hiển thị trên{' '}
                      <strong>{getMovieCount(editingGenre).toLocaleString('vi-VN')} phim</strong> đang sử dụng thể loại này.
                    </span>
                  </div>
                )}

                <div>
                  <label htmlFor="genre-name" className="text-xs font-bold text-zinc-400">
                    Tên thể loại <span className="text-orange-400">*</span>
                  </label>
                  <input
                    id="genre-name"
                    type="text"
                    autoFocus
                    maxLength={50}
                    value={formData.name}
                    onChange={event => setFormData({ name: event.target.value })}
                    placeholder="Ví dụ: Hành động, Hoạt hình…"
                    aria-describedby="genre-name-help"
                    className={`mt-2 h-11 w-full rounded-xl border bg-zinc-900 px-4 text-sm text-zinc-100 outline-none placeholder:text-zinc-600 ${
                      duplicateGenre ? 'border-amber-500/60' : 'border-zinc-800 focus:border-orange-500/60'
                    }`}
                    required
                    data-testid="genre-name-input"
                  />
                  <p id="genre-name-help" className="mt-2 text-[11px] leading-5 text-zinc-500">
                    Dùng tên ngắn gọn và không thêm tiền tố “Phim”, ví dụ dùng “Hành động” thay vì “Phim Hành Động”.
                  </p>
                  {duplicateGenre && (
                    <p className="mt-2 flex items-start gap-2 rounded-lg bg-amber-500/10 px-3 py-2 text-xs text-amber-300" role="alert">
                      <AlertTriangle className="mt-0.5 h-3.5 w-3.5 shrink-0" />
                      Tên này có thể trùng với thể loại “{duplicateGenre.name}”. Hãy sử dụng mục đã có.
                    </p>
                  )}
                </div>
              </div>
              <footer className="flex flex-col-reverse gap-2 border-t border-zinc-800 px-5 py-4 sm:flex-row sm:justify-end">
                <button type="button" onClick={closeModal} disabled={isSaving} className="h-11 rounded-xl border border-zinc-700 px-5 text-sm font-bold text-zinc-300 hover:bg-zinc-800 disabled:opacity-50">
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isSaving || Boolean(duplicateGenre)}
                  className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-orange-500 px-5 text-sm font-black text-zinc-950 hover:bg-orange-400 disabled:cursor-not-allowed disabled:opacity-50"
                  data-testid="genre-submit-btn"
                >
                  <Check className="h-4 w-4" />
                  {isSaving ? 'Đang lưu…' : editingGenre ? 'Lưu thay đổi' : 'Thêm thể loại'}
                </button>
              </footer>
            </form>
          </section>
        </div>
      )}
    </div>
  );
}
