import { useCallback, useEffect, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { UserPlus } from 'lucide-react';
import {
  createEmployeeAccount,
  getAccounts,
  getRoles,
  updateAccountRole,
  updateAccountStatus
} from '../services/authAdminService';
import { ActionModal, OperationsHeader } from '../components/OperationsConsole';
import { AsyncState, Input, Select, StatusBadge } from '@/components/common/ui/uiKit';

const EMPTY_FORM = { fullName: '', email: '', password: '' };
const ROLE_LABELS = {
  'system administrator': 'Quản trị hệ thống',
  'cinema manager': 'Quản lý rạp',
  'cinema staff': 'Nhân viên rạp',
  customer: 'Khách hàng'
};
const STATUS_LABELS = {
  ACTIVE: 'Hoạt động',
  INACTIVE: 'Chưa kích hoạt',
  LOCKED: 'Bị khóa',
  DELETED: 'Đã xóa'
};
const roleLabel = role => ROLE_LABELS[String(role?.name || '').toLowerCase()] || role?.name || 'Chưa phân vai trò';

export default function AdminAccountPage() {
  const [query, setQuery] = useState({ keyword: '', roleId: '', status: '', page: 0, size: 10 });
  const [accounts, setAccounts] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [roles, setRoles] = useState([]);
  const [state, setState] = useState({ loading: true, error: '' });
  const [updatingId, setUpdatingId] = useState(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const outlet = useOutletContext();
  const confirmAction = outlet?.triggerConfirm || (() => Promise.resolve(true));
  const notify = outlet?.triggerToast || (() => undefined);

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const [accountData, roleData] = await Promise.all([
        getAccounts({
          keyword: query.keyword || undefined,
          roleId: query.roleId || undefined,
          status: query.status || undefined,
          page: query.page,
          size: query.size
        }),
        getRoles()
      ]);
      setAccounts(accountData || { content: [], totalPages: 0, totalElements: 0 });
      setRoles(roleData || []);
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải danh sách tài khoản.' });
    }
  }, [query]);

  useEffect(() => {
    // Loading remote account state is the synchronization performed by this effect.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const handleCreate = async event => {
    event.preventDefault();
    setCreating(true);
    try {
      await createEmployeeAccount({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        password: form.password
      });
      const createdEmail = form.email.trim();
      setCreateOpen(false);
      setForm(EMPTY_FORM);
      setQuery(value => ({ ...value, keyword: createdEmail, page: 0 }));
      notify('Đã tạo tài khoản nhân viên. Bạn có thể phân vai trò hoặc lập hồ sơ nhân sự tiếp theo.');
    } catch (error) {
      notify(error?.message || 'Không thể tạo tài khoản nhân viên.', 'error');
    } finally {
      setCreating(false);
    }
  };

  const handleStatusChange = async (id, newStatus) => {
    if (!await confirmAction(`Xác nhận đổi trạng thái tài khoản thành ${newStatus}?`)) return;
    setUpdatingId(id);
    try {
      await updateAccountStatus(id, newStatus);
      await load();
      notify('Trạng thái tài khoản đã được cập nhật.');
    } catch (error) {
      notify(error?.message || 'Không thể đổi trạng thái tài khoản.', 'error');
    } finally {
      setUpdatingId(null);
    }
  };

  const handleRoleChange = async (id, newRoleId) => {
    if (!await confirmAction('Xác nhận đổi vai trò của tài khoản này?')) return;
    setUpdatingId(id);
    try {
      await updateAccountRole(id, Number(newRoleId));
      await load();
      notify('Vai trò tài khoản đã được cập nhật.');
    } catch (error) {
      notify(error?.message || 'Không thể đổi vai trò tài khoản.', 'error');
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <OperationsHeader
        eyebrow="IAM · Định danh & truy cập"
        title="Tài khoản đăng nhập"
        description="Cấp định danh cho nhân viên, quản lý trạng thái và vai trò truy cập trên toàn hệ thống rạp. Hồ sơ nhân sự được lập riêng sau khi có tài khoản."
        actions={(
          <button type="button" onClick={() => setCreateOpen(true)} className="inline-flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-2.5 text-sm font-black text-black hover:bg-orange-500">
            <UserPlus size={17} /> Cấp tài khoản nhân viên
          </button>
        )}
      />

      <div className="grid gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/50 p-4 md:grid-cols-3">
        <Input aria-label="Tìm tài khoản" placeholder="Email hoặc mã tài khoản…" value={query.keyword} onChange={event => setQuery(value => ({ ...value, keyword: event.target.value, page: 0 }))} />
        <Select aria-label="Lọc vai trò" value={query.roleId} onChange={event => setQuery(value => ({ ...value, roleId: event.target.value, page: 0 }))}>
          <option value="">Tất cả vai trò</option>
          {roles.map(role => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}
        </Select>
        <Select aria-label="Lọc trạng thái" value={query.status} onChange={event => setQuery(value => ({ ...value, status: event.target.value, page: 0 }))}>
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Hoạt động</option>
          <option value="INACTIVE">Chưa kích hoạt</option>
          <option value="LOCKED">Bị khóa</option>
          <option value="DELETED">Đã xóa</option>
        </Select>
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!accounts.content?.length} emptyMessage="Không tìm thấy tài khoản" emptyDescription="Hãy thay đổi từ khóa hoặc bộ lọc và thử lại.">
        <div className="overflow-x-auto rounded-2xl border border-zinc-800">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-zinc-900 text-xs uppercase text-zinc-500">
              <tr><th className="p-4">Tài khoản</th><th className="p-4">Xác minh</th><th className="p-4">Vai trò</th><th className="p-4">Trạng thái</th><th className="p-4 text-right">Thao tác</th></tr>
            </thead>
            <tbody className="divide-y divide-zinc-800">
              {accounts.content?.map(account => (
                <tr key={account.id} className="hover:bg-zinc-900/50">
                  <td className="p-4"><p className="font-bold text-white">{account.email}</p><p className="font-mono text-[10px] text-zinc-500">ID #{account.id}</p></td>
                  <td className="p-4 text-zinc-400">{account.enabled ? 'Đã xác minh' : 'Chưa xác minh'}</td>
                  <td className="p-4">
                    <select aria-label={`Vai trò của ${account.email}`} className="rounded bg-zinc-800 px-2 py-1 text-xs text-brand-orange focus:outline-none focus:ring-1 focus:ring-brand-orange disabled:opacity-50" value={account.role?.id || ''} disabled={updatingId === account.id || account.status === 'DELETED'} onChange={event => handleRoleChange(account.id, event.target.value)}>
                      <option value="" disabled>Chọn vai trò</option>
                      {roles.map(role => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}
                    </select>
                  </td>
                  <td className="p-4"><StatusBadge status={account.status} label={STATUS_LABELS[account.status]} /></td>
                  <td className="p-4 text-right">
                    {account.status === 'LOCKED' ? (
                      <button type="button" disabled={updatingId === account.id} onClick={() => handleStatusChange(account.id, 'ACTIVE')} className="text-xs font-bold text-emerald-400 hover:underline disabled:opacity-40">Mở khóa</button>
                    ) : account.status === 'DELETED' ? (
                      <span className="text-xs text-zinc-600">Không khả dụng</span>
                    ) : (
                      <button type="button" disabled={updatingId === account.id} onClick={() => handleStatusChange(account.id, 'LOCKED')} className="text-xs font-bold text-red-400 hover:underline disabled:opacity-40">Khóa</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncState>

      <footer className="flex items-center justify-between">
        <span className="text-xs text-zinc-500">Trang {query.page + 1} / {Math.max(accounts.totalPages || 1, 1)} · {accounts.totalElements || 0} tài khoản</span>
        <div className="flex gap-2">
          <button type="button" disabled={query.page === 0} onClick={() => setQuery(value => ({ ...value, page: value.page - 1 }))} className="rounded-lg border border-zinc-800 px-4 py-2 text-sm disabled:opacity-40">Trước</button>
          <button type="button" disabled={query.page + 1 >= accounts.totalPages} onClick={() => setQuery(value => ({ ...value, page: value.page + 1 }))} className="rounded-lg border border-zinc-800 px-4 py-2 text-sm disabled:opacity-40">Sau</button>
        </div>
      </footer>

      <ActionModal open={createOpen} onClose={() => setCreateOpen(false)} title="Cấp tài khoản nhân viên" description="Tạo định danh đăng nhập ban đầu. Sau đó phân vai trò và liên kết hồ sơ tại mục Nhân viên." onSubmit={handleCreate} submitLabel="Tạo tài khoản" submitting={creating}>
        <label className="block text-xs font-black uppercase tracking-wide text-zinc-500">Họ và tên<Input className="mt-2" value={form.fullName} onChange={event => setForm(value => ({ ...value, fullName: event.target.value }))} required minLength={2} autoComplete="name" /></label>
        <label className="block text-xs font-black uppercase tracking-wide text-zinc-500">Email công việc<Input className="mt-2" type="email" value={form.email} onChange={event => setForm(value => ({ ...value, email: event.target.value }))} required autoComplete="email" /></label>
        <label className="block text-xs font-black uppercase tracking-wide text-zinc-500">Mật khẩu tạm thời<Input className="mt-2" type="password" value={form.password} onChange={event => setForm(value => ({ ...value, password: event.target.value }))} required minLength={6} autoComplete="new-password" /></label>
        <p className="text-xs leading-5 text-zinc-500">Mật khẩu chỉ dùng để khởi tạo. Nên buộc nhân viên đổi mật khẩu ở lần đăng nhập đầu theo chính sách vận hành.</p>
      </ActionModal>
    </section>
  );
}
