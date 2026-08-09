import { useCallback, useEffect, useMemo, useState } from 'react';
import { CheckCircle2, Clock3, LogIn, LogOut } from 'lucide-react';
import { AsyncState, StatusBadge } from '@/components/common/ui/uiKit';
import { checkInShift, checkOutShift, getMyAttendance, getMyWorkShifts } from '../../admin/services/userAdminService';
import { attendanceStatus, shiftStatus } from '../employeePresentation';

const today = () => {
  const date = new Date();
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 10);
};

const checkInWindow = shift => {
  const now = Date.now();
  const start = new Date(shift.scheduledStart).getTime();
  const end = new Date(shift.scheduledEnd).getTime();
  const opensAt = start - (2 * 60 * 60 * 1000);
  return {
    allowed: now >= opensAt && now <= end,
    tooEarly: now < opensAt,
    opensAt,
  };
};

export default function EmployeeCheckInPage() {
  const [date, setDate] = useState(today);
  const [shifts, setShifts] = useState([]);
  const [attendance, setAttendance] = useState([]);
  const [state, setState] = useState({ loading: true, error: '' });
  const [message, setMessage] = useState('');
  const [working, setWorking] = useState(null);

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const params = { from: date, to: date, page: 0, size: 50 };
      const [shiftPage, attendancePage] = await Promise.all([getMyWorkShifts(params), getMyAttendance(params)]);
      setShifts(shiftPage?.content || []);
      setAttendance(attendancePage?.content || []);
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải dữ liệu chấm công.' });
    }
  }, [date]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);
  const byShift = useMemo(() => new Map(attendance.map(item => [item.shiftId, item])), [attendance]);

  const act = async (shiftId, type) => {
    setWorking(shiftId);
    setMessage('');
    try {
      await (type === 'in' ? checkInShift(shiftId) : checkOutShift(shiftId));
      setMessage(type === 'in' ? 'Đã ghi nhận giờ vào ca.' : 'Đã ghi nhận giờ ra ca.');
      await load();
    } catch (error) {
      setMessage(error?.message || 'Không thể ghi nhận chấm công.');
    } finally {
      setWorking(null);
    }
  };

  return <section className="space-y-6 text-white">
    <header className="flex flex-col gap-4 border-b border-zinc-800 pb-6 md:flex-row md:items-end md:justify-between"><div><p className="text-xs font-black uppercase tracking-[0.2em] text-amber-500">Ghi nhận giờ làm</p><h1 className="mt-2 text-3xl font-black">Chấm công ca làm</h1><p className="mt-2 text-sm text-zinc-500">Giờ vào và ra ca được hệ thống ghi nhận tự động; mọi hiệu chỉnh đều cần nêu rõ lý do.</p></div><input aria-label="Ngày chấm công" type="date" value={date} onChange={event => setDate(event.target.value)} className="rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-2" /></header>
    {message && <p className="rounded-xl border border-amber-500/20 bg-amber-500/10 p-3 text-sm text-amber-300">{message}</p>}
    <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!shifts.length} emptyMessage="Bạn không có ca làm trong ngày này">
      <div className="grid gap-4 xl:grid-cols-2">{shifts.map(shift => { const record = byShift.get(shift.id); const displayedStatus = record?.status || shift.status; const window = checkInWindow(shift); return <article key={shift.id} className="rounded-2xl border border-zinc-800 bg-zinc-900 p-5"><div className="flex items-start justify-between"><div><p className="text-lg font-black">{new Date(shift.scheduledStart).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })} – {new Date(shift.scheduledEnd).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</p><p className="mt-1 text-sm text-zinc-500">{shift.location || 'Chưa có địa điểm'}</p></div><StatusBadge status={displayedStatus} label={record ? attendanceStatus(displayedStatus) : shiftStatus(displayedStatus)} /></div><div className="mt-5 grid grid-cols-2 gap-3 rounded-xl bg-zinc-950 p-4 text-sm"><div><p className="text-xs uppercase text-zinc-600">Giờ vào</p><p className="mt-1 font-bold">{record?.checkInAt ? new Date(record.checkInAt).toLocaleTimeString('vi-VN') : '—'}</p></div><div><p className="text-xs uppercase text-zinc-600">Giờ ra</p><p className="mt-1 font-bold">{record?.checkOutAt ? new Date(record.checkOutAt).toLocaleTimeString('vi-VN') : '—'}</p></div></div><div className="mt-4 flex gap-2">{!record && shift.status === 'SCHEDULED' && <button disabled={working === shift.id || !window.allowed} type="button" onClick={() => act(shift.id, 'in')} className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-emerald-500 px-4 py-3 text-sm font-black text-black disabled:cursor-not-allowed disabled:bg-zinc-800 disabled:text-zinc-500"><LogIn size={17} /> {window.allowed ? 'Vào ca' : window.tooEarly ? `Mở chấm công lúc ${new Date(window.opensAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}` : 'Đã quá giờ vào ca'}</button>}{record && !record.checkOutAt && <button disabled={working === shift.id} type="button" onClick={() => act(shift.id, 'out')} className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-amber-500 px-4 py-3 text-sm font-black text-black"><LogOut size={17} /> Ra ca</button>}{record?.checkOutAt && <p className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-emerald-500/10 p-3 text-sm font-bold text-emerald-400"><CheckCircle2 size={17} /> {record.workedMinutes} phút công</p>}</div></article>; })}</div>
    </AsyncState>
    <p className="flex items-center gap-2 text-xs text-zinc-600"><Clock3 size={14} /> Cho phép vào ca sớm tối đa 2 giờ; đi muộn sau 5 phút được ghi nhận tự động.</p>
  </section>;
}
