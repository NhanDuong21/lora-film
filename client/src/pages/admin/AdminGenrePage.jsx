import { useState, useEffect, useCallback, useMemo } from 'react';
import { Search, Pencil, Trash2, X, Plus, Check, LayoutList } from 'lucide-react';
import adminGenreService from '../../services/adminGenreService';
import SkeletonTable from '../../components/common/SkeletonTable';

export default function AdminGenrePage({ triggerToast }) {
  const [genres, setGenres] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingGenre, setEditingGenre] = useState(null);
  const [formData, setFormData] = useState({ genreName: '' });

  // Fetch Genres
  const fetchGenres = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await adminGenreService.getAllGenres();
      let genreList = [];
      if (Array.isArray(data?.data)) {
        genreList = data.data;
      } else if (data?.data?.content && Array.isArray(data.data.content)) {
        genreList = data.data.content;
      } else if (Array.isArray(data)) {
        genreList = data;
      } else if (data?.content && Array.isArray(data.content)) {
        genreList = data.content;
      }
      setGenres(genreList);
    } catch (error) {
      if (triggerToast) triggerToast(error.response?.data?.message || 'Lỗi khi tải danh sách thể loại', 'error');
    } finally {
      setIsLoading(false);
    }
  }, [triggerToast]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchGenres();
  }, [fetchGenres]);

  const filteredGenres = useMemo(() => {
    return genres.filter(g => 
      (g.genreName || '').toLowerCase().includes(searchTerm.toLowerCase())
    );
  }, [genres, searchTerm]);

  const handleOpenAdd = useCallback(() => {
    setEditingGenre(null);
    setFormData({ genreName: '' });
    setIsModalOpen(true);
  }, []);

  const handleOpenEdit = useCallback((genre) => {
    setEditingGenre(genre);
    setFormData({ genreName: genre.genreName || '' });
    setIsModalOpen(true);
  }, []);

  const handleSave = useCallback(async (e) => {
    e.preventDefault();
    if (!formData.genreName.trim()) {
      if (triggerToast) triggerToast('Tên thể loại không được để trống!', 'error');
      return;
    }
    try {
      if (editingGenre) {
        await adminGenreService.updateGenre(editingGenre.id, formData);
        if (triggerToast) triggerToast('Cập nhật thể loại thành công!');
      } else {
        await adminGenreService.createGenre(formData);
        if (triggerToast) triggerToast('Thêm thể loại mới thành công!');
      }
      setIsModalOpen(false);
      fetchGenres();
    } catch (error) {
      if (triggerToast) triggerToast(error.response?.data?.message || 'Có lỗi xảy ra', 'error');
    }
  }, [editingGenre, formData, triggerToast, fetchGenres]);

  const handleDelete = useCallback(async (id) => {
    if (confirm('Bạn có chắc chắn muốn xóa thể loại này?')) {
      try {
        await adminGenreService.deleteGenre(id);
        if (triggerToast) triggerToast('Đã xóa thể loại khỏi danh sách!');
        fetchGenres();
      } catch (error) {
        if (triggerToast) triggerToast(error.response?.data?.message || 'Có lỗi xảy ra khi xóa', 'error');
      }
    }
  }, [triggerToast, fetchGenres]);

  // Modal UI matches prototype forms
  if (isModalOpen) {
    return (
      <div className="w-full bg-brand-dark p-6 flex flex-col gap-6 animate-fade-in text-zinc-100">
        <div className="flex justify-between items-center border-b border-zinc-800/80 pb-4">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setIsModalOpen(false)}
              className="p-2 text-zinc-400 hover:text-white bg-brand-gray border border-zinc-800/80 rounded-xl transition-all cursor-pointer"
              data-testid="genre-modal-close"
            >
              <X className="w-4 h-4" />
            </button>
            <h2 className="text-lg font-black text-zinc-100 uppercase tracking-wider">
              {editingGenre ? 'Cập Nhật Thể Loại' : 'Thêm Thể Loại Mới'}
            </h2>
          </div>
        </div>

        <form onSubmit={handleSave} className="space-y-8 max-w-2xl mx-auto w-full">
          <div className="bg-brand-gray/60 border border-zinc-800/50 rounded-2xl p-6 space-y-4">
            <h3 className="text-sm font-bold text-white uppercase tracking-wider border-b border-zinc-800 pb-2">
              Thông Tin Thể Loại
            </h3>
            
            <div className="space-y-1.5">
              <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Tên thể loại</label>
              <input
                type="text"
                value={formData.genreName}
                onChange={(e) => setFormData({ genreName: e.target.value })}
                placeholder="Ví dụ: Hành động, Viễn tưởng..."
                className="w-full bg-brand-dark border border-zinc-800 rounded-xl py-2.5 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-orange/40 focus:ring-0 transition-colors"
                required
                data-testid="genre-name-input"
              />
            </div>
          </div>

          <div className="flex flex-col sm:flex-row gap-4 items-center justify-between pt-6">
            <button
              type="button"
              onClick={() => setIsModalOpen(false)}
              className="flex items-center justify-center gap-2 border border-zinc-800/80 hover:border-zinc-700 bg-brand-gray text-zinc-300 font-bold px-6 py-3 rounded-2xl text-xs transition-colors cursor-pointer w-full sm:w-auto"
            >
              <span>Hủy Bỏ</span>
            </button>

            <button
              type="submit"
              className="bg-brand-orange hover:bg-opacity-90 text-zinc-950 font-black px-8 py-3.5 rounded-2xl text-xs transition-all shadow-xl tracking-wider uppercase flex items-center justify-center gap-2 w-full sm:w-auto cursor-pointer"
              data-testid="genre-submit-btn"
            >
              <Check className="w-4 h-4" />
              <span>{editingGenre ? 'CẬP NHẬT' : 'THÊM MỚI'}</span>
            </button>
          </div>
        </form>
      </div>
    );
  }

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-[400px] bg-zinc-950 text-white space-y-6 animate-fade-in" data-testid="admin-genre-page">
      <div className="flex flex-col border-b border-zinc-800 pb-4">
        <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">DANH MỤC THỂ LOẠI</h1>
      </div>
      <div className="flex flex-col md:flex-row gap-4 justify-between items-center bg-brand-gray/60 border border-zinc-800/50 p-4 rounded-2xl backdrop-blur-md">
        <div className="relative w-full sm:w-80">
          <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-zinc-500">
            <Search className="w-4 h-4" />
          </span>
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Tìm kiếm thể loại..."
            className="w-full bg-brand-dark border border-zinc-800 text-zinc-100 placeholder-zinc-500 focus:border-brand-orange/40 focus:ring-0 rounded-xl py-2.5 pl-9 pr-4 text-xs transition-colors"
          />
        </div>

        <button
          onClick={handleOpenAdd}
          className="bg-brand-orange hover:bg-opacity-90 text-zinc-950 font-black px-5 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-all duration-300 shadow-lg shadow-brand-orange/10 flex items-center gap-2 cursor-pointer w-full sm:w-auto justify-center"
          data-testid="create-genre-btn"
        >
          <Plus className="w-4 h-4" />
          <span>THÊM THỂ LOẠI</span>
        </button>
      </div>

      {isLoading ? (
        <SkeletonTable rows={6} columns={3} />
      ) : (
        <div className="bg-neutral-950 border border-neutral-800 rounded-2xl overflow-hidden w-full shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse whitespace-nowrap" data-testid="genre-table">
              <thead>
                <tr className="bg-neutral-900/50 border-b border-neutral-800 text-[10px] font-black text-neutral-400 uppercase tracking-wider">
                  <th className="py-4 px-6 w-24 text-center">STT</th>
                  <th className="py-4 px-6">TÊN THỂ LOẠI</th>
                  <th className="py-4 px-6 w-32 text-right">THAO TÁC</th>
                </tr>
              </thead>
              <tbody>
                {filteredGenres.length === 0 ? (
                  <tr>
                    <td colSpan="3" className="py-12 text-center text-neutral-500 text-sm font-semibold">
                      <div className="flex flex-col items-center justify-center gap-2">
                        <LayoutList className="w-8 h-8 text-neutral-700" />
                        <span>Không tìm thấy thể loại nào.</span>
                      </div>
                    </td>
                  </tr>
                ) : (
                  filteredGenres.map((genre, index) => (
                    <tr key={genre.id} className="border-b border-neutral-800/50 hover:bg-neutral-900/50 transition-colors group">
                      <td className="py-4 px-6 text-center">
                        <span className="text-xs font-black text-neutral-400">{(index + 1).toString().padStart(2, '0')}</span>
                      </td>
                      <td className="py-4 px-6">
                        <span className="text-sm font-bold text-zinc-200 group-hover:text-amber-400 transition-colors" data-testid={`genre-name-${genre.id}`}>
                          {genre.genreName}
                        </span>
                      </td>
                      <td className="py-4 px-6 text-right">
                        <div className="flex items-center justify-end gap-2 transition-opacity">
                          <button
                            onClick={() => handleOpenEdit(genre)}
                            className="p-2 text-neutral-400 hover:text-amber-500 hover:bg-amber-500/10 border border-transparent hover:border-amber-500/20 rounded-lg transition-all cursor-pointer"
                            title="Sửa thể loại"
                            data-testid={`edit-genre-${genre.id}`}
                          >
                            <Pencil className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleDelete(genre.id)}
                            className="p-2 text-neutral-400 hover:text-red-500 hover:bg-red-500/10 border border-transparent hover:border-red-500/20 rounded-lg transition-all cursor-pointer"
                            title="Xóa thể loại"
                            data-testid={`delete-genre-${genre.id}`}
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
