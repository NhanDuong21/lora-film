import { useCallback, useEffect, useMemo, useState } from 'react';
import { getDepartments, createDepartment, updateDepartment, deleteDepartment } from '../services/userAdminService';
import { AsyncState } from '@/components/common/ui/uiKit';
import { Building2, Search, Plus, Edit2, Trash2, ShieldAlert, BarChart3, Users } from 'lucide-react';
import { useOutletContext } from 'react-router-dom';
import AdminStatCard from '../components/AdminStatCard';
import useAdminAccess from '../hooks/useAdminAccess';

export default function AdminDepartmentPage() {
  const can = useAdminAccess();
  const canCreateDepartments = can('DEPARTMENT_CREATE');
  const canUpdateDepartments = can('DEPARTMENT_UPDATE');
  const canDeleteDepartments = can('DEPARTMENT_DELETE');
  const [departments, setDepartments] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [state, setState] = useState({ loading: true, error: '' });
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingDept, setEditingDept] = useState(null);
  const [formData, setFormData] = useState({ code: '', name: '', description: '' });
  const [stats, setStats] = useState({ total: 0 });
  const outlet = useOutletContext();
  const confirmAction = outlet?.triggerConfirm || (() => Promise.resolve(true));
  const notify = outlet?.triggerToast || (() => undefined);

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const data = await getDepartments();
      const depts = data || [];
      setDepartments(depts);
      setStats({ total: depts.length });
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải phòng ban.' });
    }
  }, []);
  
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const filteredDepartments = useMemo(() => {
    if (!searchQuery.trim()) {
      return departments;
    }
    const lower = searchQuery.toLowerCase();
    return departments.filter(d =>
      d.name?.toLowerCase().includes(lower) || 
      d.code?.toLowerCase().includes(lower) ||
      d.description?.toLowerCase().includes(lower)
    );
  }, [searchQuery, departments]);

  const handleSave = async (e) => {
    e.preventDefault();
    if (!formData.code.trim() || !formData.name.trim()) {
       notify('Mã và tên phòng ban là bắt buộc.', 'error');
       return;
    }
    try {
      if (editingDept) {
        await updateDepartment(editingDept.id, formData);
      } else {
        await createDepartment(formData);
      }
      setIsModalOpen(false);
      await load();
      notify(editingDept ? 'Phòng ban đã được cập nhật.' : 'Phòng ban đã được tạo.');
    } catch (error) {
      notify(error?.message || 'Lỗi khi lưu phòng ban', 'error');
    }
  };

  const handleDelete = async (id) => {
    if (await confirmAction('Hành động này không thể hoàn tác. Xóa phòng ban này?')) {
      try {
        await deleteDepartment(id);
        await load();
        notify('Phòng ban đã được xóa.');
      } catch (error) {
        notify(error?.message || 'Không thể xóa phòng ban đang có nhân viên.', 'error');
      }
    }
  };

  const openModal = (dept = null) => {
    setEditingDept(dept);
    setFormData(dept ? { code: dept.code, name: dept.name, description: dept.description } : { code: '', name: '', description: '' });
    setIsModalOpen(true);
  };

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-[#050506] p-6 text-white md:p-8">
      <header className="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
        <div>
          <h1 className="text-2xl font-black uppercase tracking-wider text-white">Quản lý <span className="text-brand-orange">Phòng Ban</span></h1>
          <p className="mt-1 text-sm text-zinc-500">Cơ cấu tổ chức và các bộ phận trong hệ thống.</p>
        </div>
        {canCreateDepartments && <button onClick={() => openModal()} className="rounded-xl bg-brand-orange px-5 py-2.5 text-sm font-bold text-zinc-950 hover:bg-orange-600 transition-colors shadow-lg shadow-brand-orange/20 flex items-center gap-2">
          <Plus size={18} />
          <span>Thêm Phòng Ban</span>
        </button>}
      </header>

      {/* Dashboard Statistics */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <AdminStatCard title="Tổng số phòng ban" value={stats.total} icon={Building2} colorClass="bg-blue-500/10 text-blue-500 border-blue-500/20" />
        <AdminStatCard title="Hoạt động" value={stats.total} icon={BarChart3} colorClass="bg-emerald-500/10 text-emerald-500 border-emerald-500/20" />
        <AdminStatCard title="Quy mô" value="100%" icon={Users} colorClass="bg-purple-500/10 text-purple-500 border-purple-500/20" />
      </div>

      <div className="bg-zinc-900/50 border border-zinc-800 rounded-2xl p-4 flex flex-col sm:flex-row gap-3">
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500 w-4 h-4" />
          <input 
            placeholder="Tìm kiếm theo mã, tên hoặc mô tả..." 
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            className="w-full bg-zinc-950 border border-zinc-800 rounded-xl h-10 pl-10 pr-4 text-sm text-zinc-100 placeholder:text-zinc-600 focus:border-brand-orange outline-none transition-colors"
          />
        </div>
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!filteredDepartments.length} emptyMessage="Không tìm thấy phòng ban nào">
        <div className="overflow-x-auto rounded-2xl border border-zinc-800 bg-zinc-900/30">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-zinc-950/50 text-xs uppercase font-black text-zinc-500 tracking-wider">
              <tr>
                <th className="p-4 rounded-tl-2xl w-32">Mã PB</th>
                <th className="p-4 w-1/3">Tên phòng ban</th>
                <th className="p-4">Mô tả</th>
                <th className="p-4 text-right rounded-tr-2xl w-24">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/50">
              {filteredDepartments.map(dept => (
                <tr key={dept.id} className="hover:bg-zinc-800/20 transition-colors group">
                  <td className="p-4">
                     <span className="font-mono text-xs font-bold text-zinc-300 bg-zinc-900 px-2.5 py-1 rounded-md border border-zinc-800 group-hover:border-zinc-700 group-hover:text-brand-orange transition-colors">
                      {dept.code}
                    </span>
                  </td>
                  <td className="p-4">
                    <p className="font-bold text-zinc-100">{dept.name}</p>
                  </td>
                  <td className="p-4 text-zinc-400 text-xs leading-relaxed max-w-md">
                    {dept.description || <span className="italic opacity-50">Không có mô tả</span>}
                  </td>
                  <td className="p-4 text-right">
                    <div className="flex items-center justify-end gap-1">
                      {canUpdateDepartments && <button
                        onClick={() => openModal(dept)} 
                        className="p-1.5 rounded-lg text-zinc-400 hover:text-blue-400 hover:bg-blue-500/10 transition-colors"
                        title="Chỉnh sửa"
                      >
                        <Edit2 size={16} />
                      </button>}
                      {canDeleteDepartments && <button
                        onClick={() => handleDelete(dept.id)} 
                        className="p-1.5 rounded-lg text-zinc-400 hover:text-red-400 hover:bg-red-500/10 transition-colors"
                        title="Xóa"
                      >
                        <Trash2 size={16} />
                      </button>}
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
                <Building2 className="text-brand-orange" size={24} />
                {editingDept ? 'Sửa phòng ban' : 'Thêm phòng ban mới'}
              </h2>
              <button type="button" onClick={() => setIsModalOpen(false)} className="text-zinc-500 hover:text-white transition-colors">
                X
              </button>
            </div>

            <div className="bg-blue-500/10 border border-blue-500/20 p-3 rounded-xl flex gap-3 text-sm text-blue-400">
               <ShieldAlert size={18} className="shrink-0 mt-0.5" />
               <p>Mã phòng ban phải là duy nhất và không thể thay đổi đối với các bộ phận đã được định danh trong hệ thống.</p>
            </div>
            
            <div className="space-y-4">
              <div className="grid grid-cols-3 gap-4">
                <div className="space-y-1.5 col-span-1">
                  <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Mã PB <span className="text-brand-orange">*</span></label>
                  <input 
                    required 
                    value={formData.code} 
                    onChange={e => setFormData({ ...formData, code: e.target.value })} 
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm focus:border-brand-orange outline-none transition-colors font-mono uppercase" 
                    placeholder="VD: IT" 
                  />
                </div>
                <div className="space-y-1.5 col-span-2">
                  <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Tên phòng ban <span className="text-brand-orange">*</span></label>
                  <input 
                    required 
                    value={formData.name} 
                    onChange={e => setFormData({ ...formData, name: e.target.value })} 
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm focus:border-brand-orange outline-none transition-colors" 
                    placeholder="VD: Công nghệ thông tin" 
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Mô tả chi tiết</label>
                <textarea 
                  value={formData.description || ''} 
                  onChange={e => setFormData({ ...formData, description: e.target.value })} 
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-xl p-4 text-sm focus:border-brand-orange outline-none transition-colors min-h-[100px] resize-none" 
                  placeholder="Chức năng và nhiệm vụ của phòng ban này..." 
                />
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-6 border-t border-zinc-800">
              <button type="button" onClick={() => setIsModalOpen(false)} className="rounded-xl border border-zinc-700 bg-zinc-900 px-5 py-2.5 text-sm font-bold text-zinc-300 hover:bg-zinc-800 transition-colors">
                Hủy
              </button>
              <button type="submit" className="rounded-xl bg-brand-orange px-5 py-2.5 text-sm font-bold text-zinc-950 hover:bg-orange-600 transition-colors shadow-lg shadow-brand-orange/20">
                {editingDept ? 'Lưu thay đổi' : 'Tạo phòng ban'}
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}
