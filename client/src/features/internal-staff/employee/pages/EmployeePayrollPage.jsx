import { useCallback, useEffect, useState } from 'react';
import { getMyPayrolls } from '../../admin/services/userAdminService';
import { AsyncState, Input, StatusBadge } from '@/components/common/ui/uiKit';

const money = value => new Intl.NumberFormat('vi-VN', {
  style: 'currency', currency: 'VND', maximumFractionDigits: 0
}).format(value || 0);

export default function EmployeePayrollPage() {
  const [query, setQuery] = useState({ month: '', page: 0, size: 12 });
  const [result, setResult] = useState({ content: [], totalPages: 0 });
  const [state, setState] = useState({ loading: true, error: '' });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const data = await getMyPayrolls({
        month: query.month || undefined,
        page: query.page,
        size: query.size
      });
      setResult(data || { content: [], totalPages: 0 });
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải bảng lương của bạn.' });
    }
  }, [query]);

  useEffect(() => {
    // Loading remote payroll state is the synchronization performed by this effect.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <div>
        <h1 className="text-2xl font-black uppercase">Lương của tôi</h1>
        <p className="mt-1 text-sm text-zinc-500">Xem lịch sử lương và thưởng phạt theo tháng.</p>
      </div>

      <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-4">
        <Input 
          type="month" 
          value={query.month}
          onChange={event => setQuery(value => ({ ...value, month: event.target.value, page: 0 }))} 
        />
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load}
        empty={!result.content?.length} emptyMessage="Bạn chưa có bản ghi lương nào">
        
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {result.content?.map(payroll => (
            <div key={payroll.id} className="rounded-2xl border border-zinc-800 bg-zinc-900 p-5 space-y-4">
              <div className="flex items-center justify-between border-b border-zinc-800 pb-3">
                <span className="font-bold text-lg text-white">
                  Tháng {String(payroll.salaryMonth || '').slice(0, 7)}
                </span>
                <StatusBadge status={payroll.status} />
              </div>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-zinc-400">Lương cơ bản</span>
                  <span className="font-bold">{money(payroll.baseSalary)}</span>
                </div>
                <div className="flex justify-between text-emerald-400">
                  <span>Thưởng</span>
                  <span className="font-bold">+{money(payroll.bonusSalary)}</span>
                </div>
                <div className="flex justify-between text-red-400">
                  <span>Phạt</span>
                  <span className="font-bold">-{money(payroll.penaltySalary)}</span>
                </div>
                {payroll.note && (
                  <div className="pt-2 border-t border-zinc-800">
                    <span className="text-xs text-zinc-500 block mb-1">Ghi chú:</span>
                    <p className="text-zinc-300 italic">"{payroll.note}"</p>
                  </div>
                )}
              </div>
              <div className="flex items-center justify-between border-t border-zinc-800 pt-3">
                <span className="text-zinc-400 font-bold">Thực nhận</span>
                <span className="text-xl font-black text-brand-orange">{money(payroll.totalSalary)}</span>
              </div>
            </div>
          ))}
        </div>

      </AsyncState>

      <div className="flex justify-end gap-2">
        <button disabled={query.page === 0} onClick={() => setQuery(value => ({ ...value, page: value.page - 1 }))}
          className="rounded-lg border border-zinc-800 px-4 py-2 disabled:opacity-40 text-sm">Trước</button>
        <button disabled={query.page + 1 >= result.totalPages}
          onClick={() => setQuery(value => ({ ...value, page: value.page + 1 }))}
          className="rounded-lg border border-zinc-800 px-4 py-2 disabled:opacity-40 text-sm">Sau</button>
      </div>
    </section>
  );
}
