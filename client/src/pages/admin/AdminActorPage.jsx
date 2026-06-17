import { useState, useMemo } from 'react';
import { Search, PlusCircle, Edit3, Trash2, X, Globe, Users, Film, UserCheck } from 'lucide-react';
import { useData } from '../../contexts/DataContext';

export default function AdminActorView({ 
  actors, 
  updateActorsState, 
  triggerToast 
}) {
  const { movies } = useData();
  const [searchQuery, setSearchQuery] = useState('');
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [editingActor, setEditingActor] = useState(null);

  // Form states
  const [formName, setFormName] = useState('');
  const [formNationality, setFormNationality] = useState('Việt Nam');
  const [formImageUrl, setFormImageUrl] = useState('');
  const [formBiography, setFormBiography] = useState('');
  const [formStarredFilms, setFormStarredFilms] = useState([]);
  const [formBirthdate, setFormBirthdate] = useState('');

  // Filter & Search computation
  const filteredActors = useMemo(() => {
    return actors.filter(actor => 
      actor.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      actor.nationality.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (actor.starredFilms && actor.starredFilms.some(f => f.toLowerCase().includes(searchQuery.toLowerCase())))
    );
  }, [actors, searchQuery]);

  // Compute Metrics
  const metrics = useMemo(() => {
    const total = actors.length;
    const nationalities = new Set(actors.map(a => a.nationality)).size;
    const totalLinkedFilms = new Set(actors.flatMap(a => a.starredFilms || [])).size;
    return { total, nationalities, totalLinkedFilms };
  }, [actors]);

  const handleOpenAdd = () => {
    setEditingActor(null);
    setFormName('');
    setFormNationality('Việt Nam');
    setFormImageUrl('');
    setFormBiography('');
    setFormStarredFilms([]);
    setFormBirthdate('');
    setIsDrawerOpen(true);
  };

  const handleOpenEdit = (actor) => {
    setEditingActor(actor);
    setFormName(actor.name);
    setFormNationality(actor.nationality || 'Việt Nam');
    setFormImageUrl(actor.imageUrl || '');
    setFormBiography(actor.biography || '');
    setFormStarredFilms(actor.starredFilms || []);
    setFormBirthdate(actor.birthdate || '');
    setIsDrawerOpen(true);
  };

  const handleSave = (e) => {
    e.preventDefault();
    if (!formName || !formNationality) {
      triggerToast('Vui lòng nhập tên diễn viên và quốc tịch!', 'error');
      return;
    }

    const actorPayload = {
      name: formName,
      nationality: formNationality,
      imageUrl: formImageUrl || 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80',
      biography: formBiography,
      starredFilms: formStarredFilms,
      birthdate: formBirthdate
    };

    if (editingActor) {
      const updated = actors.map(a => String(a.id) === String(editingActor.id) ? { ...actorPayload, id: a.id } : a);
      updateActorsState(updated);
      triggerToast('Cập nhật thông tin diễn viên thành công!');
    } else {
      const nextNumId = actors.length ? Math.max(...actors.map(a => {
        const num = parseInt(String(a.id).replace(/\D/g, ''));
        return isNaN(num) ? 0 : num;
      })) + 1 : 1;
      
      const newActor = {
        ...actorPayload,
        id: `a${nextNumId}`
      };
      updateActorsState([...actors, newActor]);
      triggerToast('Thêm diễn viên mới thành công!');
    }
    setIsDrawerOpen(false);
  };

  const handleDelete = (id) => {
    if (confirm('Bạn có chắc chắn muốn xóa diễn viên này?')) {
      const updated = actors.filter(a => String(a.id) !== String(id));
      updateActorsState(updated);
      triggerToast('Đã xóa diễn viên thành công!');
    }
  };

  const nationalityOptions = [
    'Việt Nam',
    'Mỹ',
    'Cuba/Tây Ban Nha',
    'Canada',
    'Anh Quốc',
    'Romania',
    'Hàn Quốc',
    'Nhật Bản',
    'Pháp',
    'Đức',
    'Úc',
    'Ấn Độ',
    'Khác'
  ];

  return (
    <div className="space-y-6">
      {/* 3 analytical summary indicators */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-5 flex items-center justify-between shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div>
            <span className="text-zinc-400 text-xs font-black uppercase tracking-wider block mb-1">
              TỔNG SỐ DIỄN VIÊN
            </span>
            <span className="text-2xl font-bold text-amber-400">{metrics.total} Diễn viên</span>
          </div>
          <div className="p-3 bg-brand-coral/10 border border-brand-coral/20 rounded-xl">
            <Users className="w-5 h-5 text-brand-coral" />
          </div>
        </div>

        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-5 flex items-center justify-between shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div>
            <span className="text-zinc-400 text-xs font-black uppercase tracking-wider block mb-1">
              QUỐC TỊCH ĐẠI DIỆN
            </span>
            <span className="text-2xl font-bold text-amber-400">{metrics.nationalities} Quốc tịch</span>
          </div>
          <div className="p-3 bg-amber-500/10 border border-amber-500/20 rounded-xl">
            <Globe className="w-5 h-5 text-amber-500" />
          </div>
        </div>

        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-5 flex items-center justify-between shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div>
            <span className="text-zinc-400 text-xs font-black uppercase tracking-wider block mb-1">
              BỘ SƯU TẬP TÁC PHẨM
            </span>
            <span className="text-2xl font-bold text-amber-400">{metrics.totalLinkedFilms} Phim liên kết</span>
          </div>
          <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl">
            <Film className="w-5 h-5 text-emerald-500" />
          </div>
        </div>
      </div>

      {/* Action panel */}
      <div className="flex flex-col sm:flex-row justify-between items-center gap-4 pt-4">
        <div className="relative w-full sm:w-80">
          <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-zinc-500">
            <Search className="w-4 h-4" />
          </span>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm kiếm diễn viên..."
            className="w-full bg-zinc-950 border border-zinc-900 rounded-xl py-2.5 pl-9 pr-4 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral transition-colors"
          />
        </div>
        <button
          onClick={handleOpenAdd}
          className="flex items-center gap-2 bg-brand-coral hover:bg-opacity-90 text-white text-xs font-black py-2.5 px-4 rounded-xl transition-all w-full sm:w-auto justify-center"
        >
          <PlusCircle className="w-4.5 h-4.5" />
          <span>THÊM DIỄN VIÊN</span>
        </button>
      </div>

      {/* Main Grid Data Sheet */}
      <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-zinc-400">
            <thead className="bg-zinc-950/80 text-zinc-400 font-black uppercase tracking-wider border-b border-zinc-800">
              <tr>
                <th className="py-4 px-6 text-center w-20">ID</th>
                <th className="py-4 px-6">Ảnh & Tên Diễn Viên</th>
                <th className="py-4 px-6">Quốc Tịch</th>
                <th className="py-4 px-6">Phim Tham Gia</th>
                <th className="py-4 px-6 text-center w-32">Hành Động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/60">
              {filteredActors.length === 0 ? (
                <tr>
                  <td colSpan="5" className="py-12 text-center text-zinc-500 font-semibold">
                    Không tìm thấy diễn viên nào phù hợp.
                  </td>
                </tr>
              ) : (
                filteredActors.map((actor) => (
                  <tr key={actor.id} className="hover:bg-zinc-900/20 transition-colors border-b border-zinc-800/40">
                    <td className="py-4 px-6 text-center text-zinc-400 font-mono font-medium">
                      #{actor.id}
                    </td>
                    <td className="py-4 px-6">
                      <div className="flex items-center gap-3">
                        <img 
                          src={actor.imageUrl} 
                          alt={actor.name} 
                          className="w-10 h-10 object-cover rounded-full border border-zinc-800"
                          onError={(e) => {
                            e.target.onerror = null;
                            e.target.src = 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80';
                          }}
                        />
                        <span className="font-bold text-zinc-200 text-sm">{actor.name}</span>
                      </div>
                    </td>
                    <td className="py-4 px-6 text-zinc-300 font-medium">
                      {actor.nationality}
                    </td>
                    <td className="py-4 px-6 text-zinc-400 font-medium max-w-xs truncate">
                      {actor.starredFilms && actor.starredFilms.length > 0 
                        ? actor.starredFilms.join(', ') 
                        : 'Chưa liên kết phim'}
                    </td>
                    <td className="py-4 px-6">
                      <div className="flex justify-center gap-2">
                        <button
                          onClick={() => handleOpenEdit(actor)}
                          className="p-2 text-zinc-400 hover:text-white bg-zinc-950 border border-zinc-800 rounded-xl transition-all"
                          title="Sửa diễn viên"
                        >
                          <Edit3 className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => handleDelete(actor.id)}
                          className="p-2 text-red-400 hover:text-red-300 bg-red-950/20 border border-red-900/40 rounded-xl transition-all"
                          title="Xóa diễn viên"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
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

      {/* Slide-over Side Drawer Sheet Form */}
      {isDrawerOpen && (
        <div 
          onClick={() => setIsDrawerOpen(false)}
          className="fixed inset-0 bg-black/80 backdrop-blur-sm z-40 transition-opacity duration-300"
        />
      )}

      <div className={`fixed inset-y-0 right-0 w-full max-w-md bg-zinc-950 border-l border-zinc-900 shadow-2xl z-50 p-6 flex flex-col justify-between transition-transform duration-300 transform rounded-l-2xl ${
        isDrawerOpen ? 'translate-x-0' : 'translate-x-full'
      }`}>
        <div className="flex flex-col h-full justify-between">
          <div>
            <div className="flex items-center justify-between pb-4 border-b border-zinc-900 mb-6">
              <h3 className="text-sm font-black uppercase tracking-wider text-zinc-100 flex items-center gap-2">
                <UserCheck className="w-4.5 h-4.5 text-brand-coral" />
                {editingActor ? 'Cập Nhật Diễn Viên' : 'Thêm Diễn Viên Mới'}
              </h3>
              <button 
                onClick={() => setIsDrawerOpen(false)}
                className="text-zinc-500 hover:text-white transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSave} className="space-y-4 overflow-y-auto max-h-[70vh] pr-1">
              <div>
                <label className="block text-[10px] font-black uppercase text-zinc-500 mb-1.5">
                  Tên diễn viên *
                </label>
                <input
                  type="text"
                  required
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  placeholder="Ví dụ: Kaity Nguyễn"
                  className="w-full bg-zinc-900/40 border border-zinc-900 rounded-xl px-3 py-2 text-xs text-zinc-100 outline-none focus:border-brand-coral transition-colors"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-[10px] font-black uppercase text-zinc-500 mb-1.5">
                    Quốc tịch *
                  </label>
                  <select
                    value={formNationality}
                    onChange={(e) => setFormNationality(e.target.value)}
                    className="w-full bg-zinc-900/40 border border-zinc-900 rounded-xl px-3 py-2 text-xs text-zinc-100 outline-none focus:border-brand-coral transition-colors"
                  >
                    {nationalityOptions.map((opt) => (
                      <option key={opt} value={opt}>{opt}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-[10px] font-black uppercase text-zinc-500 mb-1.5">
                    Ngày sinh (YYYY-MM-DD)
                  </label>
                  <input
                    type="text"
                    value={formBirthdate}
                    onChange={(e) => setFormBirthdate(e.target.value)}
                    placeholder="Ví dụ: 1999-04-09"
                    className="w-full bg-zinc-900/40 border border-zinc-900 rounded-xl px-3 py-2 text-xs text-zinc-100 outline-none focus:border-brand-coral transition-colors"
                  />
                </div>
              </div>

              <div>
                <label className="block text-[10px] font-black uppercase text-zinc-500 mb-1.5">
                  Ảnh đại diện URL
                </label>
                <input
                  type="text"
                  value={formImageUrl}
                  onChange={(e) => setFormImageUrl(e.target.value)}
                  placeholder="https://images.unsplash.com/photo-..."
                  className="w-full bg-zinc-900/40 border border-zinc-900 rounded-xl px-3 py-2 text-xs text-zinc-100 outline-none focus:border-brand-coral transition-colors"
                />
              </div>

              <div>
                <label className="block text-[10px] font-black uppercase text-zinc-500 mb-1.5">
                  Tiểu sử chi tiết
                </label>
                <textarea
                  value={formBiography}
                  onChange={(e) => setFormBiography(e.target.value)}
                  placeholder="Nhập giới thiệu, thành tựu nghệ thuật..."
                  rows="4"
                  className="w-full bg-zinc-900/40 border border-zinc-900 rounded-xl px-3 py-2 text-xs text-zinc-100 outline-none focus:border-brand-coral transition-colors resize-none"
                />
              </div>

              <div>
                <label className="block text-[10px] font-black uppercase text-zinc-500 mb-1.5">
                  Liên kết phim (Chọn phim tham gia)
                </label>
                <div className="bg-zinc-900/40 border border-zinc-900 rounded-xl p-3 space-y-2 max-h-40 overflow-y-auto">
                  {movies.map(movie => (
                    <label key={movie.id} className="flex items-center gap-2 text-xs text-zinc-400 cursor-pointer select-none">
                      <input 
                        type="checkbox"
                        checked={formStarredFilms.includes(movie.title)}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setFormStarredFilms([...formStarredFilms, movie.title]);
                          } else {
                            setFormStarredFilms(formStarredFilms.filter(t => t !== movie.title));
                          }
                        }}
                        className="rounded border-zinc-900 bg-zinc-950 text-brand-coral focus:ring-brand-coral/20 w-3.5 h-3.5"
                      />
                      <span>{movie.title}</span>
                    </label>
                  ))}
                </div>
              </div>
            </form>
          </div>

          <div className="pt-4 border-t border-zinc-900 flex justify-end gap-3 mt-6">
            <button
              type="button"
              onClick={() => setIsDrawerOpen(false)}
              className="px-4 py-2 border border-zinc-900 hover:bg-zinc-900 text-zinc-450 hover:text-white rounded-xl text-xs font-semibold transition-colors"
            >
              Hủy bỏ
            </button>
            <button
              onClick={handleSave}
              type="button"
              className="px-4 py-2 bg-brand-coral hover:bg-opacity-90 text-white rounded-xl text-xs font-black transition-colors shadow-lg shadow-brand-coral/10"
            >
              Lưu Thay Đổi
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
