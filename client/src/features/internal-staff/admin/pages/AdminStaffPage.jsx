import { useCallback, useDeferredValue, useEffect, useMemo, useState } from 'react';
import { Link, useOutletContext, useSearchParams } from 'react-router-dom';
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
import {
  createEmployeeAccount,
  getAccessProfiles,
  resendEmployeeInvitation,
  updateAccountAccessProfile,
} from '../services/authAdminService';
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
import { AlertTriangle, Building2, CalendarDays, CheckCircle2, FileText, Mail, Phone, RotateCcw, Search, Send, UserPlus, UserRoundCheck } from 'lucide-react';
import { HrHero, PersonAvatar, UatGuide } from '../components/HrWorkspace';
import { getEmployeeAvatarRole } from '../components/avatarUtils';
import { useAuth } from '@/contexts/AuthContext';

const EMPTY_PAGE = { content: [], totalPages: 0, totalElements: 0 };
const TODAY = new Date().toISOString().slice(0, 10);
const STATUS_LABELS = {
  ONBOARDING: 'Đang tiếp nhận', ACTIVE: 'Đang làm việc', ON_LEAVE: 'Nghỉ phép',
  SUSPENDED: 'Tạm ngưng', RESIGNED: 'Đã nghỉ việc', CANCELLED: 'Đã hủy tiếp nhận'
};
const ACTION_LABELS = {
  CANCEL_ONBOARDING: 'Hủy tiếp nhận nhân viên', REOPEN_ONBOARDING: 'Mở lại tiếp nhận',
  ACTIVATE: 'Kích hoạt lại', START_LEAVE: 'Bắt đầu nghỉ phép', END_LEAVE: 'Kết thúc nghỉ phép',
  SUSPEND: 'Tạm ngưng công việc', RESIGN: 'Ghi nhận nghỉ việc', TRANSFER: 'Điều chuyển',
  COMPENSATION_CHANGE: 'Điều chỉnh lương'
};

const initialHireForm = () => ({ accountId: '', departmentId: '', positionId: '', cinemaPublicId: '', hireDate: TODAY, baseSalary: '' });
const initialAccountForm = () => ({ fullName: '', email: '', accessProfileId: '' });
const initialActionForm = (type = 'TRANSFER') => ({ type, departmentId: '', positionId: '', baseSalary: '', effectiveDate: TODAY, reason: '' });

const actionsForStatus = status => ({
  ONBOARDING: ['CANCEL_ONBOARDING'],
  CANCELLED: ['REOPEN_ONBOARDING'],
  ACTIVE: ['START_LEAVE', 'SUSPEND', 'RESIGN', 'TRANSFER', 'COMPENSATION_CHANGE'],
  ON_LEAVE: ['END_LEAVE', 'SUSPEND', 'RESIGN', 'TRANSFER'],
  SUSPENDED: ['ACTIVATE', 'RESIGN'],
  RESIGNED: [],
}[status] || []);

export default function AdminStaffPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const can = useAdminAccess();
  const { accountId: currentAccountId } = useAuth();
  const canCreate = can('EMPLOYEE_CREATE');
  const canUpdate = can('EMPLOYEE_UPDATE');
  const outlet = useOutletContext();
  const confirmAction = outlet?.triggerConfirm || (() => Promise.resolve(true));
  const notify = outlet?.triggerToast || (() => undefined);
  const [filters, setFilters] = useState({ keyword: '', status: '', departmentId: '', positionId: '', cinemaPublicId: '', page: 0, size: 15 });
  const deferredKeyword = useDeferredValue(filters.keyword);
  const [result, setResult] = useState(EMPTY_PAGE);
  const [dashboard, setDashboard] = useState(null);
  const [options, setOptions] = useState({ departments: [], positions: [], accounts: [], cinemas: [], accessProfiles: [] });
  const [state, setState] = useState({ loading: true, error: '' });
  const [selected, setSelected] = useState(null);
  const [history, setHistory] = useState([]);
  const [detailLoading, setDetailLoading] = useState(false);
  const [hireOpen, setHireOpen] = useState(() => canCreate && searchParams.get('onboarding') === 'new');
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
    cinemaPublicId: filters.cinemaPublicId || undefined,
    excludeCurrentAccount: currentAccountId ? true : undefined
  }), [currentAccountId, deferredKeyword, filters.cinemaPublicId, filters.departmentId, filters.page, filters.positionId, filters.size, filters.status]);

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const [employees, departments, positions, accounts, summary, cinemaEnvelope, accessProfiles] = await Promise.all([
        getEmployees(query),
        getDepartments(),
        getPositions(),
        canCreate ? getEligibleEmployeeAccounts({ page: 0, size: 100 }) : Promise.resolve(EMPTY_PAGE),
        can('DASHBOARD_VIEW')
          ? getDashboard({ excludeCurrentAccount: Boolean(currentAccountId) })
          : Promise.resolve(null),
        adminCinemaService.getCinemas({ page: 0, size: 100, showDeleted: false, sort: 'name,asc' })
          .catch(() => null),
        canCreate ? getAccessProfiles() : Promise.resolve([])
      ]);
      setResult(employees || EMPTY_PAGE);
      setOptions({
        departments: departments || [],
        positions: positions || [],
        accounts: accounts?.content || [],
        cinemas: cinemaEnvelope?.data?.data || [],
        accessProfiles: (accessProfiles || []).filter(profile => profile.active !== false && profile.code !== 'GENERAL_STAFF')
      });
      setDashboard(summary);
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải danh sách nhân sự.' });
    }
  }, [can, canCreate, currentAccountId, query]);

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
    setHireForm(initialHireForm());
    setAccountForm(initialAccountForm());
    if (searchParams.has('onboarding')) {
      const nextParams = new URLSearchParams(searchParams);
      nextParams.delete('onboarding');
      setSearchParams(nextParams, { replace: true });
    }
  };

  const createAndResolveAccount = async () => {
    const created = await createEmployeeAccount({
      fullName: accountForm.fullName.trim(),
      email: accountForm.email.trim(),
      accessProfileId: Number(accountForm.accessProfileId)
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
          const email = accountForm.email.trim().toLowerCase();
          if (!accountForm.fullName.trim() || !email) {
            notify('Vui lòng nhập họ tên và email công việc.', 'error');
            return;
          }
          const existingEmployeePage = await getEmployees({ keyword: email, page: 0, size: 20 });
          const existingEmployee = existingEmployeePage?.content?.find(item => item.email?.trim().toLowerCase() === email);
          if (existingEmployee) {
            closeHire();
            await openEmployee(existingEmployee);
            notify(existingEmployee.status === 'CANCELLED'
              ? 'Email này thuộc một hồ sơ đã hủy. Hãy chọn “Mở lại tiếp nhận” trong hồ sơ.'
              : 'Email này đã có hồ sơ nhân viên. Hệ thống đã mở đúng hồ sơ để bạn kiểm tra.', 'error');
            return;
          }
          const reusableAccount = options.accounts.find(item => item.email?.trim().toLowerCase() === email);
          if (reusableAccount) {
            setHireForm(value => ({ ...value, accountId: String(reusableAccount.accountId) }));
            setAccountMode('existing');
            notify('Email đã có tài khoản nhưng chưa có hồ sơ. Hệ thống sẽ liên kết tài khoản này thay vì tạo trùng.');
          }
        }
        setHireStep(2);
        return;
      }
      if (hireStep === 2) {
        if (!hireForm.departmentId || !hireForm.positionId || !hireForm.cinemaPublicId
            || !hireForm.baseSalary || !accountForm.accessProfileId) {
          notify('Vui lòng chọn đủ nhóm nghiệp vụ, rạp, phòng ban, vị trí và nhập lương cơ bản.', 'error');
          return;
        }
        setHireStep(3);
        return;
      }
      let accountId = hireForm.accountId;
      if (accountMode === 'new') {
        const account = await createAndResolveAccount();
        accountId = String(account.accountId);
        setOptions(value => ({
          ...value,
          accounts: [account, ...value.accounts.filter(item => item.accountId !== account.accountId)]
        }));
        setHireForm(value => ({ ...value, accountId }));
        setAccountMode('existing');
      } else {
        await updateAccountAccessProfile(Number(accountId), Number(accountForm.accessProfileId));
      }
      await createEmployee({
        accountId: Number(accountId),
        departmentId: Number(hireForm.departmentId),
        positionId: Number(hireForm.positionId),
        hireDate: hireForm.hireDate,
        baseSalary: Number(hireForm.baseSalary),
        cinemaPublicId: hireForm.cinemaPublicId
      });
      closeHire();
      setHireForm(initialHireForm());
      await load();
      notify(accountMode === 'new'
        ? 'Đã tạo hồ sơ tiếp nhận và gửi lời mời đặt mật khẩu cho nhân viên.'
        : 'Đã liên kết tài khoản và tạo hồ sơ tiếp nhận nhân viên.');
    } catch (error) {
      notify(error?.message || 'Không thể hoàn tất tiếp nhận nhân viên. Bạn có thể thử lại mà không bị tạo trùng.', 'error');
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

  const openEmploymentAction = type => {
    setActionForm(initialActionForm(type || actionsForStatus(selected?.status)[0] || 'TRANSFER'));
    setActionOpen(true);
  };

  const resendInvitation = async () => {
    if (!selected) return;
    const approved = await confirmAction({
      title: 'Gửi lại lời mời đặt mật khẩu?',
      message: `Hệ thống sẽ vô hiệu hóa mã cũ và gửi mã mới có hiệu lực 48 giờ đến ${selected.email}.`,
      confirmLabel: 'Gửi lời mời mới',
      tone: 'warning',
    });
    if (!approved) return;
    setSubmitting(true);
    try {
      await resendEmployeeInvitation(selected.accountId);
      notify('Đã gửi lại lời mời. Nhân viên có 48 giờ để tự đặt mật khẩu.');
    } catch (error) {
      notify(error?.message || 'Không thể gửi lại lời mời.', 'error');
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
      notify(actionForm.type === 'CANCEL_ONBOARDING'
        ? 'Đã hủy tiếp nhận và vô hiệu hóa lời mời cũ.'
        : actionForm.type === 'REOPEN_ONBOARDING'
          ? 'Đã mở lại tiếp nhận. Hệ thống đang gửi lời mời mới cho nhân viên.'
          : 'Đã ghi nhận hành động nhân sự và lưu lịch sử thay đổi.');
    } catch (error) {
      notify(error?.message || 'Không thể ghi nhận hành động nhân sự.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const statusCounts = dashboard?.employeesByStatus || {};
  const selectedHireAccount = options.accounts.find(item => String(item.accountId) === String(hireForm.accountId));
  const selectedAccessProfile = options.accessProfiles.find(item => String(item.id) === String(accountForm.accessProfileId));
  const availableActionCodes = actionsForStatus(selected?.status);
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
        description="Tạo hồ sơ, phân công công việc và gửi lời mời đăng nhập trong một quy trình. Nhân viên tự đặt mật khẩu; người vận hành không cần quản lý mật khẩu ban đầu."
        actions={<><UatGuide compact />{canCreate ? <button type="button" onClick={openHire} className="flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2.5 text-sm font-black text-black hover:bg-orange-400"><UserPlus size={18} /> Tiếp nhận nhân viên mới</button> : null}</>}
      />

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {[
          ['Tất cả hồ sơ', dashboard?.totalEmployees ?? result.totalElements, 'text-blue-300'],
          ['Đang tiếp nhận', statusCounts.ONBOARDING ?? '—', 'text-orange-300'],
          ['Đang làm việc', statusCounts.ACTIVE ?? '—', 'text-emerald-300'],
          ['Đã kết thúc', dashboard ? (statusCounts.RESIGNED || 0) + (statusCounts.CANCELLED || 0) : '—', 'text-red-300']
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
                  <PersonAvatar name={employee.fullName} avatarUrl={employee.avatarUrl} role={getEmployeeAvatarRole(employee)} size="lg" />
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
          <div className="grid gap-3 sm:grid-cols-2">
            <Link to={`/admin/staff/${selected.accountId}/documents`} className="flex items-center justify-center gap-2 rounded-xl border border-white/10 px-4 py-3 text-sm font-black text-zinc-200 hover:bg-white/5"><FileText size={17} /> Tài liệu</Link>
            {selected.status === 'ONBOARDING' && canCreate ? <button type="button" disabled={submitting} onClick={resendInvitation} className="flex items-center justify-center gap-2 rounded-xl border border-sky-500/30 px-4 py-3 text-sm font-black text-sky-300 hover:bg-sky-500/10 disabled:opacity-40"><Send size={17} /> Gửi lại lời mời</button> : null}
            {selected.status === 'ONBOARDING' && canUpdate ? <button type="button" onClick={() => openEmploymentAction('CANCEL_ONBOARDING')} className="flex items-center justify-center gap-2 rounded-xl border border-red-500/30 px-4 py-3 text-sm font-black text-red-300 hover:bg-red-500/10"><AlertTriangle size={17} /> Hủy tiếp nhận</button> : null}
            {selected.status === 'CANCELLED' && canUpdate ? <button type="button" onClick={() => openEmploymentAction('REOPEN_ONBOARDING')} className="flex items-center justify-center gap-2 rounded-xl bg-brand-orange px-4 py-3 text-sm font-black text-black hover:bg-orange-500"><RotateCcw size={17} /> Mở lại tiếp nhận</button> : null}
            {canUpdate && !['ONBOARDING', 'CANCELLED', 'RESIGNED'].includes(selected.status) && availableActionCodes.length ? <button type="button" onClick={() => openEmploymentAction(availableActionCodes[0])} className="rounded-xl bg-brand-orange px-4 py-3 text-sm font-black text-black hover:bg-orange-500">Ghi nhận hành động</button> : null}
          </div>
        ) : null}
      >
        {detailLoading ? <p className="text-sm text-zinc-500">Đang tải hồ sơ…</p> : selected ? (
          <div className="space-y-7">
            <div className="rounded-2xl border border-orange-500/20 bg-gradient-to-br from-orange-500/10 to-transparent p-5">
              <div className="flex items-center gap-4"><PersonAvatar name={selected.fullName} avatarUrl={selected.avatarUrl} role={getEmployeeAvatarRole(selected)} size="lg" /><div><p className="text-lg font-black">{selected.fullName}</p><p className="mt-1 text-xs text-zinc-500">{selected.employeeCode} · {STATUS_LABELS[selected.status] || selected.status}</p></div></div>
              <div className="mt-5 grid gap-2 text-sm">
                <div className="flex items-center gap-3 rounded-xl bg-black/20 p-3 text-zinc-300"><Mail size={16} className="text-zinc-600" /> {selected.email || 'Chưa có email'}</div>
                <div className="flex items-center gap-3 rounded-xl bg-black/20 p-3 text-zinc-300"><Phone size={16} className="text-zinc-600" /> {selected.phoneNumber || 'Thông tin được ẩn theo quyền'}</div>
              </div>
            </div>
            {selected.status === 'ONBOARDING' ? <div className="flex items-start gap-3 rounded-2xl border border-orange-500/25 bg-orange-500/[0.07] p-4 text-sm leading-6 text-orange-100"><Mail size={18} className="mt-0.5 shrink-0" /><p><strong>Đang chờ nhân viên kích hoạt tài khoản.</strong><br />Bạn có thể gửi lại lời mời hoặc hủy tiếp nhận nếu kế hoạch tuyển dụng thay đổi.</p></div> : null}
            {selected.status === 'CANCELLED' ? <div className="flex items-start gap-3 rounded-2xl border border-zinc-700 bg-zinc-900/70 p-4 text-sm leading-6 text-zinc-300"><RotateCcw size={18} className="mt-0.5 shrink-0 text-zinc-500" /><p><strong>Hồ sơ được giữ lại để tránh trùng dữ liệu.</strong><br />Khi tiếp nhận lại cùng người, hãy mở lại hồ sơ này thay vì tạo mới.</p></div> : null}
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
        title={hireStep === 1 ? 'Bước 1 · Xác nhận nhân viên' : hireStep === 2 ? 'Bước 2 · Công việc & quyền' : 'Bước 3 · Kiểm tra và gửi lời mời'}
        description={hireStep === 1
          ? 'Nhập người mới hoặc liên kết một tài khoản nội bộ chưa có hồ sơ.'
          : hireStep === 2
            ? 'Khai báo nơi làm việc và nhóm nghiệp vụ trước khi gửi lời mời.'
            : 'Kiểm tra lần cuối. Nhân viên sẽ tự đặt mật khẩu qua email.'}
        onSubmit={submitHire}
        submitLabel={hireStep === 1 ? 'Tiếp tục' : hireStep === 2 ? 'Kiểm tra thông tin' : 'Tạo hồ sơ & gửi lời mời'}
        submitting={submitting}
        wide
      >
        <div className="grid grid-cols-3 gap-2" aria-label={'Tiến độ: bước ' + hireStep + ' trên 3'}>
          {['Nhân viên', 'Công việc', 'Xác nhận'].map((label, index) => (
            <div key={label} className={'rounded-lg border px-2 py-2 text-center text-[10px] font-black ' + (index + 1 === hireStep ? 'border-orange-500/50 bg-orange-500/10 text-orange-300' : index + 1 < hireStep ? 'border-emerald-500/20 text-emerald-400' : 'border-white/10 text-zinc-600')}>{index + 1}. {label}</div>
          ))}
        </div>

        {hireStep === 1 ? (
          <>
            <div className="rounded-2xl border border-blue-500/20 bg-blue-500/[0.06] p-4 text-sm leading-6 text-zinc-300">
              <p className="font-black text-blue-200">Một quy trình, không tạo trùng</p>
              <p className="mt-1 text-zinc-400">Hệ thống kiểm tra email trước khi tạo. Nếu đã có hồ sơ bị hủy hoặc tài khoản chưa được liên kết, bạn sẽ được đưa về đúng dữ liệu cũ.</p>
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
                <p className="text-xs leading-5 text-zinc-500">Có {options.accounts.length} tài khoản nội bộ chưa có hồ sơ nhân viên.</p>
                {!options.accounts.length ? (
                  <button type="button" onClick={() => setAccountMode('new')} className="w-full rounded-xl border border-amber-500/20 bg-amber-500/5 p-3 text-left text-xs font-bold leading-5 text-amber-300 hover:bg-amber-500/10">Chưa có tài khoản phù hợp. Bấm vào đây để tạo tài khoản mới ngay trong quy trình.</button>
                ) : null}
              </div>
            ) : (
              <div className="space-y-3">
                <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Họ và tên *</label><input value={accountForm.fullName} onChange={event => setAccountForm(value => ({ ...value, fullName: event.target.value }))} required placeholder="Ví dụ: Nguyễn Thị Lan" className="w-full rounded-xl border border-white/10 bg-black/50 p-3 text-sm outline-none focus:border-brand-orange" /></div>
                <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Email đăng nhập *</label><input type="email" value={accountForm.email} onChange={event => setAccountForm(value => ({ ...value, email: event.target.value }))} required placeholder="lan.nguyen@lorafilm.local" className="w-full rounded-xl border border-white/10 bg-black/50 p-3 text-sm outline-none focus:border-brand-orange" /></div>
                <p className="flex items-start gap-2 rounded-xl border border-emerald-500/20 bg-emerald-500/[0.06] p-3 text-xs leading-5 text-emerald-200"><CheckCircle2 size={16} className="mt-0.5 shrink-0" />Không cần nhập mật khẩu. Nhân viên sẽ nhận email và tự đặt mật khẩu an toàn.</p>
              </div>
            )}
          </>
        ) : hireStep === 2 ? (
          <>
            <div className="flex items-center gap-3 rounded-2xl border border-emerald-500/20 bg-emerald-500/[0.06] p-4">
              <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-emerald-500/10 text-emerald-300"><UserRoundCheck size={20} /></span>
              <div className="min-w-0 flex-1"><p className="truncate text-sm font-black text-zinc-100">{selectedHireAccount?.fullName || accountForm.fullName || 'Nhân viên mới'}</p><p className="mt-1 truncate text-xs text-zinc-500">{selectedHireAccount?.email || accountForm.email}</p></div>
              <button type="button" onClick={() => setHireStep(1)} className="rounded-lg border border-white/10 px-3 py-2 text-xs font-black text-zinc-400 hover:bg-white/5 hover:text-white">Đổi</button>
            </div>
            <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Nhóm nghiệp vụ *</label><SearchableSelect value={accountForm.accessProfileId} onChange={accessProfileId => setAccountForm(value => ({ ...value, accessProfileId }))} options={options.accessProfiles.map(profile => ({ value: String(profile.id), label: profile.name, subtitle: profile.description || 'Quyền thao tác trong ca', badge: `${profile.permissions?.length || profile.permissionIds?.length || 0} quyền` }))} placeholder="Chọn công việc nhân viên sẽ thực hiện" ariaLabel="Chọn nhóm nghiệp vụ" /></div>
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="sm:col-span-2"><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Rạp làm việc *</label><SearchableSelect value={hireForm.cinemaPublicId} onChange={cinemaPublicId => setHireForm(value => ({ ...value, cinemaPublicId }))} options={cinemaOptions} placeholder="Chọn rạp nhân viên sẽ làm việc" ariaLabel="Chọn rạp làm việc" /></div>
              <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Phòng ban *</label><SearchableSelect value={hireForm.departmentId} onChange={departmentId => setHireForm(value => ({ ...value, departmentId, positionId: '' }))} options={departmentOptions} placeholder="Chọn phòng ban" ariaLabel="Chọn phòng ban" /></div>
              <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Vị trí *</label><SearchableSelect value={hireForm.positionId} onChange={positionId => setHireForm(value => ({ ...value, positionId }))} options={positionOptions} placeholder={hireForm.departmentId ? 'Chọn vị trí' : 'Chọn phòng ban trước'} ariaLabel="Chọn vị trí" disabled={!hireForm.departmentId} /></div>
              <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Ngày vào làm *</label><input type="date" max={TODAY} value={hireForm.hireDate} onChange={event => setHireForm(value => ({ ...value, hireDate: event.target.value }))} required className="w-full rounded-xl border border-white/10 bg-black/50 p-3 text-sm outline-none focus:border-brand-orange" /></div>
              <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Lương cơ bản *</label><input type="number" min="1" value={hireForm.baseSalary} onChange={event => setHireForm(value => ({ ...value, baseSalary: event.target.value }))} required placeholder="Ví dụ: 12000000" className="w-full rounded-xl border border-white/10 bg-black/50 p-3 text-sm outline-none focus:border-brand-orange" /></div>
            </div>
            <p className="rounded-xl border border-orange-500/20 bg-orange-500/[0.05] p-3 text-xs leading-5 text-orange-100">Hồ sơ sẽ ở trạng thái <strong>Đang tiếp nhận</strong>. Chỉ sau khi nhân viên tự đặt mật khẩu, trạng thái mới chuyển thành <strong>Đang làm việc</strong>.</p>
            <button type="button" onClick={() => setHireStep(1)} className="text-xs font-black text-zinc-400 hover:text-white">← Quay lại thông tin nhân viên</button>
          </>
        ) : (
          <>
            <DetailGrid items={[
              { label: 'Nhân viên', value: selectedHireAccount?.fullName || accountForm.fullName },
              { label: 'Email nhận lời mời', value: selectedHireAccount?.email || accountForm.email },
              { label: 'Nhóm nghiệp vụ', value: selectedAccessProfile?.name || 'Chưa chọn' },
              { label: 'Rạp làm việc', value: cinemaName(hireForm.cinemaPublicId) },
              { label: 'Phòng ban', value: options.departments.find(item => String(item.id) === String(hireForm.departmentId))?.name || 'Chưa chọn' },
              { label: 'Vị trí', value: options.positions.find(item => String(item.id) === String(hireForm.positionId))?.name || 'Chưa chọn' },
              { label: 'Ngày vào làm', value: hireForm.hireDate },
              { label: 'Lương cơ bản', value: new Intl.NumberFormat('vi-VN').format(Number(hireForm.baseSalary) || 0) + ' ₫' },
            ]} />
            <div className="flex items-start gap-3 rounded-2xl border border-sky-500/20 bg-sky-500/[0.07] p-4 text-sm leading-6 text-sky-100"><Mail size={18} className="mt-0.5 shrink-0" /><p>Hệ thống sẽ tạo hồ sơ tiếp nhận, cấp đúng nhóm quyền và gửi lời mời có hiệu lực <strong>48 giờ</strong>. Không gửi mật khẩu qua email.</p></div>
            <button type="button" onClick={() => setHireStep(2)} className="text-xs font-black text-zinc-400 hover:text-white">← Quay lại công việc & quyền</button>
          </>
        )}
      </ActionModal>

      <ActionModal open={cinemaAssignmentOpen} onClose={() => setCinemaAssignmentOpen(false)} title="Phân công rạp làm việc" description={selected ? `Chọn rạp phụ trách cho ${selected.fullName}. Thay đổi có hiệu lực ngay trên màn hình của quản lý rạp.` : ''} onSubmit={submitCinemaAssignment} submitLabel="Lưu phân công" submitting={submitting}>
        <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Rạp làm việc</label><SearchableSelect value={cinemaAssignmentId} onChange={setCinemaAssignmentId} options={cinemaOptions} placeholder="Chọn một rạp trong hệ thống" ariaLabel="Rạp làm việc mới" /></div>
        <button type="button" onClick={() => setCinemaAssignmentId('')} className={'w-full rounded-xl border p-3 text-left text-sm transition ' + (!cinemaAssignmentId ? 'border-amber-500/30 bg-amber-500/[0.08] text-amber-200' : 'border-white/10 bg-white/[0.025] text-zinc-400 hover:bg-white/5')}><span className="font-black">Chưa phân công rạp</span><span className="mt-1 block text-xs leading-5 text-zinc-500">Dùng khi nhân viên đang chờ điều chuyển. Người này sẽ không xuất hiện trong danh sách của quản lý rạp.</span></button>
      </ActionModal>

      <ActionModal open={actionOpen} onClose={() => setActionOpen(false)} title={actionForm.type === 'CANCEL_ONBOARDING' ? 'Hủy tiếp nhận nhân viên' : actionForm.type === 'REOPEN_ONBOARDING' ? 'Mở lại hồ sơ tiếp nhận' : 'Ghi nhận hành động nhân sự'} description={actionForm.type === 'CANCEL_ONBOARDING' ? 'Lời mời hiện tại sẽ bị vô hiệu hóa. Hồ sơ vẫn được giữ để có thể mở lại mà không tạo trùng.' : actionForm.type === 'REOPEN_ONBOARDING' ? 'Hệ thống sẽ đưa hồ sơ về trạng thái đang tiếp nhận và gửi một lời mời mới có hiệu lực 48 giờ.' : 'Mọi hành động yêu cầu ngày hiệu lực và lý do. Hệ thống lưu snapshot trước/sau để audit.'} onSubmit={submitAction} submitLabel={actionForm.type === 'CANCEL_ONBOARDING' ? 'Xác nhận hủy tiếp nhận' : actionForm.type === 'REOPEN_ONBOARDING' ? 'Mở lại & gửi lời mời' : 'Ghi nhận hành động'} submitting={submitting} tone={['SUSPEND', 'RESIGN', 'CANCEL_ONBOARDING'].includes(actionForm.type) ? 'danger' : 'orange'}>
        <label className="block text-xs font-black uppercase tracking-wider text-zinc-500">Loại hành động *</label>
        <select value={actionForm.type} onChange={event => setActionForm(value => ({ ...value, type: event.target.value }))} disabled={['ONBOARDING', 'CANCELLED'].includes(selected?.status)} className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange disabled:opacity-60">{availableActionCodes.map(value => <option key={value} value={value}>{ACTION_LABELS[value]}</option>)}</select>
        {actionForm.type === 'TRANSFER' ? <div className="grid gap-3 sm:grid-cols-2"><select value={actionForm.departmentId} onChange={event => setActionForm(value => ({ ...value, departmentId: event.target.value, positionId: '' }))} className="rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange"><option value="">Giữ phòng ban hiện tại</option>{options.departments.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select><select value={actionForm.positionId} onChange={event => setActionForm(value => ({ ...value, positionId: event.target.value }))} className="rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange"><option value="">Giữ vị trí hiện tại</option>{options.positions.filter(item => String(item.departmentId) === String(actionForm.departmentId || selected?.departmentId)).map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div> : null}
        {actionForm.type === 'COMPENSATION_CHANGE' ? <input type="number" min="1" value={actionForm.baseSalary} onChange={event => setActionForm(value => ({ ...value, baseSalary: event.target.value }))} required placeholder="Lương cơ bản mới" className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange" /> : null}
        <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Ngày hiệu lực *</label><input type="date" max={TODAY} value={actionForm.effectiveDate} onChange={event => setActionForm(value => ({ ...value, effectiveDate: event.target.value }))} required className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange" /></div>
        <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Lý do *</label><textarea value={actionForm.reason} onChange={event => setActionForm(value => ({ ...value, reason: event.target.value }))} minLength={5} maxLength={500} required rows={4} placeholder={actionForm.type === 'CANCEL_ONBOARDING' ? 'Ví dụ: Ứng viên chưa thể nhận việc theo kế hoạch…' : actionForm.type === 'REOPEN_ONBOARDING' ? 'Ví dụ: Nhân viên xác nhận tiếp tục nhận việc…' : 'Căn cứ và ghi chú bàn giao…'} className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm leading-6 outline-none focus:border-brand-orange" /></div>
      </ActionModal>
    </section>
  );
}
