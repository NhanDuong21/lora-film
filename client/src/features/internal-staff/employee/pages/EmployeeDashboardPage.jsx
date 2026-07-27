import { useCallback, useEffect, useState } from 'react';
import { getMyPayrolls } from '../../admin/services/userAdminService';
import { AsyncState, StatusBadge } from '@/components/common/ui/uiKit';

export default function EmployeeDashboardPage() {
  const [result, setResult] = useState({ content: [] });
  const [state, setState] = useState({ loading: true, error: '' });
  const load = useCallback(async () => {
    try {
      setResult(await getMyPayrolls({ page: 0, size: 6, sort: 'salaryMonth,desc' }) || { content: [] });
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải thông tin nhân viên.' });
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setState(prev => ({ ...prev, loading: true, error: '' }));
    load();
  }, [load]);

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-zinc-950 p-6 text-white">
      <div><h1 className="text-xl font-black uppercase">Bảng điều khiển nhân viên</h1>
        <p className="mt-1 text-sm text-zinc-500">Các kỳ lương gần nhất của tài khoản đang đăng nhập.</p></div>
      <AsyncState loading={state.loading} error={state.error} onRetry={load}
        empty={!result.content?.length} emptyMessage="Chưa có dữ liệu lương">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {result.content?.map(payroll => <article key={payroll.id}
            className="rounded-2xl border border-zinc-800 bg-zinc-900 p-5">
            <div className="flex items-center justify-between"><p className="font-bold">
              Kỳ {String(payroll.salaryMonth || '').slice(0, 7)}</p>
              <StatusBadge status={payroll.status} /></div>
            <p className="mt-6 text-xs uppercase text-zinc-500">Thực nhận</p>
            <p className="mt-1 text-2xl font-black">
              {Number(payroll.totalSalary || 0).toLocaleString('vi-VN')} ₫</p>
          </article>)}
        </div>
      </AsyncState>
    </section>
  );
}
