import { useCallback, useEffect, useMemo, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import {
  CalendarClock,
  CalendarDays,
  CalendarPlus,
  Check,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock3,
  Mail,
  MapPin,
  Search,
  Umbrella,
  UsersRound,
  X,
} from 'lucide-react';
import SearchableSelect from '@/components/common/SearchableSelect';
import { StatusBadge } from '@/components/common/ui/uiKit';
import {
  ActionModal,
  ConsolePanel,
  MetricStrip,
} from '@/features/internal-staff/admin/components/OperationsConsole';
import {
  EmptyWorkspace,
  HrHero,
  PersonAvatar,
} from '@/features/internal-staff/admin/components/HrWorkspace';
import { getEmployeeAvatarRole } from '@/features/internal-staff/admin/components/avatarUtils';
import managerCinemaService from '../services/managerCinemaService';

const STATUS = {
  ACTIVE: 'Đang làm việc',
  ON_LEAVE: 'Đang nghỉ',
  SUSPENDED: 'Tạm ngưng',
  RESIGNED: 'Đã nghỉ việc',
  SCHEDULED: 'Đã xếp ca',
  COMPLETED: 'Đã hoàn thành',
  CANCELLED: 'Đã hủy',
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',
  ON_TIME: 'Đúng giờ',
  LATE: 'Đi muộn',
  ABSENT: 'Vắng mặt',
  CORRECTED: 'Đã điều chỉnh',
};

const LEAVE_TYPES = {
  ANNUAL: 'Nghỉ phép năm',
  SICK: 'Nghỉ ốm',
  UNPAID: 'Nghỉ không lương',
  MATERNITY: 'Nghỉ thai sản',
  PATERNITY: 'Nghỉ chăm con',
  OTHER: 'Nghỉ khác',
};

const DAY_NAMES = ['Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy', 'Chủ nhật'];
const ATTENDANCE_ISSUES = new Set(['LATE', 'ABSENT']);

const inputDate = value => {
  const date = new Date(value);
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 10);
};

const startOfWeek = (value = new Date()) => {
  const date = new Date(value);
  const day = date.getDay() || 7;
  date.setDate(date.getDate() - day + 1);
  date.setHours(0, 0, 0, 0);
  return date;
};

const addDays = (value, amount) => {
  const date = new Date(value);
  date.setDate(date.getDate() + amount);
  return date;
};

const dayKey = value => inputDate(new Date(value));

const formatTime = value => new Intl.DateTimeFormat('vi-VN', {
  hour: '2-digit',
  minute: '2-digit',
}).format(new Date(value));

const formatDateTime = value => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
  : 'Chưa ghi nhận';

const formatDate = value => value
  ? new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(`${value}T00:00:00`))
  : '—';

const initialShiftForm = (workDate = dayKey(new Date()), employeeId = '') => ({
  employeeId: String(employeeId || ''),
  workDate,
  start: '09:00',
  end: '17:00',
  note: '',
});

const buildShiftPlan = form => {
  const start = new Date(`${form.workDate}T${form.start}:00`);
  const end = new Date(`${form.workDate}T${form.end}:00`);
  if (end <= start) end.setDate(end.getDate() + 1);
  return {
    scheduledStart: `${form.workDate}T${form.start}:00`,
    scheduledEnd: `${dayKey(end)}T${form.end}:00`,
    minutes: Math.round((end - start) / 60_000),
    overnight: dayKey(end) !== form.workDate,
  };
};

const durationLabel = minutes => {
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return [hours ? `${hours} giờ` : '', rest ? `${rest} phút` : ''].filter(Boolean).join(' ') || '0 phút';
};

export default function ManagerStaffPage() {
  const { selectedCinema, selectedCinemaId, cinemaState } = useOutletContext();
  const [weekStart, setWeekStart] = useState(() => startOfWeek());
  const [tab, setTab] = useState('shifts');
  const [data, setData] = useState({ staff: [], shifts: [], attendance: [], leaves: [] });
  const [state, setState] = useState({ loading: true, error: '', success: '' });
  const [staffQuery, setStaffQuery] = useState('');
  const [staffStatus, setStaffStatus] = useState('');
  const [attendanceFilter, setAttendanceFilter] = useState('all');
  const [leaveFilter, setLeaveFilter] = useState('all');
  const [shiftOpen, setShiftOpen] = useState(false);
  const [shiftForm, setShiftForm] = useState(initialShiftForm);
  const [shiftError, setShiftError] = useState('');
  const [actionError, setActionError] = useState('');
  const [cancellation, setCancellation] = useState(null);
  const [review, setReview] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const weekDays = useMemo(() => Array.from({ length: 7 }, (_, index) => addDays(weekStart, index)), [weekStart]);
  const range = useMemo(() => ({ from: dayKey(weekDays[0]), to: dayKey(weekDays[6]) }), [weekDays]);
  const attendanceByShift = useMemo(
    () => new Map(data.attendance.map(item => [item.shiftId, item])),
    [data.attendance]
  );
  const shiftPlan = useMemo(() => buildShiftPlan(shiftForm), [shiftForm]);

  const load = useCallback(async () => {
    if (!selectedCinemaId) return;
    setState(current => ({ ...current, loading: true, error: '', success: '' }));
    try {
      const params = { cinemaPublicId: selectedCinemaId, ...range };
      const [staff, shifts, attendance, leaves] = await Promise.all([
        managerCinemaService.getStaff(selectedCinemaId),
        managerCinemaService.getShifts(params),
        managerCinemaService.getAttendance(params),
        managerCinemaService.getLeaveRequests(params),
      ]);
      setData({ staff, shifts, attendance, leaves });
      setState(current => ({ ...current, loading: false, error: '' }));
    } catch (error) {
      setState({
        loading: false,
        error: error?.message || 'Không thể tải dữ liệu nhân sự tại rạp.',
        success: '',
      });
    }
  }, [range, selectedCinemaId]);

  useEffect(() => {
    // Loading remote workforce data is the synchronization performed by this effect.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const activeShifts = data.shifts.filter(item => item.status !== 'CANCELLED');
  const pendingLeaves = data.leaves.filter(item => item.status === 'PENDING').length;
  const attendanceIssues = data.attendance.filter(item => ATTENDANCE_ISSUES.has(item.status)).length;
  const scheduledStaffIds = new Set(activeShifts.map(item => item.employeeId));
  const unscheduledStaff = data.staff.filter(item => item.status === 'ACTIVE' && !scheduledStaffIds.has(item.accountId));
  const activeStaff = data.staff.filter(item => item.status === 'ACTIVE').length;

  const visibleStaff = data.staff.filter(employee => {
    const keyword = staffQuery.trim().toLocaleLowerCase('vi-VN');
    const matchesKeyword = !keyword || [employee.fullName, employee.employeeCode, employee.email, employee.positionName]
      .some(value => String(value || '').toLocaleLowerCase('vi-VN').includes(keyword));
    return matchesKeyword && (!staffStatus || employee.status === staffStatus);
  });

  const visibleAttendance = data.attendance.filter(item =>
    attendanceFilter === 'issues' ? ATTENDANCE_ISSUES.has(item.status) : true
  );

  const visibleLeaves = data.leaves.filter(item =>
    leaveFilter === 'all' ? true : item.status === leaveFilter
  );

  const moveWeek = amount => setWeekStart(current => addDays(current, amount * 7));

  const openShiftForm = (employeeId = '', workDate = dayKey(new Date())) => {
    setShiftForm(initialShiftForm(workDate, employeeId));
    setShiftError('');
    setShiftOpen(true);
  };

  const createShift = async event => {
    event.preventDefault();
    if (!shiftForm.employeeId) {
      setShiftError('Vui lòng chọn nhân viên cần xếp ca.');
      return;
    }
    if (shiftPlan.minutes <= 0 || shiftPlan.minutes > 16 * 60) {
      setShiftError('Thời lượng ca phải lớn hơn 0 và không vượt quá 16 giờ.');
      return;
    }
    setSubmitting(true);
    setShiftError('');
    try {
      await managerCinemaService.createShift(selectedCinemaId, {
        employeeId: Number(shiftForm.employeeId),
        scheduledStart: shiftPlan.scheduledStart,
        scheduledEnd: shiftPlan.scheduledEnd,
        location: selectedCinema.name,
        note: shiftForm.note.trim() || null,
      });
      setShiftOpen(false);
      await load();
      setState({ loading: false, error: '', success: 'Đã xếp ca và cập nhật vào lịch tuần của nhân viên.' });
      setTab('shifts');
    } catch (error) {
      setShiftError(error?.message || 'Không thể xếp ca làm.');
    } finally {
      setSubmitting(false);
    }
  };

  const submitCancellation = async event => {
    event.preventDefault();
    if (!cancellation?.reason?.trim() || cancellation.reason.trim().length < 5) {
      setActionError('Lý do hủy ca cần có ít nhất 5 ký tự.');
      return;
    }
    setSubmitting(true);
    setActionError('');
    try {
      await managerCinemaService.cancelShift(selectedCinemaId, cancellation.shift.id, {
        reason: cancellation.reason.trim(),
        expectedVersion: cancellation.shift.version,
      });
      setCancellation(null);
      await load();
      setState({ loading: false, error: '', success: 'Đã hủy ca làm và lưu lý do.' });
    } catch (error) {
      setActionError(error?.message || 'Không thể hủy ca.');
    } finally {
      setSubmitting(false);
    }
  };

  const submitReview = async event => {
    event.preventDefault();
    if (review?.type === 'REJECT' && review.note.trim().length < 5) {
      setActionError('Vui lòng nhập lý do từ chối có ít nhất 5 ký tự.');
      return;
    }
    setSubmitting(true);
    setActionError('');
    try {
      await managerCinemaService.reviewLeave(selectedCinemaId, review.leave.id, {
        type: review.type,
        note: review.note.trim() || 'Quản lý rạp đã duyệt',
        expectedVersion: review.leave.version,
      });
      setReview(null);
      await load();
      setState({
        loading: false,
        error: '',
        success: review.type === 'APPROVE' ? 'Đã duyệt đơn nghỉ.' : 'Đã từ chối đơn nghỉ.',
      });
    } catch (error) {
      setActionError(error?.message || 'Không thể xử lý đơn nghỉ.');
    } finally {
      setSubmitting(false);
    }
  };

  if (cinemaState.loading) {
    return <p className="py-24 text-center text-sm font-bold text-zinc-500">Đang tải phạm vi rạp…</p>;
  }

  if (!selectedCinema) {
    return (
      <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 p-8 text-center text-amber-200">
        Chưa có rạp được phân công.
      </div>
    );
  }

  const employeeOptions = data.staff
    .filter(item => item.status === 'ACTIVE')
    .map(item => ({
      value: String(item.accountId),
      label: item.fullName,
      subtitle: `${item.employeeCode} · ${item.positionName || 'Chưa có vị trí'}`,
    }));

  const selectedEmployee = data.staff.find(item => String(item.accountId) === shiftForm.employeeId);
  const metrics = [
    {
      label: 'Nhân sự đang làm việc',
      value: activeStaff,
      hint: `${data.staff.length} hồ sơ thuộc rạp`,
      icon: UsersRound,
      tone: 'blue',
    },
    {
      label: 'Ca trong tuần',
      value: activeShifts.length,
      hint: `${formatDate(range.from)} – ${formatDate(range.to)}`,
      icon: CalendarClock,
      tone: 'orange',
    },
    {
      label: 'Đã chấm công',
      value: data.attendance.length,
      hint: attendanceIssues ? `${attendanceIssues} trường hợp cần chú ý` : 'Không có bất thường',
      icon: CheckCircle2,
      tone: attendanceIssues ? 'amber' : 'green',
    },
    {
      label: 'Đơn nghỉ chờ duyệt',
      value: pendingLeaves,
      hint: pendingLeaves ? 'Cần xử lý trong ca trực' : 'Không có việc tồn',
      icon: Umbrella,
      tone: pendingLeaves ? 'amber' : 'purple',
    },
  ];

  return (
    <main className="min-h-full space-y-5 text-white">
      <HrHero
        context={`Điều phối đội ngũ · ${selectedCinema.name}`}
        title="Lịch ca & nhân sự"
        description="Theo dõi lịch tuần, chấm công và đơn nghỉ trong đúng phạm vi rạp được phân công. Mọi thao tác đều bắt đầu từ nhân viên và ngày làm việc cụ thể."
        actions={(
          <button
            type="button"
            onClick={() => openShiftForm()}
            className="inline-flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-2.5 text-sm font-black text-black hover:bg-orange-400"
          >
            <CalendarPlus size={18} /> Xếp ca mới
          </button>
        )}
      />

      <MetricStrip items={metrics} />

      <section className="grid gap-3 lg:grid-cols-3">
        <button
          type="button"
          onClick={() => setTab('shifts')}
          className={`rounded-2xl border p-4 text-left transition hover:-translate-y-0.5 ${unscheduledStaff.length ? 'border-amber-500/25 bg-amber-500/[0.06]' : 'border-emerald-500/20 bg-emerald-500/[0.04]'}`}
        >
          <div className="flex items-start justify-between gap-3">
            <div><p className="text-xs font-black uppercase tracking-wider text-zinc-500">Độ phủ lịch tuần</p><p className="mt-2 text-sm font-black text-zinc-100">{unscheduledStaff.length ? `${unscheduledStaff.length} nhân viên chưa có ca` : 'Tất cả nhân viên đã có lịch'}</p></div>
            <CalendarDays className={unscheduledStaff.length ? 'text-amber-300' : 'text-emerald-300'} size={20} />
          </div>
          <p className="mt-2 text-xs leading-5 text-zinc-500">Mở lịch tuần để xếp ca vào đúng ngày còn trống.</p>
        </button>
        <button
          type="button"
          onClick={() => setTab('attendance')}
          className={`rounded-2xl border p-4 text-left transition hover:-translate-y-0.5 ${attendanceIssues ? 'border-red-500/20 bg-red-500/[0.05]' : 'border-white/10 bg-white/[0.025]'}`}
        >
          <div className="flex items-start justify-between gap-3">
            <div><p className="text-xs font-black uppercase tracking-wider text-zinc-500">Chấm công bất thường</p><p className="mt-2 text-sm font-black text-zinc-100">{attendanceIssues ? `${attendanceIssues} trường hợp cần kiểm tra` : 'Chưa phát hiện bất thường'}</p></div>
            <Clock3 className={attendanceIssues ? 'text-red-300' : 'text-zinc-500'} size={20} />
          </div>
          <p className="mt-2 text-xs leading-5 text-zinc-500">Ưu tiên trường hợp đi muộn hoặc vắng mặt.</p>
        </button>
        <button
          type="button"
          onClick={() => setTab('leaves')}
          className={`rounded-2xl border p-4 text-left transition hover:-translate-y-0.5 ${pendingLeaves ? 'border-orange-500/25 bg-orange-500/[0.06]' : 'border-white/10 bg-white/[0.025]'}`}
        >
          <div className="flex items-start justify-between gap-3">
            <div><p className="text-xs font-black uppercase tracking-wider text-zinc-500">Việc chờ phê duyệt</p><p className="mt-2 text-sm font-black text-zinc-100">{pendingLeaves ? `${pendingLeaves} đơn nghỉ đang chờ` : 'Không có đơn nghỉ tồn'}</p></div>
            <Umbrella className={pendingLeaves ? 'text-orange-300' : 'text-zinc-500'} size={20} />
          </div>
          <p className="mt-2 text-xs leading-5 text-zinc-500">Duyệt hoặc từ chối kèm ghi chú rõ ràng.</p>
        </button>
      </section>

      <section className="flex flex-col gap-3 rounded-2xl border border-white/10 bg-white/[0.025] p-3 xl:flex-row xl:items-center xl:justify-between">
        <div className="flex flex-wrap gap-2">
          {[
            ['shifts', 'Lịch ca tuần'],
            ['attendance', 'Chấm công'],
            ['staff', 'Hồ sơ tại rạp'],
            ['leaves', `Đơn nghỉ${pendingLeaves ? ` (${pendingLeaves})` : ''}`],
          ].map(([value, label]) => (
            <button
              key={value}
              type="button"
              onClick={() => setTab(value)}
              className={`rounded-xl px-4 py-2.5 text-sm font-black transition ${tab === value ? 'bg-white text-black' : 'text-zinc-400 hover:bg-white/5 hover:text-white'}`}
            >
              {label}
            </button>
          ))}
        </div>
        {tab !== 'staff' ? (
          <div className="flex flex-wrap items-center gap-2">
            <button type="button" aria-label="Tuần trước" onClick={() => moveWeek(-1)} className="rounded-xl border border-white/10 p-2.5 text-zinc-400 hover:bg-white/5 hover:text-white"><ChevronLeft size={18} /></button>
            <label className="flex items-center gap-2 rounded-xl border border-white/10 bg-black/30 px-3 py-2 text-xs font-black text-zinc-500">
              Tuần bắt đầu
              <input
                aria-label="Chọn tuần làm việc"
                type="date"
                value={range.from}
                onChange={event => setWeekStart(startOfWeek(`${event.target.value}T00:00:00`))}
                className="bg-transparent text-sm font-bold text-white outline-none"
              />
            </label>
            <button type="button" aria-label="Tuần sau" onClick={() => moveWeek(1)} className="rounded-xl border border-white/10 p-2.5 text-zinc-400 hover:bg-white/5 hover:text-white"><ChevronRight size={18} /></button>
            {range.from !== dayKey(startOfWeek()) ? <button type="button" onClick={() => setWeekStart(startOfWeek())} className="rounded-xl px-3 py-2.5 text-xs font-black text-orange-300 hover:bg-orange-500/10">Về tuần này</button> : null}
          </div>
        ) : null}
      </section>

      {state.error ? <p role="alert" className="rounded-xl border border-red-500/20 bg-red-500/10 p-4 text-sm text-red-200">{state.error}</p> : null}
      {state.success ? <p className="rounded-xl border border-emerald-500/20 bg-emerald-500/10 p-4 text-sm text-emerald-200">{state.success}</p> : null}

      {state.loading ? (
        <ConsolePanel><p className="p-16 text-center text-sm font-bold text-zinc-500">Đang cập nhật lịch và dữ liệu nhân sự…</p></ConsolePanel>
      ) : tab === 'shifts' ? (
        data.staff.length ? (
          <ConsolePanel className="overflow-hidden">
            <div className="overflow-x-auto">
              <div className="min-w-[1180px]">
                <div className="grid grid-cols-[220px_repeat(7,minmax(135px,1fr))] border-b border-white/10 bg-white/[0.025]">
                  <div className="p-4 text-[10px] font-black uppercase tracking-[0.16em] text-zinc-600">Nhân viên tại rạp</div>
                  {weekDays.map((day, index) => {
                    const today = dayKey(day) === dayKey(new Date());
                    return (
                      <div key={dayKey(day)} className={`border-l border-white/5 p-3 text-center ${today ? 'bg-orange-500/[0.07]' : ''}`}>
                        <p className="text-[10px] font-black uppercase text-zinc-600">{DAY_NAMES[index]}</p>
                        <p className={`mt-1 text-sm font-black ${today ? 'text-orange-300' : 'text-zinc-300'}`}>{day.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })}</p>
                      </div>
                    );
                  })}
                </div>
                {data.staff.map(employee => (
                  <div key={employee.accountId} className="grid min-h-32 grid-cols-[220px_repeat(7,minmax(135px,1fr))] border-b border-white/5 last:border-b-0">
                    <div className="flex items-start gap-3 p-4">
                      <PersonAvatar name={employee.fullName} avatarUrl={employee.avatarUrl} role={getEmployeeAvatarRole(employee)} />
                      <div className="min-w-0">
                        <p className="truncate text-sm font-black text-zinc-100">{employee.fullName}</p>
                        <p className="mt-1 truncate text-[10px] text-zinc-600">{employee.employeeCode}</p>
                        <p className="mt-2 line-clamp-2 text-[11px] leading-4 text-zinc-500">{employee.positionName || 'Chưa có vị trí'}</p>
                      </div>
                    </div>
                    {weekDays.map(day => {
                      const cellDate = dayKey(day);
                      const dayShifts = data.shifts.filter(item => item.employeeId === employee.accountId && dayKey(item.scheduledStart) === cellDate);
                      return (
                        <div key={cellDate} className={`border-l border-white/5 p-2 ${cellDate === dayKey(new Date()) ? 'bg-orange-500/[0.025]' : ''}`}>
                          {dayShifts.map(shift => {
                            const attendance = attendanceByShift.get(shift.id);
                            const cancelled = shift.status === 'CANCELLED';
                            return (
                              <article key={shift.id} className={`mb-2 rounded-xl border p-2.5 ${cancelled ? 'border-red-500/15 bg-red-500/[0.04] opacity-60' : 'border-sky-500/20 bg-sky-500/[0.08]'}`}>
                                <div className="flex items-start justify-between gap-1">
                                  <div>
                                    <p className={`text-xs font-black ${cancelled ? 'text-red-300 line-through' : 'text-sky-200'}`}>{formatTime(shift.scheduledStart)} – {formatTime(shift.scheduledEnd)}</p>
                                    <p className="mt-1 line-clamp-2 text-[10px] leading-4 text-zinc-500">{shift.note || shift.location}</p>
                                  </div>
                                  {!cancelled && shift.status === 'SCHEDULED' ? (
                                    <button
                                      type="button"
                                      aria-label={`Hủy ca của ${employee.fullName} lúc ${formatTime(shift.scheduledStart)}`}
                                      onClick={() => { setActionError(''); setCancellation({ shift, reason: '' }); }}
                                      className="grid h-6 w-6 shrink-0 place-items-center rounded-md text-zinc-600 hover:bg-red-500/10 hover:text-red-300"
                                    ><X size={13} /></button>
                                  ) : null}
                                </div>
                                {attendance ? <p className={`mt-2 text-[10px] font-black ${ATTENDANCE_ISSUES.has(attendance.status) ? 'text-amber-300' : 'text-emerald-300'}`}>{STATUS[attendance.status] || attendance.status}</p> : null}
                              </article>
                            );
                          })}
                          {employee.status === 'ACTIVE' ? (
                            <button
                              type="button"
                              aria-label={`Xếp ca cho ${employee.fullName} ngày ${formatDate(cellDate)}`}
                              onClick={() => openShiftForm(employee.accountId, cellDate)}
                              className={`grid w-full place-items-center rounded-xl border border-dashed text-[10px] font-bold transition ${dayShifts.length ? 'h-8 border-white/10 text-zinc-600 hover:border-orange-500/30 hover:text-orange-300' : 'min-h-20 border-transparent text-zinc-700 hover:border-white/10 hover:text-zinc-500'}`}
                            >{dayShifts.length ? '+ Thêm ca' : '+ Xếp ca'}</button>
                          ) : null}
                        </div>
                      );
                    })}
                  </div>
                ))}
              </div>
            </div>
          </ConsolePanel>
        ) : (
          <EmptyWorkspace title="Rạp chưa có nhân sự" description="Admin cần phân công nhân viên vào rạp trước khi quản lý có thể xếp ca." />
        )
      ) : tab === 'staff' ? (
        <>
          <section className="grid gap-3 rounded-2xl border border-white/10 bg-white/[0.025] p-3 md:grid-cols-[minmax(260px,1fr)_220px]">
            <label className="relative">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-600" size={18} />
              <input value={staffQuery} onChange={event => setStaffQuery(event.target.value)} aria-label="Tìm nhân viên tại rạp" placeholder="Tìm theo tên, mã, email hoặc vị trí" className="h-11 w-full rounded-xl border border-white/10 bg-black/30 pl-11 pr-4 text-sm outline-none focus:border-orange-500" />
            </label>
            <select value={staffStatus} onChange={event => setStaffStatus(event.target.value)} aria-label="Lọc trạng thái nhân viên" className="h-11 rounded-xl border border-white/10 bg-black/30 px-4 text-sm outline-none focus:border-orange-500">
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang làm việc</option>
              <option value="ON_LEAVE">Đang nghỉ</option>
              <option value="SUSPENDED">Tạm ngưng</option>
            </select>
          </section>
          {visibleStaff.length ? (
            <div className="grid gap-3 lg:grid-cols-2 2xl:grid-cols-3">
              {visibleStaff.map(employee => {
                const employeeShifts = activeShifts.filter(item => item.employeeId === employee.accountId).length;
                return (
                  <article key={employee.accountId} className="rounded-2xl border border-white/10 bg-[#0b0b0e] p-5">
                    <div className="flex items-start gap-3">
                      <PersonAvatar name={employee.fullName} avatarUrl={employee.avatarUrl} role={getEmployeeAvatarRole(employee)} size="lg" />
                      <div className="min-w-0 flex-1">
                        <div className="flex items-start justify-between gap-2"><div className="min-w-0"><p className="truncate font-black text-zinc-100">{employee.fullName}</p><p className="mt-1 font-mono text-[10px] text-zinc-600">{employee.employeeCode}</p></div><StatusBadge status={employee.status} label={STATUS[employee.status]} /></div>
                      </div>
                    </div>
                    <div className="mt-4 grid grid-cols-2 gap-3 border-t border-white/5 pt-4 text-xs">
                      <div><p className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Vị trí</p><p className="mt-1 font-bold text-zinc-300">{employee.positionName || 'Chưa phân bổ'}</p></div>
                      <div><p className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Ca trong tuần</p><p className="mt-1 font-bold text-zinc-300">{employeeShifts} ca</p></div>
                    </div>
                    <p className="mt-4 flex items-center gap-2 truncate text-xs text-zinc-500"><Mail size={14} /> {employee.email || 'Chưa có email'}</p>
                    {employee.status === 'ACTIVE' ? <button type="button" onClick={() => openShiftForm(employee.accountId)} className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-xl border border-orange-500/20 bg-orange-500/[0.06] px-4 py-2.5 text-xs font-black text-orange-300 hover:bg-orange-500/10"><CalendarPlus size={15} /> Xếp ca cho nhân viên</button> : null}
                  </article>
                );
              })}
            </div>
          ) : <EmptyWorkspace title="Không tìm thấy nhân viên" description="Thử đổi từ khóa hoặc trạng thái đang lọc." />}
        </>
      ) : tab === 'attendance' ? (
        <ConsolePanel className="overflow-hidden">
          <div className="flex flex-col gap-3 border-b border-white/10 p-4 sm:flex-row sm:items-center sm:justify-between">
            <div><p className="font-black text-zinc-100">Chấm công trong tuần</p><p className="mt-1 text-xs text-zinc-500">Đối chiếu giờ dự kiến với giờ vào/ra thực tế.</p></div>
            <div className="flex gap-2"><button type="button" onClick={() => setAttendanceFilter('all')} className={`rounded-lg px-3 py-2 text-xs font-black ${attendanceFilter === 'all' ? 'bg-white text-black' : 'bg-white/5 text-zinc-400'}`}>Tất cả ({data.attendance.length})</button><button type="button" onClick={() => setAttendanceFilter('issues')} className={`rounded-lg px-3 py-2 text-xs font-black ${attendanceFilter === 'issues' ? 'bg-amber-400 text-black' : 'bg-amber-500/10 text-amber-300'}`}>Cần chú ý ({attendanceIssues})</button></div>
          </div>
          {visibleAttendance.length ? (
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-white/[0.025] text-[10px] font-black uppercase tracking-wider text-zinc-600"><tr><th className="p-4">Nhân viên</th><th className="p-4">Ca dự kiến</th><th className="p-4">Vào ca</th><th className="p-4">Ra ca</th><th className="p-4">Phút công</th><th className="p-4">Trạng thái</th></tr></thead>
                <tbody className="divide-y divide-white/5">{visibleAttendance.map(item => <tr key={item.id} className="hover:bg-white/[0.02]"><td className="p-4"><p className="font-black text-zinc-100">{item.employeeName}</p><p className="mt-1 text-xs text-zinc-600">{item.employeeCode}</p></td><td className="p-4 text-zinc-300">{formatDateTime(item.scheduledStart)}<br /><span className="text-xs text-zinc-600">đến {formatDateTime(item.scheduledEnd)}</span></td><td className="p-4 text-zinc-300">{formatDateTime(item.checkInAt)}</td><td className="p-4 text-zinc-300">{formatDateTime(item.checkOutAt)}</td><td className="p-4 font-bold text-zinc-300">{item.workedMinutes || 0}<br /><span className="text-xs font-normal text-zinc-600">+{item.overtimeMinutes || 0} tăng ca</span></td><td className="p-4"><StatusBadge status={item.status} label={STATUS[item.status]} /></td></tr>)}</tbody>
              </table>
            </div>
          ) : <EmptyWorkspace title={attendanceFilter === 'issues' ? 'Không có chấm công bất thường' : 'Chưa có dữ liệu chấm công'} description={attendanceFilter === 'issues' ? 'Tuần này chưa ghi nhận trường hợp đi muộn hoặc vắng mặt.' : 'Dữ liệu sẽ xuất hiện sau khi nhân viên chấm vào hoặc ra ca.'} />}
        </ConsolePanel>
      ) : (
        <ConsolePanel className="overflow-hidden">
          <div className="flex flex-col gap-3 border-b border-white/10 p-4 sm:flex-row sm:items-center sm:justify-between">
            <div><p className="font-black text-zinc-100">Đơn nghỉ trong tuần</p><p className="mt-1 text-xs text-zinc-500">Xử lý rõ lý do và trạng thái của từng yêu cầu.</p></div>
            <select value={leaveFilter} onChange={event => setLeaveFilter(event.target.value)} aria-label="Lọc trạng thái đơn nghỉ" className="h-10 rounded-xl border border-white/10 bg-black/30 px-3 text-xs font-bold text-white outline-none"><option value="all">Tất cả đơn</option><option value="PENDING">Chờ duyệt</option><option value="APPROVED">Đã duyệt</option><option value="REJECTED">Đã từ chối</option></select>
          </div>
          {visibleLeaves.length ? (
            <div className="divide-y divide-white/5">
              {visibleLeaves.map(leave => (
                <article key={leave.id} className="grid gap-4 p-5 lg:grid-cols-[1fr_1fr_1.4fr_auto] lg:items-center">
                  <div><p className="font-black text-zinc-100">{leave.employeeName}</p><p className="mt-1 text-xs text-zinc-600">{leave.employeeCode} · {LEAVE_TYPES[leave.leaveType] || leave.leaveType}</p></div>
                  <p className="text-sm text-zinc-300">{formatDate(leave.startDate)} → {formatDate(leave.endDate)}<br /><span className="text-xs text-zinc-600">{leave.paid ? 'Nghỉ có lương' : 'Nghỉ không lương'}</span></p>
                  <p className="text-sm leading-6 text-zinc-400">{leave.reason}</p>
                  {leave.status === 'PENDING' ? (
                    <div className="flex gap-2"><button type="button" aria-label={`Duyệt đơn nghỉ của ${leave.employeeName}`} onClick={() => { setActionError(''); setReview({ leave, type: 'APPROVE', note: '' }); }} className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-500/10 px-3 py-2 text-xs font-black text-emerald-300"><Check size={15} /> Duyệt</button><button type="button" aria-label={`Từ chối đơn nghỉ của ${leave.employeeName}`} onClick={() => { setActionError(''); setReview({ leave, type: 'REJECT', note: '' }); }} className="inline-flex items-center gap-1.5 rounded-lg bg-red-500/10 px-3 py-2 text-xs font-black text-red-300"><X size={15} /> Từ chối</button></div>
                  ) : <StatusBadge status={leave.status} label={STATUS[leave.status]} />}
                </article>
              ))}
            </div>
          ) : <EmptyWorkspace title="Không có đơn nghỉ phù hợp" description="Không có yêu cầu nào trong tuần và trạng thái đang chọn." />}
        </ConsolePanel>
      )}

      <p className="flex items-center gap-2 text-xs text-zinc-600"><MapPin size={15} /> Chỉ hiển thị nhân viên, ca làm và đơn nghỉ thuộc {selectedCinema.name}.</p>

      <ActionModal open={shiftOpen} onClose={() => setShiftOpen(false)} title="Xếp ca làm việc" description="Chọn nhân viên, ngày và khung giờ. Ca sẽ xuất hiện ngay trên lịch tuần và lịch cá nhân của nhân viên." onSubmit={createShift} submitLabel="Lưu ca làm" submitting={submitting} wide>
        {shiftError ? <p role="alert" className="rounded-xl border border-red-500/20 bg-red-500/10 p-3 text-xs font-bold text-red-200">{shiftError}</p> : null}
        <div className="grid gap-4 sm:grid-cols-2">
          <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Nhân viên *</label><SearchableSelect value={shiftForm.employeeId} onChange={employeeId => setShiftForm(value => ({ ...value, employeeId }))} options={employeeOptions} placeholder="Chọn hoặc tìm nhân viên" ariaLabel="Chọn nhân viên cần xếp ca" /></div>
          <label className="text-xs font-black uppercase tracking-wider text-zinc-500">Ngày làm việc *<input required type="date" value={shiftForm.workDate} onChange={event => setShiftForm(value => ({ ...value, workDate: event.target.value }))} className="mt-2 h-11 w-full rounded-xl border border-white/10 bg-zinc-950 px-3 text-sm text-white outline-none focus:border-orange-500" /></label>
        </div>
        <div className="rounded-2xl border border-white/10 bg-black/20 p-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"><div><p className="text-sm font-black text-zinc-200">Khung giờ làm việc</p><p className="mt-1 text-xs text-zinc-600">Có thể chọn ca đêm kết thúc vào ngày hôm sau.</p></div><div className="flex flex-wrap gap-2"><button type="button" onClick={() => setShiftForm(value => ({ ...value, start: '09:00', end: '17:00' }))} className="rounded-lg border border-white/10 px-3 py-2 text-xs font-black text-zinc-400 hover:bg-white/5 hover:text-white">Ca ngày 09–17</button><button type="button" onClick={() => setShiftForm(value => ({ ...value, start: '14:00', end: '22:00' }))} className="rounded-lg border border-orange-500/20 bg-orange-500/10 px-3 py-2 text-xs font-black text-orange-300">Ca chiều 14–22</button></div></div>
          <div className="mt-4 grid gap-3 sm:grid-cols-2"><label className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Bắt đầu *<input required type="time" value={shiftForm.start} onChange={event => setShiftForm(value => ({ ...value, start: event.target.value }))} className="mt-2 h-11 w-full rounded-xl border border-white/10 bg-zinc-950 px-3 text-sm text-white outline-none focus:border-orange-500" /></label><label className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Kết thúc *<input required type="time" value={shiftForm.end} onChange={event => setShiftForm(value => ({ ...value, end: event.target.value }))} className="mt-2 h-11 w-full rounded-xl border border-white/10 bg-zinc-950 px-3 text-sm text-white outline-none focus:border-orange-500" /></label></div>
          <div className="mt-4 flex flex-col gap-2 rounded-xl border border-white/5 bg-white/[0.025] p-3 text-xs text-zinc-500 sm:flex-row sm:items-center sm:justify-between"><span className="flex items-center gap-2"><Clock3 size={14} /> Tổng thời lượng <strong className="text-zinc-200">{durationLabel(shiftPlan.minutes)}</strong></span>{shiftPlan.overnight ? <span className="font-bold text-blue-300">Kết thúc vào ngày hôm sau</span> : null}</div>
        </div>
        <label className="block text-xs font-black uppercase tracking-wider text-zinc-500">Ghi chú nội bộ<textarea value={shiftForm.note} maxLength={500} onChange={event => setShiftForm(value => ({ ...value, note: event.target.value }))} placeholder="Ví dụ: Quầy vé ca sáng, bàn giao trước 15 phút…" className="mt-2 min-h-24 w-full rounded-xl border border-white/10 bg-black/40 p-3 text-sm normal-case text-white outline-none focus:border-orange-500" /></label>
        {selectedEmployee ? <div className="flex items-center gap-3 rounded-xl border border-sky-500/15 bg-sky-500/[0.05] p-3"><PersonAvatar name={selectedEmployee.fullName} avatarUrl={selectedEmployee.avatarUrl} role={getEmployeeAvatarRole(selectedEmployee)} /><div><p className="text-xs font-black text-sky-100">{selectedEmployee.fullName}</p><p className="mt-1 text-[11px] text-sky-200/60">{selectedEmployee.positionName} · {selectedCinema.name}</p></div></div> : null}
      </ActionModal>

      <ActionModal open={Boolean(cancellation)} onClose={() => { setCancellation(null); setActionError(''); }} title="Hủy ca làm" description={cancellation ? `${cancellation.shift.employeeName} · ${formatDateTime(cancellation.shift.scheduledStart)}` : ''} onSubmit={submitCancellation} submitLabel="Xác nhận hủy ca" submitting={submitting} tone="danger">
        {actionError ? <p role="alert" className="rounded-xl border border-red-500/20 bg-red-500/10 p-3 text-xs font-bold text-red-200">{actionError}</p> : null}
        <label className="block text-xs font-black uppercase tracking-wider text-zinc-500">Lý do hủy *<textarea required minLength={5} value={cancellation?.reason || ''} onChange={event => setCancellation(value => ({ ...value, reason: event.target.value }))} placeholder="Nhập ít nhất 5 ký tự để nhân viên hiểu thay đổi." className="mt-2 min-h-24 w-full rounded-xl border border-white/10 bg-black/40 p-3 text-sm normal-case text-white outline-none focus:border-red-500" /></label>
      </ActionModal>

      <ActionModal open={Boolean(review)} onClose={() => { setReview(null); setActionError(''); }} title={review?.type === 'APPROVE' ? 'Duyệt đơn nghỉ' : 'Từ chối đơn nghỉ'} description={review ? `${review.leave.employeeName} · ${formatDate(review.leave.startDate)} → ${formatDate(review.leave.endDate)}` : ''} onSubmit={submitReview} submitLabel={review?.type === 'APPROVE' ? 'Xác nhận duyệt' : 'Xác nhận từ chối'} submitting={submitting} tone={review?.type === 'REJECT' ? 'danger' : 'orange'}>
        {actionError ? <p role="alert" className="rounded-xl border border-red-500/20 bg-red-500/10 p-3 text-xs font-bold text-red-200">{actionError}</p> : null}
        {review ? <div className="rounded-xl border border-white/10 bg-white/[0.025] p-4"><p className="text-xs font-black uppercase tracking-wider text-zinc-600">Lý do xin nghỉ</p><p className="mt-2 text-sm leading-6 text-zinc-300">{review.leave.reason}</p></div> : null}
        <label className="block text-xs font-black uppercase tracking-wider text-zinc-500">Ghi chú {review?.type === 'REJECT' ? '*' : ''}<textarea required={review?.type === 'REJECT'} minLength={review?.type === 'REJECT' ? 5 : undefined} value={review?.note || ''} onChange={event => setReview(value => ({ ...value, note: event.target.value }))} placeholder={review?.type === 'REJECT' ? 'Nêu rõ lý do từ chối…' : 'Ghi chú cho nhân viên (không bắt buộc)…'} className="mt-2 min-h-24 w-full rounded-xl border border-white/10 bg-black/40 p-3 text-sm normal-case text-white outline-none focus:border-orange-500" /></label>
      </ActionModal>
    </main>
  );
}
