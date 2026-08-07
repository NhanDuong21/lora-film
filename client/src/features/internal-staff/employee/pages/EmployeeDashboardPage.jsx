import { useCallback, useEffect, useState } from 'react';
import { CalendarDays, Clock3, WalletCards } from 'lucide-react';
import { AsyncState, StatusBadge } from '@/components/common/ui/uiKit';
import { getMyPayrolls, getMyWorkShifts } from '../../admin/services/userAdminService';

const localDate = (date = new Date()) => new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 10);

export default function EmployeeDashboardPage() {
  const [data, setData] = useState({ shifts: [], payrolls: [] });
  const [state, setState] = useState({ loading: true, error: '' });
  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const from = localDate();
      const end = new Date(); end.setDate(end.getDate() + 14);
      const [shifts, payrolls] = await Promise.all([
        getMyWorkShifts({ from, to: localDate(end), page: 0, size: 20, sort: 'scheduledStart,asc' }),
        getMyPayrolls({ page: 0, size: 6, sort: 'salaryMonth,desc' })
      ]);
      setData({ shifts: shifts?.content || [], payrolls: payrolls?.content || [] });
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải bảng điều khiển nhân viên.' });
    }
  }, []);
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  return <section className="space-y-6 text-white"><header className="border-b border-zinc-800 pb-6"><p className="text-xs font-black uppercase tracking-[0.2em] text-amber-500">Employee workspace</p><h1 className="mt-2 text-3xl font-black">Bảng điều khiển nhân viên</h1><p className="mt-2 text-sm text-zinc-500">Ca sắp tới và các kỳ lương đã được duyệt.</p></header><AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!data.shifts.length && !data.payrolls.length} emptyMessage="Chưa có dữ liệu vận hành"><div className="grid gap-6 xl:grid-cols-2"><article className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-5"><div className="flex items-center gap-2"><CalendarDays className="text-amber-500" size={19} /><h2 className="font-black">Ca sắp tới</h2></div><div className="mt-4 divide-y divide-zinc-800">{data.shifts.slice(0, 5).map(item => <div key={item.id} className="flex items-center justify-between py-4"><div><p className="font-bold">{new Date(item.scheduledStart).toLocaleString('vi-VN')}</p><p className="text-xs text-zinc-500">{item.location || 'Chưa có địa điểm'}</p></div><StatusBadge status={item.status} /></div>)}</div></article><article className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-5"><div className="flex items-center gap-2"><WalletCards className="text-emerald-400" size={19} /><h2 className="font-black">Phiếu lương gần đây</h2></div><div className="mt-4 divide-y divide-zinc-800">{data.payrolls.slice(0, 5).map(item => <div key={item.id} className="flex items-center justify-between py-4"><div><p className="font-bold">Kỳ {String(item.salaryMonth).slice(0, 7)}</p><p className="text-xs text-zinc-500">{Number(item.totalSalary || 0).toLocaleString('vi-VN')} ₫</p></div><StatusBadge status={item.status} /></div>)}</div></article></div></AsyncState><p className="flex items-center gap-2 text-xs text-zinc-600"><Clock3 size={14} /> Dữ liệu lương tự động được chốt từ ca làm, chấm công và nghỉ phép đã duyệt.</p></section>;
}
