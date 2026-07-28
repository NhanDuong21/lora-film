import { useCallback, useEffect, useState } from 'react';
import { getCustomers, setCustomerBlocked } from '../services/userAdminService';
import { AsyncState, Input, Select, StatusBadge } from '@/components/common/ui/uiKit';
import { Users, UserPlus, UserCheck, UserX, Search, Filter, MoreVertical, ShieldBan, ShieldCheck } from 'lucide-react';

export default function AdminMembersPage() {
  const [query, setQuery] = useState({ keyword: '', status: '', page: 0, size: 10 });
  const [result, setResult] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [state, setState] = useState({ loading: true, error: '' });
  const [stats, setStats] = useState({ total: 0, active: 0, blocked: 0, new: 0 });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const data = await getCustomers({
        ...query,
        keyword: query.keyword || undefined,
        status: query.status || undefined
      });
      setResult(data || { content: [], totalPages: 0, totalElements: 0 });
      
      // Calculate basic stats for the current view if we don't have a dedicated endpoint
      if (query.page === 0 && !query.keyword && !query.status) {
        setStats({
          total: data.totalElements || data.content.length || 0,
          active: data.content.filter(c => c.status === 'ACTIVE').length || 0,
          blocked: data.content.filter(c => c.status === 'BLOCKED').length || 0,
          new: data.content.filter(c => {
             // Assuming created within last 7 days
             if(!c.createdAt) return false;
             return (new Date() - new Date(c.createdAt)) / (1000 * 60 * 60 * 24) < 7;
          }).length || 0
        });
      }
      
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

  const StatCard = ({ title, value, icon: Icon, colorClass }) => (
    <div className="bg-zinc-900/50 border border-zinc-800 rounded-2xl p-5 flex items-center justify-between hover:bg-zinc-900 transition-colors">
      <div>
        <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider mb-1">{title}</p>
        <h3 className="text-3xl font-black text-white">{value}</h3>
      </div>
      <div className={`w-12 h-12 rounded-full flex items-center justify-center ${colorClass}`}>
        <Icon size={24} />
      </div>
    </div>
  );

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-[#050506] p-6 text-white md:p-8">
      <header className="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
        <div>
          <h1 className="text-2xl font-black uppercase tracking-wider text-white">Quản lý <span className="text-brand-orange">Khách Hàng</span></h1>
          <p className="mt-1 text-sm text-zinc-500">Xem danh sách, tìm kiếm và quản lý trạng thái tài khoản thành viên.</p>
        </div>
        <div className="flex gap-2">
          <button className="px-4 py-2 rounded-xl border border-zinc-800 bg-zinc-900 text-sm font-bold hover:bg-zinc-800 transition-colors flex items-center gap-2 text-zinc-300">
             Xuất dữ liệu
          </button>
        </div>
      </header>

      {/* Dashboard Statistics */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Tổng thành viên" value={stats.total || result.totalElements || '...'} icon={Users} colorClass="bg-blue-500/10 text-blue-500 border border-blue-500/20" />
        <StatCard title="Thành viên mới" value={stats.new || '...'} icon={UserPlus} colorClass="bg-emerald-500/10 text-emerald-500 border border-emerald-500/20" />
        <StatCard title="Đang hoạt động" value={stats.active || '...'} icon={UserCheck} colorClass="bg-brand-orange/10 text-brand-orange border border-brand-orange/20" />
        <StatCard title="Bị khóa" value={stats.blocked || '...'} icon={UserX} colorClass="bg-red-500/10 text-red-500 border border-red-500/20" />
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
                      <button 
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
                      </button>
                      <button className="p-1.5 rounded-lg text-zinc-500 hover:bg-zinc-800 hover:text-zinc-300 transition-colors">
                        <MoreVertical size={16} />
                      </button>
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
