import { useState, useEffect, useCallback } from 'react';
import { Search, Plus, Edit2, Trash2, X, Check, AlertCircle, Coffee } from 'lucide-react';
import apiClient from "@/services/apiClient";

export default function AdminConcessionInventoryPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Search & Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedType, setSelectedType] = useState('ALL');

  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  
  // Form State
  const [formState, setFormState] = useState({
    code: '',
    name: '',
    type: 'FOOD',
    price: 0,
    imageUrl: '',
    active: true,
    sellable: true
  });

  const fetchItems = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get('/api/admin/foods');
      // Format backend response
      setItems(response.data.data || []);
    } catch (err) {
      setError(err.response?.data?.message || err.message || "Không thể tải danh sách bắp nước.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchItems();
  }, [fetchItems]);

  const showSuccess = (msg) => {
    setSuccessMessage(msg);
    setTimeout(() => setSuccessMessage(null), 3000);
  };

  const handleOpenAdd = () => {
    setEditingItem(null);
    setFormState({
      code: '',
      name: '',
      type: 'FOOD',
      price: 0,
      imageUrl: '',
      active: true,
      sellable: true
    });
    setIsModalOpen(true);
  };

  const handleOpenEdit = (item) => {
    setEditingItem(item);
    setFormState({
      code: item.code || '',
      name: item.name || '',
      type: item.type || 'FOOD',
      price: item.price || 0,
      imageUrl: item.imageUrl || '',
      active: item.active ?? true,
      sellable: item.sellable ?? true
    });
    setIsModalOpen(true);
  };

  const handleFormSubmit = async (e) => {
    e.preventDefault();
    if (!formState.code || !formState.name || formState.price <= 0) {
      alert("Vui lòng điền đầy đủ Mã, Tên và Giá bán hợp lệ!");
      return;
    }

    try {
      if (editingItem) {
        // Update product
        await apiClient.put(`/api/admin/foods/${editingItem.id}`, {
          ...editingItem,
          ...formState
        });
        showSuccess("Cập nhật bắp nước thành công!");
      } else {
        // Add product
        await apiClient.post('/api/admin/foods', {
          ...formState,
          deleted: false,
          disabled: false
        });
        showSuccess("Thêm bắp nước mới thành công!");
      }
      setIsModalOpen(false);
      fetchItems();
    } catch (err) {
      alert("Lỗi lưu sản phẩm: " + (err.response?.data?.message || err.message));
    }
  };

  const handleDelete = async (id) => {
    if (!confirm("Bạn có chắc chắn muốn ngừng kinh doanh / xóa sản phẩm này?")) return;

    try {
      await apiClient.delete(`/api/admin/foods/${id}`);
      showSuccess("Đã xóa/ngừng kinh doanh sản phẩm!");
      fetchItems();
    } catch (err) {
      alert("Lỗi khi xóa: " + (err.response?.data?.message || err.message));
    }
  };

  // Filtered Items
  const filteredItems = items.filter(item => {
    if (item.deleted) return false;
    const matchesSearch = item.name?.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          item.code?.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesType = selectedType === 'ALL' || item.type === selectedType;
    return matchesSearch && matchesType;
  });

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-[400px] bg-zinc-950 text-white space-y-6">
      
      {/* Header section */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center border-b border-zinc-800 pb-6 gap-4">
        <div>
          <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">DANH MỤC BẮP NƯỚC</h1>
          <p className="text-xs text-zinc-400 mt-1">Quản lý kho sản phẩm bán đi kèm vé cho khách hàng</p>
        </div>
        <button
          onClick={handleOpenAdd}
          className="flex items-center gap-2 bg-gradient-to-r from-orange-500 to-amber-500 hover:from-orange-600 hover:to-amber-600 text-white font-bold px-5 py-3 rounded-xl text-xs uppercase tracking-wider transition-all shadow-lg shadow-orange-500/20 active:scale-95"
        >
          <Plus size={16} /> Thêm bắp nước
        </button>
      </div>

      {/* Success Notification */}
      {successMessage && (
        <div className="bg-emerald-950/80 border border-emerald-500/30 text-emerald-200 px-5 py-4 rounded-2xl flex items-center gap-3 text-sm animate-pulse">
          <Check size={18} className="text-emerald-400" />
          <span>{successMessage}</span>
        </div>
      )}

      {/* Filter and search bar */}
      <div className="flex flex-col md:flex-row gap-4 justify-between items-center bg-zinc-900/50 p-4 rounded-2xl border border-zinc-800/40">
        <div className="relative w-full md:w-96">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-500 w-4 h-4" />
          <input
            type="text"
            placeholder="Tìm kiếm bắp nước (tên hoặc mã)..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 bg-zinc-950 border border-zinc-800 rounded-xl text-sm focus:outline-none focus:border-amber-500 text-white transition-all"
          />
        </div>
        <div className="flex gap-2 w-full md:w-auto overflow-x-auto pb-1 md:pb-0">
          {['ALL', 'FOOD', 'DRINK', 'COMBO'].map(type => (
            <button
              key={type}
              onClick={() => setSelectedType(type)}
              className={`px-4 py-2 rounded-xl text-xs font-black uppercase tracking-wider transition-all ${
                selectedType === type
                  ? 'bg-amber-500 text-black shadow-lg shadow-amber-500/20'
                  : 'bg-zinc-900 text-zinc-400 hover:text-white hover:bg-zinc-800/60'
              }`}
            >
              {type === 'ALL' ? 'Tất cả' : type}
            </button>
          ))}
        </div>
      </div>

      {/* Table grid */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-16 gap-3">
          <div className="w-10 h-10 border-4 border-amber-500 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-zinc-500 text-xs">Đang tải danh sách bắp nước...</p>
        </div>
      ) : error ? (
        <div className="bg-red-950/50 border border-red-500/30 text-red-200 p-5 rounded-2xl flex items-center gap-3 text-sm">
          <AlertCircle size={18} className="text-red-400" />
          <span>{error}</span>
        </div>
      ) : filteredItems.length === 0 ? (
        <div className="text-center py-16 bg-zinc-900/20 rounded-3xl border border-dashed border-zinc-850">
          <Coffee size={40} className="mx-auto text-zinc-600 mb-3" />
          <p className="text-zinc-400 text-sm font-medium">Không tìm thấy sản phẩm bắp nước nào</p>
          <p className="text-zinc-650 text-xs mt-1">Hãy thử đổi bộ lọc hoặc thêm sản phẩm mới</p>
        </div>
      ) : (
        <div className="bg-zinc-900/60 border border-zinc-800/60 rounded-3xl overflow-hidden shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-zinc-800 bg-zinc-900/30">
                  <th className="px-6 py-4 text-xs font-black uppercase tracking-wider text-zinc-400">Hình ảnh</th>
                  <th className="px-6 py-4 text-xs font-black uppercase tracking-wider text-zinc-400">Mã sản phẩm</th>
                  <th className="px-6 py-4 text-xs font-black uppercase tracking-wider text-zinc-400">Tên sản phẩm</th>
                  <th className="px-6 py-4 text-xs font-black uppercase tracking-wider text-zinc-400">Phân loại</th>
                  <th className="px-6 py-4 text-xs font-black uppercase tracking-wider text-zinc-400 text-right">Đơn giá</th>
                  <th className="px-6 py-4 text-xs font-black uppercase tracking-wider text-zinc-400 text-center">Đang bán</th>
                  <th className="px-6 py-4 text-xs font-black uppercase tracking-wider text-zinc-400 text-center">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-850">
                {filteredItems.map(item => (
                  <tr key={item.id} className="hover:bg-zinc-900/40 transition-colors">
                    <td className="px-6 py-4">
                      <div className="w-12 h-12 rounded-xl bg-zinc-950 border border-zinc-850 flex items-center justify-center overflow-hidden">
                        {item.imageUrl ? (
                          <img
                            src={item.imageUrl.startsWith('http') ? item.imageUrl : `/images/foods/${item.imageUrl}`}
                            alt={item.name}
                            onError={(e) => {
                              e.target.onerror = null;
                              e.target.src = 'https://images.unsplash.com/photo-1578242187038-04f7620a273b?auto=format&fit=crop&q=80&w=120';
                            }}
                            className="w-full h-full object-cover"
                          />
                        ) : (
                          <Coffee className="text-zinc-600 w-5 h-5" />
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4 font-mono text-xs font-bold text-amber-500">{item.code}</td>
                    <td className="px-6 py-4 font-semibold text-sm">{item.name}</td>
                    <td className="px-6 py-4">
                      <span className={`px-2.5 py-1 rounded-lg text-[10px] font-black uppercase tracking-wider ${
                        item.type === 'COMBO' 
                          ? 'bg-purple-500/10 border border-purple-500/30 text-purple-400'
                          : item.type === 'DRINK'
                            ? 'bg-blue-500/10 border border-blue-500/30 text-blue-400'
                            : 'bg-amber-500/10 border border-amber-500/30 text-amber-400'
                      }`}>
                        {item.type}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right font-black text-sm text-zinc-100">
                      {item.price?.toLocaleString()}đ
                    </td>
                    <td className="px-6 py-4 text-center">
                      <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-wider ${
                        item.active && item.sellable
                          ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                          : 'bg-zinc-800 text-zinc-500'
                      }`}>
                        <span className={`w-1.5 h-1.5 rounded-full ${item.active && item.sellable ? 'bg-emerald-400' : 'bg-zinc-600'}`}></span>
                        {item.active && item.sellable ? 'Đang bán' : 'Tạm dừng'}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center justify-center gap-3">
                        <button
                          onClick={() => handleOpenEdit(item)}
                          className="p-2 text-zinc-400 hover:text-amber-500 bg-zinc-950 border border-zinc-850 hover:border-amber-500/40 rounded-xl transition-all cursor-pointer"
                          title="Sửa sản phẩm"
                        >
                          <Edit2 size={14} />
                        </button>
                        <button
                          onClick={() => handleDelete(item.id)}
                          className="p-2 text-zinc-400 hover:text-red-500 bg-zinc-950 border border-zinc-850 hover:border-red-500/40 rounded-xl transition-all cursor-pointer"
                          title="Ngừng kinh doanh"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Add/Edit Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div className="w-full max-w-lg bg-zinc-900 border border-zinc-800 rounded-3xl overflow-hidden shadow-2xl animate-in fade-in zoom-in-95 duration-150">
            {/* Modal Header */}
            <div className="flex justify-between items-center p-6 border-b border-zinc-800/80">
              <div>
                <h3 className="text-base font-black uppercase tracking-wider text-white">
                  {editingItem ? 'CẬP NHẬT SẢN PHẨM' : 'THÊM BẮP NƯỚC MỚI'}
                </h3>
                <p className="text-xs text-zinc-500 mt-1">Thông tin cơ bản bắp nước/combo</p>
              </div>
              <button
                onClick={() => setIsModalOpen(false)}
                className="p-2 hover:bg-zinc-850 text-zinc-400 hover:text-white rounded-xl transition-colors cursor-pointer"
              >
                <X size={18} />
              </button>
            </div>

            {/* Modal Form Body */}
            <form onSubmit={handleFormSubmit} className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-[10px] font-black uppercase tracking-wider text-zinc-400 mb-1.5">Mã sản phẩm *</label>
                  <input
                    type="text"
                    required
                    disabled={!!editingItem}
                    placeholder="Ví dụ: POP_L"
                    value={formState.code}
                    onChange={(e) => setFormState(prev => ({ ...prev, code: e.target.value.toUpperCase() }))}
                    className="w-full px-4 py-2.5 bg-zinc-950 border border-zinc-800 rounded-xl text-sm focus:outline-none focus:border-amber-500 text-white disabled:opacity-55"
                  />
                </div>
                <div>
                  <label className="block text-[10px] font-black uppercase tracking-wider text-zinc-400 mb-1.5">Loại sản phẩm</label>
                  <select
                    value={formState.type}
                    onChange={(e) => setFormState(prev => ({ ...prev, type: e.target.value }))}
                    className="w-full px-4 py-2.5 bg-zinc-950 border border-zinc-800 rounded-xl text-sm focus:outline-none focus:border-amber-500 text-white cursor-pointer"
                  >
                    <option value="FOOD">FOOD (Đồ ăn)</option>
                    <option value="DRINK">DRINK (Thức uống)</option>
                    <option value="COMBO">COMBO (Trọn bộ)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-[10px] font-black uppercase tracking-wider text-zinc-400 mb-1.5">Tên sản phẩm *</label>
                <input
                  type="text"
                  required
                  placeholder="Ví dụ: Bắp rang lớn vị Caramel"
                  value={formState.name}
                  onChange={(e) => setFormState(prev => ({ ...prev, name: e.target.value }))}
                  className="w-full px-4 py-2.5 bg-zinc-950 border border-zinc-800 rounded-xl text-sm focus:outline-none focus:border-amber-500 text-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-[10px] font-black uppercase tracking-wider text-zinc-400 mb-1.5">Giá bán (VND) *</label>
                  <div className="relative">
                    <input
                      type="number"
                      required
                      min={1000}
                      placeholder="50000"
                      value={formState.price}
                      onChange={(e) => setFormState(prev => ({ ...prev, price: parseFloat(e.target.value) || 0 }))}
                      className="w-full pl-4 pr-10 py-2.5 bg-zinc-950 border border-zinc-800 rounded-xl text-sm focus:outline-none focus:border-amber-500 text-white"
                    />
                    <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-zinc-500 text-xs font-bold">đ</span>
                  </div>
                </div>
                <div>
                  <label className="block text-[10px] font-black uppercase tracking-wider text-zinc-400 mb-1.5">Ảnh sản phẩm (URL hoặc tên tệp)</label>
                  <input
                    type="text"
                    placeholder="popcorn_l.png"
                    value={formState.imageUrl}
                    onChange={(e) => setFormState(prev => ({ ...prev, imageUrl: e.target.value }))}
                    className="w-full px-4 py-2.5 bg-zinc-950 border border-zinc-800 rounded-xl text-sm focus:outline-none focus:border-amber-500 text-white"
                  />
                </div>
              </div>

              <div className="flex gap-6 pt-2">
                <label className="flex items-center gap-2 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={formState.active}
                    onChange={(e) => setFormState(prev => ({ ...prev, active: e.target.checked }))}
                    className="rounded bg-zinc-950 border-zinc-800 text-amber-500 focus:ring-0 focus:ring-offset-0 cursor-pointer"
                  />
                  <span className="text-xs text-zinc-300 font-bold uppercase tracking-wider">Kích hoạt sản phẩm</span>
                </label>
                <label className="flex items-center gap-2 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={formState.sellable}
                    onChange={(e) => setFormState(prev => ({ ...prev, sellable: e.target.checked }))}
                    className="rounded bg-zinc-950 border-zinc-800 text-amber-500 focus:ring-0 focus:ring-offset-0 cursor-pointer"
                  />
                  <span className="text-xs text-zinc-300 font-bold uppercase tracking-wider">Cho phép đặt bán</span>
                </label>
              </div>

              {/* Modal Actions */}
              <div className="flex justify-end gap-3 pt-6 border-t border-zinc-800/80">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2.5 rounded-xl text-xs font-black uppercase tracking-wider bg-zinc-800 text-zinc-300 hover:bg-zinc-700/80 transition-all cursor-pointer"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  className="px-5 py-2.5 rounded-xl text-xs font-black uppercase tracking-wider bg-amber-500 text-black hover:bg-amber-600 transition-all shadow-lg shadow-amber-500/10 cursor-pointer"
                >
                  Lưu lại
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
