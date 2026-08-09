import { useCallback, useDeferredValue, useEffect, useMemo, useState } from 'react';
import { Link, useOutletContext } from 'react-router-dom';
import {
  applyEmploymentAction,
  assignEmployeeCinema,
  createEmployee,
  getDashboard,
  getDepartments,
  getEligibleEmployeeAccounts,
  getEmployee,
  getEmployees,
  getEmploymentActions,
  getPositions
} from '../services/userAdminService';
import { createEmployeeAccount } from '../services/authAdminService';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import { AsyncState, StatusBadge } from '@/components/common/ui/uiKit';
import SearchableSelect from '@/components/common/SearchableSelect';
import useAdminAccess from '../hooks/useAdminAccess';
import {
  ActionModal,
  ConsolePagination,
  ConsolePanel,
  DetailDrawer,
  DetailGrid,
} from '../components/OperationsConsole';
import { Building2, CalendarDays, FileText, KeyRound, Mail, Phone, Search, UserPlus, UserRoundCheck } from 'lucide-react';
import { HrHero, PersonAvatar, UatGuide } from '../components/HrWorkspace';

const EMPTY_PAGE = { content: [], totalPages: 0, totalElements: 0 };
const TODAY = new Date().toISOString().slice(0, 10);
const STATUS_LABELS = { ACTIVE: 'Đang làm việc', ON_LEAVE: 'Nghỉ phép', SUSPENDED: 'Tạm ngưng', RESIGNED: 'Đã nghỉ việc' };
const ACTION_LABELS = {
  ACTIVATE: 'Kích hoạt lại', START_LEAVE: 'Bắt đầu nghỉ phép', END_LEAVE: 'Kết thúc nghỉ phép',
  SUSPEND: 'Tạm ngưng công việc', RESIGN: 'Ghi nhận nghỉ việc', TRANSFER: 'Điều chuyển',
  COMPENSATION_CHANGE: 'Điều chỉnh lương'
};

const initialHireForm = () => ({ accountId: '', departmentId: '', positionId: '', cinemaPublicId: '', hireDate: TODAY, baseSalary: '' });
const initialAccountForm = () => ({ fullName: '', email: '', password: '' });
const initialActionForm = () => ({ type: 'TRANSFER', departmentId: '', positionId: '', baseSalary: '', effectiveDate: TODAY, reason: '' });

export default function AdminStaffPage() {
  const can = useAdminAccess();
  const canCreate = can('EMPLOYEE_CREATE');
  const canUpdate = can('EMPLOYEE_UPDATE');
  const outlet = useOutletContext();
  const notify = outlet?.triggerToast || (() => undefined);
  const [filters, setFilters] = useState({ keyword: '', status: '', departmentId: '', positionId: '', cinemaPublicId: '', page: 0, size: 15 });
  const deferredKeyword = useDeferredValue(filters.keyword);
  const [result, setResult] = useState(EMPTY_PAGE);
  const [dashboard, setDashboard] = useState(null);
  const [options, setOptions] = useState({ departments: [], positions: [], accounts: [], cinemas: [] });
  const [state, setState] = useState({ loading: true, error: '' });
  const [selected, setSelected] = useState(null);
  const [history, setHistory] = useState([]);
  const [detailLoading, setDetailLoading] = useState(false);
  const [hireOpen, setHireOpen] = useState(false);
  const [hireForm, setHireForm] = useState(initialHireForm);
  const [hireStep, setHireStep] = useState(1);
  const [accountMode, setAccountMode] = useState('existing');
  const [accountForm, setAccountForm] = useState(initialAccountForm);
  const [actionOpen, setActionOpen] = useState(false);
  const [actionForm, setActionForm] = useState(initialActionForm);
  const [cinemaAssignmentOpen, setCinemaAssignmentOpen] = useState(false);
  const [cinemaAssignmentId, setCinemaAssignmentId] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const query = useMemo(() => ({
    page: filters.page,
    size: filters.size,
    keyword: deferredKeyword.trim() || undefined,
    status: filters.status || undefined,
    departmentId: filters.departmentId || undefined,
    positionId: filters.positionId || undefined,
    cinemaPublicId: filters.cinemaPublicId || undefined
  }), [deferredKeyword, filters.cinemaPublicId, filters.departmentId, filters.page, filters.positionId, filters.size, filters.status]);

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const [employees, departments, positions, accounts, summary, cinemaEnvelope] = await Promise.all([
        getEmployees(query),
        getDepartments(),
        getPositions(),
        canCreate ? getEligibleEmployeeAccounts({ page: 0, size: 100 }) : Promise.resolve(EMPTY_PAGE),
        can('DASHBOARD_VIEW') ? getDashboard() : Promise.resolve(null),
        adminCinemaService.getCinemas({ page: 0, size: 100, showDeleted: false, sort: 'name,asc' })
          .catch(() => null)
      ]);
      setResult(employees || EMPTY_PAGE);
      setOptions({
        departments: departments || [],
        positions: positions || [],
        accounts: accounts?.content || [],
        cinemas: cinemaEnvelope?.data?.data || []
      });
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

  const openHire = () => {
    setHireForm(initialHireForm());
    setAccountForm(initialAccountForm());
    setAccountMode('existing');
    setHireStep(1);
    setHireOpen(true);
  };

  const closeHire = () => {
    setHireOpen(false);
    setHireStep(1);
    setAccountMode('existing');
    setAccountForm(initialAccountForm());
  };

  const createAndResolveAccount = async () => {
    const created = await createEmployeeAccount({
      fullName: accountForm.fullName.trim(),
      email: accountForm.email.trim(),
      password: accountForm.password
    });
    let eligibleAccount = null;
    for (let attempt = 0; attempt < 8 && !eligibleAccount; attempt += 1) {
      if (attempt > 0) await new Promise(resolve => setTimeout(resolve, 350));
      const accountPage = await getEligibleEmployeeAccounts({
        keyword: created.email,
        page: 0,
        size: 20
      });
      eligibleAccount = accountPage?.content?.find(item => item.accountId === created.id) || null;
    }
    return eligibleAccount || {
      accountId: created.id,
      fullName: accountForm.fullName.trim(),
      email: created.email,
      syncing: true
    };
  };

  const submitHire = async event => {
    event.preventDefault();
    setSubmitting(true);
    try {
      if (hireStep === 1) {
        if (accountMode === 'existing') {
          if (!hireForm.accountId) {
            notify('Hãy chọn một tài khoản đăng nhập để tiếp tục.', 'error');
            return;
          }
        } else {
          const account = await createAndResolveAccount();
          setOptions(value => ({
            ...value,
            accounts: [account, ...value.accounts.filter(item => item.accountId !== account.accountId)]
          }));
          setHireForm(value => ({ ...value, accountId: String(account.accountId) }));
          setAccountMode('existing');
          notify(account.syncing
            ? 'Đã tạo tài khoản. Hệ thống đang đồng bộ hồ sơ, bạn có thể tiếp tục nhập thông tin công việc.'
            : 'Đã tạo tài khoản đăng nhập. Tiếp tục khai báo thông tin công việc.');
        }
        setHireStep(2);
        return;
      }
      if (!hireForm.departmentId || !hireForm.positionId || !hireForm.cinemaPublicId || !hireForm.baseSalary) {
        notify('Vui lòng chọn đủ rạp làm việc, phòng ban, vị trí và nhập lương cơ bản.', 'error');
        return;
      }
      await createEmployee({
        accountId: Number(hireForm.accountId),
        departmentId: Number(hireForm.departmentId),
        positionId: Number(hireForm.positionId),
        hireDate: hireForm.hireDate,
        baseSalary: Number(hireForm.baseSalary),
        cinemaPublicId: hireForm.cinemaPublicId
      });
      closeHire();
      setHireForm(initialHireForm());
      await load();
      notify('Đã tạo nhân viên và phân công rạp làm việc.');
    } catch (error) {
      notify(error?.message || (hireStep === 1 ? 'Không thể tạo tài khoản đăng nhập.' : 'Không thể tạo hồ sơ nhân viên.'), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const openCinemaAssignment = () => {
    setCinemaAssignmentId(selected?.cinemaPublicId || '');
    setCinemaAssignmentOpen(true);
  };

  const submitCinemaAssignment = async event => {
    event.preventDefault();
    if (!selected) return;
    setSubmitting(true);
    try {
      const updated = await assignEmployeeCinema(selected.accountId, cinemaAssignmentId || null);
      setSelected(updated);
      setCinemaAssignmentOpen(false);
      await load();
      notify(cinemaAssignmentId
        ? 'Đã cập nhật rạp làm việc cho nhân viên.'
        : 'Đã gỡ phân công rạp. Nhân viên sẽ chưa xuất hiện trong danh sách của quản lý rạp.');
    } catch (error) {
      notify(error?.message || 'Không thể cập nhật rạp làm việc.', 'error');
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
  const selectedHireAccount = options.accounts.find(item => String(item.accountId) === String(hireForm.accountId));
  const accountOptions = options.accounts.map(account => ({
    value: String(account.accountId),
    label: account.fullName,
    subtitle: 'Email đăng nhập: ' + account.email,
    badge: 'Chưa có hồ sơ'
  }));
  const departmentOptions = options.departments.map(item => ({
    value: String(item.id),
    label: item.name,
    subtitle: item.description || 'Phòng ban đang hoạt động'
  }));
  const positionOptions = options.positions
    .filter(item => String(item.departmentId) === String(hireForm.departmentId))
    .map(item => ({
      value: String(item.id),
      label: item.name,
      subtitle: item.description || 'Vị trí công việc'
    }));
  const cinemaOptions = options.cinemas.map(cinema => ({
    value: cinema.publicId,
    label: cinema.name,
    subtitle: [cinema.district, cinema.city].filter(Boolean).join(', ') || 'Rạp trong hệ thống',
    badge: cinema.status === 'ACTIVE' ? 'Đang hoạt động' : 'Tạm ngưng'
  }));
  const cinemaByPublicId = new Map(options.cinemas.map(cinema => [String(cinema.publicId), cinema]));
  const cinemaName = cinemaPublicId => cinemaByPublicId.get(String(cinemaPublicId || ''))?.name
    || (cinemaPublicId ? 'Rạp không còn trong danh mục' : 'Chưa phân công rạp');
  return (
    <section className="min-h-full space-y-5 text-white">
      <HrHero
        context="Hồ sơ thay cho danh sách tài khoản"
        title="Hồ sơ nhân viên"
        description="Mỗi nhân viên có một hồ sơ công việc, thông tin liên hệ và dòng thời gian thay đổi. Mở một thẻ để xem toàn bộ hành trình thay vì sửa trực tiếp trên bảng."
        actions={<><UatGuide compact />{canCreate ? <button type="button" onClick={openHire} className="flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2.5 text-sm font-black text-black hover:bg-orange-400"><UserPlus size={18} /> Thêm nhân viên</button> : null}</>}
      />

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {[
          ['Tất cả hồ sơ', dashboard?.totalEmployees ?? result.totalElements, 'text-blue-300'],
          ['Đang làm việc', statusCounts.ACTIVE ?? '—', 'text-emerald-300'],
          ['Đang nghỉ / tạm ngưng', dashboard ? (statusCounts.ON_LEAVE || 0) + (statusCounts.SUSPENDED || 0) : '—', 'text-amber-300'],
          ['Đã nghỉ việc', statusCounts.RESIGNED ?? '—', 'text-red-300']
        ].map(item => <div key={item[0]} className="rounded-2xl border border-white/10 bg-[#0b0b0e] p-4"><p className="text-xs font-bold text-zinc-500">{item[0]}</p><p className={'mt-2 text-2xl font-black ' + item[2]}>{item[1]}</p></div>)}
      </div>

      <ConsolePanel className="overflow-hidden rounded-[24px]">
        <div className="grid gap-3 border-b border-white/10 p-4 md:grid-cols-2 xl:grid-cols-[minmax(260px,1fr)_210px_190px_190px_190px]">
          <label className="relative">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-600" size={18} />
            <input value={filters.keyword} onChange={event => setFilters(value => ({ ...value, keyword: event.target.value, page: 0 }))} aria-label="Tìm nhân viên" placeholder="Tên, email, mã hoặc số điện thoại" className="h-11 w-full rounded-xl border border-white/10 bg-black/30 pl-11 pr-4 text-sm outline-none focus:border-brand-orange" />
          </label>
          <select value={filters.cinemaPublicId} onChange={event => setFilters(value => ({ ...value, cinemaPublicId: event.target.value, page: 0 }))} aria-label="Lọc rạp làm việc" className="h-11 rounded-xl border border-white/10 bg-black/30 px-4 text-sm outline-none focus:border-brand-orange"><option value="">Tất cả rạp</option><option value="__unassigned__">Chưa phân công rạp</option>{options.cinemas.map(item => <option key={item.publicId} value={item.publicId}>{item.name}</option>)}</select>
          <select value={filters.departmentId} onChange={event => setFilters(value => ({ ...value, departmentId: event.target.value, page: 0 }))} aria-label="Lọc phòng ban" className="h-11 rounded-xl border border-white/10 bg-black/30 px-4 text-sm outline-none focus:border-brand-orange"><option value="">Tất cả phòng ban</option>{options.departments.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select>
          <select value={filters.positionId} onChange={event => setFilters(value => ({ ...value, positionId: event.target.value, page: 0 }))} aria-label="Lọc vị trí" className="h-11 rounded-xl border border-white/10 bg-black/30 px-4 text-sm outline-none focus:border-brand-orange"><option value="">Tất cả vị trí</option>{options.positions.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select>
          <select value={filters.status} onChange={event => setFilters(value => ({ ...value, status: event.target.value, page: 0 }))} aria-label="Lọc trạng thái nhân viên" className="h-11 rounded-xl border border-white/10 bg-black/30 px-4 text-sm outline-none focus:border-brand-orange"><option value="">Tất cả trạng thái</option>{Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
        </div>

        <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!result.content.length} emptyMessage="Không có nhân viên phù hợp">
          <div className="grid gap-3 p-4 lg:grid-cols-2 2xl:grid-cols-3">
            {result.content.map(employee => (
              <button key={employee.accountId} type="button" onClick={() => openEmployee(employee)} className="group rounded-2xl border border-white/10 bg-black/20 p-4 text-left transition hover:-translate-y-0.5 hover:border-orange-500/30 hover:bg-orange-500/[0.03]">
                <div className="flex items-start gap-3">
                  <PersonAvatar name={employee.fullName} size="lg" />
                  <div className="min-w-0 flex-1"><div className="flex items-start justify-between gap-2"><div className="min-w-0"><p className="truncate font-black text-zinc-100">{employee.fullName}</p><p className="mt-1 font-mono text-[10px] text-zinc-600">{employee.employeeCode}</p></div><StatusBadge status={employee.status} label={STATUS_LABELS[employee.status]} /></div></div>
                </div>
                <div className="mt-4 grid grid-cols-2 gap-3 border-t border-white/5 pt-4 text-xs">
                  <div><p className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Phòng ban</p><p className="mt-1 truncate font-bold text-zinc-300">{employee.departmentName || 'Chưa phân bổ'}</p></div>
                  <div><p className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Vị trí</p><p className="mt-1 truncate font-bold text-zinc-300">{employee.positionName || 'Chưa phân bổ'}</p></div>
                </div>
                <div className={'mt-3 flex items-center gap-2 rounded-xl border px-3 py-2 text-xs font-bold ' + (employee.cinemaPublicId ? 'border-sky-500/15 bg-sky-500/[0.05] text-sky-200' : 'border-amber-500/20 bg-amber-500/[0.06] text-amber-300')}><Building2 size={15} /> <span className="truncate">{cinemaName(employee.cinemaPublicId)}</span></div>
                <div className="mt-3 flex items-center justify-between text-xs text-zinc-500"><span className="flex items-center gap-1.5"><CalendarDays size={14} /> Vào làm {employee.hireDate}</span><span className="font-black text-orange-300 opacity-0 transition group-hover:opacity-100">Mở hồ sơ →</span></div>
              </button>
            ))}
          </div>
          <div className="hidden overflow-x-auto">
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
            <div className="rounded-2xl border border-orange-500/20 bg-gradient-to-br from-orange-500/10 to-transparent p-5">
              <div className="flex items-center gap-4"><PersonAvatar name={selected.fullName} size="lg" /><div><p className="text-lg font-black">{selected.fullName}</p><p className="mt-1 text-xs text-zinc-500">{selected.employeeCode} · {STATUS_LABELS[selected.status]}</p></div></div>
              <div className="mt-5 grid gap-2 text-sm">
                <div className="flex items-center gap-3 rounded-xl bg-black/20 p-3 text-zinc-300"><Mail size={16} className="text-zinc-600" /> {selected.email || 'Chưa có email'}</div>
                <div className="flex items-center gap-3 rounded-xl bg-black/20 p-3 text-zinc-300"><Phone size={16} className="text-zinc-600" /> {selected.phoneNumber || 'Thông tin được ẩn theo quyền'}</div>
              </div>
            </div>
            <DetailGrid items={[
              { label: 'Email', value: selected.email }, { label: 'Số điện thoại', value: selected.phoneNumber },
              { label: 'Phòng ban', value: selected.departmentName }, { label: 'Vị trí', value: selected.positionName },
              { label: 'Rạp làm việc', value: cinemaName(selected.cinemaPublicId) },
              { label: 'Ngày vào làm', value: selected.hireDate },
              { label: 'Lương cơ bản', value: new Intl.NumberFormat('vi-VN').format(selected.baseSalary || 0) + ' ₫' }
            ]} />
            <div className={'rounded-2xl border p-4 ' + (selected.cinemaPublicId ? 'border-sky-500/20 bg-sky-500/[0.05]' : 'border-amber-500/25 bg-amber-500/[0.06]')}>
              <div className="flex items-start gap-3">
                <span className={'grid h-10 w-10 shrink-0 place-items-center rounded-xl ' + (selected.cinemaPublicId ? 'bg-sky-500/10 text-sky-300' : 'bg-amber-500/10 text-amber-300')}><Building2 size={19} /></span>
                <div className="min-w-0 flex-1"><p className="text-xs font-black uppercase tracking-wider text-zinc-500">Phân công rạp</p><p className="mt-1 truncate text-sm font-black text-zinc-100">{cinemaName(selected.cinemaPublicId)}</p><p className="mt-1 text-xs leading-5 text-zinc-500">{selected.cinemaPublicId ? 'Quản lý rạp này có thể xem và xếp ca cho nhân viên.' : 'Hãy chọn rạp để nhân viên xuất hiện đúng trong màn hình của quản lý.'}</p></div>
              </div>
              {canUpdate ? <button type="button" onClick={openCinemaAssignment} className="mt-4 w-full rounded-xl border border-white/10 bg-white/[0.04] px-4 py-2.5 text-sm font-black text-zinc-200 hover:border-sky-500/30 hover:bg-sky-500/[0.06]">{selected.cinemaPublicId ? 'Đổi hoặc gỡ phân công rạp' : 'Phân công rạp ngay'}</button> : null}
            </div>
            <div>
              <div className="mb-3 flex items-center justify-between"><h3 className="text-sm font-black text-white">Lịch sử nhân sự</h3><span className="text-xs text-zinc-600">{history.length} hành động gần nhất</span></div>
              <div className="relative space-y-3 before:absolute before:bottom-3 before:left-[7px] before:top-3 before:w-px before:bg-white/10">
                {history.length ? history.map(action => (
                  <div key={action.id} className="relative pl-7">
                    <span className="absolute left-0 top-4 h-[15px] w-[15px] rounded-full border-4 border-[#09090b] bg-orange-400" />
                    <div className="rounded-xl border border-white/10 bg-white/[0.025] p-4">
                      <div className="flex items-center justify-between gap-3"><p className="text-sm font-black text-zinc-200">{ACTION_LABELS[action.type] || action.type}</p><time className="text-xs text-zinc-600">{action.effectiveDate}</time></div>
                      <p className="mt-2 text-sm leading-5 text-zinc-500">{action.reason}</p>
                    </div>
                  </div>
                )) : <p className="rounded-xl border border-dashed border-white/10 p-5 text-sm text-zinc-600">Chưa có hành động nhân sự nào được ghi nhận theo workflow mới.</p>}
              </div>
            </div>
          </div>
        ) : null}
      </DetailDrawer>

      <ActionModal
        open={hireOpen}
        onClose={closeHire}
        title={hireStep === 1 ? 'Bước 1 · Chọn tài khoản đăng nhập' : 'Bước 2 · Khai báo hồ sơ công việc'}
        description={hireStep === 1
          ? 'Chọn một tài khoản có sẵn hoặc tạo mới ngay tại đây.'
          : 'Bổ sung thông tin để người này xuất hiện trong ca làm, chấm công và bảng lương.'}
        onSubmit={submitHire}
        submitLabel={hireStep === 1 ? (accountMode === 'new' ? 'Tạo tài khoản & tiếp tục' : 'Tiếp tục') : 'Tạo nhân viên'}
        submitting={submitting}
      >
        <div className="grid grid-cols-2 gap-2" aria-label={'Tiến độ: bước ' + hireStep + ' trên 2'}>
          {[1, 2].map(step => (
            <div key={step} className={'h-1.5 rounded-full ' + (step <= hireStep ? 'bg-orange-500' : 'bg-white/10')} />
          ))}
        </div>

        {hireStep === 1 ? (
          <>
            <div className="rounded-2xl border border-blue-500/20 bg-blue-500/[0.06] p-4 text-sm leading-6 text-zinc-300">
              <p className="font-black text-blue-200">Tài khoản và nhân viên khác nhau thế nào?</p>
              <p className="mt-1 text-zinc-400"><strong className="text-zinc-200">Tài khoản</strong> dùng để đăng nhập. <strong className="text-zinc-200">Hồ sơ nhân viên</strong> lưu phòng ban, vị trí, ngày vào làm và lương. Hệ thống sẽ liên kết hai phần này với nhau.</p>
            </div>

            <div className="grid grid-cols-2 rounded-xl border border-white/10 bg-black/30 p-1">
              <button type="button" onClick={() => setAccountMode('existing')} className={'rounded-lg px-3 py-2.5 text-xs font-black transition ' + (accountMode === 'existing' ? 'bg-orange-500 text-black' : 'text-zinc-500 hover:text-white')}>Dùng tài khoản có sẵn</button>
              <button type="button" onClick={() => setAccountMode('new')} className={'rounded-lg px-3 py-2.5 text-xs font-black transition ' + (accountMode === 'new' ? 'bg-orange-500 text-black' : 'text-zinc-500 hover:text-white')}>Tạo tài khoản mới</button>
            </div>

            {accountMode === 'existing' ? (
              <div className="space-y-3">
                <div>
                  <label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Tài khoản chưa có hồ sơ nhân viên *</label>
                  <SearchableSelect
                    value={hireForm.accountId}
                    onChange={accountId => setHireForm(value => ({ ...value, accountId }))}
                    options={accountOptions}
                    placeholder="Chọn hoặc tìm theo tên, email"
                    ariaLabel="Chọn tài khoản đăng nhập"
                  />
                </div>
                <p className="text-xs leading-5 text-zinc-500">Có {options.accounts.length} tài khoản đang hoạt động nhưng chưa có hồ sơ nhân viên.</p>
                {!options.accounts.length ? (
                  <button type="button" onClick={() => setAccountMode('new')} className="w-full rounded-xl border border-amber-500/20 bg-amber-500/5 p-3 text-left text-xs font-bold leading-5 text-amber-300 hover:bg-amber-500/10">Chưa có tài khoản phù hợp. Bấm vào đây để tạo tài khoản mới ngay trong quy trình.</button>
                ) : null}
              </div>
            ) : (
              <div className="space-y-3">
                <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Họ và tên *</label><input value={accountForm.fullName} onChange={event => setAccountForm(value => ({ ...value, fullName: event.target.value }))} required placeholder="Ví dụ: Nguyễn Thị Lan" className="w-full rounded-xl border border-white/10 bg-black/50 p-3 text-sm outline-none focus:border-brand-orange" /></div>
                <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Email đăng nhập *</label><input type="email" value={accountForm.email} onChange={event => setAccountForm(value => ({ ...value, email: event.target.value }))} required placeholder="lan.nguyen@lorafilm.local" className="w-full rounded-xl border border-white/10 bg-black/50 p-3 text-sm outline-none focus:border-brand-orange" /></div>
                <div><label className="mb-2 flex items-center gap-2 text-xs font-black uppercase tracking-wider text-zinc-500"><KeyRound size={14} /> Mật khẩu ban đầu *</label><input type="password" minLength={6} value={accountForm.password} onChange={event => setAccountForm(value => ({ ...value, password: event.target.value }))} required placeholder="Tối thiểu 6 ký tự" className="w-full rounded-xl border border-white/10 bg-black/50 p-3 text-sm outline-none focus:border-brand-orange" /></div>
                <p className="rounded-xl border border-white/10 bg-white/[0.025] p-3 text-xs leading-5 text-zinc-500">Tài khoản mới được cấp quyền nhân viên cơ bản. Bạn có thể đổi vai trò sau tại mục “Tài khoản đăng nhập”.</p>
              </div>
            )}
          </>
        ) : (
          <>
            <div className="flex items-center gap-3 rounded-2xl border border-emerald-500/20 bg-emerald-500/[0.06] p-4">
              <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-emerald-500/10 text-emerald-300"><UserRoundCheck size={20} /></span>
              <div className="min-w-0 flex-1"><p className="truncate text-sm font-black text-zinc-100">{selectedHireAccount?.fullName || 'Tài khoản đã chọn'}</p><p className="mt-1 truncate text-xs text-zinc-500">{selectedHireAccount?.email}</p></div>
              <button type="button" onClick={() => setHireStep(1)} className="rounded-lg border border-white/10 px-3 py-2 text-xs font-black text-zinc-400 hover:bg-white/5 hover:text-white">Đổi</button>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="sm:col-span-2"><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Rạp làm việc *</label><SearchableSelect value={hireForm.cinemaPublicId} onChange={cinemaPublicId => setHireForm(value => ({ ...value, cinemaPublicId }))} options={cinemaOptions} placeholder="Chọn rạp nhân viên sẽ làm việc" ariaLabel="Chọn rạp làm việc" /></div>
              <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Phòng ban *</label><SearchableSelect value={hireForm.departmentId} onChange={departmentId => setHireForm(value => ({ ...value, departmentId, positionId: '' }))} options={departmentOptions} placeholder="Chọn phòng ban" ariaLabel="Chọn phòng ban" /></div>
              <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Vị trí *</label><SearchableSelect value={hireForm.positionId} onChange={positionId => setHireForm(value => ({ ...value, positionId }))} options={positionOptions} placeholder={hireForm.departmentId ? 'Chọn vị trí' : 'Chọn phòng ban trước'} ariaLabel="Chọn vị trí" disabled={!hireForm.departmentId} /></div>
              <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Ngày vào làm *</label><input type="date" max={TODAY} value={hireForm.hireDate} onChange={event => setHireForm(value => ({ ...value, hireDate: event.target.value }))} required className="w-full rounded-xl border border-white/10 bg-black/50 p-3 text-sm outline-none focus:border-brand-orange" /></div>
              <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Lương cơ bản *</label><input type="number" min="1" value={hireForm.baseSalary} onChange={event => setHireForm(value => ({ ...value, baseSalary: event.target.value }))} required placeholder="Ví dụ: 12000000" className="w-full rounded-xl border border-white/10 bg-black/50 p-3 text-sm outline-none focus:border-brand-orange" /></div>
            </div>
            <p className="rounded-xl border border-white/10 bg-white/[0.025] p-3 text-xs leading-5 text-zinc-500">Sau khi hoàn tất, nhân viên sẽ xuất hiện trong danh sách của đúng quản lý rạp và có thể được phân ca, chấm công, xin nghỉ và tính lương.</p>
          </>
        )}
      </ActionModal>

      <ActionModal open={cinemaAssignmentOpen} onClose={() => setCinemaAssignmentOpen(false)} title="Phân công rạp làm việc" description={selected ? `Chọn rạp phụ trách cho ${selected.fullName}. Thay đổi có hiệu lực ngay trên màn hình của quản lý rạp.` : ''} onSubmit={submitCinemaAssignment} submitLabel="Lưu phân công" submitting={submitting}>
        <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Rạp làm việc</label><SearchableSelect value={cinemaAssignmentId} onChange={setCinemaAssignmentId} options={cinemaOptions} placeholder="Chọn một rạp trong hệ thống" ariaLabel="Rạp làm việc mới" /></div>
        <button type="button" onClick={() => setCinemaAssignmentId('')} className={'w-full rounded-xl border p-3 text-left text-sm transition ' + (!cinemaAssignmentId ? 'border-amber-500/30 bg-amber-500/[0.08] text-amber-200' : 'border-white/10 bg-white/[0.025] text-zinc-400 hover:bg-white/5')}><span className="font-black">Chưa phân công rạp</span><span className="mt-1 block text-xs leading-5 text-zinc-500">Dùng khi nhân viên đang chờ điều chuyển. Người này sẽ không xuất hiện trong danh sách của quản lý rạp.</span></button>
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
