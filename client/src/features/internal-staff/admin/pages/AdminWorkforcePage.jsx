import { useCallback, useEffect, useMemo, useState } from 'react';
import { CalendarClock, CheckCircle2, ChevronLeft, ChevronRight, MapPin, Plus, ShieldCheck, Umbrella } from 'lucide-react';
import { useOutletContext, useSearchParams } from 'react-router-dom';
import { AsyncState, StatusBadge } from '@/components/common/ui/uiKit';
import useAdminAccess from '../hooks/useAdminAccess';
import {
  applyLeaveRequestAction,
  correctAttendance,
  createWorkShift,
  getAttendance,
  getEmployees,
  getLeaveRequests,
  getPiiGovernanceSummary,
  getWorkShifts
} from '../services/userAdminService';
import {
  ActionModal,
  ConsolePanel,
  MetricStrip,
} from '../components/OperationsConsole';
import { HrHero, PersonAvatar, UatGuide } from '../components/HrWorkspace';

const EMPTY_PAGE = { content: [], totalElements: 0 };
const STATUS_LABELS = {
  SCHEDULED: 'Đã xếp lịch', COMPLETED: 'Hoàn tất', CANCELLED: 'Đã hủy',
  ON_TIME: 'Đúng giờ', LATE: 'Đi muộn', CORRECTED: 'Đã hiệu chỉnh', ABSENT: 'Vắng mặt',
  PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối'
};
const ATTENDANCE_SOURCE_LABELS = {
  SELF_SERVICE: 'Nhân viên tự chấm công',
  MANUAL: 'Quản lý nhập thủ công',
  ADMIN_ADJUSTMENT: 'Quản lý hiệu chỉnh',
  SYSTEM: 'Hệ thống ghi nhận'
};
const LEAVE_TYPE_LABELS = {
  ANNUAL: 'Nghỉ phép năm',
  SICK: 'Nghỉ ốm',
  UNPAID: 'Nghỉ không lương',
  MATERNITY: 'Nghỉ thai sản',
  PATERNITY: 'Nghỉ chăm con',
  OTHER: 'Nghỉ khác'
};

const localDateTime = (date = new Date()) => {
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
};

const currentMonth = () => localDateTime().slice(0, 7);
const startOfWeek = (value = new Date()) => {
  const date = new Date(value);
  const day = date.getDay() || 7;
  date.setDate(date.getDate() - day + 1);
  date.setHours(0, 0, 0, 0);
  return date;
};
const dayKey = value => localDateTime(new Date(value)).slice(0, 10);
const monthRange = month => {
  const from = `${month}-01`;
  const end = new Date(`${from}T00:00:00`);
  end.setMonth(end.getMonth() + 1);
  end.setDate(0);
  return { from, to: localDateTime(end).slice(0, 10) };
};

export default function AdminWorkforcePage() {
  const can = useAdminAccess();
  const notify = useOutletContext()?.triggerToast || (() => undefined);
  const [searchParams] = useSearchParams();
  const [tab, setTab] = useState(() => ['attendance', 'leaves'].includes(searchParams.get('view')) ? searchParams.get('view') : 'shifts');
  const [month, setMonth] = useState(currentMonth);
  const [weekStart, setWeekStart] = useState(() => startOfWeek());
  const [employees, setEmployees] = useState([]);
  const [shifts, setShifts] = useState(EMPTY_PAGE);
  const [attendance, setAttendance] = useState(EMPTY_PAGE);
  const [leaves, setLeaves] = useState(EMPTY_PAGE);
  const [pii, setPii] = useState(null);
  const [state, setState] = useState({ loading: true, error: '' });
  const [shiftOpen, setShiftOpen] = useState(false);
  const [shiftForm, setShiftForm] = useState(() => ({
    employeeId: '', scheduledStart: localDateTime(),
    scheduledEnd: localDateTime(new Date(new Date().getTime() + 8 * 3600_000)), location: '', note: ''
  }));
  const [correction, setCorrection] = useState(null);
  const [review, setReview] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const range = useMemo(() => monthRange(month), [month]);
  const weekDays = useMemo(() => Array.from({ length: 7 }, (_, index) => {
    const date = new Date(weekStart);
    date.setDate(date.getDate() + index);
    return date;
  }), [weekStart]);
  const visibleShifts = useMemo(() => shifts.content?.filter(item => {
    const key = dayKey(item.scheduledStart);
    return key >= dayKey(weekDays[0]) && key <= dayKey(weekDays[6]);
  }) || [], [shifts.content, weekDays]);

  const moveWeek = amount => {
    const next = new Date(weekStart);
    next.setDate(next.getDate() + amount * 7);
    setWeekStart(next);
    setMonth(localDateTime(next).slice(0, 7));
  };

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const params = { ...range, page: 0, size: 200, sort: 'createdAt,desc' };
      const [employeePage, shiftPage, attendancePage, leavePage, piiSummary] = await Promise.all([
        getEmployees({ page: 0, size: 200, status: 'ACTIVE' }),
        getWorkShifts({ ...params, sort: 'scheduledStart,asc' }),
        getAttendance(params),
        getLeaveRequests(params),
        can('SYSTEM_CONFIGURATION') ? getPiiGovernanceSummary() : Promise.resolve(null)
      ]);
      setEmployees(employeePage?.content || []);
      setShifts(shiftPage || EMPTY_PAGE);
      setAttendance(attendancePage || EMPTY_PAGE);
      setLeaves(leavePage || EMPTY_PAGE);
      setPii(piiSummary);
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải dữ liệu vận hành nhân sự.' });
    }
  }, [can, range]);

  useEffect(() => {
    // Loading remote workforce state is the synchronization performed by this effect.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const metrics = [
    { label: 'Ca trong kỳ', value: shifts.totalElements || 0, hint: 'Nguồn lịch làm việc', icon: CalendarClock, tone: 'blue' },
    { label: 'Đã chấm công', value: attendance.totalElements || 0, hint: 'Có dấu thời gian và nguồn', icon: CheckCircle2, tone: 'green' },
    { label: 'Nghỉ chờ duyệt', value: leaves.content?.filter(item => item.status === 'PENDING').length || 0, hint: 'Yêu cầu maker-checker', icon: Umbrella, tone: 'amber' },
    { label: 'PII được bảo vệ', value: pii ? `${pii.protectedProfiles}/${pii.totalProfiles}` : 'Theo quyền', hint: 'AES-GCM + retention', icon: ShieldCheck, tone: 'purple' }
  ];

  const submitShift = async event => {
    event.preventDefault();
    setSubmitting(true);
    try {
      await createWorkShift({
        ...shiftForm,
        employeeId: Number(shiftForm.employeeId),
        scheduledStart: `${shiftForm.scheduledStart}:00`,
        scheduledEnd: `${shiftForm.scheduledEnd}:00`
      });
      notify('Đã phân ca và ghi nhật ký thao tác.');
      setShiftOpen(false);
      await load();
    } catch (error) {
      notify(error?.message || 'Không thể tạo ca làm.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const submitCorrection = async event => {
    event.preventDefault();
    setSubmitting(true);
    try {
      await correctAttendance(correction.shiftId, {
        checkInAt: `${correction.checkInAt}:00`,
        checkOutAt: `${correction.checkOutAt}:00`,
        reason: correction.reason,
        expectedVersion: correction.version
      });
      notify('Đã hiệu chỉnh chấm công và lưu người thực hiện.');
      setCorrection(null);
      await load();
    } catch (error) {
      notify(error?.message || 'Không thể hiệu chỉnh chấm công.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const submitReview = async event => {
    event.preventDefault();
    setSubmitting(true);
    try {
      await applyLeaveRequestAction(review.id, {
        type: review.type,
        note: review.note,
        expectedVersion: review.version
      });
      notify(review.type === 'APPROVE' ? 'Đã duyệt nghỉ phép.' : 'Đã từ chối yêu cầu nghỉ.');
      setReview(null);
      await load();
    } catch (error) {
      notify(error?.message || 'Không thể xử lý yêu cầu nghỉ.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const openCorrection = shift => {
    const existing = attendance.content?.find(item => item.shiftId === shift.id);
    setCorrection({
      shiftId: shift.id,
      version: existing?.version ?? null,
      checkInAt: (existing?.checkInAt || shift.scheduledStart).slice(0, 16),
      checkOutAt: (existing?.checkOutAt || shift.scheduledEnd).slice(0, 16),
      reason: ''
    });
  };

  return (
    <main className="min-h-full space-y-5 text-white">
      <HrHero context="Lịch làm việc theo tuần" title="Lịch ca & chấm công"
        description="Xếp ca trên lịch tuần, phát hiện chấm công bất thường và xử lý nghỉ phép. Admin nhìn thấy người nào đang trống lịch thay vì phải đọc từng dòng dữ liệu."
        actions={<><UatGuide compact />{can('EMPLOYEE_UPDATE') ? <button type="button" onClick={() => setShiftOpen(true)} className="flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2.5 text-sm font-black text-black hover:bg-orange-400"><Plus size={17} /> Phân ca mới</button> : null}</>} />

      <MetricStrip items={metrics} />

      <div className="flex flex-col gap-3 rounded-2xl border border-white/10 bg-white/[0.025] p-3 md:flex-row md:items-center md:justify-between">
        <div className="flex gap-2">{[
          ['shifts', 'Lịch ca tuần'], ['attendance', 'Dữ liệu chấm công'], ['leaves', 'Nghỉ phép']
        ].map(([key, label]) => <button key={key} type="button" onClick={() => setTab(key)} className={`rounded-xl px-4 py-2 text-sm font-black ${tab === key ? 'bg-white text-black' : 'text-zinc-400 hover:bg-white/5'}`}>{label}</button>)}</div>
        <div className="flex items-center gap-2">
          <button type="button" aria-label="Tuần trước" onClick={() => moveWeek(-1)} className="rounded-xl border border-white/10 p-2 text-zinc-400 hover:bg-white/5 hover:text-white"><ChevronLeft size={18} /></button>
          <input aria-label="Chọn ngày trong tuần" type="date" value={dayKey(weekStart)} onChange={event => { const next = startOfWeek(event.target.value + 'T00:00:00'); setWeekStart(next); setMonth(event.target.value.slice(0, 7)); }} className="rounded-xl border border-white/10 bg-black/30 px-4 py-2 text-sm text-white" />
          <button type="button" aria-label="Tuần sau" onClick={() => moveWeek(1)} className="rounded-xl border border-white/10 p-2 text-zinc-400 hover:bg-white/5 hover:text-white"><ChevronRight size={18} /></button>
        </div>
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load}
        empty={tab === 'shifts' ? !shifts.content?.length : tab === 'attendance' ? !attendance.content?.length : !leaves.content?.length}
        emptyMessage={tab === 'shifts' ? 'Chưa có ca làm trong kỳ' : tab === 'attendance' ? 'Chưa có dữ liệu chấm công' : 'Chưa có yêu cầu nghỉ phép'}>
        <ConsolePanel className="overflow-hidden">
          {tab === 'shifts' ? (
            <div className="overflow-x-auto">
              <div className="min-w-[1180px]">
                <div className="grid grid-cols-[220px_repeat(7,minmax(135px,1fr))] border-b border-white/10 bg-white/[0.025]">
                  <div className="p-4 text-[10px] font-black uppercase tracking-[0.16em] text-zinc-600">Nhân viên</div>
                  {weekDays.map((day, index) => <div key={dayKey(day)} className={'border-l border-white/5 p-3 text-center ' + (dayKey(day) === dayKey(new Date()) ? 'bg-orange-500/[0.07]' : '')}><p className="text-[10px] font-black uppercase text-zinc-600">{['Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy', 'Chủ nhật'][index]}</p><p className={'mt-1 text-sm font-black ' + (dayKey(day) === dayKey(new Date()) ? 'text-orange-300' : 'text-zinc-300')}>{day.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })}</p></div>)}
                </div>
                {employees.map(employee => (
                  <div key={employee.accountId} className="grid min-h-28 grid-cols-[220px_repeat(7,minmax(135px,1fr))] border-b border-white/5 last:border-b-0">
                    <div className="flex items-start gap-3 p-4"><PersonAvatar name={employee.fullName} /><div className="min-w-0"><p className="truncate text-sm font-black">{employee.fullName}</p><p className="mt-1 truncate text-[10px] text-zinc-600">{employee.employeeCode} · {employee.positionName || 'Chưa có vị trí'}</p></div></div>
                    {weekDays.map(day => {
                      const dayShifts = visibleShifts.filter(item => item.employeeId === employee.accountId && dayKey(item.scheduledStart) === dayKey(day));
                      return (
                        <div key={dayKey(day)} className={'border-l border-white/5 p-2 ' + (dayKey(day) === dayKey(new Date()) ? 'bg-orange-500/[0.025]' : '')}>
                          {dayShifts.length ? dayShifts.map(shift => (
                            <button key={shift.id} type="button" onClick={() => openCorrection(shift)} className="mb-2 w-full rounded-xl border border-blue-500/20 bg-blue-500/[0.08] p-2.5 text-left hover:border-blue-400/40">
                              <p className="text-xs font-black text-blue-200">{new Date(shift.scheduledStart).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })} – {new Date(shift.scheduledEnd).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</p>
                              <p className="mt-1 flex items-center gap-1 text-[10px] text-blue-200/55"><MapPin size={10} /> {shift.location || 'Chưa ghi địa điểm'}</p>
                            </button>
                          )) : <button type="button" onClick={() => { setShiftForm(value => ({ ...value, employeeId: String(employee.accountId), scheduledStart: dayKey(day) + 'T09:00', scheduledEnd: dayKey(day) + 'T17:00' })); setShiftOpen(true); }} className="grid h-full min-h-20 w-full place-items-center rounded-xl border border-dashed border-transparent text-[10px] font-bold text-zinc-700 hover:border-white/10 hover:text-zinc-500">+ Xếp ca</button>}
                        </div>
                      );
                    })}
                  </div>
                ))}
              </div>
            </div>
          ) : null}
          <div className={tab === 'shifts' ? 'hidden overflow-x-auto' : 'overflow-x-auto'}>
            {tab === 'shifts' && <table className="min-w-full text-left text-sm"><thead className="bg-white/[0.025] text-xs uppercase text-zinc-500"><tr><th className="p-4">Nhân viên</th><th className="p-4">Bắt đầu</th><th className="p-4">Kết thúc</th><th className="p-4">Địa điểm</th><th className="p-4">Trạng thái</th><th className="p-4 text-right">Kiểm soát</th></tr></thead><tbody className="divide-y divide-white/5">{shifts.content?.map(item => <tr key={item.id}><td className="p-4"><p className="font-black">{item.employeeName}</p><p className="text-xs text-zinc-600">{item.employeeCode}</p></td><td className="p-4">{new Date(item.scheduledStart).toLocaleString('vi-VN')}</td><td className="p-4">{new Date(item.scheduledEnd).toLocaleString('vi-VN')}</td><td className="p-4 text-zinc-400">{item.location || '—'}</td><td className="p-4"><StatusBadge status={item.status} label={STATUS_LABELS[item.status]} /></td><td className="p-4 text-right">{item.status !== 'CANCELLED' && <button type="button" onClick={() => openCorrection(item)} className="rounded-lg border border-white/10 px-3 py-2 text-xs font-black text-zinc-300">Hiệu chỉnh giờ</button>}</td></tr>)}</tbody></table>}
            {tab === 'attendance' && <table className="min-w-full text-left text-sm"><thead className="bg-white/[0.025] text-xs uppercase text-zinc-500"><tr><th className="p-4">Nhân viên</th><th className="p-4">Vào ca</th><th className="p-4">Ra ca</th><th className="p-4">Phút công</th><th className="p-4">Nguồn</th><th className="p-4">Trạng thái</th></tr></thead><tbody className="divide-y divide-white/5">{attendance.content?.map(item => <tr key={item.id}><td className="p-4 font-black">{item.employeeName}</td><td className="p-4">{item.checkInAt ? new Date(item.checkInAt).toLocaleString('vi-VN') : '—'}</td><td className="p-4">{item.checkOutAt ? new Date(item.checkOutAt).toLocaleString('vi-VN') : '—'}</td><td className="p-4">{item.workedMinutes} <span className="text-zinc-600">(+{item.overtimeMinutes} tăng ca)</span></td><td className="p-4 text-zinc-400">{ATTENDANCE_SOURCE_LABELS[item.source] || item.source}</td><td className="p-4"><StatusBadge status={item.status} label={STATUS_LABELS[item.status]} /></td></tr>)}</tbody></table>}
            {tab === 'leaves' && <table className="min-w-full text-left text-sm"><thead className="bg-white/[0.025] text-xs uppercase text-zinc-500"><tr><th className="p-4">Nhân viên</th><th className="p-4">Loại nghỉ</th><th className="p-4">Khoảng ngày</th><th className="p-4">Lý do</th><th className="p-4">Trạng thái</th><th className="p-4 text-right">Duyệt</th></tr></thead><tbody className="divide-y divide-white/5">{leaves.content?.map(item => <tr key={item.id}><td className="p-4 font-black">{item.employeeName}</td><td className="p-4">{LEAVE_TYPE_LABELS[item.leaveType] || item.leaveType} · {item.paid ? 'Hưởng lương' : 'Không lương'}</td><td className="p-4">{item.startDate} → {item.endDate}</td><td className="max-w-xs p-4 text-zinc-400">{item.reason}</td><td className="p-4"><StatusBadge status={item.status} label={STATUS_LABELS[item.status]} /></td><td className="p-4 text-right">{item.status === 'PENDING' && <span className="inline-flex gap-2"><button type="button" onClick={() => setReview({ ...item, type: 'APPROVE', note: '' })} className="rounded-lg bg-emerald-500/15 px-3 py-2 text-xs font-black text-emerald-400">Duyệt</button><button type="button" onClick={() => setReview({ ...item, type: 'REJECT', note: '' })} className="rounded-lg bg-red-500/15 px-3 py-2 text-xs font-black text-red-400">Từ chối</button></span>}</td></tr>)}</tbody></table>}
          </div>
        </ConsolePanel>
      </AsyncState>

      <ActionModal open={shiftOpen} onClose={() => setShiftOpen(false)} title="Phân ca làm việc" description="Hệ thống chặn ca trùng và giới hạn mỗi ca tối đa 16 giờ." onSubmit={submitShift} submitLabel="Tạo ca" submitting={submitting}>
        <label className="block text-xs font-black uppercase text-zinc-500">Nhân viên *<select required value={shiftForm.employeeId} onChange={event => setShiftForm(value => ({ ...value, employeeId: event.target.value }))} className="mt-2 w-full rounded-xl border border-white/10 bg-black/40 p-3 text-sm text-white"><option value="">Chọn nhân viên</option>{employees.map(item => <option key={item.accountId} value={item.accountId}>{item.fullName} · {item.employeeCode}</option>)}</select></label>
        <div className="grid gap-3 sm:grid-cols-2"><label className="text-xs font-black uppercase text-zinc-500">Bắt đầu *<input required type="datetime-local" value={shiftForm.scheduledStart} onChange={event => setShiftForm(value => ({ ...value, scheduledStart: event.target.value }))} className="mt-2 w-full rounded-xl border border-white/10 bg-black/40 p-3 text-white" /></label><label className="text-xs font-black uppercase text-zinc-500">Kết thúc *<input required type="datetime-local" value={shiftForm.scheduledEnd} onChange={event => setShiftForm(value => ({ ...value, scheduledEnd: event.target.value }))} className="mt-2 w-full rounded-xl border border-white/10 bg-black/40 p-3 text-white" /></label></div>
        <label className="block text-xs font-black uppercase text-zinc-500">Địa điểm<input value={shiftForm.location} onChange={event => setShiftForm(value => ({ ...value, location: event.target.value }))} className="mt-2 w-full rounded-xl border border-white/10 bg-black/40 p-3 text-white" /></label>
      </ActionModal>

      <ActionModal open={Boolean(correction)} onClose={() => setCorrection(null)} title="Hiệu chỉnh chấm công" description="Mọi chỉnh sửa thủ công bắt buộc có lý do và người thực hiện." onSubmit={submitCorrection} submitLabel="Lưu hiệu chỉnh" submitting={submitting}>
        <div className="grid gap-3 sm:grid-cols-2"><label className="text-xs font-black uppercase text-zinc-500">Giờ vào *<input required type="datetime-local" value={correction?.checkInAt || ''} onChange={event => setCorrection(value => ({ ...value, checkInAt: event.target.value }))} className="mt-2 w-full rounded-xl border border-white/10 bg-black/40 p-3 text-white" /></label><label className="text-xs font-black uppercase text-zinc-500">Giờ ra *<input required type="datetime-local" value={correction?.checkOutAt || ''} onChange={event => setCorrection(value => ({ ...value, checkOutAt: event.target.value }))} className="mt-2 w-full rounded-xl border border-white/10 bg-black/40 p-3 text-white" /></label></div>
        <label className="block text-xs font-black uppercase text-zinc-500">Lý do *<textarea required minLength={5} value={correction?.reason || ''} onChange={event => setCorrection(value => ({ ...value, reason: event.target.value }))} className="mt-2 min-h-24 w-full rounded-xl border border-white/10 bg-black/40 p-3 text-white" /></label>
      </ActionModal>

      <ActionModal open={Boolean(review)} onClose={() => setReview(null)} title={review?.type === 'APPROVE' ? 'Duyệt nghỉ phép' : 'Từ chối nghỉ phép'} description="Người gửi không thể tự duyệt yêu cầu của mình." onSubmit={submitReview} submitLabel={review?.type === 'APPROVE' ? 'Xác nhận duyệt' : 'Xác nhận từ chối'} submitting={submitting} tone={review?.type === 'REJECT' ? 'danger' : 'orange'}>
        <label className="block text-xs font-black uppercase text-zinc-500">Ghi chú {review?.type === 'REJECT' ? '*' : ''}<textarea required={review?.type === 'REJECT'} minLength={review?.type === 'REJECT' ? 5 : undefined} value={review?.note || ''} onChange={event => setReview(value => ({ ...value, note: event.target.value }))} className="mt-2 min-h-24 w-full rounded-xl border border-white/10 bg-black/40 p-3 text-white" /></label>
      </ActionModal>
    </main>
  );
}
