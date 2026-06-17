import { useState, useMemo } from 'react';
import { Search, Plus, Edit3, Trash2, X, CupSoda, Check, EyeOff, Eye } from 'lucide-react';

export default function AdminConcessionInventory({ concessions, updateConcessionsState, triggerToast }) {
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);

  // Form states
  const [formName, setFormName] = useState('');
  const [formDetails, setFormDetails] = useState('');
  const [formPrice, setFormPrice] = useState(0);
  const [formActive, setFormActive] = useState(true);

  // Filter concessions
  const filteredConcessions = useMemo(() => {
    if (!concessions) return [];
    return concessions.filter(item => 
      item?.name?.toLowerCase()?.includes(search.toLowerCase()) || 
      item?.details?.toLowerCase()?.includes(search.toLowerCase())
    );
  }, [concessions, search]);

  const openAddModal = () => {
    setEditingItem(null);
    setFormName('');
    setFormDetails('');
    setFormPrice(0);
    setFormActive(true);
    setModalOpen(true);
  };

  const openEditModal = (item) => {
    setEditingItem(item);
    setFormName(item.name ?? '');
    setFormDetails(item.details ?? '');
    setFormPrice(item.price ?? 0);
    setFormActive(item.active !== false); // default to true
    setModalOpen(true);
  };

  const handleSaveItem = (e) => {
    e.preventDefault();
    if (!formName.trim() || !formDetails.trim()) {
      triggerToast('Vui lòng nhập đầy đủ thông tin bắt buộc!', 'error');
      return;
    }

    if (editingItem) {
      updateConcessionsState(prev => 
        prev.map(item => item.id === editingItem.id 
          ? { 
              ...item, 
              name: formName, 
              details: formDetails, 
              price: parseInt(formPrice, 10) || 0,
              active: formActive
            }
          : item
        )
      );
      triggerToast('Cập nhật sản phẩm thành công!', 'success');
    } else {
      const newItem = {
        id: Date.now(),
        name: formName,
        details: formDetails,
        price: parseInt(formPrice, 10) || 0,
        salesCount: 0,
        active: formActive
      };
      updateConcessionsState(prev => [...prev, newItem]);
      triggerToast('Thêm sản phẩm mới thành công!', 'success');
    }
    setModalOpen(false);
  };

  const handleDeleteItem = (id) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa sản phẩm này khỏi thực đơn?')) {
      updateConcessionsState(prev => prev.filter(item => item.id !== id));
      triggerToast('Đã xóa sản phẩm thành công!', 'success');
    }
  };

  const toggleItemActive = (item) => {
    const nextState = !(item.active !== false);
    updateConcessionsState(prev => 
      prev.map(i => i.id === item.id ? { ...i, active: nextState } : i)
    );
    triggerToast(`Đã ${nextState ? 'kích hoạt' : 'ẩn'} sản phẩm thành công!`, 'success');
  };

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-zinc-900 pb-4">
        <div>
          <p className="text-xs text-zinc-500 mt-1 uppercase tracking-wider">Quản lý danh sách combo thực đơn, điều chỉnh giá bán và trạng thái kinh doanh tại quầy</p>
        </div>
      </div>

      <div className="flex flex-col sm:flex-row justify-between items-stretch sm:items-center gap-4">
        <div className="relative flex-1 max-w-md">
          <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-zinc-500">
            <Search className="w-4 h-4" />
          </span>
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Tìm kiếm sản phẩm, combo..."
            className="w-full bg-zinc-950 border border-zinc-900 rounded-xl py-2.5 pl-9 pr-4 text-xs text-zinc-100 focus:outline-none focus:border-amber-500 transition-colors"
          />
        </div>
        <button
          onClick={openAddModal}
          className="bg-gradient-to-r from-orange-400 to-amber-500 text-zinc-950 font-black px-4 py-2.5 rounded-xl text-xs flex items-center gap-2 transition-transform hover:scale-[1.02] cursor-pointer outline-none uppercase tracking-wider"
        >
          <Plus className="w-4 h-4 shrink-0" />
          <span>THÊM SẢN PHẨM</span>
        </button>
      </div>

      {/* Grid Matrix Layout */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredConcessions && filteredConcessions.length > 0 ? (
          filteredConcessions.map((item, index) => {
            const isActive = item.active !== false;
            let displayName = item.name ?? 'Sản phẩm dịch vụ';
            if (item.name === 'Popcorn Ngot') displayName = 'Popcorn Ngọt';

            return (
              <div 
                key={item.id ?? index} 
                className={`bg-zinc-900/60 border rounded-2xl p-5 flex flex-col justify-between shadow-xl transition-all duration-300 relative ${
                  isActive ? 'border-zinc-800' : 'border-zinc-900 opacity-60'
                }`}
              >
                <div>
                  <div className="flex justify-between items-start mb-3">
                    <span className={`px-2 py-0.5 rounded text-[9px] font-black uppercase border ${
                      isActive 
                        ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' 
                        : 'bg-zinc-800 text-zinc-500 border-zinc-700/50'
                    }`}>
                      {isActive ? 'ĐANG BÁN' : 'TẠM ẨN'}
                    </span>
                    <span className="text-[10px] text-zinc-500 font-mono">Đã bán: {item.salesCount ?? 0}</span>
                  </div>

                  <h4 className="font-black text-zinc-50 text-sm">{displayName}</h4>
                  <p className="text-[11px] text-zinc-400 mt-1.5 leading-relaxed min-h-[32px]">{item.details ?? ''}</p>
                </div>

                <div className="mt-6 pt-4 border-t border-zinc-800/60 flex justify-between items-center">
                  <span className="font-mono text-amber-500 font-black text-sm">
                    {(item.price ?? 0).toLocaleString('vi-VN')} đ
                  </span>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => toggleItemActive(item)}
                      className={`p-1.5 rounded-lg border transition-colors focus:outline-none ${
                        isActive 
                          ? 'border-zinc-800 text-zinc-400 hover:text-amber-500 hover:border-amber-500/35' 
                          : 'border-zinc-800 text-zinc-500 hover:text-emerald-400 hover:border-emerald-500/35'
                      }`}
                      title={isActive ? 'Tạm ẩn sản phẩm' : 'Hiện sản phẩm'}
                    >
                      {isActive ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                    </button>
                    <button
                      onClick={() => openEditModal(item)}
                      className="p-1.5 rounded-lg border border-zinc-800 text-zinc-400 hover:text-amber-500 hover:border-amber-500/35 transition-colors focus:outline-none"
                      title="Sửa sản phẩm"
                    >
                      <Edit3 className="w-3.5 h-3.5" />
                    </button>
                    <button
                      onClick={() => handleDeleteItem(item.id)}
                      className="p-1.5 rounded-lg border border-zinc-800 text-zinc-500 hover:text-red-500 hover:border-red-500/35 transition-colors focus:outline-none"
                      title="Xóa sản phẩm"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })
        ) : (
          <div className="col-span-full text-center p-12 text-zinc-500 font-semibold bg-zinc-900/30 rounded-2xl border border-zinc-800/80">
            Không tìm thấy sản phẩm nào trong thực đơn.
          </div>
        )}
      </div>

      {/* Floating Glassmorphic Input Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-zinc-900 border border-zinc-800 p-6 rounded-2xl max-w-md w-full shadow-2xl animate-fade-in relative">
            <button
              onClick={() => setModalOpen(false)}
              className="absolute top-4 right-4 text-zinc-500 hover:text-zinc-300 transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
            <div className="flex items-center gap-2 mb-6">
              <CupSoda className="w-5 h-5 text-amber-500" />
              <h4 className="text-sm font-bold text-zinc-100 uppercase tracking-wider">
                {editingItem ? 'Cập Nhật Thông Tin Sản Phẩm' : 'Thêm Sản Phẩm Mới'}
              </h4>
            </div>

            <form onSubmit={handleSaveItem} className="space-y-4">
              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-zinc-500 uppercase tracking-wider block">Tên Sản Phẩm / Combo</label>
                <input
                  type="text"
                  required
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-xl py-3 px-4 text-xs text-zinc-100 focus:outline-none focus:border-amber-500 transition-colors"
                  placeholder="Ví dụ: Combo Premium, Bắp Phô Mai..."
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-zinc-500 uppercase tracking-wider block">Chi Tiết / Mô Tả Sản Phẩm</label>
                <textarea
                  required
                  value={formDetails}
                  onChange={(e) => setFormDetails(e.target.value)}
                  rows="3"
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-xl py-3 px-4 text-xs text-zinc-100 focus:outline-none focus:border-amber-500 transition-colors resize-none"
                  placeholder="Ví dụ: 1 bắp ngọt lớn + 2 ly coca cỡ lớn..."
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-zinc-500 uppercase tracking-wider block">Giá Bán (VNĐ)</label>
                <input
                  type="number"
                  min="0"
                  step="1000"
                  required
                  value={formPrice}
                  onChange={(e) => setFormPrice(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-xl py-3 px-4 text-xs text-zinc-100 focus:outline-none focus:border-amber-500 transition-colors font-mono"
                />
              </div>

              <div className="flex items-center justify-between bg-zinc-950/40 p-4 rounded-xl border border-zinc-800/50 mt-1.5">
                <div>
                  <label className="text-zinc-300 text-xs font-bold block">Trạng Thái Kinh Doanh</label>
                  <span className="text-[10px] text-zinc-500 block mt-0.5">Cho phép đặt bán trực tiếp tại quầy hoặc ứng dụng</span>
                </div>
                <button
                  type="button"
                  onClick={() => setFormActive(!formActive)}
                  className={`w-11 h-6 flex items-center rounded-full p-1 cursor-pointer transition-colors duration-300 outline-none ${
                    formActive ? 'bg-amber-500' : 'bg-zinc-800'
                  }`}
                >
                  <div
                    className={`bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-300 ${
                      formActive ? 'translate-x-5' : 'translate-x-0'
                    }`}
                  />
                </button>
              </div>

              <div className="pt-4 flex items-center justify-end gap-3 border-t border-zinc-800/60">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="bg-zinc-850 hover:bg-zinc-800 text-zinc-300 font-bold px-4 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-colors outline-none"
                >
                  HỦY BỎ
                </button>
                <button
                  type="submit"
                  className="bg-gradient-to-r from-orange-400 to-amber-500 text-zinc-950 font-black px-5 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-transform hover:scale-[1.02] cursor-pointer outline-none"
                >
                  XÁC NHẬN
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
