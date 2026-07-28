import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  changeEmployeeStatus, getDepartments, getEmployees, getPositions, createEmployee, updateEmployee
} from '../services/userAdminService';
import { AsyncState, Input, Select, StatusBadge } from '@/components/common/ui/uiKit';
import { Users, Briefcase, Building, Wallet, Search, Filter, MoreVertical, Edit2, FileText, UserMinus, UserCheck, Play, Pause } from 'lucide-react';

export default function AdminStaffPage() {
  const [query, setQuery] = useState({
    keyword: '', status: '', departmentId: '', positionId: '', page: 0, size: 10
  });
  const [options, setOptions] = useState({ departments: [], positions: [] });
  const [result, setResult] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [state, setState] = useState({ loading: true, error: '' });
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingEmployee, setEditingEmployee] = useState(null);
  const [formData, setFormData] = useState({ fullName: '', employeeCode: '', departmentId: '', positionId: '', baseSalary: '' });
  const [stats, setStats] = useState({ total: 0, active: 0, onLeave: 0, departments: 0 });

  const openModal = (emp = null) => {
    setEditingEmployee(emp);
    if (emp) {
      setFormData({
        fullName: emp.fullName || '',
        employeeCode: emp.employeeCode || '',
        departmentId: emp.departmentId || '',
        positionId: emp.positionId || '',
        baseSalary: emp.baseSalary || ''
      });
    } else {
      setFormData({ fullName: '', employeeCode: '', departmentId: '', positionId: '', baseSalary: '' });
    }
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingEmployee) {
        await updateEmployee(editingEmployee.accountId, formData);
      } else {
        await createEmployee(formData);
      }
      setIsModalOpen(false);
      await load();
    } catch (error) {
      alert(error?.message || 'Lỗi khi lưu nhân viên');
    }
  };

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const [employees, departments, positions] = await Promise.all([
        getEmployees({
          ...query,
          keyword: query.keyword || undefined,
          status: query.status || undefined,
          departmentId: query.departmentId || undefined,
          positionId: query.positionId || undefined
        }),
        getDepartments(),
        getPositions()
      ]);
      setResult(employees || { content: [], totalPages: 0, totalElements: 0 });
      setOptions({ departments: departments || [], positions: positions || [] });
      
      if (query.page === 0 && !query.keyword && !query.status && !query.departmentId && !query.positionId) {
        setStats({
          total: employees?.totalElements || employees?.content?.length || 0,
          active: employees?.content?.filter(e => e.status === 'ACTIVE')?.length || 0,
          onLeave: employees?.content?.filter(e => e.status === 'ON_LEAVE' || e.status === 'SUSPENDED')?.length || 0,
          departments: departments?.length || 0
        });
      }
      
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải nhân viên.' });
    }
  }, [query]);
  useEffect(() => { load(); }, [load]);

  const changeStatus = async (employee, action) => {
    try {
      await changeEmployeeStatus(employee.accountId, action);
      await load();
    } catch (error) {
      setState(value => ({ ...value, error: error?.message || 'Không thể đổi trạng thái.' }));
    }
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
          <h1 className="text-2xl font-black uppercase tracking-wider text-white">Quản lý <span className="text-brand-orange">Nhân Sự</span></h1>
          <p className="mt-1 text-sm text-zinc-500">Lọc nhân viên theo phòng ban, vị trí và trạng thái làm việc.</p>
        </div>
        <button onClick={() => openModal()} className="rounded-xl bg-brand-orange px-5 py-2.5 text-sm font-bold text-zinc-950 hover:bg-orange-600 transition-colors shadow-lg shadow-brand-orange/20 flex items-center gap-2">
          <UserPlus size={18} />
          <span>Thêm Nhân viên</span>
        </button>
      </header>

      {/* Dashboard Statistics */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Tổng nhân sự" value={stats.total || result.totalElements || '...'} icon={Users} colorClass="bg-blue-500/10 text-blue-500 border border-blue-500/20" />
        <StatCard title="Đang làm việc" value={stats.active || '...'} icon={UserCheck} colorClass="bg-emerald-500/10 text-emerald-500 border border-emerald-500/20" />
        <StatCard title="Đang nghỉ/Tạm ngưng" value={stats.onLeave || '...'} icon={Pause} colorClass="bg-amber-500/10 text-amber-500 border border-amber-500/20" />
        <StatCard title="Phòng ban" value={stats.departments || '...'} icon={Building} colorClass="bg-purple-500/10 text-purple-500 border border-purple-500/20" />
      </div>

      <div className="bg-zinc-900/50 border border-zinc-800 rounded-2xl p-4 flex flex-col xl:flex-row gap-3">
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500 w-4 h-4" />
          <input 
            placeholder="Tìm tên hoặc mã nhân viên..." 
            value={query.keyword}
            onChange={event => setQuery(value => ({ ...value, keyword: event.target.value, page: 0 }))} 
            className="w-full bg-zinc-950 border border-zinc-800 rounded-xl h-10 pl-10 pr-4 text-sm text-zinc-100 placeholder:text-zinc-600 focus:border-brand-orange outline-none transition-colors"
          />
        </div>
        <div className="flex flex-col md:flex-row gap-3 xl:w-[60%]">
          <div className="w-full relative">
            <Building className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500 w-4 h-4 z-10 pointer-events-none" />
            <select 
              value={query.departmentId}
              onChange={event => setQuery(value => ({ ...value, departmentId: event.target.value, page: 0 }))}
              className="w-full bg-zinc-950 border border-zinc-800 rounded-xl h-10 pl-10 pr-4 text-sm text-zinc-100 outline-none focus:border-brand-orange appearance-none"
            >
              <option value="">Tất cả phòng ban</option>
              {options.departments.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
            </select>
          </div>
          <div className="w-full relative">
            <Briefcase className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500 w-4 h-4 z-10 pointer-events-none" />
            <select 
              value={query.positionId}
              onChange={event => setQuery(value => ({ ...value, positionId: event.target.value, page: 0 }))}
              className="w-full bg-zinc-950 border border-zinc-800 rounded-xl h-10 pl-10 pr-4 text-sm text-zinc-100 outline-none focus:border-brand-orange appearance-none"
            >
              <option value="">Tất cả vị trí</option>
              {options.positions.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
            </select>
          </div>
          <div className="w-full relative">
            <Filter className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500 w-4 h-4 z-10 pointer-events-none" />
            <select 
              value={query.status}
              onChange={event => setQuery(value => ({ ...value, status: event.target.value, page: 0 }))}
              className="w-full bg-zinc-950 border border-zinc-800 rounded-xl h-10 pl-10 pr-4 text-sm text-zinc-100 outline-none focus:border-brand-orange appearance-none"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Hoạt động</option>
              <option value="ON_LEAVE">Nghỉ phép</option>
              <option value="SUSPENDED">Tạm ngưng</option>
              <option value="RESIGNED">Đã nghỉ việc</option>
            </select>
          </div>
        </div>
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!result.content?.length} emptyMessage="Không tìm thấy nhân viên nào">
        <div className="overflow-x-auto rounded-2xl border border-zinc-800 bg-zinc-900/30">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-zinc-950/50 text-xs uppercase font-black text-zinc-500 tracking-wider">
              <tr>
                <th className="p-4 rounded-tl-2xl">Mã NV</th>
                <th className="p-4">Nhân viên</th>
                <th className="p-4">Phòng ban / Vị trí</th>
                <th className="p-4">Lương cơ bản</th>
                <th className="p-4">Trạng thái</th>
                <th className="p-4 text-right rounded-tr-2xl">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/50">
              {result.content?.map(employee => (
                <tr key={employee.accountId} className="hover:bg-zinc-800/20 transition-colors group">
                  <td className="p-4">
                    <span className="font-mono text-xs text-zinc-400 bg-zinc-900 px-2 py-1 rounded-md border border-zinc-800 group-hover:border-zinc-700 transition-colors">
                      {employee.employeeCode}
                    </span>
                  </td>
                  <td className="p-4">
                    <p className="font-bold text-zinc-100">{employee.fullName}</p>
                  </td>
                  <td className="p-4 text-zinc-300">
                    <p className="font-medium">{employee.departmentName || '—'}</p>
                    <p className="text-[11px] text-zinc-500 mt-0.5">{employee.positionName || '—'}</p>
                  </td>
                  <td className="p-4 font-mono text-zinc-300">
                    {Number(employee.baseSalary || 0).toLocaleString('vi-VN')} ₫
                  </td>
                  <td className="p-4">
                    <StatusBadge status={employee.status} />
                  </td>
                  <td className="p-4 text-right">
                    <div className="flex items-center justify-end gap-1">
                      <Link 
                        to={`/admin/staff/${employee.accountId}/documents`} 
                        className="p-1.5 rounded-lg text-zinc-400 hover:text-amber-400 hover:bg-amber-500/10 transition-colors tooltip"
                        title="Hồ sơ nhân viên"
                      >
                        <FileText size={16} />
                      </Link>
                      <button 
                        onClick={() => openModal(employee)} 
                        className="p-1.5 rounded-lg text-zinc-400 hover:text-blue-400 hover:bg-blue-500/10 transition-colors"
                        title="Chỉnh sửa"
                      >
                        <Edit2 size={16} />
                      </button>
                      
                      {employee.status === 'SUSPENDED' ? (
                        <button 
                          onClick={() => changeStatus(employee, 'activate')}
                          className="p-1.5 rounded-lg text-zinc-400 hover:text-emerald-400 hover:bg-emerald-500/10 transition-colors"
                          title="Kích hoạt"
                        >
                          <Play size={16} />
                        </button>
                      ) : employee.status !== 'RESIGNED' ? (
                        <button 
                          onClick={() => changeStatus(employee, 'suspend')}
                          className="p-1.5 rounded-lg text-zinc-400 hover:text-amber-400 hover:bg-amber-500/10 transition-colors"
                          title="Tạm ngưng"
                        >
                          <Pause size={16} />
                        </button>
                      ) : null}
                      
                      {employee.status !== 'RESIGNED' && (
                        <button 
                          onClick={() => changeStatus(employee, 'resign')}
                          className="p-1.5 rounded-lg text-zinc-400 hover:text-red-400 hover:bg-red-500/10 transition-colors"
                          title="Nghỉ việc"
                        >
                          <UserMinus size={16} />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {result.totalPages > 1 && (
          <div className="flex items-center justify-between mt-4 bg-zinc-900/50 p-4 rounded-xl border border-zinc-800">
            <span className="text-xs font-medium text-zinc-500 uppercase tracking-wider">
              Trang {query.page + 1} / {result.totalPages} <span className="mx-2">•</span> Tổng: {result.totalElements}
            </span>
            <div className="flex justify-end gap-2">
              <button 
                disabled={query.page === 0} 
                onClick={() => setQuery(value => ({ ...value, page: value.page - 1 }))}
                className="rounded-lg bg-zinc-900 border border-zinc-700 px-4 py-2 text-sm font-bold text-zinc-300 hover:bg-zinc-800 disabled:opacity-40 disabled:hover:bg-zinc-900 transition-colors"
              >
                Trước
              </button>
              <button 
                disabled={query.page + 1 >= result.totalPages}
                onClick={() => setQuery(value => ({ ...value, page: value.page + 1 }))}
                className="rounded-lg bg-zinc-900 border border-zinc-700 px-4 py-2 text-sm font-bold text-zinc-300 hover:bg-zinc-800 disabled:opacity-40 disabled:hover:bg-zinc-900 transition-colors"
              >
                Tiếp
              </button>
            </div>
          </div>
        )}
      </AsyncState>

      {/* Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4 animate-fade-in">
          <form onSubmit={handleSubmit} className="w-full max-w-xl rounded-2xl border border-zinc-800 bg-zinc-950 p-6 space-y-5 shadow-2xl">
            <div className="flex items-center justify-between border-b border-zinc-800 pb-4">
              <h2 className="text-xl font-black uppercase tracking-wider text-white">
                {editingEmployee ? 'Chỉnh sửa nhân viên' : 'Thêm mới nhân viên'}
              </h2>
              <button type="button" onClick={() => setIsModalOpen(false)} className="text-zinc-500 hover:text-white transition-colors">
                X
              </button>
            </div>
            
            <div className="space-y-4 pt-2">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Họ và tên</label>
                  <input value={formData.fullName} onChange={e => setFormData({ ...formData, fullName: e.target.value })} required className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm focus:border-brand-orange outline-none transition-colors" placeholder="Nhập tên nhân viên" />
                </div>
                <div className="space-y-1.5">
                  <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Mã nhân viên</label>
                  <input value={formData.employeeCode} onChange={e => setFormData({ ...formData, employeeCode: e.target.value })} required disabled={!!editingEmployee} className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm focus:border-brand-orange outline-none transition-colors disabled:opacity-50" placeholder="VD: NV001" />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5 relative">
                  <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Phòng ban</label>
                  <select value={formData.departmentId} onChange={e => setFormData({ ...formData, departmentId: e.target.value })} required className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm focus:border-brand-orange outline-none transition-colors appearance-none">
                    <option value="">Chọn phòng ban</option>
                    {options.departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
                  </select>
                </div>
                <div className="space-y-1.5 relative">
                  <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Vị trí</label>
                  <select value={formData.positionId} onChange={e => setFormData({ ...formData, positionId: e.target.value })} required className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm focus:border-brand-orange outline-none transition-colors appearance-none">
                    <option value="">Chọn vị trí</option>
                    {options.positions.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                  </select>
                </div>
              </div>
              
              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Lương cơ bản (VNĐ)</label>
                <div className="relative">
                  <Wallet className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500 w-4 h-4" />
                  <input type="number" value={formData.baseSalary} onChange={e => setFormData({ ...formData, baseSalary: e.target.value })} required className="w-full bg-zinc-900 border border-zinc-800 rounded-xl pl-10 pr-4 py-2.5 text-sm focus:border-brand-orange outline-none transition-colors font-mono" placeholder="0" />
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-6 border-t border-zinc-800">
              <button type="button" onClick={() => setIsModalOpen(false)} className="rounded-xl border border-zinc-700 bg-zinc-900 px-5 py-2.5 text-sm font-bold text-zinc-300 hover:bg-zinc-800 transition-colors">
                Hủy
              </button>
              <button type="submit" className="rounded-xl bg-brand-orange px-5 py-2.5 text-sm font-bold text-zinc-950 hover:bg-orange-600 transition-colors shadow-lg shadow-brand-orange/20">
                {editingEmployee ? 'Lưu thay đổi' : 'Tạo nhân viên'}
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}
