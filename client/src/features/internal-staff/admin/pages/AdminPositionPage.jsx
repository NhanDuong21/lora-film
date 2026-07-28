import { useCallback, useEffect, useState } from 'react';
import { getPositions, createPosition, updatePosition, deletePosition } from '../services/userAdminService';
import { AsyncState, Input } from '@/components/common/ui/uiKit';

export default function AdminPositionPage() {
  const [positions, setPositions] = useState([]);
  const [state, setState] = useState({ loading: true, error: '' });
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingPos, setEditingPos] = useState(null);
  const [formData, setFormData] = useState({ code: '', name: '', description: '' });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const data = await getPositions();
      setPositions(data || []);
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải vị trí.' });
    }
  }, []);
  useEffect(() => { load(); }, [load]);

  const handleSave = async (e) => {
    e.preventDefault();
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
    if (confirm('Bạn có chắc chắn muốn xóa vị trí này?')) {
      try {
        await deletePosition(id);
        await load();
      } catch (error) {
        alert(error?.message || 'Lỗi khi xóa');
      }
    }
  };

  const openModal = (pos = null) => {
    setEditingPos(pos);
    setFormData(pos ? { code: pos.code, name: pos.name, description: pos.description } : { code: '', name: '', description: '' });
    setIsModalOpen(true);
  };

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-black uppercase">Quản lý Vị trí</h1>
          <p className="mt-1 text-sm text-zinc-500">Danh sách các vị trí công việc.</p>
        </div>
        <button onClick={() => openModal()} className="rounded-lg bg-orange-600 px-4 py-2 font-bold hover:bg-orange-700">
          + Thêm vị trí
        </button>
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!positions.length} emptyMessage="Chưa có vị trí nào">
        <div className="overflow-x-auto rounded-2xl border border-zinc-800">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-zinc-900 text-xs uppercase text-zinc-500">
              <tr>
                <th className="p-4">Mã Vị trí</th>
                <th className="p-4">Tên Vị trí</th>
                <th className="p-4">Mô tả</th>
                <th className="p-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800">
              {positions.map(pos => (
                <tr key={pos.id}>
                  <td className="p-4 font-bold">{pos.code}</td>
                  <td className="p-4">{pos.name}</td>
                  <td className="p-4 text-zinc-400">{pos.description}</td>
                  <td className="space-x-3 p-4 text-right">
                    <button onClick={() => openModal(pos)} className="text-xs font-bold text-amber-400 hover:underline">Sửa</button>
                    <button onClick={() => handleDelete(pos.id)} className="text-xs font-bold text-red-400 hover:underline">Xóa</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncState>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <form onSubmit={handleSave} className="w-full max-w-md rounded-2xl border border-zinc-800 bg-zinc-900 p-6 space-y-4">
            <h2 className="text-xl font-bold">{editingPos ? 'Sửa vị trí' : 'Thêm vị trí'}</h2>
            <Input required placeholder="Mã vị trí" value={formData.code} onChange={e => setFormData({ ...formData, code: e.target.value })} />
            <Input required placeholder="Tên vị trí" value={formData.name} onChange={e => setFormData({ ...formData, name: e.target.value })} />
            <Input placeholder="Mô tả" value={formData.description || ''} onChange={e => setFormData({ ...formData, description: e.target.value })} />
            <div className="flex justify-end gap-2 pt-2">
              <button type="button" onClick={() => setIsModalOpen(false)} className="rounded-lg border border-zinc-700 px-4 py-2 hover:bg-zinc-800">Hủy</button>
              <button type="submit" className="rounded-lg bg-orange-600 px-4 py-2 font-bold hover:bg-orange-700">Lưu</button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}
