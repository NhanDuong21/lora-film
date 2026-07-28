import { useCallback, useEffect, useState } from 'react';
import { getPositions, createPosition, updatePosition, deletePosition } from '../services/userAdminService';
import { AsyncState, Input } from '@/components/common/ui/uiKit';
import { Briefcase, Search, Plus, Edit2, Trash2, ShieldAlert, BadgeCheck, Users } from 'lucide-react';

export default function AdminPositionPage() {
  const [positions, setPositions] = useState([]);
  const [filteredPositions, setFilteredPositions] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [state, setState] = useState({ loading: true, error: '' });
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingPos, setEditingPos] = useState(null);
  const [formData, setFormData] = useState({ code: '', name: '', description: '' });
  const [stats, setStats] = useState({ total: 0 });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const data = await getPositions();
      const posData = data || [];
      setPositions(posData);
      setFilteredPositions(posData);
      setStats({ total: posData.length });
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải vị trí.' });
    }
  }, []);
  
  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    if (!searchQuery.trim()) {
      setFilteredPositions(positions);
      return;
    }
    const lower = searchQuery.toLowerCase();
    setFilteredPositions(positions.filter(p => 
      p.name?.toLowerCase().includes(lower) || 
      p.code?.toLowerCase().includes(lower) ||
      p.description?.toLowerCase().includes(lower)
    ));
  }, [searchQuery, positions]);

  const handleSave = async (e) => {
    e.preventDefault();
    if (!formData.code.trim() || !formData.name.trim()) {
       return alert('Mã và tên vị trí là bắt buộc');
    }
    try {
      if (editingPos) {
        await updatePosition(editingPos.id, formData);
      } else {
        await createPosition(formData);
      }
      setIsModalOpen(false);
      await load();
    } catch (error) {
      alert(error?.message || 'Lỗi khi lưu vị trí');
    }
  };

  const handleDelete = async (id) => {
    if (confirm('Hành động này không thể hoàn tác. Bạn có chắc chắn muốn xóa vị trí này?')) {
      try {
        await deletePosition(id);
        await load();
      } catch (error) {
        alert(error?.message || 'Lỗi khi xóa. Có thể vị trí này đang được gán cho nhân viên.');
      }
    }
  };

  const openModal = (pos = null) => {
    setEditingPos(pos);
    setFormData(pos ? { code: pos.code, name: pos.name, description: pos.description } : { code: '', name: '', description: '' });
    setIsModalOpen(true);
  };

  const StatCard = ({ title, value, icon: Icon, colorClass }) => (
    <div className="bg-zinc-900/50 border border-zinc-800 rounded-2xl p-5 flex items-center justify-between hover:bg-zinc-900 transition-colors">
      <div>
        <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider mb-1">{title}</p>
        <h3 className="text-3xl font-black text-white">{value}</h3>
      </div>
      <div className={`w-12 h-12 rounded-full flex items-center justify-center ${colorClass}`}>
        <Icon size={24} />
      </div>
    </div>
  );

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-[#050506] p-6 text-white md:p-8">
      <header className="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
        <div>
          <h1 className="text-2xl font-black uppercase tracking-wider text-white">Quản lý <span className="text-brand-orange">Vị Trí</span></h1>
          <p className="mt-1 text-sm text-zinc-500">Danh mục chức danh và vị trí công việc trong hệ thống.</p>
        </div>
        <button onClick={() => openModal()} className="rounded-xl bg-brand-orange px-5 py-2.5 text-sm font-bold text-zinc-950 hover:bg-orange-600 transition-colors shadow-lg shadow-brand-orange/20 flex items-center gap-2">
          <Plus size={18} />
          <span>Thêm Vị Trí</span>
        </button>
      </header>

      {/* Dashboard Statistics */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard title="Tổng số vị trí" value={stats.total || '...'} icon={Briefcase} colorClass="bg-blue-500/10 text-blue-500 border border-blue-500/20" />
        <StatCard title="Phân bổ" value="100%" icon={Users} colorClass="bg-emerald-500/10 text-emerald-500 border border-emerald-500/20" />
        <StatCard title="Định danh" value="Chuẩn" icon={BadgeCheck} colorClass="bg-purple-500/10 text-purple-500 border border-purple-500/20" />
      </div>

      <div className="bg-zinc-900/50 border border-zinc-800 rounded-2xl p-4 flex flex-col sm:flex-row gap-3">
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500 w-4 h-4" />
          <input 
            placeholder="Tìm kiếm theo mã, tên chức danh hoặc mô tả..." 
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            className="w-full bg-zinc-950 border border-zinc-800 rounded-xl h-10 pl-10 pr-4 text-sm text-zinc-100 placeholder:text-zinc-600 focus:border-brand-orange outline-none transition-colors"
          />
        </div>
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!filteredPositions.length} emptyMessage="Không tìm thấy vị trí nào">
        <div className="overflow-x-auto rounded-2xl border border-zinc-800 bg-zinc-900/30">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-zinc-950/50 text-xs uppercase font-black text-zinc-500 tracking-wider">
              <tr>
                <th className="p-4 rounded-tl-2xl w-32">Mã Vị trí</th>
                <th className="p-4 w-1/3">Tên chức danh</th>
                <th className="p-4">Mô tả</th>
                <th className="p-4 text-right rounded-tr-2xl w-24">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/50">
              {filteredPositions.map(pos => (
                <tr key={pos.id} className="hover:bg-zinc-800/20 transition-colors group">
                  <td className="p-4">
                     <span className="font-mono text-xs font-bold text-zinc-300 bg-zinc-900 px-2.5 py-1 rounded-md border border-zinc-800 group-hover:border-zinc-700 group-hover:text-brand-orange transition-colors">
                      {pos.code}
                    </span>
                  </td>
                  <td className="p-4">
                    <p className="font-bold text-zinc-100">{pos.name}</p>
                  </td>
                  <td className="p-4 text-zinc-400 text-xs leading-relaxed max-w-md">
                    {pos.description || <span className="italic opacity-50">Không có mô tả</span>}
                  </td>
                  <td className="p-4 text-right">
                    <div className="flex items-center justify-end gap-1">
                      <button 
                        onClick={() => openModal(pos)} 
                        className="p-1.5 rounded-lg text-zinc-400 hover:text-blue-400 hover:bg-blue-500/10 transition-colors"
                        title="Chỉnh sửa"
                      >
                        <Edit2 size={16} />
                      </button>
                      <button 
                        onClick={() => handleDelete(pos.id)} 
                        className="p-1.5 rounded-lg text-zinc-400 hover:text-red-400 hover:bg-red-500/10 transition-colors"
                        title="Xóa"
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncState>

      {/* Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4 animate-fade-in">
          <form onSubmit={handleSave} className="w-full max-w-lg rounded-2xl border border-zinc-800 bg-zinc-950 p-6 space-y-5 shadow-2xl">
            <div className="flex items-center justify-between border-b border-zinc-800 pb-4">
              <h2 className="text-xl font-black uppercase tracking-wider text-white flex items-center gap-2">
                <Briefcase className="text-brand-orange" size={24} />
                {editingPos ? 'Sửa thông tin vị trí' : 'Thêm vị trí mới'}
              </h2>
              <button type="button" onClick={() => setIsModalOpen(false)} className="text-zinc-500 hover:text-white transition-colors">
                X
              </button>
            </div>

            <div className="bg-amber-500/10 border border-amber-500/20 p-3 rounded-xl flex gap-3 text-sm text-amber-500">
               <ShieldAlert size={18} className="shrink-0 mt-0.5" />
               <p>Mã chức danh/vị trí nên viết tắt bằng ký tự không dấu (VD: MGR, DEV). Việc thay đổi mã có thể ảnh hưởng đến các báo cáo cũ.</p>
            </div>
            
            <div className="space-y-4">
              <div className="grid grid-cols-3 gap-4">
                <div className="space-y-1.5 col-span-1">
                  <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Mã Vị trí <span className="text-brand-orange">*</span></label>
                  <input 
                    required 
                    value={formData.code} 
                    onChange={e => setFormData({ ...formData, code: e.target.value })} 
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm focus:border-brand-orange outline-none transition-colors font-mono uppercase" 
                    placeholder="VD: MGR" 
                  />
                </div>
                <div className="space-y-1.5 col-span-2">
                  <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Tên chức danh <span className="text-brand-orange">*</span></label>
                  <input 
                    required 
                    value={formData.name} 
                    onChange={e => setFormData({ ...formData, name: e.target.value })} 
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm focus:border-brand-orange outline-none transition-colors" 
                    placeholder="VD: Trưởng phòng" 
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Mô tả công việc</label>
                <textarea 
                  value={formData.description || ''} 
                  onChange={e => setFormData({ ...formData, description: e.target.value })} 
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-xl p-4 text-sm focus:border-brand-orange outline-none transition-colors min-h-[100px] resize-none" 
                  placeholder="Yêu cầu chung và trách nhiệm của vị trí này..." 
                />
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-6 border-t border-zinc-800">
              <button type="button" onClick={() => setIsModalOpen(false)} className="rounded-xl border border-zinc-700 bg-zinc-900 px-5 py-2.5 text-sm font-bold text-zinc-300 hover:bg-zinc-800 transition-colors">
                Hủy
              </button>
              <button type="submit" className="rounded-xl bg-brand-orange px-5 py-2.5 text-sm font-bold text-zinc-950 hover:bg-orange-600 transition-colors shadow-lg shadow-brand-orange/20">
                {editingPos ? 'Lưu thay đổi' : 'Tạo vị trí'}
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}
