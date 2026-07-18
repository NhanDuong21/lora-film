import { useState, useEffect } from 'react';
import { useOutletContext } from 'react-router-dom';
import useAdminSeatTypes from '../hooks/useAdminSeatTypes';
import { PlusCircle, Search, Edit } from 'lucide-react';
import { LoadingState, ErrorState, EmptyState } from '@/components/common/ui/uiKit';

export default function AdminSeatTypePage() {
  const { triggerToast } = useOutletContext() || {};
  const { seatTypes, isLoading, error, fetchSeatTypes, createSeatType, updateSeatType } = useAdminSeatTypes(triggerToast);

  const [searchTerm, setSearchTerm] = useState('');
  const [editingId, setEditingId] = useState(null);
  
  const [formData, setFormData] = useState({
    code: '',
    name: '',
    description: '',
    status: 'ACTIVE'
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);

  useEffect(() => {
     
    fetchSeatTypes();
  }, [fetchSeatTypes]);

  const filteredSeatTypes = seatTypes.filter(st => 
    st.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
    st.code.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const openCreateForm = () => {
    setFormData({ code: '', name: '', description: '', status: 'ACTIVE' });
    setEditingId(null);
    setIsFormOpen(true);
  };

  const openEditForm = (st) => {
    setFormData({
      code: st.code,
      name: st.name,
      description: st.description || '',
      status: st.status || 'ACTIVE'
    });
    setEditingId(st.publicId);
    setIsFormOpen(true);
  };

  const closeForm = () => {
    setIsFormOpen(false);
    setEditingId(null);
  };

  const handleFormSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    let success;
    if (editingId) {
      success = await updateSeatType(editingId, formData);
    } else {
      success = await createSeatType(formData);
    }
    setIsSubmitting(false);
    if (success) {
      closeForm();
    }
  };

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-[400px] bg-zinc-950 text-white space-y-6 font-sans">
      <div className="flex flex-col md:flex-row md:justify-between md:items-center border-b border-zinc-900 pb-4 gap-4">
        <div>
          <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-zinc-50">QUẢN LÝ LOẠI GHẾ</h1>
          <p className="text-xs text-zinc-400 mt-1 uppercase tracking-wider">Thiết lập các loại ghế trong rạp</p>
        </div>
        <button
          onClick={openCreateForm}
          className="flex items-center justify-center gap-2 bg-brand-coral hover:bg-opacity-95 text-white text-xs font-black py-2.5 px-4 rounded-xl uppercase tracking-wider transition-all shadow-lg shadow-brand-coral/10 hover:shadow-brand-coral/20 border border-brand-coral/10"
        >
          <PlusCircle className="w-4 h-4" />
          <span>Thêm Loại Ghế</span>
        </button>
      </div>

      <div className="flex items-center bg-zinc-900/20 border border-zinc-900 p-4 rounded-2xl">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" />
          <input
            type="text"
            placeholder="Tìm theo mã hoặc tên..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-zinc-950 border border-zinc-800 rounded-xl pl-11 pr-4 py-2.5 text-xs font-semibold text-zinc-200 outline-none focus:border-brand-coral transition-colors"
          />
        </div>
      </div>

      <div className="bg-zinc-900/20 border border-zinc-900 rounded-3xl overflow-hidden shadow-2xl relative">
        {isLoading && <LoadingState message="Đang tải loại ghế..." />}
        {!isLoading && error && <ErrorState message={error} onRetry={fetchSeatTypes} />}
        
        {!isLoading && !error && filteredSeatTypes.length === 0 ? (
          <EmptyState message="Không tìm thấy loại ghế nào" onAction={openCreateForm} actionLabel="Thêm mới" />
        ) : (
          !isLoading && !error && (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-zinc-900 text-[10px] font-black text-zinc-500 uppercase tracking-wider bg-zinc-950/40">
                    <th className="py-4 px-6">Mã (Code)</th>
                    <th className="py-4 px-6">Tên</th>
                    <th className="py-4 px-6">Mô tả</th>
                    <th className="py-4 px-6">Trạng thái</th>
                    <th className="py-4 px-6 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-900/60 text-xs font-semibold">
                  {filteredSeatTypes.map((st) => (
                    <tr key={st.publicId} className="hover:bg-zinc-900/10 transition-colors">
                      <td className="py-4 px-6 text-brand-coral font-mono">{st.code}</td>
                      <td className="py-4 px-6 font-bold">{st.name}</td>
                      <td className="py-4 px-6 text-zinc-400">{st.description || '-'}</td>
                      <td className="py-4 px-6">
                        {st.status === 'ACTIVE' 
                          ? <span className="text-emerald-400 bg-emerald-400/10 px-2 py-1 rounded text-[10px]">Đang hoạt động</span>
                          : <span className="text-red-400 bg-red-400/10 px-2 py-1 rounded text-[10px]">Ngừng hoạt động</span>}
                      </td>
                      <td className="py-4 px-6 text-right">
                        <button
                          onClick={() => openEditForm(st)}
                          className="p-2 bg-zinc-950 border border-zinc-800 hover:border-amber-500/50 text-zinc-400 hover:text-amber-500 rounded-xl transition-all"
                        >
                          <Edit className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        )}
      </div>

      {isFormOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-zinc-950 border border-zinc-800 p-6 rounded-2xl w-full max-w-md shadow-2xl">
            <h2 className="text-lg font-black uppercase mb-4">{editingId ? 'Cập nhật Loại Ghế' : 'Thêm Loại Ghế'}</h2>
            <form onSubmit={handleFormSubmit} className="space-y-4">
              <div>
                <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-1">Mã (Code)</label>
                <input
                  type="text"
                  required
                  value={formData.code}
                  onChange={(e) => setFormData({...formData, code: e.target.value.toUpperCase()})}
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs focus:border-brand-coral outline-none text-white uppercase"
                  placeholder="Vd: VIP, STANDARD"
                  disabled={!!editingId}
                />
                {editingId && <p className="text-[10px] text-amber-500 mt-1">Không thể sửa mã sau khi tạo</p>}
              </div>
              <div>
                <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-1">Tên hiển thị</label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs focus:border-brand-coral outline-none text-white"
                  placeholder="Vd: Ghế VIP"
                />
              </div>
              <div>
                <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-1">Mô tả</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({...formData, description: e.target.value})}
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs focus:border-brand-coral outline-none text-white"
                  rows={2}
                />
              </div>
              <div>
                <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-1">Trạng thái</label>
                <select
                  value={formData.status}
                  onChange={(e) => setFormData({...formData, status: e.target.value})}
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs focus:border-brand-coral outline-none text-white cursor-pointer"
                >
                  <option value="ACTIVE">Hoạt động</option>
                  <option value="INACTIVE">Ngừng hoạt động</option>
                </select>
              </div>
              
              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={closeForm}
                  className="flex-1 bg-zinc-900 hover:bg-zinc-800 text-zinc-300 font-bold py-2.5 rounded-xl uppercase tracking-wider text-xs transition-colors"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="flex-1 bg-brand-coral hover:bg-opacity-90 text-white font-bold py-2.5 rounded-xl uppercase tracking-wider text-xs transition-colors disabled:opacity-50"
                >
                  {isSubmitting ? 'Đang lưu...' : 'Lưu lại'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
