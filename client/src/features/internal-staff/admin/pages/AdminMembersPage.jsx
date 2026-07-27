import { useCallback, useEffect, useState } from 'react';
import { getCustomers, setCustomerBlocked } from '../services/userAdminService';
import { AsyncState, Input, Select, StatusBadge } from '@/components/common/ui/uiKit';

export default function AdminMembersPage() {
  const [query, setQuery] = useState({ keyword: '', status: '', page: 0, size: 10 });
  const [result, setResult] = useState({ content: [], totalPages: 0 });
  const [state, setState] = useState({ loading: true, error: '' });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const data = await getCustomers({
        ...query,
        keyword: query.keyword || undefined,
        status: query.status || undefined
      });
      setResult(data || { content: [], totalPages: 0 });
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải khách hàng.' });
    }
  }, [query]);

  useEffect(() => { load(); }, [load]);

  const toggleBlocked = async (customer) => {
    try {
      await setCustomerBlocked(customer.id, customer.status !== 'BLOCKED');
      await load();
    } catch (error) {
      setState(value => ({ ...value, error: error?.message || 'Không thể đổi trạng thái.' }));
    }
  };

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <div>
        <h1 className="text-2xl font-black uppercase">Danh sách khách hàng</h1>
        <p className="mt-1 text-sm text-zinc-500">Tìm kiếm và quản lý trạng thái hồ sơ thành viên.</p>
      </div>
      <div className="grid gap-3 md:grid-cols-[1fr_220px]">
        <Input value={query.keyword} placeholder="Tên, mã khách hàng hoặc số điện thoại"
          onChange={event => setQuery(value => ({ ...value, keyword: event.target.value, page: 0 }))} />
        <Select value={query.status}
          onChange={event => setQuery(value => ({ ...value, status: event.target.value, page: 0 }))}>
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Hoạt động</option>
          <option value="BLOCKED">Đã khóa</option>
          <option value="INACTIVE">Ngừng hoạt động</option>
        </Select>
      </div>
      <AsyncState loading={state.loading} error={state.error} onRetry={load}
        empty={!result.content?.length} emptyMessage="Không có khách hàng">
        <div className="overflow-x-auto rounded-2xl border border-zinc-800">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-zinc-900 text-xs uppercase text-zinc-500">
              <tr><th className="p-4">Khách hàng</th><th className="p-4">Liên hệ</th>
                <th className="p-4">Trạng thái</th><th className="p-4 text-right">Thao tác</th></tr>
            </thead>
            <tbody className="divide-y divide-zinc-800">
              {result.content?.map(customer => (
                <tr key={customer.id}>
                  <td className="p-4"><p className="font-bold">{customer.fullName}</p>
                    <p className="text-xs text-zinc-500">{customer.customerCode}</p></td>
                  <td className="p-4 text-zinc-300">{customer.phoneNumber || '—'}</td>
                  <td className="p-4"><StatusBadge status={customer.status} /></td>
                  <td className="p-4 text-right"><button type="button" onClick={() => toggleBlocked(customer)}
                    className="rounded-lg border border-zinc-700 px-3 py-2 text-xs font-bold hover:border-orange-500">
                    {customer.status === 'BLOCKED' ? 'Mở khóa' : 'Khóa'}
                  </button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncState>
      <div className="flex justify-end gap-2">
        <button disabled={query.page === 0} onClick={() => setQuery(value => ({ ...value, page: value.page - 1 }))}
          className="rounded-lg border border-zinc-800 px-4 py-2 disabled:opacity-40">Trước</button>
        <button disabled={query.page + 1 >= result.totalPages}
          onClick={() => setQuery(value => ({ ...value, page: value.page + 1 }))}
          className="rounded-lg border border-zinc-800 px-4 py-2 disabled:opacity-40">Sau</button>
      </div>
    </section>
  );
}
