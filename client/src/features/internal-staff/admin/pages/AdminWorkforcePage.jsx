import { useCallback, useEffect, useMemo, useState } from 'react';
import { CalendarClock, CheckCircle2, Plus, ShieldCheck, Umbrella } from 'lucide-react';
import { useOutletContext } from 'react-router-dom';
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
  OperationsHeader
} from '../components/OperationsConsole';

const EMPTY_PAGE = { content: [], totalElements: 0 };
const STATUS_LABELS = {
  SCHEDULED: 'Đã xếp lịch', COMPLETED: 'Hoàn tất', CANCELLED: 'Đã hủy',
  ON_TIME: 'Đúng giờ', LATE: 'Đi muộn', CORRECTED: 'Đã hiệu chỉnh', ABSENT: 'Vắng mặt',
  PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối'
};

const localDateTime = (date = new Date()) => {
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
};

const currentMonth = () => localDateTime().slice(0, 7);
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
  const [tab, setTab] = useState('shifts');
  const [month, setMonth] = useState(currentMonth);
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
    <main className="min-h-full space-y-6 bg-[#070708] p-6 text-white md:p-9">
      <OperationsHeader eyebrow="Workforce control" title="Ca làm, chấm công & nghỉ phép"
        description="Một nguồn dữ liệu có kiểm soát cho lịch làm việc, thời gian thực tế và nghỉ phép; payroll sử dụng chính snapshot này để tính lương."
        actions={can('EMPLOYEE_UPDATE') ? <button type="button" onClick={() => setShiftOpen(true)} className="flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-3 text-sm font-black text-black"><Plus size={17} /> Phân ca</button> : null} />

      <MetricStrip items={metrics} />

      <div className="flex flex-col gap-3 rounded-2xl border border-white/10 bg-white/[0.025] p-3 md:flex-row md:items-center md:justify-between">
        <div className="flex gap-2">{[
          ['shifts', 'Ca làm'], ['attendance', 'Chấm công'], ['leaves', 'Nghỉ phép']
        ].map(([key, label]) => <button key={key} type="button" onClick={() => setTab(key)} className={`rounded-xl px-4 py-2 text-sm font-black ${tab === key ? 'bg-white text-black' : 'text-zinc-400 hover:bg-white/5'}`}>{label}</button>)}</div>
        <input aria-label="Kỳ vận hành" type="month" value={month} onChange={event => setMonth(event.target.value)} className="rounded-xl border border-white/10 bg-black/30 px-4 py-2 text-sm text-white" />
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load}
        empty={tab === 'shifts' ? !shifts.content?.length : tab === 'attendance' ? !attendance.content?.length : !leaves.content?.length}
        emptyMessage={tab === 'shifts' ? 'Chưa có ca làm trong kỳ' : tab === 'attendance' ? 'Chưa có dữ liệu chấm công' : 'Chưa có yêu cầu nghỉ phép'}>
        <ConsolePanel className="overflow-hidden">
          <div className="overflow-x-auto">
            {tab === 'shifts' && <table className="min-w-full text-left text-sm"><thead className="bg-white/[0.025] text-xs uppercase text-zinc-500"><tr><th className="p-4">Nhân viên</th><th className="p-4">Bắt đầu</th><th className="p-4">Kết thúc</th><th className="p-4">Địa điểm</th><th className="p-4">Trạng thái</th><th className="p-4 text-right">Kiểm soát</th></tr></thead><tbody className="divide-y divide-white/5">{shifts.content?.map(item => <tr key={item.id}><td className="p-4"><p className="font-black">{item.employeeName}</p><p className="text-xs text-zinc-600">{item.employeeCode}</p></td><td className="p-4">{new Date(item.scheduledStart).toLocaleString('vi-VN')}</td><td className="p-4">{new Date(item.scheduledEnd).toLocaleString('vi-VN')}</td><td className="p-4 text-zinc-400">{item.location || '—'}</td><td className="p-4"><StatusBadge status={item.status} label={STATUS_LABELS[item.status]} /></td><td className="p-4 text-right">{item.status !== 'CANCELLED' && <button type="button" onClick={() => openCorrection(item)} className="rounded-lg border border-white/10 px-3 py-2 text-xs font-black text-zinc-300">Hiệu chỉnh giờ</button>}</td></tr>)}</tbody></table>}
            {tab === 'attendance' && <table className="min-w-full text-left text-sm"><thead className="bg-white/[0.025] text-xs uppercase text-zinc-500"><tr><th className="p-4">Nhân viên</th><th className="p-4">Vào ca</th><th className="p-4">Ra ca</th><th className="p-4">Phút công</th><th className="p-4">Nguồn</th><th className="p-4">Trạng thái</th></tr></thead><tbody className="divide-y divide-white/5">{attendance.content?.map(item => <tr key={item.id}><td className="p-4 font-black">{item.employeeName}</td><td className="p-4">{item.checkInAt ? new Date(item.checkInAt).toLocaleString('vi-VN') : '—'}</td><td className="p-4">{item.checkOutAt ? new Date(item.checkOutAt).toLocaleString('vi-VN') : '—'}</td><td className="p-4">{item.workedMinutes} <span className="text-zinc-600">(+{item.overtimeMinutes} OT)</span></td><td className="p-4 text-zinc-400">{item.source}</td><td className="p-4"><StatusBadge status={item.status} label={STATUS_LABELS[item.status]} /></td></tr>)}</tbody></table>}
            {tab === 'leaves' && <table className="min-w-full text-left text-sm"><thead className="bg-white/[0.025] text-xs uppercase text-zinc-500"><tr><th className="p-4">Nhân viên</th><th className="p-4">Loại nghỉ</th><th className="p-4">Khoảng ngày</th><th className="p-4">Lý do</th><th className="p-4">Trạng thái</th><th className="p-4 text-right">Duyệt</th></tr></thead><tbody className="divide-y divide-white/5">{leaves.content?.map(item => <tr key={item.id}><td className="p-4 font-black">{item.employeeName}</td><td className="p-4">{item.leaveType} · {item.paid ? 'Hưởng lương' : 'Không lương'}</td><td className="p-4">{item.startDate} → {item.endDate}</td><td className="max-w-xs p-4 text-zinc-400">{item.reason}</td><td className="p-4"><StatusBadge status={item.status} label={STATUS_LABELS[item.status]} /></td><td className="p-4 text-right">{item.status === 'PENDING' && <span className="inline-flex gap-2"><button type="button" onClick={() => setReview({ ...item, type: 'APPROVE', note: '' })} className="rounded-lg bg-emerald-500/15 px-3 py-2 text-xs font-black text-emerald-400">Duyệt</button><button type="button" onClick={() => setReview({ ...item, type: 'REJECT', note: '' })} className="rounded-lg bg-red-500/15 px-3 py-2 text-xs font-black text-red-400">Từ chối</button></span>}</td></tr>)}</tbody></table>}
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
