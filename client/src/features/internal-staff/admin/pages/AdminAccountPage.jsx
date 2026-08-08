import { useCallback, useEffect, useMemo, useState } from 'react';
import { useOutletContext, useSearchParams } from 'react-router-dom';
import {
  AlertTriangle,
  CheckCircle2,
  Info,
  KeyRound,
  LockKeyhole,
  Search,
  Settings2,
  ShieldCheck,
  UserPlus,
  Users,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import {
  createEmployeeAccount,
  getAccounts,
  getPermissions,
  getRoles,
  updateAccountRole,
  updateAccountStatus,
  updateRole,
} from '../services/authAdminService';
import {
  ActionModal,
  ConsolePagination,
  ConsolePanel,
  DetailDrawer,
  OperationsHeader,
} from '../components/OperationsConsole';
import { AsyncState, Input, Select, StatusBadge } from '@/components/common/ui/uiKit';
import useAdminAccess from '../hooks/useAdminAccess';
import {
  SYSTEM_ROLE_ORDER,
  getPermissionGroupKey,
  getPermissionGroupLabel,
  getPermissionLabel,
  getRolePresentation,
  normalizeRoleCode,
} from '../utils/systemPresentation';

const EMPTY_FORM = { fullName: '', email: '', password: '' };
const STATUS_LABELS = {
  ACTIVE: 'Hoạt động',
  INACTIVE: 'Chưa kích hoạt',
  LOCKED: 'Đã khóa',
  DELETED: 'Đã xóa',
};

const orderedRoles = roles => SYSTEM_ROLE_ORDER
  .map(code => roles.find(role => normalizeRoleCode(role) === code))
  .filter(Boolean);

const sameIds = (left, right) => {
  const a = [...left].map(Number).sort((x, y) => x - y);
  const b = [...right].map(Number).sort((x, y) => x - y);
  return a.length === b.length && a.every((value, index) => value === b[index]);
};

export default function AdminAccountPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedTab = searchParams.get('tab');
  const activeTab = ['access', 'roles', 'permissions'].includes(requestedTab) ? 'access' : 'accounts';
  const { user } = useAuth();
  const can = useAdminAccess();
  const canUpdateEmployeeAccess = can('ROLE_UPDATE');
  const [query, setQuery] = useState({ keyword: '', roleId: '', status: '', page: 0, size: 10 });
  const [accounts, setAccounts] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [roles, setRoles] = useState([]);
  const [permissions, setPermissions] = useState([]);
  const [accountState, setAccountState] = useState({ loading: true, error: '' });
  const [accessState, setAccessState] = useState({ loading: true, error: '' });
  const [updatingId, setUpdatingId] = useState(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [selectedAccount, setSelectedAccount] = useState(null);
  const [draftRoleId, setDraftRoleId] = useState('');
  const [selectedPermissionIds, setSelectedPermissionIds] = useState([]);
  const [permissionSearch, setPermissionSearch] = useState('');
  const [showTechnicalCodes, setShowTechnicalCodes] = useState(false);
  const [savingPermissions, setSavingPermissions] = useState(false);
  const outlet = useOutletContext();
  const confirmAction = outlet?.triggerConfirm || (() => Promise.resolve(true));
  const notify = outlet?.triggerToast || (() => undefined);

  const loadAccounts = useCallback(async () => {
    setAccountState({ loading: true, error: '' });
    try {
      const data = await getAccounts({
        keyword: query.keyword || undefined,
        roleId: query.roleId || undefined,
        status: query.status || undefined,
        page: query.page,
        size: query.size,
      });
      setAccounts(data || { content: [], totalPages: 0, totalElements: 0 });
      setAccountState({ loading: false, error: '' });
    } catch (error) {
      setAccountState({ loading: false, error: error?.message || 'Không thể tải danh sách tài khoản.' });
    }
  }, [query]);

  const loadAccessConfiguration = useCallback(async () => {
    setAccessState({ loading: true, error: '' });
    try {
      const [roleData, permissionData] = await Promise.all([getRoles(), getPermissions()]);
      const nextRoles = roleData || [];
      const employeeRole = nextRoles.find(role => normalizeRoleCode(role) === 'EMPLOYEE');
      setRoles(nextRoles);
      setPermissions(permissionData || []);
      setSelectedPermissionIds((employeeRole?.permissions || []).map(permission => permission.id));
      setAccessState({ loading: false, error: '' });
    } catch (error) {
      setAccessState({ loading: false, error: error?.message || 'Không thể tải cấu hình phân quyền.' });
    }
  }, []);

  useEffect(() => {
    // Remote account state is synchronized whenever the operator changes a filter.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadAccounts();
  }, [loadAccounts]);

  useEffect(() => {
    // Roles and permissions are synchronized once for the operational console.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadAccessConfiguration();
  }, [loadAccessConfiguration]);

  const systemRoles = useMemo(() => orderedRoles(roles), [roles]);
  const employeeRole = useMemo(
    () => roles.find(role => normalizeRoleCode(role) === 'EMPLOYEE'),
    [roles],
  );
  const employeePermissionIds = useMemo(
    () => (employeeRole?.permissions || []).map(permission => permission.id),
    [employeeRole],
  );
  const permissionChanges = useMemo(() => {
    const current = new Set(employeePermissionIds.map(Number));
    const selected = new Set(selectedPermissionIds.map(Number));
    return {
      added: [...selected].filter(id => !current.has(id)).length,
      removed: [...current].filter(id => !selected.has(id)).length,
    };
  }, [employeePermissionIds, selectedPermissionIds]);

  const permissionGroups = useMemo(() => {
    const keyword = permissionSearch.trim().toLocaleLowerCase('vi');
    const groups = new Map();
    permissions
      .filter(permission => {
        if (!keyword) return true;
        return `${getPermissionLabel(permission)} ${permission.code} ${permission.module}`
          .toLocaleLowerCase('vi')
          .includes(keyword);
      })
      .forEach(permission => {
        const key = getPermissionGroupKey(permission);
        if (!groups.has(key)) groups.set(key, []);
        groups.get(key).push(permission);
      });
    return [...groups.entries()]
      .map(([key, items]) => ({
        key,
        label: getPermissionGroupLabel(key),
        items: items.sort((a, b) => getPermissionLabel(a).localeCompare(getPermissionLabel(b), 'vi')),
      }))
      .sort((a, b) => a.label.localeCompare(b.label, 'vi'));
  }, [permissionSearch, permissions]);

  const handleCreate = async event => {
    event.preventDefault();
    setCreating(true);
    try {
      await createEmployeeAccount({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        password: form.password,
      });
      const createdEmail = form.email.trim();
      setCreateOpen(false);
      setForm(EMPTY_FORM);
      setQuery(value => ({ ...value, keyword: createdEmail, roleId: '', page: 0 }));
      notify('Đã cấp tài khoản nhân viên với quyền EMPLOYEE hiện hành.');
    } catch (error) {
      notify(error?.message || 'Không thể cấp tài khoản nhân viên.', 'error');
    } finally {
      setCreating(false);
    }
  };

  const isCurrentAccount = account => {
    if (!account) return false;
    const knownIds = [user?.id, user?.accountId].filter(value => value !== undefined && value !== null).map(Number);
    return knownIds.includes(Number(account.id)) || Boolean(user?.email && user.email === account.email);
  };

  const openAccount = account => {
    setSelectedAccount(account);
    setDraftRoleId(String(account.role?.id || ''));
  };

  const handleRoleSave = async () => {
    if (!selectedAccount || !draftRoleId || String(selectedAccount.role?.id || '') === draftRoleId) {
      setSelectedAccount(null);
      return;
    }
    const nextRole = roles.find(role => String(role.id) === draftRoleId);
    if (isCurrentAccount(selectedAccount) && normalizeRoleCode(nextRole) !== 'ADMIN') {
      notify('Bạn không thể tự thu hồi quyền quản trị của tài khoản đang sử dụng.', 'error');
      return;
    }
    const approved = await confirmAction({
      title: 'Xác nhận thay đổi vai trò',
      message: `${selectedAccount.email} sẽ chuyển sang “${getRolePresentation(nextRole).label}”. Phiên đăng nhập hiện tại của tài khoản này sẽ bị thu hồi.`,
      confirmLabel: 'Đổi vai trò',
      tone: 'warning',
    });
    if (!approved) return;
    setUpdatingId(selectedAccount.id);
    try {
      await updateAccountRole(selectedAccount.id, Number(draftRoleId));
      setSelectedAccount(null);
      await loadAccounts();
      notify('Đã cập nhật vai trò tài khoản.');
    } catch (error) {
      notify(error?.message || 'Không thể đổi vai trò tài khoản.', 'error');
    } finally {
      setUpdatingId(null);
    }
  };

  const handleStatusChange = async (account, newStatus) => {
    if (newStatus === 'LOCKED' && isCurrentAccount(account)) {
      notify('Bạn không thể tự khóa tài khoản đang sử dụng.', 'error');
      return;
    }
    const approved = await confirmAction({
      title: newStatus === 'LOCKED' ? 'Khóa tài khoản?' : 'Mở lại tài khoản?',
      message: newStatus === 'LOCKED'
        ? `${account.email} sẽ không thể đăng nhập cho đến khi được mở khóa.`
        : `${account.email} sẽ có thể đăng nhập trở lại theo vai trò hiện tại.`,
      confirmLabel: newStatus === 'LOCKED' ? 'Khóa tài khoản' : 'Mở khóa',
      tone: newStatus === 'LOCKED' ? 'danger' : 'warning',
    });
    if (!approved) return;
    setUpdatingId(account.id);
    try {
      await updateAccountStatus(account.id, newStatus);
      setSelectedAccount(null);
      await loadAccounts();
      notify(newStatus === 'LOCKED' ? 'Đã khóa tài khoản.' : 'Đã mở khóa tài khoản.');
    } catch (error) {
      notify(error?.message || 'Không thể đổi trạng thái tài khoản.', 'error');
    } finally {
      setUpdatingId(null);
    }
  };

  const togglePermission = permissionId => {
    setSelectedPermissionIds(current => current.includes(permissionId)
      ? current.filter(id => id !== permissionId)
      : [...current, permissionId]);
  };

  const togglePermissionGroup = items => {
    const itemIds = items.map(item => item.id);
    const allSelected = itemIds.every(id => selectedPermissionIds.includes(id));
    setSelectedPermissionIds(current => allSelected
      ? current.filter(id => !itemIds.includes(id))
      : [...new Set([...current, ...itemIds])]);
  };

  const saveEmployeePermissions = async () => {
    if (!employeeRole || sameIds(employeePermissionIds, selectedPermissionIds)) return;
    const approved = await confirmAction({
      title: 'Áp dụng quyền cho toàn bộ nhân viên?',
      message: `Thay đổi này sẽ cấp thêm ${permissionChanges.added} quyền và thu hồi ${permissionChanges.removed} quyền của tất cả tài khoản EMPLOYEE. Các phiên đăng nhập của nhân viên sẽ được làm mới.`,
      confirmLabel: 'Áp dụng thay đổi',
      tone: 'warning',
    });
    if (!approved) return;
    setSavingPermissions(true);
    try {
      await updateRole(employeeRole.id, {
        code: employeeRole.code,
        name: employeeRole.name,
        description: employeeRole.description,
        permissionIds: selectedPermissionIds,
      });
      await loadAccessConfiguration();
      notify('Đã cập nhật quyền cho toàn bộ nhân viên.');
    } catch (error) {
      notify(error?.message || 'Không thể cập nhật quyền nhân viên.', 'error');
    } finally {
      setSavingPermissions(false);
    }
  };

  const selectedRole = roles.find(role => String(role.id) === draftRoleId);
  const selectedRoleInfo = getRolePresentation(selectedRole);
  const isAccessChanged = !sameIds(employeePermissionIds, selectedPermissionIds);

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <OperationsHeader
        eyebrow="Hệ thống · Tài khoản và truy cập"
        title="Tài khoản & phân quyền"
        description="Cấp tài khoản, chọn đúng loại người dùng và kiểm soát những nghiệp vụ nhân viên được phép thực hiện bằng một quy trình thống nhất."
        actions={activeTab === 'accounts' ? (
          <button type="button" onClick={() => setCreateOpen(true)} className="inline-flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-2.5 text-sm font-black text-black hover:bg-orange-500">
            <UserPlus size={17} /> Cấp tài khoản nhân viên
          </button>
        ) : null}
      />

      <div className="flex w-fit gap-1 rounded-xl border border-white/10 bg-white/[0.025] p-1">
        <button type="button" onClick={() => setSearchParams({ tab: 'accounts' })} className={`rounded-lg px-4 py-2 text-sm font-bold transition-colors ${activeTab === 'accounts' ? 'bg-brand-orange text-black' : 'text-zinc-400 hover:text-white'}`}>
          Danh sách tài khoản
        </button>
        <button type="button" onClick={() => setSearchParams({ tab: 'access' })} className={`rounded-lg px-4 py-2 text-sm font-bold transition-colors ${activeTab === 'access' ? 'bg-brand-orange text-black' : 'text-zinc-400 hover:text-white'}`}>
          Vai trò & quyền nhân viên
        </button>
      </div>

      {activeTab === 'accounts' ? (
        <>
          <ConsolePanel className="grid gap-px overflow-hidden bg-white/10 md:grid-cols-3">
            {[
              { step: '01', title: 'Cấp tài khoản', description: 'Tài khoản mới luôn bắt đầu với vai trò Nhân viên.' },
              { step: '02', title: 'Chọn đúng vai trò', description: 'Chỉ chuyển sang Quản lý rạp hoặc Quản trị khi thực sự cần.' },
              { step: '03', title: 'Kiểm tra quyền chung', description: 'Mọi EMPLOYEE dùng chung cấu hình quyền ở tab bên cạnh.' },
            ].map(item => (
              <div key={item.step} className="bg-[#0b0b0e] p-5">
                <span className="text-[10px] font-black tracking-[0.2em] text-brand-orange">BƯỚC {item.step}</span>
                <p className="mt-2 font-black text-white">{item.title}</p>
                <p className="mt-1 text-xs leading-5 text-zinc-500">{item.description}</p>
              </div>
            ))}
          </ConsolePanel>

          <div className="grid gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/50 p-4 md:grid-cols-3">
            <Input aria-label="Tìm tài khoản" placeholder="Tìm theo email tài khoản…" value={query.keyword} onChange={event => setQuery(value => ({ ...value, keyword: event.target.value, page: 0 }))} />
            <Select aria-label="Lọc vai trò" value={query.roleId} onChange={event => setQuery(value => ({ ...value, roleId: event.target.value, page: 0 }))}>
              <option value="">Tất cả vai trò</option>
              {systemRoles.map(role => <option key={role.id} value={role.id}>{getRolePresentation(role).label}</option>)}
            </Select>
            <Select aria-label="Lọc trạng thái" value={query.status} onChange={event => setQuery(value => ({ ...value, status: event.target.value, page: 0 }))}>
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Hoạt động</option>
              <option value="INACTIVE">Chưa kích hoạt</option>
              <option value="LOCKED">Đã khóa</option>
              <option value="DELETED">Đã xóa</option>
            </Select>
          </div>

          <AsyncState loading={accountState.loading} error={accountState.error} onRetry={loadAccounts} empty={!accounts.content?.length} emptyMessage="Không tìm thấy tài khoản" emptyDescription="Hãy thay đổi từ khóa hoặc bộ lọc và thử lại.">
            <ConsolePanel className="overflow-hidden">
              <div className="overflow-x-auto">
                <table className="min-w-full text-left text-sm">
                  <thead className="bg-white/[0.035] text-[10px] uppercase tracking-wider text-zinc-500">
                    <tr><th className="p-4">Tài khoản</th><th className="p-4">Loại người dùng</th><th className="p-4">Phạm vi truy cập</th><th className="p-4">Trạng thái</th><th className="p-4 text-right">Thao tác</th></tr>
                  </thead>
                  <tbody className="divide-y divide-white/10">
                    {accounts.content?.map(account => {
                      const role = roles.find(item => item.id === account.role?.id) || account.role;
                      const roleInfo = getRolePresentation(role);
                      return (
                        <tr key={account.id} className="hover:bg-white/[0.025]">
                          <td className="p-4"><p className="font-bold text-white">{account.email}</p><p className="mt-1 text-[10px] text-zinc-600">Mã tài khoản #{account.id} · {account.enabled ? 'Đã xác minh' : 'Chưa xác minh'}</p></td>
                          <td className="p-4"><span className="inline-flex rounded-full border border-brand-orange/25 bg-brand-orange/10 px-2.5 py-1 text-xs font-bold text-brand-orange">{roleInfo.label}</span></td>
                          <td className="p-4"><p className="text-zinc-300">{roleInfo.scope}</p><p className="mt-1 max-w-xs text-xs text-zinc-600">{roleInfo.description}</p></td>
                          <td className="p-4"><StatusBadge status={account.status} label={STATUS_LABELS[account.status]} /></td>
                          <td className="p-4 text-right"><button type="button" onClick={() => openAccount(account)} className="inline-flex items-center gap-1.5 rounded-lg border border-white/10 px-3 py-2 text-xs font-bold text-zinc-300 hover:border-brand-orange/40 hover:text-brand-orange"><Settings2 size={14} /> Quản lý</button></td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
              <ConsolePagination page={query.page} totalPages={accounts.totalPages} totalElements={accounts.totalElements} onPage={page => setQuery(value => ({ ...value, page }))} />
            </ConsolePanel>
          </AsyncState>
        </>
      ) : (
        <AsyncState loading={accessState.loading} error={accessState.error} onRetry={loadAccessConfiguration} empty={!roles.length} emptyMessage="Chưa có cấu hình vai trò">
          <div className="space-y-6">
            <div>
              <div className="mb-3 flex items-center gap-2"><ShieldCheck className="text-brand-orange" size={19} /><h2 className="text-lg font-black text-white">Bốn vai trò cố định của hệ thống</h2></div>
              <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                {systemRoles.map(role => {
                  const code = normalizeRoleCode(role);
                  const info = getRolePresentation(role);
                  return (
                    <article key={role.id} className={`rounded-2xl border p-5 ${code === 'EMPLOYEE' ? 'border-brand-orange/40 bg-brand-orange/[0.06]' : 'border-white/10 bg-white/[0.025]'}`}>
                      <div className="flex items-start justify-between gap-3">
                        <div><p className="text-[10px] font-black tracking-[0.18em] text-zinc-600">{code}</p><h3 className="mt-1 font-black text-white">{info.label}</h3></div>
                        {code === 'EMPLOYEE' ? <span className="rounded-full bg-brand-orange px-2 py-1 text-[9px] font-black text-black">CÓ THỂ CẤU HÌNH</span> : <LockKeyhole size={16} className="text-zinc-600" />}
                      </div>
                      <p className="mt-3 text-xs leading-5 text-zinc-400">{info.description}</p>
                      <p className="mt-3 border-t border-white/10 pt-3 text-[11px] font-semibold text-zinc-500">{info.scope}</p>
                    </article>
                  );
                })}
              </div>
            </div>

            <ConsolePanel className="overflow-hidden">
              <header className="flex flex-col gap-4 border-b border-white/10 p-5 xl:flex-row xl:items-start xl:justify-between">
                <div className="max-w-2xl">
                  <div className="flex items-center gap-2"><Users size={19} className="text-brand-orange" /><h2 className="text-lg font-black text-white">Nhân viên được phép làm gì?</h2></div>
                  <p className="mt-2 text-sm leading-6 text-zinc-400">Các lựa chọn dưới đây áp dụng cho <strong className="text-white">toàn bộ tài khoản EMPLOYEE</strong>. Chỉ cấp những nghiệp vụ nhân viên thực sự cần trong ca làm việc.</p>
                </div>
                <div className="rounded-xl border border-amber-500/20 bg-amber-500/10 px-4 py-3 text-xs leading-5 text-amber-200">
                  <div className="flex items-start gap-2"><AlertTriangle size={16} className="mt-0.5 shrink-0" /><span>Thay đổi quyền sẽ làm mới phiên đăng nhập của nhân viên để áp dụng ngay.</span></div>
                </div>
              </header>

              <div className="flex flex-col gap-3 border-b border-white/10 p-4 md:flex-row md:items-center md:justify-between">
                <div className="relative w-full max-w-lg"><Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-600" /><Input aria-label="Tìm quyền" className="pl-10" placeholder="Tìm theo công việc, ví dụ: chấm công, bán vé…" value={permissionSearch} onChange={event => setPermissionSearch(event.target.value)} /></div>
                <label className="inline-flex items-center gap-2 text-xs text-zinc-500"><input type="checkbox" checked={showTechnicalCodes} onChange={event => setShowTechnicalCodes(event.target.checked)} className="rounded border-zinc-700 bg-zinc-900 text-brand-orange focus:ring-brand-orange" /> Hiện mã kỹ thuật</label>
              </div>

              <div className="grid gap-4 p-5 xl:grid-cols-2">
                {permissionGroups.map(group => {
                  const selectedCount = group.items.filter(item => selectedPermissionIds.includes(item.id)).length;
                  const allSelected = selectedCount === group.items.length;
                  return (
                    <section key={group.key} className="overflow-hidden rounded-xl border border-white/10 bg-white/[0.02]">
                      <header className="flex items-center justify-between border-b border-white/10 px-4 py-3">
                        <div><h3 className="text-sm font-black text-white">{group.label}</h3><p className="mt-0.5 text-[10px] text-zinc-600">Đã chọn {selectedCount}/{group.items.length}</p></div>
                        <button type="button" disabled={!canUpdateEmployeeAccess} onClick={() => togglePermissionGroup(group.items)} className="text-xs font-bold text-brand-orange hover:underline disabled:text-zinc-700">{allSelected ? 'Bỏ chọn nhóm' : 'Chọn cả nhóm'}</button>
                      </header>
                      <div className="divide-y divide-white/5">
                        {group.items.map(permission => (
                          <label key={permission.id} className="flex cursor-pointer items-start gap-3 px-4 py-3 hover:bg-white/[0.025]">
                            <input type="checkbox" disabled={!canUpdateEmployeeAccess} checked={selectedPermissionIds.includes(permission.id)} onChange={() => togglePermission(permission.id)} className="mt-0.5 rounded border-zinc-700 bg-zinc-900 text-brand-orange focus:ring-brand-orange disabled:opacity-40" />
                            <span className="min-w-0"><span className="block text-sm font-semibold text-zinc-200">{getPermissionLabel(permission)}</span>{showTechnicalCodes ? <span className="mt-1 block font-mono text-[10px] text-zinc-600">{permission.code}</span> : null}</span>
                          </label>
                        ))}
                      </div>
                    </section>
                  );
                })}
              </div>

              <footer className="sticky bottom-0 flex flex-col gap-3 border-t border-white/10 bg-[#0b0b0e]/95 p-5 backdrop-blur md:flex-row md:items-center md:justify-between">
                <div className="flex items-center gap-2 text-sm">
                  {isAccessChanged ? <AlertTriangle size={17} className="text-amber-400" /> : <CheckCircle2 size={17} className="text-emerald-400" />}
                  <span className={isAccessChanged ? 'text-amber-200' : 'text-zinc-500'}>{isAccessChanged ? `Chưa lưu: cấp thêm ${permissionChanges.added}, thu hồi ${permissionChanges.removed} quyền` : `${selectedPermissionIds.length} quyền đang áp dụng cho EMPLOYEE`}</span>
                </div>
                {canUpdateEmployeeAccess ? <button type="button" disabled={!isAccessChanged || savingPermissions} onClick={saveEmployeePermissions} className="rounded-xl bg-brand-orange px-5 py-2.5 text-sm font-black text-black hover:bg-orange-500 disabled:cursor-not-allowed disabled:opacity-40">{savingPermissions ? 'Đang áp dụng…' : 'Áp dụng quyền nhân viên'}</button> : <span className="text-xs text-zinc-600">Bạn chỉ có quyền xem cấu hình này.</span>}
              </footer>
            </ConsolePanel>
          </div>
        </AsyncState>
      )}

      <DetailDrawer
        open={Boolean(selectedAccount)}
        onClose={() => setSelectedAccount(null)}
        title="Quản lý tài khoản"
        subtitle={selectedAccount?.email}
        footer={selectedAccount ? (
          <div className="flex flex-wrap items-center justify-between gap-3">
            {selectedAccount.status === 'LOCKED' ? (
              <button type="button" disabled={updatingId === selectedAccount.id} onClick={() => handleStatusChange(selectedAccount, 'ACTIVE')} className="rounded-xl border border-emerald-500/30 px-4 py-2.5 text-sm font-bold text-emerald-400 hover:bg-emerald-500/10 disabled:opacity-40">Mở khóa tài khoản</button>
            ) : selectedAccount.status !== 'DELETED' ? (
              <button type="button" disabled={updatingId === selectedAccount.id} onClick={() => handleStatusChange(selectedAccount, 'LOCKED')} className="rounded-xl border border-red-500/30 px-4 py-2.5 text-sm font-bold text-red-400 hover:bg-red-500/10 disabled:opacity-40">Khóa tài khoản</button>
            ) : <span className="text-xs text-zinc-600">Tài khoản đã xóa</span>}
            <button type="button" disabled={updatingId === selectedAccount.id || selectedAccount.status === 'DELETED' || String(selectedAccount.role?.id || '') === draftRoleId} onClick={handleRoleSave} className="rounded-xl bg-brand-orange px-4 py-2.5 text-sm font-black text-black hover:bg-orange-500 disabled:opacity-40">Lưu vai trò</button>
          </div>
        ) : null}
      >
        {selectedAccount ? (
          <div className="space-y-6">
            <div className="rounded-xl border border-white/10 bg-white/[0.025] p-4">
              <p className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Tài khoản</p><p className="mt-1 font-bold text-white">{selectedAccount.email}</p>
              <div className="mt-3 flex items-center gap-2"><StatusBadge status={selectedAccount.status} label={STATUS_LABELS[selectedAccount.status]} /><span className="text-xs text-zinc-600">Mã #{selectedAccount.id}</span></div>
            </div>
            <div>
              <h3 className="text-sm font-black text-white">Chọn loại người dùng</h3>
              <p className="mt-1 text-xs leading-5 text-zinc-500">Vai trò quyết định phạm vi lớn của tài khoản. Không dùng vai trò để thể hiện chức danh công việc.</p>
              <div className="mt-3 space-y-2">
                {systemRoles.map(role => {
                  const info = getRolePresentation(role);
                  const checked = String(role.id) === draftRoleId;
                  return (
                    <button key={role.id} type="button" disabled={selectedAccount.status === 'DELETED'} onClick={() => setDraftRoleId(String(role.id))} className={`w-full rounded-xl border p-4 text-left transition-colors ${checked ? 'border-brand-orange/50 bg-brand-orange/10' : 'border-white/10 bg-white/[0.02] hover:border-white/20'}`}>
                      <span className="flex items-center justify-between gap-3"><span className="font-bold text-white">{info.label}</span>{checked ? <CheckCircle2 size={17} className="text-brand-orange" /> : null}</span>
                      <span className="mt-1 block text-xs leading-5 text-zinc-500">{info.description}</span>
                    </button>
                  );
                })}
              </div>
            </div>
            {normalizeRoleCode(selectedRole) === 'EMPLOYEE' ? (
              <div className="flex items-start gap-3 rounded-xl border border-sky-500/20 bg-sky-500/10 p-4 text-sm leading-6 text-sky-100"><Info size={18} className="mt-0.5 shrink-0" /><p>Tài khoản này sẽ nhận toàn bộ quyền đang cấu hình tại tab <strong>Vai trò & quyền nhân viên</strong>.</p></div>
            ) : null}
            {normalizeRoleCode(selectedRole) === 'ADMIN' ? (
              <div className="flex items-start gap-3 rounded-xl border border-amber-500/20 bg-amber-500/10 p-4 text-sm leading-6 text-amber-100"><AlertTriangle size={18} className="mt-0.5 shrink-0" /><p>Quản trị hệ thống có toàn quyền. Chỉ cấp vai trò này cho người chịu trách nhiệm cao nhất.</p></div>
            ) : null}
            <div className="rounded-xl border border-white/10 p-4"><p className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Phạm vi sau khi lưu</p><p className="mt-2 font-bold text-zinc-200">{selectedRoleInfo.scope}</p></div>
          </div>
        ) : null}
      </DetailDrawer>

      <ActionModal open={createOpen} onClose={() => setCreateOpen(false)} title="Cấp tài khoản nhân viên" description="Tài khoản mới tự động nhận vai trò Nhân viên và bộ quyền EMPLOYEE đang áp dụng. Hồ sơ nhân sự có thể được hoàn thiện sau." onSubmit={handleCreate} submitLabel="Cấp tài khoản" submitting={creating}>
        <div className="flex items-start gap-3 rounded-xl border border-brand-orange/20 bg-brand-orange/10 p-4 text-xs leading-5 text-orange-100"><KeyRound size={17} className="mt-0.5 shrink-0" /><span>Không cần chọn vai trò ở bước này. Hệ thống luôn cấp đúng vai trò EMPLOYEE để tránh nhầm quyền.</span></div>
        <label className="block text-xs font-black uppercase tracking-wide text-zinc-500">Họ và tên<Input className="mt-2" value={form.fullName} onChange={event => setForm(value => ({ ...value, fullName: event.target.value }))} required minLength={2} autoComplete="name" placeholder="Ví dụ: Nguyễn Văn An" /></label>
        <label className="block text-xs font-black uppercase tracking-wide text-zinc-500">Email công việc<Input className="mt-2" type="email" value={form.email} onChange={event => setForm(value => ({ ...value, email: event.target.value }))} required autoComplete="email" placeholder="ten.nhanvien@lorafilm.local" /></label>
        <label className="block text-xs font-black uppercase tracking-wide text-zinc-500">Mật khẩu tạm thời<Input className="mt-2" type="password" value={form.password} onChange={event => setForm(value => ({ ...value, password: event.target.value }))} required minLength={6} autoComplete="new-password" /></label>
        <p className="text-xs leading-5 text-zinc-500">Gửi mật khẩu qua kênh nội bộ an toàn và yêu cầu nhân viên đổi mật khẩu ngay lần đăng nhập đầu tiên.</p>
      </ActionModal>
    </section>
  );
}
