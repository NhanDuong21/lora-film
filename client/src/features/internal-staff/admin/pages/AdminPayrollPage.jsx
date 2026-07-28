import { useCallback, useEffect, useState } from 'react';
import { changePayrollStatus, getPayrolls, createPayroll, updatePayroll, getEmployees } from '../services/userAdminService';
import { AsyncState, StatusBadge } from '@/components/common/ui/uiKit';
import { Banknote, FileSpreadsheet, Plus, Edit2, CheckCircle2, XCircle, Search, Filter, Clock, CheckCircle } from 'lucide-react';
import { useOutletContext } from 'react-router-dom';
import AdminStatCard from '../components/AdminStatCard';
import useAdminAccess from '../hooks/useAdminAccess';

const money = value => new Intl.NumberFormat('vi-VN', {
  style: 'currency', currency: 'VND', maximumFractionDigits: 0
}).format(value || 0);

export default function AdminPayrollPage() {
  const can = useAdminAccess();
  const canCreatePayroll = can('PAYROLL_CREATE');
  const canUpdatePayroll = can('PAYROLL_UPDATE');
  const canApprovePayroll = can('PAYROLL_APPROVE');
  const [query, setQuery] = useState({ month: '', status: '', page: 0, size: 10 });
  const [result, setResult] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [employees, setEmployees] = useState([]);
  const [state, setState] = useState({ loading: true, error: '' });
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingPayroll, setEditingPayroll] = useState(null);
  const [formData, setFormData] = useState({
    employeeId: '', salaryMonth: '', basicSalary: '', allowance: 0, bonus: 0, deduction: 0
  });
  const [stats, setStats] = useState({ totalBudget: 0, pending: 0, paid: 0 });
  const outlet = useOutletContext();
  const confirmAction = outlet?.triggerConfirm || (() => Promise.resolve(true));
  const notify = outlet?.triggerToast || (() => undefined);

  const loadEmployees = useCallback(async () => {
    if (!canCreatePayroll && !canUpdatePayroll) return;
    try {
      const data = await getEmployees({ size: 100 });
      setEmployees(data?.content || []);
    } catch (error) {
      setState(value => ({
        ...value,
        error: error?.message || 'Không thể tải danh sách nhân viên.'
      }));
    }
  }, [canCreatePayroll, canUpdatePayroll]);

  useEffect(() => { loadEmployees(); }, [loadEmployees]);

  const openModal = (payroll = null) => {
    setEditingPayroll(payroll);
    if (payroll) {
      setFormData({
        employeeId: payroll.employeeId || '',
        salaryMonth: String(payroll.salaryMonth || '').slice(0, 7),
        basicSalary: payroll.basicSalary || '',
        allowance: payroll.allowance || 0,
        bonus: payroll.bonus || 0,
        deduction: payroll.deduction || 0
      });
    } else {
      const now = new Date();
      const monthStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
      setFormData({
        employeeId: '', salaryMonth: monthStr, basicSalary: '',
        allowance: 0, bonus: 0, deduction: 0
      });
    }
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const payload = {
      employeeId: Number(formData.employeeId),
      salaryMonth: formData.salaryMonth,
      basicSalary: Number(formData.basicSalary),
      allowance: Number(formData.allowance || 0),
      bonus: Number(formData.bonus || 0),
      deduction: Number(formData.deduction || 0),
      details: []
    };
    try {
      if (editingPayroll) {
        await updatePayroll(editingPayroll.id, payload);
      } else {
        await createPayroll(payload);
      }
      setIsModalOpen(false);
      await load();
      notify(editingPayroll ? 'Bảng lương đã được cập nhật.' : 'Bảng lương đã được tạo.');
    } catch (error) {
      notify(error?.message || 'Lỗi khi lưu bảng lương', 'error');
    }
  };

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const data = await getPayrolls({
        ...query, month: query.month || undefined, status: query.status || undefined
      });
      setResult(data || { content: [], totalPages: 0, totalElements: 0 });
      
      if (query.page === 0 && !query.status) {
        setStats({
           totalBudget: data?.content?.reduce((acc, curr) => acc + (curr.totalSalary || 0), 0) || 0,
           pending: data?.content?.filter(p => p.status === 'PENDING_APPROVAL')?.length || 0,
           paid: data?.content?.filter(p => p.status === 'PAID')?.length || 0
        });
      }
      
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải bảng lương.' });
    }
  }, [query]);
  
  useEffect(() => { load(); }, [load]);

  const transition = async (id, action) => {
    if (!await confirmAction(`Xác nhận thao tác ${action} với bảng lương này?`)) return;
    try {
      await changePayrollStatus(id, action);
      await load();
      notify('Trạng thái bảng lương đã được cập nhật.');
    } catch (error) {
      setState(value => ({ ...value, error: error?.message || 'Không thể cập nhật bảng lương.' }));
    }
  };

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-[#050506] p-6 text-white md:p-8">
      <header className="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
        <div>
          <h1 className="text-2xl font-black uppercase tracking-wider text-white">Quản lý <span className="text-brand-orange">Lương</span></h1>
          <p className="mt-1 text-sm text-zinc-500">Duyệt, ghi nhận thanh toán và theo dõi lương theo tháng.</p>
        </div>
        {canCreatePayroll && <button onClick={() => openModal()} className="rounded-xl bg-brand-orange px-5 py-2.5 text-sm font-bold text-zinc-950 hover:bg-orange-600 transition-colors shadow-lg shadow-brand-orange/20 flex items-center gap-2">
          <Plus size={18} />
          <span>Tạo Bảng Lương</span>
        </button>}
      </header>

      {/* Dashboard Statistics */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <AdminStatCard title="Tổng ngân sách hiển thị" value={money(stats.totalBudget)} valueClassName="text-brand-orange" subtitle={`Kỳ ${query.month || 'hiện tại'}`} icon={Banknote} colorClass="bg-brand-orange/10 text-brand-orange border-brand-orange/20" />
        <AdminStatCard title="Chờ duyệt" value={stats.pending} icon={Clock} colorClass="bg-amber-500/10 text-amber-500 border-amber-500/20" />
        <AdminStatCard title="Đã thanh toán" value={stats.paid} icon={CheckCircle} colorClass="bg-emerald-500/10 text-emerald-500 border-emerald-500/20" />
      </div>

      <div className="bg-zinc-900/50 border border-zinc-800 rounded-2xl p-4 flex flex-col sm:flex-row gap-3">
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500 w-4 h-4" />
          <input 
            type="month"
            value={query.month}
            onChange={event => setQuery(value => ({ ...value, month: event.target.value, page: 0 }))}
            className="w-full bg-zinc-950 border border-zinc-800 rounded-xl h-10 pl-10 pr-4 text-sm text-zinc-100 placeholder:text-zinc-600 focus:border-brand-orange outline-none transition-colors"
          />
        </div>
        <div className="w-full sm:w-64 relative">
          <Filter className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500 w-4 h-4 z-10 pointer-events-none" />
          <select 
            value={query.status}
            onChange={event => setQuery(value => ({ ...value, status: event.target.value, page: 0 }))}
            className="w-full bg-zinc-950 border border-zinc-800 rounded-xl h-10 pl-10 pr-4 text-sm text-zinc-100 outline-none focus:border-brand-orange appearance-none"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="PENDING_APPROVAL">Chờ duyệt</option>
            <option value="APPROVED">Đã duyệt</option>
            <option value="PAID">Đã trả</option>
            <option value="CANCELLED">Đã hủy</option>
          </select>
        </div>
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!result.content?.length} emptyMessage="Không có bảng lương nào">
        <div className="overflow-x-auto rounded-2xl border border-zinc-800 bg-zinc-900/30">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-zinc-950/50 text-xs uppercase font-black text-zinc-500 tracking-wider">
              <tr>
                <th className="p-4 rounded-tl-2xl">Nhân viên</th>
                <th className="p-4">Kỳ lương</th>
                <th className="p-4">Thực nhận</th>
                <th className="p-4">Trạng thái</th>
                <th className="p-4 text-right rounded-tr-2xl">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/50">
              {result.content?.map(payroll => (
                <tr key={payroll.id} className="hover:bg-zinc-800/20 transition-colors group">
                  <td className="p-4">
                    <p className="font-bold text-zinc-100">{payroll.employeeName}</p>
                    <span className="font-mono text-xs text-zinc-500 mt-0.5 inline-block bg-zinc-900 px-1.5 py-0.5 rounded border border-zinc-800">
                      {payroll.employeeCode}
                    </span>
                  </td>
                  <td className="p-4 font-mono text-zinc-300">
                    {String(payroll.salaryMonth || '').slice(0, 7)}
                  </td>
                  <td className="p-4 font-bold text-brand-orange tracking-wide">
                    {money(payroll.totalSalary)}
                  </td>
                  <td className="p-4">
                    <StatusBadge status={payroll.status} />
                  </td>
                  <td className="p-4 text-right">
                    <div className="flex items-center justify-end gap-1">
                      {canApprovePayroll && payroll.status === 'PENDING_APPROVAL' && (
                        <button 
                          onClick={() => transition(payroll.id, 'approve')}
                          className="p-1.5 rounded-lg text-zinc-400 hover:text-emerald-400 hover:bg-emerald-500/10 transition-colors tooltip"
                          title="Duyệt bảng lương"
                        >
                          <CheckCircle2 size={16} />
                        </button>
                      )}
                      
                      {canUpdatePayroll && payroll.status === 'APPROVED' && (
                        <button 
                          onClick={() => transition(payroll.id, 'paid')}
                          className="p-1.5 rounded-lg text-zinc-400 hover:text-sky-400 hover:bg-sky-500/10 transition-colors tooltip"
                          title="Xác nhận đã trả"
                        >
                          <Banknote size={16} />
                        </button>
                      )}

                      {canUpdatePayroll && <button
                        onClick={() => openModal(payroll)} 
                        className="p-1.5 rounded-lg text-zinc-400 hover:text-blue-400 hover:bg-blue-500/10 transition-colors tooltip"
                        title="Chỉnh sửa chi tiết"
                      >
                        <Edit2 size={16} />
                      </button>}

                      {canUpdatePayroll && !['APPROVED', 'PAID', 'CANCELLED'].includes(payroll.status) && (
                        <button 
                          onClick={() => transition(payroll.id, 'cancel')}
                          className="p-1.5 rounded-lg text-zinc-400 hover:text-red-400 hover:bg-red-500/10 transition-colors tooltip"
                          title="Hủy bảng lương"
                        >
                          <XCircle size={16} />
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
              <h2 className="text-xl font-black uppercase tracking-wider text-white flex items-center gap-2">
                <FileSpreadsheet className="text-brand-orange" size={24} />
                {editingPayroll ? 'Sửa Bảng Lương' : 'Tạo Bảng Lương Mới'}
              </h2>
              <button type="button" onClick={() => setIsModalOpen(false)} className="text-zinc-500 hover:text-white transition-colors">
                X
              </button>
            </div>
            
            <div className="space-y-4 pt-2">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5 relative col-span-2">
                  <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Nhân viên <span className="text-brand-orange">*</span></label>
                  <select 
                    value={formData.employeeId}
                    onChange={e => {
                      const employee = employees.find(item => String(item.accountId) === e.target.value);
                      setFormData({
                        ...formData,
                        employeeId: e.target.value,
                        basicSalary: employee?.baseSalary || ''
                      });
                    }}
                    required 
                    disabled={!!editingPayroll}
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm focus:border-brand-orange outline-none transition-colors appearance-none disabled:opacity-50"
                  >
                    <option value="">Chọn nhân viên</option>
                    {employees.map(e => <option key={e.accountId} value={e.accountId}>{e.fullName} ({e.employeeCode})</option>)}
                  </select>
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Kỳ lương <span className="text-brand-orange">*</span></label>
                <input 
                  type="month"
                  value={formData.salaryMonth} 
                  onChange={e => setFormData({ ...formData, salaryMonth: e.target.value })} 
                  required 
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm focus:border-brand-orange outline-none transition-colors font-mono" 
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="text-[10px] font-black text-emerald-400 uppercase tracking-widest">Lương cơ bản</label>
                  <input 
                    type="number" 
                    value={formData.basicSalary}
                    onChange={e => setFormData({ ...formData, basicSalary: e.target.value })}
                    className="w-full bg-zinc-900 border border-emerald-900/50 rounded-xl px-4 py-2.5 text-sm focus:border-emerald-500 outline-none transition-colors font-mono text-emerald-400" 
                    placeholder="0" 
                    min="0.01"
                    step="0.01"
                    required
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="text-[10px] font-black text-sky-400 uppercase tracking-widest">Phụ cấp</label>
                  <input 
                    type="number" 
                    value={formData.allowance}
                    onChange={e => setFormData({ ...formData, allowance: e.target.value })}
                    className="w-full bg-zinc-900 border border-sky-900/50 rounded-xl px-4 py-2.5 text-sm focus:border-sky-500 outline-none transition-colors font-mono text-sky-400"
                    placeholder="0" 
                    min="0"
                    step="0.01"
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Thưởng / Khấu trừ</label>
                <div className="grid grid-cols-2 gap-4">
                  <input type="number" min="0" step="0.01" value={formData.bonus}
                    onChange={e => setFormData({ ...formData, bonus: e.target.value })}
                    aria-label="Tiền thưởng"
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm focus:border-brand-orange outline-none"
                    placeholder="Tiền thưởng" />
                  <input type="number" min="0" step="0.01" value={formData.deduction}
                    onChange={e => setFormData({ ...formData, deduction: e.target.value })}
                    aria-label="Khấu trừ"
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm focus:border-brand-orange outline-none"
                    placeholder="Khấu trừ" />
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-6 border-t border-zinc-800">
              <button type="button" onClick={() => setIsModalOpen(false)} className="rounded-xl border border-zinc-700 bg-zinc-900 px-5 py-2.5 text-sm font-bold text-zinc-300 hover:bg-zinc-800 transition-colors">
                Hủy
              </button>
              <button type="submit" className="rounded-xl bg-brand-orange px-5 py-2.5 text-sm font-bold text-zinc-950 hover:bg-orange-600 transition-colors shadow-lg shadow-brand-orange/20">
                {editingPayroll ? 'Lưu thay đổi' : 'Tạo bảng lương'}
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}
