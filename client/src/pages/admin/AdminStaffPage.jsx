import { useState, useMemo } from 'react';
import { Search, UserPlus, Edit3, Trash2, X, Briefcase } from 'lucide-react';

export default function AdminStaffView({ employees, updateEmployeesState, triggerToast }) {
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingEmployee, setEditingEmployee] = useState(null);

  // Form states
  const [formName, setFormName] = useState('');
  const [formRole, setFormRole] = useState('Thu ngân');
  const [formEmail, setFormEmail] = useState('');
  const [formHourlyWage, setFormHourlyWage] = useState(25000);

  // Filter employees
  const filteredEmployees = useMemo(() => {
    if (!employees) return [];
    return employees.filter(emp => 
      emp?.name?.toLowerCase()?.includes(search.toLowerCase()) || 
      emp?.email?.toLowerCase()?.includes(search.toLowerCase())
    );
  }, [employees, search]);

  const openAddModal = () => {
    setEditingEmployee(null);
    setFormName('');
    setFormRole('Thu ngân');
    setFormEmail('');
    setFormHourlyWage(25000);
    setModalOpen(true);
  };

  const openEditModal = (emp) => {
    setEditingEmployee(emp);
    setFormName(emp.name ?? '');
    setFormRole(emp.role ?? 'Thu ngân');
    setFormEmail(emp.email ?? '');
    setFormHourlyWage(emp.hourlyWage ?? 25000);
    setModalOpen(true);
  };

  const handleSaveEmployee = (e) => {
    e.preventDefault();
    if (!formName.trim() || !formEmail.trim()) {
      triggerToast('Vui lòng nhập đầy đủ thông tin bắt buộc!', 'error');
      return;
    }

    if (editingEmployee) {
      updateEmployeesState(prev => 
        prev.map(emp => emp.id === editingEmployee.id 
          ? { 
              ...emp, 
              name: formName, 
              role: formRole, 
              email: formEmail, 
              hourlyWage: parseInt(formHourlyWage, 10) || 0 
            }
          : emp
        )
      );
      triggerToast('Cập nhật nhân viên thành công!', 'success');
    } else {
      const newEmployee = {
        id: Date.now(),
        name: formName,
        role: formRole,
        email: formEmail,
        hoursWorked: 0,
        hourlyWage: parseInt(formHourlyWage, 10) || 0,
        activeMultiplier: 1.0
      };
      updateEmployeesState(prev => [...prev, newEmployee]);
      triggerToast('Thêm nhân viên mới thành công!', 'success');
    }
    setModalOpen(false);
  };

  const handleDeleteEmployee = (id) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa tài khoản nhân viên này khỏi hệ thống?')) {
      updateEmployeesState(prev => prev.filter(emp => emp.id !== id));
      triggerToast('Đã xóa tài khoản nhân sự thành công!', 'success');
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-zinc-900 pb-4">
        <div>
          <p className="text-xs text-zinc-500 mt-1 uppercase tracking-wider">Danh sách nhân sự, phân quyền vai trò và thiết lập mức lương cơ bản toàn chuỗi</p>
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
            placeholder="Tìm kiếm nhân sự..."
            className="w-full bg-zinc-950 border border-zinc-900 rounded-xl py-2.5 pl-9 pr-4 text-xs text-zinc-100 focus:outline-none focus:border-amber-500 transition-colors"
          />
        </div>
        <button
          onClick={openAddModal}
          className="bg-gradient-to-r from-orange-400 to-amber-500 text-zinc-950 font-black px-4 py-2.5 rounded-xl text-xs flex items-center gap-2 transition-transform hover:scale-[1.02] cursor-pointer outline-none uppercase tracking-wider"
        >
          <UserPlus className="w-4 h-4 shrink-0" />
          <span>THÊM NHÂN VIÊN</span>
        </button>
      </div>

      <div className="bg-zinc-900/40 backdrop-blur-md border border-zinc-900 rounded-2xl shadow-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-zinc-400">
            <thead className="bg-zinc-950/80 text-zinc-500 font-black uppercase tracking-wider border-b border-zinc-900">
              <tr>
                <th className="py-4 px-6">Tên Nhân Viên</th>
                <th className="py-4 px-6">Chức Vụ</th>
                <th className="py-4 px-6">Email</th>
                <th className="py-4 px-6">Lương Cơ Bản / Giờ</th>
                <th className="py-4 px-6 text-center w-40">Hành Động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-900/60">
              {filteredEmployees && filteredEmployees.length > 0 ? (
                filteredEmployees.map((emp, index) => {
                  return (
                    <tr key={emp?.id ?? index} className="hover:bg-zinc-900/20 transition-colors">
                      <td className="py-4 px-6 font-bold text-zinc-100">{emp?.name ?? 'Không rõ'}</td>
                      <td className="py-4 px-6 font-bold text-zinc-300">
                        {emp?.role === 'Thu ngân' ? 'Thu Ngân' : emp?.role === 'Giám sát' ? 'Giám Sát' : emp?.role}
                      </td>
                      <td className="py-4 px-6 font-mono text-zinc-400">{emp?.email ?? ''}</td>
                      <td className="py-4 px-6 font-bold text-emerald-400 font-mono">
                        {(emp?.hourlyWage ?? 0).toLocaleString('vi-VN')} đ
                      </td>
                      <td className="py-4 px-6 text-center">
                        <div className="flex justify-center items-center gap-3">
                          <button
                            onClick={() => openEditModal(emp)}
                            className="text-amber-500 hover:text-amber-400 transition-colors focus:outline-none p-1"
                            title="Cập nhật chức vụ"
                          >
                            <Edit3 className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleDeleteEmployee(emp.id)}
                            className="text-red-500 hover:text-red-400 transition-colors focus:outline-none p-1"
                            title="Xóa tài khoản"
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
                  <td colSpan="5" className="py-12 text-center text-zinc-500 font-semibold">
                    Không tìm thấy nhân viên nào phù hợp.
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
              <Briefcase className="w-5 h-5 text-amber-500" />
              <h4 className="text-sm font-bold text-zinc-100 uppercase tracking-wider">
                {editingEmployee ? 'Cập Nhật Chức Vụ & Thông Tin' : 'Thêm Nhân Viên Mới'}
              </h4>
            </div>

            <form onSubmit={handleSaveEmployee} className="space-y-4">
              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-zinc-500 uppercase tracking-wider block">Họ Và Tên</label>
                <input
                  type="text"
                  required
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-xl py-3 px-4 text-xs text-zinc-100 focus:outline-none focus:border-amber-500 transition-colors"
                  placeholder="Nhập tên nhân viên..."
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
                  placeholder="staff@lorafilm.com"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-zinc-500 uppercase tracking-wider block">Chức Vụ</label>
                <select
                  value={formRole}
                  onChange={(e) => setFormRole(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-xl py-3 px-4 text-xs text-zinc-100 focus:outline-none focus:border-amber-500 transition-colors"
                >
                  <option value="Thu ngân">Thu Ngân</option>
                  <option value="Giám sát">Giám Sát</option>
                  <option value="Kế toán">Kế Toán</option>
                </select>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-zinc-500 uppercase tracking-wider block">Lương Cơ Bản / Giờ (VNĐ)</label>
                <input
                  type="number"
                  min="0"
                  step="1000"
                  value={formHourlyWage}
                  onChange={(e) => setFormHourlyWage(e.target.value)}
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
