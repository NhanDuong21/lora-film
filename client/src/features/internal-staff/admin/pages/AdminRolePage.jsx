import { useCallback, useEffect, useState } from 'react';
import { getRoles, createRole, updateRole, deleteRole, getPermissions } from '../services/authAdminService';
import { AsyncState, Input } from '@/components/common/ui/uiKit';

export default function AdminRolePage() {
  const [roles, setRoles] = useState([]);
  const [filteredRoles, setFilteredRoles] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [allPermissions, setAllPermissions] = useState([]);
  const [state, setState] = useState({ loading: true, error: '' });
  const [editingRole, setEditingRole] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({ name: '', description: '', permissionIds: [] });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const [rolesData, permsData] = await Promise.all([getRoles(), getPermissions()]);
      setRoles(rolesData || []);
      setFilteredRoles(rolesData || []);
      setAllPermissions(permsData || []);
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải vai trò.' });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    if (!searchQuery.trim()) {
      setFilteredRoles(roles);
      return;
    }
    const lower = searchQuery.toLowerCase();
    setFilteredRoles(roles.filter(r => 
      r.name?.toLowerCase().includes(lower) || 
      r.description?.toLowerCase().includes(lower)
    ));
  }, [searchQuery, roles]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingRole) {
        await updateRole(editingRole.id, formData);
      } else {
        await createRole(formData);
      }
      setIsModalOpen(false);
      await load();
    } catch (error) {
      alert(error?.message || 'Lỗi khi lưu vai trò');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa vai trò này?')) return;
    try {
      await deleteRole(id);
      await load();
    } catch (error) {
      alert(error?.message || 'Lỗi khi xóa vai trò');
    }
  };

  const openModal = (role = null) => {
    setEditingRole(role);
    if (role) {
      setFormData({
        name: role.name || '',
        description: role.description || '',
        permissionIds: role.permissions?.map(p => p.id) || []
      });
    } else {
      setFormData({ name: '', description: '', permissionIds: [] });
    }
    setIsModalOpen(true);
  };

  const handlePermissionToggle = (permId) => {
    setFormData(prev => {
      const perms = prev.permissionIds.includes(permId)
        ? prev.permissionIds.filter(id => id !== permId)
        : [...prev.permissionIds, permId];
      return { ...prev, permissionIds: perms };
    });
  };

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black uppercase">Quản lý Vai trò</h1>
          <p className="mt-1 text-sm text-zinc-500">Thiết lập và phân quyền cho các vai trò trong hệ thống.</p>
        </div>
        <button onClick={() => openModal()} className="rounded-xl bg-brand-orange px-4 py-2 text-sm font-bold text-zinc-950 hover:bg-brand-orange/90">
          + Thêm Vai trò
        </button>
      </div>

      <div className="bg-zinc-900/50 border border-zinc-800 rounded-2xl p-4 flex flex-col xl:flex-row gap-3">
        <div className="flex-1 relative">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-search absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500 w-4 h-4"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
          <input 
            placeholder="Tìm kiếm vai trò theo tên, mô tả..." 
            value={searchQuery}
            onChange={event => setSearchQuery(event.target.value)} 
            className="w-full bg-zinc-950 border border-zinc-800 rounded-xl h-10 pl-10 pr-4 text-sm text-zinc-100 placeholder:text-zinc-600 focus:border-brand-orange outline-none transition-colors"
          />
        </div>
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!filteredRoles.length} emptyMessage="Chưa có vai trò nào">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filteredRoles.map(role => (
            <div key={role.id} className="rounded-2xl border border-zinc-800 bg-zinc-900 p-5">
              <div className="flex items-start justify-between mb-2">
                <h3 className="font-bold text-lg text-brand-orange">{role.name}</h3>
                <div className="space-x-2">
                  <button onClick={() => openModal(role)} className="text-xs text-zinc-400 hover:text-white">Sửa</button>
                  <button onClick={() => handleDelete(role.id)} className="text-xs text-red-400 hover:text-red-300">Xóa</button>
                </div>
              </div>
              <p className="text-sm text-zinc-400 mb-4">{role.description || 'Không có mô tả'}</p>
              <div className="space-y-1">
                <p className="text-xs font-semibold text-zinc-500 uppercase">Quyền hạn ({role.permissions?.length || 0}):</p>
                <div className="flex flex-wrap gap-1">
                  {role.permissions?.map(p => (
                    <span key={p.id} className="rounded bg-zinc-800 px-1.5 py-0.5 text-[10px] text-zinc-300">
                      {p.name}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          ))}
        </div>
      </AsyncState>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <form onSubmit={handleSubmit} className="w-full max-w-md rounded-2xl border border-zinc-800 bg-zinc-900 p-6 space-y-4">
            <div className="flex items-center justify-between mb-2">
              <h2 className="text-xl font-bold text-white">{editingRole ? 'Sửa Vai trò' : 'Thêm Vai trò'}</h2>
            </div>
            <div>
              <label className="text-xs font-bold text-zinc-400">Tên vai trò</label>
              <Input value={formData.name} onChange={e => setFormData({ ...formData, name: e.target.value })} required placeholder="VD: ROLE_MANAGER" />
            </div>
            <div>
              <label className="text-xs font-bold text-zinc-400">Mô tả</label>
              <Input value={formData.description} onChange={e => setFormData({ ...formData, description: e.target.value })} placeholder="VD: Quản lý hệ thống" />
            </div>
            <div>
              <label className="text-xs font-bold text-zinc-400 mb-2 block">Phân quyền</label>
              <div className="max-h-60 overflow-y-auto rounded-lg border border-zinc-800 p-3 space-y-2">
                {allPermissions.map(perm => (
                  <label key={perm.id} className="flex items-center gap-2 cursor-pointer">
                    <input 
                      type="checkbox" 
                      className="rounded border-zinc-700 bg-zinc-900 text-brand-orange focus:ring-brand-orange"
                      checked={formData.permissionIds.includes(perm.id)} 
                      onChange={() => handlePermissionToggle(perm.id)} 
                    />
                    <span className="text-sm text-zinc-300">{perm.name} <span className="text-xs text-zinc-500">({perm.code})</span></span>
                  </label>
                ))}
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-4">
              <button type="button" onClick={() => setIsModalOpen(false)} className="rounded-xl border border-zinc-700 px-4 py-2 text-sm text-zinc-300 hover:bg-zinc-800">Hủy</button>
              <button type="submit" className="rounded-xl bg-brand-orange px-4 py-2 text-sm font-bold text-zinc-950 hover:bg-brand-orange/90">Lưu</button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}
