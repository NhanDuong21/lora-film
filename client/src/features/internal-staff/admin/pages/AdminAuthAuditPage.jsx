import { useCallback, useEffect, useState } from 'react';
import { getAuthAudits } from '../services/authAdminService';
import { AsyncState, Input } from '@/components/common/ui/uiKit';

export default function AdminAuthAuditPage() {
  const [query, setQuery] = useState({ keyword: '', page: 0, size: 20 });
  const [audits, setAudits] = useState({ content: [], totalPages: 0 });
  const [state, setState] = useState({ loading: true, error: '' });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const data = await getAuthAudits({
        keyword: query.keyword || undefined,
        page: query.page,
        size: query.size
      });
      setAudits(data || { content: [], totalPages: 0 });
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải nhật ký.' });
    }
  }, [query]);

  useEffect(() => {
    // Loading remote audit state is the synchronization performed by this effect.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const formatDate = (dateString) => {
    if (!dateString) return '—';
    return new Date(dateString).toLocaleString('vi-VN');
  };

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <div>
        <h1 className="text-2xl font-black uppercase">Nhật ký Truy cập (Auth Audit)</h1>
        <p className="mt-1 text-sm text-zinc-500">Xem lịch sử đăng nhập, đăng ký và các thao tác xác thực.</p>
      </div>
      
      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <Input 
          placeholder="Tìm kiếm hành động, IP, hoặc accountId..." 
          value={query.keyword}
          onChange={e => setQuery(prev => ({ ...prev, keyword: e.target.value, page: 0 }))} 
        />
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!audits.content?.length} emptyMessage="Không tìm thấy nhật ký nào">
        <div className="overflow-x-auto rounded-2xl border border-zinc-800">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-zinc-900 text-xs uppercase text-zinc-500">
              <tr>
                <th className="p-4">Thời gian</th>
                <th className="p-4">Hành động</th>
                <th className="p-4">Tài khoản (ID)</th>
                <th className="p-4">Địa chỉ IP</th>
                <th className="p-4">User Agent</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800">
              {audits.content?.map(audit => (
                <tr key={audit.id} className="hover:bg-zinc-900/50">
                  <td className="p-4 text-zinc-300">{formatDate(audit.createdAt)}</td>
                  <td className="p-4 font-bold text-white">{audit.action}</td>
                  <td className="p-4 text-brand-orange">{audit.accountId || '—'}</td>
                  <td className="p-4 text-zinc-400 font-mono text-xs">{audit.ipAddress || '—'}</td>
                  <td className="p-4 text-zinc-500 text-[10px] truncate max-w-[200px]" title={audit.userAgent}>{audit.userAgent || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncState>

      <div className="flex justify-end gap-2">
        <button disabled={query.page === 0} onClick={() => setQuery(prev => ({ ...prev, page: prev.page - 1 }))}
          className="rounded-lg border border-zinc-800 px-4 py-2 text-sm disabled:opacity-40">Trước</button>
        <button disabled={query.page + 1 >= audits.totalPages}
          onClick={() => setQuery(prev => ({ ...prev, page: prev.page + 1 }))}
          className="rounded-lg border border-zinc-800 px-4 py-2 text-sm disabled:opacity-40">Sau</button>
      </div>
    </section>
  );
}
