import { useCallback, useEffect, useState } from 'react';
import { AsyncState, Input, Select } from '@/components/common/ui/uiKit';
import { getUserAudits } from '../services/userAdminService';

const TARGET_TYPES = ['', 'USER', 'CUSTOMER', 'EMPLOYEE', 'PAYROLL', 'DEPARTMENT', 'POSITION'];

export default function AdminUserAuditPage() {
  const [query, setQuery] = useState({ keyword: '', targetType: '', page: 0, size: 20 });
  const [result, setResult] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [state, setState] = useState({ loading: true, error: '' });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const data = await getUserAudits({
        keyword: query.keyword || undefined,
        targetType: query.targetType || undefined,
        page: query.page,
        size: query.size
      });
      setResult(data || { content: [], totalPages: 0, totalElements: 0 });
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải nhật ký người dùng.' });
    }
  }, [query]);

  useEffect(() => {
    // Loading remote audit state is the synchronization performed by this effect.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <header>
        <h1 className="text-2xl font-black uppercase">Nhật ký nghiệp vụ người dùng</h1>
        <p className="mt-1 text-sm text-zinc-500">
          Theo dõi thay đổi hồ sơ, khách hàng, nhân sự và bảng lương.
        </p>
      </header>

      <div className="grid gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/50 p-4 md:grid-cols-2">
        <Input
          aria-label="Tìm nhật ký"
          placeholder="Hành động, đối tượng hoặc mã tài khoản..."
          value={query.keyword}
          onChange={(event) => setQuery((value) => ({ ...value, keyword: event.target.value, page: 0 }))}
        />
        <Select
          aria-label="Lọc loại đối tượng"
          value={query.targetType}
          onChange={(event) => setQuery((value) => ({ ...value, targetType: event.target.value, page: 0 }))}
        >
          {TARGET_TYPES.map((type) => (
            <option key={type || 'ALL'} value={type}>{type || 'Tất cả phân hệ'}</option>
          ))}
        </Select>
      </div>

      <AsyncState
        loading={state.loading}
        error={state.error}
        onRetry={load}
        empty={!result.content?.length}
        emptyMessage="Không có nhật ký phù hợp"
      >
        <div className="overflow-x-auto rounded-2xl border border-zinc-800">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-zinc-900 text-xs uppercase text-zinc-500">
              <tr>
                <th className="p-4">Thời gian</th>
                <th className="p-4">Hành động</th>
                <th className="p-4">Người thực hiện</th>
                <th className="p-4">Đối tượng</th>
                <th className="p-4">Chi tiết</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800">
              {result.content?.map((entry) => (
                <tr key={entry.id} className="hover:bg-zinc-900/50">
                  <td className="whitespace-nowrap p-4 text-zinc-400">
                    {entry.createdAt ? new Date(entry.createdAt).toLocaleString('vi-VN') : '—'}
                  </td>
                  <td className="p-4 font-bold text-white">{entry.action}</td>
                  <td className="p-4 font-mono text-brand-orange">{entry.actorAccountId || 'SYSTEM'}</td>
                  <td className="p-4 text-zinc-300">{entry.targetType} #{entry.targetId || '—'}</td>
                  <td className="max-w-sm truncate p-4 text-zinc-500" title={entry.details || ''}>
                    {entry.details || '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncState>

      <footer className="flex items-center justify-between">
        <span className="text-xs text-zinc-500">
          Trang {query.page + 1} / {Math.max(result.totalPages || 1, 1)} · {result.totalElements || 0} bản ghi
        </span>
        <div className="flex gap-2">
          <button type="button" disabled={query.page === 0}
            onClick={() => setQuery((value) => ({ ...value, page: value.page - 1 }))}
            className="rounded-lg border border-zinc-800 px-4 py-2 text-sm disabled:opacity-40">
            Trước
          </button>
          <button type="button" disabled={query.page + 1 >= result.totalPages}
            onClick={() => setQuery((value) => ({ ...value, page: value.page + 1 }))}
            className="rounded-lg border border-zinc-800 px-4 py-2 text-sm disabled:opacity-40">
            Sau
          </button>
        </div>
      </footer>
    </section>
  );
}
