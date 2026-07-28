import { useCallback, useEffect, useState } from 'react';
import { getPermissions, createPermission, updatePermission, deletePermission } from '../services/authAdminService';
import { AsyncState, Input } from '@/components/common/ui/uiKit';

export default function AdminPermissionPage() {
  const [permissions, setPermissions] = useState([]);
  const [state, setState] = useState({ loading: true, error: '' });
  const [editingPerm, setEditingPerm] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({ name: '', code: '', description: '' });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const data = await getPermissions();
      setPermissions(data || []);
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải quyền hạn.' });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingPerm) {
        await updatePermission(editingPerm.id, formData);
      } else {
        await createPermission(formData);
      }
      setIsModalOpen(false);
      await load();
    } catch (error) {
      alert(error?.message || 'Lỗi khi lưu quyền hạn');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa quyền này?')) return;
    try {
      await deletePermission(id);
      await load();
    } catch (error) {
      alert(error?.message || 'Lỗi khi xóa quyền hạn');
    }
  };

  const openModal = (perm = null) => {
    setEditingPerm(perm);
    if (perm) {
      setFormData({
        name: perm.name || '',
        code: perm.code || '',
        description: perm.description || ''
      });
    } else {
      setFormData({ name: '', code: '', description: '' });
    }
    setIsModalOpen(true);
  };

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black uppercase">Quản lý Quyền hạn</h1>
          <p className="mt-1 text-sm text-zinc-500">Danh sách quyền truy cập trong hệ thống.</p>
        </div>
        <button onClick={() => openModal()} className="rounded-xl bg-brand-orange px-4 py-2 text-sm font-bold text-zinc-950 hover:bg-brand-orange/90">
          + Thêm Quyền
        </button>
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!permissions.length} emptyMessage="Chưa có quyền hạn nào">
        <div className="overflow-x-auto rounded-2xl border border-zinc-800">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-zinc-900 text-xs uppercase text-zinc-500">
              <tr>
                <th className="p-4">Tên quyền</th>
                <th className="p-4">Mã quyền (Code)</th>
                <th className="p-4">Mô tả</th>
                <th className="p-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800">
              {permissions.map(perm => (
                <tr key={perm.id} className="hover:bg-zinc-900/50">
                  <td className="p-4 font-bold text-white">{perm.name}</td>
                  <td className="p-4 text-brand-orange">{perm.code}</td>
                  <td className="p-4 text-zinc-400">{perm.description || '—'}</td>
                  <td className="p-4 text-right space-x-3">
                    <button onClick={() => openModal(perm)} className="text-xs font-bold text-zinc-400 hover:text-white">Sửa</button>
                    <button onClick={() => handleDelete(perm.id)} className="text-xs font-bold text-red-400 hover:text-red-300">Xóa</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncState>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <form onSubmit={handleSubmit} className="w-full max-w-md rounded-2xl border border-zinc-800 bg-zinc-900 p-6 space-y-4">
            <div className="flex items-center justify-between mb-2">
              <h2 className="text-xl font-bold text-white">{editingPerm ? 'Sửa Quyền' : 'Thêm Quyền'}</h2>
            </div>
            <div>
              <label className="text-xs font-bold text-zinc-400">Tên quyền</label>
              <Input value={formData.name} onChange={e => setFormData({ ...formData, name: e.target.value })} required placeholder="VD: Quản lý vé" />
            </div>
            <div>
              <label className="text-xs font-bold text-zinc-400">Mã quyền</label>
              <Input value={formData.code} onChange={e => setFormData({ ...formData, code: e.target.value })} required placeholder="VD: PERM_MANAGE_TICKETS" disabled={!!editingPerm} />
              {editingPerm && <p className="text-[10px] text-red-400 mt-1">Không thể đổi mã quyền sau khi tạo.</p>}
            </div>
            <div>
              <label className="text-xs font-bold text-zinc-400">Mô tả</label>
              <Input value={formData.description} onChange={e => setFormData({ ...formData, description: e.target.value })} placeholder="Mô tả chức năng của quyền" />
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
