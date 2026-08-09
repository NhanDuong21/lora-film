import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Banknote, CalendarDays, CheckCircle2, ClipboardList, Clock3,
  LoaderCircle, MapPin, RefreshCw, Ticket, WalletCards,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getBookingHistory } from '@/features/booking/customer/services/bookingService';
import { getMyAttendance, getMyWorkShifts } from '../../admin/services/userAdminService';
import useEmployeeAccess from '../hooks/useEmployeeAccess';
import { EMPLOYEE_PERMISSIONS } from '../employeeAccess';
import { clock, money, seatCount, shiftStatus } from '../employeePresentation';
import { getMyEmployeeCinemaContext } from '../services/employeeBoxOfficeService';
import { getCurrentCounterSession } from '../services/employeeOperationsService';

const localDate = (date = new Date()) => new Date(
  date.getTime() - date.getTimezoneOffset() * 60_000,
).toISOString().slice(0, 10);

export default function EmployeeDashboardPage() {
  const navigate = useNavigate();
  const can = useEmployeeAccess();
  const canViewSchedule = can(EMPLOYEE_PERMISSIONS.SCHEDULE_VIEW);
  const canViewAttendance = can(EMPLOYEE_PERMISSIONS.ATTENDANCE_VIEW)
    && can(EMPLOYEE_PERMISSIONS.ATTENDANCE_UPDATE);
  const canOperateCounter = can(EMPLOYEE_PERMISSIONS.BOOKING_MANAGE)
    && can(EMPLOYEE_PERMISSIONS.CASH_PAYMENT_COLLECT);
  const [data, setData] = useState({
    context: null, shifts: [], attendance: [], orders: [], cashSession: null,
  });
  const [state, setState] = useState({ loading: true, warning: '' });

  const load = useCallback(async () => {
    setState({ loading: true, warning: '' });
    const today = localDate();
    const calls = [
      getMyEmployeeCinemaContext(),
      canViewSchedule
        ? getMyWorkShifts({ from: today, to: today, page: 0, size: 20, sort: 'scheduledStart,asc' })
        : Promise.resolve({ content: [] }),
      canViewAttendance
        ? getMyAttendance({ from: today, to: today, page: 0, size: 20 })
        : Promise.resolve({ content: [] }),
      canOperateCounter
        ? getBookingHistory({ fromDate: today, toDate: today, page: 0, size: 100 })
        : Promise.resolve({ content: [] }),
      canOperateCounter ? getCurrentCounterSession() : Promise.resolve(null),
    ];
    const [context, shifts, attendance, orders, cashSession] = await Promise.allSettled(calls);
    setData({
      context: context.status === 'fulfilled' ? context.value : null,
      shifts: shifts.status === 'fulfilled' ? shifts.value?.content || [] : [],
      attendance: attendance.status === 'fulfilled' ? attendance.value?.content || [] : [],
      orders: orders.status === 'fulfilled' ? orders.value?.content || [] : [],
      cashSession: cashSession.status === 'fulfilled' ? cashSession.value : null,
    });
    const failed = [context, shifts, attendance, orders, cashSession]
      .filter(result => result.status === 'rejected').length;
    setState({
      loading: false,
      warning: failed ? 'Một vài số liệu chưa tải được. Bạn vẫn có thể tiếp tục các công việc đang hiển thị.' : '',
    });
  }, [canOperateCounter, canViewAttendance, canViewSchedule]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const attendanceByShift = useMemo(
    () => new Map(data.attendance.map(item => [item.shiftId, item])),
    [data.attendance],
  );
  const orderSummary = useMemo(() => data.orders.reduce((result, order) => {
    result.orders += 1;
    result.tickets += seatCount(order);
    if (['CONFIRMED', 'COMPLETED'].includes(order.status)) {
      result.revenue += Number(order.totalAmount || 0);
    }
    if (order.status === 'PENDING_PAYMENT') result.pending += 1;
    return result;
  }, { orders: 0, tickets: 0, revenue: 0, pending: 0 }), [data.orders]);
  const activeAttendance = data.attendance.find(item => item.checkInAt && !item.checkOutAt);

  if (state.loading) {
    return <div className="flex min-h-[60vh] items-center justify-center gap-3 text-zinc-500"><LoaderCircle className="animate-spin" /> Đang chuẩn bị tổng quan ca…</div>;
  }

  return (
    <section className="mx-auto w-full max-w-7xl space-y-6 text-white">
      <header className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-6 md:p-8">
        <p className="text-xs font-black uppercase tracking-[0.22em] text-amber-500">Bắt đầu ngày làm việc</p>
        <div className="mt-2 flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h1 className="text-3xl font-black">Tổng quan ca</h1>
            <p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">
              Xem ca làm hôm nay, tình trạng két tiền và kết quả bán hàng của chính bạn.
            </p>
          </div>
          <div className="rounded-2xl border border-emerald-500/25 bg-emerald-500/[0.06] px-4 py-3">
            <p className="flex items-center gap-2 text-[10px] font-black uppercase text-emerald-400"><MapPin size={14} /> Rạp đang làm việc</p>
            <p className="mt-1 font-black">{data.context?.cinemaName || 'Chưa xác định được rạp'}</p>
          </div>
        </div>
      </header>

      {state.warning ? <p className="rounded-xl border border-amber-500/25 bg-amber-500/10 p-4 text-sm text-amber-200">{state.warning}</p> : null}

      {canOperateCounter ? <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[
          ['Đơn đã tạo hôm nay', orderSummary.orders, ClipboardList, 'text-sky-300'],
          ['Vé đã bán', orderSummary.tickets, Ticket, 'text-violet-300'],
          ['Doanh thu đã thu', money(orderSummary.revenue), WalletCards, 'text-emerald-300'],
          ['Đơn chờ thanh toán', orderSummary.pending, Clock3, 'text-amber-300'],
        ].map(([label, value, Icon, tone]) => <article key={label} className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-5"><div className="flex items-center justify-between"><p className="text-xs font-bold text-zinc-500">{label}</p><Icon size={18} className={tone} /></div><p className={`mt-3 text-2xl font-black ${tone}`}>{value}</p></article>)}
      </div> : null}

      <div className="grid gap-6 xl:grid-cols-2">
        <article className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-6">
          <div className="flex items-center justify-between"><div className="flex items-center gap-2"><CalendarDays className="text-amber-400" size={20} /><h2 className="font-black">Ca làm hôm nay</h2></div>{canViewAttendance ? <button type="button" onClick={() => navigate('/employee/checkin')} className="text-xs font-black text-amber-400">Mở chấm công</button> : null}</div>
          {data.shifts.length ? <div className="mt-4 space-y-3">{data.shifts.map(shift => {
            const record = attendanceByShift.get(shift.id);
            return <div key={shift.id} className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4"><div className="flex flex-wrap items-start justify-between gap-3"><div><p className="text-lg font-black">{clock(shift.scheduledStart)} – {clock(shift.scheduledEnd)}</p><p className="mt-1 text-xs text-zinc-500">{shift.location || data.context?.cinemaName || 'Rạp được phân công'}</p></div><span className="rounded-full border border-zinc-700 bg-zinc-800 px-3 py-1 text-[11px] font-black text-zinc-300">{record?.checkOutAt ? 'Đã ra ca' : record?.checkInAt ? 'Đang trong ca' : shiftStatus(shift.status)}</span></div></div>;
          })}</div> : <div className="py-10 text-center"><CalendarDays className="mx-auto text-zinc-700" size={36} /><p className="mt-3 text-sm font-black text-zinc-400">Hôm nay chưa có ca được xếp</p><p className="mt-1 text-xs text-zinc-600">Nếu đây là ngày làm việc, hãy liên hệ quản lý rạp.</p></div>}
        </article>

        {canOperateCounter ? <article className={`rounded-3xl border p-6 ${data.cashSession ? 'border-emerald-500/25 bg-emerald-500/[0.05]' : 'border-amber-500/25 bg-amber-500/[0.05]'}`}>
          <div className="flex items-center gap-2"><Banknote className={data.cashSession ? 'text-emerald-400' : 'text-amber-400'} size={20} /><h2 className="font-black">Két tiền trong ca</h2></div>
          {data.cashSession ? <><p className="mt-5 flex items-center gap-2 text-lg font-black text-emerald-300"><CheckCircle2 size={20} /> Ca thu ngân đang mở</p><div className="mt-4 grid grid-cols-2 gap-3 text-sm"><div className="rounded-xl bg-zinc-950/50 p-4"><p className="text-xs text-zinc-500">Hệ thống dự kiến</p><p className="mt-1 font-black">{money(data.cashSession.expectedCash)}</p></div><div className="rounded-xl bg-zinc-950/50 p-4"><p className="text-xs text-zinc-500">Giao dịch đã thu</p><p className="mt-1 font-black">{data.cashSession.cashTransactionCount || 0}</p></div></div><button type="button" onClick={() => navigate('/employee/cash-session')} className="mt-5 w-full rounded-xl bg-emerald-500 py-3 text-sm font-black text-black">Kiểm đếm và chốt ca</button></> : <><p className="mt-5 text-lg font-black text-amber-200">Chưa mở ca thu ngân</p><p className="mt-2 text-sm leading-6 text-zinc-400">Kiểm đếm tiền đầu ca và mở két trước khi nhận giao dịch tiền mặt đầu tiên.</p><button type="button" onClick={() => navigate('/employee/cash-session')} className="mt-5 w-full rounded-xl bg-amber-500 py-3 text-sm font-black text-black">Mở ca thu ngân</button></>}
        </article> : null}
      </div>

      {canOperateCounter ? <section className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-6"><div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between"><div><h2 className="font-black">Việc cần làm tiếp theo</h2><p className="mt-1 text-sm text-zinc-500">{activeAttendance ? 'Bạn đang trong ca làm việc.' : 'Kiểm tra chấm công trước khi bắt đầu phục vụ khách.'}</p></div><div className="flex flex-wrap gap-2"><button type="button" onClick={() => navigate('/employee/box-office')} className="rounded-xl bg-amber-500 px-4 py-2.5 text-sm font-black text-black">Bán vé mới</button><button type="button" onClick={() => navigate('/employee/orders')} className="rounded-xl border border-zinc-700 px-4 py-2.5 text-sm font-black">Xem đơn tại quầy</button><button type="button" onClick={load} className="flex items-center gap-2 rounded-xl border border-zinc-700 px-4 py-2.5 text-sm font-black"><RefreshCw size={16} /> Làm mới</button></div></div></section> : null}
    </section>
  );
}
