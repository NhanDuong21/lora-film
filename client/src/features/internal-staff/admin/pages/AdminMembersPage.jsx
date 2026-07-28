import { useCallback, useEffect, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { getCustomers, setCustomerBlocked, getDashboard } from '../services/userAdminService';
import { AsyncState, StatusBadge } from '@/components/common/ui/uiKit';
import AdminStatCard from '../components/AdminStatCard';
import useAdminAccess from '../hooks/useAdminAccess';
import { Users, UserPlus, UserCheck, UserX, Search, Filter, ShieldBan, ShieldCheck } from 'lucide-react';

export default function AdminMembersPage() {
  const can = useAdminAccess();
  const canUpdateCustomers = can('CUSTOMER_UPDATE');
  const [query, setQuery] = useState({ keyword: '', status: '', page: 0, size: 10 });
  const [result, setResult] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [state, setState] = useState({ loading: true, error: '' });
  const [stats, setStats] = useState({ total: 0, active: 0, blocked: 0, new: 0 });
  const outlet = useOutletContext();
  const confirmAction = outlet?.triggerConfirm || (() => Promise.resolve(true));
  const notify = outlet?.triggerToast || (() => undefined);

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const [data, dashboard] = await Promise.all([
        getCustomers({
          ...query,
          keyword: query.keyword || undefined,
          status: query.status || undefined
        }),
        can('DASHBOARD_VIEW') ? getDashboard() : Promise.resolve(null)
      ]);
      setResult(data || { content: [], totalPages: 0, totalElements: 0 });
      
      // Calculate basic stats for the current view if we don't have a dedicated endpoint
      if (query.page === 0 && !query.keyword && !query.status) {
        setStats({
          total: dashboard?.totalCustomers ?? data.totalElements ?? 0,
          active: dashboard?.activeCustomers ?? data.content.filter(c => c.status === 'ACTIVE').length,
          blocked: dashboard?.blockedCustomers ?? data.content.filter(c => c.status === 'BLOCKED').length,
          new: data.content.filter(c => {
             if (!c.joinedAt) return false;
             return (new Date() - new Date(c.joinedAt)) / (1000 * 60 * 60 * 24) < 7;
          }).length || 0
        });
      }
      
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải khách hàng.' });
    }
  }, [can, query]);

  useEffect(() => { load(); }, [load]);

  const toggleBlocked = async (customer) => {
    const nextAction = customer.status === 'BLOCKED' ? 'mở khóa' : 'khóa';
    if (!await confirmAction(`Xác nhận ${nextAction} khách hàng ${customer.customerCode}?`)) return;
    try {
      await setCustomerBlocked(customer.id, customer.status !== 'BLOCKED');
      await load();
      notify(`Đã ${nextAction} khách hàng.`);
    } catch (error) {
      setState(value => ({ ...value, error: error?.message || 'Không thể đổi trạng thái.' }));
    }
  };

  const exportCurrentPage = () => {
    const rows = [
      ['customerCode', 'fullName', 'email', 'phoneNumber', 'status', 'joinedAt'],
      ...(result.content || []).map(customer => [
        customer.customerCode,
        customer.fullName,
        customer.email,
        customer.phoneNumber,
        customer.status,
        customer.joinedAt
      ])
    ];
    const csv = rows.map(row => row.map(value => `"${String(value ?? '').replaceAll('"', '""')}"`).join(',')).join('\n');
    const url = URL.createObjectURL(new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `customers-page-${query.page + 1}.csv`;
    link.click();
    URL.revokeObjectURL(url);
    notify('Đã xuất dữ liệu trang hiện tại.');
  };

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-[#050506] p-6 text-white md:p-8">
      <header className="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
        <div>
          <h1 className="text-2xl font-black uppercase tracking-wider text-white">Quản lý <span className="text-brand-orange">Khách Hàng</span></h1>
          <p className="mt-1 text-sm text-zinc-500">Xem danh sách, tìm kiếm và quản lý trạng thái tài khoản thành viên.</p>
        </div>
        <div className="flex gap-2">
          <button type="button" onClick={exportCurrentPage} disabled={!result.content?.length} className="px-4 py-2 rounded-xl border border-zinc-800 bg-zinc-900 text-sm font-bold hover:bg-zinc-800 transition-colors flex items-center gap-2 text-zinc-300 disabled:opacity-40">
             Xuất trang hiện tại
          </button>
        </div>
      </header>

      {/* Dashboard Statistics */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <AdminStatCard title="Tổng thành viên" value={stats.total} icon={Users} colorClass="bg-blue-500/10 text-blue-500 border-blue-500/20" />
        <AdminStatCard title="Thành viên mới (trang)" value={stats.new} icon={UserPlus} colorClass="bg-emerald-500/10 text-emerald-500 border-emerald-500/20" />
        <AdminStatCard title="Đang hoạt động" value={stats.active} icon={UserCheck} colorClass="bg-brand-orange/10 text-brand-orange border-brand-orange/20" />
        <AdminStatCard title="Bị khóa" value={stats.blocked} icon={UserX} colorClass="bg-red-500/10 text-red-500 border-red-500/20" />
      </div>

      <div className="bg-zinc-900/50 border border-zinc-800 rounded-2xl p-4 flex flex-col md:flex-row gap-3">
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500 w-4 h-4" />
          <input 
            value={query.keyword} 
            placeholder="Tìm theo tên, email, sđt, CCCD..."
            onChange={event => setQuery(value => ({ ...value, keyword: event.target.value, page: 0 }))} 
            className="w-full bg-zinc-950 border border-zinc-800 rounded-xl h-10 pl-10 pr-4 text-sm text-zinc-100 placeholder:text-zinc-600 focus:border-brand-orange outline-none transition-colors"
          />
        </div>
        <div className="w-full md:w-56 relative">
          <Filter className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500 w-4 h-4 z-10 pointer-events-none" />
          <select 
            value={query.status}
            onChange={event => setQuery(value => ({ ...value, status: event.target.value, page: 0 }))}
            className="w-full bg-zinc-950 border border-zinc-800 rounded-xl h-10 pl-10 pr-4 text-sm text-zinc-100 outline-none focus:border-brand-orange appearance-none relative"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="ACTIVE">Hoạt động</option>
            <option value="BLOCKED">Đã khóa</option>
            <option value="INACTIVE">Ngừng hoạt động</option>
          </select>
        </div>
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!result.content?.length} emptyMessage="Không tìm thấy khách hàng nào">
        <div className="overflow-x-auto rounded-2xl border border-zinc-800 bg-zinc-900/30">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-zinc-950/50 text-xs uppercase font-black text-zinc-500 tracking-wider">
              <tr>
                <th className="p-4 rounded-tl-2xl">Mã KH</th>
                <th className="p-4">Khách hàng</th>
                <th className="p-4">Liên hệ</th>
                <th className="p-4">Trạng thái</th>
                <th className="p-4 text-right rounded-tr-2xl">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/50">
              {result.content?.map(customer => (
                <tr key={customer.id} className="hover:bg-zinc-800/20 transition-colors group">
                  <td className="p-4">
                    <span className="font-mono text-xs text-zinc-400 bg-zinc-900 px-2 py-1 rounded-md border border-zinc-800 group-hover:border-zinc-700 transition-colors">
                      {customer.customerCode || customer.id?.substring(0, 8)}
                    </span>
                  </td>
                  <td className="p-4">
                    <p className="font-bold text-zinc-100">{customer.fullName}</p>
                    <p className="text-xs text-zinc-500 mt-0.5">{customer.email || '—'}</p>
                  </td>
                  <td className="p-4 text-zinc-300">
                    <p>{customer.phoneNumber || '—'}</p>
                  </td>
                  <td className="p-4">
                    <StatusBadge status={customer.status} label={customer.status === 'BLOCKED' ? 'Bị khóa' : customer.status === 'ACTIVE' ? 'Hoạt động' : undefined} />
                  </td>
                  <td className="p-4 text-right">
                    <div className="flex justify-end gap-2">
                      {canUpdateCustomers && <button
                        type="button" 
                        onClick={() => toggleBlocked(customer)}
                        className={`rounded-lg border px-3 py-1.5 text-xs font-bold transition-all flex items-center gap-1.5 ${
                          customer.status === 'BLOCKED' 
                            ? 'border-emerald-500/30 text-emerald-500 hover:bg-emerald-500/10' 
                            : 'border-red-500/30 text-red-500 hover:bg-red-500/10'
                        }`}
                      >
                        {customer.status === 'BLOCKED' ? <ShieldCheck size={14} /> : <ShieldBan size={14} />}
                        {customer.status === 'BLOCKED' ? 'Mở khóa' : 'Khóa'}
                      </button>}
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
    </section>
  );
}
