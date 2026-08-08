import { useCallback, useEffect, useMemo, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { CalendarPlus, Check, Clock3, UserCheck, UsersRound, X } from 'lucide-react';
import managerCinemaService from '../services/managerCinemaService';

const inputDate = date => {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
};

const localDateTime = (date, hours) => {
  const value = new Date(date);
  value.setDate(value.getDate() + 1);
  value.setHours(hours, 0, 0, 0);
  const local = new Date(value.getTime() - value.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
};

const STATUS = {
  ACTIVE: 'Đang làm việc', ON_LEAVE: 'Đang nghỉ', SUSPENDED: 'Tạm ngưng', RESIGNED: 'Đã nghỉ việc',
  SCHEDULED: 'Đã xếp ca', COMPLETED: 'Đã hoàn thành', CANCELLED: 'Đã hủy',
  PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối',
  ON_TIME: 'Đúng giờ', LATE: 'Đi muộn', ABSENT: 'Vắng mặt', CORRECTED: 'Đã điều chỉnh',
};

const formatDateTime = value => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
  : 'Chưa ghi nhận';

export default function ManagerStaffPage() {
  const { selectedCinema, selectedCinemaId, cinemaState } = useOutletContext();
  const today = useMemo(() => new Date(), []);
  const [from, setFrom] = useState(inputDate(today));
  const [to, setTo] = useState(inputDate(new Date(today.getTime() + 7 * 86400000)));
  const [tab, setTab] = useState('staff');
  const [data, setData] = useState({ staff: [], shifts: [], attendance: [], leaves: [] });
  const [state, setState] = useState({ loading: true, error: '', success: '' });
  const [showShiftForm, setShowShiftForm] = useState(false);
  const [shiftForm, setShiftForm] = useState({ employeeId: '', scheduledStart: localDateTime(today, 9), scheduledEnd: localDateTime(today, 17), note: '' });

  const load = useCallback(async () => {
    if (!selectedCinemaId) return;
    setState(current => ({ ...current, loading: true, error: '' }));
    try {
      const params = { cinemaPublicId: selectedCinemaId, from, to };
      const [staff, shifts, attendance, leaves] = await Promise.all([
        managerCinemaService.getStaff(selectedCinemaId),
        managerCinemaService.getShifts(params),
        managerCinemaService.getAttendance(params),
        managerCinemaService.getLeaveRequests(params),
      ]);
      setData({ staff, shifts, attendance, leaves });
      setState(current => ({ ...current, loading: false }));
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải dữ liệu nhân sự tại rạp.', success: '' });
    }
  }, [from, selectedCinemaId, to]);

  useEffect(() => {
    load();
  }, [load]);

  const attendanceByShift = useMemo(() => new Map(data.attendance.map(item => [item.shiftId, item])), [data.attendance]);
  const pendingLeaves = data.leaves.filter(item => item.status === 'PENDING').length;

  const createShift = async event => {
    event.preventDefault();
    try {
      await managerCinemaService.createShift(selectedCinemaId, {
        employeeId: Number(shiftForm.employeeId),
        scheduledStart: shiftForm.scheduledStart,
        scheduledEnd: shiftForm.scheduledEnd,
        location: selectedCinema.name,
        note: shiftForm.note.trim() || null,
      });
      setShowShiftForm(false);
      await load();
      setState({ loading: false, error: '', success: 'Đã xếp ca và cập nhật vào lịch làm việc của nhân viên.' });
      setTab('shifts');
    } catch (error) {
      setState(current => ({ ...current, error: error?.message || 'Không thể xếp ca làm.' }));
    }
  };

  const cancelShift = async shift => {
    const reason = window.prompt('Nhập lý do hủy ca (ít nhất 5 ký tự):');
    if (!reason) return;
    try {
      await managerCinemaService.cancelShift(selectedCinemaId, shift.id, { reason, expectedVersion: shift.version });
      await load();
      setState({ loading: false, error: '', success: 'Đã hủy ca làm.' });
    } catch (error) {
      setState(current => ({ ...current, error: error?.message || 'Không thể hủy ca.' }));
    }
  };

  const reviewLeave = async (leave, type) => {
    const note = type === 'REJECT' ? window.prompt('Nhập lý do từ chối (ít nhất 5 ký tự):') : 'Quản lý rạp đã duyệt';
    if (type === 'REJECT' && !note) return;
    try {
      await managerCinemaService.reviewLeave(selectedCinemaId, leave.id, { type, note, expectedVersion: leave.version });
      await load();
      setState({ loading: false, error: '', success: type === 'APPROVE' ? 'Đã duyệt đơn nghỉ.' : 'Đã từ chối đơn nghỉ.' });
    } catch (error) {
      setState(current => ({ ...current, error: error?.message || 'Không thể xử lý đơn nghỉ.' }));
    }
  };

  if (cinemaState.loading) return <p className="py-24 text-center text-sm font-bold text-zinc-500">Đang tải phạm vi rạp…</p>;
  if (!selectedCinema) return <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 p-8 text-center">Chưa có rạp được phân công.</div>;

  return (
    <div className="space-y-6">
      <header className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between"><div><p className="text-xs font-black uppercase tracking-[0.2em] text-brand-orange">Điều phối đội ngũ tại rạp</p><h1 className="mt-2 text-3xl font-black">Nhân sự & ca làm</h1><p className="mt-2 text-sm text-zinc-500">Xem đúng nhân viên của {selectedCinema.name}, xếp ca và xử lý đơn nghỉ trên một màn hình.</p></div><button type="button" onClick={() => setShowShiftForm(value => !value)} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-orange px-4 text-sm font-black text-black"><CalendarPlus size={18} /> Xếp ca mới</button></header>

      <section className="grid gap-4 sm:grid-cols-3"><article className="rounded-2xl border border-white/10 bg-white/[0.025] p-5"><p className="text-xs font-bold text-zinc-500">Nhân viên tại rạp</p><p className="mt-2 text-3xl font-black">{data.staff.length}</p></article><article className="rounded-2xl border border-white/10 bg-white/[0.025] p-5"><p className="text-xs font-bold text-zinc-500">Ca trong khoảng đã chọn</p><p className="mt-2 text-3xl font-black">{data.shifts.length}</p></article><article className={`rounded-2xl border p-5 ${pendingLeaves ? 'border-amber-500/25 bg-amber-500/[0.06]' : 'border-white/10 bg-white/[0.025]'}`}><p className="text-xs font-bold text-zinc-500">Đơn nghỉ chờ duyệt</p><p className="mt-2 text-3xl font-black">{pendingLeaves}</p></article></section>

      {showShiftForm ? <form onSubmit={createShift} className="rounded-2xl border border-brand-orange/25 bg-brand-orange/[0.05] p-5"><h2 className="font-black">Xếp một ca làm mới</h2><p className="mt-1 text-xs text-zinc-500">Nhân viên sẽ thấy ca này trong màn hình lịch làm việc cá nhân.</p><div className="mt-4 grid gap-4 lg:grid-cols-4"><label className="text-xs font-bold text-zinc-400">Nhân viên<select required value={shiftForm.employeeId} onChange={event => setShiftForm(current => ({ ...current, employeeId: event.target.value }))} className="mt-2 min-h-11 w-full rounded-xl border border-white/10 bg-zinc-900 px-3 text-sm text-white"><option value="">Chọn nhân viên</option>{data.staff.map(item => <option key={item.accountId} value={item.accountId}>{item.fullName} · {item.positionName}</option>)}</select></label><label className="text-xs font-bold text-zinc-400">Bắt đầu<input required type="datetime-local" value={shiftForm.scheduledStart} onChange={event => setShiftForm(current => ({ ...current, scheduledStart: event.target.value }))} className="mt-2 min-h-11 w-full rounded-xl border border-white/10 bg-zinc-900 px-3 text-sm text-white" /></label><label className="text-xs font-bold text-zinc-400">Kết thúc<input required type="datetime-local" value={shiftForm.scheduledEnd} onChange={event => setShiftForm(current => ({ ...current, scheduledEnd: event.target.value }))} className="mt-2 min-h-11 w-full rounded-xl border border-white/10 bg-zinc-900 px-3 text-sm text-white" /></label><label className="text-xs font-bold text-zinc-400">Ghi chú<input value={shiftForm.note} onChange={event => setShiftForm(current => ({ ...current, note: event.target.value }))} placeholder="Ví dụ: Quầy vé ca sáng" className="mt-2 min-h-11 w-full rounded-xl border border-white/10 bg-zinc-900 px-3 text-sm text-white" /></label></div><div className="mt-4 flex justify-end"><button className="rounded-xl bg-white px-5 py-2.5 text-sm font-black text-black">Lưu ca làm</button></div></form> : null}

      <section className="flex flex-col gap-3 rounded-2xl border border-white/10 bg-white/[0.02] p-4 lg:flex-row lg:items-end lg:justify-between"><div className="flex flex-wrap gap-2">{[['staff', 'Danh sách nhân viên'], ['shifts', 'Ca làm & chấm công'], ['leaves', `Đơn nghỉ${pendingLeaves ? ` (${pendingLeaves})` : ''}`]].map(([value, label]) => <button key={value} type="button" onClick={() => setTab(value)} className={`rounded-xl px-4 py-2.5 text-sm font-black ${tab === value ? 'bg-brand-orange text-black' : 'bg-white/5 text-zinc-400'}`}>{label}</button>)}</div><div className="flex gap-3"><label className="text-xs font-bold text-zinc-500">Từ ngày<input type="date" value={from} onChange={event => setFrom(event.target.value)} className="ml-2 rounded-lg border border-white/10 bg-zinc-900 px-2 py-2 text-white" /></label><label className="text-xs font-bold text-zinc-500">Đến ngày<input type="date" value={to} onChange={event => setTo(event.target.value)} className="ml-2 rounded-lg border border-white/10 bg-zinc-900 px-2 py-2 text-white" /></label></div></section>

      {state.error ? <p className="rounded-xl border border-red-500/20 bg-red-500/10 p-4 text-sm text-red-200">{state.error}</p> : null}{state.success ? <p className="rounded-xl border border-emerald-500/20 bg-emerald-500/10 p-4 text-sm text-emerald-200">{state.success}</p> : null}

      <section className="overflow-hidden rounded-2xl border border-white/10 bg-white/[0.02]">{state.loading ? <p className="p-12 text-center text-sm text-zinc-500">Đang cập nhật dữ liệu nhân sự…</p> : tab === 'staff' ? <div className="divide-y divide-white/5">{data.staff.map(item => <article key={item.accountId} className="grid gap-3 p-5 md:grid-cols-[1fr_1fr_1fr_auto] md:items-center"><div><p className="font-black">{item.fullName}</p><p className="mt-1 text-xs text-zinc-600">{item.employeeCode}</p></div><div><p className="text-sm font-bold text-zinc-300">{item.positionName}</p><p className="mt-1 text-xs text-zinc-600">{item.departmentName}</p></div><p className="text-sm text-zinc-400">{item.email}</p><span className="rounded-full bg-emerald-500/10 px-3 py-1 text-xs font-bold text-emerald-300">{STATUS[item.status] || item.status}</span></article>)}</div> : tab === 'shifts' ? <div className="divide-y divide-white/5">{data.shifts.length ? data.shifts.map(shift => { const attendance = attendanceByShift.get(shift.id); return <article key={shift.id} className="grid gap-3 p-5 lg:grid-cols-[1.2fr_1fr_1fr_auto] lg:items-center"><div><p className="font-black">{shift.employeeName}</p><p className="mt-1 text-xs text-zinc-600">{shift.note || shift.location}</p></div><div className="flex items-center gap-2 text-sm text-zinc-300"><Clock3 size={16} className="text-brand-orange" /><span>{formatDateTime(shift.scheduledStart)}<br />đến {formatDateTime(shift.scheduledEnd)}</span></div><div><p className="text-xs font-bold text-zinc-500">Chấm công</p><p className={`mt-1 text-sm font-bold ${attendance ? 'text-emerald-300' : 'text-amber-300'}`}>{attendance ? `${STATUS[attendance.status] || attendance.status} · ${attendance.workedMinutes || 0} phút` : 'Chưa ghi nhận'}</p></div>{shift.status === 'SCHEDULED' ? <button onClick={() => cancelShift(shift)} className="rounded-lg border border-red-500/20 px-3 py-2 text-xs font-black text-red-300">Hủy ca</button> : <span className="text-xs font-bold text-zinc-500">{STATUS[shift.status] || shift.status}</span>}</article>; }) : <p className="p-12 text-center text-sm text-zinc-500">Chưa có ca làm trong khoảng ngày này.</p>}</div> : <div className="divide-y divide-white/5">{data.leaves.length ? data.leaves.map(leave => <article key={leave.id} className="grid gap-3 p-5 lg:grid-cols-[1fr_1fr_1.4fr_auto] lg:items-center"><div><p className="font-black">{leave.employeeName}</p><p className="mt-1 text-xs text-zinc-600">{leave.employeeCode}</p></div><p className="text-sm text-zinc-300">{leave.startDate} → {leave.endDate}<br /><span className="text-xs text-zinc-600">{leave.paid ? 'Nghỉ có lương' : 'Nghỉ không lương'}</span></p><p className="text-sm text-zinc-400">{leave.reason}</p>{leave.status === 'PENDING' ? <div className="flex gap-2"><button title="Duyệt đơn" onClick={() => reviewLeave(leave, 'APPROVE')} className="grid h-9 w-9 place-items-center rounded-lg bg-emerald-500/10 text-emerald-300"><Check size={17} /></button><button title="Từ chối đơn" onClick={() => reviewLeave(leave, 'REJECT')} className="grid h-9 w-9 place-items-center rounded-lg bg-red-500/10 text-red-300"><X size={17} /></button></div> : <span className="rounded-full bg-white/5 px-3 py-1 text-xs font-bold text-zinc-400">{STATUS[leave.status] || leave.status}</span>}</article>) : <div className="p-12 text-center"><UserCheck className="mx-auto text-zinc-700" /><p className="mt-3 text-sm text-zinc-500">Không có đơn nghỉ trong khoảng ngày này.</p></div>}</div>}</section>

      <p className="flex items-center gap-2 text-xs text-zinc-600"><UsersRound size={15} /> Chỉ hiển thị hồ sơ đã được Admin gắn vào {selectedCinema.name}.</p>
    </div>
  );
}
