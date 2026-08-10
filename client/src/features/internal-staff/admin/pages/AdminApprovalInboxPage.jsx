import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams, useOutletContext } from 'react-router-dom';
import { Check, CreditCard, Umbrella, X } from 'lucide-react';
import { AsyncState, StatusBadge } from '@/components/common/ui/uiKit';
import { applyLeaveRequestAction, applyPayrollAction, getLeaveRequests, getPayrolls } from '../services/userAdminService';
import useAdminAccess from '../hooks/useAdminAccess';
import { ActionModal } from '../components/OperationsConsole';
import { HrHero, PersonAvatar, UatGuide } from '../components/HrWorkspace';

const page = { content: [], totalElements: 0 };
const LEAVE_TYPE_LABELS = {
  ANNUAL: 'Nghỉ phép năm',
  SICK: 'Nghỉ ốm',
  UNPAID: 'Nghỉ không lương',
  MATERNITY: 'Nghỉ thai sản',
  PATERNITY: 'Nghỉ chăm con',
  OTHER: 'Nghỉ khác'
};
const month = () => new Date().toISOString().slice(0, 7);
const monthDates = value => {
  const first = value + '-01';
  const last = new Date(first + 'T00:00:00');
  last.setMonth(last.getMonth() + 1);
  last.setDate(0);
  return { from: first, to: last.toISOString().slice(0, 10) };
};
const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value || 0);

export default function AdminApprovalInboxPage() {
  const can = useAdminAccess();
  const notify = useOutletContext()?.triggerToast || (() => undefined);
  const [params, setParams] = useSearchParams();
  const tab = params.get('type') === 'payroll' ? 'payroll' : 'leave';
  const [data, setData] = useState({ leaves: page, payrolls: page });
  const [state, setState] = useState({ loading: true, error: '' });
  const [review, setReview] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const period = month();
      const [leaves, payrolls] = await Promise.all([
        can('EMPLOYEE_VIEW') ? getLeaveRequests({ ...monthDates(period), status: 'PENDING', page: 0, size: 200, sort: 'createdAt,asc' }) : Promise.resolve(page),
        can('PAYROLL_VIEW') ? getPayrolls({ month: period, status: 'PENDING_APPROVAL', page: 0, size: 200 }) : Promise.resolve(page)
      ]);
      setData({ leaves: leaves || page, payrolls: payrolls || page });
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải danh sách chờ duyệt.' });
    }
  }, [can]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const items = tab === 'leave' ? data.leaves.content : data.payrolls.content;
  const counts = useMemo(() => ({ leave: data.leaves.totalElements || data.leaves.content.length, payroll: data.payrolls.totalElements || data.payrolls.content.length }), [data]);

  const submit = async event => {
    event.preventDefault();
    setSubmitting(true);
    try {
      if (review.kind === 'leave') {
        await applyLeaveRequestAction(review.item.id, { type: review.action, note: review.reason.trim(), expectedVersion: review.item.version });
      } else {
        await applyPayrollAction(review.item.id, { type: review.action, reason: review.reason.trim(), expectedVersion: review.item.version });
      }
      notify(review.action === 'REJECT' ? 'Đã từ chối và lưu lý do.' : 'Đã duyệt và ghi nhận người thực hiện.');
      setReview(null);
      await load();
    } catch (error) {
      notify(error?.message || 'Không thể hoàn tất bước duyệt.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const open = (kind, item, action) => setReview({ kind, item, action, reason: '' });

  return (
    <section className="min-h-full space-y-5 text-white">
      <HrHero context="Một hàng đợi, một cách xử lý" title="Việc chờ duyệt" description="Gom các quyết định của quản lý vào một nơi. Mỗi yêu cầu hiển thị đủ căn cứ để duyệt hoặc từ chối, không cần mở nhiều màn hình." actions={<UatGuide compact />} />
      <div className="grid gap-3 sm:grid-cols-2">
        <button type="button" onClick={() => setParams({ type: 'leave' })} className={'flex items-center justify-between rounded-2xl border p-5 text-left ' + (tab === 'leave' ? 'border-orange-500/40 bg-orange-500/10' : 'border-white/10 bg-[#0b0b0e] hover:bg-white/[0.04]')}>
          <span className="flex items-center gap-3"><span className="rounded-xl bg-amber-500/10 p-3 text-amber-300"><Umbrella size={20} /></span><span><span className="block font-black">Nghỉ phép</span><span className="mt-1 block text-xs text-zinc-500">Kiểm tra ngày nghỉ và lý do</span></span></span>
          <span className="text-2xl font-black">{counts.leave}</span>
        </button>
        <button type="button" onClick={() => setParams({ type: 'payroll' })} className={'flex items-center justify-between rounded-2xl border p-5 text-left ' + (tab === 'payroll' ? 'border-orange-500/40 bg-orange-500/10' : 'border-white/10 bg-[#0b0b0e] hover:bg-white/[0.04]')}>
          <span className="flex items-center gap-3"><span className="rounded-xl bg-blue-500/10 p-3 text-blue-300"><CreditCard size={20} /></span><span><span className="block font-black">Phiếu lương</span><span className="mt-1 block text-xs text-zinc-500">Kiểm soát trước thanh toán</span></span></span>
          <span className="text-2xl font-black">{counts.payroll}</span>
        </button>
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!items.length} emptyMessage={tab === 'leave' ? 'Không còn đơn nghỉ phép nào chờ duyệt' : 'Không còn phiếu lương nào chờ duyệt'}>
        <div className="grid gap-3">
          {tab === 'leave' ? items.map(item => (
            <article key={item.id} className="grid gap-5 rounded-2xl border border-white/10 bg-[#0b0b0e] p-5 lg:grid-cols-[minmax(220px,.7fr)_minmax(300px,1.3fr)_auto] lg:items-center">
              <div className="flex items-center gap-3"><PersonAvatar name={item.employeeName} size="lg" /><div><p className="font-black">{item.employeeName}</p><p className="mt-1 text-xs text-zinc-500">{item.employeeCode}</p></div></div>
              <div><p className="text-sm font-bold text-zinc-200">{item.startDate} → {item.endDate}</p><p className="mt-1 text-xs text-zinc-500">{LEAVE_TYPE_LABELS[item.leaveType] || item.leaveType} · {item.paid ? 'Có hưởng lương' : 'Không hưởng lương'}</p><p className="mt-2 text-sm leading-6 text-zinc-400">{item.reason}</p></div>
              <div className="flex gap-2 lg:justify-end"><button type="button" onClick={() => open('leave', item, 'REJECT')} className="inline-flex items-center gap-2 rounded-xl border border-red-500/30 px-4 py-2.5 text-sm font-black text-red-300"><X size={16} /> Từ chối</button><button type="button" onClick={() => open('leave', item, 'APPROVE')} className="inline-flex items-center gap-2 rounded-xl bg-emerald-500 px-4 py-2.5 text-sm font-black text-black"><Check size={16} /> Duyệt</button></div>
            </article>
          )) : items.map(item => (
            <article key={item.id} className="grid gap-5 rounded-2xl border border-white/10 bg-[#0b0b0e] p-5 lg:grid-cols-[minmax(220px,.7fr)_minmax(300px,1.3fr)_auto] lg:items-center">
              <div className="flex items-center gap-3"><PersonAvatar name={item.employeeName} size="lg" /><div><p className="font-black">{item.employeeName}</p><p className="mt-1 text-xs text-zinc-500">{item.employeeCode} · Kỳ {String(item.salaryMonth).slice(0, 7)}</p></div></div>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-4"><div><p className="text-[10px] font-black uppercase text-zinc-600">Lương cơ bản</p><p className="mt-1 text-sm font-bold">{money(item.basicSalary)}</p></div><div><p className="text-[10px] font-black uppercase text-zinc-600">Phụ cấp</p><p className="mt-1 text-sm font-bold">{money(item.allowance)}</p></div><div><p className="text-[10px] font-black uppercase text-zinc-600">Khấu trừ</p><p className="mt-1 text-sm font-bold">{money(item.deduction)}</p></div><div><p className="text-[10px] font-black uppercase text-orange-400">Thực nhận</p><p className="mt-1 text-sm font-black text-orange-300">{money(item.totalSalary)}</p></div></div>
              <div className="flex items-center gap-3 lg:justify-end"><StatusBadge status={item.status} label="Chờ duyệt" />{can('PAYROLL_APPROVE') ? <button type="button" onClick={() => open('payroll', item, 'APPROVE')} className="inline-flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2.5 text-sm font-black text-black"><Check size={16} /> Duyệt phiếu</button> : null}</div>
            </article>
          ))}
        </div>
      </AsyncState>

      <ActionModal open={Boolean(review)} onClose={() => setReview(null)} title={review?.action === 'REJECT' ? 'Từ chối yêu cầu' : 'Xác nhận duyệt'} description={review?.kind === 'payroll' ? 'Người lập phiếu không thể tự duyệt. API sẽ kiểm tra độc lập.' : 'Quyết định sẽ được lưu cùng người thực hiện và thời điểm.'} onSubmit={submit} submitLabel={review?.action === 'REJECT' ? 'Xác nhận từ chối' : 'Xác nhận duyệt'} submitting={submitting} tone={review?.action === 'REJECT' ? 'danger' : 'orange'}>
        <label className="block text-xs font-black uppercase tracking-wider text-zinc-500">Lý do / căn cứ *<textarea required minLength={5} value={review?.reason || ''} onChange={event => setReview(value => ({ ...value, reason: event.target.value }))} rows={4} placeholder="Ví dụ: Đã kiểm tra lịch làm việc và số ngày nghỉ còn lại…" className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm font-normal leading-6 text-white outline-none focus:border-orange-500" /></label>
      </ActionModal>
    </section>
  );
}
