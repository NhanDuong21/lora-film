import { useCallback, useDeferredValue, useEffect, useMemo, useState } from 'react';
import { Link, useOutletContext } from 'react-router-dom';
import {
  applyEmploymentAction,
  createEmployee,
  getDashboard,
  getDepartments,
  getEligibleEmployeeAccounts,
  getEmployee,
  getEmployees,
  getEmploymentActions,
  getPositions
} from '../services/userAdminService';
import { AsyncState, StatusBadge } from '@/components/common/ui/uiKit';
import useAdminAccess from '../hooks/useAdminAccess';
import {
  ActionModal,
  ConsolePagination,
  ConsolePanel,
  DetailDrawer,
  DetailGrid,
  MetricStrip,
  OperationsHeader
} from '../components/OperationsConsole';
import { CalendarClock, FileText, Search, UserCheck, UserMinus, UserPlus, Users } from 'lucide-react';

const EMPTY_PAGE = { content: [], totalPages: 0, totalElements: 0 };
const TODAY = new Date().toISOString().slice(0, 10);
const STATUS_LABELS = { ACTIVE: 'Đang làm việc', ON_LEAVE: 'Nghỉ phép', SUSPENDED: 'Tạm ngưng', RESIGNED: 'Đã nghỉ việc' };
const ACTION_LABELS = {
  ACTIVATE: 'Kích hoạt lại', START_LEAVE: 'Bắt đầu nghỉ phép', END_LEAVE: 'Kết thúc nghỉ phép',
  SUSPEND: 'Tạm ngưng công việc', RESIGN: 'Ghi nhận nghỉ việc', TRANSFER: 'Điều chuyển',
  COMPENSATION_CHANGE: 'Điều chỉnh lương'
};

const initialHireForm = () => ({ accountId: '', departmentId: '', positionId: '', hireDate: TODAY, baseSalary: '' });
const initialActionForm = () => ({ type: 'TRANSFER', departmentId: '', positionId: '', baseSalary: '', effectiveDate: TODAY, reason: '' });

export default function AdminStaffPage() {
  const can = useAdminAccess();
  const canCreate = can('EMPLOYEE_CREATE');
  const canUpdate = can('EMPLOYEE_UPDATE');
  const outlet = useOutletContext();
  const notify = outlet?.triggerToast || (() => undefined);
  const [filters, setFilters] = useState({ keyword: '', status: '', departmentId: '', positionId: '', page: 0, size: 15 });
  const deferredKeyword = useDeferredValue(filters.keyword);
  const [result, setResult] = useState(EMPTY_PAGE);
  const [dashboard, setDashboard] = useState(null);
  const [options, setOptions] = useState({ departments: [], positions: [], accounts: [] });
  const [state, setState] = useState({ loading: true, error: '' });
  const [selected, setSelected] = useState(null);
  const [history, setHistory] = useState([]);
  const [detailLoading, setDetailLoading] = useState(false);
  const [hireOpen, setHireOpen] = useState(false);
  const [hireForm, setHireForm] = useState(initialHireForm);
  const [actionOpen, setActionOpen] = useState(false);
  const [actionForm, setActionForm] = useState(initialActionForm);
  const [submitting, setSubmitting] = useState(false);

  const query = useMemo(() => ({
    page: filters.page,
    size: filters.size,
    keyword: deferredKeyword.trim() || undefined,
    status: filters.status || undefined,
    departmentId: filters.departmentId || undefined,
    positionId: filters.positionId || undefined
  }), [deferredKeyword, filters.departmentId, filters.page, filters.positionId, filters.size, filters.status]);

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const [employees, departments, positions, accounts, summary] = await Promise.all([
        getEmployees(query),
        getDepartments(),
        getPositions(),
        canCreate ? getEligibleEmployeeAccounts({ page: 0, size: 100 }) : Promise.resolve(EMPTY_PAGE),
        can('DASHBOARD_VIEW') ? getDashboard() : Promise.resolve(null)
      ]);
      setResult(employees || EMPTY_PAGE);
      setOptions({ departments: departments || [], positions: positions || [], accounts: accounts?.content || [] });
      setDashboard(summary);
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải danh sách nhân sự.' });
    }
  }, [can, canCreate, query]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const openEmployee = async employee => {
    setSelected(employee);
    setHistory([]);
    setDetailLoading(true);
    try {
      const [detail, actions] = await Promise.all([
        getEmployee(employee.accountId),
        getEmploymentActions(employee.accountId, { page: 0, size: 20 })
      ]);
      setSelected(detail);
      setHistory(actions?.content || []);
    } catch (error) {
      notify(error?.message || 'Không thể tải hồ sơ nhân viên.', 'error');
    } finally {
      setDetailLoading(false);
    }
  };

  const submitHire = async event => {
    event.preventDefault();
    setSubmitting(true);
    try {
      await createEmployee({
        accountId: Number(hireForm.accountId),
        departmentId: Number(hireForm.departmentId),
        positionId: Number(hireForm.positionId),
        hireDate: hireForm.hireDate,
        baseSalary: Number(hireForm.baseSalary)
      });
      setHireOpen(false);
      setHireForm(initialHireForm());
      await load();
      notify('Đã hoàn tất hồ sơ nhân viên từ tài khoản đủ điều kiện.');
    } catch (error) {
      notify(error?.message || 'Không thể tạo hồ sơ nhân viên.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const submitAction = async event => {
    event.preventDefault();
    if (!selected) return;
    setSubmitting(true);
    try {
      const payload = {
        type: actionForm.type,
        effectiveDate: actionForm.effectiveDate,
        reason: actionForm.reason.trim(),
        expectedVersion: selected.version
      };
      if (actionForm.type === 'TRANSFER') {
        payload.departmentId = actionForm.departmentId ? Number(actionForm.departmentId) : null;
        payload.positionId = actionForm.positionId ? Number(actionForm.positionId) : null;
      }
      if (actionForm.type === 'COMPENSATION_CHANGE') payload.baseSalary = Number(actionForm.baseSalary);
      const updated = await applyEmploymentAction(selected.accountId, payload);
      setSelected(updated);
      setActionOpen(false);
      setActionForm(initialActionForm());
      const actions = await getEmploymentActions(selected.accountId, { page: 0, size: 20 });
      setHistory(actions?.content || []);
      await load();
      notify('Đã ghi nhận hành động nhân sự và lưu lịch sử thay đổi.');
    } catch (error) {
      notify(error?.message || 'Không thể ghi nhận hành động nhân sự.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const statusCounts = dashboard?.employeesByStatus || {};
  const metrics = [
    { label: 'Tổng nhân sự', value: dashboard?.totalEmployees ?? result.totalElements, hint: 'Hồ sơ chưa xóa', icon: Users, tone: 'border-blue-500/20 bg-blue-500/10 text-blue-400' },
    { label: 'Đang làm việc', value: statusCounts.ACTIVE ?? '—', hint: 'Có hiệu lực công việc', icon: UserCheck, tone: 'border-emerald-500/20 bg-emerald-500/10 text-emerald-400' },
    { label: 'Nghỉ / tạm ngưng', value: dashboard ? (statusCounts.ON_LEAVE || 0) + (statusCounts.SUSPENDED || 0) : '—', hint: 'Cần theo dõi ngày hiệu lực', icon: CalendarClock, tone: 'border-amber-500/20 bg-amber-500/10 text-amber-400' },
    { label: 'Đã nghỉ việc', value: statusCounts.RESIGNED ?? '—', hint: 'Đã khóa vòng đời nhân sự', icon: UserMinus, tone: 'border-red-500/20 bg-red-500/10 text-red-400' }
  ];

  return (
    <section className="min-h-full space-y-6 bg-[#050506] p-5 text-white md:p-8">
      <OperationsHeader
        eyebrow="Workforce operations"
        title="Điều hành nhân sự"
        description="Quản lý vòng đời nhân viên bằng các hành động có ngày hiệu lực, lý do và lịch sử. Tài khoản đăng nhập và hồ sơ việc làm được xử lý như hai lớp riêng biệt."
        actions={canCreate ? (
          <button type="button" onClick={() => { setHireForm(initialHireForm()); setHireOpen(true); }} className="flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-2.5 text-sm font-black text-black hover:bg-orange-500"><UserPlus size={18} /> Tạo hồ sơ nhân viên</button>
        ) : null}
      />

      <MetricStrip items={metrics} />

      <ConsolePanel>
        <div className="grid gap-3 border-b border-white/10 p-4 md:grid-cols-2 xl:grid-cols-[minmax(280px,1fr)_220px_220px_220px]">
          <label className="relative">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-600" size={18} />
            <input value={filters.keyword} onChange={event => setFilters(value => ({ ...value, keyword: event.target.value, page: 0 }))} aria-label="Tìm nhân viên" placeholder="Tên, email, mã hoặc số điện thoại" className="h-11 w-full rounded-xl border border-white/10 bg-black/30 pl-11 pr-4 text-sm outline-none focus:border-brand-orange" />
          </label>
          <select value={filters.departmentId} onChange={event => setFilters(value => ({ ...value, departmentId: event.target.value, page: 0 }))} aria-label="Lọc phòng ban" className="h-11 rounded-xl border border-white/10 bg-black/30 px-4 text-sm outline-none focus:border-brand-orange"><option value="">Tất cả phòng ban</option>{options.departments.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select>
          <select value={filters.positionId} onChange={event => setFilters(value => ({ ...value, positionId: event.target.value, page: 0 }))} aria-label="Lọc vị trí" className="h-11 rounded-xl border border-white/10 bg-black/30 px-4 text-sm outline-none focus:border-brand-orange"><option value="">Tất cả vị trí</option>{options.positions.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select>
          <select value={filters.status} onChange={event => setFilters(value => ({ ...value, status: event.target.value, page: 0 }))} aria-label="Lọc trạng thái nhân viên" className="h-11 rounded-xl border border-white/10 bg-black/30 px-4 text-sm outline-none focus:border-brand-orange"><option value="">Tất cả trạng thái</option>{Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
        </div>

        <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!result.content.length} emptyMessage="Không có nhân viên phù hợp">
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-white/[0.025] text-[10px] font-black uppercase tracking-[0.16em] text-zinc-600"><tr><th className="px-5 py-4">Nhân viên</th><th className="px-5 py-4">Bộ phận</th><th className="px-5 py-4">Ngày vào làm</th><th className="px-5 py-4">Trạng thái</th><th className="px-5 py-4 text-right">Hồ sơ</th></tr></thead>
              <tbody className="divide-y divide-white/5">
                {result.content.map(employee => (
                  <tr key={employee.accountId} className="hover:bg-white/[0.025]">
                    <td className="px-5 py-4"><p className="font-black text-zinc-100">{employee.fullName}</p><p className="mt-1 font-mono text-xs text-zinc-600">{employee.employeeCode} · {employee.email || `Account #${employee.accountId}`}</p></td>
                    <td className="px-5 py-4"><p className="font-semibold text-zinc-300">{employee.departmentName}</p><p className="mt-1 text-xs text-zinc-600">{employee.positionName}</p></td>
                    <td className="px-5 py-4 text-zinc-400">{employee.hireDate}</td>
                    <td className="px-5 py-4"><StatusBadge status={employee.status} label={STATUS_LABELS[employee.status]} /></td>
                    <td className="px-5 py-4 text-right"><button type="button" onClick={() => openEmployee(employee)} className="rounded-lg border border-white/10 px-3 py-2 text-xs font-black text-zinc-300 hover:border-brand-orange/50 hover:text-brand-orange">Mở hồ sơ</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <ConsolePagination page={filters.page} totalPages={result.totalPages} totalElements={result.totalElements} onPage={page => setFilters(value => ({ ...value, page }))} />
        </AsyncState>
      </ConsolePanel>

      <DetailDrawer
        open={Boolean(selected)}
        onClose={() => setSelected(null)}
        title={selected?.fullName || 'Hồ sơ nhân viên'}
        subtitle={selected?.employeeCode}
        footer={selected ? (
          <div className="grid grid-cols-2 gap-3">
            <Link to={`/admin/staff/${selected.accountId}/documents`} className="flex items-center justify-center gap-2 rounded-xl border border-white/10 px-4 py-3 text-sm font-black text-zinc-200 hover:bg-white/5"><FileText size={17} /> Tài liệu</Link>
            {canUpdate ? <button type="button" onClick={() => { setActionForm(initialActionForm()); setActionOpen(true); }} className="rounded-xl bg-brand-orange px-4 py-3 text-sm font-black text-black hover:bg-orange-500">Ghi nhận hành động</button> : null}
          </div>
        ) : null}
      >
        {detailLoading ? <p className="text-sm text-zinc-500">Đang tải hồ sơ…</p> : selected ? (
          <div className="space-y-7">
            <DetailGrid items={[
              { label: 'Email', value: selected.email }, { label: 'Số điện thoại', value: selected.phoneNumber },
              { label: 'Phòng ban', value: selected.departmentName }, { label: 'Vị trí', value: selected.positionName },
              { label: 'Ngày vào làm', value: selected.hireDate },
              { label: 'Lương cơ bản', value: new Intl.NumberFormat('vi-VN').format(selected.baseSalary || 0) + ' ₫' }
            ]} />
            <div>
              <div className="mb-3 flex items-center justify-between"><h3 className="text-sm font-black text-white">Lịch sử nhân sự</h3><span className="text-xs text-zinc-600">{history.length} hành động gần nhất</span></div>
              <div className="space-y-2">
                {history.length ? history.map(action => (
                  <div key={action.id} className="rounded-xl border border-white/10 bg-white/[0.025] p-4">
                    <div className="flex items-center justify-between gap-3"><p className="text-sm font-black text-zinc-200">{ACTION_LABELS[action.type] || action.type}</p><time className="text-xs text-zinc-600">{action.effectiveDate}</time></div>
                    <p className="mt-2 text-sm leading-5 text-zinc-500">{action.reason}</p>
                  </div>
                )) : <p className="rounded-xl border border-dashed border-white/10 p-5 text-sm text-zinc-600">Chưa có hành động nhân sự nào được ghi nhận theo workflow mới.</p>}
              </div>
            </div>
          </div>
        ) : null}
      </DetailDrawer>

      <ActionModal open={hireOpen} onClose={() => setHireOpen(false)} title="Tạo hồ sơ nhân viên" description="Chỉ các tài khoản đang hoạt động và chưa gắn với nhân viên mới xuất hiện. Việc tạo tài khoản/mật khẩu đã được tách khỏi form nhân sự." onSubmit={submitHire} submitLabel="Tạo hồ sơ" submitting={submitting}>
        <label className="block text-xs font-black uppercase tracking-wider text-zinc-500">Tài khoản đủ điều kiện *</label>
        <select value={hireForm.accountId} onChange={event => setHireForm(value => ({ ...value, accountId: event.target.value }))} required className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange"><option value="">Chọn tài khoản</option>{options.accounts.map(account => <option key={account.accountId} value={account.accountId}>{account.fullName} · {account.email}</option>)}</select>
        {!options.accounts.length ? <p className="rounded-xl border border-amber-500/20 bg-amber-500/5 p-3 text-xs leading-5 text-amber-300">Chưa có tài khoản đủ điều kiện. Hãy tạo hoặc kích hoạt tài khoản tại “Tài khoản đăng nhập” trước.</p> : null}
        <div className="grid gap-3 sm:grid-cols-2">
          <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Phòng ban *</label><select value={hireForm.departmentId} onChange={event => setHireForm(value => ({ ...value, departmentId: event.target.value, positionId: '' }))} required className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange"><option value="">Chọn phòng ban</option>{options.departments.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div>
          <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Vị trí *</label><select value={hireForm.positionId} onChange={event => setHireForm(value => ({ ...value, positionId: event.target.value }))} required disabled={!hireForm.departmentId} className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange disabled:opacity-40"><option value="">Chọn vị trí</option>{options.positions.filter(item => String(item.departmentId) === String(hireForm.departmentId)).map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div>
          <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Ngày vào làm *</label><input type="date" max={TODAY} value={hireForm.hireDate} onChange={event => setHireForm(value => ({ ...value, hireDate: event.target.value }))} required className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange" /></div>
          <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Lương cơ bản *</label><input type="number" min="1" value={hireForm.baseSalary} onChange={event => setHireForm(value => ({ ...value, baseSalary: event.target.value }))} required className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange" /></div>
        </div>
      </ActionModal>

      <ActionModal open={actionOpen} onClose={() => setActionOpen(false)} title="Ghi nhận hành động nhân sự" description="Mọi hành động yêu cầu ngày hiệu lực và lý do. Hệ thống lưu snapshot trước/sau để audit." onSubmit={submitAction} submitLabel="Ghi nhận hành động" submitting={submitting} tone={['SUSPEND', 'RESIGN'].includes(actionForm.type) ? 'danger' : 'orange'}>
        <label className="block text-xs font-black uppercase tracking-wider text-zinc-500">Loại hành động *</label>
        <select value={actionForm.type} onChange={event => setActionForm(value => ({ ...value, type: event.target.value }))} className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange">{Object.entries(ACTION_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
        {actionForm.type === 'TRANSFER' ? <div className="grid gap-3 sm:grid-cols-2"><select value={actionForm.departmentId} onChange={event => setActionForm(value => ({ ...value, departmentId: event.target.value, positionId: '' }))} className="rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange"><option value="">Giữ phòng ban hiện tại</option>{options.departments.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select><select value={actionForm.positionId} onChange={event => setActionForm(value => ({ ...value, positionId: event.target.value }))} className="rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange"><option value="">Giữ vị trí hiện tại</option>{options.positions.filter(item => String(item.departmentId) === String(actionForm.departmentId || selected?.departmentId)).map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div> : null}
        {actionForm.type === 'COMPENSATION_CHANGE' ? <input type="number" min="1" value={actionForm.baseSalary} onChange={event => setActionForm(value => ({ ...value, baseSalary: event.target.value }))} required placeholder="Lương cơ bản mới" className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange" /> : null}
        <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Ngày hiệu lực *</label><input type="date" max={TODAY} value={actionForm.effectiveDate} onChange={event => setActionForm(value => ({ ...value, effectiveDate: event.target.value }))} required className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange" /></div>
        <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Lý do *</label><textarea value={actionForm.reason} onChange={event => setActionForm(value => ({ ...value, reason: event.target.value }))} minLength={5} maxLength={500} required rows={4} placeholder="Căn cứ và ghi chú bàn giao…" className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm leading-6 outline-none focus:border-brand-orange" /></div>
      </ActionModal>
    </section>
  );
}
