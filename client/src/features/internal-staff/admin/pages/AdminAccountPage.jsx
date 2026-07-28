import { useCallback, useEffect, useState } from 'react';
import { getAccounts, updateAccountStatus, updateAccountRole, getRoles } from '../services/authAdminService';
import { AsyncState, Input, Select, StatusBadge } from '@/components/common/ui/uiKit';

export default function AdminAccountPage() {
  const [query, setQuery] = useState({ keyword: '', roleId: '', status: '', page: 0, size: 10 });
  const [accounts, setAccounts] = useState({ content: [], totalPages: 0 });
  const [roles, setRoles] = useState([]);
  const [state, setState] = useState({ loading: true, error: '' });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const [accData, rolesData] = await Promise.all([
        getAccounts({
          keyword: query.keyword || undefined,
          roleId: query.roleId || undefined,
          status: query.status || undefined,
          page: query.page,
          size: query.size
        }),
        getRoles()
      ]);
      setAccounts(accData || { content: [], totalPages: 0 });
      setRoles(rolesData || []);
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải danh sách tài khoản.' });
    }
  }, [query]);

  useEffect(() => { load(); }, [load]);

  const handleStatusChange = async (id, newStatus) => {
    if (!window.confirm(`Bạn có chắc muốn đổi trạng thái thành ${newStatus}?`)) return;
    try {
      await updateAccountStatus(id, newStatus);
      await load();
    } catch (error) {
      alert(error?.message || 'Lỗi khi đổi trạng thái');
    }
  };

  const handleRoleChange = async (id, newRoleId) => {
    if (!window.confirm('Bạn có chắc muốn đổi vai trò tài khoản này?')) return;
    try {
      await updateAccountRole(id, newRoleId);
      await load();
    } catch (error) {
      alert(error?.message || 'Lỗi khi đổi vai trò');
    }
  };

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <div>
        <h1 className="text-2xl font-black uppercase">Quản lý Tài khoản (SSO)</h1>
        <p className="mt-1 text-sm text-zinc-500">Quản lý định danh, trạng thái và vai trò của người dùng trên toàn hệ thống.</p>
      </div>
      
      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <Input 
          placeholder="Tên, Email hoặc SĐT..." 
          value={query.keyword}
          onChange={e => setQuery(prev => ({ ...prev, keyword: e.target.value, page: 0 }))} 
        />
        <Select 
          value={query.roleId}
          onChange={e => setQuery(prev => ({ ...prev, roleId: e.target.value, page: 0 }))}>
          <option value="">Tất cả vai trò</option>
          {roles.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
        </Select>
        <Select 
          value={query.status}
          onChange={e => setQuery(prev => ({ ...prev, status: e.target.value, page: 0 }))}>
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Hoạt động</option>
          <option value="INACTIVE">Chưa kích hoạt</option>
          <option value="BLOCKED">Bị khóa</option>
        </Select>
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!accounts.content?.length} emptyMessage="Không tìm thấy tài khoản nào">
        <div className="overflow-x-auto rounded-2xl border border-zinc-800">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-zinc-900 text-xs uppercase text-zinc-500">
              <tr>
                <th className="p-4">Tài khoản</th>
                <th className="p-4">Thông tin liên hệ</th>
                <th className="p-4">Vai trò (Role)</th>
                <th className="p-4">Trạng thái</th>
                <th className="p-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800">
              {accounts.content?.map(acc => (
                <tr key={acc.id} className="hover:bg-zinc-900/50">
                  <td className="p-4">
                    <p className="font-bold text-white">{acc.fullName || 'Chưa cập nhật'}</p>
                    <p className="text-[10px] text-zinc-500 font-mono">{acc.id}</p>
                  </td>
                  <td className="p-4">
                    <p>{acc.email}</p>
                    <p className="text-xs text-zinc-400">{acc.phoneNumber || 'Không có SĐT'}</p>
                  </td>
                  <td className="p-4">
                    <select
                      className="rounded bg-zinc-800 px-2 py-1 text-xs text-brand-orange focus:outline-none focus:ring-1 focus:ring-brand-orange"
                      value={acc.role?.id || ''}
                      onChange={(e) => handleRoleChange(acc.id, e.target.value)}
                    >
                      <option value="" disabled>Chọn vai trò</option>
                      {roles.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
                    </select>
                  </td>
                  <td className="p-4">
                    <StatusBadge status={acc.status} />
                  </td>
                  <td className="p-4 text-right space-x-3">
                    {acc.status === 'BLOCKED' ? (
                      <button onClick={() => handleStatusChange(acc.id, 'ACTIVE')} className="text-xs font-bold text-emerald-400 hover:underline">Mở khóa</button>
                    ) : (
                      <button onClick={() => handleStatusChange(acc.id, 'BLOCKED')} className="text-xs font-bold text-red-400 hover:underline">Khóa</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncState>

      <div className="flex justify-end gap-2">
        <button disabled={query.page === 0} onClick={() => setQuery(prev => ({ ...prev, page: prev.page - 1 }))}
          className="rounded-lg border border-zinc-800 px-4 py-2 text-sm disabled:opacity-40">Trước</button>
        <button disabled={query.page + 1 >= accounts.totalPages}
          onClick={() => setQuery(prev => ({ ...prev, page: prev.page + 1 }))}
          className="rounded-lg border border-zinc-800 px-4 py-2 text-sm disabled:opacity-40">Sau</button>
      </div>
    </section>
  );
}
