import { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertTriangle, Check, LayoutList, Pencil, Plus, Search, Trash2, X } from 'lucide-react';
import { useOutletContext } from 'react-router-dom';
import adminGenreService from '@/features/catalog/admin/services/adminGenreService';
import SkeletonTable from '@/components/common/SkeletonTable';
import { getErrorMessage } from '@/utils/apiErrorHandler';

const getGenreId = genre => genre?.publicId || genre?.id;

export default function AdminGenrePage() {
  const { triggerToast, triggerConfirm } = useOutletContext() || {};
  const [genres, setGenres] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingGenre, setEditingGenre] = useState(null);
  const [formData, setFormData] = useState({ name: '' });
  const [isSaving, setIsSaving] = useState(false);

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

  const filteredGenres = useMemo(() => {
    const normalized = searchTerm.trim().toLocaleLowerCase('vi');
    if (!normalized) return genres;
    return genres.filter(genre => genre.name?.toLocaleLowerCase('vi').includes(normalized));
  }, [genres, searchTerm]);

  const closeModal = () => {
    if (isSaving) return;
    setIsModalOpen(false);
    setEditingGenre(null);
    setFormData({ name: '' });
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

    const duplicate = genres.some(
      genre => genre.name?.trim().toLocaleLowerCase('vi') === name.toLocaleLowerCase('vi')
        && getGenreId(genre) !== getGenreId(editingGenre),
    );
    if (duplicate) {
      triggerToast?.('Thể loại này đã tồn tại.', 'error');
      return;
    }

    setIsSaving(true);
    try {
      if (editingGenre) {
        await adminGenreService.updateGenre(getGenreId(editingGenre), { name });
        triggerToast?.('Đã cập nhật thể loại.');
      } else {
        await adminGenreService.createGenre({ name });
        triggerToast?.('Đã thêm thể loại mới.');
      }
      closeModal();
      await fetchGenres();
    } catch (error) {
      triggerToast?.(getErrorMessage(error, 'Không thể lưu thể loại.'), 'error');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async genre => {
    const shouldDelete = await triggerConfirm?.({
      title: `Xóa thể loại “${genre.name}”?`,
      message: 'Chỉ xóa thể loại khi chưa có phim nào sử dụng. Nếu đang được sử dụng, hệ thống sẽ từ chối thao tác.',
      confirmLabel: 'Xóa thể loại',
      tone: 'danger',
    });
    if (!shouldDelete) return;

    try {
      await adminGenreService.deleteGenre(getGenreId(genre));
      triggerToast?.('Đã xóa thể loại.');
      await fetchGenres();
    } catch (error) {
      triggerToast?.(getErrorMessage(error, 'Không thể xóa thể loại.'), 'error');
    }
  };

  return (
    <div className="min-h-full overflow-auto bg-zinc-950 p-5 text-white md:p-8" data-testid="admin-genre-page">
      <div className="mx-auto max-w-[1400px] space-y-6">
        <header className="flex flex-col justify-between gap-4 border-b border-zinc-800 pb-6 sm:flex-row sm:items-end">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.18em] text-orange-400">Nội dung & phát hành</p>
            <h1 className="mt-2 text-2xl font-black text-white md:text-3xl">Danh mục thể loại</h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-400">
              Quản lý các lựa chọn thể loại dùng khi gắn vào hồ sơ phim. Tên dễ hiểu sẽ giúp khách hàng tìm phim nhanh hơn.
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

        <section className="rounded-2xl border border-zinc-800 bg-zinc-900/35">
          <div className="flex flex-col gap-4 border-b border-zinc-800 p-4 sm:flex-row sm:items-center sm:justify-between md:p-5">
            <div>
              <h2 className="text-base font-bold text-white">Các thể loại đang có</h2>
              <p className="mt-1 text-xs text-zinc-500">
                {filteredGenres.length} kết quả{searchTerm ? ` cho “${searchTerm}”` : ''}
              </p>
            </div>
            <div className="relative w-full sm:max-w-xs">
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

          {isLoading ? (
            <div className="p-4">
              <SkeletonTable rows={6} columns={2} />
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
                {searchTerm ? 'Không tìm thấy thể loại phù hợp.' : 'Chưa có thể loại nào.'}
              </p>
              {!searchTerm && (
                <button type="button" onClick={handleOpenAdd} className="text-xs font-bold text-orange-300 underline underline-offset-4">
                  Thêm thể loại đầu tiên
                </button>
              )}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[560px] text-left" data-testid="genre-table">
                <thead className="border-b border-zinc-800 bg-zinc-950/40 text-[11px] font-bold uppercase tracking-wide text-zinc-600">
                  <tr>
                    <th className="w-20 px-5 py-4">#</th>
                    <th className="px-5 py-4">Tên thể loại</th>
                    <th className="w-44 px-5 py-4 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-800/80">
                  {filteredGenres.map((genre, index) => {
                    const id = getGenreId(genre);
                    return (
                      <tr key={id} className="group transition hover:bg-zinc-900/60">
                        <td className="px-5 py-4 text-xs font-bold text-zinc-600">
                          {String(index + 1).padStart(2, '0')}
                        </td>
                        <td className="px-5 py-4">
                          <span className="text-sm font-semibold text-zinc-200 group-hover:text-orange-300" data-testid={`genre-name-${id}`}>
                            {genre.name}
                          </span>
                        </td>
                        <td className="px-5 py-4">
                          <div className="flex justify-end gap-2">
                            <button
                              type="button"
                              onClick={() => handleOpenEdit(genre)}
                              className="inline-flex items-center gap-1.5 rounded-lg border border-zinc-800 px-3 py-2 text-xs font-bold text-zinc-400 transition hover:border-zinc-600 hover:text-white"
                              data-testid={`edit-genre-${id}`}
                            >
                              <Pencil className="h-3.5 w-3.5" />
                              Sửa
                            </button>
                            <button
                              type="button"
                              onClick={() => handleDelete(genre)}
                              aria-label={`Xóa thể loại ${genre.name}`}
                              className="rounded-lg border border-zinc-800 p-2 text-zinc-500 transition hover:border-red-500/30 hover:bg-red-500/10 hover:text-red-300"
                              data-testid={`delete-genre-${id}`}
                            >
                              <Trash2 className="h-3.5 w-3.5" />
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
                <p className="mt-1 text-xs text-zinc-500">Tên này sẽ xuất hiện khi nhân viên gắn thể loại cho phim.</p>
              </div>
              <button type="button" onClick={closeModal} aria-label="Đóng biểu mẫu" className="rounded-lg p-2 text-zinc-500 hover:bg-zinc-800 hover:text-white">
                <X className="h-4 w-4" />
              </button>
            </header>

            <form onSubmit={handleSave}>
              <div className="p-5">
                <label htmlFor="genre-name" className="text-xs font-bold text-zinc-400">
                  Tên thể loại <span className="text-orange-400">*</span>
                </label>
                <input
                  id="genre-name"
                  type="text"
                  autoFocus
                  value={formData.name}
                  onChange={event => setFormData({ name: event.target.value })}
                  placeholder="Ví dụ: Hành động, Hoạt hình…"
                  className="mt-2 h-11 w-full rounded-xl border border-zinc-800 bg-zinc-900 px-4 text-sm text-zinc-100 outline-none placeholder:text-zinc-600 focus:border-orange-500/60"
                  required
                  data-testid="genre-name-input"
                />
              </div>
              <footer className="flex flex-col-reverse gap-2 border-t border-zinc-800 px-5 py-4 sm:flex-row sm:justify-end">
                <button type="button" onClick={closeModal} disabled={isSaving} className="h-11 rounded-xl border border-zinc-700 px-5 text-sm font-bold text-zinc-300 hover:bg-zinc-800 disabled:opacity-50">
                  Hủy
                </button>
                <button type="submit" disabled={isSaving} className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-orange-500 px-5 text-sm font-black text-zinc-950 hover:bg-orange-400 disabled:opacity-50" data-testid="genre-submit-btn">
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
