import { useCallback, useEffect, useMemo, useState } from 'react';
import { CalendarDays, Plus, Umbrella } from 'lucide-react';
import { AsyncState, StatusBadge } from '@/components/common/ui/uiKit';
import { createLeaveRequest, getMyLeaveRequests, getMyWorkShifts } from '../../admin/services/userAdminService';

const localDate = (date = new Date()) => {
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 10);
};
const monthRange = month => {
  const start = `${month}-01`;
  const end = new Date(`${start}T00:00:00`);
  end.setMonth(end.getMonth() + 1);
  end.setDate(0);
  return { from: start, to: localDate(end) };
};

export default function EmployeeSchedulePage() {
  const [month, setMonth] = useState(localDate().slice(0, 7));
  const [shifts, setShifts] = useState([]);
  const [leaves, setLeaves] = useState([]);
  const [state, setState] = useState({ loading: true, error: '' });
  const [leaveOpen, setLeaveOpen] = useState(false);
  const [leaveForm, setLeaveForm] = useState({ leaveType: 'ANNUAL', startDate: localDate(), endDate: localDate(), reason: '' });
  const [message, setMessage] = useState('');
  const range = useMemo(() => monthRange(month), [month]);

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const params = { ...range, page: 0, size: 100, sort: 'createdAt,desc' };
      const [shiftPage, leavePage] = await Promise.all([
        getMyWorkShifts({ ...params, sort: 'scheduledStart,asc' }),
        getMyLeaveRequests(params)
      ]);
      setShifts(shiftPage?.content || []);
      setLeaves(leavePage?.content || []);
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải lịch làm việc.' });
    }
  }, [range]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const submitLeave = async event => {
    event.preventDefault();
    setMessage('');
    try {
      await createLeaveRequest(leaveForm);
      setMessage('Đã gửi yêu cầu nghỉ; quản lý khác phải duyệt trước khi tính lương.');
      setLeaveOpen(false);
      await load();
    } catch (error) {
      setMessage(error?.message || 'Không thể gửi yêu cầu nghỉ.');
    }
  };

  return (
    <section className="space-y-6 text-white">
      <header className="flex flex-col gap-4 border-b border-zinc-800 pb-6 md:flex-row md:items-end md:justify-between">
        <div><p className="text-xs font-black uppercase tracking-[0.2em] text-amber-500">Self-service</p><h1 className="mt-2 text-3xl font-black">Lịch làm & nghỉ phép</h1><p className="mt-2 text-sm text-zinc-500">Ca được quản lý phân công và yêu cầu nghỉ có quy trình duyệt độc lập.</p></div>
        <div className="flex gap-2"><input aria-label="Tháng làm việc" type="month" value={month} onChange={event => setMonth(event.target.value)} className="rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-2 text-sm" /><button type="button" onClick={() => setLeaveOpen(true)} className="flex items-center gap-2 rounded-xl bg-amber-500 px-4 py-2 text-sm font-black text-black"><Plus size={16} /> Xin nghỉ</button></div>
      </header>
      {message && <p className="rounded-xl border border-amber-500/20 bg-amber-500/10 p-3 text-sm text-amber-300">{message}</p>}
      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!shifts.length && !leaves.length} emptyMessage="Chưa có lịch hoặc yêu cầu nghỉ trong tháng">
        <div className="grid gap-6 xl:grid-cols-2">
          <article className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/60"><div className="flex items-center gap-2 border-b border-zinc-800 p-5"><CalendarDays className="text-amber-500" size={19} /><h2 className="font-black">Ca được phân công</h2></div><div className="divide-y divide-zinc-800">{shifts.map(item => <div key={item.id} className="flex items-center justify-between gap-4 p-5"><div><p className="font-bold">{new Date(item.scheduledStart).toLocaleString('vi-VN')}</p><p className="mt-1 text-xs text-zinc-500">Đến {new Date(item.scheduledEnd).toLocaleString('vi-VN')} · {item.location || 'Chưa có địa điểm'}</p></div><StatusBadge status={item.status} /></div>)}</div></article>
          <article className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/60"><div className="flex items-center gap-2 border-b border-zinc-800 p-5"><Umbrella className="text-sky-400" size={19} /><h2 className="font-black">Yêu cầu nghỉ</h2></div><div className="divide-y divide-zinc-800">{leaves.map(item => <div key={item.id} className="flex items-center justify-between gap-4 p-5"><div><p className="font-bold">{item.startDate} → {item.endDate}</p><p className="mt-1 text-xs text-zinc-500">{item.leaveType} · {item.paid ? 'Hưởng lương' : 'Không lương'} · {item.reason}</p></div><StatusBadge status={item.status} /></div>)}</div></article>
        </div>
      </AsyncState>

      {leaveOpen && <div className="fixed inset-0 z-50 grid place-items-center bg-black/75 p-4"><form onSubmit={submitLeave} className="w-full max-w-lg space-y-4 rounded-2xl border border-zinc-800 bg-zinc-900 p-6"><div><h2 className="text-xl font-black">Gửi yêu cầu nghỉ</h2><p className="mt-1 text-sm text-zinc-500">Các khoảng ngày đang chờ hoặc đã duyệt không được chồng lấn.</p></div><label className="block text-xs font-black uppercase text-zinc-500">Loại nghỉ<select value={leaveForm.leaveType} onChange={event => setLeaveForm(value => ({ ...value, leaveType: event.target.value }))} className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 p-3 text-white"><option value="ANNUAL">Phép năm · hưởng lương</option><option value="SICK">Nghỉ bệnh · hưởng lương</option><option value="UNPAID">Nghỉ không lương</option><option value="OTHER">Khác</option></select></label><div className="grid gap-3 sm:grid-cols-2"><label className="text-xs font-black uppercase text-zinc-500">Từ ngày<input required type="date" value={leaveForm.startDate} onChange={event => setLeaveForm(value => ({ ...value, startDate: event.target.value }))} className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 p-3 text-white" /></label><label className="text-xs font-black uppercase text-zinc-500">Đến ngày<input required type="date" value={leaveForm.endDate} onChange={event => setLeaveForm(value => ({ ...value, endDate: event.target.value }))} className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 p-3 text-white" /></label></div><label className="block text-xs font-black uppercase text-zinc-500">Lý do<textarea required minLength={5} value={leaveForm.reason} onChange={event => setLeaveForm(value => ({ ...value, reason: event.target.value }))} className="mt-2 min-h-24 w-full rounded-xl border border-zinc-700 bg-zinc-950 p-3 text-white" /></label><div className="flex justify-end gap-2 border-t border-zinc-800 pt-4"><button type="button" onClick={() => setLeaveOpen(false)} className="rounded-xl border border-zinc-700 px-4 py-2 text-sm">Hủy</button><button type="submit" className="rounded-xl bg-amber-500 px-4 py-2 text-sm font-black text-black">Gửi yêu cầu</button></div></form></div>}
    </section>
  );
}
