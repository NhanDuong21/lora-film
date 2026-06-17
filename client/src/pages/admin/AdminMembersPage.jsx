import { useState, useMemo } from 'react';
import { Search, UserPlus, Edit3, Trash2, X, ShieldAlert } from 'lucide-react';

export default function AdminMembersView({ customers, updateCustomersState, triggerToast }) {
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingCustomer, setEditingCustomer] = useState(null);

  // Form states
  const [formName, setFormName] = useState('');
  const [formEmail, setFormEmail] = useState('');
  const [formTier, setFormTier] = useState('Standard');
  const [formPoints, setFormPoints] = useState(0);

  // Filter members based on name/email search
  const filteredCustomers = useMemo(() => {
    if (!customers) return [];
    return customers.filter(c => 
      c?.name?.toLowerCase()?.includes(search.toLowerCase()) || 
      c?.email?.toLowerCase()?.includes(search.toLowerCase())
    );
  }, [customers, search]);

  const openAddModal = () => {
    setEditingCustomer(null);
    setFormName('');
    setFormEmail('');
    setFormTier('Standard');
    setFormPoints(0);
    setModalOpen(true);
  };

  const openEditModal = (customer) => {
    setEditingCustomer(customer);
    setFormName(customer.name ?? '');
    setFormEmail(customer.email ?? '');
    setFormTier(customer.tier ?? 'Standard');
    setFormPoints(customer.points ?? 0);
    setModalOpen(true);
  };

  const handleSaveCustomer = (e) => {
    e.preventDefault();
    if (!formName.trim() || !formEmail.trim()) {
      triggerToast('Vui lòng nhập đầy đủ thông tin bắt buộc!', 'error');
      return;
    }

    if (editingCustomer) {
      // Edit mode
      updateCustomersState(prev => 
        prev.map(c => c.id === editingCustomer.id 
          ? { ...c, name: formName, email: formEmail, tier: formTier, points: parseInt(formPoints, 10) || 0 }
          : c
        )
      );
      triggerToast('Cập nhật thông tin hội viên thành công!', 'success');
    } else {
      // Add mode
      const newCustomer = {
        id: Date.now(),
        name: formName,
        email: formEmail,
        tier: formTier,
        points: parseInt(formPoints, 10) || 0,
        ticketsBought: 0
      };
      updateCustomersState(prev => [...prev, newCustomer]);
      triggerToast('Thêm hội viên mới thành công!', 'success');
    }
    setModalOpen(false);
  };

  const handleDeleteCustomer = (id) => {
    if (window.confirm('Bạn có chắc chắn muốn đình chỉ hoặc xóa hội viên này?')) {
      updateCustomersState(prev => prev.filter(c => c.id !== id));
      triggerToast('Đã đình chỉ tài khoản hội viên thành công!', 'success');
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-zinc-900 pb-4">
        <div>
          <p className="text-xs text-zinc-500 mt-1 uppercase tracking-wider">Quản lý cấp bậc, điểm tích lũy và thông tin cá nhân của hội viên LoraFilm</p>
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
            placeholder="Tìm kiếm theo tên hoặc email..."
            className="w-full bg-zinc-950 border border-zinc-900 rounded-xl py-2.5 pl-9 pr-4 text-xs text-zinc-100 focus:outline-none focus:border-amber-500 transition-colors"
          />
        </div>
        <button
          onClick={openAddModal}
          className="bg-gradient-to-r from-orange-400 to-amber-500 text-zinc-950 font-black px-4 py-2.5 rounded-xl text-xs flex items-center gap-2 transition-transform hover:scale-[1.02] cursor-pointer outline-none uppercase tracking-wider"
        >
          <UserPlus className="w-4 h-4 shrink-0" />
          <span>THÊM HỘI VIÊN</span>
        </button>
      </div>

      <div className="bg-zinc-900/40 backdrop-blur-md border border-zinc-900 rounded-2xl shadow-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-zinc-400">
            <thead className="bg-zinc-950/80 text-zinc-500 font-black uppercase tracking-wider border-b border-zinc-900">
              <tr>
                <th className="py-4 px-6">Tên Khách Hàng</th>
                <th className="py-4 px-6">Email</th>
                <th className="py-4 px-6">Hạng Thành Viên</th>
                <th className="py-4 px-6">Điểm Tích Lũy</th>
                <th className="py-4 px-6">Số Vé Đã Mua</th>
                <th className="py-4 px-6 text-center w-40">Hành Động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-900/60">
              {filteredCustomers && filteredCustomers.length > 0 ? (
                filteredCustomers.map((c, index) => {
                  const isVIP = c?.tier?.toUpperCase() === 'VIP';
                  return (
                    <tr key={c?.id ?? index} className="hover:bg-zinc-900/20 transition-colors">
                      <td className="py-4 px-6 font-bold text-zinc-100">{c?.name ?? 'Không rõ'}</td>
                      <td className="py-4 px-6 font-mono text-zinc-400">{c?.email ?? ''}</td>
                      <td className="py-4 px-6">
                        <span className={`px-2.5 py-0.5 rounded text-[10px] font-black uppercase border ${
                          isVIP
                            ? 'bg-amber-500/10 text-amber-500 border-amber-500/20' 
                            : 'bg-zinc-800 text-zinc-400 border-zinc-700/50'
                        }`}>
                          {isVIP ? 'VIP' : 'STANDARD'}
                        </span>
                      </td>
                      <td className="py-4 px-6 font-bold text-zinc-300">{c?.points ?? 0} điểm</td>
                      <td className="py-4 px-6 text-zinc-300 font-medium">{c?.ticketsBought ?? 0} vé</td>
                      <td className="py-4 px-6 text-center">
                        <div className="flex justify-center items-center gap-3">
                          <button
                            onClick={() => openEditModal(c)}
                            className="text-amber-500 hover:text-amber-400 transition-colors focus:outline-none p-1"
                            title="Sửa thông tin"
                          >
                            <Edit3 className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleDeleteCustomer(c.id)}
                            className="text-red-500 hover:text-red-400 transition-colors focus:outline-none p-1"
                            title="Đình chỉ hội viên"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan="6" className="py-12 text-center text-zinc-500 font-semibold">
                    Không tìm thấy hội viên nào phù hợp.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
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
              <ShieldAlert className="w-5 h-5 text-amber-500" />
              <h4 className="text-sm font-bold text-zinc-100 uppercase tracking-wider">
                {editingCustomer ? 'Cập Nhật Thông Tin Hội Viên' : 'Đăng Ký Hội Viên Mới'}
              </h4>
            </div>

            <form onSubmit={handleSaveCustomer} className="space-y-4">
              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-zinc-500 uppercase tracking-wider block">Họ Và Tên</label>
                <input
                  type="text"
                  required
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-xl py-3 px-4 text-xs text-zinc-100 focus:outline-none focus:border-amber-500 transition-colors"
                  placeholder="Nhập tên hội viên..."
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-zinc-500 uppercase tracking-wider block">Email</label>
                <input
                  type="email"
                  required
                  value={formEmail}
                  onChange={(e) => setFormEmail(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-xl py-3 px-4 text-xs text-zinc-100 focus:outline-none focus:border-amber-500 transition-colors"
                  placeholder="name@example.com"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-zinc-500 uppercase tracking-wider block">Hạng Thành Viên</label>
                <select
                  value={formTier}
                  onChange={(e) => setFormTier(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-xl py-3 px-4 text-xs text-zinc-100 focus:outline-none focus:border-amber-500 transition-colors"
                >
                  <option value="Standard">Standard</option>
                  <option value="VIP">VIP</option>
                </select>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-zinc-500 uppercase tracking-wider block">Điểm Tích Lũy</label>
                <input
                  type="number"
                  min="0"
                  value={formPoints}
                  onChange={(e) => setFormPoints(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-xl py-3 px-4 text-xs text-zinc-100 focus:outline-none focus:border-amber-500 transition-colors font-mono"
                />
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
