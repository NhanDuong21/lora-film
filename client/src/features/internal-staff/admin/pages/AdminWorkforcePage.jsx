import { useCallback, useEffect, useMemo, useState } from 'react';
import { CalendarClock, CheckCircle2, ChevronLeft, ChevronRight, Clock3, MapPin, Plus, ShieldCheck, Trash2, Umbrella } from 'lucide-react';
import { useOutletContext, useSearchParams } from 'react-router-dom';
import { AsyncState, StatusBadge } from '@/components/common/ui/uiKit';
import SearchableSelect from '@/components/common/SearchableSelect';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import useAdminAccess from '../hooks/useAdminAccess';
import {
  applyLeaveRequestAction,
  correctAttendance,
  createWorkShiftBatch,
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
const initialShiftForm = (workDate = dayKey(new Date()), employeeId = '') => ({
  employeeId: String(employeeId || ''),
  workDate,
  cinemaId: '',
  segments: [{ start: '09:00', end: '17:00' }],
  note: ''
});
const nextDate = dateText => {
  const date = new Date(dateText + 'T00:00:00');
  date.setDate(date.getDate() + 1);
  return dayKey(date);
};
const buildShiftPlan = (workDate, segments) => {
  const periods = segments.map(segment => ({
    scheduledStart: workDate + 'T' + segment.start + ':00',
    scheduledEnd: (segment.end <= segment.start ? nextDate(workDate) : workDate) + 'T' + segment.end + ':00'
  })).sort((left, right) => left.scheduledStart.localeCompare(right.scheduledStart));
  const durations = periods.map(period => (new Date(period.scheduledEnd) - new Date(period.scheduledStart)) / 60000);
  const totalMinutes = durations.reduce((sum, minutes) => sum + minutes, 0);
  const spanMinutes = periods.length
    ? (new Date(periods.at(-1).scheduledEnd) - new Date(periods[0].scheduledStart)) / 60000
    : 0;
  return { periods, durations, totalMinutes, breakMinutes: Math.max(0, spanMinutes - totalMinutes) };
};
const hoursLabel = minutes => {
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return [hours ? hours + ' giờ' : '', rest ? rest + ' phút' : ''].filter(Boolean).join(' ') || '0 phút';
};

export default function AdminWorkforcePage() {
  const can = useAdminAccess();
  const notify = useOutletContext()?.triggerToast || (() => undefined);
  const [searchParams] = useSearchParams();
  const [tab, setTab] = useState(() => ['attendance', 'leaves'].includes(searchParams.get('view')) ? searchParams.get('view') : 'shifts');
  const [month, setMonth] = useState(currentMonth);
  const [weekStart, setWeekStart] = useState(() => startOfWeek());
  const [employees, setEmployees] = useState([]);
  const [cinemas, setCinemas] = useState([]);
  const [shifts, setShifts] = useState(EMPTY_PAGE);
  const [attendance, setAttendance] = useState(EMPTY_PAGE);
  const [leaves, setLeaves] = useState(EMPTY_PAGE);
  const [pii, setPii] = useState(null);
  const [state, setState] = useState({ loading: true, error: '' });
  const [shiftOpen, setShiftOpen] = useState(false);
  const [shiftForm, setShiftForm] = useState(initialShiftForm);
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
      const [employeePage, shiftPage, attendancePage, leavePage, piiSummary, cinemaEnvelope] = await Promise.all([
        getEmployees({ page: 0, size: 200, status: 'ACTIVE' }),
        getWorkShifts({ ...params, sort: 'scheduledStart,asc' }),
        getAttendance(params),
        getLeaveRequests(params),
        can('SYSTEM_CONFIGURATION') ? getPiiGovernanceSummary() : Promise.resolve(null),
        adminCinemaService.getCinemas({ page: 0, size: 200, status: 'ACTIVE', sort: 'name,asc' })
          .catch(() => null)
      ]);
      setEmployees(employeePage?.content || []);
      setCinemas(cinemaEnvelope?.data?.data || []);
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
    const plan = buildShiftPlan(shiftForm.workDate, shiftForm.segments);
    if (!shiftForm.employeeId) {
      notify('Vui lòng chọn nhân viên cần phân ca.', 'error');
      return;
    }
    if (!shiftForm.cinemaId) {
      notify('Vui lòng chọn địa điểm từ danh sách rạp.', 'error');
      return;
    }
    if (plan.durations.some(minutes => minutes <= 0 || minutes > 16 * 60)) {
      notify('Mỗi khung giờ phải lớn hơn 0 và không vượt quá 16 giờ.', 'error');
      return;
    }
    if (plan.totalMinutes > 16 * 60) {
      notify('Tổng thời gian làm việc trong lần phân ca không được vượt quá 16 giờ.', 'error');
      return;
    }
    if (plan.periods.some((period, index) => index > 0 && period.scheduledStart < plan.periods[index - 1].scheduledEnd)) {
      notify('Các khung giờ đang bị trùng nhau. Vui lòng kiểm tra lại.', 'error');
      return;
    }
    const selectedCinema = cinemas.find(cinema => cinema.publicId === shiftForm.cinemaId);
    if (!selectedCinema) {
      notify('Địa điểm đã chọn không còn hoạt động. Vui lòng tải lại và chọn rạp khác.', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await createWorkShiftBatch({
        employeeId: Number(shiftForm.employeeId),
        periods: plan.periods,
        location: selectedCinema.name,
        note: shiftForm.note.trim() || undefined
      });
      notify('Đã phân ' + plan.periods.length + ' khung giờ và ghi nhật ký thao tác.');
      setShiftOpen(false);
      setShiftForm(initialShiftForm());
      await load();
    } catch (error) {
      notify(error?.message || 'Không thể tạo ca làm.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const openShiftForm = (employeeId = '', workDate = dayKey(new Date()), segments) => {
    setShiftForm({
      ...initialShiftForm(workDate, employeeId),
      segments: segments || [{ start: '09:00', end: '17:00' }]
    });
    setShiftOpen(true);
  };

  const setShiftPreset = preset => {
    setShiftForm(value => ({
      ...value,
      segments: preset === 'split'
        ? [{ start: '08:00', end: '12:00' }, { start: '14:00', end: '18:00' }]
        : [{ start: '09:00', end: '17:00' }]
    }));
  };

  const addShiftSegment = () => {
    setShiftForm(value => ({
      ...value,
      segments: [...value.segments, value.segments.length === 1
        ? { start: '14:00', end: '18:00' }
        : { start: '18:30', end: '22:30' }]
    }));
  };

  const updateShiftSegment = (index, field, nextValue) => {
    setShiftForm(value => ({
      ...value,
      segments: value.segments.map((segment, segmentIndex) =>
        segmentIndex === index ? { ...segment, [field]: nextValue } : segment)
    }));
  };

  const removeShiftSegment = index => {
    setShiftForm(value => ({
      ...value,
      segments: value.segments.filter((_, segmentIndex) => segmentIndex !== index)
    }));
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

  const employeeOptions = employees.map(employee => ({
    value: String(employee.accountId),
    label: employee.fullName,
    subtitle: employee.employeeCode + ' · ' + (employee.positionName || 'Chưa có vị trí')
  }));
  const cinemaOptions = cinemas.map(cinema => ({
    value: cinema.publicId,
    label: cinema.name,
    subtitle: [cinema.address, cinema.district, cinema.city].filter(Boolean).join(' · '),
    badge: 'Đang hoạt động'
  }));
  const shiftPlan = buildShiftPlan(shiftForm.workDate, shiftForm.segments);

  return (
    <main className="min-h-full space-y-5 text-white">
      <HrHero context="Lịch làm việc theo tuần" title="Lịch ca & chấm công"
        description="Xếp ca trên lịch tuần, phát hiện chấm công bất thường và xử lý nghỉ phép. Admin nhìn thấy người nào đang trống lịch thay vì phải đọc từng dòng dữ liệu."
        actions={<><UatGuide compact />{can('EMPLOYEE_UPDATE') ? <button type="button" onClick={() => openShiftForm()} className="flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2.5 text-sm font-black text-black hover:bg-orange-400"><Plus size={17} /> Phân ca mới</button> : null}</>} />

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
                          {dayShifts.map(shift => (
                            <button key={shift.id} type="button" onClick={() => openCorrection(shift)} className="mb-2 w-full rounded-xl border border-blue-500/20 bg-blue-500/[0.08] p-2.5 text-left hover:border-blue-400/40">
                              <p className="text-xs font-black text-blue-200">{new Date(shift.scheduledStart).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })} – {new Date(shift.scheduledEnd).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</p>
                              <p className="mt-1 flex items-center gap-1 text-[10px] text-blue-200/55"><MapPin size={10} /> {shift.location || 'Chưa ghi địa điểm'}</p>
                            </button>
                          ))}
                          <button type="button" onClick={() => openShiftForm(employee.accountId, dayKey(day))} className={'grid w-full place-items-center rounded-xl border border-dashed text-[10px] font-bold transition ' + (dayShifts.length ? 'h-8 border-white/10 text-zinc-600 hover:border-orange-500/30 hover:text-orange-300' : 'min-h-20 border-transparent text-zinc-700 hover:border-white/10 hover:text-zinc-500')}>{dayShifts.length ? '+ Thêm khung giờ' : '+ Xếp ca'}</button>
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

      <ActionModal open={shiftOpen} onClose={() => setShiftOpen(false)} title="Phân ca làm việc" description="Một lần phân ca có thể gồm nhiều khung giờ. Nếu một khung bị lỗi hoặc trùng lịch, hệ thống sẽ không lưu bất kỳ khung nào." onSubmit={submitShift} submitLabel={'Phân ' + shiftForm.segments.length + ' khung giờ'} submitting={submitting} wide>
        <div className="grid gap-4 sm:grid-cols-2">
          <div><label className="mb-2 block text-xs font-black uppercase text-zinc-500">Nhân viên *</label><SearchableSelect value={shiftForm.employeeId} onChange={employeeId => setShiftForm(value => ({ ...value, employeeId }))} options={employeeOptions} placeholder="Chọn hoặc tìm nhân viên" ariaLabel="Chọn nhân viên cần phân ca" /></div>
          <div><label className="mb-2 block text-xs font-black uppercase text-zinc-500">Ngày làm việc *</label><input required type="date" value={shiftForm.workDate} onChange={event => setShiftForm(value => ({ ...value, workDate: event.target.value }))} className="h-11 w-full rounded-xl border border-white/10 bg-zinc-950 px-3 text-sm text-white outline-none focus:border-orange-500" /></div>
        </div>

        <div>
          <label className="mb-2 block text-xs font-black uppercase text-zinc-500">Địa điểm làm việc *</label>
          <SearchableSelect value={shiftForm.cinemaId} onChange={cinemaId => setShiftForm(value => ({ ...value, cinemaId }))} options={cinemaOptions} placeholder="Chọn rạp đang hoạt động" ariaLabel="Chọn địa điểm làm việc" disabled={!cinemaOptions.length} />
          {!cinemaOptions.length ? <p className="mt-2 rounded-xl border border-amber-500/20 bg-amber-500/5 p-3 text-xs leading-5 text-amber-300">Chưa có rạp đang hoạt động để phân ca. Hãy kiểm tra mục “Cụm rạp & giờ hoạt động”.</p> : <p className="mt-2 text-xs leading-5 text-zinc-600">Địa điểm được lấy từ danh mục rạp, admin không cần nhập tay nên không phát sinh sai chính tả.</p>}
        </div>

        <div className="rounded-2xl border border-white/10 bg-black/20 p-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div><p className="text-sm font-black text-zinc-200">Khung giờ làm việc</p><p className="mt-1 text-xs text-zinc-600">Mỗi khung có lượt chấm vào và chấm ra riêng.</p></div>
            <div className="flex gap-2"><button type="button" onClick={() => setShiftPreset('continuous')} className="rounded-lg border border-white/10 px-3 py-2 text-xs font-black text-zinc-400 hover:bg-white/5 hover:text-white">Ca liền 09–17</button><button type="button" onClick={() => setShiftPreset('split')} className="rounded-lg border border-orange-500/30 bg-orange-500/10 px-3 py-2 text-xs font-black text-orange-300 hover:bg-orange-500/15">Ca gãy 08–12 · 14–18</button></div>
          </div>

          <div className="mt-4 space-y-3">
            {shiftForm.segments.map((segment, index) => (
              <div key={index} className="grid items-end gap-3 rounded-xl border border-white/10 bg-white/[0.025] p-3 sm:grid-cols-[72px_1fr_1fr_38px]">
                <div className="pb-3 text-xs font-black text-zinc-500">Khung {index + 1}</div>
                <label className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Bắt đầu *<input required type="time" value={segment.start} onChange={event => updateShiftSegment(index, 'start', event.target.value)} className="mt-2 h-11 w-full rounded-xl border border-white/10 bg-zinc-950 px-3 text-sm text-white outline-none focus:border-orange-500" /></label>
                <label className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Kết thúc *<input required type="time" value={segment.end} onChange={event => updateShiftSegment(index, 'end', event.target.value)} className="mt-2 h-11 w-full rounded-xl border border-white/10 bg-zinc-950 px-3 text-sm text-white outline-none focus:border-orange-500" /></label>
                <button type="button" aria-label={'Xóa khung giờ ' + (index + 1)} disabled={shiftForm.segments.length === 1} onClick={() => removeShiftSegment(index)} className="grid h-11 place-items-center rounded-xl border border-white/10 text-zinc-600 hover:border-red-500/30 hover:text-red-400 disabled:cursor-not-allowed disabled:opacity-30"><Trash2 size={16} /></button>
              </div>
            ))}
          </div>

          <div className="mt-3 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <button type="button" disabled={shiftForm.segments.length >= 4} onClick={addShiftSegment} className="inline-flex items-center gap-2 text-xs font-black text-orange-300 hover:text-orange-200 disabled:opacity-40"><Plus size={15} /> Thêm khung giờ</button>
            <div className="flex items-center gap-2 text-xs text-zinc-500"><Clock3 size={14} /><span>Tổng làm <strong className="text-zinc-300">{hoursLabel(shiftPlan.totalMinutes)}</strong>{shiftPlan.breakMinutes > 0 ? ' · Nghỉ giữa ca ' + hoursLabel(shiftPlan.breakMinutes) : ''}</span></div>
          </div>
          <p className="mt-3 text-[11px] leading-5 text-zinc-600">Nếu giờ kết thúc nhỏ hơn giờ bắt đầu, hệ thống hiểu là kết thúc vào ngày hôm sau, ví dụ 22:00–06:00.</p>
        </div>

        <label className="block text-xs font-black uppercase text-zinc-500">Ghi chú nội bộ<textarea value={shiftForm.note} maxLength={500} onChange={event => setShiftForm(value => ({ ...value, note: event.target.value }))} placeholder="Ví dụ: nghỉ giữa ca 2 giờ, bàn giao quầy vé…" className="mt-2 min-h-20 w-full rounded-xl border border-white/10 bg-black/40 p-3 text-sm normal-case text-white outline-none focus:border-orange-500" /></label>
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
