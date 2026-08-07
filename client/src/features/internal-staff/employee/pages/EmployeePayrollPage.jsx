import { useCallback, useEffect, useState } from 'react';
import { AsyncState, StatusBadge } from '@/components/common/ui/uiKit';
import { getMyPayrolls } from '../../admin/services/userAdminService';

const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value || 0);

export default function EmployeePayrollPage() {
  const [month, setMonth] = useState('');
  const [result, setResult] = useState({ content: [] });
  const [state, setState] = useState({ loading: true, error: '' });
  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      setResult(await getMyPayrolls({ month: month || undefined, page: 0, size: 24, sort: 'salaryMonth,desc' }) || { content: [] });
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải phiếu lương.' });
    }
  }, [month]);
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  return <section className="space-y-6 text-white"><header className="flex flex-col gap-4 border-b border-zinc-800 pb-6 md:flex-row md:items-end md:justify-between"><div><p className="text-xs font-black uppercase tracking-[0.2em] text-amber-500">Payroll self-service</p><h1 className="mt-2 text-3xl font-black">Phiếu lương của tôi</h1><p className="mt-2 text-sm text-zinc-500">Chỉ phiếu đã duyệt trở lên mới được hiển thị cho nhân viên.</p></div><input aria-label="Kỳ lương" type="month" value={month} onChange={event => setMonth(event.target.value)} className="rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-2" /></header><AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!result.content?.length} emptyMessage="Chưa có phiếu lương được công bố"><div className="grid gap-4 lg:grid-cols-2">{result.content?.map(item => <article key={item.id} className="rounded-2xl border border-zinc-800 bg-zinc-900 p-5"><div className="flex items-start justify-between"><div><p className="text-lg font-black">Kỳ {String(item.salaryMonth).slice(0, 7)}</p><p className="mt-1 text-xs text-zinc-500">Nguồn: {item.sourceType === 'TIMEKEEPING' ? 'Ca làm & chấm công' : 'Phiếu ngoại lệ'}</p></div><StatusBadge status={item.status} /></div><dl className="mt-5 grid grid-cols-2 gap-3 rounded-xl bg-zinc-950 p-4 text-sm"><div><dt className="text-zinc-500">Lương cơ bản</dt><dd className="font-bold">{money(item.basicSalary)}</dd></div><div><dt className="text-zinc-500">Phụ cấp</dt><dd className="font-bold text-emerald-400">+{money(item.allowance)}</dd></div><div><dt className="text-zinc-500">Thưởng</dt><dd className="font-bold text-emerald-400">+{money(item.bonus)}</dd></div><div><dt className="text-zinc-500">Khấu trừ</dt><dd className="font-bold text-red-400">-{money(item.deduction)}</dd></div></dl>{item.sourceType === 'TIMEKEEPING' && <p className="mt-3 text-xs text-zinc-500">{item.workedMinutes} phút công · {item.paidLeaveMinutes} phút nghỉ hưởng lương · {item.overtimeMinutes} phút tăng ca</p>}<div className="mt-5 flex items-end justify-between border-t border-zinc-800 pt-4"><span className="text-sm font-bold text-zinc-400">Thực nhận</span><span className="text-2xl font-black text-amber-500">{money(item.totalSalary)}</span></div>{item.status === 'PAYMENT_PENDING' && <p className="mt-3 text-xs text-amber-400">Đã gửi ngân hàng, đang chờ đối soát kế toán.</p>}{item.status === 'PAID' && <p className="mt-3 text-xs text-emerald-400">Đã đối soát · {item.paymentReference}</p>}</article>)}</div></AsyncState></section>;
}
