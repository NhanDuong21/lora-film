import { useCallback, useEffect, useMemo, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { AlertTriangle, Building2, CalendarClock, CheckCircle2, Plus, Wrench } from 'lucide-react';
import managerCinemaService from '../services/managerCinemaService';

const toLocalInput = date => {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
};

const initialForm = () => {
  const start = new Date(Date.now() + 60 * 60 * 1000);
  const end = new Date(start.getTime() + 2 * 60 * 60 * 1000);
  return { auditoriumPublicId: '', startTime: toLocalInput(start), endTime: toLocalInput(end), reason: '' };
};

const formatDateTime = value => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
  : 'Chưa xác định';

export default function ManagerRoomsPage() {
  const { selectedCinema, selectedCinemaId, cinemaState } = useOutletContext();
  const [windows, setWindows] = useState([]);
  const [state, setState] = useState({ loading: true, error: '', success: '' });
  const [form, setForm] = useState(initialForm);
  const [showForm, setShowForm] = useState(false);

  const rooms = useMemo(() => selectedCinema?.activeAuditoriums || [], [selectedCinema]);
  const activeWindows = windows.filter(item => item.status === 'ACTIVE' && new Date(item.endTime) > new Date());

  const load = useCallback(async () => {
    if (!selectedCinemaId) return;
    setState(current => ({ ...current, loading: true, error: '' }));
    try {
      const data = await managerCinemaService.getMaintenanceWindows(selectedCinemaId);
      setWindows(data);
      setState(current => ({ ...current, loading: false }));
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải tình trạng phòng chiếu.', success: '' });
    }
  }, [selectedCinemaId]);

  useEffect(() => {
    load();
  }, [load]);

  const submit = async event => {
    event.preventDefault();
    if (!form.auditoriumPublicId) return;
    setState(current => ({ ...current, loading: true, error: '', success: '' }));
    try {
      await managerCinemaService.createMaintenanceWindow(selectedCinemaId, form.auditoriumPublicId, {
        startTime: new Date(form.startTime).toISOString(),
        endTime: new Date(form.endTime).toISOString(),
        reason: form.reason.trim(),
      });
      setForm(initialForm());
      setShowForm(false);
      await load();
      setState({ loading: false, error: '', success: 'Đã ghi nhận lịch bảo trì. Hệ thống sẽ chặn xếp lịch trùng thời gian này.' });
    } catch (error) {
      setState(current => ({ ...current, loading: false, error: error?.message || 'Không thể tạo lịch bảo trì.' }));
    }
  };

  const cancelWindow = async item => {
    if (!window.confirm('Hủy lịch bảo trì này? Phòng sẽ có thể được dùng để xếp lịch trở lại.')) return;
    try {
      await managerCinemaService.cancelMaintenanceWindow(selectedCinemaId, item.id);
      await load();
      setState({ loading: false, error: '', success: 'Đã hủy lịch bảo trì.' });
    } catch (error) {
      setState(current => ({ ...current, error: error?.message || 'Không thể hủy lịch bảo trì.' }));
    }
  };

  if (cinemaState.loading) return <p className="py-24 text-center text-sm font-bold text-zinc-500">Đang tải phạm vi rạp…</p>;
  if (!selectedCinema) return <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 p-8 text-center">Chưa có rạp được phân công.</div>;

  return (
    <div className="space-y-6">
      <header className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div><p className="text-xs font-black uppercase tracking-[0.2em] text-brand-orange">Cơ sở vật chất tại rạp</p><h1 className="mt-2 text-3xl font-black">Phòng chiếu & bảo trì</h1><p className="mt-2 text-sm text-zinc-500">Biết phòng nào sẵn sàng và chủ động chặn lịch khi có vệ sinh, sửa chữa hoặc sự cố.</p></div>
        <button type="button" onClick={() => setShowForm(value => !value)} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-orange px-4 text-sm font-black text-black"><Plus size={17} /> Ghi nhận phòng cần bảo trì</button>
      </header>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {rooms.map(room => {
          const roomWindow = activeWindows.find(item => item.auditoriumPublicId === room.publicId);
          return <article key={room.publicId} className={`rounded-2xl border p-5 ${roomWindow ? 'border-amber-500/30 bg-amber-500/[0.07]' : 'border-white/10 bg-white/[0.025]'}`}><div className="flex items-start justify-between"><span className={`grid h-10 w-10 place-items-center rounded-xl ${roomWindow ? 'bg-amber-500/10 text-amber-300' : 'bg-emerald-500/10 text-emerald-300'}`}>{roomWindow ? <Wrench size={19} /> : <CheckCircle2 size={19} />}</span><span className={`rounded-full px-2.5 py-1 text-[11px] font-black ${roomWindow ? 'bg-amber-500/10 text-amber-300' : 'bg-emerald-500/10 text-emerald-300'}`}>{roomWindow ? 'Có lịch bảo trì' : 'Sẵn sàng'}</span></div><h2 className="mt-4 text-lg font-black">{room.name}</h2><p className="mt-1 text-xs text-zinc-500">{room.screenType || 'Màn chiếu tiêu chuẩn'} · {room.capacity || 0} ghế</p>{roomWindow ? <p className="mt-3 text-xs leading-5 text-amber-100/70">Từ {formatDateTime(roomWindow.startTime)}<br />đến {formatDateTime(roomWindow.endTime)}</p> : null}</article>;
        })}
      </section>

      {showForm ? <form onSubmit={submit} className="rounded-2xl border border-brand-orange/25 bg-brand-orange/[0.05] p-5"><div className="flex items-center gap-2"><AlertTriangle className="text-brand-orange" size={19} /><h2 className="font-black">Thời gian phòng không thể phục vụ</h2></div><p className="mt-2 text-xs leading-5 text-zinc-500">Chỉ nhập khoảng thời gian thực tế. Hệ thống không cho tạo hai lịch bảo trì chồng nhau.</p><div className="mt-5 grid gap-4 lg:grid-cols-4"><label className="text-xs font-bold text-zinc-400">Phòng chiếu<select required value={form.auditoriumPublicId} onChange={event => setForm(current => ({ ...current, auditoriumPublicId: event.target.value }))} className="mt-2 min-h-11 w-full rounded-xl border border-white/10 bg-zinc-900 px-3 text-sm text-white"><option value="">Chọn phòng</option>{rooms.map(room => <option key={room.publicId} value={room.publicId}>{room.name}</option>)}</select></label><label className="text-xs font-bold text-zinc-400">Bắt đầu<input required type="datetime-local" value={form.startTime} onChange={event => setForm(current => ({ ...current, startTime: event.target.value }))} className="mt-2 min-h-11 w-full rounded-xl border border-white/10 bg-zinc-900 px-3 text-sm text-white" /></label><label className="text-xs font-bold text-zinc-400">Kết thúc<input required type="datetime-local" value={form.endTime} onChange={event => setForm(current => ({ ...current, endTime: event.target.value }))} className="mt-2 min-h-11 w-full rounded-xl border border-white/10 bg-zinc-900 px-3 text-sm text-white" /></label><label className="text-xs font-bold text-zinc-400">Lý do<input required minLength={3} maxLength={255} value={form.reason} onChange={event => setForm(current => ({ ...current, reason: event.target.value }))} placeholder="Ví dụ: Bảo trì máy chiếu" className="mt-2 min-h-11 w-full rounded-xl border border-white/10 bg-zinc-900 px-3 text-sm text-white" /></label></div><div className="mt-4 flex justify-end"><button disabled={state.loading} className="min-h-10 rounded-xl bg-white px-5 text-sm font-black text-black">Lưu lịch bảo trì</button></div></form> : null}

      {state.error ? <p className="rounded-xl border border-red-500/20 bg-red-500/10 p-4 text-sm text-red-200">{state.error}</p> : null}
      {state.success ? <p className="rounded-xl border border-emerald-500/20 bg-emerald-500/10 p-4 text-sm text-emerald-200">{state.success}</p> : null}

      <section className="overflow-hidden rounded-2xl border border-white/10 bg-white/[0.02]"><header className="flex items-center gap-3 border-b border-white/10 p-5"><span className="grid h-10 w-10 place-items-center rounded-xl bg-brand-orange/10 text-brand-orange"><CalendarClock size={19} /></span><div><h2 className="font-black">Lịch bảo trì đã ghi nhận</h2><p className="mt-1 text-xs text-zinc-600">Dùng để bàn giao giữa các ca quản lý.</p></div></header>{state.loading ? <p className="p-10 text-center text-sm text-zinc-500">Đang cập nhật tình trạng phòng…</p> : windows.length ? <div className="divide-y divide-white/5">{windows.map(item => { const room = rooms.find(value => value.publicId === item.auditoriumPublicId); return <article key={item.id} className="grid gap-3 p-4 md:grid-cols-[1fr_1fr_1.5fr_auto] md:items-center"><div><p className="font-bold">{room?.name || 'Phòng chiếu'}</p><p className="mt-1 text-xs text-zinc-600">Mã lịch #{item.id}</p></div><p className="text-xs leading-5 text-zinc-400">{formatDateTime(item.startTime)}<br />đến {formatDateTime(item.endTime)}</p><p className="text-sm text-zinc-300">{item.reason || 'Không ghi lý do'}</p>{item.status === 'ACTIVE' ? <button type="button" onClick={() => cancelWindow(item)} className="rounded-lg border border-red-500/20 px-3 py-2 text-xs font-black text-red-300">Hủy lịch</button> : <span className="text-xs font-bold text-zinc-600">Đã hủy</span>}</article>; })}</div> : <div className="p-10 text-center"><Building2 className="mx-auto text-zinc-700" /><p className="mt-3 text-sm text-zinc-500">Chưa có lịch bảo trì nào.</p></div>}</section>
    </div>
  );
}
