import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertTriangle, ArrowRight, Building2, CalendarDays, CheckCircle2,
  Clock3, CreditCard, ShieldCheck, UserRoundCheck, UsersRound
} from 'lucide-react';
import { AsyncState } from '@/components/common/ui/uiKit';
import {
  getAttendance, getDepartments, getEmployees, getLeaveRequests, getPayrolls,
  getPayrollSummary, getPiiGovernanceSummary, getPositions, getWorkShifts
} from '../services/userAdminService';
import useAdminAccess from '../hooks/useAdminAccess';
import { HrHero, PersonAvatar, UatGuide, WorkflowSteps } from '../components/HrWorkspace';

const page = { content: [], totalElements: 0 };
const dateKey = value => new Date(value).toISOString().slice(0, 10);
const currentMonth = () => new Date().toISOString().slice(0, 7);
const weekRange = () => {
  const today = new Date();
  const day = today.getDay() || 7;
  const from = new Date(today);
  from.setDate(today.getDate() - day + 1);
  const to = new Date(from);
  to.setDate(from.getDate() + 6);
  return { from: dateKey(from), to: dateKey(to) };
};
const shortTime = value => new Date(value).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value || 0);

function PriorityItem({ tone, icon: Icon, title, detail, count, href, action }) {
  const tones = {
    red: 'border-red-500/20 bg-red-500/[0.06] text-red-300',
    amber: 'border-amber-500/20 bg-amber-500/[0.06] text-amber-300',
    blue: 'border-blue-500/20 bg-blue-500/[0.06] text-blue-300',
    green: 'border-emerald-500/20 bg-emerald-500/[0.06] text-emerald-300'
  };
  return (
    <Link to={href} className={'group flex items-center gap-4 rounded-2xl border p-4 transition hover:-translate-y-0.5 ' + tones[tone]}>
      <span className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-black/20"><Icon size={20} /></span>
      <span className="min-w-0 flex-1">
        <span className="block text-sm font-black text-zinc-100">{title}</span>
        <span className="mt-1 block text-xs leading-5 text-zinc-500">{detail}</span>
      </span>
      <span className="text-right">
        <span className="block text-2xl font-black">{count}</span>
        <span className="text-[10px] font-black uppercase tracking-wider opacity-70">{action}</span>
      </span>
      <ArrowRight size={17} className="text-zinc-600 transition group-hover:translate-x-1 group-hover:text-current" />
    </Link>
  );
}

export default function AdminHrCommandCenterPage() {
  const can = useAdminAccess();
  const [data, setData] = useState({
    employees: page, shifts: page, attendance: page, leaves: page, payrolls: page,
    payrollSummary: null, departments: [], positions: [], pii: null
  });
  const [state, setState] = useState({ loading: true, error: '' });
  const range = useMemo(weekRange, []);
  const month = currentMonth();

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const [employees, shifts, attendance, leaves, payrolls, payrollSummary, departments, positions, pii] = await Promise.all([
        can('EMPLOYEE_VIEW') ? getEmployees({ page: 0, size: 200 }) : Promise.resolve(page),
        can('EMPLOYEE_VIEW') ? getWorkShifts({ ...range, page: 0, size: 200, sort: 'scheduledStart,asc' }) : Promise.resolve(page),
        can('EMPLOYEE_VIEW') ? getAttendance({ ...range, page: 0, size: 200, sort: 'checkInAt,desc' }) : Promise.resolve(page),
        can('EMPLOYEE_VIEW') ? getLeaveRequests({ ...range, page: 0, size: 200, sort: 'createdAt,desc' }) : Promise.resolve(page),
        can('PAYROLL_VIEW') ? getPayrolls({ month, page: 0, size: 200 }) : Promise.resolve(page),
        can('PAYROLL_VIEW') ? getPayrollSummary(month) : Promise.resolve(null),
        can('DEPARTMENT_VIEW') ? getDepartments() : Promise.resolve([]),
        can('POSITION_VIEW') ? getPositions() : Promise.resolve([]),
        can('SYSTEM_CONFIGURATION') ? getPiiGovernanceSummary() : Promise.resolve(null)
      ]);
      setData({ employees: employees || page, shifts: shifts || page, attendance: attendance || page, leaves: leaves || page, payrolls: payrolls || page, payrollSummary, departments: departments || [], positions: positions || [], pii });
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải trung tâm nhân sự.' });
    }
  }, [can, month, range]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const activeEmployees = data.employees.content.filter(item => item.status === 'ACTIVE');
  const pendingLeaves = data.leaves.content.filter(item => item.status === 'PENDING');
  const pendingPayrolls = data.payrolls.content.filter(item => item.status === 'PENDING_APPROVAL');
  const attendanceAlerts = data.attendance.content.filter(item => ['LATE', 'ABSENT'].includes(item.status) || !item.checkOutAt);
  const today = dateKey(new Date());
  const todayShifts = data.shifts.content.filter(item => dateKey(item.scheduledStart) === today);
  const employeesWithShift = new Set(data.shifts.content.map(item => item.employeeId));
  const missingSchedule = activeEmployees.filter(item => !employeesWithShift.has(item.accountId)).length;
  const uncoveredPositions = data.positions.filter(item => !item.activeEmployeeCount).length;
  const payroll = data.payrollSummary || {};

  const payrollSteps = [
    { label: 'Chốt công', hint: data.attendance.totalElements + ' bản ghi', state: data.attendance.totalElements ? 'done' : 'active' },
    { label: 'Lập phiếu', hint: (payroll.totalRecords || 0) + ' phiếu', state: payroll.totalRecords ? 'done' : 'waiting' },
    { label: 'Duyệt độc lập', hint: (payroll.pendingApproval || 0) + ' chờ duyệt', state: payroll.pendingApproval ? 'active' : payroll.approved || payroll.paid ? 'done' : 'waiting' },
    { label: 'Gửi ngân hàng', hint: (payroll.paymentPending || 0) + ' chờ đối soát', state: payroll.paymentPending ? 'active' : payroll.paid ? 'done' : 'waiting' },
    { label: 'Hoàn tất', hint: (payroll.paid || 0) + ' đã trả', state: payroll.paid ? 'done' : 'waiting' }
  ];

  return (
    <section className="min-h-full space-y-5 text-white">
      <HrHero
        context="Bàn làm việc hôm nay"
        title="Trung tâm nhân sự"
        description="Một nơi để biết hôm nay cần xử lý gì, nhân viên nào đang làm việc và kỳ lương đang ở bước nào. Các con số có thể bấm để đi thẳng tới màn hình xử lý."
        actions={<><UatGuide /><Link to="/admin/staff" className="rounded-xl bg-orange-500 px-4 py-2.5 text-sm font-black text-black hover:bg-orange-400">Mở hồ sơ nhân viên</Link></>}
      />

      <AsyncState loading={state.loading} error={state.error} onRetry={load}>
        <div className="grid gap-5 2xl:grid-cols-[minmax(0,1.35fr)_minmax(360px,.65fr)]">
          <section className="rounded-[24px] border border-white/10 bg-[#0b0b0e] p-5 md:p-6">
            <div className="mb-5 flex items-end justify-between gap-4">
              <div>
                <p className="text-[10px] font-black uppercase tracking-[0.2em] text-orange-400">Ưu tiên xử lý</p>
                <h2 className="mt-2 text-xl font-black">Việc đang chờ admin</h2>
              </div>
              <span className="rounded-full bg-white/5 px-3 py-1 text-xs font-bold text-zinc-500">Cập nhật theo dữ liệu thật</span>
            </div>
            <div className="grid gap-3">
              <PriorityItem tone={pendingLeaves.length ? 'amber' : 'green'} icon={Clock3} title="Đơn nghỉ phép chờ duyệt" detail="Cần một người khác người gửi đơn xem xét và ghi lý do." count={pendingLeaves.length} action={pendingLeaves.length ? 'Duyệt ngay' : 'Đã sạch'} href="/admin/approvals" />
              <PriorityItem tone={pendingPayrolls.length ? 'red' : 'green'} icon={CreditCard} title="Phiếu lương chờ kiểm soát" detail="Không được tự duyệt phiếu do chính mình lập." count={pendingPayrolls.length} action={pendingPayrolls.length ? 'Kiểm tra' : 'Đã sạch'} href="/admin/approvals?type=payroll" />
              <PriorityItem tone={attendanceAlerts.length ? 'amber' : 'green'} icon={AlertTriangle} title="Chấm công cần rà soát" detail="Đi muộn, vắng mặt hoặc đã vào ca nhưng chưa ghi nhận ra ca." count={attendanceAlerts.length} action={attendanceAlerts.length ? 'Rà soát' : 'Bình thường'} href="/admin/workforce?view=attendance" />
              <PriorityItem tone={missingSchedule ? 'blue' : 'green'} icon={CalendarDays} title="Nhân viên chưa có ca tuần này" detail="Danh sách đang làm việc nhưng chưa được bố trí ca trong tuần hiện tại." count={missingSchedule} action={missingSchedule ? 'Phân ca' : 'Đủ lịch'} href="/admin/workforce" />
            </div>
          </section>

          <section className="rounded-[24px] border border-white/10 bg-[#0b0b0e] p-5 md:p-6">
            <p className="text-[10px] font-black uppercase tracking-[0.2em] text-blue-300">Trạng thái vận hành</p>
            <h2 className="mt-2 text-xl font-black">Hệ thống đã sẵn sàng tới đâu?</h2>
            <div className="mt-5 space-y-4">
              {[
                { label: 'Hồ sơ nhân viên đang hoạt động', value: activeEmployees.length, total: data.employees.totalElements, icon: UserRoundCheck },
                { label: 'Phòng ban đã có nhân sự', value: data.departments.filter(item => item.activeEmployeeCount > 0).length, total: data.departments.length, icon: Building2 },
                { label: 'Vị trí đã có người đảm nhiệm', value: data.positions.length - uncoveredPositions, total: data.positions.length, icon: UsersRound },
                { label: 'Hồ sơ nhạy cảm được bảo vệ', value: data.pii?.protectedProfiles, total: data.pii?.totalProfiles, icon: ShieldCheck }
              ].map(item => {
                const Icon = item.icon;
                const percent = item.total ? Math.round(((item.value || 0) / item.total) * 100) : 0;
                return (
                  <div key={item.label}>
                    <div className="mb-2 flex items-center justify-between gap-3 text-xs">
                      <span className="flex items-center gap-2 font-bold text-zinc-300"><Icon size={15} className="text-zinc-500" /> {item.label}</span>
                      <span className="font-black text-white">{item.value ?? 'Theo quyền'}/{item.total ?? '—'}</span>
                    </div>
                    <div className="h-1.5 overflow-hidden rounded-full bg-white/5"><div className="h-full rounded-full bg-gradient-to-r from-orange-500 to-amber-300" style={{ width: percent + '%' }} /></div>
                  </div>
                );
              })}
            </div>
            <Link to="/admin/organization" className="mt-6 flex items-center justify-between rounded-xl border border-white/10 px-4 py-3 text-sm font-black text-zinc-300 hover:bg-white/5">
              Xem sơ đồ tổ chức <ArrowRight size={17} />
            </Link>
          </section>
        </div>

        <section className="rounded-[24px] border border-white/10 bg-[#0b0b0e] p-5 md:p-6">
          <div className="mb-5 flex flex-col gap-2 md:flex-row md:items-end md:justify-between">
            <div>
              <p className="text-[10px] font-black uppercase tracking-[0.2em] text-emerald-300">Kỳ lương {month}</p>
              <h2 className="mt-2 text-xl font-black">Dòng chảy bảng lương</h2>
            </div>
            <p className="text-sm font-black text-orange-300">{money(payroll.totalNetAmount)}</p>
          </div>
          <WorkflowSteps steps={payrollSteps} />
        </section>

        <div className="grid gap-5 xl:grid-cols-[1.2fr_.8fr]">
          <section className="rounded-[24px] border border-white/10 bg-[#0b0b0e] p-5 md:p-6">
            <div className="mb-4 flex items-center justify-between">
              <div><p className="text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500">Hôm nay</p><h2 className="mt-1 text-xl font-black">Ai đang có ca?</h2></div>
              <Link to="/admin/workforce" className="text-xs font-black text-orange-300 hover:text-orange-200">Xem lịch tuần →</Link>
            </div>
            {todayShifts.length ? <div className="grid gap-2 sm:grid-cols-2">{todayShifts.slice(0, 8).map(shift => (
              <div key={shift.id} className="flex items-center gap-3 rounded-2xl border border-white/10 bg-white/[0.02] p-3">
                <PersonAvatar name={shift.employeeName} />
                <div className="min-w-0 flex-1"><p className="truncate text-sm font-black">{shift.employeeName}</p><p className="mt-1 text-xs text-zinc-500">{shortTime(shift.scheduledStart)} – {shortTime(shift.scheduledEnd)} · {shift.location || 'Chưa ghi địa điểm'}</p></div>
                <CheckCircle2 size={17} className="text-emerald-400" />
              </div>
            ))}</div> : <div className="rounded-2xl border border-dashed border-white/10 p-8 text-center text-sm text-zinc-500">Hôm nay chưa có ca nào được phân.</div>}
          </section>

          <section className="rounded-[24px] border border-white/10 bg-gradient-to-br from-orange-500/10 to-[#0b0b0e] p-5 md:p-6">
            <p className="text-[10px] font-black uppercase tracking-[0.2em] text-orange-300">Dành cho admin mới</p>
            <h2 className="mt-2 text-xl font-black">Không biết bắt đầu từ đâu?</h2>
            <p className="mt-3 text-sm leading-6 text-zinc-400">Mở hướng dẫn kiểm tra. Mỗi bước đều nói rõ cần bấm gì và kết quả đúng phải trông như thế nào.</p>
            <div className="mt-5"><UatGuide /></div>
          </section>
        </div>
      </AsyncState>
    </section>
  );
}
